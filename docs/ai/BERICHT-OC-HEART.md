# BERICHT oc-heart (N67 Leuchtherz Hollow Howler)
- Neu: `hollow_howler_heart.png` (32 px, Generator `howler_textures.py` `outputs()`; --check "howler textures current"). Abgenommene Howler-Texturen unverändert (git: nur neue Datei).
- `HowlerRenderer`: eigener `LivingEntityEmissiveLayer` (eyes) mit `state.heartGlow`: Herzschlag-Hüllkurve (min. 0,45, kein Blinken), Takt 40/26/14 Ticks je Phase (Wut am schnellsten), aus beim Tod. Keine neue Logik nötig.
- Tests: "Simple Mobs fabric integration 0 20 20 0", "neoforge 0 20 20 0", "alles gruen: 40/40" (Filter `simplemobs:*howler*`); Client "Simple Mobs client smoke 0 45 45 0, alles gruen: 45/45"; Compile 3 Loader ohne Fehler; wiki --check --strict, discover, generate_textures --check grün.
- Screenshot: docs/ai/previews/oc-heart/howler-heart-ingame.png (Herz auf Rippenplatte sichtbar), plus hollow_howler_heart.png/heart-vergleich.png (8x). check_data.py + wiki textures.js/Sheet-Kopie ergänzt.
- Kontrast (Textur, kein UI-Text): Herz-Kante CYAN #009295 vs. dunkle Rippenlücken 4,6–5,0:1, Füllung CYAN_B #29DFEB 10,7–11,5:1; vs. helle Knochenstreifen nur 1,1–2,5:1 (additiv/leuchtend, im Screenshot lesbar). Bei 45 % Ruhe sinkt der Kontrast entsprechend.
- Nicht getestet: Forge/NeoForge-Client-Screenshot (nur Fabric), Wut-Phase im Client (Logik per Gametest `howler_heartbeat`).
