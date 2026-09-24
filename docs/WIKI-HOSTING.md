# Wiki: immer aktuell, kostenlos gehostet

Das Wiki (`wiki/index.html` + `wiki/data/`) ist eine rein statische Seite: CSS und JS stehen in
der HTML-Datei, geladen wird nur `data/simplebuilding.js`, Texturen kommen relativ aus
`assets/textures/…`, navigiert wird über `#/…`. Es braucht also keinen Server-Code und läuft auch
unter einem Unterpfad wie `https://ouzzi.github.io/simplebuilding/`.

## Was dafür sorgt, dass es aktuell bleibt

| Wo | Was passiert | Abschalten |
|---|---|---|
| `./gradlew runDatagen` | danach läuft automatisch `generateWiki` (= `python wiki/generate.py`). Wer eine Konstante ändert und Datagen laufen lässt, bekommt Mod-Daten **und** Wiki neu; `git status` zeigt beides. Schlägt Datagen fehl, wird kein Wiki gebaut. | `-PskipWiki` |
| `./gradlew check` / `build` | `checkWiki` (= `python wiki/generate.py --check`) schlägt fehl, wenn `wiki/data` veraltet ist oder Prosa fehlt. | `-PskipWiki` |
| `git commit` (optional) | der Hook `tools/git-hooks/pre-commit` führt `--check` aus und hält den Commit an, wenn das Wiki nicht passt. | `git commit --no-verify` |
| GitHub Actions (`.github/workflows/wiki.yml`) | bei jedem Push auf `master` und jedem Pull Request: `--check`, dann „Neu erzeugen ändert nichts" (fängt auch die `.js` und die Texturkopien ab), dann `tools/wiki_site.py`; parallel der Sprachdatei-Test. Nur auf `master` und nur bei grüner Prüfung wird veröffentlicht. | – |

Ein anderes Python als `python`: `-PwikiPython=<pfad>` (Gradle) bzw. `WIKI_PYTHON=<pfad>` (Hook).

### Pre-commit-Hook einschalten (einmal pro Klon)

```bash
git config core.hooksPath tools/git-hooks
```

Ausschalten: `git config --unset core.hooksPath`. Der Hook prüft den **Arbeitsbaum**, nicht den
Index – nicht gestagte Änderungen zählen mit. Ohne Python lässt er den Commit durch.

### Wenn CI rot ist

Die Seite bleibt dann auf der letzten grünen Fassung stehen. Beheben:

```bash
python wiki/generate.py          # fehlende Prosa vorher in wiki/manual.json ergänzen (en + de)
git add wiki && git commit -m "wiki: neu erzeugt"
git push
```

## GitHub Pages einrichten (einmalig)

1. Das Repository muss **öffentlich** sein – GitHub Pages für private Repos kostet (dann siehe
   „Ausweichen" unten).
2. Auf GitHub: **Settings → Pages → Build and deployment → Source: „GitHub Actions"** wählen.
3. Auf `master` pushen (oder unter **Actions → Wiki → Run workflow** manuell starten).
4. Die Adresse steht danach im Lauf beim Job „Auf GitHub Pages veröffentlichen" und unter
   Settings → Pages, typischerweise `https://ouzzi.github.io/simplebuilding/`.

Die Umgebung `github-pages` legt GitHub selbst an. Hat sie eine Branch-Regel (Settings →
Environments → github-pages), muss `master` darin erlaubt sein; andere Branches veröffentlicht der
Workflow ohnehin nie.

## Was veröffentlicht wird – und was nicht

`tools/wiki_site.py` stellt nach `build/wiki-site/` genau das zusammen, was die Seite lädt:
`index.html`, `data/simplebuilding.js`, die referenzierten eigenen Texturen und eine leere
`.nojekyll`. Vorher prüft es, dass `index.html` nichts Absolutes oder Fremdes lädt, dass die `.js`
dasselbe Objekt enthält wie die `.json` und dass jede Textur in exakt dieser Schreibweise existiert
(Windows ignoriert Groß/Klein, die Hosts nicht).

Nicht veröffentlicht werden:
- **Mojangs Vanilla-Texturen** (`wiki/assets/textures/minecraft/`). Sie liegen absichtlich nicht im
  Repository, und die Seite fängt fehlende Bilder ab: Vanilla-Zutaten erscheinen als Textkachel.
  In CI existieren sie ohnehin nicht (kein Client-Jar), und das Skript kopiert sie auch lokal nie.
- `generate.py`, `manual.json`, `README.md`, `HANDOFF.md` und `data/simplebuilding.json`.
- Texturen, auf die nichts verweist (das Skript meldet sie; aktuell die vier Truhen-PNGs unter
  `wiki/assets/textures/{block,item}/`).

Lokal ansehen, was online ginge:

```bash
python tools/wiki_site.py                                   # -> build/wiki-site
python -m http.server 8080 --directory build/wiki-site      # http://localhost:8080/
```

In `index.html` hängt das Skript `?v=<hash>` an die Datendatei. Das verhindert, dass eine **neue**
`index.html` eine alte, zwischengespeicherte Datendatei bekommt; GitHub Pages speichert Dateien
10 Minuten zwischen.

## Ausweichen: Cloudflare Pages oder Netlify (auch für private Repos kostenlos)

Beide bauen direkt aus dem GitHub-Repository. Einstellungen:

| Feld | Wert |
|---|---|
| Build-Befehl | `python wiki/generate.py --check && python tools/wiki_site.py --out build/wiki-site` |
| Ausgabeverzeichnis | `build/wiki-site` |
| Produktions-Branch | `master` |
| Umgebungsvariable | `PYTHON_VERSION=3.12` |

Schlägt `--check` fehl, schlägt der Build fehl und die vorige Fassung bleibt online – dasselbe Gate
wie bei GitHub Pages. Achtung: beide bauen standardmäßig auch **Vorschauen** für andere Branches
und Pull Requests. Wer nur `master` online haben will, schaltet die Vorschau-Deploys in den
Projekteinstellungen ab. Den GitHub-Pages-Job im Workflow kann man dann lassen (er scheitert nur,
solange Pages nicht eingerichtet ist) oder den `deploy`-Job aus `.github/workflows/wiki.yml` löschen.

## Grenzen

- `--check` vergleicht Daten und prüft, **dass** es Prosa gibt – nicht, ob die Prosa noch stimmt.
  Das bleibt Handarbeit (siehe `wiki/README.md`).
- Gebaut wird aus den **committeten** Datagen-Ausgaben. Wer eine Java-Konstante ändert und
  `runDatagen` nicht laufen lässt, fällt keinem Check auf; genau dafür hängt `generateWiki` jetzt an
  `runDatagen`.
- Geprüft und veröffentlicht wird die 26.2-Linie (`python wiki/generate.py` ohne `--line`).
- Die Action-Versionen im Workflow (`checkout@v4`, `setup-python@v5`, `setup-java@v4`,
  `upload-pages-artifact@v3`, `deploy-pages@v4`) sind bewusst ältere, sicher vorhandene Hauptversionen;
  sie lassen sich später anheben.
