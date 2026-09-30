package com.simplebuilding.modules.simplemodels.mixin;
import com.simplebuilding.modules.simplemodels.client.ModelBrowser;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** When a server disables the module, approved appearances render as their true base items. */
@Mixin(ItemModelResolver.class)
public abstract class ModelRenderPolicyMixin {
    @ModifyVariable(method = "appendItemLayers", at = @At("HEAD"), argsOnly = true)
    private ItemStack simplemodels$visibleIdentity(ItemStack input) { return ModelBrowser.displayStack(input); }
}
