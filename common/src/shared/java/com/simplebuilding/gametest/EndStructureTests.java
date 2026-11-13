package com.simplebuilding.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** End structures (Queue N23): well, fake gateway, shipwrecks, path as datapack jigsaw structures. */
public final class EndStructureTests {
    private static final String[] STRUCTURES = {"end_well", "end_fake_gateway", "end_wreck", "end_path"};
    private static final String[] TEMPLATES = {"well_intact", "well_broken_a", "well_broken_b", "gateway_a", "gateway_b", "gateway_c",
            "wreck_bow_none", "wreck_bow_empty", "wreck_bow_elytra", "wreck_stern_none", "wreck_stern_empty", "wreck_stern_elytra",
            "wreck_broken_none", "wreck_broken_empty", "wreck_broken_elytra", "wreck_tilt_none", "wreck_tilt_empty", "wreck_tilt_elytra",
            "path_straight", "path_bend", "path_tee", "path_cross", "path_wave"};

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("simplebuilding", path);
    }

    private static boolean active(GameTestHelper helper) {
        if (com.simplebuilding.version.McVersion.END_STRUCTURES) return true;
        helper.succeed();
        return false;
    }

    public static void structuresAreRegistered(GameTestHelper helper) {
        if (!active(helper)) return;
        var regs = helper.getLevel().registryAccess();
        for (String s : STRUCTURES) {
            helper.assertTrue(regs.lookupOrThrow(Registries.STRUCTURE).get(ResourceKey.create(Registries.STRUCTURE, id(s))).isPresent(), "structure missing: " + s);
            helper.assertTrue(regs.lookupOrThrow(Registries.STRUCTURE_SET).get(ResourceKey.create(Registries.STRUCTURE_SET, id(s))).isPresent(), "structure set missing: " + s);
            helper.assertTrue(regs.lookupOrThrow(Registries.TEMPLATE_POOL).get(ResourceKey.create(Registries.TEMPLATE_POOL, id(s))).isPresent(), "template pool missing: " + s);
        }
        var tag = regs.lookupOrThrow(Registries.BIOME).get(TagKey.<Biome>create(Registries.BIOME, id("has_structure/end_outer_islands")));
        helper.assertTrue(tag.isPresent() && tag.get().size() == 4, "end island biome tag must list the four outer-island biomes");
        helper.assertTrue(helper.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id("chests/end_wreck"))) != null,
                "wreck loot table missing");
        helper.succeed();
    }

    public static void templatesLoad(GameTestHelper helper) {
        if (!active(helper)) return;
        var manager = templates(helper);
        for (String t : TEMPLATES) {
            var template = manager.get(id("end/" + t));
            helper.assertTrue(template.isPresent(), "template missing: " + t);
            helper.assertTrue(template.get().getSize().getY() >= 1 && template.get().getSize().getX() >= 3, "template empty: " + t);
        }
        // N66: wrecks use the End ship palette only (no wool, no cloth); wells use End rods, not torches
        java.util.Set<net.minecraft.world.level.block.Block> shipPalette = java.util.Set.of(Blocks.AIR, Blocks.CHEST, Blocks.END_ROD, Blocks.END_STONE,
                Blocks.END_STONE_BRICKS, Blocks.END_STONE_BRICK_WALL, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("magenta_stained_glass")), Blocks.OBSIDIAN, Blocks.PURPUR_BLOCK,
                Blocks.PURPUR_PILLAR, Blocks.PURPUR_SLAB, Blocks.PURPUR_STAIRS);
        var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
        for (String t : TEMPLATES) {
            var template = manager.get(id("end/" + t)).get();
            for (net.minecraft.world.level.block.Block block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
                if (t.startsWith("wreck_") && !shipPalette.contains(block))
                    helper.assertTrue(template.filterBlocks(BlockPos.ZERO, settings, block).isEmpty(), "wreck " + t + " holds a block outside the End ship palette: " + block);
                if (t.startsWith("well_") && (block == Blocks.TORCH || block == Blocks.WALL_TORCH || block == Blocks.SOUL_TORCH || block == Blocks.SOUL_WALL_TORCH))
                    helper.assertTrue(template.filterBlocks(BlockPos.ZERO, settings, block).isEmpty(), "well " + t + " still has torches");
            }
        }
        helper.assertTrue(!manager.get(id("end/well_intact")).get().filterBlocks(BlockPos.ZERO, settings, Blocks.END_ROD).isEmpty(), "intact well has End rods");
        helper.succeed();
    }

    /** N66: every wreck chest holds a solid base and one valuable item; none is junk any more. */
    public static void wreckLootIsNeverJunk(GameTestHelper helper) {
        if (!active(helper)) return;
        var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id("chests/end_wreck")));
        LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .create(LootContextParamSets.CHEST);
        for (int seed = 0; seed < 400; seed++) {
            int stacks = 0;
            boolean good = false;
            for (ItemStack st : table.getRandomItems(params, 7000L + seed)) {
                stacks++;
                if (st.is(Items.DIAMOND) || st.is(Items.EMERALD) || st.is(Items.ENDER_PEARL) || st.is(Items.ENCHANTED_BOOK)) good = true;
            }
            helper.assertTrue(stacks >= 4, "a wreck chest roll holds at least 4 stacks, seed " + seed + " had " + stacks);
            helper.assertTrue(good, "a wreck chest roll always holds a valuable item (diamond, emerald, pearl or enchanted book), seed " + seed);
        }
        helper.succeed();
    }

    private static void place(GameTestHelper helper, String template, BlockPos pos) {
        MinecraftServer server = helper.getLevel().getServer();
        String cmd = "place template " + id("end/" + template) + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
        try {
            int r = server.getCommands().getDispatcher().execute(cmd, server.createCommandSourceStack().withLevel(helper.getLevel()).withSuppressedOutput());
            helper.assertTrue(r > 0, "/place failed: " + cmd);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            helper.assertTrue(false, "/place error: " + e.getMessage());
        }
    }

    private static int count(GameTestHelper helper, BlockPos base, int size, net.minecraft.world.level.block.Block block) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(base, base.offset(size, size, size)))
            if (helper.getLevel().getBlockState(p).is(block)) n++;
        return n;
    }

    public static void wellPlacesFromEndstoneBricks(GameTestHelper helper) {
        if (!active(helper)) return;
        BlockPos base = helper.absolutePos(new BlockPos(0, 1, 0));
        place(helper, "well_intact", base);
        helper.assertTrue(count(helper, base, 12, Blocks.END_STONE_BRICKS) > 40, "intact well has too few end stone bricks");
        BlockPos other = base.offset(0, 0, 14);
        place(helper, "well_broken_b", other);
        helper.assertTrue(count(helper, other, 12, Blocks.END_STONE_BRICKS) < count(helper, base, 12, Blocks.END_STONE_BRICKS), "broken well must have fewer bricks");
        helper.succeed();
    }

    /** 26.3 renamed ServerLevel#getStructureManager() to getStructureTemplateManager(); the callers only run on 26.3. */
    private static net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager templates(GameTestHelper helper) {
        try {
            return (net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager)
                    helper.getLevel().getClass().getMethod("getStructureTemplateManager").invoke(helper.getLevel());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("no structure template manager", e);
        }
    }

    /** The blocks a template occupies when {@link #place placed} at {@code pos} (no rotation, no mirror). */
    private static BoundingBox templateBox(GameTestHelper helper, String template, BlockPos pos) {
        var size = templates(helper).get(id("end/" + template)).orElseThrow(() -> new IllegalStateException("template missing: " + template)).getSize();
        return BoundingBox.fromCorners(pos, pos.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1));
    }

    /**
     * Wreck with chests, mound and the broken-elytra frame; the frame-less variant spawns no frame.
     *
     * <p>Flaky until 12.11. (gate 94, 03.11.; simplebuilding-8, reasons in AUDIT.md): (1) the wreck (15x12x17 and
     * 19x11x13 blocks) is much larger than the 8x8 test room and lies mostly in chunks the runner neither force-loads nor
     * waits for - it only waits until the room's own chunks tick entities. The frame (template z 8) sits outside the room;
     * an entity added to a chunk whose entity visibility is not confirmed yet lands in a HIDDEN section, and
     * {@code getEntitiesOfClass} skips it ("found 0"). The test now force-loads every chunk under the wreck and waits until
     * their entities tick. (2) The frame search {@code AABB(base).inflate(14)} reached 14 blocks below and beside the wreck
     * into the air space of up to eight neighbour tests; every check and the clean-up now stay inside the template boxes.
     * (3) A failed check left the wreck standing; the clean-up now runs in {@code finally}.
     */
    public static void wreckHasLootChestsAndFrame(GameTestHelper helper) {
        if (!active(helper)) return;
        // build high above the 8x8 room so its own blocks never mix with the wreck
        BlockPos base = helper.absolutePos(new BlockPos(0, 48, 0));
        BoundingBox stern = templateBox(helper, "wreck_stern_elytra", base), bow = templateBox(helper, "wreck_bow_none", base);
        BoundingBox both = BoundingBox.encapsulating(stern, bow);
        var level = helper.getLevel();
        java.util.List<int[]> chunks = new java.util.ArrayList<>(), forced = new java.util.ArrayList<>();
        for (int cx = both.minX() >> 4; cx <= both.maxX() >> 4; cx++)
            for (int cz = both.minZ() >> 4; cz <= both.maxZ() >> 4; cz++) {
                chunks.add(new int[]{cx, cz});
                if (level.setChunkForced(cx, cz, true)) forced.add(new int[]{cx, cz}); // only release what this test forced
            }
        Runnable release = () -> forced.forEach(c -> level.setChunkForced(c[0], c[1], false));
        helper.runBeforeTestEnd(release); // timeout while waiting: nothing was placed yet
        helper.startSequence().thenWaitUntil(() -> {
            for (int[] c : chunks)
                helper.assertTrue(level.areEntitiesActuallyLoadedAndTicking(new net.minecraft.world.level.ChunkPos(c[0], c[1])),
                        "wreck chunk " + c[0] + "," + c[1] + " does not tick entities yet");
        }).thenExecute(() -> {
            try {
                checkWrecks(helper, base, stern, bow);
            } finally {
                clear(helper, both);
                release.run();
            }
        }).thenSucceed();
    }

    private static void checkWrecks(GameTestHelper helper, BlockPos base, BoundingBox stern, BoundingBox bow) {
        var level = helper.getLevel();
        BoundingBox both = BoundingBox.encapsulating(stern, bow);
        var before = level.getEntitiesOfClass(ItemFrame.class, AABB.of(both));
        helper.assertTrue(before.isEmpty(), "item frames in the wreck box before placing (other test?): "
                + before.stream().map(f -> f.getItem() + "@" + f.blockPosition()).toList() + " box " + both);
        place(helper, "wreck_stern_elytra", base);
        int chests = 0;
        for (BlockPos p : BlockPos.betweenClosed(stern.minX(), stern.minY(), stern.minZ(), stern.maxX(), stern.maxY(), stern.maxZ())) {
            if (level.getBlockEntity(p) instanceof RandomizableContainer c && c.getLootTable() != null) {
                helper.assertTrue(c.getLootTable().identifier().equals(id("chests/end_wreck")), "chest at " + p + " has wrong loot table " + c.getLootTable().identifier());
                chests++;
            }
        }
        helper.assertTrue(chests >= 1 && chests <= 2, "wreck must hold 1-2 loot chests, found " + chests);
        // half buried: an end-stone mound fills the lower layers (the structure starts GROUND = 9 blocks below the surface, about 80 % of the hull)
        int mound = 0;
        for (BlockPos p : BlockPos.betweenClosed(stern.minX(), stern.minY(), stern.minZ(), stern.maxX(), stern.minY() + 3, stern.maxZ()))
            if (level.getBlockState(p).is(Blocks.END_STONE)) mound++;
        helper.assertTrue(mound > 150, "wreck is not buried in an end-stone mound, found " + mound);
        var frames = level.getEntitiesOfClass(ItemFrame.class, AABB.of(stern));
        helper.assertTrue(frames.size() == 1 && frames.get(0).getItem().is(Items.ELYTRA)
                && frames.get(0).getItem().getDamageValue() == frames.get(0).getItem().getMaxDamage() - 1,
                "expected one frame with a broken elytra, found " + frames.size() + " " + frames.stream().map(f -> f.getItem() + "@" + f.blockPosition()).toList() + " base " + base);
        clear(helper, both);
        place(helper, "wreck_bow_none", base);
        var bowFrames = level.getEntitiesOfClass(ItemFrame.class, AABB.of(bow));
        helper.assertTrue(bowFrames.isEmpty(), "frame-less wreck spawned a frame: " + bowFrames.stream().map(f -> f.getItem() + "@" + f.blockPosition()).toList());
    }

    /** Removes everything a wreck test placed inside {@code box} (no loot spill, no drops, no neighbour updates). */
    private static void clear(GameTestHelper helper, BoundingBox box) {
        var level = helper.getLevel();
        level.getEntitiesOfClass(ItemFrame.class, AABB.of(box)).forEach(f -> f.discard());
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()))
            if (!level.getBlockState(p).isAir()) {
                if (level.getBlockEntity(p) instanceof RandomizableContainer r) r.setLootTable(null); // no loot spill
                if (level.getBlockEntity(p) instanceof net.minecraft.world.Clearable c) c.clearContent();
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
    }

    public static void gatewayAndPathPlace(GameTestHelper helper) {
        if (!active(helper)) return;
        BlockPos base = helper.absolutePos(new BlockPos(0, 1, 0));
        place(helper, "gateway_a", base);
        helper.assertTrue(count(helper, base, 8, Blocks.END_STONE_BRICKS) >= 5, "fake gateway lacks bricks");
        helper.assertTrue(count(helper, base, 8, Blocks.END_GATEWAY) == 0 && count(helper, base, 8, Blocks.BEDROCK) == 0, "fake gateway must be non-functional and bedrock-free");
        place(helper, "path_cross", base.offset(0, 0, 10));
        helper.assertTrue(count(helper, base.offset(0, 0, 10), 12, Blocks.END_STONE_BRICKS) >= 15, "path is missing its end stone bricks");
        helper.succeed();
    }
}
