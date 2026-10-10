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
import net.minecraft.world.phys.AABB;

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
        // 26.3 renamed ServerLevel#getStructureManager() to getStructureTemplateManager(); this test only runs on 26.3.
        net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager manager;
        try {
            manager = (net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager)
                    helper.getLevel().getClass().getMethod("getStructureTemplateManager").invoke(helper.getLevel());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("no structure template manager", e);
        }
        for (String t : TEMPLATES) {
            var template = manager.get(id("end/" + t));
            helper.assertTrue(template.isPresent(), "template missing: " + t);
            helper.assertTrue(template.get().getSize().getY() >= 1 && template.get().getSize().getX() >= 3, "template empty: " + t);
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

    public static void wreckHasLootChestsAndFrame(GameTestHelper helper) {
        if (!active(helper)) return;
        BlockPos base = helper.absolutePos(new BlockPos(0, 1, 0));
        place(helper, "wreck_stern_elytra", base);
        int chests = 0;
        for (BlockPos p : BlockPos.betweenClosed(base, base.offset(12, 12, 12))) {
            if (helper.getLevel().getBlockEntity(p) instanceof RandomizableContainer c && c.getLootTable() != null) {
                helper.assertTrue(c.getLootTable().identifier().equals(id("chests/end_wreck")), "chest has wrong loot table");
                chests++;
            }
        }
        helper.assertTrue(chests >= 1 && chests <= 2, "wreck must hold 1-2 loot chests, found " + chests);
        var frames = helper.getLevel().getEntitiesOfClass(ItemFrame.class, new AABB(base).inflate(14));
        helper.assertTrue(frames.size() == 1 && frames.get(0).getItem().is(Items.ELYTRA)
                && frames.get(0).getItem().getDamageValue() == frames.get(0).getItem().getMaxDamage() - 1, "expected one frame with a broken elytra");
        frames.forEach(f -> f.discard());
        BlockPos plain = base.offset(0, 0, 16);
        place(helper, "wreck_bow_none", plain);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemFrame.class, new AABB(plain).inflate(14)).isEmpty(), "frame-less wreck spawned a frame");
        helper.succeed();
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
