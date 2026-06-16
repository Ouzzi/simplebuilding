package com.simplebuilding.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.networking.OctantScrollPayload;
import com.simplebuilding.platform.ClientNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();

        if (client.player != null && client.screen == null) {
            if (client.player.getMainHandItem().getItem() instanceof OctantItem) {

                // --- FIX: Lock Check ---
                // Prüfen, ob das Item "locked" ist. Wenn ja, erlauben wir kein Scrollen über das Item
                // und lassen das Event ganz normal weiterlaufen (z.B. für Hotbar-Wechsel) oder brechen ab.
                // Laut Anforderung soll das Verstellen per Tasten NICHT möglich sein.

                CustomData nbt = client.player.getMainHandItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                if (nbt.copyTag().getBooleanOr("Locked", false)) {
                    // Wenn gelockt, ignorieren wir die Shift/Ctrl-Logik hier einfach.
                    // Das Event wird NICHT gecancelt, d.h. normales Minecraft Verhalten (Hotbar scrollen) ist möglich,
                    // aber die Octant-Werte ändern sich nicht.
                    return;
                }
                // -----------------------

                boolean isShift = client.options.keyShift.isDown();
                boolean isControl = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                        || InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
                boolean isAlt = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_ALT)
                        || InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);

                if ((isShift || isControl || isAlt) && vertical != 0) {
                    int amount = (int) Math.signum(vertical);
                    ClientNetworking.send(new OctantScrollPayload(amount, isShift, isControl, isAlt));
                    ci.cancel();
                }
            }
        }
    }
}