#!/usr/bin/env python3
"""
The counter-check for a sharpened test: break the mod on purpose and require red.

A test that was sharpened to catch a defect has proved nothing until the defect has
been put in and the test has gone red on it. This runs that proof for the client
tests, where a round costs six to eight minutes and therefore has to be planned:

  * every mutation names the file, the exact text to replace, the replacement, the
    client SCRIPT it lands in and the message the sharpened step has to fail with;
  * one round applies at most one mutation per script - two in the same script would
    hide each other, because a script stops at its first failing step - and as many
    scripts in parallel as have a mutation left;
  * after the run every expected message has to be in the log, and NO script may
    have failed that had no mutation - a mutation whose damage spreads further than
    its test claims is reported, not tolerated;
  * the files are restored from git afterwards, which is why the working tree has
    to be clean before this starts.

Usage
    python tools/testrunner/mutations.py --list
    python tools/testrunner/mutations.py --plan                 # rounds, nothing run
    python tools/testrunner/mutations.py --run                  # all rounds
    python tools/testrunner/mutations.py --run --only hopper-glyph-swap,bundle-scale
    python tools/testrunner/mutations.py --run --target client-fabric-262

Results land in testing/mutations/<timestamp>.json and are summarised on stdout.
"""

from __future__ import annotations

import argparse
import datetime as dt
import io
import json
import re
import subprocess
import sys
from dataclasses import dataclass, asdict
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
SHARED = "common/src/shared/java/com/simplebuilding"
CLIENT_MAIN = "src/main/java/com/simplebuilding"


@dataclass(frozen=True)
class Mutation:
    id: str
    file: str
    old: str
    new: str
    script: str
    expect: str
    claim: str


