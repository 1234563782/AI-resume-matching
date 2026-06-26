package com.example.airecruitment.dto;

import java.util.List;

public record JobProfile(
        String title,
        String roleType,
        List<String> requiredSkills,
        List<String> preferredSkills,
        List<String> requiredCapabilities,
        List<String> responsibilities,
        List<String> businessScenarios,
        List<String> achievementSignals,
        List<String> industries,
        Integer minYears,
        Integer salaryMin,
        Integer salaryMax,
        String summary
) {
}
