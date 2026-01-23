import type { RiskLevel } from '../stores/user';

export type DimensionKey = 'info' | 'finance' | 'psych';

export interface RiskQuestion {
  id: number;
  dimension: DimensionKey;
  weight: number;
  options: { value: number }[];
}

export interface RiskDimensionScores {
  info: number;
  finance: number;
  psych: number;
}

export interface RiskWeakness {
  dimension: DimensionKey;
  score: number;
  reason: string;
}

export interface RiskResult {
  score: number;
  level: RiskLevel;
  dimensions: RiskDimensionScores;
  weaknesses: RiskWeakness[];
}

// 简单的多维度加权风险评估算法
export type RiskAnswerValue = number | number[];

export function evaluateRisk(answers: Record<number, RiskAnswerValue>, questions: RiskQuestion[]): RiskResult {
  const dimRaw: Record<DimensionKey, number> = {
    info: 0,
    finance: 0,
    psych: 0,
  };
  const dimMax: Record<DimensionKey, number> = {
    info: 0,
    finance: 0,
    psych: 0,
  };

  questions.forEach((q) => {
    const rawAnswer = answers[q.id];
    const value = Array.isArray(rawAnswer) ? rawAnswer.reduce((a, b) => a + b, 0) : rawAnswer ?? 0;
    const maxOpt = q.options.reduce((m, o) => Math.max(m, o.value), 0);
    dimRaw[q.dimension] += value * q.weight;
    dimMax[q.dimension] += maxOpt * q.weight;
  });

  const dimScores: RiskDimensionScores = {
    info: dimMax.info ? Math.round((dimRaw.info / dimMax.info) * 100) : 0,
    finance: dimMax.finance ? Math.round((dimRaw.finance / dimMax.finance) * 100) : 0,
    psych: dimMax.psych ? Math.round((dimRaw.psych / dimMax.psych) * 100) : 0,
  };

  const score = Math.round((dimScores.info + dimScores.finance + dimScores.psych) / 3);

  let level: RiskLevel = 'low';
  if (score >= 70) level = 'high';
  else if (score >= 40) level = 'medium';

  const weaknesses: RiskWeakness[] = (Object.keys(dimScores) as DimensionKey[])
    .sort((a, b) => dimScores[a] - dimScores[b])
    .slice(0, 2)
    .map((key) => ({
      dimension: key,
      score: dimScores[key],
      reason:
        key === 'info'
          ? '信息防护习惯较弱，易受钓鱼链接、验证码泄露等攻击。'
          : key === 'finance'
          ? '金融安全意识有待加强，容易被高收益诱惑。'
          : '心理防线较弱，容易在恐吓或甜言蜜语下做出冲动决策。',
    }));

  return {
    score,
    level,
    dimensions: dimScores,
    weaknesses,
  };
}

