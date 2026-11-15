# Prinzipien der SimpleBuilding-Familie (Einstieg, Stand 17.10.2026)

Eine Stelle für alle Regeln: **Teil 1** gilt für jede Mod der Familie (SimpleBuilding und alle Module unter `modules/`), **Teil 2** gibt je Mod
(Sub-Mod) die eigenen Regeln. Dies ist der Einstieg; die ausführlichen Texte bleiben in den verlinkten Dateien und in `AGENTS.md`
(maßgebliche Regelsammlung für Assistenten). Neue Regeln des Besitzers hier eintragen (Teil 1 oder beim passenden Modul) und von dort verlinken.
Regel-IDs: A1–A11 gelten für alle Mods; die Besitzer-Regeln vom 17.10. tragen zusätzlich die IDs B1 (Baublöcke, = A3) und O1 (Octets, = A4).

## Teil 1 – Für alle Mods

### A1 Jedes Modul allein spielbar
Alle Module starten und sind mit Vanilla + harten Bibliotheken komplett spielbar; Partner-Mods machen es besser, nie erst möglich. Harte Abhängigkeit nur als erklärtes
Add-on, keine Klassenimporte über Modulgrenzen (nur Framework-API, öffentliche Registry-Ids, Tags). Registrierung hängt nie vom Laden eines anderen Mods ab
(nur Bezug/Sichtbarkeit/Rezepte). Ausnahme bei Fremdblock-Varianten: siehe O1.
→ `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`, `docs/MULTIMOD.md`, `docs/ai/PLAN-MODUL-UNABHAENGIGKEIT-2026-10-02.md`; AGENTS.md §10.

### A2 Modulfamilie und simplelib (AGENTS.md 3.16)
SimpleBuilding ist Super-Mod mit Sub-Mods (Octets, Blocks, End, Ovens, Crucibles, Mobheads, später Hammers, Storage, Gadgets, Enderite). Was mehrere Module brauchen
(z. B. polierter Endstein, geteilte Materialien, UI-Bausteine, In-World-Upgrades), liegt in `simplelib`. Ist ein Partner-Modul geladen, schalten sich passende Varianten frei.
Neue Features werden nach dem Mods-Verzeichnis eingeordnet (`docs/MODS-UND-FEATURES.md`, im selben Commit pflegen).
→ `docs/ai/PLAN-MODULFAMILIE-2026-10-16.md`, `docs/ai/PLAN-MODULAUFTEILUNG-2026-10-11.md`, `docs/ai/KONZEPT-SUPERMOD-SUBMOD-2026-10-06.md`.

### A3 Baublöcke gehören in die Familie, Rest in Simple Blocks (B1, AGENTS.md 3.16, verfeinert 17.10.)
Alle Baublöcke gehören zur SimpleBuilding-Familie, nie in fachfremde Mods. **Simple Blocks enthält alle Baublöcke, die keinen anderen fachlichen Ort haben**
(Themen-Sub-Mods wie Octets, End/Enderite, Schach behalten ihre Blöcke; Lager/Maschinen/Werkzeuge bleiben, wo sie sind). Verschiebe-Liste:
`docs/ai/INVENTAR-BAUBLOECKE-2026-10-17.md`. Es gibt **einen** Kreativtab „Simple Blocks“ für alle Baublöcke (Plan dort).

### A4 Octet-Regel (O1, 17.10.)
Jeder Block, der Treppen- oder Stufen-Varianten (stairs/slab) hat, bekommt auch einen Octet-Block (Achtelblock) – Vanilla-Blöcke, SimpleBuilding-Blöcke und alle Blöcke,
denen Simple Blocks Treppe/Stufe gibt. Das Octet liefert `simpleoctets` (bei Fremdblöcken als optionale Integration, nur wenn der Besitzer-Mod geladen ist; jeder Mod bleibt allein spielbar).
**Ausnahme (Besitzer 17.10.):** Naturstufen (Erde, Gras, Sand, Kies; Roter Sand zählt als Sand, PLAN40) bekommen keine Octets. Neue Treppen/Stufen/Mauern in irgendeinem Modul ohne Octet-Eintrag sind ein Fehler (Abdeckungstest `stairs_and_slabs_have_octets`, Ausnahmen seit 12.11. als exakte Ids, Formen nach Klasse und Id-Endung). Lücken: `docs/ai/INVENTAR-OCTETS-2026-10-17.md`.

### A4b Formsäge (Besitzer 17.10.)
Alles, was Treppen-, Stufen- oder Octet-Varianten hat (Stein, Holz und Blöcke, die die Vanilla-Steinsäge nicht kann), lässt sich in der fortgeschrittenen Säge schneiden („Formsäge“, EN „Shapecutter“ seit 21.10., `simpleblocks:shapecutter`, Simple Blocks; Bildschirm = Vanilla-Steinsäge). Neue Formen müssen dort auftauchen.
**Octets und Holzvarianten exklusiv in der Formsäge; Vanilla-Steinsäge bleibt Vanilla; Fallback Steinsäge nur ohne Simple Blocks** (Besitzer 17.10.): alle Octets (Stein und Holz) und alle Holzvarianten (Treppe, Stufe, Zaun, Stöcke, Bretter, Entrinden …) gibt es nur als `simpleblocks:sawing`-Rezept, solange Simple Blocks geladen ist; die Vanilla-Steinsäge bekommt keine zusätzlichen Rezepte dafür. Ein Modul, das ohne Simple Blocks spielbar bleiben muss (z. B. Simple Octets), liefert ein `minecraft:stonecutting`-Ersatzrezept nur mit der Ladebedingung „simpleblocks nicht geladen“. Die Formsäge zeigt weiterhin alle Vanilla-Steinsägen-Rezepte.

### A5 Vanilla-Rückports
Künftige Vanilla-Features je Version prüfen: hat Vanilla es schon, ist der Mod-Teil aus; bei jedem Port erneut prüfen; Liste und Flags in der Datei.
→ `docs/ai/PRINZIPIEN-VANILLA-RUECKPORTS.md`

### A6 Entdeckbarkeit und Filter
Jeder Weg ist zuerst intuitiv und zusätzlich im Spiel gehintet (mindestens zwei Hinweise, einer im Spiel-Alltag; Wiki zählt nicht allein). Filternde Blöcke lassen ein echtes Item fest im Slot
und teilen den Filter-Knopf (exakt / gleiche Art, gemeinsamer Code in simplelib).
→ `docs/ai/PRINZIPIEN-ENTDECKBARKEIT.md`, `docs/ai/PRINZIPIEN-FILTER.md`

### A7 Lesbarkeit und UI
Jede UI-Änderung: Kontrast zum Hintergrund ≥ 4,5:1 (nie dunkler Text mit dunklem Schatten auf Grau), Kontrastwerte im Bericht, Test `ui_text_contrast` bleibt grün (AGENTS.md 3.15).
Tooltips: Standard nur der Name, Beschreibung nur bei nicht offensichtlicher Bedienung oder unsichtbarem Zustand, höchstens 2 Zeilen à ca. 40 Zeichen; Balken-Information nicht doppeln (3.14).
Kein Text auf dem Bildschirm für Gadgets (3.4). Amerikanisches Englisch, EN+DE parallel (3.5).
**Container-Margin (N62, Besitzer 15.11.):** Jeder Kasten einer Container-Oberfläche (Container-Kasten, Inventar-Kasten, Teile verbundener Kästen) hat zwischen seiner hellen Innenlinie und den äußersten Slots/Elementen **auf allen vier Seiten genau `UiBoxes.MARGIN` = 3 px Fläche** (Slot = 16×16 mit dunkler Kante oben/links plus 1 px Lichtkante rechts/unten). Mit Titel hat der Text seinen eigenen Abstand, die Slots liegen trotzdem ≥ 3 px darunter. Reicht der Vanilla-Platz nicht, rückt das Spieler-Inventar nur in der Anzeige tiefer und das Fenster wächst – nie wird der Rand kleiner. Vorschau mit nachgezählten Maßen pro Oberfläche (`/root/sb-tools-b/ig57u/mock/render3.py`); Baustein `BoxLayout` (simpleinterfaces) / `UiBoxes.MARGIN` (simplelib), nicht pro Screen nachrechnen.

### A8 Texturen
Neue Pixelkunst im Stil des Besitzers, immer mit 16-fach-Vorschau alt neben neu; alte Besitzer-Texturen respektieren (3.7). **Aus Vanilla abgeleitete Pixel werden nie eingecheckt**
(auch keine Wiki-Kopien oder Vorschau-PNGs), sondern beim Build erzeugt: `tools/textures/vanilla_derived.py`, Pfade in `.gitignore`.

**Besitzer 21.10.:** (c) Jede Mod hat ein eigenes 16×16-Pixelart-Icon (Mod-Liste). (a) Alle Texturen so nah wie möglich an Vanilla halten – Formensprache, Schattierung, Farbgebung/Paletten, Kontur wie bei Minecraft (eigene Pixel, keine kopierten). (b) Jede Textur wird in Grundform **als Ebenen** gespeichert (Ebenen-Quelle neben dem PNG, Format siehe Pixeleditor-Plan), damit sie im Wiki-Pixeleditor (Dev) angepasst werden kann; das PNG ist das zusammengesetzte Ergebnis.

### A8b Weiche Übergänge – Richtwert „~7 % hart“ (Besitzer 29.10.)
Texturen nach dem Vorbild der abgenommenen Endholz-Bretter A (a-tex29): Vanilla-Raster und Tonverteilung, etwa 7 % harte Übergänge (Anteil benachbarter Pixel mit großem Helligkeitssprung), nicht ~25–30 % wie die alten, kantigen Texturen. Generatoren messen den Anteil und weisen ihn in Vorschauen aus; Kandidaten über ~12 % gelten als zu hart.

### A8c Texturen in mehreren Runden (Besitzer 09.11.)
Keine Textur wird nach dem ersten Entwurf vorgelegt. Ablauf je Textur: **erstellen → bewerten → im Spiel/Render testen → neu erstellen**, mindestens 2–3 Runden. Bewertet wird gegen Vanilla-Stil (A8 a, Vergleich mit passenden Vanilla-Texturen), klare Pixel ohne Blur und ohne weiche Verläufe/weichgezeichnete Skalierung, Härte ~7 % (A8b) und Lesbarkeit in Spielgröße. Getestet wird in Originalgröße und hochskaliert (nearest-neighbour) im Block-/Item-Kontext, besser im Client-Screenshot. Die Bewertung jeder Runde steht im Plan und in der Vorschau (Runden-Verlauf); erst danach wird vorgelegt.

### A8d Vanilla-Analyse je Textur – keine flachen Stellen (Besitzer 09.11., Methode aus RIFT14)
Vor und in jeder Runde (A8c) wird die Textur gegen 2–4 **passende Vanilla-Vorbilder** (gleiches Material/gleiche Form, aus dem Client-Jar, nur in der Vorschau) gemessen und angesehen:
1. **Flache Stellen finden:** größte einfarbige Fläche (4-verbunden, exakt gleiche Farbe, auf 16×16 normiert) und Anteil der Pixel in Flächen ≥ 6 px. Vanilla-Baustoffe liegen meist bei 6–20 px und 2–40 %; markierte Flächen (≥ 10 px rot, 6–9 px orange umrandet) zeigen, wo gebrochen werden muss.
2. **Kanten-Highlights oben/links, Innenschatten unten/rechts** – aber ein Rahmen ist nie eine durchgehende Linie in einem Ton: der Kantenton wechselt in Abschnitten von 2–3 px (Purpur, Endsteinziegel).
3. **Pixelgruppen statt Einzelpixel:** Flächen werden mit 2–4-px-Haufen eines Nachbartons der eigenen Rampe gebrochen (Tiefenschiefer, Endstein); keine Einzelpixel-Streuung (wirkt krümelig), kein Rauschen, kein Blur.
4. **Tonwechsel und Drift:** jedes Teilstück ist oben/links eine Stufe heller als unten/rechts; lange Linien (Fugen, Adern) wechseln alle 1–3 px den Ton, 1–2 Glanzpixel an der Innenecke (Amethyst, Vergoldeter Schwarzstein).
5. **Härte bleibt bei A8b** (~7 %, Tonschritte ≤ 40 Helligkeit); Hauptton höchstens ~40 % der Fläche.

Messwerte sind **Richtwerte**, entschieden wird mit dem Blick (bewusst glatte Materialien wie Vanilla-Stein, Eisenblock, Purpur sind auch orange). Werkzeug: `python3 tools/textures/texrate.py <png…>` (Ampel rot/orange/grün mit Grund), `--vanilla` (dieselbe Messung für die Vanilla-Texturen aus dem Jar, zur Laufzeit, nichts eingecheckt), `--selftest`. Ampel-Regel und Kalibrierung: docs/ai/TEXTUR-BEWERTUNG-2026-11-09.md. Neue Texturen sollen grün sein; orange nur mit Begründung im Plan.

