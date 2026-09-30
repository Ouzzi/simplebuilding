# Balancing-Zentrale (Multimod, 26.3)

Start: `python tools/devserver/serve.py --no-browser` auf http://127.0.0.1:8770/.
Der Eintrag `balancing-zentrale` in `.claude/launch.json` startet denselben Server.
Nur Python-Standardbibliothek; API-Schreibzugriffe brauchen JSON und `X-Balance-Client: 1`.

## Mod-Auswahl und Arbeitsablauf

Die Kopfzeile bietet alle Eintraege aus `modules/modules.json` an. `?mod=<id>` waehlt
auch bei direkten Links den Mod; ohne URL-Auswahl wird die letzte Auswahl aus localStorage
verwendet (Zugriff in try/catch). Entwuerfe und Rechner-Praeferenzen sind je Mod getrennt.
Die bisherigen SimpleBuilding-Browser-Schluessel bleiben erhalten.

Alle Seiten, Quellen, Zeitrechner, Solver, Auslese-Berichte, Versionen und Rollbacks
verwenden denselben ausgewaehlten Service. Die Uebersicht zeigt pro Mod die gespeicherten
Werte, offenen Plaene und Browser-Entwuerfe sowie Name, Beschreibung, Version, MC und Loader.
"Aenderungen" zaehlt die aktuell gespeicherten Werte, nicht die historische Zahl der Klicks.
"Offene Plaene" umfasst gespeicherte Werte, die nicht ihrem Quellwert entsprechen,
inklusive Rechner-Annahmen und verwaister Werte. Browser-Entwuerfe werden gesondert gezaehlt.

Wert bearbeiten -> alter Wert rot durchgestrichen -> Speichern mit Zusammenfassung
(alt/neu, Datei/Zeile, Wirkung, betroffene Items) -> Bestaetigen -> neue Version.
Rechner-Zeiten bleiben editierbar: der Solver berechnet Entwuerfe; erst der normale
Speichern-Dialog schreibt. Ohne "Werte direkt in die Mod schreiben" bleibt ein Plan.
Rollback legt eine neue Version an und stellt Quellen mit den gespeicherten
Original-Literalen wieder her. Vor dem Schreiben werden alle Stellen auf Konflikte
geprueft; teilweise fehlgeschlagene Schreibvorgaenge werden zurueckgenommen.

## Manifest und Quellen

Jeder Manifest-Eintrag enthaelt `id`, `name`, `displayName`, `description`, `version`,
`loaders`, `minecraft: "26.3"`, `paths`, `requires`, `optional` sowie bisherige Felder.
Pfade sind repo-relativ; absolute Pfade ausserhalb des Repos und `..`-Ausbrueche werden
abgewiesen. Unbekannte Mod-Ids werden vor jedem API-Schreibzugriff abgewiesen.

`paths`: `root`, `shared`, `fabric`, `neoforge`, `forge`, `generated`, `lang`,
`wikiManual`, `balanceDir`. Das Scaffold `tools/newmod.py` erzeugt diesen Vertrag mit.
Fehlende Daten/Loader-Verzeichnisse und Mods ohne Stellwerte sind erlaubt.
Modell-/Textur-Aufloesung beruecksichtigt Modul-Assets und deren generated-Verzeichnisse.

SimpleBuilding behaelt seine spezialisierten Leser (Config, Beute, Materialien,
Verzauberungen, Handel, Rezepte, Erze, Item-Export, Trank-Pads). Die Mod-UI schreibt
nur eindeutige 26.3-Stellen: gemeinsame Dateien bleiben gemeinsam, separate Port-Kopien
werden nicht beschrieben. Overlay-Konstanten haben Vorrang. Ohne eindeutige 26.3-Stelle
ist der Wert nur lesbar. Das aeltere, nicht modulgebundene `Service`-Interface bleibt
fuer Regressionstests mit seinen bisherigen Linienregeln verfuegbar.

Zusatzmodule lesen:

- Balance-benannte `static final int/long/float/double`-Konstanten aus `shared` und
  Loader-Java-Baeumen, inklusive ausrechenbarer Ausdruecke und Alias/Transform-Regeln.
- Numerische und boolesche Blaetter handgeschriebener JSON-Daten im eigenen Namespace
  unter gemeinsamen oder Loader-Resources; diese werden an Ort und Stelle geschrieben.
