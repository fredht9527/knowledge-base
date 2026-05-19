package com.kb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应对象 - 封装分页查询结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    /** 当前页数据 */
    private List<T> content;

    /** 总记录数 */
    private Long total;

    /** 当前页码 */
    private Integer page;

    /** 每页条数 */
    private Integer size;

    /** 总页数 */
    private Integer totalPages;

    /** 工厂方法：根据查询结果构建分页响应（兼容 MyBatis-Plus IPage 的 long 类型） */
    public static <T> PageResponse<T> of(List<T> content, Long total, Integer page, Integer size) {
        PageResponse<T> response = new PageResponse<>();
        response.setContent(content);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        response.setTotalPages(size != null && size > 0 ? (int) Math.ceil((double) total / size) : 0);
        return response;
    }

    /** 从 MyBatis-Plus IPage 构建 */
    public static <T> PageResponse<T> from(com.baomidou.mybatisplus.core.metadata.IPage<?> mpPage, List<T> content) {
        return of(content, mpPage.getTotal(), (int) mpPage.getCurrent(), (int) mpPage.getSize());
    }
}
