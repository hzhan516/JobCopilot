-- ==========================================
-- V2__init_resume.sql
-- Resume Module Database Schema
-- 简历模块数据库结构
-- ==========================================

-- 创建简历表
CREATE TABLE IF NOT EXISTS resumes
(
    id                 UUID PRIMARY KEY                  DEFAULT uuid_generate_v4(),
    user_id            UUID                     NOT NULL,
    title              VARCHAR(255)             NOT NULL,
    original_file_name VARCHAR(255)             NOT NULL,
    stored_file_name   VARCHAR(255)             NOT NULL UNIQUE,
    file_type          VARCHAR(100)             NOT NULL,
    file_size          BIGINT                   NOT NULL,
    storage_path       TEXT                     NOT NULL,
    storage_provider   VARCHAR(50)              NOT NULL DEFAULT 'minio',
    processing_status  VARCHAR(20)              NOT NULL DEFAULT 'PENDING',
    parsed_content     TEXT,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 外键约束
    CONSTRAINT fk_resume_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_resumes_user_id ON resumes (user_id);
CREATE INDEX IF NOT EXISTS idx_resumes_processing_status ON resumes (processing_status);
CREATE INDEX IF NOT EXISTS idx_resumes_user_status ON resumes (user_id, processing_status);
CREATE INDEX IF NOT EXISTS idx_resumes_created_at ON resumes (created_at DESC);

-- 添加表注释
COMMENT ON TABLE resumes IS '简历表 / Resume table - stores user resume metadata';
COMMENT ON COLUMN resumes.id IS '简历ID / Resume ID';
COMMENT ON COLUMN resumes.user_id IS '用户ID / User ID - foreign key to users table';
COMMENT ON COLUMN resumes.title IS '简历标题 / Resume title';
COMMENT ON COLUMN resumes.original_file_name IS '原始文件名 / Original file name uploaded by user';
COMMENT ON COLUMN resumes.stored_file_name IS '存储文件名 / Stored file name (UUID format)';
COMMENT ON COLUMN resumes.file_type IS '文件类型 / File MIME type';
COMMENT ON COLUMN resumes.file_size IS '文件大小(字节) / File size in bytes';
COMMENT ON COLUMN resumes.storage_path IS '存储路径 / Storage path in MinIO';
COMMENT ON COLUMN resumes.storage_provider IS '存储提供商 / Storage provider (minio)';
COMMENT ON COLUMN resumes.processing_status IS '处理状态 / Processing status: PENDING, PROCESSING, COMPLETED, FAILED';
COMMENT ON COLUMN resumes.parsed_content IS '解析内容 / Parsed resume content (JSON)';
COMMENT ON COLUMN resumes.created_at IS '创建时间 / Creation timestamp';
COMMENT ON COLUMN resumes.updated_at IS '更新时间 / Last update timestamp';

-- 创建更新时间触发器函数（如果不存在）
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 创建简历表更新时间触发器
DROP TRIGGER IF EXISTS update_resumes_updated_at ON resumes;
CREATE TRIGGER update_resumes_updated_at
    BEFORE UPDATE
    ON resumes
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
