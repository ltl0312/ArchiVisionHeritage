/**
 * 智观·古建 — V4 UI 重构回归验证（零依赖）
 *
 * 用 Node 内置的 fetch + WebSocket 直连 Chrome DevTools Protocol，
 * 不依赖 puppeteer-core（原 _verify.mjs 依赖 workbuddy 内的 node/puppeteer，已不可用）。
 *
 * 覆盖计划书 §7.1 的自动化检查：
 *   · 视觉基线：逐页截图（1440×900 / 390×844，玄墨 + 宣纸）
 *   · 布局稳定性：PerformanceObserver 监听 layout-shift → CLS
 *   · 帧率：rAF 采样 + 程序化滚动 3s → avgFps / p95 帧耗时 / 超 33ms 帧数
 *   · 横向溢出：scrollWidth vs innerWidth
 *   · 控制台报错：console.error + 未捕获异常 + 网络/日志错误
 *   · 交互断言：用户菜单（开 / 点外部关 / Esc 关）、主题切换、设置开关 aria、通知筛选
 *
 * 用法：
 *   node .workbuddy/_verify_v4.mjs --base=http://127.0.0.1:5173 --out=.workbuddy/shots
 *   node .workbuddy/_verify_v4.mjs --routes=/home,/zhixi --themes=dark
 */
import { spawn } from 'node:child_process'
import { mkdirSync, writeFileSync, rmSync, existsSync } from 'node:fs'
import { join, resolve } from 'node:path'
import { setTimeout as sleep } from 'node:timers/promises'

/* ══════════════════════ 配置 ══════════════════════ */

const CHROME_CANDIDATES = [
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
  `${process.env.LOCALAPPDATA}/Google/Chrome/Application/chrome.exe`
]

const DEFAULT_ROUTES = [
  '/home', '/community', '/zhixi', '/huanzhu',
  '/archive', '/notifications', '/settings', '/admin', '/login'
]

const VIEWPORTS = [
  { name: 'desktop', width: 1440, height: 900, dsf: 1, mobile: false },
  { name: 'mobile', width: 390, height: 844, dsf: 2, mobile: true }
]

const DEBUG_PORT = 9333

/* ══════════════════════ 参数 ══════════════════════ */

function argOf(name, fallback) {
  const hit = process.argv.find(a => a.startsWith(`--${name}=`))
  return hit ? hit.slice(name.length + 3) : fallback
}

const BASE = argOf('base', 'http://localhost:5173').replace(/\/$/, '')
const OUT = resolve(argOf('out', '.workbuddy/shots'))
const ROUTES = argOf('routes', DEFAULT_ROUTES.join(',')).split(',').map(s => s.trim()).filter(Boolean)
const THEMES = argOf('themes', 'dark,light').split(',').map(s => s.trim()).filter(Boolean)
const SKIP_SHOTS = process.argv.includes('--no-shots')
const SETTLE_MS = Number(argOf('settle', '1200'))
/**
 * 受保护路由（/archive、/admin、/settings、/notifications）在无 token 时会被
 * 路由守卫重定向到 /login，从而测不到真实页面。
 *   --auth=admin|user  注入**合成** JWT（仅够通过前端守卫，后端会拒签）
 *   --auth=none        不注入（默认）
 *   --token=<jwt>      注入**真实** token（如对着 Docker 栈跑时，先用
 *                      /api/v1/auth/login 拿真 token），这样数据请求也能通过
 */
const AUTH = argOf('auth', 'none')
const REAL_TOKEN = argOf('token', '')

function synthToken(role) {
  const payload = { sub: '1', username: role === 'admin' ? 'admin' : 'demo_user', role: role === 'admin' ? 'ADMIN' : 'USER' }
  const b64 = (o) => Buffer.from(JSON.stringify(o)).toString('base64url')
  return `${b64({ alg: 'HS256', typ: 'JWT' })}.${b64(payload)}.verify-only`
}

