"""
Isometric inventory icons for the wiki, drawn from the real block and item models.

The game draws a block in an inventory slot from its model: the parent chain gives the
elements (boxes with per-face textures, UVs, UV rotation, element rotation), the item
definition (assets/<ns>/items/<id>.json) says which model the item shows, and the model's
display.gui transform turns it to the familiar three-quarter view. This module does the
same in Python so stairs, slabs, walls, pistons, pressure plates, pads, heads and the
placed smithing template look like they do in the game instead of showing one face.

    renderer = IconRenderer(own_asset_dirs, client_jar)
    image = renderer.render_item("simplebuilding:ender_quartz_stairs")   # PIL image or None

Only what a static page needs: orthographic projection, a z-buffer, nearest-neighbour
texture sampling (pixel art stays crisp), the game's per-face GUI shading, cutout alpha.
Flat items (parent item/generated) are NOT drawn here - their icon is their texture.
Needs Pillow and numpy; wiki/generate.py skips rendering when either is missing, so the
committed icons stay what they are (the generated JSON only records icons that exist).
"""

from __future__ import annotations

import json
import math
import zipfile
from pathlib import Path

try:
    import numpy as np
    from PIL import Image
    AVAILABLE = True
except ImportError:  # CI and plain Python installs: generate.py then keeps the committed icons
    np = None
    Image = None
    AVAILABLE = False

SIZE = 128          # canvas edge in pixels; one slot is 16 units, so 8 px per slot pixel
ALPHA_CUTOFF = 0.1  # the game's cutout threshold

# display.gui of minecraft:block/block - used when a model (a block without item) has none
DEFAULT_GUI = {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]}

# Skull kinds -> texture and head box (texOffs, size) as in SkullModel / PiglinHeadModel.
HEAD_KINDS = {
    "skeleton": ("minecraft:entity/skeleton/skeleton", 64, 32),
    "wither_skeleton": ("minecraft:entity/skeleton/wither_skeleton", 64, 32),
    "zombie": ("minecraft:entity/zombie/zombie", 64, 64),
    "creeper": ("minecraft:entity/creeper/creeper", 64, 32),
    "piglin": ("minecraft:entity/piglin/piglin", 64, 64),
    "player": ("minecraft:entity/player/wide/steve", 64, 64),
}


def rl(value: str) -> tuple[str, str]:
    """'block/stone' -> ('minecraft', 'block/stone'); 'ns:path' -> ('ns', 'path')."""
    value = value.lstrip("#")
    if ":" in value:
        ns, path = value.split(":", 1)
        return ns, path
    return "minecraft", value


