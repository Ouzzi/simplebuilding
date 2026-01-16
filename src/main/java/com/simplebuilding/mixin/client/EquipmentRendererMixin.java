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

        // Level abrufen (0, 1 oder 2)
        int level = GlowingTrimUtils.getGlowLevel(stack);

        if (level > 0) {
            // LEVEL 1: Statisches, volles Leuchten
            if (level == 1) {
                return LightmapTextureManager.MAX_LIGHT_COORDINATE;
            }

            // LEVEL 2: Pulsierendes "Überladen"-Leuchten
            if (level >= 2) {
                return simplebuilding$calculatePulsingLight();
            }
        }

        return light;
    }

    @Unique
    private int simplebuilding$calculatePulsingLight() {
        // Schnelles, energetisches Flackern
        double speed = 80.0;
        double offset = Math.sin(System.currentTimeMillis() / speed);

        // Meistens volle Helligkeit (15), aber kurze "Aussetzer" auf 13
        if (offset > 0.7) {
            // Lightmap Pack: (BlockLight << 4) | (SkyLight << 20)
            // Wir nutzen hier 13 für Block und Sky statt 15
            return LightmapTextureManager.pack(13, 13);
        }

        return LightmapTextureManager.MAX_LIGHT_COORDINATE;
    }
}