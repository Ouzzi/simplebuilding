# Bericht claude-shops2 (Simple Shops Umbau, N66)
Branch claude-shops2 (Basis claude-wave1), Plan: KONZEPT-SHOP-VITRINE Abschnitt 9. Merge/Gate offen.
- Einnahmen 27 Slots (Verkaufsblock). Neuer Block sales_storage (EN Sales Storage / DE Verkaufslager): kein Container, Schutz wie Shop-Blöcke, zieht alle 8 Ticks aus dem Block darüber, Kette nach unten, nur bei gleichem Besitzer, max. 16 Lager; Kauf zahlt in die ganze Kette.
- Einnahmen voll (Preis passt nirgends hinein): Knopf aus + "Einnahmen voll", serverseitig EARNINGS_FULL.
- UI: Vitrine allein / Verkaufsblock allein / Lager eigene Fenster; verbunden = eine Ansicht (Ware, Preis, 27 Einnahmen, Höhe 222 = Doppeltruhe); alle Fenster Kasten + Naht (simplelib UiBoxes, Palette BARREL).
- Knopf "Developer: Käuferansicht" (nur Besitzer, Server prüft) + "Dev: Besitzeransicht" zurück. Testkauf erlaubt: gleicher ShopTrade-Pfad, Summe aller Items bleibt gleich (Test). OPs sehen auf fremden Shops nur die Käuferansicht.
- Textur: tools/textures/simple_shops_storage_2026_10_15.py, Vorschau docs/ai/previews/claude-shops2/sales_storage_8x.png.
- Tests (sbt, Commit f975d5e55): module-simpleshops fabric/neoforge/forge + standalone fabric/neoforge: "alles gruen: 260/260" (52 je Ziel, 14 neue Missbrauchs-GameTests, u. a. Fuzz, Besitzerwechsel, Explosion/Kolben/Trichter, Save/Load-Kette); JUnit ShopLayoutTest grün; check_data, Texturen, wiki --check grün. Compile 3 Loader + Gametest EXIT=0.
- NICHT getestet: Client-Screenshot (xvfb ohne GLX auf sb-test: "Couldn't find matching GLX visual", Client-Smoke 0/7, Smoke-Code für neue Screens ist ergänzt, Layout/Dev-Knopf-Optik ungeprüft); Kette >16 (Arena 12 hoch); echtes Chunk-Entladen; Jade/JEI.
- Offen: Merge nach claude-wave1, Client-Abnahme der UIs.
