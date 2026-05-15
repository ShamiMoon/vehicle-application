# 部门流程系统（用车申请审批版）API 接口文档

## 📋 文档说明

- **版本**: v1.0
- **基础路径**: `/api`
- **认证方式**: JWT Token（Header: `Authorization: Bearer <token>`）
- **响应格式**: JSON
- **字符编码**: UTF-8

---

## 🔐 通用响应格式

### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 错误响应
```json
{
  "code": 400,
  "message": "错误描述",
  "data": null
}
```

### 权限不足响应
```json
{
  "code": 403,
  "message": "无权访问",
  "data": null
}
```

---

## 📑 目录

1. [认证模块](#1-认证模块)
2. [组织管理模块](#2-组织管理模块)
   - [部门管理](#21-部门管理)
   - [角色管理](#22-角色管理)
   - [账号管理](#23-账号管理)
3. [流程管理模块](#3-流程管理模块)
   - [流程模板](#31-流程模板)
   - [审批人管理](#32-审批人管理)
4. [用车申请模块](#4-用车申请模块)
   - [申请操作](#41-申请操作)
   - [审批操作](#42-审批操作)
5. [消息中心模块](#5-消息中心模块)
6. [文件上传模块](#6-文件上传模块)
7. [数据导出模块](#7-数据导出模块)

---

## 1. 认证模块

### 1.1 用户登录

**接口地址**: `POST /api/login`

**权限要求**: 无（公开接口）

**请求参数**:
```json
{
  "username": "string",    // 登录用户名（必填）
  "password": "string"     // 密码（必填）
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "username": "admin",
    "realName": "管理员",
    "roleId": 1,
    "roleName": "超级管理员",
    "deptId": 1,
    "deptName": "总公司"
  }
}
```

---

### 1.2 刷新 Token

**接口地址**: `POST /api/refresh`

**权限要求**: 已登录

**请求头**: `Authorization: Bearer <token>`

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

---

### 1.3 验证 Token

**接口地址**: `GET /api/validate`

**权限要求**: 已登录

**请求头**: `Authorization: Bearer <token>`

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "valid": true,
    "userId": 1,
    "username": "admin"
  }
}
```

---

## 2. 组织管理模块

### 2.1 部门管理

#### 2.1.1 新增部门

**接口地址**: `POST /org/dept/add`

**权限要求**: 角色ID = 1（超级管理员）

**请求参数**:
```json
{
  "name": "string",        // 部门名称（必填，唯一）
  "parentId": 0,           // 上级部门ID（顶级为0）
  "sort": 1,               // 排序（数字越小越靠前）
  "description": "string"  // 部门描述（可选）
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

#### 2.1.2 编辑部门

**接口地址**: `PUT /org/dept/update`

**权限要求**: 角色ID = 1

**请求参数**:
```json
{
  "id": 1,                 // 部门ID（必填）
  "name": "string",
  "parentId": 0,
  "sort": 1,
  "description": "string"
}
```

---

#### 2.1.3 删除部门

**接口地址**: `DELETE /org/dept/delete/{id}`

**权限要求**: 角色ID = 1

**路径参数**:
- `id`: 部门ID

**约束条件**: 
- 部门下不能存在账号
- 部门下不能有子部门

---

#### 2.1.4 获取部门树形列表

**接口地址**: `GET /org/dept/tree`

**权限要求**: 已登录

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "总公司",
      "parentId": 0,
      "sort": 1,
      "description": "总公司",
      "status": 1,
      "children": [
        {
          "id": 2,
          "name": "行政部",
          "parentId": 1,
          "sort": 1,
          "children": []
        },
        {
          "id": 3,
          "name": "技术部",
          "parentId": 1,
          "sort": 2,
          "children": []
        }
      ]
    }
  ]
}
```

---

#### 2.1.5 获取部门详情

**接口地址**: `GET /org/dept/detail/{id}`

**权限要求**: 已登录

**路径参数**:
- `id`: 部门ID

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "总公司",
    "parentId": 0,
    "parentName": null,
    "sort": 1,
    "description": "总公司",
    "status": 1,
    "userCount": 10,
    "createTime": "2024-01-01T00:00:00",
    "createBy": 1
  }
}
```

---

#### 2.1.6 启用/禁用部门

**接口地址**: `PUT /org/dept/status/{id}/{status}`

**权限要求**: 角色ID = 1

**路径参数**:
- `id`: 部门ID
- `status`: 1-启用，0-禁用

**注意**: 禁用部门时，下级部门同步禁用

---

#### 2.1.7 部门排序

**接口地址**: `PUT /org/dept/sort`

**权限要求**: 角色ID = 1

**请求参数**:
```json
[
  {
    "id": 1,
    "sort": 1
  },
  {
    "id": 2,
    "sort": 2
  }
]
```

---

### 2.2 角色管理

#### 2.2.1 新增角色

**接口地址**: `POST /org/role/add`

**权限要求**: 角色ID = 1

**请求参数**:
```json
{
  "name": "string",        // 角色名称（必填，唯一）
  "description": "string", // 角色描述
  "status": 1              // 状态：1-启用，0-禁用
}
```

---

#### 2.2.2 编辑角色

**接口地址**: `PUT /org/role/update`

**权限要求**: 角色ID = 1

**请求参数**:
```json
{
  "id": 1,
  "name": "string",
  "description": "string",
  "status": 1
}
```

---

#### 2.2.3 删除角色

**接口地址**: `DELETE /org/role/delete/{id}`

**权限要求**: 角色ID = 1

**约束条件**: 角色下不能有关联的账号

---

#### 2.2.4 获取角色列表

**接口地址**: `GET /org/role/list`

**权限要求**: 已登录

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "超级管理员",
      "description": "拥有所有权限",
      "status": 1,
      "userCount": 2,
      "createTime": "2024-01-01T00:00:00"
    },
    {
      "id": 2,
      "name": "用车管理员",
      "description": "管理用车申请",
      "status": 1,
      "userCount": 5,
      "createTime": "2024-01-01T00:00:00"
    }
  ]
}
```

---

#### 2.2.5 启用/禁用角色

**接口地址**: `PUT /org/role/status/{id}/{status}`

**权限要求**: 角色ID = 1

**路径参数**:
- `id`: 角色ID
- `status`: 1-启用，0-禁用

---

### 2.3 账号管理

#### 2.3.1 新增账号

**接口地址**: `POST /org/user/add`

**权限要求**: 角色ID = 1 或 2（超级管理员、用车管理员）

**请求参数**:
```json
{
  "username": "string",    // 登录用户名（必填，唯一）
  "password": "string",    // 密码（必填）
  "realName": "string",    // 真实姓名（必填）
  "phone": "string",       // 手机号（必填，唯一）
  "email": "string",       // 邮箱（必填，唯一）
  "deptId": 1,             // 所属部门ID（必填）
  "roleId": 3,             // 角色ID（必填）
  "status": 1              // 状态：1-启用，0-禁用
}
```

---

#### 2.3.2 编辑账号

**接口地址**: `PUT /org/user/update`

**权限要求**: 角色ID = 1 或 2

**请求参数**:
```json
{
  "id": 1,
  "username": "string",
  "realName": "string",
  "phone": "string",
  "email": "string",
  "deptId": 1,
  "roleId": 3,
  "status": 1
}
```

---

#### 2.3.3 删除账号

**接口地址**: `DELETE /org/user/delete/{id}`

**权限要求**: 角色ID = 1 或 2

---

#### 2.3.4 获取账号列表

**接口地址**: `GET /org/user/list`

**权限要求**: 已登录

**查询参数**:
- `username`: 用户名（可选，模糊查询）
- `realName`: 真实姓名（可选，模糊查询）
- `deptId`: 部门ID（可选）
- `roleId`: 角色ID（可选）
- `status`: 状态（可选，0-禁用，1-启用）
- `pageNum`: 页码（默认1）
- `pageSize`: 每页数量（默认10）

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "pages": 10,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 1,
        "username": "admin",
        "realName": "管理员",
        "phone": "13800138000",
        "email": "admin@example.com",
        "deptId": 1,
        "deptName": "总公司",
        "roleId": 1,
        "roleName": "超级管理员",
        "status": 1,
        "lastLoginTime": "2024-01-01T10:00:00",
        "createTime": "2024-01-01T00:00:00"
      }
    ]
  }
}
```

---

#### 2.3.5 重置密码

**接口地址**: `PUT /org/user/reset-password/{id}`

**权限要求**: 角色ID = 1 或 2

**功能说明**: 
- 系统自动生成临时密码
- 临时密码有效期24小时
- 通过邮件发送临时密码给用户

---

#### 2.3.6 修改个人信息

**接口地址**: `PUT /org/user/profile`

**权限要求**: 已登录

**请求参数**:
```json
{
  "realName": "string",
  "phone": "string",
  "email": "string"
}
```

**注意**: 仅可修改自己的信息

---

#### 2.3.7 修改密码

**接口地址**: `PUT /org/user/change-password`

**权限要求**: 已登录

**请求参数**:
```json
{
  "oldPassword": "string",
  "newPassword": "string"
}
```

---

## 3. 流程管理模块

### 3.1 流程模板

#### 3.1.1 新增流程模板

**接口地址**: `POST /flow/template/add`

**权限要求**: 角色ID = 1 或 2

**请求参数**:
```json
{
  "templateName": "string",           // 模板名称（必填，唯一）
  "templateType": "internal",         // 流程类型：internal-部门内，cross-跨部门，long-distance-长途用车
  "description": "string",            // 模板描述
  "nodeConfig": [                     // 节点配置（JSON数组）
    {
      "nodeOrder": 1,                 // 节点顺序
      "nodeName": "部门经理审批",
      "approverType": "role",         // 审批人类型：user-指定用户，role-指定角色
      "approverIds": [2],             // 审批人ID列表
      "approvalRule": "or",           // 审批规则：and-会签，or-或签
      "timeoutHours": 24              // 超时时间（小时）
    },
    {
      "nodeOrder": 2,
      "nodeName": "行政部审批",
      "approverType": "user",
      "approverIds": [5, 6],
      "approvalRule": "and",
      "timeoutHours": 48
    }
  ]
}
```

---

#### 3.1.2 编辑流程模板

**接口地址**: `PUT /flow/template/update`

**权限要求**: 角色ID = 1 或 2

**请求参数**: 同新增

---

#### 3.1.3 删除流程模板

**接口地址**: `DELETE /flow/template/delete/{templateId}`

**权限要求**: 角色ID = 1 或 2

**约束条件**: 不能有未完成的用车申请关联该模板

---

#### 3.1.4 获取流程模板列表

**接口地址**: `GET /flow/template/list`

**权限要求**: 已登录

**查询参数**:
- `templateName`: 模板名称（可选，模糊查询）
- `templateType`: 流程类型（可选）
- `status`: 状态（可选，0-禁用，1-启用）

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "templateId": 1,
      "templateName": "部门内部用车审批流程",
      "templateType": "internal",
      "description": "适用于部门内部日常用车",
      "status": 1,
      "nodeCount": 2,
      "creatorName": "管理员",
      "createTime": "2024-01-01T00:00:00",
      "updateTime": "2024-01-01T00:00:00"
    }
  ]
}
```

---

#### 3.1.5 获取流程模板详情

**接口地址**: `GET /flow/template/detail/{templateId}`

**权限要求**: 已登录

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "templateId": 1,
    "templateName": "部门内部用车审批流程",
    "templateType": "internal",
    "description": "适用于部门内部日常用车",
    "status": 1,
    "nodeConfig": [
      {
        "nodeOrder": 1,
        "nodeName": "部门经理审批",
        "approverType": "role",
        "approverIds": [2],
        "approverNames": ["部门管理员"],
        "approvalRule": "or",
        "timeoutHours": 24
      }
    ],
    "creatorName": "管理员",
    "createTime": "2024-01-01T00:00:00",
    "updateTime": "2024-01-01T00:00:00"
  }
}
```

---

#### 3.1.6 启用/禁用流程模板

**接口地址**: `PUT /flow/template/status/{templateId}/{status}`

**权限要求**: 角色ID = 1 或 2

**注意**: 禁用的模板不可被新申请使用，但已关联的申请可正常流转

---

### 3.2 审批人管理

#### 3.2.1 分配审批人

**接口地址**: `POST /flow/approver/assign`

**权限要求**: 角色ID = 1 或 2

**请求参数**:
```json
{
  "templateId": 1,
  "nodeOrder": 1,
  "approverType": "user",
  "approverIds": [5, 6]
}
```

---

#### 3.2.2 批量分配审批人

**接口地址**: `POST /flow/approver/batch-assign`

**权限要求**: 角色ID = 1 或 2

**请求参数**:
```json
{
  "templateId": 1,
  "nodes": [
    {
      "nodeOrder": 1,
      "approverType": "role",
      "approverIds": [2]
    },
    {
      "nodeOrder": 2,
      "approverType": "user",
      "approverIds": [5, 6]
    }
  ]
}
```

---

#### 3.2.3 更新审批人

**接口地址**: `PUT /flow/approver/update`

**权限要求**: 角色ID = 1 或 2

**请求参数**:
```json
{
  "templateId": 1,
  "nodeOrder": 1,
  "approverType": "user",
  "approverIds": [7, 8]
}
```

---

#### 3.2.4 删除审批人

**接口地址**: `DELETE /flow/approver/delete`

**权限要求**: 角色ID = 1 或 2

**请求参数**:
```json
{
  "templateId": 1,
  "nodeOrder": 1,
  "userId": 5
}
```

---

#### 3.2.5 获取流程模板的审批人列表

**接口地址**: `GET /flow/approver/list/{templateId}`

**权限要求**: 已登录

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "nodeOrder": 1,
      "nodeName": "部门经理审批",
      "approverType": "role",
      "approvers": [
        {
          "userId": 2,
          "userName": "张三",
          "roleName": "部门管理员"
        }
      ],
      "approvalRule": "or",
      "timeoutHours": 24
    }
  ]
}
```

---

## 4. 用车申请模块

### 4.1 申请操作

#### 4.1.1 保存草稿

**接口地址**: `POST /apply/sub/save`

**权限要求**: 已登录

**请求参数**:
```json
{
  "title": "string",              // 申请标题（必填）
  "templateId": 1,                // 流程模板ID（必填）
  "startDate": "2024-01-15",      // 用车开始日期（必填）
  "endDate": "2024-01-15",        // 用车结束日期（必填）
  "startTime": "09:00",           // 用车开始时间（必填）
  "endTime": "18:00",             // 用车结束时间（必填）
  "purpose": "string",            // 用车事由（必填）
  "passengerCount": 5,            // 用车人数（必填）
  "destination": "string",        // 目的地（必填）
  "vehicleType": "sedan",         // 车辆类型：sedan-轿车，business-商务车，bus-大巴
  "attachment": "string",         // 附件URL（可选）
  "targetDeptId": null            // 目标部门ID（跨部门用车时填写）
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "applyId": 123
  }
}
```

---

#### 4.1.2 提交申请

**接口地址**: `PUT /apply/sub/submit/{applyId}`

**权限要求**: 已登录

**路径参数**:
- `applyId`: 申请ID

**约束条件**: 
- 仅草稿（待提交）和已驳回状态的申请可提交
- 用车日期必须大于当前日期（紧急用车除外）

---

#### 4.1.3 直接提交申请

**接口地址**: `POST /apply/sub/submit-directly`

**权限要求**: 已登录

**请求参数**: 同保存草稿

**说明**: 一步完成保存和提交

---

#### 4.1.4 修改申请

**接口地址**: `PUT /apply/sub/update`

**权限要求**: 已登录（仅申请人可修改）

**请求参数**:
```json
{
  "applyId": 123,
  "title": "string",
  "templateId": 1,
  "startDate": "2024-01-15",
  "endDate": "2024-01-15",
  "startTime": "09:00",
  "endTime": "18:00",
  "purpose": "string",
  "passengerCount": 5,
  "destination": "string",
  "vehicleType": "sedan",
  "attachment": "string",
  "targetDeptId": null
}
```

**约束条件**: 仅待提交和已驳回状态的申请可修改

---

#### 4.1.5 撤销申请

**接口地址**: `DELETE /apply/sub/cancel/{applyId}`

**权限要求**: 已登录（仅申请人可撤销）

**约束条件**: 仅待审批和审批中状态的申请可撤销

---

#### 4.1.6 获取我的申请列表

**接口地址**: `GET /apply/sub/my-list`

**权限要求**: 已登录

**查询参数**:
- `status`: 审批状态（可选）
- `templateId`: 模板ID（可选）
- `startDate`: 开始日期（可选）
- `endDate`: 结束日期（可选）
- `pageNum`: 页码（默认1）
- `pageSize`: 每页数量（默认10）

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 50,
    "pages": 5,
    "current": 1,
    "size": 10,
    "records": [
      {
        "applyId": 123,
        "title": "前往客户公司拜访",
        "applicantName": "李四",
        "deptName": "技术部",
        "templateName": "部门内部用车审批流程",
        "status": 2,
        "statusDesc": "审批中",
        "startDate": "2024-01-15",
        "endDate": "2024-01-15",
        "destination": "北京市朝阳区xxx",
        "createTime": "2024-01-10T10:00:00"
      }
    ]
  }
}
```

