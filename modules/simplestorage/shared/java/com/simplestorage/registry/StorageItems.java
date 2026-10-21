package com.simplestorage.registry;

import com.simplestorage.SimpleStorage;
import com.simplestorage.crate.CrateWood;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

/** Block items (crates are furnace fuel like planks, 300 ticks, not the Nether woods) and the guide book. */
public final class StorageItems {
    public static final int CRATE_FUEL = 300;
    public static final Map<CrateWood, Item> CRATES = new EnumMap<>(CrateWood.class);
    public static Item STORAGE_CRAFTING_TABLE;
    public static Item REINFORCED_CHEST, NETHERITE_CHEST, ENDERITE_CHEST;
    public static Item REINFORCED_TRAPPED_CHEST, NETHERITE_TRAPPED_CHEST, ENDERITE_TRAPPED_CHEST;
    /** The eight trapped copper chests in {@link StorageBlocks#trappedCopperChests} order. */
    public static final java.util.List<Item> TRAPPED_COPPER_CHESTS = new java.util.ArrayList<>();
    public static Item REINFORCED_SHULKER_BOX, NETHERITE_SHULKER_BOX, ENDERITE_SHULKER_BOX;
    /** Split stage 8b-2: the chest carts and chest boats per tier (one per slot like Vanilla's, the boat's wood is a component). */
    public static final Map<com.simplestorage.chest.ChestTier, Item> CHEST_MINECARTS = new EnumMap<>(com.simplestorage.chest.ChestTier.class);
    public static final Map<com.simplestorage.chest.ChestTier, Item> CHEST_BOATS = new EnumMap<>(com.simplestorage.chest.ChestTier.class);
    /** Split stage 8c-1: the three bundles and four quivers (same rarities, Netherite and Enderite fire-proof). */
    public static Item REINFORCED_BUNDLE, NETHERITE_BUNDLE, ENDERITE_BUNDLE;
    public static Item QUIVER, REINFORCED_QUIVER, NETHERITE_QUIVER, ENDERITE_QUIVER;
    /** Split stage 8c-2: the four backpacks in tier order (worn in the chest slot, placeable as a block). */
    public static final Map<com.simplestorage.backpack.BackpackTier, Item> BACKPACKS = new EnumMap<>(com.simplestorage.backpack.BackpackTier.class);

    public static void register() {
        StorageBlocks.CRATES.forEach((wood, crate) -> CRATES.put(wood, blockItem(crate, wood.nether() ? 0 : CRATE_FUEL)));
        STORAGE_CRAFTING_TABLE = blockItem(StorageBlocks.STORAGE_CRAFTING_TABLE, 0);
        // Split stage 8b, same rarities as in SimpleBuilding: Netherite uncommon, Enderite epic, both fire-proof.
        REINFORCED_CHEST = chest(StorageBlocks.REINFORCED_CHEST, p -> p);
        NETHERITE_CHEST = chest(StorageBlocks.NETHERITE_CHEST, StorageItems::netherite);
        ENDERITE_CHEST = chest(StorageBlocks.ENDERITE_CHEST, StorageItems::enderite);
        REINFORCED_TRAPPED_CHEST = chest(StorageBlocks.REINFORCED_TRAPPED_CHEST, p -> p);
        NETHERITE_TRAPPED_CHEST = chest(StorageBlocks.NETHERITE_TRAPPED_CHEST, StorageItems::netherite);
        ENDERITE_TRAPPED_CHEST = chest(StorageBlocks.ENDERITE_TRAPPED_CHEST, StorageItems::enderite);
        for (Block chest : StorageBlocks.trappedCopperChests()) TRAPPED_COPPER_CHESTS.add(blockItem(chest, 0));
        REINFORCED_SHULKER_BOX = shulker(StorageBlocks.REINFORCED_SHULKER_BOX, p -> p);
        NETHERITE_SHULKER_BOX = shulker(StorageBlocks.NETHERITE_SHULKER_BOX, StorageItems::netherite);
        ENDERITE_SHULKER_BOX = shulker(StorageBlocks.ENDERITE_SHULKER_BOX, StorageItems::enderite);
        // The entity types come later (items first on NeoForge/Forge): suppliers.
        for (com.simplestorage.chest.ChestTier tier : com.simplestorage.chest.ChestTier.values()) {
            java.util.function.UnaryOperator<Item.Properties> extra = switch (tier) {
                case REINFORCED -> p -> p;
                case NETHERITE -> StorageItems::netherite;
                case ENDERITE -> StorageItems::enderite;
            };
            CHEST_MINECARTS.put(tier, item(tier.textureName() + "_chest_minecart", extra, p -> new com.simplestorage.vehicle.ChestMinecartItem(p.stacksTo(1),
                    () -> StorageEntities.CHEST_MINECARTS.get(tier))));
            CHEST_BOATS.put(tier, item(tier.textureName() + "_chest_boat", extra, p -> new com.simplestorage.vehicle.TieredChestBoatItem(p.stacksTo(1),
                    () -> StorageEntities.CHEST_BOATS.get(tier))));
        }
        registerBundles();
        registerBackpacks();
        com.simplestorage.guide.StorageGuide.register();
    }

