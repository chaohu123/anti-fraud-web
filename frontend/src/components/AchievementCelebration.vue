<template>
  <teleport to="body">
    <transition name="celebration-fade">
      <div v-if="show" class="celebration-overlay" @click="close">
        <div class="celebration-content" @click.stop>
          <!-- 庆祝动画 -->
          <div class="confetti-container">
            <div
              v-for="i in 50"
              :key="i"
              class="confetti"
              :style="getConfettiStyle(i)"
            ></div>
          </div>

          <!-- 成就卡片 -->
          <div class="achievement-card">
            <div class="achievement-icon">
              <div class="icon-glow">{{ achievement?.icon || '🎉' }}</div>
            </div>

            <h2 class="achievement-title">{{ achievement?.name || '成就解锁！' }}</h2>
            <p class="achievement-desc">{{ achievement?.description }}</p>

            <!-- 奖励信息 -->
            <div v-if="achievement?.expReward" class="achievement-reward">
              <div class="reward-item">
                <el-icon :size="20" color="#E6A23C"><Star /></el-icon>
                <span class="reward-value">+{{ achievement.expReward }} 经验值</span>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="achievement-actions">
              <el-button type="primary" @click="handleShare">
                <el-icon><Share /></el-icon>
                <span>分享成就</span>
              </el-button>
              <el-button @click="close">
                关闭
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { Star, Share } from '@element-plus/icons-vue';
import { useShare } from '../composables/useShare';

interface Achievement {
  icon: string;
  name: string;
  description: string;
  expReward?: number;
}

interface Props {
  show: boolean;
  achievement?: Achievement;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  close: [];
  share: [];
}>();

const { shareAchievement } = useShare();

const close = () => {
  emit('close');
};

const handleShare = async () => {
  if (props.achievement) {
    await shareAchievement(props.achievement.name, props.achievement.description);
    emit('share');
  }
};

const getConfettiStyle = (index: number) => {
  const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#FFA07A', '#98D8C8', '#F7DC6F', '#BB8FCE'];
  const randomColor = colors[Math.floor(Math.random() * colors.length)];
  
  return {
    '--confetti-color': randomColor,
    '--confetti-x': `${Math.random() * 100}%`,
    '--confetti-delay': `${Math.random() * 3}s`,
    '--confetti-duration': `${3 + Math.random() * 2}s`,
    '--confetti-rotation': `${Math.random() * 360}deg`,
  };
};

// 播放庆祝音效（可选）
watch(() => props.show, (newShow) => {
  if (newShow) {
    playSound();
  }
});

const playSound = () => {
  try {
    // 使用Web Audio API播放简单的庆祝音效
    const audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
    const oscillator = audioContext.createOscillator();
    const gainNode = audioContext.createGain();

    oscillator.connect(gainNode);
    gainNode.connect(audioContext.destination);

    oscillator.frequency.value = 800;
    oscillator.type = 'sine';

    gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
    gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.5);

    oscillator.start(audioContext.currentTime);
    oscillator.stop(audioContext.currentTime + 0.5);
  } catch (error) {
    // 音效播放失败不影响功能
    console.debug('播放音效失败:', error);
  }
};
</script>

<style scoped lang="scss">
.celebration-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  padding: 20px;
}

.celebration-content {
  position: relative;
  animation: celebration-enter 0.6s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

.confetti-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  overflow: hidden;
}

.confetti {
  position: absolute;
  width: 10px;
  height: 10px;
  background-color: var(--confetti-color);
  top: -10%;
  left: var(--confetti-x);
  opacity: 1;
  animation: confetti-fall var(--confetti-duration) linear var(--confetti-delay) forwards;
  transform: rotate(var(--confetti-rotation));
}

.achievement-card {
  background: white;
  border-radius: 24px;
  padding: 48px 40px;
  max-width: 500px;
  width: 100%;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
}

.achievement-icon {
  margin-bottom: 24px;
  display: flex;
  justify-content: center;
}

.icon-glow {
  font-size: 80px;
  animation: glow-pulse 2s ease-in-out infinite;
  filter: drop-shadow(0 0 20px rgba(255, 215, 0, 0.8));
}

.achievement-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin: 0 0 16px 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.achievement-desc {
  font-size: 16px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
  margin: 0 0 32px 0;
}

.achievement-reward {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 32px;
  padding: 16px;
  background: linear-gradient(135deg, #FFF9E6 0%, #FFE5B4 100%);
  border-radius: 12px;
}

.reward-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #E6A23C;
}

.reward-value {
  color: var(--el-text-color-primary);
}

.achievement-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* 动画 */
@keyframes celebration-enter {
  0% {
    transform: scale(0.3);
    opacity: 0;
  }
  50% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes confetti-fall {
  0% {
    transform: translateY(0) rotate(var(--confetti-rotation));
    opacity: 1;
  }
  100% {
    transform: translateY(100vh) rotate(calc(var(--confetti-rotation) + 720deg));
    opacity: 0;
  }
}

@keyframes glow-pulse {
  0%, 100% {
    filter: drop-shadow(0 0 20px rgba(255, 215, 0, 0.8));
    transform: scale(1);
  }
  50% {
    filter: drop-shadow(0 0 30px rgba(255, 215, 0, 1));
    transform: scale(1.1);
  }
}

.celebration-fade-enter-active,
.celebration-fade-leave-active {
  transition: opacity 0.3s ease;
}

.celebration-fade-enter-from,
.celebration-fade-leave-to {
  opacity: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .achievement-card {
    padding: 32px 24px;
  }

  .icon-glow {
    font-size: 64px;
  }

  .achievement-title {
    font-size: 24px;
  }

  .achievement-desc {
    font-size: 14px;
  }

  .achievement-actions {
    flex-direction: column;

    .el-button {
      width: 100%;
    }
  }
}
</style>