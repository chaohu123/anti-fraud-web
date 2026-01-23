<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElNotification } from 'element-plus';
import { ArrowLeft, ArrowRight, Clock, Link, Microphone, Message, Picture, WarningFilled, Lock, Trophy, Star, Reading } from '@element-plus/icons-vue';
import http from '../api/http';
import { useUserStore } from '../stores/user';
import { useAchievementStore } from '../store/achievement';
import { loadJson, saveJson } from '../utils/storage';

type Difficulty = 'easy' | 'medium' | 'hard';
type ScenarioType = 'chat' | 'site' | 'audio';
type Judgement = 'fraud' | 'safe';

type ScenarioChat = {
  kind: 'chat';
  channel: 'sms' | 'im';
  from: string;
  messages: Array<{ from: 'scammer' | 'user'; text: string; at?: string }>;
};

type ScenarioSite = {
  kind: 'site';
  title: string;
  subtitle?: string;
  imageUrl?: string;
  // 不做可点击链接，仅用于“可疑点”提示展示
  highlightedText?: string;
};

type ScenarioAudio = {
  kind: 'audio';
  title: string;
  audioUrl?: string;
  transcript: string;
};

type TrainingCase = {
  id: number;
  scamType: string; // 冒充客服/刷流水/刷单返利/冒充公检法...
  difficulty: Difficulty;
  answer: Judgement;
  question: string;
  // 可供勾选的“特征选项”（可包含干扰项）
  options: string[];
  // 正确可疑点（用于解析命中/漏选）
  suspiciousPoints: string[];
  analysis: string;
  advice: string[];
  scenario: ScenarioChat | ScenarioSite | ScenarioAudio;
};

type TrainRecord = {
  at: string;
  caseId: number;
  scamType: string;
  correct: boolean;
  userJudgement: Judgement;
  timeSpentMs: number;
  chosenOptions: string[];
  missedPoints: string[];
};

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const achievementStore = useAchievementStore();

const TOTAL_TIME = 30;

// 步骤状态
const currentStep = computed(() => {
  if (!userJudgement.value) return 1;
  if (chosen.value.length === 0) return 2;
  return 3;
});

// 可疑特征区域是否展开（当选择"是诈骗"时自动展开）
const featurePanelExpanded = ref(false);
const featurePanelHighlight = ref(false);

// 动态验证状态
const validationError = ref<{ type: 'judgement' | 'feature' | null; message: string }>({ type: null, message: '' });

// 成就相关
const potentialAchievements = ref<Array<{ id: string; name: string; progress: number }>>([]);
const newAchievementUnlocked = ref<string | null>(null);
const showAchievementCelebration = ref(false);

// 反馈详细解析是否展开
const detailedAnalysisVisible = ref(false);

const loading = ref(false);
const cases = ref<TrainingCase[]>([]);
const currentIndex = ref(0);

const timeLeft = ref(TOTAL_TIME);
const timer = ref<number | null>(null);
const startedAt = ref<number>(Date.now());

const userJudgement = ref<Judgement | null>(null);
const chosen = ref<string[]>([]);

const feedbackVisible = ref(false);
const lastResult = ref<{
  correct: boolean;
  missedPoints: string[];
  hitPoints: string[];
  timeSpentMs: number;
} | null>(null);

const total = computed(() => cases.value.length || 10);
const currentCase = computed(() => cases.value[currentIndex.value] || null);

const progressText = computed(() => `第 ${Math.min(currentIndex.value + 1, total.value)} / ${total.value} 关`);
const difficultyStars = computed(() => {
  const lv = currentCase.value?.difficulty ?? 'easy';
  return lv === 'easy' ? 1 : lv === 'medium' ? 2 : 3;
});
const difficultyLabel = computed(() => {
  const lv = currentCase.value?.difficulty ?? 'easy';
  return lv === 'easy' ? '简单' : lv === 'medium' ? '中等' : '困难';
});
const difficultyTagType = computed(() => {
  const lv = currentCase.value?.difficulty ?? 'easy';
  return lv === 'easy' ? 'success' : lv === 'medium' ? 'warning' : 'danger';
});

const timerPercent = computed(() => Math.round((timeLeft.value / TOTAL_TIME) * 100));
const isTimeCritical = computed(() => timeLeft.value <= 10);
const isTimeVeryCritical = computed(() => timeLeft.value <= 5);

// 倒计时颜色
const timerColor = computed(() => {
  if (timeLeft.value <= 5) return '#f56c6c';
  if (timeLeft.value <= 10) return '#e6a23c';
  if (timeLeft.value <= 20) return '#409eff';
  return '#67c23a';
});

const canSubmit = computed(() => !!currentCase.value && !!userJudgement.value && chosen.value.length > 0 && timeLeft.value > 0);

const scenarioIcon = computed(() => {
  const k = currentCase.value?.scenario.kind;
  if (k === 'chat') return Message;
  if (k === 'site') return Picture;
  return Microphone;
});

