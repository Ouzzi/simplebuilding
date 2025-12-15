package com.simplebuilding.mixin;

import net.minecraft.util.dynamic.Codecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Codecs.class)
public class CodecsMixin {

    // Wir ändern den Parameter 'max', wenn die Methode aufgerufen wird.
    // ordinal = 1 bedeutet: Der zweite int-Parameter (0 = min, 1 = max).
    @ModifyVariable(
            method = "rangedInt(IILjava/util/function/Function;)Lcom/mojang/serialization/Codec;",
            at = @At("HEAD"),
            ordinal = 1,
            argsOnly = true
    )
    private static int expandStackLimit(int max) {
        // Wenn Minecraft das Standard-Limit (99) für Stacks anfordert,
        // geben wir stattdessen 1024 zurück.
        if (max == 99) {
            return 1024;
        }
        return max;
    }
}