<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import http from '../../api/http';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { Search } from '@element-plus/icons-vue';

const tableData = ref<any[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const dialogTitle = ref('新增案例');
const isEdit = ref(false);
const currentId = ref<number | null>(null);
const selectedRows = ref<any[]>([]);
const batchMode = ref(false);
const tableRef = ref<any>();

// 搜索和筛选
const searchKeyword = ref('');
const filterType = ref('');
const filterAnswer = ref('');
const filterDifficulty = ref('');

// 分页
const pagination = ref({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

const formRef = ref<FormInstance>();
const form = reactive({
  title: '',
  type: 'SMS',
  content: '',
  hint: '',
  answer: 'FRAUD',
  difficulty: 'EASY',
  mediaUrl: '',
});

const formRules: FormRules = {
  content: [{ required: true, message: '请输入案例内容', trigger: 'blur' }],
  type: [{ required: true, message: '请选择案例类型', trigger: 'change' }],
  answer: [{ required: true, message: '请选择判定结果', trigger: 'change' }],
};

const caseTypes = [
  { label: '短信诈骗', value: 'SMS' },
  { label: '邮件诈骗', value: 'EMAIL' },
  { label: '网站诈骗', value: 'WEBSITE' },
  { label: '电话诈骗', value: 'PHONE' },
  { label: '其他', value: 'OTHER' },
];

const answerTypes = [
  { label: '诈骗', value: 'FRAUD' },
  { label: '正常', value: 'SAFE' },
];

const difficultyTypes = [
  { label: '简单', value: 'EASY' },
  { label: '中等', value: 'MEDIUM' },
  { label: '困难', value: 'HARD' },
];

const fetchCases = async () => {
  loading.value = true;
  try {
    const params: any = {
      page: pagination.value.currentPage,
      size: pagination.value.pageSize,
    };
    
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value;
    }
    if (filterType.value) {
      params.type = filterType.value;
    }
    if (filterAnswer.value) {
      params.answer = filterAnswer.value;
    }
    if (filterDifficulty.value) {
      params.difficulty = filterDifficulty.value;
    }

    const resp = await http.get('/admin/cases', { params });
    if (resp.data?.data?.content) {
      // 后端返回格式：{ data: { content: [], total: number } }
      tableData.value = resp.data.data.content;
      pagination.value.total = resp.data.data.total || 0;
    } else if (resp.data?.content) {
      // 兼容格式：{ content: [], total: number }
      tableData.value = resp.data.content;
      pagination.value.total = resp.data.total || 0;
    } else {
      // 兼容旧接口格式
      tableData.value = resp.data || [];
      pagination.value.total = tableData.value.length;
    }
  } catch (error) {
    ElMessage.warning('无法加载案例列表');
    tableData.value = [];
    pagination.value.total = 0;
  } finally {
    loading.value = false;
  }
};

const handleAdd = () => {
  dialogTitle.value = '新增案例';
  isEdit.value = false;
  currentId.value = null;
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑案例';
  isEdit.value = true;
  currentId.value = row.id;
  form.title = row.title || '';
  form.type = (row.type || 'sms').toUpperCase();
  form.content = row.content || '';
  form.hint = row.hint || '';
  form.answer = (row.answer || 'fraud').toUpperCase();
  form.difficulty = (row.level || 'easy').toUpperCase();
  form.mediaUrl = row.mediaUrl || '';
  dialogVisible.value = true;
};

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该案例吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });

    try {
      await http.delete(`/admin/cases/${row.id}`);
      ElMessage.success('删除成功');
      fetchCases();
    } catch (error) {
      ElMessage.error('删除失败');
    }
  } catch {
    // 用户取消
  }
};

const resetForm = () => {
  form.title = '';
  form.type = 'SMS';
  form.content = '';
  form.hint = '';
  form.answer = 'FRAUD';
  form.difficulty = 'EASY';
  form.mediaUrl = '';
  formRef.value?.clearValidate();
};

const handleSubmit = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value && currentId.value) {
          // 编辑时，将字段转换为后端期望的格式
          const updateData: any = {
            title: form.title,
            type: form.type.toLowerCase(),
            content: form.content,
            hint: form.hint,
            answer: form.answer.toLowerCase(),
            difficulty: form.difficulty.toLowerCase(),
          };
          if (form.mediaUrl) {
            updateData.mediaUrl = form.mediaUrl;
          }
          await http.put(`/admin/cases/${currentId.value}`, updateData);
          ElMessage.success('更新成功');
        } else {
          // 创建时，转换为后端期望的格式
          const createData: any = {
            title: form.title || '后台录入案例',
            type: form.type.toLowerCase(),
            content: form.content,
            hint: form.hint,
            answer: form.answer.toLowerCase(),
            difficulty: form.difficulty.toLowerCase(),
          };
          if (form.mediaUrl) {
            createData.mediaUrl = form.mediaUrl;
          }
          await http.post('/admin/cases', createData);
          ElMessage.success('创建成功');
        }
        dialogVisible.value = false;
        fetchCases();
      } catch (error) {
        ElMessage.error(isEdit.value ? '更新失败' : '创建失败');
      }
    }
  });
};

