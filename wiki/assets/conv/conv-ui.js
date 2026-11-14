/* conv-ui.js (N51 prototype): page glue for "Bauwerke" (data-conv="blueprint") and "3D-Modelle" (data-conv="model").
 * Everything runs in the browser; nothing is uploaded. */
(function () {
  'use strict';
  var C = window.Conv51, lang = /^de/i.test(navigator.language || '') ? 'de' : 'en';
  var L = {
    en: { drop: 'Drop a file here or click to choose', bpFormats: 'Supported in this prototype: Vanilla structure .nbt (gzip or plain). Planned: .schem, .schematic, .litematic, .bp.',
      mdFormats: 'Supported in this prototype: Vanilla model .json and Blockbench .bbmodel. Planned: .obj, .gltf/.glb.', sample: 'Load example',
      layer: 'Layer', size: 'Size', blocks: 'Blocks', kinds: 'Block kinds', code: 'Blueprint code', copy: 'Copy', copied: 'Copied', part: 'Part',
      limits: 'Limits', limCode: 'Code length: pasting/editor max 32,000 characters; bigger buildings are split into parts (bottom to top).',
      limGrid: 'Grid 256 x 256 x 256 per blueprint (largest wand: Enderite, edge 256).', limTier: 'Needed building wand', creative: 'Creative blueprint needed for blocks without an item.',
      lostBE: 'Block entities (chests, signs ...): only the block state is kept, contents are dropped.', lostEnt: 'Entities are not part of a blueprint.',
      skip: 'Skipped (air, structure void ...)', unk: 'Block states written with all properties (default unknown to this prototype)',
      tooBig: 'This building is larger than 256 blocks on one axis. A blueprint holds at most 256; cutting into tiles comes in phase 2.',
      errPart: 'One layer alone is longer than the code limit; this building cannot be turned into a code.', errFile: 'This file could not be read: ', errGz: 'Your browser cannot unpack gzip. Use a current browser.',
      model: 'Simple Models code (vanilla model JSON)', elements: 'Cubes', textures: 'Textures', yaw: 'Turn',
      texNote: 'Texture images are not part of the code: supply the PNGs in a resource pack under the shown ids.', devOnly: 'This page is not public yet (development wiki only).', warn: 'Notes',
      w_non_cube: 'Non-cube elements are skipped', w_box_uv: 'Box UV model: UV taken from the saved faces', w_shade_dropped: '"shade": false is not supported and was dropped', w_face_without_texture: 'A face points to a missing texture and was skipped' },
    de: { drop: 'Datei hier ablegen oder klicken', bpFormats: 'Im Prototyp: Vanilla-Strukturdatei .nbt (gzip oder unkomprimiert). Geplant: .schem, .schematic, .litematic, .bp.',
      mdFormats: 'Im Prototyp: Vanilla-Modell .json und Blockbench .bbmodel. Geplant: .obj, .gltf/.glb.', sample: 'Beispiel laden',
      layer: 'Schicht', size: 'Größe', blocks: 'Blöcke', kinds: 'Blockarten', code: 'Blaupausen-Code', copy: 'Kopieren', copied: 'Kopiert', part: 'Teil',
      limits: 'Grenzen', limCode: 'Codelänge: Einfügen/Editor höchstens 32 000 Zeichen; größere Bauwerke werden in Teile geschnitten (von unten nach oben).',
      limGrid: 'Raster 256 x 256 x 256 je Blaupause (größter Baustab: Enderit, Kante 256).', limTier: 'Nötiger Baustab', creative: 'Für Blöcke ohne Item braucht es die Kreativ-Blaupause.',
      lostBE: 'Block-Entities (Truhen, Schilder ...): nur der Blockzustand bleibt, Inhalte entfallen.', lostEnt: 'Entities gehören nicht in eine Blaupause.',
      skip: 'Übersprungen (Luft, Strukturleere ...)', unk: 'Zustände mit allen Eigenschaften geschrieben (Standardwert dem Prototyp unbekannt)',
      tooBig: 'Das Bauwerk ist auf einer Achse größer als 256 Blöcke. Eine Blaupause fasst höchstens 256; Zerschneiden in Kacheln kommt in Phase 2.',
      errPart: 'Eine einzelne Schicht ist länger als die Codegrenze; dieses Bauwerk lässt sich nicht in Code fassen.', errFile: 'Diese Datei konnte nicht gelesen werden: ', errGz: 'Dein Browser kann kein gzip entpacken. Bitte einen aktuellen Browser benutzen.',
      model: 'Simple-Models-Code (Vanilla-Modell-JSON)', elements: 'Quader', textures: 'Texturen', yaw: 'Drehen',
      texNote: 'Texturbilder gehören nicht in den Code: die PNGs als Ressourcenpaket unter den gezeigten Ids bereitstellen.', devOnly: 'Diese Seite ist noch nicht öffentlich (nur Dev-Wiki).', warn: 'Hinweise',
      w_non_cube: 'Elemente, die keine Quader sind, werden übersprungen', w_box_uv: 'Box-UV-Modell: UV aus den gespeicherten Flächen übernommen', w_shade_dropped: '"shade": false wird nicht unterstützt und entfiel', w_face_without_texture: 'Eine Fläche zeigt auf eine fehlende Textur und wurde übersprungen' }
  };
  function T(k) { return (L[lang][k] != null ? L[lang][k] : L.en[k] || k); }
  function esc(s) { return String(s).replace(/[&<>"]/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]; }); }
  function hue(name) { var h = 0; for (var i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) >>> 0; return h % 360; }
  function isDev() { return /^\/dev(\/|$)/.test(location.pathname) || /[?&]dev=1(&|$)/.test(location.search) || location.protocol === 'file:' || !!window.CONV51_PREVIEW; }
  function b64(s) { var b = atob(s), u = new Uint8Array(b.length); for (var i = 0; i < b.length; i++) u[i] = b.charCodeAt(i); return u; }

  function init(root) {
    var mode = root.getAttribute('data-conv');
    if (!isDev()) { root.innerHTML = '<p>' + esc(T('devOnly')) + '</p>'; return; }
    root.innerHTML = '<div class="cv-drop" tabindex="0" role="button">' + esc(T('drop')) + '<input type="file" hidden></div><p class="cv-hint">' + esc(T(mode === 'blueprint' ? 'bpFormats' : 'mdFormats')) +
      ' <button type="button" class="cv-sample">' + esc(T('sample')) + '</button></p><div class="cv-out" aria-live="polite"></div>';
    var drop = root.querySelector('.cv-drop'), input = root.querySelector('input'), out = root.querySelector('.cv-out');
    drop.onclick = function () { input.click(); }; drop.onkeydown = function (e) { if (e.key === 'Enter' || e.key === ' ') input.click(); };
    drop.ondragover = function (e) { e.preventDefault(); }; drop.ondrop = function (e) { e.preventDefault(); if (e.dataTransfer.files[0]) take(e.dataTransfer.files[0]); };
    input.onchange = function () { if (input.files[0]) take(input.files[0]); };
    root.querySelector('.cv-sample').onclick = function () {
      var s = window.CONV51_SAMPLES && window.CONV51_SAMPLES[mode];
      if (s) handle(s.name, b64(s.b64)); else fetch(mode === 'blueprint' ? 'tests/conv51/examples/hut.nbt' : 'tests/conv51/examples/stool.bbmodel').then(function (r) { return r.arrayBuffer(); }).then(function (a) { handle('example', new Uint8Array(a)); });
    };
    function take(f) { f.arrayBuffer().then(function (a) { handle(f.name, new Uint8Array(a)); }); }
    function fail(e) { out.innerHTML = '<p class="cv-err">' + esc(e.message === 'no_gzip' ? T('errGz') : T('errFile') + e.message) + '</p>'; }
    function handle(name, bytes) {
      try {
        if (mode === 'blueprint') C.ungzip(bytes).then(function (raw) { try { showBlueprint(C.structureToModel(C.readNbt(raw))); } catch (e) { fail(e); } }, fail);
        else showModel(name, new TextDecoder().decode(bytes));
      } catch (e) { fail(e); }
    }
    function copyBtn(ta, btn) { btn.onclick = function () { ta.select(); (navigator.clipboard ? navigator.clipboard.writeText(ta.value) : Promise.reject()).catch(function () { document.execCommand('copy'); }); btn.textContent = T('copied'); setTimeout(function () { btn.textContent = T('copy'); }, 1200); }; }

    function showBlueprint(m) {
      var info = m.info, kinds = {}; m.cells.forEach(function (s) { kinds[s] = (kinds[s] || 0) + 1; });
      var res = m.n && !info.tooBig ? C.toCodes(m) : { parts: [], error: null };
      var tier = [16, 32, 48, 64, 128, 256].filter(function (e) { return Math.max(m.sx, m.sy, m.sz) <= e; })[0];
      var h = '<h3>' + T('size') + ': ' + m.sx + ' x ' + m.sy + ' x ' + m.sz + ' · ' + T('blocks') + ': ' + m.n + ' · ' + T('kinds') + ': ' + Object.keys(kinds).length + '</h3>';
      h += '<div class="cv-prev"><canvas width="360" height="360"></canvas><label>' + T('layer') + ' <input type="range" min="0" max="' + Math.max(0, m.sy - 1) + '" value="0"> <span class="cv-ly">0</span></label></div>';
      var notes = [];
      if (info.tooBig) notes.push(T('tooBig')); if (res.error) notes.push(T('errPart'));
      if (info.blockEntities) notes.push(T('lostBE') + ' (' + info.blockEntities + ')'); if (info.entities) notes.push(T('lostEnt') + ' (' + info.entities + ')');
      if (info.skipped) notes.push(T('skip') + ': ' + info.skipped);
      var unk = Object.keys(info.unknownDefaults); if (unk.length) notes.push(T('unk') + ': ' + unk.slice(0, 6).join(', '));
      notes.push(T('limTier') + ': ' + (tier ? tier + ' (' + T('limGrid') + ')' : '—'), T('limCode'), T('creative'));
      h += '<h4>' + T('limits') + '</h4><ul>' + notes.map(function (n) { return '<li>' + esc(n) + '</li>'; }).join('') + '</ul>';
      res.parts.forEach(function (p, i) {
        h += '<h4>' + T('code') + (res.parts.length > 1 ? ' – ' + T('part') + ' ' + (i + 1) + '/' + res.parts.length : '') + ' (' + p.code.length + ')</h4><textarea readonly rows="8" class="cv-code">' + esc(p.code) + '</textarea><button type="button" class="cv-copy">' + T('copy') + '</button>';
      });
      out.innerHTML = h;
      out.querySelectorAll('.cv-code').forEach(function (ta) { copyBtn(ta, ta.nextElementSibling); });
      var cv = out.querySelector('canvas'), ctx = cv.getContext('2d'), rng = out.querySelector('input'), ly = out.querySelector('.cv-ly');
      function draw() {
        var y = +rng.value; ly.textContent = y; ctx.clearRect(0, 0, 360, 360);
        var s = Math.max(1, Math.floor(360 / Math.max(m.sx, m.sz, 1)));
        for (var z = 0; z < m.sz; z++) for (var x = 0; x < m.sx; x++) {
          var sp = m.cells.get(C.key(x, y, z)); if (!sp) continue;
          ctx.fillStyle = 'hsl(' + hue(sp.split('[')[0]) + ',55%,52%)'; ctx.fillRect(x * s, z * s, s - (s > 3 ? 1 : 0), s - (s > 3 ? 1 : 0));
        }
      }
      rng.oninput = draw; draw();
    }

    function showModel(name, text) {
      var src, warns = [], w = function (k) { warns.push(k); };
      if (/\.bbmodel$/i.test(name) || /"meta"\s*:/.test(text.slice(0, 400))) src = C.bbmodelToVanilla(JSON.parse(text), w); else src = JSON.parse(text);
      var code = C.modelCode(src, w), els = src.elements || [];
      var h = '<h3>' + T('elements') + ': ' + els.length + ' · ' + T('textures') + ': ' + Object.keys(src.textures || {}).length + '</h3>';
      h += '<div class="cv-prev"><canvas width="360" height="300"></canvas><label>' + T('yaw') + ' <input type="range" min="0" max="359" value="35"></label></div>';
      h += '<p>' + esc(T('texNote')) + '</p>';
      if (warns.length) h += '<h4>' + T('warn') + '</h4><ul>' + warns.map(function (k) { return '<li>' + esc(T('w_' + k.split(':')[0])) + '</li>'; }).join('') + '</ul>';
      h += '<h4>' + T('model') + ' (' + code.length + ' / 65536)</h4><textarea readonly rows="12" class="cv-code">' + esc(code) + '</textarea><button type="button" class="cv-copy">' + T('copy') + '</button>';
      out.innerHTML = h; copyBtn(out.querySelector('.cv-code'), out.querySelector('.cv-copy'));
      var cv = out.querySelector('canvas'), ctx = cv.getContext('2d'), rng = out.querySelector('input');
      function draw() {
        var a = rng.value * Math.PI / 180, ca = Math.cos(a), sa = Math.sin(a); ctx.clearRect(0, 0, 360, 300);
        var quads = [];
        els.forEach(function (e, i) {
          var f = e.from, t = e.to, pts = [];
          for (var k = 0; k < 8; k++) { var x = (k & 1 ? t[0] : f[0]) - 8, y = (k & 2 ? t[1] : f[1]) - 8, z = (k & 4 ? t[2] : f[2]) - 8; pts.push([x * ca - z * sa, y, x * sa + z * ca]); }
          [[0, 1, 3, 2], [4, 5, 7, 6], [0, 1, 5, 4], [2, 3, 7, 6], [0, 2, 6, 4], [1, 3, 7, 5]].forEach(function (q) {
            var d = q.reduce(function (s, p) { return s + pts[p][2]; }, 0);
            quads.push({ d: d, p: q.map(function (p) { return [180 + pts[p][0] * 9 + pts[p][2] * 3.5, 150 - pts[p][1] * 9 + pts[p][2] * 3.5]; }), h: hue(e.name || String(i)) });
          });
        });
        quads.sort(function (p, q) { return q.d - p.d; });
        quads.forEach(function (q) { ctx.beginPath(); q.p.forEach(function (p, i) { i ? ctx.lineTo(p[0], p[1]) : ctx.moveTo(p[0], p[1]); }); ctx.closePath(); ctx.fillStyle = 'hsl(' + q.h + ',45%,60%)'; ctx.fill(); ctx.strokeStyle = '#333'; ctx.stroke(); });
      }
      rng.oninput = draw; draw();
    }
  }
  function boot() { document.querySelectorAll('[data-conv]').forEach(init); }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot); else boot();
})();
