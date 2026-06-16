package com.simplebuilding.mixin.client;

import com.simplebuilding.util.GlowingTrimUtils;
import com.simplebuilding.util.TrimEffectUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "appendHoverText", at = @At("TAIL"))
    private void simplebuilding$appendTrimBuffs(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay displayComponent,
            Consumer<Component> textConsumer,
            TooltipFlag type,
            CallbackInfo ci
    ) {
        // Client-Side Check für den Player (für Multiplikator)
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // Globaler Multiplikator (Level + Survival Stats)
        float mult = TrimEffectUtil.getGlobalMultiplier(player);

        // 1. Glowing und Emitting Level (Radiance)
        int emittingLevel = GlowingTrimUtils.getEmissionLevel(stack);
        int glowLevel = GlowingTrimUtils.getGlowLevel(stack);
        if (emittingLevel > 0) {
            textConsumer.accept(Component.translatable("tooltip.simplebuilding.radiance_level", emittingLevel).withStyle(ChatFormatting.GOLD));
        }
        if (glowLevel > 0) {
            textConsumer.accept(Component.translatable(glowLevel == 2 ? "tooltip.simplebuilding.glow_level_2" : "tooltip.simplebuilding.glow_level", glowLevel).withStyle(ChatFormatting.AQUA));
        }

        // 2. Armor Trim Boni
        var optionalTrim = stack.get(DataComponents.TRIM);

        if (optionalTrim != null) {
            String id = optionalTrim.pattern().value().assetId().getPath();
            String materialId = optionalTrim.material().unwrapKey().map(key -> key.identifier().getPath()).orElse("");

            if (emittingLevel > 0 || glowLevel > 0) {
                textConsumer.accept(Component.empty());
            }

            // --- MATERIAL BONI (Berechnet mit Multiplikator) ---
            if (materialId.contains("diamond")) {
                float val = 1.5f * mult;
                textConsumer.accept(Component.literal(String.format("Material: Hard Shell (-%.1f%% Dmg)", val)).withStyle(ChatFormatting.AQUA));
            }
            else if (materialId.contains("gold")) {
                float val = 3.0f * mult;
                textConsumer.accept(Component.literal(String.format("Material: Magic Dampening (-%.1f%% Magic)", val)).withStyle(ChatFormatting.GOLD));
            }
            else if (materialId.contains("iron")) {
                float val = 2.0f * mult;
                textConsumer.accept(Component.literal(String.format("Material: Blunt Resistance (-%.1f%% Proj)", val)).withStyle(ChatFormatting.GRAY));
            }
            else if (materialId.contains("netherite")) {
                float val = 2.0f * mult;
                textConsumer.accept(Component.literal(String.format("Material: Boss Resilience (-%.1f%% Boss)", val)).withStyle(ChatFormatting.DARK_GRAY));
            }
            else if (materialId.contains("emerald")) {
                float val = 4.0f * mult;
                textConsumer.accept(Component.literal(String.format("Material: Illager Resistance (-%.1f%%)", val)).withStyle(ChatFormatting.DARK_GREEN));
            }
            else if (materialId.contains("copper")) {
                float val = 5.0f * mult;
                textConsumer.accept(Component.literal(String.format("Material: Lightning Rod (-%.1f%% Ltn)", val)).withStyle(ChatFormatting.GOLD));
            }
            else if (materialId.contains("redstone")) {
                float val = 1.5f * mult; // Speed Bonus (grob geschätzt für Tooltip)
                textConsumer.accept(Component.literal(String.format("Material: Trap Awareness (+%.1f%% Speed)", val)).withStyle(ChatFormatting.RED));
            }
            else if (materialId.contains("quartz")) {
                float val = 2.5f * mult;
                textConsumer.accept(Component.literal(String.format("Material: Heat Shield (-%.1f%% Fire)", val)).withStyle(ChatFormatting.WHITE));
            }
            else if (materialId.contains("amethyst")) {
                float val = 3.0f * mult; // Sonic protection
                textConsumer.accept(Component.literal(String.format("Material: Resonance Shield (-%.1f%% Sonic)", val)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            else if (materialId.contains("lapis")) {
                float val = 2.0f * mult;
                textConsumer.accept(Component.literal(String.format("Material: Curse Dampening (-%.1f%% Magic)", val)).withStyle(ChatFormatting.BLUE));
            }

            // --- PATTERN BONI ---

            if (id.contains("sentry")) {
                float val = 2.5f * mult;
                textConsumer.accept(Component.literal(String.format("Trim Bonus: Projectile Dampening (-%.1f%%)", val)).withStyle(ChatFormatting.BLUE));
            }
            else if (id.contains("vex")) {
                float val = 3.0f * mult;
                textConsumer.accept(Component.literal(String.format("Trim Bonus: Magic Dampening (-%.1f%%)", val)).withStyle(ChatFormatting.DARK_PURPLE));
            }
            else if (id.contains("wild")) {
                float val = 5.0f * mult;
                textConsumer.accept(Component.literal(String.format("Trim Bonus: Trap Resilience (-%.1f%%)", val)).withStyle(ChatFormatting.DARK_GREEN));
            }
            else if (id.contains("coast")) {
                float val = 10.0f * mult; // Chance
                textConsumer.accept(Component.literal(String.format("Trim Bonus: Oxygen Efficiency (+%.1f%% Chance)", val)).withStyle(ChatFormatting.AQUA));
            }
            else if (id.contains("dune")) {
                float val = 3.0f * mult;
                textConsumer.accept(Component.literal(String.format("Trim Bonus: Blast Dampening (-%.1f%%)", val)).withStyle(ChatFormatting.GOLD));
            }
            else if (id.contains("wayfinder")) {
                float val = 5.0f * mult;
                textConsumer.accept(Component.literal(String.format("Trim Bonus: Travel Efficiency (-%.1f%% Hunger)", val)).withStyle(ChatFormatting.YELLOW));
            }
            else if (id.contains("raiser")) {
                float val = 5.0f * mult;
                textConsumer.accept(Component.literal(String.format("Trim Bonus: Experience Affinity (+%.1f%% XP)", val)).withStyle(ChatFormatting.GREEN));
            }
            else if (id.contains("host")) {
                textConsumer.accept(Component.literal("Trim Bonus: Trade Negotiator (Luck Boost)").withStyle(ChatFormatting.WHITE));
            }
            else if (id.contains("ward")) {
                float val = 1.5f * mult;
                textConsumer.accept(Component.literal(String.format("Trim Bonus: Damage Absorption (-%.1f%% All)", val)).withStyle(ChatFormatting.DARK_AQUA));
            }
            else if (id.contains("silence")) {
                float val = 10.0f * mult;
                textConsumer.accept(Component.literal(String.format("Trim Bonus: Stealth Mobility (-%.1f%% Detection)", val)).withStyle(ChatFormatting.DARK_GRAY));
            }
            else if (id.contains("tide")) {
                float val = 5.0f * mult;
                textConsumer.accept(Component.literal(String.format("Trim Bonus: Swim Agility (+%.1f%% Speed)", val)).withStyle(ChatFormatting.DARK_AQUA));
            }
            else if (id.contains("snout")) {
                float val = 2.0f * mult;
                textConsumer.accept(Component.literal(String.format("Trim Bonus: Fire Dampening (-%.1f%%)", val)).withStyle(ChatFormatting.GOLD));
            }
            else if (id.contains("rib")) {
                float val = 5.0f * mult;
                textConsumer.accept(Component.literal(String.format("Trim Bonus: Wither Resistance (-%.1f%% Duration)", val)).withStyle(ChatFormatting.DARK_RED));
            }
            else if (id.contains("eye")) {
                float val = 5.0f * mult;
                textConsumer.accept(Component.literal(String.format("Trim Bonus: Ender Stability (-%.1f%% Void/Breath)", val)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            else if (id.contains("spire")) {
                float val = 4.0f * mult;
                textConsumer.accept(Component.literal(String.format("Trim Bonus: Feather Falling (-%.1f%%)", val)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            else if (id.contains("flow")) {
                float val = 5.0f * mult;
                textConsumer.accept(Component.literal(String.format("Trim Bonus: Aerial Agility (-%.1f%% Wind)", val)).withStyle(ChatFormatting.WHITE));
            }
            else if (id.contains("bolt")) {
                float val = 10.0f * mult;
                textConsumer.accept(Component.literal(String.format("Trim Bonus: Kinetic Response (-%.1f%% Lightning)", val)).withStyle(ChatFormatting.GOLD));
            }
        }
    }
}