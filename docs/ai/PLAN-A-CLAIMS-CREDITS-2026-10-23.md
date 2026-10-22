# Plan a-claims-credits: Claims-Umbau Branch 1 – Credits und Dimensionen (SP22-8, 23.10.2026)

Basis: `claude-wave1` + Konzept `docs/ai/KONZEPT-CLAIMS-UMBAU-2026-10-22.md` (§3.1, §3.2, §6.1, §6.2, §8 Zeile 1).
Gilt nur MC 26.3 Fabric (Regel 22.10.). Kein UI in diesem Branch (Urkunden-UI = `a-claims-deedui`, Kartentisch =
`a-claims-carto`, Maps-Ebene = `a-claims-maps`).

**Entscheidungen C1–C13: vorläufig nach Empfehlung des Konzepts, Besitzer kann ändern.** Für diesen Branch relevant:
- C3 (A): Kosten bleiben Stufen-Pakete der Kurve 21.10. (`expansionTiers` in der Config, frei änderbar).
- C4 ja: Credits vorab kaufen → `/claim buy` (und Server-Methode für den späteren Urkunden-Knopf).
- C6 ja: alle Dimensionen auch auf bestehenden Servern, aber nur wenn die alte Standardliste unverändert in der Config
  steht. Leicht änderbar: `dimensions` in `config/simpletweaks-claims.json` (`["*"]` oder konkrete Liste).

## Ist (erhoben)
- `ClaimConfig.budgetOf(stored, held)` nimmt `max(…, held)` → Altland ohne gespeichertes Budget verliert beim Aufgeben
  den Platz (Konzept §1.3).
- `ClaimConfig.dimensions` Allowlist (Standard Overworld/Nether/End); `Claims.claim/create` prüfen `contains`.
- `Claims.expand(player)` kauft die nächste Stufe (Deed im Inventar, Material, Level; Budget zuerst gespeichert).
- `/claim budget` zeigt belegt/Budget/nächste Stufe; `/claim create|unclaim`.

## Änderungen
1. `ClaimConfig`
   - `budgetOf(Integer stored)` ohne `held`: `min(maxClaimsPerPlayer, max(stored ?: start, start))`; ohne Erweiterung
     weiter `maxClaimsPerPlayer`.
   - `dimensions` erlaubt `"*"` (= alle Dimensionen); `allowsDimension(id)`. Standard neu `["*"]`,
     `LEGACY_DIMENSIONS` = alte Liste.
   - `CONFIG_VERSION` 3; `migrate`: steht genau die alte Standardliste (gleiche Menge) in der Datei → einmal `["*"]`;
     jede andere Liste bleibt. Bestehende `enabled`-Migration (v1) bleibt.
2. `ClaimStore`: Marker `creditsMigrated` lesen/schreiben (additiv; alte Builds ignorieren ihn);
   `migrateCredits(budgets)`: Kopie `simpletweaks-claims.v2.json` (nur v2-Datei, nie überschreiben), dann ein atomarer
   Schreibvorgang mit Marker.
3. `Claims`
   - Einmalige Credit-Migration in `ready()`: je Besitzer `held > (stored ?: start)` → `budgets[owner] = held`
     (≤ 256). Abweichung vom Konzept-Wortlaut („jeder Besitzer ohne Budget bekommt max(start, held)“): Besitzer mit
     `held ≤ start` bekommen keinen Eintrag – Ergebnis gleich, aber eine spätere Änderung von `startChunks` wirkt für sie
     weiter. Nichts zu ändern → kein Schreibvorgang, Marker nur im Speicher (nächstes Speichern schreibt ihn).
   - `credits(owner)` = max(0, Budget − gehaltene Chunks); `paid(owner)` = Budget.
   - `unclaim(owner, key, tick)` (Server-Methode, auch für die Urkunde später); kein Material zurück, Budget bleibt.
   - `buy(player)` (= bisher `expand`, bleibt als Name erhalten) und `buyAndClaim(player)`: ohne freien Credit
     Budget + Claim **in einem** Commit, danach Material/Level; mit Credit nur Claim. Neuer Grund `PLACE`
     (Standort nicht beanspruchbar).
   - Dimensionsprüfung über `config.allowsDimension`.
4. `ClaimCommands`: `/claim credits` (bezahlt, belegt, frei, nächste Stufe), `/claim budget` zeigt dasselbe,
   `/claim buy` (Vorab-Kauf, gleiche Bedingungen wie bisher am Tisch inkl. Urkunde), `/claim create` ohne Credit →
   Hinweis auf `/claim buy`.
5. Lang de/en (neue Schlüssel, Dimensions-Tooltip „*“), Wiki `modules/simpletweaks/wiki/manual.json` (Credits,
   alle Dimensionen, `/claim credits|buy`), danach Generatoren.

## Tests (Gametests, Fabric-Adapter `ClaimsGameTest.java`)
- `claims_credit_unclaim`: Aufgeben gibt Credit, kein Material/Level zurück; Budget bleibt.
- `claims_credit_migration`: Altland (Ledger v2 ohne Budget, 6 Chunks bei Start 2) → nach Laden Budget 6, Kopie
  `.v2.json`, Marker; Aufgeben → Credit 1, neuer Claim möglich; Admin-Budget wird nach Marker nicht wieder angehoben.
- `claims_credit_dimensions`: Config-Migration alte Liste → `*`, eigene Liste bleibt, v3-Datei unverändert;
  Claim in Custom-Dimension mit `*`, abgelehnt mit eigener Liste.
- `claims_credit_buy`: Vorab-Kauf (Credits steigen um die Stufe), `buyAndClaim` atomar (Budget + Claim, Material weg),
  mit freiem Credit kostenlos, `maxClaimsPerPlayer` deckelt (SERVER_LIMIT, nichts genommen).
- Angepasst: `ExpansionTests.budget` (budgetOf ohne held).

## Verifikation (nur über `/root/sbt a-claims-credits`)
Compile `:mc26_3:fabric:compileJava` + `:modules:simpletweaks:fabric:compileJava`; Testrunner
`module-simpletweaks-fabric-263` und `module-simpletweaks-standalone-fabric-263` (Filter `simpletweaks:*claims*`),
`fabric-263` Filter `simplebuilding:*claim*`; Generatoren (Guides, Wiki, Discover) + `./gradlew check -q`.
Nicht getestet: Client (keine Client-Läufe), andere Loader/Versionen (Regel).
