package com.simplebuilding.tweaks.mixin;

import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Zugriff auf die privaten Felder der XP-Kugel fuers Verklumpen (Simple Tweaks: ExperienceOrbInvoker). */
@Mixin(ExperienceOrb.class)
public interface ExperienceOrbAccessor {
    @Invoker("setValue")
    void simplebuilding$setValue(int value);

    @Accessor("count")
    int simplebuilding$getCount();

    @Accessor("count")
    void simplebuilding$setCount(int count);

    @Accessor("age")
    void simplebuilding$setAge(int age);
}
