package com.kb.controller;

import com.kb.dto.KnowledgeDTO;
import com.kb.dto.PageRequest;
import com.kb.dto.PageResponse;
import com.kb.dto.Result;
import com.kb.service.KnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 知识条目管理接口
 */
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    /** 分页查询知识条目（支持关键字搜索、分类筛选） */
    @GetMapping
    public Result<PageResponse<KnowledgeDTO>> page(PageRequest request) {
        return Result.success(knowledgeService.page(request));
    }

    /** 获取知识详情（自动增加浏览次数） */
    @GetMapping("/{id}")
    public Result<KnowledgeDTO> getById(@PathVariable Long id) {
        return Result.success(knowledgeService.getById(id));
    }

    /** 新增知识条目 */
    @PostMapping
    public Result<KnowledgeDTO> create(@Valid @RequestBody KnowledgeDTO dto) {
        return Result.success(knowledgeService.create(dto));
    }

    /** 编辑知识条目 */
    @PutMapping("/{id}")
    public Result<KnowledgeDTO> update(@PathVariable Long id, @Valid @RequestBody KnowledgeDTO dto) {
        return Result.success(knowledgeService.update(id, dto));
    }

    /** 删除知识条目 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.success();
    }

    /**
     * 轻量搜索接口：供AI对话时检索相关知识，只返回id/title/content/summary
     * [FIX]: 新增AI对话知识检索端点
     */
    @GetMapping("/search")
    public Result<?> search(@RequestParam String keyword,
                             @RequestParam(defaultValue = "5") Integer size) {
        return Result.success(knowledgeService.searchForChat(keyword, size));
    }
}
