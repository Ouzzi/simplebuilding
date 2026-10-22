package com.simplemobheads.mixin.client;

import com.simplemobheads.client.TechnobladeCrownLayer;
import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.client.renderer.entity.state.PigRenderState;
import net.minecraft.world.entity.animal.pig.Pig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Technoblade (EH21 TO-1): the pig renderer gets the crown layer (drawn only for a crowned pig). */
@Mixin(PigRenderer.class)
public abstract class PigRendererMixin extends MobRenderer<Pig, PigRenderState, PigModel> {
    protected PigRendererMixin(EntityRendererProvider.Context context, PigModel model, float shadow) {
        super(context, model, shadow);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void simplemobheads$crownLayer(EntityRendererProvider.Context context, CallbackInfo ci) {
        this.addLayer(new TechnobladeCrownLayer(this));
    }
}
