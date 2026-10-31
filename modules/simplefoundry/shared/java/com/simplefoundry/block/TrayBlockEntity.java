package com.simplefoundry.block;

import com.simplefoundry.FoundryRegistry;
import com.simplefoundry.Metal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Up to {@value #MAX} ingots of one metal. All ingots set together: the batch is ready when the last one has cooled ({@code readyAt}). */
public class TrayBlockEntity extends BlockEntity {
    public static final int MAX = 9;
    private int metal = -1;
    private int count;
    private long readyAt;

    public TrayBlockEntity(BlockPos pos, BlockState state) {
        super(FoundryRegistry.TRAY_BE, pos, state);
    }

    public int count() {
        return count;
    }

    public Metal metal() {
        return metal < 0 ? null : Metal.values()[metal];
    }

    public long readyAt() {
        return readyAt;
    }

    public boolean accepts(Metal m) {
        return count < MAX && (count == 0 || metal == m.ordinal());
    }

    /** One more ingot; sets the tray's visible content and the time it needs to cool. */
    public void add(Metal m, long now, int castTicks) {
        if (!accepts(m)) return;
        metal = m.ordinal();
        count++;
        readyAt = now + castTicks;
        refresh();
    }

    /** Takes the set ingots (drops them above the tray); false while they are still hot. */
    public boolean take(Level level, BlockPos pos) {
        if (count == 0 || level.getGameTime() < readyAt) return count == 0;
        drop(level, pos);
        refresh();
        return true;
    }

    /** Drops every ingot above the tray and empties it (no cooling check; also used when the tray is broken). */
    public void drop(Level level, BlockPos pos) {
        if (count == 0) return;
        ItemStack stack = new ItemStack(metal().ingot(), count);
        while (!stack.isEmpty()) Block.popResource(level, pos.above(), stack.split(stack.getMaxStackSize()));
        metal = -1;
        count = 0;
        setChanged();
    }

    private void refresh() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            int content = metal < 0 ? 0 : metal + 1;
            BlockState now = getBlockState();
            if (now.getValue(TrayBlock.CONTENT) != content) level.setBlock(worldPosition, now.setValue(TrayBlock.CONTENT, content), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int m = input.getIntOr("Metal", -1);
        metal = m >= 0 && m < Metal.values().length ? m : -1;
        count = metal < 0 ? 0 : Math.max(0, Math.min(MAX, input.getIntOr("Count", 0)));
        readyAt = input.getLongOr("ReadyAt", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Metal", metal);
        output.putInt("Count", count);
        output.putLong("ReadyAt", readyAt);
    }
}
