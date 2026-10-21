# Plan b-jukebox264 (21.10.): Jukebox-Textur aus 26.4 Snapshot 4 für 26.3 / 26.2 / 1.21.11

Auftrag (Besitzer 21.10., Queue „(B, b-jukebox264, 21.10.)“): „bitte auch neue jukebox texture mit einbauen für alle
aktuellen versionen (aus 26.4 snapshot)“. Regel A8 / AGENTS.md 7: Mojang-Pixel nie einchecken.

## Ist-Zustand (erhoben)

- Mojang-Versionsmanifest (piston-meta, 21.10.): neuester Snapshot `26.4-snapshot-5`. Verglichen (Hash je Datei):
  26.3 = snapshot-3 (alte Jukebox); **snapshot-4 ändert** `jukebox_side.png`, `jukebox_top.png`, **neu**
  `jukebox_bottom.png` und das Modell `block/jukebox.json` (Eltern `cube_top` → `cube_bottom_top`, Unterseite eigene
  Textur); snapshot-5 ist dort byte-gleich mit snapshot-4. Blockstate und Item-Definition unverändert (Item nutzt das
  Blockmodell). → **Pin: `26.4-snapshot-4`** (erste Version mit der neuen Textur = Wiki-Eintrag „Snapshot 4“, schon eine
  Woche alt, Inhalt durch snapshot-5 bestätigt). Gepinnt werden Versions-JSON-URL + SHA-1, Client-Jar-SHA-1 und die
  SHA-1 der vier Dateien (Hashes sind keine Pixel).
- `tools/textures/vanilla_derived.py` liest nur das **lokale 26.3-Jar** (Loom/NeoForm/ForgeGradle-Cache) und läuft nur
  in der 26.3-Linie (`mc26_3/resources.gradle`); 26.2 und 1.21.11 haben keinen Vanilla-derived-Schritt. Ein reines
  Kopieren aus einem anderen Jar braucht weder Pillow noch numpy.
- Module gibt es nur in der 26.3-Linie (`modules/modules.json`, settings.gradle). Auf **allen drei Linien** existiert nur
  `simplebuilding` selbst (26.2: Root/`:neoforge`/`:forge`; 1.21.11: `:mc1_21_11:fabric|neoforge`; 26.3:
  `:mc26_3:fabric|neoforge|forge`). → Ort: **simplebuilding**. Simple Music (nur 26.3) und dessen Generator bleiben
  unberührt.
- Optik-Schalter der Familie: Kategorie `visuals` in `SimplebuildingConfig` (z. B. `vanillaEnchantedBookTextures`,
  Client, Standard an), gelistet in `ConfigOptions.CLIENT_SIDE`, `ConfigOptionTests`, Lang en/de. → Schalter wird
  gebaut.
- Atlas-API ist auf 26.2, 26.3 und 1.21.11 gleich (`DirectoryLister#run(ResourceManager, SpriteSource.Output)`,
  `Output#add(Identifier, Resource)` ersetzt per `Map.put`) – geprüft per javap an den Cache-Jars.

## Entwurf

1. **Gradle `gradle/jukebox-264.gradle`** (neu), angewandt von den 8 simplebuilding-Loaderprojekten der drei Linien,
   **nicht** von `mc26_4/*` (Vanilla-Rückport-Regel: 26.4 hat es selbst). Task `generateJukebox264`:
   - Cache `GRADLE_USER_HOME/caches/simplebuilding/jukebox264/26.4-snapshot-4/` (nur die 4 Dateien, Hash geprüft);
   - fehlt/kaputt und nicht `--offline`: Versions-JSON von piston-meta (SHA-1 prüfen) → Client-URL + SHA-1 (= Pin),
     Jar in den Cache-Ordner laden, SHA-1 prüfen, die 4 Einträge entnehmen und je SHA-1 prüfen, Jar wieder löschen;
   - Ausgabe `build/generated-resources/jukebox264` (nie im Repo): `assets/simplebuilding/textures/jukebox_264/
     jukebox_{side,top,bottom}.png` + `assets/minecraft/models/block/jukebox.json` (das 26.4-Modell); über
     `processResources.from(task)` in jedes Mod-Jar;
   - Offline/Netzfehler/Hash falsch: **WARNING + gar nichts ausliefern** (alles oder nichts → Vanilla-Jukebox), Build
     läuft weiter; `-PsbVanillaDerivedStrict` bricht ab, `-PsbSkipJukebox264` lässt es ohne Netz weg.
   - `checkJukebox264` (in `check`): verarbeitete Ressourcen enthalten alle vier Dateien oder keine; jede vorhandene hat
     den gepinnten Hash.
