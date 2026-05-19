package com.kb.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kb.dto.AuthResponse;
import com.kb.dto.LoginRequest;
import com.kb.dto.RegisterRequest;
import com.kb.dto.SendCodeRequest;
import com.kb.entity.EmailCode;
import com.kb.entity.User;
import com.kb.mapper.EmailCodeMapper;
import com.kb.mapper.UserMapper;
import com.kb.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 认证业务逻辑 - 注册、登录、发送验证码
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final EmailCodeMapper emailCodeMapper;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;

    // EmailJS 配置（从配置文件读取）
    @Value("${emailjs.service-id:service_kykqj2c}")
    private String emailjsServiceId;

    @Value("${emailjs.user-id:PTEJrtadvvHsIqYjJ}")
    private String emailjsUserId;

    @Value("${emailjs.template-id:template_vt8r9mm}")
    private String emailjsTemplateId;

    @Value("${emailjs.private-key:}")
    private String emailjsPrivateKey;

    /**
     * 发送验证码
     */
    @Transactional
    public void sendCode(SendCodeRequest request) {
        String email = request.getEmail();
        String type = request.getType() != null ? request.getType() : "register";

        // 1. 验证频率：60秒内不能重复发送
        QueryWrapper<EmailCode> query = new QueryWrapper<>();
        query.eq("email", email)
              .eq("type", type)
              .gt("expire_time", LocalDateTime.now())
              .orderByDesc("created_at")
              .last("LIMIT 1");
        EmailCode lastCode = emailCodeMapper.selectOne(query);

        if (lastCode != null && lastCode.getCreatedAt() != null) {
            LocalDateTime sixtySecondsAgo = LocalDateTime.now().minusSeconds(60);
            if (lastCode.getCreatedAt().isAfter(sixtySecondsAgo)) {
                throw new RuntimeException("验证码发送太频繁，请60秒后重试");
            }
        }

        // 2. 删除该邮箱的旧验证码
        QueryWrapper<EmailCode> deleteQuery = new QueryWrapper<>();
        deleteQuery.eq("email", email).eq("type", type);
        emailCodeMapper.delete(deleteQuery);

        // 3. 生成6位验证码
        String code = String.format("%06d", new Random().nextInt(1000000));

        // 4. 保存新验证码到数据库（5分钟过期）
        EmailCode emailCode = new EmailCode();
        emailCode.setEmail(email);
        emailCode.setCode(code);
        emailCode.setType(type);
        emailCode.setExpireTime(LocalDateTime.now().plusMinutes(5));
        emailCode.setCreatedAt(LocalDateTime.now());
        emailCodeMapper.insert(emailCode);

        // 5. 调用 EmailJS 发送邮件
        sendEmailViaEmailJS(email, code, type);
    }

    /**
     * 通过 EmailJS API 发送邮件
     */
    private void sendEmailViaEmailJS(String toEmail, String code, String type) {
        try {
            String apiUrl = "https://api.emailjs.com/api/v1.0/email/send";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构建 EmailJS 请求体
            Map<String, Object> templateParams = Map.of(
                "to_email", toEmail,
                "code", code,
                "type", type
            );

            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("service_id", emailjsServiceId);
            requestBody.put("user_id", emailjsUserId);
            requestBody.put("template_id", emailjsTemplateId);
            requestBody.put("template_params", templateParams);
            if (emailjsPrivateKey != null && !emailjsPrivateKey.isEmpty()) {
                requestBody.put("accessToken", emailjsPrivateKey);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            restTemplate.postForEntity(apiUrl, entity, String.class);

            log.info("验证码邮件发送成功: email={}, type={}", toEmail, type);
        } catch (Exception e) {
            log.error("EmailJS 邮件发送失败: email={}, error={}", toEmail, e.getMessage());
            log.warn("调试模式：验证码为 {}", code);
            throw new RuntimeException("邮件发送失败，请检查邮箱地址是否正确或稍后重试", e);
        }
    }

    /**
     * 用户注册
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail();
        String code = request.getCode();
        String password = request.getPassword();

        // 1. 验证验证码
        validateCode(email, code, "register");

        // 2. 检查邮箱是否已注册
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("email", email);
        User existUser = userMapper.selectOne(query);
        if (existUser != null) {
            throw new RuntimeException("该邮箱已注册，请直接登录");
        }

        // 3. 创建用户（密码加密存储）
        User user = new User();
        user.setEmail(email);
        user.setNickname("用户" + UUID.randomUUID().toString().substring(0, 6));
        user.setPassword(password); // [FIX]: 生产环境应使用 BCrypt 加密
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        // 4. 删除已使用的验证码
        QueryWrapper<EmailCode> deleteQuery = new QueryWrapper<>();
        deleteQuery.eq("email", email).eq("type", "register");
        emailCodeMapper.delete(deleteQuery);

        // 5. 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return AuthResponse.fromUser(user, token);
    }

    /**
     * 用户登录
     */
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail();
        String password = request.getPassword();

        // 1. 查找用户
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("email", email);
        User user = userMapper.selectOne(query);

        if (user == null) {
            throw new RuntimeException("该邮箱尚未注册，请先注册");
        }

        // 2. 验证密码
        if (!password.equals(user.getPassword())) { // [FIX]: 生产环境应使用 BCrypt 验证
            throw new RuntimeException("密码错误，请重试");
        }

        // 3. 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return AuthResponse.fromUser(user, token);
    }

    /**
     * 验证验证码
     */
    private void validateCode(String email, String code, String type) {
        QueryWrapper<EmailCode> query = new QueryWrapper<>();
        query.eq("email", email)
              .eq("code", code)
              .eq("type", type)
              .gt("expire_time", LocalDateTime.now())
              .orderByDesc("created_at")
              .last("LIMIT 1");

        EmailCode emailCode = emailCodeMapper.selectOne(query);
        if (emailCode == null) {
            throw new RuntimeException("验证码错误或已过期");
        }
    }
}