function escapeHtml(input: string) {
  return input
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function highlightText(raw: string, highlightSuspicious = false) {
  const safe = escapeHtml(raw);
  // URL、验证码、金额、紧急话术等：高亮但不可点击
  const patterns: Array<{ re: RegExp; cls: string }> = [
    { re: /\bhttps?:\/\/[^\s)]+/gi, cls: 'hl-link' },
    { re: /\b\d{4,6}\b/g, cls: 'hl-code' },
    { re: /(\b\d{2,}\s*(?:元|rmb|cny|￥)\b|￥\s*\d{2,})/gi, cls: 'hl-money' },
    { re: /(立刻|马上|紧急|否则|将冻结|最后通牒|限时|立即处理)/g, cls: 'hl-urgent' },
    { re: /(转账|汇款|刷流水|验证码|链接|点击|下载|远程|屏幕共享)/g, cls: 'hl-key' },
  ];
  let result = patterns.reduce((acc, p) => acc.replace(p.re, (m) => `<span class="hl ${p.cls}">${m}</span>`), safe);
  
  // 如果是在反馈模式下，高亮可疑点
  if (highlightSuspicious && currentCase.value && lastResult.value) {
    const suspiciousPoints = currentCase.value.suspiciousPoints;
    suspiciousPoints.forEach(point => {
      // 根据特征点高亮相关内容
      if (point.includes('域名') || point.includes('链接')) {
        result = result.replace(/(https?:\/\/[^\s)]+)/gi, (match) => {
          if (!match.includes('hl-link')) {
            return `<span class="hl hl-link hl-suspicious" data-tooltip="非官方域名，真正的官网域名应为官方渠道">${match}</span>`;
          }
          return match.replace('hl-link', 'hl-link hl-suspicious');
        });
      }
      if (point.includes('恐吓') || point.includes('紧急')) {
        result = result.replace(/(立刻|马上|紧急|否则|将冻结|最后通牒|限时|立即处理)/g, (match) => {
          if (!match.includes('hl-urgent')) {
            return `<span class="hl hl-urgent hl-suspicious" data-tooltip="这是典型的利用恐惧和紧迫感的恐吓话术">${match}</span>`;
          }
          return match.replace('hl-urgent', 'hl-urgent hl-suspicious');
        });
      }
    });
  }
  
  return result;
}

function stopTimer() {
  if (timer.value) window.clearInterval(timer.value);
  timer.value = null;
}

function startTimer() {
  stopTimer();
  timeLeft.value = TOTAL_TIME;
  startedAt.value = Date.now();
  timer.value = window.setInterval(() => {
    timeLeft.value -= 1;
    if (timeLeft.value <= 0) {
      timeLeft.value = 0;
      stopTimer();
      ElMessage.warning('本关倒计时结束，可点击“下一关”继续训练。');
    }
  }, 1000);
}

function resetInteraction() {
  userJudgement.value = null;
  chosen.value = [];
  feedbackVisible.value = false;
  lastResult.value = null;
  featurePanelExpanded.value = false;
  featurePanelHighlight.value = false;
  validationError.value = { type: null, message: '' };
  detailedAnalysisVisible.value = false;
}

function loadLocalRecords(): TrainRecord[] {
  return loadJson<TrainRecord[]>('af_training_records', []);
}

function appendLocalRecord(record: TrainRecord) {
  const list = loadLocalRecords();
  list.unshift(record);
  saveJson('af_training_records', list.slice(0, 200));
}

function normalizeFromBackend(item: any, idx: number): TrainingCase {
  // 兼容后端/旧页面字段：type/content/hint/answer/level/suspiciousPoints/mediaUrl
  const fallbackId = Number(item?.id ?? idx + 1);
  const answer: Judgement = item?.answer === 'safe' || item?.answer === 'SAFE' ? 'safe' : 'fraud';
  const difficulty: Difficulty =
    item?.level === 'hard' || item?.level === 'HARD'
      ? 'hard'
      : item?.level === 'medium' || item?.level === 'MEDIUM'
        ? 'medium'
        : 'easy';

  const suspiciousPoints: string[] = Array.isArray(item?.suspiciousPoints) ? item.suspiciousPoints : [];
  const options = suspiciousPoints.length
    ? suspiciousPoints
    : ['诱导转账/汇款', '可疑链接/非官方域名', '索要验证码/短信码', '紧急催促/恐吓话术', '要求下载APP/附件', '索要银行卡/身份信息'];

  const type = (item?.type || 'sms') as string;
  const content = String(item?.content ?? '');
  const hint = String(item?.hint ?? '注意识别：冒充身份、诱导转账、钓鱼链接、索要验证码等典型套路。');

  // 基于 type 映射到三种展示：chat/site/audio（email 也按 chat 展示）
  const scenario: TrainingCase['scenario'] =
    type === 'site'
      ? {
          kind: 'site',
          title: '疑似钓鱼网站页面',
          subtitle: '请观察域名、提示语、输入框与引导动作',
          imageUrl: item?.mediaUrl ? String(item.mediaUrl) : undefined,
          highlightedText: content,
        }
      : type === 'audio'
        ? {
            kind: 'audio',
            title: '诈骗语音话术',
            audioUrl: item?.mediaUrl ? String(item.mediaUrl) : undefined,
            transcript: content,
          }
        : {
            kind: 'chat',
            channel: type === 'sms' ? 'sms' : 'im',
            from: type === 'email' ? '邮件通知' : '陌生联系人',
            messages: [{ from: 'scammer', text: content }],
          };

  return {
    id: fallbackId,
    scamType: type === 'site' ? '钓鱼网站' : type === 'audio' ? '诈骗语音' : type === 'email' ? '钓鱼邮件' : '短信/聊天诈骗',
    difficulty,
    answer,
    question: '你认为这是一条诈骗信息吗？',
    options,
    suspiciousPoints,
    analysis: hint,
    advice: [
      '不点击陌生链接，不下载不明附件/APP。',
      '不向任何人提供验证码、密码、银行卡信息。',
      '通过官方渠道核实（App/官网/客服电话），不要回拨对方提供的号码。',
      '遇到转账要求，先冷静并向家人/平台/警方咨询。',
    ],
    scenario,
  };
}

