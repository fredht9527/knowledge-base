package com.kb.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 发送验证码请求 DTO
 */
@Data
public class SendCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 验证码类型：register-注册, login-登录, reset-重置密码
     * 默认 register
     */
    @Pattern(regexp = "^(register|login|reset)$", message = "类型只能是 register、login 或 reset")
    private String type = "register";
}
