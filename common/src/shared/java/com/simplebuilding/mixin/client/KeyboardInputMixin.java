package com.simplebuilding.mixin.client;

import com.simplebuilding.client.GaugeAutowalk;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Messuhr-Autowalk unter offenen, nicht pausierenden Bildschirmen (Besitzer 2026-10-02: auch im Inventar, im Chat,
 * in Truhen weiterlaufen). Vanilla liest die Bewegung aus den Tastenzustaenden; bei offenem Bildschirm ist die
 * Vorwaertstaste losgelassen (und unter NeoForge/Forge meldet {@code keyUp.isDown()} wegen ihres
 * Konfliktkontexts "im Spiel" dann ohnehin nichts). Darum setzt der Autowalk hier, nach dem Einlesen, "vorwaerts"
 * selbst - genau wie eine gehaltene Taste: rueckwaerts dazu hebt sich auf, seitwaerts mischt sich ein.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$gaugeAutowalk(CallbackInfo ci) {
        if (!GaugeAutowalk.forcesForward() || this.keyPresses.forward()) {
            return;
        }
        Input in = this.keyPresses;
        this.keyPresses = new Input(true, in.backward(), in.left(), in.right(), in.jump(), in.shift(), in.sprint());
        float forward = in.backward() ? 0.0F : 1.0F;
        float strafe = in.left() == in.right() ? 0.0F : in.left() ? 1.0F : -1.0F;
        this.moveVector = new Vec2(strafe, forward).normalized();
    }
}
