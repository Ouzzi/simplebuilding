# Konzept Wiki-Überarbeitung + TX22-5 (a-wiki22, 23.10.2026)

Auftrag: Besitzer 21.10. „Wiki-Seite überarbeiten“ (Queue Überblick Punkt 8) und TX22-5 (Texturen-Workflow
Wiki-Dev-Tab → Pixeleditor → Repo). Regel „UI/Optik erst exemplarisch“: hier nur Befunde, Varianten und Plan;
umgesetzt wurden nur eindeutige Fehler (Abschnitt 4). Vorschau mit Screenshots und Varianten:
`/root/sb-tools-a/previews/wiki22-preview.html` (sb-agent, nicht im Repo).

## 1. Audit (Ist-Stand)

Werkzeug: Playwright-Skript `/var/tmp/sb/wikiaudit/audit.cjs` auf sb-test (außerhalb des Repos, Lauf über `/root/sbt`),
14 Ansichten × Desktop 1280 / Handy 400 × hell/dunkel = 56 Läufe, dazu Linkprüfung (259 interne Links) und
Dev-Overlay + Pixeleditor. Ergebnis `report.json` und Screenshots unter `/root/sb-tools-a/wikiaudit/out1` (vorher)
bzw. `out2` (nach den Fixes).

| Bereich | Befund |
|---|---|
| Laden | Jede Ansicht 0,2–0,6 s im LAN (Erstaufruf, ohne Drosselung). Über Cloudflare/Handy ungetestet. |
| Datenmenge | `wiki/data` 23 MB, 97 Dateien. `simplebuilding.js` 6,6 MB wird auf jeder SimpleBuilding-Seite geladen, `textures.js` 1,2 MB auf der Texturseite. Jede `*.js` hat eine gleich große `*.json`-Zwillingsdatei (~11 MB), die nur Werkzeuge (`tools/audit_data.py`, `--check`) lesen, die aber mit ausgeliefert wird. |
| Konsolenfehler | Keine Skriptfehler. Einzige Meldungen: 404 auf Bilder (siehe unten) und `data/ui-shots.js` (gewollt optional, nur lokal erzeugt). |
| Bilder | Vanilla- und Vanilla-abgeleitete Texturen (A8) sind absichtlich nicht im Repo (`.gitignore` Z. 58–90, 317 Einträge). Auf sb-web (wiki-prod und wiki-dev = Git-Checkout) fehlen sie deshalb **dauerhaft**: Rezepte zeigen Text-Kacheln statt Stock/Eisenbarren, Simple Blocks 13 Text-Kacheln (cracked_*), Simple Storage 6. Im Hauptbereich fängt der Fehler-Handler das ab (Text-Kachel), in der Seitenleiste nicht → Bruch-Symbol bei „Machines“ (behoben). |
| Links | 259 interne Links ab Start/Alle Mods/Modulseite/Item/Guides geprüft: 0 kaputt. Keine externen Links. |
| Überlauf | Kein waagrechter Überlauf auf 400 px (alle 14 Ansichten, hell/dunkel). |
| Kontrast | Nach 1dd1071f0 nur noch Minecraft-Nachbildungen < 4,5:1: Stapelzahl `.slot-count` weiß auf #c6c6c6 (1,71, mit Vanilla-Schatten #3f3f3f), Zauber-Stufe `.r-num` #80ff20 auf Pergament (1,36), Galaktisch `.r-run` (3,64). Alle drei sind 1:1 Vanilla-Optik mit Textschatten → bewusst nicht geändert (Messung ohne Schatten). |
| Kopf | Sub-Mod-Wikis zeigten links oben eine leere graue Kachel (behoben: Modul-Symbol aus `modules.js`). |
| Startseite | Eine lange Spalte: Hero, „Mod & compatibility“ mit 25 Namen als Fließtext, Kategorien, 10 Referenz-Kacheln, Fortschritt. Was neu ist und wo man anfängt, sieht man nicht. Der Intro-Text nennt Vorschlaghammer, Bündel/Köcher, schnellere Öfen – die liegen seit der Modulaufteilung in Simple Hammers/Storage/Ovens (Pflicht-Sub-Mods, also nicht falsch, aber irreführend). |
| Alle Mods | Status-Zeile ist deutscher Rohtext aus docs/ai mit Stand 19.10. („Hämmer in wave1 (noch nicht im master, check rot)“) – veraltet und auf EN-Seite deutsch. |
| Modulseiten | Sub-Mod-Start = Titel ohne Bild + Absatz + Guides-Kacheln; Seitenleiste ohne Symbole, „Items · 1“-Zählung uneinheitlich zur SB-Seitenleiste. Kein Bild, keine „Wichtigste Blöcke“. |
| Dev-Overlay | 1956 Stifte auf der Texturseite, Pixeleditor öffnet ohne Fehler. Vanilla-abgeleitete Texturen sperren den Editor mit Hinweis (1dd1071f0). |
| sb-web | Platte 100 % voll (7,8 GB) – Sync steht, Dev/Prod-Wiki zeigen alten Stand. Siehe Memory simplemods-hosting 22.10.: Aufräumen nur durch Besitzer (`pct exec 109`). |

