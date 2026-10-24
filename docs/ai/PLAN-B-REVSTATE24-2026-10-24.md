# Plan b-revstate24: Pro-Spieler-Zustand (N-1 Rest) und Planung N-5 / F-17 / F-18 (24.10.2026)

Quelle: /root/sb-tools-b/review24 (teil1-kern F-17/F-18, teil2-module N-1/N-5). Basis: Hub-Branch b-revlow24 (dort schon `FearNet.forget` und `HerobrineHaunt.stop` am Fabric-DISCONNECT). Nur 26.3 Fabric.

## A) REV24-N-1 Rest: gemeinsamer Hook (umgesetzt)

Ist-Stand der Fundstellen: `FearNet.FOV`, `HerobrineHaunt.HAUNTED` (beide am Disconnect schon geraeumt, aber nicht bei Server-Stop), `NetherRoof.SAFE` (nie geraeumt), `TrimLight.PLAYER_LIGHTS` (Spieler-Eintrag raeumt `EntityRemovedTrimsMixin` bei jedem Entfernen, Tabelle ueberlebt aber den Stop theoretisch), `Pvp.TOGGLED`/`WandererSpawner` (raeumen bei Stop bzw. bewusst nicht beim Verlassen: Pvp-Cooldown darf durch Neu-Anmelden nicht umgangen werden; simplewanderers hat keine simplelib-Abhaengigkeit -> nicht gekoppelt).

Umsetzung:
- `com.simplelib.api.PlayerState` (simplelib shared): `onLeave(id, Consumer<ServerPlayer>)`, `onStop(id, Runnable)`, Loader-Aufrufe `left(player)`/`stopped()`. Listener je id ersetzbar, Ausnahme eines Listeners wird geloggt und uebersprungen.
- Fabric-Verdrahtung nur in `SimpleLibFabric`: `ServerPlayConnectionEvents.DISCONNECT` -> `left`, `ServerLifecycleEvents.SERVER_STOPPED` -> `stopped`.
- Registrierung in den Fabric-Einstiegen der Module (alle haben `simplelib` als Pflicht-Abhaengigkeit im mod.json): simplemobs (`FearNet.forget/forgetAll`), simplemobheads (`HerobrineHaunt.stop/stopAll`), simpletweaks (`NetherRoof.forget/forgetAll`), simpletrims (`TrimLight.forgetAll`). Die eigenen DISCONNECT-Registrierungen aus b-revlow24 entfallen (ein Weg statt zwei).
- Gametest `player_state_leave_stop` (simplelib): Listener laufen, werfender Listener blockiert andere nicht, ersetzte id laeuft einmal.

Risiken: Listener laufen auf dem Server-Thread (DISCONNECT/SERVER_STOPPED); `TrimLight.forgetAll` loescht nur die Tabelle, keine Licht-Bloecke (die gehen mit der Welt).

TODO NeoForge/Forge (nicht umgesetzt, Besitzer-Vorgabe nur 26.3 Fabric): `PlayerEvent.PlayerLoggedOutEvent` -> `PlayerState.left`, `ServerStoppedEvent` -> `PlayerState.stopped` in den simplelib-Loadern; die Modul-Registrierungen (`PlayerState.onLeave/onStop`) muessten dort aus den Loader-Einstiegen kommen (bisher nur Fabric). Alternativ spaeter in die shared `init()` der Module ziehen.

## B) Planung (kein Code)

### REV24-N-5: kopierter InteractionGuard (3x), Test-Helfer (5x), ModelBrowser (2x)

