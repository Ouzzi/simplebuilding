"""N136 release polish, mods 53-56 (offen-vor-release.md section 3: Simple Visuals, Simple Sounds, Simple Models, Simple Aesthetics),
word for word against the "Szenen" lines and the UI cells:

Simple Visuals   "Szenen: HUD-Anzeigen, Partikel (W4C-02)", UI "? Amboss-Mixin"
  n136-visuals-hud      HUD displays in one survival arena: pickup notifier (an item entity with PickupDelay 0 at the feet -> the stack
                        is in the inventory, shot of the row), status effect bars (Speed -> HUD shot; REAL E -> InventoryScreen with the
                        effect list, shot), held item information (enchanted, damaged sword selected with the REAL number key, shot of
                        name + durability + enchantments), damage numbers (a NoAI cow in front, /damage 4 -> its health 6 read back, shot
                        within the 2 s the number rises), speed lines (threshold lowered to 0.05 through the client command and read back,
                        Speed X, REAL W + sprint held -> shot), biome notification (switched on, /fillbiome desert beside the arena, tp
                        in -> shot of the name). All looks are shots for the owner (W4C-02); defaults restored and read back.
  n136-visuals-chat-map Chat heads (/say -> head beside the line in the chat margin, shot), own death coordinates (survival /kill: the
                        death message in client.log carries "Location: [x, y, z]" with the death position, separate-line mode; REAL
                        click on Respawn), map preview (empty map, REAL right click in the air -> filled map in the hand; REAL E, pointer
                        on the map slot -> hover reports the filled map, shot of the 128-pixel preview tooltip).
  n136-visuals-particles Immersion particles: campfire, beacon, flower and leaves beside the player, enchanted sword in hand, global
                        level MAXIMUM (client command, read back) -> REAL F5 to third person, shots while standing and walking; level
                        OFF -> shot (no extra particles); level back to Subtle (read back), F5 back to first person.
  n136-visuals-anvil    First the shared command root: "/simplevisuals config visuals enableAnvilFormatting" and "... allowAnvilObfuscation
                        false" typed as a client command must reach the server (its answer "Server anvil formatting: true" /
                        "Server anvil obfuscation (&k): false" in client.log). UI cell "Amboss-Mixin": anvil, REAL right click (crosshair read back) -> AnvilScreen; REAL clicks put a sword
                        with repair cost 3 into the first slot -> shot (the screen shows "Prior anvil uses: 2" while there is no result);
                        the name box (EditBox) cleared and "&cruby" typed with REAL keys -> box value read back; REAL clicks take the
                        result into hotbar slot 2; selected -> main hand components carry the custom name "ruby" in red (server anvil
                        formatting on by default).
Simple Visuals + Simple Sounds (N136 rewrote most config texts)
  n136-config-screens   Both Cloth screens the player's way (Escape -> Mods -> search -> Configure..., N124 helper), English AND
                        German: title, the first five tabs (Visuals: General, Speed lines, Pickups, Damage, Biomes; Sounds: General,
                        Movement, Weather, Nature, Environment - Cloth scrolls the tab bar) clicked and photographed, every name a lang text of the mod, every option
                        with its own tooltip; German names mostly German. New names checked: "Speed Lines Opacity"/"Linien-Deckkraft",
                        "Text shadow"/"Textschatten", Sounds "Effect levels" is no screen entry (map, generic screen only).
Simple Sounds    No scene: the probe client has no audio device (OpenAL fails), so playback, subtitles and the twelve triggers are
                 gametests (soundTriggers, effect*, cooldownAndWorldReset, soundFloodSafety). Listening stays W4C-44 (Vanilla sounds,
                 no synthetic sound -> no listening page).
Simple Models    "Szenen: Editor + Browser beurteilen (CL-7, W4C-31)": the existing scenario 'simplemodels' (uiprobe_simplemodels.py,
                 browser + editor with the real mouse) - run it in the same command; judging the look stays with the owner.
Simple Aesthetics "Sodium+Iris auf echter Grafik (W4C-01)": owner hardware only, no scene.

Patterns (uiprobe_n122-n135 lessons): difficulty normal in _prep; position via int(float()) of status 'pos'; 'tp ... facing' with
y - 1.62, targets <= 4 blocks, crosshair read back before every real click; main hand = selected hotbar slot, cleared before giving;
chat lines read from client.log; respawn is at the world spawn -> tp back. No game code is changed.

    timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe136 --scale 2 \\
        --only n136-visuals-hud,n136-visuals-chat-map,n136-visuals-particles,n136-visuals-anvil,n136-config-screens,simplemodels
"""
import importlib.util
import re
import time
from pathlib import Path

