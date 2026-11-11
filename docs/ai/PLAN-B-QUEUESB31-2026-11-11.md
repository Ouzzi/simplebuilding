# Plan B-QUEUESB31 (11.11.2026): Queue-[~] „nur Client-Sicht“ gegen Client-Belege

Branch `b-queuesb31`, Basis hub/b-bundle181-gen. Punkt #8 aus `/root/sb-tools-b/relsb31-lücken.md`. Nur `.claude/QUEUE.md` + dieser Plan; kein sbt (Regel 17), keine Läufe.

## Vorgehen
1. Belege gesammelt: `rel10-client-263` + `vis24all-client-263` (A, `docs/ai/RELEASE-ABNAHME-A-2026-11-09.md`, Bilder `/root/sb-tools-b/previews/a-rel10/`), compat26 (`/root/sb-tools-b/compat26-client/` shots1/shots2, mouse.log, keys.log, compat2.log), b-client09 (`previews/b-client09/`, 28 Bilder, faults 0), guidechap/guide15 (`b-guide15-sbt1.log`: ok 62, faults 0), Probes pipe_probe (30/30, A), mouse_probe/keys_probe (RC=0, 11.11.).
2. 210 Queue-Zeilen mit [~] und „Client“ durchgesehen (≈150 verschiedene IDs, viele doppelt). Entscheidung je Zeile: [x] nur, wenn der Rest allein „Client-Sicht“ war und ein Bild/Lauf genau das Verhalten zeigt. Besitzer-Abnahmen/Optik-Wahlen bleiben [~]. Bilder stichprobenartig selbst angesehen (Mob-Köpfe-Tab, Rohr-Fenster, Leuchtfeuer-Fenster, Keys 75/100 %, Giesserei-Pult, Shops, Challenges, Free Look).
3. Jede Zeile bekommt am Ende `[11.11. b-queuesb31: …]` mit Beleg oder Rest. Zeilen mit früherer a-rel10-Notiz (Z. 23, 103, 112, 114, 127, 134, 594, 1075, 1096, 1161, 1184, 1187, 1234) blieben unverändert.

## Ergebnis
- **[x] gesetzt: 10 Zeilen** (siehe Tabelle 1)
- teils belegt, Rest benannt: 99 Zeilen (Tabelle 2)
- kein Beleg vorhanden, Rest unverändert: 91 Zeilen (Tabelle 3, Pauschalnotiz)

Grenzen: Bilder sind Smoke-Standbilder auf Stand 3277653e2 (a-rel10) bzw. 84ae74108 (compat26); Animationen (Free-Look-Rückdrehung), Hören und Optik-Gefühl sind damit nicht belegt. Spätere Branches (z. B. b-clouds20, b-relui27) sind erst im nächsten Client-Lauf (Bündel) sichtbar.

## Tabelle 1: [~] → [x]

