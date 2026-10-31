#!/usr/bin/env python3
"""Static Mixin check against the 26.3 game jar (A-MIXCHECK31).

Our gametests run a dedicated server, so CLIENT mixins are never applied there; a client mixin whose
target vanished in 26.3 only crashes the player's client (LeggingsTextureMixin, 30.10.). This tool
reads every mixin config named in a fabric.mod.json of the given mod outputs (sections mixins, client,
server), parses the compiled mixin classes and checks them against the game jar (+ libraries):

  * target class exists (@Mixin value/targets; @Pseudo is skipped when missing)
  * @Shadow fields/methods and @Overwrite methods exist in the target class (name + descriptor)
  * @Accessor/@Invoker targets exist
  * injector selectors (@Inject, @Redirect, @ModifyArg(s), @ModifyVariable, @ModifyConstant and the
    MixinExtras @WrapOperation, @ModifyExpressionValue, @ModifyReturnValue, @WrapWithCondition,
    @ModifyReceiver, @WrapMethod) match a method of the target class
  * @At INVOKE/INVOKE_ASSIGN/FIELD/NEW targets occur as an instruction in a selected target method
  * @Inject handler arguments before the CallbackInfo equal the target's arguments (or are absent)
  * @Slice from/to selectors and @At CONSTANT / @ModifyConstant @Constant values occur in the target methods
  * handler signatures of @Redirect (INVOKE), @WrapOperation (INVOKE: receiver + arguments + Operation) and
    @ModifyArg (index >= 0): parameter count, primitive types and the return type (object types are compared
    loosely, a subclass or interface is fine); not checked: @Local captures, @ModifyArgs, @At FIELD handlers

Usage (Gradle passes explicit paths, see :integration:checkClientMixins):
  check_client_mixins.py --mods <dir|jar>... --cp <dir|jar>...
  check_client_mixins.py            # auto: 26.3 Fabric build outputs + Loom/Gradle caches of this machine
Exit 1 when anything does not resolve.
"""
from __future__ import annotations

import argparse
import glob
import json
import os
import struct
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MIXIN = "Lorg/spongepowered/asm/mixin/"
INJ = MIXIN + "injection/"
MX = "Lcom/llamalab/mixinextras/injector/"
INJECTORS = {
    INJ + "Inject;", INJ + "Redirect;", INJ + "ModifyArg;", INJ + "ModifyArgs;", INJ + "ModifyVariable;",
    INJ + "ModifyConstant;", MX + "wrapoperation/WrapOperation;", MX + "ModifyExpressionValue;",
    MX + "ModifyReturnValue;", MX + "WrapWithCondition;", MX + "ModifyReceiver;", MX + "wrapmethod/WrapMethod;",
    MX + "v2/WrapWithCondition;",
}
SHADOW, OVERWRITE = MIXIN + "Shadow;", MIXIN + "Overwrite;"
ACCESSOR, INVOKER = MIXIN + "gen/Accessor;", MIXIN + "gen/Invoker;"


# ---------------------------------------------------------------- class file parsing
class Member:
    __slots__ = ("access", "name", "desc", "ann", "insns")

    def __init__(self, access, name, desc, ann, insns):
        self.access, self.name, self.desc, self.ann, self.insns = access, name, desc, ann, insns


class ClassInfo:
    __slots__ = ("name", "access", "super", "interfaces", "fields", "methods", "ann")


