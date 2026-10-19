"""Placeholder / missing texture audit of built jars, per creative tab (b-texfix20, owner 19.10.).

The owner saw grey checkers instead of textures in the SimpleMachines tab: a build without Python + Pillow + numpy
(or without the 26.3 client jar) ships grey placeholders for the Vanilla-derived textures (AGENTS.md rule 7,
tools/textures/vanilla_derived.py). This tool looks at what a build really ships:

  scan   every PNG under assets/*/textures/ of the given jars: placeholder checker (vanilla_derived.placeholder_png or
         the Gradle fallback, both "two colours, the dark one half the light one, 8x8 fields") or the jar's marker
         assets/<ns>/vanilla_derived_placeholders.txt. Needs Pillow only.
  audit  (--html/--md) every entry of every creative tab: item definition -> models -> textures (wiki/model_render.py
         resolves them like the game), each texture ok / placeholder / missing, plus an offline icon per entry and one
         contact sheet per tab. Tabs come from the integration gametest dump (SB_CREATIVE_TAB_DUMP, see
         integration CrossModGameTest#dumpCreativeTabs); without one, entries are grouped by namespace.
         Needs Pillow and numpy and the 26.3 client jar (for minecraft: models/textures).

The HTML report shows Mojang pixels (Vanilla items in the tabs, the derived textures): never commit it (rule 7), publish
it privately. --md writes the text-only summary.

Run: python3.12 tools/textures/placeholder_audit.py --jars <jar>... [--fail-on-placeholder]
     python3.12 tools/textures/placeholder_audit.py --jars <jar>... --tabs tabs.json --html report.html --md report.md
Exit 1 with --fail-on-placeholder when a jar carries a placeholder texture or marker (release check).
"""
from __future__ import annotations

import argparse
import base64
import glob
import html
import io
import json
import sys
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import vanilla_derived  # noqa: E402

MARKER = vanilla_derived.MARKER


