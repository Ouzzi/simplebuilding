"""
Bilder der Items und Blöcke in der Zentrale (nur Darstellung).

Reihenfolge je Id:

1. Mod-Items: das Wiki-Bild (`wiki/data/simplebuilding.json`: `icon` = 3D-Bild aus wiki/model_render.py für
   Blöcke, Treppen, Mauern, Köpfe, Truhen; sonst `texture`), wenn die Datei da ist.
2. Vanilla: `wiki/assets/textures/minecraft/<name>.png` - generate.py legt dort je Id ein Bild ab (auch die
   3D-Bilder), der Ordner ist nicht versioniert und fehlt in frischen Checkouts/Worktrees.
3. Sonst aus den Modellen, wie das Spiel: `items/<name>.json` -> Modell -> `textures` (layer0, all, front,
   side, ... über die Eltern), dann `models/item|block/<name>.json`, `textures/item|block/<name>.png`,
   `textures/item/<name>_00.png` (Kompass-artige Bildfolgen). Mod-Assets aus `src/main/resources` und
   `src/main/generated`, Vanilla direkt aus dem Client-Jar im Gradle-Cache.
   So bekommen auch Items ein Bild, die das Wiki bewusst nicht listet (alte Spachtel, der geheime Stock).

Ergebnis: ein Pfad, den der Server ausliefert - `wiki/assets/...`, `modtex/<ns>/<pfad>.png` (Mod-Texturen)
oder `vanilla/<pfad>.png` (aus dem Jar). Animierte Texturen (senkrechte Bildstreifen) tragen `anim`, die
Oberfläche zeigt dann nur das erste Bild.
"""

from __future__ import annotations

import json
import re
import struct
import threading
import zipfile
from pathlib import Path

from . import vanilla

WIKI_DATA = "wiki/data/simplebuilding.json"
WIKI_VANILLA = "wiki/assets/textures/minecraft"
MOD_ASSETS = ("src/main/resources/assets", "src/main/generated/assets")
TEXTURE_KEYS = ("layer0", "all", "texture", "front", "side", "top", "end", "wool", "wall", "cross", "particle")
SAFE = re.compile(r"^[a-z0-9_./-]+$")


