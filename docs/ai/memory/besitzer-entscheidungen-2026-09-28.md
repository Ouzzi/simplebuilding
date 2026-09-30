---
name: besitzer-entscheidungen-2026-09-28
description: "Antworten des Besitzers auf die 69-Punkte-Liste (Welle 23) - Config-Philosophie, Hammer-Balance, Trims, Kerne, Handel"
metadata:
  node_type: memory
  type: project
  originSessionId: c63559d3-2c6a-43ab-9c5e-92fd18904742
  modified: 2026-09-28T21:02:07.432Z
---

Antworten vom 2026-09-28 (Welle 23):
- **Config-Philosophie:** alle Gameplay-Stellschrauben serverseitig autoritativ (Client hat kein Mitspracherecht), in einem eigenen fortgeschrittenen Config-Tab fuer Server-/Modpack-Ersteller; Geschwindigkeiten/Reichweiten immer mit Obergrenze, die Vanilla nicht gefaehrdet.
- **Chunk-Loader:** keine Obergrenze pro Spieler; nur aktiv solange der Besitzer online ist; Admin-Befehl zum Auflisten; beim Setzen pruefen.
- **Kerne/Handel:** Steinmetz verkauft keine Kerne mehr. Fahrender Haendler: Kupferkern selten, dazu Eisen/Gold/Diamant teuer mit absteigender Seltenheit (nur Glueckstreffer im Early Game). Zweite Eisenkern-Truhenquelle ~0,5 %. Truhenchancen nach 'Zeitalter B': mittlere gezielte Suchzeit ~85 % der Zeitalter-Spielzeit (Eisen 8 h, Gold 15 h, Diamant 25 h, Netherit 30 h, Enderit 45 h).
- **Curios/Trinkets** fuer Rucksack/Koecher: ja, als optionale Abhaengigkeit (eigener Run nach Q).
- **Rotator:** kein Mending, Unbreaking ja; geht nie kaputt, ist nur entladen, mit Material aufladbar.
- **Englisch:** amerikanisch. Cover-Buecher bleiben im Loot. Zeilen-Layout fuer alle Tabs.
- **Hammer:** Pickaxe bleibt Primaerwerkzeug. 1x1 etwas langsamer als gleiche Pickaxe; Flaechenabbau pro Block so schnell wie die Pickaxe eine Stufe darunter; nutzt sich schneller ab. Oktant-Auswahl abbauen: Schaden = Summe der Bloecke, doppelte Zeit pro Block, Bruchanimation. Code-Stand hat Vorrang, nur annaehern.
- **Trims:** Mod-Vorlagen ohne "Smithing" im Namen (Glowing/Emitting/Pulsating Armor Trim, analog Vanilla). Neu: Pulsating Armor Trim (Warden-Motiv, Echoscherbe; ohne Glowing pulsiert der Trim Farbe<->Schwarz). Spaeter: Vorlagen teurer machen. Leuchtregeln (2026-09-29): Glowing II pulsiert nicht; Helligkeit 1-15 schwankt nur bei Pulsating+Glowing; Pulsating allein laesst die Saettigung pulsieren (kein Licht, nicht schwarz). Glowing I = volle Helligkeit, heisst nur "Glowing" (2026-09-29).
- **Luftsprung:** Server setzt die Abklingzeit immer durch, Lag-Toleranz hoechstens 1 s. **Geschichtetes Rohenderit:** Name bleibt, 3 Rohenderit pro Schrott.
- **Hammer-Haltbarkeit** bleibt, bis der Besitzer selbst testet und einen Balancing-Auftrag gibt. Reittier-Ruestungen abgenommen.
- **Magnet** heisst jetzt Attractor; platzierbar wie Vorlagen, platziert zieht er Items an.
- **Abgelehnt:** Baustab-Ideen (Ersetzen/Kopieren/Symmetrie), Tasten fuer Magnet/Erzdetektor/Baustab-Undo (nur HUD-Taste). Punkte 64-69 vorgemerkt fuer spaeter.

**Why:** Grundlage fuer alle Runs der Welle 23; Details in .claude/QUEUE.md.
**How to apply:** Bei neuen Optionen/Features diese Linie halten; siehe [[feature-paket-2026-09-24]].
