# Plan b-revrest24 (24.10.2026): Rest-Funde der Code-Review (REV24-F-10/11/24/31, F-14 nur Pruefung)

Branch `b-revrest24`, Basis claude-wave1 (dda4cd77a). Nur 26.3 Fabric.

## Ist-Zustand und Entscheidung je Fund

- **F-10 bestaetigt.** `TrimEffectUtil.getTrimCount/getMaterialCount/patternWeight` nutzten `String.contains` auf dem Asset-Pfad
  (Namensraum verworfen). Fix: exakter Vergleich `Identifier.getPath().equals(name)` plus Namensraum `minecraft` oder `simple*`
  (`isOwn`). Fremde Muster/Materialien (oddeye, smoky_quartz, someothermod:rib) zaehlen nicht mehr. `patternWeight` exakt
  (`enderite`/`netherite`). Der Test `TrimEffectTests` pinnte die alte Schwaeche ("astral" findet "astralit",
  someothermod:rib_cage zaehlt als rib) und wird auf das neue Verhalten umgestellt. Bewusst NICHT geaendert:
  `TrimBonusCatalog.forPattern/forMaterial` (Tooltip, `contains`) und `AdvancementChecks` -- reine Anzeige/Erfolge, anderer
  Umfang; Tooltip kann bei fremden Mustern daher noch einen Bonus zeigen, der nicht wirkt (offen, Memory).
- **F-11 bestaetigt.** `TrimPulseTextures` (SB-Fassung `client/render`, Modul-Fassung `modules/simpletrims/.../client`) haelt
  `CACHE` mit `NativeImage` und registrierten `DynamicTexture`s ohne Reload-Hook. Fix ohne Loader-Hook: der Cache merkt sich den
  `ResourceManager`, mit dem er gefuellt wurde; ein Reload (F3+T) erzeugt einen neuen Manager, dann werden Bilder geschlossen,
  Texturen per `TextureManager.release` abgemeldet und der Cache (inkl. `MISSING`) geleert. Loader-neutral, kein neuer Entrypoint.
- **F-24 teilweise verworfen.** Die Behauptung "jede der 4.096 Positionen je Abschnitt" stimmt nur eingeschraenkt: der Code
  filtert schon Abschnitte per `maybeHas` und beschneidet auf den Kugel-Quader. Echter Rest: je Position `new BlockPos` und
  `distSqr` vor dem Blocktest. Fix: Blockzustand zuerst, `BlockPos` nur fuer Treffer; Ergebnis/Reihenfolge identisch. Der
  globale Cache-Invalidierungszaehler (`GENERATION`) bleibt (Cache je Welt wuerde API/Verhalten aendern, Nutzen klein).
- **F-31 bestaetigt, umgesetzt.** `Protection.PROVIDERS` jetzt `synchronizedMap(IdentityHashMap)` mit unveraenderlichen
  Snapshot-Maps je Server (Registrierung per `compute`, Lesen ohne Sperre, gleiche Reihenfolge/Semantik). Neu
  `Protection.clear(server)`, aufgerufen im `SERVER_STOPPED` von `modules/simpletweaks/fabric/.../ProtectFabric.java` (Abweichung: `Simplebuilding.java` im Root sieht das Framework nicht; Tweaks ist in jedem SB-Jar verschachtelt). Registrierung beim Mod-Init bleibt
  unberuehrt (Provider werden pro Serverstart in `SERVER_STARTED` neu registriert, z. B. `Claims`/`Protect.start`).
- **F-14 (nur Pruefung, nichts geaendert).** Gemessen am gebauten 26.3-Fabric-Jar (sb-test, `simplebuilding-26.3-2.0.0.jar`,
  45,7 MB, davon 25 verschachtelte Modul-Jars mit 25,1 MB): im Haupt-Jar liegen 285 Klassen `com/simplebuilding/gametest`
  (5,07 MB unkomprimiert = 42 % der 11,98 MB SB-Klassen), 51 Klassen `dev/testcentre` (0,51 MB), `PistonEventProbe` und
  `ProtectionProbe`, und `fabric.mod.json` listet 115 `fabric-gametest`-Entrypoints. Die Module bringen zusaetzlich eigene
  `ModuleGameTest`-Entrypoints (z. B. simpletrims). Risiken: Jar-Groesse/Ladezeit (Klassen werden nur bei Gametest-Lauf geladen,
  Laufzeitkosten klein), grosse Angriffsflaeche (Testzentrale-Befehle Stufe 2, Probe-Hooks in jedem Abbau), Versionspflege-
  Rauschen. Vorschlag: (1) Server-Gametests in ein eigenes Sourceset `servertest` (wie das Client-`gametest`-Sourceset), das nur
  Dev-/Test-Runs und das Gate auf den Klassenpfad bekommen; (2) `fabric-gametest`-Eintraege und `/sbtestcentre` nur ausser
  Release-Build (Resource-Filter per Gradle-Property); (3) Probes nur registrieren, wenn `FabricLoader.isDevelopmentEnvironment()`
  oder `-Dfabric-api.gametest`. Aufwand mittel bis gross (Tests referenzieren Mod-Interna, Gate-Runner muss das Sourceset kennen),
  daher nicht in diesem Branch; als eigenes Vorhaben in die Queue.

## Verifikation
Compile `:mc26_3:fabric:compileJava` + `:modules:simpletrims:fabric:compileJava` + `:framework:compileJava`; Tests
`fabric-263` Filter `simplebuilding:trim*`, Modul `module-simpletrims-fabric-263`, `module-simpletweaks-fabric-263` (Claims/Protection),
`./gradlew check`. Nicht testbar ohne Client: Reload-Verhalten F-11 (Client-Sicht).
