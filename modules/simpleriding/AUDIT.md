# Audit simpleriding (a-audsb31e1, 12.11.2026)

Basis `hub/b-bundle188` 9dfb3f643, nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases). Aufbau wie `modules/simplepads/AUDIT.md`.
Umfang: 44 Java-Dateien / 2,0k Zeilen (shared + fabric, ohne Tests; Tests zusaetzlich 4 Dateien / 0,8k Zeilen). Risikogeleitet
(P1-Klassen zuerst: Hufeisen-Menue und Schnellverschieben, Server-Bewegungsgrenzen, Abklingzeiten/Sprungpakete). Zusatzeingabe BEW39
(`/root/sb-tools-a/bewertung39/simpleriding.json`, keine roten Eintraege) wurde gegen den Code geprueft.

## Vollstaendig gelesen
- Hufeisen: `Horseshoes` (ganz), `HorseshoeSlot`, `HorseshoeHolder`, `HorseshoePanel`, `mixin/HorseshoeMenuMixin`, `HorseshoeQuickMoveMixin`,
  `HorseshoeHorseMixin`, `HorseshoeXpMixin`, `HorseshoeBrushMixin`, `fabric/.../HorseshoeDataMixin`; Client: `HorseshoeScreenMixin`,
  `HorseshoeClickMixin`, `HorseshoeExtractMixin` (nur ob Client-Abschnitt und keine Serverlogik).
- Netz/Bewegung: `RidingSecurity` (ganz), `mixin/RidingPacketMixin`, `RidingJumpInputMixin`, `RidingNautilusMixin`, `NautilusDashAccess`,
  `RidingCamelMixin`, `RidingLeapMixin`.
- Effekte/Daten: `RidingEffects` (ganz), `RidingTickMixin`, `RidingFallMixin`, `RidingHelperMixin`, `RidingEnchantmentMixin`, `Riding`,
  `RidingConfig`, `RidingLoot`, `fabric.mod.json`, `simpleriding.mixins.json`, `enderite_horseshoe_smithing`-Rezept.
