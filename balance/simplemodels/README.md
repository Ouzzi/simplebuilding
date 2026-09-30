# Simple Models producer data

`options.json` names every server setting, translated metadata key, default, and numerical
bounds. Runtime authority is `ModelPolicy.java`, loaded from `config/simplemodels/config.json`.
`DEFAULT_LEVEL_COST` is consumed by the config default; the module-scoped constant extractor
can read it. All runtime values are clamped after loading. Server settings have no client
mutation path. The catalogue scanner limits are technical safety bounds, not economy tuning.

There are no module items, recipes, loot or trades. No currency arbitrage or duplicated
storage histories. SimpleBuilding remains at `balance/`; this directory is additive.
