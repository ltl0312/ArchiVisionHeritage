/**
 * 验证「幂等防重」是否真的生效（CLAUDE.md 与 README 都把幂等锁当作核心能力宣传）
 *
 * 设计意图：同一用户提交相同 Prompt 时，Redis + Lua 原子拦截，返回已有 taskId
 *          且 duplicate=true，避免双重扣费。
 *
 * 本脚本在任务仍处于 PENDING/RUNNING（模拟耗时 3–7s）时立刻重复提交同一 Prompt，
 * 检查第二次是否命中幂等。同时检查 Prompt 是否有长度上限。
 *
 * 用法：node .workbuddy/_poc_idempotency.mjs [baseUrl]
 */
const BASE = (process.argv[2] || 'http://127.0.0.1:8088').replace(/\/$/, '')

const login = await (await fetch(`${BASE}/api/v1/auth/login`, {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'admin123' })
})).json()
const token = login?.data?.token
if (!token) { console.error('登录失败'); process.exit(2) }

const submit = async (prompt) => {
  const res = await fetch(`${BASE}/api/v1/tasks/huanzhu`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify({ prompt })
  })
  const text = await res.text()
  let j = null
  try { j = JSON.parse(text) } catch { /* ignore */ }
  return { status: res.status, taskId: j?.data?.taskId, duplicate: j?.data?.duplicate, body: text.slice(0, 140) }
}

console.log(`\n══ 幂等防重验证 ══  base=${BASE}\n`)

/* ── 1. 并发重复提交同一 Prompt ── */
const prompt = `幂等验证 ${Date.now()}：唐代重檐歇山顶大殿`
console.log('1. 连续提交同一 Prompt（任务仍在 3–7s 模拟生成中）')
const a = await submit(prompt)
console.log(`   第 1 次: HTTP ${a.status}  taskId=${a.taskId}  duplicate=${a.duplicate}`)
const b = await submit(prompt)
console.log(`   第 2 次: HTTP ${b.status}  taskId=${b.taskId}  duplicate=${b.duplicate}`)
const c = await submit(prompt)
console.log(`   第 3 次: HTTP ${c.status}  taskId=${c.taskId}  duplicate=${c.duplicate}`)

const sameTask = a.taskId != null && a.taskId === b.taskId && b.taskId === c.taskId
const flagged = b.duplicate === true && c.duplicate === true
console.log(sameTask && flagged
  ? '   ✅ 幂等生效：三次返回同一 taskId 且后两次 duplicate=true\n'
  : `   ⚠️  幂等未按预期工作：sameTaskId=${sameTask}, duplicate 标记=${flagged}\n`)

/* ── 2. 等待任务完成后再提交同一 Prompt（设计上应放行）── */
console.log('2. 等任务结束后再提交同一 Prompt（按设计应放行并新建任务）')
await new Promise(r => setTimeout(r, 9000))
const d = await submit(prompt)
console.log(`   HTTP ${d.status}  taskId=${d.taskId}  duplicate=${d.duplicate}`)
console.log(d.taskId !== a.taskId
  ? '   ✅ 释放后放行，新建了任务（符合"完成后可再次提交"的设计）\n'
  : '   ℹ️  仍返回旧 taskId\n')

/* ── 3. Prompt 长度上限 ── */
console.log('3. Prompt 长度上限（前端限制 200 字，后端只有 @NotBlank）')
const long = '唐'.repeat(20000)
const e = await submit(long)
console.log(`   提交 20000 字 Prompt: HTTP ${e.status}  ${e.body}`)
console.log(e.status === 200 || e.status === 202
  ? '   ⚠️  后端未限制长度：可提交任意长 Prompt（前端 200 字限制可被绕过）\n'
  : '   ✅ 被拒\n')
