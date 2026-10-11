# Konzept: Neuer Deep-Dark-Mob "Hollow Howler" / "Tiefenheuler" (N33, 2026-10-11, Fassung 3: umgesetzt)

## Besitzer-Entscheidungen 11.10. (abschließend, gehen allem unten vor)
- Name: EN **Hollow Howler**, DE **Tiefenheuler**.
- Licht: Block- **und** Tageslicht am Kopf (Schädel) <= 7, sonst kein Start bzw. Abbruch.
- Sensor: **beliebige Position** wie bei Shriekern/Warden-Spawn, auch entfernt – höchstens 5 Blöcke (je Achse) von der Mitte der Konstruktion (mittlerer Katalysator); er muss den Hornruf wirklich hören (Vibration). Damit entfällt die feste Sensor-Position aus Abschnitt 2.
- Bauform: Sculk-Block unten, darauf T aus 3 Sculk-Katalysatoren, Jaw (Tag `#simplemobs:howler_jaw`: Sculk-Shrieker immer, `simplebuilding:sculk_jaw` optional) auf der Mitte, Skelettschädel darauf; Drehung erlaubt.
- Ritual: ein Ruf mit beliebigem Ziegenhorn -> 5 s Manifestation; Abbruch bei abgebautem Sensor oder zu hellem Licht (Teile bleiben, Partikel/Sound/Meldung); Teile erst am Ende verbraucht.
- Stärke: 240 HP, 2 Phasen + Wut, keine Verstärkung; blind, hört Vibrationen, offene Kiefer Schwachstelle; Drops wie Abschnitt 7.

## Umsetzung (Branch claude-howler, Modul simplemobs)
- Code: `modules/simplemobs/shared/java/.../simplemobs/Howler*.java` (Entity, Logic, Pattern, Ritual, Manifestations-Marker), `MobsConfig` (`config/simplemobs-server.json`), `SilenceEffect` + Mixins (`EntitySilenceMixin`: Stille dämpft wie Wolle; `SculkSensorBlockMixin`: Sensor-Aktivierung meldet Frequenz + Quelle an das Ritual), Client `HowlerModel/Renderer/RenderState`.
- Horn-Erkennung: Frequenz von `INSTRUMENT_PLAY` + Quelle ist ein Spieler mit Ziegenhorn-Abklingzeit (Vanilla setzt sie beim Blasen); Spieler <= 16 Blöcke.
- Schwachstelle: x1,5 nur in den 0,6 s, in denen sich die Kiefer zum Biss öffnen (Phase 2 zeigt die Kiefer halb offen, ohne Dauer-Bonus) – Entscheidung Agent, damit Timing belohnt wird.
- Ohne SB: Phase-2-Fallen sind Sculk-Schlingen (Partikel, nach 1 s Langsamkeit II 2 s + 4 Schaden), Drop +1 Echosplitter statt Sculk-Kiefer. Mit SB und mobGriefing: echte Sculk-Kiefer-Blöcke für 20 s (gespeichert, werden auch nach Tod entfernt).
- Echo-Kern + Trank der Stille (3:00/8:00) als Brau-Rezepte `data/simplemobs/recipe/brewing/*` (26.3 datengetrieben).
- Texturen: `tools/textures/howler_textures.py` (eigene Pixel-Art, Palette aus Vanilla-Sculk/Katalysator), Vorschau `~/previews/howler/`.
- Nicht umgesetzt: eigenes Handbuch-Item für Simple Mobs (das Modul hat noch keins; Wiki-Einträge sind da).


Queue N33 "Neuer Deep-Dark-Mob (Konzept zuerst)". Nur Konzept, nichts umgesetzt. Vorschau: `<preview-dir>/concepts/hollow_howler.png` (Schichtplan + Seitenansicht; verbindlich ist der ASCII-Plan unten). Format wie `KONZEPT-MOBS-2026-10-07.md`.

Besitzer-Entscheidungen 11.10.: EN-Name **Hollow Howler**; "Hohl-Heuler" gefällt nicht (neue deutsche Namen unten); Bauform neu (T-Körper statt 3x3x3); ein Ruf mit beliebigem Ziegenhorn genügt, Manifestation ca. 5 s, Abbruch bei abgebautem Sensor oder zu hellem Licht; Stärke bleibt (240 HP, 2 Phasen + Wut, keine Verstärkung).

