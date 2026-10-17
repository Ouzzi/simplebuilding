package com.simplemobheads.drop;

import com.mojang.serialization.MapCodec;
import com.simplemobheads.MobHeads;
import com.simplemobheads.data.HeadDef;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Loot function {@code simplemobheads:copy_variant}: the dropped head shows the variant of the killed mob (concept §3.3). */
public record CopyVariantFunction() implements LootItemFunction {
    public static final MapCodec<CopyVariantFunction> CODEC = MapCodec.unit(new CopyVariantFunction());

    @Override
    public MapCodec<CopyVariantFunction> codec() {
        return CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.THIS_ENTITY);
    }

    @Override
    public ItemStack apply(ItemStack stack, LootContext context) {
        HeadDef head = MobHeads.of(stack);
        Entity victim = context.getOptional(LootContextParams.THIS_ENTITY);
        if (head == null || victim == null) {
            return stack;
        }
        String variant = Variants.of(head, victim);
        ItemStack out = variant.isEmpty() ? stack : MobHeads.stack(head, variant).copyWithCount(stack.getCount());
        String sound = Variants.soundOf(head, victim);
        if (!sound.isEmpty()) {
            out.set(DataComponents.NOTE_BLOCK_SOUND, Identifier.parse(sound));
        }
        return out;
    }
}
