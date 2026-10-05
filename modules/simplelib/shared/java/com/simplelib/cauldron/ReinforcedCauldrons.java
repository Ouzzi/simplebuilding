package com.simplelib.cauldron;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Partner containers for the reinforced cauldron (owner 30/56): a partner adds its own buckets (and
 * the extreme fluid, SimpleBuilding's soul lava) through {@code SimpleLibApi.registerCauldronBucket}.
 * Vanilla buckets, bottles and everything else go through the Vanilla cauldron interaction tables
 * (see {@link ReinforcedCauldronBlock}). A full container is only taken from a full cauldron.
 */
public final class ReinforcedCauldrons {
    /** One kind of container (e.g. the vanilla bucket, a copper bucket). */
    public interface Bucket {
        /** The content {@code held} pours into an empty cauldron, or null when it is not a full container of this kind. */
        @Nullable ReinforcedCauldronBlock.Content pours(ItemStack held);

        /** What is left in the hand after pouring (ItemStack.EMPTY: the container breaks). */
        ItemStack afterPour(ItemStack held);

        /** The filled container when {@code held} takes {@code content}; null when this kind cannot take it or is not this kind. */
        @Nullable ItemStack take(ItemStack held, ReinforcedCauldronBlock.Content content);
    }

    public static final List<Bucket> BUCKETS = new CopyOnWriteArrayList<>();
    /** What the extreme content does to an entity inside (set by the partner that owns the fluid). */
    public static volatile BiConsumer<Level, Entity> extremeInside = (level, entity) -> entity.lavaHurt();

    private ReinforcedCauldrons() {}
}
