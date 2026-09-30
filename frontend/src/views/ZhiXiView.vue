<template>
  <div class="view-shell">
    <div class="v4-bench v4-bench--3">
      <!-- ═══════════════ 左列 · 投放 · 叠加 · 行动 ═══════════════ -->
      <div class="v4-col">
        <!-- 投放区（可点击 / 可拖拽） -->
        <button
          type="button"
          class="v4-drop"
          :class="{ 'is-filled': !!selectedFile, 'is-over': dragActive }"
          :aria-label="selectedFile ? '重新选择古建影像' : '点击或拖入古建影像'"
          @click="triggerUpload"
          @dragover="onDragOver"
          @dragleave="onDragLeave"
          @drop="onDrop"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 16.2V4.4" />
            <path d="M12 4.4 7.7 8.7M12 4.4l4.3 4.3" />
            <path d="M3.8 15.2v2.5a2.7 2.7 0 0 0 2.7 2.7h11a2.7 2.7 0 0 0 2.7-2.7v-2.5" />
          </svg>
          <b>{{ selectedFile ? selectedFile.name : '拖入古建影像' }}</b>
          <small v-if="!selectedFile">支持 JPG / PNG / WebP / BMP · 单张 ≤ 5MB</small>
          <small v-else>已就绪 · {{ sizeText }} · 点击可重新选择</small>
        </button>

        <input
          ref="fileInputRef"
          class="zx-file"
          type="file"
          accept="image/jpeg,image/png,image/webp,image/bmp,.jpg,.jpeg,.png,.webp,.bmp"
          @change="onFileInput"
        />

        <!-- 视图叠加开关（真实生效，见交付说明） -->
        <div class="v4-fieldset">
          <span class="lab">视图叠加 · OVERLAY</span>
          <button
            type="button"
            class="v4-opt"
            :class="{ 'is-on': showBoxes }"
            role="switch"
            :aria-checked="showBoxes ? 'true' : 'false'"
            aria-label="构件框选：显示或隐藏识别框"
            @click="showBoxes = !showBoxes"
          >
            <span>构件框选</span>
            <span class="box" aria-hidden="true"></span>
          </button>
          <button
            type="button"
            class="v4-opt"
            :class="{ 'is-on': showLabels }"
            role="switch"
            :aria-checked="showLabels ? 'true' : 'false'"
            aria-label="构件标签：显示或隐藏视口热区文字"
            @click="showLabels = !showLabels"
          >
            <span>构件标签</span>
            <span class="box" aria-hidden="true"></span>
          </button>
          <button
            type="button"
            class="v4-opt"
            :class="{ 'is-on': cultureFollows }"
            role="switch"
            :aria-checked="cultureFollows ? 'true' : 'false'"
            aria-label="文化解读：点选构件时在右列切换到该构件的解读"
            @click="cultureFollows = !cultureFollows"
          >
            <span>文化解读</span>
            <span class="box" aria-hidden="true"></span>
          </button>
        </div>

        <!-- 主行动 -->
        <button
          type="button"
          class="v4-btn v4-btn--gold v4-btn--block v4-btn--lg"
          :disabled="analyzing || !selectedFile"
          :title="!selectedFile ? '请先选择一张古建影像' : '提交至 VGGT 引擎解析'"
          @click="startAnalyze"
        >
          <el-icon :size="16"><MagicStick /></el-icon>
          <span>{{ analyzing ? '几何推理中…' : '开始几何推理' }}</span>
        </button>

        <!-- 算力节制 -->
        <div class="v4-tip">
          <b>关于算力节制</b>
          古建高精几何解析犹如匠人雕琢，需耗费大量云端算力。出于对资源的敬畏，平台对单用户实行每日 5
          次节制（未登录访客按 IP 计）；额度于次日 0 点自动恢复。
        </div>

        <!-- 推理引擎卡（只写后端可确证的事实） -->
        <div class="v4-card">
          <div class="v4-cardhd"><h3>推理引擎 · ENGINE</h3></div>
          <div class="zx-kvs">
            <div class="v4-kv"><span class="k">引擎</span><span class="v v--jade">VGGT · FastAPI</span></div>
            <div class="v4-kv"><span class="k">推理服务</span><span class="v v--jade">127.0.0.1:8000</span></div>
            <div class="v4-kv"><span class="k">输出内容</span><span class="v v--jade">构件框选 · 文化解读</span></div>
            <div class="v4-kv"><span class="k">每日额度</span><span class="v v--gold">5 次 / 人</span></div>
          </div>
        </div>
      </div>

      <!-- ═══════════════ 中列 · 三维视口 ═══════════════ -->
      <div ref="viewportRef" class="v4-viewport v4-zone">
        <!-- 测绘网格底纹（影像未铺满时可见） -->
        <div class="zx-mesh" aria-hidden="true"></div>

        <!-- 真实影像 + 真实 bbox 叠加 -->
        <div v-if="uploadedImage" class="zx-stage">
          <div class="zx-frame" :style="frameStyle">
            <img class="zx-img" :src="uploadedImage" :alt="`已上传的古建影像 ${fileName}`" />

            <!-- 构件框选（bbox 为后端返回的归一化坐标） -->
            <template v-if="phase === 'done' && showBoxes">
              <button
                v-for="(el, i) in elements"
                :key="`box-${i}`"
                type="button"
                class="zx-bbox"
                :class="{ 'is-sel': selectedIndex === i }"
                :style="bboxStyle(el)"
                :aria-label="`选中构件 ${el.name || i + 1}`"
                :aria-pressed="selectedIndex === i ? 'true' : 'false'"
                @click="selectElement(i)"
              ></button>
            </template>

            <!-- 解析中：青色扫描带 -->
            <template v-if="analyzing">
              <div class="zx-dim" aria-hidden="true"></div>
              <div class="zx-scan" aria-hidden="true"></div>
            </template>
          </div>

          <!-- 构件热区标注：与影像框同心同尺寸，保证与 bbox 坐标一致 -->
          <div v-if="phase === 'done' && showLabels" class="zx-hots" :style="frameStyle">
            <button
              v-for="(el, i) in elements"
              :key="`hot-${i}`"
              type="button"
              class="v4-hot zx-hot"
              :class="{ 'is-on': revealed, 'is-sel': selectedIndex === i }"
              :style="hotStyle(el)"
              :aria-label="`选中构件 ${el.name || i + 1}`"
              :aria-pressed="selectedIndex === i ? 'true' : 'false'"
              @click="selectElement(i)"
            >
              <span class="pt" aria-hidden="true"></span>
              <span class="lb">{{ el.name || '未命名构件' }}</span>
            </button>
          </div>
        </div>

        <!-- 视口遮罩 + 四角金线 -->
        <div class="v4-vp-scrim" aria-hidden="true"></div>
        <svg class="v4-corner v4-corner--tl" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M0.5 10V0.5H10" stroke="currentColor" />
        </svg>
        <svg class="v4-corner v4-corner--tr" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M14 0.5H23.5V10" stroke="currentColor" />
        </svg>
        <svg class="v4-corner v4-corner--bl" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M0.5 14V23.5H10" stroke="currentColor" />
        </svg>
        <svg class="v4-corner v4-corner--br" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M14 23.5H23.5V14" stroke="currentColor" />
        </svg>

        <!-- 顶部状态胶囊 -->
        <div class="v4-vp-top">
          <span class="v4-pill" :class="pillClass">{{ pillText }}</span>
          <span v-if="phase === 'done' && styleLabel" class="v4-pill v4-pill--gold v4-pill--bare">
            {{ styleLabel }}
          </span>
        </div>

        <!-- 元信息行（全部来自后端返回） -->
        <div v-if="phase !== 'idle'" class="v4-vp-meta">{{ metaLine }}</div>

        <!-- ① 空态 -->
        <div v-if="phase === 'idle'" class="zx-idle">
          <b>拖入影像以开始解析</b>
          <small>左列投放区 · 支持 JPG / PNG / WebP / BMP，单张不超过 5MB</small>
        </div>

        <!-- ② 解析中 -->
        <div v-if="analyzing" class="zx-waitbar" role="status">
          <span class="zx-spin loading-spin"><el-icon :size="14"><Loading /></el-icon></span>
          <span>VGGT 引擎正在解构影像 · 已等待 {{ elapsed }} 秒</span>
        </div>

        <!-- ④ 失败：后端原文 + 明确的下一步 -->
        <div v-if="phase === 'error' && errorState" class="zx-errcard" role="alert">
          <span class="v4-pill" :class="errorState.pillClass">{{ errorState.pill }}</span>
          <h3>{{ errorState.title }}</h3>
          <p class="zx-errcard-msg">{{ errorState.message }}</p>
          <p class="zx-errcard-next">{{ errorState.next }}</p>
          <div class="zx-errcard-acts">
            <button
              v-if="errorState.kind === 'quota'"
              type="button"
              class="v4-btn v4-btn--ghost v4-btn--sm"
              disabled
            >
              明日再试
            </button>
            <button
              v-else-if="errorState.kind !== 'input'"
              type="button"
              class="v4-btn v4-btn--gold v4-btn--sm"
              :disabled="!selectedFile"
              @click="startAnalyze"
            >
              重试解析
            </button>
            <button type="button" class="v4-btn v4-btn--ghost v4-btn--sm" @click="resetAll">
              换一张影像
            </button>
          </div>
        </div>

        <!-- 底部工具条 -->
        <div class="v4-dock">
          <button
            type="button"
            :class="{ 'is-on': showBoxes }"
            :aria-pressed="showBoxes ? 'true' : 'false'"
            aria-label="构件框选开关"
            title="构件框选"
            @click="showBoxes = !showBoxes"
          >
            <el-icon :size="17"><Crop /></el-icon>
          </button>
          <button
            type="button"
            :class="{ 'is-on': showLabels }"
            :aria-pressed="showLabels ? 'true' : 'false'"
            aria-label="构件标签开关"
            title="构件标签"
            @click="showLabels = !showLabels"
          >
            <el-icon :size="17"><PriceTag /></el-icon>
          </button>
          <button type="button" aria-label="重新选择影像" title="重新选择影像" @click="triggerUpload">
            <el-icon :size="17"><Refresh /></el-icon>
          </button>
          <button type="button" aria-label="全屏查看视口" title="全屏查看" @click="toggleFullscreen">
            <el-icon :size="17"><FullScreen /></el-icon>
          </button>
        </div>
      </div>

      <!-- ═══════════════ 右列 · 构件 · 解读 · 预留 ═══════════════ -->
      <div class="v4-col">
        <!-- 识别构件 -->
        <div class="v4-card">
          <div class="v4-cardhd">
            <h3>识别构件</h3>
            <span class="k">{{ elements.length ? `${elements.length} 类` : '待解析' }}</span>
          </div>
          <div v-if="!elements.length" class="v4-empty zx-empty-sm">
            尚未解析 · 上传影像后点击「开始几何推理」
          </div>
          <div v-else class="zx-parts">
            <button
              v-for="(el, i) in elements"
              :key="`part-${i}`"
              type="button"
              class="v4-kv zx-part"
              :class="{ 'is-sel': selectedIndex === i }"
              :aria-pressed="selectedIndex === i ? 'true' : 'false'"
              @click="selectElement(i)"
            >
              <span class="k zx-partk">
                <i class="zx-dot" :class="`zx-dot--${dotTone(i)}`" aria-hidden="true"></i>
                <span class="zx-partname">{{ el.name || '未命名构件' }}</span>
              </span>
              <span class="v v--jade">{{ confText(el.confidence) }}</span>
            </button>
          </div>
        </div>

        <!-- 文化解读（青色技术语义） -->
        <div class="v4-card v4-card--jade">
          <div class="v4-cardhd">
            <h3>文化解读</h3>
            <span class="k zx-tag">{{ cultureTag }}</span>
          </div>
          <template v-if="cultureText">
            <p class="zx-culture-h">{{ cultureHeading }}</p>
            <p class="zx-culture">{{ cultureText }}</p>
          </template>
          <p v-else class="zx-culture zx-culture--mut">
            解析完成后，此处呈现 VGGT 返回的构件文化解读；点选右列构件行或视口热区可切换到该构件。
          </p>
        </div>

        <!-- 未开放的残损推演 -->
        <div class="zx-locked">
          <button type="button" class="v4-btn v4-btn--ghost v4-btn--block" disabled>
            <el-icon :size="15"><Lock /></el-icon>
            <span>启动 AI 结构推演与修复（Beta 未开放）</span>
          </button>
          <p class="zx-locked-note">
            残损推演引擎仍在训练中；后端已预留 <code>/api/v1/analysis/restoration/predict</code>，当前返回 501。
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  Crop, FullScreen, Loading, Lock, MagicStick, PriceTag, Refresh
} from '@element-plus/icons-vue'
import { zhixiApi } from '@/api/zhixi'

