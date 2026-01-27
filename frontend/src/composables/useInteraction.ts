import { ref, computed, onMounted, onUnmounted } from 'vue';

// 拖拽功能Hook
export function useDraggable(options: {
  onDragStart?: (event: MouseEvent) => void;
  onDragMove?: (event: MouseEvent, delta: { x: number; y: number }) => void;
  onDragEnd?: (event: MouseEvent) => void;
} = {}) {
  const isDragging = ref(false);
  const startPos = ref({ x: 0, y: 0 });
  const currentPos = ref({ x: 0, y: 0 });

  const delta = computed(() => ({
    x: currentPos.value.x - startPos.value.x,
    y: currentPos.value.y - startPos.value.y,
  }));

  const handleMouseDown = (event: MouseEvent) => {
    isDragging.value = true;
    startPos.value = { x: event.clientX, y: event.clientY };
    currentPos.value = { x: event.clientX, y: event.clientY };
    options.onDragStart?.(event);
  };

  const handleMouseMove = (event: MouseEvent) => {
    if (!isDragging.value) return;

    currentPos.value = { x: event.clientX, y: event.clientY };
    options.onDragMove?.(event, delta.value);
  };

  const handleMouseUp = (event: MouseEvent) => {
    if (!isDragging.value) return;

    isDragging.value = false;
    options.onDragEnd?.(event);
  };

  const enable = (element: HTMLElement) => {
    element.addEventListener('mousedown', handleMouseDown);
    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
  };

  const disable = () => {
    window.removeEventListener('mousemove', handleMouseMove);
    window.removeEventListener('mouseup', handleMouseUp);
  };

  onUnmounted(() => {
    disable();
  });

  return {
    isDragging,
    delta,
    enable,
    disable,
    startPos,
    currentPos,
  };
}

// 双击功能Hook
export function useDoubleClick(
  callback: () => void,
  delay = 300
) {
  let clickTimeout: number | null = null;
  let clickCount = 0;

  const handleClick = () => {
    clickCount++;

    if (clickCount === 1) {
      clickTimeout = window.setTimeout(() => {
        clickCount = 0;
      }, delay);
    } else if (clickCount === 2) {
      if (clickTimeout) {
        clearTimeout(clickTimeout);
      }
      clickCount = 0;
      callback();
    }
  };

  return {
    handleClick,
  };
}

// 长按功能Hook
export function useLongPress(
  callback: () => void,
  duration = 500
) {
  let pressTimer: number | null = null;
  const isPressed = ref(false);

  const start = () => {
    isPressed.value = true;
    pressTimer = window.setTimeout(() => {
      callback();
    }, duration);
  };

  const cancel = () => {
    isPressed.value = false;
    if (pressTimer) {
      clearTimeout(pressTimer);
      pressTimer = null;
    }
  };

  return {
    isPressed,
    start,
    cancel,
  };
}

// 悬停提示Hook
export function useTooltip() {
  const showTooltip = ref(false);
  const tooltipPosition = ref({ x: 0, y: 0 });
  const tooltipContent = ref('');

  const show = (event: MouseEvent, content: string) => {
    tooltipContent.value = content;
    tooltipPosition.value = {
      x: event.clientX,
      y: event.clientY,
    };
    showTooltip.value = true;
  };

  const hide = () => {
    showTooltip.value = false;
  };

  const updatePosition = (event: MouseEvent) => {
    tooltipPosition.value = {
      x: event.clientX,
      y: event.clientY,
    };
  };

  return {
    showTooltip,
    tooltipPosition,
    tooltipContent,
    show,
    hide,
    updatePosition,
  };
}

// 手势识别Hook（用于移动端）
export function useGesture() {
  const startTouch = ref<Touch | null>(null);
  const currentTouch = ref<Touch | null>(null);

  const delta = computed(() => {
    if (!startTouch.value || !currentTouch.value) {
      return { x: 0, y: 0 };
    }
    return {
      x: currentTouch.value.clientX - startTouch.value.clientX,
      y: currentTouch.value.clientY - startTouch.value.clientY,
    };
  });

  const direction = computed(() => {
    const { x, y } = delta.value;
    const absX = Math.abs(x);
    const absY = Math.abs(y);

    if (absX > absY) {
      return x > 0 ? 'right' : 'left';
    } else {
      return y > 0 ? 'down' : 'up';
    }
  });

  const handleTouchStart = (event: TouchEvent) => {
    startTouch.value = event.touches[0];
    currentTouch.value = event.touches[0];
  };

  const handleTouchMove = (event: TouchEvent) => {
    currentTouch.value = event.touches[0];
  };

  const handleTouchEnd = (callback: (direction: string, delta: { x: number; y: number }) => void) => {
    if (startTouch.value && currentTouch.value) {
      const threshold = 50; // 最小滑动距离

      if (Math.abs(delta.value.x) > threshold || Math.abs(delta.value.y) > threshold) {
        callback(direction.value, delta.value);
      }
    }

    startTouch.value = null;
    currentTouch.value = null;
  };

  return {
    delta,
    direction,
    handleTouchStart,
    handleTouchMove,
    handleTouchEnd,
  };
}

// 滚动监听Hook
export function useScroll() {
  const scrollY = ref(0);
  const scrollX = ref(0);
  const scrollDirection = ref<'up' | 'down' | null>(null);
  const isScrolling = ref(false);

  let lastScrollY = 0;
  let scrollTimeout: number | null = null;

  const handleScroll = () => {
    scrollY.value = window.pageYOffset;
    scrollX.value = window.pageXOffset;

    // 判断滚动方向
    scrollDirection.value = scrollY.value > lastScrollY ? 'down' : 'up';
    lastScrollY = scrollY.value;

    // 检测滚动状态
    isScrolling.value = true;
    if (scrollTimeout) {
      clearTimeout(scrollTimeout);
    }
    scrollTimeout = window.setTimeout(() => {
      isScrolling.value = false;
    }, 150);
  };

  onMounted(() => {
    window.addEventListener('scroll', handleScroll, { passive: true });
    handleScroll(); // 初始化
  });

  onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll);
    if (scrollTimeout) {
      clearTimeout(scrollTimeout);
    }
  });

  return {
    scrollY,
    scrollX,
    scrollDirection,
    isScrolling,
  };
}

// 键盘快捷键Hook
export function useHotkey(key: string, callback: () => void, modifiers: {
  ctrl?: boolean;
  alt?: boolean;
  shift?: boolean;
  meta?: boolean;
} = {}) {
  const handleKeydown = (event: KeyboardEvent) => {
    const matchesKey = event.key.toLowerCase() === key.toLowerCase();
    const matchesCtrl = !modifiers.ctrl || event.ctrlKey;
    const matchesAlt = !modifiers.alt || event.altKey;
    const matchesShift = !modifiers.shift || event.shiftKey;
    const matchesMeta = !modifiers.meta || event.metaKey;

    if (matchesKey && matchesCtrl && matchesAlt && matchesShift && matchesMeta) {
      event.preventDefault();
      callback();
    }
  };

  onMounted(() => {
    window.addEventListener('keydown', handleKeydown);
  });

  onUnmounted(() => {
    window.removeEventListener('keydown', handleKeydown);
  });

  return {
    handleKeydown,
  };
}