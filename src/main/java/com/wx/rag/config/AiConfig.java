/*
 * Copyright (c) 2026 the original author or authors. All rights reserved.
 *
 * @author wangxu
 * @since 2026
 */
package com.wx.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    // 2. 流式天气客户端：去掉自我修正，用于 .stream()
    @Bean
    public ChatClient weatherStreamingChatClient(OpenAiChatModel openAiChatModel) { // 💡 显式注入 OpenAI 模型
        return ChatClient.builder(openAiChatModel).defaultSystem("你是一个专业的气象助手。请直接、简洁地回答天气情况。")
            .defaultTools("weatherFunction") // 💡 关联上面定义的天气函数名
            .build();
    }
}