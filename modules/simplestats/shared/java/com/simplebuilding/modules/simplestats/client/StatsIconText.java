package com.simplebuilding.modules.simplestats.client;

import com.simplebuilding.modules.simplestats.data.StatKeys;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

/**
 * N78 (owner 16.11.): a small item icon inside a tooltip text line ("2 blocks · Top 1: [icon]"). A line can only carry text, so the
 * icon is a placeholder of two spaces (8 px) whose {@link Style#getInsertion() insertion} names the item; after Vanilla's
 * {@code ClientTextTooltip} drew the line, {@link #paint} draws the item at half size over the placeholder (mixin
 * {@code ClientTextTooltipIconMixin}, the same hook as SimpleLib's rarity letters, so every loader shows it). Without the mixin
 * the line just has a gap.
 */
public final class StatsIconText {
    static final String MARK = "simplestats:icon=";
    private static final String PLACEHOLDER = "  ";
    private static final Map<String, ItemStack> CACHE = new HashMap<>();

    private StatsIconText() {}

    /** The placeholder for the item that stands for a list entry, or null when the entry has no item (damage type, mob without egg). */
    public static Component icon(String list, String key) {
        ItemStack stack = item(list, key);
        if (stack.isEmpty()) return null;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return Component.literal(PLACEHOLDER).withStyle(Style.EMPTY.withInsertion(MARK + id));
    }

    /** Block -> its item, caught item, mob (also with variant) -> its spawn egg; EMPTY otherwise. */
    static ItemStack item(String list, String key) {
        return CACHE.computeIfAbsent(list + '|' + key, k -> lookup(list, key));
    }

    private static ItemStack lookup(String list, String key) {
        int hash = key.indexOf('#');
        Identifier id = Identifier.tryParse(hash < 0 ? key : key.substring(0, hash));
        if (id == null) return ItemStack.EMPTY;
        var item = switch (list) {
            case StatKeys.L_BLOCKS, StatKeys.L_BRUSHED -> BuiltInRegistries.BLOCK.getOptional(id).map(b -> b.asItem()).orElse(Items.AIR);
            case StatKeys.L_CATCHES -> BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
            case StatKeys.L_KILLS, StatKeys.L_KILL_VARIANTS, StatKeys.L_SHEARED, StatKeys.L_BLOCKED_BY -> BuiltInRegistries.ENTITY_TYPE.getOptional(id)
                    .flatMap(SpawnEggItem::byId).map(h -> h.value()).orElse(Items.AIR);
            default -> Items.AIR;
        };
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** Called after {@code ClientTextTooltip} drew {@code text} at x/y; does nothing for lines without a placeholder. */
    public static void paint(GuiGraphicsExtractor graphics, Font font, FormattedCharSequence text, int x, int y) {
        if (text.accept((pos, style, cp) -> !isMark(style))) return;
        List<FormattedCharSequence> letters = new ArrayList<>();
        List<Style> styles = new ArrayList<>();
        text.accept((pos, style, cp) -> {
            letters.add(FormattedCharSequence.codepoint(cp, style));
            styles.add(style);
            return true;
        });
        int i = 0;
        while (i < styles.size()) {
            if (!isMark(styles.get(i))) {
                i++;
                continue;
            }
            String mark = styles.get(i).getInsertion();
            int end = i + 1;
            while (end < styles.size() && mark.equals(styles.get(end).getInsertion())) end++;
            int start = x + font.width(FormattedCharSequence.composite(letters.subList(0, i)));
            int width = font.width(FormattedCharSequence.composite(letters.subList(i, end)));
            Identifier id = Identifier.tryParse(mark.substring(MARK.length()));
            var item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item != null && item != Items.AIR) {
                var pose = graphics.pose();
                pose.pushMatrix();
                pose.translate(start + (width - 8) / 2f, y - 0.5f);
                pose.scale(0.5f, 0.5f);
                graphics.item(CACHE.computeIfAbsent("item|" + id, k -> new ItemStack(item)), 0, 0);
                pose.popMatrix();
            }
            i = end;
        }
    }

    private static boolean isMark(Style style) {
        String insertion = style.getInsertion();
        return insertion != null && insertion.startsWith(MARK);
    }
}
