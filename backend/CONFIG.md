## 后端配置说明（案例库 / 知识库 / 测评分题库）

本后端为前端提供统一的 mock 数据源，所有内容均通过 JSON 文件集中管理，便于在毕业设计中演示“内容可配置”的设计。

目录结构：

- `src/data/cases.json`：诈骗案例训练库（Train 页使用）
- `src/data/knowledge.json`：防骗知识库（Knowledge 页使用）
- `src/data/questions.json`：防骗风险测评分题库（Assessment/Report 使用）

后端启动时会将以上 JSON 文件加载到内存中，通过以下 REST 接口对外提供：

- `GET /api/cases`
- `GET /api/knowledge`
- `GET /api/questions`

下面详细说明每个配置文件的结构与扩展方式。

---

## 1. 诈骗案例训练库：`src/data/cases.json`

用途：为“诈骗信息识别训练页（/train）”提供多模态案例，包含短信、邮件、语音占位、网页截图等。

数据结构（数组中的每一项）：

```json
{
  "id": 1,
  "type": "sms",               // 案例类型：sms(短信) | email(邮件) | audio(语音) | site(网站)
  "content": "【银行】点击 http://fake.com 验证账户，否则将冻结账号。",
  "hint": "仿冒机构 + 钓鱼链接 + 恐吓催促",              // 简要套路提示，用于答题后提示
  "suspiciousPoints": [
    "非官方域名/短链",
    "恐吓语气催促立即操作"
  ],                           // 可疑特征列表，前端多选题和解析弹窗会引用
  "level": "easy",             // 难度：easy | medium | hard（可用于前端筛选或权重）
  "mediaUrl": "https://...",   // 可选字段：网站截图/转账图等（type=site 时较常用）
  "answer": "fraud"            // 标准答案：fraud(诈骗) | safe(正常)
}
```

### 如何新增/修改案例

1. 打开 `src/data/cases.json`。
2. 在数组末尾新增一条对象，并**保证 id 不重复**：
   - `type` 建议与前端 UI 一一对应（短信/邮件/语音/网站）。
   - `suspiciousPoints` 建议为 2–5 条简洁短句，便于前端多选与解析展示。
   - `mediaUrl` 可以为空；若为网站/转账截图则配置网络图片地址或静态资源 URL。
3. 保存文件后重启后端（或借助 nodemon 自动重启），前端刷新 `/train` 即可看到新案例。

> 说明：`POST /api/admin/cases` 接口会在运行时往内存中的 `cases` 数组追加数据，但不会写回 JSON 文件，主要用于演示“后台录入”。对持久化有要求时，可扩展为写入 JSON 或数据库。

---

## 2. 防骗知识库：`src/data/knowledge.json`

用途：为“防骗知识学习页（/knowledge）”提供分类知识卡片列表，支持按类别/关键词筛选。

数据结构：

```json
{
  "id": 1,
  "category": "短信",                        // 知识类别：短信/电话/网站/社交/其他
  "title": "钓鱼链接识别",
  "summary": "检查域名是否为官方地址、是否启用 HTTPS，谨慎对待短链和陌生链接。"
}
```

后端接口：

- `GET /api/knowledge`：返回全部知识项。
- 支持查询参数：
  - `?category=短信` 按类别精确筛选。
  - `?q=链接` 按标题或摘要模糊匹配。

### 如何配置知识库

1. 打开 `src/data/knowledge.json`。
2. 新增条目时注意：
   - `category` 与前端 Knowledge 页中的分类选项对应（可自定义中文）。
   - `title` 尽量简短清晰，适合卡片标题展示。
   - `summary` 适合作为 1–2 行摘要，突出防范要点。
3. 保存后重启后端，即可在 `/knowledge` 中看到新内容，并可通过分类筛选/搜索访问。

> 前端还会在本地记录“已学习/未学习”状态（Pinia + localStorage），不依赖后端存储，因此只需要保证知识条目的 id 稳定即可。

---

## 3. 防骗风险测评分题库：`src/data/questions.json`

用途：作为“防骗风险自测问卷页（/assessment）”的题库数据源，也可供后续扩展为完全动态问卷。

数据结构：

```json
{
  "id": 1,
  "text": "收到未知链接会直接点击吗？",
  "dimension": "info",                    // 维度：info(信息防护) | finance(金融安全) | psych(心理倾向)
  "weight": 1.2,                          // 该题在维度评分中的权重
  "type": "single",                       // 题型：single(单选) | multi(多选)
  "options": [
    { "label": "经常", "value": 3 },
    { "label": "偶尔", "value": 2 },
    { "label": "从不", "value": 0 }
  ]
}
```

评分逻辑（由前端 `utils/riskEvaluator.ts` 实现，后端只负责出题）：

- 单选题：使用所选项的 `value`。
- 多选题：将选中的多个选项的 `value` 累加，再结合 `weight` 对应维度加权。
- 维度总分：每维度归一化到 0–100，最终综合得分为三维平均值。

后端接口：

- `GET /api/questions`：返回上述 JSON 完整数组，前端可直接按题目 id 读取。

### 如何配置/扩展题库

1. 打开 `src/data/questions.json`。
2. 增加新题时建议：
   - 先确定所属维度 `dimension` 和题型 `type`。
   - `weight` 决定该题影响程度，一般在 1.0–2.0 之间。
   - `options` 中：
     - `label` 为展示文案（问题选项）。
     - `value` 为风险得分贡献（数值越高表示越高风险）。
3. 多维度覆盖建议：
   - `info` 至少 2 题（信息泄露、链接点击习惯等）
   - `finance` 至少 2 题（理财/转账习惯等）
   - `psych` 至少 1–2 题（冲动、贪心、恐惧等心理倾向）

---

## 4. 端到端联调与论文撰写建议

1. **配置修改流程（运维视角）**
   - 修改 JSON → 重启后端 → 刷新前端页面 → 验证案例/知识/问卷是否按预期展示。
2. **论文《系统总体设计》章节可写点**
   - 将 `data/*.json` 描述为“内容配置层/知识与案例管理层”，与“前端展示层、业务逻辑层、评估算法层”形成分层架构。
   - 强调通过 JSON 配置可以方便地更新案例库和知识库，便于实战演练时持续补充新型诈骗手法。
3. **论文《交互设计》章节可写点**
   - 描述前端如何基于 `/api/cases`、`/api/knowledge`、`/api/questions` 动态渲染训练案例、知识卡片和测评问卷。
   - 指出后端支持按类别/关键词筛选知识内容，配合前端的分类标签与搜索框，实现“面向弱项的精准学习”。

