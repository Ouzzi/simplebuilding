package com.simpleores.mixin;

import com.simpleores.client.GrateModels;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** N78b: the client option "3D grates" picks the 3D or the flat grate models while the block models load ({@link GrateModels}). */
@Mixin(ModelManager.class)
public abstract class ModelManagerGrateMixin {
    @Inject(method = "loadBlockModels", at = @At("RETURN"), cancellable = true)
    private static void simpleores$grates(ResourceManager manager, Executor executor,
                                          CallbackInfoReturnable<CompletableFuture<Map<Identifier, UnbakedModel>>> cir) {
        cir.setReturnValue(cir.getReturnValue().thenApply(GrateModels::apply));
    }
}
