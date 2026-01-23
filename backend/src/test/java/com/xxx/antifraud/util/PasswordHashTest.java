package com.xxx.antifraud.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码哈希测试工具
 * 
 * 用于验证和生成 BCrypt 密码哈希值
 */
public class PasswordHashTest {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 测试密码
        String password = "123456";
        
        // 数据库中的哈希值
        String storedHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        
        // 验证现有哈希值
        boolean matches = encoder.matches(password, storedHash);
        System.out.println("验证现有哈希值: " + matches);
        System.out.println("现有哈希值: " + storedHash);
        
        // 生成新的哈希值（用于更新数据库）
        String newHash = encoder.encode(password);
        System.out.println("\n生成的新哈希值: " + newHash);
        System.out.println("验证新哈希值: " + encoder.matches(password, newHash));
    }
}
