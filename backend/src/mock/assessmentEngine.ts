type Q = {
  id: number;
  text: string;
  dimension: string;
  weight: number;
  type: string;
  options: { label: string; value: number }[];
};

type AnswerItem = { questionId: number; optionIds: number[] };

const DIM_NAMES: Record<string, string> = {
  info: '信息防护',
  finance: '金融安全',
  psych: '心理防线',
};

function round2(n: number) {
  return Math.round(n * 100) / 100;
}

function toRiskLevel(score: number): 'low' | 'medium' | 'high' {
  if (score < 40) return 'low';
  if (score < 70) return 'medium';
  return 'high';
}

function optionsWithIds(q: Q) {
  return q.options.map((opt, idx) => ({ id: idx + 1, label: opt.label, value: opt.value }));
}

function getOptionValue(q: Q, optionId: number): number {
  const opts = optionsWithIds(q);
  const hit = opts.find((o) => o.id === optionId);
  return hit ? hit.value : 0;
}

function maxOptionSum(q: Q): number {
  const vals = q.options.map((o) => o.value);
  if ((q.type || '').toLowerCase() === 'multi') {
    return vals.filter((v) => v > 0).reduce((a, b) => a + b, 0);
  }
  return Math.max(0, ...vals);
}

function buildSuggestions(dim: string, score: number): string[] {
  const out: string[] = [];
  if (dim === 'info' && score >= 55) {
    out.push('对陌生链接与附件保持警惕，重要操作请通过官方 App 核实。');
  }
  if (dim === 'finance' && score >= 55) {
    out.push('遇到高收益、稳赚不赔话术时先暂停转账，向家人或官方渠道二次确认。');
  }
  if (dim === 'psych' && score >= 55) {
    out.push('在催促、恐吓情境下刻意放慢节奏，避免冲动决策。');
  }
  if (out.length === 0) {
    out.push('继续保持良好习惯：核验身份、保护验证码、不轻信转账要求。');
  }
  return out;
}

export function mapQuestionsForClient(questions: Q[]) {
  return questions.map((q) => ({
    ...q,
    options: optionsWithIds(q),
  }));
}

export function evaluateAssessment(questions: Q[], body: { userId?: number; answers?: AnswerItem[] }) {
  const answers = body?.answers;
  if (!answers?.length) {
    throw new Error('answers is required');
  }

  const qMap = new Map(questions.map((q) => [q.id, q]));
  const dims = ['info', 'finance', 'psych'] as const;
  const actual: Record<string, number> = { info: 0, finance: 0, psych: 0 };
  const max: Record<string, number> = { info: 0, finance: 0, psych: 0 };

  const questionIds = [...new Set(answers.map((a) => a.questionId))];
  for (const qid of questionIds) {
    const q = qMap.get(qid);
    if (!q) continue;
    const d = String(q.dimension || 'info').toLowerCase();
    if (!(d in max)) continue;
    max[d] += maxOptionSum(q) * q.weight;
  }

  for (const a of answers) {
    const q = qMap.get(a.questionId);
    if (!q) continue;
    const d = String(q.dimension || 'info').toLowerCase();
    if (!(d in actual)) continue;
    let sum = 0;
    for (const oid of a.optionIds || []) {
      sum += getOptionValue(q, oid);
    }
    actual[d] += sum * q.weight;
  }

  const normalized: Record<string, number> = {};
  for (const d of dims) {
    normalized[d] = max[d] <= 0 ? 0 : round2((actual[d] / max[d]) * 100);
  }

  const total = round2((normalized.info + normalized.finance + normalized.psych) / 3);
  const level = toRiskLevel(total);

  const dimensions = dims.map((d) => {
    const s = normalized[d];
    return {
      dimension: d,
      name: DIM_NAMES[d] || d,
      score: s,
      level: toRiskLevel(s),
    };
  });

  let explanation = '本次测评从信息保护、金融安全、心理风险三大维度进行综合评估：';
  for (const dim of dimensions) {
    explanation += `${dim.name}维度得分为 ${dim.score} 分（等级：${dim.level}）；`;
  }

  const suggestions = Array.from(
    new Set(dims.flatMap((d) => buildSuggestions(d, normalized[d]))),
  );

  return {
    score: total,
    level,
    explanation,
    dimensions,
    suggestions,
    createdAt: new Date().toISOString(),
  };
}
