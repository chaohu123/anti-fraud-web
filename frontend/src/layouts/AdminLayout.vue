<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '../stores/user';
import {
  DataBoard,
  Document,
  QuestionFilled,
  EditPen,
  Collection,
  Trophy,
  User,
  Setting,
  ArrowLeft,
  Fold,
  Expand,
  ArrowDown,
  HomeFilled,
  SwitchButton,
} from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const activeMenu = computed(() => route.path);
const isCollapse = ref(false);

// 面包屑导航
const breadcrumbItems = computed(() => {
  const path = route.path;
  const items: { title: string; path?: string }[] = [{ title: '管理后台', path: '/admin/dashboard' }];
  
  const menuMap: Record<string, string> = {
    '/admin/dashboard': '仪表盘',
    '/admin/cases': '诈骗案例管理',
    '/admin/training': '识别训练题目管理',
    '/admin/assessment': '风险测评问卷管理',
    '/admin/knowledge': '防骗知识库管理',
    '/admin/achievement': '成就规则管理',
    '/admin/users': '用户数据管理',
    '/admin/settings': '系统设置',
  };
  
  if (path !== '/admin/dashboard' && menuMap[path]) {
    items.push({ title: menuMap[path] });
  }
  
  return items;
});

const toggleSidebar = () => {
  isCollapse.value = !isCollapse.value;
};

const menuItems = [
  {
    path: '/admin/dashboard',
    title: '仪表盘',
    icon: DataBoard,
  },
  {
    path: '/admin/cases',
    title: '诈骗案例管理',
    icon: Document,
  },
  {
    path: '/admin/training',
    title: '识别训练题目管理',
    icon: QuestionFilled,
  },
  {
    path: '/admin/assessment',
    title: '风险测评问卷管理',
    icon: EditPen,
  },
  {
    path: '/admin/knowledge',
    title: '防骗知识库管理',
    icon: Collection,
  },
  {
    path: '/admin/achievement',
    title: '成就规则管理',
    icon: Trophy,
  },
  {
    path: '/admin/users',
    title: '用户数据管理',
    icon: User,
  },
  {
    path: '/admin/settings',
    title: '系统设置',
    icon: Setting,
  },
];

const handleMenuClick = (path: string) => {
  router.push(path);
};

const goBack = () => {
  router.push('/');
};

const logout = () => {
  userStore.logout();
  router.push('/');
};
</script>

<template>
  <el-container class="admin-layout">
    <!-- 顶部 Header -->
    <el-header height="64px" class="admin-header">
      <div class="header-left">
        <el-button
          :icon="isCollapse ? Expand : Fold"
          text
          size="large"
          @click="toggleSidebar"
          class="collapse-btn"
        />
        <el-button
          :icon="ArrowLeft"
          text
          size="large"
          @click="goBack"
          class="back-btn"
        >
          返回前台
        </el-button>
        <div class="header-title">
          <h2 class="system-title">管理后台</h2>
          <span class="system-subtitle">反诈骗信息识别与风险自测系统</span>
        </div>
      </div>
      <div class="header-right">
        <el-dropdown trigger="click">
          <span class="admin-info">
            <el-avatar :size="32" :src="userStore.avatar || undefined" class="admin-avatar">
              <el-icon><User /></el-icon>
            </el-avatar>
            <span class="admin-name">{{ userStore.name || '管理员' }}</span>
            <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="goBack">
                <el-icon><HomeFilled /></el-icon>
                <span style="margin-left: 8px">返回前台</span>
              </el-dropdown-item>
              <el-dropdown-item divided @click="logout">
                <el-icon><SwitchButton /></el-icon>
                <span style="margin-left: 8px">退出登录</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container>
      <!-- 左侧 Sidebar -->
      <el-aside :width="isCollapse ? '64px' : '240px'" class="admin-sidebar">
        <el-menu
          :default-active="activeMenu"
          class="admin-menu"
          router
          :collapse="isCollapse"
          :collapse-transition="true"
        >
          <el-menu-item
            v-for="item in menuItems"
            :key="item.path"
            :index="item.path"
            @click="handleMenuClick(item.path)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 右侧 Content -->
      <el-main class="admin-content">
        <div class="content-wrapper">
          <!-- 面包屑导航 -->
          <el-breadcrumb separator="/" class="breadcrumb-nav">
            <el-breadcrumb-item
              v-for="(item, index) in breadcrumbItems"
              :key="index"
              :to="item.path"
            >
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
          
          <router-view v-slot="{ Component }">
            <transition name="fade-slide" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
.admin-layout {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e9ecef 100%);
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-bottom: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 100;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .collapse-btn,
    .back-btn {
      color: #fff;
      &:hover {
        background-color: rgba(255, 255, 255, 0.1);
      }
    }

    .header-title {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .system-title {
        margin: 0;
        font-size: 20px;
        font-weight: 700;
        color: #fff;
        line-height: 1.2;
      }

      .system-subtitle {
        font-size: 12px;
        color: rgba(255, 255, 255, 0.8);
        font-weight: 400;
      }
    }
  }

  .header-right {
    .admin-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      color: #fff;
      font-size: 14px;
      padding: 8px 16px;
      border-radius: 24px;
      transition: all 0.3s;
      background: rgba(255, 255, 255, 0.1);
      backdrop-filter: blur(10px);

      &:hover {
        background: rgba(255, 255, 255, 0.2);
        transform: translateY(-1px);
      }

      .admin-avatar {
        border: 2px solid rgba(255, 255, 255, 0.3);
      }

      .admin-name {
        font-weight: 500;
      }

      .dropdown-icon {
        font-size: 12px;
        transition: transform 0.3s;
      }
    }
  }
}

.admin-sidebar {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;
  overflow-x: hidden;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.05);
  transition: width 0.3s;

  .admin-menu {
    border-right: none;
    height: 100%;
    padding: 8px 0;

    :deep(.el-menu-item) {
      margin: 4px 8px;
      border-radius: 8px;
      height: 48px;
      line-height: 48px;
      transition: all 0.3s;

      &:hover {
        background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
        color: #667eea;
        transform: translateX(4px);
      }

      &.is-active {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;
        font-weight: 600;
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);

        .el-icon {
          color: #fff;
        }
      }
    }
  }
}

.admin-content {
  padding: 24px;
  background: transparent;
  overflow-y: auto;
  position: relative;

  .content-wrapper {
    min-height: calc(100vh - 112px);

    .breadcrumb-nav {
      margin-bottom: 20px;
      padding: 12px 20px;
      background: #fff;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

      :deep(.el-breadcrumb__inner) {
        color: #606266;
        font-weight: 500;

        &.is-link {
          color: #667eea;
          transition: color 0.3s;

          &:hover {
            color: #764ba2;
          }
        }
      }
    }
  }
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px) scale(0.98);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.98);
}

// 响应式设计
@media (max-width: 768px) {
  .admin-header {
    padding: 0 16px;

    .header-left {
      .header-title {
        .system-title {
          font-size: 16px;
        }

        .system-subtitle {
          display: none;
        }
      }
    }

    .header-right {
      .admin-info {
        .admin-name {
          display: none;
        }
      }
    }
  }

  .admin-sidebar {
    position: fixed;
    left: 0;
    top: 64px;
    height: calc(100vh - 64px);
    z-index: 99;
    transform: translateX(-100%);
    transition: transform 0.3s;

    &.is-open {
      transform: translateX(0);
    }
  }

  .admin-content {
    padding: 16px;
  }
}
</style>
