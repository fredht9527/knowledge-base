package com.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邮箱验证码实体 - 用于注册、登录、找回密码等场景
 */
@Data
@TableName("email_code")
public class EmailCode {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 邮箱地址 */
    private String email;

    /** 验证码 */
    private String code;

    /** 验证码类型：register-注册, login-登录, reset-重置密码 */
    private String type = "register";

    /** 过期时间 */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
