# Konzept: Neuer Deep-Dark-Mob "Sculk Maw" (N33, 2026-10-11)

Queue N33 "Neuer Deep-Dark-Mob (Konzept zuerst)". Nur Konzept, nichts umgesetzt. Vorschau: `<preview-dir>/concepts/sculk_mob.png` (Bauform isometrisch grob + Mob-Skizze; verbindlich ist der Schichtplan unten). Format wie `KONZEPT-MOBS-2026-10-07.md` (Shellker, Soulfire Blaze).

Besitzer-Wunsch: beschwörbar ähnlich Warden; 4 Sculk-Katalysatoren + geschnitzter Kürbis + 2 Sculk Jaws auf den Schultern + Sculk-Sensor vorne, alles in Reichweite; dann mit Ziegenhorn Geräusche machen -> Verwandlung. Feindlich.

## 1. Name (Vorschlag, Empfehlung zuerst)
1. **Sculk Maw / Sculk-Schlund** (Empfehlung: betont die Kiefern, passt zu "Sculk Jaw", kollidiert nicht mit Vanilla)
2. **Echo Gorger / Echo-Verschlinger**
3. **Hollow Howler / Hohl-Heuler** (betont Horn/Geräusch)

Im Folgenden "Maw".

## 2. Bauform (Schichtplan)
Fest 3x3-Grundriss, 3 Schichten, damit Erkennung deterministisch und billig ist (Muster-Match wie Golem/Wither, Vanilla-`BlockPattern`). "In Reichweite" aus dem Wunsch = die Teile müssen **direkt aneinander liegen** (kein loses Streuen); Spiegelung/Drehung um die Y-Achse erlaubt, "vorne" = Seite des Sensors.

Legende: `C` Sculk-Katalysator, `S` Sculk-Block (Körper), `J` Sculk Jaw, `V` Sculk-Sensor, `K` geschnitzter Kürbis (Gesicht zeigt nach vorne), `.` beliebig/Luft.
Blick von oben, vorne = unten im Bild.

```
Schicht 0 (Boden)     Schicht 1 (Rumpf/Schultern)   Schicht 2 (Kopf)
 .  C  .               .  .  .                        .  .  .
 C  S  C               J  S  J                        .  K  .
 .  C  .               .  V  .                        .  .  .
        (vorne unten)
```
Seitenansicht (von rechts), Höhe 3:
```
 y2   K          Kopf (Kürbis)
 y1  J S V       Schultern J (links/rechts vom S), Sensor V vorne
 y0  C S C       Katalysatoren vorne/hinten + links/rechts (Kreuz)
```
- 4 Katalysatoren = Kreuz um den Boden-Sculk-Block; Rumpf = 2 Sculk-Blöcke übereinander; Sensor sitzt vor dem oberen Rumpfblock, Kiefern links und rechts davon (offene Seite nach außen).
- Kosten: 4 Katalysatoren (nur aus Antiken Städten/Seidenhand) + 2 Jaws + Sensor + 2 Sculk-Blöcke + Kürbis ist bewusst Mid-/Endgame-Preis; Warden wird dafür nicht gebraucht.
- Alternative bei "lockerem" Muster (Frage 2): Teile müssen nur innerhalb 3x3x3 um den Rumpf liegen; schwerer zu erklären, schlechter testbar -> nicht empfohlen.

## 3. Auslöse-Regel
- **Horn:** Ziegenhorn mit Rechtsklick (Vanilla spielt `INSTRUMENT_PLAY`, das der Sculk-Sensor hört; genaue Frequenzzahl vor der Umsetzung im Spielcode gegen die Version prüfen, nicht aus dem Kopf).
- **Ritual:** **3 Rufe innerhalb 20 s**, jeder Ruf muss vom **Sensor des Aufbaus** wirklich empfangen werden (Sensor leuchtet, Hörweite 8; Wolle dazwischen dämpft wie Vanilla -> man muss freie Sicht lassen). Rufe mit **3 verschiedenen Hörnern** (Ponder/Sing/Seek/…) zählen alle; gleiches Horn dreimal zählt auch (Empfehlung, einfach); kein Rufmuster-Rätsel.
- **Ablauf:** Rufe 1+2 = Fortschritt (Sensor pulsiert, Katalysatoren blubbern kurz, `sculk_sensor`-Klicken). Ruf 3 startet die Verwandlung. Läuft das Fenster ab, setzt der Zähler zurück (kein Verlust, keine Strafe).
- **Licht/Nacht:** kein Tag/Nacht-Zwang. Der Kürbis darf nicht selbst leuchten und die Lichtstärke am Rumpf muss **<= 7** sein (Höhlen/Deep Dark/dunkler Keller; Config `maw.maxLight`). Draußen bei Tag funktioniert es also nicht, ohne dass man erst abdunkelt. Keine Dimensionsbeschränkung (Oberwelt; Nether/End: Config, Standard aus, weil Sculk-Ausbreitung dort fremd ist).
- Nur im **Überleben/Abenteuer-Spielerhorn**; Dispenser spielt kein Horn (Vanilla), Redstone kann den Aufbau nicht auslösen (Absicht: Spieler muss anwesend sein).

