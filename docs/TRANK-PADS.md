# Trank-Pads: Balance je Wirkung

Stand 2026-09-29 (Besitzer-Auftrag). Code: `com.simplebuilding.tweaks.PotionPadRules` (eine Stelle fuer alle
Zahlen, auch fuer die Balancing-Zentrale), angewandt in `PotionPadBlockEntity#grant`. Tests:
`PotionPadRuleTests` (je Regelkategorie ein Test) und `PotionPadTests`.

## Grundsatz

Ein Pad **behaelt seinen Trank**, bis es abgebaut wird, und gibt ihn beliebig oft wieder aus - nichts wird
verbraucht. Balanciert wird nur ueber vier Hebel:

1. **Wer** bekommt die Wirkung (alle / nur der Besitzer / niemand; Mobs nie).
2. **Wie stark**: hoechstens die Stufe, die ein Vanilla-Trank braut.
3. **Wie lange**: Stufendauer (45 / 90 / 180 s, Easter-Endstufe 360 s), aber nie laenger, als der
   gespeicherte Trank getrunken wirken wuerde, und nie ueber einer festen Obergrenze der Wirkung.
4. **Wie oft**: Abklingzeit des Pads aus der tatsaechlich gegebenen Dauer, mal Faktor der Wirkung; dazu eine
   Sperre je Spieler ueber alle Pads fuer Heilung, Schaden und Regeneration.

## Ziele (datengetrieben per Mob-Effekt-Tag)

| Tag | Bedeutung |
|---|---|
| `#simplebuilding:potion_pad/blocked` | wird nie gegeben |
| `#simplebuilding:potion_pad/public` | jeder Spieler auf dem Pad (hebt `owner_only` auf; leer ausgeliefert) |
| `#simplebuilding:potion_pad/owner_only` | nur wer das Pad gesetzt hat |
| (in keinem Tag) | Kategorie der Wirkung: positiv = alle, schaedlich/neutral = nur Besitzer |

Vorrang: blocked > public > owner_only > Kategorie. Ein Pad ohne Besitzer (per Befehl/Struktur gesetzt) zaehlt
jeden als Besitzer - dort gibt es keinen Spieler, der damit jemanden aergern koennte. Ein Spieler, fuer den
nichts erlaubt ist (fremdes Gift-Pad, gesperrt), laedt das Pad gar nicht erst auf und kann es so auch nicht fuer
den Besitzer in die Abklingzeit schicken. Mobs werden nie gesucht (das Pad scannt nur Spieler) - keine Mobfallen.

## Zahlen

