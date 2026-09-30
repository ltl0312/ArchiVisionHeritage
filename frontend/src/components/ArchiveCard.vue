<template>
  <component
    :is="tag"
    class="v4-arc"
    v-bind="linkProps"
    :type="tag === 'button' ? 'button' : undefined"
    @click="tag === 'button' ? $emit('select') : undefined"
  >
    <figure>
      <img
        v-if="image && !broken"
        :src="image"
        :alt="title"
        :width="width"
        :height="height"
        loading="lazy"
        decoding="async"
        @error="broken = true"
      />
      <div v-else class="arc-ph">
        <el-icon :size="34"><PictureFilled /></el-icon>
      </div>

      <div v-if="tags.length" class="arc-tags">
        <span v-for="t in tags" :key="t" class="arc-tag">{{ t }}</span>
      </div>

      <span v-if="badge" class="arc-badge" :class="`arc-badge--${badgeTone}`">{{ badge }}</span>
    </figure>

    <figcaption>
      <h4>{{ title }}</h4>
      <div class="meta">{{ meta }}</div>
    </figcaption>
  </component>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { PictureFilled } from '@element-plus/icons-vue'

/**
 * 档案卡（D-11 去重）：营造志「精选数字档案」/ 我的档案 / 匠人社区网格 三处共用。
 * 纯展示组件 —— 不发起请求、不持业务状态；导航或选中由父级决定。
 */
const props = defineProps({
  /** 有 to 时渲染为 router-link，否则渲染为 button 并 emit select */
  to: { type: [String, Object], default: null },
  title: { type: String, default: '未命名档案' },
  image: { type: String, default: '' },
  meta: { type: String, default: '' },
  tags: { type: Array, default: () => [] },
  /** 右上角状态角标（如「待审核」） */
  badge: { type: String, default: '' },
  badgeTone: { type: String, default: 'gold' },
  /** 图片原始尺寸提示：用于在图片解码前锁定布局，避免 CLS */
  width: { type: [Number, String], default: 1536 },
  height: { type: [Number, String], default: 1024 }
})

defineEmits(['select'])

const tag = computed(() => (props.to ? 'router-link' : 'button'))
const linkProps = computed(() => (props.to ? { to: props.to } : {}))

/* 图片 404 时回退为占位图，避免破图 */
const broken = ref(false)
watch(() => props.image, () => { broken.value = false })
</script>

<style scoped>
.arc-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-subtle);
  color: var(--color-text-ghost);
}

.arc-tags {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  z-index: 2;
  max-width: calc(100% - 20px);
}

/* 实底 pill（无 backdrop-filter，见 P0-D5） */
.arc-tag {
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 500;
  color: var(--color-text-main);
  background: var(--color-surface-float);
  border: 1px solid var(--color-border);
}

.arc-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: var(--radius-full);
  font-size: 10px;
  font-weight: 500;
}
.arc-badge--gold {
  color: var(--color-accent-text);
  background: var(--color-accent-soft);
  border: 1px solid var(--color-border-gold);
}
.arc-badge--jade {
  color: var(--color-jade-text);
  background: var(--color-jade-soft);
  border: 1px solid var(--color-border-jade);
}
.arc-badge--red {
  color: var(--color-rose-text);
  background: var(--color-rose-soft);
  border: 1px solid var(--color-border-rose);
}
</style>