## 4. Verwandlung (Animation/Partikel/Sounds)
Dauer **5 s** (100 Ticks), Spieler außerhalb 4 Blöcke verlieren nichts, innen kein Schaden (Fluchtzeit).
1. 0-1 s: Sensor + Katalysatoren pulsieren, `SCULK_CHARGE`-Partikel steigen vom Boden, Sound `sculk_catalyst.bloom` rhythmisch, Herzschlag-Sound (`warden.heartbeat`) beschleunigt.
2. 1-3 s: Kiefern öffnen sich, Kürbis bekommt cyan-leuchtende Augen (Gesichtslicht = kurzzeitig 7), Himmel/Boden wirft `SHRIEK`-Ringe (Partikel), Bildschirm-Vibration nur lokal (Sound, kein Kamerazucken).
3. 3-5 s: Blöcke lösen sich in `SCULK_SOUL`-Partikel, Wirbel um die Mitte; am Ende Aufschlag-Sound (`warden.emerge` gekürzt), Maw steht an der Rumpf-Position. Zurück bleibt eine 5x5-Fläche **Sculk** (nur ersetzbare Blöcke: Gras, Erde, Stein; keine Truhen/Betten/Spielerbauten; Config).
- Maw erscheint mit Emerge-Animation (wie Warden-Auftauchen), 2 s unverwundbar + unbeweglich.
- Die Bauteile werden **verbraucht** (nicht zurückgegeben); nichts explodiert, kein Blockabbau um den Aufbau.

## 5. Werte (Vorschlag, Normal; Vergleich)
| | Maw (Vorschlag) | Warden | Wither (Java) | Verstärkter Wither (Konzept 10.10.) |
|---|---|---|---|---|
| Leben | **240** | 500 | 300 | 600 |
| Rüstung | 6 | 0 | 4 | 8 |
| Nahschaden | **14** (Biss, Hard 21) | 30 (Hard 45) | Schädel 5-8 | n/a |
| Fernschaden | Echo-Schrei 8 (Bereich) | Schallwelle 10 | Schädel 5-8 | n/a |
| Tempo | 0,26 (Warden 0,3) | 0,3 | 0,6 Flug | n/a |
| Rückstoßresistenz | 0,6 | 1,0 | 1,0 | n/a |
Einordnung: **stärker als Wither-Skelette, schwächer als Warden** - der Maw ist der "Beschwörungs-Mid-Boss" zwischen Wither und Warden; 240 Leben statt 500, damit man ihn mit Diamant/Netherit-Ausrüstung in Reichweite besiegen kann. Beschwörungspreis (4 Katalysatoren) ist der eigentliche Aufwand.

## 6. Verhalten / KI
- **Blind wie der Warden, hört Vibrationen:** Ziel = lauteste/nächste Vibration im Hörradius 24 (eigener Vibrations-Hörer). **Anschleichen** (Sneak) und **Wolle** verstecken, der Maw jagt aber auch der Geräuschquelle (Ziegenhorn!) nach. Blockieren/Ablenken: Schneebälle, Pfeile auf Blöcke, Ziegenhorn erzeugen Ablenkung (Kite-Mechanik, wie beim Warden).
- **Kein Sonic Boom** (bleibt Warden-Signatur). Stattdessen:
  - **Biss** (Nahkampf): Kiefern klappen auf (Animation), Biss 14 Schaden + Effekt *Dunkelheit* 4 s. **Offene Kiefern = Schwachstelle:** Treffer während der 0,6 s Aufklappphase machen x1,5 Schaden (belohnt Timing, kein Schwert-Spam).
  - **Echo-Schrei** (alle 12 s, wenn Ziel > 6 Blöcke): Bereichsring r=10, 8 Schaden + Rückstoß 1, ruft keine Verstärkung (anders als Shrieker).
