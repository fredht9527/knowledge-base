-- 个人知识库系统数据库脚本 (MySQL 8.0+)
-- 支持知识管理、分类、标签、附件存储、AI对话

-- 分类表
CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT NULL COMMENT '父分类ID，NULL表示一级分类',
    sort_order INT DEFAULT 0 COMMENT '排序序号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent (parent_id),
    INDEX idx_sort (sort_order)
) COMMENT='分类表';

-- 标签表
CREATE TABLE IF NOT EXISTS tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_name (name)
) COMMENT='标签表';

-- 知识条目表
CREATE TABLE IF NOT EXISTS knowledge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '知识ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content LONGTEXT COMMENT 'Markdown内容',
    summary VARCHAR(500) COMMENT '摘要',
    category_id BIGINT COMMENT '分类ID',
    status TINYINT DEFAULT 0 COMMENT '状态:0草稿,1已发布',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at),
    FULLTEXT INDEX idx_content (title, content) WITH PARSER ngram
) COMMENT='知识条目表';

-- 知识-标签关联表
CREATE TABLE IF NOT EXISTS knowledge_tag (
    knowledge_id BIGINT NOT NULL COMMENT '知识ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    PRIMARY KEY (knowledge_id, tag_id),
    INDEX idx_tag (tag_id)
) COMMENT='知识标签关联表';

-- 附件表（文件以BLOB形式存储在数据库中）
CREATE TABLE IF NOT EXISTS attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '附件ID',
    knowledge_id BIGINT DEFAULT NULL COMMENT '关联的知识ID',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    file_type VARCHAR(200) COMMENT '文件MIME类型',
    file_data LONGBLOB COMMENT '文件二进制数据',
    file_text LONGTEXT DEFAULT NULL COMMENT '从文件提取的纯文本内容(供搜索使用)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_knowledge (knowledge_id),
    INDEX idx_file_text (file_text(100)) COMMENT '附件文本内容前缀索引(辅助LIKE搜索)'
) COMMENT='附件表';

-- ============================================
-- AI 对话相关表
-- ============================================

-- 对话会话表
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
    title VARCHAR(200) DEFAULT '新对话' COMMENT '会话标题',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID（未登录为NULL）',
    status TINYINT DEFAULT 0 COMMENT '状态:0正常,1归档,2删除',
    model VARCHAR(100) DEFAULT 'gpt-3.5-turbo' COMMENT '使用的AI模型',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_updated (updated_at)
) COMMENT='AI对话会话表';

-- 对话消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    session_id BIGINT NOT NULL COMMENT '所属会话ID',
    role ENUM('user', 'assistant', 'system') NOT NULL COMMENT '消息角色',
    content TEXT NOT NULL COMMENT '消息内容',
    thinking MEDIUMTEXT DEFAULT NULL COMMENT '思考/推理内容（reasoning_content）',
    image_urls JSON DEFAULT NULL COMMENT '图片URL列表（用户上传的图片）',
    attachment_ids JSON DEFAULT NULL COMMENT '附件ID列表（关联attachment表）',
    tokens_used INT DEFAULT NULL COMMENT '消耗的token数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session (session_id),
    INDEX idx_created (created_at)
) COMMENT='AI对话消息表';

-- AI配置表（每个用户的AI设置）
CREATE TABLE IF NOT EXISTS ai_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID（全局配置为NULL）',
    provider VARCHAR(50) DEFAULT 'openai' COMMENT '服务商:openai/anthropic/azure/custom',
    api_key VARCHAR(500) COMMENT 'API密钥（加密存储）',
    model VARCHAR(100) DEFAULT 'gpt-3.5-turbo' COMMENT '模型名称',
    api_url VARCHAR(500) COMMENT '自定义API地址',
    temperature DECIMAL(3,2) DEFAULT 0.7 COMMENT '温度参数0-2',
    max_tokens INT DEFAULT 2048 COMMENT '最大token数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user (user_id)
) COMMENT='AI配置表';

-- 插入默认分类
INSERT INTO category (name, parent_id, sort_order) VALUES
('测试', NULL, 0),
('学习笔记', NULL, 1),
('工作文档', NULL, 2)
ON DUPLICATE KEY UPDATE name = name;