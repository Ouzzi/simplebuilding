package com.simplebuilding.mixin;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.simplebuilding.datafix.ModDataFixer;
import net.minecraft.util.datafix.DataFixTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every vanilla world upgrade - chunks, entity chunks, players, saved data, structure files - goes
 * through {@code DataFixTypes#update(DataFixer, Dynamic, int, int)} (the CompoundTag overloads and
 * the Codec wrapper delegate to it; identical on 26.2 and 26.3). Right after vanilla is done, the
 * mod's own data gets the same version range, see {@link ModDataFixer}.
 */
@Mixin(DataFixTypes.class)
public abstract class DataFixTypesMixin {

    @Inject(method = "update(Lcom/mojang/datafixers/DataFixer;Lcom/mojang/serialization/Dynamic;II)Lcom/mojang/serialization/Dynamic;",
            at = @At("RETURN"), cancellable = true)
    private <T> void simplebuilding$fixModData(DataFixer fixer, Dynamic<T> input, int fromVersion, int toVersion,
                                               CallbackInfoReturnable<Dynamic<T>> cir) {
        Dynamic<T> fixed = cir.getReturnValue();
        Dynamic<T> withModData = ModDataFixer.afterVanilla(fixer, fixed, fromVersion, toVersion);
        if (withModData != fixed) {
            cir.setReturnValue(withModData);
        }
    }
}
