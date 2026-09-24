package com.simplebuilding.mixin.client;

import com.simplebuilding.util.GlowingTrimUtils;
import com.simplebuilding.util.SledgehammerUpgrades;
import com.simplebuilding.util.TrimBonusCatalog;
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
        // Aufwertbare Maschinen (verstaerkt und Netherit): Hinweis auf den Vorschlaghammer, seit
        // es fuer die Netherit- und Enderit-Stufe kein Werkbankrezept mehr gibt.
        Component upgradeHint = SledgehammerUpgrades.tooltipHint(stack);
        if (upgradeHint != null) {
            textConsumer.accept(upgradeHint);
        }

        // Client-Side Check für den Player (für Multiplikator)
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // Resonanz des Spielers (Stufe, Ueberleben, Kampf) - clientseitig aus den synchronisierten Werten
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

        // 2. Armor Trim Boni - im Stil der Vanilla-Attributliste, Werte aus TrimBonusCatalog
        // (dieselben Konstanten, mit denen TrimEffectUtil rechnet).
        var trim = stack.get(DataComponents.TRIM);
        if (trim != null) {
            TrimBonusCatalog.tooltipLines(trim, mult).forEach(textConsumer);
        }
    }
}