## 2. Prioritäten

| P | Thema | Art | Aufwand |
|---|---|---|---|
| P0 | sb-web-Platte frei machen (tmp_pack), sonst ist jede Wiki-Änderung unsichtbar | Besitzer-Schritt | klein |
| P1 | Vanilla-Bilder: Text-Kacheln wirken wie Fehler. Weg A: auf sb-test erzeugte `minecraft/`- und Derived-PNGs nur ins **Dev**-Wiki (hinter Cloudflare Access) syncen; Prod behält Text-Kacheln, aber als gestaltete Kachel (Icon-Umriss + Name). Weg B: beides nur Text. Rechtlich: nichts davon ins Repo (A8). | Entscheidung Besitzer | mittel |
| P1 | Startseite neu (Varianten Abschnitt 3) | UI, exemplarisch | mittel |
| P1 | Modulseite neu (ein Beispiel: Simple Blocks) | UI, exemplarisch | mittel |
| P1 | TX22-5 Speichern-Kette (Abschnitt 5) | Funktion | groß |
| P2 | Datenmenge: `*.json`-Zwillinge nicht ausliefern (nginx `location ~ ^/data/.*\.json$ { return 404; }` oder Generator schreibt JSON nach `build/`), `simplebuilding.js` aufteilen (Kern + Rezepte/Loot nachladen, Muster `module-loader.js`), gzip/brotli in nginx prüfen | Technik | mittel |
| P2 | Status-Text auf „Alle Mods“: nur Datum + Zahl offen, Fließtext nur DE-Seite oder weg | Text | klein |
| P2 | Intro-Text SimpleBuilding an Modulaufteilung anpassen (Hämmer/Lager/Öfen als „mit Sub-Mods“ benennen) | Text | klein |
| P3 | Seitenleiste der Sub-Mods mit Symbolen wie bei SB | UI | klein |

## 3. Varianten (nur Vorschau, Besitzer wählt)

Startseite (gilt für SimpleBuilding und jedes Sub-Mod-Wiki gleich, Daten aus `modules.js` + Moddaten):

- **S-A „Kompakt“**: Hero mit Suche bleibt, darunter sofort Kategorien-Raster (wie heute), „Mod & compatibility“
  wird eine Zeile mit Chips (Pflicht grün, optional grau, ausklappbar). Kleinste Änderung, gleiche Reihenfolge.
- **S-B „Einstieg“**: Hero mit drei großen Wegen („Ich will bauen“ → Guides, „Was kann ich herstellen“ → Rezepte,
  „Alle Mods“), darunter „Neu in v2.0“ (letzte 6 Items aus Changelog/Guides) und Kategorien. Kompatibilität ganz unten.
- **S-C „Mod-Familie“**: Startseite = Familienbild: SimpleBuilding in der Mitte, Sub-Mods als Kacheln mit Symbol,
  Zähler und Pflicht/optional; Klick öffnet das Sub-Mod-Wiki. Kategorien rutschen in die Seitenleiste.

Modulseite (ein Beispiel, Simple Blocks):

- **M-1**: Kopf mit Modul-Symbol, Ein-Satz-Beschreibung, Chips (Version, MC, Loader, Pflicht von/optional zu),
  dann „Highlights“ (6 Block-Kacheln mit Bild), Guides-Kacheln, Referenz-Zahlen als Kachelzeile wie auf der SB-Startseite.

