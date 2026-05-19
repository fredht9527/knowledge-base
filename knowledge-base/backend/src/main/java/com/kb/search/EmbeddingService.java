package com.kb.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.entity.AiConfig;
import com.kb.service.AiConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * [FIX]: 向量嵌入服务 - 调用 OpenAI Embedding API 生成文本向量
 * 用于将知识库内容、附件文本、聊天记录向量化，存入 ES 的 dense_vector 字段
 * 
 * 修复：使用宽松 SSL 上下文兼容自定义 API 地址，连续失败3次后自动跳过
 */
@Slf4j
@Service
public class EmbeddingService {

    private final SearchConfig searchConfig;
    private final AiConfigService aiConfigService;
    private final ObjectMapper objectMapper;

    // [FIX]: 连续失败计数器，超过阈值后跳过后续 embedding 调用（避免全量同步卡死）
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    // 缓存的 HttpClient（带宽松 SSL）
    private volatile HttpClient cachedClient;

    public EmbeddingService(SearchConfig searchConfig, AiConfigService aiConfigService, ObjectMapper objectMapper) {
        this.searchConfig = searchConfig;
        this.aiConfigService = aiConfigService;
        this.objectMapper = objectMapper;
    }

    /**
     * [FIX]: 获取带宽松 SSL 的 HttpClient
     * 某些自定义 API 地址（代理/中转）的 SSL 证书不被 Java 默认信任
     */
    private HttpClient getHttpClient() {
        if (cachedClient != null) return cachedClient;
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            }, new SecureRandom());

            cachedClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .sslContext(sslContext)
                    .build();
            return cachedClient;
        } catch (Exception e) {
            log.error("创建 SSL 宽松 HttpClient 失败，使用默认: {}", e.getMessage());
            cachedClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            return cachedClient;
        }
    }

    /**
     * 生成文本的向量嵌入
     * 复用 AI 配置中的 api_key 和 api_url，确保与聊天使用同一服务商
     *
     * @param text 待嵌入的文本（截断到 8000 字符，避免 token 超限）
     * @return 1536维 float 数组，生成失败返回 null
     */
    public float[] generateEmbedding(String text) {
        if (!searchConfig.getEmbedding().isEnabled()) {
            return null;
        }
        // [FIX]: 连续失败超过阈值后直接跳过，避免全量同步卡死
        if (consecutiveFailures.get() >= MAX_CONSECUTIVE_FAILURES) {
            return null;
        }
        if (text == null || text.isBlank()) {
            return null;
        }
        String input = text.length() > 8000 ? text.substring(0, 8000) : text;

        try {
            AiConfig config = aiConfigService.getConfig();
            String apiKey = config.getApiKey();
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("未配置 API Key，跳过向量生成");
                return null;
            }

            // [FIX]: 根据 API 地址自动识别服务商，选择对应的 embedding 模型、URL 和维度
            String chatApiUrl = config.getApiUrl();
            String embeddingApiUrl;
            String embeddingModel;
            int embeddingDims;

            if (chatApiUrl != null && chatApiUrl.contains("bigmodel.cn")) {
                // 智谱AI（GLM）
                embeddingApiUrl = chatApiUrl.replaceAll("/chat/completions.*", "/embeddings");
                embeddingModel = "embedding-3";
                embeddingDims = 2048;
            } else if (chatApiUrl != null && chatApiUrl.contains("openai.com")) {
                // OpenAI 官方
                embeddingApiUrl = chatApiUrl.replaceAll("/chat/completions.*", "/embeddings");
                embeddingModel = "text-embedding-3-small";
                embeddingDims = 1536;
            } else if (chatApiUrl != null && !chatApiUrl.isBlank()) {
                // 其他兼容服务，从 chat URL 推导 embedding URL
                embeddingApiUrl = chatApiUrl.replaceAll("/chat/completions.*", "/embeddings");
                embeddingModel = "text-embedding-3-small";
                embeddingDims = 1536;
            } else {
                // 无 chat URL，使用搜索配置默认值
                embeddingApiUrl = searchConfig.getEmbedding().getApiUrl();
                embeddingModel = searchConfig.getEmbedding().getModel();
                embeddingDims = searchConfig.getEmbedding().getDimensions();
            }

            log.debug("Embedding 服务商识别: url={}, model={}, dims={}", embeddingApiUrl, embeddingModel, embeddingDims);

            // [FIX]: 智谱AI embedding-3 不支持 dimensions 参数，需去掉
            String requestBody;
            if (chatApiUrl != null && chatApiUrl.contains("bigmodel.cn")) {
                requestBody = String.format(
                        "{\"model\":\"%s\",\"input\":%s}",
                        embeddingModel,
                        escapeJson(input)
                );
            } else {
                requestBody = String.format(
                        "{\"model\":\"%s\",\"input\":%s,\"dimensions\":%d}",
                        embeddingModel,
                        escapeJson(input),
                        embeddingDims
                );
            }

            HttpClient client = getHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(embeddingApiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Embedding API 调用失败: status={}, body={}", response.statusCode(),
                        response.body().substring(0, Math.min(200, response.body().length())));
                consecutiveFailures.incrementAndGet();
                return null;
            }

            // 解析响应
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode embeddingNode = root.path("data").path(0).path("embedding");
            if (embeddingNode.isMissingNode() || !embeddingNode.isArray()) {
                log.error("Embedding API 返回格式异常: {}", response.body().substring(0, Math.min(200, response.body().length())));
                return null;
            }

            // [FIX]: 从实际返回的向量长度动态分配数组，兼容不同维度
            int actualDims = embeddingNode.size();
            float[] embedding = new float[actualDims];
            int i = 0;
            for (JsonNode node : embeddingNode) {
                embedding[i++] = (float) node.asDouble();
            }

            // [FIX]: 成功后重置失败计数
            consecutiveFailures.set(0);
            log.debug("成功生成 {} 维向量", i);
            return embedding;

        } catch (Exception e) {
            consecutiveFailures.incrementAndGet();
            log.error("生成向量嵌入失败({}/{}): {}", consecutiveFailures.get(), MAX_CONSECUTIVE_FAILURES, e.getMessage());
            return null;
        }
    }

    /**
     * [FIX]: 重置失败计数，允许重新尝试 embedding
     */
    public void resetFailures() {
        consecutiveFailures.set(0);
    }

    /** JSON 字符串转义 */
    private String escapeJson(String s) {
        if (s == null) return "\"\"";
        String escaped = s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }
}