/* ═══ 常量：与后端 / Python 引擎保持一致的真实约束 ═══ */
const MAX_BYTES = 5 * 1024 * 1024
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/bmp']
const EXT_RE = /\.(jpe?g|png|webp|bmp)$/i

/* ═══ DOM 引用 ═══ */
const fileInputRef = ref(null)
const viewportRef = ref(null)

/* ═══ 影像状态 ═══ */
const selectedFile = ref(null)
const uploadedImage = ref(null)
const natural = ref({ w: 0, h: 0 })
const dragActive = ref(false)

/* ═══ 请求状态（四态） ═══ */
const analyzing = ref(false)
const elapsed = ref(0)
const result = ref(null)
const errorState = ref(null)
const revealed = ref(false)

/* ═══ 视图叠加开关（真实生效，默认全开） ═══ */
const showBoxes = ref(true)
const showLabels = ref(true)
const cultureFollows = ref(true)
const selectedIndex = ref(-1)

/* ═══ 视口尺寸测量（保证 bbox 百分比与影像像素严格对齐） ═══ */
const stageSize = ref({ w: 0, h: 0 })
let resizeObserver = null
let timer = null
let revealTimer = null

/* ═══ 派生数据：全部来自后端返回，不做任何编造 ═══ */
const elements = computed(() => {
  const list = result.value?.structural_elements
  return Array.isArray(list) ? list.filter((e) => e && typeof e === 'object') : []
})
const summary = computed(() => (typeof result.value?.summary === 'string' ? result.value.summary : ''))
const imageInfo = computed(() => (result.value?.image_info && typeof result.value.image_info === 'object'
  ? result.value.image_info
  : {}))
