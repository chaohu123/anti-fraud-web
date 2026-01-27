<template>
  <div class="knowledge-page">
    <!-- 顶部 Banner -->
    <div class="knowledge-banner">
      <div class="banner-content">
        <h1 class="banner-title">防骗知识库</h1>
        <p class="banner-subtitle">用于教学与风险防范</p>
        <div class="learning-stats">
          <div class="stat-item">
            <div class="stat-value">{{ knowledgeStore.readCount }}</div>
            <div class="stat-label">已学习</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ totalCount }}</div>
            <div class="stat-label">总知识数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ learningProgress }}%</div>
            <div class="stat-label">学习进度</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 知识内容列表区 -->
    <div class="knowledge-list">
      <!-- 搜索区域 -->
      <div class="search-section">
        <div class="search-wrapper">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索知识点、标题、摘要..."
            clearable
            class="search-input"
            size="large"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon class="search-icon"><Search /></el-icon>
            </template>
            <template #suffix>
              <el-button 
                type="primary" 
                :icon="Search" 
                circle 
                size="small"
                @click="handleSearch"
                class="search-button"
              />
            </template>
          </el-input>
          <el-dropdown 
            trigger="click" 
            @command="handleCategorySelect"
            class="filter-dropdown"
          >
            <el-button size="large" class="filter-button">
              <el-icon><Filter /></el-icon>
              <span class="filter-text">
                {{ selectedCategory ? selectedCategory : '筛选' }}
              </span>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item 
                  :command="''"
                  :class="{ 'is-selected': selectedCategory === '' }"
                >
                  <span>全部</span>
                  <el-tag type="info" size="small" style="margin-left: 8px;">
                    {{ totalCount }} 条
                  </el-tag>
                </el-dropdown-item>
                <el-dropdown-item 
                  v-for="cat in categories" 
                  :key="cat"
                  :command="cat"
                  :class="{ 'is-selected': selectedCategory === cat }"
                >
                  <span>{{ cat }}</span>
                  <el-tag :type="getCategoryTagType(cat)" size="small" style="margin-left: 8px;">
                    {{ getCategoryCount(cat) }} 条
                  </el-tag>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="search-stats" v-if="!loading">
          <span class="stats-text">
            共找到 <strong>{{ filteredItems.length }}</strong> 条结果
          </span>
        </div>
      </div>
      
      <el-row v-if="loading" :gutter="20">
        <el-col v-for="i in 6" :key="i" :span="8">
          <el-skeleton :rows="4" animated />
        </el-col>
      </el-row>

      <el-alert v-if="error" :title="error" type="warning" show-icon style="margin-bottom: 20px" />

      <el-empty v-if="!paginatedItems.length && !loading" description="暂无匹配内容" />

      <el-row v-else :gutter="20">
        <el-col v-for="item in paginatedItems" :key="item.id" :span="8" :xs="24" :sm="12" class="knowledge-col">
          <el-card shadow="hover" class="knowledge-card">
            <div class="card-header">
              <div class="header-left">
                <el-tag 
                  v-if="knowledgeStore.isRead(item.id)" 
                  type="success" 
                  size="small"
                  class="learned-tag">
                  已学习
                </el-tag>
              </div>
              <div class="header-right">
                <el-tag :type="getRiskTagType(item.riskLevel)" size="small" class="risk-tag">
                  {{ item.riskLevel }}风险
                </el-tag>
              </div>
            </div>
            
            <div class="card-category">{{ item.category }}</div>
            <h3 class="card-title">{{ item.title }}</h3>
            <p class="card-summary">{{ item.summary }}</p>
            
            <div class="card-prevention">
              <div class="prevention-label">防范要点：</div>
              <ul class="prevention-list">
                <li v-for="(tip, index) in item.preventionTips?.slice(0, 2)" :key="index">
                  {{ tip }}
                </li>
              </ul>
            </div>

            <div class="card-footer">
              <el-button 
                type="primary" 
                size="small"
                @click="goToDetail(item.id)">
                查看详情
              </el-button>
              <el-button 
                link 
                :type="knowledgeStore.isRead(item.id) ? 'success' : 'info'"
                size="small"
                @click="toggleLearn(item.id)">
                {{ knowledgeStore.isRead(item.id) ? '已学习' : '标记已学' }}
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 分页组件 -->
      <div class="pagination-wrapper" v-if="filteredItems.length > 0 && !loading">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :total="filteredItems.length"
          :page-sizes="[6, 12, 18]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import http from '../api/http';
