# Konzept Super-Mod / Sub-Mod (Besitzer, 2026-10-06)

Gilt ab jetzt für Simple QoL und künftig für alle anderen Mods der Familie.

- **Sub-Mod:** eigenständig spielbar (standalone, eigenes Standalone-Test-Target nach PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md), eigene Mod-ID, eigener Jar.
- **Super-Mod:** liegt darüber, **requires** alle ihre Sub-Mods und bündelt sie (nested wie simplelib, Regel 6a). Sie enthält nur, was nicht sinnvoll in eine Sub-Mod passt (gemeinsame Config-Oberfläche, Guide, Befehle, Übergreifendes).
- **Grundsatz:** alles, was geht, wird Sub-Mod; nur das Nötigste bleibt in der Super-Mod.
- **Config:** In der Super-Mod steht für jede Sub-Mod als oberster Punkt „Enable Simple XY“ (EN) / „Simple XY aktivieren“ (DE), damit jede Sub-Mod abgeschaltet werden kann.
- Erste Anwendung: Simple QoL wird Super-Mod; erste Sub-Mod „simplecontainers“ (Aufteilung der übrigen QoL-Teile entscheidet der Besitzer, Vorschlag in QUEUE Nachtrag 14).
