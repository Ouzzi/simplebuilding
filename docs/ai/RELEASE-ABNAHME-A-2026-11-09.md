# Release-Abnahme A, 09.11.2026 (Branch a-rel10)

Auftrag Besitzer 08.11.: alles noch einmal testen, die Release-Abnahmen selbst machen, Fixbares direkt fixen.
Plan: `docs/ai/PLAN-A-REL10-2026-11-09.md` (Abnahme-Liste nach Mod, 165 Queue-Punkte). Bilder und Bewertung:
`/root/sb-tools-b/previews/a-rel10-preview.html` (hell/dunkel).

Basis: hub `b-bundle159-gen` 3277653e2 (Gate 159). **Abweichung vom Auftrag:** Basis sollte hub/claude-wave1 sein;
Gate 159 war GRÜN, aber „NOT PUSHED“ (Standalone rot, siehe Fix 1), deshalb stand wave1 noch auf d7c6608de.
Die Client-Läufe prüfen trotzdem genau den Release-Stand 3277653e2 (+ die Fixes dieses Branches).

## Läufe (sb-test, xvfb, über /root/sbt)

| Lauf | Ergebnis |
|---|---|
| Gate 159 (3277653e2), `check` + fabric-263 | GRÜN, 1210/1210 |
| Standalone (59 Ziele = Dedicated Server je Mod allein) | 2204/2204 Tests grün, aber **Simple Weather startete nicht** → Fix 1, danach `runSimpleweatherStandaloneGameTest` 5/5 grün |
| `mixinaudit-client-263` | grün, „[mixinaudit] audit done without error“ |
| `rel10-client-263` (neu, `Rel10ClientTest`) | grün, 135 Bilder, 0 Fehler (zweiter Lauf nach Testverbesserung) |
| `vis24all-client-263` (alle Modul-Smokes + Vis24) | 1. Lauf: Abbruch im Tweaks-Smoke → Fix 2; 2. Lauf: Zeitgrenze 2400 s unter Last (155 Bilder bis Simple Mobs); 3. Lauf siehe Nachtrag |
| Echte Maus: `pipe_probe`, `mouse_probe`, `keys_probe` | im 1. Versuch nicht aussagekräftig (lief parallel zu vis24all, Fenster/Seite öffnete nicht, Zeitgrenze); Pipe erneut allein, siehe Nachtrag |
| Landing simplemods.de (4 Seiten, Desktop 1280 + Handy 390, chrome-headless) | ok; Simple Layers fehlte → Fix 3 |

**Regelverstoß (eigener):** Job 4 (Maus-Probes) lief durch die zwei Job-Slots von `heavy.sh` gleichzeitig mit dem
vis24all-Client; sb-test (8 GB) war ~15 min per ssh nicht erreichbar (Last 39). Nichts abgebrochen, beide Läufe endeten
von selbst; danach nur noch ein Client-Lauf je Job-Skript.

## Fixes auf a-rel10

1. **Simple Weather standalone startet nicht** (Release-Blocker „Dedicated Server mit jeder Mod allein“): seit 15ada4862
   hängt `simpleweather` hart von `cloth-config` ab, das Standalone-Setup brachte es nicht mit (`HARD_DEP_NO_CANDIDATE`).
   Fix: `modules.json` → `simpleweather.tests.standalone.devMods = [cloth_config]` (wie Dimensions/Fun). Auf einem echten
   Server braucht Simple Weather Cloth Config (steht in `depends`). Beleg: Standalone-Lauf 5/5 grün.
2. **vis24all brach im Tweaks-Smoke ab**: Karte A der Urkunde (7767dcb65) macht „Rechte“ zum Seiten-Reiter
   (`AbstractButton`), `clickScreenButton` drückt nur `Button`. Smoke klickt den Reiter jetzt mit der Maus. Spieler-UI war
   nicht betroffen. Beleg: Bilder `simpletweaks-deed-editor/-error/-valid/-cooldown` im 2. Lauf.
