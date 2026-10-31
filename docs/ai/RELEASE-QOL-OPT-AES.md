# Release-Audit: Simple Quality of Life / Simple Optimization / Simple Aesthetics

Lebendes Dokument (Besitzer-Auftrag 31.10.2026, `/root/b-auftrag-release-qoa.md`). Branch `b-rel32` (Basis `a-bundle82` de8215293).
Neue Besitzer-Punkte kommen in Abschnitt 6. **Veröffentlicht wird nichts ohne ausdrückliches „ja“ des Besitzers.**

## 1. Plan

### Ist-Stand (31.10., Code auf de8215293)
| Familie | Mod | Java-Zeilen | Mixin-Klassen | Version |
|---|---|---|---|---|
| QoL | simplequalityoflife (Super-Mod) | 876 | 1 | 0.9.0 |
| QoL | simpleinterfaces | 8395 | 44 | 0.1.0 |
| QoL | simpletools | 5147 | 19 | 0.1.0 |
| QoL | simplemovement | 1129 | 6 | 0.1.0 |
| QoL | simplefarming | 1021 | 3 | 0.1.0 |
| QoL | simplemobmanners | 930 | 5 | 0.1.0 |
| QoL | simpleloot | 787 | 6 | 0.1.0 |
| QoL | simpleweather | 495 | 4 | 0.1.0 |
| Opt | simpleoptimization (Super-Mod) | 159 | 0 | 0.1.0 |
| Opt | simpleoptrender | 1103 | 10 | 0.1.0 |
| Opt | simpleoptmemory | 425 | 2 | 0.1.0 |
| Opt | simpleoptserver | 638 | 1 | 0.1.0 |
| Aes | simpleaesthetics (Super-Mod) | 208 | 0 | 0.9.0 |
| Aes | simplevisuals | 1950 | 17 | 0.9.0 |
| Aes | simplemodels | 1808 | 7 | 0.1.0 |
| Aes | simplesounds | 615 | 1 | 0.1.0 |

Bekannte Release-Blocker vorab:
- `simplelib/api/DevVariants.java:88` `DEFAULT_ON = true` → vor Release auf dev-only (`devEnvironment`).
- Opt-Konzept: e3 (Benchmark auf Besitzer-PC, `priorities.json`) und e5 (Modpack-Kompatibilität K1–K23) offen, beide brauchen den Besitzer (WP-34).
- Versionsschema uneinheitlich (0.1.0 / 0.9.0, AUDIT-STIL ST-14).
- simpleinterfaces wird parallel von `b-fix32` (Tastatur-Layout, Mixin-Prüfer) geändert → das Audit fasst `client/keys`, `KeysScreen` erst nach dessen Merge an.

### Vorgehen
Gemeinsame Prüfliste (Abschnitt 2) für jede Mod. Jeder Audit-Agent:
1. liest den Code Zeile für Zeile (ohne Gradle), samt genutzter simplelib-/Kern-Teile;
2. fixt Befunde gleich (kleinste korrekte Änderung) oder notiert sie mit Schwere P1 (Absturz/Datenverlust/Release-Blocker), P2 (Fehlverhalten), P3 (Kleinkram);
3. schreibt `modules/<mod>/AUDIT.md` (Bericht, Abschnitt 5) und seinen Teil des Abnahmeplans (Abschnitt 3) in `docs/ai/release-qoa/<familie>.md`;
4. fährt am Ende EIN sbt-Lauf: Compile + `module-<mod>-fabric-263` + `module-<mod>-standalone-fabric-263` je Mod + Client-Mixin-Prüfer.
Danach: Bündel `b-rel32` (alle Audit-Branches) → bprep → Gate → echter Client-Start `mixinaudit-client-263` (xvfb) → Release-Checkliste (Abschnitt 4).

### Agenten-Aufteilung (gestaffelt, Gradle-Läufe laufen ohnehin seriell über die heavy-Schlange)
| Agent / Branch | Mods | Start |
|---|---|---|
| b-rel32-qola | simplequalityoflife, simpleinterfaces (ohne Tastatur-Teil bis b-fix32 gemergt) | sofort |
| b-rel32-opt | simpleoptimization, simpleoptrender, simpleoptmemory, simpleoptserver | sofort |
| b-rel32-qolb | simpletools, simplemovement, simplefarming, simplemobmanners, simpleloot, simpleweather | wenn ein Queue-Agent fertig ist |
| b-rel32-aes | simpleaesthetics, simplevisuals, simplemodels, simplesounds | wenn ein weiterer Queue-Agent fertig ist |
| Hauptsitzung | Dokument, Abnahme-Seite, Release-Checkliste, Bündel/Gate, DevVariants-Schalter | laufend |

