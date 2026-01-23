<script setup lang="ts">
import { ref, onMounted, computed, onBeforeUnmount } from 'vue';
import http from '../api/http';
import { useUserStore } from '../stores/user';
import CaseAnalysisDialog from '../components/CaseAnalysisDialog.vue';
import { ElMessage } from 'element-plus';
import { ElNotification } from 'element-plus';

type CaseType = 'sms' | 'email' | 'audio' | 'site';

type TrainCase = {
  id: number;
  type: CaseType;
  content: string;
  hint: string;
  answer: 'fraud' | 'safe';
  level: 'easy' | 'medium' | 'hard';
  suspiciousPoints: string[];
  mediaUrl?: string;
};

const fallback: TrainCase[] = [
  {
    id: 1,
    type: 'sms',
    content: '【银行】点击 http://fake.com 验证账户，否则将冻结账号。',
    hint: '仿冒银行+钓鱼链接+恐吓语言',
    answer: 'fraud',
    level: 'easy',
    suspiciousPoints: ['可疑短链/非官方域名', '以冻结账号恐吓催促操作'],
  },
  {
    id: 2,
    type: 'email',
    content: '发票查验通知，请下载附件 patch.exe 并安装补丁。',
    hint: '诱导安装 exe 可执行文件',
    answer: 'fraud',
    level: 'medium',
    suspiciousPoints: ['要求安装未知 exe 文件', '紧急语气催促“立刻安装”'],
  },
];
const cases = ref<TrainCase[]>(fallback);
const store = useUserStore();

const current = ref(0);
const feedback = ref('');
const correctCount = ref(0);
const loading = ref(false);
const timeLeft = ref(30);
const timer = ref<number | null>(null);
const showDialog = ref(false);
const chosen = ref<string[]>([]);
const lastChoice = ref<'fraud' | 'safe' | null>(null);

const total = computed(() => cases.value.length);
const currentCase = computed(() => cases.value[current.value] || null);
const featureHitRate = computed(() => {
  const c = currentCase.value;
  if (!c || !c.suspiciousPoints.length) return '0/0';
  const hit = c.suspiciousPoints.filter((p) => chosen.value.includes(p)).length;
  return `${hit}/${c.suspiciousPoints.length}`;
});

onMounted(async () => {
  loading.value = true;
  try {
    const resp = await http.get('/cases');
    cases.value = resp.data;
  } catch {
    cases.value = fallback;
    ElMessage.info('当前使用内置示例案例，若需真实数据请启动后端服务。');
  } finally {
    loading.value = false;
    startTimer();
  }
});

function answer(choice: 'fraud' | 'safe') {
  const target = currentCase.value;
  if (!target) return;
  if (!chosen.value.length) {
    ElMessage.warning('请至少勾选 1 项可疑特征后再提交判断。');
    return;
  }
  if (choice === target.answer) {
    feedback.value = '✅ 判断正确：' + target.hint;
    correctCount.value += 1;
    store.recordTraining();
  } else {
    feedback.value = '❌ 判断错误，注意：' + target.hint;
  }

  // 统一到后端记录训练数据（若未登录则跳过，不影响体验）
  if (store.userId) {
    const spent = Math.max(0, 30 - timeLeft.value) * 1000;
    http
      .post('/train/records', {
        userId: store.userId,
        caseId: target.id,
        // 后端期望大写枚举值：FRAUD / SAFE
        answer: choice === 'fraud' ? 'FRAUD' : 'SAFE',
        correct: choice === target.answer,
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
  lastChoice.value = choice;
  showDialog.value = true;
}

function nextCase() {
  if (!total.value) return;
  current.value = (current.value + 1) % total.value;
  feedback.value = '';
  chosen.value = [];
  timeLeft.value = 30;
  startTimer();
}

function startTimer() {
  if (timer.value) window.clearInterval(timer.value);
  timer.value = window.setInterval(() => {
    timeLeft.value -= 1;
    if (timeLeft.value <= 0) {
      timeLeft.value = 0;
      feedback.value = '⏱ 时间到，请尽快作答。';
      if (timer.value) window.clearInterval(timer.value);
    }
  }, 1000);
}

onBeforeUnmount(() => {
  if (timer.value) window.clearInterval(timer.value);
});
</script>

<template>
  <el-card>
    <template #header>
      <div class="title">
        <span>识别训练</span>
        <el-tag type="warning">情景测验</el-tag>
      </div>
    </template>
    <el-skeleton v-if="loading" :rows="3" animated />
    <div v-else-if="currentCase">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="类型">{{ currentCase.type }}</el-descriptions-item>
        <el-descriptions-item label="信息">{{ currentCase.content }}</el-descriptions-item>
      </el-descriptions>
      <el-card v-if="currentCase.type === 'audio'" class="audio-card" shadow="never">
        <div class="card-section-title">语音话术（占位）</div>
        <el-button type="primary" size="small">播放录音</el-button>
        <span class="text-muted" style="margin-left: 8px;">实际项目可接入真实音频流。</span>
      </el-card>
      <el-image
        v-if="currentCase.mediaUrl"
        :src="currentCase.mediaUrl"
        fit="contain"
        style="width: 100%; max-height: 240px; margin-top: 12px; border-radius: 12px"
      />
      <div class="features af-soft-panel">
        <div class="card-section-title">可疑特征（多选）</div>
        <el-checkbox-group v-model="chosen">
          <el-checkbox v-for="p in currentCase.suspiciousPoints" :key="p" :value="p">
            {{ p }}
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <div class="actions af-actions">
        <el-button type="danger" @click="answer('fraud')">可疑/诈骗</el-button>
        <el-button type="success" @click="answer('safe')">正常</el-button>
        <el-button @click="nextCase">下一题</el-button>
      </div>
      <el-alert v-if="feedback" :title="feedback" type="info" show-icon />
      <div class="meta">
        <el-tag type="success">正确 {{ correctCount }} 题</el-tag>
        <el-tag type="info">进度 {{ current + 1 }}/{{ total }}</el-tag>
        <el-tag type="danger" v-if="timeLeft <= 5">剩余 {{ timeLeft }}s</el-tag>
        <el-tag type="warning">特征命中 {{ featureHitRate }}</el-tag>
      </div>
      <case-analysis-dialog
        v-if="currentCase"
        v-model:visible="showDialog"
        :title="`第 ${current + 1} 题`"
        :content="currentCase.content"
        :hint="currentCase.hint"
        :suspicious-points="currentCase.suspiciousPoints"
        :user-chosen="chosen"
      />
    </div>
  </el-card>
</template>

<style scoped lang="scss">
.title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.features {
  margin-top: 12px;
  /* 视觉样式由全局 .af-soft-panel 统一 */
}
.audio-card {
  margin-top: 12px;
}
.actions {
  margin-top: 16px;
  /* 间距/换行由全局 .af-actions 统一 */
}
.meta {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}
</style>
