/* Wiki pixel editor with layers (dev mirror only; opened by tools/zentrale-web/dev-overlay.js).
 * Functions and UX after the Resprite analysis (docs/ai/ANALYSE-RESPRITE-2026-10-21.md), own code; layout, icons and
 * comfort after docs/ai/PLAN-B-PIXELEDITOR2-2026-10-21.md. Needs window.SBPixelCore (assets/pixeleditor-core.js).
 * The wiki is static: results are downloaded (<name>.png + <name>.layers.json), a dev puts both next to the original
 * and runs tools/textures/layers.py check. In the dev wiki (window.sbDevHook) "Save to repo" posts both to /api/textures
 * (TX22-5, tools/textures/editor_save.py): the pickup commits them on a branch tex-<name>-<id> for the next bundle.
 *
 * window.SBPixelEditor.open({id, name, png, layers, source, model}) - png/layers: URLs (layers optional). */
(function () {
  'use strict';
  if (window.SBPixelEditor) return;
  var C = window.SBPixelCore;

  /* ---------------- texts (EN/DE like the wiki); tool/button names without keys, keys come from KEYS */
  var STR = {
    en: {
      title: 'Pixel editor', undo: 'Undo', redo: 'Redo', grid: 'Pixel grid', symX: 'Mirror left/right', symY: 'Mirror top/bottom',
      pp: 'Pixel perfect', open: 'Open file', exp: 'Export', close: 'Close', zoomIn: 'Zoom in', zoomOut: 'Zoom out', fit: 'Fit to view',
      pencil: 'Pencil', eraser: 'Eraser', fill: 'Fill', picker: 'Eyedropper', line: 'Line', rect: 'Rectangle', select: 'Select', move: 'Move',
      tools: 'Tools', gDraw: 'Draw', gShape: 'Shapes', gSel: 'Selection',
      size: 'Brush size', filled: 'Filled', global: 'Same colour everywhere',
      hintPencil: 'Shift+click: straight line from the last point · Alt: eyedropper', hintFill: 'Shift: same colour everywhere',
      hintPicker: 'Click picks the colour you see (all layers).', hintSel: 'Drag a rectangle; actions work on the selection or the whole layer.',
      hintMove: 'Drag or use the arrow keys; without a selection the whole layer moves.', hintShape: 'Drag from start to end.',
      colors: 'Colours', primary: 'Primary colour (left click)', secondary: 'Secondary colour (right click)', swap: 'Swap colours',
      pickColor: 'Pick a colour', alpha: 'Opacity', recent: 'Recently used', noRecent: 'Colours you paint with show up here.',
      palTex: 'This texture', palVan: 'Vanilla-near', gpl: 'Palette (.gpl)', palHint: 'Click: primary · right click: secondary',
      layers: 'Layers', add: 'New layer', dup: 'Duplicate layer', del: 'Delete layer', up: 'Move layer up', down: 'Move layer down', merge: 'Merge down',
      rename: 'Rename layer', lock: 'Alpha lock', unlock: 'Alpha lock off', opacity: 'Layer opacity', show: 'Hide layer', hide: 'Show layer', layerName: 'Layer name',
      drag: 'Drag to reorder (Alt+↑/↓)', layersHint: 'Top of the list = top layer. Double click or F2: rename.',
      region: 'Selection / layer', flipH: 'Flip horizontally', flipV: 'Flip vertically', rot: 'Rotate 90°', clear: 'Clear', copy: 'Copy', paste: 'Paste', deselect: 'Deselect', selAll: 'Select all',
      preview: 'Preview', p1: '1:1', p2: '2:1', tile: 'Tiled 3×3', ref: 'Reference image', refAdd: 'Choose image', refPick: 'Click the image to pick a colour.',
      both: 'PNG + layers', png: 'PNG only', json: 'Layers only (.layers.json)', more: 'More',
      howto: 'Put {png} and {json} next to the original ({src}), then run python tools/textures/layers.py check and python wiki/generate.py --all.',
      derived: 'Vanilla-derived texture: never commit it (A8). Edit tools/textures/vanilla_derived.py instead.',
      save: 'Save to repo', saveTip: 'Saves PNG + layers on a branch tex-… (taken over with the next bundle)', saveComment: 'Comment for the commit (optional)',
      saving: 'Saving…', saved: 'Saved.', saveFail: 'Saving failed: ',
      st_new: 'Saved – waiting for the pickup.', st_picked: 'Picked up – being committed.', st_ready: 'On branch {b} – goes in with the next bundle.', st_failed: 'Not taken over: {n}',
      loading: 'Loading the texture…', loadFail: 'The texture could not be loaded.', blank: 'Empty 16×16 texture', hidden: 'Active layer is hidden',
      dirty: 'Discard your changes? They are not exported yet.', discard: 'Discard', cancel: 'Cancel', ok: 'OK',
      fileFail: 'The file could not be opened: ', rotSquare: 'Rotating needs a square area.', nothing: 'Nothing copied yet.', copied: 'Copied.',
      keysTitle: 'Keyboard shortcuts', fromLayers: 'Loaded from the layer source.', fromPng: 'No layer source yet: the PNG is one layer.',
      view: 'View', base: 'Base', pasted: 'Pasted', layer: 'Layer', closeSheet: 'Close panel', status: 'Status',
      k: [['B E G I L U M V', 'tools'], ['Alt (hold)', 'eyedropper'], ['Shift+click', 'straight line'], ['X', 'swap colours'], ['[ ]', 'brush size'],
        ['Ctrl+Z / Ctrl+Y', 'undo / redo'], ['+ − 0', 'zoom, fit'], ["Ctrl+'", 'grid'], ['Shift+N', 'new layer'], ['Shift+↑/↓', 'active layer'],
        ['Ctrl+A / Ctrl+D', 'select all / deselect'], ['Ctrl+C / Ctrl+V', 'copy / paste'], ['Arrows', 'move selection'], ['Del', 'clear'],
        ['Space+drag, middle mouse', 'pan'], ['Two fingers', 'zoom/pan; tap = undo, three = redo'], ['Long press', 'eyedropper'], ['Esc', 'close panel / deselect / close']]
    },
    de: {
      title: 'Pixeleditor', undo: 'Rückgängig', redo: 'Wiederholen', grid: 'Pixelraster', symX: 'Spiegeln links/rechts', symY: 'Spiegeln oben/unten',
      pp: 'Pixel Perfect', open: 'Datei öffnen', exp: 'Export', close: 'Schließen', zoomIn: 'Vergrößern', zoomOut: 'Verkleinern', fit: 'Einpassen',
      pencil: 'Stift', eraser: 'Radierer', fill: 'Füllen', picker: 'Pipette', line: 'Linie', rect: 'Rechteck', select: 'Auswahl', move: 'Verschieben',
      tools: 'Werkzeuge', gDraw: 'Zeichnen', gShape: 'Formen', gSel: 'Auswahl',
      size: 'Pinselgröße', filled: 'Gefüllt', global: 'Gleiche Farbe überall',
      hintPencil: 'Umschalt+Klick: gerade Linie vom letzten Punkt · Alt: Pipette', hintFill: 'Umschalt: gleiche Farbe überall',
      hintPicker: 'Klick nimmt die sichtbare Farbe auf (alle Ebenen).', hintSel: 'Rechteck ziehen; Aktionen wirken auf die Auswahl oder die ganze Ebene.',
      hintMove: 'Ziehen oder Pfeiltasten; ohne Auswahl wandert die ganze Ebene.', hintShape: 'Vom Anfang zum Ende ziehen.',
      colors: 'Farben', primary: 'Primärfarbe (Linksklick)', secondary: 'Sekundärfarbe (Rechtsklick)', swap: 'Farben tauschen',
      pickColor: 'Farbe wählen', alpha: 'Deckung', recent: 'Zuletzt benutzt', noRecent: 'Farben, mit denen du malst, erscheinen hier.',
      palTex: 'Diese Textur', palVan: 'Vanilla-nah', gpl: 'Palette (.gpl)', palHint: 'Klick: Primär · Rechtsklick: Sekundär',
      layers: 'Ebenen', add: 'Neue Ebene', dup: 'Ebene duplizieren', del: 'Ebene löschen', up: 'Ebene nach oben', down: 'Ebene nach unten', merge: 'Nach unten zusammenführen',
      rename: 'Ebene umbenennen', lock: 'Alpha-Sperre', unlock: 'Alpha-Sperre aus', opacity: 'Deckkraft der Ebene', show: 'Ebene ausblenden', hide: 'Ebene einblenden', layerName: 'Ebenen-Name',
      drag: 'Ziehen zum Sortieren (Alt+↑/↓)', layersHint: 'Oben in der Liste = oberste Ebene. Doppelklick oder F2: umbenennen.',
      region: 'Auswahl / Ebene', flipH: 'Waagerecht spiegeln', flipV: 'Senkrecht spiegeln', rot: '90° drehen', clear: 'Leeren', copy: 'Kopieren', paste: 'Einfügen', deselect: 'Auswahl aufheben', selAll: 'Alles auswählen',
      preview: 'Vorschau', p1: '1:1', p2: '2:1', tile: 'Kachel 3×3', ref: 'Referenzbild', refAdd: 'Bild wählen', refPick: 'Bild anklicken = Farbe aufnehmen.',
      both: 'PNG + Ebenen', png: 'Nur PNG', json: 'Nur Ebenen (.layers.json)', more: 'Mehr',
      howto: '{png} und {json} neben das Original legen ({src}), dann python tools/textures/layers.py check und python wiki/generate.py --all.',
      derived: 'Vanilla-abgeleitete Textur: nie einchecken (A8), stattdessen tools/textures/vanilla_derived.py anpassen.',
      save: 'Speichern ins Repo', saveTip: 'Speichert PNG + Ebenen auf einem Branch tex-… (Übernahme mit dem nächsten Bündel)', saveComment: 'Kommentar zum Commit (optional)',
      saving: 'Wird gespeichert…', saved: 'Gespeichert.', saveFail: 'Speichern fehlgeschlagen: ',
      st_new: 'Gespeichert – wartet auf die Abholung.', st_picked: 'Abgeholt – wird committet.', st_ready: 'Auf Branch {b} – kommt mit dem nächsten Bündel.', st_failed: 'Nicht übernommen: {n}',
      loading: 'Textur wird geladen…', loadFail: 'Die Textur ließ sich nicht laden.', blank: 'Leere 16×16-Textur', hidden: 'Aktive Ebene ist ausgeblendet',
      dirty: 'Änderungen verwerfen? Sie sind noch nicht exportiert.', discard: 'Verwerfen', cancel: 'Abbrechen', ok: 'OK',
      fileFail: 'Datei nicht lesbar: ', rotSquare: 'Drehen braucht eine quadratische Fläche.', nothing: 'Noch nichts kopiert.', copied: 'Kopiert.',
      keysTitle: 'Tastenkürzel', fromLayers: 'Aus der Ebenen-Quelle geladen.', fromPng: 'Noch keine Ebenen-Quelle: das PNG ist eine Ebene.',
      view: 'Ansicht', base: 'Grundform', pasted: 'Eingefügt', layer: 'Ebene', closeSheet: 'Paneel schließen', status: 'Status',
      k: [['B E G I L U M V', 'Werkzeuge'], ['Alt (halten)', 'Pipette'], ['Umschalt+Klick', 'gerade Linie'], ['X', 'Farben tauschen'], ['[ ]', 'Pinselgröße'],
        ['Strg+Z / Strg+Y', 'rückgängig / wiederholen'], ['+ − 0', 'Zoom, einpassen'], ["Strg+'", 'Raster'], ['Umschalt+N', 'neue Ebene'], ['Umschalt+↑/↓', 'aktive Ebene'],
        ['Strg+A / Strg+D', 'alles / nichts auswählen'], ['Strg+C / Strg+V', 'kopieren / einfügen'], ['Pfeiltasten', 'Auswahl verschieben'], ['Entf', 'leeren'],
        ['Leertaste+Ziehen, mittlere Maustaste', 'verschieben'], ['Zwei Finger', 'Zoom/Verschieben; Tipp = rückgängig, drei = wiederholen'], ['Langdruck', 'Pipette'], ['Esc', 'Paneel / Auswahl / Editor schließen']]
    }
  };
  function lang() { var l = String(window.LANG || document.documentElement.lang || 'en'); return l.indexOf('de') === 0 ? 'de' : 'en'; }
  function T(k, vars) {
    var s = (STR[lang()] || STR.en)[k];
    if (s == null) s = STR.en[k] != null ? STR.en[k] : k;
    if (vars) Object.keys(vars).forEach(function (v) { s = s.split('{' + v + '}').join(vars[v]); });
    return s;
  }
  var CTRL = /Mac|iP(hone|ad)/.test(navigator.platform || '') ? '⌘' : (lang() === 'de' ? 'Strg' : 'Ctrl');
  var SHIFT = lang() === 'de' ? 'Umschalt' : 'Shift';
  /* shortcut shown in tooltips (title = "Name (Key)") */
  var KEYS = {
    pencil: 'B', eraser: 'E', fill: 'G', picker: 'I', line: 'L', rect: 'U', select: 'M', move: 'V',
    undo: CTRL + '+Z', redo: CTRL + '+Y', grid: CTRL + "+'", zoomIn: '+', zoomOut: '−', fit: '0', close: 'Esc', swap: 'X',
    add: SHIFT + '+N', clear: lang() === 'de' ? 'Entf' : 'Del', copy: CTRL + '+C', paste: CTRL + '+V', deselect: CTRL + '+D', selAll: CTRL + '+A', rename: 'F2'
  };
  function tip(k) { return T(k) + (KEYS[k] ? ' (' + KEYS[k] + ')' : ''); }

  /* ---------------- icons: own drawings on a 24 px grid, stroke 2, round ends, currentColor (no icon library) */
  var ICONS = {
    pencil: '<path d="M4 20l1.2-4.6L15.6 5a2 2 0 0 1 2.8 0l.6.6a2 2 0 0 1 0 2.8L8.6 18.8z"/><path d="M13.5 7l3.5 3.5"/>',
    eraser: '<path d="M9 20h11"/><path d="M5.4 15.6l8.8-8.8a2 2 0 0 1 2.8 0l2.2 2.2a2 2 0 0 1 0 2.8L11 20H9.8z"/><path d="M9.6 11.4l5 5"/>',
    fill: '<path d="M4.2 11.8L11 5l7 7-6.4 6.4a1.6 1.6 0 0 1-2.2 0L4.2 14a1.6 1.6 0 0 1 0-2.2z"/><path d="M4.5 12h13.5"/><path d="M20.2 15s1.6 2 1.6 3.1a1.6 1.6 0 0 1-3.2 0c0-1.1 1.6-3.1 1.6-3.1z"/>',
    picker: '<path d="M14.5 5.5l1.6-1.6a2.1 2.1 0 0 1 3 3L17.5 8.5"/><path d="M12.5 7.5l4 4"/><path d="M14.5 9.5L7 17l-3 1 1-3 7.5-7.5"/>',
    line: '<path d="M7 17L17 7"/><rect x="3.5" y="16.5" width="4" height="4" rx=".5"/><rect x="16.5" y="3.5" width="4" height="4" rx=".5"/>',
    rect: '<rect x="4" y="6" width="16" height="12" rx="1"/>',
    rectFill: '<rect x="4" y="6" width="16" height="12" rx="1" fill="currentColor" fill-opacity=".4"/>',
    select: '<path d="M4 8V5h3M17 5h3v3M20 16v3h-3M7 19H4v-3M10.5 5h3M10.5 19h3M4 10.5v3M20 10.5v3"/>',
    move: '<path d="M12 3v18M3 12h18"/><path d="M9.5 5.5L12 3l2.5 2.5M9.5 18.5L12 21l2.5-2.5M5.5 9.5L3 12l2.5 2.5M18.5 9.5L21 12l-2.5 2.5"/>',
    undo: '<path d="M9 14L4 9l5-5"/><path d="M4 9h10.5a5.5 5.5 0 0 1 0 11H11"/>',
    redo: '<path d="M15 14l5-5-5-5"/><path d="M20 9H9.5a5.5 5.5 0 0 0 0 11H13"/>',
    zoomIn: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="M20 20l-4.8-4.8M8 10.5h5M10.5 8v5"/>',
    zoomOut: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="M20 20l-4.8-4.8M8 10.5h5"/>',
    fit: '<path d="M4 9V4h5M15 4h5v5M20 15v5h-5M9 20H4v-5"/><rect x="8.5" y="8.5" width="7" height="7" rx="1"/>',
    grid: '<rect x="4" y="4" width="16" height="16" rx="1"/><path d="M9.3 4v16M14.7 4v16M4 9.3h16M4 14.7h16"/>',
    symX: '<path d="M12 3v2M12 8v2M12 13v2M12 18v3"/><path d="M9 7.5L4 17h5zM15 7.5l5 9.5h-5z"/>',
    symY: '<path d="M3 12h2M8 12h2M13 12h2M18 12h3"/><path d="M7.5 9L17 4v5zM7.5 15l9.5 5v-5z"/>',
    pp: '<path d="M4 20h4v-4h4v-4h4V8h4V4"/>',
    open: '<path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>',
    exp: '<path d="M12 4v11M7.5 10.5L12 15l4.5-4.5M5 20h14"/>',
    save: '<path d="M5 4h11l3 3v12a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1z"/><path d="M8 4v5h7V4M8 20v-6h8v6"/>',
    close: '<path d="M6 6l12 12M18 6L6 18"/>',
    swap: '<path d="M7 3.5L4 6.5l3 3"/><path d="M4 6.5h10a4 4 0 0 1 4 4v1"/><path d="M17 20.5l3-3-3-3"/><path d="M20 17.5H10a4 4 0 0 1-4-4v-1"/>',
    add: '<path d="M12 5v14M5 12h14"/>',
    dup: '<path d="M12 3.5l8.5 4.5-8.5 4.5L3.5 8z"/><path d="M3.5 12.5l8.5 4.5 3-1.6"/><path d="M19 14v6M16 17h6"/>',
    copy: '<rect x="8" y="8" width="12" height="12" rx="2"/><path d="M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2"/>',
    paste: '<path d="M9 4.5H7a2 2 0 0 0-2 2V19a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V6.5a2 2 0 0 0-2-2h-2"/><rect x="9" y="3" width="6" height="3.5" rx="1"/><path d="M9 12h6M9 16h4"/>',
    del: '<path d="M4 7h16M10 11v6M14 11v6M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-12M9 7V4h6v3"/>',
    up: '<path d="M12 19V5M6 11l6-6 6 6"/>',
    down: '<path d="M12 5v14M6 13l6 6 6-6"/>',
    merge: '<path d="M12 3v8M8.5 7.5L12 11l3.5-3.5"/><path d="M3.5 15.5L12 20l8.5-4.5L16 13.2M8 13.2l-4.5 2.3"/>',
    rename: '<path d="M4 7h9M8.5 7v10"/><path d="M17 5v14M15 5h4M15 19h4"/>',
    lock: '<rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/>',
    unlock: '<rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 0 1 7.7-1.5"/>',
    eye: '<path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z"/><circle cx="12" cy="12" r="3"/>',
    eyeOff: '<path d="M6.6 6.9C4 8.6 2.5 12 2.5 12S6 18.5 12 18.5c1.9 0 3.5-.6 4.9-1.5M10 5.7c.6-.1 1.3-.2 2-.2 6 0 9.5 6.5 9.5 6.5a17 17 0 0 1-2.2 3.1"/><path d="M4 4l16 16"/>',
    flipH: '<path d="M12 3v18"/><path d="M8.5 7L4 17h4.5z"/><path d="M15.5 7l4.5 10h-4.5z" stroke-dasharray="2.2 2.2"/>',
    flipV: '<path d="M3 12h18"/><path d="M7 8.5L17 4v4.5z"/><path d="M7 15.5L17 20v-4.5z" stroke-dasharray="2.2 2.2"/>',
    rot: '<path d="M19.5 12a7.5 7.5 0 1 1-2.2-5.3"/><path d="M19.5 4v4.5H15"/>',
    clear: '<path d="M4 8V5h3M17 5h3v3M20 16v3h-3M7 19H4v-3"/><path d="M9 9l6 6M15 9l-6 6"/>',
    deselect: '<path d="M4 8V5h3M17 5h3v3M20 16v3h-3M7 19H4v-3M10.5 5h3M10.5 19h3M4 10.5v3M20 10.5v3"/><path d="M8 16l8-8"/>',
    selAll: '<path d="M4 8V5h3M17 5h3v3M20 16v3h-3M7 19H4v-3M10.5 5h3M10.5 19h3M4 10.5v3M20 10.5v3"/><rect x="8" y="8" width="8" height="8" fill="currentColor" fill-opacity=".4" stroke="none"/>',
    grip: '<g fill="currentColor" stroke="none"><circle cx="9" cy="6" r="1.5"/><circle cx="15" cy="6" r="1.5"/><circle cx="9" cy="12" r="1.5"/><circle cx="15" cy="12" r="1.5"/><circle cx="9" cy="18" r="1.5"/><circle cx="15" cy="18" r="1.5"/></g>',
    colors: '<path d="M12 3a9 9 0 1 0 0 18 2 2 0 0 0 1.5-3.3 2 2 0 0 1 1.5-3.4h2.3A3.7 3.7 0 0 0 21 10.6C21 6.4 17 3 12 3z"/><circle cx="7.5" cy="11.5" r="1.3" fill="currentColor"/><circle cx="10" cy="7.3" r="1.3" fill="currentColor"/><circle cx="15" cy="7.3" r="1.3" fill="currentColor"/>',
    layers: '<path d="M12 3l9 4.5-9 4.5-9-4.5z"/><path d="M3 12l9 4.5 9-4.5"/><path d="M3 16.5l9 4.5 9-4.5"/>',
    more: '<path d="M4 7h9M17 7h3M4 17h3M11 17h9"/><circle cx="15" cy="7" r="2"/><circle cx="9" cy="17" r="2"/>',
    image: '<rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="10" r="2"/><path d="M21 16l-5-5-9 9"/>',
    size1: '<rect x="10" y="10" width="4" height="4" fill="currentColor" stroke="none"/>',
    size2: '<rect x="7.5" y="7.5" width="9" height="9" fill="currentColor" stroke="none"/>',
    size3: '<rect x="5" y="5" width="14" height="14" fill="currentColor" stroke="none"/>',
    global: '<rect x="4" y="4" width="6.5" height="6.5" rx="1" fill="currentColor" fill-opacity=".45"/><rect x="13.5" y="13.5" width="6.5" height="6.5" rx="1" fill="currentColor" fill-opacity=".45"/><rect x="13.5" y="4" width="6.5" height="6.5" rx="1"/><rect x="4" y="13.5" width="6.5" height="6.5" rx="1"/>',
    alert: '<path d="M12 3.5L21.5 20h-19z"/><path d="M12 10v4.5M12 17.2v.3"/>',
    file: '<path d="M6 3h8l5 5v13H6z"/><path d="M14 3v5h5"/><path d="M12.5 11.5v6M9.5 14.5h6"/>'
  };
  function icon(name) {
    return '<svg class="pe-ic" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">' + (ICONS[name] || '') + '</svg>';
  }

  /* ---------------- style: tokens light/dark (wiki's data-theme wins), contrast >= 4.5:1 for text and states (A7) */
  var LIGHT = '--pe-bg:#efebe4;--pe-panel:#ffffff;--pe-line:#c9c1b4;--pe-line2:#8f8678;--pe-text:#1d1b18;--pe-muted:#585148;--pe-btn:#ebe6dd;--pe-hover:#ddd5c8;--pe-acc:#2f5aa0;--pe-acc-text:#ffffff;--pe-acc-soft:#dbe5f5;--pe-focus:#c2410c;--pe-warn:#8a4b00;--pe-danger:#a3261b;--pe-stage:#d9d3c9;--pe-shadow:0 8px 28px rgba(0,0,0,.18)';
  var DARK = '--pe-bg:#171513;--pe-panel:#221f1c;--pe-line:#3f3a34;--pe-line2:#7a7166;--pe-text:#ece6dc;--pe-muted:#b4aca0;--pe-btn:#2f2a25;--pe-hover:#3d3731;--pe-acc:#3d63a3;--pe-acc-text:#ffffff;--pe-acc-soft:#25324a;--pe-focus:#f59e5b;--pe-warn:#f0b35c;--pe-danger:#f08a7e;--pe-stage:#2b2723;--pe-shadow:0 8px 28px rgba(0,0,0,.55)';
  var CSS = [
    '.pe-root{' + LIGHT + '}',
    '@media (prefers-color-scheme: dark){:root:not([data-theme="light"]) .pe-root{' + DARK + '}}',
    ':root[data-theme="dark"] .pe-root{' + DARK + '}',
    '.pe-root{position:fixed;inset:0;z-index:40000;display:flex;flex-direction:column;background:var(--pe-bg);color:var(--pe-text);font:14px/1.35 system-ui,-apple-system,"Segoe UI",sans-serif;overflow:hidden}',
    '.pe-root *{box-sizing:border-box}',
    '.pe-root button,.pe-root select,.pe-root input{font:inherit;color:inherit}',
    '.pe-ic{width:22px;height:22px;flex:none;display:block}',
    /* header */
    '.pe-head{display:flex;gap:4px;align-items:center;padding:6px 10px;border-bottom:1px solid var(--pe-line);background:var(--pe-panel)}',
    '.pe-name{font-weight:700;margin-right:auto;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;padding:0 6px}',
    '.pe-name small{font-weight:400;color:var(--pe-muted);margin-left:6px}',
    '.pe-grp{display:flex;gap:2px;align-items:center}',
    '.pe-vsep{width:1px;align-self:stretch;margin:6px 6px;background:var(--pe-line)}',
    '.pe-zoom{min-width:3.6em;text-align:center;font:600 13px ui-monospace,monospace;color:var(--pe-muted)}',
    /* buttons */
    '.pe-btn{border:1px solid transparent;background:transparent;border-radius:8px;min-height:44px;min-width:44px;padding:0 10px;cursor:pointer;display:inline-flex;align-items:center;justify-content:center;gap:6px;touch-action:manipulation;white-space:nowrap;-webkit-tap-highlight-color:transparent}',
    '.pe-btn:hover:not(:disabled){background:var(--pe-hover)}',
    '.pe-btn.pe-icon{padding:0;width:44px}',
    '.pe-btn.pe-soft{background:var(--pe-btn);border-color:var(--pe-line)}',
    '.pe-btn.pe-primary{background:var(--pe-acc);color:var(--pe-acc-text);border-color:var(--pe-acc);font-weight:600}',
    '.pe-btn.pe-primary:hover{filter:brightness(1.1);background:var(--pe-acc)}',
    '.pe-btn.pe-danger{color:var(--pe-danger)}',
    '.pe-btn[aria-pressed="true"],.pe-btn[aria-expanded="true"]{background:var(--pe-acc);color:var(--pe-acc-text);border-color:var(--pe-acc)}',
    '.pe-btn[aria-pressed="true"]:hover{background:var(--pe-acc)}',
    '.pe-btn:disabled{opacity:.4;cursor:default}',
    '.pe-root :focus{outline:none}',
    '.pe-root :focus-visible{outline:3px solid var(--pe-focus);outline-offset:1px;border-radius:8px}',
    /* context bar */
    '.pe-ctx{display:flex;flex-wrap:wrap;gap:4px;align-items:center;padding:4px 10px;min-height:54px;border-bottom:1px solid var(--pe-line);background:var(--pe-panel)}',
    '.pe-ctx .pe-tname{font-weight:700;min-width:6.5em;display:flex;align-items:center;gap:6px}',
    '.pe-ctx .pe-hint{color:var(--pe-muted);font-size:13px;margin-left:8px;min-width:0}',
    '.pe-seg{display:inline-flex;gap:2px;padding:2px;border-radius:10px;background:var(--pe-btn);border:1px solid var(--pe-line)}',
    '.pe-seg .pe-btn{min-height:40px;min-width:40px;width:40px}',
    '.pe-lbl{font-size:13px;color:var(--pe-muted);margin:0 2px 0 6px}',
    /* body */
    '.pe-body{flex:1;min-height:0;display:grid;grid-template-columns:auto minmax(0,1fr) 320px}',
    '.pe-tools{display:flex;flex-direction:column;gap:2px;padding:8px 6px;border-right:1px solid var(--pe-line);background:var(--pe-panel);overflow:auto;align-items:center}',
    '.pe-hsep{height:1px;width:32px;background:var(--pe-line);margin:6px 0;flex:none}',
    '.pe-tools .pe-chip{margin-top:auto}',
    '.pe-chip{position:relative;width:56px;height:60px;flex:none}',
    '.pe-chip .pe-sw{position:absolute;width:34px;height:34px;border-radius:6px}',
    '.pe-chip .pe-chip-p{left:2px;top:2px;z-index:1;box-shadow:0 0 0 2px var(--pe-panel)}',
    '.pe-chip .pe-chip-s{right:2px;top:18px}',
    '.pe-chip .pe-chip-x{position:absolute;left:0;bottom:-6px;min-height:28px;min-width:28px;width:28px;padding:0;border-radius:6px}',
    '.pe-chip .pe-chip-x .pe-ic{width:16px;height:16px}',
    '.pe-center{display:flex;flex-direction:column;min-width:0;min-height:0}',
    '.pe-stage{position:relative;flex:1;min-width:0;min-height:0;background:var(--pe-stage);overflow:hidden}',
    '.pe-canvas{position:absolute;inset:0;width:100%;height:100%;touch-action:none;cursor:crosshair}',
    '.pe-canvas.pe-c-move{cursor:move}',
    '.pe-statusbar{display:flex;gap:14px;align-items:center;padding:4px 12px;min-height:30px;border-top:1px solid var(--pe-line);background:var(--pe-panel);font:12px ui-monospace,monospace;color:var(--pe-text);white-space:nowrap;overflow:hidden}',
    '.pe-statusbar span{display:inline-flex;gap:6px;align-items:center}',
    '.pe-statusbar i{display:inline-block;width:14px;height:14px;border:1px solid var(--pe-line2);border-radius:3px}',
    '.pe-statusbar .pe-warn{color:var(--pe-warn);font-weight:700}',
    '.pe-st-sp{margin-left:auto}',
    /* empty / error state and toast */
    '.pe-empty{position:absolute;inset:0;display:flex;align-items:center;justify-content:center;padding:16px}',
    '.pe-empty>div{max-width:360px;text-align:center;background:var(--pe-panel);border:1px solid var(--pe-line);border-radius:12px;padding:20px;box-shadow:var(--pe-shadow)}',
    '.pe-empty .pe-ic{width:40px;height:40px;margin:0 auto 8px;color:var(--pe-muted)}',
    '.pe-empty p{margin:0 0 12px}',
    '.pe-empty .pe-err{font:12px ui-monospace,monospace;color:var(--pe-muted);word-break:break-all}',
    '.pe-empty .pe-row{justify-content:center}',
    '.pe-spin{width:28px;height:28px;border-radius:50%;border:3px solid var(--pe-line);border-top-color:var(--pe-acc);margin:0 auto 10px;animation:pe-spin 0.9s linear infinite}',
    '@keyframes pe-spin{to{transform:rotate(360deg)}}',
    '@media (prefers-reduced-motion:reduce){.pe-spin{animation:none}}',
    '.pe-toast{position:absolute;left:50%;top:12px;transform:translateX(-50%);max-width:calc(100% - 24px);background:var(--pe-text);color:var(--pe-bg);border-radius:8px;padding:8px 14px;font-weight:600;box-shadow:var(--pe-shadow);pointer-events:none}',
    /* side panel */
    '.pe-side{overflow:auto;border-left:1px solid var(--pe-line);background:var(--pe-panel);padding:0 12px 16px}',
    '.pe-side section{padding:12px 0;border-bottom:1px solid var(--pe-line)}',
    '.pe-side section:last-child{border-bottom:0}',
    '.pe-side h3{margin:0 0 8px;font-size:12px;text-transform:uppercase;letter-spacing:.06em;color:var(--pe-muted);display:flex;align-items:center;gap:6px}',
    '.pe-side h3 .pe-ic{width:16px;height:16px}',
    '.pe-sheethead{display:none}',
    '.pe-row{display:flex;flex-wrap:wrap;gap:6px;align-items:center}',
    '.pe-sw{width:28px;height:28px;border-radius:5px;border:1px solid var(--pe-line2);cursor:pointer;padding:0;background-image:linear-gradient(45deg,#bbb 25%,transparent 25%,transparent 75%,#bbb 75%),linear-gradient(45deg,#bbb 25%,#fff 25%,#fff 75%,#bbb 75%);background-size:8px 8px;background-position:0 0,4px 4px;position:relative;flex:none}',
    '.pe-sw i{position:absolute;inset:0;border-radius:4px}',
    '.pe-sw.pe-big{width:52px;height:52px;border-radius:8px}',
    '.pe-sw.pe-mid{width:36px;height:36px;border-radius:6px}',
    '.pe-sw[aria-current="true"]{box-shadow:0 0 0 2px var(--pe-panel),0 0 0 4px var(--pe-acc)}',
    '.pe-colors{display:grid;grid-template-columns:auto auto 1fr;gap:8px;align-items:center}',
    '.pe-colors .pe-fields{display:flex;flex-direction:column;gap:6px;min-width:0}',
    '.pe-pal{display:grid;grid-template-columns:repeat(auto-fill,minmax(28px,1fr));gap:4px}',
    '.pe-pal .pe-sw{width:100%;height:28px}',
    '.pe-sub{font-size:12px;color:var(--pe-muted);margin:10px 0 6px}',
    '.pe-layers{display:flex;flex-direction:column;gap:4px;margin:0 0 8px}',
    '.pe-layer{display:flex;align-items:center;gap:4px;border:1px solid var(--pe-line);border-radius:10px;padding:3px 4px;cursor:pointer;background:var(--pe-bg);min-height:50px}',
    '.pe-layer[aria-selected="true"]{border-color:var(--pe-acc);background:var(--pe-acc-soft);box-shadow:inset 3px 0 0 var(--pe-acc)}',
    '.pe-layer.pe-hid .pe-ln,.pe-layer.pe-hid canvas{opacity:.45}',
    '.pe-layer.pe-dragging{opacity:.55}',
    '.pe-layer.pe-drop-above{box-shadow:0 -3px 0 var(--pe-acc)}',
    '.pe-layer.pe-drop-below{box-shadow:0 3px 0 var(--pe-acc)}',
    '.pe-layer .pe-ln{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}',
    '.pe-layer canvas{width:36px;height:36px;image-rendering:pixelated;border:1px solid var(--pe-line2);border-radius:4px;flex:none;background:repeating-conic-gradient(#ccc 0 25%,#fff 0 50%) 0 0/8px 8px}',
    '.pe-layer .pe-op{font:12px ui-monospace,monospace;color:var(--pe-muted);min-width:3.2em;text-align:right}',
    '.pe-layer .pe-btn{min-height:44px;min-width:36px;width:36px;padding:0}',
    '.pe-layer .pe-grip{cursor:grab;touch-action:none;color:var(--pe-muted);width:28px;min-width:28px}',
    '.pe-layer .pe-btn[aria-pressed]{background:transparent;color:inherit;border-color:transparent}',
    '.pe-layer .pe-btn.pe-on{color:var(--pe-acc)}',
    ':root[data-theme="dark"] .pe-layer .pe-btn.pe-on{color:#9cbcf0}',
    '@media (prefers-color-scheme: dark){:root:not([data-theme="light"]) .pe-layer .pe-btn.pe-on{color:#9cbcf0}}',
    '.pe-layer input{flex:1;min-width:0}',
    '.pe-opacity{display:grid;grid-template-columns:auto 1fr auto;gap:8px;align-items:center;font-size:13px;color:var(--pe-muted);margin:4px 0 8px}',
    '.pe-iconbar{display:flex;flex-wrap:wrap;gap:2px}',
    '.pe-prev{display:flex;flex-wrap:wrap;gap:12px;align-items:flex-end}',
    '.pe-prev figure{margin:0;font-size:12px;color:var(--pe-muted)}',
    '.pe-prev canvas{display:block;image-rendering:pixelated;border:1px solid var(--pe-line2);background:repeating-conic-gradient(#ccc 0 25%,#fff 0 50%) 0 0/8px 8px}',
    '.pe-ref img{max-width:100%;image-rendering:pixelated;border:1px solid var(--pe-line2);cursor:crosshair;margin-top:8px}',
    '.pe-note{font-size:12px;color:var(--pe-muted);margin:6px 0 0}',
    '.pe-save{margin-top:10px;display:flex;flex-direction:column;gap:6px}',
    '.pe-warn{font-size:13px;color:var(--pe-warn);font-weight:600}',
    '.pe-keys{display:grid;grid-template-columns:auto 1fr;gap:4px 10px;font-size:12px;margin:8px 0 0}',
    '.pe-keys dt{font:600 12px ui-monospace,monospace}',
    '.pe-keys dd{margin:0;color:var(--pe-muted)}',
    '.pe-side details summary{cursor:pointer;min-height:36px;display:flex;align-items:center;font-size:13px;font-weight:600}',
    '.pe-root input[type=text]{padding:6px 8px;min-height:40px;border-radius:8px;border:1px solid var(--pe-line2);background:var(--pe-bg);width:100%;font-family:ui-monospace,monospace}',
    '.pe-root input[type=range]{accent-color:var(--pe-acc);min-height:32px;width:100%}',
    '.pe-root input[type=color]{width:100%;height:40px;padding:2px;border:1px solid var(--pe-line2);border-radius:8px;background:var(--pe-bg);cursor:pointer}',
    '.pe-file{position:absolute;width:1px;height:1px;opacity:0;pointer-events:none}',
    /* dialog */
    '.pe-scrim{position:absolute;inset:0;background:rgba(0,0,0,.45);display:flex;align-items:center;justify-content:center;padding:16px;z-index:5}',
    '.pe-dlg{background:var(--pe-panel);color:var(--pe-text);border:1px solid var(--pe-line);border-radius:12px;padding:18px;max-width:380px;width:100%;box-shadow:var(--pe-shadow)}',
    '.pe-dlg p{margin:0 0 14px;display:flex;gap:10px;align-items:flex-start}',
    '.pe-dlg p .pe-ic{color:var(--pe-warn)}',
    '.pe-dlg .pe-row{justify-content:flex-end}',
    '.pe-mbar,.pe-phone-only{display:none}',
    'body.pe-open{overflow:hidden}',
    /* phone: tools at the bottom, panels as sheets */
    '@media (max-width:760px){',
    '.pe-dk{display:none!important}',
    '.pe-head{padding:4px 6px}.pe-name small{display:none}.pe-vsep{margin:6px 2px}',
    '.pe-ctx{padding:2px 6px;min-height:48px}.pe-ctx .pe-hint{display:none}.pe-ctx .pe-tname{min-width:0}.pe-ctx .pe-tname span{display:none}',
    '.pe-body{display:flex;flex-direction:column}',
    '.pe-center{flex:1;order:1}',
    '.pe-tools{order:2;flex-direction:row;justify-content:space-between;padding:4px 6px;border-right:0;border-top:1px solid var(--pe-line);overflow:visible}',
    '.pe-tools .pe-hsep,.pe-tools .pe-chip{display:none}',
    '.pe-statusbar{gap:10px;padding:2px 8px;min-height:24px}.pe-statusbar .pe-dkst{display:none}',
    '.pe-mbar{display:flex;justify-content:space-between;align-items:center;gap:2px;height:calc(51px + env(safe-area-inset-bottom));padding:2px 6px calc(4px + env(safe-area-inset-bottom));position:relative;z-index:4;background:var(--pe-panel);border-top:1px solid var(--pe-line)}',
    '.pe-mbar .pe-chip{width:82px;height:44px}.pe-mbar .pe-chip .pe-sw{width:44px;height:44px;top:0;border-radius:8px}.pe-mbar .pe-chip .pe-chip-p{left:0}.pe-mbar .pe-chip .pe-chip-s{left:38px;right:auto}.pe-mbar .pe-chip .pe-chip-x{display:none}',
    '.pe-side section.pe-phone-only{display:block}.pe-sw.pe-mid{width:44px;height:44px}',
    '.pe-side{display:none;position:fixed;left:0;right:0;bottom:calc(51px + env(safe-area-inset-bottom));max-height:calc(64vh - 51px);z-index:3;border-left:0;border-top:1px solid var(--pe-line);border-radius:16px 16px 0 0;box-shadow:var(--pe-shadow)}',
    '.pe-root[data-sheet] .pe-side{display:block}',
    '.pe-root[data-sheet="colors"] .pe-side section:not([data-sheet="colors"]),.pe-root[data-sheet="layers"] .pe-side section:not([data-sheet="layers"]),.pe-root[data-sheet="more"] .pe-side section:not([data-sheet="more"]){display:none}',
    '.pe-seg .pe-btn,.pe-layer .pe-btn,.pe-layer .pe-grip{min-width:44px;width:44px;min-height:44px}.pe-pal{grid-template-columns:repeat(auto-fill,minmax(44px,1fr))}.pe-pal .pe-sw{height:44px}',
    '.pe-sheethead{display:flex;align-items:center;justify-content:space-between;position:sticky;top:0;background:var(--pe-panel);z-index:1;padding:4px 0;border-bottom:1px solid var(--pe-line);font-weight:700}',
    '.pe-sheethead::before{content:"";position:absolute;left:50%;top:4px;width:36px;height:4px;border-radius:2px;background:var(--pe-line2);transform:translateX(-50%)}',
    '}'
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
      else if (k === 'html') e.innerHTML = attrs[k];
      else if (k.slice(0, 2) === 'on') e.addEventListener(k.slice(2), attrs[k]);
      else if (attrs[k] !== false && attrs[k] != null) e.setAttribute(k, attrs[k] === true ? '' : attrs[k]);
    });
    (kids || []).forEach(function (c) { if (c) e.appendChild(typeof c === 'string' ? document.createTextNode(c) : c); });
    return e;
  }
  /* icon button: ic = icon name, label = tooltip/aria text, text = visible text (optional) */
  function ibtn(ic, label, on, extra, text) {
    extra = extra || {};
    var cls = 'pe-btn' + (text ? '' : ' pe-icon') + (extra.cls ? ' ' + extra.cls : '');
    delete extra.cls;
    var b = h('button', Object.assign({ type: 'button', class: cls, title: label, 'aria-label': label, onclick: on }, extra));
    b.innerHTML = icon(ic) + (text ? '<span>' + esc(text) + '</span>' : '');
    return b;
  }
  function esc(s) { return String(s).replace(/[&<>"]/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]; }); }
  function cssColor(c) { return 'rgba(' + c[0] + ',' + c[1] + ',' + c[2] + ',' + (c[3] / 255) + ')'; }
  function hexOf(c) { return C.colorHex(c[0], c[1], c[2], c[3]); }
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
  /* [id, key, group]; the icon has the id's name */
  var TOOLS = [['pencil', 'b', 'gDraw'], ['eraser', 'e', 'gDraw'], ['fill', 'g', 'gDraw'], ['picker', 'i', 'gDraw'],
    ['line', 'l', 'gShape'], ['rect', 'u', 'gShape'], ['select', 'm', 'gSel'], ['move', 'v', 'gSel']];
  var DRAWS = { pencil: 1, eraser: 1, line: 1, rect: 1 };

  function open(opts) {
    if (!C) return Promise.reject(new Error('SBPixelCore missing'));
    if (S) close(true);
    injectCss();
    opts = opts || {};
    S = {
      opts: opts, model: null, active: 0, tool: 'pencil', primary: [30, 30, 30, 255], secondary: [255, 255, 255, 255],
      sym: { x: false, y: false }, grid: true, pp: false, filled: false, global: false, size: 1, recent: [], lastPt: null,
      zoom: 16, ox: 0, oy: 0, sel: null, clip: null, history: new C.History(), dirty: false, sheet: null, renaming: -1,
      pointers: new Map(), stroke: null, gesture: null, hover: null, alt: false, raf: 0, from: ''
    };
    buildUi();
    showEmpty('loading');
    var load = opts.model ? Promise.resolve({ model: opts.model, from: '' })
      : opts.layers ? fetch(opts.layers).then(function (r) { if (!r.ok) throw new Error(r.status + ' ' + opts.layers); return r.json(); })
        .then(function (doc) { return { model: C.fromDoc(doc), from: T('fromLayers') }; })
        : fetchBytes(opts.png).then(decodePng).then(function (img) { return { model: C.fromRgba(img.w, img.h, img.rgba, T('base')), from: T('fromPng') }; });
    return load.then(function (r) { if (S) setModel(r.model, r.from); })
      .catch(function (e) { if (S) showEmpty('error', e && e.message || String(e)); });
  }
  function setModel(model, from) {
    S.model = model; S.from = from || ''; S.active = model.layers.length - 1; S.sel = null; S.lastPt = null;
    var colors = C.colorsOf(model);
    if (colors.length) S.primary = C.parseColor(colors[0]);
    showEmpty(null); fit(); refreshSide(); sync(); paint();
  }

  function buildUi() {
    var o = S.opts;
    S.root = h('div', { class: 'pe-root', role: 'dialog', 'aria-modal': 'true', 'aria-label': T('title') });
    S.fileIn = h('input', { type: 'file', class: 'pe-file', tabindex: -1, 'aria-hidden': 'true', accept: '.png,.json,image/png,application/json', onchange: function () { var f = S.fileIn.files[0]; S.fileIn.value = ''; openFile(f); } });
    var name = h('span', { class: 'pe-name', title: (o.name || '') + ' ' + (o.id || '') }, [o.name || o.id || T('title')]);
    if (o.id && o.name) name.appendChild(h('small', { text: o.id }));
    var head = h('header', { class: 'pe-head' }, [
      name,
      h('div', { class: 'pe-grp' }, [ibtn('undo', tip('undo'), undo, { 'data-act': 'undo' }), ibtn('redo', tip('redo'), redo, { 'data-act': 'redo' })]),
      h('span', { class: 'pe-vsep pe-dk' }),
      h('div', { class: 'pe-grp pe-dk' }, [ibtn('zoomOut', tip('zoomOut'), function () { zoomStep(-1); }),
        S.zoomLbl = h('output', { class: 'pe-zoom', 'aria-label': 'Zoom' }),
        ibtn('zoomIn', tip('zoomIn'), function () { zoomStep(1); }), ibtn('fit', tip('fit'), function () { fit(); paint(); }),
        ibtn('grid', tip('grid'), toggleGrid, { 'data-toggle': 'grid' })]),
      h('span', { class: 'pe-vsep' }),
      h('div', { class: 'pe-grp' }, [ibtn('open', T('open'), askOpen, { cls: 'pe-dk' }), S.fileIn,
        canSave() ? ibtn('save', T('saveTip'), saveToRepo, { cls: 'pe-primary', 'data-act': 'save' }, T('save')) : null,
        ibtn('exp', T('exp') + ': ' + T('both'), exportBoth, { cls: canSave() ? 'pe-soft pe-dk' : 'pe-primary', 'data-act': 'export' }, T('exp')),
        ibtn('close', tip('close'), function () { close(false); }, { 'data-act': 'close' })])
    ]);
    S.ctx = h('div', { class: 'pe-ctx', role: 'toolbar', 'aria-label': T('tools') });
    var tools = h('nav', { class: 'pe-tools', role: 'toolbar', 'aria-label': T('tools'), 'aria-orientation': 'vertical' });
    var group = null;
    TOOLS.forEach(function (t) {
      if (group && group !== t[2]) tools.appendChild(h('span', { class: 'pe-hsep', role: 'separator' }));
      group = t[2];
      tools.appendChild(ibtn(t[0], tip(t[0]), function () { setTool(t[0]); }, { 'data-tool': t[0], 'aria-keyshortcuts': t[1].toUpperCase() }));
    });
    S.chips = [];
    tools.appendChild(chip(true));
    S.canvas = h('canvas', { class: 'pe-canvas', 'aria-label': T('title'), role: 'img' });
    S.stage = h('div', { class: 'pe-stage' }, [S.canvas]);
    S.statusbar = h('div', { class: 'pe-statusbar', role: 'status', 'aria-label': T('status'), 'aria-live': 'off' });
    S.side = h('aside', { class: 'pe-side', 'aria-label': T('layers') + ', ' + T('colors') });
    S.root.appendChild(head);
    S.root.appendChild(S.ctx);
    S.root.appendChild(h('div', { class: 'pe-body' }, [tools, h('div', { class: 'pe-center' }, [S.stage, S.statusbar]), S.side]));
    // phone bar: colour chip, zoom, grid, panel sheets
    S.root.appendChild(h('nav', { class: 'pe-mbar', 'aria-label': T('more') }, [chip(false),
      ibtn('zoomOut', tip('zoomOut'), function () { zoomStep(-1); }), ibtn('zoomIn', tip('zoomIn'), function () { zoomStep(1); }),
      ibtn('fit', tip('fit'), function () { fit(); paint(); }),
      ibtn('colors', T('colors'), function () { sheet('colors'); }, { 'data-sheet-btn': 'colors', 'aria-expanded': 'false' }),
      ibtn('layers', T('layers'), function () { sheet('layers'); }, { 'data-sheet-btn': 'layers', 'aria-expanded': 'false' }),
      ibtn('more', T('more'), function () { sheet('more'); }, { 'data-sheet-btn': 'more', 'aria-expanded': 'false' })]));
    document.body.appendChild(S.root);
    document.body.classList.add('pe-open');
    S.ctx2d = S.canvas.getContext('2d');
    S.off = document.createElement('canvas');
    bindPointer();
    S.onKey = onKey;
    S.onKeyUp = function (e) { if (e.key === 'Alt' && S) S.alt = false; };
    S.onBlur = function () { if (S) { S.alt = false; S.space = false; } };
    window.addEventListener('keydown', S.onKey, true);
    window.addEventListener('keyup', S.onKeyUp, true);
    window.addEventListener('blur', S.onBlur);
    S.root.addEventListener('keydown', function (e) { e.stopPropagation(); });
    S.ro = typeof ResizeObserver !== 'undefined' ? new ResizeObserver(function () { paint(); }) : null;
    if (S.ro) S.ro.observe(S.stage); else window.addEventListener('resize', paint);
    buildCtx(); sync();
    S.root.querySelector('[data-tool="pencil"]').focus();
  }
  /* primary/secondary colour chip (Aseprite-like): primary opens the colours, secondary swaps */
  function chip(desk) {
    var p = h('button', { type: 'button', class: 'pe-sw pe-chip-p', title: T('primary'), 'aria-label': T('primary'), onclick: function () { focusColors(); } }, [h('i')]);
    var s = h('button', { type: 'button', class: 'pe-sw pe-chip-s', title: T('secondary') + ' – ' + tip('swap'), 'aria-label': T('secondary') + ' – ' + T('swap'), onclick: swap }, [h('i')]);
    var box = h('div', { class: 'pe-chip' }, [s, p, desk ? ibtn('swap', tip('swap'), swap, { cls: 'pe-soft pe-chip-x' }) : null]);
    S.chips.push({ p: p, s: s });
    return box;
  }
  function focusColors() {
    if (isPhone()) { sheet('colors'); return; }
    var c = S.side.querySelector('input[type=color]'); if (c) c.focus();
  }
  function isPhone() { return window.matchMedia && window.matchMedia('(max-width:760px)').matches; }
  function sheet(name) {
    S.sheet = S.sheet === name || !name ? null : name;
    if (S.sheet) S.root.setAttribute('data-sheet', S.sheet); else S.root.removeAttribute('data-sheet');
    S.root.querySelectorAll('[data-sheet-btn]').forEach(function (b) { b.setAttribute('aria-expanded', String(b.getAttribute('data-sheet-btn') === S.sheet)); });
    if (S.sheet) { var hd = S.side.querySelector('.pe-sheethead button'); if (hd) hd.focus(); }
    refreshSheetHead();
  }
  function refreshSheetHead() {
    var t = S.side.querySelector('.pe-sheethead span');
    if (t) t.textContent = S.sheet ? T(S.sheet) : '';
  }

  /* ---------------- close / dialogs / messages */
  function close(force) {
    if (!S) return;
    if (!force && S.dirty) { ask(T('dirty'), T('discard')).then(function (yes) { if (yes) close(true); }); return; }
    window.removeEventListener('keydown', S.onKey, true);
    window.removeEventListener('keyup', S.onKeyUp, true);
    window.removeEventListener('blur', S.onBlur);
    if (S.ro) S.ro.disconnect(); else window.removeEventListener('resize', paint);
    if (S.ref) URL.revokeObjectURL(S.ref);
    clearTimeout(S.pollT);
    S.root.remove();
    document.body.classList.remove('pe-open');
    S = null;
  }
  /* own confirm dialog (focus starts on "Cancel"; Esc cancels) */
  function ask(text, okLabel) {
    return new Promise(function (done) {
      if (S.dialog) S.dialog.finish(false);
      var back = document.activeElement;
      var scrim = h('div', { class: 'pe-scrim' });
      function finish(v) { scrim.remove(); if (S) S.dialog = null; if (back && back.focus && document.contains(back)) back.focus(); done(v); }
      var cancel = h('button', { type: 'button', class: 'pe-btn pe-soft', onclick: function () { finish(false); } }, [T('cancel')]);
      var ok = h('button', { type: 'button', class: 'pe-btn pe-primary', 'data-act': 'confirm', onclick: function () { finish(true); } }, [okLabel || T('ok')]);
      var msg = h('p', { id: 'pe-dlg-msg' }); msg.innerHTML = icon('alert'); msg.appendChild(h('span', { text: text }));
      scrim.appendChild(h('div', { class: 'pe-dlg', role: 'alertdialog', 'aria-modal': 'true', 'aria-describedby': 'pe-dlg-msg' }, [msg, h('div', { class: 'pe-row' }, [cancel, ok])]));
      scrim.addEventListener('keydown', function (e) {
        if (e.key === 'Tab') { e.preventDefault(); (document.activeElement === ok ? cancel : ok).focus(); }
      });
      S.root.appendChild(scrim);
      S.dialog = { finish: finish };
      cancel.focus();
    });
  }
  function toast(text) {
    if (!S) return;
    if (S.toastEl) S.toastEl.remove();
    var t = S.toastEl = h('div', { class: 'pe-toast', role: 'alert', text: text });
    S.stage.appendChild(t);
    clearTimeout(S.toastT);
    S.toastT = setTimeout(function () { t.remove(); }, 3200);
  }
  /* loading / error state in the stage */
  function showEmpty(kind, detail) {
    if (S.emptyEl) { S.emptyEl.remove(); S.emptyEl = null; }
    if (!kind) return;
    var box = h('div');
    if (kind === 'loading') { box.appendChild(h('div', { class: 'pe-spin', 'aria-hidden': 'true' })); box.appendChild(h('p', { text: T('loading') })); }
    else {
      box.insertAdjacentHTML('beforeend', icon('image'));
      box.appendChild(h('p', null, [h('strong', { text: T('loadFail') })]));
      if (detail) box.appendChild(h('p', { class: 'pe-err', text: detail }));
      box.appendChild(h('div', { class: 'pe-row' }, [
        ibtn('open', T('open'), function () { S.fileIn.click(); }, { cls: 'pe-soft' }, T('open')),
        ibtn('file', T('blank'), function () { setModel(C.blank(16, 16, T('base')), ''); }, { cls: 'pe-primary' }, T('blank'))]));
    }
    S.emptyEl = h('div', { class: 'pe-empty', role: kind === 'loading' ? 'status' : 'alert' }, [box]);
    S.stage.appendChild(S.emptyEl);
  }

  /* ---------------- state -> buttons */
  var TOGGLES = {
    grid: function () { return S.grid; }, symX: function () { return S.sym.x; }, symY: function () { return S.sym.y; },
    pp: function () { return S.pp; }, filled: function () { return S.filled; }, global: function () { return S.global; },
    size1: function () { return S.size === 1; }, size2: function () { return S.size === 2; }, size3: function () { return S.size === 3; }
  };
  function sync() {
    if (!S) return;
    var r = S.root, has = !!S.model;
    r.querySelectorAll('[data-tool]').forEach(function (b) { b.setAttribute('aria-pressed', String(S.tool === b.getAttribute('data-tool'))); b.disabled = !has; });
    r.querySelectorAll('[data-toggle]').forEach(function (b) { var f = TOGGLES[b.getAttribute('data-toggle')]; if (f) b.setAttribute('aria-pressed', String(!!f())); });
    r.querySelectorAll('[data-act="undo"]').forEach(function (b) { b.disabled = !S.history.past.length; });
    r.querySelectorAll('[data-act="redo"]').forEach(function (b) { b.disabled = !S.history.future.length; });
    r.querySelectorAll('[data-act="export"]').forEach(function (b) { b.disabled = !has; });
    S.canvas.classList.toggle('pe-c-move', S.tool === 'move');
    if (S.zoomLbl) S.zoomLbl.textContent = S.zoom + '×';
  }
  function setTool(t) { if (S.tool !== t) { S.tool = t; buildCtx(); } sync(); paint(); }
  function toggleGrid() { S.grid = !S.grid; sync(); paint(); }
  function setSize(n) { S.size = Math.max(1, Math.min(3, n)); sync(); paint(); }
  function layer() { return S.model && S.model.layers[S.active]; }

  /* context bar: name of the tool and its options */
  function buildCtx() {
    var c = S.ctx, t = S.tool, kids = [];
    c.innerHTML = '';
    var name = h('span', { class: 'pe-tname' }); name.innerHTML = icon(t); name.appendChild(h('span', { text: T(t) }));
    kids.push(name);
    function tg(ic, key, on) { return ibtn(ic, T(key), on, { 'data-toggle': key }); }
    if (DRAWS[t]) {
      kids.push(h('span', { class: 'pe-lbl pe-dk', text: T('size') }));
      kids.push(h('div', { class: 'pe-seg', role: 'group', 'aria-label': T('size') }, [1, 2, 3].map(function (n) {
        return ibtn('size' + n, T('size') + ' ' + n + ' ([ ])', function () { setSize(n); }, { 'data-toggle': 'size' + n });
      })));
      if (t === 'pencil' || t === 'eraser') kids.push(tg('pp', 'pp', function () { S.pp = !S.pp; sync(); }));
      if (t === 'rect') kids.push(tg('rectFill', 'filled', function () { S.filled = !S.filled; sync(); }));
      kids.push(h('span', { class: 'pe-vsep' }));
      kids.push(tg('symX', 'symX', function () { S.sym.x = !S.sym.x; sync(); paint(); }));
      kids.push(tg('symY', 'symY', function () { S.sym.y = !S.sym.y; sync(); paint(); }));
      kids.push(h('span', { class: 'pe-hint', text: t === 'line' || t === 'rect' ? T('hintShape') : T('hintPencil') }));
    } else if (t === 'fill') {
      kids.push(tg('global', 'global', function () { S.global = !S.global; sync(); }));
      kids.push(h('span', { class: 'pe-hint', text: T('hintFill') }));
    } else if (t === 'picker') {
      kids.push(h('span', { class: 'pe-hint', text: T('hintPicker') }));
    } else {
      regionButtons().forEach(function (b) { kids.push(b); });
      kids.push(h('span', { class: 'pe-hint', text: t === 'move' ? T('hintMove') : T('hintSel') }));
    }
    kids.forEach(function (k) { c.appendChild(k); });
    sync();
  }
  function regionButtons() {
    return [
      ibtn('selAll', tip('selAll'), selectAll), ibtn('deselect', tip('deselect'), deselect),
      ibtn('flipH', T('flipH'), function () { transform(function (b) { return C.flipBuf(b, true); }); }),
      ibtn('flipV', T('flipV'), function () { transform(function (b) { return C.flipBuf(b, false); }); }),
      ibtn('rot', T('rot'), function () { transform(C.rotateBuf); }),
      ibtn('copy', tip('copy'), copySel), ibtn('paste', tip('paste'), paste), ibtn('clear', tip('clear'), clearSel, { cls: 'pe-danger' })];
  }

  /* ---------------- history */
  function record(before) { S.history.push(before || S.model); S.dirty = true; sync(); }
  function changed(before) { record(before); refreshSide(); paint(); }
  function restore(m) { S.model = m; S.active = Math.min(S.active, m.layers.length - 1); S.dirty = true; S.renaming = -1; sync(); refreshSide(); paint(); }
  function undo() { if (!S.model) return; var m = S.history.undo(S.model); if (m) restore(m); }
  function redo() { if (!S.model) return; var m = S.history.redo(S.model); if (m) restore(m); }

  /* ---------------- view */
  function fit() {
    var r = S.stage.getBoundingClientRect(), m = S.model;
    if (!m) return;
    S.zoom = Math.max(1, Math.min(64, Math.floor(Math.min((r.width - 32) / m.w, (r.height - 32) / m.h)) || 1));
    S.ox = Math.round((r.width - m.w * S.zoom) / 2); S.oy = Math.round((r.height - m.h * S.zoom) / 2);
    sync();
  }
  function zoomAt(z, sx, sy) {
    z = Math.max(1, Math.min(64, Math.round(z)));
    if (z === S.zoom) return;
    S.ox = sx - (sx - S.ox) * z / S.zoom; S.oy = sy - (sy - S.oy) * z / S.zoom; S.zoom = z;
    sync(); paint();
  }
  function zoomStep(d) {
    if (!S.model) return;
    var r = S.stage.getBoundingClientRect();
    zoomAt(S.zoom + d * Math.max(1, S.zoom >> 2), r.width / 2, r.height / 2);
  }
  function paint() { if (S && !S.raf) S.raf = requestAnimationFrame(draw); }
  function draw() {
    if (!S) return;
    S.raf = 0;
    var cv = S.canvas, dpr = window.devicePixelRatio || 1, r = S.stage.getBoundingClientRect();
    var W = Math.max(1, Math.round(r.width * dpr)), H = Math.max(1, Math.round(r.height * dpr));
    if (cv.width !== W || cv.height !== H) { cv.width = W; cv.height = H; }
    var g = S.ctx2d, m = S.model;
    g.setTransform(dpr, 0, 0, dpr, 0, 0);
    g.clearRect(0, 0, r.width, r.height);
    if (!m) { status(null); return; }
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
      // brush outline (size 1-3) for drawing tools, one pixel otherwise
      var n = DRAWS[S.tool] && !S.alt ? S.size : 1, lo = -((n - 1) >> 1);
      g.strokeStyle = 'rgba(47,90,160,.95)'; g.lineWidth = 2;
      g.strokeRect(x0 + (S.hover[0] + lo) * z + 1, y0 + (S.hover[1] + lo) * z + 1, n * z - 2, n * z - 2);
    }
    status(comp);
    previews(comp);
  }
  function status(comp) {
    var b = S.statusbar, m = S.model, l = layer(), p = S.hover;
    b.innerHTML = '';
    if (!m) return;
    var inside = p && p[0] >= 0 && p[1] >= 0 && p[0] < m.w && p[1] < m.h;
    var c = inside && comp ? C.pixelAt(comp, m.w, p[0], p[1]) : null;
    var sw = h('i'); if (c) sw.style.background = cssColor(c); else sw.style.visibility = 'hidden';
    b.appendChild(h('span', { 'data-st': 'pos', text: inside ? p[0] + ', ' + p[1] : '–, –' }));
    b.appendChild(h('span', { 'data-st': 'color' }, [sw, c ? hexOf(c) : '–']));
    b.appendChild(h('span', { class: 'pe-dkst', 'data-st': 'size', text: m.w + '×' + m.h }));
    b.appendChild(h('span', { 'data-st': 'zoom', text: S.zoom + '×' }));
    var ls = h('span', { class: 'pe-st-sp', 'data-st': 'layer', text: l ? l.name : '' });
    b.appendChild(ls);
    if (l && !l.visible) b.appendChild(h('span', { class: 'pe-warn', text: T('hidden') }));
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

  /* ---------------- side panel (desktop: right column; phone: sheets colors / layers / more) */
  function swatch(c, cls, onPick, onSecond, extra) {
    var hx = hexOf(c);
    var b = h('button', Object.assign({ type: 'button', class: 'pe-sw' + (cls ? ' ' + cls : ''), title: hx, 'aria-label': hx }, extra || {}));
    b.appendChild(h('i', { style: 'background:' + cssColor(c) }));
    if (onPick) b.addEventListener('click', onPick);
    if (onSecond) b.addEventListener('contextmenu', function (e) { e.preventDefault(); onSecond(); });
    return b;
  }
  function setColor(c, second) { if (second) S.secondary = c.slice(); else S.primary = c.slice(); refreshColors(); }
  function useColor(c) {
    if (!c || !c[3]) return;
    var hx = hexOf(c);
    S.recent = [hx].concat(S.recent.filter(function (x) { return x !== hx; })).slice(0, 16);
  }
  function refreshChips() {
    (S.chips || []).forEach(function (ch) {
      ch.p.firstChild.style.background = cssColor(S.primary); ch.s.firstChild.style.background = cssColor(S.secondary);
      ch.p.title = T('primary') + ' ' + hexOf(S.primary); ch.s.title = T('secondary') + ' ' + hexOf(S.secondary) + ' – ' + tip('swap');
    });
  }
  function refreshColors() {
    refreshChips();
    if (!S.colorBox) return;
    var box = S.colorBox; box.innerHTML = '';
    var p = S.primary, hex = hexOf(p);
    var picker = h('input', { type: 'color', value: hex.slice(0, 7), 'aria-label': T('pickColor'), 'data-k': 'picker',
      oninput: function () { var c = C.parseColor(picker.value); c[3] = S.primary[3] || 255; S.primary = c; text.value = hexOf(c); prim.firstChild.style.background = cssColor(c); refreshChips(); },
      onchange: function () { refreshColors(); } });
    var text = h('input', { type: 'text', value: hex, 'aria-label': 'Hex #rrggbbaa', spellcheck: 'false', autocomplete: 'off', 'data-k': 'hex',
      onchange: function () { var c = C.parseColor(text.value); if (c) setColor(c); else text.value = hex; } });
    var aval = h('output', { text: Math.round(p[3] / 2.55) + '%' });
    var alpha = h('input', { type: 'range', min: 0, max: 255, value: p[3], 'aria-label': T('alpha'), 'data-k': 'alpha',
      oninput: function () { S.primary[3] = Number(alpha.value); text.value = hexOf(S.primary); prim.firstChild.style.background = cssColor(S.primary); aval.textContent = Math.round(S.primary[3] / 2.55) + '%'; refreshChips(); } });
    var prim = swatch(p, 'pe-big', function () { picker.click(); }, null, { title: T('primary'), 'aria-label': T('primary') + ' ' + hex });
    var sec = swatch(S.secondary, 'pe-mid', swap, null, { title: T('secondary') + ' – ' + tip('swap'), 'aria-label': T('secondary') + ' ' + hexOf(S.secondary) + ' – ' + T('swap') });
    box.appendChild(h('div', { class: 'pe-colors' }, [prim, h('div', { class: 'pe-row', style: 'flex-direction:column;gap:4px' }, [sec, ibtn('swap', tip('swap'), swap, { 'data-k': 'swap' })]),
      h('div', { class: 'pe-fields' }, [picker, text])]));
    box.appendChild(h('label', { class: 'pe-opacity' }, [T('alpha'), alpha, aval]));
    box.appendChild(h('div', { class: 'pe-sub', text: T('recent') }));
    box.appendChild(S.recent.length ? palette(S.recent, 'r') : h('p', { class: 'pe-note', text: T('noRecent') }));
  }
  function swap() { var t = S.primary; S.primary = S.secondary; S.secondary = t; refreshColors(); }
  function palette(list, key) {
    var g = h('div', { class: 'pe-pal', role: 'group' }), cur = hexOf(S.primary);
    list.forEach(function (hx, i) {
      var c = C.parseColor(hx);
      g.appendChild(swatch(c, '', function () { setColor(c); refreshSide(); }, function () { setColor(c, true); }, { 'aria-current': String(hx === cur), 'data-k': key + i }));
    });
    return g;
  }
  function section(sheetName, title, ic, kids, extra) {
    var hd = h('h3'); hd.innerHTML = icon(ic); hd.appendChild(document.createTextNode(title));
    return h('section', Object.assign({ 'data-sheet': sheetName }, extra || {}), [hd].concat(kids));
  }
  function refreshSide() {
    if (!S || !S.model) return;
    var side = S.side, m = S.model, o = S.opts;
    var fk = document.activeElement && side.contains(document.activeElement) ? document.activeElement.getAttribute('data-k') : null;
    var scroll = side.scrollTop;
    side.innerHTML = '';
    side.appendChild(h('div', { class: 'pe-sheethead' }, [h('span'), ibtn('close', T('closeSheet') + ' (Esc)', function () { sheet(null); })]));
    // colours and palettes
    S.colorBox = h('div');
    side.appendChild(section('colors', T('colors'), 'colors', [S.colorBox]));
    refreshColors();
    // layers (top first, like Resprite/Aseprite)
    var list = h('div', { class: 'pe-layers', role: 'listbox', 'aria-label': T('layers') });
    S.thumbs = []; S.rows = [];
    for (var n = m.layers.length - 1; n >= 0; n--) list.appendChild(layerRow(n));
    var l = layer();
    var op = h('input', { type: 'range', min: 0, max: 100, value: l.opacity, 'aria-label': T('opacity'), 'data-k': 'opacity' });
    var opBefore = null;
    op.addEventListener('pointerdown', function () { opBefore = C.cloneModel(m); });
    op.addEventListener('input', function () {
      if (!opBefore) opBefore = C.cloneModel(m);
      l.opacity = Number(op.value); opLabel.textContent = l.opacity + '%';
      var row = S.rows.filter(function (r) { return r.n === S.active; })[0]; if (row) row.op.textContent = l.opacity + '%';
      paint();
    });
    op.addEventListener('change', function () { if (opBefore) changed(opBefore); opBefore = null; });
    var opLabel = h('output', { text: l.opacity + '%' });
    side.appendChild(section('layers', T('layers') + ' (' + m.layers.length + ')', 'layers', [list,
      h('label', { class: 'pe-opacity' }, [T('opacity'), op, opLabel]),
      h('div', { class: 'pe-iconbar', role: 'toolbar', 'aria-label': T('layers') }, [
        ibtn('add', tip('add'), addLayer, { 'data-k': 'add' }), ibtn('dup', T('dup'), dupLayer, { 'data-k': 'dup' }),
        ibtn('up', T('up'), function () { moveLayer(1); }, { disabled: S.active >= m.layers.length - 1, 'data-k': 'up' }),
        ibtn('down', T('down'), function () { moveLayer(-1); }, { disabled: S.active <= 0, 'data-k': 'down' }),
        ibtn('merge', T('merge'), mergeLayer, { disabled: S.active <= 0, 'data-k': 'merge' }),
        ibtn('rename', tip('rename'), function () { startRename(S.active); }, { 'data-k': 'rename' }),
        ibtn('del', T('del'), delLayer, { disabled: m.layers.length < 2, cls: 'pe-danger', 'data-k': 'del' })]),
      h('p', { class: 'pe-note', text: T('layersHint') })]));
    // palettes below the layers, so the layer list stays in view on the desktop
    var own = C.colorsOf(m);
    side.appendChild(section('colors', T('palTex') + ' (' + own.length + ')', 'colors', [palette(own.slice(0, 256), 't'),
      h('p', { class: 'pe-note', text: T('palHint') }),
      h('div', { class: 'pe-row', style: 'margin-top:6px' }, [ibtn('exp', T('gpl'), function () { download(baseName() + '.gpl', C.gpl(baseName(), own), 'text/plain'); }, { cls: 'pe-soft', 'data-k': 'gpl' }, T('gpl'))])]));
    side.appendChild(section('colors', T('palVan'), 'colors', [palette(C.VANILLA_NEAR, 'v')]));
    // view (phone only; the desktop has these in the header)
    side.appendChild(section('more', T('view'), 'grid', [h('div', { class: 'pe-row' }, [ibtn('grid', tip('grid'), toggleGrid, { 'data-toggle': 'grid', cls: 'pe-soft', 'data-k': 'grid' }, T('grid'))])], { class: 'pe-phone-only' }));
    // region transforms (also in the context bar of Select/Move)
    side.appendChild(section('more', T('region'), 'select', [h('div', { class: 'pe-iconbar' }, regionButtons())]));
    // previews
    S.prevs = [{ canvas: h('canvas'), scale: 1 }, { canvas: h('canvas'), scale: 2 }, { canvas: h('canvas'), scale: 'auto', tile: true }];
    side.appendChild(section('more', T('preview'), 'image', [h('div', { class: 'pe-prev' }, [
      h('figure', null, [S.prevs[0].canvas, h('figcaption', { text: T('p1') })]),
      h('figure', null, [S.prevs[1].canvas, h('figcaption', { text: T('p2') })]),
      h('figure', null, [S.prevs[2].canvas, h('figcaption', { text: T('tile') })])])]));
    // reference image (kept only in this page)
    var refIn = h('input', { type: 'file', class: 'pe-file', tabindex: -1, 'aria-hidden': 'true', accept: 'image/*', onchange: function () { var f = refIn.files[0]; if (f) { if (S.ref) URL.revokeObjectURL(S.ref); S.ref = URL.createObjectURL(f); refreshSide(); } } });
    var refBox = h('div', { class: 'pe-ref' }, [refIn, ibtn('image', T('refAdd'), function () { refIn.click(); }, { cls: 'pe-soft', 'data-k': 'ref' }, T('refAdd'))]);
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
    side.appendChild(section('more', T('ref'), 'image', [refBox]));
    // export + file
    var src = o.source || '';
    var keys = h('dl', { class: 'pe-keys' });
    T('k').forEach(function (k) { keys.appendChild(h('dt', { text: k[0] })); keys.appendChild(h('dd', { text: k[1] })); });
    side.appendChild(section('more', T('exp'), 'exp', [
      h('div', { class: 'pe-row' }, [ibtn('exp', T('exp') + ': ' + T('both'), exportBoth, { cls: 'pe-primary', 'data-k': 'expBoth' }, T('both')),
        ibtn('exp', T('png'), exportPng, { cls: 'pe-soft', 'data-k': 'expPng' }, T('png')), ibtn('exp', T('json'), exportJson, { cls: 'pe-soft', 'data-k': 'expJson' }, T('json')),
        ibtn('open', T('open'), askOpen, { cls: 'pe-soft', 'data-k': 'open' }, T('open'))]),
      /^build\/vanilla-derived\//.test(src) ? h('p', { class: 'pe-warn', text: T('derived') }) : null,
      canSave() ? h('div', { class: 'pe-save' }, [
        h('label', { class: 'pe-note', for: 'pe-save-comment', text: T('saveComment') }),
        S.commentIn = h('input', { type: 'text', id: 'pe-save-comment', maxlength: '500', value: S.saveComment || '', oninput: function () { S.saveComment = this.value; } }),
        h('div', { class: 'pe-row' }, [ibtn('save', T('saveTip'), saveToRepo, { cls: 'pe-primary', 'data-k': 'save' }, T('save'))]),
        S.saveNote = h('p', { class: 'pe-note', role: 'status', 'aria-live': 'polite', 'data-k': 'saveNote', text: S.saveMsg || '' })]) : null,
      h('p', { class: 'pe-note', text: T('howto', { png: baseName() + '.png', json: baseName() + C.SUFFIX, src: src || '?' }) }),
      S.from ? h('p', { class: 'pe-note', text: S.from }) : null,
      h('details', null, [h('summary', { text: T('keysTitle') }), keys])]));
    refreshSheetHead();
    side.scrollTop = scroll;
    if (fk) { var f = side.querySelector('[data-k="' + fk + '"]'); if (f) f.focus(); }
    var ri = side.querySelector('.pe-layer input'); if (ri) { ri.focus(); ri.select(); }
    paint();
  }
  function layerRow(n) {
    var m = S.model, l = m.layers[n], th = h('canvas', { 'aria-hidden': 'true' });
    S.thumbs.push({ n: n, canvas: th });
    var eye = ibtn(l.visible ? 'eye' : 'eyeOff', l.visible ? T('show') : T('hide'), function (e) { e.stopPropagation(); var b = C.cloneModel(m); l.visible = !l.visible; changed(b); },
      { 'aria-pressed': String(l.visible), 'data-k': 'eye' + n });
    var lock = ibtn(l.alphaLock ? 'lock' : 'unlock', l.alphaLock ? T('lock') : T('unlock'), function (e) { e.stopPropagation(); l.alphaLock = !l.alphaLock; refreshSide(); },
      { 'aria-pressed': String(!!l.alphaLock), cls: l.alphaLock ? 'pe-on' : '', 'data-k': 'lock' + n });
    var grip = h('span', { class: 'pe-btn pe-icon pe-grip', title: T('drag'), 'aria-hidden': 'true', html: icon('grip') });
    var label;
    if (S.renaming === n) {
      label = h('input', { type: 'text', value: l.name, 'aria-label': T('layerName'), 'data-k': 'rn' });
      var done = false;
      var commit = function (ok) {
        if (done) return; done = true;
        var name = label.value.trim(); S.renaming = -1;
        if (ok && name && name !== l.name) { var b = C.cloneModel(m); l.name = name; changed(b); } else refreshSide();
        var row = S.side.querySelector('[data-k="layer' + n + '"]'); if (row) row.focus();
      };
      label.addEventListener('keydown', function (e) { e.stopPropagation(); if (e.key === 'Enter') { e.preventDefault(); commit(true); } else if (e.key === 'Escape') { e.preventDefault(); commit(false); } });
      label.addEventListener('blur', function () { setTimeout(function () { if (label.isConnected) commit(true); }, 0); });
      label.addEventListener('click', function (e) { e.stopPropagation(); });
    } else label = h('span', { class: 'pe-ln', text: l.name });
    var opTxt = h('span', { class: 'pe-op', text: l.opacity + '%' });
    var row = h('div', { class: 'pe-layer' + (l.visible ? '' : ' pe-hid'), role: 'option', 'aria-selected': String(n === S.active), tabindex: n === S.active ? 0 : -1,
      'data-k': 'layer' + n, 'aria-label': l.name + ', ' + l.opacity + '%' + (l.visible ? '' : ', ' + T('hide')),
      onclick: function () { if (S.active !== n) { S.active = n; refreshSide(); } },
      ondblclick: function () { startRename(n); },
      onkeydown: function (e) { rowKey(e, n); } },
      [grip, eye, th, label, opTxt, lock]);
    grip.addEventListener('pointerdown', function (e) { startDrag(e, n, row); });
    S.rows.push({ n: n, el: row, op: opTxt });
    return row;
  }
  function rowKey(e, n) {
    var m = S.model;
    if (e.target.tagName === 'INPUT') return;
    if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); e.stopPropagation(); S.active = n; refreshSide(); }
    else if (e.key === 'F2') { e.preventDefault(); e.stopPropagation(); startRename(n); }
    else if ((e.key === 'ArrowUp' || e.key === 'ArrowDown') && e.altKey) { e.preventDefault(); e.stopPropagation(); S.active = n; moveLayer(e.key === 'ArrowUp' ? 1 : -1); }
    else if ((e.key === 'ArrowUp' || e.key === 'ArrowDown') && !e.shiftKey) {
      e.preventDefault(); e.stopPropagation();
      var k = Math.max(0, Math.min(m.layers.length - 1, n + (e.key === 'ArrowUp' ? 1 : -1)));
      S.active = k; refreshSide();
      var r = S.side.querySelector('[data-k="layer' + k + '"]'); if (r) r.focus();
    }
  }
  function startRename(n) { S.renaming = n; S.active = n; refreshSide(); }
  /* drag sorting with pointer events (works with touch); rows are shown top layer first */
  function startDrag(e, n, row) {
    e.preventDefault(); e.stopPropagation();
    var rows = S.rows.map(function (r) { return r.el; }), from = S.rows.map(function (r) { return r.n; }).indexOf(n);
    try { e.target.setPointerCapture(e.pointerId); } catch (err) { /* synthetic pointer */ }
    row.classList.add('pe-dragging');
    var pos = from;
    function where(y) { var p = 0; rows.forEach(function (r) { var b = r.getBoundingClientRect(); if (y > b.top + b.height / 2) p++; }); return p; }
    function move(ev) {
      pos = where(ev.clientY);
      var to = pos > from ? pos - 1 : pos;
      rows.forEach(function (r, i) {
        r.classList.toggle('pe-drop-above', to !== from && i === pos && pos < rows.length);
        r.classList.toggle('pe-drop-below', to !== from && pos === rows.length && i === rows.length - 1);
      });
    }
    function up() {
      window.removeEventListener('pointermove', move, true); window.removeEventListener('pointerup', up, true); window.removeEventListener('pointercancel', up, true);
      rows.forEach(function (r) { r.classList.remove('pe-drop-above', 'pe-drop-below', 'pe-dragging'); });
      var to = pos > from ? pos - 1 : pos;
      if (to !== from) moveLayerTo(n, S.model.layers.length - 1 - to);
    }
    window.addEventListener('pointermove', move, true); window.addEventListener('pointerup', up, true); window.addEventListener('pointercancel', up, true);
  }

  /* ---------------- layer operations */
  function addLayer() { var b = C.cloneModel(S.model); S.model.layers.splice(S.active + 1, 0, C.newLayer(S.model.w, S.model.h, T('layer') + ' ' + (S.model.layers.length + 1))); S.active++; changed(b); }
  function dupLayer() { var b = C.cloneModel(S.model), l = layer(), c = C.cloneModel({ w: 0, h: 0, layers: [l] }).layers[0]; c.name = l.name + ' 2'; S.model.layers.splice(S.active + 1, 0, c); S.active++; changed(b); }
  function delLayer() { if (S.model.layers.length < 2) return; var b = C.cloneModel(S.model); S.model.layers.splice(S.active, 1); S.active = Math.max(0, S.active - 1); changed(b); }
  function moveLayer(d) { moveLayerTo(S.active, S.active + d); }
  /* move layer from index a to index b (model order, 0 = bottom) */
  function moveLayerTo(a, b) {
    var ls = S.model.layers;
    if (b < 0 || b >= ls.length || a === b) return;
    var before = C.cloneModel(S.model), l = ls.splice(a, 1)[0]; ls.splice(b, 0, l); S.active = b; changed(before);
  }
  function mergeLayer() { var b = C.cloneModel(S.model); if (C.mergeDown(S.model, S.active)) { S.active--; changed(b); } }
  function region() { return S.sel ? C.clipRect(S.sel, S.model.w, S.model.h) : { x: 0, y: 0, w: S.model.w, h: S.model.h }; }
  function transform(fn) {
    if (!S.model) return;
    var r = region(), b = C.cloneModel(S.model);
    if (!r) return;
    if (C.transformRect(layer().px, S.model.w, S.model.h, r, fn)) changed(b); else toast(T('rotSquare'));
  }
  function clearSel() { if (!S.model) return; var r = region(), b = C.cloneModel(S.model); if (r) { C.clearRect(layer().px, S.model.w, r); changed(b); } }
  function copySel() { if (!S.model) return; var r = region(); if (r) { S.clip = Object.assign(C.copyRect(layer().px, S.model.w, r), { x: r.x, y: r.y }); toast(T('copied')); } }
  function selectAll() { if (S.model) { S.sel = { x: 0, y: 0, w: S.model.w, h: S.model.h }; paint(); } }
  function deselect() { S.sel = null; paint(); }
  function paste() {
    if (!S.model) return;
    if (!S.clip) { toast(T('nothing')); return; }
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
  function exportBoth() { if (S.model) exportPng().then(function () { setTimeout(exportJson, 400); }); }
  /* TX22-5: only in the dev wiki (dev overlay loaded) and only for own textures with a repository source */
  function canSave() { var src = (S && S.opts.source) || ''; return !!window.sbDevHook && !!src && !/^build\//.test(src); }
  function setSaveNote(text) { S.saveMsg = text; if (S.saveNote) S.saveNote.textContent = text; }
  function b64(bytes) { var s = ''; for (var i = 0; i < bytes.length; i += 0x8000) s += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000)); return btoa(s); }
  function saveToRepo() {
    if (!S || !S.model || !canSave() || S.saving) return;
    var m = S.model, doc = C.toDoc(m), mine = S;
    m.palette = doc.palette;
    S.saving = true; setSaveNote(T('saving'));
    C.pngEncode(m.w, m.h, C.compose(m)).then(function (bytes) {
      return fetch('/api/textures', { method: 'POST', credentials: 'same-origin', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: mine.opts.id, source: mine.opts.source, layers: C.dumps(doc), png: b64(bytes), comment: mine.saveComment || '' }) });
    }).then(function (r) {
      return r.json().catch(function () { return {}; }).then(function (j) { if (!r.ok) throw new Error(j.error || ('HTTP ' + r.status)); return j; });
    }).then(function (save) {
      if (S !== mine) return;
      S.saving = false; S.dirty = false; toast(T('saved')); setSaveNote(T('st_new'));
      pollSave(mine, save.sid, 0);
    }).catch(function (e) {
      if (S !== mine) return;
      S.saving = false; setSaveNote(T('saveFail') + (e && e.message || e)); toast(T('saveFail') + (e && e.message || e));
    });
  }
  function pollSave(mine, sid, n) {
    if (S !== mine || !sid) return;
    fetch('/api/textures/' + encodeURIComponent(sid), { credentials: 'same-origin' }).then(function (r) { return r.ok ? r.json() : null; }).then(function (s) {
      if (S !== mine || !s) return;
      setSaveNote(T('st_' + s.state, { b: s.branch || '', n: s.note || '' }));
      if ((s.state === 'new' || s.state === 'picked') && n < 120) S.pollT = setTimeout(function () { pollSave(mine, sid, n + 1); }, 5000);
    }).catch(function () { /* status stays */ });
  }
  function askOpen() {
    if (S.dirty) ask(T('dirty'), T('discard')).then(function (yes) { if (yes) S.fileIn.click(); });
    else S.fileIn.click();
  }
  function openFile(f) {
    if (!f) return;
    f.arrayBuffer().then(function (buf) {
      var bytes = new Uint8Array(buf);
      if (/\.json$/i.test(f.name)) return C.fromDoc(JSON.parse(new TextDecoder().decode(bytes)));
      return decodePng(bytes).then(function (img) { return C.fromRgba(img.w, img.h, img.rgba, T('base')); });
    }).then(function (model) {
      if (S.model) record();
      setModel(model, '');
    }).catch(function (e) { toast(T('fileFail') + (e && e.message || e)); });
  }

  /* ---------------- pointer input */
  function toPixel(e) {
    var r = S.canvas.getBoundingClientRect();
    return [Math.floor((e.clientX - r.left - S.ox) / S.zoom), Math.floor((e.clientY - r.top - S.oy) / S.zoom)];
  }
  function stagePoint(e) { var r = S.canvas.getBoundingClientRect(); return [e.clientX - r.left, e.clientY - r.top]; }
  function pick(p, second) {
    var m = S.model;
    if (p[0] < 0 || p[1] < 0 || p[0] >= m.w || p[1] >= m.h) return;
    setColor(C.pixelAt(C.compose(m), m.w, p[0], p[1]), second);
  }
  function bindPointer() {
    var cv = S.canvas;
    cv.addEventListener('contextmenu', function (e) { e.preventDefault(); });
    cv.addEventListener('wheel', function (e) {
      e.preventDefault();
      if (!S.model) return;
      var p = stagePoint(e);
      zoomAt(S.zoom + (e.deltaY < 0 ? 1 : -1) * Math.max(1, Math.round(S.zoom / 5)), p[0], p[1]);
    }, { passive: false });
    cv.addEventListener('pointerdown', function (e) {
      if (!S.model) return;
      if (S.sheet && isPhone()) sheet(null);
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
    sync(); paint();
  }

  function effTool(e) { return (e && e.altKey) || S.alt ? 'picker' : S.tool; }
  function colorFor(e) { return e && e.button === 2 ? S.secondary : S.primary; }
  function beginStroke(p, e) {
    var l = layer(); if (!l) return;
    var tool = effTool(e), m = S.model;
    S.stroke = { tool: tool, start: p, last: p, path: [p], before: C.cloneModel(m), base: new Uint8ClampedArray(l.px), color: colorFor(e), changed: false, global: S.global || (e && e.shiftKey) };
    if (tool === 'picker') { pick(p, e && e.button === 2); return; }
    if (tool === 'fill') {
      var pts = C.fill(l.px, m.w, m.h, p[0], p[1], S.stroke.color, S.stroke.global, l.alphaLock);
      S.stroke.changed = pts.length > 0; paint(); return;
    }
    if (tool === 'select') { S.sel = { x: p[0], y: p[1], w: 1, h: 1 }; paint(); return; }
    if (tool === 'move') {
      var r = region(); S.stroke.region = r;
      S.stroke.buf = r ? C.copyRect(l.px, m.w, r) : null; S.stroke.sel = S.sel; return;
    }
    // Shift+click: straight line from the end of the previous freehand stroke (Aseprite)
    if ((tool === 'pencil' || tool === 'eraser') && e && e.shiftKey && S.lastPt) S.stroke.path = C.linePoints(S.lastPt[0], S.lastPt[1], p[0], p[1]);
    else if (tool === 'pencil' && e && e.pointerType !== 'mouse') {
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
    if (st.tool === 'pencil' || st.tool === 'eraser') pts = S.pp && S.size === 1 ? C.pixelPerfect(st.path) : st.path;
    else if (st.tool === 'line') pts = C.linePoints(st.start[0], st.start[1], p[0], p[1]);
    else if (st.tool === 'rect') pts = C.rectPoints(st.start[0], st.start[1], p[0], p[1], S.filled);
    else return;
    l.px.set(st.base);
    C.plot(l.px, m.w, m.h, C.mirror(C.brush(pts, S.size), m.w, m.h, S.sym), st.tool === 'eraser' ? null : st.color, l.alphaLock);
    st.changed = true;
    paint();
  }
  function endStroke() {
    var st = S.stroke; S.stroke = null;
    if (st.long) clearTimeout(st.long);
    if (st.tool === 'pencil' || st.tool === 'eraser') S.lastPt = st.last;
    if (st.changed) {
      if (st.tool !== 'eraser' && st.tool !== 'select' && st.tool !== 'move') useColor(st.color);
      changed(st.before);
    }
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
    var t = e.target, typing = t && (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA' || t.tagName === 'SELECT') && t.type !== 'range' && t.type !== 'checkbox' && t.type !== 'color';
    var row = t && t.getAttribute && t.getAttribute('role') === 'option';
    // keys inside the editor still reach their element (rows, rename field); S.root stops them in the bubble phase
    if (!S.root.contains(t)) e.stopPropagation();
    if (S.dialog) { if (e.key === 'Escape') { e.preventDefault(); S.dialog.finish(false); } return; }
    if (e.key === 'Escape') {
      if (typing && S.renaming >= 0) return;
      e.preventDefault();
      if (S.sheet) sheet(null); else if (S.sel) { S.sel = null; paint(); } else close(false);
      return;
    }
    if (typing || !S.model) return;
    var k = e.key.toLowerCase(), mod = e.ctrlKey || e.metaKey;
    if (e.key === 'Alt') { S.alt = true; e.preventDefault(); paint(); return; }
    if (row && /^(Enter| |F2|ArrowUp|ArrowDown)$/.test(e.key)) return; // handled by the layer row
    if (e.key === ' ' && !(t && t.tagName === 'BUTTON')) {
      if (S.space) { e.preventDefault(); return; }
      S.space = true; e.preventDefault();
      window.addEventListener('keyup', function sp(ev) { if (ev.key === ' ') { if (S) S.space = false; window.removeEventListener('keyup', sp, true); } }, true);
      return;
    }
    if (mod && k === 'z') { e.preventDefault(); if (e.shiftKey) redo(); else undo(); return; }
    if (mod && k === 'y') { e.preventDefault(); redo(); return; }
    if (mod && k === 'c') { e.preventDefault(); copySel(); return; }
    if (mod && k === 'v') { e.preventDefault(); paste(); return; }
    if (mod && k === 'd') { e.preventDefault(); deselect(); return; }
    if (mod && k === 'a') { e.preventDefault(); selectAll(); return; }
    if (mod && (e.key === "'" || e.code === 'Backslash' || e.code === 'Quote')) { e.preventDefault(); toggleGrid(); return; }
    if (mod || e.altKey) return;
    if (e.shiftKey && k === 'n') { e.preventDefault(); addLayer(); return; }
    var tool = TOOLS.filter(function (x) { return x[1] === k; })[0];
    if (tool && !e.shiftKey) { e.preventDefault(); setTool(tool[0]); return; }
    if (k === 'x') { e.preventDefault(); swap(); return; }
    if (e.key === '[' || e.key === ']') { e.preventDefault(); setSize(S.size + (e.key === ']' ? 1 : -1)); return; }
    if (e.key === '+' || e.key === '=') { e.preventDefault(); zoomStep(1); return; }
    if (e.key === '-') { e.preventDefault(); zoomStep(-1); return; }
    if (e.key === '0') { e.preventDefault(); fit(); paint(); return; }
    if (e.key === 'Delete' || e.key === 'Backspace') { e.preventDefault(); clearSel(); return; }
    var arrows = { ArrowLeft: [-1, 0], ArrowRight: [1, 0], ArrowUp: [0, -1], ArrowDown: [0, 1] }[e.key];
    if (arrows) {
      if (t && t.type === 'range') return;
      e.preventDefault();
      if (e.shiftKey) { S.active = Math.max(0, Math.min(S.model.layers.length - 1, S.active + (arrows[1] < 0 || arrows[0] > 0 ? 1 : -1))); refreshSide(); paint(); }
      else nudge(arrows[0], arrows[1]);
    }
  }

  window.SBPixelEditor = { open: open, close: function () { close(true); }, state: function () { return S; }, icons: ICONS };
})();
