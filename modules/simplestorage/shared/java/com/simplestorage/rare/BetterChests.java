package com.simplestorage.rare;

import com.simplestorage.chest.ChestTier;
import com.simplestorage.chest.TieredChestBlock;
import com.simplestorage.config.StorageConfig;
import com.simplestorage.registry.StorageBlocks;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

/**
 * Better chests in structures (owner 2026-10-02, split stage 8b-3 from SimpleBuilding, docs/ai/PLAN-B-S8B3-STORAGE-2026-10-21.md):
 * a Vanilla loot chest generates with {@code betterChestPercent} (default 1 %, at most 5 %, {@link StorageConfig}) as a tiered
 * chest with double loot ({@code TieredChestBlockEntity#unpackLootTable}) - the Reinforced Chest in strongholds, the Netherite
 * Chest in bastions and nether fortresses, the Enderite Chest in end cities and end ships. The chest's loot table decides
 * ({@link #tierFor}).
 *
 * <p><b>Roll.</b> Fixed by world seed and position ({@link #rolls}): both halves of a double chest reach the same result, even
 * when they generate in separate chunk passes. Double chest (owner rule): the first half rolls; if it hits, the second rolls again
 * with the same chance; only if both hit do both become better, otherwise both stay normal chests ({@link #decides}).
 *
 * <p><b>Hooks</b> (mixins): {@code StructurePiece#createChest} (stronghold, nether fortress),
 * {@code EndCityPieces$EndCityPiece#handleDataMarker} (end city, end ship) and {@code StructureTemplate#processBlockInfos}
 * (bastion: its chests carry their table in the template NBT). Existing worlds do not change.
 */
public final class BetterChests {
    /** Salt of the roll, so that it does not run in step with other position-bound rolls (unchanged from SimpleBuilding). */
    private static final long SALT = 0x5B3C_C4E5_7A11_0D2FL;

    private BetterChests() {
    }

    /** The loot tables and their tier (a fixed list, in the wiki). */
    public static Map<ResourceKey<LootTable>, ChestTier> tables() {
        Map<ResourceKey<LootTable>, ChestTier> map = new LinkedHashMap<>();
        map.put(BuiltInLootTables.STRONGHOLD_CORRIDOR, ChestTier.REINFORCED);
        map.put(BuiltInLootTables.STRONGHOLD_CROSSING, ChestTier.REINFORCED);
        map.put(BuiltInLootTables.STRONGHOLD_LIBRARY, ChestTier.REINFORCED);
        map.put(BuiltInLootTables.BASTION_TREASURE, ChestTier.NETHERITE);
        map.put(BuiltInLootTables.BASTION_OTHER, ChestTier.NETHERITE);
        map.put(BuiltInLootTables.BASTION_BRIDGE, ChestTier.NETHERITE);
        map.put(BuiltInLootTables.BASTION_HOGLIN_STABLE, ChestTier.NETHERITE);
        map.put(BuiltInLootTables.NETHER_BRIDGE, ChestTier.NETHERITE);
        map.put(BuiltInLootTables.END_CITY_TREASURE, ChestTier.ENDERITE);
        return map;
    }

    public static @Nullable ChestTier tierFor(@Nullable ResourceKey<LootTable> table) {
        return table == null ? null : tables().get(table);
    }

    public static Block chestOf(ChestTier tier) {
        return StorageBlocks.chest(tier);
    }

    /** Switched on (super-mod switch) and a chance above 0. */
    public static boolean enabled() {
        return com.simplestorage.SimpleStorage.on() && StorageConfig.betterChestChance() > 0.0;
    }

    /** The roll of one chest half: fixed for world seed and position. */
    public static boolean rolls(long worldSeed, BlockPos pos, double chance) {
        if (chance <= 0.0) {
            return false;
        }
        return RandomSource.create(worldSeed ^ (pos.asLong() * 0x9E37_79B9_7F4A_7C15L) ^ SALT).nextDouble() < chance;
    }

