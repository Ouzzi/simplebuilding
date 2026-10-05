# Plan Crucible P5/P6 – SimpleBuilding-Teile (2026-10-05)

Grundlage: `docs/ai/PLAN-CRUCIBLE-2026-10-04.md` (§2/§2b/§2c = Besitzer-Vorgaben), Prinzipien 5a/6a. Branch `claude-crucible`.

## Ist-Zustand (erhoben)
- `simplelib` (P0–P4) fertig; API `com.simplelib.api.SimpleLibApi` (Hitzequellen, `disableAxeWays`, `upgradeCrucible`).
  `CrucibleTier.ENDERITE`, `BarrelTier.ENDERITE`, Menütyp `simplelib:enderite_crucible` existieren schon.
- Fehler gefunden: `CrucibleBlock`/`CrucibleBarrelBlock` öffnen bei Rechtsklick immer das Menü (`useWithoutItem`), bevor
  `ItemStack.useOn` (Axt-Mixin) läuft → Axt-Aufwertung/Fass-Anbringen funktionierten nur in Tests (direkter Aufruf).
- SB: Vorschlaghammer-Aufwertungen datengetrieben (`SledgehammerUpgrades.builtInTable` → `sledgehammer_upgrades/*.json`),
  `Upgrade` kennt `durationFactor`/`materialCost` (Shulker-Muster = „doppelt“). Diamantblock-Zerschlagen als Vorbild für
  Quarzblock → 4 Quarz. Shared-Code muss auf 26.2 kompilieren; 26.3-only in `mc26_3/overlay/java` mit Zwilling.
- Vanilla 26.3 (dekompiliert nach `%TEMP%\mcsrc263full`): `BlockEntityType#isValid` überschreibbar; Fluid-Modelle über
  `FluidStateModelSet` (Fabric: `FluidRenderingRegistry.register(fluid, flowing, FluidModel.Unbaked)`), NeoForge verlangt
  `Fluid#getFluidType` für Mod-Flüssigkeiten. 26.2 hat dieselben Fluid-Klassen.

## Entscheidungen (selbst getroffen, Begründung kurz)
1. **SB ↔ simplelib nur über `com.simplelib.api`** (Regel 6a). API wird erweitert: Tiegel/Fass-Fabrik für Partner-Stufen,
   Bau-/Anbring-Schlag, Werkzeug-Abfrage (`registerToolUse`), Kessel-Interaktionen. BE-Typen der Bibliothek akzeptieren
   jeden `CrucibleBlock`/`CrucibleBarrelBlock` (Ladereihenfolge egal) → Enderit-Tiegel/-Fass nutzen BE, Menü, Renderer
   der Bibliothek.
2. **SB-Code, der simplelib-Klassen braucht**, liegt in einer Overlay-Klasse `com.simplebuilding.crucible.CrucibleCompat`
   (26.3) mit 26.2-Zwilling (No-op). Flag `McVersion.CRUCIBLE` (26.3 true, 26.2 false).
3. **Seelen-Lava, Eimer, Seelenbrand** liegen in `common/src/shared` (nur Vanilla-API, kompiliert auf 26.2), registriert nur
   bei `McVersion.CRUCIBLE`. Loader-Teile (NeoForge/Forge `FluidType`, Fabric-Fluidmodell) im jeweiligen Loader-Baum über
   `PlatformServices`-Fabrik.
4. Seelen-Lava steht im Tag `minecraft:lava` (Vanilla-Physik: Schwimmen, Pfadsuche meidet, Strider, Items verbrennen).
   Abweichung von Plan §18 (dort nur Arbeitsannahme, keine Besitzerfrage): ohne den Tag wäre die Flüssigkeit auf Fabric
   begehbar wie Luft. Eigenes Verhalten (Brand ×2, Seelenbrand, Wasserseite, Zündung, Unersetzbarkeit) überschreibt.
   Nebel bleibt Lava-Orange (türkiser Nebel offen, Client).
