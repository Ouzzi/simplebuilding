# Alte Rückfragen: belegter Stand am 2026-10-05

## 1. „Liste G (58 Punkte)“

„G“ bezeichnet Run G der Welle 22: eine Liste „was fehlt noch“, keine Itemliste oder Testliste.
Beleg: `git show 55ef52229:.claude/QUEUE.md`, Abschnitt Welle 22, markiert die Lieferung am 2026-09-28 mit „58 Punkte, wartet auf Ja/Nein“.
Die eigentliche nummerierte 58er-Liste fehlt in den gefundenen Git-, docs/ai-, HANDOFF- und Memory-Quellen; eine vollständige Kurzliste lässt sich daraus nicht seriös wiedergeben.
Belegte spätere Themen: Server-Configs, Modpack-Kompatibilität, Erfolge, Handbücher, Loot/Handel, Werkzeug-Zusammenspiel, Sounds/Partikel/HUD und Optik (`git show 19a336344:.claude/QUEUE.md`, Welle 23 M–T).
Diese Folgepakete sind keine nachgewiesene Eins-zu-eins-Rekonstruktion der 58 Punkte. Viele sind bereits umgesetzt; der alte Queue-Verweis allein ist kein neuer Bauauftrag.

## 2. „Excavator/Diamond Ingots im Vorlagen-Tooltip“

Das waren tatsächlich falsche englische Spielertexte: vor `b7b76bd22` nannte `item.simplebuilding.basic_upgrade_template.applies_to` einen „Excavator“ und `.ingredients` „Diamond Ingots“.
Beleg: `git show b7b76bd22^:src/main/resources/assets/simplebuilding/lang/en_us.json`; die damalige Fassung enthält beide wörtlich.
Commit `b7b76bd22` korrigierte die Texte bereits. Die aktuellen EN/DE-Dateien in Basis und 26.3-Overlay enthalten diese falschen Vorlagenbegriffe nicht mehr.
`common/src/shared/java/com/simplebuilding/gametest/DataIntegrityTests.java`, `basicUpgradeTemplateTextNamesOnlyRealToolsAndMaterials`, vergleicht die Begriffe mit echten Rezeptzutaten; der Kommentar nennt die alten Fehler nur zur Erklärung.
Ein weiterer Spielertext-Fix ist deshalb nicht nötig. Weder ein registrierter Excavator noch Diamond Ingots werden aus diesen Alttexten abgeleitet.

## 3. „Cover-Bücher im Loot (Code vs HANDOFF)“

Gemeint ist die Verzauberung Abdeckung (Cover), kein Buchumschlag: die alte Notiz `wiki/HANDOFF.md`, Punkt 5, behauptete „cover war nie erhältlich“.
Der aktuelle Code fügt Cover I im Plünderer-Außenposten (Gewicht 8) und Waldanwesen (Gewicht 5) hinzu: `ModLootTableModifications.java`, Blöcke `PILLAGER_OUTPOST` und `WOODLAND_MANSION`.
Der Widerspruch wurde schon in `b7b76bd22` geklärt: `wiki/HANDOFF.md` markiert die alte Aussage ausdrücklich als überholt; `docs/LOOT-BALANCE.md` und `docs/ai/memory/besitzer-entscheidungen-2026-09-28.md` bestätigen „bleibt im Loot“.
**A:** Bestehenden, bereits entschiedenen Zustand beibehalten: beide Lootquellen bleiben. **B:** Beide Cover-Pools entfernen und andere Beschaffung behalten/prüfen. **C:** Nur eine Quelle behalten oder die Gewichte ändern.
B und C wären neue Balanceentscheidungen mit angepasstem Datagen, Wiki und Tests; dieser Rechercheauftrag ändert Cover nicht. A entspricht der vorhandenen Besitzerentscheidung.

## Quellenlücke: zwölf zusätzliche Config-Ideen aus Run D

`5b23f014d` führt die offene Zeile erstmals ein, aber zählt die zwölf Ideen nicht auf.
Der Implementierungscommit `5a432d6d2` und `docs/CONFIG.md` dokumentieren bereits eingebaute Run-D-Optionen; die spätere Welle 23 M beschreibt weitere Serveroptionen.
Weder diese Listen noch die 58er-Liste dürfen ohne Originalbeleg als die gesuchten zwölf Zusatzideen ausgegeben werden. Der vollständige Abgleich bleibt offen.
Geprüft wurden Git-Pickaxe (`Liste G`, `Config-Ideen`, `Kern`), docs/ai einschließlich gespiegelter Memory, beide HANDOFF-Dateien, Bauwerkzeuge-Dokument, der ausdrücklich genannte externe Memory-Ordner sowie vorhandene zugehörige Sitzungs-/Scratchpad-Verweise. Externe Quellen wurden nur gelesen.