Ist-Stellen:
- `InteractionGuard` zeichengleich (nur `package` verschieden): `modules/simplefarming/shared/java/.../simplefarming/InteractionGuard.java`, `modules/simpletools/shared/java/.../simpletools/InteractionGuard.java`, `modules/simpleinterfaces/shared/java/.../simpleinterfaces/linked/InteractionGuard.java`. Loader setzen nur die `permission`-Probe (Fabric: `PlayerBlockBreakEvents.BEFORE`, NeoForge: `BreakBlockEvent`, Forge eigener Check).
- Test-Helfer `isModLoaded`/`partnerMissing`: `FarmingTests`, `ToolsTests`, `ModelTests`, `DimensionTests`, `VisualsTests` (je 2 Zeilen identisch).
- `ModelBrowser` in simplemodels und simplevisuals.

Zielort:
- Guard: `com.simplelib.api.InteractionGuard` (simplelib). simpletools und simpleinterfaces buendeln simplelib schon (`include project(':modules:simplelib:fabric')`); **simplefarming nicht** -> Farming bekommt simplelib als Pflicht-Abhaengigkeit (mod.json `depends`, build.gradle include) oder behaelt die Kopie. Prinzip Modul-Unabhaengigkeit: simplelib ist laut Konsistenzregel erlaubt/gebuendelt, aber Standalone-Targets von Farming muessen weiter laufen -> vorher klaeren.
- Testhelfer: `com.simplelib.test.PartnerChecks` (oder Framework-Testutil, das die Standalone-Tests ohnehin laden); Module rufen nur noch `PartnerChecks.partnerMissing(h, mod, what)`.
- ModelBrowser: eigener Plan (UI, Client) - nach simplelib `api.client.ui`, nur wenn beide Seiten dieselben Daten liefern.

Schritte: (1) Guard in simplelib kopieren, `permission` als statisches Feld behalten (Loader-Code bleibt, nur Import aendert sich); (2) je Modul Kopie loeschen, Importe + Loader-Setzer (3 Module x 3 Loader = 9 Stellen) anpassen; (3) Farming-Abhaengigkeit entscheiden; (4) Testhelfer-Klasse anlegen, 5 Test-Dateien umstellen; (5) Tests `module-simplefarming|simpletools|simpleinterfaces-fabric-263` + `-standalone-` + `simplelib`.
Risiken: Farming-Standalone (neue Pflicht-Abhaengigkeit, Doku/Modrinth-Beschreibung); Loader-Setzer in Forge/NeoForge brechen unbemerkt, solange nur 26.3 Fabric gebaut wird (spaeter Port-Lauf noetig); die Kopie in `FarmingForgeChecks` referenziert die Klasse per Namen.
Aufwand: Guard + Helfer ca. 2-3 h inkl. Tests; ModelBrowser getrennt ca. 2 h.

### REV24-F-17: doppelte Implementierungen Kern gegen Module

Ist-Stellen (Zeilen, `common/src/shared/java/com/simplebuilding/...` gegen Modul): `items/custom/ReinforcedBundleItem.java` 686 gegen `modules/simplestorage/.../bundle/ReinforcedBundleItem.java` 700; `items/custom/QuiverItem.java` 352 gegen Modul-`QuiverItem`; `util/PlacedBundles.java` 311 gegen Modul; Mixins (`BowItemMixin` usw.), Hammer-, Ofen-, Block-Zweige. Steuerung ueber `McVersion.*_IN_SB`: 56 Verzweigungen (Zaehlung im common-Teil: STORAGE 27, HAMMERS 13, OVENS 12, TRIMS 7, BLOCKS 5; siehe auch Mixin-Plugins `AliasMixinPlugin`, `BlockStateBaseMixin`, `CarvedStairMixin`). Auf 26.3 sind die Konstanten false, der Kern-Teil ist toter Code, auf 26.2 live.

