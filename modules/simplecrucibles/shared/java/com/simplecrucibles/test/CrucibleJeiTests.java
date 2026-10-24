package com.simplecrucibles.test;

import com.simplecrucibles.SimpleCrucibles;
import com.simplecrucibles.api.SimpleCruciblesApi;
import com.simplecrucibles.compat.CruciblesViewer;
import com.simplelib.api.InWorldUpgrades;
import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.InWorldUpgrade;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;

/** JEI24: the crucible ways in the recipe viewer, also alone (was SimpleBuilding's "Cauldron and Crucible" tab). */
public final class CrucibleJeiTests {
    private CrucibleJeiTests() {
    }

    /**
     * Building the iron crucible (cauldron + walls + handles, strike counts of the block) and attaching a barrel, with the
     * tools of the current mode; every crucible/barrel/cauldron upgrade of the effective table is in the upgrade tab.
     */
    public static void buildAttachAndUpgrades(GameTestHelper h) {
        List<InWorldUpgrade> entries = CruciblesViewer.entries();
        InWorldUpgrade build = entries.stream().filter(e -> e.id().equals("simplecrucibles/crucible_build")).findFirst().orElse(null);
        h.assertTrue(build != null && build.output().is(BuiltInRegistries.ITEM.getValue(SimpleCrucibles.id("iron_crucible")))
                && build.inputs().get(0).items().contains(Items.CAULDRON)
                && build.inputs().get(1).count() + build.inputs().get(2).count() == SimpleCruciblesApi.buildStrikes()
                && build.tools().equals(RecipeViewer.upgradeTools()) && !build.tools().isEmpty(), "crucible build: " + build);
        h.assertTrue(entries.stream().anyMatch(e -> e.id().equals("simplecrucibles/barrel_attach")), "barrel attach: " + entries);
        List<InWorldUpgrade> table = RecipeViewer.tableEntries();
        for (InWorldUpgrades.Entry entry : InWorldUpgrades.table().values()) {
            String ns = BuiltInRegistries.BLOCK.getKey(entry.to()).getNamespace();
            if (!SimpleCrucibles.MOD_ID.equals(ns) || RecipeViewer.tools(entry).isEmpty()) continue;
            h.assertTrue(table.stream().anyMatch(e -> e.output().is(entry.to().asItem()) && e.inputs().get(0).items().contains(entry.from().asItem())),
                    "upgrade into " + BuiltInRegistries.BLOCK.getKey(entry.to()) + " missing in the upgrade tab");
        }
        h.succeed();
    }
}
