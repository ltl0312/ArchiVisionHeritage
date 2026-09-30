/**
 * 验证：未审核（PENDING）与已驳回（REJECTED）的帖子是否可被**匿名**读取。
 *
 * 依据：
 *   SecurityConfig.java:45  → GET /api/v1/posts/**  permitAll（无需登录）
 *   PostServiceImpl.java:57 → getPostDetail 只做 null 检查，**不按 status 过滤**
 *
 * 若成立：任何未登录的人只要枚举 id，就能读到尚未通过审核的内容（含被驳回内容）。
 */
const BASE = (process.argv[2] || 'http://127.0.0.1:8088').replace(/\/$/, '')
const j = async (r) => { const t = await r.text(); try { return JSON.parse(t) } catch { return { raw: t.slice(0, 100) } } }
const auth = (t) => ({ Authorization: `Bearer ${t}` })
const login = async (u, p) => (await j(await fetch(`${BASE}/api/v1/auth/login`, {
  method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: u, password: p })
})))?.data?.token

const admin = await login('admin', 'admin123')
const stamp = Date.now().toString().slice(-7)
const U = { username: `vis_${stamp}`, password: 'poc_pass_123', nickname: `可见性${stamp}` }
await fetch(`${BASE}/api/v1/auth/register`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(U) })
const user = await login(U.username, U.password)

console.log(`\n══ 未审核内容可见性验证 ══  base=${BASE}\n`)

const SECRET = `内部草稿-不应公开-${stamp}`

// 普通用户发帖 → PENDING
await fetch(`${BASE}/api/v1/posts`, {
  method: 'POST', headers: { 'Content-Type': 'application/json', ...auth(user) },
  body: JSON.stringify({ title: `待审草稿 ${stamp}`, content: SECRET, tags: '草稿' })
})

// 从管理员待审队列里拿到 id（列表接口是安全的，只给管理员）
const pending = await j(await fetch(`${BASE}/api/v1/admin/posts/pending?page=1&size=50`, { headers: auth(admin) }))
const target = (pending?.data?.records || []).find(p => p.title === `待审草稿 ${stamp}`)
if (!target) { console.error('未在待审队列找到测试帖'); process.exit(2) }
const id = target.postId

console.log(`① 公开流（GET /api/v1/posts）是否包含待审帖`)
const feed = await j(await fetch(`${BASE}/api/v1/posts?page=1&size=50`))
const inFeed = (feed?.data?.records || []).some(p => p.postId === id)
console.log(`   ${inFeed ? '⚠️  包含（不应包含）' : '✅ 未包含'} —— 列表接口的 status 过滤是生效的\n`)

console.log(`② 匿名直接按 id 读取详情（GET /api/v1/posts/${id}，不带任何 token）`)
const anon = await fetch(`${BASE}/api/v1/posts/${id}`)
const body = await j(anon)
console.log(`   HTTP ${anon.status}  code=${body?.code}`)
console.log(`   标题: ${body?.data?.title}`)
console.log(`   正文: ${JSON.stringify(body?.data?.content)}`)
const leaked = body?.data?.content === SECRET
console.log(leaked
  ? '\n   ⚠️  确认：未登录用户可读到**尚未通过审核**的帖子全文（内容泄露）。\n' +
    '      因为 /api/v1/posts/** 是 permitAll，且 getPostDetail 不校验 status。\n'
  : '\n   ✅ 未泄露\n')

console.log(`③ 被驳回后是否仍可匿名读取`)
await fetch(`${BASE}/api/v1/admin/posts/${id}/audit`, {
  method: 'PUT', headers: { 'Content-Type': 'application/json', ...auth(admin) },
  body: JSON.stringify({ status: 'REJECTED', rejectReason: 'PoC 驳回' })
})
const anon2 = await fetch(`${BASE}/api/v1/posts/${id}`)
const body2 = await j(anon2)
console.log(`   HTTP ${anon2.status}  标题=${body2?.data?.title}`)
console.log(body2?.data?.content === SECRET
  ? '   ⚠️  确认：被驳回的内容依然可被匿名读取（审核形同虚设）\n'
  : '   ✅ 已不可读\n')

console.log('④ 对照：待审帖是否出现在「我的档案」里（作者自己可见，符合预期）')
const mine = await j(await fetch(`${BASE}/api/v1/users/me/posts?page=1&size=50`, { headers: auth(user) }))
console.log(`   我的档案含该帖：${(mine?.data?.records || []).some(p => p.postId === id) ? '是 ✅' : '否'}\n`)
