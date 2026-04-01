

# Enterprise-RAG: 企业级全栈智能问答系统实施指南

![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0--GA-blue) ![Milvus](https://img.shields.io/badge/Milvus-2.6.0-orange) ![Ollama](https://img.shields.io/badge/Ollama-Latest-lightgrey) ![Vue3](https://img.shields.io/badge/Vue-3.4-green) ![Docker](https://img.shields.io/badge/Docker-Ready-blue)

本项目是一个生产就绪的 **RAG（Retrieval-Augmented Generation，检索增强生成）** 全栈解决方案。它不仅涵盖了从文档解析、向量存储到大模型调用的完整链路，还特别针对**低配服务器（如阿里云 2C4G 实例）**进行了深度性能调优，确保在有限资源下实现秒级响应。

## 🚀 核心特性

-   **全栈闭环**：集成 Vue3 前端、Spring Boot 后端、Milvus 向量数据库及 Ollama 本地大模型。
-   **极致优化**：专为 4GB 内存环境设计的“生存法则”，通过 JVM 调优、Swap 机制及组件配额管理，解决 OOM 痛点。
-   **高性能检索**：
    -   支持 **Late Chunking（延迟分块）** 策略，检索精度提升 15%+。
    -   集成 **BGE-Reranker** 重排序模型，确保回答的准确性。
    -   支持 **混合检索（Hybrid Search）**，结合语义向量与关键词过滤。
-   **生产级运维**：
    -   基于 Docker Compose 的一键容器化部署。
    -   完善的 **Prometheus + Grafana** 监控体系。
    -   内置自动化例行维护脚本（内存清理、模型预热、数据备份）。
-   **流畅交互**：原生 **SSE（Server-Sent Events）** 流式响应，支持 Markdown 渲染及引用来源追溯。

## 🏗 系统架构

```mermaid
graph TD
    A[Vue3 前端] -- SSE 流式请求 --> B[Spring Boot 后端]
    B -- 文档解析/分块 --> C[Apache Tika]
    B -- 向量化 --> D[Ollama / BGE-M3]
    B -- 相似度检索 --> E[Milvus 向量库]
    E -- 召回 Top-K --> B
    B -- 提示词组装 --> F[Llama3 / Qwen2.5]
    F -- 生成回答 --> B
    B -- 实时推送 --> A
```

## 🛠 技术栈

| 组件 | 技术选型 | 说明 |
| :--- | :--- | :--- |
| **后端框架** | Spring Boot 3.4.3 | 核心业务逻辑与生态集成 |
| **AI 框架** | Spring AI 1.0.0-GA | 统一 AI 接口，简化 RAG 开发 |
| **向量数据库** | Milvus v2.6.0 | 高性能、存算分离的向量存储 |
| **推理引擎** | Ollama | 本地大模型运行平台 |
| **前端框架** | Vue 3 + Vite | 响应式 UI 与流式渲染 |
| **文档解析** | Apache Tika | 支持 PDF/Word 等多种格式 |
| **监控运维** | Prometheus + Grafana | 系统健康度与性能监控 |

## 📦 快速开始

### 1. 环境准备
-   操作系统：Ubuntu 22.04+ (推荐) 或 Windows WSL2
-   资源配置：最低 2 核 4G (需开启 8G Swap)
-   软件依赖：Docker 20.10+, Docker Compose v2+

### 2. 部署向量数据库 (Milvus)
```bash
mkdir -p /usr/milvus && cd /usr/milvus
wget https://github.com/milvus-io/milvus/releases/download/v2.4.0/milvus-standalone-docker-compose.yml -O docker-compose.yml
docker compose up -d
```

### 3. 配置 Ollama 并拉取模型
```bash
# 安装 Ollama
curl -fsSL https://ollama.com/install.sh | sh

# 拉取推荐模型
ollama pull qwen2.5:1.5b  # 对话模型 (低配推荐)
ollama pull bge-m3        # 向量模型
```

### 4. 后端配置 (application.yml)
请确保 `embedding-dimension` 与所选模型匹配（BGE-M3 为 1024 维）。
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: qwen2.5:1.5b
      embedding:
        model: bge-m3
    vectorstore:
      milvus:
        client:
          host: localhost
          port: 19530
        embedding-dimension: 1024
        index-type: HNSW
```

## 💡 性能调优建议 (2C4G 环境)

根据文档第 25-28 章的实践，建议执行以下优化：
1.  **开启 8G Swap**：防止 Milvus 或 Java 编译时因内存不足崩溃。
2.  **限制 JVM 内存**：使用 `-Xms512m -Xmx600m -XX:MaxMetaspaceSize=256m`。
3.  **模型量化**：在 Ollama 中优先使用 `q4_K_M` 或 `q5_K_M` 量化版本的模型。
4.  **定期脱水**：每日凌晨执行 `sync && echo 3 > /proc/sys/vm/drop_caches` 清理系统缓存。
5.  **并发控制**：设置 `OLLAMA_NUM_PARALLEL=1`，避免多线程推理撑爆内存。

## 📊 监控指标

项目内置了监控面板，重点关注以下指标：
-   **TTFT (首字延迟)**：目标 < 2s
-   **检索延迟**：目标 < 100ms
-   **内存可用率**：需保持在 200MB 以上
-   **I/O Wait**：若长期 > 5%，需检查 Swap 交换频率

## 📄 许可证
本项目遵循 MIT 开源协议。

---

## 🔗 项目资源与开源贡献

*   **在线演示地址**：[http://8.140.221.150/](http://8.140.221.150/)
*   **GitHub 仓库矩阵**：
    *   ⭐ **https://github.com/SuniaW/lite-rag** : 核心后端实现，展示 Spring AI 深度集成能力。
    *   ⭐ **https://github.com/SuniaW/lite-rag-web** : 极简美观的 AI 交互界面。
    *   ⭐ **https://github.com/SuniaW/rag-deploy-scripts** : 沉淀了所有低配环境优化的 Shell 脚本。
    *   ⭐ **https://github.com/SuniaW/study-notes** : 查看全部文档。

---