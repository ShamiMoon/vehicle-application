package com.baoying.vehicleapplication.controller;

import com.baoying.vehicleapplication.annotation.RequirePermission;
import com.baoying.vehicleapplication.common.Result;
import com.baoying.vehicleapplication.service.OssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器（使用阿里云OSS存储）
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileUploadController {
    
    private final OssService ossService;
    
    @Value("${app.upload.max-size:10485760}") // 默认10MB
    private long maxFileSize;
    
    /**
     * 上传文件到OSS（所有登录用户）
     * @param file 文件
     * @return 文件访问URL
     */
    @RequirePermission(checkRole = false)
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        
        // 检查文件大小
        if (file.getSize() > maxFileSize) {
            return Result.error("文件大小不能超过 " + (maxFileSize / 1024 / 1024) + "MB");
        }
        
        // 检查文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isValidFileType(originalFilename)) {
            return Result.error("不支持的文件类型");
        }
        
        try {
            // 上传文件到OSS
            String fileUrl = ossService.uploadFile(file);
            
            log.info("文件上传成功: {}, 大小: {} bytes", originalFilename, file.getSize());
            return Result.success(fileUrl);
            
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
    /**
     * 验证文件类型
     */
    private boolean isValidFileType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        String[] allowedTypes = {"jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "xls", "xlsx"};
        
        for (String type : allowedTypes) {
            if (type.equals(extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
