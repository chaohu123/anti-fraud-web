-- 反诈信息识别与风险自测平台 - MySQL 建表脚本
-- 数据库版本：MySQL 8.x

CREATE TABLE IF NOT EXISTS af_user (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username      VARCHAR(64)  NOT NULL UNIQUE COMMENT '用户名',
    password      VARCHAR(128) NOT NULL COMMENT '登录密码（加密存储）',
    nickname      VARCHAR(64)  NULL COMMENT '昵称',
    gender        TINYINT      NULL COMMENT '性别：0-未知 1-男 2-女',
    age           INT          NULL COMMENT '年龄',
    risk_level    VARCHAR(16)  NULL COMMENT '当前风险等级：LOW/MEDIUM/HIGH',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-删除'
) COMMENT='用户表';

CREATE TABLE IF NOT EXISTS af_fraud_case (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    title           VARCHAR(128) NOT NULL COMMENT '案例标题',
    type            VARCHAR(32)  NOT NULL COMMENT '案例类型：SMS/EMAIL/WEB/CALL/OTHER',
    level           VARCHAR(16)  NOT NULL COMMENT '难度：EASY/MEDIUM/HARD',
    content         TEXT         NOT NULL COMMENT '案例正文内容',
    media_url       VARCHAR(512) NULL COMMENT '媒体资源URL（如截图）',
    hint            VARCHAR(255) NULL COMMENT '提示语 / 套路标签',
    suspicious_points TEXT       NULL COMMENT '可疑特征列表（JSON 数组字符串）',
    correct_answer  VARCHAR(16)  NOT NULL COMMENT '正确答案：FRAUD / SAFE',
    analysis        TEXT         NULL COMMENT '诈骗解析说明',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-删除'
) COMMENT='诈骗案例库';

CREATE TABLE IF NOT EXISTS af_training_record (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    case_id       BIGINT       NOT NULL COMMENT '案例ID',
    is_correct    TINYINT      NOT NULL COMMENT '是否判断正确：1-正确 0-错误',
    answer        VARCHAR(16)  NOT NULL COMMENT '用户选择的答案',
    time_spent_ms INT          NULL COMMENT '答题耗时（毫秒）',
    submitted_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-删除'
) COMMENT='识别训练记录';

CREATE TABLE IF NOT EXISTS af_risk_question (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    content     VARCHAR(255) NOT NULL COMMENT '题目内容',
    dimension   VARCHAR(32)  NOT NULL COMMENT '测评维度：INFO/FINANCE/PSYCH',
    weight      DECIMAL(5,2) NOT NULL DEFAULT 1.0 COMMENT '题目权重',
    question_type VARCHAR(16) NOT NULL COMMENT '题型：SINGLE/MULTI',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-删除'
) COMMENT='防骗风险测评题目';

CREATE TABLE IF NOT EXISTS af_risk_option (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    question_id  BIGINT       NOT NULL COMMENT '所属题目ID',
    label        VARCHAR(128) NOT NULL COMMENT '选项文案',
    option_value INT          NOT NULL COMMENT '风险分值（越高表示越高风险）',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-删除'
) COMMENT='防骗风险测评选项';

CREATE TABLE IF NOT EXISTS af_risk_assessment (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    total_score     DECIMAL(6,2) NOT NULL COMMENT '综合风险指数（0-100，越高越危险）',
    info_score      DECIMAL(6,2) NOT NULL COMMENT '信息保护意识得分（0-100）',
    finance_score   DECIMAL(6,2) NOT NULL COMMENT '金融安全意识得分（0-100）',
    psych_score     DECIMAL(6,2) NOT NULL COMMENT '心理风险倾向得分（0-100）',
    risk_level      VARCHAR(16)  NOT NULL COMMENT '风险等级：LOW/MEDIUM/HIGH',
    explanation     TEXT         NULL COMMENT '整体风险说明文本',
    suggestions     TEXT         NULL COMMENT '个性化防骗建议（JSON 数组或纯文本）',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-删除'
) COMMENT='防骗风险测评结果';

CREATE TABLE IF NOT EXISTS af_anti_fraud_article (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    category      VARCHAR(32)  NOT NULL COMMENT '知识类别：SMS/WEB/FINANCE/SOCIAL/OTHER',
    title         VARCHAR(128) NOT NULL COMMENT '知识标题',
    summary       VARCHAR(255) NULL COMMENT '摘要',
    content       TEXT         NOT NULL COMMENT '详细内容',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-删除'
) COMMENT='防骗知识库文章';

CREATE TABLE IF NOT EXISTS af_learning_record (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    article_id    BIGINT       NOT NULL COMMENT '知识文章ID',
    progress      INT          NOT NULL DEFAULT 100 COMMENT '学习进度百分比（0-100）',
    status        VARCHAR(16)  NOT NULL DEFAULT 'FINISHED' COMMENT '学习状态：READING/FINISHED',
    learned_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近学习时间',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-删除'
) COMMENT='防骗知识学习记录';

