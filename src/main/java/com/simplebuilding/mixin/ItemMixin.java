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
        // 1. Glowing und Emitting Level anzeigen (Radiance)
        int emittingLevel = GlowingTrimUtils.getEmissionLevel(stack);
        int glowLevel = GlowingTrimUtils.getGlowLevel(stack);
        if (emittingLevel > 0) {textConsumer.accept(Text.translatable("tooltip.simplebuilding.radiance_level", emittingLevel).formatted(Formatting.GOLD));}
        if (glowLevel > 0) {
            textConsumer.accept(Text.translatable(glowLevel == 2 ? "tooltip.simplebuilding.glow_level_2" : "tooltip.simplebuilding.glow_level", glowLevel).formatted(Formatting.AQUA));
        }


        // 2. Armor Trim Boni anzeigen
        // Wir prüfen sicherheitshalber, ob das Item überhaupt eine Trim-Komponente hat.
        var optionalTrim = stack.get(DataComponentTypes.TRIM);

        if (optionalTrim != null) {
            // Wir holen den Pfad der Pattern-ID (z.B. "sentry", "vex", etc.)
            String id = optionalTrim.pattern().value().assetId().getPath();
            String materialId = optionalTrim.material().getKey().map(key -> key.getValue().getPath()).orElse("");

            if (emittingLevel > 0 || glowLevel > 0) {textConsumer.accept(Text.empty());}

            if (materialId.contains("diamond")) {
                textConsumer.accept(Text.literal("Material: Hard Shell (-2.5% Dmg)").formatted(Formatting.AQUA));
            }
            else if (materialId.contains("gold")) {
                textConsumer.accept(Text.literal("Material: Magic Dampening (-5%)").formatted(Formatting.GOLD));
            }
            else if (materialId.contains("iron")) {
                textConsumer.accept(Text.literal("Material: Blunt Resistance (-3%)").formatted(Formatting.GRAY));
            }
            else if (materialId.contains("netherite")) {
                textConsumer.accept(Text.literal("Material: Pattern Boost (+50%)").formatted(Formatting.DARK_GRAY));
            }
            // -- NEUE MATERIALIEN --
            else if (materialId.contains("emerald")) {
                // Smaragd: Oft mit Handel oder Illagern verbunden. Vorschlag: Schutz gegen Illager.
                textConsumer.accept(Text.literal("Material: Illager Resistance (-3%)").formatted(Formatting.DARK_GREEN));
            }
            else if (materialId.contains("copper")) {
                // Kupfer: Leitet Blitz. Vorschlag: Blitzschutz oder Wasser-Affinität (Oxidierung).
                textConsumer.accept(Text.literal("Material: Lightning Rod (-5% Ltn)").formatted(Formatting.GOLD));
            }
            else if (materialId.contains("redstone")) { // Tippfehler korrigiert (war 'readstone')
                // Redstone: Fallen/Mechanik. Vorschlag: Schutz gegen Fallen (Pfeile aus Dispensern etc).
                textConsumer.accept(Text.literal("Material: Trap Awareness (-3%)").formatted(Formatting.RED));
            }
            else if (materialId.contains("quartz")) {
                // Quarz: Nether. Vorschlag: Feuerschutz.
                textConsumer.accept(Text.literal("Material: Heat Shield (-3% Fire)").formatted(Formatting.WHITE));
            }
            else if (materialId.contains("amethyst")) {
                // Amethyst: Schall/Vibration. Vorschlag: Ähnlich wie Ward (Sonic Boom Schutz).
                textConsumer.accept(Text.literal("Material: Resonance Shield (-3%)").formatted(Formatting.LIGHT_PURPLE));
            }
            else if (materialId.contains("lapis")) {
                // Lapis: Verzauberung/Glück. Schwer als Dmg-Reduction. Vorschlag: "Bad Omen" oder Magie-Resistenz (wie Gold).
                textConsumer.accept(Text.literal("Material: Curse Dampening (-3%)").formatted(Formatting.BLUE));
            }



        // ---------------------------

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

}