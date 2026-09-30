# -*- coding: utf-8 -*-
"""一次性补丁：给预览页新增「审核工作台 / 通知中心 / 个人设置」三个次级页面（V4 轻量重设计）。
所有替换都带命中次数断言，任一不符即中止且不写盘。"""
import io, os, sys

P = 'frontend/preview/v4-preview.html'
src = io.open(P, encoding='utf-8').read()
orig = src
report = []

def rep(anchor, new, expect=1, label=''):
    global src
    n = src.count(anchor)
    assert n == expect, 'ANCHOR FAIL [%s] expect=%d got=%d :: %s' % (label, expect, n, anchor[:70])
    src = src.replace(anchor, new)
    report.append('OK  %-22s x%d' % (label, expect))

# ─────────────────────────────── 1) CSS ───────────────────────────────
CSS = u"""
/* ════════ 次级页面：审核工作台 / 通知中心 / 个人设置（V4 轻量重设计） ════════ */
.toolrow{display:flex;align-items:center;justify-content:space-between;gap:16px;flex-wrap:wrap;margin-bottom:var(--sp-6)}
.headstats{display:flex;align-items:center;gap:10px;flex-wrap:wrap}
.card--jade{background:rgba(79,158,147,.07);border-color:var(--line-jade)}
.empty{padding:56px 20px;text-align:center;border-radius:var(--r-lg);border:1px dashed var(--line);
  color:var(--tx-3);font-size:13px;background:var(--surface)}
.empty svg{width:40px;height:40px;margin:0 auto 12px;display:block;stroke:var(--tx-5);fill:none;
  stroke-width:1.5;stroke-linecap:round;stroke-linejoin:round}

/* 审核工作台 */
.admin-grid{display:grid;grid-template-columns:minmax(0,1fr) 320px;gap:20px;align-items:start}
#auditList{display:flex;flex-direction:column;gap:12px;margin-top:12px}
.audit-row{display:flex;flex-wrap:wrap;align-items:center;gap:14px;padding:14px 16px;
  border-radius:var(--r-lg);background:var(--surface);border:1px solid var(--line)}
.audit-row .thumb{width:64px;height:48px;flex:0 0 64px;border-radius:var(--r-xs);overflow:hidden;background:var(--panel-2)}
.audit-row .thumb img{width:100%;height:100%;object-fit:cover}
.audit-row .info{flex:1 1 180px;min-width:0;display:flex;flex-direction:column;gap:3px}
.audit-row .info b{font-family:var(--serif);font-size:14px;font-weight:500;line-height:1.4}
.audit-row .info .meta{font-size:11px;color:var(--tx-4)}
.audit-row .acts{display:flex;gap:8px;flex:0 0 auto}
.audit-row .acts .btn,.audit-row .reject-box .btn{height:34px;padding:0 14px;font-size:12px;border-radius:var(--r-sm)}
.audit-row .reject-box{flex-basis:100%;display:flex;gap:8px;align-items:center;flex-wrap:wrap;
  padding-top:12px;margin-top:2px;border-top:1px solid var(--line)}
.audit-row .reject-box[hidden]{display:none}
.audit-row .reject-box input{flex:1 1 200px;min-width:0;height:34px;padding:0 12px;border-radius:var(--r-sm);
  background:var(--bg);border:1px solid var(--line);font-size:12px;color:var(--tx-1)}
.audit-row .reject-box input:focus{border-color:var(--gold)}
.audit-row .reject-box input::placeholder{color:var(--tx-5)}
.rules{list-style:none;display:flex;flex-direction:column;gap:9px}
.rules li{position:relative;padding-left:16px;font-size:11.5px;line-height:1.75;color:var(--tx-2)}
.rules li::before{content:"";position:absolute;left:0;top:8px;width:6px;height:6px;border-radius:50%;
  background:var(--gold);opacity:.72}
.loglist{display:flex;flex-direction:column}
.logrow{display:flex;align-items:baseline;gap:8px;padding:8px 0;border-bottom:1px solid var(--line-soft);font-size:11.5px}
.logrow:last-child{border-bottom:none}
.logrow>.act{flex:0 0 34px;font-weight:600;color:var(--jade-text)}
.logrow>b{flex:1;min-width:0;font-weight:400;color:var(--tx-2);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.logrow>.t{flex:0 0 auto;font-size:10px;color:var(--tx-5)}

/* 通知中心 */
.notif-list{display:flex;flex-direction:column;gap:10px}
.notif-row{display:flex;align-items:flex-start;gap:14px;padding:16px;border-radius:var(--r-lg);
  background:var(--surface);border:1px solid var(--line);cursor:pointer;
  transition:border-color var(--ease),background-color var(--ease)}
.notif-row:hover{border-color:var(--line-gold)}
.notif-row.is-unread{background:var(--surface-2);border-color:var(--line-gold)}
.nicon{width:36px;height:36px;flex:0 0 36px;border-radius:var(--r-md);display:grid;place-items:center}
.nicon svg{width:18px;height:18px;fill:none;stroke-width:1.6;stroke-linecap:round;stroke-linejoin:round}
.nicon--gold{background:rgba(201,162,39,.14);border:1px solid var(--line-gold)}
.nicon--gold svg{stroke:var(--gold-text)}
.nicon--jade{background:rgba(79,158,147,.14);border:1px solid var(--line-jade)}
.nicon--jade svg{stroke:var(--jade-text)}
.nicon--red{background:rgba(180,69,58,.16);border:1px solid rgba(180,69,58,.34)}
.nicon--red svg{stroke:var(--cinnabar-text)}
.nbody{flex:1;min-width:0}
.nbody b{font-size:13px;font-weight:600;display:block;margin-bottom:4px}
.nbody p{font-size:12px;line-height:1.7;color:var(--tx-3)}
.nbody .t{font-size:10px;color:var(--tx-5);margin-top:6px;display:block}
.nact{display:flex;align-items:center;gap:10px;flex:0 0 auto}
.unread-dot{width:8px;height:8px;border-radius:50%;background:var(--cinnabar);flex:0 0 8px}
.notif-row .btn{height:32px;padding:0 13px;font-size:11px;border-radius:var(--r-sm)}

/* 个人设置 */
.settings-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:20px;align-items:start}
.settings-grid .card{display:flex;flex-direction:column}
.kv-row{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:11px 0;
  border-bottom:1px solid var(--line-soft);font-size:12px;color:var(--tx-2)}
.kv-row:last-of-type{border-bottom:none}
.switch-row{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:11px 0;
  border-bottom:1px solid var(--line-soft)}
.switch-row:last-of-type{border-bottom:none}
.switch-row>span{font-size:12px;color:var(--tx-2);display:flex;flex-direction:column;gap:2px;min-width:0}
.switch-row>span b{font-size:10.5px;font-weight:400;color:var(--tx-4)}
.switch{width:40px;height:22px;flex:0 0 40px;border-radius:11px;background:var(--line);
  border:1px solid var(--line);position:relative;transition:background-color var(--ease),border-color var(--ease)}
.switch::after{content:"";position:absolute;top:2px;left:2px;width:16px;height:16px;border-radius:50%;
  background:var(--tx-4);transition:transform var(--ease),background-color var(--ease)}
.switch.is-on{background:var(--gold);border-color:var(--gold)}
.switch.is-on::after{transform:translateX(18px);background:var(--on-gold)}
.about-note{margin-top:12px;padding-top:12px;border-top:1px solid var(--line-soft);
  font-size:11px;line-height:1.75;color:var(--tx-4)}
.seg--sm button{height:26px;padding:0 10px;font-size:11px}

/* 信息密度偏好（真实生效） */
html.density-compact .view{padding:20px}
html.density-compact .card{padding:12px}
html.density-compact .bench{gap:10px}
html.density-compact .caps,html.density-compact .cards3{gap:14px}
html.density-compact .opt{height:32px}

@media (max-width:900px){
  .admin-grid,.settings-grid{grid-template-columns:1fr;gap:14px}
  .audit-row .acts{flex-basis:100%;justify-content:flex-end}
  .notif-row{flex-wrap:wrap}
  .notif-row .nact{flex-basis:100%;justify-content:flex-end}
  .toolrow{margin-bottom:18px}
}

"""
rep(u'/* ════ 基础移动端适配（≤900px） ════ */', CSS + u'/* ════ 基础移动端适配（≤900px） ════ */', 1, 'CSS 块')

