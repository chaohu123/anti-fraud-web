import { ref, computed, reactive } from 'vue';
import { ElMessage } from 'element-plus';

export interface UndoCommand {
  id: string;
  name: string;
  execute: () => void | Promise<void>;
  undo: () => void | Promise<void>;
  timestamp: number;
}

export interface UndoOptions {
  maxHistory?: number;
  onExecute?: (command: UndoCommand) => void;
  onUndo?: (command: UndoCommand) => void;
  onRedo?: (command: UndoCommand) => void;
}

export function useUndo(options: UndoOptions = {}) {
  const { maxHistory = 50, onExecute, onUndo, onRedo } = options;

  const history = reactive<UndoCommand[]>([]);
  const currentIndex = ref(-1);
  const isExecuting = ref(false);

  // 是否可以撤销
  const canUndo = computed(() => currentIndex.value >= 0);

  // 是否可以重做
  const canRedo = computed(() => currentIndex.value < history.length - 1);

  // 执行命令
  const execute = async (command: UndoCommand) => {
    if (isExecuting.value) return;

    try {
      isExecuting.value = true;

      // 执行命令
      await command.execute();

      // 如果当前不在历史记录末尾，清除后面的记录
      if (currentIndex.value < history.length - 1) {
        history.splice(currentIndex.value + 1);
      }

      // 添加到历史记录
      history.push(command);

      // 限制历史记录数量
      if (history.length > maxHistory) {
        history.shift();
      } else {
        currentIndex.value++;
      }

      onExecute?.(command);
    } catch (error) {
      console.error('执行命令失败:', error);
      ElMessage.error('操作执行失败');
      throw error;
    } finally {
      isExecuting.value = false;
    }
  };

  // 撤销
  const undo = async () => {
    if (!canUndo.value || isExecuting.value) return;

    try {
      isExecuting.value = true;

      const command = history[currentIndex.value];
      await command.undo();

      currentIndex.value--;
      onUndo?.(command);

      ElMessage.success(`已撤销：${command.name}`);
    } catch (error) {
      console.error('撤销失败:', error);
      ElMessage.error('撤销失败');
      throw error;
    } finally {
      isExecuting.value = false;
    }
  };

  // 重做
  const redo = async () => {
    if (!canRedo.value || isExecuting.value) return;

    try {
      isExecuting.value = true;

      currentIndex.value++;
      const command = history[currentIndex.value];
      await command.execute();

      onRedo?.(command);

      ElMessage.success(`已重做：${command.name}`);
    } catch (error) {
      console.error('重做失败:', error);
      ElMessage.error('重做失败');
      currentIndex.value--;
      throw error;
    } finally {
      isExecuting.value = false;
    }
  };

  // 清空历史
  const clear = () => {
    history.splice(0, history.length);
    currentIndex.value = -1;
  };

  // 获取历史记录
  const getHistory = () => {
    return history.slice(0, currentIndex.value + 1);
  };

  // 获取未来记录（可重做的）
  const getFuture = () => {
    return history.slice(currentIndex.value + 1);
  };

  // 创建命令
  const createCommand = (
    name: string,
    execute: () => void | Promise<void>,
    undo: () => void | Promise<void>
  ): UndoCommand => {
    return {
      id: `cmd_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      name,
      execute,
      undo,
      timestamp: Date.now(),
    };
  };

  return {
    // 状态
    canUndo,
    canRedo,
    isExecuting,
    history: computed(() => getHistory()),
    future: computed(() => getFuture()),

    // 方法
    execute,
    undo,
    redo,
    clear,
    createCommand,
  };
}

// 快捷键支持
export function useUndoHotkeys(undoFn: () => void, redoFn: () => void) {
  const handleKeydown = (event: KeyboardEvent) => {
    // Ctrl+Z / Cmd+Z: 撤销
    if ((event.ctrlKey || event.metaKey) && event.key === 'z' && !event.shiftKey) {
      event.preventDefault();
      undoFn();
    }

    // Ctrl+Shift+Z / Cmd+Shift+Z: 重做
    // 或 Ctrl+Y / Cmd+Y: 重做
    if (
      ((event.ctrlKey || event.metaKey) && event.key === 'z' && event.shiftKey) ||
      ((event.ctrlKey || event.metaKey) && event.key === 'y')
    ) {
      event.preventDefault();
      redoFn();
    }
  };

  const enable = () => {
    window.addEventListener('keydown', handleKeydown);
  };

  const disable = () => {
    window.removeEventListener('keydown', handleKeydown);
  };

  return {
    enable,
    disable,
  };
}