### A9 Server-Autorität und Belege
Alle Stellschrauben serverseitig mit Obergrenzen, die Vanilla nicht gefährden (3.8). Wiki-Aussagen müssen im Code beweisbar sein (3.6); Handbücher/Wiki nach Änderungen neu erzeugen.

### A10 Arbeitsweise
Queue-Punkte vor Arbeitsbeginn mit `[~] (A|B, <branch>, <Datum>)` beanspruchen (`docs/ai/ZWEI-SESSIONS.md`, 3.17); Push nur nach grünem Gate (3.2); Hauptlinie 26.3 zuerst, Fabric zuerst (3.1);
Ports (26.2, 1.21.11, 26.4) erst auf Anforderung. Abläufe: `docs/ai/WORKFLOW.md`, Tests/Gate: AGENTS.md §4.

### A11 Einheitliches Schema der Module (Stil-Audit, 22.10.)
Neue und angefasste Module folgen einem Schema; `tools/check_module_style.py` (Gradle `checkModuleStyle`, in `check`) prüft 1.–2.
1. **Kreativtab:** Titel-Schlüssel `itemgroup.<modid>.<name>`, EN+DE, Anzeige „Simple X“; keine Vanilla-Tab-Einsortierung (Besitzer 30.10., IG31-3): Mod-Items nur in Mod-Tabs, Vanilla-Items nicht in Mod-Tabs, Tab-Reihenfolge Super-Mod → Sub-Mods; durchgesetzt von SimpleLibs `CreativeTabPolicy` (`CreativeTabSettings` liefert immer false).
2. **Config-Texte:** Schema von SimpleBuilding/AutoConfig: `text.autoconfig.<modid>.option.<feld>` + `.@Tooltip`, Titel `text.autoconfig.<modid>.title`, Reiter `text.autoconfig.<modid>.category.<x>`; EN+DE paarig. Alte Schemata (`<mod>.option.*`, `config.<mod>.*`, `<mod>.config.*`) nur noch in den Modulen der Liste `LEGACY` des Lints (offene Umstellung ST-20 Teil 2; die Liste darf nur schrumpfen).
3. **Versionen (Besitzer 21.10., Ea; ergänzt W3B 15.11.):** Modulversionen bleiben 0.x bis zur Besitzer-Abnahme (neue Module 0.1.0, fertig gebaute 0.9.0), danach 1.0.0; SimpleLib zählt eigenständig (Mindestversion in den Abhängigkeiten der Nutzer). **Erst-Release jeder Mod als Beta:** Version `<Basis>-beta.<n>` (z. B. `0.9.0-beta.1`, `1.0.0-beta.1`), Kanal Beta auf Modrinth/CurseForge, Texte ohne Leistungsversprechen; Untergrenzen nennen die Beta (`>=0.4.0-beta.1`, `[0.9.0-beta.1,)`). Prüfung `python3 tools/release/check_beta.py`.
4. **Pakete/IDs:** zwei gültige Paketschemata, kein drittes: `com.simplebuilding.modules.<modid>` (Vorlage `tools/newmod.py`) und `com.<modid>` (aus SimpleBuilding abgespaltene Module wie simpleovens, simpleblocks, sowie simplefun/simplemobheads/simplemoney, ST-12 – Umbenennen wäre Port-/Mixin-/Weltdaten-Risiko). Einstiegsklassen `<Name>Fabric|NeoForge|Forge`, Mod-ID = Ordnername ohne Ausnahme (die frühere Ausnahme `simpledimension` ist seit b-dimid27, 11.11., auf Besitzer-Entscheid WD25-14 zu `simpledimensions` migriert: `LegacyIds` + IdMoves, Altwelten/Configs werden übernommen). Altlast ohne gespeicherte IDs: das Kernpaket `dev.simpledimension.common` von simpledimensions (nur Paketname, keine Welt-/Config-Daten; Umbenennen bei Gelegenheit, nicht nachahmen). Mod-ID-Umbenennungen nur mit Migration nach diesem Muster (Namespace-Move + explizite Moves, Dimensionsschlüssel/-ordner, Config-Ordner einmal übernehmen).
5. **Pflichtdateien:** Mod-Icon (A8 c), `wiki/manual.json` EN/DE, Handbuch (`tools/guides/module_guides.py`), `questionary/` für Module mit Items, `tools/check_data.py` – Ausnahme `wiringexample` (Beispielmodul, ST-42).

---

### A12 Vanilla-Nutzen und Eingriff klären (Besitzer 28.10.)
Jedes neue Feature (Mob, Block, Mechanik, Item) braucht vor dem Bau eine kurze Klärung: (1) welchen Nutzen es im Vanilla-Spielgeschehen hat, (2) was es am Spielgeschehen ändert, (3) ob es das Vanilla-Geschehen stark verändert. Starke Eingriffe in Vanilla eher nicht – oder nur abschaltbar und mit Besitzer-Freigabe. Gehört in jedes Konzept und jeden Plan.

### A13 Mod-Struktur-Seite aktuell halten (Besitzer 30.10.)
`docs/MODSTRUKTUR.html` (Baum am Desktop, Liste am Handy) und `docs/MODSTRUKTUR.md` werden aus `modules/modules.json` erzeugt (`python tools/modtree/modtree.py`). Bei jedem neuen Mod, jedem Sub-Mod-Umzug und jeder geänderten Abhängigkeit neu erzeugen und im selben Commit mitgeben; `checkModtree` (in `check`) schlägt bei veralteten Dateien fehl. Geplante Mods ohne Manifest-Eintrag: `tools/modtree/planned.json`. Die Hauptsitzung veröffentlicht die HTML als Artifact neu.

### A14 Wirkungen telegrafieren (Besitzer 06.11.)
Alles, was den Spieler beeinflusst oder stören kann (Statuseffekte durch Items, Verlangsamung, Schaden, Sperren, automatische Aktionen), wird **sichtbar kommuniziert**, damit ein Aha-Moment entsteht: der Spieler soll erkennen, *woher* die Wirkung kommt. Mittel: eine dezente, aber eindeutige Anzeige an der Quelle (z. B. rote, weich rotierende „Leuchtturm“-Animation hinter dem auslösenden Item im Slot – SimpleLib-Baustein `SlotAlert`), passender Tooltip-Text, ggf. Partikel/Ton. Nicht grell, nicht ablenkend, nur solange die Wirkung aktiv ist; Config/reduced-motion respektieren. Beispiel: Collapsed-Blöcke (Slowness im Inventar).

## Teil 2 – Je Mod

Reihenfolge wie `modules/modules.json`; Mods ohne Sonderregeln stehen mit „keine eigenen Regeln bisher“ da, damit jede Mod einen Platz für ihre Regeln hat. Neue Module bekommen hier
einen Abschnitt (`python tools/newmod.py` legt den Manifest-Eintrag an; den Abschnitt von Hand ergänzen).

### SimpleBuilding (`simplebuilding`)
Super-Mod der Familie: Hülle mit Konfig, Mega-Guide, Testzentrale, Baugeräte (Stäbe, Meißel, Blaupause, Oktant), Hämmer, Lager, Maschinen, Eimer, Schach, Pads.

- Keine neuen Baublöcke ohne Eintrag in `docs/ai/INVENTAR-BAUBLOECKE-2026-10-17.md`: Baublöcke ohne Themenmodul gehören nach `simpleblocks` (B1), Achtel nach `simpleoctets` (O1).
- Tooltip-Regel (AGENTS.md 3.14): Standard nur der Name; Eimer zeichnen nie eine Haltbarkeitslinie.
- Gadgets zeigen keinen Text auf dem Bildschirm (AGENTS.md 3.4): Sounds, Partikel, Blockzustände.
- Pflicht-Sub-Mods sind gebündelt (`requires` in `modules.json`); alles andere nur als `optional` (Schalter `enableSimpleX`).
- Schach (Figuren, Figurenblock, Schachbretter samt Formen, Schach-Achtel) ist seit N47 M7 (b-chess47, 14.11.) der optionale Sub-Mod Simple Chess (`modules/simplechess`); Plan F3 („bleibt im Kern“) ist damit aufgehoben. 26.2 behält nur die 12 Schachbretter.
- Endgame-Verzauberungsstufen (WP-22, MOB27-4): Vanillas `max_level` wird nie angehoben (der Amboss darf aus zwei Schutz-IV-Büchern kein V machen); Stufe V für Schutz und Federfall entsteht nur am Astral-Verzauberer auf der obersten Stufe (Tier 50), Schalter `server.features.endgameEnchantLevels`; Preis je Stufe über dem Vanilla-Maximum `+6`. Der Dark-/End-Verzauberer (BS25-20) hängt sich später nur über `AstralEnchanting.registerEndgame`/`levelCap` ein.
- Doppelkolben und Signaldämpfer (IG31-12, 31.10.; seit MACH32-MOD 01.11. Pflicht-Sub-Mod Simple Machines, `modules/simplemachines`, `simplemachines:*`, Stufe 1 der Maschinen-Tab-Verschiebung, Plan `docs/ai/PLAN-B-MACHINES01-2026-11-01.md`; SB zeigt sie per Id in seinem Tab SimpleMachines, `util/MachineRefs`): keine Block-Entities, Bewegung über Vanillas `moving_piston`/`PistonStructureResolver` (Limit 12, Obsidian hält auf), Kolbenteile unverschiebbar, Schalter `features.doublePiston`/`signalDampener` in `config/simplemachines-server.json`; alte `simpleredstone:`- und `simplebuilding:`-Ids über `SimpleMachines#registerIdMoves`. Varianten (Astral/Nihil) nur über `DoublePistonVariant`, erst nach Besitzer-Abnahme.
- **A12 (Kern):** Der Kern greift an wenigen, benannten Stellen in Vanilla ein; jeder Teil hat einen Schalter, ist an Halten/Schleichen/Verzauberung gebunden oder fügt nur Neues hinzu. Werte mit Datei:Zeile und Urteil: `docs/ai/RELEASE-SIMPLEBUILDING.md` Abschnitt 4.12.
  - *Enderit-Ausrüstung über Netherit* (Werkzeuge, Speer, Rüstung): Nutzen: Endgame-Ziel nach Netherit. Ändert: neue Items, nur am Schmiedetisch (Enderit-Barren + Aufwertung aus Simple Ores); jede Kennzahl genau eine Stufe über Netherit (Rüstung 4/9/7/4 statt 3/8/6/3, Zähigkeit 4,0 statt 3,0, Rückstoßresistenz 0,2 statt 0,1, Werkzeug 2530 statt 2031 Haltbarkeit). Eingriff: gering (additive Stufe, Vanilla-Netherit bleibt). Begründung: Enderit ist die Endgame-Stufe der Mod; ohne sie bliebe Netherit das Ende der Leiter.
  - *Netherit-/Enderit-Äpfel und -Karotten* (APPLE41): Nutzen: Nahrung für Mittel- und Endgame, Beute-Ziel in Antiken Städten, Bastionen, Prüfungskammern und End-Städten. Ändert: neue Items (8 bzw. 14 Hunger bei den Äpfeln, Wirkungen von Sekunden bis Minuten), die verzauberten nur als Beute. Eingriff: gering bis mittel (stärker als der Vanilla-Goldapfel, aber kein Vanilla-Rezept ersetzt; Beute-Schalter). Begründung: bewusst teure Rezepte (8 Nuggets) und seltene Fundorte begrenzen den Machtzuwachs; die Werte stehen zur Besitzer-Abnahme offen.
  - *Aderabbau V / Tunnelgräber III / Vielseitigkeit II*: Nutzen: schneller Bergbau und Holzfällen. Ändert: nur mit der Verzauberung in der Haupthand und nur beim Schleichen bricht ein Hieb bis zu 3/6/9/12/18 verbundene Erz- oder Stammblöcke (Aderabbau) bzw. 1/2/4 Blöcke tief (Tunnelgräber, bremst den Abbau); Vielseitigkeit wechselt beim Schleichen und Schlagen zum besten Werkzeug. Eingriff: mittel, aber ohne Schleichen und ohne Buch unverändert Vanilla. Begründung: Bücher kommen nur aus Beute und von Händlern (nicht vom Zaubertisch), die Ausnahme ist an das Schleichen gebunden.
  - *Luftsprung / Kinetischer Schutz / Reichweite*: Luftsprung (Stiefel): ein zweiter Sprung in der Luft, Abklingzeit 20 s (Stufe II 10 s), vom Server erzwungen und mit `server.features.airJump` abschaltbar. Kinetischer Schutz: Rüstungs-Verzauberung nur gegen Wandaufprall beim Gleiten (2,5 Punkte je Stufe), mit den Vanilla-Schutz-Verzauberungen nicht kombinierbar. Reichweite: +2/+6/+10 Blöcke Blockreichweite, nur in der Haupthand und nur für Blöcke (Vanilla 4,5). Eingriff: gering bis mittel (Mobilität und Reichweite); Begründung: alles hängt an einer Verzauberung mit Obergrenze und Abklingzeit, nichts ist ohne sie aktiv.
  - *Besatz-Boni (Resonanz)*: Nutzen: Rüstungsbesätze bekommen spielerischen Wert. Ändert: kleine Boni je Teil (Schadensminderung einer Art, Tempo, Glück, Reichweite, Luft, Erfahrung), skaliert mit der persönlichen Resonanz (Basis 2,0 × Mittel aus Erfahrung, Überleben und Kampf, 0,2 bis 2,0). Der Kern ist Gastgeber (Anzeige, Stärken, Schalter), die Fähigkeiten wirken in Simple Mobs, gemessen wird in Simple Resonance. Eingriff: mittel (Spieler-Schutz), aber gedeckelt: von jedem Treffer kommen mindestens 20 % an, Boni gegen jeden Schaden zusammen höchstens 25 %, Tempo höchstens +20 %. Begründung: Deckel an Vanilla angelehnt (Schutz-Deckel, Schnelligkeit I, Atmung III); abschaltbar je Spieler (`enableArmorTrimBenefits`) und je Bonus (`server.trimStrengths.*`).
  - *Baulicht*: Nutzen: heller Bauschmuck, ohne Monster fernzuhalten (Fallen, Farmen). Ändert: Licht 15, aber sein Licht zählt bei der Monster-Spawnprüfung nicht (Umkreis 14 Blöcke), andere Lichter wirken weiter. Eingriff: gering, nur mit dem Block. Begründung: bewusster Bruch der Vanilla-Regel „Licht verhindert Spawns“, im Wiki-Eintrag des Baulichts erklärt.
  - *Stiller Löwenzahn*: Nutzen: einzelne Mobs per Rechtsklick stummschalten. Ändert: Item mit Verbrauch (eine Blume, 2 s Sperre), gespeichert am Mob. Eingriff: gering. Überschneidung: Simple Mob Manners (Simple Quality of Life) schaltet Mobs über eine Config-Liste stumm; beide bleiben (Item für Einzeltiere, Liste für ganze Arten), Doppelung ist im Release-Dokument vermerkt.
  - *Beute und Händler*: Nutzen: Bau-, Abbau- und Lager-Verzauberungen und Werkzeuge im Spielverlauf auffindbar. Ändert: zusätzliche Beute-Pools in 14 Struktur-Truhentypen und im Angel-Schatz (Kerne mit fester Chance je Kiste, Massenkisten im Schnitt höchstens ein halbes Mod-Item), 20 Händler-Angebote (Bibliothekar, Werkzeugschmied, Steinmetz, Fahrender Händler). Eingriff: mittel (additiv zu Vanilla-Beute und -Händlern, nichts entfernt), schaltbar mit `worldGen.enableLootTableChanges`, `server.loot.*` je Struktur, `globalLootMultiplier`, `enableVillagerTrades`, `enableWanderingTrades`, `tradePriceMultiplier`. Begründung: Gewichte nach `docs/LOOT-BALANCE.md`; der Store-Text nennt den Eingriff.
  - *End-Weltgenerierung und -Strukturen*: Nutzen: Fundorte für Astralit und Nihilit und kleine Erkundungsziele auf den äußeren End-Inseln. Ändert: zwei Erzadern (Astralit an der Oberfläche, Nihilit an der Unterseite), vier kleine Strukturen (Brunnen, falsches Gateway, Wrack, Pfad); dazu Rissiges-Eis-Felder in Eisbiomen und Sculk-Kiefern in der Deep Dark. Eingriff: gering bis mittel (additiv, End nur außerhalb der Hauptinsel). Schalter: Erze `server.oreGeneration.endOres/astralitOre/nihilitOre`, Eis-Felder `crackedIceFields`; Strukturen, Eis-Flecken und Sculk-Kiefern ohne Schalter (nur per Datenpaket) – siehe Abschnitt 4.12.
  - *Hängematten-Zeitraffer*: Nutzen: Tageszeit überspringen ohne Bett (kein Respawnpunkt). Ändert: solange genug Spieler (wie beim Schlafen, `playersSleepingPercentage` oder `server.hammock.restingPercentage`) in Hängematten liegen, läuft die Uhr mit Faktor 8; Wetter, Pflanzen, Öfen und Redstone laufen nicht schneller. Eingriff: mittel für die Tageszeit, sonst keiner; Faktor 1 schaltet ihn ab (`server.hammock.timeFactor`).
  - *Kleine Eingriffe (je ein Schalter, Standard an)*: neue Rüstungsständer haben Arme und lassen sich per Schleich-Rechtsklick bzw. bestromt mit dem Spieler tauschen (`armorStandArms`, `armorStandSwap`); Vanilla-Kleinteile (Stock, Barren, Edelsteine, Kerzen …) lassen sich per Schleichen + Rechtsklick ablegen (`placeVanillaItems`); Reparieren ohne neue Verzauberung erhöht die Ambosskosten nicht (`anvilRepairKeepsCost`); XP-Kugeln im Umkreis von 2 Blöcken verschmelzen und lassen sich ohne Verzögerung aufheben (`tweaks.optimization.enableXpClumps`). Eingriff: gering bis mittel (Ambosskosten); Begründung: Komfort, jeweils mit eigenem Schalter.
  - *Pfeil-Rückgewinnung*: Nutzen: teure Pfeile lohnen sich. Ändert: Pfeile eines Spielers, die ein Lebewesen getroffen haben, fallen bei dessen Tod (nie bei Spielern als Ziel, nie mit Durchschlag oder Unendlichkeit, höchstens 16 je Lebewesen; `server.arrows`). Eingriff: gering, abschaltbar (`recoverFromMobs`); sie gilt auch für Simple Arrows.

