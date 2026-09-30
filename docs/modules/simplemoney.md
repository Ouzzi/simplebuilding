# Simple Money — vollständiges Quellinventar und Port 26.3

Quelle (nur gelesen): `C:/Users/oussa/Downloads/Minecraft/Mine/custom created mods/simplemoney`, Fabric 1.21.11, Version 1.2.16, Commit `a9c12a7` (sauberer Quellarbeitsbaum), CC0. Elf Java-Dateien; keine Tests. README, beide Sprachdateien, acht Rezeptdateien und alle Java-Dateien geprüft. Leere Entity-/Rezept-/Client-/Datagen-Klassen und leere Mixins registrieren nichts.

## Features und persistente IDs

Namensraum `simplemoney` unverändert. Sieben Items: `special_paper` (64), `special_fiber` (16), `resin_fiber` (16), `blank_note` (64, uncommon), `refined_blank_note` (64, uncommon), `raw_bill` (64, rare), `money_bill` (64, epic, feuerfest, Glanz). Rechtsklick Geldschein: Klang + Partikel ohne Verbrauch oder Bildschirmtext. Kreativtab `simplemoney:money_items`, Originalreihenfolge. Keine Blöcke, Entities, Verzauberungen, Befehle, Tastenkürzel, Komponenten oder Welt-Speicherformate.

## Rezepte (alle Original-IDs erhalten)

- `special_paper_from_crafting_table`: Papier / Honigwabe / Papier → 1 Spezialpapier.
- `special_fiber_from_crafting_table`: `CGA/DAG/GDC`, C Kupfernugget, G Goldnugget, A Amethystscherbe, D Diamant → 1 Spezialfaser.
- `resin_fiber_from_crafting_table`: `ERH/RBR/HRE`, E Eisennugget, R Harzklumpen, H Honigwabe, B Knochenmehl → 1 Harzfaser.
- `blank_note_smithing`: Eiseningot als Vorlage + Spezialpapier + Harzfaser → 1 Banknotenrohling.
- `refined_bank_note_blank_smithing`: Goldingot als Vorlage + Banknotenrohling + Spezialfaser → 1 veredelter Rohling.
- `raw_bill_from_crafting_table`: `#I#/PPP/I#I`, # grüner Farbstoff, I Tintenbeutel, P veredelter Rohling → 3 rohe Scheine.
- `money_bill_from_blasting`: roher Schein → Geldschein; 10000 Ticks, 20 XP.
- `rocket_from_paper`: `###/#G#/#G#`, # Papier, G Schwarzpulver → 1 Rakete.

## Handel, Beute und Konfiguration

47 zusätzliche Angebote: Fletcher 1/3, Librarian 1–5, Cleric 1, Mason 1/2, Farmer 1/2, Armorer 2–5, Toolsmith 3–5, sieben Wandering-Trader-Angebote. Alle Preise, Mengen, Nutzungen, XP, Rabatte und gewichteten Pools stehen einzeln zweisprachig in `modules/simplemoney/wiki/manual.json` und maschinenlesbar in `generated/resources/data/simplemoney/villager_trade`. Die eingefrorene Quellvergleichstabelle liegt unter `shared/resources/data/simplemoney/testing/source-trades.json`.

Sieben Truhentypen: Iglu 30 % × 1; Verlies 20 % × 2–4; Endsiedlung 20 % × 1–6; Mine 35 % × 1–2; Schiffswrack-Schatz 30 % × 1–2; Festungsbibliothek 25 % × 8–16; vergrabener Schatz 30 % × 1–4. Konstanten: `MoneyLoot.ENTRIES`.

`config/simplemoney.json`: `trades.enableVillagerTrades=true`, `trades.enableWanderingTrades=true`. Server entscheidet beim Datenladen; Änderung benötigt Reload/Neustart. Eigene Cloth-Seite mit Namen, Tab, Tooltip und Default; Fabric optionales Mod Menu, NeoForge Mods-Seite. Keine weiteren Optionen im Quellcode.

## Überschneidungen / Abweichungen

Keine Registry-Kollisionen, keine Mixins, keine Tastenkürzel. SimpleBuilding verändert dieselben Händler und mehrere derselben Truhen; Simple Money ergänzt Pools/Tags ohne Vanilla oder SimpleBuilding zu ersetzen. Keine Kerne beim Steinmetz hinzugefügt. Lagerkompatibilität wird über öffentliche Registry-IDs und Vanilla Container geprüft, ohne interne SimpleBuilding-Klassen zu importieren. Das zusätzliche Raketenrezept hat eine andere Form/Menge als Vanilla und bleibt erhalten; SimpleBuildings Raketen-Stapelkonfiguration bleibt maßgeblich.

