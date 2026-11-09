# Plan A Weather11 (W9 Wetter, W10 Baby-Namensschild), 09.11.2026
Branch a-weather11, Basis claude-wave1, nur 26.3 Fabric. Queue IG31-62, IG31-63.

## W9 Wetter
- Simple Weather: `rainFrequencyFactor`, `thunderFrequencyFactor` (0,25–4, Standard 1 = Vanilla). `ServerWeatherMixin` (TAIL von `advanceWeatherCycle`) verschiebt die Trockenzeit-Zähler (`rainTime`/`thunderTime`, nur wenn gerade nicht Regen/Gewitter, nicht bei `clearWeatherTime > 0`) um `SimpleWeather.shift(gameTime, factor)` (zustandslos, Summe über n Ticks = n·(f−1)). Dauer bleibt Vanilla. Befehl: `/simpleweather config set …` (ConfigCommands).
- Simple Tweaks: Wetter-Zeitplan in `play.events` (`weatherScheduleEnabled` aus, `weatherClock` real|game, `weatherSchedule`): `WeatherSchedule` (Zeile `SAT 20:00 thunder 30 [event]`, `D3` = jeden 3. Ingame-Tag), `WeatherPlan.tick` setzt per `MinecraftServer.setWeatherParameters`, startet optional ein ServerEvents-Event (double_xp/more_loot/blood_moon). Befehle `/tweaks weather now|schedule`. Unabhängig: kein Simple-Weather-Code; bei dessen `disableWeather` (Datei, wie Votes) nur das Event.
- „Events“ in Tweaks = `play/events/ServerEvents` (T21); Challenges nicht angefasst.
- Tests: `simpleweather:frequency_factor`, `simpletweaks:play…weather_schedule_sets_weather` (+ Config-Grenzen in `config_defaults_and_bounds`).

## W10 Mob Manners
- Vanilla 26.3: goldener Löwenzahn (Age-Lock). Eigene Mechanik war ein Namenssuffix (kein Item) → Flag `SimpleMobManners.BABY_NAMETAG_LEGACY = false` schaltet den Mixin ab; Ports 26.2/1.21.11 setzen `true`. Port-Notiz in `PRINZIPIEN-VANILLA-RUECKPORTS.md`. Keine Item-Migration nötig.
- Test `simplemobmanners … baby` prüft: Suffix wirkt nicht, Löwenzahn möglich, kein eigenes Item.

## Rückfrage
Keine. Zeitplan-Kombination mit „Events“ = optionales fünftes Feld pro Zeile.
