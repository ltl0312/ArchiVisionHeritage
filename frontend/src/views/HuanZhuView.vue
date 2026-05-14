<template>
  <div class="page-container huanzhu-page">
    <div class="page-header-center">
      <h1>
        <el-icon size="28"><MagicStick /></el-icon>
        一键幻筑
      </h1>
      <p>输入古建描述，AI 为您生成精美的 3D 模型</p>
    </div>

    <div class="huanzhu-workspace">
      <!-- 左侧控制区 -->
      <div class="huanzhu-left">
        <div class="input-section">
          <!-- 描述词输入中枢 -->
          <div class="prompt-input-wrapper">
            <label class="input-label">描绘您心中的殿宇</label>
            <el-input
              v-model="prompt"
              type="textarea"
              :rows="4"
              placeholder="例如：唐代风格的重檐歇山顶大殿，配有绿琉璃瓦与朱红色的回廊..."
              :disabled="isPolling"
              class="prompt-textarea"
            />
          </div>

          <!-- 智能提示标签 (Smart Prompt Chips) -->
          <div class="prompt-chips-section">
            <p class="chips-label">文化要素快捷填入</p>

            <div class="chips-group">
              <span class="chips-category">朝代风格</span>
              <div class="chips-row">
                <span class="prompt-chip" @click="appendPrompt('唐代风格，恢弘大气')">唐代</span>
                <span class="prompt-chip" @click="appendPrompt('宋代风格，秀丽精巧')">宋代</span>
                <span class="prompt-chip" @click="appendPrompt('明清风格，规整严谨')">明清</span>
              </div>
            </div>

            <div class="chips-group">
              <span class="chips-category">屋顶样式</span>
              <div class="chips-row">
                <span class="prompt-chip" @click="appendPrompt('重檐歇山顶')">歇山顶</span>
                <span class="prompt-chip" @click="appendPrompt('悬山顶')">悬山顶</span>
                <span class="prompt-chip" @click="appendPrompt('庑殿顶')">庑殿顶</span>
                <span class="prompt-chip" @click="appendPrompt('硬山顶')">硬山顶</span>
              </div>
            </div>

            <div class="chips-group">
              <span class="chips-category">色彩倾向</span>
              <div class="chips-row">
                <span class="prompt-chip" @click="appendPrompt('朱红大漆，黄琉璃瓦')">朱红·琉璃</span>
                <span class="prompt-chip" @click="appendPrompt('青砖黛瓦，素雅质朴')">青砖黛瓦</span>
                <span class="prompt-chip" @click="appendPrompt('金碧辉煌，皇家气度')">金碧辉煌</span>
              </div>
            </div>
          </div>

          <!-- 执行引擎按钮 -->
          <el-button
            type="primary"
            size="large"
            :loading="isPolling"
            :disabled="!prompt.trim()"
            @click="handleSubmit"
            class="generate-btn"
          >
            <el-icon v-if="!isPolling"><MagicStick /></el-icon>
            {{ isPolling ? '正在幻筑...' : '生成三维幻境' }}
          </el-button>
        </div>
      </div>

      <!-- 右侧预览/状态区 -->
      <div class="huanzhu-right">
        <!-- 轮询状态 — 宝塔构建动画 -->
        <div v-if="isPolling" class="loading-section glass-panel">
          <div class="dream-loading">
            <p class="loading-text">{{ currentLoadingText }}</p>
            <div class="pagoda-building">
              <div class="pagoda-layer" v-for="i in 6" :key="i"></div>
            </div>
            <p class="loading-sub">正在为您雕琢飞檐，构建数字孪生...</p>
          </div>
        </div>

        <!-- 生成结果 -->
        <div v-if="taskResult && taskResult.status === 'SUCCESS'" class="result-section glass-panel">
          <el-result
            icon="success"
            title="幻筑完成！"
            sub-title="您的数字古建已生成，可前往社区发布展示"
          >
            <template #extra>
              <el-button type="primary" @click="viewAsset">
                <el-icon><View /></el-icon>
                查看 3D 模型
              </el-button>
              <el-button @click="$router.push('/home')">返回社区</el-button>
            </template>
          </el-result>
        </div>

        <!-- 失败提示 -->
        <div v-if="taskResult && taskResult.status === 'FAILED'" class="result-section glass-panel">
          <el-result
            icon="error"
            title="生成失败"
            :sub-title="taskResult.errorMessage || '请调整描述词后重试'"
          >
            <template #extra>
              <el-button type="primary" @click="resetForm">重新幻筑</el-button>
            </template>
          </el-result>
        </div>

        <!-- 空闲态占位 -->
        <div v-if="!isPolling && !taskResult" class="placeholder-section glass-panel">
          <div class="placeholder-icon">&#x2302;</div>
          <p class="placeholder-title">3D 预览区</p>
          <p class="placeholder-desc">
            在左侧输入古建描述词，AI 将为您生成精美的三维模型。<br>
            支持 WebGL PBR 物理渲染，琉璃瓦高光与木质漫反射真实呈现。
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, View } from '@element-plus/icons-vue'
import { huanzhuApi } from '@/api/huanzhu'
import { useNotificationStore } from '@/stores/notification'

const notifStore = useNotificationStore()

