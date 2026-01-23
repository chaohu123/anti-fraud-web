<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import http from '../../api/http';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { Search } from '@element-plus/icons-vue';

const tableData = ref<any[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const dialogTitle = ref('新增成就');
const isEdit = ref(false);
const currentId = ref<number | null>(null);
const selectedRows = ref<any[]>([]);

// 搜索和筛选
const searchKeyword = ref('');
const filterCondition = ref('');
const filterStatus = ref('');

// 分页
const pagination = ref({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

const formRef = ref<FormInstance>();
const form = reactive({
  name: '',
  description: '',
  condition: '',
  conditionValue: 0,
  rewardExp: 0,
  icon: '',
  status: 'ACTIVE',
});

const formRules: FormRules = {
  name: [{ required: true, message: '请输入成就名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入成就描述', trigger: 'blur' }],
  condition: [{ required: true, message: '请选择成就条件', trigger: 'change' }],
  conditionValue: [{ required: true, message: '请输入条件值', trigger: 'blur' }],
  rewardExp: [{ required: true, message: '请输入奖励经验值', trigger: 'blur' }],
};

const conditions = [
  { label: '训练次数', value: 'TRAINING_COUNT' },
  { label: '训练正确次数', value: 'TRAINING_CORRECT' },
  { label: '完成测评', value: 'ASSESSMENT_COMPLETE' },
  { label: '连续登录天数', value: 'LOGIN_STREAK' },
  { label: '累计经验值', value: 'TOTAL_EXP' },
  { label: '风险等级提升', value: 'RISK_LEVEL_UP' },
];

const statusTypes = [
  { label: '启用', value: 'ACTIVE' },
  { label: '禁用', value: 'INACTIVE' },
];

const fetchAchievements = async () => {
  loading.value = true;
  try {
    const params: any = {
      page: pagination.value.currentPage,
      size: pagination.value.pageSize,
    };
    
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value;
    }
    if (filterCondition.value) {
      params.condition = filterCondition.value;
    }
    if (filterStatus.value) {
      params.status = filterStatus.value;
    }

    const resp = await http.get('/admin/achievements', { params });
    if (resp.data?.content) {
      tableData.value = resp.data.content;
      pagination.value.total = resp.data.total || 0;
    } else {
      tableData.value = resp.data || [];
      pagination.value.total = tableData.value.length;
    }
  } catch (error) {
    ElMessage.warning('无法加载成就列表');
    tableData.value = [];
    pagination.value.total = 0;
  } finally {
    loading.value = false;
  }
};

const handleAdd = () => {
  dialogTitle.value = '新增成就';
  isEdit.value = false;
  currentId.value = null;
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑成就';
  isEdit.value = true;
  currentId.value = row.id;
  form.name = row.name || '';
  form.description = row.description || '';
  form.condition = row.condition || '';
  form.conditionValue = row.conditionValue || 0;
  form.rewardExp = row.rewardExp || 0;
  form.icon = row.icon || '';
  form.status = row.status || 'ACTIVE';
  dialogVisible.value = true;
};

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该成就吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });

    try {
      await http.delete(`/admin/achievements/${row.id}`);
      ElMessage.success('删除成功');
      fetchAchievements();
    } catch (error) {
      ElMessage.error('删除失败');
    }
  } catch {
    // 用户取消
  }
};

const resetForm = () => {
  form.name = '';
  form.description = '';
  form.condition = '';
  form.conditionValue = 0;
  form.rewardExp = 0;
  form.icon = '';
  form.status = 'ACTIVE';
  formRef.value?.clearValidate();
};

const handleSubmit = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const data = {
          name: form.name,
          description: form.description,
          condition: form.condition,
          conditionValue: form.conditionValue,
          rewardExp: form.rewardExp,
          icon: form.icon,
          status: form.status,
        };

        if (isEdit.value && currentId.value) {
          await http.put(`/admin/achievements/${currentId.value}`, data);
          ElMessage.success('更新成功');
        } else {
          await http.post('/admin/achievements', data);
          ElMessage.success('创建成功');
        }
        dialogVisible.value = false;
        fetchAchievements();
      } catch (error) {
        ElMessage.error(isEdit.value ? '更新失败' : '创建失败');
      }
    }
  });
};

