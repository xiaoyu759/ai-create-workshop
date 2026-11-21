import { ref } from 'vue'

/**
 * CRT 关机转场的「请求总线」。
 *
 * 为什么不用事件（EventTarget / mitt）：
 * 关机转场需要在动画结束后**再**执行跳转，调用方得拿到一个「完成时机」，
 * 用回调比用事件更直接，也不会出现「监听器没挂上就触发」的竞态。
 *
 * 真正的动效与计时全部在 `components/CrtTransition.vue` 里，
 * 这里只做状态与回调的传递，避免把 800ms 的时间轴散落在各个业务组件里。
 */
export interface CrtShutdownRequest {
  onDone: () => void
}

/** 非 null 表示有一次关机转场待播放，由 CrtTransition 消费后置回 null */
export const crtRequest = ref<CrtShutdownRequest | null>(null)

/**
 * 播放一次「CRT 关机」转场，画面收成一个点之后执行 onDone（通常是路由跳转）。
 * 若用户开启了「减少动态效果」，会立即执行 onDone，不阻塞操作。
 */
export function requestCrtShutdown(onDone: () => void) {
  crtRequest.value = { onDone }
}