#: Every entry is one of the 21 client side false greens from testing/audit_falsegreens.json,
#: with the mutation the audit named - not a softer one - and the message the sharpened step
#: now fails with. The order inside a script does not matter; the planner spreads them.
MUTATIONS: list[Mutation] = [
    Mutation("hopper-button-geometry",
             f"{SHARED}/client/gui/NetheriteHopperScreen.java",
             "}).bounds(buttonX, buttonY, 18, 18).build());",
             "}).bounds(this.leftPos - 40, this.topPos - 40, 30, 30).build());",
             "hud-and-tooltip", "The hopper filter button is 30x30",
             "the filter button is 18x18 right of the five slots"),
    Mutation("hopper-glyph-swap",
             f"{SHARED}/client/gui/NetheriteHopperScreen.java",
             'String text = (mode == HopperFilterMode.WHITELIST) ? "✔" : "T";',
             'String text = (mode == HopperFilterMode.WHITELIST) ? "T" : "✔";',
             "hud-and-tooltip", "filter button draws [T]",
             "whitelist draws the check mark, type match the T"),
    Mutation("hopper-overlay-colour",
             f"{SHARED}/client/gui/NetheriteHopperScreen.java",
             "context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x60FFAA00);",
             "context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x60FF00FF);",
             "hud-and-tooltip", "has no 16x16 overlay in 0x60FFAA00",
             "a filtered slot carries the orange overlay"),
    Mutation("hopper-ghost-in-occupied-slot",
             f"{SHARED}/client/gui/NetheriteHopperScreen.java",
             "                    if (slot.getItem().isEmpty()) {\n                        context.item(ghostStack, slotX, slotY);",
             "                    if (true) {\n                        context.item(ghostStack, slotX, slotY);",
             "hud-and-tooltip", "painted over a real item",
             "the ghost icon stays out of an occupied slot"),
    Mutation("hopper-ghost-count",
             f"{SHARED}/blocks/entity/custom/ModHopperBlockEntity.java",
             "                ItemStack copy = stack.copy();\n                copy.setCount(1);\n                ghostItems.set(slot, copy);\n            }\n        }\n    }\n}",
             "                ItemStack copy = stack.copy();\n                ghostItems.set(slot, copy);\n            }\n        }\n    }\n}",
             "hud-and-tooltip", "instead of exactly one diamond",
             "the client side ghost item is clamped to one"),
    Mutation("hopper-immediate-feedback",
             f"{SHARED}/client/gui/NetheriteHopperScreen.java",
             "                if (this.menu.getBlockEntity() instanceof ModHopperBlockEntity be) {\n                    be.setGhostItemClient(hoveredSlot.getContainerSlot(), cursorStack);\n                }",
             "",
             "hud-and-tooltip", "still has no ghost item in slot 0",
             "the screen writes the ghost item locally, in the same tick"),
    Mutation("rangefinder-volume",
             f"{SHARED}/client/gui/RangefinderHudOverlay.java",
             "            int dx = Math.abs(pos1.getX() - pos2.getX()) + 1;\n            int dy = Math.abs(pos1.getY() - pos2.getY()) + 1;\n            int dz = Math.abs(pos1.getZ() - pos2.getZ()) + 1;",
             "            int dx = Math.abs(pos1.getX() - pos2.getX());\n            int dy = Math.abs(pos1.getY() - pos2.getY());\n            int dz = Math.abs(pos1.getZ() - pos2.getZ());",
             "hud-and-tooltip", "The rangefinder HUD does not say [Volume: 80",
             "both ends of the selection count"),
    Mutation("rangefinder-offhand",
             f"{SHARED}/client/gui/RangefinderHudOverlay.java",
             "        if (!hasOctant) {\n            stack = client.player.getOffhandItem();\n            if (stack.getItem() instanceof OctantItem) {\n                hasOctant = true;\n            }\n        }",
             "",
             "hud-and-tooltip", "With the octant in the OFF hand the rangefinder HUD draws nothing",
             "an off hand octant shows the HUD too"),
    Mutation("bundle-scale",
             f"{CLIENT_MAIN}/SimplebuildingClient.java",
             "float scale = (float) reinforcedData.maxCapacity() / 64.0f;",
             "float scale = (float) reinforcedData.maxCapacity() / 32.0f;",
             "hud-and-tooltip", "does not draw like a component scaled by maxCapacity / 64",
             "the tooltip bar scale is the capacity in stacks"),
    Mutation("wand-needs-block-hit",
             f"{SHARED}/client/render/BuildingWandPreviewRenderer.java",
             "if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK)",
             "if (!(hit instanceof BlockHitResult blockHit))",
             "building-wand-preview", "must not preview anything while the crosshair finds no block",
             "a miss is a BlockHitResult too, and draws nothing"),
    Mutation("wand-ghost-centred",
             f"{SHARED}/client/render/BuildingWandPreviewRenderer.java",
             "REPLACED_BY_PATCH_FUNCTION",
             "",
             "building-wand-preview", "The ghost preview is not centred",
             "each ghost shrinks around its block centre"),
    Mutation("settings-key-offhand",
             f"{CLIENT_MAIN}/SimplebuildingClient.java",
             "ItemStack stack = client.player.getMainHandItem();\n                    if (stack.getItem() instanceof OctantItem) {",
             "ItemStack stack = client.player.getMainHandItem().isEmpty() ? client.player.getOffhandItem() : client.player.getMainHandItem();\n                    if (stack.getItem() instanceof OctantItem) {",
             "client-bootstrap", "for an enchanted building wand in the off hand",
             "the settings key reads the main hand only"),
    Mutation("settings-key-netherite-only",
             f"{CLIENT_MAIN}/SimplebuildingClient.java",
             "} else if (stack.getItem() instanceof BuildingWandItem) {",
             "} else if (stack.is(com.simplebuilding.items.ModItems.NETHERITE_BUILDING_WAND)) {",
             "client-bootstrap", "The building wand settings for the copper wand never opened",
             "every wand tier opens the screen"),
    Mutation("octant-scroll-any-item",
             f"{SHARED}/mixin/client/MouseMixin.java",
             "if (client.player.getMainHandItem().getItem() instanceof OctantItem) {",
             "if (true) {",
             "client-bootstrap", "Scrolling with Control while holding STONE",
             "the wheel is only intercepted for an octant"),
    Mutation("octant-plain-only",
             f"{CLIENT_MAIN}/SimplebuildingClient.java",
             "                    if (stack.getItem() instanceof OctantItem) {",
             "                    if (stack.is(com.simplebuilding.items.ModItems.OCTANT)) {",
             "client-bootstrap", "The octant manager for a coloured octant never opened",
             "every octant colour opens the manager"),
    Mutation("pick-creative-guard",
             f"{SHARED}/mixin/client/MinecraftClientMixin.java",
             "        if (this.player.isCreative()) return;\n",
             "",
             "client-bootstrap", "In creative the pick took stone out of the Master Builder bundle",
             "the pick stays out of it in creative"),
    Mutation("space-key-every-tick",
             f"{CLIENT_MAIN}/SimplebuildingClient.java",
             "                if (isJumpPressed != wasJumpPressed) {\n                    ClientNetworking.send(new SpaceKeyPayload(isJumpPressed));\n                    wasJumpPressed = isJumpPressed;\n                }",
             "                ClientNetworking.send(new SpaceKeyPayload(isJumpPressed));\n                wasJumpPressed = isJumpPressed;",
             "client-bootstrap", "SpaceKeyPayloads instead of one",
             "the key state is sent only when it changes"),
    Mutation("hammer-should-break",
             f"{SHARED}/client/render/BlockHighlightRenderer.java",
             "REPLACED_BY_PATCH_FUNCTION",
             "",
             "block-highlight", "did not lose a box when one neighbour became oak planks",
             "only blocks the hammer would break get a box"),
    Mutation("strip-miner-correct-tool",
             f"{SHARED}/client/render/MultiBlockBreakingSupport.java",
             "} else if (stack.getItem().isCorrectToolForDrops(stack, mainState) && sneaking) {",
             "} else if (sneaking) {",
             "multi-block-breaking", "iron Strip Miner pickaxe produced breaking cracks",
             "sneaking alone is not enough; the tool has to fit"),
    Mutation("survival-zero-fields",
             f"{SHARED}/mixin/SurvivalTracerMixin.java",
             "new SurvivalSyncPayload(currentDist, currentTime, totalHostileKills, totalPassiveKills, currentDamage)",
             "new SurvivalSyncPayload(0, currentTime, totalPassiveKills, totalHostileKills, 0)",
             "smoke", "SurvivalSyncPayload carries the wrong numbers",
             "every field of the sync carries its own number"),
]