3. **Landing ohne Simple Layers**: `tools/landing/concepts.py` brach mit `AssertionError ['simplelayers']` ab (Kurztext
   fehlte), Paket-/Discover-Seite waren deshalb auf 59 Mods. Text DE/EN ergänzt, `paket.html`/`discover.html` neu (60 Mods).
   Live-Seite wird erst mit dem nächsten Deploy aktuell. `index.html` (Galerie aus `vorschlag-2.html`) enthält Simple Layers
   weiterhin nicht (statischer Entwurf, Generator `build.py` erzeugt nur Entwürfe) → offen, klein.
4. **WH26-1 Tab-Icon Simple Storage** war die Eichen-Kiste, Wunsch war „Bündel“ → `simplestorage:reinforced_bundle`.
5. Changelog-Entwurf `docs/release/CHANGELOG-simplelayers.md` (fehlte, Mod ist neuer als die Entwürfe).
6. Neuer Test `Rel10ClientTest` + Ziel `rel10-client-263` (Muster Vis24/B09): Kreativ-Tabs, Szenen, Entrindungs-Station,
   Rohr/Presse/Trichter, Trompete, Tooltips, JEI R/U, Titel-/Pausen-Knöpfe, Teleport Wolkenmeer/Backrooms.

## Bewertung je Feature (Screenshots selbst angesehen)

| Feature | Bewertung | Beleg / Anmerkung |
|---|---|---|
| Entrindungs-Station (IG31-59) | ok | leer → 32 Eichenstämme im Käfig (Zahl „32“ darüber) → Eisenaxt: Käfig zeigt entrindete Stämme, Axt 218/250 → leere Hand nimmt 32 entrindete Stämme. Klein: Item-Name der Axt überlappt kurz die „32“. JEI: nur Werkbank-Rezept, „Uses“ zeigt nur den Axt-Tag (keine eigene JEI-Kategorie fürs Entrinden). |
| Gemeißelte Blöcke (IG31-57) | ok | 5 gemeißelte Blöcke + glatte Steinmauer neben Vanilla `chiseled_stone_bricks`, Texturen da, kein Lila-Schwarz. JEI: je Werkbank-Rezept; Steinsägen-Rezept nicht gesehen (Vanilla-Pendants haben eins) → Vorschlag. |
| Dimensions-Schrott (IG31-58) | ok | 3 Splitterblöcke + Terranit/Infernit/Zenithit-Blöcke neben Netherit; Barren und Splitter mit Seltenheitsfarbe im Tooltip; JEI-Rezepte (Barren, Blöcke, Splitter↔Block) vorhanden, Schmelzofen-Reiter vorhanden. |
| Simple Layers Blöcke | ok | Wolkenblock, Wolkenstein, Nebel, gestörtes Grundgestein, Riss, EXIT-Schild, Neonlicht, Tapeten, Teppich, Deckenplatten gerendert; Wolkenblock-Tooltip „-30 % Fallschaden“. |
| Wolkenmeer (Teleport y 40/120/260) | unklar | Inseln und Wolkenschicht sichtbar. Unterseiten/Schattenseiten großer Inseln fast schwarz (RGB 7,7,8), auch nach 200 Ticks: Licht 0 + `ambient_light 0.0`. Besitzer: so gewollt oder etwas Umgebungslicht? |
| Backrooms (Teleport) | ok | gelbe Tapetenräume, Flure, Deckenplatten mit Licht, Teppich; Jade-Hinweis „Yellow Wallpaper“. |
| Rohr-Fenster (ROHR33/IG31-5) | ok (Bild), Maus offen | Kompass N/W/O/S + Oben/Unten, Filter- und Stil-Knopf. Echte-Maus-Probe siehe Nachtrag. |
| Plattenpresse (WH26-3) | ok | Farbstoff-Platz mit Silhouette sichtbar. JEI „Uses“ der Presse zeigt nur Abbau-Tag. |
| Trichter / Verstärkter Trichter | ok | Mod-Rahmen, Filter-Knopf beim verstärkten Trichter. |
| Trompete | ok (Bild) | Hand 1./3. Person, Spielen (Rechtsklick halten). Klang bleibt Besitzer (Hörabnahme). |
| Kreativ-Tabs (IG31-3, 43 Tabs) | ok | Jeder Mod-Tab abgebildet (Anfang/Ende). Regel geprüft wie `CreativeTabPolicy`: keine Mod-Items in Vanilla-Tabs außer Spawn-Eiern (QW11, gewollt), keine Vanilla-Items in Mod-Tabs außer Büchern mit Mod-Verzauberungen. Reihenfolge nicht geprüft (Test liest die Registry-Reihenfolge). |
| WH26-1 Tab-Icons | gefixt | Storage war Kiste → Bündel (Fix 4). |
| JEI (WF25-5, WI26-5) | ok | Verstärkter Rucksack: 8 Rezepte; Mob Drops für Wither-Skelettschädel. Wolkenblock und Trompete ohne Rezept (keins vorgesehen). |
| Free Look (QOL34/IG31-43) | ok | `freelook-before/after`, Fadenkreuz versetzt, Zoom und Zoom-Reset (simplelib-Smoke). |
| Vanilla-Stationen im Mod-Rahmen | ok | Truhe, Doppeltruhe, Fass, Ender-Truhe, Shulker, Trichter, Werfer, Crafter, Esel/Pferd, Amboss, Schleifstein, Steinsäge, Webstuhl, Kartentisch, Schmiedetisch, Händler, Werkbank, Öfen, Braustand, Leuchtfeuer, Zaubertisch. |
| Tastenbelegung (KEYS34) | ok | Liste, Funktion, 60/75/100 %, Layouts DE/UK/FR, Editor. |
| Simple Maps | ok | Karte, Wegpunkte, Konfigurieren, Zoom 1–64, Rein-Zoom, Koordinaten, Rahmen, Locator. „context-menu“/„pick-slot“-Bilder zeigen kein Menü (unklar, Smoke-Zeitpunkt). |
| Claim-Urkunde | ok | Beanspruchen, Karte A, Editor, Fehler, gültig, Abklingzeit, „kein Land mehr“. Klein: Code-Kopf (`# Claim rights …`) bleibt im deutschen Client englisch (Code-Buchstaben B D U C R A M sind englisch). |
| Configs (Money, Models, Fun, Visuals, Sounds, QoL, Dimensions) | ok | EN/DE, keine rohen Schlüssel. |
| Dev-Einträge | ok | Titel: nur Vanilla + „Mods“; Pause: nur Vanilla + „Mods“; Inventar: Stil-Knopf, Models-Tab und **„Dev“** (DevVariants, `DEFAULT_ON = true`, Besitzer schaltet vor dem Upload). Klein: bei 854×480 überdeckt die JEI-Leiste den Dev-/Stil-Knopf teilweise. |
| Landing simplemods.de | ok | Start/Paket/Reise/Discover, Desktop und Handy, Sprachumschalter. Reise: Stufen-Reiter laufen auf dem Handy rechts aus dem Bild (scrollbar, unklar ob gewollt). |

