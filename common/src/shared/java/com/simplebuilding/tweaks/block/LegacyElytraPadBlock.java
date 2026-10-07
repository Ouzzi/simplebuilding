package com.simplebuilding.tweaks.block;

import com.simplebuilding.tweaks.block.entity.ElytraPadBlockEntity;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Elytra-Pad aus der Zeit vor den drei Stufen (bis 2026-10-07 gab es fuenf: I-V). Die IDs
 * {@code reinforced_elytra_pad} (alt II) und {@code fine_elytra_pad} (alt V) bleiben registriert, damit
 * Welten und Inventare sie laden koennen; im Spiel arbeiten sie als ihre neue Stufe und werden beim
 * ersten Tick zu ihr (Besitzer bleibt, eine Easter-Stufe wird auf die neue Stufe umgerechnet), das Item
 * wird im Inventar umgetauscht ({@code LegacyTierBlockItem}). Kein Rezept, nicht im Kreativ-Tab, alte
 * Textur.
 */
public class LegacyElytraPadBlock extends ElytraPadBlock implements LegacyTierBlock {
    private final Supplier<Block> target;

    public LegacyElytraPadBlock(BlockBehaviour.Properties properties, int tier, Supplier<Block> target) {
        super(properties, tier);
        this.target = target;
    }

    /** Die neue Stufe, zu der dieses alte Elytra-Pad wird. */
    @Override
    public Block target() {
        return target.get();
    }

    /** Ersetzt das alte Elytra-Pad durch seine neue Stufe; Besitzer und Easter-Stufe bleiben. */
    public void migrate(Level level, BlockPos pos, ElytraPadBlockEntity be) {
        UUID owner = be.getOwner();
        int stage = be.rawEasterStage();
        level.setBlock(pos, target().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof ElytraPadBlockEntity fresh) {
            fresh.setOwner(owner);
            if (stage > 0) {
                fresh.setEasterStage(com.simplebuilding.tweaks.easter.EasterEggs.tierIndex(target()));
            }
        }
    }
}