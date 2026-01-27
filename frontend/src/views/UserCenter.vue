<template>
  <div class="user-center-page">
    <!-- 顶部用户信息区 -->
    <el-card class="user-header-card" shadow="never">
      <div class="user-header">
        <div class="user-avatar-section" @click="handleAvatarEdit">
          <el-avatar 
            :size="100" 
            class="user-avatar user-avatar-clickable"
            :style="{ background: avatarBg }"
            :src="userStore.avatar || undefined"
          >
            <el-icon :size="50"><UserFilled /></el-icon>
          </el-avatar>
          <div v-if="isAuthed" class="avatar-hint">点击更换头像</div>
          <el-button 
            v-if="isAuthed" 
            class="avatar-edit-btn" 
            circle 
            size="small"
            @click.stop="handleAvatarEdit"
          >
            <el-icon><Camera /></el-icon>
          </el-button>
          <input
            ref="avatarInputRef"
            type="file"
            accept="image/*"
            class="hidden-file-input"
            @change="onAvatarFileChange"
          />
        </div>
        <div class="user-info">
          <div class="user-name-row" @click="openAccountDialog">
            <h2 class="user-name">{{ userStore.name || '访客' }}</h2>
            <el-tag v-if="isAuthed" type="primary" effect="dark" size="large" round>
              Lv.{{ achievementStore.level }}
            </el-tag>
          </div>
          <div class="user-title-row">
            <el-tag type="info" effect="light" size="default">
              {{ achievementStore.levelTitle }}
            </el-tag>
            <el-tag 
              :type="riskTagType" 
              effect="light" 
              size="default"
              style="margin-left: 8px"
            >
              风险等级：{{ riskLabel }}
            </el-tag>
          </div>
          <div class="user-exp-section">
            <div class="exp-info">
              <span class="exp-label">经验值</span>
              <span class="exp-value">
                {{ achievementStore.exp }} / {{ achievementStore.expForNextLevel }}
              </span>
            </div>
            <el-progress 
              :percentage="expProgress" 
              :stroke-width="12"
              :color="expProgressColor"
              :show-text="false"
              class="exp-progress"
            />
          </div>
        </div>
      </div>
    </el-card>

    <!-- （去掉与首页重复的快捷功能和成长数据，只在本页聚焦画像与账号管理） -->

    <!-- 学习画像：已完成测评 / 潜在危险 / 错题回顾 -->
    <el-card class="analysis-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">学习画像与错题本</span>
        </div>
      </template>
      <div class="analysis-sections">
        <!-- 左侧：已完成测评 + 错题回顾 -->
        <div class="analysis-left">
          <!-- 已完成风险测评 -->
          <div>
            <div class="analysis-section-title">已完成风险测评</div>
            <div class="analysis-subtitle">
              展示最近 5 次测评记录，便于回顾风险变化。
            </div>
            <el-alert
              v-if="!completedReports.length"
              title="暂无测评记录，请先在“风险测评”完成一次评估。"
              type="info"
              show-icon
            />
            <el-table
              v-else
              :data="completedReports"
              size="small"
              class="analysis-table"
              stripe
            >
              <el-table-column label="时间" width="170">
                <template #default="scope">
                  {{ new Date(scope.row.createdAt).toLocaleString('zh-CN') }}
                </template>
              </el-table-column>
              <el-table-column prop="score" label="综合指数" width="110" />
              <el-table-column label="风险等级" width="110">
                <template #default="scope">
                  <el-tag
                    :type="scope.row.level === 'low' ? 'success' : scope.row.level === 'medium' ? 'warning' : 'danger'"
                    size="small"
                  >
                    {{ scope.row.level === 'low' ? '低风险' : scope.row.level === 'medium' ? '中等风险' : '高风险' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="110" align="center">
                <template #default="scope">
                  <el-button
                    v-if="scope.row.id"
                    type="primary"
                    text
                    size="small"
                    @click="goReportDetail(scope.row.id)"
                  >
                    查看报告
                  </el-button>
                  <span v-else class="text-muted">—</span>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 错题回顾 -->
          <div>
            <div class="analysis-section-title">错题回顾（防诈训练）</div>
            <div class="analysis-subtitle">
              汇总训练中判断错误的场景与遗漏风险点，便于有针对性地复习。
            </div>
            <el-alert
              v-if="!wrongRecords.length"
              title="目前还没有记录到错误案例，继续保持，也可以多做几关训练。"
              type="success"
              show-icon
            />
            <el-table
              v-else
              :data="wrongRecords"
              size="small"
              class="analysis-table"
              height="220"
            >
              <el-table-column label="时间" width="170">
                <template #default="scope">
                  {{ new Date(scope.row.at).toLocaleString('zh-CN') }}
                </template>
              </el-table-column>
              <el-table-column prop="scamType" label="场景类型" width="140" />
              <el-table-column label="遗漏风险点">
                <template #default="scope">
                  <div class="wrong-tags">
                    <el-tag
                      v-for="mp in scope.row.missedPoints"
                      :key="mp"
                      size="small"
                      type="warning"
                      effect="light"
                      round
                    >
                      {{ mp }}
                    </el-tag>
                  </div>
                  <span
                    v-if="!scope.row.missedPoints || !scope.row.missedPoints.length"
                    class="text-muted"
                  >
                    ——
                  </span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <!-- 右侧：潜在危险画像 -->
        <div class="analysis-right" ref="weaknessRef">
          <div>
            <div class="analysis-section-title">潜在危险画像</div>
            <div class="analysis-subtitle">
              基于最近一次测评结果，提炼出你的高风险维度。
            </div>
            <el-alert
              v-if="!lastReport"
              title="暂无评估数据，完成一次风险测评后，这里会展示你的高风险维度。"
              type="info"
              show-icon
            />
            <div v-else>
              <div
                v-for="(dim, idx) in mainRisks"
                :key="dim.dimension"
                class="risk-dimension-item"
              >
                <div class="risk-dimension-name">
                  {{ idx + 1 }}. {{ dim.name }}
                </div>
                <div class="risk-dimension-meta">
                  指数 {{ dim.score }} · 等级 {{ levelLabel(dim.level) }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 我的操作区 -->
    <el-card class="actions-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">我的操作</span>
        </div>
      </template>
      <div v-if="!isAuthed" class="guest-tip-section">
        <el-alert
          title="您当前以游客身份访问"
          description="登录后可以保存训练记录、查看详细报告、解锁成就等功能"
          type="info"
          :closable="false"
          show-icon
        />
        <div class="guest-actions">
          <el-button type="primary" @click="goLogin">立即登录</el-button>
          <el-button @click="goRegister">注册账号</el-button>
        </div>
      </div>
      <div v-else class="user-actions-layout">
        <div class="user-actions">
          <div class="user-actions-grid">
            <el-button class="action-btn" @click="handleAccountSettings">
              <el-icon><Setting /></el-icon>
              <span>账号设置</span>
            </el-button>
            <el-button class="action-btn" @click="handleChangePassword">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </el-button>
            <el-button class="action-btn" @click="openAchievementDialog">
              <el-icon><Star /></el-icon>
              <span>我的成就</span>
            </el-button>
            <el-button class="action-btn" @click="openWeaknessDialog">
              <el-icon><DataAnalysis /></el-icon>
              <span>我的薄弱</span>
            </el-button>
            <el-button class="action-btn" @click="openKnowledgeDialog">
              <el-icon><Document /></el-icon>
              <span>我的知识</span>
            </el-button>
            <el-button class="action-btn" type="danger" @click="openLogoutDialog">
              <el-icon><SwitchButton /></el-icon>
              <span>退出登录</span>
            </el-button>
          </div>
        </div>
        <div class="user-achievements">
          <div class="achievements-header">
            <span class="achievements-title">已获得成就</span>
            <span class="achievements-count" v-if="unlockedAchievementsList.length">
              共 {{ unlockedAchievementsList.length }} 项
            </span>
          </div>
          <div v-if="!unlockedAchievementsList.length" class="achievements-empty text-muted">
            暂无已解锁成就，完成训练、测评和学习后会在这里展示。
          </div>
          <ul v-else class="achievements-list">
            <li v-for="a in unlockedAchievementsList" :key="a.id" class="achievement-item">
              <span class="achievement-icon">{{ a.icon }}</span>
              <div class="achievement-main">
                <div class="achievement-name">{{ a.name }}</div>
                <div class="achievement-desc text-muted">{{ a.description }}</div>
              </div>
            </li>
          </ul>
        </div>
      </div>
    </el-card>

    <!-- 我的成就弹窗 -->
    <el-dialog v-model="achievementDialogVisible" title="我的成就" width="720px">
      <el-tabs v-model="achievementTab" class="dialog-tabs">
        <el-tab-pane :label="`已获得（${unlockedAchievementsList.length}）`" name="unlocked">
          <el-empty v-if="!unlockedAchievementsList.length" description="暂无已解锁成就" :image-size="90" />
          <el-table v-else :data="unlockedAchievementsList" size="small" stripe class="dialog-table">
            <el-table-column label="图标" width="70" align="center">
              <template #default="scope">
                <span class="emoji-icon">{{ scope.row.icon }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="成就名称" min-width="160" />
            <el-table-column prop="description" label="说明" min-width="260" />
            <el-table-column label="解锁时间" width="180">
              <template #default="scope">
                <span class="text-muted">
                  {{ scope.row.unlockedAt ? new Date(scope.row.unlockedAt).toLocaleString('zh-CN') : '—' }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="`未获得（${lockedAchievementsList.length}）`" name="locked">
          <el-empty v-if="!lockedAchievementsList.length" description="你已解锁全部成就，太强了！" :image-size="90" />
          <el-table v-else :data="lockedAchievementsList" size="small" stripe class="dialog-table">
            <el-table-column label="图标" width="70" align="center">
              <template #default="scope">
                <span class="emoji-icon">{{ scope.row.icon }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="成就名称" min-width="160" />
            <el-table-column prop="description" label="说明" min-width="240" />
            <el-table-column label="进度" width="180">
              <template #default="scope">
                <el-progress :percentage="Math.round(scope.row.progress || 0)" :stroke-width="10" />
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="achievementDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 我的薄弱弹窗 -->
    <el-dialog v-model="weaknessDialogVisible" title="我的薄弱分析" width="720px">
      <div class="dialog-section">
        <el-alert
          v-if="!lastReport && !wrongRecords.length"
          title="暂无足够数据生成薄弱分析"
          description="完成一次风险测评或进行识别训练后，系统会在这里给出你的薄弱点与改进建议。"
          type="info"
          show-icon
          :closable="false"
        />

        <template v-else>
          <div class="dialog-block">
            <div class="dialog-block-title">测评维度薄弱</div>
            <div class="dialog-block-sub">基于最近一次风险测评的高风险维度（分数越高风险越大）。</div>
            <el-empty v-if="!lastReport" description="暂无测评数据" :image-size="80" />
            <el-table v-else :data="topRiskDimensions" size="small" stripe class="dialog-table">
              <el-table-column prop="name" label="维度" min-width="180" />
              <el-table-column prop="score" label="风险指数" width="110" />
              <el-table-column label="等级" width="110">
                <template #default="scope">
                  {{ levelLabel(scope.row.level) }}
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="dialog-block">
            <div class="dialog-block-title">错题薄弱点</div>
            <div class="dialog-block-sub">从近期错题中统计你最常遗漏的风险点与高频场景。</div>
            <el-empty v-if="!wrongRecords.length" description="暂无错题数据" :image-size="80" />
            <template v-else>
              <div class="weakness-kpis">
                <div class="weakness-kpi">
                  <div class="weakness-kpi-label">高频场景</div>
                  <div class="weakness-kpi-value">{{ topScamTypes[0]?.name || '—' }}</div>
                  <div class="weakness-kpi-sub text-muted">近 20 条错题统计</div>
                </div>
                <div class="weakness-kpi">
                  <div class="weakness-kpi-label">最常遗漏风险点</div>
                  <div class="weakness-kpi-value">{{ topMissedPoints[0]?.name || '—' }}</div>
                  <div class="weakness-kpi-sub text-muted">近 20 条错题统计</div>
                </div>
              </div>

              <div class="weakness-tags-row">
                <div class="weakness-tags-col">
                  <div class="tags-title">高频场景 TOP 3</div>
                  <el-tag v-for="x in topScamTypes" :key="x.name" type="info" effect="light" round class="tag-item">
                    {{ x.name }}（{{ x.count }}）
                  </el-tag>
                </div>
                <div class="weakness-tags-col">
                  <div class="tags-title">遗漏风险点 TOP 5</div>
                  <el-tag v-for="x in topMissedPoints" :key="x.name" type="warning" effect="light" round class="tag-item">
                    {{ x.name }}（{{ x.count }}）
                  </el-tag>
                </div>
              </div>
            </template>
          </div>

          <div class="dialog-block">
            <div class="dialog-block-title">系统建议</div>
            <ul class="suggestion-list">
              <li v-if="lastReport">建议优先复盘高风险维度，并再次完成一次测评观察风险是否下降。</li>
              <li v-if="wrongRecords.length">建议针对高频场景做专项训练，并在每次训练后复盘“遗漏风险点”。</li>
              <li>建议结合知识库帮助理解对应诈骗套路与防范要点。</li>
            </ul>
            <div class="dialog-actions">
              <el-button type="primary" @click="router.push('/assessment')">去做测评</el-button>
              <el-button @click="router.push('/train')">去训练</el-button>
              <el-button @click="openKnowledgeDialog">去看知识</el-button>
            </div>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="weaknessDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 我的知识弹窗 -->
    <el-dialog v-model="knowledgeDialogVisible" title="我的知识" width="860px">
      <el-alert
        v-if="knowledgeLoadError"
        :title="knowledgeLoadError"
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom: 12px"
      />
      <el-tabs v-model="knowledgeTab" class="dialog-tabs">
        <el-tab-pane :label="`已学习（${learnedKnowledgeItems.length}）`" name="learned">
          <el-skeleton v-if="knowledgeLoading" :rows="6" animated />
          <el-empty v-else-if="!learnedKnowledgeItems.length" description="暂无已学习知识" :image-size="90" />
          <el-table v-else :data="learnedKnowledgeItems" size="small" stripe class="dialog-table">
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column prop="title" label="标题" min-width="260" />
            <el-table-column prop="riskLevel" label="风险" width="90" />
            <el-table-column label="操作" width="120" align="center">
              <template #default="scope">
                <el-button type="primary" text size="small" @click="router.push(`/knowledge/${scope.row.id}`)">
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="`未学习（${unlearnedKnowledgeItems.length}）`" name="unlearned">
          <el-skeleton v-if="knowledgeLoading" :rows="6" animated />
          <el-empty v-else-if="!unlearnedKnowledgeItems.length" description="你已学完全部知识，太棒了！" :image-size="90" />
          <el-table v-else :data="unlearnedKnowledgeItems" size="small" stripe class="dialog-table">
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column prop="title" label="标题" min-width="260" />
            <el-table-column prop="riskLevel" label="风险" width="90" />
            <el-table-column label="操作" width="140" align="center">
              <template #default="scope">
                <el-button type="primary" text size="small" @click="router.push(`/knowledge/${scope.row.id}`)">
                  查看
                </el-button>
                <el-button text size="small" @click="markKnowledgeRead(scope.row.id)">标记已学</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="knowledgeDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 退出登录弹窗（统一 dialog 交互） -->
    <el-dialog v-model="logoutDialogVisible" title="退出登录" width="420px">
      <div>确定要退出登录吗？退出后强调以游客模式浏览，部分数据不会同步到账号。</div>
      <template #footer>
        <el-button @click="logoutDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmLogout">退出</el-button>
      </template>
    </el-dialog>

    <!-- 账号设置弹窗 -->
    <el-dialog
      v-model="accountDialogVisible"
      title="账号设置"
      width="420px"
    >
      <el-form
        ref="accountFormRef"
        :model="accountForm"
        :rules="accountRules"
        label-width="80px"
      >
        <el-form-item label="账号" prop="username">
          <el-input v-model="accountForm.username" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="accountForm.nickname" placeholder="请输入昵称" maxlength="20" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="accountDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAccount">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码弹窗 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="420px"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="90px"
      >
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import {
  UserFilled,
  Camera,
  Lock,
  DataAnalysis,
  Document,
  Star,
  Setting,
  SwitchButton,
} from '@element-plus/icons-vue';
import { useUserStore } from '../stores/user';
import { useAchievementStore } from '../store/achievement';
import { useKnowledgeStore } from '../store/knowledge';
import { useAssessmentStore } from '../store/assessment';
import { loadJson } from '../utils/storage';
import http from '../api/http';

const router = useRouter();
const userStore = useUserStore();
const achievementStore = useAchievementStore();
const knowledgeStore = useKnowledgeStore();
const assessmentStore = useAssessmentStore();

const isAuthed = computed(() => !!userStore.userId);

// 风险等级标签
const riskLabel = computed(() => {
  const map: Record<string, string> = {
    low: '低',
    medium: '中',
    high: '高',
  };
  return map[userStore.riskLevel] || '低';
});

const riskTagType = computed(() => {
  const map: Record<string, 'success' | 'warning' | 'danger'> = {
    low: 'success',
    medium: 'warning',
    high: 'danger',
  };
  return map[userStore.riskLevel] || 'success';
});

// 维度等级显示：low/medium/high -> 低/中/高（兼容大小写与数字）
const levelLabel = (input: any): string => {
  if (input == null) return '低';
  const s = String(input).toLowerCase();
  const map: Record<string, string> = {
    low: '低',
    medium: '中',
    high: '高',
    '0': '低',
    '1': '中',
    '2': '高',
    '3': '高',
    low_risk: '低',
    medium_risk: '中',
    high_risk: '高',
  };
  return map[s] ?? String(input);
};

// 经验值进度
const expProgress = computed(() => {
  return Math.round(achievementStore.expProgress);
});

const expProgressColor = computed(() => {
  if (expProgress.value < 30) return '#f5576c';
  if (expProgress.value < 70) return '#f093fb';
  return '#667eea';
});

// 头像背景色（根据等级变化）
const avatarBg = computed(() => {
  const level = achievementStore.level;
  if (level >= 5) return 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)';
  if (level >= 3) return 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)';
  return 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)';
});

// 已完成测评（最近 5 次）
const completedReports = computed(() => assessmentStore.reportHistory.slice(0, 5));

// 潜在危险画像：最近一次测评的高风险维度（取前三个）
const lastReport = computed(() => assessmentStore.lastReport);
const mainRisks = computed(() => {
  if (!lastReport.value) return [];
  return [...lastReport.value.dimensions].sort((a, b) => b.score - a.score).slice(0, 3);
});

// 错题记录（本地存储中记录的训练错题）
type WrongRecord = {
  at: string;
  scamType: string;
  missedPoints: string[];
};
const wrongRecords = ref<WrongRecord[]>([]);

function loadWrongRecords() {
  const uid = userStore.userId;
  const key = uid ? `af_training_records_u_${uid}` : 'af_training_records_guest';
  const list = loadJson<any[]>(key, []);
  wrongRecords.value = (list || [])
    .filter((r) => r && r.correct === false)
    .slice(0, 20)
    .map((r) => ({
      at: r.at,
      scamType: r.scamType,
      missedPoints: r.missedPoints || [],
    }));
}

// 页面加载时刷新成就 / 用户资料 / 错题本
onMounted(() => {
  if (isAuthed.value) {
    achievementStore.refresh();
    userStore.fetchUserInfo().catch(() => {
      // 忽略错误
    });
  }
  loadWrongRecords();
});

// 已获得成就列表
const unlockedAchievementsList = computed(() =>
  achievementStore.achievements.filter((a) => achievementStore.unlockedAchievements.has(a.id)),
);
const lockedAchievementsList = computed(() =>
  achievementStore.achievements.filter((a) => !achievementStore.unlockedAchievements.has(a.id)),
);

// 我的薄弱分析：测评维度 + 错题统计
const topRiskDimensions = computed(() => {
  if (!lastReport.value) return [];
  return [...lastReport.value.dimensions].sort((a, b) => b.score - a.score).slice(0, 3);
});

type CountItem = { name: string; count: number };
function toTopCounts(list: string[], topN: number): CountItem[] {
  const map = new Map<string, number>();
  list.forEach((x) => {
    const k = (x || '').trim();
    if (!k) return;
    map.set(k, (map.get(k) || 0) + 1);
  });
  return Array.from(map.entries())
    .sort((a, b) => b[1] - a[1])
    .slice(0, topN)
    .map(([name, count]) => ({ name, count }));
}

const topScamTypes = computed(() => toTopCounts(wrongRecords.value.map((r) => r.scamType), 3));
const topMissedPoints = computed(() => toTopCounts(wrongRecords.value.flatMap((r) => r.missedPoints || []), 5));

// ===== 统一弹窗交互（我的成就 / 我的薄弱 / 我的知识 / 退出登录） =====
const achievementDialogVisible = ref(false);
const weaknessDialogVisible = ref(false);
const knowledgeDialogVisible = ref(false);
const logoutDialogVisible = ref(false);

const achievementTab = ref<'unlocked' | 'locked'>('unlocked');
const knowledgeTab = ref<'learned' | 'unlearned'>('learned');

const openAchievementDialog = () => {
  if (isAuthed.value) {
    achievementStore.refresh();
  }
  achievementTab.value = 'unlocked';
  achievementDialogVisible.value = true;
};

const openWeaknessDialog = () => {
  weaknessDialogVisible.value = true;
};

type KnowledgeItem = {
  id: number;
  category: string;
  title: string;
  riskLevel?: string;
};
const knowledgeItems = ref<KnowledgeItem[]>([]);
const knowledgeLoading = ref(false);
const knowledgeLoadError = ref('');

async function ensureKnowledgeLoaded() {
  if (knowledgeLoading.value) return;
  if (knowledgeItems.value.length) return;
  knowledgeLoading.value = true;
  knowledgeLoadError.value = '';
  try {
    const resp = await http.get('/knowledge');
    knowledgeItems.value = resp.data || [];
  } catch {
    knowledgeLoadError.value = '知识库服务不可用，暂时无法加载完整知识列表（不影响其他功能）。';
    knowledgeItems.value = [
      {
        id: 1,
        category: '冒充客服',
        title: '假冒客服退款诈骗',
        riskLevel: '高',
      },
      {
        id: 2,
        category: '投资理财',
        title: '高收益投资陷阱',
        riskLevel: '高',
      },
    ];
  } finally {
    knowledgeLoading.value = false;
  }
}

const learnedKnowledgeItems = computed(() => knowledgeItems.value.filter((x) => knowledgeStore.isRead(x.id)));
const unlearnedKnowledgeItems = computed(() => knowledgeItems.value.filter((x) => !knowledgeStore.isRead(x.id)));

const openKnowledgeDialog = async () => {
  knowledgeTab.value = 'learned';
  knowledgeDialogVisible.value = true;
  await ensureKnowledgeLoaded();
};

const markKnowledgeRead = async (id: number) => {
  knowledgeStore.markRead(id);
  ElMessage.success('已标记为已学习');
  if (userStore.userId) {
    try {
      await http.post(`/knowledge/${id}/learn`, null, {
        params: { userId: userStore.userId, progress: 100 },
      });
    } catch {
      // 同步失败不阻断
    }
  }
};

const openLogoutDialog = () => {
  logoutDialogVisible.value = true;
};

const confirmLogout = () => {
  userStore.logout();
  ElMessage.success('已退出登录');
  logoutDialogVisible.value = false;
  router.push('/');
};

// 账号设置弹窗表单
const accountDialogVisible = ref(false);
const accountFormRef = ref<FormInstance | null>(null);
const accountForm = ref({
  username: '',
  nickname: '',
});

const accountRules: FormRules = {
  nickname: [
    { required: true, message: '昵称不能为空', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度为 2-20 个字符', trigger: 'blur' },
  ],
};

// 修改密码弹窗表单
const passwordDialogVisible = ref(false);
const passwordFormRef = ref<FormInstance | null>(null);
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
};

// 头像上传（无专用上传接口：使用 base64(dataURL) 存储并提交到后端 avatar 字段）
const avatarInputRef = ref<HTMLInputElement | null>(null);

const handleAvatarEdit = () => {
  if (!isAuthed.value) {
    ElMessage.info('请先登录后再更换头像');
    return;
  }
  avatarInputRef.value?.click();
};

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ''));
    reader.onerror = () => reject(new Error('读取图片失败'));
    reader.readAsDataURL(file);
  });
}

