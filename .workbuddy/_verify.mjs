import { createRequire } from 'module';
import path from 'path';
import { pathToFileURL } from 'url';

const HOME = process.env.USERPROFILE || process.env.HOME;
const req = createRequire(path.join(HOME, '.dsh/profiles/web/package.json'));
const puppeteer = req('puppeteer-core');

const TARGET = 'd:/Code/JavaWeb/ArchiVisionHeritage/frontend/preview/v4-preview.html';
const OUT = process.argv[2];
const OUTDIR = process.argv[3];

const browser = await puppeteer.launch({
  executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe',
  headless: 'new',
  args: ['--no-sandbox', '--allow-file-access-from-files', '--font-render-hinting=none'],
});
const page = await browser.newPage();
const errs = [];
page.on('pageerror', (e) => errs.push('PAGEERROR ' + e.message));
page.on('console', (m) => { if (m.type() === 'error') errs.push('CONSOLE ' + m.text()); });
page.on('requestfailed', (r) => errs.push('REQFAIL ' + r.url().split('/').pop()));

await page.setViewport({ width: 1440, height: 900, deviceScaleFactor: 1 });
await page.goto(pathToFileURL(TARGET).href, { waitUntil: 'domcontentloaded', timeout: 60000 });
await page.evaluate(() => {
  window.__cls = 0;
  new PerformanceObserver((l) => {
    for (const e of l.getEntries()) if (!e.hadRecentInput) window.__cls += e.value;
  }).observe({ type: 'layout-shift', buffered: true });
});
await new Promise((r) => setTimeout(r, 2500));

// ═══ 1) 账户菜单功能验证 ═══
const menuTest = await page.evaluate(async () => {
  const out = {};
  const read = (m) => {
    const cs = getComputedStyle(m);
    return { open: m.classList.contains('is-open'), bg: cs.backgroundColor, vis: cs.visibility, op: cs.opacity };
  };
  const railBtn = document.getElementById('railUser');
  const railMenu = document.getElementById('railMenu');
  const tbBtn = document.getElementById('tbUser');
  const tbMenu = document.getElementById('tbMenu');

  out.initialRail = read(railMenu);
  railBtn.click();
  await new Promise((r) => setTimeout(r, 260));
  out.afterRailClick = { ...read(railMenu), aria: railBtn.getAttribute('aria-expanded') };

  // 点外部关闭
  document.querySelector('.brand')?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
  await new Promise((r) => setTimeout(r, 260));
  out.afterOutsideClick = { ...read(railMenu), aria: railBtn.getAttribute('aria-expanded') };

  // Esc 关闭
  railBtn.click();
  await new Promise((r) => setTimeout(r, 200));
  document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
  await new Promise((r) => setTimeout(r, 260));
  out.afterEsc = { open: railMenu.classList.contains('is-open') };

  // 顶栏头像
  tbBtn.click();
  await new Promise((r) => setTimeout(r, 260));
  out.afterTbClick = { ...read(tbMenu), aria: tbBtn.getAttribute('aria-expanded') };

  // 菜单条目：主题切换
  const themeBtn = tbMenu.querySelector('[data-theme-toggle]');
  themeBtn.click();
  await new Promise((r) => setTimeout(r, 300));
  out.themeAfterMenuToggle = document.documentElement.getAttribute('data-theme');
  themeBtn.click();
  await new Promise((r) => setTimeout(r, 300));
  out.themeBack = document.documentElement.getAttribute('data-theme');
  document.querySelector('.brand')?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));

  out.menuItems = railMenu.querySelectorAll('.menu-item').length;
  return out;
});

// ═══ 2) 特效开关 ═══
const fxTest = await page.evaluate(async () => {
  document.querySelector('[data-seg="fx"] button[data-v="off"]').click();
  await new Promise((r) => setTimeout(r, 200));
  const off = document.documentElement.classList.contains('no-fx');
  const bd = getComputedStyle(document.querySelector('.craft')).backdropFilter;
  document.querySelector('[data-seg="fx"] button[data-v="on"]').click();
  return { noFxApplied: off, backdropFilterWhenOff: bd };
});

// ═══ 3) FPS ═══
const fps = await page.evaluate(() => new Promise((resolve) => {
  const times = []; let last = performance.now(); const t0 = last;
  const el = document.querySelector('.view.is-on');
  const max = Math.max(el.scrollHeight - el.clientHeight, 1);
  let t = 0;
  const scrollId = setInterval(() => { el.scrollTop = (Math.sin(t++ / 6) * 0.5 + 0.5) * max; }, 16);
  function tick(now) {
    times.push(now - last); last = now;
    if (now - t0 < 3000) requestAnimationFrame(tick);
    else {
      clearInterval(scrollId); el.scrollTop = 0;
      const s = times.slice().sort((a, b) => a - b);
      const sum = times.reduce((a, b) => a + b, 0);
      resolve({ frames: times.length, avgFps: Math.round(1000 / (sum / times.length)),
                p95: Math.round(s[Math.floor(s.length * .95)]), over33: times.filter((x) => x > 33).length });
    }
  }
  requestAnimationFrame(tick);
}));

const cls = await page.evaluate(() => Math.round((window.__cls || 0) * 1000) / 1000);

// ═══ 4) 截图 ═══
async function shot(name, vp, viewId) {
  await page.setViewport(vp);
  if (viewId) {
    await page.evaluate((id) => document.querySelector('[data-go="' + id + '"]').click(), viewId);
  }
  await new Promise((r) => setTimeout(r, 900));
  await page.screenshot({ path: path.join(OUTDIR, name + '.png') });
}
const D = { width: 1440, height: 900 };
const M = { width: 390, height: 844, isMobile: true, deviceScaleFactor: 2 };
await shot('fix-d-feed', D, 'feed');
await shot('fix-d-zhixi', D, 'zhixi');
await shot('fix-d-huanzhu', D, 'huanzhu');
await shot('fix-d-archive', D, 'archive');

// 桌面：打开顶栏菜单后再截一张
await page.setViewport(D);
await page.evaluate(() => document.getElementById('tbUser').click());
await new Promise((r) => setTimeout(r, 400));
await page.screenshot({ path: path.join(OUTDIR, 'fix-d-menu.png') });
await page.evaluate(() => document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' })));

await shot('fix-m-feed', M, 'feed');
await shot('fix-m-zhixi', M, 'zhixi');
await page.setViewport(M);
await page.evaluate(() => document.getElementById('tbUser').click());
await new Promise((r) => setTimeout(r, 400));
await page.screenshot({ path: path.join(OUTDIR, 'fix-m-menu.png') });

const overflow = await page.evaluate(() => ({ scrollW: document.documentElement.scrollWidth, innerW: window.innerWidth }));

console.log(JSON.stringify({ menuTest, fxTest, fps, cls, overflow, errors: errs.length ? errs : 'none' }, null, 2));
void OUT;
await browser.close();
