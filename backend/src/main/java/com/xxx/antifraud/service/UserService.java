package com.xxx.antifraud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxx.antifraud.dto.user.UserLoginRequest;
import com.xxx.antifraud.dto.user.UserRegisterRequest;
import com.xxx.antifraud.entity.User;
import com.xxx.antifraud.vo.user.UserInfoVO;
import com.xxx.antifraud.vo.user.UserLoginVO;

/**
 * 用户业务 Service
 *
 * 简化版：注册、登录、查询基础信息。
 */
public interface UserService extends IService<User> {

    Long register(UserRegisterRequest request);

    UserLoginVO login(UserLoginRequest request);

    UserInfoVO getUserInfo(Long userId);

    void updateAvatar(Long userId, String avatarUrl);

    void updateUserInfo(Long userId, String nickname);

    void changePassword(Long userId, String oldPassword, String newPassword);
}

