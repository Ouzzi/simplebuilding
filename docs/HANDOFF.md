# Übergabe (Stand 2026-09-29) – weiterarbeiten mit jedem Assistenten

Zuerst `AGENTS.md` lesen (alle Regeln). Diese Datei sagt, was fertig ist und was als Nächstes kommt.
Der Besitzer schreibt Deutsch, will kurze Antworten, Fragen als Liste, und möchte **erst 26.3 fertig**, dann Port-Run.

## Erledigt und auf master (Welle 23 + 24 Teile)
Welle 23 komplett (Config serverseitig, Modpack-Hooks, Erfolge, Beute/Handel, Items, Zusammenspiel, Immersion, Optik, Balancing-Zentrale).
Welle 24, gemergt in master (lokal/gepusht siehe Git-Log):
- **BB** Attractor-Arme, Ore Detector → **Detector** (ID `detector`, Alias `ore_detector`), Detector platzierbar, neue Rezepte
  (Gauge, Oktant, Blaupause mit Leuchttinte), Enderit-Namen, klebriger verstärkter Kolben → Netherit, Diamantblock ab Eisenhammer.
- **CC** Handbücher komplett neu (11 Mod-Bücher, 9 Vanilla-Bücher, Enchantments-Buch, Admin-Buch nur für OPs, pausiert nicht),
  Resonanz-Anzeige (Wert + Tooltip, Maximalwerte, Cap).
- **DD** platzierte Bündel (Sneak+Scroll, Rechtsklick raus, Sneak+Rechtsklick rein, Item zum Spieler gedreht),
  platzierter Oktant (Umriss pro Spieler). Neu gebaut: nur ein **gesperrter** Oktant wird mit Sneak+Rechtsklick abgestellt
  (`PlacedTemplates.isPlaceableOctant`) – Besitzer hat die Geste noch nicht bestätigt.
- **EE** Aktiv-Texturen und Leerlauf-Partikel für alle Pads, Spawn-Teleporter (eigener Spawn / Redstone → Weltspawn, Sounds),
  Hammer ohne GUI-Text (Nugget neigt sich – nur 26.3), Echo Sounder (Klick einmal, Sperre 1–5 s, Config `echoSounderAttemptLockTicks`).
- **FF** Kerne verwandeln selten Wirtsblöcke in Erz, neue Kern-Animation, Warden-Gesicht gerade.
- **GG** Tabs: SimplePads-Tab, Bauplanung in SimpleTools, Zeilen-Layout überall, Mod-Items im Suchtab neben Vanilla-Vorbildern.
- Client-Tests repariert (732/732 auf allen sechs Zielen, Stand vor den letzten Merges).

## HH / II / JJ und Pulsating integriert (2026-09-29, Codex)
- HH gemergt: Materialkerne/Pad-Rezepte, zusätzliche Mobköpfe und Fähigkeiten; Creative-Tab-Layout aus GG erhalten.
  Cave-Spider-Test trennt Bodenschaden von Luftangriffen. Gefiltert **30/30 grün** (Run `2026-09-29T19-54-57Z-8b89`).
- II gemergt: gestufte Shulkerkisten, Tab-Zeile und Export-Katalog ergänzt. Gefiltert **16/16 grün**
  (Run `2026-09-29T19-56-30Z-3ece`).
- JJ gemergt: Amethyst Resonance Rod, Attractor/Range/Touch und Gauge. Veraltete 4-Block-Erwartung auf 3 korrigiert;
  Filter-Test beendet Schleichen, bevor Ansaugen geprüft wird. Gefiltert **22/22 grün**
  (Run `2026-09-29T20-00-47Z-fc9d`).
- Besitzerwahl Pulsating: **Raute mit stärkerem Augen-/Mundkontrast** umgesetzt und 16-fach gezeigt.
  `work` integriert, 1.21.11-Textur noch unverändert. `MAIN_TREE_ONLY` und `MAIN_TREE_PREFIXES` bis zum Port behalten.
