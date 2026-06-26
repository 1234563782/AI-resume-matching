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
        double projectScore = projectScore(resumeProfile, resume.rawText(), jobProfile.requiredSkills(), jobProfile.preferredSkills());
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

    private double projectScore(ResumeProfile profile, String rawText, List<String> requiredSkills, List<String> preferredSkills) {
        ProjectText projectText = projectText(profile, rawText);
        if (projectText.text().isBlank()) {
            return 40;
        }
        Set<String> required = normalize(requiredSkills);
        Set<String> preferred = normalize(preferredSkills);
        double requiredCoverage = required.isEmpty() ? 0.45 : keywordCoverage(projectText.text(), required);
        double preferredCoverage = preferred.isEmpty() ? 0.35 : keywordCoverage(projectText.text(), preferred);
        double projectCompleteness = projectText.hasStructuredProject() ? structuredProjectCompleteness(profile.projectExperience()) : 0.35;
        double responsibilityCoverage = responsibilityCoverage(projectText.text());
        double complexityScore = complexityScore(projectText.text());
        double score = requiredCoverage * 35
                + preferredCoverage * 15
                + projectCompleteness * 20
                + responsibilityCoverage * 15
                + complexityScore * 15;
        if (!projectText.hasStructuredProject()) {
            return Math.min(clamp(score), 65);
        }
        if (!projectText.hasResponsibilitySignal()) {
            return Math.min(clamp(score), 85);
        }
        return clamp(score);
    }

    private ProjectText projectText(ResumeProfile profile, String rawText) {
        StringBuilder builder = new StringBuilder();
        boolean hasStructuredProject = profile.projectExperience() != null && !profile.projectExperience().isEmpty();
        if (profile.projectExperience() != null) {
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
        builder.append(' ').append(String.join(" ", profile.skills() == null ? List.of() : profile.skills()));
        boolean rawTextFallback = rawText != null && containsAny(rawText.toLowerCase(Locale.ROOT), List.of("项目经历", "项目描述", "个人项目", "开源项目"));
        if (rawTextFallback) {
            builder.append(' ').append(rawText);
        }
        String text = builder.toString().toLowerCase(Locale.ROOT);
        return new ProjectText(text, hasStructuredProject, containsAny(text, responsibilityKeywords()));
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

    private double responsibilityCoverage(String text) {
        return keywordCoverage(text, new HashSet<>(responsibilityKeywords())) / 100.0;
    }

    private double complexityScore(String text) {
        return keywordCoverage(text, new HashSet<>(List.of("架构", "设计", "实现", "优化", "异步", "并发", "检索", "向量", "权限", "缓存", "消息队列", "状态机", "高可用", "性能"))) / 100.0;
    }

    private List<String> responsibilityKeywords() {
        return List.of("负责", "设计", "实现", "开发", "搭建", "优化", "落地", "接入", "对接", "重构", "维护");
    }

    private record ProjectText(String text, boolean hasStructuredProject, boolean hasResponsibilitySignal) {
    }

    private double keywordCoverage(String text, Set<String> keywords) {
        if (keywords.isEmpty()) {
            return 0;
        }
        long hits = keywords.stream().filter(text::contains).count();
        return hits * 100.0 / keywords.size();
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
