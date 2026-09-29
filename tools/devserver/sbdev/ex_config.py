"""
Config-Standardwerte aus SimplebuildingConfig.java, TweaksConfig.java und ServerTuningConfig.java
(Reiter "Server & Modpack Tuning") - derselbe Gang durch die Felder wie ConfigOptions in der Mod
(Gruppen = Felder, deren Typ eine Config-Klasse ist; static und @Gui.Excluded fallen weg). Dazu
Zeile, Grenzen (@BoundedDiscrete, Math.max(n, feld), nonNegative(feld, ...), Math.min(max, feld),
clamp(feld, min, max)), Reiter und Namen/Tooltips aus den Sprachdateien.

Klassennamen gelten je Datei: SimplebuildingConfig.Tools und ServerTuningConfig.Tools sind zwei
verschiedene Klassen (die Suche nimmt zuerst die Klasse aus derselben Datei).

Der Standardwert ist ein Wert der Mod (was ein neuer Server bekommt). Speichern ersetzt den
Feldinitialisierer (javaedit) in der Linie 26.2 und im Zwilling der Linie 1.21.11. Eine bestehende
config/simplebuilding.json behaelt ihre Werte.
"""

from __future__ import annotations

import json
import re
from pathlib import Path

from . import javasrc, sites
from .values import problem, value

CONFIG = "common/src/shared/java/com/simplebuilding/config/SimplebuildingConfig.java"
TWEAKS = "common/src/shared/java/com/simplebuilding/tweaks/TweaksConfig.java"
SERVER = "common/src/shared/java/com/simplebuilding/config/ServerTuningConfig.java"
SERVER_CONSTANTS = "common/src/shared/java/com/simplebuilding/config/ServerTuning.java"
OPTIONS = "common/src/shared/java/com/simplebuilding/config/ConfigOptions.java"
LANG = "src/main/resources/assets/simplebuilding/lang"
FILES = (CONFIG, TWEAKS, SERVER)

FIELD = re.compile(r"^\s*public\s+(static\s+)?(?:final\s+)?([\w.]+)\s+(\w+)\s*=\s*([^;]+);", re.M)
CLASS = re.compile(r"\bclass\s+(\w+)")
VALUE_TYPES = {"boolean": "bool", "int": "int", "long": "int", "double": "float", "float": "float", "String": "string"}
DEFAULT_NOTE = ("Speichern ersetzt den Standard im Java-Feld (26.2/26.3/26.4 und 1.21.11) - ein neuer Server "
                "bekommt ihn; bestehende config/simplebuilding.json behalten ihre Werte.")


def _classes(raw: str, text: str, rel: str):
    """Klassenname -> Liste der direkten Felder mit Annotationen, Zeile und Bereich des Initialisierers."""
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
                    "doc": javasrc.doc_before(raw, fm.start()), "file": rel,
                })
        classes.setdefault(cm.group(1), body_fields)
    return classes


def _load(repo: Path, rel: str):
    raw = (repo / rel).read_text(encoding="utf-8")
    raw = raw.replace("\r\n", "\n")
    text = javasrc.blank_comments(raw)
    return raw, text


def _server_constants(repo: Path, rel: str) -> dict[str, tuple]:
    path = repo / rel
    if not path.exists():
        return {}
    text = javasrc.blank_comments(path.read_text(encoding="utf-8"))
    env: dict[str, tuple] = {}
    for m in re.finditer(r"static\s+final\s+(?:int|double|float|long)\s+([A-Z][A-Z0-9_]*)\s*=\s*([^;]+);", text):
        try:
            env[m.group(1)] = javasrc.evaluate(m.group(2), env)
        except javasrc.ExprError:
            pass
    return env


