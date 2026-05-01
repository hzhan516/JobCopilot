-- 职位匹配模型元数据表 / Job matching models metadata table
CREATE TABLE IF NOT EXISTS job_matching_models
(
    id                 BIGSERIAL PRIMARY KEY,
    model_name         VARCHAR(100) NOT NULL,
    model_version      VARCHAR(20)  NOT NULL,
    -- 'recall' 或 'ranker' / 'recall' or 'ranker'
    model_type         VARCHAR(50)  NOT NULL,
    storage_path       VARCHAR(500) NOT NULL,
    evaluation_metrics JSONB,
    is_active          BOOLEAN   DEFAULT FALSE,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_job_matching_models_name_version ON job_matching_models (model_name, model_version);
CREATE INDEX IF NOT EXISTS idx_job_matching_models_is_active ON job_matching_models (is_active);

-- 模型训练日志表 / Model training logs table
CREATE TABLE IF NOT EXISTS model_training_logs
(
    id            BIGSERIAL PRIMARY KEY,
    model_name    VARCHAR(100),
    model_version VARCHAR(20),
    model_type    VARCHAR(50),
    status        VARCHAR(32),
    metrics       JSONB,
    log_message   TEXT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_model_training_logs_name_version ON model_training_logs (model_name, model_version);

-- 职位数据集表 / Job dataset table
CREATE TABLE IF NOT EXISTS job_dataset
(
    id               BIGSERIAL PRIMARY KEY,
    external_id      VARCHAR(100),
    title            TEXT,
    company          VARCHAR(200),
    description      TEXT,
    requirements     TEXT[],
    location         VARCHAR(200),
    experience_level VARCHAR(50),
    source           VARCHAR(50),
    raw_data         JSONB,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_job_dataset_external_id ON job_dataset (external_id);
CREATE INDEX IF NOT EXISTS idx_job_dataset_company ON job_dataset (company);
