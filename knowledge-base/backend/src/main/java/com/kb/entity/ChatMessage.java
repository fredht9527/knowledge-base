package com.kb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI对话消息实体 - 存储每条消息的内容和元数据
 */
@Data
@TableName("chat_message")
public class ChatMessage {
    /** 消息ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属会话ID */
    private Long sessionId;

    /** 消息角色：user(用户)、assistant(AI)、system(系统) */
    private String role;

    /** 消息文本内容 */
    private String content;

    /** 思考/推理内容（reasoning_content） */
    private String thinking;

    /** 图片URL列表（JSON数组，如 ["url1","url2"]） */
    private String imageUrls;

    /** [FIX]: 附件ID列表（JSON数组，如 [1,2,3]，关联 attachment 表） */
    private String attachmentIds;

    /** 消耗的token数 */
    private Integer tokensUsed;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
