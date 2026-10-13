# Plan: Claim-Urkunde mit eigener GUI und Rechten je Person (Queue N54, 13.10.)

Stand 2026-10-13 · Branch `claude-claimdeed-plan` (von `claude-wave1` 4f1d5a8e2) · nur Plan, nichts umgesetzt.
Besitzer-Wunsch: Die Claim-Urkunde (Simple Tweaks) soll wie die Blaupause (SimpleBuilding) eine eigene GUI bekommen,
dort Personen hinzufügen und ihnen Fähigkeiten geben (bauen, abbauen, Container öffnen …). „Code-Form oder
Checkbox-Form – was am Ende die bessere Entscheidung ist.“ Mockup beider Varianten: Artifact-Seite „Claim-Urkunde Entwurf“.

## 1. Ist-Zustand

### 1.1 Urkunde (`modules/simpletweaks/shared/java/.../LegacyDeed.java`)
- Item `simpletweaks:claim_deed`, Name „Legacy Claim Deed“, Stapel 16, **kein Rezept, kein Kreativ-Tab** (nur `/give`).
- Rechtsklick (Server, Claims aktiv) = `Claims.claim(player)`: Chunk unter dem Spieler beanspruchen, wie `/claim create`;
  Erfolg = XP-Ton + Funken, Fehlschlag = Werfer-Klick + Rauch. Die Urkunde wird **nicht** verbraucht.
- Kein Item-Datenteil wirkt als Berechtigung („only the server ledger grants authority“) – Grundsatz bleibt.

### 1.2 Claims-Datenmodell und Persistenz (`claims/ClaimStore.java`, `Claims.java`, `ClaimConfig.java`)
- Pro **Chunk** ein Eintrag `Key(dimension, chunk)` → `Claim(owner UUID, whitelist Set<UUID>)`.
- Rechte sind **binär**: Besitzer oder Vertrauter darf alles (`Claim.permits`), sonst nichts.
- Vertrauen gilt **nur für den Chunk, in dem der Besitzer steht** (`trust()` nimmt `actor.chunkPosition()`): wer zehn
  Chunks hat, muss zehnmal `/claim trust` ausführen (je 100 Ticks Abklingzeit).
- Ledger: eine globale JSON-Datei `<welt>/simpletweaks-claims.json`, `version: 1`, Zeilen
  `{dimension, chunk, Owner, Whitelist[]}`; atomar geschrieben (Temp + ATOMIC_MOVE), 32 MiB-Grenze, Symlinks abgelehnt;
  fehlerhafte Daten → `locked` (fail closed, Originale bleiben). Alte NBT-Dateien je Dimension werden nur gelesen.
- Grenzen (Config `config/simpletweaks-claims.json`, serverweit, Neustart): `maxClaimsPerPlayer` 16 (≤256),
  `maxTrustedPlayers` **8** (≤64), `globalCap` 4096 (≤10000), `cooldownTicks` 100, `opBypass` aus, `spawnBuffer` 16,
  Dimensionsliste. Seit 13.10. `enabled: true` (Plan PLAN-CLAIMS-AN).
- **Kein Sync zum Client**, keine Pakete, kein Screen, kein Client-Entrypoint im Modul. Alles serverseitig.

### 1.3 Befehle (`ClaimCommands.java`)
`/claim` (Besitzer anzeigen), `/claim create`, `/claim unclaim`, `/claim trust|untrust <Name|UUID>` (Online-Name oder
volle UUID, keine blockierende Namensauflösung), `/claim admin [unclaim|listall|list <Spieler>]` (OP4). Fremdes `/claim`
→ Warnung, Befehle entfallen, Schutz bleibt.

