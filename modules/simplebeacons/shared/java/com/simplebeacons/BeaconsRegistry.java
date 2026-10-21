package com.simplebeacons;

import com.simplebeacons.beacon.StrongBeaconBlock;
import com.simplebeacons.beacon.StrongBeaconBlockEntity;
import com.simplebeacons.beacon.StrongBeaconMenu;
import com.simplebeacons.conduit.StrongConduitBlock;
import com.simplebeacons.conduit.StrongConduitBlockEntity;
import com.simplelib.api.SimpleRarity;
import java.util.List;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/**
 * Everything Simple Beacons registers. Loaders call the methods inside their matching registry event (Fabric:
 * directly in the initializer), in this order: blocks, items, block entity types, menu.
 */
public final class BeaconsRegistry {
    public static Block STRONG_BEACON, STRONG_CONDUIT;
    public static Item STRONG_UPGRADE_TEMPLATE, STRONG_UPGRADE_SHARD, STRONG_BEACON_ITEM, STRONG_CONDUIT_ITEM;
    public static BlockEntityType<StrongBeaconBlockEntity> STRONG_BEACON_BE;
    public static BlockEntityType<StrongConduitBlockEntity> STRONG_CONDUIT_BE;
    public static MenuType<StrongBeaconMenu> STRONG_BEACON_MENU;

    public static void registerBlocks() {
        STRONG_BEACON = block("strong_beacon", StrongBeaconBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND)
                .instrument(NoteBlockInstrument.HAT).strength(3.0F).lightLevel(state -> 15).noOcclusion().isRedstoneConductor((s, l, p) -> false));
        STRONG_CONDUIT = block("strong_conduit", StrongConduitBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND)
                .forceSolidOn().instrument(NoteBlockInstrument.HAT).strength(3.0F).lightLevel(state -> 15).noOcclusion());
    }

    public static void registerItems() {
        STRONG_UPGRADE_TEMPLATE = item("strong_upgrade_template", p -> new SmithingTemplateItem(
                Component.translatable("item.simplebeacons.strong_upgrade_template.applies_to").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.simplebeacons.strong_upgrade_template.ingredients").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.simplebeacons.strong_upgrade_template.base_slot_description"),
                Component.translatable("item.simplebeacons.strong_upgrade_template.additions_slot_description"),
                List.of(), List.of(), p), SimpleRarity.MYTHIC.apply(new Item.Properties()));
        // Owner 21.10.: the Vanilla Wither always drops one; nine make a template. Survives explosions like the Nether Star.
        STRONG_UPGRADE_SHARD = item("strong_upgrade_shard", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON)
                .delayedComponent(DataComponents.DAMAGE_RESISTANT, registries -> new DamageResistant(registries.getOrThrow(DamageTypeTags.IS_EXPLOSION))));
        STRONG_BEACON_ITEM = item("strong_beacon", p -> new BlockItem(STRONG_BEACON, p),
                SimpleRarity.MYTHIC.apply(new Item.Properties().useBlockDescriptionPrefix()));
        STRONG_CONDUIT_ITEM = item("strong_conduit", p -> new BlockItem(STRONG_CONDUIT, p),
                SimpleRarity.MYTHIC.apply(new Item.Properties().useBlockDescriptionPrefix()));
        com.simplebeacons.guide.BeaconsGuide.register();
    }

    public static void registerBlockEntities(BlockEntityType<StrongBeaconBlockEntity> beacon, BlockEntityType<StrongConduitBlockEntity> conduit) {
        STRONG_BEACON_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleBeacons.id("strong_beacon"), beacon);
        STRONG_CONDUIT_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleBeacons.id("strong_conduit"), conduit);
    }

    public static void registerMenus() {
        STRONG_BEACON_MENU = Registry.register(BuiltInRegistries.MENU, SimpleBeacons.id("strong_beacon"),
                new MenuType<>(StrongBeaconMenu::new, FeatureFlags.VANILLA_SET));
    }

    /** Creative tab stacks after the Vanilla beacon/conduit (empty while the module is switched off). */
    public static List<ItemStack> tabStacks() {
        if (!SimpleBeacons.on()) return List.of();
        return List.of(new ItemStack(STRONG_BEACON_ITEM), new ItemStack(STRONG_CONDUIT_ITEM));
    }

    private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleBeacons.id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }

    private static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleBeacons.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
    }

    private BeaconsRegistry() {}

    /**
     * Owner 21.10.: the Vanilla Wither (only {@code minecraft:wither}, not the stronger Withers of other mods) always drops
     * one Strong Upgrade Shard - the way to the template without Simple Mobs or without beating the heavy Wither.
     */
    public static void witherDied(net.minecraft.world.entity.boss.wither.WitherBoss wither, net.minecraft.server.level.ServerLevel level) {
        if (!SimpleBeacons.on() || STRONG_UPGRADE_SHARD == null || wither.getType() != net.minecraft.world.entity.EntityTypes.WITHER) return;
        var drop = wither.spawnAtLocation(level, new ItemStack(STRONG_UPGRADE_SHARD));
        if (drop != null) drop.setExtendedLifetime();
    }

    /** The module's own creative tab (items are also sorted into the Vanilla tabs, see the loader hooks). */
    public static void registerTab() {
        net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebeacons", "beacons"),
                net.minecraft.world.item.CreativeModeTab.builder(net.minecraft.world.item.CreativeModeTab.Row.TOP, 0)
                        .title(net.minecraft.network.chat.Component.translatable("itemgroup.simplebeacons.beacons"))
                        .icon(() -> new net.minecraft.world.item.ItemStack(STRONG_BEACON_ITEM))
                        .displayItems((params, out) -> {
                            if (!SimpleBeacons.on()) return;
                            out.accept(com.simplebeacons.guide.BeaconsGuide.stack());
                            tabStacks().forEach(out::accept);
                            out.accept(new net.minecraft.world.item.ItemStack(STRONG_UPGRADE_TEMPLATE));
                            out.accept(new net.minecraft.world.item.ItemStack(STRONG_UPGRADE_SHARD));
                        }).build());
    }
}

