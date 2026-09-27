package com.simplebuilding.gametest;

import com.simplebuilding.blueprint.BlueprintContent;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.dev.testcentre.TcOp;
import com.simplebuilding.dev.testcentre.TestCentreBuilder;
import com.simplebuilding.dev.testcentre.TestCentreCommand;
import com.simplebuilding.dev.testcentre.TestCentreKits;
import com.simplebuilding.dev.testcentre.TestCentreLayout;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.tweaks.block.FlypadBlock;
import com.simplebuilding.tweaks.block.PadBlock;
import com.simplebuilding.tweaks.block.PadTiers;
import com.simplebuilding.util.OctantShape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

/**
 * Die Testzentrale ({@code /sbtestcentre}, docs/TESTZENTRALE.md): jedes Mod-Item hat dort einen Platz,
 * und der Bau laeuft ohne Fehler genau so ab, wie er geplant ist.
 *
 * <p>Der Abdeckungstest ist absichtlich streng: ein neues Item, das keinem Abschnitt zufaellt, landet
 * in der Wand "Unsortiert" und macht diesen Test rot. Abhilfe ist ein Platz in einem Abschnitt
 * ({@code TestCentreSections}) - oder, fuer technische Eintraege, ein begruendeter Eintrag in
 * {@code TestCentreLayout.EXCLUDED}.
 */
public final class TestCentreTests {

    private TestCentreTests() {
    }

    /** Jedes Mod-Item und jeder Mod-Block steht in einem Abschnitt (ausser den begruendeten Ausnahmen). */
    public static void everyModItemAndBlockHasItsPlaceInTheTestCentre(GameTestHelper helper) {
        TestCentreLayout.Plan plan = TestCentreLayout.plan(helper.getLevel().registryAccess(), BlockPos.ZERO);
        List<String> problems = new ArrayList<>();
        for (Item item : plan.leftovers()) {
            problems.add("item " + BuiltInRegistries.ITEM.getKey(item) + " has no section (it only shows up under 'unsorted')");
        }
        for (Block block : TestCentreLayout.modBlocks()) {
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            boolean shown = plan.coveredBlocks().contains(block) || plan.coveredItems().contains(block.asItem());
            if (!shown && !TestCentreLayout.EXCLUDED.containsKey(id)) {
                problems.add("block " + id + " is neither placed nor framed in any section");
            }
        }
        // Jeder Mod-Block mit Item steht auch gesetzt in der Welt (die Galerie nimmt, was sonst nur im
        // Rahmen haengt) - ausser den Bloecken mit Wirkung auf die Umgebung.
        for (Block block : TestCentreLayout.modBlocks()) {
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            if (block.asItem() != net.minecraft.world.item.Items.AIR && !plan.coveredBlocks().contains(block)
                    && !TestCentreLayout.frameOnly(block) && !TestCentreLayout.EXCLUDED.containsKey(id)) {
                problems.add("block " + id + " is only framed, never placed");
            }
        }
        for (String excluded : TestCentreLayout.EXCLUDED.keySet()) {
            Identifier id = Identifier.parse(excluded);
            if (!BuiltInRegistries.ITEM.containsKey(id) && !BuiltInRegistries.BLOCK.containsKey(id)) {
                problems.add("exclusion " + excluded + " names nothing that is registered - remove it");
            }
        }
        helper.assertTrue(plan.sections().size() == TestCentreLayout.SECTION_IDS.size(),
                "planned sections " + plan.sections().size() + " but SECTION_IDS lists " + TestCentreLayout.SECTION_IDS.size());
        helper.assertTrue(helper.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("sbtestcentre") != null,
                "/sbtestcentre is not registered on this loader");
        helper.assertTrue(problems.isEmpty(), problems.size() + " test centre coverage problem(s): " + String.join("; ", problems));
        helper.succeed();
    }

