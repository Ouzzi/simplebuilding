# Plan b-claimdeed45: Claim-Urkunde Phasen 4 und 5 (17.10.2026)

Grundlage: `PLAN-CLAIM-URKUNDE-GUI-2026-10-13.md` (Fassung 2 freigegeben). Phasen 1-3 (Rechte-Code, Editor, Kopie, Claim-Tisch) sind in claude-wave1.
Ist-Zustand: Kopie auf leere Urkunde (`DeedCloningRecipe`) und Claim-Tisch (Karte + Liste) existieren; es gibt noch kein Budget, keine Erweiterung, keine Claims auf der Karte von Simple Maps.

## Entscheidungen (Abweichungen vom Plan benannt)
1. **Maps-Ebene ueber das vorhandene Framework `MapLayerSources`** statt neuem `SimpleLibApi.registerClaimView`. Die Ebene-Schalter, die Anfrage-/Antwort-Pakete, Rate-Grenze und "nur erkundete Kacheln" von Simple Maps gibt es schon; keine neue Paketart, kein simplelib-Umbau. Simple Tweaks registriert die Quelle `claims` (Framework, kein Import von Simple Maps); Simple Maps kennt Tweaks nicht. Erweiterungen: `ViewerCollector` (Sicht je Spieler, damit `claimVisibility` gilt), Marker-`kind` auf der Leitung, Flaechen-Kind `area:RRGGBB:M:W` (Streifen von W Chunks einer Zeile, gleiche Besitzerfarbe wie am Tisch).
2. **Erweitern ohne Menue-Slots**: der Claim-Tisch hat keinen Container (Phase 3). Knopf "Land erweitern" am Tisch sendet `TableRequestPayload.EXPAND`; der Server prueft Sitzung/Reichweite/Rate, eigene (oder leere) Urkunde im Inventar, Material, XP-Level, Abklingzeit. Erst Budget speichern, dann Material/Level nehmen (Speicherfehler = nichts genommen).
3. **Budget** im Ledger (`budgets[]`, additiv in v2, fehlt = Startgroesse). Wirksam: `min(maxClaimsPerPlayer, max(gespeichert|start, bereits gehalten))`; Erweiterung aus (`expansionEnabled=false`) = altes Verhalten (Budget = `maxClaimsPerPlayer`). Standard `maxClaimsPerPlayer` 64 (nur neue Configs), `startChunks` 4, Stufen 6/8/12/16/24/32/48/64 laut Plan. Befehle `/claim budget`, `/claim admin budget <Spieler> <n>`.
4. Texturen: keine neuen; Vorschau Claim-Tisch liegt unter `docs/ai/previews/claude-claimdeed3/`. Abnahme durch den Besitzer bleibt offen.

## Dateien
framework `MapLayerSources`; simplemaps `LayerDataPayload`/`LayerService`/`MapLayers`; simpletweaks `ClaimConfig`, `ClaimStore`, `Claims`, `ClaimCommands`, `table/ClaimMapLayer`, `TableService`, `TableOpenPayload`, `TableRequestPayload`, `TableScreen`, `TableLayout`, Lang EN/DE, Wiki `manual.json`; Tests `ExpansionTests`.
Phase-1-Stellen (fuer b-claimreview): `Claims.create` (Budgetpruefung), `ClaimStore.replace` (3-Arg-Ueberladung, `budgets`), `ClaimConfig` (neue Felder).

## Verifikation
Compile 3 Loader + `:compileJava`; JUnit `TableLayoutTest`; Gametests `claims_expand*`, `claims_map_layer` plus bestehende `simpletweaks:claims*` auf Fabric/NeoForge/Forge; Modul-Targets simpletweaks und simplemaps (inkl. standalone).
