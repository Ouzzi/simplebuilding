# Plan a-claims-maps: Claims-Umbau Branch 4 – Claims auf der Wayfinder-Karte (SP22-8, 23.10.2026)

Basis: `a-claims-credits` (Branch 1) + `claude-wave1`. Konzept `docs/ai/KONZEPT-CLAIMS-UMBAU-2026-10-22.md` §7, §8 Zeile 4.
Gilt nur MC 26.3 Fabric (Regel 22.10.). **Nicht** in diesem Branch: Urkunden-UI (`a-claims-deedui`, zeigt den Hinweis
„Simple Maps empfohlen“ an – hier nur Flag + API), Kartentisch/Tisch-Abbau (`a-claims-carto`).

**C1–C13 vorläufig nach Empfehlung.** Hier relevant: C10 (kein Code-Import aus Simple Maps), C12 (Hinweis abschaltbar –
Sache der Urkunden-UI).

## Ist (erhoben)
- `table/ClaimMapLayer` (ViewerCollector bei `framework.api.MapLayerSources`, registriert in `Claims.start`, wenn Claims an)
  liefert schon eigene + fremde Claims der Karten-Dimension als Streifen, Namen nach `claimVisibility`
  (`TableVisibility.showsName`), Op sieht alles. Dimension = `level.dimension()` der Karte, also jede Dimension.
- Simple Maps fragt die Quelle nur für die Dimension der Karte und nur in erkundeten Kacheln (`net/LayerService`);
  Marker-Zeilen sind reiner Text (`Component.literal`, ≤ 6 × 80 Zeichen).
- Tweaks: `play/Mods.loaded(id)` (Fabric-Adapter setzt `FabricLoader::isModLoaded`), `SubMods.enabled(id)`.
- `modules.json`/`fabric.mod.json` Tweaks: `simplemaps` fehlt in `optional`/`suggests`. Modul-Testziele von Tweaks laden
  Simple Maps nicht → „mit Maps“ ist bisher nicht testbar.

## Änderungen
1. `claims/ClaimMaps.java` (neu): `present()` = `Mods.loaded("simplemaps") && SubMods.enabled("simplemaps")` – Server-Flag
   `mapsPresent` für die Urkunden-UI (deedui ruft `ClaimMaps.present()` beim Bau von `DeedOpenPayload`). Kein Import
   aus Simple Maps (Regel 3/4).
2. `table/ClaimMapLayer`: eigener Streifen trägt die Zeile „Your land · N credits“ (Credits des Betrachters, nur eigene;
   Konzept §8 Zeile 4), fremde wie bisher „Claim of X“ / „Foreign claim“. Datei bleibt in `table/` (carto verschiebt
   `TableVisibility`/`TableService`-Helfer; kleinste Überschneidung).
3. `modules.json` Tweaks: `optional` + `simplemaps`; `fabric.mod.json` `suggests` + `simplemaps` (Regel 7); dazu nur die
   Metadaten `neoforge.mods.toml`/`mods.toml` (optional), weil `check` (`tools/multimod.py`) alle Loader vergleicht – kein Code. Neue Combo
   `module-simpletweaks-with-simplemaps-fabric-263` (Standalone + Simple Maps), damit das Flag „mit Maps“ und die echte
   Quellen-Registrierung testbar sind.
4. Wiki Tweaks (neuer Eintrag „Claims auf der Karte“: alle Dimensionen, eigene mit Credits, Namen nach
   `claimVisibility`, Simple Maps empfohlen) und Maps (`claims_layer`: alle Dimensionen, Credits-Zeile). Generatoren.

Abweichung vom Konzept §8 Zeile 4: „eigene Gruppen-Umrisse“ (Zeichnung in Simple Maps) **nicht** in diesem Branch –
reine Optik im Maps-Client, Regel „UI/Optik erst exemplarisch“; eigenes Land ist heute schon umrahmt (Marker `M=1`).

## Tests (Gametests, Fabric-Adapter)
Tweaks (`claims/MapLayerTests.java`, Adapter `ClaimsGameTest.java`):
- `claims_maps_dimensions`: Claims in Overworld, Nether, End; je Karten-Dimension nur deren Claims, eigene + fremde;
  Besitzer-Zeile „Your land · N credits“ mit echten Credits.
- `claims_maps_visibility`: fremde Namen je `claimVisibility` (all / owners_hidden / own / off), Mitglied sieht Namen bei
  owners_hidden; Op-Sicht unverändert (schon durch `claims_map_layer`/Tisch-Tests belegt).
- `claims_maps_present`: Flag = echter Zustand (Simple-Maps-Item registriert && `SubMods.enabled`); Schalter
  `Mods.check` ohne/mit Maps; mit Maps ist die Quelle `claims` registriert.
Maps (`MapsTests`, Adapter `ModuleGameTest.java`):
- `claims_layer_absent`: ohne Simple Tweaks keine Quelle `claims`.

## Verifikation (nur über `/root/sbt a-claims-maps`)
Compile `:mc26_3:fabric:compileJava` + `:modules:simpletweaks:fabric:compileJava` + `:modules:simplemaps:fabric:compileJava`;
Testrunner `module-simpletweaks-fabric-263`, `-standalone-fabric-263`, `-with-simplemaps-fabric-263` (Filter
`simpletweaks:*claims_map*`), `module-simplemaps-fabric-263` + `-standalone-fabric-263`; Generatoren + `./gradlew check -q`.
Nicht getestet: Client (keine Client-Läufe), Custom-Dimension mit echter Karte (keine im Testserver; Code ist
dimensionsneutral), andere Loader/Versionen (Regel).
