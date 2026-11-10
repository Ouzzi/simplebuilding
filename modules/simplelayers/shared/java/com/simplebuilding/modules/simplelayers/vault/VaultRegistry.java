package com.simplebuilding.modules.simplelayers.vault;

import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.vault.block.AscentAltarBlock;
import com.simplebuilding.modules.simplelayers.vault.block.AscentStreamBlock;
import com.simplebuilding.modules.simplelayers.vault.block.ChimeCrystalBlock;
import com.simplebuilding.modules.simplelayers.vault.block.PathTileBlock;
import com.simplebuilding.modules.simplelayers.vault.block.SealPart;
import com.simplebuilding.modules.simplelayers.vault.block.SealPedestalBlock;
import com.simplebuilding.modules.simplelayers.vault.block.SealSocketBlock;
import com.simplebuilding.modules.simplelayers.vault.block.VaultGateBlock;
import com.simplebuilding.modules.simplelayers.vault.block.VaultLampBlock;
import com.simplebuilding.modules.simplelayers.vault.block.VaultLeverBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Everything the Bedrock Vault registers (UNDER21, plan docs/ai/PLAN-B-UNDER21-2026-11-10.md), called from {@code LayersRegistry}
 * / the loader entry: the riddle blocks (all generated, unbreakable, no items), the three seal parts, the effect, the chunk
 * generator and the two features. The dimension itself is data ({@code simplelayers:bedrock_vault}).
 */
public final class VaultRegistry {
    public static final ResourceKey<Level> VAULT = ResourceKey.create(Registries.DIMENSION, SimpleLayers.id("bedrock_vault"));
    /** Bedrock and the frieze/marker blocks: held in the vault while the player carries the Vault-bound effect. */
    public static final TagKey<Block> VAULT_HELD = TagKey.create(Registries.BLOCK, SimpleLayers.id("vault_held"));
    public static final List<String> ITEM_PATHS = List.of("seal_fragment_light", "seal_fragment_sound", "seal_fragment_path");

    public static Block VAULT_GATE, VAULT_LAMP, VAULT_LEVER, CHIME_CRYSTAL, PATH_TILE, SEAL_PEDESTAL, SEAL_SOCKET, ASCENT_ALTAR, ASCENT_SEAL, ASCENT_STREAM;
    public static final Item[] FRAGMENTS = new Item[SealPart.values().length];
    public static List<Item> ITEMS = List.of();

    private VaultRegistry() {}

    public static boolean on() {
        return SimpleLayers.on() && VaultConfig.enabled;
    }

    public static boolean inVault(Level level) {
        return level.dimension() == VAULT;
    }

    public static void registerBlocks() {
        VAULT_GATE = block("vault_gate", VaultGateBlock::new, fixed(MapColor.COLOR_PURPLE, SoundType.DEEPSLATE).lightLevel(VaultGateBlock::light).noOcclusion());
        VAULT_LAMP = block("vault_lamp", VaultLampBlock::new, fixed(MapColor.COLOR_ORANGE, SoundType.GLASS).lightLevel(VaultLampBlock::light));
        VAULT_LEVER = block("vault_lever", VaultLeverBlock::new, fixed(MapColor.NONE, SoundType.STONE).noCollision());
        CHIME_CRYSTAL = block("chime_crystal", ChimeCrystalBlock::new, fixed(MapColor.COLOR_PURPLE, SoundType.AMETHYST).lightLevel(s -> 6));
        PATH_TILE = block("path_tile", PathTileBlock::new, fixed(MapColor.DEEPSLATE, SoundType.POLISHED_DEEPSLATE).lightLevel(PathTileBlock::light));
        SEAL_PEDESTAL = block("seal_pedestal", SealPedestalBlock::new, fixed(MapColor.TERRACOTTA_CYAN, SoundType.TUFF_BRICKS).lightLevel(SealPedestalBlock::light));
        SEAL_SOCKET = block("seal_socket", SealSocketBlock::new, fixed(MapColor.TERRACOTTA_CYAN, SoundType.TUFF).lightLevel(SealSocketBlock::light).noOcclusion());
        ASCENT_ALTAR = block("ascent_altar", AscentAltarBlock::new, fixed(MapColor.STONE, SoundType.VAULT).lightLevel(s -> 5));
        ASCENT_SEAL = block("ascent_seal", Block::new, fixed(MapColor.DEEPSLATE, SoundType.DEEPSLATE));
        ASCENT_STREAM = block("ascent_stream", AscentStreamBlock::new, fixed(MapColor.NONE, SoundType.WOOL).noCollision().noOcclusion().lightLevel(s -> 8));
    }

    public static void registerItems() {
        List<Item> out = new ArrayList<>();
        for (SealPart part : SealPart.values()) {
            String name = "seal_fragment_" + part.name;
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleLayers.id(name));
            FRAGMENTS[part.ordinal()] = Registry.register(BuiltInRegistries.ITEM, key,
                    new SealFragmentItem(part, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).setId(key)));
            out.add(FRAGMENTS[part.ordinal()]);
        }
        ITEMS = List.copyOf(out);
    }

    public static void registerWorldgen() {
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, SimpleLayers.id("bedrock_vault"), VaultChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE, SimpleLayers.id("vault_gap"), VaultGapFeature.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE, SimpleLayers.id("vault_cache"), VaultCacheFeature.CODEC);
    }

    /** Tab entries (after the Backrooms items): the three seal parts. */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (on()) ITEMS.forEach(i -> out.add(new ItemStack(i)));
        return out;
    }

    /** Unbreakable, unmovable, no drops (the riddle must stay solvable). */
    private static BlockBehaviour.Properties fixed(MapColor color, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(color).sound(sound).strength(-1.0F, 3600000.0F).noLootTable()
                .pushReaction(PushReaction.IMMOVEABLE).isValidSpawn((s, l, pos, type) -> false);
    }

    private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleLayers.id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }
}
