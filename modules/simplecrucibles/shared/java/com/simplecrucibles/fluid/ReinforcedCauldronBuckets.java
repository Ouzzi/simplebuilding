package com.simplecrucibles.fluid;

import com.simplecrucibles.api.SimpleCruciblesApi;
import com.simplelib.bucket.Buckets;
import com.simplelib.bucket.ModBucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * The module's buckets on the reinforced cauldron (split stage 5b: was SimpleBuilding's part): soul lava is its
 * "extreme" content (the iron bucket breaks when pouring it, the Enderite bucket comes back); copper, Enderite and
 * ceramic buckets take and pour water and lava like everywhere else (copper breaks on lava, cannot take soul lava).
 * Vanilla's iron bucket scoops soul lava into the soul lava bucket.
 */
public final class ReinforcedCauldronBuckets implements SimpleCruciblesApi.CauldronBucket {
    @Override
    public @Nullable String pours(ItemStack held) {
        if (!(held.getItem() instanceof ModBucketItem bucket)) return null;
        Fluid content = bucket.getContent();
        if (content.isSame(CrucibleFluids.SOUL_LAVA)) return "extreme";
        if (content.isSame(Fluids.LAVA)) return "lava";
        if (content.isSame(Fluids.WATER)) return "water";
        return null;
    }

    @Override
    public ItemStack afterPour(ItemStack held) {
        return held.getItem() instanceof ModBucketItem bucket ? bucket.afterPour(held) : ItemStack.EMPTY;
    }

    @Override
    public @Nullable ItemStack take(ItemStack held, String content) {
        // Owner N21: a half Enderite bucket takes one more bucket of its own fluid and becomes full.
        Item fuller = Buckets.fullEnderite(held.getItem());
        if (fuller != null) return content.equals(pours(held)) ? ModBucketItem.fill(held, fuller) : null;
        ModBucketItem.Kind kind;
        if (held.is(Items.BUCKET)) kind = ModBucketItem.Kind.IRON;
        else if (held.getItem() instanceof ModBucketItem bucket && bucket.getContent() == Fluids.EMPTY
                && ModBucketItem.canScoop(held)) kind = bucket.kind();
        else return null;
        Fluid fluid = switch (content) {
            case "extreme" -> CrucibleFluids.SOUL_LAVA;
            case "lava" -> kind == ModBucketItem.Kind.IRON ? null : Fluids.LAVA;
            case "water" -> kind == ModBucketItem.Kind.IRON ? null : Fluids.WATER;
            default -> null;
        };
        if (fluid == null) return null;
        Item filled = ModBucketItem.filled(kind, fluid);
        return filled == null ? null : ModBucketItem.fill(held, filled);
    }
}
