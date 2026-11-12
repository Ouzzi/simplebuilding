"use strict";
// Simple Livemap web pages: hero map drawing, monthly/yearly prices, copy buttons, confirm dialogs, connect status.
// No inline scripts on the pages (CSP script-src 'self'); everything here works without data from other hosts.
(function () {
  // ------------------------------------------------------------------ hero: an animated example map (own drawing)
  const hero = document.getElementById("hero-map");
  if (hero && hero.getContext) {
    const ctx = hero.getContext("2d"), W = 128, H = 96, S = 4;
    const off = document.createElement("canvas"); off.width = W; off.height = H;
    const o = off.getContext("2d"), img = o.createImageData(W, H);
    let seed = 7;
    const rnd = () => (seed = (seed * 16807) % 2147483647) / 2147483647;
    const grid = []; for (let i = 0; i < 17 * 13; i++) grid.push(rnd());
    const lerp = (a, b, t) => a + (b - a) * (t * t * (3 - 2 * t));
    const noise = (x, y) => { // value noise on a 16 x 12 grid, two octaves
      let v = 0, amp = 1, f = 1, sum = 0;
      for (let k = 0; k < 2; k++) {
        const gx = (x * f / 8) % 16, gy = (y * f / 8) % 12, ix = Math.floor(gx), iy = Math.floor(gy), tx = gx - ix, ty = gy - iy;
        const g = (a, b) => grid[(b % 13) * 17 + (a % 17)];
        v += amp * lerp(lerp(g(ix, iy), g(ix + 1, iy), tx), lerp(g(ix, iy + 1), g(ix + 1, iy + 1), tx), ty);
        sum += amp; amp *= 0.45; f *= 2.3;
      }
      return v / sum;
    };
    const height = [];
    for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) height.push(noise(x, y));
    const colour = (h, x, y) => {
      if (h < 0.36) return h < 0.3 ? [44, 72, 170] : [60, 92, 196];          // deep / shallow water
      if (h < 0.4) return [214, 198, 140];                                   // sand
      if (h < 0.62) return (x * 7 + y * 13) % 11 === 0 ? [74, 128, 44] : [102, 160, 58]; // grass (+ some trees)
      if (h < 0.72) return [58, 104, 40];                                    // forest
      return h < 0.8 ? [128, 128, 128] : [236, 240, 244];                    // stone / snow
    };
    const seen = new Uint8Array(W * H);
    const path = []; // the explorer walks a slow loop
    for (let i = 0; i < 400; i++) { const a = i / 400 * Math.PI * 2; path.push([64 + Math.cos(a) * 40 + Math.sin(a * 3) * 8, 48 + Math.sin(a) * 28 + Math.cos(a * 2) * 6]); }
    let step = 0;
    const reveal = (cx, cy, r) => {
      for (let y = Math.max(0, cy - r); y < Math.min(H, cy + r); y++)
        for (let x = Math.max(0, cx - r); x < Math.min(W, cx + r); x++) seen[y * W + x] = 1;
    };
    for (let i = 0; i < 260; i++) { const p = path[i]; reveal(Math.round(p[0]), Math.round(p[1]), 11); }
    step = 260;
    const draw = () => {
      for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
        const i = y * W + x, p = i * 4;
        if (!seen[i]) { img.data[p] = 15; img.data[p + 1] = 20; img.data[p + 2] = 24; img.data[p + 3] = 255; continue; }
        const h = height[i], north = y > 0 ? height[i - W] : h;
        let [r, g, b] = colour(h, x, y);
        const shade = h >= 0.36 ? (h > north + 0.004 ? 1.12 : h < north - 0.004 ? 0.86 : 1) : 1; // map relief
        img.data[p] = Math.min(255, r * shade); img.data[p + 1] = Math.min(255, g * shade); img.data[p + 2] = Math.min(255, b * shade); img.data[p + 3] = 255;
      }
      o.putImageData(img, 0, 0);
      ctx.imageSmoothingEnabled = false;
      ctx.drawImage(off, 0, 0, W * S, H * S);
      const p = path[step % path.length];
      ctx.fillStyle = "#ff5a4e"; ctx.strokeStyle = "#ffffff"; ctx.lineWidth = 2;
      ctx.beginPath(); ctx.arc(p[0] * S, p[1] * S, 6, 0, Math.PI * 2); ctx.fill(); ctx.stroke();
      ctx.font = "bold 14px system-ui, sans-serif"; ctx.textBaseline = "middle";
      const label = "Steve", tw = ctx.measureText(label).width;
      ctx.fillStyle = "rgba(16,22,28,0.85)"; ctx.fillRect(p[0] * S + 10, p[1] * S - 10, tw + 10, 20);
      ctx.fillStyle = "#eef2f4"; ctx.fillText(label, p[0] * S + 15, p[1] * S);
    };
    draw();
    if (!window.matchMedia || !matchMedia("(prefers-reduced-motion: reduce)").matches) {
      setInterval(() => {
        if (document.hidden) return;
        step++;
        const p = path[step % path.length]; reveal(Math.round(p[0]), Math.round(p[1]), 11);
        if (step % path.length === 0) { seen.fill(0); for (let i = 0; i < 60; i++) { const q = path[i]; reveal(Math.round(q[0]), Math.round(q[1]), 11); } }
        draw();
      }, 120);
    }
  }

  // ------------------------------------------------------------------ monthly / yearly prices
  document.querySelectorAll(".toggle [data-interval]").forEach(btn => btn.addEventListener("click", () => {
    const which = btn.dataset.interval;
    document.querySelectorAll(".toggle [data-interval]").forEach(b => b.setAttribute("aria-pressed", String(b.dataset.interval === which)));
    document.querySelectorAll("[data-month][data-year]").forEach(el => { el.textContent = el.dataset[which]; });
    document.querySelectorAll("[data-show]").forEach(el => { el.hidden = el.dataset.show !== which; });   // struck 12 x monthly
  }));

  // ------------------------------------------------------------------ copy buttons, confirm dialogs
  document.querySelectorAll("[data-copy]").forEach(btn => btn.addEventListener("click", () => {
    const el = document.querySelector(btn.dataset.copy);
    if (!el) return;
    const text = "value" in el && el.value !== undefined ? el.value : el.textContent;
    const done = () => { const old = btn.textContent; btn.textContent = "✓"; setTimeout(() => { btn.textContent = old; }, 1200); };
    if (navigator.clipboard) navigator.clipboard.writeText(text).then(done, () => {});
    else if (el.select) { el.select(); document.execCommand("copy"); done(); }
  }));
  document.querySelectorAll("form[data-confirm]").forEach(f => f.addEventListener("submit", ev => {
    if (!window.confirm(f.dataset.confirm)) ev.preventDefault();
  }));

  // ------------------------------------------------------------------ connect: wait for the game server
  const box = document.getElementById("connect");
  if (box) {
    const out = document.getElementById("connect-state"), open = document.getElementById("connect-open");
    let left = parseInt(box.dataset.left || "0", 10), finished = false;
    const fmt = s => Math.floor(s / 60) + ":" + String(s % 60).padStart(2, "0");
    const tick = () => { if (!finished && left > 0) { left--; out.textContent = out.dataset.waiting.replace("{left}", fmt(left)); } };
    const poll = async () => {
      if (finished || document.hidden) return;
      try {
        const r = await fetch("/app/connect/state?c=" + encodeURIComponent(box.dataset.ref), { credentials: "same-origin", cache: "no-store" });
        if (!r.ok) return;
        const s = await r.json();
        if (s.state === "waiting") { left = s.left; return; }
        if (s.state === "expired") { finished = true; out.textContent = out.dataset.expired; return; }
        if (s.state === "preparing" || s.state === "ready" || s.state === "connected") {
          finished = true;
          out.textContent = out.dataset.connected + (typeof s.progress === "number" ? " " + s.progress + " %" : "");
          if (s.url) { open.href = s.url; open.classList.remove("hidden"); }
          document.querySelectorAll(".steps li").forEach((li, i) => { li.className = i < 3 ? "done" : "on"; });
        }
      } catch (e) { /* offline: try again */ }
    };
    setInterval(tick, 1000);
    setInterval(poll, 3000);
    poll();
  }
})();