- **Sculk-Ausbreitung:** der Maw verbreitet Sculk unter sich beim Gehen (alle 3 s ein Fleck, max. 25 Flecken/Maw; nur ersetzbare Blöcke, nicht auf Spielerbauten, `mobGriefing` aus -> keine Ausbreitung). Verschwindet nicht von selbst.
- **Phasen:**
  1. >50 %: Biss + Echo-Schrei, jagt Vibrationen.
  2. <=50 %: "Hunger": Kiefern bleiben offen (Tempo +15 %), Biss-Reichweite +1; er spuckt alle 15 s 3 **Sculk Jaws** (SB: echte Blöcke, 20 s Lebensdauer, aus dem Boden; ohne SB: *Sculk-Venen* + Wurzelfalle ohne Block-Item, siehe Abschnitt 9) als Falle um das Ziel.
  3. <=20 %: "Wut": Echo-Schrei alle 6 s, Rückstoßresistenz 1,0, ruft alle 25 s 2 Sculk-Stachelwürmer? -> **nicht empfohlen**; Phase 3 bleibt Tempo +, kein Beschwören (Balance, Frage 4).
- Despawn: persistent, solange er ein Ziel hat; ohne Ziel nach 10 min ohne Spieler im Umkreis 64 (Config).
- Feind von **allen Spielern**, greift Sculk-Wesen nicht an. Wird **nicht** vom Warden/Wither angegriffen (Fraktion "Deep Dark").

## 7. Drops (Balance, Bezug zu Mod-Items)
- **Sculk Jaw x1-2** (+1 pro Looting-Stufe, max. 4): Kiefer von den Schultern. Nur SB-Inhalt; ohne SB siehe Abschnitt 9.
- **Echo-Splitter x3-5** (Vanilla): Rohstoff für Wiederherstellungskompass/Verzauberung wie bisher.
- **Echo-Kern x1** (neu, garantiert, 1 pro Kill, kein Looting): Brauzutat -> **Trank der Stille** (Seltsamer Trank + Echo-Kern; 3:00 / verlängert 8:00): Vibrationen des Trägers werden unterdrückt (Sculk, Warden hört nicht - Effekt "Schleicher" ähnlich Wolle, aber kein Anschleichen nötig); Warden-Nahkampf bleibt möglich. Reiner Simple-Mobs-Inhalt, kein SB nötig.
- XP 50 (Warden 5, Wither 50). Keine Beute-Farm: nur beschworen, nie natürlich.
- Keine Netherit-/Elytra-Kopplung: der Maw darf kein Wither-/Warden-Ersatz sein; die Drops sind Nebengewerk, kein Pflicht-Item.

## 8. Sicherheit / Grief
- **Gamerule `mobGriefing=false`:** Sculk-Spur und Verwandlungs-Sculk entfallen; Maw läuft nur.
- **Config (`simplemobs`)**: `maw.enabled` (Rezeptmuster/Ritual an/aus), `maw.maxLight=7`, `maw.sculkTrail=true`, `maw.sculkTrailMax=25`, `maw.transformSculk=true`, `maw.allowDimensions=[overworld]`, `maw.hunger=true`.
- Spieler-Bauten sind tabu: Ausbreitung nur auf Block-Tag `#simplemobs:maw_replaceable` (Gras, Erde, Stein, Tiefenschiefer, Sand).
- Frieden: Maw despawnt sofort; Muster löst auf Peaceful nicht aus (Meldung "zu friedlich" in der Aktionsleiste).
- Server: Beschwörung nur wenn Bereich geladen und Spieler in <= 16 Blöcken. Keine Explosionsschäden, keine Lootexplosion.
- Gametest später: Muster-Match (4 Drehungen), Fenster 20 s/Zähler, Licht-Grenze, `mobGriefing=false`, Drops, Phasenwechsel (2 Kills in Folge).

