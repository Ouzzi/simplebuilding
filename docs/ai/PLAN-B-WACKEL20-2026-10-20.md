# Plan b-wackel20 (20.10.2026)
Ist: Gate 4f7b391c8 rot, 2. Lauf grün. Logs (sb-test testing/runs/*eb66*): claims_rights_action_matrix "Only right B ... probes 0, expected 1" auf Tick 0 (Platzieren im Nachbar-Chunk misslang); end_leaves_catch_fall "duskwood leaves catch the fall, health 5.0" (voller Fallschaden, kein Reset).
Ursachen: (1) Zellen im Nachbar-Chunk außerhalb der Struktur werden von anderen Tests/Mock-Spielern geteilt -> Platzieren durch Entität blockiert; Fix: freie Zelle suchen. (2) Laub ohne Kollision (Spinnweb-Verhalten) wurde nur im Landetick berührt, Reset von fallDistance lag vor/nach Schadenscheck -> Fix: 3 Laub hoch.
Verifikation: je Test >=3 Läufe, Modulfilter einmal, ./gradlew check.
