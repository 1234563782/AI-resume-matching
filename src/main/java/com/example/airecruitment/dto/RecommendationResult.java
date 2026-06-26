package com.example.airecruitment.dto;

public record RecommendationResult(
        Long resumeId,
        Long jobId,
        ResumeSummary resume,
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
    public static RecommendationResult from(MatchResult match, ResumeSummary resume) {
        return new RecommendationResult(
                match.resumeId(),
                match.jobId(),
                resume,
                match.totalScore(),
                match.skillScore(),
                match.experienceScore(),
                match.educationScore(),
                match.projectScore(),
                match.salaryScore(),
                match.semanticScore(),
                match.matchReason(),
                match.weaknessReason(),
                match.radar()
        );
    }
}