### Wiring Example (`wiringexample`)
Minimales Beispiel-Modul für Lader-/Integrations-Verdrahtung.

- keine eigenen Regeln bisher

### Simple Money (`simplemoney`)
Währung, gewichtete Dorfbewohner-Handel, Schatz-Loot.

- Preise nehmen fremde Items nur über `prices.json` je Modul (`modules/<id>/…/money/<ns>/prices.json`), nie über Klassen.
- Eine Preislogik (Besitzer 12.11., E1ANS F7): 1 Schein = 3 Diamanten = 16 Smaragde; alle eigenen Angebote folgen den Regeln in `modules/simplemoney/tools/source_prices.py` (dort ändern, nicht in den Handelsdateien). Angebote, die Vanilla-Knappheit aufheben, nur hinter eigenem Schalter (Standard aus).
- **A12 (PLAN40 simplemoney-2, 14.11.):** Nutzen: eine Wirtschaft für Survival-Welten (Schein herstellen, damit bei Dorfbewohnern und fahrendem Händler kaufen, Schein im Schatz-Loot). Änderung: eigene Items, zusätzliche Handelsangebote (Vanilla-Angebote bleiben im Pool) und ein Loot-Eintrag; Vanilla-Loot wird nicht ersetzt. Die Herstellkette ist bewusst teuer (Spezialfaser = Diamanten, dazu Gold, Eisen, Kupfer, Amethyst, Harz, Honigwabe, Hochofen-Zeit, etwa 3 Diamanten je Schein), damit der Schein eine Diamant-/Goldsenke ist und kein Loop entsteht (`tools/check_data.py` prüft Gewinn über Rezeptketten). Seltenes aus Vanilla (Knospender Amethyst, Diamanterz) nur hinter eigenem Schalter, Standard aus. Eingriff in Vanilla: gering bis mittel (Mixins nur auf Angebote mit Schein-Preis, Ergebnis-Slot und Schnellverschieben; bei „Enable Simple Money“ aus tun sie nichts, schon vorhandene Zusatzangebote sind dann gesperrt).
- **Zusatzangebote für andere Mods (PLAN40 simplemoney-3):** die Preistabellen folgen derselben Preislogik: Summe aus Modellstunden, Seltenheit und Rezeptplätzen in diamantähnlichen Einheiten, geteilt durch 3 (`PRICE_DIVISOR` in `MoneyLinks.java` und `tools/generate_links.py`), Mindestpreis je Zeile 1–11 (früher einheitlich 8). Verwaiste Angebotsdateien löscht der Generator und meldet sie in `--check`.

### Simple Riding (`simpleriding`)
Sattel-Tempo, Pferdesprünge, Nautilus-Dashes, Reittier-Rüstungen.

- **A12 (PLAN40 simpleriding-2, 14.11.):** Nutzen: Reiten lohnt sich länger (schnellere Sättel, höhere Sprünge, Nautilus-Dashes, Reittier-Rüstungen und Hufeisen mit Geländebonus). Änderung: die Vanilla-Reittiere bleiben, ihre Attribute werden über Sattel, Rüstung und Hufeisen angepasst; neue Hufplätze im Pferde-Menü. Eingriff in Vanilla: mittel – der Server prüft die Reittier-Bewegung selbst (`RidingSecurity`: Fahrpakete nach dem Thread-Wechsel, Horizontalbudget je Tick = min(Grenze, 0,125 + 3 × Tempo), Sprungpakete begrenzt), damit schnelle Sättel keine Teleport-Lücke öffnen. Schalter: „Enable Simple Riding“ und `/simpleriding config`. Offen: Latenz-Probe (PLAN40 simpleriding-3).

### Simple Models (`simplemodels`)
Server-genehmigte Item-Modelle, Amboss-Zuweisung, Modellbrowser.

- keine eigenen Regeln bisher

### Simple Fun (`simplefun`)
Spielerische Mechaniken (Wurfziegel, kosmetische Verwandlungen).

- **A12 (PLAN40 simplefun-2, 14.11.):** Nutzen: kleine Spaß-Funktionen für Survival, jede begrenzt (`MAX_YEET_STRENGTH` 3, Schaden ≤ 4, Wurftempo ≤ 1,5, Abklingzeit 10 Ticks). Standard an (Besitzer-Entscheide F12 u. a.): **Schleichwurf** (Werfen beim Schleichen, `enableYeet`, Stärke 3, gedeckelt auf 1,5 Blöcke je Tick) · **Ziegelwurf per Rechtsklick** (`enableThrowableBricks`; Glas zerbrechen `throwableBricksBreakBlocks` Standard aus) · **Schweine-Verwandlung** nach Schweinefleisch (`enablePiggyEffect`, 6000 Ticks, Milch hebt auf) · **Rückstoß IV** (`enableHigherKnockback`, Grenze `maxKnockback`) nur als Verzauberung per Schalter. Eingriff in Vanilla: gering bis mittel (Werfen/Rechtsklick sind Vanilla-Eingaben mit neuer Wirkung, jede Funktion einzeln abschaltbar; „Enable Simple Fun“ schaltet alles). Die 10 Spielzeuge (Konfetti-Knaller, Würfel, Abklatschen, Streicheln, Steinehüpfen, Wasserbombe, Seifenblasen, Papierflieger, Quietscheente, Schneeengel) sind eigene Items bzw. Gesten ohne Weltschaden.

### Simple Visuals (`simplevisuals`)
Client-HUD, Tooltips, Amboss-Formatierung, Immersions-Partikel.

- UI-Änderungen: Kontrast ≥ 4,5:1 (Regel A7).

### Simple Sounds (`simplesounds`)
Lokale Vanilla-Klangpartner der zwölf Immersions-Effekte.

- keine eigenen Regeln bisher

### Simple Quality of Life (`simplequalityoflife`)
Super-Mod für die QoL-Sub-Mods (Wetter, Interfaces, Loot, Mob-Manieren, Farming, Tools, Movement), je ein Schalter.

- Jedes Sub-Mod ist allein spielbar und hat seinen eigenen `modules.json`-Eintrag; der Super-Mod bündelt sie nur (`requires`).

### Simple Tweaks (`simpletweaks`)
- Standardwerte (Besitzer 17.10.): alles, was nur per Befehl ausgelöst wird, ist standardmäßig an; automatische Verhaltensänderungen nur, wenn der Besitzer es so festgelegt hat.
Legacy-Kompatibilität, serverautoritäre Claims (Standard an).

