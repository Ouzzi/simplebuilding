#!/usr/bin/env python3
"""Dump a Vanilla entity model's cube tree (name, pose, cubes, children) from the client jar's bytecode.

Only used for the owner previews (no Mojang pixels or code are copied into the repo): a tiny JVM-subset interpreter runs
the static createBodyLayer-style methods with javap -c output of the extracted classes.
Usage: dump_model.py <classdir> <class> <method> [json out]   e.g. net/minecraft/client/model/animal/equine/HorseModel createBodyLayer
"""
import json, math, re, subprocess, sys, struct
from pathlib import Path

CLS = Path(sys.argv[1]) if len(sys.argv) > 1 else None
_cache = {}
STATICS = {}
INIT = set()

def javap(cls):
    if cls not in _cache:
        out = subprocess.run(["javap", "-c", "-p", str(CLS / (cls + ".class"))], capture_output=True, text=True).stdout
        methods, cur = {}, None
        for line in out.splitlines():
            if line.strip() == "static {};":
                cur = ("<clinit>", ""); methods.setdefault(cur[0], []).append(["", []]); continue
            m = re.match(r"^  (?!\s)[^()]*?(<init>|<clinit>|\w+)\((.*)\)(?: throws .*)?;$", line)
            if m:
                cur = (m.group(1), m.group(2)); methods.setdefault(cur[0], []).append([cur[1], []]); continue
            m = re.match(r"\s+(\d+): (\w+)\s*(.*)$", line)
            if m and cur:
                methods[cur[0]][-1][1].append((int(m.group(1)), m.group(2), m.group(3)))
        _cache[cls] = methods
    return _cache[cls]

class Part:
    def __init__(s, name): s.name = name; s.cubes = []; s.pose = [0]*6; s.kids = {}
    def js(s): return {"name": s.name, "pose": s.pose, "cubes": s.cubes, "kids": [k.js() for k in s.kids.values()]}
class CLB:
    def __init__(s): s.cubes = []; s.uv = [0, 0]; s.mirror = False
class Def:
    def __init__(s, g=0.0): s.g = g
class Mesh:
    def __init__(s): s.root = Part("root"); s.size = None

def desc_args(d):
    d = d.strip("()") if d else ""
    out = []
    for m in re.finditer(r"\[*(?:L[^;]+;|[BCDFIJSZ])|\[*[BCDFIJSZ]", d):
        out.append(m.group(0))
    return out

def sig(comment, cur=""):
    m = re.search(r"// (?:Interface)?Method (?:([\w/$]+)\.)?\"?([\w<>$]+)\"?:\((.*?)\)(\S+)", comment)
    if not m: raise ValueError(comment)
    g = m.groups()
    return (g[0] or cur, g[1], g[2], g[3])

