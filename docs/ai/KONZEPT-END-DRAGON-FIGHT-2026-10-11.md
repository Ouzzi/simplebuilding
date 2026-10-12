# Konzept: Simple End Dragon Fight (Entwurf, 2026-10-11)

Quelle: `.claude/QUEUE.md` Nachtrag 40. Nur Konzept, kein Code. Namen/Methoden der MC-26.3-Klassen (EnderDragon, EndDragonFight, Phasen) sind hier aus Vanilla-Wissen genannt und müssen bei der Umsetzung gegen die Mappings geprüft werden.

## 1. Ziel und Einordnung

Neues Sub-Modul **Simple End Dragon Fight** (`modules/simpleenddragonfight`, Mod-ID/Namensraum `simpleenddragonfight`). Regeln nach `PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`: allein spielbar mit Vanilla + Fabric API/Cloth; **keine** harte Abhängigkeit (`requires: []`), keine Klassenimporte aus SimpleBuilding. SimpleBuilding bündelt das Modul wie Simple Trims und spiegelt den Schalter „Enable Simple End Dragon Fight“ / „Simple End Dragon Fight aktivieren“ (Sub-/Super-Mod-Konzept). Es ist ein reines Kampf-Modul: Spielerskalierung, schnellerer Drache, Eintritts-Animation, Dragonmites. Kein Partner nötig; der Brunnen der End-Struktur (SB) wird nicht gebraucht, es geht um das Vanilla-Ausgangsportal (Bedrock-Brunnen) der Hauptinsel.

## 2. Mechaniken

### 2.1 Spielerskalierung (Kern)
- **Besitzer-Wortlaut „+1 Leben pro Spieler“:** Vanilla-Drache hat 200 HP (100 Herzen). +1 HP bzw. +1 Herz (2 HP) je Spieler wäre nicht spürbar (201–208 HP). Empfehlung: Wert **prozentual** statt absolut (Abschnitt 6); „+1 Herz“ bleibt als Config-Modus `flat` möglich (`hpPerPlayer` in Herzen).
- **Zählen:** `n` = Spieler im Inselbereich (2.5) im Modus Survival/Adventure, am Leben; Kreativ/Zuschauer nur wenn `countCreative=true`. Mindestens 1 (Solo = Vanilla-HP). `n_eff = min(n, maxPlayers)`, Deckel Standard 8.
- **Anwenden:** Basis-Maximalleben bleibt das Vanilla-Attribut (200); das Modul legt einen transienten Attribut-Modifier `simpleenddragonfight:player_scale` (Multiplikator, nie gespeichert, bei jedem Tick-Abgleich neu gesetzt) auf `max_health`. Die Boss-Leiste stimmt dadurch von selbst.
- **Anpassung bei Beitritt:** neues `n` erkannt → Max-Leben steigt, aktuelles Leben wird **im selben Verhältnis** mitgezogen (Anteil bleibt gleich, kein Geschenk-Heilen, kein Rückschritt im Fortschritt). Chat-/Aktionsleisten-Hinweis an alle im Bereich: „<Name> betritt den Kampf (Drache: 250 HP)“.
- **Verlassen/Tod/Disconnect:** Spieler zählt nach einer **Schonfrist** (`leaveGraceSeconds`, Standard 60) weiter mit, damit Tod + Respawn-Rückkehr, kurze Verbindungsabbrüche oder Rein-Raus-Tricks das Leben nicht pumpen oder drücken. Danach `n` sinkt; Max-Leben sinkt, Anteil bleibt (aktuelles Leben wird nie unter 1 gesetzt, der Drache stirbt nie durch das Verkleinern). Abwärts also nur verzögert, aufwärts sofort (Hysterese gegen Join-Spam-Flackern).
- **Fairness:** Wer erst spät kommt, zählt voll ab sofort (kein Rückwirken). Kein Skalieren unter Solo-Wert. Die Halb-Leben-Schwelle (2.4) hängt am skalierten Maximum, springt also bei Beitritt nicht rückwärts über Schwelle: Wellen, die einmal begonnen haben, laufen weiter, auch wenn ein Beitritt den Anteil kurz über 50 % hebt (Zustand `halfReached` ist pro Kampf einrastend).
- **Neuer Kampf** (Drache respawnt per Kristallen): Zustand (Warteschlange, `halfReached`, Teilnehmer) wird zurückgesetzt.

