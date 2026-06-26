package com.example.airecruitment.ai;

import com.example.airecruitment.config.AppProperties;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class DashScopeClient {
    private static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    private final RestClient restClient;
    private final AppProperties properties;

    public DashScopeClient(RestClient restClient, AppProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public String chatJson(String systemPrompt, String userPrompt) {
        ensureApiKey();
        Map<String, Object> body = Map.of(
                "model", properties.getAi().getChatModel(),
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );
        ChatResponse response = restClient.post()
                .uri(BASE_URL + "/chat/completions")
                .header("Authorization", "Bearer " + properties.getAi().getDashScopeApiKey())
                .body(body)
                .retrieve()
                .body(ChatResponse.class);
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("DashScope 未返回有效文本");
        }
        return response.choices().get(0).message().content();
    }

    public double[] embed(String text) {
        ensureApiKey();
        Map<String, Object> body = Map.of(
                "model", properties.getAi().getEmbeddingModel(),
                "input", text
        );
        EmbeddingResponse response = restClient.post()
                .uri(BASE_URL + "/embeddings")
                .header("Authorization", "Bearer " + properties.getAi().getDashScopeApiKey())
                .body(body)
                .retrieve()
                .body(EmbeddingResponse.class);
        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new IllegalStateException("DashScope 未返回有效向量");
        }
        List<Double> vector = response.data().get(0).embedding();
        double[] result = new double[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            result[i] = vector.get(i);
        }
        return result;
    }

    private void ensureApiKey() {
        if (properties.getAi().getDashScopeApiKey().isBlank()) {
            throw new IllegalStateException("请配置 DASHSCOPE_API_KEY 环境变量");
        }
    }

    public record ChatResponse(List<Choice> choices) {
    }

    public record Choice(Message message) {
    }

    public record Message(String content) {
    }

    public record EmbeddingResponse(List<EmbeddingData> data) {
    }

    public record EmbeddingData(List<Double> embedding) {
    }
}
