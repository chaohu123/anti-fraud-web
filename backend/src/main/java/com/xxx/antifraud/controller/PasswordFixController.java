package com.xxx.antifraud.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxx.antifraud.common.ErrorCode;
import com.xxx.antifraud.common.Result;
import com.xxx.antifraud.entity.User;
import com.xxx.antifraud.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 密码修复工具 Controller（临时）
 * 
 * 用于修复数据库中的密码哈希值问题
 * 注意：此 Controller 仅用于开发/测试环境，生产环境应删除或禁用
 */
@Slf4j
@RestController
@RequestMapping("/api/tools/password")
@RequiredArgsConstructor
public class PasswordFixController {

    private final BCryptPasswordEncoder passwordEncoder;
    private final UserService userService;

    /**
     * 生成密码哈希值
     * 
     * @param password 明文密码
     * @return 生成的 BCrypt 哈希值
     */
    @GetMapping("/generate")
    public Result<Map<String, String>> generateHash(@RequestParam(defaultValue = "123456") String password) {
        String hash = passwordEncoder.encode(password);
        
        Map<String, String> result = new HashMap<>();
        result.put("password", password);
        result.put("hash", hash);
        result.put("verified", String.valueOf(passwordEncoder.matches(password, hash)));
        result.put("sql", "UPDATE af_user SET password_hash = '" + hash + "' WHERE username = 'admin';");
        
        log.info("生成密码哈希值: password={}, hash={}", password, hash);
        return Result.success(result);
    }

    /**
     * 验证密码哈希值
     * 
     * @param password 明文密码
     * @param hash BCrypt 哈希值
     * @return 验证结果
     */
    @GetMapping("/verify")
    public Result<Map<String, Object>> verifyHash(
            @RequestParam String password,
            @RequestParam String hash) {
        boolean matches = passwordEncoder.matches(password, hash);
        
        Map<String, Object> result = new HashMap<>();
        result.put("password", password);
        result.put("hash", hash);
        result.put("matches", matches);
        
        log.info("验证密码哈希值: password={}, matches={}", password, matches);
        return Result.success(result);
    }

    /**
     * 更新指定用户的密码（生成 SQL）
     * 
     * @param username 用户名
     * @param newPassword 新密码（明文）
     * @return 更新结果
     */
    @PostMapping("/update")
    public Result<String> updatePassword(
            @RequestParam String username,
            @RequestParam String newPassword) {
        String newHash = passwordEncoder.encode(newPassword);
        log.info("更新用户密码: username={}, newHash={}", username, newHash);
        
        return Result.success("请使用以下 SQL 更新数据库:\n" +
                "UPDATE af_user SET password_hash = '" + newHash + "' WHERE username = '" + username + "';");
    }

    /**
     * 直接修复指定用户的密码（更新数据库）
     * 
     * @param username 用户名
     * @param newPassword 新密码（明文，默认为 123456）
     * @return 更新结果
     */
    @PostMapping("/fix")
    public Result<Map<String, Object>> fixPassword(
            @RequestParam String username,
            @RequestParam(defaultValue = "123456") String newPassword) {
        // 查询用户
        User user = userService.getOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username));
        
        if (user == null) {
            return Result.failure(ErrorCode.USER_NOT_FOUND.getCode(), "用户不存在: " + username);
        }
        
        // 生成新的密码哈希值
        String newHash = passwordEncoder.encode(newPassword);
        
        // 验证新哈希值
        boolean verified = passwordEncoder.matches(newPassword, newHash);
        if (!verified) {
            log.error("生成的密码哈希值验证失败: username={}", username);
            return Result.failure(ErrorCode.SYSTEM_ERROR.getCode(), "生成的密码哈希值验证失败");
        }
        
        // 更新用户密码
        user.setPassword(newHash);
        boolean updated = userService.updateById(user);
        
        Map<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("updated", updated);
        result.put("newHash", newHash);
        result.put("verified", verified);
        
        if (updated) {
            log.info("密码修复成功: username={}, newHash={}", username, newHash);
            return Result.success(result);
        } else {
            log.error("密码修复失败: username={}", username);
            return Result.failure(ErrorCode.SYSTEM_ERROR.getCode(), "密码更新失败");
        }
    }
}
