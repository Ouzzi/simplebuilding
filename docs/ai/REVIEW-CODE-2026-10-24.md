# Code-Review gesamte Mod (24.10.2026, Stand Bündel 53 / 62a7740f4)

Auftrag Besitzer 24.10. Nur 26.3 Fabric. Zwei Lese-Durchgänge (statisch, Datei:Zeile belegt, nicht zur Laufzeit bestätigt). Fixes kritisch/hoch: Branch b-revfix24. Mittel/niedrig: Queue-Abschnitt REV24.

### Code-Review Teil 1: Kern (common/, framework/, mc26_3/fabric, src/main)

Stand: Worktree `/root/wt/b-review24` (HEAD 62a7740f4, Bündel 53). Maßgeblich MC 26.3 + Fabric. Nur gelesen, nichts gebaut, kein Gradle, keine sb-test-Läufe.
Pfade relativ zu `common/src/shared/java/com/simplebuilding/` (kurz `C/`), `src/main/java/com/simplebuilding/` (kurz `M/`), `framework/src/main/java/com/simplebuilding/framework/api/` (kurz `F/`).

### Gesamteinschätzung

**Stärken.** Die Server-Pakete sind überwiegend sauber gehärtet. Der Fabric-Empfänger (`M/networking/ModMessages.java`, `receive`) wechselt auf den Server-Thread. Die Handler prüfen Slot, Reichweite und Enum-Namen. Ratenbudgets gibt es für Laser, Boost und Signieren, Codecs haben Stringlimits. Die Spuren früherer Audits (SBG-xx, "Audit 2026-09-26") sind überall sichtbar und sinnvoll. Mixins sind meist `@WrapOperation`/`@ModifyExpressionValue`, mit `defaultRequire:1` und einem Plugin, das Mixins je Split-Stufe abschaltet. Menüs (`BackpackMenu`, `TieredChestMenu`, `AutonomousCrafterMenu`) haben `stillValid`, Sperr-Slots und Sonderfälle für Übergrößen-Stapel. Der Block-Entity-Code (ChunkLoader, TieredChest) behandelt Entladen und `/setblock` ausdrücklich. Die Abbaukette (Vein/Strip/Hammer) schützt per `try/finally` gegen Rekursion und hat `BuildPermissions.isProbing()`.

**Architektur-Risiken.**
1. Der größte Risikoblock sind **teure Arbeit auf dem Server-Thread, die ein einzelner Spieler anstoßen kann** (F-01, F-02, F-03). Das gilt unabhängig von der sonstigen Härtung.
2. **Statischer Zustand ohne einheitlichen Lebenszyklus.** Es gibt rund 30 statische Maps. `WandUndo` räumt bei Disconnect und Server-Stopp auf (SBG-03). `BlueprintBuilder`, `RotatorItem`, `BuildingCoreItem` und `Protection` im Framework tun es nicht (F-06). Ein zentraler "SessionState"-Mechanismus, der pro Spieler und pro Server registriert wird, würde das ein für alle Mal lösen.
3. **Zwei parallele Codebasen im Kern, gesteuert über 49 `McVersion`-Konstanten.** Auf 26.3 sind `STORAGE_IN_SB`, `HAMMERS_IN_SB`, `BLOCKS_IN_SB` und `OVENS_IN_SB` false. Bündel, Köcher, Rucksack und Platzierte Bündel existieren zweimal (z. B. `C/items/custom/ReinforcedBundleItem.java` und `modules/simplestorage/.../ReinforcedBundleItem.java`; `C/util/PlacedBundles.java` und das Gegenstück im Modul). Auf 26.3 ist der `common`-Teil dort toter Code, auf 26.2 ist er live. Die Drift-Gefahr ist hoch (F-17).
4. **Test- und Dev-Code im Hauptquellsatz.** `C/gametest` hat 97,6k Zeilen, `M/gametest` 7,3k, `C/dev` 7,5k. Von den ca. 222k Zeilen in `common` sind damit knapp die Hälfte keine Spielcode (F-18).
5. Die `ServerTuning.get()`-Konstruktion (Client-Thread liefert Serverwerte, sonst lokale Datei) ist praktisch, aber thread-implizit. Sie funktioniert nur, solange jeder Aufrufer auf dem erwarteten Thread läuft.

#### Funde nach Schwere

Anzahl: kritisch 1, hoch 5, mittel 14, niedrig 9 (insgesamt 29). Die Nummern F-21 und F-25 sind bewusst nicht vergeben.

---

### KRITISCH

**F-01 · Amboss-Strukturkompass sucht synchron und ohne Limit bei jedem `createResult`**
- Datei: `C/mixin/AnvilScreenHandlerMixin.java:310-334` (Aufruf `findNearestMapStructure(..., 300, false)` in Zeile 331)
- Problem: Der Inject sitzt am HEAD von `AnvilMenu#createResult`. Der wird bei jedem Slot-Wechsel und bei jedem Umbenennen (`setItemName` ruft `createResult`) ausgeführt. Jedes Mal läuft die Strukturssuche mit Radius 300 Chunks (laut Kommentar 4800 Blöcke), ohne Cache, auf dem Server-Thread. Das ist 9-mal der Radius einer Vanilla-Karte und entspricht `/locate` mit Rekordradius.
- Szenario: Ein beliebiger Spieler ohne Rechte legt einen Kompass links und z. B. `Items.CHORUS_FRUIT` (End City), `ENDER_EYE` oder `SLIME_BALL` rechts in den Amboss und tippt im Namensfeld oder sendet Rename-Pakete. Jedes Paket löst eine Suche über bis zu 360.000 Chunk-Positionen aus (Struktur-Starts werden dabei erzeugt). Das lässt den Server für Sekunden bis Minuten hängen und erzeugt Chunks. Es ist auch ohne Absicht spürbar, weil jedes Tippen im Feld die Suche wiederholt. Das Ergebnis ist ein Remote-DoS für jeden Spieler. Zusätzlich: Strukturen, die in der aktuellen Dimension nicht vorkommen, durchlaufen die volle Suche ohne Treffer.
- Vorschlag: Suche nur beim Herausnehmen des Ergebnisses (`onTake`/`mayPickup`) ausführen, vorher nur ein Platzhalterergebnis ("Signal wird gesucht") anzeigen. Alternativ: Ergebnis pro (Spieler, Struktur-Tag, Chunk-Zelle) mit Gültigkeit von mehreren Minuten cachen, Radius deutlich verkleinern (z. B. 100) und pro Spieler einen Abklingzeit-Zähler setzen. Passende Strukturen nach Dimension vorab filtern.

---

### HOCH

**F-02 · Blaupausen-Editor: Pixel-Code wird vor jedem Budget voll geparst (CPU)**
- Dateien: `C/networking/ModMessageHandlers.java:469` (`editedBlueprint`), `C/blueprint/BlueprintPicture.java:255-259` (`refusal`), `C/blueprint/BlueprintCode.java:197`
- Problem: `editedBlueprint` ruft `BlueprintPicture.refusal(player, code)` auf, bevor irgendein Budget geprüft wird. `refusal` parst den Code mit `parseCached`, sobald er das Wort `pixels` enthält (Zeile 256: `code.contains(BlueprintCode.PIXELS)`). Das Signier-Budget (`takeSignBudget`, Zeile 479) kommt erst danach und gilt auch nur für `sign=true`. Der Parser läuft bis `MAX_EXPANDED_CELLS` = 4.194.304 Zellen (`BlueprintCode.java:59`), jede Zelle ein `model.set`.
- Szenario: Ein Client sendet in Schleife `BlueprintEditPayload` (unsigniert, `sign=false`, jeweils ein anderer Code mit dem Wort `pixels` und einem Zusatz, der sich bei jedem Paket ändert). Jedes Paket kostet einen vollen Parse auf dem Server-Thread. Es gibt keine Rate-Begrenzung, nur das Stringlimit von 32.000 Zeichen im Codec.
- Vorschlag: Das Parsen für `refusal` nur nach einem Budgetabzug (auch für unsignierte Pakete, billiger Satz je Zeichen) durchführen oder die Pixel-Prüfung auf eine billige Zeilenprüfung (`pixels`-Zeilenpräfix, Größe aus der Zeile) beschränken. Das Budget `SIGN_BUDGET` auf alle Pakete mit `pixels` ausdehnen und das Budget vor `refusal` abziehen.

**F-03 · Parse-Cache hält bis zu 24 riesige Modelle (Speicher)**
- Datei: `C/blueprint/BlueprintCode.java:186-211` (`CACHE`, `removeEldestEntry: size() > 24`)
- Problem: Der LRU zählt Einträge, nicht Gewicht. Jedes `BlueprintModel` ist eine `Int2ObjectOpenHashMap` (`BlueprintModel.java:22`). Bei 4,19 Mio. Zellen sind das grob 2^23 Slots (rund 64 MB) je Modell. 24 Einträge ergeben bis zu rund 1,5 GB. Die Zahl ist eine Abschätzung aus dem Code (Obergrenze `MAX_EXPANDED_CELLS`); ob ein konkreter Code mit 32.000 Zeichen die Obergrenze erreicht, habe ich nicht im Parser-Detail nachgerechnet, die Syntax mit Wiederholung (`MAX_REPEAT` = 256) und Formen (`MAX_SHAPE_VOLUME`) lässt es aber zu.
- Szenario: Zusammen mit F-02 füllt ein Client den Cache mit 24 verschiedenen Großcodes. Der Server läuft in `OutOfMemoryError` oder in lange GC-Pausen. Auch ohne Angriff: mehrere Spieler mit großen Blaupausen halten den Cache dauerhaft groß.
- Vorschlag: Gewichtsbegrenzung (Summe der Zellen, z. B. 8 Mio.), Modelle über `SoftReference`, Server- und Client-Cache trennen (heute teilen sie sich im Einzelspieler dieselbe Instanz).

