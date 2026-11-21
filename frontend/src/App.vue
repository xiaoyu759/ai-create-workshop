<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import FeedbackBinder from '@/components/FeedbackBinder.vue'
import CrtTransition from '@/components/CrtTransition.vue'
import AuthBootOverlay from '@/components/AuthBootOverlay.vue'
import { antdTheme } from '@/theme/antdTheme'
</script>

<template>
  <a-config-provider :theme="antdTheme">
    <a-app>
      <FeedbackBinder />
      <BasicLayout />
      <CrtTransition />
      <AuthBootOverlay />
    </a-app>
  </a-config-provider>
</template>

<style>
* {
  box-sizing: border-box;
}
</style>