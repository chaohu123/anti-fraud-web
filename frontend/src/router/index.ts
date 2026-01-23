import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { loadJson } from '../utils/storage';
import Home from '../views/Home.vue';
import Train from '../views/Training.vue';
import Assessment from '../views/Assessment.vue';
import Report from '../views/Report.vue';
import Knowledge from '../views/Knowledge.vue';
import KnowledgeDetail from '../views/KnowledgeDetail.vue';
import Profile from '../views/Profile.vue';
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
  { path: '/report', name: 'report', component: Report },
  { path: '/knowledge', name: 'knowledge', component: Knowledge },
  { path: '/knowledge/:id', name: 'knowledge-detail', component: KnowledgeDetail },
  { path: '/profile', name: 'profile', component: Profile, meta: { requiresAuth: true } },
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
  const user = loadJson<{ userId?: number | null; username?: string | null }>('af_user', {} as any);
  const isAuthed = !!user?.userId;
  const isAdmin = (user?.username || '').toLowerCase() === 'admin';

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