    /**
     * Die ganze Zentrale wird fern aller anderen Tests gebaut und mit ihrer Planung verglichen: jeder
     * geplante Block steht (der jeweils letzte je Stelle), jedes Schild hat seinen Text, Rahmen und
     * Staender sind genau so viele wie geplant, nichts liegt als Drop herum. Danach wird ein Abschnitt
     * neu gebaut - das darf keine Rahmen verdoppeln.
     */
    public static void theWholeCentreBuildsAndMatchesItsPlan(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = new BlockPos(20000, helper.absolutePos(BlockPos.ZERO).getY() + 1, 20000);
        TestCentreLayout.Plan plan = TestCentreLayout.plan(level.registryAccess(), origin);
        TestCentreBuilder.Result result = TestCentreBuilder.build(level, plan);
        com.simplebuilding.Simplebuilding.LOGGER.info("{} | built in {} ms ({} steps, {} entities)", plan.summary(),
                result.millis(), result.ops(), result.entities());
        try {
            Map<BlockPos, BlockState> expected = new HashMap<>();
            int frames = 0;
            int stands = 0;
            List<BlockPos> signs = new ArrayList<>();
            for (TestCentreLayout.Section section : plan.sections()) {
                for (TcOp op : section.ops()) {
                    if (op.spawnsFrame()) {
                        frames++;
                    }
                    switch (op) {
                        case TcOp.Place place -> expected.put(place.pos(), place.state());
                        case TcOp.Stand stand -> stands++;
                        case TcOp.Sign sign -> signs.add(sign.pos());
                        default -> {
                        }
                    }
                }
            }
            List<String> wrong = new ArrayList<>();
            for (Map.Entry<BlockPos, BlockState> entry : expected.entrySet()) {
                Block actual = level.getBlockState(entry.getKey()).getBlock();
                if (actual != entry.getValue().getBlock() && wrong.size() < 10) {
                    wrong.add(entry.getKey() + " holds " + BuiltInRegistries.BLOCK.getKey(actual) + " instead of "
                            + BuiltInRegistries.BLOCK.getKey(entry.getValue().getBlock()));
                }
            }
            helper.assertTrue(wrong.isEmpty(), "blocks differ from the plan: " + String.join("; ", wrong));
            for (BlockPos pos : signs) {
                helper.assertTrue(level.getBlockEntity(pos) instanceof SignBlockEntity sign
                        && !com.simplebuilding.version.McVersion.signFrontLine(sign, 0).getString().isEmpty(), "no labelled sign at " + pos);
            }
            AABB area = AABB.of(plan.bounds());
            int placedFrames = level.getEntitiesOfClass(ItemFrame.class, area).size();
            int placedStands = level.getEntitiesOfClass(ArmorStand.class, area).size();
            helper.assertTrue(placedFrames == frames, "item frames: planned " + frames + ", placed " + placedFrames);
            helper.assertTrue(placedStands == stands, "armour stands: planned " + stands + ", placed " + placedStands);
            helper.assertTrue(frames > 300, "suspiciously few frames planned: " + frames);
            // Die Blaupause im Rahmen wurde beim Bau wirklich gescannt, der Oktant daneben traegt seine Auswahl.
            for (TestCentreLayout.Section section : plan.sections()) {
                for (TcOp op : section.ops()) {
                    if (op instanceof TcOp.BlueprintFrame frame) {
                        List<ItemFrame> found = level.getEntitiesOfClass(ItemFrame.class, new AABB(frame.pos()));
                        helper.assertTrue(!found.isEmpty() && !found.getFirst().getItem()
                                        .getOrDefault(ModDataComponentTypes.BLUEPRINT, BlueprintContent.EMPTY).equals(BlueprintContent.EMPTY),
                                "the blueprint frame at " + frame.pos() + " holds no scanned blueprint");
                    }
                    if (op instanceof TcOp.OctantFrame frame) {
                        List<ItemFrame> found = level.getEntitiesOfClass(ItemFrame.class, new AABB(frame.pos()));
                        helper.assertTrue(!found.isEmpty() && OctantShape.bounds(OctantShape.data(found.getFirst().getItem())) != null,
                                "the octant frame at " + frame.pos() + " holds no octant with both corners");
                    }
                }
            }
            int drops = level.getEntitiesOfClass(ItemEntity.class, area).size();
            helper.assertTrue(drops == 0, drops + " dropped items lie in the test centre after the build");

            TestCentreBuilder.buildSection(level, plan, "armour");
            int again = level.getEntitiesOfClass(ItemFrame.class, area).size();
            helper.assertTrue(again == frames, "rebuilding the armour section changed the frame count from " + frames + " to " + again);
            BoundingBox box = plan.section("armour").box(origin);
            helper.assertTrue(box.getXSpan() > 10, "armour section is implausibly small: " + box);
        } finally {
            TestCentreBuilder.clear(level, plan.bounds());
        }
        helper.succeed();
    }
    // ------------------------------------------------------------------ Befehlsbloecke und Ausgabe-Knoepfe

