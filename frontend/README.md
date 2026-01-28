# 前端项目说明（anti-fraud-web-frontend）

本目录为反诈信息识别与风险自测平台的 **Vue 3 前端**，提供前台学习训练与后台管理界面。

## 技术栈

- **Vue 3** + **TypeScript**
- **Vite 5**
- **Pinia**（状态管理）
- **Vue Router**
- **Element Plus**
- **ECharts**

## 环境要求

- **Node.js**：建议 18+

## 启动方式

### 开发启动

```bash
npm install
npm run dev
```

默认访问：`http://localhost:5173`

### 构建与预览

```bash
npm run build
npm run preview
```

## 端口与代理

Vite 代理配置见 `vite.config.ts`：

- `/api` → `http://localhost:8081`
- `/uploads` → `http://localhost:8081`

因此前端代码里一般以相对路径调用接口，例如 `src/api/http.ts` 中 `baseURL: '/api'`。

## 路由与页面

路由配置：`src/router/index.ts`

### 前台页面（用户侧）

- `/`：首页（`views/Home.vue`）
- `/login`：登录/注册（`views/Login.vue`）
- `/train`：识别训练（`views/Training.vue`）
- `/assessment`：风险测评（`views/Assessment.vue`）
- `/report/:id`：测评报告详情（`views/ReportDetail.vue`）
- `/knowledge`：知识库列表（`views/Knowledge.vue`）
- `/knowledge/learned`：已学知识（`views/LearnedKnowledge.vue`）
- `/knowledge/:id`：知识详情（`views/KnowledgeDetail.vue`）
- `/user-center`：个人中心（`views/UserCenter.vue`）
- `/achievement`：成就中心（`views/Achievement.vue`）

### 管理后台（Admin）

- `/admin`：管理后台容器（`views/Admin.vue`，使用 `layouts/AdminLayout.vue`）
- `/admin/dashboard`：仪表盘
- `/admin/cases`：诈骗案例管理
- `/admin/training`：识别训练题目管理
- `/admin/assessment`：风险测评问卷管理
- `/admin/knowledge`：知识库管理
- `/admin/carousel`：知识页轮播图管理
- `/admin/achievement`：成就规则管理
- `/admin/users`：用户数据管理
- `/admin/settings`：系统设置

权限控制（前端简化实现）：
- 需要登录：`meta.requiresAuth`
- 需要管理员：`meta.requiresAdmin`
- 管理员判定：`username === 'admin'`（见路由守卫）

## 状态管理与本地存储（账号隔离）

项目同时存在 `src/stores/` 与 `src/store/` 两套 store：

- **`src/stores/user.ts`**：用户登录态与基础资料（Pinia store）
  - 会话 key：`af_user_session`（只记录当前 `userId/username`）
  - 用户桶 key：`af_user_u_{userId}`（切换账号不串数据）
- **`src/store/assessment.ts`**：测评报告历史（本地生成 `report.id`，最多 50 条）
  - `af_assessment_u_{userId}` / `af_assessment_guest`
- **`src/store/knowledge.ts`**：已学习知识 ID 集合
  - `af_knowledge_u_{userId}` / `af_knowledge_guest`
- **`src/store/achievement.ts`**：等级/经验/成就（优先从后端加载成就规则，失败回退到内置规则）
  - `af_achievement_u_{userId}` / `af_achievement_guest`

底层存储工具：`src/utils/storage.ts`

## 技术创新点

- **账号隔离的本地持久化模型（防串号）**
  - **背景问题**：传统“全局 localStorage 一份数据”在多账号切换时会出现测评报告、已学知识、成就进度相互污染。
  - **实现方案**：
    - 用 `af_user_session` 只保存“当前是谁”（`userId/username`），对应 `src/stores/user.ts`。
    - 业务数据按用户分桶保存，例如 `af_assessment_u_{userId}`、`af_knowledge_u_{userId}`、`af_achievement_u_{userId}`（对应 `src/store/*`）。
    - 退出登录后切回 `*_guest` 桶，避免公共电脑继续展示上一用户数据。
  - **如何验证**：用 A 用户完成测评/学习后退出，B 用户登录应看到自己的空桶或自己的历史；再切回 A 用户仍能恢复 A 的桶数据。

