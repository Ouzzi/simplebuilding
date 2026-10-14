package com.simplemobheads.client;

import com.simplebuilding.framework.api.WornHeads;
import com.simplemobheads.MobHeadType;
import com.simplemobheads.MobHeads;
import net.minecraft.world.item.ItemStack;

/**
 * What Simple Mobheads tells a super mod's extra head slot through framework {@link WornHeads} (owner N55): the variant a
 * head stack shows, and how big a worn head is drawn - as big as the head on the mob's own model
 * ({@link MobHeadModels#worn}), the same size the helmet slot gets from {@code CustomHeadLayerMixin}.
 */
public final class MobHeadLook implements WornHeads.SkullLook {
    @Override
    public Object type(Object stack, Object type) {
        if (stack instanceof ItemStack head && type instanceof MobHeadType mob) {
            String variant = head.get(MobHeads.VARIANT);
            if (variant != null) {
                return MobHeads.type(mob.head(), variant);
            }
        }
        return type;
    }

    @Override
    public float factor(Object type) {
        return type instanceof MobHeadType mob ? MobHeadModels.worn(mob)[0] : 1.0F;
    }

    @Override
    public float edge(Object type) {
        return type instanceof MobHeadType mob ? MobHeadModels.worn(mob)[1] : 0.0F;
    }
}
