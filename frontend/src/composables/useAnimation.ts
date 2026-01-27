import { ref, onMounted, onUnmounted } from 'vue';

export interface AnimationOptions {
  duration?: number;
  easing?: string;
  delay?: number;
  iterations?: number;
  fill?: 'none' | 'forwards' | 'backwards' | 'both';
}

export function useAnimation() {
  const isAnimating = ref(false);

  // 淡入动画
  const fadeIn = (element: HTMLElement, options: AnimationOptions = {}) => {
    const {
      duration = 300,
      easing = 'ease',
      delay = 0,
      iterations = 1,
      fill = 'forwards',
    } = options;

    return element.animate(
      [
        { opacity: 0 },
        { opacity: 1 },
      ],
      {
        duration,
        easing,
        delay,
        iterations,
        fill,
      }
    );
  };

  // 淡出动画
  const fadeOut = (element: HTMLElement, options: AnimationOptions = {}) => {
    const {
      duration = 300,
      easing = 'ease',
      delay = 0,
      iterations = 1,
      fill = 'forwards',
    } = options;

    return element.animate(
      [
        { opacity: 1 },
        { opacity: 0 },
      ],
      {
        duration,
        easing,
        delay,
        iterations,
        fill,
      }
    );
  };

  // 滑入动画
  const slideIn = (
    element: HTMLElement,
    direction: 'left' | 'right' | 'top' | 'bottom' = 'left',
    options: AnimationOptions = {}
  ) => {
    const {
      duration = 300,
      easing = 'ease',
      delay = 0,
      iterations = 1,
      fill = 'forwards',
    } = options;

    const transforms = {
      left: ['translateX(-100%)', 'translateX(0)'],
      right: ['translateX(100%)', 'translateX(0)'],
      top: ['translateY(-100%)', 'translateY(0)'],
      bottom: ['translateY(100%)', 'translateY(0)'],
    };

    return element.animate(
      [
        { transform: transforms[direction][0], opacity: 0 },
        { transform: transforms[direction][1], opacity: 1 },
      ],
      {
        duration,
        easing,
        delay,
        iterations,
        fill,
      }
    );
  };

  // 缩放动画
  const scale = (
    element: HTMLElement,
    from: number,
    to: number,
    options: AnimationOptions = {}
  ) => {
    const {
      duration = 300,
      easing = 'ease',
      delay = 0,
      iterations = 1,
      fill = 'forwards',
    } = options;

    return element.animate(
      [
        { transform: `scale(${from})`, opacity: from === 0 ? 0 : 1 },
        { transform: `scale(${to})`, opacity: to === 0 ? 0 : 1 },
      ],
      {
        duration,
        easing,
        delay,
        iterations,
        fill,
      }
    );
  };

  // 弹跳动画
  const bounce = (element: HTMLElement, options: AnimationOptions = {}) => {
    const {
      duration = 600,
      easing = 'ease',
      delay = 0,
      iterations = 1,
      fill = 'forwards',
    } = options;

    return element.animate(
      [
        { transform: 'translateY(0)' },
        { transform: 'translateY(-20px)' },
        { transform: 'translateY(0)' },
        { transform: 'translateY(-10px)' },
        { transform: 'translateY(0)' },
      ],
      {
        duration,
        easing,
        delay,
        iterations,
        fill,
      }
    );
  };

  // 震动动画
  const shake = (element: HTMLElement, options: AnimationOptions = {}) => {
    const {
      duration = 400,
      easing = 'ease',
      delay = 0,
      iterations = 1,
      fill = 'forwards',
    } = options;

    return element.animate(
      [
        { transform: 'translateX(0)' },
        { transform: 'translateX(-10px)' },
        { transform: 'translateX(10px)' },
        { transform: 'translateX(-10px)' },
        { transform: 'translateX(10px)' },
        { transform: 'translateX(0)' },
      ],
      {
        duration,
        easing,
        delay,
        iterations,
        fill,
      }
    );
  };

  // 脉冲动画
  const pulse = (element: HTMLElement, options: AnimationOptions = {}) => {
    const {
      duration = 1000,
      easing = 'ease',
      delay = 0,
      iterations = Infinity,
      fill = 'none',
    } = options;

    return element.animate(
      [
        { transform: 'scale(1)', opacity: 1 },
        { transform: 'scale(1.05)', opacity: 0.8 },
        { transform: 'scale(1)', opacity: 1 },
      ],
      {
        duration,
        easing,
        delay,
        iterations,
        fill,
      }
    );
  };

  // 翻转动画
  const flip = (element: HTMLElement, axis: 'x' | 'y' = 'y', options: AnimationOptions = {}) => {
    const {
      duration = 600,
      easing = 'ease',
      delay = 0,
      iterations = 1,
      fill = 'forwards',
    } = options;

    const transforms = {
      x: ['rotateX(0deg)', 'rotateX(180deg)'],
      y: ['rotateY(0deg)', 'rotateY(180deg)'],
    };

    return element.animate(
      [
        { transform: transforms[axis][0] },
        { transform: transforms[axis][1] },
      ],
      {
        duration,
        easing,
        delay,
        iterations,
        fill,
      }
    );
  };

  // 连续动画序列
  const sequence = async (
    element: HTMLElement,
    animations: Array<{
      keyframes: Keyframe[];
      options?: AnimationOptions;
    }>
  ) => {
    isAnimating.value = true;

    for (const { keyframes, options = {} } of animations) {
      const animation = element.animate(keyframes, {
        duration: options.duration || 300,
        easing: options.easing || 'ease',
        delay: options.delay || 0,
        iterations: options.iterations || 1,
        fill: options.fill || 'forwards',
      });

      await animation.finished;
    }

    isAnimating.value = false;
  };

  // 停止所有动画
  const stop = (element: HTMLElement) => {
    const animations = element.getAnimations();
    animations.forEach((animation) => animation.cancel());
  };

  return {
    isAnimating,
    fadeIn,
    fadeOut,
    slideIn,
    scale,
    bounce,
    shake,
    pulse,
    flip,
    sequence,
    stop,
  };
}

// 滚动动画Hook
export function useScrollAnimation() {
  const scrollTo = (
    element: HTMLElement | string,
    options: ScrollIntoViewOptions = {}
  ) => {
    const target = typeof element === 'string' 
      ? document.querySelector(element) as HTMLElement
      : element;

    if (!target) return;

    target.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
      inline: 'nearest',
      ...options,
    });
  };

  const scrollToTop = (smooth = true) => {
    window.scrollTo({
      top: 0,
      behavior: smooth ? 'smooth' : 'auto',
    });
  };

  const scrollToBottom = (smooth = true) => {
    window.scrollTo({
      top: document.documentElement.scrollHeight,
      behavior: smooth ? 'smooth' : 'auto',
    });
  };

  return {
    scrollTo,
    scrollToTop,
    scrollToBottom,
  };
}

// 视差滚动效果
export function useParallax(element: HTMLElement, speed = 0.5) {
  let rafId: number | null = null;

  const handleScroll = () => {
    rafId = requestAnimationFrame(() => {
      const scrolled = window.pageYOffset;
      const offset = scrolled * speed;
      element.style.transform = `translateY(${offset}px)`;
    });
  };

  const enable = () => {
    window.addEventListener('scroll', handleScroll, { passive: true });
  };

  const disable = () => {
    window.removeEventListener('scroll', handleScroll);
    if (rafId) {
      cancelAnimationFrame(rafId);
    }
  };

  onMounted(() => {
    enable();
  });

  onUnmounted(() => {
    disable();
  });

  return {
    enable,
    disable,
  };
}