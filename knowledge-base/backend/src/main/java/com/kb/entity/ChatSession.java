package com.kb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI对话会话实体 - 存储每次对话的会话元信息
 */
@Data
@TableName("chat_session")
public class ChatSession {
    /** 会话ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话标题（由首条消息自动生成） */
    private String title;

    /** 用户ID（未登录为null） */
    private Long userId;

    /** 状态：0正常 1归档 2删除 */
    private Integer status;

    /** 使用的AI模型 */
    private String model;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
