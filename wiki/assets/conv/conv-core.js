/* conv-core.js (N51, b-conv51): pure JS, no libraries, no network. Runs in the browser and in node.
 * Blueprint code = text "Bausprache" of Simple Building Tools; port of BlueprintCode.serialize / boxes / spec.
 * Model code = vanilla 26.3 model JSON in the compact layout of Simple Models (ModelJson.writeCompact). */
(function (root) {
  'use strict';
  var GRID = 256, WRAP = 100, MAX_CODE = 32000;

  var D = root.Conv51Data || (typeof require === 'function' ? require('./conv-data.js') : null);
  var MAX_CELLS = 4194304; // BlueprintCode.MAX_EXPANDED_CELLS: most occupied cells one code may set

  /* ---------------- NBT (big endian), gzip detection ---------------- */
  /* Long arrays (tag 12) stay as 32-bit halves: u32[2i] = low, u32[2i+1] = high word of long i (bit k = bit k&31 of u32[k>>5]). */
  function LongArray(n) { this.length = n; this.u32 = new Uint32Array(2 * n); }
  function readNbt(buf) {
    var dv = new DataView(buf.buffer, buf.byteOffset, buf.byteLength), p = 0, dec = new TextDecoder('utf-8');
    function str() { var n = dv.getUint16(p); p += 2; var s = dec.decode(buf.subarray(p, p + n)); p += n; return s; }
    function len() { var n = dv.getInt32(p); p += 4; if (n < 0 || n > buf.length) throw new Error('nbt_bad_length'); return n; }
    function val(t, depth) {
      var v, n, i;
      if (depth > 512) throw new Error('nbt_too_deep');
      switch (t) {
        case 1: v = dv.getInt8(p); p += 1; return v;
        case 2: v = dv.getInt16(p); p += 2; return v;
        case 3: v = dv.getInt32(p); p += 4; return v;
        case 4: v = dv.getBigInt64(p); p += 8; return v;
        case 5: v = dv.getFloat32(p); p += 4; return v;
        case 6: v = dv.getFloat64(p); p += 8; return v;
        case 7: n = len(); v = new Int8Array(buf.buffer.slice(buf.byteOffset + p, buf.byteOffset + p + n)); p += n; return v;
        case 8: return str();
        case 9: var et = dv.getUint8(p++); n = len(); v = []; for (i = 0; i < n; i++) v.push(val(et, depth + 1)); return v;
        case 10: v = {}; for (;;) { var tt = dv.getUint8(p++); if (tt === 0) break; var k = str(); v[k] = val(tt, depth + 1); } return v;
        case 11: n = len(); v = new Int32Array(n); for (i = 0; i < n; i++) { v[i] = dv.getInt32(p); p += 4; } return v;
        case 12: n = len(); v = new LongArray(n); for (i = 0; i < n; i++) { v.u32[2 * i + 1] = dv.getUint32(p); v.u32[2 * i] = dv.getUint32(p + 4); p += 8; } return v;
        default: throw new Error('nbt_bad_tag:' + t);
      }
    }
    if (buf.length < 3 || dv.getUint8(0) !== 10) throw new Error('nbt_not_compound');
    p = 1; str();
    try { return val(10, 0); } catch (e) { if (e instanceof RangeError) throw new Error('nbt_truncated'); throw e; }
  }
  /* Typed values for the writer: tag(t, v). Plain JS: string -> 8, number -> 3 (int), array -> 9, object -> 10,
   * Int8Array/Uint8Array -> 7, Int32Array -> 11, LongArray -> 12, bigint -> 4. */
  function tag(t, v) { return { __t: t, v: v }; }
  function writeNbt(obj, name) {
    var cap = 1 << 16, out = new Uint8Array(cap), n = 0, enc = new TextEncoder(), f = new DataView(new ArrayBuffer(8));
    function grow(k) { if (n + k <= cap) return; while (n + k > cap) cap *= 2; var o = new Uint8Array(cap); o.set(out.subarray(0, n)); out = o; }
    function b(x) { grow(1); out[n++] = x & 255; }
    function i16(x) { grow(2); out[n++] = (x >> 8) & 255; out[n++] = x & 255; }
    function i32(x) { grow(4); out[n++] = (x >>> 24) & 255; out[n++] = (x >>> 16) & 255; out[n++] = (x >>> 8) & 255; out[n++] = x & 255; }
    function raw(k) { grow(k); for (var i = 0; i < k; i++) out[n++] = f.getUint8(i); }
    function str(s) { var e = enc.encode(s); i16(e.length); grow(e.length); out.set(e, n); n += e.length; }
    function tagOf(v) {
      if (v && v.__t) return v.__t;
      if (typeof v === 'string') return 8;
      if (typeof v === 'number') return 3;
      if (typeof v === 'bigint') return 4;
      if (typeof v === 'boolean') return 1;
      if (v instanceof Int8Array || v instanceof Uint8Array) return 7;
      if (v instanceof Int32Array) return 11;
      if (v instanceof LongArray) return 12;
      if (Array.isArray(v)) return 9;
      return 10;
    }
    function val(v, t) {
      var d = v && v.__t ? v.v : v, i;
      switch (t) {
        case 1: b(d === true ? 1 : d === false ? 0 : d); break;
        case 2: i16(d); break;
        case 3: i32(d); break;
        case 4: f.setBigInt64(0, BigInt(d)); raw(8); break;
        case 5: f.setFloat32(0, d); raw(4); break;
        case 6: f.setFloat64(0, d); raw(8); break;
        case 7: i32(d.length); grow(d.length); out.set(new Uint8Array(d.buffer, d.byteOffset, d.length), n); n += d.length; break;
        case 8: str(d); break;
        case 9: var et = d.length ? tagOf(d[0]) : 0; b(et); i32(d.length); for (i = 0; i < d.length; i++) val(d[i], et); break;
        case 10: Object.keys(d).forEach(function (k) { if (d[k] === undefined) return; var tt = tagOf(d[k]); b(tt); str(k); val(d[k], tt); }); b(0); break;
        case 11: i32(d.length); for (i = 0; i < d.length; i++) i32(d[i]); break;
        case 12: i32(d.length); for (i = 0; i < d.length; i++) { i32(d.u32[2 * i + 1]); i32(d.u32[2 * i]); } break;
        default: throw new Error('nbt_write_tag:' + t);
      }
    }
    b(10); str(name || ''); val(obj, 10);
    return out.slice(0, n);
  }
  function isGzip(buf) { return buf.length > 2 && buf[0] === 0x1f && buf[1] === 0x8b; }
  /* gunzip: injected (node zlib) or the browser's DecompressionStream; plain NBT passes through */
  async function ungzip(buf, gunzip) {
    if (!isGzip(buf)) return buf;
    if (gunzip) return gunzip(buf);
    if (typeof DecompressionStream === 'undefined') throw new Error('no_gzip');
    var ds = new Response(new Blob([buf]).stream().pipeThrough(new DecompressionStream('gzip')));
    try { return new Uint8Array(await ds.arrayBuffer()); } catch (e) { throw new Error('bad_gzip'); }
  }
  async function gzip(buf, gz) {
    if (gz) return gz(buf);
    if (typeof CompressionStream === 'undefined') throw new Error('no_gzip');
    var cs = new Response(new Blob([buf]).stream().pipeThrough(new CompressionStream('gzip')));
    return new Uint8Array(await cs.arrayBuffer());
  }

  /* ---------------- block ids: known blocks, defaults, renames (tables in conv-data.js) ---------------- */
  var KNOWN = new Set(), NO_ITEM = new Set(), DEFAULT_RULES = [], VALUE_MAPS = [];
  if (D) {
    Object.keys(D.blocks).forEach(function (ns) { D.blocks[ns].split(' ').forEach(function (b) { KNOWN.add(ns + ':' + b); }); });
    D.noItem.split(' ').forEach(function (b) { NO_ITEM.add('minecraft:' + b); });
    DEFAULT_RULES = D.defaults.map(function (r) { return [new RegExp(r[0]), r[1]]; });
    VALUE_MAPS = D.valueMaps.map(function (r) { return [new RegExp(r[0]), r[1], r[2]]; });
  }
  function fullId(id) { id = String(id).trim().toLowerCase(); return id.indexOf(':') < 0 ? 'minecraft:' + id : id; }
  function isKnown(id) { return KNOWN.has(fullId(id)); }
  function defaultsFor(name) {
    for (var i = 0; i < DEFAULT_RULES.length; i++) if (DEFAULT_RULES[i][0].test(name)) return DEFAULT_RULES[i][1];
    return null;
  }
  /* old ids/values -> 26.3; only when the old id is no current block (modern files stay untouched) */
  function upgrade(id, props, dataVersion, info) {
    id = fullId(id);
    if (!D || id.indexOf('minecraft:') !== 0) return id;
    var path = id.slice(10), to = null;
    if (!KNOWN.has(id) && D.renames[path]) to = D.renames[path];
    D.renamesBelow.forEach(function (r) { if (dataVersion != null && dataVersion < r[0] && path === r[1]) to = r[2]; });
    if (to) { if (info) info.renamed[path] = to; path = to; id = 'minecraft:' + to; }
    VALUE_MAPS.forEach(function (m) {
      if (!m[0].test(path)) return;
      m[1].forEach(function (k) { if (props[k] != null && m[2][props[k]] != null) props[k] = m[2][props[k]]; });
    });
    if (path === 'cauldron' && props.level != null) { var lv = props.level; delete props.level; if (lv !== '0') { id = 'minecraft:water_cauldron'; props.level = lv; } }
    return id;
  }
  /* Kurzform eines Zustands; props: {name:value} (strings). Unknown default -> property kept (always parses back equal). */
  function spec(id, props, info) {
    var ns = id.indexOf(':') < 0 ? 'minecraft' : id.slice(0, id.indexOf(':')), path = id.slice(id.indexOf(':') + 1);
    var name = ns === 'minecraft' ? path : ns + ':' + path;
    var defs = ns === 'minecraft' ? defaultsFor(path) : null, parts = [];
    Object.keys(props || {}).sort().forEach(function (k) {
      if (defs && defs[k] === props[k]) return;
      if (!defs || !(k in defs)) { if (info) info.unknownDefaults[name] = true; }
      parts.push(k + '=' + props[k]);
    });
    return parts.length ? name + '[' + parts.join(',') + ']' : name;
  }
  /* "id[k=v,...]" -> {id, props} */
  function parseState(text) {
    var t = String(text).trim(), i = t.indexOf('['), props = {};
    if (i >= 0) {
      var inner = t.slice(i + 1, t.lastIndexOf(']') > i ? t.lastIndexOf(']') : t.length);
      inner.split(',').forEach(function (kv) { var e = kv.indexOf('='); if (e > 0) props[kv.slice(0, e).trim()] = kv.slice(e + 1).trim(); });
      t = t.slice(0, i);
    }
    return { id: fullId(t), props: props };
  }

  /* ---------------- model builder (all readers) ---------------- */
  var SKIP = /^minecraft:(air|cave_air|void_air|structure_void|moving_piston)$/;
  /* opts.unknown: 'keep' (default, marked), 'skip', or a replacement block id. opts.dataVersion of the file. */
  function newInfo(format) {
    return { format: format, skipped: 0, unknownDefaults: {}, blockEntities: 0, entities: 0, dataVersion: null, size: null, tooBig: false,
      unknown: {}, noItem: {}, renamed: {}, approx: 0, regions: null, notes: [] };
  }
  function builder(info, opts) {
    opts = opts || {};
    var cells = new Map(), mn = [Infinity, Infinity, Infinity], mx = [-Infinity, -Infinity, -Infinity], cache = new Map();
    return {
      info: info,
      /* block state -> spec string, or null when it is not placed */
      state: function (id, props) {
        var ck = id + JSON.stringify(props || {});
        if (cache.has(ck)) return cache.get(ck);
        var p = {}; Object.keys(props || {}).forEach(function (k) { p[k] = String(props[k]); });
        var fid = upgrade(id, p, info.dataVersion, info), out;
        if (SKIP.test(fid)) out = null;
        else {
          if (D && !KNOWN.has(fid)) {
            if (opts.unknown === 'skip') out = null;
            else if (opts.unknown && opts.unknown !== 'keep') { var r = parseState(opts.unknown); out = spec(r.id, r.props, info); }
          }
          if (out === undefined) out = spec(fid, p, info);
        }
        cache.set(ck, out);
        return out;
      },
      put: function (x, y, z, s, id) {
        if (s === null || s === undefined) { info.skipped++; return; }
        if (x < 0 || y < 0 || z < 0 || x >= 4096 || y >= 4096 || z >= 4096) throw new Error('too_large_file');
        cells.set(key(x, y, z), s);
        if (x < mn[0]) mn[0] = x; if (y < mn[1]) mn[1] = y; if (z < mn[2]) mn[2] = z;
        if (x > mx[0]) mx[0] = x; if (y > mx[1]) mx[1] = y; if (z > mx[2]) mx[2] = z;
      },
      done: function () {
        var out = new Map(), base = [mn[0], mn[1], mn[2]];
        if (!cells.size) base = [0, 0, 0];
        cells.forEach(function (s, k) { out.set(key(kx(k) - base[0], ky(k) - base[1], kz(k) - base[2]), s); });
        var m = { cells: out, n: out.size, sx: cells.size ? mx[0] - mn[0] + 1 : 0, sy: cells.size ? mx[1] - mn[1] + 1 : 0, sz: cells.size ? mx[2] - mn[2] + 1 : 0, info: info, origin: base };
        markModel(m);
        return m;
      }
    };
  }
  /* counts per block id: unknown ids and ids without item (creative blueprint needed) */
  function markModel(m) {
    var info = m.info; info.unknown = {}; info.noItem = {};
    m.cells.forEach(function (s) {
      var id = fullId(s.split('[')[0]);
      if (D && !KNOWN.has(id)) info.unknown[id] = (info.unknown[id] || 0) + 1;
      if (NO_ITEM.has(id)) info.noItem[id] = (info.noItem[id] || 0) + 1;
    });
    info.tooBig = Math.max(m.sx, m.sy, m.sz) > GRID;
  }

  /* ---------------- structure .nbt (vanilla, Create) -> model ---------------- */
  function structureToModel(nbt, opts) {
    opts = opts || {};
    var info = newInfo('nbt');
    info.entities = (nbt.entities || []).length; info.dataVersion = nbt.DataVersion == null ? null : nbt.DataVersion; info.size = nbt.size || null;
    var palette = nbt.palette || (nbt.palettes && nbt.palettes[0]);
    if (!palette || !nbt.blocks) throw new Error('not_structure');
    if (nbt.palettes && nbt.palettes.length > 1) info.notes.push('palettes:' + nbt.palettes.length);
    var bld = builder(info, opts);
    var specs = palette.map(function (e) { return bld.state(e.Name, e.Properties || {}); });
    nbt.blocks.forEach(function (b) {
      if (b.nbt) info.blockEntities++;
      bld.put(b.pos[0], b.pos[1], b.pos[2], specs[b.state]);
    });
    return bld.done();
  }
  function key(x, y, z) { return x + y * 4096 + z * 16777216; } // collision-free for x,y,z < 4096 (Java: 8 bit each, grid 256)
  function kx(k) { return k % 4096; } function ky(k) { return Math.floor(k / 4096) % 4096; } function kz(k) { return Math.floor(k / 16777216); }

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

  /* cells of model inside box [x0..x1, y0..y1, z0..z1], normalised on their own; offset = min corner in the model */
  function subModel(model, x0, x1, y0, y1, z0, z1) {
    var c = [], mn = [Infinity, Infinity, Infinity], mx = [-1, -1, -1];
    model.cells.forEach(function (s, k) {
      var x = kx(k), y = ky(k), z = kz(k);
      if (y < y0 || y > y1 || x < x0 || x > x1 || z < z0 || z > z1) return;
      c.push([x, y, z, s]);
      if (x < mn[0]) mn[0] = x; if (y < mn[1]) mn[1] = y; if (z < mn[2]) mn[2] = z;
      if (x > mx[0]) mx[0] = x; if (y > mx[1]) mx[1] = y; if (z > mx[2]) mx[2] = z;
    });
    var out = new Map();
    c.forEach(function (e) { out.set(key(e[0] - mn[0], e[1] - mn[1], e[2] - mn[2]), e[3]); });
    var any = c.length > 0;
    return { cells: out, n: out.size, sx: any ? mx[0] - mn[0] + 1 : 0, sy: any ? mx[1] - mn[1] + 1 : 0, sz: any ? mx[2] - mn[2] + 1 : 0, offset: any ? mn : [0, 0, 0] };
  }
  function yBand(model, y0, y1) { return subModel(model, 0, Infinity, y0, y1, 0, Infinity); }
  /* Code parts of one model (<= 256 per axis): whole code if it fits, else y bands from the bottom, each <= maxLen
   * characters and <= MAX_CELLS cells; each part gets "# part i/n, place at +x,y,z". */
  function toCodes(model, maxLen) {
    maxLen = maxLen || MAX_CODE;
    var whole = model.n <= MAX_CELLS ? serialize(model) : null;
    if (whole !== null && whole.length <= maxLen) return { parts: [{ code: whole, offset: [0, 0, 0], n: model.n }], error: null };
    var parts = [], y = 0, perLayer = [];
    for (var yy = 0; yy < model.sy; yy++) perLayer.push(0);
    model.cells.forEach(function (s, k) { perLayer[ky(k)]++; });
    var cum = [0]; perLayer.forEach(function (c, i) { cum.push(cum[i] + c); });
    function attempt(y0, y1) { // band [y0..y1] or null when it breaks a limit
      if (cum[y1 + 1] - cum[y0] > MAX_CELLS) return null;
      var sm = yBand(model, y0, y1), code = serialize(sm);
      return code.length > maxLen ? null : { y1: y1, sm: sm, code: code };
    }
    while (y < model.sy) {
      if (perLayer[y] === 0) { y++; continue; }
      /* galloping + binary search for the highest band end that still fits (length grows with the band) */
      var best = attempt(y, y);
      if (!best) return { parts: parts, error: 'layer_too_complex', layer: y };
      var lo = y, step = 1, hi = -1;
      while (lo + step < model.sy) { var t = attempt(y, lo + step); if (!t) { hi = lo + step; break; } best = t; lo += step; step *= 2; }
      if (hi < 0) hi = model.sy;
      while (hi - lo > 1) { var mid = (lo + hi) >> 1, r = attempt(y, mid); if (r) { best = r; lo = mid; } else hi = mid; }
      parts.push({ code: best.code, offset: best.sm.offset, n: best.sm.n });
      y = best.y1 + 1;
    }
    var n = parts.length;
    parts.forEach(function (p, i) { p.code = p.code.replace('\n', '\n# part ' + (i + 1) + '/' + n + ', place at +' + p.offset.join(',') + '\n'); });
    return { parts: parts, error: null };
  }
  /* Buildings over 256 on an axis: 256er tiles (x, then z, then y), each split into parts. Offsets are relative to the
   * building's min corner; every code carries "# tile t/T, part i/n, place at +x,y,z". */
  function toTiles(model, maxLen) {
    if (Math.max(model.sx, model.sy, model.sz) <= GRID) {
      var r = toCodes(model, maxLen); r.tiles = 1; return r;
    }
    var tiles = [];
    for (var ty = 0; ty < model.sy; ty += GRID) for (var tz = 0; tz < model.sz; tz += GRID) for (var tx = 0; tx < model.sx; tx += GRID) {
      var t = subModel(model, tx, tx + GRID - 1, ty, ty + GRID - 1, tz, tz + GRID - 1);
      if (t.n) tiles.push(t);
    }
    var parts = [], error = null, layer;
    tiles.forEach(function (t, ti) {
      var r = toCodes(t, maxLen);
      if (r.error && !error) { error = r.error; layer = t.offset[1] + r.layer; }
      r.parts.forEach(function (p, pi) {
        var off = [t.offset[0] + p.offset[0], t.offset[1] + p.offset[1], t.offset[2] + p.offset[2]];
        var body = p.code.replace(/^# part [^\n]*\n/m, '');
        var lines = body.split('\n');
        lines.splice(1, 0, '# tile ' + (ti + 1) + '/' + tiles.length + ', part ' + (pi + 1) + '/' + r.parts.length + ', place at +' + off.join(','));
        parts.push({ code: lines.join('\n'), offset: off, n: p.n, tile: ti + 1 });
      });
    });
    return { parts: parts, error: error, layer: layer, tiles: tiles.length };
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
      /* resource id: written by modelToBbmodel, else Blockbench's namespace/folder, else the Simple Models namespace */
      var file = String(t.name || n).replace(/\.png$/i, '');
      texVars[v] = t.resource_id && checkId(t.resource_id) ? t.resource_id
        : t.namespace && checkId(t.namespace + ':' + (t.folder ? t.folder + '/' : '') + file) ? t.namespace + ':' + (t.folder ? t.folder + '/' : '') + file
        : 'mymodels:block/' + n;
      return v;
    });
    var elements = [];
    (bb.elements || []).forEach(function (el) {
      if (el.type && el.type !== 'cube') { warn('non_cube:' + el.type); return; }
      var e = { name: el.name, from: el.from, to: el.to, faces: {} };
      var rot = el.rotation || [0, 0, 0], nz = [0, 1, 2].filter(function (i) { return rot[i]; });
      if (nz.length === 1) e.rotation = { origin: el.origin || [8, 8, 8], axis: 'xyz'[nz[0]], angle: rot[nz[0]] };
      else if (nz.length > 1) e.rotation = { origin: el.origin || [8, 8, 8], x: rot[0], y: rot[1], z: rot[2] };
      if (e.rotation && el.rescale) e.rotation.rescale = true;
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
    if ((bb.elements || []).length > 0 && bb.outliner && bb.outliner.some(function (o) { return o && typeof o === 'object'; })) warn('groups_flattened');
    if (bb.animations && bb.animations.length) warn('animations_dropped');
    var out = { parent: typeof bb.parent === 'string' && bb.parent ? bb.parent : 'minecraft:block/block' };
    if (bb.ambientocclusion === false) out.ambientocclusion = false;
    if (bb.front_gui_light) out.gui_light = 'front';
    var part = (bb.textures || []).map(function (t, i) { return t.particle ? texNames[i] : null; }).filter(Boolean)[0];
    out.textures = Object.assign({}, texVars);
    if (part && !out.textures.particle) out.textures.particle = '#' + part;
    out.elements = elements; out.display = bb.display;
    return out;
  }

  root.Conv51 = { GRID: GRID, MAX_CODE: MAX_CODE, MAX_CELLS: MAX_CELLS, DATA: D, readNbt: readNbt, writeNbt: writeNbt, tag: tag, LongArray: LongArray,
    ungzip: ungzip, gzip: gzip, isGzip: isGzip, structureToModel: structureToModel, builder: builder, newInfo: newInfo, markModel: markModel,
    upgrade: upgrade, parseState: parseState, fullId: fullId, isKnown: isKnown, defaultsFor: defaultsFor,
    serialize: serialize, boxes: boxes, toCodes: toCodes, toTiles: toTiles, subModel: subModel, expand: expand, key: key, kx: kx, ky: ky, kz: kz,
    spec: spec, modelCode: modelCode, canonModel: canonModel, bbmodelToVanilla: bbmodelToVanilla, numText: numText, LIM: LIM, sortedKeys: sortedKeys };
  if (typeof module !== 'undefined' && module.exports) module.exports = root.Conv51;
})(typeof window !== 'undefined' ? window : globalThis);
