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
        String prompt = "从下面简历中抽取结构化信息，字段包含 name, phone, email, education, workExperience, projectExperience, caseExperience, skills, capabilities, businessScenarios, achievements, expectedSalaryMin, expectedSalaryMax, summary。"
                + "education 数组字段为 school, degree, major, startDate, endDate。workExperience 数组字段为 company, title, startDate, endDate, skills, description。"
                + "projectExperience 数组字段为 name, role, startDate, endDate, skills, description，主要用于技术/工程类项目。"
                + "caseExperience 数组字段为 name, role, actions, results, capabilities, businessScenarios, description，适用于技术、产品、运营、销售、设计、HR、财务等所有岗位的项目/案例/作品/业务经历。"
                + "capabilities 是候选人的通用能力，businessScenarios 是经历覆盖的行业/业务场景，achievements 是量化成果或荣誉。未知字段用 null 或空数组。简历：\n"
                + resumeText;
        return read(dashScopeClient.chatJson(system, prompt), ResumeProfile.class);
    }

    public JobProfile extractJob(String title, String jdText) {
        String system = "你是招聘系统的 JD 分析器，只输出合法 JSON，不要输出解释。";
        String prompt = "从下面岗位 JD 中抽取结构化信息，字段包含 title, roleType, requiredSkills, preferredSkills, requiredCapabilities, responsibilities, businessScenarios, achievementSignals, industries, minYears, salaryMin, salaryMax, summary。"
                + "roleType 可取 TECH, PRODUCT, OPERATION, SALES, DESIGN, HR, FINANCE, MANAGEMENT, OTHER。requiredCapabilities 是岗位通用能力要求，responsibilities 是工作职责，businessScenarios 是行业/业务场景，achievementSignals 是期望成果或考核指标。"
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
