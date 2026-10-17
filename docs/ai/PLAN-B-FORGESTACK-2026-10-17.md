# Plan B: Forge-Stapelkappung Fass/Truhe (17.10.)

Ist-Zustand: Der Ursprungstext des Befunds steht in keinem Audit-Dokument. Forge nutzt wie Fabric das Mixin
`TweaksItemStackMixin` (Rakete) und die loader-neutralen Pfade (`StackLimits`, `LimitedSlot`, `HopperStackLimitsMixin`
in `simplelib.mixins.json`); die Forge-eigene Mixin-Config lädt sie mit. Kein Forge-spezifischer Code für Fass/Truhe nötig.

Plan: Verhalten auf allen drei Loadern messen statt ändern; nur bei Rot fixen.
Prüfung: `simplebuilding:tiered_chest_game_test_*` (17 Tests: Slots/Stapelgrenzen je Stufe, Trichter füllen/leeren
übergroße Slots, Komparator, Speichern/Laden) und `module-simplecrucibles-*-263` (68 Tests inkl. loses Enderit-Fass
128er-Stapel, Shift-Klick, Client-Spiegel) auf fabric/neoforge/forge.

Ergebnis: alles grün, kein Befund, kein Code geändert. Die übrigen Audit-Funde (D-02/D-03 Mojibake, M-01 simplelib-Version 0.2.0)
sind im Baum bereits behoben bzw. versioniert.
