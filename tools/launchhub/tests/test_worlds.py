"""Fresh/rebuilt world preparation and launch wiring; never starts Minecraft."""
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock, patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from hub.api import Hub
from hub import settings, targets


class WorldTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.world = self.root / 'run/saves/SB-Testzentrale'
        self.world.mkdir(parents=True)
        (self.world / 'level.dat').write_bytes(b'old world and seed')
        (self.world / 'simplebuilding_testcentre.txt').write_text('0 80 0 old-hash\n')

    def test_recreate_archives_world_and_player_data_even_with_identical_timestamps(self):
        (self.world / 'playerdata').mkdir()
        (self.world / 'playerdata/player.dat').write_bytes(b'old inventory')
        with patch('hub.api.datetime') as clock:
            clock.now.return_value.strftime.return_value = 'same-time'
            for _ in range(2):
                Hub._prepare_world(self.world, 'recreate', lambda _: None, self.world.name)
                self.assertFalse(self.world.exists())
                # Simulate QuickPlay creating the next world; no reuse of level.dat or playerdata.
                self.world.mkdir()
                (self.world / 'level.dat').write_bytes(b'new world')
        archives = sorted((self.root / 'run/hub-old-worlds').iterdir())
        self.assertEqual(len(archives), 2)
        self.assertEqual((archives[0] / 'level.dat').read_bytes(), b'old world and seed')
        self.assertEqual((archives[0] / 'playerdata/player.dat').read_bytes(), b'old inventory')

    def test_rebuild_keeps_origin_world_and_only_invalidates_fingerprint(self):
        for _ in range(2):
            Hub._prepare_world(self.world, 'rebuild', lambda _: None, self.world.name)
            self.assertEqual((self.world / 'simplebuilding_testcentre.txt').read_text(), '0 80 0\n')
            self.assertEqual((self.world / 'level.dat').read_bytes(), b'old world and seed')

    def test_missing_world_is_left_for_quickplay(self):
        world = self.world.parent / 'missing'
        for mode in ('recreate', 'rebuild'):
            self.assertTrue(Hub._prepare_world(world, mode, lambda _: None, world.name))
            self.assertFalse(world.exists())

    def test_default_fresh_launch_prepares_exact_quickplay_run_directory(self):
        hub = Hub(self.root, self.root / 'logs', self.root / 'data')
        hub.manager = Mock()
        hub.manager.active_where.return_value = []
        with patch.object(hub, 'require_disk'), patch.object(hub, 'selection_step', return_value={'label': 'apply mod selection'}), \
                patch.object(settings, 'load', return_value=settings.DEFAULTS), \
                patch.object(targets, 'gradle_offline', return_value=False), patch.object(hub, '_prepare_world') as prepare:
            for loader in ('fabric-263', 'neoforge-263', 'forge-263'):
                hub.launch({'action': 'client_fresh', 'target': loader, 'workspace': 'repo'})
                steps = hub.manager.start.call_args.args[2]
                self.assertIn('(recreate)', steps[0]['label'])
                log = Mock()
                steps[0]['call'](log)
                entry = targets.find_loader(loader)
                prepare.assert_called_with(targets.world_path(entry, self.root), 'recreate', log, 'SB-Testzentrale')
                self.assertIn('--args=--quickPlaySingleplayer SB-Testzentrale', steps[-1]['argv'])
                self.assertIn('-Phub_client=true', steps[-1]['argv'])
                self.assertEqual(steps[-1]['cwd'], str(self.root))

    def test_hub_opt_in_is_client_only_and_main_line_only(self):
        with patch.object(targets, 'gradle_offline', return_value=False):
            for loader in ('fabric-263', 'neoforge-263', 'forge-263', 'fabric-262', 'fabric-264'):
                for kind in ('client', 'server'):
                    command = targets.launch_command(targets.find_loader(loader), kind, self.root)
                    self.assertEqual('-Phub_client=true' in command, kind == 'client' and loader.endswith('-263'))
