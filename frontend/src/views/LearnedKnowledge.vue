<template>
  <div class="learned-page">
    <!-- 顶部 Banner -->
    <div class="banner">
      <div class="banner-content">
        <div class="banner-left">
          <div class="banner-eyebrow">个人中心 · 学习记录</div>
          <h1 class="banner-title">已学习知识</h1>
          <p class="banner-subtitle">快速回顾你已经掌握的防诈知识点，并可一键跳转查看详情。</p>
          <div class="banner-actions">
            <el-button type="primary" size="large" @click="$router.push('/knowledge')">去知识库继续学习</el-button>
            <el-button size="large" plain @click="$router.push('/user-center')">返回个人中心</el-button>
          </div>
        </div>
        <div class="banner-right">
          <div class="stat">
            <div class="stat-label">已学习</div>
            <div class="stat-value">{{ learnedCount }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">总知识</div>
            <div class="stat-value">{{ totalCount }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">学习进度</div>
            <div class="stat-value">{{ progress }}%</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="content">
      <!-- 搜索与视图切换区域（仿知识页样式） -->
      <div class="search-section">
        <div class="search-wrapper">
          <el-input
            v-model="keyword"
            placeholder="搜索已学习知识（标题 / 摘要 / 分类）…"
            clearable
            size="large"
            class="search-input"
          >
            <template #prefix>
              <el-icon class="search-icon"><Search /></el-icon>
            </template>
          </el-input>
          <el-radio-group v-model="viewMode" size="large" class="view-toggle">
            <el-radio-button label="card">卡片</el-radio-button>
            <el-radio-button label="table">表格</el-radio-button>
          </el-radio-group>
        </div>
        <div class="search-stats">
          <span class="stats-text">
            共已学习 <strong>{{ learnedCount }}</strong> / {{ totalCount }} 条知识
          </span>
        </div>
      </div>

      <el-skeleton v-if="loading" :rows="6" animated />
      <el-alert v-else-if="error" :title="error" type="warning" show-icon style="margin-bottom: 12px" />

      <el-empty
        v-if="!loading && !filteredLearned.length"
        description="暂无已学习知识。先去知识库标记学习吧～"
        :image-size="120"
      >
        <el-button type="primary" @click="$router.push('/knowledge')">去知识库</el-button>
      </el-empty>

      <template v-else>
        <el-table
          v-if="viewMode === 'table'"
          :data="paginatedLearned"
          size="small"
          stripe
          class="table"
        >
          <el-table-column prop="category" label="分类" width="140" />
          <el-table-column prop="title" label="标题" min-width="260" />
          <el-table-column prop="riskLevel" label="风险" width="90" />
          <el-table-column label="操作" width="140" align="center">
            <template #default="scope">
              <el-button type="primary" text size="small" @click="goDetail(scope.row.id)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-row v-else :gutter="20" class="grid">
          <el-col v-for="item in paginatedLearned" :key="item.id" :span="8" :xs="24" :sm="12">
            <el-card shadow="hover" class="card" @click="goDetail(item.id)">
              <div class="card-head">
                <el-tag :type="getRiskTagType(item.riskLevel)" size="small" class="risk">
                  {{ item.riskLevel || '—' }}风险
                </el-tag>
                <el-tag type="success" size="small" effect="light">已学习</el-tag>
              </div>
              <div class="card-category">{{ item.category }}</div>
              <div class="card-title">{{ item.title }}</div>
              <div class="card-summary">{{ item.summary }}</div>
              <div class="card-footer">
                <span class="card-link">查看详情</span>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <div class="pager" v-if="filteredLearned.length > 0">
          <el-pagination
            v-model:current-page="pagination.currentPage"
            v-model:page-size="pagination.pageSize"
            :total="filteredLearned.length"
            :page-sizes="[6, 12, 18]"
            layout="total, sizes, prev, pager, next"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Search } from '@element-plus/icons-vue';
import http from '../api/http';
import { useKnowledgeStore } from '../store/knowledge';
import { useUserStore } from '../stores/user';

type KnowledgeItem = {
  id: number;
  category: string;
  title: string;
  summary: string;
  riskLevel?: string;
};

const router = useRouter();
const userStore = useUserStore();
const knowledgeStore = useKnowledgeStore();

const loading = ref(false);
const error = ref('');
const keyword = ref('');
const viewMode = ref<'card' | 'table'>('card');
const items = ref<KnowledgeItem[]>([]);

const learned = computed(() => items.value.filter((x) => knowledgeStore.isRead(x.id)));
const learnedCount = computed(() => learned.value.length);
const totalCount = computed(() => items.value.length);
const progress = computed(() => (totalCount.value ? Math.round((learnedCount.value / totalCount.value) * 100) : 0));

const filteredLearned = computed(() => {
  const k = keyword.value.trim();
  if (!k) return learned.value;
  return learned.value.filter((x) => x.title.includes(k) || x.summary.includes(k) || x.category.includes(k));
});

const pagination = ref({
  currentPage: 1,
  pageSize: 6,
});

const paginatedLearned = computed(() => {
  const start = (pagination.value.currentPage - 1) * pagination.value.pageSize;
  const end = start + pagination.value.pageSize;
  return filteredLearned.value.slice(start, end);
});

function getRiskTagType(level?: string): string {
  const types: Record<string, string> = {
    高: 'danger',
    中: 'warning',
    低: 'success',
  };
  return types[level || ''] || 'info';
}

function goDetail(id: number) {
  router.push(`/knowledge/${id}`);
}

function handlePageChange(page: number) {
  pagination.value.currentPage = page;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function handleSizeChange(size: number) {
  pagination.value.pageSize = size;
  pagination.value.currentPage = 1;
}

onMounted(async () => {
  // 恢复当前用户与已学集合
  userStore.hydrate();
  knowledgeStore.hydrate(userStore.userId);

  loading.value = true;
  error.value = '';
  try {
    const resp = await http.get('/knowledge');
    items.value = resp.data || [];
  } catch {
    error.value = '知识库服务不可用，暂时无法加载知识列表。';
    items.value = [];
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped lang="scss">
.learned-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 40px;
}

.banner {
  padding: 46px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 55%, #4facfe 100%);
  color: #fff;
  box-shadow: 0 18px 50px rgba(102, 126, 234, 0.25);
}

.banner-content {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 24px;
  align-items: center;
}

.banner-eyebrow {
  opacity: 0.9;
  font-weight: 600;
  letter-spacing: 0.6px;
  font-size: 13px;
  margin-bottom: 8px;
}

.banner-title {
  margin: 0 0 10px;
  font-size: 36px;
  font-weight: 900;
  letter-spacing: -0.4px;
}

.banner-subtitle {
  margin: 0 0 18px;
  opacity: 0.95;
  font-size: 14px;
  line-height: 1.7;
  max-width: 560px;
}

.banner-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.banner-right {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.stat {
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 14px;
  padding: 14px 14px;
  backdrop-filter: blur(10px);
}

.stat-label {
  opacity: 0.9;
  font-size: 12px;
  margin-bottom: 6px;
  font-weight: 600;
}

.stat-value {
  font-size: 24px;
  font-weight: 900;
  line-height: 1;
}

.content {
  max-width: 1200px;
  margin: 16px auto 0;
  padding: 0 20px;
}

// 仿知识页搜索布局
.search-section {
  margin-bottom: 18px;
  padding: 20px 20px 14px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

  .search-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 10px;

    .search-input {
      flex: 1;
      max-width: 720px;

      :deep(.el-input__wrapper) {
        border-radius: 999px;
        padding: 6px 16px;
        box-shadow: 0 4px 14px rgba(102, 126, 234, 0.18);
        transition: all 0.25s ease;
      }

      :deep(.el-input__wrapper.is-focus) {
        box-shadow: 0 6px 20px rgba(102, 126, 234, 0.25);
        transform: translateY(-1px);
      }

      .search-icon {
        color: #667eea;
        font-size: 18px;
      }
    }

    .view-toggle {
      white-space: nowrap;
    }
  }

  .search-stats {
    border-top: 1px solid #ebeef5;
    padding-top: 8px;

    .stats-text {
      font-size: 13px;
      color: #606266;

      strong {
        color: #667eea;
        font-weight: 600;
        margin: 0 2px;
      }
    }
  }
}

.grid {
  margin-top: 4px;
}

.card {
  cursor: pointer;
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  transition: all 0.25s ease;
  height: 100%;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 26px rgba(0, 0, 0, 0.12);
    border-color: rgba(102, 126, 234, 0.35);
  }
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.card-category {
  font-size: 12px;
  color: #667eea;
  font-weight: 700;
  margin-bottom: 6px;
}

.card-title {
  font-size: 16px;
  font-weight: 800;
  color: #303133;
  line-height: 1.45;
  margin-bottom: 10px;
}

.card-summary {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  min-height: 42px;
}

.card-footer {
  margin-top: 14px;
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
}

.card-link {
  color: #667eea;
  font-weight: 700;
  font-size: 13px;
}

.table {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.pager {
  margin-top: 18px;
  padding: 12px 16px 0;
  display: flex;
  justify-content: center;
}

@media (max-width: 960px) {
  .banner-content {
    grid-template-columns: 1fr;
  }
  .banner-right {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .banner-title {
    font-size: 26px;
  }
  .banner-right {
    grid-template-columns: 1fr;
  }
  .content {
    padding: 0 12px;
  }
  .search-section {
    padding: 14px 14px 10px;

    .search-wrapper {
      flex-direction: column;
      align-items: stretch;
    }

    .search-input {
      max-width: 100%;
    }
  }
}
</style>
