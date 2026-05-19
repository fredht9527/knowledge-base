package com.kb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kb.dto.PageRequest;
import com.kb.dto.PageResponse;
import com.kb.entity.AiConfig;
import com.kb.entity.ChatMessage;
import com.kb.entity.ChatSession;
import com.kb.mapper.ChatMessageMapper;
import com.kb.mapper.ChatSessionMapper;
import com.kb.search.SearchDocument;
import com.kb.search.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI对话业务逻辑 - 会话管理、消息管理、AI对话调用
 */
@Slf4j
@Service
public class ChatService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    // [FIX]: 注入 SearchService，聊天消息同步到 ES
    private final SearchService searchService;

    public ChatService(ChatSessionMapper sessionMapper, ChatMessageMapper messageMapper, SearchService searchService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.searchService = searchService;
    }

    // ==================== 会话管理 ====================

    /** 创建新会话 */
    @Transactional
    public ChatSession createSession(String model) {
        ChatSession session = new ChatSession();
        session.setTitle("新对话");
        session.setStatus(0);
        session.setModel(model != null ? model : "gpt-3.5-turbo");
        sessionMapper.insert(session);
        // [FIX]: 同步会话到 ES
        searchService.indexSession(session.getId(), session.getTitle(), session.getStatus(),
                session.getModel(), formatDateTime(session.getCreatedAt()), formatDateTime(session.getUpdatedAt()));
        log.info("创建新会话: id={}, model={}", session.getId(), session.getModel());
        return session;
    }

    /**
     * [FIX]: 获取会话列表 - 走 ES，不再查 MySQL
     */
    public PageResponse<ChatSession> listSessions(PageRequest req, Integer status) {
        try {
            List<SearchDocument> docs = searchService.searchByTypeWithFilter("session", null, null, status, req.getPage(), req.getSize());
            long total = searchService.countByTypeWithFilter("session", null, null, status);
            List<ChatSession> sessions = docs.stream().map(this::docToSession).collect(Collectors.toList());
            return PageResponse.of(sessions, total, req.getPage(), req.getSize());
        } catch (Exception e) {
            log.warn("ES 会话列表失败，降级 MySQL: {}", e.getMessage());
            IPage<ChatSession> page = new Page<>(req.getPage(), req.getSize());
            LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<ChatSession>()
                    .eq(status != null, ChatSession::getStatus, status)
                    .orderByDesc(ChatSession::getUpdatedAt);
            sessionMapper.selectPage(page, wrapper);
            return PageResponse.from(page, page.getRecords());
        }
    }

    /**
     * [FIX]: 搜索会话 - 走 ES，不再查 MySQL
     */
    public PageResponse<ChatSession> searchSessions(PageRequest req, String keyword) {
        try {
            List<SearchDocument> docs = searchService.searchByTypeWithFilter("session", keyword, null, 0, req.getPage(), req.getSize());
            long total = searchService.countByTypeWithFilter("session", keyword, null, 0);
            List<ChatSession> sessions = docs.stream().map(this::docToSession).collect(Collectors.toList());
            return PageResponse.of(sessions, total, req.getPage(), req.getSize());
        } catch (Exception e) {
            log.warn("ES 会话搜索失败，降级 MySQL: {}", e.getMessage());
            IPage<ChatSession> page = new Page<>(req.getPage(), req.getSize());
            LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<ChatSession>()
                    .like(ChatSession::getTitle, keyword)
                    .eq(ChatSession::getStatus, 0)
                    .orderByDesc(ChatSession::getUpdatedAt);
            sessionMapper.selectPage(page, wrapper);
            return PageResponse.from(page, page.getRecords());
        }
    }

    /**
     * [FIX]: ES 文档转 ChatSession
     */
    private ChatSession docToSession(SearchDocument doc) {
        if (doc == null) return null;
        ChatSession s = new ChatSession();
        s.setId(doc.getOriginalId());
        s.setTitle(doc.getTitle());
        s.setStatus(doc.getStatus() != null ? doc.getStatus() : 0);
        s.setModel(doc.getModel());
        try {
            if (doc.getCreatedAt() != null) s.setCreatedAt(LocalDateTime.parse(doc.getCreatedAt(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            if (doc.getUpdatedAt() != null) s.setUpdatedAt(LocalDateTime.parse(doc.getUpdatedAt(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } catch (Exception e) { /* 日期解析失败不影响主流程 */ }
        return s;
    }

    /** 更新会话标题 */
    public void updateTitle(Long sessionId, String title) {
        ChatSession s = sessionMapper.selectById(sessionId);
        if (s != null) {
            s.setTitle(title);
            s.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(s);
            // [FIX]: 同步更新 ES
            searchService.indexSession(s.getId(), s.getTitle(), s.getStatus(),
                    s.getModel(), formatDateTime(s.getCreatedAt()), formatDateTime(s.getUpdatedAt()));
        }
    }

    /** 删除会话（软删除） */
    public void deleteSession(Long sessionId) {
        ChatSession s = sessionMapper.selectById(sessionId);
        if (s != null) {
            s.setStatus(2);
            s.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(s);
            // [FIX]: 同步更新 ES 状态
            searchService.indexSession(s.getId(), s.getTitle(), s.getStatus(),
                    s.getModel(), formatDateTime(s.getCreatedAt()), formatDateTime(s.getUpdatedAt()));
        }
    }

    /** 归档/取消归档会话 */
    public void archiveSession(Long sessionId, boolean archive) {
        ChatSession s = sessionMapper.selectById(sessionId);
        if (s != null) {
            s.setStatus(archive ? 1 : 0);
            s.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(s);
            // [FIX]: 同步更新 ES 状态
            searchService.indexSession(s.getId(), s.getTitle(), s.getStatus(),
                    s.getModel(), formatDateTime(s.getCreatedAt()), formatDateTime(s.getUpdatedAt()));
        }
    }

    // ==================== 消息管理 ====================

    /**
     * [FIX]: 获取会话的所有消息 - 优先 ES，ES 数据不完整时降级 MySQL
     */
    public List<ChatMessage> getMessages(Long sessionId) {
        try {
            List<SearchDocument> docs = searchService.getMessagesBySession(sessionId);
            // [FIX]: ES 可能因索引失败导致数据不完整，查 MySQL 做交叉校验
            long mysqlCount = messageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSessionId, sessionId));
            if (docs.size() >= mysqlCount) {
                return docs.stream().map(this::docToMessage).collect(Collectors.toList());
            }
            log.warn("ES 数据不完整: sessionId={}, es={}, mysql={}, 降级 MySQL", sessionId, docs.size(), mysqlCount);
        } catch (Exception e) {
            log.warn("ES 获取消息失败，降级 MySQL: {}", e.getMessage());
        }
        return messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreatedAt));
    }

    /** [FIX]: 从数据库删除指定消息 */
    public void deleteMessage(Long msgId) {
        if (msgId == null) return;
        // 从 MySQL 删除
        messageMapper.deleteById(msgId);
        // 从 ES 删除
        try {
            searchService.deleteDocument("chat", msgId);
        } catch (Exception e) {
            log.warn("ES 删除消息失败: msgId={}, error={}", msgId, e.getMessage());
        }
    }

    /**
     * [FIX]: ES 文档转 ChatMessage
     */
    private ChatMessage docToMessage(SearchDocument doc) {
        if (doc == null) return null;
        ChatMessage m = new ChatMessage();
        m.setId(doc.getOriginalId());
        m.setSessionId(doc.getKnowledgeId()); // knowledgeId 存的是 sessionId
        // [FIX]: 优先用 role 字段，兼容旧数据从 title 反推
        if (doc.getRole() != null) {
            m.setRole(doc.getRole());
        } else {
            m.setRole("用户消息".equals(doc.getTitle()) ? "user" : "assistant");
        }
        m.setContent(doc.getContent());
        m.setThinking(doc.getThinking()); // [FIX]: 恢复 thinking
        // [FIX]: 恢复 imageUrls，否则历史消息中的图片无法回显
        m.setImageUrls(doc.getImageUrls());
        // [FIX]: 恢复 attachmentIds，否则历史消息中的附件无法回显
        m.setAttachmentIds(doc.getAttachmentIds());
        m.setCreatedAt(parseDateTime(doc.getCreatedAt()));
        return m;
    }

    /** 保存用户消息（图片URL和附件ID保存到数据库和ES） */
    public ChatMessage saveUserMessage(Long sessionId, String content, List<String> imageUrls, List<Long> attachmentIds) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole("user");
        msg.setContent(content);
        // [FIX]: 将图片 URL 列表保存为 JSON 字符串
        if (imageUrls != null && !imageUrls.isEmpty()) {
            try {
                String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(imageUrls);
                msg.setImageUrls(json);
            } catch (Exception e) {
                msg.setImageUrls("[]");
            }
        }
        // [FIX]: 将附件 ID 列表保存为 JSON 字符串
        if (attachmentIds != null && !attachmentIds.isEmpty()) {
            try {
                String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(attachmentIds);
                msg.setAttachmentIds(json);
            } catch (Exception e) {
                msg.setAttachmentIds("[]");
            }
        }
        messageMapper.insert(msg);
        // [FIX]: 同步用户消息到 ES（含 imageUrls、attachmentIds、createdAt）
        searchService.indexChatMessage(msg.getId(), sessionId, "user", content, null, msg.getImageUrls(), msg.getAttachmentIds(), msg.getCreatedAt().toString());
        return msg;
    }

    /** 保存AI回复消息 */
    public ChatMessage saveAssistantMessage(Long sessionId, String content, int tokensUsed, String thinking) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole("assistant");
        msg.setContent(content);
        // [FIX]: thinking 截断保护，防止超长内容导致 MEDIUMTEXT 也溢出
        if (thinking != null && thinking.length() > 60000) {
            thinking = thinking.substring(0, 60000) + "\n...(思考内容过长，已截断)";
        }
        msg.setThinking(thinking);
        msg.setTokensUsed(tokensUsed);
        messageMapper.insert(msg);
        // [FIX]: 同步 AI 回复到 ES（含 thinking、createdAt，无 imageUrls/attachmentIds）
        searchService.indexChatMessage(msg.getId(), sessionId, "assistant", content, thinking, null, null, msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : null);
        return msg;
    }

    public ChatMessage saveAssistantMessage(Long sessionId, String content, int tokensUsed) {
        return saveAssistantMessage(sessionId, content, tokensUsed, null);
    }

    // ==================== AI对话调用 ====================

    /**
     * 调用 AI API 进行对话（OpenAI 兼容格式）
     *
     * @param sessionId    会话ID
     * @param userMessage  用户消息文本
     * @param imageUrls    图片URL列表（可为空）
     * @param config       AI配置（主要取 apiKey/temperature/maxTokens）
     * @param modelOverride 覆盖模型名称（前端配置的模型，优先于config中的model）
     * @param apiUrlOverride 覆盖API地址（前端配置的地址，不为空则直接使用）
     * @return AI回复文本
     */
    public String callAiApi(Long sessionId, String userMessage, List<String> imageUrls,
                            AiConfig config, String modelOverride, String apiUrlOverride) {
        List<ChatMessage> history = getMessages(sessionId);

        // 确定最终使用的模型和API地址
        String finalModel = (modelOverride != null && !modelOverride.isBlank()) ? modelOverride : config.getModel();
        String finalApiUrl = apiUrlOverride;
        if (finalApiUrl == null || finalApiUrl.isBlank()) {
            finalApiUrl = config.getApiUrl();
        }
        if (finalApiUrl == null || finalApiUrl.isBlank()) {
            finalApiUrl = "https://api.openai.com/v1/chat/completions";
        }

        log.info("callAiApi -> model: {}, apiUrl: {}", finalModel, finalApiUrl);

        // 构建 OpenAI 消息格式
        StringBuilder messagesJson = new StringBuilder("[");
        messagesJson.append("{\"role\":\"system\",\"content\":\"你是一个知识库AI助手，帮助用户解答问题、整理知识。请用中文回答，回答简洁准确。\"},");

        for (ChatMessage msg : history) {
            messagesJson.append("{\"role\":\"").append(msg.getRole()).append("\",\"content\":\"")
                    .append(escapeJson(msg.getContent())).append("\"},");
        }

        // 当前用户消息（支持图片）
        if (imageUrls != null && !imageUrls.isEmpty()) {
            StringBuilder contentArr = new StringBuilder("[");
            contentArr.append("{\"type\":\"text\",\"text\":\"").append(escapeJson(userMessage)).append("\"},");
            for (String url : imageUrls) {
                contentArr.append("{\"type\":\"image_url\",\"image_url\":{\"url\":\"").append(escapeJson(url)).append("\"}},");
            }
            if (contentArr.length() > 0) contentArr.deleteCharAt(contentArr.length() - 1);
            contentArr.append("]");
            messagesJson.append("{\"role\":\"user\",\"content\":").append(contentArr).append("},");
        } else {
            messagesJson.append("{\"role\":\"user\",\"content\":\"").append(escapeJson(userMessage)).append("\"},");
        }
        // 去掉末尾逗号
        if (messagesJson.charAt(messagesJson.length() - 1) == ',') {
            messagesJson.deleteCharAt(messagesJson.length() - 1);
        }
        messagesJson.append("]");

        // 构建请求体
        String requestBody = "{" +
                "\"model\":\"" + escapeJson(finalModel) + "\"," +
                "\"messages\":" + messagesJson + "," +
                "\"temperature\":" + (config.getTemperature() != null ? config.getTemperature() : 0.7) + "," +
                "\"max_tokens\":" + (config.getMaxTokens() != null ? config.getMaxTokens() : 2048) +
                "}";

        log.info("callAiApi requestBody(前200字符): {}", requestBody.substring(0, Math.min(200, requestBody.length())));

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(60))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(finalApiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            if (response.statusCode() != 200) {
                log.error("AI API 调用失败: status={}, body={}", response.statusCode(), responseBody);
                return "抱歉，AI 服务暂时不可用（错误码: " + response.statusCode() + "）。请检查 API Key 和模型配置是否正确。";
            }

            return parseAiResponse(responseBody);

        } catch (Exception e) {
            log.error("AI API 调用异常", e);
            return "抱歉，调用 AI 服务时出现网络异常: " + e.getMessage();
        }
    }

    /**
     * 流式调用 AI API，通过 SseEmitter 推送 SSE 事件
     * 前端不再直接调用外部 API，由后端代理，统一处理认证和错误
     */
    public SseEmitter streamChat(List<java.util.Map<String, String>> messages,
                                  String model, String apiUrl, String apiKey,
                                  Double temperature, Integer maxTokens) {
        SseEmitter emitter = new SseEmitter(120_000L); // 2 分钟超时

        // 确定 API 地址
        final String finalApiUrl = (apiUrl == null || apiUrl.isBlank())
                ? "https://api.openai.com/v1/chat/completions"
                : apiUrl;

        // [FIX]: 构建消息 JSON — 兜底注入系统提示词，防止前端漏传system消息导致AI自行发挥身份
        // 如果消息列表第一条不是 system role，则在最前面插入系统身份提示
        boolean hasSystemMsg = !messages.isEmpty() && "system".equals(messages.get(0).get("role"));
        StringBuilder messagesJson = new StringBuilder("[");
        if (!hasSystemMsg) {
            messagesJson.append("{\"role\":\"system\",\"content\":\"你是一个知识库AI助手，帮助用户解答问题、整理知识。请用中文回答，回答简洁准确。你没有任何其他身份。\"},");
        }
        for (int i = 0; i < messages.size(); i++) {
            java.util.Map<String, String> msg = messages.get(i);
            messagesJson.append("{\"role\":\"").append(escapeJson(msg.get("role")))
                    .append("\",\"content\":\"").append(escapeJson(msg.get("content"))).append("\"}");
            if (i < messages.size() - 1) messagesJson.append(",");
        }
        messagesJson.append("]");

        String requestBody = "{" +
                "\"model\":\"" + escapeJson(model) + "\"," +
                "\"messages\":" + messagesJson + "," +
                "\"temperature\":" + (temperature != null ? temperature : 0.7) + "," +
                "\"max_tokens\":" + (maxTokens != null ? maxTokens : 4096) + "," +
                "\"stream\":true" +
                "}";

        log.info("streamChat -> model: {}, apiUrl: {}", model, finalApiUrl);

        // 异步执行，不阻塞请求线程
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(60))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(finalApiUrl))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .timeout(Duration.ofSeconds(120))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<java.io.InputStream> response = client.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    String errorBody = new String(response.body().readAllBytes());
                    log.error("streamChat API 错误: status={}, body={}", response.statusCode(), errorBody);
                    String friendlyMsg = switch (response.statusCode()) {
                        case 401 -> "API Key 无效或已过期，请在设置中更新 API Key。";
                        case 429 -> "AI 服务请求过于频繁，请稍后重试。";
                        case 500, 502, 503 -> "AI 服务暂时不可用，请稍后重试。";
                        default -> "AI 服务返回错误（" + response.statusCode() + "），请检查配置。";
                    };
                    emitter.send(SseEmitter.event().name("error").data(friendlyMsg));
                    emitter.complete();
                    return;
                }

                // 逐行读取 SSE 流并推送给前端
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(response.body()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            emitter.send(SseEmitter.event().data(data));
                            if ("[DONE]".equals(data)) break;
                        }
                    }
                }
                emitter.complete();

            } catch (Exception e) {
                log.error("streamChat 异常", e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data("AI 服务调用失败: " + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    /** 简单解析 OpenAI 格式的响应 */
    private String parseAiResponse(String json) {
        try {
            // 提取 choices[0].message.content
            int choicesIdx = json.indexOf("\"choices\"");
            if (choicesIdx < 0) return "AI 返回格式异常，请检查配置。";

            int msgIdx = json.indexOf("\"message\"", choicesIdx);
            if (msgIdx < 0) return "AI 返回格式异常。";

            int contentIdx = json.indexOf("\"content\"", msgIdx);
            if (contentIdx < 0) return "AI 返回格式异常。";

            int start = json.indexOf('"', contentIdx + 10);
            if (start < 0) return "AI 返回格式异常。";

            // 找到内容结束位置
            StringBuilder content = new StringBuilder();
            boolean escaped = false;
            for (int i = start + 1; i < json.length(); i++) {
                char c = json.charAt(i);
                if (escaped) {
                    content.append(c);
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '"') break;
                content.append(c);
            }
            return content.toString();
        } catch (Exception e) {
            log.error("解析AI响应失败", e);
            return "抱歉，解析AI回复时出错。";
        }
    }

    /** JSON 字符串转义 */
    /** [FIX]: String 转 LocalDateTime */
    private LocalDateTime parseDateTime(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            return LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return null;
        }
    }

    /** [FIX]: LocalDateTime 转 String */
    private String formatDateTime(LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * [FIX]: 迁移旧的 base64 图片数据为文件存储
     * 遍历所有含 base64 imageUrls 的消息，将 base64 解码保存为文件，替换为 URL
     * @return 迁移的消息条数
     */
    @Transactional
    public int migrateBase64Images() {
        List<ChatMessage> allMsgs = messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>().isNotNull(ChatMessage::getImageUrls)
        );
        int migrated = 0;
        java.nio.file.Path imgDir = java.nio.file.Paths.get(System.getProperty("user.dir"), "chat-images");
        try {
            java.nio.file.Files.createDirectories(imgDir);
        } catch (Exception e) {
            log.error("创建图片目录失败: {}", e.getMessage());
            return 0;
        }

        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

        for (ChatMessage msg : allMsgs) {
            String urls = msg.getImageUrls();
            if (urls == null || urls.isBlank() || "[]".equals(urls.trim())) continue;
            // 只处理含 base64 的数据
            if (!urls.contains("base64")) continue;

            try {
                List<String> urlList = objectMapper.readValue(urls, List.class);
                List<String> newUrls = new java.util.ArrayList<>();
                boolean changed = false;

                for (String url : urlList) {
                    if (url != null && url.startsWith("data:image/")) {
                        // 解析 base64: data:image/png;base64,xxxx
                        int commaIdx = url.indexOf(',');
                        if (commaIdx < 0) { newUrls.add(url); continue; }
                        String meta = url.substring(0, commaIdx);
                        String b64 = url.substring(commaIdx + 1);

                        // 确定 MIME 和扩展名
                        String ext = ".png";
                        if (meta.contains("jpeg") || meta.contains("jpg")) ext = ".jpg";
                        else if (meta.contains("gif")) ext = ".gif";
                        else if (meta.contains("webp")) ext = ".webp";

                        // 解码并保存为文件
                        byte[] imgBytes = java.util.Base64.getDecoder().decode(b64);
                        String filename = java.util.UUID.randomUUID().toString().replace("-", "") + ext;
                        java.nio.file.Path target = imgDir.resolve(filename);
                        java.nio.file.Files.write(target, imgBytes);

                        newUrls.add("/api/chat/images/" + filename);
                        changed = true;
                        log.info("迁移 base64 图片: msgId={}, size={}KB -> {}", msg.getId(), imgBytes.length / 1024, filename);
                    } else if (url != null && url.startsWith("/api/chat/images/")) {
                        // 已经是文件 URL，保留
                        newUrls.add(url);
                    } else if (url != null) {
                        // 其他格式（外部URL等），保留
                        newUrls.add(url);
                    }
                }

                if (changed) {
                    // 更新数据库
                    String newJson = objectMapper.writeValueAsString(newUrls);
                    msg.setImageUrls(newJson);
                    messageMapper.updateById(msg);
                    // 更新 ES
                    searchService.indexChatMessage(msg.getId(), msg.getSessionId(), msg.getRole(),
                            msg.getContent(), msg.getThinking(), newJson, msg.getAttachmentIds(),
                            msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : null);
                    migrated++;
                    log.info("迁移完成: msgId={}, 新URLs={}", msg.getId(), newJson);
                }
            } catch (Exception e) {
                log.error("迁移消息 {} 的 base64 图片失败: {}", msg.getId(), e.getMessage());
            }
        }
        log.info("base64 图片迁移完成，共迁移 {} 条消息", migrated);
        return migrated;
    }
}