**F-04 · Radiance-Licht übernimmt und zerstört fremde Licht-Blöcke und umgeht Schutz**
- Datei: `C/util/DynamicLightHandler.java:138-162` (`placeOrUpdate`, `removeLight`), `:60-90`
- Problem: `placeOrUpdate` akzeptiert einen bereits stehenden `Blocks.LIGHT` als "eigenen" (Zeile 141) und überschreibt dessen Helligkeit (Zeile 146-149). Beim Weiterlaufen entfernt `removeLight` jeden `LIGHT`-Block an der gemerkten Position und setzt Luft (Zeilen 154-162). Es gibt keine Prüfung, ob der Block von uns gesetzt wurde, und keine `WorldPermissions`/`BuildPermissions`-Abfrage.
- Szenario: Kartenbauer, Adventure-Maps und Server nutzen `minecraft:light` massiv für Beleuchtung. Ein Spieler mit leuchtender Rüstung läuft durch einen Raum mit Licht-Blöcken: Jeder Block, den sein Kopf passiert, bekommt eine andere Helligkeit und verschwindet danach (Luft). Das ist ein Datenverlust an Welteninhalt, auch in geschützten Bereichen (Claims). Zusätzlich: Stürzt der Server, bleiben gesetzte Licht-Blöcke für immer stehen, denn `lightSources` liegt nur im Speicher.
- Vorschlag: Eigene Markierung (z. B. eigener Block `simplebuilding:radiance_light` oder `Blocks.LIGHT` nur ersetzen, wenn Luft). Vorhandene fremde `LIGHT`-Blöcke nie anfassen. Vor dem Setzen `WorldPermissions.mayChange(..., BUILD)` fragen. Persistente Liste oder Block-Marker zum Aufräumen nach Absturz.

**F-05 · Rucksack-Codec verwirft unlesbare Einträge dauerhaft (Datenverlust)**
- Datei: `C/component/BackpackContents.java:79-101` (`LENIENT_LIST_CODEC`, Zeile 95 `resultOrPartial(... "Dropping unreadable backpack entry")`)
- Problem: Das Javadoc der Klasse sagt: "Doppelte oder fuer eine Stufe unbekannte Ids werden nie verworfen". Der Lenient-Decoder lässt aber jeden Eintrag fallen, der nicht dekodiert (Item-ID fehlt, Komponente einer entfernten Mod). Die nächste Speicherung schreibt die Liste ohne diese Einträge zurück.
- Szenario: Ein Modpack-Update entfernt oder deaktiviert vorübergehend eine Mod. Ein Spieler öffnet seinen Rucksack oder legt etwas hinein. Der Rucksack schreibt sich neu, und alle Gegenstände der fehlenden Mod sind für immer weg. Dasselbe passiert bei einer Datapack-Komponente, die beim frühen Laden noch nicht aufgelöst werden kann, und bei einem einzigen kaputten Komponenten-Patch an einem sonst gültigen Stapel.
- Vorschlag: Unlesbare Einträge als rohe Daten (`Dynamic`/`Tag`) mitführen und beim Speichern unverändert wieder anhängen, wie es das Javadoc verspricht. Mindestens in eine Sicherungsdatei neben dem Weltordner schreiben, bevor sie verworfen werden. Dasselbe Muster bei `TieredChest`/`TieredShulkerBox` prüfen (nicht geprüft).

**F-06 · Statischer Zustand mit Player/Level-Referenzen ohne Aufräumen (BlueprintBuilder u. a.)**
- Dateien: `C/blueprint/BlueprintBuilder.java:100,118-123` (`WARNED`, `JOBS`, `CHECKS`, `CLICK_VISITS`, `HINTED`), `M/Simplebuilding.java:142-148` (Aufräum-Hook kennt nur DynamicLight, SledgehammerUpgrades, EchoCompass, WandUndo), `C/items/custom/RotatorItem.java:89` (`PENDING_ECHOES`), `C/items/custom/BuildingCoreItem.java:363` (`PENDING`), `F/Protection.java:20` (`PROVIDERS`)
- Problem: `JOBS`/`CHECKS` halten `Player`, `Layout` (bis zu 4M Stellen) und `HINTED` hält `Player`. Entfernt werden die Einträge nur beim nächsten Bau-Klick eines beliebigen Spielers (`BlueprintBuilder.java:743-746`, `786-787`) oder wenn der Auftrag fertig ist. Es gibt keinen Disconnect- und keinen `SERVER_STOPPED`-Hook dafür. `WandUndo` hat genau diese Lücke 2026 als SBG-03 geschlossen ("Einzelspieler: sonst bliebe Welt A im Speicher"), die übrigen Klassen nicht.
- Szenario: Einzelspieler startet einen großen Blaupausen-Bau, verlässt die Welt mitten im Auftrag und lädt eine zweite Welt. `JOBS` hält den alten `ServerPlayer` und damit `ServerLevel` und alle geladenen Chunks von Welt A, bis dort jemand wieder baut. `PENDING` in `BuildingCoreItem` hat zwar einen `server != step.server()`-Filter, aber der greift erst beim nächsten Tick eines neuen Servers. Auf Dedicated Servern ist es ein kleineres, zeitweiliges Leck (Layouts großer Aufträge bleiben hängen).
- Vorschlag: In `ServerPlayConnectionEvents.DISCONNECT` und `SERVER_STOPPED` eine gemeinsame `SessionState.clear(player)`/`clearAll()` aufrufen, bei der sich alle Klassen anmelden (BlueprintBuilder, RotatorItem, BuildingCoreItem, LaunchSafety, PotionPadRules, TweaksNetwork-Fenster). `Protection.PROVIDERS` beim Server-Stopp leeren.

---

### MITTEL

**F-07 · Schutzprüfung nur am Chunk-Eck-Punkt (Claims unterhalb Chunk-Größe umgehbar)**
- Datei: `C/api/WorldPermissions.java:35-43` (`mayOccupy`), `:62-71` (`mayAutomateEntity`)
- Problem: Für jede belegte Chunk-Säule wird nur der Block `(x*16, box.minY, z*16)` gefragt (Zeile 41; in `mayAutomateEntity` mit `source.getY()`). `Protection.Target` ist blockgenau, ein Provider mit Claims, die kleiner als ein Chunk sind, sieht den wirklichen Aufenthaltsort des Spielers oder der Entität nie. Die Prüfung für Teleport (`mayTeleport`) und Entitäten (`mayAffectEntity`) hängt daran.
- Szenario: Ein Claim-Provider schützt eine Fläche, die den Chunk-Ursprung nicht enthält. Ein Teleport oder eine Aktion mit Entitäts-Wirkung dort wird erlaubt.
- Vorschlag: Statt der Chunk-Ecke die tatsächlichen Eck- und Mittelpunkte der Box (mindestens `box.minX/minZ`, `box.maxX/maxZ`, `box.getCenter()`) prüfen oder die Auflösung im Vertrag der Provider dokumentieren (Claims immer chunk-genau). Das Limit von 256 Chunks gilt weiter.

**F-08 · Gehaltener Attractor schickt pro Item und Tick ein Bewegungspaket**
- Datei: `C/items/custom/MagnetItem.java:99,178-185` (`syncVelocityToNearbyPlayers`)
- Problem: Für jedes angezogene Item wird jeden Tick ein `ClientboundSetEntityMotionPacket` an alle Spieler im Umkreis von 64 Blöcken geschickt, auch wenn sich die Bewegung kaum geändert hat. `PlacedAttractors.applyPull` (`C/util/PlacedAttractors.java`) macht es besser: `entity.push(...)` setzt das Sync-Flag, und ruhende Items bekommen keine Pakete mehr.
- Szenario: Ein Spieler mit Attractor auf einer Item-Farm (200 Items im Radius, 10 weitere Spieler in Reichweite) erzeugt 2.000+ Pakete je Tick, dauerhaft, solange die Items nicht am Ziel liegen. Zusätzlich `setPickUpDelay(0)` in jedem Tick.
- Vorschlag: Wie beim Platzierten Attractor `push()` nutzen und das Sync-Flag Vanilla überlassen. Bei Bedarf nur senden, wenn sich der Bewegungsvektor um mehr als einen Schwellwert ändert.

**F-09 · Amboss: Hammer + Hammer kombiniert Verzauberungen ohne Verträglichkeitsprüfung**
- Datei: `C/mixin/AnvilScreenHandlerMixin.java:74-124`
- Problem: Die Schleife über die Verzauberungen des rechten Hammers (Zeile ~106-118) setzt jede in den Builder, ohne `Enchantment.areCompatible`/`exclusiveSet` zu prüfen. Vanilla verwirft unverträgliche Paarungen.
- Szenario: Hammer A mit Behutsamkeit (aus dem Zaubertisch), Hammer B mit Glück. Im Amboss entsteht ein Hammer mit beidem. Auch andere ausschließende Gruppen (Schutz, Schärfe, falls auf Hämmern erlaubt) sind betroffen. Zusätzlich bleiben die Kosten immer bei 1 Stufe, egal wie viele Verzauberungen dazukommen.
- Vorschlag: Vor `builder.set` prüfen, dass `Enchantment.areCompatible(existing, enchantment)` für alle schon gesetzten gilt. Unverträgliche überspringen wie Vanilla (Kostenaufschlag +1). Die Hammer-Anvil-Logik gehört außerdem eigentlich in das Modul Simple Hammers (siehe F-18).