# ─────────────────────────────── 2) META ───────────────────────────────
rep(
u"""    archive: ['数字档案', '佛光寺东大殿 · 唐代 · 档案编号 AVH-0857']
  };""",
u"""    archive: ['数字档案', '佛光寺东大殿 · 唐代 · 档案编号 AVH-0857'],
    admin:   ['审核工作台', '内容审核 · RBAC 权限隔离 · 仅管理员可见'],
    notifications: ['通知中心', '系统通知与站内信 · 数字锦盒已送达'],
    settings: ['个人设置', '视觉偏好 · 账号资料 · 通知策略']
  };""", 1, 'META 表')

# ─────────────────────── 3) 用户菜单 / 铃铛接线 ───────────────────────
rep(u'data-demo-toast="设置中心（预览页未实现）"', u'data-go="settings"', 2, '菜单→设置')
rep(u'data-demo-toast="通知中心（预览页未实现）"', u'data-go="notifications"', 3, '菜单/铃铛→通知')
rep(u'data-demo-toast="管理控制台（预览页未实现）"', u'data-go="admin"', 1, '菜单→审核台')

# ────────────────────────── 4) 三个新页面 ──────────────────────────
BACK = (u'<button class="btn btn--ghost" data-go="feed" style="height:36px;padding:0 14px;font-size:12px">'
        u'<svg><use href="#i-back"/></svg>返回营造志</button>')