    /** Alle Befehlsbloecke der Planung (Steuerwand und Ausgabe-Knoepfe), in Planungsreihenfolge. */
    static List<TcOp.Command> commands(TestCentreLayout.Plan plan) {
        List<TcOp.Command> out = new ArrayList<>();
        for (TestCentreLayout.Section section : plan.sections()) {
            for (TcOp op : section.ops()) {
                if (op instanceof TcOp.Command command) {
                    out.add(command);
                }
            }
        }
        return out;
    }

    /**
     * Ohne Welt, aus der Planung: kein Befehlsblock beruehrt einen anderen oder dessen Knopf, kein
     * Leiterblock (Stein, Wand) beruehrt zwei Befehlsbloecke, Knopf- und Schildplatz sind frei. Ein Knopf
     * versorgt seinen Befehlsblock STARK; ein stark versorgter Leiter schaltet jeden Befehlsblock daneben
     * mit - bis 2026-09-25 loeste so jeder Knopf der Steuerwand auch den Nachbarbefehl aus.
     *
     * <p>Dazu die Ausgabe-Knoepfe: jede Station ausser der Steuerwand hat vorn links einen, der Knopf zeigt
     * zum Gang (Norden, vorderste Reihe), sein Kit ist nicht leer und passt ins Inventar; der Befehl
     * {@code /sbtestcentre give} ist registriert und fuellt Haupt-, Nebenhand und Inventar. Der
     * Fingerabdruck der Planung ist stabil und haengt am Ursprung. Das Flypad-Flugfeld beruehrt kein
     * anderes Pad.
     */
    @SuppressWarnings("removal")
    public static void commandBlocksAreIsolatedAndEveryStationHasItsGiveButton(GameTestHelper helper) {
        BlockPos origin = new BlockPos(0, 64, 0);
        TestCentreLayout.Plan plan = TestCentreLayout.plan(helper.getLevel().registryAccess(), origin);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (TestCentreLayout.Section section : plan.sections()) {
            for (TcOp op : section.ops()) {
                if (op instanceof TcOp.Place place) {
                    blocks.put(place.pos(), place.state());
                }
            }
        }
        List<TcOp.Command> commands = commands(plan);
        Map<BlockPos, TcOp.Command> byPos = new HashMap<>();
        Map<BlockPos, TcOp.Command> byButton = new HashMap<>();
        for (TcOp.Command command : commands) {
            byPos.put(command.pos(), command);
            byButton.put(command.pos().relative(command.facing()), command);
        }
        List<String> problems = new ArrayList<>();
        for (TcOp.Command command : commands) {
            String name = "'" + command.command() + "' at " + command.pos().toShortString();
            BlockPos button = command.pos().relative(command.facing());
            BlockPos sign = command.pos().above().relative(command.facing());
            if (blocks.containsKey(button) && !blocks.get(button).isAir()) {
                problems.add(name + ": its button place is taken by " + blocks.get(button));
            }
            if (blocks.containsKey(sign) && !blocks.get(sign).isAir()) {
                problems.add(name + ": its sign place is taken by " + blocks.get(sign));
            }
            if (blocks.containsKey(command.pos())) {
                problems.add(name + ": a block is planned into the command block itself");
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = command.pos().relative(direction);
                TcOp.Command other = byPos.get(next);
                if (other != null) {
                    problems.add(name + " touches the command block '" + other.command() + "'");
                }
                TcOp.Command buttonOwner = byButton.get(next);
                if (buttonOwner != null && buttonOwner != command) {
                    problems.add(name + " touches the button of '" + buttonOwner.command() + "'");
                }
            }
        }
        // Leiterbloecke neben zwei Befehlsbloecken.
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            if (!entry.getValue().isRedstoneConductor(EmptyBlockGetter.INSTANCE, entry.getKey())) {
                continue;
            }
            List<String> touching = new ArrayList<>();
            for (Direction direction : Direction.values()) {
                TcOp.Command command = byPos.get(entry.getKey().relative(direction));
                if (command != null) {
                    touching.add("'" + command.command() + "'");
                }
            }
            if (touching.size() > 1) {
                problems.add("the conductor " + entry.getValue() + " at " + entry.getKey().toShortString() + " touches " + touching);
            }
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " command block isolation problem(s): "
                + String.join("; ", problems.subList(0, Math.min(10, problems.size()))));