**F-10 · Trim-Muster/-Material werden per `String.contains` auf dem Asset-Pfad erkannt**
- Datei: `C/util/TrimEffectUtil.java:134-179` (`getTrimCount` Zeile 146-150, `getMaterialCount` Zeile 171-172), Aufrufer z. B. Zeilen 287-303
- Problem: Das Muster `"tide"`, `"eye"`, `"wild"`, `"ward"`, `"rib"`, `"host"` usw. wird mit `contains` auf `pattern().value().assetId().getPath()` verglichen, ohne Namensraum. Materialien ebenso (`"copper"`, `"quartz"`, `"resin"`, `"redstone"`, `"iron"`).
- Szenario: Ein Datapack oder eine fremde Mod bringt ein Muster `oddeye`, `forward_guard` (enthält `ward`), `tiger_ribbon` oder ein Material `smoky_quartz`/`rose_gold`. Es zählt als Vanilla-Muster bzw. -Material und gibt dessen Bonus. Mehrere Muster in einer ID zählen doppelt, wenn zwei Substrings passen.
- Vorschlag: Exakter Vergleich auf `Identifier` (Namensraum `minecraft` bzw. Mod-Namensraum) mit einer Tabelle Muster → Bonus.

**F-11 · Trim-Puls-Texturen: Cache wird bei Ressourcen-Reload nicht geleert, native Bilder bleiben**
- Datei: `C/client/render/TrimPulseTextures.java:33` (`CACHE`), `:76` (`TextureManager.register`), einziger Aufrufer `mc26_3/overlay/.../EquipmentRendererMixin.java:101`
- Problem: Der Cache hält `NativeImage` (off-heap) und registrierte `DynamicTexture`s je (Muster, Palette, Stufe). Er hat keinen Reload-Hook und kein `close()`. Nach einem Wechsel des Ressourcenpakets (F3+T) zeigt die Rüstung weiter die alten Farben, bis zum Neustart. Der Eintrag `MISSING` bleibt "kein zweiter Versuch je Sitzung", auch wenn das Paket die Datei nachliefert.
- Vorschlag: `ResourceManagerReloadListener` registrieren, der `CACHE` leert, `colored.close()` ruft und die Texturen deregistriert.

**F-12 · Daten-Fixer läuft auf 26.3 bei jedem `DataFixTypes.update`, auch bei aktueller Version**
- Datei: `C/datafix/ModDataFixer.java:79-84` (Frühausstieg `!MEGA_GUIDES && from >= to`), `:102` (`migrateGuide`), `:118-131`; Einbindung `C/mixin/DataFixTypesMixin.java`
- Problem: Auf 26.3 ist `MEGA_GUIDES` true (`mc26_3/overlay/.../McVersion.java:194`), damit entfällt der Frühausstieg. Jeder Chunk-, Entitäten-, Spieler- und Struktur-Ladevorgang läuft dann durch `walk` über den ganzen NBT-Baum, auch wenn `from == to`. In `migrateGuide` wird für jeden Item-Stapel (jedes Compound mit `count`) `Book.topics()` aufgerufen (neue `ArrayList` je Aufruf, `GuideBooks.java:167`) und je Buch ein String `NS + book.itemName()` gebaut, bevor `id.startsWith(NS)` geprüft wird. Zusätzlich `keySet().toArray(new String[0])` je Compound.
- Szenario: Jeder Chunk-Ladevorgang bezahlt einen vollständigen Baumdurchlauf mit Allokationen auf dem Worker/IO-Thread. Kosten steigen mit Kistenlast (jeder Item-Stapel). Das fällt vor allem bei Pregen und schnellem Fliegen auf.
- Vorschlag: In `migrateGuide` als Erstes `if (!id.startsWith(NS)) return;`, `Book.topics()` einmalig zwischenspeichern. Die Gästebuch-Migration über einen eigenen Marker (z. B. `SplitBackupMarker`-Muster pro Welt) einmalig ausführen statt bei jedem Update.

**F-13 · `@Redirect`/`@ModifyConstant` auf häufig angefassten Zielen (Mod-Kompatibilität)**
- Dateien (auf 26.3 aktiv): `C/mixin/AbstractArrowRecoveryMixin.java:17`, `C/mixin/PistonHandlerMixin.java:52` (`@ModifyConstant` auf Literal 12), `C/mixin/client/HudContextualBarMixin.java:28,39`, `C/tweaks/mixin/client/HudExperienceLevelMixin.java:25`. (Bow/Crossbow/AbstractArrowPickup, `BowItemMixin:36,46`, `CrossbowItemMixin:34,40`, `AbstractArrowPickupMixin:19`, sind auf 26.3 durch `AliasMixinPlugin.STORAGE_MIXINS` ausgeschaltet, auf 26.2 aktiv.)
- Problem: Ein zweiter `@Redirect` auf dieselbe Aufrufstelle durch eine andere Mod führt beim Mixin-Anwenden zum Absturz (`InjectionError`), ein zweiter `@ModifyConstant` auf dasselbe Literal zum Konflikt. Das Projekt kennt das Problem selbst (Kommentar in `EnderiteItemMixin.java:89`: "two @ModifyConstant on the same literal conflict") und löst es dort mit `@ModifyExpressionValue`, aber nicht überall. Beliebte HUD-Mods (Inventar-/Statusanzeigen) und Kolben-Mods greifen genau diese Ziele an.
- Vorschlag: `@Redirect` durch `@WrapOperation` und `@ModifyConstant` durch `@ModifyExpressionValue(at=CONSTANT)` ersetzen. Das ist eine mechanische, lokale Änderung.

**F-14 · Mod-Jar enthält Test- und Dev-Code in großem Umfang (prüfen)**
- Dateien: `C/gametest` (97.649 Zeilen), `M/gametest` (7.277), `C/dev` (7.487), dazu `src/main/resources/fabric.mod.json` mit `fabric-gametest`-Entrypoints. `build.gradle:121-130` zeigt, dass nur die *Client*-Tests (`gametest`-Sourceset) vom Jar getrennt sind, die Server-Tests liegen in `common/src/shared/java` und damit im Haupt-Sourceset.
- Problem: Das Release-Jar enthält vermutlich alle Server-Gametests, die Testzentrale-Befehle (`/sbtestcentre`, Rechtestufe 2) und die Test-Hooks `PistonEventProbe`/`ProtectionProbe` (registriert in `FabricBuildGuard.install`, `FabricPistonBreakGuard.install`, bei jedem Abbau abgefragt). Ich habe das Jar nicht gebaut, den Inhalt also nicht gesehen. Es ist eine Ableitung aus Quellsätzen und `build.gradle`.
- Vorschlag: Gametest und Dev in ein eigenes Sourceset auslagern oder per `jar { exclude ... }` ausschließen, und `/sbtestcentre` nur in der Entwicklungsumgebung registrieren (`ModEnvironment.isDevelopmentEnvironment()`). Jar-Inhalt (`unzip -l`) vor dem nächsten Release kontrollieren.

**F-15 · `DataTablesSyncPayload`: Einzeldatei-Grenze entspricht der Paketgrenze**
- Datei: `C/networking/DataTablesSyncPayload.java:20-21` (`stringUtf8(1 << 20)`)
- Problem: Pro Datei sind bis 1 MiB erlaubt, ein Paket (S2C Custom Payload) darf aber insgesamt höchstens 1 MiB haben. Alle Chisel- und Upgrade-Dateien aller Datapacks gehen in einem Paket. Heute sind es rund 30 KB (`src/main/generated/data/simplebuilding/...`), mit großen Modpack-Datapacks wächst es.
- Szenario: Bei zu großen Tabellen wird der Client beim Beitreten oder nach `/reload` mit "Payload may not be larger than..." getrennt.
- Vorschlag: Gesamtgröße beim Senden prüfen und bei Überschreitung in mehrere Pakete teilen oder die Datei-Limits senken und im Log warnen.

**F-16 · Fabric-Bauschutz feuert Abbau-Ereignisse für Platzierungen**
- Datei: `M/platform/FabricBuildGuard.java:39-42` (`mayPlace`)
- Problem: Die Platzier-Prüfung ruft `PlayerBlockBreakEvents.BEFORE` mit dem echten Spieler und dem aktuell stehenden Block auf. Das ist als Ersatz für ein fehlendes Platzier-Ereignis dokumentiert. Nebenwirkung: Jede andere Mod, die dort hört (Protokoll-/Rollback-Mods, Statistik, Quest-Fortschritt "Block abgebaut", ein zweiter Vein-Miner), sieht einen Abbau, der nicht stattfindet.
- Szenario: Ein Server mit CoreProtect-ähnlichem Logger protokolliert beim Baustab-Bauen für jede Zielzelle einen "Abbau" der Luft oder des ersetzten Blocks. Tracker-Mods zählen falsche Abbaue.
- Vorschlag: Hinweis in der Doku/Config (`pistonsFireBreakEvents`-Muster auch für Bauen), Option zum Abschalten. Besser: wenn möglich auf ein echtes Platzier-Ereignis der Zielmods warten (`UseBlockCallback` mit Dummy-Kontext) oder `Protection`/`WorldPermissions` als primären Weg bewerben.

