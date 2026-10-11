# Plan – Tiegel-Fass: Hitbox aus dem Modell + X-Ray-Culling (2026-10-08)

Quelle: `.claude/QUEUE.md` Nachtrag 15 („Tiegel-Fass: Hitbox des angedockten Fasses korrigieren (Outline/Kollision
passend zum kleineren Modell); Wallhack/X-Ray-Effekt (durchsichtige Nachbarflächen, falsches Culling/Render-Layer/
Occlusion) beheben") · `docs/ai/ROADMAP-2026-10-07.md:12` („Fass-Hitbox + X-Ray | N15 | BP").
Branch `bp-barrel`, Hauptlinie 26.3, Fabric zuerst. Kein Push in diesem Lauf.

## Ist-Zustand (erhoben)

- `modules/simplelib/shared/java/com/simplelib/crucible/CrucibleBarrelBlock.java` (174 Zeilen) überschreibt
  **keinen** Shape-Zugriff: `getShape` → `Shapes.block()` (Vollwürfel), `getCollisionShape` → Default =
  `state.getShape(...)`, `getOcclusionShape` → Default = `state.getShape(EmptyBlockGetter, ZERO)`.
  Registrierung `LibBlocks.barrel()` ohne `noOcclusion()` → `canOcclude = true`.
- Modell des angedockten Fasses `assets/simplelib/models/block/*_barrel_attached.json` (vier Elemente, identisch für
  copper/reinforced/netherite; `mc26_3/overlay/.../enderite_barrel_attached.json` gleich): Körper
  `from [1.5,0,0] to [14.5,11,13]`, Flansch `from [3,3,-1] to [13,9,0]`, Rinne `from [6,14,-4] to [10,15,1]`,
  Stirn `from [6,11,0] to [10,14,1]`. Lose Modell `*_barrel.json` = Vollwürfel `[0,0,0]–[16,16,16]`.
- Blockstate `assets/simplelib/blockstates/copper_barrel.json`: `attached=true,facing=<dir>,open=false` → Modell
  `*_attached` mit `y = 0/90/180/270` für north/east/south/west (Modell-Front = Nord = −z zeigt auf FACING).
- Occlusion-Pfad (belegt, nicht dekompiliert – Signaturen per `javap` am Vanilla-Jar, Implementierung aus dem
  Forge-Quellen-Jar `injected-sources.jar`):
  - `BlockBehaviour.BlockStateBase.initCache()` setzt `occlusionShape = canOcclude ? getOcclusionShape(state) : empty`;
    `initCache()` wird von `Blocks` (Statisch-Init) für alle registrierten Zustände aufgerufen → im Test verfügbar.
  - `Block.shouldRenderFace(BlockState, BlockState, Direction)` cullt die Nachbarfläche, wenn
    `neighborState.getFaceOcclusionShape(dir.opposite) == Shapes.block()` (Identität) → **das ist der X-Ray-Bug**:
    die volle Occlusion-Form des angedockten Fasses lässt die Wandfläche weg, das Loch dahinter wird sichtbar.
  - `canOcclude` kommt aus den Block-Eigenschaften (`properties.canOcclude`), ist also **block-global** und nicht
    zustandsabhängig (`BlockState` ist final) → `noOcclusion()` würde auch das lose Fass ändern → **nicht** möglich.
  - Konsequenz aus der einzigen notwendigen Änderung: `getOcclusionShape` leitet sich aus `getShape` ab
    (Zeile 281), `getCollisionShape` ebenfalls (Zeile ~318, `hasCollision` bleibt true) → **ein** Override genügt.
- Licht: `getLightDampening = solidRender ? 15 : propagatesSkylightDown ? 0 : 1` → angedocktes Fass wird 1 statt 15
  (volle Blockform → kein Licht mehr durchgelassen). Vanilla-`ChestBlock` verhält sich mit seiner 14/16er-Form genau
  gleich (kein Light-Override) → **kein** Light-Override vorgesehen, Nebenwirkung hier dokumentiert.
- Nicht betroffen (bewusst unverändert): das lose Fass (Vollwürfel, volle Occlusion), `attached=false`-Zustand
  generell, Block-Properties/Registry, Datagen, Texturen, Wiki.

## Umsetzung

