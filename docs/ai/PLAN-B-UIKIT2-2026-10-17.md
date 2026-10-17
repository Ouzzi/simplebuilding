# Plan b-uikit2: Modul-UIs auf simplelib (ST-30 bis ST-35)

Ist (Audit `AUDIT-STIL-MODULE-2026-10-17.md`): ItemStatsScreen, DeathVaultScreen, DownedScreen/RespawnHud mit Handrahmen; ModelsTab handgezeichnet;
StrongBeaconScreen Kontrast ungeprüft; kein Kontrast-Lint. `ModelBrowser` (Models + Visuals) nutzt `UiBoxes.box` schon (N43) - Befund ST-33 war dort veraltet.

Bündelung (Prinzip 6a): simplestats, simpletools, simplerespawn bündeln simplelib wie simplemodels/simplevisuals (Fabric `include`, NeoForge `jarJar`,
Forge `forgeBundleSimplelib`), ohne `requires` in modules.json/Metadaten (gebündelt, Standalone bleibt spielbar).

Änderungen (Layout/Bedienung/Texturen unverändert):
- ItemStatsScreen: `UiBoxes.box` (ohne Schatten, Rahmen 5 px = Layout-PAD) + `insetColored` für die Liste.
- DeathVaultScreen: rechtes Panel `UiBoxes.box`; die linke Vanilla-Truhen-Textur bleibt (Slot-Positionen/Menü hängen daran).
- DownedScreen: dunkelroter `UiBoxes.box` um die Textzeilen; RespawnHud.bar: `UiBoxes.stairFrame`.
- ModelsTab: `UiBookmarks.tab` (identische Form/Farben wie vorher).
- StrongBeaconScreen: Feldfarbe aus beacon.png gemessen (0x212121): LABEL 12,2:1, DIM 4,66:1 -> Farben bleiben.
- ST-35: `tools/check_ui_contrast.py` (Gradle `checkUiContrast`): Text- gegen Face-Konstanten je Client-Datei >= 4,5:1, Textur-Flächen in Tabelle.

Verifikation: Compile 3 Loader + :compileJava, Modul-Targets fabric+neoforge (+ standalone) von stats/tools/respawn/models/visuals, Python-Check.
