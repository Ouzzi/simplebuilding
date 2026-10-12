package com.simplebuilding.modules.simplemobs.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Skin layer flags, copied onto a player disguise stand-in. */
@Mixin(Avatar.class)
public interface AvatarAccessor {
    @Accessor("DATA_PLAYER_MODE_CUSTOMISATION")
    static EntityDataAccessor<Byte> simplemobs$customisation() { throw new AssertionError(); }
}
