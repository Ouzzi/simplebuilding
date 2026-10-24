# Plan b-revcore24 (24.10.2026): mittlere Kern-Funde aus review24/teil1-kern.md

Nur 26.3 Fabric. Jeder Fund zuerst im Code bestätigt, kleinste Änderung, Verhalten sonst unverändert.

| Fund | Befund | Änderung | Test |
| --- | --- | --- | --- |
| F-07 | bestätigt: `mayOccupy`/`mayAutomateEntity` fragen nur Chunk-Ecken | zusätzlich Eck- und Mittelpunkte der Box (bei Automation auch der Entität) über `footprintPoints`; Chunk-Ecken und 256-Chunk-Grenze bleiben | `claimsSmallerThanAChunkAreAskedAtTheRealFootprint` |
| F-08 | bestätigt: je Item und Tick ein Paket an alle im Umkreis | `MagnetItem` zieht über `push()` (Sync-Flag, Vanilla sendet an Tracker), Paketschleife entfernt; ruhende Items bleiben unmarkiert | `heldAttractorMarksMovingItemsForSyncAndLeavesRestingOnesAlone` + bestehende `MagnetTests` (Werte unverändert) |
| F-09 | bestätigt (Schleife ohne `areCompatible`) | in `AnvilScreenHandlerMixin` nur im Hammer+Hammer-Zweig: unverträgliche Paarung überspringen. Kosten bleiben 1 Stufe (Mod-Design, bewusst nicht geändert) | `anvilHammerCombineSkipsIncompatibleEnchantments` |
| F-12 | teilweise: der Frühausstieg fehlt auf 26.3 absichtlich (die Gästebuch-Migration ist inhaltlich, nicht versionsgetrieben; Bücher alter Mod-Stände liegen in gleicher DataVersion). Allokationen sind real | `migrateGuide` prüft zuerst `id.startsWith(NS)`; `Book.topics()` läuft nur noch für Mod-Stapel | bestehender Guide-Migrationstest (`GuideBookTests`) |
| F-15 | bestätigt: 1 MiB je Datei = Paketgrenze | `ModDataTables.sync` wirft die größten Dateien heraus, bis alles unter 900 000 Byte liegt (Log-Warnung), Format unverändert | `dataTablesAreTrimmedToTheSyncPacketLimit` |
| F-16 | bestätigt, aber dokumentierter Kompromiss: Fabric hat kein Platzier-Ereignis; Wegfall würde Schutz-Mods auf Fabric umgehen. Neue Config wäre breit (Lang, Wiki, Config-Test) | verworfen als Code-Änderung; Nebenwirkung und Hinweis auf `WorldPermissions` in `docs/MODPACK.md` ergänzt | keiner (Doku) |
| F-19 | bestätigt | `reconcile` lädt je Lauf höchstens 8 nicht geladene Chunks (`MAX_WAKE_LOADS_PER_RUN`), der Rest folgt im nächsten Intervall (Zustand bleibt „soll laufen, ist inaktiv“) | nicht testbar ohne entladene Chunks, durch Compile + Review |
| F-20 | bestätigt | Rest von `addItem(result)` wird wie die Rückstände vor den Crafter gedroppt | nicht testbar (abweichender Fremd-Container nötig) |

## Vorschlag (kein Code) für F-13, F-14, F-17, F-18

- **F-13** (`@Redirect`/`@ModifyConstant`): mechanisch auf `@WrapOperation` bzw. `@ModifyExpressionValue(at=@At(CONSTANT))` umstellen, je Mixin ein Commit; Reihenfolge nach Verbreitung des Ziels (HUD-Mixins, `PistonHandlerMixin`, `AbstractArrowRecoveryMixin`). Je Mixin der vorhandene Gametest als Netz. Vor dem Umbau mit `ast-grep` alle Fundstellen listen.
- **F-14** (Test-/Dev-Code im Jar): zuerst `unzip -l` am Release-Jar zur Bestätigung. Dann `jar { exclude 'com/simplebuilding/gametest/**', 'com/simplebuilding/dev/**' }` im Release-Task (nicht im Dev-Lauf) und `/sbtestcentre` nur bei `ModEnvironment.isDevelopmentEnvironment()` registrieren; `PistonEventProbe`/`ProtectionProbe` hinter eine Leerprüfung (schon `isEmpty()`), Entrypoints aus der `fabric.mod.json` des Release-Builds streichen.
- **F-17** (Doppelimplementierungen): Prüfskript in `tools/` (Zeilenzahl und öffentliche Signaturen von Kern- und Modulkopie vergleichen, Warnung bei Drift) in `check` einhängen; der tote Zweig wird gelöscht, sobald 26.2 nicht mehr gepflegt wird (Besitzerentscheidung). Gemeinsames nach SimpleLib.
- **F-18** (Hammer-Amboss im Kern): Hammer+Hammer und Hammer+Material in ein Mixin von `modules/simplehammers` (eigener Plugin-Schalter, eigener Gametest im Modul-Target) verschieben; der Kern behält nur den Strukturkompass. Erst nach Merge von b-revfix24 (gleiche Datei), danach F-09 mitnehmen.

## Verifikation

`./gradlew :mc26_3:fabric:compileJava`, gezielte Läufe `fabric-263` mit Filter `simplebuilding:rev_core24*` und `simplebuilding:magnet*`, Generatoren (Guides, Wiki, Discover), `./gradlew check`.
