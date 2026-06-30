-- 动态配置表 / Dynamic configuration table
CREATE TABLE IF NOT EXISTS dynamic_config (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value JSONB NOT NULL,
    default_value JSONB NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50) NOT NULL,
    value_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    is_sensitive BOOLEAN DEFAULT FALSE,
    is_readonly BOOLEAN DEFAULT FALSE,
    updated_by UUID REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 种子数据：从环境变量默认值填充 / Seed data from env defaults
INSERT INTO dynamic_config (config_key, config_value, default_value, description, category, value_type) VALUES
('captcha.enabled', 'true', 'true', 'Enable slider CAPTCHA', 'security', 'BOOLEAN'),
('captcha.tolerance', '8', '8', 'CAPTCHA drag tolerance (pixels)', 'security', 'NUMBER'),
('captcha.maxAttempts', '5', '5', 'Max CAPTCHA attempts per IP', 'security', 'NUMBER'),
('captcha.tokenExpiry', '300', '300', 'CAPTCHA token lifetime (seconds)', 'security', 'NUMBER'),
('emailVerification.enabled', 'false', 'false', 'Require email verification on register', 'email', 'BOOLEAN'),
('emailVerification.codeExpiry', '300', '300', 'Verification code lifetime (seconds)', 'email', 'NUMBER'),
('llm.textModel', '"gemini/gemini-2.5-flash"', '"gemini/gemini-2.5-flash"', 'Text generation model', 'ai', 'STRING'),
('llm.visionModel', '"gemini/gemini-2.5-flash"', '"gemini/gemini-2.5-flash"', 'Vision model for PDF/image', 'ai', 'STRING'),
('llm.embeddingModel', '"gemini/gemini-embedding-001"', '"gemini/gemini-embedding-001"', 'Embedding model', 'ai', 'STRING'),
('feature.googleAuth', 'true', 'true', 'Google OAuth login', 'feature', 'BOOLEAN'),
('feature.chat', 'true', 'true', 'AI chat assistant', 'feature', 'BOOLEAN'),
('feature.jobMatching', 'true', 'true', 'Job matching with AI', 'feature', 'BOOLEAN'),
('rateLimit.api', '100', '100', 'API rate limit (req/min)', 'rate', 'NUMBER'),
('rateLimit.auth', '10', '10', 'Auth rate limit (req/min)', 'rate', 'NUMBER'),
('log.backendLevel', '"INFO"', '"INFO"', 'Backend log level', 'logging', 'STRING'),
('log.aiServiceLevel', '"INFO"', '"INFO"', 'AI service log level', 'logging', 'STRING')
ON CONFLICT DO NOTHING;
