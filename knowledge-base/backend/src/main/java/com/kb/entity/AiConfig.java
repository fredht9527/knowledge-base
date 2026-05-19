package com.kb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI配置实体 - 每个用户的AI服务配置
 */
@Data
@TableName("ai_config")
public class AiConfig {
    /** 配置ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID（全局配置为null） */
    private Long userId;

    /** 服务商：openai/anthropic/azure/custom */
    private String provider;

    /** API密钥（加密存储） */
    private String apiKey;

    /** 模型名称 */
    private String model;

    /** 自定义API地址 */
    private String apiUrl;

    /** 温度参数0~2 */
    private BigDecimal temperature;

    /** 最大token数 */
    private Integer maxTokens;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
