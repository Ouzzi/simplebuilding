package com.simplebuilding.mixin.client;

import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.networking.OctantScrollPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null && client.currentScreen == null) {
            if (client.player.getMainHandStack().getItem() instanceof OctantItem) {

                boolean isShift = client.options.sneakKey.isPressed();

                // Control
                boolean isControl = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                        || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);

                // NEU: Alt Taste
                boolean isAlt = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_ALT)
                        || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);

                // Wenn Shift ODER Control ODER Alt gedrückt ist...
                if ((isShift || isControl || isAlt) && vertical != 0) {
                    int amount = (int) Math.signum(vertical);

                    // Payload mit 'isAlt' senden
                    ClientPlayNetworking.send(new OctantScrollPayload(amount, isShift, isControl, isAlt));

                    ci.cancel();
                }
            }
        }
    }
}