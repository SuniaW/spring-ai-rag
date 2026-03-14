package com.wx.rag.controller;

import com.wx.rag.service.IngestionService;
import com.wx.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // 允许前端跨域
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;
    private final IngestionService ingestionService;

    // 流式问答接口 (SSE)
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam String query,  @RequestParam(required = false) String chatId) {
        return ragService.streamAnswer(query, chatId);
    }

    // 文档上传接口
    @PostMapping("/upload")
    public String upload(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return "请选择文件";
        }
        ingestionService.processDocuments(files);
        return "成功处理 " + files.length + " 个文件";
    }
}