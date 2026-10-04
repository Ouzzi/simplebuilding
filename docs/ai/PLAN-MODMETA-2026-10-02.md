# Modul-Metadaten und Enderit-Vertrag (2026-10-04)

## Plan
1. Optionale Partner aus Manifest, Loader-Metadaten und Laufzeit-/Datenbedingungen abgleichen.
   Alle Module unter `modules/` bekommen identische optionale Partner auf den drei Loadern.
   Projekt-IDs werden ueber `modId` auf echte Loader-IDs abgebildet.
2. `tools/multimod.py` validiert diesen Vertrag mit der Python-Standardbibliothek;
   Regressionstests pruefen fehlende, zusaetzliche und falsch klassifizierte Abhaengigkeiten.
3. Riding-Rezept und optionaler Reparaturtag verwenden `c:ingots/enderite`.
   SB liefert den Tag bereits unter `src/main/resources/data/c/tags/item/ingots/enderite.json`.
   Bestehende Fabric-/NeoForge-/Forge-Conditions bleiben erhalten.
4. Datenchecks, Wiki und Server-Gates ausfuehren; die zwei Queue-Punkte abschliessen.
   Nur auf `gpt-modmeta` committen, niemals pushen oder einen Client starten.

## Dateien und Grenzen
`modules/modules.json`, Loader-Metadaten aller Module, `tools/multimod.py`, zugehoerige
Python-Tests, Riding-Rezept/Reparaturtag/Datencheck und GameTest, betroffene Wiki-Exporte,
`.claude/QUEUE.md`. Keine bleibende Aenderung an der SB-Datagen.
Der Kern besitzt loaderabhaengige Drittmod-Integrationen; der neue Gleichstandscheck
gilt wie beauftragt fuer die Zusatzmodule unter `modules/`, nicht fuer den Kern.
Vorhandene optionale Metadaten bleiben erhalten; harte Abhaengigkeiten werden nicht gelockert.
Dimensions erkennt auch externe Claim-Mods; diese werden als optionale Partner dokumentiert.

## Verifikation
UV-Python: Multimod- und Riding-Datencheck, Regressionstests.
Nur `:mc26_3:fabric:runDatagen :mc26_3:fabric:syncGenerated263` fuer Datagen.
Wiki mit Pillow-venv `--all`, danach UV-Python `--all --check`.
Riding-Serverziele Fabric/NeoForge/Forge 26.3, Kern Fabric/NeoForge 26.3;
Ergebniszeilen lesen, nicht nur Exitcodes. `check -q`, 26.2 Fabric/NeoForge-Compile,
Forge-26.3-Compile. Testzentrale und Item-Abdeckung in den Kern-Servertests.

## Risiken
`simpledimensions` hat die Loader-ID `simpledimension`. TOML muss semantisch gelesen
werden; Forge verwendet `mandatory=false`. Fehlende Metadaten duerfen nicht still
uebersprungen werden. Test-Fixtures muessen die nun geprueften Metadaten enthalten.

## Abweichung waehrend der Umsetzung
Der erste Suchlauf erfasste Datagen und generierte Tags, uebersah aber den bereits
handgepflegten gemeinsamen Tag in `src/main/resources`. Eine zunaechst ergaenzte
Datagen-Ausgabe verursachte deshalb einen Duplicate-Resource-Fehler in allen drei
26.3-Loadern (Riding-Lauf `2026-10-04T21-29-37Z-f7b0`, Kernlauf
`2026-10-04T21-30-39Z-46ca`: keine Tests ausgefuehrt). Die eigene redundante
Datagen-Aenderung und ihre Ausgabe wurden entfernt. Der Datencheck prueft nun die
vorhandene Quelle; der GameTest prueft den geladenen Tag. Der finale Stand braucht
keine neue SB-Tag-Datei und keine Aenderung am Forge-Konverter.

## Ergebnisse
- Manifest und alle Zusatzmod-Metadaten stimmen ueberein. Vorhandene ModMenu-Integrationen
  bei Riding/QoL und die Claims-Kopplung Tweaks/Dimensions sind nun ebenfalls erfasst.
  Dimensions' fuenf externe Claim-Mod-Erkennungen sind optional, keine harten Abhaengigkeiten.
- `tools/multimod.py`: `Multimod registries valid`; 21 Launchhub-Modultests: `OK`.
  Neue Faelle pruefen fehlende/zusaetzliche/harte Partner, fehlende Metadaten und Loader-ID-Aliase.
- Riding-Datencheck gruen. GameTest prueft den geladenen `c:ingots/enderite`-Tag;
  bestehende Rezept- und Reparaturtests laufen mit dem neuen Tag auf allen drei Loadern.
  Verarbeitete Forge-Rezeptdatei: `forge:condition.type = forge:mod_loaded`,
  `modid = simplebuilding`, `addition = #c:ingots/enderite`.
- Serverlauf `2026-10-04T21-34-21Z-cc14`: **alles gruen: 1997/1997 bestanden, 0 rot**.
  Riding Fabric/NeoForge/Forge jeweils 39/39; Kern Fabric/NeoForge jeweils 940/940.
  Testzentralen-Neubau sowie vollstaendige Item-/Blockabdeckung auf beiden Kern-Loadern bestanden.
- Wiki mit Pillow-venv `--all` und UV `--all --check`: Exit 0,
  `wiki: up to date, everything documented.` Die Vollgenerierung aktualisiert auch
  zuvor veraltete Kern-Exporte zu bereits implementierten senkrechten Staeben;
  keine neue Gameplay-Aenderung daran.
- Kein Clientstart, kein Push, kein Merge, kein Port-Run.
- Gesamt-Gate am 2026-10-05: `gradlew.bat --offline check -q :compileJava
  :neoforge:compileJava -Pforge263=true :mc26_3:forge:compileJava`: **GATE_EXIT=0**,
  inklusive 54 Wiki-Regressionstests (`OK`). Der erste Online-Gateversuch scheiterte
  an `UnresolvedAddressException` beim Mojang-Versionsmanifest. Offline nutzte der
  unveraenderte Gateumfang erfolgreich den vorhandenen Cache.
- Offen innerhalb dieses Auftrags: nichts. Standalone-Ziele und andere Queue-Punkte
  bleiben eigene Arbeiten. Kein Clienttest, keine Besitzerwelt angefasst.
