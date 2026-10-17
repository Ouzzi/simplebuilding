# Modpack des Besitzers (Stand 17.10.2026, Fabric, Versionen für 1.21.11)

Quelle: Liste des Besitzers vom 17.10.2026. Zweck: (1) Vergleichsbasis für **Simple Optimization**
(`docs/ai/KONZEPT-SIMPLE-OPTIMIZATION-2026-10-17.md`), (2) **Kompatibilitäts-Testliste** für unsere Mods.
Versionsnummern nur bei den Optimierungs-Mods (vom Besitzer genannt); Details/Lizenzen dort im Konzept §2.

## 1. Optimierung / Leistung
BadOptimizations 2.4.1, Bobby 5.2.11, C2ME 0.3.7, Chunky 1.4.55, Cull Leaves 4.1.1.1, Entity Culling 1.9.5,
Fast IP Ping 1.0.8, FerriteCore 8.2.0, ImmediatelyFast 1.14.1, Lithium 0.21.2, ModernFix-mVUS 5.25.2,
More Culling 1.6.1, NoisiumForked 2.8.3, No Telemetry 1.10.0, ScalableLux 0.1.6, ServerCore 1.5.15,
Sodium 0.8.2, Sodium Extra 0.8.3, Reese's Sodium Options 2.0.2, Very Many Players 0.2.0, voxy 0.2.9 (LOD).

## 2. Rendering / Shader / Optik
Iris 1.10.5, Bliss Shaders, Complementary Shaders (Reimagined, Unbound), Photon Shaders, Euphoria Patches,
Dramatic Skys, Skyboxify, EMF, ETF, Fresh Animations (+Classic Horses, Creepers, Details, Emissive, Extensions,
Objects, Quivers, Spiders), Fusion (3D Items, Block Transitions, Connected Blocks, Connected Glass, Connected Textures,
Emissive Ores, Stacking Items), 3D crops Revamped, 3D Skin Layers, Motschen's Better Leaves (Optik, keine Optimierung),
LambDynamicLights, Not Enough Animations, Visual: Armor Trims, Custom Elytra DataPack/ResourcePack, CustomSkinLoader,
Low On Fire, Make Bubbles Pop, Boat Item View, Recolourful Containers GUI+HUD (+DARK).

## 3. UI / QoL / Werkzeuge
AppleSkin, Axiom, Better Advancements, Better Item Frames, Better Statistics Screen, Better Tab, Camera Utils, CICADA,
Client Sort, Client Tweaks, Command Keys, Controlify, Default Options, Do a Barrel Roll, Dynamic Crosshair (+Compat),
Dynamic Tooltips, Easy Shulker Boxes, Echo Compass, Ender Compass, Enchantment Insights, Essential Mod, Freecam,
Gamma Utils, Immersive Hotbar, InvRestore, Inv View, ItemBound: ReBound, ItemLocks, Jade, JEI, JEBr, JEP, JourneyMap
(+Web Map), Ledger, Litematica, MineTracer, Mouse Tweaks, Nice Name Tags, No Resource Pack Warnings, SafeSlot, Scribble,
Searchables, Shulker Box Tooltip, Smooth Gui, Sound Physics Remastered, Speed FOV Limiter, uku's Armor HUD,
ViaFabricPlus, Visible Traders, MC Paint, Just Corpses.

## 4. Inhalt
Bundles Beyond, Deadly Deadly Dungeon, Ea Nasir's Items and Bosses Pack, Enderscape, Even Better Enchants,
Farmer's Delight Refabricated, Fishing Expansion, Fright's Delight, Illager Invasion, Katters Structures,
Leather From the Dead, Lithostitched (Worldgen-Bibliothek/-Werkzeug), More Delight, My Totem Doll, Nasir's Mobs Plus,
Nice Mobs, qraftyfied STRUCTURES, Rustic Delight, Scaled Dragon Fight, Spanish Delight Refabricated, True Ending,
Ube's Delight; unsere: Simplebuilding, simplefun, Simplemoney, simplequalityoflife, Simpleriding, simpletweaks, simplevisuals.

## 5. Bibliotheken / Konfiguration (ausgeschlossen: Konfig/Bibliothek)
Architectury API, Balm, Cloth Config API, Collective, Config Manager, Controlling, Fabric API, Fabric Language Kotlin,
Forge Config API Port, MaLiLib, MidnightLib, Mod Menu, Puzzles Lib, TCDCommons API, Text Placeholder API, ukulib, YACL.

## 6. Überschneidungen mit unseren Mods = Kompatibilitäts-Testliste
Jeder Punkt: beide Mods zusammen starten, Funktion beider prüfen, keine doppelte Anzeige/Doppelwirkung. Ziel: unser
Teil schaltet sich bei Gleichem ab oder ergänzt sauber (wie in Simple Optimization, Konzept §3).

