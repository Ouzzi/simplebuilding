package com.simplebuilding.neoforge.clienttest.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reaches vanilla's two private keyboard callbacks, so a client test can type into an open screen.
 *
 * <p>The keyboard half of {@link MouseHandlerAccessor}: {@code KeyMapping.set}/{@code click} feed the
 * binding layer, and a screen never reads that. A text field needs the key callback (Backspace,
 * Control+V) and - a separate GLFW callback - the character callback for anything typed. Going
 * through both is the path a player's keyboard takes, including NeoForge's screen events in
 * between. Fabric's client test API does the same through its own accessor.
 */
@Mixin(KeyboardHandler.class)
public interface KeyboardHandlerAccessor {

    @Invoker("keyPress")
    void simplebuilding$keyPress(long windowHandle, int action, KeyEvent event);

    @Invoker("charTyped")
    void simplebuilding$charTyped(long windowHandle, CharacterEvent event);
}
