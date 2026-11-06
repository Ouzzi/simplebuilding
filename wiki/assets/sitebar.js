// Login bar in the wiki header (a-zw06): "Anmelden" when the site has a login, after login also the switch
// Production | Dev and the link to the Zentrale. Asks /api/auth/check of the same host; on hosts without the login
// (GitHub Pages, local file, old hosts) the answer is not JSON 401/200 and nothing is shown. No secrets here.
(function () {
  'use strict';
  if (location.protocol === 'file:') return;
  var header = document.querySelector('header.topbar');
  if (!header || !window.fetch) return;
  var dev = location.pathname === '/dev' || location.pathname.indexOf('/dev/') === 0;
  var T = {
    de: { login: 'Anmelden', logout: 'Abmelden', prod: 'Production', dev: 'Dev', z: 'Zentrale', as: 'Angemeldet als ', sw: 'Wiki-Stand' },
    en: { login: 'Sign in', logout: 'Sign out', prod: 'Production', dev: 'Dev', z: 'Zentrale', as: 'Signed in as ', sw: 'Wiki state' }
  };
  function t(k) { var l = (document.documentElement.lang || 'en').slice(0, 2); return (T[l] || T.en)[k]; }
  function el(tag, cls, text) { var e = document.createElement(tag); if (cls) e.className = cls; if (text) e.textContent = text; return e; }

  var css = el('style');
  css.textContent = '.sitebar{flex:none;display:flex;align-items:center;gap:.4rem}' +
    '.sitebar a.icon-btn{text-decoration:none}' +
    '.sitebar .seg{display:inline-flex;border:1px solid var(--border);border-radius:var(--radius-sm);overflow:hidden}' +
    '.sitebar .seg a{display:inline-flex;align-items:center;height:34px;padding:0 .55rem;color:var(--text);text-decoration:none;font-size:.85rem;background:var(--panel)}' +
    '.sitebar .seg a+a{border-left:1px solid var(--border)}' +
    '.sitebar .seg a[aria-current="page"]{background:var(--accent);color:var(--accent-text);font-weight:600}' +
    '.sitebar form{margin:0;display:inline}' +
    '@media (max-width:1180px){.sitebar{margin-left:auto}}' +
    '@media (max-width:640px){.sitebar .zlink,.sitebar .seg a.prod{display:none}}';
  document.head.appendChild(css);

  var bar = el('div', 'sitebar');
  bar.hidden = true;
  var meta = document.getElementById('topmeta');
  header.insertBefore(bar, meta ? meta.nextSibling : document.getElementById('theme-btn'));

  function here() { return location.pathname + location.search + location.hash; }
  function render(me) {
    bar.textContent = '';
    if (!me) {
      var a = el('a', 'icon-btn', t('login'));
      a.href = '/api/auth/login?next=' + encodeURIComponent(here());
      bar.appendChild(a);
    } else {
      var seg = el('span', 'seg');
      seg.setAttribute('role', 'group');
      seg.setAttribute('aria-label', t('sw'));
      var p = el('a', 'prod', t('prod')); p.href = '/' + location.hash;
      var d = el('a', 'dev', t('dev')); d.href = '/dev/' + location.hash;
      (dev ? d : p).setAttribute('aria-current', 'page');
      seg.appendChild(p); seg.appendChild(d);
      var z = el('a', 'icon-btn zlink', t('z')); z.href = '/zentrale/';
      var f = el('form'); f.method = 'post'; f.action = '/api/auth/logout';
      var b = el('button', 'icon-btn', t('logout')); b.type = 'submit'; b.title = t('as') + me.user;
      f.appendChild(b);
      bar.appendChild(seg); bar.appendChild(z); bar.appendChild(f);
    }
    bar.hidden = false;
  }
  function check() {
    fetch('/api/auth/check', { credentials: 'same-origin', cache: 'no-store' }).then(function (r) {
      var json = (r.headers.get('Content-Type') || '').indexOf('application/json') === 0;
      if (r.status === 200 && json) return r.json().then(render);
      if (r.status === 401 && json) return render(null);
    }).catch(function () { /* no login on this host */ });
  }
  check();
  // labels follow the language switch; the links follow the current page
  var lang = document.getElementById('lang');
  if (lang) lang.addEventListener('change', function () { setTimeout(check, 0); });
  window.addEventListener('hashchange', function () {
    var a = bar.querySelectorAll('a');
    for (var i = 0; i < a.length; i++) {
      if (a[i].classList.contains('prod')) a[i].href = '/' + location.hash;
      else if (a[i].classList.contains('dev')) a[i].href = '/dev/' + location.hash;
      else if (a[i].href.indexOf('/api/auth/login') >= 0) a[i].href = '/api/auth/login?next=' + encodeURIComponent(here());
    }
  });
})();
