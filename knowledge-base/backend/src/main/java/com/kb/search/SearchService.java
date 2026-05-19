package com.kb.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * [FIX]: 统一检索服务 - 替代 MySQL LIKE 查询
 * 支持 BM25 全文检索 + kNN 语义向量检索混合模式
 * 所有搜索查询不再直接查 MySQL，而是请求 Elasticsearch
 */
@Slf4j
@Service
public class SearchService {

    private final ElasticsearchClient esClient;
    private final SearchRepository searchRepository;
    private final EmbeddingService embeddingService;
    private final SearchConfig searchConfig;

    public SearchService(ElasticsearchClient esClient, SearchRepository searchRepository,
                         EmbeddingService embeddingService, SearchConfig searchConfig) {
        this.esClient = esClient;
        this.searchRepository = searchRepository;
        this.embeddingService = embeddingService;
        this.searchConfig = searchConfig;
    }

    /**
     * [FIX]: 混合检索 - 供 AI 聊天使用
     * BM25 全文检索 + kNN 语义向量检索，使用 RRF（倒数排名融合）合并结果
     */
    public List<SearchResult> hybridSearch(String keyword, int size) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        int limit = Math.min(size, 20);

        // [FIX]: embedding 禁用时直接走纯 BM25，不调用混合检索
        if (!searchConfig.getEmbedding().isEnabled()) {
            try {
                return doKeywordSearch(keyword, limit);
            } catch (Exception e) {
                log.error("ES BM25 检索失败: {}", e.getMessage());
                return List.of();
            }
        }

