package com.simplebuilding.modules.simpleinterfaces.linked;

import com.simplebuilding.modules.simpleinterfaces.SimpleInterfaces;
import com.simplelib.api.InteractionGuard;
import com.simplebuilding.modules.simpleinterfaces.network.LinkedCancelPayload;
import com.simplebuilding.modules.simpleinterfaces.network.LinkedOpenPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
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
import net.minecraft.world.inventory.InventoryMenu;
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
 * Linked GUIs (owner queue 2026-10-04, N65 several marks): sneak + right-click a container with an empty main hand marks
 * it (up to {@code linkedContainerMax}, the oldest drops out); the next menu the player opens gets the slots of every
 * marked container appended, in marking order. Server-authoritative: the marks live on the player object (gone on logout
 * or respawn) and are checked every tick one by one (range, dimension, loaded chunk, block entity); every slot access
 * checks its container again.
 */
public final class LinkedContainers {
    /** Most slots the marked containers may show together (a double chest of the widest mod chests: 12 rows of 9). */
    public static final int MAX_SLOTS = 108;
    /** InventoryMenu slot indices of the main inventory plus hotbar: [9, 45). */
    private static final int INV_START = 9, INV_END = 45;
    /** The marks of each player, oldest first. */
    private static final Map<Player, List<Mark>> MARKS = new WeakHashMap<>();
    /** The marks the inventory menu already tried to show (a failed attempt is not repeated every tick). */
    private static final Map<Player, List<Mark>> TRIED = new WeakHashMap<>();
    /** Sends the payload when the client can receive it; set per loader. Without it nothing is appended. */
    public static BiPredicate<ServerPlayer, LinkedOpenPayload> openSync = (player, payload) -> false;
    /** The mark whose container the player has opened since marking it (leaving it again ends that mark). */
    private static final Map<Player, Mark> INSIDE = new WeakHashMap<>();
    /** The marks the client's HUD tiles last got. */
    private static final Map<Player, List<Mark>> HUD = new WeakHashMap<>();
    /** Client: sends the Escape cancel to the server; set per loader. */
    public static Consumer<LinkedCancelPayload> cancelToServer = payload -> {
    };

    private LinkedContainers() {
    }

    public record Mark(ResourceKey<Level> dimension, BlockPos pos) {
    }

    /** One marked container in a linked menu: the container, the block entities behind it and where its slots start. */
    public static final class Part {
        public final ServerPlayer player;
        public final Mark mark;
        public final Container container;
        public final List<BlockEntity> backing;
        public final int start;
        public final int size;

        Part(ServerPlayer player, Mark mark, Container container, List<BlockEntity> backing, int start, int size) {
            this.player = player;
            this.mark = mark;
            this.container = container;
            this.backing = backing;
            this.start = start;
            this.size = size;
        }

        public boolean valid() {
            if (!LinkedContainers.valid(this.player, this.mark)) return false;
            // The creative inventory never works on the mark (its slots are client-side): nothing moves there.
            if (this.player.hasInfiniteMaterials() && this.player.containerMenu == this.player.inventoryMenu) return false;
            for (BlockEntity be : this.backing) {
                if (be.isRemoved() || this.player.level().getBlockEntity(be.getBlockPos()) != be) return false;
            }
            return true;
        }
    }

    /** One linked menu: the marks it was built for and the containers actually appended (the open one itself is left out). */
    public static final class Session {
        public final List<Mark> requested;
        public final List<Part> parts;
        public final int start;

        Session(List<Mark> requested, List<Part> parts, int start) {
            this.requested = List.copyOf(requested);
            this.parts = List.copyOf(parts);
            this.start = start;
        }

        public boolean valid() {
            for (Part part : this.parts) if (!part.valid()) return false;
            return true;
        }
    }

    /** The player's newest mark, or null. */
    public static @Nullable Mark mark(Player player) {
        List<Mark> marks = MARKS.get(player);
        return marks == null || marks.isEmpty() ? null : marks.get(marks.size() - 1);
    }

    /** All marks of the player, oldest first (a copy). */
    public static List<Mark> marks(Player player) {
        List<Mark> marks = MARKS.get(player);
        return marks == null ? List.of() : List.copyOf(marks);
    }

    /** Ends every mark of the player (and tells its client). */
    public static void clear(Player player) {
        MARKS.remove(player);
        INSIDE.remove(player);
        if (player instanceof ServerPlayer serverPlayer) {
            syncInventory(serverPlayer);
            syncHud(serverPlayer);
        }
    }

