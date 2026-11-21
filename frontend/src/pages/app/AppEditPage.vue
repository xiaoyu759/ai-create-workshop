<template>
  <div id="appEditPage" class="term-page">
    <!-- 面板头：编号 + 标题 + 英文代号 + 当前应用 -->
    <header class="panel-head">
      <span class="panel-head__idx">06</span>
      <h2 class="panel-head__title">编辑应用</h2>
      <span class="panel-head__code">APP PROFILE</span>
      <span class="panel-head__rule" aria-hidden="true"></span>
      <span class="panel-head__meta">ID {{ appInfo?.id ?? '—' }}</span>
    </header>

    <div class="edit-container">
      <a-card class="panel" title="基本信息" :loading="loading">
        <a-form
          class="term-form"
          :model="formData"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit"
          ref="formRef"
        >
          <a-form-item label="应用名称" name="appName">
            <a-input
              v-model:value="formData.appName"
              placeholder="APP NAME"
              :maxlength="50"
              show-count
            />
          </a-form-item>

          <a-form-item
            v-if="isAdmin"
            label="应用封面"
            name="cover"
            extra="支持图片链接，建议尺寸：400x300"
          >
            <a-input v-model:value="formData.cover" placeholder="COVER URL" />
            <div v-if="formData.cover" class="cover-preview">
              <a-image
                :src="formData.cover"
                :width="200"
                :height="150"
                fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
              />
            </div>
          </a-form-item>

          <a-form-item v-if="isAdmin" label="优先级" name="priority" extra="设置为99表示精选应用">
            <a-input-number
              v-model:value="formData.priority"
              :min="0"
              :max="99"
              style="width: 200px"
            />
          </a-form-item>

          <a-form-item label="初始提示词" name="initPrompt">
            <a-textarea
              v-model:value="formData.initPrompt"
              placeholder="INITIAL PROMPT"
              v-auto-grow="{ minRows: 2, maxRows: 8 }"
              :maxlength="1000"
              show-count
              disabled
            />
            <div class="form-tip">初始提示词不可修改</div>
          </a-form-item>

          <a-form-item label="生成类型" name="codeGenType">
            <a-input
              :value="formatCodeGenType(formData.codeGenType)"
              placeholder="GEN TYPE"
              disabled
            />
            <div class="form-tip">生成类型不可修改</div>
          </a-form-item>

          <a-form-item v-if="formData.deployKey" label="部署密钥" name="deployKey">
            <a-input v-model:value="formData.deployKey" placeholder="DEPLOY KEY" disabled />
            <div class="form-tip">部署密钥不可修改</div>
          </a-form-item>

          <a-form-item class="form-actions">
            <a-space>
              <a-button type="primary" html-type="submit" :loading="submitting">
                [ 保存修改 ]
              </a-button>
              <a-button @click="resetForm">[ 重置 ]</a-button>
              <a-button type="link" @click="goToChat">[ 进入对话 ]</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-card>

      <!-- 应用信息展示 -->
      <a-card class="panel panel--meta" title="应用信息">
        <a-descriptions class="term-desc" :column="2" bordered>
          <a-descriptions-item label="应用 ID">
            <span class="cell-mono">{{ appInfo?.id }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="创建者">
            <UserInfo :user="appInfo?.user" size="small" />
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            <span class="cell-mono">{{ formatTime(appInfo?.createTime) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="更新时间">
            <span class="cell-mono">{{ formatTime(appInfo?.updateTime) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="部署时间">
            <span class="cell-mono">
              {{ appInfo?.deployedTime ? formatTime(appInfo.deployedTime) : '未部署' }}
            </span>
          </a-descriptions-item>
          <a-descriptions-item label="访问链接">
            <a-button v-if="appInfo?.deployKey" type="link" @click="openPreview" size="small">
              [ 查看预览 ]
            </a-button>
            <span v-else class="cell-mono">未部署</span>
          </a-descriptions-item>
        </a-descriptions>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { App } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { getAppVoById, updateApp, updateAppByAdmin } from '@/api/appController'
import { formatCodeGenType } from '@/utils/codeGenTypes'
import { formatTime } from '@/utils/time'
import UserInfo from '@/components/UserInfo.vue'
import { getStaticPreviewUrl } from '@/config/env'
import type { FormInstance } from 'ant-design-vue'

const { message } = App.useApp()

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用信息
const appInfo = ref<API.AppVO>()
const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

// 表单数据
const formData = reactive({
  appName: '',
  cover: '',
  priority: 0,
  initPrompt: '',
  codeGenType: '',
  deployKey: '',
})

// 是否为管理员
const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

// 表单验证规则
const rules = {
  appName: [
    { required: true, message: '请输入应用名称', trigger: 'blur' },
    { min: 1, max: 50, message: '应用名称长度在1-50个字符', trigger: 'blur' },
  ],
  cover: [{ type: 'url', message: '请输入有效的URL', trigger: 'blur' }],
  priority: [{ type: 'number', min: 0, max: 99, message: '优先级范围0-99', trigger: 'blur' }],
}

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return
  }

  loading.value = true
  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 检查权限
      if (!isAdmin.value && appInfo.value.userId !== loginUserStore.loginUser.id) {
        message.error('您没有权限编辑此应用')
        router.push('/')
        return
      }

      // 填充表单数据
      formData.appName = appInfo.value.appName || ''
      formData.cover = appInfo.value.cover || ''
      formData.priority = appInfo.value.priority || 0
      formData.initPrompt = appInfo.value.initPrompt || ''
      formData.codeGenType = appInfo.value.codeGenType || ''
      formData.deployKey = appInfo.value.deployKey || ''
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error('获取应用信息失败')
    router.push('/')
  } finally {
    loading.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!appInfo.value?.id) return

  submitting.value = true
  try {
    let res
    if (isAdmin.value) {
      // 管理员可以修改更多字段
      res = await updateAppByAdmin({
        id: appInfo.value.id,
        appName: formData.appName,
        cover: formData.cover,
        priority: formData.priority,
      })
    } else {
      // 普通用户只能修改应用名称
      res = await updateApp({
        id: appInfo.value.id,
        appName: formData.appName,
      })
    }

    if (res.data.code === 0) {
      message.success('修改成功')
      // 重新获取应用信息
      await fetchAppInfo()
    } else {
      message.error('修改失败：' + res.data.message)
    }
  } catch (error) {
    console.error('修改失败：', error)
    message.error('修改失败')
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  if (appInfo.value) {
    formData.appName = appInfo.value.appName || ''
    formData.cover = appInfo.value.cover || ''
    formData.priority = appInfo.value.priority || 0
  }
  formRef.value?.clearValidate()
}

// 进入对话页面
const goToChat = () => {
  if (appInfo.value?.id) {
    router.push(`/app/chat/${appInfo.value.id}`)
  }
}

// 打开预览
const openPreview = () => {
  if (appInfo.value?.codeGenType && appInfo.value?.id) {
    const url = getStaticPreviewUrl(appInfo.value.codeGenType, String(appInfo.value.id))
    window.open(url, '_blank')
  }
}

// 页面加载时获取应用信息
onMounted(() => {
  fetchAppInfo()
})
</script>

<style scoped>
.term-page {
  width: 100%;
  margin-top: 16px;
  padding: 20px 22px 18px;
  background: var(--cp-bg-surface);
  border: 1px solid var(--cp-border);
  border-radius: 2px;
  max-width: 1040px;
  margin-left: auto;
  margin-right: auto;
}

/* ---------- 面板头 ---------- */
.panel-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--cp-border-weak);
  margin-bottom: 18px;
}

.panel-head__idx {
  font-family: var(--cp-font-mono);
  font-size: 10px;
  letter-spacing: 0.16em;
  color: var(--cp-accent);
}

.panel-head__title {
  margin: 0;
  font-family: var(--cp-font-mono);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.1em;
  color: var(--cp-text);
}

.panel-head__code {
  font-family: var(--cp-font-mono);
  font-size: 9px;
  letter-spacing: 0.28em;
  color: #4b545f;
}

.panel-head__rule {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, var(--cp-border-weak), transparent);
}