---

#### 4.1.7 获取全量申请列表

**接口地址**: `GET /apply/sub/all-list`

**权限要求**: 角色ID = 1 或 2

**查询参数**:
- `applicantName`: 申请人姓名（可选，模糊查询）
- `deptId`: 部门ID（可选）
- `status`: 审批状态（可选）
- `templateId`: 模板ID（可选）
- `startDate`: 开始日期（可选）
- `endDate`: 结束日期（可选）
- `pageNum`: 页码
- `pageSize`: 每页数量

---

#### 4.1.8 获取申请详情

**接口地址**: `GET /apply/sub/detail/{applyId}`

**权限要求**: 已登录

**权限控制**:
- 普通员工：仅可查看自己的申请
- 部门管理员：可查看本部门及下级部门的申请
- 超级管理员/用车管理员：可查看全量申请

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "applyId": 123,
    "title": "前往客户公司拜访",
    "applicantId": 10,
    "applicantName": "李四",
    "deptId": 3,
    "deptName": "技术部",
    "templateId": 1,
    "templateName": "部门内部用车审批流程",
    "status": 2,
    "statusDesc": "审批中",
    "startDate": "2024-01-15",
    "endDate": "2024-01-15",
    "startTime": "09:00",
    "endTime": "18:00",
    "purpose": "前往客户公司进行技术交流",
    "passengerCount": 5,
    "destination": "北京市朝阳区xxx路xxx号",
    "vehicleType": "business",
    "vehicleTypeDesc": "商务车",
    "attachment": "https://xxx.oss-cn-beijing.aliyuncs.com/xxx.pdf",
    "currentNodeOrder": 1,
    "currentNodeName": "部门经理审批",
    "createTime": "2024-01-10T10:00:00",
    "updateTime": "2024-01-10T10:00:00",
    "approvalHistory": [
      {
        "nodeOrder": 1,
        "nodeName": "部门经理审批",
        "approverId": 2,
        "approverName": "张三",
        "action": "agree",
        "actionDesc": "同意",
        "comment": "同意用车",
        "operateTime": "2024-01-10T14:00:00"
      }
    ]
  }
}
```

---

#### 4.1.9 获取车辆类型列表

**接口地址**: `GET /apply/sub/vehicle-types/{templateType}`

**权限要求**: 已登录

**路径参数**:
- `templateType`: 流程类型（internal、cross、long-distance）

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "type": "sedan",
      "name": "轿车",
      "description": "适合1-4人短途出行"
    },
    {
      "type": "business",
      "name": "商务车",
      "description": "适合5-7人出行"
    },
    {
      "type": "bus",
      "name": "大巴",
      "description": "适合8人以上团体出行"
    }
  ]
}
```

