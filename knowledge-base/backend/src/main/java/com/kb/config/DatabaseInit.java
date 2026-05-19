package com.kb.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 数据库初始化 - 启动时自动修复和创建表结构
 */
@Slf4j
@Component
public class DatabaseInit {

    private final JdbcTemplate jdbc;

    public DatabaseInit(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    public void init() {
        log.info("数据库检查...");
        fixAttachmentTable();
        createUserTable();
        createChatTables();
        createEmailCodeTable();
        log.info("数据库检查完成");
    }

    /** 创建 user 表 */
    private void createUserTable() {
        try {
            jdbc.execute("CREATE TABLE IF NOT EXISTS `user` (" +
                    "`id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID'," +
                    "`email` VARCHAR(255) NOT NULL UNIQUE COMMENT '邮箱（唯一标识）'," +
                    "`nickname` VARCHAR(100) DEFAULT '' COMMENT '昵称'," +
                    "`password` VARCHAR(255) NOT NULL COMMENT '密码'," +
                    "`avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL'," +
                    "`gender` VARCHAR(10) DEFAULT '保密' COMMENT '性别：男/女/保密'," +
                    "`phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号'," +
                    "`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "INDEX `idx_email` (`email`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表'");
            log.info("user 表已就绪");
            // 兼容已有表：补充新增列
            try { jdbc.execute("ALTER TABLE `user` ADD COLUMN `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL' AFTER `password`"); } catch (Exception ignored) {}
            try { jdbc.execute("ALTER TABLE `user` ADD COLUMN `gender` VARCHAR(10) DEFAULT '保密' COMMENT '性别' AFTER `avatar`"); } catch (Exception ignored) {}
            try { jdbc.execute("ALTER TABLE `user` ADD COLUMN `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号' AFTER `gender`"); } catch (Exception ignored) {}
        } catch (Exception e) {
            log.warn("创建 user 表失败: {}", e.getMessage());
        }
    }

    /** 创建 email_code 表 */
    private void createEmailCodeTable() {
        try {
            jdbc.execute("CREATE TABLE IF NOT EXISTS email_code (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID'," +
                    "email VARCHAR(255) NOT NULL COMMENT '邮箱地址'," +
                    "code VARCHAR(10) NOT NULL COMMENT '验证码'," +
                    "type VARCHAR(20) NOT NULL DEFAULT 'register' COMMENT '验证码类型：register-注册, login-登录, reset-重置密码'," +
                    "expire_time DATETIME NOT NULL COMMENT '过期时间'," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "INDEX idx_email_type (email, type)," +
                    "INDEX idx_expire_time (expire_time)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮箱验证码表'");
            log.info("email_code 表已就绪");
        } catch (Exception e) {
            log.warn("创建 email_code 表失败: {}", e.getMessage());
        }
    }

    /** 修复 attachment 表：添加 file_data 列并清理旧列 */
    private void fixAttachmentTable() {
        try {
            jdbc.queryForObject("SELECT COUNT(*) FROM attachment", Integer.class);
        } catch (Exception e) {
            log.info("attachment 表不存在，跳过修复");
            return;
        }
        try {
            jdbc.execute("ALTER TABLE attachment ADD COLUMN file_data LONGBLOB AFTER file_type");
            log.info("已添加 file_data 列");
        } catch (Exception ignore) {}

        try {
            jdbc.execute("ALTER TABLE attachment MODIFY COLUMN file_data LONGBLOB NOT NULL");
        } catch (Exception ignore) {}

        try {
            jdbc.execute("ALTER TABLE attachment DROP COLUMN file_path");
            log.info("已删除旧的 file_path 列");
        } catch (Exception ignore) {}

        try {
            jdbc.execute("ALTER TABLE attachment MODIFY COLUMN knowledge_id BIGINT DEFAULT NULL");
        } catch (Exception ignore) {}

        // [FIX]: 添加 file_text 列，用于存储从文件中提取的纯文本内容（供搜索使用）
        try {
            jdbc.execute("ALTER TABLE attachment ADD COLUMN file_text LONGTEXT DEFAULT NULL COMMENT '从文件提取的纯文本内容' AFTER file_data");
            log.info("已添加 file_text 列");
        } catch (Exception ignore) {}
    }

    /** 创建 AI 对话相关表 */
    private void createChatTables() {
        // chat_session 表
        try {
            jdbc.execute("CREATE TABLE IF NOT EXISTS chat_session (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID'," +
                    "title VARCHAR(200) DEFAULT '新对话' COMMENT '会话标题'," +
                    "user_id BIGINT DEFAULT NULL COMMENT '用户ID'," +
                    "status TINYINT DEFAULT 0 COMMENT '状态:0正常 1归档 2删除'," +
                    "model VARCHAR(100) DEFAULT 'gpt-3.5-turbo' COMMENT 'AI模型'," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "INDEX idx_user (user_id)," +
                    "INDEX idx_status (status)," +
                    "INDEX idx_updated (updated_at)" +
                    ") COMMENT='AI对话会话表'");
            log.info("chat_session 表已就绪");
        } catch (Exception e) {
            log.warn("创建 chat_session 表失败: {}", e.getMessage());
        }

        // chat_message 表
        try {
            jdbc.execute("CREATE TABLE IF NOT EXISTS chat_message (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID'," +
                    "session_id BIGINT NOT NULL COMMENT '会话ID'," +
                    "role ENUM('user','assistant','system') NOT NULL COMMENT '角色'," +
                    "content TEXT NOT NULL COMMENT '消息内容'," +
                    "thinking MEDIUMTEXT DEFAULT NULL COMMENT '思考/推理内容'," +
                    "image_urls JSON DEFAULT NULL COMMENT '图片URL列表'," +
                    "tokens_used INT DEFAULT NULL COMMENT '消耗token数'," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "INDEX idx_session (session_id)," +
                    "INDEX idx_created (created_at)" +
                    ") COMMENT='AI对话消息表'");
            // [FIX]: thinking 列从 TEXT 升级为 MEDIUMTEXT，防止 AI 深度推理内容超长导致 Data truncation
            try {
                jdbc.execute("ALTER TABLE chat_message MODIFY COLUMN thinking MEDIUMTEXT DEFAULT NULL COMMENT '思考/推理内容'");
                log.info("已将 thinking 列升级为 MEDIUMTEXT");
            } catch (Exception ignored) {}
            log.info("chat_message 表已就绪");
            // 添加排序索引，避免 sort buffer 溢出
            try {
                jdbc.execute("CREATE INDEX idx_chat_msg_session_time ON chat_message(session_id, created_at)");
            } catch (Exception ignored) {}
        } catch (Exception e) {
            log.warn("创建 chat_message 表失败: {}", e.getMessage());
        }

        // ai_config 表
        try {
            jdbc.execute("CREATE TABLE IF NOT EXISTS ai_config (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID'," +
                    "user_id BIGINT DEFAULT NULL COMMENT '用户ID'," +
                    "provider VARCHAR(50) DEFAULT 'openai' COMMENT '服务商'," +
                    "api_key VARCHAR(500) COMMENT 'API密钥(加密)'," +
                    "model VARCHAR(100) DEFAULT 'gpt-3.5-turbo' COMMENT '模型'," +
                    "api_url VARCHAR(500) COMMENT '自定义API地址'," +
                    "temperature DECIMAL(3,2) DEFAULT 0.70 COMMENT '温度'," +
                    "max_tokens INT DEFAULT 2048 COMMENT '最大token'," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "UNIQUE KEY uk_user (user_id)" +
                    ") COMMENT='AI配置表'");
            log.info("ai_config 表已就绪");
        } catch (Exception e) {
            log.warn("创建 ai_config 表失败: {}", e.getMessage());
        }
    }
}