2. **Client-Mixin `JukeboxSnapshotSpritesMixin`** (common/src/shared + mc1_21_11/shared, je in
   `simplebuilding.client.mixins.json`): am Ende von `DirectoryLister#run` für die Quelle `block`:
   - eigene Texturen fehlen (Offline-Build, 26.4-Linie) → nichts;
   - Schalter an und `minecraft:textures/block/jukebox_side.png` kommt aus dem Vanilla-Paket → Sprites
     `minecraft:block/jukebox_{side,top,bottom}` = die 26.4-Dateien (Ressourcenpakete, die die Jukebox ändern, gewinnen
     damit weiter);
   - sonst (Schalter aus bzw. Paket-Jukebox) und kein Paket liefert `jukebox_bottom` → `minecraft:block/jukebox_bottom` =
     aktuelle `jukebox_side` (sieht dann aus wie das alte `cube_top`-Modell).
   Darum liegen die PNGs im eigenen Namensraum außerhalb von `textures/block` (keine Extra-Sprites, kein stilles
   Überschreiben) statt direkt unter `minecraft:textures/block/` – Abweichung vom Wortlaut „Resource-Override“, weil
   ein reiner Datei-Override nicht abschaltbar wäre. Das Modell ist ein echter Override (wirkt erst nach Reload).
3. **Schalter `jukebox264Textures`** (Kategorie `visuals`, Client, Standard an) in beiden Config-Klassen,
   `ConfigOptions.CLIENT_SIDE`, `ConfigOptionTests`, Lang en/de (src/main, mc26_3/overlay, mc1_21_11/fabric).
   Wirkt nach Ressourcen-Neuladen (F3+T) – steht im Tooltip.
4. `.gitignore`: Ausgabe liegt unter `build/` (schon ignoriert); zusätzlich Muster für die drei PNG-Namen im Quellbaum,
   damit niemand sie versehentlich eincheckt. AGENTS.md 7 / PRINZIPIEN A8: ein Satz zum Snapshot-Jar.

## Risiken

- Netz beim Build: erster Build lädt ~42 MB einmalig; danach Cache (4 kleine Dateien). Offline-Gate (`--offline`) nutzt
  nur den Cache und bricht nie.
- `defaultRequire 1`: Mixin-Ziel muss auf allen Linien passen (javap geprüft); Compile aller Loader auf sb-test.
- Fremde Mods/Pakete mit eigenem Jukebox-Modell: deren Modell gewinnt (Paketreihenfolge), unser Sprite ist dann
  harmlos.

## Verifikation (sb-test)

- `processResources` für 26.3 (Fabric/NeoForge/Forge), 26.2 (Root/NeoForge), 1.21.11 (Fabric/NeoForge) und
  Jar-/Ressourceninhalt: vier Dateien da, SHA-1 = Snapshot-4-Dateien; `--offline` mit leerem Cache → keine Datei,
  Build grün; `git status` sauber.
- Compile aller Loader, `checkJukebox264`, data_integrity (Filter), `./gradlew check -q`.
- Nicht: Client-Läufe (Sicht im Spiel ungeprüft, ehrlich melden).
