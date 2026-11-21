<template>
  <article
    class="ac"
    :class="[`ac--${cardStatus}`, { 'ac--featured': featured }]"
    role="link"
    tabindex="0"
    :aria-label="`打开应用对话：${app.appName || 'UNTITLED APP'}`"
    @click="openChat"
    @keydown.enter.prevent="openChat"
    @keydown.space.prevent="openChat"
  >
    <!-- 悬停扫描边框：独立 1px 环，不参与布局 -->
    <span class="ac__ring" aria-hidden="true"></span>

    <div class="ac__preview" :class="[`ac__preview--${cardStatus}`, { 'ac__preview--empty': !showCover }]">
      <!-- 预览永远是真实 <img>：有截图就用截图，没有就用透明像素让容器背景（网格 + 扫描线）透出来 -->
      <img
        class="ac__img"
        :src="coverSrc"
        :alt="app.cover ? `${app.appName || '应用'} 预览图` : `${app.appName || '应用'} 暂无截图`"
        loading="lazy"
        decoding="async"
        @error="onCoverError"
      />

      <span v-if="!showCover" class="ac__ghost">
        {{ cardStatus === 'failed' ? 'NO SIGNAL' : cardStatus === 'completed' ? 'NO PREVIEW' : 'AWAITING RENDER' }}
        <i v-if="cardStatus === 'generating'" class="ac__ghost-cur" aria-hidden="true">_</i>
      </span>

      <!-- 遮罩：三种状态统一出现；本身不吃点击，只有按钮吃 -->
      <div class="ac__veil">
        <div class="ac__acts">
          <template v-if="cardStatus === 'completed'">
            <button class="ac__btn" type="button" @click.stop="emit('view-chat', app.id)">
              [ 查看对话 ]
            </button>
            <button
              v-if="app.deployKey"
              class="ac__btn"
              type="button"
              @click.stop="emit('view-work', app)"
            >
              [ 查看作品 ]
            </button>
          </template>
          <template v-else-if="cardStatus === 'generating'">
            <button class="ac__btn" type="button" @click.stop="emit('view-chat', app.id)">
              [ 查看对话 ]
            </button>
          </template>
          <template v-else>
            <button class="ac__btn ac__btn--err" type="button" @click.stop="emit('view-log', app)">
              [ 查看日志 ]
            </button>
            <button class="ac__btn" type="button" @click.stop="emit('regenerate', app)">
              [ 重新生成 ]
            </button>
          </template>
        </div>
      </div>

      <span class="ac__status" :class="`ac__status--${cardStatus}`">{{ statusText }}</span>
    </div>

    <div class="ac__body">
      <h3 class="ac__title" :title="app.appName || ''">{{ app.appName || 'UNTITLED APP' }}</h3>
      <p class="ac__prompt" :title="app.initPrompt || ''">
        {{ app.initPrompt || '— 无提示词记录 —' }}
      </p>
    </div>

    <footer class="ac__foot">
      <span class="ac__author">
        <span class="ac__ava" aria-hidden="true">
          <img v-if="app.user?.userAvatar" :src="app.user.userAvatar" alt="" />
          <template v-else>{{ authorInitial }}</template>
        </span>
        <span class="ac__name">{{ authorName }}</span>
      </span>
      <span class="ac__meta">{{ genLabel }}</span>
    </footer>
  </article>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

type AppCardStatus = 'completed' | 'generating' | 'failed'

interface Props {
  app: API.AppVO
  featured?: boolean
  /** 外部可强制状态；不传则由「是否有截图」兜底推导 */
  status?: AppCardStatus
}

const props = withDefaults(defineProps<Props>(), {
  featured: false,
})

const emit = defineEmits<{
  (e: 'view-chat', appId: string | number | undefined): void
  (e: 'view-work', app: API.AppVO): void
  (e: 'view-log', app: API.AppVO): void
  (e: 'regenerate', app: API.AppVO): void
}>()

// 1×1 透明 GIF：无截图时让 <img> 占位存在，容器背景透出来
const BLANK_PIXEL =
  'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7'