- Server-Autorität (AGENTS.md 3.8): alle Stellschrauben serverseitig mit Obergrenzen.
- Server-Werkzeuge (Besitzer 12.11., E1ANS F1): Block-Log, Auto-Backup und Anti-X-Ray standardmäßig nur auf Dedicated Servern (`onlyDedicated`), im Einzelspieler/LAN aus. Claims machen Besucher nicht wehrlos (F2: Monster, eigene Items/XP, Projektile zurück); Event-Abstimmungen ab 2 aktiven Spielern (F3).
- Schutz (17.10.): Anti-X-Ray Standard an (seit E1ANS nur auf Dedicated Servern), jede andere Schutzfunktion eigener Schalter Standard aus; Prüfungen nur protokollieren (kein Auto-Bann) und Grenzen immer aus den echten Serverwerten des Spielers (Attribute, Werkzeug, Verzauberungen, unsere Mixins) ableiten, nie aus festen Vanilla-Zahlen – serverseitige Mod-Aktionen (Hammer-Fläche, Baustab, Blaupause) werden nie gemeldet.
- Befehle (Besitzer 17.10.): Alle Befehle auch unter `/simpletweaks <befehl>` (ein Wort, ohne Bindestrich; Redirect auf dieselben Knoten, gleiche Rechte). Neue Befehle per `TweakCommands.add(dispatcher, literal(...))` registrieren (oder `TweakCommands.alias(...)` nach der Registrierung; `/tweaks`-Zweige werden automatisch übernommen) und den Register-Aufruf der Gruppe im Gametest (`AdminTests`) ergänzen; `/simpletweaks help` listet nur erlaubte; Doku und Wiki nennen die `/simpletweaks`-Form.
- **A12-Steckbrief (PLAN40 simpletweaks-3, 14.11.)** – je Tweak Nutzen · Änderung · Eingriff · Schalter (`config/simpletweaks-*.json`, `/simpletweaks config`):
  - **T27 Teil-Keep-Inventory** (`keepInventory`): Nutzen: Tod ist weniger frustrierend · Modi `hotbar_armor` (Hotbar, Rüstung, Nebenhand bleiben), `xp`, `custom` (je Teil + `keepXpPercent`); nur bei ausgeschalteter Vanilla-Regel `keep_inventory`, nie in Hardcore · mittel (ändert die Vanilla-Todesregel; Reihenfolge mit Seelengebunden/Totenspeicher-Mods dokumentiert in `KeepInventory`; offen: simpletweaks-2).
  - **Todes-Hinweis** (`deathHint`): Nutzen: man findet die Todesstelle · nur der Gestorbene erhält eine Zeile mit Ort (Klick kopiert), Dimension, Ursache und Restzeit der Items (5 Min) · gering.
  - **AFK zählt nicht beim Schlafen** (`afk.excludeFromSleep`, `afk.minutes`): Nutzen: ein AFK-Spieler hält die Nacht nicht auf · nur die Schlaf-Prozentzahl, sonst Vanilla · gering; Kick ist eigener Schalter, Standard aus.
  - **Neulingsschutz** (`newPlayerShield`, `minutes`, `attackEnds`): Nutzen: neue Spieler werden nicht sofort von anderen Spielern getötet · solange die Spielzeit unter `minutes` liegt, verletzt kein Spieler sie und sie keinen (Nahkampf, Pfeile, schädliche Tränke); endet einmalig, auch bei eigenem Angriff (`attackEnds`); Mobs bleiben Vanilla · gering bis mittel (abschaltbar).
  - **Drachenei bei jedem Kampf** (`dragonEgg`): Nutzen: jeder Drachenkampf lohnt sich · Vanilla setzt das Ei nur nach dem ersten Kampf; jeder weitere Kill legt es auf das Portalpodest (oder wirft es als Item, wenn dort schon eins liegt) · mittel (Vanilla-Seltenheit entfällt), abschaltbar.
  - Admin-, Spieler- und Schutz-Teile stehen in der Store-Seite getrennt; Sub-Mod-Schalter „Enable Simple Tweaks“ schaltet alles ab.

### Simple Dimensions (`simpledimensions`)
Konfigurierbare Dimensionen, Lichtbögen, sichere verknüpfte Portale.

- **Portalformen (N33, Besitzer 12.11.):** ein Portal darf ein Bogen sein – gerader Boden, Seiten von unten nach oben nur
  gleich oder nach innen, freie Oberkante, Rahmen seitlich/diagonal verbunden (keine Eckblöcke nötig), Innenfläche 4-dicht;
  Netherportal 1 × 2 bis 21 × 21. Was Vanilla annimmt, bleibt Vanilla; alles andere ist sichtbar anders (40 % dunkler).
  Dieselben Regeln gelten für die eigenen Dimensions-Portale; ein Pack legt je Portal/Rezept `shape` (rectangle|arch|any,
  Maße) oder ein festes Muster fest. Obsidian bleibt für Vanilla reserviert. Abgelehnte Versuche nennen den Grund in der
  Aktionsleiste (Besitzer-Ausnahme zu AGENTS 3.4).
- **A12 (PLAN40 simpledimensions-2):** Nutzen: drei eigene Reise-Welten (Skyblock, Rohstoffwelt Mining, Reise-Welt Travel mit Abkürzung 10:1 statt Nether 8:1) und Portale in Bogenform, ohne dass Vanilla-Dimensionen verändert werden; die Welten und Portalrezepte sind per Datenpack/`server.json` anpassbar. Eingriff in Vanilla: gering bis mittel. Immer an (Schalter „Enable Simple Dimensions“, je Welt `skyblockEnabled`/`miningEnabled`/`travelEnabled`): eigene Dimensionen und Portalblock; Bogen-Netherportal (nur 40 % dunkler, wo Vanilla es nicht annimmt; Mixin `BaseFireBlockMixin`). Bewusste Abweichung: es reisen nur Spieler ohne Reittier und ohne Passagier (Tiere und Items reisen nicht mit, sonst Dupe-/Absturzgefahr); Feuerzeug/Feuerkugel-Klicks werden nur an einem echten Portalrahmen gedrosselt (Vanilla-Anzünden bleibt unberührt). Die Rettung in Mod-Dimensionen (Notfall-Rückkehr unter der Weltgrenze) ist per Config wählbar. Abhängigkeiten: nur SimpleLib; Cloth Config ist optional (nur der Einstellungsbildschirm braucht es). Alte Planungsdateien liegen archiviert unter `docs/ai/PLAN-DIMENSIONS-*-ARCHIV.md`.

### Simple Sandwiches (`simplesandwiches`)
Sandwiches auf dem Schneidebrett, Küchenmesser, Käse und Butter.

- Gemeinsame Nahrung („warme Speisen“) liegt in simplelib.
- **A12** (nachgetragen RELSB56, aus Code/Handbuch, keine Wertänderung): Nutzen: neue Nahrung (Sandwiches nach Zutaten, Butter, Käse) für Überleben und Kochen. Änderung: eigene Blöcke/Items; Vanilla-Haken: Essen aus dem Bündel (`BundleEatMixin`, Schalter `bundleEating`), Messer schneidet Kuchen-Stücke (`CakeKnifeMixin`, `CandleCakeKnifeMixin`), Butter-Effekt „Rutschig“ (`SlipperyFrictionMixin`), Kessel-Milch reift zu Butter/Käse. Eingriff in Vanilla: gering – Vanilla-Nahrung bleibt unverändert, die Haken greifen nur mit Mod-Items.

### SimpleLib (`simplelib`)
Gemeinsame Bibliothek: geteilte Materialien, IdMoves, In-World-Upgrades, UI-Bausteine.

- Alles, was mehrere Module brauchen, liegt hier (Regel A2), mit Mindestversion in den Abhängigkeiten.
- IG31-13 (31.10.): die sechs Baukerne (`simplelib:*_core`, neutrales `CoreItem`, Verhalten per `CoreHooks` vom Partner) liegen hier; Enderit gehört Simple Ores. simplelib kennt Enderit nur als *bereitgestellte* Gruppe (`provideMaterial`/`materialPresent`, Bedingung `simplelib:material_requested`), ohne den Besitzer zu nennen.
- UI-Bausteine (z. B. `UiFilterButton`, Filter-Prinzip A6) hier dokumentieren und wiederverwenden, nicht kopieren.
- **A12 (simplelib-5, Audit `modules/simplelib/AUDIT.md`):** Nutzen: ein gemeinsamer Unterbau, damit jeder Simple-Mod allein spielbar bleibt (geteilte Materialien, Id-Umzüge, In-Welt-Aufwertung, Handbuch, warmes Essen). Eingriff in Vanilla: mittel. Immer an: Wirrwarzen in den Truhen der Bastionen, 15 % schnelleres Essen nur für warm gehaltene Speisen (`eatSpeedBonus`, höchstens 30 %), der Hänger-Wächter (schreibt nur ein Thread-Log), die Kreativ-Tab-Regeln (heute für alle Mods im Spiel – Rückfrage AUDIT P2-1) und die gemeinsamen Mixins (Id-Umzüge, Amboss, Trichter-Stapelgrenzen, Stapelvergleich warmer Speisen, Wither). Nur auf Anforderung eines geladenen Mods: Wither-Drop der Starken Aufwertung, Amboss zerschlägt Diamantblöcke (`requestMaterial`), der Chor der Legendären Momente samt Stummschaltung des Vanilla-Herausforderungsklangs und Free Look mit Standardtaste linke Alt (`simplelib:features`, simplelib-3; sonst ohne Taste). Abschaltbar in `simplelib-server.json` (`legendaryEffects`, `legendaryMuteToast`, `hangWatchdog`, `eatSpeedBonus`).

### Simple Interfaces (`simpleinterfaces`)
Container-Bildschirme im Kastenstil, verknüpfte Container, Easy Shulkers / Ender Chests; Sub-Mod von QoL.

- UI-Kontrast ≥ 4,5:1 (A7); Stil nah an Vanilla.

### Simple Maps (`simplemaps`)
Wegweiser-Karten, Wegpunkte, gerahmte Ansichten, Kartentisch-Upgrades.

- **A12 (PLAN40 simplemaps-4, 14.11.):** Nutzen: Entdecker bekommen endlose Karten je Dimension mit Zoom, Wegpunkten in der Ortungsleiste und Kartentisch-Upgrades; Bildkarten (PNG-Kartenwand) für Bauer. Änderung: eigene Kartenart, Vanilla-Karten funktionieren unverändert weiter (Kartentisch bekommt zusätzliche Upgrade-Rezepte). Eingriff in Vanilla: gering bis mittel (Kartenbildschirm und Ortungsleiste bekommen Client-Hooks; Web-Export Standard aus). Schalter: „Enable Simple Maps“ und `/simplemaps config`.

### Simple Livemap (`simplelivemap`)
Live-Webkarte der wirklich erkundeten Welt (Besitzer ABN30-12: eigener Mod parallel zu Simple Maps, kein Sub-Mod).

- „Erkundet“ = besuchte Chunks (je Spieler) + gehaltene Vanilla-Karten; nie Pregen, nie Seed-/Biom-Abfragen, nie Chunks laden.
- Webserver Standard aus, Bind 127.0.0.1, privat mit Schlüssel (nur im URL-Fragment/Bearer, nie in Query oder Log); öffentlich nur ausdrücklich.
- Spielerdaten nur Name/Dimension/x/z, Standard aus, jeder kann sich ausblenden; keine Klassen aus Simple Maps (Regel 3).

### Simple Trims (`simpletrims`)
Leuchtende/Emittierende/Pulsierende Besatzvorlagen, platzierbare Vorlagen, Besatz-Upgrades.

- In-World-Upgrades: Axt allein, Vorschlaghammer mit SimpleBuilding.
- Hammer-Regel: In-World-Upgrade per Axt allein; Hammer-Partner nur über den Tag `#simplelib:hammers` (optional, z. B. Simple Hammers), kein Zwang zu SimpleBuilding.
- **A12 (PLAN40 simpletrims-4, 14.11.):** Nutzen: vier Besatz-Vorlagen (Leuchtend, Emittierend, Pulsierend, Invertierend) für Spieler, die Rüstungsoptik mögen; platzierbare Vorlagen und Upgrades. Änderung: Vanilla-Besatz bleibt unverändert; leuchtende Rüstung und Fackeln in der Hand setzen **unsichtbare Lichtblöcke** in die Welt (Eingriff in Weltblöcke, Handlicht getrennt abschaltbar, falls die Serverlast-Messung es verlangt; PLAN40 simpletrims-2). Eingriff in Vanilla: gering bis mittel.

### Simple Mobs (`simplemobs`)
Mobs: Hollow Howler, Hollow Watcher, Shellker, Soulfire Blaze, Dread, Deceiver. Bosse: Void Runner, Brimguard, gepanzerte Wither (Blighted/Abyssal).
Verwitterte Prüfungen mit Bösem/Leere-Omen, Prüfungen, Überfällen, Tresoren und Schlüsseln. Schwingen Stufe 1–4 mit Netherherz, Rotem
Nether-Juwel, Ender-Herz und Ender-Juwel. Spawner-Fragment, -Brocken und Behelfsmäßiger Spawner. Starkes Schwarzpulver, Starkes TNT, Starke
Rakete. Trompete. Tarnumhang (Verwandlung) und Trugbild-Spiegel. Kampf-Effekte, Besatz-Fähigkeiten, Brutmulde mit Mob-Essenz, Trank der Stille.
Pflicht-Sub-Mod von SimpleBuilding, braucht SimpleLib und Simple Resonance (`modules/modules.json`); die Biome Brush liegt in Simple Tools.

