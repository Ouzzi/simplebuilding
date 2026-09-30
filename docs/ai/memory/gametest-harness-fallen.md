---
name: gametest-harness-fallen
description: "Fallen im Minecraft-Gametest-Harness, die Tests still falsch grün oder falsch rot machen"
metadata: 
  node_type: memory
  type: reference
  originSessionId: c63559d3-2c6a-43ab-9c5e-92fd18904742
  modified: 2026-09-28T14:22:28.402Z
---

Vier Dinge, die beim Schreiben von Spieltests in diesem Repo je einen halben Umweg gekostet haben:

- **Der Mock-Spieler ist nicht wegen des Spielmodus unverwundbar.** `ServerPlayer#isInvulnerableTo`
  liefert `true`, solange `!connection.hasClientLoaded()`, und `PlayerList#placeNewPlayer` setzt
  `clientLoadedTimeoutTimer` beim Einloggen auf 60 Ticks. Weder `Player#hurtServer` noch
  `actuallyHurt` schaut auf den Spielmodus. Abhilfe:
  `player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket())` — dasselbe, was
  ein echter Client schickt. Gilt auf 26.2 **und** 1.21.11.
- **Entity-Suchen dürfen die eigene Teststruktur nicht überragen.** Die Strukturen stehen nur
  wenige Blöcke auseinander; `helper.getBounds().inflate(8)` findet die Entities des Nachbartests,
  und das Ergebnis hängt dann an der Testreihenfolge (auf einer MC-Linie grün, auf der anderen rot).
  `getBounds()` ohne Aufweitung benutzen.
- **`isCreative()` bleibt unerreichbar, `instabuild` nicht.** `GameTestHelper`s In-Level-Mock
  überschreibt `gameMode()` fest auf `CREATIVE`. Wer einen `!isCreative()`-Zweig testen will, baut
  den `ServerPlayer` selbst (anonyme Unterklasse mit überschriebenem `gameMode()` plus
  `GameType#updatePlayerAbilities`); auf 26.2 tut das `helper.makeMockServerPlayer(GameType)`
  fertig, auf 1.21.11 gibt es die Fabrik nicht.
- **Fabric leitet die Test-Id aus dem Adapter-Methodennamen ab und trennt NUR an der Grenze
  klein/Ziffer → gross.** `IntoABlock` hat zwischen `A` und `B` keine solche Grenze, wird also ein
  Token: `..._into_ablock...`, nicht `..._into_a_block...`. NeoForge nimmt dagegen den
  Katalognamen wörtlich. Weicht der Katalog ab, läuft derselbe Test auf beiden Loadern unter
  verschiedenen Ids — auf einer Seite sieht er aus wie bestanden, auf der anderen wie nicht
  vorhanden. Regex, die alle bekannten Fälle trifft: `re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name).lower()`.
  Vermeide einzelne Grossbuchstaben-Wörter in Methodennamen (`theBlock` statt `ABlock`).
  `tools/testrunner/run.py` prüft die Deckungsgleichheit von Katalog und Lauf seit 2026-09-03 selbst.
- **`tickCount` wird ab 26.2 vom Level hochgezählt, nicht vom Entity.** `tickCount++` sitzt in
  `ServerLevel#tickNonPassenger` (dekompiliert, Zeile 829), nicht mehr in `Entity#baseTick`. Ein
  von Hand ausgelöster `player.connection.tick()` zählt also **nichts** hoch: wer einen Tick auf
  einer bestimmten Taktzahl fahren will, setzt `tickCount = atTick`, nicht `atTick - 1`. Sonst
  liest jede Periodenprüfung (`tickCount % 20 == 0`) einen Tick zu früh und der geprüfte Zweig
  feuert nie — der Test macht dann den Mod für einen Fehler verantwortlich, den er nicht hat.
- **`assertValueEqual` nimmt seine zwei Werte auf den beiden MC-Linien in umgekehrter
  Reihenfolge.** Auf 26.2 ist die Signatur `(value, expected, name)` und die Meldung wird als
  `(name, expected, value)` gebaut; auf 1.21.11 wird sie als `(name, erstes, zweites)` gebaut —
  dort ist also das **erste** Argument der Sollwert. Die Vorlage ist auf beiden Linien dieselbe
  (`Expected %s to be %s: was %s`), und beide Parameter sind `N`, also merkt es weder der
  Compiler noch der Test: `equals` ist symmetrisch, grün bleibt grün. Nur die Fehlermeldung
  stand auf 1.21.11 verkehrt herum — bei allen 673 Aufrufen. Seit 2026-09-07 gehen die Aufrufe
  dort über `mc1_21_11/.../gametest/Assertions.java`, das die Reihenfolge einmal zentral dreht;
  Testkörper werden weiter in 26.2-Reihenfolge geschrieben (Ist zuerst) und sind damit portierbar.
