package com.example.airecruitment.dto;

import java.util.List;

public record JobProfile(
        String title,
        List<String> requiredSkills,
        List<String> preferredSkills,
        List<String> industries,
        Integer minYears,
        Integer salaryMin,
        Integer salaryMax,
        String summary
) {
}
