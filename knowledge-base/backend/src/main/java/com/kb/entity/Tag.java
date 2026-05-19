package com.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签实体 - 知识条目的标签，多个知识可共用同一标签
 */
@Data
@TableName("tag")
public class Tag {

    /** 标签ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标签名称（唯一） */
    private String name;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
