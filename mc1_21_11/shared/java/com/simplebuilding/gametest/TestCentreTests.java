package com.simplebuilding.gametest;

import com.simplebuilding.blueprint.BlueprintContent;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.dev.testcentre.TcOp;
import com.simplebuilding.dev.testcentre.TestCentreBuilder;
import com.simplebuilding.dev.testcentre.TestCentreLayout;
import com.simplebuilding.util.OctantShape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
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
                        && !sign.getFrontText().getMessage(0, false).getString().isEmpty(), "no labelled sign at " + pos);
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
}
