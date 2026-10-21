/* Wiki pixel editor with layers (dev mirror only; opened by tools/zentrale-web/dev-overlay.js).
 * Functions and UX after the Resprite analysis (docs/ai/ANALYSE-RESPRITE-2026-10-21.md), own code.
 * Needs window.SBPixelCore (assets/pixeleditor-core.js). The wiki is static: results are downloaded
 * (<name>.png + <name>.layers.json), a dev puts both next to the original and runs tools/textures/layers.py check.
 *
 * window.SBPixelEditor.open({id, name, png, layers, source}) - png/layers: URLs (layers optional). */
(function () {
  'use strict';
  if (window.SBPixelEditor) return;
  var C = window.SBPixelCore;

  /* ---------------- texts (EN/DE like the wiki) */
  var STR = {
    en: {
      title: 'Pixel editor', undo: 'Undo (Ctrl+Z)', redo: 'Redo (Ctrl+Y)', grid: 'Grid (Ctrl+\')', symX: 'Mirror left/right', symY: 'Mirror top/bottom',
      pp: 'Pixel perfect', open: 'Open file', exp: 'Export', close: 'Close (Esc)',
      pencil: 'Pencil (B)', eraser: 'Eraser (E)', fill: 'Fill (G, Shift: same colour everywhere)', picker: 'Eyedropper (I, Alt)', line: 'Line (L)',
      rect: 'Rectangle (U)', select: 'Select (M)', move: 'Move (V, arrows)', filled: 'Filled', global: 'Everywhere',
      colors: 'Colours', swap: 'Swap (X)', alpha: 'Alpha', palTex: 'This texture', palVan: 'Vanilla-near', gpl: 'Palette (.gpl)',
      layers: 'Layers', add: 'New layer (Shift+N)', dup: 'Duplicate', del: 'Delete layer', up: 'Up', down: 'Down', merge: 'Merge down',
      rename: 'Rename', lock: 'Alpha lock', opacity: 'Opacity', show: 'Show/hide', layerName: 'Layer name',
      region: 'Selection / layer', flipH: 'Flip ↔', flipV: 'Flip ↕', rot: 'Rotate 90°', clear: 'Clear (Del)', copy: 'Copy (Ctrl+C)', paste: 'Paste (Ctrl+V)', deselect: 'Deselect (Ctrl+D)',
      preview: 'Preview', p1: '1:1', p2: '2:1', tile: 'Tiled 3×3', ref: 'Reference image', refPick: 'Click the image to pick a colour.',
      both: 'PNG + layers', png: 'PNG', json: 'Layers (.layers.json)',
      howto: 'Put {png} and {json} next to the original ({src}), then run python tools/textures/layers.py check and python wiki/generate.py --all.',
      derived: 'Vanilla-derived texture: never commit it (A8). Edit tools/textures/vanilla_derived.py instead.',
      loading: 'Loading…', loadFail: 'Could not load the texture: ', dirty: 'Discard the changes?', hidden: 'The active layer is hidden.',
      keys: 'B E G I L U M V · X swap · Ctrl+Z/Y · +/− zoom · 0 fit · two fingers: zoom/pan, tap = undo, three = redo · long press = eyedropper',
      fromLayers: 'layers from the layer source', fromPng: 'no layer source yet: PNG as one layer', base: 'Base', pasted: 'Pasted', layer: 'Layer'
    },
    de: {
      title: 'Pixeleditor', undo: 'Rückgängig (Strg+Z)', redo: 'Wiederholen (Strg+Y)', grid: 'Raster (Strg+\')', symX: 'Spiegeln links/rechts', symY: 'Spiegeln oben/unten',
      pp: 'Pixel Perfect', open: 'Datei öffnen', exp: 'Export', close: 'Schließen (Esc)',
      pencil: 'Stift (B)', eraser: 'Radierer (E)', fill: 'Füllen (G, Umschalt: gleiche Farbe überall)', picker: 'Pipette (I, Alt)', line: 'Linie (L)',
      rect: 'Rechteck (U)', select: 'Auswahl (M)', move: 'Verschieben (V, Pfeile)', filled: 'Gefüllt', global: 'Überall',
      colors: 'Farben', swap: 'Tauschen (X)', alpha: 'Deckung', palTex: 'Diese Textur', palVan: 'Vanilla-nah', gpl: 'Palette (.gpl)',
      layers: 'Ebenen', add: 'Neue Ebene (Umschalt+N)', dup: 'Duplizieren', del: 'Ebene löschen', up: 'Hoch', down: 'Runter', merge: 'Nach unten zusammenführen',
      rename: 'Umbenennen', lock: 'Alpha-Sperre', opacity: 'Deckkraft', show: 'Ein-/ausblenden', layerName: 'Ebenen-Name',
      region: 'Auswahl / Ebene', flipH: 'Spiegeln ↔', flipV: 'Spiegeln ↕', rot: '90° drehen', clear: 'Leeren (Entf)', copy: 'Kopieren (Strg+C)', paste: 'Einfügen (Strg+V)', deselect: 'Auswahl aufheben (Strg+D)',
      preview: 'Vorschau', p1: '1:1', p2: '2:1', tile: 'Kachel 3×3', ref: 'Referenzbild', refPick: 'Bild anklicken = Farbe aufnehmen.',
      both: 'PNG + Ebenen', png: 'PNG', json: 'Ebenen (.layers.json)',
      howto: '{png} und {json} neben das Original legen ({src}), dann python tools/textures/layers.py check und python wiki/generate.py --all.',
      derived: 'Vanilla-abgeleitete Textur: nie einchecken (A8), stattdessen tools/textures/vanilla_derived.py anpassen.',
      loading: 'Lädt…', loadFail: 'Textur nicht ladbar: ', dirty: 'Änderungen verwerfen?', hidden: 'Die aktive Ebene ist ausgeblendet.',
      keys: 'B E G I L U M V · X tauschen · Strg+Z/Y · +/− Zoom · 0 einpassen · zwei Finger: Zoom/Verschieben, Tipp = rückgängig, drei = wiederholen · Langdruck = Pipette',
      fromLayers: 'Ebenen aus der Ebenen-Quelle', fromPng: 'noch keine Ebenen-Quelle: PNG als eine Ebene', base: 'Grundform', pasted: 'Eingefügt', layer: 'Ebene'
    }
  };
  function lang() { var l = String(window.LANG || document.documentElement.lang || 'en'); return l.indexOf('de') === 0 ? 'de' : 'en'; }
  function T(k, vars) {
    var s = (STR[lang()] || STR.en)[k] || STR.en[k] || k;
    if (vars) Object.keys(vars).forEach(function (v) { s = s.split('{' + v + '}').join(vars[v]); });
    return s;
  }

  /* ---------------- style: tokens light/dark (wiki's data-theme wins), contrast >= 4.5:1 (A7) */
  var CSS = [
    '.pe-root{--pe-bg:#efebe4;--pe-panel:#ffffff;--pe-line:#c9c1b4;--pe-text:#1d1b18;--pe-muted:#5a544c;--pe-btn:#e6e0d6;--pe-acc:#2f5aa0;--pe-acc-text:#ffffff;--pe-warn:#8a4b00;--pe-stage:#d9d3c9}',
    '@media (prefers-color-scheme: dark){:root:not([data-theme="light"]) .pe-root{--pe-bg:#171513;--pe-panel:#221f1c;--pe-line:#3f3a34;--pe-text:#ece6dc;--pe-muted:#b4aca0;--pe-btn:#332e29;--pe-acc:#3b5b8c;--pe-acc-text:#ffffff;--pe-warn:#f0b35c;--pe-stage:#2b2723}}',
    ':root[data-theme="dark"] .pe-root{--pe-bg:#171513;--pe-panel:#221f1c;--pe-line:#3f3a34;--pe-text:#ece6dc;--pe-muted:#b4aca0;--pe-btn:#332e29;--pe-acc:#3b5b8c;--pe-acc-text:#ffffff;--pe-warn:#f0b35c;--pe-stage:#2b2723}',
    '.pe-root{position:fixed;inset:0;z-index:40000;display:flex;flex-direction:column;background:var(--pe-bg);color:var(--pe-text);font:14px/1.35 system-ui,-apple-system,"Segoe UI",sans-serif}',
    '.pe-root *{box-sizing:border-box}',
    '.pe-root button,.pe-root select,.pe-root input{font:inherit;color:inherit}',
    '.pe-head{display:flex;flex-wrap:wrap;gap:6px;align-items:center;padding:8px 12px;border-bottom:1px solid var(--pe-line);background:var(--pe-panel)}',
    '.pe-head .pe-name{font-weight:700;margin-right:auto;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;max-width:50vw}',
    '.pe-btn{border:1px solid var(--pe-line);background:var(--pe-btn);border-radius:8px;min-height:40px;min-width:40px;padding:6px 10px;cursor:pointer;display:inline-flex;align-items:center;justify-content:center;gap:4px;touch-action:manipulation}',
    '.pe-btn[aria-pressed="true"]{background:var(--pe-acc);color:var(--pe-acc-text);border-color:var(--pe-acc)}',
    '.pe-btn:disabled{opacity:.45;cursor:default}',
    '.pe-btn:focus-visible{outline:3px solid var(--pe-acc);outline-offset:1px}',
    '.pe-body{flex:1;min-height:0;display:grid;grid-template-columns:auto 1fr 320px}',
    '.pe-tools{display:flex;flex-direction:column;gap:6px;padding:8px;border-right:1px solid var(--pe-line);background:var(--pe-panel);overflow:auto}',
    '.pe-tools .pe-btn{justify-content:flex-start;min-width:120px}',
    '.pe-tools .pe-glyph{display:inline-block;width:1.4em;text-align:center;font-size:16px}',
    '.pe-opt{display:flex;align-items:center;gap:6px;font-size:13px;color:var(--pe-muted);padding:2px 4px}',
    '.pe-stage{position:relative;min-width:0;min-height:0;background:var(--pe-stage);overflow:hidden}',
    '.pe-canvas{position:absolute;inset:0;width:100%;height:100%;touch-action:none;cursor:crosshair}',
    '.pe-status{position:absolute;left:8px;bottom:8px;right:8px;pointer-events:none;font:12px ui-monospace,monospace;color:var(--pe-text);background:var(--pe-panel);border:1px solid var(--pe-line);border-radius:6px;padding:4px 8px;width:max-content;max-width:calc(100% - 16px)}',
    '.pe-side{overflow:auto;border-left:1px solid var(--pe-line);background:var(--pe-panel);padding:10px;display:flex;flex-direction:column;gap:14px}',
    '.pe-side h3{margin:0 0 6px;font-size:13px;text-transform:uppercase;letter-spacing:.04em;color:var(--pe-muted)}',
    '.pe-row{display:flex;flex-wrap:wrap;gap:6px;align-items:center}',
    '.pe-sw{width:28px;height:28px;border-radius:4px;border:1px solid var(--pe-line);cursor:pointer;padding:0;background-image:linear-gradient(45deg,#bbb 25%,transparent 25%,transparent 75%,#bbb 75%),linear-gradient(45deg,#bbb 25%,#fff 25%,#fff 75%,#bbb 75%);background-size:8px 8px;background-position:0 0,4px 4px;position:relative}',
    '.pe-sw i{position:absolute;inset:0;border-radius:3px}',
    '.pe-sw.pe-big{width:44px;height:44px}',
    '.pe-pal{display:grid;grid-template-columns:repeat(auto-fill,minmax(26px,1fr));gap:4px}',
    '.pe-pal .pe-sw{width:100%;height:26px}',
    '.pe-layers{display:flex;flex-direction:column;gap:4px;margin:6px 0}',
    '.pe-layer{display:flex;align-items:center;gap:6px;border:1px solid var(--pe-line);border-radius:8px;padding:4px;cursor:pointer;background:var(--pe-bg)}',
    '.pe-layer[aria-selected="true"]{border-color:var(--pe-acc);box-shadow:inset 0 0 0 2px var(--pe-acc)}',
    '.pe-layer .pe-ln{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}',
    '.pe-layer canvas{width:32px;height:32px;image-rendering:pixelated;border:1px solid var(--pe-line);background:#fff}',
    '.pe-layer .pe-op{font-size:12px;color:var(--pe-muted)}',
    '.pe-layer .pe-btn{min-height:32px;min-width:32px;padding:2px 6px}',
    '.pe-prev{display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end}',
    '.pe-prev figure{margin:0;font-size:12px;color:var(--pe-muted)}',
    '.pe-prev canvas{display:block;image-rendering:pixelated;border:1px solid var(--pe-line);background:repeating-conic-gradient(#ccc 0 25%,#fff 0 50%) 0 0/8px 8px}',
    '.pe-ref img{max-width:100%;image-rendering:pixelated;border:1px solid var(--pe-line);cursor:crosshair}',
    '.pe-note{font-size:12px;color:var(--pe-muted);margin:4px 0 0}',
    '.pe-warn{font-size:13px;color:var(--pe-warn);font-weight:600}',
    '.pe-root input[type=text]{padding:6px 8px;min-height:36px;border-radius:6px;border:1px solid var(--pe-line);background:var(--pe-bg);width:9.5em}',
    '.pe-root input[type=range]{accent-color:var(--pe-acc)}',
    '.pe-root input[type=color]{width:44px;height:36px;padding:0;border:1px solid var(--pe-line);border-radius:6px;background:none}',
    'body.pe-open{overflow:hidden}',
    '@media (max-width:760px){.pe-root{overflow:auto}.pe-body{display:flex;flex-direction:column;flex:none}.pe-stage{height:56vh;flex:none}',
    '.pe-tools{flex-direction:row;overflow-x:auto;border-right:0;border-top:1px solid var(--pe-line);order:2}.pe-tools .pe-btn{min-width:44px}.pe-tools .pe-lbl{display:none}',
    '.pe-side{order:3;border-left:0;overflow:visible}.pe-head .pe-name{max-width:100%;flex-basis:100%}}'
  ].join('\n');
  function injectCss() {
    if (document.getElementById('pe-style')) return;
    var s = document.createElement('style'); s.id = 'pe-style'; s.textContent = CSS; document.head.appendChild(s);
  }

  /* ---------------- helpers */
  function h(tag, attrs, kids) {
    var e = document.createElement(tag);
    if (attrs) Object.keys(attrs).forEach(function (k) {
      if (k === 'text') e.textContent = attrs[k];
      else if (k.slice(0, 2) === 'on') e.addEventListener(k.slice(2), attrs[k]);
      else if (attrs[k] !== false && attrs[k] != null) e.setAttribute(k, attrs[k] === true ? '' : attrs[k]);
    });
    (kids || []).forEach(function (c) { if (c) e.appendChild(typeof c === 'string' ? document.createTextNode(c) : c); });
    return e;
  }
  function btn(label, title, on, extra) {
    var b = h('button', Object.assign({ type: 'button', class: 'pe-btn', title: title, 'aria-label': title, onclick: on }, extra || {}));
    b.innerHTML = label;
    return b;
  }
  function cssColor(c) { return 'rgba(' + c[0] + ',' + c[1] + ',' + c[2] + ',' + (c[3] / 255) + ')'; }
  function download(name, data, type) {
    var url = URL.createObjectURL(new Blob([data], { type: type }));
    var a = h('a', { href: url, download: name });
    document.body.appendChild(a); a.click(); a.remove();
    setTimeout(function () { URL.revokeObjectURL(url); }, 4000);
  }
  function fetchBytes(url) { return fetch(url).then(function (r) { if (!r.ok) throw new Error(r.status + ' ' + url); return r.arrayBuffer(); }).then(function (b) { return new Uint8Array(b); }); }
  /* exact decoder first; a canvas (premultiplied, may shift half-transparent pixels) only without DecompressionStream */
  function decodePng(bytes) {
    return C.pngDecode(bytes).catch(function (err) {
      if (typeof DecompressionStream !== 'undefined') throw err;
      return new Promise(function (ok, fail) {
        var img = new Image(), url = URL.createObjectURL(new Blob([bytes], { type: 'image/png' }));
        img.onload = function () {
          var c = document.createElement('canvas'); c.width = img.width; c.height = img.height;
          var g = c.getContext('2d'); g.drawImage(img, 0, 0);
          ok({ w: img.width, h: img.height, rgba: g.getImageData(0, 0, img.width, img.height).data });
          URL.revokeObjectURL(url);
        };
        img.onerror = function () { fail(err); };
        img.src = url;
      });
    });
  }

  /* ---------------- editor state */
  var S = null;
  var TOOLS = [['pencil', '✎', 'b'], ['eraser', '⌫', 'e'], ['fill', '▨', 'g'], ['picker', '⊙', 'i'], ['line', '╱', 'l'], ['rect', '▭', 'u'], ['select', '⬚', 'm'], ['move', '✥', 'v']];

  function open(opts) {
    if (!C) return Promise.reject(new Error('SBPixelCore missing'));
    if (S) close(true);
    injectCss();
    opts = opts || {};
    S = {
      opts: opts, model: null, active: 0, tool: 'pencil', primary: [30, 30, 30, 255], secondary: [255, 255, 255, 255],
      sym: { x: false, y: false }, grid: true, pp: false, filled: false, global: false,
      zoom: 16, ox: 0, oy: 0, sel: null, clip: null, history: new C.History(), dirty: false,
      pointers: new Map(), stroke: null, gesture: null, hover: null, alt: false, raf: 0, from: ''
    };
    buildUi();
    S.status.textContent = T('loading');
    var load = opts.model ? Promise.resolve({ model: opts.model, from: '' })
      : opts.layers ? fetch(opts.layers).then(function (r) { if (!r.ok) throw new Error(r.status + ' ' + opts.layers); return r.json(); })
        .then(function (doc) { return { model: C.fromDoc(doc), from: T('fromLayers') }; })
        : fetchBytes(opts.png).then(decodePng).then(function (img) { return { model: C.fromRgba(img.w, img.h, img.rgba, T('base')), from: T('fromPng') }; });
    return load.then(function (r) {
      if (!S) return;
      S.model = r.model; S.from = r.from; S.active = S.model.layers.length - 1;
      var colors = C.colorsOf(S.model);
      if (colors.length) S.primary = C.parseColor(colors[0]);
      fit(); refreshSide(); paint();
    }).catch(function (e) {
      if (S) S.status.textContent = T('loadFail') + (e && e.message || e);
    });
  }

  function buildUi() {
    var o = S.opts;
    S.root = h('div', { class: 'pe-root', role: 'dialog', 'aria-modal': 'true', 'aria-label': T('title') });
    var fileIn = h('input', { type: 'file', accept: '.png,.json,image/png,application/json', hidden: true, onchange: function () { openFile(fileIn.files[0]); fileIn.value = ''; } });
    S.btn = {};
    S.btn.undo = btn('↶', T('undo'), undo);
    S.btn.redo = btn('↷', T('redo'), redo);
    S.btn.grid = btn('▦', T('grid'), function () { S.grid = !S.grid; sync(); paint(); });
    S.btn.symX = btn('⇹', T('symX'), function () { S.sym.x = !S.sym.x; sync(); paint(); });
    S.btn.symY = btn('⇳', T('symY'), function () { S.sym.y = !S.sym.y; sync(); paint(); });
    S.btn.pp = btn('PP', T('pp'), function () { S.pp = !S.pp; sync(); });
    var head = h('header', { class: 'pe-head' }, [
      h('span', { class: 'pe-name', text: T('title') + ' · ' + (o.name || o.id || '') + (o.id && o.name ? ' (' + o.id + ')' : '') }),
      S.btn.undo, S.btn.redo, S.btn.grid, S.btn.symX, S.btn.symY, S.btn.pp,
      btn('📂', T('open'), function () { fileIn.click(); }), fileIn,
      btn('⤓ ' + T('both'), T('exp') + ': ' + T('both'), function () { exportPng().then(function () { setTimeout(exportJson, 400); }); }),
      btn('✕', T('close'), function () { close(false); })
    ]);
    var tools = h('nav', { class: 'pe-tools', 'aria-label': 'Tools' });
    S.btn.tools = {};
    TOOLS.forEach(function (t) {
      var b = btn('<span class="pe-glyph">' + t[1] + '</span><span class="pe-lbl">' + T(t[0]).replace(/ \(.*$/, '') + '</span>', T(t[0]), function () { setTool(t[0]); }, { 'data-tool': t[0] });
      S.btn.tools[t[0]] = b; tools.appendChild(b);
    });
    var filled = h('input', { type: 'checkbox', onchange: function () { S.filled = filled.checked; } });
    var global = h('input', { type: 'checkbox', onchange: function () { S.global = global.checked; } });
    tools.appendChild(h('label', { class: 'pe-opt' }, [filled, T('filled')]));
    tools.appendChild(h('label', { class: 'pe-opt' }, [global, T('global')]));
    S.canvas = h('canvas', { class: 'pe-canvas' });
    S.status = h('div', { class: 'pe-status', role: 'status' });
    var stage = h('div', { class: 'pe-stage' }, [S.canvas, S.status]);
    S.stage = stage;
    S.side = h('aside', { class: 'pe-side' });
    S.root.appendChild(head);
    S.root.appendChild(h('div', { class: 'pe-body' }, [tools, stage, S.side]));
    document.body.appendChild(S.root);
    document.body.classList.add('pe-open');
    S.ctx = S.canvas.getContext('2d');
    S.off = document.createElement('canvas');
    bindPointer();
    S.onKey = onKey; S.onKeyUp = function (e) { if (e.key === 'Alt') { S.alt = false; } };
    window.addEventListener('keydown', S.onKey, true);
    window.addEventListener('keyup', S.onKeyUp, true);
    S.ro = typeof ResizeObserver !== 'undefined' ? new ResizeObserver(function () { paint(); }) : null;
    if (S.ro) S.ro.observe(stage); else window.addEventListener('resize', paint);
    sync();
    S.btn.tools.pencil.focus();
  }

  function close(force) {
    if (!S) return;
    if (!force && S.dirty && !window.confirm(T('dirty'))) return;
    window.removeEventListener('keydown', S.onKey, true);
    window.removeEventListener('keyup', S.onKeyUp, true);
    if (S.ro) S.ro.disconnect(); else window.removeEventListener('resize', paint);
    S.root.remove();
    document.body.classList.remove('pe-open');
    S = null;
  }

  function sync() {
    if (!S) return;
    Object.keys(S.btn.tools).forEach(function (k) { S.btn.tools[k].setAttribute('aria-pressed', String(S.tool === k)); });
    S.btn.grid.setAttribute('aria-pressed', String(S.grid));
    S.btn.symX.setAttribute('aria-pressed', String(S.sym.x));
    S.btn.symY.setAttribute('aria-pressed', String(S.sym.y));
    S.btn.pp.setAttribute('aria-pressed', String(S.pp));
    S.btn.undo.disabled = !S.history.past.length;
    S.btn.redo.disabled = !S.history.future.length;
  }
  function setTool(t) { S.tool = t; sync(); }
  function layer() { return S.model && S.model.layers[S.active]; }

  /* ---------------- history */
  function record(before) { S.history.push(before || S.model); S.dirty = true; sync(); }
  function changed(before) { record(before); refreshSide(); paint(); }
  function undo() { var m = S.history.undo(S.model); if (!m) return; S.model = m; S.active = Math.min(S.active, m.layers.length - 1); S.dirty = true; sync(); refreshSide(); paint(); }
  function redo() { var m = S.history.redo(S.model); if (!m) return; S.model = m; S.active = Math.min(S.active, m.layers.length - 1); S.dirty = true; sync(); refreshSide(); paint(); }

  /* ---------------- view */
  function fit() {
    var r = S.stage.getBoundingClientRect(), m = S.model;
    if (!m) return;
    S.zoom = Math.max(1, Math.min(64, Math.floor(Math.min((r.width - 24) / m.w, (r.height - 48) / m.h)) || 1));
    S.ox = Math.round((r.width - m.w * S.zoom) / 2); S.oy = Math.round((r.height - m.h * S.zoom) / 2);
  }
  function zoomAt(z, sx, sy) {
    z = Math.max(1, Math.min(64, Math.round(z)));
    if (z === S.zoom) return;
    S.ox = sx - (sx - S.ox) * z / S.zoom; S.oy = sy - (sy - S.oy) * z / S.zoom; S.zoom = z;
    paint();
  }
  function paint() { if (S && !S.raf) S.raf = requestAnimationFrame(draw); }
  function draw() {
    if (!S) return;
    S.raf = 0;
    var cv = S.canvas, dpr = window.devicePixelRatio || 1, r = S.stage.getBoundingClientRect();
    var W = Math.max(1, Math.round(r.width * dpr)), H = Math.max(1, Math.round(r.height * dpr));
    if (cv.width !== W || cv.height !== H) { cv.width = W; cv.height = H; }
    var g = S.ctx, m = S.model;
    g.setTransform(dpr, 0, 0, dpr, 0, 0);
    g.clearRect(0, 0, r.width, r.height);
    if (!m) return;
    var z = S.zoom, x0 = S.ox, y0 = S.oy, cw = m.w * z, ch = m.h * z;
    // checkerboard behind the texture
    var cell = Math.max(4, Math.min(16, z / 2));
    g.fillStyle = '#ffffff'; g.fillRect(x0, y0, cw, ch);
    g.fillStyle = '#cfcfcf';
    g.save(); g.beginPath(); g.rect(x0, y0, cw, ch); g.clip();
    for (var cy = 0; cy * cell < ch; cy++) for (var cx = cy % 2; cx * cell < cw; cx += 2) g.fillRect(x0 + cx * cell, y0 + cy * cell, cell, cell);
    g.restore();
    var comp = C.compose(m);
    if (S.off.width !== m.w || S.off.height !== m.h) { S.off.width = m.w; S.off.height = m.h; }
    S.off.getContext('2d').putImageData(new ImageData(comp, m.w, m.h), 0, 0);
    g.imageSmoothingEnabled = false;
    g.drawImage(S.off, x0, y0, cw, ch);
    if (S.grid && z >= 6) {
      g.beginPath();
      for (var gx = 0; gx <= m.w; gx++) { g.moveTo(x0 + gx * z + .5, y0); g.lineTo(x0 + gx * z + .5, y0 + ch); }
      for (var gy = 0; gy <= m.h; gy++) { g.moveTo(x0, y0 + gy * z + .5); g.lineTo(x0 + cw, y0 + gy * z + .5); }
      g.strokeStyle = 'rgba(0,0,0,.18)'; g.lineWidth = 1; g.stroke();
      if (m.w > 16 || m.h > 16) {
        g.beginPath();
        for (gx = 16; gx < m.w; gx += 16) { g.moveTo(x0 + gx * z + .5, y0); g.lineTo(x0 + gx * z + .5, y0 + ch); }
        for (gy = 16; gy < m.h; gy += 16) { g.moveTo(x0, y0 + gy * z + .5); g.lineTo(x0 + cw, y0 + gy * z + .5); }
        g.strokeStyle = 'rgba(47,90,160,.6)'; g.stroke();
      }
    }
    g.strokeStyle = 'rgba(0,0,0,.55)'; g.lineWidth = 1; g.strokeRect(x0 - .5, y0 - .5, cw + 1, ch + 1);
    if (S.sym.x || S.sym.y) {
      g.save(); g.setLineDash([6, 4]); g.strokeStyle = '#d0342c'; g.lineWidth = 2; g.beginPath();
      if (S.sym.x) { g.moveTo(x0 + cw / 2, y0 - 8); g.lineTo(x0 + cw / 2, y0 + ch + 8); }
      if (S.sym.y) { g.moveTo(x0 - 8, y0 + ch / 2); g.lineTo(x0 + cw + 8, y0 + ch / 2); }
      g.stroke(); g.restore();
    }
    if (S.sel) {
      var s = S.sel;
      g.save(); g.lineWidth = 1; g.setLineDash([4, 4]);
      g.strokeStyle = '#000'; g.strokeRect(x0 + s.x * z + .5, y0 + s.y * z + .5, s.w * z - 1, s.h * z - 1);
      g.lineDashOffset = 4; g.strokeStyle = '#fff'; g.strokeRect(x0 + s.x * z + .5, y0 + s.y * z + .5, s.w * z - 1, s.h * z - 1);
      g.restore();
    }
    if (S.hover && S.hover[0] >= 0 && S.hover[1] >= 0 && S.hover[0] < m.w && S.hover[1] < m.h) {
      g.strokeStyle = 'rgba(47,90,160,.9)'; g.lineWidth = 2;
      g.strokeRect(x0 + S.hover[0] * z + 1, y0 + S.hover[1] * z + 1, z - 2, z - 2);
    }
    status(comp);
    previews(comp);
  }
  function status(comp) {
    var m = S.model, l = layer(), p = S.hover, txt = m.w + '×' + m.h + ' · ' + S.zoom + '× · ' + (l ? l.name : '');
    if (p && p[0] >= 0 && p[1] >= 0 && p[0] < m.w && p[1] < m.h) {
      var c = C.pixelAt(comp, m.w, p[0], p[1]);
      txt = p[0] + ',' + p[1] + ' · ' + C.colorHex(c[0], c[1], c[2], c[3]) + ' · ' + txt;
    }
    if (l && !l.visible) txt += ' · ' + T('hidden');
    S.status.textContent = txt;
  }
  function previews(comp) {
    var m = S.model;
    (S.prevs || []).forEach(function (p) {
      var c = p.canvas;
      var tw = p.tile ? 3 : 1;
      if (c.width !== m.w * tw || c.height !== m.h * tw) { c.width = m.w * tw; c.height = m.h * tw; }
      var g = c.getContext('2d'), img = new ImageData(new Uint8ClampedArray(comp), m.w, m.h);
      g.clearRect(0, 0, c.width, c.height);
      for (var ty = 0; ty < tw; ty++) for (var tx = 0; tx < tw; tx++) g.putImageData(img, tx * m.w, ty * m.h);
      var big = Math.min(4, Math.max(1, Math.floor(192 / Math.max(m.w * tw, m.h * tw))));
      var scale = p.scale === 'auto' ? big : p.scale;
      c.style.width = (c.width * scale) + 'px'; c.style.height = (c.height * scale) + 'px';
    });
    (S.thumbs || []).forEach(function (t) {
      var l = S.model.layers[t.n];
      if (!l) return;
      if (t.canvas.width !== m.w || t.canvas.height !== m.h) { t.canvas.width = m.w; t.canvas.height = m.h; }
      t.canvas.getContext('2d').putImageData(new ImageData(new Uint8ClampedArray(l.px), m.w, m.h), 0, 0);
    });
  }

  /* ---------------- side panel */
  function swatch(c, cls, onPick, onSecond) {
    var b = h('button', { type: 'button', class: 'pe-sw' + (cls ? ' ' + cls : ''), title: C.colorHex(c[0], c[1], c[2], c[3]), 'aria-label': C.colorHex(c[0], c[1], c[2], c[3]) });
    b.appendChild(h('i', { style: 'background:' + cssColor(c) }));
    if (onPick) b.addEventListener('click', onPick);
    if (onSecond) b.addEventListener('contextmenu', function (e) { e.preventDefault(); onSecond(); });
    return b;
  }
  function setColor(c, second) { if (second) S.secondary = c.slice(); else S.primary = c.slice(); refreshColors(); }
  function refreshColors() {
    if (!S.colorBox) return;
    var box = S.colorBox; box.innerHTML = '';
    var p = S.primary, hex = C.colorHex(p[0], p[1], p[2], p[3]);
    var picker = h('input', { type: 'color', value: hex.slice(0, 7), 'aria-label': T('colors'), oninput: function () { var c = C.parseColor(picker.value); c[3] = S.primary[3] || 255; S.primary = c; refreshColors(); } });
    var text = h('input', { type: 'text', value: hex, 'aria-label': 'Hex', spellcheck: 'false', onchange: function () { var c = C.parseColor(text.value); if (c) setColor(c); else text.value = hex; } });
    var alpha = h('input', { type: 'range', min: 0, max: 255, value: p[3], 'aria-label': T('alpha'), oninput: function () { S.primary[3] = Number(alpha.value); text.value = C.colorHex(S.primary[0], S.primary[1], S.primary[2], S.primary[3]); prim.firstChild.style.background = cssColor(S.primary); } });
    var prim = swatch(p, 'pe-big'), sec = swatch(S.secondary, '', function () { swap(); });
    box.appendChild(h('div', { class: 'pe-row' }, [prim, sec, btn('⇄', T('swap'), swap), picker, text]));
    box.appendChild(h('label', { class: 'pe-row pe-note' }, [T('alpha') + ' ', alpha]));
  }
  function swap() { var t = S.primary; S.primary = S.secondary; S.secondary = t; refreshColors(); }
  function palette(list) {
    var g = h('div', { class: 'pe-pal' });
    list.forEach(function (hex) { var c = C.parseColor(hex); g.appendChild(swatch(c, '', function () { setColor(c); }, function () { setColor(c, true); })); });
    return g;
  }
  function refreshSide() {
    if (!S || !S.model) return;
    var side = S.side, m = S.model, o = S.opts;
    side.innerHTML = '';
    // colours and palettes
    S.colorBox = h('div');
    side.appendChild(h('section', null, [h('h3', { text: T('colors') }), S.colorBox]));
    refreshColors();
    var own = C.colorsOf(m);
    side.appendChild(h('section', null, [h('h3', { text: T('palTex') + ' (' + own.length + ')' }), palette(own.slice(0, 256)),
      h('div', { class: 'pe-row', style: 'margin-top:6px' }, [btn(T('gpl'), T('gpl'), function () { download(baseName() + '.gpl', C.gpl(baseName(), own), 'text/plain'); })])]));
    side.appendChild(h('section', null, [h('h3', { text: T('palVan') }), palette(C.VANILLA_NEAR)]));
    // layers (top first, like Resprite/Aseprite)
    var list = h('div', { class: 'pe-layers', role: 'listbox', 'aria-label': T('layers') });
    S.thumbs = [];
    for (var n = m.layers.length - 1; n >= 0; n--) (function (n) {
      var l = m.layers[n], th = h('canvas');
      S.thumbs.push({ n: n, canvas: th });
      var eye = btn(l.visible ? '◉' : '○', T('show'), function (e) { e.stopPropagation(); var b = C.cloneModel(m); l.visible = !l.visible; changed(b); }, { 'aria-pressed': String(l.visible) });
      var row = h('div', { class: 'pe-layer', role: 'option', 'aria-selected': String(n === S.active), tabindex: 0,
        onclick: function () { S.active = n; refreshSide(); paint(); },
        ondblclick: function () { renameLayer(n); } },
        [eye, th, h('span', { class: 'pe-ln', text: l.name + (l.alphaLock ? ' 🔒' : '') }), h('span', { class: 'pe-op', text: l.opacity + '%' })]);
      list.appendChild(row);
    })(n);
    var l = layer();
    var op = h('input', { type: 'range', min: 0, max: 100, value: l.opacity, 'aria-label': T('opacity') });
    var opBefore = null;
    op.addEventListener('pointerdown', function () { opBefore = C.cloneModel(m); });
    op.addEventListener('input', function () { if (!opBefore) opBefore = C.cloneModel(m); l.opacity = Number(op.value); opLabel.textContent = l.opacity + '%'; paint(); });
    op.addEventListener('change', function () { changed(opBefore); opBefore = null; });
    var opLabel = h('span', { class: 'pe-op', text: l.opacity + '%' });
    side.appendChild(h('section', null, [h('h3', { text: T('layers') }), list,
      h('label', { class: 'pe-row pe-note' }, [T('opacity') + ' ', op, opLabel]),
      h('div', { class: 'pe-row' }, [
        btn('＋', T('add'), addLayer), btn('⧉', T('dup'), dupLayer), btn('🗑', T('del'), delLayer, { disabled: m.layers.length < 2 }),
        btn('↑', T('up'), function () { moveLayer(1); }, { disabled: S.active >= m.layers.length - 1 }),
        btn('↓', T('down'), function () { moveLayer(-1); }, { disabled: S.active <= 0 }),
        btn('⤓', T('merge'), mergeLayer, { disabled: S.active <= 0 }),
        btn('✎', T('rename'), function () { renameLayer(S.active); }),
        btn('🔒', T('lock'), function () { l.alphaLock = !l.alphaLock; refreshSide(); }, { 'aria-pressed': String(!!l.alphaLock) })])]));
    // region transforms
    side.appendChild(h('section', null, [h('h3', { text: T('region') }), h('div', { class: 'pe-row' }, [
      btn('↔', T('flipH'), function () { transform(function (b) { return C.flipBuf(b, true); }); }),
      btn('↕', T('flipV'), function () { transform(function (b) { return C.flipBuf(b, false); }); }),
      btn('⟳', T('rot'), function () { transform(C.rotateBuf); }),
      btn('⌧', T('clear'), clearSel), btn('⎘', T('copy'), copySel), btn('📋', T('paste'), paste), btn('⊘', T('deselect'), function () { S.sel = null; paint(); })])]));
    // previews
    S.prevs = [{ canvas: h('canvas'), scale: 1 }, { canvas: h('canvas'), scale: 2 }, { canvas: h('canvas'), scale: 'auto', tile: true }];
    side.appendChild(h('section', null, [h('h3', { text: T('preview') }), h('div', { class: 'pe-prev' }, [
      h('figure', null, [S.prevs[0].canvas, h('figcaption', { text: T('p1') })]),
      h('figure', null, [S.prevs[1].canvas, h('figcaption', { text: T('p2') })]),
      h('figure', null, [S.prevs[2].canvas, h('figcaption', { text: T('tile') })])])]));
    // reference image (kept only in this page)
    var refIn = h('input', { type: 'file', accept: 'image/*', 'aria-label': T('ref'), onchange: function () { var f = refIn.files[0]; if (f) { S.ref = URL.createObjectURL(f); refreshSide(); } } });
    var refBox = h('div', { class: 'pe-ref' }, [refIn]);
    if (S.ref) {
      var img = h('img', { src: S.ref, alt: T('ref') });
      img.addEventListener('click', function (e) {
        var c = document.createElement('canvas'); c.width = img.naturalWidth; c.height = img.naturalHeight;
        var g = c.getContext('2d'); g.drawImage(img, 0, 0);
        var rr = img.getBoundingClientRect(), x = Math.floor((e.clientX - rr.left) * img.naturalWidth / rr.width), y = Math.floor((e.clientY - rr.top) * img.naturalHeight / rr.height);
        var d = g.getImageData(x, y, 1, 1).data; setColor([d[0], d[1], d[2], d[3]]);
      });
      refBox.appendChild(img);
      refBox.appendChild(h('p', { class: 'pe-note', text: T('refPick') }));
    }
    side.appendChild(h('section', null, [h('h3', { text: T('ref') }), refBox]));
    // export
    var src = o.source || '';
    side.appendChild(h('section', null, [h('h3', { text: T('exp') }),
      h('div', { class: 'pe-row' }, [btn(T('png'), T('png'), exportPng), btn(T('json'), T('json'), exportJson)]),
      /^build\/vanilla-derived\//.test(src) ? h('p', { class: 'pe-warn', text: T('derived') }) : null,
      h('p', { class: 'pe-note', text: T('howto', { png: baseName() + '.png', json: baseName() + C.SUFFIX, src: src || '?' }) }),
      S.from ? h('p', { class: 'pe-note', text: S.from }) : null,
      h('p', { class: 'pe-note', text: T('keys') })]));
    paint();
  }

  /* ---------------- layer operations */
  function addLayer() { var b = C.cloneModel(S.model); S.model.layers.splice(S.active + 1, 0, C.newLayer(S.model.w, S.model.h, T('layer') + ' ' + (S.model.layers.length + 1))); S.active++; changed(b); }
  function dupLayer() { var b = C.cloneModel(S.model), l = layer(), c = C.cloneModel({ w: 0, h: 0, layers: [l] }).layers[0]; c.name = l.name + ' 2'; S.model.layers.splice(S.active + 1, 0, c); S.active++; changed(b); }
  function delLayer() { if (S.model.layers.length < 2) return; var b = C.cloneModel(S.model); S.model.layers.splice(S.active, 1); S.active = Math.max(0, S.active - 1); changed(b); }
  function moveLayer(d) {
    var n = S.active, k = n + d, ls = S.model.layers;
    if (k < 0 || k >= ls.length) return;
    var b = C.cloneModel(S.model), t = ls[n]; ls[n] = ls[k]; ls[k] = t; S.active = k; changed(b);
  }
  function mergeLayer() { var b = C.cloneModel(S.model); if (C.mergeDown(S.model, S.active)) { S.active--; changed(b); } }
  function renameLayer(n) {
    var l = S.model.layers[n], name = window.prompt(T('layerName'), l.name);
    if (name == null || !name.trim() || name === l.name) return;
    var b = C.cloneModel(S.model); l.name = name.trim(); changed(b);
  }
  function region() { return S.sel ? C.clipRect(S.sel, S.model.w, S.model.h) : { x: 0, y: 0, w: S.model.w, h: S.model.h }; }
  function transform(fn) { var r = region(), b = C.cloneModel(S.model); if (r && C.transformRect(layer().px, S.model.w, S.model.h, r, fn)) changed(b); }
  function clearSel() { var r = region(), b = C.cloneModel(S.model); if (r) { C.clearRect(layer().px, S.model.w, r); changed(b); } }
  function copySel() { var r = region(); if (r) S.clip = Object.assign(C.copyRect(layer().px, S.model.w, r), { x: r.x, y: r.y }); }
  function paste() {
    if (!S.clip) return;
    var b = C.cloneModel(S.model), m = S.model, l = C.newLayer(m.w, m.h, T('pasted'));
    var x = Math.min(S.clip.x, m.w - 1), y = Math.min(S.clip.y, m.h - 1);
    C.pasteBuf(l.px, m.w, m.h, S.clip, x, y);
    m.layers.splice(S.active + 1, 0, l); S.active++;
    S.sel = { x: x, y: y, w: S.clip.w, h: S.clip.h };
    setTool('move'); changed(b);
  }
  function nudge(dx, dy) {
    var r = region(); if (!r) return;
    var b = C.cloneModel(S.model), px = layer().px, buf = C.copyRect(px, S.model.w, r);
    C.clearRect(px, S.model.w, r); C.pasteBuf(px, S.model.w, S.model.h, buf, r.x + dx, r.y + dy);
    if (S.sel) S.sel = { x: S.sel.x + dx, y: S.sel.y + dy, w: S.sel.w, h: S.sel.h };
    changed(b);
  }

  /* ---------------- files */
  function baseName() { var id = S.opts.id || S.opts.name || 'texture'; return String(id).split(/[:/]/).pop().replace(/[^A-Za-z0-9_.-]+/g, '_') || 'texture'; }
  function exportPng() {
    var m = S.model;
    return C.pngEncode(m.w, m.h, C.compose(m)).then(function (bytes) { download(baseName() + '.png', bytes, 'image/png'); S.dirty = false; });
  }
  function exportJson() { var doc = C.toDoc(S.model); S.model.palette = doc.palette; download(baseName() + C.SUFFIX, C.dumps(doc), 'application/json'); }
  function openFile(f) {
    if (!f) return;
    f.arrayBuffer().then(function (buf) {
      var bytes = new Uint8Array(buf);
      if (/\.json$/i.test(f.name)) return C.fromDoc(JSON.parse(new TextDecoder().decode(bytes)));
      return decodePng(bytes).then(function (img) { return C.fromRgba(img.w, img.h, img.rgba, T('base')); });
    }).then(function (model) {
      record(); S.model = model; S.active = model.layers.length - 1; S.sel = null; fit(); refreshSide(); paint();
    }).catch(function (e) { window.alert(T('loadFail') + (e && e.message || e)); });
  }

  /* ---------------- pointer input */
  function toPixel(e) {
    var r = S.canvas.getBoundingClientRect();
    return [Math.floor((e.clientX - r.left - S.ox) / S.zoom), Math.floor((e.clientY - r.top - S.oy) / S.zoom)];
  }
  function stagePoint(e) { var r = S.canvas.getBoundingClientRect(); return [e.clientX - r.left, e.clientY - r.top]; }
  function pick(p) {
    var m = S.model;
    if (p[0] < 0 || p[1] < 0 || p[0] >= m.w || p[1] >= m.h) return;
    setColor(C.pixelAt(C.compose(m), m.w, p[0], p[1]));
  }
  function bindPointer() {
    var cv = S.canvas;
    cv.addEventListener('contextmenu', function (e) { e.preventDefault(); });
    cv.addEventListener('wheel', function (e) {
      e.preventDefault();
      var p = stagePoint(e);
      zoomAt(S.zoom + (e.deltaY < 0 ? 1 : -1) * Math.max(1, Math.round(S.zoom / 5)), p[0], p[1]);
    }, { passive: false });
    cv.addEventListener('pointerdown', function (e) {
      if (!S.model) return;
      try { cv.setPointerCapture(e.pointerId); } catch (err) { /* synthetic or already released pointer */ }
      var sp = stagePoint(e);
      S.pointers.set(e.pointerId, { x: sp[0], y: sp[1], sx: sp[0], sy: sp[1] });
      if (S.pointers.size >= 2) { startGesture(); return; }
      if (e.button === 1 || S.space) { S.pan = { x: sp[0], y: sp[1], ox: S.ox, oy: S.oy }; return; }
      S.tapStart = Date.now();
      beginStroke(toPixel(e), e);
    });
    cv.addEventListener('pointermove', function (e) {
      if (!S.model) return;
      var sp = stagePoint(e), pt = S.pointers.get(e.pointerId);
      S.hover = toPixel(e);
      if (pt) { pt.x = sp[0]; pt.y = sp[1]; }
      if (S.gesture) { moveGesture(); return; }
      if (S.pan) { S.ox = S.pan.ox + sp[0] - S.pan.x; S.oy = S.pan.oy + sp[1] - S.pan.y; paint(); return; }
      if (S.stroke) moveStroke(S.hover, e);
      paint();
    });
    function up(e) {
      if (!S) return;
      S.pointers.delete(e.pointerId);
      if (S.gesture) {
        if (!S.pointers.size) {
          var gst = S.gesture; S.gesture = null;
          if (Date.now() - gst.t0 < 300 && gst.moved < 12) { if (gst.fingers === 2) undo(); else if (gst.fingers >= 3) redo(); }
        }
        return;
      }
      if (S.pan) { S.pan = null; return; }
      if (S.stroke) endStroke();
    }
    cv.addEventListener('pointerup', up);
    cv.addEventListener('pointercancel', function (e) { if (S && S.stroke) cancelStroke(); up(e); });
    cv.addEventListener('pointerleave', function () { if (S) { S.hover = null; paint(); } });
  }
  function startGesture() {
    if (S.stroke) cancelStroke();
    var pts = Array.from(S.pointers.values()), a = pts[0], b = pts[1];
    if (S.gesture) { S.gesture.fingers = Math.max(S.gesture.fingers, pts.length); return; }
    S.gesture = { t0: Date.now(), fingers: pts.length, moved: 0, d: Math.hypot(a.x - b.x, a.y - b.y) || 1, mx: (a.x + b.x) / 2, my: (a.y + b.y) / 2, zoom: S.zoom, ox: S.ox, oy: S.oy };
  }
  function moveGesture() {
    var g = S.gesture, pts = Array.from(S.pointers.values());
    if (pts.length < 2) return;
    var a = pts[0], b = pts[1], d = Math.hypot(a.x - b.x, a.y - b.y), mx = (a.x + b.x) / 2, my = (a.y + b.y) / 2;
    g.moved = Math.max(g.moved, Math.abs(d - g.d), Math.hypot(mx - g.mx, my - g.my));
    var z = Math.max(1, Math.min(64, Math.round(g.zoom * d / g.d)));
    S.zoom = z;
    S.ox = mx - (g.mx - g.ox) * z / g.zoom; S.oy = my - (g.my - g.oy) * z / g.zoom;
    paint();
  }

  function effTool(e) { return (e && e.altKey) || S.alt ? 'picker' : S.tool; }
  function colorFor(e) { return e && e.button === 2 ? S.secondary : S.primary; }
  function beginStroke(p, e) {
    var l = layer(); if (!l) return;
    var tool = effTool(e), m = S.model;
    S.stroke = { tool: tool, start: p, last: p, path: [p], before: C.cloneModel(m), base: new Uint8ClampedArray(l.px), color: colorFor(e), changed: false, global: S.global || (e && e.shiftKey) };
    if (tool === 'picker') { pick(p); return; }
    if (tool === 'fill') {
      var pts = C.fill(l.px, m.w, m.h, p[0], p[1], S.stroke.color, S.stroke.global, l.alphaLock);
      S.stroke.changed = pts.length > 0; paint(); return;
    }
    if (tool === 'select') { S.sel = { x: p[0], y: p[1], w: 1, h: 1 }; paint(); return; }
    if (tool === 'move') {
      var r = region(); S.stroke.region = r;
      S.stroke.buf = r ? C.copyRect(l.px, m.w, r) : null; S.stroke.sel = S.sel; return;
    }
    if (tool === 'pencil' && e && e.pointerType !== 'mouse') {
      S.stroke.long = setTimeout(function () {
        var st = S && S.stroke;
        if (!st || st.path.length > 1) return;
        layer().px.set(st.base); pick(st.start); S.stroke = null; paint();
      }, 450);
    }
    shape(p);
  }
  function moveStroke(p, e) {
    var st = S.stroke, l = layer(), m = S.model;
    if (!st || (p[0] === st.last[0] && p[1] === st.last[1])) return;
    if (st.long) { clearTimeout(st.long); st.long = 0; }
    if (st.tool === 'picker') { pick(p); st.last = p; return; }
    if (st.tool === 'fill') return;
    if (st.tool === 'select') {
      var x0 = Math.max(0, Math.min(st.start[0], p[0])), y0 = Math.max(0, Math.min(st.start[1], p[1]));
      var x1 = Math.min(m.w - 1, Math.max(st.start[0], p[0])), y1 = Math.min(m.h - 1, Math.max(st.start[1], p[1]));
      S.sel = x1 >= x0 && y1 >= y0 ? { x: x0, y: y0, w: x1 - x0 + 1, h: y1 - y0 + 1 } : null;
      st.last = p; return;
    }
    if (st.tool === 'move') {
      if (!st.region) return;
      var dx = p[0] - st.start[0], dy = p[1] - st.start[1];
      l.px.set(st.base); C.clearRect(l.px, m.w, st.region); C.pasteBuf(l.px, m.w, m.h, st.buf, st.region.x + dx, st.region.y + dy);
      if (st.sel) S.sel = { x: st.sel.x + dx, y: st.sel.y + dy, w: st.sel.w, h: st.sel.h };
      st.changed = dx !== 0 || dy !== 0; st.last = p; return;
    }
    if (st.tool === 'pencil' || st.tool === 'eraser') st.path = st.path.concat(C.linePoints(st.last[0], st.last[1], p[0], p[1]).slice(1));
    st.last = p;
    shape(p);
  }
  /* (re)draw the current stroke from the layer as it was at its start */
  function shape(p) {
    var st = S.stroke, l = layer(), m = S.model, pts;
    if (st.tool === 'pencil' || st.tool === 'eraser') pts = S.pp ? C.pixelPerfect(st.path) : st.path;
    else if (st.tool === 'line') pts = C.linePoints(st.start[0], st.start[1], p[0], p[1]);
    else if (st.tool === 'rect') pts = C.rectPoints(st.start[0], st.start[1], p[0], p[1], S.filled);
    else return;
    l.px.set(st.base);
    C.plot(l.px, m.w, m.h, C.mirror(pts, m.w, m.h, S.sym), st.tool === 'eraser' ? null : st.color, l.alphaLock);
    st.changed = true;
    paint();
  }
  function endStroke() {
    var st = S.stroke; S.stroke = null;
    if (st.long) clearTimeout(st.long);
    if (st.changed) changed(st.before);
  }
  function cancelStroke() {
    var st = S.stroke; S.stroke = null;
    if (st.long) clearTimeout(st.long);
    var l = layer(); if (l && st.base) l.px.set(st.base);
    if (st.sel !== undefined) S.sel = st.sel;
    paint();
  }

  /* ---------------- keyboard (capture: the wiki's lightbox must not see the keys) */
  function onKey(e) {
    if (!S) return;
    var t = e.target, typing = t && (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA' || t.tagName === 'SELECT') && t.type !== 'range' && t.type !== 'checkbox';
    e.stopPropagation();
    if (e.key === 'Escape') { e.preventDefault(); if (S.sel) { S.sel = null; paint(); } else close(false); return; }
    if (typing || !S.model) return;
    var k = e.key.toLowerCase(), mod = e.ctrlKey || e.metaKey;
    if (e.key === 'Alt') { S.alt = true; e.preventDefault(); return; }
    if (e.key === ' ') { if (S.space) { e.preventDefault(); return; } S.space = true; e.preventDefault(); window.addEventListener('keyup', function sp(ev) { if (ev.key === ' ') { if (S) S.space = false; window.removeEventListener('keyup', sp, true); } }, true); return; }
    if (mod && k === 'z') { e.preventDefault(); if (e.shiftKey) redo(); else undo(); return; }
    if (mod && k === 'y') { e.preventDefault(); redo(); return; }
    if (mod && k === 'c') { e.preventDefault(); copySel(); return; }
    if (mod && k === 'v') { e.preventDefault(); paste(); return; }
    if (mod && k === 'd') { e.preventDefault(); S.sel = null; paint(); return; }
    if (mod && k === 'a') { e.preventDefault(); S.sel = { x: 0, y: 0, w: S.model.w, h: S.model.h }; paint(); return; }
    if (mod && (e.key === "'" || e.code === 'Backslash' || e.code === 'Quote')) { e.preventDefault(); S.grid = !S.grid; sync(); paint(); return; }
    if (mod) return;
    if (e.shiftKey && k === 'n') { e.preventDefault(); addLayer(); return; }
    var tool = TOOLS.filter(function (x) { return x[2] === k; })[0];
    if (tool && !e.shiftKey) { e.preventDefault(); setTool(tool[0]); return; }
    if (k === 'x') { e.preventDefault(); swap(); return; }
    if (e.key === '+' || e.key === '=') { e.preventDefault(); var r = S.stage.getBoundingClientRect(); zoomAt(S.zoom + Math.max(1, S.zoom >> 2), r.width / 2, r.height / 2); return; }
    if (e.key === '-') { e.preventDefault(); var r2 = S.stage.getBoundingClientRect(); zoomAt(S.zoom - Math.max(1, S.zoom >> 2), r2.width / 2, r2.height / 2); return; }
    if (e.key === '0') { e.preventDefault(); fit(); paint(); return; }
    if (e.key === 'Delete' || e.key === 'Backspace') { e.preventDefault(); clearSel(); return; }
    var arrows = { ArrowLeft: [-1, 0], ArrowRight: [1, 0], ArrowUp: [0, -1], ArrowDown: [0, 1] }[e.key];
    if (arrows) {
      e.preventDefault();
      if (e.shiftKey) { S.active = Math.max(0, Math.min(S.model.layers.length - 1, S.active + (arrows[1] < 0 || arrows[0] > 0 ? 1 : -1))); refreshSide(); }
      else nudge(arrows[0], arrows[1]);
    }
  }

  window.SBPixelEditor = { open: open, close: function () { close(true); }, state: function () { return S; } };
})();
