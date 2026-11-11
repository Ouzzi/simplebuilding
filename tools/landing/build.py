#!/usr/bin/env python3
"""Landing simplemods.de: Einstieg zum Bauen und Prüfen (N32, 11.11.).

  python3 tools/landing/build.py          alle vier Seiten (index/paket/reise/discover.html) über concepts.py neu schreiben
  python3 tools/landing/build.py --check  schreibt nichts, braucht kein ffmpeg:
      Exit 2 = Mod aus modules/modules.json ohne Landing-Kachel (Kurztext, Icon, Baukasten-Eintrag, Galerie-Bild)
               oder ein Mod zeigt ein fremdes bzw. ein mit einer anderen Mod geteiltes Icon (früherer Fehler: alle Kacheln
               mit dem SimpleBuilding-Icon; Simple Layers und Simple Pads hatten dieselbe Platzhalter-Raute)
      Exit 1 = Seiten passen nicht zum Generator-Stand (python3 tools/landing/build.py laufen lassen)
      Exit 0 = alles aktuell
Der alte Entwurf A (dist/index.html) liegt nur noch als archiv/build-entwuerfe.py vor.
"""
import base64, glob, json, re, subprocess, sys
from pathlib import Path

D = Path(__file__).resolve().parent
R = D.parents[1]


def tiles():
    """Fehlerliste: jede Mod aus modules.json braucht Kurztext, eigenes Icon, Baukasten-Eintrag und Galerie-Bild."""
    mods = [m['id'] for m in json.loads((R / 'modules/modules.json').read_text(encoding='utf-8'))['modules']
            if m['id'] != 'wiringexample']
    ovr = json.loads((D / 'overrides.json').read_text(encoding='utf-8')).get('mods') or {}
    page = lambda f: (D / f).read_text(encoding='utf-8') if (D / f).exists() else ''
    m = re.search(r'var D=(\{.*?\});\n\(function', page('paket.html'), re.S)
    data = json.loads(m.group(1)) if m else {'mods': {}, 'icons': {}}
    idx = page('index.html')
    gallery = dict(re.findall(r'<img data-ico="([a-z0-9_]+)" src="([^"]*)" title=', idx))
    errs = []
    for i in mods:
        own = glob.glob(str(R / f'modules/{i}/shared/resources/assets/*/icon_128.png')) if i != 'simplebuilding' else \
            [str(R / 'src/main/resources/assets/simplebuilding/icon_128.png')]
        want = 'data:image/png;base64,' + base64.b64encode(Path(own[0]).read_bytes()).decode() if own else None
        if not own:
            errs.append(f'{i}: kein icon_128.png')
        if i not in data['mods']:
            errs.append(f'{i}: fehlt im Baukasten (paket.html)')
        if not data['icons'].get(i):
            errs.append(f'{i}: kein Icon im Baukasten')
        elif want and i not in ovr and data['icons'][i] != want:
            errs.append(f'{i}: Baukasten zeigt ein fremdes Icon')
        if i != 'simplebuilding':  # die Galerie zeigt SimpleBuilding als Gruppe, nicht als Bild
            if not gallery.get(i):
                errs.append(f'{i}: fehlt in der Galerie (index.html)')
            elif want and i not in ovr and gallery[i] != want:
                errs.append(f'{i}: Galerie zeigt ein fremdes Icon')
    seen = {}
    for i, u in sorted(data['icons'].items()):  # zwei Mods mit demselben Bild (z. B. Platzhalter-Icon) fallen sofort auf
        if u in seen:
            errs.append(f'{i}: gleiches Icon wie {seen[u]} (overrides.json mods.<id>.icon oder eigenes icon_128.png)')
        seen.setdefault(u, i)
    return errs


def main():
    if '--check' not in sys.argv:
        return subprocess.run([sys.executable, str(D / 'concepts.py')] + sys.argv[1:]).returncode
    r = subprocess.run([sys.executable, str(D / 'concepts.py'), '--check'], capture_output=True, text=True)
    if r.returncode:  # z. B. AssertionError: ['simplevault'] = Kurztext fehlt in concepts.py TXT
        print('landing: Generator bricht ab:', (r.stderr.strip().splitlines() or ['?'])[-1])
        return 2
    errs = tiles()
    for e in errs:
        print('landing: Kachel', e)
    if errs:
        return 2
    out = r.stdout.strip().splitlines()[-1] if r.stdout.strip() else ''
    print(out or 'landing: ?')
    return 1 if 'veraltet' in out else 0


if __name__ == '__main__':
    sys.exit(main())
