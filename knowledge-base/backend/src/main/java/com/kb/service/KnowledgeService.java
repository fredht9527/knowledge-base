package com.kb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kb.dto.KnowledgeDTO;
import com.kb.dto.PageRequest;
import com.kb.dto.PageResponse;
import com.kb.entity.Category;
import com.kb.entity.Knowledge;
import com.kb.entity.Tag;
import com.kb.mapper.CategoryMapper;
import com.kb.mapper.KnowledgeMapper;
import com.kb.mapper.TagMapper;
import com.kb.search.SearchDocument;
import com.kb.search.SearchService;
import com.kb.search.SearchService.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识条目业务逻辑 - 分页查询、全文搜索、CRUD、标签管理
 */
@Slf4j
@Service
public class KnowledgeService {

    private final KnowledgeMapper knowledgeMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    // [FIX]: 注入 SearchService，检索走 ES 而非 MySQL LIKE
    private final SearchService searchService;
    private final JdbcTemplate jdbc;

    public KnowledgeService(KnowledgeMapper knowledgeMapper, CategoryMapper categoryMapper,
                            TagMapper tagMapper, SearchService searchService, DataSource dataSource) {
        this.knowledgeMapper = knowledgeMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.searchService = searchService;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    /**
     * [FIX]: 分页查询知识条目 - 全部走 ES，不再查 MySQL
     */
    public PageResponse<KnowledgeDTO> page(PageRequest request) {
        try {
            // [FIX]: 所有关键词/分类/状态筛选都走 ES
            List<SearchDocument> docs = searchService.searchByTypeWithFilter(
                    "knowledge", request.getKeyword(),
                    request.getCategoryId(), request.getStatus(),
                    request.getPage(), request.getSize());
            long total = searchService.countByTypeWithFilter(
                    "knowledge", request.getKeyword(),
                    request.getCategoryId(), request.getStatus());

            List<KnowledgeDTO> dtos = docs.stream()
                    .map(this::docToDTO)
                    .collect(Collectors.toList());
            return PageResponse.of(dtos, total, request.getPage(), request.getSize());
        } catch (Exception e) {
            log.warn("ES 分页查询失败，降级为 MySQL: {}", e.getMessage());
            return fallbackPage(request);
        }
    }

    /**
     * [FIX]: ES 文档转 KnowledgeDTO
     */
    private KnowledgeDTO docToDTO(SearchDocument doc) {
        if (doc == null) return null;
        KnowledgeDTO dto = new KnowledgeDTO();
        dto.setId(doc.getOriginalId());
        dto.setTitle(doc.getTitle());
        dto.setContent(doc.getContent());
        dto.setSummary(doc.getSummary());
        dto.setCategoryName(doc.getCategoryName());
        dto.setCategoryId(doc.getCategoryId());
        dto.setStatus(doc.getStatus());
        dto.setViewCount(doc.getViewCount() != null ? doc.getViewCount() : 0);
        if (doc.getTags() != null) dto.setTags(new HashSet<>(doc.getTags()));
        if (doc.getCreatedAt() != null) dto.setCreatedAt(doc.getCreatedAt());
        if (doc.getUpdatedAt() != null) dto.setUpdatedAt(doc.getUpdatedAt());
        return dto;
    }

    /**
     * [FIX]: MySQL 降级分页（仅在 ES 不可用时使用）
     */
    private PageResponse<KnowledgeDTO> fallbackPage(PageRequest request) {
        Page<Knowledge> page = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<Knowledge> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(Knowledge::getUpdatedAt);
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            qw.and(w -> w.like(Knowledge::getTitle, request.getKeyword())
                    .or().like(Knowledge::getContent, request.getKeyword()));
        }
        if (request.getCategoryId() != null) qw.eq(Knowledge::getCategoryId, request.getCategoryId());
        if (request.getStatus() != null) qw.eq(Knowledge::getStatus, request.getStatus());
        knowledgeMapper.selectPage(page, qw);
        List<KnowledgeDTO> list = page.getRecords().stream().map(this::toDTO).collect(Collectors.toList());
        return PageResponse.of(list, page.getTotal(), request.getPage(), request.getSize());
    }

