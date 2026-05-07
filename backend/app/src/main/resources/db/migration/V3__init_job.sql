-- V3__init_job.sql
-- Database schema for Job module (Link-to-Match)
-- 职位模块数据库结构（链接解析与匹配）

CREATE TABLE IF NOT EXISTS jobs
(
    id                  VARCHAR(64) PRIMARY KEY,
    user_id             VARCHAR(64) NOT NULL,
    original_url        TEXT        NOT NULL,
    image_check_enabled BOOLEAN     NOT NULL DEFAULT FALSE,
    status              VARCHAR(32) NOT NULL,
    parsed_content      JSONB,
    error_message       TEXT,
    created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引 / Indexes
CREATE INDEX IF NOT EXISTS idx_jobs_user_id ON jobs (user_id);
CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs (status);

-- 职位评分记录表 / Job score records
CREATE TABLE IF NOT EXISTS job_scores
(
    id                VARCHAR(64) PRIMARY KEY,
    job_id            VARCHAR(64) NOT NULL,
    resume_version_id VARCHAR(64) NOT NULL,
    user_id           VARCHAR(64) NOT NULL,
    suitable          BOOLEAN,
    final_score       FLOAT,
    skill_score       FLOAT,
    experience_score  FLOAT,
    overall_score     FLOAT,
    summary           TEXT,
    created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_job_scores_job_id ON job_scores (job_id);
CREATE INDEX IF NOT EXISTS idx_job_scores_user_id ON job_scores (user_id);