### Zeitplan (Schätzung)
- Phase 2 Lesen/Fixen: je Agent 3–5 h; Start gestaffelt → fertig ca. 31.10. nachmittags.
- Phase 3 Tests: je Agent ein sbt-Lauf (Schlange: 1–3 h Wartezeit), dann Bündel + Gate (~3 h) → ca. 31.10. abends / 01.11. früh.
- Phase 4 Berichte + Abnahme-Seite: direkt danach.

### Selbstprüfung des Plans
- Deckt jede Mod + Sub-Mod einzeln und standalone ab: ja (Tabelle oben, Standalone-Targets je Mod).
- Gradle-Grenze 4–5: die zwei späten Agenten starten erst, wenn Queue-Agenten fertig sind.
- Konflikte mit laufender Arbeit: simpleinterfaces/Keys (b-fix32) ausgenommen bis Merge; sonst keine laufenden Branches in diesen Mods.
- Nichts wird hochgeladen; Modrinth/CurseForge-Texte nur als Entwurf.

## 2. Audit- und Check-Plan je Mod (gemeinsame Prüfliste)
Für jede Mod abzuhaken (Ergebnis im jeweiligen `AUDIT.md`):
- **C1 Code:** jede Klasse gelesen; Fehler, Null-/Grenzfälle, Server/Client-Trennung (keine Client-Klassen im Server-Pfad), Thread-Sicherheit (statische Maps, Netzwerk-Handler auf dem Haupt-Thread), Leistung (Tick-Pfade, Allokationen), tote Teile.
- **C2 Mixins:** Ziel + Signatur gegen 26.3 (`tools/check_client_mixins.py`), `require`/Fehlertoleranz, Konfliktrisiko mit verbreiteten Mods (Sodium, Iris, Lithium, JEI, ModMenu) – im Bericht je Mixin ein Satz.
- **C3 Config:** Defaults sinnvoll, `@Category`/Tooltip, Lang EN/DE mit „Default:“/„Standard:“, Config-Befehl angemeldet, Grenzen geklemmt.
- **C4 Daten:** Rezepte, Tags, Loot (26er-Format), Lang EN/DE vollständig (`tools/check_lang_keys.py`), Texturen/Modelle vorhanden und referenziert.
- **C5 Unabhängigkeit:** standalone spielbar (PRINZIPIEN-MODUL-UNABHAENGIGKEIT), Fremdzugriffe nur bedingt (`isModLoaded`), fabric.mod.json depends/recommends korrekt.
- **C6 A12:** Vanilla-Nutzen und Eingriffsstärke je Feature, stark eingreifende Features abschaltbar.
- **C7 Netzwerk/Multiplayer:** Pakete validiert (Server traut dem Client nicht), Verhalten mit Vanilla-Client (Server-only-Teile) und auf Dedicated Server.
- **C8 Lizenz/Assets:** keine Mojang-Pixel (nur Vanilla-abgeleitet beim Build), keine Secrets/IPs/Pfade im Code oder in Ressourcen, Lizenzdatei.
- **C9 Offen:** Queue-Zeilen, Memory-Einträge, `TODO`/`FIXME` im Code der Mod.

## 3. Abnahmeplan für den Besitzer
Wird aus `docs/ai/release-qoa/*.md` zusammengeführt (pro Mod In-Game-Schritte mit erwartetem Ergebnis). Klickbare Seite: `/root/sb-tools-b/previews/release-qoa-abnahme.html` (Artifact B-Konto, Antwortcode je Punkt ok/nicht ok). *Folgt nach Phase 2.*

