package com.simplebuilding.tweaks.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.tweaks.block.entity.SpawnTeleporterBlockEntity;
import com.simplebuilding.tweaks.block.entity.TweaksBlockEntities;
import com.simplebuilding.version.BlockCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Spawn-Teleporter, drei Stufen (Besitzer 2026-09-28, wie das Launchpad): stillstehen teleportiert zum
 * Spawn-Ziel ({@code worldspawn setspawn1}, sonst Weltspawn). Die Stufen unterscheiden sich in der
 * Wartezeit - I 50 s, II 20 s, III 5 s ({@code SpawnTeleporterBlockEntity#requiredTicks}); Stufe III
 * ({@link #ENDERITE_TIER}, Enderit) behaelt die Zusatzfunktion des frueheren Enderit-Teleporters V:
 * Ziel ist der eigene Wiedereinstiegspunkt (Bett/Anker), sonst ebenfalls das Spawn-Ziel.
 */
public class SpawnTeleporterBlock extends WaterloggedPadBlock {
    public static final int ENDERITE_TIER = 3;
    /** Hoechste Stufe. */
    public static final int MAX_TIER = 3;
    public static final MapCodec<SpawnTeleporterBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCodecs.propertiesField(),
            Codec.INT.fieldOf("tier").forGetter(SpawnTeleporterBlock::getTier)
    ).apply(i, SpawnTeleporterBlock::new));

    private final int tier;

    public SpawnTeleporterBlock(BlockBehaviour.Properties properties, int tier) {
        super(properties, Block.box(1, 0, 1, 15, 1, 15), PadOwnership.OWNER_PAD, PadOwnership.STRANGER_PAD);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpawnTeleporterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TweaksBlockEntities.SPAWN_TELEPORTER,
                level.isClientSide() ? SpawnTeleporterBlockEntity::clientTick : SpawnTeleporterBlockEntity::serverTick);
    }
}
