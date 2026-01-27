import { ref, onMounted, onUnmounted, Ref } from 'vue';

export interface LazyLoadOptions {
  root?: Element | null;
  rootMargin?: string;
  threshold?: number | number[];
  once?: boolean;
  placeholder?: string;
  errorImage?: string;
}

export function useLazyLoad(options: LazyLoadOptions = {}) {
  const {
    root = null,
    rootMargin = '50px',
    threshold = 0.01,
    once = true,
  } = options;

  const observer = ref<IntersectionObserver | null>(null);
  const observedElements = new Set<Element>();

  // 创建IntersectionObserver
  const createObserver = () => {
    if (!('IntersectionObserver' in window)) {
      console.warn('IntersectionObserver not supported');
      return null;
    }

    return new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            loadElement(entry.target);
            
            if (once) {
              observer.value?.unobserve(entry.target);
              observedElements.delete(entry.target);
            }
          }
        });
      },
      {
        root,
        rootMargin,
        threshold,
      }
    );
  };

  // 加载元素
  const loadElement = (element: Element) => {
    if (element instanceof HTMLImageElement) {
      const dataSrc = element.getAttribute('data-src');
      if (dataSrc) {
        const img = new Image();
        
        // 添加加载动画类
        element.classList.add('lazy-loading');
        
        img.onload = () => {
          element.src = dataSrc;
          element.classList.remove('lazy-loading');
          element.classList.add('lazy-loaded');
          element.removeAttribute('data-src');
        };
        
        img.onerror = () => {
          element.classList.remove('lazy-loading');
          element.classList.add('lazy-error');
          
          const errorImage = options.errorImage || element.getAttribute('data-error');
          if (errorImage) {
            element.src = errorImage;
          }
        };
        
        img.src = dataSrc;
      }
    } else if (element instanceof HTMLElement) {
      const dataBg = element.getAttribute('data-bg');
      if (dataBg) {
        element.style.backgroundImage = `url(${dataBg})`;
        element.classList.add('lazy-loaded');
        element.removeAttribute('data-bg');
      }
    }
  };

  // 观察元素
  const observe = (element: Element) => {
    if (!observer.value) {
      observer.value = createObserver();
    }

    if (observer.value) {
      observer.value.observe(element);
      observedElements.add(element);
    } else {
      // 降级：直接加载
      loadElement(element);
    }
  };

  // 停止观察元素
  const unobserve = (element: Element) => {
    if (observer.value) {
      observer.value.unobserve(element);
      observedElements.delete(element);
    }
  };

  // 清理所有观察
  const cleanup = () => {
    if (observer.value) {
      observer.value.disconnect();
      observedElements.clear();
    }
  };

  // 组件卸载时清理
  onUnmounted(() => {
    cleanup();
  });

  return {
    observe,
    unobserve,
    cleanup,
  };
}

// 图片预加载
export function useImagePreload() {
  const preloadedImages = new Set<string>();

  const preload = (src: string): Promise<void> => {
    if (preloadedImages.has(src)) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      const img = new Image();
      
      img.onload = () => {
        preloadedImages.add(src);
        resolve();
      };
      
      img.onerror = reject;
      
      img.src = src;
    });
  };

  const preloadMultiple = (srcs: string[]): Promise<void[]> => {
    return Promise.all(srcs.map(preload));
  };

  return {
    preload,
    preloadMultiple,
    preloadedImages: preloadedImages as ReadonlySet<string>,
  };
}

// 响应式图片加载
export function useResponsiveImage(
  sources: {
    src: string;
    width?: number;
    media?: string;
  }[]
) {
  const currentSrc = ref('');

  const updateSource = () => {
    // 根据屏幕尺寸选择合适的图片源
    const matchedSource = sources.find(source => {
      if (source.media) {
        return window.matchMedia(source.media).matches;
      }
      if (source.width) {
        return window.innerWidth >= source.width;
      }
      return false;
    });

    currentSrc.value = matchedSource?.src || sources[0]?.src || '';
  };

  onMounted(() => {
    updateSource();
    window.addEventListener('resize', updateSource);
  });

  onUnmounted(() => {
    window.removeEventListener('resize', updateSource);
  });

  return {
    currentSrc,
  };
}