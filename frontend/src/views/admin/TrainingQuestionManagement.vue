<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import http from '../../api/http';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { Search } from '@element-plus/icons-vue';

const tableData = ref<any[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const dialogTitle = ref('新增题目');
const isEdit = ref(false);
const currentId = ref<number | null>(null);
const selectedRows = ref<any[]>([]);

// 搜索和筛选
const searchKeyword = ref('');
const filterCaseId = ref<number | null>(null);

// 分页
const pagination = ref({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

const formRef = ref<FormInstance>();
const form = reactive({
  caseId: null as number | null,
  question: '',
  options: [
    { text: '', isCorrect: false },
    { text: '', isCorrect: false },
    { text: '', isCorrect: false },
  ],
  fraudFeatures: [] as string[],
});

const formRules: FormRules = {
  caseId: [{ required: true, message: '请选择关联案例', trigger: 'change' }],
  question: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
};

const cases = ref<any[]>([]);

const fetchQuestions = async () => {
  loading.value = true;
  try {
    const params: any = {
      page: pagination.value.currentPage,
      size: pagination.value.pageSize,
    };
    
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value;
    }
    if (filterCaseId.value) {
      params.caseId = filterCaseId.value;
    }

    const resp = await http.get('/admin/training/questions', { params });
    if (resp.data?.content) {
      tableData.value = resp.data.content;
      pagination.value.total = resp.data.total || 0;
    } else {
      tableData.value = resp.data || [];
      pagination.value.total = tableData.value.length;
    }
  } catch (error) {
    ElMessage.warning('无法加载题目列表');
    tableData.value = [];
    pagination.value.total = 0;
  } finally {
    loading.value = false;
  }
};

const fetchCases = async () => {
  try {
    const resp = await http.get('/admin/cases', { params: { page: 1, size: 1000 } });
    // 后端返回的是分页数据 { content: [], total: number }
    if (resp.data?.content) {
      cases.value = resp.data.content;
    } else if (Array.isArray(resp.data)) {
      cases.value = resp.data;
    } else {
      cases.value = [];
    }
  } catch (error) {
    ElMessage.warning('无法加载案例列表');
    cases.value = [];
  }
};

const handleAdd = () => {
  dialogTitle.value = '新增题目';
  isEdit.value = false;
  currentId.value = null;
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑题目';
  isEdit.value = true;
  currentId.value = row.id;
  form.caseId = row.caseId;
  form.question = row.question || '';
  form.options = row.options || [
    { text: '', isCorrect: false },
    { text: '', isCorrect: false },
    { text: '', isCorrect: false },
  ];
  form.fraudFeatures = row.fraudFeatures || [];
  dialogVisible.value = true;
};

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该题目吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });

    try {
      await http.delete(`/admin/training/questions/${row.id}`);
      ElMessage.success('删除成功');
      fetchQuestions();
    } catch (error) {
      ElMessage.error('删除失败');
    }
  } catch {
    // 用户取消
  }
};

const resetForm = () => {
  form.caseId = null;
  form.question = '';
  form.options = [
    { text: '', isCorrect: false },
    { text: '', isCorrect: false },
    { text: '', isCorrect: false },
  ];
  form.fraudFeatures = [];
  formRef.value?.clearValidate();
};

const addOption = () => {
  form.options.push({ text: '', isCorrect: false });
};

const removeOption = (index: number) => {
  if (form.options.length > 2) {
    form.options.splice(index, 1);
  } else {
    ElMessage.warning('至少需要2个选项');
  }
};

const addFeature = () => {
  form.fraudFeatures.push('');
};

const removeFeature = (index: number) => {
  form.fraudFeatures.splice(index, 1);
};

const handleSubmit = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (valid) {
      // 验证选项
      if (form.options.length < 2) {
        ElMessage.warning('至少需要2个选项');
        return;
      }

      const hasText = form.options.every((opt) => opt.text.trim());
      if (!hasText) {
        ElMessage.warning('所有选项都必须填写内容');
        return;
      }

      try {
        const data = {
          caseId: form.caseId,
          question: form.question,
          options: form.options,
          fraudFeatures: form.fraudFeatures.filter((f) => f.trim()),
        };

        if (isEdit.value && currentId.value) {
          await http.put(`/admin/training/questions/${currentId.value}`, data);
          ElMessage.success('更新成功');
        } else {
          await http.post('/admin/training/questions', data);
          ElMessage.success('创建成功');
        }
        dialogVisible.value = false;
        fetchQuestions();
      } catch (error) {
        ElMessage.error(isEdit.value ? '更新失败' : '创建失败');
      }
    }
  });
};

