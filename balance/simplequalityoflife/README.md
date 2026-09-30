# Simple Quality of Life balance producer

Manifest storage path: `balance/simplequalityoflife/`. The generic Balancing-Zentrale owns runtime history and rollback files; no SimpleBuilding history is migrated or copied.

Numeric defaults and hard limits are named constants in `modules/simplequalityoflife/shared/java/com/simplequalityoflife/config/SimplequalityoflifeConfig.java`. Reach is `InteractionGuard.MAX_REACH`, packet tolerance is `ClimbSecurity.POSITION_TOLERANCE`. The actual server config preserves its original JSON keys and normalizes values at load/save/use. No recipes, trades, or custom loot exist in this source mod.
