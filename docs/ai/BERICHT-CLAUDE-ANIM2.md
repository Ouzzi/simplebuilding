# Bericht claude-anim2 (Konzept Mob-Animationen, Schritt 3 M-Aufgaben, ohne Howler/Watcher)

Nur Client-/Modellcode plus ein Client-Zähler in `DreadEntity`; Cubes, UV, Texturen unveraendert. Basis: oc-anim-quick gemerged.
- **Dread:** Ketten-Drag (`chainDrag`: Kette hinkt dem Arm 3 Ticks nach, schwingt nach dem Schlag als gedaempftes Pendel `FearLogic.chainPendulum` aus und setzt sich in ca. 12 Ticks), Schritt-Gewicht (Koerper-Y-Dip bei doppelter Schrittfrequenz, Kette schleift phasenversetzt), Todes-Kollaps (Knie, Kopf, Arme, `deathTime`).
- **Deceiver:** eigener Fluchtlauf ab Walk-Speed ca. 0,5 (Vorlehnen, Arme zurueck, Mantel weht), Mantel schwingt gegen Drehung (`turn` aus `yBodyRot`), Todes-Zusammensinken. Aufgesetzt nach den Keyframes.
- Neuer Test `fear_chain_pendulum` (+ Wrapper `fearChainPendulum`).
Commit: 6c6049a1c (+ Bericht). Tests: Compile simplemobs 3 Loader EXIT=0 (Root-Compile-Lauf kollidierte mit Duplikat-Job, Gradle-State-Fehler in simpletrims, kein Javafehler); `fear*` fabric: alles gruen 23/23 (inkl. fear_chain_pendulum). wiki --check: nur vorbestehende Sandwich-Textur-Kopien.
Abweichungen: Nibblemite/Dragonmite-Todeszustaende und Nibble-Kauzyklus nicht gemacht (anderes Modul, S-M, zurueckgestellt); Deceiver-Idle-One-Shots (Keyframes) nicht gemacht; Dread-Idle-Kettenrasseln nicht gemacht; kein `AnimUtil` (nur 1 Modul).
Nicht getestet: Bewegungsqualitaet/Client-Screenshot (nur auf Dev-Instanz sinnvoll beurteilbar).
