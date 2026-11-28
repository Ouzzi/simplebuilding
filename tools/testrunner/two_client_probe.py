#!/usr/bin/env python3
"""O8 audit 1: two real clients on one dedicated server (sync of blocks, block entities/menus, entities; permissions).

1. Dedicated server of the integration project (`:integration:runIntegrationServer`, every module, run dir integration/run-fabric-263)
   with a flat world, offline mode, port PORT, spawn protection 16, ProbeA as operator (ops.json with the offline UUID).
2. Client A, then client B: `ui_probe.py --server 127.0.0.1:PORT --username ProbeX --bare --only twoclient-x`, each in its own game
   directory (SB_PROBE_RUN = integration/run-o8-a|b, mods/ linked to run-fabric-263/mods) and its own Xvfb (ui_probe starts one when
   DISPLAY is unset). Scenarios: modules/simpleinterfaces/tools/uiprobe_twoclient.py; they meet at files in SB_TWOCLIENT_DIR.
3. After both clients ended: `stop` on the server's stdin (kill after 120 s), server log scanned for exceptions.

Run (sb-test, inside the heavy lock - client runs are for the main session only):
    /root/sbt <branch> "python3.12 tools/testrunner/two_client_probe.py --out /var/tmp/sb/twoclient"
Exit 0 when both clients passed every check and the server log is clean.
"""
import argparse
import hashlib
import json
import os
import re
import shutil
import signal
import subprocess
import sys
import time
import uuid
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
SERVER_RUN = REPO / 'integration/run-fabric-263'
PORT = 25599
FAULT = re.compile(r'/ERROR\]|Exception\b|\bcrash(ed|ing)?\b(?!-)', re.IGNORECASE)


def offline_uuid(name):
    h = bytearray(hashlib.md5(f'OfflinePlayer:{name}'.encode()).digest())
    h[6] = (h[6] & 0x0F) | 0x30
    h[8] = (h[8] & 0x3F) | 0x80
    return str(uuid.UUID(bytes=bytes(h)))


def prepare_server():
    SERVER_RUN.mkdir(parents=True, exist_ok=True)
    shutil.rmtree(SERVER_RUN / 'world', ignore_errors=True)
    (SERVER_RUN / 'eula.txt').write_text('eula=true\n')
    flat = ('{"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},'
            '{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains","lakes":false,"features":false}')
    (SERVER_RUN / 'server.properties').write_text('\n'.join([
        f'server-port={PORT}', 'server-ip=127.0.0.1', 'online-mode=false', 'enforce-secure-profile=false', 'white-list=false',
        'level-type=minecraft\\:flat', f'generator-settings={flat}', 'generate-structures=false', 'spawn-monsters=false',
        'difficulty=peaceful', 'spawn-protection=16', 'view-distance=4', 'simulation-distance=4', 'max-players=4', 'motd=o8 two clients',
        '']))
    (SERVER_RUN / 'ops.json').write_text(json.dumps([{'uuid': offline_uuid('ProbeA'), 'name': 'ProbeA', 'level': 4,
                                                      'bypassesPlayerLimit': False}]))


def prepare_client(name):
    run = REPO / f'integration/run-o8-{name}'
    run.mkdir(parents=True, exist_ok=True)
    mods = run / 'mods'
    if mods.is_symlink() or mods.is_file():
        mods.unlink()
    elif mods.is_dir():
        shutil.rmtree(mods)
    mods.symlink_to(SERVER_RUN / 'mods', target_is_directory=True)
    src = SERVER_RUN / 'options.txt'
    opts = src.read_text().splitlines() if src.is_file() else []
    opts = [o for o in opts if not o.startswith(('pauseOnLostFocus:', 'onboardAccessibility:', 'joinedFirstServer:'))]
    (run / 'options.txt').write_text('\n'.join(opts + ['pauseOnLostFocus:false', 'onboardAccessibility:false', 'joinedFirstServer:true']) + '\n')
    return run


