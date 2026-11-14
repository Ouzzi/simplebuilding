/* conv-core.js (N51 prototype, b-conv51): pure JS, no libraries, no network. Runs in the browser and in node.
 * Blueprint code = text "Bausprache" of Simple Building Tools; port of BlueprintCode.serialize / boxes / spec.
 * Model code = vanilla 26.3 model JSON in the compact layout of Simple Models (ModelJson.writeCompact). */
(function (root) {
  'use strict';
  var GRID = 256, WRAP = 100, MAX_CODE = 32000;

  /* ---------------- NBT (big endian), gzip detection ---------------- */
  function readNbt(buf) {
    var dv = new DataView(buf.buffer, buf.byteOffset, buf.byteLength), p = 0, dec = new TextDecoder('utf-8');
    function str() { var n = dv.getUint16(p); p += 2; var s = dec.decode(buf.subarray(p, p + n)); p += n; return s; }
    function val(t) {
      var v, n, i;
      switch (t) {
        case 1: v = dv.getInt8(p); p += 1; return v;
        case 2: v = dv.getInt16(p); p += 2; return v;
        case 3: v = dv.getInt32(p); p += 4; return v;
        case 4: v = dv.getBigInt64(p); p += 8; return v;
        case 5: v = dv.getFloat32(p); p += 4; return v;
        case 6: v = dv.getFloat64(p); p += 8; return v;
        case 7: n = dv.getInt32(p); p += 4; v = Array.from(new Int8Array(buf.buffer, buf.byteOffset + p, n)); p += n; return v;
        case 8: return str();
        case 9: var et = dv.getUint8(p++); n = dv.getInt32(p); p += 4; v = []; for (i = 0; i < n; i++) v.push(val(et)); return v;
        case 10: v = {}; for (;;) { var tt = dv.getUint8(p++); if (tt === 0) break; var k = str(); v[k] = val(tt); } return v;
        case 11: n = dv.getInt32(p); p += 4; v = []; for (i = 0; i < n; i++) { v.push(dv.getInt32(p)); p += 4; } return v;
        case 12: n = dv.getInt32(p); p += 4; v = []; for (i = 0; i < n; i++) { v.push(dv.getBigInt64(p)); p += 8; } return v;
        default: throw new Error('nbt_bad_tag:' + t);
      }
    }
    if (dv.getUint8(0) !== 10) throw new Error('nbt_not_compound');
    p = 1; str();
    return val(10);
  }
  /* writer: used by tests and for the later way back (code -> .nbt) */
  function writeNbt(obj, name) {
    var out = [], enc = new TextEncoder();
    function b(x) { out.push(x & 255); }
    function i16(x) { b(x >> 8); b(x); }
    function i32(x) { b(x >> 24); b(x >> 16); b(x >> 8); b(x); }
    function str(s) { var e = enc.encode(s); i16(e.length); for (var i = 0; i < e.length; i++) b(e[i]); }
    function tagOf(v) {
      if (v && v.__t) return v.__t;
      if (typeof v === 'string') return 8;
      if (typeof v === 'number') return 3;
      if (Array.isArray(v)) return 9;
      return 10;
    }
    function val(v, t) {
      var d = v && v.__t ? v.v : v, i;
      if (t === 1) b(d); else if (t === 3) i32(d); else if (t === 8) str(d);
      else if (t === 9) { var et = d.length ? tagOf(d[0]) : 0; b(et); i32(d.length); for (i = 0; i < d.length; i++) val(d[i], et); }
      else if (t === 10) { Object.keys(d).forEach(function (k) { var tt = tagOf(d[k]); b(tt); str(k); val(d[k], tt); }); b(0); }
      else throw new Error('nbt_write_tag:' + t);
    }
    b(10); str(name || ''); val(obj, 10);
    return Uint8Array.from(out);
  }
  function isGzip(buf) { return buf.length > 2 && buf[0] === 0x1f && buf[1] === 0x8b; }
  /* gunzip: injected (node zlib) or the browser's DecompressionStream; plain NBT passes through */
  async function ungzip(buf, gunzip) {
    if (!isGzip(buf)) return buf;
    if (gunzip) return gunzip(buf);
    if (typeof DecompressionStream === 'undefined') throw new Error('no_gzip');
    var ds = new Response(new Blob([buf]).stream().pipeThrough(new DecompressionStream('gzip')));
    return new Uint8Array(await ds.arrayBuffer());
  }

  /* ---------------- block defaults (omitted in the code, like BlueprintCode.spec) ---------------- */
  var F4 = { north: 'false', east: 'false', south: 'false', west: 'false' };
  function d(o) { return Object.assign({ waterlogged: 'false' }, o); }
  var DEFAULT_RULES = [
    [/_stairs$/, d({ facing: 'north', half: 'bottom', shape: 'straight' })],
    [/_slab$/, d({ type: 'bottom' })],
    [/_fence_gate$/, { facing: 'north', in_wall: 'false', open: 'false', powered: 'false' }],
    [/_fence$/, d(F4)],
    [/_wall$/, d({ east: 'none', north: 'none', south: 'none', west: 'none', up: 'true' })],
    [/_trapdoor$/, d({ facing: 'north', half: 'bottom', open: 'false', powered: 'false' })],
    [/_door$/, { facing: 'north', half: 'lower', hinge: 'left', open: 'false', powered: 'false' }],
    [/(_log|_wood|_stem|_hyphae|_pillar|hay_block|bone_block)$|^(basalt|deepslate|quartz_pillar|purpur_pillar)$/, { axis: 'y' }],
    [/_leaves$/, d({ distance: '7', persistent: 'false' })],
    [/^(glass_pane|iron_bars)$|_glass_pane$/, d(F4)],
    [/^(lantern|soul_lantern)$/, d({ hanging: 'false' })],
    [/^(ladder|chest|trapped_chest|ender_chest)$/, d({ facing: 'north', type: 'single' })],
    [/^(furnace|blast_furnace|smoker)$/, { facing: 'north', lit: 'false' }],
    [/^(wall_torch|soul_wall_torch|redstone_wall_torch)$/, { facing: 'north' }],
    [/_wall_sign$/, d({ facing: 'north' })],
    [/_sign$/, d({ rotation: '0' })],
    [/_button$/, { face: 'wall', facing: 'north', powered: 'false' }],
    [/_bed$/, { facing: 'north', occupied: 'false', part: 'foot' }]
  ];
  function defaultsFor(name) {
    for (var i = 0; i < DEFAULT_RULES.length; i++) if (DEFAULT_RULES[i][0].test(name)) return DEFAULT_RULES[i][1];
    return null;
  }
  /* Kurzform eines Zustands; props: {name:value} (strings). Unknown default -> property kept (always parses back equal). */
  function spec(id, props, info) {
    var ns = id.indexOf(':') < 0 ? 'minecraft' : id.slice(0, id.indexOf(':')), path = id.slice(id.indexOf(':') + 1);
    var name = ns === 'minecraft' ? path : ns + ':' + path;
    var defs = ns === 'minecraft' ? defaultsFor(path) : null, parts = [];
    Object.keys(props || {}).forEach(function (k) {
      if (defs && defs[k] === props[k]) return;
      if (!defs || !(k in defs)) { if (info) info.unknownDefaults[name] = true; }
      parts.push(k + '=' + props[k]);
    });
    return parts.length ? name + '[' + parts.join(',') + ']' : name;
  }

  /* ---------------- structure .nbt -> model ---------------- */
  var SKIP = /^minecraft:(air|cave_air|void_air|structure_void|moving_piston)$/;
  /* model: {cells: Map(key->specString), n} ; key = x | y<<8 | z<<16 like BlueprintModel.key */
  function structureToModel(nbt, opts) {
    opts = opts || {};
    var info = { skipped: 0, unknownDefaults: {}, blockEntities: 0, entities: (nbt.entities || []).length,
      dataVersion: nbt.DataVersion == null ? null : nbt.DataVersion, size: nbt.size || null, tooBig: false };
    var palette = nbt.palette || (nbt.palettes && nbt.palettes[0]);
    if (!palette || !nbt.blocks) throw new Error('not_structure');
    var specs = palette.map(function (e) {
      var props = {}; Object.keys(e.Properties || {}).sort().forEach(function (k) { props[k] = String(e.Properties[k]); });
      return SKIP.test(e.Name) ? null : spec(e.Name, props, info);
    });
    var cells = new Map(), minX = 1e9, minY = 1e9, minZ = 1e9, maxX = -1e9, maxY = -1e9, maxZ = -1e9;
    nbt.blocks.forEach(function (b) {
      var s = specs[b.state]; if (s === null || s === undefined) { info.skipped++; return; }
      var x = b.pos[0], y = b.pos[1], z = b.pos[2];
      if (b.nbt) info.blockEntities++;
      cells.set(x + ',' + y + ',' + z, s);
      if (x < minX) minX = x; if (y < minY) minY = y; if (z < minZ) minZ = z;
      if (x > maxX) maxX = x; if (y > maxY) maxY = y; if (z > maxZ) maxZ = z;
    });
    return normalize({ cells: cells, minX: minX, minY: minY, minZ: minZ, maxX: maxX, maxY: maxY, maxZ: maxZ }, info);
  }
  function normalize(m, info) {
    var out = new Map();
    m.cells.forEach(function (s, k) {
      var c = k.split(',').map(Number);
      out.set(key(c[0] - m.minX, c[1] - m.minY, c[2] - m.minZ), s);
    });
    var sx = m.cells.size ? m.maxX - m.minX + 1 : 0, sy = m.cells.size ? m.maxY - m.minY + 1 : 0, sz = m.cells.size ? m.maxZ - m.minZ + 1 : 0;
    info.tooBig = Math.max(sx, sy, sz) > GRID;
    return { cells: out, n: out.size, sx: sx, sy: sy, sz: sz, info: info };
  }
  function key(x, y, z) { return x + y * 1024 + z * 1048576; } // collision-free for x,y,z < 1024 (Java: 8 bit each, grid 256)
  function kx(k) { return k % 1024; } function ky(k) { return Math.floor(k / 1024) % 1024; } function kz(k) { return Math.floor(k / 1048576); }

  /* ---------------- serialize: port of BlueprintCode.serialize / boxes / wrap ---------------- */
  function sortedKeys(cells) { // y, then z, then x ascending (BlueprintModel.sortedKeys)
    return Array.from(cells.keys()).sort(function (a, b) { return (ky(a) - ky(b)) || (kz(a) - kz(b)) || (kx(a) - kx(b)); });
  }
  function boxes(sorted) {
    var remaining = new Set(sorted), out = [];
    function rowFree(x1, x2, y, z) { for (var x = x1; x <= x2; x++) if (!remaining.has(key(x, y, z))) return false; return true; }
    function layerFree(x1, x2, y, z1, z2) { for (var z = z1; z <= z2; z++) if (!rowFree(x1, x2, y, z)) return false; return true; }
    sorted.forEach(function (k) {
      if (!remaining.has(k)) return;
      var x = kx(k), y = ky(k), z = kz(k), x2 = x, z2 = z, y2 = y;
      while (x2 + 1 < GRID && remaining.has(key(x2 + 1, y, z))) x2++;
      while (z2 + 1 < GRID && rowFree(x, x2, y, z2 + 1)) z2++;
      while (y2 + 1 < GRID && layerFree(x, x2, y2 + 1, z, z2)) y2++;
      for (var yy = y; yy <= y2; yy++) for (var zz = z; zz <= z2; zz++) for (var xx = x; xx <= x2; xx++) remaining.delete(key(xx, yy, zz));
      out.push([x, y, z, x2, y2, z2]);
    });
    return out;
  }
  function axis(lo, hi) { return lo === hi ? String(lo) : lo + '..' + hi; }
  function region(b) { return axis(b[0], b[3]) + ',' + axis(b[1], b[4]) + ',' + axis(b[2], b[5]); }
  function wrap(sp, regions) {
    var lines = [], line = sp, on = 0;
    regions.forEach(function (r) {
      if (on > 0 && line.length + 1 + r.length > WRAP) { lines.push(line); line = sp; on = 0; }
      line += ' ' + r; on++;
    });
    lines.push(line); return lines;
  }
  function aliasName(index) {
    var s = '', i = index;
    do { s = String.fromCharCode(97 + i % 26) + s; i = Math.floor(i / 26) - 1; } while (i >= 0);
    return s;
  }
  function serialize(model) {
    if (!model.n) return '';
    var by = new Map();
    sortedKeys(model.cells).forEach(function (k) { var s = model.cells.get(k); if (!by.has(s)) by.set(s, []); by.get(s).push(k); });
    var groups = Array.from(by.entries());
    groups.sort(function (a, b) { return (b[1].length - a[1].length) || (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0); }); // String.compareTo = UTF-16 order
    var header = '# ' + model.sx + 'x' + model.sy + 'x' + model.sz + ', ' + model.n + ' blocks\n', aliases = '', body = '', ai = 0;
    groups.forEach(function (g) {
      var regions = boxes(g[1]).map(region), lines = wrap(g[0], regions);
      if (lines.length > 1 && g[0].length > 12) { var al = '$' + aliasName(ai++); aliases += al + ' = ' + g[0] + '\n'; lines = wrap(al, regions); }
      lines.forEach(function (l) { body += l + '\n'; });
    });
    return (header + aliases + body).replace(/\s+$/, '');
  }

  /* limit helper: split in y bands so each part stays <= MAX_CODE chars (editor/import limit); a part is normalised on its own */
  function subModel(model, y0, y1) {
    var c = new Map(), mn = [1e9, 1e9, 1e9], mx = [-1, -1, -1];
    model.cells.forEach(function (s, k) {
      var y = ky(k); if (y < y0 || y > y1) return;
      var p = [kx(k), y, kz(k)]; c.set(k, s);
      for (var i = 0; i < 3; i++) { if (p[i] < mn[i]) mn[i] = p[i]; if (p[i] > mx[i]) mx[i] = p[i]; }
    });
    var out = new Map();
    c.forEach(function (s, k) { out.set(key(kx(k) - mn[0], ky(k) - mn[1], kz(k) - mn[2]), s); });
    return { cells: out, n: out.size, sx: c.size ? mx[0] - mn[0] + 1 : 0, sy: c.size ? mx[1] - mn[1] + 1 : 0, sz: c.size ? mx[2] - mn[2] + 1 : 0, offset: [c.size ? mn[0] : 0, c.size ? mn[1] : 0, c.size ? mn[2] : 0] };
  }
  function toCodes(model, maxLen) {
    maxLen = maxLen || MAX_CODE;
    var whole = serialize(model);
    if (whole.length <= maxLen) return { parts: [{ code: whole, offset: [0, 0, 0] }], error: null };
    var parts = [], y = 0;
    while (y < model.sy) {
      var best = null, y1 = y;
      for (; y1 < model.sy; y1++) {
        var sm = subModel(model, y, y1), code = serialize(sm);
        if (sm.n === 0) { best = best || { y1: y1, sm: sm, code: code }; continue; }
        if (code.length > maxLen) break;
        best = { y1: y1, sm: sm, code: code };
      }
      if (!best || (best.sm.n && best.y1 < y)) return { parts: parts, error: 'layer_too_complex', layer: y };
      if (best.sm.n) parts.push({ code: best.code, offset: best.sm.offset });
      y = best.y1 + 1;
    }
    var n = parts.length;
    parts.forEach(function (p, i) { p.code = p.code.replace('\n', '\n# part ' + (i + 1) + '/' + n + ', place at +' + p.offset.join(',') + '\n'); if (p.code.indexOf('\n') < 0) p.code += ''; });
    return { parts: parts, error: null };
  }

  /* tiny reader for tests: expands axis-form regions + aliases of serialize() output (not the full language) */
  function expand(code) {
    var alias = {}, cells = new Map();
    code.split('\n').forEach(function (line) {
      line = line.replace(/#.*/, '').trim(); if (!line) return;
      var m = /^\$(\w+) = (.+)$/.exec(line); if (m) { alias[m[1]] = m[2]; return; }
      var i = line.indexOf(' '), head = line.slice(0, i), rest = line.slice(i + 1).split(' ');
      var sp = head[0] === '$' ? alias[head.slice(1)] : head;
      rest.forEach(function (r) {
        var a = r.split(',').map(function (s) { var p = s.split('..').map(Number); return [p[0], p.length > 1 ? p[1] : p[0]]; });
        for (var x = a[0][0]; x <= a[0][1]; x++) for (var y = a[1][0]; y <= a[1][1]; y++) for (var z = a[2][0]; z <= a[2][1]; z++) cells.set(key(x, y, z), sp);
      });
    });
    return cells;
  }

  /* ---------------- 3D models: vanilla JSON / .bbmodel -> Simple Models code ---------------- */
  var DIRS = ['down', 'up', 'north', 'south', 'west', 'east'];
  var SLOTS = ['thirdperson_righthand', 'thirdperson_lefthand', 'firstperson_righthand', 'firstperson_lefthand', 'head', 'gui', 'ground', 'fixed', 'on_shelf'];
  var LIM = { elements: 256, chars: 65536, textures: 64, min: -16, max: 32 };
  function f32(x) { return Math.fround(x); }
  /* ModelJson.num: whole -> int text, else shortest float32 text */
  function numText(x) {
    var f = f32(x);
    if (f === Math.round(f) && Math.abs(f) < 1e6) return String(Math.round(f));
    for (var p = 1; p <= 9; p++) { var s = Number(f.toPrecision(p)); if (f32(s) === f) { return plain(s); } }
    return plain(f);
  }
  function plain(s) { var t = String(s); if (/e/i.test(t)) t = Number(s).toFixed(10).replace(/0+$/, '').replace(/\.$/, ''); return t; }
  function N(x) { return { __num: numText(x) }; }
  function vec(a) { return [N(a[0]), N(a[1]), N(a[2])]; }
  function checkId(s) { return /^([a-z0-9_.-]{1,64}:)?[a-z0-9_./-]{1,128}$/.test(s); }

  /* canonical "EditableModel"-like object (insertion order = write order of ModelJson.toJson) */
  function canonModel(src, warn) {
    var out = {};
    if (src.parent != null) out.parent = src.parent;
    if (typeof src.ambientocclusion === 'boolean') out.ambientocclusion = src.ambientocclusion;
    if (src.gui_light != null) out.gui_light = src.gui_light;
    var tex = src.textures || {};
    if (Object.keys(tex).length) {
      if (Object.keys(tex).length > LIM.textures) throw new Error('too_many_textures');
      out.textures = {}; Object.keys(tex).forEach(function (k) { out.textures[k] = String(tex[k]); });
    }
    var els = src.elements || [];
    if (els.length > LIM.elements) throw new Error('too_many_elements');
    if (els.length) out.elements = els.map(function (e, i) {
      var o = {}, from = e.from, to = e.to;
      if (!from || !to) throw new Error('bad_element:' + i);
      [from, to].forEach(function (v) { v.forEach(function (c) { if (c < LIM.min || c > LIM.max) throw new Error('bad_extent:elements[' + i + ']'); }); });
      if (e.name != null) o.name = e.name;
      o.from = vec(from); o.to = vec(to);
      var r = e.rotation;
      if (r && r.origin) {
        var ro = { origin: vec(r.origin) };
        if (r.axis != null) { ro.axis = r.axis; ro.angle = N(r.angle || 0); }
        else { ro.x = N(r.x || 0); ro.y = N(r.y || 0); ro.z = N(r.z || 0); }
        if (r.rescale) ro.rescale = true;
        o.rotation = ro;
      }
      if (e.shade_direction_override != null) o.shade_direction_override = e.shade_direction_override;
      else if (e.shade === false) warn('shade_dropped');
      if (e.light_emission) o.light_emission = e.light_emission;
      o.faces = {};
      DIRS.forEach(function (dn) {
        var f = (e.faces || {})[dn]; if (!f) return;
        var fo = {};
        if (f.uv) fo.uv = f.uv.map(N);
        fo.texture = f.texture;
        if (f.rotation) fo.rotation = f.rotation;
        if (f.cullface != null) fo.cullface = f.cullface;
        if (f.tintindex != null && f.tintindex !== -1) fo.tintindex = f.tintindex;
        o.faces[dn] = fo;
      });
      if (!Object.keys(o.faces).length) throw new Error('no_faces:' + i);
      return o;
    });
    var disp = src.display || {};
    var dk = SLOTS.filter(function (s) { return disp[s]; });
    if (dk.length) {
      out.display = {};
      dk.forEach(function (s) {
        var t = disp[s], o = {};
        if (t.rotation && t.rotation.some(Boolean)) o.rotation = vec(t.rotation);
        if (t.translation && t.translation.some(Boolean)) o.translation = vec(t.translation);
        if (t.scale && !(t.scale[0] === 1 && t.scale[1] === 1 && t.scale[2] === 1)) o.scale = vec(t.scale);
        if (t.right_rotation && t.right_rotation.some(Boolean)) o.right_rotation = vec(t.right_rotation);
        out.display[s] = o;
      });
    }
    return out;
  }
  function jstr(s) { return JSON.stringify(s).replace(/[\u007f-￿]/g, function (c) { return c; }); }
  function inline(e) {
    if (e && e.__num !== undefined) return e.__num;
    if (Array.isArray(e)) return '[' + e.map(inline).join(', ') + ']';
    if (e && typeof e === 'object') return '{' + Object.keys(e).map(function (k) { return jstr(k) + ': ' + inline(e[k]); }).join(', ') + '}';
    return JSON.stringify(e);
  }
  function compact(e, indent, prefix, width) {
    var one = inline(e), isC = e && typeof e === 'object' && e.__num === undefined;
    if (indent * 2 + prefix + one.length + 1 <= width || !isC) return one;
    var pad = '  '.repeat(indent + 1), obj = !Array.isArray(e), keys = obj ? Object.keys(e) : null, size = obj ? keys.length : e.length, out = obj ? '{\n' : '[\n';
    for (var i = 0; i < size; i++) {
      out += pad;
      if (obj) { var k = jstr(keys[i]) + ': '; out += k + compact(e[keys[i]], indent + 1, k.length, width); }
      else out += compact(e[i], indent + 1, 0, width);
      out += i + 1 < size ? ',\n' : '\n';
    }
    return out + '  '.repeat(indent) + (obj ? '}' : ']');
  }
  function modelCode(src, warn) {
    var code = compact(canonModel(src, warn || function () {}), 0, 0, 64);
    if (code.length > LIM.chars) throw new Error('too_long');
    return code;
  }

  /* Blockbench .bbmodel (Java Block/Item): pixel units like vanilla; per-face uv in texture resolution units */
  function bbmodelToVanilla(bb, warn) {
    warn = warn || function () {};
    var rw = (bb.resolution && bb.resolution.width) || 16, rh = (bb.resolution && bb.resolution.height) || 16;
    var texVars = {}, texNames = (bb.textures || []).map(function (t, i) {
      var n = String(t.name || t.id || ('t' + i)).replace(/\.png$/i, '').toLowerCase().replace(/[^a-z0-9_]/g, '_').replace(/^_+|_+$/g, '') || ('t' + i);
      var v = n, c = 2; while (texVars[v]) v = n + '_' + c++;
      texVars[v] = 'mymodels:block/' + n; return v;
    });
    var elements = [];
    (bb.elements || []).forEach(function (el) {
      if (el.type && el.type !== 'cube') { warn('non_cube:' + el.type); return; }
      var e = { name: el.name, from: el.from, to: el.to, faces: {} };
      var rot = el.rotation || [0, 0, 0], nz = [0, 1, 2].filter(function (i) { return rot[i]; });
      if (nz.length === 1) e.rotation = { origin: el.origin || [8, 8, 8], axis: 'xyz'[nz[0]], angle: rot[nz[0]] };
      else if (nz.length > 1) e.rotation = { origin: el.origin || [8, 8, 8], x: rot[0], y: rot[1], z: rot[2] };
      if (el.light_emission) e.light_emission = el.light_emission;
      DIRS.forEach(function (dn) {
        var f = (el.faces || {})[dn];
        if (!f || f.texture == null || f.texture === false) return; // Blockbench: no texture = face not drawn
        var tv = texNames[f.texture]; if (!tv) { warn('face_without_texture'); return; }
        var uv = f.uv ? [f.uv[0] * 16 / rw, f.uv[1] * 16 / rh, f.uv[2] * 16 / rw, f.uv[3] * 16 / rh] : null;
        var o = { texture: '#' + tv }; if (uv) o.uv = uv;
        if (f.rotation) o.rotation = f.rotation; if (f.cullface) o.cullface = f.cullface; if (f.tint != null && f.tint !== -1) o.tintindex = f.tint;
        e.faces[dn] = o;
      });
      if (Object.keys(e.faces).length) elements.push(e);
    });
    if (bb.meta && bb.meta.box_uv) warn('box_uv');
    return { parent: 'minecraft:block/block', textures: texVars, elements: elements, display: bb.display };
  }

  root.Conv51 = { GRID: GRID, MAX_CODE: MAX_CODE, readNbt: readNbt, writeNbt: writeNbt, ungzip: ungzip, isGzip: isGzip, structureToModel: structureToModel,
    serialize: serialize, boxes: boxes, toCodes: toCodes, expand: expand, key: key, kx: kx, ky: ky, kz: kz, spec: spec, modelCode: modelCode,
    bbmodelToVanilla: bbmodelToVanilla, numText: numText, LIM: LIM, sortedKeys: sortedKeys };
  if (typeof module !== 'undefined' && module.exports) module.exports = root.Conv51;
})(typeof window !== 'undefined' ? window : globalThis);
