package com.baoying.vehicleapplication.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.ClientException;
import com.baoying.vehicleapplication.service.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * OSS文件存储服务实现
 */
@Slf4j
@Service
public class OssServiceImpl implements OssService {
    
    @Value("${aliyun.oss.endpoint}")
    private String endpoint;
    
    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;
    
    @Value("${aliyun.oss.region}")
    private String region;
    
    /**
     * 从环境变量获取AccessKey（安全方式）
     */
    private String getAccessKeyId() {
        return System.getenv("OSS_ACCESS_KEY_ID");
    }
    
    private String getAccessKeySecret() {
        return System.getenv("OSS_ACCESS_KEY_SECRET");
    }
    
    @Override
    public String uploadFile(MultipartFile file) {
        OSS ossClient = null;
        try {
            // 创建OSSClient实例
            ossClient = new OSSClientBuilder().build(endpoint, getAccessKeyId(), getAccessKeySecret());
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String fileName = generateFileName(extension);
            
            // 上传文件
            ossClient.putObject(bucketName, fileName, file.getInputStream());
            
            // 构建文件访问URL
            String fileUrl = "https://" + bucketName + "." + endpoint.replace("https://", "") + "/" + fileName;
            
            log.info("文件上传OSS成功: {}, 大小: {} bytes", fileName, file.getSize());
            return fileUrl;
            
        } catch (OSSException oe) {
            log.error("OSS服务端错误", oe);
            throw new RuntimeException("OSS服务异常: " + oe.getErrorMessage());
        } catch (ClientException ce) {
            log.error("OSS客户端错误", ce);
            throw new RuntimeException("OSS客户端异常: " + ce.getMessage());
        } catch (IOException e) {
            log.error("文件读取失败", e);
            throw new RuntimeException("文件读取失败: " + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
    
    @Override
    public void deleteFile(String fileUrl) {
        OSS ossClient = null;
        try {
            // 从URL中提取文件名
            String fileName = extractFileNameFromUrl(fileUrl);
            
            // 创建OSSClient实例
            ossClient = new OSSClientBuilder().build(endpoint, getAccessKeyId(), getAccessKeySecret());
            
            // 删除文件
            ossClient.deleteObject(bucketName, fileName);
            
            log.info("OSS文件删除成功: {}", fileName);
            
        } catch (Exception e) {
            log.error("OSS文件删除失败", e);
            throw new RuntimeException("文件删除失败: " + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    /**
     * 生成唯一文件名
     */
    private String generateFileName(String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        if (extension != null && !extension.isEmpty()) {
            return uuid + "_" + timestamp + "." + extension;
        }
        return uuid + "_" + timestamp;
    }
    
    /**
     * 从URL中提取文件名
     */
    private String extractFileNameFromUrl(String fileUrl) {
        // URL格式: https://bucket.endpoint/filename
        int lastSlashIndex = fileUrl.lastIndexOf("/");
        if (lastSlashIndex != -1) {
            return fileUrl.substring(lastSlashIndex + 1);
        }
        return fileUrl;
    }
}
