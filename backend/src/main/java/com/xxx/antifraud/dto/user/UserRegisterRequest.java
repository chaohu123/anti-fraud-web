package com.xxx.antifraud.dto.user;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求 DTO
 */
@Data
public class UserRegisterRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(min = 2, max = 32, message = "昵称长度需在 2~32 之间")
    private String nickname;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度需在 3~32 之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在 6~32 之间")
    private String password;
}