TAG = 'N136'
FLOOR = 'minecraft:smooth_stone'
TOOLS = Path(__file__).resolve().parent
REPO = TOOLS.parents[2]


def _load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


rp = _load('release_probe_n136', REPO / 'modules/simplelib/tools/release_probe.py')
n124 = _load('uiprobe_n124_for_n136', TOOLS / 'uiprobe_n124.py')


# ---------------------------------------------------------------- helpers
def _pos(s):
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def _log(s):
    p = s.out / 'client.log'
    return p.read_text(errors='replace') if p.exists() else ''


_seq = [0]


def _test(s, cond, tag):
    """One server condition, read back at once through a unique say tag (True/False)."""
    _seq[0] += 1
    t = f'{tag}-{_seq[0]}'
    s.chat(f'execute {cond} run say {TAG} {t}', 0.35)
    time.sleep(1.2)
    return bool(re.search(TAG + ' ' + re.escape(t) + r'(?![\w-])', _log(s)))


def _poll(s, cond, tag, seconds):
    end = time.time() + seconds
    while time.time() < end:
        if _test(s, cond, tag):
            return True
    return False


def _client_value(s, path):
    """Reads a Simple Visuals option through its client command (/simplevisuals config <path> prints the value to the chat)."""
    before = len(_log(s))
    s.chat('simplevisuals config ' + path.replace('.', ' '), 1.2)
    new = _log(s)[before:]
    m = None
    for m in re.finditer(r'\[CHAT\] ([^\r\n]+)', new):
        pass
    return m.group(1).strip() if m else None


def _set_client(s, path, value, want):
    s.chat(f'simplevisuals config {path.replace(".", " ")} {value}', 0.8)
    got = _client_value(s, path)
    s.check(f'client command sets {path} = {want} (read back)', got == want, got)
    return got == want


def _prep(s, mode='creative'):
    s.close()
    for k in ('Shift_L', 'space', 'w', 'Control_L'):
        s.io.key_up(k)
    s.chat('ride @s dismount', 0.3)
    s.chat(f'gamemode {mode} @s', 0.3)
    s.chat('difficulty normal', 0.3)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false', 'send_command_feedback true', 'random_tick_speed 0'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('weather clear', 0.3)
    s.chat('effect clear @s', 0.3)
    s.chat('clear @s', 0.3)


def _done(s):
    s.close()
    for k in ('Shift_L', 'space', 'w', 'Control_L'):
        s.io.key_up(k)
    _center_pointer(s)
    s.chat('effect clear @s', 0.3)
    s.chat('clear @s', 0.3)
    s.chat('gamemode creative @s', 0.3)
    s.chat('gamerule random_tick_speed 3', 0.3)


def _arena(s, dx, x0=-4, z0=-4, x1=8, z1=8, height=7):
    x, y, z = _pos(s)
    x += dx
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 0', 1.2)
    s.chat(f'fill {x + x0} {y} {z + z0} {x + x1} {y + height} {z + z1} minecraft:air', 0.6)
    s.chat(f'fill {x + x0} {y - 1} {z + z0} {x + x1} {y - 1} {z + z1} {FLOOR}', 0.6)
    s.chat('kill @e[type=!minecraft:player,distance=..30]', 0.3)
    time.sleep(1.0)
    return x, y, z