---

#### 4.1.10 获取我审批过的申请

**接口地址**: `GET /apply/sub/approved-by-me`

**权限要求**: 角色ID = 4（审批人）

**查询参数**: 同我的申请列表

---

#### 4.1.11 处理异常申请

**接口地址**: `POST /apply/sub/handle-abnormal/{applyId}`

**权限要求**: 角色ID = 1 或 2

**请求参数**:
```json
{
  "action": "force_complete",  // 操作：force_complete-强制完成，force_reject-强制驳回，reassign-重新分配审批人
  "reason": "string",          // 处理原因（必填）
  "newApproverId": null        // 新审批人ID（reassign时必填）
}
```

---

### 4.2 审批操作

#### 4.2.1 获取待我审批列表

**接口地址**: `GET /apply/app/pending-list`

**权限要求**: 角色ID = 4（审批人）

**查询参数**:
- `status`: 状态（可选）
- `pageNum`: 页码
- `pageSize`: 每页数量

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 10,
    "pages": 1,
    "current": 1,
    "size": 10,
    "records": [
      {
        "applyId": 123,
        "title": "前往客户公司拜访",
        "applicantName": "李四",
        "deptName": "技术部",
        "templateName": "部门内部用车审批流程",
        "currentNodeName": "部门经理审批",
        "startDate": "2024-01-15",
        "endDate": "2024-01-15",
        "destination": "北京市朝阳区xxx",
        "createTime": "2024-01-10T10:00:00",
        "waitTime": "2小时"
      }
    ]
  }
}
```

---

#### 4.2.2 审批同意

**接口地址**: `POST /apply/app/agree`

**权限要求**: 角色ID = 4

**请求参数**:
```json
{
  "applyId": 123,
  "comment": "同意用车"  // 审批意见（可选）
}
```

---

#### 4.2.3 审批驳回

**接口地址**: `POST /apply/app/reject`

**权限要求**: 角色ID = 4

**请求参数**:
```json
{
  "applyId": 123,
  "comment": "用车事由不充分，请补充详细说明"  // 驳回原因（必填）
}
```

---

#### 4.2.4 审批转审

**接口地址**: `POST /apply/app/transfer`

**权限要求**: 角色ID = 4

**请求参数**:
```json
{
  "applyId": 123,
  "transfereeId": 8,       // 转审对象ID（必填）
  "comment": "请王五代为审批"  // 转审原因（可选）
}
```

---

#### 4.2.5 获取审批历史

**接口地址**: `GET /apply/app/history/{applyId}`

**权限要求**: 已登录

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "nodeOrder": 1,
      "nodeName": "部门经理审批",
      "approverId": 2,
      "approverName": "张三",
      "action": "agree",
      "actionDesc": "同意",
      "comment": "同意用车",
      "operateTime": "2024-01-10T14:00:00"
    },
    {
      "nodeOrder": 2,
      "nodeName": "行政部审批",
      "approverId": 5,
      "approverName": "王五",
      "action": "pending",
      "actionDesc": "待审批",
      "comment": null,
      "operateTime": null
    }
  ]
}
```

