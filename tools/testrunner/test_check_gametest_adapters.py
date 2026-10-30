import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import check_gametest_adapters as c  # noqa: E402

CORE = 'GameTestSpec.named("anvil_crushing_game_test_anvils_consume_once", X::a).build(), GameTestSpec\n.named("foo_game_test_two", X::b).build()'
ADAPTER = "public final class AnvilCrushingGameTest { @GameTest(maxTicks = 120)\n public void anvilsConsumeOnce(GameTestHelper h) {} }"
CATALOG = 'public static final Map<String, Consumer<GameTestHelper>> ALL = new X();\nstatic { add("registered", h -> {}); ALL.put("qol_migration", T::q); ALL.put("other", T::o); }'


class AdapterCheck(unittest.TestCase):
    def test_core_missing_adapter_is_reported(self):
        gaps = c.core_gaps(CORE, {"AnvilCrushingGameTest": ADAPTER})
        self.assertEqual(len(gaps), 1)
        self.assertIn("foo_game_test_two", gaps[0])

    def test_core_complete(self):
        both = {"AnvilCrushingGameTest": ADAPTER, "FooGameTest": "@GameTest public void two(GameTestHelper h) {}"}
        self.assertEqual(c.core_gaps(CORE, both), [])

    def test_module_keys_by_method_name_or_get(self):
        adapter = '@GameTest public void registered(GameTestHelper h) {}\n@GameTest public void qolMigration(GameTestHelper h) {}\n' \
                  'void x() { T.ALL.get("other"); }'
        self.assertEqual(c.module_gaps({"T": CATALOG}, adapter), [])

    def test_module_missing_key_is_reported(self):
        gaps = c.module_gaps({"T": CATALOG}, '@GameTest public void registered(GameTestHelper h) {}')
        self.assertEqual(len(gaps), 2)
        self.assertTrue(any("qol_migration" in g for g in gaps))

    def test_repository_is_clean(self):
        self.assertEqual(c.main(), 0)


if __name__ == "__main__":
    unittest.main()
