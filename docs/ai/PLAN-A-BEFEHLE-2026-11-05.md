# Plan A: Befehle mit Mod-Präfix (IG31-44)
1. Inventur und Tabelle: docs/ai/BEFEHLE-PRAEFIX-2026-11-05.md.
2. simplelib `CommandAliases` (nested/contents/verify) + Lang `commands.simplelib.moved` EN/DE.
3. Umbenennen: killboats/killcarts, resonance, music, servershop, simpleopt, crawl. simpletweaks hat schon `/simpletweaks`-Zwillinge.
4. Wiki/Handbücher/Lang/Doku: Text-Ersetzung der alten Namen, Generatoren auf sb-test.
5. Gametests je Modul + Kern. Ein sbt-Lauf: Compile, Modul-Gametests, Kern-Filter, check.
Risiken: Wurzel-Sperre (movement: Sperre je Unterbefehl), Mod-Wurzel aus dem Config-Befehl existiert schon (contents legt die Sperre auf jeden Zweig), Alias-Hinweis nur bei Redirect/Handler.
Rückfrage: sbtestcentre bleibt (siehe Tabelle).
