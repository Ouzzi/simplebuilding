package com.simplefoundry.block;

import com.simplefoundry.CastTarget;
import com.simplefoundry.Fuels;
import com.simplefoundry.FoundryRegistry;
import com.simplefoundry.FoundryScan;
import com.simplefoundry.Heat;
import com.simplefoundry.Layers;
import com.simplefoundry.Melts;
import com.simplefoundry.Metal;
import com.simplefoundry.Tier;
import com.simplefoundry.config.FoundryConfig;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
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
    /** The fuel tank (owner 02.11.): burn ticks by heat. Coal fills the medium part, lava, blaze rods and coal blocks the high part. */
    private int fuelMedium;
    private int fuelHigh;
    /** Burn ticks of the fuel taken out of the tank that are still burning. */
    private int burn;
    private int burnHeat = Heat.MEDIUM.ordinal();
    private boolean lit;
    private Tier tier = Tier.STONE;
    private int selected;
    private FoundryScan.Result structure;
    private long checkedAt = Long.MIN_VALUE;
    private int age;
    private boolean viewDirty;
    private final Set<BlockPos> shown = new HashSet<>();
    /** Slots of the desk window: 0..11 inputs (the first {@code tier.slots} work), 12 = fuel slot. */
    public static final int INPUT_SLOTS = 12;
    public static final int FUEL_SLOT = 12;
    /** Capacity of the fuel tank in burn ticks (two lava buckets) and the part taken out per burn step. */
    public static final int FUEL_CAPACITY = 40000;
    public static final int FUEL_CHUNK = 100;
    public final SimpleContainer items = new SimpleContainer(INPUT_SLOTS + 1) {
        @Override
        public void setChanged() {
            super.setChanged();
            DeskBlockEntity.this.setChanged();
        }
    };

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

    /** Burn ticks in the tank plus the step that burns now. */
    public int fuelTotal() {
        return fuelMedium + fuelHigh + burn;
    }

    /** Gauge level 0 (empty) to 4 (full), shown on the desk front and in the window. */
    public int fuelLevel() {
        long total = fuelTotal();
        return total <= 0 ? 0 : (int) Math.min(4, (total * 4 + FUEL_CAPACITY - 1) / FUEL_CAPACITY);
    }

    /** Test helper: pours every melt away (the tank is empty, the chambers show nothing). */
    public void clearMelt() {
        java.util.Arrays.fill(melt, 0);
        viewDirty = true;
        setChanged();
    }

    /** Test and break helper: empties the fuel tank. */
    public void clearFuel() {
        fuelMedium = 0;
        fuelHigh = 0;
        burn = 0;
        setChanged();
        syncFuelState();
    }

    public boolean lit() {
        return lit;
    }

    public Tier tier() {
        return tier;
    }

    /** Lifts the desk by one tier when {@code stack} is the next upgrade item; returns whether it was taken (one piece). */
    public boolean upgrade(ItemStack stack) {
        Tier next = tier.next();
        if (next == null || stack.isEmpty() || !stack.is(next.upgradeItem())) return false;
        tier = next;
        setChanged();
        return true;
    }

    /** The tank level the arrow of the desk has selected (0 = lowest), kept inside the structure. */
    public int selectedLevel() {
        int levels = structure != null && structure.valid() ? structure.levels() : 1;
        return Math.max(0, Math.min(levels - 1, selected));
    }

    /** Owner SA4: one click down moves the selection one layer lower, from the bottom back to the top. */
    public int cycleLevel() {
        int levels = structure != null && structure.valid() ? structure.levels() : 1;
        selected = selectedLevel() <= 0 ? levels - 1 : selectedLevel() - 1;
        setChanged();
        return selected;
    }

    /** Window click on a level of the section: selects it (kept inside the structure). */
    public void selectLevel(int wanted) {
        selected = Math.max(0, wanted);
        setChanged();
    }

    private static final java.util.List<String> STATUS = java.util.List.of("ok", "no_chamber", "open_hull", "too_big", "unloaded", "no_grate", "second_desk");

    /** Number of values {@link #data()} syncs to the window. */
    public static final int DATA_COUNT = 20;

    /** Values for the window (16 bit each, so big numbers go in two halves): 0 tier, 1 selected level, 2 levels, 3 status, 4 lit, 5 fuel/16, 6-7 capacity, 8-13 melt, 14-19 waiting. */
    public ContainerData data() {
        return new ContainerData() {
            @Override
            public int get(int i) {
                if (i >= 8) {
                    int group = (i - 8) / 2, value = group < 3 ? melt[group] : pending[group - 3];
                    return (i & 1) == 0 ? value & 0x7FFF : value >>> 15;
                }
                return switch (i) {
                    case 0 -> tier.ordinal();
                    case 1 -> selectedLevel();
                    case 2 -> structure != null && structure.valid() ? structure.levels() : 1;
                    case 3 -> structure == null ? 1 : Math.max(0, STATUS.indexOf(structure.reason()));
                    case 4 -> lit ? 1 : 0;
                    case 5 -> fuelTotal() / 16;
                    case 6 -> capacity() & 0x7FFF;
                    default -> capacity() >>> 15;
                };
            }

            @Override
            public void set(int i, int value) {}

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    public static String statusId(int code) {
        return STATUS.get(Math.max(0, Math.min(STATUS.size() - 1, code)));
    }

    /** Variant A: sets the four window parts of every chamber to the melt that lies at that height; only changed blocks are written. */
    public void refreshChambers(ServerLevel level) {
        Set<BlockPos> now = new HashSet<>();
        if (structure != null && structure.valid()) {
            int cap = capacity(), levels = structure.levels(), minY = structure.minY();
            for (BlockPos pos : structure.chambers()) {
                if (!level.isLoaded(pos)) continue;
                BlockState state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof ChamberBlock)) continue;
                BlockState want = state;
                for (int k = 0; k < ChamberBlock.PARTS; k++) {
                    want = want.setValue(ChamberBlock.SLOTS.get(k), ChamberBlock.Fill.of(Layers.windowPart(melt, cap, levels, pos.getY() - minY, k, ChamberBlock.PARTS)));
                }
                if (want != state) level.setBlock(pos, want, Block.UPDATE_CLIENTS);
                now.add(pos);
            }
        }
        for (BlockPos old : shown) if (!now.contains(old)) resetChamber(level, old);
        shown.clear();
        shown.addAll(now);
    }

    private static void resetChamber(net.minecraft.world.level.Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChamberBlock)) return;
        BlockState empty = state;
        for (var property : ChamberBlock.SLOTS) empty = empty.setValue(property, ChamberBlock.Fill.NONE);
        if (empty != state) level.setBlock(pos, empty, Block.UPDATE_CLIENTS);
    }

    /** The metal that lies at the selected level, or null. */
    public Metal selectedMetal() {
        return structure == null || !structure.valid() ? null : Layers.at(melt, capacity(), structure.levels(), selectedLevel());
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

    /**
     * Pours fuel into the tank (owner 02.11.: lava as the main way, coal as a side way). Takes as many items of the stack as fit by burn
     * ticks and returns that count; the caller shrinks the stack (and hands back the empty bucket for lava).
     */
    public int addFuel(ItemStack stack) {
        Fuels.Fuel f = Fuels.of(stack);
        if (f == null) return 0;
        int n = Math.min(stack.getCount(), (FUEL_CAPACITY - fuelTotal()) / f.ticks());
        if (n <= 0) return 0;
        if (f.heat() == Heat.HIGH) fuelHigh += n * f.ticks();
        else fuelMedium += n * f.ticks();
        setChanged();
        syncFuelState();
        return n;
    }

    /** Writes the gauge level into the desk block (the front texture), only when it changed. */
    private void syncFuelState() {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (state.hasProperty(DeskBlock.FUEL) && state.getValue(DeskBlock.FUEL) != fuelLevel()) {
            level.setBlock(worldPosition, state.setValue(DeskBlock.FUEL, fuelLevel()), Block.UPDATE_CLIENTS);
        }
    }

    /** The tank part that feeds a fire of at least {@code need}: the cheaper medium fuel first, null when nothing fits. */
    private Heat pickFuel(Heat need) {
        if (need == Heat.MEDIUM && fuelMedium > 0) return Heat.MEDIUM;
        return fuelHigh > 0 ? Heat.HIGH : null;
    }

    private boolean slotsFilled() {
        for (int i = 0; i < INPUT_SLOTS; i++) if (!items.getItem(i).isEmpty()) return true;
        return Fuels.of(items.getItem(FUEL_SLOT)) != null;
    }

    /** The window slots work: each active input slot hands one item per melt interval to the waiting melt, the fuel slot pours into the tank. */
    private void feedFromSlots() {
        if (age % FoundryConfig.meltTicksPerUnit == 0) {
            for (int i = 0; i < tier.slots; i++) {
                ItemStack stack = items.getItem(i);
                Melts.Melt m = stack.isEmpty() ? null : Melts.of(stack);
                if (m == null || capacity() - used() < m.units()) continue;
                pending[m.metal().ordinal()] += m.units();
                stack.shrink(1);
                if (stack.isEmpty()) items.setItem(i, ItemStack.EMPTY);
                else items.setChanged();
                setChanged();
            }
        }
        ItemStack fuelStack = items.getItem(FUEL_SLOT);
        if (age % 10 == 0 && !fuelStack.isEmpty()) {
            int n = addFuel(fuelStack);
            if (n > 0) {
                if (fuelStack.is(Items.LAVA_BUCKET)) {
                    items.setItem(FUEL_SLOT, new ItemStack(Items.BUCKET));
                } else {
                    fuelStack.shrink(n);
                    if (fuelStack.isEmpty()) items.setItem(FUEL_SLOT, ItemStack.EMPTY);
                    else items.setChanged();
                }
            }
        }
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
        age++;
        if (viewDirty && age % 10 == 0) {
            viewDirty = false;
            if (structure == null) revalidate();
            refreshChambers(level);
        }
        boolean active = used() > 0 || slotsFilled();
        if (!active) {
            // nothing to do: the rest of the burning step goes back into the tank, an idle desk costs nothing
            if (burn > 0) {
                if (burnHeat == Heat.HIGH.ordinal()) fuelHigh += burn;
                else fuelMedium += burn;
                burn = 0;
                setChanged();
            }
            if (lit) setLit(level, false);
            return;
        }
        long now = level.getGameTime();
        if (structure == null || now - checkedAt >= CHECK) revalidate();
        if (!structure.valid()) {
            if (lit) setLit(level, false);
            if (!shown.isEmpty()) viewDirty = true;
            return;
        }
        feedFromSlots();
        Heat need = neededHeat();
        // fuel: take the next step out of the tank when the fire is out and something waits that the fuel can melt
        if (burn > 0 && need != null && !Heat.values()[burnHeat].atLeast(need) && fuelHigh > 0) {
            // the fire is too cool for what waits and hot fuel is at hand: the rest of the cool step goes back into the tank
            fuelMedium += burn;
            burn = 0;
        }
        if (burn <= 0 && need != null) {
            Heat source = pickFuel(need);
            if (source != null) {
                int take = Math.min(FUEL_CHUNK, source == Heat.HIGH ? fuelHigh : fuelMedium);
                if (source == Heat.HIGH) fuelHigh -= take;
                else fuelMedium -= take;
                burn = take;
                burnHeat = source.ordinal();
                setChanged();
            }
        }
        if (age % 20 == 0) syncFuelState();
        Heat heat = Heat.values()[burnHeat];
        boolean melting = false;
        if (burn > 0) {
            // all lanes of the tier work in parallel: every waiting metal melts up to `lanes` units per interval
            for (Metal m : Metal.values()) {
                if (pending[m.ordinal()] > 0 && heat.atLeast(m.heat)) {
                    melting = true;
                    if (now % FoundryConfig.meltTicksPerUnit == 0) {
                        int n = Math.min(tier.lanes, pending[m.ordinal()]);
                        pending[m.ordinal()] -= n;
                        melt[m.ordinal()] += n;
                        viewDirty = true;
                        setChanged();
                    }
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
            if (!(level.getBlockEntity(spout.pos().relative(spout.facing())) instanceof CastTarget tray)) continue;
            Metal metal = Layers.at(melt, capacity, structure.levels(), spout.level());
            if (metal == null || melt[metal.ordinal()] < Metal.INGOT || !tray.accepts(metal)) continue;
            melt[metal.ordinal()] -= Metal.INGOT;
            tray.add(metal, now, FoundryConfig.castTicks);
            viewDirty = true;
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

    /** The slot items fall out when the desk is broken (the fuel tank, waiting items and the melt are lost with the tank). */
    public void dropContents(net.minecraft.world.level.Level level, BlockPos pos) {
        for (int i = 0; i < items.getContainerSize(); i++) {
            if (!items.getItem(i).isEmpty()) Block.popResource(level, pos, items.getItem(i).copy());
        }
        items.clearContent();
        for (BlockPos pos2 : shown) resetChamber(level, pos2);
        shown.clear();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (Metal m : Metal.values()) {
            melt[m.ordinal()] = Math.max(0, input.getIntOr("Melt_" + m.id, 0));
            pending[m.ordinal()] = Math.max(0, input.getIntOr("Pending_" + m.id, 0));
        }
        tier = Tier.byIndex(input.getIntOr("Tier", 0));
        selected = Math.max(0, input.getIntOr("Selected", 0));
        burn = Math.max(0, input.getIntOr("Burn", 0));
        burnHeat = Math.max(0, Math.min(Heat.values().length - 1, input.getIntOr("BurnHeat", 0)));
        lit = input.getBooleanOr("Lit", false);
        viewDirty = true;
        fuelMedium = Math.max(0, input.getIntOr("FuelMedium", 0));
        fuelHigh = Math.max(0, input.getIntOr("FuelHigh", 0));
        items.clearContent();
        for (int i = 0; i < items.getContainerSize(); i++) items.setItem(i, input.read("Slot" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        // worlds from before the tank: the old single fuel stack pours in
        input.read("Fuel", ItemStack.CODEC).ifPresent(old -> addFuel(old));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (Metal m : Metal.values()) {
            output.putInt("Melt_" + m.id, melt[m.ordinal()]);
            output.putInt("Pending_" + m.id, pending[m.ordinal()]);
        }
        output.putInt("Tier", tier.ordinal());
        output.putInt("Selected", selected);
        output.putInt("Burn", burn);
        output.putInt("BurnHeat", burnHeat);
        output.putBoolean("Lit", lit);
        output.putInt("FuelMedium", fuelMedium);
        output.putInt("FuelHigh", fuelHigh);
        for (int i = 0; i < items.getContainerSize(); i++) {
            if (!items.getItem(i).isEmpty()) output.store("Slot" + i, ItemStack.CODEC, items.getItem(i));
        }
    }
}
