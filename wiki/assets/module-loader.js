/* Script loading also works from file:// without fetch or a web server. */
(function () {
  'use strict';
  var loc = window.location;
  /* local = file:// or a local server (generate run, test harness): every picture and every mod is there. */
  var local = loc.protocol === 'file:' || /^(localhost|127\.0\.0\.1)$/.test(loc.hostname);
  var devPath = /^\/dev(\/|$)/.test(loc.pathname);
  /* N113: the public wiki (a host outside /dev/) leaves out the mods that are not part of the release ("release": false
     in modules/modules.json: not loaded, not listed); the dev wiki keeps them (badge "not in the release").
     ?release=1 shows the public view anywhere (checks on the dev wiki). */
  var publicView = /[?&]release=1(&|$)/.test(loc.search) || (!local && !devPath);
  window.WIKI_HIDDEN_MODULES = [];
  if (publicView) window.WIKI_MODULES = (window.WIKI_MODULES || []).filter(function (m) {
    if (m && m.release === false) { window.WIKI_HIDDEN_MODULES.push(m.id); return false; }
    return true;
  });
  window.WIKI_PUBLIC_VIEW = publicView;

  /* N113: git-ignored pictures (wiki/data/local-only.js) exist on a host only if its fill step wrote them
     (wiki/vanilla_textures.py -> data/derived-present.json); the page asks only for those (WIKI_ASSET_OK). */
  var localOnly = {}, present = {};
  var groups = window.WIKI_LOCAL_ONLY || {};
  Object.keys(groups).forEach(function (folder) {
    (groups[folder] || []).forEach(function (name) { localOnly[folder + name + '.png'] = 1; });
  });
  window.WIKI_ASSET_OK = function (path) { return local || !path || !localOnly[path] || !!present[path]; };
  var presentReady = Promise.resolve();
  if (!local && window.fetch && Object.keys(localOnly).length) {
    presentReady = Promise.race([
      window.fetch('data/derived-present.json', { credentials: 'same-origin', cache: 'no-cache' })
        .then(function (r) { return r.ok ? r.json() : null; })
        .then(function (d) { (d && Array.isArray(d.files) ? d.files : []).forEach(function (f) { if (typeof f === 'string') present[f] = 1; }); })
        .catch(function () {}),
      new Promise(function (resolve) { setTimeout(resolve, 3000); })
    ]);
  }

  /* O5b: the public view also leaves out what exists only with a hidden mod: recipes whose load conditions need it
     (generate.py "needsMods") or with a slot that only a hidden mod's item fills, and items/blocks that only such recipes
     make (modules.py mark_needs_mods, e.g. Simple Slabs' chess half slabs) with their block loot; the lists that name
     them (craftedBy, usedIn) and the counts follow. In the release these recipes do not exist - no dead ingredient text. */
  function pruneHidden(all, hidden) {
    var hid = {};
    hidden.forEach(function (id) { hid[id] = 1; });
    function gone(id) { id = String(id || ''); if (id.charAt(0) === '#') id = id.slice(1); return id.indexOf(':') > 0 && !!hid[id.split(':')[0]]; }
    function needs(e) { return Array.isArray(e.needsMods) && e.needsMods.some(function (m) { return hid[m]; }); }
    function groups(r) {
      var out = [];
      if (r.key && typeof r.key === 'object') Object.keys(r.key).forEach(function (k) { out.push(r.key[k]); });
      if (Array.isArray(r.ingredientGroups)) out = out.concat(r.ingredientGroups);
      if (r.slots && typeof r.slots === 'object') Object.keys(r.slots).forEach(function (k) { out.push(r.slots[k]); });
      return out;
    }
    function deadRecipe(r) {
      if (!r || typeof r !== 'object') return false;
      if (needs(r) || (r.result && gone(r.result.id))) return true;
      return groups(r).some(function (g) { g = [].concat(g || []); return g.length > 0 && g.every(gone); });
    }
    Object.keys(all).forEach(function (mod) {
      var d = all[mod];
      if (!d || typeof d !== 'object') return;
      var deadR = {}, deadE = {}, deadLoot = {};
      (d.recipes || []).forEach(function (r) { if (deadRecipe(r)) deadR[r.id] = 1; });
      ['items', 'blocks'].forEach(function (k) {
        (d[k] || []).forEach(function (e) { if (e && needs(e)) { deadE[e.id] = 1; if (e.lootTable) deadLoot[e.lootTable] = 1; } });
      });
      if (!Object.keys(deadR).length && !Object.keys(deadE).length) return;
      if (Array.isArray(d.recipes)) d.recipes = d.recipes.filter(function (r) { return !deadR[r.id]; });
      if (Array.isArray(d.lootTables)) d.lootTables = d.lootTables.filter(function (t) { return !deadLoot[t.id] && !deadE[t.id.replace(':blocks/', ':')]; });
      ['items', 'blocks'].forEach(function (k) {
        if (!Array.isArray(d[k])) return;
        d[k] = d[k].filter(function (e) { return !deadE[e.id]; });
        d[k].forEach(function (e) {
          ['craftedBy', 'usedIn'].forEach(function (f) { if (Array.isArray(e[f])) e[f] = e[f].filter(function (id) { return !deadR[id]; }); });
        });
      });
      if (d.counts && typeof d.counts === 'object') ['recipes', 'items', 'blocks', 'lootTables'].forEach(function (k) { if (Array.isArray(d[k])) d.counts[k] = d[k].length; });
    });
  }
  window.WIKI_PRUNE_HIDDEN = pruneHidden;

  /* O6b: data per mod on demand. data/index.js (wiki/wiki_index.py: counts, names, pictures of every mod) always;
     the full data/<mod>.js only for what the address shows: a mod area (<base><mod>/... or ?mod=) its mod with the
     sub-mods, the root start page nothing (WIKI_LIGHT), every other address all mods (as before). The page asks for
     more with WIKI_LOAD_MODS(ids) / WIKI_LOAD_ALL(); before it reloads a light start page into a full one it sets
     sessionStorage "wiki-load-all" (honoured once here, so a reload can never loop). */
  var MODS = window.WIKI_MODULES || [], BY = {};
  MODS.forEach(function (m) { if (m && /^[a-z][a-z0-9_-]*$/.test(m.id)) BY[m.id] = m; });
  var DATA = window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
  if (window.WIKI_DATA && !DATA.simplebuilding) DATA.simplebuilding = window.WIKI_DATA;
  var hiddenList = window.WIKI_HIDDEN_MODULES, pruned = {}, loading = {};
  function prune(id) {
    if (pruned[id] || !DATA[id]) return;
    pruned[id] = 1;
    if (hiddenList.length) { var one = {}; one[id] = DATA[id]; pruneHidden(one, hiddenList); }
  }
  function script(src) {
    return new Promise(function (resolve, reject) {
      var s = document.createElement('script');
      s.src = src; s.onload = resolve;
      s.onerror = function () { reject(new Error('Missing wiki data: ' + src)); };
      document.head.appendChild(s);
    });
  }
  function loadMod(id) {
    if (!BY[id]) return Promise.resolve();
    if (DATA[id]) { prune(id); return Promise.resolve(); }
    if (!loading[id]) {
      loading[id] = script('data/' + id + '.js' + (BY[id].dataHash ? '?v=' + BY[id].dataHash : '')).then(function () {
        if (id === 'simplebuilding' && window.WIKI_DATA) DATA.simplebuilding = window.WIKI_DATA;
        if (!DATA[id]) throw new Error('Missing wiki data: ' + id);
        prune(id);
      });
      loading[id].catch(function () { delete loading[id]; });
    }
    return loading[id];
  }
  window.WIKI_LOAD_MODS = function (ids) { return Promise.all((ids || []).map(loadMod)).then(function () { return DATA; }); };
  window.WIKI_LOAD_ALL = function () { return window.WIKI_LOAD_MODS(Object.keys(BY)); };
  window.WIKI_HAS_ALL = function () { return Object.keys(BY).every(function (id) { return !!DATA[id]; }); };

  /* The index in the public view: no hidden mods, no entries that exist only with one (needsMods), public counts. */
  function prepareIndex() {
    var ix = window.WIKI_INDEX;
    if (!ix || typeof ix !== 'object' || !ix.mods) { window.WIKI_INDEX = null; return; }
    var hid = {};
    hiddenList.forEach(function (id) { hid[id] = 1; });
    Object.keys(ix.mods).forEach(function (id) {
      var m = ix.mods[id];
      if (!BY[id] || !m) { delete ix.mods[id]; return; }
      if (publicView && m.countsPublic) m.counts = m.countsPublic;
      if (hiddenList.length) ['items', 'blocks'].forEach(function (k) {
        m[k] = (m[k] || []).filter(function (e) { return !(Array.isArray(e[5]) && e[5].some(function (x) { return hid[x]; })); });
      });
    });
  }

  function family(id) {
    var out = [id];
    MODS.forEach(function (c) { if (c && c.parent === id && c.id !== id && BY[c.id]) out = out.concat(family(c.id)); });
    return out;
  }
  /* Mirrors the mod choice of index.html (START): path first, "#/allmods" / tab=allmods = root, then ?mod=.
     wiringexample is never a wiki mod there (PACK_HIDDEN). null = all mods. */
  function startSet() {
    var params = new URLSearchParams(loc.search), rest = loc.pathname;
    var baseEl = document.querySelector('base[data-modpath]');
    if (baseEl && /^https?:$/.test(loc.protocol)) {
      var b = baseEl.getAttribute('href') || '/';
      if (b.charAt(b.length - 1) !== '/') b += '/';
      if (loc.pathname.indexOf(b) === 0) {
        rest = loc.pathname.slice(b.length);
        var seg = rest.split('/')[0];
        if (BY[seg] && seg !== 'wiringexample') return family(seg);
      }
    }
    var hash = loc.hash || '', mod = params.get('mod');
    var allmods = /^#\/allmods/.test(hash) || params.get('tab') === 'allmods';
    if (!allmods && BY[mod] && mod !== 'wiringexample') return family(mod);
    /* light: the root start page without any route (only language, pack filter or ?release=1 in the query) */
    var bare = /(^|\/)(index\.html)?$/.test(rest) && (hash === '' || hash === '#' || hash === '#/');
    params.forEach(function (v, k) { if (k !== 'lang' && k !== 'pack' && k !== 'release') bare = false; });
    return bare ? [] : null;
  }
  var forceAll = false, canStore = false;
  try { forceAll = window.sessionStorage.getItem('wiki-load-all') === '1'; window.sessionStorage.removeItem('wiki-load-all'); canStore = true; } catch (e) { /* no storage: never light */ }
  var start = forceAll ? null : startSet();
  if (start && !start.length && !canStore) start = null;
  window.WIKI_LIGHT = !!start && !start.length;
  window.WIKI_START_MODS = start || Object.keys(BY);

  window.WIKI_READY = Promise.all([
    script('data/index.js').then(prepareIndex, function () { window.WIKI_INDEX = null; }),
    window.WIKI_LOAD_MODS(window.WIKI_START_MODS),
    presentReady
  ]).then(function () {
    /* without the index a light start page has nothing to take its numbers from: everything, as before */
    if (window.WIKI_LIGHT && !window.WIKI_INDEX) { window.WIKI_LIGHT = false; window.WIKI_START_MODS = Object.keys(BY); return window.WIKI_LOAD_ALL(); }
  });
})();