def _oplen(code: bytes, pc: int) -> int:
    op = code[pc]
    if op in (0x10, 0x12) or 0x15 <= op <= 0x19 or 0x36 <= op <= 0x3a or op in (0xa9, 0xbc):
        return 2
    if op in (0x11, 0x13, 0x14, 0x84) or 0x99 <= op <= 0xa8 or 0xb2 <= op <= 0xb8 or op in (0xbb, 0xbd, 0xc0, 0xc1, 0xc6, 0xc7):
        return 3
    if op in (0xb9, 0xba, 0xc8, 0xc9):
        return 5
    if op == 0xc5:
        return 4
    if op == 0xc4:
        return 6 if code[pc + 1] == 0x84 else 4
    if op in (0xaa, 0xab):
        p = (pc + 4) & ~3
        if op == 0xaa:
            low, high = struct.unpack_from(">ii", code, p + 4)
            return p + 12 + (high - low + 1) * 4 - pc
        n = struct.unpack_from(">i", code, p + 4)[0]
        return p + 8 + n * 8 - pc
    return 1


def _constant(code: bytes, pc: int, op: int, cp: list):
    """The constant a load instruction pushes, as (kind, value), or None."""
    if 0x02 <= op <= 0x08:
        return "int", op - 3
    if op in (0x09, 0x0a):
        return "long", op - 9
    if 0x0b <= op <= 0x0d:
        return "float", float(op - 0x0b)
    if op in (0x0e, 0x0f):
        return "double", float(op - 0x0e)
    if op == 0x10:
        return "int", struct.unpack_from(">b", code, pc + 1)[0]
    if op == 0x11:
        return "int", struct.unpack_from(">h", code, pc + 1)[0]
    if op in (0x12, 0x13, 0x14):
        idx = code[pc + 1] if op == 0x12 else struct.unpack_from(">H", code, pc + 1)[0]
        e = cp[idx]
        if isinstance(e, tuple) and e and e[0] in ("int", "float", "long", "double"):
            return e[0], e[1]
        if isinstance(e, tuple) and len(e) == 2 and e[0] == 8:
            return "string", cp[e[1]]
    return None


def parse_class(data: bytes, with_code: bool) -> ClassInfo:
    off = 10
    count = struct.unpack_from(">H", data, 8)[0]
    cp: list = [None] * count
    i = 1
    while i < count:
        tag = data[off]
        if tag == 1:
            ln = struct.unpack_from(">H", data, off + 1)[0]
            cp[i] = data[off + 3:off + 3 + ln].decode("utf-8", "replace")
            off += 3 + ln
        elif tag in (3, 4):
            cp[i] = ("int", struct.unpack_from(">i", data, off + 1)[0]) if tag == 3 else ("float", struct.unpack_from(">f", data, off + 1)[0])
            off += 5
        elif tag in (5, 6):
            cp[i] = ("long", struct.unpack_from(">q", data, off + 1)[0]) if tag == 5 else ("double", struct.unpack_from(">d", data, off + 1)[0])
            off += 9
            i += 1
        elif tag in (7, 8, 16, 19, 20):
            cp[i] = (tag, struct.unpack_from(">H", data, off + 1)[0])
            off += 3
        elif tag == 15:
            off += 4
        elif tag in (9, 10, 11, 12, 17, 18):
            cp[i] = (tag,) + struct.unpack_from(">HH", data, off + 1)
            off += 5
        else:
            raise ValueError(f"bad constant tag {tag}")
        i += 1

    def u2():
        nonlocal off
        v = struct.unpack_from(">H", data, off)[0]
        off += 2
        return v

    def u4():
        nonlocal off
        v = struct.unpack_from(">I", data, off)[0]
        off += 4
        return v

    def cls(idx):
        return cp[cp[idx][1]]

    def element():
        nonlocal off
        tag = chr(data[off])
        off += 1
        if tag in "BCDFIJSZs":
            v = cp[u2()]
            return v if tag in "sIJFDBCSZ" else None
        if tag == "e":
            u2()
            return cp[u2()]
        if tag == "c":
            return cp[u2()]
        if tag == "@":
            return annotation()
        if tag == "[":
            return [element() for _ in range(u2())]
        raise ValueError(tag)

    def annotation():
        typ = cp[u2()]
        vals = {}
        for _ in range(u2()):
            name = cp[u2()]
            vals[name] = element()
        return (typ, vals)

    def attributes(member_code: bool):
        nonlocal off
        anns, insns = {}, None
        for _ in range(u2()):
            name = cp[u2()]
            ln = u4()
            end = off + ln
            if name in ("RuntimeVisibleAnnotations", "RuntimeInvisibleAnnotations"):
                for _ in range(u2()):
                    t, v = annotation()
                    anns[t] = v
            elif name == "Code" and member_code:
                off += 4
                clen = u4()
                code = data[off:off + clen]
                insns = set()
                pc = 0
                while pc < clen:
                    op = code[pc]
                    const = _constant(code, pc, op, cp)
                    if const is not None:
                        insns.add(("CONST",) + const + ("",))
                    if 0xb2 <= op <= 0xb9 or op == 0xbb:
                        ref = cp[struct.unpack_from(">H", code, pc + 1)[0]]
                        if op == 0xbb:
                            insns.add(("NEW", cp[ref[1]], "", ""))
                        else:
                            nat = cp[ref[2]]
                            kind = "FIELD" if op <= 0xb5 else "INVOKE"
                            insns.add((kind, cls(ref[1]), cp[nat[1]], cp[nat[2]]))
                            if op == 0xb8:
                                insns.add(("STATIC", cls(ref[1]), cp[nat[1]], cp[nat[2]]))
                    pc += _oplen(code, pc)
            off = end
        return anns, insns

    ci = ClassInfo()
    ci.access = u2()
    ci.name = cls(u2())
    sup = u2()
    ci.super = cls(sup) if sup else None
    ci.interfaces = [cls(u2()) for _ in range(u2())]
    ci.fields, ci.methods = [], []
    for bucket, code in ((ci.fields, False), (ci.methods, with_code)):
        for _ in range(u2()):
            acc, name, desc = u2(), cp[u2()], cp[u2()]
            ann, insns = attributes(code)
            bucket.append(Member(acc, name, desc, ann, insns))
    ci.ann, _ = attributes(False)
    return ci