const builtinCases: TrainingCase[] = [
  {
    id: 1,
    scamType: '冒充客服（退款理赔）',
    difficulty: 'easy',
    answer: 'fraud',
    question: '你认为这是一条诈骗信息吗？',
    options: ['诱导转账/汇款', '发送可疑链接', '索要验证码', '紧急催促', '声称“退款/理赔”', '要求屏幕共享/远程控制'],
    suspiciousPoints: ['发送可疑链接', '索要验证码', '声称“退款/理赔”', '紧急催促'],
    analysis: '典型“冒充平台客服退款”套路：先制造订单/理赔场景，再引导点击链接或提供验证码，最终盗取账号或引导转账。',
    advice: ['退款只在原支付渠道完成，不添加私人客服号。', '任何索要验证码/屏幕共享的“客服”都高度可疑。', '用官方 App/官网入口自行查询订单与售后。'],
    scenario: {
      kind: 'chat',
      channel: 'im',
      from: '平台客服-售后中心',
      messages: [
        { from: 'scammer', text: '您好，这里是平台售后中心。检测到您的订单异常，将为您办理退款。' },
        { from: 'scammer', text: '请点击 https://service-refund.example.com 进入退款通道，输入验证码完成认证。否则将影响征信。' },
      ],
    },
  },
  {
    id: 2,
    scamType: '冒充银行（账户冻结）',
    difficulty: 'easy',
    answer: 'fraud',
    question: '你认为这是一条诈骗信息吗？',
    options: ['可疑短链/非官方域名', '以冻结账号恐吓', '要求立刻操作', '诱导输入卡号/密码', '诱导下载App'],
    suspiciousPoints: ['可疑短链/非官方域名', '以冻结账号恐吓', '要求立刻操作'],
    analysis: '诈骗常用“恐吓+限时”让人失去判断力。银行不会通过短信链接要求你验证或输入敏感信息。',
    advice: ['不要点击短信链接，直接打开银行官方 App 核实。', '任何“冻结/征信”威胁都先冷静核验。'],
    scenario: {
      kind: 'chat',
      channel: 'sms',
      from: '【XX银行】',
      messages: [{ from: 'scammer', text: '【XX银行】您的账户存在风险，需立即验证 https://bank-safe.example.cn 否则将冻结账号。' }],
    },
  },
  {
    id: 3,
    scamType: '钓鱼网站（登录盗号）',
    difficulty: 'medium',
    answer: 'fraud',
    question: '你认为这是一条诈骗信息吗？',
    options: ['域名可疑/拼写相近', '引导输入账号密码', '页面提示异常紧急', '诱导下载/安装插件', '客服入口异常'],
    suspiciousPoints: ['域名可疑/拼写相近', '引导输入账号密码', '页面提示异常紧急'],
    analysis: '钓鱼站常通过“仿真页面 + 相近域名”诱导输入账号密码，再利用短信验证码完成盗号。',
    advice: ['核对域名与证书，尽量使用收藏夹/官方入口。', '不要在陌生页面输入账号密码或验证码。'],
    scenario: {
      kind: 'site',
      title: '账号安全验证页（截图模拟）',
      subtitle: '提示：链接/关键词仅高亮不可点击',
      highlightedText: '登录异常，请立即验证账号。访问 https://pay-login.example.com 输入账号/密码完成验证。',
    },
  },
  {
    id: 4,
    scamType: '诈骗语音（冒充公检法）',
    difficulty: 'hard',
    answer: 'fraud',
    question: '你认为这是一条诈骗信息吗？',
    options: ['冒充公检法/权威身份', '诱导转入“安全账户”', '要求保密', '紧急威胁', '引导下载远程会议软件'],
    suspiciousPoints: ['冒充公检法/权威身份', '诱导转入“安全账户”', '要求保密', '紧急威胁'],
    analysis: '“公检法”不会通过电话要求转账到所谓“安全账户”，更不会要求你对家人保密。',
    advice: ['挂断后拨打 110 或官方电话核实，不要回拨对方号码。', '涉及资金操作一律先线下核验。'],
    scenario: {
      kind: 'audio',
      title: '来电录音（模拟）',
      transcript:
        '我们是某市公安局，您名下银行卡涉嫌洗钱。请立即配合调查，转入安全账户核验资金来源，并且不要告诉任何人，否则将立即冻结资产。',
    },
  },
];

async function fetchCases() {
  loading.value = true;
  try {
    const resp = await http.get('/cases');
    const list = Array.isArray(resp.data) ? resp.data : [];
    cases.value = list.length ? list.map((it, i) => normalizeFromBackend(it, i)) : builtinCases;
  } catch {
    cases.value = builtinCases;
    ElMessage.info('当前使用内置案例（未连接后端也可演示）。');
  } finally {
    loading.value = false;
  }
}

function jumpHome() {
  router.push('/');
}

function applyIndexFromQuery() {
  const raw = route.query.level;
  const idx = typeof raw === 'string' ? Number(raw) - 1 : NaN;
  if (Number.isFinite(idx) && idx >= 0) {
    currentIndex.value = Math.min(idx, Math.max(0, (cases.value.length || total.value) - 1));
  }
}

function computeTimeSpentMs() {
  return Math.max(0, Date.now() - startedAt.value);
}

// 检查并解锁成就
function checkAndUnlockAchievements() {
  if (!achievementStore.achievements.length) {
    achievementStore.initAchievements();
  }
  
  const trainingAchievements = achievementStore.achievementsByCategory('training');
  const userTrainingCount = userStore.trainingCount;
  
  trainingAchievements.forEach(achievement => {
    if (achievement.condition.type === 'training') {
      const progress = Math.min(100, (userTrainingCount / achievement.condition.target) * 100);
      const wasUnlocked = achievementStore.unlockedAchievements.has(achievement.id);
      
      if (progress >= 100 && !wasUnlocked) {
        // 新解锁成就
        achievementStore.unlockAchievement(achievement.id);
        newAchievementUnlocked.value = achievement.id;
        showAchievementCelebration.value = true;
        setTimeout(() => {
          showAchievementCelebration.value = false;
          newAchievementUnlocked.value = null;
        }, 3000);
      }
    }
  });
}