const getCaseTitle = (caseId: number) => {
  const caseItem = cases.value.find((c) => c.id === caseId);
  if (!caseItem) {
    return `案例 #${caseId}`;
  }
  const displayText = caseItem.title || caseItem.hint || '';
  const truncated = displayText.length > 30 ? displayText.substring(0, 30) + '...' : displayText;
  return `${caseItem.type || '未知'} - ${truncated}`;
};

const getCaseOptionLabel = (item: any) => {
  if (!item) return '未知案例';
  const displayText = item.title || item.hint || '';
  const truncated = displayText.length > 50 ? displayText.substring(0, 50) + '...' : displayText;
  return `${item.type || '未知'} - ${truncated}`;
};

// 搜索
const handleSearch = () => {
  pagination.value.currentPage = 1;
  fetchQuestions();
};

// 重置筛选
const handleReset = () => {
  searchKeyword.value = '';
  filterCaseId.value = null;
  pagination.value.currentPage = 1;
  fetchQuestions();
};

// 分页变化
const handlePageChange = (page: number) => {
  pagination.value.currentPage = page;
  fetchQuestions();
};

const handleSizeChange = (size: number) => {
  pagination.value.pageSize = size;
  pagination.value.currentPage = 1;
  fetchQuestions();
};

// 表格选择
const handleSelectionChange = (selection: any[]) => {
  selectedRows.value = selection;
};

// 批量删除
const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的题目');
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 个题目吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    try {
      const ids = selectedRows.value.map((row) => row.id);
      await http.delete('/admin/training/questions/batch', { data: { ids } });
      ElMessage.success('删除成功');
      selectedRows.value = [];
      fetchQuestions();
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
  fetchQuestions();
};

onMounted(() => {
  fetchCases();
  fetchQuestions();
});
</script>

<template>
  <div class="training-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>识别训练题目管理</span>
          <div class="header-actions">
            <el-button
              v-if="selectedRows.length > 0"
              type="danger"
              @click="handleBatchDelete"
            >
              批量删除 ({{ selectedRows.length }})
            </el-button>
            <el-button type="primary" @click="handleAdd">新增题目</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索和筛选区域 -->
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索题目内容"
          clearable
          style="width: 300px; margin-right: 12px"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="filterCaseId"
          placeholder="关联案例"
          clearable
          style="width: 300px; margin-right: 12px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="item in cases"
            :key="item.id"
            :label="getCaseOptionLabel(item)"
            :value="item.id"
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
        <el-table-column prop="caseId" label="关联案例" width="200">
          <template #default="{ row }">
            {{ getCaseTitle(row.caseId) }}
          </template>
        </el-table-column>
        <el-table-column prop="question" label="题目内容" min-width="300" show-overflow-tooltip />
        <el-table-column label="选项数量" width="100">
          <template #default="{ row }">
            {{ row.options?.length || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="诈骗特征" width="150">
          <template #default="{ row }">
            {{ row.fraudFeatures?.length || 0 }} 个
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="关联案例" prop="caseId">
          <el-select v-model="form.caseId" style="width: 100%" placeholder="请选择案例">
            <el-option
              v-for="item in cases"
              :key="item.id"
              :label="getCaseOptionLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="题目内容" prop="question">
          <el-input
            v-model="form.question"
            type="textarea"
            :rows="3"
            placeholder="请输入题目内容"
          />
        </el-form-item>
        <el-form-item label="选项">
          <div v-for="(option, index) in form.options" :key="index" class="option-item">
            <el-input
              v-model="option.text"
              placeholder="请输入选项内容"
              style="flex: 1"
            />
            <el-checkbox v-model="option.isCorrect" style="margin-left: 12px">正确答案</el-checkbox>
            <el-button
              v-if="form.options.length > 2"
              type="danger"
              link
              size="small"
              style="margin-left: 12px"
              @click="removeOption(index)"
            >
              删除
            </el-button>
          </div>
          <el-button type="primary" link size="small" @click="addOption" style="margin-top: 8px">
            + 添加选项
          </el-button>
        </el-form-item>
        <el-form-item label="诈骗特征">
          <div v-for="(feature, index) in form.fraudFeatures" :key="index" class="feature-item">
            <el-input
              v-model="form.fraudFeatures[index]"
              placeholder="请输入诈骗特征"
              style="flex: 1"
            />
            <el-button
              type="danger"
              link
              size="small"
              style="margin-left: 12px"
              @click="removeFeature(index)"
            >
              删除
            </el-button>
          </div>
          <el-button type="primary" link size="small" @click="addFeature" style="margin-top: 8px">
            + 添加特征
          </el-button>
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
.training-management {
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

  .option-item,
  .feature-item {
    display: flex;
    align-items: center;
    margin-bottom: 12px;
  }
}
</style>
