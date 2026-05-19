package com.kb.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

/**
 * [FIX]: Elasticsearch 统一搜索文档 - 涵盖知识条目、附件内容、聊天记录、分类、会话
 * 支持 BM25 全文检索 + kNN 语义向量检索
 * 所有查询走 ES，不再查 MySQL
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "knowledge_search", createIndex = false)
public class SearchDocument {

    /** 唯一ID（type + 原始ID组合，如 "knowledge_1", "attachment_5"） */
    @Id
    private String docId;

    /** 数据类型：knowledge / attachment / chat / category / session */
    @Field(type = FieldType.Keyword)
    private String type;

    /** 原始数据ID */
    @Field(type = FieldType.Long)
    private Long originalId;

    /** 关联的知识条目ID（附件和聊天记录关联到此） */
    @Field(type = FieldType.Long)
    private Long knowledgeId;

    /** 标题 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    /** 内容（知识正文 / 附件提取文本 / 聊天内容） */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    /** 摘要 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String summary;

    /** 分类名称 */
    @Field(type = FieldType.Keyword)
    private String categoryName;

    /** 分类ID */
    @Field(type = FieldType.Long)
    private Long categoryId;

    /** 父分类ID（分类树形结构） */
    @Field(type = FieldType.Long)
    private Long parentId;

    /** 文件名（仅附件类型） */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String fileName;

    /** 状态 */
    @Field(type = FieldType.Integer)
    private Integer status;

    /** 浏览次数 */
    @Field(type = FieldType.Integer)
    private Integer viewCount;

    /** AI 模型（仅会话类型） */
    @Field(type = FieldType.Keyword)
    private String model;

    /** [FIX]: 消息角色（仅聊天类型）：user/assistant/system */
    @Field(type = FieldType.Keyword)
    private String role;

    /** [FIX]: 思考/推理内容（仅聊天类型） */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String thinking;

    /** [FIX]: 图片URL列表（仅用户聊天消息，JSON数组字符串） */
    @Field(type = FieldType.Keyword)
    private String imageUrls;

    /** [FIX]: 附件ID列表（仅用户聊天消息，JSON数组字符串） */
    @Field(type = FieldType.Keyword)
    private String attachmentIds;

    /** 标签列表 */
    @Field(type = FieldType.Keyword)
    private List<String> tags = new java.util.ArrayList<>();

    /** 创建时间（ISO格式，Keyword类型也能正确排序） */
    @Field(type = FieldType.Keyword)
    private String createdAt;

    /** 更新时间（ES索引中为date类型，Java端用String避免反序列化问题） */
    @Field(type = FieldType.Keyword)
    private String updatedAt;

    /** 内容向量（智谱AI embedding-3 2048维 / OpenAI text-embedding-3-small 1536维） */
    @Field(type = FieldType.Dense_Vector, dims = 2048, index = true, similarity = "cosine")
    private float[] contentEmbedding;
}
