# Simple Beacons 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Beacons** for Minecraft 26.3 on Fabric.

Strong Beacon (6 pyramid levels, 10 more effects, range by pyramid material), Strong Conduit (7x7x7 frame, 216 blocks under water) and the Strong Upgrade template. Works on its own with the axe; a hammer mod adds the sledgehammer.

## Added

- **Strong Upgrade** - A smithing template you cannot craft from plain materials: it comes from the Wither.
- **Evil Upgrade** - A smithing template that turns a beacon into an Evil Beacon (with a Red Nether Gem; also conduits).
- **Strong Beacon** - A beacon with up to five pyramid levels (11x11 below the Vanilla four, 285 blocks in all): primary powers on levels 1 to 4, the secondary power on level 5.
- **Mighty Beacon** - The next step after the Strong Beacon: up to six pyramid levels (454 blocks), primary powers on levels 1 to 5, the secondary power on level 6, range 70 blocks with Iron at level 6.
- **Evil Beacon** - A beacon that weakens instead of helping: mild negative powers for players in range (primary levels 1 to 4, the secondary power on level 5), up to 5 pyramid levels like the Strong Beacon.
- **Mighty Evil Beacon** - The Evil Beacon's big brother: up to six pyramid levels, primary powers on levels 1 to 5, the secondary power on level 6.
- **Beacon screen** - The beacons open a beacon screen in Vanilla's own style: one row taller (Strong, Evil: four primary rows) or two rows taller (Mighty kinds: five rows), three powers per row; every pyramid level has its own marker icon.
- **Range by pyramid material** - The range of all Strong, Mighty, Evil and Vanilla beacons grows with the pyramid's material: Netherite blocks count x2, Diamond x1.5, Gold and Emerald x1.1, Iron x1.
- **Upgrading a placed beacon or conduit** - Hold an axe (with a hammer mod such as Simple Hammers: its sledgehammer) in the main hand and the Strong Upgrade in the off hand, carry a nether star and use a placed beacon or conduit 8 times.
- **Strong Conduit** - A conduit with a larger area (owner: no new ability): range is (frame blocks / 7) x 24, 1.5 times Vanilla's 16 - 144 blocks with the Vanilla 5x5x5 frame, 216 with the full 7x7x7 frame (three crossing 7x7 rings of prismarine, prismarine bricks, dark prismarine or sea lanterns, 66 blocks).
- **Mighty Conduit** - The Strong Conduit's big brother: frames up to 9x9x9 (90 blocks), range (frame blocks / 7) x 32 (at most 324), attack 16 with the full frame.
- **Evil Conduit** - The opposite of the Strong Conduit: players really under water in range get Mining Fatigue and Slowness.
- **Mighty Evil Conduit** - The Evil Conduit with a 9x9x9 frame, range up to 324 blocks and harsher effects: Mining Fatigue II, Slowness, Weakness and Darkness.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simpleores

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplebeacons config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
