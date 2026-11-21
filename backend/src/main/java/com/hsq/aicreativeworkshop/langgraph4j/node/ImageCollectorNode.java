package com.hsq.aicreativeworkshop.langgraph4j.node;

import com.hsq.aicreativeworkshop.langgraph4j.ai.ImageCollectionPlanService;
import com.hsq.aicreativeworkshop.langgraph4j.model.ImageCollectionPlan;
import com.hsq.aicreativeworkshop.langgraph4j.model.ImageResource;
import com.hsq.aicreativeworkshop.langgraph4j.state.WorkflowContext;
import com.hsq.aicreativeworkshop.langgraph4j.tools.ImageSearchTool;
import com.hsq.aicreativeworkshop.langgraph4j.tools.LogoGeneratorTool;
import com.hsq.aicreativeworkshop.langgraph4j.tools.MermaidDiagramTool;
import com.hsq.aicreativeworkshop.langgraph4j.tools.UndrawIllustrationTool;
import com.hsq.aicreativeworkshop.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 图片收集节点（并发）
 */
@Slf4j
public class ImageCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            String originalPrompt = context.getOriginalPrompt();
            List<ImageResource> collectedImages = new ArrayList<>();

            try {
                // 第一步：获取图片收集计划
                ImageCollectionPlanService planService = SpringContextUtil.getBean(ImageCollectionPlanService.class);
                ImageCollectionPlan plan = planService.planImageCollection(originalPrompt);
                log.info("获取到图片收集计划，开始并发执行");
                log.info("contentImageTasks 数量: {}", plan.getContentImageTasks() != null ? plan.getContentImageTasks().size() : 0);
                log.info("illustrationTasks 数量: {}", plan.getIllustrationTasks() != null ? plan.getIllustrationTasks().size() : 0);
                log.info("diagramTasks 数量: {}", plan.getDiagramTasks() != null ? plan.getDiagramTasks().size() : 0);
                log.info("logoTasks 数量: {}", plan.getLogoTasks() != null ? plan.getLogoTasks().size() : 0);


                // 第二步：并发执行各种图片收集任务
                List<CompletableFuture<List<ImageResource>>> futures = new ArrayList<>();
                // 并发执行内容图片搜索
                if (plan.getContentImageTasks() != null) {
                    ImageSearchTool imageSearchTool = SpringContextUtil.getBean(ImageSearchTool.class);
                    for (ImageCollectionPlan.ImageSearchTask task : plan.getContentImageTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                imageSearchTool.searchContentImages(task.query())));
                    }
                }
                // 并发执行插画图片搜索
                if (plan.getIllustrationTasks() != null) {
                    UndrawIllustrationTool illustrationTool = SpringContextUtil.getBean(UndrawIllustrationTool.class);
                    for (ImageCollectionPlan.IllustrationTask task : plan.getIllustrationTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                illustrationTool.searchIllustrations(task.query())));
                    }
                }
                // 并发执行架构图生成
                if (plan.getDiagramTasks() != null) {
                    MermaidDiagramTool diagramTool = SpringContextUtil.getBean(MermaidDiagramTool.class);
                    for (ImageCollectionPlan.DiagramTask task : plan.getDiagramTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                diagramTool.generateMermaidDiagram(task.mermaidCode(), task.description())));
                    }
                }
                // 并发执行Logo生成
                if (plan.getLogoTasks() != null) {
                    LogoGeneratorTool logoTool = SpringContextUtil.getBean(LogoGeneratorTool.class);
                    for (ImageCollectionPlan.LogoTask task : plan.getLogoTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                logoTool.generateLogos(task.description())));
                    }
                }

                // 等待所有任务完成并收集结果
                CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0]));
                allTasks.join();
                // 收集所有结果
                for (CompletableFuture<List<ImageResource>> future : futures) {
                    List<ImageResource> images = future.get();
                    if (images != null) {
                        collectedImages.addAll(images);
                    }
                }
                log.info("并发图片收集完成，共收集到 {} 张图片", collectedImages.size());
            } catch (Exception e) {
                log.error("图片收集失败: {}", e.getMessage(), e);
            }
            // 更新状态
            context.setCurrentStep("图片收集");
            context.setImageList(collectedImages);
            return WorkflowContext.saveContext(context);
        });
    }
}
