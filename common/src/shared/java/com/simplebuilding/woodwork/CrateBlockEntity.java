package com.simplebuilding.woodwork;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.ArrayList;
import java.util.List;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The contents of a crate: {@link #SLOTS} stacks. Only food (items with the {@code minecraft:food} component) and
 * items in {@link #STORABLE} go in, and at most {@link #MAX_KINDS} different kinds at once (Queue N55; checked in
 * {@link #insert}, {@link #canPlaceItem} - so hoppers too - and by hand). A plain Vanilla {@link Container}, so hoppers
 * fill and empty it, comparators read it and the contents drop when the crate is broken
 * ({@code BlockEntity#preRemoveSideEffects}).
 *
 * <p>Layers: every kind forms one {@link Layer} (all its stacks together), listed from the bottom up. The kind
 * that gained items last lies on top: a new kind goes on top, a kind that is topped up moves up again, the others keep
 * their order ({@link #reorder}, run on every {@code setChanged}, so hoppers that merge into a stack are covered
 * too). The order is saved ({@code Order}); the client gets only the layers (kind and amount, {@code Layers}), not
 * the stacks (CrateRenderer / CrateMesh draw them).
 */
public class CrateBlockEntity extends BlockEntity implements Container {
    public static final int SLOTS = 9;
    /** Different kinds (item and components) one crate holds at most. */
    public static final int MAX_KINDS = 9;
    /** One kind and how many of it the crate holds in all. */
    public record Layer(ItemStack kind, int count) {
        static final Codec<Layer> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.fieldOf("kind").forGetter(Layer::kind),
                Codec.intRange(1, SLOTS * 99).fieldOf("count").forGetter(Layer::count)).apply(i, Layer::new));
    }

    /** Extra items a crate takes besides food (empty by default; for data packs). */
    public static final TagKey<Item> STORABLE = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath("simplebuilding", "crate_storable"));

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    /** Layers bottom first; server: from {@link #reorder}, client: as sent. */
    private List<Layer> layers = List.of();
    /** Counts up whenever the layers change (the client's render cache keys on it). */
    private int version;
    private boolean syncing;
    /** Client only: what the renderer built for {@link #version} (CrateMesh). */
    public Object clientCache;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRATE_BE, pos, state);
    }

    public static boolean storable(ItemStack stack) {
        return !stack.isEmpty() && (stack.has(DataComponents.FOOD) || stack.is(STORABLE));
    }

    public NonNullList<ItemStack> items() {
        return this.items;
    }

    /** The layers from the bottom up (do not change). */
    public List<Layer> layers() {
        return this.layers;
    }

    public int version() {
        return this.version;
    }

    /** True if a stack like {@code stack} lies in the crate already. */
    public boolean hasKind(ItemStack stack) {
        for (ItemStack here : this.items) {
            if (!here.isEmpty() && ItemStack.isSameItemSameComponents(here, stack)) {
                return true;
            }
        }
        return false;
    }

    /** Different kinds in the crate, counted from the stacks. */
    public int kindCount() {
        return group().size();
    }

    /** May this stack go in: storable, and either its kind is there already or there is room for a new kind. */
    public boolean accepts(ItemStack stack) {
        return storable(stack) && (hasKind(stack) || kindCount() < MAX_KINDS);
    }

    /** Puts as much of {@code stack} as fits in (same items first, then empty slots); returns what is left. */
    public ItemStack insert(ItemStack stack) {
        if (!accepts(stack)) {
            return stack;
        }
        ItemStack rest = stack.copy();
        for (int pass = 0; pass < 2 && !rest.isEmpty(); pass++) {
            for (int slot = 0; slot < SLOTS && !rest.isEmpty(); slot++) {
                ItemStack here = this.items.get(slot);
                if (pass == 0 && !here.isEmpty() && ItemStack.isSameItemSameComponents(here, rest)) {
                    int move = Math.min(rest.getCount(), getMaxStackSize(here) - here.getCount());
                    if (move > 0) {
                        here.grow(move);
                        rest.shrink(move);
                    }
                } else if (pass == 1 && here.isEmpty()) {
                    int move = Math.min(rest.getCount(), getMaxStackSize(rest));
                    this.items.set(slot, rest.split(move));
                }
            }
        }
        if (rest.getCount() != stack.getCount()) {
            setChanged();
        }
        return rest;
    }

    /** Takes {@code count} (at most, from one stack) of the kind in the top layer. */
    public ItemStack takeTop(int count) {
        if (this.layers.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack top = this.layers.get(this.layers.size() - 1).kind();
        for (int slot = SLOTS - 1; slot >= 0; slot--) {
            ItemStack here = this.items.get(slot);
            if (!here.isEmpty() && ItemStack.isSameItemSameComponents(here, top)) {
                return removeItem(slot, count);
            }
        }
        return ItemStack.EMPTY;
    }

    /** The kinds with their totals, in slot order. */
    private List<Layer> group() {
        List<Layer> out = new ArrayList<>(SLOTS);
        for (ItemStack stack : this.items) {
            if (stack.isEmpty()) {
                continue;
            }
            int at = indexOf(out, stack);
            if (at >= 0) {
                out.set(at, new Layer(out.get(at).kind(), out.get(at).count() + stack.getCount()));
            } else {
                out.add(new Layer(stack.copyWithCount(1), stack.getCount()));
            }
        }
        return out;
    }

    private static int indexOf(List<Layer> list, ItemStack kind) {
        for (int i = 0; i < list.size(); i++) {
            if (ItemStack.isSameItemSameComponents(list.get(i).kind(), kind)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Brings {@link #layers} up to date with the stacks: gone kinds drop out, a new kind or one that gained items goes
     * to the top (several at once in slot order), all others keep their place. True if something changed.
     */
    private boolean reorder() {
        List<Layer> now = group();
        List<Layer> kept = new ArrayList<>(now.size());
        List<Layer> raised = new ArrayList<>();
        for (Layer old : this.layers) {
            int at = indexOf(now, old.kind());
            if (at >= 0) {
                (now.get(at).count() > old.count() ? raised : kept).add(now.get(at));
            }
        }
        for (Layer layer : now) {
            if (indexOf(this.layers, layer.kind()) < 0) {
                raised.add(layer);
            }
        }
        kept.addAll(raised);
        return adopt(kept);
    }

    /** Takes over {@code next} as the layers; false (and nothing counted) if it equals the current ones. */
    private boolean adopt(List<Layer> next) {
        boolean same = next.size() == this.layers.size();
        for (int i = 0; same && i < next.size(); i++) {
            same = next.get(i).count() == this.layers.get(i).count()
                    && ItemStack.isSameItemSameComponents(next.get(i).kind(), this.layers.get(i).kind());
        }
        if (!same) {
            this.layers = List.copyOf(next);
            this.version++;
        }
        return !same;
    }

    /** Client: spreads the received layers over the slots again, so {@code isEmpty}, {@code countItem} and co. agree. */
    private void spread(List<Layer> received) {
        this.items.clear();
        int slot = 0;
        for (Layer layer : received) {
            int left = layer.count();
            while (left > 0 && slot < SLOTS) {
                int move = Math.min(left, getMaxStackSize(layer.kind()));
                this.items.set(slot++, layer.kind().copyWithCount(move));
                left -= move;
            }
        }
    }

    /** Filled share 0..1 over all eight stacks. */
    public float fill() {
        float sum = 0;
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                sum += (float) stack.getCount() / getMaxStackSize(stack);
            }
        }
        return sum / SLOTS;
    }

    // ------------------------------------------------------------------ Container

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack taken = ContainerHelper.removeItem(this.items, slot, count);
        if (!taken.isEmpty()) {
            setChanged();
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null && !this.level.isClientSide() && reorder()) {
            // Only when the layers changed (amounts, kinds, order): moving stacks between slots sends nothing.
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------ saving and sync

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        var received = input.read("Layers", Layer.CODEC.listOf());
        if (received.isPresent()) {
            // Client: the layers as sent (kind and amount only).
            spread(received.get());
            adopt(received.get());
            return;
        }
        this.items.clear();
        ContainerHelper.loadAllItems(input, this.items);
        // The saved order first, then whatever is left (older saves have none: slot order).
        List<Layer> now = group();
        List<Layer> ordered = new ArrayList<>(now.size());
        for (ItemStack kind : input.read("Order", ItemStack.CODEC.listOf()).orElse(List.of())) {
            int at = indexOf(now, kind);
            if (at >= 0) {
                ordered.add(now.remove(at));
            }
        }
        ordered.addAll(now);
        adopt(ordered);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.syncing) {
            output.store("Layers", Layer.CODEC.listOf(), this.layers);
            return;
        }
        ContainerHelper.saveAllItems(output, this.items);
        output.store("Order", ItemStack.CODEC.listOf(), this.layers.stream().map(Layer::kind).toList());
    }

    /** The update for the client: only kinds and amounts (not the stacks), one entry per layer. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        this.syncing = true;
        try {
            return saveCustomOnly(registries);
        } finally {
            this.syncing = false;
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
