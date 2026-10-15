# Bericht oc-gatefix6
- Ursache: Die Wither-Texturen sind schon als `MODULE_OUTPUTS` registriert und verursachen die Abweichung nicht. Die Abweichung kam von `simplebuilding:item/{reinforced,netherite,enderite}_furnace_minecart`.
- Diese drei Texturen stehen in `OUTPUTS` und haben dieselben Pixel wie die der simpleovens-Texturen. Die Wiki-Texturindex-Logik führt gleiche Bytes zu einer Kopie zusammen.
- Mit Client-Jar sind die Bytes gleich und werden zusammengeführt. Als Platzhalter (CI) sind sie verschieden und liegen unter `sheets/`. Deshalb hing `textures.js` vom Jar ab.
- Fix: `SAME_AS` in `tools/textures/vanilla_derived.py` gibt den drei Platzhaltern den Namen der simpleovens-Platzhalter. `wiki/data/textures.js` ist neu erzeugt.
- Geprüft: `generate.py --all --check --strict` ist mit und ohne Jar (GRADLE_USER_HOME=/nonexistent) grün. Nach `:mc26_3:fabric:processResources` und `generateVanillaDerived` der Module ist `./gradlew checkWiki -q` grün und `git status` sauber. Die Modul-Texturen landen nur in `build/`.
- Nicht getestet: volles Gate, Tests.
