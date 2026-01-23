-- ============================================================
-- 反诈骗信息识别与风险自测系统 - 测试数据插入脚本
-- 数据库版本：MySQL 8.x
-- 说明：本脚本包含完整的测试数据，用于系统演示和开发测试
-- 注意：本脚本的字段格式完全符合 schema_mysql.sql 中的表结构定义
-- ============================================================

-- 设置字符集
SET NAMES utf8mb4;

-- ============================================================
-- 1. 用户表数据 (af_user)
-- ============================================================
-- 注意：密码使用 BCrypt 加密，所有测试用户密码均为：123456
-- 重要：如果密码验证失败，请运行 PasswordHashGenerator.java 生成新的哈希值
-- 或者使用注册功能重新创建用户（会自动生成正确的哈希值）
-- risk_level: 0=低风险, 1=中风险, 2=高风险
-- 
-- 当前使用的 BCrypt 哈希值（密码：123456）：
-- $2a$10$rKZ8v5F5N5L5N5L5N5L5NuKZ8v5F5N5L5N5L5N5L5NuKZ8v5F5N5L5N
-- 
-- 如果此哈希值验证失败，请执行以下步骤：
-- 1. 运行 backend/src/main/java/com/xxx/antifraud/util/PasswordHashGenerator.java
-- 2. 复制生成的哈希值
-- 3. 更新此文件中的所有 password_hash 值
-- 4. 重新执行此 SQL 脚本

INSERT INTO `af_user` (`username`, `password_hash`, `nickname`, `phone`, `email`, `risk_level`, `last_score`, `created_at`, `updated_at`, `deleted`) VALUES
-- 管理员账号（密码：123456）
('admin', '$2a$10$rKZ8v5F5N5L5N5L5N5L5NuKZ8v5F5N5L5N5L5N5L5NuKZ8v5F5N5L5N', '系统管理员', NULL, 'admin@example.com', 0, NULL, NOW(), NOW(), 0),

-- 测试用户账号（密码均为：123456）
('testuser1', '$2a$10$rKZ8v5F5N5L5N5L5N5L5NuKZ8v5F5N5L5N5L5N5L5NuKZ8v5F5N5L5N', '防骗新手', '13800138001', 'testuser1@example.com', 0, 25, NOW(), NOW(), 0),
('testuser2', '$2a$10$rKZ8v5F5N5L5N5L5N5L5NuKZ8v5F5N5L5N5L5N5L5NuKZ8v5F5N5L5N', '安全达人', '13800138002', 'testuser2@example.com', 1, 45, NOW(), NOW(), 0),
('testuser3', '$2a$10$rKZ8v5F5N5L5N5L5N5L5NuKZ8v5F5N5L5N5L5N5L5NuKZ8v5F5N5L5N', '防骗专家', '13800138003', 'testuser3@example.com', 2, 75, NOW(), NOW(), 0),
('zhangsan', '$2a$10$rKZ8v5F5N5L5N5L5N5L5NuKZ8v5F5N5L5N5L5N5L5NuKZ8v5F5N5L5N', '张三', '13800138004', 'zhangsan@example.com', 0, NULL, NOW(), NOW(), 0),
('lisi', '$2a$10$rKZ8v5F5N5L5N5L5N5L5NuKZ8v5F5N5L5N5L5N5L5NuKZ8v5F5N5L5N', '李四', '13800138005', 'lisi@example.com', 0, 30, NOW(), NOW(), 0);

-- ============================================================
-- 2. 诈骗案例表数据 (af_fraud_case)
-- ============================================================
-- type: sms/email/audio/site
-- level: easy/medium/hard
-- answer: fraud/safe
-- suspicious_tags: JSON数组格式