1. **`CrucibleBarrelBlock` – ein Override**
   - Vier statische `VoxelShape` für `ATTACHED=true`, abgeleitet aus dem Körper-Element plus Blockstate-Rotation
     (Drehung um die Blockmitte 8/8/8; `y=90` ⇒ `x' = 16 − z`, `z' = x`):
     - north: `box(1.5, 0, 0, 14.5, 11, 13)`
     - east:  `box(3, 0, 1.5, 16, 11, 14.5)`
     - south: `box(1.5, 0, 3, 14.5, 11, 16)`
     - west:  `box(0, 0, 1.5, 13, 11, 14.5)`
   - `protected VoxelShape getShape(state, level, pos, context)`: `ATTACHED` → die passende Box,
     sonst `super` (Vollwürfel = bisheriges Verhalten).
   - Kommentar verweist auf Modelldatei und Blockstate-`y`-Rotationen (Regel: Aussagen im Code müssen belegbar sein).
   - **Nur der Körper** wird zur Form: Flansch und Rinne ragen außerhalb der Blockzelle in den Tiegel hinein und
     würden die Hitbox über den Block hinaus auf den Tiegel ausdehnen (Entscheidung, hier begründet).
   - Nicht überschrieben, weil automatisch abgeleitet: `getCollisionShape`, `getOcclusionShape`.
     Nicht überschrieben, weil block-global: `canOcclude`/`noOcclusion()`.
     Nicht überschrieben, weil Vanilla-Verhalten: Licht (`15 → 1` wie `ChestBlock`).
2. **Zwei GameTests** (Testkörper `CrucibleTests`, Katalog `SimpleBuildingGameTests`, Fabric-Adapter
   `CrucibleGameTest`; NeoForge/Forge registrieren aus dem Katalog):
   - `attachedBarrelHitboxFollowsItsModel` – liest zur Laufzeit den Blockstate und das Modell per Classpath
     (`assets/simplelib/blockstates/copper_barrel.json` → Modellreferenz → `assets/simplelib/models/block/...`),
     nimmt das **erste** `elements`-Element, dreht es um die `y`-Rotation des jeweiligen `facing` und vergleicht es
     mit `getShape` **und** `getCollisionShape` aller vier Richtungen; zusätzlich `attached=false` = Vollwürfel
     (unverändert). Damit bricht der Test, sobald sich das Modell bewegt – die Hitbox bleibt an das Modell gekoppelt.
     Erhoben für copper/reinforced/netherite (`simplelib`) und enderite (`simplebuilding`, wenn
     `CrucibleCompat.enderiteBarrel() != null`); hinter `if (!McVersion.CRUCIBLE) { helper.succeed(); return; }`,
     kein Import simplelib-spezifischer Klassen in `common/src/shared` (26.2 baut es mit).
   - `attachedBarrelDoesNotOccludeItsNeighbours` – pro `facing`: `state.getOcclusionShape()` ist **kein** Vollwürfel
     (`!Block.isShapeFullBlock(...)`) und `Block.shouldRenderFace(stone, attached, d)` ist für alle sechs Richtungen
     `true` (das ist exakt die Vanilla-Culling-Entscheidung, die den Fehler erzeugte); Gegenprobe `attached=false`:
     Occlusion = Vollwürfel und `shouldRenderFace` = `false` für alle sechs Richtungen. `canOcclude()` bleibt wie
     dokumentiert `true` (block-global, kein Vollwürfel-Claim über diesen Getter).

## Tests (Ziele, Ausführung)

- `python3.12 tools/testrunner/run.py --targets fabric-263 --filter crucible_game_test_attached_barrel` –
  zwei Tests, genau eine Zeile „alles gruen“ bzw. „NICHT gruen“ muss dastehen (Report `mc26_3/fabric/build/junit.xml`).
- Enthaltene weitere Gates von `check`, soweit betroffen: `checkWiki`, `checkBalance`, `checkAtlases` (keine
  Änderung an Daten/Texten erwartet) – einzeln nachgezogen, falls der Lauf sie meldet.
- Nicht in diesem Lauf: `neoforge-263`/`forge-263` (gleiche Katalog-Testkörper, andere Loader), vollständiges
  `./gradlew check`, Client-Sicht (Klick-Test am Development-Server), Texturen-/Atlas-Checks.

