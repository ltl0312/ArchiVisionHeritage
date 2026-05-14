<template>
  <div class="huanzhu-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>
        <el-icon size="28"><MagicStick /></el-icon>
        一键幻筑
      </h1>
      <p>输入古建描述，AI 为您生成精美的 3D 模型</p>
    </div>

    <!-- 输入区域 -->
    <div class="input-section">
      <div class="prompt-input-wrapper">
        <el-input
          v-model="prompt"
          size="large"
          placeholder="例如：唐代大殿、宋代园林亭台..."
          :disabled="isPolling"
          @focus="showTips = true"
          @keyup.enter="handleSubmit"
          class="prompt-input"
        >
          <template #append>
            <el-button
              type="primary"
              :loading="isPolling"
              :disabled="!prompt.trim()"
              @click="handleSubmit"
            >
              {{ isPolling ? '幻筑中...' : '开始幻筑' }}
            </el-button>
          </template>
        </el-input>
      </div>

      <!-- 文化引导面板 -->
      <transition name="fade">
        <div v-if="showTips && !isPolling" class="tips-panel">
          <h4>
            <el-icon><InfoFilled /></el-icon>
            为了让建筑更具历史的厚重感，建议补充：
          </h4>
          <div class="tips-grid">
            <div class="tip-card" @click="appendTip('唐代风格，恢弘大气')">
              <span class="tip-tag">唐</span>
              <span>唐代 · 出檐深远，斗栱雄大</span>
            </div>
            <div class="tip-card" @click="appendTip('宋代风格，秀丽精巧')">
              <span class="tip-tag">宋</span>
              <span>宋代 · 举折平缓，装修精美</span>
            </div>
            <div class="tip-card" @click="appendTip('明代风格，规整严谨')">
              <span class="tip-tag">明</span>
              <span>明代 · 色彩厚重，装饰简练</span>
            </div>
            <div class="tip-card" @click="appendTip('清代风格，繁缛华丽')">
              <span class="tip-tag">清</span>
              <span>清代 · 彩画绚烂，规制严格</span>
            </div>
            <div class="tip-card" @click="appendTip('重檐庑殿顶，最高等级屋顶')">
              <span class="tip-tag">&#x2302;</span>
              <span>重檐庑殿顶</span>
            </div>
            <div class="tip-card" @click="appendTip('重檐歇山顶，九脊殿造型')">
              <span class="tip-tag">&#x2302;</span>
              <span>重檐歇山顶</span>
            </div>
            <div class="tip-card" @click="appendTip('朱红立柱，黄琉璃瓦')">
              <span class="tip-tag">&#127912;</span>
              <span>朱红立柱 · 黄琉璃瓦</span>
            </div>
            <div class="tip-card" @click="appendTip('青砖黛瓦，素雅质朴')">
              <span class="tip-tag">&#127912;</span>
              <span>青砖黛瓦 · 素雅质朴</span>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- 轮询状态展示 -->
    <div v-if="isPolling" class="status-section">
      <div class="dream-loading">
        <p class="loading-text">{{ loadingText }}</p>
        <div class="ink-stroke"></div>
        <p class="loading-sub">正在为您雕琢飞檐...</p>
      </div>
    </div>

    <!-- 生成结果 -->
    <div v-if="taskResult && taskResult.status === 'SUCCESS'" class="result-section">
      <el-result icon="success" title="幻筑完成！" sub-title="您的数字古建已生成，可前往社区发布展示">
        <template #extra>
          <el-button type="primary" @click="viewAsset">查看3D模型</el-button>
          <el-button @click="$router.push('/home')">返回社区</el-button>
        </template>
      </el-result>
    </div>

    <!-- 失败提示 -->
    <div v-if="taskResult && taskResult.status === 'FAILED'" class="result-section">
      <el-result icon="error" title="生成失败" :sub-title="taskResult.errorMessage || '请稍后重试'">
        <template #extra>
          <el-button type="primary" @click="resetForm">重新幻筑</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, InfoFilled } from '@element-plus/icons-vue'
import { huanzhuApi } from '@/api/huanzhu'
import { useNotificationStore } from '@/stores/notification'

const notifStore = useNotificationStore()

const prompt = ref('')
const showTips = ref(false)
const isPolling = ref(false)
const taskId = ref(null)
const taskResult = ref(null)

const loadingTexts = [
  '挥毫落纸墨痕新...',
  '探寻古建的营造密码...',
  '正在为您雕琢飞檐...',
  '榫卯交错，匠心独运...',
  '千年宫阙，一梦重现...',
]
const loadingText = computed(() => {
  const idx = Math.floor(Date.now() / 2000) % loadingTexts.length
  return loadingTexts[idx]
})

function appendTip(tip) {
  if (prompt.value.length > 0 && !prompt.value.endsWith('，') && !prompt.value.endsWith(',')) {
    prompt.value += '，'
  }
  prompt.value += tip
}

async function handleSubmit() {
  if (!prompt.value.trim()) return ElMessage.warning('请输入建筑描述')
  if (isPolling.value) return

  isPolling.value = true
  showTips.value = false
  taskResult.value = null

  try {
    const res = await huanzhuApi.submit(prompt.value.trim())
    taskId.value = res.data.taskId

    // 开始轮询，每5秒一次
    pollInterval = setInterval(pollTaskStatus, 5000)
    ElMessage.info('任务已提交，正在为您生成古建模型...')
  } catch {
    isPolling.value = false
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
      notifStore.checkTaskSuccess(res.data)
      ElMessage.success('幻筑完成！数字锦盒已送达')
    } else if (res.data.status === 'FAILED') {
      clearInterval(pollInterval)
      isPolling.value = false
    }
  } catch {
    clearInterval(pollInterval)
    isPolling.value = false
  }
}

function viewAsset() {
  // 导航到通知页查看生成的资产
  window.open('/notifications', '_self')
}

function resetForm() {
  isPolling.value = false
  taskResult.value = null
  taskId.value = null
  if (pollInterval) clearInterval(pollInterval)
}
</script>

<style scoped>
.huanzhu-page {
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

.input-section {
  background: white;
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.prompt-input-wrapper {
  margin-bottom: 16px;
}

.prompt-input :deep(.el-input-group__append) {
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.prompt-input :deep(.el-input-group__append .el-button) {
  background: transparent;
  border: none;
  color: white;
}

.tips-panel {
  background: linear-gradient(135deg, #fdf8f0 0%, #faf3e6 100%);
  border: 1px solid rgba(223,188,94,0.3);
  border-radius: var(--radius-md);
  padding: 16px;
}

.tips-panel h4 {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 12px;
}

.tips-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.tip-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid #e8e4df;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
}

.tip-card:hover {
  border-color: var(--color-accent);
  background: rgba(223,188,94,0.08);
}

.tip-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: var(--color-primary);
  color: white;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.status-section {
  margin-top: 24px;
}

.loading-text {
  font-size: 16px;
  color: var(--color-text-secondary);
}

.loading-sub {
  font-size: 14px;
  color: var(--color-text-muted);
}

.result-section {
  margin-top: 24px;
}
</style>