ADMIN = u"""
      <!-- ═══════════ ⑤ 审核工作台（V4 轻量重设计） ═══════════ -->
      <section class="view" id="v-admin">
        <div class="toolrow">
          """ + BACK + u"""
          <div class="headstats">
            <span class="pill pill--gold"><i></i>待审核 <b id="pendingN">5</b></span>
            <span class="pill pill--jade"><i></i>今日通过 24</span>
            <span class="pill pill--mut">今日驳回 3</span>
          </div>
        </div>

        <div class="admin-grid">
          <div>
            <div class="card" style="padding:14px 16px">
              <div class="cardhd" style="margin-bottom:0">
                <h3>待审队列</h3>
                <span class="k">按提交时间正序 · 先到先审</span>
              </div>
            </div>
            <div id="auditList"></div>
            <div class="empty" id="auditEmpty" hidden>
              <svg viewBox="0 0 40 40"><path d="M20 4L34 10V20.5C34 28 27.5 34.5 20 37C12.5 34.5 6 28 6 20.5V10L20 4Z"/><path d="M14.5 20.2L18.4 24.1L26 16.5"/></svg>
              待审队列已清空 · 全部内容已处理
            </div>
          </div>

          <div class="col" style="gap:16px">
            <div class="card">
              <div class="cardhd"><h3>审核要点</h3><span class="k">RUBRIC</span></div>
              <ul class="rules">
                <li>影像须为古建实拍或本平台 AI 生成，不得含第三方水印</li>
                <li>标题不得使用夸大或营销化表述</li>
                <li>涉及具体文保单位须标注地域与朝代</li>
                <li>驳回必须填写理由，理由将同步至作者站内信</li>
              </ul>
            </div>
            <div class="card card--jade">
              <div class="cardhd"><h3>最近操作</h3><span class="k">AUDIT LOG</span></div>
              <div class="loglist">
                <div class="logrow"><span class="act">通过</span><b>幻筑 · 宋式重檐歇山殿</b><span class="t">2 分钟前</span></div>
                <div class="logrow"><span class="act">驳回</span><b>未命名古建合集</b><span class="t">18 分钟前</span></div>
                <div class="logrow"><span class="act">通过</span><b>独乐寺观音阁 · 腰檐暗层</b><span class="t">1 小时前</span></div>
              </div>
            </div>
          </div>
        </div>
      </section>
"""

NOTIF = u"""
      <!-- ═══════════ ⑥ 通知中心（V4 轻量重设计） ═══════════ -->
      <section class="view" id="v-notifications">
        <div class="toolrow">
          """ + BACK + u"""
          <div class="headstats">
            <span class="pill pill--red"><i></i>未读 <b id="unreadN">3</b> 条</span>
            <button class="btn btn--ghost" id="markAll" style="height:36px;padding:0 14px;font-size:12px">全部标记已读</button>
          </div>
        </div>

        <div class="chips" style="margin-bottom:16px" data-seg="notif">
          <button class="chip is-on" data-v="all">全部</button>
          <button class="chip" data-v="box">数字锦盒</button>
          <button class="chip" data-v="audit">审核结果</button>
          <button class="chip" data-v="social">互动</button>
        </div>

        <div class="notif-list" id="notifList"></div>
        <div class="empty" id="notifEmpty" hidden>
          <svg viewBox="0 0 40 40"><path d="M10 17.5C10 11.5 14.4 7 20 7C25.6 7 30 11.5 30 17.5V25L33.5 29H6.5L10 25V17.5Z"/><path d="M16.5 32.5C17.2 34 18.5 34.8 20 34.8C21.5 34.8 22.8 34 23.5 32.5"/></svg>
          该分类下暂无通知
        </div>
      </section>
"""

