# Plan: Tooltips, 2026-10-02

## Ist-Zustand und Entscheidung

- Branch `claude-gpt-tooltips`; ausschließlich dieser Worktree, kein Push und kein Clientstart.
- Item-Tooltips bestehen aus einzelnen `Component.translatable`-Zeilen in `appendHoverText`, `InfoTooltips` und `ItemMixin`. Es gibt keinen allgemeinen Parser für nummerierte Tooltip-Keys oder eingebettete Zeilenumbrüche.
- `ReiText.wrap` verwendet bereits den Minecraft-Font-Splitter, ist aber auf REI beschränkt. Config- und Fortschrittsanzeigen haben eigene Umbruchpfade. Bloße JSON-Newlines sind für Item-Tooltips daher keine Lösung.
- Bestehendes Item-Muster beibehalten: kurze Übersetzungen und zusätzliche übersetzte Komponenten. Kein neuer Renderer, keine Gameplay-Änderung.
- Inventur aller EN/DE-Keys mit `tooltip` (auch `@Tooltip`) oder `desc`, einschließlich Hauptmod-Overlay und Module. Bereits automatisch umbrochene Beschreibungen separat dokumentieren.

## Umsetzung

1. Vorher/Nachher-Inventur in `docs/ai/TOOLTIPS-2026-10-02.md`.
2. Lange Item-Texte knapp formulieren; nötige Fortsetzungen als weitere Komponenten, EN/DE identisch strukturiert. Hauptmod in beiden Ressourcenverzeichnissen, Module in ihren gemeinsamen Ressourcen (keine separaten 26.3-Lang-Overlays vorhanden).
3. Config-Texte über deren vorhandenen Font-Umbruch verteilen; numerische Werte, Platzhalter und Warnungen erhalten. Nicht als Tooltip verwendete `description`-Treffer kennzeichnen.
4. Bestehende textabhängige Tests prüfen und erforderliche Erwartungen anpassen.

## Risiken und Prüfung

- Platzhalter-Reihenfolge, bedingte Tooltip-Zeilen, Übersetzungsstile und dynamisch gebildete Keys erhalten. Keine Informationen durch pauschales Abschneiden verlieren.
- JSON auf Syntax, doppelte Keys, EN/DE-Struktur und Spiegelung prüfen.
- Wiki mit vorhandenem venv-Python `wiki/generate.py --all` generieren; mit uv-Python `--all --check` prüfen.
- Bei Java-Änderungen Fabric-263 und NeoForge-263 Server-Suiten; Ergebniszeilen lesen. Shared/26.2 und Forge-263 kompilieren; vollständiges `check` im aktuellen Worktree.
- Kein visueller Clienttest; Testzentrale der Besitzerwelt bleibt unberührt.
- Nur gezielt eigene Dateien committen; Commit-Trailer wie beauftragt.

## Umgebung

RTK und rg sind im Sandbox-PATH nicht verfügbar. Native Git-Suche als Ersatz. Die vorhandenen Python-Interpreter benötigen wegen des blockierten uv-Trampolins einen eskalierten Start; Arbeitsdaten bleiben im Worktree.

## Plan-Abgleich während der Umsetzung

- Zusätzlicher belegter Breitenfall: `InfoTooltips.coreLines` kombinierte drei Item-Namen pro Zeile. Jetzt steht jeder Name in einer vorhandenen separaten Komponente; der bestehende Kern-Test prüft dies.
- `tools/check_tooltip_text.py` macht JSON-Duplikatprüfung, Tooltip-Zeilenlimit (48 Zeichen als Toleranz für ungefähr 45), EN/DE-Keys und Hauptmod-Spiegelung reproduzierbar. Es prüft ausschließlich versionierte Lang-Dateien, keine Build-Kopien.
- Defaultangaben bleiben beim Config-Umbruch zusammen. Die bestehenden SimpleModels-/SimpleTweaks-Datenchecks und ihr Default-Vertrag wurden nicht abgeschwächt.
- `ImmersionTests`, `MagnetTests` und `OreDetectorTests` erhalten die neuen Text-/Zeilenerwartungen; `MoneyTests.languagesAndAssets` prüft die tatsächlich erzeugten Komponenten beider betroffenen Items.
- Die vorgeschriebene Wiki-Generierung holt bereits im Ausgangsstand vorhandene, aber noch nicht exportierte Daten nach (unter anderem seltene Strukturfunde und 24.000-Tick-Geldscheinrezept). Die zugrunde liegenden Gameplay-/Rezeptdateien wurden in diesem Run nicht geändert.
- Gate-Abweichung: `testWikiModules` meldete nach dieser Generierung den fehlenden Familien-Eintrag für `polished_ender_quartz_checker`. Der vorhandene Registry-Eintrag und das 26.3-Rezept belegen die Variante und 4 Stück aus je 2 Materialien. Der fehlende EN/DE-Eintrag sowie die dadurch veraltete Zahl „sieben“ in `wiki/manual.json` wurden korrigiert; keine Testabschwächung und keine Rezeptänderung.
