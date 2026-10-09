import unittest


class DevHooksTests(unittest.TestCase):

    def test_hooks_present(self):
        path = __import__('pathlib').Path(__file__).resolve().parents[1] / 'index.html'
        with path.open('r', encoding='utf-8') as f:
            content = f.read()
        self.assertIn('function devHook', content)
        self.assertIn('devHook(\'textures\'', content)
        self.assertIn('devHook(\'texture-zoom\'', content)
        self.assertIn('devHook(\'item-page\'', content)
        self.assertIn('devHook(\'items\'', content)
        self.assertIn('window.sbDevHook', content)


if __name__ == '__main__':
    unittest.main()