// 更新潜在成就
function updatePotentialAchievements() {
  if (!achievementStore.achievements.length) {
    achievementStore.initAchievements();
  }
  
  const trainingAchievements = achievementStore.achievementsByCategory('training');
  const userTrainingCount = userStore.trainingCount;
  
  potentialAchievements.value = trainingAchievements
    .filter(a => {
      if (achievementStore.unlockedAchievements.has(a.id)) return false;
      if (a.condition.type === 'training') {
        const progress = Math.min(100, (userTrainingCount / a.condition.target) * 100);
        return progress > 0 && progress < 100;
      }
      return false;
    })
    .map(a => ({
      id: a.id,
      name: a.name,
      progress: Math.min(100, (userTrainingCount / a.condition.target) * 100),
    }))
    .slice(0, 2); // 最多显示2个
}

async function submit() {
  const c = currentCase.value;
  if (!c) return;
  if (!userJudgement.value) {
    ElMessage.warning('请先选择“是诈骗 / 非诈骗”。');
    return;
  }
  if (!chosen.value.length) {
    ElMessage.warning('请至少勾选 1 项你认为可疑的特征。');
    return;
  }
  if (timeLeft.value <= 0) {
    ElMessage.warning('本关已超时，请点击“下一关”继续训练。');
    return;
  }

  stopTimer();

  const hitPoints = c.suspiciousPoints.filter((p) => chosen.value.includes(p));
  const missedPoints = c.suspiciousPoints.filter((p) => !chosen.value.includes(p));
  const correct = userJudgement.value === c.answer;
  const spent = computeTimeSpentMs();

  lastResult.value = { correct, missedPoints, hitPoints, timeSpentMs: spent };
  feedbackVisible.value = true;

  // 训练次数：以“完成提交”为准；同时正确则计入徽章逻辑
  if (correct) userStore.recordTraining();

  appendLocalRecord({
    at: new Date().toISOString(),
    caseId: c.id,
    scamType: c.scamType,
    correct,
    userJudgement: userJudgement.value,
    timeSpentMs: spent,
    chosenOptions: [...chosen.value],
    missedPoints,
  });

  // 若登录则上报（不影响体验）
  if (userStore.userId) {
    http
      .post('/train/records', {
        userId: userStore.userId,
        caseId: c.id,
        answer: userJudgement.value === 'fraud' ? 'FRAUD' : 'SAFE',
        correct,
        timeSpentMs: spent,
      })
      .catch(() => {
        ElNotification({
          title: '提示',
          message: '训练记录上报失败（不影响继续训练），请检查后端是否启动。',
          type: 'warning',
          duration: 2500,
        });
      });
  }
}

function nextLevel() {
  feedbackVisible.value = false;
  const next = currentIndex.value + 1;
  currentIndex.value = next >= (cases.value.length || total.value) ? 0 : next;
  resetInteraction();
  startTimer();
  router.replace({ path: '/train', query: { level: String(currentIndex.value + 1) } });
}

// 监听选择"是诈骗"后展开特征区域
watch(userJudgement, (newVal) => {
  if (newVal === 'fraud') {
    featurePanelExpanded.value = true;
    featurePanelHighlight.value = true;
    setTimeout(() => {
      featurePanelHighlight.value = false;
    }, 2000);
  }
});

watch(currentIndex, () => {
  // 切关时轻微提醒：避免残留选择
  resetInteraction();
  updatePotentialAchievements();
});

onMounted(async () => {
  await fetchCases();
  applyIndexFromQuery();
  startTimer();
  // 初始化成就系统
  if (achievementStore.achievements.length === 0) {
    achievementStore.initAchievements();
  }
  achievementStore.hydrate();
  updatePotentialAchievements();
});

onBeforeUnmount(() => {
  stopTimer();
});
</script>

