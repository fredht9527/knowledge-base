package com.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 分类数据传输对象 - 用于前后端数据交互和参数校验
 */
@Data
public class CategoryDTO {
    private Long id;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称最长100字符")
    private String name;

    @Size(max = 500, message = "分类描述最长500字符")
    private String description;

    /** 父分类ID */
    private Long parentId;

    private Integer sortOrder;

    /** 子分类列表（树形展示用） */
    private List<CategoryDTO> children;

    private String createdAt;
    private String updatedAt;
}
