-- 职位匹配结果表 / Job match results table
CREATE TABLE IF NOT EXISTS job_match_results
(
    id                VARCHAR(64) PRIMARY KEY,
    user_id           VARCHAR(64) NOT NULL,
    resume_version_id VARCHAR(64),
    query             TEXT,
    status            VARCHAR(32) NOT NULL, -- PROCESSING, COMPLETED, FAILED
    recall_results    JSONB,                -- 召回阶段结果 [{jobId, distance}] / Recall stage results [{jobId, distance}]
    ranked_results    JSONB,                -- 精排结果 [MatchItem] / Ranked results [MatchItem]
    recall_time_ms    BIGINT,
    rank_time_ms      BIGINT,
    model_version     VARCHAR(50),
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at      TIMESTAMP
);

-- 索引 / Indexes
CREATE INDEX IF NOT EXISTS idx_job_match_results_user_id ON job_match_results (user_id);
CREATE INDEX IF NOT EXISTS idx_job_match_results_status ON job_match_results (status);
