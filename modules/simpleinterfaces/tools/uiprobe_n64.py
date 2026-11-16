"""N64 (owner rule N62, docs/ai/PRINZIPIEN.md A7): game screenshots of the container margin on the stations and the mods' own
screens converted in N64, each with the probe status (slot positions in window pixels) next to the shot for the pixel count
(/root/sb-tools-b/ig57u/mock/measure_shots.py; expected 3 everywhere except the title side: previews/n64/measures-nachher.json).
Scenario: simpleinterfaces/n64. Not covered (need an entity/death/item): mounts, death vault, questionary, crucible."""
import json
import time

BLOCKS = [
    ('werkbank', 'minecraft:crafting_table', 'CraftingScreen'),
    ('ofen', 'minecraft:furnace', 'FurnaceScreen'),
    ('schmelzofen', 'minecraft:blast_furnace', 'BlastFurnaceScreen'),
    ('raeucherofen', 'minecraft:smoker', 'SmokerScreen'),
    ('braustand', 'minecraft:brewing_stand', 'BrewingStandScreen'),
    ('zaubertisch', 'minecraft:enchanting_table', 'EnchantmentScreen'),
    ('leuchtfeuer', 'minecraft:beacon', 'BeaconScreen'),
    ('amboss', 'minecraft:anvil', 'AnvilScreen'),
    ('schleifstein', 'minecraft:grindstone', 'GrindstoneScreen'),
    ('steinsaege', 'minecraft:stonecutter', 'StonecutterScreen'),
    ('webstuhl', 'minecraft:loom', 'LoomScreen'),
    ('kartentisch', 'minecraft:cartography_table', 'CartographyTableScreen'),
    ('schmiedetisch', 'minecraft:smithing_table', 'SmithingScreen'),
    ('crafter', 'minecraft:crafter', 'CrafterScreen'),
    ('fletching', 'minecraft:fletching_table', 'FletchingScreen'),
    ('enchanter', 'simpleenchanting:dark_enchanter', 'EnchanterScreen'),
    ('foundry', 'simplefoundry:foundry_desk', 'FoundryScreen'),
]


def save(s, name):
    time.sleep(0.8)
    s.shot(name)
    dest = s.out / 'shots'
    dest.mkdir(parents=True, exist_ok=True)
    (dest / f'simpleinterfaces-n64-{name}.json').write_text(json.dumps(s.st(), indent=1))


def run(s):
    for name, block, fragment in BLOCKS:
        s.close()
        st = s.st()
        x, y, z = [int(v) for v in st['pos'].replace(',', ' ').split()]
        s.chat(f'setblock {x + 2} {y} {z} minecraft:air', 0.3)
        s.stage(block, 2, 0, 0)
        s.io.right_click()
        st = s.wait_screen(fragment, 10)
        ok = fragment in st.get('screen', '') and st.get('screen') != 'none'
        s.check(f'{name}: {block} opens', ok, st.get('screen'))
        if ok:
            save(s, name)
    # the player inventory (survival: the creative screen replaces it otherwise); twice, the second must not move the slots again
    s.close()
    s.chat('gamemode survival', 0.5)
    for n in (1, 2):
        s.key('e')
        st = s.wait_screen('InventoryScreen', 10)
        ok = 'InventoryScreen' in st.get('screen', '')
        s.check(f'player inventory opens ({n})', ok, st.get('screen'))
        if ok:
            save(s, f'spielerinventar-{n}')
        s.close()
    s.chat('gamemode creative', 0.5)


SCENARIOS = {'simpleinterfaces/n64': run}
