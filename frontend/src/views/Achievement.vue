<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useAchievementStore, type AchievementCategory } from '../store/achievement';
import { useUserStore } from '../stores/user';
import { ElMessage } from 'element-plus';
import {
  Trophy,
  Reading,
  DataAnalysis,
  Star,
  ArrowRight,
  Refresh,
} from '@element-plus/icons-vue';
import AchievementCard from '../components/AchievementCard.vue';
import AchievementDialog from '../components/AchievementDialog.vue';

const router = useRouter();
const achievementStore = useAchievementStore();
const userStore = useUserStore();

// Tab 分类
const activeCategory = ref<AchievementCategory>('training');
const categories = [
  { key: 'training' as AchievementCategory, label: '训练成就', icon: Trophy },
  { key: 'learning' as AchievementCategory, label: '学习成就', icon: Reading },
  { key: 'assessment' as AchievementCategory, label: '测评成就', icon: DataAnalysis },
  { key: 'special' as AchievementCategory, label: '特殊成就', icon: Star },
];

// 当前分类的成就列表
const currentAchievements = computed(() => {
  return achievementStore.achievementsByCategory(activeCategory.value);
});

// 成就统计
const stats = computed(() => {
  const total = achievementStore.achievements.length;
  const unlocked = achievementStore.unlockedAchievements.size;
  const progress = achievementStore.achievements.filter(
    (a) => a.status === 'progress'
  ).length;
  return { total, unlocked, progress };
});

// 选中的成就（用于弹窗）
const selectedAchievement = ref<string | null>(null);
const showDialog = ref(false);

// 打开成就详情
const openAchievementDetail = (id: string) => {
  selectedAchievement.value = id;
  showDialog.value = true;
};

// 关闭弹窗
const closeDialog = () => {
  showDialog.value = false;
  selectedAchievement.value = null;
};

// 跳转到相关页面
const goToPage = (category: AchievementCategory) => {
  const routeMap: Record<AchievementCategory, string> = {
    training: '/train',
    learning: '/knowledge',
    assessment: '/assessment',
    special: '/',
  };
  router.push(routeMap[category] || '/');
};

// 刷新成就
const refreshAchievements = () => {
  achievementStore.refresh();
  ElMessage.success('成就数据已刷新');
};

// 监听经验值变化，显示升级提示
watch(
  () => achievementStore.level,
  (newLevel, oldLevel) => {
    if (newLevel > oldLevel) {
      ElMessage({
        message: `🎉 恭喜升级！当前等级：Lv.${newLevel} ${achievementStore.levelTitle}`,
        type: 'success',
        duration: 3000,
      });
    }
  }
);

onMounted(() => {
  achievementStore.hydrate();
  achievementStore.refresh();
});
</script>

<template>
  <div class="achievement-page">
    <!-- 顶部成长概览区 -->
    <el-card class="overview-card" shadow="hover">
      <div class="overview-content">
        <div class="level-section">
          <div class="level-badge">
            <div class="level-number">Lv.{{ achievementStore.level }}</div>
            <div class="level-title">{{ achievementStore.levelTitle }}</div>
          </div>
          <div class="exp-section">
            <div class="exp-header">
              <span class="exp-label">经验值</span>
              <span class="exp-value">
                {{ achievementStore.exp }} / {{ achievementStore.expForNextLevel }}
              </span>
            </div>
            <el-progress
              :percentage="achievementStore.expProgress"
              :stroke-width="16"
              :show-text="false"
              class="exp-progress"
            >
              <template #default="{ percentage }">
                <span class="exp-percentage">{{ Math.round(percentage) }}%</span>
              </template>
            </el-progress>
            <div class="exp-footer">
              <span class="total-exp">累计经验：{{ achievementStore.totalExp }}</span>
            </div>
          </div>
        </div>
        <div class="stats-section">
          <div class="stat-item">
            <div class="stat-value">{{ stats.unlocked }}</div>
            <div class="stat-label">已解锁</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ stats.progress }}</div>
            <div class="stat-label">进行中</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">总成就</div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 成就分类 Tab -->
    <el-card class="category-card" shadow="hover">
      <el-tabs v-model="activeCategory" class="achievement-tabs">
        <el-tab-pane
          v-for="cat in categories"
          :key="cat.key"
          :name="cat.key"
          :label="cat.label"
        >
          <template #label>
            <span class="tab-label">
              <el-icon><component :is="cat.icon" /></el-icon>
              <span>{{ cat.label }}</span>
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <!-- 成就列表 -->
      <div class="achievements-grid">
        <AchievementCard
          v-for="achievement in currentAchievements"
          :key="achievement.id"
          :achievement="achievement"
          @click="openAchievementDetail(achievement.id)"
        />
      </div>

      <!-- 空状态 -->
      <el-empty
        v-if="currentAchievements.length === 0"
        description="暂无成就"
        :image-size="100"
      />
    </el-card>

    <!-- 底部操作区 -->
    <el-card class="actions-card" shadow="never">
      <div class="actions-content">
        <el-button type="primary" @click="goToPage(activeCategory)">
          <el-icon><ArrowRight /></el-icon>
          去完成{{ categories.find((c) => c.key === activeCategory)?.label }}
        </el-button>
        <el-button @click="refreshAchievements">
          <el-icon><Refresh /></el-icon>
          刷新成就
        </el-button>
      </div>
    </el-card>

    <!-- 成就详情弹窗 -->
    <AchievementDialog
      v-model="showDialog"
      :achievement-id="selectedAchievement"
      @close="closeDialog"
    />
  </div>
