# Plan a-claims-carto: Claims-Umbau Branch 3 – Kartentisch und Alt-Tisch (SP22-8, 23.10.2026)

Basis: `a-claims-deedui` (Branch 2) + Merge `a-claims-maps` (Branch 4, Wunsch Hauptsitzung 23.10.: a-claims-carto ist
Sammel-Branch für SP22-8 Teile 1–4). Konzept `docs/ai/KONZEPT-CLAIMS-UMBAU-2026-10-22.md` §5, §6.3, §8 Zeile 3.
Gilt nur MC 26.3 Fabric (Regel 22.10.). **C1–C13 vorläufig nach Empfehlung**; hier relevant C8 (Verwalter M darf
kopieren), C9 (Werkbank-Kopie bleibt), C11 (Alt-Tisch → Kartentisch + leere Urkunde).

## Ist (erhoben)
- Kartentisch-Hüllen: Simple Maps 1000, SimpleBuilding-Blaupause 1100, Simple Music `SheetCopy.wrapSlots` 1200 – jede
  Hülle umhüllt Slot 0/1/2 und reicht Unbekanntes an den Vorgänger weiter.
- Urkunde `simpletweaks:claim_deed` + Komponente `DeedData{owner, owner_name, generation}`; Werkbank-Kopie
  `DeedCloningRecipe` (generation 1, keine Kopie der Kopie).
- Claim-Tisch `table/ClaimTableBlock` (Block + BlockItem, Rezept, Fortschritt, Loot „sich selbst“, Kreativ-Tab +
  Vanilla-Tab „Funktionsblöcke“); Server `TableService`, Bildschirm `TableScreen/TableLayout`, Pakete `Table*Payload`,
  Tests `TableTests` (basics/service), `ExpansionTests.tableExpand`, JUnit `TableLayoutTest`.
- `ClaimMapLayer`, `ClaimGrid`, `TableVisibility` (Paket `table`) nutzt die Maps-Ebene (Branch 4) → bleiben.
- Kaufen läuft seit Branch 2 über die Urkunde (`DeedService`, Schlüssel `buy.*`), die Texte
  `message.simpletweaks.claim_table.expand.*` nutzt nur noch `TableService`.

## Änderungen
1. **`deed/DeedCartography.java`** (neu, Muster `SheetCopy`): Regeln + Slot-Hüllen.
   - `result(player, server, top, bottom)`: oben leere Urkunde + unten leer → Urkunde gebunden an den Menü-Spieler
     (generation 0); oben gebundene Original-Urkunde (generation 0) + unten leere Urkunde → Kopie (generation 1), nur
     wenn der Spieler Besitzer ist oder in dessen Land `M` hat (C8); alles andere mit Urkunde → leer.
   - `InputSlot` (Slot 0/1 nehmen zusätzlich Urkunden), `ResultSlot.onTake`: eigene Urkunde → oberer Slot −1, Kopie →
     unterer Slot −1 (Original bleibt); ohne Urkunde → `original.onTake` (Maps/SB/Music/Vanilla).
   - Shift-Klick: gebundene → oben, leere → oben wenn frei, sonst unten.
   - Nur wenn `Claims.on()` (Slots) bzw. `Claims.enabled(server)` (Ergebnis); sonst reicht die Hülle alles weiter.
   - Das Ergebnis wird serverseitig für den Menü-Spieler gebildet; ein Menü gehört genau einem Spieler, der Nehmende ist
     also immer der Gebundene (Konzept „Bindung bei onTake“ dadurch erfüllt, kein Umbinden nötig).
2. **Mixin `mixin/CartographyDeedMixin`** (Priorität 1300) in `simpletweaks.mixins.json`: `<init>` TAIL hüllt Slots,
   merkt den Spieler; `setupResultSlot` HEAD nur bei beteiligter Urkunde; `quickMoveStack` HEAD für Urkunden.
