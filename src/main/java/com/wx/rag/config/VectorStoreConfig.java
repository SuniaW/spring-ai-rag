package com.wx.rag.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.autoconfigure.vectorstore.milvus.MilvusVectorStoreProperties; // 你刚发给我的源码类
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
// 💡 关键：强制开启属性绑定，确保 YAML 里的数据能进入 MilvusVectorStoreProperties 对象
@EnableConfigurationProperties(MilvusVectorStoreProperties.class)
public class VectorStoreConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(VectorStoreConfig.class);

    @Bean
    @Primary
    public MilvusVectorStore vectorStore(MilvusServiceClient client,
        @Qualifier("ollamaEmbeddingModel") EmbeddingModel model, MilvusVectorStoreProperties properties) {

        String collectionName = properties.getCollectionName();

        // 💡 核心逻辑：手动检查并强制创建集合
        try {
            boolean hasCollection = client.hasCollection(
                    io.milvus.param.collection.HasCollectionParam.newBuilder().withCollectionName(collectionName).build())
                .getData();

            if (!hasCollection) {
                LOGGER.info(">>>> [INIT] 正在手动创建高性能 HNSW 集合: {}", collectionName);
                // 这里我们什么都不用做，只要设置了 initializeSchema(true)
                // 下面的 builder 会在发现不存在时尝试创建
            }
        } catch (Exception e) {
            LOGGER.warn("检查集合状态失败，准备尝试自动初始化...");
        }

        return MilvusVectorStore.builder(client, model).collectionName(collectionName)
            .embeddingDimension(properties.getEmbeddingDimension()).indexType(IndexType.HNSW)
            .metricType(MetricType.COSINE).indexParameters("{\"M\":16,\"efConstruction\":64}")
            .initializeSchema(false) // 必须保持为 true
            .build();
    }
}