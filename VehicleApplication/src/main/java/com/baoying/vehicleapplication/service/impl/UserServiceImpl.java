package com.baoying.vehicleapplication.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baoying.vehicleapplication.common.BusinessException;
import com.baoying.vehicleapplication.common.PageHelper;
import com.baoying.vehicleapplication.dto.request.*;
import com.baoying.vehicleapplication.dto.response.LoginResponse;
import com.baoying.vehicleapplication.dto.response.UserInfoResponse;
import com.baoying.vehicleapplication.entity.SysDept;
import com.baoying.vehicleapplication.entity.SysDeptRole;
import com.baoying.vehicleapplication.entity.SysUser;
import com.baoying.vehicleapplication.mapper.DeptRoleMapper;
import com.baoying.vehicleapplication.mapper.RoleMapper;
import com.baoying.vehicleapplication.mapper.UserMapper;
import com.baoying.vehicleapplication.service.DeptService;
import com.baoying.vehicleapplication.service.MessageService;
import com.baoying.vehicleapplication.service.UserService;
import com.baoying.vehicleapplication.utils.PasswordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUser> implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DeptRoleMapper deptRoleMapper;

    @Lazy
    @Autowired
    private DeptService deptService;
    
    @Autowired(required = false)
    private com.baoying.vehicleapplication.service.EmailService emailService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private RoleMapper roleMapper;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;


    @Override
    public void createUser(UserCreateRequest request) {
        // 检查用户名是否已存在
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        if (request.getRoleId() != null) {
            checkRoleBelongsToDept(request.getRoleId(), request.getDeptId());
        }
        SysUser user = new SysUser();
        BeanUtils.copyProperties(request, user);

        // 密码加密存储
        user.setPassword(PasswordUtils.encode(request.getPassword()));

        // 默认值
        user.setStatus(1);
        user.setIsTempPassword(0);
        user.setTempPasswordExpire(null);

        this.save(user);
    }

    private void checkRoleBelongsToDept(Integer roleId, Integer deptId) {
        LambdaQueryWrapper<SysDeptRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDeptRole::getDeptId, deptId)
                .eq(SysDeptRole::getRoleId, roleId);
        if (deptRoleMapper.selectCount(wrapper) == 0) {
            throw new BusinessException("该角色不属于当前部门");
        }
    }

    @Override
    public void updateUser(UserUpdateRequest request) {
        SysUser user = this.getById(request.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        BeanUtils.copyProperties(request, user);
        this.updateById(user);
    }

    @Override
    public void deleteUser(Long id) {
        // TODO: 检查用户是否有关联的未完成用车申请
        this.removeById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        this.updateById(user);
    }

    @Override
    @Transactional
    public void resetPasswordByAdmin(UserResetPwdRequest request) {
        SysUser user = this.getById(request.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 1. 生成随机临时密码
        String tempPassword = PasswordUtils.generateTempPassword();

        // 2. 加密后存入数据库
        user.setPassword(PasswordUtils.encode(tempPassword));
        user.setIsTempPassword(1);
        user.setTempPasswordExpire(LocalDateTime.now().plusHours(24));  // 24小时有效

        this.updateById(user);

        log.info("用户 {} 密码已重置，临时密码: {}", user.getUsername(), tempPassword);

        // 3. 发送消息中心通知
        messageService.sendMessage(
            user.getId(),
            "密码已重置",
            "您的密码已被管理员重置，请使用临时密码登录并及时修改密码。临时密码有效期为24小时。",
            6,
            null
        );

        // 4. 发送邮件通知用户（如果用户开启了邮件通知）
        if (user.getEmailNotify() != null && user.getEmailNotify() == 1) {
            sendTempPasswordEmail(user.getEmail(), tempPassword, user.getRealname());
        } else {
            log.info("用户 {} 未开启邮件通知，请在控制台查看临时密码", user.getUsername());
            System.out.println("========== 临时密码 ==========");
            System.out.println("用户：" + user.getUsername());
            System.out.println("姓名：" + user.getRealname());
            System.out.println("临时密码：" + tempPassword);
            System.out.println("有效期：24小时");
            System.out.println("=============================");
        }
    }

    @Override
    @Transactional
    public void changePassword(UserChangePwdRequest request) {
        SysUser user = this.getById(request.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证旧密码（如果不是临时密码登录，需要验证旧密码）
        if (user.getIsTempPassword() == null || user.getIsTempPassword() == 0) {
            if (!PasswordUtils.matches(request.getOldPassword(), user.getPassword())) {
                throw new BusinessException("原密码错误");
            }
        }

        // 新密码不能与旧密码相同
        if (PasswordUtils.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }

        // 更新密码
        user.setPassword(PasswordUtils.encode(request.getNewPassword()));
        user.setIsTempPassword(0);           // 清除临时密码标记
        user.setTempPasswordExpire(null);    // 清除过期时间

        this.updateById(user);

        // 发送消息中心通知
        messageService.sendMessage(
            user.getId(),
            "密码已修改",
            "您的密码已于 " + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " 修改成功。",
            6,
            null
        );
    }

    @Override
    public void updateProfile(Long userId, UserProfileRequest request) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (request.getRealname() != null) {
            user.setRealname(request.getRealname());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getEmailNotify() != null) {
            user.setEmailNotify(request.getEmailNotify());
        }
        this.updateById(user);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BusinessException("请输入用户名");
        }
        if (request.getRealname() == null || request.getRealname().trim().isEmpty()) {
            throw new BusinessException("请输入真实姓名");
        }
        if ((request.getPhone() == null || request.getPhone().trim().isEmpty()) &&
            (request.getEmail() == null || request.getEmail().trim().isEmpty())) {
            throw new BusinessException("手机号和邮箱至少填写一个");
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername())
               .eq(SysUser::getRealname, request.getRealname());
        SysUser user = this.getOne(wrapper);
        if (user == null) {
            throw new BusinessException("用户信息验证失败，请检查输入");
        }

        boolean phoneMatch = request.getPhone() != null && !request.getPhone().trim().isEmpty()
            && request.getPhone().equals(user.getPhone());
        boolean emailMatch = request.getEmail() != null && !request.getEmail().trim().isEmpty()
            && request.getEmail().equals(user.getEmail());

        if (!phoneMatch && !emailMatch) {
            throw new BusinessException("用户信息验证失败，请检查输入");
        }

        LambdaQueryWrapper<SysUser> adminWrapper = new LambdaQueryWrapper<>();
        adminWrapper.eq(SysUser::getRoleId, 1);
        List<SysUser> admins = this.list(adminWrapper);
        if (admins.isEmpty()) {
            throw new BusinessException("暂无可处理的管理员，请联系系统管理员");
        }

        String title = "密码重置申请";
        String content = "用户【" + user.getRealname() + "】(" + user.getUsername() + ") 申请重置密码，请尽快处理。";

        for (SysUser admin : admins) {
            messageService.sendMessage(admin.getId(), title, content, 7, user.getId());
        }

        log.info("密码重置申请已通知管理员 - 用户: {}", user.getUsername());
    }

    @Override
    public UserInfoResponse getUserDetail(Long id) {
        SysUser user = this.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserInfoResponse response = new UserInfoResponse();
        BeanUtils.copyProperties(user, response);

        if (user.getDeptId() != null && deptService != null) {
            SysDept dept = deptService.getById(user.getDeptId());
            if (dept != null) {
                response.setDeptName(dept.getName());
            }
        }

        if (user.getRoleId() != null) {
            com.baoying.vehicleapplication.entity.SysRole role = roleMapper.selectById(user.getRoleId());
            if (role != null) {
                response.setRoleName(role.getName());
            }
        }

        // 查询数据权限范围
        if (user.getDeptId() != null && user.getRoleId() != null) {
            com.baoying.vehicleapplication.entity.SysDeptRole deptRole = deptRoleMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.baoying.vehicleapplication.entity.SysDeptRole>()
                    .eq(com.baoying.vehicleapplication.entity.SysDeptRole::getDeptId, user.getDeptId())
                    .eq(com.baoying.vehicleapplication.entity.SysDeptRole::getRoleId, user.getRoleId())
            );
            if (deptRole != null) {
                String scope = deptRole.getDataScope();
                if ("self".equals(scope)) response.setDataScope("仅本人");
                else if ("dept".equals(scope)) response.setDataScope("本部门");
                else if ("dept_and_sub".equals(scope)) response.setDataScope("本部门及下级");
                else if ("all".equals(scope)) response.setDataScope("全部");
                else response.setDataScope(scope);
            }
        }

        return response;
    }

    @Override
    public Page<UserInfoResponse> getUserList(UserQueryRequest request) {
        // 构建查询条件
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        
        // 1. 用户名模糊搜索
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            queryWrapper.like(SysUser::getUsername, request.getUsername());
        }
        
        // 2. 真实姓名模糊搜索
        if (request.getRealname() != null && !request.getRealname().trim().isEmpty()) {
            queryWrapper.like(SysUser::getRealname, request.getRealname());
        }
        
        // 3. 部门筛选
        if (request.getDeptId() != null) {
            queryWrapper.eq(SysUser::getDeptId, request.getDeptId());
        }
        
        // 4. 角色筛选
        if (request.getRoleId() != null) {
            queryWrapper.eq(SysUser::getRoleId, request.getRoleId());
        }
        
        // 5. 状态筛选
        if (request.getStatus() != null) {
            queryWrapper.eq(SysUser::getStatus, request.getStatus());
        }
        
        // 6. 按创建时间倒序
        queryWrapper.orderByDesc(SysUser::getCreateTime);
        
        // 使用PageHelper执行分页查询和转换
        return PageHelper.queryPage(
            this, 
            request.getPageNum(), 
            request.getPageSize(), 
            queryWrapper, 
            this::convertToUserInfoResponse
        );
    }
    
    /**
     * 将 SysUser 转换为 UserInfoResponse
     */
    private UserInfoResponse convertToUserInfoResponse(SysUser user) {
        UserInfoResponse response = new UserInfoResponse();
        BeanUtils.copyProperties(user, response);

        // 设置部门名称
        if (user.getDeptId() != null) {
            SysDept dept = deptService.getById(user.getDeptId());
            if (dept != null) {
                response.setDeptName(dept.getName());
            }
        }

        // 设置角色名称
        if (user.getRoleId() != null) {
            com.baoying.vehicleapplication.entity.SysRole role = this.getRoleById(user.getRoleId());
            if (role != null) {
                response.setRoleName(role.getName());
            }
        }

        return response;
    }

    /**
     * 根据角色ID查询角色
     */
    private com.baoying.vehicleapplication.entity.SysRole getRoleById(Integer roleId) {
        return roleMapper.selectById(roleId);
    }

    /**
     * 发送临时密码邮件
     */
    private void sendTempPasswordEmail(String email, String tempPassword, String realname) {
        if (emailService == null) {
            log.warn("邮件服务未启用，无法发送临时密码邮件");
            System.out.println("========== 临时密码 ==========");
            System.out.println("用户：" + realname);
            System.out.println("邮箱：" + email);
            System.out.println("临时密码：" + tempPassword);
            System.out.println("有效期：24小时");
            System.out.println("=============================");
            return;
        }
        
        if (email == null || email.trim().isEmpty()) {
            log.warn("用户 {} 未绑定邮箱，无法发送临时密码", realname);
            System.out.println("========== 临时密码 ==========");
            System.out.println("用户：" + realname);
            System.out.println("临时密码：" + tempPassword);
            System.out.println("注意：未绑定邮箱，请在控制台查看");
            System.out.println("=============================");
            return;
        }
        
        try {
            String subject = "【用车申请系统】密码重置通知";
            StringBuilder content = new StringBuilder();
            content.append("<!DOCTYPE html>");
            content.append("<html lang='zh-CN'>");
            content.append("<head><meta charset='UTF-8'></head>");
            content.append("<body style='font-family: Microsoft YaHei, Arial, sans-serif; line-height: 1.6; color: #333;'>");
            content.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");
            content.append("<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px 8px 0 0;'>");
            content.append("<h2>密码重置通知</h2>");
            content.append("</div>");
            content.append("<div style='background: #f9f9f9; padding: 20px; border-radius: 0 0 8px 8px;'>");
            content.append("<p>尊敬的 ").append(realname).append("：</p>");
            content.append("<p>您好！您的密码已被管理员重置，请使用以下临时密码登录系统：</p>");
            content.append("<div style='margin: 20px 0; padding: 15px; background: white; border-left: 4px solid #667eea; font-size: 18px; font-weight: bold; color: #667eea;'>");
            content.append(tempPassword);
            content.append("</div>");
            content.append("<p style='color: #ff4d4f;'><strong>重要提示：</strong></p>");
            content.append("<ul>");
            content.append("<li>此临时密码有效期为 <strong>24小时</strong></li>");
            content.append("<li>登录后请立即修改密码</li>");
            content.append("<li>请妥善保管您的密码，不要泄露给他人</li>");
            content.append("</ul>");
            content.append("<p style='text-align: center; margin-top: 30px;'>");
            content.append("<a href='").append(frontendUrl).append("/login' style='display: inline-block; padding: 12px 30px; background: #667eea; color: white; text-decoration: none; border-radius: 5px;'>");
            content.append("立即登录");
            content.append("</a>");
            content.append("</p>");
            content.append("<div style='margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd; font-size: 12px; color: #999;'>");
            content.append("<p>此邮件由系统自动发送，请勿直接回复。</p>");
            content.append("<p>如有疑问，请联系系统管理员。</p>");
            content.append("</div>");
            content.append("</div>");
            content.append("</div>");
            content.append("</body>");
            content.append("</html>");
            
            emailService.sendApprovalEmail(email, subject, content.toString());
            log.info("临时密码邮件已发送至: {}", email);
            
        } catch (Exception e) {
            log.error("发送临时密码邮件失败: {}", e.getMessage(), e);
            // 失败时在控制台输出
            System.out.println("========== 临时密码（邮件发送失败） ==========");
            System.out.println("用户：" + realname);
            System.out.println("邮箱：" + email);
            System.out.println("临时密码：" + tempPassword);
            System.out.println("错误：" + e.getMessage());
            System.out.println("===========================================");
        }
    }
}