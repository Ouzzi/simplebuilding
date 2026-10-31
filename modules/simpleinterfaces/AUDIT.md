# Audit simpleinterfaces (Sub-Mod, 0.1.0)

Branch b-rel32-qola, Basis b-rel32 d9cda1e21. Auditor: Release-Audit-Agent QoL-A, 31.10.2026 (Umfang: MC 26.3 Fabric).
Urteil: **shipbar** (keine P1; 1 P2 behoben; Rest P3). `client/keys`-Teile nur gelesen (laufen auf b-fix32), Befunde dort mit Vermerk "nach b-fix32".

## Durchgesehene Dateien (Zeilen)

`clienttest/java/com/simplebuilding/modules/simpleinterfaces/`
ModuleClientSmoke.java (278), 
`fabric/`
build.gradle (10), 
`fabric/src/main/java/com/simplebuilding/modules/simpleinterfaces/`
ContainersFabricClient.java (32), ContainersModMenu.java (9), InterfacesFabric.java (56), ModuleGameTest.java (49), 
`fabric/src/main/resources/`
fabric.mod.json (47), 
`forge/`
build.gradle (11), 
`forge/src/main/java/com/simplebuilding/modules/simpleinterfaces/`
ContainersForge.java (76), ContainersForgeClient.java (29), 
`forge/src/main/java/com/simplebuilding/modules/simpleinterfaces/forge/`
ModuleForgeTests.java (19), 
`forge/src/main/java/com/simplebuilding/modules/simpleinterfaces/forge/mixin/`
RegistryLoadTaskMixin.java (54), 
`forge/src/main/resources/META-INF/`
mods.toml (44), 
`forge/src/main/resources/`
simpleinterfaces.forge.mixins.json (12), 
`neoforge/`
build.gradle (21), 
`neoforge/src/main/java/com/simplebuilding/modules/simpleinterfaces/`
ContainersNeoClient.java (30), ContainersNeoForge.java (62), ModuleNeoTests.java (60), 
`neoforge/src/main/resources/META-INF/`
neoforge.mods.toml (43), 
`shared/java/com/simplebuilding/modules/simpleinterfaces/`
ContainerTests.java (508), ContainersConfig.java (59), ContainersScreen.java (97), KeyPageTests.java (128), LinkedConfig.java (29), LinkedTests.java (565), SimpleInterfaces.java (153), TooltipTests.java (283), ZoomMath.java (31), 
`shared/java/com/simplebuilding/modules/simpleinterfaces/client/`
BundleViews.java (26), ContainerPreviewTooltip.java (90), ContainersClient.java (53), KeysClient.java (16), KeysScreen.java (493), LegacyMigration.java (39), LeggingsFix.java (27), LinkedHud.java (96), LinkedPanel.java (192), LinkedScreen.java (6), OfferChoice.java (6), ScopeOverlay.java (47), StationScreens.java (359), StorageDecors.java (92), StyleToggleButton.java (27), StyleToggleClient.java (21), StyledScreens.java (259), WorkScreens.java (362), ZoomState.java (79), 
`shared/java/com/simplebuilding/modules/simpleinterfaces/linked/`
LinkedContainers.java (503), LinkedMenu.java (27), LinkedSlot.java (56), PortableContainers.java (213), PortableMenu.java (129), 
`shared/java/com/simplebuilding/modules/simpleinterfaces/mixin/`
AnvilStationMixin.java (61), BeaconBackgroundMixin.java (55), BeaconButtonMixin.java (58), BeaconIconMixin.java (30), BrewingBackgroundMixin.java (57), BundleSelectionMixin.java (21), BundleTooltipViewMixin.java (62), CartographyHeightMixin.java (26), CartographyStationMixin.java (40), ClientTooltipPreviewMixin.java (20), ContainerLabelsMixin.java (34), ContainerMenuLinkedMixin.java (78), ContainerScreenLinkedMixin.java (74), CrafterBackgroundMixin.java (68), CraftingBackgroundMixin.java (51), CyclingIconStationMixin.java (38), EnchantmentBackgroundMixin.java (64), EnderChestSnapshotMixin.java (19), FurnaceBackgroundMixin.java (64), GrindstoneStationMixin.java (31), HudLinkedMixin.java (19), ItemStackPreviewMixin.java (38), ItemUsePortableMixin.java (21), KeyBindsScreenMixin.java (29), KeyboardLinkedMixin.java (19), LeggingsTextureMixin.java (25), LoomStationMixin.java (45), MerchantStationMixin.java (90), MountBackgroundMixin.java (64), ServerPlayerLinkedMixin.java (26), ShapecutterStationMixin.java (45), SlotHighlightMixin.java (19), SlotIconMixin.java (43), SlotPositionAccessor.java (18), SmithingStationMixin.java (31), StationBackgroundMixin.java (42), StonecutterStationMixin.java (38), StorageBackgroundMixin.java (52), StyleToggleMixin.java (36), TradeOfferButtonMixin.java (31), ZoomFovMixin.java (17), ZoomHudMixin.java (45), ZoomInputMixin.java (31), 
`shared/java/com/simplebuilding/modules/simpleinterfaces/network/`
ConfigSyncPayload.java (18), EnderChestSnapshotPayload.java (22), LinkedCancelPayload.java (17), LinkedOpenPayload.java (34), 
`shared/java/com/simplebuilding/modules/simpleinterfaces/style/`
BoxLayout.java (126), BoxMotifs.java (51), ContainerStyles.java (58), ScreenStyle.java (16), StationStyles.java (84), StorageStyles.java (130), StyleContext.java (11), WorkStyles.java (39), 
`shared/java/com/simplebuilding/modules/simpleinterfaces/tooltip/`
BundleView.java (50), ContainerPreview.java (19), ContainerTooltips.java (136), 
`shared/resources/assets/simpleinterfaces/`
icon.layers.json (118), 
`shared/resources/assets/simpleinterfaces/lang/`
de_de.json (126), en_us.json (126), 
`shared/resources/`
simpleinterfaces.mixins.json (55), 
`tools/`
check_data.py (50)

