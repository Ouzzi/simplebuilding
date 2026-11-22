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

  window.WIKI_MODULE_DATA = { simplebuilding: window.WIKI_DATA };
  window.WIKI_READY = Promise.all((window.WIKI_MODULES || []).map(function (module) {
    if (module.id !== 'simplebuilding' && /^[a-z][a-z0-9_-]*$/.test(module.id)) {
      return new Promise(function (resolve, reject) {
        var script = document.createElement('script');
        script.src = 'data/' + module.id + '.js' + (module.dataHash ? '?v=' + module.dataHash : '');
        script.onload = resolve;
        script.onerror = function () { reject(new Error('Missing wiki data: ' + module.id)); };
        document.head.appendChild(script);
      });
    }
    return Promise.resolve();
  }).concat([presentReady])).then(function (v) {
    if (window.WIKI_HIDDEN_MODULES.length) pruneHidden(window.WIKI_MODULE_DATA, window.WIKI_HIDDEN_MODULES);
    return v;
  });
})();
