# 密码修复指南

## 问题描述

如果遇到密码验证失败的问题（`matches=false`），说明数据库中的密码哈希值无法验证。这通常是因为：
1. 哈希值不是用当前的 BCryptPasswordEncoder 生成的
2. 哈希值对应的原始密码不是用户输入的密码

## 解决方案

### 方案一：使用 API 直接修复（推荐）

1. 启动后端服务
2. 调用修复 API：

```bash
# 修复 admin 用户的密码为 123456
curl -X POST "http://localhost:8081/api/tools/password/fix?username=admin&newPassword=123456"

# 修复其他用户
curl -X POST "http://localhost:8081/api/tools/password/fix?username=testuser1&newPassword=123456"
```

API 会：
- 自动生成新的 BCrypt 哈希值
- 验证哈希值是否正确
- 直接更新数据库

### 方案二：使用 API 生成哈希值，然后手动更新数据库

1. 调用生成 API：

```bash
curl "http://localhost:8081/api/tools/password/generate?password=123456"
```

2. 复制返回的 `hash` 值
3. 执行 SQL 更新：

```sql
UPDATE af_user 
SET password_hash = '<生成的哈希值>' 
WHERE username = 'admin' AND deleted = 0;
```

### 方案三：使用 Java 工具类生成

1. 运行 `PasswordHashGenerator.java` 的 main 方法
2. 复制生成的哈希值
3. 更新 `fix_password_quick.sql` 中的哈希值
4. 执行 SQL 脚本

## 验证修复结果

修复后，可以通过以下方式验证：

1. 使用验证 API：

```bash
curl "http://localhost:8081/api/tools/password/verify?password=123456&hash=<数据库中的哈希值>"
```

2. 直接尝试登录，使用用户名和密码 123456

## 注意事项

- 所有测试用户的默认密码都是 `123456`
- 修复后请测试登录功能是否正常
- 生产环境请删除或禁用 `PasswordFixController`
