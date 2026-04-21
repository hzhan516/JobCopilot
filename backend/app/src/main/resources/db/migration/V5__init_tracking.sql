-- 求职申请跟踪表 / Job application tracking table
CREATE TABLE IF NOT EXISTS application_trackings (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64),
    company_name VARCHAR(200),
    job_title VARCHAR(200),
    status VARCHAR(32) NOT NULL,
    applied_at DATE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_application_trackings_user_id ON application_trackings(user_id);
CREATE INDEX IF NOT EXISTS idx_application_trackings_job_id ON application_trackings(job_id);
CREATE INDEX IF NOT EXISTS idx_application_trackings_status ON application_trackings(status);
