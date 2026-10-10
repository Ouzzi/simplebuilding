# Konzept Astral-Verzauberungstisch, Lohen-Regale (2026-10-09)

Besitzer-Wunsch: Queue Nachtrag 27. Hier die Umsetzungsregeln; mit „(entschieden)“ markierte Punkte hat Claude
festgelegt, Besitzer kann jederzeit ändern.

## Herstellung
- Verzauberungstisch + ca. 20 Schläge mit Netherit- oder Enderit-Hammer, Enderit-Nugget in der Nebenhand
  (verbraucht beim letzten Schlag). Fortschritt wie bei den übrigen Hammer-Umwandlungen (Partikel, Klang).
- Lohenholz: 8 Lohenstaub um 1 Nether-Holz (Bretter) → 1 Lohenholz je Holzart (Karmesin, Wirr; Mod-Nether-Hölzer
  automatisch, falls vorhanden).
- Lohenbuch: wie Buch (Papier ×3 + Leder), aber mit Lohenstaub statt Leder (entschieden).
- Lohen-Bücherregal: 6 Lohenholz + 3 Lohenbücher (wie Vanilla-Regal), Varianten je Holzart.

## Stärke (Regale + Boden)
- Regale zählen wie Vanilla (Positionen, Luftspalt). Normales Regal = 1, Lohen-Regal = 2 Punkte; Deckel 15 Regale.
- Stufe = min(30, 2 × Punkte) bei normalen Regalen, wie Vanilla (15 Regale → 30).
- **40:** Boden 5×5 unter dem Tisch aus dem Sonderblock (Platzhalter weinender Obsidian) + mindestens 20 Punkte.
- **50:** Boden + 30 Punkte (alle 15 Regale Lohen-Regale).

## UI
- Gleicher Aufbau wie der Vanilla-Tisch (Item-Slot, Lapis-Slot, daneben Lohenstaub-Slot). Lapis und Lohenstaub je bis
  1 Stack, bleiben im Block gespeichert (auch beim Verlassen, droppen beim Abbau).
- 3 zufällige, zum Item passende Verzauberungen (keine Schatz-Verzauberungen, wie Vanilla), je mit Regler 0..Max.
  Neuer Zufallssatz nach jedem Verzaubern (wie Vanilla-Seed).
- Budget = Stufe des Tisches; jede Reglerstufe kostet Punkte nach Seltenheit (häufig 3, selten 6, sehr selten 10 je
  Stufe, so dass 30 grob zwei mittlere Verzauberungen erlaubt). Besitzer: fair, aber ein klein wenig teurer als ein
  vergleichbarer Vanilla-Wurf, weil man gezielt wählen kann. Bei 50 sind alle Regler bis Max frei.
  Zu teure Stufen sind ausgegraut.
- Kosten: bis 30 → 1–3 Level je nach gewähltem Anteil (wie Vanilla 1–3); 40 → 4, 50 → 5 Level. Lapis = verbrauchte
  Level, Lohenstaub = doppelt so viel.

## Abbauen
- Dauert doppelt so lange wie ein normaler Verzauberungstisch. Drop: normaler Verzauberungstisch + das eingesetzte
  Enderit-Teil zurück (Besitzer sagte „Enderit-Barren“; hergestellt wird mit einem Nugget – zurück kommt das
  eingesetzte Nugget, sonst entstünde ein Barren aus einem Nugget). Gelagerter Lapis/Lohenstaub droppt.

## Boden
- **Lohen-Obsidian** (Blazing Obsidian): Variante des weinenden Obsidians, leuchtet; Rezept analog zu den anderen
  Lohenstaub-Rezepten (8 Lohenstaub um 1 weinenden Obsidian). 5×5 unter dem Tisch für Stufe 40/50.
- Später ggf. Astralit- oder Nihilit-Fliesen als weitere Böden.