5. Fließweite über Abfall 1 + Mindest-Stand: Overworld 2, Nether 5 Blöcke (Config 1..4/1..7), Takt 45/20 Ticks.
6. Wasserkontakt: Wasserquelle → Quarzblock, fließendes Wasser → Schwarzstein (Besitzer 54); Seelen-Lava bleibt.
7. Verstärkter Kessel **in simplelib** (Besitzer 56 B: ohne SB Axt + 4 Diamanten): leer/Wasser/Lava/Pulverschnee; SB fügt
   `simplebuilding:reinforced_soul_lava_cauldron` + Hammer-Weg (4 Rissige Diamanten) hinzu. Ohne SB = Axt + 4 Diamanten.
8. Kupfer-Eimer: Komponente `simplebuilding:oxidation` (0–3) + `simplebuilding:waxed`; Item-Modell `select` auf die
   Komponente. Oxidiert beim Ausgießen um 1, Axt schabt 1 ab (bzw. entwachst), Honigwabe wachst. Wasser ausgießen setzt nur
   fließendes Wasser (Stand 7, keine Quelle). Lava ausgießen: Eimer zerbricht. Keine Seelen-Lava.
   Enderit-Eimer: Werkbank (8 Enderit-Nuggets um Eisen-Eimer), unzerbrechlich, nimmt Wasser/Lava/Seelen-Lava.
   Eisen-Eimer nimmt Seelen-Lava (`simplebuilding:soul_lava_bucket`), Ausgießen → Eimer weg.
9. Brennstoff: Seelen-Lava-Eimer 200 000 Ticks (10× Lava, Config), Eisen: kein Rest, Enderit: Eimer zurück; Kupfer-Lava-Eimer
   20 000, kein Rest.
10. Griffe mit SB: Eisenstab zusätzlich im Tag `simplelib:crucible_handles` (Eisenbarren bleibt gültig – Tags sind additiv).
11. Weltgen: Mixin auf `SpringFeature.place` (Nether, Lava, 0,5 %) und auf die Festungs-Eingangshalle (Lavabrunnen, 10 %,
    deterministisch je Stück).
12. Glow (P5d): warme Items in der Hand und gedroppt – Render-Mixin im Client (Item-Renderzustand, Glow-Outline-Farbe).

## Dateien (Hauptteile)
- simplelib: `api/SimpleLibApi.java`, `crucible/CrucibleBlock.java`, `crucible/CrucibleBarrelBlock.java`, Loader-Einstiege
  (BE-Typ `isValid`), neu `cauldron/ReinforcedCauldron*.java`, Ressourcen/Tags/Lang/Wiki.
- SB: `mc26_3/overlay/java/com/simplebuilding/crucible/CrucibleCompat.java` (+ Zwilling), `util/SledgehammerUpgrades.java`,
  `items/custom/SledgehammerItem.java` (Bau/Anbringen/Quarz), neu `fluid/*` (Seelen-Lava), `items/custom/*Bucket*.java`,
  `effect/ModEffects.java` (Seelenbrand), Mixins (Weltgen, Eisen-Eimer, Brennstoff), Datagen (Rezepte, Tags, Modelle, Loot),
  Loader (Fabric/NeoForge/Forge Fluid-Client/FluidType), GameTests.

## Risiken
- Fluid über drei Loader (NeoForge/Forge `FluidType`); Forge 26.3 nur Compile geprüft.
- Unersetzbarer Fließblock als Falle → kurze Fließweite, Claims über `WorldPermissions`.
- Mixins auf Weltgen-Klassen versionsempfindlich (nur 26.3 geprüft).

## Verifikation
GameTests (SB `crucible_*`, `soul_lava_*`, `*_bucket_*`; simplelib bestehende + Werkzeug-Weg), Gates: module-simplelib-*,
module-simplesandwiches-*, fabric-263 + neoforge-263 („alles gruen“), Compile 26.2 + Forge 26.3, `gradlew check -q`,
Datagen 26.3, Wiki `--all` + `--all --check`. Kein Client (Optik/Glow/Fluid-Rendering ungetestet → offen).
