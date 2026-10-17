# Plan: Simple Tweaks Teil 2 – Spieler/Welt/Events (17.10.2026, Sitzung A, Branch `claude-tweaks-play`)

Auftrag: Queue-Punkt „Simple Tweaks Teil 2 Spieler/Welt/Events“ (Besitzer-Auswahl 17.10.): T4–T9, T19–T28, T30–T33.
Parallel-Branch `claude-tweaks-admin` (Teil 1) erweitert dasselbe Modul – deshalb alles Neue unter eigenem Paket,
eigener Config-Datei, eigener Mixin-Config, eigener Test-Liste; Loader-Einstiege bekommen je **eine** Zeile.

**Besitzer-Nachtrag 17.10.:** Was nur auf Befehl wirkt, ist Standard **an**. Deshalb: T33 /pregen an; T21 per Befehl
gestartete Events an (der automatische Zeitplan bleibt aus); T22 Befehl /leaderboard an (die ständige Seitenleiste bleibt
aus); T30 /phantoms an. Passive/automatische Teile bleiben aus (T8, T19, T20, T26, T31, T32, T21-Zeitplan,
T22-Seitenleiste), die Tag/Nacht-Abstimmung bleibt aus. Faustregel für alles Weitere: nur Befehl = an, automatische
Verhaltensänderung = wie vorgegeben.

## Ist-Zustand (Survey)
- `modules/simpletweaks`: Claims (Server-Autorität, Shared-Mixins, Tests als `Map<String,Consumer<GameTestHelper>>`),
  Admin-Teil auf `claude-tweaks-admin` mit Muster `admin/Admin.java` (Hub), `AdminConfig` (Record je Feature, Gson-Merge
  auf Standardwerte, geklemmt), `AdminPerms` (Vanilla-OP-Stufen), `AdminState` (Welt-JSON), `AdminFabric/AdminNeo/AdminForge`.
  Teil 2 übernimmt dieses Muster 1:1 (eigene Kopien `Play*`, kein Import aus `admin`).
- `/simpletweaks` darf nicht registriert werden (Kompatibilitätstest); `/tweaks` ohne Wurzel-`requires` (Brigadier führt
  zusammen), Rechte je Unterbefehl.
- Vanilla 26.3 geprüft (Quellen `minecraft-patched-26.3` von sb-test):
  - **Weltgrenze je Dimension gibt es schon**: `ServerLevel#getWorldBorder` liegt in den Level-Daten jeder Dimension,
    `/execute in <dim> run worldborder …` wirkt je Dimension. → T25 baut **keine** eigene Grenze, sondern ist nur eine
    Config-Ebene (Größe/Mitte je Dimension beim Start auf die Vanilla-Grenze anwenden) + Übersicht `/tweaks border`.
    Ohne Einträge tut T25 nichts (Rückport-Prinzip A5: Vanilla-Fassung maßgeblich).
  - Mob-Griefing: nur global `mob_griefing`; je Mob gibt es nichts → T8 darf je Mob **nur verbieten**, nie über die
    Vanilla-Regel hinaus erlauben (aus = Vanilla). Bei jeder Version neu prüfen (Eintrag in der Rückport-Liste).
  - Phantome: nur global `spawn_phantoms` → T30 je Spieler (Vanilla-Regel aus = keine Phantome, wie gehabt).
  - Keep-Inventory: nur global `keep_inventory` → T27 greift nur, wenn die Vanilla-Regel aus ist.
  - PvP: global `pvp` (+ Server-Property) → T9 schränkt nur weiter ein.
  - AFK-Kick: Vanilla hat `player-idle-timeout` (Property/`/setidletimeout`) → unser Kick bleibt Standard aus.
  - Drachenei: Vanilla setzt es nur beim ersten Kampf (`EnderDragonFight#setDragonKilled`, `hasPreviouslyKilledDragon`).
