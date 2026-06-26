package com.example.airecruitment.repository;

import com.example.airecruitment.dto.JobProfile;
import com.example.airecruitment.dto.JobRecord;
import com.example.airecruitment.dto.ResumeProfile;
import com.example.airecruitment.dto.ResumeRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public final class JdbcMapping {
    private static final Pattern VECTOR_TRIM = Pattern.compile("^\\[|]$");

    private JdbcMapping() {
    }

    public static String vectorLiteral(double[] vector) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(vector[i]);
        }
        return builder.append(']').toString();
    }

    public static double[] parseVector(String vector) {
        String cleaned = VECTOR_TRIM.matcher(vector).replaceAll("");
        if (cleaned.isBlank()) {
            return new double[0];
        }
        return Arrays.stream(cleaned.split(","))
                .mapToDouble(Double::parseDouble)
                .toArray();
    }

    public static ResumeRecord mapResume(ResultSet rs, ObjectMapper objectMapper) throws SQLException {
        try {
            ResumeProfile profile = objectMapper.readValue(rs.getString("profile_json"), ResumeProfile.class);
            return new ResumeRecord(
                    rs.getLong("id"),
                    profile,
                    rs.getString("raw_text"),
                    rs.getString("summary"),
                    parseVector(rs.getString("embedding"))
            );
        } catch (Exception ex) {
            throw new SQLException("简历 JSON 反序列化失败", ex);
        }
    }

    public static JobRecord mapJob(ResultSet rs, ObjectMapper objectMapper) throws SQLException {
        try {
            JobProfile profile = objectMapper.readValue(rs.getString("profile_json"), JobProfile.class);
            return new JobRecord(
                    rs.getLong("id"),
                    profile,
                    rs.getString("raw_text"),
                    rs.getString("summary"),
                    parseVector(rs.getString("embedding"))
            );
        } catch (Exception ex) {
            throw new SQLException("JD JSON 反序列化失败", ex);
        }
    }

    public static ResumeProfile normalize(ResumeProfile profile) {
        return new ResumeProfile(
                profile.name(),
                profile.phone(),
                profile.email(),
                profile.education() == null ? List.of() : profile.education(),
                profile.workExperience() == null ? List.of() : profile.workExperience(),
                profile.projectExperience() == null ? List.of() : profile.projectExperience(),
                profile.skills() == null ? List.of() : profile.skills(),
                profile.expectedSalaryMin(),
                profile.expectedSalaryMax(),
                profile.summary() == null ? "" : profile.summary()
        );
    }

    public static JobProfile normalize(JobProfile profile, String fallbackTitle) {
        return new JobProfile(
                profile.title() == null || profile.title().isBlank() ? fallbackTitle : profile.title(),
                profile.requiredSkills() == null ? List.of() : profile.requiredSkills(),
                profile.preferredSkills() == null ? List.of() : profile.preferredSkills(),
                profile.industries() == null ? List.of() : profile.industries(),
                profile.minYears(),
                profile.salaryMin(),
                profile.salaryMax(),
                profile.summary() == null ? "" : profile.summary()
        );
    }
}
