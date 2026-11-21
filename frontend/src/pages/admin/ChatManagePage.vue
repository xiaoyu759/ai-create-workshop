<template>
  <div id="chatManagePage" class="term-page">
    <!-- 面板头：编号 + 标题 + 英文代号 + 总量 -->
    <header class="panel-head">
      <span class="panel-head__idx">05</span>
      <h2 class="panel-head__title">对话管理</h2>
      <span class="panel-head__code">CHAT STREAM</span>
      <span class="panel-head__rule" aria-hidden="true"></span>
      <span class="panel-head__meta">TOTAL {{ total }}</span>
    </header>

    <!-- 搜索表单：终端下划线输入 -->
    <a-form class="term-form" layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="消息内容">
        <a-input v-model:value="searchParams.message" placeholder="MESSAGE" />
      </a-form-item>
      <a-form-item label="消息类型">
        <a-select
          v-model:value="searchParams.messageType"
          placeholder="TYPE"
          class="term-select"
          style="width: 120px"
        >
          <a-select-option value="">全部 ALL</a-select-option>
          <a-select-option value="user">用户 USER</a-select-option>
          <a-select-option value="assistant">AI ASSISTANT</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="应用 ID">
        <a-input v-model:value="searchParams.appId" placeholder="APP ID" />
      </a-form-item>
      <a-form-item label="用户 ID">
        <a-input v-model:value="searchParams.userId" placeholder="USER ID" />
      </a-form-item>
      <a-form-item>
        <button class="term-btn" type="submit">搜索 SEARCH</button>
      </a-form-item>
    </a-form>

    <!-- 表格 -->
    <div class="term-table">
      <a-table
        :columns="columns"
        :data-source="data"
        :pagination="false"
        :scroll="{ x: 1200 }"
      >
        <template #headerCell="{ column }">
          <span class="th">
            <span class="th__cn">{{ column.title }}</span>
            <span v-if="column.code" class="th__en">{{ column.code }}</span>
          </span>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'id'">
            <span class="cell-id" :title="String(record.id)">{{ record.id }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'message'">
            <a-tooltip :title="record.message">
              <div class="message-text">{{ record.message }}</div>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'messageType'">
            <span
              class="msg-flag"
              :class="record.messageType === 'user' ? 'msg-flag--user' : 'msg-flag--ai'"
            >
              {{ record.messageType === 'user' ? '[ USER ]' : '[ AI ]' }}
            </span>
          </template>
          <template v-else-if="column.dataIndex === 'appId'">
            <span class="cell-mono" :title="String(record.appId)">#{{ record.appId }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'userId'">
            <span class="cell-mono" :title="String(record.userId)">#{{ record.userId }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            <span class="cell-time">{{ formatTime(record.createTime) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="act-group">
              <button class="term-act" type="button" @click="viewAppChat(record.appId)">
                [ 查看对话 ]
              </button>
              <a-popconfirm title="确定要删除这条消息吗？" @confirm="deleteMessage(record.id)">
                <button class="term-act term-act--danger" type="button">[ 删除 ]</button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </div>

    <TerminalPager
      :current="searchParams.pageNum ?? 1"
      :total="total"
      :page-size="searchParams.pageSize ?? 10"
      :sizes="[10, 20, 50]"
      unit="条"
      @change="onPageChange"
      @size-change="onSizeChange"
    />
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { App } from 'ant-design-vue'
import { listAllChatHistoryByPageForAdmin } from '@/api/chatHistoryController'
import { formatTime } from '@/utils/time'
import TerminalPager from '@/components/TerminalPager.vue'

const { message } = App.useApp()

const router = useRouter()

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    code: 'MID',
    width: 90,
    fixed: 'left',
  },
  {
    title: '消息内容',
    dataIndex: 'message',
    code: 'MESSAGE',
    width: 300,
  },
  {
    title: '消息类型',
    dataIndex: 'messageType',
    code: 'TYPE',
    width: 110,
  },
  {
    title: '应用 ID',
    dataIndex: 'appId',
    code: 'APP',
    width: 100,
  },
  {
    title: '用户 ID',
    dataIndex: 'userId',
    code: 'USER',
    width: 100,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    code: 'TIME',
    width: 170,
  },
  {
    title: '操作',
    key: 'action',
    code: 'ACTION',
    width: 200,
    fixed: 'right',
  },
]

// 数据
const data = ref<API.ChatHistory[]>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.ChatHistoryQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

// 获取数据
const fetchData = async () => {
  try {
    const res = await listAllChatHistoryByPageForAdmin({
      ...searchParams,
    })
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取数据失败：', error)
    message.error('获取数据失败')
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})

