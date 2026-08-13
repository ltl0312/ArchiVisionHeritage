<template>
  <div class="huanzhu-page animate-fade-in">
    <header class="page-header">
      <h2 class="header-title">一键幻筑</h2>
      <p class="header-sub">输入文字描述，AI 3D 大模型将为您凭空建构历史的殿堂。</p>
    </header>

    <div class="huanzhu-layout">
      <!-- 左侧控制面板 -->
      <div class="control-panel glass-card">
        <div class="prompt-header">
          <span class="prompt-label">构筑灵感</span>
          <button class="enhance-btn" @click="enhancePrompt">
            <el-icon :size="12"><MagicStick /></el-icon>
            智能润色提示词
          </button>
        </div>

        <textarea
          v-model="prompt"
          placeholder="请描述您心中的古建筑，例如：唐代重檐歇山顶大殿..."
          class="prompt-area"
        ></textarea>

        <button
          class="btn-gradient generate-btn"
          :disabled="generating || !prompt"
          @click="handleGenerate"
        >
          <template v-if="generating">
            <el-icon :size="20" class="spin"><Loading /></el-icon>
            正在演算三维拓扑...
          </template>
          <template v-else>
            <el-icon :size="20"><Box /></el-icon>
            开始幻筑
          </template>
        </button>
      </div>

      <!-- 右侧预览区 -->
      <div class="preview-panel">
        <div class="preview-grid-bg"></div>

        <template v-if="!taskResult && !generating">
          <div class="preview-idle">
            <el-icon :size="64" class="idle-icon"><Box /></el-icon>
            <p class="idle-text">等待召唤文明的虚影</p>
          </div>
        </template>

        <template v-if="generating">
          <div class="preview-generating">
            <div class="spinner-ring"></div>
            <p class="generating-text">正在重组榫卯结构...</p>
          </div>
        </template>

        <template v-if="taskResult">
          <div class="preview-result">
            <el-image
              v-if="taskResult.previewPath"
              :src="taskResult.previewPath"
              fit="cover"
              class="result-img"
            />
            <div class="result-overlay">
              <div class="result-actions">
                <button class="result-btn">导出 OBJ/GLTF</button>
                <button class="result-btn accent">推演材质</button>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 扩展方向 -->
    <div class="extend-card glass-card">
      <h4><el-icon :size="14"><Setting /></el-icon> 扩展延伸方向</h4>
      <p>架构已为「古建筑智能修复」预留标准接口。未来可通过导入受损三维资产，结合历史文献图库，实现残损构件的AI推演与补全。</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { MagicStick, Box, Loading, Setting } from '@element-plus/icons-vue'
import { huanzhuApi } from '@/api/huanzhu'
import { ElMessage } from 'element-plus'

const prompt = ref('')
const generating = ref(false)
const taskResult = ref(null)

let pollTimer = null
let pollCount = 0
const MAX_POLL = 60 // 最多轮询 60 次（2分钟）

function enhancePrompt() {
  if (!prompt.value) {
    prompt.value = '一座唐代风格的重檐歇山顶大殿，面阔七间，红柱白墙，斗栱雄大，屹立在苍翠的松林之中，夕阳余晖...'
  } else {
    prompt.value = `[优化] ${prompt.value}，补充细节：木构卯榫清晰可见，覆青灰瓦，基座为汉白玉须弥座，呈现历史的厚重感与皇家气度。`
  }
}

async function handleGenerate() {
  if (!prompt.value) return
  generating.value = true
  taskResult.value = null
  try {
    const res = await huanzhuApi.submit(prompt.value)
    if (res.data?.duplicate) {
      ElMessage.info('检测到相同的幻筑任务已在处理中')
    }
    pollTaskStatus(res.data?.taskId)
  } catch {
    ElMessage.error('任务提交失败')
    generating.value = false
  }
}

async function pollTaskStatus(taskId) {
  if (!taskId) { generating.value = false; return }
  pollCount = 0

  const check = async () => {
    if (++pollCount > MAX_POLL) {
      ElMessage.warning('任务处理超时，请稍后查看通知')
      generating.value = false
      return
    }
    try {
      const res = await huanzhuApi.getTaskStatus(taskId)
      const status = res.data?.status
      if (status === 'SUCCESS') {
        taskResult.value = res.data
        generating.value = false
        return
      }
      if (status === 'FAILED') {
        ElMessage.error('幻筑任务失败，请重试')
        generating.value = false
        return
      }
      pollTimer = setTimeout(check, 2000)
    } catch {
      generating.value = false
    }
  }
  check()
}