function tokenToInject() {
  if (REAL_TOKEN) return REAL_TOKEN
  if (AUTH !== 'none') return synthToken(AUTH)
  return ''
}

/* ══════════════════════ CDP 最小客户端 ══════════════════════ */

class CDP {
  constructor(ws) {
    this.ws = ws
    this.id = 0
    this.pending = new Map()
    this.listeners = new Map()
    ws.addEventListener('message', (ev) => {
      let msg
      try { msg = JSON.parse(ev.data) } catch { return }
      if (msg.id != null) {
        const p = this.pending.get(msg.id)
        if (p) {
          this.pending.delete(msg.id)
          msg.error ? p.reject(new Error(msg.error.message)) : p.resolve(msg.result)
        }
      } else if (msg.method) {
        for (const fn of this.listeners.get(msg.method) || []) fn(msg.params)
      }
    })
  }

  static async connect(wsUrl) {
    const ws = new WebSocket(wsUrl)
    await new Promise((res, rej) => {
      ws.addEventListener('open', res, { once: true })
      ws.addEventListener('error', () => rej(new Error(`WS connect failed: ${wsUrl}`)), { once: true })
    })
    return new CDP(ws)
  }

  send(method, params = {}) {
    const id = ++this.id
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject })
      this.ws.send(JSON.stringify({ id, method, params }))
      setTimeout(() => {
        if (this.pending.delete(id)) reject(new Error(`CDP timeout: ${method}`))
      }, 45000)
    })
  }

  on(method, fn) {
    if (!this.listeners.has(method)) this.listeners.set(method, [])
    this.listeners.get(method).push(fn)
  }

  close() { try { this.ws.close() } catch { /* ignore */ } }
}

/* ══════════════════════ 浏览器生命周期 ══════════════════════ */

function findChrome() {
  for (const p of CHROME_CANDIDATES) {
    if (p && existsSync(p)) return p
  }
  return null
}

async function launchChrome() {
  const chrome = findChrome()
  if (!chrome) throw new Error('未找到 Chrome，请把 chrome.exe 路径加入 CHROME_CANDIDATES')
  const profile = join(process.env.TEMP || '.', `dsh-v4-verify-${Date.now()}`)
  mkdirSync(profile, { recursive: true })

  const child = spawn(chrome, [
    '--headless=new',
    '--disable-gpu',
    '--no-sandbox',
    '--no-first-run',
    '--no-default-browser-check',
    '--disable-extensions',
    '--hide-scrollbars',
    `--remote-debugging-port=${DEBUG_PORT}`,
    `--user-data-dir=${profile}`
  ], { stdio: 'ignore', windowsHide: true })

  // 轮询 DevTools 端点
  const deadline = Date.now() + 30000
  let version = null
  while (Date.now() < deadline) {
    try {
      const r = await fetch(`http://127.0.0.1:${DEBUG_PORT}/json/version`)
      if (r.ok) { version = await r.json(); break }
    } catch { /* not up yet */ }
    await sleep(300)
  }
  if (!version) throw new Error('Chrome DevTools 端点未就绪')
  return { child, profile }
}

async function newTarget(url) {
  const r = await fetch(`http://127.0.0.1:${DEBUG_PORT}/json/new?${encodeURIComponent(url)}`, { method: 'PUT' })
  if (!r.ok) throw new Error(`创建 target 失败: ${r.status}`)
  return r.json()
}

async function closeTarget(id) {
  try { await fetch(`http://127.0.0.1:${DEBUG_PORT}/json/close/${id}`) } catch { /* ignore */ }
}

/* ══════════════════════ 注入脚本（导航前） ══════════════════════ */