# ---------------------------------------------------------------- class path
class ClassPath:
    def __init__(self, entries: list[str]):
        self.index: dict[str, tuple] = {}
        self.cache: dict[str, ClassInfo | None] = {}
        for e in entries:
            p = Path(e)
            if p.is_dir():
                for f in p.rglob("*.class"):
                    self.index.setdefault(f.relative_to(p).as_posix()[:-6], ("dir", f))
            elif p.suffix == ".jar" and p.is_file():
                try:
                    z = zipfile.ZipFile(p)
                except zipfile.BadZipFile:
                    continue
                for n in z.namelist():
                    if n.endswith(".class") and not n.startswith("META-INF/"):
                        self.index.setdefault(n[:-6], ("jar", z, n))

    def get(self, name: str) -> ClassInfo | None:
        if name not in self.cache:
            src = self.index.get(name)
            if src is None:
                self.cache[name] = None
            else:
                data = src[1].read_bytes() if src[0] == "dir" else src[1].read(src[2])
                self.cache[name] = parse_class(data, True)
        return self.cache[name]

    def known(self, name: str) -> bool:
        return name in self.index


# ---------------------------------------------------------------- descriptor helpers
def split_args(desc: str) -> tuple[list[str], str]:
    args, i = [], 1
    while desc[i] != ")":
        j = i
        while desc[j] == "[":
            j += 1
        j = desc.index(";", j) + 1 if desc[j] == "L" else j + 1
        args.append(desc[i:j])
        i = j
    return args, desc[i + 1:]


