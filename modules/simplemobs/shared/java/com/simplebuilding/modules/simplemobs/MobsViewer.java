package com.simplebuilding.modules.simplemobs;

import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.InWorldUpgrade;
import com.simplelib.compat.jei.MobDrop;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Simple Mobs in the recipe viewer (JEI24), free of JEI classes: the loot tables of its mobs (SimpleLib reads them from
 * this jar), the Blighted and Abyssal Wither's drops that their code places (heart, gem, nuggets, stars) and dressing a
 * Shulker in a Shellker Shell. Registered from {@link MobsRegistry#registerAll}.
 */
public final class MobsViewer {
    public static final String OWNER = "simplemobs";
    public static final String NOTE = "jei.simplemobs.note.";

    private MobsViewer() {
    }

    public static void register() {
        RecipeViewer.lootDrops(OWNER, MobsViewer.class);
        RecipeViewer.mobDrops(OWNER, MobsViewer::witherDrops);
        RecipeViewer.special(OWNER, MobsViewer::special);
    }

    /** One entry per Wither stage: what {@code BlightedWither#dropCustomDeathLoot} places, in that order. */
    public static List<MobDrop> witherDrops() {
        List<MobDrop> out = new ArrayList<>();
        wither(out, "blighted_wither", WitherMobs.BLIGHTED_WITHER, WitherLogic.NETHERITE);
        wither(out, "abyssal_wither", WitherMobs.ABYSSAL_WITHER, WitherLogic.ENDERITE);
        return out;
    }

    private static void wither(List<MobDrop> out, String id, EntityType<?> type, WitherLogic.Stage stage) {
        List<ItemStack> results = new ArrayList<>();
        if (stage.stars()) results.add(new ItemStack(Items.NETHER_STAR, WitherLogic.STARS_MAX));
        results.add(new ItemStack(WitherMobs.HEART, WitherLogic.HEARTS));
        if (stage.gems() > 0) results.add(new ItemStack(WitherMobs.GEM, stage.gems()));
        ItemStack nuggets = BlightedWither.nuggets(stage, WitherLogic.NUGGETS_MAX);
        if (!nuggets.isEmpty()) results.add(nuggets);
        out.add(new MobDrop(OWNER + "/code/" + id, type, RecipeViewer.egg(type), ItemStack.EMPTY, results,
                List.of(Component.translatable(NOTE + "wither_drops", type.getDescription()),
                        Component.translatable(NOTE + "wither_counts", WitherLogic.NUGGETS_MIN, WitherLogic.NUGGETS_MAX))));
    }

    public static List<InWorldUpgrade> special() {
        return List.of(new InWorldUpgrade(OWNER + "/shellker_dress",
                List.of(InWorldUpgrade.Input.of(Items.SHULKER_SPAWN_EGG, 1)), List.of(EndNetherMobs.SHELL),
                new ItemStack(EndNetherMobs.SHELLKER_EGG), List.of(Component.translatable(NOTE + "shellker_dress"))));
    }
}
