# Hammer nur beim Schleichen, Holz-/Melonen-Achtel, essbare Glitzermelone (Queue Nachtrag 24, Branch claude-q-hammer)

Besitzer-Wortlaut: `.claude/QUEUE.md` Nachtrag 24 (drei Punkte). Bestand: `SledgehammerItem#getTransformationState`
(vorwärts Block → Treppe → Stufe ohne Schleichen; Schleichen ohne Berührung des Konstrukteurs = Ecken abtragen über
`HammerCorners#subtract`; Schleichen mit Berührung = rückwärts), Hand-Hinweis `TransformTargets` fragt dieselbe Methode.
0,125er-System = `CheckerOctetBlock`/`CheckerOctetItem` (Schach, 13 Farben, ein Block mit Farb-Eigenschaft).

## Entscheidungen (Agent, autonom)
- **Modus als reine Funktion** `SledgehammerItem#reshapeMode(sneaking, constructorsTouch, cornersSupported)`:
  Schleichen + Berührung = rückwärts (unverändert), Schleichen ohne Berührung = abtragen, ohne Schleichen = nichts.
  Nur 26.2 (keine Ecken, `TRANSFORM_HINTS_AND_CORNERS=false`) behält das alte Vorwärts-Umformen, sonst könnte der
  Hammer dort gar nichts mehr umformen. Der Hinweis (Wackeln) folgt automatisch, weil er dieselbe Methode fragt.
- **Abtragen in Achteln** (`HammerCorners#subtract` neu): Form des Blocks als 8-Bit-Achtelmaske (Kollisionsform),
  das getroffene Achtel fällt weg. Ergebnis in dieser Reihenfolge: Stufe (genau eine Hälfte) → Treppe (Vanilla-Form,
  `simplebuilding_carved`) → **Achtelzelle des Materials** (nicht unterstützte Form, z. B. oben und unten je ein
  Achtel weg, Diagonale, Stufe mit fehlendem Viertel, Melonenblock) → sonst kein Abtragen (kein Wackeln).
  Die Treppensockel-Sperre entfällt (Sockel-Achtel abtragen → Achtel). Das abgetragene Achtel fällt als Item
  heraus, wenn das Material eins hat (nicht im Kreativmodus).
- **Achtel-Materialien** als eigene Blöcke `<holz>_octet` (12 Vanilla-Holzarten, Bretter-Familie: Bretter, Treppe,
  Stufe) und `melon_octet` (Item = Melonenscheibe): gemeinsame Basis `OctetCellBlock` (aus `CheckerOctetBlock`
  herausgezogen), je Block eigene Eigenschaften (Holz brennbar, Klang). Die Simple-Mods haben keine eigenen Holzarten;
  Mod-Hölzer anderer Mods sind nicht abgedeckt (Liste `MaterialOctets` erweiterbar, N19 baut darauf auf).
- Holz-Achtel: Steinmetz Bretter → 8, formlos 8 → 1 Bretter. Melone: Schleichen + Rechtsklick mit einer
  Melonenscheibe auf einen Block setzt ein Melonen-Achtel (ohne Schleichen wird weiter gegessen).
- Glitzernde Melonenscheibe essbar: 6 Hunger / 1,2 Sättigung (wie Goldene Karotte, gleicher Goldpreis),
  über `Item.Properties#finalizeInitializer` (Muster simplefun `EnchantabilityMixin`).
- Modelle per Generator `tools/textures/octets_2026_10_09.py` (Multipart wie `checker_octet`), Holz nutzt die
  Vanilla-Brettertextur (Referenz, nichts eingecheckt), Melone eigene Fruchtfleisch-Textur für Schnittflächen.

## Prüfung
Compile 3 Loader; Tests: Modus-Funktion, kein Umformen/Wackeln ohne Schleichen, Zerlegen in Achtel bei nicht
unterstützter Form, Holz-Achtel aus Brettern, Melone essbar/platzierbar; bestehende Hammer-/Hinweis-Tests angepasst.
