"""
Config-Standardwerte aus SimplebuildingConfig.java und TweaksConfig.java - derselbe Gang durch die
Felder wie ConfigOptions in der Mod (Gruppen = Felder, deren Typ eine Config-Klasse ist; static und
@Gui.Excluded fallen weg). Dazu Zeile, Grenzen (@BoundedDiscrete, Math.max(n, feld),
nonNegative(feld, ...), Math.min(max, feld)), Reiter und Namen/Tooltips aus den Sprachdateien.

Der Standardwert ist ein Wert der Mod (was ein neuer Server bekommt); ihn zu ändern ist Phase 2
(Java-Feldinitialisierer). Eine bestehende config/simplebuilding.json behaelt ihre Werte.
"""

from __future__ import annotations

import json
import re
from pathlib import Path

from . import javasrc
from .values import problem, value

CONFIG = "common/src/shared/java/com/simplebuilding/config/SimplebuildingConfig.java"
TWEAKS = "common/src/shared/java/com/simplebuilding/tweaks/TweaksConfig.java"
OPTIONS = "common/src/shared/java/com/simplebuilding/config/ConfigOptions.java"
LANG = "src/main/resources/assets/simplebuilding/lang"

FIELD = re.compile(r"^\s*public\s+(static\s+)?(?:final\s+)?([\w.]+)\s+(\w+)\s*=\s*([^;]+);", re.M)
CLASS = re.compile(r"\bclass\s+(\w+)")
VALUE_TYPES = {"boolean": "bool", "int": "int", "long": "int", "double": "float", "float": "float", "String": "string"}


def _classes(repo: Path, rel: str):
    """Klassenname -> Liste der direkten Felder mit Annotationen, Zeile und Bereich des Initialisierers."""
    raw = (repo / rel).read_text(encoding="utf-8")
    text = javasrc.blank_comments(raw)
    lines = javasrc.Lines(text)
    classes: dict[str, list[dict]] = {}
    for cm in CLASS.finditer(text):
        brace = text.index("{", cm.end())
        end = javasrc.closing(text, brace, "{", "}")
        body_fields = []
        depth = 0
        i = brace + 1
        # nur Felder auf Tiefe 0 dieser Klasse
        segments = []
        seg_start = i
        while i < end - 1:
            c = text[i]
            if c == "{":
                if depth == 0:
                    segments.append((seg_start, i))
                depth += 1
            elif c == "}":
                depth -= 1
                if depth == 0:
                    seg_start = i + 1
            elif c == '"':
                j = text.index('"', i + 1)
                while text[j - 1] == "\\":
                    j = text.index('"', j + 1)
                i = j
            i += 1
        segments.append((seg_start, end - 1))
        for a, b in segments:
            for fm in FIELD.finditer(text, a, b):
                decl_line_start = text.rfind("\n", 0, fm.start()) + 1
                annotations = []
                back = raw[:decl_line_start].rstrip("\n").splitlines()
                for line in reversed(back):
                    s = line.strip()
                    if s.startswith("@"):
                        annotations.append(s)
                    elif s.startswith("//") or s.startswith("*") or s.startswith("/*") or not s:
                        if not s:
                            break
                        continue
                    else:
                        break
                init_start = fm.start(4)
                body_fields.append({
                    "static": bool(fm.group(1)), "type": fm.group(2), "name": fm.group(3),
                    "init": fm.group(4).strip(), "span": [init_start, init_start + len(fm.group(4).rstrip())],
                    "line": lines.line(fm.start(3)), "annotations": annotations,
                    "doc": javasrc.doc_before(raw, fm.start()),
                })
        classes[cm.group(1)] = body_fields
    return classes, text


def _bounds(texts: list[str]) -> dict[str, dict]:
    out: dict[str, dict] = {}
    joined = "\n".join(texts)
    for m in re.finditer(r"([\w.]+)\s*=\s*Math\.max\(\s*(-?[\d.]+)\s*,\s*\1\s*\)", joined):
        out.setdefault(m.group(1).split(".")[-1], {})["min"] = float(m.group(2))
    for m in re.finditer(r"([\w.]+)\s*=\s*nonNegative\(\s*\1\s*,", joined):
        out.setdefault(m.group(1).split(".")[-1], {})["min"] = 0.0
    for m in re.finditer(r"([\w.]+)\s*=\s*Math\.max\(\s*(-?[\d.]+)\s*,\s*Math\.min\(\s*(\w+|[\d.]+)\s*,\s*\1\s*\)\s*\)", joined):
        entry = out.setdefault(m.group(1).split(".")[-1], {})
        entry["min"] = float(m.group(2))
        entry["maxRef"] = m.group(3)
    return out