const onAvatarFileChange = async (e: Event) => {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  // 允许用户再次选择同一文件也能触发 change
  input.value = '';
  if (!file) return;

  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件');
    return;
  }
  // 控制体积，避免 base64 太大导致请求失败（可按需调整）
  const maxSizeMb = 2;
  if (file.size > maxSizeMb * 1024 * 1024) {
    ElMessage.error(`图片不能超过 ${maxSizeMb}MB`);
    return;
  }

  try {
    const dataUrl = await readFileAsDataUrl(file);
    await userStore.updateAvatar(dataUrl);
    ElMessage.success('头像已更新');
  } catch (err: any) {
    ElMessage.error(err?.message || '头像更新失败，请稍后重试');
  }
};

// 处理账号设置
const openAccountDialog = () => {
  if (!isAuthed.value) {
    ElMessage.info('请先登录后再修改账号信息');
    return;
  }
  accountForm.value.username = userStore.username || '';
  accountForm.value.nickname = userStore.name || '';
  accountDialogVisible.value = true;
};

const handleAccountSettings = () => {
  openAccountDialog();
};

// 处理修改密码
const handleChangePassword = () => {
  if (!isAuthed.value) {
    ElMessage.info('请先登录后再修改密码');
    return;
  }
  passwordForm.value.oldPassword = '';
  passwordForm.value.newPassword = '';
  passwordForm.value.confirmPassword = '';
  passwordDialogVisible.value = true;
};

