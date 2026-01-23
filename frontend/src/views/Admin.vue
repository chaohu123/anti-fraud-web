<script setup lang="ts">
// 管理后台入口，使用 AdminLayout 布局
import AdminLayout from '../layouts/AdminLayout.vue';
import { useRouter } from 'vue-router';
import { onMounted } from 'vue';

const router = useRouter();

onMounted(() => {
  // 默认跳转到仪表盘
  if (router.currentRoute.value.path === '/admin') {
    router.replace('/admin/dashboard');
  }
});
</script>

<template>
  <AdminLayout>
    <router-view />
  </AdminLayout>
</template>