**F-17 · Doppelte Implementierungen im Kern und in den Modulen (Konsistenzregel)**
- Dateien (Beispiele): `C/items/custom/ReinforcedBundleItem.java` (686 Zeilen) gegen `modules/simplestorage/shared/java/com/simplestorage/bundle/ReinforcedBundleItem.java` (30 KB), `C/util/PlacedBundles.java` gegen `modules/simplestorage/.../PlacedBundles.java`, `C/items/custom/QuiverItem.java` gegen Modul-`QuiverItem`, `C/mixin/BowItemMixin` usw. gegen Modul-Mixins; Hammer, Öfen, Blöcke analog über `STORAGE_IN_SB`/`HAMMERS_IN_SB`/`BLOCKS_IN_SB`/`OVENS_IN_SB`. Verwendungszahlen: `STORAGE_IN_SB` 166-mal, `TRIMS_IN_SB` 68, `HAMMERS_IN_SB` 36.
- Problem: Auf 26.3 (Hauptlinie) sind diese Konstanten false, und der `common`-Teil ist tot, bleibt aber kompiliert, wird mitgetestet und weiterentwickelt. Auf 26.2 ist er live. Jede Korrektur muss an zwei Stellen laufen; die Gefahr der Drift ist real (die Module haben bereits eigene `BundleNet`-Pakete mit neuen Namen, `ReinforcedBundleSelectionPayload` bleibt nur für den Farbkasten).
- Vorschlag: Sobald 26.2 nicht mehr gepflegt wird, den toten Zweig löschen (`ast-grep`-Suche nach `McVersion.*_IN_SB`-Verzweigungen), bis dahin ein Prüfskript, das die Zeilenzahlen und Signaturen beider Kopien vergleicht und bei Drift warnt. Gemeinsame Teile nach SimpleLib (Konsistenzregel).

**F-18 · Kern enthält Logik für Sub-Mod-Gegenstände (Modul-Unabhängigkeit)**
- Datei: `C/mixin/AnvilScreenHandlerMixin.java:63-197` (Hammer + Hammer, Hammer + Material), `C/mixin/ShapelessRecipeMixin.java`, `mc26_3/overlay/java/.../util/SledgehammerUsageEvent.java` (26.3-Stub, der an `com.simplehammers.engine` delegiert)
- Problem: Auf 26.3 gehören die Hämmer dem Modul Simple Hammers (`com.simplehammers.item.SledgehammerItem`), aber das Amboss-Verhalten für Hämmer lebt im Kern-Mixin. Ein Build von Simple Hammers ohne SimpleBuilding bekommt weder die Reparaturregeln noch das Kombinieren. `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md` verlangt, dass jedes Modul allein spielbar bleibt; ich habe die Doku nur kurz gelesen und nicht gegen die Standalone-Targets geprüft.
- Vorschlag: Amboss-Hammerlogik nach `modules/simplehammers` (eigenes Mixin, eigener Plugin-Schalter) verlegen. Der Kern behält nur den Strukturkompass.

**F-19 · `ChunkLoaderRegistry.reconcile` lädt beim Wecken alle Chunks synchron (Join-Spitze)**
- Problem: `ChunkLoaderRegistry.reconcile` (`C/tweaks/block/entity/ChunkLoaderRegistry.java:148-180`) gelesen. Es lädt beim Wecken `level.getChunk(entry.pos())` synchron für jeden Loader, dessen Besitzer wieder online kommt. Bei vielen Loadern pro Besitzer entsteht ein Tick mit vielen Chunk-Ladungen (Spike beim Join). Betrifft vor allem Server mit sehr vielen Loadern pro Besitzer.
- Vorschlag: Pro Tick nur N Loader wecken (Warteschlange).

**F-20 · Ungültig gewordene Fortsetzung: `AutonomousCrafter` verwirft Rest von `addItem(result)`**
- Datei: `C/blocks/entity/custom/AutonomousCrafterBlockEntity.java:131-160` (`tryCraft`, Zeile 147)
- Problem: `fits(into, result)` prüft vorab, dann wird `HopperBlockEntity.addItem(this, into, result, UP)` aufgerufen und der Rückgabewert (Rest) verworfen, während die Zutaten danach um 1 verringert werden. Passt `fits` und `addItem` nicht exakt zusammen (z. B. ein Ziel-Container mit eigenem `canPlaceItem` oder Mod-Hopper mit Filter und `getMaxStackSize(stack)`), geht das Ergebnis verloren. Für Rückstände (`remainders`) wird der Rest korrekt gedroppt, für das Ergebnis nicht.
- Szenario: Ein Fremd-Container unter dem Crafter weicht von der Berechnung in `fits` ab (z. B. `getMaxStackSize` kleiner als `stack.getMaxStackSize()`, nicht zusammenführbare Stapel). Zutaten verschwinden, das Ergebnis nicht.
- Vorschlag: Rückgabewert prüfen. Bleibt ein Rest, vor dem Abziehen der Zutaten abbrechen oder den Rest wie bei den Rückständen vor den Crafter droppen.

---

### NIEDRIG

**F-22 · `LivingEntityMixin`: drei `tick`-Injektionen für jedes Lebewesen, plus Namenslücken**
- Datei: `C/mixin/LivingEntityMixin.java:85,107,126` (drei `@Inject(method="tick", at=TAIL)`), `:184` (`modifyVoidDamage` ohne `simplebuilding$`-Präfix)
- Problem: Jedes `LivingEntity.tick` (Mobs, Rüstungsständer, Spieler) läuft durch drei getrennte Injektionen: Wither-Gegenheilung (`tickCount % 20`), Amethyst-Heilung (`% 200`), Radiance/`TrimAttributeHandler.tick` (`% 10`). Jede prüft zuerst `isClientSide` und lässt dann Modulo-Zahlen laufen; die Arbeit pro Aufruf ist klein, aber die drei Hooks lassen sich zu einem zusammenführen. Zusätzlich heilt der Wither-Zweig pauschal 0,5 HP alle 20 Ticks, unabhängig von Stufe und Dauer ("Workaround", Kommentar Zeile ~95). `modifyVoidDamage` hat keinen `simplebuilding$`-Präfix, damit kann der Name mit Methoden fremder Mixins im selben Ziel kollidieren.
- Vorschlag: Einen Hook, der nach Modulo verzweigt. Methodennamen mit Präfix. Wither-Zweig über `MobEffectInstance`-Accessor sauber kürzen.

**F-23 · `BlockItemMixin` kopiert bei jeder Platzierung den Stapel in einen ThreadLocal**
- Datei: `C/mixin/BlockItemMixin.java:49-51`
- Problem: `place`-HEAD kopiert `context.getItemInHand().copy()` (bei Shulker-/Rucksack-Items mit großen Komponenten eine tiefe Kopie), auch wenn der Spieler gar keinen Rucksack trägt und das Ergebnis nie gebraucht wird. Bei einer Ausnahme zwischen HEAD und RETURN bleibt der ThreadLocal belegt (harmlos, wird beim nächsten Platzieren überschrieben).
- Vorschlag: Nur kopieren, wenn `WornStorage.entries(...)` nicht leer ist, oder die Kopie auf den Server beschränken (`!level.isClientSide()`).

**F-24 · `SpeakerBoost.inRelayRange`: Brute-Force je Kettenschritt**
- Datei: `C/util/SpeakerBoost.java:195-228` (Aufrufer `chain` Zeile 166)
- Problem: Je Kettenschritt (bis `maxChain`, Standard 16, Maximum 64) wird für jeden Abschnitt, der einen Lautsprecher enthalten kann, jede der 4.096 Positionen mit `new BlockPos` und `distSqr` geprüft. Server-seitig gibt es einen Cache (`cachedChain`, 100 Ticks), der aber bei jedem gesetzten oder abgebauten Lautsprecher global für alle Welten ungültig wird (`GENERATION`). Auf dem Client (`mc26_3/overlay/.../ChainedJukeboxSound.java:62`, `LevelEventHandlerSpeakerMixin.java:32`) wird ohne Cache gerechnet. Die Aufrufer habe ich nur an den genannten Stellen gesehen.
- Vorschlag: Positionen nach Blockzustand über die Palette suchen (`section.getStates().forEachLocation`/`maybeHas` mit Zähler), nur `MutableBlockPos` nutzen, Cache je Welt statt global invalidieren.

**F-26 · `WornHeads.extra`: Listen-Allokation je Aufruf und stilles Schlucken von Fehlern**
- Datei: `F/WornHeads.java:59-79` (ebenso `skullType`, `skullFactor`, `skullEdge`)
- Problem: Jeder Aufruf baut eine neue `ArrayList`, auch wenn kein Provider Köpfe liefert; Ausnahmen aus Providern werden mit leerem `catch (RuntimeException ignored)` verschluckt, ohne Log. Ein fehlerhafter Provider bleibt dadurch unbemerkt.
- Vorschlag: Leere Liste wiederverwenden (`List.of()`), beim ersten Fehler pro Provider einmal loggen.

**F-27 · `ConfigFiles.quarantine`: Namenskollision innerhalb einer Sekunde**
- Datei: `F/ConfigFiles.java:62-70`
- Problem: Der Zeitstempel hat Sekundengenauigkeit. Werden zwei kaputte Dateien gleichen Namens in derselben Sekunde verschoben (Neuladen und Nachladen), scheitert `Files.move` mit `FileAlreadyExistsException`, `quarantine` liefert "(not moved)", und der Aufrufer schreibt danach seine Standardwerte über die kaputte Originaldatei, die der Benutzer eigentlich retten wollte. Selten, aber der Doku-Satz "old file kept as ..." stimmt dann nicht.
- Vorschlag: Millisekunden oder Zähler im Namen, `StandardCopyOption.REPLACE_EXISTING` nicht nutzen, stattdessen hochzählen.

**F-28 · Test-Hooks laufen im Produktionspfad**
- Dateien: `M/platform/FabricBuildGuard.java:34-35`, `M/platform/FabricPistonBreakGuard.java:25-27`
- Problem: `ProtectionProbe.refused(pos)` und `PistonEventProbe.breakRefused(pos)` sind als normale `BEFORE`-Listener für jeden Abbau registriert. Sie sind billig (`isEmpty()`-Kurzschluss), aber es ist Testcode in jeder Spielsitzung, und jede Mod, die `BEFORE` aufruft, durchläuft ihn.
- Vorschlag: Registrierung nur in der Entwicklungsumgebung oder im Gametest-Lauf.

