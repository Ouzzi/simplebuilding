package com.simplebeacons;

import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.MobDrop;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Simple Beacons in the recipe viewer (AUDM24-3): the Strong Upgrade Shard has no recipe, only the Vanilla Wither drops it
 * ({@link BeaconsRegistry#witherDied}) - shown in SimpleLib's "Mob Drops" tab. Free of JEI classes; registered from
 * {@link SimpleBeacons#init}.
 */
public final class BeaconsViewer {
    private BeaconsViewer() {
    }

    public static void register() {
        RecipeViewer.mobDrops(SimpleBeacons.MOD_ID, BeaconsViewer::drops);
    }

    public static List<MobDrop> drops() {
        if (BeaconsRegistry.STRONG_UPGRADE_SHARD == null) return List.of();
        return List.of(new MobDrop(SimpleBeacons.MOD_ID + "/wither_shard", EntityTypes.WITHER, new ItemStack(Items.NETHER_STAR),
                ItemStack.EMPTY, List.of(new ItemStack(BeaconsRegistry.STRONG_UPGRADE_SHARD)),
                List.of(Component.translatable("jei.simplebeacons.note.wither_shard"))));
    }
}
