package com.example.airecruitment.match;

import com.example.airecruitment.dto.JobProfile;
import com.example.airecruitment.dto.JobRecord;
import com.example.airecruitment.dto.MatchRadar;
import com.example.airecruitment.dto.MatchResult;
import com.example.airecruitment.dto.ResumeProfile;
import com.example.airecruitment.dto.ResumeRecord;
import com.example.airecruitment.dto.WorkExperience;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MatchEngineService {
    public MatchResult match(ResumeRecord resume, JobRecord job) {
        ResumeProfile resumeProfile = resume.profile();
        JobProfile jobProfile = job.profile();
        double skillScore = skillScore(resumeProfile.skills(), jobProfile.requiredSkills(), jobProfile.preferredSkills());
        double experienceScore = experienceScore(resumeProfile.workExperience(), jobProfile.minYears());
        double educationScore = educationScore(resumeProfile);
        double projectScore = projectScore(resumeProfile.workExperience(), jobProfile.requiredSkills());
        double salaryScore = salaryScore(resumeProfile.expectedSalaryMin(), resumeProfile.expectedSalaryMax(), jobProfile.salaryMin(), jobProfile.salaryMax());
        double semanticScore = cosineScore(resume.embedding(), job.embedding());
        double totalScore = skillScore * 0.35
                + experienceScore * 0.25
                + educationScore * 0.10
                + projectScore * 0.15
                + salaryScore * 0.10
                + semanticScore * 0.05;
        String matchReason = buildMatchReason(skillScore, experienceScore, semanticScore);
        String weaknessReason = buildWeaknessReason(skillScore, salaryScore, projectScore);
        return new MatchResult(
                resume.id(),
                job.id(),
                round(totalScore),
                round(skillScore),
                round(experienceScore),
                round(educationScore),
                round(projectScore),
                round(salaryScore),
                round(semanticScore),
                matchReason,
                weaknessReason,
                new MatchRadar(round(skillScore), round(experienceScore), round(educationScore), round(projectScore), round(salaryScore), round(semanticScore))
        );
    }

    private double skillScore(List<String> resumeSkills, List<String> requiredSkills, List<String> preferredSkills) {
        Set<String> normalizedResumeSkills = normalize(resumeSkills);
        double required = coverage(normalizedResumeSkills, requiredSkills);
        double preferred = coverage(normalizedResumeSkills, preferredSkills);
        return required * 80 + preferred * 20;
    }

    private double experienceScore(List<WorkExperience> experiences, Integer minYears) {
        if (minYears == null || minYears <= 0) {
            return 80;
        }
        int estimatedYears = Math.min(10, experiences == null ? 0 : experiences.size() * 2);
        return clamp((estimatedYears * 100.0) / minYears);
    }

    private double educationScore(ResumeProfile profile) {
        if (profile.education() == null || profile.education().isEmpty()) {
            return 50;
        }
        return profile.education().stream()
                .map(education -> education.degree() == null ? "" : education.degree())
                .map(String::toLowerCase)
                .mapToDouble(degree -> {
                    if (degree.contains("博士")) {
                        return 100;
                    }
                    if (degree.contains("硕士") || degree.contains("master")) {
                        return 90;
                    }
                    if (degree.contains("本科") || degree.contains("bachelor")) {
                        return 80;
                    }
                    return 60;
                })
                .max()
                .orElse(50);
    }

    private double projectScore(List<WorkExperience> experiences, List<String> requiredSkills) {
        if (experiences == null || experiences.isEmpty()) {
            return 40;
        }
        String combined = experiences.stream()
                .map(experience -> (experience.description() == null ? "" : experience.description()) + " " + String.join(" ", experience.skills() == null ? List.of() : experience.skills()))
                .reduce("", (left, right) -> left + " " + right)
                .toLowerCase(Locale.ROOT);
        long hitCount = normalize(requiredSkills).stream().filter(combined::contains).count();
        return requiredSkills == null || requiredSkills.isEmpty() ? 70 : clamp(hitCount * 100.0 / requiredSkills.size());
    }

    private double salaryScore(Integer resumeMin, Integer resumeMax, Integer jobMin, Integer jobMax) {
        if (resumeMin == null || resumeMax == null || jobMin == null || jobMax == null) {
            return 70;
        }
        int overlap = Math.min(resumeMax, jobMax) - Math.max(resumeMin, jobMin);
        if (overlap >= 0) {
            return 100;
        }
        int gap = Math.abs(overlap);
        int jobRange = Math.max(1, jobMax - jobMin);
        return clamp(100 - gap * 100.0 / jobRange);
    }

    private double cosineScore(double[] left, double[] right) {
        if (left.length == 0 || right.length == 0 || left.length != right.length) {
            return 0;
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return clamp((dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm)) + 1) * 50);
    }

    private double coverage(Set<String> resumeSkills, List<String> targets) {
        if (targets == null || targets.isEmpty()) {
            return 1;
        }
        long hits = normalize(targets).stream().filter(resumeSkills::contains).count();
        return hits * 1.0 / targets.size();
    }

    private Set<String> normalize(List<String> values) {
        Set<String> result = new HashSet<>();
        if (values == null) {
            return result;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                result.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return result;
    }

    private String buildMatchReason(double skillScore, double experienceScore, double semanticScore) {
        return "技能匹配 " + round(skillScore) + "，经验匹配 " + round(experienceScore) + "，语义相关度 " + round(semanticScore) + "。";
    }

    private String buildWeaknessReason(double skillScore, double salaryScore, double projectScore) {
        if (skillScore < 60) {
            return "必备技能覆盖不足，需要人工复核关键技能。";
        }
        if (salaryScore < 60) {
            return "薪资期望与岗位预算存在差距。";
        }
        if (projectScore < 60) {
            return "项目经历与岗位核心技能的直接关联偏弱。";
        }
        return "未发现明显短板。";
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
