-- ============================================================================
-- Resume Assistant - Complete Database Initialization Script
-- 智能求职助手 - 完整数据库初始化脚本
-- ============================================================================
-- This script merges all Flyway migrations (V1-V12) into a single init file
-- for development environments where Flyway is disabled.
-- 本脚本将所有 Flyway 迁移（V1-V12）合并为单个初始化文件，用于禁用 Flyway 的开发环境。
-- ============================================================================

-- ==========================================
-- V1: Authentication Module / 认证模块
-- ==========================================

-- 用户主体表 / User principal table
CREATE TABLE IF NOT EXISTS users
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    email VARCHAR
(
    255
) UNIQUE NOT NULL,
    email_verified BOOLEAN DEFAULT FALSE,
    role VARCHAR
(
    50
) DEFAULT 'JOB_SEEKER',
    status VARCHAR
(
    50
) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 用户资料表 / User profile table
CREATE TABLE IF NOT EXISTS user_profiles
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    user_id UUID REFERENCES users
(
    id
) ON DELETE CASCADE,
    full_name VARCHAR
(
    255
),
    avatar_url VARCHAR
(
    1000
),
    phone VARCHAR
(
    50
),
    target_position VARCHAR
(
    255
),
    preferred_location VARCHAR
(
    255
),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE
(
    user_id
)
    );

-- 本地认证凭证表 / Local authentication credentials table
CREATE TABLE IF NOT EXISTS user_credentials
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    user_id UUID REFERENCES users
(
    id
) ON DELETE CASCADE,
    credential_type VARCHAR
(
    50
) NOT NULL,
    credential_value VARCHAR
(
    255
) NOT NULL,
    last_changed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE
(
    user_id,
    credential_type
)
    );

-- OAuth绑定表 / OAuth binding table
CREATE TABLE IF NOT EXISTS user_oauth_bindings
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    user_id UUID REFERENCES users
(
    id
) ON DELETE CASCADE,
    provider VARCHAR
(
    50
) NOT NULL,
    provider_user_id VARCHAR
(
    255
) NOT NULL,
    email VARCHAR
(
    255
),
    display_name VARCHAR
(
    255
),
    avatar_url VARCHAR
(
    1000
),
    access_token TEXT,
    refresh_token TEXT,
    expires_at TIMESTAMP,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE
(
    provider,
    provider_user_id
)
    );

-- 索引 / Indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_oauth_bindings_user ON user_oauth_bindings (user_id);
CREATE INDEX IF NOT EXISTS idx_oauth_bindings_email ON user_oauth_bindings (email);
CREATE INDEX IF NOT EXISTS idx_credentials_user ON user_credentials (user_id);

-- ==========================================
-- V2: Resume Module / 简历模块
-- ==========================================

-- 创建简历组表（一份简历的整体概念）/ Create resume group table
CREATE TABLE IF NOT EXISTS resume_groups
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    user_id UUID NOT NULL,
    title VARCHAR
(
    255
) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT fk_resume_group_user FOREIGN KEY (user_id)
    REFERENCES users
(
    id
)
                         ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_resume_groups_user_id ON resume_groups (user_id);

COMMENT
ON TABLE resume_groups IS '简历组表 - 代表一份简历的整体概念 / Resume group table representing the overall concept of a resume';

-- 创建简历版本表（原版/转换版/AI版）/ Create resume version table
CREATE TABLE IF NOT EXISTS resume_versions
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    group_id UUID NOT NULL,
    version_type VARCHAR
(
    20
) NOT NULL DEFAULT 'ORIGINAL',
    original_file_name VARCHAR
(
    255
),
    stored_file_name VARCHAR
(
    255
),
    file_type VARCHAR
(
    100
),
    file_size BIGINT,
    storage_path TEXT,
    storage_provider VARCHAR
(
    50
) DEFAULT 'minio',
    content TEXT,
    parsed_content JSONB,
    ai_optimization_id UUID,
    status VARCHAR
(
    20
) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT fk_version_group FOREIGN KEY (group_id)
    REFERENCES resume_groups
(
    id
)
                         ON DELETE CASCADE,
    CONSTRAINT chk_version_type CHECK
(
    version_type
    IN
(
    'ORIGINAL',
    'CONVERTED',
    'AI_OPTIMIZED'
)),
    CONSTRAINT chk_version_status CHECK
(
    status
    IN
(
    'ACTIVE',
    'ARCHIVED'
))
    );

-- 唯一约束：每个简历组每种类型只能有一个ACTIVE版本
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_active_version
    ON resume_versions (group_id, version_type)
    WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_resume_versions_group_id ON resume_versions (group_id);
CREATE INDEX IF NOT EXISTS idx_resume_versions_group_type ON resume_versions (group_id, version_type);

