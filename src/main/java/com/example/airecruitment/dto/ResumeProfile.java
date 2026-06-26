package com.example.airecruitment.dto;

import java.util.List;

public record ResumeProfile(
        String name,
        String phone,
        String email,
        List<Education> education,
        List<WorkExperience> workExperience,
        List<ProjectExperience> projectExperience,
        List<CaseExperience> caseExperience,
        List<String> skills,
        List<String> capabilities,
        List<String> businessScenarios,
        List<String> achievements,
        Integer expectedSalaryMin,
        Integer expectedSalaryMax,
        String summary
) {
}
