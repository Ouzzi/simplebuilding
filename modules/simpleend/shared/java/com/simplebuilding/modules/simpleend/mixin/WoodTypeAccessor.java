package com.simplebuilding.modules.simpleend.mixin;

import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** WoodType.register is private; the End woods join the type list like Vanilla woods. */
@Mixin(WoodType.class)
public interface WoodTypeAccessor {
    @Invoker("register")
    static WoodType simpleend$register(WoodType type) {
        throw new AssertionError();
    }
}
