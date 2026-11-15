"""N62 (owner 15.11.): game screenshots of the fixed container margin - Vanilla storage screens and the mods' own two-box
screens, each with the probe status (slot positions in window pixels) saved next to the shot for the pixel count
(/root/sb-tools-b/ig57u/mock/measure_shots.py). Scenario: simpleinterfaces/n62."""
import json
import time

BLOCKS = [
    ('kiste', 'minecraft:chest', 'ContainerScreen'),
    ('fass', 'minecraft:barrel', 'ContainerScreen'),
    ('shulker', 'minecraft:shulker_box', 'ShulkerBoxScreen'),
    ('trichter', 'minecraft:hopper', 'HopperScreen'),
    ('werfer', 'minecraft:dispenser', 'DispenserScreen'),
    ('netherite-kiste', 'simplestorage:netherite_chest', 'TieredChestScreen'),
    ('enderit-fass', 'simplestorage:enderite_barrel', 'Screen'),
    ('crafting-storage', 'simplestorage:storage_crafting_table', 'StorageCraftingScreen'),
    ('musikpresse', 'simplemusic:disc_press', 'DiscPressScreen'),
]


def run(s):
    for i, (name, block, fragment) in enumerate(BLOCKS):
        s.close()
        st = s.st()
        x, y, z = [int(v) for v in st['pos'].replace(',', ' ').split()]
        s.chat(f'setblock {x + 2} {y} {z + 3 * i - 12} minecraft:air', 0.3)
        s.stage(block, 2, 0, 3 * i - 12)
        s.io.right_click()
        st = s.wait_screen(fragment, 10)
        ok = fragment in st.get('screen', '') and st.get('screen') != 'none'
        s.check(f'{name}: {block} opens', ok, st.get('screen'))
        if not ok:
            continue
        time.sleep(0.8)
        s.shot(name)
        dest = s.out / 'shots'
        dest.mkdir(parents=True, exist_ok=True)
        (dest / f'simpleinterfaces-n62-{name}.json').write_text(json.dumps(s.st(), indent=1))


SCENARIOS = {'simpleinterfaces/n62': run}
