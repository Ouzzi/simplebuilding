# Bericht claude-claimdeed3 (Claim-Urkunde Phase 3: Claim-Tisch)
Branch `claude-claimdeed3` (Basis claude-wave1 mit Phase 1+2). Plan: Abschnitt „Phase 3 Umsetzung“ in `docs/ai/PLAN-CLAIM-URKUNDE-GUI-2026-10-13.md`.
- Neu: Block `simpletweaks:claim_table` (Rezept Papier+Urkunde / 4 Bretter, Axt, Loot), Pakete `TableRequest/Open/Area/List`, `TableService` (Server), `TableScreen` (Karte 16/32/64 Zellen, Ziehen, Liste 50/Seite, „Nur meine“), `ClaimGrid`, `TableLayout`, `TableVisibility`.
- Config `claimVisibility` (all Standard, owners_hidden, own, off; OP sieht alles); Mitglieder/Codes/UUIDs fremder Länder werden nie gesendet.
- Server prüft alles: Sitzung = zuletzt geöffneter Tisch, Block noch da, <= 8 Blöcke, Dimension des Tisches, w,h <= 64, <= 64 Chunks vom Tisch, >= 5 Ticks Abstand.
- Texturen neu (`tools/textures/claim_table_2026_10_15.py`, Vorschau `docs/ai/previews/claude-claimdeed3/claim_table_faces_16x.png`); keine abgenommene Textur geändert.
- Tests: JUnit `TableLayoutTest` (Layout, Raster, RLE, Farben, Sichtbarkeit); Gametests `claims_table_basics`, `claims_table_service` (alle Modi + Ablehnungen). Ergebnis: JUnit `:modules:simpletweaks:fabric:test` grün; `simpletweaks:claims*` Fabric 62/62, NeoForge 62/62, Forge 62/62 (Forge im Einzellauf; der Dreier-Lauf meldete 124/124 ohne Forge-Zeile) alles grün.
- Checks lokal: wiki --check --strict (nur alte Sandwich-Platzhalter, ohne Client-Jar), module_guides, check_tooltip_text, discover, generate_textures --check grün.
- Abweichungen: kein Reiter „Erweitern“/Budget (Phase 4/5 laut Auftrag); „Menü offen“ = serverseitige Sitzung + 8-Block-Reichweite (kein Container-Menü); Dimensionen in der Liste nur als Anzahl; Farbe je Besitzer aus UUID-Hash.
- Nicht getestet: Client-Sicht/Screen am echten Client, Forge-Laufzeit, 26.2.
- Offen: Merge nach claude-wave1, Client-Abnahme (Karte, Zoom, Ziehen), Besitzer-Abnahme Textur, Phase 4 (Maps-Ebene), Phase 5 (Erweiterung/Budget).
