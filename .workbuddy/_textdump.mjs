/** 从回归报告里导出各路由实际渲染出的文本，作为「像素截图」之外的结构性核验 */
import { readFileSync } from 'node:fs'

const r = JSON.parse(readFileSync(new URL('./shots/report.json', import.meta.url), 'utf8'))
const seen = new Set()
for (const p of r.pages) {
  if (p.theme !== 'dark' || p.viewport !== 'desktop') continue
  if (seen.has(p.route)) continue
  seen.add(p.route)
  console.log(`\n══════ ${p.route} ══════`)
  console.log(`  theme=${p.measure.theme}  nav=${p.measure.navCount}  viewShell=${p.measure.hasViewShell}  topbar=${p.measure.hasTopbar}  sidebar=${p.measure.hasSidebar}`)
  console.log(`  text: ${p.measure.text}`)
}
