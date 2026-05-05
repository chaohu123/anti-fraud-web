import express from 'express';
import cors from 'cors';
import morgan from 'morgan';
import casesJson from './data/cases.json' with { type: 'json' };
import knowledgeJson from './data/knowledge.json' with { type: 'json' };
import questionsJson from './data/questions.json' with { type: 'json' };

const app = express();
app.use(cors());
app.use(express.json());
app.use(morgan('dev'));

type CaseItem = {
  id: number;
  type: 'sms' | 'email' | 'audio' | 'site';
  content: string;
  hint: string;
  suspiciousPoints: string[];
  level: 'easy' | 'medium' | 'hard';
  mediaUrl?: string;
  answer: 'fraud' | 'safe';
};

const cases: CaseItem[] = casesJson as CaseItem[];

type KnowledgeItem = {
  id: number;
  category: string;
  title: string;
  summary: string;
};

const knowledge: KnowledgeItem[] = knowledgeJson as KnowledgeItem[];

type QuestionItem = {
  id: number;
  text: string;
  dimension: 'info' | 'finance' | 'psych';
  weight: number;
  type: 'single' | 'multi';
  options: { label: string; value: number }[];
};

const questions: QuestionItem[] = questionsJson as QuestionItem[];

app.get('/api/cases', (_req, res) => {
  res.json(cases);
});

type AssessmentAnswers = Record<string, number>;

type DimensionKey = QuestionItem['dimension'];

type DimensionStat = {
  actual: number;
  max: number;
};

app.post('/api/assessment', (req, res) => {
  const { answers } = req.body as { answers?: AssessmentAnswers };

  if (!answers || typeof answers !== 'object' || Array.isArray(answers)) {
    return res.status(400).json({ message: 'answers is required' });
  }

  const entries = Object.entries(answers);
  if (!entries.length) {
    return res.status(400).json({ message: 'answers is required' });
  }

  const stats: Record<DimensionKey, DimensionStat> = {
    info: { actual: 0, max: 0 },
    finance: { actual: 0, max: 0 },
    psych: { actual: 0, max: 0 },
  };

  for (const [idStr, value] of entries) {
    const id = Number(idStr);
    if (!Number.isFinite(id)) continue;
    if (typeof value !== 'number' || value < 0) continue;

    const q = questions.find((item) => item.id === id);
    if (!q) continue;

    // 题目理论最大风险值：
    // - 单选：取 options 中最大的 value
    // - 多选：取所有正值之和（假设可多选全选）
    const optionValues = q.options.map((o) => o.value);
    const maxPerQuestion =
      q.type === 'single'
        ? Math.max(...optionValues, 0)
        : optionValues.filter((v) => v > 0).reduce((sum, v) => sum + v, 0);

    if (maxPerQuestion <= 0) continue;

    const capped = Math.min(value, maxPerQuestion);

    const dimension = q.dimension;
    stats[dimension].actual += capped * q.weight;
    stats[dimension].max += maxPerQuestion * q.weight;
  }

  const anyValidDimension = Object.values(stats).some((s) => s.max > 0);
  if (!anyValidDimension) {
    return res.status(400).json({ message: 'answers is invalid' });
  }

  const dimensionScores: Record<DimensionKey, number> = {
    info:
      stats.info.max > 0
        ? Math.round((stats.info.actual / stats.info.max) * 100)
        : 0,
    finance:
      stats.finance.max > 0
        ? Math.round((stats.finance.actual / stats.finance.max) * 100)
        : 0,
    psych:
      stats.psych.max > 0
        ? Math.round((stats.psych.actual / stats.psych.max) * 100)
        : 0,
  };

  const overallScore = Math.round(
    (dimensionScores.info + dimensionScores.finance + dimensionScores.psych) / 3,
  );

  const score = Math.max(0, Math.min(100, overallScore));

  const level = score >= 70 ? 'high' : score >= 40 ? 'medium' : 'low';

  res.json({
    score,
    level,
    dimensions: dimensionScores,
    generatedAt: new Date().toISOString(),
  });
});

app.get('/api/knowledge', (req, res) => {
  const { category, q } = req.query as { category?: string; q?: string };
  let data = knowledge;
  if (category) {
    data = data.filter((k) => k.category === category);
  }
  if (q) {
    data = data.filter((k) => k.title.includes(q) || k.summary.includes(q));
  }
  res.json(data);
});

app.get('/api/questions', (_req, res) => {
  // 为每个选项添加 id，前端需要 id 字段来标识选项
  const questionsWithOptionIds = questions.map((q) => ({
    ...q,
    options: q.options.map((opt, idx) => ({
      id: idx + 1, // 使用索引+1作为选项id
      label: opt.label,
      value: opt.value,
    })),
  }));
  res.json(questionsWithOptionIds);
});

app.post('/api/admin/cases', (req, res) => {
  const payload = req.body as CaseItem;
  cases.push({ ...payload, id: cases.length + 1 });
  res.status(201).json({ ok: true });
});

const port = process.env.PORT || 3000;
app.listen(port, () => {
  console.log(`backend listening on ${port}`);
});
