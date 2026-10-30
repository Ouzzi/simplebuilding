#!/usr/bin/env python3
"""Mod-Struktur-Uebersicht: modules/modules.json -> docs/MODSTRUKTUR.html + docs/MODSTRUKTUR.md.

  python tools/modtree/modtree.py          # schreiben
  python tools/modtree/modtree.py --check  # Exit 1, wenn die Dateien veraltet sind (Teil von ./gradlew check)

Quelle ist modules/modules.json (requires/optional/parent/bundle = dieselben Daten wie die fabric.mod.json
depends/recommends/suggests, die daraus erzeugt werden). Status: Feld "status" im Eintrag, sonst aus der Version
(>= 0.9 fertig, sonst neu). Geplante Mods ohne Eintrag: tools/modtree/planned.json (Liste von
{id,name,parent,description}). Regel: AGENTS.md "neues Modul", docs/ai/PRINZIPIEN.md A13.
"""
import html, json, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT_HTML, OUT_MD = ROOT / "docs/MODSTRUKTUR.html", ROOT / "docs/MODSTRUKTUR.md"
STATUS = {"done": "fertig", "new": "neu", "planned": "geplant"}
BASE = "simplelib"


def load(root=ROOT):
    mods = json.loads((root / "modules/modules.json").read_text(encoding="utf-8"))["modules"]
    planned = root / "tools/modtree/planned.json"
    extra = json.loads(planned.read_text(encoding="utf-8")) if planned.exists() else []
    out = {}
    for m in mods:
        v = [int(x) if x.isdigit() else 0 for x in str(m.get("version", "0")).split(".")[:2]]
        st = m.get("status") or ("done" if v >= [0, 9] or m["id"] in ("simplebuilding", BASE) else "new")
        out[m["id"]] = dict(id=m["id"], name=m.get("displayName") or m["name"], desc=m.get("description", ""),
                            req=list(m.get("requires", [])), opt=list(m.get("optional", [])),
                            parent=m.get("parent"), bundle=m.get("bundle"), status=st,
                            loaders=m.get("loaders", []))
    for p in extra:
        out[p["id"]] = dict(id=p["id"], name=p["name"], desc=p.get("description", ""), req=[], opt=[],
                            parent=p.get("parent"), bundle=p.get("bundle", "optional"), status="planned", loaders=[])
    return out


def children(mods, pid):
    return sorted((m for m in mods.values() if m["parent"] == pid), key=lambda m: (m["bundle"] != "required", m["name"]))


def roots(mods):
    r = [m for m in mods.values() if not m["parent"] or m["parent"] not in mods]
    sup = sorted((m for m in r if children(mods, m["id"])), key=lambda m: -len(children(mods, m["id"])))
    rest = sorted((m for m in r if not children(mods, m["id"]) and m["id"] != BASE), key=lambda m: m["name"])
    return sup, rest


def label(mods, ids):
    return [mods[i]["name"] if i in mods else i for i in ids]


def used_by(mods, mid):
    return sorted(m["name"] for m in mods.values() if mid in m["req"])


def deps_html(mods, m):
    e = html.escape
    out = ""
    if m["req"]:
        out += '<div class="d req"><b>required</b> ' + ", ".join(e(x) for x in label(mods, m["req"])) + "</div>"
    if m["opt"]:
        out += '<div class="d opt"><b>optional</b> ' + ", ".join(e(x) for x in label(mods, m["opt"])) + "</div>"
    return out


def card(mods, m, depth=0):
    e = html.escape
    kids = children(mods, m["id"])
    cls = "node " + ("edge-req" if m["bundle"] == "required" else "edge-opt" if m["parent"] else "")
    s = (f'<div class="{cls}"><div class="card"><div class="hd"><span class="nm">{e(m["name"])}</span>'
         f'<span class="st {m["status"]}">{STATUS[m["status"]]}</span></div><code>{e(m["id"])}</code>'
         f'<p>{e(m["desc"])}</p>{deps_html(mods, m)}</div>')
    if kids:
        s += '<div class="kids">' + "".join(card(mods, k, depth + 1) for k in kids) + "</div>"
    return s + "</div>"


