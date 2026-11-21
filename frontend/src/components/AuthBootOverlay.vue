<template>
  <Teleport to="body">
    <div
      v-if="state.active"
      class="cp-aboot"
      :class="`cp-aboot--${state.phase}`"
      role="status"
      aria-live="polite"
    >
      <div class="cp-aboot__tear" aria-hidden="true"></div>

      <div v-if="state.phase !== 'glitch'" class="cp-aboot__term">
        <p class="cp-aboot__line">
          <span class="cp-aboot__dim">[</span><span class="cp-aboot__bar">{{ bar }}</span
          ><span class="cp-aboot__dim">]</span><span class="cp-aboot__pct">{{ percentText }}%</span
          ><span class="cp-aboot__label">AUTHENTICATING...</span>
        </p>
        <p class="cp-aboot__sub">
          <span class="cp-aboot__dot"></span>IDENTITY VERIFIED · OPENING SESSION
        </p>
        <p class="cp-aboot__caret">&gt;_</p>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { authBootState as state } from '@/utils/authBoot'

const FILLED = 10

const filled = computed(() => Math.round((state.percent / 100) * FILLED))

// 终端字符进度条：已完成为 |，未完成为 .
const bar = computed(() => '|'.repeat(filled.value) + '.'.repeat(FILLED - filled.value))

const percentText = computed(() => String(state.percent))
</script>

<style>
/* 页面故障：登录页在 220ms 内抖三下、错色、横向撕裂 */
html.auth-booting #app {
  animation: cpAuthGlitch 220ms steps(2, end) both;
}

html.auth-black,
html.auth-black body {
  background: #000;
}

@keyframes cpAuthGlitch {
  0% {
    transform: none;
    filter: none;
  }
  22% {
    transform: translate3d(-9px, 0, 0) skewX(-2.4deg);
    filter: brightness(1.5) hue-rotate(-18deg);
  }
  44% {
    transform: translate3d(7px, -2px, 0) skewX(1.8deg);
    filter: brightness(0.6) invert(0.06);
  }
  68% {
    transform: translate3d(-4px, 1px, 0);
    filter: brightness(1.2) contrast(1.4);
  }
  100% {
    transform: none;
    filter: none;
  }
}
</style>

<style scoped>
.cp-aboot {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: grid;
  place-items: center;
  background: #000;
  color: #ff5c20;
  font-family: 'JetBrains Mono', 'Cascadia Code', ui-monospace, monospace;
}

.cp-aboot--glitch {
  background: rgba(0, 0, 0, 0.06);
}

.cp-aboot--leaving {
  opacity: 0;
  transition: opacity 180ms linear;
}

/* 故障阶段的横向撕裂带，只在 glitch 时出现 */
.cp-aboot__tear {
  position: absolute;
  inset: 0;
  opacity: 0;
  pointer-events: none;
  background-image:
    linear-gradient(transparent 18%, rgba(255, 92, 32, 0.55) 18%, rgba(255, 92, 32, 0.55) 19.4%, transparent 19.4%),
    linear-gradient(transparent 52%, rgba(255, 255, 255, 0.5) 52%, rgba(255, 255, 255, 0.5) 52.8%, transparent 52.8%),
    linear-gradient(transparent 78%, rgba(255, 92, 32, 0.35) 78%, rgba(255, 92, 32, 0.35) 79.6%, transparent 79.6%);
}

.cp-aboot--glitch .cp-aboot__tear {
  opacity: 1;
  animation: cpAbootTear 220ms steps(3, end) both;
}

@keyframes cpAbootTear {
  0% {
    transform: translate3d(0, 0, 0);
  }
  35% {
    transform: translate3d(-14px, 3px, 0);
  }
  70% {
    transform: translate3d(11px, -3px, 0);
  }
  100% {
    transform: translate3d(4px, 0, 0);
    opacity: 0;
  }
}

.cp-aboot__term {
  display: flex;
  flex-direction: column;
  gap: 14px;
  font-size: 13px;
  letter-spacing: 0.16em;
  text-align: left;
}

.cp-aboot__line {
  margin: 0;
  display: flex;
  align-items: baseline;
  gap: 0;
  white-space: normal;
  font-variant-ligatures: none;
  font-variant-numeric: tabular-nums;
}

.cp-aboot__dim {
  color: #7a3d22;
}

.cp-aboot__bar {
  color: #ff5c20;
  letter-spacing: 0.06em;
}

.cp-aboot__pct {
  display: inline-block;
  min-width: 4ch;
  margin-left: 12px;
  text-align: right;
  color: #ffd7c4;
  font-weight: 700;
}

.cp-aboot__label {
  margin-left: 12px;
  color: #a3552f;
}

.cp-aboot__sub {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 10px;
  letter-spacing: 0.24em;
  color: #6f5a4f;
}

.cp-aboot__dot {
  width: 5px;
  height: 5px;
  background: #ff5c20;
  animation: cpAbootBlink 900ms steps(1, end) infinite;
}

.cp-aboot__caret {
  margin: 0;
  font-size: 12px;
  color: #ff5c20;
  animation: cpAbootBlink 620ms steps(1, end) infinite;
}

@keyframes cpAbootBlink {
  0%,
  49% {
    opacity: 1;
  }
  50%,
  100% {
    opacity: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  html.auth-booting #app,
  .cp-aboot__tear,
  .cp-aboot__dot,
  .cp-aboot__caret {
    animation: none !important;
  }
}
</style>