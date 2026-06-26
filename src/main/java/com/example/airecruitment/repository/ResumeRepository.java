package com.example.airecruitment.repository;

import com.example.airecruitment.dto.ResumeProfile;
import com.example.airecruitment.dto.ResumeRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ResumeRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ResumeRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public ResumeRecord save(ResumeProfile profile, String rawText, double[] embedding) {
        try {
            ResumeProfile normalized = JdbcMapping.normalize(profile);
            KeyHolder keyHolder = new GeneratedKeyHolder();
            String json = objectMapper.writeValueAsString(normalized);
            String summary = normalized.summary().isBlank() ? rawText.substring(0, Math.min(rawText.length(), 500)) : normalized.summary();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO resume(candidate_name, phone, email, expected_salary_min, expected_salary_max, raw_text, profile_json, summary, embedding)
                        VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?::vector)
                        """, new String[]{"id"});
                ps.setString(1, normalized.name());
                ps.setString(2, normalized.phone());
                ps.setString(3, normalized.email());
                ps.setObject(4, normalized.expectedSalaryMin());
                ps.setObject(5, normalized.expectedSalaryMax());
                ps.setString(6, rawText);
                ps.setString(7, json);
                ps.setString(8, summary);
                ps.setString(9, JdbcMapping.vectorLiteral(embedding));
                return ps;
            }, keyHolder);
            Long id = keyHolder.getKeyAs(Long.class);
            return new ResumeRecord(id, normalized, rawText, summary, embedding);
        } catch (Exception ex) {
            throw new IllegalArgumentException("保存简历失败：" + ex.getMessage(), ex);
        }
    }

    public ResumeRecord findById(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT id, raw_text, profile_json::text, summary, embedding::text FROM resume WHERE id = ?",
                (rs, rowNum) -> JdbcMapping.mapResume(rs, objectMapper),
                id
        );
    }

    public List<ResumeRecord> findAll() {
        return jdbcTemplate.query(
                "SELECT id, raw_text, profile_json::text, summary, embedding::text FROM resume ORDER BY id DESC",
                (rs, rowNum) -> JdbcMapping.mapResume(rs, objectMapper)
        );
    }
}
