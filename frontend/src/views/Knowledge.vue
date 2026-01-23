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

    <!-- 知识分类导航区 -->
    <div class="category-nav">
      <el-card shadow="hover" 
               v-for="cat in categories" 
               :key="cat"
               :class="['category-card', { active: selectedCategory === cat }]"
               @click="selectedCategory = cat">
        <div class="category-content">
          <span class="category-name">{{ cat }}</span>
          <el-tag :type="getCategoryTagType(cat)" size="small">
            {{ getCategoryCount(cat) }} 条
          </el-tag>
        </div>
      </el-card>
      <el-card shadow="hover" 
               :class="['category-card', { active: selectedCategory === '' }]"
               @click="selectedCategory = ''">
        <div class="category-content">
          <span class="category-name">全部</span>
          <el-tag type="info" size="small">{{ totalCount }} 条</el-tag>
        </div>
      </el-card>
    </div>

    <!-- 知识内容列表区 -->
    <div class="knowledge-list">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索知识点..."
        clearable
        class="search-input"
        prefix-icon="Search"
      />
      
      <el-row v-if="loading" :gutter="20">
        <el-col v-for="i in 6" :key="i" :span="8">
          <el-skeleton :rows="4" animated />
        </el-col>
      </el-row>

      <el-alert v-if="error" :title="error" type="warning" show-icon style="margin-bottom: 20px" />

      <el-empty v-if="!filteredItems.length && !loading" description="暂无匹配内容" />

      <el-row v-else :gutter="20">
        <el-col v-for="item in filteredItems" :key="item.id" :span="8" :xs="24" :sm="12" class="knowledge-col">
          <el-card shadow="hover" class="knowledge-card">
            <div class="card-header">
              <el-tag :type="getRiskTagType(item.riskLevel)" size="small" class="risk-tag">
                {{ item.riskLevel }}风险
              </el-tag>
              <el-tag 
                v-if="knowledgeStore.isRead(item.id)" 
                type="success" 
                size="small"
                class="learned-tag">
                已学习
              </el-tag>
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import http from '../api/http';
import { useKnowledgeStore } from '../store/knowledge';
import { useUserStore } from '../stores/user';
import { ElMessage } from 'element-plus';

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
const searchKeyword = ref('');
const selectedCategory = ref('');
const items = ref<KnowledgeItem[]>([]);
const loading = ref(false);
const error = ref('');
const knowledgeStore = useKnowledgeStore();
const userStore = useUserStore();

// 初始化 store
onMounted(() => {
  knowledgeStore.hydrate();
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
  knowledgeStore.toggleRead(id);

  if (willMark && userStore.userId) {
    try {
      await http.post(`/knowledge/${id}/learn`, null, {
        params: { userId: userStore.userId, progress: 100 },
      });
      ElMessage.success('已同步学习记录');
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

// 分类导航区
.category-nav {
  max-width: 1200px;
  margin: 0 auto 30px;
  padding: 0 20px;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  justify-content: center;

  .category-card {
    cursor: pointer;
    transition: all 0.3s;
    border: 2px solid transparent;
    min-width: 140px;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
    }

    &.active {
      border-color: #667eea;
      background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
    }

    .category-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 8px;

      .category-name {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }
    }
  }
}

// 知识列表区
.knowledge-list {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;

  .search-input {
    margin-bottom: 24px;
    max-width: 400px;
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
      align-items: center;
      margin-bottom: 12px;
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

  .category-nav {
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 10px;
  }

  .knowledge-list {
    .knowledge-col {
      margin-bottom: 16px;
    }
  }
}
</style>