def parse_ref(ref: str) -> tuple[str | None, str, str]:
    """'Lowner;name(desc)' / 'name(desc)' / 'Lowner;f:Ldesc;' / 'name' -> (owner, name, desc)."""
    ref = ref.replace(" ", "")
    owner = None
    if ref.startswith("L") and ";" in ref:
        owner, ref = ref[1:ref.index(";")], ref[ref.index(";") + 1:]
        owner = owner.replace(".", "/")
    elif "." in ref.split("(")[0]:
        head = ref.split("(")[0]
        owner = head[:head.rindex(".")].replace(".", "/")
        ref = ref[len(owner) + 1:]
    if ":" in ref:
        name, desc = ref.split(":", 1)
    elif "(" in ref:
        name, desc = ref[:ref.index("(")], ref[ref.index("("):]
    else:
        name, desc = ref, ""
    return owner, name, desc


def select(target: ClassInfo, selector: str) -> list[Member]:
    _, name, desc = parse_ref(selector)
    if name.startswith("/"):
        return target.methods  # regex selector: not checked
    wildcard = name.endswith("*")
    name = name.rstrip("*")
    out = []
    for m in target.methods:
        if (not name and wildcard) or m.name == name:
            if not desc or m.desc == desc:
                out.append(m)
    return out


# ---------------------------------------------------------------- the check
def mixin_targets(ci: ClassInfo) -> list[str]:
    ann = ci.ann.get(MIXIN + "Mixin;", {})
    out = [v[1:-1] for v in ann.get("value", [])]
    out += [t.replace(".", "/") for t in ann.get("targets", [])]
    return out


def strip_prefix(name: str, ann: dict) -> str:
    prefix = ann.get("prefix", "shadow$")
    return name[len(prefix):] if name.startswith(prefix) else name


def accessor_name(m: Member, ann: dict, invoker: bool) -> str:
    if ann.get("value"):
        return ann["value"]
    for p in (("call", "invoke", "new", "create") if invoker else ("get", "set", "is")):
        if m.name.startswith(p) and len(m.name) > len(p):
            rest = m.name[len(p):]
            if invoker and p in ("new", "create"):
                return "<init>"
            return rest if invoker else rest[0].lower() + rest[1:]
    return m.name


def ats(ann: dict) -> list[dict]:
    a = ann.get("at")
    if a is None:
        return []
    items = a if isinstance(a, list) else [a]
    return [v for _, v in items]


def check_mixin(cp: ClassPath, mixin: ClassInfo, problems: list[str]) -> None:
    pseudo = (MIXIN + "Pseudo;") in mixin.ann
    tag = mixin.name.split("/")[-1]
    for tname in mixin_targets(mixin):
        target = cp.get(tname)
        if target is None:
            if not pseudo:
                problems.append(f"{tag}: target class {tname} not found")
            continue
        fields = {(f.name, f.desc) for f in target.fields}
        methods = {(m.name, m.desc) for m in target.methods}
        for f in mixin.fields:
            if SHADOW in f.ann:
                n = strip_prefix(f.name, f.ann[SHADOW])
                names = [n] + list(f.ann[SHADOW].get("aliases", []))
                if not any((x, f.desc) in fields for x in names):
                    problems.append(f"{tag}: @Shadow field {n}:{f.desc} not in {tname}")
        for m in mixin.methods:
            if SHADOW in m.ann or OVERWRITE in m.ann:
                a = m.ann[SHADOW] if SHADOW in m.ann else m.ann[OVERWRITE]
                n = strip_prefix(m.name, a)
                names = [n] + list(a.get("aliases", []))
                if not any((x, m.desc) in methods for x in names):
                    kind = "@Shadow" if SHADOW in m.ann else "@Overwrite"
                    problems.append(f"{tag}: {kind} method {n}{m.desc} not in {tname}")
            if ACCESSOR in m.ann:
                n = accessor_name(m, m.ann[ACCESSOR], False)
                args, ret = split_args(m.desc)
                ftype = ret if ret != "V" else (args[-1] if args else "")
                if (n, ftype) not in fields:
                    problems.append(f"{tag}: @Accessor {n}:{ftype} not in {tname}")
            if INVOKER in m.ann:
                n = accessor_name(m, m.ann[INVOKER], True)
                want = m.desc if n != "<init>" else m.desc[:m.desc.index(")") + 1] + "V"
                if (n, want) not in methods:
                    problems.append(f"{tag}: @Invoker {n}{want} not in {tname}")
            for atype in INJECTORS & m.ann.keys():
                check_injector(cp, tag, tname, target, m, atype, m.ann[atype], problems)