        try {
            return doHybridSearch(keyword, limit);
        } catch (Exception e) {
            log.error("ES 混合检索失败，降级为纯 BM25: {}", e.getMessage());
            try {
                return doKeywordSearch(keyword, limit);
            } catch (Exception e2) {
                log.error("ES BM25 检索也失败: {}", e2.getMessage());
                return List.of();
            }
        }
    }

    /**
     * [FIX]: BM25 + kNN 混合检索
     * ES 8.x Java Client 正确用法：
     * - kNN 是 SearchRequest 级别参数，不是 Query 子类型
     * - .knn() 接受 KnnQuery builder 函数
     * - .rank().rrf() 嵌套结构实现 RRF 融合
     */
    private List<SearchResult> doHybridSearch(String keyword, int size) throws IOException {
        float[] queryVector = embeddingService.generateEmbedding(keyword);

        if (queryVector == null || queryVector.length == 0) {
            log.info("向量生成失败，降级为纯 BM25 检索");
            return doKeywordSearch(keyword, size);
        }

        // BM25 关键词查询
        Query keywordQuery = Query.of(q -> q
                .bool(b -> b
                        .must(m -> m
                                .multiMatch(mm -> mm
                                        .fields("title^3", "content^2", "summary", "fileName")
                                        .query(keyword)
                                )
                        )
                        .filter(f -> f
                                .terms(t -> t
                                        .field("type")
                                        .terms(tv -> tv.value(List.of(
                                                FieldValue.of("knowledge"),
                                                FieldValue.of("attachment")
                                        )))
                                )
                        )
                )
        );

        // [FIX]: kNN + BM25 + RRF 混合检索
        // kNN 是 SearchRequest 级别参数，使用 .knn(KnnQuery builder) 
        // RRF 使用 .rank(rank -> rank.rrf(rrf -> rrf...)) 嵌套
        SearchResponse<SearchDocument> response = esClient.search(s -> s
                        .index("knowledge_search")
                        .size(size)
                        .query(keywordQuery)
                        // kNN 语义向量搜索（SearchRequest 级别，不是 Query 类型）
                        .knn(knn -> knn
                                .field("contentEmbedding")
                                .queryVector(floatList(queryVector))
                                .k(size * 2)
                                .numCandidates(size * 10)
                        )
                        // RRF 倒数排名融合
                        .rank(rank -> rank
                                .rrf(rrf -> rrf
                                        .rankConstant(1L)
                                        .windowSize((long) size * 2)
                                )
                        ),
                SearchDocument.class
        );

        return response.hits().hits().stream()
                .map(this::toSearchResult)
                .collect(Collectors.toList());
    }

    /**
     * 纯 BM25 关键词检索（降级方案）
     */
    private List<SearchResult> doKeywordSearch(String keyword, int size) throws IOException {
        SearchResponse<SearchDocument> response = esClient.search(s -> s
                        .index("knowledge_search")
                        .size(size)
                        .query(q -> q
                                .bool(b -> b
                                        .must(m -> m
                                                .multiMatch(mm -> mm
                                                        .fields("title^3", "content^2", "summary", "fileName")
                                                        .query(keyword)
                                                )
                                        )
                                        .filter(f -> f
                                                .terms(t -> t
                                                        .field("type")
                                                        .terms(tv -> tv.value(List.of(
                                                                FieldValue.of("knowledge"),
                                                                FieldValue.of("attachment")
                                                        )))
                                                )
                                        )
                                )
                        ),
                SearchDocument.class
        );

        return response.hits().hits().stream()
                .map(this::toSearchResult)
                .collect(Collectors.toList());
    }

    /**
     * [FIX]: 知识库分页搜索
     */
    public List<SearchResult> searchKnowledge(String keyword, int page, int size) {
        try {
            SearchResponse<SearchDocument> response = esClient.search(s -> s
                            .index("knowledge_search")
                            .from((page - 1) * size)
                            .size(size)
                            .query(q -> {
                                if (keyword != null && !keyword.isBlank()) {
                                    return q.multiMatch(mm -> mm
                                            .fields("title^3", "content^2", "summary")
                                            .query(keyword)
                                    );
                                }
                                return q.matchAll(ma -> ma);
                            }),
                    SearchDocument.class
            );

            return response.hits().hits().stream()
                    .map(this::toSearchResult)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("ES 知识库搜索失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * [FIX]: 聊天记录语义搜索
     */
    public List<SearchResult> searchChatHistory(String keyword, int size) {
        try {
            float[] queryVector = embeddingService.generateEmbedding(keyword);

            SearchResponse<SearchDocument> response;

            if (queryVector != null && queryVector.length > 0 && searchConfig.getEmbedding().isEnabled()) {
                // 混合检索
                Query keywordQuery = Query.of(q -> q
                        .bool(b -> b
                                .must(m -> m.multiMatch(mm -> mm
                                        .fields("content")
                                        .query(keyword)))
                                .filter(f -> f.term(t -> t.field("type").value("chat")))
                        )
                );

                response = esClient.search(s -> s
                                .index("knowledge_search")
                                .size(size)
                                .query(keywordQuery)
                                .knn(knn -> knn
                                        .field("contentEmbedding")
                                        .queryVector(floatList(queryVector))
                                        .k(size * 2)
                                        .numCandidates(size * 10)
                                )
                                .rank(rank -> rank
                                        .rrf(rrf -> rrf
                                                .rankConstant(1L)
                                                .windowSize((long) size * 2)
                                        )
                                ),
                        SearchDocument.class
                );
            } else {
                // 纯关键词
                response = esClient.search(s -> s
                                .index("knowledge_search")
                                .size(size)
                                .query(q -> q
                                        .bool(b -> b
                                                .must(m -> m.multiMatch(mm -> mm.fields("content").query(keyword)))
                                                .filter(f -> f.term(t -> t.field("type").value("chat")))
                                        )
                                ),
                        SearchDocument.class
                );
            }

            return response.hits().hits().stream()
                    .map(this::toSearchResult)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("ES 聊天记录搜索失败: {}", e.getMessage());
            return List.of();
        }
    }

    // ==================== 索引写入 ====================

    /**
     * [FIX]: 索引知识条目
     */
    public void indexKnowledge(Long id, String title, String content, String summary,
                               String categoryName, Long categoryId, Integer status, Integer viewCount,
                               java.util.List<String> tags) {
        try {
            SearchDocument doc = new SearchDocument();
            doc.setDocId("knowledge_" + id);
            doc.setType("knowledge");
            doc.setOriginalId(id);
            doc.setKnowledgeId(id);
            doc.setTitle(title);
            doc.setContent(content != null && content.length() > 8000 ? content.substring(0, 8000) : content);
            doc.setSummary(summary);
            doc.setCategoryName(categoryName);
            doc.setCategoryId(categoryId);
            doc.setStatus(status);
            doc.setViewCount(viewCount);
            if (tags != null) doc.setTags(tags);
            String embeddingText = (title != null ? title : "") + " " +
                    (content != null && content.length() > 2000 ? content.substring(0, 2000) : (content != null ? content : ""));
            doc.setContentEmbedding(embeddingService.generateEmbedding(embeddingText));

            searchRepository.save(doc);
            log.info("已索引知识条目: id={}, title={}", id, title);
        } catch (Exception e) {
            log.error("索引知识条目失败: id={}, error={}", id, e.getMessage());
        }
    }

    /**
     * [FIX]: 快速索引知识条目（不含向量），供全量同步使用
     */
    public void indexKnowledgeNoEmbedding(Long id, String title, String content, String summary,
                                          String categoryName, Long categoryId, Integer status, Integer viewCount,
                                          java.util.List<String> tags) {
        try {
            SearchDocument doc = new SearchDocument();
            doc.setDocId("knowledge_" + id);
            doc.setType("knowledge");
            doc.setOriginalId(id);
            doc.setKnowledgeId(id);
            doc.setTitle(title);
            doc.setContent(content != null && content.length() > 8000 ? content.substring(0, 8000) : content);
            doc.setSummary(summary);
            doc.setCategoryName(categoryName);
            doc.setCategoryId(categoryId);
            doc.setStatus(status);
            doc.setViewCount(viewCount);
            if (tags != null) doc.setTags(tags);
            // 不生成向量，BM25 搜索仍可用
            searchRepository.save(doc);
        } catch (Exception e) {
            log.error("快速索引知识条目失败: id={}, error={}", id, e.getMessage());
        }
    }

    /**
     * [FIX]: 索引附件内容
     */
    public void indexAttachment(Long id, Long knowledgeId, String fileName, String fileText) {
        try {
            SearchDocument doc = new SearchDocument();
            doc.setDocId("attachment_" + id);
            doc.setType("attachment");
            doc.setOriginalId(id);
            doc.setKnowledgeId(knowledgeId);
            doc.setTitle(fileName);
            doc.setFileName(fileName);
            doc.setContent(fileText != null && fileText.length() > 8000 ? fileText.substring(0, 8000) : fileText);
            String embeddingText = (fileName != null ? fileName : "") + " " +
                    (fileText != null && fileText.length() > 2000 ? fileText.substring(0, 2000) : (fileText != null ? fileText : ""));
            doc.setContentEmbedding(embeddingService.generateEmbedding(embeddingText));

            searchRepository.save(doc);
            log.info("已索引附件: id={}, fileName={}", id, fileName);
        } catch (Exception e) {
            log.error("索引附件失败: id={}, error={}", id, e.getMessage());
        }
    }

    /**
     * [FIX]: 快速索引附件（不含向量）
     */
    public void indexAttachmentNoEmbedding(Long id, Long knowledgeId, String fileName, String fileText) {
        try {
            SearchDocument doc = new SearchDocument();
            doc.setDocId("attachment_" + id);
            doc.setType("attachment");
            doc.setOriginalId(id);
            doc.setKnowledgeId(knowledgeId);
            doc.setTitle(fileName);
            doc.setFileName(fileName);
            doc.setContent(fileText != null && fileText.length() > 8000 ? fileText.substring(0, 8000) : fileText);
            searchRepository.save(doc);
        } catch (Exception e) {
            log.error("快速索引附件失败: id={}, error={}", id, e.getMessage());
        }
    }

    /**
     * [FIX]: 索引聊天消息
     */
    public void indexChatMessage(Long id, Long sessionId, String role, String content, String thinking,
                                 String imageUrls, String attachmentIds, String createdAt) {
        try {
            SearchDocument doc = new SearchDocument();
            doc.setDocId("chat_" + id);
            doc.setType("chat");
            doc.setOriginalId(id);
            doc.setKnowledgeId(sessionId);
            doc.setTitle(role.equals("user") ? "用户消息" : "AI回复");
            // [FIX]: 聊天消息 content 不截断，保留完整内容（用户可能发送含大量文件文本的消息）
            doc.setContent(content);
            // [FIX]: 保存 role、thinking、imageUrls、attachmentIds、createdAt
            doc.setRole(role);
            doc.setThinking(thinking != null && thinking.length() > 60000 ? thinking.substring(0, 60000) : thinking);
            doc.setImageUrls(imageUrls);
            doc.setAttachmentIds(attachmentIds);
            doc.setCreatedAt(createdAt);
            doc.setStatus(1);
            String embeddingText = content != null && content.length() > 2000 ? content.substring(0, 2000) : (content != null ? content : "");
            doc.setContentEmbedding(embeddingService.generateEmbedding(embeddingText));

            searchRepository.save(doc);
            log.debug("已索引聊天消息: id={}, role={}", id, role);
        } catch (Exception e) {
            log.error("索引聊天消息失败: id={}, error={}", id, e.getMessage());
        }
    }

    /**
     * [FIX]: 快速索引聊天消息（不含向量）
     */
    public void indexChatMessageNoEmbedding(Long id, Long sessionId, String role, String content, String thinking,
                                             String createdAt, String imageUrls, String attachmentIds) {
        try {
            SearchDocument doc = new SearchDocument();
            doc.setDocId("chat_" + id);
            doc.setType("chat");
            doc.setOriginalId(id);
            doc.setKnowledgeId(sessionId);
            doc.setTitle(role.equals("user") ? "用户消息" : "AI回复");
            // [FIX]: 聊天消息 content 不截断，保留完整内容
            doc.setContent(content);
            // [FIX]: 保存 role、thinking、createdAt、imageUrls、attachmentIds
            doc.setRole(role);
            doc.setThinking(thinking != null && thinking.length() > 60000 ? thinking.substring(0, 60000) : thinking);
            doc.setCreatedAt(createdAt);
            doc.setImageUrls(imageUrls);
            doc.setAttachmentIds(attachmentIds);
            doc.setStatus(1);
            searchRepository.save(doc);
        } catch (Exception e) {
            log.error("快速索引聊天消息失败: id={}, error={}", id, e.getMessage());
        }
    }

    /**
     * [FIX]: 删除索引文档
     */
    public void deleteDocument(String type, Long id) {
        try {
            searchRepository.deleteById(type + "_" + id);
            log.info("已删除索引: type={}, id={}", type, id);
        } catch (Exception e) {
            log.error("删除索引失败: type={}, id={}, error={}", type, id, e.getMessage());
        }
    }

    // ==================== [FIX]: 分类/会话 索引方法 ====================

    /**
     * [FIX]: 索引分类
     */
    public void indexCategory(Long id, String name, Long parentId) {
        try {
            SearchDocument doc = new SearchDocument();
            doc.setDocId("category_" + id);
            doc.setType("category");
            doc.setOriginalId(id);
            doc.setTitle(name);
            doc.setContent(name);
            doc.setCategoryId(id);
            doc.setParentId(parentId);
            searchRepository.save(doc);
            log.debug("已索引分类: id={}, name={}", id, name);
        } catch (Exception e) {
            log.error("索引分类失败: id={}, error={}", id, e.getMessage());
        }
    }

    /**
     * [FIX]: 索引会话
     */
    public void indexSession(Long id, String title, Integer status, String model,
                             String createdAt, String updatedAt) {
        try {
            SearchDocument doc = new SearchDocument();
            doc.setDocId("session_" + id);
            doc.setType("session");
            doc.setOriginalId(id);
            doc.setTitle(title);
            doc.setContent(title);
            doc.setStatus(status);
            doc.setModel(model);
            doc.setCreatedAt(createdAt);
            doc.setUpdatedAt(updatedAt);
            searchRepository.save(doc);
            log.debug("已索引会话: id={}, title={}", id, title);
        } catch (Exception e) {
            log.error("索引会话失败: id={}, error={}", id, e.getMessage());
        }
    }

    // ==================== [FIX]: ES 直接查询方法（不回查 MySQL） ====================

    /**
     * [FIX]: 按 ID 和类型从 ES 获取文档
     */
    public SearchDocument getDocument(String type, Long id) {
        try {
            return searchRepository.findById(type + "_" + id).orElse(null);
        } catch (Exception e) {
            log.error("ES 获取文档失败: type={}, id={}, error={}", type, id, e.getMessage());
            return null;
        }
    }

    /**
     * [FIX]: 从 ES 按类型分页查询（替代 MySQL selectPage）
     */
    public List<SearchDocument> searchByType(String type, int page, int size) {
        try {
            SearchResponse<SearchDocument> response = esClient.search(s -> s
                            .index("knowledge_search")
                            .from((page - 1) * size)
                            .size(size)
                            .query(q -> q
                                    .bool(b -> b
                                            .filter(f -> f.term(t -> t.field("type").value(type)))
                                    )
                            )
                            .sort(so -> so.field(f -> f.field("updatedAt").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))),
                    SearchDocument.class
            );
            List<SearchDocument> results = response.hits().hits().stream()
                    .map(h -> {
                        SearchDocument doc = h.source();
                        if (doc == null) {
                            log.warn("ES Hit source 为 null, id={}", h.id());
                        }
                        return doc;
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            log.debug("ES 按类型查询返回: type={}, hits={}, parsed={}", type, response.hits().hits().size(), results.size());
            return results;
        } catch (IOException e) {
            log.error("ES 按类型查询失败: type={}, error={}", type, e.getMessage());
            return List.of();
        }
    }

    /**
     * [FIX]: 从 ES 按类型+关键词搜索+分页
     */
    public long countByType(String type) {
        try {
            SearchResponse<SearchDocument> response = esClient.search(s -> s
                            .index("knowledge_search")
                            .size(0)
                            .query(q -> q
                                    .bool(b -> b
                                            .filter(f -> f.term(t -> t.field("type").value(type)))
                                    )
                            ),
                    SearchDocument.class
            );
            return response.hits().total().value();
        } catch (IOException e) {
            log.error("ES 计数失败: type={}, error={}", type, e.getMessage());
            return 0;
        }
    }

    /**
     * [FIX]: 按类型+关键词+筛选条件计数
     */
    public long countByTypeWithFilter(String type, String keyword, Long categoryId, Integer status) {
        try {
            SearchResponse<SearchDocument> response = esClient.search(s -> s
                            .index("knowledge_search")
                            .size(0)
                            .query(q -> q
                                    .bool(b -> {
                                        b.filter(f -> f.term(t -> t.field("type").value(type)));
                                        if (keyword != null && !keyword.isBlank()) {
                                            b.must(m -> m.multiMatch(mm -> mm
                                                    .fields("title^3", "content^2", "summary")
                                                    .query(keyword)
                                            ));
                                        }
                                        if (categoryId != null) {
                                            b.filter(f -> f.term(t -> t.field("categoryId").value(categoryId)));
                                        }
                                        if (status != null) {
                                            b.filter(f -> f.term(t -> t.field("status").value(status)));
                                        }
                                        return b;
                                    })
                            ),
                    SearchDocument.class
            );
            return response.hits().total().value();
        } catch (IOException e) {
            log.error("ES 条件计数失败: type={}, error={}", type, e.getMessage());
            return 0;
        }
    }

    /**
     * [FIX]: 从 ES 按类型+关键词+筛选条件搜索
     */
    public List<SearchDocument> searchByTypeWithFilter(String type, String keyword,
                                                        Long categoryId, Integer status,
                                                        int page, int size) {
        try {
            SearchResponse<SearchDocument> response = esClient.search(s -> s
                            .index("knowledge_search")
                            .from((page - 1) * size)
                            .size(size)
                            .query(q -> q
                                    .bool(b -> {
                                        b.filter(f -> f.term(t -> t.field("type").value(type)));
                                        if (keyword != null && !keyword.isBlank()) {
                                            b.must(m -> m.multiMatch(mm -> mm
                                                    .fields("title^3", "content^2", "summary")
                                                    .query(keyword)
                                            ));
                                        }
                                        if (categoryId != null) {
                                            b.filter(f -> f.term(t -> t.field("categoryId").value(categoryId)));
                                        }
                                        if (status != null) {
                                            b.filter(f -> f.term(t -> t.field("status").value(status)));
                                        }
                                        return b;
                                    })
                            )
                            .sort(so -> so.field(f -> f.field("updatedAt").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))),
                    SearchDocument.class
            );
            List<SearchDocument> results = response.hits().hits().stream()
                    .map(h -> {
                        SearchDocument doc = h.source();
                        if (doc == null) log.warn("searchByTypeWithFilter: Hit source 为 null, id={}", h.id());
                        return doc;
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            log.info("ES 条件查询: type={}, keyword={}, status={}, total={}, parsed={}", type, keyword, status, response.hits().total().value(), results.size());
            return results;
        } catch (IOException e) {
            log.error("ES 条件查询失败: type={}, error={}", type, e.getMessage());
            return List.of();
        }
    }

    /**
     * [FIX]: 按 session ID 从 ES 获取聊天消息
     */
    public List<SearchDocument> getMessagesBySession(Long sessionId) {
        try {
            SearchResponse<SearchDocument> response = esClient.search(s -> s
                            .index("knowledge_search")
                            .size(200)
                            .query(q -> q
                                    .bool(b -> b
                                            .filter(f -> f.term(t -> t.field("type").value("chat")))
                                            .filter(f -> f.term(t -> t.field("knowledgeId").value(sessionId)))
                                    )
                            )
                            // [FIX]: 先按 createdAt 排序，再按 originalId 排序确保稳定
                            .sort(so -> so.field(f -> f.field("createdAt").order(co.elastic.clients.elasticsearch._types.SortOrder.Asc)))
                            .sort(so -> so.field(f -> f.field("originalId").order(co.elastic.clients.elasticsearch._types.SortOrder.Asc))),
                    SearchDocument.class
            );
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("ES 获取聊天消息失败: sessionId={}, error={}", sessionId, e.getMessage());
            return List.of();
        }
    }

    // ==================== 辅助方法 ====================

    private SearchResult toSearchResult(Hit<SearchDocument> hit) {
        SearchDocument doc = hit.source();
        if (doc == null) return null;
        SearchResult result = new SearchResult();
        result.setType(doc.getType());
        result.setOriginalId(doc.getOriginalId());
        result.setKnowledgeId(doc.getKnowledgeId());
        result.setTitle(doc.getTitle());
        result.setContent(doc.getContent());
        result.setSummary(doc.getSummary());
        result.setCategoryName(doc.getCategoryName());
        result.setFileName(doc.getFileName());
        result.setScore(hit.score());
        return result;
    }

    /**
     * [FIX]: float[] 转 List<Float>，ES Java Client 的 knn.queryVector 需要 List<Float>
     */
    private List<Float> floatList(float[] arr) {
        List<Float> list = new java.util.ArrayList<>(arr.length);
        for (float v : arr) {
            list.add(v);
        }
        return list;
    }

    /**
     * 搜索结果封装
     */
    public static class SearchResult {
        private String type;
        private Long originalId;
        private Long knowledgeId;
        private String title;
        private String content;
        private String summary;
        private String categoryName;
        private String fileName;
        private Double score;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Long getOriginalId() { return originalId; }
        public void setOriginalId(Long originalId) { this.originalId = originalId; }
        public Long getKnowledgeId() { return knowledgeId; }
        public void setKnowledgeId(Long knowledgeId) { this.knowledgeId = knowledgeId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
    }
}
