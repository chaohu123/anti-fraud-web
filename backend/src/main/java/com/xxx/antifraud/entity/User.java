package com.xxx.antifraud.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体（简化版）
 *
 * 仅包含注册/登录所需字段，以及论文可扩展的基础信息字段。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("af_user")
public class User extends BaseEntity {

    private String username;

    /**
     * 加密后的密码（BCrypt）
     * 数据库字段名：password_hash
     */
    @TableField("password_hash")
    private String password;

    private String nickname;

    private String phone;

    private String email;

    /**
     * 当前风险等级：0=低风险, 1=中风险, 2=高风险
     * 数据库字段名：risk_level
     */
    @TableField("risk_level")
    private Integer riskLevel;

    /**
     * 最近一次测评分数
     * 数据库字段名：last_score
     */
    @TableField("last_score")
    private Integer lastScore;

    /**
     * 用户头像URL（base64或URL）
     * 数据库字段名：avatar_url
     */
    @TableField("avatar_url")
    private String avatar;
}