    /** Ends one mark; the caller syncs. */
    private static void unmark(Player player, Mark mark) {
        List<Mark> marks = MARKS.get(player);
        if (marks != null) {
            marks.removeIf(m -> m == mark);
            if (marks.isEmpty()) MARKS.remove(player);
        }
        if (INSIDE.get(player) == mark) INSIDE.remove(player);
    }

    /** Server handler of the Escape cancel: ends every mark, no feedback beyond the vanishing tiles and the chime. */
    public static void onCancel(ServerPlayer player) {
        Mark mark = mark(player);
        if (mark == null) return;
        clear(player);
        if (mark.dimension().equals(player.level().dimension())) feedback(player.level(), mark.pos(), false);
    }

    /** Sends the HUD tiles when the marks changed (container id {@link LinkedOpenPayload#HUD}); independent of any menu. */
    public static void syncHud(ServerPlayer player) {
        List<Mark> marks = marks(player);
        if (same(HUD.getOrDefault(player, List.of()), marks)) return;
        if (marks.isEmpty()) HUD.remove(player);
        else HUD.put(player, marks);
        Level level = player.level();
        List<LinkedOpenPayload.Entry> entries = new ArrayList<>();
        for (Mark mark : marks) {
            if (!mark.dimension().equals(level.dimension())) continue;
            entries.add(new LinkedOpenPayload.Entry(1, title(level, mark.pos()), icon(level, mark.pos())));
        }
        openSync.test(player, new LinkedOpenPayload(LinkedOpenPayload.HUD, entries));
    }

