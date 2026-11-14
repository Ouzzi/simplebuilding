/* conv-formats.js (N51, b-conv51): building files -> model and model -> building files. Pure JS, no network.
 * Read: vanilla structure .nbt (also Create schematics), Sponge .schem v1/v2/v3, MCEdit .schematic (1.12 numeric ids),
 * Litematica .litematic (all regions or one). Axiom .bp is recognised and explained (not read, plan section 2/12).
 * Write: vanilla structure .nbt and Sponge .schem v3 (gzip). */
(function (root) {
  'use strict';
  var C = root.Conv51 || require('./conv-core.js');
  var D = C.DATA, MAX_VOLUME = 1 << 28;

  function num(v) { return typeof v === 'bigint' ? Number(v) : +v; }
  function u16(v) { v = num(v); return v < 0 ? v + 65536 : v; } // Sponge sizes are unsigned shorts
  function guardVolume(a, b, c) { if (a * b * c > MAX_VOLUME || a > 4096 || b > 4096 || c > 4096) throw new Error('too_large_file'); }

  /* ---------------- Sponge .schem v1 / v2 / v3 ---------------- */
  function sponge(nbt, opts) {
    var s = nbt.Schematic && typeof nbt.Schematic === 'object' && !Array.isArray(nbt.Schematic) ? nbt.Schematic : nbt;
    var v3 = num(s.Version) === 3 || (s.Blocks && s.Blocks.Palette && !(s.Blocks instanceof Int8Array));
    var info = C.newInfo('schem');
    info.version = v3 ? 3 : num(s.Version || 1);
    info.dataVersion = s.DataVersion == null ? null : num(s.DataVersion);
    var w = u16(s.Width), h = u16(s.Height), l = u16(s.Length);
    guardVolume(w, h, l);
    info.size = [w, h, l];
    var blocks = v3 ? s.Blocks : s, pal = blocks && blocks.Palette, data = v3 ? blocks && blocks.Data : s.BlockData;
    if (!pal || !data) throw new Error('schem_no_blocks');
    info.blockEntities = ((v3 ? blocks.BlockEntities : (s.BlockEntities || s.TileEntities)) || []).length;
    info.entities = (s.Entities || []).length;
    var bld = C.builder(info, opts), specs = [];
    Object.keys(pal).forEach(function (st) { var r = C.parseState(st); specs[num(pal[st])] = bld.state(r.id, r.props); });
    var i = 0, p = 0, vol = w * h * l;
    while (p < data.length && i < vol) {
      var v = 0, shift = 0, b;
      do { b = data[p++] & 255; v |= (b & 127) << shift; shift += 7; } while (b & 128 && p < data.length && shift < 35);
      var x = i % w, z = Math.floor(i / w) % l, y = Math.floor(i / (w * l));
      if (specs[v] === undefined) { info.skipped++; info.badIndex = (info.badIndex || 0) + 1; } else bld.put(x, y, z, specs[v]);
      i++;
    }
    if (i < vol) info.notes.push('short_data');
    return bld.done();
  }

  /* ---------------- Litematica .litematic ---------------- */
  function litRegions(nbt) {
    return Object.keys(nbt.Regions || {}).map(function (name) {
      var r = nbt.Regions[name], pos = r.Position || {}, size = r.Size || {};
      var sx = num(size.x), sy = num(size.y), sz = num(size.z);
      var min = [num(pos.x) + (sx < 0 ? sx + 1 : 0), num(pos.y) + (sy < 0 ? sy + 1 : 0), num(pos.z) + (sz < 0 ? sz + 1 : 0)];
      return { name: name, r: r, min: min, size: [Math.abs(sx), Math.abs(sy), Math.abs(sz)] };
    });
  }
  /* opts.region: name of one region, or null/undefined for all regions merged in their common frame */
  function litematic(nbt, opts) {
    opts = opts || {};
    var info = C.newInfo('litematic');
    info.dataVersion = nbt.MinecraftDataVersion == null ? null : num(nbt.MinecraftDataVersion);
    info.version = nbt.Version == null ? null : num(nbt.Version);
    info.name = nbt.Metadata && nbt.Metadata.Name || null;
    var regs = litRegions(nbt);
    if (!regs.length) throw new Error('litematic_no_regions');
    info.regions = regs.map(function (g) { return { name: g.name, size: g.size, min: g.min }; });
    var bld = C.builder(info, opts), base = [0, 1, 2].map(function (a) { return Math.min.apply(null, regs.map(function (g) { return g.min[a]; })); });
    if (opts.region != null && !regs.some(function (g) { return g.name === opts.region; })) throw new Error('litematic_no_region');
    regs.forEach(function (g) {
      if (opts.region != null && g.name !== opts.region) return;
      var r = g.r, pal = r.BlockStatePalette || [], la = r.BlockStates, sx = g.size[0], sy = g.size[1], sz = g.size[2];
      guardVolume(sx, sy, sz);
      info.blockEntities += (r.TileEntities || []).length; info.entities += (r.Entities || []).length;
      if (!pal.length || !la) return;
      var specs = pal.map(function (e) { return bld.state(e.Name, e.Properties || {}); });
      var bits = Math.max(2, 32 - Math.clz32(pal.length - 1)), mask = bits >= 31 ? 0x7fffffff : (1 << bits) - 1, u = la.u32 || null, vol = sx * sy * sz;
      if (!u) throw new Error('litematic_bad_states');
      if (Math.ceil(vol * bits / 64) > la.length) throw new Error('litematic_bad_states');
      for (var i = 0; i < vol; i++) {
        var s = i * bits, wd = Math.floor(s / 32), o = s % 32, v = u[wd] >>> o;
        if (o + bits > 32) v |= u[wd + 1] << (32 - o);
        v &= mask;
        var x = i % sx, z = Math.floor(i / sx) % sz, y = Math.floor(i / (sx * sz));
        var sp = specs[v];
        if (sp === undefined) { info.skipped++; continue; }
        if (sp === null) { info.skipped++; continue; }
        bld.put(g.min[0] - base[0] + x, g.min[1] - base[1] + y, g.min[2] - base[2] + z, sp);
      }
    });
    return bld.done();
  }

  /* ---------------- MCEdit .schematic (1.12 numeric ids) ---------------- */
  var RAIL = ['north_south', 'east_west', 'ascending_east', 'ascending_west', 'ascending_north', 'ascending_south', 'south_east', 'south_west', 'north_west', 'north_east'];
  var F6 = ['down', 'up', 'north', 'south', 'west', 'east'], F4 = { 2: 'north', 3: 'south', 4: 'west', 5: 'east' };
  /* (id, meta, meta of the block below) -> {name, props, approx} or null for an unknown id */
  function legacyState(id, meta, below) {
    var e = D && D.legacy[String(id)];
    if (e == null) return null;
    if (typeof e === 'string') return { name: e, props: {}, approx: meta !== 0 && e !== 'air' };
    var names = e[0], mask = e[1], kind = e[2] || '', list = Array.isArray(names);
    var name = list ? (names[meta & mask] || names[0]) : names, p = {}, approx = !kind && list ? (meta & ~mask) !== 0 || names[meta & mask] == null : false;
    switch (kind) {
      case 'level': p.level = String(meta & 15); break;
      case 'log':
        var ax = (meta >> 2) & 3;
        if (ax === 3) { name = name.replace(/_log$/, '_wood'); p.axis = 'y'; } else p.axis = ['y', 'x', 'z'][ax];
        break;
      case 'leaves': p.persistent = meta & 4 ? 'true' : 'false'; break;
      case 'facing6': p.facing = F6[meta & 7] || 'north'; if (meta & 8) approx = true; break;
      case 'prail': p.shape = RAIL[meta & 7] || 'north_south'; p.powered = meta & 8 ? 'true' : 'false'; break;
      case 'rail': p.shape = RAIL[meta] || 'north_south'; break;
      case 'dslab': name = list ? (names[meta & 7] || names[0]) : names; p.type = 'double'; break;
      case 'slab': p.type = meta & 8 ? 'top' : 'bottom'; break;
      case 'torch': case 'torch_off':
        if (meta >= 1 && meta <= 4) { name = name.replace(/torch$/, 'wall_torch'); p.facing = ['east', 'west', 'south', 'north'][meta - 1]; }
        if (kind === 'torch_off') p.lit = 'false';
        break;
      case 'stairs': p.facing = ['east', 'west', 'south', 'north'][meta & 3]; p.half = meta & 4 ? 'top' : 'bottom'; break;
      case 'facing4': p.facing = F4[meta & 7] || 'north'; break;
      case 'litfacing4': p.facing = F4[meta & 7] || 'north'; p.lit = 'true'; break;
      case 'age': p.age = String(meta & 7); break;
      case 'rot16': p.rotation = String(meta & 15); break;
      case 'door':
        var lower = meta & 8 ? (below == null ? 0 : below) : meta;
        if (meta & 8 && below == null) approx = true;
        p.facing = ['east', 'south', 'west', 'north'][lower & 3]; p.open = lower & 4 ? 'true' : 'false';
        p.half = meta & 8 ? 'upper' : 'lower'; if (meta & 8) p.hinge = meta & 1 ? 'right' : 'left';
        break;
      case 'dplant':
        if (meta & 8) { name = names[(below == null ? 2 : below) & 7] || names[2]; p.half = 'upper'; if (below == null) approx = true; }
        else name = names[meta & 7] || names[2];
        break;
      case 'pumpkin': p.facing = ['south', 'west', 'north', 'east'][meta & 3]; break;
      case 'snow': p.layers = String((meta & 7) + 1); break;
      case 'lit': p.lit = 'true'; break;
      case 'inverted': p.inverted = 'true'; break;
      case 'quartz': if (meta >= 2 && meta <= 4) p.axis = ['y', 'x', 'z'][meta - 2]; break;
      case 'axis': p.axis = ['y', 'x', 'z'][(meta >> 2) & 3] || 'y'; break;
    }
    return { name: name, props: p, approx: approx };
  }
  function mcedit(nbt, opts) {
    var info = C.newInfo('schematic');
    if (nbt.Materials && nbt.Materials !== 'Alpha') throw new Error('schematic_not_alpha');
    var w = u16(nbt.Width), h = u16(nbt.Height), l = u16(nbt.Length), ids = nbt.Blocks, data = nbt.Data || new Int8Array(ids.length), add = nbt.AddBlocks || null;
    guardVolume(w, h, l);
    info.size = [w, h, l]; info.dataVersion = 1343; // MCEdit/Schematica: Minecraft 1.12 and older
    info.blockEntities = (nbt.TileEntities || []).length; info.entities = (nbt.Entities || []).length;
    if (nbt.SchematicaMapping) info.notes.push('schematica_mapping');
    var bld = C.builder(info, opts), cache = new Map(), vol = Math.min(w * h * l, ids.length);
    function idAt(i) {
      var b = ids[i] & 255;
      if (add && (i >> 1) < add.length) b |= (i & 1) === 0 ? (add[i >> 1] & 0x0f) << 8 : (add[i >> 1] & 0xf0) << 4;
      return b;
    }
    for (var i = 0; i < vol; i++) {
      var id = idAt(i);
      if (id === 0) continue;
      var meta = data[i] & 15, below = null;
      var e = D && D.legacy[String(id)];
      if (e && typeof e !== 'string' && (e[2] === 'door' || e[2] === 'dplant') && (meta & 8) && i >= w * l && idAt(i - w * l) === id) below = data[i - w * l] & 15;
      var ck = id + ':' + meta + ':' + below, hit = cache.get(ck);
      if (hit === undefined) {
        var st = legacyState(id, meta, below);
        hit = st ? { sp: bld.state(st.name, st.props), approx: st.approx } : { sp: bld.state('legacy:id_' + id, {}), approx: false };
        cache.set(ck, hit);
      }
      if (hit.approx && hit.sp) info.approx++;
      var sp = hit.sp;
      var x = i % w, z = Math.floor(i / w) % l, y = Math.floor(i / (w * l));
      bld.put(x, y, z, sp);
    }
    return bld.done();
  }

  /* ---------------- detection ---------------- */
  function isAxiom(bytes) { return bytes.length >= 4 && bytes[0] === 0x0a && bytes[1] === 0xe5 && bytes[2] === 0xbb && bytes[3] === 0x36; }
  function detect(nbt) {
    if (nbt.Regions && typeof nbt.Regions === 'object') return 'litematic';
    var s = nbt.Schematic && typeof nbt.Schematic === 'object' && !Array.isArray(nbt.Schematic) ? nbt.Schematic : nbt;
    if ((s.Palette && s.BlockData) || (s.Blocks && s.Blocks.Palette)) return 'schem';
    if (nbt.Blocks instanceof Int8Array) return 'schematic';
    if (nbt.blocks && (nbt.palette || nbt.palettes)) return 'nbt';
    return null;
  }
  function readNbtModel(nbt, opts) {
    var f = detect(nbt);
    if (f === 'litematic') return litematic(nbt, opts);
    if (f === 'schem') return sponge(nbt, opts);
    if (f === 'schematic') return mcedit(nbt, opts);
    if (f === 'nbt') return C.structureToModel(nbt, opts);
    throw new Error('unknown_format');
  }
  /* bytes of any supported file -> model; gunzip optional (node). Friendly error codes: axiom_bp, not_nbt, unknown_format ... */
  async function readFile(name, bytes, opts, gunzip) {
    if (isAxiom(bytes)) throw new Error('axiom_bp');
    var raw = await C.ungzip(bytes, gunzip), nbt;
    try { nbt = C.readNbt(raw); } catch (e) { throw new Error(/^nbt_/.test(e.message) ? 'not_nbt' : e.message); }
    var m = readNbtModel(nbt, opts);
    m.info.file = name;
    return m;
  }

  /* ---------------- writers (way back: code -> file) ---------------- */
  function stateOf(specText) { var r = C.parseState(specText); return r; }
  function stateString(specText) { var r = stateOf(specText), k = Object.keys(r.props); return r.id + (k.length ? '[' + k.map(function (n) { return n + '=' + r.props[n]; }).join(',') + ']' : ''); }
  function dataVersion() { return D ? D.mc.dataVersion : 0; }
  /* vanilla structure file (StructureTemplate): unnamed properties keep the block default when loaded */
  function toStructureNbt(model, dv) {
    var idx = new Map(), palette = [], blocks = [];
    C.sortedKeys(model.cells).forEach(function (k) {
      var s = model.cells.get(k);
      if (!idx.has(s)) { var r = stateOf(s), e = { Name: r.id }; if (Object.keys(r.props).length) e.Properties = r.props; idx.set(s, palette.length); palette.push(e); }
      blocks.push({ pos: [C.kx(k), C.ky(k), C.kz(k)], state: idx.get(s) });
    });
    return { DataVersion: dv || dataVersion(), size: [model.sx, model.sy, model.sz], palette: palette, blocks: blocks, entities: [] };
  }
  /* Sponge schematic v3 (root compound with "Schematic") */
  function toSpongeNbt(model, dv) {
    var pal = { 'minecraft:air': 0 }, n = 1, w = model.sx, h = model.sy, l = model.sz, bytes = [];
    var ids = new Map();
    model.cells.forEach(function (s) { if (!ids.has(s)) { var st = stateString(s); if (pal[st] == null) pal[st] = n++; ids.set(s, pal[st]); } });
    for (var y = 0; y < h; y++) for (var z = 0; z < l; z++) for (var x = 0; x < w; x++) {
      var s = model.cells.get(C.key(x, y, z)), v = s === undefined ? 0 : ids.get(s);
      while (v >= 128) { bytes.push((v & 127) | 128); v >>>= 7; }
      bytes.push(v);
    }
    return { Schematic: { Version: 3, DataVersion: dv || dataVersion(), Width: C.tag(2, w), Height: C.tag(2, h), Length: C.tag(2, l),
      Offset: new Int32Array([0, 0, 0]), Blocks: { Palette: pal, Data: Int8Array.from(bytes), BlockEntities: [] } } };
  }
  async function writeFile(model, kind, gz) {
    var nbt = kind === 'schem' ? toSpongeNbt(model) : toStructureNbt(model);
    return C.gzip(C.writeNbt(nbt, ''), gz);
  }

  var F = { sponge: sponge, litematic: litematic, litRegions: litRegions, mcedit: mcedit, legacyState: legacyState, detect: detect, isAxiom: isAxiom,
    readNbtModel: readNbtModel, readFile: readFile, toStructureNbt: toStructureNbt, toSpongeNbt: toSpongeNbt, writeFile: writeFile };
  Object.keys(F).forEach(function (k) { C[k] = F[k]; });
  if (typeof module !== 'undefined' && module.exports) module.exports = C;
})(typeof window !== 'undefined' ? window : globalThis);
