package com.simplemaps.mixin.client;

import com.simplemaps.MapsItems;
import com.simplemaps.client.MapsClient;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feature 4: a framed wayfinder map fills the frame like a Vanilla map and shows its stored view. */
@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameRendererMixin {
    /** Marker id: the renderer only checks that a map id is present. */
    @Unique private static final MapId SIMPLEMAPS$FRAMED = new MapId(Integer.MIN_VALUE);
    @Shadow @Final private BlockModelResolver blockModelResolver;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V",
            at = @At("TAIL"))
    private void simplemaps$framedWayfinder(ItemFrame frame, ItemFrameRenderState state, float partialTicks, CallbackInfo ci) {
        if (!MapsItems.isWayfinder(frame.getItem()) || !MapsClient.fillFrameState(frame, frame.getItem(), state.mapRenderState)) return;
        state.mapId = SIMPLEMAPS$FRAMED;
        state.rotation = 0;
        if (!state.isInvisible) blockModelResolver.updateForItemFrame(state.frameModel, state.isGlowFrame, true);
    }
}
