/* Pixel editor core (no DOM): layer format, composition, PNG codec, drawing helpers, history.
 * Same layer format and integer composition as tools/textures/layers.py (docs/ai/PLAN-B-PIXELEDITOR-2026-10-21.md),
 * so a PNG + <name>.layers.json exported here passes `python tools/textures/layers.py check` unchanged.
 * Browser: window.SBPixelCore. Node (wiki/tests/pixeleditor_core.cjs): module.exports. */
(function (root, factory) {
  var api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.SBPixelCore = api;
})(typeof self !== 'undefined' ? self : this, function () {
  'use strict';
  var FORMAT = 'sb-layers', SUFFIX = '.layers.json';

  function rnd(x) { return Math.floor(x + 0.5); }
  function hex2(n) { return (n < 16 ? '0' : '') + n.toString(16); }
  function colorHex(r, g, b, a) { return '#' + hex2(r) + hex2(g) + hex2(b) + hex2(a); }
  function parseColor(s) {
    var v = /^#?([0-9a-fA-F]{6})([0-9a-fA-F]{2})?$/.exec(String(s || '').trim());
    if (!v) return null;
    var n = parseInt(v[1], 16);
    return [(n >> 16) & 255, (n >> 8) & 255, n & 255, v[2] ? parseInt(v[2], 16) : 255];
  }
  function digitsFor(count) { return count <= 256 ? 2 : count <= 4096 ? 3 : 4; }

  /* ---------------- model: {w, h, layers: [{name, visible, opacity, px: Uint8ClampedArray RGBA}]}, layers[0] = bottom */
  function newLayer(w, h, name) { return { name: name || 'Layer', visible: true, opacity: 100, alphaLock: false, px: new Uint8ClampedArray(w * h * 4) }; }
  function blank(w, h, name) { return { w: w, h: h, palette: [], layers: [newLayer(w, h, name || 'Base')] }; }

  function validate(doc) {
    if (!doc || doc.format !== FORMAT || doc.version !== 1) return ['format must be "' + FORMAT + '", version 1'];
    var w = doc.width, h = doc.height, d = doc.digits, pal = doc.palette, out = [];
    if (!(w > 0 && h > 0 && w % 1 === 0 && h % 1 === 0)) return ['width/height must be positive integers'];
    if ([2, 3, 4].indexOf(d) < 0) return ['digits must be 2, 3 or 4'];
    if (!Array.isArray(pal) || !pal.every(function (c) { return /^#[0-9a-f]{8}$/.test(c); })) return ['palette must be "#rrggbbaa"'];
    if (!Array.isArray(doc.layers) || !doc.layers.length) return ['layers must be a non-empty list'];
    doc.layers.forEach(function (l, n) {
      var tag = 'layer ' + n;
      if (typeof l.name !== 'string') out.push(tag + ': name missing');
      if (typeof l.visible !== 'boolean') out.push(tag + ': visible must be true/false');
      if (!(l.opacity >= 0 && l.opacity <= 100 && l.opacity % 1 === 0)) out.push(tag + ': opacity must be an integer 0-100');
      if ((l.blend || 'normal') !== 'normal') out.push(tag + ': only blend "normal"');
      if (!Array.isArray(l.rows) || l.rows.length !== h) { out.push(tag + ': needs ' + h + ' rows'); return; }
      for (var y = 0; y < h; y++) {
        var row = l.rows[y];
        if (typeof row !== 'string' || row.length !== w * d) { out.push(tag + ': row ' + y + ' must have ' + (w * d) + ' characters'); return; }
        for (var x = 0; x < w; x++) {
          var cell = row.substr(x * d, d);
          if (/^\.+$/.test(cell)) continue;
          if (!/^[0-9a-f]+$/.test(cell) || parseInt(cell, 16) >= pal.length) { out.push(tag + ': row ' + y + ' column ' + x + ' is no palette index'); return; }
        }
      }
    });
    return out;
  }

  function fromDoc(doc) {
    var problems = validate(doc);
    if (problems.length) throw new Error(problems.join('; '));
    var w = doc.width, h = doc.height, d = doc.digits;
    var pal = doc.palette.map(parseColor), empty = new Array(d + 1).join('.');
    return {
      w: w, h: h, palette: doc.palette.slice(),
      layers: doc.layers.map(function (l) {
        var layer = newLayer(w, h, l.name);
        layer.visible = l.visible; layer.opacity = l.opacity;
        for (var y = 0; y < h; y++) for (var x = 0; x < w; x++) {
          var cell = l.rows[y].substr(x * d, d);
          if (cell === empty) continue;
          layer.px.set(pal[parseInt(cell, 16)], (y * w + x) * 4);
        }
        return layer;
      })
    };
  }

  /* Palette: the colours of the loaded file first (stable diffs), new ones in order of appearance; unused ones dropped. */
  function toDoc(model) {
    var w = model.w, h = model.h, index = new Map(), used = new Set();
    model.layers.forEach(function (l) { for (var i = 0; i < w * h; i++) if (l.px[i * 4 + 3]) used.add(colorHex(l.px[i * 4], l.px[i * 4 + 1], l.px[i * 4 + 2], l.px[i * 4 + 3])); });
    (model.palette || []).forEach(function (c) { if (used.has(c) && !index.has(c)) index.set(c, index.size); });
    model.layers.forEach(function (l) { for (var i = 0; i < w * h; i++) if (l.px[i * 4 + 3]) { var c = colorHex(l.px[i * 4], l.px[i * 4 + 1], l.px[i * 4 + 2], l.px[i * 4 + 3]); if (!index.has(c)) index.set(c, index.size); } });
    var d = digitsFor(index.size), empty = new Array(d + 1).join('.');
    function cell(n) { var s = n.toString(16); while (s.length < d) s = '0' + s; return s; }
    return {
      format: FORMAT, version: 1, width: w, height: h, digits: d, palette: Array.from(index.keys()),
      layers: model.layers.map(function (l) {
        var rows = [];
        for (var y = 0; y < h; y++) {
          var r = '';
          for (var x = 0; x < w; x++) {
            var o = (y * w + x) * 4;
            r += l.px[o + 3] ? cell(index.get(colorHex(l.px[o], l.px[o + 1], l.px[o + 2], l.px[o + 3]))) : empty;
          }
          rows.push(r);
        }
        return { name: l.name, visible: l.visible, opacity: l.opacity, blend: 'normal', rows: rows };
      })
    };
  }

  /* Same text as layers.py dumps(): one header field per line, one row per line. */
  function dumps(doc) {
    var j = JSON.stringify, lines = ['{'];
    ['format', 'version', 'width', 'height', 'digits'].forEach(function (k) { lines.push('  ' + j(k) + ': ' + j(doc[k]) + ','); });
    lines.push('  "palette": [' + doc.palette.map(function (c) { return j(c); }).join(', ') + '],');
    lines.push('  "layers": [');
    doc.layers.forEach(function (l, n) {
      var meta = ['name', 'visible', 'opacity', 'blend'].map(function (k) { return j(k) + ': ' + j(l[k]); }).join(', ');
      lines.push('    {' + meta + ', "rows": [\n' + l.rows.map(function (r) { return '      ' + j(r); }).join(',\n') + '\n    ]}' + (n < doc.layers.length - 1 ? ',' : ''));
    });
    lines.push('  ]', '}');
    return lines.join('\n') + '\n';
  }

  function fromRgba(w, h, rgba, name) {
    var m = blank(w, h, name || 'Base');
    var px = m.layers[0].px;
    for (var i = 0; i < w * h * 4; i += 4) if (rgba[i + 3]) { px[i] = rgba[i]; px[i + 1] = rgba[i + 1]; px[i + 2] = rgba[i + 2]; px[i + 3] = rgba[i + 3]; }
    m.palette = toDoc(m).palette;
    return m;
  }

  /* Visible layers bottom first, source-over, integer rounding exactly as layers.py compose(). */
  function compose(model, only) {
    var w = model.w, h = model.h, out = new Uint8ClampedArray(w * h * 4);
    model.layers.forEach(function (l, n) {
      if (only != null && only !== n) return;
      if (!l.visible || l.opacity <= 0) return;
      var op = l.opacity, px = l.px;
      for (var i = 0; i < w * h * 4; i += 4) {
        var a = px[i + 3];
        if (!a) continue;
        var sa = rnd(a * op / 100);
        if (sa <= 0) continue;
        if (sa >= 255) { out[i] = px[i]; out[i + 1] = px[i + 1]; out[i + 2] = px[i + 2]; out[i + 3] = 255; continue; }
        var rest = out[i + 3] * (255 - sa) / 255, oa = sa + rest;
        for (var k = 0; k < 3; k++) out[i + k] = rnd((px[i + k] * sa + out[i + k] * rest) / oa);
        out[i + 3] = rnd(oa);
      }
    });
    return out;
  }

  function cloneModel(m) {
    return { w: m.w, h: m.h, palette: (m.palette || []).slice(), layers: m.layers.map(function (l) {
      return { name: l.name, visible: l.visible, opacity: l.opacity, alphaLock: !!l.alphaLock, px: new Uint8ClampedArray(l.px) };
    }) };
  }

  /* ---------------- PNG codec (exact: a canvas would premultiply alpha and change half-transparent pixels) */
  var CRC = (function () { var t = new Uint32Array(256); for (var n = 0; n < 256; n++) { var c = n; for (var k = 0; k < 8; k++) c = c & 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1; t[n] = c >>> 0; } return t; })();
  function crc32(bytes) { var c = 0xFFFFFFFF; for (var i = 0; i < bytes.length; i++) c = CRC[(c ^ bytes[i]) & 255] ^ (c >>> 8); return (c ^ 0xFFFFFFFF) >>> 0; }
  function adler32(bytes) { var a = 1, b = 0; for (var i = 0; i < bytes.length; i++) { a = (a + bytes[i]) % 65521; b = (b + a) % 65521; } return ((b << 16) | a) >>> 0; }
  function streamThrough(kind, Ctor, bytes) {
    var s = new Blob([bytes]).stream().pipeThrough(new Ctor(kind));
    return new Response(s).arrayBuffer().then(function (b) { return new Uint8Array(b); });
  }
  function inflate(bytes) {
    if (typeof DecompressionStream === 'undefined') return Promise.reject(new Error('DecompressionStream unavailable'));
    return streamThrough('deflate', DecompressionStream, bytes);
  }
  /* zlib stream of stored blocks: valid everywhere, used without CompressionStream */
  function deflateStored(bytes) {
    var blocks = Math.max(1, Math.ceil(bytes.length / 65535)), out = new Uint8Array(2 + bytes.length + blocks * 5 + 4), p = 0;
    out[p++] = 0x78; out[p++] = 0x01;
    for (var b = 0; b < blocks; b++) {
      var part = bytes.subarray(b * 65535, Math.min(bytes.length, (b + 1) * 65535)), len = part.length;
      out[p++] = b === blocks - 1 ? 1 : 0; out[p++] = len & 255; out[p++] = len >> 8; out[p++] = ~len & 255; out[p++] = (~len >> 8) & 255;
      out.set(part, p); p += len;
    }
    var ad = adler32(bytes);
    out[p++] = ad >>> 24; out[p++] = (ad >>> 16) & 255; out[p++] = (ad >>> 8) & 255; out[p++] = ad & 255;
    return out;
  }
  function deflate(bytes) {
    if (typeof CompressionStream === 'undefined') return Promise.resolve(deflateStored(bytes));
    return streamThrough('deflate', CompressionStream, bytes).catch(function () { return deflateStored(bytes); });
  }
  function u32(b, p) { return ((b[p] << 24) | (b[p + 1] << 16) | (b[p + 2] << 8) | b[p + 3]) >>> 0; }

  /* {w, h, ctype, depth, palette, trns, idat} without inflating */
  function pngChunks(bytes) {
    var sig = [137, 80, 78, 71, 13, 10, 26, 10];
    for (var i = 0; i < 8; i++) if (bytes[i] !== sig[i]) throw new Error('not a PNG');
    var p = 8, info = { palette: null, trns: null, parts: [] };
    while (p + 8 <= bytes.length) {
      var len = u32(bytes, p), kind = String.fromCharCode(bytes[p + 4], bytes[p + 5], bytes[p + 6], bytes[p + 7]), body = bytes.subarray(p + 8, p + 8 + len);
      p += 12 + len;
      if (kind === 'IHDR') { info.w = u32(body, 0); info.h = u32(body, 4); info.depth = body[8]; info.ctype = body[9]; if (body[12]) throw new Error('interlaced PNG is not supported'); }
      else if (kind === 'PLTE') info.palette = body;
      else if (kind === 'tRNS') info.trns = body;
      else if (kind === 'IDAT') info.parts.push(body);
      else if (kind === 'IEND') break;
    }
    var total = info.parts.reduce(function (s, x) { return s + x.length; }, 0), idat = new Uint8Array(total), q = 0;
    info.parts.forEach(function (x) { idat.set(x, q); q += x.length; });
    info.idat = idat; delete info.parts;
    return info;
  }
  function pngPixels(info, raw) {
    var w = info.w, h = info.h, depth = info.depth, ct = info.ctype;
    var ch = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[ct];
    if (!ch) throw new Error('unsupported colour type ' + ct);
    var bpp = Math.max(1, ch * depth / 8 | 0), stride = Math.ceil(w * ch * depth / 8);
    var prev = new Uint8Array(stride), out = new Uint8ClampedArray(w * h * 4), p = 0, maxv = (1 << depth) - 1;
    var trns = info.trns, pal = info.palette;
    for (var y = 0; y < h; y++) {
      var f = raw[p], line = raw.slice(p + 1, p + 1 + stride);
      p += 1 + stride;
      for (var i = 0; i < stride; i++) {
        var a = i >= bpp ? line[i - bpp] : 0, b = prev[i], c = i >= bpp ? prev[i - bpp] : 0;
        if (f === 1) line[i] = (line[i] + a) & 255;
        else if (f === 2) line[i] = (line[i] + b) & 255;
        else if (f === 3) line[i] = (line[i] + ((a + b) >> 1)) & 255;
        else if (f === 4) { var pa = Math.abs(b - c), pb = Math.abs(a - c), pc = Math.abs(a + b - 2 * c); line[i] = (line[i] + (pa <= pb && pa <= pc ? a : pb <= pc ? b : c)) & 255; }
      }
      for (var x = 0; x < w; x++) {
        var vals = [], raw16 = [];
        for (var k = 0; k < ch; k++) {
          if (depth === 16) { vals.push(line[(x * ch + k) * 2]); raw16.push(line[(x * ch + k) * 2] << 8 | line[(x * ch + k) * 2 + 1]); }
          else if (depth === 8) { vals.push(line[x * ch + k]); raw16.push(line[x * ch + k]); }
          else { var bit = x * depth, v = (line[bit >> 3] >> (8 - depth - (bit & 7))) & maxv; vals.push(ct === 3 ? v : (v * 255 / maxv) | 0); raw16.push(v); }
        }
        var o = (y * w + x) * 4, r, g, bl, al;
        if (ct === 3) { var n = raw16[0]; r = pal[n * 3]; g = pal[n * 3 + 1]; bl = pal[n * 3 + 2]; al = trns && n < trns.length ? trns[n] : 255; }
        else if (ct === 0 || ct === 4) {
          r = g = bl = vals[0]; al = ct === 4 ? vals[1] : 255;
          if (ct === 0 && trns && trns.length >= 2 && raw16[0] === (trns[0] << 8 | trns[1])) al = 0;
        } else {
          r = vals[0]; g = vals[1]; bl = vals[2]; al = ct === 6 ? vals[3] : 255;
          if (ct === 2 && trns && trns.length >= 6 && raw16[0] === (trns[0] << 8 | trns[1]) && raw16[1] === (trns[2] << 8 | trns[3]) && raw16[2] === (trns[4] << 8 | trns[5])) al = 0;
        }
        out[o] = r; out[o + 1] = g; out[o + 2] = bl; out[o + 3] = al;
      }
      prev = line;
    }
    return out;
  }
  /* bytes -> Promise<{w, h, rgba}>; inflateFn defaults to the browser's DecompressionStream */
  function pngDecode(bytes, inflateFn) {
    var info;
    try { info = pngChunks(bytes); } catch (e) { return Promise.reject(e); }
    return (inflateFn || inflate)(info.idat).then(function (raw) { return { w: info.w, h: info.h, rgba: pngPixels(info, raw) }; });
  }
  function pngEncode(w, h, rgba, deflateFn) {
    var raw = new Uint8Array(h * (w * 4 + 1));
    for (var y = 0; y < h; y++) { raw[y * (w * 4 + 1)] = 0; raw.set(rgba.subarray(y * w * 4, (y + 1) * w * 4), y * (w * 4 + 1) + 1); }
    return (deflateFn || deflate)(raw).then(function (z) {
      var parts = [];
      function chunk(kind, body) {
        var c = new Uint8Array(12 + body.length), len = body.length;
        c[0] = len >>> 24; c[1] = (len >>> 16) & 255; c[2] = (len >>> 8) & 255; c[3] = len & 255;
        for (var i = 0; i < 4; i++) c[4 + i] = kind.charCodeAt(i);
        c.set(body, 8);
        var crc = crc32(c.subarray(4, 8 + len));
        c[8 + len] = crc >>> 24; c[9 + len] = (crc >>> 16) & 255; c[10 + len] = (crc >>> 8) & 255; c[11 + len] = crc & 255;
        parts.push(c);
      }
      var hdr = new Uint8Array(13);
      hdr[0] = w >>> 24; hdr[1] = (w >>> 16) & 255; hdr[2] = (w >>> 8) & 255; hdr[3] = w & 255;
      hdr[4] = h >>> 24; hdr[5] = (h >>> 16) & 255; hdr[6] = (h >>> 8) & 255; hdr[7] = h & 255;
      hdr[8] = 8; hdr[9] = 6;
      chunk('IHDR', hdr); chunk('IDAT', z); chunk('IEND', new Uint8Array(0));
      var total = 8 + parts.reduce(function (s, x) { return s + x.length; }, 0), out = new Uint8Array(total), p = 8;
      out.set([137, 80, 78, 71, 13, 10, 26, 10]);
      parts.forEach(function (x) { out.set(x, p); p += x.length; });
      return out;
    });
  }

  /* ---------------- drawing helpers (pure, return point lists or change a pixel array in place) */
  function linePoints(x0, y0, x1, y1) {
    var pts = [], dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
    for (;;) {
      pts.push([x0, y0]);
      if (x0 === x1 && y0 === y1) return pts;
      var e2 = 2 * err;
      if (e2 >= dy) { err += dy; x0 += sx; }
      if (e2 <= dx) { err += dx; y0 += sy; }
    }
  }
  function rectPoints(x0, y0, x1, y1, filled) {
    var ax = Math.min(x0, x1), bx = Math.max(x0, x1), ay = Math.min(y0, y1), by = Math.max(y0, y1), pts = [];
    for (var y = ay; y <= by; y++) for (var x = ax; x <= bx; x++) if (filled || x === ax || x === bx || y === ay || y === by) pts.push([x, y]);
    return pts;
  }
  /* Pixel Perfect: drop the corner pixel of every L-shaped step in a freehand stroke. */
  function pixelPerfect(pts) {
    var out = [];
    for (var i = 0; i < pts.length; i++) {
      if (out.length >= 2 && i < pts.length) {
        var a = out[out.length - 2], b = out[out.length - 1], c = pts[i];
        if (Math.abs(a[0] - c[0]) === 1 && Math.abs(a[1] - c[1]) === 1 && (b[0] === a[0] || b[1] === a[1])) out.pop();
      }
      var last = out[out.length - 1];
      if (!last || last[0] !== pts[i][0] || last[1] !== pts[i][1]) out.push(pts[i]);
    }
    return out;
  }
  /* Square brush: every point grows to size x size (size 2 grows right/down, 3 is centred), duplicates removed. */
  function brush(pts, size) {
    size = Math.max(1, Math.min(8, size | 0));
    if (size === 1) return pts;
    var lo = -((size - 1) >> 1), hi = lo + size - 1, seen = new Set(), out = [];
    pts.forEach(function (p) {
      for (var dy = lo; dy <= hi; dy++) for (var dx = lo; dx <= hi; dx++) {
        var x = p[0] + dx, y = p[1] + dy, k = x + ',' + y;
        if (!seen.has(k)) { seen.add(k); out.push([x, y]); }
      }
    });
    return out;
  }
  /* Points mirrored at the texture centre: sym = {x: vertical axis, y: horizontal axis} */
  function mirror(pts, w, h, sym) {
    if (!sym || (!sym.x && !sym.y)) return pts;
    var seen = new Set(), out = [];
    function add(x, y) { var k = x + ',' + y; if (!seen.has(k)) { seen.add(k); out.push([x, y]); } }
    pts.forEach(function (p) {
      add(p[0], p[1]);
      if (sym.x) add(w - 1 - p[0], p[1]);
      if (sym.y) add(p[0], h - 1 - p[1]);
      if (sym.x && sym.y) add(w - 1 - p[0], h - 1 - p[1]);
    });
    return out;
  }
  function plot(px, w, h, pts, color, alphaLock) {
    pts.forEach(function (p) {
      var x = p[0], y = p[1];
      if (x < 0 || y < 0 || x >= w || y >= h) return;
      var o = (y * w + x) * 4;
      if (alphaLock && !px[o + 3]) return;
      if (!color || !color[3]) { px[o] = px[o + 1] = px[o + 2] = px[o + 3] = 0; return; }
      px[o] = color[0]; px[o + 1] = color[1]; px[o + 2] = color[2]; px[o + 3] = color[3];
    });
  }
  function same(px, o, c) { return c[3] === 0 ? px[o + 3] === 0 : px[o] === c[0] && px[o + 1] === c[1] && px[o + 2] === c[2] && px[o + 3] === c[3]; }
  function pixelAt(px, w, x, y) { var o = (y * w + x) * 4; return [px[o], px[o + 1], px[o + 2], px[o + 3]]; }
  /* Flood fill (4-neighbour) or, with global, every pixel of the same colour. Returns the changed points. */
  function fill(px, w, h, x, y, color, global, alphaLock) {
    if (x < 0 || y < 0 || x >= w || y >= h) return [];
    var target = pixelAt(px, w, x, y);
    color = color && color[3] ? color : [0, 0, 0, 0];
    if (same(px, (y * w + x) * 4, color)) return [];
    var pts = [], seen = new Uint8Array(w * h);
    if (global) {
      for (var i = 0; i < w * h; i++) if (same(px, i * 4, target)) pts.push([i % w, (i / w) | 0]);
    } else {
      var stack = [[x, y]];
      while (stack.length) {
        var p = stack.pop(), qx = p[0], qy = p[1];
        if (qx < 0 || qy < 0 || qx >= w || qy >= h || seen[qy * w + qx]) continue;
        seen[qy * w + qx] = 1;
        if (!same(px, (qy * w + qx) * 4, target)) continue;
        pts.push([qx, qy]);
        stack.push([qx + 1, qy], [qx - 1, qy], [qx, qy + 1], [qx, qy - 1]);
      }
    }
    plot(px, w, h, pts, color, alphaLock);
    return pts;
  }
  /* Region copy/paste/clear/flip/rotate on one layer (rect = {x, y, w, h}) */
  function clipRect(r, w, h) {
    var x0 = Math.max(0, r.x), y0 = Math.max(0, r.y), x1 = Math.min(w, r.x + r.w), y1 = Math.min(h, r.y + r.h);
    return x1 > x0 && y1 > y0 ? { x: x0, y: y0, w: x1 - x0, h: y1 - y0 } : null;
  }
  function copyRect(px, w, r) {
    var out = new Uint8ClampedArray(r.w * r.h * 4);
    for (var y = 0; y < r.h; y++) out.set(px.subarray(((r.y + y) * w + r.x) * 4, ((r.y + y) * w + r.x + r.w) * 4), y * r.w * 4);
    return { w: r.w, h: r.h, px: out };
  }
  function clearRect(px, w, r) { for (var y = 0; y < r.h; y++) px.fill(0, ((r.y + y) * w + r.x) * 4, ((r.y + y) * w + r.x + r.w) * 4); }
  /* Paste a buffer at (x, y); transparent buffer pixels keep what is below (like moving a selection). */
  function pasteBuf(px, w, h, buf, x, y) {
    for (var by = 0; by < buf.h; by++) for (var bx = 0; bx < buf.w; bx++) {
      var tx = x + bx, ty = y + by, s = (by * buf.w + bx) * 4;
      if (tx < 0 || ty < 0 || tx >= w || ty >= h || !buf.px[s + 3]) continue;
      px.set(buf.px.subarray(s, s + 4), (ty * w + tx) * 4);
    }
  }
  function flipBuf(buf, horizontal) {
    var out = new Uint8ClampedArray(buf.px.length);
    for (var y = 0; y < buf.h; y++) for (var x = 0; x < buf.w; x++) {
      var sx = horizontal ? buf.w - 1 - x : x, sy = horizontal ? y : buf.h - 1 - y;
      out.set(buf.px.subarray((sy * buf.w + sx) * 4, (sy * buf.w + sx) * 4 + 4), (y * buf.w + x) * 4);
    }
    return { w: buf.w, h: buf.h, px: out };
  }
  function rotateBuf(buf) { // 90 degrees clockwise
    var out = new Uint8ClampedArray(buf.px.length);
    for (var y = 0; y < buf.h; y++) for (var x = 0; x < buf.w; x++) {
      var nx = buf.h - 1 - y, ny = x;
      out.set(buf.px.subarray((y * buf.w + x) * 4, (y * buf.w + x) * 4 + 4), (ny * buf.h + nx) * 4);
    }
    return { w: buf.h, h: buf.w, px: out };
  }
  /* Region-wise transform: lift, transform, put back (region must keep its size, so rotate needs a square). */
  function transformRect(px, w, h, r, fn) {
    var buf = copyRect(px, w, r), t = fn(buf);
    if (t.w !== r.w || t.h !== r.h) return false;
    clearRect(px, w, r);
    for (var y = 0; y < r.h; y++) px.set(t.px.subarray(y * r.w * 4, (y + 1) * r.w * 4), ((r.y + y) * w + r.x) * 4);
    return true;
  }
  /* Merge layer n into the one below it: the result is what the two show together. */
  function mergeDown(model, n) {
    if (n <= 0 || n >= model.layers.length) return false;
    var lo = model.layers[n - 1], hi = model.layers[n];
    var tmp = { w: model.w, h: model.h, layers: [
      { visible: true, opacity: lo.visible ? lo.opacity : 0, px: lo.px },
      { visible: true, opacity: hi.visible ? hi.opacity : 0, px: hi.px }] };
    lo.px = compose(tmp); lo.opacity = 100; lo.visible = true;
    model.layers.splice(n, 1);
    return true;
  }
  function colorsOf(model) {
    var seen = new Set(), out = [];
    model.layers.forEach(function (l) {
      for (var i = 0; i < model.w * model.h * 4; i += 4) if (l.px[i + 3]) {
        var c = colorHex(l.px[i], l.px[i + 1], l.px[i + 2], l.px[i + 3]);
        if (!seen.has(c)) { seen.add(c); out.push(c); }
      }
    });
    return out;
  }
  /* GIMP palette text (Resprite/Aseprite/Lospec all read .gpl) */
  function gpl(name, colors) {
    return 'GIMP Palette\nName: ' + name + '\nColumns: 8\n#\n' + colors.map(function (c) {
      var v = parseColor(c); return ('  ' + v[0]).slice(-3) + ' ' + ('  ' + v[1]).slice(-3) + ' ' + ('  ' + v[2]).slice(-3) + '\t' + c.slice(1);
    }).join('\n') + '\n';
  }
  /* Own Vanilla-near base palette (hand-picked tones in Minecraft's material families, no copied pixels). */
  var VANILLA_NEAR = [
    '#1b1b1fff', '#3a3a40ff', '#5d5d63ff', '#7f7f84ff', '#a2a2a6ff', '#c9c9ccff', '#ebebedff', '#ffffffff',
    '#3c2a17ff', '#5a3f22ff', '#7a5832ff', '#9c7646ff', '#bd9a62ff', '#dcc08aff',
    '#5c3a23ff', '#8a5a36ff', '#b47a4aff', '#d89c63ff',
    '#7a1712ff', '#b2261cff', '#e04a36ff', '#8a5a00ff', '#d29a16ff', '#f6d04cff', '#fff3a8ff',
    '#1d4a1aff', '#2f7a29ff', '#58a83dff', '#93d25fff',
    '#123d63ff', '#2457a3ff', '#3f86d6ff', '#7cc0f0ff',
    '#0f5a57ff', '#22928bff', '#4fd1c5ff', '#a8f0e8ff',
    '#3d1d5aff', '#6b3596ff', '#a55fd0ff', '#d7a7f2ff',
    '#7d4628ff', '#b5683dff', '#d98a5aff', '#57a28aff'
  ];

  /* ---------------- history: whole-model snapshots, bounded by bytes */
  function History(limitBytes) { this.past = []; this.future = []; this.limit = limitBytes || 64e6; }
  function sizeOf(m) { return m.layers.reduce(function (s, l) { return s + l.px.length; }, 0); }
  History.prototype.push = function (model) {
    this.past.push(cloneModel(model)); this.future = [];
    var total = this.past.reduce(function (s, m) { return s + sizeOf(m); }, 0);
    while (this.past.length > 1 && total > this.limit) total -= sizeOf(this.past.shift());
  };
  History.prototype.undo = function (current) { if (!this.past.length) return null; this.future.push(cloneModel(current)); return this.past.pop(); };
  History.prototype.redo = function (current) { if (!this.future.length) return null; this.past.push(cloneModel(current)); return this.future.pop(); };

  return {
    FORMAT: FORMAT, SUFFIX: SUFFIX, VANILLA_NEAR: VANILLA_NEAR,
    rnd: rnd, colorHex: colorHex, parseColor: parseColor, digitsFor: digitsFor,
    newLayer: newLayer, blank: blank, validate: validate, fromDoc: fromDoc, toDoc: toDoc, dumps: dumps, fromRgba: fromRgba,
    compose: compose, cloneModel: cloneModel, mergeDown: mergeDown, colorsOf: colorsOf, gpl: gpl,
    crc32: crc32, inflate: inflate, deflate: deflate, deflateStored: deflateStored, pngChunks: pngChunks, pngPixels: pngPixels,
    pngDecode: pngDecode, pngEncode: pngEncode,
    linePoints: linePoints, rectPoints: rectPoints, pixelPerfect: pixelPerfect, brush: brush, mirror: mirror, plot: plot, fill: fill, pixelAt: pixelAt,
    clipRect: clipRect, copyRect: copyRect, clearRect: clearRect, pasteBuf: pasteBuf, flipBuf: flipBuf, rotateBuf: rotateBuf, transformRect: transformRect,
    History: History
  };
});