- **A12-Steckbrief (PLAN40 simplemobs-5, 12.11.)** – je Gruppe Nutzen · Änderung · Eingriff · Schalter (`config/simplemobs-server.json`, `/simplemobs config`; alles zusammen: Sub-Mod-Schalter „Enable Simple Mobs“):
  - **Neue Mobs** (Howler, Watcher, Shellker, Soulfire Blaze, Dread, Deceiver): Gegner für Orte, an denen Vanilla wenig Neues bietet (Deep Dark, äußere End-Inseln, Seelensandtal, End-Tore) · zusätzliche Spawns bzw. Beschwörungen, Vanilla-Mobs bleiben unverändert · schwach bis mittel (Howler und Dread nur per Ritual; Deceiver, Watcher und Lohe selten natürlich; Shellker nur an neuen End-Toren) · `howlerEnabled`, `dreadSummonEnabled`, `hollowWatcherSpawns`, `soulfireBlazeSpawns`, `shellkerGatewayGuards`.
  - **Bosse** (Void Runner, Brimguard, Blighted/Abyssal Wither): Ziele nach dem Enderdrachen bzw. ein härterer Wither, Quelle der Schwingen-Materialien · nur beschworen (Figur + Gaben), nie natürlich, nie auf Friedlich; ohne Helm bleibt der Vanilla-Wither · schwach (freiwillig) · `voidRunnerSummonEnabled`, `brimguardSummonEnabled`, `bossRitualsEndOnly`, `witherSummonEnabled`, `witherWilt`, `minibossBars`.
  - **Verwitterte Prüfungen + Böse/Leere-Kette** (Omen, Prüfungen, Überfälle, Tresore, Schlüssel): die Trial-Chambers-Schleife und Überfälle fürs Netherit-Zeitalter · eigene seltene Struktur; erst ein gebrauter Böser Trank (mit Netherherz) macht Prüfungen/Dorf-Überfälle stärker; Vanilla-Trial-Chambers bleiben unberührt · mittel (nur nach eigenem Trank) · `evilOmen`, `voidChain` (Standard aus), `evilOnVanillaSpawners` (Standard aus), `weatheredStructures`, `weatheredDecayPercent`, `trialSpawnerCap`, `evilHitEffects`, `voidMobTricks`, `evilRaidExtras`, `trialLootNoRepeat`. Die Aktionsleisten-Ansagen der Kette (Prüfung wird böse, Überfall beginnt, Plündererbeute-Truhe) bleiben als Ereignis-Telegrafie (A14), keine Gadget-Meldung.
  - **Schwingen 1–4** (Netherherz, Rotes Nether-Juwel, Ender-Herz, Ender-Juwel): Fortschritt über die Elytren hinaus · nur die Höchstgeschwindigkeit mit Starken Raketen steigt (x2/x4/x6/x8), Gleiten und normale Raketen bleiben Vanilla · mittel (schnelleres Reisen, nur mit Boss-Materialien) · `witherWingsSpeedFactor` … `voidWingsSpeedFactor` (1 = Vanilla).
  - **Spawner-Teile** (Fragment, Brocken, Behelfsmäßiger Spawner): ein Vanilla-Spawner lässt sich nie mitnehmen; hier hinterlässt ein abgebauter mit 30 % ein Fragment (Besitzer WF25-6), 81 Fragmente ≈ 270 Spawner ergeben einen Behelfsmäßigen Spawner, der 3-mal langsamer läuft (kein EP-Kreislauf, Audit P1-1) · ändert nur das Abbauen von Vanilla-Spawnern · schwach (Fernziel) · `spawnerFragmentChance` (0 = aus), `makeshiftSpawnerSlowdown`.
  - **Starke Sprengstoffe** (Schwarzpulver, TNT, Rakete): Treibstoff der Schwingen, größere Sprengung · eigene Items, Vanilla-Pulver/-TNT/-Raketen bleiben · schwach · `strongTntPower` (4–16); Spielregel `tntExplodes` gilt wie bei Vanilla-TNT (auch dessen Meldung).
  - **Trompete**: Instrument wie das Ziegenhorn (Tonhöhe nach Blickwinkel), herstellbar (BS28-5) · ändert am Spiel nichts · keiner · `trumpetEnabled`.
  - **Tarnumhang / Verwandlung + Trugbild-Spiegel**: Schleichen und Erkunden ohne Unsichtbarkeitstrank, Gegenstück zum Deceiver · Mobs bemerken den Träger erst im 3-Block-Radius, Verwandlung in Mob oder Spieler, Bosse durchschauen sie, jeder Schlag beendet sie · mittel · Spiegel Standard aus (`trickMirrorEnabled`, N61 E11). Rückmeldungen nur Klang + Partikel (AGENTS 3.4, PLAN40 simplemobs-2).
  - **Kampf-Effekte** (MOB27-1): 8 gebaute Statuseffekte – Explosiv, Abwehr, Spiegel (mit Tränken; Böse Stufen 4/5, Zonen) und die fünf schwachen FX-1-Effekte Blutsauger, Frostbiss, Magnetisch, Leuchtspur, Zerfall (bisher ohne Quelle im Spiel außer `/effect`) · wirken nur auf den Träger · schwach bis mittel · Spiegel bleibt Münzwurf mit Stufen 20→50 % (`mirrorReflectChanceMin`/`mirrorReflectChance`). Die übrigen 8 Vorschläge (Echo, Steinhaut, Adrenalin, Spröde, Sog, Anker, Abprall, Zeitlupe) erst nach Release.
  - **Besatz-Fähigkeiten** (ZO9/MOB27-3): jedes Besatzmuster und -material gibt eine kleine Fähigkeit (weniger Schaden einer Art, Tempo, Glück, Reichweite, Luft, Erfahrung), wächst mit der Resonanz · nur Träger von Besätzen · schwach bis mittel · `trimStrength` (0 = aus).
  - **Brutmulde + Mob-Essenz**: Spawn-Eier im Überlebensmodus (Vanilla stellt keine her) · selten Essenz von Mobs, die ein Spieler tötet (nicht aus Spawnern/Eiern), Bosse nie · mittel · `eggRitualEnabled`, `essenceChance`.
  - **Trank der Stille**: Sculk-/Wärter-Begegnungen leiser · der Träger dämpft seine Vibrationen wie Wolle · schwach · kein eigener Schalter (Sub-Mod).
- Sculk-Kiefer (Howler-Falle) gehört thematisch hierher; liegt bis zur Verschiebung in SimpleBuilding.
- Gefahrenzonen (WP-22, MOB27-2): neue Monster bekommen über `ResonanceContracts` (Simple Resonance, nur optional) Chancen auf Statuseffekte, nie mehr Leben oder Attribute; Hook nur `Mob.finalizeSpawn` RETURN, nur natürliche Spawns/Patrouillen/Chunk-Erzeugung von Monstern; `zoneMobsEnabled` Standard aus (A12: Eingriff mittel, bis zur Client-Abnahme).

### Simple Weather (`simpleweather`)
Wetter-Schalter (Server), Regendichte (Client); Sub-Mod von QoL.

- keine eigenen Regeln bisher

### Simple Movement (`simplemovement`)
Manuelles Kriechen, Autowalk, schnellere Leitern, Frost Walker auf Pulverschnee; Sub-Mod von QoL.

- keine eigenen Regeln bisher

### Simple Tools (`simpletools`)
Volle Haltbarkeit, Thrift und Soulbound, Todes-Tresor, Amboss-Reparaturen; Sub-Mod von QoL (kein Werkzeug-Item).

- Namenskollision beachten: Werkzeug-Items (Hämmer, Meißel, Stäbe) gehören nicht hierher, sondern zu SimpleBuilding bzw. dem geplanten `simplehammers`.

### Simple Farming (`simplefarming`)
Federfall schützt Ackerland, Schärfe schneidet Gras, Hacke erntet und pflanzt neu; Sub-Mod von QoL.

- keine eigenen Regeln bisher

### Simple Mob Manners (`simplemobmanners`)
Mobs stummschalten, benannte Babys jung halten, Piglins ignorieren Gold; Sub-Mod von QoL.

- keine eigenen Regeln bisher

### Simple Loot (`simpleloot`)
Tresor-Abklingzeit in Tagen; Sub-Mod von QoL.

- keine eigenen Regeln bisher

### Simple Questionary (`simplequestionary`)
Fragentisch: Item einlegen, Lore und Geheimnis im offenen Buch.

- **A12 (PLAN40 simplequestionary-3, 14.11.):** Nutzen: Einsteiger lesen zu jedem Item Herkunft, Nutzen und Geheimnis. Änderung: ein eigener Block (Fragetisch) mit Buch-Bildschirm; für Items ohne Text stehen Ersatztexte und automatisch erzeugte Rezept-Fakten. Eingriff in Vanilla: keiner (kein Mixin auf Vanilla-Verhalten, keine geänderten Vanilla-Daten).

### Simple Aesthetics (`simpleaesthetics`)
Super-Mod ohne eigenen Inhalt: Visuals, Sounds, Models mit je einem Schalter.

- keine eigenen Regeln bisher

### Simple End Dragon Fight (`simpleenddragonfight`)
Enderdrachenkampf: Lebensbalken je Spieler, Dragonmites; Pflicht: SimpleLib, optional Simple Mobs.

- **A12:** Nutzen: der Kampf wächst mit der Spielerzahl (eine Leiste je Spieler), Eintritts-Animation, Dragonmites ab halber Gesundheit. Standard für Einzelspieler = Vanilla (200 Leben, normales Tempo, keine dauerhafte Steigerung je Beschwörung; Besitzer PLAN40 „Empfehlung A“). Eingriff in Vanilla: Mixins an `EnderDragon` (Leben/Tempo/Schaden, nur der Vanilla-Kampf), Elytra-Reparatur mit Drachen-Chitin (Komponente, additiv).
- Belohnung ab dem 4./10. Drachen: Enderherz/Ender-Juwel aus Simple Mobs (nur per Item-Id gesucht); ohne Simple Mobs stattdessen Erfahrung (`rewardXpFallback`).
- Drachen-Chitin repariert Elytren am Amboss wie Phantomhaut (SE-DF-2).

### Simple End (`simpleend`)
Endholz (Aschholz, Dämmerholz), Knospen, Bäume, Endgras, Biome auf den äußeren Inseln; bündelt Simple End Dragon Fight.

- Hölzer mit Treppe/Stufe bekommen Bretter-Achtel in Simple Octets (O1), bedingt bei geladenem Simple End.
- Künftig: End-Baublöcke (polierter Endstein-Familie) – gemeinsamer Stein liegt in simplelib.
- **A12** (nachgetragen RELSB56, aus Code/Handbuch, keine Wertänderung): Nutzen: Ziel für Spieler nach dem Drachen (neue Hölzer, Pflanzen, zwei Biome auf den äußeren Inseln). Änderung: Aschtal/Dämmerland ersetzen Teile der äußeren Hoch-/Mittelländer (`TheEndBiomeSourceMixin`, Schalter `biomes`, `biomeChance`); End-Holz-Items überleben die Leere und liegen doppelt so lange (`ItemEntityMixin`, `voidSafeItems`, `longerDespawn`); Nebelwand im Wald (`forestFogWall`). Eingriff in Vanilla: mittel (Weltgenerierung der äußeren Inseln), abschaltbar; Hauptinsel und Drachenkampf unverändert (Kampf nur mit Simple End Dragon Fight, eigener Schalter).

### Simple Respawn (`simplerespawn`)
Niedergeschlagen statt tot: Teamkameraden können wiederbeleben.

- **A12 (PLAN40 simplerespawn-2):** Nutzen: Mehrspieler-Teamwork – wer tödlich getroffen wird, liegt 60 s am Boden und kann von einem Mitspieler wiederbelebt werden, statt Inventar zu verlieren. Eingriff in Vanilla: stark (der Tod selbst), deshalb begrenzt: wirkt nur, wenn ein anderer Spieler in derselben Dimension und höchstens 128 Blöcke entfernt helfen könnte (`onlyWithOtherPlayers`, `helperMaxDistance`; allein gilt der Vanilla-Tod); Totem, Void und `/kill` gehen vor; der echte Tod bleibt der Vanilla-Tod (Drops, Statistik, Loader-Ereignisse). Abwägung: Monster ignorieren Niedergeschlagene (`mobsIgnoreDowned`, abschaltbar für mehr Härte). Alles abschaltbar (`enabled`, „Enable Simple Respawn“). Ohne die Mod im Client gibt es keinen Bildschirm; Ausweg ist `/simplerespawn giveup` (im Handbuch genannt).

### Simple Beacons (`simplebeacons`)
Starker Leuchtturm, starke Conduit, Upgrade-Vorlage.

