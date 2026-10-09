/* Dev overlay for the wiki mirror in the Zentrale (only the Zentrale loads this file).
 * Adds selection boxes to texture and item tiles, a bar to send the selection with one optional comment to the
 * dev queue (/api/queue), a request button in the texture zoom and a panel on item pages.
 * The wiki calls window.sbDevHook after rendering; a MutationObserver covers renders before this script loaded. */
(function () {
  if (window.sbDevHook) return;
  var sel = new Map();          // key -> {id, label, path}
  var open = new Set();         // category::id of open/started queue entries
  var mode = '';                // 'textures' | 'items' | ''
  var lastZoom = null;
  var bar = null;

  function el(tag, css, text) {
    var e = document.createElement(tag);
    if (css) e.style.cssText = css;
    if (text) e.textContent = text;
    return e;
  }
  function toast(msg, ok) {
    var t = el('div', 'position:fixed;bottom:64px;left:12px;z-index:30001;padding:8px 12px;border-radius:4px;font-size:13px;color:#fff;background:' + (ok ? '#2f7d3a' : '#b3362b'), msg);
    t.className = ok ? 'dev-toast-ok' : 'dev-toast-err';
    document.body.appendChild(t);
    setTimeout(function () { t.remove(); }, ok ? 2000 : 4000);
  }
  function send(category, kind, targets, comment) {
    if (!targets.length) { toast('Nichts ausgewählt', false); return Promise.resolve(false); }
    return fetch('/api/queue', {
      method: 'POST', credentials: 'same-origin', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ category: category, kind: kind, targets: targets, comment: comment || '' })
    }).then(function (r) {
      if (!r.ok) return r.json().catch(function () { return {}; }).then(function (j) { toast('Fehler: ' + (j.error || r.status), false); return false; });
      toast(targets.length + ' in die KI-Queue gelegt', true);
      loadQueue();
      return true;
    }).catch(function () { toast('Netzwerkfehler', false); return false; });
  }
  function loadQueue() {
    fetch('/api/queue', { credentials: 'same-origin' }).then(function (r) { return r.ok ? r.json() : []; }).then(function (q) {
      open.clear();
      q.forEach(function (e) {
        if (e.status !== 'open' && e.status !== 'started') return;
        (e.targets || []).forEach(function (t) { open.add(e.category + '::' + t.id); });
      });
      scan();
    }).catch(function () {});
  }

  /* ---- tiles ---- */
  /* item id from a wiki link: query mode (?item=ns:id) or hash mode (#/items/ns:id) */
  function itemOf(href) {
    var h = /#\/(?:items|blocks)\/([^?&]+)/.exec(href);
    if (h) return decodeURIComponent(h[1]);
    var q = /[?&]item=([^&#]+)/.exec(href);
    return q ? decodeURIComponent(q[1]) : '';
  }
  function tileInfo(t) {
    if (t.classList.contains('tx-tile')) {
      var id = t.getAttribute('data-tx-id') || t.getAttribute('title') || '';
      return { kind: 'textures', key: 'tx:' + id, target: { id: id, label: t.getAttribute('data-tx-label') || id, path: t.getAttribute('data-tx-path') || '' } };
    }
    var iid = t.getAttribute('data-item-id') || t.getAttribute('data-id') || itemOf(t.getAttribute('href') || '');
    var name = t.querySelector('.itile-name, .row-title');
    return { kind: 'items', key: 'it:' + iid, target: { id: iid, label: name ? name.textContent : iid, path: '' } };
  }
  function decorate(t) {
    var info = tileInfo(t);
    if (!info.target.id) return;
    var cb = t.querySelector(':scope > .dev-check');
    if (!cb) {
      if (getComputedStyle(t).position === 'static') t.style.position = 'relative';
      cb = el('input', 'position:absolute;top:4px;right:4px;z-index:5;width:16px;height:16px;cursor:pointer');
      cb.type = 'checkbox';
      cb.className = 'dev-check';
      cb.title = 'Für die KI-Queue auswählen';
      // the tile itself keeps its own click (zoom / item page)
      cb.addEventListener('click', function (e) {
        e.stopPropagation();
        var i = tileInfo(t);
        if (cb.checked) sel.set(i.key, i.target); else sel.delete(i.key);
        mode = i.kind;
        paint();
      });
      t.appendChild(cb);
    }
    cb.checked = sel.has(info.key);
    t.style.outline = cb.checked ? '2px solid #3b82f6' : '';
    var cat = info.kind === 'textures' ? 'textures' : 'code';
    var badge = t.querySelector(':scope > .dev-badge');
    var queued = open.has(cat + '::' + info.target.id);
    if (queued && !badge) {
      badge = el('span', 'position:absolute;top:4px;left:4px;z-index:5;font-size:10px;padding:1px 4px;border-radius:3px;background:#b45309;color:#fff', 'Queue');
      badge.className = 'dev-badge';
      t.appendChild(badge);
    } else if (!queued && badge) badge.remove();
  }
  function tiles() { return Array.prototype.slice.call(document.querySelectorAll('.tx-tile, a.itile, a.row[href*="item="], a.row[href^="#/items/"], a.row[href^="#/blocks/"]')); }

  /* ---- bar ---- */
  function buildBar() {
    bar = el('div', 'position:fixed;left:0;right:0;bottom:0;z-index:30000;display:flex;flex-wrap:wrap;gap:8px;align-items:center;padding:8px 12px;background:#1d1b18;color:#ece6dc;border-top:1px solid #3a352f;font:13px system-ui,sans-serif');
    bar.className = 'dev-bar';
    var count = el('strong', '', '');
    count.className = 'dev-count';
    var all = el('button', '', 'Alle sichtbaren');
    all.onclick = function () {
      tiles().forEach(function (t) {
        if (t.offsetParent === null) return;
        var i = tileInfo(t);
        if (i.kind === mode || !mode) { mode = i.kind; if (i.target.id) sel.set(i.key, i.target); }
      });
      paint();
    };
    var none = el('button', '', 'Keine');
    none.onclick = function () { sel.clear(); paint(); };
    var comment = el('input', 'flex:1;min-width:180px;padding:5px 8px;border-radius:4px;border:1px solid #3a352f;background:#262320;color:#ece6dc');
    comment.placeholder = 'Kommentar (optional, gilt für alle ausgewählten)';
    comment.className = 'dev-comment';
    function go(category, kind) {
      send(category, kind, Array.from(sel.values()), comment.value).then(function (ok) {
        if (ok) { sel.clear(); comment.value = ''; paint(); }
      });
    }
    var rework = el('button', 'background:#3b82f6;color:#fff;border:0;border-radius:4px;padding:5px 10px', 'Rework anfragen');
    rework.className = 'dev-rework';
    rework.onclick = function () { go('textures', 'texture'); };
    var recipe = el('button', 'background:#3b82f6;color:#fff;border:0;border-radius:4px;padding:5px 10px', 'Rezept ändern');
    recipe.className = 'dev-recipe';
    recipe.onclick = function () { go('code', 'recipe'); };
    var note = el('button', 'background:#6b7280;color:#fff;border:0;border-radius:4px;padding:5px 10px', 'Anmerkung');
    note.className = 'dev-note';
    note.onclick = function () { go('code', 'note'); };
    [count, all, none, comment, rework, recipe, note].forEach(function (c) { bar.appendChild(c); });
    document.body.appendChild(bar);
    document.body.style.paddingBottom = '56px';
  }
  function paint() {
    var list = tiles();
    list.forEach(decorate);
    if (!list.length && !sel.size) { if (bar) bar.style.display = 'none'; return; }
    if (!bar) buildBar();
    if (!mode && list.length) mode = tileInfo(list[0]).kind;
    bar.style.display = 'flex';
    bar.querySelector('.dev-count').textContent = sel.size + ' ausgewählt';
    bar.querySelector('.dev-rework').style.display = mode === 'textures' ? '' : 'none';
    bar.querySelector('.dev-recipe').style.display = mode === 'items' ? '' : 'none';
    bar.querySelector('.dev-note').style.display = mode === 'items' ? '' : 'none';
  }

  /* ---- zoom and item page ---- */
  function zoomButton() {
    var head = document.querySelector('.tx-lb-bar');
    if (!head || head.querySelector('.dev-zoom') || !lastZoom) return;
    var wrap = el('span', 'display:inline-flex;gap:4px;margin-left:8px');
    wrap.className = 'dev-zoom';
    var c = el('input', 'padding:2px 6px;border-radius:4px;border:1px solid #3a352f;background:#262320;color:#ece6dc;font-size:12px');
    c.placeholder = 'Kommentar (optional)';
    var b = el('button', 'background:#3b82f6;color:#fff;border:0;border-radius:4px;padding:2px 8px;font-size:12px', 'Rework anfragen');
    var t = lastZoom;
    b.onclick = function () {
      send('textures', 'texture', [{ id: t.id, label: t.name || t.id, path: t.file || '' }], c.value).then(function (ok) { if (ok) c.value = ''; });
    };
    wrap.appendChild(c); wrap.appendChild(b); head.appendChild(wrap);
  }
  function itemPanel(info) {
    var id = info && info.id ? info.id : itemOf(location.hash || '') || itemOf(location.search || '');
    if (!id) return;
    var h1 = document.querySelector('main h1, h1');
    if (!h1 || document.querySelector('.dev-item[data-id="' + id + '"]')) return;
    document.querySelectorAll('.dev-item').forEach(function (x) { x.remove(); });
    var label = info && info.label ? info.label : h1.textContent;
    var p = el('div', 'margin:10px 0;padding:10px;border:1px solid #3a352f;border-radius:6px;background:#262320;color:#ece6dc;display:flex;flex-wrap:wrap;gap:6px;align-items:center;font:13px system-ui,sans-serif');
    p.className = 'dev-item';
    p.setAttribute('data-id', id);
    p.appendChild(el('strong', '', 'KI-Queue'));
    var c = el('input', 'flex:1;min-width:200px;padding:5px 8px;border-radius:4px;border:1px solid #3a352f;background:#1d1b18;color:#ece6dc');
    c.placeholder = 'Was soll anders werden? (optional)';
    p.appendChild(c);
    [['Rezept ändern', 'recipe'], ['Anmerkung', 'note']].forEach(function (x) {
      var b = el('button', 'background:#3b82f6;color:#fff;border:0;border-radius:4px;padding:5px 10px', x[0]);
      b.onclick = function () {
        send('code', x[1], [{ id: id, label: label, path: '' }], c.value).then(function (ok) { if (ok) c.value = ''; });
      };
      p.appendChild(b);
    });
    h1.insertAdjacentElement('afterend', p);
  }

  var pending = false;
  function scan(info) {
    paint();
    zoomButton();
    itemPanel(info);
  }
  function later() {
    if (pending) return;
    pending = true;
    setTimeout(function () { pending = false; scan(); }, 120);
  }

  window.sbDevHook = function (kind, node, info) {
    if (kind === 'texture-zoom') lastZoom = info || null;
    if (kind === 'item-page') { scan(info); return; }
    later();
  };
  function moved() { sel.clear(); mode = ''; later(); }
  window.addEventListener('hashchange', moved);
  window.addEventListener('popstate', moved);
  new MutationObserver(function (records) {
    for (var i = 0; i < records.length; i++) {
      var n = records[i].target;
      if (bar && bar.contains(n)) continue;
      if (n.nodeType === 1 && n.closest && n.closest('.dev-item, .dev-zoom')) continue;
      later();
      return;
    }
  }).observe(document.body, { childList: true, subtree: true });
  loadQueue();
})();
