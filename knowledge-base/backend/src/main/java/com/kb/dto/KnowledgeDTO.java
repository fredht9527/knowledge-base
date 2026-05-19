package com.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 知识条目数据传输对象 - 用于前后端数据交互和参数校验
 */
@Data
public class KnowledgeDTO {
    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题最长255字符")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    @Size(max = 500, message = "摘要最长500字符")
    private String summary;

    private Long categoryId;
    private String categoryName;
    private Integer status;
    private Integer viewCount;

    /** 标签名称列表 */
    private Set<String> tags;

    /** 上传的文件ID列表（前端传递，用于关联附件到知识） */
    private List<Long> fileIds;

    private String createdAt;
    private String updatedAt;
}
