# Simple Vault 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Vault** for Minecraft 26.3 on Fabric.

A rare gap in the Overworld bedrock leads into the **Bedrock Vault**: quiet halls on two floors, enclosed by solid bedrock. Three riddles and the Ascent Altar are the only way out. Works on its own with Vanilla and SimpleLib.

## Added

- **The Bedrock Vault** - A rare 2 x 2 gap in the bedrock (one chunk in 400 by default, adjustable in the config) hides an old ladder; step onto the Vault Gate at its bottom to arrive in the vault.
- **The three riddles** - Light (five levers and lamps), Sound (strike four crystals in the order of the frieze) and Path (cross the tiles where the lichen glows). Every try answers in the action bar, nothing punishes, and every riddle can be tried again. A solved chamber gives you its seal part.
- **The ascent** - Set the three seal parts into the Ascent Altar. The seal in the dome gives way, a reward drops and a stream carries you up to the surface above the place you came in. The vault stays solved.
- **Seal parts never lock you in** - You can take a seal part again whenever the altar still lacks it. Seal parts fit only the vault they come from.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in the config file where available.
- Back up your world before updating; the vault is generated in new chunks only.