SETTINGS = u"""
      <!-- ═══════════ ⑦ 个人设置（V4 轻量重设计） ═══════════ -->
      <section class="view" id="v-settings">
        <div class="toolrow">
          """ + BACK + u"""
          <div class="headstats">
            <span class="pill pill--mut">账号 liangsc@zhiguan.cn</span>
            <span class="pill pill--gold"><i></i>ADMIN</span>
          </div>
        </div>

        <div class="settings-grid">
          <section class="card">
            <div class="cardhd"><h3>视觉偏好</h3><span class="k">VISUAL</span></div>
            <div class="kv-row">
              <span>昼夜流转</span>
              <div class="seg seg--sm" data-seg="theme">
                <button data-v="dark" class="is-on">玄墨</button>
                <button data-v="light">宣纸</button>
              </div>
            </div>
            <div class="kv-row">
              <span>信息密度</span>
              <div class="seg seg--sm" data-seg="density">
                <button data-v="cozy" class="is-on">舒展</button>
                <button data-v="compact">紧凑</button>
              </div>
            </div>
            <p class="about-note">玄墨为默认模式，为路演投影与三维素材提供最高对比度；宣纸适合长时间阅读文献。</p>
          </section>

          <section class="card">
            <div class="cardhd"><h3>账号资料</h3><span class="k">ACCOUNT</span></div>
            <div class="kv"><span class="k">昵称</span><span class="v v--paper">梁思成</span></div>
            <div class="kv"><span class="k">邮箱</span><span class="v v--paper">liangsc@zhiguan.cn</span></div>
            <div class="kv"><span class="k">角色</span><span class="v v--gold">ADMIN</span></div>
            <div class="kv"><span class="k">认证</span><span class="v v--jade">VGGT 认证匠人</span></div>
            <div class="kv"><span class="k">文化签名</span><span class="v v--paper">以技术为舟，载文化远航</span></div>
            <button class="btn btn--ghost btn--block" style="margin-top:12px;height:38px;font-size:12px" data-demo-toast="编辑资料（预览页未实现）">编辑资料</button>
          </section>

          <section class="card">
            <div class="cardhd"><h3>通知策略</h3><span class="k">NOTIFY</span></div>
            <div class="switch-row">
              <span>数字锦盒送达提醒<b>幻筑完成时弹窗并推送站内信</b></span>
              <button class="switch is-on" role="switch" aria-checked="true" aria-label="数字锦盒送达提醒"></button>
            </div>
            <div class="switch-row">
              <span>审核结果通知<b>内容通过或被驳回时提醒</b></span>
              <button class="switch is-on" role="switch" aria-checked="true" aria-label="审核结果通知"></button>
            </div>
            <div class="switch-row">
              <span>互动通知<b>被关注、被点赞、被评论</b></span>
              <button class="switch" role="switch" aria-checked="false" aria-label="互动通知"></button>
            </div>
            <div class="switch-row">
              <span>邮件摘要<b>每周一封，汇总你的数字档案动态</b></span>
              <button class="switch" role="switch" aria-checked="false" aria-label="邮件摘要"></button>
            </div>
          </section>

          <section class="card">
            <div class="cardhd"><h3>关于平台</h3><span class="k">ABOUT</span></div>
            <div class="kv"><span class="k">平台版本</span><span class="v v--paper">智观·古建 V1.3</span></div>
            <div class="kv"><span class="k">前端技术栈</span><span class="v v--paper">Vue 3 · Vite 5 · Element Plus</span></div>
            <div class="kv"><span class="k">解析引擎</span><span class="v v--jade">VGGT-Long v2</span></div>
            <div class="kv"><span class="k">渲染管线</span><span class="v v--paper">WebGL · PBR · Draco</span></div>
            <div class="kv"><span class="k">后端</span><span class="v v--paper">Spring Boot 3.2 · MySQL · Redis</span></div>
            <p class="about-note">本项目为大学生创新创业训练计划（大创）作品。
              三维几何解析结果由 VGGT 自动生成，文化解读为 AI 转译，非人工考据。</p>
          </section>
        </div>
      </section>
"""

rep(u"""    </div>
  </main>""", ADMIN + NOTIF + SETTINGS + u"""
    </div>
  </main>""", 1, '插入三个页面')

