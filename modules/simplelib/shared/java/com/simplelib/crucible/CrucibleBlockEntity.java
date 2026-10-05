package com.simplelib.crucible;

import com.simplelib.registry.LibBlockEntities;
import com.simplelib.warm.Warm;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A crucible (plan sections 5-8). Every slot cooks on its own, all slots in parallel, without a cap
 * (owner F18). Before a slot starts it reserves the place for its result: the slot directly below
 * first (F13), otherwise any other free or matching slot, its own slot only when the last item is
 * the one cooking. Reserved empty slots show the coming result faintly; whatever a player puts into
 * a reserved slot stops that job (red) until it is taken out again (F18), unless it is exactly the
 * result item (owner 17). Finished results are locked (never cooked again) until taken out (F14).
 */
public class CrucibleBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int IDLE = 0, COOKING = 1, BLOCKED = 2, COLD = 3, RESERVED = 4, RESULT = 5, NO_RECIPE = 6;
    /** Progress is counted in thousandths of a tick so fractional speeds stay exact. */
    private static final int MILLI = 1000;

    private final CrucibleTier tier;
    private NonNullList<ItemStack> items;
    private final int[] progress;
    private final int[] target;
    private final boolean[] result;
    private final int[] state;
    private final ItemStack[] ghost;
    private final ItemStack[] lastInput;
    private final CrucibleJob[] jobs;
    private final HeatLevel[] jobHeat;
    private float experience;
    private HeatLevel heat = HeatLevel.NONE;
    private double heatMultiplier = 1.0;
    private int afterglow;
    private boolean heatDirty = true;
    private int age;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int n = tier.slots();
            if (index < n) return state[index] * 128 + percent(index);
            return switch (index - n) {
                case 0 -> heat.ordinal();
                case 1 -> heatSourceGone ? Math.min(afterglow, Short.MAX_VALUE) : 0;
                case 2 -> heatMultiplier < 1.0 ? 1 : 0;
                case 3 -> barrel() != null ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return tier.slots() + 4;
        }
    };
    private boolean heatSourceGone;

    public CrucibleBlockEntity(BlockPos pos, BlockState blockState) {
        this(LibBlockEntities.CRUCIBLE, pos, blockState);
    }

    /** For partner tiers with their own block entity type (SimpleBuilding's Enderite crucible). */
    public CrucibleBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.tier = blockState.getBlock() instanceof CrucibleBlock block ? block.tier() : CrucibleTier.IRON;
        int n = tier.slots();
        this.items = NonNullList.withSize(n, ItemStack.EMPTY);
        this.progress = new int[n];
        this.target = new int[n];
        Arrays.fill(target, -1);
        this.result = new boolean[n];
        this.state = new int[n];
        this.ghost = new ItemStack[n];
        Arrays.fill(ghost, ItemStack.EMPTY);
        this.lastInput = new ItemStack[n];
        Arrays.fill(lastInput, ItemStack.EMPTY);
        this.jobs = new CrucibleJob[n];
        this.jobHeat = new HeatLevel[n];
    }

    public CrucibleTier tier() { return tier; }
    public ContainerData data() { return data; }
    public HeatLevel heat() { return heat; }
    public int afterglow() { return afterglow; }
    public double heatMultiplier() { return heatMultiplier; }
    public int slotState(int slot) { return state[slot]; }
    public boolean isResult(int slot) { return result[slot]; }
    public int target(int slot) { return target[slot]; }
    public ItemStack ghost(int slot) { return ghost[slot]; }
    public float storedExperience() { return experience; }

    public int percent(int slot) {
        CrucibleJob job = jobs[slot];
        if (job == null || progress[slot] <= 0) return 0;
        return Math.min(100, (int) (100L * progress[slot] / ((long) job.ticks() * MILLI)));
    }

    public void markHeatDirty() {
        heatDirty = true;
    }

    // ---------------------------------------------------------------- ticking

    public static void serverTick(Level level, BlockPos pos, BlockState blockState, CrucibleBlockEntity be) {
        if (level instanceof ServerLevel server) be.tick(server, pos, blockState);
    }

    void tick(ServerLevel level, BlockPos pos, BlockState blockState) {
        age++;
        updateHeat(level, pos);
        int n = tier.slots();
        boolean changed = false;
        for (int i = 0; i < n; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                if (result[i] || progress[i] != 0 || target[i] >= 0) changed = true;
                result[i] = false;
                progress[i] = 0;
                target[i] = -1;
                jobs[i] = null;
                lastInput[i] = ItemStack.EMPTY;
                continue;
            }
            boolean same = Warm.warmable(stack) ? ItemStack.isSameItem(stack, lastInput[i])
                    : ItemStack.isSameItemSameComponents(stack, lastInput[i]);
            if (!same) {
                result[i] = false;
                progress[i] = 0;
                target[i] = -1;
                jobs[i] = null;
            }
            lastInput[i] = stack.copyWithCount(1);
        }
        if (age % 20 == 0) changed |= keepWarm(level);
        List<Integer> active = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) { state[i] = IDLE; continue; }
            if (result[i]) { state[i] = RESULT; continue; }
            if (Warm.warmable(stack) && Warm.isWarm(stack, level)) {
                result[i] = true;
                state[i] = RESULT;
                continue;
            }
            CrucibleJob job = jobs[i];
            if (job == null || jobHeat[i] != heat || age % 20 == 0) {
                CrucibleJob found = CrucibleJob.find(level, stack, heat);
                if (job != null && (found == null || !ItemStack.isSameItemSameComponents(found.result(), job.result()))) {
                    target[i] = -1;
                    progress[i] = 0;
                }
                job = jobs[i] = found;
                jobHeat[i] = heat;
            }
            if (job == null) { state[i] = NO_RECIPE; target[i] = -1; continue; }
            if (!job.allowedAt(heat)) { state[i] = COLD; continue; }
            active.add(i);
        }
        // Owner F18: when room is short, whatever finishes first goes first.
        active.sort((a, b) -> Long.compare(remaining(a), remaining(b)));
        for (int i : active) {
            CrucibleJob job = jobs[i];
            if (job.warming()) target[i] = i;
            if (target[i] >= BARREL && barrel() == null) target[i] = -1;
            if (target[i] < 0) target[i] = reserve(i, job);
            if (target[i] < 0 || !targetUsable(i, target[i], job)) {
                state[i] = BLOCKED;
                continue;
            }
            state[i] = COOKING;
            progress[i] += step();
            if (progress[i] >= job.ticks() * MILLI) {
                finish(level, i, job);
                changed = true;
            }
        }
        updateGhosts();
        boolean lit = active.stream().anyMatch(i -> state[i] == COOKING);
        if (blockState.hasProperty(CrucibleBlock.LIT) && blockState.getValue(CrucibleBlock.LIT) != lit) {
            level.setBlock(pos, blockState.setValue(CrucibleBlock.LIT, lit), Block.UPDATE_ALL);
        }
        if (changed) changedAndSync();
    }

    private long remaining(int slot) {
        CrucibleJob job = jobs[slot];
        return (long) job.ticks() * MILLI - progress[slot];
    }

    /** Ticks until the next running slot finishes at the current speed, or -1 when none runs. */
    public int nextCompletionTicks() {
        long shortest = Long.MAX_VALUE;
        if (heat == HeatLevel.NONE) return -1;
        for (int slot = 0; slot < tier.slots(); slot++) {
            if (state[slot] == COOKING && jobs[slot] != null) {
                shortest = Math.min(shortest, Math.max(0L, remaining(slot)));
            }
        }
        return shortest == Long.MAX_VALUE ? -1 : (int) Math.min(Integer.MAX_VALUE, (shortest + step() - 1) / step());
    }

    private int step() {
        return Math.max(1, (int) Math.round(MILLI * tier.speed() * heat.factor() * heatMultiplier));
    }

    private void updateHeat(ServerLevel level, BlockPos pos) {
        if (heatDirty || age % com.simplelib.config.LibConfig.heatRecheckTicks == 0) {
            heatDirty = false;
            barrelPos = attachedBarrelPos(level, pos);
            Heat.Reading reading = Heat.at(level, pos);
            if (reading.level() != HeatLevel.NONE) {
                heat = reading.level();
                heatMultiplier = reading.multiplier();
                afterglow = tier.afterglowTicks();
                heatSourceGone = false;
                return;
            }
            heatSourceGone = true;
        }
        if (heatSourceGone) {
            if (afterglow > 0) afterglow--;
            else { heat = HeatLevel.NONE; heatMultiplier = 1.0; }
        }
    }

    /** Owner 39: food does not cool down inside a crucible. */
    private boolean keepWarm(ServerLevel level) {
        boolean changed = false;
        long now = level.getGameTime();
        for (ItemStack stack : items) {
            if (Warm.isWarm(stack, level)) {
                Warm.warm(stack, now);
                changed = true;
            }
        }
        return changed;
    }

    /** Targets at or above this index are slots of the attached barrel (target - BARREL). */
    public static final int BARREL = 1000;
    private @Nullable BlockPos barrelPos;
    private final ItemStack[] barrelGhost = new ItemStack[BarrelTier.CRUCIBLE_SLOTS];

    /** The barrel attached to the crucible at {@code pos} (facing it, attached), if any. */
    public static @Nullable BlockPos attachedBarrelPos(Level level, BlockPos pos) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos p = pos.relative(d);
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof CrucibleBarrelBlock && s.getValue(CrucibleBarrelBlock.ATTACHED)
                    && s.getValue(CrucibleBarrelBlock.FACING) == d.getOpposite()) return p;
        }
        return null;
    }

    public @Nullable CrucibleBarrelBlockEntity barrel() {
        if (level == null || barrelPos == null) return null;
        return level.getBlockEntity(barrelPos) instanceof CrucibleBarrelBlockEntity b
                && b.getBlockState().getValue(CrucibleBarrelBlock.ATTACHED) ? b : null;
    }

    public ItemStack barrelGhost(int slot) {
        ItemStack g = barrelGhost[slot];
        return g == null ? ItemStack.EMPTY : g;
    }

    private ItemStack stackAt(int t) {
        if (t < BARREL) return items.get(t);
        CrucibleBarrelBlockEntity b = barrel();
        return b == null ? ItemStack.EMPTY : b.getItem(t - BARREL);
    }

    private int maxAt(int t, ItemStack stack) {
        if (t < BARREL) return getMaxStackSize(stack);
        CrucibleBarrelBlockEntity b = barrel();
        return b == null ? 0 : b.getMaxStackSize(stack);
    }

    /**
     * Picks and returns the result place for slot {@code i}, or -1: the attached barrel first (owner
     * wish round 2), then the slot directly below (F13), other rows, the same row, own slot last.
     */
    int reserve(int i, CrucibleJob job) {
        if (barrel() != null) {
            for (int k = 0; k < BarrelTier.CRUCIBLE_SLOTS; k++) if (canReserve(i, BARREL + k, job)) return BARREL + k;
        }
        int n = tier.slots();
        int below = tier.below(i);
        if (below >= 0 && canReserve(i, below, job)) return below;
        for (int k = 1; k < n; k++) {
            int t = (i + k) % n;
            if (t != below && tier.row(t) != tier.row(i) && canReserve(i, t, job)) return t;
        }
        for (int k = 1; k < n; k++) {
            int t = (i + k) % n;
            if (t != below && tier.row(t) == tier.row(i) && canReserve(i, t, job)) return t;
        }
        if (canReserve(i, i, job)) return i;
        return -1;
    }

    private boolean canReserve(int i, int t, CrucibleJob job) {
        ItemStack out = job.result();
        ItemStack there = stackAt(t);
        if (t == i) return there.getCount() == 1 && reservedBy(t, i) == 0;
        boolean barrelSlot = t >= BARREL;
        if (!there.isEmpty() && (!(barrelSlot || result[t]) || !ItemStack.isSameItemSameComponents(there, out))) return false;
        for (int k = 0; k < tier.slots(); k++) {
            if (k != i && target[k] == t && jobs[k] != null && !ItemStack.isSameItemSameComponents(jobs[k].result(), out)) return false;
        }
        int room = maxAt(t, out) - there.getCount() - reservedBy(t, i);
        return room >= out.getCount();
    }

    /** Result items other jobs have already reserved in place {@code t}. */
    private int reservedBy(int t, int except) {
        int sum = 0;
        for (int k = 0; k < tier.slots(); k++) {
            if (k != except && target[k] == t && jobs[k] != null && k != t) sum += jobs[k].result().getCount();
        }
        return sum;
    }

    /** A reserved place is still usable unless something foreign was put into it (owner F18/17). */
    private boolean targetUsable(int i, int t, CrucibleJob job) {
        if (job.warming()) return true;
        if (t >= BARREL && barrel() == null) return false;
        ItemStack there = stackAt(t);
        if (t == i) return there.getCount() == 1;
        if (there.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(there, job.result())) return false;
        if (t < BARREL) result[t] = true; // owner 17: the exact result item counts as the result stack
        return there.getCount() + job.result().getCount() <= maxAt(t, there);
    }

    private void finish(ServerLevel level, int i, CrucibleJob job) {
        int t = target[i];
        if (job.warming()) {
            ItemStack warm = items.get(i).copy();
            Warm.warm(warm, level.getGameTime());
            items.set(i, warm);
            result[i] = true;
            lastInput[i] = warm.copyWithCount(1);
        } else {
            ItemStack out = job.result().copy();
            if (Warm.warmable(out)) Warm.warm(out, level.getGameTime());
            if (t >= BARREL) {
                CrucibleBarrelBlockEntity b = barrel();
                if (b == null) return;
                ItemStack there = b.getItem(t - BARREL);
                if (there.isEmpty()) b.setItem(t - BARREL, out);
                else { there.grow(out.getCount()); b.setChanged(); }
                items.get(i).shrink(1);
            } else {
                if (t == i) {
                    items.set(i, out);
                } else {
                    ItemStack there = items.get(t);
                    if (there.isEmpty()) items.set(t, out);
                    else there.grow(out.getCount());
                    items.get(i).shrink(1);
                }
                result[t] = true;
                lastInput[t] = items.get(t).copyWithCount(1);
            }
            experience += job.experience() * (tier.doubleExperience() ? 2 : 1);
        }
        progress[i] = 0;
        target[i] = -1;
        if (items.get(i).isEmpty() || result[i]) jobs[i] = null;
    }

    private void updateGhosts() {
        Arrays.fill(ghost, ItemStack.EMPTY);
        Arrays.fill(barrelGhost, ItemStack.EMPTY);
        for (int k = 0; k < tier.slots(); k++) {
            int t = target[k];
            if (t < 0 || t == k || jobs[k] == null || !stackAt(t).isEmpty()) continue;
            if (t >= BARREL) barrelGhost[t - BARREL] = jobs[k].result();
            else ghost[t] = jobs[k].result();
        }
        for (int t = 0; t < tier.slots(); t++) {
            if (!ghost[t].isEmpty() && items.get(t).isEmpty()) state[t] = RESERVED;
        }
    }

    // ---------------------------------------------------------------- in-place upgrade

    private int upgradeStrikes;
    private long lastUpgradeStrike;

    /** Counts one upgrade strike; a pause of more than 30 s starts over. */
    public int addUpgradeStrike() {
        long now = level == null ? 0 : level.getGameTime();
        if (now - lastUpgradeStrike > 600) upgradeStrikes = 0;
        lastUpgradeStrike = now;
        return ++upgradeStrikes;
    }

    /** Everything an upgrade keeps (owner 11: contents, progress, experience). */
    public record Snapshot(List<ItemStack> items, int[] progress, int[] target, boolean[] result, float experience) {}

    public Snapshot snapshot() {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack stack : items) copy.add(stack.copy());
        return new Snapshot(copy, progress.clone(), target.clone(), result.clone(), experience);
    }

    /** Empties the old block entity so its removal drops nothing. */
    public void clearForUpgrade() {
        items.clear();
        experience = 0;
    }

    public void restore(Snapshot snapshot) {
        for (int i = 0; i < snapshot.items().size() && i < items.size(); i++) {
            items.set(i, snapshot.items().get(i));
            progress[i] = snapshot.progress()[i];
            target[i] = snapshot.target()[i] < items.size() ? snapshot.target()[i] : -1;
            result[i] = snapshot.result()[i];
            lastInput[i] = items.get(i).isEmpty() ? ItemStack.EMPTY : items.get(i).copyWithCount(1);
        }
        experience = snapshot.experience();
        heatDirty = true;
        changedAndSync();
    }

    // ---------------------------------------------------------------- experience

    /** Pays out the stored experience at the crucible (owner F20: when a player takes a result). */
    public void awardExperience(ServerLevel level, Vec3 at) {
        int whole = (int) experience;
        float fraction = experience - whole;
        if (fraction > 0 && level.getRandom().nextFloat() < fraction) whole++;
        experience = 0;
        if (whole > 0) ExperienceOrb.award(level, at, whole);
        setChanged();
    }

    // ---------------------------------------------------------------- container

    @Override
    protected NonNullList<ItemStack> getItems() { return items; }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        for (int i = 0; i < this.items.size() && i < items.size(); i++) this.items.set(i, items.get(i));
    }

    @Override
    public int getContainerSize() { return tier.slots(); }

    @Override
    public int getMaxStackSize() { return 99 * tier.stackMultiplier(); }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        int normal = stack.getMaxStackSize();
        return normal > 1 ? normal * tier.stackMultiplier() : 1;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (stack.isEmpty()) result[slot] = false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplelib." + tier.id() + "_crucible");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new CrucibleMenu(id, inventory, this);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] all = new int[tier.slots()];
        for (int i = 0; i < all.length; i++) all[i] = i;
        return all;
    }

    /** Hoppers insert from above and the sides, never into reserved/result slots, and always leave a place for results. */
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (side == Direction.DOWN || result[slot] || ghost[slot].getCount() > 0) return false;
        for (int k = 0; k < tier.slots(); k++) if (target[k] == slot) return false;
        if (!(level instanceof ServerLevel server)) return false;
        ItemStack there = items.get(slot);
        if (!there.isEmpty()) return ItemStack.isSameItemSameComponents(there, stack);
        if (CrucibleJob.find(server, stack, HeatLevel.EXTREME) == null) return false;
        int free = 0;
        for (int k = 0; k < tier.slots(); k++) if (items.get(k).isEmpty() && ghost[k].isEmpty()) free++;
        return free > 1;
    }

    /** Hoppers below take finished results only. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && result[slot];
    }

    // ---------------------------------------------------------------- removal

    /** Everything that drops when the crucible is broken: contents in normal stack sizes (owner 11). */
    public List<ItemStack> drops() {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : items) {
            ItemStack rest = stack.copy();
            while (!rest.isEmpty()) out.add(rest.split(Math.max(1, rest.getMaxStackSize())));
        }
        return out;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState blockState) {
        if (level instanceof ServerLevel server) {
            for (ItemStack stack : drops()) Block.popResource(server, pos, stack);
            awardExperience(server, Vec3.atCenterOf(pos));
        }
        items.clear();
    }

    // ---------------------------------------------------------------- persistence

    private record SavedSlot(int slot, ItemStack stack, int count) {
        static final com.mojang.serialization.Codec<SavedSlot> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                com.mojang.serialization.Codec.INT.fieldOf("Slot").forGetter(SavedSlot::slot),
                ItemStack.CODEC.fieldOf("Item").forGetter(SavedSlot::stack),
                com.mojang.serialization.Codec.INT.fieldOf("Count").forGetter(SavedSlot::count)).apply(i, SavedSlot::new));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(tier.slots(), ItemStack.EMPTY);
        input.read("Contents", SavedSlot.CODEC.listOf()).ifPresent(list -> {
            for (SavedSlot saved : list) {
                if (saved.slot() < 0 || saved.slot() >= items.size() || saved.stack().isEmpty()) continue;
                items.set(saved.slot(), saved.stack().copyWithCount(Math.max(1, Math.min(saved.count(), getMaxStackSize(saved.stack())))));
            }
        });
        int[] p = input.getIntArray("Progress").orElse(new int[0]);
        int[] t = input.getIntArray("Targets").orElse(new int[0]);
        int[] r = input.getIntArray("Results").orElse(new int[0]);
        for (int i = 0; i < tier.slots(); i++) {
            progress[i] = i < p.length ? Math.max(0, p[i]) : 0;
            target[i] = i < t.length && (t[i] >= -1 && t[i] < tier.slots() || t[i] >= BARREL && t[i] < BARREL + BarrelTier.CRUCIBLE_SLOTS) ? t[i] : -1;
            result[i] = i < r.length && r[i] != 0;
            lastInput[i] = items.get(i).copyWithCount(Math.min(1, items.get(i).getCount()));
        }
        experience = input.getFloatOr("Experience", 0.0F);
        afterglow = input.getIntOr("Afterglow", 0);
        heat = HeatLevel.byId(input.getIntOr("Heat", 0));
        heatDirty = true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        List<SavedSlot> saved = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) saved.add(new SavedSlot(i, stack.copyWithCount(Math.min(stack.getCount(), 99)), stack.getCount()));
        }
        output.store("Contents", SavedSlot.CODEC.listOf(), saved);
        output.putIntArray("Progress", progress.clone());
        output.putIntArray("Targets", target.clone());
        int[] r = new int[result.length];
        for (int i = 0; i < r.length; i++) r[i] = result[i] ? 1 : 0;
        output.putIntArray("Results", r);
        output.putFloat("Experience", experience);
        output.putInt("Afterglow", afterglow);
        output.putInt("Heat", heat.ordinal());
    }

    private void changedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
