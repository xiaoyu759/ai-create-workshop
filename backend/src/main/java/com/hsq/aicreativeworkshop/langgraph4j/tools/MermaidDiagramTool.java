package com.hsq.aicreativeworkshop.langgraph4j.tools;

import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.hsq.aicreativeworkshop.exception.BusinessException;
import com.hsq.aicreativeworkshop.exception.ErrorCode;
import com.hsq.aicreativeworkshop.langgraph4j.model.ImageResource;
import com.hsq.aicreativeworkshop.langgraph4j.model.enums.ImageCategoryEnum;
import com.hsq.aicreativeworkshop.manager.CosManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mermaid 架构图生成工具
 */
@Slf4j
@Component
public class MermaidDiagramTool {

    @Resource
    private CosManager cosManager;
    
    @Tool("将 Mermaid 代码转换为架构图图片，用于展示系统结构和技术关系")
    public List<ImageResource> generateMermaidDiagram(@P("Mermaid 图表代码") String mermaidCode,
                                                      @P("架构图描述") String description) {
        if (StrUtil.isBlank(mermaidCode)) {
            return new ArrayList<>();
        }
        try {
            // 转换为SVG图片
            File diagramFile = convertMermaidToSvg(mermaidCode);
            // 上传到COS
            String keyName = String.format("/mermaid/%s/%s",
                    RandomUtil.randomString(5), diagramFile.getName());
            String cosUrl = cosManager.uploadFile(keyName, diagramFile);
            // 清理临时文件
            FileUtil.del(diagramFile);
            if (StrUtil.isNotBlank(cosUrl)) {
                return Collections.singletonList(ImageResource.builder()
                        .category(ImageCategoryEnum.ARCHITECTURE)
                        .description(description)
                        .url(cosUrl)
                        .build());
            }
        } catch (Exception e) {
            log.error("生成架构图失败: {}", e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * 将Mermaid代码转换为SVG图片
     */
    private File convertMermaidToSvg(String mermaidCode) {
        // 创建临时输入文件
        File tempInputFile = FileUtil.createTempFile("mermaid_input_", ".mmd", true);
        FileUtil.writeUtf8String(mermaidCode, tempInputFile);
        // 创建临时输出文件
        File tempOutputFile = FileUtil.createTempFile("mermaid_output_", ".svg", true);
        // 根据操作系统选择命令
        String command = SystemUtil.getOsInfo().isWindows() ? "D:\\Node-js\\node_global\\mmdc.cmd" : "mmdc";

        String browserPath = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
        // 构建命令
        String cmdLine = String.format("\"%s\" -i \"%s\" -o \"%s\" -b transparent",
                command,
                tempInputFile.getAbsolutePath(),
                tempOutputFile.getAbsolutePath()
        );
        // 执行命令
        //RuntimeUtil.execForStr(cmdLine);

        log.info("准备执行 Mermaid 命令: {}", cmdLine);

        try {
            // 3. 使用 ProcessBuilder 替代 Hutool，这样能捕获真实错误
            ProcessBuilder pb = new ProcessBuilder();
            if (SystemUtil.getOsInfo().isWindows()) {
                pb.command("cmd.exe", "/c", cmdLine);
            } else {
                pb.command("sh", "-c", cmdLine);
            }
            pb.environment().put("PUPPETEER_EXECUTABLE_PATH", "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe");
            pb.redirectErrorStream(true); // 将错误输出合并到标准输出中
            Process process = pb.start();

            // 读取输出日志
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("mmdc 输出: {}", line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Mermaid 执行异常，退出码: {}", exitCode);
            }
        } catch (Exception e) {
            log.error("执行 Mermaid 命令时发生异常", e);
        }

        // 检查输出文件
        if (!tempOutputFile.exists() || tempOutputFile.length() == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Mermaid CLI 执行失败");
        }
        // 清理输入文件，保留输出文件供上传使用
        FileUtil.del(tempInputFile);
        return tempOutputFile;
    }
}