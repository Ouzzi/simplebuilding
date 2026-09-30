---
name: client-test-geruest
description: Warum Fabric-Client-Tests nicht nach NeoForge kopierbar sind und welcher Weg gewählt wurde
metadata:
  node_type: memory
  type: project
  originSessionId: c63559d3-2c6a-43ab-9c5e-92fd18904742
  modified: 2026-09-10T01:00:00.000Z
---

**Fabrics `FabricClientGameTest` ist kein API-Wrapper, sondern ein Framework.** Die blockierende,
imperative Oberfläche (`runOnClient`, `computeOnClient`, `waitTicks`, `waitForScreen`) funktioniert
nur, weil das Modul die Tickschleife durch eine **Vier-Phasen-Schranke** ersetzt (Phaser +
Semaphoren: Tick → Server-Tasks → Client-Tasks → Test). Das kostet **21 Mixin-Einsprungpunkte in
14 Vanilla-Methoden**: `Minecraft.run` / `runTick` / `runAllTasks` / `doWorldLoad` / **beide**
`disconnect`-Überladungen, `MinecraftServer.runServer` / `waitUntilNextTick` / `shouldRun`,
`BlockableEventLoop.schedule` / `doRunTask`, `Connection.channelRead0` / `sendPacket`, `Main.main`.
Quellen: `fabric-client-gametest-api-v1-*-sources.jar`, Paket
`net/fabricmc/fabric/impl/client/gametest/threading/ThreadingImpl.java` und die Mixins daneben.

**NeoForge hat keine Client-Test-API.** `neoforge/src/clientGameTest/` fährt einen selbstgebauten
Schrittautomaten (`TestScript` mit `act` / `await` / `idle` / `step`), getrieben aus
`ClientTickEvent.Post`. Ein früherer Versuch mit zweitem Thread und Semaphoren hat sich verklemmt:
**`Minecraft.disconnect` pumpt selbst Client-Ticks**, um seinen Fortschrittsbildschirm zu malen —
aus einem Client-Tick heraus aufgerufen kehrt es nie zurück. Deshalb legt der Lauf heute eine Welt
an und beendet danach die JVM (`Runtime.halt`) statt sich abzumelden.

**Daraus folgt die Richtung:** Der Schrittautomat ist die *schwächere* Abstraktion. Sie lässt sich
auf Fabrics API in einer Schleife ausdrücken (`while (!script.tick(log)) context.waitTick();`);
umgekehrt geht es nicht, ohne das ganze Phasengerüst nachzubauen. Ein „dünner Adapter", der die
Fabric-API auf NeoForge nachstellt, ist also **nicht dünn**.

**Entscheidung des Nutzers (2026-09-08):** Variante **(c)** — jeden Client-Test einmal als
Schrittliste gegen eine gemeinsame Fassade schreiben, je Ziel ein dünner Treiber. Dazu **ein**
Mixin auf `Minecraft.disconnect` für NeoForge, damit alle Testklassen in einem Client-Start laufen
(nicht zu verwechseln mit den 21 Eingriffen aus Variante (b)).

**Fallstrick für die Fassade:** Fabric besteht darauf, dass `getInput()` **auf dem Testthread**
läuft (`ThreadingImpl.checkOnGametestThread`), also gerade *nicht* innerhalb von `runOnClient`.
Eingabeschritte müssen deshalb eine eigene Schrittart sein und dürfen nicht mit `act` vermischt
werden. Fabric setzt Eingaben über einen Accessor auf `KeyboardHandler#onKey` bzw. `MouseHandler`
um — gewöhnliche Aufrufe auf dem Client-Thread, unter NeoForge ein Accessor-Mixin.

**Sieben Fallen, jede einmal gestellt und bezahlt:**

1. **`Minecraft.getInstance()` ist auf Fabrics Testthread verboten** und wirft mit klarer Meldung.
   Alles, was den Client braucht, gehört in `runOnClient` / `computeOnClient`.
