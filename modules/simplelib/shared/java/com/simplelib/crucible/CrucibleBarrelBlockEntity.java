package com.simplelib.crucible;

import com.simplelib.registry.LibBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Contents of a copper barrel; on its own it opens as a chest of its tier's size (owner 58). Attached
 * to a crucible it offers only as many slots as the crucible has (owner N12c: 6/9/18/27). The slots beyond are
 * kept, not dropped: nothing reaches them while attached (hoppers, the crucible and its menu only see the first
 * ones), they come back when the barrel is detached, and they drop when it is broken. Detaching keeps the contents
 * within the normal stack limits.
 */
public class CrucibleBarrelBlockEntity extends BaseContainerBlockEntity {
    private final BarrelTier tier;
    private NonNullList<ItemStack> items;
    private int attachStrikes;
    private long lastStrike;

    public CrucibleBarrelBlockEntity(BlockPos pos, BlockState state) {
        this(LibBlockEntities.BARREL, pos, state);
    }

    public CrucibleBarrelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.tier = state.getBlock() instanceof CrucibleBarrelBlock block ? block.tier() : BarrelTier.COPPER;
        this.items = NonNullList.withSize(tier.slots(), ItemStack.EMPTY);
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

    /** Slots offered while attached: the crucible's slot count (6 if it cannot be found, e.g. while loading). */
    public int attachedSlots() {
        int slots = CrucibleTier.IRON.slots();
        BlockState state = getBlockState();
        if (level != null && state.hasProperty(CrucibleBarrelBlock.FACING)
                && level.getBlockEntity(worldPosition.relative(state.getValue(CrucibleBarrelBlock.FACING))) instanceof CrucibleBlockEntity crucible) {
            slots = crucible.tier().slots();
        }
        return Math.min(slots, tier.slots());
    }

    /** Just attached: the slots beyond the crucible's count stay stored (hidden), see the class comment. */
    public void onAttached() {
        if (level == null || level.isClientSide()) return;
        setChanged();
    }

    /** Just detached: the barrel is normal again; stacks above the normal limit drop their surplus. */
    public void onDetached() {
        if (level == null || level.isClientSide()) return;
        for (int i = 0; i < items.size(); i++) {
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
    public int getMaxStackSize() { return 99 * tier.stackMultiplier(); }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        int normal = stack.getMaxStackSize();
        return normal > 1 ? normal * tier.stackMultiplier() : 1;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplelib." + tier.id() + "_barrel");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        MenuType<ChestMenu> type = switch (tier.rows()) {
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            case 6 -> MenuType.GENERIC_9x6;
            default -> MenuType.GENERIC_9x3;
        };
        return new ChestMenu(type, id, inventory, this, tier.rows());
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
        items = NonNullList.withSize(tier.slots(), ItemStack.EMPTY);
        input.read("Contents", SavedSlot.CODEC.listOf()).ifPresent(list -> {
            for (SavedSlot saved : list) {
                if (saved.slot() < 0 || saved.slot() >= items.size() || saved.stack().isEmpty()) continue;
                items.set(saved.slot(), saved.stack().copyWithCount(Math.max(1, Math.min(saved.count(), getMaxStackSize(saved.stack())))));
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