    /** Same marks (the same objects) in the same order. */
    private static boolean same(@Nullable List<Mark> a, @Nullable List<Mark> b) {
        if (a == null || b == null) return a == b;
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) if (a.get(i) != b.get(i)) return false;
        return true;
    }

    private static Component title(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BaseContainerBlockEntity be ? be.getDisplayName() : Component.empty();
    }

    private static ItemStack icon(Level level, BlockPos pos) {
        return new ItemStack(level.getBlockState(pos).getBlock());
    }

    // --- marking ---

    /**
     * Use hook of every loader: sneak + right-click with an empty main hand on a container. A marked container (either
     * half of a double chest) is unmarked; another one is added; beyond {@code linkedContainerMax} marks or
     * {@link #MAX_SLOTS} slots together the oldest marks drop out.
     */
    public static InteractionResult onRightClickBlock(Player player, InteractionHand hand, BlockPos pos) {
        Level level = player.level();
        if (hand != InteractionHand.MAIN_HAND || !SimpleInterfaces.active(level) || !SimpleInterfaces.configFor(level).enableLinkedContainers) return InteractionResult.PASS;
        if (!player.isSecondaryUseActive() || !player.getMainHandItem().isEmpty() || player.isSpectator()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof BaseContainerBlockEntity be)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        for (Mark current : marks(player)) {
            if (current.dimension().equals(level.dimension()) && backingPositions(level, current.pos()).contains(pos)) {
                unmark(player, current);
                syncHud((ServerPlayer) player);
                syncInventory((ServerPlayer) player);
                feedback(level, pos, false);
                return InteractionResult.SUCCESS;
            }
        }
        if (!InteractionGuard.allow(player, pos) || !InteractionGuard.permission.test(player, pos) || !be.canOpen(player)) return InteractionResult.PASS;
        Container container = container(level, pos);
        if (container == null || container.getContainerSize() <= 0 || container.getContainerSize() > MAX_SLOTS) return InteractionResult.PASS;
        List<Mark> marks = MARKS.computeIfAbsent(player, p -> new ArrayList<>());
        marks.add(new Mark(level.dimension(), pos.immutable()));
        trim(player, level, marks, SimpleInterfaces.configFor(level).linkedContainerMax);
        syncHud((ServerPlayer) player);
        syncInventory((ServerPlayer) player);
        feedback(level, pos, true);
        return InteractionResult.SUCCESS;
    }

    /** Drops the oldest marks while more than {@code max} or more than {@link #MAX_SLOTS} slots together (the newest stays). */
    static void trim(Player player, Level level, List<Mark> marks, int max) {
        while (marks.size() > Math.max(1, max) || marks.size() > 1 && slots(level, marks) > MAX_SLOTS) {
            Mark oldest = marks.remove(0);
            if (INSIDE.get(player) == oldest) INSIDE.remove(player);
        }
    }

    /** Slots of the marked containers in {@code level} together. */
    private static int slots(Level level, List<Mark> marks) {
        int n = 0;
        for (Mark mark : marks) {
            if (!mark.dimension().equals(level.dimension())) continue;
            Container c = container(level, mark.pos());
            if (c != null) n += c.getContainerSize();
        }
        return n;
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
        var config = SimpleInterfaces.config();
        if (!SimpleInterfaces.active() || !config.enableLinkedContainers || !player.level().dimension().equals(mark.dimension())) return false;
        Level level = player.level();
        if (!level.isLoaded(mark.pos())) return false;
        double range = config.linkedContainerRange;
        if (player.distanceToSqr(Vec3.atCenterOf(mark.pos())) > range * range) return false;
        return level.getBlockEntity(mark.pos()) instanceof BaseContainerBlockEntity be && !be.isRemoved();
    }

    // --- server tick ---

    /** The claim/lock right is re-checked this often: a mark does not outlive a revoked right (a vetoing claim, a new lock). */
    private static final int PERMISSION_TICKS = 20;

    private static boolean permitted(ServerPlayer player, Mark mark) {
        return player.level().getBlockEntity(mark.pos()) instanceof BaseContainerBlockEntity be && be.canOpen(player)
                && InteractionGuard.permission.test(player, mark.pos());
    }

    /** Start of every server player tick: end invalid marks one by one, close broken linked menus, hand particles. */
    public static void tick(ServerPlayer player) {
        for (Mark mark : marks(player)) {
            if (!valid(player, mark) || player.tickCount % PERMISSION_TICKS == 0 && !permitted(player, mark) || leftMarked(player, mark)) {
                unmark(player, mark);
            }
        }
        Session session = ((LinkedMenu) player.containerMenu).qol$session();
        if (session != null && !session.valid() && player.containerMenu != player.inventoryMenu) player.closeContainer();
        syncInventory(player);
        syncHud(player);
        if (mark(player) != null && player.tickCount % 10 == 0) handParticle(player);
    }

    /**
     * Whether the player opened the marked container itself and has now left it again (closed it or opened
     * something else): that ends the mark, the container was visited. While it is open the visit is only noted.
     */
    private static boolean leftMarked(ServerPlayer player, Mark mark) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu != player.inventoryMenu && opens(player, menu, mark)) {
            INSIDE.put(player, mark);
            return false;
        }
        return INSIDE.get(player) == mark;
    }

    /** Whether one of the menu's own slots (not the appended linked ones) is the marked container. */
    private static boolean opens(ServerPlayer player, AbstractContainerMenu menu, Mark mark) {
        Container container = container(player.level(), mark.pos());
        if (container == null) return false;
        List<BlockEntity> backing = backing(player.level(), mark.pos());
        for (Slot slot : menu.slots) {
            if (!(slot instanceof LinkedSlot) && shows(slot.container, container, backing)) return true;
        }
        return false;
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

    /** {@code ServerPlayer#initMenu}: before the menu's first sync, append the marked containers' slots. */
    public static void onInitMenu(ServerPlayer player, AbstractContainerMenu menu) {
        if (menu == player.inventoryMenu || menu instanceof PortableMenu || hasLinked(menu)) return;
        List<Mark> marks = marks(player);
        if (marks.isEmpty()) return;
        boolean dropped = false;
        for (Mark mark : marks) {
            if (!valid(player, mark)) {
                unmark(player, mark);
                dropped = true;
            }
        }
        if (dropped) {
            syncInventory(player);
            syncHud(player);
        }
        marks = marks(player);
        if (!marks.isEmpty()) attach(player, menu, marks);
    }

    /**
     * Keeps the player's own inventory menu in line with the marks: slots appended while valid marks exist (so the
     * inventory screen shows the marked containers next to it), removed again when they end. Sends the payload first.
     */
    public static void syncInventory(ServerPlayer player) {
        AbstractContainerMenu menu = player.inventoryMenu;
        // Creative: the inventory screen keeps its own client-side slots, the mark never takes part in it.
        List<Mark> marks = player.hasInfiniteMaterials() ? List.of() : marks(player);
        Session current = ((LinkedMenu) menu).qol$session();
        if (current != null && same(current.requested, marks)) return;
        if (marks.isEmpty()) TRIED.remove(player);
        else if (current == null && same(TRIED.get(player), marks)) return;
        if (current != null) {
            ((LinkedMenu) menu).qol$truncate(current.start);
            ((LinkedMenu) menu).qol$session(null);
        }
        boolean attached = false;
        if (!marks.isEmpty()) {
            TRIED.put(player, marks);
            attached = attach(player, menu, marks);
        }
        if (!attached && current != null) openSync.test(player, new LinkedOpenPayload(menu.containerId, List.of()));
        if (attached || current != null) menu.sendAllDataToRemote();
    }

    /**
     * Appends the slots of every mark in {@code marks} that the menu does not already show (the marked chest opened
     * itself), in order, at most {@link #MAX_SLOTS} together; one payload with an entry per appended container first.
     */
    private static boolean attach(ServerPlayer player, AbstractContainerMenu menu, List<Mark> marks) {
        Level level = player.level();
        int start = menu.slots.size();
        int total = 0;
        List<Part> parts = new ArrayList<>();
        List<LinkedOpenPayload.Entry> entries = new ArrayList<>();
        for (Mark mark : marks) {
            if (!mark.dimension().equals(level.dimension())) continue;
            Container container = container(level, mark.pos());
            if (container == null) continue;
            int size = container.getContainerSize();
            if (size <= 0 || total + size > MAX_SLOTS) continue;
            List<BlockEntity> backing = backing(level, mark.pos());
            boolean shown = false;
            for (Slot slot : menu.slots) {
                if (!(slot instanceof LinkedSlot) && shows(slot.container, container, backing)) shown = true;
            }
            if (shown || !InteractionGuard.permission.test(player, mark.pos())) continue;
            parts.add(new Part(player, mark, container, backing, start + total, size));
            entries.add(new LinkedOpenPayload.Entry(size, title(level, mark.pos()), icon(level, mark.pos())));
            total += size;
        }
        if (parts.isEmpty() || !openSync.test(player, new LinkedOpenPayload(menu.containerId, entries))) return false;
        for (Part part : parts) {
            for (int i = 0; i < part.size; i++) ((LinkedMenu) menu).qol$addSlot(new LinkedSlot(part.container, i, part));
        }
        ((LinkedMenu) menu).qol$session(new Session(marks, parts, start));
        return true;
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
        boolean inventoryMenu = menu instanceof InventoryMenu;
        boolean linkedSlot = slotIndex >= 0 && slotIndex < menu.slots.size() && menu.slots.get(slotIndex) instanceof LinkedSlot;
        // Only the menu the player really has open takes part (the server never moves items for another one).
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.containerMenu != menu) return true;
        // Creative inventory: the mark never works there; its clicks stay vanilla and never touch the marked slots.
        if (inventoryMenu && player.hasInfiniteMaterials()) return linkedSlot;
        if (linkedSlot && menu.slots.get(slotIndex) instanceof LinkedSlot l && l.part != null && !l.part.valid()) return true;
        if (input != ContainerInput.QUICK_MOVE || slotIndex < 0 || slotIndex >= menu.slots.size()) return false;
        Slot source = menu.slots.get(slotIndex);
        if (!source.mayPickup(player)) return true;
        if (linkedSlot) {
            ItemStack stack = source.getItem();
            if (stack.isEmpty()) return true;
            if (!inventoryMenu) moveInto(stack, ownSlots(menu));
            if (!stack.isEmpty()) moveInto(stack, inventoryMenu ? mainSlotsReversed(menu) : playerSlotsReversed(menu));
            if (stack.isEmpty()) source.setByPlayer(ItemStack.EMPTY);
            else source.setChanged();
            return true;
        }
        if (inventoryMenu) {
            // The inventory screen: main/hotbar slots fill the marked container; armor, crafting and the rest stay vanilla.
            if (source.index < INV_START || source.index >= INV_END || !source.hasItem()) return false;
            ItemStack stack = source.getItem();
            if (!moveInto(stack, linkedSlots(menu))) return false;
            if (stack.isEmpty()) source.setByPlayer(ItemStack.EMPTY);
            else source.setChanged();
            return true;
        }
        boolean fromPlayer = source.container instanceof Inventory;
        if (!fromPlayer && source.hasItem() && source.mayPlace(source.getItem())) {
            // The marked boxes are shown where the inventory was: a plain storage slot of the second GUI goes there first (all marked, in order).
            ItemStack stack = source.getItem();
            moveInto(stack, linkedSlots(menu));
            if (stack.isEmpty()) {
                source.setByPlayer(ItemStack.EMPTY);
                return true;
            }
            source.setChanged();
        }
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

    /** Main inventory and hotbar of the player's own menu, last slot first. */
    private static List<Slot> mainSlotsReversed(AbstractContainerMenu menu) {
        List<Slot> out = new ArrayList<>();
        for (int i = INV_END - 1; i >= INV_START; i--) out.add(menu.slots.get(i));
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
