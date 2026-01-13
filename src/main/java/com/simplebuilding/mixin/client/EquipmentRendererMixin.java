package com.simplebuilding.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.util.GlowingTrimUtils;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.item.equipment.trim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EquipmentRenderer.class)
public class EquipmentRendererMixin {

    /**
     * Wir nutzen ModifyVariable, um das Licht-Level (light) zu ändern.
     * Der Ankerpunkt (@At) ist der Aufruf von "getArmorTrims".
     * Dieser Aufruf passiert NUR im Code-Block für Armor Trims.
     */
    @ModifyVariable(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/TexturedRenderLayers;getArmorTrims(Z)Lnet/minecraft/client/render/RenderLayer;"
            ),
            argsOnly = true, // 'light' ist ein Argument der Methode
            ordinal = 0      // Es ist das erste Argument vom Typ 'int' (Index 8 in der Signatur, aber ordinal 0 für ints)
    )
    private int makeTrimGlow(int light, @Local(ordinal = 0) ArmorTrim armorTrim) {
        // Wenn ein Trim vorhanden ist UND es unser Glowing Trim ist -> MAX LICHT
        Simplebuilding.LOGGER.info("Making Trim Glow");
        if (armorTrim != null && GlowingTrimUtils.isVisualGlowingTrim(armorTrim)) {
            return LightmapTextureManager.MAX_LIGHT_COORDINATE; // 15728880 (Fullbright)
        }
        return light; // Sonst normales Licht
    }
}