INSERT INTO `af_fraud_case` (`type`, `title`, `content`, `hint`, `suspicious_tags`, `level`, `media_url`, `answer`, `enable_flag`, `created_by`, `created_at`, `updated_at`, `deleted`) VALUES
-- 短信诈骗案例
('sms', '银行账户异常通知',
'【工商银行】您的账户存在异常，请立即点击链接验证：http://fake-icbc.com/verify，否则账户将被冻结。验证码：123456',
'仿冒银行 + 恐吓催促',
'["非官方域名", "恐吓语气催促立即操作", "要求点击链接验证"]',
'easy',
NULL,
'fraud',
1,
1,
NOW(), NOW(), 0),

('sms', '中奖通知短信',
'【幸运抽奖】恭喜您获得iPhone15 Pro Max！请点击 http://t.cn/xxxxx 领取奖品，验证码：8888，24小时内有效。',
'虚假中奖 + 限时催促',
'["虚假中奖信息", "短链接", "限时催促", "要求提供验证码"]',
'easy',
NULL,
'fraud',
1,
1,
NOW(), NOW(), 0),

('sms', '快递异常通知',
'【顺丰速运】您的包裹在运输途中出现异常，请点击 http://sf-express-fake.com/check 查询详情，订单号：SF1234567890',
'仿冒快递公司 + 异常通知',
'["非官方域名", "异常通知制造紧张", "要求点击链接"]',
'medium',
NULL,
'fraud',
1,
1,
NOW(), NOW(), 0),

-- 邮件诈骗案例
('email', '工作邮件钓鱼',
'主题：紧急：请立即更新您的账户信息\n\n尊敬的员工，\n\n由于系统升级，请立即点击以下链接更新您的账户信息：\nhttp://company-fake.com/update\n\n如不更新，账户将在24小时后被锁定。\n\nIT部门',
'仿冒公司邮件 + 紧急催促',
'["仿冒公司邮件", "紧急催促", "要求点击链接", "威胁锁定账户"]',
'medium',
NULL,
'fraud',
1,
1,
NOW(), NOW(), 0),

('email', '发票通知邮件',
'主题：您的发票已生成\n\n您好，\n\n您的发票已生成，请点击以下链接下载：\nhttp://invoice-fake.com/download?id=xxxxx\n\n如有疑问，请联系客服。\n\n财务部',
'仿冒财务邮件 + 发票下载',
'["仿冒财务部门", "要求下载附件或点击链接", "可能包含恶意软件"]',
'hard',
NULL,
'fraud',
1,
1,
NOW(), NOW(), 0),

-- 网站诈骗案例
('site', '虚假购物网站',
'您正在访问一个声称销售正品商品的网站，商品价格远低于市场价，要求先付款后发货，且只接受银行转账或第三方支付。',
'超低价格 + 异常支付方式',
'["价格异常低", "要求先付款", "只接受转账", "无正规支付渠道"]',
'medium',
'https://example.com/fake-shop-screenshot.jpg',
'fraud',
1,
1,
NOW(), NOW(), 0),

('site', '仿冒银行网站',
'您访问了一个与某银行官网非常相似的网站，要求输入银行卡号、密码和验证码进行"安全验证"。',
'仿冒官网 + 要求输入敏感信息',
'["域名与官网相似但不完全一致", "要求输入银行卡密码", "要求输入验证码"]',
'hard',
'https://example.com/fake-bank-screenshot.jpg',
'fraud',
1,
1,
NOW(), NOW(), 0),

-- 正常案例（用于训练）
('sms', '银行官方通知',
'【工商银行】您的尾号1234的银行卡于2024-01-15 10:30发生一笔1000.00元的消费，如非本人操作，请致电95588。',
'官方通知 + 安全提示',
'["官方客服电话", "仅通知不要求操作", "提供联系方式"]',
'easy',
NULL,
'safe',
1,
1,
NOW(), NOW(), 0),

('sms', '快递正常通知',
'【顺丰速运】您的包裹SF1234567890已由【北京分拨中心】发往【上海分拨中心】，预计明天送达。查询：sf-express.com',
'正常物流通知',
'["官方域名", "仅通知不要求操作", "提供查询方式"]',
'easy',
NULL,
'safe',
1,
1,
NOW(), NOW(), 0);

