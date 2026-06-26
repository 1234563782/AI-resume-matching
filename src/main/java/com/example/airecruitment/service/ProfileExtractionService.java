package com.example.airecruitment.service;

import com.example.airecruitment.ai.DashScopeClient;
import com.example.airecruitment.dto.JobProfile;
import com.example.airecruitment.dto.ResumeProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class ProfileExtractionService {
    private final DashScopeClient dashScopeClient;
    private final ObjectMapper objectMapper;

    public ProfileExtractionService(DashScopeClient dashScopeClient, ObjectMapper objectMapper) {
        this.dashScopeClient = dashScopeClient;
        this.objectMapper = objectMapper;
    }

    public ResumeProfile extractResume(String resumeText) {
        String system = "你是招聘系统的信息抽取器，只输出合法 JSON，不要输出解释。";
        String prompt = "从下面简历中抽取结构化信息，字段包含 name, phone, email, education, workExperience, skills, expectedSalaryMin, expectedSalaryMax, summary。"
                + "education 数组字段为 school, degree, major, startDate, endDate。workExperience 数组字段为 company, title, startDate, endDate, skills, description。未知字段用 null 或空数组。简历：\n"
                + resumeText;
        return read(dashScopeClient.chatJson(system, prompt), ResumeProfile.class);
    }

    public JobProfile extractJob(String title, String jdText) {
        String system = "你是招聘系统的 JD 分析器，只输出合法 JSON，不要输出解释。";
        String prompt = "从下面岗位 JD 中抽取结构化信息，字段包含 title, requiredSkills, preferredSkills, industries, minYears, salaryMin, salaryMax, summary。"
                + "title 如无法判断则使用：" + title + "。未知字段用 null 或空数组。JD：\n" + jdText;
        return read(dashScopeClient.chatJson(system, prompt), JobProfile.class);
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            throw new IllegalArgumentException("AI 返回的 JSON 无法解析：" + json, ex);
        }
    }
}
