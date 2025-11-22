package com.simplemoney.mixin;

import com.simplemoney.items.custom.EnhancedFireworkRocketItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    // WIR INJIZIEREN AM ENDE VON travel(Vec3d movementInput)
    // und erhöhen den Vektor, um den harten Cap zu überschreiben.

    @Inject(
            method = "travel",
            at = @At("TAIL") // Füge Code am Ende der Methode ein
    )
    private void simplemoney$increaseMaxPlayerVelocity(Vec3d movementInput, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity)(Object)this;
        ItemStack heldItem = player.getMainHandStack();

        // Wenn der Spieler gleitet und unsere Rakete in der Hand hält (Proxy für aktive Rakete)
        if (player.isGliding() && heldItem.getItem() instanceof EnhancedFireworkRocketItem) {

            // RUFEN SIE HIER KEINE ZWEITE setVelocity auf!
            // Der Redirect in der FireworkRocketEntity sollte bereits einen hohen Vektor gesetzt haben.
            // Der Cap muss in der travel()-Logik selbst sein.

            // Um den Cap zu erhöhen, müssen wir die Velocity nach dem Cap neu setzen,
            // aber nur, wenn die aktuelle Geschwindigkeit sehr hoch ist (vom Server gesetzt).

            Vec3d currentVelocity = player.getVelocity();

            // Wenn die Velocity nach dem Aufruf von Vanilla-Logik unerwartet niedrig ist,
            // überschreiben wir sie HIER mit dem hohen Wert aus dem Server-Log (z.B. 10.0).
            // Dies ist unsicher, aber notwendig, wenn der Vanilla-Cap unbesiegbar ist.

            // Maximaler Boost-Wert basierend auf Multiplikator 5.5 und Base-Boost 0.5: 2.75
            // (Die Logs zeigen aber viel höhere akkumulierte Werte).

            double desiredMaxSpeed = 10.0; // Ein extremer Test-Cap (sollte sich 4.0 oder 1.8 widersetzen)

            if (currentVelocity.horizontalLengthSquared() < desiredMaxSpeed * desiredMaxSpeed) {
                // Wir nehmen an, dass der Vektor vom Server korrekt ist, aber vom Client/Server Cap reduziert wurde.
                // Wir setzen den Vektor auf die Richtung des Spielers mit dem gewünschen Cap.

                Vec3d rotationVector = player.getRotationVector();
                double currentHorizontalLength = currentVelocity.horizontalLength();

                if (currentHorizontalLength < desiredMaxSpeed) {
                    double boostFactor = (desiredMaxSpeed - currentHorizontalLength) * 0.1;

                    Vec3d newVelocity = currentVelocity.add(
                            rotationVector.x * boostFactor,
                            rotationVector.y * boostFactor,
                            rotationVector.z * boostFactor
                    );

                    player.setVelocity(newVelocity);
                    System.out.println(">>> CLIENT-SIDE VELOCITY ENFORCED: " + newVelocity);
                }
            }
        }
    }
}