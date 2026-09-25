# Rüstungsbesatz-Boni – Balance-Durchsicht

Stand 2026-09-25. Grundlage: `TrimEffectUtil`, `TrimMultiplierLogic`, `PlayerEntityMixin`,
`LivingEntityMixin`, `SurvivalTracerMixin`, der Tooltip (`ItemMixin`), `TrimReferenceScreen` und
`TrimStatsPanel` in beiden Linien (26.2 und 1.21.11 sind in diesen Dateien identisch). Alle Raten
stehen seit dieser Durchsicht als Konstanten in `TrimEffectUtil`; Tooltip und Nachschlage-Bildschirm
lesen sie über `TrimBonusCatalog`.

Ziel des Besitzers: nicht übermächtig, aber lohnend – Gründe, länger zu überleben, teure Materialien
zu nehmen, oder ein billiges Material für einen Spezialfall zu wählen.

---

## 1. Wie ein Bonus entsteht

`Bonus = Rate je Teil × Teile (Muster: gewichtet) × Resonanz`, danach der Deckel.

- **Muster** zählen gewichtet nach dem Besatzmaterial des Teils: normal 1,0, Netherit 1,75,
  Enderit 2,0 (vorher 3,5).
- **Materialien** zählen je Teil 1.
- **Resonanz** = `trimBenefitBaseMultiplier` (Standard 2,0) × Mittelwert aus Erfahrungs-,
  Überlebens- und Kampffaktor (je 0,1..1,0) → **0,2 bis 2,0**. Mobs und Rüstungsständer: fest 0,2.
- Der Schaden wird am Anfang von `hurtServer` verringert, also **vor** Rüstung und Verzauberungen –
  die Boni wirken multiplikativ zu beidem.

### Resonanz in typischen Lagen

| Lage | Faktoren L / S / C | alt (Produkt, Stufe/100) | neu (Mittel, Stufe/30) |
|---|---|---|---|
| frisch gespawnt | 0,10 / 0,10 / 0,10 | 0,002 | 0,20 |
| Stufe 15, 30 min am Leben, ~25 Kampfpunkte | 0,55 / 0,45 / 0,30 | 0,06 | 0,87 |
| Stufe 30, 3 h am Leben, ~200 Kampfpunkte | 1,00 / 0,96 / 0,88 | 0,62 | 1,89 |
| Obergrenze | 1 / 1 / 1 (alt erst ab Stufe 100) | 2,00 | 2,00 |

---

## 2. Aktuelle Werte (aus dem Code)

Voller Satz = 4 Teile mit normalem Material. Werte nach dem Deckel. „alt“ nur, wo sich etwas
geändert hat.

### Muster

| Muster | Wirkung | je Teil | Satz @1,0 | Satz @2,0 | Deckel |
|---|---|---|---|---|---|
| Sentry | Geschossschaden | 5 % | 20 % | 40 % | Treffer-Boden 80 % |
| Vex | Magie, Vex-Treffer | 6 % | 24 % | 48 % | 80 % |
| Wild | Kaktus, Beeren, Stalagmit | 10 % | 40 % | 80 % | 80 % |
| Dune | Explosionen | 8 % | 32 % | 64 % | 80 % |
| Coast | Ertrinken | 10 % | 40 % | 80 % | 80 % |
| Coast | Luft sparen (Chance) | 20 % | 75 % (80) | 75 % (160) | **75 %** (alt: keiner) |
| Ward | jeder Schaden | 3 % | 12 % | 24 % | **25 % gemeinsam** |
| Silence | Schallwelle des Wardens | 20 % | 80 % | 80 % | 80 % |
| Silence | Sichtbarkeit für Mobs | −15 % | −50 % (−60) | −50 % | **−50 %** (alt: −100 %) |
| Snout | Feuer | 5 % | 20 % | 40 % | 80 % |
| Rib | Witherschaden | 10 % | 40 % | 80 % | 80 % |
| Rib | Wither löschen bis Restdauer | 40 Ticks | 100 (160) | 100 (320) | **100 Ticks** (alt: keiner) |
| Eye | Drachenatem | 10 % | 40 % | 80 % | 80 % |
| Spire | Fallschaden | 8 % | 32 % | 64 % | 80 % |
| Flow | Windkugeln | 10 % | 40 % | 80 % | 80 % |
| Bolt | Blitz | 25 % | 80 % | 80 % | 80 % |
| Bolt | Laufgeschwindigkeit | 5 % | 20 % | 20 % (40) | **+20 %** mit Redstone (alt: keiner) |
| Tide | Schwimmgeschwindigkeit | 10 % | 40 % | 50 % (80) | **+50 %** (alt: keiner) |
| Wayfinder | Sprint-Hunger | −10 % | −40 % | −50 % (−80) | **−50 %** (alt: −100 %) |
| Raiser | Erfahrung | 10 % | 40 % | 50 % (80) | **+50 %** mit Lapis/Quarz |
| Host | Glück | +1,0 | +3 (4) | +3 (8) | **+3** mit Smaragd (alt: keiner) |
| Shaper | – | – | – | – | kein Effekt |

