<template>
  <div id="userManagePage" class="term-page">
    <!-- 面板头：编号 + 标题 + 英文代号 + 总量 -->
    <header class="panel-head">
      <span class="panel-head__idx">03</span>
      <h2 class="panel-head__title">用户管理</h2>
      <span class="panel-head__code">USER DIRECTORY</span>
      <span class="panel-head__rule" aria-hidden="true"></span>
      <span class="panel-head__meta">TOTAL {{ total }}</span>
    </header>

    <!-- 搜索表单：终端下划线输入 -->
    <a-form class="term-form" layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="账号">
        <a-input v-model:value="searchParams.userAccount" placeholder="ACCOUNT" />
      </a-form-item>
      <a-form-item label="用户名">
        <a-input v-model:value="searchParams.userName" placeholder="USERNAME" />
      </a-form-item>
      <a-form-item>
        <button class="term-btn" type="submit">搜索 SEARCH</button>
      </a-form-item>
    </a-form>

    <!-- 表格 -->
    <div class="term-table">
      <a-table :columns="columns" :data-source="data" :pagination="false">
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
          <template v-else-if="column.dataIndex === 'userAvatar'">
            <span class="cell-avatar">
              <a-image
                v-if="record.userAvatar"
                :src="record.userAvatar"
                :width="16"
                :height="16"
                :preview="false"
                class="cell-avatar__img"
              />
              <span v-else class="cell-avatar__blank">?</span>
              <span v-if="!record.userAvatar" class="cell-avatar__anon">ANONYMOUS</span>
            </span>
          </template>
          <template v-else-if="column.dataIndex === 'userRole'">
            <span class="role" :class="record.userRole === 'admin' ? 'role--admin' : 'role--user'">
              {{ record.userRole === 'admin' ? '[ ADMIN ]' : '[ USER ]' }}
            </span>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            <span class="cell-time">{{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <button class="term-act term-act--danger" type="button" @click="doDelete(record.id)">
              [ 删除 ]
            </button>
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
import { deleteUser, listUserVoByPage } from '@/api/userController.ts'
import { App } from 'ant-design-vue'
import dayjs from 'dayjs'
import TerminalPager from '@/components/TerminalPager.vue'

const { message } = App.useApp()

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    code: 'UID',
    width: 96,
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
    code: 'ACCOUNT',
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    code: 'NAME',
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
    code: 'AVATAR',
    width: 140,
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
    code: 'PROFILE',
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
    code: 'ROLE',
    width: 120,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    code: 'CREATED',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    code: 'ACTION',
    width: 110,
  },
]

// 展示的数据
const data = ref<API.UserVO[]>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

// 获取数据
const fetchData = async () => {
  const res = await listUserVoByPage({
    ...searchParams,
  })
  if (res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

// 分页：终端分页器接管（页码 / 每页条数）
const onPageChange = (page: number) => {
  searchParams.pageNum = page
  fetchData()
}

const onSizeChange = (size: number) => {
  searchParams.pageSize = size
  searchParams.pageNum = 1
  fetchData()
}

// 搜索数据
const doSearch = () => {
  // 重置页码
  searchParams.pageNum = 1
  fetchData()
}

// 删除数据
const doDelete = async (id: number) => {
  if (!id) {
    return
  }
  const res = await deleteUser({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 刷新数据
    fetchData()
  } else {
    message.error('删除失败')
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})
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
  width: 170px;
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
.cell-id {
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

.cell-avatar {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.cell-avatar__img {
  display: block;
  width: 16px;
  height: 16px;
  border: 1px solid var(--cp-border);
}

.cell-avatar__img :deep(img) {
  display: block;
  width: 16px;
  height: 16px;
  object-fit: cover;
}

.cell-avatar__blank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border: 1px solid #2a2f38;
  color: #4b545f;
  font-family: var(--cp-font-mono);
  font-size: 9px;
  line-height: 1;
}

.cell-avatar__anon {
  font-family: var(--cp-font-mono);
  font-size: 9px;
  letter-spacing: 0.16em;
  color: #343b45;
}

.role {
  display: inline-block;
  padding: 2px 6px;
  border: 1px solid #2a2f38;
  font-family: var(--cp-font-mono);
  font-size: 9px;
  letter-spacing: 0.14em;
  line-height: 1.4;
  white-space: nowrap;
}

.role--admin {
  border-color: rgba(255, 92, 32, 0.55);
  color: var(--cp-accent);
}

.role--user {
  color: #6b7480;
}

.cell-time {
  font-family: var(--cp-font-mono);
  font-size: 10px;
  letter-spacing: 0.04em;
  color: #6b7480;
}

.term-act {
  padding: 2px 0;
  background: transparent;
  border: none;
  font-family: var(--cp-font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  cursor: pointer;
  transition: none;
}

.term-act--danger {
  color: #ff003c;
}

.term-act--danger:hover,
.term-act--danger:focus-visible {
  color: #ff5c72;
  background: rgba(255, 0, 60, 0.08);
}

/* 图片预览层保持直角 */
:deep(.ant-image-img) {
  border-radius: 0;
}
</style>