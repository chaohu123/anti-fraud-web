# 后端项目说明（antifraud-backend）

本目录为反诈信息识别与风险自测平台的 **Spring Boot 后端**，提供用户、训练、测评、知识库、后台管理、上传与 WebSocket 实时通知等能力。

## 技术栈

- **Spring Boot 2.7.x**
- **Java 17**
- **MyBatis-Plus**
- **MySQL 8**（推荐）/ **H2**（可选）
- **Swagger / OpenAPI（springdoc 1.x）**
- **WebSocket（STOMP + SockJS）**

## 环境要求

- **JDK**：17
- **Maven**：3.8+
- **MySQL**：8.x（推荐）

## 配置说明

主配置文件：`src/main/resources/application.yml`

### Profile

- 默认：`spring.profiles.active: mysql`
- 可切换为：`h2`

### 端口

- `server.port: 8081`

### 数据库（MySQL）

在 `mysql` profile 下配置：

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

> 注意：仓库里可能存在示例密码，请按你的环境修改，勿用于生产环境。

## 数据库初始化（MySQL）

SQL 位于：`sql/`

- `sql/schema.sql`：创建数据库与完整表结构
- `sql/insert.sql`：插入示例/测试数据（含测试账号、案例、题目、知识、成就等）

执行方式可参考 `sql/README.md`。

## 技术创新点

- **可解释的风险测评算法链路（可展示、可追溯）**
  - **核心思路**：不是简单“加总打分”，而是构建“可解释”的评分过程：
    - 按维度（INFO/FINANCE/PSYCH）分别计算 **实际得分** 与 **理论最高分**
    - 做归一化：维度分 \(= 实际/理论最高 \times 100\)
    - 综合分取各维度平均，并按阈值分级（\<40 LOW，40~70 MEDIUM，\>=70 HIGH）
    - 生成 explanation（解释文本）与 suggestions（建议列表）
  - **实现位置**：`service/impl/AssessmentServiceImpl.java`
  - **闭环落库**：保存测评记录，并同步更新用户表 `riskLevel`（0/1/2），便于个人中心与管理统计复用。
  - **如何验证**：调整问卷题目权重/选项分值后，维度分与综合分会按归一化规则变化；同一用户测评后用户风险等级字段发生更新。

- **“前台体验 + 后台运营”的数据联动设计**
  - **核心点**：后台维护的数据直接驱动前台展示与训练/测评/知识学习逻辑，形成可演示的“运营闭环”：
    - 后台增删改案例 → 前台训练题库与随机案例变化
    - 后台维护测评题目与选项 → 前台问卷内容变化，评分随配置变化
    - 后台维护知识文章 → 前台知识列表与详情变化
    - 后台维护成就规则（ACTIVE）→ 前端优先加载并即时生效（配合前端回退机制）
  - **实现位置**：`controller/AdminController.java`（大量兼容字段映射以适配前端管理页面）

- **上传资源目录可配置 + 静态映射（不依赖对象存储）**
  - **实现**：
    - `upload.path` 指定本地保存目录（默认 `${user.home}/antifraud-uploads`）
    - `WebMvcConfig` 将 `/uploads/**` 映射到该目录
    - 上传接口返回 `/uploads/...`，前端可直接引用
  - **收益**：部署更轻量；答辩现场可离线演示“上传轮播图→前台立刻展示”。
  - **实现位置**：`config/WebMvcConfig.java`、`service/impl/CarouselServiceImpl.java`

- **实时通知能力（STOMP + SockJS）作为可扩展点**
  - **能力**：支持广播（`/topic`）与点对点（`/user/queue`）通知，可用于学习完成、风险更新、成就解锁等事件推送。
  - **实现位置**：`config/WebSocketConfig.java`、`controller/WebSocketController.java`、`service/WebSocketService.java`
  - **如何验证**：连接 `/ws` 后订阅 `/topic/notifications` 与 `/user/queue/notifications`，触发相关业务事件可扩展推送（当前已有学习进度完成触发通知的示例代码路径）。

## 技术难点

- **评分归一化与题目配置正确性（算法难点）**
  - **影响因素**：维度权重、题型（SINGLE/多选）、选项分值都会影响“理论最高分”和归一化结果。
  - **关键点**：必须保证
    - 理论最高分的计算方式与题型一致（单选取 max，多选取 sum）
    - 用户提交的 optionId 必须归属对应 questionId（否则会被刷分或产生脏数据）
  - **对应实现**：`AssessmentServiceImpl` 中题目/选项读取与归一化计算逻辑。

- **管理端接口的字段兼容与数据映射（工程难点）**
  - **原因**：管理页面字段命名与数据库字段并不总是 1:1（例如 `difficulty/level`，训练题目以案例为底层数据，问卷选项需要映射为前端表单结构）。
  - **对应实现**：`AdminController` 中大量 `Map<String,Object>` 解析、类型转换、默认值与兼容字段处理。
  - **常见坑**：前端传参类型不稳定（字符串/数字混用）、空值处理、批量删除参数结构。

