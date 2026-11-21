<template>
  <div class="au-shell">
    <div class="au-frame" aria-hidden="true">
      <span class="au-corner au-corner--tl"></span>
      <span class="au-corner au-corner--tr"></span>
      <span class="au-corner au-corner--bl"></span>
      <span class="au-corner au-corner--br"></span>
    </div>

    <header class="au-head">
      <div class="au-brand">
        <h1 class="au-brand__name">AI 应用创意工坊</h1>
        <p class="au-brand__en">AI CREATIVE WORKSHOP</p>
        <p class="au-brand__os">CODE GENERATION OS</p>
      </div>

      <div class="au-meta">
        <RouterLink class="au-skip" to="/">返回首页 ▸</RouterLink>
        <div class="au-meta__row">
          <span>REV 0.1.0</span>
          <span>·</span>
          <span>WEB PROFILE</span>
          <span class="au-meta__clock">{{ clock }}</span>
        </div>
        <div class="au-bars" aria-hidden="true">
          <i></i><i></i><i></i><i></i>
        </div>
      </div>
    </header>

    <main class="au-main">
      <RouterView />
    </main>

    <div class="au-progress" aria-hidden="true">
      <span class="au-progress__fill" :style="{ width: step === '01' ? '50%' : '100%' }"></span>
    </div>

    <footer class="au-foot">
      <span class="au-foot__idx">[ {{ step }} / 02 ] {{ stepLabel }}</span>
      <span class="au-foot__status">OPERATOR TERMINAL · SECURE CHANNEL</span>
      <span class="au-foot__powered">
        POWERED BY AI CREATIVE WORKSHOP
        <i></i>
      </span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import '@/styles/auth.css'

const route = useRoute()

const clock = ref('')
let timer: number | undefined

const tick = () => {
  const now = new Date()
  clock.value = [now.getHours(), now.getMinutes(), now.getSeconds()]
    .map((n) => String(n).padStart(2, '0'))
    .join(':')
}

const step = computed(() => (route.path.includes('register') ? '02' : '01'))
const stepLabel = computed(() => (route.path.includes('register') ? 'ENROLLMENT' : 'IDENTITY'))

onMounted(() => {
  // 亮色终端期间压掉深色主题的底色与网格层
  document.documentElement.classList.add('au-mode')
  tick()
  timer = window.setInterval(tick, 1000)
})

onBeforeUnmount(() => {
  document.documentElement.classList.remove('au-mode')
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.au-skip {
  font-family: 'JetBrains Mono', 'Cascadia Code', ui-monospace, monospace;
  font-size: 10px;
  letter-spacing: 0.18em;
  color: #55554f;
  text-decoration: none;
  border-bottom: 1px solid transparent;
}

.au-skip:hover,
.au-skip:focus-visible {
  color: #ff5c00;
  border-bottom-color: #ff5c00;
  outline: none;
}
</style>