package com.example.airecruitment.dto;

public record ResumeRecord(Long id, ResumeProfile profile, String rawText, String summary, double[] embedding) {
}