def _bounds(texts: list[str], env: dict) -> dict[str, dict]:
    """Feldname -> {"min", "max"} aus den validate()-Methoden (Schluessel: letzter Namensteil)."""
    out: dict[str, dict] = {}
    joined = "\n".join(texts)

    def num(token: str):
        token = token.strip()
        try:
            return float(javasrc.evaluate(token, dict(env, **{f"ServerTuning.{k}": v for k, v in env.items()}))[0])
        except javasrc.ExprError:
            return None
    for m in re.finditer(r"([\w.]+)\s*=\s*Math\.max\(\s*(-?[\d.]+)\s*,\s*\1\s*\)", joined):
        out.setdefault(m.group(1).split(".")[-1], {})["min"] = float(m.group(2))
    for m in re.finditer(r"([\w.]+)\s*=\s*nonNegative\(\s*\1\s*,", joined):
        out.setdefault(m.group(1).split(".")[-1], {})["min"] = 0.0
    for m in re.finditer(r"([\w.]+)\s*=\s*Math\.max\(\s*(-?[\d.]+)\s*,\s*Math\.min\(\s*(\w+|[\d.]+)\s*,\s*\1\s*\)\s*\)", joined):
        entry = out.setdefault(m.group(1).split(".")[-1], {})
        entry["min"] = float(m.group(2))
        entry["maxRef"] = m.group(3)
    for m in re.finditer(r"([\w.]+)\s*=\s*clamp\(\s*\1\s*,\s*([^,()]+)\s*,\s*([^,()]+)\s*[,)]", joined):
        lo, hi = num(m.group(2)), num(m.group(3))
        entry = out.setdefault(m.group(1), {})
        if lo is not None:
            entry["min"] = lo
        if hi is not None:
            entry["max"] = hi
    return out


def extract(repo: Path) -> tuple[list[dict], list[dict], list[dict]]:
    problems: list[dict] = []
    values: list[dict] = []
    if not (repo / CONFIG).exists():
        return [], [], [problem("config", "Config-Klasse fehlt", file=CONFIG)]
    by_file: dict[str, dict[str, list[dict]]] = {}
    texts = []
    for rel in FILES:
        if not (repo / rel).exists():
            continue
        raw, text = _load(repo, rel)
        by_file[rel] = _classes(raw, text, rel)
        texts.append(text)
    env = _server_constants(repo, SERVER_CONSTANTS)
    bounds = _bounds(texts, env)
    trim_max = env.get("MAX_TRIM_STRENGTH", (None,))[0]
    statics = {f["name"]: f["init"] for classes in by_file.values() for fields in classes.values() for f in fields if f["static"]}

    def resolve(type_name: str, from_file: str):
        """Config-Klasse zum Typnamen - zuerst in derselben Datei."""
        if type_name in by_file.get(from_file, {}):
            return from_file, by_file[from_file][type_name]
        for rel, classes in by_file.items():
            if type_name in classes:
                return rel, classes[type_name]
        return None, None

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
    main_texts = {rel: _load(repo, rel)[1] for rel in by_file}

    def walk(fields, file_rel, class_name, path_prefix, category, group):
        for field in fields:
            annotations = " ".join(field["annotations"])
            if field["static"] or "Gui.Excluded" in annotations:
                continue
            cat = re.search(r'@ConfigEntry\.Category\("(\w+)"\)', annotations)
            tab = cat.group(1) if cat else category
            name = path_prefix + field["name"]
            type_name = field["type"].split(".")[-1]
            if type_name not in VALUE_TYPES:
                sub_file, sub_fields = resolve(type_name, file_rel)
                if sub_fields is not None:
                    walk(sub_fields, sub_file, type_name, name + ".", tab, name)
                    continue
                problems.append(problem("config", f"{name}: Typ {type_name} nicht unterstützt", file=file_rel, line=field["line"]))
                continue
            kind = VALUE_TYPES[type_name]
            init = field["init"]
            literal = True
            try:
                if kind == "bool":
                    default = {"true": True, "false": False}[init]
                elif kind == "string":
                    default = json.loads(init)
                else:
                    number, is_int = javasrc.evaluate(init, {})
                    default = int(number) if kind == "int" else float(number)
                    literal = bool(re.fullmatch(r"-?\s*" + javasrc.NUMBER.pattern, init))
            except (KeyError, ValueError, javasrc.ExprError):
                problems.append(problem("config", f"{name}: Standard '{init}' nicht auslesbar",
                                        file=file_rel, line=field["line"], why="kein einfaches Literal"))
                continue
            lo = hi = None
            bd = re.search(r"BoundedDiscrete\(\s*min\s*=\s*(-?\d+)\s*,\s*max\s*=\s*(-?\d+)\s*\)", annotations)
            if bd:
                lo, hi = int(bd.group(1)), int(bd.group(2))
            b = bounds.get(name.split(".", 1)[1], {}) if name.startswith("server.") else bounds.get(field["name"], {})
            if "min" in b:
                lo = b["min"] if lo is None else max(lo, b["min"])
            if "max" in b:
                hi = b["max"] if hi is None else min(hi, b["max"])
            if "maxRef" in b:
                ref = b["maxRef"]
                try:
                    hi = float(ref) if re.fullmatch(r"[\d.]+", ref) else float(javasrc.evaluate(statics[ref], {})[0])
                except (KeyError, javasrc.ExprError, ValueError):
                    pass
            if class_name == "TrimStrengths" and kind == "float" and trim_max is not None:
                lo, hi = 0.0, float(trim_max)
            if kind == "int" and lo is not None:
                lo = int(lo)
            if kind == "int" and hi is not None:
                hi = int(hi)
            if "ColorPicker" in annotations:
                lo, hi = 0, 0xFFFFFF
            key = f"{prefix}option.{name}"
            label_de = de.get(key) or en.get(key) or name
            side = "Client" if name in client_side or field["name"] in client_side else "Server"
            text = main_texts[file_rel]
            source = sites.java_site(file_rel, text, javasrc.Lines(text), field["span"][0], field["span"][1],
                                     {"bool": "boolean", "string": "String"}.get(kind, type_name))
            source["lines"] = sites.main_lines(file_rel)
            record = value(f"config:{name}", "config", label_de, kind, default,
                           group=de.get(f"{prefix}category.{tab}") or en.get(f"{prefix}category.{tab}") or (tab or ""),
                           min=lo, max=hi, source=source, apply="mod" if literal else "plan",
                           refs={"path": name, "tab": tab, "side": side, "onReload": name in on_reload,
                                 "subgroup": de.get(f"{prefix}option.{group}") if group else None,
                                 "labelEn": en.get(key), "field": field["name"], "class": class_name},
                           note=(de.get(key + ".@Tooltip") or en.get(key + ".@Tooltip") or field["doc"] or "")[:600])
            record["refs"]["how"] = DEFAULT_NOTE
            if not literal:
                record["readonly"] = True
                record["derived"] = " ".join(init.split())
            if "ColorPicker" in annotations:
                record["refs"]["color"] = True
            values.append(record)
            options.append({"path": name, "id": record["id"], "tab": tab})

    walk(by_file[CONFIG]["SimplebuildingConfig"], CONFIG, "SimplebuildingConfig", "", None, None)
    if not options:
        problems.append(problem("config", "keine Optionen gefunden", file=CONFIG))

    # Zwillinge (1.21.11): dieselbe Auslese auf den Kopien der drei Dateien
    by_main_file: dict[str, dict[str, dict]] = {}
    for record in values:
        if not record.get("readonly"):
            by_main_file.setdefault(record["source"]["file"], {})[record["id"]] = record
    for rel, records in by_main_file.items():
        sites.attach_twins(repo, records, rel, lambda repo_, twin_rel, _main=rel: _twin_fields(repo_, twin_rel, _main, records))
    return options, values, problems