### 2.2 Schnellerer Drache
- Standard **+20 %** Fluggeschwindigkeit und Wendegeschwindigkeit (`speedMultiplier` 1.20), plus kleiner Zuwachs je Mitspieler (Abschnitt 6, Vorschlag D). Umsetzung als Mixin auf den Geschwindigkeits-Wert der Phasen (Halten/Angriff/Strafe/Landeanflug); die Sitz-/Atem-Phase (Perching) behält ihre Dauer, damit der Kampf nicht unfair kurz wird. Drachenatem-Wolken unverändert.
- Nur während des Kampfs mit dem Haupt-Drachen der Hauptinsel (`EndDragonFight` aktiv). Custom-Drachen anderer Mods werden nicht angefasst (Prüfung auf Entity-Typ `minecraft:ender_dragon` und `fight != null`).

### 2.3 Dragonmites
Siehe Abschnitt 3. Auslöser: Drache lebt, Kampf läuft, Leben ≤ 50 % des skalierten Maximums.
- Timer: alle `waveIntervalSeconds` (Standard 30 s = 600 Ticks). Erste Welle sofort beim Erreichen der Schwelle.
- Ort: Kreisring Radius 3–8 um das Ausgangsportal (Mitte der Insel, Brunnen oben auf Bodenhöhe `getHeight(MOTION_BLOCKING)`), nur auf freien, sicheren Positionen; bis zu 8 Versuche je Mite, sonst verfällt sie.
- Anzahl je Welle und Deckel siehe Tabelle Abschnitt 6. Gesamt-Lebend-Deckel (`maxAliveMites`, Standard 12) gegen Überfüllung; überzählige werden nicht nachgeholt.
- Ende: Drache tot, Kampf zurückgesetzt, Dimension leer oder Modul aus → alle Dragonmites lösen sich mit Rauchpartikeln auf (kein Drop, kein XP). Dragonmites spawnen **nur** über diesen Mechanismus (kein natürliches Spawning, keine Eier, kein Spawn-Ei im Kreativtab außer `exposeSpawnEgg`).

### 2.4 Eintritts-Animation (Warteschlange)
Siehe Abschnitt 4. Je neu zählendem Spieler ein Eintrag in einer FIFO-Warteschlange; Abarbeitung strikt nacheinander, kein Zusammenfassen.

### 2.5 Inselbereich
- Dimension `minecraft:the_end`. Mittelpunkt (0, 0) = Ausgangsportal-Spalte (aus `EndDragonFight`-Portalposition, Standard 0,0).
- Radius **`islandRadius` = 160 Blöcke** horizontal (Vanilla-Hauptinsel ≈ 100 Blöcke breit gefüllt; Puffer 60 deckt Brücken-/Randflug, Kristall-Türme am Rand r≈43 und Drachenkreisbahn ab; die äußeren Inseln beginnen ab ~1000, also weit außerhalb). Zusätzlich Höhenband: Y von Weltuntergrenze bis `maxBuildHeight + 64`; im Void unter der Insel zählt man nicht (Y < 0 → nicht im Bereich), damit Fallende nicht „drin“ bleiben.
- Prüfung alle 20 Ticks (1 s) per quadratischer Abstandsvergleich auf der Spielerliste der End-Ebene (kein Chunk-Scan). Eintritt = erster Tick im Bereich; der Spieler bleibt Teilnehmer bis Verlassen + Schonfrist.
- Beitritt über das Portal zählt wie jeder andere Eintritt (Spawn-Plattform bei (100, 49, 0) liegt innerhalb r=160). Spieler, die nur vor dem Kampf auf der Insel sind (kein Drache), lösen **keine** Animation aus; die Teilnehmerliste wird trotzdem geführt, damit der Kampfbeginn `n` korrekt hat.

## 3. Dragonmite

