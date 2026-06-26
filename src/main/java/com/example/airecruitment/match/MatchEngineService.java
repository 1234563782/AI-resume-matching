package com.example.airecruitment.match;

import com.example.airecruitment.dto.*;

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
        double projectScore = caseRelevanceScore(resumeProfile, resume.rawText(), jobProfile);
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

    private double caseRelevanceScore(ResumeProfile profile, String rawText, JobProfile jobProfile) {
        CaseText caseText = caseText(profile, rawText);
        if (caseText.text().isBlank()) {
            return 40;
        }
        double responsibilityCoverage = keywordCoverage(caseText.text(), normalize(jobProfile.responsibilities()));
        double capabilityCoverage = keywordCoverage(caseText.text(), merge(normalize(jobProfile.requiredCapabilities()), normalize(jobProfile.requiredSkills())));
        double scenarioCoverage = keywordCoverage(caseText.text(), merge(normalize(jobProfile.businessScenarios()), normalize(jobProfile.industries())));
        double achievementCoverage = keywordCoverage(caseText.text(), normalize(jobProfile.achievementSignals()));
        double completeness = caseText.hasStructuredCase() ? structuredCaseCompleteness(profile) : 0.35;
        double score = responsibilityCoverage * 30
                + capabilityCoverage * 25
                + scenarioCoverage * 15
                + achievementCoverage * 15
                + completeness * 15;
        if (!caseText.hasStructuredCase()) {
            return Math.min(clamp(score), 65);
        }
        if (responsibilityCoverage < 0.2 && capabilityCoverage < 0.2) {
            return Math.min(clamp(score), 75);
        }
        if (scenarioCoverage == 0 && achievementCoverage == 0) {
            return Math.min(clamp(score), 85);
        }
        return clamp(score);
    }

    private CaseText caseText(ResumeProfile profile, String rawText) {
        StringBuilder builder = new StringBuilder();
        boolean hasStructuredCase = false;
        if (profile.caseExperience() != null && !profile.caseExperience().isEmpty()) {
            hasStructuredCase = true;
            for (CaseExperience item : profile.caseExperience()) {
                builder.append(' ')
                        .append(item.name() == null ? "" : item.name())
                        .append(' ')
                        .append(item.role() == null ? "" : item.role())
                        .append(' ')
                        .append(item.description() == null ? "" : item.description())
                        .append(' ')
                        .append(String.join(" ", item.actions() == null ? List.of() : item.actions()))
                        .append(' ')
                        .append(String.join(" ", item.results() == null ? List.of() : item.results()))
                        .append(' ')
                        .append(String.join(" ", item.capabilities() == null ? List.of() : item.capabilities()))
                        .append(' ')
                        .append(String.join(" ", item.businessScenarios() == null ? List.of() : item.businessScenarios()));
            }
        }
        if (profile.projectExperience() != null && !profile.projectExperience().isEmpty()) {
            hasStructuredCase = true;
            for (ProjectExperience project : profile.projectExperience()) {
                builder.append(' ')
                        .append(project.name() == null ? "" : project.name())
                        .append(' ')
                        .append(project.role() == null ? "" : project.role())
                        .append(' ')
                        .append(project.description() == null ? "" : project.description())
                        .append(' ')
                        .append(String.join(" ", project.skills() == null ? List.of() : project.skills()));
            }
        }
        if (profile.workExperience() != null) {
            for (WorkExperience experience : profile.workExperience()) {
                builder.append(' ')
                        .append(experience.description() == null ? "" : experience.description())
                        .append(' ')
                        .append(String.join(" ", experience.skills() == null ? List.of() : experience.skills()));
            }
        }
        builder.append(' ').append(String.join(" ", profile.skills() == null ? List.of() : profile.skills()))
                .append(' ').append(String.join(" ", profile.capabilities() == null ? List.of() : profile.capabilities()))
                .append(' ').append(String.join(" ", profile.businessScenarios() == null ? List.of() : profile.businessScenarios()))
                .append(' ').append(String.join(" ", profile.achievements() == null ? List.of() : profile.achievements()));
        if (rawText != null && containsAny(rawText.toLowerCase(Locale.ROOT), List.of("项目经历", "项目描述", "个人项目", "开源项目", "案例", "作品", "业绩", "成果"))) {
            builder.append(' ').append(rawText);
        }
        return new CaseText(builder.toString().toLowerCase(Locale.ROOT), hasStructuredCase);
    }

    private double structuredCaseCompleteness(ResumeProfile profile) {
        double caseScore = structuredCaseExperienceCompleteness(profile.caseExperience());
        double projectScore = structuredProjectCompleteness(profile.projectExperience());
        return Math.max(caseScore, projectScore);
    }

    private double structuredCaseExperienceCompleteness(List<CaseExperience> cases) {
        if (cases == null || cases.isEmpty()) {
            return 0.35;
        }
        double total = 0;
        for (CaseExperience item : cases) {
            double score = 0.2;
            if (item.name() != null && !item.name().isBlank()) {
                score += 0.15;
            }
            if (item.description() != null && item.description().length() >= 30) {
                score += 0.2;
            }
            if (item.actions() != null && !item.actions().isEmpty()) {
                score += 0.2;
            }
            if (item.results() != null && !item.results().isEmpty()) {
                score += 0.15;
            }
            if (item.capabilities() != null && !item.capabilities().isEmpty()) {
                score += 0.1;
            }
            total += Math.min(score, 1);
        }
        return Math.min(0.9, total / cases.size());
    }

    private double structuredProjectCompleteness(List<ProjectExperience> projects) {
        if (projects == null || projects.isEmpty()) {
            return 0.35;
        }
        double total = 0;
        for (ProjectExperience project : projects) {
            double score = 0.2;
            if (project.name() != null && !project.name().isBlank()) {
                score += 0.2;
            }
            if (project.description() != null && project.description().length() >= 30) {
                score += 0.25;
            }
            if (project.skills() != null && !project.skills().isEmpty()) {
                score += 0.2;
            }
            if (project.role() != null && !project.role().isBlank()) {
                score += 0.15;
            }
            total += Math.min(score, 1);
        }
        return Math.min(0.9, total / projects.size());
    }

    private record CaseText(String text, boolean hasStructuredCase) {
    }

    private double keywordCoverage(String text, Set<String> keywords) {
        if (keywords.isEmpty()) {
            return 0;
        }
        long hits = keywords.stream().filter(text::contains).count();
        return hits * 1.0 / keywords.size();
    }

    private Set<String> merge(Set<String> left, Set<String> right) {
        Set<String> result = new HashSet<>(left);
        result.addAll(right);
        return result;
    }

    private boolean containsAny(String text, List<String> keywords) {
        String normalizedText = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return keywords.stream().anyMatch(normalizedText::contains);
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