def start_client(name, out, meet):
    env = dict(os.environ, SB_PROBE_RUN=str(prepare_client(name)), SB_TWOCLIENT_DIR=str(meet))
    env.pop('DISPLAY', None)  # ui_probe starts its own Xvfb per client: the two windows never share a screen or the pointer
    (out / name).mkdir(parents=True, exist_ok=True)
    log = open(out / name / 'driver.log', 'w')
    player = 'ProbeA' if name == 'a' else 'ProbeB'
    return subprocess.Popen([sys.executable, 'modules/simpleinterfaces/tools/ui_probe.py', '--out', str(out / name),
                             '--server', f'127.0.0.1:{PORT}', '--username', player, '--bare', '--only', f'twoclient-{name}'],
                            cwd=REPO, env=env, stdout=log, stderr=subprocess.STDOUT, start_new_session=True), log


def wait_text(path, needle, timeout, proc):
    end = time.time() + timeout
    while time.time() < end:
        if path.is_file() and needle in path.read_text(errors='replace'):
            return True
        if proc.poll() is not None:
            return False
        time.sleep(1)
    return False


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', default='/var/tmp/sb/twoclient')
    args = ap.parse_args()
    out = Path(args.out)
    shutil.rmtree(out, ignore_errors=True)
    out.mkdir(parents=True)
    meet = out / 'meet'
    meet.mkdir()
    prepare_server()
    slog = out / 'server.log'
    server = subprocess.Popen(['./gradlew', '--no-daemon', ':integration:runIntegrationServer'], cwd=REPO, stdin=subprocess.PIPE,
                              stdout=open(slog, 'w'), stderr=subprocess.STDOUT, text=True, start_new_session=True)
    clients = []
    verdict = {'server_boot': False}
    try:
        if not wait_text(slog, 'Done (', 1800, server):
            print('server did not reach Done (see server.log)', flush=True)
            return 1
        verdict['server_boot'] = True
        print('server up', flush=True)
        for name in ('a', 'b'):
            clients.append((name,) + start_client(name, out, meet))
            # one client at a time through Gradle configuration and the title screen
            wait_text(out / name / 'driver.log', 'world open after', 1800, clients[-1][1])
            print(f'client {name} in the world', flush=True)
        for name, proc, log in clients:
            try:
                proc.wait(timeout=2400)
            except subprocess.TimeoutExpired:
                os.killpg(proc.pid, signal.SIGTERM)
                proc.wait(timeout=120)
            log.close()
            verdict[f'client_{name}_exit'] = proc.returncode
    finally:
        for name, proc, _ in clients:
            if proc.poll() is None:
                os.killpg(proc.pid, signal.SIGKILL)
        try:
            server.stdin.write('stop\n')
            server.stdin.flush()
            server.wait(timeout=120)
        except (OSError, subprocess.TimeoutExpired):
            os.killpg(server.pid, signal.SIGTERM)
            try:
                server.wait(timeout=60)
            except subprocess.TimeoutExpired:
                os.killpg(server.pid, signal.SIGKILL)
    text = slog.read_text(errors='replace')
    faults = [l.strip()[:300] for l in text.splitlines() if FAULT.search(l)]
    verdict['server_fault_lines'] = len(faults)
    verdict['server_class_errors'] = sum(1 for l in text.splitlines() if 'ClassNotFound' in l or 'NoClassDefFound' in l)
    checks = []
    for name in ('a', 'b'):
        res = out / name / 'results.json'
        if res.is_file():
            checks += json.loads(res.read_text())['results']
    for c in checks:
        print(('OK   ' if c['ok'] else 'FAIL ') + f"[{c['scenario']}] {c['check']}: {c['detail'][:200]}")
    for f in faults[:20]:
        print('SERVER', f)
    ok = (verdict['server_boot'] and checks and all(c['ok'] for c in checks) and not faults
          and all(verdict.get(f'client_{n}_exit') == 0 for n in ('a', 'b')))
    print('TWOCLIENT', 'OK' if ok else 'FAIL', json.dumps(verdict), f"{sum(c['ok'] for c in checks)}/{len(checks)} checks ok")
    (out / 'verdict.json').write_text(json.dumps({'ok': bool(ok), 'verdict': verdict, 'checks': checks}, indent=1))
    return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main())