function initScript(theme) {
  const tk = tokenToInject()
  return `
    try { localStorage.setItem('theme', ${JSON.stringify(theme)}); } catch (e) {}
    ${tk ? `try { localStorage.setItem('token', ${JSON.stringify(tk)}); } catch (e) {}` : ''}
    window.__v4cls = 0;
    window.__v4shifts = [];
    try {
      new PerformanceObserver((list) => {
        for (const e of list.getEntries()) {
          if (!e.hadRecentInput) {
            window.__v4cls += e.value;
            window.__v4shifts.push({ v: +e.value.toFixed(5), t: Math.round(e.startTime) });
          }
        }
      }).observe({ type: 'layout-shift', buffered: true });
    } catch (e) {}
  `
}

/* ══════════════════════ 页面度量 ══════════════════════ */

const MEASURE_FN = `(() => {
  const de = document.documentElement;
  const body = document.body;
  const scrollRoot = document.querySelector('.view-host') || de;
  return {
    url: location.pathname + location.search,
    theme: de.getAttribute('data-theme'),
    density: de.classList.contains('density-compact') ? 'compact' : 'cozy',
    title: document.title,
    innerWidth: window.innerWidth,
    docScrollWidth: de.scrollWidth,
    bodyScrollWidth: body ? body.scrollWidth : 0,
    overflowX: Math.max(de.scrollWidth, body ? body.scrollWidth : 0) - window.innerWidth,
    scrollRootTag: scrollRoot.className || scrollRoot.tagName,
    scrollable: scrollRoot.scrollHeight > scrollRoot.clientHeight + 2,
    hasViewShell: !!document.querySelector('.view-shell'),
    hasTopbar: !!document.querySelector('.topbar'),
    hasSidebar: !!document.querySelector('.sidebar'),
    navCount: document.querySelectorAll('.sidebar-nav .nav-btn').length,
    cls: +(window.__v4cls || 0).toFixed(5),
    shifts: (window.__v4shifts || []).slice(0, 8),
    text: (body ? body.innerText : '').replace(/\\s+/g, ' ').trim().slice(0, 240)
  };
})()`

const FPS_FN = `(async () => {
  const root = document.querySelector('.view-host') || document.documentElement;
  const frames = [];
  let last = performance.now();
  let raf = 0;
  const stop = performance.now() + 3000;
  // 程序化滚动，制造持续重绘
  const scroller = setInterval(() => {
    const max = root.scrollHeight - root.clientHeight;
    if (max > 40) root.scrollTop = (root.scrollTop + 90) % max;
  }, 60);
  await new Promise((resolve) => {
    const tick = (now) => {
      frames.push(now - last);
      last = now;
      if (now < stop) raf = requestAnimationFrame(tick); else resolve();
    };
    raf = requestAnimationFrame(tick);
  });
  clearInterval(scroller);
  cancelAnimationFrame(raf);
  const d = frames.slice(2);
  const sorted = [...d].sort((a, b) => a - b);
  const avg = d.reduce((s, v) => s + v, 0) / Math.max(1, d.length);
  const p95 = sorted[Math.min(sorted.length - 1, Math.floor(sorted.length * 0.95))] || 0;
  return {
    frames: d.length,
    avgFps: +(1000 / avg).toFixed(1),
    p95FrameMs: +p95.toFixed(2),
    over33ms: d.filter(v => v > 33).length,
    avgFrameMs: +avg.toFixed(2)
  };
})()`

/* ══════════════════════ 单页跑一次 ══════════════════════ */

/**
 * 单页跑一次。
 *
 * 注意两个「探针自身」的坑（第一版踩过）：
 *  1. Runtime/Log 监听器只能注册一次 —— 每页注册会让旧页面的 errors 数组
 *     继续接收后续页面的报错，把计数越滚越大（首测 /home 显示 77 条即此因）。
 *  2. Page.addScriptToEvaluateOnNewDocument 会累积 —— 每页注入一次会让
 *     多个 PerformanceObserver 同时累加到同一个 window.__v4cls，
 *     使 CLS 被放大 N 倍（首测 /settings 的 0.25 实为 ×25 的结果）。
 *     因此每页注入后必须用返回的 identifier 移除。
 */