## 1. Name
EN: **Hollow Howler** (fix). Deutsch, nicht wörtlich, Empfehlung zuerst:
1. **Tiefenheuler** (Empfehlung: Deep Dark + Heuler/Horn, kurz, klingt wie ein echter Mob-Name)
2. **Nachhall** (der Echo-Nachhall in der Tiefe; knapp, düster, passt zum Echo-Kern-Drop)
3. **Grabesschlund** (Kiefer + Grabesstille der Ancient City)
4. **Klagekiefer** (Klage = Heulen, Kiefer = Sculk Jaw; Bezug zur Bauform)
5. **Hallschlund** (Hall/Echo + Schlund; eigene Wortschöpfung)

Im Folgenden "Howler".

## 2. Bauform (Schichtplan)
Fester Aufbau, **3 breit x 4 hoch x 2 tief** (Muster-Match wie Eisengolem, Vanilla-`BlockPattern`, deterministisch und billig). Teile müssen direkt aneinander liegen. Drehung um die Y-Achse erlaubt; "vorne" ist die Seite des Sensors, die T-Achse (Arme) liegt quer dazu. Spiegelung ist wegen Symmetrie egal.

Legende: `S` Sculk-Block, `C` Sculk-Katalysator, `J` Jaw (Tag `#simplemobs:howler_jaw`), `H` Skelettschädel, `V` Sculk-Sensor, `.` Luft/beliebig.
Frontansicht (Blick auf die Vorderseite, Sensor steht davor und verdeckt nur sich selbst):
```
 y3   .  H  .      Kopf: Skelettschädel auf der Jaw
 y2   .  J  .      Maul: Sculk Jaw (Ersatz-Tag, siehe 9)
 y1   C  C  C      T-Körper: Mitte + zwei Arme (wie Eisengolem)
 y0   .  S  .      Fuß: Sculk-Block
```
Seitenansicht (von rechts, vorne = links):
```
 y3    H
 y2    J
 y1    C
 y0  V S           Sensor V direkt vor dem Sculk-Block
```
Draufsicht Schicht y0 (vorne unten): `. V .` / `. S .`.
- **Sensor-Position (fest, Vorschlag):** unmittelbar **vor dem unteren Sculk-Block** (Schicht y0, Vorderseite). Dort liegt er in Reichweite von Katalysatoren (1 Block Abstand) und Kopf (3 Blöcke), ist gut erreichbar zum Abbauen (Abbruchbedingung) und verdeckt nichts. Drehung der ganzen Figur erlaubt.
- Kosten: 3 Katalysatoren (Ancient City / Seidenhand) + Sculk-Block + Sensor + Jaw (oder Shrieker) + Schädel; bewusst Mid-/Endgame, Warden nicht nötig.
- Die Jaw sitzt auf der Mitte des T, der Schädel (Skelett-Schädel, kein Wither-Schädel) darauf; er ist das "Gesicht" des Howlers.

## 3. Auslöse-Regel
- **Ein Ruf genügt:** Rechtsklick mit **beliebigem Ziegenhorn** (Vanilla spielt `INSTRUMENT_PLAY`, das der Sculk-Sensor hört; Frequenz vor der Umsetzung gegen die Version prüfen). Der Sensor **des Aufbaus** muss den Ruf wirklich empfangen (Hörweite 8, Wolle dazwischen dämpft wie Vanilla). Mehrere Rufe, Zähler und Zeitfenster entfallen.
- **Start:** empfängt der Sensor einen Horn-Ruf und das Muster passt, beginnt die Manifestation (Abschnitt 4). Spieler muss anwesend sein (Horn-Spieler <= 16 Blöcke); Dispenser/Redstone lösen nichts aus.
- **Licht (Vorschlag):** Die Manifestation läuft nur im Dunkeln: wirksames Licht am **Kopf (Schädelposition)** <= **7** (`getMaxLocalRawBrightness`, also Blocklicht und Tageslicht zusammen, wie bei Monster-Spawns; Config `howler.maxLight`). Draußen bei Tag funktioniert es nur nach Abdunkeln. Geprüft **beim Ruf und jeden Tick** der Manifestation (nachträglich Fackel setzen bricht ab). Der Schädel selbst leuchtet nicht.
- **Abbruch** (Teile bleiben erhalten, kein Verbrauch, nichts verloren):
  - der **Sculk-Sensor** wird in den 5 s abgebaut/zerstört/ersetzt,
  - das **Licht** am Kopf steigt über die Schwelle,
  - weiteres Muster-Teil verschwindet oder wird ersetzt (Sicherheitsnetz).
  Abbruch-Effekt: `SCULK_SOUL`-/`SMOKE`-Partikel am Aufbau, Sound `sculk_sensor.clicking_stop` plus gedämpftes `warden.sniff`, Aktionsleistenmeldung (z. B. "Der Ruf verhallt", bei Licht "Zu hell"). Danach kann man sofort erneut rufen.
