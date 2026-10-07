# Konzept: neue Effekte + Mob „Deceiver“ (2026-10-07, Besitzer)

Status: Konzept. Umsetzung als Teil von „simple mobs“ (Sub-Mod-Konzept). Effekte zuerst nur als Tränke.

## 1. Effekte (zuerst als Tränke, Stufe I/II, normale Brau-Logik)
| Effekt | Wirkung | Technik |
|---|---|---|
| **Zittern (Shivering)** | Fadenkreuz zittert; Stufe II stärker | Client: kleiner zufälliger Versatz des Fadenkreuzes (Stufe I ±1 px, II ±3 px, geglättet) |
| **Trugbild (Mirage)** | friedliche Mobs werden als feindliche Mobs ähnlicher Größe gerendert (Eisengolem → Warden/Witherskelett; Schwein → Skelett/Zombie/Silberfisch …) | Client-Renderer-Tausch; Zuordnung je Entity zufällig, aber stabil (Seed aus UUID), nur Optik |
| **Umgekehrtes Trugbild (Reverse Mirage)** | feindliche Mobs werden als friedliche gerendert | wie oben, umgekehrte Tabelle |
| **Verblasst (Faded)** | alles schwarz-weiß: Welt, Inventare, GUIs in der Welt; NICHT das Esc-Menü | Client-Post-Effekt (Graustufen-Shader) auf Welt + Container-Screens, Pause-/Optionsmenü ausgenommen |

Größenklassen für Mirage: klein (Silberfisch, Huhn, Kaninchen), mittel (Schwein, Schaf, Zombie, Skelett, Spinne), groß (Kuh, Pferd, Eisengolem, Warden, Witherskelett). Tausch nur innerhalb der Klasse.

## 2. Mob „Deceiver“ (Täuscher)
**Erscheinung:** kleinwüchsig (≈ 0,9 Blöcke), Kapuzenmantel, Gesicht im Schatten mit zwei schimmernden Augen; optional Brustplatte.

**Werte:** Leben wie ein Eisengolem (100). Mit Brustplatte +20 % (120), sichtbar am Modell. Lauftempo ≈ Spieler-Sprint; trinkt ab und zu Schnelligkeitstrank. Heiltränke wie eine Hexe, heilt aber langsamer, mit Abklingzeit (z. B. 30 s).

**Aggro:** nicht sofort feindlich. Wird feindlich durch längeres Anstarren (≈ 3 s im Blick), mehrfaches Anschubsen oder einen Schlag. Danach Effekte Mirage bzw. Reverse Mirage auf den Spieler.

**Kampf – er greift nie selbst an, nur über Mobs:**
- **Verwandeln:** nimmt das Aussehen eines beliebigen Mobs an (Schwein, Dorfbewohner, Skelett …). Wählt unabhängig, ob er sich friedlich oder feindlich tarnt.
- **Trugbild-Beschwörung:** erzeugt Fake-Mobs, die sich 1:1 wie echte verhalten, aber keinen Schaden machen und beim ersten Treffer sterben. Je Beschwörung ≈ 15 % Chance pro Mob, dass er echt ist (bei 10 Mobs also ~1–2 echte).
- **Thema je Welle:** entweder friedliche Welle (er tarnt sich passend) oder feindliche Welle; wenige Mob-Kategorien je Welle (max. 2–3 Arten), nur sinnvolle und nicht zu starke Mobs (keine Warden/Wither/Bosse; Creeper selten).
- **Synchron-Teleport:** beim Verwandeln teleportiert er sich exakt im Moment der Beschwörung.
- **Getroffen:** kurz wieder sichtbar in normalem Aussehen, dann zufälliger Teleport wie Chorusfrucht – aber auf gleicher Höhe und in Sichtweite (nicht in Höhlen).
- **Eskalation nach Leben:** 100–66 %: Wellen von 3–5; 66–33 %: 5–8, häufiger echte Mobs (20 %); < 33 %: 8–12, trinkt Heiltrank, Mirage + Reverse Mirage wechseln schneller.

**Kampfmuster (Vorschlag, Zyklus ≈ 12 s):**
1. Tarnen + Teleport + Welle beschwören (2 s Vorwarnung: kurzes Flimmern/Partikel am Zielort).
2. 6–8 s Beobachten: läuft auf Abstand, hält Sichtlinie, flieht bei Nähe.
3. Effekt-Puls: Mirage oder Reverse Mirage auf Spieler im Umkreis (Dauer 10 s, nicht stapelnd).
4. Bei Treffer: Enttarnung + Teleport, Zyklus beginnt früher.

**Gegenspiel (fair halten):**
- Fake-Mobs werfen keinen Schatten und machen keine Schrittgeräusche (aufmerksame Spieler erkennen sie).
- Milch entfernt Mirage-Effekte; Augenblüten-/Fernrohr-Blick enttarnt den Deceiver kurz (Idee).
- Echte Mobs aus Beschwörungen droppen nichts, damit kein Farmen.

**Weitere Ideen:**
- **Spiegelbild:** selten erzeugt er ein Trugbild des Spielers (Doppelgänger), das Bewegungen nachahmt.
- **Falsche Truhe:** tarnt sich als Truhe in Strukturen (Mimic-Anklang), ausgelöst beim Öffnen.
- **Drop:** „Täuscherstoff“ für Tarn-Umhang/Trank der Unsichtbarkeit ohne Partikel; selten Trugbild-Spiegel (Item: zeigt Mirage-Mobs wahr).
- **Spawn:** selten in dunklen Wäldern und Plünderer-Außenposten nachts, einzeln.
- **Sound-Design:** flüsternde Laute, Verwandlung mit gläsernem Klang.
- **Varianten:** Sumpf-Deceiver (tarnt sich als Frösche/Schleime), Nether-Deceiver (Piglins/Hoglins).

## Offene Fragen an den Besitzer
- D1: Name final (Deceiver / Täuscher / Delusioner)?
- D2: Drop-Liste und ob Tarn-Umhang gewünscht.
- D3: Spawn-Orte (Vorschlag oben) und Häufigkeit.
- D4: Soll er auch in Dörfern friedlich auftauchen (als Dorfbewohner getarnt)?