let currentErrors = []
/**
 * 后端未启动时（本机没有 8080 + MySQL + Redis），页面上的每个数据请求都会
 * 以 404/502 落进控制台。那是**环境缺失**，不是前端缺陷 —— 前端对此渲染的是
 * 错误态，本身是正确行为。默认把 /api/ 的资源加载失败归入 `envErrors`，
 * 不计入失败门禁；传 --strict-api 可把它们也当作失败。
 */
const STRICT_API = process.argv.includes('--strict-api')

function isEnvOnly(msg) {
  return !STRICT_API && /Failed to load resource/.test(msg) && /\/api\//.test(msg)
}

function attachDiagnostics(cdp) {
  const push = (m) => { if (!isEnvOnly(m)) currentErrors.push(m) }
  cdp.on('Runtime.consoleAPICalled', (p) => {
    if (p.type === 'error') {
      push(`console.error: ${(p.args || []).map(a => a.value ?? a.description ?? a.type).join(' ')}`)
    }
  })
  cdp.on('Runtime.exceptionThrown', (p) => {
    push(`exception: ${p.exceptionDetails?.exception?.description || p.exceptionDetails?.text || 'unknown'}`)
  })
  cdp.on('Log.entryAdded', (p) => {
    if (p.entry?.level === 'error') {
      const where = p.entry.url ? ` @ ${p.entry.url}` : ''
      push(`log: ${p.entry.text}${where}`)
    }
  })
}

async function runPage(cdp, route, theme, viewport, outDir) {
  currentErrors = []

  await cdp.send('Emulation.setDeviceMetricsOverride', {
    width: viewport.width,
    height: viewport.height,
    deviceScaleFactor: viewport.dsf,
    mobile: viewport.mobile
  })

  const { identifier } = await cdp.send('Page.addScriptToEvaluateOnNewDocument', { source: initScript(theme) })

  const url = `${BASE}${route}`
  // 先挂 load 监听再导航，否则 load 可能在监听注册前就触发，白等满超时
  const loaded = new Promise(res => cdp.on('Page.loadEventFired', res))
  await cdp.send('Page.navigate', { url })
  await Promise.race([loaded, sleep(15000)])
  await sleep(SETTLE_MS)

  const measure = (await cdp.send('Runtime.evaluate', {
    expression: MEASURE_FN, returnByValue: true, awaitPromise: true
  })).result.value

  const fps = (await cdp.send('Runtime.evaluate', {
    expression: FPS_FN, returnByValue: true, awaitPromise: true
  })).result.value

  let shot = null
  if (!SKIP_SHOTS) {
    const png = (await cdp.send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false })).data
    const safe = route === '/' ? 'root' : route.replace(/^\//, '').replace(/[^\w-]/g, '_')
    shot = join(outDir, `${safe}__${theme}__${viewport.name}.png`)
    writeFileSync(shot, Buffer.from(png, 'base64'))
  }

  // 注入脚本必须移除，否则 CLS 观察者会跨页累积
  try { await cdp.send('Page.removeScriptToEvaluateOnNewDocument', { identifier }) } catch { /* ignore */ }

  return { route, theme, viewport: viewport.name, measure, fps, errors: [...currentErrors], shot }
}

/* ══════════════════════ 交互断言 ══════════════════════ */

