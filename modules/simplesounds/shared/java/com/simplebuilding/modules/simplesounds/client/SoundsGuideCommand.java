package com.simplebuilding.modules.simplesounds.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.simplebuilding.modules.simplesounds.guide.SoundsGuide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;

/** Client command {@code /simplesounds guide}: opens the guide pages in the Vanilla book screen (no item, client-only mod). */
public final class SoundsGuideCommand {
    private SoundsGuideCommand() {
    }

    public static <S> void register(CommandDispatcher<S> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<S>literal("simplesounds")
                .then(LiteralArgumentBuilder.<S>literal("guide").executes(context -> {
                    open();
                    return 1;
                })));
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.schedule(() -> minecraft.gui.setScreen(new BookViewScreen(new BookViewScreen.BookAccess(SoundsGuide.pages()))));
    }
}
