// 全局配置
const API_BASE_URL = 'http://localhost:4556';
let authToken = localStorage.getItem('authToken');
let currentUser = null;

// 页面加载时检查登录状态
window.addEventListener('DOMContentLoaded', () => {
    if (authToken) {
        validateToken();
    }
});

// ==================== 认证相关 ====================

// 登录
document.getElementById('loginForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            authToken = result.data.token;
            currentUser = result.data;
            localStorage.setItem('authToken', authToken);
            // 获取用户真实姓名
            await loadUserName();
            showMainContent();
            showResult('loginResult', '登录成功！', false);
        } else {
            showResult('loginResult', result.message || '登录失败', true);
        }
    } catch (error) {
        showResult('loginResult', '登录失败：' + error.message, true);
    }
});

// 退出登录
function logout() {
    authToken = null;
    currentUser = null;
    localStorage.removeItem('authToken');
    document.getElementById('loginSection').style.display = 'block';
    document.getElementById('mainContent').style.display = 'none';
}

// 验证Token
async function validateToken() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/validate`, {
            headers: {
                'Authorization': authToken
            }
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            // Token有效，获取用户真实姓名
            await loadUserName();
            showMainContent();
        } else {
            logout();
        }
    } catch (error) {
        console.error('Token验证失败:', error);
        logout();
    }
}

// 获取用户真实姓名
async function loadUserName() {
    try {
        const response = await fetch(`${API_BASE_URL}/org/user/getName`, {
            headers: {
                'Authorization': authToken
            }
        });
        
        const result = await response.json();
        
        if (result.code === 200 && result.data) {
            currentUser = currentUser || {};
            currentUser.realName = result.data;
        }
    } catch (error) {
        console.error('获取用户姓名失败:', error);
    }
}

// 显示主内容
function showMainContent() {
    document.getElementById('loginSection').style.display = 'none';
    document.getElementById('mainContent').style.display = 'block';
    document.getElementById('realname').textContent = currentUser?.realName || currentUser?.username || '用户';
    
    // 加载数据
    loadApplications();
    loadPendingList();
    loadTemplates();
    loadDeptTree();
    loadUsers();
}

// ==================== 标签页切换 ====================

function switchTab(tabName) {
    // 隐藏所有内容
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });
    
    // 移除所有标签的active状态
    document.querySelectorAll('.tab').forEach(tab => {
        tab.classList.remove('active');
    });
    
    // 显示选中的内容
    document.getElementById(tabName).classList.add('active');
    
    // 激活对应的标签按钮
    event.target.classList.add('active');
}

// ==================== 用车申请相关 ====================

async function loadApplications() {
    const status = document.getElementById('filterStatus').value;
    const url = `${API_BASE_URL}/apply/sub/my-list${status ? '?status=' + status : ''}`;
    
    try {
        const response = await fetch(url, {
            headers: {
                'Authorization': authToken
            }
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            displayApplications(result.data.records || []);
        } else {
            console.error('加载申请列表失败:', result.message);
        }
    } catch (error) {
        console.error('加载申请列表失败:', error);
    }
}

function displayApplications(applications) {
    const tbody = document.getElementById('applicationsList');
    
    if (applications.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align: center;">暂无数据</td></tr>';
        return;
    }
    
    tbody.innerHTML = applications.map(app => `
        <tr>
            <td>${app.title}</td>
            <td>${app.applicantName}</td>
            <td>${formatDate(app.startDate)} ~ ${formatDate(app.endDate)}</td>
            <td>${app.destination}</td>
            <td><span class="status-badge status-${getStatusClass(app.status)}">${getStatusText(app.status)}</span></td>
            <td>${formatDateTime(app.createTime)}</td>
            <td class="action-buttons">
                <button class="btn btn-primary" onclick="viewApplicationDetail(${app.applyId})">详情</button>
                ${app.status === 0 || app.status === 4 ? `<button class="btn btn-warning" onclick="editApplication(${app.applyId})">编辑</button>` : ''}
                ${app.status === 1 || app.status === 2 ? `<button class="btn btn-danger" onclick="cancelApplication(${app.applyId})">撤销</button>` : ''}
            </td>
        </tr>
    `).join('');
}

function showCreateApplication() {
    const title = prompt('请输入申请标题：');
    if (!title) return;
    
    const purpose = prompt('请输入用车事由：');
    if (!purpose) return;
    
    const destination = prompt('请输入目的地：');
    if (!destination) return;
    
    const startDate = prompt('请输入开始日期（YYYY-MM-DD）：', new Date().toISOString().split('T')[0]);
    const endDate = prompt('请输入结束日期（YYYY-MM-DD）：', startDate);
    const startTime = prompt('请输入开始时间（HH:MM）：', '09:00');
    const endTime = prompt('请输入结束时间（HH:MM）：', '18:00');
    const passengerCount = prompt('请输入用车人数：', '1');
    const vehicleType = prompt('请输入车辆类型（sedan/business/bus）：', 'sedan');
    
    const data = {
        title,
        templateId: 1, // 默认使用第一个模板
        startDate,
        endDate,
        startTime,
        endTime,
        purpose,
        destination,
        passengerCount: parseInt(passengerCount),
        vehicleType
    };
    
    submitApplication(data);
}

async function submitApplication(data) {
    try {
        const response = await fetch(`${API_BASE_URL}/apply/sub/submit-directly`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': authToken
            },
            body: JSON.stringify(data)
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            alert('申请提交成功！');
            loadApplications();
        } else {
            alert('提交失败：' + result.message);
        }
    } catch (error) {
        alert('提交失败：' + error.message);
    }
}

async function cancelApplication(applyId) {
    if (!confirm('确定要撤销此申请吗？')) return;
    
    try {
        const response = await fetch(`${API_BASE_URL}/apply/sub/cancel/${applyId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': authToken
            }
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            alert('申请已撤销');
            loadApplications();
        } else {
            alert('撤销失败：' + result.message);
        }
    } catch (error) {
        alert('撤销失败：' + error.message);
    }
}