## Umsetzung (Branch claude-q-astral, 2026-10-09)
- Code: `com.simplebuilding.enchanting` (`AstralEnchanting` = Regeln als reine Funktionen + Welt-Erkennung,
  Block, Block-Entity, `BlazingObsidianBlock`), Menü `screen/AstralEnchantingMenu`, Bildschirm
  `client/gui/AstralEnchantingScreen`, schwebendes Buch `client/render/AstralEnchantingTableRenderer`. Nur 26.3
  (`McVersion.ASTRAL_ENCHANTING`).
- Herstellung: Eintrag in der Hammer-Aufwertungstabelle (`SledgehammerUpgrades`, additiv): Verzauberungstisch →
  Astral-Tisch, Enderit-Nugget, ab Netherit-Hammer, Faktor 4 (5 → 20 Schläge), 10 Haltbarkeit je Schlag.
  `EnchantingTableBlockMixin` reicht den Rechtsklick im Schmiedestand an den Hammer weiter (wie `ChestBlockMixin`).
- Entschieden (Claude, Besitzer kann ändern):
  - Budget ohne Regale = 3 Punkte (eine Stufe einer häufigen Verzauberung, seltene ausgegraut).
  - Seltenheit nach Vanilla-Gewicht: Gewicht ≥ 5 (häufig/gewöhnlich) 3, 2 (selten) 6, 1 (sehr selten) 10 Punkte.
  - Verbrauch bis Stufe 30: 1 Level je angefangene 10 Punkte (1–3); Stufe 40 immer 4, 50 immer 5 Level.
  - Mindest-Level des Spielers = ausgegebene Punkte (höchstens 30, mindestens der Verbrauch) – wie Vanilla, das für
    das dritte Angebot Level 30 verlangt.
  - Lohen-Regale zählen am Vanilla-Tisch wie ein normales Regal (Tag `enchantment_power_provider`).
  - Trichter dürfen Lapis/Lohenstaub nachfüllen, aber nichts herausziehen.
  - Lohen-Regal droppt 3 Lohenbücher (mit Behutsamkeit sich selbst), wie Vanilla-Regale.
  - Lohenholz nur für Karmesin und Wirr; Mod-Nether-Hölzer sind (noch) nicht automatisch abgedeckt.
- Prüfung: `AstralEnchantingTests` (Regeln, Regal-/Boden-Erkennung, Hammer-Herstellung, Abbau-Drop, Lager bleibt,
  Verzaubern verbraucht richtig, Rezepte), Stil-Test, Client-Screenshot `modui-astral-enchanting`.

## Nachtrag 31 (Branch claude-q-astral2, 2026-10-10)
- Name: **Astral Enchanter** / DE **Astral-Verzauberer** (IDs `astral_enchanting_table` usw. bleiben).
- Stärke neu (Besitzer: dieselbe Anzahl Lohen-Regale wie die normale Höchstzahl bringt das Maximum):
  bis 15 Punkte `2 x Punkte` (15 Bücherregale = 30), darüber gleichmäßig weiter bis **50 bei 30 Punkten**
  (15 Lohen-Regale, ohne Boden). Jeder weitere Punkt hebt die Stufe (16 → 31, 20 → 37, 23 → 41).
  Entschieden (Claude): der Lohen-Obsidian-Boden bleibt als Abkürzung, **+10 ab 15 Punkten** (15 Bücherregale auf dem
  Boden = 40; 23 Punkte auf dem Boden = 50). Vorher brauchte 40/50 zwingend den Boden.
- Verbrauch: 1 Level je angefangene 10 Punkte (bis 30 Punkte wie bisher 1–3, darüber bis 5), bei Stufe 50 immer 5
  (vorher bei 40 immer 4).
- UI: Bildschirm 36 px höher (Inventar auf y 120), Zeilen 28 px: Name + Stufe, darunter der Name in den
  Verzauberungs-Glyphen (Vanilla-Schrift `minecraft:alt`), darunter der Regler mit Lücken zwischen den Stufen und
  einem kleinen Strich je Stufe (gewählte Stufe gold).
