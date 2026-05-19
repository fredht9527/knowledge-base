package com.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识分类实体 - 支持树形结构，通过 parentId 实现无限层级
 */
@Data
@TableName("category")
public class Category {

    /** 分类ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 */
    private String name;

    /** 分类描述 */
    private String description = "";

    /** 父分类ID，null 表示根分类 */
    @TableField("parent_id")
    private Long parentId;

    /** 排序序号 */
    @TableField("sort_order")
    private Integer sortOrder = 0;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 子分类列表（非数据库字段，仅用于树形展示） */
    @TableField(exist = false)
    private List<Category> children;
}
