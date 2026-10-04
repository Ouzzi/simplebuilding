# Plan: Nihil-Gewoelbe (Nihil Vault) – Besitzer-Queue Nachtrag 8

Wunsch: „genau analog zum Astralgewoelbe, aber eine geteilte Enderkiste auf der Welt – jeder hat Zugriff;
Speicherplatz wie eine Enderkiste.“

## Ist-Zustand (Astralgewoelbe)
- `AstralVaultBlock extends EnderChestBlock`, nutzt die Vanilla-`EnderChestBlockEntity` (Mixin
  `AstralBlockEntityTypeMixin` laesst den Block fuer `BlockEntityTypes.ENDER_CHEST` zu). Deckel, Sound, Partikel,
  Wasser, Oeffner-Zaehler kommen von Vanilla; der Zaehler prueft `isOwnContainer` ueber
  `player.getEnderChestInventory().isActiveChest(chest)`.
- Renderer `AstralVaultRenderer` (Fabric + NeoForge fuer ENDER_CHEST registriert) waehlt das eigene Truhenbild.
- Rezept/Loot/Fortschritt/Modelle von Hand in `mc26_3/overlay/resources` (Generator `tools/textures/end_system_textures.py`
  schreibt Modelle + Texturen); Tags per Datagen (`ModBlockTagProvider`).
- Schalter `server.features.astralVault` (ServerTuningConfig, ConfigOptions.RECIPES_ON_RELOAD, RecipeFilter, ConfigOptionTests).
- Kreativ-Tab Zeile `astral_storage` (Endertruhe, Astralgewoelbe), Suche nach `ENDER_CHEST`, Jade (`BlockInfo` CHEST_SLOTS),
  JEI-Info (`RecipelessJeiInfo`), Handbuch-Kapitel (`GuideContent`, Buch STORAGE), Wiki (`wiki/manual.json` zweimal).
- Rechte/Claims: das Astralgewoelbe hat keine eigene Claim-Logik; Claim-Mods greifen ueber die normalen
  Benutzen-/Abbau-Ereignisse der Loader. Das Nihil-Gewoelbe macht es genauso.
- Testzentrale: `TestCentreSections.devices` stellt alle unbekannten Funktions-Zeilen automatisch auf – ein Eintrag in
  der Zeile `astral_storage` stellt das Nihil-Gewoelbe direkt neben das Astralgewoelbe.

## Umsetzung
1. `NihilVaultBlock extends EnderChestBlock` (gleiche Festigkeit 50/1200, Spitzhacke), gleiche Sperre bei festem Block
   darueber, Schalter `features.nihilVault`, Statistik/Piglin-Zorn wie Vanilla.
2. `NihilVaultStorage extends SavedData` (Oberwelt-Datenspeicher, ID `simplebuilding:nihil_vault`): ein
   27-Platz-`SimpleContainer`, `setChanged` → `setDirty`. Codec: Liste `ItemStackWithSlot`.
3. Menue: `ChestMenu.threeRows` ueber eine `NihilVaultView` (delegiert Slots an den geteilten Container; `startOpen`/`stopOpen`
   binden die Truhe als aktive Endertruhe des Spielers fuer den Vanilla-Oeffnerzaehler und loesen sie wieder).
   Alle Menues teilen dieselbe Container-Instanz – wie mehrere Spieler an einer Vanilla-Truhe, also kein Dupe;
   `stillValid` = Schalter an + Block noch da + Reichweite.
4. Mixin `AstralBlockEntityTypeMixin` auch fuer `NihilVaultBlock`; Renderer waehlt `nihil_vault`-Bild.
5. Daten: Rezept `NSN/NEN/NSN` (S Nihilitsplitter, N Enderitklumpen, E Endertruhe) – analog Astral von Hand im Overlay
   (Abweichung von „Rezepte per Datagen“, weil das Vorbild auch so liegt), Loot, Fortschritt, Blockstate/Modelle,
   Tags per Datagen.
6. Textur: Generator erweitert – Astralgewoelbe-Bild (Enderit-Truhe) auf Nihil-Toene umgefaerbt, Nihil-Ring statt
   Astral-Raute; Vorschau `previews/nihil-gewoelbe-vorschau.png` (A Endertruhe, B Astralgewoelbe, C Nihil-Gewoelbe).
7. Lang EN/DE in beiden Orten, Kreativ-Tab/Suche neben Astralgewoelbe, Jade (27 Plaetze, „mit allen Spielern geteilt“),
   JEI-Info, Handbuch-Kapitel (Astral-Kapitel erweitert), Wiki-Eintrag, Schalter + Rezeptfilter.
8. McVersion: `END_SYSTEMS` wie das Astralgewoelbe (26.3 an, 26.2 aus).

## Entscheidungen
- **Kein Komparator**: die Vanilla-Endertruhe hat keinen; ein weltweit geteilter Inhalt muesste bei jeder Aenderung
  alle Gewoelbe in allen geladenen Chunks aktualisieren (teuer) und waere ein dimensionsuebergreifender Signal-Kanal.
- **Trichter** haben keinen Zugriff (wie Endertruhe/Astralgewoelbe).
- **Speicherort** Oberwelt-SavedData: eine Welt = ein Inhalt, auch fuer Gewoelbe im Nether/End.
- Geld-Modul (simplemoney-Preise) bleibt aussen vor (eigener Lauf).

## Risiken
- Bindung der aktiven Endertruhe: ein Spieler oeffnet ueber das Gewoelbe nie sein persoenliches Inventar; die Bindung
  dient nur dem Deckel und wird bei `stopOpen` geloest.
- SavedData-Codec braucht Registry-Ops fuer Item-Komponenten (der Datenspeicher liefert sie).

## Verifikation
GameTests (EndSystemsTests): zwei Gewoelbe teilen Inhalt; zwei Spieler sehen dasselbe; kein Dupe bei gleichzeitigem
Zugriff (zwei Menues, Schnellverschieben); Speichern/Laden des SavedData; Abbau droppt Block, Inhalt bleibt; Schalter
aus sperrt und bewahrt. Gates: fabric-263, neoforge-263, Compile 26.2 + Forge 26.3, `gradlew check -q`, Wiki.
