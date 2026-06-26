package com.example.airecruitment.dto;

import java.time.Instant;

public record ResumeSummary(
        Long id,
        String candidateName,
        String email,
        String phone,
        String summary,
        String originalFilename,
        String originalContentType,
        Long originalFileSize,
        Instant createdAt
) {
}
