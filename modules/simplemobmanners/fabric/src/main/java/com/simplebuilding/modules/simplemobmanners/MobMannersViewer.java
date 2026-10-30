package com.simplebuilding.modules.simplemobmanners;

import com.simplelib.api.RecipeViewer;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Simple Mob Manners in the recipe viewer (AUDM24-3): the Bucket of Frog has no recipe, a water bucket on a frog makes it -
 * an info page via SimpleLib's {@link RecipeViewer} (JEI optional). Fabric tree: the shared tree does not see SimpleLib.
 */
public final class MobMannersViewer {
    private MobMannersViewer() {
    }

    public static void register() {
        RecipeViewer.info(SimpleMobManners.MOD_ID, MobMannersViewer::pages);
    }

    public static List<RecipeViewer.Info> pages() {
        return List.of(new RecipeViewer.Info(List.of(new ItemStack(FrogBucket.item())),
                List.of(Component.translatable("jei.simplemobmanners.info.frog_bucket"))));
    }
}