---

## 5. 消息中心模块

### 5.1 获取我的消息列表

**接口地址**: `GET /msg/list`

**权限要求**: 已登录

**查询参数**:
- `isRead`: 是否已读（可选，0-未读，1-已读）
- `pageNum`: 页码
- `pageSize`: 每页数量

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 20,
    "pages": 2,
    "current": 1,
    "size": 10,
    "records": [
      {
        "messageId": 1,
        "title": "用车申请审批通过",
        "content": "您的用车申请【前往客户公司拜访】已审批通过",
        "type": 1,
        "typeDesc": "审批通过",
        "relatedId": 123,
        "isRead": 0,
        "createTime": "2024-01-10T14:00:00"
      }
    ]
  }
}
```

---

### 5.2 标记消息已读

**接口地址**: `PUT /msg/read/{messageId}`

**权限要求**: 已登录

---

### 5.3 全部标记已读

**接口地址**: `PUT /msg/read-all`

**权限要求**: 已登录

---

### 5.4 获取未读消息数量

**接口地址**: `GET /msg/unread-count`

**权限要求**: 已登录

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": 5
}
```

---

## 6. 文件上传模块

### 6.1 上传文件

**接口地址**: `POST /api/upload`

**权限要求**: 已登录

**请求参数**:
- `file`: MultipartFile（文件）

