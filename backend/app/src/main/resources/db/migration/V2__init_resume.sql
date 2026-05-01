-- ==========================================
-- V2__init_resume.sql
-- Resume Module Database Schema (Multi-Version Support)
-- 简历模块数据库结构（支持多版本管理）
-- ==========================================

-- 创建简历组表（一份简历的整体概念）/ Create resume group table (overall concept of a resume)
CREATE TABLE IF NOT EXISTS resume_groups
(
    id          UUID PRIMARY KEY                  DEFAULT uuid_generate_v4(),
    user_id     UUID                     NOT NULL,
    title       VARCHAR(255)             NOT NULL,
    is_default  BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_resume_group_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_resume_groups_user_id ON resume_groups (user_id);

COMMENT ON TABLE resume_groups IS '简历组表 - 代表一份简历的整体概念 / Resume group table representing the overall concept of a resume';

-- 创建简历版本表（原版/转换版/AI版）/ Create resume version table (original/converted/AI-optimized)
CREATE TABLE IF NOT EXISTS resume_versions
(
    id                 UUID PRIMARY KEY                  DEFAULT uuid_generate_v4(),
    group_id           UUID                     NOT NULL,

    -- 版本类型：ORIGINAL(原版), CONVERTED(转换版), AI_OPTIMIZED(AI版)
    -- Version type: ORIGINAL, CONVERTED, AI_OPTIMIZED
    version_type       VARCHAR(20)              NOT NULL DEFAULT 'ORIGINAL',

    -- 文件信息（原版用）/ File info (for original version)
    original_file_name VARCHAR(255),
    stored_file_name   VARCHAR(255),
    file_type          VARCHAR(100),            -- application/pdf, text/markdown
    file_size          BIGINT,
    storage_path       TEXT,
    storage_provider   VARCHAR(50)              DEFAULT 'minio',

    -- 内容（转换版/AI版用，Markdown格式）/ Content (for converted/AI versions, Markdown format)
    content            TEXT,

    -- 解析结果（结构化JSON）/ Parsed content (structured JSON)
    parsed_content     JSONB,

    -- AI优化记录ID（AI版用）/ AI optimization record ID (for AI version)
    ai_optimization_id UUID,

    -- 状态：ACTIVE(活跃), ARCHIVED(归档) / Status: ACTIVE, ARCHIVED
    status             VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE',

    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_version_group FOREIGN KEY (group_id)
        REFERENCES resume_groups (id) ON DELETE CASCADE,

    -- 约束：每个组每种类型只能有一个ACTIVE版本 / Constraint: one ACTIVE version per group per type
    CONSTRAINT chk_version_type CHECK (version_type IN ('ORIGINAL', 'CONVERTED', 'AI_OPTIMIZED')),
    CONSTRAINT chk_version_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

-- 唯一约束：每个简历组每种类型只能有一个ACTIVE版本 / Unique constraint: one ACTIVE version per group per type
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_active_version
    ON resume_versions (group_id, version_type)
    WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_resume_versions_group_id ON resume_versions (group_id);
CREATE INDEX IF NOT EXISTS idx_resume_versions_group_type ON resume_versions (group_id, version_type);

COMMENT ON TABLE resume_versions IS '简历版本表 - 存储原版、转换版、AI版 / Resume version table storing original, converted, and AI-optimized versions';
COMMENT ON COLUMN resume_versions.version_type IS '版本类型: ORIGINAL(原版), CONVERTED(转换版), AI_OPTIMIZED(AI版) / Version type: ORIGINAL, CONVERTED, AI_OPTIMIZED';
COMMENT ON COLUMN resume_versions.content IS 'Markdown内容，用于编辑转换版/AI版 / Markdown content for editing converted/AI versions';

-- 触发器函数 / Trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 触发器 / Triggers
DROP TRIGGER IF EXISTS update_resume_groups_updated_at ON resume_groups;
CREATE TRIGGER update_resume_groups_updated_at
    BEFORE UPDATE ON resume_groups
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_resume_versions_updated_at ON resume_versions;
CREATE TRIGGER update_resume_versions_updated_at
    BEFORE UPDATE ON resume_versions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