- **Basis:** Endermite-Variante, eigener Entity-Typ `simpleenddragonfight:dragonmite` (Kategorie MONSTER, nicht despawnbar durch Distanz, aber vom Modul gelöscht), eigenes Modell (Endermite-Modell ×1.4 skaliert) und eigene Textur 64×32.
- **Werte (vs. Endermite 8 HP / 2 Schaden / 0,25 Tempo):** 14 HP, 3 Schaden (Nahkampf), Bewegungstempo 0,32, Rüstung 2, Folge-Reichweite 32, Knockback-Resistenz 0,3. Zusatz: 10 % Chance je Treffer, den Spieler kurz (2 s) zu verlangsamen (Slowness I, „klebriger Chitin“), kein Teleport-Zwang. Feuer/Void normal verwundbar. Kein Endermen-Aggro, keine Verfolgung durch Endermen.
- **Verhalten:** läuft zu nächstem Spieler im Inselbereich, bevorzugt wer den Drachen zuletzt getroffen hat; kann Endstein klettern wie Endermite (Spinnen-Navigation nicht nötig). Springt nicht über Abgründe; fällt es ins Void, ist es weg (zählt aus).
- **Optik-Idee:** Körper dunkles Obsidianviolett mit Purpur-Adern, Rücken mit 3 hellen Drachenschuppen/Stacheln (Endstein-Creme), zwei kleine Flügelstummel (statisch, summen mit Ruckeln), Augen leuchtend magenta (emissive Layer). Vorschau gegen die Vanilla-Endermite und den Drachen zeigen, Stil Vanilla-nah (Besitzerregel: eigene Formen statt Recolor – also zusätzliche Stachelreihe und Flügelstummel am Modell, nicht nur Farbwechsel).
- **Drops:** XP 3. Loot: 0–1 Enderperle (Chance 12 %, + Looting), harter Deckel `maxPearlsPerFight` (Standard 6) pro Kampf, damit kein Perlen-Farm entsteht; sonst nichts. Optional später: „Drachen-Chitin“ als Zutat (eigene Item-Frage, hier nicht enthalten).
- **Sounds:** Vanilla-Endermite-Sounds mit Pitch 0,8 (tiefer), Sterbe-Sound mit kurzem Dragon-Hurt-Anteil leiser.
- **Spawn-Ei/Kreativtab:** nur wenn Config `exposeSpawnEgg` an (Standard aus), damit der Kampf nicht umgangen wird.

## 4. Eintritts-Animation

Je Beitritt (nach Schonfrist erneuter Beitritt zählt wieder) wird ein Eintrag `(playerUuid, name, tick)` in die FIFO-Warteschlange gelegt (`maxQueued` Standard 16; darüber werden die ältesten verworfen, im Log vermerkt). Es läuft immer genau eine Animation; nach ihrem Ende 10 Ticks Pause, dann die nächste. Jede dauert **60 Ticks** (3 s). Rein kosmetisch: keine Betäubung, keine Phasenumschaltung, der Kampf läuft normal weiter (Fairness: wer gerade kämpft, wird nicht unterbrochen).

