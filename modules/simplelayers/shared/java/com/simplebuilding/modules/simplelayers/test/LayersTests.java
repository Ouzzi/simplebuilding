package com.simplebuilding.modules.simplelayers.test;

import com.simplebuilding.modules.simplelayers.LayersRegistry;
import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.block.BackroomBlock;
import com.simplebuilding.modules.simplelayers.block.CautionSignBlock;
import com.simplebuilding.modules.simplelayers.block.GlitchedBedrockBlock;
import com.simplebuilding.modules.simplelayers.block.LevelDoorBlock;
import com.simplebuilding.modules.simplelayers.block.NeonLightBlock;
import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import com.simplebuilding.modules.simplelayers.world.BackroomEffect;
import com.simplebuilding.modules.simplelayers.world.BackroomsChunkGenerator;
import com.simplebuilding.modules.simplelayers.world.BackroomsEntries;
import com.simplebuilding.modules.simplelayers.world.BackroomsGrid;
import com.simplebuilding.modules.simplelayers.world.BackroomsTravel;
import com.simplebuilding.modules.simplelayers.world.BackroomsAmbience;
import com.simplebuilding.modules.simplelayers.world.CloudTransition;
import com.simplebuilding.modules.simplelayers.world.EntranceShaftFeature;
import com.simplebuilding.modules.simplelayers.world.gen.BackroomsLevel;
import com.simplebuilding.modules.simplelayers.world.gen.EscapeCheck;
import com.simplebuilding.modules.simplelayers.world.gen.LobbyGen;
import com.simplebuilding.modules.simplelayers.world.gen.Piece;
import com.simplebuilding.modules.simplelayers.world.gen.PipesGen;
import com.simplebuilding.modules.simplelayers.world.gen.ShaftTemplate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
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
        ALL.put("cloud_light_properties", LayersTests::cloudLightProperties);
        ALL.put("cloud_roof_lets_light_through", LayersTests::cloudRoofLetsLightThrough);
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
        ALL.put("backroom_effect_blocks_mining", LayersTests::backroomEffectBlocksMining);
        ALL.put("netherite_piston_moves_wall", LayersTests::netheritePistonMovesWall);
        ALL.put("bedrock_hull", LayersTests::bedrockHull);
        ALL.put("supplies_generated", LayersTests::suppliesGenerated);
        ALL.put("levels_registered", LayersTests::levelsRegistered);
        ALL.put("level_transitions", LayersTests::levelTransitions);
        ALL.put("easiest_exit_reachable", LayersTests::easiestExitReachable);
        ALL.put("levels_have_free_arrival", LayersTests::levelsHaveFreeArrival);
        ALL.put("entrance_shaft_generated", LayersTests::entranceShaftGenerated);
        ALL.put("sealed_hatch_puzzle", LayersTests::sealedHatchPuzzle);
        ALL.put("ambience_sounds", LayersTests::ambienceSounds);
        ALL.put("level_blocks_protected", LayersTests::levelBlocksProtected);
    }

    private LayersTests() {}

    /**
     * The level of {@code key}, or null after checking what the test server can hold: Vanilla's GameTestServer bakes its world
     * WITHOUT data pack dimensions ({@code noDatapackDimensions} in GameTestServer), so in the standalone run (no Simple Dimensions,
     * whose pack mixin adds them) the levels do not exist. Then the dimension types and biomes must still be loaded from the
     * module's data, and the caller ends the test; the integration run (levels present) checks the travel itself.
     */
    static ServerLevel levelOrRegistryOnly(GameTestHelper h, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key) {
        ServerLevel level = h.getLevel().getServer().getLevel(key);
        if (level != null) return level;
        var types = h.getLevel().registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);
        var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        for (String id : List.of("cloud_sea", "backrooms", "backrooms_warehouse", "backrooms_pipes", "backrooms_pools", "backrooms_end")) {
            h.assertTrue(types.containsKey(SimpleLayers.id(id)), "dimension type " + id);
            h.assertTrue(biomes.containsKey(SimpleLayers.id(id)), "biome " + id);
        }
        h.succeed();
        return null;
    }

    static void registered(GameTestHelper h) {
        List<String> missing = new ArrayList<>();
        for (String path : LayersRegistry.ITEM_PATHS) if (!BuiltInRegistries.ITEM.containsKey(SimpleLayers.id(path))) missing.add("item " + path);
        for (String path : List.of("exit_door", "rift", "level_door")) {
            if (!BuiltInRegistries.BLOCK.containsKey(SimpleLayers.id(path))) missing.add("block " + path);
            if (BuiltInRegistries.ITEM.containsKey(SimpleLayers.id(path))) missing.add("no item expected for " + path);
        }
        if (!BuiltInRegistries.CHUNK_GENERATOR.containsKey(SimpleLayers.id("backrooms"))) missing.add("chunk generator");
        for (String f : List.of("mist_pit", "sky_chest", "glitched_bedrock", "entrance_shaft", "backrooms_cache")) if (!BuiltInRegistries.FEATURE_TYPE.containsKey(SimpleLayers.id(f))) missing.add("feature " + f);
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

    /**
     * CLOUDS15 (plan docs/ai/PLAN-B-CLOUDS15-2026-11-09.md): Cloud Block and Mist pass light like Glass (dampening 0), Cloud Stone like
     * Ice (dampening 1, no longer a solid render); all stay full (Cloud/Stone) and keep the spawn rule (no mob spawns on them).
     */
    static void cloudLightProperties(GameTestHelper h) {
        BlockPos rel = new BlockPos(2, 2, 2), abs = h.absolutePos(rel);
        ServerLevel level = h.getLevel();
        int[] expected = {0, 1, 0};
        Block[] blocks = {LayersRegistry.CLOUD_BLOCK, LayersRegistry.CLOUD_STONE, LayersRegistry.MIST_BLOCK};
        for (int i = 0; i < blocks.length; i++) {
            h.setBlock(rel, blocks[i]);
            BlockState s = level.getBlockState(abs);
            h.assertTrue(s.getLightDampening() == expected[i], blocks[i] + " light dampening " + s.getLightDampening() + " != " + expected[i]);
            h.assertFalse(s.isSolidRender(), blocks[i] + " must not be a solid render (would block light)");
            h.assertFalse(s.isValidSpawn(level, abs, EntityTypes.ZOMBIE), blocks[i] + " spawn rule unchanged (no spawns)");
            if (blocks[i] != LayersRegistry.MIST_BLOCK) {
                h.assertTrue(s.isCollisionShapeFullBlock(level, abs), blocks[i] + " still a full, walkable block");
            }
        }
        h.assertTrue(level.getBlockState(abs).propagatesSkylightDown(), "mist lets sky light straight down");
        h.succeed();
    }

    /**
     * CLOUDS15: three closed stone boxes (inside 1 x 2 x 1) with a roof of Stone, Cloud Stone and Cloud Block. Sky light just under the roof:
     * Stone 0, Cloud Stone above - 2 (roof -1, air -1), Cloud Block = above. Needs open sky over the test (above >= 2); without it only
     * the stone box is checked.
     */
    static void cloudRoofLetsLightThrough(GameTestHelper h) {
        BlockPos[] origins = {new BlockPos(0, 0, 0), new BlockPos(4, 0, 0), new BlockPos(0, 0, 4)};
        Block[] roofs = {Blocks.STONE, LayersRegistry.CLOUD_STONE, LayersRegistry.CLOUD_BLOCK};
        for (int b = 0; b < origins.length; b++) {
            for (int x = 0; x < 3; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 3; z++) {
                BlockPos p = origins[b].offset(x, y, z);
                boolean inside = x == 1 && z == 1 && (y == 1 || y == 2);
                h.setBlock(p, inside ? Blocks.AIR : y == 3 ? roofs[b] : Blocks.STONE);
            }
        }
        ServerLevel level = h.getLevel();
        h.succeedWhen(() -> {
            int[] in = new int[3];
            int above = level.getBrightness(LightLayer.SKY, h.absolutePos(origins[1].offset(1, 4, 1)));
            for (int b = 0; b < 3; b++) in[b] = level.getBrightness(LightLayer.SKY, h.absolutePos(origins[b].offset(1, 2, 1)));
            h.assertTrue(in[0] == 0, "stone roof: no sky light inside, got " + in[0]);
            if (above < 2) return;
            h.assertTrue(in[1] == above - 2, "cloud stone roof: sky light " + in[1] + ", expected " + (above - 2) + " (above " + above + ")");
            h.assertTrue(in[2] == above, "cloud block roof: sky light " + in[2] + ", expected " + above);
        });
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
        ServerLevel clouds = levelOrRegistryOnly(h, SimpleLayers.CLOUD_SEA);
        if (clouds == null) return;
        ServerLevel back = levelOrRegistryOnly(h, SimpleLayers.BACKROOMS);
        if (back == null) return;
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
        if (levelOrRegistryOnly(h, SimpleLayers.CLOUD_SEA) == null) return;
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

    /**
     * Stage 5 + BACK18: the grid is deterministic, closed by bedrock, carpet below, every arrival free, and about 70 % of the lines
     * have doors (40 % random + the escape tree's forced door per cell).
     */
    static void backroomsGrid(GameTestHelper h) {
        long seed = 12345L;
        LobbyGen.Style s = LobbyGen.LOBBY;
        java.util.Random random = new java.util.Random(7);
        int doors = 0, edges = 0;
        for (int i = 0; i < 400; i++) {
            int x = random.nextInt(20000) - 10000, z = random.nextInt(20000) - 10000;
            h.assertTrue(BackroomsGrid.at(seed, x, 0, z).is(Blocks.BEDROCK), "bedrock floor");
            h.assertTrue(BackroomsGrid.at(seed, x, 1, z).is(LayersRegistry.DAMP_CARPET), "carpet floor");
            h.assertTrue(BackroomsGrid.at(seed, x, 4, z) == BackroomsGrid.at(seed, x, 4, z), "deterministic");
            int[] a = LobbyGen.arrival(s, seed, x, z);
            for (int y = 2; y <= 3; y++) h.assertTrue(LobbyGen.at(s, seed, a[0], y, a[2]).passable(), "arrival free at y " + y + " " + a[0] + " " + a[2]);
            for (int cell = 0; cell < 2; cell++) {
                LobbyGen.Edge e = cell == 0 ? LobbyGen.west(s, seed, 0, Math.floorDiv(x, 8), Math.floorDiv(z, 8)) : LobbyGen.north(s, seed, 0, Math.floorDiv(x, 8), Math.floorDiv(z, 8));
                if (e != LobbyGen.Edge.OPEN) {
                    edges++;
                    if (e == LobbyGen.Edge.DOOR) doors++;
                }
            }
        }
        double share = doors / (double) edges;
        h.assertTrue(share > 0.6 && share < 0.8, "door share " + share);
        BlockState light = BackroomsGrid.at(seed, 3, BackroomsGrid.CEILING_Y, 4);
        h.assertTrue(light.is(LayersRegistry.NEON_LIGHT) && NeonLightBlock.light(light) >= 0, "neon light in the ceiling");
        h.succeed();
    }
    /** Stage 5: a generated Backrooms chunk holds exactly the grid (generator + world seed). */
    static void backroomsChunkMatchesGrid(GameTestHelper h) {
        ServerLevel back = levelOrRegistryOnly(h, SimpleLayers.BACKROOMS);
        if (back == null) return;
        long seed = BackroomsChunkGenerator.seed(back.getChunkSource().randomState());
        int cx = 4000, cz = -4000;
        back.getChunk(cx, cz);
        List<String> wrong = new ArrayList<>();
        for (int x = 0; x < 16; x += 3) {
            for (int z = 0; z < 16; z += 5) {
                for (int y = 0; y <= BackroomsGrid.HULL_TOP_Y; y++) {
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
        // 600 ticks (30 s) standing; the mock player floats, and Vanilla divides by 5 off the ground (3000 ticks).
        float standing = player.onGround() ? progress : progress * 5.0F;
        h.assertTrue(standing > 1.0F / 700.0F && standing < 1.0F / 500.0F, "about 600 ticks, progress " + progress);
        h.getLevel().removeBlock(abs, false);
        LayersRegistry.GLITCHED_BEDROCK.destroy(h.getLevel(), abs, s);
        h.assertTrue(h.getLevel().getBlockState(abs).is(LayersRegistry.RIFT), "rift left behind");
        h.assertTrue(h.getLevel().getBlockState(abs).getCollisionShape(h.getLevel(), abs).isEmpty(), "rift without collision");
        h.succeed();
    }

    /** Stage 5 entrance: whatever enters the Backrooms arrives in a free cell centre at y 2. */
    static void riftLeadsIntoBackrooms(GameTestHelper h) {
        if (levelOrRegistryOnly(h, SimpleLayers.BACKROOMS) == null) return;
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
        h.assertFalse(pig.hasEffect(net.minecraft.world.effect.MobEffects.NAUSEA), "nausea gone");
        // Instant Health from a consume effect is applied on the entity's next effect tick.
        h.succeedWhen(() -> h.assertTrue(pig.getHealth() >= 7.9F, "healed to " + pig.getHealth()));
    }

    static void guideBook(GameTestHelper h) {
        h.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath(SimpleLayers.MOD_ID, "guide_book")), "guide book");
        h.succeed();
    }

    /** LAYERS13: the Backroom effect stops survival mining of every room block; without it (or in creative) they break normally. */
    static void backroomEffectBlocksMining(GameTestHelper h) {
        h.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(SimpleLayers.id("backroom")) && BackroomEffect.holder() != null, "effect registered");
        Player player = h.makeMockPlayer(GameType.SURVIVAL), creative = h.makeMockPlayer(GameType.CREATIVE);
        h.assertFalse(BackroomEffect.due(player), "not handed out outside the Backrooms");
        BlockPos rel = new BlockPos(2, 2, 2), abs = h.absolutePos(rel);
        List<Block> walls = List.of(LayersRegistry.WALLPAPER, LayersRegistry.STRIPED_WALLPAPER, LayersRegistry.DAMP_CARPET, LayersRegistry.CEILING_TILE,
                LayersRegistry.NEON_LIGHT, LayersRegistry.EXIT_SIGN);
        for (Block b : walls) {
            h.setBlock(rel, b);
            h.assertTrue(h.getLevel().getBlockState(abs).getDestroyProgress(player, h.getLevel(), abs) > 0.0F, b + " breaks without the effect");
        }
        player.addEffect(new MobEffectInstance(BackroomEffect.holder(), 200));
        creative.addEffect(new MobEffectInstance(BackroomEffect.holder(), 200));
        for (Block b : walls) {
            h.setBlock(rel, b);
            BlockState s = h.getLevel().getBlockState(abs);
            h.assertTrue(s.getDestroyProgress(player, h.getLevel(), abs) == 0.0F, b + " holds with the effect");
            h.assertTrue(s.getDestroyProgress(creative, h.getLevel(), abs) > 0.0F, b + " creative ignores the effect");
        }
        h.setBlock(rel, Blocks.BEDROCK);
        h.assertTrue(h.getLevel().getBlockState(abs).getDestroyProgress(player, h.getLevel(), abs) == 0.0F, "the bedrock hull holds anyway");
        h.setBlock(rel, Blocks.STRIPPED_OAK_LOG);
        h.assertTrue(h.getLevel().getBlockState(abs).getDestroyProgress(player, h.getLevel(), abs) > 0.0F, "supplies stay breakable");
        h.succeed();
    }

    /**
     * LAYERS13: in the Backrooms no piston moves a room block (outside they do); Simple Machines' Netherite Piston breaks one anyway.
     * Without Simple Machines (standalone) only the piston rule is checked.
     */
    static void netheritePistonMovesWall(GameTestHelper h) {
        BlockState paper = LayersRegistry.WALLPAPER.defaultBlockState();
        h.assertTrue(PistonBaseBlock.isPushable(paper, h.getLevel(), h.absolutePos(new BlockPos(2, 2, 2)), Direction.UP, false, Direction.UP),
                "outside the Backrooms a piston moves wallpaper");
        ServerLevel back = h.getLevel().getServer().getLevel(SimpleLayers.BACKROOMS);
        if (back != null) {
            BlockPos p = new BlockPos(64004, 3, -64004);
            h.assertFalse(PistonBaseBlock.isPushable(paper, back, p, Direction.EAST, false, Direction.EAST), "held in the Backrooms");
            h.assertTrue(PistonBaseBlock.isPushable(Blocks.OAK_PLANKS.defaultBlockState(), back, p, Direction.EAST, false, Direction.EAST),
                    "other blocks still move in the Backrooms");
        }
        Identifier pistonId = Identifier.fromNamespaceAndPath("simplemachines", "netherite_piston");
        if (!BuiltInRegistries.BLOCK.containsKey(pistonId)) {
            h.succeed();
            return;
        }
        BlockPos piston = new BlockPos(2, 1, 2), wall = piston.above();
        h.setBlock(wall.above(), Blocks.AIR);
        h.setBlock(piston, BuiltInRegistries.BLOCK.getValue(pistonId).defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP));
        h.setBlock(wall, LayersRegistry.WALLPAPER);
        BlockPos lever = piston.east();
        h.setBlock(lever.below(), Blocks.STONE);
        h.setBlock(lever, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.POWERED, Boolean.TRUE));
        h.succeedWhen(() -> h.assertFalse(h.getBlockState(wall).is(LayersRegistry.WALLPAPER), "the Netherite Piston removed the wall"));
    }

    /** LAYERS13 + BACK18: bedrock at y 0, the slab y 8 between the floors (open only in stairwells) and y 16-63; in a generated chunk too. */
    static void bedrockHull(GameTestHelper h) {
        long seed = 99L;
        LobbyGen.Style s = LobbyGen.LOBBY;
        java.util.Random random = new java.util.Random(3);
        for (int i = 0; i < 200; i++) {
            int x = random.nextInt(40000) - 20000, z = random.nextInt(40000) - 20000;
            boolean stair = LobbyGen.stairwell(s, seed, Math.floorDiv(x, 8), Math.floorDiv(z, 8));
            h.assertTrue(BackroomsGrid.at(seed, x, 0, z).is(Blocks.BEDROCK), "floor y 0");
            h.assertTrue(stair || BackroomsGrid.at(seed, x, BackroomsGrid.TOP_Y, z).is(Blocks.BEDROCK), "slab y 8");
            for (int y = BackroomsGrid.HULL_FROM_Y; y <= BackroomsGrid.HULL_TOP_Y; y++) h.assertTrue(BackroomsGrid.at(seed, x, y, z).is(Blocks.BEDROCK), "hull y " + y);
            for (int y = 1; y < BackroomsGrid.HULL_FROM_Y; y++) {
                if (y != BackroomsGrid.TOP_Y) h.assertFalse(BackroomsGrid.at(seed, x, y, z).is(Blocks.BEDROCK), "no bedrock inside at y " + y);
            }
            h.assertTrue(BackroomsGrid.at(seed, x, -1, z).isAir() && BackroomsGrid.at(seed, x, BackroomsGrid.HULL_TOP_Y + 1, z).isAir(), "outside the world");
            for (BackroomsLevel level : BackroomsLevel.values()) {
                h.assertTrue(level.at(seed, x, 0, z) == Piece.BEDROCK && level.at(seed, x, 63, z) == Piece.BEDROCK, level + " hull at y 0 and 63");
            }
        }
        ServerLevel back = levelOrRegistryOnly(h, SimpleLayers.BACKROOMS);
        if (back == null) return;
        h.assertTrue(back.getMaxY() == BackroomsGrid.HULL_TOP_Y, "hull reaches the build limit " + back.getMaxY());
        int cx = -3000, cz = 2500;
        back.getChunk(cx, cz);
        for (int x = 0; x < 16; x += 5) {
            for (int z = 0; z < 16; z += 5) {
                for (int y : new int[] {0, 8, 16, 30, 63}) {
                    BlockPos p = new BlockPos(cx * 16 + x, y, cz * 16 + z);
                    boolean stair = y == 8 && LobbyGen.stairwell(LobbyGen.LOBBY, BackroomsChunkGenerator.seed(back.getChunkSource().randomState()),
                            Math.floorDiv(p.getX(), 8), Math.floorDiv(p.getZ(), 8));
                    h.assertTrue(back.getBlockState(p).is(Blocks.BEDROCK) || stair || inSpecialRoom(back, p), "generated hull at " + p.toShortString());
                }
            }
        }
        h.succeed();
    }

    /**
     * LAYERS13: offices (3 oak logs, desk, chair) and cobweb cells appear at the planned shares, only in the corners; config off =
     * none. BACK18: exit cells, signed cells and stairwells hold their own blocks and no supplies (skipped in the corner check).
     */
    static void suppliesGenerated(GameTestHelper h) {
        long seed = 4242L;
        LobbyGen.Style st = LobbyGen.LOBBY;
        boolean before = LobbyGen.supplies;
        try {
            LobbyGen.supplies = true;
            int cells = 0, offices = 0, webCells = 0, tables = 0;
            for (int cx = -30; cx < 30; cx++) {
                for (int cz = -30; cz < 30; cz++) {
                    cells++;
                    LobbyGen.Supply supply = LobbyGen.supply(st, seed, 0, cx, cz);
                    if (supply == LobbyGen.Supply.OFFICE) offices++;
                    if (supply == LobbyGen.Supply.WEBS) webCells++;
                    if (LobbyGen.exitCell(st, seed, cx, cz) || LobbyGen.signed(st, seed, cx, cz) || LobbyGen.stairwell(st, seed, cx, cz)) {
                        h.assertTrue(supply == LobbyGen.Supply.NONE || LobbyGen.signed(st, seed, cx, cz), "no supplies in exit/stair cell " + cx + " " + cz);
                        continue;
                    }
                    int logs = 0, webs = 0, wood = 0;
                    for (int lx = 1; lx < BackroomsGrid.CELL; lx++) {
                        for (int lz = 1; lz < BackroomsGrid.CELL; lz++) {
                            for (int y = 2; y < BackroomsGrid.CEILING_Y; y++) {
                                BlockState s = BackroomsGrid.at(seed, cx * BackroomsGrid.CELL + lx, y, cz * BackroomsGrid.CELL + lz);
                                if (s.isAir()) continue;
                                h.assertFalse(lx >= 3 && lx <= 5 || lz >= 3 && lz <= 5, "supply outside the corners at " + lx + " " + y + " " + lz);
                                if (s.is(Blocks.STRIPPED_OAK_LOG)) logs++;
                                if (s.is(Blocks.COBWEB)) webs++;
                                if (s.is(Blocks.CRAFTING_TABLE)) tables++;
                                if (s.is(Blocks.OAK_SLAB) || s.is(Blocks.OAK_STAIRS)) wood++;
                            }
                        }
                    }
                    if (supply == LobbyGen.Supply.OFFICE) {
                        h.assertTrue(logs == 3 && wood == 3 && webs == 0, "office " + cx + " " + cz + ": logs " + logs + ", desk/chair " + wood);
                    } else if (supply == LobbyGen.Supply.WEBS) {
                        h.assertTrue(webs >= 12 && logs == 0, "cobweb cell " + cx + " " + cz + ": " + webs);
                    } else {
                        h.assertTrue(logs + webs + wood == 0, "empty cell " + cx + " " + cz);
                    }
                }
            }
            double officeShare = offices / (double) cells, webShare = webCells / (double) cells;
            h.assertTrue(officeShare > 0.03 && officeShare < 0.08, "office share " + officeShare);
            h.assertTrue(webShare > 0.04 && webShare < 0.14, "cobweb share " + webShare);
            h.assertTrue(tables > 0, "some crafting tables");
            LobbyGen.supplies = false;
            for (int cx = -10; cx < 10; cx++) h.assertTrue(LobbyGen.supply(st, seed, 0, cx, 3) == LobbyGen.Supply.NONE, "config off: no supplies");
        } finally {
            LobbyGen.supplies = before;
        }
        h.succeed();
    }

    // ---------------------------------------------------------------- BACK18 (plan docs/ai/PLAN-B-BACK18-2026-11-10.md)

    /** BACK18: the four new levels have dimension types and biomes; loaded, each runs the Backrooms generator with its level. */
    static void levelsRegistered(GameTestHelper h) {
        var types = h.getLevel().registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);
        var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        for (BackroomsLevel level : BackroomsLevel.values()) {
            h.assertTrue(types.containsKey(SimpleLayers.id(level.dimension)) && biomes.containsKey(SimpleLayers.id(level.dimension)), "type and biome " + level);
            h.assertTrue(SimpleLayers.levelOf(SimpleLayers.levelKey(level)) == level, "key round trip " + level);
        }
        h.assertTrue(SimpleLayers.levelOf(net.minecraft.world.level.Level.OVERWORLD) == null, "the Overworld is no Backrooms level");
        for (BackroomsLevel level : BackroomsLevel.values()) {
            ServerLevel l = levelOrRegistryOnly(h, SimpleLayers.levelKey(level));
            if (l == null) return;
            h.assertTrue(l.getChunkSource().getGenerator() instanceof BackroomsChunkGenerator g && g.level() == level, "generator of " + level);
            h.assertTrue(l.getMinY() == 0 && l.getHeight() == 64 && l.dimensionType().hasCeiling(), "64 high with a ceiling: " + level);
        }
        h.succeed();
    }

    /** BACK18: where rifts and doors lead; loaded, an entity travels through every level and arrives on free ground. */
    static void levelTransitions(GameTestHelper h) {
        h.assertTrue(BackroomsLevel.LOBBY.riftTarget(2) == BackroomsLevel.WAREHOUSE, "lobby wall rift -> warehouse");
        h.assertTrue(BackroomsLevel.WAREHOUSE.riftTarget(2) == BackroomsLevel.PIPES, "warehouse wall rift -> pipes");
        h.assertTrue(BackroomsLevel.WAREHOUSE.riftTarget(1) == BackroomsLevel.POOLS && BackroomsLevel.PIPES.riftTarget(1) == BackroomsLevel.POOLS, "holes -> poolrooms");
        h.assertTrue(BackroomsLevel.PIPES.deeper() == BackroomsLevel.END && BackroomsLevel.POOLS.deeper() == null && BackroomsLevel.END.deeper() == null, "deeper chain");
        MinecraftServer server = h.getLevel().getServer();
        for (BackroomsLevel level : BackroomsLevel.values()) if (levelOrRegistryOnly(h, SimpleLayers.levelKey(level)) == null) return;
        ArmorStand stand = h.spawn(EntityTypes.ARMOR_STAND, new Vec3(1.5, 2.0, 1.5));
        Entity e = stand;
        for (BackroomsLevel level : List.of(BackroomsLevel.LOBBY, BackroomsLevel.WAREHOUSE, BackroomsLevel.PIPES, BackroomsLevel.POOLS, BackroomsLevel.END)) {
            e.setPortalCooldown(0);
            Entity moved = BackroomsTravel.travel(server, e, level);
            h.assertTrue(moved != null && SimpleLayers.levelOf(moved.level()) == level, "arrived in " + level);
            BlockPos at = moved.blockPosition();
            h.assertTrue(moved.level().getBlockState(at).getCollisionShape(moved.level(), at).isEmpty()
                    && moved.level().getBlockState(at.above()).getCollisionShape(moved.level(), at.above()).isEmpty(), "free arrival in " + level + " at " + at.toShortString());
            e = moved;
        }
        // A wall rift of the Lobby leads to the Warehouse.
        e.setPortalCooldown(0);
        Entity lobby = BackroomsTravel.travel(server, e, BackroomsLevel.LOBBY);
        lobby.setPortalCooldown(0);
        ServerLevel lobbyLevel = (ServerLevel) lobby.level();
        BackroomsTravel.enterFromRift(lobbyLevel, lobby, lobby.blockPosition());
        // A cross-dimension teleport removes the old instance; the copy stands in the Warehouse.
        h.assertTrue(lobby.isRemoved(), "lobby rift -> left the lobby");
        ServerLevel warehouse = server.getLevel(SimpleLayers.levelKey(BackroomsLevel.WAREHOUSE));
        int[] spot = BackroomsLevel.WAREHOUSE.arrival(BackroomsChunkGenerator.seed(warehouse.getChunkSource().randomState()), lobby.getBlockX(), lobby.getBlockZ());
        var found = warehouse.getEntitiesOfClass(ArmorStand.class, new net.minecraft.world.phys.AABB(spot[0] - 1, spot[1] - 1, spot[2] - 1, spot[0] + 2, spot[1] + 3, spot[2] + 2));
        h.assertTrue(!found.isEmpty(), "lobby rift -> warehouse at " + spot[0] + " " + spot[1] + " " + spot[2]);
        found.forEach(Entity::discard);
        h.succeed();
    }

    /**
     * BACK18: the quick way out - from anywhere in Level 0 an exit cell lies at most 18 cells away (escape tree), the exit cell holds
     * the Exit Door under an EXIT sign, the cells of its row and column show the way with a sign; the other levels' goals too.
     */
    static void easiestExitReachable(GameTestHelper h) {
        long seed = 777L;
        LobbyGen.Style s = LobbyGen.LOBBY;
        java.util.Random random = new java.util.Random(11);
        int worst = 0;
        for (int i = 0; i < 300; i++) {
            int x = random.nextInt(200000) - 100000, z = random.nextInt(200000) - 100000;
            int d = EscapeCheck.lobbyToExit(seed, x, z, 18);
            h.assertTrue(d >= 0 && d <= 18, "exit within 18 cells from " + x + " " + z + ": " + d);
            worst = Math.max(worst, d);
            int end = EscapeCheck.endToExit(seed, x, z, 4), pools = EscapeCheck.poolsToExit(seed, x, z, 10), pipes = EscapeCheck.pipesToHatch(seed, x, z, 10);
            h.assertTrue(end >= 0 && pools >= 0 && pipes >= 0, "exit level " + end + ", poolrooms " + pools + ", pipes hatch " + pipes);
        }
        int found = 0;
        for (int cx = 0; cx < 48 && found < 3; cx++) {
            for (int cz = 0; cz < 48 && found < 3; cz++) {
                if (!LobbyGen.exitCell(s, seed, cx, cz)) continue;
                found++;
                int bx = cx * 8, bz = cz * 8;
                h.assertTrue(LobbyGen.at(s, seed, bx + 4, 2, bz + 4) == Piece.EXIT_DOOR_LOWER && LobbyGen.at(s, seed, bx + 4, 3, bz + 4) == Piece.EXIT_DOOR_UPPER
                        && LobbyGen.at(s, seed, bx + 4, 4, bz + 4) == Piece.EXIT_SIGN, "exit door under the sign in cell " + cx + " " + cz);
                int signs = 0;
                for (int k = 1; k <= 3; k++) {
                    for (int[] c : new int[][] {{cx + k, cz}, {cx - k, cz}, {cx, cz + k}, {cx, cz - k}}) {
                        if (!LobbyGen.signed(s, seed, c[0], c[1])) continue;
                        for (int lx = 1; lx < 8; lx++) for (int lz = 1; lz < 8; lz++) if (LobbyGen.at(s, seed, c[0] * 8 + lx, 6, c[1] * 8 + lz) == Piece.EXIT_SIGN) signs++;
                    }
                }
                h.assertTrue(signs > 0, "EXIT signs near exit cell " + cx + " " + cz);
            }
        }
        h.assertTrue(found > 0, "exit cells exist");
        h.succeed();
    }

    /** BACK18: every level's arrival spot is free (feet and head) on solid ground (or in shallow water); 150 places each. */
    static void levelsHaveFreeArrival(GameTestHelper h) {
        long seed = 31337L;
        java.util.Random random = new java.util.Random(5);
        for (BackroomsLevel level : BackroomsLevel.values()) {
            for (int i = 0; i < 150; i++) {
                int x = random.nextInt(100000) - 50000, z = random.nextInt(100000) - 50000;
                int[] p = level.arrival(seed, x, z);
                Piece feet = level.at(seed, p[0], p[1], p[2]), head = level.at(seed, p[0], p[1] + 1, p[2]), below = level.at(seed, p[0], p[1] - 1, p[2]);
                h.assertTrue(feet.passable() && feet != Piece.RIFT && head.passable(), level + " arrival blocked at " + p[0] + " " + p[1] + " " + p[2] + ": " + feet + "/" + head);
                h.assertTrue(!below.passable() || below == Piece.WATER, level + " arrival floats at " + p[0] + " " + p[1] + " " + p[2] + ": " + below);
                h.assertTrue(Math.floorDiv(p[0], 64) == Math.floorDiv(x, 64) || Math.abs(p[0] - x) < 16, "arrival stays near x/z");
            }
        }
        h.succeed();
    }

    /** BACK18: the entrance shaft has its rift floor (9), ladder, barrier tape, 4 caution signs facing out and 4 warning lights. */
    static void entranceShaftGenerated(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        BlockPos ground = new BlockPos(o.getX() + 1600, Math.max(o.getY() + 20, level.getMinY() + ShaftTemplate.DEPTH + 4), o.getZ() + 8);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.getChunk((ground.getX() >> 4) + dx, (ground.getZ() >> 4) + dz);
        EntranceShaftFeature.build(level, ground);
        int rifts = 0, signs = 0, tape = 0, lamps = 0, ladders = 0;
        for (int dy = -ShaftTemplate.DEPTH; dy <= ShaftTemplate.HEIGHT; dy++) {
            for (int dx = -ShaftTemplate.RADIUS; dx <= ShaftTemplate.RADIUS; dx++) {
                for (int dz = -ShaftTemplate.RADIUS; dz <= ShaftTemplate.RADIUS; dz++) {
                    BlockState st = level.getBlockState(ground.offset(dx, dy, dz));
                    if (st.is(LayersRegistry.RIFT)) rifts++;
                    if (st.is(LayersRegistry.CAUTION_SIGN)) {
                        signs++;
                        Direction out = st.getValue(CautionSignBlock.FACING);
                        h.assertTrue(dx * out.getStepX() + dz * out.getStepZ() > 0, "sign faces out at " + dx + " " + dz);
                    }
                    if (st.is(LayersRegistry.CAUTION_TAPE)) tape++;
                    if (st.is(Blocks.COPPER_BULB.waxed().unaffected())) lamps++;
                    if (st.is(Blocks.LADDER)) ladders++;
                }
            }
        }
        h.assertTrue(rifts == 9 && level.getBlockState(ground.offset(0, ShaftTemplate.PORTAL_DY, 0)).is(LayersRegistry.RIFT), "rift floor " + rifts);
        h.assertTrue(signs == 4 && lamps == 4 && tape >= 24 && ladders >= 10, "signs " + signs + ", lamps " + lamps + ", tape " + tape + ", ladders " + ladders);
        h.assertTrue(level.getBlockState(ground.offset(1, 0, 1)).isAir() && level.getBlockState(ground.offset(1, -5, 1)).isAir(), "the shaft is open");
        h.assertTrue(LayersConfig.shaftRarity > 0, "shafts on by default");
        h.succeed();
    }

    /** BACK18: the sealed hatch counts the right valves (north, east, west lever) against its pattern; never all off. */
    static void sealedHatchPuzzle(GameTestHelper h) {
        BlockPos lower = new BlockPos(3, 1, 3);
        h.setBlock(lower, LayersRegistry.LEVEL_DOOR.defaultBlockState().setValue(LevelDoorBlock.SEALED, Boolean.TRUE));
        h.setBlock(lower.above(), LayersRegistry.LEVEL_DOOR.defaultBlockState().setValue(LevelDoorBlock.SEALED, Boolean.TRUE)
                .setValue(LevelDoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        BlockPos abs = h.absolutePos(lower);
        int pattern = LevelDoorBlock.pattern(h.getLevel().getSeed(), abs);
        h.assertTrue(pattern >= 1 && pattern <= 7, "pattern " + pattern);
        for (int i = 0; i < 3; i++) {
            Direction d = LevelDoorBlock.VALVES[i];
            h.setBlock(lower.relative(d), Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, d)
                    .setValue(LeverBlock.POWERED, (pattern >> i & 1) == 1));
        }
        h.assertTrue(LevelDoorBlock.rightValves(h.getLevel(), abs, pattern) == 3, "all valves right");
        BlockPos north = lower.relative(Direction.NORTH);
        h.setBlock(north, h.getBlockState(north).setValue(LeverBlock.POWERED, (pattern & 1) == 0));
        h.assertTrue(LevelDoorBlock.rightValves(h.getLevel(), abs, pattern) == 2, "one valve wrong");
        java.util.Random random = new java.util.Random(2);
        for (int i = 0; i < 200; i++) {
            int p = PipesGen.pattern(random.nextLong(), random.nextInt(2000) - 1000, random.nextInt(2000) - 1000);
            h.assertTrue(p >= 1 && p <= 7, "never all off: " + p);
        }
        // the generated hatch: door and three levers in the root node of a pipes region
        long seed = 5L;
        for (int nx = 0; nx < PipesGen.REGION; nx++) {
            for (int nz = 0; nz < PipesGen.REGION; nz++) {
                if (PipesGen.node(seed, 0, nx, nz) != PipesGen.Node.HATCH) continue;
                int x = nx * PipesGen.NODE, z = nz * PipesGen.NODE;
                h.assertTrue(PipesGen.at(seed, x + 3, 2, z + 3) == Piece.SEALED_DOOR_LOWER && PipesGen.at(seed, x + 3, 2, z + 2) == Piece.LEVER_N
                        && PipesGen.at(seed, x + 4, 2, z + 3) == Piece.LEVER_E && PipesGen.at(seed, x + 2, 2, z + 3) == Piece.LEVER_W, "generated hatch");
            }
        }
        h.succeed();
    }

    /** BACK18: every level has a quiet sound set of registered sounds; config 0 is silent, 50 halves the base volume; pauses 20-100 s. */
    static void ambienceSounds(GameTestHelper h) {
        int before = LayersConfig.ambientVolume;
        try {
            for (BackroomsLevel level : BackroomsLevel.values()) {
                List<BackroomsAmbience.Cue> set = BackroomsAmbience.sets().get(level);
                h.assertTrue(set != null && !set.isEmpty(), "sounds for " + level);
                for (BackroomsAmbience.Cue cue : set) {
                    h.assertTrue(cue.sound().isBound() && cue.volume() > 0.0F && cue.volume() <= 0.5F, "quiet registered sound in " + level);
                    LayersConfig.ambientVolume = 0;
                    h.assertTrue(BackroomsAmbience.volume(cue) == 0.0F, "config 0 is silent");
                    LayersConfig.ambientVolume = 50;
                    h.assertTrue(Math.abs(BackroomsAmbience.volume(cue) - cue.volume() / 2) < 1e-6, "config 50 halves");
                }
                net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(1);
                for (int i = 0; i < 50; i++) {
                    int pause = BackroomsAmbience.pause(level, random);
                    h.assertTrue(pause >= 400 && pause <= 2000, "pause " + pause);
                }
            }
        } finally {
            LayersConfig.ambientVolume = before;
        }
        var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        h.assertTrue(biomes.containsKey(SimpleLayers.id("backrooms")), "lobby biome");
        h.succeed();
    }

    /** BACK18: inside the Backrooms the Vanilla blocks of the levels are held with the effect, supplies are not; not outside. */
    static void levelBlocksProtected(GameTestHelper h) {
        h.assertTrue(BackroomBlock.structural(Blocks.TUFF_BRICKS.defaultBlockState()) && BackroomBlock.structural(Blocks.QUARTZ_BRICKS.defaultBlockState())
                && BackroomBlock.structural(LayersRegistry.LEVEL_DOOR.defaultBlockState()), "level blocks are structural");
        h.assertFalse(BackroomBlock.structural(Blocks.OAK_PLANKS.defaultBlockState()) || BackroomBlock.structural(Blocks.COBWEB.defaultBlockState())
                || BackroomBlock.structural(Blocks.CHEST.defaultBlockState()), "supplies are not");
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.addEffect(new MobEffectInstance(BackroomEffect.holder(), 200));
        h.assertFalse(BackroomBlock.heldFor(Blocks.TUFF_BRICKS.defaultBlockState(), player), "outside the Backrooms tuff bricks break");
        BlockPos rel = new BlockPos(2, 2, 2), abs = h.absolutePos(rel);
        h.setBlock(rel, Blocks.TUFF_BRICKS);
        h.assertTrue(h.getLevel().getBlockState(abs).getDestroyProgress(player, h.getLevel(), abs) > 0.0F, "and the mixin lets them");
        h.setBlock(rel, LayersRegistry.LEVEL_DOOR);
        h.assertTrue(h.getLevel().getBlockState(abs).getDestroyProgress(player, h.getLevel(), abs) == 0.0F, "the maintenance door holds with the effect");
        h.succeed();
    }}
