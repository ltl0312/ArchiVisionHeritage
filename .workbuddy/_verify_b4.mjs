/**
 * B4 健壮性修复验证：鉴权响应契约 / IDOR / 参数校验 / 404 映射 / 日志治理
 *
 * 用法：node .workbuddy/_verify_b4.mjs [baseUrl]
 */
import { createHmac, randomBytes } from 'node:crypto'

const BASE = (process.argv[2] || 'http://127.0.0.1:8088').replace(/\/$/, '')
const results = []
let pass = 0, fail = 0

function check(name, ok, detail = '') {
  results.push({ name, ok: !!ok, detail })
  if (ok) { pass++; console.log(`  ✓ ${name}${detail ? `  (${detail})` : ''}`) }
  else { fail++; console.log(`  ✗ ${name}  ← ${detail}`) }
}

async function req(method, path, { json, token, raw } = {}) {
  const headers = {}
  if (token) headers.Authorization = `Bearer ${token}`
  if (json !== undefined) headers['Content-Type'] = 'application/json'
  const res = await fetch(`${BASE}${path}`, { method, headers, body: json !== undefined ? JSON.stringify(json) : undefined })
  if (raw) return res
  const text = await res.text()
  let body = null
  try { body = JSON.parse(text) } catch { /* 非 JSON */ }
  return { status: res.status, body, text }
}

const b64u = (b) => Buffer.from(b).toString('base64url')
function sign(payload, secret) {
  const h = b64u(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
  const p = b64u(JSON.stringify(payload))
  const s = createHmac('sha256', secret).update(`${h}.${p}`).digest()
  return `${h}.${p}.${b64u(s)}`
}

const login = async (u, p) =>
  (await req('POST', '/api/v1/auth/login', { json: { username: u, password: p } })).body?.data?.token

const stamp = Date.now().toString().slice(-7)
const admin = await login('admin', 'admin123')
const U = { username: `b4_${stamp}`, password: 'b4_pass_123', nickname: `B4验证${stamp}` }
await req('POST', '/api/v1/auth/register', { json: U })
const user = await login(U.username, U.password)

console.log(`\n══ B4 健壮性验证 ══  base=${BASE}\n`)

/* ─────────── 1. 鉴权响应契约（原先 403 且无 body）─────────── */
console.log('1. 鉴权响应契约（401/403 必须有 body 且符合 {code,message,data}）')

const anon = await req('GET', '/api/v1/users/me')
check('未登录 → 401（原先 403，导致前端不跳登录）', anon.status === 401, `HTTP ${anon.status}`)
check('未登录响应含统一 body', anon.body?.code === 401 && !!anon.body?.message,
  JSON.stringify(anon.body))

const expired = sign({ sub: '1', username: 'x', role: 'USER', exp: Math.floor(Date.now() / 1000) - 60 },
  process.env.TEST_SECRET || randomBytes(48).toString('hex'))
const expRes = await req('GET', '/api/v1/users/me', { token: expired })
check('过期/无效 token → 401 + body', expRes.status === 401 && expRes.body?.code === 401,
  `HTTP ${expRes.status} ${JSON.stringify(expRes.body)?.slice(0, 60)}`)

const forbidden = await req('GET', '/api/v1/admin/posts/pending', { token: user })
check('普通用户访问管理接口 → 403 + body（已认证但无权限）',
  forbidden.status === 403 && forbidden.body?.code === 403 && !!forbidden.body?.message,
  `HTTP ${forbidden.status} ${JSON.stringify(forbidden.body)?.slice(0, 70)}`)

check('管理员可正常访问管理接口', (await req('GET', '/api/v1/admin/posts/pending', { token: admin })).status === 200, '')

/* ─────────── 2. IDOR（任务状态归属校验）─────────── */
console.log('\n2. 任务状态归属校验（IDOR）')
const submit = await req('POST', '/api/v1/tasks/huanzhu', {
  token: user, json: { prompt: `B4 归属验证 ${stamp}：唐代大殿` }
})
const taskId = submit.body?.data?.taskId
check('用户 A 提交任务成功', submit.status === 202 && !!taskId, `taskId=${taskId}`)

if (taskId) {
  const own = await req('GET', `/api/v1/tasks/${taskId}/status`, { token: user })
  check('本人查询自己的任务 → 200', own.status === 200 && own.body?.data?.taskId === taskId, `HTTP ${own.status}`)

  const U2 = { username: `b4b_${stamp}`, password: 'b4_pass_123', nickname: `B4他人${stamp}` }
  await req('POST', '/api/v1/auth/register', { json: U2 })
  const other = await login(U2.username, U2.password)

  const stolen = await req('GET', `/api/v1/tasks/${taskId}/status`, { token: other })
  check('他人枚举 taskId 查询 → 404（原先可读到资产路径）', stolen.status === 404, `HTTP ${stolen.status}`)
  check('越权响应不含资产路径', !stolen.text.includes('glb') && !stolen.text.includes('preview'),
    stolen.text.slice(0, 80))

  const anonTask = await req('GET', `/api/v1/tasks/${taskId}/status`)
  check('匿名查询任务状态 → 401', anonTask.status === 401, `HTTP ${anonTask.status}`)
}

/* ─────────── 3. 参数校验（原先撞 DB 约束变 500）─────────── */
console.log('\n3. 参数校验')
const longPrompt = await req('POST', '/api/v1/tasks/huanzhu', {
  token: user, json: { prompt: '唐'.repeat(20000) }
})
check('超长 Prompt → 400（原先 500）', longPrompt.status === 400,
  `HTTP ${longPrompt.status} ${JSON.stringify(longPrompt.body)?.slice(0, 70)}`)

const okPrompt = await req('POST', '/api/v1/tasks/huanzhu', {
  token: user, json: { prompt: '唐'.repeat(500) }
})
check('512 字以内 Prompt 正常受理', okPrompt.status === 202, `HTTP ${okPrompt.status}`)

const badType = await req('POST', '/api/v1/interactions/like', {
  token: user, json: { targetId: 1, targetType: 'EVIL' }
})
check('非法 targetType → 400（原先撞 ENUM 变 500）', badType.status === 400,
  `HTTP ${badType.status} ${JSON.stringify(badType.body)?.slice(0, 70)}`)

/* ─────────── 4. 静态资源 404（原先 500）─────────── */
console.log('\n4. 静态资源不存在的响应码')
const missing = await req('GET', '/assets/preview/definitely-not-exist.png', { raw: true })
check('不存在的 /assets 资源 → 404（原先 500「系统内部错误」）', missing.status === 404, `HTTP ${missing.status}`)

/* ─────────── 5. 注册并发/重复（DuplicateKey 兜底）─────────── */
console.log('\n5. 重复注册')
const dup = await req('POST', '/api/v1/auth/register', { json: U })
check('重复用户名注册 → 400（而非 500）', dup.status === 400,
  `HTTP ${dup.status} ${JSON.stringify(dup.body)?.slice(0, 60)}`)

/* ─────────── 汇总 ─────────── */
console.log(`\n── 汇总 ──  通过 ${pass} / 失败 ${fail} / 共 ${pass + fail}`)
if (fail) {
  console.log('\n失败项：')
  for (const f of results.filter(r => !r.ok)) console.log(`  ✗ ${f.name}  ← ${f.detail}`)
}
process.exit(fail ? 1 : 0)