Nach Abnahme: gewählte Startseite für alle Wikis (eine Funktion `pageOverview`), M-1 erst für Simple Blocks, dann Rest.

## 4. Jetzt behobene eindeutige Fehler (a-wiki22)

- 1dd1071f0: Kontrast hell `--accent` (4,25 → ≥ 4,5:1), Zauber-Slotnamen dunkel (1,82 → 5,5:1), Handy-Kopf
  Sprachwahl abgeschnitten, Pixeleditor bei Vanilla-abgeleiteten Texturen 404 → Hinweis.
- b7cfdfdfb: leere Kategorien („Storage 0“) ausgeblendet, fehlende Kategorie-Symbole nach Modulaufteilung ersetzt.
- 6295910d6: Sub-Mod-Kopf zeigt Modul-Symbol statt leerer Kachel; Seitenleiste blendet fehlende Symbole aus
  statt Bruch-Symbol.

## 5. TX22-5: Textur-Kette Wiki-Dev-Tab → Pixeleditor → Repo

Ist: Dev-Overlay (`tools/zentrale-web/dev-overlay.js`) → Stift/„Bearbeiten“ → Pixeleditor (`wiki/assets/pixeleditor*.js`)
→ **Export als Download** (`<name>.png` + `<name>.layers.json`) → Besitzer legt Dateien von Hand neben das Original
und ruft `python tools/textures/layers.py check` → Commit durch eine Sitzung. Lücke: alles ab „Download“ ist Handarbeit.

Ziel („ohne viel Reden“): Knopf **„Speichern ins Repo“** im Editor, danach nur noch Status.

1. **Editor**: neuer Knopf (nur wenn `window.sbDevHook` da ist, also nur im Dev-Wiki) schickt
   `POST /api/textures` mit `{id, source, png(base64), layers(json), comment}`.
2. **devqueue.py (sb-web, hinter Cloudflare Access)**: prüft Nutzer (`check_auth`), Größe (≤ 256 KB, PNG ≤ 512×512),
   `source` muss exakt ein Eintrag aus `wiki/data/textures.js` sein (keine freien Pfade, kein `..`, nicht
   `build/vanilla-derived/`), legt `/srv/devqueue/textures/<lauf>/` an und einen Queue-Eintrag
   `category: "texture-save"`.
3. **Abholer sb-agent** (`/root/devq/pickup.sh`, läuft schon jede Minute): für `texture-save` **kein Claude-Lauf**,
   sondern ein festes Skript `tools/textures/apply_editor_save.py <lauf-dir>`: Worktree von `claude-wave1`,
   Branch `tex-<id>-<lauf>`, Dateien an `source` schreiben, `layers.py check <datei>` (PNG = Komposition der Ebenen),
   Commit „tex(<id>): Editor-Speicherung von <nutzer>“, `git push /root/sb.git`, Status `ready` per PATCH zurück.
4. **Übernahme**: Hauptsitzung sammelt `tex-*`-Branches ins nächste Bündel (Wiki/Texturen-Generator auf sb-test,
   Gate, master). Optional später: eigener kleiner Gate-Weg nur für Textur-Branches (`checkAtlases` + `layers.py check`).
5. **Rückmeldung**: Overlay zeigt am Stift den Status aus `/api/queue` (gespeichert → im Branch → im master).

Sicherheit: nur Access-Nutzer, Pfad-Whitelist aus `textures.js`, PNG wird in Python dekodiert und neu kodiert
(keine fremden Chunks), Größenlimit, Vanilla-abgeleitete gesperrt, keine Shell-Interpolation von Nutzertext
(Kommentar nur in Commit-Nachricht via `-F`-Datei).

Offene Entscheidung (Besitzer): Soll eine Speicherung **sofort** in den nächsten Gate-Lauf (ohne Sitzung) oder erst
mit dem nächsten Bündel? Vorschlag: mit dem Bündel (Gate bleibt bei der Hauptsitzung, NUR-26.3-Fabric-Regel gilt).

Umsetzungsreihenfolge nach Abnahme: (1) Endpunkt + Tests `tools/zentrale-web/tests`, (2) apply-Skript + Test mit
Probe-Textur, (3) Editor-Knopf + `pixeleditor_ui.cjs`-Fall, (4) Pickup-Zweig, (5) Live-Klicktest im Dev-Wiki.
