import { ref } from 'vue';
import { ElMessage } from 'element-plus';

export interface ShareData {
  title: string;
  text: string;
  url?: string;
  image?: string;
}

export function useShare() {
  const isSharing = ref(false);
  const shareSupported = ref(!!navigator.share);

  // 原生分享API
  const nativeShare = async (data: ShareData): Promise<boolean> => {
    if (!navigator.share) {
      return false;
    }

    try {
      isSharing.value = true;
      await navigator.share({
        title: data.title,
        text: data.text,
        url: data.url || window.location.href,
      });
      return true;
    } catch (error: any) {
      // 用户取消分享不是错误
      if (error.name === 'AbortError') {
        return false;
      }
      console.error('分享失败:', error);
      return false;
    } finally {
      isSharing.value = false;
    }
  };

  // 分享到微信（生成二维码）
  const shareToWeChat = (data: ShareData) => {
    const url = data.url || window.location.href;
    // 实际应该调用后端API生成二维码
    ElMessage.info('请使用微信扫一扫分享此页面');
    return copyToClipboard(url);
  };

  // 分享到QQ
  const shareToQQ = (data: ShareData) => {
    const url = data.url || window.location.href;
    const shareUrl = `https://connect.qq.com/widget/shareqq/index.html?url=${encodeURIComponent(url)}&title=${encodeURIComponent(data.title)}&desc=${encodeURIComponent(data.text)}`;
    window.open(shareUrl, '_blank', 'width=600,height=400');
  };

  // 分享到微博
  const shareToWeibo = (data: ShareData) => {
    const url = data.url || window.location.href;
    const shareUrl = `https://service.weibo.com/share/share.php?url=${encodeURIComponent(url)}&title=${encodeURIComponent(data.title + ' ' + data.text)}&pic=${encodeURIComponent(data.image || '')}`;
    window.open(shareUrl, '_blank', 'width=600,height=400');
  };

  // 复制链接到剪贴板
  const copyToClipboard = async (text: string): Promise<boolean> => {
    try {
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(text);
        ElMessage.success('链接已复制到剪贴板');
        return true;
      } else {
        // 降级方案
        const textArea = document.createElement('textarea');
        textArea.value = text;
        textArea.style.position = 'fixed';
        textArea.style.left = '-999999px';
        textArea.style.top = '-999999px';
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        const successful = document.execCommand('copy');
        textArea.remove();
        if (successful) {
          ElMessage.success('链接已复制到剪贴板');
        } else {
          ElMessage.error('复制失败，请手动复制');
        }
        return successful;
      }
    } catch (error) {
      console.error('复制失败:', error);
      ElMessage.error('复制失败，请手动复制');
      return false;
    }
  };

  // 分享成就
  const shareAchievement = async (achievementName: string, description: string) => {
    const shareData: ShareData = {
      title: `我在反诈学习平台解锁了新成就！`,
      text: `🎉 ${achievementName}: ${description}`,
      url: window.location.origin,
    };

    return await share(shareData);
  };

  // 分享测评结果
  const shareAssessmentResult = async (score: number, level: string) => {
    const levelMap: Record<string, string> = {
      low: '低风险',
      medium: '中等风险',
      high: '高风险',
    };

    const shareData: ShareData = {
      title: '我完成了反诈风险测评！',
      text: `我的风险指数是 ${score.toFixed(2)}，等级：${levelMap[level] || level}。快来测测你的防骗能力吧！`,
      url: `${window.location.origin}/assessment`,
    };

    return await share(shareData);
  };

  // 分享知识内容
  const shareKnowledge = async (knowledgeTitle: string, knowledgeId: number) => {
    const shareData: ShareData = {
      title: `推荐：${knowledgeTitle}`,
      text: `这条防骗知识很有用，推荐给你！`,
      url: `${window.location.origin}/knowledge/${knowledgeId}`,
    };

    return await share(shareData);
  };

  // 通用分享方法
  const share = async (data: ShareData): Promise<boolean> => {
    // 优先使用原生分享API
    if (shareSupported.value) {
      return await nativeShare(data);
    }

    // 如果不支持原生分享，复制链接
    const url = data.url || window.location.href;
    return await copyToClipboard(url);
  };

  // 生成分享图片
  const generateShareImage = async (
    element: HTMLElement,
    options: {
      backgroundColor?: string;
      width?: number;
      height?: number;
    } = {}
  ): Promise<string | null> => {
    try {
      const { default: html2canvas } = await import('html2canvas');

      const canvas = await html2canvas(element, {
        backgroundColor: options.backgroundColor || '#ffffff',
        width: options.width,
        height: options.height,
        scale: 2, // 提高清晰度
        useCORS: true,
      });

      return canvas.toDataURL('image/png');
    } catch (error) {
      console.error('生成分享图片失败:', error);
      ElMessage.error('生成分享图片失败');
      return null;
    }
  };

  // 下载分享图片
  const downloadShareImage = (dataUrl: string, filename: string = 'share.png') => {
    const link = document.createElement('a');
    link.download = filename;
    link.href = dataUrl;
    link.click();
    ElMessage.success('图片已下载');
  };

  return {
    // 状态
    isSharing,
    shareSupported,

    // 通用方法
    share,
    copyToClipboard,

    // 平台特定方法
    nativeShare,
    shareToWeChat,
    shareToQQ,
    shareToWeibo,

    // 场景方法
    shareAchievement,
    shareAssessmentResult,
    shareKnowledge,

    // 图片分享
    generateShareImage,
    downloadShareImage,
  };
}