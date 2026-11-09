/* Dev overlay for the wiki mirror in the Zentrale (only the Zentrale loads this file).
 * Adds selection boxes to texture and item tiles, a bar to send the selection with one optional comment to the
 * dev queue (/api/queue), a request button in the texture zoom and a panel on item pages.
 * The wiki calls window.sbDevHook after rendering; a MutationObserver covers renders before this script loaded.
 * "Bearbeiten" (zoom) and the pencil on texture tiles open the pixel editor of the wiki (wiki/assets/pixeleditor*.js,
 * loaded on first use relative to the wiki page; docs/ai/PLAN-B-PIXELEDITOR-2026-10-21.md).
 * a-wikiux06 (owner 06.11.): "Änderung vorschlagen" on every card, texture and item (text + pasted/uploaded images,
 * POST /api/queue kind "suggestion"), "Bearbeiten" in the card detail asks which texture first, and the Vanilla
 * category: only this overlay loads data/vanilla-textures.js (wiki/vanilla_textures.py, dev checkout only). */
(function () {
  if (window.sbDevHook) return;
  var sel = new Map();          // key -> {id, label, path}
  var open = new Set();         // category::id of open/started queue entries
  var mode = '';                // 'textures' | 'items' | ''
  var lastZoom = null;
  var bar = null;
  var BTN = 'border:0;border-radius:8px;padding:8px 14px;min-height:38px;font:600 13px system-ui,sans-serif;cursor:pointer;';
  var PRIMARY = BTN + 'background:#3b5b8c;color:#fff', SECOND = BTN + 'background:#3a352f;color:#ece6dc', INPUT = 'padding:8px 10px;min-height:38px;border-radius:8px;border:1px solid #4a443c;background:#262320;color:#ece6dc;font:14px system-ui,sans-serif';

  function el(tag, css, text) {
    var e = document.createElement(tag);
    if (css) e.style.cssText = css;
    if (text) e.textContent = text;
    return e;
  }
  function toast(msg, ok) {
    var t = el('div', 'position:fixed;bottom:76px;left:12px;right:12px;max-width:360px;z-index:30001;padding:10px 14px;border-radius:8px;font-size:14px;color:#fff;background:' + (ok ? '#2f7d3a' : '#b3362b') + ';box-shadow:0 4px 14px rgba(0,0,0,.3);font-family:system-ui,sans-serif', msg);
    t.className = ok ? 'dev-toast-ok' : 'dev-toast-err';
    document.body.appendChild(t);
    setTimeout(function () { t.remove(); }, ok ? 2000 : 4000);
  }
  function send(category, kind, targets, comment, extra) {
    if (!targets.length) { toast('Nichts ausgewählt', false); return Promise.resolve(false); }
    var body = { category: category, kind: kind, targets: targets, comment: comment || '' };
    if (extra) Object.keys(extra).forEach(function (k) { body[k] = extra[k]; });
    return fetch('/api/queue', {
      method: 'POST', credentials: 'same-origin', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    }).then(function (r) {
      if (!r.ok) return r.json().catch(function () { return {}; }).then(function (j) { toast('Fehler: ' + (j.error || (r.status === 413 ? 'Bilder zu groß' : r.status)), false); return false; });
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

  /* ---- pixel editor ---- */
  var editorLoad = null;
  function loadScript(src) {
    return new Promise(function (ok, fail) {
      var s = document.createElement('script');
      s.src = src; s.onload = ok; s.onerror = function () { fail(new Error(src)); };
      document.head.appendChild(s);
    });
  }
  function loadEditor() {
    if (window.SBPixelEditor) return Promise.resolve();
    if (!editorLoad) editorLoad = loadScript('assets/pixeleditor-core.js').then(function () { return loadScript('assets/pixeleditor.js'); })
      .catch(function (e) { editorLoad = null; throw e; });
    return editorLoad;
  }
  function textureById(id) {
    var lists = [window.WIKI_TEXTURES && window.WIKI_TEXTURES.textures || [], window.WIKI_VANILLA_TEXTURES && window.WIKI_VANILLA_TEXTURES.textures || []];
    for (var l = 0; l < lists.length; l++) for (var i = 0; i < lists[l].length; i++) if (lists[l][i].id === id) return lists[l][i];
    return null;
  }
  /* Vanilla (dev only, never committed): loaded once when the textures page shows */
  var vanillaLoad = null;
  function loadVanilla() {
    if (vanillaLoad || window.WIKI_VANILLA_TEXTURES) return;
    vanillaLoad = loadScript('data/vanilla-textures.js').then(function () {
      if (window.sbWikiTexturesChanged) window.sbWikiTexturesChanged();
    }).catch(function () { /* not built on this checkout (wiki/vanilla_textures.py) */ });
  }
  /* vanilla-derived textures are built from the client jar, never committed (A8): nothing to edit or save */
  function isDerived(t) { return !!t && /^build\/vanilla-derived\//.test(t.source || ''); }
  function nameOf(t) { return t.name && typeof t.name === 'object' ? (t.name[window.LANG] || t.name.en || t.id) : (t.name || t.id); }
  function editTexture(t) {
    if (!t || !t.file) { toast('Textur nicht gefunden', false); return; }
    if (isDerived(t)) { toast('Aus Vanilla abgeleitet (A8): tools/textures/vanilla_derived.py anpassen', false); return; }
    // Vanilla: a template only - no repository source, so the editor offers export but no "Save to repo"
    // b-texed16: a saved but not yet taken over version opens instead of the old file (else edits would start over)
    var ov = !t.vanilla && saved.get(t.id);
    var pend = ov && ov.url ? ov : null;
    loadEditor().then(function () {
      return window.SBPixelEditor.open({ id: t.id, name: (t.vanilla ? 'Vorlage: ' : '') + nameOf(t), png: pend ? pend.url : t.file,
        layers: pend ? pend.layersUrl : (t.layers || null), from: pend ? 'Deine Speicherung ' + pend.sid + ' (wartet auf Übernahme).' : '',
        source: t.vanilla ? '' : (t.source || ''), anim: t.anim || null });
    }).catch(function (e) { toast('Editor nicht ladbar: ' + (e && e.message || e), false); });
  }

  /* ---- "Änderung vorschlagen": text + images (upload/paste) -> /api/queue kind suggestion ---- */
  var MAX_FILES = 5, MAX_BYTES = 2 * 1024 * 1024, TYPES = { 'image/png': 1, 'image/jpeg': 1, 'image/webp': 1 };
  function suggestForm(object, targets, category) {
    var files = [];
    var f = el('div', 'margin:8px 0;padding:10px;border:1px solid #4a443c;border-left:4px solid #3b5b8c;border-radius:10px;background:#24211e;color:#ece6dc;display:flex;flex-direction:column;gap:8px;font:13px system-ui,sans-serif');
    f.className = 'dev-suggest';
    f.appendChild(el('strong', '', 'Änderung vorschlagen: ' + object.label + (targets.length > 1 ? ' (' + targets.length + ' Texturen)' : '')));
    var ta = el('textarea', INPUT + ';min-height:72px;resize:vertical;width:100%;box-sizing:border-box');
    ta.className = 'dev-suggest-text';
    ta.maxLength = 4000;
    ta.placeholder = 'Was soll anders werden? Bilder hier einfügen (Strg+V) oder unten anhängen.';
    f.appendChild(ta);
    var thumbs = el('div', 'display:flex;flex-wrap:wrap;gap:6px');
    thumbs.className = 'dev-suggest-files';
    var row = el('div', 'display:flex;flex-wrap:wrap;gap:8px;align-items:center');
    var pick = el('input', 'max-width:100%;color:#ece6dc');
    pick.type = 'file'; pick.multiple = true; pick.accept = 'image/png,image/jpeg,image/webp';
    pick.className = 'dev-suggest-pick';
    var info = el('span', 'color:#b9b0a3;font-size:12px', 'PNG, JPEG oder WebP, höchstens ' + MAX_FILES + ' Bilder je 2 MB. Die aktuelle Textur geht als Referenz mit.');
    var go = el('button', PRIMARY, 'Senden');
    go.className = 'dev-suggest-send';
    var cancel = el('button', SECOND, 'Abbrechen');
    function add(list) {
      Array.prototype.forEach.call(list, function (file) {
        if (!TYPES[file.type]) { toast('Nur PNG, JPEG oder WebP: ' + (file.name || file.type), false); return; }
        if (file.size > MAX_BYTES) { toast('Größer als 2 MB: ' + (file.name || ''), false); return; }
        if (files.length >= MAX_FILES) { toast('Höchstens ' + MAX_FILES + ' Bilder', false); return; }
        files.push(file);
      });
      draw();
    }
    function draw() {
      thumbs.innerHTML = '';
      files.forEach(function (file, i) {
        var box = el('span', 'position:relative;display:inline-block');
        var img = el('img', 'width:64px;height:64px;object-fit:contain;background:#8b8b8b;border-radius:6px;image-rendering:pixelated');
        img.src = URL.createObjectURL(file); img.alt = file.name || 'Bild';
        var x = el('button', 'position:absolute;top:-6px;right:-6px;width:22px;height:22px;border-radius:50%;border:0;background:#b3362b;color:#fff;cursor:pointer;font:700 12px system-ui', '✕');
        x.title = 'Entfernen';
        x.onclick = function () { files.splice(i, 1); draw(); };
        box.appendChild(img); box.appendChild(x); thumbs.appendChild(box);
      });
    }
    pick.onchange = function () { add(pick.files); pick.value = ''; };
    f.addEventListener('paste', function (e) {
      var items = e.clipboardData && e.clipboardData.files;
      if (items && items.length) { e.preventDefault(); add(items); }
    });
    function b64(file) {
      return new Promise(function (ok, fail) {
        var r = new FileReader();
        r.onload = function () { ok(String(r.result).split(',', 2)[1] || ''); };
        r.onerror = function () { fail(r.error); };
        r.readAsDataURL(file);
      });
    }
    go.onclick = function () {
      if (!ta.value.trim() && !files.length) { toast('Text oder Bild angeben', false); ta.focus(); return; }
      go.disabled = true;
      Promise.all(files.map(b64)).then(function (data) {
        var att = files.map(function (file, i) { return { name: file.name || ('bild-' + (i + 1)), type: file.type, data: data[i] }; });
        return send(category || 'textures', 'suggestion', targets, ta.value.trim(), { object: object, attachments: att });
      }).then(function (ok) { go.disabled = false; if (ok) f.remove(); }, function () { go.disabled = false; toast('Bild nicht lesbar', false); });
    };
    cancel.onclick = function () { f.remove(); };
    [pick, go, cancel].forEach(function (c) { row.appendChild(c); });
    f.appendChild(thumbs); f.appendChild(row); f.appendChild(info);
    setTimeout(function () { ta.focus(); }, 0);
    return f;
  }
  function targetOf(t) { return { id: t.id, label: nameOf(t) + (t.role ? ' (' + String(t.role).replace(/_/g, ' ') + ')' : ''), path: t.file || '' }; }
  /* the form goes right under the textures (they stay visible); one form per place */
  function showSuggest(after, object, targets, category) {
    var host = after.parentNode;
    var old = host.querySelector(':scope > .dev-suggest');
    if (old) old.remove();
    var form = suggestForm(object, targets, category);
    after.insertAdjacentElement('afterend', form);
    return form;
  }

  /* ---- card detail (wiki devHook 'texture-card') ---- */
  function cardDev(box, info) {
    var slot = box.querySelector('.tx-cd-dev');
    if (!slot || !info) return;
    slot.innerHTML = '';
    var members = info.members || [];
    var bar = el('div', 'display:flex;flex-wrap:wrap;gap:8px;align-items:center');
    var editable = members.filter(function (t) { return !isDerived(t); });
    var edit = el('button', SECOND, '✎ Bearbeiten');
    edit.className = 'dev-card-edit';
    edit.disabled = !editable.length;
    edit.title = editable.length ? 'Erst die Textur wählen, dann Pixeleditor' : 'Nur aus Vanilla abgeleitete Texturen (A8)';
    edit.onclick = function () {
      if (editable.length === 1) { editTexture(editable[0]); return; }
      var old = slot.querySelector('.dev-choose');
      if (old) { old.remove(); return; }
      var list = el('div', 'display:flex;flex-wrap:wrap;gap:6px;margin-top:8px;padding:8px;border:1px solid #4a443c;border-radius:10px;background:#24211e');
      list.className = 'dev-choose';
      list.appendChild(el('strong', 'width:100%;color:#ece6dc;font:600 13px system-ui', 'Welche Textur bzw. Seite bearbeiten?'));
      editable.forEach(function (t) {
        var b = el('button', SECOND + ';display:inline-flex;align-items:center;gap:6px');
        b.className = 'dev-choose-item';
        var img = el('img', 'width:24px;height:24px;object-fit:cover;object-position:top;image-rendering:pixelated;background:#8b8b8b');
        img.src = t.file; img.alt = '';
        b.appendChild(img);
        b.appendChild(document.createTextNode((t.role ? String(t.role).replace(/_/g, ' ') : 'Haupt') + (t.anim ? ' ▶' : '')));
        b.title = t.id;
        b.onclick = function () { list.remove(); editTexture(t); };
        list.appendChild(b);
      });
      bar.insertAdjacentElement('afterend', list);
    };
    var sug = el('button', PRIMARY, 'Änderung vorschlagen');
    sug.className = 'dev-card-suggest';
    sug.onclick = function () { showSuggest(bar, { id: info.key, label: info.name }, members.map(targetOf)); };
    bar.appendChild(edit); bar.appendChild(sug);
    slot.appendChild(bar);
    // per texture: suggest just this one, edit just this one
    box.querySelectorAll('.tx-cd-fig').forEach(function (fig) {
      var acts = fig.querySelector('.tx-cd-acts');
      var t = textureById(fig.getAttribute('data-tx-id'));
      if (!acts || !t || acts.querySelector('.dev-fig-suggest')) return;
      if (!isDerived(t)) {
        var e = el('button', SECOND.replace('min-height:38px', 'min-height:30px'), '✎');
        e.className = 'dev-fig-edit'; e.title = 'Diese Textur im Pixeleditor bearbeiten';
        e.onclick = function () { editTexture(t); };
        acts.appendChild(e);
      }
      var s2 = el('button', SECOND.replace('min-height:38px', 'min-height:30px'), 'Vorschlagen');
      s2.className = 'dev-fig-suggest';
      s2.onclick = function () { showSuggest(bar, { id: info.key, label: info.name }, [targetOf(t)]); };
      acts.appendChild(s2);
    });
    if (info.suggest) sug.click();
  }

  /* ---- b-texed16: saved textures show at once, with their state (docs/ai/PLAN-B-TEXED16-2026-11-09.md) ----
   * saved: texture id -> {sid, status, label, url (blob PNG, only while not taken over), layersUrl, bust}.
   * Data: GET /api/textures (newest first; "adopted" = the save id is in the commit history of the Wiki-Dev checkout,
   * computed by devqueue.py) and the editor's event "sb-texture-saved" right after a save. */
  var saved = new Map();
  var BUST_DAYS = 14;
  function statusOf(s) {
    if (s.adopted) return { kind: 'done', label: 'übernommen in ' + (s.adopted.bundle ? 'Bündel ' + String(s.adopted.bundle).replace(/^b-bundle/, '') + ' (' + s.adopted.commit + ')' : s.adopted.commit) };
    if (s.state === 'failed') return { kind: 'fail', label: 'Übernahme fehlgeschlagen: ' + (s.note || '') };
    if (s.state === 'ready') return { kind: 'wait', label: 'auf Branch ' + (s.branch || s.note || '') + ' – wartet auf Bündel' };
    return { kind: 'wait', label: 'gespeichert – wartet auf Übernahme' };
  }
  function blobUrl(bytes, type) { return URL.createObjectURL(new Blob([bytes], { type: type })); }
  function setSaved(id, entry) {
    var old = saved.get(id);
    if (old && old.url && old.url !== entry.url) { URL.revokeObjectURL(old.url); if (old.layersUrl) URL.revokeObjectURL(old.layersUrl); }
    saved.set(id, entry);
    document.querySelectorAll('.dev-zoom').forEach(function (z) { z.remove(); });   // rebuilt with the new state
    later();
  }
  /* a pending save: compose its layers with the editor core (same result as the PNG the pickup will commit) */
  function pendingFromRecord(rec, st) {
    return (window.SBPixelCore ? Promise.resolve() : loadScript('assets/pixeleditor-core.js')).then(function () {
      var C = window.SBPixelCore, m = C.fromDoc(JSON.parse(rec.layers));
      return C.pngEncode(m.w, m.h, C.compose(m)).then(function (png) {
        setSaved(rec.texture, { sid: rec.sid, status: st.kind, label: st.label, url: blobUrl(png, 'image/png'), layersUrl: blobUrl(rec.layers, 'application/json') });
      });
    });
  }
  function loadSaves() {
    fetch('/api/textures', { credentials: 'same-origin' }).then(function (r) { return r.ok ? r.json() : []; }).then(function (list) {
      var seen = new Set(), now = Date.now();
      (Array.isArray(list) ? list : []).forEach(function (s) {
        if (!s || !s.texture || seen.has(s.texture)) return;   // newest save per texture only
        seen.add(s.texture);
        var st = statusOf(s), cur = saved.get(s.texture);
        if (cur && cur.url && cur.sid === s.sid && st.kind === 'wait') { cur.label = st.label; return; }
        if (st.kind === 'done') {
          // taken over: the checkout PNG is the new one; ?v=<sid> makes the browser fetch it instead of the cached old one
          setSaved(s.texture, { sid: s.sid, status: 'done', label: st.label, bust: now - (s.createdAt || 0) < BUST_DAYS * 864e5 });
        } else if (st.kind === 'fail') setSaved(s.texture, { sid: s.sid, status: 'fail', label: st.label });
        else fetch('/api/textures/' + encodeURIComponent(s.sid), { credentials: 'same-origin' }).then(function (r) { return r.ok ? r.json() : null; })
          .then(function (rec) { if (rec && typeof rec.layers === 'string') return pendingFromRecord(rec, st); })
          .catch(function () { /* stays as it is */ });
      });
    }).catch(function () {});
  }
  window.addEventListener('sb-texture-saved', function (e) {
    var d = e.detail || {};
    if (!d.id || !d.png) return;
    setSaved(d.id, { sid: d.sid || '', status: 'wait', label: 'gespeichert – wartet auf Übernahme', url: blobUrl(d.png, 'image/png'),
      layersUrl: d.layers ? blobUrl(d.layers, 'application/json') : null });
    toast('Gespeichert – die Seite zeigt jetzt deine Fassung', true);
  });
  /* swap every <img> of a saved texture (tiles, zoom, cards, item pages) and keep the original in data-dev-src */
  function swapImages() {
    if (!saved.size) return;
    var files = {};
    saved.forEach(function (v, id) {
      var t = textureById(id);
      if (!t || !t.file) return;
      var src = v.url || (v.bust ? t.file + (t.file.indexOf('?') < 0 ? '?' : '&') + 'v=' + v.sid : '');
      if (src) files[t.file] = src;
    });
    document.querySelectorAll('img').forEach(function (img) {
      var orig = img.getAttribute('data-dev-src') || img.getAttribute('src');
      var want = files[orig];
      if (!want || img.getAttribute('src') === want) return;
      img.setAttribute('data-dev-src', orig);
      img.setAttribute('src', want);
    });
  }
  function savedBadge(t, id) {
    var v = saved.get(id), b = t.querySelector(':scope > .dev-saved');
    if (!v) { if (b) b.remove(); return; }
    if (!b) {
      b = el('span', 'position:absolute;bottom:4px;left:4px;z-index:5;max-width:calc(100% - 44px);overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font:700 10px system-ui,sans-serif;padding:2px 6px;border-radius:99px;color:#fff');
      b.className = 'dev-saved';
      t.appendChild(b);
    }
    // white text: #9a6200 5.1:1, #2f7d3a 5.1:1, #b3362b 6.0:1 (>= 4.5:1)
    b.style.background = v.status === 'done' ? '#2f7d3a' : v.status === 'fail' ? '#b3362b' : '#9a6200';
    b.textContent = v.status === 'done' ? 'übernommen' : v.status === 'fail' ? 'Fehler' : 'gespeichert';
    b.title = v.label;
    b.setAttribute('aria-label', v.label);
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
      cb = el('input', 'position:absolute;top:4px;right:4px;z-index:5;width:20px;height:20px;cursor:pointer;accent-color:#3b5b8c');
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
    if (info.kind === 'textures' && !t.querySelector(':scope > .dev-edit') && !isDerived(textureById(info.target.id))) {
      var pen = el('span', 'position:absolute;bottom:4px;right:4px;z-index:5;width:30px;height:30px;display:flex;align-items:center;justify-content:center;border-radius:8px;background:#3b5b8c;color:#fff;font:700 16px system-ui,sans-serif;cursor:pointer', '✎');
      pen.className = 'dev-edit';
      pen.setAttribute('role', 'button');
      pen.title = 'Im Pixeleditor bearbeiten';
      // a span, not a button: the tile itself is a <button>
      pen.addEventListener('click', function (e) { e.stopPropagation(); e.preventDefault(); editTexture(textureById(info.target.id)); });
      t.appendChild(pen);
    }
    if (info.kind === 'textures') savedBadge(t, info.target.id);
    cb.checked = sel.has(info.key);
    t.style.outline = cb.checked ? '3px solid #3b5b8c' : '';
    t.style.outlineOffset = cb.checked ? '1px' : '';
    var cat = info.kind === 'textures' ? 'textures' : 'code';
    var badge = t.querySelector(':scope > .dev-badge');
    var queued = open.has(cat + '::' + info.target.id);
    if (queued && !badge) {
      badge = el('span', 'position:absolute;top:4px;left:4px;z-index:5;font:700 10px system-ui,sans-serif;padding:2px 6px;border-radius:99px;background:#9a6200;color:#fff', 'Queue');
      badge.className = 'dev-badge';
      t.appendChild(badge);
    } else if (!queued && badge) badge.remove();
  }
  function cards() {
    document.querySelectorAll('.tx-card').forEach(function (card) {
      var head = card.querySelector('.tx-card-h');
      if (!head || head.querySelector('.dev-card-btn')) return;
      var key = card.getAttribute('data-tx-card-key');
      var b = el('button', 'align-self:flex-start;margin-top:4px;border:0;border-radius:6px;padding:4px 10px;min-height:28px;font:600 12px system-ui,sans-serif;cursor:pointer;background:#3b5b8c;color:#fff', 'Änderung vorschlagen');
      b.className = 'dev-card-btn';
      b.onclick = function (e) { e.stopPropagation(); if (window.sbTx) window.sbTx.openCard(key, { suggest: true }); };
      head.appendChild(b);
    });
  }
  function tiles() { return Array.prototype.slice.call(document.querySelectorAll('.tx-tile, a.itile, a.row[href*="item="], a.row[href^="#/items/"], a.row[href^="#/blocks/"]')); }

  /* ---- bar ---- */
  function buildBar() {
    bar = el('div', 'position:fixed;left:0;right:0;bottom:0;z-index:30000;display:flex;flex-wrap:wrap;gap:8px;align-items:center;padding:10px 12px calc(10px + env(safe-area-inset-bottom));background:#1a1816;color:#ece6dc;border-top:1px solid #3a352f;box-shadow:0 -4px 16px rgba(0,0,0,.25);font:13px system-ui,sans-serif');
    bar.className = 'dev-bar';
    var count = el('strong', 'padding:4px 10px;border-radius:99px;background:#25324a;color:#8fb0e8;white-space:nowrap', '');
    count.className = 'dev-count';
    var all = el('button', SECOND, 'Alle sichtbaren');
    all.onclick = function () {
      tiles().forEach(function (t) {
        if (t.offsetParent === null) return;
        var i = tileInfo(t);
        if (i.kind === mode || !mode) { mode = i.kind; if (i.target.id) sel.set(i.key, i.target); }
      });
      paint();
    };
    var none = el('button', SECOND, 'Keine');
    none.onclick = function () { sel.clear(); paint(); };
    var comment = el('input', 'flex:1 1 200px;min-width:0;' + INPUT);
    comment.placeholder = 'Kommentar (optional, gilt für alle ausgewählten)';
    comment.className = 'dev-comment';
    function go(category, kind) {
      send(category, kind, Array.from(sel.values()), comment.value).then(function (ok) {
        if (ok) { sel.clear(); comment.value = ''; paint(); }
      });
    }
    var rework = el('button', PRIMARY, 'Rework anfragen');
    rework.className = 'dev-rework';
    rework.onclick = function () { go('textures', 'texture'); };
    var recipe = el('button', PRIMARY, 'Rezept ändern');
    recipe.className = 'dev-recipe';
    recipe.onclick = function () { go('code', 'recipe'); };
    var note = el('button', SECOND, 'Anmerkung');
    note.className = 'dev-note';
    note.onclick = function () { go('code', 'note'); };
    [count, all, none, comment, rework, recipe, note].forEach(function (c) { bar.appendChild(c); });
    document.body.appendChild(bar);
    document.body.style.paddingBottom = '120px';
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
    var head = document.querySelector('.tx-lb:not(.tx-cd) .tx-lb-bar');
    if (!head || head.querySelector('.dev-zoom') || !lastZoom) return;
    var wrap = el('span', 'display:inline-flex;flex-wrap:wrap;gap:6px;margin-left:8px');
    wrap.className = 'dev-zoom';
    var t = lastZoom;
    var b = el('button', PRIMARY.replace('min-height:38px', 'min-height:32px'), 'Änderung vorschlagen');
    b.className = 'dev-zoom-suggest';
    b.onclick = function () {
      var panes = document.querySelector('.tx-lb:not(.tx-cd) .tx-lb-panes');
      if (panes) showSuggest(panes, { id: t.group || t.id, label: nameOf(t) }, [targetOf(t)]);
    };
    var edit = el('button', SECOND.replace('min-height:38px', 'min-height:32px'), '✎ Bearbeiten');
    edit.className = 'dev-edit';
    edit.title = 'Im Pixeleditor bearbeiten (Ebenen)';
    edit.onclick = function () { editTexture(t); };
    if (!isDerived(t)) wrap.appendChild(edit);
    wrap.appendChild(b);
    var sv = saved.get(t.id);
    if (sv) {
      var line = el('span', 'align-self:center;padding:4px 10px;border-radius:99px;font:600 12px system-ui,sans-serif;color:#fff;background:' + (sv.status === 'done' ? '#2f7d3a' : sv.status === 'fail' ? '#b3362b' : '#9a6200'), sv.label);
      line.className = 'dev-saved-zoom';
      wrap.appendChild(line);
    }
    head.appendChild(wrap);
  }
  function itemPanel(info) {
    var id = info && info.id ? info.id : itemOf(location.hash || '') || itemOf(location.search || '');
    if (!id) return;
    var h1 = document.querySelector('main h1, h1');
    if (!h1 || document.querySelector('.dev-item[data-id="' + id + '"]')) return;
    document.querySelectorAll('.dev-item').forEach(function (x) { x.remove(); });
    var label = info && info.label ? info.label : h1.textContent;
    var p = el('div', 'margin:12px 0;padding:12px;border:1px solid #3a352f;border-left:4px solid #3b5b8c;border-radius:10px;background:#24211e;color:#ece6dc;display:flex;flex-wrap:wrap;gap:8px;align-items:center;font:13px system-ui,sans-serif');
    p.className = 'dev-item';
    p.setAttribute('data-id', id);
    p.appendChild(el('strong', '', 'KI-Queue'));
    var c = el('input', 'flex:1 1 200px;min-width:0;' + INPUT);
    c.placeholder = 'Was soll anders werden? (optional)';
    p.appendChild(c);
    [['Rezept ändern', 'recipe'], ['Anmerkung', 'note']].forEach(function (x) {
      var b = el('button', x[1] === 'recipe' ? PRIMARY : SECOND, x[0]);
      b.onclick = function () {
        send('code', x[1], [{ id: id, label: label, path: '' }], c.value).then(function (ok) { if (ok) c.value = ''; });
      };
      p.appendChild(b);
    });
    var sb = el('button', SECOND, 'Änderung vorschlagen');
    sb.className = 'dev-item-suggest';
    sb.onclick = function () {
      // the item's textures when the texture index is loaded (the current texture goes along as reference)
      var all = window.WIKI_TEXTURES && window.WIKI_TEXTURES.textures || [];
      var own = all.filter(function (t) { return t.item === id; }).map(targetOf);
      showSuggest(p, { id: id, label: label }, own.length ? own : [{ id: id, label: label, path: '' }], own.length ? 'textures' : 'code');
    };
    p.appendChild(sb);
    h1.insertAdjacentElement('afterend', p);
  }

  var pending = false;
  function scan(info) {
    swapImages();
    paint();
    cards();
    if (document.getElementById('tx-results')) loadVanilla();
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
    if (kind === 'texture-card') { cardDev(node, info); return; }
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
      if (n.nodeType === 1 && n.closest && n.closest('.dev-item, .dev-zoom, .pe-root, .dev-suggest, .tx-cd')) continue;
      later();
      return;
    }
  }).observe(document.body, { childList: true, subtree: true });
  loadQueue();
  loadSaves();
})();
