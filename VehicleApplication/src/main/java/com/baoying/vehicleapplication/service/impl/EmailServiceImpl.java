package com.baoying.vehicleapplication.service.impl;

import com.baoying.vehicleapplication.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

/**
 * 邮件服务实现
 * 使用Spring Mail集成SMTP邮件发送
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {
    
    @Value("${app.email.enabled:false}")
    private Boolean emailEnabled;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    @Value("${spring.mail.username:}")
    private String fromEmail;
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Override
    public void sendApprovalEmail(String toEmail, String subject, String content) {
        if (!emailEnabled) {
            log.debug("邮件功能未启用，跳过发送邮件。收件人: {}, 主题: {}", toEmail, subject);
            return;
        }
        
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("收件人邮箱为空，跳过发送邮件");
            return;
        }
        
        if (mailSender == null) {
            log.warn("邮件发送器未配置，请检查spring.mail配置");
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);  // true表示HTML格式
            
            mailSender.send(message);
            
            log.info("邮件发送成功 - 收件人: {}, 主题: {}", toEmail, subject);
            
        } catch (Exception e) {
            log.error("邮件发送失败 - 收件人: {}, 主题: {}, 错误: {}", toEmail, subject, e.getMessage(), e);
        }
    }
    
    @Override
    public String buildApprovalEmailContent(String userName, String applyTitle, String status, 
                                           String opinion, Long applyId) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='zh-CN'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: 'Microsoft YaHei', Arial, sans-serif; line-height: 1.6; color: #333; }");
        html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px 8px 0 0; }");
        html.append(".content { background: #f9f9f9; padding: 20px; border-radius: 0 0 8px 8px; }");
        html.append(".info-item { margin: 10px 0; padding: 10px; background: white; border-left: 4px solid #667eea; }");
        html.append(".label { font-weight: bold; color: #667eea; }");
        html.append(".button { display: inline-block; padding: 12px 30px; margin: 20px 0; background: #667eea; color: white; text-decoration: none; border-radius: 5px; }");
        html.append(".footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd; font-size: 12px; color: #999; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h2>用车申请审批通知</h2>");
        html.append("</div>");
        html.append("<div class='content'>");
        html.append("<p>尊敬的 ").append(userName).append("：</p>");
        html.append("<p>您好！您有一笔用车申请的状态已更新，详情如下：</p>");
        
        html.append("<div class='info-item'>");
        html.append("<span class='label'>申请标题：</span>").append(applyTitle).append("<br/>");
        html.append("</div>");
        
        html.append("<div class='info-item'>");
        html.append("<span class='label'>审批状态：</span><strong style='color: ").append(getStatusColor(status)).append("'>").append(status).append("</strong><br/>");
        html.append("</div>");
        
        if (opinion != null && !opinion.trim().isEmpty()) {
            html.append("<div class='info-item'>");
            html.append("<span class='label'>审批意见：</span>").append(opinion).append("<br/>");
            html.append("</div>");
        }
        
        html.append("<div class='info-item'>");
        html.append("<span class='label'>操作时间：</span>").append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("<br/>");
        html.append("</div>");
        
        html.append("<p style='text-align: center;'>");
        html.append("<a href='").append(frontendUrl).append("/apply/detail/").append(applyId).append("' class='button'>");
        html.append("查看详情");
        html.append("</a>");
        html.append("</p>");
        
        html.append("<div class='footer'>");
        html.append("<p>此邮件由系统自动发送，请勿直接回复。</p>");
        html.append("<p>如有疑问，请联系系统管理员。</p>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
    
    /**
     * 根据状态获取颜色
     */
    private String getStatusColor(String status) {
        if (status == null) {
            return "#333";
        }
        if (status.contains("通过")) {
            return "#52c41a";  // 绿色
        } else if (status.contains("驳回")) {
            return "#ff4d4f";  // 红色
        } else if (status.contains("待审批") || status.contains("转审")) {
            return "#1890ff";  // 蓝色
        } else if (status.contains("超时")) {
            return "#faad14";  // 橙色
        }
        return "#333";
    }
}
