# ATRL Chat Backend

一个基于 Spring Boot 3 的 AI 聊天应用后端，支持用户端、管理端、角色广场、私聊、群聊、文件上传、Token 统计，以及基于 LangChain4j 的记忆与 RAG 能力。

前端仓库地址：[chat-app-front](https://gitee.com/wan-yixiao-xiao/chat-app-front)

## 功能概览

- 用户注册、登录、资料维护、头像上传
- AI 角色创建、编辑、公开/私有切换、搜索、广场展示
- 用户与 AI 角色的多轮对话和历史会话查询
- 群聊创建、加群、退群、成员查询、历史消息查询
- 角色关注、关注榜单、关注数量统计
- 管理员登录、注册、用户分页管理、用户状态封禁
- Prometheus 指标暴露与 Token 用量统计
- Redis + PgVector 支持的记忆和 RAG 检索能力
- Netty WebSocket 群聊通道

## 技术栈

- Java 17
- Spring Boot 3.5.5
- MyBatis
- MySQL
- Redis
- PostgreSQL + pgvector
- LangChain4j
- Netty WebSocket
- Micrometer + Prometheus
- 阿里云 OSS
- JWT

## 项目结构

```text
src/main/java/com/yuntian/chat_app
├─ controller          REST 接口
├─ service             业务逻辑
├─ mapper              MyBatis Mapper
├─ entity / dto / vo   数据对象
├─ config              Spring 与模型配置
├─ netty               WebSocket 服务
├─ repository          记忆存储实现
└─ utils               工具类

src/main/resources
├─ application.yml     应用配置
├─ mapper              MyBatis XML
├─ database            数据库脚本
└─ static              静态调试页面与资源
```

## 运行要求

启动前请准备以下环境：

- JDK 17+
- Maven 3.9+，或者直接使用仓库自带的 `mvnw` / `mvnw.cmd`
- MySQL 8.x
- Redis 6.x+
- PostgreSQL 14+，并安装 `pgvector` 扩展
- 可用的 DashScope 兼容 OpenAI 接口 Key
- 可选：阿里云 OSS 账号，用于头像和文件上传

## 快速启动

### 1. 初始化数据库

MySQL 初始化脚本位于：

- `src/main/resources/database/database.sql`

RAG 相关向量库默认使用 PostgreSQL，配置项在 `application.yml` 的 `rag.postgres.*` 下。

### 2. 配置应用参数

当前仓库只包含一个 `src/main/resources/application.yml`，其中大量配置通过占位符读取。你可以通过环境变量、JVM 参数，或补充本地 profile 文件来提供这些值。

建议至少配置以下项目：

| 配置键 | 说明 |
| --- | --- |
| `spring.datasource.driver-class-name` | MySQL 驱动类名，通常为 `com.mysql.cj.jdbc.Driver` |
| `spring.datasource.url` | MySQL 连接串 |
| `spring.datasource.username` | MySQL 用户名 |
| `spring.datasource.password` | MySQL 密码 |
| `spring.data.redis.host` | Redis 主机 |
| `spring.data.redis.port` | Redis 端口 |
| `spring.data.redis.database` | Redis DB |
| `spring.data.redis.connect-timeout` | Redis 连接超时 |
| `spring.data.redis.timeout` | Redis 读写超时 |
| `API-KEY` | DashScope 兼容 OpenAI 接口 Key |
| `chatapp.jwt.user-secret-key` | 用户端 JWT 密钥 |
| `chatapp.jwt.user-ttl` | 用户端 JWT 过期时间 |
| `chatapp.jwt.user-token-name` | 用户端请求头名称 |
| `chatapp.jwt.admin-secret-key` | 管理端 JWT 密钥 |
| `chatapp.jwt.admin-ttl` | 管理端 JWT 过期时间 |
| `chatapp.jwt.admin-token-name` | 管理端请求头名称 |
| `spring.alioss.endpoint` | OSS Endpoint |
| `spring.alioss.access-key-id` | OSS AccessKeyId |
| `spring.alioss.access-key-secret` | OSS AccessKeySecret |
| `spring.alioss.bucket-name` | OSS Bucket 名称 |

默认端口：

- HTTP: `8085`
- WebSocket: `8090`
- WebSocket 路径: `/ws/group`

### 3. 启动项目

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS / Linux:

```bash
./mvnw spring-boot:run
```

打包命令：

```bash
./mvnw clean package
```

## 主要接口

用户端：

- `POST /user/login`
- `POST /user/register`
- `GET /user/user/UserInfo`
- `POST /user/update`

角色相关：

- `POST /character/add`
- `POST /character/update`
- `GET /character/search`
- `GET /character/square`
- `GET /character/{id}`

AI 对话：

- `POST /ai/chat`
- `GET /ai/chat/history/session/{sessionId}`
- `GET /ai/chat/sessions/{userId}/{characterId}`
- `POST /ai/chat/new/{characterId}`

群聊：

- `POST /api/group/create`
- `POST /api/group/join`
- `POST /api/group/leave`
- `GET /api/group/{groupId}/members`
- `GET /api/group/{groupId}/messages`

管理端：

- `POST /admin/login`
- `POST /admin/register`
- `GET /admin/dashboard/stats`
- `GET /admin/user/page`
- `POST /admin/user/status/{status}`

监控：

- `GET /actuator/health`
- `GET /actuator/prometheus`
- `GET /metrics/tokens/{memoryId}`
- `GET /metrics/tokens/daily`

更完整的接口说明见仓库根目录：

- `接口文档.md`

## AI 与记忆能力说明

项目当前接入了两类模型能力：

- 主对话模型：`langchain4j.open-ai.chat-model.*`
- 记忆写入模型：`memory-writer.*`

其中：

- 主对话模型默认使用 DashScope 兼容 OpenAI 协议接口
- 向量检索默认走 PostgreSQL + pgvector
- Redis 相关依赖已接入，项目中也包含聊天记忆存储实现

## 调试与测试

仓库中包含以下测试类，可作为本地联调入口：

- `src/test/java/com/yuntian/chat_app/ChatAppApplicationTests.java`
- `src/test/java/com/yuntian/chat_app/PgVectorTest.java`

静态调试页：

- `src/main/resources/static/ai-chat-test.html`

## 相关说明

- 当前默认激活的 Spring Profile 是 `dev`
- 项目启动类位于 `src/main/java/com/yuntian/chat_app/ChatAppApplication.java`
- Prometheus 指标已通过 Actuator 暴露

