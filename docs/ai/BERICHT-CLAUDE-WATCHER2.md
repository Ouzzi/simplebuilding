# Bericht claude-watcher2 (Hollow Watcher, Nachtrag 63)

- Branch `claude-watcher2` (Basis claude-wave1 a84e28a3f), nur nach /root/sb.git gepusht; Merge und Prod offen.
- Umgesetzt: Blickfeld-Zonen nach Winkel (Config `watcherGazeStare/Slow/Edge` 10/25/35, skaliert mit Client-FOV 30..110, neues Paket `simplemobs:fov`, 3 Loader); Ausweichen im Bogen (SLOW langsam, EDGE schnell mit Puffer), dann hinter den Spieler; Treffer: Teleport hinter ein neues Opfer oder fern, Opfer gewichtet zufällig; Mehrspieler-Freeze (irgendein Spieler); Boot: Pirsch-Teleport aus, Treffer löst ihn aus dem Boot.
- Client: Arme über den Schultern hinter dem lokalen Spieler, Hände 1,7x skalierte Teilquader (vorhandene Arm-Textur), weiches Ausblenden des Zitterns (calm); Shivering V Zielwackeln 4,2 statt 3,0 Grad.
- Konzept: docs/ai/KONZEPT-WATCHER-BLICKFELD-MEHRSPIELER-2026-10-15.md. "Hollow Howler" im Boot-Diktat = Watcher (angenommen).
- Abweichung: Hand-Quader (neue Cubes) trotz Hinweis "keine neuen Cubes" im Animationskonzept, da Besitzer "Hände markanter" will; keine Textur geändert.
- Tests: Compile 3 Loader grün. Neue GameTests fear_watcher_zones/_zone_logic/_multi_freeze/_hit_new_victim/_boat; Filter simplemobs:*watcher*: Fabric 11/11, NeoForge 11/11 grün (alles gruen 22/22).
- Nicht getestet: Client-Optik (Arme/Hände), echte FOV-Paketstrecke Client-Server, Forge-Laufzeit.
- Wiki-Check meldet Sandwich-Texturkopien (nicht aus diesem Branch).
