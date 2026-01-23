-- 反诈信息识别与风险自测系统 数据库建表脚本（MySQL）
-- 建议库名：anti_fraud（可按需修改）
create database anti_fraud;

use anti_fraud;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 用户表
DROP TABLE IF EXISTS `af_user`;
CREATE TABLE `af_user` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`     VARCHAR(50)  NOT NULL COMMENT '登录名（唯一）',
  `password_hash`VARCHAR(255) NOT NULL COMMENT '加密密码',
  `nickname`     VARCHAR(50)  NULL     COMMENT '昵称',
  `phone`        VARCHAR(20)  NULL     COMMENT '手机号',
  `email`        VARCHAR(100) NULL     COMMENT '邮箱',
  `risk_level`   TINYINT      NOT NULL DEFAULT 0 COMMENT '当前风险等级：0低/1中/2高',
  `last_score`   INT          NULL     COMMENT '最近一次测评分数',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0正常 1删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 诈骗案例表
DROP TABLE IF EXISTS `af_fraud_case`;
CREATE TABLE `af_fraud_case` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '案例ID',
  `type`            VARCHAR(20)  NOT NULL COMMENT '案例类型：sms/email/audio/site',
  `title`           VARCHAR(100) NOT NULL COMMENT '案例标题',
  `content`         TEXT         NOT NULL COMMENT '案例正文',
  `hint`            VARCHAR(255) NULL     COMMENT '套路提示',
  `suspicious_tags` JSON         NULL     COMMENT '可疑特征列表(JSON数组)',
  `level`           VARCHAR(20)  NOT NULL COMMENT '难度：easy/medium/hard',
  `media_url`       VARCHAR(255) NULL     COMMENT '媒体资源地址',
  `answer`          VARCHAR(20)  NOT NULL COMMENT '标准答案：fraud/safe',
  `enable_flag`     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用 1启用 0停用',
  `created_by`      BIGINT       NULL     COMMENT '创建人ID（管理员）',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_case_type_level` (`type`,`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='诈骗案例表';

-- 训练记录表
DROP TABLE IF EXISTS `af_training_record`;
CREATE TABLE `af_training_record` (
  `id`               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '训练记录ID',
  `user_id`          BIGINT      NOT NULL COMMENT '用户ID',
  `case_id`          BIGINT      NOT NULL COMMENT '案例ID',
  `user_judgement`   VARCHAR(20) NOT NULL COMMENT '用户判断：fraud/safe',
  `correct`          TINYINT     NOT NULL COMMENT '是否判断正确 1正确 0错误',
  `selected_tags`    JSON        NULL     COMMENT '用户选择的可疑特征(JSON数组)',
  `duration_seconds` INT         NULL     COMMENT '答题时长(秒)',
  `answered_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作答时间',
  `deleted`          TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除 0正常 1删除',
  PRIMARY KEY (`id`),
  KEY `idx_training_user` (`user_id`),
  KEY `idx_training_case` (`case_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练记录表';

-- 风险测评题目表
DROP TABLE IF EXISTS `af_risk_question`;
CREATE TABLE `af_risk_question` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '题目ID',
  `content`      VARCHAR(255)  NOT NULL COMMENT '题目内容',
  `dimension`    VARCHAR(20)   NOT NULL COMMENT '维度：INFO/FINANCE/PSYCH',
  `weight`       DECIMAL(4,2)  NOT NULL DEFAULT 1.00 COMMENT '题目权重',
  `question_type` VARCHAR(20)   NOT NULL COMMENT '题型：SINGLE/MULTI',
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0正常 1删除',
  PRIMARY KEY (`id`),
  KEY `idx_question_dimension` (`dimension`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险测评题目表';

-- 风险测评选项表
DROP TABLE IF EXISTS `af_risk_option`;
CREATE TABLE `af_risk_option` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '选项ID',
  `question_id`  BIGINT        NOT NULL COMMENT '所属题目ID',
  `label`        VARCHAR(128)  NOT NULL COMMENT '选项文案',
  `option_value` INT           NOT NULL COMMENT '风险分值（越高表示越高风险）',
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0正常 1删除',
  PRIMARY KEY (`id`),
  KEY `idx_option_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险测评选项表';

-- 测评结果表
DROP TABLE IF EXISTS `af_assessment_result`;
CREATE TABLE `af_assessment_result` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '测评结果ID',
  `user_id`         BIGINT      NOT NULL COMMENT '用户ID',
  `total_score`     INT         NOT NULL COMMENT '综合风险指数(0-100)',
  `risk_level`      TINYINT     NOT NULL COMMENT '风险等级：0低/1中/2高',
  `info_score`      INT         NOT NULL COMMENT '信息防护分(0-100)',
  `finance_score`   INT         NOT NULL COMMENT '金融安全分(0-100)',
  `psych_score`     INT         NOT NULL COMMENT '心理倾向分(0-100)',
  `raw_detail_json` JSON        NULL     COMMENT '原始计算细节(JSON)',
  `suggestions_json`JSON        NULL     COMMENT '建议列表(JSON)',
  `created_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '测评时间',
  `deleted`         TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除 0正常 1删除',
  PRIMARY KEY (`id`),
  KEY `idx_assessment_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测评结果表';

-- 防骗知识表
DROP TABLE IF EXISTS `af_knowledge`;
CREATE TABLE `af_knowledge` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '知识ID',
  `category`    VARCHAR(50)  NOT NULL COMMENT '分类：短信/电话/网站/社交等',
  `title`       VARCHAR(100) NOT NULL COMMENT '标题',
  `summary`     VARCHAR(255) NOT NULL COMMENT '摘要',
  `content`     TEXT         NULL     COMMENT '详细内容',
  `difficulty`  TINYINT      NOT NULL DEFAULT 1 COMMENT '重要程度/难度(1-5)',
  `tags`        VARCHAR(255) NULL     COMMENT '标签，逗号分隔',
  `enable_flag` TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0正常 1删除',
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='防骗知识表';

-- 学习记录表
DROP TABLE IF EXISTS `af_learning_record`;
CREATE TABLE `af_learning_record` (
  `id`             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '学习记录ID',
  `user_id`        BIGINT      NOT NULL COMMENT '用户ID',
  `knowledge_id`   BIGINT      NOT NULL COMMENT '知识ID',
  `progress`       INT         NOT NULL DEFAULT 0 COMMENT '进度百分比(0-100)',
  `status`         TINYINT     NOT NULL DEFAULT 0 COMMENT '状态：0未开始/1学习中/2已完成',
  `starred`        TINYINT     NOT NULL DEFAULT 0 COMMENT '是否收藏',
  `last_viewed_at` DATETIME    NULL     COMMENT '最近查看时间',
  `completed_at`   DATETIME    NULL     COMMENT '完成时间',
  `deleted`        TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除 0正常 1删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_learning_user_knowledge` (`user_id`,`knowledge_id`),
  KEY `idx_learning_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习记录表';

SET FOREIGN_KEY_CHECKS = 1;

