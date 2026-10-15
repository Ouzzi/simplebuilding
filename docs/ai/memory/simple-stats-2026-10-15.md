---
name: Simple Stats (Branch oc-simplestats)
description: Stand und offene Punkte des Moduls simplestats (Item-Statistiken), Konzept docs/ai/KONZEPT-ITEM-STATISTIKEN-2026-10-15.md Abschnitt 14
type: project
---
Modul `modules/simplestats` (26.3 Fabric/NeoForge/Forge) ist auf Branch `oc-simplestats` gebaut: Komponente `simplestats:item_stats`, Hooks per Mixin (shared), Tooltip, Statistik-Bildschirm (Taste I), `/simplestats reset|show|reload`, Handbuch, Wiki.
Entscheidungen: Byte-Budget (32 KB, `maxBytes`) statt Top-N, Rest als „weitere“; kein Cloth-Config-Bildschirm (nur `config/simplestats.json`); Stat-Id `uid` am Item, damit Pfeil-Kopien den echten Bogen finden.
**Offen (Stand Abschluss):**
- `development` → Production ziehen (Prod-Push erst nach Freigabe des Besitzers).
- Client nicht im Spiel geprüft: Screenshots des Statistik-Bildschirms (`ModuleClientSmoke`), Tooltip, Taste I, keine Re-Equip-Animation bei Zählerwechsel (`ignoreSwapAnimation` gesetzt, nicht beobachtet).
- Tragezeit wird alle 10 s übergeben: beim Rüstungswechsel innerhalb von 10 s kann Zeit beim neuen Teil landen (Konzept: Start-/Endstempel); Verbesserung nur mit Hook auf Inventar-Slotwechsel.
- Mob-Varianten-Namen (Katzenart, Papagei …) sind englisch (nur Farben/Baby/Geladen übersetzt).
- Kein Config-Bildschirm / ModMenu-Eintrag; Server-Config nur Datei.
- Port-Run (26.2, 1.21.11, 26.4) steht aus; Mixin-Ziele (`die`, `hurtServer`, `getDamageAfter*`, `useOn`, `interactOn`) dort neu prüfen.
- Vorhandene Texturen anderer Module wurden nicht angefasst; `module_guide_books_2026_10_05.py` schreibt beim Lauf 8 fremde Handbuch-Texturen anders (Altlast), deshalb nur die neue übernommen.
