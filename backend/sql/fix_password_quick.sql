-- ============================================================
-- 快速修复密码哈希值脚本
-- 说明：直接使用已知正确的 BCrypt 哈希值更新数据库
-- 所有测试用户密码均为：123456
-- ============================================================

-- ⚠️ 重要提示：
-- 此脚本中的哈希值可能已过期，如果无法验证，请使用以下方法之一：
-- 1. 使用 API 修复：POST /api/tools/password/fix?username=admin&newPassword=123456
-- 2. 使用 API 生成新哈希值：GET /api/tools/password/generate?password=123456
-- 3. 运行 PasswordHashGenerator.java 生成新的哈希值
-- 详细说明请参考：fix_password_api.md

-- 注意：以下哈希值需要用当前的 BCryptPasswordEncoder 生成
-- 密码：123456
-- 如果此哈希值无法验证，请使用上述方法生成新的哈希值并替换

-- 更新 admin 用户密码
-- ⚠️ 请先使用 API 生成新的哈希值，然后替换下面的哈希值
UPDATE `af_user`
SET `password_hash` = '$2a$10$tN2ThcwcMTjAu0cpLoEQre3UXooenm3s0AJJMwCxyj4vW7RQsS.5C'
WHERE `username` = 'admin' AND `deleted` = 0;

-- 更新所有测试用户密码
-- ⚠️ 请先使用 API 生成新的哈希值，然后替换下面的哈希值
UPDATE `af_user`
SET `password_hash` = '$2a$10$tN2ThcwcMTjAu0cpLoEQre3UXooenm3s0AJJMwCxyj4vW7RQsS.5C'
WHERE `username` IN ('testuser1', 'testuser2', 'testuser3', 'zhangsan', 'lisi')
  AND `deleted` = 0;

-- 验证更新结果

-- ============================================================
-- 如果上述哈希值仍然无法验证，请执行以下步骤：
-- 1. 运行 PasswordHashGenerator.java 的 main 方法
-- 2. 复制生成的哈希值
-- 3. 替换上面 SQL 中的哈希值
-- 4. 重新执行此脚本
-- ============================================================