- Nicht gelesen: `WeightedEnchantFunction` (Kern-Dublette), `RidingOptions`, `RidingGuide`, `RidingDiscover`, `RidingBookModels` und
  Client-Renderer (`HorseshoeLayer`, `HorseshoeDonkeyLayerMixin`, `RidingBookModelMixin`, `RidingConfigScreen`), Lang/Texturen,
  Forge/NeoForge, Tests (nur `HorseshoeTests#menu`, `RidingTests#movementBudget`, `#steeringAndBorder`).

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Keine eigenen Payloads; zwei Vanilla-Pakete werden **nach** dem Thread-Wechsel (`ensureRunningOnSameThread`, `shift = AFTER`) abgefangen: `ServerboundMoveVehiclePacket` (RidingPacketMixin.java:18) und `ServerboundPlayerCommandPacket` START_RIDING_JUMP (:26). Budgets je **Verbindung** (Feld im Listener, nicht je Reittier): Pakete/Tick (<= 20, Konfig), Strecke/Tick (<= 4), Horizontalgrenze `0,125 + 3 x Tempo`, Welt-/Chunk-/Randpruefung, NaN/unendlich/Neigung. Sprung: Ladung 1..100 (`RidingSecurity.java:110`, zusaetzlich `RidingJumpInputMixin` begrenzt 0..100), ein Sprung je Tick, Abklingzeit des Reittiers, Boden unter dem Tier. Korrekturpaket hoechstens einmal je Tick. S2C: nur Vanilla `ClientboundMoveVehiclePacket`/Motion. |
| C2 Server-Autoritaet | Wirkung (Punkte, Gelaendebonus, Verschleiss, Mending, Fallschaden) rechnet nur der Server (`Horseshoes`); der Client liest den synchronisierten Code (Entity-Daten, 22 Bit). Hufplaetze haengen am Vanilla-Menue (`stillValid` = Abstand zum Pferd), die Slots entstehen auf beiden Seiten gleich. Client-Mixins nur im `client`-Abschnitt. |
| C3 Dupes | Kein Dupe gefunden. Schnellverschieben (`HorseshoeQuickMoveMixin.java:17-33`): Hufeisen -> freie Hufplaetze, Huf-/Sattel-/Truhenplaetze -> nur Spielerinventar (Bereiche `playerStart..+36`, Hufplaetze liegen dahinter, nie im Vanilla-Zielbereich); die Quelle wird ueber dasselbe Stack-Objekt geleert (`moveItemStackTo`), `setByPlayer(EMPTY)`/`setChanged`, Rueckgabe `EMPTY` bei Misserfolg stoppt Vanillas Wiederholschleife; Hufplatz hoechstens 1 Stueck (`getMaxStackSize(stack) = 1`, `mayPlace` nur Hufeisen). Speichern/Laden: nur Hufeisen, Anzahl 1, hoechstens 4 (HorseshoeHorseMixin); Tod: `Horseshoes.drop` entnimmt (`removeItemNoUpdate`) und wirft aus, "Verschwinden" wird beachtet. Mending: dieselbe Formel wie Vanilla (`ExperienceOrb#repairPlayerItems`). Archaeologie: ersetzt den einen Fund, fuegt nichts hinzu. |
| C4 NBT/Komponenten | Hufeisen im Pferd: `ItemStack.OPTIONAL_CODEC.listOf()`, beim Laden auf 4 Eintraege und auf Hufeisen begrenzt. Konfig `normalize()` kappt alle Werte (inkl. `blocksPerDurability` 8..400, Pakete 1..20, Strecke 0,5..4). |
| C5 Tick-Kosten | `RidingTickMixin` ruft `RidingEffects.tick` fuer **jedes** Lebewesen jeden Tick (P3-1); Hufeisen-Zaehler nur fuer geritte Pferde, Sync-Vergleich je Pferd-Tick. Keine Suchen, keine Partikel ausser einer kleinen Wolke beim Sprung. |
| C6 Leaks | Keine statische Spieler-/Entity-Tabelle. `RidingSecurity` hat der Listener der Verbindung (stirbt mit ihr); `Riding.CONFIG` wird beim Config-Befehl ersetzt. |
| C7 Sicherheit/Rechte | Kein eigener Befehl (`/simpleriding config` ueber SimpleLib, Stufe 2). Keine Weltaenderung ausser den Hufeisen-Funden (Beutetabellen, Archaeologie). Mixins: 15 Server, 8 Client; `RidingEnchantmentMixin`/`RidingHelperMixin` nur fuer Reittier-Ruestung und Sattel; `RidingPacketMixin` zielt auf zwei Vanilla-Handler mit Abbruch (`cancellable`) nur fuer unterstuetzte Reittiere (Pferd, Nautilus, Schwein, Schreiter, Happy Ghast). |
| C8 Unabhaengigkeit | Allein spielbar (Standalone-Ziel, SimpleLib per Jar-in-Jar). Fehlendes `depends simplelib` war P2-1 (gefixt). Enderit-Hufeisen haengt an SimpleBuilding statt an Simple Ores (P3-3). |
| C9 Doku/Pflege | Keine TODO/FIXME. 1 Kern-Dublette (`WeightedEnchantFunction`). Lang EN=DE (`check_data.py` gruen). Metadaten-Abweichungen P3-5. |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simpleriding-P2-1 | **P2 (Absturz bei fehlender Abhaengigkeit)** | `RidingConfig.java:43` ruft beim Start (`RidingFabric` -> `RidingConfig.load`) `com.simplelib.api.config.ConfigCommands.server(...)`, `fabric.mod.json` deklarierte SimpleLib nicht -> ohne installierte SimpleLib `NoClassDefFoundError`-Absturz statt Fabric-Abhaengigkeitsmeldung. Standalone-Tests merken das nicht (SimpleLib liegt dort im Klassenpfad bzw. nistet im Jar). | **gefixt durch Hauptagent** (`fabric.mod.json` `depends simplelib >=0.4.0`); NeoForge/Forge-Metadaten nur Port-TODO. |
| simpleriding-P3-1 | P3 (Tick) | `RidingTickMixin.java:8` ruft `RidingEffects.tick` (RidingEffects.java:17-39) fuer **alle** Lebewesen: je Aufruf `getItemBySlot(SADDLE)`, sechs Attribut-Zugriffe (`apply` -> `getAttribute` + `removeModifier`), mehrere Konfig-Lesungen - auch fuer Zombies, Dorfbewohner usw. Bei 3000 Lebewesen etwa 0,3-0,6 ms je Tick (geschaetzt, nicht gemessen). Die Modifier entstehen nur auf unterstuetzten Reittieren, der Aufruf ist fuer alle anderen wirkungslos. | Liste: Frueher Ausstieg `if (!RidingSecurity.supported(e)) return;` am Anfang von `RidingEffects.tick` (Verhalten unveraendert; nach Messung entscheiden) |
| simpleriding-P3-2 | P3 (Besitzer, CL-7) | Fahrgrenzen bei Latenz (BEW39 orange): Das Horizontalbudget gilt je **Server-Tick** (RidingSecurity.java:81: `horizontalDistance + horizontal > min(cap, 0,125 + 3 x Tempo)`). Pferd mit Tempo 0,3375: Obergrenze 1,14 Bloecke je Tick, gerechnetes Spitzentempo ~0,54 Bloecke/Tick - zwei Pakete im selben Server-Tick passen (1,08), drei (Netz-Jitter ab ~100 ms) werden abgelehnt, ein Korrekturpaket je Tick laesst das Reittier zurueckruckeln. Umgekehrt erlaubt der Spielraum ~2,1-faches Tempo (Vanilla erlaubt ~10 Bloecke je Paket). `movementBudget`/`movementPackets` testen die Logik, nicht echte Latenz. | **Rueckfrage** F1 |
| simpleriding-P3-3 | P3 (Unabhaengigkeit/Besitzer) | Enderit-Hufeisen (BEW39 V): Registrierung nur mit SimpleBuilding (`Riding.java:30` -> `Horseshoes.register(SIMPLEBUILDING)`, Horseshoes.java:66), Rezept mit `fabric:all_mods_loaded simplebuilding` (`enderite_horseshoe_smithing.json`), Zutat aber `#c:ingots/enderite` (Simple Ores). Mit Simple Ores allein nicht herstellbar; die Registrierung haengt am Mod, nicht am Tag. | **Rueckfrage** F2 |
| simpleriding-P3-4 | P3 | `HorseshoeQuickMoveMixin.java:31`: `slot.setByPlayer(ItemStack.EMPTY)` (Ein-Argument-Form) liest den alten Stapel nach `moveItemStackTo` schon leer; das Abnehmen von Sattel/Pferderuestung per Schnellverschieben loest dadurch kein Ausruesten-Ereignis/Geraeusch mit dem alten Stapel aus (Vanilla uebergibt ihn). Nur Klang, kein Item-Verlust. | Hinweis |
| simpleriding-P3-5 | P3 (Metadaten) | `modules/modules.json` `requires: ["cloth_config"]`, `fabric.mod.json` verlangt kein Cloth (Mod-Menu-Einstieg prueft `isModLoaded("cloth-config")`, `RidingModMenu.java:5`); eigene Repo-URLs/Platzhalter (RELEASE-CHECK), MODSTRUKTUR-Text ohne Hufeisen. Keine Simple-Money-Preise fuer Hufeisen/Vorlage (BEW39, "nicht gebaut"). | Hauptagent (`modules.json`) |
| simpleriding-P3-6 | P3 | 1 Kern-Dublette (`WeightedEnchantFunction`); die Pflegeregel der Dubletten betrifft keinen Fix dieses Audits. | Liste |

