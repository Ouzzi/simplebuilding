package com.simplestorage.test;

import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.MobDrop;
import com.simplestorage.SimpleStorage;
import com.simplestorage.rare.ShulkerShells;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/** JEI24: the rare shulkers' tier shells in "Mob Drops"; the chest/shulker tiers in the upgrade tab (SimpleLib's table). */
public final class StorageJeiTests {
    private StorageJeiTests() {
    }

    public static void shellsAndTiers(GameTestHelper h) {
        List<MobDrop> drops = RecipeViewer.mobDrops(SimpleStorage.MOD_ID);
        h.assertTrue(drops.size() == 1 && drops.get(0).results().stream().map(ItemStack::getItem).toList().equals(ShulkerShells.tierShells()),
                "rare shulker shells: " + drops);
        var table = RecipeViewer.tableEntries();
        for (var entry : com.simplelib.api.InWorldUpgrades.table().values()) {
            if (!SimpleStorage.MOD_ID.equals(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(entry.to()).getNamespace())
                    || RecipeViewer.tools(entry).isEmpty() || RecipeViewer.partnerShowsNow().test(entry.from())) continue;
            h.assertTrue(table.stream().anyMatch(e -> e.output().is(entry.to().asItem())), "tier upgrade into " + entry.to() + " missing");
        }
        h.succeed();
    }
}
