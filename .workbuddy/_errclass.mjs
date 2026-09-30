/** 归类回归报告里的控制台报错（临时诊断脚本） */
import { readFileSync } from 'node:fs'

const r = JSON.parse(readFileSync(new URL('./shots/report.json', import.meta.url), 'utf8'))
const seen = new Map()
for (const p of r.pages) {
  for (const e of (p.errors || [])) {
    const k = String(e).replace(/https?:\/\/\S+/g, '<url>').slice(0, 170)
    if (!seen.has(k)) seen.set(k, { n: 0, routes: new Set() })
    const v = seen.get(k)
    v.n++
    v.routes.add(p.route)
  }
}
console.log('=== 控制台报错归类 ===')
for (const [k, v] of [...seen.entries()].sort((a, b) => b[1].n - a[1].n)) {
  console.log(`[${v.n}x] ${k}`)
  console.log(`      出现在: ${[...v.routes].join(', ')}`)
}
console.log('\n=== /archive dark desktop 完整报错 ===')
const a = r.pages.find(p => p.route === '/archive' && p.theme === 'dark' && p.viewport === 'desktop')
console.log(JSON.stringify(a?.errors ?? [], null, 2))
