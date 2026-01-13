package com.simplebuilding.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.util.GlowingTrimUtils;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.item.ItemStack; // Import benötigt
import net.minecraft.item.equipment.trim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EquipmentRenderer.class)
public class EquipmentRendererMixin {

    @ModifyVariable(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/TexturedRenderLayers;getArmorTrims(Z)Lnet/minecraft/client/render/RenderLayer;"
            ),
            argsOnly = true,
            ordinal = 0
    )
    // Wir holen uns zusätzlich den ItemStack mit @Local(argsOnly = true)
    private int makeTrimGlow(int light, @Local(ordinal = 0) ArmorTrim armorTrim, @Local(argsOnly = true) ItemStack stack) {
        // Leuchtet nur, wenn ein Trim da ist UND das "Visual Glow" Upgrade installiert wurde
        if (armorTrim != null && GlowingTrimUtils.hasVisualGlow(stack)) {
            // Simplebuilding.LOGGER.info("Making Trim Glow for " + stack.getItem().getName().getString());
            return LightmapTextureManager.MAX_LIGHT_COORDINATE;
        }
        return light;
    }
}