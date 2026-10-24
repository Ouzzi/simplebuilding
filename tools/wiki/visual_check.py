#!/usr/bin/env python3
"""
Sichtprüfung des Wikis im Headless-Browser (Playwright, Chromium).

    python tools/wiki/visual_check.py --serve wiki --out build/wiki-visual
    python tools/wiki/visual_check.py --base https://example.org/ --out build/wiki-visual-prod

Öffnet die Hauptseiten (Start, je Modul die Übersicht, Alle Mods, Befehle, Konfiguration,
Texturen, Suche, eine Item-Seite) in Desktop 1440 px und Handy 390 px, jeweils hell und dunkel,
und speichert Screenshots. Gesammelt werden:
  * Konsolen-Fehler und Skriptfehler,
  * Antworten >= 400 und abgebrochene Anfragen (Dateien, die laut .gitignore nicht eingecheckt
    werden - Vanilla-abgeleitete Texturen, data/ui-shots.js -, liefert kein Host aus; die Seite
    fängt sie ab, sie werden nur als expected404 gezählt),
  * kaputte Bilder, die sichtbar im DOM bleiben,
  * waagerechter Überlauf der Seite (Element ragt aus dem Fenster, ohne Scroll-Container),
  * Text mit Kontrast unter 4,5:1 (große Schrift 3:1),
  * "nicht gefunden"-Seiten, auch hinter einer Stichprobe der internen Links.
Bericht: <out>/report.json, Kurzfassung auf stdout. Exit 1 bei Fehlern.

Voraussetzung: pip install playwright && python -m playwright install chromium
(auf sb-test in einem venv, weil der System-Python extern verwaltet ist).
"""

from __future__ import annotations

import argparse
import functools
import http.server
import json
import re
import subprocess
import sys
import threading
import time
from pathlib import Path
from urllib.parse import urljoin, urlparse

from playwright.sync_api import sync_playwright

VANILLA = "/assets/textures/minecraft/"
REPO = Path(__file__).resolve().parents[2]


@functools.lru_cache(maxsize=None)
def ignored(url_path: str) -> bool:
    """Liegt die Datei in .gitignore (Vanilla-abgeleitet, nur lokal)? Dann ist ein 404 beim Hosting gewollt."""
    if VANILLA in url_path:
        return True
    rel = "wiki/" + url_path.lstrip("/")
    return subprocess.run(["git", "-C", str(REPO), "check-ignore", "-q", "--no-index", rel]).returncode == 0

FIXED_PAGES = [
    ("start", ""),
    ("allmods", "?mod=simplebuilding&tab=allmods"),
    ("commands", "?mod=simplebuilding&tab=commands"),
    ("config", "?mod=simplebuilding&tab=config"),
    ("textures", "?mod=simplebuilding&tab=textures"),
    ("search", "?mod=simplebuilding&q=hammer"),
    ("item", "?mod=simplebuilding&item=simplebuilding:enderite_chisel"),
]

READY_JS = """() => { const m = document.getElementById('main');
  return !!(m && m.children.length && !document.querySelector('.loading')); }"""

