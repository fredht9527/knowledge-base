package com.kb.controller;

import com.kb.dto.Result;
import com.kb.search.DataSyncService;
import com.kb.search.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * [FIX]: 搜索管理接口 - 提供混合检索和数据同步能力
 * 替代原 KnowledgeController 中的 MySQL LIKE 搜索端点
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final DataSyncService dataSyncService;

    /**
     * 混合检索 - BM25 全文 + kNN 语义向量
     * 供 AI 聊天和前端搜索使用
     */
    @GetMapping
    public Result<List<SearchService.SearchResult>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "5") Integer size) {
        return Result.success(searchService.hybridSearch(keyword, size));
    }

    /**
     * 聊天记录语义搜索
     */
    @GetMapping("/chat")
    public Result<List<SearchService.SearchResult>> searchChat(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(searchService.searchChatHistory(keyword, size));
    }

    /**
     * 手动触发全量同步（MySQL → ES）
     */
    @PostMapping("/sync")
    public Result<String> fullSync() {
        dataSyncService.fullSyncFromApi();
        return Result.success("全量同步完成");
    }
}
