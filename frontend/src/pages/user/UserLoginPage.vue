<template>
  <section class="au-card">
    <h2 class="au-title">身份确认</h2>
    <p class="au-sub">IDENTITY VERIFICATION</p>
    <div class="au-rule"></div>

    <a-form
      :model="formState"
      name="login"
      autocomplete="off"
      class="au-form"
      @finish="handleSubmit"
    >
      <div class="au-field">
        <span class="au-field__key" data-hint="// ACCOUNT">身份名称</span>
        <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
          <a-input
            v-model:value="formState.userAccount"
            placeholder="ACCOUNT"
            autocomplete="username"
          />
        </a-form-item>
      </div>

      <div class="au-field">
        <span class="au-field__key" data-hint="// PASSWORD">身份凭证</span>
        <a-form-item
          name="userPassword"
          :rules="[
            { required: true, message: '请输入密码' },
            { min: 8, message: '密码长度不能小于 8 位' },
          ]"
        >
          <a-input
            v-model:value="formState.userPassword"
            :type="showPassword ? 'text' : 'password'"
            placeholder="PASSWORD"
            autocomplete="current-password"
          >
            <template #suffix>
              <button
                class="au-eye"
                type="button"
                :aria-label="showPassword ? '隐藏凭证' : '显示凭证'"
                :aria-pressed="showPassword"
                @mousedown.prevent
                @click="showPassword = !showPassword"
              >
                <svg viewBox="0 0 20 14" fill="none" shape-rendering="crispEdges" aria-hidden="true">
                  <path v-if="!showPassword" class="au-eye__slash" d="M3 12L17 2" />
                  <path d="M2 7h1M17 7h1M5 4h10M5 10h10" />
                  <rect x="8.5" y="5.5" width="3" height="3" fill="currentColor" stroke="none" />
                </svg>
              </button>
            </template>
          </a-input>
        </a-form-item>
      </div>

      <button type="submit" class="au-cta" :disabled="submitting">
        <span class="au-cta__scan" aria-hidden="true"></span>
        <span>{{ submitting ? '核对身份资料…' : '确认身份' }}</span>
        <span class="au-cta__en">{{ submitting ? 'VERIFYING' : 'CONFIRM IDENTITY' }}</span>
      </button>

      <p class="au-hint">
        按 <kbd>Enter</kbd> 提交 · 未登记身份？
        <RouterLink to="/user/register">申请身份编号 →</RouterLink>
      </p>
    </a-form>
  </section>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { userLogin } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { useRouter } from 'vue-router'
import { App } from 'ant-design-vue'
import { startAuthBoot } from '@/utils/authBoot'

const { message } = App.useApp()

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const submitting = ref(false)
const showPassword = ref(false)
const router = useRouter()
const loginUserStore = useLoginUserStore()

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  if (submitting.value) {
    return
  }
  submitting.value = true
  try {
    const res = await userLogin(values)
    if (res.data.code === 0 && res.data.data) {
      // 先把登录态写进 store（页头要读），再播转场；进度条走满后才真正跳转
      await loginUserStore.fetchLoginUser()
      startAuthBoot(() => {
        submitting.value = false
        router.push({ path: '/', replace: true })
      })
    } else {
      submitting.value = false
      message.error('核验失败：' + res.data.message)
    }
  } catch (error) {
    submitting.value = false
    throw error
  }
}
</script>