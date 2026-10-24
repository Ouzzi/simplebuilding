"""Report drift between duplicated implementations: core class (common/src/shared/java/com/simplebuilding) against
its copy in a module (REV24-F-17, docs/ai/PLAN-B-REVSTATE24-2026-10-24.md). Per pair the public/protected method
signatures (name/parameter count) are compared; a method on one side only is drift.

tools/dup_pairs.txt lists every known pair: `<core path> <module path> [allow: name/n name/n ...]`. `allow` holds
the drift that exists on purpose or is known (baseline). Fails on: drift not allowed, a same-named core/module class
pair not listed (new duplicate), a listed file that is gone. Stale allows only warn.
Update the baseline after a deliberate change: python tools/check_dup_impl.py --baseline  (rewrites the list).
Self test: python tools/check_dup_impl.py --self-test"""
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LIST = ROOT / "tools" / "dup_pairs.txt"
CORE = "common/src/shared/java/com/simplebuilding/"
# name/param count of public or protected methods and constructors-free declarations
DECL = re.compile(r"\b(?:public|protected)\s+(?:(?:static|final|abstract|synchronized|default)\s+)*"
                  r"(?:<[^>]+>\s+)?[\w.<>\[\], ?]+?\s+(\w+)\s*\(")


def strip(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"//[^\n]*", "", src)


def arity(src, pos):
    """Parameter count of the list whose '(' is at pos (generics commas ignored)."""
    depth = angle = 0
    count = 0
    seen = False
    for ch in src[pos:]:
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                break
        elif depth == 1:
            if ch == "<":
                angle += 1
            elif ch == ">":
                angle -= 1
            elif ch == "," and angle == 0:
                count += 1
            elif not ch.isspace():
                seen = True
    return count + 1 if seen else 0


def signatures(src):
    src = strip(src)
    return {f"{m.group(1)}/{arity(src, m.end() - 1)}" for m in DECL.finditer(src)}


def drift(a, b):
    return signatures(a) ^ signatures(b)


def tracked():
    return subprocess.check_output(["git", "ls-files", "*.java"], cwd=ROOT, text=True).splitlines()


def discover(files):
    core = {}
    for f in files:
        if f.startswith(CORE):
            core.setdefault(Path(f).name, []).append(f)
    pairs = []
    for f in files:
        if f.startswith("modules/") and "/test" not in f and Path(f).name in core:
            pairs.extend((c, f) for c in core[Path(f).name])
    return sorted(pairs)


def read_list():
    out = {}
    for line in LIST.read_text(encoding="utf-8").splitlines():
        line = line.split("#")[0].strip()
        if line:
            head, _, allow = line.partition("allow:")
            core, mod = head.split()
            out[(core, mod)] = set(allow.split())
    return out


def main(argv):
    pairs = discover(tracked())
    if "--baseline" in argv:
        lines = ["# core path, module path, allow: drift accepted so far (tools/check_dup_impl.py --baseline rewrites this)"]
        for c, m in pairs:
            d = sorted(drift((ROOT / c).read_text(encoding="utf-8"), (ROOT / m).read_text(encoding="utf-8")))
            lines.append(f"{c} {m}" + (f" allow: {' '.join(d)}" if d else ""))
        LIST.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"wrote {len(pairs)} pairs to {LIST.relative_to(ROOT)}")
        return 0
    listed = read_list()
    bad = 0
    for pair in pairs:
        if pair not in listed:
            print(f"ERROR new duplicate pair, add it (--baseline): {pair[0]} <> {pair[1]}")
            bad += 1
            continue
        d = drift((ROOT / pair[0]).read_text(encoding="utf-8"), (ROOT / pair[1]).read_text(encoding="utf-8"))
        for sig in sorted(d - listed[pair]):
            print(f"ERROR drift {sig} only on one side: {pair[0]} <> {pair[1]}")
            bad += 1
        for sig in sorted(listed[pair] - d):
            print(f"WARN allowed drift {sig} is gone (remove from tools/dup_pairs.txt): {pair[1]}")
    for pair in sorted(set(listed) - set(pairs)):
        print(f"ERROR listed pair gone: {pair[0]} <> {pair[1]}")
        bad += 1
    print(f"check_dup_impl: {len(pairs)} pairs, {bad} problems")
    return 1 if bad else 0


def self_test():
    a = "public class A { public void f(int x, Map<String, Integer> m) {} protected static <T> T g() { return null; } private void h() {} }"
    assert signatures(a) == {"f/2", "g/0"}, signatures(a)
    b = a.replace("g()", "g(int z)")
    assert drift(a, b) == {"g/0", "g/1"}
    assert drift(a, a) == set()
    assert signatures("// public void c() {}\n/* protected void d() {} */") == set()
    print("self-test ok")
    return 0


if __name__ == "__main__":
    sys.exit(self_test() if "--self-test" in sys.argv else main(sys.argv[1:]))