Zielort/Strategie (abhaengig von der Entscheidung zur 26.2-Linie, Review nennt dasselbe):
1. Kurzfristig (klein, 1-2 h): Pruefskript `tools/check_dup_impl.py`: je Paar Zeilenzahl + Signaturen (public/protected-Methodennamen) vergleichen, in `./gradlew check` einbinden, warnt bei Drift. Paare in einer Liste `tools/dup_pairs.txt`.
2. Mittelfristig: Gemeinsames nach simplelib auslagern, was beide Kopien identisch haben (z. B. Auswahl-/Fuellstandslogik der Buendel als reine Funktionen), Item-Klassen bleiben je Linie.
3. Endgueltig, sobald 26.2 nicht mehr gepflegt wird: `ast-grep`-Suche `McVersion.$X_IN_SB` -> Zweig mit `true` behalten, Kern-Kopien und Konstanten loeschen (Muster erst an einem Beispiel testen, Modul fuer Modul: storage, hammers, ovens, blocks, trims), nach jedem Modul `check` + Standalone-Tests.
Risiken: Schritt 3 loescht Code der 26.2/1.21.11-Linie (irreversibel fuer die Linie) -> nur nach Besitzer-Freigabe; Datafix/Alias-Plugins (`AliasMixinPlugin`) haengen an den Konstanten; Wiki/Guide-Generatoren lesen teils Kern-Klassen.
Aufwand: 1) 1-2 h, 2) 1 Tag je Teilbereich, 3) 2-3 Tage mit Tests.

### REV24-F-18: Hammer-Logik im Kern-AnvilScreenHandlerMixin -> Simple Hammers

Ist-Stelle: `common/src/shared/java/com/simplebuilding/mixin/AnvilScreenHandlerMixin.java` (387 Zeilen, `@Mixin(AnvilMenu)`): Hammer+Hammer (ab Zeile 65, `SledgehammerItem.isHammer` Zeile 72/75), Hammer+Material, `mayPickup` (200), `createResult` RETURN mit Hammer-Ausgabe (210-217); weitere `createResult`-HEAD-Injects bei 177 und 309 (Nicht-Hammer: Kern behaelt). Dazu `ShapelessRecipeMixin` (Kern) und `SledgehammerUsageEvent` (26.3-Overlay, delegiert an `com.simplehammers.engine`). Modul hat schon eigene Mixins (`simplehammers.mixins.json`: CarvedStairMixin, HammerMiningSpeedMixin, ShapelessRecipeMixin, DiscoverTipMixin).

Zielort: `modules/simplehammers/shared/java/com/simplehammers/mixin/HammerAnvilMixin.java` (neu, in `simplehammers.mixins.json`), Logik auf `com.simplehammers.engine`/`item.SledgehammerItem` stuetzen; im Kern bleiben nur die Nicht-Hammer-Teile (Strukturkompass u. a.). Auf 26.2/1.21.11 (`HAMMERS_IN_SB` true) bleibt der Kern-Teil aktiv, Modul-Mixin dort per Mixin-Plugin aus (Plugin-Schalter wie `AliasMixinPlugin`).
Schritte: (1) Hammer-Zweige aus dem Kern-Mixin in neue Klasse verschieben, `HAMMERS_IN_SB` im Kern-Mixin als Bedingung belassen (Kern-Teil nur live auf 26.2); (2) Modul-Mixin registrieren, Plugin-Bedingung "SimpleBuilding nicht vorhanden oder nicht HAMMERS_IN_SB"; (3) beide Mixins gleichzeitig live = Doppelverarbeitung -> Gametest: Amboss Hammer+Hammer/Material mit und ohne SimpleBuilding (module-simplehammers-fabric-263, -standalone-, integration-263); (4) Wiki/Guide-Text pruefen.
Risiken: **Anvil-Mixin ist gerade in Arbeit eines anderen Agenten** (hier bewusst nicht angefasst) -> erst danach, sonst Merge-Konflikt; zwei `createResult`-HEAD-Injects mit `cancellable` (Reihenfolge/Prioritaet: `@Inject(priority=...)`); Mixin-Zielwechsel bei MC-Updates in zwei Mixins.
Aufwand: ca. 3-4 h plus Tests, Standalone-Test von Simple Hammers noetig (heute dort keine Amboss-Regeln = Prinzip-Verletzung).