const prompt = ref('')
const isPolling = ref(false)
const taskId = ref(null)
const taskResult = ref(null)

const loadingTexts = [
  '挥毫落纸墨痕新...',
  '探寻古建的营造密码...',
  '正在为您雕琢飞檐...',
  '榫卯交错，匠心独运...',
  '千年宫阙，一梦重现...',
  '琉璃瓦上，流光溢彩...',
]

const currentLoadingText = ref(loadingTexts[0])
let loadingTextTimer = null

function startLoadingTextRotation() {
  let idx = 0
  loadingTextTimer = setInterval(() => {
    idx = (idx + 1) % loadingTexts.length
    currentLoadingText.value = loadingTexts[idx]
  }, 2500)
}

function stopLoadingTextRotation() {
  if (loadingTextTimer) {
    clearInterval(loadingTextTimer)
    loadingTextTimer = null
  }
}

onUnmounted(() => stopLoadingTextRotation())

/** 追加智能提示标签到输入框 */
function appendPrompt(tag) {
  const current = prompt.value.trim()
  if (current && !current.endsWith('，') && !current.endsWith(',')) {
    prompt.value = current + '，'
  }
  prompt.value = (prompt.value + tag).replace(/^,/, '')
}

async function handleSubmit() {
  if (!prompt.value.trim()) return ElMessage.warning('请输入建筑描述')
  if (isPolling.value) return

  isPolling.value = true
  taskResult.value = null
  startLoadingTextRotation()

  try {
    const res = await huanzhuApi.submit(prompt.value.trim())
    taskId.value = res.data.taskId

    // 幂等检测：如果是重复提交，直接查看已有任务状态
    if (res.data.duplicate) {
      ElMessage.info('检测到相同的幻筑任务已在处理中，为您定位到已有任务')
    } else {
      ElMessage.info('任务已提交，正在为您生成古建模型...')
    }

    pollInterval = setInterval(pollTaskStatus, 5000)
  } catch {
    isPolling.value = false
    stopLoadingTextRotation()
  }
}

let pollInterval = null

async function pollTaskStatus() {
  if (!taskId.value) return

  try {
    const res = await huanzhuApi.getTaskStatus(taskId.value)
    taskResult.value = res.data

    if (res.data.status === 'SUCCESS') {
      clearInterval(pollInterval)
      isPolling.value = false
      stopLoadingTextRotation()
      notifStore.checkTaskSuccess(res.data)
      ElMessage.success('幻筑完成！数字锦盒已送达')
    } else if (res.data.status === 'FAILED') {
      clearInterval(pollInterval)
      isPolling.value = false
      stopLoadingTextRotation()
    }
  } catch {
    clearInterval(pollInterval)
    isPolling.value = false
    stopLoadingTextRotation()
  }
}

function viewAsset() {
  window.open('/notifications', '_self')
}

function resetForm() {
  isPolling.value = false
  taskResult.value = null
  taskId.value = null
  stopLoadingTextRotation()
  if (pollInterval) clearInterval(pollInterval)
}
</script>

<style scoped>
.huanzhu-page {
  max-width: 1200px;
}

/* 左右分栏创作工作区 */
.huanzhu-workspace {
  display: flex;
  gap: var(--spacing-lg);
  align-items: flex-start;
}

/* 左侧控制区 — 固定 400px */
.huanzhu-left {
  width: 400px;
  flex-shrink: 0;
}

.input-section {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-card);
}

.input-label {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-main);
  margin-bottom: var(--spacing-sm);
}

.prompt-textarea {
  margin-bottom: var(--spacing-md);
}

.prompt-textarea :deep(.el-textarea__inner) {
  min-height: 100px;
  line-height: var(--line-height-body);
  font-size: 14px;
}

/* 智能提示标签 */
.prompt-chips-section {
  margin-bottom: var(--spacing-md);
}

.chips-label {
  font-size: 13px;
  color: var(--color-text-sub);
  margin-bottom: var(--spacing-sm);
  display: block;
}

.chips-group {
  margin-bottom: var(--spacing-sm);
}

.chips-category {
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
  margin-bottom: var(--spacing-xs);
  display: block;
}

.chips-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
}

/* 生成按钮 */
.generate-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  letter-spacing: 2px;
  border-radius: var(--radius-full);
}

/* 右侧预览区 — 占据剩余空间 */
.huanzhu-right {
  flex: 1;
  min-height: 500px;
}

.loading-section,
.result-section,
.placeholder-section {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-section {
  flex-direction: column;
  gap: var(--spacing-md);
  text-align: center;
  padding: var(--spacing-xxl) var(--spacing-lg);
}

.placeholder-icon {
  font-size: 64px;
  color: var(--color-secondary);
  opacity: 0.6;
}

.placeholder-title {
  font-size: var(--font-size-subtitle);
  font-weight: 600;
  color: var(--color-text-sub);
}

.placeholder-desc {
  font-size: 14px;
  color: var(--color-text-muted);
  line-height: var(--line-height-body);
  max-width: 360px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .huanzhu-workspace {
    flex-direction: column;
  }

  .huanzhu-left {
    width: 100%;
  }

  .huanzhu-right {
    min-height: 300px;
  }
}
</style>
