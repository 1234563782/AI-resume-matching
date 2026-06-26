package com.example.airecruitment.dto;

import java.util.List;

public record CaseExperience(
        String name,
        String role,
        List<String> actions,
        List<String> results,
        List<String> capabilities,
        List<String> businessScenarios,
        String description
) {
}
