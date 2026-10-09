package com.simplebuilding.neoforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.blocks.entity.custom.BackpackBlockEntity;
import com.simplebuilding.blocks.entity.custom.ModBlastFurnaceBlockEntity;
import com.simplebuilding.blocks.entity.custom.ModFurnaceBlockEntity;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.blocks.entity.custom.ModSmokerBlockEntity;
import com.simplebuilding.items.ModItemGroups;
import com.simplebuilding.items.ModItemGroupsContent;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.recipe.BackpackUpgradeRecipe;
import com.simplebuilding.recipe.CountBasedSmithingRecipe;
import com.simplebuilding.recipe.ModRecipes;
import com.simplebuilding.recipe.ReinforcedBundleRecipe;
import com.simplebuilding.recipe.UpgradeSmithingRecipe;
import com.simplebuilding.screen.BackpackMenu;
import com.simplebuilding.screen.BackpackOpenData;
import com.simplebuilding.screen.ModScreenHandlers;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.util.ModRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class NeoForgeModRegistries {
    private static final MapCodec<UpgradeSmithingRecipe> UPGRADE_SMITHING_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.optionalFieldOf("template").forGetter(UpgradeSmithingRecipe::templateIngredient),
            Ingredient.CODEC.fieldOf("base").forGetter(UpgradeSmithingRecipe::baseIngredient),
            Ingredient.CODEC.optionalFieldOf("addition").forGetter(UpgradeSmithingRecipe::additionIngredient)
    ).apply(instance, UpgradeSmithingRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, UpgradeSmithingRecipe> UPGRADE_SMITHING_STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.encode(buf, recipe.templateIngredient());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.baseIngredient());
                Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.encode(buf, recipe.additionIngredient());
            },
            buf -> new UpgradeSmithingRecipe(
                    Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.decode(buf),
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.decode(buf)
            )
    );

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Simplebuilding.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Simplebuilding.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Simplebuilding.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Simplebuilding.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeBookCategory> RECIPE_BOOK_CATEGORIES =
            DeferredRegister.create(Registries.RECIPE_BOOK_CATEGORY, Simplebuilding.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Simplebuilding.MOD_ID);
    /**
     * NeoForges Bedingungs-System fuer Datapack-Eintraege. Hierueber wird
     * {@code simplebuilding:config} als Gegenstueck zur Fabric-Resource-Condition registriert,
     * damit enableVillagerTrades/enableWanderingTrades auch auf NeoForge greifen.
     */
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, Simplebuilding.MOD_ID);

    public static final Supplier<MapCodec<ConfigLoadCondition>> CONFIG_CONDITION =
            CONDITION_CODECS.register("config", () -> ConfigLoadCondition.CODEC);

    public static final Supplier<MenuType<NetheriteHopperScreenHandler>> NETHERITE_HOPPER_MENU =
            MENUS.register("netherite_hopper", () -> IMenuTypeExtension.create(
                    (syncId, inventory, buffer) -> new NetheriteHopperScreenHandler(syncId, inventory, buffer.readBlockPos())
            ));

    public static final Supplier<MenuType<BackpackMenu>> BACKPACK_MENU =
            MENUS.register("backpack", () -> IMenuTypeExtension.create(
                    (syncId, inventory, buffer) -> new BackpackMenu(syncId, inventory, BackpackOpenData.STREAM_CODEC.decode(buffer))
            ));

    public static final Supplier<MenuType<com.simplebuilding.screen.TieredChestMenu>> TIERED_CHEST_MENU =
            MENUS.register("tiered_chest", () -> IMenuTypeExtension.create(
                    (syncId, inventory, buffer) -> new com.simplebuilding.screen.TieredChestMenu(syncId, inventory,
                            com.simplebuilding.screen.TieredChestOpenData.STREAM_CODEC.decode(buffer))
            ));

    /** Befiederungstisch (B14), nur Hauptlinie. */
    public static final Supplier<MenuType<com.simplebuilding.fletching.FletchingMenu>> FLETCHING_MENU =
            com.simplebuilding.version.McVersion.FLETCHING
                    ? MENUS.register("fletching", () -> new MenuType<>(com.simplebuilding.fletching.FletchingMenu::new,
                            net.minecraft.world.flag.FeatureFlags.VANILLA_SET))
                    : null;

    /** Autonomer Crafter, nur Hauptlinie (McVersion.AUTONOMOUS_CRAFTER). */
    public static final Supplier<MenuType<com.simplebuilding.screen.AutonomousCrafterMenu>> AUTONOMOUS_CRAFTER_MENU =
            com.simplebuilding.version.McVersion.AUTONOMOUS_CRAFTER
                    ? MENUS.register("autonomous_crafter", () -> new MenuType<>(com.simplebuilding.screen.AutonomousCrafterMenu::new,
                            net.minecraft.world.flag.FeatureFlags.VANILLA_SET))
                    : null;
    public static final Supplier<BlockEntityType<com.simplebuilding.blocks.entity.custom.AutonomousCrafterBlockEntity>> AUTONOMOUS_CRAFTER_BE =
            com.simplebuilding.version.McVersion.AUTONOMOUS_CRAFTER
                    ? BLOCK_ENTITIES.register("autonomous_crafter", () -> new BlockEntityType<>(
                            com.simplebuilding.blocks.entity.custom.AutonomousCrafterBlockEntity::new, ModBlocks.AUTONOMOUS_CRAFTER))
                    : null;

    /** Auto-Schmied, nur Hauptlinie (McVersion.AUTO_SMITHER). */
    public static final Supplier<MenuType<com.simplebuilding.screen.AutoSmitherMenu>> AUTO_SMITHER_MENU =
            com.simplebuilding.version.McVersion.AUTO_SMITHER
                    ? MENUS.register("auto_smither", () -> new MenuType<>(com.simplebuilding.screen.AutoSmitherMenu::new,
                            net.minecraft.world.flag.FeatureFlags.VANILLA_SET))
                    : null;
    public static final Supplier<BlockEntityType<com.simplebuilding.blocks.entity.custom.AutoSmitherBlockEntity>> AUTO_SMITHER_BE =
            com.simplebuilding.version.McVersion.AUTO_SMITHER
                    ? BLOCK_ENTITIES.register("auto_smither", () -> new BlockEntityType<>(
                            com.simplebuilding.blocks.entity.custom.AutoSmitherBlockEntity::new, ModBlocks.AUTO_SMITHER))
                    : null;

    public static final Supplier<BlockEntityType<com.simplebuilding.blocks.entity.custom.TieredChestBlockEntity>> TIERED_CHEST_BE =
            BLOCK_ENTITIES.register("tiered_chest", () -> new BlockEntityType<>(
                    com.simplebuilding.blocks.entity.custom.TieredChestBlockEntity::new,
                    ModBlocks.tieredChests()));

    public static final Supplier<BlockEntityType<com.simplebuilding.blocks.entity.custom.TieredShulkerBoxBlockEntity>> TIERED_SHULKER_BOX_BE =
            BLOCK_ENTITIES.register("tiered_shulker_box", () -> new BlockEntityType<>(
                    com.simplebuilding.blocks.entity.custom.TieredShulkerBoxBlockEntity::new,
                    ModBlocks.REINFORCED_SHULKER_BOX, ModBlocks.NETHERITE_SHULKER_BOX, ModBlocks.ENDERITE_SHULKER_BOX));

    public static final Supplier<BlockEntityType<BackpackBlockEntity>> BACKPACK_BE =
            BLOCK_ENTITIES.register("backpack", () -> new BlockEntityType<>(BackpackBlockEntity::new,
                    ModBlocks.BACKPACK, ModBlocks.REINFORCED_BACKPACK, ModBlocks.NETHERITE_BACKPACK, ModBlocks.ENDERITE_BACKPACK));

    public static final Supplier<BlockEntityType<com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity>> PLACED_TEMPLATE_BE =
            BLOCK_ENTITIES.register("placed_smithing_template", () -> new BlockEntityType<>(
                    com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity::new, ModBlocks.PLACED_SMITHING_TEMPLATE, ModBlocks.PLACED_BLUEPRINT));

    public static final Supplier<BlockEntityType<com.simplebuilding.blocks.entity.custom.PlacedBundleBlockEntity>> PLACED_BUNDLE_BE =
            BLOCK_ENTITIES.register("placed_bundle", () -> new BlockEntityType<>(com.simplebuilding.blocks.entity.custom.PlacedBundleBlockEntity::new, ModBlocks.PLACED_BUNDLE));

    /** Kleinteile auf einem Fleck, nur Hauptlinie (McVersion.SMALL_PLACEABLES). */
    public static final Supplier<BlockEntityType<com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity>> PLACED_SMALL_PARTS_BE =
            com.simplebuilding.version.McVersion.SMALL_PLACEABLES
                    ? BLOCK_ENTITIES.register("placed_small_parts", () -> new BlockEntityType<>(com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity::new, ModBlocks.PLACED_SMALL_PARTS))
                    : null;

    /** Ziegenhorn-Halter (Queue N24), nur Hauptlinie (McVersion.SMALL_PLACEABLES). */
    public static final Supplier<BlockEntityType<com.simplebuilding.blocks.entity.custom.GoatHornHolderBlockEntity>> GOAT_HORN_HOLDER_BE =
            com.simplebuilding.version.McVersion.SMALL_PLACEABLES
                    ? BLOCK_ENTITIES.register("goat_horn_holder", () -> new BlockEntityType<>(com.simplebuilding.blocks.entity.custom.GoatHornHolderBlockEntity::new, ModBlocks.GOAT_HORN_HOLDER))
                    : null;

    /** Schachfiguren auf einem Block, nur Hauptlinie (McVersion.CHESS). */
    public static final Supplier<BlockEntityType<com.simplebuilding.blocks.entity.custom.ChessPiecesBlockEntity>> CHESS_PIECES_BE =
            com.simplebuilding.version.McVersion.CHESS
                    ? BLOCK_ENTITIES.register("chess_pieces", () -> new BlockEntityType<>(com.simplebuilding.blocks.entity.custom.ChessPiecesBlockEntity::new, ModBlocks.CHESS_PIECES))
                    : null;

    /** Haengematten: Tuch und Seil kennen ihre Matte (jeder Winkel), nur Hauptlinie (McVersion.HAMMOCK). */
    public static final Supplier<BlockEntityType<com.simplebuilding.blocks.entity.custom.HammockBlockEntity>> HAMMOCK_BE =
            com.simplebuilding.version.McVersion.HAMMOCK
                    ? BLOCK_ENTITIES.register("hammock", () -> new BlockEntityType<>(com.simplebuilding.blocks.entity.custom.HammockBlockEntity::new, ModBlocks.hammockBlockEntityBlocks()))
                    : null;

    /** Befiederungstisch (B14): Rezepte fuers Vanilla-Rezeptbuch, nur Hauptlinie. */
    public static final Supplier<RecipeSerializer<com.simplebuilding.fletching.FletchingRecipe>> FLETCHING_SERIALIZER =
            com.simplebuilding.version.McVersion.FLETCHING
                    ? RECIPE_SERIALIZERS.register(com.simplebuilding.fletching.FletchingRecipes.ID, () -> com.simplebuilding.fletching.FletchingRecipe.SERIALIZER)
                    : null;
    public static final Supplier<RecipeType<com.simplebuilding.fletching.FletchingRecipe>> FLETCHING_TYPE =
            com.simplebuilding.version.McVersion.FLETCHING
                    ? RECIPE_TYPES.register(com.simplebuilding.fletching.FletchingRecipes.ID, com.simplebuilding.fletching.FletchingRecipes::newType)
                    : null;
    public static final Supplier<net.minecraft.world.item.crafting.RecipeBookCategory> FLETCHING_CATEGORY =
            com.simplebuilding.version.McVersion.FLETCHING
                    ? RECIPE_BOOK_CATEGORIES.register(com.simplebuilding.fletching.FletchingRecipes.ID, net.minecraft.world.item.crafting.RecipeBookCategory::new)
                    : null;

    public static final Supplier<RecipeSerializer<BackpackUpgradeRecipe>> BACKPACK_UPGRADE_SERIALIZER =
            RECIPE_SERIALIZERS.register("backpack_upgrade", () -> BackpackUpgradeRecipe.SERIALIZER);

    public static final Supplier<BlockEntityType<ModHopperBlockEntity>> MOD_HOPPER_BE =
            BLOCK_ENTITIES.register("mod_hopper", () -> new BlockEntityType<>(
                    ModHopperBlockEntity::new, ModBlocks.REINFORCED_HOPPER, ModBlocks.NETHERITE_HOPPER, ModBlocks.ENDERITE_HOPPER));
    public static final Supplier<BlockEntityType<ModBlastFurnaceBlockEntity>> MOD_BLAST_FURNACE_BE =
            BLOCK_ENTITIES.register("mod_blast_furnace", () -> new BlockEntityType<>(
                    ModBlastFurnaceBlockEntity::new, ModBlocks.REINFORCED_BLAST_FURNACE, ModBlocks.NETHERITE_BLAST_FURNACE, ModBlocks.ENDERITE_BLAST_FURNACE));
    public static final Supplier<BlockEntityType<ModFurnaceBlockEntity>> MOD_FURNACE_BE =
            BLOCK_ENTITIES.register("mod_furnace", () -> new BlockEntityType<>(
                    ModFurnaceBlockEntity::new, ModBlocks.REINFORCED_FURNACE, ModBlocks.NETHERITE_FURNACE, ModBlocks.ENDERITE_FURNACE));
    public static final Supplier<BlockEntityType<ModSmokerBlockEntity>> MOD_SMOKER_BE =
            BLOCK_ENTITIES.register("mod_smoker", () -> new BlockEntityType<>(
                    ModSmokerBlockEntity::new, ModBlocks.REINFORCED_SMOKER, ModBlocks.NETHERITE_SMOKER, ModBlocks.ENDERITE_SMOKER));

    public static final Supplier<RecipeSerializer<CountBasedSmithingRecipe>> COUNT_BASED_SMITHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("count_based_smithing", () ->
                    new RecipeSerializer<>(CountBasedSmithingRecipe.CODEC, CountBasedSmithingRecipe.STREAM_CODEC));
    public static final Supplier<RecipeType<CountBasedSmithingRecipe>> COUNT_BASED_SMITHING =
            RECIPE_TYPES.register("count_based_smithing", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return Simplebuilding.MOD_ID + ":count_based_smithing";
                }
            });
    public static final Supplier<RecipeSerializer<UpgradeSmithingRecipe>> UPGRADE_SMITHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("upgrade_smithing", () ->
                    new RecipeSerializer<>(UPGRADE_SMITHING_CODEC, UPGRADE_SMITHING_STREAM_CODEC));
    public static final Supplier<RecipeSerializer<ReinforcedBundleRecipe>> REINFORCED_BUNDLE_SERIALIZER =
            RECIPE_SERIALIZERS.register("reinforced_bundle", () ->
                    new RecipeSerializer<>(ReinforcedBundleRecipe.MAP_CODEC, ReinforcedBundleRecipe.STREAM_CODEC));

    /**
     * Ein Tab je {@link ModItemGroupsContent.Tab}, in dessen Reihenfolge; jeder weitere Tab steht
     * per {@code withTabsBefore} hinter seinem Vorgaenger, damit das Kreativinventar sie so ordnet.
     */
    public static final java.util.Map<ModItemGroupsContent.Tab, Supplier<CreativeModeTab>> TABS = registerTabs();

    private static java.util.Map<ModItemGroupsContent.Tab, Supplier<CreativeModeTab>> registerTabs() {
        java.util.Map<ModItemGroupsContent.Tab, Supplier<CreativeModeTab>> tabs = new java.util.EnumMap<>(ModItemGroupsContent.Tab.class);
        ModItemGroupsContent.Tab previous = null;
        for (ModItemGroupsContent.Tab tab : ModItemGroupsContent.Tab.values()) {
            ModItemGroupsContent.Tab before = previous;
            tabs.put(tab, CREATIVE_TABS.register(tab.id, () -> {
                CreativeModeTab.Builder builder = CreativeModeTab.builder(CreativeModeTab.Row.TOP, tab.ordinal())
                        .icon(tab.icon)
                        .title(Component.translatable(tab.translationKey()))
                        .displayItems((displayContext, entries) -> ModItemGroupsContent.populate(tab, entries, displayContext.holders()));
                if (before != null) {
                    builder.withTabsBefore(net.minecraft.resources.ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                            net.minecraft.resources.Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, before.id)));
                }
                return builder.build();
            }));
            previous = tab;
        }
        return tabs;
    }

    /**
     * Entwickler-Tab hinter den Tabs der Mod. Immer registriert, aber nur gefuellt, wenn
     * {@link com.simplebuilding.items.DevEnchantedTab#isShown()} gilt - leer blendet Vanilla ihn aus.
     */
    public static final Supplier<CreativeModeTab> DEV_ENCHANTED_TAB = CREATIVE_TABS.register(
            com.simplebuilding.items.DevEnchantedTab.ID,
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, ModItemGroupsContent.Tab.values().length)
                    .icon(com.simplebuilding.items.DevEnchantedTab::icon)
                    .title(Component.translatable(com.simplebuilding.items.DevEnchantedTab.translationKey()))
                    .displayItems((displayContext, entries) ->
                            com.simplebuilding.items.DevEnchantedTab.populateIfShown(entries, displayContext.holders()))
                    .withTabsBefore(net.minecraft.resources.ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                            net.minecraft.resources.Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID,
                                    ModItemGroupsContent.Tab.values()[ModItemGroupsContent.Tab.values().length - 1].id)))
                    .build());

    private NeoForgeModRegistries() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_BOOK_CATEGORIES.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        CONDITION_CODECS.register(modEventBus);
    }

    public static void assignStaticFields() {
        ModScreenHandlers.NETHERITE_HOPPER_SCREEN_HANDLER = NETHERITE_HOPPER_MENU.get();
        ModScreenHandlers.BACKPACK_MENU = BACKPACK_MENU.get();
        ModScreenHandlers.TIERED_CHEST_MENU = TIERED_CHEST_MENU.get();
        if (FLETCHING_MENU != null) ModScreenHandlers.FLETCHING_MENU = FLETCHING_MENU.get();
        if (AUTO_SMITHER_MENU != null) ModScreenHandlers.AUTO_SMITHER_MENU = AUTO_SMITHER_MENU.get();
        if (AUTO_SMITHER_BE != null) ModBlockEntities.AUTO_SMITHER_BE = AUTO_SMITHER_BE.get();
        if (AUTONOMOUS_CRAFTER_MENU != null) ModScreenHandlers.AUTONOMOUS_CRAFTER_MENU = AUTONOMOUS_CRAFTER_MENU.get();
        if (AUTONOMOUS_CRAFTER_BE != null) ModBlockEntities.AUTONOMOUS_CRAFTER_BE = AUTONOMOUS_CRAFTER_BE.get();
        if (FLETCHING_TYPE != null) {
            com.simplebuilding.fletching.FletchingRecipes.TYPE = FLETCHING_TYPE.get();
            com.simplebuilding.fletching.FletchingRecipes.CATEGORY = FLETCHING_CATEGORY.get();
        }
        ModBlockEntities.TIERED_CHEST_BE = TIERED_CHEST_BE.get();
        ModBlockEntities.TIERED_SHULKER_BOX_BE = TIERED_SHULKER_BOX_BE.get();
        ModBlockEntities.BACKPACK_BE = BACKPACK_BE.get();
        ModBlockEntities.PLACED_TEMPLATE_BE = PLACED_TEMPLATE_BE.get();
        ModBlockEntities.PLACED_BUNDLE_BE = PLACED_BUNDLE_BE.get();
        if (PLACED_SMALL_PARTS_BE != null) ModBlockEntities.PLACED_SMALL_PARTS_BE = PLACED_SMALL_PARTS_BE.get();
        if (GOAT_HORN_HOLDER_BE != null) ModBlockEntities.GOAT_HORN_HOLDER_BE = GOAT_HORN_HOLDER_BE.get();
        if (CHESS_PIECES_BE != null) ModBlockEntities.CHESS_PIECES_BE = CHESS_PIECES_BE.get();
        if (HAMMOCK_BE != null) ModBlockEntities.HAMMOCK_BE = HAMMOCK_BE.get();
        ModBlockEntities.MOD_HOPPER_BE = MOD_HOPPER_BE.get();
        ModBlockEntities.MOD_BLAST_FURNACE_BE = MOD_BLAST_FURNACE_BE.get();
        ModBlockEntities.MOD_FURNACE_BE = MOD_FURNACE_BE.get();
        ModBlockEntities.MOD_SMOKER_BE = MOD_SMOKER_BE.get();
        ModRecipes.COUNT_BASED_SMITHING_SERIALIZER = COUNT_BASED_SMITHING_SERIALIZER.get();
        ModRecipes.COUNT_BASED_SMITHING = COUNT_BASED_SMITHING.get();
        ModRecipes.UPGRADE_SMITHING_SERIALIZER = UPGRADE_SMITHING_SERIALIZER.get();
        ModRegistries.REINFORCED_BUNDLE_SERIALIZER = REINFORCED_BUNDLE_SERIALIZER.get();
        TABS.forEach((tab, holder) -> ModItemGroups.GROUPS.put(tab, holder.get()));
    }
}