def at_matches(selected, kind, ref):
    """The instructions of the selected methods a @At INVOKE/FIELD/NEW selector names ({} for an other kind)."""
    owner, name, desc = parse_ref(ref)
    if kind == "NEW":
        owner = owner or name.replace(".", "/")
        if owner.startswith("L") and owner.endswith(";"):
            owner = owner[1:-1]
        return [("NEW", owner, "", "")] if any(("NEW", owner, "", "") in (m.insns or ()) for m in selected) else []
    want = "FIELD" if kind == "FIELD" else "INVOKE"
    return [i for m in selected for i in (m.insns or ())
            if i[0] == want and (owner is None or i[1] == owner) and i[2] == name and (not desc or i[3] == desc)]


def check_at(cp, tag, tname, handler, short, selected, at, problems):
    kind, ref = at.get("value", ""), at.get("target", "")
    if kind == "CONSTANT":
        check_constant_args(tag, tname, handler, short, selected, at.get("args", []), problems)
        return
    if not ref or kind not in ("INVOKE", "INVOKE_ASSIGN", "FIELD", "NEW"):
        return
    if not at_matches(selected, kind, ref):
        owner = parse_ref(ref)[0]
        where = "known class" if owner and cp.known(owner) else "unknown class" if owner else "no owner"
        problems.append(f"{tag}.{handler.name}: {short} @At({kind}) {ref} not found in {tname}."
                        f"{'/'.join(sorted({m.name for m in selected}))} ({where})")


def _has_constant(selected, kind, value):
    if kind in ("int", "long") and value == 0:
        return True  # implicit zero of a comparison (ifeq ...), Mixin finds it without an instruction
    for m in selected:
        for i in m.insns or ():
            if i[0] == "CONST" and i[1] == kind and (i[2] == value or kind == "float" and abs(i[2] - value) < 1e-6 * max(1.0, abs(value))):
                return True
    return False


def check_constant_args(tag, tname, handler, short, selected, args, problems):
    """@At(CONSTANT, args = "intValue=12" ...)"""
    if isinstance(args, str):
        args = [args]
    for arg in args:
        if "=" not in arg:
            continue
        key, val = arg.split("=", 1)
        kind = {"intValue": "int", "longValue": "long", "floatValue": "float", "doubleValue": "double", "stringValue": "string"}.get(key)
        try:
            value = {"int": int, "long": int, "float": float, "double": float, "string": str, None: None}[kind](val) if kind else None
        except (TypeError, ValueError):
            continue
        if kind and not _has_constant(selected, kind, value):
            problems.append(f"{tag}.{handler.name}: {short} CONSTANT {key}={val} not found in {tname}."
                            f"{'/'.join(sorted({m.name for m in selected}))}")


def check_constants(tag, tname, handler, short, selected, ann, problems):
    """@ModifyConstant(constant = @Constant(intValue = 12))"""
    c = ann.get("constant")
    if c is None:
        return
    for _, cv in (c if isinstance(c, list) else [c]):
        for key, kind in (("intValue", "int"), ("longValue", "long"), ("floatValue", "float"), ("doubleValue", "double"), ("stringValue", "string")):
            if key in cv and cv[key] is not None:
                v = cv[key][1] if isinstance(cv[key], tuple) else cv[key]
                if not _has_constant(selected, kind, v):
                    problems.append(f"{tag}.{handler.name}: {short} {key}={v} not found in {tname}."
                                    f"{'/'.join(sorted({m.name for m in selected}))}")


