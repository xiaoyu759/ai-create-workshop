import type { MessageInstance } from 'ant-design-vue/es/message/interface'
import type { ModalStaticFunctions } from 'ant-design-vue/es/modal/confirm'
import type { NotificationInstance } from 'ant-design-vue/es/notification/interface'

/**
 * 全局反馈实例。
 *
 * antd 的静态方法（`message.success()`、`Modal.confirm()` 等）不会读取
 * ConfigProvider 的主题，在深色主题下会弹出白底黑字的提示框，非常突兀。
 * 因此组件内一律通过 `App.useApp()` 获取实例，而非组件模块（request / access）
 * 通过这里登记后再调用。
 */
export type FeedbackApis = {
  message: MessageInstance
  modal: Omit<ModalStaticFunctions, 'warn'>
  notification: NotificationInstance
}

export const feedback: Partial<FeedbackApis> = {}

export function registerFeedback(apis: FeedbackApis) {
  Object.assign(feedback, apis)
}