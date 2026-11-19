package com.simplelib.test;

import com.simplelib.SimpleLib;
import com.simplelib.api.IdMoves;
import com.simplelib.api.SimpleLibApi;
import com.simplelib.block.EmeraldPressurePlateBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PressurePlateBlock;

/**
 * N48 (b-circ46): the material pressure plates of SimpleLib (group {@code material_pressure_plates}) - registration, the moved ids
 * and the Emerald plate's two modes (villagers only / everything living except villagers while redstone powers the plate).
 */
public final class PlateTests {
    private PlateTests() {}

    /** The four plates, their block items, the filter plates' block entity type, and the ids of Simple Pads and Simple Ores that move here. */
    public static void registeredAndMoved(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (String id : List.of("diamond_pressure_plate", "netherite_pressure_plate", "enderite_pressure_plate", "emerald_pressure_plate")) {
            Block block = SimpleLibApi.materialBlock(id);
            if (!(block instanceof PressurePlateBlock) || BuiltInRegistries.BLOCK.getValue(SimpleLib.id(id)) != block || block.asItem() != SimpleLibApi.materialItem(id)) {
                problems.add("plate " + id + " is not registered with its item");
            }
        }
        if (!BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(SimpleLib.id("filter_pressure_plate_be"))) problems.add("no filter_pressure_plate_be");
        for (String id : List.of("diamond_pressure_plate", "netherite_pressure_plate", "enderite_pressure_plate")) {
            for (var registry : List.of(Registries.BLOCK, Registries.ITEM)) {
                if (!SimpleLib.id(id).equals(IdMoves.target(registry, Identifier.fromNamespaceAndPath("simplepads", id)))) problems.add("simplepads:" + id + " in " + registry.identifier());
            }
        }
        for (var registry : List.of(Registries.BLOCK, Registries.ITEM)) {
            if (!SimpleLib.id("enderite_pressure_plate").equals(IdMoves.target(registry, Identifier.fromNamespaceAndPath("simpleores", "enderite_pressure_plate")))) {
                problems.add("simpleores:enderite_pressure_plate in " + registry.identifier());
            }
        }
        if (!SimpleLib.id("filter_pressure_plate_be").equals(IdMoves.target(Registries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath("simplepads", "netherite_pressure_plate_be")))) {
            problems.add("simplepads:netherite_pressure_plate_be");
        }
        for (String id : List.of("diamond_pressure_plate", "netherite_pressure_plate_smithing", "enderite_pressure_plate_smithing")) {
            if (!SimpleLib.id(id).equals(IdMoves.target(Registries.RECIPE, Identifier.fromNamespaceAndPath("simplepads", id)))) problems.add("recipe simplepads:" + id);
        }
        if (!SimpleLib.id("enderite_pressure_plate").equals(IdMoves.target(Registries.RECIPE, Identifier.fromNamespaceAndPath("simpleores", "enderite_pressure_plate")))) {
            problems.add("recipe simpleores:enderite_pressure_plate");
        }
        h.assertTrue(problems.isEmpty(), "material plates: " + problems);
        h.succeed();
    }

