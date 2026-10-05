# Config-Ideen vom 2026-10-05

Neue Bewertung zum aktuellen Code, keine Rekonstruktion der verschollenen Run-D-Liste.
Nur 26.3-Verhalten geändert. Die vier neuen Optionen gehören zur Server-Config
`simplebuilding.json`, sind per `/simplebuilding config` erreichbar und werden über
`ServerTuning` synchronisiert. Kein Client kann Gameplay-Werte vorgeben. Alte Linien
lesen weiterhin ihre bisherigen Laufzeitwerte; Sprachschlüssel liegen an beiden Orten.

## Vorhandenes zuerst geprüft

- `modules/simplelib/.../config/LibConfig.java`: `reinforcedSpeed`, `netheriteSpeed`,
  `enderiteSpeed` (1–16), `factorMedium/High/Extreme`, `twoBelowPenalty`, `afterglow*`,
  `heatRecheckTicks`, `warmBaseTicks`, `warmDurationTicks`, `warmBundleDurationTicks`,
  `eatSpeedBonus`. Crucible-Tempo/Hitze und Warm-Dauer benötigen keine zweite Config.
- `modules/simplesandwiches/.../config/SandwichConfig.java`: `maxIngredients` (1–5),
  Butterbonus, Effektgrenzen, Butter-/Käsezeiten und Erntefenster bestehen bereits.
- `modules/simplequalityoflife/.../config/SimplequalityoflifeConfig.java`:
  `qOL.linkedContainerRange` (8–128) und Schalter für Linked/Easy-Container bestehen.
- `ServerTuningConfig`: `hammock.timeFactor` (1–20), `soulLava.springChance` (0–0,05)
  und `fortressChance` (0–0,5), zahlreiche Werkzeug-/Maschinen-/Lootgrenzen bestehen.
  0 bei den Weltgen-Chancen verhindert die betreffende Seelenlava-Ersetzung.

## Zwölf geprüfte zusätzliche Stellschrauben

| Nr. | Idee | Entscheidung und Grund |
|---|---|---|
| 1 | Auto-Schmied: Wartezeit nach Redstone-Flanke | **Umgesetzt:** `server.machines.autoSmitherDelayTicks`, Standard 4, hart 4–100 Ticks. Admins können Automatisierung drosseln. Keine Beschleunigung gegenüber Vanilla-Crafter-Takt; ein Vorgang pro Flanke. Bereits geplante Arbeit behält ihre Frist. |
| 2 | Diamantblock mit Hammer: Kiesel-Ausbeute | **Umgesetzt:** `server.tools.diamondBlockPebbles`, Standard 81, hart 1–81. Nur Verlust gegenüber neun Eingangsdiamanten möglich, keine Ressourcenvermehrung. Betrifft ausschließlich den Vorschlaghammer, nicht fallende Ambosse. Hammer-Haltbarkeit bleibt unverändert. |
| 3 | Trefferpfeile: Rückgewinnungschance | **Umgesetzt:** `server.arrows.recoveryChance`, Standard 1, hart 0–1. Survival-Ökonomie ohne vollständiges Abschalten; Prüfung beim Treffer. Bereits gemerkte Pfeile bleiben. Unendlichkeit/Kreativ/Mehrfachschuss/Durchschlag/Amethyst bleiben ausgeschlossen, vorhandene Mengenbegrenzung gilt. |
| 4 | Linsen-Scan: Auffrischungsintervall und Leuchtdauer | **Umgesetzt:** `server.laser.scanIntervalTicks`, Standard 100, hart 20–100. Leuchtdauer jeweils Intervall +10 Ticks. Kürzeres Nachleuchten für Server; bei dauerndem Scannen öfter Ladungsverbrauch. PvP-/Claim-Prüfungen bleiben erhalten. |
| 5 | Hammer: Verschleiß je Umformung | **Verworfen:** Besitzerentscheidung lässt Hammer-Haltbarkeit bis zum eigenen Test unverändert. Die vorhandenen `RESHAPE_DAMAGE`/`RESHAPE_REVERSE_DAMAGE` bleiben maßgeblich. |
| 6 | Hammer: Schläge je Diamantblock | **Verworfen:** acht Schläge sind eine jüngere ausdrückliche Besitzerentscheidung (`DIAMOND_BLOCK_STRIKES`). Kein belegter Bedarf für einen weiteren Progressionsregler; Ausbeute aus Nr. 2 genügt für den aktuellen Balancewunsch. |
| 7 | Auto-Schmied: mehrere Rezepte je Impuls | **Verworfen:** würde den belegten Ein-Vorgang-pro-Flanke-Vertrag ändern und zusätzliche Ausgabe-/Verbrauchslogik benötigen. Nr. 1 deckt das Drosseln ab, kein Batch-Umbau. |
| 8 | Crucible: Zahl gleichzeitig laufender Aufträge | **Verworfen:** wäre eine neue Maschinenfunktion statt einer Zahl für den bestehenden Ablauf (`CrucibleBlockEntity`/`CrucibleJob`). Tempo und Hitze sind bereits begrenzt konfigurierbar. |
| 9 | Warm-Bonus je Lebensmittel statt global | **Verworfen:** kein konkreter Balancefall für eine zweite Pro-Item-Regeltabelle. Der vorhandene `warmable_food`-Tag steuert die Eignung, `eatSpeedBonus` bereits die Stärke. |
| 10 | Hängematte: eigene Spielerquote unabhängig von Betten | **Verworfen:** `HammockTime` nutzt bewusst `players_sleeping_percentage`. Eine konkurrierende Quote würde die bestehende Serverregel aufspalten; kein bestätigter Bedarf. |
| 11 | Linked-GUI: Zielchunks bei Zugriff automatisch laden | **Verworfen:** Zugriff soll keine neue Chunk-Ladeberechtigung schaffen. Die bestehende Reichweitenoption bleibt; kein Laden fremder oder entfernter Ziele durch GUI-Pakete. |
| 12 | Client: eigene Farbe für gescannte Umrisse | **Verworfen:** Scan verwendet Vanillas `GLOWING`; eigene Farben wären ein zusätzlicher Render-/Team-Eingriff. Rein kosmetisch, kein Balancing-/Admin-Nutzen, daher im Serverlauf nicht eingeführt. |

## Grenzen, Anwendung und Belege

`ServerTuningConfig.validate()` begrenzt Datei-/Befehlswerte, die vier Getter in
`ServerTuning` begrenzen nochmals beim Lesen. NaN und unendliche Pfeilchancen werden 1.
`ConfigOptions` erfasst die Felder reflektiv; serverseitig, ohne Neustart oder `/reload`.
EN/DE-Tooltips nennen Grenzen und Defaults. Die Wiki-Config-Seite wird aus diesen
Quellen erzeugt. Generierte Wiki-Exporte enthalten Standardwerte; der zur Laufzeit erzeugte
InWorld-Rezeptkatalog liest dieselbe Hammer-Ausbeute wie der Server.

Verhalten: `AutoSmitherBlock.neighborChanged`, `SledgehammerItem.crushDiamondBlock`,
`ArrowRecovery.onHit`, `LaserBeam.beamAtEntity`. GameTests: vier neue ConfigOptionTests
plus angepasste CombatTests, Default-/Sprach-/Befehlsprüfungen. Keine Moduldatei wurde
geändert; deshalb keine neuen Modul-Testläufe erforderlich. Aktuelle Testzeilen und
Einschränkungen stehen im Plan und in HANDOFF, historische Zahlen sind kein neues Gate.
