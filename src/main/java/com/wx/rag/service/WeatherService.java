/*
 * Copyright (c) 2026 the original author or authors. All rights reserved.
 *
 * @author wangxu
 * @since 2026
 */
package com.wx.rag.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class WeatherService {
    private final ChatClient streamingClient; // 专门用于流式的 Client



    public WeatherService(
        @Qualifier("weatherStreamingChatClient") ChatClient streamingClient) {
        this.streamingClient = streamingClient;
    }

    // 2. 新的流式方法（使用 streamingClient，避开报错的 Advisor）
    public Flux<String> doWorkStream(String city) {
        return this.streamingClient.prompt()
            .user(u -> u.text("你好！请查询 {city} 的天气，并以友好的态度回复用户。").param("city", city)).stream() // 流式请求
            .content();
    }
}
