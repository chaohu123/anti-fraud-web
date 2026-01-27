-- 安全修复用户头像字段类型（先检查再操作）
-- 支持 base64 编码的头像数据（可能很大，需要 LONGTEXT 类型）

-- 检查字段是否存在，如果存在则修改类型，如果不存在则添加
SET @dbname = DATABASE();
SET @tablename = 'af_user';
SET @columnname = 'avatar_url';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'ALTER TABLE `af_user` MODIFY COLUMN `avatar_url` LONGTEXT NULL COMMENT ''用户头像URL（base64或URL）'';',
  'ALTER TABLE `af_user` ADD COLUMN `avatar_url` LONGTEXT NULL COMMENT ''用户头像URL（base64或URL）'' AFTER `last_score`;'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