#: Mutations whose "old" text is not one contiguous block. Each takes the file text and
#: returns it mutated, or raises if the anchor is gone.
PATCH_FUNCTIONS = {
    "wand-ghost-centred": lambda text: _drop_lines(text, [
        "poseStack.translate(0.5, 0.5, 0.5);",
        "poseStack.translate(-0.5, -0.5, -0.5);"]),
    "hammer-should-break": lambda text: _drop_block(text,
        "if (!SledgehammerUtils.shouldBreak(client.level, pos, centerPos, stack)) {",
        "continue;", "}"),
}


def _drop_lines(text: str, needles: list[str]) -> str:
    lines = text.split("\n")
    kept = []
    dropped = 0
    for line in lines:
        if any(n in line for n in needles):
            dropped += 1
            continue
        kept.append(line)
    if dropped != len(needles):
        raise RuntimeError(f"expected to drop {len(needles)} lines, dropped {dropped}")
    return "\n".join(kept)


def _drop_block(text: str, head: str, body: str, tail: str) -> str:
    lines = text.split("\n")
    for i in range(len(lines) - 2):
        if head in lines[i] and body in lines[i + 1] and lines[i + 2].strip() == tail:
            return "\n".join(lines[:i] + lines[i + 3:])
    raise RuntimeError(f"block starting with {head!r} not found")


