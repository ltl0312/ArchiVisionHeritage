<template>
  <div class="zhixi-page animate-fade-in">
    <!-- 状态胶囊 -->
    <div class="status-capsule">
      <el-icon :size="16" class="capsule-icon"><MagicStick /></el-icon>
      <span class="capsule-text">VGGT 智析引擎</span>
      <span class="capsule-divider">|</span>
      <span class="capsule-text">每日限 5 次解析</span>
    </div>

    <!-- 头部 -->
    <header class="page-header">
      <h2 class="header-title">古建智析</h2>
      <p class="header-sub">上传影像，VGGT 视觉引擎将为您解构建筑的骨骼与历史的脉络。</p>
    </header>

    <div class="zhixi-layout">
      <!-- 上传区 -->
      <div class="upload-section">
        <div class="upload-zone glass-card" @click="triggerUpload" :class="{ analyzing }">
          <template v-if="!result && !analyzing">
            <div class="upload-icon-circle">
              <el-icon :size="32"><UploadFilled /></el-icon>
            </div>
            <h3>点击或拖拽上传古建影像</h3>
            <p class="upload-hint">支持 JPG, PNG, MP4 格式</p>
            <input ref="fileInputRef" type="file" accept="image/*" style="display:none" @change="handleFileSelect" />
            <button class="btn-gradient upload-btn" @click.stop="startAnalyze">开始解析</button>
          </template>

          <!-- 分析中 -->
          <div v-if="analyzing && !result" class="analyzing-state">
            <div class="scan-frame">
              <el-image v-if="uploadedImage" :src="uploadedImage" fit="cover" class="scan-img" />
              <div v-else class="scan-placeholder"></div>
              <div class="scan-line animate-scan"></div>
            </div>
            <p class="analyzing-text">VGGT 引擎正在解构多维特征...</p>
          </div>

          <!-- 结果图 -->
          <el-image v-if="result && !analyzing && uploadedImage" :src="uploadedImage" fit="cover" class="result-img" />
        </div>
      </div>

      <!-- 结果面板 -->
      <div class="result-section" v-if="result">
        <div class="result-card glass-card">
          <h3 class="result-title">
            <el-icon :size="20" class="result-title-icon"><InfoFilled /></el-icon>
            文化肌理报告
          </h3>

          <div class="result-grid">
            <div class="result-item">
              <div class="result-label"><el-icon :size="14"><Grid /></el-icon> 屋顶形制</div>
              <div class="result-value">{{ result.archType }}</div>
            </div>
            <div class="result-item">
              <div class="result-label"><el-icon :size="14"><Clock /></el-icon> 历史断代</div>
              <div class="result-value accent">{{ result.dynasty }}</div>
            </div>
          </div>

          <div class="result-section-title">
            <el-icon :size="14"><Location /></el-icon> 结构特征
          </div>
          <div class="feature-tags">
            <span v-for="f in result.features" :key="f" class="feature-tag">{{ f }}</span>
          </div>

          <div class="result-section-title">文化内涵解析</div>
          <div class="culture-desc">
            <p>{{ result.cultureDesc }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 智能修复预留 -->
    <div class="restore-card glass-card">
      <h4 class="restore-title">
        <el-icon :size="14"><Setting /></el-icon> 扩展延伸方向
      </h4>
      <p class="restore-desc">
        架构已为「古建筑智能修复」预留标准接口。未来可通过导入受损三维资产，结合历史文献图库，实现残损构件的AI推演与补全。
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { MagicStick, UploadFilled, InfoFilled, Grid, Clock, Location, Setting } from '@element-plus/icons-vue'
import { zhixiApi } from '@/api/zhixi'
import { ElMessage } from 'element-plus'

const fileInputRef = ref(null)
const uploadedImage = ref(null)
const selectedFile = ref(null)
const analyzing = ref(false)
const result = ref(null)

function triggerUpload() {
  if (analyzing.value) return
  fileInputRef.value?.click()
}

function handleFileSelect(e) {
  const file = e.target.files?.[0]
  if (!file) return
  selectedFile.value = file
  const reader = new FileReader()
  reader.onload = (ev) => {
    uploadedImage.value = ev.target.result
  }
  reader.readAsDataURL(file)
}

async function startAnalyze() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择一张古建影像')
    return
  }
  analyzing.value = true
  result.value = null
  try {
    const formData = new FormData()
    formData.append('image', selectedFile.value)
    const res = await zhixiApi.analyze(formData)
    result.value = res.data || res
  } catch { /* 拦截器已提示 */ }
  analyzing.value = false
}
</script>

<style scoped>
.zhixi-page {
  height: 100%;
  overflow-y: auto;
  padding: var(--spacing-xl);
  position: relative;
}