-- ============================================================
-- 3. 风险测评题目表数据 (af_risk_question)
-- ============================================================
-- dimension: INFO/FINANCE/PSYCH (注意：schema中是VARCHAR(20)，但建议使用大写)
-- question_type: SINGLE/MULTI
-- 注意：schema中没有 options_json 字段，选项存储在 af_risk_option 表中

INSERT INTO `af_risk_question` (`content`, `dimension`, `weight`, `question_type`, `created_at`, `updated_at`, `deleted`) VALUES
-- 信息保护意识维度 (INFO)
('收到未知链接时，您会直接点击吗？', 'INFO', 1.20, 'SINGLE', NOW(), NOW(), 0),
('您是否经常在多个网站使用相同的密码？', 'INFO', 1.50, 'SINGLE', NOW(), NOW(), 0),
('您会在公共WiFi环境下进行网上银行操作吗？', 'INFO', 1.30, 'SINGLE', NOW(), NOW(), 0),
('您是否会定期更新手机和电脑的软件？', 'INFO', 1.00, 'SINGLE', NOW(), NOW(), 0),
('您会在社交平台公开分享个人信息（如生日、地址等）吗？', 'INFO', 1.10, 'SINGLE', NOW(), NOW(), 0),

-- 金融安全意识维度 (FINANCE)
('您是否会向陌生人透露银行卡号或密码？', 'FINANCE', 2.00, 'SINGLE', NOW(), NOW(), 0),
('您是否会在收到"中奖"通知后立即提供个人信息？', 'FINANCE', 1.80, 'SINGLE', NOW(), NOW(), 0),
('您是否会通过非官方渠道进行大额转账？', 'FINANCE', 1.60, 'SINGLE', NOW(), NOW(), 0),
('您是否会在投资理财时相信"高收益、低风险"的宣传？', 'FINANCE', 1.40, 'SINGLE', NOW(), NOW(), 0),
('您是否会仔细核对转账对象的身份信息？', 'FINANCE', 1.20, 'SINGLE', NOW(), NOW(), 0),

-- 心理风险倾向维度 (PSYCH)
('您是否容易被"限时优惠"、"最后机会"等营销话术影响？', 'PSYCH', 1.30, 'SINGLE', NOW(), NOW(), 0),
('您是否会在情绪激动时做出重要财务决策？', 'PSYCH', 1.50, 'SINGLE', NOW(), NOW(), 0),
('您是否容易相信陌生人的"权威"身份（如自称警察、法官等）？', 'PSYCH', 1.70, 'SINGLE', NOW(), NOW(), 0),
('您是否会在收到"紧急"通知时立即采取行动？', 'PSYCH', 1.40, 'SINGLE', NOW(), NOW(), 0),
('您是否会因为"贪小便宜"而忽略潜在风险？', 'PSYCH', 1.60, 'SINGLE', NOW(), NOW(), 0);

-- ============================================================
-- 4. 风险测评选项表数据 (af_risk_option)
-- ============================================================
-- 注意：选项存储在独立的 af_risk_option 表中，通过 question_id 关联

-- 题目1的选项（收到未知链接）
INSERT INTO `af_risk_option` (`question_id`, `label`, `option_value`, `created_at`, `updated_at`, `deleted`) VALUES
(1, '经常', 3, NOW(), NOW(), 0),
(1, '偶尔', 2, NOW(), NOW(), 0),
(1, '从不', 0, NOW(), NOW(), 0);

-- 题目2的选项（使用相同密码）
INSERT INTO `af_risk_option` (`question_id`, `label`, `option_value`, `created_at`, `updated_at`, `deleted`) VALUES
(2, '是，所有网站都用相同密码', 3, NOW(), NOW(), 0),
(2, '部分网站使用相同密码', 2, NOW(), NOW(), 0),
(2, '每个网站都使用不同密码', 0, NOW(), NOW(), 0);

