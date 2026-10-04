package com.simplequalityoflife.container;

import com.simplequalityoflife.Simplequalityoflife;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Easy Shulkers / Easy Ender Chests (owner queue 2026-10-04): right-click a shulker box (any color, any
 * mod tier that extends {@link ShulkerBoxBlock}) or an ender chest in the air or on its own inventory
 * slot to open it without placing it. The shulker contents live in a detached block entity of the item's
 * own block (so mod tiers keep their own components); every change is written straight back into the
 * stack, but only while that exact stack still sits in its locked slot.
 */
public final class PortableContainers {
    private static final Map<Player, Integer> PENDING = new WeakHashMap<>();

    private PortableContainers() {
    }

    public static boolean openable(ItemStack stack, Level level) {
        var config = Simplequalityoflife.configFor(level).qOL;
        if (stack.isEmpty()) return false;
        if (stack.is(Items.ENDER_CHEST)) return config.enableEasyEnderChests;
        return config.enableEasyShulkers && stack.getCount() == 1 && stack.getItem() instanceof BlockItem item
                && item.getBlock() instanceof ShulkerBoxBlock && !stack.has(DataComponents.CONTAINER_LOOT);
    }

    /** {@code Item#use}: right-click in the air. True when the use is taken (both sides). */
    public static boolean onUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSpectator() || !openable(stack, level)) return false;
        if (!level.isClientSide()) PENDING.put(player, hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND);
        return true;
    }

    /** {@code AbstractContainerMenu#clicked}: right-click (empty cursor) on the item in the player's own inventory. */
    public static boolean interceptClick(AbstractContainerMenu menu, int slotIndex, int button, ContainerInput input, Player player) {
        if (input != ContainerInput.PICKUP || button != 1 || slotIndex < 0 || slotIndex >= menu.slots.size() || !menu.getCarried().isEmpty()) return false;
        Slot slot = menu.slots.get(slotIndex);
        if (slot.container != player.getInventory() || !openable(slot.getItem(), player.level())) return false;
        if (menu instanceof PortableMenu portable && portable.lockedSlot() == slot.getContainerSlot()) return true;
        if (!player.level().isClientSide()) PENDING.put(player, slot.getContainerSlot());
        return true;
    }

    /** Server player tick: open what a click or use asked for (never inside the click handler itself). */
    public static void tick(ServerPlayer player) {
        Integer slot = PENDING.remove(player);
        if (slot != null) open(player, slot);
    }

    /** Opens the shulker box or ender chest in inventory slot {@code slot}; false when it cannot be opened. */
    public static boolean open(ServerPlayer player, int slot) {
        if (player.isSpectator() || !player.isAlive()) return false;
        ItemStack stack = player.getInventory().getItem(slot);
        if (!openable(stack, player.level())) return false;
        if (stack.is(Items.ENDER_CHEST)) {
            PlayerEnderChestContainer ender = player.getEnderChestInventory();
            if (player.containerMenu != player.inventoryMenu) player.closeContainer();
            ender.setActiveChest(null);
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PortableMenu(MenuType.GENERIC_9x3, id, inventory, ender, 3, slot, stack,
                    () -> sound(player, false, true)), Component.translatable("container.enderchest")));
            sound(player, true, true);
            return true;
        }
        Block block = ((BlockItem) stack.getItem()).getBlock();
        if (!(block instanceof EntityBlock entityBlock)) return false;
        BlockEntity be = entityBlock.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
        if (!(be instanceof Container contents)) return false;
        int size = contents.getContainerSize();
        if (size < 9 || size > 54 || size % 9 != 0) return false;
        be.applyComponentsFromItemStack(stack);
        int rows = size / 9;
        boolean vanillaBox = rows == 3 && block.getClass() == ShulkerBoxBlock.class;
        MenuType<?> type = vanillaBox ? MenuType.SHULKER_BOX : switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
        BooleanSupplier holds = () -> player.getInventory().getItem(slot) == stack && openable(stack, player.level());
        Container backed = new ItemBackedContainer(be, contents, stack, holds);
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PortableMenu(type, id, inventory, backed, rows, slot, stack,
                () -> sound(player, false, false)), stack.getHoverName()));
        sound(player, true, false);
        return true;
    }

    private static void sound(ServerPlayer player, boolean open, boolean ender) {
        var event = ender ? (open ? SoundEvents.ENDER_CHEST_OPEN : SoundEvents.ENDER_CHEST_CLOSE) : (open ? SoundEvents.SHULKER_BOX_OPEN : SoundEvents.SHULKER_BOX_CLOSE);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), event, SoundSource.PLAYERS, 0.5f, 1.0f);
    }

    /**
     * The detached block entity's container, written back into the item after every change. Opening and
     * closing never reach the block entity (it has no level); the menu decides validity.
     */
    static final class ItemBackedContainer implements Container {
        private final BlockEntity be;
        private final Container contents;
        private final ItemStack stack;
        private final BooleanSupplier holds;

        ItemBackedContainer(BlockEntity be, Container contents, ItemStack stack, BooleanSupplier holds) {
            this.be = be;
            this.contents = contents;
            this.stack = stack;
            this.holds = holds;
        }

        private void write() {
            if (this.holds.getAsBoolean()) this.stack.applyComponents(this.be.collectComponents());
        }

        @Override
        public int getContainerSize() {
            return this.contents.getContainerSize();
        }

        @Override
        public boolean isEmpty() {
            return this.contents.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return this.contents.getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            ItemStack out = this.contents.removeItem(slot, count);
            this.write();
            return out;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack out = this.contents.removeItemNoUpdate(slot);
            this.write();
            return out;
        }

        @Override
        public void setItem(int slot, ItemStack item) {
            this.contents.setItem(slot, item);
            this.write();
        }

        @Override
        public int getMaxStackSize() {
            return this.contents.getMaxStackSize();
        }

        @Override
        public int getMaxStackSize(ItemStack item) {
            return this.contents.getMaxStackSize(item);
        }

        @Override
        public void setChanged() {
            this.write();
        }

        @Override
        public boolean stillValid(Player player) {
            return this.holds.getAsBoolean();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack item) {
            return item.getItem().canFitInsideContainerItems() && this.contents.canPlaceItem(slot, item);
        }

        @Override
        public void clearContent() {
            this.contents.clearContent();
            this.write();
        }
    }
}
