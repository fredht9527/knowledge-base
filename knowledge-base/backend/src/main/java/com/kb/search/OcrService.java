package com.kb.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * OCR 服务 - 调用 Python RapidOCR 识别图片中的文字
 * 用于不支持图片输入的 AI 模型（如 glm-4.7-flash）
 */
@Slf4j
@Service
public class OcrService {

    private static final String PYTHON_SCRIPT = "ocr_service.py";
    private static final int TIMEOUT_SECONDS = 30;

    /**
     * 对 base64 编码的图片执行 OCR
     *
     * @param base64Data base64 编码的图片数据（可含 data:image/...;base64, 前缀）
     * @return 识别出的文本，失败返回 null
     */
    public String recognizeText(String base64Data) {
        if (base64Data == null || base64Data.isBlank()) {
            return null;
        }

        try {
            // 定位 Python 脚本路径（与 jar 同目录或 classpath）
            String scriptPath = getScriptPath();
            if (scriptPath == null) {
                log.error("OCR 脚本未找到: {}", PYTHON_SCRIPT);
                return null;
            }

            String pythonCmd = "python3";
            // 检查 python3 是否可用
            if (!isCommandAvailable(pythonCmd)) {
                pythonCmd = "python";
                if (!isCommandAvailable(pythonCmd)) {
                    log.error("Python 未安装，OCR 不可用");
                    return null;
                }
            }

            ProcessBuilder pb = new ProcessBuilder(pythonCmd, scriptPath);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 通过 stdin 传入 base64 数据
            var os = process.getOutputStream();
            os.write(base64Data.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("OCR 处理超时");
                return null;
            }

            int exitCode = process.exitValue();
            String result = output.toString().trim();

            if (exitCode != 0 || result.isEmpty()) {
                log.warn("OCR 返回异常: exitCode={}, output={}", exitCode, result);
                return null;
            }

            // 解析 JSON 结果 {"text": "..."} 或 {"error": "..."}
            if (result.contains("\"error\"")) {
                log.warn("OCR 识别失败: {}", result);
                return null;
            }

            // 提取 text 字段值
            String textKey = "\"text\":";
            int idx = result.indexOf(textKey);
            if (idx < 0) {
                return null;
            }
            int start = idx + textKey.length();
            // 去掉引号
            while (start < result.length() && result.charAt(start) == '"') start++;
            int end = result.lastIndexOf('"');
            if (end <= start) return null;
            String text = result.substring(start, end);
            // 处理转义字符
            text = text.replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\");

            log.info("OCR 识别完成, 文本长度: {}", text.length());
            return text.isBlank() ? null : text;

        } catch (Exception e) {
            log.error("OCR 识别异常", e);
            return null;
        }
    }

    private String getScriptPath() {
        // 1. 尝试当前工作目录
        String path = System.getProperty("user.dir") + "/" + PYTHON_SCRIPT;
        if (new java.io.File(path).exists()) return path;

        // 2. 尝试 src/main/resources
        path = System.getProperty("user.dir") + "/src/main/resources/" + PYTHON_SCRIPT;
        if (new java.io.File(path).exists()) return path;

        // 3. 尝试 classpath
        var resource = getClass().getClassLoader().getResource(PYTHON_SCRIPT);
        if (resource != null) return resource.getPath();

        // 4. 尝试 backend 目录下
        path = System.getProperty("user.dir") + "/backend/" + PYTHON_SCRIPT;
        if (new java.io.File(path).exists()) return path;

        return null;
    }

    private boolean isCommandAvailable(String cmd) {
        try {
            Process p = new ProcessBuilder(cmd, "--version")
                    .redirectErrorStream(true)
                    .start();
            p.waitFor(5, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
