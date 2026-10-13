package com.simpletrims;

import com.mojang.serialization.Codec;
import com.simpletrims.placed.PlacedTrimTemplateBlock;
import com.simpletrims.placed.PlacedTrimTemplateBlockEntity;
import com.simpletrims.recipe.ToolShapelessRecipe;
import com.simpletrims.recipe.TrimUpgradeRecipe;
import java.util.List;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Everything Simple Trims registers. Loaders call the methods inside their matching registry event (Fabric: directly
 * in the initializer), in this order: components, blocks, items, block entity type, recipe serializers.
 */
public final class TrimsRegistry {
    // Components (moved from simplebuilding:*, see SimpleTrims#MOVED_COMPONENTS). All synced: the client draws them.
    public static final DataComponentType<Integer> GLOW_LEVEL = DataComponentType.<Integer>builder()
            .persistent(Codec.INT.xmap(TrimGlow::normalizeGlowLevel, level -> level)).networkSynchronized(ByteBufCodecs.VAR_INT).build();
    /** Legacy boolean Glowing of very old stacks (read as Glowing, removed when upgraded). */
    public static final DataComponentType<Boolean> VISUAL_GLOW = DataComponentType.<Boolean>builder()
            .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build();
    /** Legacy marker of older Emitting upgrades; kept so such stacks still load (nothing reads it). */
    public static final DataComponentType<Boolean> LIGHT_SOURCE = DataComponentType.<Boolean>builder()
            .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build();
    public static final DataComponentType<Boolean> PULSATING = DataComponentType.<Boolean>builder()
            .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build();

    public static Item GLOWING_TRIM_TEMPLATE, EMITTING_TRIM_TEMPLATE, PULSATING_TRIM_TEMPLATE;
    public static PlacedTrimTemplateBlock PLACED_TEMPLATE;
    public static BlockEntityType<PlacedTrimTemplateBlockEntity> PLACED_TEMPLATE_BE;

    public static void registerComponents() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleTrims.id("glow_level"), GLOW_LEVEL);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleTrims.id("visual_glow"), VISUAL_GLOW);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleTrims.id("light_source"), LIGHT_SOURCE);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleTrims.id("pulsating"), PULSATING);
    }

    public static void registerBlocks() {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleTrims.id("placed_template"));
        PLACED_TEMPLATE = Registry.register(BuiltInRegistries.BLOCK, key, new PlacedTrimTemplateBlock(BlockBehaviour.Properties.of()
                .strength(0.5F).sound(SoundType.METAL).noCollision().noLootTable().mapColor(MapColor.NONE)
                .pushReaction(PushReaction.POPPED).setId(key)));
    }

    public static void registerItems() {
        List<Identifier> armor = List.of(Identifier.withDefaultNamespace("container/slot/helmet"), Identifier.withDefaultNamespace("container/slot/chestplate"),
                Identifier.withDefaultNamespace("container/slot/leggings"), Identifier.withDefaultNamespace("container/slot/boots"));
        List<Identifier> glowMaterials = List.of(Identifier.withDefaultNamespace("container/slot/ingot"),
                Identifier.withDefaultNamespace("container/slot/lapis_lazuli"), Identifier.withDefaultNamespace("container/slot/redstone_dust"));
        GLOWING_TRIM_TEMPLATE = template("glowing_trim_template", ChatFormatting.GRAY, armor, glowMaterials);
        EMITTING_TRIM_TEMPLATE = template("emitting_trim_template", ChatFormatting.GOLD, armor, glowMaterials);
        PULSATING_TRIM_TEMPLATE = template("pulsating_trim_template", ChatFormatting.DARK_AQUA, armor,
                List.of(Identifier.withDefaultNamespace("container/slot/amethyst_shard")));
        com.simpletrims.guide.TrimsGuide.register();
    }

    public static void registerBlockEntity(BlockEntityType<PlacedTrimTemplateBlockEntity> type) {
        PLACED_TEMPLATE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleTrims.id("placed_template"), type);
    }

    public static void registerRecipeSerializers() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, SimpleTrims.id("trim_upgrade"), TrimUpgradeRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, SimpleTrims.id("tool_shapeless"), ToolShapelessRecipe.SERIALIZER);
    }

    /** The three templates, for the creative tab (empty while the module is switched off). */
    public static List<ItemStack> tabStacks() {
        if (!SimpleTrims.on()) return List.of();
        return List.of(new ItemStack(GLOWING_TRIM_TEMPLATE), new ItemStack(EMITTING_TRIM_TEMPLATE), new ItemStack(PULSATING_TRIM_TEMPLATE));
    }

    private static Item template(String name, ChatFormatting appliesTo, List<Identifier> baseIcons, List<Identifier> additionIcons) {
        String prefix = "item.simpletrims." + name;
        return register(name, p -> new SmithingTemplateItem(
                Component.translatable(prefix + ".applies_to").withStyle(appliesTo),
                Component.translatable(prefix + ".ingredients").withStyle(ChatFormatting.GRAY),
                Component.translatable(prefix + ".base_slot_description").withStyle(ChatFormatting.GRAY),
                Component.translatable(prefix + ".additions_slot_description").withStyle(ChatFormatting.GRAY),
                baseIcons, additionIcons, p.stacksTo(64).rarity(Rarity.RARE)));
    }

    private static Item register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleTrims.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }

    private TrimsRegistry() {}
}
