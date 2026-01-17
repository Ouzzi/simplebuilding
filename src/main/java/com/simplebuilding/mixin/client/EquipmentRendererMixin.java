package com.simplebuilding.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.simplebuilding.util.GlowingTrimUtils;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EquipmentRenderer.class)
public class EquipmentRendererMixin {

    @ModifyVariable(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/TexturedRenderLayers;getArmorTrims(Z)Lnet/minecraft/client/render/RenderLayer;"
            ),
            argsOnly = true,
            ordinal = 0
    )
    private int makeTrimGlow(int light, @Local(ordinal = 0) ArmorTrim armorTrim, @Local(argsOnly = true) ItemStack stack) {
        if (armorTrim == null) return light;

        // Level abrufen (0, 1 oder 2)
        int level = GlowingTrimUtils.getGlowLevel(stack);

        if (level > 0) {
            // LEVEL 1: Statisches, volles Leuchten
            if (level == 1) {
                return LightmapTextureManager.MAX_LIGHT_COORDINATE;
            }

            // LEVEL 2: Pulsierendes "Überladen"-Leuchten
            if (level >= 2) {
                return simplebuilding$calculatePulsingLight();
            }
        }

        return light;
    }

    @Unique
    private int simplebuilding$calculatePulsingLight() {
        // 1. Geschwindigkeit erhöhen
        // Ein Divisor von 1000.0 ergab einen Zyklus von >6 Sekunden.
        // Ein Divisor von 150.0 ergibt ca. 1 Sekunde pro Puls -> wirkt "energetisch".
        double speedDivisor = 500.0;
        double time = System.currentTimeMillis() / speedDivisor;

        // 2. Sinus (-1.0 bis 1.0)
        double sine = Math.sin(time);

        // 3. Normalisieren (0.0 bis 1.0)
        double normalized = (sine + 1.0) / 2.0;

        // 4. Lichtbereich definieren
        // TIPP: Wenn wir von 0 bis 15 gehen, sind die Sprünge visuell sehr hart.
        // Wenn wir von 5 bis 15 gehen, wirkt das "Leuchten" stabiler, aber pulsiert immer noch deutlich.
        // Ich habe es hier auf 1 bis 15 gesetzt, damit es nicht ganz schwarz wird (was wie ein Bug aussieht).
        double minLight = 1.0;
        double maxLight = 20.0;

        // 5. Wert berechnen
        double val = minLight + (normalized * (maxLight - minLight));

        // 6. RUNDEN statt abschneiden (WICHTIG für weiche Übergänge)
        // 14.9 wird zu 15, nicht zu 14.
        int lightValue = (int) Math.round(val);

        // Clamping (zur Sicherheit, falls Mathe-Rundungsfehler auftreten)
        if (lightValue > 15) lightValue = 15;
        if (lightValue < 0) lightValue = 0;

        return LightmapTextureManager.pack(lightValue, lightValue);
    }
}