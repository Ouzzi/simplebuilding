"""simplechallenges-4: what of Simple Challenges is reachable in the running creative test world with the REAL mouse
(scenario for modules/simpleinterfaces/tools/ui_probe.py).

NOT REACHABLE HERE (reported, not faked): the Select Challenge window (SelectChallengeScreen) and the Start-Area Editor (StartAreaEditorScreen) are
opened only from the "Create New World" screen (World tab, CreateWorldScreenMixin / WorldTabMixin); the driver boots straight into a flat world, so no
command, key or block opens them.  For them ui_probe would need a start phase on the title screen (or a probe command that opens CreateWorldScreen);
that is a Java/driver change, listed in the report.

SCENARIOS
  simplechallenges          (a) the handbook: client command `/simplechallenges guide` opens the BookViewScreen; the real mouse clicks the page-turn
                            buttons (PageButton: no label, told apart by position; the forward button is visible on the first page, the back button
                            appears after turning, disappears on the first page again).
                            (b) pictures of the Unlock grid in the running world: the admin lock (`allowAdminCommands`, off by default) is opened with
                            `/simplechallenges config set allowAdminCommands true`, `/simplechallenges activate vanilla_unlock` starts the free start
                            area (cells -1..0, 16 blocks each: x/z -16..15), the player stands at the east edge and looks over the border (red dust
                            line) at the locked cells (price plate under the crosshair), then everything is switched off again (deactivate, option false,
                            player back at his place) - in a try/finally, so a failure cannot leave the world locked for the next scenarios.
                            The status cannot show a HUD plate or particles, so (b) is pictures only (one check: that the first step ran); whether the
                            commands were accepted (option live? permission?) is visible only in the pictures.

Only a human can judge: border line, price plate (text, colours, contrast), what the locked cells look like, the guide's pages.
"""
import time

DZ = 50  # own area of this module (south of the music area); the unlock pictures walk to the world origin


def _xyz(st):
    return [int(v) for v in st['pos'].replace(',', ' ').split()]


def look(s, at, target, wait=0.9):
    """The player stands at 'at' and the EYES look at 'target' (tp ... facing turns from the feet: eye height 1.62 taken off)."""
    s.chat(f'tp @s {at[0]:.2f} {at[1]} {at[2]:.2f} facing {target[0]:.3f} {target[1] - 1.62:.3f} {target[2]:.3f}', wait)
    time.sleep(0.5)
    return s.st()


def page_buttons(s, st):
    """The visible PageButtons of the book screen, left to right (back first when both show)."""
    return sorted([w for w in s.widgets(st) if w['class'] == 'PageButton'], key=lambda w: w['x'])


def challenges(s):
    s.chat('tp @s ~ ~ ~' + str(DZ), 1.0)
    # ---- (a) the handbook with real page-turn clicks
    s.chat('simplechallenges guide', 1.2)
    st = s.wait_screen('BookViewScreen', 8)
    s.check('the client command opens the handbook (BookViewScreen)', 'BookViewScreen' in st.get('screen', ''), st.get('screen'))
    if 'BookViewScreen' in st.get('screen', ''):
        pb = page_buttons(s, st)
        s.check('first page: only the forward page button shows (right of the middle)', len(pb) == 1 and pb[0]['x'] > 640, [(w['x'], w['y']) for w in pb])
        s.shot('guide-first-page')
        if pb:
            for _ in range(3):
                pb = page_buttons(s, s.st())
                if pb:
                    s.click_xy(pb[-1]['x'], pb[-1]['y'], s.st())
            st = s.st()
            pb = page_buttons(s, st)
            s.check('after turning forward the back button shows as well', len(pb) == 2, [(w['x'], w['y']) for w in pb])
            s.shot('guide-page-4')
            for _ in range(3):
                pb = page_buttons(s, s.st())
                if len(pb) == 2:
                    s.click_xy(pb[0]['x'], pb[0]['y'], s.st())
            st = s.st()
            pb = page_buttons(s, st)
            s.check('turning back to the first page hides the back button again', len(pb) == 1, [(w['x'], w['y']) for w in pb])
        done = s.click_widget('Done')
        if done is not None:
            st = s.wait_screen('none', 6)
            s.check('Done closes the handbook', st.get('screen') == 'none', st.get('screen'))
    s.close()

    # ---- (b) pictures of the unlock grid (admin lock opened, always closed again)
    x, y, z = _xyz(s.st())
    opened = False
    try:
        s.chat('simplechallenges config set allowAdminCommands true', 1.0)
        s.chat('simplechallenges activate vanilla_unlock', 1.5)
        opened = True
        # the free area is x/z -16..15; stand 6 blocks inside its east edge and look east over the border
        look(s, (9.5, y, 8.5), (60.5, y + 0.2, 8.5), 1.5)
        time.sleep(2.0)  # the sync of the grid and the dust line need a moment
        s.shot('unlock-border-east')
        look(s, (9.5, y, 8.5), (20.5, y - 0.4, 8.5), 1.0)  # the first locked cell: the price plate shows under the crosshair
        time.sleep(1.5)
        s.shot('unlock-price-plate')
        look(s, (0.5, y, 0.5), (0.5, y + 0.2, 60.5), 1.0)  # towards the south edge
        time.sleep(1.0)
        s.shot('unlock-border-south')
        s.check('unlock pictures taken (commands sent; see the pictures for the effect)', True, 'pictures only')
    finally:
        if opened:
            s.chat('simplechallenges deactivate', 1.0)
        s.chat('simplechallenges config set allowAdminCommands false', 0.8)
        s.chat(f'tp @s {x + 0.5} {y} {z + 0.5}', 1.0)
    time.sleep(0.5)


SCENARIOS = {'simplechallenges': challenges}
