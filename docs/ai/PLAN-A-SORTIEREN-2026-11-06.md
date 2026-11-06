# Plan A: Inventar-/Container-Sortierung (WE25-3, 06.11.)
- Ist: nichts vorhanden (grep sort/QuickStack ohne Treffer). Zuordnung: Simple Interfaces (Sub-Mod von Simple QoL): besitzt die Container-Screens, die Linked-Payloads und die Keybind-Seite; ist allein spielbar.
- Client: Keybind `key.simpleinterfaces.sort` (Standard mittlere Maustaste), Mixin `SortClickMixin` auf `AbstractContainerScreen.mouseClicked`; Config `sortKey` (Standard an). Im Kreativmodus aus (dort klont die mittlere Taste).
- Netz: `SortPayload(containerId, slotIndex)` clientseitig serverbound; Server (`ContainerSort`) prüft Container-ID, `stillValid`, Menü-Whitelist (Inventar, Truhe/Fass/Ender, Shulker), Slot-Klassen, `mayPickup/mayPlace`.
- Bereich: Spielerinventar nur Slots 9..35 (ohne Hotbar/Rüstung/Crafting); Container = ganzer Behälter des Slots unter dem Zeiger. Maschinen/Öfen/Mod-Slots abgelehnt.
- Reihenfolge: Item-Registry-ID (Vanilla registriert in Kreativ-Tab-Reihenfolge; serverseitiger Tab-Aufbau wäre fragil), gleiche Stacks zusammengeführt.
- Tests: `SortTests` (merge, inventory, chest, refused, payload) + Fabric-Katalog + Neo-Spiegel. Nur Fabric 26.3; Payload-Registrierung für Neo/Forge nicht umgesetzt.
- Rückfrage Besitzer: Sortierung nach echter Kreativ-Tab-Reihenfolge gewünscht? Echte-Maus-Test (menu_probe.py) nicht durchgeführt (keine Client-Läufe während Gate).
