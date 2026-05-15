package com.baoying.vehicleapplication.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * OSS文件存储服务接口
 */
public interface OssService {
    
    /**
     * 上传文件到OSS
     * @param file 文件
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file);
    
    /**
     * 删除OSS文件
     * @param fileUrl 文件URL
     */
    void deleteFile(String fileUrl);
}
