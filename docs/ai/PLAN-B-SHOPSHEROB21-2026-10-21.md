# Plan b-shopsherob21 (21.10.)

Basis claude-wave1 586327b29. Zwei Queue-Zeilen (B, b-shopsherob21, 21.10.).

## 1. Simple Shops: Karte zeigt entladene öffentliche Shops (KONZ20 Stufe 4, Konzept §7.4)
Ist: `ShopMapLayer.collect` liest nur Blockentities geladener Chunks. Das persistente Register gibt es schon:
`logic/EnderShops` (SavedData `simpleshops:ender_shops`, Overworld-Speicher) hält je Shop-Block Dimension, Position,
Besitzer, Preise, Ware, öffentlich/privat und die Verknüpfung Verkaufsblock→Vitrine. Kein zweites Register bauen.
Änderung (nur `ShopMapLayer.java`):
- geladene Chunks wie bisher (alle aktiven Shops, Ist-Verhalten bleibt);
- danach jeder SALES-Eintrag aus `EnderShops` derselben Dimension im Rechteck, dessen Chunk **nicht** geladen ist
  (`getChunkNow == null`), nur wenn öffentlich (`listed`), Preis gesetzt, Vitrine verknüpft (`caseOf`), Vitrine mit Ware,
  gleiche Shop-Art (Server/Spieler) – dieselben Regeln wie der Katalog des Handelskontors. Lädt nie einen Chunk.
- gleiche Marker-Zeilen, Obergrenze MAX bleibt.
Test: `ShopsTests`/`TerminalTests` neuer Gametest `map_shows_unloaded_public_shops` (Eintrag in fernem, entladenem Chunk:
öffentlich → Marker, privat → keiner, ohne Ware → keiner, Chunk bleibt entladen); Fabric-Adapter `ModuleGameTest` ergänzen
(NeoForge/Forge registrieren `ShopsTests.ALL` automatisch).

## 2. Simple Mobheads Herobrine
a) Abklingzeiten persistent: neue SavedData `ritual/RitualCooldowns` (`simplemobheads:herobrine_ritual`, Overworld-Speicher,
   Codec mit Listenlimit) statt der zwei ConcurrentHashMaps; Spielzeit (`getGameTime`, weltweit gleich) als Uhr; abgelaufene
   Einträge werden beim Schreiben entfernt (Größe beschränkt). `resetForTests` leert die SavedData.
   Test: im bestehenden `herobrineRitual`-Test Rundreise über den Codec (encode → decode → Abklingzeit gilt weiter).
b) Wiki-Spoiler: Muster = versteckte Inhalte (`SECRET_ITEMS`, Spoiler-Abschnitt in docs): Rezept/Schrein nicht offen zeigen.
   Neues Feld `"spoiler": true` in `modules/simplemobheads/wiki/manual.json` (Feature `herobrine` + exakte Item-Notiz
   `herobrine_head`, die heute fälschlich „geladener Creeper“ sagt); Zusammenfassung = Andeutung ohne Lösung, Details hinter
   einem standardmäßig zugeklappten `<details class="spoiler">` in `wiki/index.html` (Feature-Seite + noteBlock).
   „Erst nach erstem Fund aufklappbar“ kann eine statische Webseite nicht wissen → Entscheidung: Aufklappen durch den Leser.
   Regeneration (wiki --all, Guides, Discover) auf sb-test.

## Verifikation
Compile 3 Loader; run.py module-simpleshops-{fabric,neoforge,standalone-fabric}-263 und module-simplemobheads-{fabric,neoforge,
standalone-fabric}-263 (gezielte Filter); fabric-263/neoforge-263 mit Filter auf die Module; Generatoren + `./gradlew check -q`.
Nicht: Client-Sicht der Karte, Forge-Laufzeit (nur Compile).
