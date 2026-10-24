# Plan b-octhammer24 (24.10.): Achtel-Zelle mit Hammer 1,5x langsamer als der Vollblock

Ursache: `MaterialOctetBlock#getDestroyProgress` fragt `sourceState().getDestroyProgress(...)`. Der Vorschlaghammer-Mixin
(`HammerMiningSpeedMixin`, RETURN von `BlockStateBase#getDestroyProgress`) teilt dadurch zweimal durch den Faktor
(innen Vollblock-Zustand, aussen Zelle): 1,5^2 statt 1,5 -> Zelle 1,5x langsamer. Spitzhacke: Faktor 1, keine Abweichung.

Aenderung (klein): Mixin wendet den Faktor nur an, wenn der Zustand wirklich bei `pos` steht
(`level.getBlockState(pos) == this`); der delegierte Vollblock-Aufruf wird uebersprungen.
Test: `cornerBreakNeedsTheToolAndCreativeDropsNothing` um Vergleich Steinhammer Zelle vs. Diamantblock erweitert
(laeuft ueber module-simpleoctets-fabric-263, Hammer nur wenn geladen). Verifikation: compile, Modultests, `./gradlew check`.

Nachtrag (Testlauf 1 rot: Zelle 0.00107 vs Block 0.0016 mit Steinhammer): zweite Ursache. `SledgehammerUtils.shouldBreak`
prueft das Werkzeug am Zellen-Zustand (traegt kein needs_iron) -> Zelle zaehlt als abbaubar (Faktor 1,5), der Diamantblock
nicht (Steinhammer, Faktor 1). Fix: Ziel- und Ursprungs-Zustand einer Zelle gelten als ihr Vollblock (`OctetCells.baseOf`, SimpleLib).
