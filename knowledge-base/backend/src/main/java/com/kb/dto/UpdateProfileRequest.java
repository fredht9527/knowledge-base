package com.kb.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户资料请求 DTO
 */
@Data
public class UpdateProfileRequest {

    @Size(max = 50, message = "昵称最长50个字符")
    private String nickname;

    private String gender;

    @Size(max = 20, message = "手机号最长20个字符")
    private String phone;
}
