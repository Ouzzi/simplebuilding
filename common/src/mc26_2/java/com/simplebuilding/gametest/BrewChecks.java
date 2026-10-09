package com.simplebuilding.gametest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** MC 26.2 side of {@code BrewChecks}: no data driven brewing and no mod potions there (McVersion.BREWING_EFFECTS). */
final class BrewChecks {

    private BrewChecks() {
    }

    static ItemStack brew(ServerLevel level, ItemStack input, ItemStack reagent) {
        return ItemStack.EMPTY;
    }
}
