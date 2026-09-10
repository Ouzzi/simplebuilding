#!/usr/bin/env python3
"""
Ports the shared CLIENT test tree from the MC 26.2 line to the MC 1.21.11 line.

The sibling script port_tests_to_1_21_11.py does the same for the server tests.
This one exists for the same reason and was written the same way: by porting the
whole tree once, reading every difference out of the mapped 1.21.11 jar, and
then writing the recurring ones down so the next edit to a shared test body is
one invocation away from being on both lines.

The rules below are all measured, not guessed. Every one of them was a compiler
error in the first port (131 of them across 11 685 lines), and the replacement
was checked against the class file, not against memory:

  client.gui.screen()  ->  client.screen
  client.gui.setScreen ->  client.setScreen
      26.2 moved the screen behind Gui; on 1.21.11 it is still a public field on
      Minecraft itself.

  client.gui.hud.getChat()   ->  client.gui.getChat()
  client.gui.hud.isHidden()  ->  client.options.hideGui
  client.gui.hud.toggle()    ->  flipping that flag
  client.gui.hud             ->  client.gui
      26.2 split the HUD out of Gui into its own Hud object with a hide toggle.
      1.21.11 has neither; the flag is the plain field Options.hideGui, and it
      carries the same two guarantees the screenshot tests rely on -
      GameRenderer.renderItemInHand skips the first person hand while it is set,
      and Gui.render skips the whole HUD.

  client.gui.toastManager()  ->  client.getToastManager()

  ItemStackTemplate.fromNonEmptyStack(x)  ->  x
      BundleContents holds plain ItemStacks on 1.21.11.

  BundleContents#getSelectedItemIndex  ->  getSelectedItem

  TextColor.AQUA etc.  ->  TextColor.fromLegacyFormat(ChatFormatting.AQUA)
      The named constants arrived with 26.2.

  keyMapping.matches(type.getOrCreate(code))
      ->  keyMapping.saveString().equals(type.getOrCreate(code).getName())
      26.2 takes the InputConstants.Key itself; 1.21.11 takes a KeyEvent and has
      a separate matchesMouse for buttons. Comparing the saved names is the same
      statement for both key types and needs no event to be invented - and
      saveString is what the failure messages already print.

  renderer.state.level.*  ->  renderer.state.*
  state.blockPos() / .progress()  ->  the fields
      The block breaking render states lost a package level and their accessors.

  net.minecraft.client.gui.GuiGraphicsExtractor  ->  ...gui.GuiGraphics
  renderer.state.gui.*  ->  client.gui.render.state.*
  centeredText(...)  ->  drawCenteredString(...)
  extractImage(...)   ->  renderImage(...)
  screen.extractRenderState(...)  ->  screen.render(...)
      The two phase GUI extraction exists on both lines; on 1.21.11 the class is
      still called GuiGraphics and the render state lives elsewhere.

  BlockStateModelSet   ->  BlockModelShaper (via getBlockModelShaper)
  BlockStateModelPart  ->  BlockModelPart
  quad.materialInfo().sprite()  ->  quad.sprite()

What this script deliberately does NOT do:

  * The two loader drivers. They are small, they differ in more than
    substitutions (1.21.11 runs against fabric-client-gametest-api-v1 4.3.5,
    which has no getConnection()), and each carries its own comment about it.
  * The chat filter. ChatComponent.setVisibleMessageFilter is 26.2 only, and the
    substitute - Options.chatVisibility set to HIDDEN - needs a place to
    remember the old value. That is a decision, so it is reported, not made.

Usage
    python tools/port_client_tests_to_1_21_11.py --check
    python tools/port_client_tests_to_1_21_11.py --all
    python tools/port_client_tests_to_1_21_11.py HudAndTooltipClientTest ...

Always compile afterwards. The rules cover what recurs, not everything.
"""

from __future__ import annotations

import argparse
import difflib
import io
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
SOURCE = REPO / "common/src/shared/clientgametest/java/com/simplebuilding/clientgametest"
TARGET = REPO / "mc1_21_11/shared/clientgametest/java/com/simplebuilding/clientgametest"

#: Wird von der Chat-Regel vor restoreHudDrift eingesetzt. Steht hier statt inline,
#: weil ein mehrzeiliger Java-Block in einem Regex-Ersatz unlesbar wird.
CHAT_FIELD_DECLARATION = (
    "    /**\n"
    "     * The chat visibility on entry, so restoreHudDrift can put it back.\n"
    "     *\n"
    "     * <p>A field rather than a second return value: silenceHudDrift already returns the\n"
    "     * vignette, the two calls are always a pair inside one case, and on 26.2 there is nothing\n"
    "     * to carry at all - there the filter is restored without remembering anything. Keeping the\n"
    "     * two signatures identical across the lines is worth more than the field is worth avoiding.\n"
    "     */\n"
    "    private static Later<ChatVisiblity> chatVisibilityBefore =\n"
    "            new Later<>(\"the chat visibility found on entry\");\n"
    "\n"
)

