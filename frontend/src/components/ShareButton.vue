<template>
  <el-dropdown
    trigger="click"
    @command="handleShareCommand"
    :disabled="isSharing"
    placement="bottom"
  >
    <el-button
      :type="buttonType"
      :size="buttonSize"
      :icon="Share"
      :loading="isSharing"
      :class="buttonClass"
    >
      {{ buttonText }}
    </el-button>

    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item v-if="shareSupported" command="native">
          <el-icon><Share /></el-icon>
          <span>系统分享</span>
        </el-dropdown-item>
        <el-dropdown-item command="copy">
          <el-icon><Link /></el-icon>
          <span>复制链接</span>
        </el-dropdown-item>
        <el-dropdown-item command="wechat" divided>
          <el-icon><ChatDotRound /></el-icon>
          <span>分享到微信</span>
        </el-dropdown-item>
        <el-dropdown-item command="qq">
          <el-icon><ChatLineRound /></el-icon>
          <span>分享到QQ</span>
        </el-dropdown-item>
        <el-dropdown-item command="weibo">
          <el-icon><Share /></el-icon>
          <span>分享到微博</span>
        </el-dropdown-item>
        <el-dropdown-item v-if="showImageShare" command="image" divided>
          <el-icon><Picture /></el-icon>
          <span>生成分享图片</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  Share,
  Link,
  ChatDotRound,
  ChatLineRound,
  Picture,
} from '@element-plus/icons-vue';
import { useShare, type ShareData } from '../composables/useShare';

interface Props {
  shareData: ShareData;
  buttonType?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'text' | 'default';
  buttonSize?: 'large' | 'default' | 'small';
  buttonText?: string;
  buttonClass?: string;
  showImageShare?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  buttonType: 'default',
  buttonSize: 'default',
  buttonText: '分享',
  buttonClass: '',
  showImageShare: false,
});

const emit = defineEmits<{
  shareSuccess: [platform: string];
  shareError: [error: Error];
  generateImage: [];
}>();

const {
  isSharing,
  shareSupported,
  nativeShare,
  copyToClipboard,
  shareToWeChat,
  shareToQQ,
  shareToWeibo,
} = useShare();

const handleShareCommand = async (command: string) => {
  try {
    let success = false;

    switch (command) {
      case 'native':
        success = await nativeShare(props.shareData);
        if (success) {
          emit('shareSuccess', 'native');
        }
        break;

      case 'copy':
        success = await copyToClipboard(props.shareData.url || window.location.href);
        if (success) {
          emit('shareSuccess', 'copy');
        }
        break;

      case 'wechat':
        success = await shareToWeChat(props.shareData);
        if (success) {
          emit('shareSuccess', 'wechat');
        }
        break;

      case 'qq':
        shareToQQ(props.shareData);
        emit('shareSuccess', 'qq');
        break;

      case 'weibo':
        shareToWeibo(props.shareData);
        emit('shareSuccess', 'weibo');
        break;

      case 'image':
        emit('generateImage');
        break;
    }
  } catch (error: any) {
    emit('shareError', error);
  }
};
</script>

<style scoped lang="scss">
// 可以添加自定义样式
</style>