- In-World-Upgrades: Axt allein, Vorschlaghammer mit einem Hammer-Mod.
- **A12:** Nutzen: Langzeitziel nach dem Vanilla-Leuchtfeuer. Einziger Eingriff in Vanilla: Reichweite des Vanilla-Leuchtfeuers nach Pyramidenmaterial (`vanillaBeaconMaterialRange`, Standard an, Eisen = Vanilla). Böse Varianten sind absichtliche Schwächung (Schonfrist nach Tod, fremde Claims verschont). Der Sättigungs-Puls (nie hungrig in Reichweite) bleibt als Stützpunkt-Belohnung und ist je Server abschaltbar (`strongBeaconEffects.saturation`); Besitzer PLAN40 „Empfehlung okay“.
- Simple Mobs optional, aber empfohlen (8 Rezepte hängen an Nether-Herz/-Juwel); ohne sie nur kreativ/Beute.

### Simple Ovens (`simpleovens`)
Verstärkte, Netherit- und Enderit-Öfen, Räucher- und Hochöfen, Ofen-Loren; Pflicht-Sub-Mod von SimpleBuilding.

- In-World-Upgrades: Axt allein, Vorschlaghammer in SimpleBuilding. Ids wanderten aus SimpleBuilding (IdMoves).
- **A12 (PLAN40 simpleovens-3):** Nutzen: schneller schmelzen, räuchern und erzschmelzen im Mittel- und Spätspiel (2×/4×/8×), doppelte Erfahrung ab Netherit, dazu Antriebsloren und Ofenboote aller Vanilla-Hölzer. Ändert die Vanilla-Öfen nicht: die Stufen sind eigene Blöcke (Vanilla-Unterklassen mit Vanilla-Menüs, Trichter, Komparator und Abbau wie Vanilla); einziger Mixin `AbstractFurnaceMenuMixin` teilt nur Garzeiten über 32767 für die Anzeige. Balance: Brennstoff je Gegenstand wie in der Vanilla-Maschine (schneller, nicht billiger; PLAN40 simpleovens-2), die frühere Ersparnis nur per Config `fuelEfficiency`; keine Erfahrung aus dem Rissdiamant-Kreislauf (`#simpleovens:furnace_bonus_excluded`). Eingriff in Vanilla: gering, abschaltbar (`enabled`, „Enable Simple Ovens“).

### Simple Mobheads (`simplemobheads`)
Kopf für jeden Mob; Pflicht-Sub-Mod von SimpleBuilding.

- Abhängigkeiten: nur SimpleLib; Cloth Config ist optional (nur der Einstellungsbildschirm über Mod Menu braucht es, `ModMenuIntegration` prüft es; PLAN40 simplemobheads-2).
- **A12 (PLAN40 simplemobheads-2, Besitzer-Entscheide 29.09./13.10./KONZ20/EH21 bleiben):** Nutzen: Sammel- und Deko-Ziel für jeden Mob (Notenblock spielt den Mob-Klang), Köpfe fallen nach Vanilla-Regel (Explosion eines geladenen Creepers, ein Kopf je Explosion); kleine Trage-Fähigkeiten, je Fähigkeit abschaltbar (`abilities`). Eingriffe, standardmäßig an und begründet: (1) **Zusatz-Kopfplatz** (SimpleBuilding, N49): Kopf und Helm zugleich – Fähigkeit plus voller Rüstungsschutz; bewusst, weil die Fähigkeiten klein sind und sonst kein Kopf getragen würde; nur mit SimpleBuilding. (2) **Easter-Spawns**: Killerkaninchen (1 : 1000 in Blumenwald/Wiese), Riesenzombie (natürlicher Blitz bei Vollmond), Riesenkuh (1 : 5000 je gefressenem Heuballen) – selten, nur Spaß, einzeln abschaltbar (`killerBunnies`, `giantZombies`, `giantCows`); der Riesenkopf prüft beim Wachsen den Platz (AUDSB31B F8, a-audsb31b). (3) **Herobrine**: Ritual nur auf ausdrückliche Tat des Spielers (goldener Schrein bei Nacht), danach fünf Minuten Spuk nur für diesen Spieler ohne Weltänderung (`herobrineRitual`, `hauntPhase`, Hinweisbuch `hintBook`). Keine Vanilla-Beutetabelle wird ersetzt, Wither-Kopf standardmäßig aus. Eingriff in Vanilla: gering bis mittel (seltene Zusatz-Spawns), alles abschaltbar außer dem Zusatz-Kopfplatz (SimpleBuilding).

### Simple Shops (`simpleshops`)
Spieler-Shops: Verkaufsblock mit Preis, Vitrine mit Waren.

- Käufer brauchen kein Claim-Recht; Schutz vor Fremden, Explosionen, Kolben, Trichtern.
- Der Besitzer geht immer vor (Besitzer 12.11., E1ANS F5): ein Kunden-Fenster wird nicht geschlossen, sondern wartet hinter dem Wartungsschild (`ownerFirst`).
- **A12** (nachgetragen RELSB56, aus Code/Handbuch, keine Wertänderung): Nutzen: Handel zwischen Spielern auf Servern (Verkaufsblock, Vitrine). Änderung: eigene Blöcke; ein Vanilla-Haken (`ServerPlayerGameModeMixin`) schützt Shop-Blöcke vor Abbau durch Fremde vor allen anderen Schutz-Mods. Eingriff in Vanilla: gering – Dorfbewohner-Handel unberührt, wirkt nur auf Shop-Blöcke; Grenzen per Config (`maxShopsPerPlayer`, `remoteStock`, `shopMusic`).

### Simple Wanderers (`simplewanderers`)
Dorf-Wanderer: ein fahrender Händler je Dorf mit Pferd, Warenwechsel alle 3 Tage.

- **A12:** Nutzen: jedes Dorf bekommt einen Händler mit Pferd; der Alltagskram (47 von 78 Waren) liegt dort, der fahrende Händler behält die exotische Hälfte. Eingriff in Vanilla: ein kleiner Mixin (`WanderingTraderSplitMixin`) wählt die Händler-Hälfte nur bei eingeschalteter Mod; Vanilla-Listen bleiben unverändert, Waren fremder Mods im Vanilla-Tag bleiben beim fahrenden Händler (SW2B-2). Mod aus = Vanilla-Händler wie zuvor.
- Kein Wanderer ohne Dorf-Glocke einer Dorfstruktur; das Pferd ist einfangbar (Besitzer W4), nie ein Spawn-Ei-Spam: höchstens ein Pferd je Dorf und Respawn-Zeit.
- Pflicht: SimpleLib (Fabric-API ohnehin); Simple Money / Simple Building / Simple Mobheads nur als optionale Waren.

### Simple Animals (`simpleanimals`)
Fünf Tiere, die Vanilla nicht hat: Hirsch, Wildschwein, Igel, Krebs, Zebra (mit unfruchtbarem Zorse); allein mit Vanilla spielbar.

- **A12 je Tier** steht in `docs/ai/PLAN-A-ANIMALS-2026-10-29.md`; neue Tiere kommen erst nach Besitzer-Wahl dazu (SPÄTER-Liste im Konzept WP-18).
- Natürliches Spawnen nur über niedrige Gewichte (1 bis 4) und die Regel in `checkSpawnRules` der Entität (Tageslicht, Boden); Ei, Spawner und Zucht sind nie begrenzt. Kein Blockschaden, nur Vanilla-Goals.
- Modelle und Texturen kommen aus einer Spezifikation (`tools/gen_models.py`, `tools/gen_items.py`); Java-`*Layers` nie von Hand ändern.
### Simple Resonance (`simpleresonance`)
Spielerwert, Gefahrenzonen, Spawn-Besitzer-Wahl und Anfängerschutz als Schnittstelle für andere Mods (`framework` `ResonanceContracts`, reines Java); allein mit Vanilla spielbar.

- **A12:** Nutzen: ein gemeinsamer, erklärbarer Erfahrungswert statt verstreuter Zähler; Zonen machen fernes Gelände abwechslungsreicher. Ändert für sich nichts am Spiel (nur Messung und `/simpleresonance`); die Wirkung kommt erst durch Verwender wie Simple Mobs (`zoneMobsEnabled`, Standard aus). Eingriff in Vanilla: keiner im Modul selbst.
- Der Wert steuert nur Chancen, nie Leben oder Schaden. Kein Modul importiert ein anderes: Verwender fragen `ResonanceContracts.get()` und behandeln `null` als „Vanilla“.
- Spielerdaten liegen in einem `SavedData` (`simpleresonance:players`), nicht im Spieler-NBT; der alte SB-Block `SimpleBuildingData` wird einmal gelesen (Mixin) und bleibt im Kern unverändert (Welten brechen nicht).
- Konstanten stehen in `ResonanceMath`/`ResonanceConfig`; Wiki-Zahlen müssen dort beweisbar sein. Neue Verwender nutzen den Vertrag, keine Klassen des Moduls.

### Simple Dinosaurs (`simpledinos`)
Fossilienblock, Dinosaurier-Eier zum Freipinseln und Ausbrüten (Daraufsitzen) und die Dinosaurier selbst (Stufe 1: der reitbare T-Rex); allein mit Vanilla spielbar.

- **A12 je Teil** steht in `docs/ai/PLAN-A-DINOS-2026-10-30.md`; Dinosaurier gibt es nur durch Schlüpfen, nie wild, sie greifen nie Spieler an und zerstören keine Blöcke.
- Fortschritte (Pinseln, Brüten) laufen über `Progress` (reine Rechnung mit Zeitstempeln, Rücklauf 2 pro Tick nach der Gnadenfrist); kein Dauer-Tick im Block, nur geplante Block-Ticks.
- Die Zeiten stehen in `simpledinos-server.json` (Standard = Konzept: 60 s, 5 min, 10 min, 40 min); neue Sorten erst nach Besitzer-Abnahme des Musters (T-Rex).
- Modelle und Texturen kommen aus Spezifikationen (`tools/gen_models.py`, `tools/gen_blocks.py`); Java-`*Layers` nie von Hand ändern.
- Jede neue Entität des Moduls braucht eine Ausnahme in `simplemobheads/data/mobs.json`, solange es keinen Kopf gibt.

### Simple Arrows (`simplearrows`)
Pfeile als Pflicht-Sub-Mod von SB (b-arrows02, 03.11.), nur Fabric 26.3, allein mit Vanilla spielbar.

- Silberpfeil (aus Simple Ores umgezogen, alte `simpleores:`-Ids per `SimpleArrows#registerIdMoves`): Rezept nur mit `fabric:all_mods_loaded ["simpleores"]`, ohne Ores kein Kreativ-Eintrag.
- Lichtpfeil: Licht = Vanillas `minecraft:light` (Stufe 15) in der Luft vor der getroffenen Fläche, gehört dem Pfeil (`light_pos`), verschwindet mit ihm (Despawn wie ein normaler Pfeil, Aufheben, `/kill`, Herausfallen); entfernt wird nur ein Block, der noch `minecraft:light` ist.
- Texturen Vanilla-abgeleitet (`tools/textures/arrows_derived.py`), nie eingecheckt.
- Befiederungstisch: seit b-arrows03 (04.11.) liegt er vollständig in Simple Arrows – Menü, Rezepte, gefertigter Pfeil und Trank-Slot (WE25-9); der Rechtsklick auf den Vanilla-Block öffnet das Menü (`modules/simplearrows/shared/java/com/simplearrows/mixin/FletchingTableMixin.java`, Registrierungen `fletching/Fletchings.java`). Der SB-Kern behält nur die Pfeil-Rückgewinnung (`common/src/shared/java/com/simplebuilding/fletching/ArrowRecovery.java`, Schalter `server.arrows`) und liefert die Diamantrute als optionalen Schaft.
- **A12:** Nutzen: abwechslungsreichere Pfeile für Bogen- und Armbrustschützen (Spitze, Schaft und Befiederung kombinierbar, auf Wunsch mit Trank), der Lichtpfeil als Beleuchtung, der Silberpfeil gegen Untote. Ändert: Rechtsklick auf den Vanilla-Befiederungstisch öffnet das neue Menü (ein Mixin an `BlockBehaviour.useWithoutItem`, nur für diesen Block; in Vanilla hat er keine Bedienung); der Lichtpfeil setzt Vanillas unsichtbaren Lichtblock (Stufe 15) vor die getroffene Fläche (abschaltbar mit `lightArrow`); der Silberpfeil macht gegen Untote Zusatzschaden (`silverArrowUndeadBonus`, 0 = aus); gefertigte Pfeile sind neue Items und Entitäten. Vanilla-Pfeile, Bögen, Armbrüste und der Befiederungstisch-Block bleiben unverändert. Eingriff in Vanilla: gering.

### Simple Ores (`simpleores`)
Silbererz (Oberwelt, End) und Roségold (zwei Legierungen, Pressling, nur Hochofen) mit Werkzeugen und Rüstung; allein mit Vanilla spielbar.