// 保留函数名给其他地方复用（实际触发统一弹窗）
const handleLogout = () => {
  openLogoutDialog();
};

// 查看测评报告详情
const goReportDetail = (id?: string) => {
  if (!id) return;
  router.push(`/report/${id}`);
};

// 跳转登录
const goLogin = () => {
  router.push('/login');
};

// 跳转注册
const goRegister = () => {
  router.push('/login?tab=register');
};

// 提交账号设置
const submitAccount = () => {
  if (!accountFormRef.value) return;
  accountFormRef.value.validate(async (valid) => {
    if (!valid) return;
    try {
      await userStore.updateProfile(accountForm.value.nickname);
      ElMessage.success('账号信息已更新');
      accountDialogVisible.value = false;
    } catch (e: any) {
      ElMessage.error(e?.message || '更新账号信息失败，请稍后重试');
    }
  });
};

// 提交修改密码
const submitPassword = () => {
  if (!passwordFormRef.value) return;
  passwordFormRef.value.validate(async (valid) => {
    if (!valid) return;
    try {
      await userStore.changePassword(passwordForm.value.oldPassword, passwordForm.value.newPassword);
      ElMessage.success('密码修改成功，请使用新密码重新登录');
      passwordDialogVisible.value = false;
    } catch (e: any) {
      ElMessage.error(e?.message || '修改密码失败，请检查当前密码是否正确');
    }
  });
};

