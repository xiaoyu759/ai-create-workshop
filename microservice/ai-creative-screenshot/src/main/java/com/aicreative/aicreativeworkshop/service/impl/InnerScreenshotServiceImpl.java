package com.aicreative.aicreativeworkshop.service.impl;

import com.aicreative.aicreativeworkshop.innerservice.InnerScreenshotService;
import com.aicreative.aicreativeworkshop.service.ScreenshotService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class InnerScreenshotServiceImpl implements InnerScreenshotService {

    @Resource
    private ScreenshotService screenshotService;

    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        return screenshotService.generateAndUploadScreenshot(webUrl);
    }
}