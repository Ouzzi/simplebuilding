"""simplemobmanners-1: Bucket of Frog in 3 variants (temperate / cold / warm), caught with the REAL right mouse button, in hand and in the
inventory with the tooltip (name and variant) - a-w4c, scenario for modules/simpleinterfaces/tools/ui_probe.py.

Flow (survival): three silent AI-less named frogs (one per variant) stand in front of the player; a water bucket in hotbar 1/2/3 is
used on each frog with the real right click (crosshair on the frog) -> the water bucket becomes a simplemobmanners:frog_bucket
(checked in the inventory and by its frog_variant component on the server), first-person picture of each bucket in the hand (GUI hidden),
then E opens the inventory and the real pointer rests on each bucket slot: the tooltip ("Frog: <name>", "Variant: <variant>") is in the picture.
# NUR BESITZER: whether the three bucket textures differ clearly enough at a glance (cold / warm / temperate colour) and the tooltip wording.
"""
import math
import time

VARIANTS = (('temperate', 'Freddy', -1.2), ('cold', 'Kermit', 0.0), ('warm', 'Fiona', 1.2))


def _pos(s):
    return tuple(int(v) for v in s.st()['pos'].replace(',', ' ').split())


def _aim(s, f, t, wait=0.8):
    """Stand at feet point f and look at point t (eye height 1.62, like a crosshair)."""
    dx, dy, dz = t[0] - f[0], t[1] - (f[1] + 1.62), t[2] - f[2]
    yaw = math.degrees(math.atan2(-dx, dz))
    pitch = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
    s.chat(f'tp @s {f[0]:.2f} {f[1]} {f[2]:.2f} {yaw:.1f} {pitch:.1f}', wait)


def _sense(s, cond, slot=26):
    """A server-side test made visible: a stone appears in inventory.<slot> when the condition holds."""
    s.chat(f'item replace entity @s inventory.{slot} with minecraft:air', 0.2)
    s.chat(f'execute {cond} run item replace entity @s inventory.{slot} with minecraft:stone 1', 0.3)
    inv = s.st().get('inventory', [])
    return len(inv) > 9 + slot and inv[9 + slot].startswith('minecraft:stone')


def frog_bucket(s):
    s.chat('time set day', 0.3)
    s.chat('weather clear', 0.3)
    s.chat('gamemode survival @s', 0.6)
    s.chat('clear @s', 0.3)
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 20.5} 0 0', 1.8)
    x, y, z = _pos(s)
    feet = (x + 0.5, y, z + 0.5)
    s.chat('kill @e[type=minecraft:frog,tag=uipfrog]', 0.2)
    caught = []
    try:
        for i, (variant, name, off) in enumerate(VARIANTS):
            s.chat(f'summon minecraft:frog {x + 0.5 + off} {y} {z + 2.0} {{variant:"minecraft:{variant}",CustomName:"{name}",NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:["uipfrog"]}}', 0.4)
            s.chat(f'item replace entity @s hotbar.{i} with minecraft:water_bucket', 0.3)
        for i, (variant, name, off) in enumerate(VARIANTS):
            s.io.key(str(i + 1), 0.4)
            _aim(s, feet, (x + 0.5 + off, y + 0.25, z + 2.0), 1.0)
            s.io.right_click()
            time.sleep(0.5)
            inv = s.st().get('inventory', [])
            got = len(inv) > i and inv[i].startswith('simplemobmanners:frog_bucket')
            s.check(f'real right click with a water bucket catches the {variant} frog', got, inv[:3])
            if not got:  # keep the rest of the scene: give the bucket directly
                s.chat(f'give @s simplemobmanners:frog_bucket[frog/variant="minecraft:{variant}",custom_name="{name}"]', 0.5)
                continue
            caught.append(i)
            # N60P: an item predicate on the hotbar slot instead of an NBT path into the player's Inventory list (run 1: red for all three
            # while the tooltip of the same stacks shows "Variant: ..." and the gametest frog_bucket checks the component)
            # N60P r2: the component id on 26.x is minecraft:frog/variant (client.log run 2: "Unknown item component 'minecraft:frog_variant'")
            pred = f'simplemobmanners:frog_bucket[minecraft:frog/variant="minecraft:{variant}"]'
            by_items = _sense(s, f'if items entity @s hotbar.{i} {pred}')
            by_data = _sense(s, f'if data entity @s SelectedItem.components{{"minecraft:frog/variant":"minecraft:{variant}"}}')
            s.check(f'the {variant} bucket keeps its variant (frog_variant component)', by_items or by_data,
                    f'server-side: if items hotbar.{i} {pred} = {by_items}; SelectedItem data = {by_data}')
        # first-person pictures of each bucket in the hand
        s.chat('kill @e[type=minecraft:frog,tag=uipfrog]', 0.3)
        _aim(s, feet, (x + 0.5, y - 1.0, z + 3.0), 0.8)
        s.key('F1', 0.5)
        try:
            for i, (variant, _name, _off) in enumerate(VARIANTS):
                s.io.key(str(i + 1), 0.6)
                s.shot(f'hand-{variant}')
        finally:
            s.key('F1', 0.5)
        # inventory with the tooltip of each bucket under the real pointer
        s.key('e', 1.2)
        st = s.wait_screen('InventoryScreen', 8)
        s.check('E opens the inventory', 'InventoryScreen' in st.get('screen', ''), st.get('screen'))
        s.shot('inventory')
        for i, (variant, _name, _off) in enumerate(VARIANTS):
            st = s.st()
            slot = s.slot_xy(st, cslot=i, own=True)
            if not slot or 'frog_bucket' not in slot.get('stack', ''):
                s.check(f'hotbar slot {i + 1} shows the {variant} frog bucket', False, slot)
                continue
            s.io.move(*s.abs(st, slot['x'] + 3, slot['y'] + 3))
            time.sleep(0.4)
            s.io.move(*s.abs(st, slot['x'], slot['y']))
            time.sleep(0.9)
            s.shot(f'tooltip-{variant}')
    finally:
        s.close()
        st = s.st()
        s.io.move(st['windowX'] + 640, st['windowY'] + 360)
        s.chat('kill @e[type=minecraft:frog,tag=uipfrog]', 0.3)
        s.chat('clear @s', 0.3)
        s.chat('gamemode creative @s', 0.5)


SCENARIOS = {'simplemobmanners': frog_bucket}
