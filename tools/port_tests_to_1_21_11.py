#!/usr/bin/env python3
"""
Ports game test classes from the MC 26.2 tree to the MC 1.21.11 tree.

The two Minecraft lines keep separate copies of everything, tests included. Most
of a test body is identical between them; what differs is a handful of API
changes that recur in every single class. Translating those by hand once per
class is where mistakes creep in - so they live here, each with the reason it
exists, and the port becomes one invocation plus whatever the compiler still
complains about.

The rules below were all established by porting real classes and reading the
mapped jars, not guessed:

  EntityTypes -> EntityType
      The split into a separate EntityTypes holder only happened in 26.2; on
      1.21.11 the constants still sit on EntityType itself.

  ItemStack#typeHolder().is(tag) -> ItemStack#is(tag)
      typeHolder() is 26.2 only. is(TagKey<Item>) exists on both and says the
      same thing.

  ItemFrame#interact(player, hand, hitVec) -> interact(player, hand)
      1.21.11's signature takes no hit position.

  helper.runBeforeTestEnd(x) -> TestCleanup.before(helper, x)
  helper.succeed()           -> TestCleanup.succeed(helper)
  .thenSucceed()             -> .thenExecute(() -> TestCleanup.run(helper)).thenSucceed()
      1.21.11's GameTestHelper has no runBeforeTestEnd. TestCleanup collects the
      clean-up and runs it where every body already has a point: right before it
      reports success. See mc1_21_11/.../gametest/TestCleanup.java.

What this script deliberately does NOT do:

  * GameTestHelper#makeMockServerPlayer(GameType) has no 1.21.11 counterpart and
    needs a hand rolled ServerPlayer subclass. There is no safe mechanical
    rewrite, so the script reports the call sites and leaves them alone.
  * The catalogue and the Fabric adapters. Those are generated centrally, and a
    class that cannot run on this line must be left out of the catalogue with a
    reason rather than silently dropped.

Usage
    python tools/port_tests_to_1_21_11.py --check          # what is missing
    python tools/port_tests_to_1_21_11.py --all            # port everything missing
    python tools/port_tests_to_1_21_11.py MagnetTests ...  # port named classes

Always compile afterwards; the rules cover the recurring differences, not every
one. Anything left is a real API gap that wants a decision, not a substitution.
"""

from __future__ import annotations

import argparse
import io
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
SOURCE = REPO / "common/src/shared/java/com/simplebuilding/gametest"
TARGET = REPO / "mc1_21_11/shared/java/com/simplebuilding/gametest"

#: (Muster, Ersatz, Begruendung). Reihenfolge zaehlt: die Aufraeum-Regeln bauen
#: aufeinander auf, deshalb kommt succeed() nach runBeforeTestEnd().
RULES: list[tuple[str, str, str]] = [
    (r"import net\.minecraft\.world\.entity\.EntityTypes;",
     "import net.minecraft.world.entity.EntityType;",
     "EntityTypes gibt es erst ab 26.2"),
    (r"\bEntityTypes\.",
     "EntityType.",
     "dito, Verwendungsstellen"),
    (r"helper\.runBeforeTestEnd\(",
     "TestCleanup.before(helper, ",
     "1.21.11 hat den Hook nicht, siehe TestCleanup"),
    (r"helper\.succeed\(\);",
     "TestCleanup.succeed(helper);",
     "Aufraeumen vor dem Erfolg"),
    (r"\.thenSucceed\(\);",
     ".thenExecute(() -> TestCleanup.run(helper))\n                .thenSucceed();",
     "dito, aber innerhalb einer Sequenz"),
    (r"\.typeHolder\(\)\.is\(",
     ".is(",
     "ItemStack.typeHolder() gibt es erst ab 26.2; is(TagKey) tut dasselbe"),
    (r"(\w+)\.interact\((\w+), InteractionHand\.(\w+), Vec3\.ZERO\)",
     r"\1.interact(\2, InteractionHand.\3)",
     "ItemFrame.interact nimmt auf 1.21.11 keinen Trefferpunkt"),
]

