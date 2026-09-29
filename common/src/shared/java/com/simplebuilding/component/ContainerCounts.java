package com.simplebuilding.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * The real counts of the slots of a {@code minecraft:container} that hold more than vanilla lets a
 * stored stack hold: at most 99 (the item codec) and, inside {@code minecraft:container}, at most the
 * item's own stack size (26.3 drops a container whose stacks are larger). {@code minecraft:container}
 * keeps a readable copy capped at that limit (64 cobblestone, 16 pearls); this component carries the
 * real count, so an item that holds a Netherite or Enderite Shulker Box keeps its stacks of 128 and
 * 256 (see {@code TieredShulkerBoxBlockEntity}).
 *
 * <p>Empty lists are never stored: {@link #of} returns null when no slot is oversized.
 */
public record ContainerCounts(List<Slot> slots) {
    /** Highest count vanilla's item codec (and so {@code minecraft:container}) stores. */
    public static final int CODEC_MAX_COUNT = 99;
    /** Upper bound of a slot index, like {@code ItemContainerContents#MAX_SIZE}. */
    public static final int MAX_SLOTS = 256;

    /** One oversized slot. */
    public record Slot(int slot, int count) {
        public static final Codec<Slot> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, MAX_SLOTS - 1).fieldOf("slot").forGetter(Slot::slot),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("count").forGetter(Slot::count)
        ).apply(i, Slot::new));

        public static final StreamCodec<ByteBuf, Slot> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Slot::slot,
                ByteBufCodecs.VAR_INT, Slot::count,
                Slot::new);
    }

    public static final Codec<ContainerCounts> CODEC = Slot.CODEC.listOf()
            .xmap(ContainerCounts::new, ContainerCounts::slots);

    public static final StreamCodec<ByteBuf, ContainerCounts> STREAM_CODEC = Slot.STREAM_CODEC
            .apply(ByteBufCodecs.list(MAX_SLOTS))
            .map(ContainerCounts::new, ContainerCounts::slots);

    public ContainerCounts {
        slots = List.copyOf(slots);
    }

    /** The largest count vanilla stores for {@code stack}: its own stack size, never more than 99. */
    public static int storableCount(ItemStack stack) {
        return Math.max(1, Math.min(CODEC_MAX_COUNT, stack.getMaxStackSize()));
    }

    /** The oversized slots of {@code items}, or null when every slot is storable as it is. */
    public static ContainerCounts of(List<ItemStack> items) {
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getCount() > storableCount(items.get(i))) {
                slots.add(new Slot(i, items.get(i).getCount()));
            }
        }
        return slots.isEmpty() ? null : new ContainerCounts(slots);
    }

    /** Puts the real counts back into {@code items} (slots that are empty or out of range stay as they are). */
    public void applyTo(List<ItemStack> items) {
        for (Slot slot : this.slots) {
            if (slot.slot() < items.size() && !items.get(slot.slot()).isEmpty()) {
                items.get(slot.slot()).setCount(slot.count());
            }
        }
    }

    /** {@code items} with every oversized stack replaced by a capped copy (other stacks are the same objects). */
    public static NonNullList<ItemStack> codecSafeCopy(List<ItemStack> items) {
        NonNullList<ItemStack> copy = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            int cap = storableCount(stack);
            copy.set(i, stack.getCount() > cap ? stack.copyWithCount(cap) : stack);
        }
        return copy;
    }
}
