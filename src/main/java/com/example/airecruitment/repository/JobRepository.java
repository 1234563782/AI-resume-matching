package com.example.airecruitment.repository;

import com.example.airecruitment.dto.JobProfile;
import com.example.airecruitment.dto.JobRecord;
import com.example.airecruitment.dto.JobSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JobRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JobRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public JobRecord save(JobProfile profile, String fallbackTitle, String rawText, double[] embedding) {
        try {
            JobProfile normalized = JdbcMapping.normalize(profile, fallbackTitle);
            KeyHolder keyHolder = new GeneratedKeyHolder();
            String json = objectMapper.writeValueAsString(normalized);
            String summary = normalized.summary().isBlank() ? rawText.substring(0, Math.min(rawText.length(), 500)) : normalized.summary();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO job_description(title, salary_min, salary_max, raw_text, profile_json, summary, embedding)
                        VALUES (?, ?, ?, ?, ?::jsonb, ?, ?::vector)
                        """, new String[]{"id"});
                ps.setString(1, normalized.title());
                ps.setObject(2, normalized.salaryMin());
                ps.setObject(3, normalized.salaryMax());
                ps.setString(4, rawText);
                ps.setString(5, json);
                ps.setString(6, summary);
                ps.setString(7, JdbcMapping.vectorLiteral(embedding));
                return ps;
            }, keyHolder);
            Long id = keyHolder.getKeyAs(Long.class);
            return new JobRecord(id, normalized, rawText, summary, embedding);
        } catch (Exception ex) {
            throw new IllegalArgumentException("保存 JD 失败：" + ex.getMessage(), ex);
        }
    }

    public JobRecord findById(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT id, raw_text, profile_json::text, summary, embedding::text FROM job_description WHERE id = ?",
                (rs, rowNum) -> JdbcMapping.mapJob(rs, objectMapper),
                id
        );
    }

    public List<JobSummary> findAllSummaries() {
        return jdbcTemplate.query(
                """
                        SELECT id, title, summary, salary_min, salary_max, created_at
                        FROM job_description
                        ORDER BY created_at DESC
                        """,
                (rs, rowNum) -> new JobSummary(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("summary"),
                        rs.getObject("salary_min", Integer.class),
                        rs.getObject("salary_max", Integer.class),
                        rs.getTimestamp("created_at").toInstant()
                )
        );
    }
}