# ────────────────────────── 5) JS 逻辑 ──────────────────────────
JS = u"""
  /* ═══ 次级页面：审核工作台 ═══ */
  var AUDIT = [
    { id: 'a1', title: '唐风重檐歇山殿 · 雨后', author: '青竹', time: '8 分钟前', tags: '唐代 · 歇山顶' },
    { id: 'a2', title: '独乐寺观音阁 · 腰檐暗层', author: '沈砚', time: '22 分钟前', tags: '辽代 · 木构' },
    { id: 'a3', title: '未命名古建合集（含第三方水印）', author: '匿名', time: '35 分钟前', tags: '合集' },
    { id: 'a4', title: '幻筑 · 宋式庑殿顶正殿', author: '梁思成', time: '1 小时前', tags: 'AI 幻筑' },
    { id: 'a5', title: '飞檐与斗栱 · 细部', author: '陈墨', time: '1 小时前', tags: '斗栱' }
  ];
  function renderAudit() {
    var list = $('#auditList');
    list.innerHTML = AUDIT.map(function (p) {
      return '' +
        '<article class="audit-row" data-id="' + p.id + '">' +
          '<div class="thumb"><img src="assets/eave.jpg" width="724" height="1086" alt="" loading="lazy" decoding="async" /></div>' +
          '<div class="info"><b>' + p.title + '</b><span class="meta">' + p.author + ' · ' + p.time + ' · ' + p.tags + '</span></div>' +
          '<div class="acts">' +
            '<button class="btn btn--gold" data-approve>通过</button>' +
            '<button class="btn btn--ghost" data-reject>驳回</button>' +
          '</div>' +
          '<div class="reject-box" hidden>' +
            '<input type="text" placeholder="填写驳回理由（必填，将同步给作者）" />' +
            '<button class="btn btn--ghost" data-cancel>取消</button>' +
            '<button class="btn btn--gold" data-confirm>确认驳回</button>' +
          '</div>' +
        '</article>';
    }).join('');
    $('#pendingN').textContent = AUDIT.length;
    $('#auditEmpty').hidden = AUDIT.length > 0;
  }
  function auditTitle(id) {
    for (var i = 0; i < AUDIT.length; i++) if (AUDIT[i].id === id) return AUDIT[i].title;
    return '';
  }
  function removeAudit(id) {
    AUDIT = AUDIT.filter(function (x) { return x.id !== id; });
    renderAudit();
  }
  $('#auditList').addEventListener('click', function (e) {
    var row = e.target.closest('.audit-row'); if (!row) return;
    var id = row.dataset.id, box = row.querySelector('.reject-box');
    if (e.target.closest('[data-approve]')) { var t = auditTitle(id); removeAudit(id); toast('已通过：「' + t + '」'); return; }
    if (e.target.closest('[data-reject]')) { box.hidden = false; box.querySelector('input').focus(); return; }
    if (e.target.closest('[data-cancel]')) { box.hidden = true; return; }
    if (e.target.closest('[data-confirm]')) {
      var inp = box.querySelector('input');
      if (!inp.value.trim()) { toast('驳回理由为必填'); inp.focus(); return; }
      var t2 = auditTitle(id); removeAudit(id); toast('已驳回：「' + t2 + '」理由已同步作者');
    }
  });

  /* ═══ 次级页面：通知中心 ═══ */
  var NOTIF = [
    { id: 'n1', type: 'box',   title: '数字锦盒已送达', body: '您的「宋式重檐歇山殿」已营造完成，点此拆阅。', time: '2 分钟前', unread: true, box: true },
    { id: 'n2', type: 'audit', title: '内容审核通过',   body: '您发布的「佛光寺东大殿 · 侧立面」已通过审核，现已展示于匠人社区。', time: '26 分钟前', unread: true },
    { id: 'n3', type: 'social',title: '青竹 关注了你',  body: '对方同时收藏了你的 2 件数字档案。', time: '1 小时前', unread: true },
    { id: 'n4', type: 'box',   title: '数字锦盒已送达', body: '您的「唐风重檐歇山殿」已营造完成。', time: '昨天', unread: false, box: true },
    { id: 'n5', type: 'audit', title: '内容已驳回',     body: '驳回理由：影像含第三方水印，请更换素材后重新提交。', time: '昨天', unread: false },
    { id: 'n6', type: 'social',title: '沈砚 评论了你',  body: '“斗栱的层次感处理得很好，请问用了哪一代引擎？”', time: '2 天前', unread: false }
  ];
  var notifFilter = 'all';
  function notifIcon(t) {
    if (t === 'box') return ['nicon--gold', 'i-spark'];
    if (t === 'audit') return ['nicon--jade', 'i-shield'];
    return ['nicon--red', 'i-users'];
  }
  function renderNotif(type) {
    if (type) notifFilter = type;
    var rows = NOTIF.filter(function (n) { return notifFilter === 'all' || n.type === notifFilter; });
    $('#notifList').innerHTML = rows.map(function (n) {
      var ic = notifIcon(n.type);
      return '' +
        '<article class="notif-row' + (n.unread ? ' is-unread' : '') + '" data-id="' + n.id + '">' +
          '<div class="nicon ' + ic[0] + '"><svg><use href="#' + ic[1] + '"/></svg></div>' +
          '<div class="nbody"><b>' + n.title + '</b><p>' + n.body + '</p><span class="t">' + n.time + '</span></div>' +
          '<div class="nact">' + (n.unread ? '<span class="unread-dot"></span>' : '') +
            (n.box ? '<button class="btn btn--outline" data-open-box>打开锦盒</button>' : '') + '</div>' +
        '</article>';
    }).join('');
    $('#notifEmpty').hidden = rows.length > 0;
    $('#unreadN').textContent = NOTIF.filter(function (n) { return n.unread; }).length;
  }
  function findNotif(id) {
    for (var i = 0; i < NOTIF.length; i++) if (NOTIF[i].id === id) return NOTIF[i];
    return null;
  }
  $('#notifList').addEventListener('click', function (e) {
    var row = e.target.closest('.notif-row'); if (!row) return;
    var n = findNotif(row.dataset.id); if (!n) return;
    if (e.target.closest('[data-open-box]')) toast('已拆阅数字锦盒：「' + n.title + '」');
    n.unread = false;
    renderNotif();
  });
  $('#markAll').addEventListener('click', function () {
    NOTIF.forEach(function (n) { n.unread = false; });
    renderNotif();
    toast('已全部标记为已读');
  });

  /* ═══ 次级页面：设置开关 ═══ */
  $$('.switch').forEach(function (s) {
    s.addEventListener('click', function () {
      var on = s.classList.toggle('is-on');
      s.setAttribute('aria-checked', on ? 'true' : 'false');
    });
  });

  renderAudit();
  renderNotif('all');
"""
rep(u"""  setState('done');
  setTheme('dark');""", JS + u"""
  setState('done');
  setTheme('dark');""", 1, 'JS 逻辑块')

