# Xiaozhi Agent 部署指南

本文档详细说明如何在服务器上部署 Xiaozhi Agent (Java)，并连接到已部署的 MCP DevOps Server。

## 1. 前置条件

- **MCP DevOps Server 已部署**：确保 MCP 服务已在端口 8001 运行（参考 `mcp-devops/DEPLOYMENT.md`）。
- **环境准备**：
  - Linux 服务器
  - Docker & Docker Compose
  - JDK 17 & Maven (用于构建)

## 2. 部署步骤

### 2.1 拉取代码

```bash
mkdir -p /opt/xiaozhi
cd /opt/xiaozhi

# 拉取代码 (请替换为你的仓库地址)
git clone https://github.com/yourname/xiaozhi.git .
```

### 2.2 构建项目

```bash
# 使用 Maven 打包
mvn clean package -DskipTests

# 验证构建结果
ls -l target/*.jar
```

### 2.3 创建 Dockerfile

在项目根目录下创建 `Dockerfile`：

```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2.4 创建 Docker Compose 文件

创建 `docker-compose.yml`，包含 Agent 服务及其依赖的数据库：

```yaml
version: '3.8'

services:
  # 1. MongoDB (对话记忆)
  mongodb:
    image: mongo:7
    container_name: xiaozhi-mongodb
    restart: always
    ports:
      - "27017:27017"
    volumes:
      - mongodb_data:/data/db
    networks:
      - xiaozhi-network

  # 2. MySQL (业务数据)
  mysql:
    image: mysql:8
    container_name: xiaozhi-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: guiguxiaozhi
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - xiaozhi-network

  # 3. Xiaozhi Agent
  xiaozhi:
    build: .
    container_name: xiaozhi-agent
    restart: always
    ports:
      - "8080:8080"
    environment:
      # 数据库连接
      SPRING_DATA_MONGODB_URI: mongodb://mongodb:27017/chat_memory_db
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/guiguxiaozhi?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      
      # AI 模型 Key
      DEEP_SEEK_API_KEY: ${DEEP_SEEK_API_KEY}
      LANGCHAIN4J_KEY: ${LANGCHAIN4J_KEY}
      DASH_SCOPE_API_KEY: ${DASH_SCOPE_API_KEY}
      PINECONE_API_KEY: ${PINECONE_API_KEY}
      
      # MCP 连接配置
      # 注意：使用 host.docker.internal 访问宿主机的 8001 端口
      MCP_SERVER_URL: http://host.docker.internal:8001
      GITHUB_TOKEN: ${GITHUB_TOKEN}
    depends_on:
      - mongodb
      - mysql
    extra_hosts:
      - "host.docker.internal:host-gateway"
    networks:
      - xiaozhi-network

volumes:
  mongodb_data:
  mysql_data:

networks:
  xiaozhi-network:
    driver: bridge
```

### 2.5 配置环境变量

创建 `.env` 文件：

```bash
# MySQL 密码
MYSQL_ROOT_PASSWORD=your_mysql_password

# GitHub Token (用于代码审查)
GITHUB_TOKEN=ghp_your_github_token

# AI 模型 Keys
DEEP_SEEK_API_KEY=sk-your_deepseek_key
LANGCHAIN4J_KEY=sk-your_dashscope_key
DASH_SCOPE_API_KEY=sk-your_dashscope_key
PINECONE_API_KEY=your_pinecone_key
```

### 2.6 启动服务

```bash
docker-compose up -d --build
```

## 3. 验证部署

1.  **检查服务状态**: `docker-compose ps`
2.  **查看日志**: `docker-compose logs -f xiaozhi`
3.  **测试 Webhook 接口**:
    ```bash
    curl -X POST http://localhost:8080/webhook/github \
      -H "Content-Type: application/json" \
      -H "X-GitHub-Event: ping" \
      -d '{}'
    ```

## 4. 配置 GitHub Webhook

1.  进入 GitHub 仓库 -> Settings -> Webhooks。
2.  Payload URL: `http://<你的服务器IP>:8080/webhook/github`
3.  Content type: `application/json`
4.  Events: 选择 **Pull requests**。
5.  Active: 勾选。

## 5. 常见问题

- **无法连接 MCP 服务**: 检查 `MCP_SERVER_URL` 是否正确，确保 MCP 服务已在宿主机 8001 端口运行，且防火墙允许访问。
- **数据库连接失败**: 检查 `.env` 中的密码是否与数据库配置一致。
