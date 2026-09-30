---
name: testlaeufe-sparsam
description: Wann Client-Tests laufen, Server-Ziele parallel, Agenten nur Server-Tests - Token- und Zeitregel des Besitzers
metadata:
  type: feedback
---
Waehrend der Umsetzung NUR Teiltests: Kompilierung + gefilterte Server-Tests der geaenderten Features (`run.py --targets fabric-262,fabric-12111 --filter <ids>`); volles Gate genau einmal am Ende einer Welle (Besitzer-Anweisung 2026-09-25). Server-Ziele laufen parallel in einem Gradle-Aufruf (run.py, abschaltbar mit SIMPLEBUILDING_SERIAL_TESTS=1). Client-Ziele bleiben seriell (Maus/Fokus). Client-Tests nur, wenn Anzeige/Steuerung betroffen ist, und einmal vor jedem groesseren Push; Agenten in Worktrees fuehren nur Server-Tests aus, das volle Gate laeuft einmal nach dem Zusammenfuehren. Parallele Agenten nutzen je einen eigenen Scratchpad-Unterordner.

Nach JEDEM Merge eines Agenten-Branches sofort kompilieren (`./gradlew.bat check -q`, in Bash auf Exit-Code pruefen - PowerShell meldet wegen Warnungen auf stderr faelschlich Fehler): ein konfliktfreier Merge zweier Branches baute am 2026-09-25 nicht (Hunger-Hook landete in neuer Blaupausen-Methode).

**Why:** Besitzer will Tokens sparen (Wochenlimit am 2026-09-24 erreicht, Agenten brachen ab); Warten auf 40-min-Client-Laeufe kostete die meisten Agenten-Tokens; zwei Agenten zerschossen sich ueber einen gemeinsamen Scratchpad.
**How to apply:** Agenten-Prompts ohne Client-Laeufe, dafür ein Gate am Ende; nicht mehr als 2 Agenten gleichzeitig, wenn das Limit knapp ist. Siehe [[testabdeckung-2026-09]], [[gametest-harness-fallen]].
