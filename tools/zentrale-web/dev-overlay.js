(function () {
  if (window.sbDevHook) {
    return;
  }
  const queue = new Map();
  let queueLoaded = false;

  function apiBase() {
    return "/api";
  }

  function showError(msg) {
    try {
      const el = document.createElement("div");
      el.style.cssText = "position:fixed;bottom:10px;left:10px;background:#b3362b;color:#fff;padding:8px 12px;border-radius:4px;z-index:30000;font-size:13px;";
      el.textContent = msg;
      document.body.appendChild(el);
      setTimeout(() => {
        el.remove();
      }, 3000);
    } catch (e) {
    }
  }

  function showMsg(msg) {
    try {
      const el = document.createElement("div");
      el.style.cssText = "position:fixed;bottom:10px;left:10px;background:#2f7d3a;color:#fff;padding:8px 12px;border-radius:4px;z-index:30000;font-size:13px;";
      el.textContent = msg;
      document.body.appendChild(el);
      setTimeout(() => {
        el.remove();
      }, 2000);
    } catch (e) {
    }
  }

  async function loadQueue() {
    try {
      const res = await fetch(apiBase() + "/queue", { credentials: "same-origin" });
      if (!res.ok) {
        return;
      }
      const data = await res.json();
      queue.clear();
      for (const item of data) {
        if (item.status === "open" || item.status === "started") {
          for (const t of item.targets || []) {
            const key = item.category + "::" + (t.id || t.path);
            queue.set(key, item);
          }
        }
      }
      queueLoaded = true;
      document.querySelectorAll("[data-dev-badge]").forEach((b) => b.remove());
      document.querySelectorAll(".tx-tile, .itile, [data-item-id]").forEach((el) => markOpen(el));
    } catch (e) {
      showError("Queue-Fehler");
    }
  }

  function markOpen(el) {
    if (!queueLoaded) return;
    let key = null;
    if (el.classList && el.classList.contains("tx-tile")) {
      key = "textures::" + (el.getAttribute("data-tx-id") || el.getAttribute("data-tx-path") || "");
    } else if (el.hasAttribute("data-item-id")) {
      key = "code::" + el.getAttribute("data-item-id");
    } else if (el.classList && el.classList.contains("itile")) {
      const id = el.getAttribute("data-id");
      if (id) key = "code::" + id;
    }
    if (!key || !queue.get(key)) return;
    const badge = document.createElement("span");
    badge.setAttribute("data-dev-badge", "1");
    badge.textContent = "Angefragt";
    badge.style.cssText = "position:absolute;top:4px;left:4px;background:#a86b00;color:#fff;font-size:10px;padding:1px 4px;border-radius:3px;z-index:10;";
    el.style.position = el.style.position || "relative";
    el.appendChild(badge);
  }

  function buildBar(selected, onAction) {
    const bar = document.createElement("div");
    bar.className = "dev-queue-bar";
    bar.style.cssText = "position:fixed;bottom:0;left:0;right:0;background:#1d1b18;color:#ece6dc;border-top:1px solid #3a352f;display:flex;gap:8px;align-items:center;flex-wrap:wrap;padding:8px 12px;z-index:20000;font-size:13px;";
    const cnt = document.createElement("span");
    cnt.textContent = selected.size + " ausgewählt";
    bar.appendChild(cnt);
    const selAll = document.createElement("button");
    selAll.textContent = "Alle sichtbaren auswählen";
    selAll.onclick = () => onAction("selectAll");
    bar.appendChild(selAll);
    const none = document.createElement("button");
    none.textContent = "Keine";
    none.onclick = () => onAction("none");
    bar.appendChild(none);
    const ta = document.createElement("input");
    ta.type = "text";
    ta.placeholder = "Kommentar (optional)";
    ta.style.cssText = "flex:1;min-width:200px;padding:4px 6px;border-radius:4px;border:1px solid #3a352f;background:#262320;color:#ece6dc;";
    bar.appendChild(ta);
    const btn = document.createElement("button");
    btn.textContent = "Rework anfragen";
    btn.onclick = () => onAction("submit", ta.value);
    bar.appendChild(btn);
    return { bar, ta };
  }

  function hookTextures() {
    const tiles = document.querySelectorAll(".tx-tile");
    const selected = new Set();
    let bar = null;
    function refresh() {
      tiles.forEach((t) => {
        const has = selected.has(t);
        t.style.outline = has ? "2px solid #3b5b8c" : "";
        t.style.outlineOffset = has ? "2px" : "";
      });
      if (bar) bar.querySelector("span").textContent = selected.size + " ausgewählt";
    }
    const { bar: b, ta } = buildBar(selected, async (action, val) => {
      if (action === "selectAll") {
        tiles.forEach((t) => selected.add(t));
      } else if (action === "none") {
        selected.clear();
      } else if (action === "submit") {
        if (selected.size === 0) return;
        const targets = [];
        for (const t of selected) {
          const id = t.getAttribute("data-tx-id") || t.getAttribute("data-tx-path") || t.getAttribute("title") || "";
          const label = t.querySelector(".tx-name")?.textContent || id;
          const path = t.getAttribute("data-tx-path") || "";
          targets.push({ id, label, path });
        }
        try {
          const res = await fetch(apiBase() + "/queue", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "same-origin",
            body: JSON.stringify({ category: "textures", kind: "texture", targets, comment: val || "" })
          });
          if (!res.ok) {
            const j = await res.json().catch(() => ({}));
            showError("Fehler: " + (j.error || res.status));
            return;
          }
          showMsg("Gesendet");
          selected.clear();
          refresh();
          loadQueue();
        } catch (e) {
          showError("Netzwerkfehler");
        }
      }
      refresh();
    });
    bar = b;
    document.body.appendChild(bar);
    tiles.forEach((t) => {
      t.addEventListener("click", (e) => {
        if (e.target.tagName === "INPUT" || e.target.closest("input")) return;
        const cb = t.querySelector(".dev-tx-check");
        if (cb && e.target === cb) return;
        selected.add(t);
        refresh();
      });
      const cb = document.createElement("input");
      cb.type = "checkbox";
      cb.className = "dev-tx-check";
      cb.style.cssText = "position:absolute;top:6px;right:6px;z-index:10;";
      cb.onchange = () => {
        if (cb.checked) selected.add(t); else selected.delete(t);
        refresh();
      };
      t.style.position = t.style.position || "relative";
      t.appendChild(cb);
    });
    refresh();
  }

  function hookTextureZoom(info) {
    const lb = document.querySelector(".tx-lb, .tx-lb-box");
    if (!lb) return;
    const box = document.querySelector(".tx-lb-box") || lb;
    if (box.querySelector(".dev-tx-zoom-btn")) return;
    const ta = document.createElement("input");
    ta.type = "text";
    ta.placeholder = "Kommentar";
    ta.style.cssText = "margin-left:6px;padding:2px 4px;border-radius:4px;border:1px solid #3a352f;background:#262320;color:#ece6dc;font-size:12px;";
    const btn = document.createElement("button");
    btn.className = "dev-tx-zoom-btn";
    btn.textContent = "Rework anfragen";
    btn.style.cssText = "margin-left:4px;padding:2px 6px;border-radius:4px;background:#3b5b8c;color:#fff;border:0;font-size:12px;cursor:pointer;";
    btn.onclick = async () => {
      const id = info && (info.id || (typeof info === "string" ? info : "")) || "";
      const label = document.querySelector(".tx-lb-title")?.textContent || id;
      try {
        const res = await fetch(apiBase() + "/queue", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "same-origin",
          body: JSON.stringify({ category: "textures", kind: "texture", targets: [{ id: String(id), label: String(label), path: info && info.file ? String(info.file) : "" }], comment: ta.value || "" })
        });
        if (!res.ok) {
          const j = await res.json().catch(() => ({}));
          showError("Fehler: " + (j.error || res.status));
          return;
        }
        showMsg("Gesendet");
      } catch (e) {
        showError("Netzwerkfehler");
      }
    };
    const head = document.querySelector(".tx-lb-head") || box;
    head.appendChild(ta);
    head.appendChild(btn);
  }

  function hookItemPage(info) {
    const container = document.querySelector("main, article, .hero-card")?.parentElement || document.body;
    if (container.querySelector(".dev-item-actions")) return;
    const wrap = document.createElement("div");
    wrap.className = "dev-item-actions";
    wrap.style.cssText = "margin:10px 0;padding:10px;background:#262320;border:1px solid #3a352f;border-radius:6px;";
    const id = info && info.id ? info.id : (location.hash.split("/").pop() || "");
    const label = info && info.label ? info.label : document.querySelector("h1")?.textContent || id;
    wrap.innerHTML = "<div style='margin-bottom:6px;font-weight:600'>KI-Queue</div>";
    const taR = document.createElement("input");
    taR.type = "text";
    taR.placeholder = "Kommentar für Rezept";
    taR.style.cssText = "width:100%;margin-bottom:6px;padding:4px 6px;border-radius:4px;border:1px solid #3a352f;background:#1d1b18;color:#ece6dc;";
    const btnR = document.createElement("button");
    btnR.textContent = "Rezept ändern";
    btnR.onclick = async () => {
      try {
        const res = await fetch(apiBase() + "/queue", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "same-origin",
          body: JSON.stringify({ category: "code", kind: "recipe", targets: [{ id: String(id), label: String(label), path: "" }], comment: taR.value || "" })
        });
        if (!res.ok) {
          const j = await res.json().catch(() => ({}));
          showError("Fehler: " + (j.error || res.status));
          return;
        }
        showMsg("Gesendet");
      } catch (e) {
        showError("Netzwerkfehler");
      }
    };
    const taN = document.createElement("input");
    taN.type = "text";
    taN.placeholder = "Kommentar für Anmerkung";
    taN.style.cssText = "width:100%;margin-bottom:6px;padding:4px 6px;border-radius:4px;border:1px solid #3a352f;background:#1d1b18;color:#ece6dc;";
    const btnN = document.createElement("button");
    btnN.textContent = "Anmerkung";
    btnN.onclick = async () => {
      try {
        const res = await fetch(apiBase() + "/queue", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "same-origin",
          body: JSON.stringify({ category: "code", kind: "note", targets: [{ id: String(id), label: String(label), path: "" }], comment: taN.value || "" })
        });
        if (!res.ok) {
          const j = await res.json().catch(() => ({}));
          showError("Fehler: " + (j.error || res.status));
          return;
        }
        showMsg("Gesendet");
      } catch (e) {
        showError("Netzwerkfehler");
      }
    };
    wrap.appendChild(btnR);
    wrap.appendChild(taR);
    wrap.appendChild(btnN);
    wrap.appendChild(taN);
    const hero = document.querySelector(".hero-card") || document.querySelector("h1");
    if (hero && hero.parentElement) {
      hero.parentElement.insertBefore(wrap, hero.nextSibling);
    } else {
      container.insertBefore(wrap, container.firstChild);
    }
  }

  function hookItems() {
    const tiles = document.querySelectorAll(".itile, [data-item-id]");
    if (tiles.length === 0) return;
    const selected = new Set();
    let bar = null;
    function refresh() {
      tiles.forEach((t) => {
        const has = selected.has(t);
        t.style.outline = has ? "2px solid #3b5b8c" : "";
        t.style.outlineOffset = has ? "2px" : "";
      });
      if (bar) bar.querySelector("span").textContent = selected.size + " ausgewählt";
    }
    const { bar: b, ta } = buildBar(selected, async (action, val) => {
      if (action === "selectAll") {
        tiles.forEach((t) => selected.add(t));
      } else if (action === "none") {
        selected.clear();
      } else if (action === "submit") {
        if (selected.size === 0) return;
        const targets = [];
        for (const t of selected) {
          const id = t.getAttribute("data-id") || t.getAttribute("data-item-id") || "";
          const label = t.querySelector(".itile-name, .name")?.textContent || id;
          const path = "";
          targets.push({ id, label, path });
        }
        try {
          const res = await fetch(apiBase() + "/queue", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "same-origin",
            body: JSON.stringify({ category: "code", kind: "recipe", targets, comment: val || "" })
          });
          if (!res.ok) {
            const j = await res.json().catch(() => ({}));
            showError("Fehler: " + (j.error || res.status));
            return;
          }
          showMsg("Gesendet");
          selected.clear();
          refresh();
          loadQueue();
        } catch (e) {
          showError("Netzwerkfehler");
        }
      }
      refresh();
    });
    bar = b;
    const actions = document.createElement("div");
    actions.style.cssText = "margin-left:8px;display:flex;gap:6px;";
    const btnA = document.createElement("button");
    btnA.textContent = "Rezept ändern";
    btnA.onclick = async () => {
      if (selected.size === 0) return;
      const targets = [];
      for (const t of selected) {
        const id = t.getAttribute("data-id") || t.getAttribute("data-item-id") || "";
        const label = t.querySelector(".itile-name, .name")?.textContent || id;
        targets.push({ id, label, path: "" });
      }
      try {
        const res = await fetch(apiBase() + "/queue", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "same-origin",
          body: JSON.stringify({ category: "code", kind: "recipe", targets, comment: ta.value || "" })
        });
        if (!res.ok) {
          const j = await res.json().catch(() => ({}));
          showError("Fehler: " + (j.error || res.status));
          return;
        }
        showMsg("Gesendet");
        selected.clear();
        refresh();
        loadQueue();
      } catch (e) {
        showError("Netzwerkfehler");
      }
    };
    const btnB = document.createElement("button");
    btnB.textContent = "Anmerkung";
    btnB.onclick = async () => {
      if (selected.size === 0) return;
      const targets = [];
      for (const t of selected) {
        const id = t.getAttribute("data-id") || t.getAttribute("data-item-id") || "";
        const label = t.querySelector(".itile-name, .name")?.textContent || id;
        targets.push({ id, label, path: "" });
      }
      try {
        const res = await fetch(apiBase() + "/queue", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "same-origin",
          body: JSON.stringify({ category: "code", kind: "note", targets, comment: ta.value || "" })
        });
        if (!res.ok) {
          const j = await res.json().catch(() => ({}));
          showError("Fehler: " + (j.error || res.status));
          return;
        }
        showMsg("Gesendet");
        selected.clear();
        refresh();
        loadQueue();
      } catch (e) {
        showError("Netzwerkfehler");
      }
    };
    actions.appendChild(btnA);
    actions.appendChild(btnB);
    bar.insertBefore(actions, bar.querySelector("input").nextSibling);
    document.body.appendChild(bar);
    tiles.forEach((t) => {
      const cb = document.createElement("input");
      cb.type = "checkbox";
      cb.style.cssText = "position:absolute;top:4px;right:4px;z-index:10;";
      cb.onchange = () => {
        if (cb.checked) selected.add(t); else selected.delete(t);
        refresh();
      };
      t.style.position = t.style.position || "relative";
      t.appendChild(cb);
    });
    refresh();
  }

  window.sbDevHook = function (kind, el, info) {
    try {
      if (kind === "textures") {
        hookTextures();
        return;
      }
      if (kind === "texture-zoom") {
        hookTextureZoom(info);
        return;
      }
      if (kind === "item-page") {
        hookItemPage(info);
        return;
      }
      if (kind === "items") {
        hookItems();
        return;
      }
    } catch (e) {
    }
  };

  setTimeout(loadQueue, 500);
})();