#: (Muster, Ersatz, Begruendung). Reihenfolge zaehlt: die spezielleren Regeln
#: muessen vor den allgemeineren stehen, sonst frisst client.gui.hud die
#: getChat()-Regel auf.
RULES: list[tuple[str, str, str]] = [
    (r"client\.gui\.screen\(\)", "client.screen",
     "der Bildschirm haengt auf 1.21.11 noch an Minecraft"),
    (r"client\.gui\.setScreen\(", "client.setScreen(",
     "dito"),
    (r"client\.gui\.hud\.getChat\(\)", "client.gui.getChat()",
     "kein eigenes Hud-Objekt"),
    (r"!client\.gui\.hud\.isHidden\(\)", "!client.options.hideGui",
     "die HUD-Sperre ist auf 1.21.11 ein Options-Feld"),
    (r"client\.gui\.hud\.isHidden\(\)", "client.options.hideGui",
     "dito"),
    (r"client\.gui\.hud\.toggle\(\)", "client.options.hideGui = !client.options.hideGui",
     "dito, es gibt keinen Umschalter"),
    (r"client\.gui\.hud\b", "client.gui",
     "Restverwendungen, etwa die Reflexion auf overlayMessageString"),
    (r"client\.gui\.toastManager\(\)", "client.getToastManager()",
     "die Toasts haengen an Minecraft"),
    (r"client\.levelExtractor\.allChanged\(\)", "client.levelRenderer.allChanged()",
     "den Chunk-Neubau haelt auf 1.21.11 noch der LevelRenderer, nicht der Extraktor"),
    (r"ItemStackTemplate\.fromNonEmptyStack\(", "(",
     "BundleContents haelt auf 1.21.11 schon ItemStacks"),
    (r"\.map\(ItemStackTemplate::fromNonEmptyStack\)", "",
     "dito, die Stroemungsvariante - ohne Umwandlung ist die Liste schon richtig"),
    (r"import net\.minecraft\.world\.item\.ItemStackTemplate;\n", "",
     "dito"),
    (r"\.getSelectedItemIndex\(\)", ".getSelectedItem()",
     "BundleContents-Zugriffsmethode heisst anders"),
    (r"TextColor\.(AQUA|GOLD|BLUE|RED|GREEN|YELLOW|WHITE|GRAY)\b",
     r"TextColor.fromLegacyFormat(ChatFormatting.\1)",
     "TextColor hat auf 1.21.11 keine benannten Konstanten"),
    (r"!(\S+?)\.matches\((.*?)\.getOrCreate\((\w+)\)\)",
     r"!\1.saveString().equals(\2.getOrCreate(\3).getName())",
     "matches nimmt auf 1.21.11 ein KeyEvent, nicht den Key"),
    (r"net\.minecraft\.client\.renderer\.state\.level\.", "net.minecraft.client.renderer.state.",
     "die Abbau-Zustaende liegen eine Ebene hoeher"),
    (r"\.blockPos\(\)", ".blockPos",
     "MovingBlockRenderState.blockPos ist ein Feld"),
    (r"(\bstate|\bframe\.get\(0\))\.progress\(\)", r"\1.progress",
     "BlockBreakingRenderState.progress ist ein Feld"),
    (r"import net\.minecraft\.client\.gui\.GuiGraphicsExtractor;",
     "import net.minecraft.client.gui.GuiGraphics;",
     "GuiGraphicsExtractor heisst auf 1.21.11 noch GuiGraphics"),
    (r"import net\.minecraft\.client\.renderer\.state\.gui\.",
     "import net.minecraft.client.gui.render.state.",
     "der GUI-Renderzustand liegt woanders"),
    (r"\bGuiGraphicsExtractor\b", "GuiGraphics",
     "dito, Verwendungsstellen"),
    (r"\.centeredText\(", ".drawCenteredString(",
     "GuiGraphics zeichnet direkt statt zu sammeln"),
    (r"public void centeredText\(", "public void drawCenteredString(",
     "dito, die Ueberschreibung im Rekorder"),
    (r"\.extractImage\(", ".renderImage(",
     "ClientTooltipComponent-Methode heisst anders"),
    (r"screen\.extractRenderState\(graphics, ", "screen.render(graphics, ",
     "Screen zeichnet auf 1.21.11 direkt"),
    (r"screen\.extractRenderStateWithTooltipAndSubtitles\(", "screen.renderWithTooltipAndSubtitles(",
     "dito, die Fassung mit dem aufgeschobenen Tooltip"),
    (r"\.pickParticleMaterial\(RandomSource\.create\(\)\)\.sprite\(\)", ".pickParticleIcon(RandomSource.create())",
     "ItemStackRenderState nennt sein Partikelbild auf 1.21.11 direkt als Sprite"),
    (r"import net\.minecraft\.client\.renderer\.block\.BlockStateModelSet;",
     "import net.minecraft.client.renderer.block.BlockModelShaper;",
     "der Modell-Nachschlag heisst BlockModelShaper"),
    (r"import net\.minecraft\.client\.renderer\.block\.dispatch\.BlockStateModelPart;\n", "",
     "BlockModelPart liegt bei den uebrigen Modellklassen"),
    (r"import net\.minecraft\.client\.renderer\.block\.dispatch\.BlockStateModel;",
     "import net.minecraft.client.renderer.block.model.BakedQuad;\n"
     "import net.minecraft.client.renderer.block.model.BlockModelPart;\n"
     "import net.minecraft.client.renderer.block.model.BlockStateModel;",
     "dito"),
    (r"import net\.minecraft\.client\.resources\.model\.geometry\.BakedQuad;\n", "",
     "BakedQuad liegt bei den Modellklassen"),
    (r"import net\.minecraft\.client\.gui\.Hud;", "import net.minecraft.client.gui.Gui;",
     "kein Hud auf 1.21.11"),
    (r"\bHud\.class\b", "Gui.class",
     "dito, overlayMessageString sitzt auf Gui"),
    (r"Hud\.overlayMessageString", "Gui.overlayMessageString",
     "dito, im Fehlertext"),
    (r"BlockStateModelSet models = client\.getModelManager\(\)\.getBlockStateModelSet\(\);",
     "BlockModelShaper models = client.getModelManager().getBlockModelShaper();",
     "der Nachschlag geht ueber den Shaper"),
    (r"models\.get\(state\)", "models.getBlockModel(state)",
     "dito"),
    (r"model == models\.missingModel\(\)",
     "model == client.getModelManager().getMissingBlockStateModel()",
     "das Ersatzmodell haengt am ModelManager"),
    (r"\bBlockStateModelPart\b", "BlockModelPart",
     "Teilmodelle heissen anders"),
    (r"quad\.materialInfo\(\)\.sprite\(\)", "quad.sprite()",
     "BakedQuad traegt die Textur direkt"),
    (r"for \(ItemStackTemplate template : contents\.items\(\)\) \{\n            ItemStack stack = template\.create\(\);\n",
     "for (ItemStack stack : contents.items()) {\n",
     "der Buendelinhalt ist auf 1.21.11 schon ein ItemStack"),
    (r"client\.gui\.getChat\(\)\.setVisibleMessageFilter\(message -> false\);",
     "chatVisibilityBefore.set(client.options.chatVisibility().get());\n"
     "            client.options.chatVisibility().set(ChatVisiblity.HIDDEN);",
     "ChatComponent kennt den Filter erst ab 26.2; die Sichtbarkeitsoption tut dasselbe"),
    (r"client\.gui\.getChat\(\)\.setVisibleMessageFilter\(message -> true\);",
     "client.options.chatVisibility().set(chatVisibilityBefore.get());",
     "dito, das Zurueckstellen"),
    (r'(Later<Boolean> vignetteBefore = new Later<>\("the vignette option found on entry"\);\n)',
     r'\1        chatVisibilityBefore = new Later<>("the chat visibility found on entry");\n',
     "der alte Wert der Sichtbarkeitsoption muss gemerkt werden"),
    (r"(    private static void restoreHudDrift\(Script script, Later<Boolean> vignetteBefore\) \{)",
     CHAT_FIELD_DECLARATION + r"\1",
     "dito, das Feld dazu, direkt vor dem einzigen Leser"),
    (r"put the vignette and the chat filter back", "put the vignette and the chat visibility back",
     "dito, der Schrittname"),
]