- IG31-13 (31.10.): Simple Ores besitzt Enderit, Dimensions-Schrott und Weisheitserz (Pflicht-Sub-Mod von SB). Enderit-Varianten anderer Module bleiben dort registriert, ihre Rezepte laden nur mit `fabric:all_mods_loaded ["simpleores"]`, Kreativ-Tabs blenden sie ohne Ores aus (`CreativeTabPolicy`). Plan: `docs/ai/PLAN-B-IG31-13-2026-10-31.md`.
- **A12 je Teil** steht in `docs/ai/PLAN-A-ORES-2026-10-30.md`; Silber ist **kein** Werkzeugmaterial, Roségold-Hämmer gibt es nur in Simple Hammers und nur mit Simple Ores (optionale Stufe, kein `requires`).
- Daten (Modelle, Loot, Rezepte, Fortschritte, Tags, Worldgen, Lang, Wiki) kommen aus `modules/simpleores/tools/gen_data.py`; alle Texturen sind Vanilla-abgeleitet (`tools/textures/ores_2026_10_30.py`), nie eingecheckt.
- Ausbeute je Pressling ist immer 1 Barren; der Verlust steckt im Rezept (1 Pressling aus 7–9 Barren). Hochofen-`cookingtime` 1200 = 600 Ticks (26.3 halbiert im Hochofen).

### Simple Stats (`simplestats`)
Statistiken je Werkzeug/Waffe/Rüstung mit eigenem Statistik-Bildschirm.

- keine eigenen Regeln bisher

### Simple Blocks (`simpleblocks`)
Fehlende Baublöcke: Hushed Deepslate Tiles, Sculk-Tiefenschiefer und -Fliesen, fehlende Vanilla-Treppen/-Stufen/-Mauern, rissige/bemooste/gemeißelte Varianten, Eis-Bausteine, Naturstufen, Blasse und komprimierte Blöcke, die Formsäge; allein mit Vanilla spielbar.

- **B1 (17.10.):** Simple Blocks enthält alle Baublöcke der Familie, die keinen anderen fachlichen Ort haben (Deko-/Bauvarianten ohne Themen-Sub-Mod). Lager, Maschinen und Werkzeuge bleiben, wo sie sind. Bestand und Verschiebe-Liste: `docs/ai/INVENTAR-BAUBLOECKE-2026-10-17.md`.
- Gibt Simple Blocks einem Block Treppe oder Stufe, gilt Regel O1: der Block bekommt ein Achtel (Eintrag im Octet-Generator von `simpleoctets`; Simple Blocks kennt Simple Octets nicht). Ausgenommen sind nur die Naturstufen Erde, Gras, Sand, Roter Sand und Kies (Besitzer 17.10.).
- Genau **ein** Kreativtab „Simple Blocks“ für alle Baublöcke der Familie (Plan im Baublock-Inventar, Abschnitt „Plan: ein Kreativtab“); Standalone-SimpleBuilding behält seinen eigenen Tab.
- Nur öffentliche Registry-Ids und Tags nach außen; verschobene Blöcke mit IdMoves/Alias, Registrierung nie vom Laden anderer Mods abhängig.
- **A12 (PLAN40 simpleblocks-4, 12.11.):** Nutzen: die Formen und Varianten, die Vanilla-Familien fehlen (Treppe/Stufe/Mauer, rissig/bemoost/gemeißelt, Säulen, Eis-Bausteine, Naturstufen), plus Formsäge – Bauen ohne Lücken in den Paletten; kein Vanilla-Block ändert sein Verhalten. Was am Spielgeschehen hängt und wie stark:
  - *Formsäge* – Rechtsklick mit Axt/Hammer auf eine Vanilla-Steinsäge wertet sie auf (Spieler-Aktion, Claims/Bauerlaubnis geprüft); sie zeigt alle Steinsägen-Rezepte und mehr, die Vanilla-Steinsäge bleibt Vanilla (A4b). Gering.
  - *Verschneien* – nur von Hand (Schneeschicht auf Zaun/Pflanze); Mixin nur für Zaun-Verbindungen zur Schneezelle. Gering, Schalter `snow.snowlogging`.
  - *Schneeauflagen* – fallender Schnee legt eine Schicht auf Zäune, Mauern, Treppen und Stufen (Mixin an `tickPrecipitation`, auch an natürlichen Blöcken; der Block darunter bleibt unverändert). Gering bis mittel, Schalter `snow.snowCaps`.
  - *Frostgras* – würde verschneite Biome flächig in einen Mod-Block wandeln (Mixin am Gras-Zufallstick): stark, daher **Standard aus** (simpleblocks-2), Schalter `snow.frostGrass`.
  - *Blasse Blöcke* – Kürbisse, Melonen und Heuballen altern nur im Blassen Garten (wie Kupfer, auch natürlich gewachsene); Mixin nur für den Golem-Kopf. Gering, Schalter `pale.enabled` + Chance je Kette.
  - *Kollabierte Blöcke* – verlangsamen ihren Träger (nur diese Mod-Blöcke, SlotAlert zeigt die Quelle, A14). Gering, Schalter `collapsed.enabled`.
  - Mod entfernen: Mod-Blöcke (auch Schneezellen, Frostgras, blasse Stufen) fehlen danach in der Welt – im Wiki je Feature beschrieben.

### Simple Octets (`simpleoctets`)
Achtelblöcke (kleine 1/8-Blöcke), gemischte Zellen, Melonen-Achtel, Ecken-Abbau mit dem Vorschlaghammer; geschnitten in der Formsäge von Simple Blocks (ohne Simple Blocks im Steinmetz); Pflicht-Sub-Mod von SimpleBuilding, allein mit Vanilla spielbar.

- **O1 (17.10.):** Jeder Block, der Treppen- oder Stufen-Varianten (stairs/slab) hat, bekommt auch einen Octet-Block (Achtelblock). Gilt für Vanilla-Blöcke, SimpleBuilding-Blöcke und alle Blöcke, denen Simple Blocks Treppe/Stufe gibt. Fremdblöcke nur bedingt bei geladenem Besitzer-Mod (`SimpleOctets.isLoaded`).
- Quelle der Wahrheit ist die Generator-Liste (`tools/textures/octets_2026_10_09.py`, `octet_materials.json`, `OctetMaterials`); der Abdeckungstest `stairs_and_slabs_have_octets` meldet fehlende Familien automatisch (die 23 Lücken aus `docs/ai/INVENTAR-OCTETS-2026-10-17.md` sind geschlossen).
- Bretter gehören dazu (frühere Ausnahme „ohne Bretter“ entfällt). Die Regel gilt einseitig: Octets ohne Treppe-Familie (Obsidian, Metallblöcke, Stämme, Terrakotta) bleiben. Ein Achtel behält die Regeln seines Blocks (Vibrationsdämpfung von Wolle/gedämpften Fliesen, Bleidämmung, Licht, Feuerfestigkeit, Klang).
- Schach-Achtel bleiben in SimpleBuilding (F3); die Zellen-Basisklasse gehört dem Modul.
- **A12 (PLAN40 simpleoctets-4, 12.11.):** Nutzen: feinere Details beim Bauen (halbe Kanten, Simse, Schrägen), ohne Vanilla-Blöcke zu ändern; Achtel entstehen nur aus ihrem Vollblock (1 Block = 8 Achtel, verlustfrei). Eingriffe in Vanilla und ihre Schalter (`config/simpleoctets-server.json`, `/simpleoctets config`, Standard an): (1) Melonenscheibe schleichend auf einen Block setzt ein Melonen-Achtel statt zu essen (`features.melonSlicePlacing`); (2) Ecken-Abbau mit dem Vorschlaghammer hängt per Mixin am Abbau jedes Blocks (`features.cornerBreaking`, aus = Mixin kehrt sofort zurück). Sonst keine Mixins außer Tooltip-Hinweis und Feuer-Accessor. Eingriff: gering bis mittel, beide abschaltbar.
- **Fremd-Mod entfernen:** Achtel aus Blöcken einer anderen Mod gibt es nur, solange sie geladen ist; wird sie entfernt, verschwinden ihre gesetzten Zellen und Items wie ihre übrigen Blöcke, eine gemischte Zelle verliert nur die Achtel des fehlenden Materials (Test `mixed_cell_keeps_readable_materials`). Steht im Wiki (Abschnitt „Achtel aus Blöcken anderer Mods“).

### Simple Crucibles (`simplecrucibles`)
Eisen-, verstärkte, Netherit- und Enderit-Tiegel, verstärkter Kessel, Kupfer- und Seelen-Lava-Eimer, Seelen-Lava, Dorf-Feldküchen; Sub-Mod von Simple Ovens (seit IG31-15, optional gebündelt), allein spielbar. Die Fässer gehören seit WH26-6 Simple Storage und docken nur an einen Tiegel an.

- In-World-Upgrades: Axt allein, Vorschlaghammer mit Simple Hammers (solange die Hämmer an sind). Ids wanderten aus SimpleLib/SimpleBuilding (IdMoves). Der Schalter „Enable Simple Crucibles“ (Config von Simple Ovens) nimmt Tab-Einträge, Handbuch, Dorfküche und Axt-/Hammer-Wege weg; gebaute Tiegel arbeiten weiter.
- **A12 (PLAN40 simplecrucibles-2):** Nutzen: brennstofffreies Garen vieler Stapel zugleich im Mittel- und Spätspiel, ein stabilerer Kessel, Seelen-Lava als seltene Nether-Flüssigkeit. Eingriffe: (1) Seelen-Lava in der Nether-Weltgenerierung (Anteil der Lava-Quellen `soulLava.springChance` 0,5 %, Festungs-Brunnen `soulLava.fortressChance` 10 %, 0 = aus, nur neue Chunks; Mixins `SoulLavaSpringMixin`, `SoulLavaFortressMixin`, Nebel `SoulLavaFogMixin`); (2) der Vanilla-Eisen-Eimer zerbricht an Seelen-Lava – dafür Kupfer-Eimer (nur für die neue Flüssigkeit, Vanilla-Lava/Wasser unverändert); (3) Kupfer-Eimer funktionieren auch am Vanilla-Kessel und altern nur in Schnellleiste/Nebenhand; (4) Dorf-Feldküchen in den Häuser-Pools aller fünf Dorfarten (`villageKitchenWeight`, 0 = aus, nur neue Dörfer); (5) Axt-Bau auf dem leeren Vanilla-Kessel (Mixin `AxeBuildMixin`, nur mit Teil in der Nebenhand). Eingriff in Vanilla: mittel (Weltgenerierung), alles abschaltbar.
- **Balance Tiegel ↔ Simple Ovens (Besitzer F4/F5/N30/N37, begründet):** Tiegel garen ohne Brennstoff alle Plätze zugleich (6/12/18/30 Plätze, Tempo 1/2/4/8) und sind damit stärker als Öfen. Ausgleich: eine feste Hitzequelle unter dem Tiegel ist Pflicht (Mittel ×0,5, Hoch ×0,75, nur Seelen-Lava extrem ×1,0; Schmelzen/Erzschmelzen erst ab hoher Hitze), Bau in der Welt (16 Eisenbarren) und Aufwertung mit Rissdiamanten, Netherit- bzw. Enderit-Klumpen. Öfen bleiben die einfache, überall stellbare Lösung mit Vanilla-Brennstoff je Gegenstand. Keine Zahl ohne Besitzer-Entscheid ändern.

### Simple Building Tools (`simplebuildingtools`)
Bauzauberstäbe, Octant in 17 Farben, Bauplan mit Editor und Bildimport; Pflicht-Sub-Mod von SimpleBuilding.

- **A12:** Nutzen: schnelleres Bauen großer Flächen und Kopieren von Bauten. Ändert Vanilla nur, wenn man das Werkzeug hält; kleine Hooks an Kartentisch (Bauplan-Kopien), Mausrad und Container-Menü. Eingriff in Vanilla: gering.

### Simple Challenges (`simplechallenges`)
Freischalt-Modus (Zellen, Preise) und Skyblock-Weltmodi, nur bei gewählter Challenge.

- **A12:** Nutzen: schwererer Start für Spieler, die Vanilla zu leicht finden. Ohne gewählte Challenge ändert das Modul nichts (kein Zustand, Hooks kehren sofort zurück). Gewählt: Betreten und Blockänderungen außerhalb freier Zellen werden verhindert (Hooks an Entity/Level/ServerLevel). Eingriff in Vanilla: stark, aber nur auf ausdrückliche Wahl bei der Welterstellung.

### Simple Chisels (`simplechisels`)
Sieben Meißel von Stein bis Enderit; Blöcke einen Schritt entlang ihrer Familie, mit Sneak zurück.

- **A12:** Nutzen: Bauvarianten ohne Handwerk und Steinsäge. Keine Mixins, nur Items und Daten-Ketten. Eingriff in Vanilla: keiner.