def _aim(s, at, target, want, tag, tries=4, key='hitBlock'):
    hit = None
    for _ in range(tries):
        s.chat(f'tp @s {at[0]:.2f} {at[1]} {at[2]:.2f} facing {target[0]:.3f} {target[1] - 1.62:.3f} {target[2]:.3f}', 0.8)
        for _ in range(6):
            hit = s.st().get(key)
            if hit == want:
                return True
            time.sleep(0.3)
    s.check(f'{tag}: crosshair on {want}', False, hit)
    return False


def _hit_is(s, want, tag, key='hitBlock', wait=2.0):
    end, hit = time.time() + wait, None
    while time.time() < end:
        hit = s.st().get(key)
        if hit == want:
            return True
        time.sleep(0.25)
    s.check(f'{tag}: crosshair on {want} before the click', False, hit)
    return False


def _hand(s, slot, stack, tag):
    s.chat(f'item replace entity @s hotbar.{slot} with minecraft:air', 0.3)
    s.chat(f'item replace entity @s hotbar.{slot} with {stack}', 0.4)
    s.io.key(str(slot + 1), 0.5)
    item = stack.split('[', 1)[0].split(' ', 1)[0]
    ok = _test(s, f'if items entity @s weapon.mainhand {item}', f'{tag}-mainhand')
    s.check(f'{tag}: {item} is in the main hand (selected hotbar slot {slot + 1})', ok)
    return ok


def _click(s, after=0.7, button=3):
    s.io.button(button, True)
    time.sleep(0.12)
    s.io.button(button, False)
    time.sleep(after)


def _center_pointer(s):
    st = s.st()
    if 'windowX' in st:
        s.io.move(st['windowX'] + 640, st['windowY'] + 360)
        time.sleep(0.3)


def _total(s, item):
    n = 0
    for v in s.st().get('inventory', []):
        if v and v.split(' x')[0] == item:
            n += int(v.rsplit(' x', 1)[1]) if ' x' in v else 1
    return n


# ---------------------------------------------------------------- Simple Visuals: HUD
def visuals_hud(s):
    _prep(s, 'survival')
    x, y, z = _arena(s, 160, x0=-4, z0=-4, x1=24, z1=8)
    try:
        # pickup notifier: a server pickup (TakeItemEntity) of 3 diamonds
        s.chat(f'summon minecraft:item {x + 0.5} {y} {z + 0.5} {{Item:{{id:"minecraft:diamond",count:3}},PickupDelay:0}}', 0.6)
        end = time.time() + 5
        while _total(s, 'minecraft:diamond') < 3 and time.time() < end:
            time.sleep(0.3)
        s.check('pickup: the 3 diamonds were picked up (server pickup -> notifier row)', _total(s, 'minecraft:diamond') == 3, s.st().get('inventory'))
        s.shot('1-pickup-row')
        # status effect bars: HUD, then the inventory list
        s.chat('effect give @s minecraft:speed 60 1', 0.8)
        s.chat('effect give @s minecraft:jump_boost 20 0', 0.8)
        s.shot('2-effect-bars-hud')
        s.io.key('e', 1.0)
        st = s.wait_screen('InventoryScreen', 6)
        s.check('REAL E opens the survival inventory (effect bars beside the effect list)', 'InventoryScreen' in st.get('screen', ''), st.get('screen'))
        s.shot('3-effect-bars-inventory')
        s.close()
        # held item information
        if _hand(s, 1, 'minecraft:diamond_sword[enchantments={sharpness:3,unbreaking:2,looting:1},damage=100]', 'held'):
            s.io.key('1', 1.0)
            s.io.key('2', 0.6)
            s.shot('4-held-item-info')
        # damage numbers on a NoAI cow 3 blocks ahead
        s.chat(f'summon minecraft:cow {x + 0.5} {y} {z + 3.5} {{NoAI:1b,Silent:1b}}', 0.8)
        s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {x + 0.5} {y + 0.7 - 1.62:.2f} {z + 3.5}', 1.2)
        s.chat('damage @e[type=minecraft:cow,limit=1,sort=nearest] 4 minecraft:generic', 0.2)
        s.shot('5-damage-number')
        s.check('damage: the cow lost 4 health (10 -> 6)', _test(s, 'if entity @e[type=minecraft:cow,limit=1,sort=nearest,nbt={Health:6.0f}]', 'cow-6'))
        print('NOTE n136 hud: the damage number is drawn for 40 ticks; the shot follows the command by about 0.3 s', flush=True)
        s.chat('kill @e[type=minecraft:cow,distance=..20]', 0.3)
        # speed lines: threshold 0.05, Speed X, REAL W + sprint
        if _set_client(s, 'visuals.speedLines.speedThreshold', '0.05', '0.05'):
            s.chat('effect give @s minecraft:speed 20 9 true', 0.4)
            s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} -90 0', 1.0)
            s.io.key_down('Control_L')
            s.io.key_down('w')
            try:
                time.sleep(1.5)
                s.shot('6-speed-lines')
                moved = _pos(s)[0] - x
            finally:
                s.io.key_up('w')
                s.io.key_up('Control_L')
            s.check('speed lines: REAL W moved the player east (lines need speed)', moved >= 4, f'{moved} blocks')
            _set_client(s, 'visuals.speedLines.speedThreshold', '0.6', '0.6')
        s.chat('effect clear @s', 0.3)
        # biome notification: desert patch at the east end, notification switched on
        if _set_client(s, 'visuals.biomeInfo.enable', 'true', 'true'):
            s.chat(f'fillbiome {x + 14} {y - 3} {z - 4} {x + 24} {y + 6} {z + 8} minecraft:desert', 1.0)
            s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} -90 0', 4.0)  # the current biome is shown first (3 s), then the change
            s.chat(f'tp @s {x + 19.5} {y} {z + 0.5} -90 0', 0.9)
            s.shot('7-biome-desert')
            s.check('biome: the desert patch is in place', _test(s, f'positioned {x + 19} {y} {z} if biome ~ ~ ~ minecraft:desert', 'desert'))
            _set_client(s, 'visuals.biomeInfo.enable', 'false', 'false')
    finally:
        _done(s)


