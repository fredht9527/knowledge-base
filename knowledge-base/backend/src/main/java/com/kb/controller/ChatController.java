package com.kb.controller;

import com.kb.dto.PageRequest;
import com.kb.dto.PageResponse;
import com.kb.dto.Result;
import com.kb.entity.AiConfig;
import com.kb.entity.ChatMessage;
import com.kb.entity.ChatSession;
import com.kb.search.OcrService;
import com.kb.service.AiConfigService;
import com.kb.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * AI对话控制器 - 管理会话和消息的增删改查，以及AI对话调用
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final AiConfigService aiConfigService;
    private final OcrService ocrService;

    public ChatController(ChatService chatService, AiConfigService aiConfigService, OcrService ocrService) {
        this.chatService = chatService;
        this.aiConfigService = aiConfigService;
        this.ocrService = ocrService;
    }

    /**
     * 创建新会话
     */
    @PostMapping("/session")
    public Result<ChatSession> createSession(@RequestBody(required = false) Map<String, String> body) {
        String model = body != null ? body.get("model") : null;
        ChatSession session = chatService.createSession(model);
        return Result.success(session);
    }

    /**
     * 获取会话列表（分页）
     */
    @GetMapping("/sessions")
    public Result<PageResponse<ChatSession>> listSessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Integer status) {
        PageRequest req = new PageRequest(page, size);
        return Result.success(chatService.listSessions(req, status));
    }

    /**
     * 搜索会话
     */
    @GetMapping("/sessions/search")
    public Result<PageResponse<ChatSession>> searchSessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam String keyword) {
        PageRequest req = new PageRequest(page, size);
        return Result.success(chatService.searchSessions(req, keyword));
    }

    /**
     * 更新会话标题
     */
    @PutMapping("/session/{id}/title")
    public Result<Void> updateTitle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        chatService.updateTitle(id, body.get("title"));
        return Result.success();
    }

    /**
     * 删除会话（软删除）
     */
    @DeleteMapping("/session/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        chatService.deleteSession(id);
        return Result.success();
    }

    /**
     * 归档/取消归档会话
     */
    @PutMapping("/session/{id}/archive")
    public Result<Void> archiveSession(@PathVariable Long id, @RequestParam boolean archive) {
        chatService.archiveSession(id, archive);
        return Result.success();
    }

    /**
     * 获取会话的所有消息
     */
    @GetMapping("/session/{id}/messages")
    public Result<List<ChatMessage>> getMessages(@PathVariable Long id) {
        return Result.success(chatService.getMessages(id));
    }

    /**
     * 保存用户消息（用于前端流式对话时的消息记录）
     */
    @PostMapping("/session/{id}/message/user")
    public Result<ChatMessage> saveUserMessage(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String message = (String) body.get("message");
        @SuppressWarnings("unchecked")
        List<String> imageUrls = (List<String>) body.get("imageUrls");
        @SuppressWarnings("unchecked")
        List<Number> attachmentIdNums = (List<Number>) body.get("attachmentIds");
        List<Long> attachmentIds = attachmentIdNums != null
                ? attachmentIdNums.stream().map(Number::longValue).toList()
                : null;
        chatService.saveUserMessage(id, message, imageUrls, attachmentIds);
        // 第一条消息自动更新标题
        List<ChatMessage> msgs = chatService.getMessages(id);
        long userCount = msgs.stream().filter(m -> "user".equals(m.getRole())).count();
        if (userCount <= 1) {
            String title = message.length() > 30 ? message.substring(0, 30) + "..." : message;
            chatService.updateTitle(id, title);
        }
        return Result.success();
    }

    /**
     * 保存AI回复消息（用于前端流式对话时的消息记录）
     */
    @PostMapping("/session/{id}/message/assistant")
    public Result<ChatMessage> saveAssistantMessage(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String reply = (String) body.get("reply");
        String thinking = (String) body.get("thinking");
        ChatMessage msg = chatService.saveAssistantMessage(id, reply, 0, thinking);
        return Result.success(msg);
    }

    /**
     * [FIX]: 删除指定消息（从数据库真实删除）
     */
    @DeleteMapping("/message/{msgId}")
    public Result<Void> deleteMessage(@PathVariable Long msgId) {
        chatService.deleteMessage(msgId);
        return Result.success(null);
    }

    /**
     * 发送消息并获取AI回复
     */
    @PostMapping("/session/{id}/send")
    public Result<Map<String, Object>> sendMessage(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String message = (String) body.get("message");
        @SuppressWarnings("unchecked")
        List<String> imageUrls = (List<String>) body.get("imageUrls");

        if (message == null || message.isBlank()) {
            return Result.error("消息不能为空");
        }

        // 1. 保存用户消息
        @SuppressWarnings("unchecked")
        List<Number> attachmentIdNums = (List<Number>) body.get("attachmentIds");
        List<Long> attachmentIds = attachmentIdNums != null
                ? attachmentIdNums.stream().map(Number::longValue).toList()
                : null;
        chatService.saveUserMessage(id, message, imageUrls, attachmentIds);

        // 2. 获取AI配置（只取 API Key 和温度等参数，模型和地址由前端指定）
        AiConfig config = aiConfigService.getConfig();
        String model = (String) body.get("model");
        String apiUrl = (String) body.get("apiUrl");
        // 前端如果没有传model，回退到数据库配置
        if (model == null || model.isBlank()) {
            model = config.getModel();
        }
        log.info("AI对话 request: model={}, apiUrl={}, provider={}", model, apiUrl, config.getProvider());

        // 3. 调用AI（传model和apiUrl覆盖config中的值）
        String reply = chatService.callAiApi(id, message, imageUrls, config, model, apiUrl);

        // 4. 保存AI回复
        ChatMessage assistantMsg = chatService.saveAssistantMessage(id, reply, 0);

        // 5. 如果是第一条用户消息，自动更新会话标题
        List<ChatMessage> msgs = chatService.getMessages(id);
        long userCount = msgs.stream().filter(m -> "user".equals(m.getRole())).count();
        if (userCount <= 1) {
            String title = message.length() > 30 ? message.substring(0, 30) + "..." : message;
            chatService.updateTitle(id, title);
        }

        return Result.success(Map.of(
                "reply", reply,
                "messageId", assistantMsg.getId(),
                "sessionId", id
        ));
    }

    /**
     * 流式 AI 对话 - 后端代理外部 AI API，前端通过 SSE 接收流式响应
     * 解决前端直接调用外部 API 导致的 401 等错误无法友好处理的问题
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/stream")
    public SseEmitter streamChat(@RequestBody Map<String, Object> body) {
        List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
        String model = (String) body.get("model");
        String apiUrl = (String) body.get("apiUrl");
        Double temperature = body.get("temperature") != null ? ((Number) body.get("temperature")).doubleValue() : null;
        Integer maxTokens = body.get("maxTokens") != null ? ((Number) body.get("maxTokens")).intValue() : null;

        // 从后端配置获取 API Key，前端不再传 apiKey
        AiConfig config = aiConfigService.getConfig();
        String apiKey = config.getApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("未配置 API Key，请在设置中配置。"));
            } catch (Exception ignored) {}
            emitter.complete();
            return emitter;
        }

        // model 和 apiUrl 优先用前端传的，回退到数据库配置
        if (model == null || model.isBlank()) model = config.getModel();
        if (apiUrl == null || apiUrl.isBlank()) apiUrl = config.getApiUrl();

        return chatService.streamChat(messages, model, apiUrl, apiKey, temperature, maxTokens);
    }

    /**
     * OCR 识别图片中的文字
     * 用于不支持图片输入的 AI 模型，先提取文字再送入大模型
     */
    @PostMapping("/ocr")
    public Result<Map<String, String>> ocrImage(@RequestBody Map<String, String> body) {
        String base64 = body.get("image");
        if (base64 == null || base64.isBlank()) {
            return Result.error("图片数据不能为空");
        }
        String text = ocrService.recognizeText(base64);
        if (text == null || text.isBlank()) {
            return Result.success(Map.of("text", ""));
        }
        return Result.success(Map.of("text", text));
    }

    /**
     * [FIX]: 迁移旧的 base64 图片数据为文件存储
     * 将数据库中存储的 base64 图片解码保存为磁盘文件，替换为 URL 引用
     * 大幅减少数据库和 ES 存储空间
     */
    @PostMapping("/migrate-images")
    public Result<Map<String, Object>> migrateImages() {
        int count = chatService.migrateBase64Images();
        return Result.success(Map.of("migratedCount", count, "message", "迁移完成，" + count + " 条消息的图片已转为文件存储"));
    }
}