const analysisId = computed(() => (typeof result.value?.analysis_id === 'string' ? result.value.analysis_id : ''))
const fileName = computed(() => imageInfo.value.filename || selectedFile.value?.name || '')
const styleLabel = computed(() => imageInfo.value.architectural_style || '')
const selectedElement = computed(() => (
  selectedIndex.value >= 0 ? elements.value[selectedIndex.value] || null : null
))

const phase = computed(() => {
  if (errorState.value) return 'error'
  if (analyzing.value) return 'analyzing'
  if (result.value) return 'done'
  return 'idle'
})

const pillClass = computed(() => ({
  idle: 'v4-pill--mut',
  analyzing: 'v4-pill--jade',
  done: 'v4-pill--jade',
  error: 'v4-pill--red'
}[phase.value]))

const pillText = computed(() => ({
  idle: '等待影像',
  analyzing: `解析中 · 已等待 ${elapsed.value}s`,
  done: `解析完成 · ${elements.value.length} 类构件`,
  error: '解析未完成'
}[phase.value]))

const metaLine = computed(() => {
  const parts = []
  if (fileName.value) parts.push(fileName.value)
  if (phase.value === 'done') {
    if (analysisId.value) parts.push(analysisId.value)
    parts.push('ENGINE VGGT')
  } else if (phase.value === 'analyzing') {
    parts.push('VGGT 解析中')
  } else {
    parts.push('解析未完成')
  }
  return parts.join('　·　')
})

