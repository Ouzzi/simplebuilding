package com.simpleenchanting.block;

import com.simpleenchanting.EnchanterStage;
import com.simpleenchanting.EnchantingRegistry;
import com.simpleenchanting.menu.EnchanterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Dark and Astral Enchanter (one block entity type {@code simpleenchanting:enchanter}, until 03.11. SimpleBuilding's
 * {@code astral_enchanting_table}): keeps lapis lazuli (slot {@value #LAPIS}) and blaze powder (slot {@value #BLAZE}), one stack
 * each, while nobody uses it - they stay in the block when the menu closes and drop when it is broken (Vanilla's
 * {@code preRemoveSideEffects} for containers). Hoppers may top both up from any side but never pull them out.
 * On the client it turns and flips the floating book like Vanilla's table ({@link #bookAnimationTick}).
 */
public class EnchanterBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int LAPIS = 0;
    public static final int BLAZE = 1;
    public static final int SIZE = 2;
    private static final int[] SLOTS = {LAPIS, BLAZE};
    private static final RandomSource RANDOM = RandomSource.create();

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    // Book animation (client), the fields of Vanilla's EnchantingTableBlockEntity.
    public int time;
    public float flip;
    public float oFlip;
    public float flipT;
    public float flipA;
    public float open;
    public float oOpen;
    public float rot;
    public float oRot;
    public float tRot;

    public EnchanterBlockEntity(BlockPos pos, BlockState state) {
        super(EnchantingRegistry.ENCHANTER_BE, pos, state);
    }

    /** The stage of the block this entity sits in (Dark when the block is something else). */
    public EnchanterStage stage() {
        return getBlockState().getBlock() instanceof EnchanterBlock block ? block.stage() : EnchanterStage.DARK;
    }

    /** Which stored item a slot takes. */
    public static boolean fits(int slot, ItemStack stack) {
        return slot == LAPIS ? stack.is(Items.LAPIS_LAZULI) : slot == BLAZE && stack.is(Items.BLAZE_POWDER);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return fits(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return fits(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simpleenchanting." + stage().path);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new EnchanterMenu(containerId, inventory, this, ContainerLevelAccess.create(this.level, this.worldPosition));
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
    }

    /** Vanilla's book animation: opens and turns towards a player within three blocks, flips pages now and then. */
    public static void bookAnimationTick(Level level, BlockPos pos, BlockState state, EnchanterBlockEntity entity) {
        entity.oOpen = entity.open;
        entity.oRot = entity.rot;
        Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3.0, false);
        if (player != null) {
            double xd = player.getX() - (pos.getX() + 0.5);
            double zd = player.getZ() - (pos.getZ() + 0.5);
            entity.tRot = (float) Mth.atan2(zd, xd);
            entity.open += 0.1F;
            if (entity.open < 0.5F || RANDOM.nextInt(40) == 0) {
                float old = entity.flipT;
                do {
                    entity.flipT = entity.flipT + (RANDOM.nextInt(4) - RANDOM.nextInt(4));
                } while (old == entity.flipT);
            }
        } else {
            entity.tRot += 0.02F;
            entity.open -= 0.1F;
        }
        while (entity.rot >= (float) Math.PI) entity.rot -= (float) (Math.PI * 2);
        while (entity.rot < (float) -Math.PI) entity.rot += (float) (Math.PI * 2);
        while (entity.tRot >= (float) Math.PI) entity.tRot -= (float) (Math.PI * 2);
        while (entity.tRot < (float) -Math.PI) entity.tRot += (float) (Math.PI * 2);
        float rotDir = entity.tRot - entity.rot;
        while (rotDir >= (float) Math.PI) rotDir -= (float) (Math.PI * 2);
        while (rotDir < (float) -Math.PI) rotDir += (float) (Math.PI * 2);
        entity.rot += rotDir * 0.4F;
        entity.open = Mth.clamp(entity.open, 0.0F, 1.0F);
        entity.time++;
        entity.oFlip = entity.flip;
        float diff = (entity.flipT - entity.flip) * 0.4F;
        diff = Mth.clamp(diff, -0.2F, 0.2F);
        entity.flipA = entity.flipA + (diff - entity.flipA) * 0.9F;
        entity.flip = entity.flip + entity.flipA;
    }
}
