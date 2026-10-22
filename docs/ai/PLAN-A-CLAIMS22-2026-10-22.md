# Plan a-claims22: Konzept Claims-Umbau SP22-8 (22.10.2026)

Auftrag: nur Konzept (kein Spielcode), nur MC 26.3 Fabric.
1. Ist-Stand erheben: simpletweaks claims/deed/table (Budget, Kostenkurve, Maps-Ebene `ClaimMapLayer`), simplemaps
   (Wayfinder-Ebenen, Kartentisch-Mixin-Hüllen), Modulgrenzen (`modules.json`, PRINZIPIEN-MODUL-UNABHAENGIGKEIT).
2. Konzept `docs/ai/KONZEPT-CLAIMS-UMBAU-2026-10-22.md`: Ist→Soll, Datenmodell (Credits = Budget − gehalten,
   Dimensionen `*`, Chunk-Bilder), UI-Skizzen, Migration (Ledger, Config, Alt-Tische), Modulgrenzen, 4 Branches, Fragen C1…
3. Mockup `/root/sb-tools-a/previews/claims22-preview.html` (außerhalb des Repos).
4. Queue SP22-8 „Konzept fertig, wartet auf Besitzer“, Memory-Abschnitt, Push nur a-claims22.
Verifikation: Doku-Abgleich gegen Code (Pfade/Klassen existieren); kein Gradle nötig (nur Markdown).
