package com.simplebuilding.mixin;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.util.GlowingTrimUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "appendTooltip", at = @At("TAIL"))
    private void simplebuilding$appendTrimBuffs(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplayComponent displayComponent,
            Consumer<Text> textConsumer,
            TooltipType type,
            CallbackInfo ci
    ) {
        // 1. Glowing Level anzeigen (Radiance)
        int glowLevel = GlowingTrimUtils.getGlowLevel(stack);
        if (glowLevel > 0) {
            textConsumer.accept(Text.empty()); // Leerzeile
            textConsumer.accept(Text.literal("Radiance Level: " + glowLevel + "/5").formatted(Formatting.GOLD));
            textConsumer.accept(Text.literal(" Light Level: " + (glowLevel * 3)).formatted(Formatting.DARK_GRAY));
        }

        // 2. Armor Trim Boni anzeigen
        // Wir prüfen sicherheitshalber, ob das Item überhaupt eine Trim-Komponente hat.
        var optionalTrim = stack.get(DataComponentTypes.TRIM);

        if (optionalTrim != null) {
            // Wir holen den Pfad der Pattern-ID (z.B. "sentry", "vex", etc.)
            String id = optionalTrim.pattern().value().assetId().getPath();

            // Leerzeile vor den Boni sieht oft besser aus
            if (glowLevel == 0) { // Nur wenn nicht schon durch Radiance eine da ist
                // textConsumer.accept(Text.empty()); // Optional: Leerzeile einfügen
            }

            // --- Overworld & Strukturen ---

            if (id.contains("sentry")) { // Pillager Outpost
                textConsumer.accept(Text.literal("Trim Bonus: Projectile Dampening (+1.5%)").formatted(Formatting.BLUE));
            }
            else if (id.contains("vex")) { // Woodland Mansion
                textConsumer.accept(Text.literal("Trim Bonus: Magic Dampening (+2%)").formatted(Formatting.DARK_PURPLE));
            }
            else if (id.contains("wild")) { // Jungle Temple
                textConsumer.accept(Text.literal("Trim Bonus: Trap Resilience (+2.5%)").formatted(Formatting.DARK_GREEN));
            }
            else if (id.contains("coast")) { // Shipwreck
                textConsumer.accept(Text.literal("Trim Bonus: Oxygen Efficiency").formatted(Formatting.AQUA));
            }
            else if (id.contains("dune")) { // Desert Pyramid
                textConsumer.accept(Text.literal("Trim Bonus: Blast Dampening (+1.5%)").formatted(Formatting.GOLD));
            }

            // --- Trail Ruins (Archäologie) ---

            else if (id.contains("wayfinder")) {
                textConsumer.accept(Text.literal("Trim Bonus: Travel Efficiency").formatted(Formatting.YELLOW));
            }
            else if (id.contains("raiser")) {
                textConsumer.accept(Text.literal("Trim Bonus: Experience Affinity (+1%)").formatted(Formatting.GREEN));
            }
            else if (id.contains("shaper")) {
                textConsumer.accept(Text.literal("Trim Bonus: Durability Retention (1%)").formatted(Formatting.LIGHT_PURPLE));
            }
            else if (id.contains("host")) {
                textConsumer.accept(Text.literal("Trim Bonus: Trade Negotiator").formatted(Formatting.WHITE));
            }

            // --- Deep Dark ---

            else if (id.contains("ward")) { // Ancient City
                textConsumer.accept(Text.literal("Trim Bonus: Damage Absorption").formatted(Formatting.DARK_AQUA));
            }
            else if (id.contains("silence")) { // Ancient City
                textConsumer.accept(Text.literal("Trim Bonus: Stealth Mobility (+3%)").formatted(Formatting.DARK_GRAY));
            }

            // --- Ocean ---

            else if (id.contains("tide")) { // Ocean Monument
                textConsumer.accept(Text.literal("Trim Bonus: Swim Agility (+2%)").formatted(Formatting.DARK_AQUA));
            }

            // --- Nether ---

            else if (id.contains("snout")) { // Bastion
                textConsumer.accept(Text.literal("Trim Bonus: Knockback Resistance").formatted(Formatting.GOLD));
            }
            else if (id.contains("rib")) { // Fortress
                textConsumer.accept(Text.literal("Trim Bonus: Wither Resistance (+5%)").formatted(Formatting.DARK_RED));
            }

            // --- End ---

            else if (id.contains("eye")) { // Stronghold
                textConsumer.accept(Text.literal("Trim Bonus: Ender Stability").formatted(Formatting.LIGHT_PURPLE));
            }
            else if (id.contains("spire")) { // End City
                textConsumer.accept(Text.literal("Trim Bonus: Feather Falling (+2%)").formatted(Formatting.LIGHT_PURPLE));
            }

            // --- 1.21 Trial Chambers ---

            else if (id.contains("flow")) { // Trial Chambers
                textConsumer.accept(Text.literal("Trim Bonus: Aerial Agility").formatted(Formatting.WHITE));
            }
            else if (id.contains("bolt")) { // Trial Chambers
                textConsumer.accept(Text.literal("Trim Bonus: Kinetic Response").formatted(Formatting.GOLD));
            }
        }
    }

    @Inject(method = "appendTooltip", at = @At("HEAD"))
    private void simplebuilding$addUpgradeTooltips(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplayComponent displayComponent,
            Consumer<Text> textConsumer,
            TooltipType type,
            CallbackInfo ci
    ) {
        // Prüfen auf Visual Glow
        if (Boolean.TRUE.equals(stack.get(ModDataComponentTypes.VISUAL_GLOW))) {
            textConsumer.accept(Text.literal("✨ Glowing Trim").formatted(Formatting.AQUA));
        }

        // Prüfen auf Light Source
        if (Boolean.TRUE.equals(stack.get(ModDataComponentTypes.LIGHT_SOURCE))) {
            textConsumer.accept(Text.literal("💡 Emits Light").formatted(Formatting.GOLD));
        }
    }
}