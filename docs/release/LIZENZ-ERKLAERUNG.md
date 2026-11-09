# Lizenz: Erklärung (aktiv seit b-lic15, 09.11.2026)

> **Rechtliche Prüfung durch den Besitzer bleibt empfohlen; keine Rechtsberatung.**
> Verbindlich ist nur der englische Text in `LICENSE` (und `CLA.md`). Diese Seite erklärt ihn auf Deutsch.

Besitzer-Entscheidung 09.11.2026: „All Rights Reserved, source-available“ + Contributor License Agreement (CLA).

## Was gilt
- `LICENSE` (Repo-Wurzel, gleiche Kopie in Modulordnern mit eigener LICENSE, in jedem Jar als `LICENSE_<jarname>`):
  „Simple Mods License – All Rights Reserved, Source-Available“, SPDX-Kennung `LicenseRef-SimpleMods-ARR`.
- Alle Mod-Metadaten (`fabric.mod.json`, `neoforge.mods.toml`, `mods.toml`, Vorlagen in `tools/templates/module`):
  `license = "LicenseRef-SimpleMods-ARR"`. `tools/multimod.py` prüft das (kein „CC0“ mehr in Metadaten).
- Beiträge: `CONTRIBUTING.md` + `CLA.md`; GitHub-Prüfung `.github/workflows/cla.yml` (Signaturen auf Branch `cla-signatures`).

## Erlaubt
Spielen (auch auf Servern, auch mit Einnahmen), Quellcode lesen, Aufnahme in Modpacks (auch öffentliche), wenn die Dateien
über Modrinth/CurseForge bzw. deren API geladen werden und das Projekt genannt/verlinkt ist, private Änderungen für sich
selbst, Videos/Streams/Screenshots.

## Verboten (ohne schriftliche Erlaubnis)
Jars oder Code neu hochladen/spiegeln, verkaufen/unterlizenzieren, eigene Download-Seiten (außer offiziellen Plattformen und
den Seiten des Autors, z. B. simplemods.de), Forks/Ports/abgeleitete Veröffentlichungen (auch Texturen, Modelle, Klänge),
Urheber-/Lizenzhinweise entfernen, als eigenes Werk ausgeben.

## Stichtag: CC0 bleibt für alte Versionen
Der Stichtag steht in `LICENSE` als Feld „Effective date: the date of the first release containing this file“ – also der
Gate-Push/Release, mit dem diese Datei zum ersten Mal veröffentlicht wird. Alle früher veröffentlichten Versionen bleiben
CC0 1.0 (nicht widerrufbar); auch Forks alter Stände bleiben legal. Empfehlung: den letzten CC0-Stand per Git-Tag markieren
(z. B. `last-cc0`) und das Datum nach dem Release in `LICENSE` eintragen, wenn der Besitzer ein festes Datum möchte.

## Warum eine CLA
Ohne CLA gilt ein fremder Pull Request nur unter der Lizenz, unter der er eingereicht wurde; ein späterer Lizenzwechsel
wäre ohne Zustimmung aller Beitragenden nicht möglich. Die CLA lässt das Urheberrecht beim Beitragenden und sichert dem Autor
das Recht, Beiträge zu nutzen und neu zu lizenzieren. Zustimmung per PR-Kommentar
„I have read the CLA Document and I hereby sign the CLA“ (der Workflow speichert sie).

## Offene Punkte (Besitzer)
1. **Rechtsprüfung** (deutsches Recht: Haftungsausschluss §6, Moralrechte, Wirksamkeit der CLA, Minderjährige als Beitragende).
2. **Name/Kontakt:** eingesetzt sind „Ouzzi“ und `https://azarfane.de/` / GitHub-Issues (aus `fabric.mod.json`). Rechtsname oder
   E-Mail ergänzen, falls gewünscht.
3. **GitHub einrichten:** Branch `cla-signatures` anlegen, Workflow-Rechte „Read and write“ erlauben (siehe Kopf von `cla.yml`).
4. **Plattformen:** Lizenzfeld auf Modrinth/CurseForge auf „Custom/All Rights Reserved“ mit Link auf `LICENSE` stellen;
   Modpack-/Third-Party-Download-Regeln von CurseForge mit §1.3/§2.3 abgleichen.
5. **Fremdquellen:** Module aus Quellprojekten mit eigenen Lizenzangaben (z. B. `docs/modules/simpletweaks.md`: README nannte
   Apache 2.0; `mr_echo_compass` AGPL-3.0) vor dem Release prüfen; Fremdbeiträge vor der CLA sind nicht rückwirkend erfasst.

## Was bewusst unverändert blieb
Historische Berichte/Pläne/Audits (`docs/ai/*`, `modules/*/AUDIT.md`, `docs/modules/*.md`) beschreiben den damaligen CC0-Stand;
`audit/*-inventory.json` (Fremdlizenzen), gemeinfreie Musikbeispiele in Simple Music („public domain“ = Werke, nicht unsere Lizenz).
