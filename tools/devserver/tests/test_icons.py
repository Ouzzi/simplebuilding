"""
Bilder: jedes Item, das eine Seite der Zentrale zeigt (Gegenstände, Beute, Handel, Drops, Rezepte, Rechner),
hat ein Bild, und der Server liefert es aus - keine 404, keine Textkacheln.
"""

import json
import tempfile
import threading
import unittest
import urllib.error
import urllib.request
from pathlib import Path

import helpers
import serve
from sbdev import extract, icons


def shown_keys(snap: dict) -> set:
    """Alle Ids, die die Oberfläche mit slot()/itemRef() zeigt."""
    keys = set(snap["display"]) | {i["id"] for i in snap["items"]} | {b["key"] for b in snap["books"]} | set(snap["sources"])
    for table in snap["loot"]["tables"]:
        for pool in table["pools"]:
            for e in pool["entries"]:
                if e.get("enchantment"):
                    keys.add(f"book:{e['enchantment']}:{e['level']}")
                elif e.get("item"):
                    keys.add(e["item"])
    for t in snap["trades"]:
        for field in ("gives", "wants", "alsoWants"):
            if (t.get(field) or {}).get("id"):
                keys.add(t[field]["id"])
    for r in snap["recipes"]:
        keys.add(r["result"]["id"])
        for ing in r["ingredients"]:
            keys.update(x for x in ing["id"].split(" / ") if x and not x.startswith("#"))
    for d in snap["blockDrops"]:
        keys.update((d["block"], d["item"]))
    for d in snap["mobDrops"]:
        keys.add(d["item"])
    return {k for k in keys if k}


class IconTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.service = helpers.shared_service()
        cls.snap = cls.service.snapshot
        cls.resolver = extract.icon_resolver(helpers.REPO)
        cls.vanilla_available = bool(cls.resolver.jar_names()) or (helpers.REPO / icons.WIKI_VANILLA).is_dir()

    def test_every_listed_item_resolves_to_an_existing_icon(self):
        missing, unreadable = [], []
        for key in sorted(shown_keys(self.snap)):
            if key.startswith("minecraft:") and not self.vanilla_available:
                continue
            entry = self.snap["display"].get(key)
            if not entry or not entry.get("icon"):
                missing.append(key)
                continue
            data = self.resolver.read(entry["icon"])
            if not data or icons.png_size(data[:32]) is None:
                unreadable.append(f"{key} -> {entry['icon']}")
        self.assertEqual(missing, [], "Items ohne Bild (Textkachel in der Zentrale)")
        self.assertEqual(unreadable, [], "Bild-Pfade, die der Server nicht ausliefern kann (404)")
        if not self.vanilla_available:
            self.skipTest("Client-Jar und wiki/assets/textures/minecraft fehlen - Vanilla-Bilder nicht geprüft")

    def test_the_owner_reported_items(self):
        for key in ("simplebuilding:velocity_gauge", "simplebuilding:echo_sounder", "simplebuilding:amethyst_lens",
                    "simplebuilding:layered_raw_enderite", "simplebuilding:enderite_horse_armor",
                    "simplebuilding:enderite_nautilus_armor", "simplebuilding:blaze_head", "simplebuilding:enderman_head",
                    "simplebuilding:netherite_chest", "simplebuilding:enderite_chest", "simplebuilding:copper_spatula",
                    "simplebuilding:funny_stick"):
            icon = (self.snap["display"].get(key) or {}).get("icon")
            self.assertTrue(icon, key)
            self.assertIsNotNone(self.resolver.read(icon), f"{key}: {icon}")
        # Köpfe und Truhen: das 3D-Bild des Wikis, nicht eine flache Textur
        for key in ("simplebuilding:blaze_head", "simplebuilding:netherite_chest"):
            self.assertIn("/render/", self.snap["display"][key]["icon"], key)

    def test_books_use_the_enchanted_book_texture(self):
        book = self.snap["books"][0]["key"]
        icon = self.snap["display"][book]["icon"]
        self.assertTrue(icon.endswith("enchanted_book.png"), icon)
        if self.vanilla_available:
            self.assertIsNotNone(self.resolver.read(icon))

    def test_model_fallback_without_wiki_files(self):
        """Frischer Checkout ohne generierte Wiki-Bilder: Mod-Items aus den Modellen, Vanilla aus dem Jar."""
        repo = Path(tempfile.mkdtemp(prefix="bz-icons-"))
        for rel in ("src/main/resources/assets/simplebuilding/textures/item", "src/main/resources/assets/simplebuilding/models",
                    "src/main/resources/assets/simplebuilding/items", "src/main/generated/assets/simplebuilding/models/item",
                    "src/main/generated/assets/simplebuilding/items"):
            src = helpers.REPO / rel
            if src.is_dir():
                import shutil
                shutil.copytree(src, repo / rel)
        res = icons.Resolver(repo, self.resolver.jar)
        got = res.resolve("simplebuilding:copper_spatula")
        self.assertEqual(got["icon"], "modtex/simplebuilding/item/copper_spatula.png")
        self.assertIsNotNone(res.read(got["icon"]))
        echo = res.resolve("simplebuilding:echo_sounder")
        self.assertIsNotNone(echo)
        self.assertIsNotNone(res.read(echo["icon"]))
        if not self.resolver.jar_names():
            self.skipTest("Client-Jar fehlt")
        for key, want in (("minecraft:apple", "vanilla/item/apple.png"), ("minecraft:diamond_pickaxe", "vanilla/item/diamond_pickaxe.png"),
                          ("minecraft:furnace", "vanilla/block/furnace_front.png"), ("simplebuilding:funny_stick", "vanilla/item/stick.png"),
                          ("book:simplebuilding:range:1", "vanilla/item/enchanted_book.png")):
            self.assertEqual((res.resolve(key) or {}).get("icon"), want, key)
            self.assertIsNotNone(res.read(want), want)

    def test_animated_texture_is_flagged(self):
        repo = Path(tempfile.mkdtemp(prefix="bz-icons-"))
        tex = repo / "src/main/resources/assets/demo/textures/item"
        tex.mkdir(parents=True)
        import struct
        import zlib

        def png(w, h):
            raw = b"".join(b"\x00" + b"\x00\x00\x00\xff" * w for _ in range(h))
            def chunk(kind, data):
                return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)
            return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")
        (tex / "strip.png").write_bytes(png(16, 64))
        (tex / "flat.png").write_bytes(png(16, 16))
        res = icons.Resolver(repo, repo / "no.jar")
        self.assertTrue(res.resolve("demo:strip")["anim"])
        self.assertFalse(res.resolve("demo:flat")["anim"])

    def test_read_refuses_paths_outside_the_texture_folders(self):
        for bad in ("modtex/simplebuilding/../../../../build.gradle", "vanilla/../../lang/en_us.json", "wiki/assets/../index.html",
                    "modtex/simplebuilding/item/x.txt", "vanilla/item/..%2f..%2fx.png", "etc/passwd.png"):
            self.assertIsNone(self.resolver.read(bad), bad)