### 1.4 Schutz-Hooks (~45 Mixins unter `mixin/claims`) und Framework
- Spieler-Aktionen laufen über wenige Eingänge: `Claims.allowBlock` (Abbau `destroyBlock`, **jedes** `useItemOn` –
  Truhe, Tür, Werkbank und Platzieren sind dieselbe Prüfung), `Claims.allow` (`mayInteract`, Block-Item, Eimer),
  `Claims.allowEntity` (`interactOn`, `attack`, `stabAttack`, Boote), `Claims.action/damage` (Geschosse, Effekte, Aufheben).
- Umgebung/Automation (Feuer, Explosion, Fluss, Kolben, Trichter, Werfer, Kupfergolem, Mobs) prüft nur
  „gleicher Besitzer“ – **unabhängig von Vertrauten**, bleibt unverändert.
- Framework `framework/src/main/java/com/simplebuilding/framework/api/Protection.java`: `Target(actor, dimension, x,y,z,
  administrator)` ohne Aktionsart. SB ruft es über `api/WorldPermissions` (`mayAct`, `mayChange`, `mayPlace`,
  `mayAffectEntity`, `mayTeleport`, `mayAutomate`) für Hammer/Vein/Strip, Baustab, Laser, Echo, Pads, Attractor.

### 1.5 Blaupause (SimpleBuilding, Vorbild)
- Item mit Komponente `simplebuilding:blueprint = {code, title, author, signed}`, Rezept formlos Enderquarz + Papier +
  Tintenbeutel. Rechtsklick öffnet `BlueprintScreen` (Kartenblatt-Stil): Material-Liste, **Code-Editor** (eigene
  Bausprache, ≤32000 Zeichen, Syntaxfarben, „Code OK“), 3D-Vorschau, Hilfe-Buch, Signieren | Fertig.
- Speichern: `BlueprintEditPayload(slot, code, sign, title, placed?)`, Server prüft Slot/Position, Item, Länge,
  Signatur, `WorldPermissions.mayChange`, Zeichenbudget (Token-Bucket). Kopieren am Kartentisch (beschrieben + leer →
  Kopie ohne Autor). Text-Export „Text kopieren“ (Zwischenablage).
- **Liegt komplett in SB** (`com.simplebuilding.blueprint.*`, `client/blueprint/*`), nichts davon in simplelib.
  Wiederverwendbar ist nur das **Muster** (Item öffnet Screen, Server validiert Payload, Kopie per Rezept, Text-Export),
  kein Code. Die Bausprache passt inhaltlich nicht (beschreibt Blöcke, nicht Rechte).
- Simple Tweaks darf SB nicht importieren (Regel 3); `modules.json`: simpletweaks `requires: []`,
  `optional: [simpledimensions, simplebuilding]`.

### 1.6 UI-Stil
- SimpleLib-Bausteine `com.simplelib.api.client.ui` (`docs/ai/UI-BAUSTEINE.md`): `UiBoxes.box/slot/insetColored`,
  `UiPalette.derived`, `UiSymbols`, `UiContextMenu`, `UiBookmarks.tab/button/confirm/greyOut`, `UiFilterButton`.
- Screens ohne Slots (N43): `UiBoxes.box` als Fläche, Titel x 8 / y 6 in `palette.label()`, Listen im hellen Feld
  (`insetColored` mit `INVENTORY.fill`, dunkle Schrift), Vanilla-Widgets 8 px vom Rand.
- Helligkeit N49: `UiBrightness` LIGHT/DAWN/DARK wirkt zentral über `UiPalette`, Labels werden bei dunkel hell.
- **Kein Checkbox-Baustein** in simplelib. Simple Tweaks bündelt simplelib heute **nicht** (Vorbild für das Bündeln:
  `modules/simplemaps` – `include`/`jarJar`, `WaypointEditPayload`, `WayfinderScreen`, `MapsFabricClient`).

## 2. Entscheidung Code-Form vs. Checkbox-Form

