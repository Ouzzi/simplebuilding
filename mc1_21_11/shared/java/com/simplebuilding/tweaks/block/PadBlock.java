package com.simplebuilding.tweaks.block;

import com.simplebuilding.tweaks.block.entity.OwnedBlockEntity;
import com.simplebuilding.tweaks.easter.EasterEggs;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.simplebuilding.tweaks.block.entity.PadSignalSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Gemeinsamer Teil aller Simple-Tweaks-Pads mit Block-Entity: flache Form, Besitzer beim Setzen,
 * Abbautempo nach Besitz (siehe {@link PadOwnership}). Ein Easter-Pad ({@link EasterEggs}) laesst
 * beim Abbau wieder sein Easter-Item fallen.
 *
 * <p><b>Redstone</b> (Besitzer 2026-09-28): die eigentlichen Pads (Launchpad, Trank-Pad, Flypad,
 * Elytra-Pad, Spawn-Teleporter; {@link #isRedstoneControlled}) schalten ab, solange sie ein
 * Redstone-Signal bekommen ({@link #isDisabledByRedstone}), und liefern einem Komparator ein Signal
 * ({@link PadSignalSource}). Chunk-Loader und Kupfer-Druckplatte teilen nur die Basisklasse.
 */
public abstract class PadBlock extends BaseEntityBlock {
    private final VoxelShape shape;
    private final float ownerSpeed;
    private final float strangerSpeed;

    protected PadBlock(BlockBehaviour.Properties properties, VoxelShape shape, float ownerSpeed, float strangerSpeed) {
        super(properties);
        this.shape = shape;
        this.ownerSpeed = ownerSpeed;
        this.strangerSpeed = strangerSpeed;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    /** Pads, die Redstone abschaltet und die ein Komparator liest; Chunk-Loader und Druckplatten nicht. */
    protected boolean isRedstoneControlled() {
        return false;
    }

    /** Bekommt das Pad an {@code pos} ein Redstone-Signal? Dann gibt es nichts (kein Start, kein Flug, keine Wirkung). */
    public static boolean isDisabledByRedstone(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof PadBlock pad && pad.isRedstoneControlled()
                && level.hasNeighborSignal(pos);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return isRedstoneControlled();
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return isRedstoneControlled() && level.getBlockEntity(pos) instanceof PadSignalSource source
                ? Math.max(0, Math.min(15, source.comparatorSignal())) : 0;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, by, itemStack);
        PadOwnership.onPlaced(level, pos, by);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return PadOwnership.destroyProgress(player, level, pos, ownerSpeed, strangerSpeed,
                super.getDestroyProgress(state, player, level, pos));
    }

    /** Die Loot-Tabelle liefert das normale Item; ein Easter-Pad schreibt seine Stufe samt Namen darauf. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof OwnedBlockEntity owned && owned.easterStage() > 0) {
            for (ItemStack stack : drops) {
                if (stack.is(this.asItem())) {
                    EasterEggs.mark(stack, owned.easterStage());
                }
            }
        }
        return drops;
    }
}
