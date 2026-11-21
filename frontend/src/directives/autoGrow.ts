import type { Directive } from 'vue'

type AutoGrowOptions = {
  minRows?: number
  maxRows?: number
}

/**
 * 支持 CSS `field-sizing: content` 的浏览器（Chromium 123+）走纯 CSS：
 * 高度由布局引擎自己算，每敲一个字都不会触发 JS 读写样式，零开销。
 */
const supportsFieldSizing =
  typeof CSS !== 'undefined' && typeof CSS.supports === 'function' && CSS.supports('field-sizing', 'content')

function resolveTextarea(el: HTMLElement): HTMLTextAreaElement | null {
  if (el instanceof HTMLTextAreaElement) return el
  return el.querySelector('textarea')
}

function metrics(el: HTMLTextAreaElement) {
  const style = getComputedStyle(el)
  const lineHeight = parseFloat(style.lineHeight) || 22
  const chrome =
    parseFloat(style.paddingTop || '0') +
    parseFloat(style.paddingBottom || '0') +
    parseFloat(style.borderTopWidth || '0') +
    parseFloat(style.borderBottomWidth || '0')
  return { lineHeight, chrome }
}

/**
 * 用内联 min/max-height 兜住高度区间。
 * 不写在 CSS 里：组件库运行时注入的 .ant-input 规则会盖掉同优先级声明。
 */
function applyOptions(el: HTMLTextAreaElement, options: AutoGrowOptions) {
  const { lineHeight, chrome } = metrics(el)
  const minRows = options.minRows ?? 2
  const maxRows = options.maxRows ?? 6
  el.style.minHeight = `${Math.round(lineHeight * minRows + chrome)}px`
  el.style.maxHeight = `${Math.round(lineHeight * maxRows + chrome)}px`
}

/**
 * 兜底实现：`scrollHeight` 量一次、写一次高度。
 * 只在浏览器不支持 field-sizing 时启用。
 */
function resize(el: HTMLTextAreaElement) {
  if (supportsFieldSizing) return
  const min = parseFloat(el.style.minHeight) || 0
  const max = parseFloat(el.style.maxHeight) || Number.MAX_SAFE_INTEGER
  el.style.height = 'auto'
  el.style.height = `${Math.min(Math.max(el.scrollHeight, min), max)}px`
}

/**
 * 多行输入框自动增高（聊天框行为）。
 *
 * 用法：`v-auto-grow="{ minRows: 2, maxRows: 6 }"`
 * 相比 antd 的 `auto-size`：不再每次都复制节点、逐条拷贝计算样式，
 * 输入时不会有明显的掉帧感。
 */
export const vAutoGrow: Directive<HTMLElement, AutoGrowOptions> = {
  mounted(el, binding) {
    const target = resolveTextarea(el)
    if (!target) return
    target.classList.add('cp-autogrow')
    if (!supportsFieldSizing) {
      const onInput = () => resize(target)
      target.addEventListener('input', onInput)
      ;(target as unknown as { __cpAutoGrow?: () => void }).__cpAutoGrow = onInput
    }
    // 等一帧：组件库的样式是运行时注入的，挂载当帧拿到的行高/内边距还不是最终值
    requestAnimationFrame(() => {
      applyOptions(target, binding.value ?? {})
      resize(target)
    })
  },
  updated(el, binding) {
    const target = resolveTextarea(el)
    if (!target) return
    applyOptions(target, binding.value ?? {})
    // 外部改动（发送后清空、回填历史）也要跟着收放
    resize(target)
  },
  unmounted(el) {
    const target = resolveTextarea(el)
    const onInput = (target as unknown as { __cpAutoGrow?: () => void } | null)?.__cpAutoGrow
    if (target && onInput) target.removeEventListener('input', onInput)
  },
}