| Kriterium | Code-Form (Text wie Blaupause, z. B. `alex PBCU`) | Checkbox-Form (Matrix Person × Recht) |
|---|---|---|
| Verständlich ohne Anleitung | nein – Buchstaben lernen, Syntax | ja – Symbol + Tooltip, sofort sichtbar |
| Fehler möglich | Tippfehler, unbekannte Namen, Syntaxfehler | keine (nur gültige Zustände klickbar) |
| Überblick „wer darf was“ | muss gelesen werden | auf einen Blick (8 Zeilen × 7 Spalten) |
| Viele Claims gleich einstellen | Code kopieren/einfügen | über Geltungsbereich „Grundstück/alle“ + Vorlage auf der Urkunde |
| Teilen mit anderen Spielern | Text im Chat | Urkunden-Kopie (Rezept) oder Text-Export |
| Server-Sicherheit | Parser auf dem Server nötig (Länge, Namen auflösen) | feste Bitmaske, triviale Prüfung |
| Bezug zur Blaupause | gleiches Bedienmuster | gleiches Muster (Item → Screen → Payload), anderer Editor |
| Aufwand | Parser, Syntaxfarben, Hilfe-Buch | Checkbox-Baustein + Liste |

Die Blaupause braucht Code, weil Bauwerke beliebig groß und frei formbar sind. Rechte sind eine **kleine feste Menge**
(≤ 65 Personen × 7 Flags) – dafür ist eine Checkbox-Matrix das passende Werkzeug; Code bringt nur Fehlerquellen.

**Empfehlung: Kombi, Checkboxen führen.**
1. GUI mit Checkbox-Matrix (Bearbeiten).
2. Übertragen ohne Code: Geltungsbereich (Chunk / Grundstück / alle meine Claims) + die Urkunde merkt sich die zuletzt
   gespeicherte Liste als **Vorlage** („Vorlage anwenden“ in einem anderen Claim) + Urkunden-Kopie per Rezept.
3. Nur **Export** als Text (Knopf „Als Text kopieren“, Zwischenablage, Format unten §5) – für Chat/Admins/KI-Fragen,
   wie „Text kopieren“ bei der Blaupause. **Kein Code-Import** in Phase 1/2; nur falls der Besitzer ihn später will (§11).

## 3. Datenmodell

### 3.1 Rechte-Flags (Bitmaske `int`, 7 Bits)
| Bit | Recht (DE / EN) | Schlüssel | Deckt ab |
|---|---|---|---|
| 0 | Bauen / Build | `P` | Blöcke platzieren, Eimer ausgießen, SB-Baustab/Pads platzieren |
| 1 | Abbauen / Break | `B` | Blöcke abbauen, Eimer füllen, SB Hammer/Vein/Strip/Laser, Feldtrampeln durch Spieler |
| 2 | Container / Containers | `C` | Truhen, Fässer, Öfen, Trichter-GUI, Shulker, Lesepult, Bücherregal, Topf; Rahmen & Rüstungsständer; Gegenstände aufheben, Attractor |
| 3 | Benutzen / Use | `U` | Türen, Falltüren, Tore, Knöpfe, Hebel, Druckplatten, Glocke, Werkbänke/Amboss/Zaubertisch, Betten |
| 4 | Redstone / Redstone | `R` | Verstärker, Komparator, Notenblock, Tageslichtsensor, Crafter, Kolben-/Schaltungen einstellen |
| 5 | Tiere & Fahrzeuge / Animals & vehicles | `A` | Tiere/Dorfbewohner füttern, scheren, anleinen, handeln, verletzen; Boote/Loren setzen, fahren, abbauen |
| 6 | Verwalten / Manage | `M` | Personen hinzufügen/entfernen und Rechte vergeben (§3.4) |

