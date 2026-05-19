package com.kb.controller;

import com.kb.dto.Result;
import com.kb.entity.AiConfig;
import com.kb.service.AiConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI配置控制器 - 支持查看和保存AI服务配置
 */
@Slf4j
@RestController
@RequestMapping("/api/ai-config")
public class AiConfigController {

    private final AiConfigService aiConfigService;

    public AiConfigController(AiConfigService aiConfigService) {
        this.aiConfigService = aiConfigService;
    }

    /**
     * 获取当前AI配置（API Key会自动解密返回）
     */
    @GetMapping
    public Result<AiConfig> getConfig() {
        return Result.success(aiConfigService.getConfig());
    }

    /**
     * 保存AI配置（API Key会自动加密存储）
     */
    @PostMapping
    public Result<Void> saveConfig(@RequestBody AiConfig config) {
        aiConfigService.saveConfig(config);
        return Result.success();
    }
}