- Keine Dimensionsbeschränkung per Default außer Config `howler.allowDimensions=[overworld]` (Nether/End aus).
- Peaceful: Muster löst nicht aus (Meldung "zu friedlich").

## 4. Manifestation (Animation/Partikel/Sounds)
Dauer **5 s** (100 Ticks); innen kein Schaden (Fluchtzeit). Gesteuert vom Beschwörungs-Marker (Abschnitt 10), der pro Tick Sensor + Licht prüft.
1. 0-1 s: Sensor + Katalysatoren pulsieren, `SCULK_CHARGE`-Partikel steigen auf, `sculk_catalyst.bloom` rhythmisch, `warden.heartbeat` beschleunigt.
2. 1-3 s: Jaw öffnet sich, Schädel bekommt cyan-leuchtende Augen (nur Effekt, kein echtes Licht), `SHRIEK`-Ringe, kein Kamerazucken.
3. 3-5 s: Blöcke lösen sich in `SCULK_SOUL`-Wirbel; Aufschlag-Sound (`warden.emerge` gekürzt), Howler steht an der Körperposition (y1).
- Erscheint mit Emerge-Animation, 2 s unverwundbar + unbeweglich.
- Bauteile werden erst **am Ende** verbraucht (nicht zurückgegeben); vorher Abbruch = alles bleibt stehen. Zurück bleibt eine 5x5-Fläche **Sculk** (nur ersetzbare Blöcke, `#simplemobs:howler_replaceable`; Config). Nichts explodiert.

## 5. Werte (Vorschlag, Normal; Vergleich)
| | Howler (Vorschlag) | Warden | Wither (Java) | Verstärkter Wither (Konzept 10.10.) |
|---|---|---|---|---|
| Leben | **240** | 500 | 300 | 600 |
| Rüstung | 6 | 0 | 4 | 8 |
| Nahschaden | **14** (Biss, Hard 21) | 30 (Hard 45) | Schädel 5-8 | n/a |
| Fernschaden | Echo-Schrei 8 (Bereich) | Schallwelle 10 | Schädel 5-8 | n/a |
| Tempo | 0,26 (Warden 0,3) | 0,3 | 0,6 Flug | n/a |
| Rückstoßresistenz | 0,6 | 1,0 | 1,0 | n/a |
Einordnung: **stärker als Wither-Skelette, schwächer als Warden** - der Howler ist der "Beschwörungs-Mid-Boss" zwischen Wither und Warden; 240 Leben statt 500, damit man ihn mit Diamant/Netherit-Ausrüstung in Reichweite besiegen kann. Beschwörungspreis (3 Katalysatoren + Jaw + Schädel) ist der eigentliche Aufwand.

## 6. Verhalten / KI
- **Blind wie der Warden, hört Vibrationen:** Ziel = lauteste/nächste Vibration im Hörradius 24 (eigener Vibrations-Hörer). **Anschleichen** (Sneak) und **Wolle** verstecken, der Howler jagt aber auch der Geräuschquelle (Ziegenhorn!) nach. Blockieren/Ablenken: Schneebälle, Pfeile auf Blöcke, Ziegenhorn erzeugen Ablenkung (Kite-Mechanik, wie beim Warden).
- **Kein Sonic Boom** (bleibt Warden-Signatur). Stattdessen:
  - **Biss** (Nahkampf): Kiefern klappen auf (Animation), Biss 14 Schaden + Effekt *Dunkelheit* 4 s. **Offene Kiefern = Schwachstelle:** Treffer während der 0,6 s Aufklappphase machen x1,5 Schaden (belohnt Timing, kein Schwert-Spam).
  - **Echo-Schrei** (alle 12 s, wenn Ziel > 6 Blöcke): Bereichsring r=10, 8 Schaden + Rückstoß 1, ruft keine Verstärkung (anders als Shrieker).