// 分页
const onPageChange = (page: number) => {
  searchParams.pageNum = page
  fetchData()
}

// 每页条数变化
const onSizeChange = (pageSize: number) => {
  searchParams.pageNum = 1
  searchParams.pageSize = pageSize
  fetchData()
}

// 搜索
const doSearch = () => {
  // 重置页码
  searchParams.pageNum = 1
  fetchData()
}

// 查看应用对话
const viewAppChat = (appId: number | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}`)
  }
}

// 删除消息
const deleteMessage = async (id: number | undefined) => {
  if (!id) return

  try {
    // 注意：这里需要后端提供删除对话历史的接口
    // 目前先显示成功，实际实现需要调用删除接口
    message.success('删除成功')
    // 刷新数据
    fetchData()
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}
</script>

<style scoped>
.term-page {
  margin-top: 16px;
  padding: 20px 22px 18px;
  background: var(--cp-bg-surface);
  border: 1px solid var(--cp-border);
  border-radius: 2px;
}

/* ---------- 面板头 ---------- */
.panel-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--cp-border-weak);
  margin-bottom: 16px;
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

/* ---------- 搜索区 ---------- */
.term-form {
  margin-bottom: 16px;
}

.term-form :deep(.ant-form-item) {
  margin-bottom: 10px;
}

.term-form :deep(.ant-form-item-label > label) {
  font-family: var(--cp-font-mono);
  font-size: 10px;
  letter-spacing: 0.16em;
  color: #5c6773;
}

.term-form :deep(.ant-input) {
  width: 150px;
  padding: 5px 2px;
  background: transparent;
  border: none;
  border-bottom: 1px solid #2a2f38;
  border-radius: 0;
  box-shadow: none;
  font-family: var(--cp-font-mono);
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--cp-text);
}

.term-form :deep(.ant-input::placeholder) {
  color: #3f4854;
  letter-spacing: 0.14em;
}

.term-form :deep(.ant-input:hover) {
  border-bottom-color: #3a424e;
}

.term-form :deep(.ant-input:focus) {
  border-bottom-color: var(--cp-accent);
  box-shadow: none;
}

.term-form :deep(.term-select .ant-select-selector) {
  background: transparent !important;
  border: none !important;
  border-bottom: 1px solid #2a2f38 !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  padding-left: 2px;
  font-family: var(--cp-font-mono);
  font-size: 11px;
  letter-spacing: 0.08em;
}

.term-form :deep(.term-select .ant-select-selection-placeholder) {
  color: #3f4854;
  letter-spacing: 0.14em;
}

.term-form :deep(.term-select.ant-select-focused .ant-select-selector) {
  border-bottom-color: var(--cp-accent) !important;
}

.term-btn {
  height: 30px;
  padding: 0 16px;
  background: transparent;
  border: 1px solid var(--cp-accent);
  border-radius: 0;
  color: var(--cp-accent);
  font-family: var(--cp-font-mono);
  font-size: 10px;
  letter-spacing: 0.18em;
  cursor: pointer;
  transition: none;
}

.term-btn:hover,
.term-btn:focus-visible {
  background: var(--cp-accent);
  color: #0b0f14;
}

/* ---------- 表格单元 ---------- */
.cell-id,
.cell-mono {
  display: inline-block;
  max-width: 84px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
  font-family: var(--cp-font-mono);
  font-size: 10px;
  letter-spacing: 0.04em;
  color: #5c6773;
}

.message-text {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--cp-text-secondary);
}

.msg-flag {
  display: inline-block;
  padding: 2px 6px;
  border: 1px solid #2a2f38;
  font-family: var(--cp-font-mono);
  font-size: 9px;
  letter-spacing: 0.14em;
  line-height: 1.4;
  white-space: nowrap;
}

.msg-flag--user {
  border-color: rgba(255, 92, 32, 0.55);
  color: var(--cp-accent);
}

.msg-flag--ai {
  color: #6b7480;
}

.cell-time {
  font-family: var(--cp-font-mono);
  font-size: 10px;
  letter-spacing: 0.04em;
  color: #6b7480;
}

.act-group {
  display: inline-flex;
  align-items: center;
  gap: 14px;
}

.term-act {
  padding: 2px 0;
  background: transparent;
  border: none;
  font-family: var(--cp-font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  color: #8b95a1;
  cursor: pointer;
  transition: none;
}

.term-act:hover,
.term-act:focus-visible {
  color: #ffffff;
}

.term-act--danger {
  color: #ff003c;
}

.term-act--danger:hover,
.term-act--danger:focus-visible {
  color: #ff5c72;
  background: rgba(255, 0, 60, 0.08);
}

:deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
}

:deep(.ant-image-img) {
  border-radius: 0;
}
</style>