<template>
  <div class="training-page">
    <!-- 顶部训练信息栏 -->
    <el-card class="topbar" shadow="never">
      <div class="topbar-row">
        <div class="left">
          <el-button :icon="ArrowLeft" plain @click="jumpHome">返回首页</el-button>
          <div class="meta">
            <div class="progress">{{ progressText }}</div>
            <div class="sub">
              <el-tag size="small" effect="light" type="info">{{ currentCase?.scamType || '加载中' }} · {{ difficultyLabel }}</el-tag>
              <el-rate :model-value="difficultyStars" disabled :max="3" class="rate" />
            </div>
          </div>
        </div>

        <div class="right">
          <div class="countdown" :class="{ danger: isTimeCritical, 'very-danger': isTimeVeryCritical }">
            <el-progress 
              type="circle" 
              :percentage="timerPercent" 
              :width="56" 
              :stroke-width="6"
              :color="timerColor"
            />
            <div class="countdown-text">
              <div class="label"><el-icon><Clock /></el-icon>倒计时</div>
              <div class="value" :class="{ 'time-critical': isTimeCritical }">{{ timeLeft }}s</div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 步骤指示器 -->
      <div class="steps-indicator">
        <div class="step" :class="{ active: currentStep >= 1, completed: currentStep > 1 }">
          <div class="step-number">1</div>
          <div class="step-label">判断真伪</div>
        </div>
        <div class="step-arrow">→</div>
        <div class="step" :class="{ active: currentStep >= 2, completed: currentStep > 2 }">
          <div class="step-number">2</div>
          <div class="step-label">选择特征</div>
        </div>
        <div class="step-arrow">→</div>
        <div class="step" :class="{ active: currentStep >= 3, completed: currentStep > 3 }">
          <div class="step-number">3</div>
          <div class="step-label">提交</div>
        </div>
      </div>
    </el-card>
    
    <!-- 成就解锁庆祝动画 -->
    <transition name="achievement-celebration">
      <div v-if="showAchievementCelebration && newAchievementUnlocked" class="achievement-celebration">
        <div class="celebration-content">
          <el-icon class="celebration-icon"><Trophy /></el-icon>
          <div class="celebration-text">
            <div class="celebration-title">🎉 成就解锁！</div>
            <div class="celebration-name">
              {{ achievementStore.achievements.find(a => a.id === newAchievementUnlocked)?.name }}
            </div>
          </div>
        </div>
      </div>
    </transition>

    <el-skeleton v-if="loading" :rows="6" animated />

    <div v-else class="grid" v-if="currentCase">
      <!-- 诈骗场景展示区 -->
      <el-card class="scene" shadow="hover" :class="{ 'with-feedback': feedbackVisible && lastResult }">
        <template #header>
          <div class="scene-header">
            <div class="title">
              <el-icon><component :is="scenarioIcon" /></el-icon>
              <span>诈骗场景</span>
            </div>
            <el-tag size="small" effect="dark" type="warning">沉浸式模拟 · 链接不可点击</el-tag>
          </div>
        </template>
        
        <!-- 安全提示横幅 -->
        <div class="safety-banner">
          <el-icon><Lock /></el-icon>
          <span>仿真案例，请勿模仿，切勿点击真实链接</span>
        </div>

        <!-- chat -->
        <div v-if="currentCase.scenario.kind === 'chat'" class="chat">
          <div class="chat-top">
            <div class="chat-name">{{ currentCase.scenario.from }}</div>
            <div class="chat-tip">请识别话术、链接与引导动作</div>
          </div>
          <div class="chat-body" :class="{ 'with-annotations': feedbackVisible && lastResult }">
            <div
              v-for="(m, i) in currentCase.scenario.messages"
              :key="i"
              class="bubble"
              :class="m.from === 'scammer' ? 'left' : 'right'"
            >
              <div class="bubble-inner" v-html="highlightText(m.text, feedbackVisible && lastResult)"></div>
            </div>
          </div>
          <div class="chat-bottom">
            <el-icon><WarningFilled /></el-icon>
            <span>提示：真实训练中请勿点击任何陌生链接。</span>
          </div>
        </div>

        <!-- site -->
        <div v-else-if="currentCase.scenario.kind === 'site'" class="site">
          <div class="site-title">{{ currentCase.scenario.title }}</div>
          <div v-if="currentCase.scenario.subtitle" class="site-sub">{{ currentCase.scenario.subtitle }}</div>
          <el-image
            v-if="currentCase.scenario.imageUrl"
            class="site-img"
            :src="currentCase.scenario.imageUrl"
            fit="contain"
          />
          <div v-else class="site-mock">
            <div class="mock-bar">
              <span class="dot red" /><span class="dot yellow" /><span class="dot green" />
              <span class="mock-url">
                <el-icon><Link /></el-icon>
                <span v-html="highlightText(currentCase.scenario.highlightedText || '')"></span>
              </span>
            </div>
            <div class="mock-body">
              <div class="mock-h1">账号安全验证</div>
              <div class="mock-desc">检测到登录异常，需要立即验证以避免账户冻结。</div>
              <div class="mock-form">
                <div class="mock-input" />
                <div class="mock-input" />
                <div class="mock-btn">立即验证</div>
              </div>
              <div class="mock-foot">链接与关键词仅用于训练展示，不可点击。</div>
            </div>
          </div>
        </div>

        <!-- audio -->
        <div v-else class="audio">
          <div class="audio-title">{{ currentCase.scenario.title }}</div>
          <audio v-if="currentCase.scenario.audioUrl" class="audio-player" :src="currentCase.scenario.audioUrl" controls />
          <el-alert v-else title="当前为演示模式（未提供真实音频），请阅读转写内容进行判断。" type="info" show-icon />
          <div class="transcript af-soft-panel">
            <div class="card-section-title">文字转写（关键词高亮）</div>
            <div class="transcript-text" v-html="highlightText(currentCase.scenario.transcript)"></div>
          </div>
        </div>
      </el-card>

      <!-- 用户判断区 -->
      <el-card 
        class="judge" 
        shadow="hover"
        :class="{ 'has-error': validationError.type === 'judgement' }"
      >
        <template #header>
          <div class="judge-header">
            <div class="title">你的判断</div>
            <el-tag size="small" effect="light" type="info">未完成选择将禁止提交</el-tag>
          </div>
        </template>

        <div class="q">{{ currentCase.question }}</div>
        <div class="judge-buttons">
          <el-button
            size="large"
            type="danger"
            :plain="userJudgement !== 'fraud'"
            @click="userJudgement = 'fraud'"
          >
            是诈骗
          </el-button>
          <el-button
            size="large"
            type="success"
            :plain="userJudgement !== 'safe'"
            @click="userJudgement = 'safe'"
          >
            非诈骗
          </el-button>
        </div>
        
        <div v-if="validationError.type === 'judgement'" class="validation-error">
          <el-icon><WarningFilled /></el-icon>
          <span>{{ validationError.message }}</span>
        </div>

        <transition name="expand">
          <div 
            v-show="featurePanelExpanded || userJudgement" 
            class="af-soft-panel feature-panel"
            :class="{ 
              highlight: featurePanelHighlight,
              'has-error': validationError.type === 'feature'
            }"
          >
            <div class="card-section-title">
              选择你认为可疑的特征（多选）
              <span class="required-badge">必选</span>
            </div>
            <div class="hint-before">建议：至少选择 1 项；越精准越能提升识别能力。</div>
            <el-checkbox-group v-model="chosen" class="feature-group">
              <el-checkbox v-for="opt in currentCase.options" :key="opt" :value="opt">{{ opt }}</el-checkbox>
            </el-checkbox-group>
            <div v-if="validationError.type === 'feature'" class="validation-error">
              <el-icon><WarningFilled /></el-icon>
              <span>{{ validationError.message }}</span>
            </div>
          </div>
        </transition>

        <div class="submit-row">
          <el-button type="primary" size="large" :disabled="!canSubmit" @click="submit">提交</el-button>
          <el-button size="large" plain @click="nextLevel">下一关</el-button>
          <div class="submit-tip" :class="{ ok: canSubmit }">
            <span v-if="canSubmit">已完成选择，可以提交。</span>
            <span v-else>请选择“是/非”并勾选至少 1 项可疑特征（且未超时）。</span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 提交与反馈弹层 -->
    <el-dialog v-model="feedbackVisible" width="720px" :close-on-click-modal="false" align-center>
      <template #header>
        <div class="dialog-head" :class="{ ok: !!lastResult?.correct, bad: lastResult && !lastResult.correct }">
          <div class="dialog-title">
            <span v-if="lastResult?.correct">判断正确</span>
            <span v-else>判断有误</span>
          </div>
          <div class="dialog-sub">
            用时：{{ Math.round((lastResult?.timeSpentMs || 0) / 1000) }}s ·
            命中：{{ lastResult?.hitPoints.length || 0 }}/{{ currentCase?.suspiciousPoints.length || 0 }}
          </div>
        </div>
      </template>

      <div v-if="currentCase && lastResult" class="dialog-body">
        <!-- 简短反馈 -->
        <el-alert
          v-if="lastResult.correct"
          title="判断正确！"
          type="success"
          show-icon
          :closable="false"
        />
        <el-alert
          v-else
          title="判断有误，继续学习提升识别能力。"
          type="error"
          show-icon
          :closable="false"
        />

        <!-- 详细解析（可展开） -->
        <div class="detailed-analysis-section">
          <el-button 
            text 
            type="primary" 
            @click="detailedAnalysisVisible = !detailedAnalysisVisible"
            class="toggle-analysis-btn"
          >
            <span>{{ detailedAnalysisVisible ? '收起' : '查看' }}详细解析</span>
            <el-icon><ArrowRight /></el-icon>
          </el-button>
          
          <transition name="expand">
            <div v-show="detailedAnalysisVisible" class="detailed-analysis">
              <div class="block">
                <div class="block-title">诈骗解析</div>
                <div class="block-text">{{ currentCase.analysis }}</div>
              </div>

              <div class="block">
                <div class="block-title">你的选择</div>
                <div class="tags">
                  <el-tag 
                    v-for="x in chosen" 
                    :key="x" 
                    :type="currentCase.suspiciousPoints.includes(x) ? 'success' : 'info'" 
                    effect="light" 
                    round
                  >
                    {{ x }}
                    <el-icon v-if="currentCase.suspiciousPoints.includes(x)" class="tag-icon"><Star /></el-icon>
                  </el-tag>
                  <span v-if="!chosen.length" class="text-muted">（无）</span>
                </div>
              </div>

              <div class="block">
                <div class="block-title" v-if="lastResult.correct">防范建议</div>
                <div class="block-title" v-else>忽略的风险点 & 学习建议</div>

                <div v-if="!lastResult.correct && lastResult.missedPoints.length" class="missed">
                  <div class="missed-title">你可能漏掉了这些关键风险点：</div>
                  <div class="tags">
                    <el-tag v-for="p in lastResult.missedPoints" :key="p" type="warning" effect="dark" round>{{ p }}</el-tag>
                  </div>
                </div>

                <ul class="advice">
                  <li v-for="(a, i) in currentCase.advice" :key="i">{{ a }}</li>
                </ul>
              </div>
              
              <!-- 链接到防骗知识模块 -->
              <div class="knowledge-link-block">
                <el-divider />
                <div class="knowledge-link-content">
                  <el-icon><Reading /></el-icon>
                  <div>
                    <div class="knowledge-link-title">深入学习</div>
                    <div class="knowledge-link-desc">
                      此类型诈骗的常见手法与防范要点，可在
                      <el-button text type="primary" @click="router.push({ path: '/knowledge', query: { type: currentCase.scamType } })">
                        "防骗知识"
                      </el-button>
                      模块中深入学习。
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </transition>
        </div>
      </div>

      <template #footer>
        <el-button @click="feedbackVisible = false">继续查看本题</el-button>
        <el-button type="primary" @click="nextLevel">下一关</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.training-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 0 4px;
}