- `ALL = 0b1111111`, `LEGACY = ALL & ~M` (alles außer Verwalten = bisheriges „vertraut“).
- Ernten (Getreide ohne Abbaurecht) ist **nicht** dabei → offene Frage F2.
- Zuordnung der Hooks über Block-/Entity-Tags `simpletweaks:claim_use`, `claim_redstone`, `claim_containers`
  (anpassbar per Datapack); Block mit `MenuProvider`/`Container`-Block-Entity → Container; sonst `useItemOn` mit Block-Item
  in der Hand → Bauen; übriges `useItemOn` → Benutzen. Unbekannte Fälle **streng**: Bauen **und** Abbauen nötig.
- PvP/Schaden an Spielern im Claim: wie bisher (jedes Mitglied mit Rechten ≠ 0 gilt wie früher „vertraut“).

### 3.2 Claim-Datensatz
`Claim(owner UUID, Map<UUID,Integer> members, int everyone)` – `everyone` = Rechte für alle Spieler (Zeile „Alle“,
Standard 0; z. B. nur „Benutzen“ für einen Laden). `members.size() ≤ maxTrustedPlayers`, Owner nie in `members`,
Null-UUID verboten, Maske `& ALL`, Einträge mit 0 werden entfernt.

### 3.3 Geltungsbereich
Rechte bleiben **pro Chunk gespeichert** (kein Rechte-Gewinn durch Migration). Die GUI schreibt auf einen Bereich:
- **Grundstück** (Standard): alle über Kanten verbundenen Chunks desselben Besitzers in derselben Dimension
  (Flood-Fill, ≤ `maxClaimsPerPlayer` ≤ 256).
- **Dieser Chunk** oder **Alle meine Claims** (alle Dimensionen).
Gemischte Bereiche (Chunks mit unterschiedlichen Listen) zeigt die GUI als „gemischt“ (Kästchen halb gefüllt); Speichern
setzt nur geänderte Zellen, unveränderte Zellen bleiben je Chunk wie sie sind.

### 3.4 Verwalten
Verwalter dürfen Personen **ohne** Verwalten-Recht hinzufügen, ändern, entfernen und nur Rechte vergeben, die sie selbst
haben. Sie dürfen keine Verwalter ernennen, nicht `unclaim`en, „Alle“ nicht ändern, den Besitzer nicht ändern. Nur der
Besitzer vergibt `M` (bisheriger Grundsatz „Trust never delegates administration“ bleibt für den Besitzer-Teil).

### 3.5 Ledger v2 und Migration
- Datei bleibt `simpletweaks-claims.json`, `version: 2`, Zeile `{dimension, chunk, Owner, Members:[{id, rights}], Everyone}`.
- Lesen v1: jeder `Whitelist`-Eintrag → `LEGACY` (alles außer Verwalten), `Everyone 0`. Gleiches für alte NBT-Dateien.
  Damit ändert sich für bestehende Welten **nichts** am Verhalten.
- Vor dem ersten v2-Schreiben einmalig Kopie `simpletweaks-claims.v1.json` (atomar, nie überschrieben).
- Unbekannte Bits/Version > 2 → `locked` wie heute (fail closed). Downgrade auf alte Mod-Fassung → alte Fassung sperrt
  (liest v2 nicht) – Kopie v1 liegt bereit; im Changelog nennen.

### 3.6 Rollen (Vorlagen in der GUI, keine gespeicherte Rolle)
| Rolle | Rechte |
|---|---|
| Gast | Benutzen |
| Helfer | Benutzen, Container, Tiere & Fahrzeuge |
| Mitbewohner (Standard bei „Hinzufügen“ und `/claim trust`) | alles außer Verwalten (= bisher „vertraut“) |
| Verwalter (nur Besitzer) | alles |
Rollen sind nur Knöpfe, die Kästchen setzen; gespeichert wird die Maske.

### 3.7 Urkunden-Item
- Umbenennen „Claim-Urkunde“ / „Claim Deed“ (ohne „Legacy“), Stapel 1 (trägt Daten), in den Kreativ-Tab von Simple Tweaks.
- Komponente `simpletweaks:deed_template = {members:[{id, name, rights}], everyone}` (≤ 65 Einträge, Name ≤ 16 Zeichen,
  nur Anzeige/Vorlage, **nie Berechtigung**). Alte Stapel ohne Komponente bleiben gültig (leere Vorlage).
