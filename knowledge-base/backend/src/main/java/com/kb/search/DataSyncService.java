package com.kb.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kb.entity.Attachment;
import com.kb.entity.ChatMessage;
import com.kb.entity.ChatSession;
import com.kb.entity.Category;
import com.kb.entity.Knowledge;
import com.kb.mapper.AttachmentMapper;
import com.kb.mapper.CategoryMapper;
import com.kb.mapper.ChatMessageMapper;
import com.kb.mapper.ChatSessionMapper;
import com.kb.mapper.KnowledgeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * [FIX]: 数据同步服务 - 将 MySQL 数据全量同步到 Elasticsearch
 * 1. 应用启动时快速同步（不含向量），确保搜索立即可用
 * 2. 后台异步补全向量，不阻塞应用启动
 */
@Slf4j
@Service
public class DataSyncService implements CommandLineRunner {

    private final KnowledgeMapper knowledgeMapper;
    private final AttachmentMapper attachmentMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final CategoryMapper categoryMapper;
    private final SearchService searchService;
    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbc;

    public DataSyncService(KnowledgeMapper knowledgeMapper, AttachmentMapper attachmentMapper,
                           ChatMessageMapper chatMessageMapper, ChatSessionMapper chatSessionMapper,
                           CategoryMapper categoryMapper,
                           SearchService searchService, EmbeddingService embeddingService,
                           DataSource dataSource) {
        this.knowledgeMapper = knowledgeMapper;
        this.attachmentMapper = attachmentMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.chatSessionMapper = chatSessionMapper;
        this.categoryMapper = categoryMapper;
        this.searchService = searchService;
        this.embeddingService = embeddingService;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    private List<String> getTagNames(Long knowledgeId) {
        return jdbc.queryForList(
                "SELECT t.name FROM tag t JOIN knowledge_tag kt ON t.id = kt.tag_id WHERE kt.knowledge_id = ?",
                String.class, knowledgeId);
    }

    /**
     * [FIX]: 应用启动后执行全量同步
     * 先快速同步基础数据（不含向量），确保 BM25 搜索立即可用
     * 然后异步补全向量（不阻塞应用启动）
     */
    @Override
    public void run(String... args) {
        try {
            log.info("开始全量同步数据到 Elasticsearch（快速模式，不含向量）...");
            quickSync();
            log.info("全量同步完成，BM25 搜索已可用");

            // [FIX]: 异步补全向量，不阻塞主线程
            new Thread(this::asyncEmbeddingSync, "embedding-sync").start();
        } catch (Exception e) {
            log.warn("全量同步失败（ES 可能未启动），搜索功能将降级: {}", e.getMessage());
        }
    }

    /**
     * [FIX]: 快速同步 - 只索引文本内容，不生成向量
     * 确保 BM25 全文检索立即可用
     */
    public void quickSync() {
        AtomicInteger total = new AtomicInteger(0);

        // 1. 同步知识条目（不含向量）
        List<Knowledge> knowledgeList = knowledgeMapper.selectList(null);
        for (Knowledge k : knowledgeList) {
            String categoryName = null;
            if (k.getCategoryId() != null) {
                Category cat = categoryMapper.selectById(k.getCategoryId());
                if (cat != null) categoryName = cat.getName();
            }
            searchService.indexKnowledgeNoEmbedding(k.getId(), k.getTitle(), k.getContent(),
                    k.getSummary(), categoryName, k.getCategoryId(), k.getStatus(), k.getViewCount(),
                    new ArrayList<>(getTagNames(k.getId())));
            total.incrementAndGet();
        }
        log.info("已同步 {} 条知识条目（无向量）", knowledgeList.size());

        // 2. 同步附件（不含向量）
        List<Attachment> attachments = attachmentMapper.selectList(
                new LambdaQueryWrapper<Attachment>().isNotNull(Attachment::getFileText)
        );
        for (Attachment a : attachments) {
            searchService.indexAttachmentNoEmbedding(a.getId(), a.getKnowledgeId(),
                    a.getFileName(), a.getFileText());
            total.incrementAndGet();
        }
        log.info("已同步 {} 条附件（无向量）", attachments.size());

        // 3. 同步聊天消息（不含向量）
        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>().isNotNull(ChatMessage::getContent)
        );
        for (ChatMessage m : messages) {
            // [FIX]: 同步到 ES 时过滤 base64 图片数据，只保留文件 URL
            // base64 数据高达数百 KB，不应存入 ES，应在迁移后同步
            String esImageUrls = m.getImageUrls();
            if (esImageUrls != null && esImageUrls.contains("base64")) {
                esImageUrls = null; // 跳过 base64，等迁移完成后重新同步
            }
            searchService.indexChatMessageNoEmbedding(m.getId(), m.getSessionId(),
                    m.getRole(), m.getContent(), m.getThinking(), formatDateTime(m.getCreatedAt()), esImageUrls, m.getAttachmentIds());
            total.incrementAndGet();
        }
        log.info("已同步 {} 条聊天消息（无向量）", messages.size());

        // [FIX]: 4. 同步分类
        List<Category> categories = categoryMapper.selectList(null);
        for (Category cat : categories) {
            searchService.indexCategory(cat.getId(), cat.getName(), cat.getParentId());
            total.incrementAndGet();
        }
        log.info("已同步 {} 条分类", categories.size());

        // [FIX]: 5. 同步会话
        List<ChatSession> sessions = chatSessionMapper.selectList(null);
        for (ChatSession s : sessions) {
            searchService.indexSession(s.getId(), s.getTitle(), s.getStatus(),
                    s.getModel(), formatDateTime(s.getCreatedAt()), formatDateTime(s.getUpdatedAt()));
            total.incrementAndGet();
        }
        log.info("已同步 {} 条会话", sessions.size());

        log.info("快速同步完成，共 {} 条文档（BM25 搜索可用，向量异步补全中）", total.get());
    }