- **Volle Dauer** = min(Stufendauer, Dauer im Trank x Anteil, Obergrenze). Anteil ist ueberall 1,0 ("nie laenger
  als getrunken"); eine unendliche Wirkung im Trank hat nur die Stufengrenze.
- **Stufen** werden auf die Regel-Hoechststufe begrenzt (Befehls-Traenke mit Resistenz V o. ae.).
- **Abklingzeit** = Config-Faktor (`tweaks.padTuning.potionPadCooldownFactor`, Standard 1,5) x laengste gegebene
  volle Dauer x Faktor der Wirkung, genommen ueber alle gegebenen Wirkungen. Sofortwirkungen zaehlen als 45 s
  (unabhaengig von der Stufe - ein besseres Pad heilt nicht seltener). Faktor 0 = keine Abklingzeit; die
  Sperren je Spieler gelten trotzdem.
- **Sperre je Spieler**: nach einer vollen Ladung bekommt derselbe Spieler dieselbe Wirkung von keinem Pad,
  bis die Sperre ablaeuft (nicht gespeichert; laeuft nach Spielzeit ab).

## Regeltabelle

Stufe = hoechste Stufe (I = Verstaerkung 0). "Trank" = nie laenger als der Trank. Abkling-x = Faktor auf die
Abklingzeit.

| Wirkung | Vanilla-Trank | Ziel | max. Stufe | Dauer | Abkling-x | Sperre |
|---|---|---|---|---|---|---|
| Schnelligkeit (speed) | Swiftness | alle | II | Trank | 1 | - |
| Langsamkeit (slowness) | Slowness, Turtle Master | Besitzer | VI | Trank | 1 | - |
| Eile (haste) | - | alle | I | Trank | 1 | - |
| Abbaulaehmung (mining_fatigue) | - | Besitzer | I | Trank | 1 | - |
| Staerke (strength) | Strength | alle | II | Trank | 1 | - |
| Direktheilung (instant_health) | Healing | alle | II | sofort, einmal bei 3 s | 2 (Basis 45 s) | 60 s |
| Direktschaden (instant_damage) | Harming | Besitzer | II | sofort, einmal bei 3 s | 2 (Basis 45 s) | 60 s |
| Sprungkraft (jump_boost) | Leaping | alle | II | Trank | 1 | - |
| Uebelkeit (nausea) | - | Besitzer | I | Trank | 1 | - |
| Regeneration | Regeneration | alle | II | Trank | 1,5 | 60 s |
| Resistenz (resistance) | Turtle Master | alle | IV | Trank | 1,5 | - |
| Feuerresistenz | Fire Resistance | alle | I | Trank | 1 | - |
| Unterwasseratmung | Water Breathing | alle | I | Trank | 1 | - |
| Unsichtbarkeit | Invisibility | alle | I | Trank | 1,5 | - |
| Blindheit | - | Besitzer | I | Trank | 1 | - |
| Nachtsicht | Night Vision | alle | I | Trank | 0,5 | - |
| Hunger | - | Besitzer | I | Trank | 1 | - |
| Schwaeche (weakness) | Weakness | Besitzer | I | Trank | 1 | - |
| Vergiftung (poison) | Poison | Besitzer | II | Trank | 1 | - |
| Ausdoerrung (wither) | - | Besitzer | I | Trank | 1 | - |
| Leuchten (glowing) | - | Besitzer | I | Trank | 1 | - |
| Schwebe (levitation) | - | Besitzer | I | Trank, max. 10 s | 1 | - |
| Glueck (luck) | Luck | alle | I | Trank | 1 | - |
| Pech (unluck) | - | Besitzer | I | Trank | 1 | - |
| Sanfter Fall (slow_falling) | Slow Falling | alle | I | Trank | 1 | - |
| Dunkelheit (darkness) | - | Besitzer | I | Trank | 1 | - |
| Windgeladen, Weben, Schleimen, Befall | Wind Charged, Weaving, Oozing, Infested | Besitzer | I | Trank | 1 | - |
| Extraenergie, Absorption, Saettigung | - | **gesperrt** | | | | |
| Meereskraft, Gunst des Delfins, Atem des Nautilus | - | **gesperrt** | | | | |
| Held des Dorfes, Boeses Omen, Raid-Omen, Pruefungs-Omen | - | **gesperrt** | | | | |

Begruendungen:

- **Sofortwirkungen** (Heilung/Schaden) wirken nur beim 100-%-Schritt, einmal pro Aufladung; der
  Config-Faktor 1,5 auf fester 45-s-Basis (135 s auf jeder Stufe) und 60 s Sperre je Spieler: eine Reihe von
  Pads kann keine Dauerheilung liefern. Schaden trifft nur den Besitzer (keine Fallen fuer Fremde).
- **Regeneration** heilt ueber Zeit mehr als Direktheilung: nie laenger als der Trank, 1,5-fache Abklingzeit,
  60 s Sperre gegen Pad-Ketten.
- **Turtle Master / Resistenz**: Vanilla-Stufen (Resistenz IV, Langsamkeit VI) und die kurze Trankdauer (20 s /
  40 s) bleiben; 1,5-fache Abklingzeit, weil Resistenz im Kampf entscheidet.
- **Unsichtbarkeit**: PvP-relevant, daher 1,5-fache Abklingzeit. **Nachtsicht** ist harmloser Komfort: halbe
  Abklingzeit, erneutes Betreten haelt sie aufrecht.
- **Schwebe** nur fuer den Besitzer und hoechstens 10 s (wie ein Shulkergeschoss): ein Aufzug, kein Flug.
- **Schaedliche und neutrale Wirkungen** (Gift, Ausdoerrung, Langsamkeit, Schwaeche, Befall, Windgeladen,
  Weben, Schleimen, Leuchten ...) nur fuer den Besitzer: niemand kann andere Spieler per Pad vergiften,
  markieren oder mit Silberfischen belasten.
- **Gesperrt** sind Wirkungen, die kein brauchbarer Trank traegt und die an ihre Quelle gebunden sind (Nahrung,
  Leuchtfeuer, Aquisitor, Raids, Pruefkammern): Saettigung waere Dauer-Essen, Omen wuerden Raids/Pruefungen auf
  Knopfdruck ausloesen, Held des Dorfes Dauerrabatt.
- **Staerkere/verlaengerte Varianten**: ein langer Trank gibt die volle Stufendauer (8 min > 180 s), ein starker
  nur seine eigene kurze Dauer (Strong Swiftness II 90 s auch auf Stufe III). Stufen von Befehls-Traenken werden
  auf die Vanilla-Hoechststufe gekappt.

## Beispiele (Config-Faktor 1,5)

| Pad | Trank | gegeben | Abklingzeit |
|---|---|---|---|
| I | Swiftness (3 min) | 45 s | 67,5 s |
| III | Long Night Vision (8 min) | 180 s | 135 s (x0,5) |
| III | Strong Regeneration (22,5 s) | Regeneration II 22,5 s | etwa 50,7 s (x1,5) |
| I oder III | Healing | +4 HP | 135 s (45 s x 1,5 x 2) |
| III | Strong Swiftness (90 s) | Swiftness II 90 s | 135 s |

## Stellschrauben

- Tags (Datapack): wer eine Wirkung bekommt.
- `PotionPadRules.TABLE` (Code, eine Stelle): Hoechststufe, Obergrenze in s, Anteil an der Trankdauer,
  Abkling-Faktor, Sperre. `PotionPadRules.setOverrides(...)` ersetzt einzelne Eintraege zur Laufzeit (fuer die
  Balancing-Zentrale, Phase 2).
- Config: `tweaks.padTuning.potionPadCooldownFactor` (global), `potionPadChargeStepTicks`,
  `tweaks.pads.enablePotionPads`.
