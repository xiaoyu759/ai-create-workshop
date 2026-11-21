<template>
  <a-layout class="basic-layout">
    <!-- 键盘用户可直接跳过导航 -->
    <a class="cp-skip-link" href="#main">跳到主要内容</a>
    <!-- 顶部导航栏 -->
    <GlobalHeader />
    <!-- 主要内容区域 -->
    <a-layout-content id="main" tabindex="-1" class="main-content">
      <router-view v-slot="{ Component, route }">
        <transition name="cp-route" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </a-layout-content>
    <!-- 底部版权信息 -->
    <GlobalFooter />
  </a-layout>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: none;
}

.main-content {
  flex: 1 0 auto;
  display: flex;
  flex-direction: column;
  width: 100%;
  padding: 0;
  background: none;
  margin: 0;
}
</style>
