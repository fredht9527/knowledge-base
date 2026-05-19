package com.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体 - 支持邮箱注册登录
 */
@Data
@TableName("user")
public class User {

    /** 用户ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 邮箱（唯一） */
    private String email;

    /** 昵称 */
    private String nickname = "";

    /** 密码（加密存储） */
    private String password;

    /** 头像URL */
    private String avatar;

    /** 性别：男/女/保密 */
    private String gender = "保密";

    /** 手机号 */
    private String phone;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
