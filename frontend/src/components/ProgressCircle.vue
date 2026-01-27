<template>
  <div class="progress-circle" :style="{ width: size + 'px', height: size + 'px' }">
    <svg :width="size" :height="size" class="progress-svg">
      <!-- 背景圆 -->
      <circle
        :cx="center"
        :cy="center"
        :r="radius"
        class="progress-bg"
        :stroke-width="strokeWidth"
        fill="none"
      />

      <!-- 进度圆 -->
      <circle
        :cx="center"
        :cy="center"
        :r="radius"
        class="progress-bar"
        :stroke-width="strokeWidth"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="progressOffset"
        :stroke="color"
        fill="none"
        :class="{ animating: animate }"
      />
    </svg>

    <!-- 中心内容 -->
    <div class="progress-content">
      <slot>
        <div class="progress-text">
          <div class="progress-value">{{ displayValue }}{{ unit }}</div>
          <div v-if="label" class="progress-label">{{ label }}</div>
        </div>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';

interface Props {
  percentage: number;
  size?: number;
  strokeWidth?: number;
  color?: string;
  label?: string;
  unit?: string;
  animate?: boolean;
  showValue?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  size: 120,
  strokeWidth: 8,
  color: '#409EFF',
  unit: '%',
  animate: true,
  showValue: true,
});

const center = computed(() => props.size / 2);
const radius = computed(() => (props.size - props.strokeWidth) / 2);
const circumference = computed(() => 2 * Math.PI * radius.value);

const progressOffset = computed(() => {
  const progress = Math.min(100, Math.max(0, props.percentage));
  return circumference.value * (1 - progress / 100);
});

// 动画显示的值
const displayValue = ref(0);

// 监听百分比变化，动画更新显示值
watch(() => props.percentage, (newVal) => {
  if (props.animate) {
    animateValue(displayValue.value, newVal);
  } else {
    displayValue.value = newVal;
  }
}, { immediate: true });

// 数值动画
const animateValue = (from: number, to: number, duration = 800) => {
  const startTime = Date.now();
  const difference = to - from;

  const updateValue = () => {
    const elapsed = Date.now() - startTime;
    const progress = Math.min(elapsed / duration, 1);

    // 使用缓动函数
    const easeProgress = easeOutCubic(progress);
    displayValue.value = Math.round(from + difference * easeProgress);

    if (progress < 1) {
      requestAnimationFrame(updateValue);
    }
  };

  requestAnimationFrame(updateValue);
};

// 缓动函数
const easeOutCubic = (t: number) => {
  return 1 - Math.pow(1 - t, 3);
};
</script>

<style scoped lang="scss">
.progress-circle {
  position: relative;
  display: inline-block;
}

.progress-svg {
  transform: rotate(-90deg);
}

.progress-bg {
  stroke: var(--el-border-color-lighter);
  opacity: 0.3;
}

.progress-bar {
  stroke-linecap: round;
  transition: stroke-dashoffset 0.6s ease;

  &.animating {
    animation: progress-rotate 2s linear infinite;
  }
}

.progress-content {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  width: 80%;
}

.progress-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.progress-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1;
}

.progress-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

@keyframes progress-rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>