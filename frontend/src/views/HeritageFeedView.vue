<template>
  <div class="view-shell">
    <!-- ═══════════ ① 能力宣言首屏（设计稿 F2 · .v4-hero） ═══════════ -->
    <section class="v4-hero" aria-labelledby="feed-hero-title">
      <img
        src="/media/hero.jpg"
        width="1400"
        height="933"
        alt="唐式木构大殿夜景，斗栱与屋檐在暖光中层层出挑"
        fetchpriority="high"
        decoding="async"
      />
      <div class="v4-hero-in">
        <div class="v4-hero-txt">
          <div class="v4-eyebrow">古建数字孪生平台 · 大创项目</div>
          <h1 id="feed-hero-title">以技术为舟，载文化远航</h1>
          <p>
            VGGT 从一张影像推理三维结构，AI 从一段文字营造殿宇。
            让每一块瓦当、每一组斗栱在数字世界重获新生。
          </p>
          <div class="v4-hero-cta">
            <router-link class="v4-btn v4-btn--gold" to="/zhixi">
              <el-icon :size="15"><MagicStick /></el-icon>
              上传影像 · 开始解析
            </router-link>
            <router-link class="v4-btn v4-btn--ghost" to="/huanzhu">用文字幻筑殿宇</router-link>
          </div>
        </div>

        <!-- 可信事实条：只列仓库内可核查的事实（设计稿「平台数据 · EVIDENCE」位） -->
        <aside class="v4-statpanel" aria-label="平台可核查事实">
          <h4>平台事实 · EVIDENCE</h4>
          <div v-if="archiveTotal !== null" class="v4-stat">
            <span class="l">已归档数字档案</span>
            <span class="n">{{ formatNumber(archiveTotal) }} 件</span>
          </div>
          <div v-for="f in heroFacts" :key="f.label" class="v4-stat">
            <span class="l">{{ f.label }}</span>
            <span class="n" :class="{ 'n--sm': f.small }">{{ f.value }}</span>
          </div>
        </aside>
      </div>
    </section>

    <!-- ═══════════ ② 双能力卡 ═══════════ -->
    <div class="v4-caps">
      <router-link class="v4-cap v4-cap--zhixi" to="/zhixi">
        <span class="v4-cap-ico"><el-icon :size="26"><Cpu /></el-icon></span>
        <span class="cap-body">
          <h3>古建智析 · VGGT 结构解析</h3>
          <p>上传一张古建影像，定位斗栱等结构要素并给出置信度，自动转译为通俗的文化解读。</p>
          <span class="meta">每日 5 次 · 高精结构推理</span>
        </span>
      </router-link>

      <router-link class="v4-cap v4-cap--huanzhu" to="/huanzhu">
        <span class="v4-cap-ico"><el-icon :size="26"><MagicStick /></el-icon></span>
        <span class="cap-body">
          <h3>一键幻筑 · AI 三维生成</h3>
          <p>用自然语言描述朝代、屋顶与色彩，AI 逐步营造出可交互的三维殿宇，并归入数字档案。</p>
          <span class="meta">异步任务 · 幂等防重</span>
        </span>
      </router-link>
    </div>

    <!-- ═══════════ ③ 技术可信度条（可核查事实，克制呈现） ═══════════ -->
    <div class="v4-sechead">
      <h2>技术可信度</h2>
      <span class="k">FACTS · 均可在本仓库核查</span>
    </div>
    <div class="v4-card feed-facts">
      <div v-for="g in factGroups" :key="g.title" class="feed-factcol">
        <h3 class="feed-facthd">{{ g.title }}</h3>
        <div v-for="row in g.rows" :key="row.k" class="v4-kv">
          <span class="k">{{ row.k }}</span>
          <span class="v" :class="row.tone ? `v--${row.tone}` : ''">{{ row.v }}</span>
        </div>
      </div>
    </div>

    <!-- ═══════════ ④ 精选数字档案 ═══════════ -->
    <div class="v4-sechead">
      <h2>精选数字档案</h2>
      <router-link class="more" to="/community">{{ moreLabel }}</router-link>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="v4-cards3" aria-busy="true" aria-label="正在加载数字档案">
      <div v-for="n in 3" :key="n" class="v4-card feed-skel">
        <el-skeleton animated>
          <template #template>
            <el-skeleton-item variant="image" class="feed-skel-img" />
            <div class="feed-skel-txt">
              <el-skeleton-item variant="h3" style="width: 72%" />
              <el-skeleton-item variant="text" style="width: 52%" />
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <!-- 错误 -->
    <div v-else-if="error" class="v4-card feed-error" role="alert">
      <h3>数字档案加载失败</h3>
      <p>{{ error }}</p>
      <button type="button" class="v4-btn v4-btn--outline v4-btn--sm" @click="loadFeatured">
        重新加载
      </button>
    </div>

    <!-- 空态：全新库没有已审核帖子，这里就是空的（不填充假档案） -->
    <div v-else-if="!posts.length" class="v4-empty">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M3 9.5L12 3.5l9 6" />
        <path d="M5.5 10.5v9h13v-9" />
        <path d="M9.5 19.5v-5h5v5" />
      </svg>
      <p>还没有公开的数字档案。</p>
      <p class="feed-empty-sub">第一件由你营造 —— 用一段文字生成殿宇，审核通过后即出现在这里。</p>
      <router-link class="v4-btn v4-btn--gold v4-btn--sm" to="/huanzhu">去一键幻筑</router-link>
    </div>

    <!-- 正常 -->
    <div v-else class="v4-cards3">
      <ArchiveCard
        v-for="post in posts"
        :key="post.postId"
        :to="`/archive/${post.postId}`"
        :title="post.title"
        :image="post.preview2dPath"
        :meta="post.meta"
        :tags="post.tags"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { MagicStick, Cpu } from '@element-plus/icons-vue'
