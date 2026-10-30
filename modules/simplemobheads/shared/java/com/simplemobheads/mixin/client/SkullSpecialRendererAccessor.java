package com.simplemobheads.mixin.client;

import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.special.SkullSpecialRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** IG31-10: the model of a head item, to tell Mobheads' models from Vanilla's. */
@Mixin(SkullSpecialRenderer.class)
public interface SkullSpecialRendererAccessor {
    @Accessor("model")
    SkullModelBase simplemobheads$model();
}