def li(mods, m):
    e = html.escape
    kids = children(mods, m["id"])
    kind = "required" if m["bundle"] == "required" else "optional" if m["parent"] else ""
    s = (f'<li class="{kind}"><span class="nm">{e(m["name"])}</span> <code>{e(m["id"])}</code> '
         f'<span class="st {m["status"]}">{STATUS[m["status"]]}</span>'
         + (f' <i class="k">{kind}</i>' if kind else "") + f'<p>{e(m["desc"])}</p>{deps_html(mods, m)}')
    if kids:
        s += "<ul>" + "".join(li(mods, k) for k in kids) + "</ul>"
    return s + "</li>"


CSS = """
:root{--bg:#f6f5f1;--fg:#1d1d1b;--mut:#66665f;--card:#fff;--bd:#c9c6bb;--acc:#1f6f43;--req:#1f6f43;--opt:#8a6d1c;--base:#2b4f86}
@media(prefers-color-scheme:dark){:root:not([data-theme=light]){--bg:#15171a;--fg:#e7e6e1;--mut:#9a9a92;--card:#1f2226;--bd:#3a3e44;--acc:#5fc08a;--req:#5fc08a;--opt:#d8b455;--base:#7fa6e6}}
:root[data-theme=dark]{--bg:#15171a;--fg:#e7e6e1;--mut:#9a9a92;--card:#1f2226;--bd:#3a3e44;--acc:#5fc08a;--req:#5fc08a;--opt:#d8b455;--base:#7fa6e6}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.45 'Inter',system-ui,sans-serif}
main{max-width:1800px;margin:0 auto;padding:20px 16px 48px}h1{margin:0 0 4px;font-size:26px}h2{margin:32px 0 10px;font-size:19px}
.sub{color:var(--mut);margin:0 0 14px}.bar{display:flex;gap:14px;flex-wrap:wrap;align-items:center;font-size:13px;color:var(--mut);margin-bottom:10px}
button{background:var(--card);color:var(--fg);border:1px solid var(--bd);border-radius:6px;padding:4px 10px;cursor:pointer;font:inherit}
.sw{display:inline-block;width:26px;border-top:3px solid var(--req);vertical-align:middle;margin-right:4px}.sw.o{border-top:3px dashed var(--opt)}
code{font:12px ui-monospace,monospace;color:var(--mut)}p{margin:4px 0}
.card{background:var(--card);border:1px solid var(--bd);border-radius:8px;padding:8px 10px;font-size:13px}
.hd{display:flex;justify-content:space-between;gap:8px;align-items:baseline}.nm{font-weight:600}
.st{font-size:11px;border-radius:9px;padding:0 7px;border:1px solid currentColor;white-space:nowrap}.st.done{color:var(--req)}.st.new{color:var(--base)}.st.planned{color:var(--opt)}
.d{font-size:12px;color:var(--mut)}.d b{color:var(--req)}.d.opt b{color:var(--opt)}
.tree{display:block}.list{display:none}
.root>.card{border:2px solid var(--acc);display:inline-block;min-width:300px;max-width:520px}
.root>.kids{margin-left:18px;border-left:3px solid var(--req);padding:14px 0 0 0;display:grid;grid-template-columns:repeat(auto-fill,minmax(250px,1fr));gap:14px 18px;align-items:start}
.kids .node{position:relative;padding-left:22px}.kids .node::before{content:"";position:absolute;left:-0px;top:22px;width:22px;border-top:3px solid var(--req)}
.kids .edge-opt::before{border-top:3px dashed var(--opt)}.kids .edge-opt>.card{border-style:dashed;border-color:var(--opt)}.kids .edge-req>.card{border-color:var(--req)}
.node .node{margin-top:8px}.node>.kids{margin-left:14px;border-left:2px solid var(--bd);padding-top:8px}
.base{border:2px solid var(--base);border-radius:8px;padding:10px 14px;background:var(--card)}.base .nm{color:var(--base)}
.solo{display:grid;grid-template-columns:repeat(auto-fill,minmax(250px,1fr));gap:12px}
.list ul{list-style:none;margin:0;padding-left:14px;border-left:2px solid var(--req)}.list>ul{padding-left:0;border:0}
.list li{margin:10px 0;padding-left:8px}.list li.optional{border-left:2px dashed var(--opt);margin-left:-2px}.list .k{font-size:11px;color:var(--mut)}
@media(max-width:700px){.tree{display:none}.list{display:block}h1{font-size:21px}}
"""

JS = """<script>(function(){var r=document.documentElement,b=document.getElementById('th');
try{var t=localStorage.getItem('th');if(t)r.dataset.theme=t}catch(e){}
b.onclick=function(){var d=r.dataset.theme?r.dataset.theme==='dark':matchMedia('(prefers-color-scheme:dark)').matches;
r.dataset.theme=d?'light':'dark';try{localStorage.setItem('th',r.dataset.theme)}catch(e){}}})();</script>"""


