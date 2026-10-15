# Bericht claude-watcher3 (Hollow Watcher Korrektur, Nachtrag 64)

- Branch `claude-watcher3` (Basis claude-wave1 + watcher2), nur nach /root/sb.git gepusht; Merge/Prod offen.
- Direkter Blick in die Augen (`watcherGazeDirect` 4 Grad): kein Freeze, aggressiv gegen den Blickenden (5 s, Tempo x1,35, Mehrspieler: wer direkt schaut). Nähe-Blick: Zonen 10/25/35 wie zuvor, innerhalb 10 Grad ruhig.
- Reichweite `watcherRange` 48: außerhalb kein Ziel/Verfolgen/Teleport; innerhalb Teleport hinter den Spieler alle `watcherTeleportSeconds` (6 s).
- Code: HollowWatcherEntity, FearLogic, MobsConfig (shared, alle Loader); Konzept-Abschnitt „Korrektur 15.10.“ ergänzt.
- Tests: umgebaut fear_watcher_gaze/_zones/_multi_freeze; neu fear_watcher_range, _range_config (Fabric-Wrapper in ModuleGameTest).
- Abweichung/Annahmen: Fernglas-Freeze bleibt; Aggro-Dauer/Tempo geraten.