/* ═══ 扫描线动画（原 style.css 全局块逐字搬移）═══ */
@keyframes scan {
  0% { transform: translateY(0); opacity: 0; }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% { transform: translateY(380px); opacity: 0; }
}

.animate-scan {
  animation: scan 2s linear infinite;
}

/* ═══ 状态胶囊 ═══ */
.status-capsule {
  position: absolute;
  top: 24px;
  right: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-surface);
  backdrop-filter: blur(10px);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  padding: 8px 18px;
  font-size: 13px;
  color: var(--color-text-sub);
  box-shadow: var(--shadow-card);
  z-index: 5;
}

.capsule-icon { color: var(--color-accent); }
.capsule-text { font-weight: 500; color: var(--color-text-main); }
.capsule-divider { color: var(--color-border); }
.capsule-quota strong { color: var(--color-accent); }

/* ═══ 头部 ═══ */
.page-header { margin-bottom: var(--spacing-xl); max-width: 600px; }
.header-title {
  font-family: var(--font-family-serif);
  font-size: var(--font-size-title);
  color: var(--color-text-main);
  margin-bottom: 4px;
}
.header-sub { font-size: 14px; color: var(--color-text-sub); }

/* ═══ 布局 ═══ */
.zhixi-layout { display: flex; gap: var(--spacing-lg); align-items: flex-start; }
.upload-section { flex: 1; }
.result-section { flex: 1; }

/* ═══ 上传区 ═══ */
.upload-zone {
  min-height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-xl);
  border: 2px dashed var(--color-border);
  cursor: pointer;
  text-align: center;
  transition: all var(--transition-normal);
}

.upload-zone:hover { border-color: var(--color-accent); }

.upload-icon-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--color-bg-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  margin-bottom: var(--spacing-lg);
  transition: transform var(--transition-normal);
}

.upload-zone:hover .upload-icon-circle { transform: scale(1.08); }

.upload-zone h3 { font-size: 16px; color: var(--color-text-main); margin-bottom: 4px; }
.upload-hint { font-size: 13px; color: var(--color-text-muted); }
.upload-btn { margin-top: var(--spacing-lg); padding: 10px 32px; border-radius: var(--radius-full); cursor: pointer; font-size: 15px; }

/* ═══ 分析中 ═══ */
.analyzing-state { width: 100%; display: flex; flex-direction: column; align-items: center; }
.scan-frame {
  width: 260px;
  height: 260px;
  border: 3px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  position: relative;
  margin-bottom: var(--spacing-lg);
  box-shadow: var(--shadow-card);
}

.scan-img { width: 100%; height: 100%; opacity: 0.5; }
.scan-placeholder { width: 100%; height: 100%; background: var(--color-bg-subtle); }
.scan-line {
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
  background: var(--color-accent);
  box-shadow: 0 0 16px var(--color-accent);
}

.analyzing-text {
  color: var(--color-accent);
  font-family: var(--font-family-serif);
  letter-spacing: 1px;
  animation: pulse 1.5s ease-in-out infinite;
  font-weight: 500;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* ═══ 结果图 ═══ */
.result-img { width: 100%; height: 100%; border-radius: var(--radius-2xl); }

/* ═══ 结果面板 ═══ */
.result-card { padding: var(--spacing-xl); }
.result-title {
  font-family: var(--font-family-serif);
  font-size: 22px;
  color: var(--color-text-main);
  margin-bottom: var(--spacing-lg);
  display: flex;
  align-items: center;
  gap: 8px;
}
.result-title-icon { color: var(--color-accent); }

.result-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.result-item {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-md);
}

.result-label {
  font-size: 13px;
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
}

.result-value { font-size: 18px; font-weight: 500; color: var(--color-text-main); }
.result-value.accent { color: var(--color-accent); }

.result-section-title {
  font-size: 13px;
  color: var(--color-text-muted);
  margin-bottom: var(--spacing-sm);
  display: flex;
  align-items: center;
  gap: 4px;
}

.feature-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: var(--spacing-lg); }
.feature-tag {
  padding: 6px 16px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--color-text-main);
}

.culture-desc {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-md);
  font-size: 14px;
  line-height: 1.8;
  color: var(--color-text-sub);
}

/* ═══ 修复预留卡片 ═══ */
.restore-card {
  margin-top: var(--spacing-lg);
  padding: var(--spacing-lg);
}
.restore-title { font-size: 14px; color: var(--color-text-sub); display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }
.restore-desc { font-size: 13px; color: var(--color-text-muted); line-height: 1.6; }

@media (max-width: 768px) {
  .zhixi-layout { flex-direction: column; }
  .status-capsule { position: static; margin-bottom: var(--spacing-md); justify-content: center; }
}
</style>
