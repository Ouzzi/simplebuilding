package com.simplebuilding.modules.simplelayers.test;

import com.simplebuilding.modules.simplelayers.LayersRegistry;
import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.block.GlitchedBedrockBlock;
import com.simplebuilding.modules.simplelayers.block.NeonLightBlock;
import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import com.simplebuilding.modules.simplelayers.world.BackroomsChunkGenerator;
import com.simplebuilding.modules.simplelayers.world.BackroomsEntries;
import com.simplebuilding.modules.simplelayers.world.BackroomsGrid;
import com.simplebuilding.modules.simplelayers.world.BackroomsTravel;
import com.simplebuilding.modules.simplelayers.world.CloudTransition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Simple Layers tests (plan docs/ai/PLAN-A-LAYERS10-2026-11-08.md stage 5). Loader-neutral; the Fabric adapter registers {@link #ALL}
 * as module_game_test_<name> on {@code simplelayers:empty} (8 x 8 x 8); every test runs standalone and with SimpleBuilding.
 */
public final class LayersTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    static {
        ALL.put("registered", LayersTests::registered);
        ALL.put("sub_mod_switch", LayersTests::subModSwitch);
        ALL.put("cloud_blocks_instabreak", LayersTests::cloudBlocksInstabreak);
        ALL.put("mist_has_no_collision", LayersTests::mistHasNoCollision);
        ALL.put("fall_damage_factors", LayersTests::fallDamageFactors);
        ALL.put("dimensions_load", LayersTests::dimensionsLoad);
        ALL.put("cloud_entry_rule", LayersTests::cloudEntryRule);
        ALL.put("cloud_travel_both_ways", LayersTests::cloudTravelBothWays);
        ALL.put("backrooms_grid", LayersTests::backroomsGrid);
        ALL.put("backrooms_chunk_matches_grid", LayersTests::backroomsChunkMatchesGrid);
        ALL.put("special_rooms_registered", LayersTests::specialRoomsRegistered);
        ALL.put("glitched_bedrock_needs_netherite", LayersTests::glitchedBedrockNeedsNetherite);
        ALL.put("rift_leads_into_backrooms", LayersTests::riftLeadsIntoBackrooms);
        ALL.put("exit_leads_to_the_surface", LayersTests::exitLeadsToTheSurface);
        ALL.put("almond_water", LayersTests::almondWater);
        ALL.put("guide_book", LayersTests::guideBook);
    }

    private LayersTests() {}

    static void registered(GameTestHelper h) {
        List<String> missing = new ArrayList<>();
        for (String path : LayersRegistry.ITEM_PATHS) if (!BuiltInRegistries.ITEM.containsKey(SimpleLayers.id(path))) missing.add("item " + path);
        for (String path : List.of("exit_door", "rift")) {
            if (!BuiltInRegistries.BLOCK.containsKey(SimpleLayers.id(path))) missing.add("block " + path);
            if (BuiltInRegistries.ITEM.containsKey(SimpleLayers.id(path))) missing.add("no item expected for " + path);
        }
        if (!BuiltInRegistries.CHUNK_GENERATOR.containsKey(SimpleLayers.id("backrooms"))) missing.add("chunk generator");
        for (String f : List.of("mist_pit", "sky_chest", "glitched_bedrock")) if (!BuiltInRegistries.FEATURE_TYPE.containsKey(SimpleLayers.id(f))) missing.add("feature " + f);
        for (String s : List.of("ambient.backrooms.hum", "ambient.backrooms.additions", "ambient.cloud_sea.wind", "block.neon_light.buzz", "ambient.backrooms.listener")) {
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(SimpleLayers.id(s))) missing.add("sound " + s);
        }
        if (LayersRegistry.ITEMS.size() != LayersRegistry.ITEM_PATHS.size()) missing.add("item list " + LayersRegistry.ITEMS.size());
        h.assertTrue(missing.isEmpty(), "missing: " + missing);
        h.succeed();
    }

    static void subModSwitch(GameTestHelper h) {
        com.simplebuilding.framework.api.SubMods.set(SimpleLayers.MOD_ID, false);
        try {
            h.assertFalse(SimpleLayers.on() || SimpleLayers.cloudsOn() || SimpleLayers.backroomsOn(), "switched off");
            h.assertTrue(LayersRegistry.tabStacks().isEmpty(), "no tab stacks while off");
        } finally {
            com.simplebuilding.framework.api.SubMods.set(SimpleLayers.MOD_ID, true);
        }
        h.assertTrue(SimpleLayers.on() && LayersRegistry.tabStacks().size() == LayersRegistry.ITEMS.size() + 1, "back on: items + guide");
        h.succeed();
    }

    /** Stage 5: Cloud Block, Cloud Stone and Mist Block break instantly, by hand too. */
    static void cloudBlocksInstabreak(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos rel = new BlockPos(2, 2, 2), abs = h.absolutePos(rel);
        for (Block b : List.of(LayersRegistry.CLOUD_BLOCK, LayersRegistry.CLOUD_STONE, LayersRegistry.MIST_BLOCK)) {
            h.setBlock(rel, b);
            BlockState s = h.getLevel().getBlockState(abs);
            h.assertTrue(s.getDestroySpeed(h.getLevel(), abs) == 0.0F, b + " hardness 0");
            h.assertTrue(s.getDestroyProgress(player, h.getLevel(), abs) >= 1.0F, b + " breaks in one tick by hand");
        }
        h.succeed();
    }

    /** Stage 5: mist has no collision (an armor stand falls through), is not replaceable; the cloud block holds. */
    static void mistHasNoCollision(GameTestHelper h) {
        BlockPos mist = new BlockPos(2, 4, 2), cloud = new BlockPos(5, 4, 5);
        h.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        h.setBlock(mist, LayersRegistry.MIST_BLOCK);
        h.setBlock(cloud, LayersRegistry.CLOUD_BLOCK);
        var level = h.getLevel();
        h.assertTrue(level.getBlockState(h.absolutePos(mist)).getCollisionShape(level, h.absolutePos(mist)).isEmpty(), "mist collision empty");
        h.assertFalse(level.getBlockState(h.absolutePos(mist)).canBeReplaced(), "mist is not replaceable");
        h.assertFalse(level.getBlockState(h.absolutePos(cloud)).getCollisionShape(level, h.absolutePos(cloud)).isEmpty(), "cloud has collision");
        ArmorStand falling = h.spawn(EntityTypes.ARMOR_STAND, new Vec3(2.5, 5.2, 2.5));
        ArmorStand standing = h.spawn(EntityTypes.ARMOR_STAND, new Vec3(5.5, 5.2, 5.5));
        h.succeedWhen(() -> {
            double fallY = falling.getY() - h.absolutePos(BlockPos.ZERO).getY(), standY = standing.getY() - h.absolutePos(BlockPos.ZERO).getY();
            h.assertTrue(fallY < 3.0, "fell through the mist (y " + fallY + ")");
            h.assertTrue(Math.abs(standY - 5.0) < 0.1 && standing.onGround(), "stands on the cloud (y " + standY + ")");
        });
    }

    /** Stage 5: a fall of 8 blocks (5 damage on stone) does 3 on a Cloud Block (x0.7) and 4 on Cloud Stone (x0.9). */
    static void fallDamageFactors(GameTestHelper h) {
        int[] expected = {5, 3, 4};
        Block[] blocks = {Blocks.STONE, LayersRegistry.CLOUD_BLOCK, LayersRegistry.CLOUD_STONE};
        for (int i = 0; i < blocks.length; i++) {
            BlockPos rel = new BlockPos(1 + i * 2, 1, 1);
            h.setBlock(rel, blocks[i]);
            Pig pig = h.spawnWithNoFreeWill(EntityTypes.PIG, rel.above());
            float before = pig.getHealth();
            BlockPos abs = h.absolutePos(rel);
            blocks[i].fallOn(h.getLevel(), h.getLevel().getBlockState(abs), abs, pig, 8.0);
            float damage = before - pig.getHealth();
            h.assertTrue(Math.abs(damage - expected[i]) < 0.01F, blocks[i] + ": damage " + damage + ", expected " + expected[i]);
        }
        h.assertTrue(LayersRegistry.CLOUD_DAMAGE == 0.7F && LayersRegistry.CLOUD_STONE_DAMAGE == 0.9F, "factors 0.7 / 0.9");
        h.succeed();
    }

    /** Stage 5: both dimensions exist with the planned heights and generators. */
    static void dimensionsLoad(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerLevel clouds = server.getLevel(SimpleLayers.CLOUD_SEA), back = server.getLevel(SimpleLayers.BACKROOMS);
        h.assertTrue(clouds != null && back != null, "both levels loaded");
        h.assertTrue(clouds.getMinY() == 0 && clouds.getHeight() == 512, "cloud sea 0..512");
        h.assertTrue(back.getMinY() == 0 && back.getHeight() == 64, "backrooms 0..64");
        h.assertTrue(back.getChunkSource().getGenerator() instanceof BackroomsChunkGenerator, "backrooms generator");
        h.assertTrue(clouds.dimensionType().hasSkyLight() && !back.dimensionType().hasSkyLight() && back.dimensionType().hasCeiling(), "sky / ceiling");
        h.succeed();
    }

    /** Stage 5: only an upward crossing of the entry height counts, and never at or above an announced orbit height. */
    static void cloudEntryRule(GameTestHelper h) {
        h.assertTrue(CloudTransition.entersClouds(699.5, 700.5, 700, OptionalInt.empty()), "crossing up");
        h.assertFalse(CloudTransition.entersClouds(700.5, 702.0, 700, OptionalInt.empty()), "already above");
        h.assertFalse(CloudTransition.entersClouds(701.0, 699.0, 700, OptionalInt.empty()), "going down");
        h.assertTrue(CloudTransition.entersClouds(699.0, 701.0, 700, OptionalInt.of(1536)), "below the orbit");
        h.assertFalse(CloudTransition.entersClouds(699.0, 701.0, 700, OptionalInt.of(650)), "orbit lower than the clouds wins");
        h.assertTrue(LayersConfig.cloudReturnY() == LayersConfig.cloudEntryY - 20, "return 20 below the entry");
        h.succeed();
    }

    /** Stage 5: an entity goes up into the Cloud Sea (y 16, cloud platform below it) and falls back to the Overworld at y 680. */
    static void cloudTravelBothWays(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ArmorStand stand = h.spawn(EntityTypes.ARMOR_STAND, new Vec3(3.5, 2.0, 3.5));
        Entity up = CloudTransition.toClouds(server, stand);
        h.assertTrue(up != null && up.level().dimension() == SimpleLayers.CLOUD_SEA, "in the cloud sea");
        h.assertTrue(up.getY() >= CloudTransition.ARRIVAL_Y - 0.01 && up.level().getBlockState(up.blockPosition()).getCollisionShape(up.level(), up.blockPosition()).isEmpty(), "free arrival at " + up.getY());
        BlockPos below = up.blockPosition().below();
        BlockState under = up.level().getBlockState(below);
        h.assertFalse(under.isAir(), "something to stand on below the arrival");
        Entity down = CloudTransition.toOverworld(server, up, LayersConfig.cloudReturnY(), true);
        h.assertTrue(down != null && down.level().dimension() == net.minecraft.world.level.Level.OVERWORLD, "back in the overworld");
        h.assertTrue(Math.abs(down.getY() - LayersConfig.cloudReturnY()) < 0.01, "return height " + down.getY());
        down.discard();
        h.succeed();
    }

    /** Stage 5: the grid is deterministic, closed by bedrock, carpet below, every arrival cell free, and about 2/3 of the edges have doors. */
    static void backroomsGrid(GameTestHelper h) {
        long seed = 12345L;
        java.util.Random random = new java.util.Random(7);
        int doors = 0, edges = 0;
        for (int i = 0; i < 400; i++) {
            int x = random.nextInt(20000) - 10000, z = random.nextInt(20000) - 10000;
            h.assertTrue(BackroomsGrid.at(seed, x, 0, z).is(Blocks.BEDROCK) && BackroomsGrid.at(seed, x, BackroomsGrid.TOP_Y, z).is(Blocks.BEDROCK), "bedrock bounds");
            h.assertTrue(BackroomsGrid.at(seed, x, 1, z).is(LayersRegistry.DAMP_CARPET), "carpet floor");
            h.assertTrue(BackroomsGrid.at(seed, x, 4, z) == BackroomsGrid.at(seed, x, 4, z), "deterministic");
            int cx = BackroomsGrid.cellCentre(x), cz = BackroomsGrid.cellCentre(z);
            for (int y = 2; y <= 6; y++) h.assertTrue(BackroomsGrid.at(seed, cx, y, cz).isAir(), "arrival cell free at y " + y);
            BackroomsGrid.Edge e = BackroomsGrid.west(seed, Math.floorDiv(x, 8), Math.floorDiv(z, 8));
            if (e != BackroomsGrid.Edge.OPEN) {
                edges++;
                if (e == BackroomsGrid.Edge.DOOR) doors++;
            }
        }
        double share = doors / (double) edges;
        h.assertTrue(share > 0.55 && share < 0.77, "door share " + share);
        BlockState light = BackroomsGrid.at(seed, 3, BackroomsGrid.CEILING_Y, 4);
        h.assertTrue(light.is(LayersRegistry.NEON_LIGHT) && NeonLightBlock.light(light) >= 0, "neon light in the ceiling");
        h.succeed();
    }

    /** Stage 5: a generated Backrooms chunk holds exactly the grid (generator + world seed). */
    static void backroomsChunkMatchesGrid(GameTestHelper h) {
        ServerLevel back = h.getLevel().getServer().getLevel(SimpleLayers.BACKROOMS);
        h.assertTrue(back != null, "backrooms level");
        long seed = BackroomsChunkGenerator.seed(back.getChunkSource().randomState());
        int cx = 4000, cz = -4000;
        back.getChunk(cx, cz);
        List<String> wrong = new ArrayList<>();
        for (int x = 0; x < 16; x += 3) {
            for (int z = 0; z < 16; z += 5) {
                for (int y = 0; y <= BackroomsGrid.TOP_Y; y++) {
                    BlockPos p = new BlockPos(cx * 16 + x, y, cz * 16 + z);
                    BlockState want = BackroomsGrid.at(seed, p.getX(), y, p.getZ()), got = back.getBlockState(p);
                    if (got.getBlock() != want.getBlock() && !inSpecialRoom(back, p)) wrong.add(p.toShortString() + " " + got + " != " + want);
                }
            }
        }
        h.assertTrue(wrong.isEmpty(), "chunk differs: " + wrong.subList(0, Math.min(4, wrong.size())));
        h.succeed();
    }

    private static boolean inSpecialRoom(ServerLevel level, BlockPos pos) {
        return level.structureManager().getStructureWithPieceAt(pos, s -> true).isValid();
    }

    /** Stage 5: the special rooms (jigsaw) and their templates are there and fit the grid (17 x 7 x 17). */
    static void specialRoomsRegistered(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        var structures = registries.lookupOrThrow(Registries.STRUCTURE);
        var pools = registries.lookupOrThrow(Registries.TEMPLATE_POOL);
        for (String s : List.of("backrooms_room", "backrooms_exit")) h.assertTrue(structures.containsKey(SimpleLayers.id(s)), "structure " + s);
        for (String p : List.of("backrooms/rooms", "backrooms/exit")) h.assertTrue(pools.containsKey(SimpleLayers.id(p)), "pool " + p);
        var templates = h.getLevel().getStructureTemplateManager();
        for (String t : List.of("exit_room", "storage_room", "water_room", "dark_room")) {
            var template = templates.get(SimpleLayers.id("backrooms/" + t));
            h.assertTrue(template.isPresent(), "template " + t);
            var size = template.get().getSize();
            h.assertTrue(size.getX() == 17 && size.getY() == 7 && size.getZ() == 17, t + " size " + size);
        }
        h.succeed();
    }

    /** Stage 5 entrance: only the Netherite Pickaxe makes progress (about 30 s), breaking leaves a rift. */
    static void glitchedBedrockNeedsNetherite(GameTestHelper h) {
        BlockPos rel = new BlockPos(2, 2, 2), abs = h.absolutePos(rel);
        h.setBlock(rel, LayersRegistry.GLITCHED_BEDROCK);
        BlockState s = h.getLevel().getBlockState(abs);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        h.assertTrue(s.getDestroyProgress(player, h.getLevel(), abs) == 0.0F, "diamond pickaxe: no progress");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
        h.assertTrue(GlitchedBedrockBlock.canMine(player), "netherite allowed");
        float progress = s.getDestroyProgress(player, h.getLevel(), abs);
        h.assertTrue(progress > 1.0F / 700.0F && progress < 1.0F / 500.0F, "about 600 ticks, progress " + progress);
        h.getLevel().removeBlock(abs, false);
        LayersRegistry.GLITCHED_BEDROCK.destroy(h.getLevel(), abs, s);
        h.assertTrue(h.getLevel().getBlockState(abs).is(LayersRegistry.RIFT), "rift left behind");
        h.assertTrue(h.getLevel().getBlockState(abs).getCollisionShape(h.getLevel(), abs).isEmpty(), "rift without collision");
        h.succeed();
    }

    /** Stage 5 entrance: whatever enters the Backrooms arrives in a free cell centre at y 2. */
    static void riftLeadsIntoBackrooms(GameTestHelper h) {
        ArmorStand stand = h.spawn(EntityTypes.ARMOR_STAND, new Vec3(1.5, 2.0, 1.5));
        Entity in = BackroomsTravel.enter(h.getLevel().getServer(), stand);
        h.assertTrue(in != null && in.level().dimension() == SimpleLayers.BACKROOMS, "in the backrooms");
        h.assertTrue(Math.abs(in.getY() - BackroomsTravel.ARRIVAL_Y) < 0.01, "arrival y " + in.getY());
        h.assertTrue(Math.floorMod(in.getBlockX(), BackroomsGrid.CELL) == 4 && Math.floorMod(in.getBlockZ(), BackroomsGrid.CELL) == 4, "cell centre");
        h.assertTrue(in.level().getBlockState(in.blockPosition()).isAir() || in.level().getBlockState(in.blockPosition()).getCollisionShape(in.level(), in.blockPosition()).isEmpty(), "free spot");
        h.assertTrue(in.isOnPortalCooldown(), "cooldown against bouncing");
        in.discard();
        h.succeed();
    }

    /** Stage 5 exit: the entry is remembered per player, the way out stands on the Overworld ground above it. */
    static void exitLeadsToTheSurface(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        UUID id = UUID.randomUUID();
        BackroomsEntries entries = BackroomsEntries.get(server);
        BlockPos entry = h.absolutePos(new BlockPos(1, 1, 1));
        entries.put(id, entry);
        h.assertTrue(entry.equals(entries.get(id)), "entry remembered");
        entries.remove(id);
        h.assertTrue(entries.get(id) == null, "entry forgotten after the exit");
        h.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
        BlockPos col = h.absolutePos(new BlockPos(4, 1, 4));
        Vec3 out = BackroomsTravel.surface(server.overworld(), col.getX(), col.getZ());
        h.assertTrue(out.y >= col.getY() + 1 && out.x == col.getX() + 0.5, "stands on the ground: " + out);
        h.assertTrue(BuiltInRegistries.BLOCK.getValue(SimpleLayers.id("exit_door")) instanceof com.simplebuilding.modules.simplelayers.block.ExitDoorBlock, "exit door");
        h.succeed();
    }

    /** Almond Water heals 2 hearts (Instant Health I) and takes Nausea away; the bottle stays. */
    static void almondWater(GameTestHelper h) {
        Pig pig = h.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 2));
        pig.setHealth(4.0F);
        pig.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NAUSEA, 200));
        ItemStack water = new ItemStack(LayersRegistry.ALMOND_WATER);
        h.assertTrue(water.has(DataComponents.CONSUMABLE) && water.has(DataComponents.USE_REMAINDER), "drink with a bottle left");
        water.get(DataComponents.CONSUMABLE).onConsume(h.getLevel(), pig, water);
        h.assertTrue(pig.getHealth() >= 7.9F, "healed to " + pig.getHealth());
        h.assertFalse(pig.hasEffect(net.minecraft.world.effect.MobEffects.NAUSEA), "nausea gone");
        h.succeed();
    }

    static void guideBook(GameTestHelper h) {
        h.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath(SimpleLayers.MOD_ID, "guide_book")), "guide book");
        h.succeed();
    }
}
