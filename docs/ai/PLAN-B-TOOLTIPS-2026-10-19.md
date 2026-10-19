# Plan B: Shulker- und Endertruhen-Tooltip (b-tooltips, 19.10.2026)

Umsetzung von `docs/ai/KONZEPT-CONTAINER-TOOLTIPS-2026-10-19.md` mit den Empfehlungen (Zielmod Simple Interfaces,
Raster nur mit Shift, Endertruhen-Snapshot bei Login + Schließen, Server-Teil optional, Rucksack/Truhen erst Stufe 3).

## Ist-Zustand (geprüft)
- Vanilla 26.3 (`minecraft-patched-26.3.0.16-beta-sources`): `ClientTooltipComponent.create` kennt nur `BundleTooltip`
  und `ActivePlayersTooltip`; `ShulkerBoxBlock`/`BlockItem` liefern kein `getTooltipImage`. Shulker zeigen nur die
  Textliste aus `ItemContainerContents.addToTooltip` (5 Zeilen + "und n weitere"), Endertruhe nichts.
  -> Rückport-Regel greift nicht, Mod-Teil wird gebaut.
- Vorhandene Muster: `simplevisuals` hängt eigene Tooltip-Bilder per Interface-Mixin an `ClientTooltipComponent.create`
  (läuft auf allen drei Loadern); SB-`BackpackTooltip` zeichnet ein Shift-Raster. Simple Interfaces hat Payloads
  (`ConfigSyncPayload`, `LinkedOpenPayload`, je Loader registriert, optional), lokale Config `ContainersConfig`,
  Server-Config `LinkedConfig` (an Clients gesynct), Gametest-Kataloge Fabric/Neo/Forge.

## Umsetzung
1. `tooltip/ContainerPreview` (shared, `TooltipComponent`-Record): Slots, Spalten, Tönung, Raster sichtbar?, Hinweis.
   `tooltip/ContainerTooltips` (shared, ohne Client-Klassen): `preview(stack, shift, local, server, ender)` entscheidet
   (Shulker mit Inhalt / Endertruhe mit bekanntem Snapshot; Config; Sub-Mod-Schalter), `hidesText(...)` für die
   Vanilla-Textliste, `snapshot(PlayerEnderChestContainer)`, Client-Cache `enderSnapshot`, `send(ServerPlayer)`.
2. `network/EnderChestSnapshotPayload(ItemContainerContents)` mit Vanilla-`ItemContainerContents.STREAM_CODEC`,
   nur Server->Client, optional. Registrierung Fabric/NeoForge/Forge wie `LinkedOpenPayload`.
3. Mixins: `PlayerEnderChestContainerMixin` (common, `stopOpen` TAIL -> Snapshot an diesen ServerPlayer),
   `ItemStackTooltipMixin` (client: `getTooltipImage` liefert Vorschau, `addToTooltip` für `container` unterdrückt,
   solange das Raster sichtbar ist), `ClientTooltipComponentMixin` (client: `ContainerPreview` -> `ContainerPreviewTooltip`).
4. Client-Zeichnung `client/ContainerPreviewTooltip`: Vanilla-Feld (hellgrau #C6C6C6, bei gefärbter Kiste 50 % Färbung,
   Endertruhe leicht türkis), 9x3 Slots im Vanilla-Slotstil (#8B8B8B, Kante #373737/#FFFFFF), Items + Anzahl wie
   Vanilla. Ohne Shift eine graue Hinweiszeile (#AAAAAA auf Tooltip-Grund #100010, ~8:1).
5. Login: JOIN-Handler aller Loader schickt den Snapshot zusammen mit `config_sync`. Logout leert den Client-Cache.
6. Config: lokal `tooltipShulker`, `tooltipEnderChest`, `tooltipNeedsShift`, `tooltipColorByDye` (alle an);
   Server `enableEnderChestTooltip` (an; aus = kein Snapshot, Client zeigt nichts). Cloth-Screen-Einträge.
7. Lang EN/DE, Wiki `modules/simpleinterfaces/wiki/manual.json`, `docs/MODS-UND-FEATURES.md`, Queue-Punkt.

## Nicht jetzt (Stufe 3)
Rucksäcke, Truhen-Items, Bündel, Interaktion, verschachtelte Vorschau, `maxRows` (Shulker hat immer 3 Reihen).

## Risiken
Mixin auf Interface-Static-Methode (bewährt in simplevisuals); generische `ItemStack.addToTooltip`-Signatur auf
NeoForge/Forge gleich (Vanilla-Methode, nicht gepatcht); Forge-Config-Screen kommt aus `forge-native-config.gradle`.

## Verifikation
Gametests `TooltipTests` (Snapshot aus PlayerEnderChestContainer, Codec-Roundtrip, Shulker-Inhalt aus
ItemContainerContents, Config aus -> nichts, Sub-Mod aus -> nichts, Schließen sendet Snapshot nur an den eigenen
Spieler) in module-simpleinterfaces-fabric-263/-neoforge-263 und standalone; Compile inkl.
`:modules:simpleinterfaces:forge:compileJava`; `tools/check_tooltip_text.py`, `tools/check_ui_contrast.py`;
Abschluss-Gate `./gradlew check -q`. Client-Sicht (Zeichnung, Shift-Wechsel) nicht per Client-Lauf -> offen.