// 截图 URL 失效时同样落到占位态，而不是露出浏览器的破图图标
const imgBroken = ref(false)
const showCover = computed(() => Boolean(props.app.cover) && !imgBroken.value)
const coverSrc = computed(() => (showCover.value ? (props.app.cover as string) : BLANK_PIXEL))
const onCoverError = () => {
  imgBroken.value = true
}
watch(
  () => props.app.cover,
  () => {
    imgBroken.value = false
  },
)

const cardStatus = computed<AppCardStatus>(
  () => props.status ?? (props.app.cover ? 'completed' : 'generating'),
)

const statusText = computed(() => cardStatus.value.toUpperCase())

const openChat = () => emit('view-chat', props.app.id)

const authorName = computed(() => {
  const name = (props.app.user?.userName || '').trim()
  return name || 'ANONYMOUS'
})

const authorInitial = computed(() => {
  const name = authorName.value
  return name === 'ANONYMOUS' ? '?' : name.slice(0, 1).toUpperCase()
})

const GEN_LABELS: Record<string, string> = {
  html: 'HTML',
  multi_file: 'MULTI-FILE',
  vue_project: 'VUE PROJECT',
}

const genLabel = computed(() => {
  const type = props.app.codeGenType || ''
  return GEN_LABELS[type] || type.toUpperCase() || '—'
})
</script>

<style scoped>
.ac {
  --ac-line: #333;
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #0f1317;
  border: 1px solid var(--ac-line);
  border-radius: 2px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 160ms linear;
}

.ac:hover,
.ac:focus-visible,
.ac:focus-within {
  border-color: rgba(255, 92, 32, 0.42);
  outline: none;
}

/* 1px 扫描环：父级用 mask 挖出 1px 边，子级旋转圆锥渐变 */
.ac__ring {
  position: absolute;
  inset: 0;
  z-index: 4;
  padding: 1px;
  pointer-events: none;
  opacity: 0;
  background: rgba(255, 92, 32, 0.22);
  transition: opacity 160ms linear;
  -webkit-mask:
    linear-gradient(#000 0 0) content-box,
    linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask:
    linear-gradient(#000 0 0) content-box,
    linear-gradient(#000 0 0);
  mask-composite: exclude;
}

.ac__ring::before {
  content: '';
  position: absolute;
  inset: -50%;
  background: conic-gradient(
    from 0deg,
    rgba(255, 92, 32, 0) 0deg,
    rgba(255, 92, 32, 0.95) 32deg,
    rgba(255, 92, 32, 0.35) 74deg,
    rgba(255, 92, 32, 0) 104deg,
    rgba(255, 92, 32, 0) 360deg
  );
  animation: acScan 2.4s linear infinite;
}

.ac:hover .ac__ring,
.ac:focus-visible .ac__ring,
.ac:focus-within .ac__ring {
  opacity: 1;
}

@keyframes acScan {
  to {
    transform: rotate(1turn);
  }
}

.ac__preview {
  position: relative;
  aspect-ratio: 16 / 9;
  background: #12161b;
  overflow: hidden;
}

/* 无截图时的容器背景：深色赛博网格 + 细扫描线 + 顶部冷光 */
.ac__preview--empty {
  background-color: #0b0e12;
  background-image:
    linear-gradient(rgba(255, 92, 32, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 92, 32, 0.07) 1px, transparent 1px),
    repeating-linear-gradient(0deg, rgba(255, 255, 255, 0.022) 0 1px, transparent 1px 4px),
    radial-gradient(120% 85% at 50% 0%, rgba(255, 92, 32, 0.07), transparent 62%);
  background-size:
    24px 24px,
    24px 24px,
    100% 4px,
    100% 100%;
}

.ac--failed .ac__preview--empty {
  background-color: #0d0a0b;
  background-image:
    linear-gradient(rgba(255, 70, 70, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 70, 70, 0.08) 1px, transparent 1px),
    repeating-linear-gradient(0deg, rgba(255, 255, 255, 0.02) 0 1px, transparent 1px 4px),
    radial-gradient(120% 85% at 50% 0%, rgba(255, 70, 70, 0.08), transparent 62%);
  background-size:
    24px 24px,
    24px 24px,
    100% 4px,
    100% 100%;
}

.ac__img {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: saturate(0.92) contrast(1.03);
}

.ac__preview--empty .ac__img {
  opacity: 0;
}

/* 生成中：一条 1px 扫描线缓慢下移，暗示"还在跑" */
.ac__preview--generating::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 92, 32, 0.6),
    rgba(255, 92, 32, 0.6),
    transparent
  );
  animation: acPhScan 2.6s linear infinite;
}

