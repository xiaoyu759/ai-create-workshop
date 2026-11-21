<template>
  <div id="appManagePage" class="term-page">
    <!-- 面板头 -->
    <header class="panel-head">
      <span class="panel-head__idx">04</span>
      <h2 class="panel-head__title">应用管理</h2>
      <span class="panel-head__code">APP REGISTRY</span>
      <span class="panel-head__rule" aria-hidden="true"></span>
      <span class="panel-head__meta">TOTAL {{ total }}</span>
    </header>

    <!-- 搜索表单 -->
    <a-form class="term-form" layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="应用名称">
        <a-input v-model:value="searchParams.appName" placeholder="APP NAME" />
      </a-form-item>
      <a-form-item label="创建者">
        <a-input v-model:value="searchParams.userId" placeholder="USER ID" />
      </a-form-item>
      <a-form-item label="生成类型">
        <a-select
          v-model:value="searchParams.codeGenType"
          class="term-select"
          placeholder="ALL TYPES"
          style="width: 150px"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option
            v-for="option in CODE_GEN_TYPE_OPTIONS"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </a-select-option>
        </a-select>
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
        :scroll="{ x: 1180 }"
      >
        <template #headerCell="{ column }">
          <span class="th">
            <span class="th__cn">{{ column.title }}</span>
            <span v-if="column.code" class="th__en">{{ column.code }}</span>
          </span>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'id'">
            <span class="cell-id" :title="String(record.id)">#{{ record.id }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'appName'">
            <span class="cell-name" :title="record.appName">{{ record.appName }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'cover'">
            <span class="cover">
              <a-image
                v-if="record.cover && !brokenCovers.has(String(record.id))"
                :src="record.cover"
                :width="72"
                :height="41"
                class="cover__img"
                @error="markBroken(record)"
              />
              <span v-else class="cover__empty">NO SIGNAL</span>
            </span>
          </template>
          <template v-else-if="column.dataIndex === 'initPrompt'">
            <a-tooltip :title="record.initPrompt">
              <div class="prompt-text">{{ record.initPrompt }}</div>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'codeGenType'">
            <span class="cell-mono">{{ formatCodeGenType(record.codeGenType) }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'priority'">
            <span v-if="record.priority === 99" class="pri pri--on">[ FEATURED ]</span>
            <span v-else class="pri">{{ record.priority ?? 0 }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'deployedTime'">
            <span v-if="record.deployedTime" class="cell-mono">
              {{ formatTime(record.deployedTime) }}
            </span>
            <span v-else class="text-dim">未部署</span>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            <span class="cell-mono">{{ formatTime(record.createTime) }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'user'">
            <span class="cell-user">
              <span class="cell-user__av">
                <img v-if="record.user?.userAvatar" :src="record.user.userAvatar" alt="" />
                <template v-else>{{ initialOf(record.user?.userName) }}</template>
              </span>
              <span class="cell-user__name">{{ record.user?.userName || 'ANONYMOUS' }}</span>
            </span>
          </template>
          <template v-else-if="column.key === 'action'">
            <div class="act-group">
              <button class="term-act term-act--edit" type="button" @click="editApp(record)">
                [ 编辑 ]
              </button>
              <button
                class="term-act term-act--star"
                :class="{ 'is-on': record.priority === 99 }"
                type="button"
                @click="toggleFeatured(record)"
              >
                {{ record.priority === 99 ? '[ 取消精选 ]' : '[ 精选 ]' }}
              </button>
              <a-popconfirm title="确定要删除这个应用吗？" @confirm="deleteApp(record.id)">
                <button class="term-act term-act--danger" type="button">[ 删除 ]</button>
              </a-popconfirm>
            </div>
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
import { listAppVoByPageByAdmin, deleteAppByAdmin, updateAppByAdmin } from '@/api/appController'
import { CODE_GEN_TYPE_OPTIONS, formatCodeGenType } from '@/utils/codeGenTypes'
import { formatTime } from '@/utils/time'
import TerminalPager from '@/components/TerminalPager.vue'

// 封面 URL 失效（OSS 过期 / 网络异常）时回落成 NO SIGNAL，避免露出浏览器的破图图标
const brokenCovers = reactive(new Set<string>())
const markBroken = (record: API.AppVO) => {
  brokenCovers.add(String(record.id))
}

const { message } = App.useApp()

const router = useRouter()

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    code: 'UID',
    width: 104,
    fixed: 'left',
  },
  {
    title: '应用名称',
    dataIndex: 'appName',
    code: 'NAME',
    width: 150,
  },
  {
    title: '封面',
    dataIndex: 'cover',
    code: 'COVER',
    width: 96,
  },
  {
    title: '初始提示词',
    dataIndex: 'initPrompt',
    code: 'PROMPT',
    width: 200,
  },
  {
    title: '生成类型',
    dataIndex: 'codeGenType',
    code: 'TYPE',
    width: 110,
  },
  {
    title: '优先级',
    dataIndex: 'priority',
    code: 'RANK',
    width: 110,
  },
  {
    title: '部署时间',
    dataIndex: 'deployedTime',
    code: 'DEPLOYED',
    width: 160,
  },
  {
    title: '创建者',
    dataIndex: 'user',
    code: 'OWNER',
    width: 130,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    code: 'CREATED',
    width: 160,
  },
  {
    title: '操作',
    key: 'action',
    code: 'ACTION',
    width: 210,
    fixed: 'right',
  },
]

// 数据
const data = ref<API.AppVO[]>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

// 获取数据
const fetchData = async () => {
  try {
    const res = await listAppVoByPageByAdmin({
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

// 搜索
const doSearch = () => {
  // 重置页码
  searchParams.pageNum = 1
  fetchData()
}

// 编辑应用
const editApp = (app: API.AppVO) => {
  router.push(`/app/edit/${app.id}`)
}

// 切换精选状态
const toggleFeatured = async (app: API.AppVO) => {
  if (!app.id) return

  const newPriority = app.priority === 99 ? 0 : 99

  try {
    const res = await updateAppByAdmin({
      id: app.id,
      priority: newPriority,
    })

    if (res.data.code === 0) {
      message.success(newPriority === 99 ? '已设为精选' : '已取消精选')
      // 刷新数据
      fetchData()
    } else {
      message.error('操作失败：' + res.data.message)
    }
  } catch (error) {
    console.error('操作失败：', error)
    message.error('操作失败')
  }
}

// 删除应用
const deleteApp = async (id: number | undefined) => {
  if (!id) return

  try {
    const res = await deleteAppByAdmin({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
      // 刷新数据
      fetchData()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}

// 头像占位：取用户名首字符，无则问号
const initialOf = (name?: string) => (name ? name.charAt(0).toUpperCase() : '?')
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

.term-form :deep(.term-select .ant-select-selector) {
  background: transparent !important;
  border: none !important;
  border-bottom: 1px solid #2a2f38 !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  padding: 0 2px !important;
}

.term-form :deep(.term-select .ant-select-selection-item),
.term-form :deep(.term-select .ant-select-selection-placeholder) {
  font-family: var(--cp-font-mono);
  font-size: 11px;
  letter-spacing: 0.08em;
}

.term-form :deep(.term-select .ant-select-arrow) {
  color: #4b545f;
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

/* ---------- 单元格 ---------- */
.cell-id {
  display: inline-block;
  max-width: 88px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
  font-family: var(--cp-font-mono);
  font-size: 10px;
  letter-spacing: 0.04em;
  color: #5c6773;
}

.cell-name {
  display: inline-block;
  max-width: 132px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.cell-mono {
  font-family: var(--cp-font-mono);
  font-size: 10px;
  letter-spacing: 0.04em;
  color: #6b7480;
}

.cover {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  aspect-ratio: 16 / 9;
  border: 1px solid #262b33;
  background: #0d1015;
  vertical-align: middle;
}

.cover__img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover__empty {
  font-family: var(--cp-font-mono);
  font-size: 8px;
  letter-spacing: 0.14em;
  color: #3f4854;
}

.prompt-text {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #8b95a1;
}

.text-dim {
  color: #3f4854;
  font-family: var(--cp-font-mono);
  font-size: 10px;
}

.pri {
  font-family: var(--cp-font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: #5c6773;
}

.pri--on {
  color: var(--cp-accent);
}

.cell-user {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.cell-user__av {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border: 1px solid #2a2f38;
  color: #5c6773;
  font-family: var(--cp-font-mono);
  font-size: 9px;
  line-height: 1;
  overflow: hidden;
}

.cell-user__av img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cell-user__name {
  max-width: 84px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  color: #8b95a1;
}

/* ---------- 操作列 ---------- */
.act-group {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.term-act {
  padding: 2px 0;
  background: transparent;
  border: none;
  font-family: var(--cp-font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  cursor: pointer;
  transition: none;
  white-space: nowrap;
}

.term-act--edit {
  color: #8b95a1;
}

.term-act--edit:hover,
.term-act--edit:focus-visible {
  color: #ffffff;
}

.term-act--star {
  color: var(--cp-accent);
}

.term-act--star:hover,
.term-act--star:focus-visible,
.term-act--star.is-on {
  color: #ff8a5c;
}

.term-act--danger {
  color: #ff003c;
}

.term-act--danger:hover,
.term-act--danger:focus-visible {
  color: #ff5c72;
  background: rgba(255, 0, 60, 0.08);
}

:deep(.ant-image-img) {
  border-radius: 0;
}
</style>