- Item-Exporte (`generated/wiki/items.json`) und numerische Blaetter aus
  `generated/data/<id>` bleiben Planung: generated ist keine
  schreibbare Quelle. Ein Producer sollte Stellwerte als benannte Java-Konstanten oder
  handgeschriebene Daten halten. Automatische Rueckzuordnung beliebiger Java-Datagen-
  Builder zu generated JSON ist nicht implementiert; dafuer braucht es einen Adapter.
- Rezeptdaten und 26.3-`villager_trade`-Angebote nutzen die bestehenden Strukturen der
  Rechner. Lang-Namen verwenden den eigenen Namespace aus `paths.lang`.
- Gewoehnliche JSON-Beute mit item/empty-Eintraegen, festen/uniformen Wuerfen und
  festen/uniformen `set_count`-Mengen wird modelliert. Ereignisse/h sind editierbare
  Rechner-Annahmen (Standard 1/h, bewusst kein belegter Gameplay-Wert). Der Solver
  verwendet Item-Gewichte, gemeinsame Wuerfe oder als Fallback die Ereignisrate.
  Unbekannte Bedingungen/Funktionen/Provider werden im Bericht genannt und nicht
  mit einer unbewiesenen Beschaffungszeit angezeigt. Zahlen bleiben in "Alle Stellwerte".

## Ablage: keine Migration und kein Datenverlust

SimpleBuilding nutzt weiterhin **`balance/`**, genau wie `paths.balanceDir` im Manifest.
Dies ist die ausdrueckliche Legacy-Ausnahme. Es gibt keinen zweiten aktiven Store, keine Kopie, kein Verschieben
und keine Synchronisation zweier Versionierungsfolgen. Saemtliche vorhandenen Versionen,
angewendeten Stellen und Rollback-Originaltexte werden unveraendert gelesen.

Andere Mods nutzen **`balance/<id>/`**, entsprechend `paths.balanceDir`:

```
balance/<id>/balance.json
balance/<id>/versions/v0001.json
balance/<id>/versions/v0001.applied.json
balance/<id>/applied-log.jsonl
```

Versionsdateien sind unveraenderlich; aktueller Stand wird atomar geschrieben.
Die vorhandenen Regeln fuer Sperre, `baseVersion`, Konflikte und Absturz-Wiederherstellung
bleiben je Mod bestehen. `--store <ordner>` verwendet fuer SimpleBuilding genau diesen
Ordner und fuer Zusatzmods dessen Unterordner `<id>`; Tests schreiben nur Temp-Ablagen.

## Datagen und Gate

`python tools/devserver/serve.py --check` prueft **jeden Manifest-Mod**, auch wenn er
in der Integration deaktiviert ist. `--check --json` liefert Ergebnisse nach Mod sowie
aggregierte Fehler, Hinweise und Zaehler. Gradles bestehendes `checkBalance` (Teil von
`check`) ruft diesen Einstieg auf. Angewendete Werte gegen Code und vorhandene
Source/generated-Verknuepfungen bleiben geprueft; Plaene/verwaiste Werte sind Hinweise.

SimpleBuilding-Datagen in der Mod-UI startet ausschliesslich
`:mc26_3:fabric:runDatagen`, danach `python wiki/generate.py`.
Zusatzmods koennen einen expliziten `datagenTask` im Manifest hinterlegen, z. B.
`:modules:simplemoney:fabric:runDatagen`; ohne Task gibt es eine erklaerte Fehlermeldung.
Der Befehl wird als Argumentliste ausgefuehrt. Laufende Spiele dieses Checkouts werden
vorher erkannt. Auftrag, Abbrechen, Ergebnis und Diff bleiben je Mod sichtbar.
Andere Minecraft-Linien werden erst im eigenen Port-Run bearbeitet.

## API und Tests

Alle vorhandenen JSON-Routen akzeptieren `?mod=<id>`: state, preview/save,
version/history, rollback, calc/reverse/solve-time, overview, reload, check,
pending-apply/apply-planned, handover, docs, datagen und cancel.
`GET /api/modules` liefert Metadaten und die moduebergreifende Uebersicht.
Die Auswahl ist request-lokal, kein globaler veraenderlicher "aktiver Mod" auf dem Server.

`python -m unittest discover -s tools/devserver/tests`: bestehende 107 Tests plus
Modul-Fixtures fuer Java/JSON, Vorschau, Schreiben, Drift, bytegenauen Rollback,
Legacy-Historie, getrennte Stores, HTTP-Routing, Quellen/Rechner/Solver,
generated-Planung, unmodellierte Beute und das wiringexample-Modul ohne Stellwerte.
Desktop-/Handy-Sichtpruefung ist ein eigener Schritt; HTTP und Syntaxpruefung ersetzen sie nicht.
