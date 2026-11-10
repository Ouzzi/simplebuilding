package com.simplebuilding.modules.simplelayers.test;

import com.simplebuilding.modules.simplelayers.LayersRegistry;
import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.block.BackroomBlock;
import com.simplebuilding.modules.simplelayers.block.CautionSignBlock;
import com.simplebuilding.modules.simplelayers.block.ChalkBoxItem;
import com.simplebuilding.modules.simplelayers.block.ChalkItem;
import com.simplebuilding.modules.simplelayers.block.ChalkMarkBlock;
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
import com.simplebuilding.modules.simplelayers.world.gen.Maze;
import com.simplebuilding.modules.simplelayers.world.gen.PoolsGen;
import com.simplebuilding.modules.simplelayers.world.gen.WarehouseGen;
import com.simplebuilding.modules.simplelayers.world.gen.Piece;
import com.simplebuilding.modules.simplelayers.world.gen.PipesGen;
import com.simplebuilding.modules.simplelayers.world.gen.HouseTemplate;
import com.simplebuilding.modules.simplelayers.world.BackroomsBlocks;
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
import net.minecraft.world.item.Item;
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
        ALL.put("cloud_face_culling", LayersTests::cloudFaceCulling);
        ALL.put("cloud_texture_layers", LayersTests::cloudTextureLayers);
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
        ALL.put("labyrinth_depth_gradient", LayersTests::labyrinthDepthGradient);
        ALL.put("level_heights_vary", LayersTests::levelHeightsVary);
        ALL.put("chalk_marks", LayersTests::chalkMarks);
        ALL.put("chalk_box", LayersTests::chalkBox);
        ALL.put("chalk_recipes", LayersTests::chalkRecipes);
        ALL.put("stairwells_walkable", LayersTests::stairwellsWalkable);
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
        // CLOUDS17: Cloud Block = Glass (sky light straight down), Cloud Stone = Ice (no straight-down sky light); neither occludes.
        h.assertTrue(LayersRegistry.CLOUD_BLOCK.defaultBlockState().propagatesSkylightDown(), "cloud block lets sky light down like glass");
        h.assertFalse(LayersRegistry.CLOUD_STONE.defaultBlockState().propagatesSkylightDown(), "cloud stone dampens like ice");
        h.assertFalse(LayersRegistry.CLOUD_BLOCK.defaultBlockState().canOcclude(), "cloud block does not occlude (glass)");
        h.assertFalse(LayersRegistry.CLOUD_STONE.defaultBlockState().canOcclude(), "cloud stone does not occlude (ice)");
        h.succeed();
    }

    /**
     * CLOUDS17 (plan docs/ai/PLAN-B-CLOUDS17-2026-11-10.md): face culling like Glass/Ice - same blocks merge; the icy core skips its
     * (translucent, sorted) faces behind the cloud shell, the shell keeps its face toward the core (Glass draws toward other blocks).
     */
    static void cloudFaceCulling(GameTestHelper h) {
        BlockState cloud = LayersRegistry.CLOUD_BLOCK.defaultBlockState(), stone = LayersRegistry.CLOUD_STONE.defaultBlockState();
        BlockState mist = LayersRegistry.MIST_BLOCK.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        for (Direction d : Direction.values()) {
            h.assertTrue(stone.skipRendering(stone, d), "cloud stone hides faces between core blocks (" + d + ")");
            h.assertTrue(stone.skipRendering(cloud, d), "cloud stone hides its face behind the cloud shell (" + d + ")");
            h.assertTrue(cloud.skipRendering(cloud, d), "cloud blocks merge like glass (" + d + ")");
            h.assertTrue(cloud.skipRendering(mist, d), "cloud merges with the mist plug (" + d + ")");
            h.assertFalse(cloud.skipRendering(stone, d), "cloud shell draws its face toward the core (" + d + ")");
            h.assertFalse(stone.skipRendering(air, d), "open core face is drawn (" + d + ")");
            h.assertFalse(cloud.skipRendering(air, d), "open cloud face is drawn (" + d + ")");
        }
        h.succeed();
    }

    /**
     * CLOUDS17: in 26.3 the chunk layer follows the sprite's alpha (only 255 = solid, any 0 = cutout, any 1..254 = translucent). Cloud
     * Stone must be part-transparent everywhere like Ice (translucent), the Cloud Block only clear or opaque with clear gaps like Glass
     * (cutout). Reads the shipped PNGs, so a repainted texture cannot silently change the render type.
     */
    static void cloudTextureLayers(GameTestHelper h) {
        int[] stone = alphaCounts(h, "cloud_stone"), cloud = alphaCounts(h, "cloud_block");
        h.assertTrue(stone[0] == 0 && stone[2] == 0 && stone[1] > 0, "cloud stone: only part-transparent pixels (ice, translucent), got clear "
                + stone[0] + " / part " + stone[1] + " / opaque " + stone[2]);
        h.assertTrue(cloud[1] == 0 && cloud[0] > 0 && cloud[2] > cloud[0], "cloud block: opaque with clear gaps (glass, cutout), got clear "
                + cloud[0] + " / part " + cloud[1] + " / opaque " + cloud[2]);
        h.succeed();
    }

    /** {clear, part-transparent, opaque} pixel counts of a Simple Layers block texture. */
    private static int[] alphaCounts(GameTestHelper h, String name) {
        String path = "/assets/simplelayers/textures/block/" + name + ".png";
        try (java.io.InputStream in = LayersTests.class.getResourceAsStream(path)) {
            h.assertTrue(in != null, "texture on the class path: " + path);
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(in);
            int[] counts = new int[3];
            for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) {
                int a = img.getRGB(x, y) >>> 24;
                counts[a == 0 ? 0 : a == 255 ? 2 : 1]++;
            }
            return counts;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
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
            for (int y = a[1]; y <= a[1] + 1; y++) h.assertTrue(LobbyGen.at(s, seed, a[0], y, a[2]).passable(), "arrival free at y " + y + " " + a[0] + " " + a[2]);
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
            boolean stair = LobbyGen.opensSlab(s, seed, Math.floorDiv(x, 8), Math.floorDiv(z, 8));
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
                    boolean stair = y == 8 && LobbyGen.opensSlab(LobbyGen.LOBBY, BackroomsChunkGenerator.seed(back.getChunkSource().randomState()),
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
            int cells = 0, offices = 0, webCells = 0, tables = 0, rooms = 0;
            for (int cx = -30; cx < 30; cx++) {
                for (int cz = -30; cz < 30; cz++) {
                    cells++;
                    LobbyGen.Supply supply = LobbyGen.supply(st, seed, 0, cx, cz);
                    if (supply == LobbyGen.Supply.OFFICE) offices++;
                    if (supply == LobbyGen.Supply.WEBS) webCells++;
                    if (LobbyGen.exitCell(st, seed, cx, cz) || LobbyGen.signed(st, seed, cx, cz) || LobbyGen.stairwell(st, seed, cx, cz)
                            || LobbyGen.shape(st, seed, cx, cz) != LobbyGen.Shape.NORMAL) {
                        h.assertTrue(supply == LobbyGen.Supply.NONE || LobbyGen.signed(st, seed, cx, cz), "no supplies in exit/stair cell " + cx + " " + cz);
                        continue;
                    }
                    if (supply == LobbyGen.Supply.BREAK_ROOM || supply == LobbyGen.Supply.LOUNGE) rooms++;
                    int logs = 0, webs = 0, wood = 0, house = 0;
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
                                if (s.is(Blocks.SMOKER) || s.is(Blocks.WATER_CAULDRON) || s.is(Blocks.BARREL) || s.is(Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.RED)) || s.is(Blocks.POTTED_DEAD_BUSH)) house++;
                            }
                        }
                    }
                    if (supply == LobbyGen.Supply.OFFICE) {
                        h.assertTrue(logs == 3 && wood == 3 && webs == 0, "office " + cx + " " + cz + ": logs " + logs + ", desk/chair " + wood);
                    } else if (supply == LobbyGen.Supply.BREAK_ROOM) {
                        h.assertTrue(house == 3 && wood == 2 && logs + webs == 0, "break room " + cx + " " + cz + ": sink, counter, smoker, " + house);
                    } else if (supply == LobbyGen.Supply.LOUNGE) {
                        h.assertTrue(house == 4 && wood == 2 && logs + webs == 0, "lounge " + cx + " " + cz + ": sofa " + wood + ", rug/pot/counter " + house);
                    } else if (supply == LobbyGen.Supply.WEBS) {
                        h.assertTrue(webs >= 12 && logs == 0, "cobweb cell " + cx + " " + cz + ": " + webs);
                    } else {
                        h.assertTrue(logs + webs + wood + house == 0, "empty cell " + cx + " " + cz);
                    }
                }
            }
            double officeShare = offices / (double) cells, webShare = webCells / (double) cells;
            h.assertTrue(officeShare > 0.03 && officeShare < 0.08, "office share " + officeShare);
            h.assertTrue(webShare > 0.04 && webShare < 0.14, "cobweb share " + webShare);
            h.assertTrue(tables > 0, "some crafting tables");
            h.assertTrue(rooms > 40, "break rooms and lounges " + rooms);
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
        var box = new net.minecraft.world.phys.AABB(spot[0] - 3, spot[1] - 3, spot[2] - 3, spot[0] + 4, spot[1] + 4, spot[2] + 4);
        // The copy joins the Warehouse's entity sections a tick later.
        h.succeedWhen(() -> {
            var found = warehouse.getEntitiesOfClass(ArmorStand.class, box);
            h.assertTrue(!found.isEmpty(), "lobby rift -> warehouse at " + spot[0] + " " + spot[1] + " " + spot[2]);
            found.forEach(Entity::discard);
        });
    }

    /**
     * BACK18: the quick way out - from anywhere in Level 0 an exit cell lies at most 10 cells away (escape tree, BACK20: regions of 8), the exit cell holds
     * the Exit Door under an EXIT sign, the cells of its row and column show the way with a sign; the other levels' goals too.
     */
    static void easiestExitReachable(GameTestHelper h) {
        long seed = 777L;
        LobbyGen.Style s = LobbyGen.LOBBY;
        java.util.Random random = new java.util.Random(11);
        int worst = 0;
        for (int i = 0; i < 300; i++) {
            int x = random.nextInt(200000) - 100000, z = random.nextInt(200000) - 100000;
            int d = EscapeCheck.lobbyToExit(seed, x, z, 10);
            h.assertTrue(d >= 0 && d <= 10, "exit within 10 cells from " + x + " " + z + ": " + d);
            worst = Math.max(worst, d);
            int end = EscapeCheck.endToExit(seed, x, z, 4), pools = EscapeCheck.poolsToExit(seed, x, z, 2 * 64), pipes = EscapeCheck.pipesToHatch(seed, x, z, 2 * 144);
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

    /**
     * BACK18 shaft, BACK20 house: the abandoned house stands (door, panes, roof), a trapdoor and ladder lead into the cellar, the
     * cellar's west wall is a grey concrete plug framed in hazard stripes with tape, two caution signs facing into the cellar and a
     * warning light; behind it the shaft's floor of 9 rifts (the portal) on glitched bedrock; barrels carry the house loot; the wood
     * follows the biome.
     */
    static void entranceShaftGenerated(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        BlockPos ground = new BlockPos(o.getX() + 1600, Math.max(o.getY() + 20, level.getMinY() + HouseTemplate.DEPTH + 4), o.getZ() + 8);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.getChunk((ground.getX() >> 4) + dx, (ground.getZ() >> 4) + dz);
        EntranceShaftFeature.build(level, ground, BackroomsBlocks.Wood.OAK, level.getRandom());
        int rifts = 0, signs = 0, tape = 0, lamps = 0, ladders = 0, plug = 0, barrels = 0, doors = 0, panes = 0, roof = 0, trapdoors = 0;
        for (int dy = -HouseTemplate.DEPTH; dy <= HouseTemplate.HEIGHT; dy++) {
            for (int dx = HouseTemplate.MIN_X; dx <= HouseTemplate.MAX_X; dx++) {
                for (int dz = -HouseTemplate.HALF_Z; dz <= HouseTemplate.HALF_Z; dz++) {
                    BlockPos at = ground.offset(dx, dy, dz);
                    BlockState st = level.getBlockState(at);
                    if (st.is(LayersRegistry.RIFT)) rifts++;
                    if (st.is(LayersRegistry.CAUTION_SIGN)) {
                        signs++;
                        h.assertTrue(st.getValue(CautionSignBlock.FACING) == Direction.EAST, "sign faces into the cellar at " + dx + " " + dz);
                    }
                    if (st.is(LayersRegistry.CAUTION_TAPE)) tape++;
                    if (st.is(Blocks.COPPER_BULB.waxed().unaffected())) lamps++;
                    if (st.is(Blocks.LADDER)) ladders++;
                    if (st.is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.GRAY)) && dx == HouseTemplate.PLUG_X) plug++;
                    if (st.is(Blocks.BARREL) && level.getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity barrel
                            && EntranceShaftFeature.LOOT.equals(barrel.getLootTable())) barrels++;
                    if (st.is(Blocks.OAK_DOOR)) doors++;
                    if (st.is(Blocks.GLASS_PANE)) panes++;
                    if (st.is(Blocks.OAK_STAIRS)) roof++;
                    if (st.is(Blocks.OAK_TRAPDOOR)) trapdoors++;
                }
            }
        }
        h.assertTrue(rifts == 9 && level.getBlockState(ground.offset(HouseTemplate.SHAFT_X, HouseTemplate.PORTAL_DY, 0)).is(LayersRegistry.RIFT)
                && level.getBlockState(ground.offset(HouseTemplate.SHAFT_X, -HouseTemplate.DEPTH, 0)).is(LayersRegistry.GLITCHED_BEDROCK), "rift floor " + rifts);
        h.assertTrue(plug == 6 && signs == 2 && tape == 3 && lamps == 1, "sealed wall: plug " + plug + ", signs " + signs + ", tape " + tape + ", lamp " + lamps);
        h.assertTrue(ladders >= 13 && trapdoors == 1 && barrels == 2, "ladders " + ladders + ", trapdoor " + trapdoors + ", loot barrels " + barrels);
        h.assertTrue(doors == 2 && panes == 5 && roof >= 60, "house: door " + doors + ", panes " + panes + ", roof " + roof);
        // the passage behind the plug is open into the shaft; the cellar is 3 high
        h.assertTrue(level.getBlockState(ground.offset(-5, -2, 0)).isAir() && level.getBlockState(ground.offset(HouseTemplate.SHAFT_X, -6, 0)).isAir(), "passage and shaft open");
        h.assertTrue(level.getBlockState(ground.offset(0, -2, 0)).isAir() && level.getBlockState(ground.offset(0, HouseTemplate.CELLAR_FLOOR, 0)).is(Blocks.STONE_BRICKS), "cellar");
        // wood per biome
        var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        h.assertTrue(EntranceShaftFeature.wood(biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.SNOWY_TAIGA).value()) == BackroomsBlocks.Wood.SPRUCE
                && EntranceShaftFeature.wood(biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.SAVANNA).value()) == BackroomsBlocks.Wood.ACACIA
                && EntranceShaftFeature.wood(biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS).value()) == BackroomsBlocks.Wood.OAK, "wood per biome");
        h.assertTrue(BackroomsBlocks.wood(Piece.ROOF_N, BackroomsBlocks.Wood.SPRUCE).is(Blocks.SPRUCE_STAIRS), "spruce roof");
        h.assertTrue(LayersConfig.shaftRarity > 0, "houses on by default");
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
    }

    // ---------------------------------------------------------------- BACK20 (plan docs/ai/PLAN-B-BACK20-2026-11-10.md)

    /**
     * BACK20: the deeper, the more of a labyrinth - measured as the shortest way (in blocks) from 150 random places to each level's
     * goal: the Lobby is quick (exit at most 10 rooms, rifts near), the Poolrooms and the Warehouse are medium, the Pipes the longest;
     * the exit level stays short. Every goal is reachable within its region.
     */
    static void labyrinthDepthGradient(GameTestHelper h) {
        long seed = 2026L;
        java.util.Random random = new java.util.Random(20);
        int n = 150;
        double lobby = 0, deeper = 0, warehouse = 0, pools = 0, pipes = 0, end = 0;
        for (int i = 0; i < n; i++) {
            int x = random.nextInt(200000) - 100000, z = random.nextInt(200000) - 100000;
            int a = EscapeCheck.lobbyToExit(seed, x, z, 10), b = EscapeCheck.lobbyToDeeper(seed, x, z, 30), c = EscapeCheck.warehouseToLift(seed, x, z, 128),
                    d = EscapeCheck.poolsToExit(seed, x, z, 128), e = EscapeCheck.pipesToHatch(seed, x, z, 288), f = EscapeCheck.endToExit(seed, x, z, 4);
            h.assertTrue(a >= 0 && b >= 0 && c >= 0 && d >= 0 && e >= 0 && f >= 0, "goal reachable from " + x + " " + z + ": " + a + " " + b + " " + c + " " + d + " " + e + " " + f);
            lobby += a * LobbyGen.CELL;
            deeper += b * LobbyGen.CELL;
            warehouse += c * WarehouseGen.BAY;
            pools += d * PoolsGen.ROOM;
            pipes += e * PipesGen.NODE;
            end += f * LobbyGen.CELL;
        }
        lobby /= n;
        deeper /= n;
        warehouse /= n;
        pools /= n;
        pipes /= n;
        end /= n;
        String stats = String.format(java.util.Locale.ROOT, "lobby %.0f, deeper %.0f, pools %.0f, warehouse %.0f, pipes %.0f, end %.0f blocks", lobby, deeper, pools, warehouse, pipes, end);
        h.assertTrue(lobby <= 48 && deeper <= 48, "the Lobby is quick to leave: " + stats);
        h.assertTrue(lobby < pools && pools < warehouse && warehouse < pipes, "harder with depth: " + stats);
        h.assertTrue(pipes >= 80 && pipes > 2.5 * lobby, "the Pipes are a labyrinth: " + stats);
        h.assertTrue(end <= 32, "the exit level is a relief: " + stats);
        // the region maze is a spanning tree: inside one region every node reaches the root without leaving it
        int rx = 3, rz = -2, size = PipesGen.REGION;
        for (int k = 0; k < 20; k++) {
            int nx = rx * size + random.nextInt(size), nz = rz * size + random.nextInt(size);
            int dist = EscapeCheck.distance(nx, nz, size * size, (cx, cz) -> Math.floorMod(cx, size) != 0 && Maze.west(seed, cx, cz, size, 321, 0),
                    (cx, cz) -> Math.floorMod(cz, size) != 0 && Maze.north(seed, cx, cz, size, 321, 0), q -> Maze.root(seed, (int) q[0], (int) q[1], size, 321));
            h.assertTrue(dist >= 0, "maze tree spans its region from " + nx + " " + nz);
        }
        h.succeed();
    }

    /** BACK20: every level has rooms of other heights - Lobby ceilings/daises/atriums, mezzanines and docks, boiler rooms and crawl ways, galleries. */
    static void levelHeightsVary(GameTestHelper h) {
        long seed = 77L;
        LobbyGen.Style s = LobbyGen.LOBBY;
        int low = 0, dais = 0, atrium = 0, cells = 0;
        for (int cx = -40; cx < 40; cx++) {
            for (int cz = -40; cz < 40; cz++) {
                cells++;
                int x = cx * 8, z = cz * 8;
                switch (LobbyGen.shape(s, seed, cx, cz)) {
                    case LOW -> {
                        low++;
                        h.assertTrue(LobbyGen.at(s, seed, x + 2, 4, z + 2).isAir() && !LobbyGen.at(s, seed, x + 2, 5, z + 2).passable(), "dropped ceiling at y 5 in " + cx + " " + cz);
                    }
                    case DAIS -> {
                        dais++;
                        h.assertTrue(LobbyGen.at(s, seed, x + 4, 2, z + 4) == s.carpet() && LobbyGen.at(s, seed, x + 2, 2, z + 4) == Piece.SLAB
                                && LobbyGen.arrival(s, seed, x, z)[1] == 3, "dais with a step in " + cx + " " + cz);
                    }
                    case ATRIUM -> {
                        atrium++;
                        for (int y = 2; y <= 14; y++) h.assertTrue(LobbyGen.at(s, seed, x + 4, y, z + 4).isAir(), "light well open at y " + y + " in " + cx + " " + cz);
                        h.assertTrue(LobbyGen.at(s, seed, x + 2, 10, z + 4) == s.dotted() && LobbyGen.at(s, seed, x + 1, 10, z + 4).passable(), "gallery with a parapet in " + cx + " " + cz);
                    }
                    default -> {
                    }
                }
            }
        }
        h.assertTrue(low > cells * 0.05 && dais > cells * 0.02 && atrium > cells * 0.015, "lobby shapes: low " + low + ", dais " + dais + ", atrium " + atrium + " of " + cells);
        int mezz = 0, docks = 0, boilers = 0, crawls = 0, galleries = 0, terraces = 0;
        for (int a = -30; a < 30; a++) {
            for (int b = -30; b < 30; b++) {
                WarehouseGen.Bay bay = WarehouseGen.bay(seed, a, b);
                int x = a * WarehouseGen.BAY, z = b * WarehouseGen.BAY;
                if (bay == WarehouseGen.Bay.MEZZANINE) {
                    mezz++;
                    h.assertTrue(WarehouseGen.at(seed, x + 8, WarehouseGen.DECK_Y, z + 9) == Piece.DECK && WarehouseGen.at(seed, x + 8, WarehouseGen.DECK_Y + 1, z + 9).isAir()
                            && WarehouseGen.at(seed, x + 6, 3, z + 6) == Piece.LADDER_N && WarehouseGen.at(seed, x + 8, 3, z + 9).isAir(), "mezzanine " + a + " " + b);
                }
                if (bay == WarehouseGen.Bay.DOCK) {
                    docks++;
                    h.assertTrue(WarehouseGen.at(seed, x + 6, WarehouseGen.DOCK_Y, z + 3) == Piece.CONCRETE_FLOOR && WarehouseGen.at(seed, x + 6, WarehouseGen.DOCK_Y, z + 5) == Piece.STONE_SLAB, "dock " + a + " " + b);
                }
                if (PipesGen.node(seed, 0, a, b) == PipesGen.Node.BOILER) {
                    boilers++;
                    int px = a * PipesGen.NODE, pz = b * PipesGen.NODE;
                    for (int y = 2; y <= PipesGen.STOREY; y++) h.assertTrue(PipesGen.at(seed, px + 2, y, pz + 2).isAir(), "boiler open through the storeys at y " + y);
                    h.assertTrue(PipesGen.at(seed, px + 2, PipesGen.STOREY + 1, pz + 2) == Piece.GRATE && PipesGen.at(seed, px + 3, 5, pz + 3) == Piece.COPPER_BOX, "grate gallery and tank");
                }
                if (PipesGen.west(seed, 0, a, b) && PipesGen.crawl(seed, 0, a, b, 0, 3)) {
                    crawls++;
                    int px = a * PipesGen.NODE, pz = b * PipesGen.NODE;
                    h.assertTrue(PipesGen.at(seed, px, 2, pz + 3).passable() && PipesGen.at(seed, px, 3, pz + 3).passable() && !PipesGen.at(seed, px, 4, pz + 3).passable(), "crawl way 2 high");
                }
                PoolsGen.Room room = PoolsGen.room(seed, a, b);
                int qx = a * PoolsGen.ROOM, qz = b * PoolsGen.ROOM;
                if (room == PoolsGen.Room.GALLERY) {
                    galleries++;
                    h.assertTrue(PoolsGen.at(seed, qx + 5, PoolsGen.BALCONY_Y, qz + 2) == Piece.TILE_FLOOR && PoolsGen.at(seed, qx + 9, PoolsGen.WALK_Y + 1, qz + 7) == Piece.STAIR_N
                            && PoolsGen.ceiling(seed, a, b) == 13, "gallery " + a + " " + b);
                }
                if (room == PoolsGen.Room.TERRACE) {
                    terraces++;
                    h.assertTrue(PoolsGen.at(seed, qx + 3, PoolsGen.WALK_Y - 1, qz + 5) == Piece.POOL_TILE && PoolsGen.at(seed, qx + 6, PoolsGen.WALK_Y - 1, qz + 5) == Piece.WATER, "terrace steps " + a + " " + b);
                }
            }
        }
        h.assertTrue(mezz > 50 && docks > 50 && boilers > 30 && crawls > 150 && galleries > 100 && terraces > 100,
                "mezzanines " + mezz + ", docks " + docks + ", boilers " + boilers + ", crawls " + crawls + ", galleries " + galleries + ", terraces " + terraces);
        h.succeed();
    }

    /** BACK20: chalk draws arrows/cross/circle by the click, costs durability, redraws, glows with a glow ink sac and falls with its support. */
    static void chalkMarks(GameTestHelper h) {
        h.assertTrue(ChalkMarkBlock.symbolFor(Direction.SOUTH, new Vec3(0.4, 0, 0), false) == ChalkMarkBlock.Symbol.ARROW_RIGHT, "south wall, east = right");
        h.assertTrue(ChalkMarkBlock.symbolFor(Direction.EAST, new Vec3(0, 0, -0.4), false) == ChalkMarkBlock.Symbol.ARROW_RIGHT, "east wall, north = right");
        h.assertTrue(ChalkMarkBlock.symbolFor(Direction.NORTH, new Vec3(0, 0.4, 0.3), false) == ChalkMarkBlock.Symbol.ARROW_UP, "up");
        h.assertTrue(ChalkMarkBlock.symbolFor(Direction.UP, new Vec3(0, 0, -0.4), false) == ChalkMarkBlock.Symbol.ARROW_UP
                && ChalkMarkBlock.symbolFor(Direction.UP, new Vec3(-0.4, 0, 0), false) == ChalkMarkBlock.Symbol.ARROW_LEFT, "floor: up = north");
        h.assertTrue(ChalkMarkBlock.symbolFor(Direction.WEST, new Vec3(0, 0.05, 0.05), false) == ChalkMarkBlock.Symbol.CROSS
                && ChalkMarkBlock.symbolFor(Direction.WEST, new Vec3(0, 0.4, 0), true) == ChalkMarkBlock.Symbol.CIRCLE, "cross in the middle, circle sneaking");
        BlockPos wall = new BlockPos(2, 2, 2), front = wall.south();
        h.setBlock(wall, Blocks.STONE);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack chalk = new ItemStack(LayersRegistry.RED_CHALK);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, chalk);
        BlockPos abs = h.absolutePos(wall);
        Vec3 hit = Vec3.atCenterOf(abs).add(0.35, 0.05, 0.5);
        var result = chalk.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(hit, Direction.SOUTH, abs, false)));
        BlockState mark = h.getBlockState(front);
        h.assertTrue(result.consumesAction() && mark.is(LayersRegistry.CHALK_MARK) && mark.getValue(ChalkMarkBlock.FACING) == Direction.SOUTH
                && mark.getValue(ChalkMarkBlock.COLOR) == ChalkMarkBlock.Color.RED && mark.getValue(ChalkMarkBlock.SYMBOL) == ChalkMarkBlock.Symbol.ARROW_RIGHT, "drawn: " + mark);
        h.assertTrue(chalk.getDamageValue() == 1 && chalk.getMaxDamage() == ChalkItem.USES, "one stroke used: " + chalk.getDamageValue());
        h.assertTrue(mark.getCollisionShape(h.getLevel(), h.absolutePos(front)).isEmpty() && mark.canBeReplaced() && mark.getLightEmission() == 0, "no collision, replaceable, dark");
        // redraw over the mark itself: a cross
        ChalkMarkBlock.draw(h.getLevel(), h.absolutePos(front), Direction.SOUTH, Vec3.atCenterOf(h.absolutePos(front)), ChalkMarkBlock.Color.WHITE, false, false);
        h.assertTrue(h.getBlockState(front).getValue(ChalkMarkBlock.SYMBOL) == ChalkMarkBlock.Symbol.CROSS && h.getBlockState(front).getValue(ChalkMarkBlock.COLOR) == ChalkMarkBlock.Color.WHITE, "redrawn");
        // glow ink sac
        h.setBlock(front, h.getBlockState(front).setValue(ChalkMarkBlock.GLOWING, Boolean.TRUE));
        h.assertTrue(h.getBlockState(front).getLightEmission() == ChalkMarkBlock.GLOW_LIGHT, "glowing mark gives light");
        // no mark into a non-sturdy face or water
        h.assertFalse(ChalkMarkBlock.draw(h.getLevel(), h.absolutePos(new BlockPos(5, 5, 5)), Direction.UP, Vec3.atCenterOf(h.absolutePos(new BlockPos(5, 5, 5))),
                ChalkMarkBlock.Color.WHITE, false, false), "nothing to draw on in the air");
        // the support goes, the mark goes
        h.setBlock(wall, Blocks.AIR);
        h.assertTrue(h.getBlockState(front).isAir(), "mark falls off with its wall");
        // the generated marks: Lobby arrows point the true way out, pipe dead ends get crosses
        long seed = 9L;
        int arrows = 0, crosses = 0;
        for (int cx = -40; cx < 40; cx++) {
            for (int cz = -40; cz < 40; cz++) {
                if (LobbyGen.chalked(LobbyGen.LOBBY, seed, cx, cz)) {
                    Piece p = LobbyGen.at(LobbyGen.LOBBY, seed, cx * 8 + 2, 4, cz * 8 + 1), q = LobbyGen.at(LobbyGen.LOBBY, seed, cx * 8 + 1, 4, cz * 8 + 2);
                    int dir = com.simplebuilding.modules.simplelayers.world.gen.GenHash.toward(seed, cx, cz, LobbyGen.LOBBY.region(), LobbyGen.LOBBY.salt() + 21);
                    if (p == Piece.CHALK_E || p == Piece.CHALK_W || q == Piece.CHALK_N || q == Piece.CHALK_S) {
                        arrows++;
                        Piece m = p == Piece.CHALK_E || p == Piece.CHALK_W ? p : q;
                        int want = switch (m) {
                            case CHALK_E -> com.simplebuilding.modules.simplelayers.world.gen.GenHash.EAST;
                            case CHALK_W -> com.simplebuilding.modules.simplelayers.world.gen.GenHash.WEST;
                            case CHALK_N -> com.simplebuilding.modules.simplelayers.world.gen.GenHash.NORTH;
                            default -> com.simplebuilding.modules.simplelayers.world.gen.GenHash.SOUTH;
                        };
                        h.assertTrue(dir == want, "chalk arrow points the escape way in " + cx + " " + cz);
                    }
                }
                if (PipesGen.crossed(seed, 0, cx, cz)) {
                    crosses++;
                    h.assertTrue(PipesGen.at(seed, cx * PipesGen.NODE + 3, 2, cz * PipesGen.NODE + 3) == Piece.CHALK_X, "cross on the floor of a dead end");
                }
            }
        }
        h.assertTrue(arrows > 100 && crosses > 50, "generated marks: arrows " + arrows + ", crosses " + crosses);
        h.succeed();
    }

    /** BACK20: the chalk box takes chalk and glow ink, picks the next filled colour, draws with it and counts down. */
    static void chalkBox(GameTestHelper h) {
        ItemStack box = new ItemStack(LayersRegistry.CHALK_BOX);
        ItemStack blue = new ItemStack(LayersRegistry.BLUE_CHALK);
        blue.setDamageValue(10);
        h.assertTrue(ChalkBoxItem.absorb(box, blue) && blue.isEmpty() && ChalkBoxItem.strokes(box, ChalkMarkBlock.Color.BLUE) == ChalkItem.USES - 10, "blue chalk taken");
        h.assertTrue(ChalkBoxItem.selected(box) == ChalkMarkBlock.Color.BLUE, "the first chalk selects its colour");
        ItemStack white = new ItemStack(LayersRegistry.CHALK, 5);
        for (int i = 0; i < 5; i++) ChalkBoxItem.absorb(box, white);
        h.assertTrue(ChalkBoxItem.strokes(box, ChalkMarkBlock.Color.WHITE) == ChalkBoxItem.CAPACITY && white.getCount() == 1, "white full at " + ChalkBoxItem.CAPACITY + ", one left over");
        h.assertFalse(ChalkBoxItem.absorb(box, white), "a full colour takes no more");
        ItemStack ink = new ItemStack(Items.GLOW_INK_SAC, 2);
        h.assertTrue(ChalkBoxItem.absorb(box, ink) && ChalkBoxItem.glow(box) == ChalkBoxItem.GLOW_PER_SAC && ink.getCount() == 1, "glow ink");
        h.assertTrue(ChalkBoxItem.next(box) == ChalkMarkBlock.Color.WHITE, "next filled colour after blue is white");
        BlockPos floor = new BlockPos(3, 1, 3);
        h.setBlock(floor, Blocks.STONE);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, box);
        BlockPos abs = h.absolutePos(floor);
        box.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
        BlockState mark = h.getBlockState(floor.above());
        h.assertTrue(mark.is(LayersRegistry.CHALK_MARK) && mark.getValue(ChalkMarkBlock.COLOR) == ChalkMarkBlock.Color.BLUE && mark.getValue(ChalkMarkBlock.GLOWING)
                && mark.getValue(ChalkMarkBlock.SYMBOL) == ChalkMarkBlock.Symbol.CROSS, "box draws a glowing blue cross: " + mark);
        h.assertTrue(ChalkBoxItem.strokes(box, ChalkMarkBlock.Color.BLUE) == ChalkItem.USES - 11 && ChalkBoxItem.glow(box) == ChalkBoxItem.GLOW_PER_SAC - 1, "one stroke and one glow used");
        h.assertTrue(box.isBarVisible() && box.getBarWidth() > 0, "bar shows the selected colour");
        h.succeed();
    }

    /** BACK20: the chalk recipes exist (calcite + bone meal, + dye; the box from paper and slabs) and chalk is in the tab. */
    static void chalkRecipes(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        for (String id : List.of("chalk", "red_chalk", "blue_chalk", "yellow_chalk", "chalk_box")) {
            var key = net.minecraft.resources.ResourceKey.create(Registries.RECIPE, SimpleLayers.id(id));
            h.assertTrue(server.getRecipeManager().byKey(key).isPresent(), "recipe " + id);
        }
        List<Item> tab = LayersRegistry.tabStacks().stream().map(ItemStack::getItem).toList();
        h.assertTrue(tab.contains(LayersRegistry.CHALK) && tab.contains(LayersRegistry.CHALK_BOX), "chalk in the tab");
        h.assertFalse(BuiltInRegistries.ITEM.containsKey(SimpleLayers.id("chalk_mark")), "no item for the mark");
        // round 11: loot ties only while the other mod's item exists; chalk in mineshaft chests always
        List<Object> pools = new ArrayList<>();
        com.simplebuilding.modules.simplelayers.world.BackroomsLoot.apply(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,
                Identifier.parse(com.simplebuilding.modules.simplelayers.world.BackroomsLoot.MINESHAFT)), pools::add);
        h.assertTrue(pools.size() == 1, "chalk pool in mineshaft chests");
        pools.clear();
        com.simplebuilding.modules.simplelayers.world.BackroomsLoot.apply(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,
                Identifier.parse(com.simplebuilding.modules.simplelayers.world.BackroomsLoot.PIPES)), pools::add);
        boolean gadgets = BuiltInRegistries.ITEM.containsKey(com.simplebuilding.modules.simplelayers.world.BackroomsLoot.ECHO_SOUNDER);
        h.assertTrue(pools.size() == (gadgets ? 1 : 0), "echo sounder only with Simple Gadgets: " + pools.size() + ", gadgets " + gadgets);
        h.succeed();
    }

    // ---------------------------------------------------------------- STAIRS22 (plan docs/ai/PLAN-B-STAIRS22-2026-11-10.md, merged in b-back20s)

    /** A generator seen as absolute block positions (for the stair checks). */
    interface At {
        Piece at(int x, int y, int z);
    }

    /**
     * STAIRS22 (live bug: loose stair blocks, head against the ceiling tiles), extended to every stair of the BACK20 levels: per seed
     * and level every step stands on a base and has {@link LobbyGen#STAIR_HEADROOM} passable blocks above it, and a simulated player
     * (0.6 x 1.8 box, step height 0.6, the real collision shapes of the generated blocks) walks every flight up without jumping and
     * without hitting anything, exit included. Level 0: Lobby stairwells and dais steps; Level 1: loading dock steps, mezzanine
     * ladders; Level 2: ladder shafts and boiler ladders; Poolrooms: gallery stairs to the balcony.
     */
    static void stairwellsWalkable(GameTestHelper h) {
        LobbyGen.Style s = LobbyGen.LOBBY;
        int[] count = new int[7];
        for (long seed : new long[] {1L, 99L, 12345L, -7L, 424242L}) {
            java.util.Random random = new java.util.Random(seed);
            At lobby = (x, y, z) -> LobbyGen.at(s, seed, x, y, z), pools = (x, y, z) -> PoolsGen.at(seed, x, y, z),
                    warehouse = (x, y, z) -> WarehouseGen.at(seed, x, y, z), pipes = (x, y, z) -> PipesGen.at(seed, x, y, z);
            int[] found = new int[7];
            for (int i = 0; i < 40000 && java.util.Arrays.stream(found).anyMatch(f -> f < 3); i++) {
                int cx = random.nextInt(4000) - 2000, cz = random.nextInt(4000) - 2000;
                // Level 0: stairwells (the L-shaped flight to the upper floor)
                if (found[0] < 3 && LobbyGen.stairwell(s, seed, cx, cz)) {
                    found[0]++;
                    int ox = cx * LobbyGen.CELL, oz = cz * LobbyGen.CELL;
                    for (int[] step : LobbyGen.STAIR_STEPS) assertStep(h, lobby, "lobby stairwell", ox + step[0], step[1], oz + step[2]);
                    String fail = walk(lobby, ox, oz, ox + LobbyGen.CELL, oz + LobbyGen.CELL, LobbyGen.TOP_Y + LobbyGen.UPPER, ox + 1.5, oz + 2.5,
                            LobbyGen.FLOOR_Y + 1, new double[][] {{ox + 7.5, oz + 2.5}, {ox + 7.5, oz + 5.5}}, LobbyGen.TOP_Y + 2);
                    h.assertTrue(fail == null, "seed " + seed + " stairwell " + cx + " " + cz + ": " + fail);
                }
                // Level 0: daises (one slab step up, BACK20)
                if (found[1] < 3 && LobbyGen.shape(s, seed, cx, cz) == LobbyGen.Shape.DAIS) {
                    found[1]++;
                    int ox = cx * LobbyGen.CELL, oz = cz * LobbyGen.CELL;
                    for (int lx = 2; lx <= 6; lx++) assertStep(h, lobby, "dais edge", ox + lx, 2, oz + 2);
                    String fail = walk(lobby, ox, oz, ox + LobbyGen.CELL, oz + LobbyGen.CELL, LobbyGen.CEILING_Y, ox + 1.5, oz + 4.5,
                            LobbyGen.FLOOR_Y + 1, new double[][] {{ox + 4.5, oz + 4.5}}, 3);
                    h.assertTrue(fail == null, "seed " + seed + " dais " + cx + " " + cz + ": " + fail);
                }
                // Level 1: loading docks (one block up, edged by slabs as a step) and mezzanine ladders (BACK20)
                WarehouseGen.Bay bay = WarehouseGen.bay(seed, cx, cz);
                int bx = cx * WarehouseGen.BAY, bz = cz * WarehouseGen.BAY;
                if (found[2] < 3 && bay == WarehouseGen.Bay.DOCK) {
                    found[2]++;
                    for (int lx = 2; lx <= 10; lx++) assertStep(h, warehouse, "dock edge", bx + lx, WarehouseGen.DOCK_Y, bz + 5);
                    String fail = walk(warehouse, bx, bz, bx + WarehouseGen.BAY, bz + WarehouseGen.BAY, WarehouseGen.CEILING_Y, bx + 6.5, bz + 7.5,
                            WarehouseGen.FLOOR_Y + 1, new double[][] {{bx + 6.5, bz + 3.5}}, WarehouseGen.DOCK_Y + 1);
                    h.assertTrue(fail == null, "seed " + seed + " dock " + cx + " " + cz + ": " + fail);
                }
                if (found[3] < 3 && bay == WarehouseGen.Bay.MEZZANINE) {
                    found[3]++;
                    assertLadder(h, warehouse, "mezzanine ladder", bx + 6, WarehouseGen.FLOOR_Y + 1, WarehouseGen.DECK_Y, bz + 6, 0, 1);
                }
                // Level 2: ladder shafts and boiler ladders
                PipesGen.Node node = PipesGen.node(seed, 0, cx, cz);
                int px = cx * PipesGen.NODE, pz = cz * PipesGen.NODE;
                if (found[4] < 3 && node == PipesGen.Node.SHAFT) {
                    found[4]++;
                    assertLadder(h, pipes, "ladder shaft", px + 3, 2, PipesGen.STOREY + 2, pz + 2, 0, 1);
                }
                if (found[5] < 3 && node == PipesGen.Node.BOILER) {
                    found[5]++;
                    assertLadder(h, pipes, "boiler ladder", px + 3, 2, PipesGen.STOREY + 1, pz + 4, 1, 0);
                }
                // Poolrooms: gallery stairs up to the balcony (BACK20)
                if (found[6] < 3 && PoolsGen.room(seed, cx, cz) == PoolsGen.Room.GALLERY) {
                    found[6]++;
                    int qx = cx * PoolsGen.ROOM, qz = cz * PoolsGen.ROOM;
                    for (int lz = 4; lz <= 7; lz++) assertStep(h, pools, "gallery stair", qx + 9, PoolsGen.WALK_Y + 1 + (7 - lz), qz + lz);
                    String fail = walk(pools, qx, qz, qx + PoolsGen.ROOM, qz + PoolsGen.ROOM, PoolsGen.ceiling(seed, cx, cz), qx + 9.5, qz + 8.5,
                            PoolsGen.WALK_Y + 1, new double[][] {{qx + 9.5, qz + 2.5}}, PoolsGen.BALCONY_Y + 1);
                    h.assertTrue(fail == null, "seed " + seed + " gallery " + cx + " " + cz + ": " + fail);
                }
            }
            for (int k = 0; k < found.length; k++) {
                h.assertTrue(found[k] > 0, "seed " + seed + ": no stair of kind " + k + " found");
                count[k] += found[k];
            }
        }
        h.assertTrue(java.util.Arrays.stream(count).allMatch(c -> c >= 10), "stairs checked " + java.util.Arrays.toString(count));
        h.succeed();
    }

    /** One step at (x, y, z): solid, a base under it (nothing loose), {@link LobbyGen#STAIR_HEADROOM} passable blocks above it. */
    static void assertStep(GameTestHelper h, At at, String what, int x, int y, int z) {
        h.assertFalse(at.at(x, y, z).passable(), what + ": step at " + x + " " + y + " " + z);
        h.assertFalse(at.at(x, y - 1, z).passable(), what + ": a base under the step at " + x + " " + y + " " + z);
        for (int k = 1; k <= LobbyGen.STAIR_HEADROOM; k++) {
            h.assertTrue(at.at(x, y + k, z).passable(), what + ": headroom " + k + " above the step at " + x + " " + y + " " + z);
        }
    }

    /**
     * A ladder column at (x, z) from feet {@code bottom} to the floor {@code top} it leads to: free all the way and 2 free blocks
     * above the top, and where it lets off (dx, dz) a floor at {@code top} with 2 free blocks above.
     */
    static void assertLadder(GameTestHelper h, At at, String what, int x, int bottom, int top, int z, int dx, int dz) {
        for (int y = bottom; y <= top + 2; y++) h.assertTrue(at.at(x, y, z).passable(), what + " free at " + x + " " + y + " " + z);
        h.assertFalse(at.at(x + dx, top, z + dz).passable(), what + ": a floor beside the ladder top at " + x + " " + top + " " + z);
        h.assertTrue(at.at(x + dx, top + 1, z + dz).passable() && at.at(x + dx, top + 2, z + dz).passable(), what + ": 2 free blocks where the ladder lets off at " + x + " " + top + " " + z);
    }

    /**
     * Walks a simulated player through the blocks of the box (x0..x1, 0..y1, z0..z1) from (startX, startZ) with the feet on
     * {@code startFeet} along the path; null when he ends with the feet on {@code endFeet}, else where he got stuck.
     */
    static String walk(At at, int x0, int z0, int x1, int z1, int y1, double startX, double startZ, double startFeet, double[][] path, double endFeet) {
        List<net.minecraft.world.phys.AABB> boxes = new ArrayList<>();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = 0; y <= y1; y++) {
                    BlockState st = BackroomsBlocks.state(at.at(x, y, z));
                    for (net.minecraft.world.phys.AABB b : st.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs()) {
                        boxes.add(b.move(x, y, z));
                    }
                }
            }
        }
        double px = startX, pz = startZ, feet = stairSupport(boxes, px, pz, startFeet);
        if (Math.abs(feet - startFeet) > 1.0E-4) return "start not on the floor: " + feet;
        for (double[] target : path) {
            while (Math.abs(px - target[0]) > 1.0E-6 || Math.abs(pz - target[1]) > 1.0E-6) {
                double nx = px + Math.max(-0.05, Math.min(0.05, target[0] - px)), nz = pz + Math.max(-0.05, Math.min(0.05, target[1] - pz));
                double nf = stairSupport(boxes, nx, nz, feet);
                net.minecraft.world.phys.AABB body = new net.minecraft.world.phys.AABB(nx - 0.3, nf, nz - 0.3, nx + 0.3, nf + 1.8, nz + 0.3).deflate(1.0E-5);
                for (net.minecraft.world.phys.AABB b : boxes) {
                    if (b.intersects(body)) return String.format(java.util.Locale.ROOT, "stuck at %.2f %.2f %.2f against %s", nx, nf, nz, b);
                }
                px = nx;
                pz = nz;
                feet = nf;
            }
        }
        return Math.abs(feet - endFeet) < 1.0E-4 ? null : "ends at feet " + feet + " instead of " + endFeet;
    }

    /** The highest top under the player's footprint he can stand on from feet height {@code feet} (step height 0.6, falling ok). */
    static double stairSupport(List<net.minecraft.world.phys.AABB> boxes, double x, double z, double feet) {
        double best = Double.NEGATIVE_INFINITY;
        for (net.minecraft.world.phys.AABB b : boxes) {
            if (b.minX < x + 0.3 - 1.0E-6 && b.maxX > x - 0.3 + 1.0E-6 && b.minZ < z + 0.3 - 1.0E-6 && b.maxZ > z - 0.3 + 1.0E-6 && b.maxY <= feet + 0.6 + 1.0E-6) {
                best = Math.max(best, b.maxY);
            }
        }
        return best;
    }
}
