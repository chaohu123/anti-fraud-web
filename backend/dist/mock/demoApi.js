import casesJson from '../data/cases.json' with { type: 'json' };
import knowledgeJson from '../data/knowledge.json' with { type: 'json' };
import questionsJson from '../data/questions.json' with { type: 'json' };
import carouselJson from './data/carousel.json' with { type: 'json' };
import achievementsJson from './data/achievements.json' with { type: 'json' };
import adminUsersJson from './data/adminUsers.json' with { type: 'json' };
import { ok, fail } from './result.js';
import { evaluateAssessment, mapQuestionsForClient } from './assessmentEngine.js';
const MOCK_DEMO_PASSWORD = 'demo123';
let nextUserId = 1000;
const registeredUsers = new Map();
function paginate(items, page, size) {
    const p = Math.max(1, Number(page) || 1);
    const s = Math.max(1, Math.min(500, Number(size) || 10));
    const start = (p - 1) * s;
    return { content: items.slice(start, start + s), total: items.length };
}
function toAdminCaseRow(c) {
    const typeMap = {
        sms: 'SMS',
        email: 'EMAIL',
        audio: 'PHONE',
        site: 'WEBSITE',
    };
    const levelMap = {
        easy: 'EASY',
        medium: 'MEDIUM',
        hard: 'HARD',
    };
    return {
        id: c.id,
        title: `${c.type} 案例`,
        type: typeMap[String(c.type).toLowerCase()] || 'OTHER',
        content: c.content,
        hint: c.hint,
        answer: c.answer === 'safe' ? 'SAFE' : 'FRAUD',
        level: levelMap[String(c.level).toLowerCase()] || 'EASY',
        mediaUrl: c.mediaUrl || '',
    };
}
function toAdminKnowledgeRow(k) {
    return {
        ...k,
        contentType: 'ARTICLE',
        status: 'PUBLISHED',
        coverImage: '',
        videoUrl: '',
    };
}
function toAdminRiskQuestionRow(q, idx) {
    return {
        id: q.id ?? idx + 1,
        dimension: String(q.dimension || 'info').toUpperCase(),
        question: q.text,
        weight: q.weight,
        options: q.options.map((o) => ({ text: o.label, score: o.value })),
    };
}
function filterCases(rows, req) {
    let list = [...rows];
    const { keyword, type, answer, difficulty } = req.query;
    if (keyword) {
        const k = keyword.toLowerCase();
        list = list.filter((r) => r.content.toLowerCase().includes(k) || r.hint.toLowerCase().includes(k));
    }
    if (type)
        list = list.filter((r) => (r.type || '').toUpperCase() === type.toUpperCase());
    if (answer)
        list = list.filter((r) => (r.answer || '').toUpperCase() === answer.toUpperCase());
    if (difficulty)
        list = list.filter((r) => (r.level || '').toUpperCase() === difficulty.toUpperCase());
    return list;
}
const defaultSettings = {
    lowRiskThreshold: 30,
    mediumRiskThreshold: 60,
    highRiskThreshold: 90,
    expPerTraining: 10,
    expPerCorrect: 5,
    expPerAssessment: 20,
};
export function attachDemoRoutes(app) {
    const cases = casesJson;
    const knowledge = knowledgeJson;
    const questions = questionsJson;
    const carousel = [...carouselJson];
    const achievements = [...achievementsJson];
    const adminUsers = [...adminUsersJson];
    const adminCaseRows = cases.map(toAdminCaseRow);
    /** 前台轮播（知识页） */
    app.get('/api/carousel', (_req, res) => {
        res.json(ok(carousel));
    });
    app.get('/api/cases', (_req, res) => {
        res.json(ok(cases));
    });
    app.get('/api/knowledge', (req, res) => {
        const { category, q } = req.query;
        let data = knowledge;
        if (category)
            data = data.filter((k) => k.category === category);
        if (q)
            data = data.filter((k) => k.title.includes(q) || k.summary.includes(q));
        res.json(ok(data));
    });
    app.get('/api/knowledge/progress/:userId', (_req, res) => {
        res.json(ok({ finishedArticleIds: [1] }));
    });
    app.get('/api/knowledge/:id', (req, res) => {
        const id = Number(req.params.id);
        const row = knowledge.find((k) => k.id === id);
        if (!row) {
            res.status(404).json(fail(404, '知识不存在'));
            return;
        }
        res.json(ok(row));
    });
    app.post('/api/knowledge/:id/learn', (_req, res) => {
        res.json(ok(true));
    });
    app.get('/api/questions', (_req, res) => {
        res.json(ok(mapQuestionsForClient(questions)));
    });
    app.post('/api/assessment', (req, res) => {
        try {
            const report = evaluateAssessment(questions, req.body);
            res.json(ok(report));
        }
        catch (e) {
            res.status(400).json(fail(400, e?.message || '参数错误'));
        }
    });
    app.post('/api/train/records', (_req, res) => {
        res.json(ok(null));
    });
    app.post('/api/users/register', (req, res) => {
        const { username, password, nickname } = req.body;
        if (!username?.trim()) {
            res.status(400).json(fail(400, '用户名不能为空'));
            return;
        }
        if (registeredUsers.has(username) || adminUsers.some((u) => u.username === username)) {
            res.status(400).json(fail(1001, '用户名已存在'));
            return;
        }
        const id = nextUserId++;
        registeredUsers.set(username, {
            id,
            password: String(password || ''),
            nickname: nickname || username,
        });
        res.json(ok(id));
    });
    app.post('/api/users/login', (req, res) => {
        const { username, password } = req.body;
        if (!username?.trim()) {
            res.status(400).json(fail(400, '用户名不能为空'));
            return;
        }
        const reg = registeredUsers.get(username);
        if (reg) {
            if (reg.password !== String(password ?? '')) {
                res.status(400).json(fail(1003, '密码错误'));
                return;
            }
            res.json(ok({ userId: reg.id, username }));
            return;
        }
        const builtin = adminUsers.find((u) => u.username === username);
        if (builtin) {
            if (String(password ?? '') !== MOCK_DEMO_PASSWORD) {
                res.status(400).json(fail(1003, '密码错误'));
                return;
            }
            res.json(ok({ userId: builtin.id, username: builtin.username }));
            return;
        }
        res.status(400).json(fail(1002, '用户不存在'));
    });
    app.get('/api/users/:id', (req, res) => {
        const id = Number(req.params.id);
        const fromAdmin = adminUsers.find((u) => u.id === id);
        if (fromAdmin) {
            res.json(ok({
                id,
                username: fromAdmin.username,
                nickname: fromAdmin.nickname || fromAdmin.username,
                avatar: fromAdmin.avatar ?? null,
                riskLevel: fromAdmin.riskLevel || 'LOW',
            }));
            return;
        }
        const regEntry = [...registeredUsers.entries()].find(([, v]) => v.id === id);
        if (regEntry) {
            const [username, u] = regEntry;
            res.json(ok({
                id,
                username,
                nickname: u.nickname || username,
                avatar: null,
                riskLevel: 'LOW',
            }));
            return;
        }
        res.status(404).json(fail(404, '用户不存在'));
    });
    app.put('/api/users/:id/avatar', (_req, res) => {
        res.json(ok(null));
    });
    app.put('/api/users/:id/info', (_req, res) => {
        res.json(ok(null));
    });
    app.put('/api/users/:id/password', (_req, res) => {
        res.json(ok(null));
    });
    /* ---------- 管理端：统计 ---------- */
    app.get('/api/admin/statistics/users', (_req, res) => {
        res.json(ok({ total: adminUsers.length + registeredUsers.size }));
    });
    app.get('/api/admin/statistics/risk-distribution', (_req, res) => {
        res.json(ok({
            data: [
                { name: '低风险', value: 42 },
                { name: '中风险', value: 28 },
                { name: '高风险', value: 15 },
            ],
        }));
    });
    app.get('/api/admin/statistics/active-trend', (_req, res) => {
        const data = [6, 5, 8, 12, 9, 14, 11].map((count, i) => ({
            date: `2026-04-${String(24 + i).padStart(2, '0')}`,
            count,
        }));
        res.json(ok({ data }));
    });
    /* ---------- 管理端：用户 ---------- */
    app.get('/api/admin/users', (req, res) => {
        const page = Number(req.query.page) || 1;
        const size = Number(req.query.size) || 10;
        const merged = [
            ...adminUsers,
            ...[...registeredUsers.entries()].map(([username, u]) => ({
                id: u.id,
                username,
                nickname: u.nickname,
                email: `${username}@mock.local`,
                avatar: null,
                level: 1,
                exp: 0,
                riskLevel: 'LOW',
                createdAt: new Date().toISOString(),
            })),
        ];
        const { content, total } = paginate(merged, page, size);
        res.json(ok({ data: { content, total } }));
    });
    app.get('/api/admin/users/:id/report', (_req, res) => {
        res.json(ok({
            trainingCount: 12,
            correctCount: 9,
            avgTimeSpent: 18500,
            assessmentCount: 2,
            lastAssessmentTime: new Date().toISOString(),
            infoScore: 35,
            financeScore: 42,
            psychScore: 28,
            totalScore: 38,
            achievementCount: 2,
        }));
    });
    /* ---------- 管理端：案例 ---------- */
    app.get('/api/admin/cases', (req, res) => {
        const page = Number(req.query.page) || 1;
        const size = Number(req.query.size) || 10;
        const filtered = filterCases(adminCaseRows, req);
        const { content, total } = paginate(filtered, page, size);
        res.json(ok({ data: { content, total } }));
    });
    app.post('/api/admin/cases', (_req, res) => {
        res.json(ok({ id: nextUserId++ }));
    });
    app.put('/api/admin/cases/:id', (_req, res) => {
        res.json(ok(null));
    });
    app.delete('/api/admin/cases/:id', (_req, res) => {
        res.json(ok(null));
    });
    app.delete('/api/admin/cases/batch', (_req, res) => {
        res.json(ok(null));
    });
    /* ---------- 管理端：知识 ---------- */
    app.get('/api/admin/knowledge', (req, res) => {
        const page = Number(req.query.page) || 1;
        const size = Number(req.query.size) || 10;
        const rows = knowledge.map(toAdminKnowledgeRow);
        const { content, total } = paginate(rows, page, size);
        res.json(ok({ data: { content, total } }));
    });
    app.post('/api/admin/knowledge', (_req, res) => {
        res.json(ok({ id: nextUserId++ }));
    });
    app.put('/api/admin/knowledge/:id', (_req, res) => {
        res.json(ok(null));
    });
    app.delete('/api/admin/knowledge/:id', (_req, res) => {
        res.json(ok(null));
    });
    app.delete('/api/admin/knowledge/batch', (_req, res) => {
        res.json(ok(null));
    });
    /* ---------- 管理端：测评题库 ---------- */
    app.get('/api/admin/assessment/questions', (req, res) => {
        const page = Number(req.query.page) || 1;
        const size = Number(req.query.size) || 10;
        const rows = questions.map(toAdminRiskQuestionRow);
        const { content, total } = paginate(rows, page, size);
        res.json(ok({ data: { content, total } }));
    });
    app.post('/api/admin/assessment/questions', (_req, res) => {
        res.json(ok({ id: nextUserId++ }));
    });
    app.put('/api/admin/assessment/questions/:id', (_req, res) => {
        res.json(ok(null));
    });
    app.delete('/api/admin/assessment/questions/:id', (_req, res) => {
        res.json(ok(null));
    });
    app.delete('/api/admin/assessment/questions/batch', (_req, res) => {
        res.json(ok(null));
    });
    /* ---------- 管理端：训练题（演示返回空列表，避免与测评题库字段混淆） ---------- */
    app.get('/api/admin/training/questions', (_req, res) => {
        res.json(ok({ data: { content: [], total: 0 } }));
    });
    app.post('/api/admin/training/questions', (_req, res) => {
        res.json(ok({ id: nextUserId++ }));
    });
    app.put('/api/admin/training/questions/:id', (_req, res) => {
        res.json(ok(null));
    });
    app.delete('/api/admin/training/questions/:id', (_req, res) => {
        res.json(ok(null));
    });
    app.delete('/api/admin/training/questions/batch', (_req, res) => {
        res.json(ok(null));
    });
    /* ---------- 管理端：成就 ---------- */
    app.get('/api/admin/achievements', (req, res) => {
        const page = Number(req.query.page) || 1;
        const size = Number(req.query.size) || 10;
        const { content, total } = paginate(achievements, page, size);
        res.json(ok({ data: { content, total } }));
    });
    app.post('/api/admin/achievements', (_req, res) => {
        res.json(ok({ id: nextUserId++ }));
    });
    app.put('/api/admin/achievements/:id', (_req, res) => {
        res.json(ok(null));
    });
    app.delete('/api/admin/achievements/:id', (_req, res) => {
        res.json(ok(null));
    });
    app.delete('/api/admin/achievements/batch', (_req, res) => {
        res.json(ok(null));
    });
    /* ---------- 管理端：轮播 ---------- */
    app.get('/api/admin/carousel', (_req, res) => {
        res.json(ok(carousel));
    });
    app.post('/api/admin/carousel', (req, res) => {
        const body = req.body;
        const id = Math.max(0, ...carousel.map((c) => c.id || 0)) + 1;
        carousel.push({
            id,
            imageUrl: body.imageUrl || '',
            title: body.title || `轮播图 ${id}`,
            linkUrl: body.linkUrl || '',
            sortOrder: body.sortOrder ?? carousel.length,
            enableFlag: body.enableFlag ?? 1,
        });
        res.json(ok(id));
    });
    app.put('/api/admin/carousel/:id', (req, res) => {
        const id = Number(req.params.id);
        const idx = carousel.findIndex((c) => c.id === id);
        if (idx >= 0)
            Object.assign(carousel[idx], req.body);
        res.json(ok(null));
    });
    app.delete('/api/admin/carousel/:id', (req, res) => {
        const id = Number(req.params.id);
        const idx = carousel.findIndex((c) => c.id === id);
        if (idx >= 0)
            carousel.splice(idx, 1);
        res.json(ok(null));
    });
    app.post('/api/admin/carousel/upload', (_req, res) => {
        res.json(ok({ url: `https://picsum.photos/seed/up${Date.now()}/800/400` }));
    });
    app.post('/api/admin/carousel/upload/batch', (_req, res) => {
        res.json(ok([`https://picsum.photos/seed/b1/800/400`, `https://picsum.photos/seed/b2/800/400`]));
    });
    /* ---------- 管理端：系统设置 ---------- */
    app.get('/api/admin/settings', (_req, res) => {
        res.json(ok({ ...defaultSettings }));
    });
    app.put('/api/admin/settings', (_req, res) => {
        res.json(ok(null));
    });
}