## Risiken / Annahmen

- Annahme: Modell-Elemente außerhalb der Zelle gehören nicht zur Hitbox (Flansch/Rinne sitzen im Tiegel).
- Annahme: Occlusion = Hitbox ist gewollt (kein separater, größerer Occlusion-Shape) – genau das behebt X-Ray.
- Nebenfolge Licht 15 → 1 am angedockten Fass (wie Vanilla-Brust); keine Textanzeige betroffen (Regel: kein Text).
- Classpath: simplelib-Ressourcen liegen über `implementation project(':modules:simplelib:fabric')` im Fabric-Lauf;
  fällt die Ressource aus, schlägt der Test mit „not on the classpath“ fehl → dann Konstanten im Test statt JSON-Lesen
  (Entscheidung erst nach dem Lauf, hier vermerkt).
- Lauf-Konkurrenz: ein fremder Gradle-/GameTest-Lauf ist aktiv (flock `<job-lock>`), daher nur ein Lauf
  nacheinander.

## Bericht (nach dem Lauf)

Lauf: `python3.12 tools/testrunner/run.py --targets fabric-263 --filter 'simplebuilding:crucible_game_test_attached_barrel_*'`
· Run-ID `2026-10-08T09-50-44Z-b7ee` · Basis `a25d4fc` (schmutzig) auf `bp-barrel` · Dauer 24,8 s · Exit 0.

Exakte Zeilen aus der Ausgabe:

```
  Ziel                           Exit  Tests  gruen  rot    Dauer
  Fabric - MC 26.3                  0      2      2    0    24.8s

  alles gruen: 2/2 bestanden, 0 rot
  Datensatz: testing/runs/2026-10-08T09-50-44Z-b7ee.json
```

- [x] Testzeilen eingetragen (oben, exakt; Datensatz `testing/runs/2026-10-08T09-50-44Z-b7ee.json`,
      `totals: total 2, passed 2, failed 0`, `ok: true`).
- [x] Nicht-getestetes (offen):

  - NeoForge-26.3- und Forge-26.3-Lauf (Katalog-Einträge und Fabric-Adapter wurden nur geschrieben, nicht ausgeführt).
  - Vollständiges `./gradlew check` inkl. `checkWiki`, `checkBalance`, `checkAtlases`, `checkAtlases`/Jade-Split –
    nicht in diesem Lauf gefahren.
  - Client-Sicht/Klick-Test: Hitbox-Outline und sichtbare Nachbarflächen wurden **nicht** im Spiel gesehen;
    gedeckt ist nur die Vanilla-Culling-Entscheidung (`Block.shouldRenderFace`) und die Form per Unit-Assertion.
  - Licht-Nebenfolge 15 → 1 am angedockten Fass nicht im Spiel verifiziert (Begründung/Verhalten wie Vanilla-`ChestBlock`).
  - Linien 26.2, 1.21.11 und 26.4 nicht gebaut/getestet; für 26.4 ist `Block.shouldRenderFace(BlockState,BlockState,Direction)`
    nicht gegen das Vanilla-Jar geprüft (Restrisiko, wird erst mit `-Pmc264=true` gebaut).
  - Textur-/Atlas-Checks (`tools/textures/generate_textures.py --check`) – keine Textur geändert, nicht erneut gefahren.

- Abweichung vom Plan (laufbedingt): erster Kompilierlauf schlug fehl, weil `BlockGetter` **nicht** in
  `net.minecraft.world.level.block` liegt, sondern in `net.minecraft.world.level` (Import in `CrucibleBarrelBlock`
  korrigiert; so nutzt ihn auch `CrucibleBlock`). Danach `BUILD SUCCESSFUL`.
- Bestätigt aus dem Lauf: simplelib-Ressourcen lagen auf dem Fabric-Testclasspath (keine „not on the classpath“-Meldung,
  JSON-Lesen funktionierte) → der im Plan notierte Fallback auf Konstanten war nicht nötig.
- Klassenpfad/Reihenfolge: `:modules:simplelib:fabric:compileJava` und `:common:compileJava` grün, danach
  `:mc26_3:fabric:compileJava` grün (auch die Fabric-Adapter in `src/main/java`).
