package com.wx.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.stream.Collectors;

@Service
@Slf4j
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    // 1. 提炼系统提示词：指令越短，小模型 prefill（预热）速度越快
    private static final String SYSTEM_PROMPT = """
        你是专业架构师。请基于背景资料回答，要求：
        1. Markdown 格式。标题前空行。
        2. 代码使用三反引号并注语言。
        3. 资料未提及则告知不知道。
        """;

    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder
            .defaultSystem(SYSTEM_PROMPT)
            .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
            .build();
    }

    public Flux<String> streamAnswer(String query, String chatId) {
        long startTime = System.currentTimeMillis();

        // 2. 将阻塞的向量检索移至弹性线程池 (boundedElastic)，不占用请求主线程
        return Mono.fromCallable(() -> {
                // 3. 检索调优：topK=2 或 3 是 2核服务器的极限平衡点。
                // 减少召回片段能直接缩短大模型的 CPU 推理时间。
                SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(2)
                    .similarityThreshold(0.5) // 提高阈值至 0.5，过滤杂音，减少上下文长度
                    .build();
                return vectorStore.similaritySearch(searchRequest);
            })
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(docs -> {
                log.info("检索耗时: {}ms", (System.currentTimeMillis() - startTime));

                if (docs.isEmpty()) {
                    return Flux.just("🔍 知识库中未找到相关内容。");
                }

                // 4. 精简上下文拼接，减少 Token 消耗
                String context = docs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n"));

                String references = docs.stream()
                    .map(d -> (String) d.getMetadata().getOrDefault("filename", "未知"))
                    .distinct()
                    .collect(Collectors.joining(", "));

                // 5. 调用流式生成
                return chatClient.prompt()
                    .user(u -> u.text("背景：{context}\n问题：{query}")
                        .param("query", query)
                        .param("context", context))
                    .advisors(a -> a.param(MessageChatMemoryAdvisor.DEFAULT_CHAT_MEMORY_CONVERSATION_ID, chatId))
                    .stream()
                    .content()
                    .concatWith(Flux.just("\n\n---\n> 📚 **参考来源：** " + references))
                    .doOnComplete(() -> log.info("全流程总耗时: {}ms", (System.currentTimeMillis() - startTime)));
            })
            .onErrorResume(e -> {
                log.error("RAG流程异常", e);
                return Flux.just("⚠️ [系统繁忙] 处理请求超时，请稍后再试。");
            });
    }
}