- Überschneidungen mit anderen Modulen (nur öffentliche IDs/Config-Dateien, keine Klassenimporte):
  - **Simple Respawn**: Niedergeschlagen ist kein Tod (`ServerPlayer.die` wird erst beim echten Tod gerufen) → T5-Hinweis
    kommt genau beim echten Tod (Aufgeben/Ablauf), nie beim Niederschlagen; T27 ebenso. Keine Doppelung.
  - **Simple Stats**: zählt je Gegenstand; T7/T22 lesen die Vanilla-Spielerstatistik (Spielzeit, Tode, Kills, Abbau) – keine Doppelung.
  - **Simple Weather**: `disableWeather` → die Wetter-Abstimmung (T23) wird nicht angeboten, wenn `config/simpleweather.json`
    das Wetter abschaltet und das Modul geladen ist.
  - **Simple Money**: T20 gibt zusätzlich `simplemoney:money_bill` (Registry-ID, nur wenn geladen).
  - **Simple End Dragon Fight**: ändert Leben/Tempo/Mites, nicht das Ei → T28 ergänzt nur das Ei; keine Doppelung (das
    Ei kommt aus `setDragonKilled`, das auch dort gerufen wird).
  - **Simple Mob Manners**: kein Griefing-Bezug.
  - **Claims**: Schaden in Claims prüft `Claims.damage` zuerst (Mixin auf `hurtServer`); T9 kann eine Claim-Sperre nie
    aufheben, nur zusätzlich sperren („Claims haben Vorrang“).

## Gemeinsames
- Paket `com.simplebuilding.modules.simpletweaks.play` (+ Unterpaket je Feature), Mixins `play.mixin`,
  `simpletweaks.play.mixins.json`.
- Config `config/simpletweaks-play.json`, ein Abschnitt je Feature mit `enabled` (+ `permission` wo nötig). Fehlt die
  Datei, wird sie geschrieben; Werte geklemmt (AGENTS 3.8); kaputte Datei bleibt, Standardwerte gelten (Log).
- Weltzustand `<world>/simpletweaks/play-state.json` (PvP, Phantome aus, zuletzt gesehen, Login-Serien, Pregen-Fortschritt).
- Lang `simpletweaks.play.*` (EN/DE); `check_data.py` lässt das Präfix zu.
- Feature aus = keine Befehle, Mixins tun nichts (ein Boolean-Check). Super-Mod-Schalter `SubMods.enabled("simpletweaks")` aus = alles aus.
- Loader: `PlayFabric`, `PlayNeo`, `PlayForge` (Lifecycle, Befehle, Tests); Ticks, Beitritt, Austritt, Chat über Shared-Mixins.