- Klasse `LegacyDeed` → `ClaimDeedItem` (Registry-ID bleibt `simpletweaks:claim_deed`, keine Alias-Änderung nötig).

## 4. Beschaffung im Survival
- **Rezept formlos:** Papier + Tintenbeutel + Honigwabe (Siegelwachs) → 1 Claim-Urkunde. Nur Vanilla-Material (Regel 1/5),
  keine SB-Variante nötig. Die Urkunde wird beim Beanspruchen **nicht** verbraucht (Werkzeug wie die Blaupause);
  Begrenzung über `maxClaimsPerPlayer`/`globalCap`/Abklingzeit. → F3.
- **Kopie:** formloses Sonderrezept „beschriebene Urkunde + leere Urkunde → 2 Urkunden mit gleicher Vorlage“ (wie Buch-
  Kopie; Kartentisch-Weg der Blaupause liegt in SB und ist hier nicht nutzbar). Rezeptbuch-Freischaltung über Papier.
- Rezept-Advancement `advancement/recipes/misc/claim_deed.json`, JEI-Info-Text.

## 5. GUI (Variante A, empfohlen) – Layout 256 × 196 px (GUI-Skala 1)
```
┌ Claim-Urkunde ─ Grundstück „Nord“ · 5 Chunks · Overworld ─────────── [?] ┐
│ [Grundstück ▾]  Besitzer: Ouzzi                                          │
│ ┌ Person ───────────── P  B  C  U  R  A  M ┐  ┌ Hinzufügen ────────────┐ │
│ │ ★ Alle                □  □  □  ■  □  □  · │  │ [Name/UUID______] [+]  │ │
│ │ ☺ Alex                ■  ■  ■  ■  ■  ■  □ │  │ Online:                │ │
│ │ ☺ Steve               □  □  ■  ■  □  ■  □ │  │  ☺ Lena     [+]        │ │
│ │ ☺ Mira (offline)      ■  ■  ■  ■  ■  ■  ■ │  │  ☺ Tom      [+]        │ │
│ │ …  (Scrollleiste, max 8 sichtbar)         │  │ Rolle für Neue:        │ │
│ └───────────────────────── 4 / 8 Personen ─┘  │ [Mitbewohner ▾]        │ │
│ Zeile Rechtsklick: Gast | Helfer | Mitbewohner | Verwalter | Entfernen    │ │
│ [Vorlage anwenden] [Als Text kopieren]               [Abbrechen] [Fertig] │
└───────────────────────────────────────────────────────────────────────────┘
```
- Kasten `UiBoxes.box` in Papier/Siegel-Palette (`UiPalette.derived(#C9B48A)` o. ä.), folgt `UiBrightness`.
- Liste im hellen Feld (`insetColored`, dunkle Schrift), Spaltenköpfe = `UiSymbols`-Pixel-Symbole (Kelle, Spitzhacke,
  Truhe, Tür, Redstone-Fackel, Pfote/Lore, Siegel) mit Tooltip „Bauen – Blöcke platzieren …“.
- Kästchen: neuer simplelib-Baustein **`UiCheckbox`** (eingelassenes 9×9-Feld, Pixel-Haken; Zustände an/aus/gemischt/
  gesperrt-grau). Klick schaltet, Klick auf Spaltenkopf schaltet die ganze Spalte, Rechtsklick auf Zeile =
  `UiContextMenu` mit Rollen und „Entfernen“ (Zweitklick-Bestätigung `UiBookmarks.confirm`).
