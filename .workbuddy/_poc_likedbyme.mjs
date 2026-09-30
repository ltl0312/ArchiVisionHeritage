/**
 * 验证 PostBriefResponse 是否缺少 likedByMe，以及它对信息流点赞态的影响。
 *
 * 前端 PostCard.vue 用 post.likedByMe 决定星标实心/空心与 .liked 类。
 * 若帖子流响应里没有该字段，刷新后点赞态恒为 false，
 * 用户再次点击"赞赏"时，乐观更新会把 UI 置为已赞，而后端 toggle 实际是**取消点赞** → 状态反转。
 */
const BASE = (process.argv[2] || 'http://127.0.0.1:8088').replace(/\/$/, '')
const j = async (r) => { const t = await r.text(); try { return JSON.parse(t) } catch { return { raw: t.slice(0, 100) } } }
const auth = (t) => ({ Authorization: `Bearer ${t}` })

const login = async (u, p) => (await j(await fetch(`${BASE}/api/v1/auth/login`, {
  method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: u, password: p })
})))?.data?.token

const admin = await login('admin', 'admin123')
const stamp = Date.now().toString().slice(-7)
const U = { username: `like_${stamp}`, password: 'poc_pass_123', nickname: `点赞态${stamp}` }
await fetch(`${BASE}/api/v1/auth/register`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(U) })
const user = await login(U.username, U.password)

console.log(`\n══ likedByMe 字段与点赞态验证 ══  base=${BASE}\n`)

// 造一个帖子并公开
const title = `点赞态PoC ${stamp}`
await fetch(`${BASE}/api/v1/posts`, {
  method: 'POST', headers: { 'Content-Type': 'application/json', ...auth(admin) },
  body: JSON.stringify({ title, content: 'PoC' })
})
const feed1 = await j(await fetch(`${BASE}/api/v1/posts?page=1&size=50`, { headers: auth(user) }))
const post = (feed1?.data?.records || []).find(p => p.title === title)
if (!post) { console.error('未找到测试帖'); process.exit(2) }

console.log('① 帖子流响应字段清单')
console.log(`   ${Object.keys(post).sort().join(', ')}`)
const hasField = 'likedByMe' in post
console.log(hasField
  ? `   ✅ 含 likedByMe = ${post.likedByMe}\n`
  : '   ⚠️  不含 likedByMe —— 前端 PostCard 的点赞态将恒为 false\n')

console.log('② 详情响应是否含 likedByMe（对照）')
const detail = (await j(await fetch(`${BASE}/api/v1/posts/${post.postId}`, { headers: auth(user) })))?.data
console.log(`   详情字段含 likedByMe = ${'likedByMe' in (detail || {})}，值 = ${detail?.likedByMe}\n`)

console.log('③ 实际点赞后，再取帖子流看点赞态能否体现')
await fetch(`${BASE}/api/v1/interactions/like`, {
  method: 'POST', headers: { 'Content-Type': 'application/json', ...auth(user) },
  body: JSON.stringify({ targetId: post.postId, targetType: 'POST' })
})
const feed2 = await j(await fetch(`${BASE}/api/v1/posts?page=1&size=50`, { headers: auth(user) }))
const post2 = (feed2?.data?.records || []).find(p => p.postId === post.postId)
console.log(`   已点赞后 likeCount = ${post2?.likeCount}，likedByMe = ${post2?.likedByMe}`)
const detail2 = (await j(await fetch(`${BASE}/api/v1/posts/${post.postId}`, { headers: auth(user) })))?.data
console.log(`   详情 likedByMe = ${detail2?.likedByMe}（详情是对的）`)
console.log(!hasField
  ? '\n   ⚠️  结论：帖子流无法表达"我是否已赞"。用户在信息流再点一次"赞赏"，\n      乐观更新会把 UI 变成已赞，而后端 toggle 实际执行的是**取消点赞** → 点赞态反转\n'
  : '\n   ✅ 帖子流可表达点赞态\n')

// 清理：取消点赞
await fetch(`${BASE}/api/v1/interactions/like`, {
  method: 'POST', headers: { 'Content-Type': 'application/json', ...auth(user) },
  body: JSON.stringify({ targetId: post.postId, targetType: 'POST' })
})
