<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import http from '../../api/http';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { Search } from '@element-plus/icons-vue';

const tableData = ref<any[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const dialogTitle = ref('新增问题');
const isEdit = ref(false);
const currentId = ref<number | null>(null);
const selectedRows = ref<any[]>([]);
const batchMode = ref(false);
const tableRef = ref<any>();

// 搜索和筛选
const searchKeyword = ref('');
const filterDimension = ref('');

// 分页
const pagination = ref({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

const formRef = ref<FormInstance>();
const form = reactive({
  dimension: 'INFO',
  question: '',
  options: [
    { text: '', score: 1 },
    { text: '', score: 2 },
    { text: '', score: 3 },
  ],
  weight: 1,
});

const formRules: FormRules = {
  dimension: [{ required: true, message: '请选择问题维度', trigger: 'change' }],
  question: [{ required: true, message: '请输入问题内容', trigger: 'blur' }],
  weight: [{ required: true, message: '请输入权重', trigger: 'blur' }],
};

const dimensions = [
  { label: '信息保护意识', value: 'INFO' },
  { label: '金融安全意识', value: 'FINANCE' },
  { label: '心理风险倾向', value: 'PSYCH' },
];

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
    if (filterDimension.value) {
      params.dimension = filterDimension.value;
    }

    const resp = await http.get('/admin/assessment/questions', { params });
    // 后端返回格式: { data: { content: [], total: number } }
    if (resp.data?.data?.content) {
      tableData.value = resp.data.data.content;
      pagination.value.total = resp.data.data.total || 0;
    } else if (resp.data?.content) {
      tableData.value = resp.data.content;
      pagination.value.total = resp.data.total || 0;
    } else {
      tableData.value = resp.data || [];
      pagination.value.total = tableData.value.length;
    }
  } catch (error) {
    ElMessage.warning('无法加载问题列表');
    tableData.value = [];
    pagination.value.total = 0;
  } finally {
    loading.value = false;
  }
};

const handleAdd = () => {
  dialogTitle.value = '新增问题';
  isEdit.value = false;
  currentId.value = null;
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑问题';
  isEdit.value = true;
  currentId.value = row.id;
  form.dimension = row.dimension || 'INFO';
  form.question = row.question || '';
  form.options = row.options || [
    { text: '', score: 1 },
    { text: '', score: 2 },
    { text: '', score: 3 },
  ];
  form.weight = row.weight || 1;
  dialogVisible.value = true;
};

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该问题吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });

    try {
      await http.delete(`/admin/assessment/questions/${row.id}`);
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
  form.dimension = 'INFO';
  form.question = '';
  form.options = [
    { text: '', score: 1 },
    { text: '', score: 2 },
    { text: '', score: 3 },
  ];
  form.weight = 1;
  formRef.value?.clearValidate();
};

const addOption = () => {
  const maxScore = Math.max(...form.options.map((o) => o.score), 0);
  form.options.push({ text: '', score: maxScore + 1 });
};

