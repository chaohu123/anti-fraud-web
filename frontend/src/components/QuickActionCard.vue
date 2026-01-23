<template>
  <el-card 
    class="quick-action-card" 
    shadow="hover" 
    :body-style="{ padding: '24px' }"
    @click="handleClick"
  >
    <div class="action-content">
      <div class="action-icon" :style="{ background: iconBg }">
        <el-icon :size="32" :color="iconColor">
          <component :is="icon" />
        </el-icon>
      </div>
      <div class="action-info">
        <div class="action-title">{{ title }}</div>
        <div class="action-desc">{{ description }}</div>
      </div>
      <div class="action-arrow">
        <el-icon :size="20" color="#909399">
          <ArrowRight />
        </el-icon>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ArrowRight } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';

interface Props {
  title: string;
  description: string;
  icon: any;
  iconBg?: string;
  iconColor?: string;
  path: string;
}

const props = withDefaults(defineProps<Props>(), {
  iconBg: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  iconColor: '#fff',
});

const router = useRouter();

const handleClick = () => {
  router.push(props.path);
};
</script>

<style scoped lang="scss">
.quick-action-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(102, 126, 234, 0.15);
  }
}

.action-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.3s ease;
}

.quick-action-card:hover .action-icon {
  transform: scale(1.1) rotate(5deg);
}

.action-info {
  flex: 1;
  min-width: 0;
}

.action-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 6px;
}

.action-desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.action-arrow {
  flex-shrink: 0;
  transition: transform 0.3s ease;
}

.quick-action-card:hover .action-arrow {
  transform: translateX(4px);
}
</style>