## Release-Checkliste Abschnitt 4 (RELEASE-QOL-OPT-AES.md)

- Dedicated Server je Mod allein: **grün nach Fix 1** (Standalone-Gametest-Server, 59 Ziele; Simple Weather nachgeprüft).
- mixinaudit-Client ohne Fehler: **grün**.
- Dev-Schalter: `DevVariants.DEFAULT_ON = true` unverändert (Besitzer schaltet), sonst keine Dev-Menüs/Testzentralen sichtbar.
- Wiki/Handbuch: Gate-Generatoren grün (checkWiki/Guides im Bündel 159); Handbuch-Bilder im vis24all-Lauf.
- Seitentexte Modrinth/CurseForge: Entwürfe in `docs/release/` (jetzt 60 inkl. Simple Layers), müssen von Hand bearbeitet werden.

## Offen (größer oder Besitzer)

- Wolkenmeer: fast schwarze Inselunterseiten (Licht/Umgebungslicht, Entscheidung Besitzer).
- Landing `index.html`-Galerie ohne Simple Layers; Live-Seite erst nach Deploy aktuell.
- JEI: keine eigene Kategorie für die Entrindungs-Station und die Plattenpresse; gemeißelte Blöcke ohne Steinsägen-Rezept.
- Echte-Maus-Probes (Bau-Werkzeuge, Tasten) unter Last nicht aussagekräftig → einzeln neu laufen lassen.
- Hören (Trompete, Platten, Howler) und Gefühl bleiben Besitzer-Abnahme.
