"""python -m unittest discover -s tools/testrunner/tests  (b-server26: dedicated server solo + vanilla client)"""

import io
import json
import socket
import struct
import sys
import threading
import unittest
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import server_solo as ss  # noqa: E402


class FakeServer(threading.Thread):
    """Plays the 26.3 server side of status + offline login, compression on, one login query, known packs, keep alive."""

    def __init__(self, reject: bool = False):
        super().__init__(daemon=True)
        self.sock = socket.socket()
        self.sock.bind(("127.0.0.1", 0))
        self.sock.listen(2)
        self.port = self.sock.getsockname()[1]
        self.reject = reject
        self.seen: dict = {}

    def run(self):
        for _ in range(2):
            conn, _ = self.sock.accept()
            with conn:
                c = ss.Conn.__new__(ss.Conn)
                c.sock, c.file, c.threshold = conn, conn.makefile("rb"), -1
                pid, s = c.recv()
                protocol, host = ss.read_varint(s), ss.read_string(s)
                s.read(2)
                intent = ss.read_varint(s)
                if intent == 1:
                    c.recv()
                    c.send(0x00, ss.mc_string(json.dumps({"version": {"name": "26.3", "protocol": 777}})))
                    pid, s = c.recv()
                    c.send(0x01, s.read(8))
                    continue
                self.seen["protocol"] = protocol
                pid, s = c.recv()
                self.seen["name"] = ss.read_string(s)
                if self.reject:
                    c.send(ss.LOGIN_CB["disconnect"], ss.mc_string('{"text":"needs Fabric API"}'))
                    continue
                c.send(ss.LOGIN_CB["compression"], ss.varint(16))
                c.threshold = 16
                c.send(ss.LOGIN_CB["custom_query"], ss.varint(7) + ss.mc_string("fabric:probe") + b"\x01\x02")
                pid, s = c.recv()
                self.seen["query_answer"] = (pid, ss.read_varint(s), s.read())
                c.send(ss.LOGIN_CB["finished"], b"\x00" * 16 + ss.mc_string("SoloProbe") + ss.varint(0) + b"\x00")
                self.seen["ack"] = c.recv()[0]
                packs = [("minecraft", "core", "26.3"), ("fabric", "simpletweaks", "1.0.0")]
                c.send(ss.CONFIG_CB["select_known_packs"],
                       ss.varint(len(packs)) + b"".join(ss.mc_string(a) + ss.mc_string(b) + ss.mc_string(v) for a, b, v in packs))
                pid, s = c.recv()
                n = ss.read_varint(s)
                self.seen["known"] = (pid, [(ss.read_string(s), ss.read_string(s), ss.read_string(s)) for _ in range(n)])
                c.send(ss.CONFIG_CB["keep_alive"], struct.pack(">q", 99))
                pid, s = c.recv()
                self.seen["keep_alive"] = (pid, struct.unpack(">q", s.read(8))[0])
                c.send(ss.CONFIG_CB["finish"])
                self.seen["finish"] = c.recv()[0]
                c.send(0x30, b"x" * 40)  # the play Login packet, compressed (over threshold)


class ProtocolTest(unittest.TestCase):
    def test_varint_roundtrip(self):
        for value in (0, 1, 127, 128, 255, 25565, 2097151, 2147483647, -1):
            self.assertEqual(ss.read_varint(io.BytesIO(ss.varint(value))), value)
        self.assertEqual(ss.varint(-1), b"\xff\xff\xff\xff\x0f")

    def test_status_and_login_like_vanilla(self):
        server = FakeServer()
        server.start()
        status = ss.status_ping(server.port)
        self.assertEqual(status["version"]["protocol"], 777)
        self.assertEqual(ss.vanilla_login(server.port, 777, wait_play=5), ("play", ""))
        server.join(5)
        seen = server.seen
        self.assertEqual((seen["protocol"], seen["name"]), (777, "SoloProbe"))
        self.assertEqual(seen["query_answer"], (ss.LOGIN_SB["custom_query_answer"], 7, b"\x00"))
        self.assertEqual(seen["ack"], ss.LOGIN_SB["acknowledged"])
        self.assertEqual(seen["known"], (ss.CONFIG_SB["select_known_packs"], [("minecraft", "core", "26.3")]))
        self.assertEqual(seen["keep_alive"], (ss.CONFIG_SB["keep_alive"], 99))
        self.assertEqual(seen["finish"], ss.CONFIG_SB["finish"])

    def test_login_rejected(self):
        server = FakeServer(reject=True)
        server.start()
        ss.status_ping(server.port)
        outcome, reason = ss.vanilla_login(server.port, 777, wait_play=5)
        self.assertEqual(outcome, "rejected")
        self.assertIn("Fabric API", reason)

    def test_compressed_frame_layout(self):
        a, b = socket.socketpair()
        c = ss.Conn.__new__(ss.Conn)
        c.sock, c.file, c.threshold = a, b.makefile("rb"), 8
        c.send(0x05, b"y" * 20)
        raw = b.makefile("rb")
        length = ss.read_varint(raw)
        body = io.BytesIO(raw.read(length))
        self.assertEqual(ss.read_varint(body), 21)
        self.assertEqual(zlib.decompress(body.read()), b"\x05" + b"y" * 20)
        a.close(), b.close()


class ClosureTest(unittest.TestCase):
    def test_cloth_only_where_declared_hard(self):
        self.assertIn("cloth-config", ss.closure("simplebuilding")["external"])
        for mod in ("simplequalityoflife", "simplefun", "simpledimensions", "simplemoney", "simpleriding", "simpletweaks"):
            self.assertEqual(ss.closure(mod)["external"], {}, mod)

    def test_requirements_recursive_and_known(self):
        qol = ss.closure("simplequalityoflife")
        self.assertEqual(qol["modules"][0], "simplequalityoflife")
        self.assertIn("simplelib", qol["modules"])  # only through a sub-mod
        self.assertTrue(qol["fabricApi"])
        for module in ss.solo_modules():
            self.assertEqual(ss.closure(module["id"])["unknown"], [], module["id"])
        self.assertNotIn("wiringexample", [m["id"] for m in ss.solo_modules()])

    def test_log_faults(self):
        log = "\n".join([
            "[Server thread/INFO] (FabricLoader) Loading 3 mods: fabric-crash-report-info-v1 1.0, simpletweaks",
            "[Server thread/INFO]: Done (3.2s)! For help, type \"help\"",
            "[Server thread/ERROR]: Failed to load something",
            "java.lang.IllegalStateException: boom",
            "---- Minecraft Crash Report ----",
        ])
        faults = ss.log_faults(log)
        self.assertEqual(len(faults), 3)
        self.assertFalse(any("Loading 3 mods" in f for f in faults))


    def test_properties_no_whitelist_and_flat_layers(self):
        props = dict(line.split("=", 1) for line in ss.properties_text(25599).splitlines() if line)
        self.assertEqual(props["server-port"], "25599")
        self.assertEqual(props["online-mode"], "false")
        self.assertEqual(props["white-list"], "false")  # 26.3 default is true -> "not white-listed"
        self.assertEqual(props["enforce-whitelist"], "false")
        self.assertEqual(props["level-type"], "minecraft\\:flat")
        flat = json.loads(props["generator-settings"])  # empty {} -> "No key layers in MapLike[{}]"
        self.assertEqual([l["block"] for l in flat["layers"]],
                         ["minecraft:bedrock", "minecraft:dirt", "minecraft:grass_block"])
        self.assertFalse(ss.log_faults(ss.properties_text(1)))

if __name__ == "__main__":
    unittest.main()
