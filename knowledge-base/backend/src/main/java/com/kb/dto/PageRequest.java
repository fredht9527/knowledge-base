package com.kb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页请求参数 - 前端列表查询通用参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
    /** 当前页码（从1开始） */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 10;

    /** 搜索关键字 */
    private String keyword;

    /** 分类筛选 */
    private Long categoryId;

    /** 标签筛选 */
    private Long tagId;

    /** 状态筛选 */
    private Integer status;

    /** 构造函数：页码+每页条数 */
    public PageRequest(Integer page, Integer size) {
        this.page = page;
        this.size = size;
    }
}