- Bücher-Faktenpass: Namen, Pad-Rezepte, Spawn-Ziele, Attractor-Reichweite/Filter, Rod-Reparatur, Gauge und
  Shulkerkisten aktualisiert; passende Rezeptkarten. Bücherprüfung **0 Probleme**, keine doppelten Lang-Schlüssel.
- Wiki-Fakten für Gadgets und Pads nach Code korrigiert; Quest-Hinweise der Hauptlinie korrigiert.
- Texturprüfung **470 Texturen + 9 mcmeta aktuell**. Shulker-/Gauge-Vorschau ebenfalls gezeigt.
- Der erste `check` stoppte an `checkQuests` (Reihenfolge der generierten Lang-Schlüssel); Generator erneut ausgeführt.
  Vollständiges Server-Gate und abschließender Build-Check stehen noch aus; bisher **kein Push** dieser Merges.

## Nächste Schritte in Reihenfolge
1. Laufendes vollständiges Server-Gate auf `fabric-263,neoforge-263` auswerten, Fehler beheben.
   Alte Flypad-/Chunkloader-Rezepterwartungen im Tweaks-Test sind bereits korrigiert, im Gesamt-Gate gegenprüfen.
2. Generiertes Wiki übernehmen; `check` im Worktree `C:/Users/oussa/AppData/Local/Temp/sbgate` grün lesen.
3. Client-Gate für beide 26.3-Ziele seriell, erst wenn der Besitzer-Client geschlossen ist. Besitzer wurde gefragt.
   Testzentrale neu bauen und vollständige Item-Abdeckung prüfen. Keine Besitzerwelt ungefragt ersetzen.
4. Nur nach gelesenem grünen Gate (Runner: **alles gruen**) pushen.
5. Nach Abnahme von 26.3 durch den Besitzer: eigener **Port-Run** auf 26.2 Fabric/NeoForge/Forge, 1.21.11 und 26.4.
   Mitzunehmen: HH/II/JJ, neue Bücher/Texturen/Texte, Pulsating-Kontrast, Quest-Fakten, drei Mixins des Admin-Buchs
   (`OperatorBook*Mixin`), Nugget-Neigung im 26.2-`HeldItemRenderer`, Textur-Scope-Schalter in `tools/textures`,
   REI-Ausblendung des Admin-Buchs, Forge-Paket für `PlacedBundleScrollPayload`.
6. Später (Queue): Vorlagen teurer machen, Punkte 64–69, Wiki-UX-Ideen aus der Zentrale, Baustab über den Planer, Kerne als Module.

Die unversionierten Detector-Texturen unter `mc1_21_11/fabric/.../textures/item` lagen schon vor diesem Run
im Haupt-Repo und wurden nicht angefasst. Redundante Wiki-Sicherungen liegen als beschriftete Stashes vor.

## Fakten, die man leicht vergisst
- Mason verkauft keine Kerne; fahrender Händler: Kupfer/Eisen/Gold/Diamant-Kern selten und teuer.
- Chunk-Loader nur bei Online-Besitzer; Admin-Befehl zum Auflisten.
- Luftsprung 20 s / 10 s, Balken am XP-Balken (Priorität XP-Änderung > Luftsprung > Locator), Server erzwingt (≤ 1 s Lag).
- Linear baut eine Linie, Bridge von einem Ende mit doppelter Geschwindigkeit.
- Glowing hat eine Stufe (volle Helligkeit, Name „Glowing“); Pulsating allein pulsiert Sättigung, Pulsating+Glowing die Helligkeit 1–15.
- Hammer: 1×1 = 1,2× gleiche Spitzhacke, Fläche wie eine Stufe darunter, Haltbarkeit unverändert bis der Besitzer testet.
- Kerne nicht stapelbar; Enderman-/Lohenkopf nutzen die echten Vanilla-Texturen.
- Placed-Bundle/Oktant/Detector nutzen den Block `placed_smithing_template`.
- Zuletzt gepushter grüner Stand: siehe `git log origin/master`; danach nur, was im Commit „Handoff“ steht.