def run(cls, name, args):
    code = None
    for params, c in javap(cls).get(name, []):
        code = c; break
    if code is None: raise KeyError((cls, name))
    idx = {pc: i for i, (pc, _, _) in enumerate(code)}
    loc = {i: a for i, a in enumerate(args)}
    st = []; i = 0
    while i < len(code):
        pc, op, rest = code[i]; i += 1
        if op in ("new",): st.append("NEW:" + rest.split("class ")[-1].strip())
        elif op == "dup": st.append(st[-1])
        elif op == "pop": st.pop()
        elif op == "checkcast" or op == "nop": pass
        elif op in ("aload", "iload", "fload", "dload"): st.append(loc[int(rest)])
        elif re.match(r"[aifd]load_\d", op): st.append(loc[int(op[-1])])
        elif op in ("astore", "istore", "fstore", "dstore"): loc[int(rest)] = st.pop()
        elif re.match(r"[aifd]store_\d", op): loc[int(op[-1])] = st.pop()
        elif op in ("ldc", "ldc_w", "ldc2_w"):
            m = re.search(r"// (float|int|double|String|long) (.*)$", rest)
            t, v = m.groups()
            st.append(v if t == "String" else (float(v.rstrip("fFdDlL").replace("Infinity", "inf")) if t != "int" else int(v)))
        elif op.startswith("fconst_") or op.startswith("dconst_"): st.append(float(op[-1]))
        elif op.startswith("iconst_"): st.append(-1 if op.endswith("m1") else int(op[-1]))
        elif op in ("bipush", "sipush"): st.append(int(rest))
        elif op == "putstatic":
            f = re.search(r"Field (?:([\w/$]+)\.)?(\w+):", rest)
            STATICS[(f.group(1) or cls, f.group(2))] = st.pop()
        elif op == "getstatic":
            f = re.search(r"Field (?:([\w/$]+)\.)?(\w+):", rest)
            fc = f.group(1) or cls
            if fc.startswith("net/minecraft/client/model/") and (fc, f.group(2)) not in STATICS and fc not in INIT:
                INIT.add(fc)
                try: run(fc, "<clinit>", [])
                except KeyError: pass
            if (fc, f.group(2)) in STATICS: st.append(STATICS[(fc, f.group(2))])
            elif "PartPose.ZERO" in rest: st.append([0]*6)
            elif "CubeDeformation.NONE" in rest: st.append(Def(0.0))
            else: st.append(("static", rest))
        elif op in ("newarray", "anewarray"): n = st.pop(); st.append([0.0] * n)
        elif op in ("faload", "aaload", "iaload", "daload"): k = st.pop(); arr = st.pop(); st.append(arr[k])
        elif op in ("fastore", "aastore", "iastore", "dastore"): v = st.pop(); k = st.pop(); arr = st.pop(); arr[k] = v
        elif op == "arraylength": st.append(len(st.pop()))
        elif op == "iinc": a, b = rest.split(","); loc[int(a)] += int(b)
        elif op in ("i2f", "i2d", "f2d", "d2f"): st.append(float(st.pop()) if op in ("i2f", "i2d", "f2d") else st.pop())
        elif op in ("fadd", "dadd", "iadd"): b = st.pop(); a = st.pop(); st.append(a + b)
        elif op in ("fsub", "dsub", "isub"): b = st.pop(); a = st.pop(); st.append(a - b)
        elif op in ("fmul", "dmul", "imul"): b = st.pop(); a = st.pop(); st.append(a * b)
        elif op in ("fdiv", "ddiv"): b = st.pop(); a = st.pop(); st.append(a / b)
        elif op == "irem": b = st.pop(); a = st.pop(); st.append(a % b)
        elif op == "idiv": b = st.pop(); a = st.pop(); st.append(int(a / b))
        elif op in ("fneg", "dneg", "ineg"): st.append(-st.pop())
        elif op == "goto": i = idx[int(rest)]
        elif op.startswith("if_icmp") or op.startswith("if"):
            if op.startswith("if_icmp"):
                b = st.pop(); a = st.pop(); c = op[7:]
            else:
                a = st.pop(); b = 0; c = op[2:]
            t = {"lt": a < b, "ge": a >= b, "gt": a > b, "le": a <= b, "eq": a == b, "ne": a != b}[c]
            if op in ("ifnull", "ifnonnull"): t = (a is None) == (op == "ifnull")
            if t: i = idx[int(rest)]
        elif op in ("areturn", "ireturn", "freturn", "dreturn"): return st.pop()
        elif op == "return": return None
        elif op.startswith("invoke"):
            if "InvokeDynamic" in rest:
                n = len(desc_args(re.search(r":\((.*?)\)", rest).group(1))); a = [st.pop() for _ in range(n)][::-1]
                st.append("".join(str(x) for x in a)); continue
            c, m, d, ret = sig(rest, cls)
            ps = desc_args(d); a = [st.pop() for _ in ps][::-1]
            recv = None if op == "invokestatic" else st.pop()
            r = call(op, c, m, recv, a, ps, ret)
            if ret != "V":
                st.append(r)
            elif m == "<init>" and isinstance(recv, str) and recv.startswith("NEW:"):
                pass
            if m == "<init>" and op == "invokespecial":
                # replace the NEW placeholders (dup'd copies) by the constructed object
                obj = call(op, c, m, recv, a, ps, ret)
                for k in range(len(st)):
                    if st[k] is recv: st[k] = obj
                if len(st) and st[-1] is r and ret != "V": pass
        else:
            raise NotImplementedError((op, rest))
    return None

