-- V11: Add auth_provider to users table for multi-auth-source support
-- V11: 为用户表添加 auth_provider 字段以支持多认证源

-- Add auth_provider column with default EMAIL for existing users
-- 添加 auth_provider 列，现有用户默认值为 EMAIL
ALTER TABLE users
    ADD COLUMN auth_provider VARCHAR(50) NOT NULL DEFAULT 'EMAIL';

-- Add index for quick auth provider lookups
-- 添加索引以便快速按认证源查询
CREATE INDEX idx_users_auth_provider ON users (auth_provider);