**F-29 · `FabricItemAutomation`: `Transaction.openOuter()` in verschachtelter Lage**
- Datei: `M/platform/FabricItemAutomation.java:26,39`
- Problem: `openOuter()` wirft `IllegalStateException`, wenn auf dem Thread bereits eine Transaktion offen ist (etwa wenn die Automation aus einem Callback einer anderen Transfer-Operation aufgerufen wird). Ich habe keinen konkreten Aufrufpfad mit offener Transaktion gefunden, der Fall ist nicht belegt.
- Vorschlag: `Transaction.openNested(Transaction.getCurrentUnsafe())` verwenden, falls vorhanden, sonst `openOuter()`.

**F-30 · `BuildPermissions.probing` als globaler Zähler, nicht thread-lokal**
- Datei: `C/util/BuildPermissions.java:24-31`
- Problem: Ein statischer `int` zählt laufende Prüfungen. Er stimmt nur, solange alle Aufrufer auf dem Server-Thread laufen (heute der Fall). Im Einzelspieler teilen Client- und Server-Thread dieselbe Klasse; `isProbing()` wird nur serverseitig gefragt, aber ein Aufruf aus dem Client-Thread (z. B. Vorhersage in Mausmixin) würde den Zähler ungeschützt verändern.
- Vorschlag: `ThreadLocal<Integer>` oder `assert`/Warnung bei Aufruf von einem anderen Thread.

**F-31 · `Protection.PROVIDERS` (Framework): nicht thread-sicher und ohne Aufräumen beim Server-Stopp**
- Datei: `F/Protection.java:20-22`
- Problem: `IdentityHashMap` mit `LinkedHashMap` pro Server, ohne Synchronisation. Die Registrierung geschieht normalerweise auf dem Server-Thread, ein Provider aus einer Mod mit eigenem Thread (Datenbankabfragen) könnte aber gleichzeitig lesen. Ohne `unregister` beim Stopp hält die Map `MinecraftServer` und alle Welten fest. (Hängt mit F-06 zusammen.)
- Vorschlag: `ConcurrentHashMap`/`CopyOnWriteArrayList`, Eintrag beim `SERVER_STOPPED` entfernen.

---

### Nicht geprüft

- Alles ab `modules/` (Hammer, Storage, Claims usw.) und die Loader NeoForge/Forge sowie ältere MC-Linien, wie vorgegeben.
- `mc26_3/overlay` nur an einzelnen Stellen (`McVersion`, `SledgehammerUsageEvent`, `EquipmentRendererMixin`-Aufrufstelle), nicht systematisch.
- Die große Masse der Client-Klassen (Screens, Renderer, `BlockHighlightRenderer`, `GuideBookScreen`): nur auf statische Caches durchgesehen.
- `ChiselItem`, `InWorldTransformations`, `OreDetectorItem` (Teil), `ReinforcedBundleItem`, `BuildingWandItem` (nur `inventoryTick`), `ShapeFill`, `BlueprintCode` (nur Grenzen), `GuideBooks`, `Discover`: nicht Zeile für Zeile gelesen.
- `ModBlocks`/`ModItems`/`ModBlockEntities` Registrierung: Stichprobe auf nullbare Konstanten (`X = FLAG ? register : null`). Ich habe 67 solche Felder gefunden und unbedingte Dereferenzierungen gesucht; die einzige Fundstelle (`PlacedBundles.java:170`) ist durch `STORAGE_IN_SB` geschützt. Zeilenweise Prüfung aller Zugriffe steht aus.
- Komplettes Schadens-Balancing (`TrimEffectUtil`) und Rezepte/Datagen.
- Keine Laufzeitprüfung: Alle Funde stammen aus dem Lesen des Codes. Insbesondere F-01 (Dauer der Suche), F-02/F-03 (Speicherbedarf der Modelle) und F-14 (Jar-Inhalt) sind rechnerische Ableitungen und sollten mit einem Gametest oder einem Profil auf sb-test bestätigt werden, bevor daraus Prioritäten entstehen.
- Thread-Zusicherung des Fabric-Empfängers: Der Code schaltet selbst auf den Server-Thread (`context.server().execute`). Ob Fabric bereits auf dem Server-Thread aufruft, habe ich nicht geprüft; es ist unkritisch.

### Reihenfolge der Behebung (Vorschlag)

1. F-01 (Amboss-Suche), F-02/F-03 (Blaupausen-Parse), danach F-04 und F-05 (Datenverlust). Das sind kleine, lokale Änderungen mit großer Wirkung.
2. F-06 (zentraler Aufräum-Hook) und F-12 (DataFixer-Frühausstieg), beide mit wenig Aufwand.
3. F-08, F-09, F-10, F-13: gezielte Einzelfixes.
4. F-14, F-17, F-18: strukturelle Aufräumarbeit, abhängig von der Entscheidung zur 26.2-Linie.

## Code-Review Teil 2: Sub-Module unter `modules/` (Stand Bündel 53, Worktree /root/wt/b-review24)

Umfang: MC 26.3 + Fabric, nur lesen (kein Gradle, keine sb-test-Läufe). Alle Pfade relativ zu `/root/wt/b-review24/`.
Methode: Modulgrößen erfasst (43 Module, ~194k Zeilen; größte: simplestorage 24,8k, simpletweaks 19k, simplemobs 13,3k,
simplemaps 10,6k, simplecrucibles 10k, simpleshops 10k, simplemusic 9,6k, simplelib 8,4k). Priorisiert gelesen: simplelib-API,
simpleshops, simplemoney, simplemusic, simplemaps (Netz/Web), simpletweaks (Deed, Admin, Protect, Claims-Mixins), simplestorage
(Backpack), simplecrucibles, simpleinterfaces (Linked), simpletools (Death Vault), simplehammers (Upgrades), simpleoptserver,
simpletrims (Licht), simplerespawn/mobheads/mobs/wanderers stichprobenartig. Jeder Fund unten wurde am Code nachgelesen
(Datei:Zeile); Vanilla-Verhalten, das nicht im Repo liegt (keine MC-Quellen im Worktree), ist als "nach Vanilla-Wissen" markiert.

Ergebnis in Zahlen: 0 kritisch, 4 hoch, 12 mittel, 8 niedrig (24 Funde).

## Was bewusst GUT ist (geprüft, kein Fund)

- **Modul-Unabhängigkeit (Regel 3):** Skript über alle Module: kein einziger Klassen-Import über Modulgrenzen außer
  `com.simplelib.api.*` / `framework.api` (einzige Ausnahme siehe N-2). Module ohne `depends: simplelib` binden simplelib per
  Jar-in-Jar (`include project(':modules:simplelib:fabric')`), kein Absturz bei fehlendem Partner. Fremde Ids (`simplebuilding:*`)
  werden nur über Registry-Lookups mit `isLoaded`-Bedingung benutzt (z. B. `OctetMaterials`, `OctetRegistry:227`).
- **Client-Code im Server-Pfad:** alle 317 Mixins aus den Fabric-`*.mixins.json` (Gemeinsam-Teil) geprüft: keiner importiert
  `net.minecraft.client`. Kein Fund.
- **Server-Pakete:** Deed (`DeedService`), Musik-Upload, Maps-Waypoints/-Display/-Rahmen, Backpack/Bundle, Fear-FOV: Eingaben
  werden gedrosselt, begrenzt (Codec-Limits), Hand 0/1 geprüft, Item aus der Hand neu gelesen. Einzige Lücke: F-2.
- **Shops/Geld:** `ShopTrade.core` ist transaktional (prüfen, simulieren, Commit mit Rollback), Preis/Menge nur aus Weltdaten,
  Revisionsprüfung gegen Race; Menüs (`ShopBuyerMenu`, `TradeTerminalMenu`, `ShopMenu.ViewSlot`) verhindern Klicks auf
  Anzeige-Slots, `onClosed` gibt Zahl-Slots zurück. Kein Dupe gefunden.
- Claims-Abdeckung (Projektile, Transport/Kupfergolem, Kolben, Trichter, Griefing) ist breit; Rechte kommen aus dem Ledger.

---

## HOCH

### H-1  Backup schaltet `noSave` ein und stellt es nur im Worker-Callback zurück: Server-Stop während des Backups verliert Welt-Fortschritt
- Datei: `modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/admin/backup/Backups.java:56` (setzt `level.noSave = true`), `:69` (Rücksetzen nur in `server.execute(...)` vom Worker), `:42-44` (`stop()` ruft nur `worker.shutdown()`); `admin/Admin.java:38-43` ruft `Backups.stop()` erst bei `SERVER_STOPPED`.
- Problem: Während das ZIP geschrieben wird (Minuten bei großen Welten), sind alle Level auf `noSave`. Stoppt der Server in dieser Zeit (`/stop`, Konsole, systemd, Neustart-Wächter), läuft Vanillas Abschluss-Speichern mit `noSave` -> nach Vanilla-Wissen überspringt `saveAllChunks(false, true, false)` Level mit `noSave` (nur `forced=true` speichert trotzdem; `Restart.stopServer:98` nutzt `forced=true`, normales `/stop` nicht). `Backups.stop()` kommt erst nach dem Speichern und setzt `noSave` nicht zurück.
- Szenario: Backup alle N Stunden läuft; Admin tippt `/stop` oder der Host bekommt SIGTERM; alles seit dem letzten Autosave/`saveEverything` zu Backup-Beginn (Bauwerke, Inventare, Shop-/Claim-Daten) ist weg. Zusätzlich liest das ZIP weiter Dateien, während der Prozess beendet wird (`.part`-Rest).
- Vorschlag: `paused`-Liste statisch halten und in einem `SERVER_STOPPING`-Handler (vor dem Speichern) `noSave=false` setzen und den Lauf abbrechen; oder statt `noSave` den Weg "save-all flush, dann Snapshot-Kopie" gehen. Test ergänzen: Stop während Backup.

