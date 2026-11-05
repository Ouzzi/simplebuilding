package com.simplearrows.fletching;

import com.simplearrows.SimpleArrows;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.level.block.DispenserBlock;

/**
 * The Fletching Table's registrations (b-arrows03, 04.11.: moved from the SimpleBuilding core, ids moved from
 * {@code simplebuilding:} - {@link SimpleArrows#registerIdMoves}): the arrow parts component, the crafted arrow (item and entity,
 * Vanilla arrow sizes), the menu, the recipe type with its serializer and the three recipe book categories. Vanilla's
 * Fletching Table block opens the menu ({@code FletchingTableMixin}).
 */
public final class Fletchings {
    public static final String CRAFTED_ARROW_ID = "crafted_arrow";
    public static final ResourceKey<EntityType<?>> CRAFTED_ARROW_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SimpleArrows.id(CRAFTED_ARROW_ID));
    public static final EntityType<CraftedArrow> CRAFTED_ARROW_ENTITY = EntityType.Builder.<CraftedArrow>of(CraftedArrow::new, MobCategory.MISC)
            .noLootTable().sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20).build(CRAFTED_ARROW_KEY);
    /** The parts of a crafted arrow (until 04.11. {@code simplebuilding:arrow_parts}). */
    public static DataComponentType<ArrowParts.Parts> ARROW_PARTS;
    public static Item CRAFTED_ARROW;
    public static MenuType<FletchingMenu> MENU;

    private Fletchings() {}

    public static void registerAll() {
        ARROW_PARTS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleArrows.id("arrow_parts"),
                DataComponentType.<ArrowParts.Parts>builder().persistent(ArrowParts.Parts.CODEC).networkSynchronized(ArrowParts.Parts.STREAM_CODEC).build());
        Registry.register(BuiltInRegistries.ENTITY_TYPE, CRAFTED_ARROW_KEY, CRAFTED_ARROW_ENTITY);
        ResourceKey<Item> item = ResourceKey.create(Registries.ITEM, SimpleArrows.id(CRAFTED_ARROW_ID));
        CRAFTED_ARROW = Registry.register(BuiltInRegistries.ITEM, item, new CraftedArrowItem(new Item.Properties().setId(item)));
        DispenserBlock.registerProjectileBehavior(CRAFTED_ARROW);
        MENU = Registry.register(BuiltInRegistries.MENU, SimpleArrows.id(FletchingRecipes.ID),
                new MenuType<>(FletchingMenu::new, FeatureFlags.VANILLA_SET));
        Identifier recipes = SimpleArrows.id(FletchingRecipes.ID);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, recipes, FletchingRecipe.SERIALIZER);
        FletchingRecipes.TYPE = Registry.register(BuiltInRegistries.RECIPE_TYPE, recipes, FletchingRecipes.newType());
        for (FletchingRecipe.Kind kind : FletchingRecipe.Kind.values()) {
            RecipeBookCategory category = Registry.register(BuiltInRegistries.RECIPE_BOOK_CATEGORY,
                    SimpleArrows.id(FletchingRecipes.categoryId(kind)), new RecipeBookCategory());
            switch (kind) {
                case TIP -> FletchingRecipes.TIP_CATEGORY = category;
                case SHAFT -> FletchingRecipes.SHAFT_CATEGORY = category;
                case FLETCHING -> FletchingRecipes.FLETCHING_CATEGORY = category;
            }
        }
    }

    /** The recipe paths of all parts (moved from {@code simplebuilding:} on 04.11., {@link SimpleArrows#registerIdMoves}). */
    public static List<String> recipePaths() {
        List<String> out = new ArrayList<>();
        for (FletchingRecipe recipe : FletchingRecipe.all()) out.add(recipe.idPath());
        return out;
    }

    /** Every crafted arrow of this installation ({@code count} each), in tip order (tab, test centre). */
    public static List<ItemStack> arrowStacks(int count) {
        List<ItemStack> out = new ArrayList<>();
        for (ArrowParts.Parts parts : ArrowParts.allCombinations()) out.add(ArrowParts.stack(parts, count));
        return out;
    }

    /** JEI24: one entry - any tip, shaft and fletching of this installation on the Fletching Table give a crafted arrow. */
    public static List<com.simplelib.compat.jei.InWorldUpgrade> recipeViewerEntries() {
        if (CRAFTED_ARROW == null) return List.of();
        List<com.simplelib.compat.jei.InWorldUpgrade.Input> inputs = new ArrayList<>();
        for (FletchingRecipe.Kind kind : FletchingRecipe.Kind.values()) {
            List<Item> parts = new ArrayList<>();
            for (String part : kind.parts()) {
                Item item = kind.input(part);
                if (item != net.minecraft.world.item.Items.AIR && !parts.contains(item)) parts.add(item);
            }
            if (parts.isEmpty()) return List.of();
            inputs.add(new com.simplelib.compat.jei.InWorldUpgrade.Input(parts, 1));
        }
        return List.of(new com.simplelib.compat.jei.InWorldUpgrade("simplearrows/fletching", inputs,
                List.of(net.minecraft.world.item.Items.FLETCHING_TABLE), new ItemStack(CRAFTED_ARROW),
                List.of(net.minecraft.network.chat.Component.translatable("jei.simplearrows.note.fletching"))));
    }
}