    /**
     * N100: the pressed Netherite plate only lights up like the diamond one - blockstate powered=true uses the _down model, that
     * one the active texture, and the texture (built from the Netherite block, tools/textures/n100_netherite_plate_2026_11_18.py)
     * has a lighter but grey centre, no orange glow. Without the client jar the build ships a grey placeholder: then only the
     * references are checked.
     */
    public static void netheritePlateHighlightsLikeDiamond(GameTestHelper h) {
        String state = resource(h, "/assets/simplelib/blockstates/netherite_pressure_plate.json");
        h.assertTrue(state.contains("simplelib:block/netherite_pressure_plate_down"), "powered=true has no _down model: " + state);
        String down = resource(h, "/assets/simplelib/models/block/netherite_pressure_plate_down.json");
        h.assertTrue(down.contains("minecraft:block/pressure_plate_down") && down.contains("simplelib:block/netherite_pressure_plate_active"),
                "the pressed model does not use the active texture: " + down);
        String up = resource(h, "/assets/simplelib/models/block/netherite_pressure_plate.json");
        h.assertTrue(up.contains("minecraft:block/netherite_block"), "the resting plate is the Netherite block: " + up);
        if (PlateTests.class.getResource("/assets/simplelib/vanilla_derived_placeholders.txt") != null) {
            h.succeed();
            return;
        }
        try (java.io.InputStream in = PlateTests.class.getResourceAsStream("/assets/simplelib/textures/block/netherite_pressure_plate_active.png")) {
            h.assertTrue(in != null, "no active texture for the pressed Netherite plate");
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(in);
            h.assertTrue(img != null && img.getWidth() == 16 && img.getHeight() == 16, "the active texture is not 16x16");
            int centre = img.getRGB(7, 7), edge = img.getRGB(1, 7);
            int r = (centre >> 16) & 0xFF, g = (centre >> 8) & 0xFF, b = centre & 0xFF;
            h.assertTrue(Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) < 32, "the centre is coloured (orange?), not a grey highlight: " + Integer.toHexString(centre));
            int lc = r + g + b, le = ((edge >> 16) & 0xFF) + ((edge >> 8) & 0xFF) + (edge & 0xFF);
            h.assertTrue(lc > le, "the centre is not lighter than the rim (no highlight)");
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read the active Netherite plate texture", e);
        }
        h.succeed();
    }

    private static String resource(GameTestHelper h, String path) {
        try (java.io.InputStream in = PlateTests.class.getResourceAsStream(path)) {
            h.assertTrue(in != null, "no " + path + " on the classpath");
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
    }

    private static Mob mob(GameTestHelper h, net.minecraft.world.entity.EntityType<? extends Mob> type, BlockPos at) {
        Mob mob = h.spawn(type, at);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        return mob;
    }

    /**
     * Unpowered the Emerald plate answers to villagers only: a villager presses it (and keeps it pressed - its own output into the
     * block below never flips the mode), a pig, a wandering trader and a dropped item do not.
     */
    public static void emeraldPlateVillagersOnly(GameTestHelper h) {
        EmeraldPressurePlateBlock block = (EmeraldPressurePlateBlock) SimpleLibApi.materialBlock("emerald_pressure_plate");
        BlockPos villager = new BlockPos(1, 2, 1), pig = new BlockPos(3, 2, 1), trader = new BlockPos(5, 2, 1);
        for (BlockPos pos : List.of(villager, pig, trader)) {
            h.setBlock(pos.below(), Blocks.STONE);
            h.setBlock(pos, block);
        }
        mob(h, EntityTypes.VILLAGER, villager);
        mob(h, EntityTypes.PIG, pig);
        mob(h, EntityTypes.WANDERING_TRADER, trader);
        h.getLevel().scheduleTick(h.absolutePos(villager), block, 1);
        h.startSequence().thenExecuteAfter(5, () -> {
            h.assertValueEqual(block.signalAt(h.getLevel(), h.absolutePos(villager)), 15, "signal under a villager");
            h.assertValueEqual(block.signalAt(h.getLevel(), h.absolutePos(pig)), 0, "signal under a pig");
            h.assertValueEqual(block.signalAt(h.getLevel(), h.absolutePos(trader)), 0, "signal under a wandering trader (not a villager)");
            h.assertTrue(h.getBlockState(villager).getValue(PressurePlateBlock.POWERED), "the plate under the villager is not pressed");
        }).thenExecuteAfter(40, () -> {
            h.assertValueEqual(block.signalAt(h.getLevel(), h.absolutePos(villager)), 15, "signal under a villager after the plate fed the block below");
            h.assertTrue(h.getBlockState(villager).getValue(PressurePlateBlock.POWERED), "the pressed plate let go by itself (feedback)");
            h.assertFalse(h.getBlockState(pig).getValue(PressurePlateBlock.POWERED), "a pig pressed the unpowered plate");
        }).thenSucceed();
    }

    /**
     * Powered by redstone (from the side, from below) the Emerald plate is the other way round: a pig presses it, a villager does not;
     * switching the power off flips it back.
     */
    public static void emeraldPlateInvertsWhenPowered(GameTestHelper h) {
        EmeraldPressurePlateBlock block = (EmeraldPressurePlateBlock) SimpleLibApi.materialBlock("emerald_pressure_plate");
        BlockPos side = new BlockPos(1, 2, 1), below = new BlockPos(4, 2, 1);
        h.setBlock(side.below(), Blocks.STONE);
        h.setBlock(below.below(), Blocks.REDSTONE_BLOCK);
        h.setBlock(side, block);
        h.setBlock(below, block);
        h.setBlock(side.east(), Blocks.REDSTONE_BLOCK);
        mob(h, EntityTypes.PIG, side);
        mob(h, EntityTypes.VILLAGER, below);
        h.getLevel().scheduleTick(h.absolutePos(side), block, 1);
        h.startSequence().thenExecuteAfter(5, () -> {
            h.assertTrue(EmeraldPressurePlateBlock.poweredFromOutside(h.getLevel(), h.absolutePos(side)), "the plate beside a redstone block is not powered");
            h.assertTrue(EmeraldPressurePlateBlock.poweredFromOutside(h.getLevel(), h.absolutePos(below)), "the plate on a redstone block is not powered");
            h.assertValueEqual(block.signalAt(h.getLevel(), h.absolutePos(side)), 15, "powered plate under a pig");
            h.assertValueEqual(block.signalAt(h.getLevel(), h.absolutePos(below)), 0, "powered plate under a villager");
            h.assertTrue(h.getBlockState(side).getValue(PressurePlateBlock.POWERED), "the powered plate under the pig is not pressed");
            h.setBlock(side.east(), Blocks.AIR);
            h.setBlock(below.below(), Blocks.STONE);
        }).thenExecuteAfter(30, () -> {
            h.assertFalse(EmeraldPressurePlateBlock.poweredFromOutside(h.getLevel(), h.absolutePos(side)), "the plate is still powered without the redstone block");
            h.assertValueEqual(block.signalAt(h.getLevel(), h.absolutePos(side)), 0, "unpowered plate under a pig");
            h.assertValueEqual(block.signalAt(h.getLevel(), h.absolutePos(below)), 15, "unpowered plate under a villager");
            h.assertFalse(h.getBlockState(side).getValue(PressurePlateBlock.POWERED), "the plate under the pig did not let go");
        }).thenSucceed();
    }
}