// 统一弹窗交互：不再直接跳转/滚动
</script>

<style scoped lang="scss">
.user-center-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 0 4px;
  animation: fadeIn 0.6s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 顶部用户信息区
.user-header-card {
  border-radius: 16px;
  border: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  overflow: hidden;

  :deep(.el-card__body) {
    padding: 32px;
  }
}

.user-header {
  display: flex;
  align-items: flex-start;
  gap: 32px;
}

.user-avatar-section {
  position: relative;
  flex-shrink: 0;
  cursor: pointer;
}

.user-avatar {
  border: 4px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.user-avatar-clickable {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.user-avatar-section:hover .user-avatar-clickable {
  transform: translateY(-2px);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.25);
}

.avatar-hint {
  margin-top: 8px;
  font-size: 12px;
  text-align: center;
  color: rgba(255, 255, 255, 0.9);
  user-select: none;
}

.avatar-edit-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  background: #fff;
  border: 2px solid #667eea;
  color: #667eea;

  &:hover {
    background: #f5f7fa;
  }
}

.hidden-file-input {
  display: none;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.user-name {
  margin: 0;
  font-size: 32px;
  font-weight: 800;
  color: #fff;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.user-title-row {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.user-exp-section {
  margin-top: 20px;
}

.exp-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
}

