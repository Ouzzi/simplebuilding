package com.simplebuilding.mixin.forge;

import com.simplebuilding.blocks.entity.custom.AutoSmitherBlockEntity;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep Forge hoppers and pipes subject to the Auto Smither's output-only extraction. */
@Mixin(BaseContainerBlockEntity.class)
public abstract class AutoSmitherItemHandlerMixin {
    @Inject(method = "createUnSidedHandler", at = @At("HEAD"), cancellable = true, remap = false)
    private void simplebuilding$smitherItemHandler(CallbackInfoReturnable<IItemHandler> cir) {
        if (McVersion.AUTO_SMITHER && (Object) this instanceof AutoSmitherBlockEntity smither) {
            // All faces share the same rules, including unsided capability queries. The base
            // class still owns the LazyOptional and its invalidation/revival lifecycle.
            cir.setReturnValue(new SidedInvWrapper(smither, Direction.DOWN));
        }
    }
}