### Materialien

| Material | Wirkung | je Teil | Satz @1,0 | Satz @2,0 | Anmerkung |
|---|---|---|---|---|---|
| Diamant | rüstungswirksamer Schaden | 3 % | 12 % | 24 % | im 25-%-Topf |
| Gold | Magie | 6 % | 24 % | 48 % | |
| Lapislazuli | Magie / Erfahrung | 4 % / 5 % | 16 % / 20 % | 32 % / 40 % | |
| Eisen | Geschosse | 5 % | 20 % | 40 % | |
| Smaragd | Illager / Glück | 8 % / +0,5 | 32 % / +2 | 64 % / +3 (4) | |
| Netherit | Wither-Boss, verzauberungsumgehend | 5 % | 20 % | 40 % | + Muster ×1,75 |
| Quarz | Feuer / Erfahrung | 5 % / 5 % | 20 % / 20 % | 40 % / 40 % | |
| Enderit | **jeder** Schaden | 5 % | 20 % | 25 % (40) | + Muster ×2,0 (alt 3,5), im 25-%-Topf |
| Astralit | rüstungswirksamer Schaden, Sprungkraft | 2 % | 8 %, Sprung I | 16 %, Sprung II | im 25-%-Topf |
| Nihilith | rüstungswirksamer Schaden, Sturzflug | 2 % | 8 % | 16 % | im 25-%-Topf |
| Redstone | Laufgeschwindigkeit | 3 % | 12 % | 20 % (24) | |
| Amethyst | Heilchance alle 10 s | 25 % | 100 % | 100 % | 1 Lebenspunkt / 10 s |
| Kupfer, Harz | – | – | – | – | kein Effekt |

### Deckel (neu, alle nach Resonanz und Teilezahl)

| Deckel | Wert | Vanilla-Anker |
|---|---|---|
| Schaden je Treffer | mindestens 20 % kommen an (alt 10 %) | Schutz-Verzauberung deckelt bei 80 % |
| Boni gegen jeden Schaden (Ward, Diamant, Enderit, Astralit, Nihilith) | zusammen 25 % | – |
| Sichtbarkeit | −50 % | getragener Mob-Kopf |
| Luft sparen | 75 % | Atmung III |
| Sprint-Hunger | −50 % | – |
| Glück | +3 | Glück des Meeres III |
| Erfahrung | +50 % | – |
| Laufen | +20 % | Schnelligkeit I |
| Schwimmen | +50 % | – |
| Wither löschen | 100 Ticks | – |
| Heilchance | 100 % | – |

---

## 3. Gefundene Probleme und was umgesetzt ist

1. **Resonanz verhungerte (behoben).** Das Produkt dreier Faktoren mit je 0,1 Boden ergab frisch 0,002
   und selbst nach Stufe 30 und drei Stunden Überleben nur ~0,6. Die Boni waren fast das ganze Spiel
   unsichtbar. Jetzt: Mittelwert × Basis (0,2..2,0).
