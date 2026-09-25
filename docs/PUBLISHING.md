# Publishing: what must be set by hand on Modrinth and CurseForge

Modrinth and CurseForge do **not** read dependency relations from the jar. `fabric.mod.json`,
`neoforge.mods.toml` and `mods.toml` only tell the mod loader what to check at startup; the
"Dependencies" / "Relations" of every uploaded file have to be entered on the website (or in the
upload script). This page lists them per file. Checked against the metadata on 2026-09-24.

## Required (upload fails the player at startup without them)

| File | Minecraft | Java | Loader | Required mods |
|---|---|---|---|---|
| `simplebuilding-<v>.jar` (Fabric) | 26.2 | 25 | Fabric Loader >= 0.19.2 | **Fabric API**, **Cloth Config** (>= 26.2) |
| `simplebuilding-neoforge-<v>.jar` | 26.2 | 25 | NeoForge >= 26 | **Cloth Config** (>= 26) |
| `simplebuilding-forge-<v>.jar` (parked, best effort) | 26.2 | 25 | Forge >= 65 | none |
| `simplebuilding-1.21.11-<v>.jar` (Fabric) | 1.21.11 | 21 | Fabric Loader >= 0.19.2 | **Fabric API**, **Cloth Config** (>= 21.11) |
| `simplebuilding-1.21.11-neoforge-<v>.jar` | 1.21.11 | 21 | NeoForge >= 21.11 | **Cloth Config** (>= 21.11) |
| `simplebuilding-26.3-<v>.jar` (Fabric) | 26.3 | 25 | Fabric Loader >= 0.19.2 (tested with 0.19.5) | **Fabric API** (>= 0.161.0+26.3), **Cloth Config** (>= 26.3) |
| `simplebuilding-26.3-neoforge-<v>.jar` - **upload as Beta** | 26.3 | 25 | NeoForge >= 26 (built against 26.3.0.16-beta) | **Cloth Config** (>= 26) |

Cloth Config is required on every Fabric and NeoForge file: the config is registered through
Cloth's AutoConfig on startup and Cloth is not bundled. (Until 2026-09-24 `neoforge.mods.toml`
wrongly declared it optional and `fabric.mod.json` did not list it at all.) Forge has no Cloth Config
for 26.x; the Forge module ships a small AutoConfig stand-in instead, so nothing is required there.

The 26.3 files are built by `:mc26_3:fabric` and `:mc26_3:neoforge` from the same sources as the
26.2 files plus the overlay in `mc26_3/` (see MULTILOADER_TODO.md). There is no Forge build for 26.3.
NeoForge 26.3 is still a beta (26.3.0.x-beta) - mark the NeoForge 26.3 upload as beta as well.
Cloth Config for 26.3 is 26.3.159 at the time of writing; the Fabric file checks `>=26.3`, the NeoForge
file the same `[26,)` range as on 26.2.

### Upload settings for the two 26.3 files

| Field | Modrinth | CurseForge | Fabric 26.3 | NeoForge 26.3 |
|---|---|---|---|---|
| Release channel | "Version type" | "Release type" | Release (or what the version is) | **Beta** - NeoForge 26.3 itself is 26.3.0.x-beta; never Release while it is |
| Game version | "Game versions" | "Game Version" | 26.3 only | 26.3 only |
| Loader | "Loaders" | "Mod loader" | Fabric (Quilt not tested) | NeoForge only (no Forge 26.3 file exists) |
| Java | - | "Java version" | Java 25 | Java 25 |
| Environment | "Environment" (project level) | "Environment" | client and server | client and server |
| Version number / display name (suggested) | "Version number" / "Version title" | "Display name" | `26.3-<v>` / `SimpleBuilding <v> (Fabric 26.3)` | `26.3-neoforge-<v>` / `SimpleBuilding <v> (NeoForge 26.3, beta)` |
| Required relations | "Dependencies: required" | "Required Dependency" | Fabric API, Cloth Config | Cloth Config |
| Optional relations | "Dependencies: optional" | "Optional Dependency" | JEI (recommended), Jade, AppleSkin, Mouse Tweaks, Mod Menu | JEI (recommended), Jade, AppleSkin, Mouse Tweaks |
| Changelog | "Changelog" | "Changelog" | mention "26.3: requires Fabric API 0.161.0+26.3" | mention "NeoForge 26.3 is beta - report loader crashes to NeoForge first" |

