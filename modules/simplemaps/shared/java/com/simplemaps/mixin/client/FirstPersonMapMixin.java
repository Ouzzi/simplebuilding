package com.simplemaps.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simplemaps.MapsItems;
import com.simplemaps.client.MapsClient;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The wayfinder map in hand is held and drawn like a Vanilla map, showing the area around the holder. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonMapMixin {
    /** Vanilla / Fabric: {@code stack.has(MAP_ID)}. */
    @ModifyExpressionValue(method = "submitArmWithItem", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z"))
    private boolean simplemaps$holdLikeMap(boolean original, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) ItemStack stack) {
        return original || MapsItems.isWayfinder(stack);
    }

    /** NeoForge's patched renderer asks {@code stack.getItem() instanceof MapItem} instead (the Fabric target is absent there and crashed the client at startup). */
    @ModifyExpressionValue(method = "submitArmWithItem", require = 0,
            at = @At(value = "INSTANCEOF", args = "class=net/minecraft/world/item/MapItem"))
    private boolean simplemaps$holdLikeMapInstanceof(boolean original, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) ItemStack stack) {
        return original || MapsItems.isWayfinder(stack);
    }

    @Inject(method = "renderMap", at = @At("HEAD"))
    private void simplemaps$wayfinderPicture(PoseStack poseStack, SubmitNodeCollector collector, int light, ItemStack stack,
                                             boolean mainHand, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
        if (!MapsItems.isWayfinder(stack)) return;
        boolean ready = MapsClient.fillHandState(stack, mainHand, mainHand ? state.mainHandMapRenderState : state.offHandMapRenderState);
        if (mainHand) state.hasMainHandMapData = ready;
        else state.hasOffHandMapData = ready;
    }
}