.topbar {
  border: none;
  background: linear-gradient(120deg, rgba(47, 113, 255, 0.12), rgba(111, 195, 255, 0.12));
  position: relative;
  padding: 20px 24px;
  margin-bottom: 8px;
}

/* 步骤指示器 */
.steps-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  opacity: 0.4;
  transition: all 0.3s ease;
}

.step.active {
  opacity: 1;
}

.step.completed .step-number {
  background: var(--el-color-success);
  color: #fff;
}

.step-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  transition: all 0.3s ease;
}

.step.active .step-number {
  background: var(--el-color-primary);
  color: #fff;
  transform: scale(1.1);
}

.step-label {
  font-size: 12px;
  color: var(--af-muted);
  font-weight: 600;
}

.step.active .step-label {
  color: var(--el-color-primary);
  font-weight: 700;
}

.step-arrow {
  color: var(--af-muted);
  font-size: 20px;
  opacity: 0.4;
  font-weight: 300;
  margin: 0 4px;
}


/* 成就庆祝动画 */
.achievement-celebration {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 3000;
  pointer-events: none;
}

.celebration-content {
  background: linear-gradient(135deg, #ffd700, #ffed4e);
  padding: 24px 32px;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(255, 215, 0, 0.4);
  display: flex;
  align-items: center;
  gap: 16px;
  animation: celebrationPop 0.5s ease;
}

.celebration-icon {
  font-size: 48px;
  color: #fff;
}

.celebration-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.celebration-title {
  font-size: 20px;
  font-weight: 900;
  color: #fff;
}

.celebration-name {
  font-size: 16px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
}

@keyframes celebrationPop {
  0% {
    transform: scale(0.5);
    opacity: 0;
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.achievement-celebration-enter-active,
.achievement-celebration-leave-active {
  transition: all 0.3s ease;
}

.achievement-celebration-enter-from,
.achievement-celebration-leave-to {
  opacity: 0;
  transform: translate(-50%, -50%) scale(0.8);
}
.topbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.meta {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.progress {
  font-weight: 800;
  font-size: 18px;
}
.sub {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.rate {
  transform: translateY(1px);
}

.right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  min-width: 0;
}

/* 潜在成就提示 */
.potential-achievements {
  margin-right: 0;
  margin-bottom: 0;
  white-space: nowrap;
}

.countdown {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(0, 0, 0, 0.04);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.countdown:hover {
  transform: translateY(-1px);
}
.countdown.danger {
  animation: pulse 0.9s ease-in-out infinite;
  border-color: rgba(245, 108, 108, 0.35);
}

.countdown.very-danger {
  animation: pulse 0.5s ease-in-out infinite;
  border-color: rgba(245, 108, 108, 0.6);
}

.countdown-text .value.time-critical {
  color: #f56c6c;
  font-weight: 900;
  animation: blink 1s ease-in-out infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
.countdown-text .label {
  color: var(--af-muted);
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}
.countdown-text .value {
  font-weight: 800;
  font-size: 16px;
}

.grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 24px;
  margin-top: 8px;
}

/* 安全横幅 */
.safety-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.15), rgba(255, 152, 0, 0.15));
  border: 2px dashed rgba(255, 152, 0, 0.4);
  border-radius: 8px;
  margin-bottom: 20px;
  margin-top: 8px;
  color: #e65100;
  font-size: 13px;
  font-weight: 600;
}

.safety-banner .el-icon {
  font-size: 18px;
}

.scene.with-feedback {
  border: 2px solid rgba(47, 113, 255, 0.2);
}

/* 卡片内边距优化 */
:deep(.el-card__body) {
  padding: 20px 24px;
}

.scene :deep(.el-card__body),
.judge :deep(.el-card__body) {
  padding: 24px;
}

.scene-header,
.judge-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 4px 0 8px 0;
  margin-bottom: 8px;
}
.title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
}

