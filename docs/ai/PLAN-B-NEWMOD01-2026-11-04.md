# Plan B-NEWMOD01 (04.11.2026): `tools/newmod.py` legt ein gate-festes Modul an (B2)

Ist: `tools/newmod.py <id> "Name"` schrieb Gerüst (3 Loader) + modules.json + enabled-mods.json; Sub-Mod-Felder, Preis-Tabelle,
Icon, FAMILY, Lang-Schalter, Sync und Modtree blieben von Hand (Gates bemängelten sie in den Bündeln 98/99).
Ausbau (Entscheidungen): CLI `newmod.py <id> --name … [--parent simplebuilding --bundle --config-switch --loaders --enderite]`,
Standard nur Fabric 26.3 (Besitzer 22.10.); `create(...)` bleibt rückwärtskompatibel (3 Loader, ohne Verdrahtung), `do_wire=True`
im CLI. Verdrahtet: simplemoney-Preis-Tabelle, Platzhalter-Icon (+BG) nur für das neue Modul, TcContext.FAMILY, ModTags
ENDERITE_NAMESPACES, SB requires/optional, Schalter-Lang EN/DE, sync.py, modtree. Als Checkliste (nicht automatisch): Guide-Buch
(braucht Registrierung), Config/ConfigCommands, InWorldUpgrades.registerDefaults, Kreativtab, echte Preise, Texturen.
Verifikation: Unit-Test `tools/tests/test_newmod.py` (temp-Kopie), Probe-Modul `simpleprobe` im Worktree (nicht committet):
precheck grün, Compile + Modul-/Standalone-Targets auf sb-test grün, danach Probe entfernen.
