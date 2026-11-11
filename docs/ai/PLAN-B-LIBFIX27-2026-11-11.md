# PLAN-B-LIBFIX27 (11.11.2026) – SimpleLib-Hygiene: REV2-B-11 Rest + REV2-A-12 Rest (prio27 #6)

Branch `b-libfix27`, Basis `hub/b-bundle179-gen` 089fdc551. Nur 26.3 Fabric.

## Ist-Stand
- REV2-B-11 (a-hyg30): `SafeCodecs.listLenient` + `checkCodecLists` fertig; offen `ClientState.onDisconnect` und Check gegen `Files.writeString` auf Config-JSON.
- REV2-A-12 (a-qw29): CarriedStacks-Log + OwnedLights-Index fertig; offen WarmStackingMixin-Prüfung.
- `ClientState` gab es nicht (grep 0). SimpleLib hatte einen eigenen DISCONNECT-Hook nur für Free Look.

## Umsetzung
1. **`com.simplelib.api.ClientState`** (shared, ohne Client-Klassen): `onDisconnect(id, Runnable)`, `remove(id)`, `disconnected()`; Zwilling von `PlayerState`, werfende Listener werden geloggt und übersprungen. `SimpleLibFabricClient`: DISCONNECT ruft nur noch `ClientState.disconnected()`.
   Vorhandene SimpleLib-Client-Caches eingehängt: Free Look (wie bisher), **Modpack-Guide-Freischaltungen** (`ModpackGuideScreens.done` war die Antwort des alten Servers und blieb beim Serverwechsel stehen → `forgetServer()`), **Portal-Puls** (`PortalPulse.reset()`).
   Bewusst nicht: `InWorldUpgrades.loaded` (statisch geteilt mit dem integrierten Server; Einzelspieler-Server lädt neu, Fremd-Server schickt beim Beitritt die Tabelle) – Leeren beim Trennen würde im Einzelspieler die Server-Tabelle treffen. Module mit eigenem DISCONNECT (Mobs, Maps, Visuals …) bleiben unverändert (funktionieren, Umbau ohne Nutzen; neue Caches sollen `ClientState` nutzen). Simple Maps/Visuals leeren ihre Caches bei Weltwechsel bereits selbst.
2. **CONFIG-WRITE-Regel** in `tools/check_codec_lists.py` (läuft als `checkCodecLists` in `check` und precheck): `Files.writeString` in Produktions-Java, das eine Config + `.json` nennt, muss in eine Temp-Datei schreiben, `CREATE_NEW`/`!Files.exists` (erste Standarddatei) sein oder in `WRITE_ALLOWED` stehen (Liste darf nur schrumpfen). Mehrzeilige Aufrufe werden erkannt.
   8 Befunde auf `ConfigFiles.writeAtomic` umgestellt: Simple Challenges `PresetRegistry`, `RecentChallenges`; Simple Interfaces `KeyboardStore`, `LegacyMigration` (options.txt); SimpleLib `DevVariants`, `UiBrightness`; Simple Maps `MapLayers`; Simple Tweaks `DeedHint`.
   Ausnahmen: `RenamedModels` (erzeugte Resourcepack-Dateien), `HangWatchdog` (Dump-Bericht). `FunConfigScreen` in `WRITE_PENDING` (b-cfgpath27 schreibt genau diese Zeile um; veralteter Eintrag nur Hinweis, kein Fehler – vermeidet Merge-Rot).
3. **WarmStackingMixin**: Befund bestätigt – `RemoteSlot.Synchronized.matches` nutzt `ItemStack.matches` → `isSameItemSameComponents` (warm-blind) → reine Wärme-Änderung im offenen Menü wurde nicht gesendet. Fix: zweite Injektion `ItemStack.matches(ItemStack,ItemStack)` RETURN → bei gleicher Anzahl, aber anderem `simplelib:warm` false. Stapeln (isSameItemSameComponents) bleibt warm-blind (Besitzer 40). `WarmMerge.sameWarmth` verwirft zusätzlich das Paar, das der Vergleich in `matches` gemerkt hätte (sonst könnte ein späteres `grow` Wärme mischen).

## Tests
- Gametests simplelib: `client_state_disconnect` (Ersetzen, werfender Listener), `warm_change_reaches_menu_sync` (matches warm-genau, Stapeln weiter warm-blind, `RemoteSlot.Synchronized` meldet Wärme-Änderung, kein Mischen nach reinem Vergleich).
- Lokal: javac der geänderten Dateien gegen MC-26.3-Jar ohne neue Fehler; `check_codec_lists.py` vorher 8 Befunde, nachher OK; `check_gametest_adapters` OK; precheck --fast grün.
- sbt: Compile Kern + simplelib/challenges/interfaces/maps/tweaks, Gametests simplelib fabric + standalone.

## Nicht getestet
Client-Lauf (Regel 16: keine Client-Läufe); DISCONNECT-Pfad selbst nur über die Registry geprüft.
