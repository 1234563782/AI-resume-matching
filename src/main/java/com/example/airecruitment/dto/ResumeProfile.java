package com.example.airecruitment.dto;

import java.util.List;

public record ResumeProfile(
        String name,
        String phone,
        String email,
        List<Education> education,
        List<WorkExperience> workExperience,
        List<String> skills,
        Integer expectedSalaryMin,
        Integer expectedSalaryMax,
        String summary
) {
}
