package com.simplebuilding.modules.simpletweaks.claims;

import com.simplebuilding.modules.simpletweaks.table.ClaimTableBlock;
import com.simplebuilding.modules.simpletweaks.table.TableVisibility;
import net.minecraft.gametest.framework.GameTestHelper;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimRightsTests.roomy;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimRightsTests.seeded;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimTests.*;

/** What is left of the claim table (SP22-8 §6.3): the old ids and the visibility rules of {@code claimVisibility}. */
public final class TableTests {
    private TableTests() {}

    /** The old table is still registered (worlds keep loading), the claims tab, and the visibility rules (pure; used by the map layer). */
    public static void basics(GameTestHelper h) {
        yes(h, net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(net.minecraft.resources.Identifier.fromNamespaceAndPath("simpletweaks", "claims")), "Own creative tab claims is registered");
        yes(h, ClaimTableBlock.block() != net.minecraft.world.level.block.Blocks.AIR && ClaimTableBlock.item() != net.minecraft.world.item.Items.AIR, "The old claim table block and item stay registered");
        yes(h, h.getLevel().getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, ClaimTableBlock.ID)).isEmpty(), "The old table has no recipe any more");
        yes(h, TableVisibility.parse("all") == TableVisibility.ALL && TableVisibility.parse("owners_hidden") == TableVisibility.OWNERS_HIDDEN
                && TableVisibility.parse("own") == TableVisibility.OWN && TableVisibility.parse("off") == TableVisibility.OFF && TableVisibility.parse("x") == null, "Visibility values");
        yes(h, TableVisibility.ALL.shows(false, false) && TableVisibility.OWNERS_HIDDEN.shows(false, false) && !TableVisibility.OWN.shows(false, false) && TableVisibility.OWN.shows(false, true)
                && !TableVisibility.OFF.shows(false, true), "Who sees which claim");
        yes(h, TableVisibility.OFF.shows(true, false) && TableVisibility.OWN.shows(true, false) && TableVisibility.OWNERS_HIDDEN.showsName(true, false) && !TableVisibility.OWNERS_HIDDEN.showsName(false, false), "An operator sees everything with names");
        boolean refused = false;
        try { ClaimConfig.DEFAULT.withVisibility("everyone"); } catch (IllegalArgumentException e) { refused = true; }
        yes(h, refused && ClaimConfig.DEFAULT.visibility() == TableVisibility.ALL, "Config: unknown visibility refused, default all");
        h.succeed();
    }
}
