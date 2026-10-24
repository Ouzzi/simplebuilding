# Plan b-corelight24 (24.10.2026): REV24-M-5a Rest, Kern-DynamicLightHandler

## Ist-Zustand
- `DynamicLightHandler` (common/src/shared, Aufrufer: Simplebuilding.java Server-Tick/Disconnect, NeoForgeGameplayEvents, LivingEntityMixin, EntityLightRemovalMixin, ItemFrame-Mixin) setzt Licht nur ueber `lightLevelFor(wornEmission(..))`.
- `wornEmission` und `ItemFrame`-Pfad lesen `GlowingTrimUtils.getEmissionLevel`; dieses liefert auf 26.3 immer 0, weil `McVersion.TRIMS_IN_SB = false` (mc26_3/overlay/.../McVersion.java:111). Radiance lebt dort in Simple Trims (`TrimLight`, bereits an `OwnedLights` angebunden).
- Folge: der Kern setzt auf 26.3 nie ein Licht (placeOrUpdate wird nur bei Level > 0 gerufen, removeLight nur fuer selbst gemerkte Positionen). Keine verwaisten Lichter nach Absturz, kein Risiko fuer fremde minecraft:light-Bloecke.
- Live nur im 26.2-Zweig (TRIMS_IN_SB = true); der ist laut Besitzer-Vorgabe 22.10. ausser Scope, und `common` hat keine simplelib-Abhaengigkeit.

## Entscheidung
Nichts bauen (Auftrag: Handler auf 26.3 nicht live -> nur dokumentieren). Anbindung an OwnedLights waere beim 26.2-Port faellig (dort Trims ohnehin spaeter nach simpletrims). Queue-Zeile REV24-M-5a schliessen, Hinweis in der Zeile.

## Verifikation
Code-Lesen (siehe oben); keine Codeaenderung, daher keine Gametests/Gates noetig.
