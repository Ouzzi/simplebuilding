package com.simplequalityoflife.container;

import com.simplequalityoflife.Simplequalityoflife;
import com.simplequalityoflife.event.InteractionGuard;
import com.simplequalityoflife.network.LinkedOpenPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Linked GUIs (owner queue 2026-10-04): sneak + right-click a container with an empty main hand marks
 * it; the next menu the player opens gets the marked container's slots appended (shown above it on the
 * client). Server-authoritative: the mark lives on the player object (gone on logout or respawn) and is
 * checked every tick (range, dimension, loaded chunk, block entity); every slot access checks it again.
 */
public final class LinkedContainers {
    /** Most slots a marked container may show (a double chest of the widest mod chests: 12 rows of 9). */
    public static final int MAX_SLOTS = 108;
    private static final Map<Player, Mark> MARKS = new WeakHashMap<>();
    /** Sends the payload when the client can receive it; set per loader. Without it nothing is appended. */
    public static BiPredicate<ServerPlayer, LinkedOpenPayload> openSync = (player, payload) -> false;

    private LinkedContainers() {
    }

    public record Mark(ResourceKey<Level> dimension, BlockPos pos) {
    }

    /** One linked menu: the container, the block entities behind it and where its slots start. */
    public static final class Session {
        public final ServerPlayer player;
        public final Mark mark;
        public final Container container;
        public final List<BlockEntity> backing;
        public final int start;
        public final int size;

        Session(ServerPlayer player, Mark mark, Container container, List<BlockEntity> backing, int start, int size) {
            this.player = player;
            this.mark = mark;
            this.container = container;
            this.backing = backing;
            this.start = start;
            this.size = size;
        }

        public boolean valid() {
            if (!LinkedContainers.valid(this.player, this.mark)) return false;
            for (BlockEntity be : this.backing) {
                if (be.isRemoved() || this.player.level().getBlockEntity(be.getBlockPos()) != be) return false;
            }
            return true;
        }
    }

    public static @Nullable Mark mark(Player player) {
        return MARKS.get(player);
    }

    public static void clear(Player player) {
        MARKS.remove(player);
    }

    // --- marking ---

    /** Use hook of every loader: sneak + right-click with an empty main hand on a container. */
    public static InteractionResult onRightClickBlock(Player player, InteractionHand hand, BlockPos pos) {
        Level level = player.level();
        if (hand != InteractionHand.MAIN_HAND || !Simplequalityoflife.configFor(level).qOL.enableLinkedContainers) return InteractionResult.PASS;
        if (!player.isSecondaryUseActive() || !player.getMainHandItem().isEmpty() || player.isSpectator()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof BaseContainerBlockEntity be)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Mark current = MARKS.get(player);
        if (current != null && current.dimension().equals(level.dimension()) && backingPositions(level, current.pos()).contains(pos)) {
            MARKS.remove(player);
            feedback(level, pos, false);
            return InteractionResult.SUCCESS;
        }
        if (!InteractionGuard.allow(player, pos) || !InteractionGuard.permission.test(player, pos) || !be.canOpen(player)) return InteractionResult.PASS;
        Container container = container(level, pos);
        if (container == null || container.getContainerSize() <= 0 || container.getContainerSize() > MAX_SLOTS) return InteractionResult.PASS;
        MARKS.put(player, new Mark(level.dimension(), pos.immutable()));
        feedback(level, pos, true);
        return InteractionResult.SUCCESS;
    }

