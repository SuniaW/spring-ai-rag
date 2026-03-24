/*
 * Copyright (c) 2026 the original author or authors. All rights reserved.
 *
 * @author wangxu
 * @since 2026
 */
package com.wx.rag.controller;

import com.wx.rag.service.WeatherService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
public class AiController {

    @Resource
    private WeatherService weatherService;

    private final OpenAiChatModel chatModel;

    @Autowired
    public AiController(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }


    @GetMapping(value = "/weather/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> weatherStream(@RequestParam(value = "city", defaultValue = "北京") String city) {
        // 简单的后端校验
        if (!city.matches("^[a-zA-Z\\u4e00-\\u9fa5\\s·]+$")) {
            return Flux.just("无效的城市名");
        }
        return weatherService.doWorkStream(city);
    }


    @GetMapping(value = "/generateStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> generateStream(
            @RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return chatModel.stream(new Prompt(new UserMessage(message)));
    }
}


