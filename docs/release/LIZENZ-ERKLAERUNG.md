# Lizenz-Entwurf: Erklärung (N3, 09.11.2026)

> **Entwurf, Rechtsprüfung durch Besitzer nötig, keine Rechtsberatung.**
> Nur Entwürfe. Bestehende `LICENSE` (CC0-1.0), `license`-Felder in `fabric.mod.json` usw. und die Webseite sind unverändert.

Dateien: `LICENSE-ENTWURF.md` (EN, Lizenztext), `CLA-ENTWURF.md` (EN, Beitragsvereinbarung).

## Ist-Stand
- `LICENSE` im Repo-Root = CC0 1.0; Mod-Metadaten `"license": "CC0-1.0"`, Autor `Ouzzi`.
- Einzelne Modul-Docs nennen ebenfalls CC0 (Quellprojekte).

## Was erlaubt ist (Entwurf)
Spielen (auch auf Servern), Quellcode lesen, Aufnahme in Modpacks (auch öffentliche) wenn der Download über Modrinth/CurseForge/offizielle API läuft, private Änderungen, Videos/Screenshots.

## Was verboten ist
Jars oder Code neu hochladen/spiegeln, verkaufen, eigene Download-Seiten, Forks/Ports/abgeleitete Veröffentlichungen ohne schriftliche Erlaubnis, Urheberhinweise entfernen.

## Warum eine CLA
Ohne CLA gilt jeder fremde PR nur unter der Lizenz, unter der er eingereicht wurde; ein späterer Lizenzwechsel (wie hier CC0 -> All Rights Reserved) wäre ohne Zustimmung aller Beitragenden nicht möglich. Die CLA lässt das Urheberrecht beim Beitragenden und sichert dem Autor das Relizenzierungsrecht.

## Wichtig: CC0 ist nicht rückholbar
Bereits unter CC0 veröffentlichte Versionen bleiben CC0. Die neue Lizenz gilt nur für neue Versionen ab Stichtag. Auch fremde Forks alter Stände bleiben legal. Der Entwurf stellt das in §0 klar.

## Offene Punkte (Besitzer)
1. **Stichtag/Version X** festlegen (Platzhalter in §0); Repo-Stand vor/nach Stichtag per Tag markieren.
2. **Plattformregeln prüfen:** Modrinth (Lizenzfeld, "All Rights Reserved" möglich, Modpack-Regeln), CurseForge (Distribution-/Modpack-Berechtigungen, Third-Party-Download-Regel). Passen §1.3/§2.3?
3. **Name/Jahr/Kontakt** einsetzen (Autor derzeit nur "Ouzzi" in den Metadaten; Rechtsname?).
4. **Rechtsprüfung** (insb. deutsches Recht: Haftungsausschluss §6, Moralrechte, Wirksamkeit CLA/unwiderrufliche Rechte, Minderjährige als Beitragende).
5. Mixed-Quellen: Module aus Quellprojekten mit eigenen Lizenzangaben (z. B. docs/modules/simpletweaks.md: README Apache 2.0 vs. CC0, `mr_echo_compass` AGPL-3.0) klären, bevor relizenziert wird; Fremd-Beiträge bisher? (CLA rückwirkend nicht möglich).
6. Umsetzung nach Freigabe: `LICENSE` ersetzen, `license`-Felder (Fabric/NeoForge/Forge-Metadaten, Plattformseiten) auf `LicenseRef-All-Rights-Reserved` o. ä., `CLA.md` + PR-Vorlage mit Checkbox, Webseite/LAND11-Downloadtexte anpassen.
