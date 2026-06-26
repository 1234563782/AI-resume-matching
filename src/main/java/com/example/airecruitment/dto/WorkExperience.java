package com.example.airecruitment.dto;

import java.util.List;

public record WorkExperience(
        String company,
        String title,
        String startDate,
        String endDate,
        List<String> skills,
        String description
) {
}
