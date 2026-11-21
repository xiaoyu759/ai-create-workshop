<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { App } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, listMyAppVoByPage, listGoodAppVoByPage } from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import AppCard from '@/components/AppCard.vue'
import TerminalPager from '@/components/TerminalPager.vue'

const { message } = App.useApp()

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

// 用户提示词
const userPrompt = ref('')
const creating = ref(false)

// 通电闪烁（从登录页切入时由 CRT 转场触发）。状态挂在 <html> 上，见 handlePowerOn。
let bootTimer: number | undefined

// 我的应用数据
const myApps = ref<API.AppVO[]>([])
const myAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

// 精选应用数据
const featuredApps = ref<API.AppVO[]>([])
const featuredAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})
// 是否已完成过一次精选列表请求：避免首屏先闪一下「无数据」
const featuredLoaded = ref(false)

// 设置提示词
const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
}

// 卡片状态：后端 AppVO 暂无 status 字段，先用「是否有截图 / 部署记录 + 创建时长」兜底推导。
// 支持 ?cardState=failed 强制预览三种状态；后端补字段后只需改 statusOf 一处。
type AppCardStatus = 'completed' | 'generating' | 'failed'

const parseAppTime = (value?: string) => {
  if (!value) return 0
  const ts = Date.parse(value.replace(/-/g, '/'))
  return Number.isNaN(ts) ? 0 : ts
}

const forcedCardState = (() => {
  const value = route.query.cardState
  return value === 'completed' || value === 'generating' || value === 'failed'
    ? (value as AppCardStatus)
    : undefined
})()

const statusOf = (app: API.AppVO): AppCardStatus => {
  if (forcedCardState) return forcedCardState
  if (app.cover || app.deployKey) return 'completed'
  const created = parseAppTime(app.createTime)
  if (!created) return 'generating'
  return Date.now() - created < 3 * 60 * 1000 ? 'generating' : 'failed'
}

// 创建应用
const createApp = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请输入应用描述')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return
  }

  creating.value = true
  try {
    const res = await addApp({
      initPrompt: userPrompt.value.trim(),
    })

    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功')
      // 跳转到对话页面，确保ID是字符串类型
      const appId = String(res.data.data)
      await router.push(`/app/chat/${appId}`)
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } catch (error) {
    console.error('创建应用失败：', error)
    message.error('创建失败，请重试')
  } finally {
    creating.value = false
  }
}

// 加载我的应用
const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) {
    return
  }

  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPage.current,
      pageSize: myAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载我的应用失败：', error)
  }
}

