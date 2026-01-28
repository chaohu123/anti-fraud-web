<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import http from '../../api/http';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { Plus, PictureFilled, Delete } from '@element-plus/icons-vue';

type CarouselItem = {
  id: number;
  imageUrl: string;
  title?: string;
  linkUrl?: string;
  sortOrder: number;
  enableFlag: number;
  createdAt?: string;
};

const tableData = ref<CarouselItem[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const dialogTitle = ref('新增轮播图');
const isEdit = ref(false);
const currentId = ref<number | null>(null);
const uploadLoading = ref(false);
const uploadRef = ref<any>(null);
const urlInput = ref('');
const showAddUrlBtn = ref(false);
const previewImages = ref<string[]>([]); // 已添加的图片URL列表，用于小图预览

const formRef = ref<FormInstance>();
const form = reactive({
  imageUrl: '',
  title: '',
  linkUrl: '',
  sortOrder: 0,
  enableFlag: 1,
});

const formRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  imageUrl: [{ required: true, message: '请上传图片或输入图片URL', trigger: 'change' }],
};

function resolveImageSrc(url: string): string {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  return url.startsWith('/') ? url : '/' + url;
}

/** 格式化日期时间 */
function formatDateTime(dateTime: string | null | undefined): string {
  if (!dateTime) return '-';
  try {
    const date = new Date(dateTime);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}`;
  } catch {
    return dateTime;
  }
}

/** 检测是否为有效URL */
function isValidUrl(str: string): boolean {
  if (!str || str.trim().length === 0) return false;
  const urlPattern = /^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?(\?[&#\w=]*)?$/i;
  return urlPattern.test(str.trim()) || str.trim().startsWith('http://') || str.trim().startsWith('https://') || str.trim().startsWith('/');
}

/** 监听URL输入，自动识别并显示添加按钮 */
function handleUrlInput(value: string) {
  urlInput.value = value;
  showAddUrlBtn.value = isValidUrl(value);
}

/** 通过URL添加图片 */
function addImageByUrl() {
  const url = urlInput.value.trim();
  if (!isValidUrl(url)) {
    ElMessage.warning('请输入有效的图片URL');
    return;
  }
  form.imageUrl = url;
  previewImages.value = [...previewImages.value, url];
  urlInput.value = '';
  showAddUrlBtn.value = false;
  ElMessage.success('已添加图片URL');
}

const fetchList = async () => {
  loading.value = true;
  try {
    const resp = await http.get('/admin/carousel');
    tableData.value = Array.isArray(resp.data) ? resp.data : [];
  } catch {
    ElMessage.warning('加载轮播图列表失败');
    tableData.value = [];
  } finally {
    loading.value = false;
  }
};

const handleAdd = () => {
  dialogTitle.value = '新增轮播图';
  isEdit.value = false;
  currentId.value = null;
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (row: CarouselItem) => {
  dialogTitle.value = '编辑轮播图';
  isEdit.value = true;
  currentId.value = row.id;
  form.imageUrl = row.imageUrl || '';
  form.title = row.title || '';
  form.linkUrl = row.linkUrl || '';
  form.sortOrder = row.sortOrder ?? 0;
  form.enableFlag = row.enableFlag ?? 1;
  previewImages.value = row.imageUrl ? [row.imageUrl] : [];
  urlInput.value = '';
  showAddUrlBtn.value = false;
  dialogVisible.value = true;
};

const handleDelete = async (row: CarouselItem) => {
  try {
    await ElMessageBox.confirm('确定要删除该轮播图吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await http.delete(`/admin/carousel/${row.id}`);
    ElMessage.success('删除成功');
    fetchList();
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败');
  }
};

const resetForm = () => {
  form.imageUrl = '';
  form.title = '';
  form.linkUrl = '';
  form.sortOrder = tableData.value.length;
  form.enableFlag = 1;
  urlInput.value = '';
  showAddUrlBtn.value = false;
  previewImages.value = [];
  uploadRef.value?.clearFiles?.();
  formRef.value?.clearValidate();
};

let uploadDebounceTimer: ReturnType<typeof setTimeout> | null = null;
let uploadSeq = 0;

/** 本地上传（支持选一张或多张）：单张则填入表单，多张则直接批量添加为轮播图并关闭弹窗 */
const handleUploadChange = async (_uploadFile: any, uploadFiles: any[]) => {
  if (uploadDebounceTimer) clearTimeout(uploadDebounceTimer);
  uploadDebounceTimer = setTimeout(async () => {
    uploadDebounceTimer = null;
    uploadSeq += 1;
    const seq = uploadSeq;
    const raws = uploadFiles.map((f: any) => f.raw).filter(Boolean);
    if (!raws.length) return;
    uploadLoading.value = true;
    try {
      if (raws.length === 1) {
        const formData = new FormData();
        formData.append('file', raws[0]);
        const resp = await http.post('/admin/carousel/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        });
        const url = resp.data?.url;
        // 仅处理“最新一次上传选择”的结果，避免并发覆盖
        if (seq !== uploadSeq) return;
        if (url) {
          form.imageUrl = url;
          previewImages.value = [...previewImages.value, url];
          ElMessage.success('上传成功');
        } else {
          ElMessage.error('上传失败');
        }
      } else {
        const formData = new FormData();
        raws.forEach((f) => formData.append('files', f));
        const resp = await http.post('/admin/carousel/upload/batch', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        });
        const urls = resp.data as string[];
        // 仅处理“最新一次上传选择”的结果，避免并发覆盖
        if (seq !== uploadSeq) return;
        if (Array.isArray(urls) && urls.length) {
          const baseOrder = tableData.value.length;
          for (let i = 0; i < urls.length; i++) {
            await http.post('/admin/carousel', {
              imageUrl: urls[i],
              // 给一个默认标题，避免后端/数据库对 title 有约束时批量失败，也避免产生大量空标题项
              title: `轮播图 ${baseOrder + i + 1}`,
              linkUrl: '',
              sortOrder: baseOrder + i,
              enableFlag: 1,
            });
          }
          ElMessage.success(`已添加 ${urls.length} 张轮播图`);
          dialogVisible.value = false;
          fetchList();
        } else {
          ElMessage.error('上传失败');
        }
      }
    } catch (e: unknown) {
      ElMessage.error((e as Error)?.message || '上传失败');
    } finally {
      uploadLoading.value = false;
      uploadRef.value?.clearFiles?.();
    }
  }, 150);
  return false;
};

/** 移除预览图片 */
function removePreviewImage(index: number) {
  previewImages.value = previewImages.value.filter((_, i) => i !== index);
  if (previewImages.value.length > 0) {
    form.imageUrl = previewImages.value[0];
  } else {
    form.imageUrl = '';
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    if (!form.imageUrl || previewImages.value.length === 0) {
      ElMessage.warning('请至少添加一张图片');
      return;
    }
    try {
      const payload = {
        imageUrl: form.imageUrl.trim(),
        title: form.title.trim() || undefined,
        linkUrl: form.linkUrl.trim() || undefined,
        sortOrder: form.sortOrder,
        enableFlag: form.enableFlag,
      };
      if (isEdit.value && currentId.value != null) {
        await http.put(`/admin/carousel/${currentId.value}`, payload);
        ElMessage.success('更新成功');
      } else {
        await http.post('/admin/carousel', payload);
        ElMessage.success('添加成功');
      }
      dialogVisible.value = false;
      fetchList();
    } catch (e: any) {
      ElMessage.error(e?.message || '保存失败');
    }
  });
};

onMounted(() => {
  fetchList();
});
</script>

<template>
  <div class="carousel-management">
    <div class="page-header">
      <span class="page-title">知识页轮播图管理</span>
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增轮播图</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" stripe border>
      <el-table-column label="预览" width="120" align="center">
        <template #default="{ row }">
          <el-image
            v-if="row.imageUrl"
            :src="resolveImageSrc(row.imageUrl)"
            fit="cover"
            style="width: 80px; height: 48px; border-radius: 6px;"
            :preview-src-list="[resolveImageSrc(row.imageUrl)]"
          />
          <span v-else class="text-placeholder">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
      <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enableFlag === 1 ? 'success' : 'info'" size="small">
            {{ row.enableFlag === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180" align="center">
        <template #default="{ row }">
          <span v-if="row.createdAt">{{ formatDateTime(row.createdAt) }}</span>
          <span v-else class="text-placeholder">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && tableData.length === 0" description="暂无轮播图，请添加" />

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      destroy-on-close
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="标题" prop="title" required>
          <el-input v-model="form.title" placeholder="请输入标题" clearable />
        </el-form-item>
        <el-form-item label="图片" prop="imageUrl" required>
          <div class="image-upload-section">
            <div class="upload-row">
              <el-upload
                ref="uploadRef"
                :show-file-list="false"
                :auto-upload="false"
                accept="image/jpeg,image/png,image/gif,image/webp"
                multiple
                :on-change="handleUploadChange"
                :disabled="uploadLoading"
              >
                <el-button type="primary" :loading="uploadLoading">
                  <el-icon><PictureFilled /></el-icon>
                  本地上传
                </el-button>
              </el-upload>
            </div>
            <div class="url-add-row">
              <el-input
                v-model="urlInput"
                placeholder="输入图片URL"
                clearable
                @input="handleUrlInput"
                @keyup.enter="showAddUrlBtn && addImageByUrl()"
              >
                <template #append>
                  <el-button
                    v-if="showAddUrlBtn"
                    type="primary"
                    @click="addImageByUrl"
                  >
                    添加
                  </el-button>
                </template>
              </el-input>
            </div>
            <!-- 小图预览 -->
            <div v-if="previewImages.length" class="thumb-preview">
              <div class="thumb-preview-label">已添加图片预览</div>
              <div class="thumb-preview-list">
                <div v-for="(url, index) in previewImages" :key="index" class="thumb-item">
                  <el-image
                    :src="resolveImageSrc(url)"
                    fit="cover"
                    class="thumb-img"
                    :preview-src-list="previewImages.map((u) => resolveImageSrc(u))"
                    :initial-index="index"
                  />
                  <el-button
                    size="small"
                    type="danger"
                    circle
                    class="thumb-remove"
                    @click="removePreviewImage(index)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
          <span class="form-tip">数值越小越靠前</span>
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态" prop="enableFlag">
          <el-radio-group v-model="form.enableFlag">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
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
.carousel-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  .page-title {
    font-size: 18px;
    font-weight: 600;
  }
}

.text-placeholder {
  color: var(--el-text-color-placeholder);
}

.image-upload-section {
  width: 100%;
}

.upload-row {
  margin-bottom: 12px;
}

.url-add-row {
  margin-bottom: 12px;
}

.thumb-preview {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.thumb-preview-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 10px;
}

.thumb-preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.thumb-item {
  position: relative;
  width: 88px;
  height: 66px;
}

.thumb-img {
  width: 88px;
  height: 66px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color);
}

.thumb-remove {
  position: absolute;
  top: -8px;
  right: -8px;
  padding: 4px;
}

.form-tip {
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
