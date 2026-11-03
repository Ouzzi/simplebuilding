package com.simpleovens;

import com.simpleovens.block.OvenItem;
import com.simpleovens.block.TierBlastFurnaceBlock;
import com.simpleovens.block.TierBlastFurnaceBlockEntity;
import com.simpleovens.block.TierFurnaceBlock;
import com.simpleovens.block.TierFurnaceBlockEntity;
import com.simpleovens.block.TierSmokerBlock;
import com.simpleovens.block.TierSmokerBlockEntity;
import com.simpleovens.boat.OvenBoat;
import com.simpleovens.cart.FurnaceMinecartItem;
import com.simpleovens.cart.TieredFurnaceMinecart;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
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
import net.minecraft.world.item.BoatItem;
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
    /** BOAT40: the Vanilla wood types a furnace boat exists in (the bamboo one is a raft). */
    public static final List<String> BOAT_WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo");
    /** Keyed by {@link #boatKey}: {@code oak}, {@code reinforced_oak}, {@code netherite_bamboo}, ... (b-small04: one boat per furnace tier). */
    public static final Map<String, Item> BOAT_ITEMS = new LinkedHashMap<>();
    public static final Map<String, EntityType<OvenBoat>> BOATS = new LinkedHashMap<>();
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
        for (String key : boatKeys()) {
            ResourceKey<Item> id = ResourceKey.create(Registries.ITEM, SimpleOvens.id(boatName(key)));
            OvenTier tier = boatTier(key);
            Item.Properties props = new Item.Properties().setId(id).stacksTo(1);
            if (tier != null) props = props.rarity(tier.rarity());
            if (tier != null && tier.fireResistant()) props = props.fireResistant();
            BOAT_ITEMS.put(key, Registry.register(BuiltInRegistries.ITEM, id, new BoatItem(boatType(key), props)));
        }
        com.simpleovens.guide.OvensGuide.register();
    }

    /** The key of a boat: the wood, with the tier prefix of the furnace in front for the upper tiers ({@code null} = plain furnace). */
    public static String boatKey(OvenTier tier, String wood) {
        return tier == null ? wood : tier.prefix() + "_" + wood;
    }

    /** All boat keys: the plain furnace first, then Reinforced, Netherite, Enderite; each over every wood. */
    public static List<String> boatKeys() {
        List<String> out = new ArrayList<>();
        for (OvenTier tier : boatTiers()) for (String wood : BOAT_WOODS) out.add(boatKey(tier, wood));
        return out;
    }

    /** null (plain furnace) followed by the three furnace tiers. */
    public static List<OvenTier> boatTiers() {
        List<OvenTier> out = new ArrayList<>();
        out.add(null);
        out.addAll(List.of(OvenTier.values()));
        return out;
    }

    /** The furnace tier of a boat key; null for the plain furnace. */
    public static OvenTier boatTier(String key) {
        for (OvenTier tier : OvenTier.values()) if (key.startsWith(tier.prefix() + "_")) return tier;
        return null;
    }

    /** The wood of a boat key ({@code bamboo} is the raft). */
    public static String boatWood(String key) {
        OvenTier tier = boatTier(key);
        return tier == null ? key : key.substring(tier.prefix().length() + 1);
    }

    /** {@code oak_furnace_boat}, {@code reinforced_oak_furnace_boat}, {@code bamboo_furnace_raft}, {@code enderite_bamboo_furnace_raft}. */
    public static String boatName(String key) {
        return key + (boatWood(key).equals("bamboo") ? "_furnace_raft" : "_furnace_boat");
    }

    /** The furnace boat type of a key; built on first use (the item needs it before the types are registered). */
    public static EntityType<OvenBoat> boatType(String key) {
        return BOATS.computeIfAbsent(key, k -> EntityType.Builder.<OvenBoat>of((t, level) -> new OvenBoat(t, level, () -> BOAT_ITEMS.get(k), boatWood(k).equals("bamboo"), boatTier(k)), MobCategory.MISC)
                .noLootTable().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, SimpleOvens.id(boatName(k)))));
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
        for (String key : boatKeys()) Registry.register(BuiltInRegistries.ENTITY_TYPE, SimpleOvens.id(boatName(key)), boatType(key));
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
        BOAT_ITEMS.values().forEach(item -> out.add(new ItemStack(item)));
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

    public static List<ItemStack> boatStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (SimpleOvens.on()) BOAT_ITEMS.values().forEach(item -> out.add(new ItemStack(item)));
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
