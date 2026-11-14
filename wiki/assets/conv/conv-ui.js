/* conv-ui.js (N51, b-conv51/b-conv51b): page glue (standalone or mounted by the wiki SPA) for "Bauwerke" (data-conv="blueprint") and "3D-Modelle" (data-conv="model").
 * Everything runs in the browser; files are read into memory only, nothing is uploaded, no other host is contacted. */
(function () {
  'use strict';
  /* Release switch: stays false until the owner releases the converters; until then only the dev wiki shows them. */
  var CONV51_PUBLIC = false;
  var C = window.Conv51;
  var lang = (function () {
    try {
      var q = new URLSearchParams(location.search).get('lang'); if (q === 'en' || q === 'de') return q;
      var s = localStorage.getItem('simplebuilding-wiki-lang'); if (s === 'en' || s === 'de') return s;
    } catch (err) { /* storage blocked */ }
    return /^de/i.test(navigator.language || '') ? 'de' : 'en';
  })();
  var L = {
    en: {
      drop: 'Drop a file here or click to choose', dropModel: 'Drop files here or click to choose (several at once: .obj + .mtl, .gltf + .bin)',
      bpFormats: 'Reads: vanilla structure .nbt (also Create schematics), Sponge .schem (v1–v3, WorldEdit/FAWE), Litematica .litematic, MCEdit/Schematica .schematic (old number ids, approximated). Axiom .bp: export it in Axiom as .schem first.',
      mdFormats: 'Reads: vanilla model .json, Blockbench .bbmodel (Java block/item), and as a cube approximation .obj (+ .mtl), .gltf (+ .bin), .glb.',
      sample: 'Load example', samplePick: 'Example', local: 'Files stay on your computer: they are read in this browser tab only.',
      fmt: 'Format', dv: 'Data version', size: 'Size', blocks: 'Blocks', kinds: 'Block kinds', layer: 'Layer', code: 'Blueprint code', copy: 'Copy', copied: 'Copied',
      part: 'Part', tile: 'Tile', limits: 'Limits and notes', limTier: 'Needed building wand (longest edge)', limCode: 'Code length: pasting and the editor take at most 32,000 characters; longer buildings are cut into parts from bottom to top. Place each part at its offset.',
      tiles: 'Larger than 256 blocks on an axis: cut into {0} tiles of at most 256 × 256 × 256. Each code says where it goes (offset from the corner of the whole building).',
      creative: 'Blocks without an item need the creative blueprint: {0}', lostBE: 'Block entities (chests, signs …): only the block state is kept, contents are dropped ({0}).',
      lostEnt: 'Entities (mobs, item frames …) are not part of a blueprint ({0}).', skip: 'Not placed (air, structure void …): {0}',
      unkDefs: 'Written with all properties (default value not in the table, still valid): {0}', renamed: 'Old block ids renamed: {0}',
      approx: 'Old number ids: {0} blocks only approximated (direction or variant unknown).', palettes: 'The file holds {0} random palettes; the first is used.',
      mapping: 'The file carries a Schematica id mapping (mods); numbers are read as vanilla 1.12 ids.', errPart: 'One layer alone is longer than the code limit (layer {0}); this building cannot be turned into codes.',
      unknown: 'Unknown blocks (not in Minecraft 26.3 or the Simple mods)', unkKeep: 'keep (marked red, the game will refuse them)', unkSkip: 'leave out', unkRep: 'replace with',
      region: 'Litematica region', allRegions: 'all regions together', back: 'Way back: code → file', backHint: 'Paste one blueprint code (one part at a time).',
      check: 'Check code', dlNbt: 'Download .nbt (structure)', dlSchem: 'Download .schem (Sponge v3)', ok: 'Code is fine.', problems: 'Problems', line: 'line',
      warnUnk: 'Unknown block ids (written into the file anyway): {0}', backEmpty: 'The code sets no block.', backLong: 'Longer than 32,000 characters: the editor in the game will not take it, but the file is written.',
      model: 'Simple Models code (vanilla model JSON)', elements: 'Cubes', textures: 'Textures', yaw: 'Turn', texNote: 'Texture images are not part of the code: the resource pack below contains the model and the PNGs that came with the file.',
      approxModel: 'Approximation: the mesh was turned into cubes. Shapes, curves and texture pictures are lost; each material becomes one colour.', res: 'Detail (cells)', sizeOpt: 'Size', fill: 'fill inside',
      sizeBlock: '1 block', size2: '2 blocks', size3: '3 blocks (max)', dlJson: 'Download .json', dlBb: 'Download .bbmodel', dlPack: 'Download resource pack (.zip)', dlTex: 'Download textures (.zip)',
      packName: 'Name', mBack: 'Way back: model code → file', mBackHint: 'Paste a Simple Models code (vanilla model JSON) to get a .json, a Blockbench .bbmodel or a resource pack.',
      devOnly: 'This page is not public yet (development wiki only).', warn: 'Notes', of: 'of',
      w_non_cube: 'Non-cube elements (meshes, locators) were skipped', w_box_uv: 'Box UV model: UV taken from the saved faces', w_shade_dropped: '"shade": false is not supported and was dropped',
      w_face_without_texture: 'A face points to a missing texture and was skipped', w_groups_flattened: 'Groups (outliner) are flattened; pivots of groups are not kept', w_animations_dropped: 'Animations are not part of a model and were dropped',
      w_reduced: 'Lowered the detail so the model stays within 256 cubes', w_textures_ignored: 'Texture pictures of the mesh are ignored; material colours are used', w_sparse_ignored: 'Sparse glTF data was ignored',
      w_non_triangles: 'Points and lines in the mesh were ignored',
      e_axiom_bp: 'Axiom blueprints (.bp) cannot be read here. In Axiom, export the blueprint as a Sponge schematic (.schem) and load that.',
      e_not_nbt: 'This is not an NBT file (.nbt, .schem, .litematic, .schematic). Is it maybe a different format?', e_unknown_format: 'The file is NBT, but none of the supported building formats.',
      e_too_large_file: 'The building is too large (more than 4096 blocks on an axis or 268 million cells).', e_no_gzip: 'Your browser cannot unpack gzip. Use a current browser.', e_bad_gzip: 'The file is damaged (gzip).',
      e_schem_no_blocks: 'This schematic holds no blocks (only biomes?).', e_litematic_no_regions: 'This Litematica file has no regions.', e_litematic_bad_states: 'The block data of a Litematica region is damaged.',
      e_litematic_no_region: 'This region does not exist.', e_schematic_not_alpha: 'Only Java Edition schematics ("Alpha") can be read.', e_mesh_empty: 'The model contains no triangles.',
      e_obj_bad_face: 'The .obj file has a face with a wrong vertex number.', e_gltf_compressed: 'Compressed glTF (Draco/meshopt) is not supported; export without compression.',
      e_gltf_no_buffer: 'The glTF file has no data. Choose the .glb, or the .gltf together with its .bin.', e_gltf_missing_bin: 'The glTF file needs "{0}": choose it together with the .gltf.',
      e_glb_bad: 'The .glb file is damaged.', e_too_many_elements: 'More than 256 cubes – the editor takes at most 256.', e_too_many_textures: 'More than 64 textures – the editor takes at most 64.',
      e_bad_extent: 'A cube lies outside −16..32 ({0}).', e_bad_element: 'Cube {0} has no from/to.', e_no_faces: 'Cube {0} has no faces.', e_too_long: 'The code is longer than 65,536 characters.',
      e_bad_json: 'This is not valid JSON: {0}', e_unknown_file: 'Unknown file type. Supported: {0}', e_generic: 'This file could not be read: {0}',
      pe_too_long: 'longer than {1} characters', pe_no_region: 'block without position', pe_bad_alias_name: 'bad alias name {0}', pe_alias_without_block: 'alias {0} without block', pe_alias_extra: 'extra words after the alias',
      pe_alias_of_alias: 'an alias cannot point to an alias', pe_unknown_alias: 'unknown alias {0}', pe_bad_block_id: 'bad block id "{0}"', pe_unclosed_properties: '"[" is not closed', pe_empty_property: 'empty property',
      pe_bad_property: 'bad property "{0}"', pe_bad_repeat: 'bad repeat {0}', pe_repeat_range: 'repeat count {0} outside 1..{1}', pe_bad_region: 'bad position "{0}"', pe_coordinate_range: 'coordinate {0} outside 0..{1}',
      pe_too_many_cells: 'more than {0} cells', pe_repeat_outside: 'copy {0} leaves the grid 0..{1}', pe_unknown_shape: 'unknown shape {0}', pe_bad_shape_args: 'bad arguments for {0}', pe_shape_size: 'size {0} below {1}',
      pe_shape_volume: 'shapes too large together (> {0})', pe_bad_expression: 'bad calculation "{0}"', pe_unknown_variable: 'unknown variable {0}', pe_alias_as_number: 'alias {0} used as a number',
      pe_variable_as_block: 'variable {0} used as a block', pe_number_range: 'number {0} beyond ±{1}', pe_division_by_zero: 'division by zero', pe_pixels_usage: 'pixels x,y,z <row>', pe_pixels_bad_row: 'bad pixel row "{0}"', pe_pixels_outside: 'pixel row leaves the grid 0..{0}'
    },
    de: {
      drop: 'Datei hier ablegen oder klicken', samplePick: 'Beispiel', dropModel: 'Dateien hier ablegen oder klicken (mehrere zugleich: .obj + .mtl, .gltf + .bin)',
      bpFormats: 'Liest: Vanilla-Strukturdatei .nbt (auch Create-Schematics), Sponge .schem (v1–v3, WorldEdit/FAWE), Litematica .litematic, MCEdit/Schematica .schematic (alte Zahlen-Ids, angenähert). Axiom .bp: in Axiom zuerst als .schem exportieren.',
      mdFormats: 'Liest: Vanilla-Modell .json, Blockbench .bbmodel (Java Block/Item) und als Würfel-Näherung .obj (+ .mtl), .gltf (+ .bin), .glb.',
      sample: 'Beispiel laden', local: 'Dateien bleiben auf deinem Rechner: sie werden nur in diesem Browser-Tab gelesen.',
      fmt: 'Format', dv: 'Datenversion', size: 'Größe', blocks: 'Blöcke', kinds: 'Blockarten', layer: 'Schicht', code: 'Blaupausen-Code', copy: 'Kopieren', copied: 'Kopiert',
      part: 'Teil', tile: 'Kachel', limits: 'Grenzen und Hinweise', limTier: 'Nötiger Baustab (längste Kante)', limCode: 'Codelänge: Einfügen und Editor nehmen höchstens 32 000 Zeichen; längere Bauwerke werden von unten nach oben in Teile geschnitten. Jeden Teil am angegebenen Versatz setzen.',
      tiles: 'Auf einer Achse größer als 256 Blöcke: in {0} Kacheln zu höchstens 256 × 256 × 256 geschnitten. Jeder Code nennt seinen Platz (Versatz von der Ecke des ganzen Bauwerks).',
      creative: 'Blöcke ohne Item brauchen die Kreativ-Blaupause: {0}', lostBE: 'Block-Entities (Truhen, Schilder …): nur der Blockzustand bleibt, Inhalte entfallen ({0}).',
      lostEnt: 'Entities (Tiere, Rahmen …) gehören nicht in eine Blaupause ({0}).', skip: 'Nicht gesetzt (Luft, Strukturleere …): {0}',
      unkDefs: 'Mit allen Eigenschaften geschrieben (Standardwert nicht in der Tabelle, trotzdem gültig): {0}', renamed: 'Alte Block-Ids umbenannt: {0}',
      approx: 'Alte Zahlen-Ids: {0} Blöcke nur angenähert (Richtung oder Variante unbekannt).', palettes: 'Die Datei hat {0} Zufallspaletten; die erste wird genommen.',
      mapping: 'Die Datei trägt eine Schematica-Id-Zuordnung (Mods); Zahlen werden als Vanilla-1.12-Ids gelesen.', errPart: 'Eine einzelne Schicht ist länger als die Codegrenze (Schicht {0}); dieses Bauwerk lässt sich nicht in Codes fassen.',
      unknown: 'Unbekannte Blöcke (nicht in Minecraft 26.3 oder den Simple-Mods)', unkKeep: 'behalten (rot markiert, das Spiel lehnt sie ab)', unkSkip: 'weglassen', unkRep: 'ersetzen durch',
      region: 'Litematica-Region', allRegions: 'alle Regionen zusammen', back: 'Rückweg: Code → Datei', backHint: 'Einen Blaupausen-Code einfügen (ein Teil auf einmal).',
      check: 'Code prüfen', dlNbt: '.nbt herunterladen (Struktur)', dlSchem: '.schem herunterladen (Sponge v3)', ok: 'Code ist in Ordnung.', problems: 'Fehler', line: 'Zeile',
      warnUnk: 'Unbekannte Block-Ids (trotzdem in die Datei geschrieben): {0}', backEmpty: 'Der Code setzt keinen Block.', backLong: 'Länger als 32 000 Zeichen: der Editor im Spiel nimmt ihn nicht, die Datei wird trotzdem geschrieben.',
      model: 'Simple-Models-Code (Vanilla-Modell-JSON)', elements: 'Quader', textures: 'Texturen', yaw: 'Drehen', texNote: 'Texturbilder gehören nicht in den Code: das Ressourcenpaket unten enthält das Modell und die PNGs, die in der Datei steckten.',
      approxModel: 'Näherung: das Netz wurde in Würfel umgewandelt. Formen, Rundungen und Texturbilder gehen verloren; jedes Material wird eine Farbe.', res: 'Feinheit (Zellen)', sizeOpt: 'Größe', fill: 'innen füllen',
      sizeBlock: '1 Block', size2: '2 Blöcke', size3: '3 Blöcke (max.)', dlJson: '.json herunterladen', dlBb: '.bbmodel herunterladen', dlPack: 'Ressourcenpaket herunterladen (.zip)', dlTex: 'Texturen herunterladen (.zip)',
      packName: 'Name', mBack: 'Rückweg: Modell-Code → Datei', mBackHint: 'Einen Simple-Models-Code (Vanilla-Modell-JSON) einfügen und als .json, Blockbench-.bbmodel oder Ressourcenpaket speichern.',
      devOnly: 'Diese Seite ist noch nicht öffentlich (nur Dev-Wiki).', warn: 'Hinweise', of: 'von',
      w_non_cube: 'Elemente, die keine Quader sind (Netze, Locator), wurden übersprungen', w_box_uv: 'Box-UV-Modell: UV aus den gespeicherten Flächen übernommen', w_shade_dropped: '"shade": false wird nicht unterstützt und entfiel',
      w_face_without_texture: 'Eine Fläche zeigt auf eine fehlende Textur und wurde übersprungen', w_groups_flattened: 'Gruppen (Outliner) werden aufgelöst; Drehpunkte von Gruppen entfallen', w_animations_dropped: 'Animationen gehören nicht in ein Modell und entfielen',
      w_reduced: 'Feinheit gesenkt, damit das Modell unter 256 Quadern bleibt', w_textures_ignored: 'Texturbilder des Netzes werden nicht übernommen; es gelten die Materialfarben', w_sparse_ignored: 'Dünnbesetzte glTF-Daten wurden ignoriert',
      w_non_triangles: 'Punkte und Linien im Netz wurden ignoriert',
      e_axiom_bp: 'Axiom-Blaupausen (.bp) lassen sich hier nicht lesen. In Axiom die Blaupause als Sponge-Schematic (.schem) exportieren und diese laden.',
      e_not_nbt: 'Das ist keine NBT-Datei (.nbt, .schem, .litematic, .schematic). Ist es vielleicht ein anderes Format?', e_unknown_format: 'Die Datei ist NBT, aber keines der unterstützten Bauwerk-Formate.',
      e_too_large_file: 'Das Bauwerk ist zu groß (mehr als 4096 Blöcke auf einer Achse oder 268 Millionen Zellen).', e_no_gzip: 'Dein Browser kann kein gzip entpacken. Bitte einen aktuellen Browser benutzen.', e_bad_gzip: 'Die Datei ist beschädigt (gzip).',
      e_schem_no_blocks: 'Diese Schematic enthält keine Blöcke (nur Biome?).', e_litematic_no_regions: 'Diese Litematica-Datei hat keine Regionen.', e_litematic_bad_states: 'Die Blockdaten einer Litematica-Region sind beschädigt.',
      e_litematic_no_region: 'Diese Region gibt es nicht.', e_schematic_not_alpha: 'Nur Java-Edition-Schematics („Alpha“) lassen sich lesen.', e_mesh_empty: 'Das Modell enthält keine Dreiecke.',
      e_obj_bad_face: 'Die .obj-Datei hat eine Fläche mit falscher Punktnummer.', e_gltf_compressed: 'Komprimiertes glTF (Draco/meshopt) wird nicht unterstützt; ohne Kompression exportieren.',
      e_gltf_no_buffer: 'Die glTF-Datei hat keine Daten. Die .glb wählen oder die .gltf zusammen mit ihrer .bin.', e_gltf_missing_bin: 'Die glTF-Datei braucht „{0}“: zusammen mit der .gltf auswählen.',
      e_glb_bad: 'Die .glb-Datei ist beschädigt.', e_too_many_elements: 'Mehr als 256 Quader – der Editor nimmt höchstens 256.', e_too_many_textures: 'Mehr als 64 Texturen – der Editor nimmt höchstens 64.',
      e_bad_extent: 'Ein Quader liegt außerhalb von −16..32 ({0}).', e_bad_element: 'Quader {0} hat kein from/to.', e_no_faces: 'Quader {0} hat keine Flächen.', e_too_long: 'Der Code ist länger als 65 536 Zeichen.',
      e_bad_json: 'Das ist kein gültiges JSON: {0}', e_unknown_file: 'Unbekannter Dateityp. Unterstützt: {0}', e_generic: 'Diese Datei konnte nicht gelesen werden: {0}',
      pe_too_long: 'länger als {1} Zeichen', pe_no_region: 'Block ohne Position', pe_bad_alias_name: 'ungültiger Alias-Name {0}', pe_alias_without_block: 'Alias {0} ohne Block', pe_alias_extra: 'zusätzliche Wörter nach dem Alias',
      pe_alias_of_alias: 'ein Alias darf nicht auf einen Alias zeigen', pe_unknown_alias: 'unbekannter Alias {0}', pe_bad_block_id: 'ungültige Block-Id „{0}“', pe_unclosed_properties: '„[“ ist nicht geschlossen', pe_empty_property: 'leere Eigenschaft',
      pe_bad_property: 'ungültige Eigenschaft „{0}“', pe_bad_repeat: 'ungültige Wiederholung {0}', pe_repeat_range: 'Wiederholung {0} außerhalb 1..{1}', pe_bad_region: 'ungültige Position „{0}“', pe_coordinate_range: 'Koordinate {0} außerhalb 0..{1}',
      pe_too_many_cells: 'mehr als {0} Stellen', pe_repeat_outside: 'Kopie {0} verlässt das Raster 0..{1}', pe_unknown_shape: 'unbekannte Form {0}', pe_bad_shape_args: 'ungültige Angaben für {0}', pe_shape_size: 'Größe {0} unter {1}',
      pe_shape_volume: 'Formen zusammen zu groß (> {0})', pe_bad_expression: 'ungültige Rechnung „{0}“', pe_unknown_variable: 'unbekannte Variable {0}', pe_alias_as_number: 'Alias {0} als Zahl benutzt',
      pe_variable_as_block: 'Variable {0} als Block benutzt', pe_number_range: 'Zahl {0} über ±{1}', pe_division_by_zero: 'Division durch null', pe_pixels_usage: 'pixels x,y,z <Zeile>', pe_pixels_bad_row: 'ungültige Bildzeile „{0}“', pe_pixels_outside: 'Bildzeile verlässt das Raster 0..{0}'
    }
  };
  function T(k) {
    var s = L[lang][k] != null ? L[lang][k] : L.en[k] != null ? L.en[k] : k, a = arguments;
    return String(s).replace(/\{(\d)\}/g, function (m, i) { return a[+i + 1] != null ? a[+i + 1] : ''; });
  }
  function esc(s) { return String(s).replace(/[&<>"]/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]; }); }
  function hue(name) { var h = 0; for (var i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) >>> 0; return h % 360; }
  function fmtN(n) { return Number(n).toLocaleString(lang === 'de' ? 'de-DE' : 'en-US'); }
  function short(id) { return String(id).replace(/^minecraft:/, ''); }
  function list(obj, max) {
    max = max || 8;
    var k = Object.keys(obj).sort(function (a, b) { return (obj[b] - obj[a]) || (a < b ? -1 : 1); });
    return k.slice(0, max).map(function (id) { return short(id) + (typeof obj[id] === 'number' ? ' ×' + fmtN(obj[id]) : ''); }).join(', ') + (k.length > max ? ', … (+' + (k.length - max) + ')' : '');
  }
  /* readable text for an error code like "gltf_missing_bin:house.bin" or a JSON SyntaxError */
  function errText(e) {
    var msg = e && e.message || String(e), key = msg.split(':')[0], arg = msg.slice(key.length + 1);
    if (e instanceof SyntaxError) return T('e_bad_json', msg);
    if (L.en['e_' + key]) return T('e_' + key, arg);
    return T('e_generic', msg);
  }
  /* Dev gate, same rule as isDevSite() in wiki/index.html: /dev/ (behind the login), the Zentrale dev overlay,
   * ?dev=1 (remembered for the tab session like the wiki), plus file:// and the CONV51 preview. Not public before owner OK. */
  function isDev() {
    var asked = /[?&]dev=1(&|$)/.test(location.search), kept = false;
    try { if (asked) sessionStorage.setItem('simple-wiki-dev', '1'); kept = sessionStorage.getItem('simple-wiki-dev') === '1'; } catch (err) { /* storage blocked */ }
    return /^\/dev(\/|$)/.test(location.pathname) || !!window.sbDevHook || asked || kept || location.protocol === 'file:' || !!window.CONV51_PREVIEW;
  }
  function samples(mode) { var s = window.CONV51_SAMPLES && window.CONV51_SAMPLES[mode]; return Array.isArray(s) ? s : []; }
  function b64(s) { var b = atob(s), u = new Uint8Array(b.length); for (var i = 0; i < b.length; i++) u[i] = b.charCodeAt(i); return u; }
  function download(name, data, type) {
    var url = URL.createObjectURL(new Blob([data], { type: type || 'application/octet-stream' })), a = document.createElement('a');
    a.href = url; a.download = name; document.body.appendChild(a); a.click(); a.remove();
    setTimeout(function () { URL.revokeObjectURL(url); }, 2000);
  }
  function copyBtn(ta, btn) {
    btn.onclick = function () {
      ta.select();
      (navigator.clipboard ? navigator.clipboard.writeText(ta.value) : Promise.reject()).catch(function () { try { document.execCommand('copy'); } catch (err) { /* nothing */ } });
      btn.textContent = T('copied'); setTimeout(function () { btn.textContent = T('copy'); }, 1200);
    };
  }
  function readAll(files) { return Promise.all(Array.prototype.map.call(files, function (f) { return f.arrayBuffer().then(function (a) { return { name: f.name, bytes: new Uint8Array(a) }; }); })); }
  function noteList(notes) { return notes.length ? '<ul class="cv-notes">' + notes.map(function (n) { return '<li' + (n.cls ? ' class="' + n.cls + '"' : '') + '>' + esc(n.t || n) + '</li>'; }).join('') + '</ul>' : ''; }

  function init(root) {
    var mode = root.getAttribute('data-conv');
    if (!(CONV51_PUBLIC || isDev())) { root.innerHTML = '<p class="cv-devonly">' + esc(T('devOnly')) + '</p>'; return; }
    var model = mode === 'model';
    root.innerHTML = '<div class="cv-drop" tabindex="0" role="button">' + esc(T(model ? 'dropModel' : 'drop')) + '<input type="file" hidden' + (model ? ' multiple accept=".json,.bbmodel,.obj,.mtl,.gltf,.glb,.bin"' : ' accept=".nbt,.schem,.schematic,.litematic,.bp"') + '></div>' +
      '<p class="cv-hint">' + esc(T(model ? 'mdFormats' : 'bpFormats')) + '</p><p class="cv-hint"><label>' + esc(T('samplePick')) + ' <select class="cv-sample-pick">' + samples(mode).map(function (s, i) { return '<option value="' + i + '">' + esc(s.label) + '</option>'; }).join('') + '</select></label> <button type="button" class="btn cv-sample">' + esc(T('sample')) + '</button></p><p class="cv-hint cv-local">' + esc(T('local')) + '</p>' +
      (model ? '' : '<p class="cv-opts"><label>' + esc(T('unknown')) + ': <select class="cv-unk"><option value="keep">' + esc(T('unkKeep')) + '</option><option value="skip">' + esc(T('unkSkip')) + '</option><option value="rep">' + esc(T('unkRep')) + '</option></select></label> <input class="cv-rep" value="stone" size="18" hidden> <label class="cv-regl" hidden>' + esc(T('region')) + ': <select class="cv-reg"></select></label></p>') +
      '<div class="cv-out" aria-live="polite"></div>' + backHtml(model);
    var drop = root.querySelector('.cv-drop'), input = root.querySelector('input[type=file]'), out = root.querySelector('.cv-out'), last = null, regionFile = null;
    drop.onclick = function () { input.click(); }; drop.onkeydown = function (e) { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); input.click(); } };
    drop.ondragover = function (e) { e.preventDefault(); drop.classList.add('cv-over'); }; drop.ondragleave = function () { drop.classList.remove('cv-over'); };
    drop.ondrop = function (e) { e.preventDefault(); drop.classList.remove('cv-over'); if (e.dataTransfer.files.length) readAll(e.dataTransfer.files).then(fresh, fail); };
    input.onchange = function () { if (input.files.length) readAll(input.files).then(fresh, fail); input.value = ''; };
    /* examples: wiki/assets/conv/examples/examples.js (generated by tools/conv51/make_examples.js, own files, embedded) */
    root.querySelector('.cv-sample').onclick = function () {
      var s = samples(mode)[+root.querySelector('.cv-sample-pick').value || 0];
      if (!s) { fail(new Error('generic:no example')); return; }
      fresh(s.files.map(function (f) { return { name: f.name, bytes: b64(f.b64) }; }));
    };
    function fail(e) { out.innerHTML = '<p class="cv-err" role="alert">' + esc(errText(e)) + '</p>'; }
    function fresh(files) { regionFile = null; take(files); }
    function take(files) { last = files; try { (model ? handleModel : handleBlueprint)(files); } catch (e) { fail(e); } }
    if (model) initModel(); else initBlueprint();

    /* ------------------------------------------------ Bauwerke ------------------------------------------------ */
    function initBlueprint() {
      var unk = root.querySelector('.cv-unk'), rep = root.querySelector('.cv-rep'), reg = root.querySelector('.cv-reg');
      unk.onchange = function () { rep.hidden = unk.value !== 'rep'; if (last) take(last); };
      rep.onchange = function () { if (last) take(last); };
      reg.onchange = function () { if (last) take(last); };
      var ta = root.querySelector('.cv-back-code'), res = root.querySelector('.cv-back-out');
      root.querySelector('.cv-back-check').onclick = function () { backCheck(false); };
      root.querySelector('.cv-back-nbt').onclick = function () { backCheck('nbt'); };
      root.querySelector('.cv-back-schem').onclick = function () { backCheck('schem'); };
      function backCheck(kind) {
        var r = C.parseCode(ta.value), h = '';
        if (r.problems.length) h += '<p class="cv-err">' + esc(T('problems')) + ':</p><ul>' + r.problems.slice(0, 30).map(function (p) { return '<li>' + esc(T('line') + ' ' + (p.line + 1) + ': ' + T.apply(null, ['pe_' + p.key].concat(p.args))) + '</li>'; }).join('') + '</ul>';
        else h += '<p class="cv-ok">' + esc(T('ok')) + ' ' + esc(T('blocks')) + ': ' + fmtN(r.model.n) + '</p>';
        if (Object.keys(r.warnings).length) h += '<p>' + esc(T('warnUnk', list(r.warnings))) + '</p>';
        if (ta.value.length > C.MAX_CODE) h += '<p>' + esc(T('backLong')) + '</p>';
        if (!r.model.n) h += '<p>' + esc(T('backEmpty')) + '</p>';
        res.innerHTML = h;
        if (kind && r.model.n) C.writeFile(r.model, kind).then(function (bytes) { download('blueprint.' + kind, bytes); }, function (e) { res.innerHTML += '<p class="cv-err">' + esc(errText(e)) + '</p>'; });
      }
    }
    function handleBlueprint(files) {
      var f = files[0], unk = root.querySelector('.cv-unk').value, opts = { unknown: unk === 'rep' ? root.querySelector('.cv-rep').value.trim() || 'stone' : unk };
      var reg = root.querySelector('.cv-reg'), regl = root.querySelector('.cv-regl');
      if (regionFile === f.name && reg.value) opts.region = reg.value;
      C.readFile(f.name, f.bytes, opts).then(function (m) {
        var regs = m.info.regions;
        if (regs && regs.length > 1) {
          if (regionFile !== f.name) {
            reg.innerHTML = '<option value="">' + esc(T('allRegions')) + '</option>' + regs.map(function (r) { return '<option value="' + esc(r.name) + '">' + esc(r.name + ' (' + r.size.join(' × ') + ')') + '</option>'; }).join('');
            regionFile = f.name;
          }
          regl.hidden = false;
        } else regl.hidden = true;
        showBlueprint(m);
      }).catch(fail);
    }
    function showBlueprint(m) {
      var info = m.info, kinds = {}; m.cells.forEach(function (s) { kinds[s] = (kinds[s] || 0) + 1; });
      var res = m.n ? C.toTiles(m) : { parts: [], error: null, tiles: 0 }, edge = Math.max(m.sx, m.sy, m.sz);
      var tier = [16, 32, 48, 64, 128, 256].filter(function (e) { return edge <= e; })[0];
      var h = '<h3>' + esc(T('fmt')) + ': ' + esc(info.format + (info.version ? ' v' + info.version : '')) + (info.dataVersion != null ? ' · ' + esc(T('dv')) + ' ' + info.dataVersion : '') + '</h3>' +
        '<p><b>' + esc(T('size')) + ':</b> ' + m.sx + ' × ' + m.sy + ' × ' + m.sz + ' · <b>' + esc(T('blocks')) + ':</b> ' + fmtN(m.n) + ' · <b>' + esc(T('kinds')) + ':</b> ' + Object.keys(kinds).length + '</p>';
      h += '<div class="cv-prev"><canvas width="360" height="360"></canvas><label>' + esc(T('layer')) + ' <input type="range" class="cv-ly-r" min="0" max="' + Math.max(0, m.sy - 1) + '" value="0"> <span class="cv-ly">0</span></label></div>';
      var notes = [];
      if (Object.keys(info.unknown).length) notes.push({ t: T('unknown') + ': ' + list(info.unknown), cls: 'cv-bad' });
      if (res.error) notes.push({ t: T('errPart', res.layer), cls: 'cv-bad' });
      if (res.tiles > 1) notes.push(T('tiles', res.tiles));
      notes.push(T('limTier') + ': ' + edge + ' → ' + (tier ? tier : '256 (' + T('tile') + ')'));
      if (Object.keys(info.noItem).length) notes.push(T('creative', list(info.noItem)));
      if (info.blockEntities) notes.push(T('lostBE', fmtN(info.blockEntities)));
      if (info.entities) notes.push(T('lostEnt', fmtN(info.entities)));
      if (info.skipped) notes.push(T('skip', fmtN(info.skipped)));
      if (Object.keys(info.renamed).length) notes.push(T('renamed', Object.keys(info.renamed).map(function (k) { return k + ' → ' + info.renamed[k]; }).join(', ')));
      if (info.approx) notes.push(T('approx', fmtN(info.approx)));
      info.notes.forEach(function (n) { if (/^palettes:/.test(n)) notes.push(T('palettes', n.split(':')[1])); if (n === 'schematica_mapping') notes.push(T('mapping')); });
      var ud = Object.keys(info.unknownDefaults); if (ud.length) notes.push(T('unkDefs', ud.slice(0, 8).join(', ') + (ud.length > 8 ? ' …' : '')));
      notes.push(T('limCode'));
      h += '<h4>' + esc(T('limits')) + '</h4>' + noteList(notes);
      res.parts.forEach(function (p, i) {
        var label = T('code') + (res.parts.length > 1 ? ' – ' + T('part') + ' ' + (i + 1) + ' ' + T('of') + ' ' + res.parts.length + (res.tiles > 1 ? ' (' + T('tile') + ' ' + p.tile + ')' : '') + ', +' + p.offset.join(',') : '');
        h += '<h4>' + esc(label) + ' <span class="cv-len">(' + fmtN(p.code.length) + ' / ' + fmtN(C.MAX_CODE) + ')</span></h4><textarea readonly rows="8" class="cv-code" spellcheck="false">' + esc(p.code) + '</textarea><button type="button" class="btn cv-copy">' + esc(T('copy')) + '</button>';
      });
      out.innerHTML = h;
      out.querySelectorAll('.cv-code').forEach(function (ta) { copyBtn(ta, ta.nextElementSibling); });
      var cv = out.querySelector('canvas'), ctx = cv.getContext('2d'), rng = out.querySelector('.cv-ly-r'), ly = out.querySelector('.cv-ly'), unknownSet = info.unknown;
      function draw() {
        var y = +rng.value; ly.textContent = y; ctx.clearRect(0, 0, 360, 360);
        var s = Math.max(1, Math.floor(360 / Math.max(m.sx, m.sz, 1))), gap = s > 3 ? 1 : 0;
        m.cells.forEach(function (sp, k) {
          if (C.ky(k) !== y) return;
          var x = C.kx(k), z = C.kz(k); if (x * s >= 360 || z * s >= 360) return;
          var id = C.fullId(sp.split('[')[0]), bad = unknownSet[id] != null;
          ctx.fillStyle = bad ? '#d00' : 'hsl(' + hue(sp.split('[')[0]) + ',55%,52%)'; ctx.fillRect(x * s, z * s, s - gap, s - gap);
          if (bad && s > 5) { ctx.strokeStyle = '#fff'; ctx.beginPath(); ctx.moveTo(x * s + 1, z * s + 1); ctx.lineTo(x * s + s - 2, z * s + s - 2); ctx.stroke(); }
        });
      }
      rng.oninput = draw; draw();
    }

    /* ------------------------------------------------ 3D-Modelle ------------------------------------------------ */
    function initModel() {
      var ta = root.querySelector('.cv-back-code'), res = root.querySelector('.cv-back-out'), nm = root.querySelector('.cv-back-name');
      function current() {
        var src = JSON.parse(ta.value), code = C.modelCode(src);
        res.innerHTML = '<p class="cv-ok">' + esc(T('ok')) + ' ' + esc(T('elements')) + ': ' + (src.elements || []).length + '</p>';
        return { src: JSON.parse(code), code: code };
      }
      function guard(fn) { return function () { try { fn(); } catch (e) { res.innerHTML = '<p class="cv-err">' + esc(errText(e)) + '</p>'; } }; }
      root.querySelector('.cv-back-json').onclick = guard(function () { var c = current(); download(C.safeName(nm.value) + '.json', c.code, 'application/json'); });
      root.querySelector('.cv-back-bb').onclick = guard(function () { var c = current(); download(C.safeName(nm.value) + '.bbmodel', JSON.stringify(C.modelToBbmodel(c.src, nm.value)), 'application/json'); });
      root.querySelector('.cv-back-pack').onclick = guard(function () { var c = current(); download(C.safeName(nm.value) + '-pack.zip', C.resourcePack(nm.value, c.code, []), 'application/zip'); });
    }
    function handleModel(files) {
      var byName = {}; files.forEach(function (f) { byName[f.name.toLowerCase()] = f.bytes; });
      var main = files.filter(function (f) { return /\.(json|bbmodel|obj|gltf|glb)$/i.test(f.name); })[0];
      if (!main) throw new Error('unknown_file:.json .bbmodel .obj .gltf .glb');
      var text = function (b) { return new TextDecoder().decode(b); }, ext = main.name.toLowerCase().split('.').pop(), warns = [], w = function (k) { warns.push(k); };
      if (ext === 'obj' || ext === 'gltf' || ext === 'glb') {
        var mesh;
        if (ext === 'obj') { var mtl = files.filter(function (f) { return /\.mtl$/i.test(f.name); })[0]; mesh = C.parseObj(text(main.bytes), mtl ? text(mtl.bytes) : null); }
        else if (ext === 'gltf') mesh = C.parseGltf(text(main.bytes), null, byName);
        else mesh = C.parseGlb(main.bytes, byName);
        showMesh(main.name, mesh, null);
        return;
      }
      var src, tex = [];
      if (ext === 'bbmodel' || /"meta"\s*:/.test(text(main.bytes).slice(0, 400))) { var bb = JSON.parse(text(main.bytes)); src = C.bbmodelToVanilla(bb, w); tex = C.bbTextures(bb).map(function (t) { return { name: t.name, data: t.data }; }); }
      else src = JSON.parse(text(main.bytes));
      showModel(main.name, src, warns, tex, null);
    }
    function showMesh(name, mesh, o) {
      o = o || { res: 16, size: 16, fill: true };
      var r = C.meshToModel(mesh, { res: o.res, size: o.size, fill: o.fill, name: name });
      showModel(name, r.model, r.warn, r.textures, { mesh: mesh, res: o.res, size: o.size, fill: o.fill });
    }
    function showModel(name, src, warns, textures, meshOpts) {
      var code = C.modelCode(src, function (k) { warns.push(k); }), j = JSON.parse(code), els = j.elements || [], base = C.safeName(name), h = '';
      if (meshOpts) {
        h += '<p class="cv-mopts">' + esc(T('approxModel')) + '<br><label>' + esc(T('res')) + ' <select class="cv-res">' + [8, 12, 16, 24, 32, 48].map(function (v) { return '<option' + (v === meshOpts.res ? ' selected' : '') + '>' + v + '</option>'; }).join('') + '</select></label> ' +
          '<label>' + esc(T('sizeOpt')) + ' <select class="cv-size">' + [[16, 'sizeBlock'], [32, 'size2'], [48, 'size3']].map(function (s) { return '<option value="' + s[0] + '"' + (meshOpts.size === s[0] ? ' selected' : '') + '>' + esc(T(s[1])) + '</option>'; }).join('') + '</select></label> ' +
          '<label><input type="checkbox" class="cv-fill"' + (meshOpts.fill ? ' checked' : '') + '> ' + esc(T('fill')) + '</label></p>';
      }
      h += '<h3>' + esc(T('elements')) + ': ' + els.length + ' / 256 · ' + esc(T('textures')) + ': ' + Object.keys(j.textures || {}).length + '</h3>';
      h += '<div class="cv-prev"><canvas width="360" height="300"></canvas><label>' + esc(T('yaw')) + ' <input type="range" class="cv-yaw" min="0" max="359" value="35"></label></div>';
      h += '<p>' + esc(T('texNote')) + '</p>';
      if (warns.length) h += '<h4>' + esc(T('warn')) + '</h4>' + noteList(warns.filter(function (k, i, a) { return a.indexOf(k) === i; }).map(function (k) { return T('w_' + k.split(':')[0]); }));
      h += '<h4>' + esc(T('model')) + ' <span class="cv-len">(' + fmtN(code.length) + ' / ' + fmtN(65536) + ')</span></h4><textarea readonly rows="12" class="cv-code" spellcheck="false">' + esc(code) + '</textarea><button type="button" class="btn cv-copy">' + esc(T('copy')) + '</button>';
      h += '<p class="cv-dl"><label>' + esc(T('packName')) + ' <input class="cv-name" value="' + esc(base) + '" size="20" maxlength="20"></label> <button type="button" class="btn cv-dl-json">' + esc(T('dlJson')) + '</button> <button type="button" class="btn cv-dl-bb">' + esc(T('dlBb')) + '</button> <button type="button" class="btn cv-dl-pack">' + esc(T('dlPack')) + '</button>' +
        (textures.length ? ' <button type="button" class="btn cv-dl-tex">' + esc(T('dlTex')) + ' (' + textures.length + ')</button>' : '') + '</p>';
      out.innerHTML = h;
      copyBtn(out.querySelector('.cv-code'), out.querySelector('.cv-copy'));
      if (meshOpts) out.querySelectorAll('.cv-mopts select, .cv-mopts input').forEach(function (el) {
        el.onchange = function () {
          try { showMesh(name, meshOpts.mesh, { res: +out.querySelector('.cv-res').value, size: +out.querySelector('.cv-size').value, fill: out.querySelector('.cv-fill').checked }); } catch (e) { fail(e); }
        };
      });
      var nm = function () { return C.safeName(out.querySelector('.cv-name').value); };
      out.querySelector('.cv-dl-json').onclick = function () { download(nm() + '.json', code, 'application/json'); };
      out.querySelector('.cv-dl-bb').onclick = function () { download(nm() + '.bbmodel', JSON.stringify(C.modelToBbmodel(j, nm())), 'application/json'); };
      out.querySelector('.cv-dl-pack').onclick = function () { download(nm() + '-pack.zip', C.resourcePack(nm(), code, textures), 'application/zip'); };
      var tb = out.querySelector('.cv-dl-tex');
      if (tb) tb.onclick = function () { download(nm() + '-textures.zip', C.zip(textures.map(function (t) { return { name: t.name + '.png', data: t.data }; })), 'application/zip'); };
      var colors = {}; textures.forEach(function (t) { if (t.color) colors[t.name] = t.color; });
      var cv = out.querySelector('canvas'), ctx = cv.getContext('2d'), rng = out.querySelector('.cv-yaw');
      function faceColor(e, i) {
        var f = e.faces && (e.faces.up || e.faces.north || e.faces[Object.keys(e.faces)[0]]), v = f && String(f.texture || '').replace('#', ''), id = v && j.textures && j.textures[v];
        var nmT = id && String(id).split('/').pop(), c = nmT && colors[nmT];
        return c ? 'rgb(' + c[0] + ',' + c[1] + ',' + c[2] + ')' : 'hsl(' + hue(e.name || v || String(i)) + ',45%,60%)';
      }
      function draw() {
        var a = rng.value * Math.PI / 180, ca = Math.cos(a), sa = Math.sin(a), quads = [], mx = 16;
        ctx.clearRect(0, 0, 360, 300);
        els.forEach(function (e) { e.from.concat(e.to).forEach(function (v) { mx = Math.max(mx, Math.abs(v - 8) * 2); }); });
        var sc = 9 * 16 / mx;
        els.forEach(function (e, i) {
          var f = e.from, t = e.to, pts = [], col = faceColor(e, i);
          for (var k = 0; k < 8; k++) { var x = (k & 1 ? t[0] : f[0]) - 8, y = (k & 2 ? t[1] : f[1]) - 8, z = (k & 4 ? t[2] : f[2]) - 8; pts.push([x * ca - z * sa, y, x * sa + z * ca]); }
          [[0, 1, 3, 2], [4, 5, 7, 6], [0, 1, 5, 4], [2, 3, 7, 6], [0, 2, 6, 4], [1, 3, 7, 5]].forEach(function (q) {
            var d = q.reduce(function (s, p) { return s + pts[p][2]; }, 0);
            quads.push({ d: d, p: q.map(function (p) { return [180 + pts[p][0] * sc + pts[p][2] * sc * 0.38, 160 - pts[p][1] * sc + pts[p][2] * sc * 0.38]; }), c: col });
          });
        });
        quads.sort(function (p, q) { return q.d - p.d; });
        quads.forEach(function (q) { ctx.beginPath(); q.p.forEach(function (p, i) { if (i) ctx.lineTo(p[0], p[1]); else ctx.moveTo(p[0], p[1]); }); ctx.closePath(); ctx.fillStyle = q.c; ctx.fill(); ctx.strokeStyle = '#333'; ctx.stroke(); });
      }
      rng.oninput = draw; draw();
    }
  }
  function backHtml(model) {
    if (model) return '<details class="cv-back"><summary>' + esc(T('mBack')) + '</summary><p class="cv-hint">' + esc(T('mBackHint')) + '</p><textarea class="cv-back-code cv-code" rows="8" spellcheck="false"></textarea>' +
      '<p><label>' + esc(T('packName')) + ' <input class="cv-back-name" value="model" size="20" maxlength="20"></label> <button type="button" class="btn cv-back-json">' + esc(T('dlJson')) + '</button> <button type="button" class="btn cv-back-bb">' + esc(T('dlBb')) + '</button> <button type="button" class="btn cv-back-pack">' + esc(T('dlPack')) + '</button></p><div class="cv-back-out"></div></details>';
    return '<details class="cv-back"><summary>' + esc(T('back')) + '</summary><p class="cv-hint">' + esc(T('backHint')) + '</p><textarea class="cv-back-code cv-code" rows="8" spellcheck="false"></textarea>' +
      '<p><button type="button" class="btn cv-back-check">' + esc(T('check')) + '</button> <button type="button" class="btn cv-back-nbt">' + esc(T('dlNbt')) + '</button> <button type="button" class="btn cv-back-schem">' + esc(T('dlSchem')) + '</button></p><div class="cv-back-out"></div></details>';
  }
  function boot() { document.querySelectorAll('[data-conv]').forEach(init); }
  /* Wiki SPA (wiki/index.html, page "Bauwerke"/"3D-Modelle"): sets CONV51_EMBED and mounts with the wiki language. */
  function mount(root, opts) { if (opts && L[opts.lang]) { lang = opts.lang; window.Conv51UI.lang = lang; } init(root); }
  window.Conv51UI = { isDev: isDev, T: T, errText: errText, lang: lang, PUBLIC: CONV51_PUBLIC, strings: L, mount: mount };
  if (!window.CONV51_EMBED) { if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot); else boot(); }
})();
