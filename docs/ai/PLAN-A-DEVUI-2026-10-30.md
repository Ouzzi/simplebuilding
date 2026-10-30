# Plan A-DEVUI31 (30.10.): Dev-Varianten-Panel (ABN30-9)

Nur MC 26.3 Fabric. Vorschau: `/root/sb-tools-a/previews/a-devui31-preview.html`.

## Ist-Stand
- UI-Umschalter: `simpleinterfaces` `StyleToggleMixin`/`StyleToggleButton` (Knopf rechts oben neben dem Container, nur bei `StyleToggleClient.development` oder Option `showStyleToggle`). Vorbild fuer Dev-Gating: Simple Earth `EarthClient.devButton()` (AUTO = nur Dev-Umgebung), Widgets per `Screens.getWidgets`.
- Fernrohr: `ScopeOverlay` (ABN29-1) A = 2 px schwarz + Rahmen (Standard), B = + 4 px graue UI-Flaeche (nur Vorschau a-abn29f).
- Client-Dateien in simplelib: `simplelib-client.json` (UiBrightness) - eigene Datei fuer Varianten statt diese zu ueberschreiben.

## Umsetzung
- `com.simplelib.api.DevVariants` (reine Logik, keine Client-Klassen): `register(id, mod, name, default, Option...)`, `get/is/select`, `onChange`, `enabled()`, Datei `config/simplelib-devvariants.json` (`devVariants` optional, `choices`). Alte/ungueltige Auswahl faellt auf Standard, kaputte Datei = Standards.
- `com.simplelib.api.client.ui.DevVariantsPanel` (Widget): Knopf "Dev" neben dem Inventar (4 px rechts vom Panel, neben dem UI-Umschalter-Platz), oeffnet Kasten (`UiBoxes.box`, Palette INVENTORY, Label 4,5:1+) mit je Mod eine Ueberschrift, je Variante Name + Klickboxen; Auswahl wirkt sofort und wird gespeichert. Einhaengen per Fabric `ScreenEvents.AFTER_INIT` (nur `InventoryScreen`), Position ueber `ContainerScreenAccessor`.
- Sichtbarkeit: `devVariants` explizit true/false gewinnt; sonst an in Dev-Umgebung (`isDevelopmentEnvironment`, dort laufen Besitzer-Tests und Client-Smoke), aus im Release. Begruendung: Release-Spieler sollen kein Entwicklerpanel sehen; der Besitzer kann es im Test-Jar per Datei einschalten.
- Anbindung 1: Fernrohr-Rahmen A/B (`ScopeOverlay.registerVariants`, ID `simpleinterfaces:scope_frame`, Standard A).
- Nicht angebunden (kein vorhandenes A/B im Code, nicht "einfach"): Leuchtfeuer-Icons, Schwingen-Look -> offen, jeweils ein `register`-Aufruf plus Abfrage `DevVariants.is`.

## Tests
- Gametest `dev_variants_registry_persistence` (Katalog `LibTests.ALL` + Fabric-Adapter): Standard, Auswahl, ungueltig, Persistenz, veraltete Auswahl, Enable-Regel, kaputte Datei.
- Client-Smoke simplelib: Inventar mit Panel-Knopf, Screenshot, Auswahl (nicht gelaufen; Client-Laeufe verboten).
- Ein sbt-Lauf: Compile + Modul-Targets simplelib/simpleinterfaces + `./gradlew check -q`.

## Offen / Rueckfrage
- Config-Befehl `/simpleclient simplelib set devVariants` nicht gebaut; Datei-Schluessel genuegt.
- Panel scrollt nicht (bei sehr vielen Varianten Hoehe pruefen).
- NeoForge/Forge-Einhaengung fehlt (nur 26.3 Fabric).