def extract(repo: Path) -> tuple[list[dict], list[dict], list[dict]]:
    problems: list[dict] = []
    values: list[dict] = []
    if not (repo / CONFIG).exists():
        return [], [], [problem("config", "Config-Klasse fehlt", file=CONFIG)]
    classes, main_text = _classes(repo, CONFIG)
    files = {name: CONFIG for name in classes}
    texts = [main_text]
    if (repo / TWEAKS).exists():
        tweak_classes, tweak_text = _classes(repo, TWEAKS)
        classes.update(tweak_classes)
        files.update({name: TWEAKS for name in tweak_classes})
        texts.append(tweak_text)
    bounds = _bounds(texts)
    statics = {f["name"]: f["init"] for fields in classes.values() for f in fields if f["static"]}

    lang = {}
    for locale in ("en_us", "de_de"):
        path = repo / LANG / f"{locale}.json"
        if path.exists():
            lang[locale] = json.loads(path.read_text(encoding="utf-8"))
    en, de = lang.get("en_us", {}), lang.get("de_de", {})
    prefix = "text.autoconfig.simplebuilding."

    client_side, on_reload = set(), set()
    if (repo / OPTIONS).exists():
        opt = (repo / OPTIONS).read_text(encoding="utf-8")
        for name, target in (("CLIENT_SIDE", client_side), ("APPLY_ON_RELOAD", on_reload)):
            m = re.search(name + r"\s*=\s*Set\.of\(([^;]*)\);", opt)
            if m:
                target.update(re.findall(r'"([\w.]+)"', m.group(1)))

    options: list[dict] = []

    def walk(class_name, path_prefix, category, group):
        for field in classes.get(class_name, []):
            annotations = " ".join(field["annotations"])
            if field["static"] or "Gui.Excluded" in annotations:
                continue
            cat = re.search(r'@ConfigEntry\.Category\("(\w+)"\)', annotations)
            tab = cat.group(1) if cat else category
            name = path_prefix + field["name"]
            type_name = field["type"].split(".")[-1]
            if type_name in classes and type_name not in VALUE_TYPES:
                walk(type_name, name + ".", tab, name)
                continue
            kind = VALUE_TYPES.get(type_name)
            if not kind:
                problems.append(problem("config", f"{name}: Typ {type_name} nicht unterstützt",
                                        file=files[class_name], line=field["line"]))
                continue
            init = field["init"]
            try:
                if kind == "bool":
                    default = {"true": True, "false": False}[init]
                elif kind == "string":
                    default = json.loads(init)
                else:
                    number, is_int = javasrc.evaluate(init, {})
                    default = int(number) if kind == "int" else float(number)
            except (KeyError, ValueError, javasrc.ExprError):
                problems.append(problem("config", f"{name}: Standard '{init}' nicht auslesbar",
                                        file=files[class_name], line=field["line"], why="kein einfaches Literal"))
                continue
            lo = hi = None
            bd = re.search(r"BoundedDiscrete\(\s*min\s*=\s*(-?\d+)\s*,\s*max\s*=\s*(-?\d+)\s*\)", annotations)
            if bd:
                lo, hi = int(bd.group(1)), int(bd.group(2))
            b = bounds.get(field["name"], {})
            if "min" in b:
                lo = b["min"] if lo is None else max(lo, b["min"])
            if "maxRef" in b:
                ref = b["maxRef"]
                try:
                    hi = float(ref) if re.fullmatch(r"[\d.]+", ref) else float(javasrc.evaluate(statics[ref], {})[0])
                except (KeyError, javasrc.ExprError, ValueError):
                    pass
            if kind == "int" and lo is not None:
                lo = int(lo)
            if kind == "int" and hi is not None:
                hi = int(hi)
            if "ColorPicker" in annotations:
                lo, hi = 0, 0xFFFFFF
            key = f"{prefix}option.{name}"
            label_de = de.get(key) or en.get(key) or name
            side = "Client" if name in client_side or field["name"] in client_side else "Server"
            record = value(f"config:{name}", "config", label_de, kind, default,
                           group=de.get(f"{prefix}category.{tab}") or en.get(f"{prefix}category.{tab}") or (tab or ""),
                           min=lo, max=hi, source={"file": files[class_name], "line": field["line"], "span": field["span"]},
                           refs={"path": name, "tab": tab, "side": side, "onReload": name in on_reload,
                                 "subgroup": de.get(f"{prefix}option.{group}") if group else None,
                                 "labelEn": en.get(key)},
                           note=(de.get(key + ".@Tooltip") or en.get(key + ".@Tooltip") or field["doc"] or "")[:600])
            if "ColorPicker" in annotations:
                record["refs"]["color"] = True
            values.append(record)
            options.append({"path": name, "id": record["id"], "tab": tab})

    walk("SimplebuildingConfig", "", None, None)
    if not options:
        problems.append(problem("config", "keine Optionen gefunden", file=CONFIG))
    return options, values, problems
