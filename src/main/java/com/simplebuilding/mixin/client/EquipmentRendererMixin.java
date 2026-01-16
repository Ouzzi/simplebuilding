package com.simplebuilding.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.simplebuilding.util.GlowingTrimUtils;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.trim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
    private int makeTrimGlow(int light, @Local(ordinal = 0) ArmorTrim armorTrim, @Local(argsOnly = true) ItemStack stack) {
        if (armorTrim == null) return light;
        int level = GlowingTrimUtils.getGlowLevel(stack);

        if (level > 0) {
            if (level == 1) {
                return LightmapTextureManager.MAX_LIGHT_COORDINATE;
            }
            if (level >= 2) {
                return simplebuilding$calculatePulsingLight();
            }
        }

        return light;
    }

    // Hilfsmethode für den Pulsier-Effekt
    @Unique
    private int simplebuilding$calculatePulsingLight() {
        double speed = 100.0;

        double offset = Math.sin(System.currentTimeMillis() / speed);
        if (offset > 0.8) {
            return LightmapTextureManager.pack(13, 13); // Etwas dunkler (13 statt 15)
        }

        return LightmapTextureManager.MAX_LIGHT_COORDINATE;
    }
}