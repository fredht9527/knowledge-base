-- =============================================
-- 知识库系统 - 认证模块数据库初始化脚本
-- 执行前请先确保 knowledge_base 数据库已创建
-- =============================================

-- 使用 knowledge_base 数据库
USE knowledge_base;

-- =============================================
-- 用户表
-- =============================================
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `email` VARCHAR(255) NOT NULL UNIQUE COMMENT '邮箱（唯一标识）',
    `nickname` VARCHAR(100) DEFAULT '' COMMENT '昵称',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =============================================
-- 邮箱验证码表
-- =============================================
CREATE TABLE IF NOT EXISTS `email_code` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `email` VARCHAR(255) NOT NULL COMMENT '邮箱地址',
    `code` VARCHAR(10) NOT NULL COMMENT '验证码',
    `type` VARCHAR(20) NOT NULL DEFAULT 'register' COMMENT '验证码类型：register-注册, login-登录, reset-重置密码',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_email_type` (`email`, `type`),
    INDEX `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮箱验证码表';

-- =============================================
-- 初始化测试用户（可选，用于开发测试）
-- =============================================
-- INSERT INTO `user` (`email`, `nickname`, `password`) VALUES
-- ('test@example.com', '测试用户', '123456');
