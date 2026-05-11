import express from 'express';
import cors from 'cors';
import morgan from 'morgan';
import { attachDemoRoutes } from './mock/demoApi.js';
/**
 * Vercel / 本地演示：统一走静态 Mock API（见 `src/mock/`）。
 * 内置账号：用户名 demo，密码 demo123（见 demoApi.ts 常量 MOCK_DEMO_PASSWORD）。
 */
const app = express();
app.use(cors());
app.use(express.json());
app.use(morgan('dev'));
attachDemoRoutes(app);
const port = process.env.PORT || 3000;
app.listen(port, () => {
    console.log(`backend listening on ${port} (demo mock API)`);
});
