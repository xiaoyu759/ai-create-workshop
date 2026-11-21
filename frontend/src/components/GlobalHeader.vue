<script setup lang="ts">
import { computed, h, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HomeOutlined } from '@ant-design/icons-vue'
import { App, type MenuProps } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { userLogout } from '@/api/userController'
import { requestCrtShutdown } from '@/utils/crt'

const { message } = App.useApp()
const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

const selectedKeys = ref<string[]>(['/'])

watch(
  () => route.path,
  (path) => {
    selectedKeys.value = [path]
  },
  { immediate: true },
)

const menuItems = computed<MenuProps['items']>(() => {
  const items: MenuProps['items'] = [
    {
      key: '/',
      icon: () => h(HomeOutlined),
      label: '主页',
      title: '主页',
    },
  ]
  if (loginUserStore.loginUser.userRole === 'admin') {
    items.push({
      key: '/admin/userManage',
      label: '用户管理',
      title: '用户管理',
    })
    items.push({
      key: '/admin/appManage',
      label: '应用管理',
      title: '应用管理',
    })
  }
  return items
})

const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
  if (key !== route.path) {
    router.push(key as string)
  } else {
    message.info('您已在这个页面')
  }
}

const isAuthed = computed(() => Boolean(loginUserStore.loginUser.id))

/**
 * 去登录页也走 CRT 关机：从深色机位切到浅色「身份核验终端」，
 * 直接 router.push 会看到一帧深色页面变浅色，很生硬。
 */
const goLogin = () => {
  requestCrtShutdown(() => router.push('/user/login'))
}

const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    requestCrtShutdown(() => {
      router.push('/user/login')
      message.success('退出登录成功')
    })
  } else {
    message.error('操作失败，' + res.data.message)
  }
}
</script>

<template>
  <a-layout-header class="header">
    <RouterLink to="/" class="brand">
      <span class="brand-mark" aria-hidden="true">AC</span>
      <span class="brand-text">
        <span class="brand-name">AI 应用创意工坊</span>
        <span class="brand-code">SYS://TERMINAL · v0.1.0</span>
      </span>
    </RouterLink>

    <div class="hud-nav">
      <a-menu
        v-model:selectedKeys="selectedKeys"
        mode="horizontal"
        :items="menuItems"
        @click="handleMenuClick"
      />
    </div>

    <div class="hud-right">
      <span class="hud-status" :class="{ 'is-authed': isAuthed }">
        <i class="hud-status__dot" aria-hidden="true"></i>
        <span class="hud-status__label">{{ isAuthed ? 'AUTHED' : 'GUEST' }}</span>
      </span>

      <template v-if="isAuthed">
        <RouterLink to="/" class="hud-user">
          <img
            v-if="loginUserStore.loginUser.userAvatar"
            :src="loginUserStore.loginUser.userAvatar"
            alt=""
            class="hud-user__avatar"
          />
          <span v-else class="hud-user__avatar hud-user__avatar--blank" aria-hidden="true">?</span>
          <span class="hud-user__name">{{ loginUserStore.loginUser.userName ?? '无名' }}</span>
        </RouterLink>
        <button class="hud-auth" type="button" @click="doLogout">[ LOG OUT ]</button>
      </template>

      <button v-else class="hud-auth" type="button" @click="goLogin">[ LOG IN ]</button>
    </div>
  </a-layout-header>
</template>

<style scoped>
.header {
  position: relative;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 26px;
  height: 64px;
  padding: 0 20px;
  background: #0d0f13;
  border-bottom: 1px solid rgba(255, 92, 32, 0.16);
}

/* 细线之上再压一层极弱的冷光，让页头与内容之间有一道"设备分缝" */
.header::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 1px;
  pointer-events: none;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 92, 32, 0.35) 18%,
    rgba(255, 92, 32, 0.12) 62%,
    transparent
  );
}

