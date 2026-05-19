package com.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 知识条目实体 - 存储标题、Markdown 内容、状态等核心信息
 */
@Data
@TableName("knowledge")
public class Knowledge {

    /** 知识ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 内容（Markdown 格式） */
    private String content;

    /** 摘要 */
    private String summary = "";

    /** 所属分类ID */
    @TableField("category_id")
    private Long categoryId;

    /** 状态：0-草稿，1-已发布 */
    private Integer status = 1;

    /** 浏览次数 */
    @TableField("view_count")
    private Integer viewCount = 0;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 分类名称（非数据库字段，列表展示用） */
    @TableField(exist = false)
    private String categoryName;

    /** 标签列表（非数据库字段，前端传输用） */
    @TableField(exist = false)
    private Set<String> tags;
}
