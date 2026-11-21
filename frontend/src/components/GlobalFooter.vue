<template>
  <a-layout-footer class="footer">
    <div class="statusbar" role="contentinfo" aria-label="站点状态栏">
      <span class="statusbar__seg">
        <i class="statusbar__dot" aria-hidden="true"></i>
        OPERATOR TERMINAL
      </span>
      <span class="statusbar__sep" aria-hidden="true">|</span>
      <span class="statusbar__seg">AI CREATIVE WORKSHOP</span>
      <span class="statusbar__sep" aria-hidden="true">|</span>
      <span class="statusbar__seg statusbar__seg--dim">POWERED BY CODE GENERATION OS</span>
      <i class="statusbar__caret" aria-hidden="true">▌</i>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
// 纯展示的状态栏，无数据依赖
</script>

<style scoped>
.footer {
  flex: 0 0 auto;
  height: 34px;
  padding: 0 16px;
  margin-top: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #08090b;
  border-top: 1px solid rgba(255, 92, 32, 0.14);
}

.statusbar {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-family: var(--cp-font-mono);
  font-size: 10px;
  line-height: 1;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #4f5a66;
  white-space: nowrap;
  animation: cpStatusBreath 5.6s ease-in-out infinite;
}

.statusbar__seg {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.statusbar__seg--dim {
  color: #3c454f;
}

.statusbar__dot {
  width: 4px;
  height: 4px;
  background: var(--cp-accent);
  box-shadow: 0 0 6px rgba(255, 92, 32, 0.85);
}

.statusbar__sep {
  color: #262b31;
  letter-spacing: 0;
  animation: cpStatusBlink 3.4s steps(1, end) infinite;
}

.statusbar__caret {
  color: var(--cp-accent);
  font-style: normal;
  opacity: 0.75;
  animation: cpStatusBlink 1.1s steps(1, end) infinite;
}

@keyframes cpStatusBreath {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.82;
  }
}

@keyframes cpStatusBlink {
  0%,
  62% {
    opacity: 1;
  }
  63%,
  100% {
    opacity: 0.15;
  }
}

@media (max-width: 640px) {
  .statusbar {
    gap: 6px;
    font-size: 9px;
    letter-spacing: 0.1em;
  }

  .statusbar__seg--dim {
    display: none;
  }

  .statusbar__seg--dim + .statusbar__caret,
  .statusbar__sep:nth-of-type(2) {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .statusbar,
  .statusbar__sep,
  .statusbar__caret {
    animation: none;
  }
}
</style>