def png_size(head: bytes) -> tuple[int, int] | None:
    if len(head) < 24 or head[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    return struct.unpack(">II", head[16:24])


class Resolver:
    def __init__(self, repo: Path, jar: Path | None = None):
        self.repo = Path(repo)
        self.jar = Path(jar) if jar else vanilla.client_jar()
        self._names: set[str] | None = None
        self._archive: zipfile.ZipFile | None = None
        self._lock = threading.Lock()
        self.wiki = self._wiki_icons()

    # ---- Quellen ------------------------------------------------------------------------------

    def _wiki_icons(self) -> dict[str, list[str]]:
        path = self.repo / WIKI_DATA
        out: dict[str, list[str]] = {}
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            return out
        for collection in ("items", "blocks"):
            for entry in data.get(collection, []):
                pics = [p for p in (entry.get("icon"), entry.get("texture")) if p]
                if pics:
                    out.setdefault(entry["id"], []).extend(pics)
        return out

    def jar_names(self) -> set[str]:
        if self._names is None:
            with self._lock:
                if self._names is None:
                    names: set[str] = set()
                    if self.jar.exists():
                        try:
                            with zipfile.ZipFile(self.jar) as archive:
                                names = {n for n in archive.namelist() if n.startswith("assets/minecraft/")}
                        except (OSError, zipfile.BadZipFile):
                            names = set()
                    self._names = names
        return self._names

    def jar_read(self, entry: str) -> bytes | None:
        if entry not in self.jar_names():
            return None
        with self._lock:
            if self._archive is None:
                self._archive = zipfile.ZipFile(self.jar)
            return self._archive.read(entry)

    def _read_json(self, ns: str, rel: str) -> dict | None:
        if ns == "minecraft":
            raw = self.jar_read(f"assets/minecraft/{rel}")
            if raw is None:
                return None
            try:
                return json.loads(raw.decode("utf-8"))
            except (UnicodeDecodeError, json.JSONDecodeError):
                return None
        for root in MOD_ASSETS:
            path = self.repo / root / ns / rel
            if path.is_file():
                try:
                    return json.loads(path.read_text(encoding="utf-8"))
                except (OSError, json.JSONDecodeError):
                    return None
        return None

    def _texture(self, ref: str) -> str | None:
        ns, _, path = ref.partition(":") if ":" in ref else ("minecraft", "", ref)
        if not SAFE.match(path):
            return None
        if ns == "minecraft":
            return f"vanilla/{path}.png" if f"assets/minecraft/textures/{path}.png" in self.jar_names() else None
        for root in MOD_ASSETS:
            if (self.repo / root / ns / "textures" / f"{path}.png").is_file():
                return f"modtex/{ns}/{path}.png"
        return None

    def _model_textures(self, ref: str, depth: int = 0) -> list[str]:
        """Textur-Verweise eines Modells in Vorzugsreihenfolge (Eltern eingeschlossen, #Variablen aufgelöst)."""
        textures: dict[str, str] = {}
        chain = ref
        for _ in range(8):
            ns, _, path = chain.partition(":") if ":" in chain else ("minecraft", "", chain)
            model = self._read_json(ns, f"models/{path}.json")
            if not model:
                break
            for key, value in (model.get("textures") or {}).items():
                if isinstance(value, str):
                    textures.setdefault(key, value)
            parent = model.get("parent")
            if not isinstance(parent, str) or parent.startswith("builtin/"):
                break
            chain = parent

        def resolve(value: str, hops: int = 0) -> str | None:
            while value.startswith("#") and hops < 8:
                value = textures.get(value[1:], "")
                hops += 1
            return value or None
        ordered = [textures[k] for k in TEXTURE_KEYS if k in textures] + list(textures.values())
        out = []
        for value in ordered:
            got = resolve(value)
            if got and got not in out:
                out.append(got)
        return out

    def _item_definition_models(self, ns: str, name: str) -> list[str]:
        data = self._read_json(ns, f"items/{name}.json")
        found: list[str] = []

        def walk(node):
            if isinstance(node, dict):
                if node.get("type") in ("minecraft:model", "model") and isinstance(node.get("model"), str):
                    found.append(node["model"])
                for value in node.values():
                    walk(value)
            elif isinstance(node, list):
                for value in node:
                    walk(value)
        if data:
            walk(data.get("model"))
        return found

    # ---- Auflösen ---------------------------------------------------------------------------------

    def resolve(self, key: str) -> dict | None:
        """-> {"icon": Pfad, "anim": bool, "via": Quelle} oder None."""
        if not key or key.startswith("#"):
            return None
        if key.startswith("book:"):
            key = "minecraft:enchanted_book"
        ns, _, name = key.partition(":")
        if not name or not SAFE.match(name):
            return None
        for pic in self.wiki.get(key, []):
            if (self.repo / "wiki" / pic).is_file():
                return self._result("wiki/" + pic, "wiki")
        if ns == "minecraft":
            flat = f"{WIKI_VANILLA}/{name}.png"
            if (self.repo / flat).is_file():
                return self._result(flat, "wiki")
        candidates: list[str] = []
        for model in self._item_definition_models(ns, name):
            candidates += self._model_textures(model)
        candidates += self._model_textures(f"{ns}:item/{name}") + self._model_textures(f"{ns}:block/{name}")
        candidates += [f"{ns}:item/{name}", f"{ns}:block/{name}", f"{ns}:item/{name}_00"]
        for ref in candidates:
            icon = self._texture(ref)
            if icon:
                return self._result(icon, "model")
        return None

    def _result(self, icon: str, via: str) -> dict:
        head = self.read(icon, head_only=True)
        size = png_size(head or b"")
        return {"icon": icon, "anim": bool(size and size[1] > size[0]), "via": via}

    # ---- Ausliefern ---------------------------------------------------------------------------------

    def read(self, url: str, head_only: bool = False) -> bytes | None:
        """Bytes hinter einem Bild-Pfad (wiki/..., modtex/..., vanilla/...) oder None. Nur PNGs, nur in den
        erlaubten Ordnern."""
        url = url.lstrip("/")
        if not url.endswith(".png") or ".." in url or "\\" in url:
            return None
        if url.startswith("wiki/assets/"):
            path = (self.repo / url).resolve()
            base = (self.repo / "wiki" / "assets").resolve()
            if base not in path.parents or not path.is_file():
                return None
            return _head(path) if head_only else path.read_bytes()
        if url.startswith("modtex/"):
            ns, _, rest = url[len("modtex/"):].partition("/")
            if not SAFE.match(ns) or not SAFE.match(rest):
                return None
            for root in MOD_ASSETS:
                base = (self.repo / root / ns / "textures").resolve()
                path = (base / rest).resolve()
                if base in path.parents and path.is_file():
                    return _head(path) if head_only else path.read_bytes()
            return None
        if url.startswith("vanilla/"):
            rest = url[len("vanilla/"):]
            if not SAFE.match(rest):
                return None
            data = self.jar_read(f"assets/minecraft/textures/{rest}")
            return data[:32] if (data is not None and head_only) else data
        return None


def _head(path: Path) -> bytes:
    with open(path, "rb") as handle:
        return handle.read(32)
