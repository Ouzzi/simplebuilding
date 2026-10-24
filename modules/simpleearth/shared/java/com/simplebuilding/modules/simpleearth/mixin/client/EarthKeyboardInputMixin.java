package com.simplebuilding.modules.simpleearth.mixin.client;

import com.simplebuilding.modules.simpleearth.client.EarthClient;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Parked in orbit (Simple Earth): the horizontal movement keys glide the orbit position instead of moving the figure - the
 * input is read here after Vanilla and handed to {@link EarthClient}; up/down (jump, sneak) stay Vanilla.
 */
@Mixin(KeyboardInput.class)
public abstract class EarthKeyboardInputMixin extends ClientInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void simpleearth$glide(CallbackInfo ci) {
        if (!EarthClient.parked()) {
            EarthClient.glideStrafe = 0;
            EarthClient.glideForward = 0;
            return;
        }
        EarthClient.glideStrafe = this.moveVector.x;
        EarthClient.glideForward = this.moveVector.y;
        Input in = this.keyPresses;
        this.keyPresses = new Input(false, false, false, false, in.jump(), in.shift(), in.sprint());
        this.moveVector = Vec2.ZERO;
    }
}
