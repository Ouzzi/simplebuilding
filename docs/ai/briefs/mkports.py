import os
SP = os.path.dirname(os.path.abspath(__file__))
ROOT = "C:/Users/oussa/Downloads/Minecraft/Mine/custom created mods"
MODS = {
    "money": ("simplemoney", "Simple Money", "simplemoney",
              "Fabric-only, Minecraft 1.21.11, about 11 Java files, version 1.2.16 (design/ folder has art sources). Small mod: port fully."),
    "fun": ("simplefun", "Simple Fun", "simplefun",
            "Multiloader (common/fabric/neoforge, buildSrc), Minecraft 1.21.11, about 188 Java files: the biggest one. Split the work into clear stages and commit after each working stage (inventory, registry/content, gameplay, client, tests)."),
    "qol": ("simplequalityoflife", "Simple Quality of Life", "simplequalityoflife",
            "Multiloader (common/fabric/neoforge) with a legacy-src folder, Minecraft 1.21.11, about 72 Java files, version 1.0.6. Has server config sync (furnace lava-fill etc.). Check carefully for overlaps with SimpleBuilding (furnaces, hoppers, tooltips) and with the QoL dev mods listed in tools/devmods.json."),
    "riding": ("simpleriding", "Simple Riding", "simpleriding",
               "Fabric-only, Minecraft 1.21.11, about 22 Java files, version 1.0.5. SimpleBuilding ships mount armor (horse/nautilus): check for overlap."),
    "visuals": ("simplevisuals", "Simple Visuals", "simplevisuals",
                "Fabric-only, Minecraft 1.21.11, about 35 Java files, version 1.0.7, client-heavy (tooltips, visuals). Keep client code in client-only classes; add client smoke checks carefully."),
    "dimensions": ("simpledimensions", "Simple Dimensions", "simpledimensions",
                   "TWO source folders exist: simpledimentions_05-2026 (Minecraft 26.1.2, common/fabric/forge/neoforge, mod_id simpledimension, version 0.1.0-26.1.2, newest) and simpledimensions (older, has paper/ and instructions+ folders). Use the newest as primary source, compare the older one for features that got lost, and document the decision. The module id is simpledimensions (plural); keep the source registry namespace if worlds depend on it and document it."),
    "models": ("simplemodels", "Simple Models", "renamed",
               "The source folder is called `renamed` (mod id renamed, Minecraft 1.21.11, Fabric, only about 8 Java files, single initial commit - probably a skeleton). The owner wants it renamed to Simple Models (module id simplemodels): it is about customising item and block models. Inventory what exists; implement what is there; propose (docs/modules/simplemodels.md) a sensible feature scope for item/block model customisation but implement only what the skeleton already does plus the wiring/tests/wiki - ask the owner in the report for the scope of new features."),
    "tweaks": ("simpletweaks", "Simple Tweaks", "simpletweaks",
               "Fabric, Minecraft 1.21.11, about 56 Java files, version 1.2.12, has libs/. Most features were ALREADY ported into SimpleBuilding (see docs/SIMPLETWEAKS-UEBERNAHME.md; pressure plates, chunk loader, elytra pads, fly pads, spawn teleporter, spawn elytra, XP orbs, laser, echo compass, commands, config). In the source repo a local branch `remove-ported-features` may exist (read it with git show/git worktree-free commands, do not check it out inside the source repo): it removes the ported features. The module simpletweaks = ONLY what is NOT ported (claims were deliberately NOT ported - keep them out unless the source has them as the remaining core; report the decision as a question). Make sure a world that has both SimpleBuilding and the module never gets duplicate registry entries."),
}
pre = open(os.path.join(SP, "pre.md"), encoding="utf-8").read()
contract = open(os.path.join(SP, "mm-contract.md"), encoding="utf-8").read()
port = open(os.path.join(SP, "port.md"), encoding="utf-8").read()
for key, (mid, disp, src, note) in MODS.items():
    spec = f"- module id: {mid}\n- display name: {disp}\n- source repo (read-only): {ROOT}/{src}\n- notes: {note}\n"
    with open(os.path.join(SP, f"brief-port-{key}.md"), "w", encoding="utf-8") as f:
        f.write(pre + "\n" + contract + "\n" + port + spec)
print("ok", list(MODS))
