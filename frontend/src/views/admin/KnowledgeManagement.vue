<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import http from '../../api/http';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { Search } from '@element-plus/icons-vue';

const tableData = ref<any[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const dialogTitle = ref('新增知识');
const isEdit = ref(false);
const currentId = ref<number | null>(null);
const selectedRows = ref<any[]>([]);

// 搜索和筛选
const searchKeyword = ref('');
const filterCategory = ref('');
const filterContentType = ref('');
const filterStatus = ref('');

// 分页
const pagination = ref({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

const formRef = ref<FormInstance>();
const form = reactive({
  title: '',
  category: 'SMS',
  content: '',
  contentType: 'ARTICLE',
  status: 'PUBLISHED',
  coverImage: '',
  videoUrl: '',
});

const formRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
};

const categories = [
  { label: '短信诈骗', value: 'SMS' },
  { label: '邮件诈骗', value: 'EMAIL' },
  { label: '网站诈骗', value: 'WEBSITE' },
  { label: '电话诈骗', value: 'PHONE' },
  { label: '金融诈骗', value: 'FINANCE' },
  { label: '社交平台诈骗', value: 'SOCIAL' },
  { label: '其他', value: 'OTHER' },
];

const contentTypes = [
  { label: '文章', value: 'ARTICLE' },
  { label: '视频', value: 'VIDEO' },
];

const statusTypes = [
  { label: '已发布', value: 'PUBLISHED' },
  { label: '草稿', value: 'DRAFT' },
];

const fetchKnowledge = async () => {
  loading.value = true;
  try {
    const params: any = {
      page: pagination.value.currentPage,
      size: pagination.value.pageSize,
    };
    
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value;
    }
    if (filterCategory.value) {
      params.category = filterCategory.value;
    }
    if (filterContentType.value) {
      params.contentType = filterContentType.value;
    }
    if (filterStatus.value) {
      params.status = filterStatus.value;
    }

    const resp = await http.get('/admin/knowledge', { params });
    if (resp.data?.content) {
      tableData.value = resp.data.content;
      pagination.value.total = resp.data.total || 0;
    } else {
      tableData.value = resp.data || [];
      pagination.value.total = tableData.value.length;
    }
  } catch (error) {
    ElMessage.warning('无法加载知识库列表');
    tableData.value = [];
    pagination.value.total = 0;
  } finally {
    loading.value = false;
  }
};

const handleAdd = () => {
  dialogTitle.value = '新增知识';
  isEdit.value = false;
  currentId.value = null;
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑知识';
  isEdit.value = true;
  currentId.value = row.id;
  form.title = row.title || '';
  form.category = row.category || 'SMS';
  form.content = row.content || '';
  form.contentType = row.contentType || 'ARTICLE';
  form.status = row.status || 'PUBLISHED';
  form.coverImage = row.coverImage || '';
  form.videoUrl = row.videoUrl || '';
  dialogVisible.value = true;
};

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该知识吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });

    try {
      await http.delete(`/admin/knowledge/${row.id}`);
      ElMessage.success('删除成功');
      fetchKnowledge();
    } catch (error) {
      ElMessage.error('删除失败');
    }
  } catch {
    // 用户取消
  }
};

const resetForm = () => {
  form.title = '';
  form.category = 'SMS';
  form.content = '';
  form.contentType = 'ARTICLE';
  form.status = 'PUBLISHED';
  form.coverImage = '';
  form.videoUrl = '';
  formRef.value?.clearValidate();
};

const handleSubmit = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const data = {
          title: form.title,
          category: form.category,
          content: form.content,
          contentType: form.contentType,
          status: form.status,
          coverImage: form.coverImage,
          videoUrl: form.videoUrl,
        };

        if (isEdit.value && currentId.value) {
          await http.put(`/admin/knowledge/${currentId.value}`, data);
          ElMessage.success('更新成功');
        } else {
          await http.post('/admin/knowledge', data);
          ElMessage.success('创建成功');
        }
        dialogVisible.value = false;
        fetchKnowledge();
      } catch (error) {
        ElMessage.error(isEdit.value ? '更新失败' : '创建失败');
      }
    }
  });
};

const getCategoryLabel = (category: string) => {
  return categories.find((c) => c.value === category)?.label || category;
};

const getContentTypeLabel = (type: string) => {
  return contentTypes.find((t) => t.value === type)?.label || type;
};

const getStatusLabel = (status: string) => {
  return statusTypes.find((s) => s.value === status)?.label || status;
};

// 搜索
const handleSearch = () => {
  pagination.value.currentPage = 1;
  fetchKnowledge();
};

// 重置筛选
const handleReset = () => {
  searchKeyword.value = '';
  filterCategory.value = '';
  filterContentType.value = '';
  filterStatus.value = '';
  pagination.value.currentPage = 1;
  fetchKnowledge();
};

// 分页变化
const handlePageChange = (page: number) => {
  pagination.value.currentPage = page;
  fetchKnowledge();
};

const handleSizeChange = (size: number) => {
  pagination.value.pageSize = size;
  pagination.value.currentPage = 1;
  fetchKnowledge();
};

// 表格选择
const handleSelectionChange = (selection: any[]) => {
  selectedRows.value = selection;
};

// 批量删除
const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的知识');
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 个知识吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    try {
      const ids = selectedRows.value.map((row) => row.id);
      await http.delete('/admin/knowledge/batch', { data: { ids } });
      ElMessage.success('删除成功');
      selectedRows.value = [];
      fetchKnowledge();
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
  fetchKnowledge();
};

onMounted(() => {
  fetchKnowledge();
});
</script>

<template>
  <div class="knowledge-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>防骗知识库管理</span>
          <div class="header-actions">
            <el-button
              v-if="selectedRows.length > 0"
              type="danger"
              @click="handleBatchDelete"
            >
              批量删除 ({{ selectedRows.length }})
            </el-button>
            <el-button type="primary" @click="handleAdd">新增知识</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索和筛选区域 -->
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索标题或内容"
          clearable
          style="width: 300px; margin-right: 12px"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="filterCategory"
          placeholder="分类"
          clearable
          style="width: 150px; margin-right: 12px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="item in categories"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select
          v-model="filterContentType"
          placeholder="内容类型"
          clearable
          style="width: 150px; margin-right: 12px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="item in contentTypes"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select
          v-model="filterStatus"
          placeholder="发布状态"
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
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            {{ getCategoryLabel(row.category) }}
          </template>
        </el-table-column>
        <el-table-column prop="contentType" label="类型" width="100">
          <template #default="{ row }">
            {{ getContentTypeLabel(row.contentType) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容预览" min-width="300" show-overflow-tooltip />
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="800px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" style="width: 100%">
            <el-option
              v-for="item in categories"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="内容类型" prop="contentType">
          <el-radio-group v-model="form.contentType">
            <el-radio-button
              v-for="item in contentTypes"
              :key="item.value"
              :label="item.value"
            >
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="请输入内容（支持Markdown格式）"
          />
        </el-form-item>
        <el-form-item v-if="form.contentType === 'VIDEO'" label="视频URL">
          <el-input v-model="form.videoUrl" placeholder="请输入视频链接" />
        </el-form-item>
        <el-form-item label="封面图片">
          <el-input v-model="form.coverImage" placeholder="请输入封面图片URL" />
        </el-form-item>
        <el-form-item label="发布状态" prop="status">
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
.knowledge-management {
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
}
</style>
