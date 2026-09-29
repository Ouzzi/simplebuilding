package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.blocks.custom.TieredShulkerBoxBlock;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.component.ContainerCounts;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.custom.BackpackItem;
import com.simplebuilding.screen.TieredChestMenu;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The block entity of the Reinforced, Netherite and Enderite Shulker Boxes (one type for all three
 * tiers). Vanilla's {@code ShulkerBoxBlockEntity} in everything the world sees - lid animation,
 * pushing entities out of the way, sounds, game events, sided hopper access that refuses shulker
 * boxes - with the slot count and stack factor of its {@link ChestTier} (36/45/54 slots, stacks
 * x1/x2/x4) and a dye color of its own.
 *
 * <p>Vanilla's class cannot be extended: its constructors hard-wire vanilla's block entity type, and
 * a block entity saved under that type would load back as a plain 27-slot shulker box.
 *
 * <p><b>Color.</b> One block per tier, the color lives here and on the item as
 * {@code minecraft:base_color} (null = undyed). It is saved under {@value #COLOR_TAG}.
 *
 * <p><b>Oversized stacks.</b> Vanilla stores at most a normal stack per slot (the item codec 99,
 * {@code minecraft:container} the item's own stack size). Saving and collecting components (the item
 * that drops when the box is broken) therefore see a capped copy; the real counts go to {@value TieredChestBlockEntity#EXTRA_COUNTS} in the block
 * entity tag and to {@code simplebuilding:container_counts} on the item ({@link ContainerCounts}),
 * and are put back after loading or placing.
 */
public class TieredShulkerBoxBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    public static final String COLOR_TAG = "simplebuilding:color";
    public static final String EXTRA_COUNTS = TieredChestBlockEntity.EXTRA_COUNTS;
    private static final int EVENT_SET_OPEN_COUNT = 1;

    private final ChestTier tier;
    private final int[] slotsForFace;
    private NonNullList<ItemStack> items;
    private int openCount;
    private ShulkerBoxBlockEntity.AnimationStatus animationStatus = ShulkerBoxBlockEntity.AnimationStatus.CLOSED;
    private float progress;
    private float progressOld;
    private @Nullable DyeColor color;

    public TieredShulkerBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIERED_SHULKER_BOX_BE, pos, state);
        this.tier = state.getBlock() instanceof TieredShulkerBoxBlock box ? box.tier() : ChestTier.REINFORCED;
        this.items = NonNullList.withSize(this.tier.slots(), ItemStack.EMPTY);
        this.slotsForFace = IntStream.range(0, this.tier.slots()).toArray();
    }

    public ChestTier tier() {
        return this.tier;
    }

    public @Nullable DyeColor getColor() {
        return this.color;
    }

    public void setColor(@Nullable DyeColor color) {
        this.color = color;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    // =====================================================================================
    // ANIMATION (vanilla's, line for line)
    // =====================================================================================

    public static void tick(Level level, BlockPos pos, BlockState state, TieredShulkerBoxBlockEntity entity) {
        entity.updateAnimation(level, pos, state);
    }

    private void updateAnimation(Level level, BlockPos pos, BlockState state) {
        this.progressOld = this.progress;
        switch (this.animationStatus) {
            case CLOSED -> this.progress = 0.0F;
            case OPENING -> {
                this.progress += 0.1F;
                if (this.progressOld == 0.0F) {
                    doNeighborUpdates(level, pos, state);
                }
                if (this.progress >= 1.0F) {
                    this.animationStatus = ShulkerBoxBlockEntity.AnimationStatus.OPENED;
                    this.progress = 1.0F;
                    doNeighborUpdates(level, pos, state);
                }
                this.moveCollidedEntities(level, pos, state);
            }
            case OPENED -> this.progress = 1.0F;
            case CLOSING -> {
                this.progress -= 0.1F;
                if (this.progressOld == 1.0F) {
                    doNeighborUpdates(level, pos, state);
                }
                if (this.progress <= 0.0F) {
                    this.animationStatus = ShulkerBoxBlockEntity.AnimationStatus.CLOSED;
                    this.progress = 0.0F;
                    doNeighborUpdates(level, pos, state);
                }
            }
        }
    }

    public ShulkerBoxBlockEntity.AnimationStatus getAnimationStatus() {
        return this.animationStatus;
    }

    public boolean isClosed() {
        return this.animationStatus == ShulkerBoxBlockEntity.AnimationStatus.CLOSED;
    }

    public float getProgress(float partialTick) {
        return Mth.lerp(partialTick, this.progressOld, this.progress);
    }

    public AABB getBoundingBox(BlockState state) {
        return Shulker.getProgressAabb(1.0F, state.getValue(ShulkerBoxBlock.FACING), 0.5F * this.getProgress(1.0F), new Vec3(0.5, 0.0, 0.5));
    }

    private void moveCollidedEntities(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof ShulkerBoxBlock)) {
            return;
        }
        Direction direction = state.getValue(ShulkerBoxBlock.FACING);
        AABB aabb = Shulker.getProgressDeltaAabb(1.0F, direction, this.progressOld, this.progress, Vec3.atBottomCenterOf(pos));
        List<Entity> entities = level.getEntities(null, aabb);
        for (Entity entity : entities) {
            if (entity.getPistonPushReaction() != com.simplebuilding.version.McVersion.PUSH_IGNORED) {
                entity.move(MoverType.SHULKER_BOX, new Vec3(
                        (aabb.getXsize() + 0.01) * direction.getStepX(),
                        (aabb.getYsize() + 0.01) * direction.getStepY(),
                        (aabb.getZsize() + 0.01) * direction.getStepZ()));
            }
        }
    }

    private static void doNeighborUpdates(Level level, BlockPos pos, BlockState state) {
        state.updateNeighbourShapes(level, pos, 3);
        level.updateNeighborsAt(pos, state.getBlock());
    }

    @Override
    public boolean triggerEvent(int type, int value) {
        if (type == EVENT_SET_OPEN_COUNT) {
            this.openCount = value;
            if (value == 0) {
                this.animationStatus = ShulkerBoxBlockEntity.AnimationStatus.CLOSING;
            }
            if (value == 1) {
                this.animationStatus = ShulkerBoxBlockEntity.AnimationStatus.OPENING;
            }
            return true;
        }
        return super.triggerEvent(type, value);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // Like vanilla's shulker box: the contents leave with the item, nothing spills.
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (!this.remove && !user.getLivingEntity().isSpectator() && this.level != null) {
            if (this.openCount < 0) {
                this.openCount = 0;
            }
            this.openCount++;
            this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), EVENT_SET_OPEN_COUNT, this.openCount);
            if (this.openCount == 1) {
                this.level.gameEvent(user.getLivingEntity(), GameEvent.CONTAINER_OPEN, this.worldPosition);
                this.level.playSound(null, this.worldPosition, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.5F,
                        this.level.getRandom().nextFloat() * 0.1F + 0.9F);
            }
        }
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!this.remove && !user.getLivingEntity().isSpectator() && this.level != null) {
            this.openCount--;
            this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), EVENT_SET_OPEN_COUNT, this.openCount);
            if (this.openCount <= 0) {
                this.level.gameEvent(user.getLivingEntity(), GameEvent.CONTAINER_CLOSE, this.worldPosition);
                this.level.playSound(null, this.worldPosition, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.5F,
                        this.level.getRandom().nextFloat() * 0.1F + 0.9F);
            }
        }
    }

    public int openCount() {
        return this.openCount;
    }

    // =====================================================================================
    // SLOTS AND STACKS
    // =====================================================================================

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    /** All stacks (for the upgrade, the tooltip and the tests). */
    public List<ItemStack> itemsView() {
        return java.util.Collections.unmodifiableList(this.items);
    }

    /** Vanilla's container limit (99) times the tier factor, like the tier chests. */
    @Override
    public int getMaxStackSize() {
        return 99 * this.tier.stackMultiplier();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return BackpackItem.maxStackSizeIn(stack, this.tier.stackMultiplier());
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return this.slotsForFace;
    }

    /** Like vanilla's shulker box: nothing that is not allowed inside a container item (no shulker box, of any tier). */
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return canHold(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return canHold(stack);
    }

    /** Whether a shulker box (vanilla or tiered) may hold {@code stack}: vanilla's own rule. */
    public static boolean canHold(ItemStack stack) {
        return !(net.minecraft.world.level.block.Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock)
                && stack.getItem().canFitInsideContainerItems();
    }

    // =====================================================================================
    // SAVING AND COMPONENTS
    // =====================================================================================

    @Override
    protected Component getDefaultName() {
        return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(input)) {
            ContainerHelper.loadAllItems(input, this.items);
            input.read(EXTRA_COUNTS, ContainerCounts.CODEC).ifPresent(counts -> counts.applyTo(this.items));
        }
        this.color = input.read(COLOR_TAG, DyeColor.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.trySaveLootTable(output)) {
            ContainerHelper.saveAllItems(output, ContainerCounts.codecSafeCopy(this.items), false);
            ContainerCounts counts = ContainerCounts.of(this.items);
            if (counts != null) {
                output.store(EXTRA_COUNTS, ContainerCounts.CODEC, counts);
            }
        }
        if (this.color != null) {
            output.store(COLOR_TAG, DyeColor.CODEC, this.color);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        ContainerCounts counts = components.get(ModDataComponentTypes.CONTAINER_COUNTS);
        if (counts != null) {
            counts.applyTo(this.items);
        }
        this.color = components.get(DataComponents.BASE_COLOR);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        NonNullList<ItemStack> real = this.items;
        this.items = ContainerCounts.codecSafeCopy(real);
        try {
            super.collectImplicitComponents(components);
        } finally {
            this.items = real;
        }
        ContainerCounts counts = ContainerCounts.of(real);
        if (counts != null) {
            components.set(ModDataComponentTypes.CONTAINER_COUNTS, counts);
        }
        if (this.color != null) {
            components.set(DataComponents.BASE_COLOR, this.color);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard(EXTRA_COUNTS);
        output.discard(COLOR_TAG);
    }

    /** The client needs the color (renderer); the contents stay on the server like vanilla's. */
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        if (this.color != null) {
            tag.putString(COLOR_TAG, this.color.getSerializedName());
        }
        return tag;
    }

    /**
     * The upgrade: the contents of the old box, slot for slot. The new tier has at least as many
     * slots and as large stacks; whatever still does not fit drops instead of vanishing.
     */
    public void receiveUpgradedContents(List<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (i < this.items.size() && this.items.get(i).isEmpty() && stack.getCount() <= getMaxStackSize(stack)) {
                this.items.set(i, stack);
            } else if (this.level != null) {
                net.minecraft.world.level.block.Block.popResource(this.level, this.worldPosition, stack);
            }
        }
        this.setChanged();
    }

    // =====================================================================================
    // MENU
    // =====================================================================================

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return TieredChestMenu.server(containerId, inventory, this, this.tier, false, true);
    }
}