.panel-head__meta {
  font-family: var(--cp-font-mono);
  font-size: 9px;
  letter-spacing: 0.2em;
  color: #4b545f;
}

/* ---------- 面板 ---------- */
.edit-container {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel :deep(.ant-card) {
  background: transparent;
  border: 1px solid var(--cp-border-weak);
  border-radius: 2px;
  box-shadow: none;
}

.panel :deep(.ant-card-head) {
  min-height: 40px;
  padding: 0 14px;
  background: var(--cp-bg-elevated);
  border-bottom: 1px solid var(--cp-border-weak);
}

.panel :deep(.ant-card-head-title) {
  padding: 10px 0;
  font-family: var(--cp-font-mono);
  font-size: 11px;
  letter-spacing: 0.18em;
  color: var(--cp-text);
}

.panel :deep(.ant-card-body) {
  padding: 16px 14px;
}

/* ---------- 表单：终端下划线 ---------- */
.term-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.term-form :deep(.ant-form-item-label > label) {
  font-family: var(--cp-font-mono);
  font-size: 10px;
  letter-spacing: 0.16em;
  color: #5c6773;
}

.term-form :deep(.ant-form-item-extra) {
  font-family: var(--cp-font-mono);
  font-size: 9px;
  letter-spacing: 0.08em;
  color: #4b545f;
  margin-top: 4px;
}

.term-form :deep(.ant-input:not(textarea)),
.term-form :deep(.ant-input-number) {
  background: transparent;
  border: none;
  border-bottom: 1px solid #2a2f38;
  border-radius: 0;
  box-shadow: none;
  font-family: var(--cp-font-mono);
  font-size: 12px;
  letter-spacing: 0.06em;
  color: var(--cp-text);
}

.term-form :deep(.ant-input:not(textarea):hover),
.term-form :deep(.ant-input-number:hover) {
  border-bottom-color: #3a424e;
}

.term-form :deep(.ant-input:not(textarea):focus),
.term-form :deep(.ant-input-number-focused) {
  border-bottom-color: var(--cp-accent);
  box-shadow: none;
}

.term-form :deep(.ant-input-number-input) {
  font-family: var(--cp-font-mono);
  font-size: 12px;
}

.term-form :deep(textarea.ant-input) {
  padding: 10px 12px;
  background: var(--cp-bg-elevated);
  border: 1px solid #2a2f38;
  border-radius: 2px;
  box-shadow: none;
  font-family: var(--cp-font-mono);
  font-size: 12px;
  line-height: 1.6;
  color: var(--cp-text-secondary);
  resize: none;
}

.term-form :deep(.ant-input[disabled]),
.term-form :deep(textarea.ant-input[disabled]) {
  background: rgba(255, 255, 255, 0.02);
  color: #6b7480;
  cursor: not-allowed;
}

.term-form :deep(.ant-input-data-count) {
  font-family: var(--cp-font-mono);
  font-size: 9px;
  color: #4b545f;
}

.form-tip {
  margin-top: 4px;
  font-family: var(--cp-font-mono);
  font-size: 9px;
  letter-spacing: 0.08em;
  color: #4b545f;
}

.form-actions :deep(.ant-btn) {
  border-radius: 0;
  font-family: var(--cp-font-mono);
  font-size: 11px;
  letter-spacing: 0.1em;
}

.cover-preview {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid var(--cp-border);
  background: var(--cp-bg-elevated);
}

.cover-preview :deep(.ant-image-img) {
  border-radius: 0;
}

/* ---------- 应用信息表 ---------- */
.term-desc :deep(.ant-descriptions-view) {
  border: 1px solid var(--cp-border-weak);
  border-radius: 0;
}

.term-desc :deep(.ant-descriptions-item-label) {
  padding: 8px 12px;
  background: var(--cp-bg-elevated);
  border-right: 1px solid var(--cp-border-weak);
  font-family: var(--cp-font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  color: #5c6773;
}

.term-desc :deep(.ant-descriptions-item-content) {
  padding: 8px 12px;
  border-right: 1px solid var(--cp-border-weak);
  font-size: 12px;
  color: var(--cp-text-secondary);
}

.term-desc :deep(.ant-descriptions-row > th),
.term-desc :deep(.ant-descriptions-row > td) {
  border-bottom: 1px solid var(--cp-border-weak);
}

.cell-mono {
  font-family: var(--cp-font-mono);
  font-size: 11px;
  letter-spacing: 0.04em;
  color: #8b95a1;
}
</style>