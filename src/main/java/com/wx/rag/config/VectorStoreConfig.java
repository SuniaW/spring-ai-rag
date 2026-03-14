package com.wx.rag.config;

import io.milvus.client.MilvusServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.autoconfigure.vectorstore.milvus.MilvusVectorStoreProperties; // 你刚发给我的源码类
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
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
    public MilvusVectorStore vectorStore(MilvusServiceClient client, EmbeddingModel model,
        MilvusVectorStoreProperties properties) {

        // 💡 核心：直接从 properties 对象中取值，不再依赖 @Value
        String colName = properties.getCollectionName();
        int dimension = properties.getEmbeddingDimension();

        // 这里的日志会告诉你 Spring 到底读没读到 YAML
        LOGGER.info(">>>>>> [CONFIG_CHECK] 当前绑定的集合名: {}, 维度: {}", colName, dimension);

        return MilvusVectorStore.builder(client, model).collectionName(colName).embeddingDimension(dimension)
            .initializeSchema(true).build();
    }

}