### H-2  Maps: jeder Spieler kann beliebige Wayfinder-Karten lesen (Karten-Id wird nur auf Existenz geprüft) und dabei Daten dauerhaft in den Speicher laden
- Datei: `modules/simplemaps/shared/java/com/simplemaps/net/MapsNetwork.java:52-63` (`handleTiles`), `WayfinderIds.java:25` (`exists`: nur `0 <= id < next`), `WayfinderData.java:97` (`computeIfAbsent(type(id))`).
- Problem: Der Server liefert Kacheln (`TileDataPayload`) und Marken (`MapStatePayload` mit `data.marks()`, `data.dimension()`) zu jeder `mapId` aus dem Paket. Es wird nicht geprüft, dass der Spieler diese Karte hält/sieht. Ids sind fortlaufend (0..N-1).
- Szenario: Ein Client (oder Mod) fragt 48 Ids je Paket alle 4 Ticks ab (`REQUEST_INTERVAL`, `MAX = 48`): liest die erkundeten Gebiete und Struktur-Marken aller Spieler (Basen, Fundorte). Jede angefragte Karte wird als `SavedData` von der Platte geladen und bleibt im `DimensionDataStorage` gecacht; bei vielen/großen Karten Speicher- und Platten-I/O-Druck auf dem Server-Thread (DoS).
- Vorschlag: nur Ids beantworten, die der Spieler gerade im Inventar/Off-Hand/Rahmen in Reichweite hat (Set pro Spieler beim Öffnen/Halten), Rest verwerfen; `render`-Aufrufe pro Paket zählen, nicht nur gesendete Bytes.

### H-3  Ender-Shops: ein einziger nicht lesbarer Item-Eintrag macht den GESAMTEN Shop-Bestand unlesbar
- Datei: `modules/simpleshops/shared/java/com/simpleshops/logic/EnderShops.java:65` (`ItemStack.CODEC` je Slot), `:95` (Transit), `:307-311` (alle Vaults in EINER `listOf`); ebenso `logic/ServerShops.java:51`.
- Problem: Alle Waren/Einnahmen/Preise aller Spielershops liegen in einem SavedData-Blob mit strikten Codecs. Ein Eintrag mit unbekannter Item-Id (entfernte Fremd-Mod, in MC-Update umbenanntes Item) lässt die ganze Liste scheitern. SavedData, das nicht decodiert, wird nach Vanilla-Wissen als leer behandelt und beim nächsten Speichern überschrieben -> sämtliche Shops (bis 200 000 Vaults) verlieren Inhalt. Die Backpack-Autoren haben genau dieses Risiko erkannt und `BackpackContents` mit eintragsweisem Lenient-Codec gelöst (`simplestorage/.../backpack/BackpackContents.java`, LENIENT_LIST_CODEC); Shops fehlt das.
- Szenario: Ein Spieler verkauft ein Item einer Fremd-Mod; Mod wird entfernt -> beim nächsten Start sind alle Shops leer/Geld weg.
- Vorschlag: `SlotStack`/`Stored` eintragsweise lenient lesen (wie `LENIENT_LIST_CODEC`), unlesbare Einträge mit Warnung in eine `lost.json`/Quarantäne schreiben statt verwerfen; Sicherungskopie der `.dat` vor Überschreiben.

### H-4  Block-Entity-Inhalte mit strikten Listen-Codecs: ein schlechtes Element löscht den ganzen Inhalt (Tiegel, Fass, Schneidebrett, Kiste)
- Dateien: `modules/simplecrucibles/shared/java/com/simplecrucibles/crucible/CrucibleBlockEntity.java:598` (`input.read("Contents", SavedSlot.CODEC.listOf())`) und `:617` (`UpgradeUnits`), `CrucibleBarrelBlockEntity.java:189,197`, `modules/simplesandwiches/shared/java/com/simplesandwiches/block/CuttingBoardBlockEntity.java:140`, `modules/simplestorage/shared/java/com/simplestorage/crate/CrateBlockEntity.java:323` (`Order`).
- Problem: `ValueInput.read(name, listCodec)` scheitert als Ganzes (Optional.empty) bei einem einzigen unlesbaren Element; Vanilla (`ContainerHelper`/`ValueInput.list`) liest nach Vanilla-Wissen elementweise und überspringt Defektes. Hier verschwinden dann alle Slots. Zusätzlich kürzt `CrucibleBlockEntity.java:601` Mengen beim Laden auf `getMaxStackSize(stack)`: sinkt dieser Wert (Config/Tier/Datapack), wird der Überschuss still gelöscht.
- Szenario: Tiegel mit 9 Stacks enthält 1 Item einer entfernten Mod -> Tiegel lädt leer, Rest ist weg.
- Vorschlag: `input.list("Contents", SavedSlot.CODEC)` (elementweise) bzw. Lenient-Codec nutzen; Mengen-Clamp nicht beim Laden, sondern nur beim Einlegen.

---

## MITTEL

### M-1  Mehrere `@Redirect` auf häufig genutzte Vanilla-Aufrufe: Konfliktgefahr mit anderen Mods (harter Absturz "Redirect conflict")
- Dateien: `modules/simplestorage/shared/java/com/simplestorage/mixin/BowItemMixin.java:36,46` und `CrossbowItemMixin.java:34,40` (`Player/LivingEntity.getProjectile`), `AbstractArrowPickupMixin.java:19` (`Inventory.add` in `tryPickup`), `FallingStorageDropMixin.java:26` (`spawnAtLocation`); `modules/simpleriding/shared/java/com/simpleriding/mixin/RidingHelperMixin.java:11` (`ItemStack.get` in `getEnchantmentCost`/`selectEnchantment`), `RidingPacketMixin.java:40` (`onGround()`).
- Problem: Zwei Mods mit `@Redirect` auf dasselbe Ziel crashen beim Start; Bogen-/Pfeil-/Verzauberungs-Mods sind typische Kandidaten. Die Module benutzen sonst schon `@WrapOperation` (z. B. `BundleTooltipViewMixin`, `ServerLevelMixin`).
- Vorschlag: auf `@WrapOperation` (MixinExtras) umstellen; bei den Köcher-Mixins zusätzlich ThreadLocal-Zustand (`usedQuiver`, `quiverProjectile`) in `try/finally` räumen.

### M-2  Aktivierungsbereich (S5b) überspringt den ganzen `Entity.tick()` für ferne Mobs, ohne Effekte/Zähler auszunehmen
- Datei: `modules/simpleoptserver/shared/java/com/simplebuilding/modules/simpleoptserver/ActivationRange.java:34-47`, `mixin/activation_range/ServerLevelMixin.java`.
- Problem: `exempt` nimmt Fahrzeuge, Ziele, Wasser, Feuer, Babys, Dorfbewohner aus, aber nicht: aktive Trank-Effekte (Gift/Wither/Regeneration laufen in `LivingEntity.tick` -> bei jedem 4. Tick zu langsam), Paarungs-Abkühlzeit erwachsener Tiere und Eier-Legen bei Hühnern (`aiStep`), Lore-/Zähler-Logik in Fremd-Mod-Mobs. Standard ist "aus" (laut Doku), aber schaltbar.
- Szenario: Farm 40 Blöcke entfernt vom Spieler: Zucht-/Eier-Raten sinken auf ein Viertel, Giftschaden/Heilung tickt nicht.
- Vorschlag: Mobs mit `!getActiveEffects().isEmpty()` und mit laufendem Brut-/Eier-Timer ausnehmen; Doku-Hinweis in der Config.

### M-3  Anti-X-Ray rechnet pro Chunk und Spieler 4096-Blöcke-Schleifen auf dem Server-Thread
- Datei: `modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/protect/AntiXray.java:99-185` (`obfuscate` -> `section` -> `enclosed`).
- Problem: Im Engine-Mode 2 zählt Stein als "Host", `section.maybeHas(rules::concealable)` ist damit für fast jeden Untergrund-Abschnitt wahr: je Abschnitt 4096 `getBlockState` + bis zu 6 Nachbar-Lookups, je Chunk bis `maxY`; keine Zwischenspeicherung, jeder Spieler berechnet denselben Chunk neu. Läuft beim Chunk-Senden (Server-Thread).
- Szenario: 20 Spieler laden gleichzeitig nach einem Neustart/Teleport Chunks -> spürbare MSPT-Spitzen.
- Vorschlag: Ergebnis pro Chunk+Version cachen (Spieler-unabhängig, nur Exempt-Spieler bekommen Original), Bit-/Palette-Vorabprüfung, Chunk-Nachbarn nur bei Randblöcken nachschlagen; Messwert in `bench-results` ergänzen.

### M-4  Linked GUIs: Berechtigung (Claim/Schloss) wird nur beim Markieren und beim Anhängen geprüft, nicht mehr bei Zugriffen einer bestehenden Sitzung
- Datei: `modules/simpleinterfaces/shared/java/com/simplebuilding/modules/simpleinterfaces/linked/LinkedContainers.java:90-97` (`Session.valid()`), `:203-213` (`valid(player, mark)`), `:153` (Prüfung nur beim Markieren), `:317` (beim Anhängen), `:286-312` (`syncInventory` behält die Sitzung an `inventoryMenu`, solange `current.mark == mark`).
- Problem: `valid()` prüft Reichweite, Chunk und Block-Entity, nicht `InteractionGuard.permission`/`canOpen`. Die an das Spieler-Inventar angehängte Sitzung bleibt, solange die Markierung hält.
- Szenario: Spieler wurde in einem Claim vertraut, markiert die Kiste; der Besitzer entzieht die Rechte -> der Spieler kann weiter Items aus der Kiste ziehen (Inventar-Bildschirm), bis er die Markierung löscht oder wegläuft.
- Vorschlag: Berechtigung alle N Ticks (z. B. in `tick`) bzw. bei jedem `LinkedSlot`-Zugriff erneut prüfen; bei Verlust Markierung beenden.