    /**
     * [FIX]: 根据 ID 获取知识详情 - 优先从 ES 获取，浏览计数仍用 MySQL
     */
    public KnowledgeDTO getById(Long id) {
        // [FIX]: 优先从 ES 获取完整数据
        SearchDocument doc = searchService.getDocument("knowledge", id);
        if (doc != null) {
            KnowledgeDTO dto = docToDTO(doc);
            // 浏览计数仍需 MySQL 更新（ES 不适合计数操作）
            try {
                Knowledge k = knowledgeMapper.selectById(id);
                if (k != null) {
                    k.setViewCount(k.getViewCount() + 1);
                    knowledgeMapper.updateById(k);
                    dto.setViewCount(k.getViewCount());
                }
            } catch (Exception e) {
                log.warn("浏览计数更新失败: {}", e.getMessage());
            }
            return dto;
        }
        // ES 没有则从 MySQL 获取
        Knowledge k = knowledgeMapper.selectById(id);
        if (k == null) throw new RuntimeException("知识条目不存在");
        k.setViewCount(k.getViewCount() + 1);
        knowledgeMapper.updateById(k);
        return toDTO(k);
    }

    /** 新增知识条目 */
    @Transactional
    public KnowledgeDTO create(KnowledgeDTO dto) {
        Knowledge k = new Knowledge();
        setFields(k, dto);
        knowledgeMapper.insert(k);
        Long kid = k.getId();
        // 保存标签关联
        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            saveTags(kid, dto.getTags());
        }
        // 关联附件
        if (dto.getFileIds() != null && !dto.getFileIds().isEmpty()) {
            linkFiles(kid, dto.getFileIds());
        }
        // [FIX]: 实时同步到 ES
        syncToEs(k);
        return toDTO(k);
    }

    /** 更新知识条目 */
    @Transactional
    public KnowledgeDTO update(Long id, KnowledgeDTO dto) {
        Knowledge k = knowledgeMapper.selectById(id);
        if (k == null) throw new RuntimeException("知识条目不存在");
        setFields(k, dto);
        knowledgeMapper.updateById(k);
        // 重新保存标签关联
        jdbc.update("DELETE FROM knowledge_tag WHERE knowledge_id = ?", id);
        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            saveTags(id, dto.getTags());
        }
        // 关联附件
        if (dto.getFileIds() != null && !dto.getFileIds().isEmpty()) {
            linkFiles(id, dto.getFileIds());
        }
        // [FIX]: 更新后重新同步到 ES
        syncToEs(k);
        return toDTO(k);
    }

    /** 删除知识条目 */
    @Transactional
    public void delete(Long id) {
        jdbc.update("DELETE FROM knowledge_tag WHERE knowledge_id = ?", id);
        knowledgeMapper.deleteById(id);
        // [FIX]: 删除 ES 索引
        searchService.deleteDocument("knowledge", id);
    }

    /**
     * [FIX]: 供AI对话使用的知识检索 - 改为走 Elasticsearch 混合检索
     * BM25 全文检索 + kNN 语义向量检索，替代原 MySQL LIKE 查询
     * 搜索范围涵盖知识条目 + 附件提取文本，语义理解同义词/近义词
     */
    public List<KnowledgeDTO> searchForChat(String keyword, int size) {
        if (keyword == null || keyword.isBlank()) return List.of();
        int limit = Math.min(size, 10);

        // [FIX]: 调用 ES 混合检索，替代 MySQL LIKE 全表扫描
        List<SearchResult> results = searchService.hybridSearch(keyword, limit);

        // 将 ES 搜索结果转换为 KnowledgeDTO（兼容前端接口格式）
        return results.stream().map(r -> {
            KnowledgeDTO dto = new KnowledgeDTO();
            dto.setId(r.getOriginalId());
            dto.setTitle(r.getTitle());
            // 截断 content 到 500 字符避免 token 超限
            String c = r.getContent();
            dto.setContent(c != null && c.length() > 500 ? c.substring(0, 500) + "..." : c);
            dto.setSummary(r.getSummary());
            dto.setCategoryName(r.getCategoryName());

            // 如果是附件类型，标注来源文件
            if ("attachment".equals(r.getType()) && r.getFileName() != null) {
                String existingContent = dto.getContent() != null ? dto.getContent() : "";
                dto.setContent(existingContent + "\n\n[来源文件: " + r.getFileName() + "]");
            }

            // 如果附件关联了知识条目，用 knowledgeId 作为主 ID
            if ("attachment".equals(r.getType()) && r.getKnowledgeId() != null) {
                dto.setId(r.getKnowledgeId());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    /** 设置知识条目字段 */
    private void setFields(Knowledge k, KnowledgeDTO dto) {
        k.setTitle(dto.getTitle());
        k.setContent(dto.getContent());
        k.setSummary(dto.getSummary());
        k.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        k.setCategoryId(dto.getCategoryId());
    }

    /**
     * 保存标签：不存在则创建，存在则复用，然后写入 knowledge_tag 中间表
     */
    private void saveTags(Long knowledgeId, Set<String> tagNames) {
        for (String name : tagNames) {
            LambdaQueryWrapper<Tag> qw = new LambdaQueryWrapper<>();
            qw.eq(Tag::getName, name);
            Tag tag = tagMapper.selectOne(qw);
            if (tag == null) {
                tag = new Tag();
                tag.setName(name);
                tagMapper.insert(tag);
            }
            // 写入知识-标签关联表
            jdbc.update("INSERT IGNORE INTO knowledge_tag (knowledge_id, tag_id) VALUES (?, ?)",
                    knowledgeId, tag.getId());
        }
    }

    /** 关联上传的附件到知识条目 */
    private void linkFiles(Long knowledgeId, List<Long> fileIds) {
        for (Long fid : fileIds) {
            jdbc.update("UPDATE attachment SET knowledge_id = ? WHERE id = ?", knowledgeId, fid);
        }
        log.info("已关联 {} 个附件到知识 {}", fileIds.size(), knowledgeId);
    }

    /** [FIX]: 同步知识条目到 ES */
    private void syncToEs(Knowledge k) {
        try {
            String categoryName = null;
            if (k.getCategoryId() != null) {
                Category cat = categoryMapper.selectById(k.getCategoryId());
                if (cat != null) categoryName = cat.getName();
            }
            searchService.indexKnowledge(k.getId(), k.getTitle(), k.getContent(),
                    k.getSummary(), categoryName, k.getCategoryId(), k.getStatus(), k.getViewCount(),
                    new java.util.ArrayList<>(getTagNames(k.getId())));
        } catch (Exception e) {
            log.warn("同步知识条目到 ES 失败: id={}, error={}", k.getId(), e.getMessage());
        }
    }

    /** 查询知识关联的标签名称列表 */
    private Set<String> getTagNames(Long knowledgeId) {
        List<String> names = jdbc.queryForList(
                "SELECT t.name FROM tag t JOIN knowledge_tag kt ON t.id = kt.tag_id WHERE kt.knowledge_id = ?",
                String.class, knowledgeId);
        return new HashSet<>(names);
    }

    /** 实体转 DTO */
    private KnowledgeDTO toDTO(Knowledge k) {
        KnowledgeDTO dto = new KnowledgeDTO();
        dto.setId(k.getId());
        dto.setTitle(k.getTitle());
        dto.setContent(k.getContent());
        dto.setSummary(k.getSummary());
        dto.setStatus(k.getStatus());
        dto.setViewCount(k.getViewCount());
        dto.setCategoryId(k.getCategoryId());

        // 填充分类名称
        if (k.getCategoryId() != null) {
            Category c = categoryMapper.selectById(k.getCategoryId());
            if (c != null) dto.setCategoryName(c.getName());
        }

        // 填充标签名称
        dto.setTags(getTagNames(k.getId()));

        if (k.getCreatedAt() != null) dto.setCreatedAt(k.getCreatedAt().toString());
        if (k.getUpdatedAt() != null) dto.setUpdatedAt(k.getUpdatedAt().toString());
        return dto;
    }
}
