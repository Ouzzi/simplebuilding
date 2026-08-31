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

    public static final RegistryObject<MenuType<NetheriteHopperScreenHandler>> NETHERITE_HOPPER_MENU =
            MENUS.register("netherite_hopper", () -> IForgeMenuType.create(
                    (syncId, inventory, buffer) -> new NetheriteHopperScreenHandler(syncId, inventory, buffer.readBlockPos())
            ));

    public static final RegistryObject<BlockEntityType<ModHopperBlockEntity>> MOD_HOPPER_BE =
            BLOCK_ENTITIES.register("mod_hopper", () -> new BlockEntityType<ModHopperBlockEntity>(
                    ModHopperBlockEntity::new, Set.of(ModBlocks.REINFORCED_HOPPER, ModBlocks.NETHERITE_HOPPER)));
    public static final RegistryObject<BlockEntityType<ModBlastFurnaceBlockEntity>> MOD_BLAST_FURNACE_BE =
            BLOCK_ENTITIES.register("mod_blast_furnace", () -> new BlockEntityType<ModBlastFurnaceBlockEntity>(
                    ModBlastFurnaceBlockEntity::new, Set.of(ModBlocks.REINFORCED_BLAST_FURNACE, ModBlocks.NETHERITE_BLAST_FURNACE)));
    public static final RegistryObject<BlockEntityType<ModFurnaceBlockEntity>> MOD_FURNACE_BE =
            BLOCK_ENTITIES.register("mod_furnace", () -> new BlockEntityType<ModFurnaceBlockEntity>(
                    ModFurnaceBlockEntity::new, Set.of(ModBlocks.REINFORCED_FURNACE, ModBlocks.NETHERITE_FURNACE)));
    public static final RegistryObject<BlockEntityType<ModSmokerBlockEntity>> MOD_SMOKER_BE =
            BLOCK_ENTITIES.register("mod_smoker", () -> new BlockEntityType<ModSmokerBlockEntity>(
                    ModSmokerBlockEntity::new, Set.of(ModBlocks.REINFORCED_SMOKER, ModBlocks.NETHERITE_SMOKER)));

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
            RECIPE_SERIALIZERS.register("reinforced_bundle", () -> new RecipeSerializer<>(
                    RecordCodecBuilder.mapCodec(instance -> instance.group(
                            Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::group),
                            CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
                            ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> ((ReinforcedBundleRecipe) recipe).getRaw()),
                            ItemStack.CODEC.fieldOf("result").forGetter(ReinforcedBundleRecipe::getResultStack)
                    ).apply(instance, ReinforcedBundleRecipe::new)),
                    StreamCodec.of(
                            (buf, recipe) -> {
                                buf.writeUtf(recipe.group());
                                buf.writeEnum(recipe.category());
                                ShapedRecipePattern.STREAM_CODEC.encode(buf, recipe.getRaw());
                                ItemStack.STREAM_CODEC.encode(buf, recipe.getResultStack());
                            },
                            buf -> new ReinforcedBundleRecipe(
                                    buf.readUtf(),
                                    buf.readEnum(CraftingBookCategory.class),
                                    ShapedRecipePattern.STREAM_CODEC.decode(buf),
                                    ItemStack.STREAM_CODEC.decode(buf)
                            )
                    )
            ));

    public static final RegistryObject<CreativeModeTab> BUILDING_ITEMS_TAB =
            CREATIVE_TABS.register("building_items", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .icon(() -> new ItemStack(ModItems.IRON_CHISEL))
                    .title(Component.translatable("itemgroup.simplebuilding.building_items"))
                    .displayItems((displayContext, entries) -> ModItemGroupsContent.populate(entries, displayContext.holders()))
                    .build());

    private ForgeModRegistries() {
    }

    public static void register(BusGroup modBus) {
        MENUS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        RECIPE_TYPES.register(modBus);
        CREATIVE_TABS.register(modBus);
    }

    public static void assignStaticFields() {
        ModScreenHandlers.NETHERITE_HOPPER_SCREEN_HANDLER = NETHERITE_HOPPER_MENU.get();
        ModBlockEntities.MOD_HOPPER_BE = MOD_HOPPER_BE.get();
        ModBlockEntities.MOD_BLAST_FURNACE_BE = MOD_BLAST_FURNACE_BE.get();
        ModBlockEntities.MOD_FURNACE_BE = MOD_FURNACE_BE.get();
        ModBlockEntities.MOD_SMOKER_BE = MOD_SMOKER_BE.get();
        ModRecipes.COUNT_BASED_SMITHING_SERIALIZER = COUNT_BASED_SMITHING_SERIALIZER.get();
        ModRecipes.COUNT_BASED_SMITHING = COUNT_BASED_SMITHING.get();
        ModRecipes.UPGRADE_SMITHING_SERIALIZER = UPGRADE_SMITHING_SERIALIZER.get();
        ModRegistries.REINFORCED_BUNDLE_SERIALIZER = REINFORCED_BUNDLE_SERIALIZER.get();
        ModItemGroups.BUILDING_ITEMS_GROUP = BUILDING_ITEMS_TAB.get();
    }
}
