package com.simplebuilding.mixin.client;

import com.simplebuilding.version.McClientVersion;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.networking.OctantScrollPayload;
import com.simplebuilding.platform.ClientNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();

        if (client.player != null && client.gui.screen() == null) {
            // Abgestelltes Buendel: Schleichen + Mausrad waehlt das gezeigte Item (der Server besitzt
            // den Index); die Hotbar bleibt dabei stehen.
            if (vertical != 0 && client.player.isShiftKeyDown() && !client.player.isSpectator()
                    && client.hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit
                    && client.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                    && client.level != null
                    && client.level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof com.simplebuilding.blocks.custom.PlacedBundleBlock) {
                // Rad nach unten = naechstes Item, wie in der Hotbar.
                ClientNetworking.send(new com.simplebuilding.networking.PlacedBundleScrollPayload(
                        blockHit.getBlockPos(), vertical < 0 ? 1 : -1));
                ci.cancel();
                return;
            }
            // Blaupausen-Baumodus: Strg+Mausrad dreht das Bauwerk in Viertelschritten.
            if (vertical != 0
                    && client.player.getMainHandItem().getItem() instanceof com.simplebuilding.items.custom.BuildingWandItem
                    && client.player.getOffhandItem().getItem() instanceof com.simplebuilding.items.custom.BlueprintItem
                    && (McClientVersion.isKeyDown(client, InputConstants.KEY_LCONTROL)
                        || McClientVersion.isKeyDown(client, InputConstants.KEY_RCONTROL))) {
                ClientNetworking.send(new com.simplebuilding.networking.BlueprintRotatePayload((int) Math.signum(vertical)));
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
                boolean isControl = McClientVersion.isKeyDown(client, InputConstants.KEY_LCONTROL)
                        || McClientVersion.isKeyDown(client, InputConstants.KEY_RCONTROL);
                boolean isAlt = McClientVersion.isKeyDown(client, InputConstants.KEY_LALT)
                        || McClientVersion.isKeyDown(client, InputConstants.KEY_RALT);

                if ((isShift || isControl || isAlt) && vertical != 0) {
                    int amount = (int) Math.signum(vertical);
                    ClientNetworking.send(new OctantScrollPayload(amount, isShift, isControl, isAlt));
                    ci.cancel();
                }
            }
        }
    }
}