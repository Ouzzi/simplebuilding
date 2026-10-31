# AUDIT Simple Weather (`simpleweather`) – Release-Audit b-rel32-qolb, 31.10.2026

Basis: b-rel32 d9cda1e21, Branch `b-rel32-qolb`. Umfang: 495 Zeilen Java (davon ca. 112 Tests), 3 Mixins (1 Server, 2 Client). Nur MC 26.3 Fabric. Pruefliste C1–C9 aus `docs/ai/RELEASE-QOL-OPT-AES.md`.

## Durchgesehene Dateien (jede Datei vollstaendig gelesen)

Java (Fabric 26.3 + shared; neoforge/forge-Loader-Klassen nicht Teil des Auftrags, nur NUR-Fabric):
- `clienttest/java/com/simplebuilding/modules/simpleweather/ModuleClientSmoke.java` (9 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simpleweather/ModuleGameTest.java` (13 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simpleweather/WeatherFabric.java` (14 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simpleweather/WeatherModMenu.java` (9 Zeilen)
- `shared/java/com/simplebuilding/modules/simpleweather/SimpleWeather.java` (88 Zeilen)
- `shared/java/com/simplebuilding/modules/simpleweather/WeatherConfig.java` (14 Zeilen)
- `shared/java/com/simplebuilding/modules/simpleweather/WeatherScreen.java` (35 Zeilen)
- `shared/java/com/simplebuilding/modules/simpleweather/WeatherTests.java` (112 Zeilen)
- `shared/java/com/simplebuilding/modules/simpleweather/mixin/ServerWeatherMixin.java` (25 Zeilen)
- `shared/java/com/simplebuilding/modules/simpleweather/mixin/client/ClientWeatherMixin.java` (19 Zeilen)
- `shared/java/com/simplebuilding/modules/simpleweather/mixin/client/VisualRainMixin.java` (25 Zeilen)

Ressourcen/Daten (Zeilen):
- `fabric/src/main/resources/fabric.mod.json` (43)
- `shared/resources/assets/simpleweather/icon.layers.json` (100)
- `shared/resources/assets/simpleweather/lang/de_de.json` (8)
- `shared/resources/assets/simpleweather/lang/en_us.json` (8)
- `shared/resources/simpleweather.mixins.json` (15)
- 2 PNG-Dateien (Texturen/Icons, aus `*.layers.json` erzeugt; Pixelgroesse geprueft durch check_mod_icons / check_missing_textures)
- `fabric/build.gradle`, `wiki/manual.json`, `tools/check_data.py`


## Ergebnis C1–C9

- **C1** Alle Klassen gelesen. Server-Teil (`ServerWeatherMixin`) fasst den Wetter-Zustand nur an, wenn es regnet. Client-Teile (`ClientWeatherMixin`, `VisualRainMixin`) liegen im `client`-Abschnitt der Mixin-JSON (auf dem Dedicated Server nie geladen). Keine Befunde.
- **C2** ServerWeather (`ServerLevel.advanceWeatherCycle` HEAD, abbrechbar); ClientWeather (`ClientLevel.tickWeatherEffects` HEAD, duenn mit Zufall); VisualRain (`@Redirect` auf `ClientLevel.getRainLevel(F)` in `WeatherEffectRenderer.extractRenderState`). `check_client_mixins.py` im Testlauf prueft die beiden Client-Mixins gegen 26.3. Konfliktrisiko: `@Redirect` auf `getRainLevel` kollidiert mit anderen Mods, die denselben Aufruf umleiten (z. B. Wetter-/Shader-Mods) – nur eine kann gewinnen; Fehler wuerde beim Start sichtbar (`require=1`).
- **C3** Defaults: Wetter an, Regendichte 20 % (spuerbarer Eingriff; Rückfrage Q-B2). Grenze 0–100. Cloth-Screen, `/simpleweather config`. Lang 8 Schluessel EN=DE.
- **C4** Keine Daten ausser Lang/Icon.
- **C5** Allein spielbar. Keine Fremdzugriffe. depends ohne `fabric-api` (wird ueber das mitgelieferte SimpleLib transitiv verlangt). Cloth Config harte Abhaengigkeit, bei einem reinen Server unnoetig (Q-B1).
- **C6** A12: `disableWeather` ist ein starker Server-Eingriff (aus); Regendichte rein clientseitig (Darstellung), Standard 20 % reduziert Regen fuer alle Spieler deutlich (Rückfrage Q-B2).
- **C7** Kein eigenes Netzwerk. Wetter-Abschaltung wirkt serverseitig und nutzt Vanilla-Wetterpakete; Regendichte nur lokal.
- **C8** Keine Assets ausser Icon; CC0.
- **C9** Keine TODO/FIXME.

## Befunde

| ID | Befund | Fix / Restpunkt |
|---|---|---|
| simpleweather-P3-1 | Standard-Regendichte 20 % (Darstellung, nicht Serverwetter) ist fuer eine frisch installierte Mod ungewoehnlich: Regen faellt „kaum sichtbar“, bis man die Option kennt. | Rückfrage Q-B2 (Besitzerentscheidung, kein Fehler). |

## Test (ein sbt-Lauf fuer alle sechs Mods)

TESTERGEBNIS-PLATZHALTER

## Urteil

**shipbar** (nach gruenem Testlauf). Keine Befunde ausser einer Besitzerfrage. Rückfragen: Q-B1, Q-B2.
