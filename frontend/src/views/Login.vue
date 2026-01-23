<template>
  <div class="login-page">
    <div class="login-container">
      <!-- 左侧：品牌介绍区 -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="brand-logo">
            <el-icon class="logo-icon"><Lock /></el-icon>
          </div>
          <h1 class="brand-title">反诈骗信息识别与风险自测系统</h1>
          <p class="brand-subtitle">Anti-Fraud Information Recognition & Risk Assessment System</p>
          <div class="brand-slogan">
            <p class="slogan-text">守护您的数字安全</p>
            <p class="slogan-desc">通过智能识别训练与风险评估，提升防骗意识与能力</p>
          </div>
          <div class="brand-features">
            <div class="feature-item">
              <el-icon><MagicStick /></el-icon>
              <span>专业识别训练</span>
            </div>
            <div class="feature-item">
              <el-icon><DataAnalysis /></el-icon>
              <span>风险智能评估</span>
            </div>
            <div class="feature-item">
              <el-icon><Document /></el-icon>
              <span>防骗知识库</span>
            </div>
          </div>
          <div class="brand-illustration">
            <div class="illustration-placeholder">
              <el-icon class="illustration-icon"><Lock /></el-icon>
              <p class="illustration-text">安全防护插画</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：登录/注册卡片 -->
      <div class="form-section">
        <el-card class="form-card" shadow="hover">
          <el-tabs v-model="activeTab" class="auth-tabs">
            <el-tab-pane label="登录" name="login">
              <el-form
                ref="loginFormRef"
                :model="loginForm"
                :rules="loginRules"
                label-width="0"
                class="auth-form"
              >
                <el-form-item prop="username">
                  <el-input
                    v-model="loginForm.username"
                    placeholder="请输入账号"
                    size="large"
                    :prefix-icon="User"
                    clearable
                  />
                </el-form-item>
                <el-form-item prop="password">
                  <el-input
                    v-model="loginForm.password"
                    type="password"
                    placeholder="请输入密码"
                    size="large"
                    :prefix-icon="Lock"
                    show-password
                    clearable
                    @keyup.enter="handleLogin"
                  />
                </el-form-item>
                <el-form-item>
                  <div class="form-options">
                    <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
                  </div>
                </el-form-item>
                <el-form-item>
                  <el-button
                    type="primary"
                    size="large"
                    class="submit-button"
                    :loading="loginLoading"
                    @click="handleLogin"
                  >
                    {{ loginLoading ? '登录中...' : '登录' }}
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="注册" name="register">
              <el-form
                ref="registerFormRef"
                :model="registerForm"
                :rules="registerRules"
                label-width="0"
                class="auth-form"
              >
                <el-form-item prop="nickname">
                  <el-input
                    v-model="registerForm.nickname"
                    placeholder="请输入昵称"
                    size="large"
                    :prefix-icon="User"
                    clearable
                  />
                </el-form-item>
                <el-form-item prop="username">
                  <el-input
                    v-model="registerForm.username"
                    placeholder="请输入账号"
                    size="large"
                    :prefix-icon="User"
                    clearable
                  />
                </el-form-item>
                <el-form-item prop="password">
                  <el-input
                    v-model="registerForm.password"
                    type="password"
                    placeholder="请输入密码"
                    size="large"
                    :prefix-icon="Lock"
                    show-password
                    clearable
                  />
                </el-form-item>
                <el-form-item prop="confirmPassword">
                  <el-input
                    v-model="registerForm.confirmPassword"
                    type="password"
                    placeholder="请确认密码"
                    size="large"
                    :prefix-icon="Lock"
                    show-password
                    clearable
                    @keyup.enter="handleRegister"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button
                    type="primary"
                    size="large"
                    class="submit-button"
                    :loading="registerLoading"
                    @click="handleRegister"
                  >
                    {{ registerLoading ? '注册中...' : '注册' }}
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>

          <!-- 游客体验按钮 -->
          <div class="guest-section">
            <el-divider>
              <span class="divider-text">或</span>
            </el-divider>
            <el-button
              type="info"
              size="large"
              plain
              class="guest-button"
              @click="handleGuestMode"
            >
              <el-icon><UserFilled /></el-icon>
              <span>游客体验</span>
            </el-button>
            <p class="guest-tip">无需注册，直接体验系统功能</p>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { User, Lock, MagicStick, DataAnalysis, Document, UserFilled } from '@element-plus/icons-vue';