**支持的文件类型**:
- 图片: jpg, jpeg, png, gif
- 文档: pdf, doc, docx, xls, xlsx

**文件大小限制**: 10MB

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "https://vatest-1778746645609-1667.oss-cn-beijing.aliyuncs.com/a1b2c3d4_1715678901234.jpg"
}
```

---

## 7. 数据导出模块

### 7.1 导出用车申请数据

**接口地址**: `GET /api/export`

**权限要求**: 角色ID = 1 或 2

**查询参数**:
- `applicantName`: 申请人姓名（可选）
- `deptId`: 部门ID（可选）
- `status`: 审批状态（可选）
- `templateId`: 模板ID（可选）
- `startDate`: 开始日期（可选）
- `endDate`: 结束日期（可选）

**响应**: Excel文件下载

**文件名格式**: `用车申请数据_{时间戳}.xlsx`

---

## 📊 附录

### A. 角色ID对照表

| 角色ID | 角色名称 | 说明 |
|--------|---------|------|
| 1 | 超级管理员 | 拥有所有权限 |
| 2 | 用车管理员 | 管理用车申请、流程模板 |
| 3 | 普通员工 | 仅可发起用车申请 |
| 4 | 审批人 | 可审批用车申请 |

---

### B. 审批状态码

| 状态码 | 状态名称 | 说明 |
|--------|---------|------|
| 0 | 待提交 | 草稿状态 |
| 1 | 待审批 | 已提交，等待第一个节点审批 |
| 2 | 审批中 | 部分节点已通过 |
| 3 | 已通过 | 所有节点审批通过 |
| 4 | 已驳回 | 任意节点驳回 |
| 5 | 已撤销 | 申请人主动撤销 |

---

### C. 流程类型

| 类型标识 | 类型名称 | 说明 |
|---------|---------|------|
| internal | 部门内用车 | 本部门内部用车 |
| cross | 跨部门用车 | 涉及多个部门的用车 |
| long-distance | 长途用车 | 长途出行用车 |

---

### D. 车辆类型

| 类型标识 | 类型名称 | 适用场景 |
|---------|---------|---------|
| sedan | 轿车 | 1-4人短途出行 |
| business | 商务车 | 5-7人出行 |
| bus | 大巴 | 8人以上团体出行 |

---

### E. 审批规则

| 规则标识 | 规则名称 | 说明 |
|---------|---------|------|
| and | 会签 | 所有审批人都同意才算通过 |
| or | 或签 | 任意一人同意即可通过 |

---

### F. 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或Token失效 |
| 403 | 无权访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