import ArchiveCard from '@/components/ArchiveCard.vue'
import { communityApi } from '@/api/community'
import { recordsFallback } from '@/utils/pagination'
import { parseTags } from '@/utils/format'

/**
 * 营造志（首页 · 设计稿 F2）—— 回答「这个平台是什么、凭什么可信」。
 * 不是内容流：内容流已迁至 /community 匠人社区。
 *
 * 数据源只有一个：GET /api/v1/posts?page=1&size=3（仅 APPROVED 帖子，按 createdAt 倒序）。
 * 设计稿里的 1,284 / 3,920 / 12 三个统计后端并不存在，
 * 因此首屏数据条改为「可核查事实」，数字仅使用接口真实返回的 total。
 */

const FEATURED_SIZE = 3

const posts = ref([])
const archiveTotal = ref(null)
const loading = ref(true)
const error = ref('')

/* ═══ 首屏数据条：后端无统计接口，故只列仓库内可核查的技术事实 ═══
   来源：@RateLimit(maxCalls = 5)；pom.xml spring-boot-starter-parent 3.2.5；
        @google/model-viewer；MySQL / Redis 依赖；PostBriefResponse 字段。 */
const heroFacts = [
  { label: '解析引擎', value: 'VGGT 结构解析', small: true },
  { label: '每日解析额度', value: '5 次 / 人' },
  { label: '三维渲染', value: 'model-viewer · WebGL', small: true }
]

const factGroups = [
  {
    title: '解析链路',
    rows: [
      { k: '解析引擎', v: 'VGGT 结构解析', tone: 'jade' },
      { k: '转发目标', v: 'FastAPI :8000/v1/analyze' },
      { k: '每日额度', v: '5 次 / 人（Redis 计数）', tone: 'jade' }
    ]
  },
  {
    title: '三维与数据',
    rows: [
      { k: '渲染组件', v: '@google/model-viewer' },
      { k: '资产字段', v: 'glb3dPath · preview2dPath' },
      { k: '存储', v: 'MySQL 8.0 · Redis 7' }
    ]
  },
  {
    title: '服务端',
    rows: [
      { k: '框架', v: 'Spring Boot 3.2.5' },
      { k: '语言 / 运行时', v: 'Java 17' },
      { k: '鉴权', v: 'JWT · Spring Security' }
    ]
  }
]

/* ═══ 「查看全部」文案：total 未知时只写「查看全部」 ═══ */
const moreLabel = computed(() =>
  archiveTotal.value !== null ? `查看全部 ${formatNumber(archiveTotal.value)} 件` : '查看全部'
)

function formatNumber(n) {
  const num = Number(n)
  return Number.isFinite(num) ? num.toLocaleString('zh-CN') : String(n)
}

/* ═══ 卡片 meta：作者 + 点赞 + 评论 + 时间（全部来自 PostBriefResponse 真实字段） ═══ */
function formatDate(raw) {
  if (!raw) return ''
  const s = String(raw).replace('T', ' ')
  return s.slice(0, 10) // 后端格式为 yyyy-MM-dd HH:mm
}

