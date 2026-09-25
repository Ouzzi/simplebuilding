# Rüstungsbesatz-Boni – Balance-Durchsicht

Stand 2026-09-25, zweite Runde mit den Entscheidungen des Besitzers (Abschnitt 4). Grundlage: `TrimEffectUtil`, `TrimMultiplierLogic`, `PlayerEntityMixin`,
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
- **Erfahrungsfaktor** aus den seit dem Tod **gesammelten** Punkten (`totalExperience` minus Wert beim
  Tod), voll bei 1395 Punkten (= Stufe 0 bis 30). Ausgeben am Zaubertisch/Amboss kostet nichts.
- **Zeitkurve** des Überlebensfaktors aus **aktiver** Zeit: alle 20 Ticks zählen 20 Ticks, wenn sich die
  zurückgelegte Strecke in der letzten Minute geändert hat. AFK hält die Uhr nach einer Minute an.
- **Tempo, Glück, Schwimmen, Rückstoß, Reichweite** sind Vanilla-Attribut-Modifikatoren
  (`TrimAttributeHandler`, flüchtig, alle 10 Ticks aufgefrischt): movement_speed (Spieler), luck
  (Spieler), block_interaction_range (Spieler), water_movement_efficiency (jeder Träger),
  knockback_resistance (jeder Träger).
- Der Schaden wird am Anfang von `hurtServer` verringert, also **vor** Rüstung und Verzauberungen –
  die Boni wirken multiplikativ zu beidem.

### Resonanz in typischen Lagen

| Lage | Faktoren L / S / C | alt (Produkt, Stufe/100) | neu (Mittel, Punkte/1395) |
|---|---|---|---|
| frisch gespawnt | 0,10 / 0,10 / 0,10 | 0,002 | 0,20 |
| ~700 Punkte gesammelt, 30 min aktiv, ~25 Kampfpunkte | 0,55 / 0,45 / 0,30 | 0,06 | 0,87 |
| 1395+ Punkte, 3 h aktiv, ~200 Kampfpunkte | 1,00 / 0,96 / 0,88 | 0,62 | 1,89 |
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
| Coast | Luft sparen (Chance) | 10 % (war 20) | 40 % | 75 % (80) | **75 %** |
| Ward | jeder Schaden | 3 % | 12 % | 24 % | **25 % gemeinsam** |
| Silence | Schallwelle des Wardens | 20 % | 80 % | 80 % | 80 % |
| Silence | Sichtbarkeit für Mobs | −8 % (war −15) | −32 % | −50 % (−64) | **−50 %** |
| Snout | Feuer | 5 % | 20 % | 40 % | 80 % |
| Rib | Witherschaden | 10 % | 40 % | 80 % | 80 % |
| Rib | Wither löschen bis Restdauer | 40 Ticks | 100 (160) | 100 (320) | **100 Ticks** (alt: keiner) |
| Eye | Drachenatem | 10 % | 40 % | 80 % | 80 % |
| Spire | Fallschaden | 8 % | 32 % | 64 % | 80 % |
| Flow | Windkugeln | 10 % | 40 % | 80 % | 80 % |
| Bolt | Blitz | 25 % | 80 % | 80 % | 80 % |
| Bolt | Laufgeschwindigkeit (Attribut, nur Spieler) | 5 % | 20 % | 20 % (40) | **+20 %** mit Redstone |
| Tide | Wasserbewegungs-Effizienz (Attribut) | +0,10 | +0,4 | +0,5 (0,8) | **+0,5** (Wassertritt I = 0,33) |
| Wayfinder | Sprint-Hunger | −10 % | −40 % | −50 % (−80) | **−50 %** (alt: −100 %) |
| Raiser | Erfahrung | 10 % | 40 % | 50 % (80) | **+50 %** mit Lapis/Quarz |
| Host | Glück (Attribut, nur Spieler) | +0,5 (war 1,0) | +2 | +3 (4) | **+3** mit Smaragd |
| Shaper | Blockreichweite (Attribut, nur Spieler) | +0,25 Blöcke | +1 | +2 | **+2 Blöcke** |

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
| Enderit | jeder Schaden außer `bypasses_invulnerability` (/kill, Leere) | 5 % | 20 % | 25 % (40) | + Muster ×2,0 (alt 3,5), im 25-%-Topf |
| Astralit | rüstungswirksamer Schaden, Sprungkraft | 2 % | 8 %, Sprung I | 16 %, Sprung II | im 25-%-Topf |
| Nihilith | rüstungswirksamer Schaden, Sturzflug | 2 % | 8 % | 16 % | im 25-%-Topf |
| Redstone | Laufgeschwindigkeit | 3 % | 12 % | 20 % (24) | |
| Amethyst | Heilchance alle 10 s | 25 % | 100 % | 100 % | 1 Lebenspunkt / 10 s |
| Kupfer | Blitz | 10 % | 40 % | 80 % | Gegenstück zu Bolt |
| Harz | Rückstoßresistenz (Attribut, jeder Träger) | +0,025 | +0,1 | +0,2 | |

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
| Blockreichweite | +2 Blöcke | – |
| Stasis (Enderscape) | Resistenz II | – |

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