- Gesperrt/grau (`greyOut`): `M`-Spalte für Verwalter, alles für Nicht-Besitzer ohne `M`, „Alle“-Zeile `M`.
- Geltungsbereich-Knopf oben links (Chunk / Grundstück / Alle meine Claims).
- Hinzufügen: Textfeld (Online-Name oder volle UUID, wie `/claim trust`) + Online-Liste (≤ 64 Namen, ohne sich selbst
  und ohne vorhandene Mitglieder). Offline-Namen nur aus dem **Server-Namens-Cache**, nie Netzwerk-Abfrage.
- Wildnis: statt Liste ein Feld „Hier ist kein Claim“ + Knopf **„Chunk beanspruchen“** (= `/claim create`); die Vorlage
  der Urkunde ist dann direkt editierbar und wird beim Beanspruchen angewendet.
- Fremder Claim ohne `M`: nur Anzeige „Besitzer: X – du darfst: Benutzen, Container“ (hilft beim Verstehen, keine Liste
  fremder Mitglieder).
- Text-Export (Zwischenablage), Format eine Zeile je Person: `Alex: PBCURA` / `Alle: U` + Kopfzeile Grundstück.
- Hilfe `[?]`: kurze Seite im Guide (TweaksGuide) statt eigenem Hilfe-Buch.
- Variante B (Code-Form) ist im Mockup daneben gezeigt; wird nicht gebaut.

## 6. Netzwerk und Server-Sicherheit
- `ClaimDeedOpenPayload` (S→C, Antwort auf Rechtsklick): Modus (eigen/verwalten/fremd/wildnis), Bereich, Chunkzahl,
  Besitzername, Mitglieder `(uuid, name ≤16, rights, gemischt-Maske)` ≤ 65, Online-Namen ≤ 64, `revision` (Hash der
  Rechte im Bereich), Grenzen (maxTrusted). Keine fremden Daten außerhalb des Bereichs.
- `ClaimRightsEditPayload` (C→S): `slot`, `scope`, `revision`, Änderungen `List<(uuid, setMask, clearMask)>` ≤ 66,
  `adds List<String name|uuid>` ≤ 8, `everyone (set/clear)`, `saveTemplate bool`. Codec mit festen Längen
  (`stringUtf8(36)`, `list(…, 66)`), sonst Verbindungsabbruch.
- `ClaimCreatePayload` (C→S) nur Slot; Server ruft vorhandenes `Claims.claim`.
- Server prüft **alles neu**: Spieler hält eine Urkunde im Slot (wie Blaupause), steht im Bereich bzw. hat Reichweite,
  ist Besitzer oder Verwalter (§3.4), `revision` stimmt (sonst „Daten haben sich geändert“ + neue Open-Antwort), Masken
  `& ALL`, Cap `maxTrustedPlayers`, Null-UUID/Besitzer abgelehnt, Namen nur online/Cache, Abklingzeit
  `mutationReady` (eine Speicherung = ein Commit, egal wie viele Kästchen), Claims nicht `locked`, Hauptschalter an.
  Ein Commit pro Speichern für den ganzen Bereich (atomar, alles oder nichts).
- Client sendet nur beim „Fertig“ (nicht je Klick); „Abbrechen“/Esc verwirft.
- `/claim`-Befehle laufen über dieselbe Server-Methode `Claims.setRights(actor, scope, changes)`.

## 7. Befehle (Zusammenspiel)
- `/claim trust <Spieler> [rolle|rechte]` – ohne Angabe Mitbewohner (= bisheriges Verhalten), Rolle `guest|helper|
  member|manager` oder Buchstaben `PBCURAM`; `/claim untrust <Spieler>` unverändert.
- `/claim rights <Spieler> <+P-B…>` ändert Einzelrechte; `/claim info` zeigt Mitglieder + Rechte des Chunks.
- Neues optionales Argument `scope` (`chunk|plot|all`, Standard `chunk` – Befehle bleiben rückwärtskompatibel).
- Admin-Befehle unverändert; `listall` zeigt Mitgliederzahl.

