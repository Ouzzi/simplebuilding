# Simple Models — 26.3 Fabric / NeoForge

## Source inventory and compatibility

Read-only source: `C:/Users/oussa/Downloads/Minecraft/Mine/custom created mods/renamed`,
initial commit `da086a2`, Fabric 1.21.11. The owner's checkout has staged/unstaged changes;
the port reads that working tree, never edits it. No README, todo file, tests, lang,
recipes, config, registry items/blocks/entities/enchantments, commands, or keybinds exist.
Eight Java files: empty initializer/datagen; unregistered client initializer; CIT resource
loader; definition record; unreachable catalog screen; outdated render substitution;
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
  deterministic bounded folder scan and join/reload catalog synchronization.
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

For an old pack, copy its `assets/renamed/cit/*.json` definitions into the server's catalog.
Keep each `model` identifier unchanged and place the new wrapper at
`assets/<namespace>/items/<model-path>.json`; for `renamed:item/tomato` that is
`assets/renamed/items/item/tomato.json`, pointing to the original geometry. No automatic
client-pack import can grant server approval.

Multiplayer: admins publish a trusted ZIP via Minecraft's standard `server.properties`
`resource-pack`, `resource-pack-sha1`, and `require-resource-pack=true` (or the vanilla
server pack push API supplied by hosting software). Catalog metadata is synchronized by
this mod; rendering resources use vanilla's consent/download/hash pipeline. No custom
HTTP downloader, remote URL importer, filesystem upload or archive extraction is added.
Private server directories are never published. Missing pack resources use vanilla missing
model fallback and do not grant gameplay effects. Local-only resources are previews, not
authority: only the server catalog can be assigned. Refreshing server metadata and client
assets are separate explicit operations. External pack geometry/texture validation is
handled by vanilla; hostile third-party packs are not made trustworthy by the catalog scanner.

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
28,000 wire characters; files are read with a hard byte limit. More than 256 candidate files
disable the catalog instead of accepting a filesystem-dependent subset. Duplicate IDs keep the first
sorted entry. Invalid policy fails closed. Folder enumeration is nonrecursive. Admin filesystem
permissions remain the boundary; no untrusted client writes or chosen file paths exist.

No duplication, arbitrage, movement/reach, entity/chunk spam, block interaction, world border
or claim bypass is introduced. Assignment requires an empty second slot and rechecks the
input, permission, catalog and cost at pickup. Defaults restrict cosmetics to operators.
True item/custom name stays visible; no global bracket hiding, lore forging or stat changes.

SimpleBuilding overlap: both mix into `AnvilMenu.createResult` / `mayPickup`. The model mixin
uses priority 1100 and intercepts only reserved model requests. Normal recharge/repair/rename
falls through untouched; mod items require explicit opt-in. Rendering hooks target the native
item-model resolver instead of the source's obsolete ItemRenderer signature. No registry,
recipe/tag/config/keybind collisions or duplicate SimpleTweaks features were found. Other
modules are accessed only by public registry IDs in integration tests, never internal classes.

## Verification and remaining ports

Shared 16-test server catalog runs on both loaders with SimpleBuilding loaded: launch,
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

### Executed verification (2026-09-30)

- Resumed work was reviewed and saved in `d2af64c1`; hard file-count rejection and its
  regression were saved in `abe43d67`. Source repository was only read; its original
  dirty working tree remains unchanged by this run.
- Existing 26.3 suites: 781 Fabric + 781 NeoForge, integration harness 1, module 16 + 16;
  all 1,595 selected tests passed, fresh reports in `2026-09-30T15-30-36Z-a0ee`.
  This complete run preceded the file-count hardening. The final module suites reran
  that change: **32/32, alles gruen**, `2026-09-30T15-56-03Z-8478`.
- Final Fabric client: **5/5 screenshot checkpoints, alles gruen**,
  `2026-09-30T15-59-22Z-0133`. Title/world, authoritative sync, tag search, native model
  preview, disabled rendering policy, settings/help and real C2S anvil assignment passed.
  Browser/settings/help/anvil screenshots were visually inspected and retained under
  `modules/simplemodels/previews`. The fixture deliberately uses a vanilla golden-apple
  model, so it does not claim validation of an owner's external geometry/texture pack.
- EN/DE config/default metadata, data integrity and module wiki checks passed;
  `wiki/generate.py --all` and `--all --check` passed. No new pixel art was created.
  Both isolated existing GameTest worlds rebuilt the test center and passed complete
  item/block coverage during the full server suite. No owner world was changed.
- Not verified: NeoForge client rendering, German UI layout, real external server-pack
  download, an old owner world, owner-world test center, or arbitrary third-party modpacks.
  There are no source tests or selected module tests skipped. Placed block geometry and
  direct remote file uploads are outside this implementation; additions/removals use
  the administrator's filesystem via the browser's import-folder/help workflow.
- Final `gradlew.bat check -q --no-daemon`: **GRADLE_EXIT=0**, output read; includes
  shared 26.2 compilation, module compilation/data, balance, wiki and existing gates.
