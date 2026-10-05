# Forge 26.3: Auto Smither hopper extraction

## Scope and findings

Work on `gpt-forgesmither` only; commit without pushing or merging. No client.
Forge 26.3 / 66.0.8 patches BaseContainerBlockEntity to expose an InvWrapper
for every capability face. InvWrapper.extractItem ignores WorldlyContainer's
canTakeItemThroughFace. VanillaInventoryCodeHooks.extractHook therefore takes
templates and additions before reaching the result. The 20-tick failure is a
real inventory leak, not a reason to lengthen the test.

## Implementation plan

- Reuse Forge's SidedInvWrapper for AutoSmitherBlockEntity through a narrow
  Forge mixin in the existing Forge mixin configuration. All Auto Smither faces
  have identical slot, insertion and extraction rules; one wrapper is sufficient.
  Keep BaseContainerBlockEntity's existing capability invalidation/revival.
- Preserve the existing hopper test and its 20-tick deadline. Strengthen it by
  keeping all three input types present during extraction and checking retention.
- Update `.claude/QUEUE.md` and record evidence here. No gameplay changes to
  Fabric/NeoForge or ports; shared tests must still compile on 26.2.

## Risks and verification

Confirm the narrow mixin does not affect other Forge containers. Confirm real
hopper extraction and forbidden input extraction, without weakening assertions.
Run Forge `simplebuilding:workstation*` before/after, then full Fabric/NeoForge/
Forge 26.3 server suites and read `alles gruen`. Run `gradlew.bat check -q
-PskipWiki`, plus required 26.2 Fabric/NeoForge and Forge 26.3 compilation.
Verify test-centre rebuild and item/block coverage in the full server reports.
No textures, recipes or wiki claims change; no client or owner-world commands.

## Results

- Baseline run `2026-10-05T03-50-41Z-2c1e`: `NICHT gruen: 5/6 bestanden,
  1 rot`, exactly `workstation_game_test_auto_smither_sorts_hopper_input`:
  `hopper removed output on tick 20`. The wildcard selects six workstation tests.
- Added a Forge-only interception of `createUnSidedHandler`, guarded by
  `McVersion.AUTO_SMITHER` and the concrete block-entity type. No registry/factory
  replacement or duplicated block entity. Forge's SidedInvWrapper uses DOWN as
  the representative face because the existing rules are identical on all faces.
- The existing test now also retains a diamond pickaxe in the base input while
  extracting the netherite pickaxe. Its fixed deadline and original assertions
  remain unchanged.
- RTK/ripgrep were not available in the command PATH. Serena was activated at the
  worktree's absolute path, but its symbol lookup returned no match; exact native
  reads were used. Local Forge 66.0.8 sources provided the wrapper evidence.
- Post-fix filtered run `2026-10-05T03-56-10Z-29e4`: Forge 26.3
  `alles gruen: 6/6 bestanden, 0 rot`.
- Full server run `2026-10-05T03-59-31Z-4aed`: Fabric 946/946,
  NeoForge 946/946, Forge 947/947; `alles gruen: 2839/2839 bestanden, 0 rot`.
  Fresh JUnit reports on all three loaders confirm the original hopper test,
  `test_centre_game_test_the_whole_centre_builds_and_matches_its_plan` and
  `test_centre_game_test_every_mod_item_and_block_has_its_place_in_the_test_centre`
  pass. Test worlds only; no owner-world changes.
- Gate command: `gradlew.bat check -q -PskipWiki :compileJava
  :neoforge:compileJava -Pforge263=true :mc26_3:forge:compileJava
  -PwikiPython=C:/Users/o_o/code/simplebuilding/.ai-runs/venv/Scripts/python.exe`.
  Final result: `GRADLE_EXIT=0`. Includes shared 26.2 compilation and the Forge
  mixin compilation. Raw log: `.ai-runs/forgesmither-gate-retry.log`.
- First gate attempt failed during
  `:modules:simplefun:neoforge:createMinecraftArtifacts` because the Minecraft
  manifest download raised `ConnectException` / `UnresolvedAddressException`.
  Retrying the identical command passed; no code workaround or skipped task.
  Initial log: `.ai-runs/forgesmither-gate.log`.
- No plan deviation. No client, owner-world test-centre command, ports, merge
  or push. Wiki generation and the tasks gated by `skipWiki` were not run, as
  requested for this gate; no wiki content or generated data changed.
