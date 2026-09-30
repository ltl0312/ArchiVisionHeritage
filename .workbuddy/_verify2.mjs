import { createRequire } from 'module';
import path from 'path';
import { pathToFileURL } from 'url';

const HOME = process.env.USERPROFILE || process.env.HOME;
const req = createRequire(path.join(HOME, '.dsh/profiles/web/package.json'));
const puppeteer = req('puppeteer-core');

const TARGET = 'd:/Code/JavaWeb/ArchiVisionHeritage/frontend/preview/v4-preview.html';
const OUT = process.argv[2];

const browser = await puppeteer.launch({
  executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe',
  headless: 'new',
  args: ['--no-sandbox', '--allow-file-access-from-files', '--font-render-hinting=none'],
});
const page = await browser.newPage();
const errs = [];
page.on('pageerror', (e) => errs.push('PAGEERROR ' + e.message));
page.on('console', (m) => { if (m.type() === 'error') errs.push('CONSOLE ' + m.text()); });

await page.setViewport({ width: 1440, height: 900 });
await page.goto(pathToFileURL(TARGET).href, { waitUntil: 'domcontentloaded', timeout: 60000 });
await page.evaluate(() => {
  window.__cls = 0;
  new PerformanceObserver((l) => { for (const e of l.getEntries()) if (!e.hadRecentInput) window.__cls += e.value; })
    .observe({ type: 'layout-shift', buffered: true });
});
await new Promise((r) => setTimeout(r, 2200));

const w = (ms) => new Promise((r) => setTimeout(r, ms));

// ═══ 1) 通过用户菜单进入 → 审核工作台 ═══
const admin = await page.evaluate(async () => {
  const o = {};
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
  document.getElementById('railUser').click(); await sleep(280);
  o.railMenuHasAll3 = !!(
    document.getElementById('railMenu').querySelector('[data-go="admin"]') &&
    document.getElementById('railMenu').querySelector('[data-go="notifications"]') &&
    document.getElementById('railMenu').querySelector('[data-go="settings"]')
  );
  document.getElementById('railMenu').querySelector('[data-go="admin"]').click(); await sleep(320);
  o.viewOn = document.querySelector('.view.is-on')?.id;
  o.menuClosed = !document.getElementById('railMenu').classList.contains('is-open');
  o.pending = +document.getElementById('pendingN').textContent;
  o.rows = document.querySelectorAll('#auditList .audit-row').length;

  // 通过第 1 条
  document.querySelector('#auditList .audit-row [data-approve]').click(); await sleep(200);
  o.afterApprove = { pending: +document.getElementById('pendingN').textContent, rows: document.querySelectorAll('#auditList .audit-row').length };

  // 驳回第 1 条 → 展开理由框
  document.querySelector('#auditList .audit-row [data-reject]').click(); await sleep(160);
  o.rejectBoxVisible = !document.querySelector('#auditList .audit-row .reject-box').hidden;
  // 空理由确认 → 应被拦截
  document.querySelector('#auditList .audit-row [data-confirm]').click(); await sleep(160);
  o.emptyReasonBlocked = document.querySelectorAll('#auditList .audit-row').length;
  // 填理由后确认
  const inp = document.querySelector('#auditList .audit-row .reject-box input');
  inp.value = '影像含第三方水印';
  document.querySelector('#auditList .audit-row [data-confirm]').click(); await sleep(200);
  o.afterReject = { pending: +document.getElementById('pendingN').textContent, rows: document.querySelectorAll('#auditList .audit-row').length };
  o.thumbsUseReusedAsset = [...document.querySelectorAll('#auditList .thumb img')].every((i) => i.getAttribute('src') === 'assets/eave.jpg');
  return o;
});

// ═══ 2) 铃铛 → 通知中心 ═══
const notif = await page.evaluate(async () => {
  const o = {}; const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
  document.querySelector('.topbar .iconbtn[data-go="notifications"]').click(); await sleep(320);
  o.viewOn = document.querySelector('.view.is-on')?.id;
  o.unread = document.getElementById('unreadN').textContent;
  o.rows = document.querySelectorAll('#notifList .notif-row').length;
  o.unreadRows = document.querySelectorAll('#notifList .notif-row.is-unread').length;

  // 点第一条 → 标记已读
  document.querySelector('#notifList .notif-row').click(); await sleep(200);
  o.afterRead = { unread: document.getElementById('unreadN').textContent, unreadRows: document.querySelectorAll('#notifList .notif-row.is-unread').length };

  // 筛选「数字锦盒」
  document.querySelector('[data-seg="notif"] button[data-v="box"]').click(); await sleep(200);
  o.boxFilterRows = document.querySelectorAll('#notifList .notif-row').length;
  o.boxFilterAllBox = [...document.querySelectorAll('#notifList .notif-row')].every((r) => r.querySelector('.nicon--gold'));

  // 空态：筛选一个不存在的分类？用 social 有 2 条；改为断言「全部」恢复
  document.querySelector('[data-seg="notif"] button[data-v="all"]').click(); await sleep(160);
  o.allRows = document.querySelectorAll('#notifList .notif-row').length;

  // 全部已读
  document.getElementById('markAll').click(); await sleep(200);
  o.afterMarkAll = { unread: document.getElementById('unreadN').textContent, unreadRows: document.querySelectorAll('#notifList .notif-row.is-unread').length };
  return o;
});