- Der „Plus/Minus“-Knopf unter Lapis/Lohenstaub war der Verzaubern-Knopf: ein Stern (sah aus wie „+“) und die
  Level-Kosten, „-“ ohne Wahl. Jetzt zeigt er ein verzaubertes Buch und die Kosten; Tooltip wie bisher.
- Ziehen: Client-Test `astral-enchanter` (AstralEnchanterClientTest) drückt, zieht und lässt los über den echten
  Fensterpfad (MouseHandler) und prüft Client- und Server-Wert des Reglers.

## Stufenregel 10.10. (Nachtrag 32, Branch claude-q-astral3) - ersetzt die Stärke aus Nachtrag 31
Besitzer: 0-30 mit normalen Bücherregalen (15 = 30); 30-40 mit Lohen-Regalen ODER dem Lohen-Obsidian-Boden; 50 nur mit
beidem. Je höher die Stufe, desto mehr Regler-Punkte.

**Formel.** Regal-Punkte `p` = Summe der besten 15 Regale (Bücherregal 1, Lohen-Regal 2), also 0..30.
- Regale allein: `p <= 15` → `2 x p` (15 Bücherregale = 30, wie Vanilla); darüber `30 + gerundet((p - 15) x 2/3)`,
  bei `p = 30` (15 Lohen-Regale) also **40**.
- Boden (5x5 Lohen-Obsidian direkt unter dem Tisch, vollständig): **+10, sobald die Regale 30 ergeben** (`p >= 15`).
- Stufe = Regale + Boden, höchstens 50. **50 genau bei `p = 30` und Boden** (15 Lohen-Regale UND Boden).
- Wie gemischte Regale zählen: jedes Lohen-Regal statt eines Bücherregals bringt einen Punkt mehr, über 15 Punkte
  hinaus etwa 2/3 Stufe (z. B. 5 Lohen + 10 Bücher = 20 Punkte = 33; 8 Lohen + 7 Bücher = 23 Punkte = 35).
  Mehr als 15 Regale zählen nicht (die besten 15), Luftspalt-Regel wie Vanilla.

| Aufbau | Stufe |
|---|---|
| keine Regale | 0 (Budget 3) |
| 5 Bücherregale | 10 |
| 10 Bücherregale | 20 |
| 15 Bücherregale | 30 |
| 15 Bücherregale + Boden | 40 |
| 5 Lohen + 10 Bücher | 33 |
| 8 Lohen + 7 Bücher + Boden | 45 |
| 15 Lohen-Regale | 40 |
| 15 Lohen-Regale + Boden | 50 |

**Budget / Kosten je Stufe (Regler-Punkte).** Budget = Stufe, mindestens 3, bei 50 unbegrenzt (alle Regler bis Maximum):
0 → 3, 10 → 10, 20 → 20, 30 → 30, 40 → 40, 50 → ∞. Punkte je Reglerstufe nach Seltenheit unverändert (3 / 6 / 10).
Level-Verbrauch: 1 je angefangene 10 ausgegebene Punkte (30 → 3, 40 → 4), bei Stufe 50 immer 5; Lapis = Level,
Lohenstaub doppelt. Spieler braucht so viele Level wie Punkte (höchstens 30). Fair: 40 ist wirklich ein Ziel mit Aufwand
(15 Lohen-Regale ODER der Boden), 50 verlangt beides; Bücherregale allein bleiben bei Vanilla-30.

**Hinweis im Tooltip** (Mauszeiger über der Stärke): Stufen-Erklärung, „Regale x/30 Punkte, Boden ja/nein“ und eine
Zeile, was für die nächste Stufe fehlt: mehr Bücherregale (`p < 15`), Lohen-Regale ODER Boden (`15 <= p < 30`, kein
Boden), Boden (`p = 30`), Lohen-Regale statt Bücherregale (Boden vorhanden, `p < 30`), oder „Maximum“.
Code: `AstralEnchanting.tier/nextStep`, Menü-DataSlot `setup`. Entschieden (Claude): Boden bleibt +10 erst ab 15 Punkten;
Zwischenwerte gerundet (nicht jede Zusatzpunkt-Stufe steigt um 1).
