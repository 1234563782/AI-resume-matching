CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS resume (
    id BIGSERIAL PRIMARY KEY,
    candidate_name VARCHAR(128),
    phone VARCHAR(64),
    email VARCHAR(128),
    expected_salary_min INTEGER,
    expected_salary_max INTEGER,
    raw_text TEXT NOT NULL,
    profile_json JSONB NOT NULL,
    summary TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS job_description (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    salary_min INTEGER,
    salary_max INTEGER,
    raw_text TEXT NOT NULL,
    profile_json JSONB NOT NULL,
    summary TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS resume_job_match (
    id BIGSERIAL PRIMARY KEY,
    resume_id BIGINT NOT NULL REFERENCES resume(id) ON DELETE CASCADE,
    job_id BIGINT NOT NULL REFERENCES job_description(id) ON DELETE CASCADE,
    total_score NUMERIC(6, 2) NOT NULL,
    skill_score NUMERIC(6, 2) NOT NULL,
    experience_score NUMERIC(6, 2) NOT NULL,
    education_score NUMERIC(6, 2) NOT NULL,
    project_score NUMERIC(6, 2) NOT NULL,
    salary_score NUMERIC(6, 2) NOT NULL,
    semantic_score NUMERIC(6, 2) NOT NULL,
    match_reason TEXT NOT NULL,
    weakness_reason TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (resume_id, job_id)
);

CREATE INDEX IF NOT EXISTS idx_resume_embedding ON resume USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_job_embedding ON job_description USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_match_job_score ON resume_job_match (job_id, total_score DESC);
