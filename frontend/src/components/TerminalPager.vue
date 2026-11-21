<template>
  <div v-if="total > 0" class="tp">
    <button
      class="tp__nav"
      type="button"
      :disabled="current <= 1"
      aria-label="上一页"
      @click="go(current - 1)"
    >
      &lt;
    </button>

    <span class="tp__meta">共 <b>{{ total }}</b> {{ unit }}</span>
    <span class="tp__sep" aria-hidden="true">|</span>

    <div class="tp__nums">
      <button
        v-for="(page, index) in pages"
        :key="`${page}-${index}`"
        class="tp__num"
        :class="{ 'tp__num--on': page === current, 'tp__num--gap': page === '…' }"
        type="button"
        :disabled="page === '…'"
        @click="typeof page === 'number' && go(page)"
      >
        {{ page === current ? `[ ${page} ]` : page }}
      </button>
    </div>

    <button
      class="tp__nav"
      type="button"
      :disabled="current >= pageCount"
      aria-label="下一页"
      @click="go(current + 1)"
    >
      &gt;
    </button>

    <template v-if="sizes.length">
      <span class="tp__sep" aria-hidden="true">|</span>
      <a-select
        class="tp__size"
        :value="pageSize"
        :options="sizeOptions"
        :dropdown-match-select-width="false"
        popup-class-name="term-select-popup"
        size="small"
        @change="onSizeChange"
      />
      <span class="tp__unit">/ PAGE</span>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  current: number
  total: number
  pageSize?: number
  /** 计数单位，例如「应用」「案例」「条」 */
  unit?: string
  /** 传入后出现每页条数切换（去边框的终端下拉） */
  sizes?: number[]
}

const props = withDefaults(defineProps<Props>(), {
  pageSize: 6,
  unit: '应用',
  sizes: () => [],
})

const emit = defineEmits<{
  (e: 'change', page: number): void
  (e: 'size-change', size: number): void
}>()

const pageCount = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))

const sizeOptions = computed(() => props.sizes.map((size) => ({ value: size, label: `${size}` })))

const pages = computed<(number | '…')[]>(() => {
  const count = pageCount.value
  const cur = props.current
  if (count <= 7) return Array.from({ length: count }, (_, i) => i + 1)

  const wanted = [1, cur - 1, cur, cur + 1, count].filter((n) => n >= 1 && n <= count)
  const sorted = [...new Set(wanted)].sort((a, b) => a - b)
  const out: (number | '…')[] = []
  sorted.forEach((n, i) => {
    if (i > 0 && n - sorted[i - 1] > 1) out.push('…')
    out.push(n)
  })
  return out
})

const go = (page: number) => {
  if (page < 1 || page > pageCount.value || page === props.current) return
  emit('change', page)
}

const onSizeChange = (value: unknown) => {
  emit('size-change', Number(value))
}
</script>

<style scoped>
.tp {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-top: 14px;
  font-family: var(--cp-font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  color: #6b7480;
}

.tp__nav,
.tp__num {
  background: transparent;
  border: none;
  padding: 2px 5px;
  color: inherit;
  font: inherit;
  letter-spacing: inherit;
  cursor: pointer;
  transition: none;
}

.tp__nav:hover:not(:disabled),
.tp__num:hover:not(:disabled) {
  color: var(--cp-accent);
}

.tp__nav:disabled,
.tp__num:disabled {
  color: #2f3740;
  cursor: default;
}

.tp__meta {
  letter-spacing: 0.1em;
  color: #5c6773;
}

.tp__meta b {
  color: #aab4c0;
  font-weight: 500;
}

.tp__sep {
  color: #2b3138;
}

.tp__nums {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.tp__num--on {
  color: var(--cp-accent);
}

.tp__num--gap {
  color: #3f4854;
}

.tp__size {
  min-width: 44px;
}

.tp__size :deep(.ant-select-selector) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  padding: 0 2px !important;
  height: 20px !important;
}

.tp__size :deep(.ant-select-selection-item) {
  font-family: var(--cp-font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  color: #8b95a1;
  line-height: 20px !important;
  text-align: right;
  padding-inline-end: 20px !important;
}

.tp__size :deep(.ant-select-arrow) {
  color: #4b545f;
  font-size: 8px;
  inset-inline-end: 4px;
}

.tp__size:hover :deep(.ant-select-selection-item) {
  color: var(--cp-accent);
}

.tp__unit {
  color: #3f4854;
  letter-spacing: 0.14em;
}
</style>