<script setup lang="ts">
import { App } from 'ant-design-vue'
import { registerFeedback } from '@/utils/feedback'

// 必须渲染在 <a-app> 内部，才能拿到读取了 ConfigProvider 主题的实例
registerFeedback(App.useApp())
</script>

<template>
  <span style="display: none" />
</template>