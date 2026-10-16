package com.simplecrucibles.fluid;

import com.simplecrucibles.api.SimpleCruciblesApi;
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
        if (!(held.getItem() instanceof CrucibleBucketItem bucket)) return null;
        Fluid content = bucket.getContent();
        if (content.isSame(CrucibleFluids.SOUL_LAVA)) return "extreme";
        if (content.isSame(Fluids.LAVA)) return "lava";
        if (content.isSame(Fluids.WATER)) return "water";
        return null;
    }

    @Override
    public ItemStack afterPour(ItemStack held) {
        return held.getItem() instanceof CrucibleBucketItem bucket ? bucket.afterPour(held) : ItemStack.EMPTY;
    }

    @Override
    public @Nullable ItemStack take(ItemStack held, String content) {
        // Owner N21: a half Enderite bucket takes one more bucket of its own fluid and becomes full.
        Item fuller = CrucibleFluids.fullEnderite(held.getItem());
        if (fuller != null) return content.equals(pours(held)) ? CrucibleBucketItem.fill(held, fuller) : null;
        CrucibleBucketItem.Kind kind;
        if (held.is(Items.BUCKET)) kind = CrucibleBucketItem.Kind.IRON;
        else if (held.getItem() instanceof CrucibleBucketItem bucket && bucket.getContent() == Fluids.EMPTY
                && CrucibleBucketItem.canScoop(held)) kind = bucket.kind();
        else return null;
        Fluid fluid = switch (content) {
            case "extreme" -> CrucibleFluids.SOUL_LAVA;
            case "lava" -> kind == CrucibleBucketItem.Kind.IRON ? null : Fluids.LAVA;
            case "water" -> kind == CrucibleBucketItem.Kind.IRON ? null : Fluids.WATER;
            default -> null;
        };
        if (fluid == null) return null;
        Item filled = CrucibleBucketItem.filled(kind, fluid);
        return filled == null ? null : CrucibleBucketItem.fill(held, filled);
    }
}