import { useKnowledgeStore } from '../store/knowledge';
import { useUserStore } from '../stores/user';
import { useAchievementStore } from '../store/achievement';
import { ElMessage } from 'element-plus';
import { Search, Filter, ArrowDown } from '@element-plus/icons-vue';

type KnowledgeItem = {
  id: number;
  category: string;
  title: string;
  summary: string;
  riskLevel?: string;
  preventionTips?: string[];
  commonTactics?: string[];
  cases?: string[];
  content?: string;
  relatedTraining?: string;
};

const router = useRouter();
const route = useRoute();
const searchKeyword = ref('');
const selectedCategory = ref('');
const items = ref<KnowledgeItem[]>([]);
const loading = ref(false);
const error = ref('');
const knowledgeStore = useKnowledgeStore();
const userStore = useUserStore();
const achievementStore = useAchievementStore();

// 分页配置 - 6的倍数，最多18条
const pagination = ref({
  currentPage: 1,
  pageSize: 6,
});

// 初始化 store（先恢复用户，再按 userId 分桶加载“已学习”与成就），并处理来自报告页的筛选参数
onMounted(() => {
  userStore.hydrate();
  knowledgeStore.hydrate(userStore.userId);
  achievementStore.hydrate(userStore.userId);

  const { category, keyword, dimension } = route.query as {
    category?: string;
    keyword?: string;
    dimension?: string;
  };
  if (category && typeof category === 'string') {
    selectedCategory.value = category;
  }
  const kwSource = typeof keyword === 'string' ? keyword : typeof dimension === 'string' ? dimension : '';
  if (kwSource) {
    searchKeyword.value = kwSource;
  }
});

const categories = computed(() => {
  const cats = Array.from(new Set(items.value.map((i) => i.category)));
  return cats.sort();
});

const totalCount = computed(() => items.value.length);

const learningProgress = computed(() => {
  if (totalCount.value === 0) return 0;
  return Math.round((knowledgeStore.readCount / totalCount.value) * 100);
});

const filteredItems = computed(() => {
  return items.value.filter((item) => {
    const matchCategory = !selectedCategory.value || item.category === selectedCategory.value;
    const matchKeyword = !searchKeyword.value || 
      item.title.includes(searchKeyword.value) || 
      item.summary.includes(searchKeyword.value) ||
      item.category.includes(searchKeyword.value);
    return matchCategory && matchKeyword;
  });
});

// 分页后的数据
const paginatedItems = computed(() => {
  const start = (pagination.value.currentPage - 1) * pagination.value.pageSize;
  const end = start + pagination.value.pageSize;
  return filteredItems.value.slice(start, end);
});

// 搜索处理
function handleSearch() {
  pagination.value.currentPage = 1;
}

// 分类筛选处理
function handleCategorySelect(category: string) {
  selectedCategory.value = category;
  pagination.value.currentPage = 1;
}

