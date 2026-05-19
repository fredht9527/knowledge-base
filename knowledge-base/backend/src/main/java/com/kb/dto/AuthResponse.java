package com.kb.dto;

import com.kb.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应 DTO - 登录成功后返回的用户信息和 Token
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** JWT Token */
    private String token;

    /** 用户ID */
    private Long userId;

    /** 邮箱 */
    private String email;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 性别 */
    private String gender;

    /** 手机号 */
    private String phone;

    public static AuthResponse fromUser(User user, String token) {
        return new AuthResponse(
            token,
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getAvatar(),
            user.getGender(),
            user.getPhone()
        );
    }
}