Zusätzlich gelesen: `framework/.../SubMods.java`, `ConfigFiles.java`; simplelib `InteractionGuard`, `UiStyleToggle`, `UiBrightness`, `UiShiftTooltip`, `DevVariants` (Ausschnitt); `modules.json`-Eintrag.
Nur überflogen (Struktur, Zustandsänderungen, keine Zeile-für-Zeile-Prüfung): Testklassen `ContainerTests`, `TooltipTests`, `KeyPageTests`, hinterer Teil von `LinkedTests`, `ModuleClientSmoke`. Forge/NeoForge-Einstiege gelesen, aber nicht im Release-Umfang. Nicht gelesen: `wiki/manual.json` (nur per `check_data.py`), `icon.layers.json`, `structure/empty.nbt`.

## Prüfliste
- **C1 Code:** Server: `LinkedContainers`/`LinkedSlot`/`Session` (Marke am Spieler, jeden Tick geprüft: Reichweite, Dimension, Chunk, Block-Entity, alle 20 Ticks Claim/Schloss; jeder Slot-Zugriff prüft erneut; Creative ausgeschlossen), `PortableContainers`/`PortableMenu` (gesperrter Slot, Tausch/Wurf/Doppelklick gesperrt, Menü schließt wenn der Stack den Slot verlässt, Inhalt wird nur zurückgeschrieben solange derselbe Stack im Slot liegt, Loot-Tabellen-Boxen ausgeschlossen), Config (`SimpleInterfaces`, Übernahme aus alter Simple-QoL-Datei, 64-KiB-Grenzen, Werte geklemmt). Client: Panel/HUD/Tooltip/Zoom/Stil. Statische Maps sind `WeakHashMap` auf Server-Thread, Client-Zustand `volatile`. Befund INT-P2-1 behoben. Ergebnis: ok.
- **C2 Mixins (44):** 4 gemeinsame (`ContainerMenuLinked`, `ServerPlayerLinked`, `ItemUsePortable`, `EnderChestSnapshot`: Server, `required`), 40 reine Client-Mixins. Fast alle `@WrapWithCondition`/`@WrapOperation`/`@Inject`, kein `@Overwrite`; `@Pseudo` für Simple Blocks' Shapecutter. Die zwei `@Redirect` (`ContainerScreenLinkedMixin`, `KeyboardLinkedMixin`) liefen bei Konflikt mit einer anderen Mod in einen Startabsturz: umgestellt (INT-P3-1). Konfliktrisiko: `ItemStackPreviewMixin`/`getTooltipImage` (HEAD, nur Shulker/Endertruhe), Tooltip-Mods (Item Tooltip Fixes, Shulker-Preview-Mods) können doppelt zeichnen; Inventar-Mods (Mouse Tweaks, Inventory Profiles Next) arbeiten auf `clicked`/`mouseScrolled` – bewusst per Duck-Typing, kein Cancel außer bei Linked-Slots. Ziele gegen 26.3: `tools/check_client_mixins.py` (siehe Testlauf). Ergebnis: ok.
- **C3 Config:** Client `config/simpleinterfaces.json` (Hauptschalter, 22 Stil-Schalter, Tooltips, Bundles, Zoom), Server `simpleinterfaces-server.json` (5 Werte), Befehl `/simpleinterfaces config`; EN/DE je 124 Schlüssel gleich, "Default:"/"Standard:" in allen Tooltips (INT-P3-3 ergänzt Helligkeit). Zoom-Taste Standard C, Funktion aus (`zoomHotkey=false`). Ergebnis: ok.
- **C4 Daten:** keine Items/Blöcke/Rezepte; Lang vollständig (`check_lang_keys.py`, `check_data.py` grün), Icons 16/128. Ergebnis: ok.
- **C5 Unabhängigkeit:** `requires` nur cloth_config + simplelib (verschachtelt), `suggests` simplebuilding/modmenu; Fremdzugriffe nur per Namen/Reflexion (`simplestorage:container_counts`, Shapecutter-Pseudo-Mixin, `ContainerStyleHints`, Titel-Schlüssel `block.simplebuilding.*`). Standalone-Target vorhanden. Ergebnis: ok.
- **C6 A12:** Stark: Linked GUIs und Easy Shulkers/Endertruhe greifen in `AbstractContainerMenu.clicked` und `Item.use` ein (je Server-Schalter, Standard an, Super-Mod-Schalter schaltet alles ab). Mittel: Spyglass-Rahmen/HUD-Ausblenden (Standard an, abschaltbar), Tooltip-Raster (Shift). Schwach: Stil der Container-Screens (Hauptschalter), Beinfix. Ergebnis: ok, alles abschaltbar.
- **C7 Netzwerk:** 4 Payloads: `config_sync` (S→C, 32 KiB-Grenze, ungültig wird ignoriert), `linked_open` (S→C), `ender_chest_snapshot` (S→C, nur eigene Truhe), `linked_cancel` (C→S ohne Daten; Server entscheidet). Gesendet wird nur, wenn der Client den Kanal hat; Vanilla-Client sieht Vanilla-Menüs (Easy Shulkers funktionieren trotzdem, serverseitig). Der Client vertraut dem Server; Server vertraut dem Client nirgends. Ergebnis: ok.
- **C8 Lizenz/Assets:** CC0-1.0; Leggings-Fix leitet Pixel beim Laden ab (nichts ausgeliefert); keine Pfade/Schlüssel; Dev-Texte "Fernrohr-Rahmen" nur im Dev-Panel. Ergebnis: ok.
- **C9 Offen:** keine TODO/FIXME. Queue: WP-25 Tastenbelegung-UI (b-fix32). Ergebnis: ok.