3. **Alt-Tisch** (`ClaimTableBlock`, IDs bleiben registriert): Rechtsklick → Vanilla-Kartentisch + leere Urkunde
   (ausgeworfen); Abbau → Loot Kartentisch + leere Urkunde; Setzen des Alt-Items (`setPlacedBy`) → Kartentisch + leere
   Urkunde ins Inventar (sonst ausgeworfen). Alt-Item mit Tooltip „Veraltet: wird zum Kartentisch“. Kein Tick-Scan.
   Rezept + Rezept-Fortschritt gelöscht, aus Kreativ-Tab und Vanilla-Tab (Fabric/NeoForge/Forge-Hook) entfernt.
4. **Aufräumen:** `TableService`, `TableLayout`, `table/client/*`, `table/net/*`, `TableLayoutTest`, Netz-Registrierung
   (Fabric + NeoForge/Forge nur Zeilen entfernen, damit `check` kompiliert – keine Anpassung/kein Test dieser Loader),
   Lang `simpletweaks.table.*`, `message.simpletweaks.claim_table.*`; `block.simpletweaks.claim_table` bleibt (Alt-Item).
   `TableTests` → `basics` ohne Paket-/Dienstteile (Sichtbarkeitsregeln bleiben, Maps-Ebene nutzt sie),
   `service` und `ExpansionTests.tableExpand` entfallen (Kaufen ist in `claims_deedui_actions` getestet).
5. Tooltip leere Urkunde: zweite Zeile „Kartentisch: deine eigene Urkunde“. Wiki `claim_table` → „Alter Claim-Tisch“,
   Urkunden-Eintrag um Kartentisch ergänzt; Discover/check_data ohne Tisch-Eintrag; Generatoren.
   **Abweichung:** Erstnutzungs-Tipp beim Kartentisch entfällt vorerst (Tweaks hat noch keinen Tipp-Mixin; Tooltip,
   Wiki und Guide decken den Hinweis) – als offen notiert.

## Tests (Gametests `claims/CartoTests.java`, Fabric-Adapter `ClaimsGameTest`)
- `claims_carto_own`: leere Urkunde oben → eigene Urkunde (Name/UUID des Spielers, generation 0); Nehmen verbraucht
  die leere; Urkunde unten belegt → kein Ergebnis.
- `claims_carto_copy`: eigene + leere → Kopie (generation 1), Original bleibt, nur untere verbraucht; Verwalter (M)
  darf kopieren; fremde Urkunde → kein Ergebnis; Kopie oben → kein Ergebnis; Werkbank-Rezept bleibt registriert.
- `claims_carto_wrap`: ohne Urkunde reicht die Hülle weiter (leere Karte + Papier → Vanilla-Ergebnis), Slots nehmen
  Urkunden, Shift-Klick-Ziele.
- `claims_carto_legacy`: Alt-Tisch Rechtsklick → Kartentisch + Urkunden-Item; Alt-Item setzen → Kartentisch + Urkunde
  im Inventar; Abbau-Loot = Kartentisch + Urkunde; kein Rezept, nicht im Kreativ-Tab.

## Verifikation (nur über `/root/sbt a-claims-carto`)
Compile `:mc26_3:fabric:compileJava` + `:modules:simpletweaks:fabric:compileJava`; Testrunner
`module-simpletweaks-fabric-263`, `module-simpletweaks-standalone-fabric-263`, `module-simpletweaks-with-simplemaps-fabric-263`,
`module-simplemaps-fabric-263`; Generatoren + `./gradlew check -q`.
SB-Kartentisch (Blaupause) ist nicht geändert; `fabric-263` mit Filter `simplebuilding:blueprint*` nur, falls SB-Tests
die Hülle berühren (Hülle reicht ohne Urkunde unverändert weiter).
Nicht getestet: Client (keine Client-Läufe), NeoForge/Forge (Regel), `integration-263` mit Music+SB (Hüllen-Reihenfolge).