// 加载精选应用
const loadFeaturedApps = async () => {
  try {
    const res = await listGoodAppVoByPage({
      pageNum: featuredAppsPage.current,
      pageSize: featuredAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载精选应用失败：', error)
  } finally {
    featuredLoaded.value = true
  }
}

// 分页：换成终端命令行样式后，页码变化由组件抛事件
const onMyAppsPageChange = (page: number) => {
  myAppsPage.current = page
  loadMyApps()
}

const onFeaturedAppsPageChange = (page: number) => {
  featuredAppsPage.current = page
  loadFeaturedApps()
}

// 失败卡片「查看日志」：后端暂未提供日志接口，先展示应用记录本身（字段全部来自 AppVO）
const logOpen = ref(false)
const logApp = ref<API.AppVO | null>(null)

const viewLog = (app: API.AppVO) => {
  logApp.value = app
  logOpen.value = true
}

// 失败卡片「重新生成」：把原提示词填回输入框并聚焦，复用原有创建流程，不新增接口
const regenerate = (app: API.AppVO) => {
  setPrompt(app.initPrompt || '')
  document.querySelector('.prompt-input')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  window.setTimeout(() => {
    const input = document.querySelector('.prompt-input') as HTMLTextAreaElement | null
    input?.focus()
  }, 320)
  message.info('已填入原提示词，确认后可重新生成')
}

// 查看对话
const viewChat = (appId: string | number | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}?view=1`)
  }
}

// 查看作品
const viewWork = (app: API.AppVO) => {
  if (app.deployKey) {
    const url = getDeployUrl(app.deployKey)
    window.open(url, '_blank')
  }
}

// CRT 转场播放到一半时开始「通电闪烁」。
// 加在 <html> 上而不是 #homePage 上：页头也在 #app 里，整机一起闪才像显示器通电，
// 只闪内容区的话页头会始终亮着，看起来像"贴了一张图"。
const handlePowerOn = () => {
  const root = document.documentElement
  root.classList.remove('home-boot')
  // 强制回流，保证连续两次进入主页都能重新触发同一段动画
  void root.offsetWidth
  root.classList.add('home-boot')
  window.clearTimeout(bootTimer)
  bootTimer = window.setTimeout(() => {
    root.classList.remove('home-boot')
  }, 1500)
}

// 页面加载时获取数据
onMounted(() => {
  document.documentElement.classList.add('home-mode')
  window.addEventListener('cp:power-on', handlePowerOn)
  loadMyApps()
  loadFeaturedApps()
})

onBeforeUnmount(() => {
  document.documentElement.classList.remove('home-mode')
  document.documentElement.classList.remove('home-boot')
  window.removeEventListener('cp:power-on', handlePowerOn)
  window.clearTimeout(bootTimer)
})
</script>

<template>
  <div id="homePage">
    <div class="container">

      <!-- 网站标题和描述 -->
      <div class="hero-section">
        <p class="hero-kicker">
          <i class="kicker-dot"></i>
          SYS://HOME — NEURAL LINK ACTIVE
        </p>
        <h1 class="hero-title">AI 应用创意工坊</h1>
        <p class="hero-sub">AI CREATIVE WORKSHOP</p>
        <p class="hero-secure">
          <i class="secure-dot" aria-hidden="true"></i>
          <span>SECURE CHANNEL ESTABLISHED</span>
          <span class="secure-bar" aria-hidden="true">|</span>
          <span class="secure-cn">安全通道已建立</span>
        </p>
        <p class="hero-description">一句话轻松创建网站应用</p>
      </div>

      <!-- 用户提示词输入框：拼装出来的金属面板 -->
      <div class="input-section">
        <span class="prompt-cursor" aria-hidden="true">&gt;_</span>
        <a-textarea
          v-model:value="userPrompt"
          placeholder="帮我创建个人博客网站"
          v-auto-grow="{ minRows: 3, maxRows: 8 }"
          :maxlength="1000"
          class="prompt-input"
        />
        <div class="input-actions">
          <span class="send-wrap">
            <a-button
              type="primary"
              size="large"
              aria-label="生成应用"
              @click="createApp"
              :loading="creating"
            >
              <template #icon>
                <span>↑</span>
              </template>
            </a-button>
            <span class="scan-ring" aria-hidden="true"></span>
          </span>
        </div>
        <span class="panel-screw panel-screw--tl" aria-hidden="true"></span>
        <span class="panel-screw panel-screw--tr" aria-hidden="true"></span>
        <span class="panel-screw panel-screw--bl" aria-hidden="true"></span>
        <span class="panel-screw panel-screw--br" aria-hidden="true"></span>
        <span class="panel-serial" aria-hidden="true">PORT 0x1F · 5.02V · LINK OK</span>
        <span class="panel-glitch" aria-hidden="true"></span>
        <!-- 胶带警告标签：直接贴在机箱面板的角上 -->
        <div class="tape tape--panel-a" aria-hidden="true"><span>PROPERTY OF NIGHT CITY</span></div>
        <div class="tape tape--panel-b" aria-hidden="true"><span>DO NOT REMOVE</span></div>
        <span class="scan-ring" aria-hidden="true"></span>
      </div>

      <!-- 快捷按钮：散落的旧磁带 -->
      <div class="quick-actions">
        <a-button
          type="default"
          class="quick-chip"
          style="--tilt: -2deg; --dy: -3px"
          @click="
            setPrompt(
              '创建一个现代化的个人博客网站，包含文章列表、详情页、分类标签、搜索功能、评论系统和个人简介页面。采用简洁的设计风格，支持响应式布局，文章支持Markdown格式，首页展示最新文章和热门推荐。',
            )
          "
          ><span class="chip-index" aria-hidden="true">01</span>个人博客网站</a-button
        >
        <a-button
          type="default"
          class="quick-chip"
          style="--tilt: 1.4deg; --dy: 2px"
          @click="
            setPrompt(
              '设计一个专业的企业官网，包含公司介绍、产品服务展示、新闻资讯、联系我们等页面。采用商务风格的设计，包含轮播图、产品展示卡片、团队介绍、客户案例展示，支持多语言切换和在线客服功能。',
            )
          "
          ><span class="chip-index" aria-hidden="true">02</span>企业官网</a-button
        >
        <a-button
          type="default"
          class="quick-chip"
          style="--tilt: -1.2deg; --dy: -1px"
          @click="
            setPrompt(
              '构建一个功能完整的在线商城，包含商品展示、购物车、用户注册登录、订单管理、支付结算等功能。设计现代化的商品卡片布局，支持商品搜索筛选、用户评价、优惠券系统和会员积分功能。',
            )
          "
          ><span class="chip-index" aria-hidden="true">03</span>在线商城</a-button
        >
        <a-button
          type="default"
          class="quick-chip"
          style="--tilt: 2.2deg; --dy: 4px"
          @click="
            setPrompt(
              '制作一个精美的作品展示网站，适合设计师、摄影师、艺术家等创作者。包含作品画廊、项目详情页、个人简历、联系方式等模块。采用瀑布流或网格布局展示作品，支持图片放大预览和作品分类筛选。',
            )
          "
          ><span class="chip-index" aria-hidden="true">04</span>作品展示网站</a-button
        >
      </div>

      <!-- 我的作品 -->
      <div class="section">
        <h2 class="section-title"><span class="section-index">01</span>我的作品</h2>
        <div class="app-grid">
          <AppCard
            v-for="app in myApps"
            :key="app.id"
            :app="app"
            :status="statusOf(app)"
            @view-chat="viewChat"
            @view-work="viewWork"
            @view-log="viewLog"
            @regenerate="regenerate"
          />
        </div>
        <div class="pagination-wrapper">
          <TerminalPager
            :current="myAppsPage.current"
            :total="myAppsPage.total"
            :page-size="myAppsPage.pageSize"
            unit="应用"
            @change="onMyAppsPageChange"
          />
        </div>
      </div>

      <!-- 精选案例 -->
      <div class="section">
        <h2 class="section-title"><span class="section-index">02</span>精选案例</h2>
        <div class="featured-grid">
          <AppCard
            v-for="app in featuredApps"
            :key="app.id"
            :app="app"
            :featured="true"
            :status="statusOf(app)"
            @view-chat="viewChat"
            @view-work="viewWork"
            @view-log="viewLog"
            @regenerate="regenerate"
          />
        </div>
        <div v-if="featuredLoaded && !featuredApps.length" class="cp-empty">
          <span class="cp-empty__tag">[ SYSTEM IDLE ]</span>
          <p class="cp-empty__title">&gt;&gt; NO CASE RECORDS FOUND.</p>
          <p class="cp-empty__hint">// 暂无精选案例 · 管理员在「应用管理」标记精选后自动同步</p>
        </div>
        <div class="pagination-wrapper">
          <TerminalPager
            :current="featuredAppsPage.current"
            :total="featuredAppsPage.total"
            :page-size="featuredAppsPage.pageSize"
            unit="案例"
            @change="onFeaturedAppsPageChange"
          />
        </div>
      </div>
    </div>

    <a-modal v-model:open="logOpen" :footer="null" :width="560" class="ac-log-modal">
      <div class="ac-log">
        <p class="ac-log__head">
          // APP RECORD —
          <span>{{ logApp?.appName || 'UNTITLED' }}</span>
        </p>
        <div class="ac-log__row"><span>ID</span><b>{{ logApp?.id ?? '—' }}</b></div>
        <div class="ac-log__row">
          <span>COVER</span><b>{{ logApp?.cover ? 'PRESENT' : 'MISSING' }}</b>
        </div>
        <div class="ac-log__row"><span>DEPLOY KEY</span><b>{{ logApp?.deployKey || '—' }}</b></div>
        <div class="ac-log__row"><span>CREATE</span><b>{{ logApp?.createTime || '—' }}</b></div>
        <div class="ac-log__row"><span>UPDATE</span><b>{{ logApp?.updateTime || '—' }}</b></div>
        <div class="ac-log__row"><span>PROMPT</span><b>{{ logApp?.initPrompt || '—' }}</b></div>
        <p class="ac-log__tip">后端日志接口接入后，这里替换为真实构建日志流。</p>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
/*
 * 视觉主题：NIGHT CITY LOW-LIFE TERMINAL
 * 设定：地下黑客用二手零件拼出来的终端 —— 金属面板、胶带、错位、噪点、绿色光标。
 * 颜色纪律：底色只有 #131418 系灰阶；强调色三枚（酸绿 / 粉 / 警示红）+ 胶带黄，
 *           每屏强调色总量控制在 5% 以内，其余全部让位给灰阶与材质。
 */
#homePage {
  --ll-ink: #e9edf2;
  --ll-dim: #8b93a1;
  --ll-acid: #ff5c20;
  --ll-pink: #ff2e97;
  --ll-warn: #ff2d3f;
  --ll-tape: #f2c500;
  --ll-mono: 'JetBrains Mono', 'Cascadia Code', 'Rajdhani', ui-monospace, monospace;

  position: relative;
  width: 100%;
  padding: 54px 0 90px;
  background: transparent;
}



.container {
  position: relative;
  z-index: 3;
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ── 胶带：黄黑斜纹 + 撕边 ─────────────────────────── */
.tape {
  position: absolute;
  z-index: 4;
  padding: 4px 0;
  font-family: var(--ll-mono);
  font-size: 9px;
  letter-spacing: 0.2em;
  color: #101104;
  background: repeating-linear-gradient(
    -45deg,
    var(--ll-tape) 0 10px,
    #16181c 10px 20px
  );
  box-shadow:
    0 12px 26px rgba(0, 0, 0, 0.6),
    0 1px 0 rgba(255, 255, 255, 0.12) inset;
  pointer-events: none;
}

.tape span {
  display: inline-block;
  padding: 3px 12px;
  background: var(--ll-tape);
  clip-path: polygon(0 0, 100% 2%, 98% 100%, 2% 96%);
}

/* 贴在机箱"外侧"：整体落在面板矩形以外，内边缘只差 2px 就要压到边框，
   这样既保留随手一贴的物理感，又永远不会盖住输入内容 */
.tape--panel-a {
  top: -30px;
  right: -36px;
  transform: rotate(-2deg);
}

.tape--panel-b {
  bottom: -34px;
  left: -36px;
  transform: rotate(-2deg);
}

/* ── 标题区 ─────────────────────────────────────── */
.hero-section {
  position: relative;
  text-align: center;
  margin: 18px auto 34px;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  margin: 0 0 22px;
  font-family: var(--ll-mono);
  font-size: 10px;
  letter-spacing: 0.3em;
  color: #3f4753;
}

.kicker-dot {
  width: 6px;
  height: 6px;
  background: var(--ll-acid);
  box-shadow: 0 0 10px rgba(255, 92, 32, 0.9);
  animation: cpDotBlink 1.6s steps(1, end) infinite;
}

@keyframes cpDotBlink {
  0%,
  62% {
    opacity: 1;
  }
  63%,
  100% {
    opacity: 0.15;
  }
}

.hero-title {
  margin: 0;
  font-size: clamp(40px, 5.6vw, 74px);
  font-weight: 900;
  line-height: 1.02;
  letter-spacing: 0.04em;
  color: #ffffff;
  text-shadow:
    3px 0 rgba(255, 45, 63, 0.55),
    -3px 0 rgba(34, 211, 238, 0.4),
    0 0 60px rgba(255, 255, 255, 0.08);
}

.hero-sub {
  margin: 14px 0 0;
  font-family: 'Orbitron', var(--ll-mono);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.58em;
  text-indent: 0.58em;
  color: #565e6b;
}

/* 安全通道：与右上角 AUTHED 同语义的"已建立"状态。
   刻意不闪 —— 连接稳定本身就是信息，闪烁等于又在报警。 */
.hero-secure {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin: 16px 0 0;
  font-family: var(--ll-mono);
  font-size: 12px;
  letter-spacing: 0.16em;
  color: var(--ll-acid);
  text-shadow: 0 0 16px rgba(255, 92, 32, 0.3);
}

.secure-dot {
  width: 7px;
  height: 7px;
  background: var(--ll-acid);
  box-shadow:
    0 0 0 1px rgba(255, 92, 32, 0.3),
    0 0 10px rgba(255, 92, 32, 0.85);
}

.secure-bar {
  color: rgba(255, 92, 32, 0.4);
}

.secure-cn {
  letter-spacing: 0.24em;
  color: rgba(255, 92, 32, 0.72);
}

.hero-description {
  margin: 20px 0 0;
  font-size: 15px;
  letter-spacing: 0.12em;
  color: var(--ll-dim);
}

/* ── 金属输入面板 ───────────────────────────────── */
.input-section {
  position: relative;
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 18px 20px 58px;
  transform: rotate(-1.2deg);
  background: linear-gradient(180deg, #1d2025 0%, #16181c 58%, #111316 100%);
  border: 1px solid #2f343c;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.07),
    inset 0 -2px 0 rgba(0, 0, 0, 0.9),
    inset 0 0 70px rgba(0, 0, 0, 0.55),
    0 0 0 1px #08090b,
    0 26px 54px rgba(0, 0, 0, 0.6);
}

/* 扫描环：1px 边框上跑一条橙色光带。
   用"旋转的圆锥渐变 + ring mask"实现，比 background-position 更接近真实巡逻光。 */
.scan-ring {
  position: absolute;
  inset: -1px;
  z-index: 7;
  padding: 1px;
  overflow: hidden;
  pointer-events: none;
  opacity: 0;
  background: rgba(255, 92, 32, 0.22);
  transition: opacity 140ms linear;
  -webkit-mask:
    linear-gradient(#000, #000) content-box,
    linear-gradient(#000, #000);
  -webkit-mask-composite: xor;
  mask:
    linear-gradient(#000, #000) content-box,
    linear-gradient(#000, #000);
  mask-composite: exclude;
}

.scan-ring::before {
  content: '';
  position: absolute;
  left: -60%;
  top: -60%;
  width: 220%;
  height: 220%;
  background: conic-gradient(
    from 0deg,
    transparent 0 50%,
    rgba(255, 92, 32, 0.18) 62%,
    #ff5c20 78%,
    rgba(255, 92, 32, 0.18) 92%,
    transparent 100%
  );
  animation: cpScanSpin 2.6s linear infinite;
}

.input-section:hover {
  border-color: rgba(255, 92, 32, 0.38);
}

.input-section {
  transition: border-color 140ms linear;
}

.send-wrap:hover :deep(.ant-btn-primary) {
  border-color: var(--ll-acid);
}

.input-section:hover .scan-ring,
.input-section:focus-within .scan-ring,
.send-wrap:hover .scan-ring,
.send-wrap:focus-within .scan-ring {
  opacity: 1;
}

@keyframes cpScanSpin {
  to {
    transform: rotate(360deg);
  }
}

.send-wrap {
  position: relative;
  display: inline-flex;
}

.send-wrap .scan-ring {
  inset: -2px;
  z-index: 3;
}

/* 左侧绿色终端光标 */
.prompt-cursor {
  position: absolute;
  top: 18px;
  left: 20px;
  font-family: var(--ll-mono);
  font-size: 20px;
  line-height: 1;
  color: var(--ll-acid);
  text-shadow: 0 0 12px rgba(255, 92, 32, 0.75);
  animation: cpCursorBlink 1.05s steps(1, end) infinite;
  pointer-events: none;
}

@keyframes cpCursorBlink {
  0%,
  49% {
    opacity: 1;
  }
  50%,
  100% {
    opacity: 0.08;
  }
}

.input-section :deep(.prompt-input.ant-input) {
  padding: 0;
  font-family: var(--ll-mono);
  font-size: 14.5px;
  line-height: 1.7;
  color: #d7e4da;
  caret-color: var(--ll-acid);
  background: transparent;
  border: none;
  box-shadow: none;
  resize: none;
}

.input-section :deep(.prompt-input.ant-input::placeholder) {
  color: #4c545f;
}

.input-actions {
  position: absolute;
  right: 16px;
  bottom: 16px;
}

/* 发送键：常亮指示灯。哑光金属底 + 酸绿内发光 + 双层描边，
   不用 hover 才亮——通电的设备上，指示灯本来就是亮着的。 */
.input-section :deep(.ant-btn-primary) {
  border-radius: 0;
  border: 1px solid rgba(255, 92, 32, 0.55);
  background: linear-gradient(180deg, #232a24 0%, #141914 100%);
  color: #eafff0;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.12),
    inset 0 0 20px rgba(255, 92, 32, 0.18),
    inset 0 -2px 0 rgba(0, 0, 0, 0.7),
    0 0 0 1px #07080a,
    0 0 0 2px rgba(255, 92, 32, 0.3),
    0 0 20px rgba(255, 92, 32, 0.22);
  transition: none;
}

.input-section :deep(.ant-btn-primary) {
  font-size: 18px;
}

.input-section :deep(.ant-btn-primary:hover) {
  color: #ffffff;
  border-color: rgba(255, 92, 32, 0.9);
  background: linear-gradient(180deg, #2b342d 0%, #171d17 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.16),
    inset 0 0 26px rgba(255, 92, 32, 0.26),
    inset 0 -2px 0 rgba(0, 0, 0, 0.7),
    0 0 0 1px #07080a,
    0 0 0 2px rgba(255, 92, 32, 0.5),
    0 0 28px rgba(255, 92, 32, 0.4);
}

/* 四角螺丝：让它看起来是被螺丝拧在机箱上的 */
.panel-screw {
  position: absolute;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #6b7480, #23272d 60%, #0c0e11 100%);
  box-shadow:
    0 0 0 1px #0a0c0e,
    0 1px 2px rgba(0, 0, 0, 0.9);
}

.panel-screw--tl {
  top: 8px;
  left: 9px;
}

.panel-screw--tr {
  top: 8px;
  right: 9px;
}

.panel-screw--bl {
  bottom: 8px;
  left: 9px;
}

.panel-screw--br {
  bottom: 8px;
  right: 9px;
}

.panel-serial {
  position: absolute;
  right: 14px;
  bottom: -22px;
  font-family: var(--ll-mono);
  font-size: 9px;
  letter-spacing: 0.22em;
  color: #363d47;
}

/* 0.2s 横向故障：只在边框上错位，不动输入内容 */
.panel-glitch {
  position: absolute;
  inset: -1px;
  border: 1px solid var(--ll-acid);
  opacity: 0;
  pointer-events: none;
  animation: cpFrameGlitch 8.2s steps(1, end) infinite;
}

@keyframes cpFrameGlitch {
  0%,
  60.9% {
    opacity: 0;
    transform: translateX(0);
    clip-path: inset(0 0 0 0);
  }
  61% {
    opacity: 0.55;
    transform: translateX(-6px);
    clip-path: inset(14% 0 66% 0);
  }
  62% {
    opacity: 0.5;
    transform: translateX(7px);
    clip-path: inset(48% 0 28% 0);
  }
  62.6% {
    opacity: 0.4;
    transform: translateX(-3px);
    clip-path: inset(76% 0 6% 0);
  }
  63.4% {
    opacity: 0;
  }
  100% {
    opacity: 0;
  }
}

/* ── 快捷标签：旧磁带 / 散落芯片 ─────────────────── */
.quick-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 14px;
  margin: 30px auto 0;
}

.quick-chip {
  position: relative;
  height: 34px;
  padding: 0 14px 0 44px;
  border-radius: 0;
  font-family: var(--ll-mono);
  font-size: 12px;
  letter-spacing: 0.08em;
  color: #c3ccd6;
  background: #17181c;
  border: 2px solid #343a42;
  box-shadow:
    inset 0 0 0 1px #0b0d10,
    inset 0 2px 0 rgba(255, 255, 255, 0.05),
    3px 3px 0 rgba(0, 0, 0, 0.55);
  transform: rotate(var(--tilt, 0deg)) translateY(var(--dy, 0));
  transition: none;
}

/* 磁带窗口：让标签像从旧设备上撕下来的 */
.quick-chip::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 10px;
  width: 22px;
  height: 8px;
  transform: translateY(-50%);
  background: repeating-linear-gradient(90deg, #07080a 0 4px, #20252b 4px 6px);
  border: 1px solid #07080a;
}

.quick-chip:hover,
.quick-chip:focus-visible {
  color: #ffe3d2;
  background: #201614;
  border-color: var(--ll-acid);
  box-shadow:
    inset 0 0 0 1px #0b0d10,
    0 0 0 1px rgba(255, 92, 32, 0.6),
    0 0 18px rgba(255, 92, 32, 0.4),
    3px 3px 0 rgba(0, 0, 0, 0.55);
  animation: cpChipShake 0.14s steps(2, end) infinite;
}

/* 按下等于"插上这块模板"，给一次实心的强调 */
.quick-chip:active {
  color: #100a06;
  background: var(--ll-acid);
  border-color: var(--ll-acid);
}

@keyframes cpChipShake {
  0% {
    transform: rotate(var(--tilt, 0deg)) translateY(var(--dy, 0)) translate(0, 0);
  }
  50% {
    transform: rotate(var(--tilt, 0deg)) translateY(var(--dy, 0)) translate(-1px, 1px);
  }
  100% {
    transform: rotate(var(--tilt, 0deg)) translateY(var(--dy, 0)) translate(1px, -1px);
  }
}

.chip-index {
  margin-right: 7px;
  font-size: 9px;
  color: #4a525d;
  vertical-align: 2px;
}

.quick-chip:hover .chip-index,
.quick-chip:focus-visible .chip-index {
  color: var(--ll-acid);
}

.quick-chip:active .chip-index {
  color: #100a06;
}

/* ── 分区标题 ───────────────────────────────────── */
.section {
  margin-top: 64px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 20px;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.2em;
  color: var(--ll-ink);
}

.section-index {
  font-family: var(--ll-mono);
  font-size: 11px;
  font-weight: 400;
  letter-spacing: 0.1em;
  color: var(--ll-tape);
}

.section-title::after {
  content: '';
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0));
}

.app-grid,
.featured-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 26px;
}

@media (max-width: 900px) {
  .tape--panel-a {
    top: -24px;
    right: -14px;
  }

  .tape--panel-b {
    bottom: -28px;
    left: -14px;
  }

  .input-section {
    padding-left: 46px;
    transform: rotate(-0.8deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .panel-glitch,
  .hero-secure,
  .kicker-dot,
  .prompt-cursor,
  .quick-chip:hover {
    animation: none !important;
  }

  }
</style>

<style>
/*
 * 首页专属的"低生活"底色：盖在整个视口上，而不是只盖内容块，
 * 这样内容多短都不会在底部露出一条接缝。挂载时由 HomePage 给 <html> 加 .home-mode。
 */
/* 通电闪烁：挂在 #app 上，页头与内容一起闪（整机通电，而不是只有屏幕亮） */
html.home-boot #app {
  animation: cpBootFlicker 1.5s steps(1, end) both;
}

@keyframes cpBootFlicker {
  0% {
    opacity: 0;
    filter: brightness(2.4) contrast(1.2);
  }
  9% {
    opacity: 0.55;
  }
  13% {
    opacity: 0.04;
  }
  21% {
    opacity: 0.86;
    filter: brightness(1.4);
  }
  26% {
    opacity: 0.12;
  }
  33% {
    opacity: 1;
  }
  39% {
    opacity: 0.42;
  }
  46% {
    opacity: 1;
    filter: brightness(1.05);
  }
  52% {
    opacity: 0.68;
  }
  58% {
    opacity: 1;
  }
  100% {
    opacity: 1;
    filter: none;
  }
}

html.home-mode body {
  background: #131418;
}

html.home-mode body::before {
  opacity: 1;
  background-image:
    repeating-linear-gradient(
      0deg,
      rgba(0, 0, 0, 0.12) 0 1px,
      rgba(0, 0, 0, 0) 1px 4px
    ),
    radial-gradient(130% 105% at 50% -6%, rgba(0, 0, 0, 0) 42%, rgba(0, 0, 0, 0.62) 100%),
    repeating-linear-gradient(
      0deg,
      rgba(255, 255, 255, 0.035) 0 1px,
      transparent 1px 64px
    ),
    repeating-linear-gradient(
      90deg,
      rgba(255, 255, 255, 0.035) 0 1px,
      transparent 1px 64px
    ),
    repeating-linear-gradient(
      0deg,
      rgba(255, 255, 255, 0.05) 0 1px,
      transparent 1px 320px
    ),
    repeating-linear-gradient(
      90deg,
      rgba(255, 255, 255, 0.05) 0 1px,
      transparent 1px 320px
    ),
    radial-gradient(1000px 520px at 50% -8%, rgba(255, 46, 151, 0.07), transparent 72%),
    radial-gradient(900px 480px at 80% 20%, rgba(34, 211, 238, 0.05), transparent 70%);
}

/* 老显示器噪点：用 SVG 湍流做一张可平铺的噪声图，靠 steps 动画抖动 */
html.home-mode body::after {
  content: '';
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0.05;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='160' height='160'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3' stitchTiles='stitch'/></filter><rect width='160' height='160' filter='url(%23n)'/></svg>");
  animation: cpNoiseShift 0.7s steps(5, end) infinite;
}

@keyframes cpNoiseShift {
  0% {
    transform: translate3d(0, 0, 0);
  }
  20% {
    transform: translate3d(-3%, 2%, 0);
  }
  40% {
    transform: translate3d(2%, -3%, 0);
  }
  60% {
    transform: translate3d(-2%, -2%, 0);
  }
  80% {
    transform: translate3d(3%, 1%, 0);
  }
  100% {
    transform: translate3d(0, 0, 0);
  }
}

@media (prefers-reduced-motion: reduce) {
  html.home-mode body::after {
    animation: none;
  }
}

/* 应用卡片「查看日志」弹层（a-modal 默认挂到 body，样式必须写在全局块） */
.ac-log {
  font-family: var(--cp-font-mono);
  font-size: 11px;
  letter-spacing: 0.06em;
  color: #8b97a4;
}

.ac-log__head {
  margin: 0 0 14px;
  font-size: 10px;
  letter-spacing: 0.2em;
  color: #ff8a5c;
}

.ac-log__head span {
  color: #e6edf3;
}

.ac-log__row {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 10px;
  padding: 6px 0;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.07);
}

.ac-log__row span {
  color: #4f5a66;
  letter-spacing: 0.18em;
}

.ac-log__row b {
  font-weight: 400;
  color: #c7d3e0;
  word-break: break-all;
}

.ac-log__tip {
  margin: 14px 0 0;
  font-size: 10px;
  color: #4f5a66;
}
</style>

<style>
/* ---------- 精选案例空状态（终端留白，避免裸空） ---------- */
.cp-empty {
  position: relative;
  margin: 4px 0 8px;
  padding: 34px 20px 30px;
  border: 1px solid rgba(255, 92, 32, 0.16);
  background:
    linear-gradient(rgba(255, 92, 32, 0.018), rgba(255, 92, 32, 0.018)),
    repeating-linear-gradient(0deg, rgba(255, 255, 255, 0.012) 0 1px, transparent 1px 4px);
  text-align: center;
}

/* 四角只留左上、右下两条短刻线，比整圈括号更克制 */
.cp-empty::before,
.cp-empty::after {
  content: '';
  position: absolute;
  width: 18px;
  height: 18px;
  pointer-events: none;
}

.cp-empty::before {
  top: -1px;
  left: -1px;
  border-top: 1px solid var(--cp-accent);
  border-left: 1px solid var(--cp-accent);
}

.cp-empty::after {
  right: -1px;
  bottom: -1px;
  border-right: 1px solid var(--cp-accent);
  border-bottom: 1px solid var(--cp-accent);
}

.cp-empty__tag {
  position: absolute;
  top: -1px;
  right: 12px;
  transform: translateY(-50%);
  padding: 0 6px;
  background: var(--cp-bg-base);
  font-family: var(--cp-font-mono);
  font-size: 9px;
  letter-spacing: 0.2em;
  color: rgba(255, 92, 32, 0.72);
}

.cp-empty__title {
  margin: 0;
  font-family: var(--cp-font-mono);
  font-size: 13px;
  letter-spacing: 0.24em;
  color: #8d98a5;
}

.cp-empty__hint {
  margin: 12px 0 0;
  font-family: var(--cp-font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  color: #4b545f;
}
</style>
