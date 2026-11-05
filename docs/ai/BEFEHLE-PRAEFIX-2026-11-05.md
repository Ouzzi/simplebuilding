# Befehle beginnen mit dem Mod-Namen (IG31-44, 05.11.2026)

Regel: Der Top-Level-Befehl ist die Mod-ID. Alte Namen bleiben als Alias (Brigadier-`redirect` auf denselben Knoten, gleiche
Berechtigung, gleicher Handler) mit einem gelben Hinweis im Chat, der den neuen Namen nennt. Hilfsfunktion:
`com.simplelib.api.CommandAliases` (`nested` = alter Befehl wird Unterbefehl, `contents` = alter Befehl wird die Mod-Wurzel,
`verify` = Prüfung für Gametests). Ein Alias nimmt nur einen freien Namen (ein fremdes `/claim` wird nie ersetzt).

| Alter Befehl | Mod-ID | Neuer Name | Stand |
|---|---|---|---|
| `/killboats`, `/killcarts` | simplebuilding | `/simplebuilding killboats`, `/simplebuilding killcarts` | umbenannt, Alias + Hinweis |
| `/resonance [debug]` | simpleresonance | `/simpleresonance [debug]` | umbenannt, Alias + Hinweis |
| `/music list\|pending\|info\|ban\|unban …` | simplemusic | `/simplemusic …` | umbenannt, Alias + Hinweis |
| `/servershop …` | simpleshops | `/simpleshops servershop …` | umbenannt, Alias + Hinweis |
| `/simpleopt status …` | simpleoptserver | `/simpleoptserver status …` | umbenannt, Alias + Hinweis |
| `/crawl` | simplemovement | `/simplemovement crawl` (Taste sendet den neuen Namen) | umbenannt, Alias + Hinweis; Wurzel jetzt offen, GM-Sperre je Unterbefehl |
| `/claim`, `/afk`, `/pregen`, `/backup`, `/maintenance`, `/freeze`, `/unfreeze`, `/phantoms`, `/lag`, `/invsee`, `/endersee`, `/pvp`, `/playtime`, `/seen`, `/coords`, `/inspect`, `/vanish`, `/vote`, `/leaderboard`, `/tweaks …` | simpletweaks | `/simpletweaks <name> …` | schon vorhanden (`TweakCommands.alias`, Zwilling als Redirect); alte Namen sind dort Primärknoten und zeigen keinen Hinweis (Umstellung = eigener Umbau, nicht nötig) |
| `/chunkloaders`, `/simplebuilding config\|tweaks` | simplebuilding | unverändert | trägt schon den Namen |
| `/simplemobheads`, `/simplemobmanners`, `/simplestats`, `/simpleloot`, `/simpletools`, `/simplefarming`, `/simplerespawn`, `/simplemodels`, `/simplefun`, `/simplequalityoflife`, `/simplemaps web`, `/<modid> config` | je Mod | unverändert | tragen schon den Namen |
| `/simplevisuals`, `/simplesounds guide`, `/simpleearth guide`, `/simplemaps picture` (Client) | je Mod | unverändert | tragen schon den Namen |
| `/sbtestcentre`, `/testcenter` | simplebuilding (Dev) | unverändert | **Ausnahme/Rückfrage:** Entwicklungswerkzeug für Befehlsblöcke der Testzentrale (Stufe-2-Rechte); die Wurzel `/simplebuilding` verlangt Operator-Spieler und würde Befehlsblöcke aussperren. Umbenennung nur mit Umbau der Wurzel-Sperre |
| `/simplefun pvp headDrops` (Alias in simplemobheads) | simplemobheads | `/simplemobheads pvp headDrops` | bestehender Alias aus 1.3, bleibt |

Gametests: `CommandAliasTests` (simplelib, je Modul-Adapter `commandAliases` in simplemovement, simpleresonance, simplemusic,
simpleoptserver, simpleshops) und `tweaks_game_test_core_commands_live_under_simplebuilding_and_old_names_lead_there` (Kern).
Nur MC 26.3 Fabric; NeoForge/Forge/ältere Linien nicht angefasst.
