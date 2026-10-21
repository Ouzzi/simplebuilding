package com.simplebuilding.modules.simplemobs;

import java.util.function.BiConsumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Blighted and Abyssal Wither (concept N26, owner N50) and their loot (owner 21.10., docs/ai/PLAN-B-WITHER21-2026-10-21.md):
 * the Nether Heart (stage 2 and 3, replaces the Wither Heart) and the Nether Gem (stage 3, for later). Both are small
 * blocks: the Wither sets them under itself when its death ends ({@link WitherLoot}), broken they give the item back.
 * The Biome Brush moved to Simple Tools.
 */
public final class WitherMobs {
    public static final String ID = MobsRegistry.ID;

    public static final ResourceKey<EntityType<?>> BLIGHTED_WITHER_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ID, "blighted_wither"));
    public static final EntityType<BlightedWither> BLIGHTED_WITHER =
            EntityType.Builder.<BlightedWither>of(BlightedWither::new, MobCategory.MONSTER)
                    .fireImmune().immuneTo(BlockTags.WITHER_IMMUNE_TO).sized(0.9f, 3.5f).clientTrackingRange(10)
                    .notInPeaceful().dontTrackDeltas()
                    .build(BLIGHTED_WITHER_KEY);
    public static final ResourceKey<EntityType<?>> ABYSSAL_WITHER_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ID, "abyssal_wither"));
    public static final EntityType<AbyssalWither> ABYSSAL_WITHER =
            EntityType.Builder.<AbyssalWither>of(AbyssalWither::new, MobCategory.MONSTER)
                    .fireImmune().immuneTo(BlockTags.WITHER_IMMUNE_TO).sized(0.9f, 3.5f).clientTrackingRange(10)
                    .notInPeaceful().dontTrackDeltas()
                    .build(ABYSSAL_WITHER_KEY);
    /** Helmets that summon the Abyssal Wither (optional Enderite Helmets of other mods; empty when none is installed). */
    public static final TagKey<Item> ABYSSAL_CROWNS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, "abyssal_wither_crowns"));

    /** Fire and explosions: the loot survives the fight's skulls and lava (damage type tag of this mod). */
    public static final TagKey<DamageType> LOOT_RESISTANT = TagKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(ID, "wither_loot_resistant"));

    public static final ResourceKey<Block> HEART_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ID, "nether_heart"));
    public static final ResourceKey<Block> GEM_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ID, "nether_gem"));
    public static final Block HEART_BLOCK = new WitherLootBlock(Block.box(4, 0, 4, 12, 8, 12), lootBlock(SoundType.WART_BLOCK, MapColor.CRIMSON_NYLIUM, 7).setId(HEART_BLOCK_KEY));
    public static final Block GEM_BLOCK = new WitherLootBlock(Block.box(3, 0, 3, 13, 11, 13), lootBlock(SoundType.AMETHYST_CLUSTER, MapColor.COLOR_RED, 5).setId(GEM_BLOCK_KEY));

    public static final ResourceKey<Item> HEART_KEY = itemKey("nether_heart");
    public static final ResourceKey<Item> GEM_KEY = itemKey("nether_gem");
    public static final Item HEART = new BlockItem(HEART_BLOCK, lootItem().setId(HEART_KEY));
    public static final Item GEM = new NetherGemItem(GEM_BLOCK, lootItem().setId(GEM_KEY));

    private WitherMobs() {}

    private static BlockBehaviour.Properties lootBlock(SoundType sound, MapColor color, int light) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(1.5F, 1200.0F).sound(sound).lightLevel(s -> light)
                .noOcclusion().pushReaction(PushReaction.DESTROY);
    }

    private static Item.Properties lootItem() {
        return new Item.Properties().rarity(Rarity.EPIC).useBlockDescriptionPrefix()
                .delayedComponent(DataComponents.DAMAGE_RESISTANT, registries -> new DamageResistant(registries.getOrThrow(LOOT_RESISTANT)));
    }

    /** Nether Gem: its use is still to come (one tooltip line says so). */
    static final class NetherGemItem extends BlockItem {
        NetherGemItem(Block block, Item.Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                    java.util.function.Consumer<net.minecraft.network.chat.Component> builder, net.minecraft.world.item.TooltipFlag flag) {
            builder.accept(net.minecraft.network.chat.Component.translatable("item.simplemobs.nether_gem.tooltip").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, path));
    }

    public static void entityTypes(BiConsumer<ResourceKey<EntityType<?>>, EntityType<?>> sink) {
        sink.accept(BLIGHTED_WITHER_KEY, BLIGHTED_WITHER);
        sink.accept(ABYSSAL_WITHER_KEY, ABYSSAL_WITHER);
    }

    public static void blocks(BiConsumer<ResourceKey<Block>, Block> sink) {
        sink.accept(HEART_BLOCK_KEY, HEART_BLOCK);
        sink.accept(GEM_BLOCK_KEY, GEM_BLOCK);
    }

    public static void items(BiConsumer<ResourceKey<Item>, Item> sink) {
        sink.accept(HEART_KEY, HEART);
        sink.accept(GEM_KEY, GEM);
    }

    /** Fabric: direct registration. */
    public static void registerAll() {
        entityTypes((k, v) -> Registry.register(BuiltInRegistries.ENTITY_TYPE, k, v));
        blocks((k, v) -> Registry.register(BuiltInRegistries.BLOCK, k, v));
        items((k, v) -> Registry.register(BuiltInRegistries.ITEM, k, v));
    }
}