## Befunde
| ID | Schwere | Befund | Stand |
|---|---|---|---|
| INT-P2-1 | P2 | Easy Ender Chest (`PortableMenu`) ruft nie `stopOpen` der Endertruhe: Tooltip-Abbild der Endertruhe blieb nach Änderungen darüber veraltet (Doku/Mixin versprechen Aktualisierung beim Schließen) | behoben, Test `portable_ender_chest` erweitert |
| INT-P3-1 | P3 | Zwei `@Redirect` (Panel-Klick außerhalb, Escape/HUD) kollidieren mit Mods, die dieselben Aufrufe umleiten (Startabsturz bei `defaultRequire 1`) | behoben (`@WrapOperation`) |
| INT-P3-2 | P3 | Lokale Stil-Config wurde nicht atomar geschrieben, defekte Datei stillschweigend durch Standard ersetzt und beim nächsten Speichern überschrieben | behoben (`ConfigFiles`) |
| INT-P3-3 | P3 | Tooltip "Inventar-Helligkeit" ohne "Default:"-Satz | behoben |
| INT-P3-4 | P3 | Server-Reiter im Konfigurationsbildschirm erscheint auch auf einem fremden Server und ändert dort nur lokale Dateien | Restpunkt (Hinweistext wäre ein Feature) |
| INT-P3-5 | P3 | Zoom-Taste C ist belegt, auch wenn die Funktion aus ist (Konflikt-Anzeige mit anderen Zoom-Mods) | Restpunkt, Besitzerwunsch? (Rückfrage R-INT-1) |
| INT-P3-6 | P3 | `KeysScreen.extractRenderState` ruft jedes Bild `refresh()` (alle Tasten + Übersetzungen neu) | nach b-fix32 (Datei dort in Arbeit) |
| INT-P3-7 | P3 | Forge/NeoForge-Clients registrieren Zoom-Taste und Tick nicht (nur Fabric); nur Port-Lauf relevant | Restpunkt (Port, nicht Release 26.3 Fabric) |
| INT-P3-8 | P3 | Tooltip `applyOversized` setzt Stack-Anzahl aus fremdem Komponenten-JSON ungeprüft (Simple Storage, vertraut) | Restpunkt, harmlos |

Keine P1.

## Urteil
**shipbar.** Offene Release-Punkte: Version/Changelog, Icon-Pfad (128 px), Beschreibung (Entwurf in `docs/ai/release-qoa/qol-a.md`), `DevVariants.DEFAULT_ON` (Hauptsitzung; das Dev-Panel steuert hier nur die Fernrohr-Rahmen-Variante A/B, Standard A), Abnahme von Keys-Seite nach b-fix32.
