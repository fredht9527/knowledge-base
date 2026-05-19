package com.kb.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kb.dto.Result;
import com.kb.dto.UpdateProfileRequest;
import com.kb.entity.User;
import com.kb.mapper.UserMapper;
import com.kb.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 用户资料接口 - 获取/更新用户信息、上传头像
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;

    /**
     * 头像本地缓存 — 避免每次请求都穿透到 wx.qlogo.cn
     * - 最大 500 条，24 小时过期，自动淘汰旧条目
     * - 有效避免微信 CDN 的限流 RST
     */
    private final Cache<String, byte[]> avatarCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    @Value("${app.upload-dir:./data/uploads}")
    private String uploadDir;

    /**
     * 获取当前登录用户资料
     * GET /api/user/profile
     */
    @GetMapping("/profile")
    public Result<User> getProfile(@RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null); // 不返回密码
        return Result.success(user);
    }

    /**
     * 更新用户资料
     * PUT /api/user/profile
     */
    @PutMapping("/profile")
    public Result<User> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateProfileRequest req) {
        Long userId = extractUserId(authHeader);
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        if (req.getNickname() != null && !req.getNickname().isBlank()) {
            user.setNickname(req.getNickname());
        }
        if (req.getGender() != null && !req.getGender().isBlank()) {
            user.setGender(req.getGender());
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone());
        }

        userMapper.updateById(user);
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 上传头像
     * POST /api/user/avatar
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file) {
        Long userId = extractUserId(authHeader);

        if (file.isEmpty()) {
            return Result.error("请选择要上传的头像");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("仅支持图片格式（jpg/png/gif/webp）");
        }

        try {
            // 使用绝对路径，避免 Tomcat 临时目录问题
            Path baseDir = Paths.get(System.getProperty("user.dir"), uploadDir);
            Path avatarDir = baseDir.resolve("avatars");
            Files.createDirectories(avatarDir);

            // 文件名：userId.{后缀}
            String ext = getExtension(contentType);
            String fileName = userId + "." + ext;

            // 删除旧头像
            Path avatarPath = avatarDir.resolve(fileName);
            Files.deleteIfExists(avatarPath);

            // 保存新头像
            file.transferTo(avatarPath.toFile());

            // 更新用户记录的 avatar 字段
            String avatarUrl = "/api/user/avatar-file/" + fileName;
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setAvatar(avatarUrl);
                userMapper.updateById(user);
            }

            log.info("头像上传成功: userId={}, url={}", userId, avatarUrl);
            return Result.success(avatarUrl);
        } catch (IOException e) {
            log.error("头像上传失败: userId={}, error={}", userId, e.getMessage());
            return Result.error("头像上传失败，请重试");
        }
    }

    /**
     * 获取头像文件
     * GET /api/user/avatar-file/{fileName}
     */
    @GetMapping("/avatar-file/{fileName}")
    public void getAvatarFile(
            @PathVariable String fileName,
            jakarta.servlet.http.HttpServletResponse response) throws IOException {
        Path baseDir = Paths.get(System.getProperty("user.dir"), uploadDir);
        Path filePath = baseDir.resolve("avatars").resolve(fileName);
        if (!Files.exists(filePath)) {
            response.setStatus(404);
            return;
        }
        // 根据后缀设置 Content-Type
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) {
            response.setContentType(MediaType.IMAGE_PNG_VALUE);
        } else if (lower.endsWith(".gif")) {
            response.setContentType(MediaType.IMAGE_GIF_VALUE);
        } else if (lower.endsWith(".webp")) {
            response.setContentType("image/webp");
        } else {
            response.setContentType(MediaType.IMAGE_JPEG_VALUE);
        }
        Files.copy(filePath, response.getOutputStream());
    }

    /**
     * 头像代理 - 解决 wx.qlogo.cn 等外部 CDN 防盗链导致的 ERR_CONNECTION_RESET
     * 前端将 wx.qlogo.cn URL 拼接到此端点后，由后端转发请求（附带正确请求头）
     * 使用方式: <img :src="'/api/user/avatar-proxy?url=' + encodeURIComponent(avatarUrl)" />
     *
     * [FIX-彻底]: 三层防护
     * 1. 本地 Caffeine 缓存，24h 内仅第一次穿透到 wx.qlogo.cn
     * 2. 设置 Cache-Control: max-age=86400，浏览器强缓存
     * 3. 修复异常处理：写输出流前检查 response.isCommitted()，避免 200+502 双重状态码
     */
    @GetMapping("/avatar-proxy")
    public void avatarProxy(
            @RequestParam("url") String url,
            jakarta.servlet.http.HttpServletResponse response) throws IOException {
        if (url == null || url.isBlank()) {
            response.setStatus(400);
            response.getWriter().write("url parameter is required");
            return;
        }

        // [FIX]: 只允许代理图片类的 URL，防止 SSRF 攻击
        if (!url.startsWith("https://wx.qlogo.cn/") && !url.startsWith("https://thirdwx.qlogo.cn/")) {
            log.warn("头像代理拒绝非白名单URL: {}", url);
            response.setStatus(403);
            response.getWriter().write("proxied url not allowed");
            return;
        }

        try {
            // [FIX]: 从本地缓存获取 — 24h 内只穿透到 wx.qlogo.cn 一次
            byte[] body = avatarCache.get(url, key -> {
                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                headers.set("Referer", "https://wx.qq.com/");
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<byte[]> proxyResp = restTemplate.exchange(
                        key, HttpMethod.GET, entity, byte[].class);
                return proxyResp.getBody();
            });

            if (body == null || body.length == 0) {
                response.setStatus(502);
                response.getWriter().write("failed to fetch avatar");
                return;
            }

            // 设置正确的 Content-Type
            String contentType = guessContentType(url);
            response.setContentType(contentType);

            // [FIX]: 设置浏览器强缓存 24 小时，杜绝重复穿透请求
            response.setHeader("Cache-Control", "public, max-age=86400");
            response.setHeader("Expires", DateTimeFormatter.RFC_1123_DATE_TIME.format(
                    ZonedDateTime.now().plusDays(1)));

            // [FIX]: 写输出流时捕获 IOException，避免 200 已提交后的异常导致 ERR_CONNECTION_RESET
            try {
                response.getOutputStream().write(body);
                response.getOutputStream().flush();
            } catch (IOException e) {
                log.warn("头像代理响应写入失败(浏览器可能已断开): url={}, error={}", url, e.getMessage());
            }
        } catch (Exception e) {
            log.warn("头像代理请求失败: url={}, error={}", url, e.getMessage());
            // [FIX]: 检查 response 是否已提交，避免 200+502 双重状态码导致 ERR_CONNECTION_RESET
            if (!response.isCommitted()) {
                response.setStatus(502);
                response.getWriter().write("proxy fetch failed");
            }
        }
    }

    /** 根据 URL 后缀或参数推断 Content-Type */
    private String guessContentType(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".png") || lower.contains("png")) {
            return MediaType.IMAGE_PNG_VALUE;
        } else if (lower.contains(".gif") || lower.contains("gif")) {
            return MediaType.IMAGE_GIF_VALUE;
        } else if (lower.contains(".webp") || lower.contains("webp")) {
            return "image/webp";
        }
        return MediaType.IMAGE_JPEG_VALUE;
    }

    /** 从 Authorization 头提取用户ID */
    private Long extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未登录或登录已过期");
        }
        String token = authHeader.substring(7);
        return jwtUtil.getUserIdFromToken(token);
    }

    /** 根据 Content-Type 获取文件后缀 */
    private String getExtension(String contentType) {
        if (contentType.contains("png")) return "png";
        if (contentType.contains("gif")) return "gif";
        if (contentType.contains("webp")) return "webp";
        return "jpg";
    }
}