## 9. Modul-Einordnung und Unabhängigkeit
- **Modul: Simple Mobs** (`modules/simplemobs`, eigenes Standalone-Target); Mob, Ritual, Verwandlung, Drops (Echo-Kern, Trank) gehören dorthin. Kein Neu-Modul, kein SB-Kern-Code (SB bleibt Stilreferenz).
- **`simplebuilding:sculk_jaw` ist SB** (`ModBlocks.SCULK_JAW`, nur wenn `McVersion.NATURE_VARIANTS`, d. h. nicht auf allen Linien; lautlos, Biss 3 Schaden, Warden-immun). Regel 1 aus `PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`: Simple Mobs darf nicht von SB abhängen.
- **Lösung (Tag, wie die Hammer-Werkzeug-Tags):** Schultern akzeptieren den Block-Tag **`#simplemobs:maw_shoulders`**. Inhalt: `minecraft:sculk_shrieker` (immer) + `simplebuilding:sculk_jaw` (`required:false`, nur mit SB). Ohne SB trägt der Maw also **Sculk-Shrieker auf den Schultern** (thematisch passend, Vanilla-Block, ebenfalls Ancient-City-Loot); mit SB sind **Jaws und Shrieker austauschbar** (Besitzer-Wunsch Jaws erfüllt, Modpack-Autoren dürfen Tag erweitern).
- Drop "Sculk Jaw" nur mit SB (Beutetabelle `simplemobs:entities/sculk_maw` mit bedingtem Eintrag `neoforge:/fabric:load_conditions` bzw. Tag-Bedingung); ohne SB statt dessen Echo-Splitter +1.
- Phase-2-Falle: mit SB echte Jaws, ohne SB 3 Sculk-Venen-Flächen (Verlangsamung 2 s) - eine Codepfad-Weiche über `Mods.isLoaded("simplebuilding")`/Registry-Lookup, kein Importieren von `com.simplebuilding.*` (Regel 2).
- 26.2/1.21.11 (ohne `NATURE_VARIANTS`): Shrieker-Pfad reicht, Port-Run unproblematisch.
- Wiki-Eintrag `simplemobs/wiki/manual.json`: Zeile "mit SimpleBuilding: Sculk Jaw als Schulter" (Vorbild `simpleriding`).

## 10. Technik / Risiken
- Muster-Match: Vanilla `BlockPattern` mit Predicates (Tag + Property), 4 Drehungen; Auslöser im Horn-Item-Mixin ODER Game-Event-Listener am Sensor (Loader-neutral: `GameEvent`-Callback im `SculkSensorBlockEntity`-Mixin, Loader-Klebeschicht im Modul). Risiko: Horn-Event-Frequenz je Version - Testlauf auf Fabric/NeoForge/Forge.
- Verwandlung = Ticking-"Beschwörungs-Entity" (unsichtbarer Marker) statt Block-Tick-Kette, damit Neustart-fest und abbrechbar (Spieler zerstört Block -> Abbruch + Fehlschlag-Partikel).
- Geometrie: Tiermodell aus ~12 Quadern (Torso, Kopf=Kürbis, 2 Kiefern je 2 Teile, Arme, Beine), 128x128-Textur; Animation Biss (Kiefer rotieren), Emerge (wie Warden).
- Pixelart: Vorschau zeigt Stil (Dunkelsculk-Teal, leuchtende Cyan-Adern, Kürbis behält Orange); Gesichtstextur des geschnitzten Kürbis als eigenes Motiv.

## Offene Besitzer-Fragen (Empfehlung zuerst, max. 5)
1. Name: **Sculk Maw / Sculk-Schlund** (Empfehlung), Echo Gorger oder Hollow Howler?
2. Bauform: festes 3x3-Muster mit direkt anliegenden Teilen (Empfehlung) oder "alles innerhalb 3 Blöcken" ohne feste Form?
3. Ritual: 3 Rufe in 20 s mit beliebigem Horn, Sensor muss hören (Empfehlung) oder 3 verschiedene Hörner/festes Horn (z. B. nur "Seek")?
4. Stärke: 240 Leben, 2 Phasen+Wut ohne Beschwören (Empfehlung) oder gleich Warden-Niveau (500 Leben, Sonic-Boom-Ersatz)?
5. Ohne SimpleBuilding: Shrieker statt Jaws auf den Schultern (Empfehlung) oder Mob erst gar nicht ohne SB?