/* 文化解读卡：开关开启且选中构件（且有 cultural_note）时跟随该构件，否则呈现整体 summary */
const cultureSel = computed(() => {
  if (!cultureFollows.value) return null
  const el = selectedElement.value
  if (!el) return null
  const note = typeof el.cultural_note === 'string' ? el.cultural_note.trim() : ''
  return note ? { heading: el.name || '构件', text: note } : null
})
const cultureText = computed(() => cultureSel.value?.text || summary.value)
const cultureHeading = computed(() => cultureSel.value?.heading || '整体解读')
const cultureTag = computed(() => (cultureSel.value ? '构件 · 选中' : 'AI 转译'))

const sizeText = computed(() => {
  const size = selectedFile.value?.size || 0
  if (!size) return ''
  return size >= 1024 * 1024
    ? `${(size / 1024 / 1024).toFixed(1)} MB`
    : `${Math.max(1, Math.round(size / 1024))} KB`
})

/* 影像按 contain 适配视口后的精确显示尺寸 */
const frameBox = computed(() => {
  const { w: sw, h: sh } = stageSize.value
  const { w: nw, h: nh } = natural.value
  if (!sw || !sh || !nw || !nh) return null
  const scale = Math.min(sw / nw, sh / nh)
  return { width: Math.round(nw * scale), height: Math.round(nh * scale) }
})
const frameStyle = computed(() => {
  const box = frameBox.value
  return box
    ? { width: `${box.width}px`, height: `${box.height}px` }
    : { width: '100%', height: '100%' }
})