def _rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def _rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def _rot_z(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def euler_xyz(rotation) -> np.ndarray:
    """Quaternionf.rotationXYZ(x, y, z) as a matrix: Rx * Ry * Rz (z applied first)."""
    rx, ry, rz = (math.radians(float(v)) for v in (list(rotation) + [0, 0, 0])[:3])
    return _rot_x(rx) @ _rot_y(ry) @ _rot_z(rz)


# Corners of each face as the texture sees it: top-left, top-right, bottom-left (the fourth
# follows), in model pixels, from the element's from (f) and to (t).
def _face_corners(face: str, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    return {
        "up":    ((x0, y1, z0), (x1, y1, z0), (x0, y1, z1)),
        "down":  ((x0, y0, z1), (x1, y0, z1), (x0, y0, z0)),
        "north": ((x1, y1, z0), (x0, y1, z0), (x1, y0, z0)),
        "south": ((x0, y1, z1), (x1, y1, z1), (x0, y0, z1)),
        "west":  ((x0, y1, z0), (x0, y1, z1), (x0, y0, z0)),
        "east":  ((x1, y1, z1), (x1, y1, z0), (x1, y0, z1)),
    }[face]


def _default_uv(face: str, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    return {
        "up": (x0, z0, x1, z1),
        "down": (x0, 16 - z1, x1, 16 - z0),
        "north": (16 - x1, 16 - y1, 16 - x0, 16 - y0),
        "south": (x0, 16 - y1, x1, 16 - y0),
        "west": (z0, 16 - y1, z1, 16 - y0),
        "east": (16 - z1, 16 - y1, 16 - z0, 16 - y0),
    }[face]


NORMALS = {"up": (0, 1, 0), "down": (0, -1, 0), "north": (0, 0, -1),
           "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0)}


def item_parts(node, tints=None):
    """
    The models an item definition shows by default, as [(kind, payload, tints)]: kind "model"
    (payload = model id) or "special" (payload = the special node). Conditions take their
    false branch (undyed backpack, pad not cooling down), selects and range dispatches their
    fallback or first case, composites all parts.
    """
    if not isinstance(node, dict):
        return []
    kind = str(node.get("type", "")).replace("minecraft:", "")
    if kind == "model":
        return [("model", node.get("model"), node.get("tints") or tints)]
    if kind == "composite":
        out = []
        for part in node.get("models", []):
            out += item_parts(part, tints)
        return out
    if kind == "special":
        return [("special", node, None)]
    if kind == "empty":
        return [("empty", None, None)]
    if kind == "condition":
        return item_parts(node.get("on_false"), tints)
    if kind in ("select", "range_dispatch"):
        # A slot is the "gui" display context: spears and tridents show their icon model there.
        if str(node.get("property", "")).replace("minecraft:", "") == "display_context":
            for case in node.get("cases", []):
                when = case.get("when")
                if when == "gui" or (isinstance(when, list) and "gui" in when):
                    return item_parts(case.get("model"), tints)
        if node.get("fallback"):
            return item_parts(node["fallback"], tints)
        for key in ("cases", "entries"):
            if node.get(key):
                return item_parts(node[key][0].get("model"), tints)
        return []
    for key in ("fallback", "model", "on_false"):
        if isinstance(node.get(key), dict):
            return item_parts(node[key], tints)
    return []


def tint_color(tint: dict | None) -> tuple[float, float, float] | None:
    """The colour an item-definition tint source gives an undyed, default item."""
    if not isinstance(tint, dict):
        return None
    kind = tint.get("type", "").replace("minecraft:", "")
    value = None
    if kind == "constant":
        value = tint.get("value")
    elif kind in ("dye", "firework", "potion", "map_color", "custom_model_data", "team"):
        value = tint.get("default")
    elif kind == "grass":
        value = 0x7CBD6B
    if not isinstance(value, int) or value == -1:
        return None
    value &= 0xFFFFFF
    return ((value >> 16) & 255) / 255.0, ((value >> 8) & 255) / 255.0, (value & 255) / 255.0


class Assets:
    """Models, item definitions, blockstates and textures of the mod (folders) and vanilla (jar)."""

    def __init__(self, own: dict[str, list[Path]], jar: Path | None):
        self.own = own                       # namespace -> asset roots (assets/<ns>)
        self.jar = zipfile.ZipFile(jar) if jar and Path(jar).exists() else None
        self.names = set(self.jar.namelist()) if self.jar else set()
        self._json: dict = {}
        self._tex: dict = {}

    def _read(self, ns: str, relpath: str) -> bytes | None:
        for root in self.own.get(ns, []):
            path = root / relpath
            if path.exists():
                return path.read_bytes()
        if self.jar:
            entry = f"assets/{ns}/{relpath}"
            if entry in self.names:
                return self.jar.read(entry)
        return None

    def json(self, folder: str, ref: str):
        ns, path = rl(ref)
        key = (folder, ns, path)
        if key not in self._json:
            payload = self._read(ns, f"{folder}/{path}.json")
            try:
                self._json[key] = json.loads(payload.decode("utf-8")) if payload else None
            except (json.JSONDecodeError, UnicodeDecodeError):
                self._json[key] = None
        return self._json[key]

    def texture(self, ref: str):
        """RGBA float array (h, w, 4) of the first animation frame, or None."""
        ns, path = rl(ref)
        key = (ns, path)
        if key not in self._tex:
            payload = self._read(ns, f"textures/{path}.png")
            arr = None
            if payload:
                import io
                image = Image.open(io.BytesIO(payload)).convert("RGBA")
                w, h = image.size
                if self._read(ns, f"textures/{path}.png.mcmeta") is not None and h > w:
                    image = image.crop((0, 0, w, w))
                arr = np.asarray(image, dtype=np.float32) / 255.0
            self._tex[key] = arr
        return self._tex[key]


class Face:
    __slots__ = ("corners", "uv", "texture", "rotation", "normal", "tint", "shade")

    def __init__(self, corners, uv, texture, rotation, normal, tint, shade):
        self.corners = corners      # 3 points (TL, TR, BL) in model pixels, np arrays
        self.uv = uv                # (u0, v0, u1, v1) in TEXTURE pixels
        self.texture = texture      # RGBA array
        self.rotation = rotation    # 0/90/180/270
        self.normal = normal        # np array, model space
        self.tint = tint            # rgb or None
        self.shade = shade


class IconRenderer:
    def __init__(self, own: dict[str, list[Path]], jar: Path | None):
        self.assets = Assets(own, jar)
        self.problems: list[str] = []
        self.context = ""

    # ------------------------------------------------------------------ models

    def resolve_model(self, ref: str) -> dict | None:
        """Parent chain merged: textures (child wins), elements (nearest), display, gui_light, flat."""
        chain = []
        seen = set()
        current = ref
        while current and current not in seen and len(chain) < 16:
            seen.add(current)
            if current.replace("minecraft:", "") in ("builtin/generated",):
                chain.append({"__flat__": True})
                break
            model = self.assets.json("models", current)
            if model is None:
                self.problems.append(f"model {current} not found (parent chain of {ref})")
                return None
            chain.append(model)
            parent = model.get("parent")
            current = (parent if ":" in parent else "minecraft:" + parent) if isinstance(parent, str) else None
        textures: dict = {}
        elements = None
        display: dict = {}
        gui_light = None
        flat = False
        for model in reversed(chain):
            if model.get("__flat__"):
                flat = True
                continue
            textures.update(model.get("textures") or {})
            if "elements" in model:
                elements = model["elements"]
                flat = False
            for key, value in (model.get("display") or {}).items():
                display[key] = value
            if model.get("gui_light"):
                gui_light = model["gui_light"]
        return {"textures": textures, "elements": elements, "display": display,
                "gui_light": gui_light or "side", "flat": flat}

    @staticmethod
    def texture_ref(textures: dict, ref: str) -> str | None:
        for _ in range(10):
            if isinstance(ref, dict):  # 26.x: {"sprite": "...", "force_translucent": true}
                ref = ref.get("sprite")
            if not isinstance(ref, str):
                return None
            if not ref.startswith("#") and "/" not in ref and ":" not in ref and ref in textures:
                ref = "#" + ref  # heavy_core writes "texture": "all" without the '#'
            if not ref.startswith("#"):
                return ref
            ref = textures.get(ref[1:])
        return None

    def model_faces(self, model: dict, tints=None) -> list[Face]:
        faces: list[Face] = []
        for element in model["elements"] or []:
            f = [float(v) for v in element["from"]]
            t = [float(v) for v in element["to"]]
            rotation = element.get("rotation")
            shade = element.get("shade", True)
            for name, spec in (element.get("faces") or {}).items():
                ref = self.texture_ref(model["textures"], spec.get("texture", ""))
                tex = self.assets.texture(ref) if ref else None
                if tex is None:
                    self.problems.append(f"{self.context}: texture {ref or spec.get('texture')} missing")
                    continue
                h, w = tex.shape[:2]
                uv = spec.get("uv") or _default_uv(name, f, t)
                uv = (uv[0] * w / 16, uv[1] * h / 16, uv[2] * w / 16, uv[3] * h / 16)
                corners = [np.array(c, dtype=float) for c in _face_corners(name, f, t)]
                normal = np.array(NORMALS[name], dtype=float)
                if rotation:
                    corners, normal = self._rotate_element(corners, normal, rotation)
                tint = None
                if "tintindex" in spec and tints:
                    index = int(spec["tintindex"])
                    if 0 <= index < len(tints):
                        tint = tint_color(tints[index])
                faces.append(Face(corners, uv, tex, int(spec.get("rotation", 0)) % 360, normal, tint, shade))
        return faces

    @staticmethod
    def _rotate_element(corners, normal, rotation):
        origin = np.array(rotation.get("origin", [8, 8, 8]), dtype=float)
        axis = rotation.get("axis")
        angle = rotation.get("angle", 0)
        if axis is None:  # {"x": 22.5} form
            axis = next((k for k in ("x", "y", "z") if k in rotation), "y")
            angle = rotation.get(axis, 0)
        angle = math.radians(float(angle))
        matrix = {"x": _rot_x, "y": _rot_y, "z": _rot_z}[axis](angle)
        scale = np.ones(3)
        if rotation.get("rescale"):
            factor = 1.0 / max(math.cos(abs(angle)), 1e-6)
            scale = np.array([1.0 if axis == "x" else factor, 1.0 if axis == "y" else factor,
                              1.0 if axis == "z" else factor])
        out = [origin + matrix @ ((c - origin) * scale) for c in corners]
        return out, matrix @ normal

    # ------------------------------------------------------------------ items

    def render_item(self, item_id: str):
        self.context = item_id
        definition = self.assets.json("items", item_id)
        if definition is None:
            return None
        faces: list[Face] = []
        gui = None
        light = "side"
        for kind, payload, tints in item_parts(definition.get("model")):
            if kind == "special":
                special = self._special_faces(payload)
                if special is None:
                    return None
                part_faces, part_gui = special
                faces += part_faces
                gui = gui or part_gui
                continue
            model = self.resolve_model(payload) if payload else None
            if not model or model["flat"] or not model["elements"]:
                return None
            faces += self.model_faces(model, tints)
            gui = gui or model["display"].get("gui")
            light = model["gui_light"]
        if not faces:
            return None
        return self.draw(faces, gui or DEFAULT_GUI, light)

    def render_block_model(self, model_ref: str):
        self.context = model_ref
        model = self.resolve_model(model_ref)
        if not model or model["flat"] or not model["elements"]:
            return None
        faces = self.model_faces(model)
        if not faces:
            return None
        return self.draw(faces, model["display"].get("gui") or DEFAULT_GUI, model["gui_light"])

    def blockstate_model(self, block_id: str) -> str | None:
        """The model of the block's first variant (what a block without an item looks like)."""
        state = self.assets.json("blockstates", block_id)
        if not state:
            return None
        variants = state.get("variants")
        if isinstance(variants, dict) and variants:
            first = variants[sorted(variants)[0]] if "" not in variants else variants[""]
            first = first[0] if isinstance(first, list) else first
            return first.get("model")
        for part in state.get("multipart", []):
            if "when" not in part:
                apply = part.get("apply")
                apply = apply[0] if isinstance(apply, list) else apply
                return apply.get("model")
        return None

    # ------------------------------------------------------------------ special models

    @staticmethod
    def _special_kind(node):
        model = node.get("model") or {}
        kind = str(model.get("type", "")).replace("minecraft:", "")
        if kind == "head":
            head = str(model.get("kind", ""))
            return "head" if (head in HEAD_KINDS or ":" in head) else None
        if kind == "chest":
            return "chest"
        return None

    def head_texture(self, kind: str):
        if kind in HEAD_KINDS:
            ref, w, h = HEAD_KINDS[kind]
        else:
            ns, name = rl(kind)
            ref, w, h = f"{ns}:entity/{name}_head", 64, 32
        return ref, w, h

    def head_faces(self, kind: str) -> list[Face] | None:
        ref, _, _ = self.head_texture(kind)
        tex = self.assets.texture(ref)
        if tex is None:
            self.problems.append(f"head texture {ref} missing")
            return None
        if kind == "piglin":
            faces = self.box((3, 0, 4), (13, 8, 12), tex, (0, 0), (10, 8, 8))
            faces += self.box((6, 0, 12), (10, 4, 13), tex, (31, 1), (4, 4, 1))
        else:
            faces = self.box((4, 0, 4), (12, 8, 12), tex, (0, 0), (8, 8, 8))
            # Humanoid heads (64x64: zombie, player) carry a hat layer at (32, 0), 0.25 px out.
            if tex.shape[0] == tex.shape[1] and kind in ("zombie", "player"):
                faces += self.box((3.75, -0.25, 3.75), (12.25, 8.25, 12.25), tex, (32, 0), (8, 8, 8))
        return faces

    def box(self, f, t, tex, offset, size, flip_y: bool = False) -> list[Face]:
        """
        An entity-model cube (texOffs + size) with its face at +z, as a skull item shows it.
        flip_y: the model is drawn upside down in the game (chests), so top and bottom trade
        their texture regions and the sides are read bottom-up.
        """
        u, v = offset
        w, h, d = size
        regions = {
            "up": (u + d, v, u + d + w, v + d),
            "down": (u + d + w, v + d, u + d + 2 * w, v),
            "west": (u, v + d, u + d, v + d + h),
            "south": (u + d, v + d, u + d + w, v + d + h),
            "east": (u + d + w, v + d, u + 2 * d + w, v + d + h),
            "north": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
        }
        if flip_y:
            regions["up"], regions["down"] = (u + d + w, v, u + d + 2 * w, v + d), (u + d, v + d, u + d + w, v)
            for side in ("west", "south", "east", "north"):
                a, b, c, e = regions[side]
                regions[side] = (a, e, c, b)
        faces = []
        for name, uv in regions.items():
            corners = [np.array(c, dtype=float) for c in _face_corners(name, f, t)]
            faces.append(Face(corners, uv, tex, 0, np.array(NORMALS[name], dtype=float), None, True))
        return faces

    def chest_faces(self, texture_ref: str) -> list[Face] | None:
        """ChestModel: body 14x10x14, lid 14x5x14 on top, lock 2x4x1 in front (+z)."""
        tex = self.assets.texture(texture_ref)
        if tex is None:
            self.problems.append(f"chest texture {texture_ref} missing")
            return None
        faces = self.box((1, 0, 1), (15, 10, 15), tex, (0, 19), (14, 10, 14), flip_y=True)
        faces += self.box((1, 9, 1), (15, 14, 15), tex, (0, 0), (14, 5, 14), flip_y=True)
        faces += self.box((7, 7, 15), (9, 11, 16), tex, (0, 0), (2, 4, 1), flip_y=True)
        return faces

    def _special_faces(self, node):
        kind = self._special_kind(node)
        if kind == "chest":
            texture = str(node["model"].get("texture", "minecraft:normal"))
            ns, name = rl(texture)
            faces = self.chest_faces(f"{ns}:entity/chest/{name}")
            if faces is None:
                return None
            base = self.resolve_model(node.get("base") or "minecraft:item/chest")
            return faces, (base or {}).get("display", {}).get("gui") or {"rotation": [30, 45, 0], "scale": [0.625] * 3}
        if kind != "head":
            return None
        faces = self.head_faces(str(node["model"].get("kind")))
        if faces is None:
            return None
        base = self.resolve_model(node.get("base") or "minecraft:item/template_skull")
        gui = (base or {}).get("display", {}).get("gui") or {"rotation": [30, 45, 0], "translation": [0, 3, 0], "scale": [1, 1, 1]}
        return faces, gui

    def render_head(self, kind: str):
        faces = self.head_faces(kind)
        if faces is None:
            return None
        base = self.resolve_model("minecraft:item/template_skull")
        gui = (base or {}).get("display", {}).get("gui") or {"rotation": [30, 45, 0], "translation": [0, 3, 0], "scale": [1, 1, 1]}
        return self.draw(faces, gui, "side")

    def render_lying_item(self, texture_ref: str, scale: float = 14 / 16, thickness: float = 1.4):
        """
        A flat item lying on the floor, as PlacedTemplateRenderer draws a placed smithing
        template: the item texture as a plate of `scale` block widths, `thickness` pixels thick.
        """
        tex = self.assets.texture(texture_ref)
        if tex is None:
            self.problems.append(f"texture {texture_ref} missing")
            return None
        size = 16 * scale
        lo = (16 - size) / 2
        hi = lo + size
        faces = []
        layers = max(2, int(round(thickness * 2)))
        for i in range(layers + 1):
            y = thickness * i / layers
            corners = [np.array(c, dtype=float) for c in ((lo, y, lo), (hi, y, lo), (lo, y, hi))]
            h, w = tex.shape[:2]
            faces.append(Face(corners, (0, 0, w, h), tex, 0, np.array((0, 1, 0), dtype=float), None,
                              True if i == layers else "edge"))
        # A bit larger than a block (0.625) and lifted: a flat plate would otherwise sit small at the bottom.
        return self.draw(faces, {"rotation": [30, 225, 0], "translation": [0, 3, 0], "scale": [0.9, 0.9, 0.9]}, "side")

    # ------------------------------------------------------------------ drawing

    @staticmethod
    def _brightness(normal) -> float:
        """GUI 'side' lighting: top brightest, left face lighter than the right one."""
        nx, ny, nz = normal
        return float(min(1.0, max(0.45, 0.787 - 0.1414 * nx + 0.246 * ny)))

    def draw(self, faces: list[Face], gui: dict, light: str = "side") -> Image.Image:
        rotation = euler_xyz(gui.get("rotation", [0, 0, 0]))
        translation = np.array([float(v) for v in (list(gui.get("translation", [0, 0, 0])) + [0, 0, 0])[:3]]) / 16.0
        scale = np.array([float(v) for v in (list(gui.get("scale", [1, 1, 1])) + [1, 1, 1])[:3]])

        def to_view(p):
            return translation + rotation @ (scale * (p / 16.0 - 0.5))

        color = np.zeros((SIZE, SIZE, 3), dtype=np.float32)
        alpha = np.zeros((SIZE, SIZE), dtype=np.float32)
        depth = np.full((SIZE, SIZE), -np.inf, dtype=np.float32)

        prepared = []
        for face in faces:
            pts = [to_view(c) for c in face.corners]
            normal = rotation @ (face.normal / np.where(scale == 0, 1, scale))
            norm = np.linalg.norm(normal)
            if norm == 0:
                continue
            normal = normal / norm
            if normal[2] <= 1e-4:
                continue  # back face - the game culls it
            center = (pts[1] + pts[2]) / 2
            prepared.append((center[2], face, pts, normal))
        prepared.sort(key=lambda item: item[0])  # back to front, for translucent pixels

        for _, face, pts, normal in prepared:
            self._raster(face, pts, normal, light, color, alpha, depth)

        rgba = np.dstack([color, alpha])
        rgba = (np.clip(rgba, 0, 1) * 255 + 0.5).astype(np.uint8)
        return Image.fromarray(rgba, "RGBA")

    def _raster(self, face: Face, pts, normal, light, color, alpha, depth):
        def screen(p):
            return np.array([SIZE / 2 + p[0] * SIZE, SIZE / 2 - p[1] * SIZE]), p[2]

        (s0, z0), (s1, z1), (s3, z3) = screen(pts[0]), screen(pts[1]), screen(pts[2])
        e1, e2 = s1 - s0, s3 - s0
        det = e1[0] * e2[1] - e1[1] * e2[0]
        if abs(det) < 1e-9:
            return
        s2 = s1 + e2
        xs = [s0[0], s1[0], s2[0], s3[0]]
        ys = [s0[1], s1[1], s2[1], s3[1]]
        x_lo, x_hi = max(0, int(math.floor(min(xs)))), min(SIZE, int(math.ceil(max(xs))))
        y_lo, y_hi = max(0, int(math.floor(min(ys)))), min(SIZE, int(math.ceil(max(ys))))
        if x_lo >= x_hi or y_lo >= y_hi:
            return
        gx, gy = np.meshgrid(np.arange(x_lo, x_hi) + 0.5, np.arange(y_lo, y_hi) + 0.5)
        dx, dy = gx - s0[0], gy - s0[1]
        s = (dx * e2[1] - dy * e2[0]) / det
        t = (e1[0] * dy - e1[1] * dx) / det
        inside = (s >= 0) & (s < 1) & (t >= 0) & (t < 1)
        if not inside.any():
            return

        # UV corners in texture pixels, rotated with the face's "rotation".
        u0, v0, u1, v1 = face.uv
        uv_corners = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]  # TL, TR, BR, BL
        shift = face.rotation // 90
        tl = uv_corners[(0 - shift) % 4]
        tr = uv_corners[(1 - shift) % 4]
        bl = uv_corners[(3 - shift) % 4]
        u = tl[0] + s * (tr[0] - tl[0]) + t * (bl[0] - tl[0])
        v = tl[1] + s * (tr[1] - tl[1]) + t * (bl[1] - tl[1])
        tex = face.texture
        th, tw = tex.shape[:2]
        ui = np.clip(np.floor(u).astype(int), 0, tw - 1)
        vi = np.clip(np.floor(v).astype(int), 0, th - 1)
        sample = tex[vi, ui]
        a = sample[..., 3]
        z = z0 + s * (z1 - z0) + t * (z3 - z0)
        region = (slice(y_lo, y_hi), slice(x_lo, x_hi))
        mask = inside & (a >= ALPHA_CUTOFF) & (z > depth[region])
        if not mask.any():
            return
        rgb = sample[..., :3].copy()
        if face.tint is not None:
            rgb *= np.array(face.tint, dtype=np.float32)
        if face.shade == "edge":
            rgb *= 0.55
        elif face.shade and light != "front":
            rgb *= self._brightness(normal)
        c = color[region]
        al = alpha[region]
        d = depth[region]
        a_m = a[mask][:, None]
        c[mask] = rgb[mask] * a_m + c[mask] * (1 - a_m)
        al[mask] = a[mask] + al[mask] * (1 - a[mask])
        d[mask] = z[mask]
