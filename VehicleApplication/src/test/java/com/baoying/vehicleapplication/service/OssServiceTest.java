package com.baoying.vehicleapplication.service;

import com.aliyun.oss.*;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.common.auth.*;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyuncs.exceptions.ClientException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OSS服务测试
 */
@SpringBootTest
public class OssServiceTest {
    
    @Autowired
    private OssService ossService;

    @Test
    public void createBucket() throws com.aliyuncs.exceptions.ClientException {
        // 创建bucket
        String endpoint = "https://oss-cn-beijing.aliyuncs.com";
        String bucketName = "vehicle";
        String region = "cn-beijing";
        EnvironmentVariableCredentialsProvider credentialsProvider =
                CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();

        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .region(region)
                .build();
        try {
            ossClient.createBucket(bucketName);
            System.out.println("1. Bucket " + bucketName + " 创建成功。");
        }catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

    }
    @Test
    public void testUploadFile() {
        try {
            // 创建模拟文件
            MultipartFile file = new MockMultipartFile(
                "test.jpg",
                "test.jpg",
                "image/jpeg",
                "Hello OSS".getBytes()
            );
            
            // 上传文件
            String fileUrl = ossService.uploadFile(file);
            
            // 验证返回的URL不为空
            assertNotNull(fileUrl, "文件URL不应为空");
            assertTrue(fileUrl.contains("oss-cn-beijing.aliyuncs.com"), 
                "URL应包含OSS域名");
            
            System.out.println("文件上传成功，URL: " + fileUrl);
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("文件上传失败: " + e.getMessage());
        }
    }
}
