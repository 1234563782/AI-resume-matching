ALTER TABLE resume ALTER COLUMN embedding TYPE vector(1024);
ALTER TABLE job_description ALTER COLUMN embedding TYPE vector(1024);

DROP INDEX IF EXISTS idx_resume_embedding;
DROP INDEX IF EXISTS idx_job_embedding;

CREATE INDEX IF NOT EXISTS idx_resume_embedding ON resume USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_job_embedding ON job_description USING hnsw (embedding vector_cosine_ops);