| Queue-Z. | ID | Beleg |
|---|---|---|
| 26 | (B, b-compat26, 10.11.; vorbereitet, Lau | Client-Sicht belegt: compat26-client-263 Lauf 2 11.11. 10:58 COMPAT26_RC=0 (Lithium+JEI+Mod Menu+61 Simple-Mods geladen, JEI R/U, Config-Screens, Bilder /root/sb-tools-b/compat26-client/shots2/ 0000-0013, Log compat2.log; Lauf 1 rot, Lauf 2 gruen), mouse_probe MOUSEPROBE_RC=0 + keys_probe KEYSPROBE_RC=0 (mouse.log/keys.log); Vorschau b-compat26-preview.html |
| 81 | IG31-39 | Client-Sicht belegt: Tab-Bild simplemobheads-creative-tab (a-rel10 rel10-client-263 (135 Bilder, 0 Fehler; /root/sb-tools-b/previews/a-rel10/)): Feld-Raster gefuellt, Koepfe im Feld; simplemobheads-slotfit-00/-items-00 ohne Verschiebung |
| 137 | IG31-5 | Client-Sicht belegt: Rohr-Fenster mit Kompass N/W/O/S + Oben/Unten (rel10-068/-070 screen-simplestorage-pipe, a-rel10 rel10-client-263 (135 Bilder, 0 Fehler; /root/sb-tools-b/previews/a-rel10/)) + pipe_probe 30/30 echte Maus GUI 2+3 (RELEASE-ABNAHME-A-2026-11-09.md); Sechseck wurde durch ROHR33-Kompass ersetzt |
| 282 | ABN30-8 | Client-Sicht belegt: simplebeacons-screen-5 (vis24all-client-263 Lauf 4 (711 Bilder; dieselbe Ablage)): Pyramiden-Icons B wachsen mit der Ebene, Pyramid 5/6, Tab simplebeacons (rel10-006); Extraherzen/Texturen/Formsaege/Koepfe bereits ok |
| 308 | WP-25 | Client-Sicht belegt: Tastenbelegung 75/100 % mit Pfeilen als umgedrehtes T (^ ueber < v >), simpleinterfaces-keys-75/-100 (vis24all-client-263 Lauf 4 (711 Bilder; dieselbe Ablage)) + keys_probe KEYSPROBE_RC=0 (compat26-client/keys.log, 30 Pruefungen GUI 3: Layout-Wechsel, Editor, Taste setzen/entfernen) |
| 541 | VIS24-10 | Client-Sicht belegt: Deed-Editor im Sammellauf: simpletweaks-deed-editor/-error/-valid/-cooldown/-map in vis24all Lauf 4 (0 Fehler, vis24all-client-263 Lauf 4 (711 Bilder; dieselbe Ablage); Fix 2 in a-rel10: Reiter „Rechte“ per Maus) |
| 998 | ABN30-8 | Client-Sicht belegt: simplebeacons-screen-5 (vis24all-client-263 Lauf 4 (711 Bilder; dieselbe Ablage)): Pyramiden-Icons B wachsen mit der Ebene, Pyramid 5/6, Tab simplebeacons (rel10-006); Extraherzen/Texturen/Formsaege/Koepfe bereits ok |
| 1003 | ABN30-8 | Client-Sicht belegt: simplebeacons-screen-5 (vis24all-client-263 Lauf 4 (711 Bilder; dieselbe Ablage)): Pyramiden-Icons B wachsen mit der Ebene, Pyramid 5/6, Tab simplebeacons (rel10-006); Extraherzen/Texturen/Formsaege/Koepfe bereits ok |
| 1114 | DW04A-2 | Client-Sicht belegt: Smoke freelook-zoomed/-zoom-reset in vis24all Lauf 4 (0 Fehler, vis24all-client-263 Lauf 4 (711 Bilder; dieselbe Ablage)) |
| 1191 | DW04A-2 | Client-Sicht belegt: Smoke freelook-zoomed/-zoom-reset in vis24all Lauf 4 (0 Fehler, vis24all-client-263 Lauf 4 (711 Bilder; dieselbe Ablage)) |

## Tabelle 2: teils belegt, Rest benannt (bleibt [~])

| Queue-Z. | ID | Beleg / Rest |
|---|---|---|
| 25 | CLOUDS20-2026-11-10 | Client-Sicht teils belegt/Rest: Wolkenblock/Wolkenstein in a-rel10 rel10-client-263 (135 Bilder, 0 Fehler; /root/sb-tools-b/previews/a-rel10/) rel10-058/-059 scene-layers gerendert, aber Stand 3277653e2 vor b-clouds20; Rest: translucent (Milchglas) + FPS am Stand b-clouds20, Besitzer CLOUDS17 |
| 82 | WD25-1 | Client-Sicht teils belegt/Rest: nur Gametests (BS25-4/-21); Rest: Raketenflug/staerkere Rakete im Client nie gesehen |
| 93 | WE25-3 | Client-Sicht teils belegt/Rest: Rest: sort_probe (echte Maus Kreativ) lief nicht; Kreativ-Tabs allgemein ok (rel10 043 Tabs) |
| 94 | WE25-4 | Client-Sicht teils belegt/Rest: freelook-zoomed/-zoom-reset (vis24all) belegen nur Free-Look-Zoom; Rest: Zoom-Taste V + Mausrad + Fernrohr-Overlay (WE25-4/BS29-3) nicht abgebildet |
| 104 | WF25-6 | Client-Sicht teils belegt/Rest: Tab simplemobs (rel10-049/-050) zeigt nur Items; Rest: Spawner-Fragment-Drop beim Abbau, behelfsmaessiger Spawner platziert, Rezepte im Client |
| 107 | WF25-9 | Client-Sicht teils belegt/Rest: Client-Sicht belegt: b-client09 (28 Bilder, faults 0: Tabs, Meissel/Stab 1./3. Person, Oktant-HUD/-Screen, Blaupause-Screen/platziert, JEI) + mouse_probe MOUSEPROBE_RC=0 11.11. (compat26-client/mouse.log: Blaupause-Editor, Leerhand-Rechtsklick N23 auf platzierter Blaupause, Oktant-/Stab-Knoepfe, JEI R); Rest nur Gate/Merge, SB-Config-Schluessel, Handbuch-Farben (Besitzer) |
| 116 | WH26-5 | Client-Sicht teils belegt/Rest: Tab Simple Octets (rel10-032/-033) zeigt Bloecke; Formsaege dort nicht einzeln sichtbar; Rest: Formsaege-Item im Octet-Tab bzw. Nutzung |
| 117 | WH26-6 | Client-Sicht teils belegt/Rest: Bild simplestorage-dw03c-barrels + simplecrucibles-crucible-netherite-with-barrel (vis24all) vorhanden (nicht Fass-fuer-Fass ausgewertet); Rest Besitzer, Neo/Forge |
| 121 | WH26-10 | Client-Sicht teils belegt/Rest: Rest: Compressed/Dense/Collapsed (Tab simpleblocks rel10-044/-045 nur Anfang/Ende), SlotAlert-Pulsieren und Hotbar-Alarm im Client nicht gesehen |
| 122 | WH26-10 | Client-Sicht teils belegt/Rest: Rest: Compressed/Dense/Collapsed (Tab simpleblocks rel10-044/-045 nur Anfang/Ende), SlotAlert-Pulsieren und Hotbar-Alarm im Client nicht gesehen |
| 128 | WI26-6 | Client-Sicht teils belegt/Rest: Tab dragonfight (rel10-030) nur Items; Rest: Enderherz-Belohnung/Tönung im Client nicht gesehen |
| 133 | IG31-2 | Client-Sicht teils belegt/Rest: Rest: Mighty/Evil-Conduit als Block mit Partikeln/Umgebungston nicht im Client gesehen (nur Tab simplebeacons rel10-006 + Leuchtfeuer-Fenster) |
| 138 | IG31-6 | Client-Sicht teils belegt/Rest: Rest: Kupferkabel-Klemmringe + Interface buendig/Randstreifen an Truhe nicht im Client gesehen (nur Rohr-Fenster) |
| 142 | IG31-9 | Client-Sicht teils belegt/Rest: Rest: Tuer-Durchsicht-Loecher, 2x2-/2x3-Tuer-Platzierung und Tuergeraeusch nicht im Client gesehen |
| 143 | IG31-10 | Client-Sicht teils belegt/Rest: simplemobheads-slotfit-00/-items-00 (vis24all, Test-Hintergrund magenta): Einpassung im Slot ok (A); Rest: Guerteltier-Pose einzeln, Shift-Klick in Kopf-Slot |
| 144 | IG31-11 | Client-Sicht teils belegt/Rest: Rest: Starkes TNT/Schwarzpulver-Farben im Client nicht gesehen |
| 149 | IG31-19 | Client-Sicht teils belegt/Rest: Rest: Krebs-Modell (Beine/Winken) im Client nicht gesehen (Tab simpleanimals rel10-009 nur Items) |
| 150 | IG31-20 | Client-Sicht teils belegt/Rest: Rest: Wildschwein-Modell im Client nicht gesehen |
| 152 | IG31-21 | Client-Sicht teils belegt/Rest: Rest: Kessel auf Lagerfeuer mit Gerüst im Client nicht gesehen |
| 153 | IG31-22 | Client-Sicht teils belegt/Rest: simplemobheads-row-36/-worn-04 (vis24all) zeigen platzierte/getragene Koepfe; Rest: Fisch-/Kaulquappen-/Nautilus-/Hasen-/Phantom-Kopf nicht einzeln benannt gesehen |
| 154 | IG31-23 | Client-Sicht teils belegt/Rest: Rest: Sattel/Ruestung/Frosch-Eimer C nur als Tab-Icons (rel10), Render/Tooltip der Varianten nicht gesehen |
| 156 | MACH32-DAEMPFER-A | Client-Sicht teils belegt/Rest: Rest: Signaldaempfer-Stufen und Doppelkolben-Stufen im Client (vis24all-Machines-Smoke, laut RELEASE-ABNAHME-A „Doppelkolben-Stufen ok“, Bilder nicht einzeln ausgewertet); Besitzer-Wahl |
| 157 | MACH32-KOLBEN-A | Client-Sicht teils belegt/Rest: Rest: Signaldaempfer-Stufen und Doppelkolben-Stufen im Client (vis24all-Machines-Smoke, laut RELEASE-ABNAHME-A „Doppelkolben-Stufen ok“, Bilder nicht einzeln ausgewertet); Besitzer-Wahl |
| 165 | IG31-34 | Client-Sicht teils belegt/Rest: Rest: 1,5-Tuer Kanten/Doppeltuer im Client nicht gesehen |
| 170 | IG31-34 | Client-Sicht teils belegt/Rest: Rest: 1,5-Tuer Kanten/Doppeltuer im Client nicht gesehen |
| 172 | CLICK33-1 | Client-Sicht teils belegt/Rest: echte Linksklicks belegt in Keys-/Oktant-/Stab-/Blaupause-Screens und Rohr-Fenster (keys_probe, mouse_probe, pipe_probe); Rest: ControllerScreen, ItemStatsScreen, SheetMusicScreen, StartAreaEditorScreen und DeedMapScreen nicht mit echter Maus geklickt |
| 173 | IG31-36 | Client-Sicht teils belegt/Rest: simpleearth-v10-exact/-v17-corner (vis24all) zeigen Wuerfel-/Faltansicht; Rest: Grenzuebertritt im Orbit (Taste N) im Client nicht gesehen, Besitzer |
| 174 | IG31-21 | Client-Sicht teils belegt/Rest: Rest: Kessel auf Lagerfeuer mit Gerüst im Client nicht gesehen |
| 175 | IG31-22 | Client-Sicht teils belegt/Rest: simplemobheads-row-36/-worn-04 (vis24all) zeigen platzierte/getragene Koepfe; Rest: Fisch-/Kaulquappen-/Nautilus-/Hasen-/Phantom-Kopf nicht einzeln benannt gesehen |
| 176 | IG31-23 | Client-Sicht teils belegt/Rest: Rest: Sattel/Ruestung/Frosch-Eimer C nur als Tab-Icons (rel10), Render/Tooltip der Varianten nicht gesehen |
| 178 | MACH32-DAEMPFER-A | Client-Sicht teils belegt/Rest: Rest: Signaldaempfer-Stufen und Doppelkolben-Stufen im Client (vis24all-Machines-Smoke, laut RELEASE-ABNAHME-A „Doppelkolben-Stufen ok“, Bilder nicht einzeln ausgewertet); Besitzer-Wahl |
| 179 | MACH32-KOLBEN-A | Client-Sicht teils belegt/Rest: Rest: Signaldaempfer-Stufen und Doppelkolben-Stufen im Client (vis24all-Machines-Smoke, laut RELEASE-ABNAHME-A „Doppelkolben-Stufen ok“, Bilder nicht einzeln ausgewertet); Besitzer-Wahl |
| 187 | IG31-34 | Client-Sicht teils belegt/Rest: Rest: 1,5-Tuer Kanten/Doppeltuer im Client nicht gesehen |
| 194 | IG31-36 | Client-Sicht teils belegt/Rest: simpleearth-v10-exact/-v17-corner (vis24all) zeigen Wuerfel-/Faltansicht; Rest: Grenzuebertritt im Orbit (Taste N) im Client nicht gesehen, Besitzer |
| 195 | IG31-39 | Client-Sicht teils belegt/Rest: Tab-Bild ok (siehe IG31-39 [x] oben); Rest nur Gate/Production + Besitzer-Sicht |
| 204 | SNOW32-2026-10-31 | Client-Sicht teils belegt/Rest: Rest: Schneedecke/Frostgras im Client nicht gesehen |
| 205 | SN30-2 | Client-Sicht teils belegt/Rest: Rest: Schneedecke/Frostgras im Client nicht gesehen |
| 275 | ABN30-1 | Client-Sicht teils belegt/Rest: Rest: Dino-Felle je Biom im Client nicht gesehen (Tab simpledinos rel10-024 nur Items) |
| 276 | ABN30-2 | Client-Sicht teils belegt/Rest: Rest: Tier-Varianten (Hirsch/Wildschwein/Igel) im Client nicht gesehen |
| 277 | ABN30-3 | Client-Sicht teils belegt/Rest: Rohr-Fenster im Trichter-Raster: rel10-068 vs -076 (a-rel10 rel10-client-263 (135 Bilder, 0 Fehler; /root/sb-tools-b/previews/a-rel10/)) ok; Rest nur Besitzer-Wahl Vorschau ST31 |
| 278 | ABN30-4 | Client-Sicht teils belegt/Rest: simplechallenges-select-challenge (vis24all-client-263 Lauf 4 (711 Bilder; dieselbe Ablage)): Auswahlfenster mit Karten-Vorschau/Start area/Play ok; Rest: Siegel-Loesung, Welt-Vorschau, S5-S8 im Client nicht gesehen |
| 279 | ABN30-5 | Client-Sicht teils belegt/Rest: Rest: 1,5-Tuer Kanten/Doppeltuer im Client nicht gesehen |
| 280 | ABN30-6 | Client-Sicht teils belegt/Rest: simpleshops-world/-glow/-owner-screen (vis24all-client-263 Lauf 4 (711 Bilder; dieselbe Ablage)): Verkaufsblock mit Glas/Ware/Gluehen + Besitzerfenster sichtbar; Rest: Trade Terminal und Sales Storage nicht abgebildet, Besitzer-Abnahme |
| 281 | ABN30-7 | Client-Sicht teils belegt/Rest: Rest: rissiges Eis (3 Bloecke, Abstufung) im Client nicht gesehen |
| 283 | ABN30-9 | Client-Sicht teils belegt/Rest: Dev-Eintrag im Inventar (Stil-Knopf/Models/Dev) rel10-080 ok (A); Rest: Dev-Panel-Klickboxen je Variante, Pyramiden-Icons/Schwingen-Look-Varianten nicht geoeffnet |
| 284 | ABN30-10 | Client-Sicht teils belegt/Rest: Tabs ores (rel10-002/-003) zeigen Items; Rest: Rosegold-Farben/Bloecke im Client nicht einzeln gesehen |
| 285 | ABN30-11 | Client-Sicht teils belegt/Rest: Rest: Trials Spawner B/Tresor B im Client nicht gesehen |
| 287 | ABN30-13 | Client-Sicht teils belegt/Rest: simplefoundry-window-stone (vis24all-client-263 Lauf 4 (711 Bilder; dieselbe Ablage)): Giesserei-Pult mit Tank/Level ok; Rest: Aufbau Huelle/Kammer/Rost/Tuelle in der Welt, Schmelzen, Wahl |
| 294 | ABN30-10 | Client-Sicht teils belegt/Rest: Tabs ores (rel10-002/-003) zeigen Items; Rest: Rosegold-Farben/Bloecke im Client nicht einzeln gesehen |
| 301 | WP-20 | Client-Sicht teils belegt/Rest: Rest: Trials Spawner B/Tresor B im Client nicht gesehen |
| 303 | WP-18 | Client-Sicht teils belegt/Rest: Rest: Tiere/Dinos im Client nicht gesehen |
| 304 | WP-18 | Client-Sicht teils belegt/Rest: Rest: Tiere/Dinos im Client nicht gesehen |
| 306 | WP-24 | Client-Sicht teils belegt/Rest: simplechallenges-select-challenge (vis24all-client-263 Lauf 4 (711 Bilder; dieselbe Ablage)): Auswahlfenster mit Karten-Vorschau/Start area/Play ok; Rest: Siegel-Loesung, Welt-Vorschau, S5-S8 im Client nicht gesehen |
| 314 | ABN29-4 | Client-Sicht teils belegt/Rest: Rest: Starkes Schwarzpulver/TNT bzw. Detektor-Haltbarkeit im Client nicht gesehen |
| 315 | ABN29-5 | Client-Sicht teils belegt/Rest: Rest: Boote/Ruten/Bretter-Optik (Besitzer) im Client nicht einzeln gesehen |
| 316 | ABN29-6 | Client-Sicht teils belegt/Rest: vis24all simplemobheads-row-36/-worn-04: 36 Reihen platziert, 17 getragen ok (A); Rest: Pferd flach, Warden-Fuehler, Nautilus/Tintenfisch einzeln nicht benannt gesehen |
| 416 | SNOW32-FALL | Client-Sicht teils belegt/Rest: Rest: Schneedecke/Frostgras im Client nicht gesehen |
| 524 | OW24-4 | Client-Sicht teils belegt/Rest: Rest: rissiges Eis (3 Bloecke, Abstufung) im Client nicht gesehen |
| 629 | WP23-2026-10-29 | Client-Sicht teils belegt/Rest: Client-Sicht belegt: simpleinterfaces-keys-list/-function/-60/-75/-100/-layout-de/-uk/-fr/-editor (vis24all) + keys_probe KEYSPROBE_RC=0; Rest nur Besitzer-Abnahme/Wahl, Bündel/Gate |
| 656 | (A, a-heads29, 29.10.: Branch a-heads29, | Client-Sicht teils belegt/Rest: vis24all simplemobheads-row-36/-worn-04: 36 Reihen platziert, 17 getragen ok (A); Rest: Pferd flach, Warden-Fuehler, Nautilus/Tintenfisch einzeln nicht benannt gesehen |
| 657 | (A, a-fix29, 29.10.) BS28b-3 Detektor ab | Client-Sicht teils belegt/Rest: Rest: Starkes Schwarzpulver/TNT bzw. Detektor-Haltbarkeit im Client nicht gesehen |
| 668 | WP18-2026-10-29 | Client-Sicht teils belegt/Rest: Rest: Tier-Varianten (Hirsch/Wildschwein/Igel) im Client nicht gesehen |
| 705 | WP20-2026-10-29 | Client-Sicht teils belegt/Rest: Rest: Trials Spawner B/Tresor B im Client nicht gesehen |
| 706 | WP20-2026-10-29 | Client-Sicht teils belegt/Rest: Rest: Trials Spawner B/Tresor B im Client nicht gesehen |
| 707 | WP20-2026-10-29 | Client-Sicht teils belegt/Rest: Rest: Trials Spawner B/Tresor B im Client nicht gesehen |
| 708 | WP20-2026-10-29 | Client-Sicht teils belegt/Rest: Rest: Trials Spawner B/Tresor B im Client nicht gesehen |
| 723 | BS25-11 | Client-Sicht teils belegt/Rest: Rest: Boote/Ruten/Bretter-Optik (Besitzer) im Client nicht einzeln gesehen |
| 724 | BS25-12 | Client-Sicht teils belegt/Rest: Rest: Boote/Ruten/Bretter-Optik (Besitzer) im Client nicht einzeln gesehen |
| 730 | BS25-18 | Client-Sicht teils belegt/Rest: vis24all simplemobheads-row-36/-worn-04: 36 Reihen platziert, 17 getragen ok (A); Rest: Pferd flach, Warden-Fuehler, Nautilus/Tintenfisch einzeln nicht benannt gesehen |
| 732 | BS25-21 | Client-Sicht teils belegt/Rest: Rest: Starkes Schwarzpulver/TNT bzw. Detektor-Haltbarkeit im Client nicht gesehen |
| 762 | WP-24 | Client-Sicht teils belegt/Rest: simplechallenges-select-challenge (vis24all-client-263 Lauf 4 (711 Bilder; dieselbe Ablage)): Auswahlfenster mit Karten-Vorschau/Start area/Play ok; Rest: Siegel-Loesung, Welt-Vorschau, S5-S8 im Client nicht gesehen |
| 925 | CL-1 | Client-Sicht teils belegt/Rest: Rest: Buch-Screen, Kern-Animation, neue Gadgets nicht im Client gesehen; Truhen (simpleinterfaces-chest/-double-chest/-barrel/-shulker, vis24all) ok; Octant-Manager mouse_probe ok |
| 982 | ABN29-6 | Client-Sicht teils belegt/Rest: vis24all simplemobheads-row-36/-worn-04: 36 Reihen platziert, 17 getragen ok (A); Rest: Pferd flach, Warden-Fuehler, Nautilus/Tintenfisch einzeln nicht benannt gesehen |
| 987 | ABN29-4 | Client-Sicht teils belegt/Rest: Rest: Starkes Schwarzpulver/TNT bzw. Detektor-Haltbarkeit im Client nicht gesehen |
| 988 | ABN29-5 | Client-Sicht teils belegt/Rest: Rest: Boote/Ruten/Bretter-Optik (Besitzer) im Client nicht einzeln gesehen |
| 992 | ABN30-3 | Client-Sicht teils belegt/Rest: Rohr-Fenster im Trichter-Raster: rel10-068 vs -076 (a-rel10 rel10-client-263 (135 Bilder, 0 Fehler; /root/sb-tools-b/previews/a-rel10/)) ok; Rest nur Besitzer-Wahl Vorschau ST31 |
| 995 | ABN30-5 | Client-Sicht teils belegt/Rest: Rest: 1,5-Tuer Kanten/Doppeltuer im Client nicht gesehen |
| 996 | ABN30-6 | Client-Sicht teils belegt/Rest: simpleshops-world/-glow/-owner-screen (vis24all-client-263 Lauf 4 (711 Bilder; dieselbe Ablage)): Verkaufsblock mit Glas/Ware/Gluehen + Besitzerfenster sichtbar; Rest: Trade Terminal und Sales Storage nicht abgebildet, Besitzer-Abnahme |
| 997 | ABN30-7 | Client-Sicht teils belegt/Rest: Rest: rissiges Eis (3 Bloecke, Abstufung) im Client nicht gesehen |
| 999 | ABN30-9 | Client-Sicht teils belegt/Rest: Dev-Eintrag im Inventar (Stil-Knopf/Models/Dev) rel10-080 ok (A); Rest: Dev-Panel-Klickboxen je Variante, Pyramiden-Icons/Schwingen-Look-Varianten nicht geoeffnet |
| 1000 | ABN30-5 | Client-Sicht teils belegt/Rest: Rest: 1,5-Tuer Kanten/Doppeltuer im Client nicht gesehen |
| 1001 | ABN30-6 | Client-Sicht teils belegt/Rest: simpleshops-world/-glow/-owner-screen (vis24all-client-263 Lauf 4 (711 Bilder; dieselbe Ablage)): Verkaufsblock mit Glas/Ware/Gluehen + Besitzerfenster sichtbar; Rest: Trade Terminal und Sales Storage nicht abgebildet, Besitzer-Abnahme |
| 1002 | ABN30-7 | Client-Sicht teils belegt/Rest: Rest: rissiges Eis (3 Bloecke, Abstufung) im Client nicht gesehen |
| 1004 | ABN30-9 | Client-Sicht teils belegt/Rest: Dev-Eintrag im Inventar (Stil-Knopf/Models/Dev) rel10-080 ok (A); Rest: Dev-Panel-Klickboxen je Variante, Pyramiden-Icons/Schwingen-Look-Varianten nicht geoeffnet |
| 1005 | ABN30-9 | Client-Sicht teils belegt/Rest: Dev-Eintrag im Inventar (Stil-Knopf/Models/Dev) rel10-080 ok (A); Rest: Dev-Panel-Klickboxen je Variante, Pyramiden-Icons/Schwingen-Look-Varianten nicht geoeffnet |
| 1008 | ABN30-1 | Client-Sicht teils belegt/Rest: Rest: Dino-Felle je Biom im Client nicht gesehen (Tab simpledinos rel10-024 nur Items) |
| 1009 | ABN30-2 | Client-Sicht teils belegt/Rest: Rest: Tier-Varianten (Hirsch/Wildschwein/Igel) im Client nicht gesehen |
| 1013 | IG31-2 | Client-Sicht teils belegt/Rest: Rest: Mighty/Evil-Conduit als Block mit Partikeln/Umgebungston nicht im Client gesehen (nur Tab simplebeacons rel10-006 + Leuchtfeuer-Fenster) |
| 1014 | IG31-3 | Client-Sicht teils belegt/Rest: Tab-Inhalt belegt (rel10 43 Tabs, siehe IG31-3 oben); Rest: Tab-Reihenfolge Super-Mod -> Sub-Mods nicht geprueft (Test liest Registry-Reihenfolge) |
| 1018 | IG31-6 | Client-Sicht teils belegt/Rest: Rest: Kupferkabel-Klemmringe + Interface buendig/Randstreifen an Truhe nicht im Client gesehen (nur Rohr-Fenster) |
| 1022 | IG31-2 | Client-Sicht teils belegt/Rest: Rest: Mighty/Evil-Conduit als Block mit Partikeln/Umgebungston nicht im Client gesehen (nur Tab simplebeacons rel10-006 + Leuchtfeuer-Fenster) |
| 1023 | IG31-3 | Client-Sicht teils belegt/Rest: Tab-Inhalt belegt (rel10 43 Tabs, siehe IG31-3 oben); Rest: Tab-Reihenfolge Super-Mod -> Sub-Mods nicht geprueft (Test liest Registry-Reihenfolge) |
| 1027 | IG31-6 | Client-Sicht teils belegt/Rest: Rest: Kupferkabel-Klemmringe + Interface buendig/Randstreifen an Truhe nicht im Client gesehen (nur Rohr-Fenster) |
| 1034 | IG31-9 | Client-Sicht teils belegt/Rest: Rest: Tuer-Durchsicht-Loecher, 2x2-/2x3-Tuer-Platzierung und Tuergeraeusch nicht im Client gesehen |
| 1035 | IG31-10 | Client-Sicht teils belegt/Rest: simplemobheads-slotfit-00/-items-00 (vis24all, Test-Hintergrund magenta): Einpassung im Slot ok (A); Rest: Guerteltier-Pose einzeln, Shift-Klick in Kopf-Slot |
| 1036 | IG31-11 | Client-Sicht teils belegt/Rest: Rest: Starkes TNT/Schwarzpulver-Farben im Client nicht gesehen |
| 1088 | GUIDE01-2026-11-03 | Client-Sicht teils belegt/Rest: guidechap-client-263 (b-guide15-sbt1.log) [guidechap] ok: 62, faults: 0; Rest: Besitzer-Abnahme, GUI-4-Klicks, Bündel/Gate |
| 1089 | GUIDECHAP14-2026-11-09 | Client-Sicht teils belegt/Rest: guidechap-client-263: [guidechap] ok: 62, faults: 0 (b-guide15-sbt1.log; Marker/Uebersicht/Schalter aus ok); Rest nur Besitzer-Wahl GUIDE14 |
| 1104 | (B, b-keys34, 03.11., Tastatur-Layouts U | Client-Sicht teils belegt/Rest: Client-Sicht belegt: simpleinterfaces-keys-list/-function/-60/-75/-100/-layout-de/-uk/-fr/-editor (vis24all) + keys_probe KEYSPROBE_RC=0; Rest nur Besitzer-Abnahme/Wahl, Bündel/Gate |

## Tabelle 3: kein Client-Beleg gefunden (bleibt [~], Rest: Client-Sicht im Spiel)

| Queue-Z. | ID |
|---|---|
| 148 | IG31-18 |
| 199 | IG31-13-2026-10-31 |
| 286 | ABN30-12 |
| 313 | BS28-2 |
| 355 | IG3114-PLATTEN |
| 356 | IG3114-NOTEN |
| 357 | IG3114-DAEMMER |
| 358 | IG3114-BESATZ |
| 378 | AUTO-CLIENT Automatischer Client-Start j |
| 384 | (B, b-lead02, 01.11.) GEBAUT: lead_plate |
| 415 | MACH32S4B-2026-11-10 |
| 435 | WP-22 |
| 547 | WD25UI-2026-10-25 |
| 553 | WD25EARTH-2026-10-25 |
| 554 | WD25-17 |
| 598 | WB24-3 |
| 601 | WA24-6 |
| 605 | WB24-1 |
| 606 | WB24WINGS-2026-10-24 |
| 608 | WB24-4 |
| 609 | WB24-5 |
| 610 | WB24-6 |
| 611 | OW24-2 |
| 612 | WB24-7 |
| 620 | STORAGE30-2026-10-30 |
| 638 | HS29-3 |
| 641 | BS29-1 |
| 643 | BS29-3 |
| 644 | BS29-4 |
| 658 | WP-08 |
| 664 | WP-08 |
| 670 | WP17-2026-10-29 |
| 671 | BS28-4 |
| 684 | STORAGE30-2026-10-30 |
| 693 | MOB27-2 |
| 695 | MOB27-3 |
| 704 | BS27-1 |
| 709 | BS27-4 |
| 713 | BS25-1 |
| 714 | BS25-2 |
| 715 | BS25-3 |
| 717 | BS25-5 |
| 718 | BS25-6 |
| 719 | BS25-7 |
| 720 | BS25-8 |
| 721 | BS25-9 |
| 722 | BS25-10 |
| 725 | BS25-13 |
| 726 | BS25-14 |
| 727 | BS25-15 |
| 733 | BS25-22 |
| 734 | BS25-23 |
| 735 | BS25-24 |
| 736 | BS25-25 |
| 741 | WA24-1 |
| 754 | NA24-5 |
| 756 | NA24-8 |
| 759 | EL24-1 |
| 785 | TZ22-9 |
| 793 | AUDM24-3 |
| 891 | REV2-B-11 |
| 912 | (B, b-opte4, 11.11.: N27b Messung headle |
| 913 | AUDIT-STIL-MODULE-2026-10-17 |
| 981 | ABN29-1 |
| 1017 | IG31-5 |
| 1026 | IG31-5 |
| 1041 | MIXCHECK31-2 |
| 1042 | MIXCHECK31-3 |
| 1074 | (B, b-boat02, 02.11.) BOAT40 Ofen-Boot i |
| 1083 | BS25-20 |
| 1109 | ENCH03-2026-11-04 |
| 1123 | IG31-26 |
| 1125 | DW04C-2 |
| 1126 | DW04C-3 |
| 1128 | DW04C-5 |
| 1200 | IG31-26 |
| 1202 | DW04C-2 |
| 1207 | DW04C-7 |
| 1210 | (A, a-ores07, 06.11., Plan docs/ai/PLAN- |
| 1215 | (A, a-tex07b, 06.11., Besitzer-Abnahme O |
| 1219 | IG31-57 |
| 1222 | IG31-61 |
| 1225 | IG31-60 |
| 1227 | IG31-60 |
| 1229 | IG31-65 |
| 1244 | (B, b-fun14, 09.11.) N10b FEAT11=alle: * |
| 1249 | PAL16-2026-11-10 |
| 1257 | (B, b-fun14, 09.11.) N10b FEAT11=alle: * |
| 1262 | PAL15-2026-11-09 |
| 1281 | (B, b-server26, 10.11., Basis hub/b-clot |
| 1283 | ABN-A |

## Offen / Empfehlung
- Nächster Client-Lauf (Bündel) sollte gezielt abbilden: Kupferkabel/Interface an Truhe, Türen 1,5/2×2, rissiges Eis, Kompakte Blöcke + SlotAlert, Conduits, Trials-Spawner/Tresor, Tier-/Dino-Varianten, Trade Terminal/Sales Storage, Doppelkolben/Dämpfer, Schneedecke/Frostgras.
- `sort_probe`, `deed_probe`, `fletching_probe`, `menu_probe` (Earth) liefen nicht in diesem Zug; Klick-Linksklick-Lücke CLICK33-1 bleibt für Controller/ItemStats/SheetMusic/StartArea/DeedMap.
- Hinweis: Vor dem Bündel auf neuestes hub/b-bundle18x-gen rebasen, `.claude/QUEUE.md`-Konflikte möglich (nur Zeilenenden betroffen: bei Konflikt beide Teile behalten).
