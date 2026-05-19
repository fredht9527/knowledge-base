# 🧠 AI 知识助手 (Knowledge Base)

<div align="center">

**个人知识库 + AI 对话系统**

一款集知识管理、文件存储、AI 智能对话于一体的全栈 Web 应用

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-green.svg)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue.js-3.4-blue.svg)](https://vuejs.org/)
[![Element Plus](https://img.shields.io/badge/Element_Plus-2.6-blue.svg)](https://element-plus.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-orange.svg)](https://www.mysql.com/)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.x-00bfb3.svg)](https://www.elastic.co/)
[![Java 17](https://img.shields.io/badge/Java-17-red.svg)](https://openjdk.java.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

---

## 📖 目录

- [✨ 功能特性](#-功能特性)
- [🏗️ 系统架构](#-系统架构)
- [📁 项目结构](#-项目结构)
- [💻 技术栈详情](#-技术栈详情)
  - [后端技术栈](#后端技术栈)
  - [前端技术栈](#前端技术栈)
- [🚀 快速开始](#-快速开始)
- [⚙️ 配置说明](#️-配置说明)
- [🗄️ 数据库设计](#️-数据库设计)
- [📡 API 接口文档](#-api-接口文档)
- [🎨 前端页面与组件](#-前端页面与组件)
- [🔍 搜索引擎配置](#-搜索引擎配置)
- [🐳 Docker 部署](#-docker-部署)
- [📝 开发指南](#-开发指南)

---

## ✨ 功能特性

### 🔐 用户认证
- **邮箱注册登录**：支持邮箱验证码注册、密码登录
- **JWT Token 认证**：无状态认证，Token 有效期 2 小时
- **用户资料管理**：头像上传（本地存储/微信头像代理）、昵称修改、性别/手机号设置
- **EmailJS 邮件服务**：发送注册/登录验证码

### 📚 知识库管理
- **知识条目 CRUD**：创建、编辑、删除、查看知识条目
- **Markdown 编辑器**：支持 Markdown 格式内容编写和渲染
- **分类树形管理**：支持多级分类，无限层级嵌套
- **标签系统**：为知识添加标签，便于检索
- **全文搜索**：基于 Elasticsearch 的 BM25 中文分词搜索
- **浏览统计**：自动记录知识条目浏览次数

### 📁 文件管理
- **多格式文件上传**：支持 PDF、Word、Excel、PPT、Markdown、图片、代码等 30+ 种格式
- **BLOB 存储**：文件以二进制形式存储在 MySQL LONGBLOB 字段（单文件最大 200MB）
- **文本提取**：使用 Apache Tika 自动提取文件文本内容（PDF/DOCX/XLSX/PPTX/TXT 等）
- **在线预览**：浏览器内直接预览 PDF、图片、文本类文件
- **智能归类**：根据文件扩展名自动创建对应分类并归档
- **附件关联**：文件可关联到知识条目，形成知识-附件关系

### 🤖 AI 对话系统
- **多模型支持**：兼容 OpenAI / Anthropic / Azure / 自定义 API 地址
- **SSE 流式输出**：实时展示 AI 回复，打字机效果
- **会话管理**：新建、重命名、归档、删除会话；会话历史分页加载
- **消息持久化**：所有对话记录保存到数据库，支持断点续聊
- **图片理解**：支持发送图片给 AI 进行识别和分析
- **OCR 识别**：内置 OCR 服务，将图片转为文字后送入不支持图片的模型
- **文件对话**：可在聊天中上传文件，AI 可读取文件内容进行问答
- **思维链显示**：支持展示 AI 的思考/推理过程（reasoning_content）
- **知识库增强 (RAG)**：AI 对话时自动从知识库检索相关内容作为上下文
- **混合搜索**：BM25 全文检索 + kNN 语义向量检索（RRF 融合）

### 🎯 其他特性
- **响应式设计**：适配桌面端和移动端
- **暗色主题**：护眼暗色 UI 设计
- **滑动验证码**：登录/注册时的安全验证
- **头像代理**：解决微信 CDN 防盗链问题（Caffeine 本地缓存 + 浏览器强缓存）
- **数据同步**：MySQL → Elasticsearch 全量/增量同步

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                           客户端 (Browser)                          │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    Vue 3 + Element Plus                      │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────────┐  │   │
│  │  │ 登录/注册 │  │ AI 对话  │  │ 知识库   │  │ 分类/标签   │  │   │
│  │  └──────────┘  └──────────┘  └──────────┘  └─────────────┘  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              │                                      │
│                              ▼ (HTTP/SSE)                           │
├─────────────────────────────────────────────────────────────────────┤
│                         后端服务 (8080)                             │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                   Spring Boot 3.2.5                         │   │
│  │  ┌────────────┬─────────────┬────────────┬───────────────┐  │   │
│  │  │ Controller │  Service    │  Mapper    │    Config     │  │   │
│  │  ├────────────┼─────────────┼────────────┼───────────────┤  │   │
│  │  │ AuthCtrl   │ AuthService │ UserMapper │ CorsConfig    │  │   │
│  │  │ UserCtrl   │ UserService │ Knowledge  │ DBInit        │  │   │
│  │  │ ChatCtrl   │ ChatService │ Category   │ MPConfig      │  │   │
│  │  │ UploadCtrl │ KnowledgeS  │ Attachment │ RTConfig      │  │   │
│  │  │ SearchCtrl │ SearchSvc   │ ChatMsg    │              │  │   │
│  │  │ AiConfig   │ Embedding   │ ChatSession│              │  │   │
│  │  │ Category   │ DataSync    │ AiConfig   │              │  │   │
│  │  │ TagCtrl    │ OcrService  │ TagMapper  │              │  │   │
│  │  └────────────┴─────────────┴────────────┴───────────────┘  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│         │                │                  │                     │
│         ▼                ▼                  ▼                     │
│  ┌────────────┐  ┌────────────┐  ┌──────────────────────┐       │
│  │   MySQL    │  │Elasticsearch│  │   外部 AI API        │       │
│  │   8.0+     │  │   8.x      │  │ OpenAI/Anthropic/..  │       │
│  │  :3306     │  │  :9200     │  │                      │       │
│  └────────────┘  └────────────┘  └──────────────────────┘       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 📁 项目结构

```
knowledge-base/
├── backend/                        # 后端 Spring Boot 项目
│   ├── src/main/java/com/kb/       # Java 源码根目录
│   │   ├── KnowledgeBaseApplication.java    # 应用启动类
│   │   ├── config/                 # 配置类
│   │   │   ├── CorsConfig.java             # 跨域配置
│   │   │   ├── DatabaseInit.java           # 数据库初始化（启动时自动建表）
│   │   │   ├── MyMetaObjectHandler.java    # MyBatis-Plus 自动填充
│   │   │   ├── MybatisPlusConfig.java      # MyBatis-Plus 分页插件
│   │   │   └── RestTemplateConfig.java     # RestTemplate Bean
│   │   ├── controller/            # 控制器层
│   │   │   ├── AuthController.java          # 认证接口（注册/登录/验证码）
│   │   │   ├── UserController.java          # 用户资料接口
│   │   │   ├── ChatController.java          # AI 对话接口
│   │   │   ├── ChatImageController.java     # 聊天图片上传/访问
│   │   │   ├── KnowledgeController.java     # 知识条目 CRUD
│   │   │   ├── CategoryController.java      # 分类管理
│   │   │   ├── TagController.java           # 标签管理
│   │   │   ├── UploadController.java        # 文件上传/下载/预览
│   │   │   ├── SearchController.java        # ES 搜索接口
│   │   │   └── AiConfigController.java      # AI 配置管理
│   │   ├── service/               # 业务逻辑层
│   │   │   ├── AuthService.java             # 认证业务逻辑
│   │   │   ├── UserService.java             # 用户业务逻辑
│   │   │   ├── ChatService.java             # 对话业务逻辑
│   │   │   ├── KnowledgeService.java        # 知识库业务逻辑
│   │   │   ├── CategoryService.java         # 分类业务逻辑
│   │   │   ├── TagService.java              # 标签业务逻辑
│   │   │   └── AiConfigService.java         # AI 配置业务逻辑
│   │   ├── search/                # 搜索引擎模块
│   │   │   ├── SearchService.java            # 统一搜索服务（BM25+kNN混合）
│   │   │   ├── SearchRepository.java         # ES Repository
│   │   │   ├── SearchDocument.java           # 搜索文档实体
│   │   │   ├── SearchConfig.java             # 搜索配置
│   │   │   ├── EmbeddingService.java         # 向量嵌入服务（智谱AI）
│   │   │   ├── DataSyncService.java          # MySQL→ES 数据同步
│   │   │   └── OcrService.java               # OCR 文字识别
│   │   ├── mapper/                # MyBatis-Plus Mapper 接口
│   │   │   ├── UserMapper.java
│   │   │   ├── KnowledgeMapper.java
│   │   │   ├── CategoryMapper.java
│   │   │   ├── TagMapper.java
│   │   │   ├── AttachmentMapper.java
│   │   │   ├── ChatSessionMapper.java
│   │   │   ├── ChatMessageMapper.java
│   │   │   ├── EmailCodeMapper.java
│   │   │   └── AiConfigMapper.java
│   │   ├── entity/                # 数据库实体类
│   │   │   ├── User.java                    # 用户实体
│   │   │   ├── Knowledge.java               # 知识条目实体
│   │   │   ├── Category.java                # 分类实体
│   │   │   ├── Tag.java                     # 标签实体
│   │   │   ├── Attachment.java              # 附件实体
│   │   │   ├── ChatSession.java             # 对话会话实体
│   │   │   ├── ChatMessage.java             # 对话消息实体
│   │   │   ├── AiConfig.java                # AI 配置实体
│   │   │   └── EmailCode.java               # 验证码实体
│   │   ├── dto/                   # 数据传输对象
│   │   │   ├── Result.java                  # 统一响应封装
│   │   │   ├── PageRequest.java             # 分页请求
│   │   │   ├── PageResponse.java            # 分页响应
│   │   │   ├── LoginRequest.java            # 登录请求
│   │   │   ├── RegisterRequest.java         # 注册请求
│   │   │   ├── SendCodeRequest.java         # 发送验证码请求
│   │   │   ├── UpdateProfileRequest.java    # 更新资料请求
│   │   │   ├── AuthResponse.java            # 认证响应
│   │   │   ├── KnowledgeDTO.java            # 知识条目 DTO
│   │   │   └── CategoryDTO.java             # 分类 DTO
│   │   └── util/                  # 工具类
│   │       ├── JwtUtil.java                 # JWT Token 工具
│   │       └── FileTextExtractor.java       # 文本提取工具(Apache Tika)
│   ├── src/main/resources/         # 资源文件
│   │   ├── application.yml               # 主配置文件
│   │   ├── es-index-init.json            # ES 索引初始化配置
│   │   └── es-settings.json              # ES 设置
│   ├── sql/                       # SQL 脚本
│   │   └── init_auth.sql                 # 认证模块建表脚本
│   ├── pom.xml                   # Maven 构建配置
│   └── ocr_service.py            # Python OCR 服务（备用）
│
├── frontend/                       # 前端 Vue 3 项目
│   ├── public/                    # 静态资源目录
│   ├── src/                       # 源码目录
│   │   ├── main.js                        # 入口文件
│   │   ├── App.vue                        # 根组件（布局+路由出口）
│   │   ├── router/index.js               # 路由配置
│   │   ├── api/                           # API 接口层
│   │   │   ├── request.js                 # Axios 封装（拦截器）
│   │   │   ├── auth.js                    # 认证 API
│   │   │   ├── user.js                    # 用户 API
│   │   │   ├── chat.js                    # 对话 API
│   │   │   ├── knowledge.js               # 知识库 API
│   │   │   ├── category.js                # 分类 API
│   │   │   ├── tag.js                     # 标签 API
│   │   │   ├── upload.js                  # 上传 API
│   │   │   └── aiConfig.js                # AI 配置 API
│   │   ├── stores/                        # Pinia 状态管理
│   │   │   ├── user.js                    # 用户状态
│   │   │   ├── knowledge.js               # 知识库状态
│   │   │   ├── category.js                # 分类状态
│   │   │   └── aiConfig.js                # AI 配置状态
│   │   ├── composables/                   # 组合式函数
│   │   │   ├── useAvatarProxy.js          # 头像代理 Hook
│   │   │   └── useEmailSuggest.js         # 邮箱建议 Hook
│   │   ├── views/                         # 页面组件
│   │   │   ├── Login.vue                  # 登录页（滑动验证码）
│   │   │   ├── Register.vue               # 注册页（滑动验证码）
│   │   │   ├── HomeChat.vue               # 首页/AI 对话主页
│   │   │   ├── KnowledgeList.vue          # 知识列表页
│   │   │   ├── KnowledgeDetail.vue        # 知识详情页
│   │   │   ├── KnowledgeEdit.vue          # 知识编辑页
│   │   │   ├── CategoryManage.vue         # 分类管理页
│   │   │   └── NotFound.vue               # 404 页面
│   │   └── components/                    # 公共组件
│   │       ├── AiSettings.vue             # AI 设置弹窗
│   │       ├── UserProfile.vue            # 用户资料弹窗
│   │       ├── CategoryTree.vue           # 分类树组件
│   │       ├── MarkdownViewer.vue         # Markdown 渲染组件
│   │       └── SlideCaptcha.vue           # 滑动验证码组件
│   ├── index.html                # HTML 模板
│   ├── package.json              # NPM 依赖配置
│   ├── vite.config.js            # Vite 构建配置
│   └── dist/                     # 构建产物
│
├── docs/                            # 项目文档
│   └── database.sql                # 完整数据库初始化脚本
│
├── data/                            # 运行时数据目录
│   └── uploads/                     # 文件上传目录
│       └── avatars/                 # 用户头像
│
├── chat-images/                     # 聊天图片存储目录
│
├── docker-compose.yml               # Docker Compose（Elasticsearch）
└── README.md                        # 项目文档（本文件）
```

---

## 💻 技术栈详情

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| **Java** | 17 | LTS 版本，使用 Record、Switch 表达式等新特性 |
| **Spring Boot** | 3.2.5 | 基础框架，自动配置 |
| **Spring Web** | 3.2.5 | REST API 开发 |
| **Spring Validation** | 3.2.5 | 参数校验 |
| **MyBatis-Plus** | 3.5.7 | ORM 框架，简化数据库操作 |
| **MySQL Connector** | 最新 | JDBC 驱动 |
| **JWT (JJWT)** | 0.12.5 | JSON Web Token 认证 |
| **Apache Tika** | 2.9.2 | 文件内容提取（PDF/DOCX/XLSX/PPTX/TXT 等） |
| **Elasticsearch Java Client** | 内置 | ES 8.x 客户端 |
| **Spring Data Elasticsearch** | 内置 | ES 数据访问层 |
| **Caffeine** | 内置 | 本地缓存（用于头像代理） |
| **Lombok** | 最新 | 减少样板代码 |

#### Maven 依赖清单 (`pom.xml`)

```xml
<dependencies>
    <!-- Spring Boot 核心 -->
    <dependency>spring-boot-starter-web</dependency>
    <dependency>spring-boot-starter-validation</dependency>
    
    <!-- ORM -->
    <dependency>mybatis-plus-spring-boot3-starter:3.5.7</dependency>
    <dependency>mysql-connector-j</dependency>
    
    <!-- 认证 -->
    <dependency>jjwt-api:0.12.5</dependency>
    <dependency>jjwt-impl:0.12.5</dependency>
    <dependency>jjwt-jackson:0.12.5</dependency>
    
    <!-- 缓存 -->
    <dependency>caffeine</dependency>
    
    <!-- 文件解析 -->
    <dependency>tika-core:2.9.2</dependency>
    <dependency>tika-parsers-standard-package:2.9.2</dependency>
    
    <!-- 搜索引擎 -->
    <dependency>spring-boot-starter-data-elasticsearch</dependency>
    
    <!-- 开发工具 -->
    <dependency>lombok</dependency>
    <dependency>spring-boot-starter-test</dependency>
</dependencies>
```

#### Java 包结构 (`com.kb`)

```
com.kb
├── KnowledgeBaseApplication.java    # 启动类 (@SpringBootApplication + @MapperScan)
├── config/                          # 配置层
├── controller/                      # 控制器层 (REST API)
├── service/                         # 业务逻辑层
├── mapper/                          # 数据访问层 (MyBatis-Plus)
├── entity/                          # 实体类 (数据库映射)
├── dto/                             # 数据传输对象
├── search/                          # 搜索引擎模块 (ES)
└── util/                            # 工具类
```

### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| **Vue.js** | 3.4.21 | 渐进式 JavaScript 框架（Composition API） |
| **Vite** | 5.2.8 | 新一代前端构建工具 |
| **Vue Router** | 4.3.0 | 官方路由管理 |
| **Pinia** | 2.1.7 | 官方状态管理（替代 Vuex） |
| **Element Plus** | 2.6.3 | Vue 3 UI 组件库 |
| **@element-plus/icons-vue** | 2.3.1 | Element Plus 图标库 |
| **Axios** | 1.6.8 | HTTP 客户端 |
| **markdown-it** | 14.1.0 | Markdown 解析与渲染 |
| **highlight.js** | 11.9.0 | 代码高亮 |
| **DOMPurify** | 3.4.4 | XSS 过滤 |
| **Sass** | 1.72.0 | CSS 预处理器 |
| **@vitejs/plugin-vue** | 5.0.4 | Vite Vue 插件 |

#### NPM 依赖清单 (`package.json`)

```json
{
  "dependencies": {
    "@element-plus/icons-vue": "^2.3.1",    // 图标
    "axios": "^1.6.8",                        // HTTP 请求
    "dompurify": "^3.4.4",                    // XSS 防护
    "element-plus": "^2.6.3",                 // UI 组件库
    "highlight.js": "^11.9.0",                // 代码高亮
    "markdown-it": "^14.1.0",                // Markdown 渲染
    "pinia": "^2.1.7",                       // 状态管理
    "vue": "^3.4.21",                        // 核心框架
    "vue-router": "^4.3.0"                   // 路由管理
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.4",          // Vite Vue 插件
    "sass": "^1.72.0",                       // SCSS 预处理
    "vite": "^5.2.8"                         // 构建工具
  }
}
```

#### NPM Scripts

```bash
npm run dev      # 启动开发服务器 (localhost:5173)
npm run build    # 生产环境构建
npm run preview  # 预览生产构建
```

---

## 🚀 快速开始

### 环境要求

- **Java**: JDK 17+
- **Node.js**: 18+
- **MySQL**: 8.0+
- **Elasticsearch**: 8.x（可选，用于搜索功能）

### 1. 克隆项目

```bash
git clone <repository-url>
cd knowledge-base
```

### 2. 启动 Elasticsearch（可选但推荐）

```bash
docker compose up -d
```

首次启动需安装中文分词插件：

```bash
# 进入容器安装 IK 分词器
docker exec -it kb-es bash
./bin/elasticsearch-plugin install https://get.infini.cloud/elasticsearch/analysis-ik/8.15.0.zip
exit
docker restart kb-es
```

### 3. 初始化数据库

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE knowledge_base DEFAULT CHARSET utf8mb4;"

# 导入完整表结构（推荐）
mysql -u root -p knowledge_base < docs/database.sql

# 或仅导入认证相关表（最小化）
mysql -u root -p knowledge_base < backend/sql/init_auth.sql
```

> **注意**：系统启动时会通过 `DatabaseInit.java` 自动检测并补全缺失的表结构，因此即使不手动执行 SQL 也能正常运行。

### 4. 配置后端

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/knowledge_base?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root          # 你的 MySQL 用户名
    password: your_password # 你的 MySQL 密码
  elasticsearch:
    uris: http://localhost:9200  # ES 地址

jwt:
  secret: your-secret-key-change-in-production  # 生产环境请更换

emailjs:
  service-id: your-service-id
  user-id: your-user-id
  template-id: your-template-id
  private-key: your-private-key
```

### 5. 启动后端

```bash
cd backend
mvn spring-boot:run
# 或 IDE 直接运行 KnowledgeBaseApplication.java
```

后端默认运行在 `http://localhost:8080`

### 6. 安装前端依赖并启动

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器运行在 `http://localhost:5173`

### 7. 访问应用

打开浏览器访问 `http://localhost:5173`

---

## ⚙️ 配置说明

### 后端主配置 (`application.yml`)

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | 8080 | 服务端口 |
| `spring.datasource.url` | - | MySQL 连接地址 |
| `spring.datasource.username` | root | 数据库用户名 |
| `spring.datasource.password` | rootroot | 数据库密码 |
| `spring.elasticsearch.uris` | http://localhost:9200 | ES 地址 |
| `jwt.secret` | - | JWT 密钥（生产环境必须修改） |
| `jwt.expiration` | 7200000 | Token 有效期（毫秒），默认 2 小时 |
| `spring.servlet.multipart.max-file-size` | 200MB | 单文件上传大小限制 |
| `app.upload-dir` | ./data/uploads | 文件上传根目录 |
| `search.embedding.enabled` | false | 是否启用向量搜索（收费） |
| `search.embedding.model` | embedding-3 | Embedding 模型名称 |
| `search.hybrid.keyword-weight` | 0.4 | BM25 权重 |
| `search.hybrid.semantic-weight` | 0.6 | kNN 权重 |
| `emailjs.*` | - | EmailJS 邮件服务配置 |

### Vite 配置 (`vite.config.js`)

```javascript
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,                    // 开发服务器端口
    proxy: {                       // API 代理配置
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('error', (err) => { /* 错误处理 */ })
        }
      }
    }
  }
})
```

---

## 🗄️ 数据库设计

### ER 关系图

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────┐
│    user      │     │    knowledge     │     │   category   │
├──────────────┤     ├──────────────────┤     ├──────────────┤
│ id (PK)      │◄┐   │ id (PK)          │     │ id (PK)      │
│ email (UQ)   │  │   │ title           │────►│ category_id  │
│ nickname     │  │   │ content         │     │ name         │
│ password     │  │   │ summary         │     │ parent_id    │
│ avatar       │  │   │ status          │     │ sort_order   │
│ gender       │  │   │ view_count      │     └──────────────┘
│ phone        │  │   │ created_at      │            │
│ created_at   │  └───│ user_id (FK)    │            │
│ updated_at   │     │ updated_at      │     ┌──────┴───────┐
└──────────────┘     └────────┬─────────┘     │   children   │
                              │               └──────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
          ▼                   ▼                   ▼
┌──────────────────┐ ┌──────────────┐ ┌──────────────────┐
│   attachment     │ │knowledge_tag │ │   chat_session    │
├──────────────────┤ ├──────────────┤ ├──────────────────┤
│ id (PK)          │ │ knowledge_id │ │ id (PK)          │
│ knowledge_id(FK) │ │ tag_id       │ │ title            │
│ file_name        │ └──────────────┘ │ user_id          │
│ file_size        │                  │ status           │
│ file_type        │                  │ model            │
│ file_data (BLOB) │                  │ created_at       │
│ file_text        │                  │ updated_at       │
│ created_at       │                  └────────┬─────────┘
└──────────────────┘                           │
                                               │ 1
                                               │
                                        ┌──────▼──────────┐
                                        │  chat_message   │
                                        ├─────────────────┤
                                        │ id (PK)         │
                                        │ session_id (FK) │
                                        │ role            │
                                        │ content         │
                                        │ thinking        │
                                        │ image_urls      │
                                        │ attachment_ids  │
                                        │ tokens_used     │
                                        │ created_at      │
                                        └─────────────────┘

┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│     tag      │  │ email_code   │  │  ai_config   │
├──────────────┤  ├──────────────┤  ├──────────────┤
│ id (PK)      │  │ id (PK)      │  │ id (PK)      │
│ name (UQ)    │  │ email        │  │ user_id (UQ) │
│ created_at   │  │ code         │  │ provider     │
└──────────────┘  │ type         │  │ api_key      │
                  │ expire_time  │  │ model        │
                  │ created_at   │  │ api_url      │
                  └──────────────┘  │ temperature  │
                                    │ max_tokens   │
                                    └──────────────┘
```

### 表结构详解

#### 1. 用户表 (`user`)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 用户ID |
| email | VARCHAR(255) UNIQUE | 邮箱（唯一标识） |
| nickname | VARCHAR(100) | 昵称 |
| password | VARCHAR(255) | 密码（加密存储） |
| avatar | VARCHAR(500) | 头像URL |
| gender | VARCHAR(10) | 性别：男/女/保密 |
| phone | VARCHAR(20) | 手机号 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### 2. 分类表 (`category`)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 分类ID |
| name | VARCHAR(100) NOT NULL | 分类名称 |
| parent_id | BIGINT NULL | 父分类ID（NULL=一级） |
| sort_order | INT DEFAULT 0 | 排序序号 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

**索引**：`idx_parent(parent_id)`, `idx_sort(sort_order)`

#### 3. 标签表 (`tag`)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 标签ID |
| name | VARCHAR(50) UNIQUE | 标签名称 |
| created_at | DATETIME | 创建时间 |

#### 4. 知识条目表 (`knowledge`)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 知识ID |
| title | VARCHAR(200) NOT NULL | 标题 |
| content | LONGTEXT | Markdown 内容 |
| summary | VARCHAR(500) | 摘要 |
| category_id | BIGINT | 分类ID（外键） |
| status | TINYINT DEFAULT 0 | 状态：0草稿 1已发布 |
| view_count | INT DEFAULT 0 | 浏览次数 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

**索引**：
- `idx_category(category_id)`
- `idx_status(status)`
- `idx_created(created_at)`
- **FULLTEXT** `idx_content(title, content)` WITH PARSER ngram

#### 5. 附件表 (`attachment`)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 附件ID |
| knowledge_id | BIGINT | 关联的知识ID |
| file_name | VARCHAR(255) | 原始文件名 |
| file_size | BIGINT | 文件大小（字节） |
| file_type | VARCHAR(200) | MIME 类型 |
| file_data | LONGBLOB | **文件二进制数据** |
| file_text | LONGTEXT | 提取的纯文本内容（供搜索） |
| created_at | DATETIME | 创建时间 |

**支持的文件类型**：PDF、DOC、DOCX、XLS、XLSX、PPT、PPTX、MD、JSON、CSV、XML、YAML/YML、INI/CONF/CFG、LOG、TXT、HTML/HTM、ZIP/RAR/7Z、PNG/JPG/JPEG/GIF/WebP/SVG

**索引**：`idx_knowledge(knowledge_id)`, `idx_file_text(file_text(100))`

#### 6. 知识-标签关联表 (`knowledge_tag`)

| 字段 | 类型 | 说明 |
|------|------|------|
| knowledge_id | BIGINT PK | 知识ID |
| tag_id | BIGINT PK | 标签ID |

**索引**：`idx_tag(tag_id)`

#### 7. 对话会话表 (`chat_session`)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 会话ID |
| title | VARCHAR(200) DEFAULT '新对话' | 会话标题 |
| user_id | BIGINT NULL | 用户ID |
| status | TINYINT DEFAULT 0 | 状态：0正常 1归档 2删除 |
| model | VARCHAR(100) | 使用的AI模型 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 最后更新时间 |

**索引**：`idx_user(user_id)`, `idx_status(status)`, `idx_updated(updated_at)`

#### 8. 对话消息表 (`chat_message`)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 消息ID |
| session_id | BIGINT NOT NULL | 所属会话ID |
| role | ENUM('user','assistant','system') | 消息角色 |
| content | TEXT NOT NULL | 消息内容 |
| thinking | MEDIUMTEXT | 思考/推理内容 |
| image_urls | JSON | 图片URL列表 |
| attachment_ids | JSON | 附件ID列表 |
| tokens_used | INT | 消耗的token数 |
| created_at | DATETIME | 创建时间 |

**索引**：`idx_session(session_id)`, `idx_created(created_at)`, `idx_chat_msg_session_time(session_id, created_at)`

#### 9. AI 配置表 (`ai_config`)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 配置ID |
| user_id | BIGINT UNIQUE | 用户ID（NULL=全局） |
| provider | VARCHAR(50) DEFAULT 'openai' | 服务商 |
| api_key | VARCHAR(500) | API密钥（加密） |
| model | VARCHAR(100) | 模型名称 |
| api_url | VARCHAR(500) | 自定义API地址 |
| temperature | DECIMAL(3,2) DEFAULT 0.70 | 温度参数 |
| max_tokens | INT DEFAULT 2048 | 最大token数 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### 10. 验证码表 (`email_code`)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键ID |
| email | VARCHAR(255) | 邮箱地址 |
| code | VARCHAR(10) | 验证码 |
| type | VARCHAR(20) DEFAULT 'register' | 类型：register/login/reset |
| expire_time | DATETIME | 过期时间 |
| created_at | DATETIME | 创建时间 |

**索引**：`idx_email_type(email, type)`, `idx_expire_time(expire_time)`

---

## 📡 API 接口文档

### 基础信息

- **Base URL**: `/api`
- **认证方式**: `Authorization: Bearer <token>`
- **统一响应格式**: `{ code: 200, message: "success", data: {} }`
- **错误响应**: `{ code: xxx, message: "错误信息", data: null }`

---

### 1. 认证模块 (`/api/auth`)

| 方法 | 路径 | 说明 | 请求体 | 认证 |
|------|------|------|--------|------|
| POST | `/send-code` | 发送邮箱验证码 | `{ email }` | ❌ |
| POST | `/register` | 用户注册 | `{ email, code, password, captchaToken }` | ❌ |
| POST | `/login` | 用户登录 | `{ email, password, captchaToken }` | ❌ |

#### 请求/响应示例

**发送验证码**
```json
// POST /api/auth/send-code
// Request
{ "email": "user@example.com" }

// Response
{ "code": 200, "message": "success", "data": null }
```

**注册**
```json
// POST /api/auth/register
// Request
{ "email": "user@example.com", "code": "123456", "password": "mypassword" }

// Response
{ "code": 200, "message": "success", "data": { "token": "jwt...", "nickname": "" } }
```

**登录**
```json
// POST /api/auth/login
// Request
{ "email": "user@example.com", "password": "mypassword" }

// Response
{ "code": 200, "message": "success", "data": { "token": "jwt...", "nickname": "用户名" } }
```

---

### 2. 用户模块 (`/api/user`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/profile` | 获取当前用户信息 | ✅ |
| PUT | `/profile` | 更新用户资料 | ✅ |
| POST | `/avatar` | 上传头像 (multipart/form-data) | ✅ |
| GET | `/avatar-file/{fileName}` | 获取头像文件 | ❌ |
| GET | `/avatar-proxy?url=` | 头像代理（解决防盗链） | ❌ |

#### 更新资料请求体
```json
{
  "nickname": "新昵称",
  "gender": "男",
  "phone": "13800138000"
}
```

---

### 3. AI 对话模块 (`/api/chat`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/session` | 创建新会话 | ✅ |
| GET | `/sessions?page=&size=&status=` | 获取会话列表（分页） | ✅ |
| GET | `/sessions/search?keyword=&page=&size=` | 搜索会话 | ✅ |
| PUT | `/session/{id}/title` | 更新会话标题 | ✅ |
| DELETE | `/session/{id}` | 删除会话（软删除） | ✅ |
| PUT | `/session/{id}/archive?archive=` | 归档/取消归档 | ✅ |
| GET | `/session/{id}/messages` | 获取会话消息列表 | ✅ |
| POST | `/session/{id}/message/user` | 保存用户消息 | ✅ |
| POST | `/session/{id}/message/assistant` | 保存AI回复消息 | ✅ |
| DELETE | `/message/{msgId}` | 删除指定消息 | ✅ |
| POST | `/session/{id}/send` | 发送消息并获取回复（非流式） | ✅ |
| POST | `/stream` | 流式对话（SSE） | ✅ |
| POST | `/ocr` | OCR 图片文字识别 | ✅ |
| POST | `/migrate-images` | 迁移 base64 图片到文件 | ✅ |

#### 流式对话请求体
```json
{
  "messages": [
    { "role": "system", "content": "You are a helpful assistant." },
    { "role": "user", "content": "Hello!" }
  ],
  "model": "gpt-4o",
  "apiUrl": "https://api.openai.com/v1/chat/completions",
  "temperature": 0.7,
  "maxTokens": 2048
}
```

**响应**：`text/event-stream` (SSE)

#### 保存用户消息请求体
```json
{
  "message": "你好",
  "imageUrls": ["/api/chat/images/xxx.png"],
  "attachmentIds": [1, 2, 3]
}
```

---

### 4. 聊天图片模块 (`/api/chat/images`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/upload` | 上传聊天图片 (multipart/form-data) | ✅ |
| GET | `/{filename}` | 获取已上传的图片 | ❌ |

---

### 5. 知识库模块 (`/api/knowledge`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/?page=&size=&keyword=&categoryId=` | 分页查询知识列表 | ✅ |
| GET | `/{id}` | 获取知识详情 | ✅ |
| POST | `/` | 创建知识条目 | ✅ |
| PUT | `/{id}` | 编辑知识条目 | ✅ |
| DELETE | `/{id}` | 删除知识条目 | ✅ |
| GET | `/search?keyword=&size=` | 轻量搜索（供AI对话用） | ✅ |

#### Knowledge DTO 结构
```json
{
  "id": 1,
  "title": "知识标题",
  "content": "Markdown 内容...",
  "summary": "摘要",
  "categoryId": 1,
  "categoryName": "学习笔记",
  "status": 1,
  "viewCount": 10,
  "tags": ["标签1", "标签2"]
}
```

---

### 6. 分类模块 (`/api/categories`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/tree` | 获取分类树（带层级） | ✅ |
| GET | `/` | 获取所有分类（平铺） | ✅ |
| GET | `/{id}` | 获取单个分类 | ✅ |
| POST | `/` | 新增分类 | ✅ |
| PUT | `/{id}` | 编辑分类 | ✅ |
| DELETE | `/{id}` | 删除分类 | ✅ |

#### Category DTO 结构
```json
{
  "id": 1,
  "name": "学习笔记",
  "parentId": null,
  "sortOrder": 1,
  "children": [
    { "id": 5, "name": "前端", "parentId": 1, "children": [] },
    { "id": 6, "name": "后端", "parentId": 1, "children": [] }
  ]
}
```

---

### 7. 标签模块 (`/api/tags`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/` | 获取所有标签名称 | ✅ |
| DELETE | `/{id}` | 删除标签 | ✅ |

---

### 8. 文件上传模块 (`/api/files`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/upload?autoKnowledge=true` | 上传文件（支持多文件） | ✅ |
| PUT | `/link?knowledgeId=&fileIds=` | 关联文件到知识 | ✅ |
| GET | `/download/{id}` | 下载文件 | ✅ |
| GET | `/preview/{id}` | 在线预览文件 | ✅ |
| GET | `/text/{id}` | 获取文件提取的文本 | ✅ |
| GET | `/info/{id}` | 获取附件元信息 | ✅ |
| POST | `/info` | 批量获取附件元信息 | ✅ |
| POST | `/re-extract-text` | 重新提取所有文件文本 | ✅ |

#### 上传响应示例
```json
{
  "code": 200,
  "data": [
    { "id": 1, "fileName": "document.pdf", "size": 102400, "type": "application/pdf" }
  ]
}
```

#### 附件信息结构
```json
{
  "id": 1,
  "fileName": "document.pdf",
  "fileSize": 102400,
  "fileType": "application/pdf",
  "knowledgeId": 1,
  "previewable": true
}
```

---

### 9. 搜索模块 (`/api/search`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/?keyword=&size=` | 混合搜索（BM25+kNN） | ✅ |
| GET | `/chat?keyword=&size=` | 聊天记录语义搜索 | ✅ |
| POST | `/sync` | 手动触发全量同步（MySQL→ES） | ✅ |

#### 搜索结果结构
```json
[
  {
    "docId": "knowledge_1",
    "type": "knowledge",
    "originalId": 1,
    "title": "文章标题",
    "content": "内容摘要...",
    "summary": "摘要",
    "categoryName": "学习笔记",
    "score": 0.95
  }
]
```

---

### 10. AI 配置模块 (`/api/ai-config`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/` | 获取当前AI配置（API Key解密返回） | ✅ |
| POST | `/` | 保存AI配置（API Key加密存储） | ✅ |

#### AiConfig 结构
```json
{
  "provider": "openai",
  "apiKey": "sk-xxx",
  "model": "gpt-4o",
  "apiUrl": "https://api.openai.com/v1/chat/completions",
  "temperature": 0.7,
  "maxTokens": 4096
}
```

---

## 🎨 前端页面与组件

### 路由配置

| 路径 | 名称 | 组件 | 认证 | 说明 |
|------|------|------|------|------|
| `/login` | Login | `Login.vue` | ❌ | 登录页 |
| `/register` | Register | `Register.vue` | ❌ | 注册页 |
| `/` | Home | `HomeChat.vue` | ✅ | AI 对话首页 |
| `/knowledge` | KnowledgeList | `KnowledgeList.vue` | ✅ | 知识列表页 |
| `/knowledge/:id` | KnowledgeDetail | `KnowledgeDetail.vue` | ✅ | 知识详情页 |
| `/edit/:id` | KnowledgeEdit | `KnowledgeEdit.vue` | ✅ | 知识编辑页 |
| `/categories` | CategoryManage | `CategoryManage.vue` | ✅ | 分类管理页 |
| `/:pathMatch(.*)*` | NotFound | `NotFound.vue` | - | 404 页面 |

### 页面组件详解

#### 1. Login.vue（登录页）- **36KB**
- **功能**：邮箱密码登录 + 滑动验证码安全校验
- **特性**：
  - 滑块验证码组件（自定义实现，非第三方库）
  - 表单验证（邮箱格式、必填项）
  - 登录状态检查（已登录自动跳转首页）
  - 神经网络风格背景动画
  - 响应式布局（移动端适配）
  - 注册入口链接

#### 2. Register.vue（注册页）- **38KB**
- **功能**：邮箱验证码注册 + 密码设置
- **特性**：
  - 滑动验证码安全校验
  - 发送验证码倒计时（60秒冷却）
  - 密码强度提示
  - 注册协议勾选
  - 与登录页共享验证码组件
  - 神经网络风格背景动画（与登录页一致）

#### 3. HomeChat.vue（AI 对话主页）- **101KB** ⭐核心页面
- **功能**：AI 对话交互界面
- **布局**：
  ```
  ┌──────────────────────────────────────────────────┐
  │ 侧边栏 (280px)     │      主对话区域              │
  │ ┌────────────────┐ │  ┌────────────────────────┐  │
  │ │ + 新建对话      │ │  │ 欢迎区域/模型选择栏    │  │
  │ ├────────────────┤ │  ├────────────────────────┤  │
  │ │ 搜索框          │ │  │                        │  │
  │ ├────────────────┤ │  │    消息列表区域         │  │
  │ │ [全部][已归档]  │  │  │    （Markdown 渲染）   │  │
  │ ├────────────────┤ │  │                        │  │
  │ │ 会话列表        │ │  │                        │  │
  │ │  - 对话1       │ │  ├────────────────────────┤  │
  │ │  - 对话2       │ │  │ 输入框区域              │  │
  │ │  ...           │ │  │ [图片][文件] 输入 [发送]│  │
  │ ├────────────────┤ │  └────────────────────────┘  │
  │ │ 用户信息/AI设置│ │                               │
  │ └────────────────┘ │                               │
  └──────────────────────────────────────────────────┘
  ```
- **功能特性**：
  - **会话管理**：新建、切换、重命名、归档、删除
  - **流式对话**：SSE 实时接收 AI 回复，打字机效果
  - **消息渲染**：Markdown 渲染 + 代码高亮 + 数学公式
  - **思维链展示**：展示 AI 推理过程（可折叠）
  - **图片发送**：粘贴/选择/拖拽上传图片
  - **文件上传**：在对话中上传文件供 AI 阅读
  - **OCR 识别**：对图片进行文字识别
  - **消息操作**：复制、删除单条消息
  - **侧边栏折叠**：节省屏幕空间
  - **会话搜索**：关键词搜索历史对话
  - **无限滚动**：分页加载更多会话
  - **模型切换**：前端选择不同 AI 模型和 API 地址

#### 4. KnowledgeList.vue（知识列表页）- **7KB**
- **功能**：知识条目的浏览和管理
- **特性**：
  - 分类筛选（左侧分类树）
  - 关键词搜索
  - 状态筛选（全部/已发布/草稿）
  - 卡片/列表视图切换
  - 分页加载
  - 新建知识入口
  - 批量操作（删除）
  - 空状态展示

#### 5. KnowledgeDetail.vue（知识详情页）- **2.6KB**
- **功能**：查看知识条目详细内容
- **特性**：
  - Markdown 内容渲染（代码高亮）
  - 附件列表展示与下载
  - 标签展示
  - 浏览次数统计
  - 编辑/删除操作按钮
  - 返回列表导航

#### 6. KnowledgeEdit.vue（知识编辑页）- **11KB**
- **功能**：创建/编辑知识条目
- **特性**：
  - Markdown 编辑器（纯文本模式）
  - 标题输入
  - 分类选择（下拉/树形选择）
  - 标签添加（多选）
  - 摘要编辑
  - 状态切换（草稿/发布）
  - 附件上传与管理
  - 实时预览
  - 自动保存（可选）

#### 7. CategoryManage.vue（分类管理页）- **9KB**
- **功能**：知识分类的管理
- **特性**：
  - 树形结构展示
  - 添加顶级/子分类
  - 编辑分类名称
  - 删除分类（含子级检查）
  - 拖拽排序
  - 展开/折叠控制

#### 8. NotFound.vue（404页面）- **0.9KB**
- **功能**：页面未找到提示
- **特性**：
  - 友好的错误提示
  - 返回首页链接

### 公共组件

#### 1. AiSettings.vue（AI 设置弹窗）- **9KB**
- **位置**：侧边栏底部
- **功能**：配置 AI 服务参数
- **配置项**：
  - 服务商选择（OpenAI / Anthropic / Custom）
  - API Key 输入（密码框，加密存储）
  - API 地址（支持自定义）
  - 模型选择（下拉 + 自定义输入）
  - 温度参数（滑块 0~2）
  - 最大 Token 数
  - 保存/测试连接

#### 2. UserProfile.vue（用户资料弹窗）- **10KB**
- **位置**：点击用户头像弹出
- **功能**：编辑个人资料
- **功能项**：
  - 头像上传/更换（裁剪预览）
  - 昵称编辑
  - 性别选择
  - 手机号填写
  - 邮箱显示（只读）
  - 保存/取消

#### 3. CategoryTree.vue（分类树组件）- **2.4KB**
- **位置**：知识库页面左侧边栏
- **功能**：展示分类层级结构
- **特性**：
  - 树形展开/折叠
  - 选中高亮
  - 点击筛选知识列表
  - 显示各分类下的知识数量
  - 支持多级嵌套

#### 4. MarkdownViewer.vue（Markdown 渲染组件）- **3KB**
- **用途**：渲染 Markdown 内容
- **特性**：
  - markdown-it 解析
  - highlight.js 代码高亮
  - DOMPurify XSS 过滤
  - 表格/列表/引用/代码块支持
  - 响应式布局

#### 5. SlideCaptcha.vue（滑动验证码组件）- **13KB**
- **用途**：登录/注册的安全验证
- **特性**：
  - Canvas 绘制拼图
  - 随机生成缺口位置
  - 拖动滑块验证
  - 验证成功/失败动画
  - 刷新重新生成
  - 移动端触摸支持
  - 自定义样式主题

### Pinia 状态管理 Stores

#### 1. user.js（用户状态）
```
State: { token, userInfo(nickname, avatar, email), isLoggedIn }
Actions: login(), logout(), fetchProfile(), updateProfile()
```

#### 2. knowledge.js（知识库状态）
```
State: { knowledgeList, currentKnowledge, total, filters }
Actions: loadList(), getDetail(), create(), update(), delete()
```

#### 3. category.js（分类状态）
```
State: { categoryTree, flatList, currentCategory }
Actions: loadTree(), loadAll(), create(), update(), delete()
```

#### 4. aiConfig.js（AI 配置状态）
```
State: { config(provider, apiKey, model, apiUrl, temperature, maxTokens) }
Actions: loadConfig(), saveConfig()
```

### Composables（组合式函数）

#### 1. useAvatarProxy.js（头像代理 Hook）
- **作用**：处理微信 CDN 防盗链问题
- **用法**：`const proxyAvatarUrl = useAvatarProxy()`
- **原理**：将外部头像 URL 通过后端代理转发

#### 2. useEmailSuggest.js（邮箱建议 Hook）
- **作用**：输入邮箱时自动补全常见域名
- **用法**：`const { suggest } = useEmailSuggest()`
- **支持域名**：gmail.com, qq.com, 163.com, outlook.com, etc.

---

## 🔍 搜索引擎配置

### Elasticsearch 索引结构 (`es-index-init.json`)

```json
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "analyzer": {
        "ik_max_word": { "type": "custom", "tokenizer": "ik_max_word" },
        "ik_smart": { "type": "custom", "tokenizer": "ik_smart" }
      }
    }
  },
  "mappings": {
    "properties": {
      "docId": { "type": "keyword" },
      "type": { "type": "keyword" },
      "originalId": { "type": "long" },
      "knowledgeId": { "type": "long" },
      "title": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "content": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "summary": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "categoryName": { "type": "keyword" },
      "fileName": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "status": { "type": "integer" },
      "model": { "type": "keyword" },
      "role": { "type": "keyword" },
      "thinking": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "createdAt": { "type": "date" },
      "contentEmbedding": {
        "type": "dense_vector",
        "dims": 2048,
        "index": true,
        "similarity": "cosine"
      }
    }
  }
}
```

### 搜索策略

| 模式 | 说明 | 使用场景 |
|------|------|----------|
| **纯 BM25** | 基于 IK 中文分词的全文检索 | 默认模式，免费 |
| **混合检索** | BM25 + kNN 向量检索（RRF 融合） | 启用 embedding 时 |
| **降级机制** | ES 故障时自动降级到 MySQL LIKE | 容灾保障 |

### 搜索权重配置

```yaml
search:
  hybrid:
    keyword-weight: 0.4    # BM25 全文检索权重
    semantic-weight: 0.6   # kNN 语义检索权重
```

---

## 🐳 Docker 部署

### Elasticsearch 容器

```yaml
# docker-compose.yml
services:
  elasticsearch:
    image: elasticsearch:8.15.0
    container_name: kb-es
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - xpack.security.http.ssl.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
      - "9300:9300"
    volumes:
      - es_data:/usr/share/elasticsearch/data
    restart: unless-stopped
```

### 启动命令

```bash
# 启动 Elasticsearch
docker compose up -d

# 查看日志
docker logs kb-es -f

# 停止
docker compose down
```

### 注意事项

1. 首次启动需安装 **IK 中文分词插件**
2. JVM 内存建议不低于 512MB
3. 生产环境建议开启 security 并配置 TLS
4. 如已有 ES 实例，可直接修改 `application.yml` 中的地址

---

## 📝 开发指南

### 构建和运行命令

#### 后端

```bash
cd backend

# 编译
mvn clean compile

# 运行（开发模式）
mvn spring-boot:run

# 打包
mvn clean package -DskipTests

# 运行 JAR
java -jar target/knowledge-base-1.0.0.jar

# 运行测试
mvn test
```

#### 前端

```bash
cd frontend

# 安装依赖
npm install

# 开发模式（热更新）
npm run dev

# 生产构建
npm run build

# 预览构建产物
npm run preview
```

### 目录约定

```
backend/
  pom.xml                    # Maven 配置
  src/main/java/com/kb/      # Java 源码
  src/main/resources/        # 配置/资源
  sql/                       # SQL 脚本

frontend/
  package.json               # NPM 配置
  vite.config.js             # Vite 配置
  src/                       # 源码
  dist/                      # 构建产物（部署目标）
```

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `MYSQL_HOST` | MySQL 主机地址 | localhost |
| `MYSQL_PORT` | MySQL 端口 | 3306 |
| `MYSQL_DB` | 数据库名 | knowledge_base |
| `MYSQL_USER` | MySQL 用户名 | root |
| `MYSQL_PASSWORD` | MySQL 密码 | rootroot |
| `ES_HOST` | Elasticsearch 主机 | localhost |
| `ES_PORT` | Elasticsearch 端口 | 9200 |
| `JWT_SECRET` | JWT 密钥 | (见配置文件) |

### 常见问题

#### Q: 启动报错 "Table doesn't exist"
A: 系统会通过 `DatabaseInit.java` 自动建表，确保 MySQL 中已创建 `knowledge_base` 数据库即可。

#### Q: 搜索功能不可用？
A: 确保 Elasticsearch 已启动且可访问（`http://localhost:9200`）。如未启用 ES，系统会降级到 MySQL LIKE 搜索。

#### Q: 文件上传失败？
A: 检查 MySQL 的 `max_allowed_packet` 配置，建议设置为 200MB 以上。检查磁盘空间。

#### Q: 邮箱验证码收不到？
A: 检查 `application.yml` 中的 EmailJS 配置是否正确，需要在 [EmailJS 官网](https://dashboard.emailjs.com/) 注册账号并获取配置。

#### Q: 如何启用向量搜索？
A: 在 `application.yml` 中设置 `search.embedding.enabled: true`，并确保有有效的智谱 AI API Key（Embedding API 收费）。

---

## 📄 License

MIT License

---

## 👥 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

---

<p align="center">
  Made with ❤️ by CodeBuddy Team
</p>