import { useUserStore } from '../stores/user';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

// Tab 切换
const activeTab = ref<'login' | 'register'>('login');

// 登录表单
const loginFormRef = ref<FormInstance>();
const loginLoading = ref(false);
const loginForm = reactive({
  username: '',
  password: '',
  remember: false,
});

// 注册表单
const registerFormRef = ref<FormInstance>();
const registerLoading = ref(false);
const registerForm = reactive({
  nickname: '',
  username: '',
  password: '',
  confirmPassword: '',
});

// 登录表单校验规则
const validateLoginUsername = (_rule: any, value: string, callback: Function) => {
  if (!value) {
    callback(new Error('请输入账号'));
  } else if (value.length < 3) {
    callback(new Error('账号长度至少3个字符'));
  } else {
    callback();
  }
};

const validateLoginPassword = (_rule: any, value: string, callback: Function) => {
  if (!value) {
    callback(new Error('请输入密码'));
  } else if (value.length < 6) {
    callback(new Error('密码长度至少6个字符'));
  } else {
    callback();
  }
};

const loginRules: FormRules = {
  username: [{ validator: validateLoginUsername, trigger: 'blur' }],
  password: [{ validator: validateLoginPassword, trigger: 'blur' }],
};

// 注册表单校验规则
const validateRegisterNickname = (_rule: any, value: string, callback: Function) => {
  if (!value) {
    callback(new Error('请输入昵称'));
  } else if (value.length < 2) {
    callback(new Error('昵称长度至少2个字符'));
  } else {
    callback();
  }
};

const validateRegisterUsername = (_rule: any, value: string, callback: Function) => {
  if (!value) {
    callback(new Error('请输入账号'));
  } else if (value.length < 3) {
    callback(new Error('账号长度至少3个字符'));
  } else if (!/^[a-zA-Z0-9_]+$/.test(value)) {
    callback(new Error('账号只能包含字母、数字和下划线'));
  } else {
    callback();
  }
};

const validateRegisterPassword = (_rule: any, value: string, callback: Function) => {
  if (!value) {
    callback(new Error('请输入密码'));
  } else if (value.length < 6) {
    callback(new Error('密码长度至少6个字符'));
  } else {
    callback();
  }
};

const validateConfirmPassword = (_rule: any, value: string, callback: Function) => {
  if (!value) {
    callback(new Error('请确认密码'));
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
};

const registerRules: FormRules = {
  nickname: [{ validator: validateRegisterNickname, trigger: 'blur' }],
  username: [{ validator: validateRegisterUsername, trigger: 'blur' }],
  password: [{ validator: validateRegisterPassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
};

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return;

  try {
    await loginFormRef.value.validate();
    loginLoading.value = true;

    try {
      await userStore.login(loginForm.username, loginForm.password);
      
      // 登录成功后获取完整用户信息（包括昵称）
      try {
        await userStore.fetchUserInfo();
      } catch {
        // 如果获取用户信息失败，不影响登录流程
      }
      
      ElMessage.success('登录成功');
      
      // 如果记住我，可以在这里保存账号信息（可选）
      if (loginForm.remember) {
        localStorage.setItem('af_remember_username', loginForm.username);
      } else {
        localStorage.removeItem('af_remember_username');
      }

      // 根据用户角色跳转到不同页面
      // 管理员跳转到管理后台，普通用户跳转到首页
      const isAdmin = userStore.isAdmin;
      const redirect = isAdmin 
        ? '/admin' 
        : ((route.query.redirect as string) || '/');
      router.push(redirect);
    } catch (error: any) {
      ElMessage.error(error.message || '登录失败，请检查账号和密码');
    } finally {
      loginLoading.value = false;
    }
  } catch {
    // 表单校验失败
  }
};

// 处理注册
const handleRegister = async () => {
  if (!registerFormRef.value) return;

  try {
    await registerFormRef.value.validate();
    registerLoading.value = true;

    try {
      await userStore.register(
        registerForm.username, 
        registerForm.password, 
        registerForm.nickname
      );

      ElMessage.success('注册成功，已自动登录');
      
      // 跳转到首页
      const redirect = (route.query.redirect as string) || '/';
      router.push(redirect);
    } catch (error: any) {
      ElMessage.error(error.message || '注册失败，请稍后重试');
    } finally {
      registerLoading.value = false;
    }
  } catch {
    // 表单校验失败
  }
};

// 处理游客体验
const handleGuestMode = () => {
  // 清除已登录状态（如果有）
  userStore.logout();
  ElMessage.info('欢迎以游客身份体验系统');
  router.push('/');
};

// 页面加载时，根据 query 参数设置 tab
if (route.query.tab === 'register') {
  activeTab.value = 'register';
}

// 页面加载时，如果有记住的账号，自动填充
const rememberedUsername = localStorage.getItem('af_remember_username');
if (rememberedUsername) {
  loginForm.username = rememberedUsername;
  loginForm.remember = true;
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    right: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
    animation: rotate 20s linear infinite;
  }

  @keyframes rotate {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
  }
}

.login-container {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 60px;
  max-width: 1200px;
  width: 100%;
  position: relative;
  z-index: 1;

  @media (max-width: 960px) {
    grid-template-columns: 1fr;
    gap: 40px;
  }
}

// 左侧品牌介绍区
.brand-section {
  display: flex;
  align-items: center;
  color: #fff;
}

.brand-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
}