def loose(a: str, b: str) -> bool:
    """Descriptor types equal; object types match any object type (subclass, interface, erased generic)."""
    if a == b:
        return True
    return a[0] in "L[" and b[0] in "L[" and not (a[0] == "[" and b[0] == "[" and a.lstrip("[")[0] != "L" and b.lstrip("[")[0] != "L")


def check_signature(tag, handler, atype, short, selected, ann, problems):
    """Handler signature of @Redirect / @WrapOperation (INVOKE) and @ModifyArg (index >= 0) against the invoked method."""
    kinds = {INJ + "Redirect;": 0, MX + "wrapoperation/WrapOperation;": 1, INJ + "ModifyArg;": 2}
    if atype not in kinds:
        return
    kind = kinds[atype]
    at_list = ats(ann)
    if len(at_list) != 1 or at_list[0].get("value") not in ("INVOKE", "INVOKE_ASSIGN") or not at_list[0].get("target"):
        return
    owner, name, desc = parse_ref(at_list[0]["target"])
    hits = at_matches(selected, "INVOKE", at_list[0]["target"])
    if not hits or not desc:
        return  # not found is reported by check_at
    hargs, hret = split_args(handler.desc)
    if kind == 2:
        index = ann.get("index")
        index = index[1] if isinstance(index, tuple) else -1
    reasons = []
    for hit in hits:
        static = any(i[0] == "STATIC" and i[1:] == hit[1:] for m in selected for i in (m.insns or ()))
        iargs, iret = split_args(hit[3])
        recv = [] if static else ["L" + hit[1] + ";"]
        if kind == 2:
            if index < 0 or index >= len(iargs):
                return
            want_args, want_ret = [iargs[index]], iargs[index]
            ok = len(hargs) >= 1 and loose(hargs[0], want_args[0]) and loose(hret, want_ret)
            # Mixin also accepts the invoked method's whole argument list as handler parameters
            ok = ok or (len(hargs) >= len(iargs) and all(loose(h, w) for h, w in zip(hargs, iargs)) and loose(hret, want_ret))
        else:
            want_args, want_ret = recv + iargs, iret
            extra = 1 if kind == 1 else 0
            ok = (len(hargs) >= len(want_args) + extra and all(loose(h, w) for h, w in zip(hargs, want_args))
                  and (not extra or "wrapoperation/Operation" in hargs[len(want_args)]) and loose(hret, want_ret))
        if ok:
            return
        reasons.append(f"{''.join(want_args)}{'+Operation' if kind == 1 else ''} -> {want_ret}")
    problems.append(f"{tag}.{handler.name}: {short} handler {handler.desc} does not fit {name}{desc}; expected ({reasons[0]})")


def check_injector(cp, tag, tname, target, handler, atype, ann, problems):
    short = "@" + atype.rsplit("/", 1)[-1].rstrip(";")
    selectors = ann.get("method", [])
    if isinstance(selectors, str):
        selectors = [selectors]
    if not selectors:
        return  # @Desc-based selection: not checked
    # Several selectors are version alternatives: only all of them missing fails (require counts injections).
    selected = [m for s in selectors for m in select(target, s)]
    if not selected:
        problems.append(f"{tag}.{handler.name}: {short} method {', '.join(selectors)} not in {tname}")
        return
    slices = []
    for _, sl in (ann.get("slice") if isinstance(ann.get("slice"), list) else [ann["slice"]] if ann.get("slice") else []):
        slices += [v for key in ("from", "to") if sl.get(key) for _, v in [sl[key]]]
    for at in ats(ann) + slices:
        check_at(cp, tag, tname, handler, short, selected, at, problems)
    check_constants(tag, tname, handler, short, selected, ann, problems)
    check_signature(tag, handler, atype, short, selected, ann, problems)
    if atype == INJ + "Inject;":
        args, _ = split_args(handler.desc)
        ci_at = next((i for i, a in enumerate(args) if "callback/CallbackInfo" in a), None)
        if ci_at:
            prefix = args[:ci_at]
            if not any(split_args(m.desc)[0] == prefix for m in selected):
                problems.append(f"{tag}.{handler.name}: @Inject handler args {''.join(prefix)} do not match "
                                f"{tname}.{selected[0].name}{selected[0].desc}")


