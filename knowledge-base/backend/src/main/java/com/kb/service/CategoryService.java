package com.kb.service;

import com.kb.dto.CategoryDTO;
import com.kb.entity.Category;
import com.kb.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类业务逻辑 - 树形结构管理、CRUD
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    /** 获取分类树（包含父子层级） */
    public List<CategoryDTO> getTree() {
        List<Category> all = categoryMapper.selectList(null);
        // 筛选根节点（parentId 为 null），按 sortOrder 排序
        List<Category> roots = all.stream()
                .filter(c -> c.getParentId() == null)
                .sorted(Comparator.comparingInt(Category::getSortOrder))
                .collect(Collectors.toList());
        return roots.stream().map(r -> toTreeDTO(r, all)).collect(Collectors.toList());
    }

    /** 获取所有分类（平铺列表） */
    public List<CategoryDTO> getAll() {
        return categoryMapper.selectList(null).stream().map(this::toDTO).collect(Collectors.toList());
    }

    /** 根据 ID 获取分类 */
    public CategoryDTO getById(Long id) {
        Category c = categoryMapper.selectById(id);
        if (c == null) throw new RuntimeException("分类不存在");
        return toDTO(c);
    }

    /** 新增分类 */
    @Transactional
    public CategoryDTO create(CategoryDTO dto) {
        Category c = new Category();
        c.setName(dto.getName());
        c.setDescription(dto.getDescription());
        c.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        c.setParentId(dto.getParentId());
        categoryMapper.insert(c);
        return toDTO(c);
    }

    /** 编辑分类 */
    @Transactional
    public CategoryDTO update(Long id, CategoryDTO dto) {
        Category c = categoryMapper.selectById(id);
        if (c == null) throw new RuntimeException("分类不存在");
        c.setName(dto.getName());
        c.setDescription(dto.getDescription());
        if (dto.getSortOrder() != null) c.setSortOrder(dto.getSortOrder());
        c.setParentId(dto.getParentId());
        categoryMapper.updateById(c);
        return toDTO(c);
    }

    /** 删除分类 */
    @Transactional
    public void delete(Long id) {
        categoryMapper.deleteById(id);
    }

    /** 实体转 DTO */
    private CategoryDTO toDTO(Category c) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setDescription(c.getDescription());
        dto.setSortOrder(c.getSortOrder());
        dto.setParentId(c.getParentId());
        if (c.getCreatedAt() != null) dto.setCreatedAt(c.getCreatedAt().toString());
        if (c.getUpdatedAt() != null) dto.setUpdatedAt(c.getUpdatedAt().toString());
        return dto;
    }

    /** 递归构建树形 DTO */
    private CategoryDTO toTreeDTO(Category node, List<Category> all) {
        CategoryDTO dto = toDTO(node);
        List<Category> children = all.stream()
                .filter(c -> node.getId().equals(c.getParentId()))
                .sorted(Comparator.comparingInt(Category::getSortOrder))
                .collect(Collectors.toList());
        if (!children.isEmpty()) {
            dto.setChildren(children.stream().map(c -> toTreeDTO(c, all)).collect(Collectors.toList()));
        }
        return dto;
    }
}
