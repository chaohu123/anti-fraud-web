import type { Directive, DirectiveBinding } from 'vue';

interface LazyLoadElement extends HTMLElement {
  _lazyObserver?: IntersectionObserver;
}

// 懒加载指令
export const vLazyLoad: Directive = {
  mounted(el: LazyLoadElement, binding: DirectiveBinding) {
    const options = {
      root: null,
      rootMargin: '50px',
      threshold: 0.01,
      ...binding.value?.options,
    };

    const loadImage = () => {
      if (el instanceof HTMLImageElement) {
        const src = binding.value?.src || el.getAttribute('data-src');
        if (src) {
          // 添加加载状态
          el.classList.add('lazy-loading');

          const img = new Image();
          img.onload = () => {
            el.src = src;
            el.classList.remove('lazy-loading');
            el.classList.add('lazy-loaded');
          };

          img.onerror = () => {
            el.classList.remove('lazy-loading');
            el.classList.add('lazy-error');

            // 使用错误图片
            const errorSrc = binding.value?.error || el.getAttribute('data-error');
            if (errorSrc) {
              el.src = errorSrc;
            }
          };

          img.src = src;
        }
      } else if (el instanceof HTMLElement) {
        const bgSrc = binding.value?.bg || el.getAttribute('data-bg');
        if (bgSrc) {
          el.style.backgroundImage = `url(${bgSrc})`;
          el.classList.add('lazy-loaded');
        }
      }
    };

    // 创建IntersectionObserver
    if ('IntersectionObserver' in window) {
      const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            loadImage();
            observer.unobserve(el);
            el._lazyObserver = undefined;
          }
        });
      }, options);

      observer.observe(el);
      el._lazyObserver = observer;
    } else {
      // 降级：直接加载
      loadImage();
    }
  },

  unmounted(el: LazyLoadElement) {
    if (el._lazyObserver) {
      el._lazyObserver.disconnect();
      el._lazyObserver = undefined;
    }
  },
};

// 懒加载背景图片指令
export const vLazyBg: Directive = {
  mounted(el: LazyLoadElement, binding: DirectiveBinding) {
    const bgSrc = binding.value;
    if (!bgSrc) return;

    const options = {
      root: null,
      rootMargin: '50px',
      threshold: 0.01,
    };

    const loadBg = () => {
      // 预加载图片
      const img = new Image();
      img.onload = () => {
        el.style.backgroundImage = `url(${bgSrc})`;
        el.classList.add('lazy-loaded');
      };
      img.src = bgSrc;
    };

    if ('IntersectionObserver' in window) {
      const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            loadBg();
            observer.unobserve(el);
            el._lazyObserver = undefined;
          }
        });
      }, options);

      observer.observe(el);
      el._lazyObserver = observer;
    } else {
      loadBg();
    }
  },

  unmounted(el: LazyLoadElement) {
    if (el._lazyObserver) {
      el._lazyObserver.disconnect();
      el._lazyObserver = undefined;
    }
  },
};