- **Ein Item auf Weltboden + 5 liegt im Bodenblock des Gametest-Raums** (Strukturursprung y = −59
  = minY + 5), und `ItemEntity` schiebt es auf einem von `(tickCount + id) % 4` gewählten Tick
  wieder hinaus — die Id hängt an allen Entities der Tests davor. Exakte Positionsprüfungen nach
  n Ticks flackern dann je Testreihenfolge; die Hubhöhe **einen Tick nach dem Spawn** lesen und
  Vanillas Schubs danach nur grob eingrenzen.
- **`handleUseItemOn` tut beim Mock-Spieler still nichts** (2026-09-28): der frisch eingeloggte
  Spieler hat `awaitingPositionFromClient != null` (Teleport nie quittiert), und
  `ServerGamePacketListenerImpl#handleUseItemOn` verlangt `== null`. `handleUseItem` (Luftklick)
  prueft das nicht. Echten Klickpfad ab Server stattdessen ueber
  `player.gameMode.useItemOn(player, level, stack, hand, hit)` fahren (enthaelt die Schleich-
  Umgehung des Blocks), Ticks ueber `player.doTick()` (laeuft `Inventory#tick` -> `inventoryTick`).
- **1.21.11 hat kein `runBeforeTestEnd`.** Ersatz im Repo:
  `mc1_21_11/shared/java/com/simplebuilding/gametest/TestCleanup.java` — sammelt das Aufräumen und
  führt es unmittelbar vor `helper.succeed()` aus, also nur auf dem Erfolgspfad.

**Clientseitig, aus der P7-Runde vom 2026-09-10:**

- **Vanilla setzt `isDestroying` nicht in jedem Abbaupfad.** `MultiPlayerGameMode.continueDestroyBlock`
  geht bei `sameDestroyTarget` (gleiche Position **und** gleiches Werkzeug wie beim letzten
  Abbau) in einen Zweig, der nur `destroyProgress` aufsummiert; `stopDestroyBlock` und ein
  fertiger Abbau setzen `destroyBlockPos` nicht zurück. Ein verschluckter Klick (siehe nächster
  Punkt) plus gehaltener Knopf bringt genau diesen Zweig — der Block bricht nach der normalen
  Zeit, `isDestroying()` bleibt false, `getDestroyStage()` läuft trotzdem 0..9. Eine Testspur
  (`MiningTrace` in `MultiBlockBreakingClientTest`) hat das entschieden, nicht Raten.
- **`MouseHandler.grabMouse()` armiert `Minecraft.missTime` mit 10000**, und `startAttack()` weigert
  sich darüber. Reihenfolge in `setAttacking`: greifen, **dann** Sperre löschen, dann drücken —
  andersherum wird der registrierte Klick vom nächsten Tick verschluckt.
- **Fensterfokus.** `Minecraft.pauseIfInactive` öffnet nach 500 ms ohne Fokus den Pausenbildschirm,
  `grabMouse()` verlangt `isWindowActive()`. Auf einem Rechner, der aus dem Standby kommt, oder
  wenn man daneben arbeitet, scheitert damit **alles** — sieben Skripte, ein Grund. Fabrics
  Gerüst schluckt `Window.onFocus/onEnter/onIconify` per Mixin; der NeoForge-Testmod tut es seit
  dem 2026-09-10 genauso (`WindowFocusMixin`) und setzt `pauseOnLostFocus=false`.
- **`GuiGraphicsExtractor.fill` vertauscht die Ecken**: nach dem Aufruf hält `x0` den **größeren**
  Wert. Wer einen `ColoredRectangleRenderState` an seiner Ecke sucht, vergleicht min/max, nicht
  benannte Ecken.
- **Slot-Items liegen relativ unter einer Pose.** `AbstractContainerScreen` zeichnet den Inhalt
  eines Slots bei `slot.x/slot.y` unter einer auf `leftPos/topPos` verschobenen Pose; eine Mod,
  die *danach* absolut zeichnet, landet bei denselben Bildschirmpixeln mit anderer `x()/y()`.
  Vergleichbar nur über `item.pose().transformPosition(x, y)`.
- **Chunks bauen sich nicht von selbst neu.** Nach einem `fill` über eine alte Struktur blieb
  die Leiter sieben Sekunden lang im Bild (Blockdaten längst leer). `LevelExtractor.allChanged()`
  (26.2) bzw. `LevelRenderer.allChanged()` (1.21.11) wirft alles weg; danach braucht es eine
  **Render-Schranke** — `LevelRenderer.hasRenderedAllSections()`, dieselbe Frage, die Fabrics
  `waitForChunksRender` stellt. Ohne sie erwischen die Grundlinien-Screenshots die leere Welt.
- **`hasRenderedAllSections()` ist unmittelbar nach `allChanged()` trivial wahr** — die Warteschlange
  ist leer, weil noch nichts eingereiht wurde (das tut erst `compileSections` im nächsten Frame).
  Ein Screenshot in genau diesem Moment zeigt Himmel von Rand zu Rand (409920 von 409920 Pixeln
  anders, vier Versuche lang). Die echte Schranke ist Vanillas eigene Ladefrage
  `LevelRenderer.isSectionCompiledAndVisible(spielerPos)` **und** `hasRenderedAllSections()`;
  alle vier Treiber fragen seit dem 2026-09-10 so — Fabrics `waitForChunksRender()` kehrt nach
  `allChanged()` genauso sofort zurück (zwei Mutationsrunden am Himmel-Frame gescheitert). Dazu `chunkSectionFadeInTime = 0` in den
  eingefrorenen Optionen, sonst blendet eine ferne Sektion nach dem Neubau noch ein.
