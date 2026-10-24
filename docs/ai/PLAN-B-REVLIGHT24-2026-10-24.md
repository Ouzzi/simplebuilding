# Plan b-revlight24 (REV24-M-5 Teil a)

Ist: TrimLight (simpletrims) und DynamicLightHandler (Kern) setzen Vanilla `minecraft:light`; Position nur in Laufzeit-Map bzw. Entity-NBT (Halter). Nach Absturz bleiben Spieler-Lichter stehen. Keine Marke vorhanden, kein eigener Block.

Loesung (ohne Welt-Scan, ohne neuen Block): `simplelib/api/OwnedLights` = SavedData je Dimension mit den Positionen aller von Mods gesetzten Lichter (mark beim Setzen, unmark beim Entfernen). Beim Chunk-Laden (Fabric CHUNK_LOAD) entfernt `sweep` markierte Lichtbloecke, die kein lebender Traeger beansprucht (`claim`: Spieler-Tabelle); Halter (Ruestungsstaender, Mobs, Rahmen) setzen ihr Licht binnen 10 Ticks neu. Nie markiertes `minecraft:light` (Spieler/Befehl) wird nicht angefasst; Marken ohne Lichtblock werden verworfen. Kein neuer Block, daher keine Generatoren/Testzentrale-Aenderung.

Dateien: OwnedLights.java (neu), SimpleLibFabric (Hook), TrimLight (mark/unmark/claim), TrimsTests `orphan_light_swept` + Fabric-Adapter.
Kern-DynamicLightHandler bewusst NICHT angebunden: common/shared kompiliert ohne simplelib (26.2 / Kern allein), Abhaengigkeit waere ein Architekturbruch; offen, falls der Kern-Pfad (Simplebuilding.java) noch Spieler-Lichter setzt.
Risiken: Marken nur bis zum letzten Autosave gesichert (Chunks und Marken werden gemeinsam gespeichert, Restluecke klein); bestehendes Verhalten, ein fremdes Licht an Kopfposition zu uebernehmen (placeOrUpdate), bleibt unveraendert (nicht Teil von M-5a).
Pruefung: module-simpletrims(+standalone)-fabric-263, fabric-263 filter light/trim, check.
