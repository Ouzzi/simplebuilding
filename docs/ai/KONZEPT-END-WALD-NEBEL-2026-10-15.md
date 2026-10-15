# Konzept: Wald-Nebel von außen (Nachtrag 66, End-Biome) – NUR Konzept, nichts gebaut

Wunsch (Besitzer 15.10.): Von außerhalb soll man nicht in den Wald (Ash Valley / Dawnlands) hineinsehen können –
dicht und/oder neblig. Der Nebel *im* Wald ist mit diesem Job entschärft (Kern 8–56, Saum 24–112 Blöcke).

## Ist-Zustand und Kernproblem
Nebel ist in Vanilla ein Biom-Attribut (`minecraft:visual/fog_start_distance`, `fog_end_distance`, `fog_color`). Er
gilt aber **für das Biom, in dem die Kamera steht**, nicht für das Biom, auf das man schaut. Von außen sieht man den
Wald daher klar (ohne Dunst), nur die Blöcke selbst sind dunkel. Reine Daten lösen „von außen unsichtbar“ nicht;
die Biom-Übergänge blenden Nebelwerte nur über wenige Blöcke (Biome-Blend), nicht über Hunderte.

## Variante A – Dichter Rand aus Vegetation (nur Weltgeneration, Vanilla-Mechanik)
Im Saum-Biom (`*_fringe`) die Features verdichten: mehr Stick-Trees und Giant-Stick-Trees (heute `rarity:2` / `rarity:8`,
Kern `count:1:1` / `rarity:3`), zusätzlich ein „Hecken-Feature“ (hohe Blattwände aus Ash-/Duskwood-Blättern, 4–7 Blöcke
hoch, dicht gepflanzt am Rand). Blätter sind blickdicht (Vanilla: Culling aus, aber lichtschluckend).
- Plus: keine Client-Arbeit, funktioniert auf allen Loadern, auch ohne Mod-Client; passt zur Optik („Wald hat Wand“).
- Minus: nie vollständig blickdicht (Lücken, Sicht von oben beim Fliegen/Elytra); Spawn-/Pfad-Änderung der Features
  kostet Weltgen-Zeit; nur neue Chunks; Chunk-Ränder können sichtbar werden.

## Variante B – Client-Nebelvolumen (kleiner Mixin/Event auf die Nebel-Berechnung)
Der Client fragt je Frame ein paar Punkte entlang der Blickrichtung (z. B. 32/64/96/128 Blöcke) im Biome-Cache der
geladenen Chunks ab. Liegt dort Kern- oder Saumbiom (`simpleend:watcher_core`), wird `fog_end` Richtung Kern-Wert
gezogen (weich über ca. 1 s interpoliert). Der Wald wirkt von außen wie eine Nebelwand, die erst beim Betreten
durchsichtiger wird. Nutzt ausschließlich vorhandene Biome-Daten; Config (Client, an/aus, Stärke).
- Plus: wirkt wirklich „von außen undurchsichtig“, auch bei Sicht von oben; stufenlos einstellbar.
- Minus: Client-Code je Loader (Fabric/NeoForge/Forge: Nebel-Hook verschieden; 26.3 hat ein neues Fog-System), Risiko
  mit Shadern/Sodium; Welt ohne Mod-Client (Server-only) sieht nichts; Wand ist ein Näherungswert, kein physischer Nebel.

## Variante C – Ambient-Partikel/„Dunstwand“ am Rand
Am Saum zusätzlich Partikel (Asche, Sporen) mit höherer Dichte; optional ein dünner Kranz aus Dunstquellen über
Bodenhöhe (Server-Ticker setzt Partikel in 16–24 Blöcken Umkreis um Spieler am Biomrand).
- Plus: stimmungsvoll, rein Vanilla-Partikel, serverseitig; kein Fog-Code.
- Minus: Partikel erscheinen nur im Nahbereich (Vanilla-Ambientpartikel ~ 10–15 Blöcke), also keine Sichtsperre auf
  Distanz; Tick-Kosten; nur Begleiteffekt.

## Empfehlung
**A als Basis (Phase 1), B als gezielte Ergänzung (Phase 2, nur wenn A nach Spieltest nicht reicht).** A ist günstig,
loaderneutral und verändert das Spielgefühl sichtbar („wir gehen in einen dichten Wald“). B erst nach Prüfung des
26.3-Fog-Systems; C nur als Zierde auf den Saum. Entscheidungen vor Umsetzung: Wie dicht darf der Rand sein
(Elytra-Sicht von oben erlaubt?), Client-Config ja/nein, und ob bestehende Welten (nur neue Chunks) reichen.