# Prüft im Browser: Überlauf, kaputte Bilder, Kontrast, Nicht-gefunden.
INSPECT_JS = r"""() => {
  const out = { overflow: [], images: [], contrast: [], notfound: false, scrollWidth: 0 };
  const W = document.documentElement.clientWidth;
  out.scrollWidth = document.documentElement.scrollWidth;
  out.notfound = !!document.querySelector('#main .notfound');
  const desc = el => {
    let s = el.tagName.toLowerCase();
    if (el.id) s += '#' + el.id;
    if (el.classList.length) s += '.' + Array.from(el.classList).slice(0, 2).join('.');
    const p = el.parentElement;
    if (p && p !== document.body) {
      let q = p.tagName.toLowerCase();
      if (p.id) q += '#' + p.id; else if (p.classList.length) q += '.' + p.classList[0];
      s = q + ' > ' + s;
    }
    return s;
  };
  const visible = el => {
    const r = el.getBoundingClientRect();
    if (r.width < 1 || r.height < 1) return false;
    for (let e = el; e; e = e.parentElement) {
      const cs = getComputedStyle(e);
      if (cs.display === 'none' || cs.visibility === 'hidden' || parseFloat(cs.opacity) === 0) return false;
      if (e.tagName === 'DETAILS' && !e.open && e !== el && !el.closest('summary')) return false;
    }
    return true;
  };
  const clipped = el => {
    for (let e = el.parentElement; e && e !== document.body; e = e.parentElement) {
      const cs = getComputedStyle(e);
      if (/(auto|scroll|hidden|clip)/.test(cs.overflowX)) return true;
      if (cs.position === 'fixed') return true;
    }
    return false;
  };
  // Überlauf: nur, wenn die Seite selbst breiter als das Fenster wird.
  if (out.scrollWidth > W + 1) {
    const seen = new Set();
    for (const el of document.body.querySelectorAll('*')) {
      const r = el.getBoundingClientRect();
      if (r.right > W + 1 && r.width > 0 && !clipped(el) && visible(el)) {
        const d = desc(el); if (seen.has(d)) continue; seen.add(d);
        out.overflow.push({ el: d, right: Math.round(r.right), width: Math.round(r.width) });
        if (out.overflow.length >= 8) break;
      }
    }
  }
  for (const img of document.images) {
    if (img.complete && img.naturalWidth === 0 && img.getAttribute('src') && visible(img))
      out.images.push(img.getAttribute('src'));
  }
  // Kontrast
  const parse = c => { const m = c.match(/rgba?\(([^)]+)\)/); if (!m) return null;
    const p = m[1].split(/[ ,\/]+/).filter(Boolean).map(Number); return [p[0], p[1], p[2], p.length > 3 ? p[3] : 1]; };
  const lum = c => { const f = v => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); };
    return 0.2126 * f(c[0]) + 0.7152 * f(c[1]) + 0.0722 * f(c[2]); };
  const blend = (top, bot) => { const a = top[3]; return [top[0] * a + bot[0] * (1 - a), top[1] * a + bot[1] * (1 - a), top[2] * a + bot[2] * (1 - a), 1]; };
  const bgOf = el => {
    const layers = [];
    for (let e = el; e; e = e.parentElement) {
      const cs = getComputedStyle(e);
      if (cs.backgroundImage && cs.backgroundImage !== 'none') return null;  // Verlauf/Bild: nicht messbar
      const c = parse(cs.backgroundColor);
      if (c && c[3] > 0) { layers.push(c); if (c[3] >= 1) break; }
    }
    let bg = [255, 255, 255, 1];
    for (let i = layers.length - 1; i >= 0; i--) bg = blend(layers[i], bg);
    return bg;
  };
  const seen = new Map();
  const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
  let n, checked = 0;
  while ((n = walker.nextNode()) && checked < 4000) {
    if (!n.nodeValue.trim()) continue;
    const el = n.parentElement;
    if (!el || /^(SCRIPT|STYLE|NOSCRIPT|OPTION)$/.test(el.tagName) || !visible(el)) continue;
    checked++;
    const cs = getComputedStyle(el);
    let fg = parse(cs.color); if (!fg) continue;
    const bg = bgOf(el); if (!bg) continue;
    if (fg[3] < 1) fg = blend(fg, bg);
    const op = parseFloat(cs.opacity); // eigene Deckkraft des Elements
    if (op < 1) fg = blend([fg[0], fg[1], fg[2], op], bg);
    const L1 = lum(fg), L2 = lum(bg);
    const ratio = (Math.max(L1, L2) + 0.05) / (Math.min(L1, L2) + 0.05);
    const size = parseFloat(cs.fontSize), bold = parseInt(cs.fontWeight, 10) >= 700;
    const need = (size >= 24 || (bold && size >= 18.66)) ? 3 : 4.5;
    if (ratio + 0.005 < need) {
      const key = desc(el) + '|' + cs.color + '|' + Math.round(ratio * 100);
      if (!seen.has(key)) seen.set(key, { el: desc(el), ratio: Math.round(ratio * 100) / 100, need,
        fg: cs.color, bg: 'rgb(' + bg.slice(0, 3).map(Math.round).join(',') + ')', text: n.nodeValue.trim().slice(0, 40) });
    }
  }
  out.contrast = Array.from(seen.values()).slice(0, 25);
  return out;
}"""

LINKS_JS = """() => Array.from(document.querySelectorAll('a[href]')).map(a => a.href)"""

PERF_JS = """() => { const nav = performance.getEntriesByType('navigation')[0];
  const res = performance.getEntriesByType('resource');
  let bytes = nav ? (nav.transferSize || nav.encodedBodySize || 0) : 0, big = [];
  for (const r of res) { const b = r.transferSize || r.encodedBodySize || 0; bytes += b;
    if (b > 500000) big.push([r.name.replace(location.origin, ''), b]); }
  return { bytes, big, domContentLoaded: nav ? Math.round(nav.domContentLoadedEventEnd) : null }; }"""


