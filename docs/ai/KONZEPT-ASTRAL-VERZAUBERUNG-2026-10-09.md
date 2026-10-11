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

## Kosten-Konzept 11.10. (Nachtrag 35, Branch claude-astral4) - ersetzt Budget/Kosten aus N27/N32
Besitzer: höchstens Level 50; erst bei Stärke 50 sind alle angezeigten Verzauberungen zugleich maximierbar, darunter
weniger (kein „∞“ mehr). Preis je Verzauberungsstufe nach ihrem Wert, dazu eine „Steuer“ fürs Kombinieren: drei
schwache dürfen unter 50 Level kosten, drei starke kosten 50.

**Punkte = XP-Level.** Es gibt nur noch eine Einheit: der Preis ist die Zahl der Level, die das Verzaubern verbraucht
(der Spieler muss sie haben). Budget = Stärke des Tisches, mindestens 3, höchstens 50.

**Formel.**
1. Wert einer Verzauberung auf Höchststufe: `W = Grundwert(Amboss-Kosten) + 2 x Höchststufe`, Grundwert nach den
   Vanilla-Amboss-Kosten (= Seltenheit): 1 → 4, 2 → 6, bis 4 → 9, darüber 12.
2. Reglerstufe `L` von `M` kostet ihren Anteil: `ceil(W x L / M)`.
3. Steuer: bei `n` gewählten Verzauberungen `Preis = min(50, ceil(Summe x (3 + n) / 4))`, also +1/4 der Summe je
   weiterer Verzauberung (eine: x1, zwei: x1,25, drei: x1,5).
4. Lapislazuli = 1 je angefangene 10 Level (1-5), Lohenstaub doppelt so viel.

Weil kein Preis 50 übersteigt, passt bei Stärke 50 jede Kombination; darunter schneidet das Budget ab (zu teure
Reglerstufen grau). Entschieden (Claude): Verbrauch = Preis (Besitzer: „XP-Level-Kosten max 50“), statt der alten 1-5
Level; das ist teurer als Vanilla, dafür gezielt und ohne Amboss-Strafkosten.

**Werte (Vanilla 26.3).**

| Verzauberung | Amboss | Max | Wert W | Preis je Stufe 1..Max |
|---|---|---|---|---|
| Schärfe / Effizienz / Stärke | 1 | 5 | 14 | 3, 6, 9, 12, 14 |
| Schutz / Durchschuss | 1 | 4 | 12 | 3, 6, 9, 12 |
| Treue | 1 | 3 | 10 | 4, 7, 10 |
| Haltbarkeit / Schnellladen | 2 | 3 | 12 | 4, 8, 12 |
| Rückstoß | 2 | 2 | 10 | 5, 10 |
| Federfall | 2 | 4 | 14 | 4, 7, 11, 14 |
| Bann / Dichte | 2 | 5 | 16 | 4, 7, 10, 13, 16 |
| Plünderung / Glück / Glück des Meeres / Köder | 4 | 3 | 15 | 5, 10, 15 |
| Verbrennung / Schlag | 4 | 2 | 13 | 7, 13 |
| Flamme / Mehrfachschuss / Wasseraffinität | 4 | 1 | 11 | 11 |
| Harpune | 4 | 5 | 19 | 4, 8, 12, 16, 19 |
| Behutsamkeit / Unendlichkeit / Entladung | 8 | 1 | 14 | 14 |
| Dornen | 8 | 3 | 18 | 6, 12, 18 |

**Beispiele.**

| Item | Wahl | Summe | Steuer | Preis (Level) | Lapis / Lohenstaub | ab Stärke |
|---|---|---|---|---|---|---|
| Schwert | Schärfe V | 14 | x1 | 14 | 2 / 4 | 14 |
| Schwert | Schärfe V + Plünderung III | 29 | x1,25 | 37 | 4 / 8 | 37 |
| Schwert | Schärfe V + Plünderung III + Verbrennung II | 42 | x1,5 | 50 (63) | 5 / 10 | 50 |
| Schwert | Schärfe II + Haltbarkeit I + Rückstoß I | 15 | x1,5 | 23 | 3 / 6 | 23 |
| Spitzhacke | Effizienz V + Haltbarkeit III | 26 | x1,25 | 33 | 4 / 8 | 33 |
| Spitzhacke | Effizienz V + Glück III + Haltbarkeit III | 41 | x1,5 | 50 (62) | 5 / 10 | 50 |
| Spitzhacke | Behutsamkeit + Effizienz V + Haltbarkeit III | 40 | x1,5 | 50 (60) | 5 / 10 | 50 |
| Bogen | Stärke V + Schlag II + Flamme | 38 | x1,5 | 50 (57) | 5 / 10 | 50 |
| Buch | Schutz IV + Rückstoß II + Treue III (drei schwache) | 32 | x1,5 | 48 | 5 / 10 | 48 |
| Buch | Schärfe I (ohne Regale) | 3 | x1 | 3 | 1 / 2 | 0 (Budget 3) |

Bei Stärke 30 (15 Bücherregale) z. B. Schärfe V + Plünderung II (24 x 1,25 = 30); bei 40 Schärfe V + Plünderung III (37).

**UI (N35).** Namen in kleinerer Schrift (75 %), Regler-Abschnitte je Stufe abgestuft (Stufe 1 in der Reglerfarbe,
jede höhere einen Ton dunkler, höchstens bis 55 % Helligkeit), oben rechts „Preis/Budget Lv“ statt „x/∞“ (Tooltip:
Stärke, Preis von Budget, Erklärung), im Verzaubern-Knopf unter Buch und Level in einer Zeile ein blauer Punkt mit dem
Lapis- und ein orange-roter Punkt mit dem Lohenstaub-Bedarf (rot, wenn er fehlt). Tooltip je Regler: Preis Stufe 1 und
Wert auf Höchststufe, Steuer-Hinweis. Code: `AstralEnchanting.value/levelPrice/price/maxAffordable/budget`.
