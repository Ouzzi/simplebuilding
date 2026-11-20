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
  }).concat([presentReady]));
})();