#: Was sich nicht mechanisch uebersetzen laesst und gemeldet statt geraten wird.
HAND_WORK: list[tuple[str, str]] = [
    (r"makeMockServerPlayer\(",
     "GameTestHelper#makeMockServerPlayer(GameType) gibt es auf 1.21.11 nicht - der "
     "ServerPlayer mit ueberschriebenem gameMode() muss von Hand gebaut werden, siehe "
     "ConsumptionAndDurabilityTests#detachedPlayer in mc1_21_11"),
    (r"\bBlockItemTags\b",
     "BlockItemTags (gepaarte Block-/Item-Tags) gibt es erst ab 26.2; auf 1.21.11 stehen "
     "die Konstanten noch in BlockTags"),
    (r"\bsendOverlayMessage\(",
     "sendOverlayMessage heisst auf 1.21.11 displayClientMessage(component, true)"),
    (r"getAirDrag\(\)",
     "Entity#getAirDrag gibt es nur auf 26.2; auf 1.21.11 steht die 0.98 als Literal"),
]


def force_utf8_stdout() -> None:
    for name in ("stdout", "stderr"):
        stream = getattr(sys, name)
        if isinstance(stream, io.TextIOWrapper) and (stream.encoding or "").lower() != "utf-8":
            stream.reconfigure(encoding="utf-8", errors="replace")


def test_classes(directory: Path) -> set[str]:
    return {p.stem for p in directory.glob("*Tests.java")}


def missing() -> list[str]:
    return sorted(test_classes(SOURCE) - test_classes(TARGET))


def port(name: str) -> tuple[int, list[str]]:
    """Translates one class. Returns (substitutions, notes needing a human)."""
    text = (SOURCE / f"{name}.java").read_bytes().decode("utf-8").replace("\r\n", "\n")

    total = 0
    for pattern, replacement, _why in RULES:
        text, n = re.subn(pattern, replacement, text)
        total += n

    notes: list[str] = []
    for pattern, note in HAND_WORK:
        hits = len(re.findall(pattern, text))
        if hits:
            notes.append(f"{hits}x  {note}")

    (TARGET / f"{name}.java").write_bytes(text.replace("\n", "\r\n").encode("utf-8"))
    return total, notes


#: Classes whose 1.21.11 body is allowed to differ in substance, each with the reason. The
#: drift check subtracts the mechanical rules first and then complains about what is left; a
#: class listed here is reported but does not fail the check. Every entry names a REAL line
#: difference, not a port that has fallen behind.
DRIFT_EXPLAINED: dict[str, str] = {
    "TradeAndMigrationTests": "Handel ist erst ab MC 26.1 datengetrieben; die 26.2-Faelle zu Registry und "
                              "Tag-Merge haben auf 1.21.11 kein Gegenstueck (siehe LINE_DIFFERENCES)",
    "TradeRegistryTests": "26.2-only, siehe LINE_DIFFERENCES",
    "TradeOfferTests": "26.2-only, siehe LINE_DIFFERENCES",
    "BuildingEnchantmentTests": "der Stock-Abschnitt (ConstructorsTouchInteraction) fehlt auf 1.21.11 - die "
                                "Logik liegt dort je Loader doppelt, siehe die Notiz in der Klasse",
    "BundleWiringTests": "Handelsangebote: 26.2 zieht sie aus der VILLAGER_TRADE-Registry mit LootParams, "
                         "1.21.11 aus ModTradeDefinitions - gleiche Aussagen, anderer Weg",
    "WandEnchantmentTests": "wie BundleWiringTests: Registry gegen ModTradeDefinitions",
    "MiningEnchantmentTests": "wie BundleWiringTests: Registry gegen ModTradeDefinitions",
    "HopperTests": "ContainerInput heisst auf 1.21.11 ClickType, assemble nimmt den Registry-Zugriff; "
                   "sonst dieselben Zeilen",
}

#: How many lines a class may have on one side only before the drift check calls it drift.
#: Reworded messages and moved braces produce a handful; a sharpening that never reached the
#: other line produces dozens.
DRIFT_TOLERANCE = 12


