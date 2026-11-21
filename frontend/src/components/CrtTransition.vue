<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { crtRequest } from '@/utils/crt'

/**
 * CRT 转场容器：一个组件管两个方向。
 *
 *  - boot（通电）：浅色登录页 → 主页。撕裂 + 压扁成白线，然后主页闪几下通电。
 *  - shutdown（关机）：任意深色页 → 登录页。整机闪烁抖动 → 上下压缩 → 白线 → 点。
 *
 * 为什么 teleport 到 body：
 * shutdown 时会把 #app 整体缩放（模拟显像管收束），如果这层遮罩在 #app 内部，
 * 它也会被一起压缩，白线就变成随页面缩小的一条短线；挂到 body 上才能保持满屏。
 */
const router = useRouter()

const active = ref(false)
const mode = ref<'boot' | 'shutdown'>('boot')
const fading = ref(false)
const slices = ref<Array<{ top: number; height: number; dx: number; dur: number; delay: number }>>([])

let timers: number[] = []
let stopHook: (() => void) | undefined

const prefersReduced = () =>
  typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches

const later = (fn: () => void, ms: number) => {
  timers.push(window.setTimeout(fn, ms))
}

const clearTimers = () => {
  timers.forEach((t) => window.clearTimeout(t))
  timers = []
}

/** 随机生成撕裂带：高度、位移、速度都随机，保证每次进主页不是同一段动画 */
const buildSlices = () => {
  const out: Array<{ top: number; height: number; dx: number; dur: number; delay: number }> = []
  let y = 0
  while (y < 100) {
    const height = 3 + Math.random() * 11
    out.push({
      top: y,
      height,
      dx: (Math.random() - 0.5) * 16,
      dur: 90 + Math.random() * 110,
      delay: Math.random() * 60,
    })
    y += height + 2 + Math.random() * 5
  }
  return out
}

/** 通电：浅色画面撕裂后收成白线，0.86s 通知主页开始闪烁 */
const playBoot = () => {
  if (prefersReduced()) return
  clearTimers()
  slices.value = buildSlices()
  mode.value = 'boot'
  active.value = true
  later(() => window.dispatchEvent(new CustomEvent('cp:power-on')), 860)
  later(() => {
    active.value = false
  }, 1150)
}

/**
 * 关机时间轴（总计约 1.0s，跳转发生在 0.64s——此时画面已经全黑，用户感知不到切换）：
 *   0.00 ~ 0.62s  整机闪烁抖动 + 上下压缩
 *   0.30 ~ 0.62s  黑幕升起（避免压缩时露出页面底下的 body 底色）
 *   0.52 ~ 0.82s  白线由细变宽再收成一个点
 *   0.64s         撤掉压缩类 + 真正跳转（被黑幕完全遮住）
 *   0.82 ~ 1.00s  黑幕淡出，露出目标页
 */
const playShutdown = (onDone: () => void) => {
  if (prefersReduced()) {
    onDone()
    return
  }
  clearTimers()
  mode.value = 'shutdown'
  active.value = true
  fading.value = false
  document.documentElement.classList.add('crt-off')

  later(() => {
    document.documentElement.classList.remove('crt-off')
    onDone()
  }, 640)

  later(() => {
    fading.value = true
  }, 820)

  later(() => {
    active.value = false
    fading.value = false
  }, 1020)
}

watch(crtRequest, (req) => {
  if (!req) return
  const { onDone } = req
  crtRequest.value = null
  playShutdown(onDone)
})

onMounted(() => {
  stopHook = router.afterEach((to, from) => {
    if (to.path === '/' && from.path.startsWith('/user')) playBoot()
  })
})