Do not tick 26.2 on a 26.3 file (or the other way round): the 26.3 jars check `~26.3` (Fabric) /
`[26.3,26.4)` (NeoForge), the 26.2 jars `~26.2` / `[26.2,26.3)`,
at startup and refuse the other version.

## Optional / recommended ("Optional dependency" on Modrinth, "Optional Dependency" on CurseForge)

| Mod | Why | Fabric 26.2 | NeoForge 26.2 | Forge 26.2 | Fabric 1.21.11 | NeoForge 1.21.11 | Fabric 26.3 | NeoForge 26.3 |
|---|---|---|---|---|---|---|---|---|
| **JEI** (Just Enough Items) - recommended | the mod ships a JEI plugin: in-world transformation categories + count-based smithing | yes | yes | no JEI build | yes | yes | yes (31.x beta) | yes (31.x beta) |
| Jade | block/entity tooltips | yes | yes | no build | yes | yes | yes (26.3.1) | yes (26.3.1) |
| AppleSkin | food values | yes | yes | no build | yes | yes | yes (3.0.10+mc26.3) | yes (3.0.10+mc26.3) |
| Mouse Tweaks | inventory handling | yes | yes | yes | yes | yes | yes (26.3-2.31) | yes (26.3-2.31) |
| Mod Menu (Fabric only) | opens the config screen (ModMenu entrypoint) | yes | - | - | yes | - | yes (21.0.0) | - |

Mark JEI as the one "recommended"/featured relation; the others are plain optional. Mod ids, in case
an upload tool asks: `jei`, `jade`, `appleskin`, `mousetweaks`, `modmenu`, `cloth-config` (Fabric) /
`cloth_config` (NeoForge), `fabric-api`.

In the jars these are declared as `recommends` (JEI) / `suggests` (the rest) in `fabric.mod.json`,
and as `type="optional"` (NeoForge) / `mandatory=false` (Forge) with ordering `NONE` in the
mods.toml files. An optional dependency with a version range still fails the start when an
incompatible version is installed (JEI: `[30,)` on 26.2 and 26.3 - JEI 31 for 26.3 satisfies it -, `[27,)` on 1.21.11).

## Dev-only runtime mods (not shipped, not a relation)

The dev clients/servers (`runClient`, `runServer`) load these extra mods so JEI support can be
checked by hand. They never reach a gametest or client-gametest run and never the jar.
Switch off with `-Pdev_mods=false`. Versions are pinned in `gradle.properties`.

| | Fabric 26.2 | NeoForge 26.2 | Forge 26.2 | Fabric 1.21.11 | NeoForge 1.21.11 | Fabric 26.3 | NeoForge 26.3 |
|---|---|---|---|---|---|---|---|
| JEI | 30.38.0.229 | 30.38.0.229 | - | 27.44.0.103 | 27.44.0.103 | 31.7.0.37 | 31.7.0.37 |
| Jade | 26.2.11+fabric | 26.2.10+neoforge | - | 21.1.6+fabric | 21.1.7+neoforge | 26.3.1+fabric | 26.3.1+neoforge |
| Mouse Tweaks | 26.2-2.31 | 26.2-2.31 | 26.2-2.31 | 1.21.11-2.30 | 1.21.11-2.30 | 26.3-2.31 (client only on Fabric) | 26.3-2.31 |
| AppleSkin | 3.0.10+mc26.2 | 3.0.10+mc26.2 | - | 3.0.8+mc1.21.11 | 3.0.8+mc1.21.11 | 3.0.10+mc26.3 | 3.0.10+mc26.3 |
| Mod Menu | 20.0.1 (already a regular dependency) | - | - | 17.0.1-beta.1 (regular dependency) | - | 21.0.0 (regular dependency) | - |

How they are wired: Fabric passes a synced folder (`build/devMods`) with `-Dfabric.addMods` to the
`runClient`/`runServer` tasks; NeoForge adds them to the `client`/`server` runs' own additional
runtime classpath; Forge copies Mouse Tweaks into `forge/run/mods` (only with `-Pforge_runs=true`).
IDE run configurations generated by Loom do not get them on Fabric.
