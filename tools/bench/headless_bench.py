#!/usr/bin/env python3
"""Headless Simple Optimization measurement (N27b OPT-e4, plan docs/ai/PLAN-B-OPTE4-2026-11-11.md).

Runs the server gametest simpleoptimization:module_game_test_bench_headless (OptimizationTests.benchHeadless) in the
standalone instance of Simple Optimization (only the super-mod, its three sub-mods, SimpleLib and Fabric API), each run a
fresh JVM, interleaved per round:
  off      every area of the three sub-mods "off" and every "Enable Simple Opt X" switch false (= without the mod's
           effect; the jars stay loaded, since the bench test lives in the super-mod)
  default  no config files: what a player gets after installing
  all      every area "auto" including S5b activation_range (gameplay-changing, default off)
Metrics per run: MSPT avg/p95 over 400 server ticks (fixed scene: 120 cows, 64 hoppers, 32 furnaces, 6^3 leaves),
heap after GC, O4 state-table counts, O1 leaf faces (Vanilla mesher rule vs. ours on the server level).
Profile "sb" (N31b L9, plan docs/ai/PLAN-B-BENCHSB31-2026-11-11.md): Vanilla server (Fabric API only) against SimpleBuilding
+ its 20 mandatory mods (integration/bench-sb-full.json, closure of modules.json "requires" of simplebuilding + Cloth Config),
same scene, same fresh world/seed, interleaved fresh JVMs, median of >= 3 runs, via the sbintegration gametest
SbBenchGameTest (-PbenchSb). Verdict PASS/FAIL: median MSPT avg of "sb" at most 10 % above "vanilla" (owner f5=B); heap is
only reported. On FAIL (or --hot) one extra "sb" run with a server-thread stack sampler names the top com.simplebuilding frames.
No client, no GPU: FPS stays a follow-up (client run, concept §6.5). Output bench-results/<date>-headless/results.json + .md.

  python3.12 tools/bench/headless_bench.py --runs 2
  python3.12 tools/bench/headless_bench.py --profile sb --runs 5   (L9, b-benchsb31: see below)
  python3.12 tools/bench/headless_bench.py --report bench-results/<dir>/results.json     (rebuild results.md)
Runner target: python3.12 tools/testrunner/run.py --targets bench-opt-headless-263
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import re
import shutil
import statistics
import subprocess
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]
RUN_DIR = REPO / "integration" / "run-standalone-fabric-263" / "simpleoptimization"
TASK = ":integration:runSimpleoptimizationStandaloneGameTest"
TEST = "simpleoptimization:module_game_test_bench_headless"
LINE = re.compile(r"SB-BENCH (?:headless|sb) (\{.*\})")
AREAS = {
    "simpleoptrender": ["leaf_culling", "lightmap_cache", "fast_ip_ping", "no_telemetry", "more_culling", "details"],
    "simpleoptmemory": ["state_tables"],
    "simpleoptserver": ["dynamic_distance", "activation_range"],
}
SWITCHES = {"simpleoptrender": "enableSimpleOptRender", "simpleoptmemory": "enableSimpleOptMemory",
            "simpleoptserver": "enableSimpleOptServer"}
SCENARIOS = ("off", "default", "all")
SB_SCENARIOS = ("vanilla", "sb")
SB_TASK = ":integration:runIntegrationGameTest"
SB_TEST = "sbintegration:sb_bench_game_test_measure"
SB_LIMIT_PCT = 10.0
PROFILES = {"opt": SCENARIOS, "sb": SB_SCENARIOS}
# metric -> (label, lower is better)
METRICS = {"mspt_avg": ("MSPT Mittel ms", True), "mspt_p95": ("MSPT p95 ms", True), "heap_mb": ("Heap nach GC MB", True),
           "state_tables_shared": ("O4 geteilte Arrays", False), "state_tables_saved_kib": ("O4 gespart KiB", False),
           "leaf_faces_ours": ("O1 Blattflächen (Vanilla s. u.)", True), "mods": ("Mods geladen", False)}


def write_config(scenario: str, cfg: Path = RUN_DIR / "config") -> None:
    cfg.mkdir(parents=True, exist_ok=True)
    for name in list(AREAS) + ["simpleoptimization"]:
        (cfg / f"{name}.json").unlink(missing_ok=True)
    if scenario == "default":
        return
    mode = "off" if scenario == "off" else "auto"
    for owner, areas in AREAS.items():
        data = {"areas": {a: {"mode": mode} for a in areas}, "conflictScan": True}
        (cfg / f"{owner}.json").write_text(json.dumps(data, indent=2), encoding="utf-8")
    switches = {key: scenario != "off" for key in SWITCHES.values()}
    (cfg / "simpleoptimization.json").write_text(json.dumps(switches, indent=2), encoding="utf-8")


def gradlew() -> list[str]:
    return [str(REPO / ("gradlew.bat" if os.name == "nt" else "gradlew"))]


def sb_command(scenario: str, hot: bool) -> list[str]:
    """Vanilla = no module jars (integration/bench-sb-vanilla.json); sb = SimpleBuilding + 20 mandatory mods."""
    run = REPO / "integration" / "run-fabric-263"
    shutil.rmtree(run / "config", ignore_errors=True)  # defaults as a player gets them, never leftovers of other suites
    shutil.rmtree(run / "world", ignore_errors=True)   # same fresh world/seed
    return gradlew() + [SB_TASK, f"-PintegrationMods=integration/bench-sb-{'full' if scenario == 'sb' else 'vanilla'}.json",
                        "-PbenchSb", f"-PgametestFilter={SB_TEST}", "--console=plain"] + (["-PbenchSbProfile"] if hot else [])


def run_once(scenario: str, rep: int, out_dir: Path, timeout: int, hot: bool = False) -> dict:
    sb = scenario in SB_SCENARIOS
    if not sb:
        write_config(scenario)
    log = out_dir / "logs" / f"{scenario}{'-hot' if hot else ''}-{rep}.log"
    log.parent.mkdir(parents=True, exist_ok=True)
    cmd = sb_command(scenario, hot) if sb else gradlew() + [TASK, f"-PgametestFilter={TEST}", "--console=plain"]
    t = time.time()
    with log.open("w", encoding="utf-8") as fh:
        try:
            code = subprocess.run(cmd, cwd=REPO, stdout=fh, stderr=subprocess.STDOUT, timeout=timeout).returncode
        except subprocess.TimeoutExpired:
            code = "timeout"
    text = log.read_text(encoding="utf-8", errors="replace")
    entry = dict(scenario=scenario, rep=rep, exit=code, wall_s=round(time.time() - t, 1), log=str(log.relative_to(REPO)))
    hits = LINE.findall(text)
    if hits:
        entry.update(json.loads(hits[-1]))
    else:
        entry["error"] = "\n".join(text.splitlines()[-20:])
    print(f"  {scenario} #{rep}: exit {code}, {entry['wall_s']} s" + (" (no result)" if "error" in entry else
          f", MSPT {entry.get('mspt_avg')} ms, heap {entry.get('heap_mb')} MB"), flush=True)
    return entry


def summarize(runs: list[dict]) -> dict:
    out = {}
    for sc in SCENARIOS + SB_SCENARIOS:
        rs = [r for r in runs if r["scenario"] == sc and "error" not in r]
        if not rs:
            continue
        s = {"runs": len(rs), "areas_on": rs[-1].get("areas_on", ""), "leaf_faces_vanilla": rs[-1].get("leaf_faces_vanilla")}
        for m in METRICS:
            vals = [r[m] for r in rs if isinstance(r.get(m), (int, float))]
            if vals:
                s[m] = round(statistics.median(vals), 3)
                s[m + "_sd"] = round(statistics.stdev(vals), 3) if len(vals) > 1 else 0.0
        out[sc] = s
    base = out.get("off") or out.get("vanilla")
    base_name = "off" if "off" in out else "vanilla"
    if base:
        for sc, s in out.items():
            if sc == base_name:
                continue
            s["delta_vs_off_pct"] = {m: round(100.0 * (s[m] - base[m]) / base[m], 1)
                                     for m in METRICS if m in s and m in base and base[m]}
    return out


def verdict(summ: dict) -> dict | None:
    """L9: PASS when the median MSPT average of sb is at most SB_LIMIT_PCT above Vanilla (owner f5=B)."""
    v, b = summ.get("vanilla"), summ.get("sb")
    if not v or not b or not v.get("mspt_avg"):
        return None
    pct = round(100.0 * (b["mspt_avg"] - v["mspt_avg"]) / v["mspt_avg"], 1)
    p95 = round(100.0 * (b["mspt_p95"] - v["mspt_p95"]) / v["mspt_p95"], 1) if v.get("mspt_p95") else None
    return {"result": "PASS" if pct <= SB_LIMIT_PCT else "FAIL", "mspt_avg_delta_pct": pct, "mspt_p95_delta_pct": p95,
            "limit_pct": SB_LIMIT_PCT, "heap_delta_mb": round(b["heap_mb"] - v["heap_mb"], 1)}


def sb_markdown(results: dict) -> str:
    summ, ver = results["summary"], results.get("verdict")
    cols = [sc for sc in SB_SCENARIOS if sc in summ]
    lines = [f"# SimpleBuilding gegen Vanilla headless ({results['date']}, {results['machine']})", "",
             f"Runs je Setup: {results['runs_per_scenario']}; Median (± Std.-Abw.). vanilla = Fabric API + Messmod; sb = SimpleBuilding + 20 "
             "Pflicht-Mods (Welle 1) + Cloth Config. Szene: 120 Kühe, 64 Trichter, 32 Öfen, 6³ Blätter, 400 Server-Ticks nach 100 Ticks "
             "Warmup, frische Welt. Kein Client.", "", "| Kennzahl | " + " | ".join(cols) + " |", "|---|" + "---|" * len(cols)]
    for m, (label, _) in METRICS.items():
        if not any(m in summ[c] for c in cols) or m.startswith(("state_", "leaf_")):
            continue
        cells = []
        for c in cols:
            s = summ[c]
            d = s.get("delta_vs_off_pct", {}).get(m)
            cells.append(f"{s[m]} ± {s[m + '_sd']}" + (f" ({d:+}%)" if d is not None else "") if m in s else "–")
        lines.append(f"| {label} | " + " | ".join(cells) + " |")
    if ver:
        lines += ["", f"**Bewertung: {ver['result']}** - MSPT Mittel {ver['mspt_avg_delta_pct']:+} % gegen Vanilla "
                  f"(Grenze +{ver['limit_pct']:g} %), p95 {ver['mspt_p95_delta_pct']:+} %, Heap {ver['heap_delta_mb']:+} MB (nur berichtet)."]
    hot = next((r.get("hot") for r in results["runs"] if r.get("hot")), None)
    if hot:
        lines += ["", f"Stack-Sampler (Server-Thread, {hot['samples']} Proben, davon {hot['samples_in_simplebuilding']} in com.simplebuilding):", ""]
        lines += [f"- Mod `{k}`: {v}" for k, v in hot["by_mod"].items()] + [""]
        lines += [f"- `{k}`: {v}" for k, v in hot["by_method"].items()]
    bad = [r for r in results["runs"] if "error" in r]
    if bad:
        lines += ["", "Ohne Ergebnis: " + ", ".join(f"{r['scenario']}#{r['rep']} (exit {r['exit']})" for r in bad)]
    lines += ["", "sb-test ist geteilt: nur bei leerem Rechner messen (Wachlauf in docs/ai/PLAN-B-BENCHSB31-2026-11-11.md)."]
    return "\n".join(lines) + "\n"


def markdown(results: dict) -> str:
    if results.get("profile") == "sb":
        return sb_markdown(results)
    summ = results["summary"]
    lines = [f"# Simple Optimization headless ({results['date']}, {results['machine']})", "",
             f"Runs je Szenario: {results['runs_per_scenario']}; Median (± Std.-Abw.). Szene: 120 Kühe, 64 Trichter, "
             "32 Öfen, 6³ Blätter, 400 Server-Ticks nach 100 Ticks Warmup. Kein Client: FPS nicht gemessen.", "",
             "| Kennzahl | " + " | ".join(sc for sc in SCENARIOS if sc in summ) + " |",
             "|---|" + "---|" * len([sc for sc in SCENARIOS if sc in summ])]
    for m, (label, _) in METRICS.items():
        if not any(m in summ[sc] for sc in SCENARIOS if sc in summ):
            continue
        cells = []
        for sc in SCENARIOS:
            if sc not in summ:
                continue
            s = summ[sc]
            if m not in s:
                cells.append("–")
                continue
            d = s.get("delta_vs_off_pct", {}).get(m)
            cells.append(f"{s[m]} ± {s[m + '_sd']}" + (f" ({d:+}%)" if d is not None else ""))
        lines.append(f"| {label} | " + " | ".join(cells) + " |")
    vanilla = next((s.get("leaf_faces_vanilla") for s in summ.values() if s.get("leaf_faces_vanilla")), None)
    lines += ["", f"Blattflächen Vanilla (Mesher-Regel): {vanilla}.", ""]
    lines += [f"- {sc}: Bereiche an = `{summ[sc]['areas_on'] or '(keiner)'}`" for sc in SCENARIOS if sc in summ]
    bad = [r for r in results["runs"] if "error" in r]
    if bad:
        lines += ["", "Ohne Ergebnis: " + ", ".join(f"{r['scenario']}#{r['rep']} (exit {r['exit']})" for r in bad)]
    lines += ["", "Gilt nur, wenn Unterschied > 2× Streuung und > 3 % (Konzept §6.3). sb-test ist geteilt: MSPT verrauscht."]
    return "\n".join(lines) + "\n"


def run(runs: int, scenarios: list[str], out_dir: Path, timeout: int, profile: str = "opt", hot: bool = False) -> dict:
    out_dir.mkdir(parents=True, exist_ok=True)
    entries = []
    for rep in range(1, runs + 1):
        for sc in scenarios:
            entries.append(run_once(sc, rep, out_dir, timeout))
    summ = summarize(entries)
    ver = verdict(summ) if profile == "sb" else None
    if profile == "sb" and (hot or (ver and ver["result"] == "FAIL")):
        # a separate sampled run: the sampler must not disturb the measured ones
        e = run_once("sb", 0, out_dir, timeout, hot=True)
        e["scenario"] = "sb_hot"
        entries.append(e)
    if profile == "opt":
        write_config("default")  # leave the instance as a player gets it
    results = dict(date=dt.date.today().isoformat(), machine=os.uname().nodename if hasattr(os, "uname") else "",
                   profile=profile, runs_per_scenario=runs, test=SB_TEST if profile == "sb" else TEST, runs=entries,
                   summary=summ, verdict=ver)
    (out_dir / "results.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
    (out_dir / "results.md").write_text(markdown(results), encoding="utf-8")
    return results


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--runs", type=int, default=2)
    p.add_argument("--profile", choices=sorted(PROFILES), default="opt", help="opt = Simple Optimization areas, sb = SimpleBuilding vs Vanilla")
    p.add_argument("--hot", action="store_true", help="profile sb: always add the sampled run (default: only on FAIL)")
    p.add_argument("--scenarios", default=None)
    p.add_argument("--out", type=Path, default=None)
    p.add_argument("--timeout", type=int, default=1800, help="seconds per run")
    p.add_argument("--report", type=Path, default=None, help="rebuild results.md from a results.json")
    a = p.parse_args()
    if a.report:
        res = json.loads(a.report.read_text(encoding="utf-8"))
        res["summary"] = summarize(res["runs"])
        if res.get("profile") == "sb":
            res["verdict"] = verdict(res["summary"])
        a.report.with_suffix(".md").write_text(markdown(res), encoding="utf-8")
        return 0
    allowed = PROFILES[a.profile]
    scs = [s for s in (a.scenarios or ",".join(allowed)).split(",") if s]
    unknown = set(scs) - set(allowed)
    if unknown:
        raise SystemExit(f"unknown scenarios {sorted(unknown)}; possible: {', '.join(allowed)}")
    if a.profile == "sb" and a.runs < 3:
        raise SystemExit("profile sb needs --runs >= 3 (median)")
    out = a.out or REPO / "bench-results" / (f"{dt.date.today().isoformat()}-headless" + ("-sb" if a.profile == "sb" else ""))
    res = run(a.runs, scs, out, a.timeout, a.profile, a.hot)
    print(markdown(res))
    if res.get("verdict"):
        print(f"L9 verdict: {res['verdict']['result']} ({res['verdict']['mspt_avg_delta_pct']:+} % MSPT)")
    return 0 if all("error" not in r for r in res["runs"]) else 1


if __name__ == "__main__":
    sys.exit(main())