/* ═══ 工具函数 ═══ */
function clamp01(v) {
  return Math.min(1, Math.max(0, v))
}
function parseBbox(el) {
  const b = el?.bbox
  if (!Array.isArray(b) || b.length < 4) return null
  const nums = b.slice(0, 4).map(Number)
  if (!nums.every(Number.isFinite)) return null
  const [x1, y1, x2, y2] = nums
  return {
    left: clamp01(Math.min(x1, x2)),
    top: clamp01(Math.min(y1, y2)),
    right: clamp01(Math.max(x1, x2)),
    bottom: clamp01(Math.max(y1, y2))
  }
}
function bboxStyle(el) {
  const b = parseBbox(el)
  if (!b) return { display: 'none' }
  return {
    left: `${b.left * 100}%`,
    top: `${b.top * 100}%`,
    width: `${Math.max(0, b.right - b.left) * 100}%`,
    height: `${Math.max(0, b.bottom - b.top) * 100}%`
  }
}
function hotStyle(el) {
  const b = parseBbox(el)
  if (!b) return { display: 'none' }
  return {
    left: `${((b.left + b.right) / 2) * 100}%`,
    top: `${((b.top + b.bottom) / 2) * 100}%`
  }
}
function confText(c) {
  const n = Number(c)
  return Number.isFinite(n) ? n.toFixed(2) : '—'
}
function dotTone(i) {
  return ['gold', 'goldlt', 'jade', 'mut'][i % 4]
}

/* ═══ 计时器 / 测量 ═══ */
function startTimer() {
  stopTimer()
  elapsed.value = 0
  timer = window.setInterval(() => { elapsed.value += 1 }, 1000)
}
function stopTimer() {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
}
function clearReveal() {
  if (revealTimer) {
    window.clearTimeout(revealTimer)
    revealTimer = null
  }
}
function measure() {
  const el = viewportRef.value
  if (!el) return
  stageSize.value = { w: el.clientWidth, h: el.clientHeight }
}

/* ═══ 文件选择 / 拖拽 ═══ */
function triggerUpload() {
  if (analyzing.value) return
  fileInputRef.value?.click()
}

function onFileInput(e) {
  const input = e.target
  acceptFiles(input.files)
  input.value = '' // 允许再次选择同一文件
}

function acceptFiles(fileList) {
  if (analyzing.value) return
  const file = fileList?.[0]
  if (!file) return

  const typeOk = ALLOWED_TYPES.includes(file.type)
  if (!typeOk && !EXT_RE.test(file.name)) {
    setLocalError('影像格式不受支持', '解析引擎只接受 JPEG / PNG / WebP / BMP 四种格式，请转换后再上传。')
    return
  }
  if (file.size > MAX_BYTES) {
    setLocalError(
      '影像超过 5MB',
      `当前影像约 ${(file.size / 1024 / 1024).toFixed(1)} MB，服务端单文件上限为 5MB，请压缩后重试。`
    )
    return
  }

  selectedFile.value = file
  errorState.value = null
  result.value = null
  selectedIndex.value = -1
  revealed.value = false
  clearReveal()

  const reader = new FileReader()
  reader.onload = (ev) => {
    const dataUrl = ev.target?.result
    if (typeof dataUrl !== 'string') return
    uploadedImage.value = dataUrl
    const probe = new Image()
    probe.onload = () => { natural.value = { w: probe.naturalWidth, h: probe.naturalHeight } }
    probe.onerror = () => { natural.value = { w: 0, h: 0 } }
    probe.src = dataUrl
  }
  reader.readAsDataURL(file)
}

