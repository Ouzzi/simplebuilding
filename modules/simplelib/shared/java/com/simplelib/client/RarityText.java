package com.simplelib.client;

import com.simplelib.api.SimpleRarity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/**
 * Tooltip letters of {@link SimpleRarity} items, painted pixel row by pixel row (owner 21.10., queue b-rarity21b,
 * docs/ai/PLAN-B-RARITY21B-2026-10-22.md). A text component can only colour whole letters, so after Vanilla has drawn the
 * line (with the tier shadow) each run of marked letters ({@link SimpleRarity#MARKER}) is drawn again once per pixel row,
 * clipped to that row by a scissor, without shadow, in {@link SimpleRarity#pixel}'s colour: cursed letters burn from
 * below, mythic letters shine like an opal. Glyphs are drawn in submission order, so the rows lie on top of Vanilla's.
 */
public final class RarityText {
    /** Rows drawn, relative to the line's y: accented capitals reach 3 rows up, descenders down to row 7. */
    private static final int FIRST_ROW = -3, LAST_ROW = 8;

    private RarityText() {}

    /** Called after {@code ClientTextTooltip} drew {@code text} at x/y; does nothing for lines without marked letters. */
    public static void paint(GuiGraphicsExtractor graphics, Font font, FormattedCharSequence text, int x, int y, long millis) {
        if (text.accept((pos, style, cp) -> SimpleRarity.byMarker(style.getInsertion()) == null)) return;
        List<Integer> cps = new ArrayList<>();
        List<Style> styles = new ArrayList<>();
        text.accept((pos, style, cp) -> {
            cps.add(cp);
            styles.add(style);
            return true;
        });
        int i = 0;
        while (i < cps.size()) {
            SimpleRarity tier = SimpleRarity.byMarker(styles.get(i).getInsertion());
            int end = i + 1;
            while (end < cps.size() && SimpleRarity.byMarker(styles.get(end).getInsertion()) == tier) end++;
            if (tier != null) {
                // the run starts where Vanilla put its first letter (widths of whole sequences, as the font sums them)
                int start = x + font.width(sequence(cps, styles, 0, i));
                run(graphics, font, tier, cps.subList(i, end), styles.subList(i, end), start, y, font.width(sequence(cps, styles, i, end)), millis);
            }
            i = end;
        }
    }

    private static FormattedCharSequence sequence(List<Integer> cps, List<Style> styles, int from, int to) {
        List<FormattedCharSequence> letters = new ArrayList<>(to - from);
        for (int k = from; k < to; k++) letters.add(FormattedCharSequence.codepoint(cps.get(k), styles.get(k)));
        return FormattedCharSequence.composite(letters);
    }

    private static void run(GuiGraphicsExtractor graphics, Font font, SimpleRarity tier, List<Integer> cps, List<Style> styles,
                            int x, int y, int width, long millis) {
        int n = cps.size();
        for (int row = FIRST_ROW; row <= LAST_ROW; row++) {
            List<FormattedCharSequence> letters = new ArrayList<>(n);
            for (int k = 0; k < n; k++) {
                letters.add(FormattedCharSequence.codepoint(cps.get(k), styles.get(k).withColor(tier.pixel(k, n, row, millis)).withoutShadow()));
            }
            // a little wider than the run: italic (renamed items) and bold glyphs lean or reach past their advance
            graphics.enableScissor(x - 2, y + row, x + width + 3, y + row + 1);
            graphics.text(font, FormattedCharSequence.composite(letters), x, y, -1, false);
            graphics.disableScissor();
        }
    }
}