README ist veraltet: kein Phantomhaut-Rezept, keine behaupteten festen 5-Smaragd-Wechselkurse oder garantierten Mending-Angebote. Tatsächlicher Wechselkurs 3–35 Smaragde pro Schein, zufällige gewichtete Pools. Quellcodewerte sind maßgeblich. `rocketStackSize`/`vaultCooldownDays` waren nur Sprachreste, nicht Features; nicht dupliziert. Fehlende deutsche Harzfaser-/Rohlingtexte ergänzt, beschädigte Umlautkodierung bereinigt. Original-PNGs unverändert übernommen; keine neue Pixelkunst.

## Integration / Verifikation

Fabric: Auswahl im Manifest/Launch Hub, `integration-263` für den Bestand und `module-simplemoney-fabric-263` für diesen Katalog. NeoForge: `module-simplemoney-neoforge-263`, eigene Welt unter `integration/run-neoforge-263`, SimpleBuilding mitgeladen. Kein Besitzer-Save wird verwendet. Gemeinsamer Testkatalog prüft Start/Registry, alle Rezepte, alle 47 echten Handelsangebote (je 128 Ziehungen), alle Beutetypen, Config, Sprach-/Assetvollständigkeit und fremde Items im verstärkten Trichter.

Forge 26.3 und 26.2/1.21.11/26.4 folgen erst im freigegebenen Port-Run. Dazu Loader-Einstieg, Configbedingungen, Lootadapter, Client-Config und Testadapter ergänzen; Registry-IDs/Configschlüssel erhalten. Client-Smoke und visuelle Besitzerabnahme werden separat dokumentiert; keine erfolgreiche Prüfung behaupten, bevor sie ausgeführt wurde.

## Abschlussverifikation (2026-09-30)

- Bestand 26.3: Fabric 777/777 + NeoForge 777/777; Integration 1/1, **1555/1555 alles gruen**, Run `2026-09-30T13-22-27Z-8711`. Testzentralen neu gebaut, Abdeckung bestanden.
- Eigene finale Modsuite mit korrigierten Quell-Pools: **20/20 alles gruen**, Run `2026-09-30T13-26-13Z-38db`. Alle sieben Items, acht echte Herstellungswege, 47 Angebote, sieben Beutetypen, Config, Assets/Sprache und Lagerinteraktion geprüft.
- Beide Handelsschalter aus: **2/2 alles gruen**, Run `2026-09-30T13-16-41Z-43e8`; alle Money-Angebote fehlen, SimpleBuilding-Angebote bleiben. Isolierte Configdateien exakt wiederhergestellt. Der frühere Lauf `13-10-48Z-7e21` war durch eine zu früh vorbereitete Configdatei gestört; nicht als Verifikation verwendet.
- Fabric-Client-Smoke: echter Start, Titel zur Welt, alle sieben Itemregistrierungen, sieben Texturen in der Hotbar und Configseite; Exit 0. Screenshots und unveränderte Originaltexturen als 16-fach Vergleich unter `docs/previews/simplemoney/`. Kein Besitzerclient war bei der Prozessprüfung aktiv. Die Configseite wurde ohne Speichern geschlossen.
- Launch Hub **35 Unit-Tests grün**, einschließlich sequenzieller Modul-Testschritte. Modul im Mods-Manifest und `launch_targets.json`; Fabric über die Integration startbar, NeoForge über den dedizierten Integrations-Testserver.
- Wiki generiert und `--check` aktuell; eigener Datenvertrag-Gate grün (57 zweisprachige Kapitel). Vollständiges `gradlew.bat --no-daemon check -q` im Worktree **Exit 0**, Ausgabe gelesen; gemeinsame 26.2-Kompilierbarkeit erhalten.
- Nicht verifiziert: NeoForge-Client/Configdarstellung, echte bestehende 1.21.11-Spielwelt, Besitzerwelt und deren Testzentrale. Forge 26.3 sowie die anderen MC-Linien nicht portiert oder spielgetestet. Quelle unverändert, keine Änderungen an deren Git-Arbeitsbaum.
- Entscheidungen: Quell-Balancing erhalten; README-Abweichungen nach Code korrigiert, leere Features/Sprachreste nicht übernommen. Keine Registry-Kollisionen; Händler/Beute additiv. Besitzer-Abnahme von Bedienung/Balancing bleibt offen. Kein Push, kein Merge.

- Abschließender kombinierter Harness-/Modlauf nach Trennung der Fabric-Tasks: **21/21 alles gruen**, `2026-09-30T13-35-03Z-5946`. Eigene Berichte/Filter, gemeinsame Fabric-Instanz seriell. Danach vollständiges `gradlew.bat --no-daemon check -q` erneut **Exit 0**, Ausgabe gelesen.
