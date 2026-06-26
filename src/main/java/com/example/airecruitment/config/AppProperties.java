package com.example.airecruitment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private final Ai ai = new Ai();
    private final Ocr ocr = new Ocr();

    public Ai getAi() {
        return ai;
    }

    public Ocr getOcr() {
        return ocr;
    }

    public static class Ai {
        private String dashScopeApiKey = "";
        private String chatModel = "qwen-plus";
        private String embeddingModel = "text-embedding-v3";
        private int embeddingDimension = 1536;

        public String getDashScopeApiKey() {
            return dashScopeApiKey;
        }

        public void setDashScopeApiKey(String dashScopeApiKey) {
            this.dashScopeApiKey = dashScopeApiKey;
        }

        public String getChatModel() {
            return chatModel;
        }

        public void setChatModel(String chatModel) {
            this.chatModel = chatModel;
        }

        public String getEmbeddingModel() {
            return embeddingModel;
        }

        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }

        public int getEmbeddingDimension() {
            return embeddingDimension;
        }

        public void setEmbeddingDimension(int embeddingDimension) {
            this.embeddingDimension = embeddingDimension;
        }
    }

    public static class Ocr {
        private boolean enabled;
        private String language = "chi_sim+eng";
        private String dataPath = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getDataPath() {
            return dataPath;
        }

        public void setDataPath(String dataPath) {
            this.dataPath = dataPath;
        }
    }
}
