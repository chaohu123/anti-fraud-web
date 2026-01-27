-- 修复用户头像字段类型
-- 如果字段不存在则添加，如果存在则修改类型为 LONGTEXT 以支持 base64 数据

-- 方法1：如果字段已存在，修改类型
ALTER TABLE `af_user` 
MODIFY COLUMN `avatar_url` LONGTEXT NULL COMMENT '用户头像URL（base64或URL）';

-- 如果上面的语句报错（字段不存在），则执行下面的语句添加字段
-- ALTER TABLE `af_user` 
-- ADD COLUMN `avatar_url` LONGTEXT NULL COMMENT '用户头像URL（base64或URL）' 
-- AFTER `last_score`;