onBeforeUnmount(() => {
  stopHook?.()
  clearTimers()
  document.documentElement.classList.remove('crt-off')
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="active"
      class="cp-crt"
      :class="[`cp-crt--${mode}`, { 'is-fading': fading }]"
      aria-hidden="true"
    >
      <template v-if="mode === 'boot'">
        <span
          v-for="(s, i) in slices"
          :key="i"
          class="cp-crt__slice"
          :style="{
            top: `${s.top}%`,
            height: `${s.height}%`,
            '--dx': `${s.dx}vw`,
            animationDuration: `${s.dur}ms`,
            animationDelay: `${s.delay}ms`,
          }"
        ></span>
        <span class="cp-crt__line"></span>
      </template>

      <template v-else>
        <span class="cp-crt__veil"></span>
        <span class="cp-crt__offline"></span>
      </template>
    </div>
  </Teleport>
</template>

<style>
.cp-crt {
  position: fixed;
  inset: 0;
  z-index: 9999;
  overflow: hidden;
  pointer-events: none;
  transform-origin: 50% 50%;
}

.cp-crt.is-fading {
  animation: none;
  opacity: 0;
  transition: opacity 180ms linear;
}

/* ── 通电（登录页 → 主页）───────────────────────────── */
.cp-crt--boot {
  background: #f4f3ef;
  animation: cpCrtShutdown 1.15s cubic-bezier(0.4, 0, 0.9, 0.25) forwards;
}

.cp-crt__slice {
  position: absolute;
  left: -12%;
  width: 124%;
  display: block;
  background: #0f1114;
  opacity: 0.9;
  animation-name: cpCrtSlice;
  animation-timing-function: steps(3, end);
  animation-iteration-count: infinite;
  animation-fill-mode: both;
}

.cp-crt__slice:nth-child(3n) {
  background: #1b1f26;
  opacity: 0.75;
}

.cp-crt__slice:nth-child(3n + 1) {
  background: #06070a;
}

@keyframes cpCrtSlice {
  0% {
    transform: translateX(0) scaleX(1);
  }
  40% {
    transform: translateX(var(--dx)) scaleX(1.04);
  }
  70% {
    transform: translateX(calc(var(--dx) * -0.7)) scaleX(0.98);
  }
  100% {
    transform: translateX(0) scaleX(1);
  }
}

@keyframes cpCrtShutdown {
  0% {
    transform: scale(1, 1);
    filter: brightness(1);
    opacity: 1;
  }
  26% {
    transform: scale(1, 1) skewX(-1.2deg);
    filter: brightness(1.18);
  }
  30% {
    transform: scale(1, 1) skewX(0.8deg);
  }
  34% {
    transform: scale(1, 1);
    filter: brightness(1.05);
  }
  54% {
    transform: scale(1.02, 0.52);
    filter: brightness(1.9);
  }
  70% {
    transform: scale(1.04, 0.075);
    filter: brightness(3.6);
  }
  80% {
    transform: scale(0.55, 0.004);
    filter: brightness(7);
    opacity: 1;
  }
  84% {
    opacity: 0.85;
  }
  100% {
    transform: scale(0.1, 0.002);
    opacity: 0;
    filter: brightness(9);
  }
}

.cp-crt__line {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 100%;
  height: 2px;
  display: block;
  background: #ffffff;
  opacity: 0;
  transform: translate(-50%, -50%) scaleX(0);
  box-shadow:
    0 0 26px 8px rgba(255, 255, 255, 0.85),
    0 0 80px 24px rgba(180, 230, 255, 0.45);
  animation: cpCrtLine 1.15s ease-out forwards;
}

@keyframes cpCrtLine {
  0%,
  62% {
    opacity: 0;
    transform: translate(-50%, -50%) scaleX(0.2);
  }
  70% {
    opacity: 1;
    transform: translate(-50%, -50%) scaleX(1);
  }
  84% {
    opacity: 0.9;
    transform: translate(-50%, -50%) scaleX(0.9);
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) scaleX(0);
  }
}

/* ── 关机（主页/任意页 → 登录页）────────────────────── */

/* 黑幕：晚于压缩起步才升起，这样用户先看到"画面在压缩"，而不是"突然黑屏" */
.cp-crt__veil {
  position: absolute;
  inset: 0;
  display: block;
  background: #000;
  opacity: 0;
  animation: cpCrtVeil 620ms linear forwards;
}

@keyframes cpCrtVeil {
  0%,
  46% {
    opacity: 0;
  }
  100% {
    opacity: 1;
  }
}

/* 白线：先横向拉满，再纵向收束成一个点 */
.cp-crt__offline {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 100%;
  height: 2px;
  display: block;
  background: #fff;
  opacity: 0;
  transform: translate(-50%, -50%) scaleX(0);
  box-shadow:
    0 0 24px 6px rgba(255, 255, 255, 0.9),
    0 0 90px 26px rgba(150, 220, 255, 0.45);
  animation: cpCrtOffline 820ms ease-out forwards;
}

@keyframes cpCrtOffline {
  0%,
  58% {
    opacity: 0;
    transform: translate(-50%, -50%) scaleX(0);
  }
  66% {
    opacity: 1;
    transform: translate(-50%, -50%) scaleX(1);
  }
  80% {
    opacity: 1;
    transform: translate(-50%, -50%) scaleX(0.86);
  }
  90% {
    opacity: 1;
    transform: translate(-50%, -50%) scaleX(0.02);
  }
  96% {
    opacity: 0.9;
    transform: translate(-50%, -50%) scaleX(0.01) scaleY(1.6);
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.001);
  }
}

/* 整机（含页头）一起压缩：挂在 #app 上而不是页面块上 */
html.crt-off {
  overflow: hidden;
}

html.crt-off #app {
  transform-origin: 50% 42%;
  animation: cpCrtPageOff 640ms cubic-bezier(0.45, 0, 0.9, 0.35) forwards;
}

@keyframes cpCrtPageOff {
  0% {
    transform: none;
    filter: brightness(1);
    opacity: 1;
  }
  6% {
    transform: translateX(-3px);
    filter: brightness(2.6);
    opacity: 0.55;
  }
  9% {
    opacity: 1;
    filter: brightness(0.45);
  }
  13% {
    transform: translateX(3px);
    filter: brightness(2);
  }
  17% {
    transform: translateX(-2px);
    filter: brightness(1.1);
  }
  22% {
    transform: translateX(2px) scale(1, 0.995);
    filter: brightness(1.5);
  }
  28% {
    transform: none;
    filter: brightness(1);
  }
  58% {
    transform: scale(1.015, 0.4);
    filter: brightness(2);
  }
  78% {
    transform: scale(1.03, 0.05);
    filter: brightness(4.2);
  }
  92% {
    transform: scale(0.88, 0.006);
    filter: brightness(6.5);
  }
  100% {
    transform: scale(0.2, 0.002);
    filter: brightness(8);
    opacity: 0.9;
  }
}
</style>