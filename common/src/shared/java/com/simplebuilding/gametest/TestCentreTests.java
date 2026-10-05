package com.simplebuilding.gametest;

import com.simplebuilding.blueprint.BlueprintContent;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.dev.testcentre.FeatureStations;
import com.simplebuilding.dev.testcentre.TabBrowser;
import com.simplebuilding.dev.testcentre.TcContext;
import com.simplebuilding.dev.testcentre.TcOp;
import com.simplebuilding.dev.testcentre.TestCentreBuilder;
import com.simplebuilding.dev.testcentre.TestCentreCommand;
import com.simplebuilding.dev.testcentre.TestCentreKits;
import com.simplebuilding.dev.testcentre.TestCentreLayout;
import com.simplebuilding.items.ModItemGroupsContent;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.tweaks.item.TweaksItems;
import com.simplebuilding.tweaks.block.FlypadBlock;
import com.simplebuilding.tweaks.block.PadBlock;
import com.simplebuilding.tweaks.block.PadTiers;
import com.simplebuilding.util.OctantShape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

    /** Regressions: unloaded-origin minY, invalid old marker, falling join and respawn scatter. */
    public static void freshWorldOriginAndEntranceAreSafe(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.HUB_TEST_WORLD) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        var marker = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("simplebuilding_testcentre.txt");
        var spawn = level.getRespawnData();
        var rules = level.getGameRules();
        int radius = rules.get(net.minecraft.world.level.gamerules.GameRules.RESPAWN_RADIUS);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> level.getServer().getPlayerList().remove(player));
        byte[] before;
        try {
            before = java.nio.file.Files.exists(marker) ? java.nio.file.Files.readAllBytes(marker) : null;
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        try {
            java.nio.file.Files.deleteIfExists(marker);
            BlockPos fresh = TestCentreCommand.savedOrDefaultOrigin(level);
            helper.assertTrue(fresh.getY() > level.getMinY(), "fresh origin must leave room for a floor");
            helper.assertTrue(level.hasChunk(0, 0), "origin chunk must be loaded before reading its height");
            helper.assertTrue(fresh.getY() == Math.max(level.getMinY() + 1,
                    level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0)),
                    "fresh origin must use the generated terrain height");
            java.nio.file.Files.writeString(marker, "0 " + level.getMinY() + " 0 old-hash\n");
            helper.assertTrue(TestCentreCommand.savedOrDefaultOrigin(level).equals(fresh), "repair old minY origin");
            BlockPos origin = helper.absolutePos(new BlockPos(2, 3, 2));
            java.nio.file.Files.writeString(marker, origin.getX() + " " + origin.getY() + " " + origin.getZ() + "\n");
            helper.assertTrue(TestCentreCommand.savedOrDefaultOrigin(level).equals(origin), "rebuild must retain its origin");
            var plan = TestCentreLayout.plan(level.registryAccess(), origin);
            BlockPos entrance = plan.entrance();
            for (int attempt = 0; attempt < 2; attempt++) {
                level.setBlockAndUpdate(entrance.below(), Blocks.AIR.defaultBlockState());
                player.snapTo(entrance.getX(), level.getMinY() - 5, entrance.getZ(), 0, 0);
                player.setDeltaMovement(0, -2, 0);
                player.fallDistance = 50;
                TestCentreCommand.arriveAtEntrance(level, plan, player);
                helper.assertTrue(player.blockPosition().equals(entrance), "join/rebuild must teleport to entrance");
                helper.assertTrue(level.getBlockState(entrance.below()).isSolid(), "entrance needs a real floor");
                helper.assertTrue(level.noCollision(player), "entrance must have clear headroom");
                helper.assertTrue(player.getDeltaMovement().lengthSqr() == 0 && player.fallDistance == 0,
                        "landing must clear falling motion and damage");
                helper.assertTrue(level.getRespawnData().pos().equals(entrance), "world spawn must be the entrance");
                helper.assertTrue(rules.get(net.minecraft.world.level.gamerules.GameRules.RESPAWN_RADIUS) == 0,
                        "respawn must not scatter players off the platform");
            }
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        } finally {
            level.setRespawnData(spawn);
            rules.set(net.minecraft.world.level.gamerules.GameRules.RESPAWN_RADIUS, radius, level.getServer());
            try {
                if (before == null) java.nio.file.Files.deleteIfExists(marker);
                else java.nio.file.Files.write(marker, before);
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }
        helper.succeed();
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
            helper.assertTrue(!level.getBlockState(plan.entrance().below()).isAir(), "built entrance must have a floor");
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
                    if (op instanceof TcOp.Hammock hammock) {
                        var spot = new com.simplebuilding.blocks.custom.HammockLayout.Spot(
                                hammock.pos(), hammock.dx(), hammock.dz());
                        helper.assertTrue(com.simplebuilding.blocks.custom.HammockLayout.intact(level, spot.clothHead()),
                                "built hammock is not linked or its anchor is invalid: " + spot);
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
            // Eine Station ohne Kit ist nur erlaubt, wenn sie nichts zeigt (etwa "devices", sobald jede
            // Tab-Zeile eine eigene Station hat, oder ein leeres "unsorted"): dann gibt es keinen Knopf.
            if (kit == null) {
                if (!TestCentreKits.of(new TcContext(helper.getLevel().registryAccess()), id, section.ops()).isEmpty()) {
                    stations.add(id + " shows items but has no kit");
                }
                if (section.ops().stream().anyMatch(op -> op instanceof TcOp.Command command
                        && command.command().equals(TestCentreLayout.giveCommand(id)))) {
                    stations.add(id + " has nothing to give but a give button");
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
        for (String withOffHand : List.of("tools", "chisel", "inworld", "templates", "planning", "states")) {
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

    // ------------------------------------------------------------------ Teststationen (B12/P10)

    /** Die Rahmen eines Abschnitts nach Lage, in Planungsreihenfolge. */
    static Map<BlockPos, ItemStack> frames(TestCentreLayout.Section section) {
        Map<BlockPos, ItemStack> out = new LinkedHashMap<>();
        for (TcOp op : section.ops()) {
            if (op instanceof TcOp.Frame frame) {
                out.put(frame.pos(), frame.stack());
            }
        }
        return out;
    }

    /**
     * Station "states": kaputt neben heil. Resonanzstab und Rotator leer, halb und voll; das Echolot in
     * allen drei Riss-Stufen seines Modells und repariert; jedes abnutzbare Werkzeug und Ruestungsteil der
     * Haltbarkeits-Zeilen fast verbraucht mit dem neuen Stueck direkt daneben; rissiger und heiler
     * Diamantblock gesetzt, Amboss, Hochofen mit rissigen Diamanten, abgenutzter und neuer Ruestungsstaender.
     */
    public static void brokenAndRepairedStatesStandSideBySide(GameTestHelper helper) {
        TestCentreLayout.Plan plan = TestCentreLayout.plan(helper.getLevel().registryAccess(), BlockPos.ZERO);
        TcContext ctx = new TcContext(helper.getLevel().registryAccess());
        TestCentreLayout.Section states = plan.section("states");
        Map<BlockPos, ItemStack> frames = frames(states);
        List<String> problems = new ArrayList<>();
        for (Item gadget : List.of(TweaksItems.LASER_POINTER, ModItems.ROTATOR)) {
            boolean empty = frames.values().stream().anyMatch(s -> s.is(gadget) && s.getDamageValue() == s.getMaxDamage());
            boolean half = frames.values().stream().anyMatch(s -> s.is(gadget) && s.getDamageValue() > 0 && s.getDamageValue() < s.getMaxDamage());
            boolean full = frames.values().stream().anyMatch(s -> s.is(gadget) && s.getDamageValue() == 0);
            if (!empty || !half || !full) {
                problems.add(BuiltInRegistries.ITEM.getKey(gadget) + " lacks a state (empty " + empty + ", half " + half + ", full " + full + ")");
            }
        }
        Set<Integer> crackStages = new HashSet<>();
        boolean repaired = false;
        for (ItemStack stack : frames.values()) {
            if (stack.is(TweaksItems.ECHO_COMPASS)) {
                if (stack.isDamaged()) {
                    // Wie das Item-Modell: normierter Schaden, Schwellen 0, 1/3, 2/3.
                    crackStages.add(Math.min(2, (int) (3.0F * stack.getDamageValue() / stack.getMaxDamage())));
                } else {
                    repaired = true;
                }
            }
        }
        if (crackStages.size() != 3 || !repaired) {
            problems.add("echo sounder: crack stages " + crackStages + " of 3, repaired " + repaired);
        }
        int pairs = 0;
        for (String row : FeatureStations.DURABILITY_ROWS) {
            for (ItemStack stack : ctx.row(row)) {
                if (!stack.isDamageableItem() || FeatureStations.hasOwnStateLine(stack.getItem())) {
                    continue;
                }
                boolean found = false;
                for (Map.Entry<BlockPos, ItemStack> entry : frames.entrySet()) {
                    ItemStack worn = entry.getValue();
                    ItemStack partner = frames.get(entry.getKey().east());
                    if (worn.is(stack.getItem()) && worn.getDamageValue() == worn.getMaxDamage() - 1
                            && partner != null && partner.is(stack.getItem()) && partner.getDamageValue() == 0) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    problems.add(BuiltInRegistries.ITEM.getKey(stack.getItem()) + " (" + row + ") has no worn frame next to a new one");
                }
                pairs++;
            }
        }
        Set<Block> placed = new HashSet<>();
        for (TcOp op : states.ops()) {
            if (op instanceof TcOp.Place place) {
                placed.add(place.state().getBlock());
            }
        }
        for (Block block : List.of(ModBlocks.CRACKED_DIAMOND_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.ANVIL, Blocks.BLAST_FURNACE)) {
            if (!placed.contains(block)) {
                problems.add("states does not place " + BuiltInRegistries.BLOCK.getKey(block));
            }
        }
        boolean blasting = states.ops().stream().anyMatch(op -> op instanceof TcOp.Fill fill
                && fill.contents().stream().anyMatch(s -> s.is(ModItems.CRACKED_DIAMOND)));
        if (!blasting) {
            problems.add("the blast furnace holds no cracked diamonds");
        }
        long wornStands = states.ops().stream().filter(op -> op instanceof TcOp.Stand stand
                && stand.gear().stream().anyMatch(ItemStack::isDamaged)).count();
        long newStands = states.ops().stream().filter(op -> op instanceof TcOp.Stand stand
                && stand.gear().stream().noneMatch(ItemStack::isDamaged)).count();
        if (wornStands < 1 || newStands < 1) {
            problems.add("armour stands: worn " + wornStands + ", new " + newStands);
        }
        helper.assertTrue(pairs > 40, "suspiciously few durability pairs: " + pairs);
        helper.assertTrue(problems.isEmpty(), problems.size() + " state problem(s): " + String.join("; ", problems));
        helper.succeed();
    }

    /**
     * Item-orientierter Rundgang: je Kreativ-Tab der Mod ein Abschnitt {@code tab_<id>}, dessen Rahmen
     * genau den Tab zeigen (gleiche Stapel, gleiche Reihenfolge), mit Ausgabe-Knopf. Die Waende zaehlen
     * nicht fuer die Abdeckung: jedes Item braucht trotzdem eine Station.
     */
    public static void everyCreativeTabHasItsItemBrowserWall(GameTestHelper helper) {
        TestCentreLayout.Plan plan = TestCentreLayout.plan(helper.getLevel().registryAccess(), BlockPos.ZERO);
        TcContext ctx = new TcContext(helper.getLevel().registryAccess());
        List<String> problems = new ArrayList<>();
        for (ModItemGroupsContent.Tab tab : ModItemGroupsContent.Tab.values()) {
            String id = TabBrowser.sectionId(tab);
            if (!TestCentreLayout.SECTION_IDS.contains(id)) {
                problems.add(id + " is missing from SECTION_IDS");
                continue;
            }
            List<ItemStack> expected = ctx.tab(tab);
            List<ItemStack> shown = new ArrayList<>(frames(plan.section(id)).values());
            if (shown.size() != expected.size()) {
                problems.add(id + " frames " + shown.size() + " stacks, the tab holds " + expected.size());
                continue;
            }
            for (int i = 0; i < expected.size(); i++) {
                if (!ItemStack.isSameItemSameComponents(shown.get(i), expected.get(i))) {
                    problems.add(id + " frame " + i + " shows " + shown.get(i) + " instead of " + expected.get(i));
                    break;
                }
            }
            if (!expected.isEmpty() && plan.kits().get(id) == null) {
                problems.add(id + " has no give button");
            }
        }
        helper.assertTrue(!ctx.tab(ModItemGroupsContent.Tab.TOOLS).isEmpty(), "the tools tab is empty");
        helper.assertTrue(problems.isEmpty(), problems.size() + " item browser problem(s): " + String.join("; ", problems));
        helper.succeed();
    }

    /**
     * Testorientierter Rundgang: jede Mod-Verzauberung steht auf einem Gegenstand (kein Buch) in einer
     * Station zum Ausprobieren; jedes Kleinteil und jedes Ei liegt abgelegt in "placeables"; jeder
     * Eisenstab steht unter freiem Himmel (nichts darueber, kein Kupfer-Blitzableiter, der ihm die Blitze
     * nimmt) und die Steuerwand hat einen Gewitter-Knopf; der Fallturm des Sinkdaempfers ist hoch genug,
     * seine Leiter reicht bis oben, und ein Staender traegt den vollen Enderit-Satz.
     */
    public static void everyFeatureStationSetsUpItsScenario(GameTestHelper helper) {
        BlockPos origin = new BlockPos(0, 64, 0);
        TestCentreLayout.Plan plan = TestCentreLayout.plan(helper.getLevel().registryAccess(), origin);
        TcContext ctx = new TcContext(helper.getLevel().registryAccess());
        List<String> problems = new ArrayList<>();
        if (!ModBlocks.HAMMOCKS.isEmpty()) {
            assertHammockExhibits(helper, plan);
        }

        // Verzauberungen.
        List<ItemStack> handsOn = new ArrayList<>();
        for (String id : List.of("mining", "planning", "chisel", "enchants")) {
            handsOn.addAll(frames(plan.section(id)).values());
        }
        ctx.enchantmentLookup().listElements().forEach(ref -> {
            if (!TcContext.isMod(ref.key().identifier())) {
                return;
            }
            boolean shown = handsOn.stream().anyMatch(stack -> !stack.is(Items.ENCHANTED_BOOK)
                    && stack.getEnchantments().getLevel(ref) > 0);
            if (!shown) {
                problems.add("enchantment " + ref.key().identifier() + " has no hands-on station (only a book)");
            }
        });

        // Kleinteile und Eier.
        TestCentreLayout.Section placeables = plan.section("placeables");
        if (com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            List<Item> parts = FeatureStations.smallParts();
            helper.assertTrue(parts.contains(ModItems.STONE_PEBBLE), "the small parts lack the stone pebble: " + parts);
            for (Item part : parts) {
                boolean laid = placeables.ops().stream().anyMatch(op -> op instanceof TcOp.Fill fill
                        && !fill.contents().isEmpty() && fill.contents().getFirst().is(part));
                if (!laid) {
                    problems.add("small part " + BuiltInRegistries.ITEM.getKey(part) + " is not laid down in placeables");
                }
            }
            Set<Object> eggs = new HashSet<>();
            for (TcOp op : placeables.ops()) {
                if (op instanceof TcOp.Place place && place.state().getBlock() == ModBlocks.PLACED_EGG) {
                    eggs.add(place.state().getValue(com.simplebuilding.blocks.custom.PlacedEggBlock.EGG));
                }
            }
            if (eggs.size() != com.simplebuilding.blocks.custom.PlacedEggBlock.Egg.values().length) {
                problems.add("placeables stands up " + eggs.size() + " egg kinds of "
                        + com.simplebuilding.blocks.custom.PlacedEggBlock.Egg.values().length);
            }
        }

        // Metallstaebe (Eisen, Gold, Netherit, Enderit) unter freiem Himmel.
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (TestCentreLayout.Section section : plan.sections()) {
            for (TcOp op : section.ops()) {
                if (op instanceof TcOp.Place place) {
                    blocks.put(place.pos(), place.state());
                }
            }
        }
        int rods = 0;
        Set<Object> metalRods = new HashSet<>();
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            if (entry.getValue().getBlock() instanceof net.minecraft.world.level.block.LightningRodBlock
                    && !(entry.getValue().getBlock() instanceof com.simplebuilding.blocks.custom.MetalRodBlock)) {
                problems.add("a copper lightning rod at " + entry.getKey().toShortString() + " would take the iron rods' lightning");
            }
            if (!(entry.getValue().getBlock() instanceof com.simplebuilding.blocks.custom.MetalRodBlock)) {
                continue;
            }
            metalRods.add(entry.getValue().getBlock());
            if (entry.getValue().is(ModBlocks.IRON_ROD)) {
                rods++;
            }
            for (Map.Entry<BlockPos, BlockState> other : blocks.entrySet()) {
                BlockPos pos = other.getKey();
                if (pos.getX() == entry.getKey().getX() && pos.getZ() == entry.getKey().getZ() && pos.getY() > entry.getKey().getY()
                        && !other.getValue().isAir()) {
                    problems.add("the metal rod " + entry.getValue().getBlock() + " at " + entry.getKey().toShortString() + " has " + other.getValue() + " above it");
                }
            }
        }
        if (ModBlocks.IRON_ROD != null && rods == 0) {
            problems.add("no iron rod stands anywhere");
        }
        if (ModBlocks.IRON_ROD != null && !metalRods.containsAll(List.of(ModBlocks.GOLD_ROD, ModBlocks.NETHERITE_ROD, ModBlocks.ENDERITE_ROD))) {
            problems.add("not every metal rod stands in the test centre: " + metalRods);
        }
        if (commands(plan).stream().noneMatch(command -> command.command().equals("weather thunder"))) {
            problems.add("the controls have no thunder button");
        }

        // Fallturm.
        TestCentreLayout.Section tower = plan.section("sinkdamper");
        BlockPos top = plan.anchors().get("sink_tower_top");
        if (top == null || top.getY() - origin.getY() < 10) {
            problems.add("the sink damper tower is missing or too low: " + top);
        } else {
            long ladders = tower.ops().stream().filter(op -> op instanceof TcOp.Place place && place.state().is(Blocks.LADDER)).count();
            if (ladders < top.getY() - origin.getY()) {
                problems.add("the tower ladder has " + ladders + " rungs for " + (top.getY() - origin.getY()) + " blocks");
            }
            BlockState below = blocks.get(top.below());
            if (below == null || below.isAir()) {
                problems.add("the tower top has no platform");
            }
        }
        boolean suit = tower.ops().stream().anyMatch(op -> op instanceof TcOp.Stand stand && stand.gear().stream()
                .filter(com.simplebuilding.util.EnderiteSinkDamper::isEnderiteArmor).count() == 4);
        if (!suit) {
            problems.add("no armour stand wears the full enderite set at the tower");
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " station problem(s): " + String.join("; ", problems));
        helper.succeed();
    }

    /** Nach der ersten Ausfuehrung noch weitere Ticks auf doppelte/fremde Ausfuehrungen pruefen. */
    static final int CHECK_AFTER = 3;
    /** Every requested layout has its own labeled, disjoint bay; all colors and anchor types appear. */
    private static void assertHammockExhibits(GameTestHelper helper, TestCentreLayout.Plan plan) {
        var machines = plan.section("machines");
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        Map<BlockPos, Integer> writes = new HashMap<>();
        Map<BlockPos, TcOp.Sign> signs = new HashMap<>();
        for (var section : plan.sections()) {
            if (!section.id().equals("machines")) {
                helper.assertTrue(!machines.box(plan.origin()).intersects(section.box(plan.origin())),
                        "machines overlap section " + section.id());
            }
            for (TcOp op : section.ops()) {
                if (op instanceof TcOp.Place place) {
                    blocks.put(place.pos(), place.state());
                    writes.merge(place.pos(), 1, Integer::sum);
                }
                if (op instanceof TcOp.Sign sign) signs.put(sign.pos(), sign);
            }
        }
        var spans = new HashSet<String>();
        var colors = new HashSet<Block>();
        var anchors = new HashSet<BlockState>();
        var occupied = new HashSet<BlockPos>();
        int count = 0;
        for (TcOp op : machines.ops()) {
            if (!(op instanceof TcOp.Hammock hammock)) continue;
            count++;
            var spot = new com.simplebuilding.blocks.custom.HammockLayout.Spot(hammock.pos(), hammock.dx(), hammock.dz());
            helper.assertTrue(spot.valid(), "invalid exhibit span " + spot);
            String span = hammock.dx() + ":" + hammock.dz();
            spans.add(span);
            colors.add(blocks.get(spot.clothHead()).getBlock());
            var cells = new ArrayList<>(spot.cells());
            for (BlockPos anchor : List.of(spot.anchor(), spot.otherAnchor())) {
                BlockState post = blocks.get(anchor);
                anchors.add(post);
                helper.assertTrue(post != null && (post.is(ModBlocks.NETHERITE_ROD)
                                ? Blocks.OAK_LOG.defaultBlockState() : post).equals(blocks.get(anchor.below())),
                        "exhibit post is not two blocks tall: " + anchor);
                cells.add(anchor);
                cells.add(anchor.below());
            }
            for (BlockPos cell : cells) {
                helper.assertTrue(occupied.add(cell) && writes.getOrDefault(cell, 0) == 1 && !signs.containsKey(cell),
                        "hammock exhibit overlaps another placement at " + cell);
                helper.assertTrue(machines.box(plan.origin()).isInside(cell), "exhibit outside machines: " + cell);
            }
            BlockPos label = new BlockPos(Math.min(spot.anchor().getX(), spot.otherAnchor().getX()),
                    spot.anchor().getY() - 1, spot.anchor().getZ() - 1);
            TcOp.Sign sign = signs.get(label);
            helper.assertTrue(sign != null && sign.lines().size() == 4 && sign.lines().getFirst().getString().equals(span)
                            && sign.lines().stream().noneMatch(line -> line.getString().isBlank()),
                    "missing four-line hammock label for " + spot);
        }
        helper.assertTrue(count == 22, "expected 22 hammock exhibits, got " + count);
        helper.assertTrue(spans.containsAll(List.of("3:0", "4:0", "5:0", "0:3", "0:4", "0:5",
                "3:3", "4:4", "5:5", "3:1", "4:1", "5:2", "3:2", "4:3", "-3:2")),
                "missing hammock lengths, axes or angles: " + spans);
        helper.assertTrue(colors.containsAll(ModBlocks.HAMMOCKS), "not all hammock colors are displayed");
        helper.assertTrue(anchors.contains(Blocks.OAK_FENCE.defaultBlockState())
                        && anchors.contains(Blocks.OAK_LOG.defaultBlockState())
                        && anchors.contains(ModBlocks.NETHERITE_ROD.defaultBlockState()
                                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.UP)),
                "missing fence, log or netherite anchor");
        for (var rod : com.simplebuilding.blocks.custom.StandingRodBlock.Rod.values()) {
            helper.assertTrue(anchors.contains(ModBlocks.STANDING_ROD.defaultBlockState()
                            .setValue(com.simplebuilding.blocks.custom.StandingRodBlock.ROD, rod)),
                    "missing standing rod anchor: " + rod);
        }
    }

    // Serial execution needs a command tick, three observation ticks and a release tick
    // per button, plus asynchronous loading of the remote centre. The old 600 covered
    // only the fixed four-tick schedule and could expire before the final buttons.
    public static final int BUTTON_RUN_MAX_TICKS = 1200;

    /**
     * Die Zentrale steht wirklich (fern aller Tests, Chunks erzwungen), jeder Befehlsblock bekommt statt
     * seines Befehls einen eigenen Scoreboard-Zaehler. Dann wird jeder
     * Knopf der Reihe nach gedrueckt wie von Hand ({@link ButtonBlock#press}); nach seiner Ausfuehrung muss
     * genau der eigene Zaehler um eins gestiegen sein und kein anderer. So faellt jeder Knopf auf, der
     * einen zweiten Befehlsblock ausloest (auch ueber Umwege, die der statische Test nicht kennt), einen
     * doppelt oder gar keinen.
     */
    public static void eachButtonRunsExactlyItsOwnCommandBlock(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = new BlockPos(26000, helper.absolutePos(BlockPos.ZERO).getY() + 1, 26000);
        TestCentreLayout.Plan plan = TestCentreLayout.plan(level.registryAccess(), origin);
        BoundingBox bounds = plan.bounds();
        Scoreboard scoreboard = level.getScoreboard();
        String objectiveName = "sb_tc_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        Objective objective = scoreboard.addObjective(objectiveName, ObjectiveCriteria.DUMMY,
                Component.literal(objectiveName), ObjectiveCriteria.RenderType.INTEGER, false, null);
        List<int[]> forced = new ArrayList<>();
        for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++) {
            for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                if (level.setChunkForced(cx, cz, true)) {
                    forced.add(new int[]{cx, cz});
                }
                level.getChunk(cx, cz);
            }
        }
        Runnable cleanup = () -> {
            TestCentreBuilder.clear(level, bounds);
            level.getBlockTicks().clearArea(bounds);
            level.getFluidTicks().clearArea(bounds);
            for (int[] chunk : forced) {
                level.setChunkForced(chunk[0], chunk[1], false);
            }
            scoreboard.removeObjective(objective);
        };
        helper.runBeforeTestEnd(cleanup);
        List<TcOp.Command> commands = commands(plan);
        // Wait until forced tickets take effect before writing the structure. Each command
        // then waits for its actual execution, including any pending chunk/entity loading.
        var sequence = helper.startSequence().thenWaitUntil(() -> {
            for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++) {
                for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                    helper.assertTrue(level.shouldTickBlocksAt(
                                    new net.minecraft.world.level.ChunkPos(cx, cz).pack()),
                            "test centre forced ticket is not active: " + cx + ", " + cz);
                }
            }
        }).thenExecute(cleanupOnFailure(cleanup, () -> {
            // This fixed remote area is outside the GameTest structure's automatic reset.
            // Pending ticks from a previous run can suppress a newly scheduled tick at the
            // same position, even after its block and block entity have been replaced.
            level.getBlockTicks().clearArea(bounds);
            level.getFluidTicks().clearArea(bounds);
            TestCentreBuilder.build(level, plan);
            helper.assertTrue(commands.size() > 20, "suspiciously few command blocks: " + commands.size());
            for (int i = 0; i < commands.size(); i++) {
                TcOp.Command command = commands.get(i);
                helper.assertTrue(level.getBlockEntity(command.pos()) instanceof CommandBlockEntity,
                        "no command block at " + command.pos().toShortString());
                ((CommandBlockEntity) level.getBlockEntity(command.pos())).getCommandBlock().setCommand(
                        // A marker count also depends on entity-section visibility. Scoreboard
                        // increments measure every command execution without that extra clock.
                        "scoreboard players add button_" + i + " " + objectiveName + " 1");
            }
        }));
        for (int i = 0; i < commands.size(); i++) {
            int index = i;
            TcOp.Command pressed = commands.get(i);
            BlockPos buttonPos = pressed.pos().relative(pressed.facing());
            String[] waitingState = {"not pressed"};
            sequence.thenExecute(cleanupOnFailure(cleanup, () -> {
                BlockState button = level.getBlockState(buttonPos);
                helper.assertTrue(button.getBlock() instanceof ButtonBlock,
                        "no button in front of '" + pressed.command() + "' at " + pressed.pos().toShortString() + " but " + button);
                helper.assertTrue(!button.getValue(ButtonBlock.POWERED), "button was already powered: " + pressed.command());
                CommandBlockEntity entity = (CommandBlockEntity) level.getBlockEntity(pressed.pos());
                helper.assertValueEqual(entity.getCommandBlock().getCommand(),
                        "scoreboard players add button_" + index + " " + objectiveName + " 1",
                        "counter command changed before pressing " + pressed.command());
                helper.assertTrue(!entity.isPowered(), "command block was already powered: " + pressed.command());
                ((ButtonBlock) button.getBlock()).press(button, level, buttonPos, null);
                helper.assertTrue(entity.isPowered(), "button did not power its command block: " + pressed.command());
                helper.assertTrue(entity.wasConditionMet(), "command block condition was not met: " + pressed.command());
            })).thenWaitUntil(() -> {
                if (level.getBlockEntity(pressed.pos()) instanceof CommandBlockEntity entity) {
                    waitingState[0] = entity.getCommandBlock().getCommand() + "; powered=" + entity.isPowered()
                            + "; scheduled=" + level.getBlockTicks().hasScheduledTick(pressed.pos(), Blocks.COMMAND_BLOCK)
                            + "; ready=" + level.isPositionTickingWithEntitiesLoaded(
                                    net.minecraft.world.level.ChunkPos.pack(pressed.pos()));
                }
                helper.assertTrue(commandRuns(scoreboard, objective, index) > 0,
                        "waiting for command block " + (index + 1) + "/" + commands.size()
                                + " after pressing '" + pressed.command() + "': " + waitingState[0]);
            })
                    .thenExecuteAfter(CHECK_AFTER, cleanupOnFailure(cleanup, () -> {
                List<String> wrong = new ArrayList<>();
                for (int j = 0; j < commands.size(); j++) {
                    int runs = commandRuns(scoreboard, objective, j);
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
            })).thenExecuteAfter(1, () -> {});
        }
        sequence.thenExecute(cleanup).thenSucceed();
    }

    private static int commandRuns(Scoreboard scoreboard, Objective objective, int index) {
        var score = scoreboard.getPlayerScoreInfo(ScoreHolder.forNameOnly("button_" + index), objective);
        return score == null ? 0 : score.value();
    }

    private static Runnable cleanupOnFailure(Runnable cleanup, Runnable action) {
        return () -> {
            try {
                action.run();
            } catch (RuntimeException failure) {
                cleanup.run();
                throw failure;
            }
        };
    }
}