async function viewApplicationDetail(applyId) {
    try {
        const response = await fetch(`${API_BASE_URL}/apply/sub/detail/${applyId}`, {
            headers: {
                'Authorization': authToken
            }
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            const detail = result.data;
            const info = `
申请标题：${detail.title}
申请人：${detail.applicantName}
部门：${detail.deptName}
用车日期：${formatDate(detail.startDate)} ~ ${formatDate(detail.endDate)}
用车时间：${detail.startTime} ~ ${detail.endTime}
目的地：${detail.destination}
用车人数：${detail.passengerCount}
车辆类型：${detail.vehicleTypeDesc}
事由：${detail.purpose}
状态：${getStatusText(detail.status)}
${detail.attachment ? '附件：' + detail.attachment : ''}

审批历史：
${(detail.approvalHistory || []).map(h => 
    `- [${h.nodeName}] ${h.approverName}: ${h.actionDesc || '待审批'} ${h.comment ? '(' + h.comment + ')' : ''} ${h.operateTime ? formatDateTime(h.operateTime) : ''}`
).join('\n')}
            `.trim();
            
            alert(info);
        } else {
            alert('获取详情失败：' + result.message);
        }
    } catch (error) {
        alert('获取详情失败：' + error.message);
    }
}

// ==================== 审批管理 ====================

async function loadPendingList() {
    try {
        const response = await fetch(`${API_BASE_URL}/apply/app/pending-list`, {
            headers: {
                'Authorization': authToken
            }
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            displayPendingList(result.data.records || []);
        }
    } catch (error) {
        console.error('加载待审批列表失败:', error);
    }
}

function displayPendingList(pendingList) {
    const tbody = document.getElementById('pendingList');
    
    if (pendingList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center;">暂无待审批事项</td></tr>';
        return;
    }
    
    tbody.innerHTML = pendingList.map(item => `
        <tr>
            <td>${item.title}</td>
            <td>${item.applicantName}</td>
            <td>${item.currentNodeName}</td>
            <td>${formatDate(item.startDate)}</td>
            <td>${item.waitTime || '-'}</td>
            <td class="action-buttons">
                <button class="btn btn-success" onclick="approveApplication(${item.applyId})">同意</button>
                <button class="btn btn-danger" onclick="rejectApplication(${item.applyId})">驳回</button>
            </td>
        </tr>
    `).join('');
}

async function approveApplication(applyId) {
    const comment = prompt('请输入审批意见（可选）：');
    
    try {
        const response = await fetch(`${API_BASE_URL}/apply/app/agree`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': authToken
            },
            body: JSON.stringify({ applyId, comment })
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            alert('审批通过');
            loadPendingList();
        } else {
            alert('审批失败：' + result.message);
        }
    } catch (error) {
        alert('审批失败：' + error.message);
    }
}

async function rejectApplication(applyId) {
    const comment = prompt('请输入驳回原因（必填）：');
    if (!comment) {
        alert('驳回原因不能为空');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/apply/app/reject`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': authToken
            },
            body: JSON.stringify({ applyId, comment })
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            alert('已驳回申请');
            loadPendingList();
        } else {
            alert('驳回失败：' + result.message);
        }
    } catch (error) {
        alert('驳回失败：' + error.message);
    }
}

// ==================== 流程模板 ====================

async function loadTemplates() {
    try {
        const response = await fetch(`${API_BASE_URL}/flow/template/list`, {
            headers: {
                'Authorization': authToken
            }
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            displayTemplates(result.data || []);
        }
    } catch (error) {
        console.error('加载模板列表失败:', error);
    }
}

function displayTemplates(templates) {
    const tbody = document.getElementById('templatesList');
    
    if (templates.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center;">暂无数据</td></tr>';
        return;
    }
    
    tbody.innerHTML = templates.map(template => `
        <tr>
            <td>${template.templateName}</td>
            <td>${getTemplateTypeText(template.templateType)}</td>
            <td>${template.nodeCount}</td>
            <td><span class="status-badge status-${template.status === 1 ? 'success' : 'rejected'}">${template.status === 1 ? '启用' : '禁用'}</span></td>
            <td>${formatDateTime(template.createTime)}</td>
            <td class="action-buttons">
                <button class="btn btn-primary" onclick="viewTemplateDetail(${template.templateId})">详情</button>
            </td>
        </tr>
    `).join('');
}

async function viewTemplateDetail(templateId) {
    try {
        const response = await fetch(`${API_BASE_URL}/flow/template/detail/${templateId}`, {
            headers: {
                'Authorization': authToken
            }
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            const template = result.data;
            const info = `
模板名称：${template.templateName}
流程类型：${getTemplateTypeText(template.templateType)}
描述：${template.description || '-'}
状态：${template.status === 1 ? '启用' : '禁用'}

节点配置：
${(template.nodeConfig || []).map(node => 
    `- [节点${node.nodeOrder}] ${node.nodeName}\n  审批人类型：${node.approverType === 'user' ? '指定用户' : '指定角色'}\n  审批规则：${node.approvalRule === 'and' ? '会签' : '或签'}\n  超时时间：${node.timeoutHours}小时`
).join('\n')}
            `.trim();
            
            alert(info);
        }
    } catch (error) {
        alert('获取详情失败：' + error.message);
    }
}

function showCreateTemplate() {
    alert('创建流程模板功能较为复杂，建议在后端管理系统中操作。\n\n您可以参考API文档中的接口说明进行开发。');
}

// ==================== 部门管理 ====================

async function loadDeptTree() {
    try {
        const response = await fetch(`${API_BASE_URL}/org/dept/tree`, {
            headers: {
                'Authorization': authToken
            }
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            displayDeptTree(result.data || []);
        }
    } catch (error) {
        console.error('加载部门树失败:', error);
    }
}

function displayDeptTree(depts, level = 0) {
    const container = document.getElementById('deptTree');
    
    if (level === 0) {
        container.innerHTML = '';
    }
    
    if (depts.length === 0 && level === 0) {
        container.innerHTML = '<p style="text-align: center; color: #999;">暂无数据</p>';
        return;
    }
    
    const html = depts.map(dept => `
        <div style="margin-left: ${level * 20}px; padding: 10px; border-left: 2px solid #667eea; margin-bottom: 5px;">
            <strong>${dept.name}</strong>
            <span style="color: #999; margin-left: 10px;">${dept.description || ''}</span>
            ${dept.children && dept.children.length > 0 ? displayDeptTree(dept.children, level + 1) : ''}
        </div>
    `).join('');
    
    if (level === 0) {
        container.innerHTML = html;
    } else {
        return html;
    }
}

function showCreateDept() {
    alert('创建部门功能需要填写较多字段，建议参考API文档进行完整实现。');
}

// ==================== 账号管理 ====================

async function loadUsers() {
    const username = document.getElementById('searchUsername').value;
    const realName = document.getElementById('searchRealName').value;
    
    let url = `${API_BASE_URL}/org/user/list?`;
    if (username) url += `username=${username}&`;
    if (realName) url += `realName=${realName}&`;
    
    try {
        const response = await fetch(url, {
            headers: {
                'Authorization': authToken
            }
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            displayUsers(result.data.records || []);
        }
    } catch (error) {
        console.error('加载用户列表失败:', error);
    }
}

function displayUsers(users) {
    const tbody = document.getElementById('usersList');
    
    if (users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align: center;">暂无数据</td></tr>';
        return;
    }
    
    tbody.innerHTML = users.map(user => `
        <tr>
            <td>${user.username}</td>
            <td>${user.realName}</td>
            <td>${user.phone}</td>
            <td>${user.email}</td>
            <td>${user.deptName}</td>
            <td>${user.roleName}</td>
            <td><span class="status-badge status-${user.status === 1 ? 'success' : 'rejected'}">${user.status === 1 ? '启用' : '禁用'}</span></td>
            <td class="action-buttons">
                <button class="btn btn-warning" onclick="resetPassword(${user.id})">重置密码</button>
            </td>
        </tr>
    `).join('');
}

async function resetPassword(userId) {
    if (!confirm('确定要重置该用户的密码吗？')) return;
    
    try {
        const response = await fetch(`${API_BASE_URL}/org/user/reset-password/${userId}`, {
            method: 'PUT',
            headers: {
                'Authorization': authToken
            }
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            alert('密码重置成功！临时密码已发送至用户邮箱。');
        } else {
            alert('重置失败：' + result.message);
        }
    } catch (error) {
        alert('重置失败：' + error.message);
    }
}

// ==================== 工具函数 ====================

function showResult(elementId, message, isError) {
    const element = document.getElementById(elementId);
    element.textContent = message;
    element.className = 'result-box' + (isError ? ' error' : '');
    element.style.display = 'block';
    
    setTimeout(() => {
        element.style.display = 'none';
    }, 3000);
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    return dateStr.split('T')[0];
}

function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '-';
    return dateTimeStr.replace('T', ' ').substring(0, 19);
}

function getStatusClass(status) {
    const statusMap = {
        0: 'pending',
        1: 'pending',
        2: 'pending',
        3: 'success',
        4: 'rejected',
        5: 'rejected'
    };
    return statusMap[status] || 'pending';
}

function getStatusText(status) {
    const statusMap = {
        0: '待提交',
        1: '待审批',
        2: '审批中',
        3: '已通过',
        4: '已驳回',
        5: '已撤销'
    };
    return statusMap[status] || '未知';
}

function getTemplateTypeText(type) {
    const typeMap = {
        'internal': '部门内用车',
        'cross': '跨部门用车',
        'long-distance': '长途用车'
    };
    return typeMap[type] || type;
}