- **成就系统“规则可配置化”（后端下发 + 本地回退）**
  - **创新点**：成就不完全写死在前端，管理员可在后台维护成就规则；前端启动时优先拉取规则，接口不可用时仍可用内置规则保证可演示。
  - **实现位置**：`src/store/achievement.ts`
    - `loadFromBackendConfig()` 请求 `GET /api/admin/achievements`（只取 `ACTIVE`）
    - 映射 `conditionType/conditionValue/rewardExp/icon` → 前端成就模型
    - 失败回退 `initAchievements()` 内置规则
  - **如何验证**：在后台新增启用成就规则后刷新前端，成就列表会按后端规则变化；关闭后端或接口报错时，前端自动回退到内置成就集。

- **统一接口返回适配层（Result 解包）**
  - **背景问题**：后端统一返回 `Result`（包含 `success/code/message/data`），页面逐个判断会导致大量重复代码且容易漏处理错误。
  - **实现方案**：在 `src/api/http.ts` 统一拦截响应：
    - `success=true` 时把 `resp.data` 直接替换为业务 `data`（页面可直接用 `resp.data`）
    - `success=false` 时抛出 `Error(message)`，统一走异常提示
  - **收益**：页面/组件更专注业务渲染，错误处理统一且可扩展（例如全局错误弹窗、重试）。

- **管理后台路由权限的“最小可用实现”**
  - **实现方案**：`src/router/index.ts` 使用路由 meta（`requiresAuth/requiresAdmin`）+ 前置守卫：
    - 未登录跳转 `/login?redirect=...`
    - 非管理员访问 `/admin/**` 跳回首页并携带 `denied=admin`
  - **特点**：不引入复杂 RBAC/JWT，也能完成“后台入口受控”的演示闭环，适合课程设计/毕设场景。

## 技术难点

- **状态一致性与“本地/后端”同步边界（知识学习）**
  - **矛盾点**：前端为了体验会本地即时标记“已学”，但权威完成状态来自后端（`finishedArticleIds`）。
  - **关键设计**：`src/store/knowledge.ts`
    - `readIds`：当前展示用已学集合（可被后端完成列表覆盖）
    - `everReadIds`：做并集防重复计数（避免“反复标记已学”刷经验/成就）
    - `syncFinishedFromBackend(ids)`：用后端完成列表覆盖 `readIds`，并与 `everReadIds` 做并集
  - **常见坑**：集合序列化/反序列化（Set ↔ Array）、NaN/字符串 ID 清洗、游客与登录用户切换时的桶隔离。

- **管理端接口返回结构与字段命名的兼容**
  - **问题来源**：管理端分页 VO/Map 返回结构可能出现多种形态（例如 `data.content`、`content`、直接数组），且字段可能存在别名（如 `difficulty/level`）。
  - **处理策略**：前端解析时必须做“多路径兜底”，避免某个字段缺失直接白屏；同时需要把后端数据映射为 UI 组件需要的结构（表格列、表单初值、选项列表）。

- **WebSocket（SockJS + STOMP）接入与可靠性**
  - **难点**：连接建立、订阅时机、断线重连、重复订阅导致消息重复、网络抖动导致“偶发收不到”。
  - **建议验证点**：手动断开网络/重启后端，前端应能重新连接并恢复订阅；通知中心不应出现重复消息或内存泄漏。

## 接口调用约定

统一 Axios 实例：`src/api/http.ts`

后端统一返回格式：`{ success, code, message, data }`  
前端拦截器会在 `success=true` 时把 `resp.data` 替换为业务 `data`，页面通常直接使用 `resp.data` 即可。

## WebSocket（可选）

前端封装：`src/composables/useWebSocket.ts`  
后端端点为 `/ws`（SockJS + STOMP），可订阅广播通知与点对点通知：

- 广播：`/topic/notifications`
- 点对点：`/user/queue/notifications`

## 常见问题

- **接口 404 或跨域**：请确认通过 `http://localhost:5173` 访问前端，且后端已启动在 `8081`，Vite 代理会转发 `/api`。
- **上传图片无法显示**：上传返回的通常是 `/uploads/...`，前端通过代理或直接访问后端同路径即可。

