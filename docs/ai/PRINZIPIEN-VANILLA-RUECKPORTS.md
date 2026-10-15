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
| Jukebox mit Kiste darüber (Platten automatisch einlegen/tauschen) | 26.4-Snapshot (Okt. 2026) | 26.3 (+ ältere im Port-Run) | offen | Queue Nachtrag 62 |
