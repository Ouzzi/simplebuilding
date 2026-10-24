# Plan b-flakywaves24 (24.10.)
Befund: `simpleenddragonfight:module_game_test_half_and_waves` rot im 1. Gate-Lauf (e11eed7ef, standalone fabric-263): "first wave at once: 2 mites (got 1)".
Ursache: `FightSession.spawnSpot` wuerfelt 8 Ringpunkte (3-8 Bl. um das Portal); im Testgebiet (leere 8x8x8-Struktur, Nachbartests, frisch geladene Chunks) sind viele Punkte unbrauchbar (Leere, Fluessigkeit, Kollision). Nach 8 Fehlwuerfen faellt ein Mite der Welle ersatzlos aus -> zufaellig 1 statt 2.
Korrektur: SPAWN_TRIES 8 -> 24 (Fehlwahrscheinlichkeit p^24 statt p^8; im Spiel ebenfalls robuster, Kosten nur bei Fehlwuerfen). Pruefung im Test unveraendert.
Verifikation: standalone- und Modul-Target je 3x gefiltert, danach `./gradlew check`.