.exp-label {
  opacity: 0.9;
  font-weight: 500;
}

.exp-value {
  opacity: 0.9;
  font-weight: 600;
}

.exp-progress {
  :deep(.el-progress-bar__outer) {
    background-color: rgba(255, 255, 255, 0.2);
    border-radius: 6px;
  }

  :deep(.el-progress-bar__inner) {
    border-radius: 6px;
  }
}

// 卡片通用样式
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

// 快捷功能区
.quick-actions-card {
  border-radius: 16px;
  border: 1px solid var(--el-border-color-lighter);

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

// 成长数据概览区
.stats-section {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

.stats-card {
  border-radius: 16px;
  border: 1px solid var(--el-border-color-lighter);

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 480px) {
    grid-template-columns: 1fr;
  }
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--el-bg-color-page);
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

// 我的操作区
.actions-card {
  border-radius: 16px;
  border: 1px solid var(--el-border-color-lighter);

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.guest-tip-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.guest-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.user-actions-layout {
  display: grid;
  grid-template-columns: 1.2fr 1.3fr;
  gap: 20px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.user-actions {
  display: block;
  width: 100%;
}

.user-actions-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
  justify-items: stretch;
  align-items: stretch;
  grid-auto-rows: 54px;

  // Element Plus 默认相邻按钮会加 margin-left，在 grid 下会破坏等分布局
  :deep(.el-button + .el-button) {
    margin-left: 0 !important;
  }

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  @media (max-width: 480px) {
    grid-template-columns: 1fr;
  }
}

.user-achievements {
  border-left: 1px solid var(--el-border-color-lighter);
  padding-left: 16px;

  @media (max-width: 768px) {
    border-left: none;
    padding-left: 0;
    border-top: 1px solid var(--el-border-color-lighter);
    padding-top: 16px;
  }
}

.achievements-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
}

.achievements-title {
  font-size: 15px;
  font-weight: 600;
}

.achievements-count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.achievements-empty {
  font-size: 13px;
}

.achievements-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 210px;
  overflow-y: auto;
}

