<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { UserFilled, SwitchButton } from '@element-plus/icons-vue';
import { useUserStore } from '../stores/user';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const active = computed(() => {
  if (route.path === '/') return '/';
  return route.path;
});

const goHome = () => {
  router.push('/');
};

const isAdmin = computed(() => userStore.isAdmin);
const isAuthed = computed(() => !!userStore.userId);

const goLogin = () => {
  router.push('/login');
};

const goProfile = () => {
  router.push('/user-center');
};

const logout = () => {
  userStore.logout();
  router.push('/');
};

const handleCommand = (command: string) => {
  if (command === 'profile') {
    goProfile();
  } else if (command === 'logout') {
    logout();
  }
};

// 导航菜单项配置
const menuItems = computed(() => [
  { path: '/', label: '首页' },
  { path: '/train', label: '识别训练' },
  { path: '/assessment', label: '风险测评' },
  { path: '/knowledge', label: '防骗知识' },
  { path: '/achievement', label: '成就页' },
  ...(isAdmin.value ? [{ path: '/admin', label: '管理' }] : []),
]);

const navigate = (path: string) => {
  router.push(path);
};
</script>

<template>
  <el-container class="app-shell">
    <el-header height="64px" class="app-header">
      <div class="brand" @click="goHome">Anti-Fraud Lab</div>
      <div class="nav-right">
        <nav class="main-nav-menu">
          <a
            v-for="item in menuItems"
            :key="item.path"
            :class="['nav-item', { active: active === item.path }]"
            @click.prevent="navigate(item.path)"
          >
            {{ item.label }}
          </a>
        </nav>
        <div class="user-area">
          <el-button v-if="!isAuthed" size="small" type="primary" plain @click="goLogin">
            登录 / 注册
          </el-button>
          <el-dropdown v-else trigger="hover" @command="handleCommand">
            <div class="user-avatar-wrapper" @click.stop="goProfile">
              <el-avatar 
                :size="36" 
                :src="userStore.avatar || undefined"
                class="user-avatar"
              >
                <el-icon :size="18"><UserFilled /></el-icon>
              </el-avatar>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><UserFilled /></el-icon>
                  <span style="margin-left: 8px">个人中心</span>
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  <span style="margin-left: 8px">退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>
    <el-container>
      <el-main class="app-main">
        <div class="af-page">
          <slot />
        </div>
      </el-main>
    </el-container>
    <el-footer height="56px" class="app-footer">
      <div class="footer-inner">
        <div class="text-muted">
          免责声明：本系统用于教学与科普演示，案例为仿真数据，不构成法律或金融建议。
        </div>
        <div class="text-muted">© Anti-Fraud Lab</div>
      </div>
    </el-footer>
  </el-container>
</template>

<style scoped lang="scss">
.app-shell {
  min-height: 100vh;
}
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid var(--el-border-color);
  background: #fff;
  overflow: visible;
  // 确保header有足够空间，不压缩导航
  min-width: 0;
}
.brand {
  font-weight: 700;
  font-size: 20px;
  cursor: pointer;
  flex-shrink: 0; // 品牌不收缩
  white-space: nowrap;
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  justify-content: flex-end;
  min-width: 0;
  overflow: visible;
}

// 自定义导航菜单样式
.main-nav-menu {
  display: flex;
  align-items: center;
  gap: 0;
  list-style: none;
  margin: 0;
  padding: 0;
  height: 64px;
  flex-wrap: nowrap;
  overflow: visible;
  
  .nav-item {
    display: inline-flex;
    align-items: center;
    padding: 0 16px;
    height: 64px;
    line-height: 64px;
    color: var(--el-text-color-primary);
    text-decoration: none;
    cursor: pointer;
    font-size: 14px;
    white-space: nowrap;
    flex-shrink: 0;
    transition: color 0.3s;
    border-bottom: 2px solid transparent;
    
    &:hover {
      color: var(--af-primary, var(--el-color-primary));
    }
    
    &.active {
      color: var(--af-primary, var(--el-color-primary));
      border-bottom-color: var(--af-primary, var(--el-color-primary));
      font-weight: 500;
    }
  }
}
.user-area {
  display: flex;
  align-items: center;
}
.user-avatar-wrapper {
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: transform 0.2s;
  
  &:hover {
    transform: scale(1.05);
  }
}

.user-avatar {
  border: 2px solid var(--el-border-color);
  transition: all 0.2s;
  
  &:hover {
    border-color: var(--el-color-primary);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }
}
.app-main {
  padding: 0;
  background: var(--af-bg);
}
.app-footer {
  background: #fff;
  border-top: 1px solid var(--el-border-color);
}
.footer-inner {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}
</style>

