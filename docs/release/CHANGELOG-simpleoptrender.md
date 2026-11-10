# Simple Opt Render 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Opt Render** for Minecraft 26.3 on Fabric.

Client optimizations of Simple Optimization: leaf culling, light-map cache, more culling (sign text, item frames, beacon beams), fast IP ping, telemetry off, optional detail switches (particles, texture animations, air fog). Each area steps back when a known optimization mod covers it.

## Added

- **Leaf culling** - Leaves no longer draw the faces they share with other leaves, so forests build smaller chunk meshes while the outside keeps the fancy cutout look.
- **Light map cache** - The light map is only uploaded again when one of its inputs (sky and block light, gamma, effects, boss darkening, colors) moved by at least half a color step since the last upload; smaller changes cannot change a pixel, so the picture stays identical.
- **Fast IP ping** - A server address typed as a plain IP (IPv4, or IPv6 in brackets) is used as it is: no reverse DNS lookup and no SRV query, so the server list fills faster.
- **Telemetry off** - The client never starts a telemetry session: every telemetry event goes to the Vanilla disabled sender, nothing is sent.
- **More culling** - Three more things are left out when they cannot be seen, each with its own switch (all on): signText drops the text side facing away from you once you stand more than half a block behind the board; itemFrames skips frames (with item or map) seen from behind their solid wall, frames with the Glowing effect stay; beaconBeams skips beacon beams outside the view.
- **Detail switches** - Switches without needing Sodium, all true (Vanilla) by default: particles false adds no particles, textureAnimations false keeps animated textures (water, lava, fire, portals) on their current frame, fog false lifts the air and render-distance fog.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleoptrender config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