def mixin_configs(mod_roots: list[str]) -> list[tuple[str, dict]]:
    """(config name, json) for each config named in a fabric.mod.json of the mod outputs."""
    files: dict[str, bytes] = {}
    mods: list[bytes] = []
    for r in mod_roots:
        p = Path(r)
        if p.is_dir():
            for f in p.glob("*.json"):
                files.setdefault(f.name, f.read_bytes())
            if (p / "fabric.mod.json").is_file():
                mods.append((p / "fabric.mod.json").read_bytes())
        elif p.is_file():
            with zipfile.ZipFile(p) as z:
                for n in z.namelist():
                    if "/" not in n and n.endswith(".json"):
                        files.setdefault(n, z.read(n))
                mods.append(z.read("fabric.mod.json"))
    names: list[str] = []
    for raw in mods:
        for e in json.loads(raw).get("mixins", []):
            n = e if isinstance(e, str) else e.get("config")
            if n and n not in names:
                names.append(n)
    out = []
    for n in names:
        if n not in files:
            out.append((n, None))
        else:
            out.append((n, json.loads(files[n])))
    return out


def auto_paths() -> tuple[list[str], list[str]]:
    mods = []
    for proj in ["mc26_3/fabric"] + sorted(glob.glob("modules/*/fabric", root_dir=ROOT)):
        for sub in ("classes/java/main", "resources/main"):
            d = ROOT / proj / "build" / sub
            if d.is_dir():
                mods.append(str(d))
    home = Path.home() / ".gradle/caches"
    cp = glob.glob(str(home / "fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.3/*.jar"))
    cp += glob.glob(str(home / "modules-2/files-2.1/net.fabricmc.fabric-api/*/*+26.3/*/*.jar"))
    cp += glob.glob(str(home / "modules-2/files-2.1/net.fabricmc/fabric-loader/0.19*/*/*.jar"))
    return mods, cp


def main() -> int:
    ap = argparse.ArgumentParser(fromfile_prefix_chars="@", description=__doc__.split("\n")[0])
    ap.add_argument("--mods", nargs="*", default=[], help="mod class/resource dirs or jars")
    ap.add_argument("--cp", nargs="*", default=[], help="game jar and libraries")
    a = ap.parse_args()
    mods, cp_entries = (a.mods, a.cp) if a.mods else auto_paths()
    if not any("minecraft" in Path(c).name for c in cp_entries) or not mods:
        print("check_client_mixins: no 26.3 game jar or no mod outputs found (compile the Fabric 26.3 projects first)")
        return 1
    cp = ClassPath(mods + cp_entries)
    problems: list[str] = []
    checked = 0
    for cfg_name, cfg in mixin_configs(mods):
        if cfg is None:
            problems.append(f"{cfg_name}: config named in fabric.mod.json but missing")
            continue
        pkg = cfg.get("package", "").replace(".", "/")
        for section in ("mixins", "client", "server"):
            for entry in cfg.get(section, []):
                name = f"{pkg}/{entry.replace('.', '/')}"
                ci = cp.get(name)
                if ci is None:
                    problems.append(f"{cfg_name} [{section}]: mixin class {name} not compiled")
                    continue
                before = len(problems)
                check_mixin(cp, ci, problems)
                for i in range(before, len(problems)):
                    problems[i] = f"{cfg_name} [{section}] {problems[i]}"
                checked += 1
    for p in problems:
        print("  " + p)
    print(f"check_client_mixins: {checked} mixins checked, {len(problems)} problem(s)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
