## 后端（Java / Spring Boot）重构与扩展 TODO

> 目标：在现有 mock Node.js 后端基础上，新增一个毕业设计级别的 **Spring Boot 3 + MyBatis-Plus + MySQL/H2** 后端，实现与当前前端协议基本兼容，并支持后续论文撰写与功能扩展。

---

### 一期：核心业务闭环（必须完成）

- **项目脚手架与基础配置**
  - 使用 Spring Initializr 创建 `anti-fraud-backend`（Spring Boot 3.x, Maven, JDK 17）。
  - 引入依赖：`spring-boot-starter-web`、`spring-boot-starter-validation`、`mybatis-plus-boot-starter`、`mysql-connector-j`（或 `h2`）、`lombok`、`spring-boot-starter-test`。
  - 配置数据源、MyBatis-Plus（分页插件、逻辑删除等），区分 `dev` / `prod` profile。

- **基础模块与包结构**
  - 统一包前缀：`com.example.antifraud`（可按实际修改）。
  - 子包划分：
    - `common`：统一返回体（Result）、全局异常处理、通用枚举（风险等级、题目类型等）。
    - `user`：用户实体、Mapper、Service、Controller。
    - `case`：诈骗案例与训练记录相关模块。
    - `assessment`：问卷题目、测评提交与评估逻辑。
    - `knowledge`：防骗知识与学习记录。
    - `report`：评估报告 VO/DTO 聚合。

- **数据库表与实体映射（MyBatis-Plus）**
  - 按设计创建表（建议前缀 `af_`）：
    - `af_user`：用户基础信息、当前风险等级。
    - `af_fraud_case`：诈骗案例库（类型、难度、解析等）。
    - `af_training_record`：单题训练作答记录。
    - `af_question`：问卷题库（维度、权重、题型、选项 JSON）。
    - `af_assessment_result`：测评结果与维度得分。
    - `af_knowledge`：防骗知识条目。
    - `af_learning_record`：知识学习进度与完成状态。
  - 为每张表创建对应实体、Mapper 接口，并开启 MyBatis-Plus 通用 CRUD。

- **核心 REST 接口（与现有前端联调优先）**
  - 诈骗案例训练：
    - `GET /api/cases`：分页 + 条件（类型、难度）查询案例列表。
    - `GET /api/cases/{id}`：获取单个案例详情。
    - `POST /api/train/records`：提交某次训练作答（含是否正确、可疑特征等），写入 `af_training_record`。
  - 防骗知识：
    - `GET /api/knowledge`：按分类/关键词搜索知识列表（兼容现有 `/api/knowledge?category=&q=` 风格）。
    - `GET /api/knowledge/{id}`：知识详情。
    - `POST /api/knowledge/{id}/learn`：上报学习进度/完成状态，写入 `af_learning_record`。
  - 问卷与风险测评：
    - `GET /api/questions`：返回题库列表（兼容现有结构）。
    - `POST /api/assessment`：提交问卷答案，计算维度得分与总风险指数，写入 `af_assessment_result`，返回评估报告数据结构。

- **防骗风险评估逻辑实现**
  - 在 `assessment` 模块实现 service：
    - 根据前端提交的答案（问题 ID → 选项值/选项 ID）从 `af_question` 读取题目配置。
    - 计算每个维度的加权得分与理论最高分，归一化到 0–100。
    - 综合得分为各维度平均值，根据阈值划分风险等级（低/中/高）。
  - 将维度得分、总分、等级等落库到 `af_assessment_result`。

- **评估报告 VO / 返回结构**
  - 设计 `AssessmentReportVO`（供 `/api/assessment` 返回）：
    - `score`：综合风险指数（0–100）。
    - `level`：风险等级（low/medium/high）。
    - `dimensions`：各维度名称 + 分数 + 等级（用于前端雷达图/柱状图）。
    - `suggestions`：按维度给出的防范建议列表。
    - `createdAt`：评估生成时间。
  - Service 层组装 VO，Controller 统一返回 `Result<AssessmentReportVO>`。

---

### 二期：后台管理与扩展功能（可选 / 加分项）

- **认证与简单权限控制**
  - 增加 `AuthController`：注册、登录接口（基于 JWT 或简单 Token）。
  - 为管理端接口增加管理员角色判断（可先用硬编码/简单角色字段实现）。

- **后台内容管理（Admin 模块）**
  - 诈骗案例管理：`/api/admin/cases` 新增 / 修改 / 上下架。
  - 题库管理：`/api/admin/questions` 新增 / 编辑题目、选项与权重。
  - 知识库管理：`/api/admin/knowledge` 增删改查。

- **数据统计与可视化支撑接口**
  - 训练统计：按用户统计训练次数、正确率、最近 7 天趋势。
  - 测评统计：按维度统计平均分、用户风险等级分布。
  - 学习统计：知识条目完成率、阅读热度排行。

- **JSON Mock 数据迁移脚本（从现有 Node.js mock 迁移）**
  - 提供启动初始化脚本：将 `backend/src/data/*.json` 中的案例/知识/题库导入 MySQL/H2。
  - 便于与现有前端快速打通、减少双份维护成本。

---

> 后续可以根据实际开发进度，在本文件中继续细化每个模块的子任务（如 DTO 设计、单元测试、接口文档生成等），以支撑论文的“系统实现”与“系统测试”章节。

