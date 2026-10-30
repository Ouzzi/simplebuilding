package com.simpledinos.block;

import com.simpledinos.DinoKind;
import com.simpledinos.DinosRegistry;
import com.simpledinos.Progress;
import com.simpledinos.config.DinosConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Brushing state of a fossil block: which egg is inside ({@link #kind}, rolled from the position so that stopping and starting
 * again never rerolls it) and the {@link Progress} (about 60 s of brushing; after a pause of {@link #GRACE} ticks it runs back by
 * two ticks per tick).
 */
public class FossilBlockEntity extends BlockEntity {
    /** A brusher is checked every tick; a gap of 5 ticks (0.25 s) is still "brushing". */
    public static final int GRACE = 5;
    private int sort = -1;
    private final Progress progress = new Progress(GRACE);

    public FossilBlockEntity(BlockPos pos, BlockState state) {
        super(DinosRegistry.FOSSIL_BE, pos, state);
    }

    /** The dinosaur whose egg this block holds (stable per position). */
    public DinoKind kind() {
        if (sort < 0) {
            sort = DinoKind.roll(RandomSource.create(this.worldPosition.asLong() * 0x9E3779B97F4A7C15L + 0x51ED2701L)).ordinal();
            setChanged();
        }
        return DinoKind.byIndex(sort);
    }

    public int progress() {
        return progress.value();
    }

    public Progress progressState() {
        return progress;
    }

    /** One step at game time {@code now}: brushing adds a tick, otherwise the rollback is settled. Finishes the block at 100 %. */
    public void advance(ServerLevel level, long now, boolean brushing) {
        int total = DinosConfig.brushTicks();
        if (brushing) progress.add(now, 1, total);
        else progress.settle(now);
        setChanged();
        BlockState state = getBlockState();
        int stage = Math.min(FossilBlock.STAGES - 1, (int) ((long) progress.value() * FossilBlock.STAGES / total));
        if (progress.value() >= total) {
            finish(level);
        } else if (state.getValue(FossilBlock.STAGE) != stage) {
            level.setBlock(this.worldPosition, state.setValue(FossilBlock.STAGE, stage), Block.UPDATE_CLIENTS);
        }
    }

    private void finish(ServerLevel level) {
        BlockPos pos = this.worldPosition;
        BlockState state = getBlockState();
        ItemStack egg = new ItemStack(DinosRegistry.eggItem(kind()));
        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, egg));
        if (level.getRandom().nextBoolean()) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, new ItemStack(Items.BONE)));
        }
        level.levelEvent(2001, pos, Block.getId(state));
        level.playSound(null, pos, SoundEvents.BRUSH_SAND_COMPLETED, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.setBlock(pos, Blocks.SANDSTONE.defaultBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.sort = input.getIntOr("sort", -1);
        this.progress.set(input.getIntOr("progress", 0), input.getLongOr("last_brush", 0L), input.getLongOr("settled", 0L));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("sort", this.sort);
        output.putInt("progress", this.progress.value());
        output.putLong("last_brush", this.progress.lastWork());
        output.putLong("settled", this.progress.settledAt());
    }
}
