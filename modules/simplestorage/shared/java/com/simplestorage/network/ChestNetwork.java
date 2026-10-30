package com.simplestorage.network;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * The chest network (BS29b-3): cables and interfaces connect chests to ONE controller chest. This class finds the network
 * and works on its chests; it never runs on its own (no tick, no scan per tick).
 *
 * <p><b>Events instead of polling.</b> Cables and interfaces call {@link #changed} when they are placed, removed or see a neighbour
 * change. A controller keeps its last {@link Scan} and searches again ({@link #scan}) only when {@link #version} moved on since.
 * The chests behind the interfaces ({@link #members}) and their contents ({@link Storage}) are looked up only while somebody has the
 * controller window open, a few times a second.
 *
 * <p>Wiring: an interface sits in the cell next to a chest and points at it ({@code facing}); it connects to cables and other
 * interfaces on its other five sides. A cable connects to cables and to the non-front sides of interfaces.
 */
public final class ChestNetwork {
    /** Most cables and interfaces one search visits (a bigger network is "too big" and the controller breaks). */
    public static final int MAX_CONDUCTORS = 512;

    private static final AtomicLong VERSION = new AtomicLong();

    private ChestNetwork() {
    }

    /** Counts up whenever a cable or interface appears, goes or sees a neighbour change. */
    public static long version() {
        return VERSION.get();
    }

    public static void changed() {
        VERSION.incrementAndGet();
    }

    public enum Status {
        OK,
        /** More chest blocks than the controller's step allows. */
        TOO_MANY,
        /** A second controller hangs on the same network. */
        CONFLICT,
        /** More than {@link #MAX_CONDUCTORS} cables and interfaces. */
        TOO_BIG
    }

    /** One search: the interfaces (not the controller's own) that sit on chests, and what the search found out about the whole. */
    public record Scan(long version, Status status, List<BlockPos> interfaces, int conductors, int ownInterfaces) {
    }

    /** The chests of a network: block positions (a double chest has two), and how many were ignored for being above the controller. */
    public record Members(List<BlockPos> chests, int rejected) {
    }

    /** Whether the state {@code neighbor} accepts a wire from the side {@code from} (the side that faces the wire's source). */
    public static boolean conducts(BlockState neighbor, Direction from) {
        if (neighbor.getBlock() instanceof CableBlock) return true;
        return neighbor.getBlock() instanceof InterfaceBlock && neighbor.getValue(InterfaceBlock.FACING) != from;
    }

    /** Searches the network of the controller at {@code controller}: breadth first over cables and interfaces. */
    public static Scan scan(Level level, BlockPos controller) {
        long version = version();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        int own = 0;
        for (Direction direction : Direction.values()) {
            BlockPos pos = controller.relative(direction);
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof InterfaceBlock && state.getValue(InterfaceBlock.FACING) == direction.getOpposite()) {
                seen.add(pos);
                queue.add(pos);
                own++;
            }
        }
        Status status = Status.OK;
        List<BlockPos> interfaces = new ArrayList<>();
        while (!queue.isEmpty()) {
            if (seen.size() > MAX_CONDUCTORS) {
                status = Status.TOO_BIG;
                break;
            }
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            Direction front = null;
            if (state.getBlock() instanceof InterfaceBlock) {
                front = state.getValue(InterfaceBlock.FACING);
                BlockPos mounted = pos.relative(front);
                if (!mounted.equals(controller)) {
                    Object block = level.getBlockState(mounted).getBlock();
                    if (block instanceof ControllerChestBlock) {
                        if (status == Status.OK) status = Status.CONFLICT;
                    } else if (block instanceof ChestBlock) {
                        interfaces.add(pos);
                    }
                }
            }
            for (Direction direction : Direction.values()) {
                if (direction == front) continue;
                BlockPos next = pos.relative(direction);
                if (seen.contains(next) || !level.hasChunkAt(next)) continue;
                if (!conducts(level.getBlockState(next), direction.getOpposite())) continue;
                seen.add(next);
                queue.add(next);
            }
        }
        return new Scan(version, status, List.copyOf(interfaces), seen.size(), own);
    }

    /** The chests behind the scan's interfaces, split into those the controller {@code rank} takes and those above it. */
    public static Members members(Level level, int rank, Scan scan) {
        Set<BlockPos> accepted = new LinkedHashSet<>();
        Set<BlockPos> rejected = new HashSet<>();
        for (BlockPos interfacePos : scan.interfaces()) {
            BlockState interfaceState = level.getBlockState(interfacePos);
            if (!(interfaceState.getBlock() instanceof InterfaceBlock)) continue;
            BlockPos chestPos = interfacePos.relative(interfaceState.getValue(InterfaceBlock.FACING));
            BlockState chest = level.getBlockState(chestPos);
            int chestRank = ControllerLevel.rankOf(chest.getBlock());
            if (chestRank < 0) continue;
            List<BlockPos> halves = new ArrayList<>(2);
            halves.add(chestPos);
            if (chest.hasProperty(ChestBlock.TYPE) && chest.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                halves.add(ChestBlock.getConnectedBlockPos(chestPos, chest));
            }
            (chestRank > rank ? rejected : accepted).addAll(halves);
        }
        return new Members(List.copyOf(accepted), rejected.size());
    }

    // ------------------------------------------------------------------ contents

    /** A kind of item (item and components) across the whole network, with its total. {@code kind} has count 1. */
    public record Entry(ItemStack kind, long count) {
    }

    private record Key(Item item, DataComponentPatch components) {
        static Key of(ItemStack stack) {
            return new Key(stack.getItem(), stack.getComponentsPatch());
        }
    }

    public enum Sort {
        COUNT,
        NAME;

        public Sort next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /**
     * The containers a window works on: the controller itself first, then the chests. A chest a player may not open (locked) stays
     * out for that player. Looked up fresh for every use; hold it only for one action.
     */
    public static final class Storage {
        private final List<Container> containers = new ArrayList<>();

        public Storage(Level level, Container controller, Members members, Player player) {
            this.containers.add(controller);
            for (BlockPos pos : members.chests()) {
                BlockEntity entity = level.getBlockEntity(pos);
                if (!(entity instanceof Container container)) continue;
                if (entity instanceof BaseContainerBlockEntity locked && !locked.canOpen(player)) continue;
                if (entity instanceof RandomizableContainerBlockEntity loot && loot.getLootTable() != null) loot.unpackLootTable(player);
                this.containers.add(container);
            }
        }

        public int containerCount() {
            return this.containers.size();
        }

        /** Every kind with its total, sorted. */
        public List<Entry> entries(Sort sort) {
            Map<Key, Entry> sums = new LinkedHashMap<>();
            for (Container container : this.containers) {
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                    ItemStack stack = container.getItem(slot);
                    if (stack.isEmpty()) continue;
                    sums.merge(Key.of(stack), new Entry(stack.copyWithCount(1), stack.getCount()),
                            (a, b) -> new Entry(a.kind(), a.count() + b.count()));
                }
            }
            List<Entry> out = new ArrayList<>(sums.values());
            Comparator<Entry> byName = Comparator.comparing(e -> BuiltInRegistries.ITEM.getKey(e.kind().getItem()).toString());
            if (sort == Sort.NAME) out.sort(byName);
            else out.sort(Comparator.comparingLong((Entry e) -> e.count()).reversed().thenComparing(byName));
            return out;
        }

        /** Takes up to {@code max} of {@code kind} out of the chests (the later ones first), returns what it got. */
        public ItemStack take(ItemStack kind, int max) {
            ItemStack out = kind.copyWithCount(0);
            for (int c = this.containers.size() - 1; c >= 0 && out.getCount() < max; c--) {
                Container container = this.containers.get(c);
                boolean touched = false;
                for (int slot = container.getContainerSize() - 1; slot >= 0 && out.getCount() < max; slot--) {
                    ItemStack stack = container.getItem(slot);
                    if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, kind)) continue;
                    ItemStack taken = container.removeItem(slot, Math.min(max - out.getCount(), stack.getCount()));
                    out.grow(taken.getCount());
                    touched = true;
                }
                if (touched) container.setChanged();
            }
            return out.isEmpty() ? ItemStack.EMPTY : out;
        }

        /** Puts {@code stack} into the chests (onto the same kind first, then empty slots, controller first); returns the rest. */
        public ItemStack insert(ItemStack stack) {
            ItemStack rest = stack.copy();
            for (int pass = 0; pass < 2 && !rest.isEmpty(); pass++) {
                for (Container container : this.containers) {
                    if (rest.isEmpty()) break;
                    boolean touched = false;
                    for (int slot = 0; slot < container.getContainerSize() && !rest.isEmpty(); slot++) {
                        ItemStack here = container.getItem(slot);
                        if (pass == 0 && !here.isEmpty() && ItemStack.isSameItemSameComponents(here, rest)) {
                            int room = container.getMaxStackSize(here) - here.getCount();
                            int move = Math.min(room, rest.getCount());
                            if (move > 0) {
                                here.grow(move);
                                rest.shrink(move);
                                touched = true;
                            }
                        } else if (pass == 1 && here.isEmpty() && container.canPlaceItem(slot, rest)) {
                            int move = Math.min(container.getMaxStackSize(rest), rest.getCount());
                            if (move > 0) {
                                container.setItem(slot, rest.split(move));
                                touched = true;
                            }
                        }
                    }
                    if (touched) container.setChanged();
                }
            }
            return rest;
        }
    }
}