Ablauf pro Trigger (Tick relativ zum Start):
1. **0–10 – Ankündigung:** Am Eintretenden ein Ring aus `REVERSE_PORTAL`/`PORTAL`-Partikeln (Radius 1,5, 24 Stück) + Sound `entity.enderman.teleport` leise; Aktionsleiste an alle: „<Name> betritt den Kampf“.
2. **10–35 – Antwort des Drachen:** Der Drache (wenn nicht gerade sitzend) steigt per kurzem Auftriebs-Impuls (Y-Zielhöhe +12 über aktueller Bahn, Abklingen nach 25 Ticks) auf und dreht den Kopf zum Eintretenden (Kopf-Ziel nur visuell, Flugbahn bleibt Phase). Zwischen Drache und Spieler eine Partikelspur (`DRAGON_BREATH`, 1 pro Block, Bogen) – „er wittert den Neuen“.
3. **35–45 – Brüllen:** Sound `entity.ender_dragon.growl` (Pitch 0,7 + 0,05 × Warteschlangenposition, max. 1,0) laut, Reichweite 128 Blöcke; Partikel-Schockring `DRAGON_BREATH` + `END_ROD` (Radius wächst 0 → 8 Blöcke in 10 Ticks, 64 Partikel) um den Kopf.
4. **45–60 – Abklingen:** `END_ROD`-Funken regnen vom Drachen zum Eintretenden, kleine violette Welle am Boden am Eintretenden (`WITCH`-Partikel), Bossbar-Name pulsiert 1×  (Titel kurz „Ender Dragon ✦ +“, zurück zum Vanilla-Namen).
- Aufbau: ausschließlich serverseitig (Partikel- und Sound-Pakete sind Vanilla); kein Client-Mod nötig → auch auf Servern ohne Client-Installation sichtbar. Die echte Modell-Animation (Kopf/Flügel) ist **Stufe 2** (Datenparameter am Drachen + Client-Renderer-Mixin), nur falls der Besitzer die reine Partikel-/Sound-Fassung zu schwach findet.
- Bei sehr vielen gleichzeitigen Beitritten (8 Spieler per Portal in 2 s) dauert die Schlange 8 × 3,5 s = 28 s; Config `animationTicks` und `maxQueued` mindern das.

## 5. Config (`config/simpleenddragonfight.json`, Cloth-Screen später; zuerst Datei + Tooltips)

| Schlüssel | Standard | Bedeutung |
|---|---|---|
| `enabled` | true | Schalter „Enable Simple End Dragon Fight“ |
| `scalingMode` | `sqrt` | `off`, `flat`, `linear`, `sqrt`, `tiers` (Abschnitt 6) |
| `hpFactorPerPlayer` | 0.6 | Faktor k je Modus (flat: Herzen je Spieler) |
| `maxPlayers` | 8 | Deckel für `n_eff` |
| `countCreative` | false | Kreativ/Zuschauer mitzählen |
| `activityWeighting` | false | Vorschlag E: AFK-Spieler zählen halb |
| `leaveGraceSeconds` | 60 | Schonfrist vor Absenken |
| `islandRadius` | 160 | Inselbereich in Blöcken |
| `speedMultiplier` | 1.20 | Grundtempo des Drachen |
| `speedPerPlayer` | 0.015 | Zusatz je weiterem Spieler |
| `speedCap` | 1.35 | Obergrenze Gesamttempo |
| `damageScalePerPlayer` / `damageCap` | 0.04 / 1.25 | Schaden des Drachen |
| `crystalHealScaling` | true | Kristallheilung mit Max-Leben skalieren |
| `mitesEnabled` | true | Dragonmites an/aus |
| `waveIntervalSeconds` | 30 | Wellenabstand |
| `miteBase` / `miteExtraPerTwoPlayers` | 2 / 1 | Anzahl je Welle |
| `maxAliveMites` | 12 | Lebend-Deckel (+1 je Spieler, max 16) |
| `maxPearlsPerFight` | 6 | Perlen-Deckel |
| `animationEnabled` / `animationTicks` / `maxQueued` | true / 60 / 16 | Eintritts-Animation |
| `exposeSpawnEgg` | false | Dragonmite-Spawn-Ei im Kreativtab |

Alle Werte serverseitig; Standardwerte = Empfehlung D mit sqrt. Schalter + Tooltips EN/DE (Audit M-08: Config nicht nur als Datei).

## 6. Vorschläge zur Verfeinerung der Skalierung

Basis Solo: 200 HP, Schaden 1,0, Tempo ×1,20, Kristallheilung vanilla (1 HP alle 10 Ticks je Kristall, 2 HP/s). Tabellen für n = 1 / 2 / 4 / 8 Spieler.

**A – Linear, +25 % HP je weiteren Spieler.** HP = 200 × (1 + 0,25 (n−1)).
| n | 1 | 2 | 4 | 8 |
|---|---|---|---|---|
| HP | 200 | 250 | 350 | 550 |
Einfach, aber ab 8 Spielern zäh: 550 HP bei 8 Schadensquellen ≈ nur 69 HP pro Kopf, also eigentlich leichter pro Person als Solo. Linear heißt hier Spieler-Anteil 1/n sinkt trotzdem.