# ---------------------------------------------------------------- Simple Visuals: chat heads, death coordinates, map preview
def visuals_chat_map(s):
    _prep(s, 'survival')
    x, y, z = _arena(s, 200)
    home = (x, y, z)
    try:
        s.chat(f'say {TAG} head beside this line', 1.0)
        s.shot('1-chat-head')
        mode = _client_value(s, 'visuals.deathCoordsMode')
        s.check('own death coordinates default to a separate local line', mode == 'SEPARATE', mode)
        before = len(_log(s))
        p = _pos(s)
        s.chat('kill @s', 1.5)
        st = s.wait_screen('DeathScreen', 8)
        s.check('/kill shows the death screen', 'DeathScreen' in st.get('screen', ''), st.get('screen'))
        s.shot('2-death-screen')
        new = _log(s)[before:]
        m = re.search(r'Location: \[(-?\d+), (-?\d+), (-?\d+)\]', new)
        s.check('the death message carries "Location: [x, y, z]" (client.log)', m is not None, new[-400:])
        if m:
            got = [int(v) for v in m.groups()]
            s.check('the location is the death position', all(abs(a - b) <= 1 for a, b in zip(got, p)), f'{got} vs {p}')
        s.click_widget('Respawn', name='death screen button "Respawn"')
        time.sleep(2.0)
        s.chat(f'tp @s {home[0] + 0.5} {home[1]} {home[2] + 0.5} 0 0', 2.0)
        # map preview tooltip
        if _hand(s, 0, 'minecraft:map', 'map'):
            time.sleep(1.0)
            _click(s, 1.5)
            filled = _poll(s, 'if items entity @s weapon.mainhand minecraft:filled_map', 'filled', 6)
            s.check('REAL right click turns the empty map into a filled map', filled)
            if filled:
                time.sleep(2.0)
                s.io.key('e', 1.0)
                st = s.wait_screen('InventoryScreen', 6)
                slot = next((v for v in st.get('slots', []) if v['own'] and v['stack'].startswith('minecraft:filled_map')), None)
                s.check('the filled map is in an inventory slot', slot is not None, [v['stack'] for v in st.get('slots', []) if v['own']][:9])
                if slot:
                    rp.hover(s, st, slot['x'], slot['y'], '3-map-preview')
                    hv = s.st().get('hover', {})
                    s.check('the pointer rests on the filled map (preview tooltip in the shot)', hv.get('stack', '').startswith('minecraft:filled_map'), hv)
                s.close()
    finally:
        _done(s)


