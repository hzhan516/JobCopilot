-- 启用 UUID 扩展（PostgreSQL 内置）/ Enable UUID extension (built-in PostgreSQL)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 用户主体表 / User principal table
CREATE TABLE users
(
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email          VARCHAR(255) UNIQUE NOT NULL,
    email_verified BOOLEAN          DEFAULT FALSE,
    role           VARCHAR(50)      DEFAULT 'JOB_SEEKER',
    status         VARCHAR(50)      DEFAULT 'ACTIVE',
    created_at     TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

-- 用户资料表 / User profile table
CREATE TABLE user_profiles
(
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id            UUID REFERENCES users (id) ON DELETE CASCADE,
    full_name          VARCHAR(255),
    avatar_url         VARCHAR(1000),
    phone              VARCHAR(50),
    target_position    VARCHAR(255),
    preferred_location VARCHAR(255),
    created_at         TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id)
);

-- 本地认证凭证表 / Local authentication credentials table
CREATE TABLE user_credentials
(
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id          UUID REFERENCES users (id) ON DELETE CASCADE,
    credential_type  VARCHAR(50)  NOT NULL,
    credential_value VARCHAR(255) NOT NULL,
    last_changed_at  TIMESTAMP,
    created_at       TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, credential_type)
);

-- OAuth绑定表 / OAuth binding table
CREATE TABLE user_oauth_bindings
(
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id          UUID REFERENCES users (id) ON DELETE CASCADE,
    provider         VARCHAR(50)  NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    email            VARCHAR(255),
    display_name     VARCHAR(255),
    avatar_url       VARCHAR(1000),
    access_token     TEXT,
    refresh_token    TEXT,
    expires_at       TIMESTAMP,
    is_primary       BOOLEAN          DEFAULT FALSE,
    created_at       TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (provider, provider_user_id)
);

-- 索引 / Indexes
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_oauth_bindings_user ON user_oauth_bindings (user_id);
CREATE INDEX idx_oauth_bindings_email ON user_oauth_bindings (email);
CREATE INDEX idx_credentials_user ON user_credentials (user_id);
