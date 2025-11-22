package com.simplemoney.mixin;

import com.simplemoney.entity.custom.EnhancedFireworkRocket;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {

    @Shadow @Nullable private LivingEntity shooter;

    // Wir verwenden @Redirect, um den AUFRUF der setVelocity-Methode INNERHALB der tick() Methode
    // der FireworkRocketEntity zu kapern und unseren eigenen Vektor zu liefern.
    // Zielen auf den Aufruf von setVelocity auf dem Shooter (Spieler):
    @Redirect(
            method = "tick",
            // Wir zielen auf den Call der setVelocity-Methode, die den Boost setzt.
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"
                    // Dies zielt auf die Zeile, die die Beschleunigung zuweist:
                    // this.shooter.setVelocity(vec3d2.add(vec3d.x * 0.1 + (vec3d.x * 1.5 - vec3d2.x) * 0.5, ...));
            )
    )
    private void simplemoney$redirectSetVelocity(LivingEntity shooter, Vec3d vanillaVelocity) {

        // Fängt den Aufruf ab, der die neue Geschwindigkeit setzt.
        // shooter ist der LivingEntity-Parameter (der Spieler).
        // vanillaVelocity ist der Vektor, den Minecraft setzen wollte.

        if (this.shooter != null && this.shooter.isGliding() &&  (FireworkRocketEntity)(Object)this instanceof EnhancedFireworkRocket enhancedRocket)
        {
            double multiplier = enhancedRocket.getSpeedMultiplier(); // Ihr 5.5er Multiplikator
            Vec3d rotationVector = this.shooter.getRotationVector();
            Vec3d currentVelocity = this.shooter.getVelocity();

            // --- VEREINFACHTE AGGRESSIVE BOOST-FORMEL ---

            // 1. Wir überspringen die komplexe Vanilla-Formel und addieren einen direkten Schub.
            // 2. Basis-Schub-Multiplikator: 0.5 (aus der Vanilla-Formel abgeleitet)
            double baseBoost = 0.5;

            Vec3d boostVector = rotationVector.multiply(baseBoost * multiplier);

            // Setze die Geschwindigkeit als eine Addition aus aktuellen Bewegungen und dem Boost.
            Vec3d newVelocity = currentVelocity.add(boostVector);

            // Setze die Geschwindigkeit über den ursprünglichen Shooter-Aufruf
            shooter.setVelocity(newVelocity);

            System.out.println(">>> REDIRECT SUCCESS: New Velocity set to: " + newVelocity);

        } else {
            // Wenn es NICHT unsere Rakete ist, rufen wir die ursprüngliche Methode mit dem ursprünglichen Vektor auf.
            shooter.setVelocity(vanillaVelocity);
        }
    }
}