/* Enchanting view (?tab=enchanting): the enchanting table and enchanted-item tooltips drawn like the game,
   so texts and colours can be checked without starting Minecraft. Data: data/enchanting.js (wiki/enchanting.py).
   Plan: docs/ai/PLAN-B-WIKIENCH-2026-10-21.md.

   Game logic follows 26.3 (EnchantmentMenu.slotsChanged/getEnchantmentList, EnchantmentHelper.getEnchantmentCost/
   selectEnchantment/getAvailableEnchantmentResults) with the same random generator as the game (LegacyRandomSource,
   the java.util.Random LCG). The GUI is drawn with CSS in own colours - no Vanilla pixels (PRINZIPIEN A8).
   UMD: the logic also runs under node (wiki/tests/enchanting_core.cjs). */
(function (root, factory) {
  if (typeof module === 'object' && module.exports) module.exports = factory();
  else root.SBEnchanting = factory();
})(typeof self !== 'undefined' ? self : this, function () {
  'use strict';

  /* ---------------- java.util.Random / LegacyRandomSource ---------------- */
  var MULT = BigInt('0x5DEECE66D'), MASK = (BigInt(1) << BigInt(48)) - BigInt(1);
  function JavaRandom(seed) { this.setSeed(seed); }
  JavaRandom.prototype.setSeed = function (seed) { this.s = (BigInt.asIntN(64, BigInt(seed)) ^ MULT) & MASK; };
  JavaRandom.prototype.next = function (bits) {
    this.s = (this.s * MULT + BigInt(11)) & MASK;
    return Number(BigInt.asIntN(32, this.s >> BigInt(48 - bits)));
  };
  JavaRandom.prototype.nextInt = function (bound) {
    if (bound === undefined) return this.next(32);
    if (bound <= 0) throw new Error('bound must be positive');
    if ((bound & -bound) === bound) return Number((BigInt(bound) * BigInt(this.next(31))) >> BigInt(31));
    var bits, val;
    do { bits = this.next(31); val = bits % bound; } while (((bits - val + (bound - 1)) | 0) < 0);
    return val;
  };
  JavaRandom.prototype.nextFloat = function () { return Math.fround(this.next(24) * Math.fround(5.9604645e-8)); };

  /* ---------------- game rules ---------------- */
  function costAt(cost, level) { return cost ? cost.base + (cost.per_level_above_first || 0) * (level - 1) : 0; }
  /* EnchantmentHelper.getEnchantmentCost */
  function tableCost(rand, slot, shelves, enchantability) {
    if (!enchantability) return 0;
    if (shelves > 15) shelves = 15;
    var i = rand.nextInt(8) + 1 + (shelves >> 1) + rand.nextInt(shelves + 1);
    if (slot === 0) return Math.max((i / 3) | 0, 1);
    return slot === 1 ? ((i * 2 / 3) | 0) + 1 : Math.max(i, shelves * 2);
  }
  function byId(data) {
    if (!data._byId) { data._byId = {}; data.enchantments.forEach(function (e) { data._byId[e.id] = e; }); }
    return data._byId;
  }
  /* getAvailableEnchantmentResults: highest level whose cost window holds the power, in tag order. */
  function available(data, item, power) {
    var E = byId(data), out = [];
    item.table.forEach(function (id) {
      var e = E[id];
      for (var lvl = e.maxLevel; lvl >= 1; lvl--) {
        if (power >= costAt(e.minCost, lvl) && power <= costAt(e.maxCost, lvl)) { out.push({ id: id, level: lvl, weight: e.weight }); break; }
      }
    });
    return out;
  }
  function compatible(data, a, b) {
    var E = byId(data), x = E[a], y = E[b];
    return a !== b && x.exclusive.indexOf(b) < 0 && y.exclusive.indexOf(a) < 0;
  }
  function weighted(rand, list) {
    var total = 0;
    list.forEach(function (e) { total += e.weight; });
    if (total <= 0) return null;
    var j = rand.nextInt(total);
    for (var i = 0; i < list.length; i++) { j -= list[i].weight; if (j < 0) return list[i]; }
    return null;
  }
  /* selectEnchantment */
  function select(rand, data, item, cost) {
    var list = [], ench = item.enchantability;
    if (ench <= 0) return list;
    var level = cost + 1 + rand.nextInt(((ench / 4) | 0) + 1) + rand.nextInt(((ench / 4) | 0) + 1);
    var f = Math.fround(Math.fround(Math.fround(rand.nextFloat() + rand.nextFloat()) - 1) * Math.fround(0.15));
    var lf = Math.fround(level);
    level = Math.max(1, Math.floor(Math.fround(lf + Math.fround(lf * f)) + 0.5));
    var pool = available(data, item, level);
    if (!pool.length) return list;
    var first = weighted(rand, pool);
    if (first) list.push(first);
    while (rand.nextInt(50) <= level) {
      if (list.length) { var last = list[list.length - 1]; pool = pool.filter(function (e) { return compatible(data, last.id, e.id); }); }
      if (!pool.length) break;
      var more = weighted(rand, pool);
      if (more) list.push(more);
      level = (level / 2) | 0;
    }
    return list;
  }
  /* EnchantmentMenu.getEnchantmentList */
  function enchantList(rand, data, item, seed, slot, cost) {
    rand.setSeed((seed + slot) | 0);
    var list = select(rand, data, item, cost);
    if (item.id === 'minecraft:book' && list.length > 1) list.splice(rand.nextInt(list.length), 1);
    return list;
  }
  /* EnchantmentMenu.slotsChanged: three costs and clues. */
  function offers(data, item, shelves, seed) {
    var rand = new JavaRandom(seed), costs = [0, 1, 2].map(function (j) {
      var c = tableCost(rand, j, shelves, item.enchantability);
      return c < j + 1 ? 0 : c;
    });
    return costs.map(function (cost, j) {
      if (!cost) return { slot: j, cost: 0, clue: null, list: [] };
      var list = enchantList(rand, data, item, seed, j, cost);
      var clue = list.length ? list[rand.nextInt(list.length)] : null;
      return { slot: j, cost: cost, clue: clue, list: list };
    });
  }

  /* ---------------- texts and tooltip lines ---------------- */
  var COLORS = { white: '#FFFFFF', gray: '#AAAAAA', red: '#FF5555', aqua: '#55FFFF', yellow: '#FFFF55', desc: '#AAAAAA' };
  function fmt(s, a) { return String(s).replace('%s', a); }
  function nm(obj, lang) { return obj ? (obj[lang] || obj.en || '') : ''; }
  function S(data, lang, key) { return (data.strings[lang] || {})[key] || (data.strings.en || {})[key] || key; }
  /* Enchantment.getFullname: level only when level != 1 or max level != 1; curses red, others gray. */
  function fullName(data, lang, id, level) {
    var e = byId(data)[id];
    var t = nm(e.name, lang);
    if (level !== 1 || e.maxLevel !== 1) t += ' ' + S(data, lang, 'enchantment.level.' + level);
    return [t, e.curse ? 'red' : 'gray'];
  }
  /* EnchantmentScreen tooltip of a hovered offer. */
  function offerTooltip(data, lang, offer, playerLevel, lapis) {
    var name = fullName(data, lang, offer.clue.id, offer.clue.level);
    var parts = S(data, lang, 'container.enchant.clue').split('%s');
    var lines = [[[parts[0], 'white'], name, [parts.slice(1).join('%s'), 'white']].filter(function (s) { return s[0]; })];
    lines.push([]);
    var need = offer.slot + 1;
    if (playerLevel < offer.cost) {
      lines.push([[fmt(S(data, lang, 'container.enchant.level.requirement'), offer.cost), 'red']]);
    } else {
      lines.push([[need === 1 ? S(data, lang, 'container.enchant.lapis.one') : fmt(S(data, lang, 'container.enchant.lapis.many'), need), lapis >= need ? 'gray' : 'red']]);
      lines.push([[need === 1 ? S(data, lang, 'container.enchant.level.one') : fmt(S(data, lang, 'container.enchant.level.many'), need), 'gray']]);
    }
    return lines;
  }
  /* Item tooltip: name (enchanted book yellow, enchanted common item aqua), then one gray/red line each. */
  function itemTooltip(data, lang, item, list, withDesc) {
    var book = item.id === 'minecraft:book' || item.id === 'minecraft:enchanted_book';
    var lines = [[[book ? S(data, lang, 'item.minecraft.enchanted_book') : nm(item.name, lang), book ? 'yellow' : 'aqua']]];
    list.forEach(function (x) {
      lines.push([fullName(data, lang, x.id, x.level)]);
      var e = byId(data)[x.id];
      if (withDesc && e.desc && nm(e.desc, lang)) lines.push([[nm(e.desc, lang), 'desc']]);
    });
    return lines;
  }

  /* Rune hint: own word list mapped to Standard Galactic-like letters (Unicode), not the game's list or font. */
  var WORDS = 'astral nihil lohen octant chisel ember void echo drift shard vein lumen weave bind mend gleam rune spark stone deep tide frost glint quill moss'.split(' ');
  var SGA = { a: 'ᔑ', b: 'ʖ', c: 'ᓵ', d: '↸', e: 'ᒷ', f: '⎓', g: '⊣', h: '⍑', i: '╎', j: '⋮', k: 'ꖌ', l: 'ꖎ', m: 'ᒲ', n: 'リ', o: 'フ', p: '¡', q: 'ᑑ', r: '∷', s: 'ᓭ', t: 'ℸ', u: '⚍', v: '⍊', w: '∴', x: '/', y: '॥', z: '⨅' };
  function runes(seed, slot) {
    var rand = new JavaRandom((seed * 31 + slot) | 0), n = 2 + rand.nextInt(3), out = [];
    for (var i = 0; i < n; i++) out.push(WORDS[rand.nextInt(WORDS.length)]);
    return out.join(' ').split('').map(function (c) { return SGA[c] || c; }).join('');
  }

  /* ---------------- page ---------------- */
  var STR = {
    en: {
      title: 'Enchanting view', intro: 'The enchanting table and enchanted tooltips as the game draws them, with the game\'s own texts. Hover, focus or tap an offer or a slot. Click an offer to enchant.',
      item: 'Item', shelves: 'Bookshelves', level: 'Experience level', lapis: 'Lapis Lazuli', seed: 'Enchantment seed', reroll: 'Roll again', reset: 'Take the item back', desc: 'Show descriptions (wiki extra)', open: 'Show every tooltip open',
      assumed: 'enchantability assumed ({0}) - the module exports no value', limitsTitle: 'What is simulated and what is not',
      limits: ['Costs, levels, the chosen enchantments and the clue follow the 26.3 formulas and the game\'s random generator, so a given seed always gives the same offers here.', 'The seed of your player and the order in which the game loads the enchantment tags are not known to the wiki; your world shows other offers. Treat the offers as typical examples, not as a prediction.', 'Bookshelves are a number here; in the world they have to stand two blocks away with free air in between.', 'Item names use the "common" rarity (enchanted: aqua); attribute and durability lines are left out.', 'The game window uses game colours (green level numbers on beige) for the comparison; the same numbers are listed below in readable text.'],
      table: 'Offers in plain text', thSlot: 'Row', thCost: 'Level requirement', thPay: 'Pays', thClue: 'Hint shown', thAll: 'All enchantments (hidden in game)', none: '–', emptyRow: 'no offer',
      books: 'Enchanted books of the Simple Mods', booksIntro: 'Each family enchantment at its highest level.', items: 'Enchanted items', itemsIntro: 'The family enchantments an item accepts (anvil, commands); enchantments that exclude each other are split over several items.',
      never: 'Never offered by the table', neverIntro: 'These family enchantments are not in #minecraft:in_enchanting_table: loot, trades, the anvil or commands only.',
      curse: 'curse', treasure: 'treasure', module: 'module', result: 'Enchanted', offer: 'Offer {0}', noTable: 'This item gets no table offers from any enchantment.', missing: 'data/enchanting.js is missing - run python wiki/generate.py.',
      overridden: 'changed by {0}'
    },
    de: {
      title: 'Verzauberungs-Ansicht', intro: 'Der Zaubertisch und verzauberte Tooltips so, wie das Spiel sie zeichnet, mit den Texten des Spiels. Angebot oder Platz überfahren, fokussieren oder antippen. Klick auf ein Angebot verzaubert.',
      item: 'Gegenstand', shelves: 'Bücherregale', level: 'Erfahrungsstufe', lapis: 'Lapislazuli', seed: 'Verzauberungs-Seed', reroll: 'Neu würfeln', reset: 'Gegenstand zurücknehmen', desc: 'Beschreibungen zeigen (Wiki-Zusatz)', open: 'Alle Tooltips offen zeigen',
      assumed: 'Verzauberbarkeit angenommen ({0}) – das Modul exportiert keinen Wert', limitsTitle: 'Was nachgebildet ist und was nicht',
      limits: ['Kosten, Stufen, die gewählten Verzauberungen und der Hinweis folgen den Formeln von 26.3 und dem Zufallsgenerator des Spiels; ein Seed ergibt hier immer dieselben Angebote.', 'Den Seed deines Spielers und die Reihenfolge, in der das Spiel die Verzauberungs-Tags lädt, kennt das Wiki nicht; deine Welt zeigt andere Angebote. Die Angebote sind typische Beispiele, keine Vorhersage.', 'Bücherregale sind hier eine Zahl; in der Welt müssen sie zwei Blöcke entfernt mit Luft dazwischen stehen.', 'Item-Namen mit Seltenheit „gewöhnlich“ (verzaubert: aqua); Attribut- und Haltbarkeitszeilen fehlen.', 'Das Spielfenster nutzt die Spielfarben (grüne Stufenzahl auf Beige) für den Vergleich; dieselben Zahlen stehen unten lesbar als Text.'],
      table: 'Angebote als Text', thSlot: 'Zeile', thCost: 'Erforderliche Stufe', thPay: 'Kosten', thClue: 'Gezeigter Hinweis', thAll: 'Alle Verzauberungen (im Spiel verborgen)', none: '–', emptyRow: 'kein Angebot',
      books: 'Verzauberte Bücher der Simple Mods', booksIntro: 'Jede Verzauberung der Familie auf ihrer höchsten Stufe.', items: 'Verzauberte Gegenstände', itemsIntro: 'Die Familien-Verzauberungen, die ein Gegenstand annimmt (Amboss, Befehle); sich ausschließende Verzauberungen stehen auf mehreren Gegenständen.',
      never: 'Nie am Zaubertisch', neverIntro: 'Diese Familien-Verzauberungen stehen nicht in #minecraft:in_enchanting_table: nur Beute, Handel, Amboss oder Befehle.',
      curse: 'Fluch', treasure: 'Schatz', module: 'Modul', result: 'Verzaubert', offer: 'Angebot {0}', noTable: 'Dieser Gegenstand bekommt am Tisch keine Angebote.', missing: 'data/enchanting.js fehlt – python wiki/generate.py ausführen.',
      overridden: 'geändert von {0}'
    }
  };
  function esc(s) { return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]; }); }
  function tr(lang, key) { var a = arguments, s = (STR[lang] || STR.en)[key]; if (s == null) s = STR.en[key]; return typeof s === 'string' ? s.replace(/\{(\d)\}/g, function (m, i) { return a[+i + 2]; }) : s; }
  function linesHtml(lines) {
    return lines.map(function (line, i) {
      return '<div class="mc-line' + (i === 0 ? ' mc-first' : '') + '">' + (line.length ? line.map(function (seg) { return '<span class="mc-' + seg[1] + '">' + esc(seg[0]) + '</span>'; }).join('') : '&nbsp;') + '</div>';
    }).join('');
  }
  function linesText(lines) { return lines.map(function (l) { return l.map(function (s) { return s[0]; }).join(''); }).filter(Boolean).join(' · '); }

  var CSS = '' +
    '.ench-page{--mcpx:min(3px,calc((100vw - 48px)/178))}' +
    '.ench-ctrl{display:flex;flex-wrap:wrap;gap:.6rem 1rem;align-items:end;margin:.8rem 0}' +
    '.ench-ctrl label{display:flex;flex-direction:column;gap:.2rem;font-size:.9rem;color:var(--text)}' +
    '.ench-ctrl input[type=number]{width:6.5rem}.ench-ctrl select{max-width:100%}.ench-ctrl .ench-chk{flex-direction:row;align-items:center;gap:.4rem}' +
    '.ench-ctrl input,.ench-ctrl select,.ench-ctrl button{font:inherit;padding:.3rem .5rem;border:1px solid var(--border-strong);border-radius:6px;background:var(--panel);color:var(--text)}' +
    '.ench-ctrl button{cursor:pointer}.ench-ctrl button:focus-visible,.ench-gui button:focus-visible,.ench-slot:focus-visible{outline:2px solid var(--accent);outline-offset:2px}' +
    '.ench-gui{position:relative;width:calc(var(--mcpx)*176);height:calc(var(--mcpx)*78);background:#c6c6c6;box-sizing:border-box;border:var(--mcpx) solid;border-color:#fff #555 #555 #fff;box-shadow:0 0 0 var(--mcpx) #000;margin:calc(var(--mcpx)*2) 0 1rem;user-select:none}' +
    '.ench-gui .g-title{position:absolute;left:calc(var(--mcpx)*7);top:calc(var(--mcpx)*4);font-size:calc(var(--mcpx)*8);line-height:1;color:#404040}' +
    '.ench-book{position:absolute;left:calc(var(--mcpx)*14);top:calc(var(--mcpx)*16);width:calc(var(--mcpx)*34);height:calc(var(--mcpx)*22)}' +
    '.ench-book i{position:absolute;top:0;width:48%;height:100%;background:#e8dcc0;border:var(--mcpx) solid #6b3f22;box-sizing:border-box}' +
    '.ench-book i:first-child{left:0;transform:skewY(8deg)}.ench-book i:last-child{right:0;transform:skewY(-8deg)}' +
    '.ench-slot{position:absolute;width:calc(var(--mcpx)*18);height:calc(var(--mcpx)*18);box-sizing:border-box;background:#8b8b8b;border:var(--mcpx) solid;border-color:#373737 #fff #fff #373737;display:flex;align-items:center;justify-content:center;padding:0;cursor:default}' +
    '.ench-slot img{width:calc(var(--mcpx)*16);height:calc(var(--mcpx)*16);image-rendering:pixelated}' +
    '.ench-slot .s-txt{font-size:calc(var(--mcpx)*6);color:#fff;text-align:center;line-height:1;overflow:hidden}' +
    '.ench-slot .s-cnt{position:absolute;right:0;bottom:0;font-size:calc(var(--mcpx)*8);line-height:1;color:#fff;text-shadow:var(--mcpx) var(--mcpx) 0 #3f3f3f}' +
    '.ench-row{position:absolute;left:calc(var(--mcpx)*60);width:calc(var(--mcpx)*108);height:calc(var(--mcpx)*19);box-sizing:border-box;border:var(--mcpx) solid #5e5640;background:#cfc3a0;padding:0;font:inherit;cursor:pointer;display:block;text-align:left}' +
    '.ench-row.r-hi{background:#d9a7d6;border-color:#7a4c78}.ench-row.r-off{background:#74695a;border-color:#3e372d;cursor:default}.ench-row.r-none{background:#74695a;border-color:#3e372d;cursor:default}' +
    '.ench-row .r-orb{position:absolute;left:calc(var(--mcpx)*1);top:calc(var(--mcpx)*1);width:calc(var(--mcpx)*15);height:calc(var(--mcpx)*15);display:flex;flex-wrap:wrap;gap:calc(var(--mcpx)*1);align-content:center;justify-content:center}' +
    '.ench-row .r-orb b{width:calc(var(--mcpx)*4);height:calc(var(--mcpx)*4);background:#80ff20;box-shadow:0 0 0 calc(var(--mcpx)*.8) #2a5e0a}.ench-row.r-off .r-orb b{background:#407f10;box-shadow:0 0 0 calc(var(--mcpx)*.8) #1f3a08}' +
    '.ench-row .r-run{position:absolute;left:calc(var(--mcpx)*19);top:calc(var(--mcpx)*1.5);width:calc(var(--mcpx)*86);font-size:calc(var(--mcpx)*6.5);line-height:1.15;color:#685e4a;overflow:hidden;max-height:calc(var(--mcpx)*15);word-break:break-all}' +
    '.ench-row.r-hi .r-run{color:#ffff80}.ench-row.r-off .r-run{color:#342f25}' +
    '.ench-row .r-num{position:absolute;right:calc(var(--mcpx)*2);bottom:calc(var(--mcpx)*1);font-size:calc(var(--mcpx)*8);line-height:1;color:#80ff20;text-shadow:var(--mcpx) var(--mcpx) 0 #203f08}.ench-row.r-off .r-num{color:#407f10;text-shadow:none}' +
    '.mc-tip{position:fixed;z-index:9999;pointer-events:none;background:#100010;padding:6px 8px;border:2px solid;border-image:linear-gradient(#5000ffcc,#28007fcc) 1;box-shadow:0 0 0 2px #100010;font-size:15px;line-height:1.35;max-width:min(92vw,420px);white-space:normal}' +
    '.mc-tip.mc-static{position:static;display:inline-block;pointer-events:auto;margin:.2rem 0 0;max-width:100%}' +
    '.mc-line.mc-first{margin-bottom:4px}.mc-white{color:#fff;text-shadow:2px 2px 0 #3f3f3f}.mc-gray{color:#aaa;text-shadow:2px 2px 0 #2a2a2a}.mc-red{color:#f55;text-shadow:2px 2px 0 #3f1515}' +
    '.mc-aqua{color:#5ff;text-shadow:2px 2px 0 #153f3f}.mc-yellow{color:#ff5;text-shadow:2px 2px 0 #3f3f15}.mc-desc{color:#aaa;font-style:italic;text-shadow:2px 2px 0 #2a2a2a}' +
    '.ench-grid{display:flex;flex-wrap:wrap;gap:.8rem}.ench-cell{display:flex;flex-direction:column;align-items:flex-start;gap:.25rem;max-width:100%}' +
    '.ench-cell .ench-slot{position:relative}.ench-cell .c-label{font-size:.85rem;color:var(--text);max-width:12rem}.ench-cell .c-label small{color:var(--muted)}' +
    '.ench-open .ench-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(min(100%,17rem),1fr));align-items:start}' +
    '.ench-glint{overflow:hidden}.ench-glint::after{content:"";position:absolute;inset:0;pointer-events:none;background:linear-gradient(115deg,transparent 30%,rgba(168,92,255,.55) 45%,transparent 60%);background-size:300% 300%;mix-blend-mode:screen;animation:ench-glint 3.2s linear infinite}' +
    '@keyframes ench-glint{from{background-position:100% 0}to{background-position:0 100%}}@media (prefers-reduced-motion:reduce){.ench-glint::after{animation:none;background-position:50% 50%}}' +
    '.ench-tbl td,.ench-tbl th{vertical-align:top}';

  function iconHtml(ctx, id, name) {
    var src = ctx.icon ? ctx.icon(id) : null;
    return src ? '<img src="' + esc(src) + '" alt="' + esc(name) + '" onerror="this.replaceWith(Object.assign(document.createElement(\'span\'),{className:\'s-txt\',textContent:this.alt.slice(0,3)}))">' : '<span class="s-txt">' + esc(String(name).slice(0, 3)) + '</span>';
  }

  function mount(el, ctx) {
    var data = ctx.data, lang = ctx.lang === 'de' ? 'de' : 'en';
    if (!data) { el.innerHTML = '<div class="box box-danger">' + esc(tr(lang, 'missing')) + '</div>'; return; }
    var st = ctx.state || (ctx.state = { item: 'minecraft:diamond_pickaxe', shelves: 15, level: 30, lapis: 3, seed: 1337, desc: true, open: false, result: null });
    var items = data.items, E = byId(data);
    var tips = {}, tipN = 0;
    function tip(lines) { var k = 't' + (tipN++); tips[k] = lines; return ' data-tip="' + k + '"'; }
    function item() { return items.filter(function (i) { return i.id === st.item; })[0] || items[0]; }

    function render() {
      tips = {}; tipN = 0;
      var it = item(), off = offers(data, it, st.shelves, st.seed);
      var h = '<div class="ench-page' + (st.open ? ' ench-open' : '') + '"><style>' + CSS + '</style>';
      h += '<h1>' + esc(tr(lang, 'title')) + '</h1><p class="muted">' + esc(tr(lang, 'intro')) + '</p>';
      h += '<div class="ench-ctrl">';
      h += '<label>' + esc(tr(lang, 'item')) + '<select data-k="item">' + items.map(function (i) { return '<option value="' + esc(i.id) + '"' + (i.id === it.id ? ' selected' : '') + '>' + esc(nm(i.name, lang)) + '</option>'; }).join('') + '</select></label>';
      h += '<label>' + esc(tr(lang, 'shelves')) + ' <span data-out="shelves">' + st.shelves + '</span><input type="range" min="0" max="15" step="1" value="' + st.shelves + '" data-k="shelves"></label>';
      h += '<label>' + esc(tr(lang, 'level')) + '<input type="number" min="0" max="999" value="' + st.level + '" data-k="level"></label>';
      h += '<label>' + esc(tr(lang, 'lapis')) + '<input type="number" min="0" max="64" value="' + st.lapis + '" data-k="lapis"></label>';
      h += '<label>' + esc(tr(lang, 'seed')) + '<input type="number" value="' + st.seed + '" data-k="seed"></label>';
      h += '<button type="button" data-act="reroll">' + esc(tr(lang, 'reroll')) + '</button>';
      if (st.result) h += '<button type="button" data-act="reset">' + esc(tr(lang, 'reset')) + '</button>';
      h += '<label class="ench-chk"><input type="checkbox" data-k="desc"' + (st.desc ? ' checked' : '') + '>' + esc(tr(lang, 'desc')) + '</label>';
      h += '<label class="ench-chk"><input type="checkbox" data-k="open"' + (st.open ? ' checked' : '') + '>' + esc(tr(lang, 'open')) + '</label>';
      h += '</div>';
      if (it.assumed) h += '<p class="hint">' + esc(nm(it.name, lang) + ': ' + tr(lang, 'assumed', it.enchantability)) + '</p>';

      /* the game window */
      var shown = st.result ? { id: it.id === 'minecraft:book' ? 'minecraft:enchanted_book' : it.id, name: it.name, list: st.result } : null;
      h += '<div class="ench-gui" role="group" aria-label="' + esc(S(data, lang, 'container.enchant')) + '"><span class="g-title">' + esc(S(data, lang, 'container.enchant')) + '</span><span class="ench-book" aria-hidden="true"><i></i><i></i></span>';
      var slotName = shown ? (shown.id === 'minecraft:enchanted_book' ? S(data, lang, 'item.minecraft.enchanted_book') : nm(it.name, lang)) : nm(it.name, lang);
      var slotTip = shown ? itemTooltip(data, lang, { id: shown.id, name: it.name }, shown.list, st.desc) : [[[nm(it.name, lang), 'white']]];
      h += '<span class="ench-slot' + (shown ? ' ench-glint' : '') + '" tabindex="0" style="left:calc(var(--mcpx)*14);top:calc(var(--mcpx)*46)"' + tip(slotTip) + ' aria-label="' + esc(linesText(slotTip)) + '">' + iconHtml(ctx, shown ? shown.id : it.id, slotName) + '</span>';
      var lapisTip = [[[S(data, lang, 'item.minecraft.lapis_lazuli'), 'white']]];
      h += '<span class="ench-slot" tabindex="0" style="left:calc(var(--mcpx)*34);top:calc(var(--mcpx)*46)"' + (st.lapis ? tip(lapisTip) : '') + ' aria-label="' + esc(S(data, lang, 'item.minecraft.lapis_lazuli') + ' ' + st.lapis) + '">' + (st.lapis ? iconHtml(ctx, 'minecraft:lapis_lazuli', 'Lapis') + (st.lapis > 1 ? '<span class="s-cnt">' + st.lapis + '</span>' : '') : '') + '</span>';
      off.forEach(function (o) {
        var top = 'top:calc(var(--mcpx)*' + (13 + 19 * o.slot) + ')';
        if (st.result || !o.cost || !o.clue) { h += '<span class="ench-row r-none" style="' + top + '" aria-label="' + esc(tr(lang, 'offer', o.slot + 1) + ': ' + tr(lang, 'emptyRow')) + '"></span>'; return; }
        var ok = st.lapis >= o.slot + 1 && st.level >= o.cost;
        var lines = offerTooltip(data, lang, o, st.level, st.lapis);
        h += '<button type="button" class="ench-row' + (ok ? '' : ' r-off') + '" style="' + top + '" data-slot="' + o.slot + '"' + tip(lines) + ' aria-label="' + esc(tr(lang, 'offer', o.slot + 1) + ': ' + linesText(lines)) + '"' + (ok ? '' : ' aria-disabled="true"') + '>' +
          '<span class="r-orb" aria-hidden="true">' + new Array(o.slot + 2).join('<b></b>') + '</span><span class="r-run" aria-hidden="true">' + esc(runes(st.seed, o.slot)) + '</span><span class="r-num" aria-hidden="true">' + o.cost + '</span></button>';
      });
      h += '</div>';
      if (!it.table.length) h += '<p class="hint">' + esc(tr(lang, 'noTable')) + '</p>';

      /* plain text table */
      function payText(n) {
        return (n === 1 ? S(data, lang, 'container.enchant.lapis.one') : fmt(S(data, lang, 'container.enchant.lapis.many'), n)) + ', ' +
          (n === 1 ? S(data, lang, 'container.enchant.level.one') : fmt(S(data, lang, 'container.enchant.level.many'), n));
      }
      h += '<h2>' + esc(tr(lang, 'table')) + '</h2><div class="table-wrap"><table class="table ench-tbl"><thead><tr><th>' + esc(tr(lang, 'thSlot')) + '</th><th>' + esc(tr(lang, 'thCost')) + '</th><th>' + esc(tr(lang, 'thPay')) + '</th><th>' + esc(tr(lang, 'thClue')) + '</th><th>' + esc(tr(lang, 'thAll')) + '</th></tr></thead><tbody>';
      off.forEach(function (o) {
        h += '<tr data-row="' + o.slot + '"><td>' + (o.slot + 1) + '</td><td>' + (o.cost || tr(lang, 'none')) + '</td><td>' + (o.cost ? esc(payText(o.slot + 1)) : tr(lang, 'none')) + '</td><td>' + (o.clue ? esc(fullName(data, lang, o.clue.id, o.clue.level)[0]) : esc(tr(lang, 'emptyRow'))) + '</td><td>' + (o.list.length ? esc(o.list.map(function (x) { return fullName(data, lang, x.id, x.level)[0]; }).join(', ')) : tr(lang, 'none')) + '</td></tr>';
      });
      h += '</tbody></table></div>';
      h += '<details class="box box-info"><summary class="box-title">' + esc(tr(lang, 'limitsTitle')) + '</summary><ul>' + tr(lang, 'limits').map(function (l) { return '<li>' + esc(l) + '</li>'; }).join('') + '</ul></details>';

      /* books and items */
      var fam = data.enchantments.filter(function (e) { return e.family; }).sort(function (a, b) { return nm(a.name, lang).localeCompare(nm(b.name, lang)); });
      function cell(id, name, lines, label) {
        var t = st.open ? '<div class="mc-tip mc-static">' + linesHtml(lines) + '</div>' : '';
        return '<div class="ench-cell"><span class="ench-slot ench-glint" tabindex="0"' + tip(lines) + ' aria-label="' + esc(linesText(lines)) + '">' + iconHtml(ctx, id, name) + '</span><span class="c-label">' + label + '</span>' + t + '</div>';
      }
      function modLabel(e) { return '<small>' + esc(e.module) + (e.id.split(':')[0] === 'minecraft' ? ' · ' + tr(lang, 'overridden', e.module) : '') + (e.curse ? ' · ' + tr(lang, 'curse') : '') + (e.treasure ? ' · ' + tr(lang, 'treasure') : '') + '</small>'; }
      h += '<h2>' + esc(tr(lang, 'books')) + '</h2><p class="muted">' + esc(tr(lang, 'booksIntro')) + '</p><div class="ench-grid" data-sec="books">';
      fam.forEach(function (e) {
        var lines = itemTooltip(data, lang, { id: 'minecraft:enchanted_book' }, [{ id: e.id, level: e.maxLevel }], st.desc);
        h += cell('minecraft:enchanted_book', S(data, lang, 'item.minecraft.enchanted_book'), lines, esc(nm(e.name, lang)) + '<br>' + modLabel(e));
      });
      h += '</div><h2>' + esc(tr(lang, 'items')) + '</h2><p class="muted">' + esc(tr(lang, 'itemsIntro')) + '</p><div class="ench-grid" data-sec="items">';
      items.forEach(function (i) {
        i.family.forEach(function (group) {
          var list = group.map(function (id) { return { id: id, level: E[id].maxLevel }; });
          h += cell(i.id, nm(i.name, lang), itemTooltip(data, lang, i, list, st.desc), esc(nm(i.name, lang)) + '<br><small>' + group.length + ' ×</small>');
        });
      });
      h += '</div>';
      var never = fam.filter(function (e) { return !e.table; });
      if (never.length) {
        h += '<h2>' + esc(tr(lang, 'never')) + '</h2><p class="muted">' + esc(tr(lang, 'neverIntro')) + '</p><ul class="ench-never">' + never.map(function (e) { return '<li>' + esc(nm(e.name, lang)) + ' <code>' + esc(e.id) + '</code> ' + modLabel(e) + '</li>'; }).join('') + '</ul>';
      }
      h += '</div>';
      el.innerHTML = h;
    }

    /* tooltip that follows the pointer, like the game; focus/tap anchors it below the element */
    var box = document.getElementById('mc-tip') || document.body.appendChild(Object.assign(document.createElement('div'), { id: 'mc-tip', className: 'mc-tip', hidden: true }));
    box.setAttribute('role', 'tooltip');
    var pinned = null;
    function show(target, x, y) {
      var lines = tips[target.getAttribute('data-tip')];
      if (!lines) return hide();
      box.innerHTML = linesHtml(lines); box.hidden = false;
      if (x == null) { var r = target.getBoundingClientRect(); x = r.left; y = r.bottom + 4; } else { x += 12; y -= 12; }
      var w = box.offsetWidth, hh = box.offsetHeight;
      box.style.left = Math.max(4, Math.min(x, window.innerWidth - w - 4)) + 'px';
      box.style.top = Math.max(4, Math.min(y, window.innerHeight - hh - 4)) + 'px';
    }
    function hide() { box.hidden = true; pinned = null; }
    if (!window.__enchScroll) { window.__enchScroll = true; window.addEventListener('scroll', function () { var b = document.getElementById('mc-tip'); if (b) b.hidden = true; }, { passive: true }); }
    if (!el._enchBound) {
      el._enchBound = true;
      el.addEventListener('pointermove', function (ev) { if (ev.pointerType === 'touch') return; var t = ev.target.closest('[data-tip]'); if (t && el.contains(t)) show(t, ev.clientX, ev.clientY); else if (!pinned) box.hidden = true; });
      el.addEventListener('pointerleave', function () { if (!pinned) box.hidden = true; });
      el.addEventListener('focusin', function (ev) { var t = ev.target.closest('[data-tip]'); if (t) show(t); });
      el.addEventListener('focusout', function () { if (!pinned) box.hidden = true; });
      /* touch: the first tap shows the tooltip, a second tap on the same offer enchants */
      var touchFirst = false;
      el.addEventListener('pointerdown', function (ev) {
        touchFirst = false;
        if (ev.pointerType !== 'touch') return;
        var t = ev.target.closest('[data-tip]');
        if (t) { touchFirst = !pinned || pinned.getAttribute('data-tip') !== t.getAttribute('data-tip'); show(t); pinned = t; } else hide();
      });
      function onValue(ev) {
        var k = ev.target.getAttribute('data-k'); if (!k) return;
        var live = ev.type === 'input';
        if (live && ev.target.type !== 'range') return; /* numbers apply on change, so typing keeps the caret */
        var v = ev.target.type === 'checkbox' ? ev.target.checked : ev.target.type === 'number' || ev.target.type === 'range' ? (parseInt(ev.target.value, 10) || 0) : ev.target.value;
        if (k === 'shelves') v = Math.max(0, Math.min(15, v));
        if (k === 'level' || k === 'lapis') v = Math.max(0, v);
        if (k === 'seed') v = v | 0;
        if (st[k] === v) return;
        st[k] = v; if (k === 'item') st.result = null;
        hide(); render();
        var again = el.querySelector('[data-k="' + k + '"]'); if (again) again.focus();
      }
      el.addEventListener('input', onValue);
      el.addEventListener('change', onValue);
      el.addEventListener('click', function (ev) {
        var act = ev.target.closest('[data-act]'), row = ev.target.closest('.ench-row[data-slot]');
        if (act) { if (act.getAttribute('data-act') === 'reroll') st.seed = (Math.random() * 4294967296 - 2147483648) | 0; else st.result = null; hide(); render(); return; }
        if (row && touchFirst) { touchFirst = false; return; }
        if (row && !row.hasAttribute('aria-disabled')) {
          var it = item(), o = offers(data, it, st.shelves, st.seed)[+row.getAttribute('data-slot')];
          if (!o.list.length) return;
          st.result = o.list; st.level = Math.max(0, st.level - (o.slot + 1)); st.lapis = Math.max(0, st.lapis - (o.slot + 1));
          st.seed = (Math.random() * 4294967296 - 2147483648) | 0; /* the game rolls a new seed after each enchant */
          hide(); render();
        }
      });
    }
    render();
  }

  return { JavaRandom: JavaRandom, tableCost: tableCost, available: available, select: select, offers: offers, compatible: compatible,
           fullName: fullName, offerTooltip: offerTooltip, itemTooltip: itemTooltip, runes: runes, mount: mount, STR: STR, CSS: CSS };
});