async function interactionChecks(cdp) {
  const results = []
  const ev = async (expr) => (await cdp.send('Runtime.evaluate', {
    expression: expr, returnByValue: true, awaitPromise: true
  })).result.value

  const check = (name, ok, detail) => results.push({ name, ok: !!ok, detail })

  // 回到首页（侧栏 + 顶栏都在）
  await cdp.send('Page.navigate', { url: `${BASE}/home` })
  await sleep(2500)

  const menuSel = '.sidebar-footer .user-menu'

  check('用户菜单初始关闭',
    await ev(`!document.querySelector('${menuSel}')`),
    '侧栏用户菜单不应在初始状态渲染')

  await ev(`document.querySelector('.sidebar-footer .user-trigger').click()`)
  await sleep(350)
  check('点击用户条可开菜单',
    await ev(`!!document.querySelector('${menuSel}')`),
    '点击 .user-trigger 后 .user-menu 应出现')

  check('菜单项数量（账户菜单 6 项）',
    await ev(`document.querySelectorAll('${menuSel} .menu-item').length >= 6`),
    `实际 ${await ev(`document.querySelectorAll('${menuSel} .menu-item').length`)}`)

  // 点外部关闭
  await ev(`document.body.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }))`)
  await sleep(300)
  check('点外部可关菜单',
    await ev(`!document.querySelector('${menuSel}')`),
    'mousedown 在菜单外应关闭')

  // Esc 关闭
  await ev(`document.querySelector('.sidebar-footer .user-trigger').click()`)
  await sleep(300)
  await ev(`document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))`)
  await sleep(300)
  check('Esc 可关菜单',
    await ev(`!document.querySelector('${menuSel}')`),
    'keydown Escape 应关闭')

  // 顶栏头像也能开
  await ev(`document.querySelector('.tb-av-btn').click()`)
  await sleep(350)
  check('顶栏头像可开菜单',
    await ev(`!!document.querySelector('.tb-av .user-menu')`),
    '点击 .tb-av-btn 后顶栏菜单应出现')

  // 主题切换（通过菜单内「昼夜流转」）
  const before = await ev(`document.documentElement.getAttribute('data-theme')`)
  await ev(`[...document.querySelectorAll('.tb-av .menu-item')].find(b => b.textContent.includes('昼夜流转')).click()`)
  await sleep(400)
  const after = await ev(`document.documentElement.getAttribute('data-theme')`)
  check('菜单内昼夜流转生效', before && after && before !== after, `${before} → ${after}`)

  // 设置页：开关 aria-checked 与信息密度
  await cdp.send('Page.navigate', { url: `${BASE}/settings` })
  await sleep(2500)
  const sw = await ev(`(() => {
    const list = [...document.querySelectorAll('[role="switch"]')];
    return { count: list.length, allHaveAria: list.every(s => s.hasAttribute('aria-checked')) };
  })()`)
  check('设置页开关带 role=switch + aria-checked', sw.count > 0 && sw.allHaveAria, JSON.stringify(sw))

  const densityToggled = await ev(`(async () => {
    const btns = [...document.querySelectorAll('.v4-seg button')];
    const compact = btns.find(b => b.textContent.trim() === '紧凑');
    if (!compact) return 'no-compact-button';
    compact.click();
    await new Promise(r => setTimeout(r, 300));
    return document.documentElement.classList.contains('density-compact');
  })()`)
  check('信息密度「紧凑」真实改变布局', densityToggled === true, String(densityToggled))

  // 通知中心：筛选 chip
  await cdp.send('Page.navigate', { url: `${BASE}/notifications` })
  await sleep(2500)
  const chips = await ev(`document.querySelectorAll('.v4-chip').length`)
  check('通知中心有筛选 chip', chips >= 2, `chips=${chips}`)

  return results
}

/* ══════════════════════ 主流程 ══════════════════════ */

