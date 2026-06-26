package com.example.airecruitment.dto;

import java.util.List;

public record ProjectExperience(
        String name,
        String role,
        String startDate,
        String endDate,
        List<String> skills,
        String description
) {
}
