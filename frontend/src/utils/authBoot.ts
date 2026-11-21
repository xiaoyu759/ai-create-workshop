import { reactive } from 'vue'

export type AuthBootPhase = 'idle' | 'glitch' | 'loading' | 'leaving'

/**
 * 登录成功后的终端转场状态。
 * 用模块级 reactive 而不是组件内状态：转场要在「页面已经被路由切走」之后继续存活，
 * 所以它必须挂在 App 层，且只有 Vite 的模块单例能保证唯一。
 */
export const authBootState = reactive({
  active: false,
  phase: 'idle' as AuthBootPhase,
  percent: 0,
})

let timers: number[] = []

function clearTimers() {
  timers.forEach((id) => window.clearTimeout(id))
  timers = []
}

/**
 * 登录校验通过后播放：故障闪烁 → 全黑 → 终端进度条 → 跳转 → 淡出。
 *
 * @param onDone     进度条走满后执行（真正的路由跳转放这里）
 * @param duration   进度条总时长（毫秒），默认 1000
 */
export function startAuthBoot(onDone: () => void, duration = 1000) {
  if (authBootState.active) {
    return
  }

  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (reduce) {
    onDone()
    return
  }

  clearTimers()
  authBootState.active = true
  authBootState.phase = 'glitch'
  authBootState.percent = 0
  document.documentElement.classList.add('auth-booting')

  // ① 故障闪烁 220ms：画面抖一下，遮罩还只是半透明
  timers.push(
    window.setTimeout(() => {
      authBootState.phase = 'loading'
      document.documentElement.classList.remove('auth-booting')
      document.documentElement.classList.add('auth-black')

      // ② 10 格进度，每格 duration/10
      const step = duration / 10
      for (let i = 1; i <= 10; i += 1) {
        timers.push(
          window.setTimeout(() => {
            authBootState.percent = i * 10
          }, step * i),
        )
      }

      // ③ 走满后停留 160ms 再跳转，避免「100% 与跳转同时发生」的突兀
      timers.push(
        window.setTimeout(() => {
          onDone()
          authBootState.phase = 'leaving'
          document.documentElement.classList.remove('auth-black')

          // ④ 黑幕淡出
          timers.push(
            window.setTimeout(() => {
              authBootState.active = false
              authBootState.phase = 'idle'
              authBootState.percent = 0
            }, 200),
          )
        }, duration + 160),
      )
    }, 220),
  )
}

/**
 * 供路由异常/组件卸载时兜底复位，避免遮罩卡死。
 */
export function resetAuthBoot() {
  clearTimers()
  document.documentElement.classList.remove('auth-booting', 'auth-black')
  authBootState.active = false
  authBootState.phase = 'idle'
  authBootState.percent = 0
}