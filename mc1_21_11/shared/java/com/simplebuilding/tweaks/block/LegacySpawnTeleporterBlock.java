package com.simplebuilding.tweaks.block;

import com.simplebuilding.tweaks.block.entity.SpawnTeleporterBlockEntity;
import com.simplebuilding.tweaks.easter.EasterEggs;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Spawn-Teleporter aus der Zeit vor den drei Stufen (bis 2026-09-28 gab es fuenf: I-V). Die IDs
 * {@code spawn_teleporter_tier_3} (alt III) und {@code spawn_teleporter_tier_4} (alt IV) bleiben
 * registriert, damit Welten und Inventare sie laden koennen: alt III wird Stufe II, alt IV Stufe III
 * (wie das alte V, {@code enderite_spawn_teleporter}, das seine Id als Stufe III behaelt). Im Spiel
 * arbeiten sie als ihre neue Stufe und werden beim ersten Tick zu ihr; Besitzer, Wasser und eine
 * Easter-Stufe (auf die neue Stufe umgerechnet) bleiben.
 */
public class LegacySpawnTeleporterBlock extends SpawnTeleporterBlock implements LegacyTierBlock {
    private final Supplier<Block> target;

    public LegacySpawnTeleporterBlock(BlockBehaviour.Properties properties, int tier, Supplier<Block> target) {
        super(properties, tier);
        this.target = target;
    }

    @Override
    public Block target() {
        return target.get();
    }

    /** Ersetzt den alten Teleporter durch seine neue Stufe. */
    public void migrate(Level level, BlockPos pos, BlockState state, SpawnTeleporterBlockEntity be) {
        UUID owner = be.getOwner();
        int stage = be.rawEasterStage();
        BlockState fresh = target().defaultBlockState();
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && fresh.hasProperty(BlockStateProperties.WATERLOGGED)) {
            fresh = fresh.setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED));
        }
        level.setBlock(pos, fresh, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof SpawnTeleporterBlockEntity next) {
            next.setOwner(owner);
            if (stage > 0) {
                next.setEasterStage(EasterEggs.tierIndex(target()));
            }
        }
    }
}