function onDragOver(e) {
  e.preventDefault()
  if (!analyzing.value) dragActive.value = true
}
function onDragLeave() {
  dragActive.value = false
}
function onDrop(e) {
  e.preventDefault()
  dragActive.value = false
  if (analyzing.value) return
  acceptFiles(e.dataTransfer?.files)
}

/* ═══ 错误态 ═══ */
/* 本地输入被拒（格式 / 体积）：不保留旧影像与旧结果，回到干净的空态 + 说明卡 */
function setLocalError(title, message) {
  selectedFile.value = null
  uploadedImage.value = null
  natural.value = { w: 0, h: 0 }
  result.value = null
  selectedIndex.value = -1
  revealed.value = false
  clearReveal()
  errorState.value = {
    kind: 'input',
    pill: '未能提交',
    pillClass: 'v4-pill--red',
    title,
    message,
    next: '请更换一张符合要求的古建影像后重新提交。'
  }
}

/* ═══ 提交解析 ═══ */
async function startAnalyze() {
  if (analyzing.value) return
  if (!selectedFile.value) return

  analyzing.value = true
  errorState.value = null
  result.value = null
  selectedIndex.value = -1
  revealed.value = false
  clearReveal()
  startTimer()

  try {
    const formData = new FormData()
    formData.append('image', selectedFile.value)
    const res = await zhixiApi.analyze(formData)
    const payload = res?.data ?? res

    // Python 端对「非法图片 / 不支持类型」会以 HTTP 200 + success:false 返回
    if (!payload || payload.success === false) {
      errorState.value = {
        kind: 'engine',
        pill: '引擎已响应 · 未通过',
        pillClass: 'v4-pill--red',
        title: '解析引擎未能识别该影像',
        message: payload?.error || '引擎未返回结构解析结果，请更换一张清晰的古建影像后重试。',
        next: '建议上传正面立面或斗栱细节的清晰照片（JPEG / PNG / WebP / BMP，≤ 5MB）。'
      }
      return
    }

    result.value = payload
    revealTimer = window.setTimeout(() => { revealed.value = true }, 80)
  } catch (err) {
    const status = err?.response?.status
    const serverMsg = err?.response?.data?.message

    if (status === 429) {
      // 后端写好的温润文案：原样展示，不做二次改写
      errorState.value = {
        kind: 'quota',
        pill: '每日额度已用完 · 429',
        pillClass: 'v4-pill--gold',
        title: '今日解析额度已用完',
        message: serverMsg || '出于对资源的敬畏，平台对单用户实行每日最多 5 次的解析节制。今日额度已用完，请明日再试。',
        next: '额度按用户（未登录按 IP）每日 5 次，次日 0 点自动恢复。你也可以先更换影像，明日再来提交。'
      }
    } else if (status === 503) {
      errorState.value = {
        kind: 'engine',
        pill: '引擎未就绪 · 503',
        pillClass: 'v4-pill--red',
        title: 'VGGT 解析引擎未就绪',
        message: serverMsg || 'VGGT 深度解析引擎未就绪，请确认 Python 服务已启动（端口 8000）。',
        next: '请在 vggt-api 目录执行 python api_server.py（监听 127.0.0.1:8000），确认服务可用后点击「重试解析」。'
      }
    } else {
      errorState.value = {
        kind: 'other',
        pill: status ? `解析失败 · ${status}` : '请求未送达',
        pillClass: 'v4-pill--red',
        title: '解析未能完成',
        message: serverMsg || err?.message || '请求解析引擎时发生未知错误，请稍后重试。',
        next: '请确认后端服务（端口 8080）可访问，然后点击「重试解析」。'
      }
    }
  } finally {
    analyzing.value = false
    stopTimer()
  }
}

