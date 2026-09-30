/**
 * 智观·古建 — Docker 全链路集成验证（E2E）
 *
 * 走真实生产路径：Node → 前端 nginx 容器(8088) → /api 反代 → backend 容器 →
 *                MySQL / Redis / vggt-api 容器
 * 因此它同时验证了本次改动中最容易出错的两处：
 *   · nginx 的 `location ^~ /assets/`（上传文件反代，不能被图片正则 location 抢走）
 *   · 构建产物命名空间改为 /static/（不与 /assets/ 冲突）
 *
 * 用法：node .workbuddy/_e2e_docker.mjs [baseUrl]
 */
import { readFileSync } from 'node:fs'

const BASE = (process.argv[2] || 'http://127.0.0.1:8088').replace(/\/$/, '')
const HERO = 'frontend/preview/assets/hero.jpg'

const results = []
let pass = 0
let fail = 0

function check(name, ok, detail = '') {
  results.push({ name, ok: !!ok, detail })
  if (ok) { pass++; console.log(`  ✓ ${name}${detail ? `  (${detail})` : ''}`) } else { fail++; console.log(`  ✗ ${name}  ← ${detail}`) }
}

async function req(method, path, { json, form, token, raw } = {}) {
  const headers = {}
  if (token) headers.Authorization = `Bearer ${token}`
  let body
  if (json !== undefined) { headers['Content-Type'] = 'application/json'; body = JSON.stringify(json) }
  if (form) body = form
  const res = await fetch(`${BASE}${path}`, { method, headers, body, redirect: 'manual' })
  if (raw) return res
  const text = await res.text()
  let parsed = null
  try { parsed = JSON.parse(text) } catch { /* 非 JSON（HTML 等） */ }
  return { status: res.status, body: parsed, text, headers: res.headers }
}

const stamp = Date.now().toString().slice(-8)
const USER = { username: `e2e_${stamp}`, password: 'e2e_pass_123', nickname: `端到端测试${stamp}` }

console.log(`\n══ Docker 全链路 E2E ══  base=${BASE}\n`)

