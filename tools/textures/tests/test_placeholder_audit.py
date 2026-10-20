"""Grey placeholder textures (b-texfix20, owner 19.10.): the detector of placeholder_audit.py, the jar marker, the build's
Python probe and why it must not be python -c "<code with quotes>" on Windows, and Windows-safe repository paths.

Run: python3 -m unittest discover -s tools/textures/tests  (Gradle: testTextureTools, part of check)
"""
import contextlib
import io
import re
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parents[1]
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import vanilla_derived as vd  # noqa: E402

try:
    from PIL import Image
    import placeholder_audit as pa
except ImportError:  # the detector needs Pillow; the probe/path tests below run without it
    Image = pa = None


def png(pixels, w, h) -> bytes:
    im = Image.new('RGBA', (w, h))
    im.putdata(pixels)
    buf = io.BytesIO()
    im.save(buf, format='PNG')
    return buf.getvalue()


def checker(w, h, light, dark) -> bytes:
    step = max(1, w // 8)
    return png([light if ((x // step) + (y // step)) % 2 else dark for y in range(h) for x in range(w)], w, h)


@unittest.skipIf(pa is None, 'Pillow missing')
class Detector(unittest.TestCase):
    def test_every_python_placeholder_is_detected(self):
        for rel in vd.OUTPUTS:
            self.assertTrue(pa.is_placeholder(vd.placeholder(rel)), rel)
        for module, outputs in vd.MODULE_OUTPUTS.items():
            for rel in outputs:
                self.assertTrue(pa.is_placeholder(vd.placeholder(rel, module)), f'{module}:{rel}')

    def test_gradle_fallback_grey_is_detected(self):
        # mc26_3/resources.gradle sbVanillaDerivedPlaceholders: 0xFF8A8A8A / 0xFF454545, fields of w / 8
        for w, h in ((16, 16), (64, 64), (16, 512), (256, 256)):
            self.assertTrue(pa.is_placeholder(checker(w, h, (0x8A, 0x8A, 0x8A, 255), (0x45, 0x45, 0x45, 255))), (w, h))

    def test_real_art_and_chess_boards_are_not(self):
        self.assertFalse(pa.is_placeholder(checker(16, 16, (240, 240, 240, 255), (30, 30, 30, 255))))  # chess board
        noise = [((x * 37 + y * 11) % 256, (x * 7) % 256, (y * 13) % 256, 255) for y in range(16) for x in range(16)]
        self.assertFalse(pa.is_placeholder(png(noise, 16, 16)))
        self.assertFalse(pa.is_placeholder(checker(16, 16, (138, 138, 138, 128), (69, 69, 69, 128))))  # translucent
        self.assertFalse(pa.is_placeholder(b'not a png'))

    def test_scan_reports_placeholders_and_marker_and_fails_on_request(self):
        with tempfile.TemporaryDirectory() as t:
            bad, good = Path(t) / 'bad.jar', Path(t) / 'good.jar'
            with zipfile.ZipFile(bad, 'w') as z:
                z.writestr('assets/simplebuilding/textures/item/color_brush.png', vd.placeholder('item/color_brush.png'))
                z.writestr('assets/simplebuilding/' + vd.MARKER, 'generateVanillaDerived: test\n')
                z.writestr('assets/simplebuilding/textures/item/ok.png', checker(16, 16, (240, 0, 0, 255), (0, 0, 240, 255)))
            with zipfile.ZipFile(good, 'w') as z:
                z.writestr('assets/simplebuilding/textures/item/ok.png', checker(16, 16, (240, 0, 0, 255), (0, 0, 240, 255)))
            s = pa.scan_jar(bad)
            self.assertEqual(s['placeholders'], ['assets/simplebuilding/textures/item/color_brush.png'])
            self.assertEqual(s['markers'], ['assets/simplebuilding/' + vd.MARKER])
            self.assertEqual(s['textures'], 2)
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(pa.main(['--jars', str(good), '--fail-on-placeholder']), 0)
                self.assertEqual(pa.main(['--jars', str(bad), '--fail-on-placeholder']), 1)
                self.assertEqual(pa.main(['--jars', str(bad)]), 0)


class Marker(unittest.TestCase):
    def test_placeholder_set_carries_the_marker(self):
        with tempfile.TemporaryDirectory() as t:
            self.assertEqual(vd.write(Path(t), None, placeholders=True), 'placeholder')
            self.assertTrue((Path(t) / 'assets/simplebuilding' / vd.MARKER).is_file())
            vd.write(Path(t), None, placeholders=True, module='simpleovens')
            self.assertTrue((Path(t) / 'assets/simpleovens' / vd.MARKER).is_file())

    def test_client_and_gradle_use_the_same_marker_name(self):
        java = (ROOT / 'common/src/shared/java/com/simplebuilding/client/PlaceholderTextureCheck.java').read_text(encoding='utf-8')
        self.assertIn('/assets/simplebuilding/' + vd.MARKER, java)
        for gradle in ('mc26_3/resources.gradle', 'gradle/module-vanilla-derived.gradle'):
            self.assertIn(vd.MARKER, (ROOT / gradle).read_text(encoding='utf-8'), gradle)


# --------------------------------------------------------------------------------------- Windows command line
def java_legacy_command_line(args):
    """java.lang.ProcessImpl (Windows, default legacy mode): an argument with a space or tab is put in quotes, quotes
    inside it are NOT escaped, a trailing backslash is doubled."""
    out = []
    for s in args:
        if (' ' in s or '\t' in s) and not (len(s) > 1 and s.startswith('"') and s.endswith('"')):
            s = '"' + s + ('\\' if s.endswith('\\') else '') + '"'
        out.append(s)
    return ' '.join(out)


def msvcrt_parse(line):
    """How python.exe / py.exe (Microsoft C runtime) split their command line into argv."""
    args, cur, quoted, have, i = [], [], False, False, 0
    while i < len(line):
        c = line[i]
        if c == '\\':
            n = 0
            while i < len(line) and line[i] == '\\':
                n, i = n + 1, i + 1
            if i < len(line) and line[i] == '"':
                cur.append('\\' * (n // 2))
                if n % 2:
                    cur.append('"')
                    i += 1
            else:
                cur.append('\\' * n)
            have = True
            continue
        if c == '"':
            if quoted and i + 1 < len(line) and line[i + 1] == '"':
                cur.append('"')
                i += 2
                continue
            quoted, have, i = not quoted, True, i + 1
            continue
        if c in ' \t' and not quoted:
            if have:
                args.append(''.join(cur))
            cur, have, i = [], False, i + 1
            continue
        cur.append(c)
        have, i = True, i + 1
    if have:
        args.append(''.join(cur))
    return args


class WindowsCommandLine(unittest.TestCase):
    SCRIPT = r'C:\Users\Oussama Ä\code\simplemods-dev\tools\textures\vanilla_derived.py'

    def roundtrip(self, args):
        return msvcrt_parse(java_legacy_command_line(args))

    def test_the_old_inline_probe_lost_its_quotes(self):
        # the cause of the owner's grey checkers: python -c with "..." inside reached Python without the quotes
        old = 'import sys\nfor m in ("PIL", "numpy"):\n    print("SBPY")\n'
        self.assertNotEqual(self.roundtrip(['py', '-3', '-c', old])[3], old)

    def test_the_build_arguments_survive(self):
        for args in (['py', '-3', '-B', self.SCRIPT, '--probe'],
                     [r'C:\Program Files\Python312\python.exe', '-B', self.SCRIPT, '--out',
                      r'C:\Users\Oussama Ä\code\simplemods-dev\mc26_3\fabric\build\generated-resources\vanillaDerived',
                      '--jar', r'C:\Users\Oussama Ä\.gradle\caches\fabric-loom\26.3\minecraft-client.jar'],
                     ['python', '-B', self.SCRIPT, '--out', r'D:\out dir\x', '--module', 'simpleovens']):
            self.assertEqual(self.roundtrip(args), args)

    def test_gradle_probes_without_inline_code(self):
        for gradle in ('mc26_3/resources.gradle', 'gradle/module-vanilla-derived.gradle'):
            text = (ROOT / gradle).read_text(encoding='utf-8')
            self.assertIn("'--probe'", text, gradle)
            self.assertNotRegex(text, r"\['-c',\s*'[^']*\"", gradle)   # -c code with a double quote
            self.assertNotIn('sbVanillaDerivedProbe', text, gradle)


class Probe(unittest.TestCase):
    def test_probe_line(self):
        out = subprocess.run([sys.executable, '-B', str(HERE / 'vanilla_derived.py'), '--probe'], capture_output=True,
                             text=True, check=True).stdout.strip()
        self.assertRegex(out, r'^SBPY \d+\.\d+( (pillow|numpy))*$')
        self.assertEqual(out, vd.probe())

    def test_launcher_jar_is_a_candidate(self):
        import ntpath
        c = vd.jar_candidates(None, {'APPDATA': r'C:\Users\o\AppData\Roaming'}, r'C:\Users\o', join=ntpath.join)
        self.assertIn(r'C:\Users\o\AppData\Roaming\.minecraft\versions\26.3\26.3.jar', c)
        self.assertEqual(c[0], r'C:\Users\o\.gradle\caches\fabric-loom\26.3\minecraft-client.jar')


class RepositoryPaths(unittest.TestCase):
    """17.10.: a Windows default preview path run on Linux was committed as C:/Users/... - Git for Windows cannot check
    such a path out, so the starter's checkout of the working state failed."""
    BAD_CHARS = re.compile(r'[<>:"|?*\\\x00-\x1f]')
    RESERVED = re.compile(r'^(con|prn|aux|nul|com[0-9]|lpt[0-9])(\..*)?$', re.I)

    def test_every_tracked_path_checks_out_on_windows(self):
        try:
            files = subprocess.run(['git', '-C', str(ROOT), 'ls-files', '-z'], capture_output=True, check=True).stdout
        except (OSError, subprocess.CalledProcessError):
            self.skipTest('no git checkout')
        bad = []
        for path in files.decode('utf-8', 'replace').split('\0'):
            for part in filter(None, path.split('/')):
                if self.BAD_CHARS.search(part) or self.RESERVED.match(part) or part[-1] in ' .':
                    bad.append(path)
                    break
        self.assertEqual(bad, [])


if __name__ == '__main__':
    unittest.main()
