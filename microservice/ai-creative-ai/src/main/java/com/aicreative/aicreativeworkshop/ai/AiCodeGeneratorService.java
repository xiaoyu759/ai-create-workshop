package com.aicreative.aicreativeworkshop.ai;

import com.aicreative.aicreativeworkshop.ai.model.HtmlCodeResult;
import com.aicreative.aicreativeworkshop.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {

    /**
     * 生成 HTML 代码
     *
     * @param userMessage 用户提示词
     * @return AI 的输出结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    /**
     * 生成多文件代码
     *
     * @param userMessage 用户提示词
     * @return AI 的输出结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);

    /**
     * 生成 HTML 代码
     *
     * @param userMessage 用户提示词
     * @return AI 的输出结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);

    /**
     * 生成多文件代码
     *
     * @param userMessage 用户提示词
     * @return AI 的输出结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);

    /**
     * 多文件输出不完整时，补全缺失的 CSS / JavaScript 代码块（流式）
     *
     * 注意：返回类型必须是 Flux，LangChain4j 才会走 streamingChatModel；
     * 写成 String 会落到阻塞的 chatModel，触发 SpringRestClient 截断问题。
     */
    @SystemMessage("""
            你刚才只输出了 HTML 代码块，缺少 CSS 和 JavaScript 代码块。
            现在只补齐缺失的部分，不要重复输出 HTML：
            1. 输出 ```css 代码块，内容为完整 style.css
            2. 输出 ```javascript 代码块，内容为完整 script.js
            每种只输出 1 个代码块。
            """)
    Flux<String> generateMultiFileCodeRetry(@UserMessage String partialOutput);

    /**
     * 生成 Vue 项目代码（流式）
     *
     * @param userMessage 用户提示词
     * @return AI 的输出结果
     */
    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
    TokenStream generateVueProjectCodeStream(@MemoryId long appId, @UserMessage String userMessage);
}