.brand-logo {
  margin-bottom: 8px;
}

.logo-icon {
  font-size: 64px;
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.2));
}

.brand-title {
  font-size: 32px;
  font-weight: 800;
  line-height: 1.3;
  margin: 0;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  letter-spacing: -0.5px;
}

.brand-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin: 0;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.brand-slogan {
  margin-top: 8px;
}

.slogan-text {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 8px 0;
  opacity: 0.95;
}

.slogan-desc {
  font-size: 14px;
  line-height: 1.6;
  margin: 0;
  opacity: 0.85;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 8px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  opacity: 0.9;
  font-weight: 500;

  .el-icon {
    font-size: 20px;
  }
}

.brand-illustration {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.illustration-placeholder {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 16px;
  padding: 40px;
  text-align: center;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.2);
    transform: translateY(-2px);
  }
}

.illustration-icon {
  font-size: 64px;
  margin-bottom: 12px;
  opacity: 0.9;
}

.illustration-text {
  font-size: 14px;
  margin: 0;
  opacity: 0.8;
}

// 右侧表单区
.form-section {
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-card {
  width: 100%;
  max-width: 440px;
  border-radius: 20px;
  border: none;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;

  :deep(.el-card__body) {
    padding: 40px;
  }
}

.auth-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 32px;
  }

  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
  }

  :deep(.el-tabs__item) {
    font-size: 18px;
    font-weight: 600;
    padding: 0 24px;
    height: 48px;
    line-height: 48px;
  }

  :deep(.el-tabs__active-bar) {
    height: 3px;
  }
}

.auth-form {
  .el-form-item {
    margin-bottom: 20px;
  }

  :deep(.el-input__wrapper) {
    border-radius: 10px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
    }

    &.is-focus {
      box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
    }
  }
}

.form-options {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.submit-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
  }

  &:active {
    transform: translateY(0);
  }
}

.guest-section {
  margin-top: 32px;
  text-align: center;
}

.divider-text {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  padding: 0 12px;
  background: #fff;
}

.guest-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
}

.guest-tip {
  margin-top: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 0;
}

// 响应式设计
@media (max-width: 960px) {
  .login-container {
    max-width: 100%;
  }

  .brand-section {
    text-align: center;
  }

  .brand-title {
    font-size: 28px;
  }

  .form-card {
    max-width: 100%;
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 16px;
  }

  .form-card {
    :deep(.el-card__body) {
      padding: 24px;
    }
  }

  .brand-title {
    font-size: 24px;
  }

  .logo-icon {
    font-size: 48px;
  }
}
</style>
