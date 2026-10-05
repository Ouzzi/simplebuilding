package com.simplelib.cauldron;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Which containers pour into and take from the reinforced cauldron (owner 30/56). Vanilla buckets
 * are built in; a partner adds its own buckets (and the extreme fluid, SimpleBuilding's soul lava)
 * through {@code SimpleLibApi.registerCauldronBucket}.
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

    static {
        BUCKETS.add(new Bucket() {
            @Override
            public ReinforcedCauldronBlock.Content pours(ItemStack held) {
                if (held.is(Items.WATER_BUCKET)) return ReinforcedCauldronBlock.Content.WATER;
                if (held.is(Items.LAVA_BUCKET)) return ReinforcedCauldronBlock.Content.LAVA;
                if (held.is(Items.POWDER_SNOW_BUCKET)) return ReinforcedCauldronBlock.Content.POWDER_SNOW;
                return null;
            }

            @Override
            public ItemStack afterPour(ItemStack held) {
                return new ItemStack(Items.BUCKET);
            }

            @Override
            public ItemStack take(ItemStack held, ReinforcedCauldronBlock.Content content) {
                if (!held.is(Items.BUCKET)) return null;
                return switch (content) {
                    case WATER -> new ItemStack(Items.WATER_BUCKET);
                    case LAVA -> new ItemStack(Items.LAVA_BUCKET);
                    case POWDER_SNOW -> new ItemStack(Items.POWDER_SNOW_BUCKET);
                    default -> null;
                };
            }
        });
    }

    private ReinforcedCauldrons() {}
}