2. **Erfahrungsfaktor erst ab Stufe 100 voll (behoben).** Das ist weit jenseits dessen, was Vanilla je
   verlangt, und bestraft Zaubern. Jetzt voll ab Stufe 30.
3. **Client rechnete mit fester Resonanz 0,2 (behoben).** `getGlobalMultiplier` kannte nur
   `ServerPlayer`; der Client (Tooltips, und die vom Client vorhergesagte Laufgeschwindigkeit aus
   Bolt/Redstone) nahm immer 0,2. Jetzt nutzt jeder Spieler `TrimMultiplierLogic`, auf dem Client aus
   den synchronisierten Zählern. Dazu: Forge schickte die Todes-Basiswerte beim Einloggen nicht an den
   Client (Fabric und NeoForge schon) – nachgezogen in `ForgeGameplayEvents.onPlayerLogin`.
4. **Tooltip zeigte erfundene Zahlen (behoben).** Diamant „1,5 %“ (echt 3 %), Sentry „2,5 %“ (5 %),
   Tide „5 %“ (10 %), Kupfer „Lightning Rod“ (kein Effekt), Amethyst „Sonic“ (heilt in Wahrheit),
   Host ohne Zahl; alles fest auf Englisch. Jetzt: graue Kopfzeile „Besatzbonus (Resonanz X×):“ und
   blaue Vanilla-Attributzeilen („+1.2% Schusssicherheit“), übersetzt, aus denselben Konstanten.
   Der Nachschlage-Bildschirm ebenso (Vanilla-Namen der Muster und Materialien, „Kein Bonus“ für
   Kupfer/Harz/Shaper).
5. **Enderit als Muster-Gewicht 3,5 (behoben → 2,0).** Ein Enderit-Satz zählte als 14 Teile: Ward
   allein 84 %, Raiser +280 % Erfahrung, Bolt +140 % Tempo, Host +28 Glück. Mit 2,0 bleibt Enderit
   klar vor Netherit (1,75), ohne jeden Musterbonus an den Deckel zu schieben.
6. **Keine Deckel (behoben).** Silence machte ab Resonanz 1,67 unsichtbar für jeden Mob (mit Netherit-
   oder Enderit-Besatz viel früher), Coast ab 1,25 unendlich atmen, Wayfinder mit Enderit-Besatz ab
   0,71 Sprinten ohne Hunger, Rib löschte bei voller Resonanz jede Wither-Wirkung unter 16 s sofort,
   Glück/Erfahrung/Tempo waren offen. Jetzt die Deckel oben.
7. **Allround-Schutz stapelte bis 90 % (behoben).** Ward + Diamant/Enderit + Astralit/Nihilith lagen
   alle im selben Abzug; Enderit-Ward allein 62 % auf *jeden* Schaden. Jetzt gemeinsam höchstens 25 %,
   und der Treffer-Boden liegt bei 20 %.
8. **Resonanz-Panel: „Dmg Taken … Hearts“ doppelt so hoch (behoben).** Die Statistik zählt
   Zehntel-Lebenspunkte; geteilt wurde durch 10 (= Lebenspunkte), ein Herz sind 20. Das Panel zeigt
   außerdem jetzt „+“ statt „x“ zwischen L, S und C und die Formel im Hover.

Tests: die betroffenen Servertests (`TrimEffectTests`, `TrimBonusTests`, `TrimWiringTests`,
`HopperAndTrimTests`) messen die neuen Werte und jeden Deckel; die Paar-Tests (Muster + Material)
laufen bei Resonanz 0,4 bzw. 0,5, unter jedem Deckel, damit beide Hälften sichtbar bleiben. Der
Coast-Test würfelt jetzt 100 Ticks statt eines garantierten Ticks (75-%-Deckel).

---

## 4. Offene Entscheidungen für den Besitzer (nicht umgesetzt)

