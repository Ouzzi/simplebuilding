# Folgearbeit nach dem grünen Push – 2026-10-01

## Ausgangspunkt und Auftrag

Der Besitzer hat nach Durchsicht der offenen Punkte die selbstständig ausführbare
Folgearbeit beauftragt. Ausgang ist `17c5c754`, auf Remote-master bestätigt:
check erfolgreich, 1600/1600 Hauptlinien- und 477/477 Integrationstests, GREEN.
Der bisherige Orchestrator ist beendet. Der Besitzer-Client bleibt unangetastet.

## Umsetzung und Zuständigkeit

1. Bettplatzierung: `cx-next-dimensions` / `codex-next-dimensions` wurde auf den
   Ausgangsstand vorgezogen. Gemeinsamen Platzierungsablauf und alle Aufrufer
   prüfen, Bett-/Mehrblock-Regressionen ergänzen, kleinsten korrekten Fix bauen.
2. Claims: `cx-next-small` / `codex-next-small` wurde ebenfalls vorgezogen.
   Kupfergolem, Crafter, Blitzfolgen und verbleibende Automationspfade anhand
   tatsächlicher Laufzeitquellen prüfen und belegbare Schutzlücken schließen.
   Claims bleibt AUS; keine Freigabe allein aufgrund einzelner grüner Tests.
3. Wiki: `cx-next-merge-review` / `codex-next-merge-review` bearbeitet kleine
   bestehende UX-Todos (Filterzustand, Tabellenköpfe, Direktlinks, leere Zustände).
   Vorhandenes Verhalten wiederverwenden, keine neuen Abhängigkeiten.
4. Orchestrator: veraltete Todos mit Code/Prüfbelegen abgleichen, Diffs prüfen,
   Ergebnisse integrieren und gemeinsamen exakten Commit im Gate prüfen.
5. Ergänzter Besitzerauftrag: Launch-Zentrale startet standardmäßig alle
   ausgewählten Projektmods; einklappbare Auswahl direkt auf der Startseite.
   Bisher übernahmen normale Starts nur Entwickler-Mods. Die gespeicherte
   Projektmod-Auswahl muss auf Fabric, NeoForge und Forge 26.3 tatsächlich in
   den Laufzeitpfad eingehen. Ältere Minecraft-Linien unverändert lassen.

## Grenzen und Risiken

Keine Besitzerwelt ändern, keinen laufenden Client stoppen, keine Builds im
Hauptcheckout. Clientabnahmen bleiben bis zum Schließen des Besitzer-Clients
offen. Echolot-Bedienung, Sprachbericht, neue Portalformen, Balancing-Entscheidungen
und andere Minecraft-Ports werden nicht eigenmächtig entschieden.
Die vorhandenen unversionierten Serena-Dateien bleiben erhalten.
Serverprüfungen auf höchstens zwei Gradle-Worker und begrenzten Heap einstellen;
schwere Workerprüfungen untereinander koordinieren.

## Verifikation und Abschluss

Verhaltenstests für Bett/Mehrblock-Platzierung sowie erlaubte, verbotene und
inaktive Claims-Pfade auf Fabric/NeoForge 26.3. Wiki mit bestehenden Tests und
Browserprüfung kontrollieren. Gemeinsame Quellen müssen weiterhin kompilieren.
Nach Integration Generatoren und vollständiges Gate mit Integration ausführen;
nur die exakte GREEN-SHA pushen. Testzentralen-Abdeckung in isolierten Welten
erhalten. Offene Abnahmen und verbleibende Schutzlücken ehrlich dokumentieren.

## Plan-Abgleich

Alle vier Arbeitspakete sind integriert: Mehrblockplatzierung, Claims-Folgepfade,
Wiki-UX und Launch-Mod-Auswahl. Notwendige Korrekturen bei der Verifikation:
NeoForge nutzt das distributierbare `jar` statt des Ressourcen-Tasks `jarJar`;
Modularchive werden erst nach Konfiguration ihrer Producer aufgelöst. Der
NF-Kurzstart mit `--initSettings` wurde wegen seiner Serverthread-Erwartung durch
einen normalen isolierten Serverlauf mit sauberem Stopp ersetzt. Abwahl und
gemeinsame Claims-/Bett-Regression sind geprüft. Einzelbelege und Grenzen:
`FOLLOWUP-VERIFICATION-2026-10-01.md`. Abschluss ist das gemeinsame exakte Gate,
keine Clientabnahme wurde aus Serverergebnissen abgeleitet.

## Clientabnahme: belegte Testkorrekturen

Nach Ende der Besitzer-Clients folgen serielle Fabric-/NeoForge-26.3-Tests.
Der erste Fabric-Lauf beanstandet beim Bundle-Tooltip die Zahl der Textelemente:
Die Produktionsanzeige zeigt inzwischen `64/192` statt Vanillas `Full`.
Der Test muss zuerst diesen tatsächlichen Kapazitätstext prüfen und anschließend
für seine bestehenden Skalierungs-Gegenproben nur die zusätzliche Beschriftung
abschalten. Faktoren 3, 6 und 1 sowie Wiederholbarkeit bleiben geprüft.
Keine Produktionsänderung; gezielte HUD-Tests auf beiden Loadern und vollständiges
exaktes SHA-Gate vor Push. Dimensions' Cloth-Unterklassenkorrektur und deren
sieben UI-Aufnahmen sind im modul-eigenen UI-Plan beschrieben.

Der Visuals-Smoke verlangt außerdem Legacy-CIT-Modelle trotz geladenem Simple
Models. Das widerspricht der dokumentierten Koexistenz: Simple Models übernimmt
diese Funktion. Der Smoke muss die tatsächliche Loader-Auswahl mit dem Guard
abgleichen, bei Koexistenz die unveränderte Renderkopie prüfen und seine
positiven Legacy-Prüfungen ohne Simple Models behalten. Beide Konstellationen
seriell prüfen; die temporäre isolierte Mod-Auswahl bytegleich wiederherstellen.
