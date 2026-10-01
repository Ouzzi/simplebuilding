# Claims stages 5 and 6 — verification

Branch: `codex-next-claims-access`; base: `7422a3ab`.
Stage 5: `4ac2875a` (bounded owner trust and explicit OP4 administration).
Stage 6 is the separate commit containing this report; its hash is reported in the handoff response.
No push, merge, agents, clients or runtime ports were performed.

## Security decisions

- Claims remain disabled by default. Stages 1–3, 5 and 6 are implemented here.
  Stage 4 belongs to a separate unmerged worker; its completion is not established.
  This is not certification for enabling a complete land-protection system.
- Only owners grant/revoke trust. Trusted players cannot delegate; even OP4 uses
  explicit admin removal rather than modifying another owner's trust list.
  OP4 administration is independent of the existing optional OP4 protection bypass.
- Online names resolve against connected server players. Offline identities require
  full nonzero UUIDs; no network lookup, guessed UUID, selector or shortened UUID.
  Server-owned immutable snapshots, hard caps and a shared mutation cooldown apply.
  Failed atomic writes publish no new permissions and lock the active provider.
- Dimensions consumes only the existing public framework API. No API signature or
  shared root implementation was changed. Both production loader jars include the API.
  The manifest change is confined to the Tweaks test dependency on Dimensions;
  NeoForge's extra module wiring is confined to the explicit Tweaks test task.
- Check all matched frame/interior cells before ignition, actual source body,
  destination portal and landing body/floor, and every generated platform/exit cell.
  Legacy metadata conversion also requires footprint permission. Landing permission
  is checked before construction and immediately before teleporting.
- Generated exits may escape a newly denied source. Denied, unsafe, missing-dimension
  or out-of-border return links use authorized safe candidates near the saved origin,
  then the bounded overworld spawn fallback. No authorized safe candidate means refusal
  and administrative help; no destructive platform or unauthorized landing is invented.
- Unsupported foreign claim mods keep their separate fail-closed gate, even with an
  allowing framework provider. Disabled/absent Tweaks adds no new claim restrictions.
  Custom dimensions still require the existing explicit claim-dimension allowlist
  before new claims may be created there; this task does not alter its defaults.
- EN/DE option names, tooltips, tabs and defaults share one module resource directory
  used by both loaders. There is no second Claims language copy in root resources.
  Command replies are translated; no gadget display text or art was added.

## Successful server runs

All runs use Minecraft 26.3 Fabric and NeoForge, one filter per invocation and at
most two Gradle workers. Each success below was read from `alles gruen`, not inferred
from the runner's exit code. Reports/logs remain in ignored `testing/runs/`.

| Run ID | Selection | Result |
| --- | --- | --- |
| `2026-09-30T23-18-06Z-4f7d` | Tweaks `simpletweaks:*claims*`, stage 5 | 42/42, 21 per loader |
| `2026-09-30T23-24-05Z-521d` | Complete Dimensions catalogues | 74/74, 37 per loader |
| `2026-09-30T23-29-57Z-39f5` | Real Claims/portal flow | 2/2, one per loader |
| `2026-09-30T23-37-40Z-b5cf` | Complete Tweaks catalogues | 66/66, 33 per loader |
| `2026-09-30T23-47-18Z-af45` | Dimensions `simpledimension:*claims*` | 8/8, four per loader |
| `2026-09-30T23-49-03Z-e365` | Tweaks `simpletweaks:*claims*` | 44/44, 22 per loader |
| `2026-09-30T23-51-37Z-36e1` | Final Dimensions `simpledimension:*claim*` | 10/10, five per loader |
| `2026-09-30T23-57-47Z-0885` | Final real Claims/portal flow after body checks | 2/2, one per loader |

The stage-5 tests exercise real Brigadier dispatch, real block access after trust,
revocation and reload, online join/offline UUID, outsider/delegation denial, OP3/OP4
with both bypass settings, caps/cooldown and failed grant/revocation persistence.
Disabled command registration and pre-mutation collision rejection remain covered.

The new cross-mod flow uses two server players, actual loader ignition, real portal
tick hooks and commands: outsider denial, owner travel, source trust, revocation
while away, newly claimed generated exit, safe evacuation, destination denial,
destination trust, exact return and disabled-provider/no-ledger-IO positive control.
It imports no Dimensions implementation. Dimensions tests additionally exercise
per-cell refusal on both axes and unsupported-provider refusal. The NeoForge
Dimensions target runs without Tweaks; Fabric runs with disabled Tweaks.
The final Dimensions run also covers wider landing bodies, off-center source bodies,
missing and out-of-border return links, and the existing destination-claim test.

Earlier failures were inspected and resolved: the MC 26.3 ChunkPos API, a NeoForge
unmodifiable player list in a test, beam entity visibility in an adjacent chunk,
and a provider fixture shared with the disabled-deed test. The latter now has its
own test environment/batch; existing assertions remain intact. Run
`2026-09-30T23-39-52Z-43b5` selected no tests because `*claims*` lacked a namespace;
it is not counted as verification. Subsequent namespaced runs are listed above.

## Verification limits

- Full merged normal/integration matrix belongs to the orchestrator after merging
  stage 4 and the dimensions-settings branch. Neither branch is merged here.
- Existing stage-3 gaps remain: full blueprint/transformation/double-chest and
  wider tool matrix, environmental/automation coverage. See the module's security
  findings; these runs do not establish the absent stage-4 behavior.
- No clients, owner world, real remote clients, arbitrary modpacks, external claim-mod
  jars, production old-save migration, Forge, 26.2/1.21.11/26.4 runtime tests.
  Shared 26.2 compilation is part of the requested final Gradle check.
