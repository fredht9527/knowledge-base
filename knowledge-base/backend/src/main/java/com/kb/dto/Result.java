package com.kb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应格式 - 所有接口返回此结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    /** 状态码：200-成功，其他-失败 */
    private Integer code;

    /** 提示消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 成功响应（带数据） */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /** 成功响应（无数据） */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /** 失败响应 */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    /** 自定义错误码失败响应 */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
