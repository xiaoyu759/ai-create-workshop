package com.hsq.aicreativeworkshop.core;

import cn.hutool.json.JSONUtil;
import com.hsq.aicreativeworkshop.ai.AiCodeGeneratorService;
import com.hsq.aicreativeworkshop.ai.AiCodeGeneratorServiceFactory;
import com.hsq.aicreativeworkshop.ai.model.HtmlCodeResult;
import com.hsq.aicreativeworkshop.ai.model.MultiFileCodeResult;
import com.hsq.aicreativeworkshop.ai.model.message.AiResponseMessage;
import com.hsq.aicreativeworkshop.ai.model.message.ToolExecutedMessage;
import com.hsq.aicreativeworkshop.ai.model.message.ToolRequestMessage;
import com.hsq.aicreativeworkshop.constant.AppConstant;
import com.hsq.aicreativeworkshop.core.builder.VueProjectBuilder;
import com.hsq.aicreativeworkshop.core.parser.CodeParserExecutor;
import com.hsq.aicreativeworkshop.core.parser.MultiFileCodeParser;
import com.hsq.aicreativeworkshop.core.saver.CodeFileSaverExecutor;
import com.hsq.aicreativeworkshop.exception.BusinessException;
import com.hsq.aicreativeworkshop.exception.ErrorCode;
import com.hsq.aicreativeworkshop.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;