# ──────────────────── 6) 分段控件处理扩展 ────────────────────
rep(
u"""      if (kind === 'fx') document.documentElement.classList.toggle('no-fx', b.dataset.v === 'off');""",
u"""      if (kind === 'fx') document.documentElement.classList.toggle('no-fx', b.dataset.v === 'off');
      if (kind === 'density') document.documentElement.classList.toggle('density-compact', b.dataset.v === 'compact');
      if (kind === 'notif') renderNotif(b.dataset.v);""", 1, '分段处理扩展')

# ──────────────────── 7) 控制台说明文案 ────────────────────
rep(
u"""      ① 点侧栏用户条或顶栏头像可打开账户菜单；<br>
      ② 帧率低于 50 时指示灯转为警示色；<br>
      ③ 若「特效 关」后帧率明显回升，说明瓶颈在模糊/滤镜；<br>
      ④ 若两者相近而仍卡顿，请把帧率读数与你的设备告诉我。""",
u"""      ① 点侧栏用户条或顶栏头像可打开账户菜单，菜单里的<b>设置 / 通知中心 / 管理控制台</b>已可实现跳转；<br>
      ② 新增三个次级页面：<b>审核工作台</b>（可逐条通过/驳回，驳回需填理由）、<b>通知中心</b>（可筛选、逐条已读、全部已读）、<b>个人设置</b>（昼夜流转与信息密度真实生效）；<br>
      ③ 帧率低于 50 时指示灯转为警示色；若「特效 关」后明显回升，说明瓶颈在模糊/滤镜；<br>
      ④ 若两种状态帧率相近却仍卡顿，请把帧率读数与设备情况告诉我。""", 1, '控制台文案')

io.open(P, 'w', encoding='utf-8', newline='').write(src)
print('\n'.join(report))
print('\nWROTE %s  (%d -> %d bytes)' % (P, len(orig.encode('utf-8')), len(src.encode('utf-8'))))
