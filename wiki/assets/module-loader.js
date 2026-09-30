/* Script loading also works from file:// without fetch or a web server. */
(function () {
  'use strict';
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
  }));
})();
