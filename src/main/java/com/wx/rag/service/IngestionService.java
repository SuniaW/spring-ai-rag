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

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {
    private final VectorStore vectorStore;

    // 1. 提炼分词器参数：400 Token 是 0.5b/1.5b 模型的黄金分割点
    private final TokenTextSplitter splitter = new TokenTextSplitter(400, 100, 5, 10000, true);

    public void processDocuments(MultipartFile[] files) {
        long totalStartTime = System.currentTimeMillis();
        AtomicInteger totalSegments = new AtomicInteger(0);

        // 2. 逐个文件处理代替全量并行解析
        // 理由：Tika 解析非常吃内存。在 4G 环境下，如果并行解析多个大 PDF，容易瞬间触发 OOM 杀掉 Java 进程
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String fileName = file.getOriginalFilename();
            long fileStartTime = System.currentTimeMillis();

            try (InputStream is = file.getInputStream()) { // 3. 使用 try-with-resources 严谨关闭流
                log.info("开始解析文件: {}", fileName);

                TikaDocumentReader loader = new TikaDocumentReader(new InputStreamResource(is));
                List<Document> rawDocs = loader.get();

                // 4. 执行切片
                List<Document> splitDocs = splitter.apply(rawDocs);

                // 5. 注入元数据并准备入库
                splitDocs.forEach(doc -> doc.getMetadata().put("filename", fileName));

                log.info("文件 [{}] 解析完成，耗时: {}ms，产生分片: {}",
                    fileName, (System.currentTimeMillis() - fileStartTime), splitDocs.size());

                // 6. 执行分批入库（核心：控制远程向量化调用的负载）
                processInBatches(splitDocs);

                totalSegments.addAndGet(splitDocs.size());

            } catch (Exception e) {
                log.error("处理文件 [{}] 时发生异常: ", fileName, e);
            }
        }

        log.info(">>> 所有文件入库任务完成！总分片数: {}, 总耗时: {}ms",
            totalSegments.get(), (System.currentTimeMillis() - totalStartTime));
    }

    /**
     * 分批次将文档写入向量数据库
     * 针对远程 Ollama 调优：一批 16-32 条，防止推理时间过长导致连接超时
     */
    private void processInBatches(List<Document> documents) {
        int batchSize = 16; // 4G 内存环境下，16-32 是安全阈值
        int total = documents.size();

        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            List<Document> batch = documents.subList(i, end);

            try {
                // 执行真正的 Embedding + Milvus 写入
                vectorStore.add(batch);
                log.debug("成功写入批次: {}/{}", end, total);
            } catch (Exception e) {
                log.error("批次入库失败 (index: {}): ", i, e);
                // 可以在此处增加重试逻辑
            }
        }
    }
}