/* ═══ 交互 ═══ */
function selectElement(i) {
  selectedIndex.value = selectedIndex.value === i ? -1 : i
}

function resetAll() {
  selectedFile.value = null
  uploadedImage.value = null
  natural.value = { w: 0, h: 0 }
  result.value = null
  errorState.value = null
  selectedIndex.value = -1
  revealed.value = false
  dragActive.value = false
  stopTimer()
  clearReveal()
  elapsed.value = 0
}

async function toggleFullscreen() {
  const el = viewportRef.value
  if (!el) return
  try {
    if (document.fullscreenElement) await document.exitFullscreen()
    else await el.requestFullscreen?.()
  } catch {
    /* 浏览器不支持或被用户拒绝，保持原状 */
  }
}

/* ═══ 生命周期 ═══ */
onMounted(() => {
  measure()
  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(measure)
    if (viewportRef.value) resizeObserver.observe(viewportRef.value)
  }
  window.addEventListener('resize', measure)
})

onBeforeUnmount(() => {
  stopTimer()
  clearReveal()
  window.removeEventListener('resize', measure)
  resizeObserver?.disconnect()
  resizeObserver = null
})
</script>

<style scoped>
/* ═══ 隐藏的文件输入 ═══ */
.zx-file { display: none; }

/* ═══ 左列微调 ═══ */
.v4-drop > b {
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.v4-drop.is-over { background: var(--color-accent-soft); border-color: var(--color-accent); }
.zx-kvs { display: flex; flex-direction: column; gap: 9px; }

/* ═══ 视口：测绘网格（媒体令牌在日夜两种模式下都可见） ═══ */
.zx-mesh {
  position: absolute;
  inset: 0;
  z-index: 0;
  opacity: 0.55;
  background-image:
    linear-gradient(var(--color-media-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--color-media-line) 1px, transparent 1px);
  background-size: 48px 48px;
}

/* 四角金线：以媒体令牌着色，浅色模式下同样可见 */
.v4-corner { color: var(--color-border-gold); }

/* ═══ 影像舞台：contain 适配后，叠加层与影像像素严格对齐 ═══ */
.zx-stage {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: grid;
  place-items: center;
}
/* 影像框与热区层共用同一网格单元，尺寸同为 contain 适配结果 → 坐标完全对齐 */
.zx-frame,
.zx-hots { grid-area: 1 / 1; }
.zx-frame {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-xs);
}
.zx-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* bbox 框选 */
.zx-bbox {
  position: absolute;
  padding: 0;
  cursor: pointer;
  background: var(--color-accent-soft);
  border: 1px solid var(--color-accent-light);
  border-radius: var(--radius-xs);
  transition: border-color var(--transition-fast), background-color var(--transition-fast);
}
.zx-bbox:hover { border-color: var(--color-accent); }
.zx-bbox.is-sel {
  background: var(--color-jade-soft);
  border: 2px solid var(--color-jade-text);
}

/* 解析中：压暗 + 青色扫描带 */
.zx-dim {
  position: absolute;
  inset: 0;
  background: var(--color-scrim-soft);
}
.zx-scan {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 40%;
  pointer-events: none;
  background: linear-gradient(to bottom, transparent, var(--color-jade-soft));
  border-bottom: 2px solid var(--color-jade-text);
  box-shadow: 0 0 18px var(--color-jade-text);
  animation: zx-scan 2.4s linear infinite;
}
@keyframes zx-scan {
  0% { transform: translateY(-100%); }
  100% { transform: translateY(250%); }
}

/* ═══ 热区标注（.v4-hot 原语 + 可点击） ═══ */
.zx-hots {
  position: relative;
  z-index: 4;
  pointer-events: none;
}
.zx-hot {
  padding: 0;
  background: none;
  border: none;
  cursor: pointer;
  pointer-events: auto;
  transform: translate(-5.5px, -50%);
}
.zx-hot.is-sel > .lb {
  color: var(--color-jade-text);
  border-color: var(--color-border-jade);
}

