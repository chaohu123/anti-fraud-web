import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { loadJson } from '../utils/storage';
import Home from '../views/Home.vue';
import Train from '../views/Training.vue';
import Assessment from '../views/Assessment.vue';
import ReportDetail from '../views/ReportDetail.vue';
import Knowledge from '../views/Knowledge.vue';
import LearnedKnowledge from '../views/LearnedKnowledge.vue';
import KnowledgeDetail from '../views/KnowledgeDetail.vue';
import UserCenter from '../views/UserCenter.vue';
import Admin from '../views/Admin.vue';
import Achievement from '../views/Achievement.vue';
import Login from '../views/Login.vue';

// 管理后台页面
import Dashboard from '../views/admin/Dashboard.vue';
import FraudCaseManagement from '../views/admin/FraudCaseManagement.vue';
import TrainingQuestionManagement from '../views/admin/TrainingQuestionManagement.vue';
import RiskAssessmentManagement from '../views/admin/RiskAssessmentManagement.vue';
import KnowledgeManagement from '../views/admin/KnowledgeManagement.vue';
import AchievementManagement from '../views/admin/AchievementManagement.vue';
import UserManagement from '../views/admin/UserManagement.vue';
import SystemSettings from '../views/admin/SystemSettings.vue';

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: Login, meta: { noLayout: true } },
  { path: '/', name: 'home', component: Home },
  { path: '/train', name: 'train', component: Train },
  { path: '/assessment', name: 'assessment', component: Assessment },
  { path: '/report', redirect: '/assessment' }, // 重定向到测评页
  { path: '/report/:id', name: 'report-detail', component: ReportDetail },
  { path: '/knowledge', name: 'knowledge', component: Knowledge },
  { path: '/knowledge/learned', name: 'knowledge-learned', component: LearnedKnowledge },
  { path: '/knowledge/:id', name: 'knowledge-detail', component: KnowledgeDetail },
  // 兼容旧路径：/profile 直接跳转到新版个人中心 /user-center
  { path: '/profile', redirect: '/user-center' },
  { path: '/user-center', name: 'user-center', component: UserCenter },
  { path: '/achievement', name: 'achievement', component: Achievement },
  {
    path: '/admin',
    name: 'admin',
    component: Admin,
    meta: { requiresAuth: true, requiresAdmin: true, noLayout: true },
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: Dashboard,
      },
      {
        path: 'cases',
        name: 'admin-cases',
        component: FraudCaseManagement,
      },
      {
        path: 'training',
        name: 'admin-training',
        component: TrainingQuestionManagement,
      },
      {
        path: 'assessment',
        name: 'admin-assessment',
        component: RiskAssessmentManagement,
      },
      {
        path: 'knowledge',
        name: 'admin-knowledge',
        component: KnowledgeManagement,
      },
      {
        path: 'achievement',
        name: 'admin-achievement',
        component: AchievementManagement,
      },
      {
        path: 'users',
        name: 'admin-users',
        component: UserManagement,
      },
      {
        path: 'settings',
        name: 'admin-settings',
        component: SystemSettings,
      },
    ],
  },
  // 兼容旧路径
  { path: '/challenge', redirect: '/train' },
  { path: '/:pathMatch(.*)*', redirect: '/' },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  },
});

router.beforeEach((to) => {
  // 使用会话 key 判断当前登录身份，避免读取按用户分桶的数据
  const session = loadJson<{ userId?: number | null; username?: string | null }>('af_user_session', {} as any);
  const isAuthed = !!session?.userId;
  const isAdmin = (session?.username || '').toLowerCase() === 'admin';

  const requiresAuth = !!to.meta?.requiresAuth;
  const requiresAdmin = !!to.meta?.requiresAdmin;

  if (requiresAuth && !isAuthed) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  if (requiresAdmin && !isAdmin) {
    return { path: '/', query: { denied: 'admin' } };
  }
  return true;
});

export default router;
