package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/**
 * Fabric adapter (MC 1.21.11 line) for the enchantment effect tests. No logic here; see
 * {@link EnchantmentEffectTests}. Everything that mines is pinned to an unrotated structure,
 * because the sledgehammer's shape depends on the direction the player looks at.
 */
public final class EnchantmentEffectGameTest {

    @GameTest(rotation = Rotation.NONE)
    public void radiusWidensTheSledgehammerFaceAndSneakingSuppressesIt(GameTestHelper helper) {
        EnchantmentEffectTests.radiusWidensTheSledgehammerFaceAndSneakingSuppressesIt(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void breakThroughAddsLayersBehindTheMinedFace(GameTestHelper helper) {
        EnchantmentEffectTests.breakThroughAddsLayersBehindTheMinedFace(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void versatilitySwapsInTheBetterToolWhileSneaking(GameTestHelper helper) {
        EnchantmentEffectTests.versatilitySwapsInTheBetterToolWhileSneaking(helper);
    }

    @GameTest
    public void funnelDecidesWhatTheBundlePicksUp(GameTestHelper helper) {
        EnchantmentEffectTests.funnelDecidesWhatTheBundlePicksUp(helper);
    }

    @GameTest
    public void dataDrivenEnchantmentEffectsSurviveDatagen(GameTestHelper helper) {
        EnchantmentEffectTests.dataDrivenEnchantmentEffectsSurviveDatagen(helper);
    }

    @GameTest
    public void coverAndBridgeAreInertAndThisIsDeliberatelyPinnedDown(GameTestHelper helper) {
        EnchantmentEffectTests.coverAndBridgeAreInertAndThisIsDeliberatelyPinnedDown(helper);
    }
}