# ---------------------------------------------------------------- Simple Visuals: immersion particles
def visuals_particles(s):
    _prep(s)
    x, y, z = _arena(s, 240)
    third = False
    try:
        s.chat(f'setblock {x + 1} {y} {z} minecraft:campfire', 0.3)
        s.chat(f'setblock {x - 1} {y} {z} minecraft:beacon', 0.3)
        s.chat(f'setblock {x} {y - 1} {z - 1} minecraft:grass_block', 0.3)
        s.chat(f'setblock {x} {y} {z - 1} minecraft:poppy', 0.3)
        s.chat(f'setblock {x + 1} {y + 2} {z} minecraft:oak_leaves[persistent=true]', 0.3)
        _hand(s, 0, 'minecraft:diamond_sword[enchantments={sharpness:1}]', 'particles')
        if not _set_client(s, 'particles.globalLevel', 'maximum', 'MAXIMUM'):
            return
        s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 30 20', 1.0)
        s.io.key('F5', 1.0)
        third = True
        time.sleep(2.0)
        s.shot('1-maximum-standing')
        time.sleep(1.5)
        s.shot('2-maximum-standing-later')
        _set_client(s, 'particles.globalLevel', 'off', 'OFF')
        time.sleep(3.0)
        s.shot('3-off')
        print('NOTE n136 particles: the particle count is not in the status; compare shots 1/2 (maximum) with 3 (off) - W4C-02', flush=True)
    finally:
        _set_client(s, 'particles.globalLevel', 'subtle', 'SUBTLE')
        if third:  # third person back -> front -> first person
            s.io.key('F5', 0.6)
            s.io.key('F5', 0.6)
        _done(s)


# ---------------------------------------------------------------- Simple Visuals: anvil (UI cell "Amboss-Mixin")
def _server_says(s, command, pattern, tag):
    """Sends a command through the client dispatcher (Fabric client commands first) and waits for the server's answer in client.log."""
    before = len(_log(s))
    s.chat(command, 1.5)
    new = _log(s)[before:]
    ok = re.search(pattern, new) is not None
    s.check(f'{tag}: "/{command}" reaches the server (answer in the chat)', ok, new[-300:])
    return ok


