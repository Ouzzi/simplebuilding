package com.simplestorage.rare;

import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.MobDrop;
import com.simplestorage.SimpleStorage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Simple Storage in the recipe viewer's "Mob Drops" tab (JEI24): the rare end city shulkers drop 0-2 tier shells of their
 * tier ({@link RareShulkers#dropShells}). The chest and shulker tiers are SimpleLib's upgrade table, the backpack and
 * bundle upgrades are shaped recipes JEI shows itself. Free of JEI classes; registered from {@link SimpleStorage#init}.
 */
public final class StorageViewer {
    private StorageViewer() {
    }

    public static void register() {
        RecipeViewer.mobDrops(SimpleStorage.MOD_ID, StorageViewer::drops);
    }

    public static List<MobDrop> drops() {
        List<ItemStack> shells = new ArrayList<>();
        for (Item shell : ShulkerShells.tierShells()) {
            if (shell != null && shell != Items.AIR) shells.add(new ItemStack(shell, RareShulkers.MAX_SHELL_DROPS));
        }
        if (shells.isEmpty()) return List.of();
        return List.of(new MobDrop(SimpleStorage.MOD_ID + "/rare_shulker_shells", EntityType.SHULKER, new ItemStack(Items.SHULKER_SPAWN_EGG),
                ItemStack.EMPTY, shells, List.of(Component.translatable("jei.simplestorage.note.rare_shulker", RareShulkers.MAX_SHELL_DROPS),
                        Component.translatable("jei.simplestorage.note.rare_shulker_where"))));
    }
}
