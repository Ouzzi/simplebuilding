package com.simplebuilding.util;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.blocks.custom.TieredChestBlock;
import com.simplebuilding.config.ServerTuning;
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
 * Bessere Truhen in Strukturen (Besitzer 2026-10-02): eine Vanilla-Loot-Truhe entsteht mit
 * {@code server.loot.betterChestPercent} (Standard 1 %, hoechstens 5 %) als Stufen-Truhe mit doppelter Beute
 * ({@code TieredChestBlockEntity#unpackLootTable}) - in der Festung die Verstaerkte Truhe, in Bastion und Netherfestung
 * die Netherit-Truhe, in End-Stadt und End-Schiff die Enderit-Truhe. Entscheidet die Loot-Tabelle der Truhe
 * ({@link #tierFor}).
 *
 * <p><b>Wuerfel.</b> Deterministisch aus Weltseed und Position ({@link #rolls}): beide Haelften einer Doppeltruhe
 * kommen zum selben Ergebnis, auch wenn sie in getrennten Chunk-Durchlaeufen entstehen. Doppeltruhe (Besitzerregel):
 * die erste Haelfte wuerfelt, trifft sie, wuerfelt die zweite noch einmal mit derselben Chance; nur wenn beide
 * treffen, werden beide besser, sonst bleiben beide normale Truhen ({@link #decides}).
 *
 * <p><b>Haken</b> (Mixins): {@code StructurePiece#createChest} (Festung, Netherfestung),
 * {@code EndCityPieces$EndCityPiece#handleDataMarker} (End-Stadt, End-Schiff) und
 * {@code StructureTemplate#processBlockInfos} (Bastion: die Truhen tragen ihre Tabelle im Vorlagen-NBT).
 * Bestehende Welten aendern sich nicht.
 */
public final class BetterChests {
    /** Salz des Wuerfels, damit er nicht mit anderen positionsgebundenen Wuerfeln gleichlaeuft. */
    private static final long SALT = 0x5B3C_C4E5_7A11_0D2FL;

    private BetterChests() {
    }

    /** Die Loot-Tabellen und ihre Stufe (feste Liste, im Wiki nachlesbar). */
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
        return switch (tier) {
            case REINFORCED -> ModBlocks.REINFORCED_CHEST;
            case NETHERITE -> ModBlocks.NETHERITE_CHEST;
            case ENDERITE -> ModBlocks.ENDERITE_CHEST;
        };
    }

    public static boolean enabled() {
        return com.simplebuilding.version.McVersion.RARE_STRUCTURE_FINDS && ServerTuning.betterChestChance() > 0.0;
    }

    /** Der Wuerfel einer Truhenhaelfte: fest fuer Weltseed und Position. */
    public static boolean rolls(long worldSeed, BlockPos pos, double chance) {
        if (chance <= 0.0) {
            return false;
        }
        return RandomSource.create(worldSeed ^ (pos.asLong() * 0x9E37_79B9_7F4A_7C15L) ^ SALT).nextDouble() < chance;
    }

    /**
     * Ob die Truhe {@code state} bei {@code pos} besser wird: einzeln ihr eigener Wurf, als Doppeltruhe beide Wuerfe
     * (erste Haelfte trifft und die zweite trifft neu) - fuer beide Haelften dasselbe Ergebnis.
     */
    public static boolean decides(long worldSeed, BlockPos pos, BlockState state, double chance) {
        Optional<BlockPos> partner = state.hasProperty(TieredChestBlock.TYPE) ? TieredChestBlock.partner(state, pos) : Optional.empty();
        if (partner.isEmpty()) {
            return rolls(worldSeed, pos, chance);
        }
        BlockPos first = pos.asLong() < partner.get().asLong() ? pos : partner.get();
        BlockPos second = first == pos ? partner.get() : pos;
        return rolls(worldSeed, first, chance) && rolls(worldSeed, second, chance);
    }

    /**
     * Nach dem Setzen einer Loot-Truhe in der Weltgenerierung ({@code createChest}, End-Stadt-Marker): ist es eine
     * Vanilla-Truhe mit einer Tabelle aus {@link #tables} und faellt der Wurf, wird sie zur Stufen-Truhe mit derselben
     * Tabelle und demselben Seed. Liefert true, wenn ersetzt wurde.
     */
    public static boolean upgradePlaced(ServerLevelAccessor level, BlockPos pos) {
        return enabled() && upgradePlaced(level, pos, ServerTuning.betterChestChance());
    }

    /** {@link #upgradePlaced(ServerLevelAccessor, BlockPos)} mit ausdruecklicher Chance (Tests). */
    public static boolean upgradePlaced(ServerLevelAccessor level, BlockPos pos, double chance) {
        if (!com.simplebuilding.version.McVersion.RARE_STRUCTURE_FINDS) {
            return false;
        }
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
     * Vorlagen-Truhen (Bastion): die verarbeiteten Bloecke einer Vorlage, Vanilla-Truhen mit passender Tabelle im NBT
     * werden zur Stufen-Truhe (der Zustand wird hier noch vor Spiegelung/Drehung getauscht, die Pruefung der
     * Doppeltruhe nimmt die gedrehte Lage). Liefert dieselbe Liste, wenn sich nichts aendert.
     */
    public static List<StructureTemplate.StructureBlockInfo> upgradeTemplate(ServerLevelAccessor level, StructurePlaceSettings settings,
                                                                             List<StructureTemplate.StructureBlockInfo> infos) {
        if (!enabled() || infos.isEmpty()) {
            return infos;
        }
        List<StructureTemplate.StructureBlockInfo> out = null;
        double chance = ServerTuning.betterChestChance();
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

    /** Die Loot-Tabelle aus dem Vorlagen-NBT ({@code LootTable}), oder null. */
    public static @Nullable ResourceKey<LootTable> lootTableOf(CompoundTag nbt) {
        Optional<String> id = nbt.getString(RandomizableContainer.LOOT_TABLE_TAG);
        if (id.isEmpty()) {
            return null;
        }
        Identifier parsed = Identifier.tryParse(id.get());
        return parsed == null ? null : ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, parsed);
    }
}
