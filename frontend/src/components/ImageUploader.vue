<template>
  <div class="image-uploader">
    <el-upload
      class="uploader-area"
      :show-file-list="false"
      :before-upload="handleUpload"
      accept="image/jpeg,image/png,image/gif,image/webp"
      drag
    >
      <img v-if="modelValue" :src="modelValue" class="preview-image" alt="已上传图片预览" />
      <div v-else class="upload-placeholder">
        <el-icon size="32" class="upload-icon"><UploadFilled /></el-icon>
        <p class="upload-text">{{ placeholder }}</p>
        <p class="upload-hint">
          支持 JPG / PNG / GIF / WebP · 最大 {{ maxSizeMB }} MB
          <template v-if="compress"><br />上传前自动压缩至长边 {{ MAX_EDGE }}px（约 300 KB）</template>
        </p>
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
import { compressImage, formatBytes } from '@/utils/imageCompress'

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
  },
  /** 是否在上传前做 canvas 压缩（P3-A）。缩略图/图标类上传可关掉。 */
  compress: {
    type: Boolean,
    default: true
  },
  maxSizeMB: {
    type: Number,
    default: 5
  }
})

const emit = defineEmits(['update:model-value', 'uploaded'])

/** 与 utils/imageCompress.js 保持一致，仅用于提示文案 */
const MAX_EDGE = 1600

const ACCEPTED = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']

async function handleUpload(file) {
  // 1. 前置校验 —— 让提示文案成为真实约束（原实现只写了提示、没有校验）
  if (!ACCEPTED.includes(file.type)) {
    ElMessage.error('仅支持 JPG / PNG / GIF / WebP 格式')
    return false
  }
  if (file.size > props.maxSizeMB * 1024 * 1024) {
    ElMessage.error(`图片不能超过 ${props.maxSizeMB} MB`)
    return false
  }

  // 2. 上传前压缩（从源头治理封面体积，唯一有效的纯前端手段）
  let payload = file
  let saved = 0
  if (props.compress) {
    try {
      const compressed = await compressImage(file)
      if (compressed && compressed.size < file.size) {
        saved = file.size - compressed.size
        payload = compressed
      }
    } catch {
      // 压缩失败不阻塞上传：退化为原图直传
    }
  }

  // 3. 上传
  try {
    const res = await uploadApi.uploadImage(payload, props.subDir)
    emit('update:model-value', res.data.url)
    emit('uploaded', res.data)
    if (saved > 0) {
      ElMessage.success(
        `上传成功 · 已优化 ${formatBytes(file.size)} → ${formatBytes(payload.size)}`
      )
    } else {
      ElMessage.success('上传成功')
    }
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
  line-height: 1.7;
  color: var(--color-text-muted);
  margin: 0;
}

.url-input {
  margin-top: var(--spacing-sm);
}
</style>
