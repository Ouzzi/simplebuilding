# Simple Models — 26.3 Fabric / NeoForge

## Source inventory and compatibility

Read-only source: `C:/Users/oussa/Downloads/Minecraft/Mine/custom created mods/renamed`,
initial commit `da086a2`, Fabric 1.21.11. The owner's checkout has staged/unstaged changes;
the port reads that working tree, never edits it. No README, todo file, tests, lang,
recipes, config, registry items/blocks/entities/enchantments, commands, or keybinds exist.
Eight Java files: empty initializer/datagen; unregistered client initializer; CIT resource
loader; definition record; unreachable catalogue screen; outdated render substitution;
global bracket-name hiding. The source mixes incompatible Yarn/Mojmap APIs. No verified
working source launch is claimed.

Source intent (comments): resource-pack models/textures for arbitrary item shapes, anvil
name association, collision-resistant IDs, searchable tags, GUI item previews and base→result
recipe instructions. The source's data fields are `id`, `base_item`, `match_name`, `model`,
`tags`, `author` (the loader previously ignored author). These fields remain accepted.
Bare definition IDs become `renamed:<id>`; model/base IDs retain their original namespaces.
The module id is `simplemodels`, as explicitly requested; Fabric provides the old `renamed`
mod id as an alias. No old mod registry IDs require a data fixer. Vanilla custom names remain
loadable, including `[suffix]`, but are no longer globally hidden. Optional exact legacy name
matching works at an anvil; prefix matching and hidden identity are intentionally rejected.
Existing name-only items do not silently change appearance on login: use the anvil once.

## Complete shipped feature inventory

- Server-approved model assignment and removal through a vanilla anvil, using the vanilla
  `minecraft:item_model` component. Put the approved item left; keep the second slot empty.
  The browser sends only `ServerboundRenameItemPacket("@model:<id>")`, never a stack/model
  component/upload packet. The result is a server-owned copy; taking it consumes the input
  once and charges vanilla XP, with vanilla anvil damage/sounds. Creative follows vanilla
  free XP. An entire input stack keeps its exact count. Repeated same-model changes yield
  no result. Removal restores the item's default model, not a prior third-party override.
- Inventory and anvil **Models** buttons, searchable paged browser (name/base ID/model ID,
  author/tags), base→result preview and actual vanilla 3D item rendering. No new keybind.
  Inventory browsing is read-only; assignment binds to the open anvil menu.
- Tabs: Models, Server Settings, Import Help. Settings show the authoritative server
  values and translated defaults/tooltips. Settings are deliberately read-only: admins
  edit the server JSON and reload; clients have no gameplay config mutation packet.
- Add/remove server definition files and `/simplemodels reload` without restarting;
  deterministic bounded folder scan and join/reload catalogue synchronization.
  **Reload Assets** reloads enabled resource packs so model changes appear in previews.
- Bilingual EN/DE UI/wiki, import example using existing vanilla apple artwork (no new pixel art).
- Configurable exact legacy `match_name` assignment (off by default). It changes only
  appearance; custom names/UUID suffixes are never rewritten or hidden.
- No registered items, blocks, mobs, enchantments, creative tab, recipes, loot, advancements,
  worldgen or custom gameplay stats. Block-item previews work; placed block geometry is
  outside this item-component port.

## Import and resource delivery

The authoritative server folder is `config/simplemodels/`, with `config.json` and
`catalogue/*.json` (nonrecursive). A definition:

```json
{"id":"renamed:tomato","base_item":"minecraft:apple","match_name":"Tomato",
 "model":"renamed:tomato","tags":["food","red"],"author":"Example"}
```

`model` refers to a **26.3 client item definition**, not directly to a geometry file:
`assets/renamed/items/tomato.json`:

```json
{"model":{"type":"minecraft:model","model":"renamed:item/tomato"}}
```

Put geometry in `assets/renamed/models/item/tomato.json`, PNGs in
`assets/renamed/textures/item/`. Import an old `models/item/*` file unchanged when compatible,
then add the new `items/*` wrapper. Each resource pack needs a 26.3-compatible `pack.mcmeta`;
use the format of a working 26.3 pack. The included `examples/resourcepack/assets` fragment
uses vanilla apple geometry and is copied into an existing enabled pack; it is not a
standalone pack. Open vanilla Options → Resource Packs to enable a folder pack, then
Reload Assets (or F3+T). Add/remove definition files on the server and run the admin command.

