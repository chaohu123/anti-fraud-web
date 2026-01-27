package com.xxx.antifraud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxx.antifraud.common.BusinessException;
import com.xxx.antifraud.common.ErrorCode;
import com.xxx.antifraud.dto.user.UserLoginRequest;
import com.xxx.antifraud.dto.user.UserRegisterRequest;
import com.xxx.antifraud.entity.User;
import com.xxx.antifraud.mapper.UserMapper;
import com.xxx.antifraud.service.UserService;
import com.xxx.antifraud.vo.user.UserInfoVO;
import com.xxx.antifraud.vo.user.UserLoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户业务实现
 *
 * 说明：
 * - 登录不使用 JWT，只返回 userId（按需求简化）
 * - 密码使用 BCrypt 加密，避免明文存储（答辩更规范）
 * - 统一登录错误提示，防止用户枚举攻击
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public Long register(UserRegisterRequest request) {
        // 参数 trim 处理
        String nickname = StringUtils.hasText(request.getNickname()) 
            ? request.getNickname().trim() : null;
        String username = StringUtils.hasText(request.getUsername()) 
            ? request.getUsername().trim() : null;
        String password = StringUtils.hasText(request.getPassword()) 
            ? request.getPassword() : null;

        // 验证必填字段
        if (!StringUtils.hasText(nickname)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "昵称不能为空");
        }
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "用户名不能为空");
        }
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "密码不能为空");
        }

        // 验证用户名格式（字母、数字、下划线）
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "用户名只能包含字母、数字和下划线");
        }

        // 验证用户名长度
        if (username.length() < 3 || username.length() > 32) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "用户名长度需在 3~32 之间");
        }

        // 验证昵称长度
        if (nickname.length() < 2 || nickname.length() > 32) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "昵称长度需在 2~32 之间");
        }

        // 验证密码长度
        if (password.length() < 6 || password.length() > 32) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "密码长度需在 6~32 之间");
        }

        // 检查用户名是否已存在
        long count = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (count > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPassword(passwordEncoder.encode(password));
        user.setRiskLevel(0); // 默认低风险
        this.save(user);

        log.info("用户注册成功: username={}, nickname={}", username, nickname);
        return user.getId();
    }

    @Override
    public UserLoginVO login(UserLoginRequest request) {
        // 参数 trim 处理
        String username = StringUtils.hasText(request.getUsername()) 
            ? request.getUsername().trim() : null;
        String password = StringUtils.hasText(request.getPassword()) 
            ? request.getPassword() : null;

        // 验证必填字段
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "用户名不能为空");
        }
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "密码不能为空");
        }

        // 查询用户
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        // 统一错误提示，防止用户枚举攻击（不区分用户不存在和密码错误）
        if (user == null) {
            log.warn("登录失败: username={}, reason=用户不存在", username);
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT.getCode(), "用户名或密码错误");
        }

        // 验证密码
        String storedPasswordHash = user.getPassword();
        
        // 添加详细的调试日志
        if (storedPasswordHash == null || storedPasswordHash.isEmpty()) {
            log.error("密码哈希值为空: username={}", username);
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT.getCode(), "用户名或密码错误");
        }
        
        // 检查哈希值格式（BCrypt 哈希值应该以 $2a$, $2b$ 或 $2y$ 开头）
        if (!storedPasswordHash.startsWith("$2a$") && !storedPasswordHash.startsWith("$2b$") && !storedPasswordHash.startsWith("$2y$")) {
            log.error("密码哈希值格式不正确: username={}, hash={}", username, storedPasswordHash);
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT.getCode(), "用户名或密码错误");
        }
        
        boolean passwordMatches = passwordEncoder.matches(password, storedPasswordHash);
        
        log.info("密码验证详情: username={}, inputPasswordLength={}, storedHashPrefix={}, matches={}", 
                username, password != null ? password.length() : 0, 
                storedPasswordHash.length() > 10 ? storedPasswordHash.substring(0, 10) : storedPasswordHash, 
                passwordMatches);
        
        if (!passwordMatches) {
            log.warn("登录失败: username={}, reason=密码不匹配, storedHash={}", username, storedPasswordHash);
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT.getCode(), "用户名或密码错误");
        }

        log.info("用户登录成功: username={}, userId={}", username, user.getId());
        return new UserLoginVO(user.getId(), user.getUsername());
    }

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "用户ID不能为空");
        }

        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRiskLevel(user.getRiskLevel() != null ? user.getRiskLevel().toString() : "0");
        return vo;
    }

    /**
     * 更新用户头像
     */
    public void updateAvatar(Long userId, String avatarUrl) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "用户ID不能为空");
        }
        if (!StringUtils.hasText(avatarUrl)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "头像URL不能为空");
        }

        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setAvatar(avatarUrl);
        this.updateById(user);
        log.info("用户头像更新成功: userId={}", userId);
    }

    /**
     * 更新用户信息（昵称）
     */
    public void updateUserInfo(Long userId, String nickname) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "用户ID不能为空");
        }

        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (StringUtils.hasText(nickname)) {
            String trimmedNickname = nickname.trim();
            // 验证昵称长度
            if (trimmedNickname.length() < 2 || trimmedNickname.length() > 32) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "昵称长度需在 2~32 之间");
            }
            user.setNickname(trimmedNickname);
            this.updateById(user);
            log.info("用户信息更新成功: userId={}, nickname={}", userId, trimmedNickname);
        }
    }

    /**
     * 修改密码
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "用户ID不能为空");
        }
        if (!StringUtils.hasText(oldPassword)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "原密码不能为空");
        }
        if (!StringUtils.hasText(newPassword)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "新密码不能为空");
        }

        // 验证新密码长度
        if (newPassword.length() < 6 || newPassword.length() > 32) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "新密码长度需在 6~32 之间");
        }

        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 验证原密码
        String storedPasswordHash = user.getPassword();
        if (storedPasswordHash == null || storedPasswordHash.isEmpty()) {
            log.error("密码哈希值为空: userId={}", userId);
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT.getCode(), "原密码错误");
        }

        boolean passwordMatches = passwordEncoder.matches(oldPassword, storedPasswordHash);
        if (!passwordMatches) {
            log.warn("修改密码失败: userId={}, reason=原密码不匹配", userId);
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT.getCode(), "原密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
        log.info("用户密码修改成功: userId={}", userId);
    }
}

