<template>
  <div class="image-uploader">
    <el-upload
      class="uploader-area"
      :show-file-list="false"
      :before-upload="handleUpload"
      :accept="'image/*'"
      drag
    >
      <img v-if="modelValue" :src="modelValue" class="preview-image" />
      <div v-else class="upload-placeholder">
        <el-icon size="32" class="upload-icon"><UploadFilled /></el-icon>
        <p class="upload-text">{{ placeholder }}</p>
        <p class="upload-hint">支持 JPG, PNG, GIF, WebP 格式，最大 5MB</p>
      </div>
    </el-upload>
    <div class="url-input" v-if="showUrlInput">
      <el-input
        :model-value="modelValue"
        @update:model-value="$emit('update:model-value', $event)"
        placeholder="或输入图片URL"
        clearable
        :prefix-icon="Link"
      />
    </div>
  </div>
</template>

<script setup>
import { Link, UploadFilled } from '@element-plus/icons-vue'
import { uploadApi } from '@/api/upload'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '点击或拖拽上传图片'
  },
  showUrlInput: {
    type: Boolean,
    default: true
  },
  subDir: {
    type: String,
    default: 'images'
  }
})

const emit = defineEmits(['update:model-value'])

async function handleUpload(file) {
  try {
    const res = await uploadApi.uploadImage(file, props.subDir)
    emit('update:model-value', res.data.url)
    ElMessage.success('上传成功')
  } catch { /* 拦截器已提示 */ }
  // 阻止 el-upload 默认上传行为
  return false
}
</script>

<style scoped>
.image-uploader {
  width: 100%;
}

.uploader-area {
  width: 100%;
}

.uploader-area :deep(.el-upload) {
  width: 100%;
}

.uploader-area :deep(.el-upload-dragger) {
  width: 100%;
  padding: var(--spacing-lg);
  border-radius: var(--radius-lg);
  border: 2px dashed var(--color-border);
  background: var(--color-bg-subtle);
  transition: all var(--transition-fast);
}

.uploader-area :deep(.el-upload-dragger:hover) {
  border-color: var(--color-accent);
  background: var(--color-accent-soft);
}

.preview-image {
  max-width: 100%;
  max-height: 200px;
  object-fit: contain;
  border-radius: var(--radius-md);
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-sm);
}

.upload-icon {
  color: var(--color-text-muted);
}

.upload-text {
  font-size: 14px;
  color: var(--color-text-sub);
  margin: 0;
}

.upload-hint {
  font-size: 12px;
  color: var(--color-text-muted);
  margin: 0;
}

.url-input {
  margin-top: var(--spacing-sm);
}
</style>
