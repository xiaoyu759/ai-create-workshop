import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
// 赛博朋克字体（本地打包，不依赖外部 CDN）
import '@fontsource/orbitron/latin-500.css'
import '@fontsource/orbitron/latin-700.css'
import '@fontsource/jetbrains-mono/latin-400.css'
import '@fontsource/jetbrains-mono/latin-500.css'
import '@fontsource/jetbrains-mono/latin-700.css'
import '@/styles/tokens.css'
import '@/styles/cyberpunk.css'

import '@/access'

import { vAutoGrow } from '@/directives/autoGrow'

// 关闭浏览器的滚动位置恢复：否则刷新时页面会先停在旧的滚动位置再跳回顶部
if ('scrollRestoration' in history) {
  history.scrollRestoration = 'manual'
}

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd)
app.directive('auto-grow', vAutoGrow)

app.mount('#app')