**B – Abnehmender Zuwachs (√n), k = 0,6.** HP = 200 × (1 + 0,6 (√n − 1)).
| n | 1 | 2 | 4 | 8 |
|---|---|---|---|---|
| HP | 200 | 250 | 320 | 419 |
| HP/Kopf | 200 | 125 | 80 | 52 |
Mehr Spieler töten schneller, aber nicht trivial; fair, weil Gruppenschaden ohnehin überproportional steigt (Heilung, Koordination, Bogenmassen).

**C – Flach nach Besitzerwort („+1 Herz“, hier +5 Herzen).** HP = 200 + 10 (n−1).
| n | 1 | 2 | 4 | 8 |
|---|---|---|---|---|
| HP | 200 | 210 | 230 | 270 |
Praktisch wirkungslos; nur als Spaß-/Compat-Modus.

**D – Mehrwertig (Empfehlung):** HP nach B, dazu Schaden, Tempo, Heilung, Mites.
| n | 1 | 2 | 4 | 8 |
|---|---|---|---|---|
| Max-HP | 200 | 250 | 320 | 419 |
| Drachenschaden | ×1,00 | ×1,04 | ×1,12 | ×1,25 (Deckel) |
| Tempo | ×1,200 | ×1,215 | ×1,245 | ×1,305 |
| Kristallheilung je Kristall | 2 HP/s | 2,5 | 3,2 | 4,2 (bleibt relativ gleich) |
| Mites je Welle | 2 | 2 | 3 | 5 |
| Mites gleichzeitig max | 9 | 10 | 12 | 16 |
Heilung wächst mit den Max-HP, damit Kristalle nicht relativ unwichtiger werden; Mite-Zahl wächst langsam (+1 je 2 Spieler), damit Gruppen beschäftigt bleiben, ohne den Boden zuzumüllen.

**E – Aktivitätsgewichtung (Zusatz zu D, `activityWeighting`).** AFK-/Zuschauer im Bereich (kein Schaden, kein Block-/Bewegungsereignis seit 120 s) zählen 0,5. Beispiel: 8 anwesend, 4 aktiv: n_eff = 4 + 4 × 0,5 = 6 → HP = 200 × (1 + 0,6 (√6 − 1)) = **374** (statt 419). Verhindert, dass ein Server mit vielen Inaktiven den Drachen unnötig aufbläst und macht „AFK im Portalraum“ als Troll-Weg wirkungslos. Preis: etwas mehr Zustand, Aktivitätstest per Ereignis.

**F – Stufen (Tiers).** n 1: ×1,0; 2–3: ×1,25; 4–5: ×1,5; 6–8: ×1,75.
| n | 1 | 2 | 4 | 8 |
|---|---|---|---|---|
| HP | 200 | 250 | 300 | 350 |
Wenige, vorhersehbare Sprünge; keine Anpassung bei jedem einzelnen Beitritt, aber Sprung beim Schwellen-Beitritt und weniger „+1 pro Spieler“-Gefühl.

**Empfehlung:** **D mit sqrt-Modus** (B als HP-Basis), `maxPlayers` 8, Schonfrist 60 s, **E als Option** (Standard aus, später an, falls AFK-Probleme). A als Modus `linear` für kleine Server, die es einfacher mögen; C/F nur wählbar. Der Besitzerwunsch „+1 Leben je Spieler mit automatischer Anpassung“ bleibt im Kern erhalten (Zuwachs je Spieler, live nachgezogen), aber prozentual und abnehmend, weil absolute +1 bei 200 HP unsichtbar bleibt.

## 7. Modul-Aufbau

