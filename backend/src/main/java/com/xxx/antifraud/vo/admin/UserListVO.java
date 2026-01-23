package com.xxx.antifraud.vo.admin;

import com.xxx.antifraud.entity.User;
import lombok.Data;

/**
 * 用户列表 VO（包含前端需要的额外字段）
 */
@Data
public class UserListVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private Integer level; // 等级（默认 1）
    private Integer exp; // 经验值（默认 0）
    private String riskLevel; // 风险等级：LOW/MEDIUM/HIGH
    private String createdAt; // 注册时间

    public static UserListVO from(User user) {
        UserListVO vo = new UserListVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setLevel(1); // 默认等级
        vo.setExp(0); // 默认经验值
        vo.setRiskLevel(user.getRiskLevel() != null ? 
                switch (user.getRiskLevel()) {
                    case 0 -> "LOW";
                    case 1 -> "MEDIUM";
                    case 2 -> "HIGH";
                    default -> "LOW";
                } : "LOW");
        if (user.getCreatedAt() != null) {
            vo.setCreatedAt(user.getCreatedAt().toString());
        }
        return vo;
    }
}
