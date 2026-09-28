package com.simplebuilding.compat.rei;

import com.simplebuilding.compat.InWorldRecipeCatalog;
import com.simplebuilding.compat.MobDropCatalog;
import com.simplebuilding.compat.RecipelessJeiInfo;
import com.simplebuilding.tweaks.item.TweaksJeiInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.displays.DefaultInformationDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REI support, client half - the same content as the JEI plugin: one category per in-world
 * transformation ({@link InWorldRecipeCatalog}), "Mob drops" ({@link MobDropCatalog}), and REI
 * information pages for the items without a visible recipe ({@link RecipelessJeiInfo},
 * {@link TweaksJeiInfo}; same texts as the JEI info pages). The count-based smithing recipes come
 * from {@link SimplebuildingReiCommonPlugin}, because REI builds recipe displays on the server.
 *
 * <p><b>Only loaded by REI.</b> Fabric finds it through the {@code rei_client} entrypoint in
 * {@code fabric.mod.json}, NeoForge through the annotated subclass in the NeoForge module's {@code src/rei/java}
 * (the annotation only exists in REI's NeoForge API, so it cannot sit on this shared class).
 * Nothing in the mod references it, so without REI neither it nor the REI API (compileOnly) is
 * ever loaded. Not final for that subclass.
 */
public class SimplebuildingReiClientPlugin implements REIClientPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("simplebuilding/rei");

    private InWorldRecipeCatalog.Catalog catalog;

    private InWorldRecipeCatalog.Catalog catalog() {
        if (catalog == null) {
            catalog = InWorldRecipeCatalog.build();
            if (!catalog.problems().isEmpty()) {
                LOGGER.warn("In-world REI catalog skipped {} entries: {}", catalog.problems().size(), catalog.problems());
            }
        }
        return catalog;
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        // REI reloads its plugins on every world join and resource reload; build the catalog afresh.
        catalog = null;
        for (InWorldRecipeCatalog.Kind kind : InWorldRecipeCatalog.Kind.values()) {
            registry.add(new InWorldReiCategory(kind, catalog().of(kind)));
            for (Item tool : catalog().toolsOf(kind)) {
                registry.addWorkstations(InWorldDisplay.category(kind), EntryStacks.of(tool));
            }
        }
        registry.add(new MobDropReiCategory());
        registry.addWorkstations(MobDropDisplay.CATEGORY, EntryStacks.of(Items.CREEPER_SPAWN_EGG));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (InWorldRecipeCatalog.Entry entry : catalog().entries()) {
            registry.add(new InWorldDisplay(entry));
        }
        for (MobDropCatalog.Drop drop : MobDropCatalog.drops()) {
            registry.add(new MobDropDisplay(drop));
        }
        // Information pages of the pads, plates and tools taken over from Simple Tweaks ...
        for (Map.Entry<String, List<ItemLike>> family : TweaksJeiInfo.families().entrySet()) {
            addInfo(registry, family.getValue(), TweaksJeiInfo.KEY_PREFIX + family.getKey());
        }
        // ... and of the items without a recipe REI can show (loot, ore mining, legacy items).
        for (Map.Entry<String, List<ItemLike>> page : RecipelessJeiInfo.pages().entrySet()) {
            addInfo(registry, page.getValue(), RecipelessJeiInfo.KEY_PREFIX + page.getKey());
        }
    }

    private static void addInfo(DisplayRegistry registry, List<ItemLike> items, String key) {
        if (items.isEmpty()) {
            return;
        }
        // REI info pages carry a title; the first item's name, the text is the JEI info text.
        Component title = new ItemStack(items.get(0)).getHoverName();
        registry.add(DefaultInformationDisplay.createFromEntries(EntryIngredients.ofItems(new ArrayList<>(items)), title)
                .line(Component.translatable(key)));
    }
}