const getTypeLabel = (type: string) => {
  const normalizedType = type.toUpperCase();
  // 映射后端的小写类型到前端的大写类型
  const typeMap: Record<string, string> = {
    'SMS': '短信诈骗',
    'EMAIL': '邮件诈骗',
    'WEBSITE': '网站诈骗',
    'SITE': '网站诈骗',
    'PHONE': '电话诈骗',
    'AUDIO': '电话诈骗',
    'OTHER': '其他',
  };
  return typeMap[normalizedType] || caseTypes.find((t) => t.value === normalizedType)?.label || type;
};

const getAnswerLabel = (answer: string) => {
  return answerTypes.find((a) => a.value === answer)?.label || answer;
};

const getDifficultyLabel = (difficulty: string) => {
  return difficultyTypes.find((d) => d.value === difficulty)?.label || difficulty;
};

// 搜索
const handleSearch = () => {
  pagination.value.currentPage = 1;
  fetchCases();
};

// 重置筛选
const handleReset = () => {
  searchKeyword.value = '';
  filterType.value = '';
  filterAnswer.value = '';
  filterDifficulty.value = '';
  pagination.value.currentPage = 1;
  fetchCases();
};

// 分页变化
const handlePageChange = (page: number) => {
  pagination.value.currentPage = page;
  fetchCases();
};

const handleSizeChange = (size: number) => {
  pagination.value.pageSize = size;
  pagination.value.currentPage = 1;
  fetchCases();
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
    ElMessage.warning('请选择要删除的案例');
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
      await http.delete('/admin/cases/batch', { data: { ids } });
      ElMessage.success('删除成功');
      cancelBatchDelete();
      fetchCases();
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
  fetchCases();
};

onMounted(() => {
  fetchCases();
});
</script>

<template>
  <div class="case-management admin-page">
    <el-card class="management-card">
      <template #header>
        <div class="card-header">
          <span>诈骗案例管理</span>
        </div>
      </template>

      <!-- 搜索和筛选区域 -->
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索案例内容或提示"
          clearable
          style="width: 300px; margin-right: 12px"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="filterType"
          placeholder="案例类型"
          clearable
          style="width: 150px; margin-right: 12px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="item in caseTypes"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select
          v-model="filterAnswer"
          placeholder="判定结果"
          clearable
          style="width: 150px; margin-right: 12px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="item in answerTypes"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select
          v-model="filterDifficulty"
          placeholder="难度等级"
          clearable
          style="width: 150px; margin-right: 12px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="item in difficultyTypes"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button type="primary" @click="handleAdd">新增案例</el-button>
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
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            {{ getTypeLabel((row.type || '').toUpperCase()) }}
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="hint" label="提示" width="200" show-overflow-tooltip />
        <el-table-column prop="answer" label="判定" width="100">
          <template #default="{ row }">
            <el-tag :type="(row.answer || '').toUpperCase() === 'FRAUD' ? 'danger' : 'success'">
              {{ getAnswerLabel((row.answer || '').toUpperCase()) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="难度" width="100">
          <template #default="{ row }">
            <el-tag :type="(row.level || 'easy').toUpperCase() === 'EASY' ? 'success' : (row.level || '').toUpperCase() === 'MEDIUM' ? 'warning' : 'danger'">
              {{ getDifficultyLabel((row.level || 'easy').toUpperCase()) }}
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
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px" class="dialog-form">
        <el-form-item label="案例标题">
          <el-input v-model="form.title" placeholder="请输入案例标题（可选）" />
        </el-form-item>
        <el-form-item label="案例类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option
              v-for="item in caseTypes"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="案例内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            placeholder="请输入案例内容"
          />
        </el-form-item>
        <el-form-item label="可疑特征提示" prop="hint">
          <el-input
            v-model="form.hint"
            placeholder="请输入可疑特征提示，如：钓鱼链接、恐吓等"
          />
        </el-form-item>
        <el-form-item label="判定结果" prop="answer">
          <el-radio-group v-model="form.answer">
            <el-radio-button
              v-for="item in answerTypes"
              :key="item.value"
              :label="item.value"
            >
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="难度等级" prop="difficulty">
          <el-select v-model="form.difficulty" style="width: 100%">
            <el-option
              v-for="item in difficultyTypes"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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
.case-management {
  // 继承 admin-page 样式
}
</style>