Multiplayer: admins publish a trusted ZIP via Minecraft's standard `server.properties`
`resource-pack`, `resource-pack-sha1`, and `require-resource-pack=true` (or the vanilla
server pack push API supplied by hosting software). Catalogue metadata is synchronized by
this mod; rendering resources use vanilla's consent/download/hash pipeline. No custom
HTTP downloader, remote URL importer, filesystem upload or archive extraction is added.
Private server directories are never published. Missing pack resources use vanilla missing
model fallback and do not grant gameplay effects. Local-only resources are previews, not
authority: only the server catalogue can be assigned. Refreshing server metadata and client
assets are separate explicit operations. External pack geometry/texture validation is
handled by vanilla; hostile third-party packs are not made trustworthy by the catalogue scanner.

## Server config (all options, JSON keys stable)

| Key | Default | Bound / effect |
|---|---|---|
| enabled | true | Refuse all model changes when false; known approved appearances display as base items on compliant clients |
| operatorsOnly | true | Game master permission required for assignment/removal |
| allowModItems | false | Only vanilla items unless explicitly enabled and individually approved |
| legacyNameMatching | false | Exact source match_name accepted; never hidden suffix/prefix matching |
| levelCost | 1 | Clamped 1–10 levels, vanilla Creative exemption |
| maxModels | 64 | Clamped 1–64 definitions; additional total serialization cap 24,000 chars |
| maxFileBytes | 16384 | Clamped 256–16384 bytes per definition |

Named hard caps live in `ModelPolicy`, readable by the Balancing-Zentrale. No recipes,
loot/trades/economy or extra balance store is needed. Storage contract is `balance/simplemodels`.
Client configs never affect server decisions. Config reload takes effect before result pickup;
stale results are refused after model removal, disablement or cost changes. Rendering suppression
only covers current approved base/model pairs: previously withdrawn definitions and arbitrary
third-party/Creative-created item_model values are outside this mod's ownership. A modified
client can always lie about its own visuals; inventory identity/gameplay remains server-owned.

## Security and collisions

IDs are parsed and length-limited; absolute/backslash/traversal paths, unknown/air base items,
control characters, oversized names/authors/tags, deep/malformed JSON and nonregular/symlink
definition files are rejected. At most 256 candidate filenames, 64 accepted models and
28,000 wire characters; files are read with a hard byte limit. Duplicate IDs keep the first
sorted entry. Invalid policy fails closed. Folder enumeration is nonrecursive. Admin filesystem
permissions remain the boundary; no untrusted client writes or chosen file paths exist.

No duplication, arbitrage, movement/reach, entity/chunk spam, block interaction, world border
or claim bypass is introduced. Assignment requires an empty second slot and rechecks the
input, permission, catalogue and cost at pickup. Defaults restrict cosmetics to operators.
True item/custom name stays visible; no global bracket hiding, lore forging or stat changes.

SimpleBuilding overlap: both mix into `AnvilMenu.createResult` / `mayPickup`. The model mixin
uses priority 1100 and intercepts only reserved model requests. Normal recharge/repair/rename
falls through untouched; mod items require explicit opt-in. Rendering hooks target the native
item-model resolver instead of the source's obsolete ItemRenderer signature. No registry,
recipe/tag/config/keybind collisions or duplicate SimpleTweaks features were found. Other
modules are accessed only by public registry IDs in integration tests, never internal classes.

## Verification and remaining ports

Shared 16-test server catalogue runs on both loaders with SimpleBuilding loaded: launch,
assignment/reset preservation, permissions, forged request bounds, all config bounds,
definition validation, malformed/oversized/deep folder files, reload fail-closed/add/remove,
wire round-trip, search, real anvil cost/take/no-duplication/insufficient XP/second slot,
stale pickup, normal rename/brackets, foreign SimpleBuilding item opt-in, exact legacy names,
and all config/lang metadata. Module data hook checks bilingual wiki/config/data and adapters.
Client smoke covers title→world, browser/search/item preview, settings, help and anvil assignment.
Actual run results are appended below after execution; source had no tests to port or skip.

Forge 26.3: add entrypoint/network/login/commands/client config registration and test adapter;
no Forge project is declared as shipped. 26.2, 1.21.11, 26.4 are deferred until owner release
approval; no source changes there. Shared module logic is loader-neutral. Owner acceptance
of UI and resource distribution with their actual custom model pack remains necessary.
