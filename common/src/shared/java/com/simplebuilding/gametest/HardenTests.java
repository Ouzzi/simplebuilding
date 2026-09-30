package com.simplebuilding.gametest;

import com.simplebuilding.config.ConfigOptions;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksConfig;
import com.simplebuilding.tweaks.component.TweaksComponents;
import com.simplebuilding.tweaks.item.TweaksItems;
import com.simplebuilding.tweaks.network.ElytraBoostPayload;
import com.simplebuilding.tweaks.network.TweaksNetwork;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Regression for the renamed resonance rod recipe. */
public final class HardenTests {
    private HardenTests() {}

    public static void recipeRename(GameTestHelper helper) {
        var config = SimpleTweaks.config().laserPointer;
        boolean previous = config.enable;
        try {
            config.enable = false;
            helper.assertTrue(com.simplebuilding.recipe.RecipeFilter.removes(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", "amethyst_lens")),
                    "disabled rod recipe remains craftable");
            helper.assertFalse(com.simplebuilding.recipe.RecipeFilter.removes(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "amethyst_lens")),
                    "foreign recipe removed");
            config.enable = true;
            helper.assertFalse(com.simplebuilding.recipe.RecipeFilter.removes(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", "amethyst_lens")),
                    "enabled rod recipe removed");
        } finally { config.enable = previous; }
        helper.succeed();
    }
}
