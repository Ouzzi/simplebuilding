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
