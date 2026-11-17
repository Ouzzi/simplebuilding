/* Zoom + full screen for large scroll areas of the wiki (N72, plan docs/ai/PLAN-N72-TZEDITOR-2026-11-16.md).
   One component for all of them: the test centre editor grid, the texture list, the recipe lists, large tables.
   - bar above the area: minus, zoom level (click = 100 %), plus, "Full screen" / "Back" (also Esc);
   - Ctrl + mouse wheel (and the trackpad pinch, which browsers send as Ctrl + wheel) over the area zooms the area;
     preventDefault only there, everywhere else the browser zoom stays as it is;
   - the zoom level stays per area (sessionStorage) - across full screen / normal and when the page redraws it.
   Use: SBZoom.attach(element, { key, lang, mode, apply, min, max }) or data-zoom="<key>" + SBZoom.scan(root, lang).
   mode "self" zooms the element (content boxes), "inner" its children (scroll boxes like .table-wrap, whose own
   max-height in vh must not grow), apply(f) replaces both (the editor scales its cells itself).
   Styles use the wiki tokens (--panel, --border, --accent, --bg ...), so light and dark mode follow the page. */
(function (root) {
  'use strict';
  var STR = {
    en: { zoomOut: 'Zoom out', zoomIn: 'Zoom in', reset: 'Zoom level - click for 100 %', full: 'Full screen', fullTip: 'Fill the window with this area',
      back: 'Back', backTip: 'Back to the page (Esc)', hint: 'Ctrl + mouse wheel zooms this area', region: 'Zoomable area' },
    de: { zoomOut: 'Verkleinern', zoomIn: 'Vergrößern', reset: 'Zoomstufe - Klick für 100 %', full: 'Vollbild', fullTip: 'Diese Fläche füllt das Fenster',
      back: 'Zurück', backTip: 'Zurück zur Seite (Esc)', hint: 'Strg + Mausrad zoomt diese Fläche', region: 'Zoombare Fläche' }
  };
  var LEVELS = [0.25, 0.33, 0.5, 0.67, 0.75, 0.8, 0.9, 1, 1.1, 1.25, 1.5, 1.75, 2, 2.5, 3, 4];
  var CSS = [
    '.zm{position:relative;margin:.3rem 0 .6rem}',
    '.zm-bar{display:flex;flex-wrap:wrap;align-items:center;gap:.3rem;margin:0 0 .35rem;font-size:.82rem}',
    '.zm-bar .zm-b{font:inherit;display:inline-flex;align-items:center;justify-content:center;gap:.3rem;height:28px;min-width:28px;padding:0 .5rem;border:1px solid var(--border);border-radius:var(--radius-sm,6px);background:var(--panel-2,var(--panel));color:var(--text);cursor:pointer;line-height:1}',
    '.zm-bar .zm-b:hover{border-color:var(--accent)}.zm-bar .zm-b:focus-visible{outline:2px solid var(--accent);outline-offset:1px}',
    '.zm-bar .zm-b:disabled{opacity:.45;cursor:default}.zm-bar .zm-lvl{min-width:4.2em;font-variant-numeric:tabular-nums}',
    '.zm-bar .zm-full svg{width:14px;height:14px}.zm-bar .zm-hint{color:var(--muted,inherit);opacity:.8}',
    '.zm-self{zoom:var(--zm,1)}.zm-inner>*{zoom:var(--zm,1)}',
    '.zm.zm-on{position:fixed;inset:0;z-index:150;margin:0;display:flex;flex-direction:column;background:var(--bg);color:var(--text);padding:.5rem .7rem .7rem}',
    '.zm.zm-on>.zm-bar{flex:none;background:var(--panel);border:1px solid var(--accent);border-radius:8px;padding:.3rem .5rem;box-shadow:var(--shadow-lg,0 4px 18px rgba(0,0,0,.3))}',
    '.zm.zm-on>.zm-view{flex:1;min-height:0;overflow:auto;border:1px solid var(--border);border-radius:6px;background:var(--panel-2,var(--panel));padding:6px}',
    '.zm.zm-on>.zm-view>*{max-height:none!important;overflow:visible!important}',
    'body.zm-lock{overflow:hidden}'
  ].join('\n');
  var ICON_FULL = '<svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.6" aria-hidden="true"><path d="M2 6V2h4M10 2h4v4M14 10v4h-4M6 14H2v-4"/></svg>';
  var ICON_BACK = '<svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.6" aria-hidden="true"><path d="M6 2v4H2M14 6h-4V2M10 14v-4h4M2 10h4v4"/></svg>';

  /* pure helpers (node test wiki/tests/zoomarea_core.cjs) */
  function clamp(f, min, max) { f = +f; if (!(f > 0)) f = 1; return Math.min(max, Math.max(min, f)); }
  function stepLevel(f, dir, min, max) {
    var lv = LEVELS.filter(function (l) { return l >= min - 1e-9 && l <= max + 1e-9; }), i;
    if (dir > 0) { for (i = 0; i < lv.length; i++) if (lv[i] > f + 1e-6) return lv[i]; return clamp(f, min, max); }
    for (i = lv.length - 1; i >= 0; i--) if (lv[i] < f - 1e-6) return lv[i];
    return clamp(f, min, max);
  }
  function pct(f) { return Math.round(f * 100) + ' %'; }

  var mem = {};      // key -> { zoom, full } of this page view
  var live = [];     // attached areas
  function stored(key) { try { var v = root.sessionStorage && root.sessionStorage.getItem('sbzoom:' + key); return v ? +v : null; } catch (e) { return null; } }
  function store(key, f) { try { if (root.sessionStorage) root.sessionStorage.setItem('sbzoom:' + key, String(f)); } catch (e) { /* private mode */ } }
  function ensureCss() {
    if (document.getElementById('zm-css')) return;
    var s = document.createElement('style'); s.id = 'zm-css'; s.textContent = CSS; document.head.appendChild(s);
  }
  function lockBody() { document.body.classList.toggle('zm-lock', live.some(function (a) { return a.full && a.zm.isConnected; })); }

  function attach(target, opts) {
    if (!target) return null;
    if (target.parentNode && target.parentNode.classList && target.parentNode.classList.contains('zm-view')) return target.__zm || null;
    opts = opts || {};
    ensureCss();
    var key = opts.key || target.getAttribute('data-zoom') || 'area';
    var S = STR[opts.lang === 'de' ? 'de' : 'en'];
    var min = opts.min || 0.25, max = opts.max || 4;
    var m = mem[key] || (mem[key] = { zoom: clamp(stored(key) || opts.initial || 1, min, max), full: false });
    var zm = document.createElement('div'); zm.className = 'zm' + (opts.className ? ' ' + opts.className : ''); zm.setAttribute('data-zm', key);
    var bar = document.createElement('div'); bar.className = 'zm-bar'; bar.setAttribute('role', 'toolbar'); bar.setAttribute('aria-label', S.region);
    bar.innerHTML = '<button type="button" class="zm-b zm-out" data-zm-act="out" title="' + S.zoomOut + '" aria-label="' + S.zoomOut + '">−</button>'
      + '<button type="button" class="zm-b zm-lvl" data-zm-act="reset" title="' + S.reset + '"></button>'
      + '<button type="button" class="zm-b zm-in" data-zm-act="in" title="' + S.zoomIn + '" aria-label="' + S.zoomIn + '">+</button>'
      + '<button type="button" class="zm-b zm-full" data-zm-act="full"></button>'
      + (opts.extra ? '<span class="zm-extra"></span>' : '');
    bar.title = S.hint; /* N73 Runde 4: Hinweis als Tooltip statt Dauertext neben jeder Liste */
    var view = document.createElement('div'); view.className = 'zm-view';
    target.parentNode.insertBefore(zm, target);
    zm.appendChild(bar); zm.appendChild(view); view.appendChild(target);
    if (!opts.apply) target.classList.add(opts.mode === 'inner' ? 'zm-inner' : 'zm-self');
    var a = { key: key, zm: zm, target: target, full: false, opts: opts };
    live.push(a);

    function paint() {
      bar.querySelector('.zm-lvl').textContent = pct(m.zoom);
      bar.querySelector('.zm-out').disabled = m.zoom <= min + 1e-6;
      bar.querySelector('.zm-in').disabled = m.zoom >= max - 1e-6;
      var fb = bar.querySelector('.zm-full');
      fb.innerHTML = (a.full ? ICON_BACK + S.back : ICON_FULL + S.full);
      fb.title = a.full ? S.backTip : S.fullTip;
      fb.setAttribute('aria-pressed', a.full ? 'true' : 'false');
    }
    function setZoom(f, anchor) {
      f = clamp(f, min, max);
      var sc = a.full ? view : null, before = m.zoom;
      m.zoom = f; store(key, f);
      if (opts.apply) opts.apply(f, target); else target.style.setProperty('--zm', String(f));
      // keep the point under the mouse in place while zooming in full screen (scroll box = the view)
      if (sc && anchor && before) {
        var r = sc.getBoundingClientRect(), x = anchor.clientX - r.left, y = anchor.clientY - r.top, k = f / before;
        sc.scrollLeft = (sc.scrollLeft + x) * k - x; sc.scrollTop = (sc.scrollTop + y) * k - y;
      }
      paint();
    }
    function setFull(on) {
      a.full = !!on; m.full = a.full;
      zm.classList.toggle('zm-on', a.full);
      lockBody(); paint();
      if (opts.onFull) opts.onFull(a.full);
      var fb = bar.querySelector('.zm-full'); if (fb && document.activeElement && zm.contains(document.activeElement)) fb.focus();
    }
    bar.addEventListener('click', function (e) {
      var b = e.target.closest('[data-zm-act]'); if (!b) return;
      var act = b.getAttribute('data-zm-act');
      if (act === 'in') setZoom(stepLevel(m.zoom, 1, min, max));
      else if (act === 'out') setZoom(stepLevel(m.zoom, -1, min, max));
      else if (act === 'reset') setZoom(1);
      else if (act === 'full') setFull(!a.full);
    });
    zm.addEventListener('wheel', function (e) {
      if (!e.ctrlKey) return;            // plain wheel scrolls as always
      e.preventDefault();                // only here: the browser page zoom stays untouched
      var dy = e.deltaY || e.deltaX; if (!dy) return;
      var f = Math.abs(dy) < 20 ? m.zoom * Math.exp(-dy * 0.01) : stepLevel(m.zoom, dy < 0 ? 1 : -1, min, max);  // pinch = fine, wheel = steps
      setZoom(f, e);
    }, { passive: false });
    a.setZoom = setZoom; a.setFull = setFull; a.zoom = function () { return m.zoom; };
    if (opts.extra) bar.querySelector('.zm-extra').appendChild(opts.extra);
    target.__zm = a;
    setZoom(m.zoom);
    if (m.full) setFull(true); else paint();
    return a;
  }

  /* attach every [data-zoom] below root that is not attached yet; forget areas that left the page */
  function scan(rootEl, lang, onlyKeys) {
    live = live.filter(function (a) { return a.zm.isConnected; });
    Object.keys(mem).forEach(function (k) { if (!live.some(function (a) { return a.key === k; })) mem[k].full = false; });
    if (rootEl && rootEl.querySelectorAll) rootEl.querySelectorAll('[data-zoom]').forEach(function (el) {
      if (el.__zm && el.__zm.zm.isConnected) return;
      if (onlyKeys && onlyKeys.indexOf(el.getAttribute('data-zoom')) < 0) return;
      attach(el, { key: el.getAttribute('data-zoom'), lang: lang, mode: el.getAttribute('data-zoom-mode') || 'self' });
    });
    lockBody();
  }
  function exitAll() { var done = false; live.forEach(function (a) { if (a.full && a.zm.isConnected) { a.setFull(false); done = true; } }); return done; }
  function get(key) { for (var i = live.length - 1; i >= 0; i--) if (live[i].key === key && live[i].zm.isConnected) return live[i]; return null; }

  if (typeof window !== 'undefined' && typeof document !== 'undefined') {
    /* Esc leaves full screen - unless a dialog on top (texture lightbox ...) took the key already */
    window.addEventListener('keydown', function (e) { if (e.key === 'Escape' && !e.defaultPrevented && exitAll()) e.preventDefault(); });
  }
  var api = { attach: attach, scan: scan, exitAll: exitAll, get: get, STR: STR, LEVELS: LEVELS, clamp: clamp, stepLevel: stepLevel, pct: pct };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.SBZoom = api;
})(typeof self !== 'undefined' ? self : this);