/* chat */
.chat {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 0;
}
.chat-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 4px;
}
.chat-name {
  font-weight: 800;
}
.chat-tip {
  color: var(--af-muted);
  font-size: 12px;
}
.chat-body {
  background: var(--af-soft);
  border-radius: 14px;
  padding: 16px;
  min-height: 280px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: auto;
}

.chat-body.with-annotations {
  border: 2px solid rgba(47, 113, 255, 0.2);
  background: rgba(47, 113, 255, 0.03);
}
.bubble {
  display: flex;
}
.bubble.left {
  justify-content: flex-start;
}
.bubble.right {
  justify-content: flex-end;
}
.bubble-inner {
  max-width: 84%;
  padding: 10px 12px;
  border-radius: 14px;
  line-height: 1.55;
  position: relative;
  animation: pop 0.18s ease;
}
.bubble.left .bubble-inner {
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.06);
}
.bubble.right .bubble-inner {
  background: rgba(47, 113, 255, 0.12);
  border: 1px solid rgba(47, 113, 255, 0.18);
}
.chat-bottom {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-color-warning);
  font-size: 12px;
  margin-top: 4px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

/* site */
.site-title {
  font-weight: 800;
  font-size: 16px;
}
.site-sub {
  color: var(--af-muted);
  margin-top: 8px;
  margin-bottom: 16px;
}
.site-img {
  width: 100%;
  max-height: 360px;
  border-radius: 14px;
  border: 1px solid rgba(0, 0, 0, 0.06);
}
.site-mock {
  border-radius: 14px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  overflow: hidden;
}
.mock-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px;
  background: rgba(0, 0, 0, 0.03);
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}
.dot.red {
  background: #ef4444;
}
.dot.yellow {
  background: #f59e0b;
}
.dot.green {
  background: #22c55e;
}
.mock-url {
  margin-left: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--af-muted);
  font-size: 12px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.mock-body {
  padding: 14px;
  background: #fff;
}
.mock-h1 {
  font-size: 18px;
  font-weight: 800;
}
.mock-desc {
  margin-top: 6px;
  color: var(--af-muted);
}
.mock-form {
  margin-top: 12px;
  display: grid;
  gap: 10px;
  max-width: 360px;
}
.mock-input {
  height: 38px;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.04);
}
.mock-btn {
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #2f71ff, #6fc3ff);
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 800;
}
.mock-foot {
  margin-top: 12px;
  font-size: 12px;
  color: var(--af-muted);
}

/* audio */
.audio {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 0;
}
.audio-title {
  font-weight: 800;
  font-size: 16px;
}
.audio-player {
  width: 100%;
}
.transcript-text {
  line-height: 1.6;
}

/* judge */
.q {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 18px;
  padding: 8px 0;
}
.judge-buttons {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.judge.has-error {
  border: 2px solid rgba(245, 108, 108, 0.4);
  animation: shake 0.5s ease;
}

/* 抖动动画 */
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); }
  20%, 40%, 60%, 80% { transform: translateX(5px); }
}

.feature-panel {
  margin-top: 20px;
  padding: 16px;
  transition: all 0.3s ease;
}

.feature-panel.highlight {
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.1), rgba(255, 152, 0, 0.1));
  border: 2px solid rgba(255, 152, 0, 0.3);
  animation: highlightPulse 2s ease;
}