- No separate server boot from published release jars: loader GameTests exercise the
  development runtimes; the built production jars receive a nested-API content check.
- Test-centre construction/coverage is restricted to disposable test worlds;
  the owner's existing centre is not changed.
- Final owner acceptance and enabled-ready review remain open. No additional
  implementation decision is required to review these two commits.

## Exact changed files

Stage 5 (`4ac2875a`):

- `.claude/QUEUE.md`
- `balance/simpletweaks/module.json`
- `docs/HANDOFF.md`
- `docs/modules/simpletweaks.md`
- `modules/simpletweaks/CLAIMS-ACCESS-PLAN.md`
- `modules/simpletweaks/fabric/src/main/java/com/simplebuilding/modules/simpletweaks/ClaimsGameTest.java`
- `modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ClaimAccessTests.java`
- `modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ClaimCommands.java`
- `modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ClaimTests.java`
- `modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/Claims.java`
- `modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ToolClaimTests.java`
- `modules/simpletweaks/shared/resources/assets/simpletweaks/lang/de_de.json`
- `modules/simpletweaks/shared/resources/assets/simpletweaks/lang/en_us.json`
- `modules/simpletweaks/wiki/manual.json`
- `wiki/data/modules.js`
- `wiki/data/simpletweaks.js`
- `wiki/data/simpletweaks.json`

Stage 6 (this report's commit):

- `.claude/QUEUE.md`
- `balance/simpletweaks/module.json`
- `docs/HANDOFF.md`
- `docs/modules/simpledimensions.md`
- `docs/modules/simpletweaks.md`
- `modules/modules.json`
- `modules/simpledimensions/fabric/build.gradle`
- `modules/simpledimensions/fabric/src/main/java/com/simplebuilding/modules/simpledimensions/ModuleGameTest.java`
- `modules/simpledimensions/neoforge/build.gradle`
- `modules/simpledimensions/shared/java/com/simplebuilding/modules/simpledimensions/DimensionRuntime.java`
- `modules/simpledimensions/shared/java/com/simplebuilding/modules/simpledimensions/DimensionTests.java`
- `modules/simpledimensions/shared/java/com/simplebuilding/modules/simpledimensions/PortalActivation.java`
- `modules/simpledimensions/shared/java/com/simplebuilding/modules/simpledimensions/PortalProtectionTests.java`
- `modules/simpledimensions/wiki/manual.json`
- `modules/simpletweaks/CLAIMS-ACCESS-PLAN.md`
- `modules/simpletweaks/CLAIMS-ACCESS-VERIFICATION.md`
- `modules/simpletweaks/fabric/src/main/java/com/simplebuilding/modules/simpletweaks/ClaimsGameTest.java`
- `modules/simpletweaks/fabric/src/main/resources/data/simpletweaks/test_environment/claims_portal.json`
- `modules/simpletweaks/neoforge/build.gradle`
- `modules/simpletweaks/neoforge/src/main/java/com/simplebuilding/modules/simpletweaks/ModuleNeoTests.java`
- `modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ClaimCommands.java`
- `modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ClaimPortalTests.java`
- `modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ClaimTests.java`
- `modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ToolClaimTests.java`
- `modules/simpletweaks/shared/resources/assets/simpletweaks/lang/de_de.json`
- `modules/simpletweaks/shared/resources/assets/simpletweaks/lang/en_us.json`
- `modules/simpletweaks/wiki/manual.json`
- `wiki/data/modules.js`
- `wiki/data/simpledimensions.js`
- `wiki/data/simpledimensions.json`
- `wiki/data/simpletweaks.js`
- `wiki/data/simpletweaks.json`

## Test-centre result

Run `2026-09-30T23-53-58Z-9ce6` built and checked the centre in disposable
26.3 worlds: Fabric 5/5, NeoForge 4/5. Item coverage and complete plan/build
passed on both loaders. The existing NeoForge button-isolation test reported
a missing marker from the gallery command block (9/10 overall, not green).
The unchanged failed test passed in isolation: `2026-09-30T23-56-25Z-2d76`,
1/1, alles gruen. Its intermittent cause is not established; no unrelated
root test or production code was changed. This is not a claimed 10/10 clean run.

## Final gate and data checks

`gradlew.bat check :modules:simpledimensions:fabric:jar :modules:simpledimensions:neoforge:jarJar -q --max-workers=2` completed with **GRADLE_EXIT=0**.
Log: `scratchpad/claims-access/final-check.log`. The complete requested check includes
shared/baseline compilation, balance, Jade/provider separation, atlases, module data,
wiki and tests (19 wiki tests). Existing compiler warnings and informational cache
misses remain; no failed Gradle task. Default wiki generation/check, both affected
module generators/checks, bilingual data checks and manifest validation also passed.
QUEUE/HANDOFF are append-only; only the Tweaks manifest entry changed.

NeoForge `jarJar` generates the embedded-library input tree rather than the final
mod jar; the final production artifact is built with the additional module `jar` task.

Production package inspection (additional NeoForge jar task: JAR_EXIT=0):

- `modules/simpledimensions/fabric/build/libs/simpledimensions-26.3-fabric-0.1.0.jar`: `META-INF/jars/framework-0.1.0.jar`, Protection.class and loader metadata verified.
- `modules/simpledimensions/neoforge/build/libs/simpledimensions-26.3-neoforge-0.1.0.jar`: `META-INF/jarjar/com.simplebuilding.framework.framework-0.1.0.jar`, Protection.class and loader metadata verified.