def force_utf8_stdout() -> None:
    for name in ("stdout", "stderr"):
        stream = getattr(sys, name)
        if isinstance(stream, io.TextIOWrapper) and (stream.encoding or "").lower() != "utf-8":
            stream.reconfigure(encoding="utf-8", errors="replace")


def plan(selected: list[Mutation]) -> list[list[Mutation]]:
    """Rounds with at most one mutation per script."""
    remaining = list(selected)
    rounds: list[list[Mutation]] = []
    while remaining:
        this_round: list[Mutation] = []
        used: set[str] = set()
        for m in list(remaining):
            if m.script in used:
                continue
            this_round.append(m)
            used.add(m.script)
            remaining.remove(m)
        rounds.append(this_round)
    return rounds


def mutate(m: Mutation, text: str) -> str:
    """The mutated file text, or a RuntimeError naming the missing anchor."""
    if m.id in PATCH_FUNCTIONS:
        mutated = PATCH_FUNCTIONS[m.id](text)
    else:
        if m.old not in text:
            raise RuntimeError(f"{m.id}: anchor not found in {m.file}")
        mutated = text.replace(m.old, m.new, 1)
    if mutated == text:
        raise RuntimeError(f"{m.id}: mutation changed nothing")
    return mutated


def apply(m: Mutation) -> None:
    path = REPO / m.file
    path.write_text(mutate(m, path.read_text(encoding="utf-8")), encoding="utf-8")


def check_anchors(selected: list[Mutation]) -> int:
    """Every anchor has to be in its file, or the mutation could never be applied."""
    problems = 0
    for m in selected:
        try:
            mutate(m, (REPO / m.file).read_text(encoding="utf-8"))
            print(f"  ok   {m.id}")
        except (RuntimeError, FileNotFoundError) as e:
            problems += 1
            print(f"  FEHLT {m.id}: {e}")
    return problems


def restore(files: set[str]) -> None:
    subprocess.run(["git", "checkout", "--", *sorted(files)], cwd=REPO, check=True)


def working_tree_clean(files: set[str]) -> bool:
    out = subprocess.run(["git", "status", "--porcelain", "--", *sorted(files)],
                         cwd=REPO, capture_output=True, text=True).stdout
    return out.strip() == ""


LOG_FOR_TARGET = {
    "client-fabric-262": "build/run/clientGameTest/logs/latest.log",
    "client-neoforge-262": "neoforge/build/run/clientGameTest/logs/latest.log",
    "client-fabric-12111": "mc1_21_11/fabric/build/run/clientGameTest/logs/latest.log",
    "client-neoforge-12111": "mc1_21_11/neoforge/build/run/clientGameTest/logs/latest.log",
}

FAILED_LINE = re.compile(r"\[(?P<script>[a-z-]+)\] FAILED: (?P<message>.*)|FAILED in (?P<script2>[a-z-]+) at step '(?P<step>[^']*)': (?P<message2>.*)")


def failures_in_log(target: str) -> dict[str, str]:
    """script -> failure message, from the client log of the last run."""
    text = (REPO / LOG_FOR_TARGET[target]).read_text(encoding="utf-8", errors="replace")
    found: dict[str, str] = {}
    for line in text.splitlines():
        m = FAILED_LINE.search(line)
        if not m:
            continue
        script = m.group("script") or m.group("script2")
        message = m.group("message") or m.group("message2") or ""
        found.setdefault(script, message)
    return found


