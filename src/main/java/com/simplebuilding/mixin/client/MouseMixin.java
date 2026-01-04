package com.simplebuilding.mixin.client;

import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.networking.OctantScrollPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
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

                // --- FIX: Lock Check ---
                // Prüfen, ob das Item "locked" ist. Wenn ja, erlauben wir kein Scrollen über das Item
                // und lassen das Event ganz normal weiterlaufen (z.B. für Hotbar-Wechsel) oder brechen ab.
                // Laut Anforderung soll das Verstellen per Tasten NICHT möglich sein.

                NbtComponent nbt = client.player.getMainHandStack().getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
                if (nbt.copyNbt().getBoolean("Locked", false)) {
                    // Wenn gelockt, ignorieren wir die Shift/Ctrl-Logik hier einfach.
                    // Das Event wird NICHT gecancelt, d.h. normales Minecraft Verhalten (Hotbar scrollen) ist möglich,
                    // aber die Octant-Werte ändern sich nicht.
                    return;
                }
                // -----------------------

                boolean isShift = client.options.sneakKey.isPressed();
                boolean isControl = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                        || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
                boolean isAlt = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_ALT)
                        || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);

                if ((isShift || isControl || isAlt) && vertical != 0) {
                    int amount = (int) Math.signum(vertical);
                    ClientPlayNetworking.send(new OctantScrollPayload(amount, isShift, isControl, isAlt));
                    ci.cancel();
                }
            }
        }
    }
}