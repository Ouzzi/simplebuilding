package com.simplefoundry.block;

import com.simplefoundry.Fuels;
import com.simplefoundry.FoundryRegistry;
import com.simplefoundry.FoundryScan;
import com.simplefoundry.Heat;
import com.simplefoundry.Layers;
import com.simplefoundry.Melts;
import com.simplefoundry.Metal;
import com.simplefoundry.config.FoundryConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The foundry's brain (concept sections 5-8). Items thrown in wait as {@code pending} units, melt (one unit per {@code meltTicksPerUnit}
 * ticks while fuel burns hot enough) into {@code melt}, which lies in the tank by density ({@link Layers}); every {@code castTicks} each
 * spout pours nine units of the layer at its height into the tray in front of it. A desk with nothing to do does no work at all;
 * an active one checks the structure every {@value #CHECK} ticks (not per tick).
 */
public class DeskBlockEntity extends BlockEntity {
    public static final int CHECK = 100;
    private final int[] melt = new int[Metal.values().length];
    private final int[] pending = new int[Metal.values().length];
    private ItemStack fuel = ItemStack.EMPTY;
    private int burn;
    private int burnHeat = Heat.MEDIUM.ordinal();
    private boolean lit;
    private FoundryScan.Result structure;
    private long checkedAt = Long.MIN_VALUE;

    public DeskBlockEntity(BlockPos pos, BlockState state) {
        super(FoundryRegistry.DESK_BE, pos, state);
    }

    public int melt(Metal metal) {
        return melt[metal.ordinal()];
    }

    public int pending(Metal metal) {
        return pending[metal.ordinal()];
    }

    public int[] meltUnits() {
        return melt.clone();
    }

    public int burnTicks() {
        return burn;
    }

    public ItemStack fuel() {
        return fuel;
    }

    public boolean lit() {
        return lit;
    }

    public FoundryScan.Result structure() {
        return structure;
    }

    public int used() {
        int sum = 0;
        for (int i = 0; i < melt.length; i++) sum += melt[i] + pending[i];
        return sum;
    }

    /** Capacity of the checked structure in units (0 while it is not valid). */
    public int capacity() {
        return structure != null && structure.valid() ? structure.chambers().size() * Metal.CHAMBER_UNITS : 0;
    }

    /** Checks the structure now (use, and the periodic check of an active desk). */
    public FoundryScan.Result revalidate() {
        if (level == null) return structure;
        structure = FoundryScan.scan(level, worldPosition);
        checkedAt = level.getGameTime();
        return structure;
    }

    /** Puts as many items of the stack as fit into the pending melt; returns how many were taken. Needs a valid structure. */
    public int insert(ItemStack stack) {
        Melts.Melt m = Melts.of(stack);
        if (m == null || !revalidate().valid()) return 0;
        int room = capacity() - used();
        int n = Math.min(stack.getCount(), room / m.units());
        if (n <= 0) return 0;
        pending[m.metal().ordinal()] += n * m.units();
        setChanged();
        return n;
    }

    /** Adds fuel to the single fuel slot (same item only); returns how many were taken. */
    public int addFuel(ItemStack stack) {
        if (Fuels.of(stack) == null) return 0;
        if (!fuel.isEmpty() && !ItemStack.isSameItemSameComponents(fuel, stack)) return 0;
        int n = Math.min(stack.getCount(), stack.getMaxStackSize() - fuel.getCount());
        if (n <= 0) return 0;
        if (fuel.isEmpty()) fuel = stack.copyWithCount(n);
        else fuel.grow(n);
        setChanged();
        return n;
    }

    /** The lowest heat any waiting item needs, or null when nothing waits. */
    private Heat neededHeat() {
        Heat need = null;
        for (Metal m : Metal.values()) {
            if (pending[m.ordinal()] > 0 && (need == null || m.heat.ordinal() < need.ordinal())) need = m.heat;
        }
        return need;
    }

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, DeskBlockEntity be) {
        be.serverTick(level);
    }

    private void serverTick(ServerLevel level) {
        boolean active = used() > 0 || burn > 0;
        if (!active) {
            if (lit) setLit(level, false);
            return;
        }
        long now = level.getGameTime();
        if (structure == null || now - checkedAt >= CHECK) revalidate();
        if (!structure.valid()) {
            if (lit) setLit(level, false);
            return;
        }
        Heat need = neededHeat();
        // fuel: take one when the fire is out and something waits that the fuel can melt
        if (burn <= 0 && need != null && !fuel.isEmpty()) {
            Fuels.Fuel f = Fuels.of(fuel);
            if (f != null && f.heat().atLeast(need)) {
                burn = f.ticks();
                burnHeat = f.heat().ordinal();
                if (fuel.is(Items.LAVA_BUCKET)) Block.popResource(level, worldPosition.above(), new ItemStack(Items.BUCKET));
                fuel.shrink(1);
                if (fuel.isEmpty()) fuel = ItemStack.EMPTY;
                setChanged();
            }
        }
        Heat heat = Heat.values()[burnHeat];
        boolean melting = false;
        if (burn > 0) {
            for (Metal m : Metal.values()) {
                if (pending[m.ordinal()] > 0 && heat.atLeast(m.heat)) {
                    melting = true;
                    if (now % FoundryConfig.meltTicksPerUnit == 0) {
                        pending[m.ordinal()]--;
                        melt[m.ordinal()]++;
                        setChanged();
                    }
                    break;
                }
            }
            if (melting) burn--;
        }
        boolean shouldBeLit = burn > 0 && melting;
        if (shouldBeLit != lit) setLit(level, shouldBeLit);
        if (now % FoundryConfig.castTicks == 0) pour(level, now);
    }

    private void pour(ServerLevel level, long now) {
        int capacity = capacity();
        for (FoundryScan.Spout spout : structure.spouts()) {
            if (!(level.getBlockEntity(spout.pos().relative(spout.facing())) instanceof TrayBlockEntity tray)) continue;
            Metal metal = Layers.at(melt, capacity, structure.levels(), spout.level());
            if (metal == null || melt[metal.ordinal()] < Metal.INGOT || !tray.accepts(metal)) continue;
            melt[metal.ordinal()] -= Metal.INGOT;
            tray.add(metal, now, FoundryConfig.castTicks);
            setChanged();
        }
    }

    private void setLit(ServerLevel level, boolean value) {
        lit = value;
        if (structure != null) {
            for (BlockPos grate : structure.grates()) {
                BlockState state = level.getBlockState(grate);
                if (state.hasProperty(GrateBlock.LIT) && state.getValue(GrateBlock.LIT) != value) level.setBlock(grate, state.setValue(GrateBlock.LIT, value), Block.UPDATE_ALL);
            }
        }
        setChanged();
    }

    /** The fuel falls out when the desk is broken (waiting items and the melt are lost with the tank). */
    public void dropContents(net.minecraft.world.level.Level level, BlockPos pos) {
        if (!fuel.isEmpty()) Block.popResource(level, pos, fuel.copy());
        fuel = ItemStack.EMPTY;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (Metal m : Metal.values()) {
            melt[m.ordinal()] = Math.max(0, input.getIntOr("Melt_" + m.id, 0));
            pending[m.ordinal()] = Math.max(0, input.getIntOr("Pending_" + m.id, 0));
        }
        burn = Math.max(0, input.getIntOr("Burn", 0));
        burnHeat = Math.max(0, Math.min(Heat.values().length - 1, input.getIntOr("BurnHeat", 0)));
        lit = input.getBooleanOr("Lit", false);
        fuel = input.read("Fuel", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (Metal m : Metal.values()) {
            output.putInt("Melt_" + m.id, melt[m.ordinal()]);
            output.putInt("Pending_" + m.id, pending[m.ordinal()]);
        }
        output.putInt("Burn", burn);
        output.putInt("BurnHeat", burnHeat);
        output.putBoolean("Lit", lit);
        if (!fuel.isEmpty()) output.store("Fuel", ItemStack.CODEC, fuel);
    }
}