// ═══ 3) 设置页 ═══
const settings = await page.evaluate(async () => {
  const o = {}; const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
  document.getElementById('tbUser').click(); await sleep(260);
  document.getElementById('tbMenu').querySelector('[data-go="settings"]').click(); await sleep(320);
  o.viewOn = document.querySelector('.view.is-on')?.id;
  o.switches = document.querySelectorAll('.switch').length;

  // 主题：切宣纸
  document.querySelector('[data-seg="theme"] button[data-v="light"]').click(); await sleep(300);
  o.themeLight = document.documentElement.getAttribute('data-theme');
  o.consoleThemeSynced = document.querySelector('#demo [data-seg="theme"] button[data-v="light"]').classList.contains('is-on');
  o.settingsThemeSynced = document.querySelector('#v-settings [data-seg="theme"] button[data-v="light"]').classList.contains('is-on');
  document.querySelector('[data-seg="theme"] button[data-v="dark"]').click(); await sleep(260);
  o.themeBack = document.documentElement.getAttribute('data-theme');

  // 密度
  document.querySelector('[data-seg="density"] button[data-v="compact"]').click(); await sleep(220);
  o.densityApplied = document.documentElement.classList.contains('density-compact');
  document.querySelector('[data-seg="density"] button[data-v="cozy"]').click(); await sleep(180);
  o.densityRemoved = !document.documentElement.classList.contains('density-compact');

  // 开关
  const sw = document.querySelector('#v-settings .switch:not(.is-on)');
  const before = sw.getAttribute('aria-checked');
  sw.click(); await sleep(160);
  o.switch = { before, after: sw.getAttribute('aria-checked'), on: sw.classList.contains('is-on') };
  return o;
});

// ═══ 4) 性能与溢出 ═══
const perf = await page.evaluate(() => new Promise((resolve) => {
  const times = []; let last = performance.now(); const t0 = last;
  const el = document.querySelector('.view.is-on');
  const max = Math.max(el.scrollHeight - el.clientHeight, 1);
  let t = 0; const id = setInterval(() => { el.scrollTop = (Math.sin(t++ / 6) * 0.5 + 0.5) * max; }, 16);
  function tick(now) {
    times.push(now - last); last = now;
    if (now - t0 < 2500) requestAnimationFrame(tick);
    else {
      clearInterval(id); el.scrollTop = 0;
      const sum = times.reduce((a, b) => a + b, 0); const s = times.slice().sort((a, b) => a - b);
      resolve({ frames: times.length, avgFps: Math.round(1000 / (sum / times.length)),
                p95: Math.round(s[Math.floor(s.length * .95)]), over33: times.filter((x) => x > 33).length });
    }
  }
  requestAnimationFrame(tick);
}));

// ═══ 5) 截图 ═══
async function shot(name, vp, target) {
  await page.setViewport(vp);
  await page.evaluate((t) => document.querySelector(t).click(), target);
  await new Promise((r) => setTimeout(r, 900));
  await page.screenshot({ path: path.join(OUT, name + '.png') });
}
const D = { width: 1440, height: 900 };
const M = { width: 390, height: 844, isMobile: true, deviceScaleFactor: 2 };
await shot('new-d-admin', D, '#railUser');
await page.evaluate(() => document.querySelector('#railMenu [data-go="admin"]').click());
await new Promise((r) => setTimeout(r, 800));
await page.screenshot({ path: path.join(OUT, 'new-d-admin.png') });
for (const [id, name] of [['notifications', 'new-d-notif'], ['settings', 'new-d-settings']]) {
  await page.setViewport(D);
  await page.evaluate((i) => { document.getElementById('tbUser').click(); }, id);
  await new Promise((r) => setTimeout(r, 240));
  await page.evaluate((i) => document.getElementById('tbMenu').querySelector('[data-go="' + i + '"]').click(), id);
  await new Promise((r) => setTimeout(r, 800));
  await page.screenshot({ path: path.join(OUT, name + '.png') });
}
for (const [id, name] of [['admin', 'new-m-admin'], ['notifications', 'new-m-notif'], ['settings', 'new-m-settings']]) {
  await page.setViewport(M);
  await page.evaluate((i) => { document.getElementById('tbUser').click(); }, id);
  await new Promise((r) => setTimeout(r, 240));
  await page.evaluate((i) => {
    const m = document.getElementById('tbMenu').querySelector('[data-go="' + i + '"]');
    if (m) m.click(); else document.querySelector('#railUser').click() || document.getElementById('railMenu').querySelector('[data-go="' + i + '"]').click();
  }, id);
  await new Promise((r) => setTimeout(r, 800));
  await page.screenshot({ path: path.join(OUT, name + '.png') });
}

const misc = await page.evaluate(() => ({
  cls: Math.round((window.__cls || 0) * 1000) / 1000,
  scrollW: document.documentElement.scrollWidth,
  innerW: window.innerWidth,
  views: [...document.querySelectorAll('.view')].map((v) => v.id),
  lines: document.documentElement.outerHTML.length,
}));

console.log(JSON.stringify({ admin, notif, settings, perf, misc, errors: errs.length ? errs : 'none' }, null, 2));
await browser.close();
