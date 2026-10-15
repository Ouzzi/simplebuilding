# Prinzip: Rückports künftiger Vanilla-Features (Besitzer 2026-10-15)

Die Mod bringt manchmal Features aus neueren Minecraft-Versionen (Snapshots) in ältere Linien, z. B. die
Kisten-Jukebox aus dem 26.4-Snapshot.

1. **Vor dem Einbau je Version prüfen**, ob die Ziel-Version das Feature schon selbst hat (Vanilla-Code, Changelog,
   Snapshot-Notizen). Hat sie es, wird die Mod-Umsetzung für diese Version **ausgeschlossen** (Versions-Flag in
   `McVersion` o. ä., kein doppeltes Verhalten), sonst eingebaut.
2. **Bei jedem Port/Versionswechsel erneut prüfen** (Port-Run 26.2/1.21.11/26.4 und jede neue Version): Liste der
   Rückports durchgehen, je Version „Vanilla hat es“ → Mod-Teil aus, „Vanilla hat es nicht“ → Mod-Teil an. Abweichendes
   Vanilla-Verhalten (andere Details als unser Rückport) übernimmt die Vanilla-Fassung.
3. Jeder Rückport steht in der Liste unten (Feature, Ursprungs-Version, betroffene Linien, Versions-Flag, Test).
   Ein GameTest je Version belegt, dass genau eine Umsetzung aktiv ist.

## Liste der Rückports
| Feature | Vanilla ab | Mod-Linien | Flag/Ort | Stand |
|---|---|---|---|---|
| Jukebox mit Kiste darüber (Platten automatisch einlegen/tauschen) | 26.4-Snapshot 4 (13.10.2026) | 26.3: **an** (Fabric/NeoForge/Forge). 26.2 + 1.21.11: aus bis zum Port-Run (dann an, Vanilla hat es dort nicht). 26.4+: **aus** | `McVersion.JUKEBOX_CHEST_PLAYLIST` (26.3 true, 26.2 false) + Laufzeit-Sperre: existiert der Vanilla-Block-Tag `#minecraft:jukebox_containers` (nicht leer), ist die Mod-Umsetzung aus (`JukeboxPlaylist.active`); Code `util/JukeboxPlaylist.java`, Mixins `JukeboxBlockEntityPlaylistMixin`/`JukeboxBlockPlaylistTickerMixin` (26.2: leere Stubs) | GameTests `jukebox_playlist_game_test_*` (inkl. `mod_part_is_active_only_without_vanillas_own`); Details und Quellen: `docs/ai/BERICHT-OC-JUKEBOX.md` |

Beim Port-Run 26.4 (Release): `McVersion`-Zwilling der 26.4-Linie (`mc26_4/overlay/java/.../McVersion.java`, existiert noch nicht -
die 26.4-Snapshot-Linie erbt die 26.3-Datei) mit `JUKEBOX_CHEST_PLAYLIST = false` anlegen; bis dahin schaltet der Tag-Test es
ab, sobald das 26.4-Snapshot-Jar den Tag mitbringt (Snapshot 1, die gebaute Linie, hat ihn noch nicht).
