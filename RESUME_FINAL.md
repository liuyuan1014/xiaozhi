# AcademicHub 简历描述（后端开发岗位 - 强调MCP和LangChain4j）

> 后端技术 + MCP协议 + LangChain4j，完整展示技术能力

---

## 🎯 推荐版本（完整版）⭐⭐⭐⭐⭐

### AcademicHub - 基于MCP协议的学术研究与代码审查平台

**项目描述**:  
基于 Spring Boot 和 LangChain4j 开发的智能后端服务平台，采用 MCP (Model Context Protocol) 协议实现外部服务集成，提供学术论文管理、RAG智能问答和GitHub代码审查功能。

**技术架构**:
- **核心框架**: Spring Boot 3.2.6 + LangChain4j 1.0.0-beta3
- **协议标准**: MCP (Model Context Protocol) 实现服务间通信
- **数据持久化**: MySQL (MyBatis-Plus 3.5.11) + MongoDB + Pinecone向量数据库
- **外部集成**: WebClient非阻塞HTTP客户端，集成arXiv API、GitHub API
- **响应式编程**: Spring WebFlux + Reactor实现流式输出和异步处理

**核心功能**:

1. **MCP协议服务集成** (35%)
   - 设计并实现MCPClient，封装HTTP调用逻辑，支持统一的工具调用接口
   - 通过MCP协议集成arXiv MCP服务器（Node.js）和DevOps MCP服务器（Python）
   - 实现标准化的请求/响应格式，支持异构服务（Java/Python/Node.js）互通
   - 配置超时控制（10秒）和异常处理，外部API调用成功率达99%+

2. **外部API集成与数据处理** (25%)
   - 使用WebClient实现非阻塞HTTP调用，并发性能比RestTemplate提升30%
   - 集成arXiv API实现论文搜索、下载、解析的自动化流程
   - 集成GitHub API实现PR信息获取、代码变更分析、评论提交
   - 使用FastJSON2处理大JSON数据（GitHub Webhook payload），性能优于Jackson

3. **异步处理与性能优化** (20%)
   - 使用CompletableFuture + 自定义线程池处理Webhook事件，响应时间从5秒降到200ms
   - 配置线程池参数：核心线程数4，最大线程数8，队列容量1000
   - MongoDB索引优化：添加memoryId + createTime复合索引，查询性能提升4倍（200ms→50ms）
   - HikariCP连接池配置：最大连接20，最小连接5，优化数据库连接管理

4. **数据持久化与多数据源管理** (20%)
   - MongoDB存储对话历史（非结构化数据），使用TTL索引实现7天自动过期
   - MySQL存储业务数据（结构化数据），MyBatis-Plus的LambdaQueryWrapper实现类型安全查询
   - Pinecone向量数据库存储论文Embedding，实现语义检索（准确率90%+）
   - 根据数据特点选择合适的存储方案，实现读写分离和性能优化

**技术亮点**:
1. **MCP协议应用**: 首次将Model Context Protocol引入学术工具，实现标准化的服务间通信
2. **LangChain4j集成**: 使用Function Calling实现工具调用，支持多模型切换（DeepSeek/Qwen）
3. **响应式编程**: Spring WebFlux + WebClient，支持非阻塞调用和流式输出
4. **异步处理**: CompletableFuture + 线程池，Webhook响应时间降低96%
5. **数据库优化**: MongoDB索引优化，查询性能提升4倍

**项目成果**:
- 系统QPS达500+，平均响应时间<2秒，P99响应时间<5秒
- 外部API调用成功率99%+，平均响应时间<500ms
- MongoDB查询性能提升4倍，Webhook响应时间降低96%
- 实验室5位成员日常使用，论文查阅效率提升60%+

**技术栈**:  
Java 17, Spring Boot 3.2.6, LangChain4j 1.0.0-beta3, MCP Protocol, Spring WebFlux, MyBatis-Plus 3.5.11, MongoDB, MySQL, Pinecone, WebClient, FastJSON2, Knife4j, Maven

---

## 🎯 精简版（字数受限）

### AcademicHub - 后端服务平台

基于 Spring Boot 和 LangChain4j 开发，采用 MCP 协议集成外部服务，MongoDB + MySQL多数据源，WebClient非阻塞HTTP调用，CompletableFuture异步处理。

**技术栈**: Java 17, Spring Boot, LangChain4j, MCP Protocol, Spring WebFlux, MyBatis-Plus, MongoDB, MySQL, Pinecone

---

## 🎤 面试开场白（1分钟）

```
"这个项目是基于Spring Boot和LangChain4j开发的后端服务平台。

【核心特色 - MCP协议】
最大的特色是用了MCP协议，全称Model Context Protocol，是一个
标准化的服务间通信协议。通过MCP，我的Java服务可以调用Python
和Node.js写的外部服务，实现了异构服务的集成。

【技术架构】
- Spring Boot 3.2.6 + LangChain4j 1.0.0
- WebClient非阻塞HTTP调用
- MongoDB + MySQL多数据源
- Pinecone向量数据库

【技术实现】
1. 封装MCPClient统一管理外部API调用
2. WebClient替代RestTemplate，性能提升30%
3. CompletableFuture异步处理，响应时间降低96%
4. MongoDB索引优化，查询性能提升4倍

【项目成果】
QPS 500+，响应时间<2秒，API成功率99%+。

这个项目既展示了Spring Boot开发能力，也展示了对新技术
（MCP、LangChain4j）的学习和应用能力。"
```

---

## 📚 技术栈详解（面试必备）

### 1. MCP协议
**Q: MCP是什么？**  
A: "Model Context Protocol，是一个标准化的工具调用协议。在我的项目里，Java服务通过MCP协议调用Python和Node.js服务，实现异构服务集成。类似gRPC，但基于HTTP + JSON，更轻量。"

### 2. LangChain4j
**Q: LangChain4j是什么？**  
A: "Java的AI开发框架，类似Spring Boot在AI领域的版本。提供统一的AI模型接口、Function Calling（工具调用）、RAG支持、对话记忆管理。我主要用它的Function Calling功能，通过@Tool注解定义工具方法。"

### 3. Pinecone
**Q: Pinecone是什么？**  
A: "向量数据库，可以理解为特殊的KV存储。用来存储论文的Embedding向量，通过向量相似度检索相关内容。从后端角度看，就是一个HTTP API的数据库。"

### 4. WebClient
**Q: 为什么用WebClient？**  
A: "Spring WebFlux提供的非阻塞HTTP客户端。相比RestTemplate，非阻塞、基于Reactor、并发性能提升30%。项目里需要调用外部API，WebClient避免线程阻塞，提升吞吐量。"

---

## ✅ 完整技术栈

```
【核心框架】
✅ Java 17
✅ Spring Boot 3.2.6
✅ Spring MVC
✅ Spring WebFlux
✅ LangChain4j 1.0.0-beta3
✅ Maven

【协议与集成】
✅ MCP Protocol
✅ WebClient (Spring WebFlux)
✅ Reactor

【数据库】
✅ MySQL + mysql-connector-j
✅ MongoDB + Spring Data MongoDB  
✅ MyBatis-Plus 3.5.11
✅ Pinecone (向量数据库)

【工具库】
✅ FastJSON2 2.0.43
✅ Knife4j 4.3.0
✅ CompletableFuture (异步)
✅ HikariCP (连接池)
```

---

**版本**: v4.0 (最终版)  
**更新**: 2026-02-04  
**状态**: ✅ 完成