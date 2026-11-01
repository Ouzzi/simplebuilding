package com.simplebuilding.modules.simplemobs;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Spawner Fragment, Spawner Chunk and the Makeshift Spawner (WF25-6, plan docs/ai/PLAN-B-SPAWN01-2026-11-01.md).
 *
 * <p>Prinzip A12: use in Vanilla play - a Vanilla spawner drops nothing today, so its room is a dead end; a pickaxe now yields a
 * Fragment with {@link MobsConfig#spawnerFragmentChance} (default 30 %), 9 Fragments = 1 Chunk, 9 Chunks = 1 Makeshift Spawner.
 * Change to Vanilla: small - the Vanilla spawner, its mobs and the Silk Touch rule stay; only an extra item may drop. The
 * Makeshift Spawner is empty until a spawn egg is used on it (the Vanilla way) and works {@link MobsConfig#makeshiftSpawnerSlowdown}
 * times slower than a Vanilla one, so it is a stand-in, not a better spawner. Every part is off when Simple Mobs is switched off.
 */
public final class SpawnerParts {
    public static final String ID = MobsRegistry.ID;

    public static final ResourceKey<Item> FRAGMENT_KEY = itemKey("spawner_fragment");
    public static final ResourceKey<Item> CHUNK_KEY = itemKey("spawner_chunk");
    public static final ResourceKey<Block> BLOCK_KEY = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ID, "makeshift_spawner"));
    public static final ResourceKey<Item> BLOCK_ITEM_KEY = itemKey("makeshift_spawner");

    public static final Item FRAGMENT = new Item(new Item.Properties().setId(FRAGMENT_KEY));
    public static final Item CHUNK = new Item(new Item.Properties().rarity(Rarity.UNCOMMON).setId(CHUNK_KEY));
    public static final Block MAKESHIFT_SPAWNER = new MakeshiftSpawnerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
            .requiresCorrectToolForDrops().strength(4.0F).sound(SoundType.SPAWNER).noOcclusion().setId(BLOCK_KEY));
    public static final Item MAKESHIFT_SPAWNER_ITEM = new BlockItem(MAKESHIFT_SPAWNER,
            new Item.Properties().rarity(Rarity.UNCOMMON).useBlockDescriptionPrefix().setId(BLOCK_ITEM_KEY));

    private SpawnerParts() {}

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, path));
    }

    public static void blocks(BiConsumer<ResourceKey<Block>, Block> sink) {
        sink.accept(BLOCK_KEY, MAKESHIFT_SPAWNER);
    }

    public static void items(BiConsumer<ResourceKey<Item>, Item> sink) {
        sink.accept(FRAGMENT_KEY, FRAGMENT);
        sink.accept(CHUNK_KEY, CHUNK);
        sink.accept(BLOCK_ITEM_KEY, MAKESHIFT_SPAWNER_ITEM);
    }

    /** A pickaxe without Silk Touch: the tool that chips a Fragment off (Silk Touch lifts nothing from a Vanilla spawner, so it gets nothing here either). */
    public static boolean chips(ItemStack tool) {
        return tool.is(ItemTags.PICKAXES) && tool.getEnchantments().keySet().stream().noneMatch(e -> e.is(Enchantments.SILK_TOUCH));
    }

    /** The fragment roll, pure so a test can drive it with a seeded random: tool must chip, then {@code chance} (0 = never, 1 = always). */
    public static boolean roll(RandomSource random, ItemStack tool, float chance) {
        return chance > 0.0f && chips(tool) && random.nextFloat() < chance;
    }

    /** Called from the spawner-break mixin after a Vanilla spawner was mined (not in Creative, not by explosions: those have no pickaxe). */
    public static void onVanillaSpawnerBroken(ServerLevel level, BlockPos pos, ItemStack tool) {
        if (MobsRegistry.on() && roll(level.getRandom(), tool, MobsConfig.spawnerFragmentChance)) {
            Block.popResource(level, pos, new ItemStack(FRAGMENT));
        }
    }
}
