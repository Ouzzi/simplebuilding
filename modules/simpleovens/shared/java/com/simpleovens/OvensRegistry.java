package com.simpleovens;

import com.simpleovens.block.OvenItem;
import com.simpleovens.block.TierBlastFurnaceBlock;
import com.simpleovens.block.TierBlastFurnaceBlockEntity;
import com.simpleovens.block.TierFurnaceBlock;
import com.simpleovens.block.TierFurnaceBlockEntity;
import com.simpleovens.block.TierSmokerBlock;
import com.simpleovens.block.TierSmokerBlockEntity;
import com.simpleovens.cart.FurnaceMinecartItem;
import com.simpleovens.cart.TieredFurnaceMinecart;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Everything Simple Ovens registers. Loaders call the methods inside their matching registry event (Fabric: directly
 * in the initializer), in this order: blocks, items, block entity types, entity types, creative tab. Ids and numbers
 * as in SimpleBuilding before the move (hardness/resistance per tier like its pistons, built from the Vanilla machine
 * of the family: tool requirement, light while lit, map colour).
 */
public final class OvensRegistry {
    public static final Map<OvenTier, Block> FURNACES = new EnumMap<>(OvenTier.class);
    public static final Map<OvenTier, Block> SMOKERS = new EnumMap<>(OvenTier.class);
    public static final Map<OvenTier, Block> BLAST_FURNACES = new EnumMap<>(OvenTier.class);
    public static final Map<OvenTier, Item> CART_ITEMS = new EnumMap<>(OvenTier.class);
    public static final Map<OvenTier, EntityType<TieredFurnaceMinecart>> CARTS = new EnumMap<>(OvenTier.class);
    public static BlockEntityType<TierFurnaceBlockEntity> FURNACE_BE;
    public static BlockEntityType<TierSmokerBlockEntity> SMOKER_BE;
    public static BlockEntityType<TierBlastFurnaceBlockEntity> BLAST_FURNACE_BE;
    public static CreativeModeTab TAB;

    private OvensRegistry() {}

    public static void registerBlocks() {
        for (OvenTier tier : OvenTier.values()) {
            FURNACES.put(tier, block(tier.prefix() + "_furnace", Blocks.FURNACE, tier, p -> new TierFurnaceBlock(tier, p)));
            SMOKERS.put(tier, block(tier.prefix() + "_smoker", Blocks.SMOKER, tier, p -> new TierSmokerBlock(tier, p)));
            BLAST_FURNACES.put(tier, block(tier.prefix() + "_blast_furnace", Blocks.BLAST_FURNACE, tier, p -> new TierBlastFurnaceBlock(tier, p)));
        }
    }

    public static void registerItems() {
        for (OvenTier tier : OvenTier.values()) {
            for (Block block : List.of(FURNACES.get(tier), SMOKERS.get(tier), BLAST_FURNACES.get(tier))) {
                item(BuiltInRegistries.BLOCK.getKey(block).getPath(), tier, p -> new OvenItem(block, tier, p));
            }
            CART_ITEMS.put(tier, item(tier.prefix() + "_furnace_minecart", tier, p -> new FurnaceMinecartItem(p.stacksTo(1), () -> CARTS.get(tier))));
        }
        com.simpleovens.guide.OvensGuide.register();
    }

    /** The three block entity types (built by the loader: NeoForge/Forge take an extra constructor argument). */
    public static void registerBlockEntities(BlockEntityType<TierFurnaceBlockEntity> furnace, BlockEntityType<TierSmokerBlockEntity> smoker,
                                             BlockEntityType<TierBlastFurnaceBlockEntity> blast) {
        FURNACE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleOvens.id("furnace"), furnace);
        SMOKER_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleOvens.id("smoker"), smoker);
        BLAST_FURNACE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleOvens.id("blast_furnace"), blast);
    }

    public static Block[] furnaces() {
        return FURNACES.values().toArray(Block[]::new);
    }

    public static Block[] smokers() {
        return SMOKERS.values().toArray(Block[]::new);
    }

    public static Block[] blastFurnaces() {
        return BLAST_FURNACES.values().toArray(Block[]::new);
    }

    /** Furnace minecarts: Vanilla's furnace cart size and tracking range. */
    public static void registerEntityTypes() {
        for (OvenTier tier : OvenTier.values()) {
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, SimpleOvens.id(tier.prefix() + "_furnace_minecart"));
            EntityType<TieredFurnaceMinecart> type = EntityType.Builder.<TieredFurnaceMinecart>of((t, level) -> new TieredFurnaceMinecart(t, level, tier), MobCategory.MISC)
                    .noLootTable().sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8).build(key);
            CARTS.put(tier, Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type));
        }
    }

    public static void registerTab() {
        TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleOvens.id("ovens"), CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemgroup.simpleovens.ovens"))
                .icon(() -> new ItemStack(FURNACES.get(OvenTier.NETHERITE)))
                .displayItems((parameters, output) -> tabStacks().forEach(output::accept))
                .build());
    }

    /** All blocks, then the carts, then the guide (empty while the module is switched off). */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (!SimpleOvens.on()) return out;
        for (Map<OvenTier, Block> family : List.of(FURNACES, SMOKERS, BLAST_FURNACES)) {
            family.values().forEach(block -> out.add(new ItemStack(block)));
        }
        CART_ITEMS.values().forEach(item -> out.add(new ItemStack(item)));
        out.add(com.simpleovens.guide.OvensGuide.stack());
        return out;
    }

    /** The furnace blocks only, for Vanilla's functional tab. */
    public static List<ItemStack> machineStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (!SimpleOvens.on()) return out;
        for (Map<OvenTier, Block> family : List.of(FURNACES, SMOKERS, BLAST_FURNACES)) {
            family.values().forEach(block -> out.add(new ItemStack(block)));
        }
        return out;
    }

    /** One family's three tiers, for Vanilla's functional tab (behind the Vanilla machine). */
    public static List<ItemStack> familyStacks(Map<OvenTier, Block> family) {
        List<ItemStack> out = new ArrayList<>();
        if (SimpleOvens.on()) family.values().forEach(block -> out.add(new ItemStack(block)));
        return out;
    }

    public static List<ItemStack> cartStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (SimpleOvens.on()) CART_ITEMS.values().forEach(item -> out.add(new ItemStack(item)));
        return out;
    }

    private static Block block(String name, Block vanilla, OvenTier tier, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleOvens.id(name));
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(vanilla).setId(key);
        properties = switch (tier) {
            case REINFORCED -> properties.strength(3.5F).sound(SoundType.METAL);
            case NETHERITE -> properties.strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK);
            case ENDERITE -> properties.strength(6.0F, 1500.0F).sound(SoundType.NETHERITE_BLOCK);
        };
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties));
    }

    private static Item item(String name, OvenTier tier, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleOvens.id(name));
        Item.Properties properties = new Item.Properties().setId(key).rarity(tier.rarity());
        if (tier.fireResistant()) properties = properties.fireResistant();
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties));
    }
}