    /**
     * [FIX]: 异步补全向量 - 在后台逐条生成向量并更新索引
     * 即使失败也不影响已索引的 BM25 搜索
     */
    private void asyncEmbeddingSync() {
        try {
            log.info("开始异步补全向量...");
            embeddingService.resetFailures();
            Thread.sleep(5000); // 等5秒让应用完全启动

            // 补全知识条目向量
            List<Knowledge> knowledgeList = knowledgeMapper.selectList(null);
            int count = 0;
            for (Knowledge k : knowledgeList) {
                try {
                    String categoryName = null;
                    if (k.getCategoryId() != null) {
                        Category cat = categoryMapper.selectById(k.getCategoryId());
                        if (cat != null) categoryName = cat.getName();
                    }
                    searchService.indexKnowledge(k.getId(), k.getTitle(), k.getContent(),
                            k.getSummary(), categoryName, k.getCategoryId(), k.getStatus(), k.getViewCount(),
                            new ArrayList<>(getTagNames(k.getId())));
                    count++;
                    if (count % 5 == 0) log.info("向量补全进度: {}/{}", count, knowledgeList.size());
                } catch (Exception e) {
                    log.warn("知识条目 {} 向量补全失败: {}", k.getId(), e.getMessage());
                }
            }
            log.info("知识条目向量补全完成: {}/{}", count, knowledgeList.size());

            // 补全附件向量
            List<Attachment> attachments = attachmentMapper.selectList(
                    new LambdaQueryWrapper<Attachment>().isNotNull(Attachment::getFileText)
            );
            int attachCount = 0;
            for (Attachment a : attachments) {
                try {
                    searchService.indexAttachment(a.getId(), a.getKnowledgeId(),
                            a.getFileName(), a.getFileText());
                    attachCount++;
                } catch (Exception e) {
                    log.warn("附件 {} 向量补全失败: {}", a.getId(), e.getMessage());
                }
            }
            log.info("附件向量补全完成: {}/{}", attachCount, attachments.size());

        } catch (Exception e) {
            log.warn("异步向量补全失败: {}", e.getMessage());
        }
    }

    /**
     * 全量同步（含向量，供手动触发）
     */
    public int fullSyncFromApi() {
        quickSync();
        new Thread(this::asyncEmbeddingSync, "embedding-sync-api").start();
        return 1;
    }

    /** [FIX]: LocalDateTime 转 String */
    private String formatDateTime(LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