class IconRouteTests(unittest.TestCase):
    """Der Server liefert jedes Bild der Zentrale aus (was das Netzwerk-Tab des Browsers sähe)."""

    @classmethod
    def setUpClass(cls):
        cls.service = helpers.fresh_store(helpers.shared_service())
        cls.server = serve.start(cls.service, helpers.REPO, "127.0.0.1", 0, tries=1)
        cls.port = cls.server.server_address[1]
        threading.Thread(target=cls.server.serve_forever, daemon=True).start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()

    def get(self, path):
        try:
            with urllib.request.urlopen(f"http://127.0.0.1:{self.port}{path}", timeout=30) as res:
                return res.status, res.headers.get("Content-Type"), res.read()
        except urllib.error.HTTPError as err:
            return err.code, err.headers.get("Content-Type"), err.read()

    def test_every_icon_url_answers_200(self):
        resolver = extract.icon_resolver(helpers.REPO)
        vanilla_ok = bool(resolver.jar_names()) or (helpers.REPO / icons.WIKI_VANILLA).is_dir()
        failed = []
        for key, entry in sorted(self.service.snapshot["display"].items()):
            icon = entry.get("icon")
            if not icon or (key.startswith("minecraft:") and not vanilla_ok):
                continue
            status, ctype, body = self.get("/" + icon)
            if status != 200 or "image/png" not in (ctype or "") or not body.startswith(b"\x89PNG"):
                failed.append(f"{key}: /{icon} -> {status}")
        self.assertEqual(failed, [])

    def test_unknown_icons_are_404(self):
        self.assertEqual(self.get("/vanilla/item/does_not_exist_xyz.png")[0], 404)
        self.assertEqual(self.get("/modtex/simplebuilding/item/nope.png")[0], 404)
        self.assertIn(self.get("/modtex/simplebuilding/../../../build.gradle")[0], (403, 404))


if __name__ == "__main__":
    unittest.main()
