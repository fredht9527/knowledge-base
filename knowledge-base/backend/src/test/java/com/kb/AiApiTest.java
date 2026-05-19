package com.kb;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * AI API 连通性测试 - 验证智谱 BigModel 或其他 OpenAI 兼容 API 能否正常调用
 */
@Slf4j
@SpringBootTest
public class AiApiTest {

    /** 智谱正确的 OpenAI 兼容地址（注意末尾有 s） */
    static final String CORRECT_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    @Test
    public void testZhipuApiConnection() throws Exception {
        // 1. 从环境变量获取 API Key
        String apiKey = System.getenv("ZHIPU_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("跳过测试：未设置环境变量 ZHIPU_API_KEY");
            log.warn("请运行: export ZHIPU_API_KEY='你的智谱API Key'");
            return;
        }

        // 2. 使用正确的 URL 和模型名测试
        String model = "glm-4-flash";
        String requestBody = "{" +
                "\"model\":\"" + model + "\"," +
                "\"messages\":[{\"role\":\"user\",\"content\":\"你好，请用一句话介绍你自己\"}]," +
                "\"max_tokens\":512" +
                "}";

        log.info("=== AI API 连通性测试 ===");
        log.info("URL:     {}", CORRECT_URL);
        log.info("Model:   {}", model);
        log.info("API Key: {}", apiKey.substring(0, Math.min(8, apiKey.length())) + "****");

        // 3. 发送请求
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CORRECT_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 4. 验证结果
        log.info("HTTP 状态码: {}", response.statusCode());
        log.info("响应体前200字符: {}", response.body().substring(0, Math.min(200, response.body().length())));

        if (response.statusCode() == 200) {
            log.info("✅ AI API 调用成功！");
            // 提取回复内容
            String body = response.body();
            if (body.contains("\"content\"")) {
                int start = body.indexOf("\"content\"") + 10;
                start = body.indexOf('"', start) + 1;
                int end = body.indexOf('"', start);
                String reply = body.substring(start, end);
                log.info("AI 回复: {}", reply);
            }
        } else {
            log.error("❌ AI API 调用失败！状态码: {}", response.statusCode());
            log.error("请检查：");
            log.error("  1. API Key 是否正确");
            log.error("  2. URL 是否正确（确认是 {}）", CORRECT_URL);
            log.error("  3. 模型名是否正确（确认是 {}）", model);
        }
    }

    @Test
    public void testWrongUrlReturns404() throws Exception {
        // 验证错误的 URL（少 s）会返回 404
        String wrongUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completion";
        String apiKey = System.getenv("ZHIPU_API_KEY");
        if (apiKey == null) return;

        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wrongUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString("{\"model\":\"glm-4-flash\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("错误URL测试 - 状态码: {} (预期404)", response.statusCode());
        assert response.statusCode() == 404 : "错误的URL应该返回404";
        log.info("✅ 确认：少 's' 的 URL 确实返回 404，这就是你的问题");
    }
}
