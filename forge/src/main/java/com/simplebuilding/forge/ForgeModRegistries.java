package com.simplebuilding.forge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.blocks.entity.custom.ModBlastFurnaceBlockEntity;
import com.simplebuilding.blocks.entity.custom.ModFurnaceBlockEntity;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.blocks.entity.custom.ModSmokerBlockEntity;
import com.simplebuilding.items.ModItemGroups;
import com.simplebuilding.items.ModItemGroupsContent;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.recipe.CountBasedSmithingRecipe;
import com.simplebuilding.recipe.ModRecipes;
import com.simplebuilding.recipe.ReinforcedBundleRecipe;
import com.simplebuilding.recipe.UpgradeSmithingRecipe;
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
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public final class ForgeModRegistries {
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
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Simplebuilding.MOD_ID);
    /** Forge's {@code forge:condition_codecs}; holds {@code simplebuilding:config} for the trade jsons. */
    public static final DeferredRegister<MapCodec<? extends net.minecraftforge.common.crafting.conditions.ICondition>> CONDITION_CODECS =
            DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.Keys.CONDITION_SERIALIZERS, Simplebuilding.MOD_ID);

    public static final RegistryObject<MapCodec<ConfigLoadCondition>> CONFIG_CONDITION =
            CONDITION_CODECS.register("config", () -> ConfigLoadCondition.CODEC);

    public static final RegistryObject<MenuType<NetheriteHopperScreenHandler>> NETHERITE_HOPPER_MENU =
            MENUS.register("netherite_hopper", () -> IForgeMenuType.create(
                    (syncId, inventory, buffer) -> new NetheriteHopperScreenHandler(syncId, inventory, buffer.readBlockPos())
            ));

    public static final RegistryObject<MenuType<com.simplebuilding.screen.BackpackMenu>> BACKPACK_MENU =
            MENUS.register("backpack", () -> IForgeMenuType.create(
                    (syncId, inventory, buffer) -> new com.simplebuilding.screen.BackpackMenu(syncId, inventory,
                            com.simplebuilding.screen.BackpackOpenData.STREAM_CODEC.decode(buffer))
            ));

    public static final RegistryObject<MenuType<com.simplebuilding.screen.TieredChestMenu>> TIERED_CHEST_MENU =
            MENUS.register("tiered_chest", () -> IForgeMenuType.create(
                    (syncId, inventory, buffer) -> new com.simplebuilding.screen.TieredChestMenu(syncId, inventory,
                            com.simplebuilding.screen.TieredChestOpenData.STREAM_CODEC.decode(buffer))
            ));

    /** Befiederungstisch (B14), nur Hauptlinie. */
    public static final RegistryObject<MenuType<com.simplebuilding.fletching.FletchingMenu>> FLETCHING_MENU =
            com.simplebuilding.version.McVersion.FLETCHING
                    ? MENUS.register("fletching", () -> new MenuType<>(com.simplebuilding.fletching.FletchingMenu::new,
                            net.minecraft.world.flag.FeatureFlags.VANILLA_SET))
                    : null;

    public static final RegistryObject<BlockEntityType<com.simplebuilding.blocks.entity.custom.TieredChestBlockEntity>> TIERED_CHEST_BE =
            BLOCK_ENTITIES.register("tiered_chest", () -> new BlockEntityType<com.simplebuilding.blocks.entity.custom.TieredChestBlockEntity>(
                    com.simplebuilding.blocks.entity.custom.TieredChestBlockEntity::new,
                    Set.of(ModBlocks.REINFORCED_CHEST, ModBlocks.NETHERITE_CHEST, ModBlocks.ENDERITE_CHEST)));

    public static final RegistryObject<BlockEntityType<com.simplebuilding.blocks.entity.custom.TieredShulkerBoxBlockEntity>> TIERED_SHULKER_BOX_BE =
            BLOCK_ENTITIES.register("tiered_shulker_box", () -> new BlockEntityType<com.simplebuilding.blocks.entity.custom.TieredShulkerBoxBlockEntity>(
                    com.simplebuilding.blocks.entity.custom.TieredShulkerBoxBlockEntity::new,
                    Set.of(ModBlocks.REINFORCED_SHULKER_BOX, ModBlocks.NETHERITE_SHULKER_BOX, ModBlocks.ENDERITE_SHULKER_BOX)));

    public static final RegistryObject<BlockEntityType<com.simplebuilding.blocks.entity.custom.BackpackBlockEntity>> BACKPACK_BE =
            BLOCK_ENTITIES.register("backpack", () -> new BlockEntityType<com.simplebuilding.blocks.entity.custom.BackpackBlockEntity>(
                    com.simplebuilding.blocks.entity.custom.BackpackBlockEntity::new,
                    Set.of(ModBlocks.BACKPACK, ModBlocks.REINFORCED_BACKPACK, ModBlocks.NETHERITE_BACKPACK, ModBlocks.ENDERITE_BACKPACK)));

    public static final RegistryObject<BlockEntityType<com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity>> PLACED_TEMPLATE_BE =
            BLOCK_ENTITIES.register("placed_smithing_template", () -> new BlockEntityType<com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity>(
                    com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity::new, Set.of(ModBlocks.PLACED_SMITHING_TEMPLATE, ModBlocks.PLACED_BLUEPRINT)));

    public static final RegistryObject<BlockEntityType<com.simplebuilding.blocks.entity.custom.PlacedBundleBlockEntity>> PLACED_BUNDLE_BE =
            BLOCK_ENTITIES.register("placed_bundle", () -> new BlockEntityType<com.simplebuilding.blocks.entity.custom.PlacedBundleBlockEntity>(com.simplebuilding.blocks.entity.custom.PlacedBundleBlockEntity::new, Set.of(ModBlocks.PLACED_BUNDLE)));

    /** Kleinteile auf einem Fleck, nur Hauptlinie (McVersion.SMALL_PLACEABLES). */
    public static final RegistryObject<BlockEntityType<com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity>> PLACED_SMALL_PARTS_BE =
            com.simplebuilding.version.McVersion.SMALL_PLACEABLES
                    ? BLOCK_ENTITIES.register("placed_small_parts", () -> new BlockEntityType<com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity>(
                            com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity::new, Set.of(ModBlocks.PLACED_SMALL_PARTS)))
                    : null;

    public static final RegistryObject<RecipeSerializer<com.simplebuilding.recipe.BackpackUpgradeRecipe>> BACKPACK_UPGRADE_SERIALIZER =
            RECIPE_SERIALIZERS.register("backpack_upgrade", () -> com.simplebuilding.recipe.BackpackUpgradeRecipe.SERIALIZER);

    public static final RegistryObject<BlockEntityType<ModHopperBlockEntity>> MOD_HOPPER_BE =
            BLOCK_ENTITIES.register("mod_hopper", () -> new BlockEntityType<ModHopperBlockEntity>(
                    ModHopperBlockEntity::new, Set.of(ModBlocks.REINFORCED_HOPPER, ModBlocks.NETHERITE_HOPPER, ModBlocks.ENDERITE_HOPPER)));
    public static final RegistryObject<BlockEntityType<ModBlastFurnaceBlockEntity>> MOD_BLAST_FURNACE_BE =
            BLOCK_ENTITIES.register("mod_blast_furnace", () -> new BlockEntityType<ModBlastFurnaceBlockEntity>(
                    ModBlastFurnaceBlockEntity::new, Set.of(ModBlocks.REINFORCED_BLAST_FURNACE, ModBlocks.NETHERITE_BLAST_FURNACE, ModBlocks.ENDERITE_BLAST_FURNACE)));
    public static final RegistryObject<BlockEntityType<ModFurnaceBlockEntity>> MOD_FURNACE_BE =
            BLOCK_ENTITIES.register("mod_furnace", () -> new BlockEntityType<ModFurnaceBlockEntity>(
                    ModFurnaceBlockEntity::new, Set.of(ModBlocks.REINFORCED_FURNACE, ModBlocks.NETHERITE_FURNACE, ModBlocks.ENDERITE_FURNACE)));
    public static final RegistryObject<BlockEntityType<ModSmokerBlockEntity>> MOD_SMOKER_BE =
            BLOCK_ENTITIES.register("mod_smoker", () -> new BlockEntityType<ModSmokerBlockEntity>(
                    ModSmokerBlockEntity::new, Set.of(ModBlocks.REINFORCED_SMOKER, ModBlocks.NETHERITE_SMOKER, ModBlocks.ENDERITE_SMOKER)));

    public static final RegistryObject<RecipeSerializer<CountBasedSmithingRecipe>> COUNT_BASED_SMITHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("count_based_smithing", () ->
                    new RecipeSerializer<>(CountBasedSmithingRecipe.CODEC, CountBasedSmithingRecipe.STREAM_CODEC));
    public static final RegistryObject<RecipeType<CountBasedSmithingRecipe>> COUNT_BASED_SMITHING =
            RECIPE_TYPES.register("count_based_smithing", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return Simplebuilding.MOD_ID + ":count_based_smithing";
                }
            });
    public static final RegistryObject<RecipeSerializer<UpgradeSmithingRecipe>> UPGRADE_SMITHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("upgrade_smithing", () ->
                    new RecipeSerializer<>(UPGRADE_SMITHING_CODEC, UPGRADE_SMITHING_STREAM_CODEC));
    public static final RegistryObject<RecipeSerializer<ReinforcedBundleRecipe>> REINFORCED_BUNDLE_SERIALIZER =
            RECIPE_SERIALIZERS.register("reinforced_bundle", () ->
                    new RecipeSerializer<>(ReinforcedBundleRecipe.MAP_CODEC, ReinforcedBundleRecipe.STREAM_CODEC));

    /**
     * Ein Tab je {@link ModItemGroupsContent.Tab}, in dessen Reihenfolge; jeder weitere Tab steht
     * per {@code withTabsBefore} hinter seinem Vorgaenger, damit das Kreativinventar sie so ordnet.
     */
    public static final java.util.Map<ModItemGroupsContent.Tab, RegistryObject<CreativeModeTab>> TABS = registerTabs();

    private static java.util.Map<ModItemGroupsContent.Tab, RegistryObject<CreativeModeTab>> registerTabs() {
        java.util.Map<ModItemGroupsContent.Tab, RegistryObject<CreativeModeTab>> tabs = new java.util.EnumMap<>(ModItemGroupsContent.Tab.class);
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
    public static final RegistryObject<CreativeModeTab> DEV_ENCHANTED_TAB = CREATIVE_TABS.register(
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

    private ForgeModRegistries() {
    }

    public static void register(BusGroup modBus) {
        MENUS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        RECIPE_TYPES.register(modBus);
        CREATIVE_TABS.register(modBus);
        CONDITION_CODECS.register(modBus);
    }

    public static void assignStaticFields() {
        ModScreenHandlers.NETHERITE_HOPPER_SCREEN_HANDLER = NETHERITE_HOPPER_MENU.get();
        ModScreenHandlers.BACKPACK_MENU = BACKPACK_MENU.get();
        ModScreenHandlers.TIERED_CHEST_MENU = TIERED_CHEST_MENU.get();
        if (FLETCHING_MENU != null) ModScreenHandlers.FLETCHING_MENU = FLETCHING_MENU.get();
        ModBlockEntities.TIERED_CHEST_BE = TIERED_CHEST_BE.get();
        ModBlockEntities.TIERED_SHULKER_BOX_BE = TIERED_SHULKER_BOX_BE.get();
        ModBlockEntities.BACKPACK_BE = BACKPACK_BE.get();
        ModBlockEntities.PLACED_TEMPLATE_BE = PLACED_TEMPLATE_BE.get();
        ModBlockEntities.PLACED_BUNDLE_BE = PLACED_BUNDLE_BE.get();
        if (PLACED_SMALL_PARTS_BE != null) ModBlockEntities.PLACED_SMALL_PARTS_BE = PLACED_SMALL_PARTS_BE.get();
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