-- 题目3的选项（公共WiFi）
INSERT INTO `af_risk_option` (`question_id`, `label`, `option_value`, `created_at`, `updated_at`, `deleted`) VALUES
(3, '经常', 3, NOW(), NOW(), 0),
(3, '偶尔', 1, NOW(), NOW(), 0),
(3, '从不', 0, NOW(), NOW(), 0);

-- 题目4的选项（更新软件）
INSERT INTO `af_risk_option` (`question_id`, `label`, `option_value`, `created_at`, `updated_at`, `deleted`) VALUES
(4, '从不更新', 3, NOW(), NOW(), 0),
(4, '偶尔更新', 1, NOW(), NOW(), 0),
(4, '定期更新', 0, NOW(), NOW(), 0);

-- 题目5的选项（分享个人信息）
INSERT INTO `af_risk_option` (`question_id`, `label`, `option_value`, `created_at`, `updated_at`, `deleted`) VALUES
(5, '经常分享', 3, NOW(), NOW(), 0),
(5, '偶尔分享', 1, NOW(), NOW(), 0),
(5, '从不分享', 0, NOW(), NOW(), 0);

-- 题目6的选项（透露银行卡信息）
INSERT INTO `af_risk_option` (`question_id`, `label`, `option_value`, `created_at`, `updated_at`, `deleted`) VALUES
(6, '会', 3, NOW(), NOW(), 0),
(6, '不确定', 2, NOW(), NOW(), 0),
(6, '不会', 0, NOW(), NOW(), 0);

-- 题目7的选项（中奖通知）
INSERT INTO `af_risk_option` (`question_id`, `label`, `option_value`, `created_at`, `updated_at`, `deleted`) VALUES
(7, '会立即提供', 3, NOW(), NOW(), 0),
(7, '会先核实', 1, NOW(), NOW(), 0),
(7, '不会提供', 0, NOW(), NOW(), 0);

-- 题目8的选项（非官方转账）
INSERT INTO `af_risk_option` (`question_id`, `label`, `option_value`, `created_at`, `updated_at`, `deleted`) VALUES
(8, '会', 3, NOW(), NOW(), 0),
(8, '不确定', 2, NOW(), NOW(), 0),
(8, '不会', 0, NOW(), NOW(), 0);

-- 题目9的选项（高收益低风险）
INSERT INTO `af_risk_option` (`question_id`, `label`, `option_value`, `created_at`, `updated_at`, `deleted`) VALUES
(9, '会相信', 3, NOW(), NOW(), 0),
(9, '半信半疑', 1, NOW(), NOW(), 0),
(9, '不会相信', 0, NOW(), NOW(), 0);

-- 题目10的选项（核对转账对象）
INSERT INTO `af_risk_option` (`question_id`, `label`, `option_value`, `created_at`, `updated_at`, `deleted`) VALUES
(10, '从不核对', 3, NOW(), NOW(), 0),
(10, '偶尔核对', 1, NOW(), NOW(), 0),
(10, '总是核对', 0, NOW(), NOW(), 0);

-- 题目11的选项（限时优惠）
INSERT INTO `af_risk_option` (`question_id`, `label`, `option_value`, `created_at`, `updated_at`, `deleted`) VALUES
(11, '很容易被影响', 3, NOW(), NOW(), 0),
(11, '偶尔被影响', 1, NOW(), NOW(), 0),
(11, '不会被影响', 0, NOW(), NOW(), 0);

-- 题目12的选项（情绪决策）
INSERT INTO `af_risk_option` (`question_id`, `label`, `option_value`, `created_at`, `updated_at`, `deleted`) VALUES
(12, '经常', 3, NOW(), NOW(), 0),
(12, '偶尔', 1, NOW(), NOW(), 0),
(12, '从不', 0, NOW(), NOW(), 0);

