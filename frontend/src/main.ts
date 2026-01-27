import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import './styles/global.scss';
import App from './App.vue';
import router from './router';
import { useUserStore } from './stores/user';
import { useAssessmentStore } from './store/assessment';
import { useKnowledgeStore } from './store/knowledge';
import { useAchievementStore } from './store/achievement';

// 再次确保清除所有 Service Worker（双重保险）
// 使用立即执行函数，确保在应用启动前清除
(function clearServiceWorkers() {
  if ('serviceWorker' in navigator) {
    // 立即清除所有注册
    navigator.serviceWorker.getRegistrations().then((registrations) => {
      registrations.forEach((registration) => {
        registration.unregister().catch(() => {
          // 静默忽略错误
        });
      });
    }).catch(() => {
      // 静默忽略错误
    });
    
    // 清除所有缓存
    if ('caches' in window) {
      caches.keys().then((names) => {
        names.forEach((name) => {
          caches.delete(name).catch(() => {
            // 静默忽略错误
          });
        });
      }).catch(() => {
        // 静默忽略错误
      });
    }
    
    // 如果已经有活动的 Service Worker，立即注销
    if (navigator.serviceWorker.controller) {
      navigator.serviceWorker.controller.postMessage({ type: 'SKIP_WAITING' });
      navigator.serviceWorker.getRegistrations().then((registrations) => {
        registrations.forEach((registration) => {
          if (registration.active) {
            registration.active.postMessage({ type: 'SKIP_WAITING' });
          }
          registration.unregister();
        });
      });
    }
    
    // 监听控制器变化，立即注销新的 Service Worker
    navigator.serviceWorker.addEventListener('controllerchange', () => {
      navigator.serviceWorker.getRegistrations().then((registrations) => {
        registrations.forEach((registration) => {
          registration.unregister();
        });
      });
    });
  }
})();

const app = createApp(App);
app.use(createPinia());
app.use(router);
// Element Plus 全局中文：修复分页显示 “/page” 等英文文案
app.use(ElementPlus, { locale: zhCn });
app.mount('#app');

// 初始化本地持久化数据
try {
  const store = useUserStore();
  store.hydrate();
} catch {
  // ignore
}

try {
  const user = useUserStore();
  const assessment = useAssessmentStore();
  assessment.hydrate(user.userId);
} catch {
  // ignore
}

try {
  const user = useUserStore();
  const knowledge = useKnowledgeStore();
  knowledge.hydrate(user.userId);
} catch {
  // ignore
}

try {
  const user = useUserStore();
  const achievement = useAchievementStore();
  achievement.hydrate(user.userId);
} catch {
  // ignore
}