- **统计口径与缺表/空数据的容错**
  - **问题**：仪表盘活跃趋势依赖测评结果表；在“未初始化数据/表结构不完整/演示环境”下可能查询失败。
  - **处理策略**：统计服务在异常时返回最近 7 天的零数据而不是 500，保证管理后台可用。
  - **对应实现**：`AdminStatisticsServiceImpl#getActiveTrend` 的 try/catch 降级逻辑。

- **上传安全、路径与权限（部署难点）**
  - **问题点**：
    - Windows/Unix 路径差异与目录权限
    - 文件名生成、防止覆盖、目录穿越风险
    - 类型校验（仅允许图片后缀）与访问映射一致性
  - **对应实现**：`CarouselServiceImpl` 的后缀校验、UUID 文件名、`upload.path` 目录创建与 `/uploads/**` 映射。

## 启动方式

### 开发启动

```bash
mvn spring-boot:run
```

启动后访问：

- 后端：`http://localhost:8081`
- Swagger UI：`http://localhost:8081/swagger-ui.html`
- OpenAPI：`http://localhost:8081/v3/api-docs`

### 打包运行

```bash
mvn -DskipTests package
java -jar target/antifraud-backend-1.0.0.jar
```

## API 模块概览（核心业务）

> 下面列举的是项目中“最核心、最常用”的接口路径；更完整的接口列表建议以 Swagger 为准。

### 1) 用户模块

前缀：`/api/users`

- `POST /register`：注册
- `POST /login`：登录（返回 `userId/username`，不使用 JWT，按场景简化）
- `GET /{userId}`：用户信息
- `PUT /{userId}/avatar`：更新头像
- `PUT /{userId}/info`：更新昵称等
- `PUT /{userId}/password`：修改密码（BCrypt）

### 2) 诈骗案例库（训练数据）

前缀：`/api/cases`

- `GET /`：训练页案例列表（支持 `type/level`）
- `GET /random`：随机案例
- `GET /{id}`：案例详情
- `GET /page`：分页列表（扩展/管理用途）

### 3) 训练记录

前缀：`/api/train`

- `POST /records`：提交训练结果（落库训练记录）
- `GET /stats/{userId}`：训练统计（总次数/正确率/按类型错误率）

### 4) 风险测评（问卷 + 报告）

- `GET /api/questions`：问卷题目列表（含选项）
- `POST /api/assessment`：提交作答并返回报告

测评算法特点（后端实现为可解释评分）：
- INFO/FINANCE/PSYCH 三维度加权得分并归一化到 0~100
- 综合分为各维度平均
- 阈值分级：\<40 LOW，40~70 MEDIUM，\>=70 HIGH
- 结果会落库并同步更新用户表 `riskLevel`（0/1/2）

### 5) 防骗知识库与学习进度

前缀：`/api/knowledge`

- `GET /`：列表（`category`/`q`）
- `GET /{id}`：详情
- `POST /{id}/learn?userId=...&progress=...`：记录学习行为（进度 0~100，100 视为完成）
- `GET /progress/{userId}`：学习进度统计与已完成 ID 列表

### 6) 轮播图（前台）

前缀：`/api/carousel`

- `GET /`：获取已启用轮播列表

## 管理后台接口（Admin）

前缀：`/api/admin`

说明：当前项目对后台接口**未做完整鉴权**（注释中也明确指出），主要用于毕业设计展示“后台可维护数据”。真实环境建议补齐鉴权与权限系统。

### 仪表盘统计

前缀：`/api/admin/statistics`

- `GET /users`：用户总数
- `GET /risk-distribution`：风险等级分布
- `GET /active-trend`：近 7 天活跃趋势（按测评记录统计）

### 业务数据维护（部分示例）

- 案例管理：`/api/admin/cases`（CRUD + batch delete）
- 训练题目管理：`/api/admin/training/questions`（基于案例转换的题库维护）
- 测评问卷管理：`/api/admin/assessment/questions`（题目 + 选项 CRUD）
- 知识库管理：`/api/admin/knowledge`（CRUD + batch delete）
- 轮播图管理：`/api/admin/carousel`（CRUD + upload）
- 成就规则管理：`/api/admin/achievements`（CRUD + batch delete）
- 用户管理：`/api/admin/users`、`/api/admin/users/{id}/report`（简化报告）
- 系统设置：`/api/admin/settings`（当前为参数校验 + 成功返回，未持久化）

## 上传与静态资源（/uploads）

上传目录配置：`upload.path`（`application.yml`），默认 `${user.home}/antifraud-uploads`。

静态映射：`WebMvcConfig` 将 `/uploads/**` 映射到上述本地目录，上传后返回的 URL 一般形如：

- `/uploads/carousel/xxx.png`

前端可通过 Vite 代理 `/uploads` 或直接访问后端同路径获取图片。

## WebSocket（STOMP + SockJS）

- 端点：`/ws`
- 应用前缀：`/app`
- Broker：`/topic`（广播）、`/queue`（队列）
- 用户前缀：`/user`

常见订阅地址：
- 广播：`/topic/notifications`
- 点对点：`/user/queue/notifications`

后端相关类：
- `config/WebSocketConfig.java`
- `controller/WebSocketController.java`
- `service/WebSocketService.java`

