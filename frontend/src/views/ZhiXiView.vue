<template>
  <div class="page-container">
    <div class="page-header-center">
      <h1>
        <el-icon size="28"><Search /></el-icon>
        古建智析 · VGGT 结构解析
      </h1>
      <p>上传古建图片，深度解析营造法式之奥秘</p>
    </div>

    <!-- 文化温度提示 — 琉璃金底色 -->
    <div class="cultural-alert">
      <el-icon size="18"><WarningFilled /></el-icon>
      <span>古建高精几何解析犹如匠人雕琢，需耗费大量云端算力。出于对资源的敬畏与合理配置，<strong>平台对单用户实行每日最多 5 次的解析节制</strong>。请选择最具代表性的建筑立面或斗栱细节进行探索。</span>
    </div>

    <!-- 虚线拖拽上传区 — 琉璃金边框 -->
    <div class="upload-section">
      <el-upload
        class="upload-area"
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="1"
        accept="image/*"
      >
        <div class="dashed-upload-zone" v-if="!selectedFile">
          <div class="upload-icon">&#x2302;</div>
          <p class="upload-text">拖拽或点击上传古建图片</p>
          <p class="upload-hint">支持 JPG / PNG，建议分辨率 512×512 以上</p>
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
        class="analyze-btn"
      >
        <el-icon><Search /></el-icon>
        开始结构解析
      </el-button>
    </div>

    <!-- VGGT 处理中的古塔构建动画 -->
    <div v-if="analyzing" class="dream-loading">
      <p class="loading-text">古建解析引擎正在推算三维结构...</p>
      <div class="pagoda-building">
        <div class="pagoda-layer" v-for="i in 6" :key="i"></div>
      </div>
      <p class="loading-sub">榫卯交错，匠心独运</p>
    </div>

    <!-- 解析结果展示 -->
    <div v-if="result" class="result-section glass-panel">
      <h2 class="result-title">
        <el-icon size="20"><DataAnalysis /></el-icon>
        {{ result.title || '结构解析结果' }}
      </h2>

      <!-- 转译后的文化解读卡片 -->
      <div class="insight-cards" v-if="parsedElements.length">
        <div
          v-for="(elem, idx) in parsedElements"
          :key="idx"
          class="insight-card"
          @mouseenter="hoveredElement = idx"
          @mouseleave="hoveredElement = null"
        >
          <div class="insight-header">
            <span class="ontology-badge">{{ elem.ontology }}</span>
            <el-tag size="small" type="success" effect="plain">
              置信度 {{ (elem.confidence * 100).toFixed(0) }}%
            </el-tag>
          </div>

          <div class="bounding-box-info">
            <span class="bb-label">定位区域</span>
            <code>[{{ elem.boundingBox?.join(', ') }}]</code>
          </div>

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

      <!-- 原始JSON数据 (折叠) -->
      <el-collapse style="margin-top: var(--spacing-md)">
        <el-collapse-item title="查看原始解析数据 (JSON)">
          <pre class="json-preview">{{ JSON.stringify(result, null, 2) }}</pre>
        </el-collapse-item>
      </el-collapse>

      <!-- 预留：AI结构推演与修复入口 (Ghost Button) -->
      <div class="restoration-placeholder">
        <button class="ghost-btn-disabled">
          <el-icon><MagicStick /></el-icon>
          启动 AI 结构推演与修复 (Beta 未开放)
        </button>
        <p class="placeholder-hint">智能修复能力正在深度训练中，敬请期待</p>
      </div>
    </div>

    <!-- 演示数据列表 -->
    <div class="demo-section" v-if="demos.length">
      <h3>演示解析案例</h3>
      <div class="demo-list">
        <el-card
          v-for="demo in demos"
          :key="demo.id"
          class="demo-card"
          shadow="hover"
          :class="{ active: result?.id === demo.id }"
          @click="loadDemo(demo)"
        >
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
import { Search, DataAnalysis, FolderOpened, WarningFilled, MagicStick } from '@element-plus/icons-vue'
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
.upload-section {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-card);
  margin-bottom: var(--spacing-lg);
}

.upload-preview {
  text-align: center;
}

.upload-preview img {
  max-width: 100%;
  max-height: 300px;
  border-radius: var(--radius-sm);
}

.file-name {
  margin-top: var(--spacing-sm);
  font-size: 13px;
  color: var(--color-text-sub);
}

.analyze-btn {
  margin-top: var(--spacing-md);
  width: 100%;
}

.result-section {
  margin-bottom: var(--spacing-lg);
}

.result-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-subtitle);
  font-weight: 600;
  margin-bottom: var(--spacing-md);
  color: var(--color-text-main);
}

.insight-cards {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.insight-card {
  padding: var(--spacing-md);
  border: 1px solid #e8e4df;
  border-radius: var(--radius-md);
  transition: border-color var(--transition-fast);
}

.insight-card:hover {
  border-color: var(--color-accent-jade);
}

.insight-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-sm);
}

.ontology-badge {
  padding: 2px 10px;
  border: 2px solid var(--color-accent-jade);
  border-radius: var(--radius-sm);
  font-weight: 600;
  font-size: 14px;
  color: var(--color-accent-jade);
}

.bounding-box-info {
  font-size: 13px;
  color: var(--color-text-sub);
  margin-bottom: var(--spacing-sm);
}

.bounding-box-info code {
  background: var(--color-bg-base);
  padding: 2px 6px;
  border-radius: 2px;
  font-size: var(--font-size-caption);
}

.cultural-insight-text {
  font-size: 14px;
  line-height: var(--line-height-body);
  color: var(--color-text-main);
  padding: var(--spacing-sm) var(--spacing-md);
  background: rgba(45, 155, 92, 0.05);
  border-left: 3px solid var(--color-accent-jade);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.json-preview {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: var(--spacing-md);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-caption);
  overflow-x: auto;
  max-height: 400px;
}

/* 预留修复入口 */
.restoration-placeholder {
  margin-top: var(--spacing-lg);
  padding-top: var(--spacing-md);
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-sm);
}

.placeholder-hint {
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
}

.demo-section {
  margin-top: var(--spacing-lg);
}

.demo-section h3 {
  font-size: 16px;
  margin-bottom: var(--spacing-sm);
  color: var(--color-text-sub);
}

.demo-list {
  display: flex;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
}

.demo-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  cursor: pointer;
  padding: 10px var(--spacing-md);
  font-size: 14px;
  transition: all var(--transition-fast);
  border-radius: var(--radius-md);
}

.demo-card:hover {
  border-color: var(--color-primary);
}

.demo-card.active {
  border-color: var(--color-primary);
  background: rgba(184, 38, 31, 0.05);
}
</style>
