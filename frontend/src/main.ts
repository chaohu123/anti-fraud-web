import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import './styles/global.scss';
import App from './App.vue';
import router from './router';
import { useUserStore } from './stores/user';
import { useAssessmentStore } from './store/assessment';
import { useKnowledgeStore } from './store/knowledge';

const app = createApp(App);
app.use(createPinia());
app.use(router);
app.use(ElementPlus);
app.mount('#app');

// 初始化本地持久化数据
try {
  const store = useUserStore();
  store.hydrate();
} catch {
  // ignore
}

try {
  const assessment = useAssessmentStore();
  assessment.hydrate();
} catch {
  // ignore
}

try {
  const knowledge = useKnowledgeStore();
  knowledge.hydrate();
} catch {
  // ignore
}
