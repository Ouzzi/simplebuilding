package com.simplebuilding.modules.simpletools.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Tests age a recorded death drop to its despawn without waiting five minutes. */
@Mixin(ItemEntity.class)
public interface ItemEntityAgeAccessor {
    @Accessor("age")
    void simpletools$setAge(int age);
}
