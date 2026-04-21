-- V8__add_resume_parse_status.sql
-- Add parse_status and parse_error_message to resume_versions table

ALTER TABLE resume_versions 
ADD COLUMN parse_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

ALTER TABLE resume_versions 
ADD COLUMN parse_error_message TEXT;

CREATE INDEX idx_resume_versions_parse_status ON resume_versions(parse_status);
