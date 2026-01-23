package com.xxx.antifraud.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 诈骗案例实体
 *
 * 对应表：af_fraud_case（匹配 schema_mysql.sql 中的表结构）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("af_fraud_case")
public class FraudCase extends BaseEntity {

    /**
     * 案例标题（可供列表显示）
     */
    private String title;

    /**
     * 案例类型：sms/email/audio/site
     */
    private String type;

    /**
     * 难度：easy/medium/hard
     */
    private String level;

    /**
     * 案例正文内容
     */
    private String content;

    /**
     * 可选媒体资源 URL（如网站截图等）
     * 数据库字段名：media_url
     */
    @TableField("media_url")
    private String mediaUrl;

    /**
     * 简要套路提示
     */
    private String hint;

    /**
     * 可疑特征列表（JSON 数组）
     * 数据库字段名：suspicious_tags
     */
    @TableField("suspicious_tags")
    private String suspiciousTags;

    /**
     * 正确答案：fraud/safe
     * 数据库字段名：answer
     */
    @TableField("answer")
    private String correctAnswer;

    /**
     * 是否启用：1启用 0停用
     * 数据库字段名：enable_flag
     */
    @TableField("enable_flag")
    private Integer enableFlag;

    /**
     * 创建人ID（管理员）
     * 数据库字段名：created_by
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * 获取可疑特征列表（兼容旧代码）
     * 注意：实际数据库字段是 suspicious_tags，这里提供 getter 方法兼容
     */
    public String getSuspiciousPoints() {
        return this.suspiciousTags;
    }

    /**
     * 设置可疑特征列表（兼容旧代码）
     */
    public void setSuspiciousPoints(String suspiciousPoints) {
        this.suspiciousTags = suspiciousPoints;
    }

    /**
     * 诈骗解析说明（兼容旧代码，实际表中没有此字段）
     * 可以从 hint 或其他字段推导
     */
    public String getAnalysis() {
        return "解析要点：" + (this.hint != null ? this.hint : "");
    }
}