// 分页变化处理
function handlePageChange(page: number) {
  pagination.value.currentPage = page;
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function handleSizeChange(size: number) {
  pagination.value.pageSize = size;
  pagination.value.currentPage = 1;
}

// 监听分类变化，重置分页
watch(selectedCategory, () => {
  pagination.value.currentPage = 1;
});

// 监听搜索关键词变化，重置分页
watch(searchKeyword, () => {
  pagination.value.currentPage = 1;
});

function getCategoryCount(category: string): number {
  return items.value.filter((item) => item.category === category).length;
}

function getCategoryTagType(category: string): string {
  const types: Record<string, string> = {
    '冒充客服': 'danger',
    '投资理财': 'warning',
    '冒充熟人': 'warning',
    '网络钓鱼': 'danger',
    '情感诈骗': 'danger',
    '冒充公检法': 'danger',
  };
  return types[category] || 'info';
}

function getRiskTagType(level?: string): string {
  const types: Record<string, string> = {
    '高': 'danger',
    '中': 'warning',
    '低': 'success',
  };
  return types[level || ''] || 'info';
}

function goToDetail(id: number) {
  router.push(`/knowledge/${id}`);
}

async function toggleLearn(id: number) {
  const willMark = !knowledgeStore.isRead(id);
  const wasAlreadyRead = knowledgeStore.isRead(id);
  const wasEverRead = knowledgeStore.hasEverRead(id);

  knowledgeStore.toggleRead(id);

  // 如果是“第一次”学习该知识点（无论之后是否取消过标记），只增加一次经验
  if (willMark && !wasEverRead) {
    const expResult = achievementStore.addExp(50);
    if (expResult.leveledUp) {
      ElMessage.success({
        message: `恭喜升级！获得50经验值，当前等级：${expResult.newLevel}级`,
        duration: 3000,
      });
    } else {
      ElMessage.success(`已标记为已学习，获得50经验值`);
    }
    // 检查成就
    achievementStore.checkAchievements();
  }

  if (willMark && userStore.userId) {
    try {
      await http.post(`/knowledge/${id}/learn`, null, {
        params: { userId: userStore.userId, progress: 100 },
      });
      if (wasAlreadyRead) {
        ElMessage.success('已同步学习记录');
      }
    } catch {
      ElMessage.warning('同步学习记录失败（不影响本地标记）');
    }
  }
}

onMounted(async () => {
  loading.value = true;
  error.value = '';
  try {
    const resp = await http.get('/knowledge');
    items.value = resp.data || [];
    // 登录状态下，同步后端学习进度（包括已完成的知识ID）
    if (userStore.userId) {
      try {
        const progressResp = await http.get(`/knowledge/progress/${userStore.userId}`);
        const finishedIds = progressResp.data?.finishedArticleIds || [];
        if (Array.isArray(finishedIds) && finishedIds.length) {
          knowledgeStore.syncFinishedFromBackend(finishedIds.map((x: any) => Number(x)));
        }
      } catch {
        // 背景同步失败可以忽略，不影响页面使用
      }
    }
  } catch (err) {
    error.value = '知识库服务不可用，展示本地示例。';
    items.value = [
      {
        id: 1,
        category: '冒充客服',
        title: '假冒客服退款诈骗',
        summary: '诈骗分子冒充电商、银行等客服，以退款、账户异常等为由，诱导用户提供验证码或点击钓鱼链接。',
        riskLevel: '高',
        preventionTips: ['官方客服不会主动索要验证码', '退款通常原路返回，无需额外操作'],
      },
      {
        id: 2,
        category: '投资理财',
        title: '高收益投资陷阱',
        summary: '以高额回报为诱饵，诱导用户投资虚假平台，最终卷款跑路。',
        riskLevel: '高',
        preventionTips: ['高收益必然伴随高风险', '核实平台资质和监管信息'],
      },
    ];
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped lang="scss">
.knowledge-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 40px;
}

// 顶部 Banner
.knowledge-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 60px 20px;
  margin-bottom: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

  .banner-content {
    max-width: 1200px;
    margin: 0 auto;
    text-align: center;
  }

  .banner-title {
    font-size: 42px;
    font-weight: 700;
    margin: 0 0 12px 0;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  }

  .banner-subtitle {
    font-size: 18px;
    margin: 0 0 30px 0;
    opacity: 0.95;
  }

  .learning-stats {
    display: flex;
    justify-content: center;
    gap: 60px;
    margin-top: 30px;

    .stat-item {
      text-align: center;

      .stat-value {
        color: white;
        font-size: 32px;
        font-weight: 700;
        margin-bottom: 8px;
        text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
      }

      .stat-label {
        color: rgba(255, 255, 255, 0.9);
        font-size: 14px;
      }
    }
  }
}


// 知识列表区
.knowledge-list {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;

  // 搜索区域
  .search-section {
    margin-bottom: 30px;
    padding: 24px;
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    transition: all 0.3s;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
    }

    .search-wrapper {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;

      .search-input {
        flex: 1;
        max-width: 600px;

        :deep(.el-input__wrapper) {
          border-radius: 24px;
          padding: 8px 16px;
          box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15);
          transition: all 0.3s;

          &:hover {
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.25);
          }

          &.is-focus {
            box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
          }
        }

        .search-icon {
          color: #667eea;
          font-size: 18px;
        }

        .search-button {
          margin-right: 4px;
        }
      }

      .filter-dropdown {
        .filter-button {
          border-radius: 24px;
          padding: 8px 20px;
          box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15);
          transition: all 0.3s;
          border: 1px solid #dcdfe6;

          &:hover {
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.25);
            border-color: #667eea;
          }

          .filter-text {
            margin: 0 8px;
            font-weight: 500;
          }
        }
      }
    }

    // 下拉菜单选中状态样式
    :deep(.el-dropdown-menu__item) {
      &.is-selected {
        color: #667eea;
        background-color: rgba(102, 126, 234, 0.1);
        font-weight: 600;
      }
    }

    .search-stats {
      display: flex;
      align-items: center;
      padding-top: 8px;
      border-top: 1px solid #ebeef5;

      .stats-text {
        font-size: 14px;
        color: #606266;

        strong {
          color: #667eea;
          font-weight: 600;
          font-size: 16px;
        }
      }
    }
  }

  .knowledge-col {
    margin-bottom: 20px;
  }

  .knowledge-card {
    height: 100%;
    display: flex;
    flex-direction: column;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 12px;
      
      .header-left {
        .learned-tag {
          margin-right: 4px;
        }
      }

      .header-right {
        margin-left: auto;

        .risk-tag {
          font-weight: 600;
        }
      }
    }

    .card-category {
      font-size: 12px;
      color: #667eea;
      font-weight: 600;
      margin-bottom: 8px;
    }

    .card-title {
      font-size: 18px;
      font-weight: 700;
      color: #303133;
      margin: 0 0 12px 0;
      line-height: 1.4;
    }

    .card-summary {
      font-size: 14px;
      color: #606266;
      line-height: 1.6;
      margin: 0 0 16px 0;
      flex: 1;
    }

    .card-prevention {
      background: #f8f9fa;
      padding: 12px;
      border-radius: 8px;
      margin-bottom: 16px;

      .prevention-label {
        font-size: 12px;
        color: #909399;
        margin-bottom: 6px;
        font-weight: 600;
      }

      .prevention-list {
        margin: 0;
        padding-left: 20px;
        font-size: 13px;
        color: #606266;
        line-height: 1.8;

        li {
          margin-bottom: 4px;
        }
      }
    }

    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: auto;
      padding-top: 12px;
      border-top: 1px solid #ebeef5;
    }
  }

  // 分页组件
  .pagination-wrapper {
    margin-top: 40px;
    padding: 24px;
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    display: flex;
    justify-content: center;

    :deep(.el-pagination) {
      .el-pagination__total {
        color: #606266;
        font-weight: 500;
      }

      .btn-prev,
      .btn-next {
        background: #f5f7fa;
        border-radius: 6px;
        transition: all 0.3s;

        &:hover {
          background: #667eea;
          color: white;
        }
      }

      .el-pager li {
        border-radius: 6px;
        transition: all 0.3s;

        &:hover {
          background: rgba(102, 126, 234, 0.1);
          color: #667eea;
        }

        &.is-active {
          background: #667eea;
          color: white;
        }
      }

      .el-pagination__jump {
        .el-input__wrapper {
          border-radius: 6px;
        }
      }
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .knowledge-banner {
    padding: 40px 20px;

    .banner-title {
      font-size: 32px;
    }

    .learning-stats {
      flex-direction: column;
      gap: 20px;
    }
  }

  .knowledge-list {
    .knowledge-col {
      margin-bottom: 16px;
    }

    .search-section {
      padding: 16px;

      .search-wrapper {
        flex-direction: column;
        gap: 12px;

        .search-input {
          max-width: 100%;
        }

        .filter-dropdown {
          width: 100%;

          .filter-button {
            width: 100%;
            justify-content: center;
          }
        }
      }
    }

    .pagination-wrapper {
      padding: 16px;
      margin-top: 24px;

      :deep(.el-pagination) {
        .el-pagination__sizes,
        .el-pagination__jump {
          display: none;
        }
      }
    }
  }
}
</style>
