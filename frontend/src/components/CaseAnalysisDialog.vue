<script setup lang="ts">
const props = defineProps<{
  visible: boolean;
  title: string;
  content: string;
  hint: string;
  suspiciousPoints: string[];
  userChosen: string[];
}>();

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
}>();

const close = () => {
  emit('update:visible', false);
};
</script>

<template>
  <el-dialog :model-value="visible" title="案例解析" width="520px" @close="close">
    <div class="block">
      <div class="label">原始信息</div>
      <el-alert :title="title" type="info" :closable="false" />
      <p class="content">{{ content }}</p>
    </div>
    <div class="block">
      <div class="label">诈骗手法 & 防范要点</div>
      <p class="hint">
        {{ hint }}
      </p>
      <div class="sub-label">特征命中情况</div>
      <div class="tags">
        <div>
          <span class="sub">命中特征：</span>
          <el-tag
            v-for="p in suspiciousPoints.filter((p) => userChosen.includes(p))"
            :key="`hit-${p}`"
            type="success"
            size="small"
          >
            {{ p }}
          </el-tag>
          <span v-if="!suspiciousPoints.some((p) => userChosen.includes(p))" class="sub text-muted">
            暂无命中
          </span>
        </div>
        <div>
          <span class="sub">漏选特征：</span>
          <el-tag
            v-for="p in suspiciousPoints.filter((p) => !userChosen.includes(p))"
            :key="`miss-${p}`"
            type="warning"
            size="small"
          >
            {{ p }}
          </el-tag>
        </div>
        <div>
          <span class="sub">误选特征：</span>
          <el-tag
            v-for="p in userChosen.filter((p) => !suspiciousPoints.includes(p))"
            :key="`extra-${p}`"
            type="info"
            size="small"
          >
            {{ p }}
          </el-tag>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button type="primary" @click="close">我已了解</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.block {
  margin-bottom: 12px;
}
.label {
  font-weight: 600;
  margin-bottom: 4px;
}
.content {
  margin-top: 6px;
}
.hint {
  color: var(--af-text);
}
.sub-label {
  margin-top: 6px;
  font-weight: 600;
}
.tags {
  margin-top: 4px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.sub {
  font-size: 12px;
  margin-right: 4px;
}
.text-muted {
  color: var(--af-muted);
}
ul {
  margin: 8px 0 0;
  padding-left: 0;
  list-style: none;
}
</style>

