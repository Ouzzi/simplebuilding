# Plan a-freelook05 (IG31-43 Free Look Fehler)
Ist: FreeLook.tick lässt den Offset tickweise mit *0.66 abklingen (ruckelt, Yaw-Offset unnormiert -> langer Weg), Fadenkreuz nutzt Tick-Pitch ohne partialTick.
Plan: Rückdreh-Easing zeitbasiert (200 ms, cubic ease-out), Yaw-Ziel über FreeLookRules.shortestDelta (wrapDegrees), Auswertung in yawOffset()/pitchOffset() pro Frame; Mausdrehen/neues Aktivieren bricht das Easing ab; PLAYER-Modus unverändert. Fadenkreuz mit getViewXRot(partialTick) statt Tick-Pitch (kein Ruckeln/Shift). Körper bleibt unberührt (Turn-Mixin).
Abweichung: Option "Real Crosshair" (QOL34) bleibt, wird nur ruckelfrei gemacht (nicht fest Mitte).
Test: Gametest free_look_ease_back (170 -> -170 = +20), simplelib fabric+standalone, check, checkClientMixins.