## 8. Dateien (Umsetzung)
- `modules/simpletweaks/shared/java/.../claims/ClaimRights.java` (neu: Bits, Rollen, Text-Export, Parser für Befehle),
  `ClaimStore.java` (v2 + Migration + v1-Kopie), `Claims.java` (`allowed(…, right)`, `setRights`, Bereich-Flood-Fill),
  `ClaimCommands.java`, Hook-Zuordnung in `ClaimGameModeMixin`, `ClaimPlayerMixin`, `ClaimBlockItemMixin`,
  `ClaimBucketMixin`, `ClaimBoatMixin`, `ClaimPickupMixin`, `ClaimLevelMixin`, `ClaimProjectileMixin`.
- `LegacyDeed.java` → `ClaimDeedItem.java`; `net/` (3 Payloads + `ClaimNetwork`), `client/ClaimDeedScreen.java`.
- Loader: Fabric `client`-Entrypoint + Payload-Registrierung, NeoForge Client-Event + Payload; Forge erst auf Anforderung
  (Memory „Ports erst auf Anforderung“).
- simplelib bündeln (Regel 6a): `modules.json` `requires: [simplelib]`, Fabric `include`, NeoForge `jarJar`,
  `fabric.mod.json`/`neoforge.mods.toml` `>=0.2.0`; Standalone-Target anpassen.
- simplelib: `UiCheckbox` (+ Eintrag in `docs/ai/UI-BAUSTEINE.md`), 7 neue `UiSymbols` falls nicht vorhanden.
- Framework `Protection`: `enum Action {PLACE, BREAK, CONTAINER, USE, REDSTONE, ENTITY, ANY}` + Überladung
  `allows(server, target, action)`; `Check` bekommt `default allows(target, action) → allows(target)`. Alte Aufrufe =
  `ANY` = streng (Bauen **und** Abbauen). SB `WorldPermissions` gibt die Aktion mit (Hammer → BREAK, Baustab → PLACE,
  Attractor → CONTAINER, `mayAffectEntity` → ENTITY). Dimensions-Adapter/`mayTeleport` bleibt Mitgliedschaft ≠ 0.
- Daten: Rezept + Kopier-Rezept-Serializer, Tags `claim_use/claim_redstone/claim_containers`, Lang EN/DE,
  Item-Modell + Textur (Leer / mit Vorlage), `wiki/manual.json`, `TweaksGuide`, Doku `CLAIMS-*.md`.

## 9. Tests
- JUnit (ohne MC): `ClaimRights` (Masken, Rollen, Export, Befehls-Parser), Ledger-Migration v1 → v2 (Whitelist → LEGACY,
  v1-Kopie, Unbekannte Bits → locked), Bereich-Flood-Fill (Lücken, Ecken, Dimensionsgrenze).
- GameTests (Fabric + NeoForge 26.3, Filter `*claim*`):
  - je Recht: Mitglied **mit** nur diesem Recht darf genau diese Aktion, **ohne** wird abgelehnt (Platzieren, Abbauen,
    Truhe öffnen, Tür, Hebel, Verstärker, Schaf scheren, Boot setzen), Doppeltruhe über Chunkgrenze, Bett.
  - „Alle“-Zeile (Fremder darf nur Tür), Verwalter darf keinen Verwalter ernennen / keine fehlenden Rechte vergeben.
  - Payload: falscher Slot, keine Urkunde, fremder Claim ohne M, veraltete `revision`, zu lange Liste, Null-UUID,
    Abklingzeit, `locked` – alles abgelehnt, Ledger unverändert.
  - Bereich: Grundstück ändert alle verbundenen Chunks in einem Commit; „gemischt“ bleibt pro Chunk erhalten.
  - Befehle: `trust` ohne Rolle = LEGACY, `rights +P-B`, `info`.
  - Bestehende 61 Claim-GameTests unverändert grün (Migration = gleiches Verhalten).
  - SB-Filter `*attractor*`, `*hammer*`, `*wand*` mit Aktionsarten.
