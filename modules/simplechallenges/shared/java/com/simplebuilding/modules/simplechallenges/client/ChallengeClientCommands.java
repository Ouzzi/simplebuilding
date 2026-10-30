package com.simplebuilding.modules.simplechallenges.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.simplebuilding.modules.simplechallenges.guide.ChallengesGuide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;

/** Client commands: {@code /simplechallenges guide} (the handbook, Simple Challenges has no items) and {@code /simplechallenges border} (border line on/off). */
public final class ChallengeClientCommands {
    private ChallengeClientCommands() {}

    public static <S> void register(CommandDispatcher<S> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<S>literal("simplechallenges")
                .then(LiteralArgumentBuilder.<S>literal("guide").executes(context -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.schedule(() -> minecraft.gui.setScreen(new BookViewScreen(new BookViewScreen.BookAccess(ChallengesGuide.pages()))));
                    return 1;
                }))
                .then(LiteralArgumentBuilder.<S>literal("border").executes(context -> {
                    boolean on = ChallengeClient.toggleBorder();
                    Minecraft.getInstance().player.sendSystemMessage(Component.translatable(on ? "simplechallenges.border.on" : "simplechallenges.border.off"));
                    return 1;
                })));
    }
}
