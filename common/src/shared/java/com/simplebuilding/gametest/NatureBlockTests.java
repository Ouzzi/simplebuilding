package com.simplebuilding.gametest;

import com.simplebuilding.util.BlockRefs;
import com.simplebuilding.version.McVersion;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

/**
 * SimpleBuilding's uses of the nature variants (QUEUE N24/N25). The blocks themselves (slabs, cracked and chiseled ice, the
 * nautilus shell block, the froglights) belong to Simple Blocks since b-blocksmove, together with their tests
 * ({@code simpleblocks:module_game_test_*}, {@code com.simpleblocks.test.NatureTests}); here stay the chisel way and the
 * cracked ice fields of SimpleBuilding's world generation. Without {@link McVersion#NATURE_VARIANTS} (26.2) the tests pass
 * without a check.
 */
public final class NatureBlockTests {

    private NatureBlockTests() {
    }

    /** The stone chisel turns ice into Simple Blocks' cracked ice (datapack file cracked_ice_stone_chisel.json), the spatula back. */
    public static void chiselMakesCrackedIce(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        var stone = com.simplebuilding.data.ChiselTables.tier("stone");
        var cracked = BlockRefs.block(BlockRefs.CRACKED_ICE);
        helper.assertTrue(cracked != Blocks.AIR, "cracked ice is registered (Simple Blocks)");
        helper.assertTrue(stone.forward().get(Blocks.ICE) == cracked, "the stone chisel turns ice into cracked ice, gives " + stone.forward().get(Blocks.ICE));
        helper.assertTrue(stone.backward().get(cracked) == Blocks.ICE, "the spatula turns cracked ice back into ice");
        helper.succeed();
    }

    /**
     * N53: die Rissiges-Eis-Felder haengen (Schalter an) im Schritt top_layer_modification an Eiszapfen und gefrorenen
     * Ozeanen, nicht an Ebenen; eine kleine Scheibe des registrierten Features macht aus einer Schneeflaeche rissiges Eis
     * ueber einer Grube (CrackedIceFieldChecks).
     */
    public static void crackedIceFieldsGenerateInIceSpikesAndFrozenOceans(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        helper.assertTrue(com.simplebuilding.config.ConfigFlags.test(com.simplebuilding.config.ConfigFlags.CRACKED_ICE_FIELDS),
                "the crackedIceFields switch is on by default");
        int top = net.minecraft.world.level.levelgen.GenerationStep.Decoration.TOP_LAYER_MODIFICATION.ordinal();
        for (var biome : List.of(net.minecraft.world.level.biome.Biomes.ICE_SPIKES, net.minecraft.world.level.biome.Biomes.FROZEN_OCEAN,
                net.minecraft.world.level.biome.Biomes.DEEP_FROZEN_OCEAN)) {
            helper.assertValueEqual(fieldStep(helper, biome), top, "cracked_ice_field_placed step in " + biome.identifier());
        }
        helper.assertValueEqual(fieldStep(helper, net.minecraft.world.level.biome.Biomes.PLAINS), -1, "cracked_ice_field_placed in plains");
        CrackedIceFieldChecks.placeSmallField(helper);
        helper.succeed();
    }

    private static int fieldStep(GameTestHelper helper, net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> key) {
        var biome = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getValue(key);
        helper.assertTrue(biome != null, "biome " + key.identifier() + " missing");
        var steps = biome.getGenerationSettings().features();
        for (int step = 0; step < steps.size(); step++) {
            for (var holder : steps.get(step)) {
                if (holder.is(com.simplebuilding.util.ModWorldGen.CRACKED_ICE_FIELD_PLACED_KEY)) {
                    return step;
                }
            }
        }
        return -1;
    }
}