/* ═══ 空态 ═══ */
.zx-idle {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 var(--spacing-lg);
  text-align: center;
}
.zx-idle > b {
  font-family: var(--font-family-serif);
  font-size: 16px;
  font-weight: 600;
  color: var(--color-on-media);
}
.zx-idle > small {
  font-size: 11px;
  line-height: 1.7;
  color: var(--color-on-media-faint);
}

/* ═══ 解析中提示条 ═══ */
.zx-waitbar {
  position: absolute;
  left: 50%;
  bottom: 84px;
  z-index: 5;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: calc(100% - 32px);
  padding: 8px 14px;
  border-radius: var(--radius-full);
  font-size: 11px;
  white-space: nowrap;
  color: var(--color-jade-text);
  background: var(--color-scrim-strong);
  border: 1px solid var(--color-border-jade);
}
.zx-spin { display: inline-flex; }

/* ═══ 失败态卡片（影像之上的浮层，用媒体令牌保证可读） ═══ */
.zx-errcard {
  position: absolute;
  left: 50%;
  top: 50%;
  z-index: 6;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: min(430px, calc(100% - 32px));
  max-height: calc(100% - 120px);
  overflow-y: auto;
  padding: 20px;
  border-radius: var(--radius-lg);
  background: var(--color-scrim-strong);
  border: 1px solid var(--color-border-gold);
  box-shadow: var(--shadow-card);
}
.zx-errcard > h3 {
  font-family: var(--font-family-serif);
  font-size: 16px;
  font-weight: 600;
  color: var(--color-on-media-strong);
}
.zx-errcard-msg {
  font-size: 12px;
  line-height: 1.9;
  color: var(--color-on-media);
}
.zx-errcard-next {
  font-size: 11px;
  line-height: 1.8;
  color: var(--color-on-media-dim);
  padding-top: 10px;
  border-top: 1px solid var(--color-media-line);
}
.zx-errcard-acts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 2px;
}

/* ═══ 右列：构件行 ═══ */
.zx-empty-sm { padding: 26px 14px; font-size: 12px; }
.zx-parts { display: flex; flex-direction: column; gap: 4px; }
.zx-part {
  width: 100%;
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  background: transparent;
  border: 1px solid transparent;
  cursor: pointer;
  text-align: left;
  transition: background-color var(--transition-fast), border-color var(--transition-fast);
}
.zx-part:hover { background: var(--color-surface-hover); }
.zx-part.is-sel {
  background: var(--color-accent-soft);
  border-color: var(--color-border-gold);
}
.zx-partk { display: flex; align-items: center; gap: 8px; min-width: 0; }
.zx-partname {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-sub);
}
.zx-part.is-sel .zx-partname { color: var(--color-accent-text); }
.zx-dot {
  width: 8px;
  height: 8px;
  flex: 0 0 8px;
  border-radius: 50%;
  background: currentColor;
}
.zx-dot--gold { color: var(--color-accent); }
.zx-dot--goldlt { color: var(--color-accent-light); }
.zx-dot--jade { color: var(--color-jade); }
.zx-dot--mut { color: var(--color-text-muted); }

/* ═══ 右列：文化解读 ═══ */
.zx-tag { color: var(--color-jade-text); }
.zx-culture-h {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-jade-text);
  margin-bottom: 6px;
}
.zx-culture {
  font-size: 11.5px;
  line-height: 1.9;
  color: var(--color-text-sub);
}
.zx-culture--mut { color: var(--color-text-muted); }

/* ═══ 右列：未开放的残损推演 ═══ */
.zx-locked { display: flex; flex-direction: column; }
.zx-locked-note {
  margin-top: 8px;
  font-size: 10px;
  line-height: 1.75;
  text-align: center;
  color: var(--color-text-ghost);
}
.zx-locked-note code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  color: var(--color-text-faint);
  word-break: break-all;
}

/* ═══ 本视图专属窄屏微调（全局 ≤900px 已把 .v4-bench--3 降为单列） ═══ */
@media (max-width: 900px) {
  .zx-waitbar { bottom: 74px; font-size: 10.5px; }
  .zx-errcard { max-height: calc(100% - 96px); padding: 16px; }
  .zx-idle { padding: 0 var(--spacing-md); }
}
</style>