- **Sculk-Ausbreitung:** der Howler verbreitet Sculk unter sich beim Gehen (alle 3 s ein Fleck, max. 25 Flecken/Howler; nur ersetzbare Blöcke, nicht auf Spielerbauten, `mobGriefing` aus -> keine Ausbreitung). Verschwindet nicht von selbst.
- **Phasen:**
  1. >50 %: Biss + Echo-Schrei, jagt Vibrationen.
  2. <=50 %: "Hunger": Kiefern bleiben offen (Tempo +15 %), Biss-Reichweite +1; er spuckt alle 15 s 3 **Sculk Jaws** (SB: echte Blöcke, 20 s Lebensdauer, aus dem Boden; ohne SB: *Sculk-Venen* + Wurzelfalle ohne Block-Item, siehe Abschnitt 9) als Falle um das Ziel.
  3. <=20 %: "Wut": Echo-Schrei alle 6 s, Rückstoßresistenz 1,0, Tempo +25 %; ruft **keine** Verstärkung (Besitzer-Entscheidung 11.10.).
- Despawn: persistent, solange er ein Ziel hat; ohne Ziel nach 10 min ohne Spieler im Umkreis 64 (Config).
- Feind von **allen Spielern**, greift Sculk-Wesen nicht an. Wird **nicht** vom Warden/Wither angegriffen (Fraktion "Deep Dark").

## 7. Drops (Balance, Bezug zu Mod-Items)
- **Sculk Jaw x1-2** (+1 pro Looting-Stufe, max. 4): Kiefer vom Maul. Nur SB-Inhalt; ohne SB siehe Abschnitt 9.
- **Echo-Splitter x3-5** (Vanilla): Rohstoff für Wiederherstellungskompass/Verzauberung wie bisher.
- **Echo-Kern x1** (neu, garantiert, 1 pro Kill, kein Looting): Brauzutat -> **Trank der Stille** (Seltsamer Trank + Echo-Kern; 3:00 / verlängert 8:00): Vibrationen des Trägers werden unterdrückt (Sculk, Warden hört nicht - Effekt "Schleicher" ähnlich Wolle, aber kein Anschleichen nötig); Warden-Nahkampf bleibt möglich. Reiner Simple-Mobs-Inhalt, kein SB nötig.
- XP 50 (Warden 5, Wither 50). Keine Beute-Farm: nur beschworen, nie natürlich.
- Keine Netherit-/Elytra-Kopplung: der Howler darf kein Wither-/Warden-Ersatz sein; die Drops sind Nebengewerk, kein Pflicht-Item.

## 8. Sicherheit / Grief
- **Gamerule `mobGriefing=false`:** Sculk-Spur und Verwandlungs-Sculk entfallen; Howler läuft nur.
- **Config (`simplemobs`)**: `howler.enabled` (Rezeptmuster/Ritual an/aus), `howler.maxLight=7`, `howler.sculkTrail=true`, `howler.sculkTrailMax=25`, `howler.transformSculk=true`, `howler.allowDimensions=[overworld]`, `howler.hunger=true`.
- Spieler-Bauten sind tabu: Ausbreitung nur auf Block-Tag `#simplemobs:howler_replaceable` (Gras, Erde, Stein, Tiefenschiefer, Sand).
- Frieden: Howler despawnt sofort; Muster löst auf Peaceful nicht aus (Meldung "zu friedlich" in der Aktionsleiste).
- Server: Beschwörung nur wenn Bereich geladen und Spieler in <= 16 Blöcken. Keine Explosionsschäden, keine Lootexplosion.
- Gametest später: Muster-Match (4 Drehungen, T-Form), Ein-Ruf-Start, Abbruch bei Sensor-Abbau und bei Licht > 7 (Teile bleiben), `mobGriefing=false`, Drops, Phasenwechsel (2 Kills in Folge).

