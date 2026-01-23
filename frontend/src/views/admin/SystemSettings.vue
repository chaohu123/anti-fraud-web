<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import http from '../../api/http';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';

const loading = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({
  lowRiskThreshold: 30,
  mediumRiskThreshold: 60,
  highRiskThreshold: 90,
  expPerTraining: 10,
  expPerCorrect: 5,
  expPerAssessment: 20,
});

const formRules: FormRules = {
  lowRiskThreshold: [
    { required: true, message: '请输入低风险阈值', trigger: 'blur' },
    { type: 'number', min: 0, max: 100, message: '阈值必须在0-100之间', trigger: 'blur' },
  ],
  mediumRiskThreshold: [
    { required: true, message: '请输入中风险阈值', trigger: 'blur' },
    { type: 'number', min: 0, max: 100, message: '阈值必须在0-100之间', trigger: 'blur' },
  ],
  highRiskThreshold: [
    { required: true, message: '请输入高风险阈值', trigger: 'blur' },
    { type: 'number', min: 0, max: 100, message: '阈值必须在0-100之间', trigger: 'blur' },
  ],
  expPerTraining: [
    { required: true, message: '请输入每次训练经验值', trigger: 'blur' },
    { type: 'number', min: 0, message: '经验值必须大于等于0', trigger: 'blur' },
  ],
  expPerCorrect: [
    { required: true, message: '请输入每次正确经验值', trigger: 'blur' },
    { type: 'number', min: 0, message: '经验值必须大于等于0', trigger: 'blur' },
  ],
  expPerAssessment: [
    { required: true, message: '请输入每次测评经验值', trigger: 'blur' },
    { type: 'number', min: 0, message: '经验值必须大于等于0', trigger: 'blur' },
  ],
};

const fetchSettings = async () => {
  loading.value = true;
  try {
    const resp = await http.get('/admin/settings');
    if (resp.data) {
      Object.assign(form, resp.data);
    }
  } catch (error) {
    ElMessage.warning('无法加载系统设置，使用默认值');
  } finally {
    loading.value = false;
  }
};

const handleSubmit = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (valid) {
      // 验证阈值逻辑
      if (form.lowRiskThreshold >= form.mediumRiskThreshold) {
        ElMessage.warning('低风险阈值必须小于中风险阈值');
        return;
      }
      if (form.mediumRiskThreshold >= form.highRiskThreshold) {
        ElMessage.warning('中风险阈值必须小于高风险阈值');
        return;
      }

      loading.value = true;
      try {
        await http.put('/admin/settings', form);
        ElMessage.success('设置保存成功');
      } catch (error) {
        ElMessage.error('设置保存失败');
      } finally {
        loading.value = false;
      }
    }
  });
};

const handleReset = () => {
  form.lowRiskThreshold = 30;
  form.mediumRiskThreshold = 60;
  form.highRiskThreshold = 90;
  form.expPerTraining = 10;
  form.expPerCorrect = 5;
  form.expPerAssessment = 20;
  formRef.value?.clearValidate();
};

onMounted(() => {
  fetchSettings();
});
</script>

<template>
  <div class="system-settings">
    <el-card>
      <template #header>
        <span>系统设置</span>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="200px"
        style="max-width: 800px"
      >
        <el-divider content-position="left">风险等级阈值配置</el-divider>
        <el-form-item label="低风险阈值" prop="lowRiskThreshold">
          <el-input-number
            v-model="form.lowRiskThreshold"
            :min="0"
            :max="100"
            style="width: 200px"
          />
          <div class="form-tip">评分低于此值为低风险</div>
        </el-form-item>
        <el-form-item label="中风险阈值" prop="mediumRiskThreshold">
          <el-input-number
            v-model="form.mediumRiskThreshold"
            :min="0"
            :max="100"
            style="width: 200px"
          />
          <div class="form-tip">评分在此值范围内为中风险</div>
        </el-form-item>
        <el-form-item label="高风险阈值" prop="highRiskThreshold">
          <el-input-number
            v-model="form.highRiskThreshold"
            :min="0"
            :max="100"
            style="width: 200px"
          />
          <div class="form-tip">评分高于此值为高风险</div>
        </el-form-item>

        <el-divider content-position="left">经验值配置</el-divider>
        <el-form-item label="每次训练经验值" prop="expPerTraining">
          <el-input-number
            v-model="form.expPerTraining"
            :min="0"
            style="width: 200px"
          />
          <div class="form-tip">用户完成一次训练获得的经验值</div>
        </el-form-item>
        <el-form-item label="每次正确经验值" prop="expPerCorrect">
          <el-input-number
            v-model="form.expPerCorrect"
            :min="0"
            style="width: 200px"
          />
          <div class="form-tip">用户训练答对获得的额外经验值</div>
        </el-form-item>
        <el-form-item label="每次测评经验值" prop="expPerAssessment">
          <el-input-number
            v-model="form.expPerAssessment"
            :min="0"
            style="width: 200px"
          />
          <div class="form-tip">用户完成一次测评获得的经验值</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">保存设置</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.system-settings {
  .form-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
    margin-left: 8px;
  }
}
</style>