def serve(directory: Path, port: int) -> str:
    handler = functools.partial(http.server.SimpleHTTPRequestHandler, directory=str(directory))
    handler.log_message = lambda *a, **k: None
    httpd = http.server.ThreadingHTTPServer(("127.0.0.1", port), handler)
    threading.Thread(target=httpd.serve_forever, daemon=True).start()
    return f"http://127.0.0.1:{httpd.server_address[1]}/"


def slug(text: str) -> str:
    return re.sub(r"[^a-z0-9_-]+", "-", text.lower()).strip("-") or "page"


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--base", help="URL der ausgelieferten Wiki-Seite")
    ap.add_argument("--serve", type=Path, help="Ordner statisch ausliefern (statt --base)")
    ap.add_argument("--port", type=int, default=0)
    ap.add_argument("--out", type=Path, default=Path("build/wiki-visual"))
    ap.add_argument("--header", action="append", default=[], help="Name: Wert, z. B. für eine Zugangssperre")
    ap.add_argument("--widths", default="1440,390")
    ap.add_argument("--schemes", default="light,dark")
    ap.add_argument("--modules", default="all", help="all, none oder Komma-Liste von Modul-Ids")
    ap.add_argument("--max-height", type=int, default=4000, help="Screenshot-Höhe höchstens (px)")
    ap.add_argument("--links", type=int, default=120, help="so viele interne Links nachprüfen")
    ap.add_argument("--no-shots", action="store_true")
    args = ap.parse_args()
    if bool(args.base) == bool(args.serve):
        ap.error("genau eins von --base oder --serve angeben")
    base = serve(args.serve.resolve(), args.port) if args.serve else args.base.rstrip("/") + "/"
    headers = dict(h.split(":", 1) for h in args.header)
    headers = {k.strip(): v.strip() for k, v in headers.items()}
    args.out.mkdir(parents=True, exist_ok=True)
    report = {"base": base if args.base else str(args.serve), "pages": [], "links": [], "perf": None}
    errors = 0
    links: set[str] = set()

    with sync_playwright() as p:
        browser = p.chromium.launch()
        probe = browser.new_context(extra_http_headers=headers)
        page = probe.new_page()
        page.goto(base, wait_until="load")
        page.wait_for_function(READY_JS, timeout=60000)
        modules = page.evaluate("() => (window.WIKI_MODULES || []).map(m => m.id)")
        report["perf"] = page.evaluate(PERF_JS)
        probe.close()
        pages = list(FIXED_PAGES)
        wanted = modules if args.modules == "all" else [] if args.modules == "none" else args.modules.split(",")
        pages[1:1] = [("mod-" + m, "?mod=" + m) for m in wanted if m in modules]

        for width in [int(w) for w in args.widths.split(",")]:
            mobile = width < 700
            for scheme in args.schemes.split(","):
                ctx = browser.new_context(viewport={"width": width, "height": 844 if mobile else 900},
                                          color_scheme=scheme, is_mobile=mobile, has_touch=mobile,
                                          extra_http_headers=headers)
                for name, query in pages:
                    entry = {"page": name, "url": query, "width": width, "scheme": scheme,
                             "console": [], "pageerrors": [], "http": [], "expected404": 0}
                    pg = ctx.new_page()

                    def on_response(resp, entry=entry):
                        if resp.status >= 400:
                            if ignored(urlparse(resp.url).path):
                                entry["expected404"] += 1
                            else:
                                entry["http"].append(f"{resp.status} {resp.url.replace(base, '/')}")

                    pg.on("console", lambda m, e=entry: m.type == "error" and not m.text.startswith("Failed to load resource") and e["console"].append(m.text[:300]))
                    pg.on("pageerror", lambda exc, e=entry: e["pageerrors"].append(str(exc)[:300]))
                    pg.on("response", on_response)
                    pg.on("requestfailed", lambda r, e=entry: ignored(urlparse(r.url).path) or e["http"].append(f"failed {r.url.replace(base, '/')} {r.failure}"))
                    t0 = time.time()
                    try:
                        pg.goto(urljoin(base, query), wait_until="load", timeout=90000)
                        pg.wait_for_function(READY_JS, timeout=60000)
                        pg.wait_for_load_state("networkidle", timeout=30000)
                    except Exception as exc:  # Zeitüberschreitung zählt als Fehler, Prüfung läuft weiter
                        entry["pageerrors"].append(f"load: {exc}"[:300])
                    entry["seconds"] = round(time.time() - t0, 2)
                    res = pg.evaluate(INSPECT_JS)
                    entry.update(overflow=res["overflow"], images=res["images"], contrast=res["contrast"],
                                 notfound=res["notfound"], scrollWidth=res["scrollWidth"])
                    if width == 1440 and scheme == args.schemes.split(",")[0]:
                        links.update(pg.evaluate(LINKS_JS))
                    if not args.no_shots:
                        # Durchscrollen: Kacheln, die erst beim Sichtbarwerden gefuellt werden, mit aufnehmen.
                        pg.evaluate("""async (max) => { const h = Math.min(document.documentElement.scrollHeight, max);
                          for (let y = 0; y < h; y += innerHeight / 2) { scrollTo(0, y); await new Promise(r => setTimeout(r, 60)); }
                          scrollTo(0, 0); await new Promise(r => setTimeout(r, 150)); }""", args.max_height)
                        h = min(pg.evaluate("document.documentElement.scrollHeight"), args.max_height)
                        shot = args.out / f"{slug(name)}-{width}-{scheme}.png"
                        pg.screenshot(path=str(shot), full_page=True, clip={"x": 0, "y": 0, "width": width, "height": h})
                        entry["shot"] = shot.name
                    pg.close()
                    n = (len(entry["console"]) + len(entry["pageerrors"]) + len(entry["http"]) + len(entry["images"])
                         + len(entry["overflow"]) + len(entry["contrast"]) + (1 if entry["notfound"] else 0))
                    entry["errors"] = n
                    errors += n
                    report["pages"].append(entry)
                    print(f"{'FAIL' if n else 'ok  '} {width:4} {scheme:5} {name:28} {entry['seconds']:5}s"
                          + (f"  errors={n}" if n else ""), flush=True)
                ctx.close()

        # Interne Links: Stichprobe ansteuern, "nicht gefunden" und Skriptfehler zählen.
        origin = urlparse(base).netloc
        internal = sorted(u for u in links if urlparse(u).netloc == origin and urlparse(u).scheme.startswith("http"))
        step = max(1, len(internal) // max(1, args.links))
        sample = internal[::step][: args.links]
        ctx = browser.new_context(viewport={"width": 1440, "height": 900}, extra_http_headers=headers)
        pg = ctx.new_page()
        bad: list[str] = []
        pg.on("pageerror", lambda exc: bad.append(f"pageerror {str(exc)[:200]}"))
        for url in sample:
            before = len(bad)
            path = urlparse(url).path
            if path.endswith((".png", ".js", ".json", ".css", ".html")) and not path.endswith("index.html"):
                r = ctx.request.get(url)
                if r.status >= 400 and not ignored(path):
                    bad.append(f"{r.status} {url}")
                continue
            try:
                pg.goto(url, wait_until="load", timeout=90000)
                pg.wait_for_function(READY_JS, timeout=60000)
                if pg.query_selector("#main .notfound"):
                    bad.append(f"notfound {url.replace(base, '/')}")
            except Exception as exc:
                bad.append(f"load {url.replace(base, '/')}: {str(exc)[:120]}")
            if len(bad) > before and not bad[-1].startswith(("notfound", "load", "4", "5")):
                bad[-1] += f" (auf {url.replace(base, '/')})"
        report["links"] = {"found": len(internal), "checked": len(sample), "bad": bad}
        errors += len(bad)
        ctx.close()
        browser.close()

    (args.out / "report.json").write_text(json.dumps(report, indent=1, ensure_ascii=False), encoding="utf-8")
    perf = report["perf"] or {}
    print(f"\nStartseite: {perf.get('bytes', 0) / 1e6:.1f} MB übertragen, DOMContentLoaded {perf.get('domContentLoaded')} ms")
    for b in perf.get("big", []):
        print(f"  groß: {b[0]} {b[1] / 1e6:.1f} MB")
    print(f"Links: {report['links']['checked']}/{report['links']['found']} geprüft, {len(report['links']['bad'])} kaputt")
    for b in report["links"]["bad"][:20]:
        print("  " + b)
    for e in report["pages"]:
        if not e["errors"]:
            continue
        print(f"- {e['page']} {e['width']} {e['scheme']}:")
        for key in ("console", "pageerrors", "http", "images"):
            for v in e[key][:5]:
                print(f"    {key}: {v}")
        for v in e["overflow"][:4]:
            print(f"    overflow: {v['el']} right={v['right']}")
        for v in e["contrast"][:6]:
            print(f"    contrast {v['ratio']}<{v['need']}: {v['el']} {v['fg']} on {v['bg']} \"{v['text']}\"")
        if e["notfound"]:
            print("    notfound")
    print(f"\n{len(report['pages'])} Ansichten, {errors} Fehler. Bericht: {args.out / 'report.json'}")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