def run_round(number: int, mutations: list[Mutation], target: str, timeout: int) -> dict:
    files = {m.file for m in mutations}
    if not working_tree_clean(files):
        raise SystemExit("the files to mutate are not clean in git; commit or stash first: "
                         + ", ".join(sorted(files)))

    print(f"\nRunde {number}: " + ", ".join(m.id for m in mutations))
    for m in mutations:
        apply(m)
        print(f"  eingespielt  {m.id}  ({m.file})")

    try:
        subprocess.run([sys.executable, "tools/testrunner/run.py", "--targets", target,
                        "--timeout", str(timeout), "--trigger", f"mutation-round-{number}"],
                       cwd=REPO, capture_output=True, text=True)
        failures = failures_in_log(target)
    finally:
        restore(files)
        print("  zurueckgenommen")

    verdicts = []
    mutated_scripts = {m.script for m in mutations}
    for m in mutations:
        message = failures.get(m.script)
        if message is None:
            verdict = "GRUEN GEBLIEBEN - die Schaerfung beisst nicht"
            ok = False
        elif m.expect in message:
            verdict = "rot, mit der erwarteten Meldung"
            ok = True
        else:
            verdict = "rot, aber mit einer ANDEREN Meldung: " + message[:200]
            ok = False
        verdicts.append({"id": m.id, "script": m.script, "ok": ok, "verdict": verdict,
                         "message": message})
        print(f"  {'OK ' if ok else 'XX '} {m.id}: {verdict}")

    collateral = {s: msg for s, msg in failures.items() if s not in mutated_scripts}
    for script, message in collateral.items():
        print(f"  !! Nebenschaden in {script}: {message[:200]}")

    return {"round": number, "target": target, "mutations": verdicts,
            "collateral": collateral}


def main(argv: list[str] | None = None) -> int:
    force_utf8_stdout()
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[1])
    parser.add_argument("--list", action="store_true")
    parser.add_argument("--check", action="store_true", help="nur pruefen, ob jeder Anker noch da ist")
    parser.add_argument("--plan", action="store_true")
    parser.add_argument("--run", action="store_true")
    parser.add_argument("--only", default="", help="comma separated mutation ids")
    parser.add_argument("--target", default="client-fabric-262")
    parser.add_argument("--timeout", type=int, default=1200)
    args = parser.parse_args(argv)

    selected = MUTATIONS
    if args.only:
        wanted = set(args.only.split(","))
        unknown = wanted - {m.id for m in MUTATIONS}
        if unknown:
            raise SystemExit("unbekannte Mutationen: " + ", ".join(sorted(unknown)))
        selected = [m for m in MUTATIONS if m.id in wanted]

    if args.list:
        for m in MUTATIONS:
            print(f"  {m.id:32s} {m.script:22s} {m.claim}")
        return 0

    if args.check:
        return 1 if check_anchors(selected) else 0

    rounds = plan(selected)
    if args.plan or not args.run:
        for i, r in enumerate(rounds, 1):
            print(f"Runde {i}: " + ", ".join(f"{m.id} [{m.script}]" for m in r))
        print(f"{len(rounds)} Runden fuer {len(selected)} Mutationen")
        return 0

    results = []
    for i, r in enumerate(rounds, 1):
        results.append(run_round(i, r, args.target, args.timeout))

    out_dir = REPO / "testing" / "mutations"
    out_dir.mkdir(parents=True, exist_ok=True)
    stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%dT%H-%M-%SZ")
    out = out_dir / f"{stamp}.json"
    out.write_text(json.dumps({"target": args.target, "rounds": results}, indent=2,
                              ensure_ascii=False), encoding="utf-8")

    total = sum(len(r["mutations"]) for r in results)
    bitten = sum(1 for r in results for v in r["mutations"] if v["ok"])
    collateral = sum(len(r["collateral"]) for r in results)
    print(f"\n{bitten} von {total} Mutationen wurden rot mit der erwarteten Meldung, "
          f"{collateral} Nebenschaeden. Datensatz: {out.relative_to(REPO)}")
    return 0 if bitten == total and collateral == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