.achievement-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.achievement-icon {
  width: 24px;
  text-align: center;
}

.achievement-main {
  flex: 1;
  min-width: 0;
}

.achievement-name {
  font-size: 14px;
  font-weight: 600;
}

.action-btn {
  width: 100%;
  height: 54px;
  justify-content: center;
  padding: 14px 10px;
  font-size: 15px;
  border-radius: 10px;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
  }

  .el-icon {
    margin-right: 6px;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .user-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .user-name {
    font-size: 24px;
  }

  .user-title-row {
    justify-content: center;
  }
}

// 学习画像区域
.analysis-card {
  border-radius: 16px;
  border: 1px solid var(--el-border-color-lighter);

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  :deep(.el-card__body) {
    padding: 20px 24px 18px;
  }
}

.analysis-sections {
  display: grid;
  grid-template-columns: 2fr 1.7fr;
  gap: 20px;

  @media (max-width: 960px) {
    grid-template-columns: 1fr;
  }
}

.analysis-left,
.analysis-right {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.analysis-section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
}

.analysis-subtitle {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.analysis-table {
  margin-top: 4px;
}

.risk-dimension-item {
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--el-bg-color-page);
  border: 1px solid var(--el-border-color-lighter);
  & + & {
    margin-top: 8px;
  }
}