### Simple Chess (`simplechess`)
Schach aus SimpleBuilding (N47 M7): 156 Figuren in 13 Farben (3D und flach), bis vier je Block auf den Quarz-Schachbrettern, Schachbretter mit Treppe/Stufe, mit Simple Octets die Schach-Achtel. Optional, nur SimpleLib Pflicht; Release-Status PHASE 3 (`releasePhase: 3`, nicht in `bundleOptional`).

- **A12:** Nutzen: Deko und echte Brettspiele auf Bauten (ein 4×4-Feld Schachbretter ist ein Schachbrett). Keine Mixins, eigene Blöcke/Items/Block-Entity und Daten. Eingriff in Vanilla: keiner.
- Fremdes nur bedingt: Paletten-Farben (Astralit/Nihilit/Enderquarz) nur mit ihrer Quelle `ChessColor.PALETTE_SOURCE` (heute SimpleBuilding, bei M4 Simple Ores), Schach-Achtel nur mit Simple Octets (einzige Klassen mit dessen Typen: `ChessOctets`, `CheckerOctetBlock`, `CheckerOctetItem`, Test `ChessOctetTests`); ohne Simple Octets Figuren direkt aus dem Schachbrett (8 je Block).

### Simple Earth (`simpleearth`)
Würfel-Erde in sehr großer Höhe, Vanilla-Kartenfarben aus dem Weltgenerator.

- **A12:** Nutzen: Orbit-Ansicht der eigenen Welt. Unter der Renderhöhe bleibt alles Vanilla; Client-Mixins an Himmel, Wolken, Wetter und Optionen. Eingriff in Vanilla: nur Client-Darstellung in der Höhe, abschaltbar (`enabled`).

### Simple Enchanting (`simpleenchanting`)
Dunkler und Astraler Verzauberer über dem Vanilla-Tisch; Pflicht-Sub-Mod von SimpleBuilding.

- **A12:** Nutzen: höhere Verzauberungsstufen im Spätspiel (Schutz V, Federfall V). Keine Mixins; der Vanilla-Tisch bleibt unverändert. Eingriff in Vanilla: gering (Zusatzblöcke).

### Simple Foundry (`simplefoundry`)
Mehrblock-Gießerei: Gold, Kupfer, Eisen schmelzen und in Gießtabletts ausgießen; braucht Simple Ovens.

- **A12:** Nutzen: Ziegel-Mehrblock als Mid-Game-Ziel, ein Barren je Barren. Keine Mixins; Vanilla-Schmelzen bleibt. Eingriff in Vanilla: keiner.

### Simple Gadgets (`simplegadgets`)
Hand-Werkzeuge: Messgerät, Transposer, Detektor, Attraktor, Resonanzstab.

- **A12:** Nutzen: Mess-, Dreh- und Erzsuch-Hilfen für Bauen und Bergbau. Wirken nur gehalten (je Gadget abschaltbar, z. B. `attractorEnabled`); Client-Mixins für HUD, Blickrichtung und Item-Glanz, Server-Mixins an Rüstungsständer- und Todesdrops. Eingriff in Vanilla: gering bis mittel (Drop-Markierung bei Tod).

### Simple Hammers (`simplehammers`)
Sieben Vorschlaghämmer (3x3 abbauen, umformen, Octets schnitzen, Blöcke aufwerten) und die Verzauberungen Durchbruch, Überschreiben, Radius.

- **A12:** Nutzen: schneller Abbau und Umbau. Mixins an Abbau-Tempo, Amboss, Treppenform, Rezepten und Rendern. Eingriff in Vanilla: mittel (Abbau-Hook), nur mit Hammer in der Hand.
- **Balance (PLAN40 simplehammers-2):** Steinhammer ohne Eisen (Steinblock + `#minecraft:stone_tool_materials`, wie Vanillas Steinwerkzeuge); Amboss mit Vanillas Arbeitskosten (Vorbelastung steigt, ab 40 Stufen zu teuer); Aufwertungs-Leiter mit SimpleBuilding bleibt bei der Familienregel der einfachen Aufwertung (doppeltes Handwerksmaterial, 22; Leiter über Gold wie bei den Vanilla-Werkzeugen), Neubau bleibt billiger, die Aufwertung behält Verzauberungen. Übersteuerung II (Allzweck-Hammer) ist der stärkste Eingriff, nur über die Verzauberung Stufe II, getestet (`override_levels`).

### Simple Layers (`simplelayers`)
Wolkenmeer über y 700 und Backrooms als eigene Dimension mit zehn Ebenen (Eingänge: Keller eines verlassenen Hauses, Riss im gestörten Grundgestein).

- **A12:** Nutzen: zwei zusätzliche Erkundungsorte. Betreten über Höhe bzw. seltene Eingänge; Overworld sonst Vanilla (Mixins nur Kolben und Blockzustand für die neuen Blöcke). Eingriff in Vanilla: mittel (Höhenübergang), Wolken und Backrooms je abschaltbar (`cloudsEnabled`, `backroomsEnabled`).
- Platzhalter-Ebenen (`StubBand`, heute u. a. Archiv, Elektro, Lichtlos, Vorstadt, Weizenfeld) sind versiegelt: Türen und Reisen dorthin bleiben ruhend (`backrooms.lockUnfinishedLevels`, Standard an); eine gebaute Ebene öffnet sich von selbst (PLAN40 simplelayers-2). Simple Layers gehört nicht ins erste Release-Paket (pausiert).

### Simple Lights (`simplelights`)
1-px-Lampen für jede Blockseite, 16 Farben, Redstone-Scheibe, Kupferscheiben.

- **A12:** Nutzen: dezente Beleuchtung beim Bauen. Keine Mixins, nur Blöcke. Eingriff in Vanilla: keiner.

### Simple Machines (`simplemachines`)
Doppelkolben, Signaldämpfer, verstärkte/Netherit-/Enderit-Kolben, schnellere Trichter und Trichterloren, Schmied und Auto-Crafter.

- **A12:** Nutzen: größere Redstone-Maschinen und schnellerer Itemtransport. Mixins an Kolben-Logik (Verschieben, Explosion, Rendern); Vanilla-Kolben und -Trichter bleiben. Eingriff in Vanilla: mittel (Kolben-Hooks), greifen nur bei den neuen Kolben.
- **A12 je Teil (PLAN40 simplemachines-2, 12.11.):**
  - *Auto-Crafter* – der Vanilla-Crafter stellt je Redstone-Puls genau ein Ergebnis her und wirft es aus; der Auto-Crafter arbeitet ohne Takt-Schaltung von selbst (alle 4 Ticks, solange ein Trichter darunter steht, ein Signal hält ihn an), legt Ergebnis und Reste direkt in diesen Trichter und hält mit dem Filter-Knopf je Platz ein Rezept-Item fest (A6). Mehrwert: Dauer-Fertigung ohne Redstone-Uhr und ohne Verlust des Rezepts. Eingriff: keiner, eigener Block, der Vanilla-Crafter bleibt unverändert.
  - *Schmied* – Vanilla hat keinen automatischen Schmiedetisch; er schmiedet auf eine steigende Flanke (frühestens nach 4 Ticks wie der Crafter). Eingriff: keiner.
  - *Verstärkter (klebriger) Kolben* – schiebt 18 statt 12 Blöcke und genau einen unzerstörbaren Block (Grundgestein, Verstärkter Tiefenschiefer), aber nur gegen einen verbrauchten Redstone-Block direkt am Kolben, nie Blöcke mit Block-Entity oder Portale, Endportalrahmen nur per Schalter (aus). Begründung: Vanilla lässt Grundgestein schon über Kolben-Tricks brechen; hier ist es planbar, kostet etwas und bleibt begrenzt. Eingriff: mittel (Mixin an der Schiebe-Prüfung), wirkt nur bei den eigenen Kolben.
  - *Netherit-/Enderit-Kolben* – bauen den Block vor sich je nach Signalstärke ab: Komfort für Endgame-Bauer (Tunnel, Farmen), dafür nutzen sie sich wie Werkzeuge ab (Schalter), werden mit ihrem Klumpen repariert und fragen Claim-Mods vorher über das Abbau-Ereignis (Schalter `pistons.fireBreakEvents`, an). Eingriff: mittel, nur bei diesen Kolben.

### Simple Music (`simplemusic`)
Notenblätter (Textcode, .txt/.nbs), Plattenpresse, hochgeladene Ogg-Platten, Taschen-Jukebox.

- **A12:** Nutzen: eigene Musik im Spiel. Mixins an Jukebox, Sound-Laden und Kartentisch/Bündel-Anzeige; Vanilla-Platten laufen weiter. Eingriff in Vanilla: mittel (Jukebox-Hook), Upload mit Quote und Operator-Freigabe, abschaltbar (`enabled`).
- **Texturen (PLAN40 simplemusic-4):** Die Etiketten eigener Platten (Lied Amethyst, Komposition Papier) sind bewusst zweifarbig: sie sind Färbe-Masken und werden im Spiel je Platte/Farbstoff getönt; feinere Verläufe würden dabei matschig (A8-Ausnahme, Besitzer-Abnahme W2C).

### Simple Pads (`simplepads`)
Startpad, Elytra-Pad, Flugpad, Trankpad, Spawn-Teleporter, Chunk-Lader, stärkere Druckplatten, Spawn-Elytra.

- **A12:** Nutzen: Reisen und Automatisierung. Mixins an Windladung, Erfahrungsleiste/HUD, Spawn-Elytra und Schmiede-Rezept-Wächter. Eingriff in Vanilla: gering bis mittel (Spawn-Elytra-Schaden/Slot-Hooks).

### Simple Storage (`simplestorage`)
Kisten, Truhen- und Shulker-Stufen, Fässer, Loren/Boote, Bündel, Köcher, Rucksäcke, Verzauberungen Schublade/Tiefe Taschen/Trichter.

- **A12:** Nutzen: mehr Platz und Ordnung. Viele Mixins an Truhe, Shulker, Bündel, Bögen und Pfeil-Aufnahme, Strukturtruhen. Eingriff in Vanilla: mittel bis stark (Behälterverhalten); Altbestände wandern per IdMoves.
- **Balance (PLAN40 simplestorage-4, 12.11.):** Stufen kosten je Schritt Material aus dem Endspiel (Rissiger Diamant → Netherit → Enderit, nur mit Hammer bzw. Axt in der Welt, Enderit nur mit Simple Ores), deshalb dürfen sie groß sein: Enderit-Doppeltruhe 108 Plätze × 4 Stapel (= 432 Vanilla-Stapel) entspricht dem Endspiel-Lager anderer Lager-Mods und ersetzt keine Sortierung. Der Vanilla-Kupfergolem beliefert nur Vanilla-Truhen (Stufen-Truhen bewusst nicht, Anbindung nach Release: simplestorage-8). Tragbare Enderit-Shulkerkiste 54 × 4 ist das teuerste Lager-Item (zwei Enderitklumpen je Stufe, End-Stadt-Funde nur mit Simple Ores) – Vanilla kennt Shulker-in-Bündel-Ketten ohnehin. Köcher und Rucksack belegen den Brust-Platz und konkurrieren absichtlich mit Brustpanzer/Elytra (Platz gegen Schutz/Flug; Trinkets/Curios geben eigene Plätze). Fässer stapeln wie die Truhe ihrer Stufe (Netherit ×2, Enderit ×4); das Kupfer-Fass bleibt Vanilla-groß als billige Andock-Stufe der Tiegel.

### Simple Vault (`simplevault`)
Seltene Lücke im Grundgestein, Tresor mit drei Rätseln und Aufstiegsaltar.

- **A12:** Nutzen: seltener Erkundungsfund mit Belohnung. Eigene Dimension; Mixins nur an Kolben und Blockzustand des Tresors. Eingriff in Vanilla: gering, Seltenheit `gapRarity`, abschaltbar (`enabled`).

### Geplante Module (noch nicht in `modules.json`)

- `simplehammers`: Geplant (Stufe 7): Hämmer, Hammer-Verzauberungen, Schnitzen, In-World-Crafting. Eigene Regeln: keine bisher.
- `simplestorage`: Stufe 8a umgesetzt (b-s8-storage): Kisten aller Hölzer, Werkbank mit Lager; 8b (b-s8b-storage): Stufen-Truhen, Fallen-Truhen, Fallen-Kupfertruhen, Stufen-Shulkerkisten; offen Kisten-Boote/-Loren, seltene Funde, 8c Bündel, Rucksäcke, Köcher, Lager-Verzauberungen. Eigene Regeln: Aufwertung in der Welt über SimpleLib `in_world_upgrades` (Axt allein, Hammer mit Simple Hammers); Menüs ohne Loader-Shims (ein Menütyp je Stufe und Form).
- `simplegadgets`: Geplant: Geräte in der Hand (Rotator/Transposer, Messuhr, Detektor, Magnet, Oktant, Attractor). Eigene Regeln: keine bisher.
- `simpleenderite`: Geplant: Enderit-Ausrüstung, Astral/Nihil-Paletten. Eigene Regeln: keine bisher.
