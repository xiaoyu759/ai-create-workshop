package com.hsq.aicreativeworkshop.core.parser;

import com.hsq.aicreativeworkshop.ai.model.HtmlCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML 单文件代码解析器
 */
public class HtmlCodeParser implements CodeParser<HtmlCodeResult> {

    /**
     * 支持未闭合的代码块（AI 输出被 max-tokens 截断时）
     */
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile(
            "```html\\s*\\n?([\\s\\S]*?)(?:```|$)", Pattern.CASE_INSENSITIVE);

    @Override
    public HtmlCodeResult parseCode(String codeContent) {
        HtmlCodeResult result = new HtmlCodeResult();
        // 提取 HTML 代码
        String htmlCode = extractHtmlCode(codeContent);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        } else {
            // 如果没有找到代码块，尝试从 DOCTYPE 或 <html 标签开始提取
            String doctypeCode = extractFromDoctype(codeContent);
            if (doctypeCode != null && !doctypeCode.trim().isEmpty()) {
                result.setHtmlCode(doctypeCode.trim());
            } else {
                // 最后兜底：将整个内容作为HTML
                result.setHtmlCode(codeContent.trim());
            }
        }
        return result;
    }

    /**
     * 提取 HTML 代码内容
     *
     * @param content 原始内容
     * @return HTML代码
     */
    private String extractHtmlCode(String content) {
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 从 DOCTYPE 或 html 标签开始提取代码（用于处理未闭合代码块的情况）
     *
     * @param content 原始内容
     * @return HTML代码
     */
    private String extractFromDoctype(String content) {
        // 尝试从 <!DOCTYPE html> 开始
        int doctypeIndex = content.indexOf("<!DOCTYPE html>");
        if (doctypeIndex != -1) {
            return content.substring(doctypeIndex);
        }
        // 尝试从 <html 开始（不区分大小写）
        Pattern htmlTagPattern = Pattern.compile("<html[\\s>]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = htmlTagPattern.matcher(content);
        if (matcher.find()) {
            return content.substring(matcher.start());
        }
        return null;
    }
}