## 4. Entscheidungen des Besitzers (2026-09-25, umgesetzt)

| # | Frage | Entscheidung | Umsetzung |
|---|---|---|---|
| 1 | Kupfer, Harz, Shaper ohne Wirkung | füllen | Kupfer +10 % Blitzschutz, Harz +0,025 Rückstoßresistenz, Shaper +0,25 Blockreichweite (je Teil × Resonanz; Shaper höchstens +2) |
| 2–4 | Host, Silence, Coast erreichen den Deckel zu früh | senken | Host 0,5 Glück, Silence 8 %, Coast 10 % je Teil |
| 5 | Enderit schützt gegen alles | nicht gegen `bypasses_invulnerability` | /kill und Leere gehen durch |
| 6 | AFK zählt als Überleben | nur aktive Zeit | Aktivzähler im `SurvivalTracerMixin` (Strecke in der letzten Minute geändert), gespeichert als `ActiveTicks`; alte Spielstände starten die Uhr bei 0 |
| 7 | Ausgegebene Stufen senken die Resonanz | gesammelte Punkte | `totalExperience` minus `BaseXp` (Wert beim Tod), voll bei 1395; `BaseXp` wird mit `TrimDataPayload` an den Client geschickt |
| 8 | Stasis bis Resistenz III | höchstens II | `stasisAmplifier` |
| 9–10 | Tempo/Glück/Schwimmen als Attribute; Tide ohne Wassertritt | umsetzen | `TrimAttributeHandler`; die getSpeed-/getLuck-Eingriffe sind entfernt; Tide wirkt über water_movement_efficiency |
| 11 | Mobs/Rüstungsständer | fest 0,2 behalten | unverändert |
| 12–13 | Netherit-Gewicht, Basis 2,0 | so lassen | unverändert |
| 14 | Mobs mit Radiance-Rüstung ohne Licht | Licht geben | siehe Abschnitt 5 |
| 15–18 | Oktant-Bildschirm, Entfernungsmesser, Luftsprung-Balken, Netherit-Trichter, Statistik-Panel | an Vanilla angleichen | siehe Bericht; Popup-Hintergrund, Tooltip-Hintergrund, Pferde-Sprungleiste, übersetzte Texte, Knopfposition wie am Schmiedetisch |

Tests: `HopperAndTrimTests` (Erfahrungsfaktor aus Punkten, Basis beim Tod), `TrimEffectTests`
(neue Raten, Harz, Shaper, Stasis-Deckel, Enderit gegen Leere und /kill), `TrimBonusTests` (Kupfer gegen
Blitz), `TrimWiringTests` (Attribut-Modifikatoren samt Tick-Verdrahtung und Schalter, Aktivzeit ohne AFK,
Speicherung von `BaseXp`/`ActiveTicks`, Migration alter Spielstände), `DynamicLightTests` (Mob-Licht).
Jede neue Prüfung ist mit einer absichtlich eingebauten Gegenprobe rot gesehen worden.

## 5. Radiance (emittierende Rüstung) – Nachtrag

Nicht Teil der Balance, aber in derselben Runde geändert: Rüstungsständer und Rahmen mit
strahlenden Teilen setzen jetzt auch Licht (`DynamicLightHandler.tickWearer`/`tickItemFrame`,
Position über `OwnedLightHolder` mit der Entity gespeichert, Aufräumen in `Entity.setRemoved`), und
getragene/ausgestellte strahlende Teile geben feine Wachs-Glanz-Partikel ab (Chance je Tick
2 % × Strahlkraft, höchstens 12 %). Seit der zweiten Runde setzen auch Mobs Licht (`DynamicLightHandler.tickWearer`, alle 4 Ticks, Position mit dem Mob gespeichert; Rüstungsständer alle 10 Ticks).
