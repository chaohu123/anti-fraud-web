# 反诈信息识别与风险自测平台（anti-fraud-web）

本仓库包含一个前后端分离的反诈学习平台：

- **后端**：Spring Boot + MyBatis-Plus + MySQL（可切换 H2）+ WebSocket(STOMP) + Swagger(OpenAPI)
- **前端**：Vue 3 + Vite + Pinia + Element Plus + ECharts

## 目录结构

```text
anti-fraud-web/
  backend/    # Spring Boot 后端（端口 8081）
  frontend/   # Vue3 前端（端口 5173，代理到后端）
```

## 环境要求

- **Node.js**：建议 18+（与 Vite 5 兼容）
- **JDK**：17（后端 `pom.xml` 指定 Java 17）
- **Maven**：3.8+
- **MySQL**：8.x（推荐，用于持久化数据）

## 快速启动（推荐：MySQL）

### 1) 初始化数据库

1. 创建数据库并建表（脚本在 `backend/sql/schema.sql`）
2. 插入测试数据（脚本在 `backend/sql/insert.sql`）

你可以参考 `backend/sql/README.md` 中的说明执行。

> 注意：后端 `backend/src/main/resources/application.yml` 里默认启用 `mysql` profile，并包含数据库连接信息。请按你的本机环境修改 `url/username/password`，不要使用仓库中的默认密码跑生产环境。

### 2) 启动后端

在项目根目录执行：

```bash
cd backend
mvn spring-boot:run
```

后端默认端口：`http://localhost:8081`

Swagger/OpenAPI（后端开启时可用）：
- `http://localhost:8081/swagger-ui.html`
- `http://localhost:8081/v3/api-docs`

### 3) 启动前端

在项目根目录执行：

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

Vite 代理配置（见 `frontend/vite.config.ts`）：
- `/api  -> http://localhost:8081`
- `/uploads -> http://localhost:8081`

## 构建与部署（可选）

### 前端打包

```bash
cd frontend
npm run build
npm run preview
```

### 后端打包

```bash
cd backend
mvn -DskipTests package
java -jar target/antifraud-backend-1.0.0.jar
```

## 端口与访问路径

- **前端**：`http://localhost:5173`
- **后端**：`http://localhost:8081`
- **后端接口前缀**：`/api/**`
- **上传文件访问**：`/uploads/**`（映射到本地目录，见下文“上传与静态资源”）
- **WebSocket 端点**：`/ws`（SockJS + STOMP）

## 核心功能与业务逻辑（完整说明）

下面按“用户视角的业务闭环”说明系统功能，并给出前后端对应的页面/接口/数据流。

### 1. 登录与用户体系

#### 功能
- 用户注册、登录
- 个人资料：昵称、头像
- 修改密码
- 区分管理员：用户名为 `admin` 视为管理员（前端路由守卫判断）

#### 前端实现要点
- 登录态：Pinia 的 `stores/user.ts` 使用 `localStorage` 持久化
  - **会话 key**：`af_user_session`（仅记录 `userId/username`）
  - **用户桶 key**：按用户隔离为 `af_user_u_{userId}`（避免切换账号串数据）
- 路由权限：
  - 管理后台 `/admin/**` 需要登录并且 `username=admin`（见 `frontend/src/router/index.ts`）

#### 后端接口
- `POST /api/users/register`：注册（返回 `userId`）
- `POST /api/users/login`：登录（返回 `userId/username`，不使用 JWT，按毕业设计场景简化）
- `GET /api/users/{userId}`：用户基础信息
- `PUT /api/users/{userId}/avatar`：更新头像（URL/base64 均可由前端传入）
- `PUT /api/users/{userId}/info`：更新昵称等
- `PUT /api/users/{userId}/password`：修改密码（BCrypt）

---

### 2. 识别训练（诈骗案例库 + 训练记录）

#### 功能
- 浏览/筛选诈骗案例（按类型、难度）
- 训练作答并提交结果
- 训练统计：总次数、正确次数、正确率、按诈骗类型的错误率

#### 前端页面
- 识别训练页：`/train`（`frontend/src/views/Training.vue`）

#### 后端接口
- `GET /api/cases`：训练页使用的案例列表（支持 `type/level`）
- `GET /api/cases/random`：随机获取一个案例
- `GET /api/cases/{id}`：案例详情
- `POST /api/train/records`：提交训练结果（写入训练记录表）
- `GET /api/train/stats/{userId}`：获取用户训练统计数据

