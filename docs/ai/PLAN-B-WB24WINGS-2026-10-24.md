# Plan B: Schwingen Stufe 1–4, Ender-Herz und Ender-Juwel (WB24-2 / WA24-2, 24.10.2026)

Ist: b-wa24wings (master) hat `simplemobs:end_wings` (Stufe 3, x6, 960, ohne Rezept/Loot) gebaut, Optik Stufe 3 = Variante 1
aus `tools/textures/wa24wings_textures.py`; Stufe 1/2 tragen noch Look A aus `elytra24_textures.py`.
Besitzer 24.10.: Stufe 1, 2 und 3 je in Variante 1; Variante 2 von Stufe 3 wird eine neue Stufe 4. Stufe 3 braucht das neue
Ender-Herz, Stufe 4 das neue Ender-Juwel (Optik-Wahl WB24-3 läuft auf b-wb24gem, Empfehlung H1/E1). Herkunft offen (WA24-7).

Entscheidungen (selbst getroffen, Begründung)
- Stufe 4 heißt `simplemobs:void_wings` „Void Wings“ / „Leeren-Schwingen“ (End-Leere; `ender_wings` wäre neben `end_wings` verwechselbar).
- Tempo x8 (`voidWingsSpeedFactor`, = bisherige Obergrenze MAX_FACTOR 8), Haltbarkeit 1152 (+192 je Stufe wie bisher).
- Seltenheit: Stufe 1–3 bleiben CURSED, Stufe 4 und das Ender-Juwel MYTHIC (höchste Stufe), Ender-Herz CURSED wie das Netherherz.
- `ender_heart`/`ender_gem` als einfache Items (nicht setzbar wie Netherherz/-juwel, da ohne Boss-Herkunft) in `WitherWings`,
  Texturen VORLÄUFIG H1 (Leerenkern) / E1 (Perlschliff) aus b-wb24gem (PNG + mcmeta + Ebenen-Quelle) – Besitzer-Wahl offen.
- Kein Loot, kein Rezept für die Materialien: nur Befehl/Kreativ/Testzentrale.

Umsetzung
1. Generator `wa24wings_textures.py`: schreibt Stufe 1–3 Variante 1 und `void_wings` = Stufe 3 Variante 2 (Item, kaputt, getragen,
   je mit `.layers.json`); `elytra24_textures.py` schreibt Stufe 1/2 nicht mehr (nur noch Bibliothek + `--preview`).
2. `WitherWings`: VOID + Asset/Key, ENDER_HEART, ENDER_GEM; `isWings`/`configFactor`; `MobsConfig.voidWingsSpeedFactor`.
3. Rezepte `end_wings_smithing(_without_simplebeacons)` (Nether-Schwingen + Ender-Herz) und `void_wings_smithing(...)` (End-Schwingen
   + Ender-Juwel), Vorlage wie Stufe 2 (Starke Aufwertung mit Simple Beacons, sonst Netherit-Aufwertung).
4. Assets (items/, models/item/, equipment/), Lang EN/DE, Kreativ-Reiter (Modul + Vanilla), Discover, Wiki manual.json, Guide-Seite 19.
5. `check_data.py`: Rezepte aller vier Stufen, keine Loot-Tabellen für end/void/ender_*.
6. Gametests: `wings_items` (Stufe 4: 1152, x8, MYTHIC), `wings_recipes` (Stufe 3 + 4 schmieden), `wings_speed_cap` (x6/x8),
   `wings_void_rocket` (Raketenschub Stufe 4 = 8 x Vanilla).
7. Queue WB24-2 + WA24-2 [x], WA24-7 um Ender-Herz/Ender-Juwel ergänzen.

Verifikation: compile 26.3 Fabric, `module-simplemobs-fabric-263` + `module-simplemobs-standalone-fabric-263` (Filter wings),
Generatoren `--check`, check_data, Guides/Wiki/Discover, `./gradlew check -q`. Keine Client-Läufe (Client-Sicht offen).