const removeOption = (index: number) => {
  if (form.options.length > 2) {
    form.options.splice(index, 1);
  } else {
    ElMessage.warning('至少需要2个选项');
  }
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
          dimension: form.dimension,
          question: form.question,
          options: form.options,
          weight: form.weight,
        };

        if (isEdit.value && currentId.value) {
          await http.put(`/admin/assessment/questions/${currentId.value}`, data);
          ElMessage.success('更新成功');
        } else {
          await http.post('/admin/assessment/questions', data);
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

const getDimensionLabel = (dimension: string) => {
  return dimensions.find((d) => d.value === dimension)?.label || dimension;
};

// 搜索
const handleSearch = () => {
  pagination.value.currentPage = 1;
  fetchQuestions();
};

// 重置筛选
const handleReset = () => {
  searchKeyword.value = '';
  filterDimension.value = '';
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

const enterBatchDeleteMode = () => {
  batchMode.value = true;
  selectedRows.value = [];
  tableRef.value?.clearSelection?.();
};

const cancelBatchDelete = () => {
  batchMode.value = false;
  selectedRows.value = [];
  tableRef.value?.clearSelection?.();
};

const confirmBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的问题');
    return;
  }

  try {
    await ElMessageBox.confirm('确认删除所选数据吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });

    try {
      const ids = selectedRows.value.map((row) => row.id);
      await http.delete('/admin/assessment/questions/batch', { data: { ids } });
      ElMessage.success('删除成功');
      cancelBatchDelete();
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
  fetchQuestions();
});
</script>

<template>
  <div class="assessment-management admin-page">
    <el-card class="management-card">
      <template #header>
        <div class="card-header">
          <span>风险测评问卷管理</span>
        </div>
      </template>

      <!-- 搜索和筛选区域 -->
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索问题内容"
          clearable
          style="width: 300px; margin-right: 12px"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="filterDimension"
          placeholder="问题维度"
          clearable
          style="width: 200px; margin-right: 12px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="item in dimensions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button type="primary" @click="handleAdd">新增问题</el-button>
        <template v-if="!batchMode">
          <el-button type="danger" @click="enterBatchDeleteMode">批量删除</el-button>
        </template>
        <template v-else>
          <el-button type="danger" :disabled="selectedRows.length === 0" @click="confirmBatchDelete">
            确认删除 ({{ selectedRows.length }})
          </el-button>
          <el-button @click="cancelBatchDelete">取消删除</el-button>
        </template>
      </div>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="tableData"
        border
        stripe
        class="data-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column v-if="batchMode" type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="dimension" label="维度" width="150">
          <template #default="{ row }">
            {{ getDimensionLabel(row.dimension) }}
          </template>
        </el-table-column>
        <el-table-column prop="question" label="问题内容" min-width="300" show-overflow-tooltip />
        <el-table-column label="选项数量" width="100">
          <template #default="{ row }">
            {{ row.options?.length || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="weight" label="权重" width="100" />
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
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px" class="dialog-form">
        <el-form-item label="问题维度" prop="dimension">
          <el-select v-model="form.dimension" style="width: 100%">
            <el-option
              v-for="item in dimensions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="问题内容" prop="question">
          <el-input
            v-model="form.question"
            type="textarea"
            :rows="3"
            placeholder="请输入问题内容"
          />
        </el-form-item>
        <el-form-item label="选项">
          <div v-for="(option, index) in form.options" :key="index" class="option-item">
            <el-input
              v-model="option.text"
              placeholder="请输入选项内容"
              style="flex: 1"
            />
            <el-input-number
              v-model="option.score"
              :min="1"
              :max="10"
              style="width: 120px; margin-left: 12px"
              placeholder="分值"
            />
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
          <div class="add-option-row">
            <el-button
              type="primary"
              plain
              size="small"
              class="add-option-btn"
              @click="addOption"
            >
              + 添加选项
            </el-button>
          </div>
          <div class="form-tip">分值越高表示风险越高</div>
        </el-form-item>
        <el-form-item label="权重" prop="weight">
          <el-input-number
            v-model="form.weight"
            :min="0.1"
            :max="10"
            :step="0.1"
            style="width: 100%"
            placeholder="请输入权重"
          />
          <div class="form-tip">用于计算风险评分时的权重系数</div>
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
.assessment-management {
  .option-item {
    display: flex;
    align-items: center;
    margin-bottom: 12px;
    padding: 12px;
    background: #f8f9fa;
    border-radius: 8px;
    transition: all 0.3s;

    &:hover {
      background: #f0f0f0;
      transform: translateX(4px);
    }
  }

  .add-option-row {
    margin-top: 8px;
    width: 100%;
    display: flex;
    justify-content: flex-start;

    .add-option-btn {
      min-width: 96px;
    }
  }
}
</style>
