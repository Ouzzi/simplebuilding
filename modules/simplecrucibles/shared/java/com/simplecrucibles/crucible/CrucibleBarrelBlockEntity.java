package com.simplecrucibles.crucible;

import com.simplecrucibles.registry.CrucibleBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Contents of a copper barrel; on its own it opens as a chest of its tier's size (owner 58). Attached to a crucible it
 * always has exactly as many slots as the crucible (owner N12c: 6/12/18/30; N37: also when that is more than the barrel
 * holds on its own - a copper barrel of 27 gets the Enderite crucible's 30). Slots beyond the crucible's count are kept,
 * not dropped: nothing reaches them while attached (hoppers, the crucible and its menu only see the first ones), they
 * come back when the barrel is detached, and they drop when it is broken. Detaching moves what lies in the extra slots
 * beyond the barrel's own size into its free slots and drops only what does not fit (nothing is lost), and keeps the
 * contents within the normal stack limits.
 */
public class CrucibleBarrelBlockEntity extends BaseContainerBlockEntity {
    private final BarrelTier tier;
    private NonNullList<ItemStack> items;
    private int attachStrikes;
    private long lastStrike;
    private int upgradeStrikes; // own counter: attaching and upgrading with the axe must not feed each other
    private long lastUpgradeStrike;

    public CrucibleBarrelBlockEntity(BlockPos pos, BlockState state) {
        this(CrucibleBlockEntities.BARREL, pos, state);
    }

    public CrucibleBarrelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.tier = state.getBlock() instanceof CrucibleBarrelBlock block ? block.tier() : BarrelTier.COPPER;
        this.items = NonNullList.withSize(capacity(), ItemStack.EMPTY);
    }

    /** Stored slots: the barrel's own, or the largest crucible's if that has more (N37). */
    private int capacity() {
        return Math.max(tier.slots(), BarrelTier.MAX_CRUCIBLE_SLOTS);
    }

    public BarrelTier tier() {
        return tier;
    }

    public int addAttachStrike() {
        long now = level == null ? 0 : level.getGameTime();
        if (now - lastStrike > 600) attachStrikes = 0;
        lastStrike = now;
        return ++attachStrikes;
    }

    public int addUpgradeStrike() {
        long now = level == null ? 0 : level.getGameTime();
        if (now - lastUpgradeStrike > 600) upgradeStrikes = 0;
        lastUpgradeStrike = now;
        return ++upgradeStrikes;
    }

    @Override
    protected NonNullList<ItemStack> getItems() { return items; }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        for (int i = 0; i < items.size() && i < list.size(); i++) items.set(i, list.get(i));
    }

    /** Whether this barrel is attached to a crucible (then the crucible's slot count and the crucible's menu). */
    public boolean attached() {
        BlockState state = getBlockState();
        return state.hasProperty(CrucibleBarrelBlock.ATTACHED) && state.getValue(CrucibleBarrelBlock.ATTACHED);
    }

    @Override
    public int getContainerSize() { return attached() ? attachedSlots() : tier.slots(); }

    /** Slots offered while attached: the crucible's slot count, even above the barrel's own (6 if it cannot be found, e.g. while loading). */
    public int attachedSlots() {
        int slots = CrucibleTier.IRON.slots();
        BlockState state = getBlockState();
        if (level != null && state.hasProperty(CrucibleBarrelBlock.FACING)
                && level.getBlockEntity(worldPosition.relative(state.getValue(CrucibleBarrelBlock.FACING))) instanceof CrucibleBlockEntity crucible) {
            slots = crucible.tier().slots();
        }
        return Math.min(slots, items.size());
    }

    /** Just attached: the slots beyond the crucible's count stay stored (hidden), see the class comment. */
    public void onAttached() {
        if (level == null || level.isClientSide()) return;
        setChanged();
    }

    /**
     * Just detached: the barrel is normal again. What lies in slots beyond its own size (only filled by a bigger
     * crucible, N37) moves into its free slots, the rest drops; stacks above the normal limit drop their surplus.
     */
    public void onDetached() {
        if (level == null || level.isClientSide()) return;
        for (int i = tier.slots(); i < items.size(); i++) {
            ItemStack extra = items.get(i);
            if (extra.isEmpty()) continue;
            items.set(i, ItemStack.EMPTY);
            for (int k = 0; k < tier.slots() && !extra.isEmpty(); k++) {
                ItemStack there = items.get(k);
                if (there.isEmpty()) {
                    items.set(k, extra);
                    extra = ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(there, extra)) {
                    int move = Math.min(extra.getCount(), getMaxStackSize(there) - there.getCount());
                    if (move > 0) {
                        there.grow(move);
                        extra.shrink(move);
                    }
                }
            }
            if (!extra.isEmpty()) popAll(extra);
        }
        for (int i = 0; i < tier.slots(); i++) {
            ItemStack stack = items.get(i);
            int max = getMaxStackSize(stack);
            if (stack.getCount() > max) popAll(stack.split(stack.getCount() - max));
        }
        setChanged();
    }

    private void popAll(ItemStack stack) {
        while (!stack.isEmpty()) Block.popResource(level, worldPosition, stack.split(Math.max(1, stack.getMaxStackSize())));
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return !attached() && super.stillValid(player);
    }

    @Override
    public int getMaxStackSize() { return com.simplelib.api.StackLimits.cap(stackMultiplier()); }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return com.simplelib.api.StackLimits.max(stack, stackMultiplier());
    }

    /** Owner N15: attached, the barrel holds at least as much per slot as its crucible. */
    public int stackMultiplier() {
        int own = tier.stackMultiplier();
        if (!attached() || level == null) return own;
        return level.getBlockEntity(worldPosition.relative(getBlockState().getValue(CrucibleBarrelBlock.FACING))) instanceof CrucibleBlockEntity crucible
                ? Math.max(own, crucible.tier().stackMultiplier()) : own;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplecrucibles." + tier.id() + "_barrel");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new BarrelMenu(tier, id, inventory, this);
    }

    public List<ItemStack> drops() {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : items) {
            ItemStack rest = stack.copy();
            while (!rest.isEmpty()) out.add(rest.split(Math.max(1, rest.getMaxStackSize())));
        }
        return out;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) for (ItemStack stack : drops()) Block.popResource(level, pos, stack);
        items.clear();
    }

    private record SavedSlot(int slot, ItemStack stack, int count) {
        static final com.mojang.serialization.Codec<SavedSlot> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                com.mojang.serialization.Codec.INT.fieldOf("Slot").forGetter(SavedSlot::slot),
                ItemStack.CODEC.fieldOf("Item").forGetter(SavedSlot::stack),
                com.mojang.serialization.Codec.INT.fieldOf("Count").forGetter(SavedSlot::count)).apply(i, SavedSlot::new));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(capacity(), ItemStack.EMPTY);
        input.read("Contents", SavedSlot.CODEC.listOf()).ifPresent(list -> {
            for (SavedSlot saved : list) {
                if (saved.slot() < 0 || saved.slot() >= items.size() || saved.stack().isEmpty()) continue;
                // Loading happens before the crucible beside it is known: allow the largest limit any crucible gives.
                int most = com.simplelib.api.StackLimits.max(saved.stack(), Math.max(tier.stackMultiplier(), CrucibleTier.ENDERITE.stackMultiplier()));
                items.set(saved.slot(), saved.stack().copyWithCount(Math.max(1, Math.min(saved.count(), most))));
            }
        });
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
    }
}