## Features
| T | Standard | Design | Config | Tests |
|---|---|---|---|---|
| T4 Koordinaten teilen | an | `/coords [Nachricht]` an alle, `/coords to <Spieler>` privat: Zeile mit Name, x y z, Dimension; Klick kopiert, Hover erklärt. Optionaler Ping: Partikelsäule (End-Stab) `pingSeconds` lang. Abklingzeit gegen Spam. | `coords{enabled,ping,pingSeconds,cooldownSeconds}` | Nachricht enthält Koordinaten + Klick-Ereignis, Ping läuft und endet, Abklingzeit |
| T5 Todes-Hinweis | an | Nach `ServerPlayer.die` (RETURN, wenn wirklich tot) privat: Ort, Dimension, Ursache, Item-Verfall (5 min, nur solange geladen) bzw. „nichts fallen gelassen“ bei Keep-Inventory; Klick kopiert. Simple Respawn: erst beim echten Tod. | `deathHint{enabled}` | Tod → Nachricht mit Ort/Ursache; aus → keine |
| T6 Erwähnungen | an | `@Name` in Chat: `PlayerList.broadcastChatMessage` bekommt markierten Inhalt (unsignierter Zusatzinhalt wie Vanilla-Dekoration), Erwähnte hören einen leisen Ton (Glocke). `@everyone`/`@all` nur ab OP-Stufe. | `mentions{enabled,sound,volume,everyonePermission}` | Markierung + Liste der Erwähnten, Groß/Klein egal, kein Selbstton |
| T7 /playtime, /seen | an | `/playtime [Spieler]` aus Vanilla-Statistik `play_time` (offline aus `stats/<uuid>.json`), `/seen <Spieler>`: online / zuletzt vor X (eigene Austrittszeit im Weltzustand). | `playtime{enabled}` | Formatierung, Austritt speichert Zeit, offline lesen |
| T8 Mob-Griefing je Mob | aus | Liste `blocked` (Wesen-IDs): während ein Wesen tickt (ThreadLocal in `ServerLevel.tickNonPassenger`) und bei Explosionen mit Verursacher liefert `GameRules.get(MOB_GRIEFING)` für gelistete Mobs `false`. Nie mehr als Vanilla erlaubt. | `mobGriefing{enabled,blocked[]}` | Creeper gelistet → Regel false, Schaf nicht; aus = Vanilla |
| T9 PvP je Spieler | an | `/pvp on|off`, ohne Argument Status. Schaden Spieler→Spieler nur, wenn beide an (`ServerPlayer.canHarmPlayer`, auch Pfeile/Dreizack). Nach Umschalten `cooldownSeconds` Sperre; Claims prüfen vorher und behalten Vorrang. Start-Zustand `defaultOn` (aus). | `pvp{enabled,defaultOn,cooldownSeconds}` | beide an → Schaden, einer aus → keiner, Abklingzeit, Persistenz |
| T19 Willkommen/Ankündigungen | aus | Willkommenstext beim Beitritt (`{player}`), rotierende Tipps alle `tipMinutes`, eigene Beitritts-/Austrittszeilen statt Vanilla (leer = Vanilla). | `welcome{enabled,message,tips[],tipMinutes,joinMessage,leaveMessage}` | Platzhalter, Tipp-Rotation, eigene Zeilen |
| T20 Login-Belohnung | aus | Erster Beitritt je Kalendertag (Serverzeit): Serie +1 (Lücke = 1), Items aus Liste, je Tag `billsPerDay` Geldscheine bis `maxBills` (nur mit Simple Money), alle 7 Tage Bonus. Volles Inventar → Rest fällt vor die Füße. | `loginReward{enabled,items[],billsPerDay,maxBills,bonusEvery,bonusItems[]}` | Serie zählt/reißt, nur einmal je Tag, Scheine nur mit Money |
| T21 Server-Events | Befehl an, Zeitplan aus | `/tweaks event start <double_xp|more_loot|blood_moon> [Minuten]`, `stop`, `status` (OP 2). Doppel-XP: `ExperienceOrb.award` ×`xpMultiplier`; Mehr Beute: Mob-Loottabelle bei Spieler-Kill zusätzlich `extraLootRolls` mal; Blutmond: nur nachts, alle `bloodMoonWaveSeconds` je Spieler in der Oberwelt bis `bloodMoonMobs` Monster, endet bei Tagesanbruch. Zeitplan (aus): `"SAT 20:00 double_xp 60"`. Ansage + Bossleiste. | `events{enabled,permission,scheduleEnabled,schedule[],xpMultiplier,extraLootRolls,bloodMoonWaveSeconds,bloodMoonMobs}` | Start/Stopp/Ablauf, XP-Faktor, Zusatzbeute, Zeitplan-Parser, Blutmond nur nachts |
| T22 Bestenliste | Befehl an, Seitenleiste aus | `/leaderboard <playtime|deaths|kills|mined>` (Top 10, alle bekannten Spieler, offline aus Statistikdateien). Seitenleiste: eigenes Ziel `simpletweaks_top`, alle `refreshSeconds` neu. | `leaderboard{enabled,sidebar,sidebarStat,refreshSeconds}` | Sortierung, Statistik-Datei lesen, Seitenleiste nur wenn an |
| T23 Abstimmungen | an (Tag/Nacht aus) | `/vote weather clear`, `/vote event <name>` (wenn T21 an), `/vote day|night` (nur mit `dayNight`), `/vote yes|no`; klickbare [Ja]/[Nein], `seconds` Laufzeit, angenommen ab `percent` % Ja der aktiven (nicht AFK) Spieler, Abklingzeit je Thema. | `vote{enabled,seconds,percent,cooldownSeconds,weather,events,dayNight}` | Mehrheit/Ablehnung, eine Abstimmung zur Zeit, Tag/Nacht aus = kein Befehl, Simple-Weather-Sperre |
| T24 AFK | an | Nach `minutes` ohne Aktion (`getLastActionTime`) AFK: grauer Name in der Tab-Liste (`getTabListDisplayName`), zählt nicht für Schlafen (`SleepStatus.update`), `/afk` von Hand; Kick optional (aus). | `afk{enabled,minutes,excludeFromSleep,kick,kickMinutes}` | AFK nach Zeit, Rückkehr, Schlafzählung, Kick aus |
| T25 Weltgrenze je Dimension | an (ohne Einträge wirkungslos) | Vanilla-Grenze je Dimension (s. o.); Config `dimensions{"minecraft:the_nether":{size,centerX,centerZ}}` beim Start angewandt, `/tweaks border` listet alle Dimensionen. | `border{enabled,dimensions{}}` | Eintrag setzt Vanilla-Grenze nur dieser Dimension |
| T26 Nether-Dach | aus | Spieler über Y 127 im Nether (Survival/Adventure, ohne OP-Bypass) wird an die letzte Stelle darunter zurückgesetzt (sonst sichere Stelle unter dem Dach). | `netherRoof{enabled,opBypass,bypassPermission}` | auf dem Dach → zurück; aus/Kreativ → bleibt |
| T27 Teil-Keep-Inventory | an | Nur wenn Vanilla `keep_inventory` aus: Modus `hotbar_armor` (Hotbar+Rüstung+Nebenhand bleiben), `xp` (Erfahrung bleibt), `custom` (Schalter je Teil + XP-Prozent). Vor `Inventory.dropAll` herausnehmen, bei `restoreFrom` zurück. Fluch des Verschwindens wirkt weiter. | `keepInventory{enabled,mode,keepHotbar,keepArmor,keepOffhand,keepMain,keepXpPercent}` | Modi behalten/werfen richtig, Vanilla-Regel an = unberührt |
| T28 Drachenei | an | Nach `setDragonKilled` bei Wiederholungskampf: Ei auf das Podest (belegt → als Item). | `dragonEgg{enabled}` | zweiter Kill setzt Ei, erster Kill bleibt Vanilla |
| T30 Phantome je Spieler | an (Befehl) | `/phantoms on|off`: abgemeldete Spieler gelten im `PhantomSpawner` als Zuschauer (kein Spawn). | `phantoms{enabled}` | aus → übersprungen, Persistenz |
| T31 Entity-Grenze | aus | Alle `scanSeconds` Mob-Zählung je Chunk; über `perChunk`: Meldung an Claim-Besitzer bzw. nahe Spieler + OPs (einmal je Chunk und Stunde), neue Mobs dort (Zucht, Spawner, natürlich) werden verweigert, nichts wird still gelöscht. | `entityLimit{enabled,perChunk,scanSeconds,blockSpawns}` | Überschreitung meldet + verhindert neue Mobs, unter Grenze frei |
| T32 Bodenitems aufräumen | aus | Alle `intervalMinutes` Ansage mit Countdown (`warnSeconds`), dann Items älter `minAgeSeconds` weg; benannte, seltene (ab `exemptRarity`) und gelistete bleiben. `/tweaks cleanup now` (OP 2). | `cleanup{enabled,intervalMinutes,warnSeconds[],minAgeSeconds,exemptRarity,exempt[],permission}` | nur alte/gewöhnliche weg, Ausnahmen bleiben |
| T33 Vorgenerierung | an (Befehl) | `/pregen start <Radius> [Dimension]`, `pause`, `resume`, `stop`, `status` (OP 3): Spirale um die Weltmitte, asynchron über `getChunkFuture` mit höchstens `parallel` offenen Aufträgen und `perTick` neuen je Tick, Fortschritt in Bossleiste/Chat, Stand im Weltzustand (fortsetzbar nach Neustart). | `pregen{enabled,permission,parallel,perTick,maxRadius}` | Spiralzählung, kleiner Lauf erzeugt Chunks, Pause/Fortsetzen |

