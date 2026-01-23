# SQL 脚本使用说明

## 文件说明

### 1. `schema_spring_mysql.sql`
- **用途**：创建数据库表结构
- **内容**：包含所有表的 CREATE TABLE 语句
- **执行顺序**：第一步

### 2. `insert_data.sql`
- **用途**：插入测试数据
- **内容**：包含完整的测试数据，包括用户、案例、题目、选项、知识文章等
- **执行顺序**：第二步（在创建表结构之后）

## 使用步骤

### 方式一：使用 MySQL 命令行

```bash
# 1. 登录 MySQL
mysql -u root -p

# 2. 创建数据库（如果还没有）
CREATE DATABASE anti_fraud CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE anti_fraud;

# 3. 执行建表脚本
SOURCE /path/to/backend/sql/schema_spring_mysql.sql;

# 4. 执行数据插入脚本
SOURCE /path/to/backend/sql/insert_data.sql;
```

### 方式二：使用 MySQL Workbench 或其他图形化工具

1. 打开 MySQL Workbench
2. 连接到数据库服务器
3. 创建数据库 `anti_fraud`（如果还没有）
4. 打开 `schema_spring_mysql.sql`，执行建表脚本
5. 打开 `insert_data.sql`，执行数据插入脚本

### 方式三：使用命令行直接执行

```bash
# 执行建表脚本
mysql -u root -p anti_fraud < backend/sql/schema_spring_mysql.sql

# 执行数据插入脚本
mysql -u root -p anti_fraud < backend/sql/insert_data.sql
```

## 测试账号说明

### 默认测试用户

所有测试用户的密码均为：**123456**

| 用户名 | 昵称 | 风险等级 | 说明 |
|--------|------|----------|------|
| admin | 系统管理员 | LOW | 管理员账号 |
| testuser1 | 防骗新手 | LOW | 测试用户1 |
| testuser2 | 安全达人 | MEDIUM | 测试用户2 |
| testuser3 | 防骗专家 | HIGH | 测试用户3 |
| zhangsan | 张三 | NULL | 普通用户 |
| lisi | 李四 | LOW | 普通用户 |

### 密码说明

- 所有密码已使用 BCrypt 加密存储
- 测试密码：`123456`
- BCrypt Hash：`$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`

## 数据内容说明

### 1. 用户数据 (af_user)
- 6 个测试用户账号
- 包含不同风险等级的用户

### 2. 诈骗案例数据 (af_fraud_case)
- 9 个案例，包括：
  - 短信诈骗案例（3个）
  - 邮件诈骗案例（2个）
  - 网站诈骗案例（2个）
  - 正常案例（2个，用于训练）

### 3. 风险测评题目 (af_risk_question)
- 15 道题目，分为三个维度：
  - 信息保护意识 (INFO)：5题
  - 金融安全意识 (FINANCE)：5题
  - 心理风险倾向 (PSYCH)：5题

### 4. 风险测评选项 (af_risk_option)
- 每道题目包含 3 个选项
- 选项值越高表示风险越高

### 5. 防骗知识文章 (af_anti_fraud_article)
- 8 篇文章，涵盖：
  - 短信诈骗防范（2篇）
  - 邮件诈骗防范（1篇）
  - 网站诈骗防范（2篇）
  - 金融诈骗防范（1篇）
  - 社交平台诈骗防范（1篇）
  - 电话诈骗防范（1篇）

## 注意事项

1. **生产环境**：
   - 不要使用默认测试密码
   - 修改所有用户的密码
   - 删除测试数据

2. **数据完整性**：
   - 执行插入脚本前确保表结构已创建
   - 如果表已存在数据，可能需要先清空或使用 `TRUNCATE TABLE`

3. **外键约束**：
   - 训练记录、测评结果等数据需要根据实际用户ID和案例ID调整
   - 相关数据已注释，可根据需要取消注释

4. **字符集**：
   - 确保数据库使用 `utf8mb4` 字符集
   - 支持中文和特殊字符

## 常见问题

### Q: 如何重置密码？
A: 可以使用以下 SQL 更新密码（密码：123456）：
```sql
UPDATE af_user SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' WHERE username = 'your_username';
```

### Q: 如何生成新的 BCrypt 密码？
A: 可以使用在线工具或后端代码生成：
```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hashedPassword = encoder.encode("your_password");
```

### Q: 插入数据时出现外键错误？
A: 确保先插入主表数据（如用户、案例），再插入关联表数据（如训练记录）

### Q: 如何清空所有数据？
A: 可以使用以下 SQL（谨慎操作）：
```sql
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE af_user;
TRUNCATE TABLE af_fraud_case;
TRUNCATE TABLE af_risk_question;
TRUNCATE TABLE af_risk_option;
TRUNCATE TABLE af_anti_fraud_article;
-- 其他表...
SET FOREIGN_KEY_CHECKS = 1;
```

## 联系支持

如有问题，请查看项目文档或联系开发团队。
