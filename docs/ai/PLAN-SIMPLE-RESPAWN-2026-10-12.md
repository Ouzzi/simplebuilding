# Plan Simple Respawn (2026-10-12, Branch `claude-respawn`)

Grundlage: Besitzer-Vorgabe 12.10. (`.claude/QUEUE.md`), Konzept `KONZEPT-FARBPINSEL-RESPAWN-2026-10-09.md` Teil B,
Besitzer-Antworten 09.10. (Einzelspieler = normaler Tod, Mobs ignorieren Niedergeschlagene, keine Item-Wiederbelebung).

## Ist-Zustand
- Kein Respawn-/Revive-Code im Repo, kein Soulbound/Death-Vault (simpletools hat keins) → Kompatibilität heißt:
  echter Tod läuft immer über Vanilla `die()` (Drops, Todesnachricht, NeoForge/Forge `LivingDeathEvent`).
- Vorlagen: `simpleenddragonfight` (Sub-Mod-Bündelung, JSON-Config, Forge-Testregistrierung),
  `simplemovement` (S2C-Payloads auf 3 Loadern, Kriech-Pose per `updatePlayerPose`-Mixin, Befehl als C2S-Weg),
  `tools/newmod.py` (Gerüst), `tools/guides/module_guides.py` (Handbuch), Questionary-JSON je Item.

## Entscheidungen (selbst getroffen, vanillanah/fair/konfigurierbar)
| Frage | Entscheidung |
|---|---|
| Wann niedergeschlagen | Tödlicher Schaden, kein Totem (Totem wirkt zuerst), Quelle nicht `bypasses_invulnerability` (Void, /kill), Überleben/Abenteuer, und mind. ein anderer Nicht-Zuschauer online (`onlyWithOtherPlayers`, aus = immer). |
| Ausbluten | `bleedOutSeconds` 60; Schaden im Zustand kostet `secondsLostPerDamage` 1 s je Schadenspunkt; Gesundheit bleibt bei 1. |
| Wiederbeleben | Mitspieler hält Rechtsklick auf den Körper `reviveSeconds` 5 s (Abbruch: loslassen > 10 Ticks, Abstand > `reviveRange` 3, Schaden am Helfer). Danach `reviveHealth` 6 (3 Herzen), `reviveFood` 0, Sättigung 0, 3 s Resistenz V (`reviveResistanceSeconds`), Schwäche `reviveWeaknessSeconds` 0. Fortschritt bei beiden als Balken. Fortschritt „Erste Hilfe“. |
| Niedergeschlagen | Liegt (Schwimm-Pose), kriecht mit `crawlSpeed` 0,5 × Schleichtempo, kein Springen; kein Angriff/Abbau/Benutzen/Interagieren/Wegwerfen/Hand-Tausch (Client blockiert, Server erzwingt). Mobs ignorieren (`mobsIgnoreDowned`). Chat-Meldung an alle (`announce`). |
| Bildschirm | Eigener „Niedergeschlagen“-Bildschirm im Stil des Vanilla-Todesbildschirms (roter Verlauf, Titel 2×, Ursache, Restzeit, Hilfe-Balken), Knöpfe **Zuschauen**, **Inventar**, **Aufgeben** (Knöpfe erst nach 1 s aktiv wie Vanilla). |
| Zuschauen | Bildschirm zu, Kamera kreist frei 360° um den eigenen Körper (Maus), WASD kriecht in Blickrichtung; Inventar-Taste (Standard E) öffnet den Bildschirm wieder (Hinweis im HUD). |
| Inventar | Vanilla-Inventarbildschirm, nur Ansicht: Klicks/Ziehen/Tasten außer Esc/E werden verschluckt, kein Paket an den Server; E/Esc zurück. |
| Aufgeben | Befehl `/simplerespawn giveup` → Vanilla-Tod mit der ursprünglichen Todesursache. |
| Exploits | Logout/Kick/Server-Stop (Mixin `PlayerList.remove`, vor dem Speichern) = Tod → Drops liegen in der Welt, Datei leer (kein Dupe). Absturz: Spieler-Tag `simplerespawn_downed` wird gespeichert → beim nächsten Einloggen Tod. Gamemode-Wechsel zu Kreativ/Zuschauer hebt den Zustand auf. Server prüft alles selbst (manipulierter Client kann nichts). |
| Einzelspieler | Standard normaler Tod (niemand kann helfen); Config `onlyWithOtherPlayers=false` erlaubt es immer. |
| Befehle | `/simplerespawn giveup` (jeder), `/simplerespawn down <Spieler>` und `/simplerespawn revive <Spieler>` (OP 2, Tests/Testzentrale). |

## Umsetzung (Dateien)
- `modules/simplerespawn/` aus `tools/newmod.py`; `shared/java/.../simplerespawn/`:
  `RespawnConfig` (JSON `config/simplerespawn.json`, Grenzen), `Downed` (Server-Zustand + Logik), `RespawnServer`
  (Tick, Wiederbeleben, Tod, Sync), `RespawnCommands`, `RespawnState` (seitengetrennte Abfrage), Payloads
  `DownedListPayload` (alle Spieler) + `RespawnStatePayload` (Betroffene), `RespawnTests`, `client/` (Bildschirm,
  Nur-Ansicht-Inventar, Kamera, HUD, Tick), Mixins (Server: Tod abfangen, Pose, Aktionen, Ziele, Logout; Client:
  Tasten, Maus, Kamera, HUD, Tick).
- Loader-Einträge Fabric/NeoForge/Forge (Config, Payloads, Server-Tick, Befehle, Client-Empfang, Forge-Tests).
- SB-Bündelung: `enableSimpleRespawn` (Config, Lang EN/DE beide Lang-Orte, ConfigOptionTests), Jar-in-Jar je Loader,
  `fabric.mod.json`/`mods.toml` optional, `modules.json` (`parent`/`bundle`, Forge-Tests), Simple-Money-Preisdatei (leer).
- Handbuch über `tools/guides/module_guides.py` (Rezept Buch + Glitzernde Melonenscheibe), Wiki `manual.json`,
  Questionary-Texte, `docs/MODS-UND-FEATURES.md`, Testzentrale-Station `respawn` (nur wenn Modul geladen).

## Risiken
- Mixin-Ziele müssen auf allen drei Loadern gleich heißen (Vanilla-Namen 26.3, mit `javap` geprüft).
- Kamera-Mixin (`Camera.alignWithEntity` TAIL) könnte mit anderen Kamera-Mods kollidieren → nur aktiv im Zuschauen-Modus.
- Wiederbeleben braucht das Halten-Wiederholen des Clients (alle 4 Ticks ein Interaktionspaket) → Lücke 10 Ticks.

## Verifikation
- Compile 3 Loader; Gametests (Fabric/NeoForge/Forge mit SB, Standalone Fabric/NeoForge): registriert, Config-Grenzen,
  Niederschlag statt Tod, Totem zuerst, Void/kill normal, Einzelspieler normaler Tod, Frist → Tod, Schaden kürzt Frist,
  Aufgeben → Tod, Wiederbeleben → 6 HP/0 Hunger, Abbruch bei Abstand, Logout → Tod (Drops, kein Dupe), Tag nach
  Neustart → Tod, Aktionen blockiert, Mobs ignorieren, Gamemode-Wechsel.
- Client-Smoke mit Screenshots (Bildschirm, Zuschauen, Inventar) → `~/previews/respawn/`.
- `check_data.py`, `wiki/generate.py --all --check`, `module_guides.py --check`, Questionary-Check, SB-ConfigOptionTests.