        // Ausgabe-Knoepfe: jede Station mit Inhalt, Knopf vorn zum Gang.
        List<String> stations = new ArrayList<>();
        for (TestCentreLayout.Section section : plan.sections()) {
            String id = section.id();
            if (id.equals("controls")) {
                continue;
            }
            TestCentreKits.Kit kit = plan.kits().get(id);
            // "unsorted" und "gallery" duerfen leer sein (dann ohne Knopf).
            boolean mayBeEmpty = id.equals("unsorted") || id.equals("gallery");
            if (kit == null) {
                if (!mayBeEmpty) {
                    stations.add(id + " has no kit");
                }
                continue;
            }
            helper.assertTrue(!kit.mainHand().isEmpty(), "the kit of " + id + " leaves the main hand empty");
            helper.assertTrue(kit.inventory().size() + (kit.mainHand().isEmpty() ? 0 : 1) <= TestCentreKits.MAIN_SLOTS,
                    "the kit of " + id + " does not fit into the inventory: " + kit.inventory().size());
            BlockPos front = origin.offset(section.offset());
            TcOp.Command give = null;
            for (TcOp op : section.ops()) {
                if (op instanceof TcOp.Command command && command.command().equals(TestCentreLayout.giveCommand(id))) {
                    give = command;
                }
            }
            if (give == null) {
                stations.add(id + " has no give button");
                continue;
            }
            if (give.facing() != Direction.NORTH || give.pos().relative(give.facing()).getZ() != front.getZ()) {
                stations.add(id + ": the give button does not face the aisle (" + give.facing() + ", z "
                        + give.pos().relative(give.facing()).getZ() + " instead of " + front.getZ() + ")");
            }
        }
        helper.assertTrue(stations.isEmpty(), "give buttons: " + String.join("; ", stations));
        for (String withOffHand : List.of("tools", "chisel", "inworld", "planning")) {
            TestCentreKits.Kit kit = plan.kits().get(withOffHand);
            helper.assertTrue(kit != null && !kit.offHand().isEmpty(), "the kit of " + withOffHand + " should fill the off hand");
        }
        helper.assertTrue(plan.kits().get("planning").offHand().is(ModItems.OCTANT),
                "the planning kit should hold the octant in the off hand (roof mode needs it there)");

