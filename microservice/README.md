# AI Creative Workshop（微服务版）

AI 应用生成平台的后端微服务实现：用户在页面描述需求，AI 生成可运行的网页 / 多文件 / Vue 工程。本仓库只包含**后端**，前端仓库暂不在此管理。

## 架构

```
                    ┌──────────────────────────────┐
   前端 / Nginx ───► │  ai-creative-app       :8125 │  主业务（应用、对话历史、代码生成、部署、静态资源）
                    └──────────┬───────────────────┘
                               │ Dubbo (tri)
              ┌────────────────┼─────────────────┐
              ▼                                  ▼
   ┌────────────────────┐            ┌────────────────────────┐
   │ ai-creative-user   │            │ ai-creative-screenshot │
   │ :8124 / dubbo 50051│            │ :8127 / dubbo 50052    │
   └────────────────────┘            └────────────────────────┘
              │                                  │
        MySQL（共用库）/ Redis（共享 session）/ Nacos（注册中心）
```

- 服务间调用：**Dubbo 3.3 + triple 协议**，注册中心 **Nacos 3.x**（`nacos://127.0.0.1:8848`）
- 登录态：Spring Session + Redis 共享，三个服务必须使用**同一个 Redis 库**
- 存储：MySQL 共用 `ai_creative_workshop` 库（按表拆到各服务的 mapper），截图上传腾讯云 COS

## 模块

| 模块 | 说明 |
|---|---|
| `ai-creative-common` | 通用返回体、异常、常量、CORS、COS 客户端、Jackson 配置 |
| `ai-creative-model` | 实体 / DTO / VO / 枚举（`App`、`ChatHistory`、`User`） |
| `ai-creative-client` | Dubbo 接口契约：`InnerUserService`、`InnerScreenshotService` |
| `ai-creative-ai` | AI 能力库：生成服务、模型配置、工具调用、输入护栏、LangChain4j 补丁类 |
| `ai-creative-user` | 用户服务：注册登录、权限、用户管理（端口 8124） |
| `ai-creative-app` | 主业务服务：应用管理、对话历史、代码生成、部署、截图调度（端口 8125） |
| `ai-creative-screenshot` | 截图服务：Selenium 无头浏览器截图并上传 COS（端口 8127） |

## 快速开始

1. **环境**：JDK 21、Maven 3.9+、MySQL 8、Redis、Nacos 3.x（`8848` 端口，账号密码均为 `nacos`）
2. **建库**：在 MySQL 中创建 `ai_creative_workshop` 并导入表结构
3. **启动顺序**：Nacos / MySQL / Redis → `AiCreativeUserApplication`、`AiCreativeScreenshotApplication` → `AiCreativeAppApplication`
4. **接口文档**：`http://localhost:8125/api/doc.html`（app）、`http://localhost:8124/api/doc.html`（user）

## 配置说明

- 三个服务的 yml 都在各自模块的 `src/main/resources/application.yml`
- 端口：user `8124`、app `8125`、screenshot `8127`，统一 `context-path: /api`
- Dubbo 端口：user `50051`、screenshot `50052`、app `50053`
- 本仓库当前为**私有**，yml 中保留了开发期的明文密钥（DeepSeek、DashScope、腾讯云 COS、数据库密码）。**转为公开仓库前必须替换为占位符**。
- 应用部署产物落在 `AppConstant.CODE_DEPLOY_ROOT_DIR`（`{user.dir}/tmp/code_deploy`），需要由 Nginx 等反向代理暴露，否则部署链接无法访问。

## 已知问题 / 待办

- [x] app 服务组件扫描（`scanBasePackages` 覆盖 `com.aicreative.aicreativeworkshop`）
- [x] AI 模型 HTTP 客户端统一（`langchain4j-http-client-jdk` + 各 builder 显式 `httpClientBuilder`）
- [x] 多文件生成缺块补全（`generateMultiFileCodeRetry` + `AiCodeGeneratorFacade` 合并保存）
- [ ] 监控未接入：缺少 actuator / micrometer-prometheus，Grafana 面板无数据
- [ ] 部署链路缺 Nginx 反向代理，`code.deploy-host` 未配置
- [ ] LangGraph4j 工作流（`/workflow/**`）尚未迁移到本仓库
- [ ] 截图异步调用缺少异常处理；`InnerUserService` 中混入了 Web 相关的静态方法