def _twin_fields(repo: Path, twin_rel: str, main_rel: str, records: dict[str, dict]) -> dict[str, dict]:
    """Id -> Stelle in der Zwillingsdatei: Feld gleichen Namens in der Klasse gleichen Namens."""
    raw, text = _load(repo, twin_rel)
    classes = _classes(raw, text, twin_rel)
    lines = javasrc.Lines(text)
    out = {}
    for vid, record in records.items():
        cls = record["refs"].get("class")
        field = next((f for f in classes.get(cls, []) if f["name"] == record["refs"].get("field") and not f["static"]), None)
        if field is None:
            continue
        site = sites.java_site(twin_rel, text, lines, field["span"][0], field["span"][1], record["source"]["jtype"])
        init = field["init"]
        try:
            if record["type"] == "bool":
                site["value"] = {"true": True, "false": False}[init]
            elif record["type"] == "string":
                site["value"] = json.loads(init)
            elif re.fullmatch(r"-?\s*" + javasrc.NUMBER.pattern, init):
                number, is_int = javasrc.evaluate(init, {})
                site["value"] = int(number) if record["type"] == "int" else float(number)
            else:
                site["value"] = None
        except (KeyError, ValueError, javasrc.ExprError):
            site["value"] = None
        out[vid] = site
    return out