## 9. Modul-Einordnung und Unabhängigkeit
- **Modul: Simple Mobs** (`modules/simplemobs`, eigenes Standalone-Target); Mob, Ritual, Verwandlung, Drops (Echo-Kern, Trank) gehören dorthin. Kein Neu-Modul, kein SB-Kern-Code (SB bleibt Stilreferenz).
- **`simplebuilding:sculk_jaw` ist SB** (`ModBlocks.SCULK_JAW`, nur wenn `McVersion.NATURE_VARIANTS`, d. h. nicht auf allen Linien; lautlos, Biss 3 Schaden, Warden-immun). Regel 1 aus `PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`: Simple Mobs darf nicht von SB abhängen.
- **Lösung (Tag, wie die Hammer-Werkzeug-Tags):** Das Maul (Position y2) akzeptiert den Block-Tag **`#simplemobs:howler_jaw`**. Inhalt: `minecraft:sculk_shrieker` (immer) + `simplebuilding:sculk_jaw` (`required:false`, nur mit SB). Ohne SB trägt der Howler also **Sculk-Shrieker als Maul** (thematisch passend, Vanilla-Block, ebenfalls Ancient-City-Loot); mit SB sind **Jaws und Shrieker austauschbar** (Besitzer-Wunsch Jaws erfüllt, Modpack-Autoren dürfen Tag erweitern).
- Drop "Sculk Jaw" nur mit SB (Beutetabelle `simplemobs:entities/hollow_howler` mit bedingtem Eintrag `neoforge:/fabric:load_conditions` bzw. Tag-Bedingung); ohne SB statt dessen Echo-Splitter +1.
- Phase-2-Falle: mit SB echte Jaws, ohne SB 3 Sculk-Venen-Flächen (Verlangsamung 2 s) - eine Codepfad-Weiche über `Mods.isLoaded("simplebuilding")`/Registry-Lookup, kein Importieren von `com.simplebuilding.*` (Regel 2).
- 26.2/1.21.11 (ohne `NATURE_VARIANTS`): Shrieker-Pfad reicht, Port-Run unproblematisch.
- Wiki-Eintrag `simplemobs/wiki/manual.json`: Zeile "mit SimpleBuilding: Sculk Jaw als Maul" (Vorbild `simpleriding`).

## 10. Technik / Risiken
- Muster-Match: Vanilla `BlockPattern` mit Predicates (Tag + Property), 4 Drehungen (Eisengolem-ähnliches T, 3x4x2); Auslöser im Horn-Item-Mixin ODER Game-Event-Listener am Sensor (Loader-neutral: `GameEvent`-Callback im `SculkSensorBlockEntity`-Mixin, Loader-Klebeschicht im Modul). Risiko: Horn-Event-Frequenz je Version - Testlauf auf Fabric/NeoForge/Forge.
- Manifestation = Ticking-"Beschwörungs-Entity" (unsichtbarer Marker) statt Block-Tick-Kette, damit Neustart-fest und abbrechbar (Sensor-Abbau oder Licht -> Abbruch + Partikel, Teile bleiben).
- Geometrie: Tiermodell aus ~12 Quadern (Torso, Kopf=Skelettschädel, 1 Kiefer aus 2 Teilen, Arme, Beine), 128x128-Textur; Animation Biss (Kiefer rotieren), Emerge (wie Warden).
- Pixelart: Vorschau zeigt Stil (Dunkelsculk-Teal, leuchtende Cyan-Adern, Schädel bleibt hell); Gesichtstextur des Schädels als eigenes Motiv.

## Offene Besitzer-Fragen (max. 3)
1. Deutscher Name: **Tiefenheuler** (Empfehlung), Nachhall, Grabesschlund, Klagekiefer oder Hallschlund?
2. Licht-Schwelle: wirksames Licht (Block + Tag) am Kopf <= 7 (Empfehlung, Tag draußen verboten) oder nur Blocklicht <= 7 (Tageslicht egal)?
3. Sensor-Position: fest vor dem unteren Sculk-Block (Empfehlung) oder an beliebiger der drei Seiten dort (vorne/links/rechts-hinten erlaubt)?

Entschieden (Besitzer 11.10. bzw. Standard): Stärke 240 HP/2 Phasen + Wut ohne Verstärkung; ein Ruf mit beliebigem Horn; Ohne SB Shrieker als Maul-Ersatz (Tag `#simplemobs:howler_jaw`).