#: Was gemeldet statt geraten wird.
HAND_WORK: list[tuple[str, str]] = [
    (r"setVisibleMessageFilter\(",
     "ChatComponent#setVisibleMessageFilter gibt es auf 1.21.11 nicht. Der Ersatz ist "
     "Options.chatVisibility = HIDDEN, aber der alte Wert muss irgendwo gemerkt werden - "
     "siehe HudAndTooltipClientTest#silenceHudDrift auf der 1.21.11-Seite"),
    (r"\bItemStackTemplate\b",
     "ItemStackTemplate ist nach den Regeln noch uebrig - eine Stelle, die nicht dem "
     "Muster fromNonEmptyStack(...) folgt"),
    (r"\bMouseButtonEvent\b",
     "MouseButtonEvent-Signaturen unterscheiden sich zwischen den Linien; von Hand pruefen"),
]

#: Dateien, die der gemeinsame Baum hat, die 1.21.11 aber selbst pflegt.
HAND_MAINTAINED: set[str] = {
    # Der Vertrag selbst. Sein packetsSettled-Javadoc beschreibt, was auf DIESER Linie gilt -
    # dass dort auch Fabric die Frage nicht exakt beantworten kann, weil die 4.3.5-API
    # getConnection() nicht hat. Das ist keine Uebersetzung, sondern eine andere Aussage.
    "Harness",
}


