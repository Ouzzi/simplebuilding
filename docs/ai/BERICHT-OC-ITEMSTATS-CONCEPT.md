# Bericht oc-itemstats-concept (2026-10-15)
- Nur Konzept, kein Code: `docs/ai/KONZEPT-ITEM-STATISTIKEN-2026-10-15.md` (171 Zeilen); Queue und Texturen unverändert, kein Gate nötig (nur Doku).
- Recherche: Modrinth-API zu „Item Stat Tracker“ (Slug `itemstats`, Autor Potota, ARR-Lizenz, Fabric+NeoForge, Client+Server nötig). Quelltext auf tangled.org **nicht gelesen** (Lizenz); Speicherformat öffentlich nur als Item-Komponente „stats“ belegt, Feldaufbau/Limits unbekannt – im Konzept so markiert.
- Schwächen des Vorbilds teils Schlussfolgerung (keine Größenbegrenzung/Reset/Config dokumentiert) – als solche gekennzeichnet.
- Ist-Zustand per grep: ein Hook auf `ItemStack#mineBlock` deckt auch den Hammer-Flächenabbau (`SledgehammerUsageEvent` → `destroyBlock`); Tooltip-Mixin-Muster aus Simple Tools; Kreativ ruft `mineBlock` nicht auf.
- Entscheidungen: Komponente `<modul>:item_stats` mit Versionsfeld, Top-N (12, max 32) + „Sonstige“ (Invariante Σ=Gesamt), keine Komponente ohne Aktivität, gebündelte Schreibvorgänge statt Tick-Polling, Tooltip 2 Zeilen + Shift-Details, Reset per Befehl + Werkbank (Item + Tintenbeutel).
- Modul: Empfehlung neues Sub-Mod `simplestats` (nicht Kern, nicht Simple Tools, nicht Pflicht in QoL); Zuordnung nach Tag/Komponente, keine Klassenimporte.
- Risiken benannt: Re-Equip-Animation bei Komponentenänderung, Näherung bei „Schaden absorbiert“, Sichtbarkeit nur kosmetisch steuerbar. 12 Besitzerfragen mit Empfehlung (Abschnitt 13).
- Nicht getestet: alles Spielverhalten (Hooks, Animation, Komponentengröße) – im Konzept als per Test zu belegen markiert.
