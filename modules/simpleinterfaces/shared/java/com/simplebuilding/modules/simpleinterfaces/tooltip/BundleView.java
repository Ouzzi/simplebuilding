package com.simplebuilding.modules.simpleinterfaces.tooltip;

import com.simplebuilding.modules.simpleinterfaces.ContainersConfig;
import net.minecraft.world.item.component.BundleContents;
import org.jspecify.annotations.Nullable;

/**
 * Bundle tooltip without Vanilla's 12-entry limit (docs/ai/KONZEPT-BUNDLES-BEYOND-2026-10-19.md, stage 1). Reads only
 * the Vanilla {@code bundle_contents} component, so Vanilla bundles and every mod bundle drawn by Vanilla's
 * {@code ClientBundleTooltip} get it alike. Up to {@code maxRows * 4} entries all show; more go in pages of
 * {@code maxRows * 4 - 1} entries plus the "+n" cell, and the page follows the mouse-wheel selection.
 * {@code start}/{@code shown}: the entries on the page, {@code cells} the grid cells including "+n".
 */
public record BundleView(int start, int shown, boolean overflow, int cells) {
    public static final int COLUMNS = 4;
    /** Vanilla shows at most 12 cells (11 entries + "+n" when there are more). */
    public static final int VANILLA_CELLS = 12;
    public static final int MIN_ROWS = 3;
    public static final int MAX_ROWS = 16;

    /** The view for a bundle with {@code size} entries; null = Vanilla unchanged (switch off or nothing hidden anyway). */
    public static @Nullable BundleView of(int size, int selected, ContainersConfig config) {
        if (config == null || !config.bundleShowAll || size <= VANILLA_CELLS) return null;
        int cap = rows(config) * COLUMNS;
        if (size <= cap) return new BundleView(0, size, false, size);
        int page = cap - 1;
        int start = selected < 0 || selected >= size ? 0 : selected / page * page;
        int shown = Math.min(page, size - start);
        return new BundleView(start, shown, true, shown + 1);
    }

    /** Configured rows within {@link #MIN_ROWS}..{@link #MAX_ROWS} (never fewer than Vanilla's three). */
    public static int rows(ContainersConfig config) {
        return Math.clamp(config.bundleMaxRows, MIN_ROWS, MAX_ROWS);
    }

    /** How many entries the mouse wheel cycles through: all with the view, else Vanilla's {@code vanillaShown}. */
    public static int selectable(int size, int vanillaShown, ContainersConfig config) {
        return of(size, -1, config) == null ? vanillaShown : size;
    }

    /** Items (summed counts) not on the page, for the "+n" cell. */
    public static int hidden(BundleContents contents, BundleView view) {
        int sum = 0;
        for (int i = 0; i < contents.size(); i++) {
            if (i < view.start || i >= view.start + view.shown) sum += contents.items().get(i).count();
        }
        return sum;
    }
}
