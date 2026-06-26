package com.example.airecruitment.dto;

public record MatchResult(
        Long resumeId,
        Long jobId,
        double totalScore,
        double skillScore,
        double experienceScore,
        double educationScore,
        double projectScore,
        double salaryScore,
        double semanticScore,
        String matchReason,
        String weaknessReason,
        MatchRadar radar
) {
}
