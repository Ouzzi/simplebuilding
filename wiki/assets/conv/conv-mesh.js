/* conv-mesh.js (N51, b-conv51): 3D models beyond cubes and the way back. Pure JS, no network.
 * - .obj (+ optional .mtl), .gltf (embedded buffers or the .bin selected alongside), .glb -> voxel approximation
 *   (triangle/box overlap, optional interior fill, greedy cuboids, <= 256 elements) -> vanilla model JSON.
 * - model code (vanilla JSON) -> Blockbench .bbmodel; embedded .bbmodel textures -> PNG.
 * - tiny PNG (stored deflate) and ZIP (stored) writers for texture downloads and the resource pack. */
(function (root) {
  'use strict';
  var C = root.Conv51 || require('./conv-core.js');
  var NS = 'mymodels', MAX_NAME = 20;

  /* ---------------- CRC32, PNG, ZIP ---------------- */
  var CRC = (function () { var t = new Uint32Array(256); for (var n = 0; n < 256; n++) { var c = n; for (var k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; t[n] = c >>> 0; } return t; })();
  function crc32(bytes, start, end) { var c = 0xffffffff; for (var i = start || 0, e = end == null ? bytes.length : end; i < e; i++) c = CRC[(c ^ bytes[i]) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; }
  function concat(chunks) { var n = 0; chunks.forEach(function (c) { n += c.length; }); var o = new Uint8Array(n), p = 0; chunks.forEach(function (c) { o.set(c, p); p += c.length; }); return o; }
  function be32(v) { return [(v >>> 24) & 255, (v >>> 16) & 255, (v >>> 8) & 255, v & 255]; }
  function le16(v) { return [v & 255, (v >>> 8) & 255]; }
  function le32(v) { return [v & 255, (v >>> 8) & 255, (v >>> 16) & 255, (v >>> 24) & 255]; }
  function ascii(s) { return Array.prototype.map.call(s, function (c) { return c.charCodeAt(0); }); }
  /* RGBA pixels (w*h*4) -> PNG bytes; zlib with stored blocks (no compression, tiny textures) */
  function png(w, h, rgba) {
    var rawLen = h * (w * 4 + 1), raw = new Uint8Array(rawLen), p = 0;
    for (var y = 0; y < h; y++) { raw[p++] = 0; raw.set(rgba.subarray(y * w * 4, (y + 1) * w * 4), p); p += w * 4; }
    var z = [0x78, 0x01], a = 1, b = 0;
    for (var i = 0; i < raw.length; i++) { a = (a + raw[i]) % 65521; b = (b + a) % 65521; }
    for (var off = 0; off < raw.length || off === 0; off += 65535) {
      var len = Math.min(65535, raw.length - off), last = off + len >= raw.length ? 1 : 0;
      z.push(last, len & 255, len >> 8, ~len & 255, (~len >> 8) & 255);
      for (var j = 0; j < len; j++) z.push(raw[off + j]);
      if (!raw.length) break;
    }
    z = z.concat(be32(((b << 16) | a) >>> 0));
    function chunk(type, data) { var td = new Uint8Array(ascii(type).concat(Array.from(data))); return concat([new Uint8Array(be32(data.length)), td, new Uint8Array(be32(crc32(td)))]); }
    var ihdr = new Uint8Array(be32(w).concat(be32(h), [8, 6, 0, 0, 0]));
    return concat([new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', new Uint8Array(z)), chunk('IEND', new Uint8Array(0))]);
  }
  function solidPng(rgb, size) {
    size = size || 16; var px = new Uint8Array(size * size * 4);
    for (var i = 0; i < size * size; i++) { px[i * 4] = rgb[0]; px[i * 4 + 1] = rgb[1]; px[i * 4 + 2] = rgb[2]; px[i * 4 + 3] = rgb[3] == null ? 255 : rgb[3]; }
    return png(size, size, px);
  }
  /* files: [{name, data: Uint8Array|string}] -> ZIP (stored, fixed date 1980-01-01) */
  function zip(files) {
    var enc = new TextEncoder(), locals = [], central = [], off = 0;
    files.forEach(function (f) {
      var name = enc.encode(f.name), data = typeof f.data === 'string' ? enc.encode(f.data) : f.data, crc = crc32(data);
      var head = [].concat([0x50, 0x4b, 3, 4], le16(20), le16(0x0800), le16(0), le16(0), le16(33), le32(crc), le32(data.length), le32(data.length), le16(name.length), le16(0));
      locals.push(new Uint8Array(head), name, data);
      central.push(new Uint8Array([].concat([0x50, 0x4b, 1, 2], le16(20), le16(20), le16(0x0800), le16(0), le16(0), le16(33), le32(crc), le32(data.length), le32(data.length),
        le16(name.length), le16(0), le16(0), le16(0), le16(0), le32(0), le32(off))), name);
      off += head.length + name.length + data.length;
    });
    var cd = concat(central);
    return concat(locals.concat([cd, new Uint8Array([].concat([0x50, 0x4b, 5, 6], le16(0), le16(0), le16(files.length), le16(files.length), le32(cd.length), le32(off), le16(0)))]));
  }
  function safeName(raw) {
    var n = String(raw || 'model').replace(/\.[a-z0-9]+$/i, '').trim().toLowerCase().replace(/[\s-]+/g, '_').replace(/[^a-z0-9_]/g, '').slice(0, MAX_NAME);
    return n || 'model';
  }
  /* resource pack like Simple Models' editor export (ModelExport.packFiles) + texture PNGs */
  function resourcePack(name, code, textures) {
    var n = safeName(name), D = C.DATA, fmt = D ? D.mc.packFormat : 97;
    var files = [{ name: 'pack.mcmeta', data: JSON.stringify({ pack: { description: 'Simple Models converter (wiki)', min_format: fmt, max_format: fmt } }, null, 2) },
      { name: 'assets/' + NS + '/models/item/' + n + '.json', data: code },
      { name: 'assets/' + NS + '/items/' + n + '.json', data: JSON.stringify({ model: { type: 'minecraft:model', model: NS + ':item/' + n } }, null, 2) }];
    (textures || []).forEach(function (t) { files.push({ name: 'assets/' + NS + '/textures/block/' + t.name + '.png', data: t.data }); });
    return zip(files);
  }

  /* ---------------- mesh readers -> {tris: Float64Array-like [ax,ay,az,bx,...] per tri, mats: [], triMat: []} ---------------- */
  function mesh() { return { pos: [], triMat: [], mats: [], warn: [] }; }
  function addTri(m, a, b, c, mat) { m.pos.push(a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2]); m.triMat.push(mat); }
  function parseMtl(text) {
    var mats = {}, cur = null;
    text.split(/\r?\n/).forEach(function (l) {
      var t = l.trim().split(/\s+/);
      if (t[0] === 'newmtl') { cur = t.slice(1).join(' '); mats[cur] = { name: cur, color: [200, 200, 200, 255] }; }
      else if (t[0] === 'Kd' && cur) mats[cur].color = [0, 1, 2].map(function (i) { return Math.round(Math.max(0, Math.min(1, +t[i + 1])) * 255); }).concat([255]);
      else if (t[0] === 'map_Kd' && cur) mats[cur].texture = true;
    });
    return mats;
  }
  function parseObj(text, mtlText) {
    var m = mesh(), v = [], cur = 0, mtl = mtlText ? parseMtl(mtlText) : {}, index = {};
    function mat(name) {
      if (index[name] == null) { index[name] = m.mats.length; var d = mtl[name]; m.mats.push({ name: name, color: d ? d.color : null }); if (d && d.texture) m.warn.push('textures_ignored'); }
      return index[name];
    }
    mat('default');
    text.split(/\r?\n/).forEach(function (l) {
      var t = l.trim().split(/\s+/);
      if (t[0] === 'v') v.push([+t[1], +t[2], +t[3]]);
      else if (t[0] === 'usemtl') cur = mat(t.slice(1).join(' ') || 'default');
      else if (t[0] === 'f') {
        var ids = t.slice(1).map(function (s) { var i = parseInt(s.split('/')[0], 10); return i < 0 ? v.length + i : i - 1; });
        if (ids.some(function (i) { return !(i >= 0 && i < v.length); })) throw new Error('obj_bad_face');
        for (var k = 1; k + 1 < ids.length; k++) addTri(m, v[ids[0]], v[ids[k]], v[ids[k + 1]], cur);
      }
    });
    if (!m.triMat.length) throw new Error('mesh_empty');
    return m;
  }
  function b64bytes(s) { var b = typeof atob === 'function' ? atob(s) : Buffer.from(s, 'base64').toString('binary'), u = new Uint8Array(b.length); for (var i = 0; i < b.length; i++) u[i] = b.charCodeAt(i); return u; }
  /* files: map lower-case name -> Uint8Array (for external .bin) */
  function parseGltf(json, bin, files) {
    var g = typeof json === 'string' ? JSON.parse(json) : json, m = mesh();
    var req = (g.extensionsRequired || []).filter(function (e) { return /draco|meshopt|quantization/i.test(e); });
    if (req.length) throw new Error('gltf_compressed');
    var buffers = (g.buffers || []).map(function (b, i) {
      if (!b.uri) { if (bin) return bin; throw new Error('gltf_no_buffer'); }
      var mm = /^data:[^,]*;base64,(.*)$/.exec(b.uri);
      if (mm) return b64bytes(mm[1]);
      var f = files && files[decodeURIComponent(b.uri).split('/').pop().toLowerCase()];
      if (!f) throw new Error('gltf_missing_bin:' + b.uri);
      return f;
    });
    function accessor(i) {
      var a = g.accessors[i], bv = g.bufferViews[a.bufferView], buf = buffers[bv.buffer];
      if (a.sparse) m.warn.push('sparse_ignored');
      var comps = { SCALAR: 1, VEC2: 2, VEC3: 3, VEC4: 4 }[a.type], size = { 5120: 1, 5121: 1, 5122: 2, 5123: 2, 5125: 4, 5126: 4 }[a.componentType];
      var stride = bv.byteStride || comps * size, base = (bv.byteOffset || 0) + (a.byteOffset || 0), dv = new DataView(buf.buffer, buf.byteOffset, buf.byteLength), out = [];
      for (var k = 0; k < a.count; k++) {
        var e = [];
        for (var c = 0; c < comps; c++) {
          var p = base + k * stride + c * size;
          switch (a.componentType) {
            case 5126: e.push(dv.getFloat32(p, true)); break; case 5125: e.push(dv.getUint32(p, true)); break;
            case 5123: e.push(dv.getUint16(p, true)); break; case 5122: e.push(dv.getInt16(p, true)); break;
            case 5121: e.push(dv.getUint8(p)); break; default: e.push(dv.getInt8(p));
          }
        }
        out.push(comps === 1 ? e[0] : e);
      }
      return out;
    }
    var mats = (g.materials || []).map(function (mt, i) {
      var pbr = mt.pbrMetallicRoughness || {}, f = pbr.baseColorFactor || [1, 1, 1, 1];
      if (pbr.baseColorTexture) m.warn.push('textures_ignored');
      return { name: mt.name || 'material_' + i, color: f.slice(0, 3).map(function (x) { return Math.round(Math.pow(Math.max(0, Math.min(1, x)), 1 / 2.2) * 255); }).concat([255]) };
    });
    m.mats = mats.concat([{ name: 'default', color: null }]);
    var defMat = mats.length;
    function mul(a, b) { var o = new Array(16); for (var r = 0; r < 4; r++) for (var c = 0; c < 4; c++) { var s = 0; for (var k = 0; k < 4; k++) s += a[k * 4 + r] * b[c * 4 + k]; o[c * 4 + r] = s; } return o; }
    function trs(n) {
      if (n.matrix) return n.matrix.slice();
      var t = n.translation || [0, 0, 0], q = n.rotation || [0, 0, 0, 1], s = n.scale || [1, 1, 1], x = q[0], y = q[1], z = q[2], w = q[3];
      return [(1 - 2 * (y * y + z * z)) * s[0], 2 * (x * y + z * w) * s[0], 2 * (x * z - y * w) * s[0], 0,
        2 * (x * y - z * w) * s[1], (1 - 2 * (x * x + z * z)) * s[1], 2 * (y * z + x * w) * s[1], 0,
        2 * (x * z + y * w) * s[2], 2 * (y * z - x * w) * s[2], (1 - 2 * (x * x + y * y)) * s[2], 0, t[0], t[1], t[2], 1];
    }
    function tf(M, p) { return [M[0] * p[0] + M[4] * p[1] + M[8] * p[2] + M[12], M[1] * p[0] + M[5] * p[1] + M[9] * p[2] + M[13], M[2] * p[0] + M[6] * p[1] + M[10] * p[2] + M[14]]; }
    function visit(ni, parent, depth) {
      if (depth > 64) return;
      var n = g.nodes[ni], M = mul(parent, trs(n));
      if (n.mesh != null) g.meshes[n.mesh].primitives.forEach(function (pr) {
        var mode = pr.mode == null ? 4 : pr.mode;
        if (mode < 4) { m.warn.push('non_triangles'); return; }
        if (pr.attributes.POSITION == null) return;
        var P = accessor(pr.attributes.POSITION).map(function (p) { return tf(M, p); }), I = pr.indices != null ? accessor(pr.indices) : P.map(function (_, i) { return i; });
        var mat = pr.material != null ? pr.material : defMat;
        if (mode === 4) for (var k = 0; k + 2 < I.length; k += 3) addTri(m, P[I[k]], P[I[k + 1]], P[I[k + 2]], mat);
        else if (mode === 5) for (k = 0; k + 2 < I.length; k++) addTri(m, P[I[k]], P[I[k + 1 + (k & 1)]], P[I[k + 2 - (k & 1)]], mat);
        else for (k = 1; k + 1 < I.length; k++) addTri(m, P[I[0]], P[I[k]], P[I[k + 1]], mat);
      });
      (n.children || []).forEach(function (c) { visit(c, M, depth + 1); });
    }
    var I4 = [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1], sc = g.scenes && g.scenes[g.scene || 0];
    var rootsN = sc ? sc.nodes || [] : (g.nodes || []).map(function (_, i) { return i; });
    rootsN.forEach(function (r) { visit(r, I4, 0); });
    if (!m.triMat.length) throw new Error('mesh_empty');
    return m;
  }
  function parseGlb(bytes, files) {
    var dv = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
    if (bytes.length < 20 || dv.getUint32(0, true) !== 0x46546c67) throw new Error('glb_bad');
    var p = 12, json = null, bin = null;
    while (p + 8 <= bytes.length) {
      var len = dv.getUint32(p, true), type = dv.getUint32(p + 4, true), data = bytes.subarray(p + 8, p + 8 + len);
      if (type === 0x4e4f534a) json = new TextDecoder().decode(data); else if (type === 0x004e4942) bin = data;
      p += 8 + len;
    }
    if (!json) throw new Error('glb_bad');
    return parseGltf(json, bin, files);
  }

  /* ---------------- voxel approximation ---------------- */
  /* triangle / axis-aligned box overlap (separating axis test, Akenine-Moeller), box = center c, half size h */
  function triBox(c, h, t) {
    var v = [[t[0] - c[0], t[1] - c[1], t[2] - c[2]], [t[3] - c[0], t[4] - c[1], t[5] - c[2]], [t[6] - c[0], t[7] - c[1], t[8] - c[2]]];
    var e = [sub(v[1], v[0]), sub(v[2], v[1]), sub(v[0], v[2])];
    for (var i = 0; i < 3; i++) for (var j = 0; j < 3; j++) {
      var a = cross(unit(i), e[j]), p0 = dot(a, v[0]), p1 = dot(a, v[1]), p2 = dot(a, v[2]);
      var r = h[0] * Math.abs(a[0]) + h[1] * Math.abs(a[1]) + h[2] * Math.abs(a[2]);
      if (Math.min(p0, p1, p2) > r || Math.max(p0, p1, p2) < -r) return false;
    }
    for (i = 0; i < 3; i++) if (Math.min(v[0][i], v[1][i], v[2][i]) > h[i] || Math.max(v[0][i], v[1][i], v[2][i]) < -h[i]) return false;
    var n = cross(e[0], e[1]), d = dot(n, v[0]), rr = h[0] * Math.abs(n[0]) + h[1] * Math.abs(n[1]) + h[2] * Math.abs(n[2]);
    return Math.abs(d) <= rr;
  }
  function sub(a, b) { return [a[0] - b[0], a[1] - b[1], a[2] - b[2]]; }
  function cross(a, b) { return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]; }
  function dot(a, b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }
  function unit(i) { return [i === 0 ? 1 : 0, i === 1 ? 1 : 0, i === 2 ? 1 : 0]; }
  function hueColor(name) { var h = 0; for (var i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) >>> 0; var a = (h % 360) / 60, x = Math.round(255 * (1 - Math.abs(a % 2 - 1))), k = Math.floor(a); return [[220, x, 60], [x, 220, 60], [60, 220, x], [60, x, 220], [x, 60, 220], [220, 60, x]][k % 6].concat([255]); }
  /* voxel grid of the mesh: res cells along the longest axis */
  function voxelize(m, res, fill) {
    var P = m.pos, mn = [Infinity, Infinity, Infinity], mx = [-Infinity, -Infinity, -Infinity];
    for (var i = 0; i < P.length; i += 3) for (var a = 0; a < 3; a++) { if (P[i + a] < mn[a]) mn[a] = P[i + a]; if (P[i + a] > mx[a]) mx[a] = P[i + a]; }
    var ext = Math.max(mx[0] - mn[0], mx[1] - mn[1], mx[2] - mn[2]);
    if (!(ext > 0) || !isFinite(ext)) throw new Error('mesh_empty');
    var vs = ext / res, n = [0, 1, 2].map(function (k) { return Math.max(1, Math.ceil((mx[k] - mn[k]) / vs - 1e-9)); });
    var grid = new Int32Array(n[0] * n[1] * n[2]).fill(-1), h = [vs / 2 * 1.0001, vs / 2 * 1.0001, vs / 2 * 1.0001];
    function idx(x, y, z) { return x + n[0] * (z + n[2] * y); }
    for (var t = 0; t < m.triMat.length; t++) {
      var tri = P.slice(t * 9, t * 9 + 9), lo = [], hi = [];
      for (a = 0; a < 3; a++) {
        var tmin = Math.min(tri[a], tri[a + 3], tri[a + 6]), tmax = Math.max(tri[a], tri[a + 3], tri[a + 6]);
        lo.push(Math.max(0, Math.floor((tmin - mn[a]) / vs))); hi.push(Math.min(n[a] - 1, Math.floor((tmax - mn[a]) / vs)));
      }
      for (var y = lo[1]; y <= hi[1]; y++) for (var z = lo[2]; z <= hi[2]; z++) for (var x = lo[0]; x <= hi[0]; x++) {
        var k = idx(x, y, z); if (grid[k] >= 0) continue;
        if (triBox([mn[0] + (x + 0.5) * vs, mn[1] + (y + 0.5) * vs, mn[2] + (z + 0.5) * vs], h, tri)) grid[k] = m.triMat[t];
      }
    }
    if (fill) { // flood the outside from the border; unreached empty cells are inside and take the material of the cell below/beside
      var out = new Uint8Array(grid.length), stack = [];
      for (y = 0; y < n[1]; y++) for (z = 0; z < n[2]; z++) for (x = 0; x < n[0]; x++)
        if ((x === 0 || y === 0 || z === 0 || x === n[0] - 1 || y === n[1] - 1 || z === n[2] - 1) && grid[idx(x, y, z)] < 0) { out[idx(x, y, z)] = 1; stack.push(x, y, z); }
      while (stack.length) {
        z = stack.pop(); y = stack.pop(); x = stack.pop();
        [[1, 0, 0], [-1, 0, 0], [0, 1, 0], [0, -1, 0], [0, 0, 1], [0, 0, -1]].forEach(function (d) {
          var X = x + d[0], Y = y + d[1], Z = z + d[2];
          if (X < 0 || Y < 0 || Z < 0 || X >= n[0] || Y >= n[1] || Z >= n[2]) return;
          var q = idx(X, Y, Z); if (out[q] || grid[q] >= 0) return; out[q] = 1; stack.push(X, Y, Z);
        });
      }
      for (y = 0; y < n[1]; y++) for (z = 0; z < n[2]; z++) { var last = -1; for (x = 0; x < n[0]; x++) { k = idx(x, y, z); if (grid[k] >= 0) last = grid[k]; else if (!out[k] && last >= 0) grid[k] = last; } }
    }
    return { grid: grid, n: n, vs: vs, mn: mn, mx: mx };
  }
  /* mesh -> vanilla model; opts: res (cells on the longest axis, default 16), size (model units of the longest axis,
   * default 16 = one block, max 48), fill (default true), name. Lowers res until <= 256 elements. */
  function meshToModel(m, opts) {
    opts = opts || {};
    var size = Math.min(48, Math.max(1, opts.size || 16)), res = Math.max(1, Math.min(64, opts.res || 16)), warn = m.warn.slice(), name = safeName(opts.name);
    var tries = 0, els, vox, used;
    for (;;) {
      vox = voxelize(m, res, opts.fill !== false);
      var byMat = new Map(), n = vox.n;
      for (var y = 0; y < n[1]; y++) for (var z = 0; z < n[2]; z++) for (var x = 0; x < n[0]; x++) {
        var g = vox.grid[x + n[0] * (z + n[2] * y)]; if (g < 0) continue;
        if (!byMat.has(g)) byMat.set(g, []); byMat.get(g).push(C.key(x, y, z));
      }
      els = []; used = [];
      byMat.forEach(function (keys, mat) { used.push(mat); C.boxes(keys).forEach(function (b) { els.push([b, mat]); }); });
      if (els.length <= 256 || res <= 2 || ++tries > 12) break;
      res = Math.max(2, Math.floor(res * 0.8)); warn.push('reduced');
    }
    if (els.length > 256) throw new Error('too_many_elements');
    var u = size / res, ext = vox.n.map(function (c) { return c * u; }), off = [8 - ext[0] / 2, 0, 8 - ext[2] / 2];
    if (size > 16) off[1] = Math.max(-16, 8 - ext[1] / 2);
    var tex = {}, varOf = {};
    used.sort(function (a, b) { return a - b; }).forEach(function (mat, i) {
      var mt = m.mats[mat] || { name: 'm' + i }, v = safeName(mt.name).replace(/^(\d)/, 'm$1') || 'm' + i, base = v, c = 2;
      while (tex[v]) v = base + '_' + c++;
      tex[v] = NS + ':block/' + name + '_' + v; varOf[mat] = v;
    });
    var first = Object.keys(tex)[0];
    var textures = Object.assign({ particle: '#' + first }, tex);
    var elements = els.map(function (e) {
      var b = e[0], r = function (v) { return Math.round(v * 1000) / 1000; };
      var faces = {}; ['down', 'up', 'north', 'south', 'west', 'east'].forEach(function (d) { faces[d] = { uv: [0, 0, 16, 16], texture: '#' + varOf[e[1]] }; });
      return { from: [r(off[0] + b[0] * u), r(off[1] + b[1] * u), r(off[2] + b[2] * u)], to: [r(off[0] + (b[3] + 1) * u), r(off[1] + (b[4] + 1) * u), r(off[2] + (b[5] + 1) * u)], faces: faces };
    });
    var pngs = used.map(function (mat) { var mt = m.mats[mat] || {}; return { name: name + '_' + varOf[mat], data: solidPng(mt.color || hueColor(mt.name || String(mat))), color: mt.color || hueColor(mt.name || String(mat)) }; });
    return { model: { parent: 'minecraft:block/block', textures: textures, elements: elements }, textures: pngs, res: res, warn: warn.filter(function (w, i, a) { return a.indexOf(w) === i; }) };
  }

  /* ---------------- model code -> .bbmodel ---------------- */
  function uuid(i) { var h = ('000000000000' + (i + 1).toString(16)).slice(-12); return '00000000-0000-4000-8000-' + h; }
  function modelToBbmodel(src, name) {
    var tvars = Object.keys(src.textures || {}).filter(function (k) { return String(src.textures[k])[0] !== '#'; });
    function resolve(ref, depth) { // "#var" -> texture variable that holds a resource id
      var v = String(ref || '').replace(/^#/, '');
      if (depth > 8) return null;
      var t = (src.textures || {})[v];
      if (t == null) return null;
      return String(t)[0] === '#' ? resolve(t, depth + 1) : v;
    }
    var textures = tvars.map(function (v, i) { return { name: v, id: String(i), path: '', folder: 'block', namespace: NS, uuid: uuid(1000 + i), relative_path: '', particle: false, render_mode: 'default', visible: true, saved: true, uv_width: 16, uv_height: 16, source: '', resource_id: String(src.textures[v]) }; });
    var pv = (src.textures || {}).particle && resolve(src.textures.particle, 0);
    if (pv != null && tvars.indexOf(pv) >= 0) textures[tvars.indexOf(pv)].particle = true;
    var elements = (src.elements || []).map(function (e, i) {
      var rot = [0, 0, 0], origin = [8, 8, 8];
      if (e.rotation) {
        origin = e.rotation.origin || origin;
        if (e.rotation.axis) rot['xyz'.indexOf(e.rotation.axis)] = e.rotation.angle || 0; else rot = [e.rotation.x || 0, e.rotation.y || 0, e.rotation.z || 0];
      }
      var faces = {};
      ['north', 'east', 'south', 'west', 'up', 'down'].forEach(function (d) {
        var f = (e.faces || {})[d];
        if (!f) { faces[d] = { uv: [0, 0, 0, 0], texture: null }; return; }
        var tv = resolve(f.texture, 0), o = { uv: f.uv ? f.uv.slice() : autoUv(e, d), texture: tv == null ? null : tvars.indexOf(tv) };
        if (f.rotation) o.rotation = f.rotation; if (f.cullface) o.cullface = f.cullface; if (f.tintindex != null) o.tint = f.tintindex;
        faces[d] = o;
      });
      var el = { name: e.name || 'cube', box_uv: false, rescale: !!(e.rotation && e.rotation.rescale), locked: false, render_order: 'default', allow_mirror_modeling: true,
        from: e.from.slice(), to: e.to.slice(), autouv: 0, color: i % 8, origin: origin.slice(), faces: faces, type: 'cube', uuid: uuid(i) };
      if (rot.some(Boolean)) el.rotation = rot;
      if (e.light_emission) el.light_emission = e.light_emission;
      return el;
    });
    var bb = { meta: { format_version: '4.10', model_format: 'java_block', box_uv: false }, name: safeName(name), parent: src.parent || '', ambientocclusion: src.ambientocclusion !== false,
      front_gui_light: src.gui_light === 'front', visible_box: [1, 1, 0], resolution: { width: 16, height: 16 }, elements: elements,
      outliner: elements.map(function (e) { return e.uuid; }), textures: textures };
    if (src.display) bb.display = JSON.parse(JSON.stringify(src.display));
    return bb;
  }
  function autoUv(e, d) { // vanilla's uv from the element position when a face has none
    var f = e.from, t = e.to;
    switch (d) {
      case 'down': return [f[0], 16 - t[2], t[0], 16 - f[2]]; case 'up': return [f[0], f[2], t[0], t[2]];
      case 'north': return [16 - t[0], 16 - t[1], 16 - f[0], 16 - f[1]]; case 'south': return [f[0], 16 - t[1], t[0], 16 - f[1]];
      case 'west': return [f[2], 16 - t[1], t[2], 16 - f[1]]; default: return [16 - t[2], 16 - t[1], 16 - f[2], 16 - f[1]];
    }
  }
  /* embedded PNGs of a .bbmodel: [{name (texture variable as in bbmodelToVanilla), data}] */
  function bbTextures(bb) {
    var seen = {}, out = [];
    (bb.textures || []).forEach(function (t, i) {
      var n = String(t.name || t.id || ('t' + i)).replace(/\.png$/i, '').toLowerCase().replace(/[^a-z0-9_]/g, '_').replace(/^_+|_+$/g, '') || ('t' + i);
      var v = n, c = 2; while (seen[v]) v = n + '_' + c++; seen[v] = true;
      var mm = /^data:image\/png;base64,(.*)$/.exec(t.source || '');
      if (mm) out.push({ name: n, variable: v, data: b64bytes(mm[1]) });
    });
    return out;
  }

  var M = { png: png, solidPng: solidPng, zip: zip, crc32: crc32, resourcePack: resourcePack, safeName: safeName, parseObj: parseObj, parseMtl: parseMtl,
    parseGltf: parseGltf, parseGlb: parseGlb, voxelize: voxelize, meshToModel: meshToModel, modelToBbmodel: modelToBbmodel, bbTextures: bbTextures, triBox: triBox };
  Object.keys(M).forEach(function (k) { C[k] = M[k]; });
  if (typeof module !== 'undefined' && module.exports) module.exports = C;
})(typeof window !== 'undefined' ? window : globalThis);
