# Simple Riding producer data

`options.json` maps the six preserved JSON config paths to bilingual names, tooltips,
defaults, tabs, and numeric bounds. Runtime source: `RidingConfig.java`.

Loot tuning is exposed by `RidingLoot.java`: named public roll/empty-weight constants
and the public `BASTION`, `FORTRESS`, and `TRIAL` weighted-book tables
(`[enchantment selector, level, weight]`; selectors 0 = Tailwind, 1 = Leaping,
2 = Protection). Trades are ordinary 26.3 data under
`modules/simpleriding/generated/resources/data/simpleriding/villager_trade/`;
prices use uniform providers, weights and second chances are explicit data.

This directory is additive. SimpleBuilding's existing balance location stays unchanged.
Per-module UI switchers and storage history belong to the separate infrastructure run.
