<template>
  <div class="undo-toolbar" v-if="showToolbar">
    <el-button-group>
      <el-button
        :disabled="!canUndo"
        :icon="RefreshLeft"
        @click="undo"
        title="撤销 (Ctrl+Z)"
        size="small"
      >
        撤销
      </el-button>
      <el-button
        :disabled="!canRedo"
        :icon="RefreshRight"
        @click="redo"
        title="重做 (Ctrl+Y)"
        size="small"
      >
        重做
      </el-button>
    </el-button-group>

    <!-- 历史记录下拉菜单 -->
    <el-dropdown v-if="showHistory && history.length > 0" trigger="click">
      <el-button size="small" :icon="Clock">
        历史
      </el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <div class="history-header">操作历史</div>
          <el-dropdown-item
            v-for="(cmd, index) in history"
            :key="cmd.id"
            @click="goToHistory(index)"
            :class="{ current: index === currentHistoryIndex }"
          >
            <div class="history-item">
              <span class="history-name">{{ cmd.name }}</span>
              <span class="history-time">{{ formatTime(cmd.timestamp) }}</span>
            </div>
          </el-dropdown-item>
          <el-dropdown-item v-if="history.length === 0" disabled>
            暂无历史记录
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <!-- 清空按钮 -->
    <el-button
      v-if="showClear && history.length > 0"
      size="small"
      :icon="Delete"
      @click="handleClear"
      type="danger"
      text
    >
      清空
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  RefreshLeft,
  RefreshRight,
  Clock,
  Delete,
} from '@element-plus/icons-vue';
import { ElMessageBox } from 'element-plus';

interface Props {
  canUndo: boolean;
  canRedo: boolean;
  history: Array<{ id: string; name: string; timestamp: number }>;
  showToolbar?: boolean;
  showHistory?: boolean;
  showClear?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  showToolbar: true,
  showHistory: true,
  showClear: false,
});

const emit = defineEmits<{
  undo: [];
  redo: [];
  clear: [];
  goToHistory: [index: number];
}>();

const currentHistoryIndex = computed(() => props.history.length - 1);

const undo = () => {
  emit('undo');
};

const redo = () => {
  emit('redo');
};

const handleClear = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有历史记录吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    emit('clear');
  } catch {
    // 用户取消
  }
};

const goToHistory = (index: number) => {
  emit('goToHistory', index);
};

const formatTime = (timestamp: number) => {
  const now = Date.now();
  const diff = now - timestamp;
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);

  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  return new Date(timestamp).toLocaleString();
};
</script>

<style scoped lang="scss">
.undo-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--el-bg-color-page);
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);
}

.history-header {
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  min-width: 200px;

  &.current {
    color: var(--el-color-primary);
    font-weight: 500;
  }
}

.history-name {
  flex: 1;
  font-size: 13px;
}

.history-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>