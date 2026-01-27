package com.xxx.antifraud.controller;

import com.xxx.antifraud.common.Result;
import com.xxx.antifraud.dto.user.UserLoginRequest;
import com.xxx.antifraud.dto.user.UserRegisterRequest;
import com.xxx.antifraud.service.UserService;
import com.xxx.antifraud.vo.user.UserInfoVO;
import com.xxx.antifraud.vo.user.UserLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户模块 Controller（简化版）
 */
@Tag(name = "用户模块")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户注册（用户名 + 密码）")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody UserRegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @Operation(summary = "用户登录（返回 userId）")
    @PostMapping("/login")
    public Result<UserLoginVO> login(@Valid @RequestBody UserLoginRequest request) {
        return Result.success(userService.login(request));
    }

    @Operation(summary = "查询用户基础信息")
    @GetMapping("/{userId}")
    public Result<UserInfoVO> getUserInfo(@PathVariable Long userId) {
        return Result.success(userService.getUserInfo(userId));
    }

    @Operation(summary = "更新用户头像")
    @PutMapping("/{userId}/avatar")
    public Result<Void> updateAvatar(@PathVariable Long userId, @RequestBody Map<String, String> data) {
        String avatarUrl = data.get("avatar");
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return Result.failure(400, "头像URL不能为空");
        }
        userService.updateAvatar(userId, avatarUrl);
        return Result.success();
    }

    @Operation(summary = "更新用户信息（昵称等）")
    @PutMapping("/{userId}/info")
    public Result<Void> updateUserInfo(@PathVariable Long userId, @RequestBody Map<String, String> data) {
        String nickname = data.get("nickname");
        userService.updateUserInfo(userId, nickname);
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/{userId}/password")
    public Result<Void> changePassword(@PathVariable Long userId, @RequestBody Map<String, String> data) {
        String oldPassword = data.get("oldPassword");
        String newPassword = data.get("newPassword");
        if (oldPassword == null || oldPassword.isEmpty()) {
            return Result.failure(400, "原密码不能为空");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return Result.failure(400, "新密码不能为空");
        }
        userService.changePassword(userId, oldPassword, newPassword);
        return Result.success();
    }
}

