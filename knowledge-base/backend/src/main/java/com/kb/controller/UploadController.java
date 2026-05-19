package com.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kb.dto.Result;
import com.kb.entity.Attachment;
import com.kb.entity.Category;
import com.kb.entity.Knowledge;
import com.kb.mapper.AttachmentMapper;
import com.kb.mapper.CategoryMapper;
import com.kb.mapper.KnowledgeMapper;
import com.kb.search.SearchService;
import com.kb.util.FileTextExtractor;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文件上传/下载接口 - 文件以二进制（BLOB）存储到 MySQL
 * 上传后自动创建知识条目并按文件类型智能归类
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class UploadController {

    private final AttachmentMapper attachmentMapper;
    private final KnowledgeMapper knowledgeMapper;
    private final CategoryMapper categoryMapper;
    private final SearchService searchService;
    private final DataSource dataSource;

    /** [FIX]: 文件类型 → 分类名称映射 */
    private static final Map<String, String> TYPE_CATEGORY_MAP = Map.ofEntries(
            Map.entry("pdf", "PDF文档"),
            Map.entry("doc", "Word文档"), Map.entry("docx", "Word文档"),
            Map.entry("xls", "Excel表格"), Map.entry("xlsx", "Excel表格"),
            Map.entry("ppt", "PPT演示"), Map.entry("pptx", "PPT演示"),
            Map.entry("md", "Markdown文档"), Map.entry("markdown", "Markdown文档"),
            Map.entry("json", "数据文件"),
            Map.entry("csv", "数据文件"),
            Map.entry("xml", "数据文件"),
            Map.entry("yaml", "配置文件"), Map.entry("yml", "配置文件"),
            Map.entry("ini", "配置文件"), Map.entry("conf", "配置文件"), Map.entry("cfg", "配置文件"),
            Map.entry("log", "日志文件"),
            Map.entry("txt", "文本文件"),
            Map.entry("html", "网页文件"), Map.entry("htm", "网页文件"),
            Map.entry("zip", "压缩文件"), Map.entry("rar", "压缩文件"), Map.entry("7z", "压缩文件"),
            Map.entry("png", "图片文件"), Map.entry("jpg", "图片文件"), Map.entry("jpeg", "图片文件"),
            Map.entry("gif", "图片文件"), Map.entry("webp", "图片文件"), Map.entry("svg", "图片文件")
    );

    /** 上传文件（支持多文件），文件数据存入数据库 LONGBLOB 字段，并自动创建知识条目 */
    @PostMapping("/upload")
    public Result<List<UploadResult>> upload(@RequestParam("files") MultipartFile[] files,
                                              @RequestParam(required = false, defaultValue = "true") boolean autoKnowledge) {
        List<UploadResult> results = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try {
                // 1. 保存附件（二进制 LONGBLOB）
                Attachment a = new Attachment();
                a.setFileName(file.getOriginalFilename());
                a.setFileSize(file.getSize());
                a.setFileType(file.getContentType());
                a.setFileData(file.getBytes());
                a.setFileText(FileTextExtractor.extractText(file.getBytes(), file.getOriginalFilename()));
                attachmentMapper.insert(a);

                // 2. 同步附件到 ES 索引
                searchService.indexAttachment(a.getId(), a.getKnowledgeId(),
                        a.getFileName(), a.getFileText());

                // 3. [FIX]: 自动创建知识条目并归类
                if (autoKnowledge) {
                    autoCreateKnowledge(a);
                }

                results.add(new UploadResult(a.getId(), a.getFileName(), a.getFileSize(), a.getFileType()));
                log.info("文件上传成功: id={}, name={}, size={}B", a.getId(), a.getFileName(), a.getFileSize());
            } catch (IOException e) {
                log.error("文件上传失败: {}", e.getMessage());
            }
        }
        return Result.success(results);
    }

    /**
     * [FIX]: 根据上传文件自动创建知识条目
     * - 按文件扩展名自动归类到对应分类（分类不存在则自动创建）
     * - 知识条目的 content 设为文件提取的文本（用于搜索）
     * - 附件通过 attachment.knowledge_id 关联
     * - 文件原始内容始终以二进制存储在 attachment.file_data
     */
    private void autoCreateKnowledge(Attachment attachment) {
        String fileName = attachment.getFileName();
        if (fileName == null || fileName.isBlank()) return;

        try {
            // 确定分类
            String ext = getFileExtension(fileName).toLowerCase();
            String categoryName = TYPE_CATEGORY_MAP.getOrDefault(ext, "其他文件");
            Long categoryId = ensureCategory(categoryName);

            // 生成摘要（取前200字符）
            String summary = "";
            if (attachment.getFileText() != null && !attachment.getFileText().isBlank()) {
                String text = attachment.getFileText();
                summary = text.length() > 200 ? text.substring(0, 200) + "..." : text;
            }

            // 创建知识条目
            Knowledge k = new Knowledge();
            k.setTitle(fileName);
            // content 存提取的文本（供搜索），原始文件始终以二进制存 attachment
            k.setContent(attachment.getFileText() != null ? attachment.getFileText() : "");
            k.setSummary(summary);
            k.setCategoryId(categoryId);
            k.setStatus(1); // 直接发布
            knowledgeMapper.insert(k);

            // 关联附件到知识条目
            attachment.setKnowledgeId(k.getId());
            attachmentMapper.updateById(attachment);

            // 同步知识条目到 ES
            String catName = null;
            if (categoryId != null) {
                Category cat = categoryMapper.selectById(categoryId);
                if (cat != null) catName = cat.getName();
            }
            searchService.indexKnowledge(k.getId(), k.getTitle(), k.getContent(),
                    k.getSummary(), catName, k.getCategoryId(), k.getStatus(), k.getViewCount(),
                    new java.util.ArrayList<>());

            log.info("自动创建知识条目: knowledgeId={}, title={}, category={}", k.getId(), fileName, categoryName);
        } catch (Exception e) {
            log.warn("自动创建知识条目失败: fileName={}, error={}", fileName, e.getMessage());
        }
    }

    /**
     * [FIX]: 确保分类存在，不存在则自动创建
     */
    private Long ensureCategory(String categoryName) {
        LambdaQueryWrapper<Category> qw = new LambdaQueryWrapper<Category>()
                .eq(Category::getName, categoryName)
                .isNull(Category::getParentId);
        Category existing = categoryMapper.selectOne(qw);
        if (existing != null) return existing.getId();

        Category cat = new Category();
        cat.setName(categoryName);
        cat.setSortOrder(0);
        categoryMapper.insert(cat);
        searchService.indexCategory(cat.getId(), cat.getName(), cat.getParentId());
        log.info("自动创建分类: id={}, name={}", cat.getId(), categoryName);
        return cat.getId();
    }

    /** 获取文件扩展名（不含点号） */
    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "";
        return fileName.substring(dot + 1);
    }

    /** 将已上传的文件关联到知识条目 */
    @PutMapping("/link")
    public Result<Void> linkFiles(@RequestParam Long knowledgeId, @RequestParam String fileIds) {
        String[] ids = fileIds.split(",");
        for (String id : ids) {
            try {
                Long fid = Long.parseLong(id.trim());
                Attachment a = attachmentMapper.selectById(fid);
                if (a != null) {
                    a.setKnowledgeId(knowledgeId);
                    attachmentMapper.updateById(a);
                }
            } catch (NumberFormatException ignore) {}
        }
        log.info("已关联 {} 个附件到知识 {}", ids.length, knowledgeId);
        return Result.success();
    }

    /** 下载文件（根据附件ID从数据库读取） */
    @GetMapping("/download/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        Attachment a = attachmentMapper.selectById(id);
        if (a == null) throw new RuntimeException("文件不存在");
        try {
            response.setContentType(a.getFileType() != null ? a.getFileType() : MediaType.APPLICATION_OCTET_STREAM_VALUE);
            // 中文文件名需编码
            String encodedName = URLEncoder.encode(a.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
            response.setContentLengthLong(a.getFileSize());
            response.getOutputStream().write(a.getFileData());
            response.flushBuffer();
        } catch (IOException e) {
            log.error("文件下载失败: {}", e.getMessage());
        }
    }

    /**
     * [FIX]: 获取附件提取的完整文本内容（不截断）
     * 用于 AI 对话时获取文件的完整文字内容
     */
    @GetMapping("/text/{id}")
    public Result<Map<String, Object>> getText(@PathVariable Long id) {
        Attachment a = attachmentMapper.selectById(id);
        if (a == null) return Result.error("文件不存在");
        String fileText = a.getFileText();
        return Result.success(Map.of(
                "id", a.getId(),
                "fileName", a.getFileName(),
                "text", fileText != null ? fileText : "",
                "length", fileText != null ? fileText.length() : 0
        ));
    }

    /** [FIX]: 重新提取所有文本类型附件的文本内容（修复历史数据换行丢失问题） */
    @PostMapping("/re-extract-text")
    public Result<Map<String, Object>> reExtractText() {
        List<Attachment> all = attachmentMapper.selectList(null);
        int total = 0, updated = 0;
        for (Attachment a : all) {
            if (a.getFileData() == null || a.getFileData().length == 0) continue;
            String newText = FileTextExtractor.extractText(a.getFileData(), a.getFileName());
            if (newText != null && !newText.equals(a.getFileText())) {
                a.setFileText(newText);
                attachmentMapper.updateById(a);
                updated++;
            }
            total++;
        }
        log.info("重新提取文本完成: 共{}条，更新{}条", total, updated);
        return Result.success(Map.of("total", total, "updated", updated));
    }

    /**
     * [FIX]: 在线预览文件（inline 方式，浏览器直接渲染 PDF/图片/文本）
     * 对于不可预览的格式，返回 application/octet-stream 让浏览器下载
     */
    @GetMapping("/preview/{id}")
    public void preview(@PathVariable Long id, HttpServletResponse response) {
        Attachment a = attachmentMapper.selectById(id);
        if (a == null) {
            response.setStatus(404);
            return;
        }
        try {
            // 根据文件扩展名确定是否可 inline 预览
            String contentType = getPreviewContentType(a.getFileName(), a.getFileType());
            response.setContentType(contentType);
            String encodedName = URLEncoder.encode(a.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
            // 可预览的用 inline，不可预览的用 attachment（触发下载）
            boolean canPreview = isPreviewable(a.getFileName());
            response.setHeader("Content-Disposition",
                    (canPreview ? "inline" : "attachment") + "; filename*=UTF-8''" + encodedName);
            response.setContentLengthLong(a.getFileSize());
            response.getOutputStream().write(a.getFileData());
            response.flushBuffer();
        } catch (IOException e) {
            log.error("文件预览失败: {}", e.getMessage());
        }
    }

    /** [FIX]: 获取附件元信息（不含文件内容），用于前端展示文件列表 */
    @GetMapping("/info/{id}")
    public Result<AttachmentInfo> getInfo(@PathVariable Long id) {
        Attachment a = attachmentMapper.selectById(id);
        if (a == null) return Result.error("文件不存在");
        return Result.success(new AttachmentInfo(a.getId(), a.getFileName(), a.getFileSize(),
                a.getFileType(), a.getKnowledgeId(), isPreviewable(a.getFileName())));
    }

    /** [FIX]: 批量获取附件元信息 */
    @PostMapping("/info")
    public Result<List<AttachmentInfo>> getInfos(@RequestBody List<Long> ids) {
        List<AttachmentInfo> list = new ArrayList<>();
        for (Long id : ids) {
            Attachment a = attachmentMapper.selectById(id);
            if (a != null) {
                list.add(new AttachmentInfo(a.getId(), a.getFileName(), a.getFileSize(),
                        a.getFileType(), a.getKnowledgeId(), isPreviewable(a.getFileName())));
            }
        }
        return Result.success(list);
    }

    /** 判断文件是否可在浏览器中直接预览 */
    private boolean isPreviewable(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".pdf") || lower.endsWith(".png") || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".webp")
                || lower.endsWith(".svg") || lower.endsWith(".txt") || lower.endsWith(".json")
                || lower.endsWith(".log") || lower.endsWith(".csv") || lower.endsWith(".xml")
                || lower.endsWith(".html") || lower.endsWith(".htm") || lower.endsWith(".md")
                || lower.endsWith(".yaml") || lower.endsWith(".yml") || lower.endsWith(".ini")
                || lower.endsWith(".conf") || lower.endsWith(".cfg");
    }

    /** 获取预览时的 Content-Type */
    private String getPreviewContentType(String fileName, String defaultType) {
        if (fileName == null) return defaultType != null ? defaultType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".txt") || lower.endsWith(".log") || lower.endsWith(".csv")
                || lower.endsWith(".md") || lower.endsWith(".yaml") || lower.endsWith(".yml")
                || lower.endsWith(".ini") || lower.endsWith(".conf") || lower.endsWith(".cfg"))
            return "text/plain; charset=utf-8";
        if (lower.endsWith(".json")) return "application/json; charset=utf-8";
        if (lower.endsWith(".xml")) return "text/xml; charset=utf-8";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=utf-8";
        return defaultType != null ? defaultType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    /** 附件元信息（不含文件二进制数据） */
    public record AttachmentInfo(Long id, String fileName, long fileSize, String fileType,
                                Long knowledgeId, boolean previewable) {}

    /** 上传结果封装 */
    public record UploadResult(Long id, String fileName, long size, String fileType) {}
}
