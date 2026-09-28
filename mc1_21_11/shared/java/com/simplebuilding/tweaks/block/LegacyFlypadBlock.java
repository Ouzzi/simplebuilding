package com.simplebuilding.tweaks.block;

import com.simplebuilding.tweaks.block.entity.FlypadBlockEntity;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Flypad aus der Zeit vor den drei Enderit-Stufen (bis 2026-09-27 gab es fuenf: I-V). Die IDs
 * {@code netherite_flypad} (alt III) und {@code enderite_flypad} (alt IV) bleiben registriert, damit
 * Welten und Inventare sie laden koennen; im Spiel arbeiten sie als ihre neue Stufe und werden beim
 * ersten Tick zu ihr (Besitzer und verfolgte Flieger bleiben), das Item wird im Inventar
 * umgetauscht ({@code LegacyTierBlockItem}). Kein Rezept, nicht im Kreativ-Tab, alte Textur.
 */
public class LegacyFlypadBlock extends FlypadBlock implements LegacyTierBlock {
    private final Supplier<Block> target;

    public LegacyFlypadBlock(BlockBehaviour.Properties properties, int tier, Supplier<Block> target) {
        super(properties, tier);
        this.target = target;
    }

    /** Die neue Stufe, zu der dieses alte Flypad wird. */
    @Override
    public Block target() {
        return target.get();
    }

    /** Ersetzt das alte Flypad durch seine neue Stufe, ohne dass jemand den Flug verliert. */
    public void migrate(Level level, BlockPos pos, FlypadBlockEntity be) {
        UUID owner = be.getOwner();
        int stage = be.rawEasterStage();
        Set<UUID> flying = new HashSet<>(be.flyingPlayers());
        // Das Ersetzen entfernt die alte Block-Entity; ihr setRemoved nimmt sonst allen den Flug.
        be.flyingPlayers().clear();
        level.setBlock(pos, target().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof FlypadBlockEntity fresh) {
            fresh.setOwner(owner);
            fresh.flyingPlayers().addAll(flying);
            if (stage > 0) {
                fresh.setEasterStage(com.simplebuilding.tweaks.easter.EasterEggs.tierIndex(target()));
            }
        }
    }
}
