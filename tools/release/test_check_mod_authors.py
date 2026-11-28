import io
import json
import sys
import unittest
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import check_mod_authors as cma


def jar(files: dict) -> zipfile.ZipFile:
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, 'w') as z:
        for name, data in files.items():
            z.writestr(name, data)
    return zipfile.ZipFile(io.BytesIO(buf.getvalue()))


class Authors(unittest.TestCase):
    def test_repo_metadata_agrees(self):
        ref, bad = cma.findings(list(cma.sources()))
        self.assertEqual(ref, ('Ouzzi',))
        self.assertEqual(bad, [])

    def test_odd_author_is_found_in_nested_jar(self):
        inner = io.BytesIO()
        with zipfile.ZipFile(inner, 'w') as z:
            z.writestr('fabric.mod.json', json.dumps({'id': 'b', 'authors': ['Someone Else']}))
        outer = jar({'fabric.mod.json': json.dumps({'id': 'a', 'authors': ['Ouzzi']}), 'META-INF/jars/b.jar': inner.getvalue(),
                     'META-INF/neoforge.mods.toml': 'authors="Ouzzi"\n'})
        ref, bad = cma.findings(list(cma.jar_entries(outer, 'x.jar!')))
        self.assertEqual(ref, ('Ouzzi',))
        self.assertEqual(bad, ['x.jar!b.jar!fabric.mod.json: authors differ from the other mods'])

    def test_library_without_authors_is_skipped(self):
        ref, bad = cma.findings([('a', ('Ouzzi',)), ('framework', ())])
        self.assertEqual(bad, [])


if __name__ == '__main__':
    unittest.main()