</template>

<style scoped lang="scss">
.achievement-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

// 顶部概览区
.overview-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;

  :deep(.el-card__body) {
    padding: 30px;
  }

  .overview-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 40px;
  }

  .level-section {
    display: flex;
    align-items: center;
    gap: 30px;
    flex: 1;
  }

  .level-badge {
    text-align: center;
    padding: 20px 30px;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 16px;
    backdrop-filter: blur(10px);

    .level-number {
      font-size: 48px;
      font-weight: 700;
      line-height: 1;
      margin-bottom: 8px;
    }

    .level-title {
      font-size: 18px;
      opacity: 0.9;
    }
  }

  .exp-section {
    flex: 1;
    min-width: 300px;

    .exp-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .exp-label {
        font-size: 16px;
        opacity: 0.9;
      }

      .exp-value {
        font-size: 18px;
        font-weight: 600;
      }
    }

    .exp-progress {
      margin-bottom: 8px;

      :deep(.el-progress-bar__outer) {
        background: rgba(255, 255, 255, 0.2);
      }

      :deep(.el-progress-bar__inner) {
        background: linear-gradient(90deg, #ffd700, #ffed4e);
      }

      .exp-percentage {
        color: white;
        font-weight: 600;
        font-size: 14px;
      }
    }

    .exp-footer {
      text-align: right;

      .total-exp {
        font-size: 14px;
        opacity: 0.8;
      }
    }
  }

  .stats-section {
    display: flex;
    gap: 30px;

    .stat-item {
      text-align: center;
      padding: 15px 25px;
      background: rgba(255, 255, 255, 0.15);
      border-radius: 12px;
      backdrop-filter: blur(10px);

      .stat-value {
        font-size: 32px;
        font-weight: 700;
        line-height: 1;
        margin-bottom: 8px;
      }

      .stat-label {
        font-size: 14px;
        opacity: 0.9;
      }
    }
  }
}

// 分类卡片
.category-card {
  :deep(.el-card__body) {
    padding: 20px;
  }

  .achievement-tabs {
    margin-bottom: 24px;

    :deep(.el-tabs__header) {
      margin: 0 0 20px 0;
    }

    :deep(.el-tabs__item) {
      font-size: 16px;
      padding: 0 24px;
    }

    .tab-label {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .achievements-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
  }
}

// 底部操作区
.actions-card {
  background: transparent;
  border: none;

  :deep(.el-card__body) {
    padding: 20px;
  }

  .actions-content {
    display: flex;
    justify-content: center;
    gap: 16px;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .achievement-page {
    padding: 10px;
  }

  .overview-content {
    flex-direction: column;
    gap: 20px !important;
  }

  .level-section {
    flex-direction: column;
    width: 100%;
  }

  .exp-section {
    min-width: auto;
    width: 100%;
  }

  .stats-section {
    width: 100%;
    justify-content: space-around;
    gap: 10px;
  }

  .achievements-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
