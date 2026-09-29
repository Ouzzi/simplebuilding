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

## NICHT fertig (WIP-Branches, nichts davon ist in master)
Alle drei Branches liegen im Repo als WIP-Commit (Agent wurde wegen Limit gestoppt, Tests waren teils rot/unvollständig):
| Paket | Branch | Inhalt | Stand |
|---|---|---|---|
| HH | `worktree-agent-ad588a015b1bb6192` | Pad-Tier-I mit Materialkern, Mobköpfe (Vanilla-Texturen, nur Charged Creeper, geheime Fähigkeiten, Silberfisch schrumpft), Chunkloader-I/Launchpad-I akzeptieren Trial-Chamber-Kopf, Flypad-I braucht Shulkerkopf + Enderit-Kern/-Platte + Elytra mit Mending | mitten in der Arbeit (`HeadAbilities`) |
| II | `worktree-agent-a1fbc00162b5ec7fc` | gestufte Shulkerkisten (wie die Truhen, etwas teurer, 10 Hammerschläge + Material) | Tests zu reparieren |
| JJ | `worktree-agent-a985977351248fc71` | Linse → mystischer „…rod“-Name, schnellerer Verschleiß, Amboss-Reparatur ohne Level, Neige-Animation + Partikel, Laserpunkt mittig, Fadenkreuz aus; Attractor (Filter nur mit Constructor's Touch, Range-Verzauberung); Velocity Gauge → „Gauge“ + Zusatzfunktion + Verzauberung + animierte Textur; einheitliche Overlays | Registrierungen/Config-Tests offen |
| Pulsating | `work` (Commit 29415666, Worktree `agent-a253429a9fb439791`) | Warden-Motiv kleiner und um 45° gedreht (Raute) | **wartet auf Okay des Besitzers**: Augen/Mund wirken blass |

Vorgehen pro Branch: `git merge --no-ff <branch>` in master, Konflikte lösen (siehe AGENTS.md §5), Tests des Pakets, dann Gate.
HH: Merge-Punkt ist `TweaksItems.extraMobHeads()` (gibt bisher leere Liste; GG legt darunter die Reihe „mob_heads“ an).

## Nächste Schritte in Reihenfolge
1. HH, II, JJ fertigstellen und mergen (je Paket gefilterte Server-Tests auf `fabric-263,neoforge-263`).
2. Pulsating-Textur mit dem Besitzer klären (Kontrast von Augen/Mund erhöhen?).
3. **Bücher-Faktenpass**: Namen/Rezepte nach den Merges prüfen. Bekannt: deutscher Titel „Radius: Detektor“ und „detector“ im
   Admin-Kapitel „Feature Switches“ sind fest verdrahtet; `docs/` sagt an einigen Stellen noch „Ore Detector“.
4. Volles Gate im Worktree (`check`, Server-Tests 26.3), lesen, erst bei grün pushen.
5. Client-Gate und Testzentrale neu bauen (nur wenn keine Besitzer-Clients laufen).
6. **Port-Run** auf 26.2 Fabric/NeoForge/Forge, 1.21.11, 26.4. Mitzunehmen: neue Bücher/Texturen/Texte, drei Mixins des Admin-Buchs
   (`OperatorBook*Mixin`), Nugget-Neigung im 26.2-`HeldItemRenderer`, `MAIN_TREE_ONLY`/`POTION_PAD_MAIN_ONLY`/`MAIN_LINE_ONLY` in
   `tools/textures` leeren und neu generieren, REI-Ausblendung des Admin-Buchs, Forge-Paket für `PlacedBundleScrollPayload`.
7. Später (Queue): Vorlagen teurer machen, Punkte 64–69, Wiki-UX-Ideen aus der Zentrale, Baustab über den Planer, Kerne als Module.

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
