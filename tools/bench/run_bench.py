#!/usr/bin/env python3
"""Simple Optimization benchmark harness (docs/ai/KONZEPT-SIMPLE-OPTIMIZATION-2026-10-17.md §6, plan PLAN-OPT1).

Per area four scenarios, each run in a fresh JVM (Fabric client gametest BenchClientTest, own run folder
integration/run-bench-263 with its own mods/ and config/):
  a  vanilla                  (Fabric API only)
  b  <other mod> alone        (pinned 26.3 jar from tools/bench/modsets.json, see fetch_mods.py)
  c  ours, only this area on  (our sub-mod jar, every other area of it "off")
  d  ours + other mod         (mode auto: shows our part stepping back; logged decision kept)
Fixed seed, scene and camera path, warmup, N runs (interleaved a,b,c per round). Metrics: frame time avg/1%-low/p99,
MSPT avg/p95, heap after GC, startup, world/chunk load, mesh rebuild; S5a also the view distance trace.
Output: bench-results/<date>-<profile>/results.json + report.html.

sb-test has no GPU (Mesa llvmpipe): frame-time numbers are "nur Tendenz" (GPU-bound); MSPT, heap, startup, chunk and
mesh times are valid there (concept §6.5, owner answer §9).

  LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a -s '-screen 0 1920x1080x24 +extension GLX +render -noreset' \\
      python3.12 tools/bench/run_bench.py --profile sbtest --runs 3 --areas o1,s5a
  python3 tools/bench/run_bench.py --report bench-results/<dir>/results.json      rebuild report.html
  python3 tools/bench/run_bench.py --apply-priorities A/results.json B/results.json  update priorities.json
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import platform
import re
import shutil
import statistics
import subprocess
import sys
import time
from html import escape
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]
sys.path.insert(0, str(HERE))
import fetch_mods  # noqa: E402

RUN_DIR = "run-bench-263"
OWNER_AREAS = {
    "simpleoptrender": ["leaf_culling", "lightmap_cache", "fast_ip_ping", "no_telemetry", "more_culling", "details"],
    "simpleoptmemory": ["state_tables"],
    "simpleoptserver": ["dynamic_distance", "activation_range"],
}
LIBS = {"moreculling": ["cloth-config"]}
AREAS = {
    "o1": dict(code="O1", area="leaf_culling", owner="simpleoptrender", scene="o1", rivals=["cullleaves", "moreculling"],
               gpu=True, props={"sb.bench.warmup": 60, "sb.bench.frames": 300}, extra={},
               decisive=["quads_cutout", "meshbuild_s", "mspt_avg", "heap_mb"]),
    "s5a": dict(code="S5a", area="dynamic_distance", owner="simpleoptserver", scene="s5a", rivals=["servercore"],
                gpu=False, props={"sb.bench.warmupSeconds": 15, "sb.bench.seconds": 75, "sb.bench.flySpeed": 1.5},
                extra={"stepSeconds": 5, "targetMspt": 3}, decisive=["mspt_avg", "mspt_p95", "heap_mb"]),
    # O4 needs no own scene: heap after GC and start time of the S5a scene (server side, not GPU-bound).
    "o4": dict(code="O4", area="state_tables", owner="simpleoptmemory", scene="s5a", rivals=["ferritecore"],
               gpu=False, props={"sb.bench.warmupSeconds": 15, "sb.bench.seconds": 30, "sb.bench.flySpeed": 1.5},
               extra={}, decisive=["heap_mb", "startup_s", "mspt_avg"]),
}
# metric: (label, lower is better, GPU-bound under llvmpipe)
METRICS = {
    "fps_avg": ("FPS Mittel", False, True), "fps_1low": ("FPS 1%-Low", False, True),
    "frame_avg_ms": ("Frame-Zeit ms", True, True), "frame_p99_ms": ("Frame p99 ms", True, True),
    "quads_total": ("Quads im Blick", True, False), "quads_cutout": ("Quads Cutout (Blätter)", True, False),
    "meshbuild_s": ("Mesh-Neubau s", True, False), "chunkload_s": ("Chunk-Laden s", True, False),
    "worldload_s": ("Welt-Laden s", True, False), "mspt_avg": ("MSPT Mittel", True, False),
    "mspt_p95": ("MSPT p95", True, False), "heap_mb": ("Heap nach GC MB", True, False),
    "startup_s": ("Start s", True, False), "view_final": ("Sichtweite Ende", False, False),
}


def machine(profile: str) -> dict:
    cpu = ""
    try:
        cpu = next(l.split(":", 1)[1].strip() for l in Path("/proc/cpuinfo").read_text().splitlines() if l.startswith("model name"))
    except Exception:
        cpu = platform.processor()
    ram = ""
    try:
        kb = int(next(l.split()[1] for l in Path("/proc/meminfo").read_text().splitlines() if l.startswith("MemTotal")))
        ram = f"{kb / 1048576:.1f} GB"
    except Exception:
        pass
    java = ""
    try:
        java = subprocess.run(["java", "-version"], capture_output=True, text=True).stderr.splitlines()[0]
    except Exception:
        pass
    return dict(profile=profile, os=platform.platform(), cpu=cpu, cores=os.cpu_count(), ram=ram, java=java)


def gradlew() -> list[str]:
    return [str(REPO / ("gradlew.bat" if os.name == "nt" else "gradlew"))]


def our_jars(owners: set[str]) -> dict[str, Path]:
    subprocess.run(gradlew() + [f":modules:{o}:fabric:jar" for o in sorted(owners)] + ["-q"], cwd=REPO, check=True)
    out = {}
    for o in owners:
        libs = REPO / "modules" / o / "fabric" / "build" / "libs"
        jars = [j for j in libs.glob("*.jar") if not j.name.endswith(("-sources.jar", "-dev.jar"))]
        if len(jars) != 1:
            raise SystemExit(f"expected one jar in {libs}, found {[j.name for j in jars]}")
        out[o] = jars[0]
    return out


def scenarios(key: str, runs: int, both_runs: int) -> list[tuple[str, list[str], bool, int]]:
    """(name, foreign mod ids, ours, repetitions)"""
    a = AREAS[key]
    out = [("a-vanilla", [], False, runs)]
    out += [(f"b-{r}", [r], False, runs) for r in a["rivals"]]
    out.append(("c-ours", [], True, runs))
    out += [(f"d-ours+{r}", [r], True, both_runs) for r in a["rivals"]]
    return out


def write_config(cfg_dir: Path, owner: str, area: str, extra: dict) -> None:
    areas = {x: {"mode": "off"} for x in OWNER_AREAS[owner]}
    areas[area] = {"mode": "auto", **extra}
    cfg_dir.mkdir(parents=True, exist_ok=True)
    (cfg_dir / f"{owner}.json").write_text(json.dumps({"areas": areas, "conflictScan": True}, indent=2), encoding="utf-8")


def run_once(key: str, name: str, foreign: list[str], ours: bool, rep: int, jars: dict, foreign_jars: dict, out_dir: Path,
             timeout: int) -> dict:
    a = AREAS[key]
    run = REPO / "integration" / RUN_DIR
    for sub in ("mods", "config", "saves", "world"):
        if (run / sub).exists():
            shutil.rmtree(run / sub)
    (run / "mods").mkdir(parents=True)
    (run / "eula.txt").write_text("eula=true\n", encoding="utf-8")  # in-process dedicated server (S5a)
    mods = []
    for mid in foreign + [lib for f in foreign for lib in LIBS.get(f, [])]:
        shutil.copy2(foreign_jars[mid], run / "mods")
        pin = next(m for m in fetch_mods.load()["mods"] if m["modId"] == mid)
        mods.append(dict(id=mid, version=pin["version"], sha512=pin["sha512"][:16]))
    if ours:
        shutil.copy2(jars[a["owner"]], run / "mods")
        write_config(run / "config", a["owner"], a["area"], a["extra"])
        mods.append(dict(id=a["owner"], version="branch", sha512=""))
    result_file = out_dir / "runs" / f"{key}-{name}-{rep}.json"
    log_file = out_dir / "logs" / f"{key}-{name}-{rep}.log"
    result_file.parent.mkdir(parents=True, exist_ok=True)
    log_file.parent.mkdir(parents=True, exist_ok=True)
    if result_file.exists():
        result_file.unlink()
    props = {"sb.bench.scene": a["scene"], "sb.bench.out": str(result_file), **a["props"]}
    cmd = gradlew() + [":integration:runClientGameTest", "-PmoduleClientTest=bench", f"-PbenchRunDir={RUN_DIR}",
                       "-PbenchProps=" + ";".join(f"{k}={v}" for k, v in props.items()), "--console=plain"]
    t = time.time()
    with log_file.open("w", encoding="utf-8") as log:
        try:
            code = subprocess.run(cmd, cwd=REPO, stdout=log, stderr=subprocess.STDOUT, timeout=timeout).returncode
        except subprocess.TimeoutExpired:
            code = "timeout"
    text = log_file.read_text(encoding="utf-8", errors="replace")
    entry = dict(area=key, scenario=name, rep=rep, mods=mods, wall_s=round(time.time() - t, 1), exit=code)
    decision = [l.strip() for l in text.splitlines() if re.search(rf"\b{a['code']}\s+{a['area']}\b", l)]
    if decision:
        entry["decision"] = decision[0][-160:]
    if result_file.is_file():
        entry.update(json.loads(result_file.read_text(encoding="utf-8")))
    else:
        entry["error"] = "\n".join(text.splitlines()[-25:])
    print(f"  {key} {name} #{rep}: exit {code}, {entry['wall_s']} s" + (" (no result)" if "error" in entry else ""), flush=True)
    return entry


def summarize(results: dict) -> dict:
    summary = {}
    for key, a in AREAS.items():
        runs = [r for r in results["runs"] if r["area"] == key and "error" not in r]
        if not runs:
            continue
        per = {}
        for name in dict.fromkeys(r["scenario"] for r in runs):
            rs = [r for r in runs if r["scenario"] == name]
            stats = {}
            for m in METRICS:
                vals = [r[m] for r in rs if isinstance(r.get(m), (int, float))]
                if vals:
                    stats[m] = dict(median=statistics.median(vals), sd=statistics.pstdev(vals) if len(vals) > 1 else 0.0, n=len(vals))
            per[name] = stats
        comp = {}
        ours = per.get("c-ours", {})
        for name, stats in per.items():
            if not name.startswith(("a-", "b-")):
                continue
            rows = {}
            for m, (label, lower, gpu) in METRICS.items():
                if m not in ours or m not in stats or not stats[m]["median"]:
                    continue
                o, x = ours[m]["median"], stats[m]["median"]
                delta = (x - o) / abs(x) * 100 if lower else (o - x) / abs(x) * 100  # >0: ours better
                noise = 2 * max(ours[m]["sd"], stats[m]["sd"])
                sig = abs(o - x) > noise and abs(delta) >= 3
                rows[m] = dict(delta_pct=round(delta, 2), significant=sig, verdict=("besser" if delta > 0 else "schlechter") if sig else "gleich",
                               tendency_only=gpu and a["gpu"])
            decisive = [rows[m] for m in a["decisive"] if m in rows]
            better = any(r["verdict"] == "besser" for r in decisive)
            worse = any(r["verdict"] == "schlechter" for r in decisive) or any(
                rows.get(m, {}).get("verdict") == "schlechter" and not rows[m]["tendency_only"] for m in ("fps_1low", "heap_mb"))
            comp[name] = dict(metrics=rows, winner="ours" if better and not worse else "theirs" if name.startswith("b-") else "-")
        summary[key] = dict(code=a["code"], area=a["area"], gpu_bound=a["gpu"], decisive=a["decisive"], scenarios=per, vs=comp)
    return summary


def report(results: dict, path: Path) -> None:
    s = results["summary"]
    m = results["machine"]
    parts = [f"<h1>Benchmark Simple Optimization</h1><p class=meta>{escape(results['date'])} · {escape(m.get('cpu', ''))} · "
             f"{m.get('cores')} Kerne · {escape(m.get('ram', ''))} · GL: {escape(results.get('gl', '?'))} · {results['runs_per_scenario']} Läufe je Szenario</p>",
             "<p class=note>sb-test hat keine Grafikkarte (llvmpipe): Frame-Zeiten/FPS sind <b>nur Tendenz</b> (GPU-gebunden). "
             "Gültig sind MSPT, Heap, Start-, Chunk- und Mesh-Zeiten. Unterschied zählt nur bei &gt;3 % und &gt;2× Streuung.</p>"]
    for key, a in s.items():
        names = list(a["scenarios"])
        parts.append(f"<h2>{escape(a['code'])} {escape(a['area'])}{' <span class=tag>GPU-gebunden: nur Tendenz</span>' if a['gpu_bound'] else ''}</h2>")
        rows = []
        for metric, (label, lower, gpu) in METRICS.items():
            vals = {n: a["scenarios"][n].get(metric) for n in names}
            if not any(vals.values()):
                continue
            top = max(v["median"] for v in vals.values() if v) or 1
            cells = "".join(
                f"<td><div class=bar style='width:{max(2, 100 * v['median'] / top):.0f}%'></div>{v['median']:.3g}"
                f"<small> ±{v['sd']:.2g}</small></td>" if v else "<td>–</td>" for v in vals.values())
            flag = " <span class=tag>Tendenz</span>" if gpu and a["gpu_bound"] else ""
            rows.append(f"<tr><th>{escape(label)}{flag}<small>{' ↓' if lower else ' ↑'}</small></th>{cells}</tr>")
        parts.append("<div class=scroll><table><tr><th>Kennzahl</th>" + "".join(f"<th>{escape(n)}</th>" for n in names) + "</tr>" + "".join(rows) + "</table></div>")
        for vs, c in a["vs"].items():
            items = ", ".join(f"{escape(METRICS[k][0])} {v['delta_pct']:+.1f} % ({v['verdict']}{', Tendenz' if v['tendency_only'] else ''})"
                              for k, v in c["metrics"].items() if k in a["decisive"] or v["significant"])
            parts.append(f"<p><b>unsere vs {escape(vs)}:</b> Gewinner <b class={'win' if c['winner'] == 'ours' else 'lose'}>{c['winner']}</b> – {items}</p>")
        dec = [r for r in results["runs"] if r["area"] == key and r["scenario"].startswith("d-") and r.get("decision")]
        for r in dec:
            parts.append(f"<p class=meta>{escape(r['scenario'])}: <code>{escape(r['decision'])}</code></p>")
        fails = [r for r in results["runs"] if r["area"] == key and "error" in r]
        if fails:
            parts.append("<p class=note>Ohne Ergebnis: " + ", ".join(f"{escape(r['scenario'])} #{r['rep']}" for r in fails) + "</p>")
    miss = ", ".join(x["modId"] for x in results.get("missing26_3", []))
    parts.append(f"<p class=meta>Ohne 26.3-Fassung übersprungen: {escape(miss or 'keine')}</p>")
    css = """:root{--bg:#fbfaf7;--fg:#1d1d1b;--muted:#5b5b57;--line:#dcd9d0;--bar:#7aa6c2;--win:#1f7a3a;--lose:#9a3b2e;--tag:#efe6c8}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){--bg:#171716;--fg:#ecebe6;--muted:#a8a69f;--line:#3a3935;--bar:#4f7f9c;--win:#6fcf8a;--lose:#f08f7c;--tag:#4a4224}}
:root[data-theme="dark"]{--bg:#171716;--fg:#ecebe6;--muted:#a8a69f;--line:#3a3935;--bar:#4f7f9c;--win:#6fcf8a;--lose:#f08f7c;--tag:#4a4224}
body{background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif;margin:0;padding:16px;max-width:1100px}
.scroll{overflow-x:auto}table{border-collapse:collapse;width:100%;font-size:13px}td,th{border-bottom:1px solid var(--line);padding:4px 6px;text-align:left;vertical-align:top}
.bar{height:5px;background:var(--bar);border-radius:2px;margin-bottom:2px}small{color:var(--muted)}.meta,.note{color:var(--muted);font-size:13px}
.tag{background:var(--tag);border-radius:3px;padding:0 4px;font-size:11px;font-weight:normal}.win{color:var(--win)}.lose{color:var(--lose)}code{font-size:12px;word-break:break-all}"""
    path.write_text(f"<!doctype html><html lang=de><head><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'>"
                    f"<title>Benchmark Simple Optimization</title><style>{css}</style></head><body>{''.join(parts)}</body></html>\n", encoding="utf-8")


def apply_priorities(files: list[Path]) -> None:
    """Concept §5.1: ours only when every series says ours (≥3 % median, 1%-low/heap not worse), at least 2 series."""
    series = [json.loads(f.read_text(encoding="utf-8")) for f in files]
    if len(series) < 2:
        raise SystemExit("--apply-priorities needs at least 2 separate result series")
    for key, a in AREAS.items():
        target = REPO / "modules" / a["owner"] / "shared/resources/data" / a["owner"] / "priorities.json"
        table = json.loads(target.read_text(encoding="utf-8"))
        verdicts = [s.get("summary", {}).get(key, {}).get("vs", {}) for s in series]
        for rival in a["rivals"]:
            name = f"b-{rival}"
            wins = [v.get(name, {}).get("winner") == "ours" for v in verdicts]
            gpu_only = a["gpu"] and any(s["machine"]["profile"] == "sbtest" for s in series)
            winner = "ours" if all(wins) and not gpu_only else "theirs"
            entry = table["areas"].setdefault(a["area"], {})
            entry.update(winner=winner if entry.get("winner") != "ours" or winner == "ours" else "theirs",
                         measured=", ".join(f"{s['date']} {s['machine']['profile']}" for s in series), runs=len(series),
                         delta={rival: [v.get(name, {}).get("metrics", {}) for v in verdicts][-1]})
        target.write_text(json.dumps(table, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"{target.relative_to(REPO)}: {a['area']} -> {table['areas'][a['area']]['winner']}")


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--profile", default="sbtest", choices=["sbtest", "pc"])
    p.add_argument("--runs", type=int, default=None, help="runs per a/b/c scenario (sbtest 3, pc 5)")
    p.add_argument("--both-runs", type=int, default=1, help="runs per d scenario (ours + other mod)")
    p.add_argument("--areas", default="o1,s5a")
    p.add_argument("--only", default="", help="comma list of scenario names (smoke runs), e.g. a-vanilla,c-ours")
    p.add_argument("--timeout", type=int, default=1800, help="seconds per run")
    p.add_argument("--out", default=None)
    p.add_argument("--report", type=Path, help="only rebuild report.html from a results.json")
    p.add_argument("--recover", type=Path, help="with --report: folder with run JSONs to merge into entries without result")
    p.add_argument("--apply-priorities", nargs="+", type=Path)
    args = p.parse_args()
    if args.apply_priorities:
        apply_priorities(args.apply_priorities)
        return 0
    if args.report:
        data = json.loads(args.report.read_text(encoding="utf-8"))
        if args.recover:  # run JSONs written elsewhere (relative sb.bench.out before the fix): merge them back in
            for r in data["runs"]:
                f = args.recover / f"{r['area']}-{r['scenario']}-{r['rep']}.json"
                if "error" in r and f.is_file():
                    r.pop("error")
                    r.update(json.loads(f.read_text(encoding="utf-8")))
            data["gl"] = next((r.get("gl_renderer") for r in data["runs"] if r.get("gl_renderer")), "?")
        data["summary"] = summarize(data)
        args.report.write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
        report(data, args.report.with_name("report.html"))
        return 0
    runs = args.runs or (5 if args.profile == "pc" else 3)
    keys = [k.strip() for k in args.areas.split(",") if k.strip()]
    # Absolute: the game resolves sb.bench.out against its own run folder.
    out_dir = (Path(args.out) if args.out else REPO / "bench-results" / f"{dt.date.today().isoformat()}-{args.profile}").resolve()
    out_dir.mkdir(parents=True, exist_ok=True)
    foreign_ids = sorted({r for k in keys for r in AREAS[k]["rivals"]} | {lib for k in keys for r in AREAS[k]["rivals"] for lib in LIBS.get(r, [])})
    foreign_jars = fetch_mods.fetch(foreign_ids)
    jars = our_jars({AREAS[k]["owner"] for k in keys})
    results = dict(date=dt.datetime.now().isoformat(timespec="minutes"), machine=machine(args.profile), mc="26.3",
                   runs_per_scenario=runs, missing26_3=fetch_mods.load().get("missing26_3", []), runs=[])
    path = out_dir / "results.json"
    for key in keys:
        plan = [s for s in scenarios(key, runs, args.both_runs) if not args.only or s[0] in args.only.split(",")]
        print(f"{key}: {', '.join(f'{n} x{r}' for n, _, _, r in plan)}", flush=True)
        for rep in range(1, max([r for *_, r in plan] + [0]) + 1):
            for name, foreign, ours, reps in plan:
                if rep <= reps:
                    results["runs"].append(run_once(key, name, foreign, ours, rep, jars, foreign_jars, out_dir, args.timeout))
                    results["gl"] = next((r.get("gl_renderer") for r in results["runs"] if r.get("gl_renderer")), "?")
                    results["summary"] = summarize(results)
                    path.write_text(json.dumps(results, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
                    report(results, out_dir / "report.html")
    shutil.rmtree(REPO / "integration" / RUN_DIR / "saves", ignore_errors=True)
    print(f"results: {path}; report: {out_dir / 'report.html'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
