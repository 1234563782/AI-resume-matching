package com.example.airecruitment.dto;

public record OriginalResumeFile(
        String filename,
        String contentType,
        byte[] content
) {
}
