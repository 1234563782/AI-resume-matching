package com.example.airecruitment.dto;

import java.time.Instant;

public record JobSummary(
        Long id,
        String title,
        String summary,
        Integer salaryMin,
        Integer salaryMax,
        Instant createdAt
) {
}