| # | Fremd-Mod | unser Teil | Risiko | Prüfen |
|---|---|---|---|---|
| K1 | Easy Shulker Boxes, Shulker Box Tooltip | Simple Interfaces (Easy Shulkers/Ender Chests) | Doppel-Öffnen per Rechtsklick, Doppel-Tooltip | Rechtsklick Shulker in Hand/Inventar, Tooltip |
| K2 | Recolourful Containers GUI+HUD (+DARK) | Simple Interfaces (Container-Restyle in Code) | Ressourcenpaket färbt Texturen, unser Code zeichnet drüber | Truhe/Ofen/Fass öffnen, Lesbarkeit (Kontrast ≥4,5:1) |
| K3 | Ledger, MineTracer | Simple Tweaks T10 Block-/Truhen-Protokoll | doppelte Protokolle, doppelte DB-Last | Abbauen/Truhe; Leistungs-Zuschlag messen |
| K4 | InvRestore, Inv View | Simple Tweaks T11 Inventar-Sicherung, T13 /invsee+/endersee | Befehlsname-Kollision `/invsee` | Befehle beider, Brigadier-Konflikt |
| K5 | Chunky | Simple Tweaks T33 /pregen | parallel laufende Vorgenerierung | nur einer gleichzeitig; Befehle getrennt |
| K6 | Just Corpses | Simple Respawn (Downed), Simple Tools Todes-Tresor | wer bekommt die Items beim Tod | Tod mit vollem Inventar, beide Reihenfolgen |
| K7 | Scaled Dragon Fight, True Ending | Simple End Dragon Fight | doppelte Drachen-Skalierung/Lebensbalken | Drachenkampf 1 und 2 Spieler |
| K8 | Better Tab | Simple Tweaks T7 Spielzeit, T24 AFK (falls in der Tab-Liste angezeigt) | Tab-Liste doppelt überschrieben | Tab drücken, AFK-Markierung |
| K9 | Jade, JEI, JEBr, JEP | eigene Jade-Provider, JEI-Plugins (SB, Module) | Fehler im Plugin bricht JEI | JEI-Suche, Jade-Hover auf Mod-Blöcken |
| K10 | JourneyMap (+Web Map) | Simple Maps (Wegpunkte in Locator-Bar) | doppelte Wegpunkte, Minimap-Overlays | Wegpunkt setzen, Karte zoomen |
| K11 | uku's Armor HUD, AppleSkin, Dynamic Tooltips, Enchantment Insights | Simple Visuals (HUD, Tooltips) | doppelte HUD-Elemente/Tooltip-Zeilen | HUD ansehen, Tooltip Rüstung/Essen |
| K12 | Better Statistics Screen | Simple Stats (Item-Statistik-Screen) | nur Nebeneinander, keine Kollision erwartet | beide Screens öffnen |
| K13 | Mouse Tweaks, Client Sort, ItemLocks, SafeSlot | Simple Interfaces (verknüpfte Container, Filter-Slots) | Sortieren/Ziehen in unseren Menüs | Ziehen, Sortieren in SB-Lagern |
| K14 | Freecam | Simple Tweaks Anti-Xray-/Freecam-Regel (Besitzer 17.10.) | gewollt: Freecam auf Servern begrenzen | Regel greift, Einzelspieler unberührt |
| K15 | Iris + Shader (Bliss, Complementary, Photon, Euphoria) | eigene Renderer (Hängematte BE, Schach 3D, End-Nebel, Simple Visuals Partikel) | unsichtbare/flackernde BE, Nebel falsch | Shader an, Testwelt durchgehen |
| K16 | Sodium, More Culling, Cull Leaves, Entity Culling | eigene Block-/Item-Modelle, Octets, BE-Renderer, End-Blätter | weggecullte Flächen, fehlende BE | Testzentrale ansehen |
| K17 | ImmediatelyFast | eigene HUD-/Text-Zeichnung (Simple Visuals, Simple Interfaces) | Batching ändert Zeichenreihenfolge | HUD/Container-Texte |
| K18 | Lithium, C2ME, ServerCore, ScalableLux | Mixins in SB/Modulen (Trichter, Öfen, Kolben, Licht der Lichtbögen) | Mixin-Konflikte, Trichter-Logik doppelt | Server-Gametests mit diesen Mods |
| K19 | Fusion Connected Textures/Glass | Simple Blocks, Octets | verbundene Texturen an Achtelblöcken | Glas-/Stein-Octets nebeneinander |
| K20 | Farmer's Delight (+Delight-Addons) | Simple Sandwiches, Simple Crucibles (Kochen, Messer) | Messer-Tags, Schneidebrett-Doppelung | Rezepte, Tags `c:tools/knife` |
| K21 | Enderscape | Simple End (Biome äußere Inseln) | Biom-Platz auf äußeren End-Inseln | End-Biome-Verteilung |
| K22 | Do a Barrel Roll, Camera Utils | Simple Riding/Movement (Kamera, Elytra) | Kamera-Mixins | Elytra-Flug, Krabbeln |
| K23 | ViaFabricPlus | Netzwerk-Pakete aller Mods | ältere Server-Protokolle | nur Hinweis: unsere Pakete nur auf 26.3 |

Ablauf der Prüfung: Queue-Punkt „Kompatibilitätstest Modpack des Besitzers“ (`.claude/QUEUE.md`, Block Besitzer 17.10.).
Auf sb-test nur für Mods mit 26.3-Version möglich; der Rest im Port-Run 1.21.11 oder beim Besitzer.
