"""simplelib/experimental-warning (EXP52 = A, b-exp61): re-opening a world whose only non-Vanilla dimensions are the Simple
mods' own asks no "experimental settings" / backup question.

Scenario for modules/simpleinterfaces/tools/ui_probe.py. It leaves the shared probe world and opens it again, so it runs ONLY
when asked by name (it would otherwise move every later scenario through a world reload):

    xvfb-run python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe --only simplelib/experimental-warning

Flow (real mouse/keys): Escape -> 'Save and Quit to Title' -> 'Singleplayer' -> first (= most recent = probe) world ->
'Play Selected World' -> every screen until the world is open is recorded; a BackupConfirmScreen / ConfirmScreen is the warning.
Also checks the client log for SimpleLib's decision line ("Experimental warning skipped (open)"). Needs SortProbe's status
without a player (b-exp61). Not run yet (sb-test down when written).
"""
import sys
import time

NAME = 'simplelib/experimental-warning'


def _asked_by_name():
    argv = sys.argv
    if '--only' in argv:
        i = argv.index('--only')
        return i + 1 < len(argv) and NAME in argv[i + 1].split(',')
    return any(a.startswith('--only=') and NAME in a.split('=', 1)[1].split(',') for a in argv)


def _wait_for(s, fragment, seconds):
    end = time.time() + seconds
    st = s.st()
    while time.time() < end:
        st = s.st()
        if fragment in st.get('screen', ''):
            return st
        time.sleep(0.3)
    return st


def reopen(s):
    if not _asked_by_name():
        s.check('skipped (run with --only ' + NAME + ')', True, 'reloads the shared world')
        return
    s.close()
    s.key('Escape', 1.0)
    st = _wait_for(s, 'PauseScreen', 8)
    s.check('pause screen open', 'PauseScreen' in st.get('screen', ''), st.get('screen'))
    if not s.click_widget('Save and Quit'):
        return
    st = _wait_for(s, 'TitleScreen', 60)
    s.check('back on the title screen', 'TitleScreen' in st.get('screen', ''), st.get('screen'))
    if not s.click_widget('Singleplayer'):
        return
    st = _wait_for(s, 'SelectWorldScreen', 20)
    s.check('world list open', 'SelectWorldScreen' in st.get('screen', ''), st.get('screen'))
    lst = s.find_widget(cls='WorldSelectionList', st=st)
    if not lst:
        s.check('world list widget present', False, '; '.join(s.labels(st))[:500])
        return
    # first row: list top + 4 GUI px padding + half the 36 px row height
    top = lst['y'] - lst['h'] / 2
    s.click_xy(lst['x'], top + 22 * st.get('scale', 2), st)
    time.sleep(0.6)
    s.shot('world-list')
    if not s.click_widget('Play Selected World'):
        return
    seen, end, st = [], time.time() + 180, s.st()
    while time.time() < end:
        st = s.st()
        screen = st.get('screen', 'none')
        if not seen or seen[-1] != screen:
            seen.append(screen)
        if 'ConfirmScreen' in screen:  # BackupConfirmScreen (open) or ConfirmScreen: the warning
            break
        if screen == 'none' and 'pos' in st:
            break
        time.sleep(0.3)
    warned = [x for x in seen if 'ConfirmScreen' in x]
    s.check('no experimental / backup question when re-opening', not warned, ' -> '.join(seen))
    if warned:
        s.shot('warning')
        # get back into the world anyway: "I know what I'm doing!" (Vanilla) / the plain confirm button
        if not s.click_widget("know what"):
            s.click_widget('Proceed') or s.click_widget('Yes')
        st = _wait_for(s, 'none', 180)
    s.check('world open again', st.get('screen') == 'none' and 'pos' in st, st.get('screen'))
    try:
        log = (s.out / 'client.log').read_text(errors='replace')
    except OSError:
        log = ''
    s.check('SimpleLib logged the decision (open)', 'Experimental warning skipped (open)' in log,
            next((ln for ln in log.splitlines() if 'Experimental warning' in ln), 'no decision line (stable world or mixin missing)')[:300])
    s.shot('reopened')


SCENARIOS = {NAME: reopen}
