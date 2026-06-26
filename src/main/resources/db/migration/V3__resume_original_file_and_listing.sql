ALTER TABLE resume ADD COLUMN IF NOT EXISTS original_filename VARCHAR(255);
ALTER TABLE resume ADD COLUMN IF NOT EXISTS original_content_type VARCHAR(160);
ALTER TABLE resume ADD COLUMN IF NOT EXISTS original_file_size BIGINT;
ALTER TABLE resume ADD COLUMN IF NOT EXISTS original_file BYTEA;

CREATE INDEX IF NOT EXISTS idx_resume_created_at ON resume (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_job_created_at ON job_description (created_at DESC);
