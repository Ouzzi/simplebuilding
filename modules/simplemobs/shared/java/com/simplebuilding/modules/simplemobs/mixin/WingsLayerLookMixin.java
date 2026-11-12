package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.WitherWings;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Worn wings follow the chosen wings look (a-w1b, PLAN40 simplemobs-4, ABN30-9): Vanilla's WingsLayer draws the equipment asset of
 * the chest item; for the four Simple Mobs wings with look 2 chosen it draws {@code simplemobs:<wings>_v2} instead. Look 1, no
 * Dev variants panel and every other asset (Elytra, other mods) pass through unchanged ({@link WitherWings#wornAsset}).
 */
@Mixin(WingsLayer.class)
public abstract class WingsLayerLookMixin {
    @ModifyArg(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/EquipmentLayerRenderer;renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V"),
            index = 1)
    private ResourceKey<EquipmentAsset> simplemobs$wingsLook(ResourceKey<EquipmentAsset> asset) {
        return WitherWings.wornAsset(asset);
    }
}
