package com.wx.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {
    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter = new TokenTextSplitter(400, 100, 5, 10000, true);

    public void processDocuments(MultipartFile[] files) {
        // 1. 利用 parallelStream 开启并行文件解析 (提升 Tika 效率)
        List<Document> allSplitDocs = Arrays.stream(files).parallel().flatMap(file -> {
            try {
                TikaDocumentReader loader = new TikaDocumentReader(new InputStreamResource(file.getInputStream()));
                return splitter.apply(loader.get()).stream()
                    .peek(doc -> doc.getMetadata().put("filename", file.getOriginalFilename()));
            } catch (IOException e) {
                log.error("解析失败: {}", file.getOriginalFilename(), e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        // 2. 严控分批入库：每批 32 条最稳健，防止远程请求超时
        int batchSize = 32;
        for (int i = 0; i < allSplitDocs.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allSplitDocs.size());
            vectorStore.add(allSplitDocs.subList(i, end));
        }
    }
}