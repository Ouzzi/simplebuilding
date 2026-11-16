/* Testzentrale-Editor (dev page ?tab=tzeditor / #/tzeditor, N50 TZEDIT50, plan docs/ai/PLAN-B-TZEDIT50-2026-11-14.md).
   Edits the layout configs of the test centre order areas (first export from the game: all areas of both variants,
   data/testcentre/index.json + layout_<variant>_<area>.json, written by tools/tzedit/build_editor.py).
   Shown only on the dev site (index.html: isDevSite, dev sidebar group). Styles use the wiki tokens (--panel, --border,
   --slot-bg ...); the placeholder colours are the in-game blocks (grey line, light grey "none", red "missing").
   Save = POST /api/testcentre per area (tools/tzedit/tzedit_save.py check_save), JSON download always works. */
(function (root) {
  'use strict';
  var STR = {
    en: { title: 'Test centre editor', intro: 'The order areas of the test centre as config: one file per area, exported from the game (all modules). Drag cells, rows and columns; Ctrl-click selects several; Delete empties.',
      area: 'Area', view: 'View', viewA: 'A grid + side panel', viewB: 'B full screen + floating bar', undo: 'Undo', redo: 'Redo',
      grey: 'grey', light: 'light grey', red: 'red', frame: 'frame', clear: 'empty', zoom: 'Zoom', search: 'Search', searchPh: 'id/name',
      save: 'Save', saveTip: 'Stores a save on the dev host; picked up as branch tz-layout-*, goes into the next bundle', download: 'JSON download',
      check: 'Check (live)', fresh: 'Area "New" (not placed)', unknownTest: 'Try an unknown id', setCell: 'set as cell', saved: 'Saved (latest)', none: 'none yet',
      dups: 'Duplicates', missing: 'Missing (not placed)', unknown: 'Unknown', reds: 'Red fields', loading: 'Loading the test centre layout …',
      noData: 'data/testcentre/index.json is missing (python3 tools/tzedit/build_editor.py).', areaFail: 'Area cannot be loaded: ', pick: ' select a cell first',
      size: '{0} rows x {1} columns, {2} ids', discard: 'Discard unsaved changes?', commentQ: 'Short: what changed? (optional)', saving: ' saving …',
      okSave: ' saved (no. {0}) - comes as branch tz-layout-* into the next bundle', badSave: ' not saved: ', login: ' (logged in?)', offline: ' not saved: server unreachable (JSON download always works)',
      downloaded: ' downloaded', mod: 'Mod', row: 'Row', column: 'Column', top: 'Octet on top', isNew: 'New', devOnly: 'This page exists only on the dev site.',
      legend: 'Small picture top right = octet standing on the block. Light grey = placeholder "does not exist", red = missing, empty = no field. Vanilla pictures only on the dev host, found through the block models (data/vanilla-icons.json); without them the name.' },
    de: { title: 'Testzentrale-Editor', intro: 'Die geordneten Bereiche der Testzentrale als Config: eine Datei je Bereich, aus dem Spiel exportiert (alle Module). Zellen, Reihen und Spalten ziehen; Strg-Klick wählt mehrere; Entf leert.',
      area: 'Bereich', view: 'Ansicht', viewA: 'A Raster + Seitenleiste', viewB: 'B Vollbild + schwebende Leiste', undo: 'Rückgängig', redo: 'Wiederholen',
      grey: 'grau', light: 'hellgrau', red: 'rot', frame: 'Rahmen', clear: 'leeren', zoom: 'Zoom', search: 'Suche', searchPh: 'Id/Name',
      save: 'Speichern', saveTip: 'Legt eine Speicherung im Dev-Bereich ab; wird als Branch tz-layout-* abgeholt und kommt ins nächste Bündel', download: 'JSON-Download',
      check: 'Prüfung (live)', fresh: 'Bereich „Neu“ (nicht platziert)', unknownTest: 'Unbekannte Id testen', setCell: 'als Zelle setzen', saved: 'Gespeichert (letzte)', none: 'noch keine',
      dups: 'Doppelte', missing: 'Fehlende (nicht platziert)', unknown: 'Unbekannte', reds: 'Rote Felder', loading: 'Lade das Layout der Testzentrale …',
      noData: 'data/testcentre/index.json fehlt (python3 tools/tzedit/build_editor.py).', areaFail: 'Bereich nicht ladbar: ', pick: ' erst Zelle wählen',
      size: '{0} Reihen x {1} Spalten, {2} Ids', discard: 'Ungespeicherte Änderungen verwerfen?', commentQ: 'Kurz: was wurde geändert? (optional)', saving: ' speichere …',
      okSave: ' gespeichert (Nr. {0}) - kommt als Branch tz-layout-* ins nächste Bündel', badSave: ' nicht gespeichert: ', login: ' (angemeldet?)', offline: ' nicht gespeichert: Server nicht erreichbar (JSON-Download geht immer)',
      downloaded: ' heruntergeladen', mod: 'Mod', row: 'Reihe', column: 'Spalte', top: 'Achtel darauf', isNew: 'Neu', devOnly: 'Diese Seite gibt es nur im Dev-Bereich.',
      legend: 'Kleines Bild oben rechts = Achtel auf dem Block. Hellgrau = Platzhalter „gibt es nicht“, rot = fehlt, leer = kein Feld. Vanilla-Bilder nur auf dem Dev-Host, über die Blockmodelle gefunden (data/vanilla-icons.json); ohne sie der Name.' }
  };
  var CSS = [
    '.tz-page{--z:36px}.tz-bar{display:flex;flex-wrap:wrap;gap:.4rem;align-items:center;margin:.4rem 0 .6rem}',
    '.tz-bar label{display:inline-flex;gap:.3rem;align-items:center}.tz-wrap{display:flex;gap:.8rem;align-items:flex-start}',
    '.tz-main{flex:1;min-width:0;overflow:auto;max-height:72vh;border:1px solid var(--border);background:var(--panel-2);border-radius:6px;padding:6px}',
    '.tz-side{width:280px;flex:none;max-height:72vh;overflow:auto}.tz-side h3{font-size:.85rem;margin:.6rem 0 .2rem}.tz-side ul{margin:.1rem 0;padding-left:1rem;max-height:110px;overflow:auto;font-size:.8rem}',
    '.tz-grid{display:grid;gap:2px;width:max-content}',
    '.tz-h,.tz-r{font-size:10px;background:var(--panel);color:var(--text);padding:2px;overflow:hidden;cursor:grab;user-select:none;border:1px solid var(--border);border-radius:2px}',
    '.tz-h{writing-mode:vertical-rl;height:84px;width:var(--z);text-align:center}.tz-r{width:120px;display:flex;align-items:center;height:var(--z);white-space:nowrap}',
    '.tz-c{width:var(--z);height:var(--z);background:var(--slot-bg);box-shadow:inset 2px 2px 0 var(--slot-lo),inset -2px -2px 0 var(--slot-hi);border-radius:2px;position:relative;display:flex;align-items:center;justify-content:center;overflow:hidden}',
    '.tz-c.tz-nil{background:transparent;box-shadow:inset 0 0 0 1px var(--border)}',
    '.tz-c img{width:calc(var(--z) - 8px);height:calc(var(--z) - 8px);image-rendering:pixelated;pointer-events:none}',
    '.tz-c img.tz-t{position:absolute;right:1px;top:1px;width:45%;height:45%;background:var(--panel);outline:1px solid var(--slot-lo)}',
    '.tz-c .tz-n{font-size:8px;line-height:1;word-break:break-all;color:var(--slot-text);text-align:center;pointer-events:none;padding:1px}',
    '.tz-c.sel{outline:2px solid var(--accent);z-index:1}.tz-c.dim{opacity:.2}.tz-c.hit{outline:2px solid var(--warn-border)}',
    '.tz-c.dup{box-shadow:inset 0 0 0 3px var(--danger-border)}.tz-c.unk{box-shadow:inset 0 0 0 3px var(--warn-border)}.tz-over{outline:2px dashed var(--accent)}',
    '.tz-c[class*=tz-ph-]{box-shadow:none}.tz-ph-grey{background:#777}.tz-ph-light{background:repeating-linear-gradient(45deg,#dcdcdc 0 4px,#c4c4c4 4px 8px)}.tz-ph-red{background:#c33}.tz-ph-frame{box-shadow:inset 0 0 0 3px #8b6b3d}',
    '.tz-pool{display:flex;flex-wrap:wrap;gap:2px}.tz-pool .tz-c{width:34px;height:34px}',
    '.tz-ok{color:var(--accent)}.tz-bad{color:var(--danger-text)}',
    '.tz-info{position:fixed;pointer-events:none;background:var(--panel);color:var(--text);border:1px solid var(--border-strong);padding:5px 8px;border-radius:4px;display:none;z-index:99;font-size:12px;box-shadow:0 3px 12px rgba(0,0,0,.25)}',
    '.tz-page.tz-b{position:fixed;inset:0;z-index:60;background:var(--bg);padding:3.4rem .6rem .6rem;overflow:auto}',
    '.tz-page.tz-b .tz-side{display:none}.tz-page.tz-b .tz-main{max-height:none}',
    '.tz-page.tz-b .tz-bar{position:fixed;left:50%;top:.4rem;transform:translateX(-50%);z-index:61;background:var(--panel);border:1px solid var(--accent);border-radius:8px;padding:.3rem .6rem;box-shadow:0 4px 18px rgba(0,0,0,.3);max-width:96vw;margin:0}',
    '.tz-mini{font-size:.8rem}',
    '.tz-wrap>.tz-zm{flex:1;min-width:0;margin:0}.tz-zm .tz-main{max-height:72vh}.tz-page.tz-b .tz-zm .tz-main{max-height:none}'
  ].join('\n');
  var PH = ['#grey', '#light', '#red', '#frame'];
  var ZBASE = 36, ZMIN = 18, ZMAX = 108;  // cell size in px: 100 % = 36, zoom 50 % .. 300 %

  function esc(t) { return String(t == null ? '' : t).replace(/[&<>"']/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]; }); }
  function nsOf(id) { return id.indexOf(':') >= 0 ? id.split(':')[0] : 'minecraft'; }
  function pathOf(id) { return id.indexOf(':') >= 0 ? id.split(':')[1] : id; }
  function fmt(s, a) { return s.replace(/\{(\d)\}/g, function (m, i) { return a[+i]; }); }

  /* Pure helpers (also used by the node test wiki/tests/tzeditor.cjs). */
  function placedCount(rows) { var m = {}; rows.forEach(function (r) { r.cells.forEach(function (c) { if (c && c.charAt(0) !== '#') m[c] = (m[c] || 0) + 1; }); }); return m; }
  function layoutOf(D, cols, rows) {
    return { schema: 1, variant: D.variant, area: D.area, columns: cols, excluded: D.excluded || [],
      rows: rows.map(function (r) { var o = { key: r.key, label: r.label, cells: r.cells }, t = {};
        Object.keys(r.tops || {}).forEach(function (c) { if (cols.indexOf(c) >= 0) t[c] = r.tops[c]; });
        if (Object.keys(t).length) o.tops = t; return o; }) };
  }
  function moveColumn(cols, rows, from, to) { var c = cols.splice(from, 1)[0]; cols.splice(to, 0, c); rows.forEach(function (r) { var x = r.cells.splice(from, 1)[0]; r.cells.splice(to, 0, x); }); }

  function mount(el, ctx) {
    var lang = ctx.lang === 'de' ? 'de' : 'en', S = STR[lang];
    function tr(k) { return S[k] || STR.en[k] || k; }
    if (!ctx.dev) { el.innerHTML = '<div class="box box-danger">' + esc(tr('devOnly')) + '</div>'; return; }
    var st = ctx.state || (ctx.state = { idx: null, file: '', D: null, cols: [], rows: [], known: {}, sel: {}, hist: [], fut: [], view: 'a', zoom: 36, q: '', msg: '', history: [], icons: null });
    var drag = null, zoom = null;
    api.dirtyNow = function () { return st.hist.length > 0; };
    function $(s) { return el.querySelector(s); }
    function snap() { return JSON.stringify({ cols: st.cols, rows: st.rows }); }
    function push() { st.hist.push(snap()); if (st.hist.length > 200) st.hist.shift(); st.fut = []; }
    function restore(s) { var o = JSON.parse(s); st.cols = o.cols; st.rows = o.rows; st.sel = {}; draw(); }
    function msg(t) { st.msg = t; var m = $('.tz-msg'); if (m) m.textContent = t; }

    /* Picture of an id (N72). Candidates in order, each tried when the one before fails to load:
       1. the module picture of index.json (tools/tzedit/build_editor.py),
       2. data/vanilla-icons.json (dev host only, wiki/vanilla_textures.py --icons): the game's own item -> model ->
          texture walk, so slabs/stairs/walls/bars/waxed/lanterns/octets get the file their model names,
       3. the wiki's picture of the id (ctx.icon), 4. the old guesses by name (Vanilla block/item of the same name).
       "file#x,y,w,h" = crop of a skin (mob heads without a flat picture), drawn in the browser. All failing: the name. */
    function candidates(id) {
      var out = [], full = id.indexOf(':') >= 0 ? id : 'minecraft:' + id;
      function add(u) { if (u && out.indexOf(u) < 0) out.push(u); }
      add(st.idx && st.idx.textures[id]);
      add(st.icons && (st.icons[id] || st.icons[full]));
      if (ctx.icon) add(ctx.icon(full));
      var names = nsOf(id) === 'minecraft' ? [pathOf(id)] : (st.idx && st.idx.vanilla && st.idx.vanilla[id]) || [];
      names.forEach(function (n) { ['assets/vanilla/block/', 'assets/vanilla/item/'].forEach(function (p) { add(p + n + '.png'); }); });
      return out;
    }
    function crop(i, url) {
      var h = url.indexOf('#'), r = url.slice(h + 1).split(',').map(Number), src = new Image();
      src.onload = function () {
        var c = document.createElement('canvas'); c.width = r[2]; c.height = r[3];
        c.getContext('2d').drawImage(src, r[0], r[1], r[2], r[3], 0, 0, r[2], r[3]);
        i.src = c.toDataURL();
      };
      src.onerror = function () { i.onerror(); };
      src.src = url.slice(0, h);
    }
    function img(id, cls) {
      var cands = candidates(id);
      if (!cands.length) return null;
      var i = new Image(), k = 0; i.alt = ''; i.dataset.ownFallback = '1'; if (cls) i.className = cls;
      function load() { if (cands[k].indexOf('#') > 0) crop(i, cands[k]); else i.src = cands[k]; }
      i.onerror = function () { k++; if (k < cands.length) { load(); return; } var p = i.parentNode; i.remove(); if (p && !cls) name(p, id); };
      load();
      return i;
    }
    function name(e, id) { var n = document.createElement('span'); n.className = 'tz-n'; n.textContent = pathOf(id).replace(/_/g, ' '); e.appendChild(n); }
    function cellEl(id, r, c, pool) {
      var e = document.createElement('div'); e.className = 'tz-c'; e.draggable = true; e.dataset.r = r; e.dataset.c = c; e.dataset.id = id || '';
      if (pool) e.dataset.pool = '1';
      if (!id) e.classList.add('tz-nil');
      else if (id.charAt(0) === '#') e.classList.add('tz-ph-' + id.slice(1));
      else { var i = img(id); if (i) e.appendChild(i); else name(e, id); }
      var row = !pool && st.rows[r];
      if (row && id && row.tops && row.tops[st.cols[c]]) { var t = img(row.tops[st.cols[c]], 'tz-t'); if (t) e.appendChild(t); }
      return e;
    }

    function shell() {
      var h = '<div class="tz-page' + (st.view === 'b' ? ' tz-b' : '') + '" style="--z:' + st.zoom + 'px"><style>' + CSS + '</style>';
      h += '<h1>' + esc(tr('title')) + '</h1><p class="muted">' + esc(tr('intro')) + '</p>';
      h += '<div class="tz-bar"><label>' + esc(tr('area')) + ' <select data-k="area">' + st.idx.areas.map(function (a) {
        return '<option value="' + esc(a.file) + '"' + (a.file === st.file ? ' selected' : '') + '>' + esc(a.variant + ' / ' + a.name + ' (' + a.rows + ')') + '</option>'; }).join('') + '</select></label>';
      h += '<span class="muted tz-size"></span>';
      h += '<label>' + esc(tr('view')) + ' <select data-k="view"><option value="a"' + (st.view === 'a' ? ' selected' : '') + '>' + esc(tr('viewA')) + '</option><option value="b"' + (st.view === 'b' ? ' selected' : '') + '>' + esc(tr('viewB')) + '</option></select></label>';
      h += '<button type="button" class="btn" data-act="undo">' + esc(tr('undo')) + '</button><button type="button" class="btn" data-act="redo">' + esc(tr('redo')) + '</button>';
      PH.forEach(function (p) { h += '<button type="button" class="btn" data-ph="' + p + '">' + esc(tr(p.slice(1))) + '</button>'; });
      h += '<button type="button" class="btn" data-act="clear">' + esc(tr('clear')) + '</button>';
      h += '<label>' + esc(tr('zoom')) + ' <input type="range" min="' + ZMIN + '" max="' + ZMAX + '" value="' + st.zoom + '" data-k="zoom" style="width:80px"></label>';
      h += '<label>' + esc(tr('search')) + ' <input data-k="q" placeholder="' + esc(tr('searchPh')) + '" value="' + esc(st.q) + '"></label>';
      h += '<button type="button" class="btn btn-primary" data-act="save" title="' + esc(tr('saveTip')) + '">' + esc(tr('save')) + '</button>';
      h += '<button type="button" class="btn" data-act="dl">' + esc(tr('download')) + '</button><span class="tz-msg">' + esc(st.msg) + '</span><span class="tz-mini muted"></span></div>';
      h += '<div class="tz-wrap"><div class="tz-main"><div class="tz-grid"></div></div><div class="tz-side box box-info">';
      h += '<h3>' + esc(tr('check')) + '</h3><div class="tz-chk"></div><h3>' + esc(tr('fresh')) + ' <span class="tz-ncnt"></span></h3><div class="tz-pool"></div>';
      h += '<h3>' + esc(tr('unknownTest')) + '</h3><input class="tz-unk" placeholder="mod:id"> <button type="button" class="btn" data-act="unk">' + esc(tr('setCell')) + '</button>';
      h += '<h3>' + esc(tr('saved')) + '</h3><ul class="tz-hist"></ul><p class="muted tiny">' + esc(tr('legend')) + '</p></div></div><div class="tz-info"></div></div>';
      el.innerHTML = h;
      wire();
      zoomArea();
      draw();
      drawHistory();
    }

    /* N72: the shared zoom/full screen bar of the wiki (assets/zoomarea.js) on the grid; the zoom level is the cell size
       (--z, base 36 px = 100 %), so the pixel pictures stay sharp; the slider is the same value. */
    function zoomArea() {
      var Z = root.SBZoom, main = $('.tz-main'), page = $('.tz-page');
      if (!Z || !main) return;
      zoom = Z.attach(main, { key: 'tzeditor', lang: lang, className: 'tz-zm', initial: st.zoom / ZBASE, min: ZMIN / ZBASE, max: ZMAX / ZBASE,
        apply: function (f) { st.zoom = Math.round(ZBASE * f); page.style.setProperty('--z', st.zoom + 'px'); var r = $('[data-k=zoom]'); if (r) r.value = st.zoom; } });
    }

    function draw() {
      var g = $('.tz-grid'); if (!g) return;
      g.innerHTML = '';
      g.style.gridTemplateColumns = '120px repeat(' + st.cols.length + ',var(--z))';
      var pl = placedCount(st.rows), q = st.q.trim().toLowerCase();
      var frag = document.createDocumentFragment();
      frag.appendChild(document.createElement('div'));
      st.cols.forEach(function (c, ci) { var h = document.createElement('div'); h.className = 'tz-h'; h.textContent = c; h.draggable = true; h.dataset.col = ci; frag.appendChild(h); });
      st.rows.forEach(function (row, ri) {
        var l = document.createElement('div'); l.className = 'tz-r'; l.textContent = row.label; l.title = row.key; l.draggable = true; l.dataset.row = ri; frag.appendChild(l);
        row.cells.forEach(function (id, ci) {
          var e = cellEl(id, ri, ci, false);
          if (st.sel[ri + ',' + ci]) e.classList.add('sel');
          if (id && id.charAt(0) !== '#') { if (pl[id] > 1) e.classList.add('dup'); else if (!st.known[id]) e.classList.add('unk'); }
          if (q) e.classList.add(id && id.toLowerCase().indexOf(q) >= 0 ? 'hit' : 'dim');
          frag.appendChild(e);
        });
      });
      g.appendChild(frag);
      var unplaced = Object.keys(st.known).filter(function (k) { return !pl[k]; }).sort();
      var P = $('.tz-pool'); P.innerHTML = '';
      unplaced.forEach(function (id) { var e = cellEl(id, 'p', id, true); if (q && id.indexOf(q) < 0) e.classList.add('dim'); P.appendChild(e); });
      $('.tz-ncnt').textContent = '(' + unplaced.length + ')';
      var dups = Object.keys(pl).filter(function (k) { return pl[k] > 1; }).map(function (k) { return k + ' x' + pl[k]; });
      var unk = Object.keys(pl).filter(function (k) { return !st.known[k]; });
      var reds = []; st.rows.forEach(function (r) { r.cells.forEach(function (c, i) { if (c === '#red') reds.push(r.label + ' / ' + st.cols[i]); }); });
      function sec(t, a) { return '<div class="' + (a.length ? 'tz-bad' : 'tz-ok') + '">' + esc(t) + ': ' + a.length + '</div>' + (a.length ? '<ul>' + a.slice(0, 300).map(function (x) { return '<li>' + esc(x) + '</li>'; }).join('') + '</ul>' : ''); }
      $('.tz-chk').innerHTML = sec(tr('dups'), dups) + sec(tr('missing'), unplaced) + sec(tr('unknown'), unk) + sec(tr('reds'), reds);
      $('.tz-size').textContent = fmt(tr('size'), [st.rows.length, st.cols.length, Object.keys(st.known).length]);
      $('.tz-mini').textContent = st.view === 'b' ? [tr('dups') + ': ' + dups.length, tr('missing') + ': ' + unplaced.length, tr('unknown') + ': ' + unk.length, tr('reds') + ': ' + reds.length].join(' | ') : '';
    }

    function setSel(v) { var keys = Object.keys(st.sel); if (!keys.length) return; push(); keys.forEach(function (k) { var p = k.split(','); st.rows[+p[0]].cells[+p[1]] = v; }); draw(); }

    keys = function (e) {
      if (e.target && /INPUT|SELECT|TEXTAREA/.test(e.target.tagName)) return;
      if (e.key === 'Delete') setSel(null);
      if (e.ctrlKey && e.key === 'z') { e.preventDefault(); if (st.hist.length) { st.fut.push(snap()); restore(st.hist.pop()); } }
      if (e.ctrlKey && e.key === 'y') { e.preventDefault(); if (st.fut.length) { st.hist.push(snap()); restore(st.fut.pop()); } }
    };

    function wire() {
      var page = $('.tz-page'), info = $('.tz-info');
      page.addEventListener('change', function (e) {
        var k = e.target.dataset.k;
        if (k === 'area') { if (st.hist.length && !window.confirm(tr('discard'))) { e.target.value = st.file; return; } loadArea(e.target.value); }
        else if (k === 'view') { st.view = e.target.value; shell(); }
      });
      page.addEventListener('input', function (e) {
        var k = e.target.dataset.k;
        if (k === 'zoom') { if (zoom) zoom.setZoom(+e.target.value / ZBASE); else { st.zoom = +e.target.value; page.style.setProperty('--z', st.zoom + 'px'); } }
        else if (k === 'q') { st.q = e.target.value; draw(); }
      });
      page.addEventListener('click', function (e) {
        var b = e.target.closest('[data-act],[data-ph]');
        if (b) {
          var a = b.dataset.act;
          if (b.dataset.ph) setSel(b.dataset.ph);
          else if (a === 'undo') { if (st.hist.length) { st.fut.push(snap()); restore(st.hist.pop()); } }
          else if (a === 'redo') { if (st.fut.length) { st.hist.push(snap()); restore(st.fut.pop()); } }
          else if (a === 'clear') setSel(null);
          else if (a === 'unk') { var v = $('.tz-unk').value.trim(); if (v && Object.keys(st.sel).length) setSel(v); else msg(tr('pick')); }
          else if (a === 'dl') download();
          else if (a === 'save') save();
          return;
        }
        var t = e.target.closest('.tz-c');
        if (!t || t.dataset.pool) return;
        var k = t.dataset.r + ',' + t.dataset.c;
        if (e.ctrlKey || e.shiftKey || e.metaKey) { if (st.sel[k]) delete st.sel[k]; else st.sel[k] = 1; }
        else { var only = Object.keys(st.sel).length === 1 && st.sel[k]; st.sel = {}; if (!only) st.sel[k] = 1; }
        draw();
      });
      page.addEventListener('dragstart', function (e) {
        var t = e.target.closest('.tz-c,.tz-h,.tz-r'); if (!t) return;
        if (t.dataset.col != null) drag = { t: 'col', i: +t.dataset.col };
        else if (t.dataset.row != null) drag = { t: 'row', i: +t.dataset.row };
        else if (t.dataset.pool) drag = { t: 'pool', id: t.dataset.id };
        else { var k = t.dataset.r + ',' + t.dataset.c; if (!st.sel[k]) { st.sel = {}; st.sel[k] = 1; } drag = { t: 'cell', r: +t.dataset.r, c: +t.dataset.c }; }
        e.dataTransfer.setData('text/plain', 'x');
      });
      page.addEventListener('dragover', function (e) { if (drag && e.target.closest('.tz-c,.tz-h,.tz-r')) e.preventDefault(); });
      page.addEventListener('drop', function (e) {
        var t = e.target.closest('.tz-c,.tz-h,.tz-r'); if (!t || !drag) return; e.preventDefault(); push();
        if (drag.t === 'col' && t.dataset.col != null) moveColumn(st.cols, st.rows, drag.i, +t.dataset.col);
        else if (drag.t === 'row' && t.dataset.row != null) { var r = st.rows.splice(drag.i, 1)[0]; st.rows.splice(+t.dataset.row, 0, r); }
        else if (t.classList.contains('tz-c') && !t.dataset.pool) {
          var ri = +t.dataset.r, ci = +t.dataset.c;
          if (drag.t === 'pool') st.rows[ri].cells[ci] = drag.id;
          else if (drag.t === 'cell') { var a = st.rows[drag.r].cells, bb = st.rows[ri].cells, x = a[drag.c]; a[drag.c] = bb[ci]; bb[ci] = x; }
          else st.hist.pop();
        } else st.hist.pop();
        drag = null; st.sel = {}; draw();
      });
      page.addEventListener('mousemove', function (e) {
        var t = e.target.closest('.tz-c');
        if (!t || !t.dataset.id || t.dataset.id.charAt(0) === '#') { info.style.display = 'none'; return; }
        var id = t.dataset.id, row = t.dataset.pool ? null : st.rows[t.dataset.r], top = row && row.tops && row.tops[st.cols[t.dataset.c]];
        info.innerHTML = '<b>' + esc(id) + '</b><br>' + esc(tr('mod')) + ': ' + esc(nsOf(id)) + '<br>' + esc(tr('row')) + ': ' + esc(row ? row.label + ' (' + row.key + ')' : tr('isNew'))
          + (row ? '<br>' + esc(tr('column')) + ': ' + esc(st.cols[t.dataset.c]) : '') + (top ? '<br>' + esc(tr('top')) + ': ' + esc(top) : '');
        info.style.display = 'block'; info.style.left = e.clientX + 14 + 'px'; info.style.top = e.clientY + 14 + 'px';
      });
      page.addEventListener('mouseleave', function () { info.style.display = 'none'; });
    }

    function download() {
      var a = document.createElement('a');
      a.href = URL.createObjectURL(new Blob([JSON.stringify(layoutOf(st.D, st.cols, st.rows), null, 1)], { type: 'application/json' }));
      a.download = 'layout_' + st.D.variant + '_' + st.D.area + '.json'; a.click(); msg(tr('downloaded'));
    }
    function save() {
      var comment = window.prompt(tr('commentQ'), ''); if (comment === null) return;
      var b = $('[data-act=save]'); b.disabled = true; msg(tr('saving'));
      fetch('/api/testcentre', { method: 'POST', credentials: 'same-origin', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(Object.assign(layoutOf(st.D, st.cols, st.rows), { comment: comment })) })
        .then(function (r) { return r.json().catch(function () { return {}; }).then(function (j) {
          if (r.ok) { msg(fmt(tr('okSave'), [j.sid])); st.hist = []; st.fut = []; loadHistory(); }
          else msg(tr('badSave') + (j.error || r.status) + (r.status === 401 || r.status === 403 ? tr('login') : ''));
        }); })
        .catch(function () { msg(tr('offline')); })
        .then(function () { var bb = $('[data-act=save]'); if (bb) bb.disabled = false; });
    }
    function drawHistory() {
      var u = $('.tz-hist'); if (!u || !st.D) return;
      var mine = st.history.filter(function (x) { return x.area === st.D.area && (!x.variant || x.variant === st.D.variant); }).slice(0, 10);
      u.innerHTML = mine.map(function (x) { return '<li>' + esc(new Date(x.createdAt).toLocaleString()) + ' · ' + esc(x.by || '?') + ' · ' + esc(x.state) + (x.comment ? ' · ' + esc(x.comment) : '') + '</li>'; }).join('') || '<li>' + esc(tr('none')) + '</li>';
    }
    function loadHistory() {
      fetch('/api/testcentre', { credentials: 'same-origin' }).then(function (r) { return r.ok ? r.json() : []; })
        .then(function (a) { st.history = Array.isArray(a) ? a : []; drawHistory(); }).catch(function () {});
    }
    function loadArea(file) {
      fetch('data/testcentre/' + file, { cache: 'no-cache' }).then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); }).then(function (D) {
        st.file = file; st.D = D; st.cols = D.columns.slice();
        st.rows = D.rows.map(function (r) { return { key: r.key, label: r.label, cells: r.cells.slice(), tops: Object.assign({}, r.tops || {}) }; });
        st.known = {}; st.rows.forEach(function (r) { r.cells.forEach(function (c) { if (c && c.charAt(0) !== '#') st.known[c] = 1; }); });
        st.sel = {}; st.hist = []; st.fut = []; st.msg = '';
        if (ctx.onArea) ctx.onArea(file);
        shell();
      }).catch(function () { msg(tr('areaFail') + file); });
    }

    if (st.idx && st.D) { shell(); return; }
    el.innerHTML = '<h1>' + esc(tr('title')) + '</h1><p class="hint">' + esc(tr('loading')) + '</p>';
    /* the id -> picture list exists only on the dev host (git-ignored); without it the editor still works */
    var iconsP = st.icons ? Promise.resolve(st.icons) : fetch('data/vanilla-icons.json', { cache: 'no-cache' })
      .then(function (r) { return r.ok ? r.json() : null; }).then(function (j) { return (j && j.icons) || {}; }).catch(function () { return {}; });
    Promise.all([fetch('data/testcentre/index.json', { cache: 'no-cache' }).then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); }), iconsP]).then(function (both) {
      var idx = both[0]; st.icons = both[1];
      st.idx = idx;
      var want = ctx.area && idx.areas.filter(function (a) { return a.file === ctx.area; })[0];
      loadArea(want ? want.file : idx.areas[0].file);
      loadHistory();
    }).catch(function () { el.innerHTML = '<h1>' + esc(tr('title')) + '</h1><div class="box box-danger">' + esc(tr('noData')) + '</div>'; });
  }

  /* One document listener for Delete / Ctrl+Z / Ctrl+Y; it acts on the mounted editor only while its page is shown. */
  var keys = null;
  if (typeof document !== 'undefined') document.addEventListener('keydown', function (e) { if (keys && document.querySelector('.tz-page')) keys(e); });
  if (typeof window !== 'undefined') window.addEventListener('beforeunload', function (e) { var p = document.querySelector('.tz-page'); if (p && api.dirtyNow && api.dirtyNow()) { e.preventDefault(); e.returnValue = ''; } });

  var api = { mount: mount, STR: STR, placedCount: placedCount, layoutOf: layoutOf, moveColumn: moveColumn,
    dirty: function (ctx) { return !!(ctx && ctx.state && ctx.state.hist.length); } };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.SBTzEditor = api;
})(typeof self !== 'undefined' ? self : this);
