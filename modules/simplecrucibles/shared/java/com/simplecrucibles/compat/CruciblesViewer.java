package com.simplecrucibles.compat;

import com.simplecrucibles.SimpleCrucibles;
import com.simplecrucibles.api.SimpleCruciblesApi;
import com.simplecrucibles.crucible.HammerWays;
import com.simplelib.api.RecipeViewer;
import com.simplelib.api.SimpleLibApi;
import com.simplelib.compat.jei.InWorldUpgrade;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Simple Crucibles in the recipe viewer (JEI24, was SimpleBuilding's "Cauldron and Crucible" tab, so it was missing with
 * the module alone): building the iron crucible on a cauldron and attaching a barrel, with the axe (Vanilla axe ways) or
 * the hammer of a hammer mod. The tier upgrades of cauldron, crucibles and barrels come from SimpleLib's upgrade table.
 * Free of JEI classes; registered from {@link SimpleCrucibles#init}.
 */
public final class CruciblesViewer {
    public static final String NOTE = "jei.simplecrucibles.note.";
    private static final List<String> CRUCIBLES = List.of("iron_crucible", "reinforced_crucible", "netherite_crucible", "enderite_crucible");
    private static final List<String> BARRELS = List.of("copper_barrel", "reinforced_barrel", "netherite_barrel", "enderite_barrel");

    private CruciblesViewer() {
    }

    public static void register() {
        RecipeViewer.inWorld(SimpleCrucibles.MOD_ID, CruciblesViewer::entries);
    }

    public static List<InWorldUpgrade> entries() {
        List<InWorldUpgrade> out = new ArrayList<>();
        List<Item> tools = RecipeViewer.upgradeTools();
        if (tools.isEmpty()) return out;
        int damage = SimpleLibApi.axeWaysEnabled() ? 1 : HammerWays.HAMMER_DAMAGE_PER_STRIKE;
        Item iron = item("iron_crucible");
        List<Item> walls = RecipeViewer.tagged(TagKey.create(Registries.ITEM, SimpleCrucibles.id("crucible_walls")));
        List<Item> handles = RecipeViewer.tagged(TagKey.create(Registries.ITEM, SimpleCrucibles.id("crucible_handles")));
        int strikes = SimpleCruciblesApi.buildStrikes();
        int wallStrikes = SimpleCruciblesApi.buildWallStrikes();
        if (iron != null && !walls.isEmpty() && !handles.isEmpty() && strikes > wallStrikes) {
            out.add(new InWorldUpgrade("simplecrucibles/crucible_build",
                    List.of(InWorldUpgrade.Input.of(Items.CAULDRON, 1), new InWorldUpgrade.Input(walls, wallStrikes),
                            new InWorldUpgrade.Input(handles, strikes - wallStrikes)),
                    tools, new ItemStack(iron),
                    List.of(Component.translatable(NOTE + "crucible_build.how", strikes),
                            Component.translatable(NOTE + "crucible_build.parts", wallStrikes, strikes - wallStrikes),
                            Component.translatable(NOTE + "strike_damage", damage))));
        }
        List<Item> barrels = items(BARRELS);
        List<Item> crucibles = items(CRUCIBLES);
        int attach = SimpleCruciblesApi.attachStrikes();
        if (!barrels.isEmpty() && !crucibles.isEmpty() && attach > 0) {
            out.add(new InWorldUpgrade("simplecrucibles/barrel_attach",
                    List.of(new InWorldUpgrade.Input(barrels, 1), new InWorldUpgrade.Input(crucibles, 1)), tools,
                    new ItemStack(barrels.get(0)),
                    List.of(Component.translatable(NOTE + "barrel_attach.how", attach),
                            Component.translatable(NOTE + "barrel_attach.use", SimpleCruciblesApi.attachedBarrelSlots()),
                            Component.translatable(NOTE + "strike_damage", damage))));
        }
        return out;
    }

    private static List<Item> items(List<String> paths) {
        List<Item> out = new ArrayList<>();
        for (String path : paths) {
            Item item = item(path);
            if (item != null) out.add(item);
        }
        return out;
    }

    private static Item item(String path) {
        Item item = BuiltInRegistries.ITEM.getOptional(SimpleCrucibles.id(path)).orElse(Items.AIR);
        return item == Items.AIR ? null : item;
    }
}