## Doku, Prüfungen
- `modules/simpletweaks/wiki/manual.json`: je Feature ein Eintrag (EN/DE, Quellen); `docs/MODS-UND-FEATURES.md`;
  Rückport-Liste (`PRINZIPIEN-VANILLA-RUECKPORTS.md`) bekommt T8/T25-Vermerk.
- Regenerieren nur auf sb-test: discover.py, module_guides.py, simplemoney generate_links.py, `wiki/generate.py --all`;
  danach `checkModuleGuides checkModuleData checkWiki checkDiscover checkMultimod`.
- Kompilieren: 26.3 Fabric/NeoForge/Forge (Modul) + 26.2 `:compileJava`; GameTests `module-simpletweaks-{fabric,neoforge}-263`
  + Standalone.

## Risiken
- Mixins auf von NeoForge/Forge gepatchte Methoden (`dropEquipment`, `getTabListDisplayName`, `PhantomSpawner`) → Tests
  auf beiden Loadern; nur Aufrufe, die in allen Fassungen vorkommen (`isSpectator`, `GameRules.get`).
- Chat-Markierung nutzt den unsignierten Zusatzinhalt (wie Vanilla-Dekoration); Clients mit „nur signierte“ Anzeige zeigen das Original.
- Vorgenerierung großer Radien kostet Platz/Zeit (`maxRadius`, Pause).