#### 数据流（典型一次训练）
1. 前端请求案例（列表/随机/详情）
2. 用户选择“诈骗/安全”等答案并提交
3. 前端调用 `POST /api/train/records` 持久化训练记录
4. 个人中心/训练页拉取 `GET /api/train/stats/{userId}` 展示统计与薄弱类型

---

### 3. 风险测评（问卷 + 评估报告 + 风险等级）

#### 功能
- 拉取风险测评问卷题目与选项
- 用户提交作答，后端计算并返回“可解释”的测评报告
- 报告包含：综合分、等级（low/medium/high）、维度分（信息/金融/心理）、解释文本、建议列表
- 后端会将本次测评结果落库，并同步更新用户表的 `riskLevel`

#### 前端页面
- 测评页：`/assessment`（`frontend/src/views/Assessment.vue`）
- 报告详情：`/report/:id`（`frontend/src/views/ReportDetail.vue`）

#### 前端本地存储逻辑（报告历史）
- `store/assessment.ts` 以用户隔离的 key 保存报告：
  - `af_assessment_u_{userId}` 或 `af_assessment_guest`
  - 保存 `lastReport` 与 `reportHistory`（最多 50 条），并生成本地 `report.id`

#### 后端接口
- `GET /api/questions`：获取问卷题目列表（含选项）
- `POST /api/assessment`：提交作答并返回报告

#### 评分与分级逻辑（后端）
后端（`AssessmentServiceImpl`）核心流程：
1. 按题目维度（INFO/FINANCE/PSYCH）计算**实际得分**与**理论最高分**
2. 将各维度归一化到 0~100
3. 综合分为各维度平均
4. 按阈值分级：\<40 低风险、40~70 中风险、\>=70 高风险
5. 生成 explanation 与 suggestions（可解释性）
6. 落库并同步更新用户 `riskLevel`（0/1/2）

---

### 4. 防骗知识库（文章 + 学习进度）

#### 功能
- 知识列表（按分类/关键词）
- 文章详情（含常见话术、案例、推荐训练等扩展信息）
- 记录学习进度（0~100），100 视为完成
- 查询整体学习进度：总数、完成数、完成率、称号、已完成文章 ID 列表

#### 前端页面
- 知识列表：`/knowledge`（`frontend/src/views/Knowledge.vue`）
- 已学知识：`/knowledge/learned`（`frontend/src/views/LearnedKnowledge.vue`）
- 知识详情：`/knowledge/:id`（`frontend/src/views/KnowledgeDetail.vue`）

#### 前端本地状态与后端同步
- `store/knowledge.ts` 维护：
  - `readIds`：当前“已学”集合
  - `everReadIds`：曾经学习过的集合（用于防止重复刷经验/重复计数）
- 后端返回完成列表后，前端用 `syncFinishedFromBackend(ids)` 同步 `readIds`

#### 后端接口
- `GET /api/knowledge`：列表（`category`/`q`）
- `GET /api/knowledge/{id}`：详情
- `POST /api/knowledge/{id}/learn?userId=...&progress=...`：记录学习行为并返回整体学习进度
- `GET /api/knowledge/progress/{userId}`：查询学习进度

---

### 5. 知识页轮播图（Banner）

#### 功能
- 前台知识页展示轮播图（仅启用项）
- 管理端增删改轮播图
- 支持本地上传图片，返回 `/uploads/...` 形式的可访问 URL

#### 后端接口
- 前台：
  - `GET /api/carousel`：获取已启用轮播列表
- 管理端：
  - `GET /api/admin/carousel`：全部轮播图
  - `POST /api/admin/carousel`：新增
  - `PUT /api/admin/carousel/{id}`：更新
  - `DELETE /api/admin/carousel/{id}`：删除
  - `POST /api/admin/carousel/upload`：单张上传
  - `POST /api/admin/carousel/upload/batch`：批量上传

---

### 6. 成就系统（等级/经验/成就规则）

#### 功能
- 用户等级与经验条（升级所需经验：`level * 100`）
- 成就分类：训练/学习/测评/特殊
- 既支持**前端内置规则**，也支持从后端加载管理员维护的成就规则（优先后端）

#### 前端逻辑
- `store/achievement.ts`
  - 启动时尝试请求 `GET /api/admin/achievements` 拉取规则（`status=ACTIVE`）
  - 失败则使用内置成就列表（如“完成 3 次训练”“学习 5 条知识”等）
  - 解锁成就会奖励经验，并可触发等级提升