    /** Split stage 8c-1: bundles and quivers (call inside {@link #register}, after the chests). */
    private static void registerBundles() {
        REINFORCED_BUNDLE = item("reinforced_bundle", p -> p.stacksTo(1), com.simplestorage.bundle.ReinforcedBundleItem::new);
        NETHERITE_BUNDLE = item("netherite_bundle", p -> netherite(p.stacksTo(1)), com.simplestorage.bundle.ReinforcedBundleItem::new);
        ENDERITE_BUNDLE = item("enderite_bundle", p -> enderite(p.stacksTo(1)), com.simplestorage.bundle.ReinforcedBundleItem::new);
        QUIVER = item("quiver", p -> p.stacksTo(1).component(DataComponents.EQUIPPABLE, quiverChestSlot()), com.simplestorage.bundle.QuiverItem::new);
        // Between quiver and Netherite quiver: neither fire- nor explosion-proof, common rarity (docs/RARITAETEN.md).
        REINFORCED_QUIVER = item("reinforced_quiver", p -> p.stacksTo(1).component(DataComponents.EQUIPPABLE, quiverChestSlot()), com.simplestorage.bundle.QuiverItem::new);
        NETHERITE_QUIVER = item("netherite_quiver", p -> netherite(p.stacksTo(1)).component(DataComponents.EQUIPPABLE, quiverChestSlot()), com.simplestorage.bundle.QuiverItem::new);
        ENDERITE_QUIVER = item("enderite_quiver", p -> enderite(p.stacksTo(1)).component(DataComponents.EQUIPPABLE, quiverChestSlot()), com.simplestorage.bundle.QuiverItem::new);
    }

    /**
     * Split stage 8c-2 (as SimpleBuilding's {@code ModItems#backpack}): one per slot, worn in the chest slot (right click
     * swaps like a chestplate, players only, no damage on hurt), armour 1/2/3/4 per tier; Netherite uncommon and Enderite epic,
     * both fire-proof. No default contents component: an empty backpack has none, so the stack stays canonical.
     */
    private static void registerBackpacks() {
        for (com.simplestorage.backpack.BackpackTier tier : com.simplestorage.backpack.BackpackTier.values()) {
            String path = StorageBlocks.BACKPACK_IDS.get(tier.ordinal());
            boolean metal = tier.ordinal() >= com.simplestorage.backpack.BackpackTier.NETHERITE.ordinal();
            java.util.function.UnaryOperator<Item.Properties> extra = switch (tier) {
                case NETHERITE -> StorageItems::netherite;
                case ENDERITE -> StorageItems::enderite;
                default -> p -> p;
            };
            var sound = metal ? net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_NETHERITE : net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER;
            Block block = StorageBlocks.BACKPACKS.get(tier);
            BACKPACKS.put(tier, item(path, p -> extra.apply(p).stacksTo(1)
                    .component(DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable.builder(net.minecraft.world.entity.EquipmentSlot.CHEST)
                            .setEquipSound(sound)
                            .setAllowedEntities(net.minecraft.world.entity.EntityTypes.PLAYER)
                            .setDamageOnHurt(false)
                            .setSwappable(true)
                            .build())
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(SimpleStorage.id("armor.backpack"),
                                            tier.armor(), net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.CHEST)
                            .build()), p -> new com.simplestorage.backpack.BackpackItem(tier, block, p)));
        }
    }

    /** The four backpacks in tier order. */
    public static java.util.List<Item> backpacks() {
        return java.util.List.copyOf(BACKPACKS.values());
    }

    /** The bundles, then the quivers, in tier order. */
    public static java.util.List<Item> bundles() {
        return java.util.List.of(REINFORCED_BUNDLE, NETHERITE_BUNDLE, ENDERITE_BUNDLE, QUIVER, REINFORCED_QUIVER, NETHERITE_QUIVER, ENDERITE_QUIVER);
    }

    /**
     * The chest slot of the four quivers: a carrier, not armour (as in SimpleBuilding's {@code ModItems#quiverChestSlot}): no
     * asset (nothing drawn), no attributes, {@code damageOnHurt} off, not {@code swappable} ({@code QuiverItem#use} passes, a
     * right click would throw the arrows out), players only (a zombie must not pick up a dropped quiver and wear it).
     */
    private static net.minecraft.world.item.equipment.Equippable quiverChestSlot() {
        return net.minecraft.world.item.equipment.Equippable.builder(net.minecraft.world.entity.EquipmentSlot.CHEST)
                .setAllowedEntities(net.minecraft.world.entity.EntityTypes.PLAYER)
                .setDamageOnHurt(false)
                .setSwappable(false)
                .build();
    }

    private static Item.Properties netherite(Item.Properties p) {
        return p.fireResistant().rarity(net.minecraft.world.item.Rarity.UNCOMMON);
    }

    private static Item.Properties enderite(Item.Properties p) {
        return p.fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC);
    }

    private static Item chest(Block block, java.util.function.UnaryOperator<Item.Properties> extra) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
        return Registry.register(BuiltInRegistries.ITEM, key, new com.simplestorage.chest.TieredChestItem(block, extra.apply(new Item.Properties().setId(key))));
    }

    /** One per slot like Vanilla's box, with an empty {@code minecraft:container} so that it counts as a container item. */
    private static Item shulker(Block block, java.util.function.UnaryOperator<Item.Properties> extra) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
        Item.Properties p = extra.apply(new Item.Properties().setId(key)).stacksTo(1)
                .component(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY);
        return Registry.register(BuiltInRegistries.ITEM, key, new com.simplestorage.chest.TieredShulkerBoxItem(block, p));
    }

    private static Item item(String path, java.util.function.UnaryOperator<Item.Properties> extra, java.util.function.Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleStorage.id(path));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(extra.apply(new Item.Properties().setId(key))));
    }

    private static Item blockItem(Block block, int fuel) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
        Item.Properties properties = new Item.Properties().setId(key);
        if (fuel > 0) {
            properties = properties.component(DataComponents.COOKING_FUEL,
                    new CookingFuel(new ResolvableInt.Constant(fuel), new ResolvableFloat.Constant(1.0F)));
        }
        return Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, properties));
    }

    private StorageItems() {}
}