1. **Kupfer, Harz und Shaper geben nichts.** Vorschlag: Kupfer +10 % Blitzschutz je Teil (billiges
   Gegenstück zu Bolt), Harz +0,025 Rückstoßresistenz je Teil (Vanilla-Attribut, „klebrig“), Shaper
   +0,25 Block-Reichweite je Teil (Vanilla-Attribut `block_interaction_range` – passt zu einer
   Bau-Mod).
2. **Host erreicht den Glücksdeckel schon bei Resonanz 0,75.** Vorschlag: Host 0,5 je Teil
   (Satz @2,0 = +4 → Deckel +3 erst spät).
3. **Silence erreicht −50 % Sichtbarkeit schon bei Resonanz 0,83.** Vorschlag: 8 % je Teil.
4. **Coast-Luftsparen erreicht 75 % schon bei Resonanz 0,94.** Vorschlag: 10 % je Teil.
5. **Enderit-Material wirkt auch gegen Leere, Hunger, Magie usw.** (kein Schadensart-Filter). Mit dem
   25-%-Topf nicht mehr übermächtig; Vorschlag trotzdem: `BYPASSES_INVULNERABILITY` ausnehmen, damit
   `/kill`-artige Schäden unberührt bleiben.
6. **AFK zählt als Überleben.** Spielzeit läuft auch im Stillstand; drei Stunden AFK = voller
   Überlebensfaktor. Vorschlag: Zeit nur zählen, solange sich die Distanz in der letzten Minute
   geändert hat, oder die Zeitskala verdoppeln.
7. **Ausgegebene Stufen senken die Resonanz.** Wer zaubert, verliert Erfahrungsfaktor. Vorschlag:
   statt der aktuellen Stufe die seit dem Tod gesammelten Erfahrungspunkte (`totalExperience`, sinkt
   beim Zaubern nicht) mit 1395 Punkten (= Stufe 30) als voll.
8. **Stasis (Enderscape) kann Resistenz III geben**, wenn die Basis per Befehl hochgesetzt wird
   (Schwelle 15 = Resonanz 3,75 bei 4 Teilen). Vorschlag: höchstens Resistenz II.
9. **Tide wirkt ohne Tiefenschreiter kaum** (Vanilla verrechnet `getSpeed()` beim Schwimmen nur über
   `WATER_MOVEMENT_EFFICIENCY`). Vorschlag: Tide als Modifikator auf dieses Attribut.
10. **Boni als echte Attribut-Modifikatoren.** Tempo, Glück und Wasserbewegung ließen sich als
    Vanilla-Attribute führen (sichtbar in Mods wie AppleSkin/Jade, sauber für andere Mods). Mehr
    Umbau, reine Stilfrage.
11. **Mobs und Rüstungsständer: feste Resonanz 0,2.** Alternativ 0 (Boni nur für Spieler) oder die
    Resonanz des Besitzers.
12. **Netherit-Gewicht 1,75** passt zum „teuer lohnt sich“-Ziel; ein alter Code-Kommentar nannte 1,5.
    Keine Änderung empfohlen.
13. **Basis 2,0**: mit dem Mittelwert liegt ein durchschnittlich gespielter Spieler bei ~0,9–1,9. Wer
    es stärker auf „Überleben lohnt sich“ trimmen will, senkt den Boden der Faktoren (0,1 → 0) statt
    die Basis zu heben.

---

## 5. Radiance (emittierende Rüstung) – Nachtrag

Nicht Teil der Balance, aber in derselben Runde geändert: Rüstungsständer und Rahmen mit
strahlenden Teilen setzen jetzt auch Licht (`DynamicLightHandler.tickArmorStand`/`tickItemFrame`,
Position über `OwnedLightHolder` mit der Entity gespeichert, Aufräumen in `Entity.setRemoved`), und
getragene/ausgestellte strahlende Teile geben feine Wachs-Glanz-Partikel ab (Chance je Tick
2 % × Strahlkraft, höchstens 12 %). Offen: Mobs mit strahlender Rüstung setzen weiterhin kein Licht.