async function main() {
  mkdirSync(OUT, { recursive: true })
  console.log(`\n══ V4 UI 回归验证 ══\n  base=${BASE}\n  out=${OUT}\n  routes=${ROUTES.length}  themes=${THEMES.join('/')}\n`)

  const { child, profile } = await launchChrome()
  const report = { base: BASE, at: new Date().toISOString(), pages: [], interactions: [], summary: {} }

  try {
    const target = await newTarget('about:blank')
    const cdp = await CDP.connect(target.webSocketDebuggerUrl)
    await cdp.send('Page.enable')
    await cdp.send('Runtime.enable')
    await cdp.send('Log.enable')
    attachDiagnostics(cdp)

    for (const route of ROUTES) {
      for (const theme of THEMES) {
        for (const vp of VIEWPORTS) {
          let row
          try {
            row = await runPage(cdp, route, theme, vp, OUT)
          } catch (e) {
            row = { route, theme, viewport: vp.name, error: String(e.message || e), measure: null, fps: null, errors: [] }
          }
          report.pages.push(row)
          const m = row.measure
          const tag = `${route} [${theme}/${vp.name}]`
          if (row.error) {
            console.log(`  ✗ ${tag}  ${row.error}`)
          } else {
            const flags = []
            if (m.overflowX > 1) flags.push(`溢出 ${m.overflowX}px`)
            if (m.cls >= 0.1) flags.push(`CLS ${m.cls}`)
            if (row.errors.length) flags.push(`${row.errors.length} 报错`)
            if (!m.hasViewShell) flags.push('缺 .view-shell')
            const fpsTxt = row.fps ? `fps=${row.fps.avgFps} p95=${row.fps.p95FrameMs}ms >33ms=${row.fps.over33ms}` : 'fps=n/a'
            console.log(`  ${flags.length ? '⚠' : '✓'} ${tag.padEnd(38)} ${fpsTxt}  nav=${m.navCount}  cls=${m.cls}${flags.length ? '  ← ' + flags.join(' / ') : ''}`)
          }
        }
      }
    }

    console.log('\n── 交互断言 ──')
    try {
      report.interactions = await interactionChecks(cdp)
      for (const r of report.interactions) {
        console.log(`  ${r.ok ? '✓' : '✗'} ${r.name}${r.ok ? '' : `  ← ${r.detail}`}`)
      }
    } catch (e) {
      console.log(`  ✗ 交互断言中断: ${e.message}`)
      report.interactions = [{ name: 'interaction-block', ok: false, detail: String(e.message || e) }]
    }

    cdp.close()
    await closeTarget(target.id)
  } finally {
    try { child.kill() } catch { /* ignore */ }
    await sleep(400)
    try { rmSync(profile, { recursive: true, force: true }) } catch { /* ignore */ }
  }

  /* ── 汇总 ── */
  const pages = report.pages.filter(p => !p.error)
  const overflow = pages.filter(p => p.measure.overflowX > 1)
  const clsBad = pages.filter(p => p.measure.cls >= 0.1)
  const errPages = pages.filter(p => p.errors.length)
  const noShell = pages.filter(p => !p.measure.hasViewShell)
  const lowFps = pages.filter(p => p.fps && p.fps.avgFps < 55)

  report.summary = {
    pagesChecked: pages.length,
    failedPages: report.pages.filter(p => p.error).length,
    horizontalOverflow: overflow.map(p => `${p.route}[${p.viewport}] +${p.measure.overflowX}px`),
    clsOverLimit: clsBad.map(p => `${p.route}[${p.viewport}] ${p.measure.cls}`),
    pagesWithConsoleErrors: errPages.map(p => `${p.route}[${p.theme}/${p.viewport}] ${p.errors.length}`),
    missingViewShell: noShell.map(p => `${p.route}[${p.viewport}]`),
    lowFps: lowFps.map(p => `${p.route}[${p.viewport}] ${p.fps.avgFps}`),
    interactionsFailed: report.interactions.filter(i => !i.ok).map(i => i.name),
    navItemCounts: [...new Set(pages.map(p => p.measure.navCount))],
    themesSeen: [...new Set(pages.map(p => p.measure.theme))]
  }

  console.log('\n── 汇总 ──')
  console.log(JSON.stringify(report.summary, null, 2))

  const outJson = join(OUT, 'report.json')
  writeFileSync(outJson, JSON.stringify(report, null, 2))
  console.log(`\n  完整报告：${outJson}\n`)

  const failed =
    report.summary.failedPages || overflow.length || clsBad.length || errPages.length ||
    noShell.length || report.summary.interactionsFailed.length
  return failed ? 1 : 0
}

main()
  .then(code => process.exit(code))
  .catch(e => { console.error('验证脚本异常:', e); process.exit(2) })
