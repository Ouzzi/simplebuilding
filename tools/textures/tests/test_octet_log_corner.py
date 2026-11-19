"""N100B (owner 19.11.): the inner faces of a log octet show straight lines along the grain, not the rings.

Run: python3 -m unittest discover -s tools/textures/tests
"""
import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import octets_2026_10_09 as oc  # noqa: E402

MODELS = Path(oc.ASSETS) / 'models' / 'block' / 'octet'


class LogCornerLines(unittest.TestCase):
    def test_inner_faces_are_one_end_grain_row(self):
        faces = oc.log_corner()['elements'][0]['faces']
        for side, u0 in (('south', 0), ('east', 8)):
            self.assertEqual(faces[side]['texture'], '#up', side)
            u_from, v_from, u_to, v_to = faces[side]['uv']
            # one texel row stretched over the octet height = straight lines, no ring arcs
            self.assertEqual(v_to - v_from, 1, side)
            self.assertIn(v_from, (7, 8), side)  # a middle row: it crosses every ring and has bark only at both ends
            # u at the face's place in the block: the bark texel (u 0 / 15) lies on the block's outer edge
            self.assertEqual((u_from, u_to), (u0, u0 + 8), side)

    def test_outer_faces_stay_bark(self):
        faces = oc.log_corner()['elements'][0]['faces']
        self.assertEqual(faces['north'], {'texture': '#side', 'cullface': 'north'})
        self.assertEqual(faces['west'], {'texture': '#side', 'cullface': 'west'})

    def test_written_template_and_all_log_models_use_it(self):
        self.assertEqual(json.loads((MODELS / '_corner_log.json').read_text(encoding='utf-8')), oc.log_corner())
        logs = [m for m in oc.materials() if m['group'] == 'log']
        self.assertGreaterEqual(len(logs), 28)
        for m in logs:
            model = json.loads((MODELS / f"{m['key']}.json").read_text(encoding='utf-8'))
            self.assertEqual(model['parent'], 'simpleoctets:block/octet/_corner_log', m['key'])


if __name__ == '__main__':
    unittest.main()
