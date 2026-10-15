# Bericht oc-anim-quick (Konzept Mob-Animationen, Schritt 1 Quick Wins, ohne Howler/Watcher)

Nur Client-/Modellcode (plus kleine Client-Zähler in den Entities); Cubes, UV, Texturen unverändert.
- **Dread:** Wind-up für Swing/Stomp und Rise per Smoothstep (`FearLogic.ease`); Recovery 6 Ticks (`RECOVER_TICKS`) statt Pop beim Aktionsende (Client-Zähler in `DreadEntity`, Auswertung in `DreadRenderer.pose`).
- **Nibblemite / Dragonmite:** Wackeln = Mischung aus langsamem Atmen (Stand) und Gehwackeln (an `walkAnimationPos` gekoppelt, kein Rutschen); Phasenversatz je Entity-ID (neues `phase` im State, Dragonmite neuer `DragonmiteRenderState`). Dragonmite: Flügel im Stand gefaltet/langsam, Buzz-Ruck pro Mob versetzt, Biss-Lunge (`getSwingAnimation`, nur Segment-Offsets). Keine Abhängigkeit zu simplemobs.
- **Shellker:** Deckel-Zittern 6 Ticks nach dem Öffnen (`clientOpenAge`), eased.
- **Deceiver:** Idle addierte sich zum Walk; Idle blendet jetzt per Speed (0,02..0,15) aus (Rotationen zurückgeblendet, kein Sprung).
- **Soulfire Blaze:** eigene Animationsuhr (`animTime`), läuft geladen bis 2x schneller, weich geglättet (kein Phasensprung).
Commits: siehe `git log oc-anim-quick`.
Tests: Compile 3 Loader + `:modules:{simplemobs,simpleend,simpleenddragonfight}:{fabric,neoforge,forge}:compileJava` ohne Fehler. `--targets module-simplemobs-fabric-263,module-simplemobs-neoforge-263 --filter 'simplemobs:module_game_test_fear*'`: „alles gruen: 34/34 bestanden“ (inkl. neuer Test `fear_anim_ease`); Shellker-Filter fabric „alles gruen: 4/4“. Client-Smoke unter xvfb (`module-simplemobs-client-263`): „alles gruen: 45/45“, Screenshots (Dread swing, Dragonmite) geprüft: rendern ohne Fehler.
Abweichungen: Shellker-Zittern NACH dem Öffnen statt davor (Besitzer N45: Schuss nach 2 Ticks, kein visuelles Verzögern). Dread-Gewicht/Kette-Drag (M) nicht gemacht. Nibble-Kauzyklus und Dragonmite-Angriff nur als Lunge (kein Kauzyklus, S–M, zurückgestellt). Kein neuer `AnimUtil` (nur 1 Nutzer).
Offen/nicht getestet: Bewegungsqualität nur auf Dev-Instanz im Spiel beurteilbar (Frame-Serie nicht erstellt); `wiki/generate.py --all --check --strict` meldet vorbestehende Sandwich-Textur-Kopien (unabhängig von dieser Änderung, keine Wiki-/Lang-Änderung hier). Prod-Übernahme development -> Production bleibt TODO.
