<script setup lang="ts">
import { computed } from 'vue';
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
} from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const activeMenu = computed(() => route.path);

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
          :icon="ArrowLeft"
          text
          size="large"
          @click="goBack"
          style="margin-right: 16px"
        >
          返回前台
        </el-button>
        <h2 class="system-title">反诈骗信息识别与风险自测系统 - 管理后台</h2>
      </div>
      <div class="header-right">
        <el-dropdown>
          <span class="admin-info">
            <el-icon><User /></el-icon>
            <span style="margin-left: 8px">{{ userStore.name || '管理员' }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container>
      <!-- 左侧 Sidebar -->
      <el-aside width="240px" class="admin-sidebar">
        <el-menu
          :default-active="activeMenu"
          class="admin-menu"
          router
          :collapse="false"
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
  background: #f5f7fa;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);

  .header-left {
    display: flex;
    align-items: center;

    .system-title {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }
  }

  .header-right {
    .admin-info {
      display: flex;
      align-items: center;
      cursor: pointer;
      color: #606266;
      font-size: 14px;
      padding: 8px 12px;
      border-radius: 4px;
      transition: background-color 0.3s;

      &:hover {
        background-color: #f5f7fa;
      }
    }
  }
}

.admin-sidebar {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;

  .admin-menu {
    border-right: none;
    height: 100%;
  }
}

.admin-content {
  padding: 24px;
  background: #f5f7fa;
  overflow-y: auto;

  .content-wrapper {
    min-height: calc(100vh - 112px);
  }
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.25s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