        // Der Befehl selbst: registriert, und er ersetzt das Inventar durch das Kit.
        var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("sbtestcentre");
        helper.assertTrue(root != null && root.getChild("give") != null && root.getChild("give").getChild("station") != null,
                "/sbtestcentre give <station> is not registered");
        TestCentreKits.Kit blocksKit = plan.kits().get("blocks");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            player.getInventory().setItem(20, new ItemStack(Items.DIRT, 5));
            TestCentreCommand.giveKit(player, blocksKit);
            helper.assertTrue(ItemStack.isSameItemSameComponents(player.getMainHandItem(), blocksKit.mainHand()),
                    "give: main hand holds " + player.getMainHandItem() + " instead of " + blocksKit.mainHand());
            int dirt = 0;
            int kitStacks = 0;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.is(Items.DIRT)) {
                    dirt += stack.getCount();
                }
                for (ItemStack wanted : blocksKit.inventory()) {
                    if (ItemStack.isSameItemSameComponents(stack, wanted)) {
                        kitStacks++;
                        break;
                    }
                }
            }
            helper.assertTrue(dirt == 0, "give should replace the inventory, but " + dirt + " dirt stayed");
            helper.assertTrue(kitStacks == blocksKit.inventory().size(),
                    "give: " + kitStacks + " of " + blocksKit.inventory().size() + " kit stacks arrived in the inventory");
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }

        // Fingerabdruck: gleiche Planung gleicher Wert, anderer Ursprung (andere Befehle) anderer Wert.
        String fingerprint = plan.fingerprint();
        helper.assertTrue(fingerprint.equals(TestCentreLayout.plan(helper.getLevel().registryAccess(), origin).fingerprint()),
                "the plan fingerprint is not stable - every join would rebuild the centre");
        helper.assertTrue(!fingerprint.equals(TestCentreLayout.plan(helper.getLevel().registryAccess(), origin.east(7)).fingerprint()),
                "the plan fingerprint ignores the origin");

        // Flypad: im Flugfeld steht kein anderes Pad und keine Druckplatte.
        List<String> crowded = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            if (!(entry.getValue().getBlock() instanceof FlypadBlock flypad)) {
                continue;
            }
            AABB field = PadTiers.flyArea(entry.getKey(), flypad.getTier());
            for (Map.Entry<BlockPos, BlockState> other : blocks.entrySet()) {
                Block block = other.getValue().getBlock();
                boolean pad = block instanceof PadBlock || block instanceof BasePressurePlateBlock;
                if (pad && !other.getKey().equals(entry.getKey()) && field.intersects(new AABB(other.getKey()))) {
                    crowded.add(BuiltInRegistries.BLOCK.getKey(block) + " at " + other.getKey().toShortString());
                }
            }
        }
        helper.assertTrue(crowded.isEmpty(), "the flypad's flight field reaches other pads: " + crowded);
        helper.succeed();
    }

    /** Ticks zwischen zwei Knopfdruecken; geprueft wird {@link #CHECK_AFTER} Ticks nach dem Druck. */
    static final int PRESS_STEP = 4;
    static final int CHECK_AFTER = 3;
    /** Erster Druck: die erzwungenen Chunks brauchen ein paar Ticks, bis sie Bloecke ticken. */
    static final int FIRST_PRESS = 5;
    public static final int BUTTON_RUN_MAX_TICKS = 600;

    /**
     * Die Zentrale steht wirklich (fern aller Tests, Chunks erzwungen), jeder Befehlsblock bekommt statt
     * seines Befehls einen Zaehler: {@code summon marker} an einer eigenen Stelle ueber ihm. Dann wird jeder
     * Knopf der Reihe nach gedrueckt wie von Hand ({@link ButtonBlock#press}); drei Ticks spaeter muss
     * genau der eigene Zaehler um eins gestiegen sein und kein anderer. So faellt jeder Knopf auf, der
     * einen zweiten Befehlsblock ausloest (auch ueber Umwege, die der statische Test nicht kennt), einen
     * doppelt oder gar keinen.
     */
    public static void eachButtonRunsExactlyItsOwnCommandBlock(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = new BlockPos(26000, helper.absolutePos(BlockPos.ZERO).getY() + 1, 26000);
        TestCentreLayout.Plan plan = TestCentreLayout.plan(level.registryAccess(), origin);
        BoundingBox bounds = plan.bounds();
        List<int[]> forced = new ArrayList<>();
        for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++) {
            for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                if (level.setChunkForced(cx, cz, true)) {
                    forced.add(new int[]{cx, cz});
                }
            }
        }
        Runnable cleanup = () -> {
            TestCentreBuilder.clear(level, bounds);
            for (int[] chunk : forced) {
                level.setChunkForced(chunk[0], chunk[1], false);
            }
        };
        List<TcOp.Command> commands = commands(plan);
        try {
            TestCentreBuilder.build(level, plan);
            helper.assertTrue(commands.size() > 20, "suspiciously few command blocks: " + commands.size());
            for (TcOp.Command command : commands) {
                helper.assertTrue(level.getBlockEntity(command.pos()) instanceof CommandBlockEntity,
                        "no command block at " + command.pos().toShortString());
                BlockPos counter = counter(command);
                ((CommandBlockEntity) level.getBlockEntity(command.pos())).getCommandBlock().setCommand(
                        "summon minecraft:marker " + counter.getX() + ".5 " + counter.getY() + ".5 " + counter.getZ() + ".5");
            }
        } catch (RuntimeException e) {
            cleanup.run();
            throw e;
        }
        for (int i = 0; i < commands.size(); i++) {
            int index = i;
            TcOp.Command pressed = commands.get(i);
            BlockPos buttonPos = pressed.pos().relative(pressed.facing());
            helper.runAfterDelay(FIRST_PRESS + (long) PRESS_STEP * i, guarded(cleanup, () -> {
                BlockState button = level.getBlockState(buttonPos);
                helper.assertTrue(button.getBlock() instanceof ButtonBlock,
                        "no button in front of '" + pressed.command() + "' at " + pressed.pos().toShortString());
                ((ButtonBlock) button.getBlock()).press(button, level, buttonPos, null);
            }));
            helper.runAfterDelay(FIRST_PRESS + (long) PRESS_STEP * i + CHECK_AFTER, guarded(cleanup, () -> {
                List<String> wrong = new ArrayList<>();
                for (int j = 0; j < commands.size(); j++) {
                    int runs = level.getEntitiesOfClass(Marker.class, new AABB(counter(commands.get(j)))).size();
                    int expected = j <= index ? 1 : 0;
                    if (runs != expected) {
                        wrong.add("'" + commands.get(j).command() + "' ran " + runs + "x");
                    }
                }
                helper.assertTrue(wrong.isEmpty(), "after pressing the button of '" + pressed.command() + "': " + wrong);
                // Loslassen wie der geplante Tick des Knopfs: Zustand zurueck, Traeger benachrichtigen.
                BlockState button = level.getBlockState(buttonPos);
                level.setBlock(buttonPos, button.setValue(ButtonBlock.POWERED, false), Block.UPDATE_ALL);
                level.updateNeighborsAt(pressed.pos(), button.getBlock());
            }));
        }
        helper.runAfterDelay(FIRST_PRESS + (long) PRESS_STEP * commands.size() + 1, () -> {
            cleanup.run();
            helper.succeed();
        });
    }

    /** Wo der Zaehler eines Befehlsblocks landet: drei Bloecke ueber ihm (Luft; jede Stelle nur einmal). */
    private static BlockPos counter(TcOp.Command command) {
        return command.pos().above(3);
    }

    /** Fuehrt einen Schritt aus und raeumt die Zentrale ab, wenn er scheitert. */
    private static Runnable guarded(Runnable cleanup, Runnable step) {
        return () -> {
            try {
                step.run();
            } catch (RuntimeException e) {
                cleanup.run();
                throw e;
            }
        };
    }
}
