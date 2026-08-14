<template>
  <div>
    <div class="comment-input-wrap" v-if="isLoggedIn">
      <el-avatar :size="36" :icon="UserFilled" />
      <div class="comment-field">
        <el-input
          :model-value="modelValue"
          @update:model-value="emit('update:model-value', $event)"
          type="textarea"
          :rows="3"
          placeholder="在此抒发您的见解与共鸣..."
          resize="none"
        />
        <el-button type="primary" size="small" class="comment-submit" :loading="submitting" @click="handleSubmit">
          发表
        </el-button>
      </div>
    </div>

    <div v-if="comments && comments.length" class="comments-list">
      <div v-for="c in comments" :key="c.id" class="comment-item">
        <el-avatar :size="32" :icon="UserFilled" />
        <div class="comment-body">
          <div class="comment-author">{{ c.nickname || '匿名' }}</div>
          <div class="comment-text">{{ c.content }}</div>
          <div class="comment-time">{{ c.createdAt }}</div>
        </div>
      </div>
    </div>

    <div v-if="!loading && (!comments || !comments.length)" class="comments-empty">
      "静水流深，等待第一缕思想的涟漪"
    </div>
  </div>
</template>

<script setup>
import { UserFilled } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  comments: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  isLoggedIn: { type: Boolean, default: false },
  submitting: { type: Boolean, default: false }
})

const emit = defineEmits(['update:model-value', 'submit'])

function handleSubmit() {
  if (!props.modelValue.trim()) return
  emit('submit', props.modelValue)
}
</script>

<style scoped>
/* ═══ 评论区（原 PostDetailView scoped 评论样式逐字搬移）═══ */
.comment-input-wrap {
  display: flex;
  gap: 12px;
  margin-bottom: var(--spacing-lg);
}

.comment-field {
  flex: 1;
  position: relative;
}

.comment-submit {
  position: absolute;
  bottom: 8px;
  right: 8px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: var(--spacing-md) 0;
  border-bottom: 1px solid var(--color-border);
}

.comment-author {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-main);
  margin-bottom: 4px;
}

.comment-text {
  font-size: 14px;
  color: var(--color-text-main);
  line-height: 1.6;
}

.comment-time {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.comments-empty {
  text-align: center;
  padding: 40px;
  color: var(--color-text-muted);
  font-family: var(--font-family-serif);
  font-size: 15px;
}
</style>