function buildMeta(post) {
  return [
    post.authorNickname || '匿名匠人',
    `♥ ${post.likeCount ?? 0}`,
    `评论 ${post.commentCount ?? 0}`,
    formatDate(post.createdAt)
  ].filter(Boolean).join(' ｜ ')
}

/* ═══ 数据加载（四态由 loading / error / posts.length 驱动） ═══ */
async function loadFeatured() {
  loading.value = true
  error.value = ''
  try {
    const res = await communityApi.getPosts({ page: 1, size: FEATURED_SIZE })
    const data = res?.data
    posts.value = recordsFallback(data, []).map(post => ({
      ...post,
      tags: parseTags(post.tags),
      meta: buildMeta(post)
    }))
    // total 字段存在时才使用，否则不显示任何数字（不编造）
    archiveTotal.value = typeof data?.total === 'number' ? data.total : null
  } catch (e) {
    posts.value = []
    archiveTotal.value = null
    error.value = e?.message || '网络异常，请检查后端服务是否已启动。'
  } finally {
    loading.value = false
  }
}

onMounted(loadFeatured)
</script>

<style scoped>
/* ═══ 本视图的 .v4-btn / .v4-cap / .sechead .more 都是 router-link（锚点）═══
   style.css 未声明 a { text-decoration: none }，此处补齐，避免默认下划线。 */
a.v4-btn,
a.v4-cap,
a.more {
  text-decoration: none;
}
a.more { display: inline-block; }

/* ═══ 首屏数据条：值可能是文字而非纯数字，降一号并允许换行 ═══ */
.v4-statpanel .n--sm {
  font-size: 14px;
  line-height: 1.35;
  text-align: right;
}

/* ═══ 能力卡内层文本容器（.v4-cap 是 flex，需要一层承载标题/描述/元信息） ═══ */
.cap-body {
  display: block;
  min-width: 0;
  flex: 1;
}
.cap-body > h3 {
  font-family: var(--font-family-serif);
  font-size: 17px;
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--color-text-main);
}
.cap-body > p {
  font-size: 12px;
  line-height: 1.75;
  color: var(--color-text-muted);
}

/* ═══ 技术可信度条 ═══ */
.feed-facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--spacing-lg);
}
.feed-factcol {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.feed-facthd {
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.08em;
  color: var(--color-text-faint);
  padding-bottom: 8px;
  margin-bottom: 2px;
  border-bottom: 1px solid var(--color-border-light);
}
/* 键值行里的技术标识（如 FastAPI :8000/v1/analyze）在窄屏需可换行，避免横向溢出 */
.feed-factcol .v4-kv {
  align-items: flex-start;
}
.feed-factcol .v4-kv > .k {
  flex: 0 0 auto;
  white-space: nowrap;
}
.feed-factcol .v4-kv > .v {
  flex: 1 1 auto;
  min-width: 0;
  text-align: right;
  word-break: break-word;
}

/* ═══ 骨架（占位尺寸与 ArchiveCard 对齐，避免加载完成时跳动） ═══ */
.feed-skel {
  padding: 0;
  overflow: hidden;
}
/* Element Plus 对 .el-skeleton__image 设了 width:unset，需显式压过 */
.feed-skel-img {
  width: 100% !important;
  height: 158px !important;
  display: block;
  border-radius: 0;
}
.feed-skel-txt {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
}

/* ═══ 错误态 ═══ */
.feed-error {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}
.feed-error > h3 {
  font-family: var(--font-family-serif);
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-main);
}
.feed-error > p {
  font-size: 12px;
  line-height: 1.7;
  color: var(--color-text-muted);
  word-break: break-word;
}

/* ═══ 空态（比全局 .v4-empty 多一条出路说明） ═══ */
.feed-empty-sub {
  margin: 6px 0 18px;
  font-size: 12px;
  line-height: 1.75;
  color: var(--color-text-faint);
}

/* ═══ 本视图专属窄屏微调（全局 ≤900px 已处理 .v4-caps / .v4-cards3 / .v4-statpanel） ═══ */
@media (max-width: 900px) {
  .feed-facts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: var(--spacing-md);
  }
  .feed-factcol:last-child {
    grid-column: 1 / -1;
  }
}
</style>
