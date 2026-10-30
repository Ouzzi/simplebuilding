package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.simplebuilding.modules.simpleinterfaces.client.LeggingsFix;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Leggings fix (BS29-4): fills the missing underside of the leg part in loaded leggings textures (nothing is shipped; the pixels are derived at load).
 * In 26.3 resourceId() is declared on the superclass ReloadableTexture, so it is called through a cast, not shadowed
 * (a @Shadow must exist in the target class itself and crashed the client at start, A-MIXCHECK31).
 */
@Mixin(SimpleTexture.class)
public abstract class LeggingsTextureMixin {
    @Inject(method = "loadContents", at = @At("RETURN"))
    private void sb$fixLeggings(net.minecraft.server.packs.resources.ResourceManager manager, CallbackInfoReturnable<TextureContents> cir) {
        Identifier id = ((ReloadableTexture) (Object) this).resourceId();
        if (LeggingsFix.isLeggings(id.getPath())) LeggingsFix.fill(cir.getReturnValue().image());
    }
}
