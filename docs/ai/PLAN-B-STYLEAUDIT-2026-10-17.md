# Plan B-STYLEAUDIT (17.10.2026) - Stil-Audit der Module

Branch `b-styleaudit` (Basis claude-wave1 92ffa4fd7). Queue-Punkt "Andere Mods an SimpleBuilding-Stil angleichen".

## Ist-Zustand
Frühere Audits (`AUDIT-MODULE-2026-10-11.md`, `AUDIT-DATEN-2026-10-11.md`) decken Funktion, Unabhängigkeit, Daten ab; ein
Stil-Audit fehlt. Werkzeuge vorhanden: `tools/audit_data.py` (lang/names), `tools/audit_modules_checks.py`, `modules/*/tools/check_data.py`.

## Vorgehen
1. Audit per Skript über alle 34 Module + SB: Metadaten (fabric.mod.json, neoforge.mods.toml, mods.toml), Lang (EN/DE-Parität,
   britisches Englisch, ASCII-Umlaute im Deutschen, Schlüssel-Schema, Kreativtab-Titel), UI (simplelib-Bausteine, Kontrast
   >= 4,5:1), Registrierung/Einstiegsklassen, Guide/Wiki/Questionary, Kreativtab-Schalter.
2. Kleine, eindeutige Angleichungen sofort (keine Gameplay-/Textur-Änderung): fehlende Metadaten (Beschreibung, Autor, Kontakt,
   Lizenz), Vorlage `tools/templates/module`, EN auf amerikanisch, DE mit echten Umlauten (Lang + Wiki-Handbücher),
   Kreativtab-Titelschlüssel `itemgroup.<mod>.<name>`, Kontrast im Todesvorrat-Fenster.
3. Größere Punkte (Umbenennungen, UI auf simplelib umstellen = neue Abhängigkeit, Tab-Schalter, Icons) nur im Audit und in der Memory.

## Prüfung
Compile 3 Loader + `:compileJava`, Modul-Targets Fabric+NeoForge der berührten Module, `check_data.py`, `wiki/generate.py --all`
(auf sb-test erzeugen), `audit_data.py`, Python-Unittests.
