package com.simplebuilding.modules.simpleearth.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.simplebuilding.modules.simpleearth.guide.EarthGuide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;

/** Client command {@code /simpleearth guide}: opens the guide pages in the Vanilla book screen (Simple Earth has no items). */
public final class EarthGuideCommand {
    private EarthGuideCommand() {
    }

    public static <S> void register(CommandDispatcher<S> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<S>literal("simpleearth")
                .then(LiteralArgumentBuilder.<S>literal("guide").executes(context -> {
                    open();
                    return 1;
                })));
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.schedule(() -> minecraft.gui.setScreen(new BookViewScreen(new BookViewScreen.BookAccess(EarthGuide.pages()))));
    }
}
