"""Offline launch policy: no real DNS, Gradle or Minecraft process."""
import os
import socket
import sys
import threading
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from hub import targets
from hub.api import Hub


class OfflineLaunchTests(unittest.TestCase):
    def setUp(self):
        for p in (patch.dict(os.environ, {'SIMPLEBUILDING_GRADLE_OFFLINE': '0'}),
                  patch.object(targets, '_dns_result', None),
                  patch.object(targets, '_dns_expires', 0)):
            p.start()
            self.addCleanup(p.stop)

    def test_online_launches_skip_only_unneeded_forge(self):
        with patch.object(targets.socket, 'getaddrinfo', return_value=[]):
            for target in targets.loaders().values():
                for kind in ('client', 'server'):
                    with self.subTest(target=target['id'], kind=kind):
                        argv = targets.launch_command(target, kind, Path('/ws'))
                        self.assertEqual('-PskipForge262=true' in argv, target['id'] != 'forge-262')
                        self.assertNotIn('--offline', argv)

    def test_dns_failure_and_cache(self):
        with patch.object(targets.socket, 'getaddrinfo', side_effect=socket.gaierror) as dns:
            for tid in ('fabric-263', 'neoforge-263', 'forge-263'):
                argv = targets.launch_command(targets.find_loader(tid), 'client', Path('/ws'))
                self.assertIn('--offline', argv)
                self.assertIn('-PskipForge262=true', argv)
            dns.assert_called_once_with('piston-meta.mojang.com', 443)
            with patch.object(targets.time, 'monotonic', return_value=targets._dns_expires + 1):
                self.assertTrue(targets.gradle_offline())
            self.assertEqual(dns.call_count, 2)

    def test_forced_offline_never_resolves_dns(self):
        with patch.dict(os.environ, {'SIMPLEBUILDING_GRADLE_OFFLINE': '1'}), \
                patch.object(targets.socket, 'getaddrinfo') as dns:
            self.assertIn('--offline', targets.gradle_args())
            dns.assert_not_called()
            for action in (lambda: targets.launch_command(targets.find_loader('forge-262'), 'server', Path('/ws')),
                           lambda: targets.test_env(['fabric-263', 'forge-262']),
                           lambda: targets.check_argv(Path('/ws'))):
                with self.assertRaisesRegex(targets.TargetError, r'Forge 26.2 braucht Netz \(Mavenizer\)'):
                    action()

    def test_hanging_dns_is_bounded_and_reuses_worker(self):
        release = threading.Event()
        with patch.object(targets.socket, 'getaddrinfo', side_effect=lambda *a: release.wait()) as dns:
            try:
                self.assertTrue(targets.gradle_offline())
                self.assertFalse(targets._dns_result.done())
                self.assertTrue(targets.gradle_offline())
                dns.assert_called_once()
            finally:
                release.set()
                targets._dns_result.result(timeout=2)

    def test_runner_receives_offline_and_skip_flags(self):
        with patch.object(targets.socket, 'getaddrinfo', side_effect=socket.gaierror):
            steps = Hub._run_steps(None, Path('/ws'), [(['fabric-263', 'neoforge-263'], None)],
                                   {'SIMPLEBUILDING_CLIENT_ONLY': 'example'})
            self.assertEqual(steps[0]['env']['SIMPLEBUILDING_CLIENT_ONLY'], 'example')
            with patch.dict(os.environ, steps[0]['env']):
                argv = targets.runner().gradlew()
            self.assertIn('--offline', argv)
            self.assertEqual(argv.count('-PskipForge262=true'), 1)

    def test_online_runner_and_full_gate(self):
        with patch.object(targets.socket, 'getaddrinfo', return_value=[]):
            with patch.dict(os.environ, targets.test_env(['fabric-263'])):
                self.assertIn('-PskipForge262=true', targets.runner().gradlew())
            with patch.dict(os.environ, targets.test_env(['forge-262'])):
                self.assertNotIn('-PskipForge262=true', targets.runner().gradlew())
            self.assertNotIn('-PskipForge262=true', targets.check_argv(Path('/ws')))


if __name__ == '__main__':
    unittest.main()
