package com.kb.controller;

import com.kb.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * [FIX]: 聊天图片上传/访问接口
 * 图片保存到本地文件系统，返回可访问的 URL
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/images")
public class ChatImageController {

    @Value("${chat.image-dir:#{null}}")
    private String imageDir;

    /** 上传聊天图片，返回图片 URL 列表 */
    @PostMapping("/upload")
    public Result<List<String>> uploadImages(@RequestParam("images") MultipartFile[] files) {
        Path dir = getImageDir();
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try {
                // 生成唯一文件名
                String ext = getExtension(file.getOriginalFilename());
                String filename = UUID.randomUUID().toString().replace("-", "") + ext;
                Path target = dir.resolve(filename);
                Files.copy(file.getInputStream(), target);
                String url = "/api/chat/images/" + filename;
                urls.add(url);
                log.debug("聊天图片上传: {}", filename);
            } catch (IOException e) {
                log.error("图片上传失败: {}", e.getMessage());
            }
        }
        return Result.success(urls);
    }

    /** 访问已上传的图片 */
    @GetMapping("/{filename}")
    public org.springframework.http.ResponseEntity<byte[]> getImage(@PathVariable String filename) {
        try {
            Path filePath = getImageDir().resolve(filename);
            if (Files.exists(filePath)) {
                byte[] data = Files.readAllBytes(filePath);
                String ext = getExtension(filename);
                String contentType = switch (ext) {
                    case ".jpg", ".jpeg" -> MediaType.IMAGE_JPEG_VALUE;
                    case ".gif" -> MediaType.IMAGE_GIF_VALUE;
                    case ".webp" -> "image/webp";
                    case ".bmp" -> "image/bmp";
                    default -> MediaType.IMAGE_PNG_VALUE;
                };
                return org.springframework.http.ResponseEntity.ok()
                        .header("Content-Type", contentType)
                        .header("Cache-Control", "public, max-age=86400")
                        .body(data);
            }
        } catch (IOException e) {
            log.error("读取图片失败: {}", e.getMessage());
        }
        return org.springframework.http.ResponseEntity.notFound().build();
    }

    private Path getImageDir() {
        if (imageDir != null && !imageDir.isBlank()) {
            Path dir = Paths.get(imageDir);
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                log.warn("创建图片目录失败: {}, 使用默认目录", imageDir);
            }
            if (Files.isDirectory(dir)) return dir;
        }
        // 默认目录: user.dir/chat-images
        Path dir = Paths.get(System.getProperty("user.dir"), "chat-images");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建图片存储目录: " + dir, e);
        }
        return dir;
    }

    private String getExtension(String filename) {
        if (filename == null) return ".png";
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return ".png";
        String ext = filename.substring(dot).toLowerCase();
        if (!ext.matches("\\.(png|jpg|jpeg|gif|webp|bmp)")) return ".png";
        return ext;
    }
}
