# Plan b-revmixin24 (24.10.2026): REV24-M-1 / REV24-F-13, Mixin-Kompatibilitaet

Ziel: kein `@Redirect`/`@ModifyConstant` mehr auf Vanilla-Zielen, die andere Mods ebenfalls anfassen (zwei Redirects auf
dieselbe Stelle = `InjectionError` beim Start). MixinExtras ist im Projekt schon aktiv (`@WrapOperation`,
`@ModifyExpressionValue`, z. B. `PistonHandlerMixin`, `EnderiteItemMixin`). Verhalten bleibt exakt gleich: Handler
bekommt zusaetzlich `Operation<T> original`, der frueher direkt gemachte Vanilla-Aufruf geht ueber `original.call(...)`.

## Stellenliste (26.3 Fabric, alle `@Redirect`/`@ModifyConstant`) und Entscheidung
| Stelle | Entscheidung |
|---|---|
| simplestorage `BowItemMixin` (use, releaseUsing), `CrossbowItemMixin` (use, tryLoadProjectiles), `AbstractArrowPickupMixin`, `FallingStorageDropMixin` | WrapOperation (M-1) |
| common `BowItemMixin`, `CrossbowItemMixin`, `AbstractArrowPickupMixin` (Kern-Dubletten, auf 26.3 per AliasMixinPlugin aus, kompilieren trotzdem) | WrapOperation, gleich gehalten |
| simpleriding `RidingHelperMixin` (`ItemStack.get`), `RidingPacketMixin` (`onGround`) | WrapOperation (M-1) |
| common `AbstractArrowRecoveryMixin` | WrapOperation, ruft `original` statt Invoker (F-13); `AbstractArrowAccessor` bleibt (nur Json-Eintrag) |
| common `PistonHandlerMixin` `@ModifyConstant 12` | `@ModifyExpressionValue(CONSTANT intValue=12)` (F-13) |
| common `HudContextualBarMixin` (2x), `HudExperienceLevelMixin` | WrapOperation (F-13), Client-Mixins |
| gleiches Muster, gleiche Risikoklasse: simpleinterfaces `ContainerScreenLinkedMixin`, `KeyboardLinkedMixin`, `BundleTooltipViewMixin` (ModifyConstant 12), simplerespawn `MouseHandlerMixin`, simpleweather `VisualRainMixin`, simpleenddragonfight `EnderDragonMixin` | mit umgebaut |
| simpledimensions `DimensionTestWorldMixin` | gelassen: nur im GameTestServer, Lambda-Ziel, keine fremde Mod |

## ThreadLocal in try/finally (M-1, Koecher)
Gelassen: `usedQuiver`/`quiverProjectile`/`quiverArrow` werden beim Start jedes Schusses (checkQuiverOnStop,
quiverOnLoad) zurueckgesetzt und im RETURN-Inject geleert; ein Rest nach einer Exception wird beim naechsten Schuss
ueberschrieben, `billQuiver` bricht bei leerem Pfeil ab. Ein try/finally ginge nur ueber einen zusaetzlichen
Wrapper um die ganze Methode und wuerde Verhalten aendern.

## Verifikation
Compile 26.3 Fabric (Kern + betroffene Module), gezielte Tests fabric-263 und module-<mod>-(standalone-)fabric-263 fuer
storage, riding, enddragonfight, weather, respawn, interfaces; `./gradlew check`. Nicht testbar: Client-Mixins (kein
Client-Lauf erlaubt) - Handler-Signatur ist mechanisch (Empfaenger, Args, Operation).
