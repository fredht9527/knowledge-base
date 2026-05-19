package com.kb.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;

/**
 * [FIX]: 文件文本提取工具 - 从上传文件中提取纯文本内容，供知识库搜索使用
 * 基于 Apache Tika，支持 PDF、DOCX、XLSX、PPTX、TXT、CSV、HTML、MD 等常见格式
 * 不截断，保留完整内容
 */
@Slf4j
public class FileTextExtractor {

    private static final Tika TIKA = new Tika();

    /**
     * 判断是否为文本类型文件（需要保留换行）
     */
    private static boolean isTextFile(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".md") || lower.endsWith(".markdown")
                || lower.endsWith(".txt") || lower.endsWith(".log")
                || lower.endsWith(".csv") || lower.endsWith(".xml")
                || lower.endsWith(".yaml") || lower.endsWith(".yml")
                || lower.endsWith(".ini") || lower.endsWith(".conf")
                || lower.endsWith(".cfg") || lower.endsWith(".json")
                || lower.endsWith(".html") || lower.endsWith(".htm")
                || lower.endsWith(".js") || lower.endsWith(".java")
                || lower.endsWith(".py") || lower.endsWith(".sql");
    }

    /**
     * 从文件二进制数据中提取纯文本内容（完整提取，不截断）
     *
     * @param fileData 文件的二进制数据
     * @param fileName 原始文件名（用于日志记录和格式推断）
     * @return 提取的纯文本内容；如果提取失败返回空字符串
     */
    public static String extractText(byte[] fileData, String fileName) {
        if (fileData == null || fileData.length == 0) {
            return "";
        }
        try {
            String text = TIKA.parseToString(new java.io.ByteArrayInputStream(fileData));
            if (text == null) {
                return "";
            }
            // [FIX]: 文本类型文件保留换行；其他文件清理多余空白
            if (isTextFile(fileName)) {
                // 统一 CRLF → LF，折叠超长空行（最多保留3个）
                text = text.replaceAll("\\r?\\n", "\n");          // CRLF → LF
                text = text.replaceAll("\\n{4,}", "\n\n\n");     // 超过3个换行 → 3个
            } else {
                // PDF/DOCX 等非文本格式：全部空白折叠为空格
                text = text.replaceAll("\\s+", " ").trim();
            }
            log.info("文件文本提取完成: fileName={}, 原始大小={}B, 提取文本长度={}",
                    fileName, fileData.length, text.length());
            return text;
        } catch (Exception e) {
            log.warn("文件文本提取失败: fileName={}, 原因={}", fileName, e.getMessage());
            return "";
        }
    }
}
