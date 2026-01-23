<script setup lang="ts">
import { ref, onMounted } from 'vue';
import http from '../../api/http';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search } from '@element-plus/icons-vue';

const tableData = ref<any[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const currentUser = ref<any>(null);
const userReport = ref<any>(null);

// 搜索和筛选
const searchKeyword = ref('');
const filterRiskLevel = ref('');

const pagination = ref({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

const fetchUsers = async () => {
  loading.value = true;
  try {
    const params: any = {
      page: pagination.value.currentPage,
      size: pagination.value.pageSize,
    };
    
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value;
    }
    if (filterRiskLevel.value) {
      params.riskLevel = filterRiskLevel.value;
    }

    const resp = await http.get('/admin/users', { params });
    tableData.value = resp.data?.content || [];
    pagination.value.total = resp.data?.total || 0;
  } catch (error) {
    ElMessage.warning('无法加载用户列表');
    tableData.value = [];
    pagination.value.total = 0;
  } finally {
    loading.value = false;
  }
};

const handleViewReport = async (row: any) => {
  currentUser.value = row;
  try {
    const resp = await http.get(`/admin/users/${row.id}/report`);
    userReport.value = resp.data;
    dialogVisible.value = true;
  } catch (error) {
    ElMessage.warning('无法加载用户报告');
  }
};

const handlePageChange = (page: number) => {
  pagination.value.currentPage = page;
  fetchUsers();
};

const handleSizeChange = (size: number) => {
  pagination.value.pageSize = size;
  pagination.value.currentPage = 1;
  fetchUsers();
};

const getRiskLevelLabel = (level: string) => {
  const map: Record<string, { label: string; type: string }> = {
    LOW: { label: '低风险', type: 'success' },
    MEDIUM: { label: '中风险', type: 'warning' },
    HIGH: { label: '高风险', type: 'danger' },
  };
  return map[level] || { label: level, type: 'info' };
};

const riskLevelOptions = [
  { label: '低风险', value: 'LOW' },
  { label: '中风险', value: 'MEDIUM' },
  { label: '高风险', value: 'HIGH' },
];

// 搜索
const handleSearch = () => {
  pagination.value.currentPage = 1;
  fetchUsers();
};

// 重置筛选
const handleReset = () => {
  searchKeyword.value = '';
  filterRiskLevel.value = '';
  pagination.value.currentPage = 1;
  fetchUsers();
};

// 筛选变化时自动刷新
const handleFilterChange = () => {
  pagination.value.currentPage = 1;
  fetchUsers();
};

onMounted(() => {
  fetchUsers();
});
</script>

<template>
  <div class="user-management">
    <el-card>
      <template #header>
        <span>用户数据管理</span>
      </template>

      <!-- 搜索和筛选区域 -->
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名、昵称或邮箱"
          clearable
          style="width: 300px; margin-right: 12px"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="filterRiskLevel"
          placeholder="风险等级"
          clearable
          style="width: 150px; margin-right: 12px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="item in riskLevelOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" show-overflow-tooltip />
        <el-table-column prop="level" label="等级" width="100">
          <template #default="{ row }">
            <el-tag>Lv.{{ row.level || 1 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="exp" label="经验值" width="100" />
        <el-table-column prop="riskLevel" label="风险等级" width="120">
          <template #default="{ row }">
            <el-tag :type="getRiskLevelLabel(row.riskLevel || 'LOW').type">
              {{ getRiskLevelLabel(row.riskLevel || 'LOW').label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180">
          <template #default="{ row }">
            {{ row.createdAt ? new Date(row.createdAt).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleViewReport(row)">
              查看报告
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 用户报告对话框 -->
    <el-dialog v-model="dialogVisible" title="用户报告" width="800px">
      <div v-if="userReport" class="user-report">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户名">{{ currentUser?.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ currentUser?.nickname }}</el-descriptions-item>
          <el-descriptions-item label="等级">Lv.{{ currentUser?.level || 1 }}</el-descriptions-item>
          <el-descriptions-item label="经验值">{{ currentUser?.exp || 0 }}</el-descriptions-item>
          <el-descriptions-item label="风险等级">
            <el-tag :type="getRiskLevelLabel(currentUser?.riskLevel || 'LOW').type">
              {{ getRiskLevelLabel(currentUser?.riskLevel || 'LOW').label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">
            {{ currentUser?.createdAt ? new Date(currentUser.createdAt).toLocaleString() : '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <h3>训练统计</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="训练次数">
            {{ userReport.trainingCount || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="正确次数">
            {{ userReport.correctCount || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="正确率">
            {{
              userReport.trainingCount > 0
                ? ((userReport.correctCount / userReport.trainingCount) * 100).toFixed(2)
                : 0
            }}%
          </el-descriptions-item>
          <el-descriptions-item label="平均用时">
            {{ userReport.avgTimeSpent || 0 }}ms
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <h3>测评统计</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="完成测评次数">
            {{ userReport.assessmentCount || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="最新测评时间">
            {{
              userReport.lastAssessmentTime
                ? new Date(userReport.lastAssessmentTime).toLocaleString()
                : '-'
            }}
          </el-descriptions-item>
          <el-descriptions-item label="信息保护意识得分">
            {{ userReport.infoScore || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="金融安全意识得分">
            {{ userReport.financeScore || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="心理风险倾向得分">
            {{ userReport.psychScore || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="综合风险评分">
            {{ userReport.totalScore || 0 }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <h3>成就统计</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="获得成就数">
            {{ userReport.achievementCount || 0 }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <div v-else class="loading-placeholder">
        <el-skeleton :rows="5" animated />
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.user-management {
  .filter-bar {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
    flex-wrap: wrap;
    gap: 8px;
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .user-report {
    h3 {
      margin: 16px 0;
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }

    .loading-placeholder {
      padding: 20px;
    }
  }
}
</style>
