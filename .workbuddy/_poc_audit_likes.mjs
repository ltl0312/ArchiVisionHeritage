/**
 * 验证两个后端行为缺陷：
 *
 * ① 审核状态机是否真的受约束
 *    AdminController 的 javadoc 写明「状态机约束：PENDING → APPROVED / REJECTED」，
 *    但 PostServiceImpl.auditPost 只做 setStatus，不检查当前状态。
 *    预期（若有约束）：对已 APPROVED 的帖子再次审核应被拒。
 *
 * ② 「我的收藏」的顺序与状态过滤
 *    LikeServiceImpl.getUserLikedPosts：先按点赞时间分页取出 targetId，
 *    再用 postMapper.selectBatchIds(pageIds) 取帖子 —— selectBatchIds 不保证顺序，
 *    且没有任何 status 过滤。预期：应按点赞时间倒序，且不应包含已驳回的帖子。
 *
 * 用法：node .workbuddy/_poc_audit_likes.mjs [baseUrl]
 */
const BASE = (process.argv[2] || 'http://127.0.0.1:8088').replace(/\/$/, '')

const j = async (res) => { const t = await res.text(); try { return JSON.parse(t) } catch { return { raw: t.slice(0, 120) } } }

async function login(u, p) {
  const r = await fetch(`${BASE}/api/v1/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: u, password: p })
  })
  return (await j(r))?.data?.token
}
const auth = (t) => ({ Authorization: `Bearer ${t}` })

const admin = await login('admin', 'admin123')
if (!admin) { console.error('admin 登录失败'); process.exit(2) }

const stamp = Date.now().toString().slice(-7)
const USER = { username: `likes_${stamp}`, password: 'poc_pass_123', nickname: `收藏测试${stamp}` }
await fetch(`${BASE}/api/v1/auth/register`, {
  method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(USER)
})
const user = await login(USER.username, USER.password)
if (!user) { console.error('测试用户登录失败'); process.exit(2) }

const mkPost = async (title) => {
  await fetch(`${BASE}/api/v1/posts`, {
    method: 'POST', headers: { 'Content-Type': 'application/json', ...auth(admin) },
    body: JSON.stringify({ title, content: 'PoC', tags: 'PoC' })
  })
  const r = await fetch(`${BASE}/api/v1/posts?page=1&size=50`, { headers: auth(admin) })
  return (await j(r))?.data?.records?.find(p => p.title === title)?.postId
}

console.log(`\n══ 审核状态机 & 我的收藏 缺陷验证 ══  base=${BASE}\n`)

/* ─────────── ① 审核状态机 ─────────── */
console.log('① 审核状态机约束')
const t1 = `PoC 状态机 ${stamp}`
const p1 = await mkPost(t1)
console.log(`   建帖（管理员发帖自动 APPROVED）postId=${p1}`)

const audit = async (id, status, reason) => {
  const r = await fetch(`${BASE}/api/v1/admin/posts/${id}/audit`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json', ...auth(admin) },
    body: JSON.stringify({ status, rejectReason: reason ?? null })
  })
  return { status: r.status, body: await j(r) }
}
const statusOf = async (id) => (await j(await fetch(`${BASE}/api/v1/posts/${id}`, { headers: auth(admin) })))?.data

const a1 = await audit(p1, 'REJECTED', '第一次驳回')
console.log(`   对 APPROVED 帖子执行驳回 → HTTP ${a1.status}  ${JSON.stringify(a1.body).slice(0, 70)}`)

const a2 = await audit(p1, 'APPROVED')
console.log(`   对已 REJECTED 的帖子再次通过 → HTTP ${a2.status}  ${JSON.stringify(a2.body).slice(0, 70)}`)

const a3 = await audit(p1, 'PENDING')
console.log(`   试图把它改回 PENDING（非法转移）→ HTTP ${a3.status}  ${JSON.stringify(a3.body).slice(0, 70)}`)

const a4 = await audit(p1, 'APPROVED')
console.log(`   重复通过同一帖子 → HTTP ${a4.status}  ${JSON.stringify(a4.body).slice(0, 70)}`)

const anyIllegalAccepted = [a1, a2, a3, a4].some(x => x.status === 200)
console.log(anyIllegalAccepted
  ? '   ⚠️  状态机约束未被实现 —— 已发布/已驳回的帖子可被任意反复改判\n'
  : '   ✅ 非法转移均被拒（期望：APPROVED→驳回 409 / 已驳回→通过 409 / 改回 PENDING 400 / 重复通过 409）\n')

/* ─────────── ② 我的收藏 顺序 ─────────── */
console.log('② 「我的收藏」的顺序')
const titleA = `收藏A ${stamp}`
const titleB = `收藏B ${stamp}`
const pA = await mkPost(titleA)
const pB = await mkPost(titleB)
console.log(`   两帖已公开：A=${pA}, B=${pB}`)

// 先赞 A，再赞 B → 期望"我的收藏"里 B 在前（按点赞时间倒序）
await fetch(`${BASE}/api/v1/interactions/like`, {
  method: 'POST', headers: { 'Content-Type': 'application/json', ...auth(user) },
  body: JSON.stringify({ targetId: pA, targetType: 'POST' })
})
await new Promise(r => setTimeout(r, 1100))
await fetch(`${BASE}/api/v1/interactions/like`, {
  method: 'POST', headers: { 'Content-Type': 'application/json', ...auth(user) },
  body: JSON.stringify({ targetId: pB, targetType: 'POST' })
})

const likes1 = await j(await fetch(`${BASE}/api/v1/users/me/likes?page=1&size=10`, { headers: auth(user) }))
const order = (likes1?.data?.records || []).map(r => r.postId)
console.log(`   点赞顺序：A(${pA}) 先、B(${pB}) 后`)
console.log(`   「我的收藏」返回顺序：${JSON.stringify(order)}  （期望 [${pB},${pA}]）`)
const orderOk = order[0] === pB && order[1] === pA
console.log(orderOk ? '   ✅ 顺序正确\n' : '   ⚠️  顺序错误：未按点赞时间倒序（selectBatchIds 不保序）\n')

/* ─────────── ③ 我的收藏 是否过滤不可见内容 ─────────── */
console.log('③ 「我的收藏」是否包含「当前不可见」的帖子')
// 注：审核状态机修复后，已发布帖不可能再被改判为驳回，
//     因此这里用「待审帖被点赞」来构造场景（点赞接口目前不校验目标可见性）。
const titleP = `待审可赞 ${stamp}`
await fetch(`${BASE}/api/v1/posts`, {
  method: 'POST', headers: { 'Content-Type': 'application/json', ...auth(user) },
  body: JSON.stringify({ title: titleP, content: '待审内容', tags: '草稿' })
})
const pendingList = await j(await fetch(`${BASE}/api/v1/admin/posts/pending?page=1&size=50`, { headers: auth(admin) }))
const pP = (pendingList?.data?.records || []).find(p => p.title === titleP)?.postId
console.log(`   普通用户建帖（PENDING）postId=${pP}`)

await fetch(`${BASE}/api/v1/interactions/like`, {
  method: 'POST', headers: { 'Content-Type': 'application/json', ...auth(user) },
  body: JSON.stringify({ targetId: pP, targetType: 'POST' })
})
const likes2 = await j(await fetch(`${BASE}/api/v1/users/me/likes?page=1&size=10`, { headers: auth(user) }))
const hasInvisible = (likes2?.data?.records || []).some(r => r.postId === pP)
console.log(`   「我的收藏」包含这条不可见（PENDING）帖子：${hasInvisible ? '是' : '否'}`)
console.log(hasInvisible
  ? '   ⚠️  确认：未过滤可见性，用户收藏里会出现打不开的卡片（详情接口已返回 404）\n'
  : '   ✅ 已过滤\n')

/* ─────────── ③ 顺带：给不存在的帖子点赞 ─────────── */
console.log('③ 给不存在的帖子点赞（无目标存在性校验）')
const r = await fetch(`${BASE}/api/v1/interactions/like`, {
  method: 'POST', headers: { 'Content-Type': 'application/json', ...auth(user) },
  body: JSON.stringify({ targetId: 99999999, targetType: 'POST' })
})
console.log(`   HTTP ${r.status}  ${JSON.stringify(await j(r)).slice(0, 80)}`)
console.log(r.status === 200 ? '   ⚠️  接受：产生了指向不存在帖子的孤儿点赞记录\n' : '   ✅ 被拒\n')

/* ─────────── ④ 给不存在的帖子评论 ─────────── */
console.log('④ 给不存在的帖子评论')
const rc = await fetch(`${BASE}/api/v1/posts/99999999/comments`, {
  method: 'POST', headers: { 'Content-Type': 'application/json', ...auth(user) },
  body: JSON.stringify({ content: '孤儿评论' })
})
console.log(`   HTTP ${rc.status}  ${JSON.stringify(await j(rc)).slice(0, 100)}`)
console.log(rc.status === 200 ? '   ⚠️  接受：可为不存在的帖子创建评论\n' : '   ✅ 被拒\n')