.risk-dimension-name {
  font-weight: 600;
  margin-bottom: 2px;
}

.risk-dimension-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.wrong-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.text-muted {
  color: var(--el-text-color-secondary);
}

/* ===== 弹窗通用样式（我的成就/我的薄弱/我的知识） ===== */
.dialog-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 12px;
  }
}

.dialog-table {
  :deep(.el-table__header-wrapper th) {
    background: var(--el-fill-color-light);
  }
}

/* ===== 我的薄弱分析：布局与视觉优化 ===== */
.dialog-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.dialog-block {
  background: var(--el-bg-color-page);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  padding: 14px 16px;
}

.dialog-block-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  letter-spacing: 0.2px;
}

.dialog-block-sub {
  margin-top: 6px;
  margin-bottom: 10px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.weakness-kpis {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}

.weakness-kpi {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  padding: 12px 14px;
  position: relative;
  overflow: hidden;
}

.weakness-kpi::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.weakness-kpi-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}

.weakness-kpi-value {
  font-size: 18px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  line-height: 1.2;
}

.weakness-kpi-sub {
  margin-top: 6px;
  font-size: 12px;
}

.weakness-tags-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}

.weakness-tags-col {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  padding: 12px 14px;
}

.tags-title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 10px;
  color: var(--el-text-color-primary);
}

.tag-item {
  margin-right: 6px;
  margin-bottom: 6px;
}

.suggestion-list {
  margin: 10px 0 0;
  padding-left: 18px;
  line-height: 1.8;
  color: var(--el-text-color-regular);

  li + li {
    margin-top: 6px;
  }
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
  flex-wrap: wrap;
}
</style>