### M-5  Strahlen-Licht (Trims): Lichtblöcke bleiben nach Absturz stehen; Entfernen kann Chunks synchron laden
- Datei: `modules/simpletrims/shared/java/com/simpletrims/light/TrimLight.java:38` (Spieler-Licht nur in statischer `HashMap`), `:104-123` (`tickPlayer`), `:149-165` (`placeOrUpdate`/`removeLight` rufen `level.getBlockState/setBlock` ohne `hasChunkAt`-Prüfung).
- Problem: (a) Die Position des Lichts steht für Spieler nur im RAM; Kill/Absturz zwischen Setzen und Entfernen lässt echte `minecraft:light`-Blöcke dauerhaft in der Welt (kein Aufräum-Scan). (b) Nach `/tp` über große Distanz ruft der nächste Tick `removeLight(level, old)` auf der alten Position; ist ihr Chunk bereits entladen, lädt `getBlockState` ihn synchron (Server-Hänger). (c) Die Tabelle kennt keine Dimension; `removeLight` nutzt die aktuelle Level-Referenz.
- Vorschlag: `level.hasChunkAt(old)` prüfen (sonst Eintrag für später merken); Licht-Position zusätzlich im Spieler-Attachment speichern oder Lichtblöcke mit Marker/ Tag erkennen und beim Chunk-Laden bereinigen.

### M-6  Web-Karten-Server: Slowloris/Thread-Aushungern und Rate-Limit greift zu spät
- Datei: `modules/simplemaps/shared/java/com/simplemaps/web/WebHttp.java:64` (4 Worker, Queue 32), `:145` (`setSoTimeout(TIMEOUT_MS)` = Timeout pro Lese-Aufruf), `:227-240` (`readLine` Byte für Byte), `:162` (`limit.allow` erst NACH Lesen von Request-Zeile und Headern), `:283` (Token in der URL).
- Problem: Ein Angreifer sendet alle ~4 s ein Byte; jede Verbindung hält einen der 4 Worker bis zu 2048 Bytes + 64 Header lang (Stunden). Wenige Verbindungen genügen, um die Web-Karte (und bei Reverse-Proxy alle Besucher, die dieselbe IP haben) lahmzulegen. Rate-Limit pro IP zählt erst nach vollständigem Header-Empfang. Token als `?token=` landet in Logs/Browser-Verlauf (Referrer-Policy ist gesetzt, mildert nur).
- Vorschlag: Gesamt-Deadline pro Verbindung (z. B. 5 s ab accept, per `System.nanoTime` in `readLine` prüfen), Rate-Limit direkt nach `accept`, `Authorization`-Header bevorzugen.

### M-7  Musik-Upload: Vorab-Allokation, nie abgelaufene Uploads, Verarbeitung auf dem Server-Thread
- Datei: `modules/simplemusic/shared/java/com/simplemusic/upload/UploadSession.java:31` (`new ByteArrayOutputStream(declared)` mit client-gemeldeter Größe bis `maxUploadMiB` = 16), `MusicServer.java:164` (Aufräumen abgelaufener Uploads nur in `handleUploadBegin`), `:188` (`handleUploadChunk` ohne `SimpleMusic.on()`), `:213-229` (`OggCheck.check`/`strip`/`store.add` mit Datei-I/O auf dem Server-Thread).
- Problem: Jeder Spieler (Config `uploads=EVERYONE`) allokiert mit einem `UploadBegin` sofort bis zu 16 MiB, ohne ein Byte zu senden; Abbrecher/Trennende bleiben bis zum nächsten fremden Begin in `UPLOADS`. 16 MiB Ogg-Prüfung plus Schreiben (Hash, Datei) laufen synchron.
- Szenario: 50 Spieler starten Uploads und trennen: 800 MiB Heap bleiben hängen; große Uploads erzeugen Tick-Spitzen.
- Vorschlag: Puffer wachsen lassen (nicht vorab), Ablauf im `tick` und bei Disconnect, `finish` auf Worker-Thread, `on()`-Prüfung auch in `handleUploadChunk`.

### M-8  Block-Log: Abschluss-Daten und Warteschlange
- Datei: `modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/admin/blocklog/BlockLog.java:68-77` (`stop()` ruft `collect()` nicht auf: aufsummierte Trichter-Bewegungen seit dem letzten Flush gehen verloren; `QUEUE.clear()` direkt danach), `:45` (`QUEUE` unbegrenzt: Spieler-Aktionen haben kein Budget, nur Welt-Ereignisse), `:115`.
- Problem/Szenario: Ein hängender/voller Datenträger lässt den IO-Thread zurückfallen; die Queue wächst unbegrenzt (Heap). Beim Stop fehlen Einträge der letzten Flush-Periode, genau die, die man nach einem Absturz sucht.
- Vorschlag: obere Grenze (z. B. 200 000 Einträge, älteste verwerfen + Warnung), `collect()` vor dem Abschluss-Drain.

### M-9  Death Vault / Death Ledger: statischer Zwischenzustand, strikter Codec, Voll-Neuserialisierung
- Dateien: `modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/vault/DeathVault.java:35,49,54` (statisches `collecting`), `mixin/PlayerDeathDropsMixin.java:22` (`@At("RETURN")` läuft nicht, wenn `dropEquipment` wirft -> `collecting` und Soulbound-Zustand bleiben hängen), `vault/DeathLedger.java:93` (strikter `ItemStack.CODEC` für das ganze Ledger), `:135` (`reindex()` über ALLE Spieler bei jedem Tod), `unloaded()` setzt `setDirty()` je entladener Drop-Entity.
- Problem: (a) ein Fremd-Item im Ledger macht das ganze Ledger beim Laden unlesbar (Information weg, Items bleiben in der Welt; daher nur mittel); (b) Ledger behält die ganze Stack-Kopie samt Inhalt (Shulker/Rucksack) je Spieler dauerhaft; (c) jede Chunk-Entladung eines Drops serialisiert den ganzen Ledger neu.
- Vorschlag: `RETURN`-Inject durch `try/finally`-Wrapper oder `@WrapMethod` ersetzen; Ledger-Einträge lenient lesen; `reindex` nur für den betroffenen Spieler.

### M-10  Nether-Dach: gemerkter "sicherer Ort" wird nie neu geprüft
- Datei: `modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/play/roof/NetherRoof.java:18` (`SAFE`, nie geleert), `:31` (Merken bei `onGround()`), `:43-45` (`moveDown` teleportiert dorthin, wenn <= 64 Blöcke entfernt).
- Problem: Die Position kann Stunden alt sein; inzwischen kann dort Lava stehen/ein Block gesetzt sein. Kein Sicherheitscheck wie in `below(...)`. Map wächst je UUID und wird bei Server-Wechsel (Singleplayer, andere Welt) nicht zurückgesetzt.
- Vorschlag: Ort vor dem Teleport validieren (zwei Luftblöcke, tragender Block, keine Flüssigkeit) und sonst `below()` nutzen; bei `SERVER_STOPPED`/Disconnect räumen.

### M-11  Berechtigungsprüfung über das echte `PlayerBlockBreakEvents.BEFORE` (Fabric)
- Dateien: `modules/simpletools/fabric/src/main/java/com/simplebuilding/modules/simpletools/ToolsFabric.java:28`, `modules/simplefarming/fabric/src/main/java/com/simplebuilding/modules/simplefarming/FarmingFabric.java:12`, `modules/simpleinterfaces/fabric/src/main/java/com/simplebuilding/modules/simpleinterfaces/InterfacesFabric.java` (`InteractionGuard.permission = ...BEFORE.invoker().beforeBlockBreak(...)`).
- Problem: Die "Darf ich?"-Probe löst alle BEFORE-Handler aus, darunter solche mit Nebenwirkung (Hammer-Flächenabbau in `SledgehammerUsageEvent.handleBeforeBlockBreak`, Fremd-Mods mit Chat-Meldungen/Protokoll). Der Hammer schützt sich über `HammerActions.areaVetoed()`, andere Handler nicht. Forge/NeoForge senden dagegen ein Event (anderes Verhalten pro Loader).
- Vorschlag: gemeinsame Probe in simplelib/`framework.api.Protection` (reine Abfrage), die ohne Nebenwirkung arbeitet; bis dahin Handler-Liste dokumentieren.

### M-12  Dorf-Wanderer: Suche und Block-Zugriffe ohne Chunk-Schutz
- Datei: `modules/simplewanderers/shared/java/com/simplewanderers/logic/WandererSpawner.java:65-80` (je Spieler, bei jedem neuen 8-Block-Raster oder `REVISIT`: POI-Suche + `getStructureWithPieceAt` + `getEntitiesOfClass` in Radius+32), `:132-146` (`findSpot`: `getBlockState` ohne `hasChunkAt`/`isLoaded`).
- Problem: Wandert ein Spieler, löst jede 8-Block-Zelle eine POI-/Struktursuche aus (Strukturreferenzen können Chunks nachladen). `findSpot` liest bis 12x7 Positionen im Umkreis 8 um die Glocke; liegt die an einer Chunk-Grenze mit ungeladener Nachbarschaft, lädt `getBlockState` synchron.
- Vorschlag: `level.isLoaded(pos)` vor jedem Zugriff, Suche pro Spieler und Dimension zusätzlich zeitlich drosseln (z. B. 5 s).

---

## NIEDRIG

