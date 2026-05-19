package com.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件实体 - 文件以二进制（BLOB）形式存储在数据库中
 */
@Data
@TableName("attachment")
public class Attachment {

    /** 附件ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的知识条目ID */
    @TableField("knowledge_id")
    private Long knowledgeId;

    /** 原始文件名 */
    @TableField("file_name")
    private String fileName;

    /** 文件大小（字节） */
    @TableField("file_size")
    private Long fileSize = 0L;

    /** 文件 MIME 类型 */
    @TableField("file_type")
    private String fileType = "";

    /** 文件二进制数据（以 LONGBLOB 存储） */
    @TableField("file_data")
    private byte[] fileData;

    /** [FIX]: 从文件中提取的纯文本内容（用于搜索匹配） */
    @TableField("file_text")
    private String fileText;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
