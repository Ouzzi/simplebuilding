"""N50 TZEDIT50: logic of the wiki dev page "Testzentrale-Editor" (wiki/assets/tzeditor.js) and its wiring in index.html."""
import json
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NODE = shutil.which('node')
sys.path.insert(0, str(ROOT / 'tools' / 'tzedit'))
import build_editor  # noqa: E402


class TzEditor(unittest.TestCase):
    @unittest.skipUnless(NODE, 'node not on PATH')
    def test_core(self):
        done = subprocess.run([NODE, str(ROOT / 'wiki' / 'tests' / 'tzeditor_core.cjs')], capture_output=True, text=True, timeout=60)
        self.assertEqual(done.returncode, 0, done.stderr)

    @unittest.skipUnless(NODE, 'node not on PATH')
    def test_zoomarea_core(self):
        done = subprocess.run([NODE, str(ROOT / 'wiki' / 'tests' / 'zoomarea_core.cjs')], capture_output=True, text=True, timeout=60)
        self.assertEqual(done.returncode, 0, done.stderr)

    def test_n72_pictures_and_zoom_wiring(self):
        html = (ROOT / 'wiki' / 'index.html').read_text(encoding='utf-8')
        js = (ROOT / 'wiki' / 'assets' / 'tzeditor.js').read_text(encoding='utf-8')
        # the page-wide image error handler leaves the editor's own candidate chain alone (it made every miss a "?")
        start = html.index("$main.addEventListener('error'")
        handler = html[start:html.index('}, true);', start)]
        self.assertLess(handler.index("hasAttribute('data-own-fallback')"), handler.index('replaceWith'))
        self.assertIn('i.dataset.ownFallback', js)
        self.assertIn("data/vanilla-icons.json", js)
        # candidates (N107 order): the dev host's model list, module picture, wiki picture, name guesses
        body = js[js.index('function candidates(id)'):js.index('function crop(')]
        order = [body.index(k) for k in ('st.icons[id]', 'st.idx.textures[id]', 'ctx.icon(full)', "'assets/vanilla/block/'")]
        self.assertEqual(order, sorted(order))
        # N110 pre-check: a module id the host's list names -> its module picture is known missing there, never asked for;
        # a picture file that failed once is never asked for again
        self.assertIn("if (!(dev && nsOf(full) !== 'minecraft')) add(st.idx && st.idx.textures[id]);", body)
        self.assertIn("return out.filter(function (u) { return !BAD[u.split('#')[0]]; });", body)
        # one shared zoom component, loaded by the page, used by the editor grid and the big lists
        self.assertIn('<script src="assets/zoomarea.js"></script>', html)
        self.assertIn("Z.attach(main, { key: 'tzeditor'", js)
        for key in ('data-zoom="textures"', 'data-zoom="allrecipes"', 'data-zoom="recipes"', "data-zoom=\"list-", "'table-' + r.section"):
            self.assertIn(key, html)
        self.assertIn('window.SBZoom.scan($main, LANG)', html)

    def test_n110_missing_pictures(self):
        """Owner 19.11. (N99B): missing pictures get a clean fallback symbol and the list "ohne Bild" / "without picture"."""
        js = (ROOT / 'wiki' / 'assets' / 'tzeditor.js').read_text(encoding='utf-8')
        self.assertIn("noPic: 'ohne Bild'", js)
        self.assertIn("noPic: 'without picture'", js)
        self.assertNotIn('ohne sie der Name', js)  # old legend: the bare name instead of a picture
        img = js[js.index('    function img(id, cls) {'):js.index('/* one cell; f/r/c')]
        # every path without a picture ends in the symbol (never null -> never a bare name or a broken image)
        self.assertEqual(img.count('return noPic(id, cls)'), 2)
        self.assertIn('i.replaceWith(noPic(id, cls))', img)
        self.assertIn('BAD[cands[k].split', img)
        self.assertIn("e.appendChild(img(id));", js)
        self.assertIn("e.appendChild(img(opt.top, 'tz-t'));", js)
        # the symbol is an own drawing (SVG), no game pixels; the list jumps to the cell
        self.assertIn('data:image/svg+xml', js)
        self.assertIn("closest('.tz-nopic [data-npj]')", js)
        self.assertIn('function jumpNoPic(i)', js)

    def test_n72b_menu_replaces_the_wiki_menu(self):
        html = (ROOT / 'wiki' / 'index.html').read_text(encoding='utf-8')
        js = (ROOT / 'wiki' / 'assets' / 'tzeditor.js').read_text(encoding='utf-8')
        start = html.index("document.addEventListener('contextmenu'")
        body = html[start:html.index('});', start)]
        self.assertLess(body.index('ev.defaultPrevented'), body.index('openCtx('))
        self.assertIn("page.addEventListener('contextmenu'", js)
        for key in ("'ContextMenu'", "e.key === 'F10'", "role', 'menu'", "'*all'", "data/testcentre/plan.json"):
            self.assertIn(key, js)

    @unittest.skipUnless(NODE, 'node not on PATH')
    def test_as_built_matches_the_game_plan(self):
        """The view "as built" (tzeditor.builtPlan) puts every line row on the side and at the place the game dump has."""
        plan = json.loads((ROOT / 'wiki/data/testcentre/plan.json').read_text(encoding='utf-8'))
        layouts = {a: [r['key'] for r in json.loads((ROOT / build_editor.LAYOUT_DIR / f'layout_lines_{a}.json').read_text(encoding='utf-8'))['rows']]
                   for a in ('metals', 'wood', 'aging', 'stone')}
        if set(sum(layouts.values(), [])) != set(plan['lines']['side']):
            self.skipTest('layout rows changed since the plan (python3 tools/tzedit/build_editor.py --plan <dump>)')
        script = ("const T=require(process.argv[1]);const d=JSON.parse(process.argv[2]);"
                  "const by={};for(const a in d.l)by[a]=d.l[a].map(k=>({key:k,family:d.f[k]||k}));"
                  "const b=T.builtPlan(by,d.p);console.log(JSON.stringify({a:b.a.map(r=>r.key),b:b.b.map(r=>r.key)}));")
        arg = json.dumps({'l': layouts, 'f': plan['lines']['family'], 'p': plan['lines']['perPair']})
        out = subprocess.run([NODE, '-e', script, str(ROOT / 'wiki/assets/tzeditor.js'), arg], capture_output=True, text=True, timeout=60)
        self.assertEqual(out.returncode, 0, out.stderr)
        got = json.loads(out.stdout)
        sides = plan['lines']['side']
        for side in ('A', 'B'):
            want = [k for k, v in sorted(sides.items(), key=lambda kv: kv[1][1]) if v[0] == side]
            self.assertEqual(got[side.lower()], want, side)

    def test_plan_from_dump(self):
        with tempfile.TemporaryDirectory() as tmp:
            repo, dump = Path(tmp) / 'repo', Path(tmp) / 'dump'
            (repo / build_editor.LAYOUT_DIR).mkdir(parents=True)
            (repo / build_editor.GAME).mkdir(parents=True)
            (repo / 'wiki/data').mkdir(parents=True)
            dump.mkdir()
            (repo / build_editor.GAME / 'TestCentreLayout.java').write_text(
                'excluded.put("simplebuilding:copper_fire", "Feuer ohne Item");\n', encoding='utf-8')
            (repo / build_editor.LAYOUT_DIR / 'layout_lines_metals.json').write_text(json.dumps({'rows': [{'key': 'iron'}]}))
            (repo / build_editor.LAYOUT_DIR / 'layout_blocks_compact.json').write_text(json.dumps({'rows': [{'key': 'a', 'cells': ['m:a_block']}]}))
            (repo / 'wiki/data/m.json').write_text(json.dumps({'blocks': [{'id': 'm:a_block'}, {'id': 'm:x_wall_sign'}, {'id': 'm:x_sign'},
                                                                         {'id': 'simplebuilding:copper_fire'}, {'id': 'm:lever_thing'}]}))
            (dump / 'lines-rows.tsv').write_text('\n'.join([
                '#lineZ\t5', 'iron\tMETAL\tiron\tA\t2\t3\t0\tBUTTON\tminecraft:iron_block\t',
                'iron\tMETAL\tiron\tA\t2\t2\t1\tPLATE\tMISSING\t', 'moss\tREST\tmoss\tA\t3\t3\t0\tBLOCK\tminecraft:moss_block\t',
                'new:metal\tMETAL\tx\tA\t4\t3\t0\tBLOCK\tm:fresh\t', '#label\tmoss\t3\t3\tMoss Block']) + '\n')
            (dump / 'compact-cells.tsv').write_text('m\t0\tm:a_block\t2\t0\t3\tplace\nm\t1\tm:x_sign\t3\t0\t3\tplace\nm\t2\tm:lever_thing\t3\t0\t3\tplace\n')
            plan = build_editor.plan_from_dump(repo, dump)
        self.assertEqual(plan['lines']['family'], {'iron': 'iron', 'moss': 'moss'})
        self.assertEqual(plan['lines']['rest'], [{'key': 'moss', 'label': 'Moss Block', 'family': 'moss', 'cells': ['minecraft:moss_block']}])
        self.assertEqual(plan['lines']['fresh'], ['m:fresh'])
        self.assertEqual(plan['compact']['expected'], {'m': ['m:a_block', 'm:x_sign', 'm:lever_thing']})
        reasons = {i: e['reason'] for e in plan['exceptions'] for i in e['ids']}
        self.assertIn('Wandvariante', reasons['m:x_wall_sign'])
        self.assertEqual(reasons['simplebuilding:copper_fire'], 'Feuer ohne Item')
        self.assertNotIn('m:x_sign', reasons)

    def test_dev_only_wiring(self):
        html = (ROOT / 'wiki' / 'index.html').read_text(encoding='utf-8')
        self.assertIn("{ key: 'tzeditor', labelKey: 'navTzEditor'", html)
        start = html.index('function pageTzEditor()')
        body = html[start:html.index('\n}\n', start)]
        self.assertLess(body.index('isDevSite()'), body.index('assets/tzeditor.js'))  # nothing loads on the public wiki
        self.assertIn('noindex', body)


if __name__ == '__main__':
    unittest.main()
