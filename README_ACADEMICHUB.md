# AcademicHub - AI驱动的学术研究与代码审查平台

> 基于 LangChain4j 的智能学术助手，支持论文搜索入库、RAG问答和GitHub代码审查

[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.6-green)]()
[![LangChain4j](https://img.shields.io/badge/LangChain4j-1.0.0--beta3-blue)]()

## ✨ 核心功能

### 🎓 学术研究助手
- **论文搜索**: 从arXiv数据库搜索学术论文
- **自动入库**: 自动解析论文并向量化存入Pinecone
- **RAG问答**: 基于向量检索的智能学术问答
- **研究推荐**: 推荐热门研究方向和高引作者

### 💻 代码审查系统
- **自动审查**: GitHub Webhook自动触发PR审查
- **深度分析**: 从6大维度分析代码质量
- **专业评论**: 自动提交专业的审查建议

## 🏗️ 技术架构

```
AcademicHub (Java + Spring Boot)
    │
    ├─ AcademicTools (学术工具)
    │   └─ MCPClient → mcp-arxiv (Python) → arXiv API
    │
    ├─ CodeReviewService (代码审查)
    │   └─ MCPClient → mcp-devops (Python) → GitHub API
    │
    └─ RAG Engine
        └─ Pinecone Vector DB + Qwen Embedding
```

## 🚀 快速开始

### 1. 前置条件

- **Java 17+**
- **Maven 3.6+**
- **MongoDB** (存储对话历史)
- **MySQL** (可选，如果需要存储论文元数据)
- **Pinecone账号** (向量数据库)
- **AI模型API Key** (DeepSeek 或 Qwen)

### 2. 配置MCP服务器

#### 2.1 安装arXiv MCP服务器

使用官方的arXiv MCP服务器：

```bash
# 方法1: 使用npx (推荐)
npx -y @modelcontextprotocol/server-arxiv

# 方法2: 全局安装
npm install -g @modelcontextprotocol/server-arxiv
```

#### 2.2 启动arXiv MCP服务器

```bash
# 启动在8002端口
npx @modelcontextprotocol/server-arxiv --port 8002
```

或者创建一个启动脚本 `start-arxiv-mcp.sh`:

```bash
#!/bin/bash
npx -y @modelcontextprotocol/server-arxiv --port 8002
```

#### 2.3 启动DevOps MCP服务器 (代码审查)

```bash
cd mcp-devops
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
python src/server.py --port 8001
```

### 3. 配置环境变量

创建 `.env` 文件或配置环境变量：

```bash
# AI模型配置
export DEEP_SEEK_API_KEY=sk-your-deepseek-key
export LANGCHAIN4J_KEY=sk-your-dashscope-key
export DASH_SCOPE_API_KEY=sk-your-dashscope-key

# Pinecone配置
export PINECONE_API_KEY=your-pinecone-key

# GitHub配置
export GITHUB_TOKEN=ghp_your-github-token

# 数据库配置
export MONGODB_URI=mongodb://localhost:27017/chat_memory_db
export MYSQL_URL=jdbc:mysql://localhost:3306/academichub
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=your-password
```

### 4. 启动应用

```bash
# 构建项目
mvn clean package -DskipTests

# 运行应用
java -jar target/java-ai-langchain4j-1.0-SNAPSHOT.jar

# 或者直接运行
mvn spring-boot:run
```

应用将在 `http://localhost:8080` 启动。

### 5. 访问接口

- **Swagger UI**: http://localhost:8080/doc.html
- **对话接口**: POST http://localhost:8080/xiaozhi/chat
- **GitHub Webhook**: POST http://localhost:8080/webhook/github

## 📖 使用示例

### 示例1: 搜索并入库论文

```
用户: 搜索YOLOv8相关的论文，最多3篇

AI: 🔍 找到3篇相关论文：
1. **YOLOv8: A New State-of-the-Art**
   👤 作者: Glenn Jocher et al.
   ...
   
请告诉我要入库哪篇论文

用户: 入库第1篇论文，标签是目标检测、深度学习、YOLO，缓存ID是search_1234567

AI: ✅ 论文已成功添加到知识库！
   分段数: 15
   现在你可以提问了
```

### 示例2: 学术问答

```
用户: YOLOv8的核心创新是什么？

AI: 根据知识库中的论文内容，YOLOv8的核心创新包括：
1. 改进的Neck结构，使用C2f模块替代C3
2. Anchor-Free的检测头，简化训练流程
3. ...（引用自论文《YOLOv8: A New State-of-the-Art》）
```

### 示例3: 研究推荐

```
用户: 推荐目标检测领域的研究方向

AI: 📊 目标检测领域分析报告
🔥 热门研究方向:
1. Transformer在检测中的应用
2. 轻量化模型设计
...
```

## 🔧 arXiv MCP工具说明

### 可用工具

1. **search_arxiv**: 搜索arXiv论文
   - 参数: `query` (搜索关键词), `max_results` (最大返回数量)
   - 返回: 论文列表（标题、作者、摘要、PDF链接等）

2. **get_paper**: 获取论文详情
   - 参数: `paper_id` (arXiv ID)
   - 返回: 完整的论文信息

### MCP配置文件示例

如果使用MCP配置文件（可选）：

```json
{
  "mcpServers": {
    "arxiv": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-arxiv"],
      "env": {}
    }
  }
}
```

## 📊 项目结构

```
xiaozhi/
├── src/main/java/com/atcsm/java/ai/langchain4j/
│   ├── assistant/
│   │   └── XiaozhiAgent.java          # AI Agent接口
│   ├── tools/
│   │   ├── AcademicTools.java         # 学术工具（新）
│   │   └── CalculatorTools.java
│   ├── service/
│   │   └── CodeReviewService.java     # 代码审查服务
│   ├── client/
│   │   └── MCPClient.java             # MCP客户端
│   ├── config/
│   │   ├── XiaozhiAgentConfig.java    # Agent配置
│   │   └── EmbeddingStoreConfig.java  # 向量库配置
│   └── Controller/
│       ├── XiaozhiController.java     # 对话接口
│       └── GitHubWebhookController.java # Webhook接口
├── src/main/resources/
│   ├── application.properties          # 应用配置
│   └── xiaozhi-prompt-template.txt    # Prompt模板（已更新）
└── pom.xml
```

## 🎯 配置说明

### application.properties 关键配置

```properties
# AI模型配置
langchain4j.open-ai.chat-model.base-url=https://api.deepseek.com
langchain4j.open-ai.chat-model.api-key=${DEEP_SEEK_API_KEY}
langchain4j.open-ai.chat-model.model-name=deepseek-chat

# MCP服务器配置
mcp.server.url=http://localhost:8001      # DevOps MCP
mcp.arxiv.url=http://localhost:8002       # arXiv MCP

# Pinecone配置
pinecone.api-key=${PINECONE_API_KEY}
pinecone.environment=us-east-1-aws
pinecone.index-name=academic-papers

# MongoDB配置
spring.data.mongodb.uri=mongodb://localhost:27017/chat_memory_db
```

## 🐛 常见问题

### 1. arXiv MCP连接失败

**问题**: `❌ 搜索失败: Connection refused`

**解决方案**:
```bash
# 检查arXiv MCP是否运行
netstat -an | grep 8002

# 重启arXiv MCP
npx -y @modelcontextprotocol/server-arxiv --port 8002
```

### 2. Pinecone向量存储失败

**问题**: `Failed to store embeddings in Pinecone`

**解决方案**:
- 检查Pinecone API Key是否正确
- 确认Index名称和维度配置正确
- 查看Pinecone控制台的配额使用情况

### 3. 论文入库慢

**原因**: 向量化和网络传输需要时间

**优化**:
- 使用更快的Embedding模型
- 调整分段大小（`DocumentSplitters.recursive(500, 100)`）
- 考虑批量入库

## 🔐 安全建议

1. **不要提交API Key到代码库**
   - 使用环境变量
   - 添加 `.env` 到 `.gitignore`

2. **GitHub Token权限最小化**
   - 只授予必要的权限（repo读取、PR评论）

3. **MCP服务器安全**
   - 生产环境建议添加认证
   - 使用HTTPS连接

## 📚 参考文档

- [LangChain4j官方文档](https://docs.langchain4j.dev/)
- [arXiv API文档](https://arxiv.org/help/api)
- [MCP协议规范](https://modelcontextprotocol.io/)
- [Pinecone文档](https://docs.pinecone.io/)

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📄 许可证

MIT License

---

**项目作者**: [Your Name]
**联系方式**: [Your Email]
**实验室**: 先进视觉传感器实验室
