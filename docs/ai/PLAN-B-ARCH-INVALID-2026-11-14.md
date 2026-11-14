# PLAN B: arch_invalid (N44, b-fix192) – 2026-11-14

Ist: Gametest `archInvalid` erwartet fuer offenen U-Rahmen (auf dem Testboden) `portal_shape.open`, bekommt `gap`.
Ursache: `ArchPortalScanner.scan` flutet ueber den offenen Rahmen hinaus, an der Aussenseite herab und trifft den
Boden (Fremdblock) -> bricht sofort mit GAP ab, bevor die Groessengrenze (OPEN) greift. Unit-Test sah das nicht,
weil das Raster ausserhalb nur Luft hat. Spielverhalten falsch (jeder offene Rahmen auf Erde meldet "Fremdblock").
Fix (Code): Fremdblock nur vormerken, Fluten fortsetzen; erst am Ende GAP melden. Laeuft die Flut aus der
Grenze -> OPEN hat Vorrang. Geschlossene Rahmen mit Fremdblock bleiben GAP (Flut bleibt innen).
Dateien: shared/java/dev/simpledimension/common/portal/ArchPortalScanner.java, Unit-Test um Fall "offener
Rahmen auf Erdboden -> OPEN" ergaenzt.
Verifikation: ein sbt-Lauf compileJava + module-simpledimensions-fabric-263 + standalone-fabric-263.