.feature-panel.has-error {
  border: 2px solid rgba(245, 108, 108, 0.4);
  animation: shake 0.5s ease;
}

@keyframes highlightPulse {
  0%, 100% { box-shadow: 0 0 0 rgba(255, 152, 0, 0); }
  50% { box-shadow: 0 0 20px rgba(255, 152, 0, 0.3); }
}

.card-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  margin-bottom: 14px;
  font-size: 15px;
}

.required-badge {
  font-size: 11px;
  padding: 2px 6px;
  background: rgba(245, 108, 108, 0.15);
  color: #f56c6c;
  border-radius: 4px;
  font-weight: 600;
}

.hint-before {
  margin-bottom: 16px;
  color: var(--af-muted);
  font-size: 12px;
  padding: 10px 12px;
  background: rgba(47, 113, 255, 0.05);
  border-radius: 6px;
  border-left: 3px solid var(--el-color-primary);
  line-height: 1.6;
}

.validation-error {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #f56c6c;
  font-size: 13px;
  margin-top: 12px;
  padding: 8px 12px;
  background: rgba(245, 108, 108, 0.1);
  border-radius: 6px;
}

/* 展开动画 */
.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
  margin-top: 0;
  margin-bottom: 0;
  padding-top: 0;
  padding-bottom: 0;
}
.feature-group {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px 14px;
  margin-top: 8px;
}
.hint {
  margin-top: 12px;
  font-size: 12px;
  padding-top: 8px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}
.submit-row {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.submit-tip {
  color: var(--af-muted);
  font-size: 12px;
}
.submit-tip.ok {
  color: var(--el-color-success);
  font-weight: 600;
}

/* dialog */
.dialog-head {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.dialog-title {
  font-size: 18px;
  font-weight: 900;
}
.dialog-sub {
  color: var(--af-muted);
  font-size: 12px;
}
.dialog-head.ok .dialog-title {
  color: var(--el-color-success);
}
.dialog-head.bad .dialog-title {
  color: var(--el-color-danger);
}
.dialog-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.block-title {
  font-weight: 800;
  margin: 10px 0 6px;
}
.block-text {
  line-height: 1.6;
}
.tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.missed-title {
  font-weight: 700;
  margin: 8px 0 6px;
}
.advice {
  margin: 8px 0 0;
  padding-left: 18px;
  line-height: 1.7;
}

/* 详细解析区域 */
.detailed-analysis-section {
  margin-top: 16px;
}

.toggle-analysis-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px;
  margin-bottom: 12px;
}

.detailed-analysis {
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.tag-icon {
  margin-left: 4px;
  font-size: 12px;
}

/* 知识库链接 */
.knowledge-link-block {
  margin-top: 16px;
}

.knowledge-link-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: rgba(47, 113, 255, 0.05);
  border-radius: 8px;
  border-left: 3px solid var(--el-color-primary);
}

.knowledge-link-content .el-icon {
  font-size: 24px;
  color: var(--el-color-primary);
  margin-top: 2px;
}

.knowledge-link-title {
  font-weight: 700;
  margin-bottom: 4px;
  color: var(--el-color-primary);
}

.knowledge-link-desc {
  font-size: 13px;
  color: var(--af-muted);
  line-height: 1.6;
}

/* 高亮（不可点击） */
:deep(.hl) {
  display: inline-block;
  padding: 0 4px;
  border-radius: 6px;
  margin: 0 1px;
  border: 1px dashed rgba(0, 0, 0, 0.08);
}
:deep(.hl-link) {
  color: #1d4ed8;
  background: rgba(59, 130, 246, 0.12);
  text-decoration: line-through;
  cursor: not-allowed;
  pointer-events: none;
}

:deep(.hl-suspicious) {
  position: relative;
  border: 2px solid rgba(245, 108, 108, 0.5) !important;
  background: rgba(245, 108, 108, 0.15) !important;
  animation: suspiciousPulse 2s ease-in-out infinite;
}

:deep(.hl-suspicious::after) {
  content: attr(data-tooltip);
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.9);
  color: #fff;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 12px;
  white-space: nowrap;
  margin-bottom: 4px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s ease;
  z-index: 1000;
}

:deep(.hl-suspicious:hover::after) {
  opacity: 1;
}

@keyframes suspiciousPulse {
  0%, 100% { box-shadow: 0 0 0 rgba(245, 108, 108, 0); }
  50% { box-shadow: 0 0 8px rgba(245, 108, 108, 0.5); }
}
:deep(.hl-code) {
  color: #7c3aed;
  background: rgba(139, 92, 246, 0.12);
}
:deep(.hl-money) {
  color: #b45309;
  background: rgba(245, 158, 11, 0.14);
}
:deep(.hl-urgent) {
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.14);
}
:deep(.hl-key) {
  color: #0f766e;
  background: rgba(20, 184, 166, 0.12);
}

@keyframes pop {
  from {
    transform: scale(0.98);
    opacity: 0.7;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}
@keyframes pulse {
  0% {
    transform: translateY(0);
    box-shadow: 0 0 0 rgba(245, 108, 108, 0.0);
  }
  50% {
    transform: translateY(-1px);
    box-shadow: 0 10px 24px rgba(245, 108, 108, 0.18);
  }
  100% {
    transform: translateY(0);
    box-shadow: 0 0 0 rgba(245, 108, 108, 0.0);
  }
}

/* 响应式优化 */
@media (max-width: 980px) {
  .grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  
  .training-page {
    gap: 16px;
  }
  
  .topbar {
    padding: 16px 20px;
  }
  
  .scene :deep(.el-card__body),
  .judge :deep(.el-card__body) {
    padding: 20px;
  }
  
  .topbar-row {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .right {
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    margin-top: 12px;
  }
  
  .potential-achievements {
    margin-bottom: 0;
  }
}
</style>