- **Die Chisel-Neigung des Handrenderers glättet pro gerendertem Frame, nicht pro Tick**
  (`progress += (ziel - progress) * 0.15`, Abschneiden bei 0.001 → ~43 Frames). 60 Ticks Ruhe
  reichen nur, wenn der Client ≥ 1 Frame/Tick schafft; auf NeoForge 26.2 blieben 522 Pixel einer
  nicht ganz zurückgekehrten Hand. Deterministisch: das Mixin-Feld `mainHandChiselProgress` per
  Reflexion vom `ItemInHandRenderer` lesen und darauf warten (`awaitTheChiselTiltAt`).
- **Ein Skript hinterlässt Zustand für das nächste**: der Luftsprung-Test des Bootstrap-Skripts
  lässt einen laufenden Cooldown zurück, dessen Balken in der ersten Szene des Folgeskripts noch
  ausblendet. `awaitStableFrame` (zwei Frames, 20 Ticks Abstand, bis zu vier Versuche) fängt
  das; ohne diese Schranke stünde es als „Rauschen 2823 Pixel" in der Grundlinie.
- **Ein roter Schritt hinterlässt seinen Zustand dem nächsten Skript**: offener Bildschirm (ein
  offener `BuildingWandScreen` ist ein Pausenbildschirm → integrierter Server steht → „No
  SurvivalSyncPayload arrived"), gehaltene Tasten, Konfigschalter. `TestScene.build` schließt
  seit dem 2026-09-10 zuerst und lässt alles los. Ohne das ist ein Mutationslauf mit einer
  Mutation je Skript wertlos — sechs Folgeskripte scheiterten an „a screen is open".
- **Die Zeitüberschreitungs-Diagnose lief auf Fabrics Test-Thread**, wo `Minecraft.getInstance()`
  verweigert — „the diagnosis itself failed" war der ganze Befund. Jetzt `harness.run(CLIENT)`.
- **Äquivalente Mutanten gibt es**: die Werkzeugwache des Riss-Renderers ist auf dem
  Strip-Miner-Zweig folgenlos, weil `getStripMinerBlocks` selbst am ersten unabbaubaren Block
  abbricht; nur der Vein-Miner-Zweig (Erzliste fragt nie nach dem Werkzeug) sieht sie. Und ein
  Schwerpunkt-Test auf einer Ebene, die aus dem Bild ragt, misst immer die Bildmitte.
- **`/fill` lässt Blöcke fallen, deren Halt in derselben Füllung früher weggeht.** Der Befehl
  setzt mit `2 | 256` (Formupdates sofort, ohne `UPDATE_SUPPRESS_DROPS`): eine Leiter, deren
  Steinpfeiler in der Iteration vor ihr zu Luft wird, wird per `updateOrDestroy` **zerstört,
  mit Drop**. Dreizehn wippende Leiter-Items im Bild → Szene hält nie still, und zwar nur, wenn
  der Zufallswurf sie in den Bildrand legt. `kill @e` muss **nach** dem Leeren laufen, nicht
  nur davor. Die Entity-Liste in der `awaitStableFrame`-Meldung war der Beweis.
- **Schrittnamen mit Apostroph im NeoForge-Log**: „FAILED in x at step '…': msg" — ein `[^']*`
  bricht bei `block's`/`stone's`/`button's` ab, der rote Schritt wird nicht gelesen und gilt als
  „grün geblieben". Non-greedy bis `': `. Drei Mutationen auf zwei Zielen sahen deshalb falsch grün aus.
- **Der Rechner schläft mitten in langen Läufen** (zweimal 7–20 h Pause): Runden mit dem Zeitsprung
  scheitern an „SurvivalSyncPayload does not arrive once a second" o. ä. — Nebenschaden, kein Befund.
  `request_keep_awake(session_idle)` vor Mehrstundenläufen, und danach die betroffenen Runden wach
  wiederholen (`--only`).
- **NeoForge-HUD-Ebenen aus `RegisterGuiLayersEvent` ignorieren F1** (`Hud.isHidden` gilt dort
  nur für die Vanilla-Ebenen, je einzeln), Fabrics `HudElementRegistry` hängt das Element in
  eine Vanilla-Ebene und erbt deren Schalter. Ein geteiltes Overlay muss die Frage selbst stellen
  — 26.2: `client.gui.hud.isHidden()`, 1.21.11: `client.options.hideGui` (Zeilen-Differenz).

Und der Klassiker: **„BUILD SUCCESSFUL" ist kein Beweis.** `./gradlew ... > log; echo $?` liefert
den Exit-Code von `echo`. Exit-Code getrennt festhalten und im Log nach
`required tests failed` suchen.

Siehe auch [[mc-26-api-nachschlagen]] für die Jars, mit denen sich so etwas am Bytecode belegen lässt.
