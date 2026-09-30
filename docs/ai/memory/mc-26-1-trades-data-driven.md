---
name: mc-26-1-trades-data-driven
description: Villager-Trades sind ab MC 26.1 datengetrieben; Fabric TradeOfferHelper existiert nicht mehr — Registrierungsweg und Datei-Layout
metadata: 
  node_type: memory
  type: reference
  originSessionId: 14a0978a-ba3c-4cfd-8adb-1ba325dad838
  modified: 2026-08-20T14:22:31.124Z
---

In Minecraft 26.1.2 sind Villager-/Wandering-Trades vollständig datengetrieben. `net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper` ist in Fabric API 0.150.0+26.1.2 **ersatzlos entfallen** (im Modul `fabric-object-builder-api-v1` 23.0.16 gibt es keine Trade-Klassen mehr) — verifiziert im Gradle-Cache, nicht aus Doku geraten.

Aufbau (Vanilla wie Mod identisch):
- Einzel-Trade: `data/<ns>/villager_trade/<profession>/<level>/<name>.json`, Codec `VillagerTrade.CODEC`. Felder: `wants` / optional `additional_wants` (je `{id, count}`), `gives` (`{id, count, components}`), `max_uses`, `xp`, `reputation_discount` (entspricht dem alten `priceMultiplier` im TradeOffer-Konstruktor), `given_item_modifiers` (Loot-Funktionen), `merchant_predicate`, `double_trade_price_enchantments`.
- Anbindung an den Pool per Tag-Merge: `data/minecraft/tags/villager_trade/<profession>/level_<n>.json` bzw. `wandering_trader/{buying,common,uncommon}.json`. Mod-Einträge dort mit `{"id": ..., "required": false}` eintragen, damit deaktivierte/fehlende Trades die Tags nicht brechen.
- Vanilla liest diese Tags über `data/minecraft/trade_set/...` — Mods müssen die trade_set-Dateien nicht anfassen.

Verzauberte Handelsware: `EnchantmentHelper.updateEnchantments` bzw. `ItemStack.enchant` wählen die Zielkomponente automatisch (`getComponentType`: ENCHANTED_BOOK → `stored_enchantments`, sonst `enchantments`). Alle Items haben über `DataComponents.COMMON_ITEM_COMPONENTS` ein leeres `enchantments`-Default, ENCHANTED_BOOK ein leeres `stored_enchantments` — deshalb greift `updateEnchantments` und läuft nicht in den Early-Return.

Für gewichtete Auswahl (Vanilla `enchant_randomly` kann keine Gewichte/festen Level) eigene Loot-Funktion registrieren: `Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, id, MAP_CODEC)`. Config-abhängiges Laden auf Fabric via eigener Resource-Condition (`ResourceConditionType.create` + `ResourceConditions.register`, JSON-Key `fabric:load_conditions`); auf NeoForge wird dieser Key ignoriert. Siehe [[multiloader-parity-audit-2026-08]].
