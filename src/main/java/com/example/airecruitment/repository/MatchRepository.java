package com.example.airecruitment.repository;

import com.example.airecruitment.dto.MatchResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MatchRepository {
    private final JdbcTemplate jdbcTemplate;

    public MatchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(MatchResult result) {
        jdbcTemplate.update("""
                INSERT INTO resume_job_match(resume_id, job_id, total_score, skill_score, experience_score, education_score, project_score, salary_score, semantic_score, match_reason, weakness_reason)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (resume_id, job_id) DO UPDATE SET
                    total_score = EXCLUDED.total_score,
                    skill_score = EXCLUDED.skill_score,
                    experience_score = EXCLUDED.experience_score,
                    education_score = EXCLUDED.education_score,
                    project_score = EXCLUDED.project_score,
                    salary_score = EXCLUDED.salary_score,
                    semantic_score = EXCLUDED.semantic_score,
                    match_reason = EXCLUDED.match_reason,
                    weakness_reason = EXCLUDED.weakness_reason,
                    created_at = now()
                """,
                result.resumeId(), result.jobId(), result.totalScore(), result.skillScore(), result.experienceScore(),
                result.educationScore(), result.projectScore(), result.salaryScore(), result.semanticScore(),
                result.matchReason(), result.weaknessReason());
    }
}