- Client-Test (`clienttest`): Screen öffnet, Kästchen klicken, Fertig sendet; Screenshot hell/dunkel nach `previews/`.
- Standalone-Target simpletweaks (+ simplelib) grün.

## 10. Wiki, Guide, Tooltip
- Tooltip (AGENTS Punkt 14: nichts wiederholen, was sichtbar ist): „Rechtsklick: Urkunde öffnen“; mit Vorlage zusätzlich
  „Vorlage: 3 Personen“. Keine Rechte-Liste im Tooltip (steht in der GUI).
- Wiki `modules/simpletweaks/wiki/manual.json`: Abschnitt „Claim-Urkunde“ (Rezept, GUI, Rechte-Tabelle, Rollen,
  Bereich, Kopie, Befehle), EN + DE; Guide-Seite in `TweaksGuide`; Questionary-Eintrag falls Schema es verlangt.

## 11. Offene Fragen (mit Empfehlung)
- **F1 Code-Import?** Empfehlung: nein, nur Text-Export; Übertragen über Vorlage/Kopie/Bereich.
- **F2 Eigenes Recht „Ernten“** (reife Pflanzen ernten + neu säen ohne Abbaurecht)? Empfehlung: ja, als 8. Bit in Phase 2,
  wenn der Besitzer es will; Phase 1 nicht.
- **F3 Rezept/Verbrauch:** Empfehlung Papier + Tintenbeutel + Honigwabe, Urkunde wird nicht verbraucht.
- **F4 Standard-Bereich:** Empfehlung „Grundstück“ (verbundene Chunks); Befehle bleiben bei „Chunk“.
- **F5 Verwalten-Recht** in Phase 1? Empfehlung: ja, mit den Grenzen aus §3.4.
- **F6 Zeile „Alle“** (öffentliche Rechte)? Empfehlung: ja, Standard leer.
- **F7 Textur:** Besitzer-Motiv (Pergament + rotes Siegel, analog Blaupause signiert)? Empfehlung: 2 Entwürfe als
  Artifact-Abnahme vor dem Merge.
- **F8 simplelib bündeln** (Simple Tweaks bekommt harte Lib-Abhängigkeit nach Regel 6a)? Empfehlung: ja, sonst wäre der
  Container-Stil nicht einheitlich.

## 12. Aufwand und Phasen
| Phase | Inhalt | Aufwand |
|---|---|---|
| 1 | Rechte-Bits, Ledger v2 + Migration, Hook-Zuordnung, Framework-Aktion, Befehle, JUnit + GameTests | ~2 Agent-Tage (Hook-Zuordnung ist der größte Teil) |
| 2 | Urkunden-Item, Rezept/Kopie, Payloads, Screen + `UiCheckbox`, simplelib-Bündeln, Lang/Wiki/Guide, Client-Test | ~2 Agent-Tage |
| 3 | Vorlage anwenden, Text-Export, Textur-Abnahme, ggf. „Ernten“ | ~0,5–1 Agent-Tag |
Gesamt ~4,5–5 Agent-Tage, 2–3 Branches nacheinander (Phase 1 zuerst allein mergen, weil sie Verhalten aller Claims berührt).
Läufe: simpletweaks Fabric 26.3, dann NeoForge 26.3, dann SB-Filter; nie parallel (sb-test).

## 13. Risiken
- Falsche Zuordnung eines Hooks lockert Schutz → Standard „streng“ (unbekannt = Bauen + Abbauen) und je Recht ein
  Negativtest.
- Mod-Blöcke anderer Mods: Tag-basierte Zuordnung, sonst streng.
- Downgrade nach v2 sperrt Claims (fail closed) – v1-Kopie bleibt, Changelog-Hinweis.
- Abklingzeit 100 Ticks wirkt in der GUI wie „hängt“ → GUI zeigt Restzeit, Speichern bündelt.
