package com.example.airecruitment.service;

import com.example.airecruitment.ai.DashScopeClient;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {
    private final DashScopeClient dashScopeClient;

    public EmbeddingService(DashScopeClient dashScopeClient) {
        this.dashScopeClient = dashScopeClient;
    }

    public double[] embedResume(String text) {
        return dashScopeClient.embed("简历摘要：" + text);
    }

    public double[] embedJob(String text) {
        return dashScopeClient.embed("岗位需求：" + text);
    }
}
