package com.simplebuilding.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.simplebuilding.client.render.BlockHighlightRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(
            method = "render",
            at = @At(value = "RETURN")
    )
    private void onRender(
            ObjectAllocator allocator,
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            Matrix4f positionMatrix, // <--- DAS ist der Schlüssel! Die fertige Matrix.
            Matrix4f matrix4f,
            Matrix4f projectionMatrix,
            GpuBufferSlice fogBuffer,
            Vector4f fogColor,
            boolean renderSky,
            CallbackInfo ci
    ) {
        // Wir übergeben die fertige Positions-Matrix direkt an unseren Renderer.
        // Keine manuelle Rotation mehr nötig!
        BlockHighlightRenderer.render(positionMatrix, camera);
    }
}