package com.simplebuilding.modules.simplestats.client;

import com.simplebuilding.modules.simplestats.StatNames;
import com.simplebuilding.modules.simplestats.StatsComponents;
import com.simplebuilding.modules.simplestats.data.StatData;
import com.simplebuilding.modules.simplestats.data.StatFormat;
import com.simplebuilding.modules.simplestats.data.StatKeys;
import com.simplebuilding.modules.simplestats.data.StatTop;
import com.simplelib.api.client.ui.UiShiftTooltip;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Tooltip lines (tooltip rule, number 14 of the project): nothing for items without statistics; one headline
 * ("2 blocks · Top 1: [icon]", N78) plus SimpleLib's "Hold Shift for more" ({@link UiShiftTooltip}, the same hint in every Simple mod)
 * when Shift shows more than the key line; with Shift the top entries of the headline's list (icon, name, amount), the other most
 * important totals and the key that opens the full screen.
 */
public final class StatsTooltip {
    /** Values listed with Shift (the screen shows everything). */
    public static final int SHIFT_VALUES = 5;
    /** Entries of the headline's list shown with Shift (N78). */
    public static final int SHIFT_TOP = 3;

    private StatsTooltip() {}

    public static void append(ItemStack stack, Consumer<Component> builder) {
        StatData data = StatsComponents.of(stack);
        if (data.isBlank()) return;
        Locale locale = StatsClient.locale();
        String primary = primary(data);
        if (primary == null) return;
        builder.accept(headline(data, primary, locale));
        String list = listOf(primary);
        List<Map.Entry<String, Long>> top = list == null ? List.of() : StatTop.of(data.detail(list), SHIFT_TOP);
        List<String> others = new ArrayList<>();
        for (String key : StatKeys.TOTAL_ORDER) {
            if (others.size() >= SHIFT_VALUES - 1) break;
            if (!key.equals(primary) && data.total(key) > 0) others.add(key);
        }
        if (!UiShiftTooltip.held()) {
            // N78: the hint only when Shift adds more than the key line
            if (!top.isEmpty() || !others.isEmpty()) builder.accept(UiShiftTooltip.hint());
            return;
        }
        for (Map.Entry<String, Long> e : top) {
            Component amount = Component.literal(StatFormat.value(StatKeys.unit(primary), e.getValue(), locale, true));
            Component icon = StatsIconText.icon(list, e.getKey());
            Component name = StatNames.entry(list, e.getKey());
            builder.accept((icon == null ? Component.translatable("tooltip.simplestats.value", name, amount)
                    : Component.translatable("tooltip.simplestats.entry", icon, name, amount)).withStyle(ChatFormatting.GRAY));
        }
        for (String key : others) {
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

    /** "5,472 blocks · Top 1: [icon]" style line (the name instead of the icon when the entry has no item); plain total without a list. */
    static Component headline(StatData data, String primary, Locale locale) {
        Component value = Component.literal(StatFormat.value(StatKeys.unit(primary), data.total(primary), locale, true));
        String list = listOf(primary);
        if (list != null) {
            List<Map.Entry<String, Long>> top = StatTop.of(data.detail(list), 1);
            if (!top.isEmpty()) {
                String key = top.getFirst().getKey();
                Component icon = StatsIconText.icon(list, key);
                return Component.translatable("tooltip.simplestats.top." + primary, value, icon != null ? icon : StatNames.entry(list, key)).withStyle(ChatFormatting.GOLD);
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
}