def normalised_body(text: str) -> set[str]:
    """The lines of a test class that mean something, in 26.2 spelling, as a set.

    The 1.21.11 helpers (TestCleanup, Assertions) are folded back into the 26.2 calls, the
    mechanical RULES are applied in reverse where they can be, imports and comments are dropped.
    What remains differs only where the BODIES differ.
    """
    text = text.replace("\r\n", "\n")
    text = re.sub(r"Assertions\.valueEqual\(helper, ", "helper.assertValueEqual(", text)
    text = text.replace("TestCleanup.succeed(helper);", "helper.succeed();")
    text = re.sub(r"TestCleanup\.before\(helper, ", "helper.runBeforeTestEnd(", text)
    text = text.replace(".thenExecute(() -> TestCleanup.run(helper))\n                .thenSucceed();", ".thenSucceed();")
    text = re.sub(r"\bEntityType\.", "EntityTypes.", text)
    text = re.sub(r"^import .*\n", "", text, flags=re.M)
    return {line.strip() for line in text.split("\n")
            if line.strip() and not line.strip().startswith(("*", "//", "/*"))}


def drift() -> list[tuple[str, int, int, str]]:
    """(class, lines only on 26.2, lines only on 1.21.11, explanation or '') per class, worst first."""
    rows = []
    for source in sorted(SOURCE.glob("*Tests.java")):
        target = TARGET / source.name
        if not target.exists():
            rows.append((source.stem, -1, -1, "fehlt"))
            continue
        a = normalised_body(source.read_bytes().decode("utf-8"))
        b = normalised_body(target.read_bytes().decode("utf-8"))
        rows.append((source.stem, len(a - b), len(b - a), DRIFT_EXPLAINED.get(source.stem, "")))
    rows.sort(key=lambda r: -r[1])
    return rows


def main(argv: list[str] | None = None) -> int:
    force_utf8_stdout()
    parser = argparse.ArgumentParser(description="Ports game tests to the MC 1.21.11 tree.")
    parser.add_argument("classes", nargs="*", help="class names, e.g. MagnetTests")
    parser.add_argument("--all", action="store_true", help="port every class the target lacks")
    parser.add_argument("--check", action="store_true", help="only list what is missing")
    parser.add_argument("--drift", action="store_true",
                        help="compare the BODIES of the classes both lines have; exit 1 on unexplained drift")
    args = parser.parse_args(argv)

    if args.drift:
        print()
        print(f"  {'Klasse':40s} {'nur 26.2':>9s} {'nur 1.21.11':>12s}")
        bad = 0
        for name, only26, only11, why in drift():
            flag = ""
            if only26 < 0:
                flag = "FEHLT"
                bad += 1
            elif only26 > DRIFT_TOLERANCE:
                flag = "erklaert: " + why if why else "ZURUECKGEFALLEN"
                if not why:
                    bad += 1
            print(f"  {name:40s} {max(only26, 0):9d} {max(only11, 0):12d}  {flag}")
        print()
        if bad:
            print(f"  {bad} Klasse(n) haengen ohne Erklaerung hinter dem 26.2-Koerper zurueck - mit dem "
                  "Werkzeug nachziehen, dann uebersetzen.")
            return 1
        print("  Kein unerklaerter Unterschied zwischen den Testkoerpern der beiden Linien.")
        return 0

    gap = missing()
    if args.check or (not args.classes and not args.all):
        print()
        print(f"  26.2:    {len(test_classes(SOURCE))} Testklassen")
        print(f"  1.21.11: {len(test_classes(TARGET))} Testklassen")
        print()
        if gap:
            print("  Fehlt auf der 1.21.11-Linie:")
            for name in gap:
                print(f"    {name}")
        else:
            print("  Beide Linien tragen dieselben Testklassen.")
        print()
        return 0

    wanted = gap if args.all else args.classes
    unknown = [n for n in wanted if not (SOURCE / f"{n}.java").exists()]
    if unknown:
        raise SystemExit("unbekannte Klassen: " + ", ".join(unknown))

    print()
    for name in wanted:
        count, notes = port(name)
        print(f"  {name}: {count} Ersetzungen")
        for note in notes:
            print(f"      HANDARBEIT  {note}")
    print()
    print("  Jetzt uebersetzen - was der Compiler noch meldet, ist ein echter API-Unterschied")
    print("  und will eine Entscheidung, keine Ersetzung:")
    print("      ./gradlew :mc1_21_11:fabric:compileJava")
    print()
    return 0


if __name__ == "__main__":
    sys.exit(main())
