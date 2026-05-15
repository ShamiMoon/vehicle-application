package com.baoying.vehicleapplication.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 字符串转 Long 的自定义转换器
 * 处理前端传递 "null" 字符串的问题
 */
@Component
public class StringToLongConverter implements Converter<String, Long> {
    
    @Override
    public Long convert(String source) {
        // 如果字符串为空或 "null"，返回 null
        if (!StringUtils.hasText(source) || "null".equalsIgnoreCase(source.trim())) {
            return null;
        }
        
        try {
            return Long.valueOf(source);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的数字格式: " + source);
        }
    }
}
