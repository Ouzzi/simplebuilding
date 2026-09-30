---
name: kein-find-ueber-das-ganze-laufwerk
description: "Nie `find /` in Git Bash auf diesem Windows-Rechner - die Prozesse enden nie und bremsen die Maschine tagelang"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: c63559d3-2c6a-43ab-9c5e-92fd18904742
  modified: 2026-09-06T22:11:02.949Z
---

**Nie `find / …` (oder sonst eine Suche ab dem Wurzelverzeichnis) über die Bash-Tools laufen
lassen.** Auf diesem Rechner endet das nie.

**Warum:** Am 2026-09-07 hing der PC des Nutzers. Ursache waren 16 `find.exe`-Prozesse, die
Subagenten am 2026-09-03 gestartet hatten — alle in der Form `find / -name X -path *minecraft*`.
Git Bashs `/` umfasst das komplette Laufwerk samt virtueller Pfade; die Suche kommt nie zum Ende.
Sie liefen **vier Tage** weiter, überlebten das Ende ihrer Shell, verbrauchten je rund 22 % eines
Kerns (zusammen über drei ausgelastete Kerne von 16) und hatten zusammen etwa 60 CPU-Stunden
angesammelt. Ein einzelner Prozess stand bei 34.005 Sekunden CPU-Zeit.

Erschwerend: sie sind zäh. `Stop-Process -Force` wirkte erst verzögert, `taskkill /F` meldete
„Von dieser Aufgabe wird momentan keine Instanz ausgeführt", während der Prozess weiterlief. Zum
Aufräumen taugt: `Get-CimInstance Win32_Process -Filter "Name='find.exe'"` ansehen, `Stop-Process
-Force` schicken, ein paar Sekunden warten, nachzählen.

**Wie es stattdessen geht:**
- Dateien suchen: das **Glob**-Werkzeug, nicht `find`.
- Inhalte suchen: das **Grep**-Werkzeug.
- Muss es doch `find` sein: immer mit konkretem Startpfad und `-maxdepth`, nie ab `/` oder `~`.
- Jars und dekompilierte Quellen liegen an bekannten Stellen — siehe [[mc-26-api-nachschlagen]].
  Dort nachschlagen statt das Laufwerk abzusuchen.

Gilt genauso für Subagenten: die Anweisung gehört in den Prompt, sonst greifen sie zu `find /`.
Hängende `grep.exe` (Pipes, die nie geschlossen werden) sammeln sich ebenfalls an, sind aber
harmlos — sie verbrauchen keine CPU.