def render_html(mods):
    e = html.escape
    sup, rest = roots(mods)
    n = len(mods)
    base = mods.get(BASE)
    b = ""
    if base:
        ub = used_by(mods, BASE)
        b = (f'<h2>Gemeinsame Basis</h2><div class="base"><span class="nm">{e(base["name"])}</span> <code>{BASE}</code> '
             f'<span class="st {base["status"]}">{STATUS[base["status"]]}</span><p>{e(base["desc"])}</p>'
             f'<div class="d"><b>required von {len(ub)} Mods:</b> {e(", ".join(ub))}</div></div>')
    parts = []
    for m in sup:
        kids = children(mods, m["id"])
        c = (f'<div class="card"><div class="hd"><span class="nm">{e(m["name"])}</span><span class="st {m["status"]}">'
             f'{STATUS[m["status"]]}</span></div><code>{e(m["id"])}</code><p>{e(m["desc"])}</p>{deps_html(mods, m)}</div>')
        parts.append(f'<div class="root"><h2>{e(m["name"])}: {len(kids)} Sub-Mods</h2>{c}<div class="kids">'
                     + "".join(card(mods, k) for k in kids) + "</div></div>")
    solo = '<h2>Eigenstaendige Mods</h2><div class="solo">' + "".join(card(mods, m) for m in rest) + "</div>" if rest else ""
    lst = "".join(f"<ul>{li(mods, m)}</ul>" for m in sup + rest)
    return f"""<!doctype html>
<html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Mod-Struktur</title>
<link rel="preconnect" href="https://fonts.googleapis.com"><link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600&display=swap" rel="stylesheet">
<style>{CSS}</style></head><body><main>
<h1>Mod-Struktur</h1>
<p class="sub">{n} Mods, erzeugt aus modules/modules.json (tools/modtree/modtree.py). Nicht von Hand bearbeiten.</p>
<div class="bar"><span><span class="sw"></span>required (Pflicht-Sub-Mod)</span><span><span class="sw o"></span>optional (Integration)</span>
<span class="st done">fertig</span><span class="st new">neu</span><span class="st planned">geplant</span><button id="th" type="button">hell/dunkel</button></div>
{b}
<div class="tree">{"".join(parts)}{solo}</div>
<div class="list"><h2>Struktur</h2>{lst}</div>
</main>{JS}</body></html>
"""


def md_node(mods, m, ind=0):
    pad = "  " * ind
    kind = f" ({m['bundle']})" if m["parent"] and m["bundle"] else ""
    s = f"{pad}- **{m['name']}** `{m['id']}`{kind} [{STATUS[m['status']]}]: {m['desc']}\n"
    if m["req"]:
        s += f"{pad}  - required: {', '.join(label(mods, m['req']))}\n"
    if m["opt"]:
        s += f"{pad}  - optional: {', '.join(label(mods, m['opt']))}\n"
    for k in children(mods, m["id"]):
        s += md_node(mods, k, ind + 1)
    return s


def render_md(mods):
    sup, rest = roots(mods)
    s = ("# Mod-Struktur\n\nErzeugt aus `modules/modules.json` durch `python tools/modtree/modtree.py` "
         "(nicht von Hand bearbeiten). Grafische Fassung: `docs/MODSTRUKTUR.html`.\n\n")
    if BASE in mods:
        s += "## Gemeinsame Basis\n\n" + md_node(mods, mods[BASE]) + f"  - required von: {', '.join(used_by(mods, BASE))}\n\n"
    for m in sup:
        s += f"## {m['name']}\n\n" + md_node(mods, m) + "\n"
    if rest:
        s += "## Eigenstaendige Mods\n\n" + "".join(md_node(mods, m) for m in rest)
    return s


def main(argv):
    mods = load()
    files = {OUT_HTML: render_html(mods), OUT_MD: render_md(mods)}
    if "--check" in argv:
        old = [p.name for p, t in files.items() if not p.exists() or p.read_text(encoding="utf-8") != t]
        if old:
            print("veraltet: " + ", ".join(old) + " -> python tools/modtree/modtree.py")
            return 1
        print("modtree ok")
        return 0
    for p, t in files.items():
        p.write_text(t, encoding="utf-8", newline="\n")
        print("geschrieben", p.relative_to(ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