### N-1  Statische Pro-Spieler-/Welt-Zustände werden bei Stop/Disconnect nicht überall geräumt
- `modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/FearNet.java:27` (`FOV`, `forget` wird nirgends aufgerufen), `modules/simplewanderers/.../WandererSpawner.java:49,51` (`MISSES`, `LAST`), `modules/simpletweaks/.../play/pvp/Pvp.java:17` (`TOGGLED`), `play/roof/NetherRoof.java:18`, `modules/simplemobheads/shared/java/com/simplemobheads/ritual/HerobrineHaunt.java:40` (`end` in Spiel-Ticks: nach Weltwechsel im selben JVM kann eine Heimsuchung "ewig" laufen), `modules/simpletrims/.../TrimLight.java:38`.
- Folgen klein (Speicher je UUID, im Singleplayer Zustand der vorigen Welt). Gute Beispiele zum Nachziehen: `Afk.left`, `Votes.left`, `SledgehammerUpgrades.onDisconnect`.
- Vorschlag: ein gemeinsamer Hook in simplelib (`PlayerState.onLeave/onStop`) statt je Modul.

### N-2  `simplecrucibles` importiert interne simplelib-Klassen statt der API
- `modules/simplecrucibles/shared/java/com/simplecrucibles/crucible/CrucibleJob.java:3,5` (`com.simplelib.config.LibConfig`, `com.simplelib.warm.Warm`), `CrucibleBlockEntity.java:5` (`Warm`); Tests: `test/CrucibleTests.java:14-15`.
- Regel 3 erlaubt nur `com.simplelib.api.*`. Bricht bei internem Umbau von simplelib (Pflicht-Abhängigkeit, daher kein Laufzeit-Risiko heute).
- Vorschlag: `Warm`/`LibConfig`-Zugriff als API-Methoden in `com.simplelib.api` bereitstellen.

### N-3  `options.txt` wird von zwei Modulen per String-Ersetzung umgeschrieben (Duplikat, nicht atomar)
- `modules/simplestorage/shared/java/com/simplestorage/backpack/BackpackKeyMigration.java:33` und `modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/KeyMigration.java` (nahezu identisch, `Files.writeString(options, migrated)`).
- Problem: kein temp+move; Absturz beim Schreiben zerstört die Tasten-/Grafikoptionen des Spielers; `text.replace` ersetzt jedes Vorkommen. Doppelter Code.
- Vorschlag: ein Helfer in simplelib mit atomarem Schreiben und Zeilen-Parser; beide Module nutzen ihn.

### N-4  Config-Dateien werden nicht atomar geschrieben (~30 Stellen)
- Beispiele: `modules/simplemobs/.../MobsConfig.java:85,118`, `simplehammers/.../HammerConfig.java:59`, `simplestorage/.../StorageConfig.java:60`, `simplemaps/.../MapsConfig.java:97`, `simplelib/.../LibConfig.java:54` (je `Files.writeString(file, ...)`). Gegenbeispiele mit temp+move: `PlayState.java:63`, `AdminFiles.java:14`, `DimensionRuntime.java:89`, `TrackStore.java:208`.
- Problem: Absturz beim Schreiben hinterlässt halbe JSON; Lader behalten laut Doku teils die Datei und nehmen Defaults (stilles Zurücksetzen der Einstellungen).
- Vorschlag: einen `ConfigFiles.writeAtomic` in `framework.api` (dort liegt bereits `ConfigFiles.adoptLegacy`) und die Module umstellen.

### N-5  Kopierter Code, der nach simplelib/Framework gehört
- `InteractionGuard` ist in `modules/simplefarming/shared/java/com/simplebuilding/modules/simplefarming/InteractionGuard.java`, `simpletools/.../InteractionGuard.java` und `simpleinterfaces/.../linked/InteractionGuard.java` zeichengleich (16 Zeilen, nur `package` verschieden); `isModLoaded`/`partnerMissing`-Testhelfer 5x identisch (`FarmingTests`, `ToolsTests`, `ModelTests`, `DimensionTests`, `VisualsTests`); `ModelBrowser` in simplemodels und simplevisuals.
- Vorschlag: Guard + Testhelfer einmal in simplelib (API) bzw. Test-Util; Loader setzen nur noch die `permission`-Probe.

### N-6  Backup-Worker: `shutdown()` ohne Warten, ZIP liest lebende Dateien
- `Backups.java:42-44`, `:90-108` (`zip`): `worker.shutdown()` beendet den laufenden Auftrag nicht; Dateien wie `playerdata/*.dat` und `data/*.dat` werden bei laufendem Server gelesen (nur Chunks sind durch `noSave` eingefroren) -> mögliche inkonsistente Sicherung; `.part`-Reste werden von `prune` nicht entfernt. `server.saveEverything(true, true, true)` in `begin` blockiert den Server-Thread bei großen Welten.
- Vorschlag: `awaitTermination` mit Timeout in `stop()`, `.part` aufräumen, Hinweis/Option für "save-off + flush" vor dem Kopieren der `.dat`-Dateien.

### N-7  Maps: Waypoint-/Display-Handler ohne `MapsConfig.enabled`-Prüfung
- `modules/simplemaps/shared/java/com/simplemaps/net/MapsNetwork.java:79-111` (`handleWaypoint`, `handleDisplay`): `handleTiles`, `handleSelectDimension`, `saveFrameView` prüfen `enabled`, diese beiden nicht. Folge: bei abgeschaltetem Modul schreiben Clients weiter Komponenten auf Karten in ihrer Hand. Gering (nur eigene Items).
- Vorschlag: einheitlicher `if (!enabled) return` am Anfang jedes Handlers (auch `LayerService.handle`).

### N-8  Sledgehammer-Fläche: statisches `HARVESTED_BLOCKS` ohne Dimension
- `modules/simplehammers/shared/java/com/simplehammers/engine/SledgehammerUsageEvent.java:25,37,58,76`: `Set<BlockPos>` ohne Level; durch `finally`-Entfernen und Ein-Thread-Betrieb heute unkritisch, wird aber falsch, sobald `destroyBlock` (Fremd-Mod-Handler) in ein anderes Level springt (Portal/Mod-Dimension).
- Vorschlag: `record Key(ResourceKey<Level>, BlockPos)` oder `ThreadLocal`-Tiefenzähler wie `HammerActions.areaBreaking`.

---

## Gesamteinschätzung je großem Modul (1 Zeile)

- **simplelib:** sehr sauber (IdMoves, InWorldStrikes mit Tick-Aufräumen, API-Grenze); offen: Hilfsklassen für Wiederholtes (Guard, atomares Schreiben, Spielerzustand).
- **simplestorage:** Menüs/Quickmove sehr sorgfältig (Backpack-Codec lenient, gesperrter Slot, Übergrößen); Risiken nur bei `@Redirect`-Konflikten (M-1) und Crate-Order-Codec (H-4).
- **simpleshops:** Handel transaktional und gut abgesichert; größtes Restrisiko ist der zentrale strikte SavedData-Codec (H-3).
- **simplemoney:** klein und sauber (Budget, Preise validiert); keine Funde.
- **simplemusic:** Eingaben begrenzt, Hash-/Dateinamen sicher; Upload-Ressourcen (M-7).
- **simplemobs:** Server-Tick und Aufräumen vorbildlich (`FearServer`); kleine Reste (N-1); Entitäten/Disguise-Mixins nur oberflächlich gesehen.
- **simpleend / simpleenddragonfight:** nur Mixin-Stichprobe (`EnderDragonMixin` unauffällig).
- **simpletweaks:** größtes Modul, überwiegend hohe Qualität (Deed, Claims, Admin); echte Lücken: Backup-Stop (H-1), Anti-X-Ray-Kosten (M-3), Block-Log (M-8), Nether-Dach (M-10).
- **simpleoptimization/-server/-memory/-render:** klein, Standard "aus" für riskante Bereiche, Mixin-Plugins mit Rivalen-Erkennung; Aktivierungsbereich (M-2).
- **claims-Teile (simpletweaks `claims/`, `mixin/claims`):** Abdeckung breit, Rechte aus Ledger; Linked-GUI-Lücke (M-4) liegt in simpleinterfaces.
- **simplemaps:** Netz-Handler gut validiert bis auf die fehlende Besitzprüfung (H-2); Web-Server solide Whitelist, aber Slowloris (M-6).
- **simplecrucibles:** Logik gut; Persistenz anfällig (H-4), interne simplelib-Imports (N-2).

## NICHT geprüft

- NeoForge/Forge-Sourcesets, ältere MC-Versionen, `mc26_4` (laut Auftrag).
- Alle Client-/Render-Pfade (Screens, Renderer, Shader), Texturen, Sounds, JEI/Jade/FTB-Anbindungen, Wiki-/Guide-Inhalte.
- Nur Stichprobe oder gar nicht: `simplemobs` (Entities, Disguise, Wither-Logik), `simplemobheads` (Rituale außer Heimsuchung), `simpleend`, `simpledimensions`, `simplebeacons`, `simpleoctets`, `simplesandwiches`, `simpleblocks` (ohne Säge-Menü), `simplequestionary`, `simplestats` (Laufzeit nur überflogen), `simpleriding` (`RidingSecurity` nicht gelesen), `simplefun`, `simplevisuals`, `simpleoptrender`-Mixins, `simpletweaks` Play-Features außer Dach/Pvp/Afk/Votes, `ClaimStore`/`Claims.java` (40 kB) nur über Mixins/Deed-Service.
- Nicht ausgeführt: keine Tests, kein Gradle, keine GameTests; alle Funde sind statisch aus dem Code belegt. Vanilla-Verhalten (H-1 `saveAllChunks`, H-3/H-4 SavedData-/`ValueInput`-Semantik) ist nach Vanilla-Wissen formuliert und sollte beim Umsetzen gegen die 26.3-Quellen bestätigt werden (je ein GameTest: Stop während Backup, Welt mit entferntem Item laden).
