# Plan: Zentrale – Mod-Auswahl für den Spielstart + Downloads (2026-10-12)

Auftrag Besitzer 12.10. Branch `claude-zentrale-mods` (von `claude-wave1`).

## Ist-Zustand
- Zentrale = statische Seite `tools/zentrale-web/index.html` + `devqueue.py` (API `/api/`, Login über Zugangs-Header).
- Spielstart: Knöpfe `simplemods://start?ref=wave|master` → Laptop-Starter (nicht im Repo) holt den Stand und startet
  `:integration:runIntegrationClient`. Die Mod-Menge liest `integration/build.gradle` aus `integration/enabled-mods.json`
  (Hub-Muster `tools/launchhub/hub/mods.py`, Prüfung `tools/multimod.py`).
- Super-Mods nesten ihre Sub-Mods (Jar-in-Jar); Abschalten einer Sub-Mod innerhalb der Super-Mod geht nur über
  `enableSimpleX` in `config/<super-mod>.json` (SimpleBuilding, QoL, Aesthetics).
- Gate (`aitool.py gate` + Standalone) baut im Gate-Checkout nach `*/build/libs`; nichts wird aufbewahrt.

## Umsetzung
1. `modselect.py`: Baum aus `modules.json` (`parent`/`bundle`/`requires`) + geplante SB-Sub-Mods (Octets, Hammers,
   Storage, Ovens, Crucibles, gesperrt bis sie in `modules.json` stehen); Regeln: Eltern schalten Kinder, `required`
   gesperrt solange Eltern an, `requires` + genestete SimpleLib automatisch; `apply` für den Starter.
2. `devqueue.py`: `GET /api/mods`, `PUT /api/mods/selection` (je Nutzer, Datei-Name = Hash der Adresse).
3. `index.html`: Reiter „Mods“ (Suche, Filter An/Aus, Speicherstatus, Sperr-Gründe) und „Downloads“ (Loader, MC,
   Art, Stand, Suche, Leer-/Fehlerzustände); Start-Knöpfe hängen `&off=` an.
4. `collect_downloads.py` + `publish_downloads.sh`: nur gate-grüne Stände, letzte drei behalten.
5. Tests `tests/test_mods.py`; Vorschau-Harness `tests/harness.py`.

## Offen (Betrieb, nicht im Repo)
- Laptop-Starter: `off` aus der URL lesen und vor dem Gradle-Start `python tools/zentrale-web/modselect.py apply --off …` aufrufen.
- Gate-Skript: nach GRÜN `publish_downloads.sh` aufrufen; Webserver liefert `/downloads/` aus.
