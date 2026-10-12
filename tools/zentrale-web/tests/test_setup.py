"""PC setup for the Laptop-Start buttons: wave bundle (make_wave_bundle.sh) and the Windows scripts in starter/.

The PowerShell parts run when pwsh is on PATH (or $PWSH): syntax of both scripts, PSScriptAnalyzer errors when the
module is installed, and the starter's repo sync (master from the "GitHub" repo, working state from the bundle)
against local repos.
"""
import json
import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parents[1]
SCRIPT = HERE / 'make_wave_bundle.sh'
STARTER = HERE / 'starter' / 'starter.ps1'
INSTALL = HERE / 'starter' / 'install.ps1'
PWSH = os.environ.get('PWSH') or shutil.which('pwsh')


def git(cwd, *args):
    return subprocess.run(['git', '-C', str(cwd), *args], check=True, capture_output=True, text=True).stdout.strip()


def commit(repo, name):
    (Path(repo) / name).write_text(name)
    git(repo, 'add', name)
    git(repo, '-c', 'user.name=t', '-c', 'user.email=t@t', 'commit', '-qm', name)
    return git(repo, 'rev-parse', 'HEAD')


class WaveBundleTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.gh = self.tmp / 'github'
        git(self.tmp, 'init', '-q', '-b', 'master', str(self.gh))
        commit(self.gh, 'a')
        self.master = commit(self.gh, 'b')
        self.hub = self.tmp / 'hub'
        git(self.tmp, 'clone', '-q', str(self.gh), str(self.hub))
        git(self.hub, 'checkout', '-qb', 'claude-wave1')
        commit(self.hub, 'c')
        self.wave = commit(self.hub, 'd')
        self.out = self.tmp / 'setup'

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def make(self):
        subprocess.run(['bash', str(SCRIPT), str(self.hub), 'claude-wave1', str(self.gh), str(self.out)], check=True)
        return json.loads((self.out / 'wave.json').read_text())

    def test_bundle_holds_only_commits_on_top_of_master(self):
        info = self.make()
        self.assertEqual(info['commit'], self.wave)
        self.assertEqual(info['base'], self.master)
        self.assertEqual(info['branch'], 'claude-wave1')
        self.assertEqual(info['bytes'], (self.out / 'wave.bundle').stat().st_size)
        client = self.tmp / 'client'
        git(self.tmp, 'clone', '-q', str(self.gh), str(client))
        git(client, 'fetch', '-q', str(self.out / 'wave.bundle'), '+refs/heads/claude-wave1:refs/remotes/zentrale/claude-wave1')
        self.assertEqual(git(client, 'rev-parse', 'refs/remotes/zentrale/claude-wave1'), self.wave)
        # a client without master's history cannot use it: the bundle really is thin
        empty = self.tmp / 'empty'
        git(self.tmp, 'init', '-q', str(empty))
        r = subprocess.run(['git', '-C', str(empty), 'bundle', 'verify', str(self.out / 'wave.bundle')], capture_output=True)
        self.assertNotEqual(r.returncode, 0)
        self.assertEqual([p.name for p in self.out.iterdir() if p.name.startswith('.')], [])

    def test_unchanged_head_keeps_files_and_new_commit_rebuilds(self):
        self.make()
        before = (self.out / 'wave.bundle').stat().st_mtime_ns
        os.utime(self.out / 'wave.bundle', ns=(before - 10**9, before - 10**9))
        self.make()
        self.assertEqual((self.out / 'wave.bundle').stat().st_mtime_ns, before - 10**9)
        newer = commit(self.hub, 'e')
        self.assertEqual(self.make()['commit'], newer)

    def test_branch_equal_to_master_still_gives_a_bundle(self):
        git(self.hub, 'reset', '-q', '--hard', self.master)
        info = self.make()
        self.assertEqual(info['commit'], self.master)
        self.assertTrue((self.out / 'wave.bundle').stat().st_size > 0)