## 4. Release-Checkliste
- [ ] Version je Mod festlegen (Vorschlag: 1.0.0 für alle 16 Jars) + Changelog je Mod
- [ ] fabric.mod.json: depends/recommends, Super-Mod requires Sub-Mods (Supermod-Konzept), Kontakt/Quellen/Lizenz, Icon
- [ ] Beschreibung je Mod (Kurz + Lang), Lizenz
- [ ] Build-Jars je Mod standalone (Fabric 26.3), Gate grün
- [ ] Echter Client-Start `xvfb-run -a python3.12 tools/testrunner/run.py --targets mixinaudit-client-263` ohne Fehler
- [ ] Kompatibilität: Server-only-Teile mit Vanilla-Client, Dedicated Server startet mit jeder Mod allein
- [ ] Dev-Schalter aus: `DevVariants.DEFAULT_ON` → dev-only; keine Dev-Menüs/Testzentralen im Release sichtbar
- [ ] Wiki- und Handbuch-Seiten aktuell
- [ ] Seitentexte Modrinth/CurseForge (nur Entwurf, `docs/release/`)
- [ ] Besitzer-Abnahme (Abschnitt 3) vollständig ok
- [ ] Freigabe „ja“ des Besitzers → erst dann hochladen

## 5. Berichte je Mod
Stand 31.10. (Aesthetics läuft noch):
| Mod | Urteil | P1/P2/P3 | Tests | Bericht |
|---|---|---|---|---|
| simplequalityoflife | shipbar | 0/0/4 | grün (6+6) | modules/simplequalityoflife/AUDIT.md |
| simpleinterfaces | shipbar | 0/1 (fix)/8 | grün (41+41) | modules/simpleinterfaces/AUDIT.md |
| simpletools | shipbar | 0/0/7 | grün | modules/simpletools/AUDIT.md |
| simplemovement | shipbar | 0/1 (fix)/2 | grün | modules/simplemovement/AUDIT.md |
| simplefarming | shipbar | 0/2 (fix)/2 | grün | modules/simplefarming/AUDIT.md |
| simplemobmanners | shipbar | 0/0/3 | grün | modules/simplemobmanners/AUDIT.md |
| simpleloot | shipbar | 0/1 (fix)/1 | grün | modules/simpleloot/AUDIT.md |
| simpleweather | shipbar | 0/0/1 | grün | modules/simpleweather/AUDIT.md |
| simpleoptimization | shipbar | 0/0/1 | Nachlauf (Zeitgrenze bei sb-test-Überlast) | modules/simpleoptimization/AUDIT.md |
| simpleoptrender | shipbar, Vorbehalt Sodium/Iris nie gelaufen | 0/0/5 | Client-Smoke Nachlauf | modules/simpleoptrender/AUDIT.md |
| simpleoptmemory | shipbar | 0/1 (fix)/3 | grün | modules/simpleoptmemory/AUDIT.md |
| simpleoptserver | shipbar | 0/1 (fix)/3 | grün | modules/simpleoptserver/AUDIT.md |
Familien-Abnahmepläne: docs/ai/release-qoa/qol-a.md, qol-b.md, opt.md (aes.md folgt). Noch für alle offen: echter Client-Start im Bündel, Lauf mit Sodium/Iris/Lithium/JEI/Mod Menu.

## 6. Weitere Besitzer-Punkte
*(leer)*

## Rückfragen an den Besitzer
- R1 Versionsnummer zum Release: alle 1.0.0? (Empfehlung: ja)
- R2 e3/e5 (Benchmark auf deinem PC, Modpack-Test) vor dem Opt-Release oder Opt als „Beta“ ausliefern? (Audit: blockiert 1.0 nicht, wenn die Texte keine Leistungszusagen machen; vorhandene Messungen belegen nur S5b.)
- R3 Opt: Mod-Menu-Bildschirm vor Release nötig (heute nur JSON + Befehle)?
- R-INT-1 Zoom-Taste C unbelegt lassen, solange `zoomHotkey` aus ist?
- Q-B1 Cloth Config ist bei den sechs QoL-Sub-Mods harte Abhängigkeit, obwohl nur der Client-Screen sie braucht – optional machen?
- Q-B2 Simple Weather dünnt Regen standardmäßig auf 20 % aus – Standard so lassen?
- Q-B3 Leitern standardmäßig doppelt so schnell (0,4 statt 0,2) – so lassen?
- Q-B4 Biom-Pinsel-Rezept braucht 3 Nether-Sterne – so lassen?
- Q-B5 Halb fertigen Death-Vault-Rückkauf in der Release-Beschreibung nennen oder verschweigen/ausblenden?
