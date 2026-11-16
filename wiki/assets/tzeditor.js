/* Testzentrale-Editor (dev page ?tab=tzeditor / #/tzeditor, N50 TZEDIT50, plan docs/ai/PLAN-B-TZEDIT50-2026-11-14.md;
   N72/N72b plan docs/ai/PLAN-N72-TZEDITOR-2026-11-16.md).
   Edits the layout configs of the test centre order areas (first export from the game: all areas of both variants,
   data/testcentre/index.json + layout_<variant>_<area>.json, written by tools/tzedit/build_editor.py).
   Shown only on the dev site (index.html: isDevSite, dev sidebar group). Styles use the wiki tokens (--panel, --border,
   --slot-bg ...); the placeholder colours are the in-game blocks (grey line, light grey "none", red "missing").
   N72b: own right-click menu per cell (also Shift+F10 / context menu key), "All areas" side by side, view "as built"
   (the real line arrangement of OrderAreas.families: grey middle line, side A metals -> wood -> aging -> rest -> last stone,
   side B stone; families and the algorithmic rows from the game plan data/testcentre/plan.json), checks from the plan.
   Save = POST /api/testcentre per area (tools/tzedit/tzedit_save.py check_save), JSON download always works. */
(function (root) {
  'use strict';
  var STR = {
    en: { title: 'Test centre editor', intro: 'The order areas of the test centre as config: one file per area, exported from the game (all modules). Drag cells, rows and columns; Ctrl-click selects several; right-click (or Shift+F10) opens the cell menu; Delete empties.',
      area: 'Area', allAreas: 'All areas (overview)', view: 'View', viewA: 'A grid + side panel', viewB: 'B full screen + floating bar', layout: 'Layout', table: 'Table', built: 'As built (lines)',
      undo: 'Undo', redo: 'Redo',
      grey: 'grey', light: 'light grey', red: 'red', frame: 'frame', clear: 'empty', zoom: 'Zoom', search: 'Search', searchPh: 'id/name',
      save: 'Save', saveTip: 'Stores a save on the dev host; picked up as branch tz-layout-*, goes into the next bundle', download: 'JSON download',
      check: 'Check (live)', fresh: 'Area "New" (not placed)', unknownTest: 'Try an unknown id', setCell: 'set as cell', saved: 'Saved (latest)', none: 'none yet',
      dups: 'Duplicates', missing: 'Missing (not placed)', unknown: 'Unknown', reds: 'Red fields', loading: 'Loading the test centre layout …',
      noData: 'data/testcentre/index.json is missing (python3 tools/tzedit/build_editor.py).', areaFail: 'Area cannot be loaded: ', pick: ' select a cell first',
      size: '{0} rows x {1} columns, {2} ids', sizeAll: '{0} areas, {1} ids', discard: 'Discard unsaved changes?', commentQ: 'Short: what changed? (optional)', saving: ' saving …',
      okSave: ' saved (no. {0}) - comes as branch tz-layout-* into the next bundle', badSave: ' not saved: ', login: ' (logged in?)', offline: ' not saved: server unreachable (JSON download always works)',
      downloaded: ' downloaded', mod: 'Mod', row: 'Row', column: 'Column', top: 'Octet on top', isNew: 'New', devOnly: 'This page exists only on the dev site.',
      legend: 'Small picture top right = octet standing on the block. Light grey = placeholder "does not exist", red = missing, empty = no field. Vanilla pictures only on the dev host, found through the block models (data/vanilla-icons.json); without them the name.',
      menu: 'Cell menu', mChange: 'Change block …', mMissing: 'Mark as missing (red)', mAbsent: 'Does not exist (light grey)', mClear: 'Empty', mFrame: 'Frame on/off',
      mFrameItem: 'Frame only on empty or placeholder fields', mRow: 'Insert new row here', mCol: 'Insert new column here', mUndo: 'Undo',
      chTitle: 'Change block', chPh: 'id or name, e.g. minecraft:stone', chSet: 'Set', chCancel: 'Cancel', chAny: 'Any id works (mod:name); Enter sets the marked hit or the typed id.', chBad: 'Not an id: ',
      newRowQ: 'Name of the new row', newRow: 'New row', newColQ: 'Name of the new column (letters, digits, _ -)', newCol: 'new',
      compact: 'Compact per mod: not placed', compactOk: 'every mod block of the game plan has a field', exceptions: 'Exceptions (with reason)', linesNew: 'Lines: band "New" of the game',
      planSrc: 'Game plan: {0}', noPlan: 'No game plan (data/testcentre/plan.json) - checks only from the layout files.',
      sideA: 'Side A', sideB: 'Side B', line: 'grey middle line', band: 'Line pair {0}', restRo: 'built by the algorithm (no config file) - read only', builtNote: 'The view follows OrderAreas.families: rows next to each other along the middle line, field 0 next to the path. Families and the rows without a config come from the game plan.',
      dirty: 'unsaved: {0}', roCell: 'read only (algorithm)' },
    de: { title: 'Testzentrale-Editor', intro: 'Die geordneten Bereiche der Testzentrale als Config: eine Datei je Bereich, aus dem Spiel exportiert (alle Module). Zellen, Reihen und Spalten ziehen; Strg-Klick wählt mehrere; Rechtsklick (oder Umschalt+F10) öffnet das Zellmenü; Entf leert.',
      area: 'Bereich', allAreas: 'Alle Bereiche (Gesamtansicht)', view: 'Ansicht', viewA: 'A Raster + Seitenleiste', viewB: 'B Vollbild + schwebende Leiste', layout: 'Anordnung', table: 'Tabelle', built: 'Wie gebaut (Linien)',
      undo: 'Rückgängig', redo: 'Wiederholen',
      grey: 'grau', light: 'hellgrau', red: 'rot', frame: 'Rahmen', clear: 'leeren', zoom: 'Zoom', search: 'Suche', searchPh: 'Id/Name',
      save: 'Speichern', saveTip: 'Legt eine Speicherung im Dev-Bereich ab; wird als Branch tz-layout-* abgeholt und kommt ins nächste Bündel', download: 'JSON-Download',
      check: 'Prüfung (live)', fresh: 'Bereich „Neu“ (nicht platziert)', unknownTest: 'Unbekannte Id testen', setCell: 'als Zelle setzen', saved: 'Gespeichert (letzte)', none: 'noch keine',
      dups: 'Doppelte', missing: 'Fehlende (nicht platziert)', unknown: 'Unbekannte', reds: 'Rote Felder', loading: 'Lade das Layout der Testzentrale …',
      noData: 'data/testcentre/index.json fehlt (python3 tools/tzedit/build_editor.py).', areaFail: 'Bereich nicht ladbar: ', pick: ' erst Zelle wählen',
      size: '{0} Reihen x {1} Spalten, {2} Ids', sizeAll: '{0} Bereiche, {1} Ids', discard: 'Ungespeicherte Änderungen verwerfen?', commentQ: 'Kurz: was wurde geändert? (optional)', saving: ' speichere …',
      okSave: ' gespeichert (Nr. {0}) - kommt als Branch tz-layout-* ins nächste Bündel', badSave: ' nicht gespeichert: ', login: ' (angemeldet?)', offline: ' nicht gespeichert: Server nicht erreichbar (JSON-Download geht immer)',
      downloaded: ' heruntergeladen', mod: 'Mod', row: 'Reihe', column: 'Spalte', top: 'Achtel darauf', isNew: 'Neu', devOnly: 'Diese Seite gibt es nur im Dev-Bereich.',
      legend: 'Kleines Bild oben rechts = Achtel auf dem Block. Hellgrau = Platzhalter „gibt es nicht“, rot = fehlt, leer = kein Feld. Vanilla-Bilder nur auf dem Dev-Host, über die Blockmodelle gefunden (data/vanilla-icons.json); ohne sie der Name.',
      menu: 'Zellmenü', mChange: 'Block ändern …', mMissing: 'Als fehlend markieren (rot)', mAbsent: 'Gibt es nicht (hellgrau)', mClear: 'Leeren', mFrame: 'Rahmen an/aus',
      mFrameItem: 'Rahmen nur auf leeren oder Platzhalter-Feldern', mRow: 'Neue Reihe hier einfügen', mCol: 'Neue Spalte hier einfügen', mUndo: 'Rückgängig',
      chTitle: 'Block ändern', chPh: 'Id oder Name, z. B. minecraft:stone', chSet: 'Setzen', chCancel: 'Abbrechen', chAny: 'Jede Id geht (mod:name); Enter setzt den markierten Treffer oder die getippte Id.', chBad: 'Keine Id: ',
      newRowQ: 'Name der neuen Reihe', newRow: 'Neue Reihe', newColQ: 'Name der neuen Spalte (Buchstaben, Ziffern, _ -)', newCol: 'neu',
      compact: 'Kompakt je Mod: nicht platziert', compactOk: 'jeder Mod-Block des Spielplans hat einen Platz', exceptions: 'Ausnahmen (mit Begründung)', linesNew: 'Linien: Band „Neu“ des Spiels',
      planSrc: 'Spielplan: {0}', noPlan: 'Kein Spielplan (data/testcentre/plan.json) - Prüfung nur aus den Layout-Dateien.',
      sideA: 'Seite A', sideB: 'Seite B', line: 'graue Mittellinie', band: 'Linien-Paar {0}', restRo: 'vom Algorithmus gebaut (keine Config-Datei) - nur lesen', builtNote: 'Die Ansicht folgt OrderAreas.families: Reihen nebeneinander an der Mittellinie, Feld 0 am Weg. Familien und die Reihen ohne Config kommen aus dem Spielplan.',
      dirty: 'ungespeichert: {0}', roCell: 'nur lesen (Algorithmus)' }
  };
  var CSS = [
    '.tz-page{--z:36px}.tz-bar{display:flex;flex-wrap:wrap;gap:.4rem;align-items:center;margin:.4rem 0 .6rem}',
    '.tz-bar label{display:inline-flex;gap:.3rem;align-items:center}.tz-wrap{display:flex;gap:.8rem;align-items:flex-start}',
    '.tz-main{flex:1;min-width:0;overflow:auto;max-height:72vh;border:1px solid var(--border);background:var(--panel-2);border-radius:6px;padding:6px}',
    '.tz-main:focus-visible{outline:2px solid var(--accent);outline-offset:1px}',
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
    '.tz-c.tz-ro{opacity:.75;cursor:default}.tz-c.tz-fill{background:repeating-linear-gradient(45deg,#dcdcdc 0 4px,#c4c4c4 4px 8px);opacity:.5;box-shadow:none}',
    '.tz-pool{display:flex;flex-wrap:wrap;gap:2px}.tz-pool .tz-c{width:34px;height:34px}',
    '.tz-ok{color:var(--accent)}.tz-bad{color:var(--danger-text)}',
    '.tz-info{position:fixed;pointer-events:none;background:var(--panel);color:var(--text);border:1px solid var(--border-strong);padding:5px 8px;border-radius:4px;display:none;z-index:160;font-size:12px;box-shadow:0 3px 12px rgba(0,0,0,.25)}',
    '.tz-page.tz-b{position:fixed;inset:0;z-index:60;background:var(--bg);padding:3.4rem .6rem .6rem;overflow:auto}',
    '.tz-page.tz-b .tz-side{display:none}.tz-page.tz-b .tz-main{max-height:none}',
    '.tz-page.tz-b .tz-bar{position:fixed;left:50%;top:.4rem;transform:translateX(-50%);z-index:61;background:var(--panel);border:1px solid var(--accent);border-radius:8px;padding:.3rem .6rem;box-shadow:0 4px 18px rgba(0,0,0,.3);max-width:96vw;margin:0}',
    '.tz-mini{font-size:.8rem}',
    '.tz-wrap>.tz-zm{flex:1;min-width:0;margin:0}.tz-zm .tz-main{max-height:72vh}.tz-page.tz-b .tz-zm .tz-main{max-height:none}',
    /* all areas side by side */
    '.tz-all{display:flex;gap:18px;align-items:flex-start;width:max-content}.tz-area h3{margin:0 0 .3rem;font-size:.9rem}',
    '.tz-area{background:var(--panel);border:1px solid var(--border);border-radius:6px;padding:6px}',
    /* as built */
    '.tz-built{display:flex;flex-direction:column;gap:22px;width:max-content}.tz-bandh{font-size:.8rem;color:var(--muted);margin:0 0 .2rem}',
    '.tz-bg{display:grid;gap:2px;width:max-content}.tz-side-l{writing-mode:vertical-rl;transform:rotate(180deg);font-size:.75rem;font-weight:600;color:var(--muted);text-align:center;padding:2px}',
    '.tz-sign{height:calc(var(--z) * 1.6);width:var(--z);font-size:9px;line-height:1.05;writing-mode:vertical-rl;overflow:hidden;color:var(--text);background:var(--panel);border:1px solid var(--border);border-radius:2px;padding:1px;text-align:center;box-sizing:border-box}',
    '.tz-sign.tz-sa{transform:rotate(180deg)}.tz-line{height:calc(var(--z) / 2);background:#7a7a7a;border-radius:2px}',
    '.tz-note{font-size:.8rem;margin:.2rem 0 .5rem;max-width:70ch}',
    /* cell menu + change dialog */
    '.tz-menu{position:fixed;z-index:170;min-width:230px;background:var(--panel);color:var(--text);border:1px solid var(--border-strong,var(--border));border-radius:8px;box-shadow:var(--shadow-lg,0 6px 22px rgba(0,0,0,.3));padding:.3rem;font-size:.88rem}',
    '.tz-menu .tz-mh{font-size:.75rem;color:var(--muted);padding:.2rem .5rem .3rem;border-bottom:1px solid var(--border);margin-bottom:.2rem;word-break:break-all}',
    '.tz-menu button{display:flex;width:100%;align-items:center;gap:.5rem;text-align:left;font:inherit;background:none;border:0;color:inherit;padding:.38rem .55rem;border-radius:5px;cursor:pointer}',
    '.tz-menu button:hover,.tz-menu button:focus-visible{background:var(--panel-2);outline:none;box-shadow:inset 0 0 0 1px var(--accent)}.tz-menu button:disabled{opacity:.45;cursor:default}',
    '.tz-menu .tz-sw{width:12px;height:12px;border-radius:2px;flex:none;border:1px solid var(--border)}.tz-menu hr{border:0;border-top:1px solid var(--border);margin:.25rem 0}',
    '.tz-menu kbd{margin-left:auto;font-size:.72rem;color:var(--muted)}',
    '.tz-dlg{position:fixed;z-index:175;width:min(420px,94vw);background:var(--panel);color:var(--text);border:1px solid var(--accent);border-radius:10px;box-shadow:var(--shadow-lg,0 6px 22px rgba(0,0,0,.3));padding:.7rem}',
    '.tz-dlg h3{margin:0 0 .4rem;font-size:1rem}.tz-dlg input{width:100%;box-sizing:border-box}.tz-dlg ul{list-style:none;margin:.4rem 0;padding:0;max-height:260px;overflow:auto}',
    '.tz-dlg li{display:flex;align-items:center;gap:.5rem;padding:.25rem .35rem;border-radius:5px;cursor:pointer;font-size:.85rem}.tz-dlg li.on,.tz-dlg li:hover{background:var(--panel-2);box-shadow:inset 0 0 0 1px var(--accent)}',
    '.tz-dlg .tz-c{--z:30px;flex:none}.tz-dlg .tz-row{display:flex;gap:.4rem;justify-content:flex-end}.tz-dlg .tz-pv{display:flex;align-items:center;gap:.5rem;margin:.3rem 0;min-height:30px;font-size:.85rem}'
  ].join('\n');
  var PH = ['#grey', '#light', '#red', '#frame'];
  var ZBASE = 36, ZMIN = 18, ZMAX = 108;  // cell size in px: 100 % = 36, zoom 50 % .. 300 %
  var ALL = '*all';
  var ID_RE = /^(?:[a-z0-9_.-]{1,64}:)?[a-z0-9_./-]{1,128}$/;
  var LINE_AREAS = ['metals', 'wood', 'aging', 'rest', 'stone'];

  function esc(t) { return String(t == null ? '' : t).replace(/[&<>"']/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]; }); }
  function nsOf(id) { return id.indexOf(':') >= 0 ? id.split(':')[0] : 'minecraft'; }
  function pathOf(id) { return id.indexOf(':') >= 0 ? id.split(':')[1] : id; }
  function fmt(s, a) { return s.replace(/\{(\d)\}/g, function (m, i) { return a[+i]; }); }
  function isItem(v) { return !!v && v.charAt(0) !== '#'; }

  /* ---------- pure helpers (node test wiki/tests/tzeditor_core.cjs) ---------- */
  function placedCount(rows) { var m = {}; rows.forEach(function (r) { r.cells.forEach(function (c) { if (c && c.charAt(0) !== '#') m[c] = (m[c] || 0) + 1; }); }); return m; }
  function layoutOf(D, cols, rows) {
    return { schema: 1, variant: D.variant, area: D.area, columns: cols, excluded: D.excluded || [],
      rows: rows.map(function (r) { var o = { key: r.key, label: r.label, cells: r.cells }, t = {};
        Object.keys(r.tops || {}).forEach(function (c) { if (cols.indexOf(c) >= 0) t[c] = r.tops[c]; });
        if (Object.keys(t).length) o.tops = t; return o; }) };
  }
  function moveColumn(cols, rows, from, to) { var c = cols.splice(from, 1)[0]; cols.splice(to, 0, c); rows.forEach(function (r) { var x = r.cells.splice(from, 1)[0]; r.cells.splice(to, 0, x); }); }
  /* a name for a new row key / column that check_save accepts (NAME pattern, unique) */
  function uniqueName(taken, base) {
    var b = String(base || 'new').toLowerCase().replace(/[^a-z0-9_-]+/g, '_').replace(/^[^a-z0-9]+/, '').replace(/[_-]+$/, '').slice(0, 40) || 'new', n = b, i = 2;
    while (taken.indexOf(n) >= 0) n = b + '_' + (i++);
    return n;
  }
  /* area = { cols, rows }: an empty row before index at / an empty column before index at (same file format) */
  function insertRow(area, at, label) {
    var key = uniqueName(area.rows.map(function (r) { return r.key; }), label || 'new_row');
    area.rows.splice(Math.max(0, Math.min(at, area.rows.length)), 0, { key: key, label: label || key, cells: area.cols.map(function () { return null; }), tops: {} });
    return key;
  }
  function insertColumn(area, at, name) {
    var col = uniqueName(area.cols, name || 'new');
    at = Math.max(0, Math.min(at, area.cols.length));
    area.cols.splice(at, 0, col);
    area.rows.forEach(function (r) { r.cells.splice(at, 0, null); });
    return col;
  }
  /* the frame toggle: only on fields without an item (an item cell keeps its item) */
  function toggleFrame(v) { return isItem(v) ? v : (v === '#frame' ? null : '#frame'); }
  /* Undo over everything: each step holds the touched areas as they were before (several areas in the overview). */
  function History(limit) { this.past = []; this.future = []; this.limit = limit || 200; }
  function snapOf(a) { return JSON.stringify({ cols: a.cols, rows: a.rows }); }
  History.prototype.record = function (areas) { var s = {}; Object.keys(areas).forEach(function (f) { s[f] = snapOf(areas[f]); }); this.past.push(s); if (this.past.length > this.limit) this.past.shift(); this.future = []; };
  History.prototype.swap = function (from, to, all) {
    var s = from.pop(); if (!s) return null; var back = {};
    Object.keys(s).forEach(function (f) { var a = all[f]; if (!a) return; back[f] = snapOf(a); var o = JSON.parse(s[f]); a.cols = o.cols; a.rows = o.rows; });
    to.push(back); return Object.keys(s);
  };
  History.prototype.undo = function (all) { return this.swap(this.past, this.future, all); };
  History.prototype.redo = function (all) { return this.swap(this.future, this.past, all); };
  /* As built (OrderAreas.families, TZ38E-13): side A = metals, wood, aging, rest, then the last stone families; side B = the
     other stone rows; the cut sits on a family border and makes both sides as equal as possible; the rows of each side go
     into line pairs of perPair rows (OrderAreas.MAX_WIDTH - START_X). byArea: area -> [{ key, family, ... }]. */
  function builtPlan(byArea, perPair) {
    var sideA = [];
    ['metals', 'wood', 'aging', 'rest'].forEach(function (k) { sideA = sideA.concat(byArea[k] || []); });
    var stone = (byArea.stone || []).slice(), cut = stone.length, best = Infinity;
    for (var i = stone.length; i >= 0; i--) {
      if (i > 0 && i < stone.length && stone[i - 1].family === stone[i].family) continue;
      var diff = Math.abs(sideA.length + stone.length - i - i);
      if (diff < best) { best = diff; cut = i; }
    }
    var a = sideA.concat(stone.slice(cut)), b = stone.slice(0, cut), per = perPair || 114, bands = [];
    var pairs = Math.max(1, Math.max(Math.ceil(a.length / per), Math.ceil(b.length / per)));
    for (var p = 0; p < pairs; p++) bands.push({ a: a.slice(p * per, (p + 1) * per), b: b.slice(p * per, (p + 1) * per) });
    return { a: a, b: b, bands: bands, cut: cut };
  }
  /* "not placed": ids the game plan expects (per mod) without a field in the edited layout */
  function unplaced(expected, rows) {
    var placed = placedCount(rows), out = [];
    Object.keys(expected || {}).forEach(function (ns) { (expected[ns] || []).forEach(function (id) { if (!placed[id]) out.push(id); }); });
    return out;
  }

  function mount(el, ctx) {
    var lang = ctx.lang === 'de' ? 'de' : 'en', S = STR[lang];
    function tr(k) { return S[k] || STR.en[k] || k; }
    if (!ctx.dev) { el.innerHTML = '<div class="box box-danger">' + esc(tr('devOnly')) + '</div>'; return; }
    var st = ctx.state || (ctx.state = {});
    var defaults = { idx: null, file: '', areas: {}, plan: null, sel: {}, focus: null, hist: null, dirty: {}, view: 'a', layout: 'table', zoom: 36, q: '', msg: '', history: [], icons: null };
    Object.keys(defaults).forEach(function (k) { if (st[k] === undefined) st[k] = defaults[k]; });
    if (!(st.hist instanceof History)) st.hist = new History(200);
    var drag = null, zoom = null;
    api.dirtyNow = function () { return Object.keys(st.dirty).length > 0; };
    function $(s) { return el.querySelector(s); }
    function msg(t) { st.msg = t; var m = $('.tz-msg'); if (m) m.textContent = t; }
    function A(file) { return st.areas[file]; }
    function shown() {
      if (st.layout === 'built') return Object.keys(st.areas).filter(function (f) { return A(f).D.variant === 'lines'; });
      return st.file === ALL ? st.idx.areas.map(function (a) { return a.file; }).filter(A) : [st.file];
    }
    function areaMeta(file) { return st.idx.areas.filter(function (a) { return a.file === file; })[0] || {}; }
    function fileOf(variant, area) { var m = st.idx.areas.filter(function (a) { return a.variant === variant && a.area === area; })[0]; return m && m.file; }

    /* ---- edits: every change goes through edit() - one undo step over all touched areas, dirty flags, redraw ---- */
    function edit(files, fn) {
      var touched = {}; files.forEach(function (f) { if (A(f)) touched[f] = A(f); });
      if (!Object.keys(touched).length) return;
      st.hist.record(touched); fn(); Object.keys(touched).forEach(function (f) { st.dirty[f] = 1; }); draw();
    }
    function undo() { var f = st.hist.undo(st.areas); if (f) { f.forEach(function (x) { st.dirty[x] = 1; }); st.sel = {}; draw(); } }
    function redo() { var f = st.hist.redo(st.areas); if (f) { f.forEach(function (x) { st.dirty[x] = 1; }); st.sel = {}; draw(); } }
    function selList() { return Object.keys(st.sel).map(function (k) { var p = k.split('|'); return { f: p[0], r: +p[1], c: +p[2] }; }).filter(function (s) { return A(s.f) && A(s.f).rows[s.r]; }); }
    function setSel(v) {
      var list = selList(); if (!list.length) { msg(tr('pick')); return; }
      edit(list.map(function (s) { return s.f; }), function () { list.forEach(function (s) { var row = A(s.f).rows[s.r]; row.cells[s.c] = typeof v === 'function' ? v(row.cells[s.c]) : v; }); });
    }

    /* ---- pictures (N72) ----
       Candidates in order, each tried when the one before fails to load: 1. the module picture of index.json, 2.
       data/vanilla-icons.json (dev host only, wiki/vanilla_textures.py --icons: the game's item -> model -> texture walk),
       3. the wiki's picture of the id (ctx.icon), 4. the old guesses by name. "file#x,y,w,h" = crop of a skin. All failing: the name. */
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
    /* one cell; f/r/c = area file, row, column (pool and previews have no file) */
    function cellEl(id, f, r, c, opt) {
      opt = opt || {};
      var e = document.createElement('div'); e.className = 'tz-c'; e.dataset.f = f; e.dataset.r = r; e.dataset.c = c; e.dataset.id = id || '';
      if (opt.pool) e.dataset.pool = '1'; else if (opt.ro) { e.classList.add('tz-ro'); e.dataset.ro = '1'; } else e.draggable = true;
      if (!id) e.classList.add('tz-nil');
      else if (id.charAt(0) === '#') e.classList.add('tz-ph-' + id.slice(1));
      else { var i = img(id); if (i) e.appendChild(i); else name(e, id); }
      if (opt.top && id) { var t = img(opt.top, 'tz-t'); if (t) e.appendChild(t); }
      if (!opt.pool && !opt.ro && st.sel[f + '|' + r + '|' + c]) e.classList.add('sel');
      return e;
    }
    function topOf(a, r, c) { var row = a.rows[r]; return row && row.tops && row.tops[a.cols[c]]; }

    /* ---- shell ---- */
    function shell() {
      var h = '<div class="tz-page' + (st.view === 'b' ? ' tz-b' : '') + '" style="--z:' + st.zoom + 'px"><style>' + CSS + '</style>';
      h += '<h1>' + esc(tr('title')) + '</h1><p class="muted">' + esc(tr('intro')) + '</p>';
      h += '<div class="tz-bar"><label>' + esc(tr('area')) + ' <select data-k="area"><option value="' + ALL + '"' + (st.file === ALL ? ' selected' : '') + '>' + esc(tr('allAreas')) + '</option>' + st.idx.areas.map(function (a) {
        return '<option value="' + esc(a.file) + '"' + (a.file === st.file ? ' selected' : '') + '>' + esc(a.variant + ' / ' + a.name + ' (' + a.rows + ')') + '</option>'; }).join('') + '</select></label>';
      h += '<span class="muted tz-size"></span>';
      h += '<label>' + esc(tr('layout')) + ' <select data-k="layout"><option value="table"' + (st.layout === 'table' ? ' selected' : '') + '>' + esc(tr('table')) + '</option><option value="built"' + (st.layout === 'built' ? ' selected' : '') + '>' + esc(tr('built')) + '</option></select></label>';
      h += '<label>' + esc(tr('view')) + ' <select data-k="view"><option value="a"' + (st.view === 'a' ? ' selected' : '') + '>' + esc(tr('viewA')) + '</option><option value="b"' + (st.view === 'b' ? ' selected' : '') + '>' + esc(tr('viewB')) + '</option></select></label>';
      h += '<button type="button" class="btn" data-act="undo">' + esc(tr('undo')) + '</button><button type="button" class="btn" data-act="redo">' + esc(tr('redo')) + '</button>';
      PH.forEach(function (p) { h += '<button type="button" class="btn" data-ph="' + p + '">' + esc(tr(p.slice(1))) + '</button>'; });
      h += '<button type="button" class="btn" data-act="clear">' + esc(tr('clear')) + '</button>';
      h += '<label>' + esc(tr('zoom')) + ' <input type="range" min="' + ZMIN + '" max="' + ZMAX + '" value="' + st.zoom + '" data-k="zoom" style="width:80px"></label>';
      h += '<label>' + esc(tr('search')) + ' <input data-k="q" placeholder="' + esc(tr('searchPh')) + '" value="' + esc(st.q) + '"></label>';
      h += '<button type="button" class="btn btn-primary" data-act="save" title="' + esc(tr('saveTip')) + '">' + esc(tr('save')) + '</button>';
      h += '<button type="button" class="btn" data-act="dl">' + esc(tr('download')) + '</button><span class="tz-msg" role="status">' + esc(st.msg) + '</span><span class="tz-mini muted"></span></div>';
      h += '<div class="tz-wrap"><div class="tz-main" tabindex="0" aria-label="' + esc(tr('title')) + '"><div class="tz-body"></div></div><div class="tz-side box box-info">';
      h += '<h3>' + esc(tr('check')) + '</h3><div class="tz-chk"></div><div class="tz-plan"></div><h3>' + esc(tr('fresh')) + ' <span class="tz-ncnt"></span></h3><div class="tz-pool"></div>';
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

    /* ---- drawing ---- */
    var known = {};
    function decorate(e, id, pl, q) {
      if (id && id.charAt(0) !== '#') { if (pl[id] > 1) e.classList.add('dup'); else if (!known[id]) e.classList.add('unk'); }
      if (q) e.classList.add(id && id.toLowerCase().indexOf(q) >= 0 ? 'hit' : 'dim');
    }
    function tableEl(file, pl, q) {
      var a = A(file), g = document.createElement('div'); g.className = 'tz-grid'; g.dataset.f = file;
      g.style.gridTemplateColumns = '120px repeat(' + a.cols.length + ',var(--z))';
      g.appendChild(document.createElement('div'));
      a.cols.forEach(function (c, ci) { var h = document.createElement('div'); h.className = 'tz-h'; h.textContent = c; h.draggable = true; h.dataset.col = ci; h.dataset.f = file; g.appendChild(h); });
      a.rows.forEach(function (row, ri) {
        var l = document.createElement('div'); l.className = 'tz-r'; l.textContent = row.label; l.title = row.key; l.draggable = true; l.dataset.row = ri; l.dataset.f = file; g.appendChild(l);
        row.cells.forEach(function (id, ci) { var e = cellEl(id, file, ri, ci, { top: topOf(a, ri, ci) }); decorate(e, id, pl, q); g.appendChild(e); });
      });
      return g;
    }
    function rowLen(cells) { for (var i = cells.length - 1; i >= 0; i--) if (cells[i]) return i + 1; return 1; }
    /* "as built": every line row is a column of the picture, field 0 next to the path, side A above the grey line (going up),
       side B below (going down); empty fields inside a row are light grey placeholders in the game, filler beyond its end */
    function builtEl(pl, q) {
      var plan = st.plan && st.plan.lines, fam = (plan && plan.family) || {}, byArea = {};
      LINE_AREAS.forEach(function (k) { byArea[k] = []; });
      shown().forEach(function (f) {
        var a = A(f); if (!byArea[a.D.area]) return;
        a.rows.forEach(function (row, ri) { byArea[a.D.area].push({ key: row.key, label: row.label, family: fam[row.key] || row.key, f: f, r: ri, len: rowLen(row.cells) }); });
      });
      if (!fileOf('lines', 'rest') && plan && plan.rest) byArea.rest = plan.rest.map(function (row) { return { key: row.key, label: row.label, family: row.family || row.key, ro: row, len: row.cells.length }; });
      var bp = builtPlan(byArea, (plan && plan.perPair) || 114), out = document.createElement('div'); out.className = 'tz-built';
      var note = document.createElement('p'); note.className = 'muted tz-note'; note.textContent = tr('builtNote') + (plan ? '' : ' ' + tr('noPlan')); out.appendChild(note);
      bp.bands.forEach(function (band, bi) {
        var width = Math.max(1, band.a.length, band.b.length);
        var dA = Math.max.apply(null, [1].concat(band.a.map(function (x) { return x.len; })));
        var dB = Math.max.apply(null, [1].concat(band.b.map(function (x) { return x.len; })));
        var wrap = document.createElement('section'); wrap.className = 'tz-band';
        var hd = document.createElement('div'); hd.className = 'tz-bandh'; hd.textContent = fmt(tr('band'), [bi + 1]) + ' · ' + tr('sideA') + ' ' + band.a.length + ' · ' + tr('sideB') + ' ' + band.b.length; wrap.appendChild(hd);
        var g = document.createElement('div'); g.className = 'tz-bg';
        g.style.gridTemplateColumns = '22px repeat(' + width + ',var(--z))';
        function put(node, col, row) { node.style.gridColumn = String(col + 2); node.style.gridRow = String(row + 1); g.appendChild(node); }
        function label(text, row, span) { var s = document.createElement('div'); s.className = 'tz-side-l'; s.textContent = text; s.style.gridColumn = '1'; s.style.gridRow = (row + 1) + ' / span ' + span; g.appendChild(s); }
        label(tr('sideA'), 0, dA + 1); label(tr('sideB'), dA + 2, dB + 1);
        function side(list, isA) {
          for (var i = 0; i < width; i++) {
            var it = list[i];
            for (var col = 0; col < (isA ? dA : dB); col++) {
              var gridRow = isA ? dA - 1 - col : dA + 3 + col, e;
              if (!it || col >= it.len) { e = document.createElement('div'); e.className = 'tz-c tz-fill'; }
              else if (it.ro) { e = cellEl(it.ro.cells[col], '', 'ro', col, { ro: true }); e.title = tr('roCell'); }
              else { var a = A(it.f), id = a.rows[it.r].cells[col]; e = cellEl(id, it.f, it.r, col, { top: topOf(a, it.r, col) }); if (!id) e.classList.add('tz-ph-light'); decorate(e, id, pl, q); }
              put(e, i, gridRow);
            }
            var sg = document.createElement('div'); sg.className = 'tz-sign' + (isA ? ' tz-sa' : ''); sg.textContent = it ? it.label : ''; sg.title = it ? it.key + (it.ro ? ' - ' + tr('restRo') : '') : '';
            put(sg, i, isA ? dA : dA + 2);
          }
        }
        side(band.a, true); side(band.b, false);
        var ln = document.createElement('div'); ln.className = 'tz-line'; ln.title = tr('line'); ln.style.gridColumn = '2 / span ' + width; ln.style.gridRow = String(dA + 2); g.appendChild(ln);
        wrap.appendChild(g); out.appendChild(wrap);
      });
      return out;
    }

    function draw() {
      var body = $('.tz-body'); if (!body) return;
      var files = shown(), q = st.q.trim().toLowerCase(), rowsAll = [];
      known = {};
      files.forEach(function (f) { A(f).rows.forEach(function (r) { rowsAll.push(r); }); (A(f).known || []).forEach(function (k) { known[k] = 1; }); });
      var pl = placedCount(rowsAll);
      body.innerHTML = '';
      if (st.layout === 'built') body.appendChild(builtEl(pl, q));
      else if (st.file === ALL) {
        var all = document.createElement('div'); all.className = 'tz-all';
        files.forEach(function (f) { var box = document.createElement('section'); box.className = 'tz-area'; box.dataset.f = f; var m = areaMeta(f);
          var h3 = document.createElement('h3'); h3.textContent = m.variant + ' / ' + m.name + (st.dirty[f] ? ' *' : ''); box.appendChild(h3); box.appendChild(tableEl(f, pl, q)); all.appendChild(box); });
        body.appendChild(all);
      } else if (files[0] && A(files[0])) body.appendChild(tableEl(files[0], pl, q));
      drawSide(files, pl, q);
    }
    function drawSide(files, pl, q) {
      var unplacedIds = Object.keys(known).filter(function (k) { return !pl[k]; }).sort();
      var P = $('.tz-pool'); P.innerHTML = '';
      unplacedIds.forEach(function (id) { var e = cellEl(id, '', 'p', id, { pool: true }); if (q && id.indexOf(q) < 0) e.classList.add('dim'); P.appendChild(e); });
      $('.tz-ncnt').textContent = '(' + unplacedIds.length + ')';
      var dups = Object.keys(pl).filter(function (k) { return pl[k] > 1; }).map(function (k) { return k + ' x' + pl[k]; });
      var unk = Object.keys(pl).filter(function (k) { return !known[k]; });
      var reds = []; files.forEach(function (f) { var a = A(f); a.rows.forEach(function (r) { r.cells.forEach(function (c, i) { if (c === '#red') reds.push(r.label + ' / ' + a.cols[i]); }); }); });
      function sec(t, arr) { return '<div class="' + (arr.length ? 'tz-bad' : 'tz-ok') + '">' + esc(t) + ': ' + arr.length + '</div>' + (arr.length ? '<ul>' + arr.slice(0, 300).map(function (x) { return '<li>' + esc(x) + '</li>'; }).join('') + '</ul>' : ''); }
      $('.tz-chk').innerHTML = sec(tr('dups'), dups) + sec(tr('missing'), unplacedIds) + sec(tr('unknown'), unk) + sec(tr('reds'), reds);
      $('.tz-plan').innerHTML = planChecks();
      var ids = Object.keys(pl).length;
      $('.tz-size').textContent = files.length === 1 && st.layout !== 'built' && st.file !== ALL ? fmt(tr('size'), [A(files[0]).rows.length, A(files[0]).cols.length, ids]) : fmt(tr('sizeAll'), [files.length, ids]);
      var d = Object.keys(st.dirty).map(function (f) { var m = areaMeta(f); return m.variant + '/' + m.area; });
      $('.tz-mini').textContent = (d.length ? fmt(tr('dirty'), [d.join(', ')]) + ' ' : '') + (st.view === 'b' ? [tr('dups') + ': ' + dups.length, tr('missing') + ': ' + unplacedIds.length, tr('unknown') + ': ' + unk.length, tr('reds') + ': ' + reds.length].join(' | ') : '');
    }
    /* N72b-2: the game plan (integration dump, tools/tzedit/build_editor.py --plan) against the edited layouts, live */
    function planChecks() {
      var p = st.plan;
      if (!p) return '<p class="muted tiny">' + esc(tr('noPlan')) + '</p>';
      var h = '', cf = fileOf('blocks', 'compact');
      if (p.compact && cf && A(cf)) {
        var miss = unplaced(p.compact.expected, A(cf).rows);
        h += '<div class="' + (miss.length ? 'tz-bad' : 'tz-ok') + ' tz-cmp">' + esc(tr('compact')) + ': ' + miss.length + '</div>';
        h += miss.length ? '<ul class="tz-cmp-list">' + miss.slice(0, 400).map(function (x) { return '<li>' + esc(x) + '</li>'; }).join('') + '</ul>' : '<p class="muted tiny">' + esc(tr('compactOk')) + '</p>';
      }
      if (p.lines && p.lines.fresh) {
        var placed = {}; Object.keys(st.areas).forEach(function (f) { if (A(f).D.variant === 'lines') A(f).rows.forEach(function (r) { r.cells.forEach(function (c) { if (c) placed[c] = 1; }); Object.keys(r.tops || {}).forEach(function (k) { placed[r.tops[k]] = 1; }); }); });
        var fresh = p.lines.fresh.filter(function (id) { return !placed[id]; });
        h += '<div class="' + (fresh.length ? 'tz-bad' : 'tz-ok') + '">' + esc(tr('linesNew')) + ': ' + fresh.length + '</div>' + (fresh.length ? '<ul>' + fresh.map(function (x) { return '<li>' + esc(x) + '</li>'; }).join('') + '</ul>' : '');
      }
      if (p.exceptions && p.exceptions.length) {
        h += '<h3>' + esc(tr('exceptions')) + ' (' + p.exceptions.length + ')</h3><ul class="tz-exc">' + p.exceptions.map(function (x) { return '<li><b>' + esc(x.family || x.id) + '</b>' + (x.ids && x.ids.length > 1 ? ' (' + x.ids.length + ')' : '') + ': ' + esc(x.reason) + '</li>'; }).join('') + '</ul>';
      }
      return h + '<p class="muted tiny">' + esc(fmt(tr('planSrc'), [p.source || '?'])) + '</p>';
    }

    /* ---- cell menu (N72b-1): replaces the wiki menu inside the editor, mouse and keyboard ---- */
    function closeMenu() { var m = el.querySelector('.tz-menu'); if (m) m.remove(); }
    function openMenu(cell, x, y) {
      closeMenu(); closeDialog();
      var f = cell.dataset.f, r = +cell.dataset.r, c = +cell.dataset.c, k = f + '|' + r + '|' + c;
      if (!st.sel[k]) { st.sel = {}; st.sel[k] = 1; draw(); }
      st.focus = k;
      var v = A(f).rows[r].cells[c], item = isItem(v);
      var m = document.createElement('div'); m.className = 'tz-menu'; m.setAttribute('role', 'menu'); m.setAttribute('aria-label', tr('menu'));
      var items = [['change', tr('mChange'), null, 'Enter'], ['missing', tr('mMissing'), '#c33'], ['absent', tr('mAbsent'), '#dcdcdc'], ['clear', tr('mClear'), 'transparent', 'Del'],
        ['frame', tr('mFrame'), '#8b6b3d'], null, ['row', tr('mRow')], ['col', tr('mCol')], null, ['undo', tr('mUndo'), null, 'Ctrl+Z']];
      var h = '<div class="tz-mh">' + esc(v || '-') + ' · ' + esc(A(f).rows[r].label) + ' / ' + esc(A(f).cols[c]) + '</div>';
      items.forEach(function (it) {
        if (!it) { h += '<hr>'; return; }
        var dis = (it[0] === 'frame' && item && Object.keys(st.sel).length === 1) || (it[0] === 'undo' && !st.hist.past.length);
        h += '<button type="button" role="menuitem" data-m="' + it[0] + '"' + (dis ? ' disabled' : '') + (it[0] === 'frame' && dis ? ' title="' + esc(tr('mFrameItem')) + '"' : '') + '>'
          + '<span class="tz-sw" style="' + (it[2] ? 'background:' + it[2] : 'border:0') + '"></span>' + esc(it[1]) + (it[3] ? '<kbd>' + esc(it[3]) + '</kbd>' : '') + '</button>';
      });
      m.innerHTML = h;
      $('.tz-page').appendChild(m);
      var w = m.offsetWidth, hh = m.offsetHeight;
      m.style.left = Math.max(4, Math.min(x, window.innerWidth - w - 6)) + 'px'; m.style.top = Math.max(4, Math.min(y, window.innerHeight - hh - 6)) + 'px';
      var first = m.querySelector('button:not(:disabled)'); if (first) first.focus();
      m.addEventListener('keydown', function (e) {
        var bs = [].slice.call(m.querySelectorAll('button:not(:disabled)')), i = bs.indexOf(document.activeElement);
        if (e.key === 'ArrowDown' || e.key === 'ArrowUp') { e.preventDefault(); bs[(i + (e.key === 'ArrowDown' ? 1 : bs.length - 1)) % bs.length].focus(); }
        else if (e.key === 'Home') { e.preventDefault(); bs[0].focus(); } else if (e.key === 'End') { e.preventDefault(); bs[bs.length - 1].focus(); }
        else if (e.key === 'Escape' || e.key === 'Tab') { e.preventDefault(); e.stopPropagation(); closeMenu(); focusCell(); }
      });
      m.addEventListener('click', function (e) { var b = e.target.closest('[data-m]'); if (!b || b.disabled) return; closeMenu(); menuAct(b.dataset.m, f, r, c); });
    }
    function menuAct(act, f, r, c) {
      if (act === 'change') { changeDialog(); return; }
      if (act === 'missing') setSel('#red');
      else if (act === 'absent') setSel('#light');
      else if (act === 'clear') setSel(null);
      else if (act === 'frame') setSel(toggleFrame);
      else if (act === 'row') { var label = window.prompt(tr('newRowQ'), tr('newRow')); if (label === null) return; edit([f], function () { insertRow(A(f), r, label.trim() || tr('newRow')); }); st.sel = {}; draw(); }
      else if (act === 'col') { var nm = window.prompt(tr('newColQ'), tr('newCol')); if (nm === null) return; edit([f], function () { insertColumn(A(f), c, nm.trim() || 'new'); }); st.sel = {}; draw(); }
      else if (act === 'undo') undo();
      focusCell();
    }
    function closeDialog() { var d = el.querySelector('.tz-dlg'); if (d) d.remove(); }
    function idPool() {
      var seen = {};
      Object.keys((st.idx && st.idx.textures) || {}).forEach(function (k) { seen[k] = 1; });
      Object.keys(st.icons || {}).forEach(function (k) { seen[k] = 1; });
      Object.keys(st.areas).forEach(function (f) { A(f).rows.forEach(function (r) { r.cells.forEach(function (c) { if (isItem(c)) seen[c] = 1; }); }); });
      return Object.keys(seen).sort();
    }
    /* "Block ändern …": search field with picture preview; any id (mod:name) can be set */
    function changeDialog() {
      var list = selList(); if (!list.length) { msg(tr('pick')); return; }
      closeDialog();
      var pool = idPool(), d = document.createElement('div'); d.className = 'tz-dlg'; d.setAttribute('role', 'dialog'); d.setAttribute('aria-label', tr('chTitle'));
      var cur = A(list[0].f).rows[list[0].r].cells[list[0].c];
      d.innerHTML = '<h3>' + esc(tr('chTitle')) + '</h3><input type="text" class="tz-chq" autocomplete="off" spellcheck="false" placeholder="' + esc(tr('chPh')) + '" value="' + esc(isItem(cur) ? cur : '') + '">'
        + '<div class="tz-pv"></div><ul class="tz-chl" role="listbox"></ul><p class="muted tiny tz-chm">' + esc(tr('chAny')) + '</p>'
        + '<div class="tz-row"><button type="button" class="btn" data-d="cancel">' + esc(tr('chCancel')) + '</button><button type="button" class="btn btn-primary" data-d="set">' + esc(tr('chSet')) + '</button></div>';
      $('.tz-page').appendChild(d);
      d.style.left = Math.max(8, (window.innerWidth - d.offsetWidth) / 2) + 'px'; d.style.top = Math.max(8, window.innerHeight * 0.18) + 'px';
      var input = d.querySelector('.tz-chq'), ul = d.querySelector('.tz-chl'), pv = d.querySelector('.tz-pv'), on = -1, hits = [], typed = false;
      function norm(v) { v = v.trim().toLowerCase(); return v && v.indexOf(':') < 0 && ID_RE.test(v) ? 'minecraft:' + v : v; }
      function preview(v) { pv.innerHTML = ''; if (v && ID_RE.test(v)) { pv.appendChild(cellEl(v, '', 'v', 0, { pool: true })); pv.appendChild(document.createTextNode(v)); } }
      function fill() {
        var raw = input.value.trim().toLowerCase(), words = raw.split(/[\s_:]+/).filter(Boolean);
        hits = !words.length ? [] : pool.filter(function (id) { return words.every(function (w) { return id.indexOf(w) >= 0; }); }).slice(0, 60);
        ul.innerHTML = ''; on = typed && hits.length ? 0 : -1;
        hits.forEach(function (id, i) { var li = document.createElement('li'); li.setAttribute('role', 'option'); li.dataset.id = id; if (i === on) li.className = 'on'; li.appendChild(cellEl(id, '', 'v', i, { pool: true })); li.appendChild(document.createTextNode(id)); ul.appendChild(li); });
        preview(on >= 0 ? hits[on] : norm(input.value));
      }
      function mark() { [].forEach.call(ul.children, function (li, i) { li.classList.toggle('on', i === on); if (i === on) li.scrollIntoView({ block: 'nearest' }); }); preview(hits[on] || norm(input.value)); }
      function choice() { return on >= 0 && hits[on] ? hits[on] : norm(input.value); }
      function apply(v) {
        v = norm(v || '');
        if (!ID_RE.test(v)) { d.querySelector('.tz-chm').textContent = tr('chBad') + v; return; }
        closeDialog(); setSel(v); focusCell();
      }
      input.addEventListener('input', function () { typed = true; fill(); });
      input.addEventListener('keydown', function (e) {
        if (e.key === 'ArrowDown' && hits.length) { e.preventDefault(); on = (on + 1) % hits.length; mark(); }
        else if (e.key === 'ArrowUp' && hits.length) { e.preventDefault(); on = (on + hits.length - 1) % hits.length; mark(); }
        else if (e.key === 'Enter') { e.preventDefault(); apply(choice()); }
        else if (e.key === 'Escape') { e.preventDefault(); e.stopPropagation(); closeDialog(); focusCell(); }
      });
      ul.addEventListener('click', function (e) { var li = e.target.closest('li'); if (li) apply(li.dataset.id); });
      d.addEventListener('click', function (e) { var b = e.target.closest('[data-d]'); if (!b) return; if (b.dataset.d === 'set') apply(choice()); else { closeDialog(); focusCell(); } });
      fill(); input.focus(); input.select();
    }
    function cellNode(k) { if (!k) return null; var p = k.split('|'); return el.querySelector('.tz-body .tz-c[data-f="' + p[0] + '"][data-r="' + p[1] + '"][data-c="' + p[2] + '"]'); }
    function focusCell() { var main = $('.tz-main'); if (main) main.focus({ preventScroll: true }); var n = cellNode(st.focus); if (n) n.scrollIntoView({ block: 'nearest', inline: 'nearest' }); }
    /* keyboard: arrows move the (single) selection inside an area table, Shift+F10 / context menu key opens the menu */
    function moveFocus(dr, dc) {
      var k = st.focus || Object.keys(st.sel)[0];
      if (!k || !A(k.split('|')[0])) { var f0 = shown()[0]; if (!f0 || !A(f0) || !A(f0).rows.length) return; k = f0 + '|0|0'; dr = 0; dc = 0; }
      var p = k.split('|'), a = A(p[0]);
      var r = Math.max(0, Math.min(a.rows.length - 1, +p[1] + dr)), c = Math.max(0, Math.min(a.cols.length - 1, +p[2] + dc));
      st.focus = p[0] + '|' + r + '|' + c; st.sel = {}; st.sel[st.focus] = 1; draw(); focusCell();
    }

    keys = function (e) {
      if (el.querySelector('.tz-dlg') || (e.target && e.target.closest && e.target.closest('.tz-menu'))) return;
      if (e.target && /INPUT|SELECT|TEXTAREA/.test(e.target.tagName)) return;
      var inGrid = e.target && e.target.closest && e.target.closest('.tz-main');
      if (e.key === 'Delete') setSel(null);
      else if (e.ctrlKey && e.key === 'z') { e.preventDefault(); undo(); }
      else if (e.ctrlKey && e.key === 'y') { e.preventDefault(); redo(); }
      else if (inGrid && /^Arrow/.test(e.key) && !e.ctrlKey && !e.altKey) { e.preventDefault(); moveFocus(e.key === 'ArrowUp' ? -1 : e.key === 'ArrowDown' ? 1 : 0, e.key === 'ArrowLeft' ? -1 : e.key === 'ArrowRight' ? 1 : 0); }
      else if (inGrid && (e.key === 'ContextMenu' || (e.shiftKey && e.key === 'F10') || e.key === 'Enter')) {
        var n = cellNode(st.focus || Object.keys(st.sel)[0]); if (!n) { moveFocus(0, 0); n = cellNode(st.focus); }
        if (!n) return;
        e.preventDefault();
        st.focus = n.dataset.f + '|' + n.dataset.r + '|' + n.dataset.c;
        if (e.key === 'Enter') { if (!st.sel[st.focus]) { st.sel = {}; st.sel[st.focus] = 1; } changeDialog(); }
        else { var rc = n.getBoundingClientRect(); openMenu(n, rc.left + rc.width / 2, rc.bottom); }
      }
    };

    function wire() {
      var page = $('.tz-page'), info = $('.tz-info');
      page.addEventListener('change', function (e) {
        var k = e.target.dataset.k;
        if (k === 'area') { if (e.target.value !== ALL && Object.keys(st.dirty).length && !window.confirm(tr('discard'))) { e.target.value = st.file; return; } choose(e.target.value); }
        else if (k === 'view') { st.view = e.target.value; shell(); }
        else if (k === 'layout') { st.layout = e.target.value; if (st.layout === 'built') loadMany(st.idx.areas.filter(function (a) { return a.variant === 'lines'; }).map(function (a) { return a.file; }), shell); else shell(); }
      });
      page.addEventListener('input', function (e) {
        var k = e.target.dataset.k;
        if (k === 'zoom') { if (zoom) zoom.setZoom(+e.target.value / ZBASE); else { st.zoom = +e.target.value; page.style.setProperty('--z', st.zoom + 'px'); } }
        else if (k === 'q') { st.q = e.target.value; draw(); }
      });
      page.addEventListener('click', function (e) {
        if (!e.target.closest('.tz-menu')) closeMenu();
        var b = e.target.closest('[data-act],[data-ph]');
        if (b) {
          var a = b.dataset.act;
          if (b.dataset.ph) setSel(b.dataset.ph);
          else if (a === 'undo') undo();
          else if (a === 'redo') redo();
          else if (a === 'clear') setSel(null);
          else if (a === 'unk') { var v = $('.tz-unk').value.trim(); if (v && Object.keys(st.sel).length) setSel(v); else msg(tr('pick')); }
          else if (a === 'dl') download();
          else if (a === 'save') save();
          return;
        }
        var t = e.target.closest('.tz-body .tz-c');
        if (!t || t.dataset.ro || !t.dataset.f || !A(t.dataset.f)) return;
        var k = t.dataset.f + '|' + t.dataset.r + '|' + t.dataset.c;
        if (e.ctrlKey || e.shiftKey || e.metaKey) { if (st.sel[k]) delete st.sel[k]; else st.sel[k] = 1; }
        else { var only = Object.keys(st.sel).length === 1 && st.sel[k]; st.sel = {}; if (!only) st.sel[k] = 1; }
        st.focus = k;
        draw();
      });
      /* N72b-1: right-click on a cell opens the editor menu (index.html leaves events the editor took alone) */
      page.addEventListener('contextmenu', function (e) {
        var t = e.target.closest('.tz-body .tz-c');
        if (!t || t.dataset.ro || !t.dataset.f || !A(t.dataset.f)) return;
        e.preventDefault();
        openMenu(t, e.clientX, e.clientY);
      });
      page.addEventListener('dragstart', function (e) {
        var t = e.target.closest('.tz-c,.tz-h,.tz-r'); if (!t || t.dataset.ro) return;
        if (t.dataset.col != null) drag = { t: 'col', f: t.dataset.f, i: +t.dataset.col };
        else if (t.dataset.row != null) drag = { t: 'row', f: t.dataset.f, i: +t.dataset.row };
        else if (t.dataset.pool) drag = { t: 'pool', id: t.dataset.id };
        else if (t.dataset.f && A(t.dataset.f)) { var k = t.dataset.f + '|' + t.dataset.r + '|' + t.dataset.c; if (!st.sel[k]) { st.sel = {}; st.sel[k] = 1; } drag = { t: 'cell', f: t.dataset.f, r: +t.dataset.r, c: +t.dataset.c }; }
        e.dataTransfer.setData('text/plain', 'x');
      });
      page.addEventListener('dragover', function (e) { if (drag && e.target.closest('.tz-c,.tz-h,.tz-r')) e.preventDefault(); });
      page.addEventListener('drop', function (e) {
        var t = e.target.closest('.tz-c,.tz-h,.tz-r'); if (!t || !drag || t.dataset.ro) return; e.preventDefault();
        var d = drag; drag = null;
        if (d.t === 'col' && t.dataset.col != null && t.dataset.f === d.f) edit([d.f], function () { moveColumn(A(d.f).cols, A(d.f).rows, d.i, +t.dataset.col); });
        else if (d.t === 'row' && t.dataset.row != null && t.dataset.f === d.f) edit([d.f], function () { var rs = A(d.f).rows, r = rs.splice(d.i, 1)[0]; rs.splice(+t.dataset.row, 0, r); });
        else if (t.classList.contains('tz-c') && !t.dataset.pool && t.dataset.f && A(t.dataset.f)) {
          var f = t.dataset.f, ri = +t.dataset.r, ci = +t.dataset.c;
          if (d.t === 'pool') edit([f], function () { A(f).rows[ri].cells[ci] = d.id; });
          else if (d.t === 'cell') edit([d.f, f], function () { var a = A(d.f).rows[d.r].cells, bb = A(f).rows[ri].cells, x = a[d.c]; a[d.c] = bb[ci]; bb[ci] = x; });
        }
        st.sel = {}; draw();
      });
      page.addEventListener('mousemove', function (e) {
        var t = e.target.closest('.tz-c');
        if (!t || !t.dataset.id || t.dataset.id.charAt(0) === '#' || el.querySelector('.tz-menu,.tz-dlg')) { info.style.display = 'none'; return; }
        var id = t.dataset.id, a = t.dataset.f && A(t.dataset.f), row = a && !t.dataset.pool ? a.rows[t.dataset.r] : null, top = row && row.tops && row.tops[a.cols[t.dataset.c]];
        info.innerHTML = '<b>' + esc(id) + '</b><br>' + esc(tr('mod')) + ': ' + esc(nsOf(id)) + '<br>' + esc(tr('row')) + ': ' + esc(row ? row.label + ' (' + row.key + ')' : t.dataset.ro ? tr('roCell') : tr('isNew'))
          + (row ? '<br>' + esc(tr('column')) + ': ' + esc(a.cols[t.dataset.c]) : '') + (top ? '<br>' + esc(tr('top')) + ': ' + esc(top) : '');
        info.style.display = 'block'; info.style.left = e.clientX + 14 + 'px'; info.style.top = e.clientY + 14 + 'px';
      });
      page.addEventListener('mouseleave', function () { info.style.display = 'none'; });
    }

    function payload(f) { var a = A(f); return layoutOf(a.D, a.cols, a.rows); }
    function targets() { return st.file === ALL || st.layout === 'built' ? (Object.keys(st.dirty).length ? Object.keys(st.dirty) : shown()) : [st.file]; }
    function download() {
      targets().forEach(function (f) {
        var a = document.createElement('a'), D = A(f).D;
        a.href = URL.createObjectURL(new Blob([JSON.stringify(payload(f), null, 1)], { type: 'application/json' }));
        a.download = 'layout_' + D.variant + '_' + D.area + '.json'; a.click();
      });
      msg(tr('downloaded'));
    }
    function save() {
      var files = targets();
      var comment = window.prompt(tr('commentQ'), ''); if (comment === null) return;
      var b = $('[data-act=save]'); b.disabled = true; msg(tr('saving'));
      var done = [], chain = Promise.resolve();
      files.forEach(function (f) {
        chain = chain.then(function () {
          return fetch('/api/testcentre', { method: 'POST', credentials: 'same-origin', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(Object.assign(payload(f), { comment: comment })) })
            .then(function (r) { return r.json().catch(function () { return {}; }).then(function (j) {
              if (!r.ok) { var err = new Error((j.error || r.status) + (r.status === 401 || r.status === 403 ? tr('login') : '')); err.server = true; throw err; }
              done.push(j.sid); delete st.dirty[f];
            }); });
        });
      });
      chain.then(function () { msg(fmt(tr('okSave'), [done.join(', ')])); loadHistory(); draw(); })
        .catch(function (e) { msg(e && e.server ? tr('badSave') + e.message : tr('offline')); })
        .then(function () { var bb = $('[data-act=save]'); if (bb) bb.disabled = false; });
    }
    function drawHistory() {
      var u = $('.tz-hist'); if (!u) return;
      var areas = shown().filter(A).map(function (f) { return A(f).D; });
      var mine = st.history.filter(function (x) { return areas.some(function (D) { return x.area === D.area && (!x.variant || x.variant === D.variant); }); }).slice(0, 10);
      u.innerHTML = mine.map(function (x) { return '<li>' + esc(new Date(x.createdAt).toLocaleString()) + ' · ' + esc(x.by || '?') + ' · ' + esc(x.variant ? x.variant + '/' : '') + esc(x.area) + ' · ' + esc(x.state) + (x.comment ? ' · ' + esc(x.comment) : '') + '</li>'; }).join('') || '<li>' + esc(tr('none')) + '</li>';
    }
    function loadHistory() {
      fetch('/api/testcentre', { credentials: 'same-origin' }).then(function (r) { return r.ok ? r.json() : []; })
        .then(function (a) { st.history = Array.isArray(a) ? a : []; drawHistory(); }).catch(function () {});
    }
    function loadOne(file) {
      if (A(file)) return Promise.resolve(A(file));
      return fetch('data/testcentre/' + file, { cache: 'no-cache' }).then(function (r) { if (!r.ok) throw new Error(file + ' ' + r.status); return r.json(); }).then(function (D) {
        var a = { file: file, D: D, cols: D.columns.slice(), rows: D.rows.map(function (r) { return { key: r.key, label: r.label, cells: r.cells.slice(), tops: Object.assign({}, r.tops || {}) }; }) };
        var k = {}; a.rows.forEach(function (r) { r.cells.forEach(function (c) { if (c && c.charAt(0) !== '#') k[c] = 1; }); }); a.known = Object.keys(k);
        st.areas[file] = a; return a;
      });
    }
    function loadMany(files, then) { Promise.all(files.map(loadOne)).then(then).catch(function (e) { msg(tr('areaFail') + (e && e.message)); }); }
    function choose(file) {
      if (file !== ALL && Object.keys(st.dirty).length) { st.areas = {}; st.dirty = {}; st.hist = new History(200); }
      var files = file === ALL || st.layout === 'built' ? st.idx.areas.map(function (a) { return a.file; }) : [file];
      loadMany(files, function () { st.file = file; st.sel = {}; st.focus = null; st.msg = ''; if (ctx.onArea) ctx.onArea(file); shell(); });
    }

    if (st.idx && st.file && Object.keys(st.areas).length) { shell(); return; }
    el.innerHTML = '<h1>' + esc(tr('title')) + '</h1><p class="hint">' + esc(tr('loading')) + '</p>';
    /* the id -> picture list exists only on the dev host (git-ignored); the game plan is committed (names only) */
    function optional(url) { return fetch(url, { cache: 'no-cache' }).then(function (r) { return r.ok ? r.json() : null; }).catch(function () { return null; }); }
    Promise.all([fetch('data/testcentre/index.json', { cache: 'no-cache' }).then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); }),
      st.icons ? Promise.resolve({ icons: st.icons }) : optional('data/vanilla-icons.json'), optional('data/testcentre/plan.json')]).then(function (got) {
      st.idx = got[0]; st.icons = (got[1] && got[1].icons) || {}; st.plan = got[2];
      var want = ctx.area === ALL ? ALL : ctx.area && (st.idx.areas.filter(function (a) { return a.file === ctx.area; })[0] || {}).file;
      choose(want || st.idx.areas[0].file);
      loadHistory();
    }).catch(function () { el.innerHTML = '<h1>' + esc(tr('title')) + '</h1><div class="box box-danger">' + esc(tr('noData')) + '</div>'; });
  }

  /* One document listener for Delete / Ctrl+Z / Ctrl+Y / arrows; it acts on the mounted editor only while its page is shown. */
  var keys = null;
  if (typeof document !== 'undefined') document.addEventListener('keydown', function (e) { if (keys && document.querySelector('.tz-page')) keys(e); });
  if (typeof window !== 'undefined') window.addEventListener('beforeunload', function (e) { var p = document.querySelector('.tz-page'); if (p && api.dirtyNow && api.dirtyNow()) { e.preventDefault(); e.returnValue = ''; } });

  var api = { mount: mount, STR: STR, placedCount: placedCount, layoutOf: layoutOf, moveColumn: moveColumn, uniqueName: uniqueName,
    insertRow: insertRow, insertColumn: insertColumn, toggleFrame: toggleFrame, History: History, builtPlan: builtPlan, unplaced: unplaced,
    dirty: function (ctx) { return !!(ctx && ctx.state && ctx.state.dirty && Object.keys(ctx.state.dirty).length); } };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.SBTzEditor = api;
})(typeof self !== 'undefined' ? self : this);
