<template>
  <div class="zhixi-page">
    <div class="page-header">
      <h1>
        <el-icon size="28"><Search /></el-icon>
        古建智析 · VGGT 结构解析
      </h1>
      <p>上传古建图片，深度解析营造法式之奥秘</p>
    </div>

    <!-- 每日次数限制提示 -->
    <el-alert
      title="为保障算力资源，VGGT 深度结构解析每日限调用 5 次。请在提交前确认图片清晰度以获得最佳的解析效果。"
      type="warning"
      :closable="false"
      show-icon
      class="rate-limit-alert"
    />

    <!-- 上传区域 -->
    <div class="upload-section">
      <el-upload
        class="upload-area"
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="1"
        accept="image/*"
      >
        <div class="upload-content" v-if="!selectedFile">
          <el-icon size="48" color="#d73c37"><UploadFilled /></el-icon>
          <p>拖拽或点击上传古建图片</p>
          <p class="upload-hint">支持 JPG / PNG，建议分辨率 512x512 以上</p>
        </div>
        <div v-else class="upload-preview">
          <img :src="previewUrl" alt="预览" />
          <p class="file-name">{{ selectedFile.name }}</p>
        </div>
      </el-upload>

      <el-button
        type="primary"
        size="large"
        :loading="analyzing"
        :disabled="!selectedFile"
        @click="handleAnalyze"
        style="margin-top: 16px; width: 100%"
      >
        <el-icon><Search /></el-icon>
        开始结构解析
      </el-button>
    </div>

    <!-- 解析结果展示 -->
    <div v-if="result" class="result-section">
      <h2 class="result-title">
        <el-icon size="20"><DataAnalysis /></el-icon>
        {{ result.title }}
      </h2>

      <!-- 转译后的通俗文化解读 -->
      <div class="insight-cards" v-if="parsedElements.length">
        <div
          v-for="(elem, idx) in parsedElements"
          :key="idx"
          class="insight-card"
          @mouseenter="hoveredElement = idx"
          @mouseleave="hoveredElement = null"
        >
          <div class="insight-header">
            <span class="ontology-badge" :style="{ borderColor: '#2D9B5C' }">
              {{ elem.ontology }}
            </span>
            <el-tag size="small" type="success">置信度: {{ (elem.confidence * 100).toFixed(0) }}%</el-tag>
          </div>

          <div class="bounding-box-info">
            <span class="bb-label">定位区域:</span>
            <code>[{{ elem.boundingBox?.join(', ') }}]</code>
          </div>

          <!-- 悬浮文化解读 Tooltip -->
          <el-tooltip placement="right" :visible="hoveredElement === idx">
            <template #content>
              <div style="max-width:280px; line-height:1.7;">
                {{ elem.culturalInsight }}
              </div>
            </template>
            <p class="cultural-insight-text">{{ elem.culturalInsight }}</p>
          </el-tooltip>
        </div>
      </div>

      <!-- 原始JSON（可折叠） -->
      <el-collapse style="margin-top:16px">
        <el-collapse-item title="查看原始解析数据 (JSON)">
          <pre class="json-preview">{{ JSON.stringify(result, null, 2) }}</pre>
        </el-collapse-item>
      </el-collapse>
    </div>

    <!-- 演示数据列表 -->
    <div class="demo-section" v-if="demos.length">
      <h3>演示解析案例</h3>
      <div class="demo-list">
        <el-card v-for="demo in demos" :key="demo.id" class="demo-card"
                 @click="loadDemo(demo)" :class="{ active: result?.id === demo.id }">
          <el-icon size="20"><FolderOpened /></el-icon>
          <span>{{ demo.title }}</span>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, UploadFilled, DataAnalysis, FolderOpened } from '@element-plus/icons-vue'
import { zhixiApi } from '@/api/zhixi'

const selectedFile = ref(null)
const previewUrl = ref('')
const analyzing = ref(false)
const result = ref(null)
const demos = ref([])
const hoveredElement = ref(null)

const parsedElements = computed(() => {
  if (!result.value) return []
  try {
    const json = typeof result.value.mockJsonData === 'string'
      ? JSON.parse(result.value.mockJsonData)
      : result.value.mockJsonData
    return json.structural_elements || []
  } catch {
    return []
  }
})

onMounted(async () => {
  try {
    const res = await zhixiApi.getDemos()
    demos.value = res.data || []
  } catch { /* ignore */ }
})

function handleFileChange(file) {
  selectedFile.value = file.raw
  previewUrl.value = URL.createObjectURL(file.raw)
}

async function handleAnalyze() {
  if (!selectedFile.value) return
  analyzing.value = true
  try {
    const formData = new FormData()
    formData.append('image', selectedFile.value)
    const res = await zhixiApi.analyze(formData)
    result.value = res.data
    ElMessage.success('结构解析完成')
  } catch {
    // 拦截器已处理
  } finally {
    analyzing.value = false
  }
}

function loadDemo(demo) {
  result.value = demo
}
</script>

<style scoped>
.zhixi-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  text-align: center;
  padding: 40px 0 24px;
}

.page-header h1 {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 28px;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.page-header p {
  color: var(--color-text-secondary);
  font-size: 15px;
}

.rate-limit-alert {
  margin-bottom: 24px;
  background: rgba(215,60,55,0.06);
  border-color: rgba(215,60,55,0.2);
}

.upload-section {
  background: white;
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.upload-content { text-align: center; padding: 32px; }
.upload-content p { margin-top: 12px; color: var(--color-text-secondary); }
.upload-hint { font-size: 12px; color: var(--color-text-muted); }

.upload-preview { text-align: center; }
.upload-preview img { max-width: 100%; max-height: 300px; border-radius: var(--radius-sm); }
.file-name { margin-top: 8px; font-size: 13px; color: var(--color-text-secondary); }

.result-section {
  background: white;
  border-radius: var(--radius-lg);
  padding: 24px;
  margin-top: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.result-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  margin-bottom: 16px;
}

.insight-cards { display: flex; flex-direction: column; gap: 12px; }

.insight-card {
  padding: 16px;
  border: 1px solid #e8e4df;
  border-radius: var(--radius-md);
  transition: border-color 0.2s;
}

.insight-card:hover { border-color: var(--color-success); }

.insight-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.ontology-badge {
  padding: 2px 10px;
  border: 2px solid var(--color-success);
  border-radius: 4px;
  font-weight: 600;
  font-size: 14px;
  color: var(--color-success);
}

.bounding-box-info {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}
.bounding-box-info code {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 2px;
  font-size: 12px;
}

.cultural-insight-text {
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-text-primary);
  padding: 8px 12px;
  background: rgba(45,155,92,0.05);
  border-left: 3px solid var(--color-success);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.json-preview {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  overflow-x: auto;
  max-height: 400px;
}

.demo-section {
  margin-top: 24px;
}

.demo-section h3 {
  font-size: 16px;
  margin-bottom: 12px;
  color: var(--color-text-secondary);
}

.demo-list { display: flex; gap: 12px; flex-wrap: wrap; }

.demo-card {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 10px 16px;
  font-size: 14px;
  transition: all 0.2s;
}

.demo-card:hover { border-color: var(--color-primary); }
.demo-card.active { border-color: var(--color-primary); background: rgba(215,60,55,0.05); }
</style>