def visuals_anvil(s):
    _prep(s)
    x, y, z = _arena(s, 280)
    try:
        # N136: the client root /simplevisuals shares its name with the server's; both server policies must still arrive
        _server_says(s, 'simplevisuals config visuals enableAnvilFormatting', r'Server anvil formatting: ON', 'server policy get')
        _server_says(s, 'simplevisuals config visuals allowAnvilObfuscation false', r'Server anvil obfuscation \(&k\): OFF', 'server policy set')
        s.chat(f'setblock {x} {y} {z + 3} minecraft:anvil', 0.6)
        if not _hand(s, 0, 'minecraft:diamond_sword[repair_cost=3]', 'anvil'):
            return
        me, centre = (x + 0.5, y, z + 0.5), (x + 0.5, y + 0.7, z + 3.5)
        if not (_aim(s, me, centre, 'minecraft:anvil', 'anvil') and _hit_is(s, 'minecraft:anvil', 'anvil')):
            return
        _click(s, 0.6)
        st = s.wait_screen('AnvilScreen', 8)
        s.check('REAL right click opens the anvil (AnvilScreen)', 'AnvilScreen' in st.get('screen', ''), st.get('screen'))
        if 'AnvilScreen' not in st.get('screen', ''):
            return
        st = s.click_slot(cslot=0, own=True)
        s.check('REAL click lifts the sword', st and st.get('carried', '').startswith('minecraft:diamond_sword'), st and st.get('carried'))
        st = s.click_slot(cslot=0, own=False)
        first = st and s.slot_xy(st, 0, False)
        s.check('REAL click puts it into the first anvil slot', first and first['stack'].startswith('minecraft:diamond_sword'), first and first['stack'])
        _center_pointer(s)
        time.sleep(0.8)
        s.shot('1-prior-uses')
        print('NOTE n136 anvil: "Prior anvil uses: 2" (repair cost 3) is drawn by AnvilScreenMixin while there is no result - shot 1', flush=True)
        box = s.find_widget(cls='EditBox')
        if not box:
            s.check('anvil name box (EditBox) present', False, s.labels())
            return
        s.click_xy(box['x'], box['y'])
        s.io.key_down('Control_L')
        s.io.key('a', 0.3)
        s.io.key_up('Control_L')
        s.io.key('BackSpace', 0.3)
        # '&' is Shift+7 on the X server's US layout (the keysym alone maps to the 7 key and typed "7")
        s.io.key_down('Shift_L')
        s.io.key('7', 0.2)
        s.io.key_up('Shift_L')
        for k in ('c', 'r', 'u', 'b', 'y'):
            s.io.key(k, 0.2)
        time.sleep(0.8)
        st = s.st()
        box = s.find_widget(cls='EditBox', st=st)
        s.check('REAL keys typed "&cruby" into the name box', box and box.get('value') == '&cruby', box and box.get('value'))
        result = s.slot_xy(st, 2, False)
        s.check('the anvil offers a result', result and result['stack'].startswith('minecraft:diamond_sword'), result and result['stack'])
        s.shot('2-renamed-result')
        st = s.click_slot(cslot=2, own=False)
        s.check('REAL click takes the result', st and st.get('carried', '').startswith('minecraft:diamond_sword'), st and st.get('carried'))
        s.click_slot(cslot=1, own=True)
        s.close()
        s.io.key('2', 0.8)
        comps = s.st().get('mainhandComponents', '')
        # Visuals.formatName turns valid & codes into section-sign codes: the name is the literal "§cruby" (red)
        s.check('the taken sword is named "ruby" in red (server anvil formatting, &c -> §c)', '§cruby' in comps or ('ruby' in comps and 'red' in comps), comps[:300])
        s.check('"&c" itself is not left in the name', '&c' not in comps, comps[:300])
    finally:
        _done(s)


# ---------------------------------------------------------------- Simple Visuals + Simple Sounds: config screens EN/DE
# The first five tabs of each screen (14 and 8 tabs in all; Cloth scrolls the tab bar, later tabs may sit outside the window)
VISUALS_TABS = [f'simplevisuals.tab.{t}' for t in ('general', 'speed', 'pickup', 'damage', 'biome')]
SOUNDS_TABS = [f'simplesounds.tab.{t}' for t in ('general', 'movement', 'weather', 'nature', 'environment')]
NEW_NAMES = {'en_us': ('Speed Lines Opacity', 'Text shadow'), 'de_de': ('Linien-Deckkraft', 'Textschatten')}


def config_screens(s):
    _prep(s)
    seen = {}

    def run(loc):
        v = n124._config_pass(s, 'simplevisuals', 'Simple Visuals', loc, 'text.autoconfig.simplevisuals.title', VISUALS_TABS)
        seen[('visuals', loc)] = v
        for name in NEW_NAMES[loc]:
            s.check(f'visuals {loc}: shows "{name}" (N136 name)', name in v, v[:60])
        so = n124._config_pass(s, 'simplesounds', 'Simple Sounds', loc, 'simplesounds.title', SOUNDS_TABS)
        seen[('sounds', loc)] = so
    n124._both_languages(s, run)
    for mod in ('visuals', 'sounds'):
        a, b = seen.get((mod, 'en_us'), []), seen.get((mod, 'de_de'), [])
        s.check(f'{mod}: both languages list the same number of options', a and len(a) == len(b), (len(a), len(b)))


SCENARIOS = {
    'n136-visuals-hud': visuals_hud,
    'n136-visuals-chat-map': visuals_chat_map,
    'n136-visuals-particles': visuals_particles,
    'n136-visuals-anvil': visuals_anvil,
    'n136-config-screens': config_screens,
}
