<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { logout } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

async function handleLogout() {
  try {
    await logout()
  } catch {
    // 即使后端请求失败也清除前端状态
  }
  authStore.clearAuth()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<template>
  <el-container style="height: 100vh">
    <el-aside width="200px">
      <div class="logo">通知管理后台</div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/publishers">
          <el-icon><Promotion /></el-icon>
          <span>发布方管理</span>
        </el-menu-item>
        <el-menu-item index="/event-types">
          <el-icon><Collection /></el-icon>
          <span>事件类型管理</span>
        </el-menu-item>
        <el-menu-item index="/subscriptions">
          <el-icon><Connection /></el-icon>
          <span>订阅关系管理</span>
        </el-menu-item>
        <el-menu-item index="/suppliers">
          <el-icon><Setting /></el-icon>
          <span>订阅方管理</span>
        </el-menu-item>
        <el-menu-item index="/dlq">
          <el-icon><Warning /></el-icon>
          <span>死信队列</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <span>{{ authStore.username }}</span>
        <el-button type="text" @click="handleLogout">退出</el-button>
      </el-header>
      <el-main>
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  background-color: #263445;
}

.el-aside {
  background-color: #304156;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  border-bottom: 1px solid #e6e6e6;
}
</style>
