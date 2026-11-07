package com.simplebuildingtools.mixin.client;


import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuildingtools.items.custom.OctantItem;
import com.simplebuildingtools.networking.OctantScrollPayload;
import com.simplebuildingtools.platform.ClientNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
/** Mouse wheel with the octant (Shift/Ctrl/Alt move corners or change the shape) and in the blueprint build mode (Ctrl turns). */
public class MouseMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void simplebuildingtools$onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();

        if (client.player != null && client.gui.screen() == null) {
            // Blaupausen-Baumodus: Strg+Mausrad dreht das Bauwerk in Viertelschritten.
            if (vertical != 0
                    && client.player.getMainHandItem().getItem() instanceof com.simplebuildingtools.items.custom.BuildingWandItem
                    && client.player.getOffhandItem().getItem() instanceof com.simplebuildingtools.items.custom.BlueprintItem
                    && (InputConstants.isKeyDown(InputConstants.KEY_LCONTROL)
                        || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL))) {
                ClientNetworking.send(new com.simplebuildingtools.networking.BlueprintRotatePayload((int) Math.signum(vertical)));
                ci.cancel();
                return;
            }
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
                boolean isControl = InputConstants.isKeyDown(InputConstants.KEY_LCONTROL)
                        || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
                boolean isAlt = InputConstants.isKeyDown(InputConstants.KEY_LALT)
                        || InputConstants.isKeyDown(InputConstants.KEY_RALT);

                if ((isShift || isControl || isAlt) && vertical != 0) {
                    int amount = (int) Math.signum(vertical);
                    ClientNetworking.send(new OctantScrollPayload(amount, isShift, isControl, isAlt));
                    ci.cancel();
                }
            }
        }
    }
}