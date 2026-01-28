# SQL 脚本使用说明

## 文件说明

### 1. `schema.sql`
- **用途**：创建数据库及完整表结构
- **内容**：CREATE DATABASE、所有表的 CREATE TABLE 语句
- **执行顺序**：第一步

### 2. `insert.sql`
- **用途**：插入示例/测试数据
- **内容**：用户、诈骗案例、风险测评题目与选项、防骗知识、成就等示例数据
- **执行顺序**：第二步（在创建表结构之后）

## 使用步骤

### 方式一：MySQL 命令行

```bash
# 1. 登录 MySQL
mysql -u root -p

# 2. 执行建表脚本（会创建库 anti_fraud 并建表）
SOURCE /path/to/backend/sql/schema.sql;

# 3. 执行数据插入脚本
SOURCE /path/to/backend/sql/insert.sql;
```

### 方式二：命令行直接执行

```bash
mysql -u root -p < backend/sql/schema.sql
mysql -u root -p < backend/sql/insert.sql
```

### 方式三：图形化工具（如 MySQL Workbench）

1. 执行 `schema.mysql` 创建数据库和表
2. 执行 `insert.sql` 插入示例数据

## 测试账号说明

所有测试用户密码均为：**123456**（BCrypt 存储）

| 用户名    | 昵称       | 说明       |
|----------|------------|------------|
| admin    | 系统管理员 | 管理员账号 |
| testuser1| 防骗新手   | 测试用户   |
| testuser2| 安全达人   | 测试用户   |
| testuser3| 防骗专家   | 测试用户   |
| zhangsan | 张三       | 普通用户   |
| lisi     | 李四       | 普通用户   |

若登录时密码校验失败，可运行后端 `PasswordHashGenerator.java` 生成新的 BCrypt 哈希，并更新 `insert.sql` 中的 `@pwd_hash` 或直接执行 `UPDATE af_user SET password_hash = '...' WHERE username = 'admin';`。

## 表结构概览

- **af_user**：用户
- **af_fraud_case**：诈骗案例（训练题目）
- **af_training_record**：识别训练记录
- **af_risk_question** / **af_risk_option**：风险测评题目与选项
- **af_assessment_result**：测评结果（综合分数与维度分）
- **af_risk_assessment**：防骗风险测评结果（LOW/MEDIUM/HIGH）
- **af_knowledge**：防骗知识文章
- **af_learning_record**：学习记录
- **af_achievement** / **af_user_achievement**：成就及用户成就
- **af_system_config**：系统配置

## 注意事项

1. **生产环境**：勿使用默认测试密码，请修改并删除或脱敏测试数据。
2. **字符集**：库与表均使用 `utf8mb4`，以支持中文及 emoji。
3. **执行顺序**：必须先执行 `schema.sql` 再执行 `insert.sql`。
4. 本目录脚本与后端配置文件 `backend/src/main/resources/application.yml` 中的 `spring.datasource.*` 对应，请确保数据库名、用户名和密码保持一致。
