## 接口测试

- 本地启动：`npm install && npm run dev`（nodemon，端口 3000）。
- 健康检查：
  - `GET /api/cases` 获取训练案例列表
  - `GET /api/knowledge` 获取防骗知识列表（支持 ?category=短信&q=关键词）
  - `GET /api/questions` 获取风险测评分题库
  - `POST /api/assessment` 提交测评答案，返回风险指数与等级
- 自动化建议：使用 Supertest/Jest 覆盖案例列表、知识筛选、题库格式与风险计算逻辑。

示例请求：

```bash
curl http://localhost:3000/api/cases
curl "http://localhost:3000/api/knowledge?category=短信&q=链接"
curl http://localhost:3000/api/questions
curl -X POST http://localhost:3000/api/assessment -H "Content-Type: application/json" -d '{"answers":{"1":2,"2":3}}'
```