2. **Beide Loader verschlucken Befehlsfehler**, wenn man ihren bequemen Pfad nimmt — Fabrics
   `TestServerContext.runCommand` ebenso wie NeoForges `performPrefixedCommand`. Beide gehen
   deshalb direkt über `getCommands().getDispatcher().execute(...)`, per `server.execute(...)` auf
   den Serverthread. Vanilla meldet außerdem „nichts getan" als Fehler (`fill` ohne Änderung,
   `kill` ohne Treffer, `clear` bei leerem Inventar) — dafür gibt es ein Flag **je Befehl**, nicht
   pauschal.
3. **Screenshot-Pfade darf man nicht aus dem Namen bauen.** Fabric schreibt `0004_name.png` mit
   Laufzähler, NeoForge `name.png`. Der Pfad kommt vom Harness, direkt nach der Aufnahme.
4. **Graben ist auf NeoForge kein gehaltener Mausknopf.** Es braucht gleichzeitig: gegriffene Maus,
   gesetzte Angriffsbindung, `KeyMapping.click` (sonst läuft `startAttack()` nie), einen geleerten
   `Minecraft.missTime` (Vanillas Eingabesperre, nach jedem Bildschirm neu armiert) und beim
   Loslassen `gameMode.stopDestroyBlock()` — sonst gräbt es weiter.
5. **Ein Bildschirm liest Ereignisse, keine Bindungen.** Mausknöpfe müssen über
   `MouseHandler.onButton` gehen (Accessor-Mixin), sonst erreicht kein Klick je einen Knopf.
   **`onButton` ruft `KeyMapping.set` und `click` selbst auf**, wenn kein Bildschirm offen ist —
   beides zu tun ist ein Doppelklick. Genau das hat einen Bilderrahmen gesperrt und sofort wieder
   entsperrt. Fabric macht es genauso (`TestInputImpl.pressOrReleaseKey`).
6. **`setCursorPos` nimmt rohe Fensterpixel.** Zweimal wurde dort skaliert, obwohl der Aufrufer
   die GUI-Umrechnung schon gemacht hatte (sie muss auf dem Client-Thread passieren, der
   Harness-Aufruf nicht). Der Zeiger landete neben dem Inventar, und kein Slot meldete sich je
   als überfahren.
7. **Ein gescheitertes Skript lässt gehaltene Tasten liegen.** Seine Freigabeschritte laufen ja
   nicht mehr. Eine hängengebliebene Sneak-Taste hat danach zwei weitere Skripte rot gemacht, und
   schuld war jedes Mal die Mod. `Harness.releaseAllInput()` räumt nach **jedem** Skript auf.

**Umfang (2026-09-10):** alle vier Ziele fahren dieselben **85 Prüfpunkte** aus demselben Baum
(11 685 Zeilen, `common/src/shared/clientgametest/` und die heruntergeportete Kopie unter
`mc1_21_11/shared/clientgametest/`). `CLIENT_PARITY_DEBT` ist **leer**. Stand: Fabric 26.2 und
Fabric 1.21.11 85/85, NeoForge 26.2 83/85 (offen: `breaking-d-strip-miner-sneaking`).

**Was die 1.21.11-Portierung wirklich kostete:** 131 Compilerfehler, alle mechanisch — `Gui` trägt
Bildschirm und HUD selbst, `BundleContents` hält `ItemStack` statt `ItemStackTemplate`,
`BlockStateModelSet`/`BlockStateModelPart` heißen `BlockModelShaper`/`BlockModelPart`,
`GuiGraphicsExtractor` ist noch `GuiGraphics`, `KeyMapping.matches` nimmt ein `KeyEvent`. Die
Befürchtung, ein Teil der Tests müsse „neu gedacht" werden, hat sich nicht bestätigt.

Siehe auch [[multiloader-structure]], [[gametest-harness-fallen]] und [[testabdeckung-2026-09]].
