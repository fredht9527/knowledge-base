package com.kb.controller;

import com.kb.dto.Result;
import com.kb.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签管理接口
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /** 获取所有标签名称 */
    @GetMapping
    public Result<List<String>> getAll() {
        return Result.success(tagService.getAllNames());
    }

    /** 删除标签 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.success();
    }
}
