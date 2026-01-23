package com.xxx.antifraud.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码哈希生成工具
 * 
 * 用于生成正确的 BCrypt 密码哈希值，更新数据库中的测试数据
 */
public class PasswordHashGenerator {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 测试密码
        String password = "123456";
        
        // 生成新的哈希值
        String newHash = encoder.encode(password);
        System.out.println("密码: " + password);
        System.out.println("BCrypt 哈希值: " + newHash);
        System.out.println();
        System.out.println("验证生成的哈希值: " + encoder.matches(password, newHash));
        System.out.println();
        System.out.println("SQL 更新语句:");
        System.out.println("UPDATE af_user SET password_hash = '" + newHash + "' WHERE username = 'admin';");
    }
}