/**
 * AI 代码生成门面类，组合代码生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        // 根据 appId 获取相应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID
     * @return 保存的目录
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        // 根据 appId 获取相应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @param appId       应用 ID
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        // 执行 Vue 项目构建（同步执行，确保预览时项目已就绪）
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                        vueProjectBuilder.buildProject(projectPath);
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }

    /**
     * 通用流式代码处理方法
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @param appId       应用 ID
     * @return 流式响应
     */
    /**
     * 通用流式代码处理方法
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @param appId       应用 ID
     * @return 流式响应（主链 complete 时文件已保存）
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        // 字符串拼接器，用于当流式返回所有的代码之后，再保存代码
        StringBuilder codeBuilder = new StringBuilder();

        // 主链：透传原始 chunk，同时收集完整代码
        Flux<String> mainStream = codeStream.doOnNext(codeBuilder::append);

        // 主链结束后，追加一段“补全（如需）+ 保存”的收尾逻辑
        return mainStream.concatWith(Flux.defer(() -> {
            String completeCode = codeBuilder.toString();

            // 非多文件模式：直接解析保存
            if (codeGenType != CodeGenTypeEnum.MULTI_FILE) {
                saveCode(completeCode, codeGenType, appId);
                return Flux.empty();
            }

            // 多文件模式：先探一次，缺任意一块就走流式补全
            MultiFileCodeResult probe = new MultiFileCodeParser().parseCode(completeCode);
            boolean incomplete = probe.getHtmlCode() == null || probe.getHtmlCode().isBlank()
                    || probe.getCssCode() == null || probe.getCssCode().isBlank()
                    || probe.getJsCode() == null || probe.getJsCode().isBlank();
            log.info("多文件首次解析：html={}字, css={}字, js={}字",
                    probe.getHtmlCode() == null ? 0 : probe.getHtmlCode().length(),
                    probe.getCssCode() == null ? 0 : probe.getCssCode().length(),
                    probe.getJsCode() == null ? 0 : probe.getJsCode().length());

            if (!incomplete) {
                // 完整：直接保存
                saveCode(completeCode, codeGenType, appId);
                return Flux.empty();
            }

            // 不完整：流式补全一次，合并后保存；补全失败则按首次结果保存
            log.warn("多文件输出不完整，触发一次流式补全");
            return aiCodeGeneratorServiceFactory
                    .getAiCodeGeneratorService(appId, CodeGenTypeEnum.MULTI_FILE)
                    .generateMultiFileCodeRetry(completeCode)
                    .collectList()
                    .map(chunks -> completeCode + "\n" + String.join("", chunks))
                    .doOnNext(merged -> {
                        MultiFileCodeResult after = new MultiFileCodeParser().parseCode(merged);
                        log.info("补全后解析：html={}字, css={}字, js={}字",
                                after.getHtmlCode() == null ? 0 : after.getHtmlCode().length(),
                                after.getCssCode() == null ? 0 : after.getCssCode().length(),
                                after.getJsCode() == null ? 0 : after.getJsCode().length());
                    })
                    .onErrorResume(error -> {
                        log.error("补全调用失败，按首次结果保存: {}", error.getMessage());
                        return Mono.just(completeCode);
                    })
                    .doOnNext(merged -> saveCode(merged, codeGenType, appId))
                    .thenMany(Flux.empty());
        }));
    }

    /**
     * 解析并保存代码
     *
     * @param completeCode 完整代码
     * @param codeGenType  代码生成类型
     * @param appId        应用 ID
     */
    private void saveCode(String completeCode, CodeGenTypeEnum codeGenType, Long appId) {
        try {
            Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
            File saveDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
            log.info("保存成功，目录为：{}", saveDir.getAbsolutePath());
        } catch (Exception e) {
            log.error("保存失败: {}", e.getMessage(), e);
        }
    }

}
//    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
//        // 字符串拼接器，用于当流式返回所有的代码之后，再保存代码
//        StringBuilder codeBuilder = new StringBuilder();
//        return codeStream.doOnNext(chunk -> {
//            // 实时收集代码片段
//            codeBuilder.append(chunk);
//        }).doOnComplete(() -> {
//            try {
//                String completeCode = codeBuilder.toString();
//                // 多文件模式：先探一次，缺 CSS/JS 就补全一次
//                if (codeGenType == CodeGenTypeEnum.MULTI_FILE) {
//                    MultiFileCodeResult probe = new MultiFileCodeParser().parseCode(completeCode);
//                    int htmlLen = probe.getHtmlCode() == null ? 0 : probe.getHtmlCode().length();
//                    int cssLen = probe.getCssCode() == null ? 0 : probe.getCssCode().length();
//                    int jsLen = probe.getJsCode() == null ? 0 : probe.getJsCode().length();
//                    log.info("多文件首次解析：html={}字, css={}字, js={}字", htmlLen, cssLen, jsLen);
//                    if (cssLen == 0 || jsLen == 0) {
//                        log.warn("多文件输出不完整，触发一次补全");
//                        try {
//                            String retryCode = aiCodeGeneratorServiceFactory
//                                    .getAiCodeGeneratorService(appId, codeGenType)
//                                    .generateMultiFileCodeRetry(completeCode);
//                            if (retryCode != null && !retryCode.isBlank()) {
//                                completeCode = completeCode + "\n" + retryCode;
//                                MultiFileCodeResult after = new MultiFileCodeParser().parseCode(completeCode);
//                                log.info("补全后解析：html={}字, css={}字, js={}字",
//                                        after.getHtmlCode() == null ? 0 : after.getHtmlCode().length(),
//                                        after.getCssCode() == null ? 0 : after.getCssCode().length(),
//                                        after.getJsCode() == null ? 0 : after.getJsCode().length());
//                            }
//                        } catch (Exception retryEx) {
//                            log.error("补全调用失败，按首次结果保存: {}", retryEx.getMessage());
//                        }
//                    }
//                }
//                Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
//                File saveDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
//                log.info("保存成功，目录为：{}", saveDir.getAbsolutePath());
//            } catch (Exception e) {
//                log.error("保存失败: {}", e.getMessage(), e);
//            }
//        });
//    }
//.doOnComplete(() -> {
//        // 流式返回完成后，保存代码
//        try {
//String completeCode = codeBuilder.toString();
//// 使用执行器解析代码
//Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
//// 使用执行器保存代码
//File saveDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
//                log.info("保存成功，目录为：{}", saveDir.getAbsolutePath());
//        } catch (Exception e) {
//        log.error("保存失败: {}", e.getMessage());
//        }
//        });