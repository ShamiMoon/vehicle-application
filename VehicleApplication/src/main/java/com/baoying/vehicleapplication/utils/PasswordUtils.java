package com.baoying.vehicleapplication.utils;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.util.Random;

@Component
public class PasswordUtils {
    
    private static final Random RANDOM = new SecureRandom();
    
    // 字符集：大小写字母 + 数字
    private static final String CHAR_SET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    
    /**
     * 生成随机临时密码（8位）
     */
    public static String generateTempPassword() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(CHAR_SET.charAt(RANDOM.nextInt(CHAR_SET.length())));
        }
        return sb.toString();
    }

    public static String encode(String rawPassword) {
        //使用 MD5
        return org.springframework.util.DigestUtils.md5DigestAsHex(rawPassword.getBytes());
    }

    public static String decode(String encodedPassword) {
        //使用 MD5
        return new String(org.springframework.util.DigestUtils.md5Digest(encodedPassword.getBytes()));
    }

    /**
     * 密码匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword);
    }
}