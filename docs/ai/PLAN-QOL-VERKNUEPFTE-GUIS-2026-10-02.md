# Plan: Simple QoL – verknüpfte GUIs + Easy Shulkers/Ender Chests (Nachtrag 8)

Modul `modules/simplequalityoflife` (nur 26.3, Fabric/NeoForge/Forge). Branch `claude-qolgui`.

## Ist-Zustand
- Modul hat keine eigenen Menüs/Registry-Inhalte (Test `launch` verlangt: keine ITEM/BLOCK/ENTITY-Einträge).
- Rechtsklick-Hooks je Loader rufen `HoeHarvestHandler`/`FurnaceLavaFillHandler` (UseBlock/RightClickBlock).
- Claims: `InteractionGuard.permission` (Break-Event des Loaders), S2C-Payloads je Loader registriert.
- Module dürfen SimpleBuilding nur über Registry-IDs koppeln → Mod-Stufen-Shulker generisch über
  `ShulkerBoxBlock`-Unterklasse + eigenen Block-Entity-Weg (`applyComponentsFromItemStack`/`collectComponents`).

## Entscheidungen (selbst getroffen)
1. **Keine eigenen MenuTypes.** Easy Shulker/Ender nutzen serverseitig ein eigenes Menü mit Vanilla-Typ
   (`SHULKER_BOX`, `GENERIC_9xN`) → Client zeigt Vanilla-Bildschirm, Server ist autoritativ.
2. **Verknüpfte GUIs = Zusatz-Slots am Ende des zweiten Menüs** (beliebiges Vanilla-/Mod-Menü bleibt
   unverändert; Indizes 0..n bleiben gleich). Server hängt die Slots in `ServerPlayer#initMenu` an und
   schickt `LinkedOpenPayload(containerId,size,title)`; Client hängt dieselbe Anzahl Slots an.
3. **Bildschirm:** zweites GUI bleibt vanilla-zentriert (viele Bildschirme rechnen ihr Bild aus
   width/height, ein Verschieben würde sie zerreißen). Das Truhenfeld sitzt **darüber**; passt es nicht
   ganz, entweder rechts daneben (wenn dort mehr Reihen passen) oder oben mit Scrollen (Mausrad).
4. **Shift-Klick:** vorgemerkte Truhe → zweites GUI (dessen Nicht-Inventar-Slots), sonst Inventar.
   Zweites GUI → wie Vanilla ins Inventar (vorgemerkte Slots währenddessen gesperrt).
   Inventar → wie Vanilla ins zweite GUI; landet dort nichts, in die vorgemerkte Truhe.
5. **Rezeptbuch aus der Truhe füllen: nicht umgesetzt.** Vanillas Rezeptbuch zählt nur das
   Spielerinventar (Client-Anzeige + `ServerPlaceRecipe`); halb erweitert würde die Anzeige lügen.
   Werkbank wird per Verschieben/Shift-Klick direkt aus der Truhe befüllt.
6. **Vormerken:** Schleich-Rechtsklick, Haupthand leer, Block-Entity ist `BaseContainerBlockEntity`
   (Truhe = ganze Doppeltruhe). Gleiche Truhe erneut → Vormerkung weg; andere Truhe → umgemerkt.
   Prüfung: Claims (`InteractionGuard.permission`), Reichweite (`InteractionGuard.allow`), Schloss (`canOpen`).
7. **Ende der Vormerkung** jeden Tick: Entfernung > Reichweite, andere Dimension, Chunk nicht geladen,
   Block-Entity weg/ersetzt, Logout/Tod (Vormerkung hängt am Spielerobjekt, WeakHashMap).
   Offenes kombiniertes Menü wird dann geschlossen. Jeder Slot-Zugriff prüft zusätzlich die Gültigkeit.
8. **Partikel**: alle 10 Ticks ein dezentes Partikel an der Haupthand, nur an den Spieler selbst gesendet.
9. **Easy Shulker/Ender:** Rechtsklick in die Luft (Item#use-Mixin) oder Rechtsklick auf das Item im
   eigenen Inventar (Mixin `AbstractContainerMenu#clicked`, beide Seiten brechen ab; Server öffnet im
   nächsten Spieler-Tick). Kreativinventar ausgenommen (Client-seitige Slots).
   Shulker-Inhalt wird bei **jeder** Änderung sofort ins Item zurückgeschrieben (überlebt Absturz/Logout),
   Stack-Identität wird geprüft; Slot gesperrt (Pickup, Swap/Zifferntaste, Offhand, Werfen, Doppelklick,
   Ziehen). Keine Shulker in Shulker (`canFitInsideContainerItems`). Items mit Loot-Tabelle werden nicht geöffnet.
   Mod-Stufen-Shulker: Größe aus dem Block-Entity (27/36/45/54 → 9xN), Komponenten über das
   Block-Entity (bewahrt eigene Komponenten wie Stapelzähler); Einlagerung bis normale Stapelgröße.
10. **Config (Server, Obergrenzen):** `enableLinkedContainers` (true), `linkedContainerRange` (64, 8–128),
    `enableEasyShulkers` (true), `enableEasyEnderChests` (true). Tooltips EN/DE mit Default zusammenhängend.

## Dateien
- shared: `linked/LinkedContainers.java` (Server-Logik), `linked/LinkedSlot.java`,
  `portable/PortableContainers.java`, `portable/PortableMenu.java`, `network/LinkedOpenPayload.java`,
  `client/LinkedPanel.java`; Mixins `ServerPlayerLinkedMixin`, `ContainerMenuLinkedMixin`,
  `ItemUsePortableMixin`, `client.ContainerScreenLinkedMixin`, `client.SlotPositionAccessor`.
- Loader: Fabric/NeoForge/Forge – Use-Hook um `LinkedContainers.onRightClickBlock`, Payload registrieren.
- Config + `QolConfigScreen.tab` (neuer Reiter `containers`), Lang EN/DE (shared/resources), Wiki `manual.json`.
- Tests in `QolTests` + Fabric-Adapter `ModuleGameTest`.

## Risiken
- Fremde Menüs mit eigenem `quickMoveStack` über `slots.size()` → durch Sperr-Flag abgefangen.
- Client-Vorhersage bei Vanilla-Bildschirm weicht ab (gesperrter Slot) → Server korrigiert per Resync.
- Client-Anzeige nicht im Spiel abgenommen (kein Client-Start erlaubt).

## Verifikation
GameTests: vormerken/entvormerken, Rechte-Veto, Reichweite (Config-Grenze), Verschieben ohne Dupe
(Summen), abgebaute Truhe → keine Entnahme, Shulker-Inhalt bleibt, Slot gesperrt, kein Shulker in Shulker,
Ender-Truhe, Config-Schalter. Gates: module-simplequalityoflife-fabric-263/-neoforge-263, fabric-263,
Compile 26.2 + Forge 26.3, `gradlew check -q`, Wiki `--all --check`.
