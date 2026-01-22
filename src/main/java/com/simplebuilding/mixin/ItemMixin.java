package com.simplebuilding.mixin;

import com.simplebuilding.util.GlowingTrimUtils;
import com.simplebuilding.util.TrimEffectUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        // Client-Side Check für den Player (für Multiplikator)
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        // Globaler Multiplikator (Level + Survival Stats)
        float mult = TrimEffectUtil.getGlobalMultiplier(player);

        // 1. Glowing und Emitting Level (Radiance)
        int emittingLevel = GlowingTrimUtils.getEmissionLevel(stack);
        int glowLevel = GlowingTrimUtils.getGlowLevel(stack);
        if (emittingLevel > 0) {
            textConsumer.accept(Text.translatable("tooltip.simplebuilding.radiance_level", emittingLevel).formatted(Formatting.GOLD));
        }
        if (glowLevel > 0) {
            textConsumer.accept(Text.translatable(glowLevel == 2 ? "tooltip.simplebuilding.glow_level_2" : "tooltip.simplebuilding.glow_level", glowLevel).formatted(Formatting.AQUA));
        }

        // 2. Armor Trim Boni
        var optionalTrim = stack.get(DataComponentTypes.TRIM);

        if (optionalTrim != null) {
            String id = optionalTrim.pattern().value().assetId().getPath();
            String materialId = optionalTrim.material().getKey().map(key -> key.getValue().getPath()).orElse("");

            if (emittingLevel > 0 || glowLevel > 0) {
                textConsumer.accept(Text.empty());
            }

            // --- MATERIAL BONI (Berechnet mit Multiplikator) ---
            if (materialId.contains("diamond")) {
                float val = 1.5f * mult;
                textConsumer.accept(Text.literal(String.format("Material: Hard Shell (-%.1f%% Dmg)", val)).formatted(Formatting.AQUA));
            }
            else if (materialId.contains("gold")) {
                float val = 3.0f * mult;
                textConsumer.accept(Text.literal(String.format("Material: Magic Dampening (-%.1f%% Magic)", val)).formatted(Formatting.GOLD));
            }
            else if (materialId.contains("iron")) {
                float val = 2.0f * mult;
                textConsumer.accept(Text.literal(String.format("Material: Blunt Resistance (-%.1f%% Proj)", val)).formatted(Formatting.GRAY));
            }
            else if (materialId.contains("netherite")) {
                float val = 2.0f * mult;
                textConsumer.accept(Text.literal(String.format("Material: Boss Resilience (-%.1f%% Boss)", val)).formatted(Formatting.DARK_GRAY));
            }
            else if (materialId.contains("emerald")) {
                float val = 4.0f * mult;
                textConsumer.accept(Text.literal(String.format("Material: Illager Resistance (-%.1f%%)", val)).formatted(Formatting.DARK_GREEN));
            }
            else if (materialId.contains("copper")) {
                float val = 5.0f * mult;
                textConsumer.accept(Text.literal(String.format("Material: Lightning Rod (-%.1f%% Ltn)", val)).formatted(Formatting.GOLD));
            }
            else if (materialId.contains("redstone")) {
                float val = 1.5f * mult; // Speed Bonus (grob geschätzt für Tooltip)
                textConsumer.accept(Text.literal(String.format("Material: Trap Awareness (+%.1f%% Speed)", val)).formatted(Formatting.RED));
            }
            else if (materialId.contains("quartz")) {
                float val = 2.5f * mult;
                textConsumer.accept(Text.literal(String.format("Material: Heat Shield (-%.1f%% Fire)", val)).formatted(Formatting.WHITE));
            }
            else if (materialId.contains("amethyst")) {
                float val = 3.0f * mult; // Sonic protection
                textConsumer.accept(Text.literal(String.format("Material: Resonance Shield (-%.1f%% Sonic)", val)).formatted(Formatting.LIGHT_PURPLE));
            }
            else if (materialId.contains("lapis")) {
                float val = 2.0f * mult;
                textConsumer.accept(Text.literal(String.format("Material: Curse Dampening (-%.1f%% Magic)", val)).formatted(Formatting.BLUE));
            }

            // --- PATTERN BONI ---

            if (id.contains("sentry")) {
                float val = 2.5f * mult;
                textConsumer.accept(Text.literal(String.format("Trim Bonus: Projectile Dampening (-%.1f%%)", val)).formatted(Formatting.BLUE));
            }
            else if (id.contains("vex")) {
                float val = 3.0f * mult;
                textConsumer.accept(Text.literal(String.format("Trim Bonus: Magic Dampening (-%.1f%%)", val)).formatted(Formatting.DARK_PURPLE));
            }
            else if (id.contains("wild")) {
                float val = 5.0f * mult;
                textConsumer.accept(Text.literal(String.format("Trim Bonus: Trap Resilience (-%.1f%%)", val)).formatted(Formatting.DARK_GREEN));
            }
            else if (id.contains("coast")) {
                float val = 10.0f * mult; // Chance
                textConsumer.accept(Text.literal(String.format("Trim Bonus: Oxygen Efficiency (+%.1f%% Chance)", val)).formatted(Formatting.AQUA));
            }
            else if (id.contains("dune")) {
                float val = 3.0f * mult;
                textConsumer.accept(Text.literal(String.format("Trim Bonus: Blast Dampening (-%.1f%%)", val)).formatted(Formatting.GOLD));
            }
            else if (id.contains("wayfinder")) {
                float val = 5.0f * mult;
                textConsumer.accept(Text.literal(String.format("Trim Bonus: Travel Efficiency (-%.1f%% Hunger)", val)).formatted(Formatting.YELLOW));
            }
            else if (id.contains("raiser")) {
                float val = 5.0f * mult;
                textConsumer.accept(Text.literal(String.format("Trim Bonus: Experience Affinity (+%.1f%% XP)", val)).formatted(Formatting.GREEN));
            }
            else if (id.contains("host")) {
                textConsumer.accept(Text.literal("Trim Bonus: Trade Negotiator (Luck Boost)").formatted(Formatting.WHITE));
            }
            else if (id.contains("ward")) {
                float val = 1.5f * mult;
                textConsumer.accept(Text.literal(String.format("Trim Bonus: Damage Absorption (-%.1f%% All)", val)).formatted(Formatting.DARK_AQUA));
            }
            else if (id.contains("silence")) {
                float val = 10.0f * mult;
                textConsumer.accept(Text.literal(String.format("Trim Bonus: Stealth Mobility (-%.1f%% Detection)", val)).formatted(Formatting.DARK_GRAY));
            }
            else if (id.contains("tide")) {
                float val = 5.0f * mult;
                textConsumer.accept(Text.literal(String.format("Trim Bonus: Swim Agility (+%.1f%% Speed)", val)).formatted(Formatting.DARK_AQUA));
            }
            else if (id.contains("snout")) {
                float val = 2.0f * mult;
                textConsumer.accept(Text.literal(String.format("Trim Bonus: Fire Dampening (-%.1f%%)", val)).formatted(Formatting.GOLD));
            }
            else if (id.contains("rib")) {
                float val = 5.0f * mult;
                textConsumer.accept(Text.literal(String.format("Trim Bonus: Wither Resistance (-%.1f%% Duration)", val)).formatted(Formatting.DARK_RED));
            }
            else if (id.contains("eye")) {
                float val = 5.0f * mult;
                textConsumer.accept(Text.literal(String.format("Trim Bonus: Ender Stability (-%.1f%% Void/Breath)", val)).formatted(Formatting.LIGHT_PURPLE));
            }
            else if (id.contains("spire")) {
                float val = 4.0f * mult;
                textConsumer.accept(Text.literal(String.format("Trim Bonus: Feather Falling (-%.1f%%)", val)).formatted(Formatting.LIGHT_PURPLE));
            }
            else if (id.contains("flow")) {
                float val = 5.0f * mult;
                textConsumer.accept(Text.literal(String.format("Trim Bonus: Aerial Agility (-%.1f%% Wind)", val)).formatted(Formatting.WHITE));
            }
            else if (id.contains("bolt")) {
                float val = 10.0f * mult;
                textConsumer.accept(Text.literal(String.format("Trim Bonus: Kinetic Response (-%.1f%% Lightning)", val)).formatted(Formatting.GOLD));
            }
        }
    }
}