COMMENT
ON TABLE resume_versions IS '简历版本表 - 存储原版、转换版、AI版 / Resume version table storing original, converted, and AI-optimized versions';
COMMENT
ON COLUMN resume_versions.version_type IS '版本类型: ORIGINAL(原版), CONVERTED(转换版), AI_OPTIMIZED(AI版) / Version type: ORIGINAL, CONVERTED, AI_OPTIMIZED';
COMMENT
ON COLUMN resume_versions.content IS 'Markdown内容，用于编辑转换版/AI版 / Markdown content for editing converted/AI versions';

-- 触发器函数 / Trigger function
CREATE
OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at
= CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$
language 'plpgsql';

-- 触发器 / Triggers
DROP TRIGGER IF EXISTS update_resume_groups_updated_at ON resume_groups;
CREATE TRIGGER update_resume_groups_updated_at
    BEFORE UPDATE
    ON resume_groups
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_resume_versions_updated_at ON resume_versions;
CREATE TRIGGER update_resume_versions_updated_at
    BEFORE UPDATE
    ON resume_versions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- V3: Job Module / 职位模块
-- ==========================================

CREATE TABLE IF NOT EXISTS jobs
(
    id
    VARCHAR
(
    64
) PRIMARY KEY,
    user_id VARCHAR
(
    64
) NOT NULL,
    original_url TEXT NOT NULL,
    image_check_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR
(
    32
) NOT NULL,
    parsed_content JSONB,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_jobs_user_id ON jobs (user_id);
CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs (status);

-- 职位评分记录表 / Job score records
CREATE TABLE IF NOT EXISTS job_scores
(
    id
    VARCHAR
(
    64
) PRIMARY KEY,
    job_id VARCHAR
(
    64
) NOT NULL,
    resume_version_id VARCHAR
(
    64
) NOT NULL,
    user_id VARCHAR
(
    64
) NOT NULL,
    suitable BOOLEAN,
    final_score FLOAT,
    skill_score FLOAT,
    experience_score FLOAT,
    overall_score FLOAT,
    summary TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_job_scores_job_id ON job_scores (job_id);
CREATE INDEX IF NOT EXISTS idx_job_scores_user_id ON job_scores (user_id);

-- ==========================================
-- V4: Conversation Module / 对话模块
-- ==========================================

CREATE TABLE IF NOT EXISTS conversations
(
    id
    VARCHAR
(
    64
) PRIMARY KEY,
    user_id VARCHAR
(
    64
) NOT NULL,
    title VARCHAR
(
    200
),
    status VARCHAR
(
    32
),
    resume_version_id VARCHAR
(
    64
),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_conversations_user_id ON conversations (user_id);
CREATE INDEX IF NOT EXISTS idx_conversations_status ON conversations (status);

CREATE TABLE IF NOT EXISTS messages
(
    id
    VARCHAR
(
    64
) PRIMARY KEY,
    conversation_id VARCHAR
(
    64
) NOT NULL,
    role VARCHAR
(
    32
) NOT NULL,
    content TEXT,
    sequence INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS file_url VARCHAR (500);

CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON messages (conversation_id);

-- ==========================================
-- V5: Tracking Module / 求职跟踪模块
-- ==========================================

CREATE TABLE IF NOT EXISTS application_trackings
(
    id
    VARCHAR
(
    64
) PRIMARY KEY,
    user_id VARCHAR
(
    64
) NOT NULL,
    job_id VARCHAR
(
    64
),
    company_name VARCHAR
(
    200
),
    job_title VARCHAR
(
    200
),
    status VARCHAR
(
    32
) NOT NULL,
    applied_at DATE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    events JSONB
    );

CREATE INDEX IF NOT EXISTS idx_application_trackings_user_id ON application_trackings (user_id);
CREATE INDEX IF NOT EXISTS idx_application_trackings_job_id ON application_trackings (job_id);
CREATE INDEX IF NOT EXISTS idx_application_trackings_status ON application_trackings (status);

-- ==========================================
-- V6: pgvector Extension / 向量扩展
-- ==========================================

CREATE
EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS resume_vectors
(
    id
    VARCHAR
(
    64
) PRIMARY KEY,
    resume_version_id VARCHAR
(
    64
) NOT NULL UNIQUE,
    embedding vector
(
    1536
),
    status VARCHAR
(
    32
) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_resume_vectors_version_id ON resume_vectors (resume_version_id);
CREATE INDEX IF NOT EXISTS idx_resume_vectors_status ON resume_vectors (status);

CREATE TABLE IF NOT EXISTS job_vectors
(
    id
    VARCHAR
(
    64
) PRIMARY KEY,
    job_id VARCHAR
(
    64
) NOT NULL UNIQUE,
    embedding vector
(
    1536
),
    status VARCHAR
(
    32
) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR
(
    255
)
    );

CREATE INDEX IF NOT EXISTS idx_job_vectors_job_id ON job_vectors (job_id);
CREATE INDEX IF NOT EXISTS idx_job_vectors_status ON job_vectors (status);

-- ==========================================
-- V7: Drop unused table / 删除废弃表
-- ==========================================

DROP TABLE IF EXISTS resumes;

COMMENT
ON TABLE resume_groups IS '简历组表 - 代表一份简历的整体概念（取代旧版 resumes 表）/ Resume group table representing the overall concept of a resume (replaces old resumes table)';
COMMENT
ON TABLE resume_versions IS '简历版本表 - 存储原版、转换版、AI版 / Resume version table storing original, converted, and AI-optimized versions';

-- ==========================================
-- V8: Add resume parse status / 添加解析状态
-- ==========================================

ALTER TABLE resume_versions
    ADD COLUMN IF NOT EXISTS parse_status VARCHAR (20) NOT NULL DEFAULT 'PENDING';

ALTER TABLE resume_versions
    ADD COLUMN IF NOT EXISTS parse_error_message TEXT;

CREATE INDEX IF NOT EXISTS idx_resume_versions_parse_status ON resume_versions (parse_status);

-- ==========================================
-- V9: Job Matching / 职位匹配
-- ==========================================

CREATE TABLE IF NOT EXISTS job_matching_models
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    model_name
    VARCHAR
(
    100
) NOT NULL,
    model_version VARCHAR
(
    20
) NOT NULL,
    model_type VARCHAR
(
    50
) NOT NULL,
    storage_path VARCHAR
(
    500
) NOT NULL,
    evaluation_metrics JSONB,
    is_active BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_job_matching_models_name_version ON job_matching_models (model_name, model_version);
CREATE INDEX IF NOT EXISTS idx_job_matching_models_is_active ON job_matching_models (is_active);

CREATE TABLE IF NOT EXISTS model_training_logs
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    model_name
    VARCHAR
(
    100
),
    model_version VARCHAR
(
    20
),
    model_type VARCHAR
(
    50
),
    status VARCHAR
(
    32
),
    metrics JSONB,
    log_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_model_training_logs_name_version ON model_training_logs (model_name, model_version);

CREATE TABLE IF NOT EXISTS job_dataset
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    external_id
    VARCHAR
(
    100
),
    title TEXT,
    company VARCHAR
(
    200
),
    description TEXT,
    requirements TEXT[],
    location VARCHAR
(
    200
),
    experience_level VARCHAR
(
    50
),
    source VARCHAR
(
    50
),
    raw_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_job_dataset_external_id ON job_dataset (external_id);
CREATE INDEX IF NOT EXISTS idx_job_dataset_company ON job_dataset (company);

-- ==========================================
-- V10: Job Match Results / 职位匹配结果
-- ==========================================

CREATE TABLE IF NOT EXISTS job_match_results
(
    id
    VARCHAR
(
    64
) PRIMARY KEY,
    user_id VARCHAR
(
    64
) NOT NULL,
    resume_version_id VARCHAR
(
    64
),
    query TEXT,
    status VARCHAR
(
    32
) NOT NULL,
    recall_results JSONB,
    ranked_results JSONB,
    recall_time_ms BIGINT,
    rank_time_ms BIGINT,
    model_version VARCHAR
(
    50
),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_job_match_results_user_id ON job_match_results (user_id);
CREATE INDEX IF NOT EXISTS idx_job_match_results_status ON job_match_results (status);

-- ==========================================
-- V11: Add auth_provider / 添加认证源字段
-- ==========================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS auth_provider VARCHAR (50) NOT NULL DEFAULT 'EMAIL';

CREATE INDEX IF NOT EXISTS idx_users_auth_provider ON users (auth_provider);

-- ==========================================
-- V12: Add job_id to conversations / 对话表添加职位ID
-- ==========================================

ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS job_id VARCHAR (64);

CREATE INDEX IF NOT EXISTS idx_conversations_job_id ON conversations(job_id);

-- ==========================================
-- V13: Outbox Message / 事务发件箱消息表
-- ==========================================

-- Outbox 消息表 / Outbox message table
-- 用于事务发件箱模式，保证数据库写入与 MQ 消息发布的原子性
-- Used for the Transactional Outbox pattern to ensure atomicity between DB writes and MQ publishing
CREATE TABLE IF NOT EXISTS outbox_message
(
    id
    VARCHAR
(
    36
) PRIMARY KEY DEFAULT gen_random_uuid
(
),
    exchange VARCHAR
(
    255
) NOT NULL,
    routing_key VARCHAR
(
    255
) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR
(
    20
) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP
    );

-- 索引：按状态查询 PENDING 记录（OutboxRelayScheduler 轮询用）
-- Index for querying PENDING records (used by OutboxRelayScheduler polling)
CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_message(status);

-- 索引：按状态 + 发送时间查询过期记录（OutboxCleanupScheduler 清理用）
-- Index for querying expired records by status + sent time (used by OutboxCleanupScheduler)
CREATE INDEX IF NOT EXISTS idx_outbox_status_sent_at ON outbox_message(status, sent_at);