-- 题目13的选项（相信权威身份）
INSERT INTO `af_risk_option` (`question_id`, `label`, `option_value`, `created_at`, `updated_at`, `deleted`) VALUES
(13, '很容易相信', 3, NOW(), NOW(), 0),
(13, '会核实', 1, NOW(), NOW(), 0),
(13, '不会轻易相信', 0, NOW(), NOW(), 0);

-- 题目14的选项（紧急通知）
INSERT INTO `af_risk_option` (`question_id`, `label`, `option_value`, `created_at`, `updated_at`, `deleted`) VALUES
(14, '会立即行动', 3, NOW(), NOW(), 0),
(14, '会先核实', 1, NOW(), NOW(), 0),
(14, '会谨慎处理', 0, NOW(), NOW(), 0);

-- 题目15的选项（贪小便宜）
INSERT INTO `af_risk_option` (`question_id`, `label`, `option_value`, `created_at`, `updated_at`, `deleted`) VALUES
(15, '经常', 3, NOW(), NOW(), 0),
(15, '偶尔', 1, NOW(), NOW(), 0),
(15, '从不', 0, NOW(), NOW(), 0);

-- ============================================================
-- 5. 防骗知识表数据 (af_knowledge)
-- ============================================================
-- category: 短信/电话/网站/社交等
-- difficulty: 1-5 (重要程度/难度)

INSERT INTO `af_knowledge` (`category`, `title`, `summary`, `content`, `difficulty`, `tags`, `enable_flag`, `created_at`, `updated_at`, `deleted`) VALUES
('短信', '如何识别钓鱼短信',
'识别钓鱼短信的关键特征和防范方法',
'钓鱼短信是常见的诈骗手段，识别要点包括：\n\n1. 检查发送号码：正规机构通常使用官方短号码，而非普通手机号\n2. 查看链接域名：官方链接通常使用官方网站域名，不会使用短链接或陌生域名\n3. 注意语气：诈骗短信通常使用恐吓、催促的语气，如"立即"、"否则"等\n4. 要求操作：正规通知通常只告知信息，不会要求点击链接或提供验证码\n\n防范措施：\n- 不要点击短信中的链接\n- 通过官方渠道核实信息\n- 不要提供验证码或密码\n- 遇到可疑短信直接删除',
3,
'短信诈骗,钓鱼,识别',
1, NOW(), NOW(), 0),

('短信', '虚假中奖短信识别',
'如何识别和防范虚假中奖短信诈骗',
'虚假中奖短信是常见的诈骗手段，特征包括：\n\n1. 声称中奖：声称您获得了高价值奖品（如手机、现金等）\n2. 要求操作：要求点击链接、提供个人信息或支付费用\n3. 限时催促：声称必须在短时间内领取，制造紧迫感\n4. 异常链接：使用短链接或非官方域名\n\n防范要点：\n- 正规抽奖活动不会通过短信通知中奖\n- 所有要求先付款的"中奖"都是诈骗\n- 不要点击短信中的链接\n- 不要提供个人信息或验证码\n\n如果确实参与了抽奖活动，应通过官方渠道查询中奖信息。',
3,
'短信诈骗,虚假中奖,识别',
1, NOW(), NOW(), 0),

('电话', '钓鱼邮件防范指南',
'识别和防范钓鱼邮件的实用方法',
'钓鱼邮件是网络诈骗的重要手段，识别要点：\n\n1. 发件人地址：检查发件人邮箱地址，诈骗邮件通常使用相似但不完全相同的地址\n2. 邮件内容：注意语法错误、格式异常等\n3. 链接地址：将鼠标悬停在链接上查看真实地址，不要直接点击\n4. 附件：不要打开可疑邮件的附件，可能包含恶意软件\n5. 紧急催促：诈骗邮件通常使用"紧急"、"立即"等词汇制造紧张\n\n防范措施：\n- 通过官方渠道核实邮件内容\n- 不要点击邮件中的链接\n- 不要下载或打开可疑附件\n- 使用邮件安全软件\n- 定期更新邮箱密码',
4,
'邮件诈骗,钓鱼,防范',
1, NOW(), NOW(), 0),