def call(op, c, m, recv, a, ps, ret):
    if c.endswith("PartNames") and m == "tentacle": return f"tentacle{a[0]}"
    if c.endswith("RandomSource"): return "rng" if ret.startswith("L") else (0 if ret == "I" else 0.5)
    if m == "<init>":
        if c.endswith("MeshDefinition"): return Mesh()
        if c.endswith("CubeDeformation"): return Def(a[0] if a else 0.0)
        return recv
    if c.endswith("MeshDefinition") and m == "getRoot": return recv.root
    if c.endswith("CubeListBuilder"):
        if m == "create": return CLB()
        if m == "texOffs": recv.uv = [a[0], a[1]]; return recv
        if m == "mirror": return recv
        if m == "addBox":
            nums = [x for x in a if isinstance(x, (int, float)) and not isinstance(x, bool)]
            g = 0.0
            for x in a:
                if isinstance(x, Def): g = x.g
            f = [float(x) for x in nums[:6]]
            if len(nums) == 7 and not any(isinstance(x, Def) for x in a): g = float(nums[6])
            if len(nums) > 6 and isinstance(a[-1], (int, float)) and ps[-1] == "F": g = float(a[-1])
            recv.cubes.append({"box": f, "uv": list(recv.uv), "grow": g}); return recv
    if c.endswith("PartPose"):
        if m == "offset": return [a[0], a[1], a[2], 0, 0, 0]
        if m == "offsetAndRotation": return list(a)
        if m == "rotation": return [0, 0, 0, a[0], a[1], a[2]]
    if c.endswith("PartDefinition"):
        if m == "addOrReplaceChild":
            p = Part(a[0]); p.cubes = list(a[1].cubes); p.pose = list(a[2]); recv.kids[a[0]] = p; return p
        if m == "getChild": return recv.kids[a[0]]
    if c.endswith("CubeDeformation") and m == "extend": return Def(recv.g + a[0])
    if c.endswith("LayerDefinition") and m == "create": a[0].size = [a[1], a[2]]; return a[0]
    if c.endswith("util/Mth") and m in ("cos", "sin"): return getattr(math, m)(*a)
    if c.startswith("java/lang/Math") or c == "java/lang/Math":
        if m == "signum": return (a[0] > 0) - (a[0] < 0) + 0.0
        if m == "abs": return abs(a[0])
        return getattr(math, {"toRadians": "radians"}.get(m, m))(*a)
    if c.endswith("LayerDefinition") or c.endswith("MeshDefinition"):
        if m == "apply": return recv.root and recv if isinstance(recv, Mesh) else recv
    if op == "invokestatic" and m != "<init>":
        try:
            return run(c, m, a)
        except KeyError:
            pass
    return None

if __name__ == "__main__":
    params = desc_args(javap(sys.argv[2])[sys.argv[3]][0][0].replace(",", ""))
    ps = [x.strip() for x in javap(sys.argv[2])[sys.argv[3]][0][0].split(",") if x.strip()]
    args = [Def(0.0) if "CubeDeformation" in x else (0.0 if x == "float" else None) for x in ps]
    mesh = run(sys.argv[2], sys.argv[3], args)
    root = mesh.root if isinstance(mesh, Mesh) else mesh
    out = root.js(); out["size"] = getattr(mesh, "size", None)
    json.dump(out, open(sys.argv[4], "w") if len(sys.argv) > 4 else sys.stdout, indent=1)
