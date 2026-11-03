package com.simplemachines.hopper;

import com.simplemachines.SimpleMachines;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The mod hoppers and tiered hopper carts of the Simple Machines tab (MACH32-MOD stage 3, docs/ai/PLAN-B-MACHINES03-2026-11-03.md):
 * Reinforced, Netherite and Enderite Hopper (2/4/8 times a Vanilla hopper's speed, item filter) and their carts. Until 03.11.
 * SimpleBuilding's ({@code simplebuilding:*}, block entity {@code simplebuilding:mod_hopper}; the ids move,
 * {@link SimpleMachines#registerIdMoves}); numbers and properties unchanged. One menu type per tier (the tier needs no opening data).
 */
public final class Hoppers {
    public static final Map<HopperTier, Block> BLOCKS = new EnumMap<>(HopperTier.class);
    public static final Map<HopperTier, Item> ITEMS = new EnumMap<>(HopperTier.class);
    public static final Map<HopperTier, EntityType<TierHopperMinecart>> CARTS = new EnumMap<>(HopperTier.class);
    public static final Map<HopperTier, Item> CART_ITEMS = new EnumMap<>(HopperTier.class);
    public static final Map<HopperTier, MenuType<TierHopperMenu>> MENUS = new EnumMap<>(HopperTier.class);
    public static BlockEntityType<TierHopperBlockEntity> BLOCK_ENTITY;
    /** SimpleBuilding's block entity id the hoppers were saved under until 03.11. */
    public static final String OLD_BLOCK_ENTITY = "mod_hopper";
    public static final String BLOCK_ENTITY_ID = "hopper";
    /** The six recipes and the folder of their recipe advancements ({@code recipes/<folder>/<path>}). */
    public static final Map<String, String> RECIPES = Map.of(
            "reinforced_hopper_from_crafting", "redstone",
            "reinforced_hopper_minecart", "transportation",
            "netherite_hopper_minecart", "transportation",
            "netherite_hopper_minecart_smithing", "transportation",
            "enderite_hopper_minecart", "transportation",
            "enderite_hopper_minecart_smithing", "transportation");

    private Hoppers() {}

    /** Vanilla hopper properties with the tier's strength and sound (as SimpleBuilding registered them). */
    public static void registerBlocks() {
        BLOCKS.put(HopperTier.REINFORCED, block(HopperTier.REINFORCED, p -> p.strength(3.0F, 4.8F).noOcclusion().sound(SoundType.METAL)));
        BLOCKS.put(HopperTier.NETHERITE, block(HopperTier.NETHERITE, p -> p.strength(5.0F, 1200.0F).noOcclusion().sound(SoundType.NETHERITE_BLOCK)));
        BLOCKS.put(HopperTier.ENDERITE, block(HopperTier.ENDERITE, p -> p.strength(6.0F, 1500.0F).noOcclusion().sound(SoundType.NETHERITE_BLOCK)));
    }

    public static void registerItems() {
        for (HopperTier tier : HopperTier.values()) {
            ITEMS.put(tier, item(tier.hopperPath(), tier, p -> new HopperItem(BLOCKS.get(tier), tier, p)));
            CART_ITEMS.put(tier, item(tier.cartPath(), tier, p -> new HopperCartItem(p.stacksTo(1), () -> CARTS.get(tier))));
        }
    }

    public static void registerBlockEntity(BlockEntityType<TierHopperBlockEntity> type) {
        BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleMachines.id(BLOCK_ENTITY_ID), type);
    }

    /** Vanilla's cart size (0.98 x 0.7, seat height, tracking range 8). */
    public static void registerEntityTypes() {
        for (HopperTier tier : HopperTier.values()) {
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, SimpleMachines.id(tier.cartPath()));
            EntityType<TierHopperMinecart> type = EntityType.Builder.<TierHopperMinecart>of((t, level) -> new TierHopperMinecart(t, level, tier), MobCategory.MISC)
                    .noLootTable().sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8).build(key);
            CARTS.put(tier, Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type));
        }
    }

    public static void registerMenus() {
        for (HopperTier tier : HopperTier.values()) {
            MENUS.put(tier, Registry.register(BuiltInRegistries.MENU, SimpleMachines.id(tier.hopperPath()),
                    new MenuType<>((id, inventory) -> new TierHopperMenu(tier, id, inventory), FeatureFlags.VANILLA_SET)));
        }
    }

    public static Block[] blocks() {
        return BLOCKS.values().toArray(Block[]::new);
    }

    public static Block block(HopperTier tier) {
        return BLOCKS.get(tier);
    }

    public static Item item(HopperTier tier) {
        return ITEMS.get(tier);
    }

    public static Item cartItem(HopperTier tier) {
        return CART_ITEMS.get(tier);
    }

    /** The three hoppers in tab order. */
    public static List<ItemStack> stacks() {
        List<ItemStack> out = new ArrayList<>();
        for (HopperTier tier : HopperTier.values()) out.add(new ItemStack(ITEMS.get(tier)));
        return out;
    }

    /** The three hopper carts in tab order. */
    public static List<ItemStack> cartStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (HopperTier tier : HopperTier.values()) out.add(new ItemStack(CART_ITEMS.get(tier)));
        return out;
    }

    private static Block block(HopperTier tier, Function<BlockBehaviour.Properties, BlockBehaviour.Properties> properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleMachines.id(tier.hopperPath()));
        return Registry.register(BuiltInRegistries.BLOCK, key, new TierHopperBlock(tier, properties.apply(BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER).setId(key))));
    }

    /** Netherite: fire resistant, uncommon; Enderite: fire resistant, epic (as in SimpleBuilding). */
    private static Item item(String path, HopperTier tier, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleMachines.id(path));
        Item.Properties properties = new Item.Properties().setId(key);
        if (tier == HopperTier.NETHERITE) properties = properties.fireResistant().rarity(Rarity.UNCOMMON);
        if (tier == HopperTier.ENDERITE) properties = properties.fireResistant().rarity(Rarity.EPIC);
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties));
    }
}
