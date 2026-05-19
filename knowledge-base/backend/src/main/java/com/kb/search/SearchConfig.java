package com.kb.search;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

/**
 * [FIX]: 搜索相关配置 - ES 连接 + Embedding 参数 + 混合检索权重
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "search")
public class SearchConfig {

    private EmbeddingConfig embedding = new EmbeddingConfig();
    private HybridConfig hybrid = new HybridConfig();

    @Data
    public static class EmbeddingConfig {
        /** 是否启用向量搜索 */
        private boolean enabled = true;
        /** Embedding 模型名称（智谱AI: embedding-3, OpenAI: text-embedding-3-small） */
        private String model = "embedding-3";
        /** 向量维度（智谱AI: 2048, OpenAI: 1536） */
        private int dimensions = 2048;
        /** Embedding API 地址 */
        private String apiUrl = "https://open.bigmodel.cn/api/paas/v4/embeddings";
    }

    @Data
    public static class HybridConfig {
        /** BM25 全文检索权重 */
        private double keywordWeight = 0.4;
        /** kNN 语义检索权重 */
        private double semanticWeight = 0.6;
    }
}