def is_placeholder(data: bytes) -> bool:
    """True for a placeholder checker: exactly two opaque colours on fields of max(1, w // 8) pixels, the dark one half
    the light one per channel (vanilla_derived.placeholder_png and the Gradle fallback 0x8A8A8A/0x454545)."""
    from PIL import Image
    try:
        im = Image.open(io.BytesIO(data)).convert('RGBA')
    except Exception:  # noqa: BLE001 - an unreadable PNG is the game's problem, not a placeholder
        return False
    w, h = im.size
    step = max(1, w // 8)
    if w < 2 or h < 2:
        return False
    px = im.load()
    colours = {}
    for y in range(h):
        for x in range(w):
            c = px[x, y]
            if colours.setdefault(((x // step) + (y // step)) % 2, c) != c:
                return False
    a, b = colours.get(1), colours.get(0)
    if a is None or b is None or a == b or a[3] != 255 or b[3] != 255:
        return False
    light, dark = (a, b) if sum(a[:3]) >= sum(b[:3]) else (b, a)
    return all(dark[i] == light[i] // 2 for i in range(3))


def scan_jar(path: Path) -> dict:
    """{'jar', 'placeholders': [entry], 'markers': [entry], 'textures': n} for one jar (or resource folder)."""
    out = {'jar': str(path), 'placeholders': [], 'markers': [], 'textures': 0}
    if Path(path).is_dir():
        names = [p.relative_to(path).as_posix() for p in Path(path).rglob('*') if p.is_file()]
        read = lambda n: (Path(path) / n).read_bytes()  # noqa: E731
    else:
        z = zipfile.ZipFile(path)
        names = z.namelist()
        read = z.read
    for name in names:
        if name.startswith('assets/') and name.endswith('/' + MARKER):
            out['markers'].append(name)
        elif name.startswith('assets/') and '/textures/' in name and name.endswith('.png'):
            out['textures'] += 1
            if is_placeholder(read(name)):
                out['placeholders'].append(name)
    return out


# ---------------------------------------------------------------------------------------------------- tab audit

class JarAssets:
    """namespace -> asset roots of the built jars for wiki/model_render.IconRenderer (jars are unpacked into a temporary
    folder, assets/ only; a folder argument is used as it is)."""

    def __init__(self, jars: list[Path]):
        import tempfile
        self.own: dict[str, list[Path]] = {}
        self._tmp = tempfile.TemporaryDirectory(prefix='sb-placeholder-audit-')
        for i, jar in enumerate(jars):
            base = Path(jar)
            if not base.is_dir():
                base = Path(self._tmp.name) / str(i)
                with zipfile.ZipFile(jar) as z:
                    z.extractall(base, [n for n in z.namelist() if n.startswith('assets/') and '..' not in n])
            for ns_dir in sorted((base / 'assets').glob('*')):
                if ns_dir.is_dir() and ns_dir.name != 'minecraft':
                    self.own.setdefault(ns_dir.name, []).append(ns_dir)


def texture_refs(renderer, item_model: str) -> list[str]:
    """Texture ids an item shows in a slot (default branch of conditions/selects), as the game resolves them."""
    from model_render import item_parts
    definition = renderer.assets.json('items', item_model)
    refs: list[str] = []
    if definition is None:
        return refs
    for kind, payload, _ in item_parts(definition.get('model')):
        if kind == 'special':
            model = (payload or {}).get('model') or {}
            if str(model.get('type', '')).replace('minecraft:', '') == 'chest':
                texture = str(model.get('texture', 'normal'))
                ns, name = texture.split(':', 1) if ':' in texture else ('minecraft', texture)
                refs.append(f'{ns}:entity/chest/{name}')
            base = payload.get('base') if isinstance(payload, dict) else None
            if base:
                refs += model_texture_refs(renderer, base)
            continue
        if kind == 'model' and payload:
            refs += model_texture_refs(renderer, payload)
    return list(dict.fromkeys(refs))


def model_texture_refs(renderer, model_ref: str) -> list[str]:
    model = renderer.resolve_model(model_ref if ':' in model_ref else 'minecraft:' + model_ref)
    if not model:
        return []
    refs = []
    for key in model['textures']:
        ref = renderer.texture_ref(model['textures'], '#' + key)
        if ref:
            refs.append(ref if ':' in ref else 'minecraft:' + ref)
    return refs


def classify(renderer, ref: str) -> str:
    ns, path = ref.split(':', 1)
    data = renderer.assets._read(ns, f'textures/{path}.png')
    if data is None:
        return 'ok' if renderer.assets._permuted(ref) else 'missing'
    return 'placeholder' if ns != 'minecraft' and is_placeholder(data) else 'ok'


def icon(renderer, item_model: str, refs: list[str]):
    """32x32 icon: the wiki renderer's 3D/overlay render, else the stacked flat layers, else None."""
    from PIL import Image
    try:
        im = renderer.render_item(item_model)
    except Exception:  # noqa: BLE001 - one broken model must not stop the audit
        im = None
    if im is None:
        layers = []
        definition = renderer.assets.json('items', item_model) or {}
        from model_render import item_parts
        for kind, payload, _ in item_parts(definition.get('model')):
            model = renderer.resolve_model(payload) if kind == 'model' and payload else None
            if model and model['flat']:
                for i in range(8):
                    ref = renderer.texture_ref(model['textures'], f'#layer{i}')
                    if ref:
                        layers.append(ref)
        canvas = None
        for ref in layers or refs[:1]:
            arr = renderer.assets.texture(ref)
            if arr is None:
                continue
            layer = Image.fromarray((arr * 255.0 + 0.5).astype('uint8'), 'RGBA')
            canvas = layer if canvas is None else Image.alpha_composite(canvas, layer.resize(canvas.size, Image.NEAREST))
        im = canvas
    if im is None:
        return None
    return im.convert('RGBA').resize((32, 32), Image.NEAREST if im.width <= 32 else Image.LANCZOS)


def load_tabs(tabs_file: Path | None, assets: JarAssets) -> list[dict]:
    if tabs_file:
        return json.loads(Path(tabs_file).read_text(encoding='utf-8'))
    tabs = []
    for ns, roots in sorted(assets.own.items()):
        ids = set()
        for root in roots:
            folder = root / 'items'
            if folder.exists():
                ids |= {f'{ns}:{p.name[:-5]}' for p in folder.iterdir() if p.name.endswith('.json')}
        if ids:
            tabs.append({'tab': f'{ns} (all item definitions, no tab dump)', 'items': [{'id': i} for i in sorted(ids)]})
    return tabs


def contact_sheet(cells: list[tuple]) -> 'Image.Image':
    """Creative-inventory-like grid, 9 per row; red frame = placeholder, magenta = missing/unrendered."""
    from PIL import Image, ImageDraw
    cols, slot = 9, 36
    rows = max(1, (len(cells) + cols - 1) // cols)
    sheet = Image.new('RGBA', (cols * slot + 2, rows * slot + 2), (198, 198, 198, 255))
    d = ImageDraw.Draw(sheet)
    for i, (im, status) in enumerate(cells):
        x, y = 1 + (i % cols) * slot, 1 + (i // cols) * slot
        d.rectangle((x, y, x + slot - 2, y + slot - 2), fill=(139, 139, 139, 255))
        if im is not None:
            sheet.alpha_composite(im, (x + 1, y + 1))
        colour = {'placeholder': (230, 30, 30, 255), 'missing': (230, 0, 230, 255), 'noicon': (230, 0, 230, 255)}.get(status)
        if colour:
            d.rectangle((x, y, x + slot - 2, y + slot - 2), outline=colour, width=2)
    return sheet


def audit(jars: list[Path], client_jar: Path | None, tabs_file: Path | None) -> dict:
    sys.path.insert(0, str(ROOT / 'wiki'))
    from model_render import IconRenderer
    assets = JarAssets(jars)
    renderer = IconRenderer({}, client_jar)
    renderer.assets.own = assets.own
    result = {'tabs': [], 'client_jar': str(client_jar) if client_jar else None}
    for tab in load_tabs(tabs_file, assets):
        rows, cells = [], []
        for entry in tab['items']:
            item_id = entry['id']
            model = entry.get('model') or item_id
            if item_id.startswith('minecraft:') and model.startswith('minecraft:'):
                status, problems = 'ok', []  # Vanilla items in Vanilla or mod tabs: drawn from the client jar
            elif renderer.assets.json('items', model) is None:
                status, problems = 'missing', [f'no item definition assets/{model.replace(":", "/items/", 1)}.json']
            else:
                refs = texture_refs(renderer, model)
                states = {ref: classify(renderer, ref) for ref in refs}
                problems = [f'{s}: {r}' for r, s in states.items() if s != 'ok']
                status = 'placeholder' if 'placeholder' in states.values() else (
                    'missing' if 'missing' in states.values() else 'ok')
            im = icon(renderer, model, [])
            cells.append((im, 'noicon' if im is None and status == 'ok' else status))
            rows.append({'id': item_id, 'model': model, 'status': status, 'problems': problems, 'icon': im is not None})
        result['tabs'].append({'tab': tab['tab'], 'entries': rows, 'sheet': contact_sheet(cells)})
    return result


def counts(entries: list[dict]) -> dict:
    out = {'ok': 0, 'placeholder': 0, 'missing': 0}
    for e in entries:
        out[e['status']] += 1
    out['noicon'] = sum(1 for e in entries if not e['icon'])
    return out


def write_md(result: dict, scans: list[dict], out: Path) -> None:
    lines = ['# Platzhalter-/Textur-Audit der Kreativtabs', '',
             'Erzeugt von `tools/textures/placeholder_audit.py` (b-texfix20). Grau = Platzhalter der Vanilla-derived-Texturen,',
             '„fehlt“ = Textur/Item-Definition nicht im Jar. Kontaktbögen nur in der HTML-Seite (Mojang-Pixel, nicht im Repo).', '',
             '## Jars', '', '| Jar | Texturen | Platzhalter | Marker |', '|---|---:|---:|---|']
    for s in scans:
        lines.append(f"| `{Path(s['jar']).name}` | {s['textures']} | {len(s['placeholders'])} | {', '.join(s['markers']) or '–'} |")
    if result:
        lines += ['', '## Kreativtabs', '', '| Tab | Einträge | ok | Platzhalter | fehlt | ohne Icon |', '|---|---:|---:|---:|---:|---:|']
        for tab in result['tabs']:
            c = counts(tab['entries'])
            lines.append(f"| `{tab['tab']}` | {len(tab['entries'])} | {c['ok']} | {c['placeholder']} | {c['missing']} | {c['noicon']} |")
        bad = [(t['tab'], e) for t in result['tabs'] for e in t['entries'] if e['status'] != 'ok']
        lines += ['', '## Befunde', '']
        lines += [f"- `{tab}` `{e['id']}`: {'; '.join(e['problems'])}" for tab, e in bad] or ['- keine']
    out.write_text('\n'.join(lines) + '\n', encoding='utf-8')


def _png_uri(im) -> str:
    buf = io.BytesIO()
    im.save(buf, format='PNG', optimize=True)
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def write_html(result: dict, scans: list[dict], out: Path, title: str) -> None:
    e = html.escape
    total = {'ok': 0, 'placeholder': 0, 'missing': 0, 'noicon': 0}
    for tab in result['tabs']:
        for k, v in counts(tab['entries']).items():
            total[k] += v
    jar_rows = ''.join(f"<tr><td>{e(Path(s['jar']).name)}</td><td>{s['textures']}</td><td class=\"{'bad' if s['placeholders'] else ''}\">"
                       f"{len(s['placeholders'])}</td><td>{e(', '.join(s['markers']) or '–')}</td></tr>" for s in scans)
    tabs_html = []
    for tab in result['tabs']:
        c = counts(tab['entries'])
        bad = [x for x in tab['entries'] if x['status'] != 'ok']
        items = ''.join(f"<li><code>{e(x['id'])}</code> – {e('; '.join(x['problems']))}</li>" for x in bad)
        tabs_html.append(
            f"<section><h2>{e(tab['tab'])}</h2><p class=\"meta\">{len(tab['entries'])} Einträge · {c['ok']} ok · "
            f"<b class=\"{'bad' if c['placeholder'] else ''}\">{c['placeholder']} Platzhalter</b> · {c['missing']} fehlt · "
            f"{c['noicon']} ohne Offline-Icon</p><img class=\"sheet\" alt=\"Kontaktbogen {e(tab['tab'])}\" "
            f"src=\"{_png_uri(tab['sheet'])}\">{'<ul>' + items + '</ul>' if items else ''}</section>")
    page = f"""<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{e(title)}</title><style>
:root{{--bg:#f6f5f2;--fg:#1d1d1b;--muted:#5d5d58;--card:#fff;--line:#d9d6cf;--bad:#b3261e}}
@media (prefers-color-scheme:dark){{:root:not([data-theme="light"]){{--bg:#1b1b1a;--fg:#ecebe6;--muted:#a9a8a0;--card:#252523;--line:#3a3a37;--bad:#ff8a80}}}}
:root[data-theme="dark"]{{--bg:#1b1b1a;--fg:#ecebe6;--muted:#a9a8a0;--card:#252523;--line:#3a3a37;--bad:#ff8a80}}
body{{margin:0;background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif}}
main{{max-width:980px;margin:0 auto;padding:16px}}
section{{background:var(--card);border:1px solid var(--line);border-radius:8px;padding:12px 16px;margin:16px 0}}
h1{{font-size:1.5rem}} h2{{font-size:1.1rem;margin:.2em 0}} .meta{{color:var(--muted);margin:.2em 0 .6em}}
.bad{{color:var(--bad)}} .sheet{{image-rendering:pixelated;max-width:100%;height:auto;border:1px solid var(--line)}}
table{{border-collapse:collapse;width:100%;font-size:.9rem}} td,th{{border-bottom:1px solid var(--line);padding:4px 6px;text-align:left}}
.scroll{{overflow-x:auto}} code{{font-size:.85em;word-break:break-all}}
</style></head><body><main>
<h1>{e(title)}</h1>
<p>Jedes Kreativtab als Kontaktbogen (Icons offline aus Modellen/Texturen der gebauten Jars gerendert, wie das Wiki).
Roter Rahmen = graue Platzhalter-Textur, magenta = Textur/Definition fehlt oder kein Offline-Icon (Spezial-Renderer).</p>
<p><b>Gesamt:</b> {sum(len(t['entries']) for t in result['tabs'])} Einträge · {total['ok']} ok ·
<b class="{'bad' if total['placeholder'] else ''}">{total['placeholder']} Platzhalter</b> · {total['missing']} fehlt ·
{total['noicon']} ohne Offline-Icon</p>
<section><h2>Jars</h2><div class="scroll"><table><tr><th>Jar</th><th>Texturen</th><th>Platzhalter</th><th>Marker</th></tr>{jar_rows}</table></div></section>
{''.join(tabs_html)}
</main></body></html>"""
    out.write_text(page, encoding='utf-8')


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--jars', nargs='+', required=True, help='built jars or resource folders (globs allowed)')
    ap.add_argument('--client-jar', help='26.3 client jar (default: vanilla_derived.find_jar())')
    ap.add_argument('--tabs', type=Path, help='creative tab dump (SB_CREATIVE_TAB_DUMP of the integration gametest)')
    ap.add_argument('--html', type=Path)
    ap.add_argument('--md', type=Path)
    ap.add_argument('--title', default='Platzhalter-Audit der Kreativtabs')
    ap.add_argument('--fail-on-placeholder', action='store_true')
    a = ap.parse_args(argv)
    jars = [Path(p) for pattern in a.jars for p in (sorted(glob.glob(pattern)) or [pattern])]
    scans = [scan_jar(j) for j in jars]
    for s in scans:
        flag = 'PLACEHOLDER' if s['placeholders'] or s['markers'] else 'ok'
        print(f"{flag:11} {Path(s['jar']).name}: {s['textures']} textures, {len(s['placeholders'])} placeholders"
              + (f", marker {', '.join(s['markers'])}" if s['markers'] else ''))
        for name in s['placeholders'][:20]:
            print(f'    {name}')
    result = None
    if a.html or a.md:
        client = Path(a.client_jar) if a.client_jar else vanilla_derived.find_jar()
        result = audit(jars, client, a.tabs)
        for tab in result['tabs']:
            c = counts(tab['entries'])
            print(f"tab {tab['tab']}: {len(tab['entries'])} entries, {c['placeholder']} placeholder, {c['missing']} missing, "
                  f"{c['noicon']} without offline icon")
        if a.html:
            write_html(result, scans, a.html, a.title)
        if a.md:
            write_md(result, scans, a.md)
    bad = sum(len(s['placeholders']) + len(s['markers']) for s in scans)
    print(f'placeholder_audit: {len(scans)} jars, {bad} placeholder textures/markers')
    return 1 if a.fail_on_placeholder and bad else 0


if __name__ == '__main__':
    sys.exit(main())
