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
const batchMode = ref(false);
const tableRef = ref<any>();

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
  title: '',
  question: '',
  type: 'sms',
  level: 'easy',
  hint: '',
  answer: 'fraud',
  mediaUrl: '',
  options: [
    { text: '', isCorrect: false },
    { text: '', isCorrect: false },
    { text: '', isCorrect: false },
  ],
  fraudFeatures: [] as string[],
});

const formRules: FormRules = {
  question: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  level: [{ required: true, message: '请选择难度', trigger: 'change' }],
  answer: [{ required: true, message: '请选择正确答案', trigger: 'change' }],
};

const caseTypes = [
  { label: '短信', value: 'sms' },
  { label: '邮件', value: 'email' },
  { label: '音频', value: 'audio' },
  { label: '网站', value: 'site' },
];

const difficultyLevels = [
  { label: '简单', value: 'easy' },
  { label: '中等', value: 'medium' },
  { label: '困难', value: 'hard' },
];

const answerTypes = [
  { label: '诈骗', value: 'fraud' },
  { label: '安全', value: 'safe' },
];

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
    // 后端返回格式: { data: { content: [], total: number } }
    if (resp.data?.data?.content) {
      cases.value = resp.data.data.content;
    } else if (resp.data?.content) {
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
  form.caseId = row.caseId || row.id;
  form.title = row.title || row.caseTitle || '';
  form.question = row.question || row.content || '';
  form.type = row.type || 'sms';
  form.level = row.level || 'easy';
  form.hint = row.hint || '';
  form.answer = row.answer || row.correctAnswer || 'fraud';
  form.mediaUrl = row.mediaUrl || '';
  // 从选项或诈骗特征中提取数据
  if (row.options && Array.isArray(row.options)) {
    form.options = row.options.map((opt: any) => ({
      text: opt.text || opt,
      isCorrect: opt.isCorrect || false,
    }));
  } else {
    form.options = [
      { text: '', isCorrect: false },
      { text: '', isCorrect: false },
      { text: '', isCorrect: false },
    ];
  }
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
  form.title = '';
  form.question = '';
  form.type = 'sms';
  form.level = 'easy';
  form.hint = '';
  form.answer = 'fraud';
  form.mediaUrl = '';
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
        const data: any = {
          question: form.question,
          title: form.title,
          type: form.type,
          level: form.level,
          hint: form.hint,
          answer: form.answer,
          fraudFeatures: form.fraudFeatures.filter((f) => f.trim()),
        };

        // 如果有媒体URL，添加到数据中
        if (form.mediaUrl) {
          data.mediaUrl = form.mediaUrl;
        }

        // 如果是新增且有caseId，则关联已有案例；如果没有caseId，则创建新案例
        if (!isEdit.value && form.caseId) {
          data.caseId = form.caseId;
        }

        if (isEdit.value && currentId.value) {
          // 编辑时，更新所有字段
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
  // 先从表格数据中查找（因为表格数据中可能包含caseTitle字段）
  const tableItem = tableData.value.find((item) => item.id === caseId || item.caseId === caseId);
  if (tableItem && tableItem.caseTitle) {
    return tableItem.caseTitle;
  }
  // 如果表格数据中没有，从案例列表中查找
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
    ElMessage.warning('请选择要删除的题目');
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
      await http.delete('/admin/training/questions/batch', { data: { ids } });
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
  fetchCases();
  fetchQuestions();
});
</script>

<template>
  <div class="training-management admin-page">
    <el-card class="management-card">
      <template #header>
        <div class="card-header">
          <span>识别训练题目管理</span>
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
        <el-button type="primary" @click="handleAdd">新增题目</el-button>
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
        <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="caseId" label="关联案例" width="150">
          <template #default="{ row }">
            {{ getCaseTitle(row.caseId) }}
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">
              {{ caseTypes.find((t) => t.value === row.type)?.label || row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="难度" width="100">
          <template #default="{ row }">
            <el-tag :type="row.level === 'easy' ? 'success' : row.level === 'medium' ? 'warning' : 'danger'" size="small">
              {{ difficultyLevels.find((l) => l.value === row.level)?.label || row.level }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="question" label="题目内容" min-width="250" show-overflow-tooltip />
        <el-table-column label="选项数量" width="100">
          <template #default="{ row }">
            {{ row.options?.length || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="诈骗特征" width="120">
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
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px" class="dialog-form">
        <el-form-item v-if="!isEdit" label="关联案例">
          <el-select v-model="form.caseId" style="width: 100%" placeholder="请选择案例（可选，不选择则创建新案例）" clearable>
            <el-option
              v-for="item in cases"
              :key="item.id"
              :label="getCaseOptionLabel(item)"
              :value="item.id"
            />
          </el-select>
          <div style="font-size: 12px; color: #909399; margin-top: 4px">
            不选择案例将创建新的训练题目案例
          </div>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="题目内容" prop="question">
          <el-input
            v-model="form.question"
            type="textarea"
            :rows="4"
            placeholder="请输入题目内容"
          />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option
              v-for="item in caseTypes"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="难度" prop="level">
          <el-select v-model="form.level" style="width: 100%">
            <el-option
              v-for="item in difficultyLevels"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="正确答案" prop="answer">
          <el-select v-model="form.answer" style="width: 100%">
            <el-option
              v-for="item in answerTypes"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="提示信息">
          <el-input
            v-model="form.hint"
            type="textarea"
            :rows="2"
            placeholder="请输入提示信息（可选）"
          />
        </el-form-item>
        <el-form-item label="媒体URL">
          <el-input v-model="form.mediaUrl" placeholder="请输入媒体资源URL（可选）" />
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
  .option-item,
  .feature-item {
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
