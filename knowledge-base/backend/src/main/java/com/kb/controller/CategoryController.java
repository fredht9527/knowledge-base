package com.kb.controller;

import com.kb.dto.CategoryDTO;
import com.kb.dto.Result;
import com.kb.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理接口
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** 获取分类树（带层级结构） */
    @GetMapping("/tree")
    public Result<List<CategoryDTO>> getTree() {
        return Result.success(categoryService.getTree());
    }

    /** 获取所有分类（平铺列表） */
    @GetMapping
    public Result<List<CategoryDTO>> getAll() {
        return Result.success(categoryService.getAll());
    }

    /** 根据 ID 获取分类 */
    @GetMapping("/{id}")
    public Result<CategoryDTO> getById(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    /** 新增分类 */
    @PostMapping
    public Result<CategoryDTO> create(@Valid @RequestBody CategoryDTO dto) {
        return Result.success(categoryService.create(dto));
    }

    /** 编辑分类 */
    @PutMapping("/{id}")
    public Result<CategoryDTO> update(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        return Result.success(categoryService.update(id, dto));
    }

    /** 删除分类 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
