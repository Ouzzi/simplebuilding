package com.simplebuilding.modules.simplelayers.test;

import com.simplebuilding.modules.simplelayers.LayersRegistry;
import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.block.GlitchedBedrockBlock;
import com.simplebuilding.modules.simplelayers.block.NeonLightBlock;
import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import com.simplebuilding.modules.simplelayers.world.BackroomEffect;
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
        for (String id : List.of("cloud_sea", "backrooms")) {
            h.assertTrue(types.containsKey(SimpleLayers.id(id)), "dimension type " + id);
            h.assertTrue(biomes.containsKey(SimpleLayers.id(id)), "biome " + id);
        }
        h.succeed();
        return null;
    }

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

    /** LAYERS13: bedrock at y 0 and from the ceiling y 8 up to the build limit y 63; in a generated chunk too. */
    static void bedrockHull(GameTestHelper h) {
        long seed = 99L;
        java.util.Random random = new java.util.Random(3);
        for (int i = 0; i < 200; i++) {
            int x = random.nextInt(40000) - 20000, z = random.nextInt(40000) - 20000;
            h.assertTrue(BackroomsGrid.at(seed, x, 0, z).is(Blocks.BEDROCK), "floor y 0");
            for (int y = BackroomsGrid.TOP_Y; y <= BackroomsGrid.HULL_TOP_Y; y++) h.assertTrue(BackroomsGrid.at(seed, x, y, z).is(Blocks.BEDROCK), "hull y " + y);
            for (int y = 1; y < BackroomsGrid.TOP_Y; y++) h.assertFalse(BackroomsGrid.at(seed, x, y, z).is(Blocks.BEDROCK), "no bedrock inside at y " + y);
            h.assertTrue(BackroomsGrid.at(seed, x, -1, z).isAir() && BackroomsGrid.at(seed, x, BackroomsGrid.HULL_TOP_Y + 1, z).isAir(), "outside the world");
        }
        ServerLevel back = levelOrRegistryOnly(h, SimpleLayers.BACKROOMS);
        if (back == null) return;
        h.assertTrue(back.getMaxY() == BackroomsGrid.HULL_TOP_Y, "hull reaches the build limit " + back.getMaxY());
        int cx = -3000, cz = 2500;
        back.getChunk(cx, cz);
        for (int x = 0; x < 16; x += 5) {
            for (int z = 0; z < 16; z += 5) {
                for (int y : new int[] {0, 8, 9, 30, 63}) {
                    BlockPos p = new BlockPos(cx * 16 + x, y, cz * 16 + z);
                    h.assertTrue(back.getBlockState(p).is(Blocks.BEDROCK) || inSpecialRoom(back, p), "generated hull at " + p.toShortString());
                }
            }
        }
        h.succeed();
    }

    /** LAYERS13: offices (3 oak logs, desk, chair) and cobweb cells appear at the planned shares, only in the corners; config off = none. */
    static void suppliesGenerated(GameTestHelper h) {
        long seed = 4242L;
        boolean before = LayersConfig.supplies;
        try {
            LayersConfig.supplies = true;
            int cells = 0, offices = 0, webCells = 0, tables = 0;
            for (int cx = -30; cx < 30; cx++) {
                for (int cz = -30; cz < 30; cz++) {
                    cells++;
                    BackroomsGrid.Supply supply = BackroomsGrid.supply(seed, cx, cz);
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
                    if (supply == BackroomsGrid.Supply.OFFICE) {
                        offices++;
                        h.assertTrue(logs == 3 && wood == 3 && webs == 0, "office " + cx + " " + cz + ": logs " + logs + ", desk/chair " + wood);
                    } else if (supply == BackroomsGrid.Supply.WEBS) {
                        webCells++;
                        h.assertTrue(webs >= 12 && logs == 0, "cobweb cell " + cx + " " + cz + ": " + webs);
                    } else {
                        h.assertTrue(logs + webs + wood == 0, "empty cell " + cx + " " + cz);
                    }
                }
            }
            double officeShare = offices / (double) cells, webShare = webCells / (double) cells;
            h.assertTrue(officeShare > 0.03 && officeShare < 0.08, "office share " + officeShare);
            h.assertTrue(webShare > 0.05 && webShare < 0.14, "cobweb share " + webShare);
            h.assertTrue(tables > 0, "some crafting tables");
            LayersConfig.supplies = false;
            for (int cx = -10; cx < 10; cx++) h.assertTrue(BackroomsGrid.supply(seed, cx, 3) == BackroomsGrid.Supply.NONE, "config off: no supplies");
        } finally {
            LayersConfig.supplies = before;
        }
        h.succeed();
    }
}
