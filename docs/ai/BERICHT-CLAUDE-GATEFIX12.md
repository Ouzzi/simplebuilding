# Bericht claude-gatefix12
- Ursache: 7 Wiki-Sheets `sheets/simplebuilding/simplecrucibles/block/enderite_{barrel_*,crucible_side,crucible_top}.png` waren eingecheckt, obwohl vanilla_derived.MODULE_OUTPUTS['simplecrucibles'] sie schon erzeugt (Platzhalter ohne Jar).
- Fix: aus dem Index entfernt (`git rm --cached`), .gitignore-Block fuer Modul-Resource, wiki/assets/textures/simplecrucibles und Sheets (je 7 Dateien).
- Zusatzfund: checkWiki war auf wave1 rot (35 simpleblocks-Bloecke ohne Prosa) -> Glob-Notizen (*_slab/_stairs/_wall, cracked_*, mossy_*, blue_nether_bricks) in modules/simpleblocks/wiki/manual.json; wiki/data/simpleblocks.* + modules.js neu.
- Geprueft (sb-test, mit Jar): generate --all => git status sauber, textures.js unveraendert (kein Neuerzeugen noetig); `gradlew :modules:simplecrucibles:fabric:processResources checkWiki` EXIT=0; generate --all --check --strict gruen.
- Tests: module-simplecrucibles fabric+neoforge 74/74 gruen; simplebuilding:data_integrity* fabric+neoforge 100/100 gruen.
- Nicht getestet: Forge, Zustand ohne Jar (Platzhalter) auf sb-test.
- Offen: Prod-Uebernahme (development -> Production) nach Freigabe.
