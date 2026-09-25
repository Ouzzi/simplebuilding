package com.simplebuilding.mixin;

import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Der Layout-Platzhalter {@code simplebuilding:creative_spacer} erscheint nicht in den
 * Item-Vorschlaegen von Befehlen ({@code /give}, {@code /clear}, {@code /item} ...).
 *
 * <p>Alle diese Befehle nehmen ein {@code ItemArgument}; dessen Vorschlaege baut
 * {@code ItemParser.State#suggestItem} aus allen Ids der Item-Registry (auf dem Client, aus dessen
 * Registern). Hier faellt genau die eine Id aus dem Strom. Wer {@code /give @s
 * simplebuilding:creative_spacer} von Hand tippt, bekommt ihn weiterhin - nur angeboten wird er nicht,
 * weil er ausserhalb des Kreativinventars nichts tut (er loescht sich dort selbst).
 */
@Mixin(targets = "net.minecraft.commands.arguments.item.ItemParser$State")
public abstract class ItemParserStateMixin {
    private static final Identifier SIMPLEBUILDING$SPACER =
            Identifier.fromNamespaceAndPath("simplebuilding", "creative_spacer");

    @ModifyArg(method = "suggestItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/commands/SharedSuggestionProvider;suggestResource(Ljava/util/stream/Stream;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;"),
            index = 0)
    private Stream<Identifier> simplebuilding$hideSpacer(Stream<Identifier> ids) {
        return ids.filter(id -> !SIMPLEBUILDING$SPACER.equals(id));
    }
}