## Besitzer-Fragen (fuer die Vorschau AUDSB31E1)
**F1 Fahrgrenzen bei Latenz (simpleriding-P3-2, CL-7).** Kontext: Das Server-Budget je Tick kann bei Netz-Jitter (>= 3 Pakete in einem Server-Tick bei Spitzentempo) zurueckrucken lassen und erlaubt gleichzeitig ~2,1-faches Tempo; echte Latenz wurde nie getestet.
- A: Abnahme-Lauf mit kuenstlicher Verzoegerung (z. B. 100 ms Jitter, Pferd im Galopp) vor dem Release; bei Rucken B.
- B: Budget ueber zwei Ticks gleitend (z. B. `horizontalCap` x 2 ueber ein Fenster von 2 Ticks) oder `0,125 + 4 x Tempo`; Sprung-/Randpruefungen unveraendert.
- C: so lassen und im Store-Text "bei hohem Ping kann es zurueckrucken" ausweisen.
- Empfehlung: **A**, bei Befund **B** - die Pruefungen (Rand, Chunk, NaN, Sprung) bleiben der eigentliche Schutz, das Tempo-Budget darf grosszuegiger sein.

**F2 Enderit-Hufeisen und Simple Ores (simpleriding-P3-3).** Kontext: Das Hufeisen-Item existiert nur mit SimpleBuilding; Enderit gehoert zu Simple Ores.
- A: Item immer registrieren (wie die uebrigen Stufen), Rezept auf `#c:ingots/enderite` (Tag-Inhalt) statt auf den Mod-Namen bedingen.
- B: Registrierung und Rezept an `simpleores` binden.
- C: lassen (nur mit SimpleBuilding), im Handbuch sagen.
- Empfehlung: **A** - jedes Modul bleibt allein spielbar, ohne Enderit-Quelle ist das Item schlicht nicht herstellbar; Achtung: Welten mit SimpleBuilding behalten dieselbe id.

## Antworten des Besitzers (12.11.2026, E1ANS)
- **F1 (Vorschau F16) = A**: Abnahme mit kuenstlicher Verzoegerung ist sbt-TODO (bei Rucken Option B).
- **F2 (F17) = A**: Enderit-Hufeisen immer registriert (`Horseshoes.register()`), Rezept/Advancement mit `fabric:tags_populated c:ingots/enderite`
  (NeoForge/Forge: `not tag_empty`, ungetestet), Tag `horseshoes` fuehrt es fest; Gametest `horseshoe_enderite_always`, Rezept-/Item-Tests angepasst.

## Urteil
**shipbar** (0 P1, 1 P2 gefixt durch den Hauptagent, 6 P3). Menue und Schnellverschieben sind dupe-sicher, die Bewegungsgrenzen laufen nach dem Thread-Wechsel und je Verbindung; offen ist nur die Abnahme unter Latenz (F1).

## Testergebnis
Lokal: keine Java-Datei des Moduls geaendert (kein javac noetig); `modules/simpleriding/tools/check_data.py` gruen. sbt: siehe `docs/ai/PLAN-A-AUDSB31E-2026-11-12.md`.
Nicht getestet: echte Latenz/Jitter, echte Maus im Hufeisen-Panel (RELEASE-SB 4.4), Hufeisen-Darstellung an den Hufen (Client), Mehrspieler-Zugriff auf dieselben Hufplaetze, Forge/NeoForge.
