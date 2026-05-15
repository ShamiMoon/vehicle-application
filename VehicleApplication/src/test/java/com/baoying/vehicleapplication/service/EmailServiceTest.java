package com.baoying.vehicleapplication.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 邮件服务测试类
 */
@Slf4j
@SpringBootTest
public class EmailServiceTest {
    
    @Autowired
    private EmailService emailService;
    
    /**
     * 测试发送审批通知邮件
     */
    @Test
    public void testSendApprovalEmail() {
        String toEmail = "1733904948@qq.com";  // 替换为测试邮箱
        String subject = "【测试】用车申请审批通知";
        String content = "<h1>测试邮件</h1><p>这是一封测试邮件，请忽略。</p>";
        
        emailService.sendApprovalEmail(toEmail, subject, content);
        
        log.info("测试邮件已发送至: {}", toEmail);
    }
    
    /**
     * 测试构建邮件内容
     */
    @Test
    public void testBuildEmailContent() {
        String htmlContent = emailService.buildApprovalEmailContent(
            "张三",
            "跨部门长途用车申请",
            "已通过",
            "同意申请，请注意安全驾驶",
            1L
        );
        
        System.out.println(htmlContent);
        log.info("邮件内容生成成功，长度: {}", htmlContent.length());
    }
}
