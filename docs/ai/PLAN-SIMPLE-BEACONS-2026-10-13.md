# Plan: Simple Beacons (Strong Beacon, Strong Conduit, Strong Upgrade) – 2026-10-13

Branch `claude-beacons` (Basis `claude-wave1`). Grundlage: `docs/ai/KONZEPT-STRONG-BEACON-2026-10-13.md`
(Besitzer 13.10.: **alle Empfehlungen übernommen**), `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`.

## [prüfen]-Stellen gegen die 26.3-Quellen (erledigt)

| Konzept-Annahme | 26.3-Befund | Folge |
|---|---|---|
| `BeaconBlockEntity` statisch (4 Ebenen, `BEACON_EFFECTS`, `VALID_EFFECTS`) | stimmt; `updateBase`/`applyEffects` privat statisch, Typ `BlockEntityTypes.BEACON` fest im Konstruktor | eigener Block + Block-Entity (Variante B) |
| `BeaconBeamOwner` + generischer Renderer | `BeaconRenderer<T extends BlockEntity & BeaconBeamOwner>` | Strahl = Vanilla-Renderer, nur für den neuen Typ registriert |
| Bezahlung `#minecraft:beacon_payment_items` | stimmt (`ItemTags.BEACON_PAYMENT_ITEMS`) | Vanilla-Bezahlslot unverändert |
| Paket prüft feste Liste | `handleSetBeaconPacket`: `containerMenu instanceof BeaconMenu` → `menu.updateEffects(...)` (öffentlich, überschreibbar); `getType()` überschreibbar | `StrongBeaconMenu extends BeaconMenu` mit eigenem Menütyp und eigener Prüfung – **kein eigenes Paket** |
| innere Knopfklassen privat | stimmt | Bildschirm erbt `BeaconScreen`, Zusatzknöpfe eigen; `primary/secondary` über `@Accessor`; „II“-Knopf über Klassennamen (wie `simpleinterfaces`) ausgegraut |
| Conduit-Schale 42 / `(n/7)*16` / Angriff 8 bei 42 | stimmt (`ConduitBlockEntity`, nur Spieler `isInWaterOrRain`) | eigener Block + Block-Entity; Renderer delegiert an Vanilla-`ConduitRenderer` (Vanilla-Optik) |
| Absorption bei Auffrischung | `addEffect` ruft `onEffectStarted` **jedes Mal** → würde alle 4 s nachfüllen | Absorption: vorhandenen Effekt nur verlängern (`update` + Effekt-Paket), Puffer einmal je Aufenthalt wie im Konzept |

## Umfang (alles in `modules/simplebeacons`, Namespace `simplebeacons:`)

1. **Modulgerüst** (`tools/newmod.py`), Abhängigkeit nur `simplelib` (gebündelt) + `framework` (gebündelt), wie Simple Trims.
   SB bündelt es optional (`fabric include`, `neoforge jarJar`, Forge `forgeBundleModules`), Schalter „Enable Simple Beacons“ (`SubMods`).
2. **Starke Aufwertungsvorlage** `strong_upgrade_template` (SmithingTemplateItem): Rezept Netherstern + 4 Diamant + 4 Obsidian;
   bedingt mit SB: Basic-Vorlage statt Diamanten; Duplikation 7 Diamant + Obsidian + Vorlage → 2.
3. **Starkes Leuchtfeuer** `strong_beacon` (eigener Block/BE): 1–6 Ebenen (`maxLevels` 4–6), Reichweite `rangeBase + rangePerLevel × E`
   (10/10 → E5 60, E6 70), Dauer `9 + 2E` s, nur Spieler, Ambient. Basis `#minecraft:beacon_base_blocks` oder `#simplebeacons:strong_base_blocks` (leer).
   Effekte E5: Nachtsicht, Wasseratmung, Langsamer Fall; E6: Feuerresistenz, Delfingunst, Absorption I (je Schalter). „II“ nur für Vanilla-Primäreffekte.
4. **Zweispaltige UI**: Vanilla-Teil links (Mixins von `simpleinterfaces` greifen weiter), rechts Zusatzfeld im SimpleLib-Kastenstil
   (`UiBoxes`): Pyramide n/6, Ebene-5-/Ebene-6-Zeile, Reichweite/Dauer, gesperrte Ebenen ausgegraut mit Tooltip.
5. **Aufwertung**: Schmiedetisch (Leuchtfeuer/Conduit + Vorlage + Netherstern) und In-World: Werkzeug aus `#simplelib:upgrade_tools`
   (Axt solange SimpleLib die Axt-Wege erlaubt) Haupthand, Vorlage Nebenhand, Netherstern im Inventar, 8 Schläge (`InWorldStrikes`:
   Risse, Ton, wachsende Vorschau), Tausch an Ort; Wahl/Sperre/Name bleiben; Vorlage + Stern verbraucht. Abweichung: Hilfsklasse statt
   Eintrag in `simplelib:in_world_upgrades` (den lädt heute nur SB; das Modul muss allein laufen; Datentyp kennt kein Zusatzmaterial).
6. **Starker Conduit** `strong_conduit`: Rahmen 5×5×5 (42) oder 7×7×7 (66), aktiv ab 16, Reichweite `(n/7)×16` bis 144
   (`conduitMaxRange`, Deckel 96–144), über 96 nur echt unter Wasser; Angriff 8 (42er) bzw. 12 (66er); zusätzlich Delfingunst im Wasser.
7. **Daten/Doku**: Lang EN/DE, Wiki-Kapitel (`--strict`), Guide-Buch (`module_guides.py`), Fragentisch, `discover --check`,
   Money-Preise, Testzentrale-Station (`framework TestCentreStations`), `docs/MODS-UND-FEATURES.md`.
8. **Texturen**: eigene Pixel-Art (Leuchtfeuer-Kern, Vorlage) per `tools/textures/simple_beacons_2026_10_13.py`; Glas/weinender
   Obsidian nur als Modell-Verweis auf Vanilla (keine Kopie).

## Risiken
- Forge/NeoForge-Paketpfad: `handleSetBeaconPacket` von Loadern gepatcht? (NeoForge-Quelle geprüft: unverändert.)
- Fremde Beacon-Mixins treffen den neuen Bildschirm als `BeaconScreen`-Unterklasse – gewollt.
- Conduit-Optik = Vanilla (Konzept-Rückfall Frage 5); getönter Renderer offen.

## Verifikation
Compile 3 Loader; Gametests `simplebeacons:*` Modul + Standalone (Fabric/NeoForge/Forge-Modul-Target): Ebenen 5/6 + Abbruch, Reichweite/Dauer,
Effekte je Ebene + Ablehnung, Upgrade-Weg (Schläge, Tausch, Daten, Verbrauch), Schmiede-Rezepte, Conduit 42/66 + Reichweite + Delfingunst,
Konfig, Schalter; `wiki/generate.py --all --check --strict`, `module_guides.py --check`, `discover.py --check`, `check_data.py`;
Client-Screenshots (`client-fabric-263`) → `~/previews/beacons/`.