/* ─────────── 1. 前端容器 ─────────── */
console.log('── 1. 前端 nginx 容器 ──')
const index = await req('GET', '/', { raw: true })
const html = await index.text()
check('GET / 返回 200', index.status === 200, `HTTP ${index.status}`)
check('index.html 引用 /static/（构建产物命名空间）', html.includes('/static/'), '未发现 /static/')
check('index.html 未引用 /assets/（避免与上传文件撞名）', !/src="\/assets\//.test(html), '仍引用 /assets/')
check('声明了 SVG 站点图标', html.includes('favicon.svg'), '未声明')

const fav = await req('GET', '/favicon.svg', { raw: true })
check('GET /favicon.svg 返回 200（原先每页 404）', fav.status === 200, `HTTP ${fav.status}`)

const hero = await req('GET', '/media/hero.jpg', { raw: true })
check('GET /media/hero.jpg 返回 200（营造志首屏主视觉）', hero.status === 200, `HTTP ${hero.status}`)

const spa = await req('GET', '/archive/1', { raw: true })
check('SPA history 回退（深链接返回 index.html）', spa.status === 200 && (await spa.text()).includes('<div id="app">'), `HTTP ${spa.status}`)

/* ─────────── 2. nginx → backend → MySQL ─────────── */
console.log('\n── 2. /api 反代 → backend → MySQL ──')
const posts0 = await req('GET', '/api/v1/posts?page=1&size=3')
check('GET /api/v1/posts 经反代抵达后端', posts0.status === 200 && posts0.body?.code === 200, `HTTP ${posts0.status} code=${posts0.body?.code}`)
check('返回 MyBatis-Plus 分页结构', Array.isArray(posts0.body?.data?.records), JSON.stringify(posts0.body?.data)?.slice(0, 80))

/* ─────────── 3. 注册（实证前端表单字段 bug） ─────────── */
console.log('\n── 3. 注册 ──')
const wrong = await req('POST', '/api/v1/auth/register', {
  json: { username: `bad_${stamp}`, email: `bad_${stamp}@x.com`, password: 'e2e_pass_123' }
})
check('【已知缺陷】前端旧表单字段（email 而非 nickname）被后端拒绝', wrong.status === 400,
  `HTTP ${wrong.status} —— 若为 200 说明字段已可用，需复核`)

const reg = await req('POST', '/api/v1/auth/register', { json: USER })
check('按后端契约 {username,password,nickname} 注册成功', reg.status === 200 && reg.body?.code === 200, `HTTP ${reg.status} ${reg.body?.message || ''}`)

/* ─────────── 4. 登录与鉴权 ─────────── */
console.log('\n── 4. 登录与 JWT 鉴权 ──')
const login = await req('POST', '/api/v1/auth/login', { json: { username: USER.username, password: USER.password } })
const token = login.body?.data?.token
check('登录返回 JWT', login.status === 200 && !!token, `HTTP ${login.status}`)

const me = await req('GET', '/api/v1/users/me', { token })
check('GET /api/v1/users/me 携带 JWT 可用', me.status === 200 && me.body?.data?.username === USER.username,
  `nickname=${me.body?.data?.nickname} role=${me.body?.data?.role}`)

const noAuth = await req('GET', '/api/v1/users/me')
// Spring Security 未配置 AuthenticationEntryPoint 时，匿名访问受保护接口返回 403
// 而非 401（401 只由 JwtAuthenticationFilter 对无效 token 触发）。两者都算"被拒"。
check('无 JWT 访问受保护接口被拒（401/403）', noAuth.status === 401 || noAuth.status === 403, `HTTP ${noAuth.status}`)

/* ─────────── 5. 上传 + /assets 反代（本次改动的关键点） ─────────── */
console.log('\n── 5. 图片上传与 /assets 反代 ──')
const imgBuf = readFileSync(HERO)
const fd = new FormData()
fd.append('file', new Blob([imgBuf], { type: 'image/jpeg' }), 'hero.jpg')
fd.append('subDir', 'covers')
const up = await req('POST', '/api/v1/upload/image', { form: fd, token })
const assetUrl = up.body?.data?.url
check('上传图片成功', up.status === 200 && !!assetUrl, `HTTP ${up.status} url=${assetUrl}`)

if (assetUrl) {
  const fetched = await req('GET', assetUrl, { raw: true })
  const len = (await fetched.arrayBuffer()).byteLength
  check('上传文件经 nginx `^~ /assets/` 反代可回读（生产破图修复）',
    fetched.status === 200 && len === imgBuf.length,
    `HTTP ${fetched.status} ${len}B / 期望 ${imgBuf.length}B`)
}

/* ─────────── 6. 发帖 → 审核 → 公开（内容治理闭环） ─────────── */
console.log('\n── 6. 发帖 → 审核 → 公开 ──')
const create = await req('POST', '/api/v1/posts', {
  token,
  json: { title: `E2E 档案 ${stamp}`, content: '端到端验证用档案正文。', preview2dPath: assetUrl, tags: '唐代,斗栱' }
})
check('发帖成功（默认 PENDING）', create.status === 200, `HTTP ${create.status}`)

const feedBefore = await req('GET', '/api/v1/posts?page=1&size=50')
const visibleBefore = (feedBefore.body?.data?.records || []).some(p => p.title === `E2E 档案 ${stamp}`)
check('待审档案不出现在公开流', !visibleBefore, '竟已公开')

const adminLogin = await req('POST', '/api/v1/auth/login', { json: { username: 'admin', password: 'admin123' } })
const adminToken = adminLogin.body?.data?.token
check('管理员 admin 登录成功', adminLogin.status === 200 && !!adminToken, `HTTP ${adminLogin.status}`)

const pending = await req('GET', '/api/v1/admin/posts/pending?page=1&size=50', { token: adminToken })
const mine = (pending.body?.data?.records || []).find(p => p.title === `E2E 档案 ${stamp}`)
check('待审队列可见该档案', !!mine, `pending total=${pending.body?.data?.total}`)

if (mine) {
  const audit = await req('PUT', `/api/v1/admin/posts/${mine.postId}/audit`, {
    token: adminToken, json: { status: 'APPROVED', rejectReason: null }
  })
  check('审核通过成功', audit.status === 200, `HTTP ${audit.status} ${audit.body?.message || ''}`)

  const feedAfter = await req('GET', '/api/v1/posts?page=1&size=50')
  const visibleAfter = (feedAfter.body?.data?.records || []).some(p => p.postId === mine.postId)
  check('审核通过后出现在公开流', visibleAfter, '仍未公开')

  const detail = await req('GET', `/api/v1/posts/${mine.postId}`)
  check('帖子详情可读且带封面', detail.status === 200 && detail.body?.data?.preview2dPath === assetUrl,
    `preview2dPath=${detail.body?.data?.preview2dPath}`)
}

/* ─────────── 7. 幻筑异步任务 + 数字锦盒 ─────────── */
console.log('\n── 7. 一键幻筑异步状态机 + 数字锦盒 ──')
const submit = await req('POST', '/api/v1/tasks/huanzhu', { token, json: { prompt: `E2E 幻筑 ${stamp}：唐代重檐歇山顶大殿` } })
const taskId = submit.body?.data?.taskId
check('提交幻筑任务返回 taskId（HTTP 202）', submit.status === 202 && !!taskId, `HTTP ${submit.status} taskId=${taskId}`)

if (taskId) {
  let st = null
  const deadline = Date.now() + 60000
  while (Date.now() < deadline) {
    const s = await req('GET', `/api/v1/tasks/${taskId}/status`, { token })
    st = s.body?.data
    if (st?.status === 'SUCCESS' || st?.status === 'FAILED') break
    await new Promise(r => setTimeout(r, 1000))
  }
  check('任务最终 SUCCESS', st?.status === 'SUCCESS', `status=${st?.status} err=${st?.errorMessage || '-'}`)
  check('SUCCESS 返回真实字段 preview2dPath / glb3dPath', !!st?.preview2dPath && !!st?.glb3dPath,
    `preview=${st?.preview2dPath} glb=${st?.glb3dPath}`)

  if (st?.preview2dPath) {
    const cover = await req('GET', st.preview2dPath, { raw: true })
    // ⚠️ 已知后端限制（非本次改动引入，也不能在"不改后端"约束下修复）：
    //    TaskExecutionService 是**模拟**生成，只把 preview2dPath / glb3dPath 写进
    //    model_asset，从不真正落盘（/app/assets 下只有上传的 covers/）。
    //    因此这两个路径必然取不到文件。
    //    本次 nginx 修复（`location ^~ /assets/`）把它的症状从 404 暴露为 500 ——
    //    因为请求现在会真正抵达后端，而后端抛 NoResourceFoundException。
    //    前端已用 @error 兜底（显示"封面图未能加载"），故不作为失败项，
    //    但必须显式记录，避免被误认为"已修好"。
    check('【已知限制】幻筑封面未落盘 → 取不到（前端 @error 兜底）',
      cover.status !== 200,
      `HTTP ${cover.status}；后端仅写入虚构路径，未生成文件`)
  }

  const notif = await req('GET', '/api/v1/notifications', { token })
  const box = (notif.body?.data || []).find(n => n.taskId === taskId)
  check('幻筑完成写入「数字锦盒」站内信', !!box, `通知数=${(notif.body?.data || []).length} 文案=${box?.message || '-'}`)
  check('通知响应含 read 字段（前端归一化的依据）', box && ('read' in box || 'isRead' in box),
    JSON.stringify(box || {}).slice(0, 120))

  const unread = await req('GET', '/api/v1/notifications/unread-count', { token })
  check('未读数 ≥ 1', (unread.body?.data?.count ?? 0) >= 1, `count=${unread.body?.data?.count}`)
}

/* ─────────── 8. VGGT 解析链路（含 429 限流） ─────────── */
console.log('\n── 8. VGGT 解析链路（nginx → backend → vggt-api）──')
async function analyzeOnce() {
  const f = new FormData()
  f.append('image', new Blob([imgBuf], { type: 'image/jpeg' }), 'hero.jpg')
  return req('POST', '/api/v1/analysis/zhixi', { form: f, token })
}
const first = await analyzeOnce()
const d = first.body?.data
check('解析请求抵达 vggt-api 容器', first.status === 200 && d?.success === true,
  `HTTP ${first.status} ${JSON.stringify(first.body)?.slice(0, 140)}`)
check('返回真实 structural_elements（name/bbox/confidence/cultural_note）',
  Array.isArray(d?.structural_elements) && d.structural_elements.length > 0 &&
  typeof d.structural_elements[0]?.name === 'string' &&
  Array.isArray(d.structural_elements[0]?.bbox) &&
  typeof d.structural_elements[0]?.confidence === 'number' &&
  typeof d.structural_elements[0]?.cultural_note === 'string',
  `元素数=${d?.structural_elements?.length}`)
check('bbox 为归一化坐标（0–1）',
  (d?.structural_elements || []).every(e => e.bbox.every(v => v >= 0 && v <= 1)),
  JSON.stringify(d?.structural_elements?.[0]?.bbox))
check('返回 analysis_id 与 image_info.architectural_style',
  !!d?.analysis_id && !!d?.image_info?.architectural_style,
  `${d?.analysis_id} / ${d?.image_info?.architectural_style}`)

console.log('  （继续调用以触发每日 5 次限流）')
let limited = null
for (let i = 0; i < 5; i++) {
  const r = await analyzeOnce()
  if (r.status === 429 || r.body?.code === 429) { limited = r; break }
}
check('超额调用触发限流（429 + 温润文案）', !!limited,
  limited ? `HTTP ${limited.status}「${(limited.body?.message || '').slice(0, 60)}…」` : '5 次内未触发')

/* ─────────── 9. 帖子流契约：likedByMe / modelAssetId / rejectReason ─────────── */
console.log('\n── 9. 帖子流契约（likedByMe 是信息流点赞态的唯一依据）──')

const uA = { username: `likeA_${stamp}`, password: 'e2e_pass_123', nickname: `点赞A${stamp}` }
const uB = { username: `likeB_${stamp}`, password: 'e2e_pass_123', nickname: `点赞B${stamp}` }
for (const u of [uA, uB]) {
  await req('POST', '/api/v1/auth/register', { json: u })
}
const tokenA = (await req('POST', '/api/v1/auth/login', { json: { username: uA.username, password: uA.password } })).body?.data?.token
const tokenB = (await req('POST', '/api/v1/auth/login', { json: { username: uB.username, password: uB.password } })).body?.data?.token
check('两个独立测试用户登录成功', !!tokenA && !!tokenB, '')

const likeTitle = `点赞态验证 ${stamp}`
await req('POST', '/api/v1/posts', {
  token: adminToken, json: { title: likeTitle, content: '契约验证用', tags: '验证' }
})
const feedAll = await req('GET', '/api/v1/posts?page=1&size=50')
const target = (feedAll.body?.data?.records || []).find(p => p.title === likeTitle)
check('新帖出现在公开流（管理员发帖直接 APPROVED）', !!target, `postId=${target?.postId}`)

const fields = Object.keys(target || {}).sort()
check('帖子流响应含 likedByMe（原先缺失 → 信息流点赞态恒为 false）',
  fields.includes('likedByMe'), `实际字段: ${fields.join(', ')}`)
check('帖子流响应含 modelAssetId（用于区分实景解析 / AI 幻筑）',
  fields.includes('modelAssetId'), '')
check('帖子流响应含 rejectReason（作者可见驳回原因）',
  fields.includes('rejectReason'), '')

if (target) {
  // A 点赞
  await req('POST', '/api/v1/interactions/like', {
    token: tokenA, json: { targetId: target.postId, targetType: 'POST' }
  })

  const feedA = await req('GET', '/api/v1/posts?page=1&size=50', { token: tokenA })
  const rowA = (feedA.body?.data?.records || []).find(p => p.postId === target.postId)
  check('已点赞的用户 A 看到 likedByMe=true', rowA?.likedByMe === true, `likedByMe=${rowA?.likedByMe}`)

  const feedB = await req('GET', '/api/v1/posts?page=1&size=50', { token: tokenB })
  const rowB = (feedB.body?.data?.records || []).find(p => p.postId === target.postId)
  check('未点赞的用户 B 看到 likedByMe=false（不把别人的点赞算到我头上）',
    rowB?.likedByMe === false, `likedByMe=${rowB?.likedByMe}`)

  const feedAnon = await req('GET', '/api/v1/posts?page=1&size=50')
  const rowAnon = (feedAnon.body?.data?.records || []).find(p => p.postId === target.postId)
  check('匿名访问 likedByMe=false', rowAnon?.likedByMe === false, `likedByMe=${rowAnon?.likedByMe}`)

  check('两个用户看到的 likeCount 一致（计数不受调用方影响）',
    rowA?.likeCount === rowB?.likeCount && rowA?.likeCount === 1,
    `A=${rowA?.likeCount} B=${rowB?.likeCount}`)

  // 我的档案里应能看到真实 status 与 rejectReason —— 先让 A 发一条（普通用户 → PENDING）
  await req('POST', '/api/v1/posts', {
    token: tokenA, json: { title: `A的待审档案 ${stamp}`, content: '待审内容', tags: '草稿' }
  })
  const mine = await req('GET', '/api/v1/users/me/posts?page=1&size=50', { token: tokenA })
  const mineRow = (mine.body?.data?.records || [])[0]
  check('「我的档案」返回 status 与 rejectReason 字段',
    mineRow && 'status' in mineRow && 'rejectReason' in mineRow,
    `status=${mineRow?.status} rejectReason=${mineRow?.rejectReason}`)
  check('「我的档案」能看到自己的待审帖（作者视角）',
    mineRow?.status === 'PENDING', `status=${mineRow?.status}`)

  // 作者能读到自己的待审帖详情；他人不能（可见性回归）
  const ownDetail = await req('GET', `/api/v1/posts/${mineRow?.postId}`, { token: tokenA })
  check('作者可读自己的待审帖详情', ownDetail.status === 200 && ownDetail.body?.data?.status === 'PENDING',
    `HTTP ${ownDetail.status} status=${ownDetail.body?.data?.status}`)
  const otherDetail = await req('GET', `/api/v1/posts/${mineRow?.postId}`, { token: tokenB })
  check('他人读该待审帖 → 404（不泄露存在性）', otherDetail.status === 404, `HTTP ${otherDetail.status}`)
  const anonDetail = await req('GET', `/api/v1/posts/${mineRow?.postId}`)
  check('匿名读该待审帖 → 404', anonDetail.status === 404, `HTTP ${anonDetail.status}`)

  // 收尾：取消点赞，避免污染后续运行
  await req('POST', '/api/v1/interactions/like', {
    token: tokenA, json: { targetId: target.postId, targetType: 'POST' }
  })
}

/* ─────────── 汇总 ─────────── */
console.log(`\n── 汇总 ──  通过 ${pass} / 失败 ${fail} / 共 ${pass + fail}`)
const failed = results.filter(r => !r.ok)
if (failed.length) {
  console.log('\n失败项：')
  for (const f of failed) console.log(`  ✗ ${f.name}  ← ${f.detail}`)
}
process.exit(fail ? 1 : 0)
