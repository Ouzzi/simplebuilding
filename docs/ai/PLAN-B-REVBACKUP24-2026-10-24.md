# Plan b-revbackup24 (REV24-N-6, Rest)

Ist-Zustand (Backups.java): (a) ist im Kern schon richtig: begin() setzt `noSave` auf allen Levels und ruft `saveEverything(true,true,true)` (= save-off + save-all flush, schreibt auch playerdata/data), `resume()` nach dem Zip auf dem Server-Thread, `stopping()` bei /stop, Test `backup_stop_resumes_saving`. Luecke: wirft saveEverything oder submit (RejectedExecution nach stop) eine Exception, bleibt noSave an und `running` true.
(b) Luecke: `.part`-Reste werden nie geraeumt; Move ohne ATOMIC_MOVE.

Aenderungen: Backups.java (try/catch -> resume()+running=false+rethrow in begin; cleanParts() in start() und zip(); ATOMIC_MOVE mit Fallback). AdminTests.backup: stale `.zip.part` vorher anlegen, danach weg, kein `.part` uebrig; noSave danach aus (schon vorhanden).
Nicht geaendert: saveEverything blockiert den Server-Thread (Vanilla-save-all tut dasselbe, bewusst).
Verifikation: module-simpletweaks-fabric-263 + standalone, ./gradlew check.
