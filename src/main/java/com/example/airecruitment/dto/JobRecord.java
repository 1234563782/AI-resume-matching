package com.example.airecruitment.dto;

public record JobRecord(Long id, JobProfile profile, String rawText, String summary, double[] embedding) {
}
