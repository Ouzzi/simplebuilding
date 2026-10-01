# Prüfbelege der Folgewelle am 2026-10-01

Diese Welle folgt auf den grünen, gepushten Stand `17c5c754`. Der Besitzer hat
selbstständig ausführbare Restarbeit und die Mod-Auswahl im Launch-Hub beauftragt.

## Einzelbelege

- Wand-Fix `b13ebe3a`: echte Platzierung von Bett, Tür und Doppelpflanze,
  freie/blockierte/geschützte zweite Zellen, kopierte Ausrichtung, Entity-Kollision
  und verweigerte Oktant-Füllung ohne Verbrauch. Gesamte WandMode-Familie auf
  Fabric/NeoForge: **54/54**, Run `2026-10-01T03-21-49Z-a73f`.
- Claims-Fix `c800630a`: Crafter vor Zutatenverbrauch, vollständige Container bei
  Kupfergolem-Transfers, Blitzentzündung und direkte/zufällige Kupferreinigung.
  Vollständige SimpleTweaks-Suiten auf Fabric/NeoForge: **100/100**, Run
  `2026-10-01T03-23-37Z-f1a5`. Positiv-, Negativ- und Abschaltfälle enthalten.
- Wiki `a51ac895`: 19 Python-Tests und Browserregression für Listen-/Rezeptfilter,
  Navigation, Neuladen, Mod-Trennung, leere Ergebnisse, feste Tabellenköpfe,
  390-Pixel-Viewport und nicht verfügbaren Storage bestanden.
- Launch-Hub: **45 Python-Tests**, einschließlich Forge-Client/Server/frischer
  Welt als Dry Run. Im Browser Abwahl, Speichern, Neuladen und Wiederherstellen
  aller **11/11** Projektmods geprüft. Lokaler Screenshot:
  `.ai-runs/launch-mod-selection.png`. JavaScript-Syntaxprüfung bestanden.
- Gradle-Korrektur `9ff57bd8`: echte Mod-Erkennung aller elf Projektmods auf
  Fabric, NeoForge und Forge. NeoForge/Forge erreichten mit allen Mods `Done`;
  alle drei Loader erreichten mit nur SimpleBuilding `Done`, ohne die zehn
  abgewählten Modul-IDs. Server sauber beendet, Auswahl bytegleich
  wiederhergestellt. Einzelheiten: `HUB-MOD-LOADING-VERIFICATION.md`.
- Balancing-Zentrale: **19 Tests, einer davon übersprungen**; bestehende
  Java-/JSON-Speicherfunktion, Vorschau, Konfliktprüfung und Rollback bestätigt.
  Der alte Todo-Punkt war überholt; kein automatischer Java-Live-Reload behauptet.
- Wiki-Generator `--all` und `--all --check` aktuell; Quests: **7 Kapitel,
  117 Quests**; Bücher: **0 Probleme**; Texturen: **470 und 9 .mcmeta aktuell**.

Die Worker-Belege wurden vor ihren Commits erzeugt; die Run-Datensätze weisen
deshalb ihren Ausgangscommit mit lokalen Änderungen aus. Sie ersetzen nicht das
gemeinsame Gate gegen einen sauberen, exakten Commit.

## Gemeinsame Abschlussprüfung

Der zusätzliche aktive Claims-Anbieter-Test prüft den echten FOOT eines HEAD
statt einer unbeteiligten dritten Zelle: **2/2** auf Fabric/NeoForge,
Run `2026-10-01T03-35-20Z-a9f9`, sauberer Commit `1ab309b1`.
Anschließend muss das vollständige Gate
mit Integration auf dem endgültigen Commit laufen. SHA und abschließendes Urteil
werden in `.ai-runs/followup-full-gate.log` festgehalten. Nur die dort als GREEN
geprüfte SHA darf nach `master` gepusht werden.

## Grenzen

Keine Besitzerwelt und keinen Besitzer-Client verändert. Client-/GUI-/Audio-
Abnahmen bleiben offen, solange der Besitzer-Client läuft. Claims bleiben AUS:
Blitzableiter-/Entity-Sekundärfolgen, vertrauenswürdige mobile Besitzer und weitere
Mod-/Storage-/Physikpfade sind noch nicht vollständig belegt. Forge bleibt opt-in.
