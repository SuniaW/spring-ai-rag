/*
 * Copyright (c) 2026 the original author or authors. All rights reserved.
 *
 * @author wangxu
 * @since 2026
 */
package com.wx.rag.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * @author wangxu
 * @version 1.0
 * @date 2026/2/12
 * @description 天气工具类
 */
@Component
public class WeatherTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeatherTool.class);

    final int[] temperatures = {-125, 15, -255};
    private final Random random = new Random();

    /**
     * 获取天气信息
     * 
     * @param location 位置
     * @return 答案
     */
    @Tool(description = "Get the current weather for a given location")
    public String weather(String location) {
        LOGGER.info("WeatherTool called with location: {}", location);
        int temperature = temperatures[random.nextInt(temperatures.length)];
        System.out.println(">>> Tool Call responseTemp: " + temperature);
        return "The current weather in " + location + " is sunny with a temperature of " + temperature + "°C.";
    }

}