-- ==========================================
-- V7__drop_unused_resumes_table.sql
-- 删除废弃的 resumes 表 / Drop unused resumes table
-- 该表是老版本设计的单表结构，现已被 resume_groups + resume_versions 替代
-- This table is from the old single-table design, now replaced by resume_groups + resume_versions
-- ==========================================

-- 删除 resumes 表（如果存在）/ Drop resumes table (if exists)
DROP TABLE IF EXISTS resumes;

-- 添加注释说明 / Add table comments
COMMENT ON TABLE resume_groups IS '简历组表 - 代表一份简历的整体概念（取代旧版 resumes 表）/ Resume group table representing the overall concept of a resume (replaces old resumes table)';
COMMENT ON TABLE resume_versions IS '简历版本表 - 存储原版、转换版、AI版 / Resume version table storing original, converted, and AI-optimized versions';