    /**
     * Whether the chest {@code state} at {@code pos} becomes better: alone its own roll, as a double chest both rolls (the first
     * half hits and the second hits anew) - the same result for both halves.
     */
    public static boolean decides(long worldSeed, BlockPos pos, BlockState state, double chance) {
        Optional<BlockPos> partner = state.hasProperty(net.minecraft.world.level.block.ChestBlock.TYPE) ? TieredChestBlock.partner(state, pos) : Optional.empty();
        if (partner.isEmpty()) {
            return rolls(worldSeed, pos, chance);
        }
        BlockPos first = pos.asLong() < partner.get().asLong() ? pos : partner.get();
        BlockPos second = first == pos ? partner.get() : pos;
        return rolls(worldSeed, first, chance) && rolls(worldSeed, second, chance);
    }

    /**
     * After world generation placed a loot chest ({@code createChest}, end city marker): a Vanilla chest with a table of
     * {@link #tables} whose roll hits becomes the tiered chest with the same table and seed. True when it was replaced.
     */
    public static boolean upgradePlaced(ServerLevelAccessor level, BlockPos pos) {
        return enabled() && upgradePlaced(level, pos, StorageConfig.betterChestChance());
    }

    /** {@link #upgradePlaced(ServerLevelAccessor, BlockPos)} with an explicit chance (tests). */
    public static boolean upgradePlaced(ServerLevelAccessor level, BlockPos pos, double chance) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.CHEST) || !(level.getBlockEntity(pos) instanceof RandomizableContainer container)) {
            return false;
        }
        ResourceKey<LootTable> table = container.getLootTable();
        ChestTier tier = tierFor(table);
        if (tier == null || !decides(level.getLevel().getSeed(), pos, state, chance)) {
            return false;
        }
        long seed = container.getLootTableSeed();
        level.setBlock(pos, chestOf(tier).withPropertiesOf(state), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(pos) instanceof RandomizableContainer upgraded) {
            upgraded.setLootTable(table, seed);
        }
        return true;
    }

    /**
     * Template chests (bastion): of the processed blocks of a template, Vanilla chests with a matching table in their NBT become
     * the tiered chest (the state is swapped here before mirroring/rotation, the double chest check takes the rotated position).
     * Returns the same list when nothing changes.
     */
    public static List<StructureTemplate.StructureBlockInfo> upgradeTemplate(ServerLevelAccessor level, StructurePlaceSettings settings,
                                                                             List<StructureTemplate.StructureBlockInfo> infos) {
        if (!enabled() || infos.isEmpty()) {
            return infos;
        }
        List<StructureTemplate.StructureBlockInfo> out = null;
        double chance = StorageConfig.betterChestChance();
        long worldSeed = level.getLevel().getSeed();
        for (int i = 0; i < infos.size(); i++) {
            StructureTemplate.StructureBlockInfo info = infos.get(i);
            if (!info.state().is(Blocks.CHEST) || info.nbt() == null) {
                continue;
            }
            ChestTier tier = tierFor(lootTableOf(info.nbt()));
            BlockState placed = info.state().mirror(settings.getMirror()).rotate(settings.getRotation());
            if (tier == null || !decides(worldSeed, info.pos(), placed, chance)) {
                continue;
            }
            if (out == null) {
                out = new ArrayList<>(infos);
            }
            out.set(i, new StructureTemplate.StructureBlockInfo(info.pos(), chestOf(tier).withPropertiesOf(info.state()), info.nbt()));
        }
        return out == null ? infos : out;
    }

    /** The loot table out of the template NBT ({@code LootTable}), or null. */
    public static @Nullable ResourceKey<LootTable> lootTableOf(CompoundTag nbt) {
        Optional<String> id = nbt.getString(RandomizableContainer.LOOT_TABLE_TAG);
        if (id.isEmpty()) {
            return null;
        }
        Identifier parsed = Identifier.tryParse(id.get());
        return parsed == null ? null : ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, parsed);
    }
}
