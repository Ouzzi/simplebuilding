/* conv-lang.js (N51, b-conv51): JS port of the Bausprache parser (BlueprintCode.Parser, modules/simplebuildingtools,
 * spec docs/BLUEPRINT.md): aliases, variables and expressions, axis/corner boxes, repeats, shapes incl. hollow_ (raster
 * like OctantShape, orientation up), pixels rows (PixelRle), air clears, same limits and error keys as Java.
 * Differences (no game registry in the browser): an unknown block id is a warning and still placed; property names and
 * values are not checked. Result model: cells Map(key -> canonical spec) in the code's own coordinates. */
(function (root) {
  'use strict';
  var C = root.Conv51 || require('./conv-core.js');
  var GRID = 256, MAX_STORED = 1000000, MAX_CELLS = 4194304, MAX_SHAPE_VOLUME = GRID * GRID * GRID, MAX_NUMBER = 999999, MAX_DEPTH = 32, MAX_REPEAT = GRID;
  var SHAPES = { box: 6, sphere: 4, dome: 4, cylinder: 5, pyramid: 5, line: 0 };
  var SYMBOLS = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
  var AIR = /^minecraft:(air|cave_air|void_air)$/;
  function SyntaxErr() {} function Reported() {}
  var SYNTAX = new SyntaxErr(), REPORTED = new Reported();
  function isWs(c) { return c === ' ' || c === '\t' || c === '\r' || c === '\f' || c === '\v' || c === '\u000b'; }
  function isDigit(c) { return c >= '0' && c <= '9'; }
  function isLetter(c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'); }
  function isNameChar(c) { return isLetter(c) || c === '_' || isDigit(c); }
  function isAliasName(n) { return n.length > 0 && n.length <= 24 && /^[A-Za-z_][A-Za-z_0-9]*$/.test(n); }
  function floorDiv(a, b) { return Math.floor(a / b); }
  function floorMod(a, b) { return a - Math.floor(a / b) * b; }

  /* PixelRle.decode: indices (-1 = free) or null */
  function decodePixelRow(text, maxLength) {
    var out = [], i = 0;
    while (i < text.length) {
      var run = 1, start = i;
      while (i < text.length && isDigit(text[i])) i++;
      if (i > start) { if (text[start] === '0' || i - start > 6) return null; run = parseInt(text.slice(start, i), 10); if (run < 2) return null; }
      if (i >= text.length) return null;
      var c = text[i++], v = c === '.' ? -1 : SYMBOLS.indexOf(c);
      if (v === -1 && c !== '.') return null;
      if (out.length + run > maxLength) return null;
      for (var k = 0; k < run; k++) out.push(v);
    }
    return out;
  }

  /* ---------------- shape raster (OctantShape, Direction.UP) ---------------- */
  function inEllipse(x, z, b) {
    var w = b[3] - b[0], l = b[5] - b[2], cx = b[0] + w / 2, cz = b[2] + l / 2, rx = w / 2, rz = l / 2;
    if (rx <= 0 || rz <= 0) return false;
    return (x - cx) * (x - cx) / (rx * rx) + (z - cz) * (z - cz) / (rz * rz) <= 1.0;
  }
  function inEllipsoid(x, y, z, b) {
    var w = b[3] - b[0], h = b[4] - b[1], l = b[5] - b[2], cx = b[0] + w / 2, cy = b[1] + h / 2, cz = b[2] + l / 2, rx = w / 2, ry = h / 2, rz = l / 2;
    if (rx <= 0 || ry <= 0 || rz <= 0) return false;
    return (x - cx) * (x - cx) / (rx * rx) + (y - cy) * (y - cy) / (ry * ry) + (z - cz) * (z - cz) / (rz * rz) <= 1.0;
  }
  function inPyramid(x, y, z, b) {
    var w = b[3] - b[0], h = b[4] - b[1], l = b[5] - b[2], cx = b[0] + w / 2, cz = b[2] + l / 2, rx = w / 2, rz = l / 2;
    if (y < b[1] || y >= b[4]) return false;
    var progress = (y - b[1]) / h;
    return Math.abs(x - cx) <= rx * (1.0 - progress) && Math.abs(z - cz) <= rz * (1.0 - progress);
  }
  /* cells of the box b (inclusive ints) as Uint8Array, index x + sx*(z + sz*y) */
  function raster(shape, b) {
    var sx = b[3] - b[0] + 1, sy = b[4] - b[1] + 1, sz = b[5] - b[2] + 1, cells = new Uint8Array(sx * sy * sz), i = 0;
    if (shape === 'box') { cells.fill(1); return cells; }
    var aabb = shape === 'dome' ? [b[0], b[1] - (b[4] - b[1]), b[2], b[3] + 1, b[4] + 1, b[5] + 1] : [b[0], b[1], b[2], b[3] + 1, b[4] + 1, b[5] + 1];
    for (var y = 0; y < sy; y++) for (var z = 0; z < sz; z++) for (var x = 0; x < sx; x++, i++) {
      var px = b[0] + x, py = b[1] + y, pz = b[2] + z, ok;
      if (shape === 'cylinder') ok = py >= aabb[1] && py < aabb[4] && inEllipse(px + 0.5, pz + 0.5, aabb);
      else if (shape === 'pyramid') ok = inPyramid(px + 0.5, py + 0.5, pz + 0.5, aabb);
      else ok = inEllipsoid(px + 0.5, py + 0.5, pz + 0.5, aabb);
      if (ok) cells[i] = 1;
    }
    return cells;
  }
  function rasterLine(b, r) {
    var sx = b[3] - b[0] + 1, sy = b[4] - b[1] + 1, sz = b[5] - b[2] + 1, cells = new Uint8Array(sx * sy * sz);
    var ddx = r[3] - r[0], ddy = r[4] - r[1], ddz = r[5] - r[2], steps = Math.max(Math.abs(ddx), Math.abs(ddy), Math.abs(ddz));
    for (var i = 0; i <= steps; i++) {
      var x = r[0] + (steps === 0 ? 0 : Math.round(ddx * i / steps)), y = r[1] + (steps === 0 ? 0 : Math.round(ddy * i / steps)), z = r[2] + (steps === 0 ? 0 : Math.round(ddz * i / steps));
      cells[(x - b[0]) + sx * ((z - b[2]) + sz * (y - b[1]))] = 1;
    }
    return cells;
  }
  function shell(solid, b) {
    var sx = b[3] - b[0] + 1, sy = b[4] - b[1] + 1, sz = b[5] - b[2] + 1, layer = sx * sz, out = new Uint8Array(solid.length);
    for (var i = 0; i < solid.length; i++) {
      if (!solid[i]) continue;
      var x = i % sx, z = Math.floor(i / sx) % sz, y = Math.floor(i / layer);
      var interior = x > 0 && x < sx - 1 && y > 0 && y < sy - 1 && z > 0 && z < sz - 1 && solid[i - 1] && solid[i + 1] && solid[i - sx] && solid[i + sx] && solid[i - layer] && solid[i + layer];
      if (!interior) out[i] = 1;
    }
    return out;
  }
  function count(cells) { var n = 0; for (var i = 0; i < cells.length; i++) n += cells[i]; return n; }
  function ordered(r) { return [Math.min(r[0], r[3]), Math.min(r[1], r[4]), Math.min(r[2], r[5]), Math.max(r[0], r[3]), Math.max(r[1], r[4]), Math.max(r[2], r[5])]; }
  function volume(b) { return (b[3] - b[0] + 1) * (b[4] - b[1] + 1) * (b[5] - b[2] + 1); }
  function inGrid(x, y, z) { return x >= 0 && y >= 0 && z >= 0 && x < GRID && y < GRID && z < GRID; }

  function parse(code) {
    code = code == null ? '' : String(code);
    var cells = new Map(), problems = [], warnings = {}, aliases = new Map(), variables = new Map();
    var expanded = 0, pixels = 0, shapeVolume = 0, budgetExceeded = false, line = 0, ep = 0, eEnd = 0, eDepth = 0;
    function problem(start, end, key) { problems.push({ line: line, start: start, end: Math.max(end, start + 1), key: key, args: Array.prototype.slice.call(arguments, 3).map(String) }); }
    function set(x, y, z, st) { var k = C.key(x, y, z); if (st === null) cells.delete(k); else cells.set(k, st); }

    if (code.length > MAX_STORED) problems.push({ line: 0, start: MAX_STORED, end: code.length, key: 'too_long', args: [String(code.length), String(MAX_STORED)] });
    var lineStart = 0;
    while (lineStart <= code.length) {
      var lineEnd = code.indexOf('\n', lineStart);
      if (lineEnd < 0) lineEnd = code.length;
      parseLine(lineStart, lineEnd);
      lineStart = lineEnd + 1; line++;
    }

    function splitWords(from, to) {
      var words = [], i = from;
      while (i < to) {
        while (i < to && isWs(code[i])) i++;
        if (i >= to) break;
        var start = i, depth = 0, parens = 0;
        while (i < to) {
          var c = code[i];
          if (c === '[') depth++; if (c === ']') depth = Math.max(0, depth - 1);
          if (c === '(') parens++; if (c === ')') parens = Math.max(0, parens - 1);
          if (depth === 0 && parens === 0 && isWs(c)) break;
          i++;
        }
        words.push({ text: code.slice(start, i), start: start, end: i });
      }
      return words;
    }
    function W(text, start) { return { text: text, start: start, end: start + text.length }; }
    function parseLine(from, to) {
      var hash = code.indexOf('#', from);
      if (hash >= 0 && hash < to) to = hash;
      var words = splitWords(from, to);
      if (!words.length) return;
      var first = words[0], rest;
      if (first.text[0] === '$') {
        var eq = first.text.indexOf('=');
        if (eq >= 0) {
          rest = [];
          if (eq + 1 < first.text.length) rest.push(W(first.text.slice(eq + 1), first.start + eq + 1));
          define(W(first.text.slice(0, eq), first.start), first.start + eq, rest.concat(words.slice(1)));
          return;
        }
        if (words.length > 1 && words[1].text[0] === '=') {
          var eqw = words[1]; rest = [];
          if (eqw.text.length > 1) rest.push(W(eqw.text.slice(1), eqw.start + 1));
          define(first, eqw.start, rest.concat(words.slice(2)));
          return;
        }
      }
      if (first.text === 'pixels') { pixelsLine(first, words); return; }
      var state = parseSpec(first);
      if (words.length === 1) { problem(first.start, first.end, 'no_region'); return; }
      for (var i = 1; i < words.length; i++) {
        var region = parseRegion(words[i]);
        if (region && state !== undefined) apply(region, state, words[i]);
      }
    }
    function pixelsLine(keyword, words) {
      if (words.length !== 3) { problem(keyword.start, words[words.length - 1].end, 'pixels_usage'); return; }
      var at = words[1], row = words[2], p, states = [], failed = false;
      try { p = triple(at.start, at.end); } catch (e) { if (e === SYNTAX) problem(at.start, at.end, 'bad_region', at.text); else if (e !== REPORTED) throw e; return; }
      for (var i = 0; i < row.text.length; i++) {
        var c = row.text[i], idx = SYMBOLS.indexOf(c);
        if (idx >= 0) {
          var st = aliases.has(c) ? aliases.get(c) : undefined;
          if (st === undefined) { problem(row.start + i, row.start + i + 1, variables.has(c) ? 'variable_as_block' : 'unknown_alias', '$' + c); failed = true; }
          else states[idx] = st;
        }
      }
      var row2 = decodePixelRow(row.text, GRID);
      if (!row2) { problem(row.start, row.end, 'pixels_bad_row', row.text); return; }
      if (failed) return;
      var x0 = p[0], y = p[1], z = p[2];
      if (row2.length > 0 && (!inGrid(x0, y, z) || !inGrid(x0 + row2.length - 1, y, z))) { problem(keyword.start, row.end, 'pixels_outside', String(GRID - 1)); return; }
      var filled = row2.filter(function (v) { return v >= 0; }).length;
      if (budgetExceeded) return;
      if (expanded + filled > MAX_CELLS) { budgetExceeded = true; problem(keyword.start, row.end, 'too_many_cells', String(MAX_CELLS)); return; }
      expanded += filled; pixels += filled;
      row2.forEach(function (v, i) { if (v >= 0) set(x0 + i, y, z, states[v]); });
    }
    function define(name, eqPos, rest) {
      var an = name.text.slice(1);
      if (!isAliasName(an)) { problem(name.start, name.end, 'bad_alias_name', name.text); return; }
      if (!rest.length) { problem(name.start, eqPos + 1, 'alias_without_block', name.text); return; }
      var sp = rest[0];
      if (isExpressionStart(sp, rest.length)) {
        var from = sp.start, to = rest[rest.length - 1].end;
        try { var v = evalExpr(from, to); variables.set(an, v); aliases.delete(an); }
        catch (e) { if (e === SYNTAX) problem(from, to, 'bad_expression', code.slice(from, to)); else if (e !== REPORTED) throw e; }
        return;
      }
      if (rest.length > 1) problem(rest[1].start, rest[rest.length - 1].end, 'alias_extra');
      if (sp.text[0] === '$') { problem(sp.start, sp.end, 'alias_of_alias'); return; }
      var st = parseSpec(sp);
      if (st !== undefined) { aliases.set(an, st); variables.delete(an); }
    }
    function isExpressionStart(sp, n) {
      var c = sp.text[0];
      if (isDigit(c) || c === '-' || c === '+' || c === '(') return true;
      if (c !== '$') return false;
      var k = 1; while (k < sp.text.length && isNameChar(sp.text[k])) k++;
      var nm = sp.text.slice(1, k);
      return !(n === 1 && k === sp.text.length && aliases.has(nm));
    }
    /* block word -> canonical spec, null for air (clears), undefined on error */
    function parseSpec(word) {
      var text = word.text;
      if (text[0] === '$') {
        var nm = text.slice(1);
        if (!aliases.has(nm)) { problem(word.start, word.end, variables.has(nm) ? 'variable_as_block' : 'unknown_alias', text); return undefined; }
        return aliases.get(nm);
      }
      var br = text.indexOf('['), idText = br >= 0 ? text.slice(0, br) : text;
      if (!/^([a-z0-9_.-]+:)?[a-z0-9_.\/-]+$/.test(idText)) { problem(word.start, word.start + Math.max(1, idText.length), 'bad_block_id', idText); return undefined; }
      var id = C.fullId(idText), props = {};
      if (C.DATA && !C.isKnown(id)) warnings[id] = (warnings[id] || 0) + 1;
      if (br >= 0) {
        if (text[text.length - 1] !== ']') { problem(word.start + br, word.end, 'unclosed_properties'); return undefined; }
        var inner = text.slice(br + 1, text.length - 1), offset = word.start + br + 1, failed = false, parts = inner.split(',');
        for (var i = 0; i < parts.length; i++) {
          var part = parts[i], ps = offset; offset += part.length + 1;
          var tr = part.trim();
          if (!tr) { if (inner.trim()) { problem(ps, ps + 1, 'empty_property'); failed = true; } continue; }
          var eq = tr.indexOf('=');
          if (eq <= 0 || eq === tr.length - 1) { problem(ps, ps + tr.length, 'bad_property', tr); failed = true; continue; }
          props[tr.slice(0, eq).trim()] = tr.slice(eq + 1).trim();
        }
        if (failed) return undefined;
      }
      if (AIR.test(id)) return null;
      return C.spec(id, props);
    }
    function topLevel(from, to, ch) {
      var d = 0; for (var i = from; i < to; i++) { var c = code[i]; if (c === '(') d++; else if (c === ')') d--; else if (c === ch && d === 0) return i; } return -1;
    }
    function topLevelDots(from, to) {
      var d = 0; for (var i = from; i + 1 < to; i++) { var c = code[i]; if (c === '(') d++; else if (c === ')') d--; else if (c === '.' && code[i + 1] === '.' && d === 0) return i; } return -1;
    }
    function balanced(from, to) {
      var d = 0;
      for (var i = from; i < to; i++) { var c = code[i]; if (c === '(') d++; if (c === ')' && --d < 0) return false; if (d === 0 && i < to - 1) return false; }
      return d === 0;
    }
    function splitTop(from, to) {
      var parts = [], d = 0, ps = from;
      for (var i = from; i < to; i++) { var c = code[i]; if (c === '(') d++; else if (c === ')') d--; else if (c === ',' && d === 0) { parts.push([ps, i]); ps = i + 1; } }
      parts.push([ps, to]); return parts;
    }
    function parseRegion(word) {
      var start = word.start, end = word.end, cnt = 1, dx = 0, dy = 0, dz = 0;
      var star = topLevel(start, end, '*'), baseEnd = star >= 0 ? star : end;
      if (star >= 0) {
        var at = code.indexOf('@', star + 1), n, step;
        if (at < 0 || at >= end) { problem(star, end, 'bad_repeat', code.slice(star, end)); return null; }
        try { n = evalExpr(star + 1, at); step = triple(at + 1, end); }
        catch (e) { if (e === SYNTAX) { problem(star, end, 'bad_repeat', code.slice(star, end)); return null; } if (e === REPORTED) return null; throw e; }
        if (n < 1 || n > MAX_REPEAT) { problem(star + 1, at, 'repeat_range', String(n), String(MAX_REPEAT)); return null; }
        cnt = n; dx = step[0]; dy = step[1]; dz = step[2];
      }
      if (baseEnd > start && isLetter(code[start])) return parseShape(start, baseEnd, cnt, dx, dy, dz);
      var box;
      try { box = ordered(parseBox(start, baseEnd)); }
      catch (e) { if (e === SYNTAX) { problem(start, baseEnd, 'bad_region', code.slice(start, baseEnd)); return null; } if (e === REPORTED) return null; throw e; }
      var b = inGridOrReport(box, start, baseEnd);
      return b ? { box: b, count: cnt, dx: dx, dy: dy, dz: dz, cells: null, cellCount: volume(b) } : null;
    }
    function parseShape(start, end, cnt, dx, dy, dz) {
      var open = code.indexOf('(', start);
      if (open < 0 || open >= end || code[end - 1] !== ')' || !balanced(open, end)) { problem(start, end, 'bad_region', code.slice(start, end)); return null; }
      var name = code.slice(start, open), hollow = name.indexOf('hollow_') === 0, shape = hollow ? name.slice(7) : name;
      if (!(shape in SHAPES) || (hollow && shape === 'line')) { problem(start, open, 'unknown_shape', name); return null; }
      var from = open + 1, to = end - 1, box, ln = null;
      try {
        var dots = topLevelDots(from, to), parts = splitTop(from, to);
        if (shape === 'line') {
          if (dots < 0 || splitTop(from, dots).length !== 3 || splitTop(dots + 2, to).length !== 3) throw SYNTAX;
          ln = parseBox(from, to); box = ordered(ln);
        } else if (dots >= 0 || parts.length === 3) box = ordered(parseBox(from, to));
        else {
          if (parts.length !== SHAPES[shape]) throw SYNTAX;
          var v = parts.map(function (p) { return evalExpr(p[0], p[1]); });
          box = numericBox(shape, v, parts);
        }
      } catch (e) {
        if (e === SYNTAX) { problem(from - 1, end, 'bad_shape_args', name); return null; }
        if (e === REPORTED) return null; throw e;
      }
      if (!box) return null;
      var b = inGridOrReport(box, start, end);
      if (!b) return null;
      var vol = volume(b);
      if (budgetExceeded) return { box: b, count: cnt, dx: dx, dy: dy, dz: dz, cells: new Uint8Array(vol), cellCount: 0 };
      if (shape === 'box' && !hollow) return { box: b, count: cnt, dx: dx, dy: dy, dz: dz, cells: null, cellCount: vol };
      if (shapeVolume + vol > MAX_SHAPE_VOLUME) { problem(start, end, 'shape_volume', String(MAX_SHAPE_VOLUME)); return null; }
      shapeVolume += vol;
      var cells = shape === 'line' ? rasterLine(b, ln) : raster(shape, b);
      if (hollow) cells = shell(cells, b);
      return { box: b, count: cnt, dx: dx, dy: dy, dz: dz, cells: cells, cellCount: count(cells) };
    }
    function numericBox(shape, v, parts) {
      function atLeast(val, min, span) { if (val >= min) return true; problem(span[0], span[1], 'shape_size', String(val), String(min)); return false; }
      switch (shape) {
        case 'box': for (var i = 3; i < 6; i++) if (!atLeast(v[i], 1, parts[i])) return null; return [v[0], v[1], v[2], v[0] + v[3] - 1, v[1] + v[4] - 1, v[2] + v[5] - 1];
        case 'sphere': return atLeast(v[3], 0, parts[3]) ? [v[0] - v[3], v[1] - v[3], v[2] - v[3], v[0] + v[3], v[1] + v[3], v[2] + v[3]] : null;
        case 'dome': return atLeast(v[3], 0, parts[3]) ? [v[0] - v[3], v[1], v[2] - v[3], v[0] + v[3], v[1] + v[3], v[2] + v[3]] : null;
        default: return atLeast(v[3], 0, parts[3]) && atLeast(v[4], 1, parts[4]) ? [v[0] - v[3], v[1], v[2] - v[3], v[0] + v[3], v[1] + v[4] - 1, v[2] + v[3]] : null;
      }
    }
    function inGridOrReport(box, start, end) {
      for (var i = 0; i < 6; i++) if (box[i] < 0 || box[i] >= GRID) { problem(start, end, 'coordinate_range', String(box[i]), String(GRID - 1)); return null; }
      return box.slice();
    }
    function parseBox(from, to) {
      var dots = topLevelDots(from, to);
      if (dots >= 0 && topLevelDots(dots + 2, to) < 0 && splitTop(from, dots).length === 3 && splitTop(dots + 2, to).length === 3) {
        var a = triple(from, dots), b = triple(dots + 2, to); return [a[0], a[1], a[2], b[0], b[1], b[2]];
      }
      var parts = splitTop(from, to);
      if (parts.length !== 3) throw SYNTAX;
      var out = [0, 0, 0, 0, 0, 0];
      for (var i = 0; i < 3; i++) {
        var p = parts[i], r = topLevelDots(p[0], p[1]);
        if (r >= 0) { out[i] = evalExpr(p[0], r); out[i + 3] = evalExpr(r + 2, p[1]); } else { out[i] = evalExpr(p[0], p[1]); out[i + 3] = out[i]; }
      }
      return out;
    }
    function triple(from, to) {
      var parts = splitTop(from, to);
      if (parts.length !== 3) throw SYNTAX;
      return parts.map(function (p) { return evalExpr(p[0], p[1]); });
    }
    function evalExpr(from, to) { ep = from; eEnd = to; eDepth = 0; var v = sum(); skipWs(); if (ep !== eEnd) throw SYNTAX; return v; }
    function skipWs() { while (ep < eEnd && (isWs(code[ep]) || code[ep] === '\n')) ep++; }
    function checked(v, start) { if (Math.abs(v) > MAX_NUMBER) { problem(start, ep, 'number_range', String(v), String(MAX_NUMBER)); throw REPORTED; } return v; }
    function sum() {
      skipWs(); var start = ep, v = product();
      for (;;) { skipWs(); if (ep >= eEnd) return v; var c = code[ep]; if (c !== '+' && c !== '-') return v; ep++; var r = product(); v = checked(c === '+' ? v + r : v - r, start); }
    }
    function product() {
      skipWs(); var start = ep, v = factor();
      for (;;) {
        skipWs(); if (ep >= eEnd) return v;
        var c = code[ep]; if (c !== '*' && c !== '/' && c !== '%') return v;
        var op = ep++, r = factor();
        if (c !== '*' && r === 0) { problem(op, ep, 'division_by_zero'); throw REPORTED; }
        v = checked(c === '*' ? v * r : c === '/' ? floorDiv(v, r) : floorMod(v, r), start);
      }
    }
    function factor() {
      skipWs();
      if (ep >= eEnd || ++eDepth > MAX_DEPTH) throw SYNTAX;
      var start = ep, c = code[ep];
      try {
        if (c === '+' || c === '-') { ep++; var v = factor(); return c === '-' ? -v : v; }
        if (c === '(') { ep++; var w = sum(); skipWs(); if (ep >= eEnd || code[ep] !== ')') throw SYNTAX; ep++; return w; }
        if (c === '$') {
          ep++; while (ep < eEnd && isNameChar(code[ep])) ep++;
          var nm = code.slice(start + 1, ep);
          if (!nm) throw SYNTAX;
          if (!variables.has(nm)) { problem(start, ep, aliases.has(nm) ? 'alias_as_number' : 'unknown_variable', '$' + nm); throw REPORTED; }
          return variables.get(nm);
        }
        if (isDigit(c)) { while (ep < eEnd && isDigit(code[ep])) ep++; if (ep - start > 6) throw SYNTAX; return parseInt(code.slice(start, ep), 10); }
        throw SYNTAX;
      } finally { eDepth--; }
    }
    function apply(region, state, word) {
      if (budgetExceeded) return;
      var b = region.box, n = region.cellCount * region.count;
      if (expanded + n > MAX_CELLS) { budgetExceeded = true; problem(word.start, word.end, 'too_many_cells', String(MAX_CELLS)); return; }
      for (var k = 0; k < region.count; k++) {
        var ox = region.dx * k, oy = region.dy * k, oz = region.dz * k;
        if (!inGrid(b[0] + ox, b[1] + oy, b[2] + oz) || !inGrid(b[3] + ox, b[4] + oy, b[5] + oz)) { problem(word.start, word.end, 'repeat_outside', String(k + 1), String(GRID - 1)); return; }
      }
      expanded += n;
      var sx = b[3] - b[0] + 1, sz = b[5] - b[2] + 1;
      for (k = 0; k < region.count; k++) {
        ox = region.dx * k; oy = region.dy * k; oz = region.dz * k;
        if (!region.cells) {
          for (var y = b[1]; y <= b[4]; y++) for (var z = b[2]; z <= b[5]; z++) for (var x = b[0]; x <= b[3]; x++) set(x + ox, y + oy, z + oz, state);
          continue;
        }
        for (var i = 0; i < region.cells.length; i++) {
          if (!region.cells[i]) continue;
          set(b[0] + i % sx + ox, b[1] + Math.floor(i / (sx * sz)) + oy, b[2] + Math.floor(i / sx) % sz + oz, state);
        }
      }
    }

    var mx = [-1, -1, -1];
    cells.forEach(function (s, k) { var x = C.kx(k), y = C.ky(k), z = C.kz(k); if (x > mx[0]) mx[0] = x; if (y > mx[1]) mx[1] = y; if (z > mx[2]) mx[2] = z; });
    var info = C.newInfo('code');
    var model = { cells: cells, n: cells.size, sx: mx[0] + 1, sy: mx[1] + 1, sz: mx[2] + 1, info: info };
    C.markModel(model);
    return { model: model, problems: problems, warnings: warnings, pixels: pixels, ok: problems.length === 0 };
  }
  /* model in the code frame -> model normalised to its min corner (like the scan) */
  function normalized(model) {
    var t = C.subModel(model, 0, Infinity, 0, Infinity, 0, Infinity);
    t.info = model.info; return t;
  }

  C.parseCode = parse; C.normalized = normalized; C.decodePixelRow = decodePixelRow; C.rasterShape = raster;
  if (typeof module !== 'undefined' && module.exports) module.exports = C;
})(typeof window !== 'undefined' ? window : globalThis);