@keyframes acPhScan {
  0% {
    top: 16%;
    opacity: 0;
  }
  14% {
    opacity: 1;
  }
  86% {
    opacity: 1;
  }
  100% {
    top: 84%;
    opacity: 0;
  }
}

.ac__ghost {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-family: var(--cp-font-mono);
  font-size: 9.5px;
  letter-spacing: 0.3em;
  color: rgba(255, 255, 255, 0.34);
  text-transform: uppercase;
  pointer-events: none;
  transition: opacity 160ms linear;
}

.ac--failed .ac__ghost {
  color: rgba(255, 110, 110, 0.62);
}

/* 悬停时遮罩 + 按钮会盖在占位文字上，淡出避免两层文字打架 */
.ac:hover .ac__ghost,
.ac:focus-visible .ac__ghost,
.ac:focus-within .ac__ghost {
  opacity: 0;
}

.ac__ghost-cur {
  font-style: normal;
  animation: acCursor 1s steps(1, end) infinite;
}

@keyframes acCursor {
  0%,
  50% {
    opacity: 1;
  }
  51%,
  100% {
    opacity: 0;
  }
}

/* 遮罩：三种状态统一出现，但不拦截卡片点击，只有按钮可点 */
.ac__veil {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
  opacity: 0;
  pointer-events: none;
  transition: opacity 200ms linear;
}

.ac:hover .ac__veil,
.ac:focus-visible .ac__veil,
.ac:focus-within .ac__veil {
  opacity: 1;
}

.ac__acts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  padding: 0 10px;
}

.ac__btn {
  appearance: none;
  padding: 6px 10px;
  font-family: var(--cp-font-mono);
  font-size: 10px;
  letter-spacing: 0.14em;
  line-height: 1;
  color: #ff9a6b;
  background: transparent;
  border: 1px solid rgba(255, 92, 32, 0.75);
  border-radius: 0;
  cursor: pointer;
  pointer-events: none;
  transition: none;
}

.ac:hover .ac__btn,
.ac:focus-visible .ac__btn,
.ac:focus-within .ac__btn {
  pointer-events: auto;
}

.ac__btn:hover,
.ac__btn:focus-visible {
  color: #0b0b0b;
  background: var(--cp-accent);
  border-color: var(--cp-accent);
  outline: none;
}

.ac__btn--err {
  color: #ff8a8a;
  border-color: rgba(255, 70, 70, 0.75);
}

.ac__btn--err:hover,
.ac__btn--err:focus-visible {
  color: #120606;
  background: #ff4d4d;
  border-color: #ff4d4d;
}

.ac__status {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 5;
  padding: 4px 6px;
  font-size: 9px;
  line-height: 1;
  letter-spacing: 0.18em;
  color: #ff8a5c;
  background: rgba(6, 8, 10, 0.82);
  border: 1px solid rgba(255, 92, 32, 0.55);
  pointer-events: none;
}

.ac__status--generating {
  animation: acBlink 1.6s steps(1, end) infinite;
}

.ac__status--failed {
  color: #ff7070;
  border-color: rgba(255, 64, 64, 0.6);
}

@keyframes acBlink {
  0%,
  64% {
    opacity: 1;
  }
  65%,
  80% {
    opacity: 0.25;
  }
  81%,
  100% {
    opacity: 1;
  }
}

.ac__body {
  padding: 12px 12px 10px;
}

.ac__title {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--cp-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ac__prompt {
  margin: 6px 0 0;
  font-size: 10.5px;
  line-height: 1.5;
  color: #6d7884;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ac__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  padding: 8px 12px 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.ac__author {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

.ac__ava {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  overflow: hidden;
  font-size: 9px;
  color: #9fb0c3;
  background: #171c22;
  border: 1px solid rgba(255, 255, 255, 0.16);
}

.ac__ava img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ac__name {
  color: #8b97a4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ac__meta {
  flex-shrink: 0;
  color: #4f5a66;
}

@media (prefers-reduced-motion: reduce) {
  .ac__ring::before,
  .ac__preview--generating::after,
  .ac__status--generating,
  .ac__ghost-cur {
    animation: none;
  }
}
</style>