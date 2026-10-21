package com.simplebuilding.modules.simplestats.client;

import com.simplebuilding.modules.simplestats.StatNames;
import com.simplebuilding.modules.simplestats.StatsComponents;
import com.simplebuilding.modules.simplestats.data.StatData;
import com.simplebuilding.modules.simplestats.data.StatFormat;
import com.simplebuilding.modules.simplestats.data.StatKeys;
import com.simplelib.api.client.ui.UiShiftTooltip;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Tooltip lines (tooltip rule, number 14 of the project): nothing for items without statistics; one headline plus SimpleLib's
 * "Hold Shift for more" ({@link UiShiftTooltip}, the same hint in every Simple mod); with Shift the most important values
 * and the key that opens the full screen.
 */
public final class StatsTooltip {
    /** Values listed with Shift (the screen shows everything). */
    public static final int SHIFT_VALUES = 5;

    private StatsTooltip() {}

    public static void append(ItemStack stack, Consumer<Component> builder) {
        StatData data = StatsComponents.of(stack);
        if (data.isBlank()) return;
        Locale locale = StatsClient.locale();
        String primary = primary(data);
        if (primary == null) return;
        builder.accept(headline(data, primary, locale));
        if (!UiShiftTooltip.held()) {
            builder.accept(UiShiftTooltip.hint());
            return;
        }
        int shown = 0;
        for (String key : StatKeys.TOTAL_ORDER) {
            if (key.equals(primary) || data.total(key) <= 0) continue;
            if (shown++ >= SHIFT_VALUES - 1) break;
            builder.accept(Component.translatable("tooltip.simplestats.value", StatNames.total(key), StatFormat.value(StatKeys.unit(key), data.total(key), locale, true))
                    .withStyle(ChatFormatting.GRAY));
        }
        builder.accept(Component.translatable("tooltip.simplestats.open", StatsClient.openKeyName()).withStyle(ChatFormatting.DARK_GRAY));
    }

    /** The first present total in priority order, or null. */
    public static String primary(StatData data) {
        for (String key : StatKeys.TOTAL_ORDER) if (data.total(key) > 0) return key;
        return null;
    }

    /** "5,472 blocks - mostly Stone" style line; the most frequent entry is added when the total has a list. */
    static Component headline(StatData data, String primary, Locale locale) {
        Component value = Component.literal(StatFormat.value(StatKeys.unit(primary), data.total(primary), locale, true));
        String list = listOf(primary);
        if (list != null) {
            String top = topKey(data.detail(list));
            if (top != null) {
                return Component.translatable("tooltip.simplestats.top." + primary, value, StatNames.entry(list, top)).withStyle(ChatFormatting.GOLD);
            }
        }
        return Component.translatable("tooltip.simplestats.line." + primary, value).withStyle(ChatFormatting.GOLD);
    }

    private static String listOf(String total) {
        return switch (total) {
            case StatKeys.BLOCKS_MINED -> StatKeys.L_BLOCKS;
            case StatKeys.KILLS -> StatKeys.L_KILLS;
            case StatKeys.CATCHES -> StatKeys.L_CATCHES;
            case StatKeys.DAMAGE_ABSORBED -> StatKeys.L_ABSORBED;
            default -> null;
        };
    }

    static String topKey(StatData.Detail detail) {
        String best = null;
        long bestValue = 0;
        for (Map.Entry<String, Long> e : detail.entries().entrySet()) {
            if (e.getValue() > bestValue) {
                best = e.getKey();
                bestValue = e.getValue();
            }
        }
        return best;
    }
}
