package com.kb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kb.entity.AiConfig;
import com.kb.mapper.AiConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * AI配置业务逻辑 - 配置的读取、保存、加密存储
 */
@Slf4j
@Service
public class AiConfigService {

    private final AiConfigMapper aiConfigMapper;

    public AiConfigService(AiConfigMapper aiConfigMapper) {
        this.aiConfigMapper = aiConfigMapper;
    }

    /** 获取全局AI配置 */
    public AiConfig getConfig() {
        AiConfig config = aiConfigMapper.selectOne(new LambdaQueryWrapper<AiConfig>().isNull(AiConfig::getUserId));
        if (config == null) {
            // 返回默认配置
            config = new AiConfig();
            config.setProvider("openai");
            config.setModel("gpt-3.5-turbo");
            config.setTemperature(new java.math.BigDecimal("0.7"));
            config.setMaxTokens(2048);
        } else {
            // 解密 API Key
            if (config.getApiKey() != null) {
                try {
                    config.setApiKey(new String(Base64.getDecoder().decode(config.getApiKey())));
                } catch (Exception e) {
                    log.warn("API Key 解密失败，可能是明文存储");
                }
            }
        }
        return config;
    }

    /** 保存/更新全局AI配置 */
    public void saveConfig(AiConfig config) {
        // Base64 加密 API Key（简单混淆，非安全加密）
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            config.setApiKey(Base64.getEncoder().encodeToString(config.getApiKey().getBytes()));
        }

        AiConfig existing = aiConfigMapper.selectOne(new LambdaQueryWrapper<AiConfig>().isNull(AiConfig::getUserId));
        if (existing != null) {
            config.setId(existing.getId());
            config.setUserId(null);
            aiConfigMapper.updateById(config);
        } else {
            aiConfigMapper.insert(config);
        }
        log.info("AI配置已保存: provider={}, model={}", config.getProvider(), config.getModel());
    }
}