/* ── 品牌：机械切角铭牌 ─────────────────────────── */
.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  text-decoration: none;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  /* 同上：不重置 line-height 的话，字母会被 64px 行高挤出这块 30px 的铭牌 */
  line-height: 1;
  font-family: var(--cp-font-display, 'Orbitron', sans-serif);
  font-size: 10px;
  letter-spacing: 0.04em;
  font-weight: 700;
  color: #ffe3d2;
  background: linear-gradient(180deg, #1b222a 0%, #0e1318 100%);
  clip-path: polygon(
    0 0,
    100% 0,
    100% calc(100% - 9px),
    calc(100% - 9px) 100%,
    0 100%
  );
  box-shadow:
    inset 0 0 0 1px rgba(255, 92, 32, 0.45),
    inset 0 0 16px rgba(255, 92, 32, 0.16);
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-name {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.3em;
  color: #e6edf3;
  line-height: 1.1;
}

.brand-code {
  font-family: var(--cp-font-mono, 'JetBrains Mono', monospace);
  font-size: 8px;
  letter-spacing: 0.22em;
  color: #4d5765;
  line-height: 1.1;
}

/* ── 导航 ─────────────────────────────────────── */
.hud-nav {
  flex: 1;
  min-width: 0;
}

.header :deep(.ant-menu-horizontal) {
  background: transparent;
  border-bottom: none;
  line-height: 62px;
}

.header :deep(.ant-menu-item) {
  border-radius: 0;
  font-family: var(--cp-font-mono, monospace);
  font-size: 11px;
  letter-spacing: 0.22em;
  color: #7b8593;
}

.header :deep(.ant-menu-item:hover) {
  color: #d7e4ea;
}

.header :deep(.ant-menu-item-selected) {
  color: var(--cp-cyan);
}

.header :deep(.ant-menu-item-selected::after) {
  border-bottom: 2px solid var(--cp-cyan);
}

/* ── 右侧：状态点 + 身份 ────────────────────────── */
.hud-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.hud-status {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-family: var(--cp-font-mono, monospace);
  font-size: 9px;
  letter-spacing: 0.24em;
  color: #5f6b79;
}

.hud-status__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #35ff6a;
  box-shadow:
    0 0 8px rgba(53, 255, 106, 0.9),
    0 0 2px rgba(53, 255, 106, 1);
  animation: hudDot 2.6s steps(1, end) infinite;
}

.hud-status.is-authed .hud-status__label {
  color: #7fd79a;
}

@keyframes hudDot {
  0%,
  72% {
    opacity: 1;
  }
  73%,
  79% {
    opacity: 0.15;
  }
  80%,
  100% {
    opacity: 1;
  }
}

.hud-user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 30px;
  padding: 0 12px 0 5px;
  line-height: 1;
  text-decoration: none;
  background: #14181d;
  border: 1px solid #262d35;
  clip-path: polygon(
    0 0,
    100% 0,
    100% calc(100% - 9px),
    calc(100% - 9px) 100%,
    0 100%
  );
}

.hud-user:hover {
  border-color: rgba(255, 92, 32, 0.4);
}

.hud-user__avatar {
  width: 22px;
  height: 22px;
  object-fit: cover;
  border-radius: 0;
  background: #0b0e12;
}

.hud-user__avatar--blank {
  display: grid;
  place-items: center;
  font-family: var(--cp-font-mono, monospace);
  font-size: 10px;
  color: #5b6674;
}

.hud-user__name {
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: var(--cp-font-mono, monospace);
  font-size: 10px;
  letter-spacing: 0.14em;
  color: #b9c3ce;
}

/* 终端文字按钮：暗红 1px 线框，无圆角，切右下角 */
.hud-auth {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 30px;
  padding: 0 14px;
  /* antd 的 .ant-layout-header 把 line-height 设成 64px，不重置会把 10px 文字顶出按钮外 */
  line-height: 1;
  font-family: var(--cp-font-mono, monospace);
  font-size: 10px;
  letter-spacing: 0.18em;
  color: #ff6b78;
  background: transparent;
  border: 1px solid rgba(255, 45, 63, 0.55);
  clip-path: polygon(
    0 0,
    100% 0,
    100% calc(100% - 8px),
    calc(100% - 8px) 100%,
    0 100%
  );
  cursor: pointer;
  transition: none;
}

.hud-auth:hover {
  color: #ffe1e4;
  border-color: rgba(255, 45, 63, 0.9);
  background: rgba(255, 45, 63, 0.09);
  box-shadow: inset 0 0 18px rgba(255, 45, 63, 0.18);
}

.hud-auth:active {
  background: rgba(255, 45, 63, 0.2);
}

.hud-auth:focus-visible {
  outline: 2px solid rgba(255, 108, 120, 0.9);
  outline-offset: 2px;
}

@media (max-width: 900px) {
  .header {
    gap: 14px;
    padding: 0 12px;
  }

  .brand-code,
  .hud-status__label {
    display: none;
  }
}

@media (max-width: 640px) {
  .brand-name {
    letter-spacing: 0.16em;
    font-size: 11px;
  }

  .hud-user__name {
    display: none;
  }
}
</style>