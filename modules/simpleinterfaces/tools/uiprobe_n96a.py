"""N96A (b-dinotex96, texture review N96A open): the redrawn dino armour icons in the hotbar and the inventory, and the sheet/plate
items (silver sheet, lead plate) in hand and in the inventory next to a Vanilla pressure plate - since N96A their item model inherits
the pressure plate's display (minecraft:block/thin_block), so they should look like flat 3D plates, not like a bare block model.

    xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe96a --scale 2 --only simpleinterfaces/n96a
"""
TREX = ['leather', 'iron', 'diamond', 'copper', 'gold', 'rose_gold', 'rose_bronze', 'netherite', 'enderite']
SMALL = ['leather', 'iron', 'diamond', 'gold', 'rose_gold', 'rose_bronze']
KINDS = ['triceratops', 'stegosaurus', 'parasaurolophus', 'utahraptor']
PLATES = ['simpleores:silver_sheet', 'simpleores:lead_plate', 'minecraft:heavy_weighted_pressure_plate']


def _pos(s):
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def _has(s, item):
    return any(str(v).startswith(item) for v in s.st().get('inventory', []))


def _setup(s):
    s.close()
    s.chat('gamemode creative @s', 0.3)
    s.chat('time set noon', 0.3)
    s.chat('weather clear', 0.3)
    # 26.x game rule ids are snake_case (the camelCase names are rejected, N83 run 2)
    s.chat('gamerule advance_time false', 0.3)
    s.chat('gamerule spawn_mobs false', 0.3)
    s.chat('effect give @s night_vision 99999 0 true', 0.3)


def scenario(s):
    _setup(s)
    x, y, z = _pos(s)
    # look at the open flat ground, so the items stand out against a calm background
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {x + 0.5} {y - 1} {z + 8.5}', 0.8)

    # Block 1: hotbar = the nine T. rex armours; inventory = the 24 armours of the other four dinosaurs.
    stacks = [f'simpledinos:trex_armor_{t} 1' for t in TREX]
    stacks += [f'simpledinos:{k}_armor_{t} 1' for k in KINDS for t in SMALL]
    s.clear_and_give(*stacks)
    for t in TREX:
        s.check(f'hotbar holds trex_armor_{t}', _has(s, f'simpledinos:trex_armor_{t}'))
    for k in KINDS:
        for t in SMALL:
            s.check(f'inventory holds {k}_armor_{t}', _has(s, f'simpledinos:{k}_armor_{t}'))
    s.key('1', 0.5)
    s.shot('1-hotbar-trex')
    s.key('e', 1.2)
    s.wait_screen('Inventory', 5)
    s.shot('1-inventar-dino-ruestungen')
    s.close()

    # Block 2: sheets and plates. Hotbar 1 silver sheet, 2 lead plate, 3 Vanilla heavy pressure plate (reference).
    s.clear_and_give(*[f'{p} 16' for p in PLATES])
    for p in PLATES:
        s.check(f'got {p}', _has(s, p))
    for i, p in enumerate(PLATES, start=1):
        s.key(str(i), 0.8)
        s.shot(f'2-hand-{p.split(":")[1]}')
    # third person (F5) with the silver sheet, then back to first person
    s.key('1', 0.5)
    s.key('F5', 0.8)
    s.shot('2-hand-dritte-person-silver_sheet')
    s.key('F5', 0.4)
    s.key('F5', 0.6)
    s.key('e', 1.2)
    s.wait_screen('Inventory', 5)
    s.shot('2-inventar-sheets-platten')
    s.close()
    # dropped on the ground (ground display) next to each other
    for i, p in enumerate(PLATES):
        s.chat(f'summon item {x + 0.5 + i - 1} {y + 0.2} {z + 2.5} {{Item:{{id:"{p}",count:1}},PickupDelay:32767,Age:-32768}}', 0.3)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {x + 0.5} {y} {z + 2.5}', 1.5)
    s.shot('2-boden-sheets-platten')
    s.chat('kill @e[type=item]', 0.3)
    s.check('N96A shots taken (hotbar, inventory, hand, ground)', True, 'see shots/')


SCENARIOS = {'simpleinterfaces/n96a': scenario}