#### 后端接口（管理端）
- `GET /api/admin/achievements`：成就规则列表（分页）
- `POST /api/admin/achievements`：创建规则
- `PUT /api/admin/achievements/{id}`：更新规则
- `DELETE /api/admin/achievements/{id}`：删除规则
- `DELETE /api/admin/achievements/batch`：批量删除

---

### 7. 管理后台（Admin）

前端管理入口：`/admin`（`frontend/src/views/Admin.vue` + `layouts/AdminLayout.vue`）

包含模块：
- **仪表盘**：用户数、风险分布、近 7 天活跃趋势（测评次数）
- **诈骗案例管理**：增删改查、批量删除、筛选
- **识别训练题目管理**：基于案例转换的题库管理（本质仍操作案例）
- **风险测评问卷管理**：题目与选项的 CRUD
- **防骗知识库管理**：文章 CRUD
- **知识页轮播图管理**：轮播 CRUD + 上传
- **成就规则管理**：成就 CRUD（供前端加载规则）
- **用户数据管理**：用户列表、风险筛选、用户报告（简化版）
- **系统设置**：阈值/经验配置（当前为“校验 + 返回成功”，未持久化）

主要后端接口（管理端，均在 `AdminController`/`AdminStatisticsController`）：
- 统计：
  - `GET /api/admin/statistics/users`
  - `GET /api/admin/statistics/risk-distribution`
  - `GET /api/admin/statistics/active-trend`
- 案例：
  - `GET /api/admin/cases`
  - `POST /api/admin/cases`
  - `PUT /api/admin/cases/{id}`
  - `DELETE /api/admin/cases/{id}`
  - `DELETE /api/admin/cases/batch`
- 训练题目（基于案例）：
  - `GET /api/admin/training/questions`
  - `POST /api/admin/training/questions`
  - `PUT /api/admin/training/questions/{id}`
  - `DELETE /api/admin/training/questions/{id}`
  - `DELETE /api/admin/training/questions/batch`
- 测评问卷题目：
  - `GET /api/admin/assessment/questions`
  - `POST /api/admin/assessment/questions`
  - `PUT /api/admin/assessment/questions/{id}`
  - `DELETE /api/admin/assessment/questions/{id}`
  - `DELETE /api/admin/assessment/questions/batch`
- 知识库：
  - `GET /api/admin/knowledge`
  - `POST /api/admin/knowledge`
  - `PUT /api/admin/knowledge/{id}`
  - `DELETE /api/admin/knowledge/{id}`
  - `DELETE /api/admin/knowledge/batch`
- 用户：
  - `GET /api/admin/users`
  - `GET /api/admin/users/{id}/report`
- 系统设置：
  - `GET /api/admin/settings`
  - `PUT /api/admin/settings`

> 说明：`AdminController` 注释中明确写了“此处不做完整鉴权”，前端也仅用 `username=admin` 作为管理员判断。若用于真实环境，建议补齐 JWT/Session + 后端鉴权与权限控制。

---

### 8. WebSocket 实时通知（可选增强）

#### 连接方式
- STOMP 端点：`ws://localhost:8081/ws`（启用 SockJS）
- 订阅：
  - 广播通知：`/topic/notifications`
  - 点对点通知：`/user/queue/notifications`
- 发送（应用前缀）：`/app/**`
  - `/app/ping`：心跳（服务端转发到 `/topic/pong`）
  - `/app/user-status`：状态更新
  - `/app/learning-progress`：学习进度同步（完成时可触发通知）

#### 后端相关代码
- 配置：`backend/src/main/java/com/xxx/antifraud/config/WebSocketConfig.java`
- 控制器：`backend/src/main/java/com/xxx/antifraud/controller/WebSocketController.java`
- 推送服务：`backend/src/main/java/com/xxx/antifraud/service/WebSocketService.java`

---

## 上传与静态资源（/uploads）

后端通过 `WebMvcConfig` 将 `/uploads/**` 映射到本地目录 `upload.path`：

- 配置项：`upload.path`（见 `backend/src/main/resources/application.yml`）
- 默认值：`${user.home}/antifraud-uploads`
- 访问示例：上传后返回 `/uploads/carousel/xxx.png`，前端通过代理或直连即可访问

## 常见问题（Troubleshooting）

- **前端请求接口报错/跨域**：请确认你是通过 `http://localhost:5173` 访问前端，并且 Vite 代理已生效（`/api` -> `8081`）。
- **数据库连不上**：检查 `backend/src/main/resources/application.yml` 的 MySQL 连接信息是否匹配本机。
- **轮播图/上传文件无法访问**：确认 `upload.path` 指向的目录存在且有写权限；访问路径应为 `/uploads/**`。

