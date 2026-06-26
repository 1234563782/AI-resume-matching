package com.example.airecruitment.repository;

import com.example.airecruitment.dto.OriginalResumeFile;
import com.example.airecruitment.dto.ResumeProfile;
import com.example.airecruitment.dto.ResumeRecord;
import com.example.airecruitment.dto.ResumeSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.util.ArrayList;
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

    public ResumeRecord save(
            ResumeProfile profile,
            String rawText,
            double[] embedding,
            String originalFilename,
            String originalContentType,
            long originalFileSize,
            byte[] originalFile
    ) {
        try {
            ResumeProfile normalized = JdbcMapping.normalize(profile);
            KeyHolder keyHolder = new GeneratedKeyHolder();
            String json = objectMapper.writeValueAsString(normalized);
            String summary = normalized.summary().isBlank() ? rawText.substring(0, Math.min(rawText.length(), 500)) : normalized.summary();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO resume(candidate_name, phone, email, expected_salary_min, expected_salary_max, raw_text, profile_json, summary, embedding,
                                           original_filename, original_content_type, original_file_size, original_file)
                        VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?::vector, ?, ?, ?, ?)
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
                ps.setString(10, originalFilename);
                ps.setString(11, originalContentType);
                ps.setLong(12, originalFileSize);
                ps.setBytes(13, originalFile);
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

    public List<ResumeRecord> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        String placeholders = placeholders(ids.size());
        return jdbcTemplate.query(
                "SELECT id, raw_text, profile_json::text, summary, embedding::text FROM resume WHERE id IN (" + placeholders + ") ORDER BY id DESC",
                (rs, rowNum) -> JdbcMapping.mapResume(rs, objectMapper),
                ids.toArray()
        );
    }

    public List<ResumeSummary> findSummaries() {
        return jdbcTemplate.query(
                """
                        SELECT id, candidate_name, email, phone, summary, original_filename, original_content_type, original_file_size, created_at
                        FROM resume
                        ORDER BY created_at DESC
                        """,
                (rs, rowNum) -> new ResumeSummary(
                        rs.getLong("id"),
                        rs.getString("candidate_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("summary"),
                        rs.getString("original_filename"),
                        rs.getString("original_content_type"),
                        rs.getObject("original_file_size", Long.class),
                        rs.getTimestamp("created_at").toInstant()
                )
        );
    }

    public List<ResumeSummary> findSummariesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        String placeholders = placeholders(ids.size());
        return jdbcTemplate.query(
                """
                        SELECT id, candidate_name, email, phone, summary, original_filename, original_content_type, original_file_size, created_at
                        FROM resume
                        WHERE id IN (
                        """ + placeholders + ")",
                (rs, rowNum) -> new ResumeSummary(
                        rs.getLong("id"),
                        rs.getString("candidate_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("summary"),
                        rs.getString("original_filename"),
                        rs.getString("original_content_type"),
                        rs.getObject("original_file_size", Long.class),
                        rs.getTimestamp("created_at").toInstant()
                ),
                ids.toArray()
        );
    }

    public OriginalResumeFile findOriginalFileById(long id) {
        List<OriginalResumeFile> files = jdbcTemplate.query(
                """
                        SELECT original_filename, original_content_type, original_file
                        FROM resume
                        WHERE id = ? AND original_file IS NOT NULL
                        """,
                (rs, rowNum) -> new OriginalResumeFile(
                        rs.getString("original_filename"),
                        rs.getString("original_content_type"),
                        rs.getBytes("original_file")
                ),
                id
        );
        if (files.isEmpty()) {
            throw new IllegalArgumentException("这份简历没有保存原始文件，请重新上传后再打开");
        }
        return files.get(0);
    }

    private String placeholders(int size) {
        List<String> placeholders = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            placeholders.add("?");
        }
        return String.join(",", placeholders);
    }
}