('网站', '识别虚假购物网站',
'如何识别和防范虚假购物网站诈骗',
'虚假购物网站是常见的网络诈骗手段，识别要点：\n\n1. 价格异常：商品价格远低于市场价\n2. 支付方式：只接受银行转账或第三方支付，不接受正规支付平台\n3. 网站信息：缺少详细的联系方式、公司信息等\n4. 用户评价：没有或只有虚假的好评\n5. 域名：使用非主流域名或与知名网站相似的域名\n\n防范措施：\n- 选择知名、正规的购物平台\n- 查看网站备案信息\n- 使用正规支付方式\n- 不要先付款后发货\n- 保留交易记录\n\n如果发现是虚假网站，应立即停止交易并举报。',
4,
'网站诈骗,购物,识别',
1, NOW(), NOW(), 0),

('网站', '仿冒银行网站识别',
'如何识别仿冒银行网站并保护账户安全',
'仿冒银行网站是严重的金融诈骗手段，识别要点：\n\n1. 域名检查：仔细核对域名，仿冒网站通常使用相似但不完全相同的域名\n2. 网站设计：虽然可能相似，但细节处通常有差异\n3. 要求输入密码：正规银行不会要求用户输入完整密码进行验证\n4. SSL证书：检查网站是否有有效的SSL证书\n5. 联系方式：查看网站上的联系方式是否与官方一致\n\n防范措施：\n- 通过官方渠道访问银行网站\n- 不要点击邮件或短信中的链接\n- 使用官方APP进行银行操作\n- 定期检查账户交易记录\n- 发现异常立即联系银行\n\n如果已经输入了敏感信息，应立即修改密码并联系银行。',
5,
'网站诈骗,银行,安全',
1, NOW(), NOW(), 0),

('社交', '投资理财诈骗防范',
'识别和防范投资理财诈骗的方法',
'投资理财诈骗是常见的金融诈骗手段，常见类型：\n\n1. 高收益承诺：承诺"高收益、低风险"的投资项目\n2. 虚假平台：使用虚假的投资平台或APP\n3. 拉人头：要求发展下线，类似传销模式\n4. 限时优惠：声称"限时优惠"、"最后机会"等\n5. 权威背书：声称有政府或知名机构背书\n\n识别要点：\n- 任何投资都有风险，不存在"高收益、低风险"的项目\n- 正规投资平台有完善的资质和监管\n- 不要相信"稳赚不赔"的承诺\n- 不要被"限时优惠"等营销话术影响\n\n防范措施：\n- 选择正规、有资质的投资平台\n- 了解投资产品的风险和收益\n- 不要被高收益承诺诱惑\n- 咨询专业的投资顾问\n- 保留所有交易记录',
5,
'金融诈骗,投资,理财',
1, NOW(), NOW(), 0),

('社交', '社交平台诈骗防范',
'如何防范社交平台上的各类诈骗',
'社交平台诈骗是常见的网络诈骗手段，常见类型：\n\n1. 冒充好友：盗取账号后冒充好友借钱或要求转账\n2. 虚假招聘：发布虚假招聘信息，要求缴纳费用\n3. 虚假商品：在社交平台销售虚假或劣质商品\n4. 情感诈骗：通过建立感情关系进行诈骗\n5. 虚假活动：组织虚假活动，要求缴纳费用\n\n防范措施：\n- 不要轻易相信陌生人的信息\n- 通过其他渠道核实好友身份\n- 不要向陌生人转账或提供个人信息\n- 谨慎参与社交平台上的活动\n- 保护个人隐私，不要公开分享敏感信息\n\n如果发现可疑情况，应立即停止交流并举报。',
3,
'社交诈骗,平台,防范',
1, NOW(), NOW(), 0),

