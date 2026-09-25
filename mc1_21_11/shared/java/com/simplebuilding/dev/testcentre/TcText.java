package com.simplebuilding.dev.testcentre;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Beschriftungen der Testzentrale. Alles ist {@code translatable}: Schilder und Namensschilder werden
 * im Client uebersetzt, ein deutscher Client liest also Deutsch, ein englischer Englisch. Der
 * englische Text steht zusaetzlich als Rueckfall im Code, falls ein Schluessel fehlt.
 */
public final class TcText {

    /** Praefix aller Schluessel ({@code en_us.json}/{@code de_de.json}). */
    public static final String PREFIX = "simplebuilding.testcentre.";

    private TcText() {
    }

    public static MutableComponent t(String key, String fallback, Object... args) {
        return Component.translatableWithFallback(PREFIX + key, fallback, args);
    }

    public static MutableComponent lit(String text) {
        return Component.literal(text);
    }

    public static Component bold(Component text) {
        return text.copy().withStyle(ChatFormatting.BOLD);
    }
}