@unittest.skipUnless(PWSH, 'pwsh not installed')
class PowerShellTest(unittest.TestCase):
    def pwsh(self, code, env=None):
        return subprocess.run([PWSH, '-NoProfile', '-NonInteractive', '-Command', code], capture_output=True, text=True,
                              env={**os.environ, **(env or {})}, timeout=300)

    def test_scripts_parse_and_are_ascii(self):
        for f in (STARTER, INSTALL):
            f.read_bytes().decode('ascii')  # PowerShell 5.1 reads BOM-less files as ANSI
            r = self.pwsh(f"$e=$null; [void][System.Management.Automation.Language.Parser]::ParseFile('{f}', [ref]$null, [ref]$e); "
                          "$e | ForEach-Object { $_.ToString() }; exit $e.Count")
            self.assertEqual(r.returncode, 0, f'{f.name}: {r.stdout}{r.stderr}')

    def test_script_analyzer_has_no_errors(self):
        has = self.pwsh('if (Get-Module -ListAvailable PSScriptAnalyzer) { exit 0 } else { exit 3 }')
        if has.returncode == 3:
            self.skipTest('PSScriptAnalyzer not installed')
        for f in (STARTER, INSTALL):
            r = self.pwsh(f"$r = Invoke-ScriptAnalyzer -Path '{f}' -Severity Error,Warning "
                          "-ExcludeRule PSAvoidUsingWriteHost,PSUseShouldProcessForStateChangingFunctions,PSUseSingularNouns,"
                          "PSUseApprovedVerbs,PSAvoidUsingEmptyCatchBlock,PSUseBOMForUnicodeEncodedFile; "
                          "$r | ForEach-Object { '{0}:{1} {2} {3}' -f $_.ScriptName,$_.Line,$_.RuleName,$_.Message }; exit @($r).Count")
            self.assertEqual(r.returncode, 0, f'{f.name}:\n{r.stdout}{r.stderr}')

    def test_starter_syncs_master_and_working_state(self):
        base = WaveBundleTest('test_bundle_holds_only_commits_on_top_of_master')
        base.setUp()
        try:
            info = base.make()
            home = base.tmp / 'home'
            home.mkdir()
            dev = home / 'dev'
            code = f"""
$ErrorActionPreference = 'Stop'
. '{STARTER}'
$GitHubUrl = '{base.gh}'
function Get-FromZentrale([hashtable]$Cfg, [string]$Path, [string]$OutFile) {{
    Copy-Item -LiteralPath (Join-Path '{base.out}' ($Path -replace '^setup/', '')) -Destination $OutFile -Force }}
New-Item -ItemType Directory -Force -Path $Home2 | Out-Null
$cfg = @{{ zentrale = 'https://zentrale.example'; devDir = '{dev}' }}
Sync-Repo $cfg 'master' 'git'
'MASTER=' + (git -C '{dev}' rev-parse HEAD)
Sync-Repo $cfg 'wave' 'git'
'WAVE=' + (git -C '{dev}' rev-parse HEAD)
Sync-Repo $cfg 'wave' 'git'
Set-Content -LiteralPath '{dev}/a' -Value changed
try {{ Sync-Repo $cfg 'master' 'git'; 'DIRTY=accepted' }} catch {{ 'DIRTY=' + $_.Exception.Message }}
"""
            r = self.pwsh(code, env={'USERPROFILE': str(home)})
            out = r.stdout
            self.assertEqual(r.returncode, 0, out + r.stderr)
            self.assertIn(f'MASTER={base.master}', out)
            self.assertIn(f'WAVE={info["commit"]}', out)
            self.assertIn('DIRTY=Der Dev-Ordner', out)
            log = (home / '.simplemods' / 'starter.log').read_text(encoding='utf-8-sig')
            self.assertIn('Arbeitsstand ist schon aktuell.', log)
        finally:
            base.tearDown()


if __name__ == '__main__':
    unittest.main()