onUnmounted(() => {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
})
</script>

<style scoped>
.huanzhu-page {
  height: 100%;
  overflow-y: auto;
  padding: var(--spacing-xl);
}

.huanzhu-page::-webkit-scrollbar { width: 6px; }
.huanzhu-page::-webkit-scrollbar-track { background: transparent; }
.huanzhu-page::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 3px; }

.page-header { margin-bottom: var(--spacing-xl); }
.header-title {
  font-family: var(--font-family-serif);
  font-size: var(--font-size-title);
  color: var(--color-text-main);
  margin-bottom: 4px;
}
.header-sub { font-size: 14px; color: var(--color-text-sub); }

/* ═══ 布局 ═══ */
.huanzhu-layout { display: flex; gap: var(--spacing-lg); min-height: 500px; }

/* ═══ 控制面板 ═══ */
.control-panel {
  width: 380px;
  min-width: 340px;
  display: flex;
  flex-direction: column;
  padding: var(--spacing-xl);
}

.prompt-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-md);
}

.prompt-label { font-size: 15px; font-weight: 500; color: var(--color-text-main); }

.enhance-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border: 1px solid var(--color-accent);
  border-radius: var(--radius-full);
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-size: 11px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.enhance-btn:hover { background: var(--color-accent); color: #FFF; }

.prompt-area {
  flex: 1;
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-md);
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-text-main);
  background: var(--color-bg-subtle);
  resize: none;
  outline: none;
  font-family: var(--font-family-base);
  transition: border-color var(--transition-fast);
}

.prompt-area:focus { border-color: var(--color-accent); }
.prompt-area::placeholder { color: var(--color-text-muted); }

.generate-btn {
  margin-top: var(--spacing-lg);
  width: 100%;
  padding: 14px;
  border-radius: var(--radius-lg);
  font-size: 17px;
  font-family: var(--font-family-serif);
  cursor: pointer;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  letter-spacing: 1px;
}

.generate-btn:disabled {
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  cursor: not-allowed;
  box-shadow: none;
}

/* ═══ 预览面板 ═══ */
.preview-panel {
  flex: 1;
  border-radius: var(--radius-2xl);
  border: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-grid-bg {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0,0,0,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,0,0,0.03) 1px, transparent 1px);
  background-size: 40px 40px;
}

[data-theme="dark"] .preview-grid-bg {
  background-image:
    linear-gradient(rgba(255,255,255,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.03) 1px, transparent 1px);
}

/* 空闲 */
.preview-idle {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
  z-index: 2;
}
.idle-icon { color: var(--color-text-muted); opacity: 0.4; }
.idle-text {
  color: var(--color-text-muted);
  font-family: var(--font-family-serif);
  letter-spacing: 2px;
}

/* 生成中 */
.preview-generating {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-lg);
  z-index: 2;
}
.spinner-ring {
  width: 100px;
  height: 100px;
  border: 4px solid var(--color-accent-soft);
  border-top-color: var(--color-accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.generating-text {
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

/* 结果 */
.preview-result {
  position: absolute;
  inset: 8px;
  z-index: 3;
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.result-img { width: 100%; height: 100%; }
.result-overlay {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  padding: 16px;
  background: linear-gradient(to top, rgba(0,0,0,0.6), transparent);
}
.result-actions { display: flex; gap: 10px; }
.result-btn {
  padding: 8px 18px;
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: var(--radius-md);
  background: rgba(255,255,255,0.1);
  backdrop-filter: blur(8px);
  color: #FFF;
  font-size: 13px;
  cursor: pointer;
  transition: background var(--transition-fast);
}
.result-btn:hover { background: rgba(255,255,255,0.2); }
.result-btn.accent { color: var(--color-accent-light); }

/* ═══ 扩展卡片 ═══ */
.extend-card {
  margin-top: var(--spacing-lg);
  padding: var(--spacing-lg);
}
.extend-card h4 {
  font-size: 14px;
  color: var(--color-text-sub);
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-weight: 500;
}
.extend-card p {
  font-size: 13px;
  color: var(--color-text-muted);
  line-height: 1.6;
}

.spin { animation: spin 1s linear infinite; }

@media (max-width: 768px) {
  .huanzhu-layout { flex-direction: column; }
  .control-panel { width: 100%; min-width: unset; }
  .preview-panel { min-height: 350px; }
}
</style>
