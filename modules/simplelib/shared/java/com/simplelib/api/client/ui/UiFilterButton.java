package com.simplelib.api.client.ui;

import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * The filter key of every filtering block of the Simple mods (owner's filter principle, 2026-10-09: mod hoppers,
 * autonomous crafter, later ones): a raised 18x18 key that shows only the mode - off (red cross), exact (green
 * check), same kind (three yellow squares). Its caption is {@link #label}: an engraved funnel and a colon in front of
 * the key, instead of the word "Filter". Same pixels everywhere; the block decides what a press does. Client only.
 */
public class UiFilterButton extends Button {
    public static final int SIZE = 18;
    /** Mode numbers the key draws (the user's enum order: off, exact, same kind). */
    public static final int OFF = 0, EXACT = 1, KIND = 2;
    /** Width of {@link #label}: funnel (12), gap (2), colon (2). */
    public static final int LABEL_WIDTH = 16;

    private static final int RED = 0xFFD8402F, GREEN = 0xFF55E05A, YELLOW = 0xFFFFE055;

    private final IntSupplier mode;
    private final Supplier<UiPalette> palette;

    public UiFilterButton(int x, int y, OnPress onPress, IntSupplier mode, Supplier<UiPalette> palette) {
        super(x, y, SIZE, SIZE, Component.empty(), onPress, DEFAULT_NARRATION);
        this.mode = mode;
        this.palette = palette;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        draw(g, getX(), getY(), palette.get(), mode.getAsInt(), isHoveredOrFocused());
    }

    /** The key at ({@code x}, {@code y}) in {@code p}, showing {@code mode}. */
    public static void draw(GuiGraphicsExtractor g, int x, int y, UiPalette p, int mode, boolean hovered) {
        UiBoxes.raised(g, x, y, SIZE, SIZE, UiPalette.mix(p.fill(), 0xFFFFFFFF, hovered ? 0.32 : 0.18));
        switch (mode) {
            case EXACT -> UiSymbols.engrave(g, UiSymbols.CHECK, x + 5, y + 6, p, GREEN, false);
            case KIND -> {
                for (int k = 0; k < 3; k++) g.fill(x + 3 + k * 4, y + 7, x + 6 + k * 4, y + 10, YELLOW);
            }
            default -> UiSymbols.engrave(g, UiSymbols.CROSS, x + 5, y + 5, p, RED, false);
        }
    }

    /** The caption in front of the key: an engraved funnel and a colon, {@link #LABEL_WIDTH} wide, 12 high. */
    public static void label(GuiGraphicsExtractor g, int x, int y, UiPalette p) {
        UiSymbols.engrave(g, UiSymbols.FUNNEL, x, y, p);
        UiSymbols.engrave(g, COLON, x + 14, y + 3, p);
    }

    /** 2x7 colon after the funnel. */
    public static final UiSymbols.Bitmap COLON = UiSymbols.Bitmap.of("##", "##", "..", "..", "..", "##", "##");
}
