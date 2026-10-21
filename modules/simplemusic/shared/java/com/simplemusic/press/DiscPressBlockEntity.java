package com.simplemusic.press;

import com.simplemusic.SimpleMusic;
import com.simplemusic.registry.MusicRegistry;
import com.simplemusic.sheet.SheetMusicItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The disc press's inventory and timer: slot 0 sheet of music (one, never used up), slot 1 blank discs, slot 2 the
 * pressed disc. While a pressable sheet and a blank lie in it and the output is empty, it presses for
 * {@link DiscPress#PRESS_TICKS}; changing the inputs starts over. Hoppers: from above into the sheet slot, from the sides
 * into the blank slot, out of the bottom only the pressed disc (like the furnace).
 */
public class DiscPressBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SHEET = 0, BLANK = 1, OUTPUT = 2;
    private static final int[] TOP = {SHEET}, SIDES = {BLANK}, BOTTOM = {OUTPUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int progress;
    /** Pressed discs since load (tests). */
    private int pressed;

    final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? progress : DiscPress.PRESS_TICKS;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public DiscPressBlockEntity(BlockPos pos, BlockState state) {
        super(MusicRegistry.DISC_PRESS_BE, pos, state);
    }

    public int progress() {
        return progress;
    }

    public int pressed() {
        return pressed;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DiscPressBlockEntity be) {
        if (level instanceof ServerLevel server) be.tick(server);
    }

    /** One server tick (public for tests). */
    public void tick(ServerLevel level) {
        ItemStack result = SimpleMusic.on() && items.get(OUTPUT).isEmpty()
                ? DiscPress.result(items.get(SHEET), items.get(BLANK)) : ItemStack.EMPTY;
        if (result.isEmpty()) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }
        if (progress == 0) {
            level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, 0.8F);
        }
        if (++progress < DiscPress.PRESS_TICKS) return;
        progress = 0;
        items.get(BLANK).shrink(1);
        items.set(OUTPUT, result);
        pressed++;
        level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, 1.0F);
        setChanged();
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        boolean inputChanged = slot != OUTPUT && !ItemStack.isSameItemSameComponents(stack, items.get(slot));
        super.setItem(slot, stack);
        if (inputChanged) progress = 0;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return stack.getItem() instanceof SheetMusicItem ? 1 : super.getMaxStackSize(stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SHEET -> stack.getItem() instanceof SheetMusicItem;
            case BLANK -> stack.is(MusicRegistry.BLANK_DISC);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? TOP : side == Direction.DOWN ? BOTTOM : SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot != OUTPUT && canPlaceItem(slot, stack) && (slot != SHEET || items.get(SHEET).isEmpty());
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT && side == Direction.DOWN;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.simplemusic.disc_press");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new DiscPressMenu(id, inventory, this, data);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) Containers.dropContents(level, pos, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        progress = input.getIntOr("Progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("Progress", progress);
    }
}