`modules/simpleenddragonfight/` nach Vorbild `simplemobs`/`simpletrims`:
- `shared/java/.../DragonScaling.java` (reine Logik ohne MC-Typen: HP-Faktor, Tempo, Mite-Zahlen, Wellenzeitpunkte, Schonfrist/Hysterese – JUnit-testbar), `FightState` (Teilnehmer, `halfReached`, Warteschlange), `DragonFightController` (Server-Tick-Hook, Attribut-Modifier, Spawns, Animation), `DragonmiteEntity`, `EnterAnimation`.
- `fabric/`, `neoforge/`, `forge/` Adapter (Tick-Event für End-Level, Registrierung des Entity-Typs, Mixin für Drachentempo, Config-Laden). Mixin-Ziele: Phase-Fluggeschwindigkeit, ggf. Kristall-Heilung (`EnderDragon.aiStep`).
- `resources/` (Textur, Lang EN/DE, Loot-Tabelle `entities/dragonmite`, Tag `minecraft:...` nur falls nötig), `wiki/manual.json` (Sektion Kampf, Config, „Dragonmite“), `clienttest/` (Dragonmite-Screenshot), `tools/check_data.py`.
- `modules/modules.json` (Eintrag, `requires: []`, Tests-Namespace `simpleenddragonfight`), Standalone-Target `module-simpleenddragonfight-<loader>-263`, SimpleBuilding-Spiegel-Schalter + `enabled-mods.json`.
- Nur MC 26.3 (Fabric zuerst, NeoForge/Forge im selben Zug); 26.2/1.21.11 im Port-Run.

## 8. Tests

- **JUnit (schnell, ohne Welt):** `DragonScaling` – HP-Faktor für n = 1/2/4/8 aller Modi gegen die Tabellen, Deckel, Hysterese (aufwärts sofort, abwärts nach Schonfrist), Anteil-Erhalt bei Max-HP-Wechsel, Mite-Zahl/Deckel, `halfReached` einrastend, Warteschlange (FIFO, Überlauf, Pausenabstand).
- **Gametests (`simpleenddragonfight:*`):** Dragonmite Attribute/Typ; Spawn-Position-Auswahl (Ringfilter, kein Block im Weg); Kontroller mit simuliertem Drachen und Fake-Spielerliste in der Testwelt (Overworld, Drache als Entity ohne echten Kampf): Modifier gesetzt, Anteil gehalten bei n 1→3→1; Wellen bei 30 s-Timer (Tick-Vorlauf); Aufräumen aller Mites bei Kampfende; Config aus → Vanilla-Werte.
- **Standalone-Target** nur dieses Modul + Vanilla (Regel 8). Test „kein SimpleBuilding geladen“ läuft darin.
- **Client-Test:** Screenshot Dragonmite neben Endermite; optional Aufnahme der Animation (Partikel) auf dem Dev-Server.
- **Nicht automatisierbar (Besitzer-Abnahme):** Gefühl Tempo +20 %, Hörbarkeit Brüllen, Lesbarkeit der Warteschlange bei mehreren Spielern.

## 9. Risiken
- Drachenphasen sind eng gekoppelt; Tempo-Mixin kann Perching/Landung brechen → Tempo nur in Flugphasen, Test gegen jede Phase.
- Max-HP-Änderung während Kristallheilung (`getHealth` über Max) → nach jedem Wechsel `health = min(health, max)`.
- Mod-Drachen/Datapacks mit Fremd-Drachen: nur der Vanilla-Typ im aktiven `EndDragonFight`.
- Mehrspieler-Schleifen: alle Prüfungen nur alle 20 Ticks, Spielerliste der End-Ebene, keine Chunk-Scans.

## 10. Offene Fragen (max. 4)
1. **Skalierung:** Empfehlung D/sqrt (200 → 250/320/419 HP) statt wörtlich „+1 Leben“ in Ordnung? Oder gewünscht ein anderer Modus als Standard?
2. **Dragonmite-Drops:** Enderperle 12 % mit Deckel 6 je Kampf ok, oder lieber eigenes Item (Drachen-Chitin) als Zutat?
3. **Animation:** Reicht Stufe 1 (serverseitig Partikel + Sound + kleiner Auftrieb), oder soll gleich echte Modell-Animation (Client-Mixin) eingeplant werden?
4. **Zählregeln:** Inselradius 160 und Schonfrist 60 s passend? Kreativ-Spieler nie zählen (Standard) und AFK-Gewichtung (E) erst später?