const getConditionLabel = (condition: string) => {
  return conditions.find((c) => c.value === condition)?.label || condition;
};

const getStatusLabel = (status: string) => {
  return statusTypes.find((s) => s.value === status)?.label || status;
};

// 搜索
const handleSearch = () => {
  pagination.value.currentPage = 1;
  fetchAchievements();
};

// 重置筛选
const handleReset = () => {
  searchKeyword.value = '';
  filterCondition.value = '';
  filterStatus.value = '';
  pagination.value.currentPage = 1;
  fetchAchievements();
};

// 分页变化
const handlePageChange = (page: number) => {
  pagination.value.currentPage = page;
  fetchAchievements();
};

const handleSizeChange = (size: number) => {
  pagination.value.pageSize = size;
  pagination.value.currentPage = 1;
  fetchAchievements();
};

// 表格选择
const handleSelectionChange = (selection: any[]) => {
  selectedRows.value = selection;
};

// 批量删除
const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的成就');
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 个成就吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    try {
      const ids = selectedRows.value.map((row) => row.id);
      await http.delete('/admin/achievements/batch', { data: { ids } });
      ElMessage.success('删除成功');
      selectedRows.value = [];
      fetchAchievements();
    } catch (error) {
      ElMessage.error('删除失败');
    }
  } catch {
    // 用户取消
  }
};

// 筛选变化时自动刷新
const handleFilterChange = () => {
  pagination.value.currentPage = 1;
  fetchAchievements();
};

onMounted(() => {
  fetchAchievements();
});
</script>

<template>
  <div class="achievement-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>成就规则管理</span>
          <div class="header-actions">
            <el-button
              v-if="selectedRows.length > 0"
              type="danger"
              @click="handleBatchDelete"
            >
              批量删除 ({{ selectedRows.length }})
            </el-button>
            <el-button type="primary" @click="handleAdd">新增成就</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索和筛选区域 -->
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索成就名称或描述"
          clearable
          style="width: 300px; margin-right: 12px"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="filterCondition"
          placeholder="成就条件"
          clearable
          style="width: 200px; margin-right: 12px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="item in conditions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select
          v-model="filterStatus"
          placeholder="状态"
          clearable
          style="width: 150px; margin-right: 12px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="item in statusTypes"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="成就名称" width="150" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="condition" label="条件类型" width="150">
          <template #default="{ row }">
            {{ getConditionLabel(row.condition) }}
          </template>
        </el-table-column>
        <el-table-column prop="conditionValue" label="条件值" width="100" />
        <el-table-column prop="rewardExp" label="奖励经验" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="成就名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入成就名称" />
        </el-form-item>
        <el-form-item label="成就描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入成就描述"
          />
        </el-form-item>
        <el-form-item label="成就条件" prop="condition">
          <el-select v-model="form.condition" style="width: 100%">
            <el-option
              v-for="item in conditions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="条件值" prop="conditionValue">
          <el-input-number
            v-model="form.conditionValue"
            :min="1"
            style="width: 100%"
            placeholder="请输入条件值"
          />
          <div class="form-tip">达到此值即可获得成就</div>
        </el-form-item>
        <el-form-item label="奖励经验值" prop="rewardExp">
          <el-input-number
            v-model="form.rewardExp"
            :min="0"
            style="width: 100%"
            placeholder="请输入奖励经验值"
          />
        </el-form-item>
        <el-form-item label="图标URL">
          <el-input v-model="form.icon" placeholder="请输入图标URL（可选）" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button
              v-for="item in statusTypes"
              :key="item.value"
              :label="item.value"
            >
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.achievement-management {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-actions {
      display: flex;
      gap: 12px;
    }
  }

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

  .form-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}
</style>
