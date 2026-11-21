<template>
  <section class="au-card">
    <h2 class="au-title">身份登记</h2>
    <p class="au-sub">ENROLLMENT RECORD</p>
    <div class="au-rule"></div>

    <a-form
      :model="formState"
      name="register"
      autocomplete="off"
      class="au-form"
      @finish="handleSubmit"
    >
      <div class="au-field">
        <span class="au-field__key" data-hint="// ACCOUNT">身份名称</span>
        <a-form-item
          name="userAccount"
          :rules="[
            { required: true, message: '请输入账号' },
            { min: 4, message: '账号不能小于 4 位' },
          ]"
        >
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
            { min: 8, message: '密码不能小于 8 位' },
          ]"
        >
          <a-input
            v-model:value="formState.userPassword"
            :type="showPassword ? 'text' : 'password'"
            placeholder="PASSWORD"
            autocomplete="new-password"
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

      <div class="au-field">
        <span class="au-field__key" data-hint="// PASSWORD AGAIN">凭证复核</span>
        <a-form-item
          name="checkPassword"
          :rules="[
            { required: true, message: '请确认密码' },
            { min: 8, message: '密码不能小于 8 位' },
            { validator: validateCheckPassword },
          ]"
        >
          <a-input
            v-model:value="formState.checkPassword"
            :type="showCheck ? 'text' : 'password'"
            placeholder="REPEAT PASSWORD"
            autocomplete="new-password"
          >
            <template #suffix>
              <button
                class="au-eye"
                type="button"
                :aria-label="showCheck ? '隐藏复核凭证' : '显示复核凭证'"
                :aria-pressed="showCheck"
                @mousedown.prevent
                @click="showCheck = !showCheck"
              >
                <svg viewBox="0 0 20 14" fill="none" shape-rendering="crispEdges" aria-hidden="true">
                  <path v-if="!showCheck" class="au-eye__slash" d="M3 12L17 2" />
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
        <span>{{ submitting ? '写入登记记录…' : '提交登记' }}</span>
        <span class="au-cta__en">{{ submitting ? 'WRITING' : 'SUBMIT ENROLLMENT' }}</span>
      </button>

      <p class="au-hint">
        按 <kbd>Enter</kbd> 提交 · 已有身份编号？
        <RouterLink to="/user/login">返回核验 →</RouterLink>
      </p>
    </a-form>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { userRegister } from '@/api/userController.ts'
import { App } from 'ant-design-vue'

const { message } = App.useApp()

const router = useRouter()
const submitting = ref(false)
const showPassword = ref(false)
const showCheck = ref(false)

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

/**
 * 验证确认密码
 * @param rule
 * @param value
 * @param callback
 */
const validateCheckPassword = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: API.UserRegisterRequest) => {
  if (submitting.value) {
    return
  }
  submitting.value = true
  try {
    const res = await userRegister(values)
    if (res.data.code === 0) {
      message.success('登记完成，请核验身份')
      router.push({ path: '/user/login', replace: true })
    } else {
      message.error('登记失败：' + res.data.message)
    }
  } finally {
    submitting.value = false
  }
}
</script>