def force_utf8_stdout() -> None:
    for name in ("stdout", "stderr"):
        stream = getattr(sys, name)
        if isinstance(stream, io.TextIOWrapper) and (stream.encoding or "").lower() != "utf-8":
            stream.reconfigure(encoding="utf-8", errors="replace")


def sources() -> list[str]:
    return sorted(p.stem for p in SOURCE.glob("*.java") if p.stem not in HAND_MAINTAINED)


def translate(text: str) -> tuple[str, int]:
    total = 0
    for pattern, replacement, _why in RULES:
        text, n = re.subn(pattern, replacement, text)
        total += n

    if "ChatFormatting." in text and "import net.minecraft.ChatFormatting;" not in text:
        text = text.replace("import net.minecraft.client.",
                            "import net.minecraft.ChatFormatting;\nimport net.minecraft.client.", 1)
        total += 1

    # Die Chat-Regel bringt ihren eigenen Typ mit; ohne diesen Import waere sie nur halb
    # uebersetzt, und der Compiler saehe eine Klasse, die die 26.2-Seite nie brauchte.
    if "ChatVisiblity" in text and "import net.minecraft.world.entity.player.ChatVisiblity;" not in text:
        text = text.replace("import net.minecraft.world.inventory.Slot;",
                            "import net.minecraft.world.entity.player.ChatVisiblity;\n"
                            "import net.minecraft.world.inventory.Slot;", 1)
        total += 1

    return text, total


def port(name: str, check_only: bool) -> tuple[int, list[str], bool]:
    """Translates one class. Returns (substitutions, notes, changed)."""
    text = (SOURCE / f"{name}.java").read_bytes().decode("utf-8").replace("\r\n", "\n")
    text, total = translate(text)

    notes = []
    for pattern, note in HAND_WORK:
        hits = len(re.findall(pattern, text))
        if hits:
            notes.append(f"{hits}x  {note}")

    target = TARGET / f"{name}.java"
    before = target.read_bytes().decode("utf-8").replace("\r\n", "\n") if target.exists() else ""
    changed = before != text

    if changed and not check_only:
        target.write_bytes(text.replace("\n", "\r\n").encode("utf-8"))

    return total, notes, changed


def main(argv: list[str] | None = None) -> int:
    force_utf8_stdout()
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[1])
    parser.add_argument("names", nargs="*", help="Klassennamen ohne .java")
    parser.add_argument("--all", action="store_true", help="alle gemeinsamen Client-Tests portieren")
    parser.add_argument("--check", action="store_true",
                        help="nur zeigen, was sich aendern wuerde; nichts schreiben")
    parser.add_argument("--diff", action="store_true", help="mit --check auch den Unterschied zeigen")
    args = parser.parse_args(argv)

    names = args.names or (sources() if (args.all or args.check) else [])
    if not names:
        parser.print_help()
        return 2

    TARGET.mkdir(parents=True, exist_ok=True)
    stale = []

    for name in names:
        if not (SOURCE / f"{name}.java").exists():
            print(f"  ?  {name}: gibt es im gemeinsamen Baum nicht")
            continue

        total, notes, changed = port(name, args.check)
        mark = "veraltet" if (changed and args.check) else ("neu geschrieben" if changed else "gleich")
        print(f"  {name}: {total} Ersetzungen, {mark}")

        if changed:
            stale.append(name)

        for note in notes:
            print(f"       von Hand: {note}")

        if changed and args.check and args.diff:
            source = (SOURCE / f"{name}.java").read_bytes().decode("utf-8").replace("\r\n", "\n")
            new, _ = translate(source)
            old = (TARGET / f"{name}.java").read_bytes().decode("utf-8").replace("\r\n", "\n")
            for line in list(difflib.unified_diff(old.splitlines(), new.splitlines(),
                                                  "1.21.11", "portiert", lineterm=""))[:80]:
                print("       " + line)

    if args.check and stale:
        print()
        print(f"{len(stale)} Datei(en) haengen hinter dem gemeinsamen Baum zurueck: "
              + ", ".join(stale))
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