    private static void feedback(Level level, BlockPos pos, boolean marked) {
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, marked ? 1.5f : 0.7f);
    }

    /** The whole container behind {@code pos}: both halves of a double chest, else the block entity. */
    public static @Nullable Container container(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chest) {
            Container combined = ChestBlock.getContainer(chest, state, level, pos, true);
            if (combined != null) return combined;
        }
        return level.getBlockEntity(pos) instanceof BaseContainerBlockEntity be ? be : null;
    }

    private static List<BlockEntity> backing(Level level, BlockPos pos) {
        List<BlockEntity> out = new ArrayList<>();
        for (BlockPos p : backingPositions(level, pos)) {
            BlockEntity be = level.getBlockEntity(p);
            if (be != null) out.add(be);
        }
        return out;
    }

    private static List<BlockPos> backingPositions(Level level, BlockPos pos) {
        List<BlockPos> out = new ArrayList<>(List.of(pos));
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            var direction = ChestBlock.getConnectedDirection(state);
            BlockPos other = pos.relative(direction);
            BlockState otherState = level.getBlockState(other);
            if (otherState.is(state.getBlock()) && otherState.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                    && ChestBlock.getConnectedDirection(otherState) == direction.getOpposite()) {
                out.add(other);
            }
        }
        return out;
    }

    /** Whether {@code mark} still holds for {@code player}: switch, range, dimension, chunk, container. */
    public static boolean valid(ServerPlayer player, @Nullable Mark mark) {
        if (mark == null || player.isRemoved() || player.isSpectator()) return false;
        var config = Simplequalityoflife.getConfig().qOL;
        if (!config.enableLinkedContainers || !player.level().dimension().equals(mark.dimension())) return false;
        Level level = player.level();
        if (!level.isLoaded(mark.pos())) return false;
        double range = config.linkedContainerRange;
        if (player.distanceToSqr(Vec3.atCenterOf(mark.pos())) > range * range) return false;
        return level.getBlockEntity(mark.pos()) instanceof BaseContainerBlockEntity be && !be.isRemoved();
    }

    // --- server tick ---

    /** Start of every server player tick: end invalid marks, close broken linked menus, hand particles. */
    public static void tick(ServerPlayer player) {
        Mark mark = MARKS.get(player);
        if (mark != null && !valid(player, mark)) {
            MARKS.remove(player);
            mark = null;
        }
        Session session = ((LinkedMenu) player.containerMenu).qol$session();
        if (session != null && !session.valid()) player.closeContainer();
        if (mark != null && player.tickCount % 10 == 0) handParticle(player);
    }

    private static void handParticle(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        double yaw = Math.toRadians(player.getYRot());
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        if (player.getMainArm() == HumanoidArm.LEFT) right = right.scale(-1);
        Vec3 at = player.getEyePosition().add(look.scale(0.5)).add(right.scale(0.32)).add(0, -0.38, 0);
        ((ServerLevel) player.level()).sendParticles(player, new DustParticleOptions(0xB38EF3, 0.5f), false, false,
                at.x, at.y, at.z, 1, 0.04, 0.04, 0.04, 0.0);
    }

    // --- appending to the second menu ---

    /** {@code ServerPlayer#initMenu}: before the menu's first sync, append the marked container's slots. */
    public static void onInitMenu(ServerPlayer player, AbstractContainerMenu menu) {
        if (menu == player.inventoryMenu || menu instanceof PortableMenu || hasLinked(menu)) return;
        Mark mark = MARKS.get(player);
        if (mark == null) return;
        if (!valid(player, mark)) {
            MARKS.remove(player);
            return;
        }
        Level level = player.level();
        Container container = container(level, mark.pos());
        if (container == null) return;
        int size = container.getContainerSize();
        if (size <= 0 || size > MAX_SLOTS) return;
        List<BlockEntity> backing = backing(level, mark.pos());
        for (Slot slot : menu.slots) {
            if (shows(slot.container, container, backing)) return;
        }
        if (!InteractionGuard.permission.test(player, mark.pos())) return;
        Component title = level.getBlockEntity(mark.pos()) instanceof BaseContainerBlockEntity be ? be.getDisplayName() : Component.empty();
        if (!openSync.test(player, new LinkedOpenPayload(menu.containerId, size, title))) return;
        Session session = new Session(player, mark, container, backing, menu.slots.size(), size);
        for (int i = 0; i < size; i++) ((LinkedMenu) menu).qol$addSlot(new LinkedSlot(container, i, session));
        ((LinkedMenu) menu).qol$session(session);
    }

    /** Whether a slot of the second menu already shows the marked container (it is the marked chest itself). */
    private static boolean shows(Container slotContainer, Container marked, List<BlockEntity> backing) {
        if (slotContainer == marked) return true;
        for (BlockEntity be : backing) {
            if (slotContainer == be) return true;
            if (slotContainer instanceof CompoundContainer compound && be instanceof Container c && compound.contains(c)) return true;
        }
        return false;
    }

    public static boolean hasLinked(AbstractContainerMenu menu) {
        return !menu.slots.isEmpty() && menu.slots.get(menu.slots.size() - 1) instanceof LinkedSlot;
    }

    // --- shift-click routing (both sides; the client predicts with the same rules) ---

    /** {@code AbstractContainerMenu#clicked}: true when the click was handled here. */
    public static boolean interceptClick(AbstractContainerMenu menu, int slotIndex, ContainerInput input, Player player) {
        if (!hasLinked(menu)) return false;
        Session session = ((LinkedMenu) menu).qol$session();
        boolean linkedSlot = slotIndex >= 0 && slotIndex < menu.slots.size() && menu.slots.get(slotIndex) instanceof LinkedSlot;
        if (session != null && !session.valid() && linkedSlot) return true;
        if (input != ContainerInput.QUICK_MOVE || slotIndex < 0 || slotIndex >= menu.slots.size()) return false;
        Slot source = menu.slots.get(slotIndex);
        if (!source.mayPickup(player)) return true;
        if (linkedSlot) {
            ItemStack stack = source.getItem();
            if (stack.isEmpty()) return true;
            moveInto(stack, ownSlots(menu));
            if (!stack.isEmpty()) moveInto(stack, playerSlotsReversed(menu));
            if (stack.isEmpty()) source.setByPlayer(ItemStack.EMPTY);
            else source.setChanged();
            return true;
        }
        boolean fromPlayer = source.container instanceof Inventory;
        List<ItemStack> before = fromPlayer ? snapshot(menu) : null;
        LinkedSlot.SUPPRESSED.set(true);
        try {
            ItemStack moved = menu.quickMoveStack(player, slotIndex);
            while (!moved.isEmpty() && ItemStack.isSameItem(source.getItem(), moved)) moved = menu.quickMoveStack(player, slotIndex);
        } finally {
            LinkedSlot.SUPPRESSED.set(false);
        }
        if (fromPlayer && !ownSlotsChanged(menu, before)) {
            // The second menu took nothing (or only shuffled the inventory): fill the marked container instead.
            List<ItemStack> after = snapshot(menu);
            restorePlayerSlots(menu, before);
            ItemStack stack = source.getItem();
            List<Slot> linked = linkedSlots(menu);
            if (!stack.isEmpty() && moveInto(stack, linked)) {
                if (stack.isEmpty()) source.setByPlayer(ItemStack.EMPTY);
                else source.setChanged();
            } else {
                restorePlayerSlots(menu, after);
            }
        }
        return true;
    }

    /** Vanilla's two-pass move (merge, then empty slots) over an explicit slot list; true when anything moved. */
    static boolean moveInto(ItemStack stack, List<Slot> targets) {
        boolean changed = false;
        if (stack.isStackable()) {
            for (Slot slot : targets) {
                if (stack.isEmpty()) break;
                ItemStack target = slot.getItem();
                if (target.isEmpty() || !ItemStack.isSameItemSameComponents(stack, target) || !slot.mayPlace(stack)) continue;
                int max = slot.getMaxStackSize(target);
                if (target.getCount() >= max) continue;
                int add = Math.min(stack.getCount(), max - target.getCount());
                target.grow(add);
                stack.shrink(add);
                slot.setChanged();
                changed = true;
            }
        }
        for (Slot slot : targets) {
            if (stack.isEmpty()) break;
            if (slot.hasItem() || !slot.mayPlace(stack)) continue;
            int max = Math.min(stack.getCount(), slot.getMaxStackSize(stack));
            if (max <= 0) continue;
            slot.setByPlayer(stack.split(max));
            slot.setChanged();
            changed = true;
        }
        return changed;
    }

    /** The second menu's own slots that are not the player's inventory (chest, crafting grid, furnace...). */
    private static List<Slot> ownSlots(AbstractContainerMenu menu) {
        List<Slot> out = new ArrayList<>();
        for (Slot slot : menu.slots) if (!(slot instanceof LinkedSlot) && !(slot.container instanceof Inventory) && slot.isActive()) out.add(slot);
        return out;
    }

    /** Player slots in vanilla's chest-to-player order (from the last slot backwards). */
    private static List<Slot> playerSlotsReversed(AbstractContainerMenu menu) {
        List<Slot> out = new ArrayList<>();
        for (int i = menu.slots.size() - 1; i >= 0; i--) {
            Slot slot = menu.slots.get(i);
            if (!(slot instanceof LinkedSlot) && slot.container instanceof Inventory && slot.isActive()) out.add(slot);
        }
        return out;
    }

    private static List<Slot> linkedSlots(AbstractContainerMenu menu) {
        List<Slot> out = new ArrayList<>();
        for (Slot slot : menu.slots) if (slot instanceof LinkedSlot) out.add(slot);
        return out;
    }

    private static List<ItemStack> snapshot(AbstractContainerMenu menu) {
        List<ItemStack> out = new ArrayList<>(menu.slots.size());
        for (Slot slot : menu.slots) out.add(slot.getItem().copy());
        return out;
    }

    private static boolean ownSlotsChanged(AbstractContainerMenu menu, List<ItemStack> before) {
        for (int i = 0; i < before.size() && i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot instanceof LinkedSlot || slot.container instanceof Inventory) continue;
            if (!ItemStack.matches(slot.getItem(), before.get(i))) return true;
        }
        return false;
    }

    private static void restorePlayerSlots(AbstractContainerMenu menu, List<ItemStack> stacks) {
        for (int i = 0; i < stacks.size() && i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot instanceof LinkedSlot || !(slot.container instanceof Inventory)) continue;
            if (!ItemStack.matches(slot.getItem(), stacks.get(i))) slot.set(stacks.get(i).copy());
        }
    }
}
