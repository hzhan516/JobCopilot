-- =============================================================================
-- 智能求职助手 - 数据库初始化脚本
-- 创建基础表结构和pgvector扩展
-- =============================================================================

-- 启用pgvector扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'JOB_SEEKER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户资料表
CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(255),
    phone VARCHAR(50),
    target_position VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 简历表
CREATE TABLE IF NOT EXISTS resumes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    original_file_name VARCHAR(500),
    file_path VARCHAR(1000),
    file_type VARCHAR(50),
    file_size BIGINT,
    status VARCHAR(50) DEFAULT 'PENDING',
    summary JSONB,
    embedding_status VARCHAR(50) DEFAULT 'PENDING',
    parse_error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 简历详细解析数据
CREATE TABLE IF NOT EXISTS resume_details (
    id BIGSERIAL PRIMARY KEY,
    resume_id BIGINT REFERENCES resumes(id) ON DELETE CASCADE,
    full_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    all_skills JSONB,
    all_experiences JSONB,
    all_education JSONB,
    raw_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 简历向量表（pgvector）
CREATE TABLE IF NOT EXISTS resume_embeddings (
    id BIGSERIAL PRIMARY KEY,
    resume_id BIGINT REFERENCES resumes(id) ON DELETE CASCADE,
    embedding VECTOR(384),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(resume_id)
);

-- 职位表
CREATE TABLE IF NOT EXISTS jobs (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    company VARCHAR(500),
    description TEXT,
    summary JSONB,
    source VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 职位详细要求
CREATE TABLE IF NOT EXISTS job_details (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT REFERENCES jobs(id) ON DELETE CASCADE,
    requirements JSONB,
    responsibilities JSONB,
    qualifications JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 职位向量表（pgvector）
CREATE TABLE IF NOT EXISTS job_embeddings (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT REFERENCES jobs(id) ON DELETE CASCADE,
    embedding VECTOR(384),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(job_id)
);

-- 匹配结果表
CREATE TABLE IF NOT EXISTS match_results (
    id BIGSERIAL PRIMARY KEY,
    resume_id BIGINT REFERENCES resumes(id) ON DELETE CASCADE,
    job_id BIGINT REFERENCES jobs(id) ON DELETE CASCADE,
    overall_score DECIMAL(5,2),
    similarity_score DECIMAL(5,4),
    skill_match JSONB,
    experience_match JSONB,
    explanation TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 对话表
CREATE TABLE IF NOT EXISTS conversations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    resume_id BIGINT REFERENCES resumes(id) ON DELETE CASCADE,
    title VARCHAR(500),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_message_at TIMESTAMP
);

-- 消息表
CREATE TABLE IF NOT EXISTS messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT REFERENCES conversations(id) ON DELETE CASCADE,
    type VARCHAR(50),
    content TEXT NOT NULL,
    suggested_change JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 求职投递记录表
CREATE TABLE IF NOT EXISTS job_applications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    job_id BIGINT REFERENCES jobs(id) ON DELETE CASCADE,
    resume_id BIGINT REFERENCES resumes(id) ON DELETE CASCADE,
    status VARCHAR(50),
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT
);

-- 面试安排表
CREATE TABLE IF NOT EXISTS interviews (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT REFERENCES job_applications(id) ON DELETE CASCADE,
    interview_type VARCHAR(50),
    scheduled_at TIMESTAMP,
    duration_minutes INTEGER,
    location VARCHAR(500),
    notes TEXT,
    status VARCHAR(50) DEFAULT 'SCHEDULED'
);

-- 创建向量索引（IVFFlat - 快速近似搜索）
CREATE INDEX IF NOT EXISTS idx_resume_embeddings_vector ON resume_embeddings 
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE INDEX IF NOT EXISTS idx_job_embeddings_vector ON job_embeddings 
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 其他索引
CREATE INDEX IF NOT EXISTS idx_resumes_user_id ON resumes(user_id);
CREATE INDEX IF NOT EXISTS idx_resumes_status ON resumes(status);
CREATE INDEX IF NOT EXISTS idx_jobs_source ON jobs(source);
CREATE INDEX IF NOT EXISTS idx_match_results_resume_id ON match_results(resume_id);
CREATE INDEX IF NOT EXISTS idx_match_results_job_id ON match_results(job_id);
CREATE INDEX IF NOT EXISTS idx_match_results_score ON match_results(overall_score DESC);
CREATE INDEX IF NOT EXISTS idx_conversations_user_id ON conversations(user_id);
CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_applications_user_id ON job_applications(user_id);

-- 插入测试数据（可选）
-- INSERT INTO users (email, password_hash, role) VALUES ('test@example.com', 'hash', 'JOB_SEEKER');