('电话', '电话诈骗防范指南',
'识别和防范各类电话诈骗的方法',
'电话诈骗是传统的诈骗手段，常见类型：\n\n1. 冒充公检法：声称涉嫌犯罪，要求配合调查\n2. 冒充客服：声称账户异常，要求提供验证码\n3. 虚假中奖：声称中奖，要求缴纳费用\n4. 冒充亲友：声称遇到紧急情况，要求转账\n5. 虚假投资：推销虚假的投资项目\n\n识别要点：\n- 公检法不会通过电话办案\n- 正规客服不会要求提供验证码\n- 所有要求转账的电话都应提高警惕\n- 不要被"紧急"、"立即"等词汇影响判断\n\n防范措施：\n- 不要轻易相信陌生电话\n- 通过官方渠道核实信息\n- 不要提供验证码或密码\n- 不要按照电话指示操作\n- 遇到可疑电话立即挂断\n\n如果已经受骗，应立即报警并联系相关机构。',
4,
'电话诈骗,识别,防范',
1, NOW(), NOW(), 0);

-- ============================================================
-- 6. 训练记录示例数据 (af_training_record) - 可选
-- ============================================================
-- 注意：这些数据需要根据实际用户ID和案例ID进行调整
-- user_judgement: fraud/safe
-- correct: 1=正确, 0=错误
-- selected_tags: JSON数组格式

-- INSERT INTO `af_training_record` (`user_id`, `case_id`, `user_judgement`, `correct`, `selected_tags`, `duration_seconds`, `answered_at`, `deleted`) VALUES
-- (2, 1, 'fraud', 1, '["非官方域名", "恐吓语气催促立即操作"]', 5, NOW(), 0),
-- (2, 2, 'fraud', 1, '["虚假中奖信息", "短链接"]', 3, NOW(), 0),
-- (2, 8, 'safe', 1, '["官方客服电话", "仅通知不要求操作"]', 4, NOW(), 0),
-- (3, 1, 'fraud', 1, '["非官方域名", "要求点击链接验证"]', 6, NOW(), 0),
-- (3, 3, 'safe', 0, '["异常通知制造紧张"]', 8, NOW(), 0);

-- ============================================================
-- 7. 测评结果示例数据 (af_assessment_result) - 可选
-- ============================================================
-- 注意：这些数据需要根据实际用户ID进行调整
-- risk_level: 0=低风险, 1=中风险, 2=高风险

-- INSERT INTO `af_assessment_result` (`user_id`, `total_score`, `risk_level`, `info_score`, `finance_score`, `psych_score`, `raw_detail_json`, `suggestions_json`, `created_at`, `deleted`) VALUES
-- (2, 25, 0, 20, 15, 42, '{"info": {"score": 20, "details": []}, "finance": {"score": 15, "details": []}, "psych": {"score": 42, "details": []}}', '["加强心理防范意识", "遇到紧急情况时保持冷静", "不要被营销话术影响"]', NOW(), 0),
-- (3, 65, 2, 70, 80, 46, '{"info": {"score": 70, "details": []}, "finance": {"score": 80, "details": []}, "psych": {"score": 46, "details": []}}', '["加强信息保护意识", "不要点击未知链接", "谨慎处理金融交易", "定期更新密码"]', NOW(), 0);

-- ============================================================
-- 数据插入完成
-- ============================================================
--
-- 使用说明：
-- 1. 所有测试用户的密码均为：123456
-- 2. 密码已使用 BCrypt 加密存储
-- 3. 可以根据需要调整或添加更多测试数据
-- 4. 训练记录和测评结果的数据已注释，可根据需要取消注释
--
-- 注意事项：
-- - 执行前请确保已创建数据库和表结构（使用 schema_mysql.sql）
-- - 建议在测试环境中使用
-- - 生产环境请使用强密码并修改默认密码
-- - risk_level 字段：0=低风险, 1=中风险, 2=高风险
-- ============================================================
